import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public class StartServer implements Runnable {
    private static final int MAX_SIZE = 4096;
    private static String ADDR_PORT_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static InetAddress AddrGRDS = null;
    private static int PortGRDS = 0;
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pd_chat";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static ArrayList<Socket> listClientSockets = new ArrayList<>();
    private static ServerSocket listeningSocket;
    private static MulticastSocket SocketGRDS = null;
    private static DatagramPacket packet = null;
    private static ArrayList<ServerData> serverList = new ArrayList<>();
    private static boolean newFile = false;
    private static File file = new File();
    private Request request = new Request();
    private static ArrayList<ClientData> clientsToNotify = new ArrayList<>();
    private static ClientData client;
    private static boolean notification = false;
    private static String notificationMessage;
    private static String notificationType;
    private static String username;
    private static String groupName;

    public StartServer() {

    }

    public void setNotificationType(String notificationType) {
        StartServer.notificationType = notificationType;
    }

    public boolean isNewFile() { return newFile; }

    public void setNewFile(boolean newFile) { StartServer.newFile = newFile; }

    public boolean isNotification() { return notification; }

    public void startServer(String [] args) {
        boolean connected = false;
        Statement stmt;
        Connection conn;

        Socket nextClient;
        NetworkInterface nif;

        int attempt = 0;

        //Verifica se receber os argumentos necessários: IP e do SGBD e opcionamente o IP e porto de escuta do GRDS
        if (args.length < 1 || args.length > 3 || args.length == 2) {
            System.out.println("Sintaxe: java Servidor SGBDaddress GRDSaddress(opcional) GRDSport(opcional) ");
            return;
        }

        //Tentar connectar-se à base de dados
        try {
            Class.forName(JDBC_DRIVER);

            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        }catch (SQLException | ClassNotFoundException e){
            e.printStackTrace();
            return;
        }

        try {
            //Cria um DatagramSocket para comunicar com o GRDS
            SocketGRDS = new MulticastSocket();
            SocketGRDS.setSoTimeout(10000);
        } catch(SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            if (args.length == 1) {
                AddrGRDS = InetAddress.getByName("230.30.30.30");
                PortGRDS = 3030;
            } else {
                AddrGRDS = InetAddress.getByName(args[1]);
                PortGRDS = Integer.parseInt(args[2]);
            }

            SocketGRDS.joinGroup(AddrGRDS);
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (attempt != 3 && !connected) {
            try {


                request.setMessage(ADDR_PORT_REQUEST);
                byte [] data = serialize(request);
                //Cria um DatagramPacket e envia-o ao GRDS através do DatagramSocket criado anteriormente
                packet = new DatagramPacket(data, data.length, AddrGRDS, PortGRDS);
                SocketGRDS.send(packet);

                //Limpa o packet e recebe resposta do GRDS
                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                SocketGRDS.receive(packet);

                connected = true;
            } catch (SocketTimeoutException e) {
                attempt++;
                System.out.println("\nNão foi possivel estabelecer ligação com o GRDS. Tentativas restantes: " + (3 - attempt));
            } catch (UnknownHostException e) {
                System.out.println("\nEndereço desconhecido.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        //Se não se conseguir conectar com o GRDS, encerra
        if (!connected){
            System.out.println("\nA desligar o servidor...");
            SocketGRDS.close();
            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            return;
        }

        try {
            ADDR_PORT_REQUEST = "SERVER_ACTIVE";

            //Runnable r = new PingGRDS(packet, SocketGRDS, AddrGRDS, PortGRDS);
            //new Thread(r).start();

            //Cria um ServerSocket para comunicar com os clientes
            listeningSocket = new ServerSocket(SocketGRDS.getLocalPort());
            System.out.println("\nPorto de escuta: " + listeningSocket.getLocalPort());

            //Lança thread para verificar se a ligação com o GRDS se mantém
            Runnable check = new StartServer();
            new Thread(check).start();

            System.out.println("\nA aguardar por clientes...");

            while (true) {

                //Aceita cliente e adiciona-o à lista de sockets
                nextClient = listeningSocket.accept();
                listClientSockets.add(nextClient);

                System.out.println("\nEntrou um novo cliente.");

                //Lança thread para comunicar com o cliente
                new ThreadClient(nextClient, stmt, conn, this).start();

            }
        } catch (UnknownHostException e) {
            System.out.println("\nDestino desconhecido:\n\t" + e);
        } catch (NumberFormatException e) {
            System.out.println("\nO porto de escuta do servidor deve ser um inteiro positivo.");
        } catch (SocketTimeoutException e) {
            System.out.println("\nNão foi recebida qualquer resposta:\n\t"+e);
        } catch (SocketException e) {
            System.out.println("\nOcorreu um erro ao nível do socket UDP:\n\t" + e);
        } catch (IOException e) {
            System.out.println("\nOcorreu um erro no acesso ao socket:\n\t" + e);
        }
    }

    public File getFile() {
        return file;
    }

    public void setFile(File file) {
        StartServer.file = file;
    }

    public void run() {
        boolean sendFileNotif = false;
        int attempt = 0;
        int cycle = 0;
        String req;
        byte [] data;
        while(attempt != 3) {
            try {
                if (cycle % 10 == 0) request.setMessage("CHECK_GRDS");
                else request.setMessage("SERVER_ACTIVE");

                if (isNewFile()) {
                    ServerSocket fileReplicaSocket = new ServerSocket(0);

                    request.setFileSocketAddress(fileReplicaSocket.getInetAddress());
                    request.setFileSocketPort(fileReplicaSocket.getLocalPort());

                    request.setMessage("NEW_FILE");
                    System.out.println("file " + file.getUniqueName());
                    request.setF(file);

                    Runnable sendFileReplica = new SendFileReplica(request.getF().getName(), fileReplicaSocket, request.getF().getAffectedClients());
                    new Thread(sendFileReplica).start();
                    sendFileNotif = true;
                    notification = true;
                    setNewFile(false);
                }
                else if (isNotification()) {
                    if (sendFileNotif){
                        request.setF(file);
                        System.out.println("file " + file.getUniqueName());
                        System.out.println("ficheiroL " + request.getF().getUniqueName());
                        sendFileNotif = false;
                    }
                    request.setUsername(username);
                    request.setGroupName(groupName);
                    request.setUserToNotify(client);
                    request.setNotificationMessage(notificationMessage);
                    request.setNotificationType(notificationType);
                    request.setMessage("SEND_NOTIFICATION");


//                    for(ClientData client : clientsToNotify) {
//                        request.setUserToNotify(client);
//                    }

                    setNotification(false);
                }

                data = serialize(request);
                packet = new DatagramPacket(data, data.length, AddrGRDS, PortGRDS);
                SocketGRDS.send(packet);

                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                SocketGRDS.receive(packet);

                request = (Request) deserialize(packet.getData());
                if (request.getMessage().equalsIgnoreCase("NOTIFICATION_NEW_FILE")){

                    File fileInfo = request.getF();

                    System.out.println("nome file: " + fileInfo.getUniqueName());

                    Socket receiveFileSocket = new Socket(request.getFileSocketAddress(), request.getFileSocketPort());

                    Runnable r = new ReceiveFileReplica(fileInfo.getUniqueName(), receiveFileSocket);
                    new Thread(r).start();
                }
                else if (request.getMessage().equalsIgnoreCase("NEW_NOTIFICATION")) {

                    List<ClientData> toRemove = new ArrayList<>();

                    for (ClientData clie : request.getConnectedClients()) {
                        for (ClientData cli : request.getClientsToNotify()) {

                            if ((clie.getServerAddress().toString().equals(cli.getServerAddress().toString()) && clie.getPort() == cli.getPort())) {
                                new SendNotification(request, clie).start();
                                toRemove.add(cli);
                            }
                        }
                    }
                    request.getClientsToNotify().removeAll(toRemove);
                }
                else if (request.getMessage().equalsIgnoreCase("CONTINUE")) {
                    request.getClientsToNotify().clear();
                }

                attempt = 0;
            } catch (SocketTimeoutException e) {
                attempt++;
                System.out.println("\nNão foi possível estabelecer ligação com o GRDS. Tentativas restantes: " + (3 - attempt));
            } catch (IOException e) {
                e.printStackTrace();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
            cycle++;
        }
        if (attempt == 3){
            ObjectOutputStream out;
            System.out.println("\nA fechar o servidor...");

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SocketGRDS.close();
            System.exit(0);
        }
    }

    public void setNotification(boolean notification) {
        this.notification = notification;
    }

    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException,   ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }

    public ArrayList<ServerData> getServerList() {
        return serverList;
    }

    public int getServerSocketPort() {
        return listeningSocket.getLocalPort();
    }

    public String getServerSocketAddress() {
        System.out.println("aaaaaaaaaaa " + listeningSocket.getLocalSocketAddress().toString());
        return listeningSocket.getLocalSocketAddress().toString();
    }

    public void addUserToNotify(ClientData userAffectedByNotification) {
        client = userAffectedByNotification;
        clientsToNotify.add(userAffectedByNotification);
    }

    public void setNotificationMessage(String test_notification) {
        notificationMessage = test_notification;
    }

    public void setUsername(String username) {
        StartServer.username = username;
    }

    public void setGroupName(String groupName) { StartServer.groupName = groupName; }
}
