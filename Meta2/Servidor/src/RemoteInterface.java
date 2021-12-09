import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {

    String createAccount(String username, String password, String name) throws RemoteException;

    void checkServerConnection(String s) throws RemoteException;
}


