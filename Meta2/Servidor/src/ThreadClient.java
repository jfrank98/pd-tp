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
import java.util.ArrayList;
import java.util.Arrays;

public class ThreadClient extends Thread{
    private Socket socket;
    private String dbAddress;
    private static final String GET_USERS_QUERY = "SELECT * FROM User;";
    private static final String GET_USERNAMES_QUERY = "SELECT username FROM User;";
    private static final String GET_GROUPS_QUERY = "SELECT * FROM `Group`;";
    private static final String GET_CONTACTS_QUERY = "SELECT * FROM UserContact;";
    private static final String GET_USERINGROUP_QUERY = "SELECT * FROM Useringroup;";
    private static final String COUNT_USERS_QUERY = "SELECT COUNT(*) FROM User;";
    private static final String COUNT_GROUPS_QUERY = "SELECT COUNT(*) FROM `Group`;";
    private Statement stmt;
    private Connection conn;
    private ArrayList<String> listaC = new ArrayList<>();
    private ArrayList<String> listaG = new ArrayList<>();
    private ArrayList<String> listaGAmin = new ArrayList<>();
    private ArrayList<String> listaM = new ArrayList<>();
    private int contactID;
    private int groupID;
    private int adminID;

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
                    else if (req.getMessage().equalsIgnoreCase("LIST_CONTACTS")){
                        req.getListaContactos().clear();
                        req.setMessage(listContacts(req.getID()));

                        for (String c : listaC) {
                            req.addContactSuccess(c);
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("ADD_CONTACT")){
                        req.setMessage(addContact(req.getContact(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("REMOVE_CONTACT")){
                        req.setMessage(removeContact(req.getContact(), req.getID()));

                        if(req.getMessage().equalsIgnoreCase("SUCCESS")){
                            req.removeContactSuccess(String.valueOf(contactID));
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("LIST_GROUPS")){
                        req.getListaGrupos().clear();
                        req.setMessage(listGroups(req.getID()));

                        for (String g : listaG) {
                            req.addGroupSuccess(g);
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("JOIN_GROUP")){
                        req.setMessage(joinGroup(req.getGroupName(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("CREATE_GROUP")){
                        req.setMessage(createGroup(req.getGroupName(), req.getID()));

                        joinGroup(req.getGroupName(), req.getID());
                    }
                    else if (req.getMessage().equalsIgnoreCase("LIST_ADMIN_GROUPS")){
                        req.setMessage(listGroupsAdmin(req.getID()));

                        for (String g : listaGAmin) {
                            req.addGroupAdminSuccess(g);
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("LIST_MEMBERS_GROUP")){
                        req.setMessage(listMembers(req.getGroupName(), req.getID()));

                        if(req.getMessage().equals("SUCCESS")){
                            for (String m : listaM) {
                                req.addMemberSuccess(m);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("CHANGE_GROUP_NAME")){
                        req.setMessage(changeGroupName(req.getOldGroupName(), req.getGroupName(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("LEAVE_GROUP")){
                        req.setMessage(leaveGroup(req.getGroupName(), req.getID()));

                        if(req.getMessage().equalsIgnoreCase("SUCCESS")){
                            req.leaveGroupSuccess(String.valueOf(groupID));
                        }
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

    public String listContacts(int id){
        String ans = "FAILURE";
        String contacto = "";

        listaC.clear();

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM UserContact WHERE user_id = " + id);
            ResultSet rs2;
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            while (rs.next()) {
                rs2 = stmt2.executeQuery("SELECT * FROM User WHERE user_id = " + rs.getInt(2));
                rs2.next();
                contacto = rs2.getString(3);
                listaC.add(contacto);
            }

            ans = "SUCCESS";
        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String addContact(String u, int id){
        String ans = "FAILURE";
        boolean encontrou = false;
        contactID = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    contactID = rs.getInt(1);

                    if(contactID == id){
                        ans = "FAILURE - Não pode adicionar o seu próprio contacto.";
                        return ans;
                    }
                    encontrou = true;
                    break;
                }
            }

            if(!encontrou){
                ans = "FAILURE - Esse contacto não existe";
                return ans;
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

    public String removeContact(String u, int id){
        String ans = "FAILURE";
        boolean encontrou = false;

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    contactID = rs.getInt(1);
                    encontrou = true;
                    break;
                }
            }

            if(!encontrou || contactID == id){
                ans = "FAILURE - Esse contacto não existe na sua lista.";
                return ans;
            }

            PreparedStatement ps = conn.prepareStatement("DELETE FROM UserContact WHERE user_id = ? AND contact_id = ?");
            ps.setInt(1, id);
            ps.setInt(2, contactID);

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM UserContact WHERE user_id = ? AND contact_id = ?");
            ps2.setInt(1, contactID);
            ps2.setInt(2, id);

            ps.executeUpdate();
            ps2.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String listGroups(int id){
        String ans = "FAILURE";
        String grupo = "";

        listaG.clear();

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM useringroup WHERE group_user_id = " + id);
            ResultSet rs2;
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            while (rs.next()) {
                rs2 = stmt2.executeQuery("SELECT * FROM `Group` WHERE group_id = " + rs.getInt(1));
                rs2.next();
                grupo = rs2.getString(3);
                listaG.add(grupo);
            }

            ans = "SUCCESS";
        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String joinGroup(String n, int id){
        String ans = "FAILURE";
        boolean encontrou = false;
        adminID = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_GROUPS_QUERY);

            while (rs.next()) {
                if (n.equalsIgnoreCase(rs.getString(3))) {
                    groupID = rs.getInt(1);
                    adminID = rs.getInt(4);
                    encontrou = true;
                    break;
                }
            }

            if(!encontrou){
                ans = "FAILURE - Esse grupo não existe";
                return ans;
            }

            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet group = stmt2.executeQuery(GET_USERINGROUP_QUERY);

            while(group.next()){
                if(id == group.getInt(3) && groupID == group.getInt(1)){
                    ans = "FAILURE - Já pertence a esse grupo";
                    return ans;
                }
            }

            PreparedStatement ps = conn.prepareStatement("INSERT INTO useringroup (group_group_id, group_admin, group_user_id) VALUES (?, ?, ?)");
            ps.setInt(1, groupID);
            ps.setInt(2, adminID);
            ps.setInt(3, id);

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

        return ans;
    }

    public String listGroupsAdmin(int id){
        String ans = "FAILURE";
        String grupo = "";

        listaGAmin.clear();

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM `Group` WHERE admin = " + id);
            ResultSet rs2;
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            while (rs.next()) {
                rs2 = stmt2.executeQuery("SELECT * FROM `Group` WHERE group_id = " + rs.getInt(1));
                rs2.next();
                grupo = rs2.getString(3);
                listaGAmin.add(grupo);
            }

            ans = "SUCCESS";
        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String listMembers(String g, int id){
        String ans = "FAILURE";
        boolean encontrou = false;
        groupID = 0;
        String membro = "";

        listaM.clear();

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM `Group` WHERE admin = " + id);

            while(rs.next()){
                if(g.equalsIgnoreCase(rs.getString(3))){
                    groupID = rs.getInt(1);
                    encontrou = true;
                    break;
                }
            }

            if(!encontrou){
                ans = "FAILURE - Não é administrador de nenhum grupo com esse nome.";
                return ans;
            }


            ResultSet rs2;
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            rs2 = stmt2.executeQuery("SELECT * FROM useringroup WHERE group_group_id = " + groupID);

            ResultSet rs3;
            Statement stmt3 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);

            while (rs2.next()) {

                rs3 = stmt3.executeQuery("SELECT * FROM User WHERE user_id = " + rs2.getInt(3));
                rs3.next();
                membro = rs3.getString(3);

                listaM.add(membro);
            }

            ans = "SUCCESS";
        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String changeGroupName(String o, String n, int id){
        String ans = "FAILURE";
        boolean exists = false;

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE `Group` SET group_name = ? WHERE group_name = ? AND admin = ?");
            ps.setString(1, n);
            ps.setString(2, o);
            ps.setInt(3, id);

            ResultSet rs = stmt.executeQuery(GET_GROUPS_QUERY);
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet groups = stmt2.executeQuery("SELECT * FROM `Group` WHERE admin = " + id);

            while (rs.next()) {
                if (o.equalsIgnoreCase(rs.getString(3)) && id == rs.getInt(4)) {
                    //vê se o user já é admin de um grupo com este nome
                    while (groups.next()) {
                        if (n.equalsIgnoreCase(groups.getString(3))) {
                            exists = true;
                            break;
                        }
                    }

                    if(!exists){
                        //altera nome do grupo na base de dados
                        ps.executeUpdate();
                        ans = "SUCCESS";
                    }
                    else{
                        ans = "FAILURE - Já tem um grupo com esse nome.";
                    }

                    break;
                }
            }
        }catch (SQLException e) {
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String leaveGroup(String n, int id){
        String ans = "FAILURE";
        boolean encontrou = false;
        adminID = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_GROUPS_QUERY);

            while (rs.next()) {
                if (n.equalsIgnoreCase(rs.getString(3))) {
                    groupID = rs.getInt(1);
                    adminID = rs.getInt(4);
                    encontrou = true;
                    break;
                }
            }

            if(!encontrou){
                ans = "FAILURE - Não pertence a nenhum grupo com esse nome.";
                return ans;
            }

            if(id == adminID){
                ans = "FAILURE - É administrador deste grupo, para poder sair tem que o eliminar.";
                return ans;
            }

            PreparedStatement ps = conn.prepareStatement("DELETE FROM useringroup WHERE group_user_id = ? AND group_group_id = ?");
            ps.setInt(1, id);
            ps.setInt(2, groupID);

            ps.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }
}
