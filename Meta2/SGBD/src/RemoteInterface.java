import java.rmi.Remote;
import java.rmi.RemoteException;

public interface RemoteInterface extends Remote {

    String createAccount(String u, String p, String n) throws RemoteException;

    void checkServerConnection(String s) throws RemoteException;

    public String loginUser(String u, String p) throws RemoteException;
}


