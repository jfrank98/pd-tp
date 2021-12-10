import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Cliente {
    public static final int MAX_SIZE = 1024;
    public static final String ADDR_PORT_REQUEST = "GET_ADDR_PORT_TCP";
    public static final String SERVER_REQUEST = "SERVER_REQUEST";
    public static void main(String args[]) {

        InetAddress AddrGRDS, serverAddress = null;
        int PortGRDS, serverPort;
        DatagramSocket SocketGRDS = null;
        Socket socket = null;
        DatagramPacket packet;
        Servidor server;
        ByteArrayInputStream bin;
        ObjectInputStream oin, oinS;
        ObjectOutputStream oout;
        boolean connected = false;
        String ans;
        Request request = new Request();

        if (args.length != 2) {
            System.out.println("Sintaxe: java Cliente serverAddress serverUdpPort");
            return;
        }

        try {

            /////////Pede dados de um servidor ativo///////////

            SocketGRDS = new DatagramSocket();
            AddrGRDS = InetAddress.getByName(args[0]);
            PortGRDS = Integer.parseInt(args[1]);
            System.out.println("bruh");
            packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
            SocketGRDS.send(packet);
            System.out.println("bruh");
            packet.setData(new byte[MAX_SIZE], 0, MAX_SIZE);
            SocketGRDS.receive(packet);
            System.out.println("bruh");
            bin = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            oin = new ObjectInputStream(bin);
            System.out.println("bruh");
            server = (Servidor) oin.readObject();

            SocketGRDS.close();

            System.out.println("server hostname: " + server.getServerAddress().toString());
            System.out.println("server port: " + server.getListeningPort());



        } catch (UnknownHostException e) {
            System.out.println("Destino desconhecido:\n\t" + e);
            return;
        } catch (NumberFormatException e) {
            System.out.println("O porto do servidor deve ser um inteiro positivo.");
            return;
        } catch (SocketTimeoutException e) {
            System.out.println("Nao foi recebida qualquer resposta:\n\t" + e);
            return;
        } catch (SocketException e) {
            System.out.println("Ocorreu um erro ao nivel do socket:\n\t" + e);
            return;
        } catch (IOException e) {
            System.out.println("Ocorreu um erro no acesso ao socket:\n\t" + e);
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        } finally {
            if (SocketGRDS != null) {
                SocketGRDS.close();
            }
        }

        Scanner sc = new Scanner(System.in);
        ArrayList<String> credentials = new ArrayList<>(3);
        int option = 0;
        try {

            ///////////////Tenta conectar a servidor///////////////

            serverAddress = server.getServerAddress();
            serverPort = server.getListeningPort();

            socket = new Socket(serverAddress, serverPort);
            socket.setSoTimeout(5000);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oinS = new ObjectInputStream(socket.getInputStream());

            request.setRequest(SERVER_REQUEST);
            oout.writeUnshared(request);
            oout.flush();

            ans = (String) oinS.readObject();

            System.out.println(ans);

            while (true) {


                if (!request.isSession()) {
                    System.out.println("1 - Iniciar sessão");
                    System.out.println("2 - Criar conta");
                }
                System.out.println("3 - Fechar cliente");
                System.out.println();
                System.out.print("Opção: ");
                while (!sc.hasNextInt()) ;
                option = sc.nextInt();

                if (option == 1 && !request.isSession()) {
                    getUserCredentials(credentials);
                    request.setUsername(credentials.get(0));
                    request.setPassword(credentials.get(1));
                    request.setRequest("LOGIN");
                    oout.writeUnshared(request);
                    oout.flush();
                    ans = (String) oinS.readObject();
                    System.out.println(ans);
                    if (ans.equals("SUCCESS")) {
                        request.setSession(true);
                    }
                } else if (option == 2 && !request.isSession()) {
                    getUserCredentials(credentials);
                    request.setUsername(credentials.get(0));
                    request.setPassword(credentials.get(1));
                    request.setName(credentials.get(2));
                    request.setRequest("CREATE_ACCOUNT");
                    oout.writeUnshared(request);
                    oout.flush();
                    ans = (String) oinS.readObject();
                    System.out.println(ans);
                    if (ans.equals("SUCCESS")) {
                        request.setSession(true);
                    }
                } else if (option == 3)
                    return;
                else {
                    System.out.println("Opção inexistente.");
                }
            }
        } catch (SocketTimeoutException e) {

        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void getUserCredentials(ArrayList<String> cred) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        cred.add(sc.nextLine());

        System.out.print("Password: ");
        cred.add(sc.nextLine());

        System.out.print("Name: ");
        cred.add(sc.nextLine());
    }
}
