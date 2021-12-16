import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
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
            oin = new ObjectInputStream(socket.getInputStream());
            out = new ObjectOutputStream(socket.getOutputStream());
        } catch (IOException e) {
            return;
        }
        Request req;

        while (true) {
            try {
                try {
                    req = (Request) oin.readObject();
                }catch(EOFException | SocketException e) {
                    System.out.println("Cliente da thread ID " + Thread.currentThread().getId() + " fechado.");
                    socket.close();
                    return;
                }

                if ((req == null) || req.getMessage().equalsIgnoreCase("QUIT") ) {
                    socket.close();
                    return;
                } else {
                    if (req.getMessage().equalsIgnoreCase("SERVER_REQUEST")){
                        req.setMessage("Connected to server successfully.");
                    }
                    else if (req.getMessage().equalsIgnoreCase("CREATE_ACCOUNT")){
                        System.out.println("REQUEST: " + req.getMessage());
                        req.setMessage(createAccount(req.getUsername(), req.getPassword(), req.getName()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("LOGIN")) {
                        System.out.println("REQUEST: " + req.getMessage());
                        req.setMessage(loginUser(req.getUsername(), req.getPassword()));
                    }
                    out.writeUnshared(req);
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

    public String loginUser(String u, String p) throws RemoteException {
        String ans = "FAILURE";
        System.out.println("ok nice");


        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);
            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3)) && p.equalsIgnoreCase(rs.getString(2))) {
                    ans = "SUCCESS";
                    break;
                }
            }

        }catch(SQLException e){ }

        System.out.println(ans);
        return ans;
    }
}
