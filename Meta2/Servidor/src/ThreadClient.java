import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class ThreadClient extends Thread{
    private Socket socket;
    private String dbAddress;
    public ThreadClient(Socket clientSocket, String dbAddress) {
        this.socket = clientSocket;
        this.dbAddress = dbAddress;
    }

    public void run() {
        ObjectInputStream oin;
        ObjectOutputStream out;
        RemoteInterface remoteDB = null;
        try {
            remoteDB = (RemoteInterface) Naming.lookup("rmi://" + dbAddress + "/chatdb");
            remoteDB.checkServerConnection("CONNECTED MAN");
        } catch (NotBoundException e) {
            e.printStackTrace();
        } catch (MalformedURLException e) {
            e.printStackTrace();
        } catch (RemoteException e) {
            e.printStackTrace();
        }

        try {
            System.out.println("bruhhhh");
            oin = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
            System.out.println("bruhhhh");
        } catch (IOException e) {
            return;
        }
        Request req;
        String ans = null;
        System.out.println("bruhhhh");
        while (true) {
            System.out.println("bruhhhh");
            try {
                System.out.println("Waiting for clients...");
                req = (Request) oin.readObject();

                if ((req == null) || req.getRequest().equalsIgnoreCase("QUIT") ) {
                    socket.close();
                    return;
                } else {
                    if (req.getRequest().equalsIgnoreCase("SERVER_REQUEST")){
                        ans = "Connected to server successfully.";
                    }
                    else if (req.getRequest().equalsIgnoreCase("CREATE_ACCOUNT")){
                        System.out.println("REQUEST: " + req.getRequest());
                        ans = remoteDB.createAccount(req.getUsername(), req.getPassword(), req.getName());
                    }
                    else if (req.getRequest().equalsIgnoreCase("LOGIN")) {
                        System.out.println("REQUEST: " + req.getRequest());
                        ans = remoteDB.loginUser(req.getUsername(), req.getPassword());
                    }
                    out.writeUnshared(ans);
                    out.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
                return;
            }
        }
    }
}
