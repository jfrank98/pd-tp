import java.io.*;
import java.net.*;
import java.util.*;

public class GRDS implements Runnable{

    private static final int MAX_SIZE = 4096;
    private static final String SERVER_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static final String SERVER_GRDS_CHECK = "CHECK_GRDS";
    private static String SERVER_CHECK = "SERVER_ACTIVE";
    private static final String CLIENT_REQUEST = "GET_ADDR_PORT_TCP";
    private static List<ServerData> servers = new ArrayList<>();
    private static List<ClientData> allClients = new ArrayList<>();
    private static int server_index = 0;

    public GRDS(){
    }

    public static void main(String[] args)  {
        int listeningPort;
        MulticastSocket socket = null;
        DatagramPacket packet;
        ByteArrayOutputStream baos;
        ObjectOutputStream oos;
        boolean firstServer = true;
        int serverID = 1, clientID = 1;
        ServerData empty = new ServerData();
        ArrayList<ServerData> serverList;
        Request req, notifNewFile = null, notificationRequest = null;
        List<ServerData> toNotifyNewFileServers = new ArrayList<>();
        List<ServerData> newNotificationServers = new ArrayList<>();
        List<ClientData> clientsToNotify = new ArrayList<>();

        //Verifica se recebeu os argumentos necessários: porto de escuta
        if(args.length != 1){
            System.out.println("Sintaxe: java GRDS listeningPort");
            return;
        }

        try{
            listeningPort = Integer.parseInt(args[0]);
            System.out.println("\nPorto de escuta: " + listeningPort);

            //Cria um DatagramSocket para comunicar com os servidores e clientes
            //socket = new DatagramSocket(listeningPort);
            socket = new MulticastSocket(listeningPort);
            String group = "224.69.69.69";

            socket.joinGroup(InetAddress.getByName(group));

            while(true) {

                //Cria e recebe um DatagramPacket
                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                socket.receive(packet);

                baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);
                req = (Request) deserialize(packet.getData());

                if (!req.getMessage().equalsIgnoreCase(SERVER_CHECK) && !req.getMessage().equalsIgnoreCase(SERVER_GRDS_CHECK)) {
                    System.out.println("\nPedido: " + req.getMessage());
                }

                if (req.getMessage().equals(SERVER_REQUEST)) {
                    System.out.println("\nMensagem do servidor com endereço IP " + packet.getAddress() + " e porto de escuta " + packet.getPort());
                    ServerData newServer = new ServerData(packet.getAddress(), packet.getPort());
                    newServer.setTimeSinceLastMsg(System.currentTimeMillis()/1000);
                    newServer.setId(serverID);
                    newServer.setOnline(true);
                    serverID++;

                    synchronized (servers) {
                        servers.add(newServer);
                    }
                    if (firstServer) {
                        Runnable r = new GRDS();
                        new Thread(r).start();
                        firstServer = false;
                    }
                    req.setMessage(SERVER_REQUEST);

                    byte [] data = serialize(req);

                    packet.setData(data, 0, data.length);
                    socket.send(packet);
                }
                else if (req.getMessage().equals(SERVER_CHECK)) {
                    byte [] data;

                    synchronized (servers) {
                        for (ServerData s : servers) {
                            if (packet.getAddress().equals(s.getServerAddress()) && packet.getPort() == s.getListeningPort()) {
                                s.setPeriods(0);
                                s.setTimeSinceLastMsg(System.currentTimeMillis() / 1000);
                                s.setOnline(true);
                            }
                        }
                        serverList = new ArrayList<>(servers);
                    }
                    /*ArrayList<Object> array = new ArrayList<>();
                    array.add(serverList);*/
                    data = serialize(req);
                    packet.setData(data, 0, data.length);
                    socket.send(packet);
                }
                else if (req.getMessage().equals(SERVER_GRDS_CHECK)) {
                    byte [] data;
                    synchronized (servers) {
                        serverList = new ArrayList<>(servers);
                    }
                    ServerData removeFromNewFileList = null, removeFromNewNotifList = null;
                    boolean notifiednewfile = false, notified = false;

                    //Verifica se o packet recebido é de um servidor ao qual tem de ser enviada uma notificação
                    for (ServerData s : newNotificationServers) {
                        if (s.getListeningPort() == packet.getPort()){
                            System.out.println("new notif");
                            removeFromNewNotifList = s;
                            notified = true;
                        }
                    }

                    for (ServerData s : toNotifyNewFileServers){
                        //System.out.println("packet: " + s.getListeningPort() + " tonotify: " + packet.getPort());
                        if (s.getListeningPort() == packet.getPort()){
                            System.out.println("new file");
                            removeFromNewFileList = s;
                            notifiednewfile = true;
                        }
                    }
                    if (notified){
                        notificationRequest.setMessage("NEW_NOTIFICATION");
                        newNotificationServers.remove(removeFromNewNotifList);
                        System.out.println("asfmsfgdim " + notificationRequest.getClientsToNotify().size());
                        data = serialize(notificationRequest);
                    }
                    else if (notifiednewfile){
                        notifNewFile.setMessage("NOTIFICATION_NEW_FILE");
                        toNotifyNewFileServers.remove(removeFromNewFileList);
                        data = serialize(notifNewFile);
                    } else { data = serialize(req); }

                    packet.setData(data, 0, data.length);
                    socket.send(packet);
                }
                else if (req.getMessage().equals("UPDATE_CLIENT_LIST")){
                    System.out.println("clients size: " + req.getConnectedClients().size());
                    for (ClientData cli : req.getConnectedClients()) {
                        allClients.add(cli);
                    }
                }
                else if (req.getMessage().equals(CLIENT_REQUEST)){
                    boolean sent = false;
                    int nOffline = 0;

                    System.out.println("\nDados do servidor enviados ao cliente:");
                    synchronized (servers) {
                        ServerData s;
                        if (servers.size() > 0) {
                            while (!sent) {
                                if (nOffline == servers.size()) {
                                    break;
                                }
                                if (server_index >= servers.size() || server_index < 0) {
                                    server_index = 0;
                                }
                                if (servers.get(server_index).isOnline()) {
                                    System.out.println("ID - " + server_index);
                                    System.out.println("Porto de escuta - " + servers.get(server_index).getListeningPort());
                                    s = servers.get(server_index);
                                    oos.writeUnshared(s);
                                    byte[] data = baos.toByteArray();
                                    packet.setData(data, 0, data.length);

                                    sent = true;
                                } else {
                                    nOffline++;
                                }
                                server_index++;
                            }
                            if (!sent) {
                                oos.writeUnshared(empty);
                                byte[] data = baos.toByteArray();
                                packet.setData(data, 0, data.length);
                            }
                        } else {
                            oos.writeUnshared(empty);
                            byte[] data = baos.toByteArray();
                            packet.setData(data, 0, data.length);
                        }
                    }
                    socket.send(packet);
                }
                else if (req.getMessage().equalsIgnoreCase("SEND_NOTIFICATION")) {
                    req.setMessage("CONTINUE");

                    System.out.println("sizeallclients " + allClients.size());

                    req.getConnectedClients().clear();

                    for (ClientData cli : allClients) {
                        req.addConnectedClient(cli);
                    }

                    byte [] data = serialize(req);
                    packet.setData(data, 0, data.length);

                    boolean added = false;
                    System.out.println("ok so far so good");
                    for (ClientData cli : req.getClientsToNotify()) {
                        System.out.println("cli notif: " + cli.getPort());
                        for (ServerData s : newNotificationServers) {
                            System.out.println("packet: " + s.getListeningPort() + " tonotify: " + cli.getPort());
                            if (cli.getServerAddress() == s.getServerAddress() && cli.getPort() == s.getListeningPort()) {
                                added = true;
                                break;
                            }
                        }
                        if (!added)
                            newNotificationServers.add(new ServerData(cli.getServerAddress(), cli.getPort()));
                        added = false;
                    }

                    notificationRequest = req;

                    socket.send(packet);
                }
                else if (req.getMessage().equalsIgnoreCase("NEW_FILE")){
                    req.setMessage("CONTINUE");

                    byte [] data = serialize(req);
                    packet.setData(data, 0, data.length);

                    boolean added = false;

                    for (ClientData cli : req.getF().getAffectedClients()) {
                        for (ServerData s : toNotifyNewFileServers) {
                            System.out.println("packet: " + s.getListeningPort() + " tonotify: " + cli.getPort());
                            if (cli.getServerAddress() == s.getServerAddress() && cli.getPort() == s.getListeningPort()) {
                                added = true;
                                break;
                            }
                        }
                        if (!added)
                            toNotifyNewFileServers.add(new ServerData(cli.getServerAddress(), cli.getPort()));
                        added = false;
                    }

                    notifNewFile = req;

                    socket.send(packet);
                }
            }
        }catch(UnknownHostException e){
            System.out.println("\nDestino desconhecido:\n\t" + e);
        }catch(NumberFormatException e){
            System.out.println("\nO porto do servidor deve ser um inteiro positivo.");
        }catch(SocketTimeoutException e){
            System.out.println("\nNão foi recebida qualquer resposta:\n\t" + e);
        }catch(SocketException e){
            System.out.println("\nOcorreu um erro ao nível do socket UDP:\n\t" + e);
        }catch(IOException e){
            System.out.println("\nOcorreu um erro no acesso ao socket:\n\t" + e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally{
            if(socket != null){
                socket.close();
            }
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

    public void run() {
        while (true) {
            synchronized (servers) {
                if (servers.size() == 0) continue;

                //Verifica tempo desde a última resposta de cada servidor,
                // se passarem 60 segundos é eliminado do ArrayList
                for (final Iterator it = servers.listIterator(); it.hasNext(); ) {
                    double seconds = 2;
                    ServerData s = (ServerData) it.next();

                    if ((System.currentTimeMillis() / 1000) - s.getTimeSinceLastMsg() >= seconds) {
                        System.out.println("\nO servidor com id " + s.getId() + " não respondeu " + (s.getPeriods() + 1) + " vezes.");
                        s.setPeriods(s.getPeriods() + 1);
                        s.setTimeSinceLastMsg(System.currentTimeMillis() / 1000);
                        s.setOnline(false);

                        if (s.getPeriods() == 3) {
                            System.out.println("\nO servidor com id " + s.getId() + " foi removido.");
                            it.remove();
                            server_index--;
                            if (!it.hasNext()) break;
                        }
                    }
                }
            }
        }
    }
}
