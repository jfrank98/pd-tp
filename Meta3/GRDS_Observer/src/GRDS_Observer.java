import java.io.Serializable;
import java.net.DatagramPacket;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.Remote;
import java.rmi.RemoteException;
import java.rmi.server.UnicastRemoteObject;
import java.util.List;
import java.util.Scanner;

public class GRDS_Observer extends UnicastRemoteObject implements GRDS_ObserverI, Serializable {

    private static List<ServerData> servers;
    private static Scanner scanner = new Scanner(System.in);
    private static InterfaceGRDS interfaceGRDS;
    public GRDS_Observer() throws RemoteException {
    }

    public static void main(String[] args) {
        System.out.println("À procura do GRDS...");

        try {
            String registration = "rmi://localhost/GRDS";
            Remote remoteService = Naming.lookup(registration);
            interfaceGRDS = (InterfaceGRDS) remoteService;

            listServers();

            GRDS_Observer observer = new GRDS_Observer();
            interfaceGRDS.addServersObserver(observer);

            while (true) {
                System.out.println("1 - Listar servidores ativos");
                System.out.print("Opcao: ");
                while (!scanner.hasNextInt()) {
                    System.out.print("Opcao: ");
                }
                int opt = scanner.nextInt();

                if (opt == 1) {
                    listServers();
                }
                else {
                    System.out.println("Nao existe uma opcao \"" + opt + "\"");
                }
            }

        } catch (MalformedURLException | NotBoundException | RemoteException e) {
            e.printStackTrace();
        }
    }

    private static void listServers() throws RemoteException {
        servers = interfaceGRDS.getServers();

        if (servers.isEmpty()) {
            System.out.println("Não há servidores conectados ao GRDS.");
            return;
        }
        System.out.println("\n---------------------");
        System.out.println("# Servidores ativos #");
        System.out.println("---------------------");
        for (ServerData s : servers) {
            System.out.println("Endereço IP: " + s.getServerAddress());
            System.out.println("Porto: " + s.getListeningPort());
            System.out.println("Tempo desde o último pacote recebido: " + ((System.currentTimeMillis() / 1000) - s.getTimeSinceLastMsg()) + " segundos.");
            System.out.println("---------------------");
        }
    }

    @Override
    public void newServer(ServerData server) throws RemoteException {
        System.out.println("\nNovo servidor!");
        servers.add(server);
    }

    @Override
    public void serverRemoved(ServerData server) throws RemoteException {
        System.out.println("\nServidor removido.");
        servers.remove(server);
    }

    @Override
    public void newClientServerRequest(InetAddress address, int port) throws RemoteException {
        System.out.println("\nNovo pedido de servidor por cliente:");
        System.out.println("\tEndereço IP: " + address.toString());
        System.out.println("\tPorto: " + port);
    }

    @Override
    public void newNotification(String notification) throws RemoteException {
        System.out.println("Nova notificacao do tipo: " + notification);
    }
}
