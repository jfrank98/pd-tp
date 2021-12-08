import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.*;

public class Servidor {

    private static final int MAX_SIZE = 1024;
    private static final String ADDR_PORT_REQUEST = "SERVER_GET_ADDR_PORT_TCP";

    public static void main(String[] args) {
        InetAddress AddrGRDS;
        int PortGRDS;
        DatagramSocket SocketGRDS = null;
        DatagramPacket packet;
        String responseGRDS;
        InetAddress SGBDaddress;
        boolean connected = false;

        int attempt = 0;
        if (args.length < 1 || args.length > 3 || args.length == 2){
            System.out.println("Sintaxe: java Servidor SGBDaddress GRDSaddress(opcional) GRDSport(opcional) ");
            return;
        }
        while(attempt != 3 || connected) {
            try {
                if (args.length == 1) {
                    AddrGRDS = InetAddress.getByName("230.30.30.30");
                    PortGRDS = 3030;
                } else {
                    AddrGRDS = InetAddress.getByName(args[1]);
                    PortGRDS = Integer.parseInt(args[2]);
                }
                SGBDaddress = InetAddress.getByName(args[0]);
                SocketGRDS = new DatagramSocket();
                SocketGRDS.setSoTimeout(5000);
                packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
                SocketGRDS.send(packet);

                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE, AddrGRDS, PortGRDS);
                SocketGRDS.receive(packet);

                responseGRDS = new String(packet.getData(), 0, packet.getLength());
                //response = packet.getData().toString();
                String[] addr_port_GRDS = responseGRDS.split(",");
                for (String s : addr_port_GRDS) {
                    System.out.println(s);
                }

                connected = true;

            } catch (UnknownHostException e) {
                System.out.println("Destino desconhecido:\n\t" + e);
            } catch (NumberFormatException e) {
                System.out.println("O porto do servidor deve ser um inteiro positivo.");
            } catch (SocketTimeoutException e) {
                attempt++;
                System.out.println("Nao foi recebida qualquer resposta. Tentativas restantes: " + (3-attempt));
            } catch (SocketException e) {
                System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t" + e);
            } catch (IOException e) {
                System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
            } finally {
                if (SocketGRDS != null) {
                    SocketGRDS.close();
                }
            }

            startServer();

        }
    }

    public static void startServer() {
        int listeningPort;

        ServerSocket listeningSocket;
        Socket nextClient = null;
        PrintStream pout;
        BufferedReader bin;
        String receivedMsg, timeMsg, hostname;

        try{

            listeningSocket = new ServerSocket(0);

            listeningPort = listeningSocket.getLocalPort();

            System.out.println("TCP Server iniciado...");

            while(true){
                nextClient = listeningSocket.accept();

                bin = new BufferedReader(new InputStreamReader(nextClient.getInputStream()));
                pout = new PrintStream(nextClient.getOutputStream());

                receivedMsg = bin.readLine();

                System.out.println(receivedMsg);

                pout.println();
                pout.flush();

            }

        }catch(NumberFormatException e){
            System.out.println("O porto de escuta deve ser um inteiro positivo.");
        }catch(SocketException e){
            System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t"+e);
        }catch(IOException e){
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t"+e);
        }finally{
            if(nextClient != null){
                try {
                    nextClient.close();
                } catch(IOException e) {
                    System.out.println("Erro ao fechar socket de cliente.");
                }
            }
        }
    }
}
