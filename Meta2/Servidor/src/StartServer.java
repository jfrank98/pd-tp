import java.io.*;
import java.net.*;
import java.sql.*;
import java.util.ArrayList;

public class StartServer implements Runnable {
    private static final int MAX_SIZE = 4096;
    private static String ADDR_PORT_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static DatagramSocket s;
    private static DatagramPacket p;
    private static InetAddress AddrGRDS = null;
    private static int PortGRDS = 0;
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pd_chat";
    private static final String USER = "root";
    private static final String PASS = "rootpw";
    private static ArrayList<Socket> listClientSockets = new ArrayList<>();
    private static ServerSocket listeningSocket;
    private static String command = "";
    private static boolean grdsClosed = false;
    private static DatagramSocket SocketGRDS = null;
    private static DatagramPacket packet = null;
    private static ArrayList<ServerData> serverList = new ArrayList<>();
    public StartServer(StartServer startServer) {

    }

    public StartServer() {

    }

    public void startServer(String [] args) {
        boolean connected = false;
        Statement stmt;
        Connection conn;

        Socket nextClient = null;
        ObjectOutputStream oout;
        ObjectInputStream oin;
        String receivedMsg;
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
            SocketGRDS = new DatagramSocket();
            SocketGRDS.setSoTimeout(3000);
        } catch(SocketException e) {
            e.printStackTrace();
        }

        while (attempt != 3 && !connected) {
            try {
                if (args.length == 1) {
                    AddrGRDS = InetAddress.getByName("230.30.30.30");
                    PortGRDS = 3030;
                } else {
                    AddrGRDS = InetAddress.getByName(args[1]);
                    PortGRDS = Integer.parseInt(args[2]);
                }

                //Cria um DatagramPacket e envia-o ao GRDS através do DatagramSocket criado anteriormente
                packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
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

    public void run() {
        int attempt = 0;
        int cycle = 0;
        String req = "CHECK_GRDS";

        while(attempt != 3) {
            try {
                if (cycle % 10 == 0) req = "CHECK_GRDS";
                else req = "SERVER_ACTIVE";

                packet = new DatagramPacket(req.getBytes(), req.length(), AddrGRDS, PortGRDS);
                SocketGRDS.send(packet);

                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                SocketGRDS.receive(packet);

                ArrayList<Object> obj = (ArrayList) deserialize(packet.getData());
                serverList = (ArrayList<ServerData>) obj.get(0);

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
}
