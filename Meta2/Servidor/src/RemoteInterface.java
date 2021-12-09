import java.rmi.RemoteException;

public interface RemoteInterface {
    void createAccount(String username, String password) throws RemoteException;
}
