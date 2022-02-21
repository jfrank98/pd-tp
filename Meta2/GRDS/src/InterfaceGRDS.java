import java.rmi.Remote;
import java.rmi.RemoteException;
import java.util.List;

public interface InterfaceGRDS extends Remote {
    List<ServerData> getServers() throws RemoteException;
    void addServersObserver(GRDS_ObserverI observer) throws RemoteException;
}

