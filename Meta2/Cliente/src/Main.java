import java.io.IOException;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static final int MAX_SIZE = 1024;
    public static final String ADDR_PORT_REQUEST = "GET_ADDR_PORT_TCP";

    public static void main(String args[]) {

        InetAddress AddrGRDS;
        int PortGRDS;
        DatagramSocket SocketGRDS = null;
        DatagramPacket packet;
        String responseGRDS;

        if (args.length != 2){
            System.out.println("Sintaxe: java Cliente serverAddress serverUdpPort");
            return;
        }

        try{
            SocketGRDS = new DatagramSocket();
            AddrGRDS = InetAddress.getByName(args[0]);
            PortGRDS = Integer.parseInt(args[1]);

            packet = new DatagramPacket(ADDR_PORT_REQUEST.getBytes(), ADDR_PORT_REQUEST.length(), AddrGRDS, PortGRDS);
            SocketGRDS.send(packet);

            packet = new DatagramPacket(new byte[MAX_SIZE], MAX_SIZE, AddrGRDS, PortGRDS);
            SocketGRDS.receive(packet);

            responseGRDS = new String(packet.getData(), 0, packet.getLength());
            //response = packet.getData().toString();
            String [] addr_port_GRDS = responseGRDS.split(",");
            for (String s : addr_port_GRDS) {
                System.out.println(s);
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
            if(SocketGRDS != null){
                SocketGRDS.close();
            }
        }

        Scanner sc = new Scanner(System.in);
        ArrayList<String> credentials = new ArrayList<>(2);
        int option = 0;
        User user = new User();

        if (!user.isSession()) {
            System.out.println("1 - Iniciar sessão");
            System.out.println("2 - Criar conta");
        }
        System.out.println("3 - Fechar cliente");
        System.out.println();
        System.out.print("Opção: ");
        while (!sc.hasNextInt());
            option = sc.nextInt();

        if (option == 1 && !user.isSession()) {
            getUserCredentials(credentials);
            user.login(credentials.get(0), credentials.get(1));
        }
        else if (option == 2 && !user.isSession()) {
            getUserCredentials(credentials);
            user = new User(credentials.get(0), credentials.get(1));
            user.createAccount();
        }
        else if (option == 3)
            return;
        else {
            System.out.println("Opção inexistente.");
        }

    }

    public static void getUserCredentials(ArrayList<String> cred) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        cred.add(sc.nextLine());

        System.out.print("Password: ");
        cred.add(sc.nextLine());
    }
}
