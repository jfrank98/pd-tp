import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

public class GRDS {

    private static final int MAX_SIZE = 1024;
    private static final String SERVER_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static String SERVER_CHECK = "SERVER_ACTIVE";
    private static final String CLIENT_REQUEST = "GET_ADDR_PORT_TCP";

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
        ArrayList<Servidor> servers = new ArrayList<>();

        if(args.length != 1){
            System.out.println("Sintaxe: java GRDS listeningPort");
            return;
        }

        try{
            baos = new ByteArrayOutputStream();
            oos = new ObjectOutputStream(baos);

            listeningPort = Integer.parseInt(args[0]);
            System.out.println("Listening on port " + listeningPort);

            socket = new DatagramSocket(listeningPort);
            while(true) {

                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                socket.receive(packet);

                response = new String(packet.getData(), 0, packet.getLength());

                if (!response.equalsIgnoreCase("SERVER_ACTIVE")) {
                    System.out.println(response);
                }

                if (response.equals(SERVER_REQUEST)) {
                    System.out.println("Message from server with address: " + packet.getAddress() + ", port: " + packet.getPort());
                    Servidor newServer = new Servidor(packet.getAddress(), packet.getPort());
                    newServer.setTimeSinceLastMsg(System.currentTimeMillis()/1000);
                    newServer.setId(id);
                    newServer.setOnline(true);
                    id++;
                    servers.add(newServer);
                }

                else if (response.equals(SERVER_CHECK)) {
                    for (Servidor s : servers) {
                        if (packet.getAddress().equals(s.getServerAddress()) && packet.getPort() == s.getListeningPort()) {
                            s.setPeriods(0);
                            s.setTimeSinceLastMsg(System.currentTimeMillis() / 1000);
                            s.setOnline(true);
                        }
                    }
                }

                else if (response.equals(CLIENT_REQUEST)){
                    System.out.println("Sent server details to client.");
                    if (servers.size() > 0) {
                        if (server_index >= servers.size() || server_index < 0) {
                            server_index = 0;
                        }
                        if (servers.size() == 1 && servers.get(0).isOnline()){
                            oos.writeUnshared(servers.get(server_index));
                            byte[] data = baos.toByteArray();
                            packet.setData(data, 0, data.length);
                        }
                        else if(servers.get(server_index).isOnline()) {
                            oos.writeUnshared(servers.get(server_index));
                            byte[] data = baos.toByteArray();
                            packet.setData(data, 0, data.length);
                            server_index++;
                        }
                        else if ((server_index + 1) >= servers.size()) server_index = 0;
                    } else {
                        oos.writeUnshared(empty);
                        byte[] data = baos.toByteArray();
                        packet.setData(data, 0, data.length);
                    }
                    socket.send(packet);
                }



                //Verifica tempo desde a última resposta de cada servidor,
                // se passarem 60 segundos é eliminado do ArrayList

                if (servers.size() == 0) continue;
                for (Servidor s : servers) {
                    double seconds = 2;
                    //System.out.println("Time since last msg: " + ((System.currentTimeMillis()/1000) - s.getTimeSinceLastMsg()));
                    if ((System.currentTimeMillis() / 1000) - s.getTimeSinceLastMsg() >= seconds) {
                        System.out.println("Server id " + s.getId() + " has not answered " + (s.getPeriods() + 1) + " times.");
                        s.setPeriods(s.getPeriods() + 1);
                        s.setTimeSinceLastMsg(System.currentTimeMillis() / 1000);
                        s.setOnline(false);
                        if (s.getPeriods() == 3) {
                            System.out.println("Server id " + s.getId() + " has been removed.");
                            servers.remove(s);
                            server_index--;
                            if (servers.size() == 0) break;
                        }
                    }
                }
            }
        }catch(UnknownHostException e){
            System.out.println("Destino desconhecido:\n\t"+e);
        }catch(NumberFormatException e){
            System.out.println("O porto do servidor deve ser um inteiro positivo.");
        }catch(SocketTimeoutException e){
            System.out.println("Nao foi recebida qualquer resposta:\n\t"+e);
        }catch(SocketException e){
            System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
        }finally{
            if(socket != null){
                socket.close();
            }
        }
    }

}
