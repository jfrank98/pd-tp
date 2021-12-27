import java.io.*;
import java.net.*;
import java.util.*;

public class GRDS implements Runnable{

    private static final int MAX_SIZE = 1024;
    private static final String SERVER_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static final String SERVER_GRDS_CHECK = "CHECK_GRDS";
    private static String SERVER_CHECK = "SERVER_ACTIVE";
    private static final String CLIENT_REQUEST = "GET_ADDR_PORT_TCP";
    private static List<ServerData> servers = new ArrayList<>();
    private static int server_index = 0;

    public GRDS(){
    }

    public static void main(String[] args)  {
        int listeningPort;
        DatagramSocket socket = null;
        DatagramPacket packet;
        String response;
        ByteArrayOutputStream baos;
        ObjectOutputStream oos;
        boolean firstServer = true;
        int id = 1;
        ServerData empty = new ServerData();
        ArrayList<ServerData> serverList;

        //Verifica se recebeu os argumentos necessários: porto de escuta
        if(args.length != 1){
            System.out.println("Sintaxe: java GRDS listeningPort");
            return;
        }

        try{
            listeningPort = Integer.parseInt(args[0]);
            System.out.println("\nPorto de escuta: " + listeningPort);

            //Cria um DatagramSocket para comunicar com os servidores e clientes
            socket = new DatagramSocket(listeningPort);

            while(true) {

                //Cria e recebe um DatagramPacket
                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                socket.receive(packet);

                baos = new ByteArrayOutputStream();
                oos = new ObjectOutputStream(baos);

                response = new String(packet.getData(), 0, packet.getLength());

                if (!response.equalsIgnoreCase(SERVER_CHECK) && !response.equalsIgnoreCase(SERVER_GRDS_CHECK)) {
                    System.out.println("\nPedido: " + response);
                }

                if (response.equals(SERVER_REQUEST)) {
                    System.out.println("\nMensagem do servidor com endereço IP " + packet.getAddress() + " e porto de escuta " + packet.getPort());
                    ServerData newServer = new ServerData(packet.getAddress(), packet.getPort());
                    newServer.setTimeSinceLastMsg(System.currentTimeMillis()/1000);
                    newServer.setId(id);
                    newServer.setOnline(true);
                    id++;

                    synchronized (servers) {
                        servers.add(newServer);
                    }
                    if (firstServer) {
                        Runnable r = new GRDS();
                        new Thread(r).start();
                        firstServer = false;
                    }
                    packet.setData(SERVER_REQUEST.getBytes(), 0, SERVER_REQUEST.length());
                    socket.send(packet);
                }
                else if (response.equals(SERVER_CHECK)) {
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
                    ArrayList<Object> array = new ArrayList<>();
                    array.add(serverList);
                    data = serialize(array);
                    packet.setData(data, 0, data.length);
                    socket.send(packet);
                }
                else if (response.equals(SERVER_GRDS_CHECK)) {
                    byte [] data;
                    synchronized (servers) {
                        serverList = new ArrayList<>(servers);
                    }
                    ArrayList<Object> array = new ArrayList<>();
                    array.add(serverList);
                    data = serialize(array);
                    packet.setData(data, 0, data.length);
                    socket.send(packet);
                }
                else if (response.equals(CLIENT_REQUEST)){
                    boolean sent = false;
                    int nOffline = 0;
                    System.out.println("\nDados do servidor enviados ao cliente:");
                    synchronized (servers) {
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
                                    oos.writeUnshared(servers.get(server_index));
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

                //Verifica tempo desde a última resposta de cada servidor,
                // se passarem 60 segundos é eliminado do ArrayList

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
        }finally{
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

                for (final Iterator it = servers.listIterator(); it.hasNext(); ) {

                    double seconds = 2;
                    //System.out.println("Time since last msg: " + ((System.currentTimeMillis()/1000) - s.getTimeSinceLastMsg()));
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

                /*for (ServerData s : servers) {
                    double seconds = 2;
                    //System.out.println("Time since last msg: " + ((System.currentTimeMillis()/1000) - s.getTimeSinceLastMsg()));

                    if ((System.currentTimeMillis() / 1000) - s.getTimeSinceLastMsg() >= seconds) {
                        System.out.println("\nO servidor com id " + s.getId() + " não respondeu " + (s.getPeriods() + 1) + " vezes.");
                        s.setPeriods(s.getPeriods() + 1);
                        s.setTimeSinceLastMsg(System.currentTimeMillis() / 1000);
                        s.setOnline(false);

                        if (s.getPeriods() == 3) {
                            System.out.println("\nO servidor com id " + s.getId() + " foi removido.");
                            servers.remove(s);
                            server_index--;
                            if (servers.size() == 0) break;
                        }
                    }
                }*/
            }
        }
    }
}
