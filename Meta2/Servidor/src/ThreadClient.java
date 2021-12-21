import javax.swing.*;
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
                    //Lê o pedido do cliente
                    req = (Request) oin.readObject();
                }catch(EOFException | SocketException e) {
                    System.out.println("\nO cliente da thread com ID " + Thread.currentThread().getId() + " saiu.");
                    socket.close();
                    return;
                }

                if ((req == null) || req.getMessage().equalsIgnoreCase("QUIT") ) {
                    socket.close();
                    return;
                } else {
                    if (req.getMessage().equalsIgnoreCase("SERVER_REQUEST")){
                        req.setMessage("\nLigação com o servidor estabelecida.");
                    }
                    else if (req.getMessage().equalsIgnoreCase("CREATE_ACCOUNT")){
                        System.out.println("\nPedido do cliente: " + req.getMessage());
                        req.setMessage(createAccount(req.getUsername(), req.getPassword(), req.getName()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("LOGIN")) {
                        System.out.println("\nPedido do cliente: " + req.getMessage());
                        req.setMessage(loginUser(req.getUsername(), req.getPassword()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("CHANGE_USERNAME")){
                        System.out.println("\nPedido do cliente: " + req.getMessage());
                        req.setMessage(changeUsername(req.getUsername(), req.getOldUsername(), req.getPassword()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("CHANGE_PASSWORD")){
                        System.out.println("\nPedido do cliente: " + req.getMessage());
                        req.setMessage(changePassword(req.getUsername(), req.getPassword()));
                    }

                    //Envia resposta ao cliente
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

            //Verifica se já existe um username igual
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

        } catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Resposta: " + ans);

        return ans;
    }

    public String loginUser(String u, String p) throws RemoteException {
        String ans = "FAILURE";

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3)) && p.equalsIgnoreCase(rs.getString(2))) {
                    ans = "SUCCESS";
                    break;
                }
            }
        }catch(SQLException e){
            e.printStackTrace();
        }

        System.out.println("Resposta: " + ans);

        return ans;
    }

    public String changeUsername(String u, String o, String p) {
        String ans = "FAILURE";
        boolean exists = false;

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE User SET username = ? WHERE username = ? AND password = ?");
            ps.setString(1, u);
            ps.setString(2, o);
            ps.setString(3, p);

            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);
            ResultSet usernames = stmt.executeQuery(GET_USERNAMES_QUERY);

            while (rs.next()) {
                if (o.equalsIgnoreCase(rs.getString(4)) && p.equalsIgnoreCase(rs.getString(2))) {
                    //vê se o username já existe
                    while (rs.next()) {
                        if (u.equalsIgnoreCase(usernames.getString(1))) {
                            exists = true;
                            break;
                        }
                    }

                    if(!exists){
                        //altera username na base de dados
                        ps.executeUpdate();
                        ans = "SUCCESS";
                    }
                    else{
                        ans = "FAILURE";
                    }

                    break;
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Resposta: " + ans);

        return ans;
    }

    public String changePassword(String u, String p){
        String ans = "FAILURE";

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE User SET password = ? WHERE username = ?");
            ps.setString(1, p);
            ps.setString(2, u);

            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    //altera password na base de dados
                    ps.executeUpdate();
                    ans = "SUCCESS";
                    break;
                }
            }
        }catch (SQLException e) {
            e.printStackTrace();
        }

        System.out.println("Resposta: " + ans);

        return ans;
    }
}
