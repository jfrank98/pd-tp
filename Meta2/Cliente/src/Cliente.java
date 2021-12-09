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
        ObjectInputStream oin;
        ObjectOutputStream oout = null;
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

            packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
            SocketGRDS.send(packet);

            packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE, AddrGRDS, PortGRDS);
            SocketGRDS.receive(packet);

            bin = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            oin = new ObjectInputStream(bin);

            server = (Servidor) oin.readObject();

            System.out.println("server hostname: " + server.getServerAddress().toString());
            System.out.println("server port: " + server.getListeningPort());

            ///////////////Tenta conectar a servidor///////////////

            serverAddress = server.getServerAddress();
            serverPort = server.getListeningPort();

            socket = new Socket(serverAddress, serverPort);
            socket.setSoTimeout(30000);

            while (true) {

                if (!connected) {
                    System.out.println("bruhhhh");
                    oout = new ObjectOutputStream(socket.getOutputStream());
                    oin = new ObjectInputStream(socket.getInputStream());
                    System.out.println("bruhhhh");

                    System.out.println("bruhhhh");
                    request.setRequest(SERVER_REQUEST);
                    oout.writeUnshared(request);
                    oout.flush();

                    ans = (String) oin.readObject();

                    System.out.println(ans);

                    connected = true;
                }

                Scanner sc = new Scanner(System.in);
                ArrayList<String> credentials = new ArrayList<>(3);
                int option = 0;


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
                    ans = (String) oin.readObject();
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
                    ans = (String) oin.readObject();
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