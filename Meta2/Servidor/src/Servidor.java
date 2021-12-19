import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.*;
import java.util.*;

public class Servidor implements Runnable{

    private static final int MAX_SIZE = 1024;
    private static String ADDR_PORT_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static DatagramSocket s;
    private static DatagramPacket p;
    private static InetAddress AddrGRDS = null;
    private static int PortGRDS = 0;
    private static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    private static final String DB_URL = "jdbc:mysql://localhost:3306/pd_chat";
    private static final String USER = "root";
    private static final String PASS = "root";
    private static ArrayList<Socket> listClientSockets = new ArrayList<>();
    private static ServerSocket listeningSocket;
    private static String command = "";
    private static boolean grdsClosed = false;
    private static DatagramSocket SocketGRDS = null;
    private static DatagramPacket packet = null;

    public Servidor() {

    }

    public static void main(String[] args) {


        boolean connected = false;
        Statement stmt;
        Connection conn;

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
            Class.forName(JDBC_DRIVER);

            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        }catch (SQLException | ClassNotFoundException e){
            e.printStackTrace();
            return;
        }

        try {
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
                //Envia mensagem ao GRDS

                packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
                SocketGRDS.send(packet);
                packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE);
                SocketGRDS.receive(packet);
                connected = true;
            } catch (SocketTimeoutException e) {
                attempt++;
                System.out.println("Nao foi possivel conectar ao GRDS. Tentativas restantes: " + (3 - attempt));
            } catch (UnknownHostException e) {
                System.out.println("Endereço desconhecido.");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        if (!connected){
            System.out.println("A desligar servidor...");
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

            listeningSocket = new ServerSocket(SocketGRDS.getLocalPort());
            System.out.println("Listening on port " + listeningSocket.getLocalPort());

            Runnable check = new Servidor();
            new Thread(check).start();

            while (true) {

                    System.out.println("Waiting for clients...");
                    //Começa a aceitar clientes
                    nextClient = listeningSocket.accept();
                    listClientSockets.add(nextClient);
                    new ThreadClient(nextClient, stmt, conn).start();

            }
        } catch (UnknownHostException e) {
            System.out.println("Destino desconhecido:\n\t" + e);
        } catch (NumberFormatException e) {
            System.out.println("O porto do servidor deve ser um inteiro positivo.");
        } catch (SocketTimeoutException e) {
            System.out.println("Nao foi recebida qualquer resposta:\n\t"+e);
        } catch (SocketException e) {
            System.out.println("Ocorreu um erro ao nivel do socket UDaasdP:\n\t" + e);
        } catch (IOException e) {
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
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
                attempt = 0;
            } catch (SocketTimeoutException e) {
                attempt++;
                System.out.println("Nao foi possivel conectar ao GRDS. Tentativas restantes: " + (3 - attempt));
            } catch (IOException e) {
                e.printStackTrace();
            }
            cycle++;
        }
        if (attempt == 3){
            ObjectOutputStream out;
            System.out.println("A fechar servidor...");

            try {
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            SocketGRDS.close();
            System.exit(0);
        }
    }

//    public void run() {
//        //System.out.println("Comando: ");
//        Scanner sc = new Scanner(System.in);
//        while(true){
//
//            command = sc.nextLine();
//
//            ObjectOutputStream out;
//
//            if (command.equalsIgnoreCase("Y") || grdsClosed) {
//                if (listClientSockets.size() > 0) {
//                    for (Socket cli : listClientSockets) {
//                        try {
//                            out = new ObjectOutputStream(cli.getOutputStream());
//                            if (grdsClosed){
//                                out.writeUnshared("GRDS_CLOSED");
//                            }
//                            else {
//                                out.writeUnshared("SERVER_OFF");
//                            }
//                            Thread.sleep(2000);
//                            listeningSocket.close();
//                        } catch (IOException | InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                return;
//            }
//            else {
//                if (command == "") continue;
//                System.out.println("Comando: ");
//            }
//        }
//    }

}
