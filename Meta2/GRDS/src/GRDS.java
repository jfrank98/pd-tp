import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GRDS implements Runnable{

    private static final int MAX_SIZE = 1024;
    private static final String SERVER_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static final String SERVER_GRDS_CHECK = "CHECK_GRDS";
    private static String SERVER_CHECK = "SERVER_ACTIVE";
    private static final String CLIENT_REQUEST = "GET_ADDR_PORT_TCP";
    private static List<Servidor> servers = Collections.synchronizedList(new ArrayList<>());
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
        Servidor empty = new Servidor();

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
                    Servidor newServer = new Servidor(packet.getAddress(), packet.getPort());
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
                    synchronized (servers) {
                        for (Servidor s : servers) {
                            if (packet.getAddress().equals(s.getServerAddress()) && packet.getPort() == s.getListeningPort()) {
                                s.setPeriods(0);
                                s.setTimeSinceLastMsg(System.currentTimeMillis() / 1000);
                                s.setOnline(true);
                            }
                        }
                    }
                    packet.setData(SERVER_REQUEST.getBytes(), 0, SERVER_REQUEST.length());
                    socket.send(packet);
                }
                else if (response.equals(SERVER_GRDS_CHECK)) {
                    packet.setData(SERVER_REQUEST.getBytes(), 0, SERVER_REQUEST.length());
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

    public void run() {
        while (true) {
            if (servers.size() == 0) continue;
            for (Servidor s : servers) {
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
            }
        }
    }
}
