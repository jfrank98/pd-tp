import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.*;

public class Servidor implements Runnable{

    private static final int MAX_SIZE = 1024;
    private static String ADDR_PORT_REQUEST = "SERVER_GET_ADDR_PORT_TCP";
    private static DatagramSocket s;
    private static DatagramPacket p;
    private static InetAddress addr;
    private static int port;
    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost:3306/pd_chat";
    static final String USER = "root";
    static final String PASS = "rootpw";
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
        boolean connected = false;
        Statement stmt;
        Connection conn;
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
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException ex) {
            System.out.println(ex);
        }

        try {
            conn = DriverManager.getConnection(DB_URL, USER, PASS);
            stmt = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
        }catch (SQLException e){
            e.printStackTrace();
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
            SocketGRDS = new DatagramSocket();
            SocketGRDS.setSoTimeout(5000);

            listeningSocket = new ServerSocket(SocketGRDS.getLocalPort());

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

                System.out.println("Waiting for clients...");
                    //Começa a aceitar clientes
                    nextClient = listeningSocket.accept();

                    new ThreadClient(nextClient, stmt, conn).start();


//                oout.writeObject(calendar);
//                oout.flush();

            }
        } catch (UnknownHostException e) {
            System.out.println("Destino desconhecido:\n\t" + e);
        } catch (NumberFormatException e) {
            System.out.println("O porto do servidor deve ser um inteiro positivo.");
        } catch (SocketTimeoutException e) {
            System.out.println("Nao foi recebida qualquer resposta:\n\t"+e);
            attempt++;
        } catch (SocketException e) {
            System.out.println("Ocorreu um erro ao nivel do socket UDP:\n\t" + e);
        } catch (IOException e) {
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
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
