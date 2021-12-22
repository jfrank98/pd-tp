import javax.swing.*;
import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.SocketException;
import java.rmi.RemoteException;
import java.sql.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;

public class ThreadClient extends Thread{
    private Socket socket;
    private String dbAddress;
    private static final String GET_USERS_QUERY = "SELECT * FROM User;";
    private static final String GET_USERNAMES_QUERY = "SELECT username FROM User;";
    private static final String GET_GROUPS_QUERY = "SELECT * FROM `Group`;";
    private static final String GET_CONTACTS_QUERY = "SELECT * FROM UserContact;";
    private static final String COUNT_USERS_QUERY = "SELECT COUNT(*) FROM User;";
    private static final String COUNT_GROUPS_QUERY = "SELECT COUNT(*) FROM `Group`;";
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
                    System.out.println("\nPedido do cliente: " + req.getMessage());

                    if (req.getMessage().equalsIgnoreCase("SERVER_REQUEST")){
                        req.setMessage("\nLigação com o servidor estabelecida.");
                    }
                    else if (req.getMessage().equalsIgnoreCase("CREATE_ACCOUNT")){
                        req.setMessage(createAccount(req.getUsername(), req.getPassword(), req.getName()));

                        if(req.getMessage().equalsIgnoreCase("SUCCESS")){
                            req.setID(getIDFromDB(req.getUsername()));
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("LOGIN")) {
                        req.setMessage(loginUser(req.getUsername(), req.getPassword()));

                        if(req.getMessage().equalsIgnoreCase("SUCCESS")){
                            req.setID(getIDFromDB(req.getUsername()));
                            req.setName(getNameFromDB(req.getUsername()));
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("CHANGE_USERNAME")){
                        req.setMessage(changeUsername(req.getUsername(), req.getOldUsername(), req.getPassword()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("CHANGE_PASSWORD")){
                        req.setMessage(changePassword(req.getUsername(), req.getPassword()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("ADD_CONTACT")){
                        req.setMessage(addContact(req.getNewContact(), req.getID()));
                        if (req.getMessage().equalsIgnoreCase("SUCCESS")){
                            req.addContactSuccess(req.getNewContact());
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("CREATE_GROUP")){
                        req.setMessage(createGroup(req.getGroupName(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("JOIN_GROUP")){
                        req.setMessage(joinGroup(req.getGroupName(), req.getID()));
                    }


                    //Envia resposta ao cliente
                    out.writeUnshared(req);
                    out.flush();
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("\n" + e);
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
                        ans = "FAILURE - Esse username já está a ser usado";
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
            System.out.println("\n" + e);
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
            System.out.println("\n" + e);
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
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet usernames = stmt2.executeQuery(GET_USERNAMES_QUERY);

            while (rs.next()) {
                if (o.equalsIgnoreCase(rs.getString(3)) && p.equalsIgnoreCase(rs.getString(2))) {
                    //vê se o username já existe
                    while (usernames.next()) {
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
                        ans = "FAILURE - Esse username já está a ser usado";
                    }

                    break;
                }
            }
        }catch (SQLException e) {
            System.out.println("\n" + e);
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
            System.out.println("\n" + e);
        }

        System.out.println("Resposta: " + ans);

        return ans;
    }

    private int getIDFromDB(String u){
        int id = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    id = rs.getInt(1);
                    break;
                }
            }
        }catch(SQLException e){
            System.out.println("\n" + e);
        }
        return id;
    }

    private String getNameFromDB(String u){
        String name = null;

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    name = rs.getString(4);
                    break;
                }
            }
        }catch(SQLException e){
            System.out.println("\n" + e);
        }
        return name;
    }

    public String addContact(String u, int id){
        String ans = "FAILURE";
        int contactID = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    contactID = rs.getInt(1);
                    break;
                }
            }

            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet contact = stmt2.executeQuery(GET_CONTACTS_QUERY);

            while(contact.next()){
                if(id == contact.getInt(1) && contactID == contact.getInt(2)){
                    ans = "FAILURE - Já tem este contacto adicionado";
                    return ans;
                }
            }

            PreparedStatement ps = conn.prepareStatement("INSERT INTO UserContact (user_id, contact_id) VALUES (?, ?)");
            ps.setInt(1, id);
            ps.setInt(2, contactID);

            ps.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String createGroup(String n, int id){
        String ans = "FAILURE";

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO `Group` (create_date, group_name, admin) VALUES (?, ?, ?);");
            ps.setDate(1, Date.valueOf(LocalDate.now()));
            ps.setString(2, n);
            ps.setInt(3, id);

            ResultSet rs = stmt.executeQuery(COUNT_GROUPS_QUERY);
            rs.next();
            int size = rs.getInt(1);

            ResultSet rs2 = stmt.executeQuery(GET_GROUPS_QUERY);

            boolean newGroup = true;

            //Verifica se já tem um grupo com o mesmo nome
            if (size > 0) {
                while (rs2.next()) {
                    if (n.equalsIgnoreCase(rs2.getString(3)) && id == rs2.getInt(4)) {
                        ans = "FAILURE - Já tem um grupo com esse nome";
                        newGroup = false;
                        break;
                    }
                }

                if (newGroup) {
                    ps.executeUpdate();
                    ans = "SUCCESS";
                }
            }

            else {
                ps.executeUpdate();
                ans = "SUCCESS";
            }

        } catch (SQLException e) {
            System.out.println("\n" + e);
        }

        System.out.println("Resposta: " + ans);

        return ans;
    }

    public String joinGroup(String n, int id){
        String ans = "FAILURE";



        return ans;
    }
}
