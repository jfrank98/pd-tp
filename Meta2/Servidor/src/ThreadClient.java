import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.MalformedURLException;
import java.net.Socket;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;
import java.sql.*;

public class ThreadClient extends Thread{
    private Socket socket;
    private String dbAddress;
    private static final String GET_USERS_QUERY = "SELECT * FROM User;";
    private static final String COUNT_USERS_QUERY = "SELECT COUNT(*) FROM User;";
    private static final String GET_USERNAMES_QUERY = "SELECT username FROM User;";
    private Statement stmt;
    private Connection conn;

    public ThreadClient(Socket clientSocket, Statement stmt, Connection conn) {
        this.socket = clientSocket;
        this.stmt = stmt;
        this.conn = conn;
    }



    public void run() {
        ObjectInputStream oin;
        ObjectOutputStream out;

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
                        ans = createAccount(req.getUsername(), req.getPassword(), req.getName());
                    }
                    else if (req.getRequest().equalsIgnoreCase("LOGIN")) {
                        System.out.println("REQUEST: " + req.getRequest());
                        ans = loginUser(req.getUsername(), req.getPassword());
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


    public String createAccount(String u, String p, String n) throws RemoteException {
        String ans = null;

        System.out.println("ok nice");

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO User (password, username, name) VALUES (?, ?, ?)");
            ps.setString(1, p);
            ps.setString(2, u);
            ps.setString(3, n);

            ResultSet r = stmt.executeQuery(COUNT_USERS_QUERY);
            r.next();
            int size = r.getInt(1);
            ResultSet usernames = stmt.executeQuery(GET_USERNAMES_QUERY);

            boolean newUser = true;


            if (size > 0) {
                while (usernames.next()) {
                    if (u.equalsIgnoreCase(usernames.getString(1))) {
                        ans = "FAILURE";
                        newUser = false;
                        break;
                    }
                }
                if (newUser) {
                    ps.executeUpdate();
                    ans = "SUCCESS";
                }
            }

            else {
                ps.executeUpdate();
                ans = "SUCCESS";
            }


        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return ans;
    }

    public void checkServerConnection(String s) throws RemoteException {
        System.out.println(s);
    }

    public String loginUser(String u, String p) throws RemoteException {
        String ans = "FAILURE";
        System.out.println("ok nice");


        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);
            while (rs.next()) {
                System.out.println(" checking u: " + rs.getString(3) + " p: " + rs.getString(2));
                if (u.equalsIgnoreCase(rs.getString(3)) && u.equalsIgnoreCase(rs.getString(2))) {
                    ans = "SUCCESS";
                    break;
                }
            }

        }catch(SQLException e){ }

        System.out.println(ans);
        return ans;
    }
}
