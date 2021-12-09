import java.io.*;
import java.net.*;

public class Servidor implements Runnable{

    private static final int MAX_SIZE = 1024;
    private static String ADDR_PORT_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static DatagramSocket s;
    private static DatagramPacket p;
    private static InetAddress addr;
    private static int port;
    public Servidor(DatagramPacket packet, DatagramSocket socket, InetAddress addr, int port){
        s = socket;
        p = packet;
        this.addr = addr;
        this.port = port;
    }

    public static void main(String[] args) {
        InetAddress AddrGRDS;
        int PortGRDS;
        DatagramSocket SocketGRDS = null;
        DatagramPacket packet;
        String responseGRDS;
        InetAddress SGBDaddress;
        boolean connected = false;

        int listeningPort;

        ServerSocket listeningSocket;
        Socket nextClient = null;
        ObjectOutputStream oout;
        ObjectInputStream oin;
        String receivedMsg;

        int attempt = 0;
        if (args.length < 1 || args.length > 3 || args.length == 2) {
            System.out.println("Sintaxe: java Servidor SGBDaddress GRDSaddress(opcional) GRDSport(opcional) ");
            return;
        }
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

            listeningSocket = new ServerSocket(SocketGRDS.getLocalPort());

            listeningPort = listeningSocket.getLocalPort();

            System.out.println("Listening on port " + listeningSocket.getLocalPort());

            while (true && attempt != 3) {

                //Envia mensagem ao GRDS
                if (!connected) {
                    try {
                        packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
                        SocketGRDS.send(packet);
                        connected = true;
                        Runnable r = new Servidor(packet, SocketGRDS, AddrGRDS, PortGRDS);
                        new Thread(r).start();
                    } catch (SocketTimeoutException e) {
                        attempt++;
                        System.out.println("Nao foi recebida qualquer resposta. Tentativas restantes: " + (3 - attempt));
                    }
                }

                ADDR_PORT_REQUEST = "SERVER_ACTIVE";


                //Começa a aceitar clientes
                nextClient = listeningSocket.accept();

                oout = new ObjectOutputStream(nextClient.getOutputStream());
                oin = new ObjectInputStream(nextClient.getInputStream());

                receivedMsg = (String) oin.readObject();

                if(receivedMsg == null){ //EOF
                    continue; //to next client request
                }

                System.out.println("Recebido \"" + receivedMsg.trim() + "\" de " +
                        nextClient.getInetAddress().getHostAddress() + ":" +
                        nextClient.getPort());

//                oout.writeObject(calendar);
//                oout.flush();
            }
        } catch (UnknownHostException e) {
            System.out.println("Destino desconhecido:\n\t" + e);
        } catch (NumberFormatException e) {
            System.out.println("O porto do servidor deve ser um inteiro positivo.");
        } catch (SocketTimeoutException e) {
            System.out.println("Nao foi recebida qualquer resposta:\n\t"+e);
        } catch (SocketException e) {
            System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t" + e);
        } catch (IOException e) {
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        } finally {
            if (SocketGRDS != null) {
                SocketGRDS.close();
            }
            if (nextClient != null) {
                try {
                    nextClient.close();
                } catch (IOException e) {
                    System.out.println("Erro ao fechar socket de cliente.");
                }
            }
        }
    }

    public void run() {
        while(true) {
            try {
                p = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), addr, port);
                s.send(p);

            } catch (SocketTimeoutException e) {

            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
