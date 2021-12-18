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
        Servidor server = null;
        ByteArrayInputStream bin;
        ObjectInputStream oin, oinS;
        ObjectOutputStream oout;
        boolean connected = false;
        String ans;
        Request request = new Request();

        //Verifica se recebeu os argumentos necessários: endereço IP e porto de escuta do GRDS
        if (args.length != 2) {
            System.out.println("Sintaxe: java Cliente GRDSaddress GRDSport");
            return;
        }

        //Tenta contactar o GRDS para receber os dados de um servidor ativo
        try {

            //Cria um DatagramSocket e guarda o IP e porto de escuta do GRDS
            SocketGRDS = new DatagramSocket();
            AddrGRDS = InetAddress.getByName(args[0]);
            PortGRDS = Integer.parseInt(args[1]);
            SocketGRDS.setSoTimeout(5000);

            //System.out.println("bruh");

            //Cria um DatagramPacket e envia-o ao GRDS através do DatagramSocket criado antes
            packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
            SocketGRDS.send(packet);

            //System.out.println("bruh 1");

            //Limpa o packet e recebe resposta do GRDS
            packet.setData(new byte[MAX_SIZE], 0, MAX_SIZE);
            SocketGRDS.receive(packet);

            //System.out.println("bruh 2");

            //Lê o packet com os dados de um servidor ativo e cria um objeto
            bin = new ByteArrayInputStream(packet.getData(), 0, packet.getLength());
            oin = new ObjectInputStream(bin);

            //System.out.println("bruh 3");

            //Lê o objeto e guarda os dados
            server = (Servidor) oin.readObject();

            //Fecha o socket
            SocketGRDS.close();

            System.out.println("\nEndereço IP: " + server.getServerAddress().toString());
            System.out.println("Porto de escuta: " + server.getListeningPort());

        } catch (UnknownHostException e) {
            System.out.println("\nDestino desconhecido:\n\t" + e);
            return;
        } catch (NumberFormatException e) {
            System.out.println("\nO porto de escuta do servidor deve ser um inteiro positivo:\n\t" + e);
            return;
        } catch (SocketTimeoutException e) {
            System.out.println("\nNão foi recebida qualquer resposta do GRDS:\n\t" + e);
            return;
        } catch (SocketException e) {
            System.out.println("\nOcorreu um erro ao nível do socket:\n\t" + e);
            return;
        } catch (IOException e) {
            System.out.println("\nOcorreu um erro no acesso ao socket:\n\t" + e);
            return;
        } catch (NullPointerException e){
            System.out.println("\nNão foi encontrado um servidor ativo:\n\t" + e);
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

        //Tenta conectar-se ao servidor
        try {

            serverAddress = server.getServerAddress();
            serverPort = server.getListeningPort();

            //Cria um Socket para comuninar comunicar com o servidor
            socket = new Socket(serverAddress, serverPort);
            socket.setSoTimeout(5000);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oinS = new ObjectInputStream(socket.getInputStream());

            //Envia pedido ao servidor
            request.setRequest(SERVER_REQUEST);
            oout.writeUnshared(request);
            oout.flush();

            //Recebe resposta do servidor
            ans = (String) oinS.readObject();
            System.out.println("\n" + ans + "\n");

            while (true) {

                if (!request.isSession()) {
                    System.out.println("1 - Iniciar sessão");
                    System.out.println("2 - Criar conta");
                }

                System.out.println("3 - Sair");
                System.out.println();
                System.out.print("Opção: ");
                while (!sc.hasNextInt()) ;
                option = sc.nextInt();

                if (option == 1 && !request.isSession()) {
                    getUserCredentials(credentials);
                    request.setUsername(credentials.get(0));
                    request.setPassword(credentials.get(1));
                    request.setRequest("LOGIN");

                    //Envia pedido de LOGIN ao servidor
                    oout.writeUnshared(request);
                    oout.flush();

                    //Recebe resposta do servidor
                    ans = (String) oinS.readObject();
                    System.out.println("\n" + ans + "\n");

                    if (ans.equals("SUCCESS")) {
                        request.setSession(true);
                    }
                } else if (option == 2 && !request.isSession()) {
                    getUserCredentials(credentials);
                    request.setUsername(credentials.get(0));
                    request.setPassword(credentials.get(1));
                    request.setName(credentials.get(2));
                    request.setRequest("CREATE_ACCOUNT");

                    //Envia pedido de CREATE_ACCOUNT ao servidor
                    oout.writeUnshared(request);
                    oout.flush();

                    //Recebe resposta do servidor
                    ans = (String) oinS.readObject();
                    System.out.println("\n" + ans + "\n");

                    if (ans.equals(null)) {
                        System.out.println("\nErro ao tentar criar conta.\n");
                        return;
                    }

                    if (ans.equals("SUCCESS")) {
                        request.setSession(true);
                    }
                    else {
                        return;
                    }
                } else if (option == 3)
                    return;
                else {
                    System.out.println("\nOpção inválida.\n");
                }
            }
        } catch (SocketTimeoutException e) {
            System.out.println("\nNão foi possível estabelecer ligação com o servidor:\n\t" + e);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }
    }

    public static void getUserCredentials(ArrayList<String> cred) {
        Scanner sc = new Scanner(System.in);

        System.out.print("\nUsername: ");
        cred.add(sc.nextLine());

        System.out.print("Password: ");
        cred.add(sc.nextLine());

        System.out.print("Name: ");
        cred.add(sc.nextLine());
    }
}
