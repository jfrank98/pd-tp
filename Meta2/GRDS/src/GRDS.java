import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.*;
import java.util.ArrayList;

public class GRDS {

    private static final int MAX_SIZE = 1024;
    private static final String SERVER_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static final String CLIENT_REQUEST = "GET_ADDR_PORT_TCP";
    private int serverCount = 0;

    public static void main(String[] args) throws IOException {
        InetAddress AddrGRDS;
        int listeningPort;
        DatagramSocket socket = null;
        DatagramPacket packet;
        String response;
        ArrayList<Servidor> servers = new ArrayList<>();
        ByteArrayOutputStream baos;
        ObjectOutputStream oos;
        int server_index = 0;
        Servidor empty = new Servidor();
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

                if (response.equals(SERVER_REQUEST)) {
                    System.out.println("Sent message to server.");
                    packet.setData("msg received server".getBytes(), 0, "msg received server".length());
                    servers.add(new Servidor(packet.getAddress(), packet.getPort()));
                }
                else if (response.equals(CLIENT_REQUEST)){
                    System.out.println("Sent message to client.");
                    if (servers.size() > 0) {
                        if (server_index == servers.size()){
                            server_index = 0;
                        }
                        oos.writeUnshared(servers.get(server_index));
                        byte[] data = baos.toByteArray();
                        packet.setData(data, 0, data.length);
                    }
                    else {
                        oos.writeUnshared(empty);
                        byte[] data = baos.toByteArray();
                        packet.setData(data, 0, data.length);
                    }
                }

                InetAddress clientAddress = packet.getAddress();
                int clientPort = packet.getPort();


                socket.send(packet);

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
