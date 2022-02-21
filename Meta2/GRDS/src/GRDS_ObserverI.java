import java.net.DatagramPacket;
import java.net.InetAddress;
import java.rmi.Remote;
import java.rmi.RemoteException;

public interface GRDS_ObserverI extends Remote {
    //public void listServers(List<ServerData> serverList) throws RemoteException;
    void newServer(ServerData server) throws RemoteException;
    void serverRemoved(ServerData server) throws RemoteException;
    void newClientServerRequest(InetAddress address, int port) throws RemoteException;
    void newNotification(String notification) throws RemoteException;
}
