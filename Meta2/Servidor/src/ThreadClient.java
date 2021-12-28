import java.io.EOFException;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.sql.*;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;

public class ThreadClient extends Thread{

    private Socket socket;
    private String dbAddress;
    private static final String GET_USERS_QUERY = "SELECT * FROM User;";
    private static final String GET_USERNAMES_QUERY = "SELECT username FROM User;";
    private static final String GET_GROUPS_QUERY = "SELECT * FROM `Group`;";
    private static final String GET_CONTACTS_QUERY = "SELECT * FROM UserContact;";
    private static final String GET_USERINGROUP_QUERY = "SELECT * FROM UserInGroup;";
    private static final String COUNT_USERS_QUERY = "SELECT COUNT(*) FROM User;";
    private static final String COUNT_GROUPS_QUERY = "SELECT COUNT(*) FROM `Group`;";
    private static final String COUNT_USERS_IN_GROUP_QUERY = "SELECT COUNT(*) FROM UserInGroup;";
    private static final String COUNT_USER_CONTACTS_QUERY = "SELECT COUNT(*) FROM UserContact";
    private Statement stmt;
    private Connection conn;
    private ArrayList<String> listaC = new ArrayList<>();
    private ArrayList<String> listaG = new ArrayList<>();
    private ArrayList<String> listaGAmin = new ArrayList<>();
    private ArrayList<String> listaM = new ArrayList<>();
    private ArrayList<String> pendingJoinRequests = new ArrayList<>();
    private ArrayList<String> pendingContactRequests = new ArrayList<>();
    private ArrayList<Integer> pendingJoinRequestsGroupId = new ArrayList<>();
    private ArrayList<String> messageHistory = new ArrayList<>();
    private ArrayList<String> groupHistory = new ArrayList<>();
    private int contactID;
    private int groupID;
    private int adminID;
    private StartServer startServer;
    private ArrayList<ServerData> serverList;

    public ThreadClient(Socket clientSocket, Statement stmt, Connection conn, StartServer startServer) {
        this.socket = clientSocket;
        this.stmt = stmt;
        this.conn = conn;
        this.startServer = startServer;
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

        Request req = new Request();

        while (true) {
            serverList = startServer.getServerList();

            try {
                try {
                    //Lê o pedido do cliente
                    req = (Request) oin.readObject();
                }catch(EOFException | SocketException e) {
                    System.out.println("\nO cliente da thread com ID " + Thread.currentThread().getId() + " saiu.");
                    if (req.getID() != -1) {
                        for (ServerData serverData : serverList) {
                            if (serverData.getListeningPort() == socket.getLocalPort()) {
                                serverData.removeClient(req.getUsername());
                            }
                        }
                        logout(req.getID());
                    }
                    socket.close();
                    return;
                }

                if ((req == null) || req.getMessage().equalsIgnoreCase("QUIT") ) {
                    if(req.getID() != -1)
                        logout(req.getID());
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
                            for (ServerData serverData : serverList){
                                if (serverData.getListeningPort() == socket.getLocalPort()){
                                    serverData.setClients(req.getUsername());
                                }
                            }
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

                        if (listaC.size() > 0) {
                            for (String c : listaC) {
                                req.addContactSuccess(c);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("ADD_CONTACT")){
                        req.setMessage(addContact(req.getContact(), req.getID(), false));
                    }
                    else if (req.getMessage().equalsIgnoreCase("REMOVE_CONTACT")){
                        req.setMessage(removeContact(req.getContact(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("LIST_GROUPS")){
                        req.getListaGrupos().clear();
                        req.setMessage(listGroups(req.getID()));

                        if (listaG.size() > 0) {
                            for (String g : listaG) {
                                req.addGroupSuccess(g);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("JOIN_GROUP")){
                        req.setMessage(joinGroup(req.getGroupName(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("CREATE_GROUP")){
                        req.setMessage(createGroup(req.getGroupName(), req.getID()));
                        req.setGroupOwner(true);
                        joinGroup(req.getGroupName(), req.getID());
                    }
                    else if (req.getMessage().equalsIgnoreCase("LIST_ADMIN_GROUPS")){
                        req.setMessage(listGroupsAdmin(req.getID()));

                        if (listaGAmin.size() > 0) {
                            for (String g : listaGAmin) {
                                req.addGroupAdminSuccess(g);
                            }
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
                    else if (req.getMessage().equalsIgnoreCase("REMOVE_MEMBER")){
                        req.setMessage(removeMember(req.getContact(), req.getGroupName(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("DELETE_GROUP")){
                        req.setMessage(deleteGroup(req.getGroupName(), req.getID()));
                        if (req.getListaGruposAdmin().isEmpty()) req.setGroupOwner(false);
                    }
                    else if (req.getMessage().equalsIgnoreCase("LEAVE_GROUP")){
                        req.setMessage(leaveGroup(req.getGroupName(), req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("GET_PENDING_GROUP_REQUESTS")){
                        req.setMessage(checkNewGroupRequests(req.getID()));
                        if (req.getMessage().equalsIgnoreCase("SUCCESS")){
                            if (!pendingJoinRequests.isEmpty()) {
                                req.setGroupOwner(true);
                                for (String s : pendingJoinRequests)
                                    req.setPendingJoinRequests(s);
                                for (Integer a : pendingJoinRequestsGroupId)
                                    req.setPendingJoinRequestsGroupId(a);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("GET_PENDING_CONTACT_REQUESTS")) {
                        req.setMessage(checkNewContactRequests(req.getID()));
                        if (req.getMessage().equalsIgnoreCase("SUCCESS")) {
                            if (!pendingContactRequests.isEmpty()) {
                                for (String s : pendingContactRequests)
                                    req.setPendingContactRequests(s);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("ACCEPT_ALL_CONTACT_REQUESTS")) {
                        req.setMessage(acceptAllContactRequests(req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("REJECT_ALL_CONTACT_REQUESTS")) {
                        req.setMessage(rejectAllContactRequests(req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("IGNORE_ALL_CONTACT_REQUESTS")) {
                        req.setMessage("SUCCESS");
                    }
                    else if (req.getMessage().equalsIgnoreCase("ACCEPT_CONTACT_REQUESTS")) {
                        req.setMessage(manageContactRequests(req.getID(), req.getAcceptRejectIgnoreRequests(), req.getPendingContactRequests()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("ACCEPT_GROUP_REQUESTS")) {
                        req.setMessage(manageGroupRequests(req.getID(), req.getAcceptRejectIgnoreRequests(), req.getPendingJoinRequests(), req.getPendingJoinRequestsGroupId()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("ACCEPT_ALL_GROUP_REQUESTS")){
                        req.setMessage(acceptAllGroupRequests(req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("REJECT_ALL_GROUP_REQUESTS")) {
                        req.setMessage(rejectAllGroupRequests(req.getID()));
                    }
                    else if (req.getMessage().equalsIgnoreCase("IGNORE_ALL_GROUP_REQUESTS")) {
                        req.setMessage("SUCCESS");
                    }
                    else if (req.getMessage().equalsIgnoreCase("GET_MESSAGES_FROM")){
                        req.getHistoricoMensagens().clear();
                        contactID = getIDFromDB(req.getContact());
                        req.setMessage(getMessagesFrom(req.getID(), contactID));

                        if(messageHistory.size() > 0){
                            for(String m : messageHistory){
                                req.addMessageSuccess(m);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("GET_GROUP_MESSAGES")){
                        req.getHistoricoGrupo().clear();

                        //descobrir o id do grupo
                        //groupID = ;
                        req.setMessage(getGroupMessages(groupID));

                        if(groupHistory.size() > 0){
                            for(String m: groupHistory){
                                req.addGroupHistorySuccess(m);
                            }
                        }
                    }
                    else if (req.getMessage().equalsIgnoreCase("SEND_MESSAGE")) {
                        if (req.isSendFile()){
                            ServerSocket fileSocket = new ServerSocket(0);
                            req.setFileSocketPort(fileSocket.getLocalPort());
                            req.setFileSocketAddress(fileSocket.getInetAddress());
                            System.out.println("Receber ficheiro ");
                            Runnable r = new ReceiveFile(req.getF().getName(), fileSocket);
                            new Thread(r).start();
                        }
                        req.setMessage(createMessage(req.getID(), req.getMessageContent(), getIDFromDB(req.getContact()), req.isSendFile(), req.getF()));
                    }
                    //Envia resposta ao cliente
                    out.writeUnshared(req);
                }
            } catch (IOException | ClassNotFoundException e) {
                System.out.println("\n" + e);
                return;
            }
        }
    }

    public String createMessage(int id, String message, int cid, boolean isFile, File f) {
        String ans = "FAILURE";

        try {

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM UserContact WHERE user_id = ? AND contact_id = ?");
            ps1.setInt(1, id);
            ps1.setInt(2, cid);
            ps1.executeQuery();

            ResultSet rs = ps1.executeQuery();
            rs.next();

            if(!rs.getBoolean(3)){
                ans = "FAILURE - Este contacto ainda não aceitou o seu pedido.";
                return ans;
            }

            PreparedStatement ps = conn.prepareStatement("INSERT INTO Message (content, timestamp, User_user_id) VALUES (?, ?, ?);", Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, getUsernameByID(id) + ": " + message);

            Timestamp ts = Timestamp.from(Instant.now());
            ps.setTimestamp(2, ts);
            ps.setInt(3, id);


            ps.executeUpdate();

            ResultSet rs2 = ps.getGeneratedKeys();
            int mid = 0;
            if (rs2.next()) {
                mid = rs2.getInt(1);
                f.setUniqueName(f.getName() + "_" + mid);
            }

            if (isFile) {
                PreparedStatement ps2 = conn.prepareStatement("INSERT INTO File (file_name, Message_message_id) VALUES (?, ?)");
                ps2.setString(1, f.getUniqueName());
                ps2.setInt(2, mid);
                ps2.executeUpdate();
            }

            PreparedStatement ps2 = conn.prepareStatement("INSERT INTO MessageRecipient (recipient_id, message_id, sender_id) VALUES (?, ?, ?);");
            ps2.setInt(1, cid);
            ps2.setInt(2, mid);
            ps2.setInt(3, id);

            ps2.executeUpdate();

            ans = "SUCCESS";
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ans;
    }

    private String getGroupMessages(int gID){
        String ans = "FAILURE";
        groupHistory.clear();

        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Message WHERE group_id = ?");
            ps.setInt(1, gID);

            ResultSet rs = ps.executeQuery();

            while (rs.next()){
                groupHistory.add(rs.getString(2));
            }

            ans = "SUCCESS";

        } catch (SQLException e) {
            e.printStackTrace();
        }

        return ans;
    }

    private String getMessagesFrom(int id, int cid) {
        String ans = "FAILURE";
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM MessageRecipient WHERE recipient_id = ? AND sender_id = ?;");
            ps.setInt(1, id);
            ps.setInt(2, cid);

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM MessageRecipient WHERE recipient_id = ? AND sender_id = ?;");
            ps1.setInt(1, cid);
            ps1.setInt(2, id);

            ResultSet receivedMessages = ps.executeQuery();
            ResultSet sentMessages = ps1.executeQuery();

            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM Message ORDER BY timestamp");

            ArrayList<Message> everyMessage = new ArrayList<>();

            while(receivedMessages.next()){
                everyMessage.add(new Message(receivedMessages.getInt(3)));
            }

            while(sentMessages.next()){
                everyMessage.add(new Message(sentMessages.getInt(3)));
            }

            ResultSet rs2 = ps2.executeQuery();
            while (rs2.next()) {
                for(Message m : everyMessage) {
                    if (m.getId() == rs2.getInt(1))
                        messageHistory.add(" (" + rs2.getTimestamp(3) + ") " + rs2.getString(2));
                }
            }
            ans = "SUCCESS";
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return ans;
    }

    public String createAccount(String u, String p, String n) {
        String ans = null;

        try {
            PreparedStatement ps = conn.prepareStatement("INSERT INTO User (password, username, name, session) VALUES (?, ?, ?, ?)");
            ps.setString(1, p);
            ps.setString(2, u);
            ps.setString(3, n);
            ps.setBoolean(4, true);

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

    public String loginUser(String u, String p) {
        String ans = "FAILURE";

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM User WHERE username = ?");
            ps2.setString(1, u);

            ResultSet rs2 = ps2.executeQuery();
            rs2.next();

            if(rs2.getBoolean(5)){
                ans = "FAILURE - Utilizador já logado.";
                return ans;
            }

            PreparedStatement ps = conn.prepareStatement("UPDATE User SET session = ? WHERE username = ?");
            ps.setBoolean(1, true);
            ps.setString(2, u);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3)) && p.equalsIgnoreCase(rs.getString(2))) {
                    ans = "SUCCESS";
                    ps.executeUpdate();
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

    private String getUsernameByID(int id){
        String username = null;

        try {
            Statement stmt2 = conn.createStatement(ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet rs = stmt2.executeQuery(GET_USERS_QUERY);

            while(rs.next()){
                if (rs.getInt(1) == id){
                    username = rs.getString(3);
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return username;
    }

    private String listContacts(int id){
        String ans = "FAILURE";
        String contacto;

        listaC.clear();

        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM UserContact WHERE user_id = ? AND accepted = ?");
            ps.setInt(1, id);
            ps.setBoolean(2, true);

            ResultSet rs = ps.executeQuery();
            ResultSet rs2;
            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM User WHERE user_id = ?");


            while (rs.next()) {
                ps2.setInt(1, rs.getInt(2));
                rs2 = ps2.executeQuery();
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

    private String addContact(String u, int id, boolean accepted){
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
            PreparedStatement ps;

            ps = conn.prepareStatement("INSERT INTO UserContact (user_id, contact_id, accepted) VALUES (?, ?, ?)");

            ps.setInt(1, id);
            ps.setInt(2, contactID);
            ps.setBoolean(3, accepted);

            ps.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    private String removeContact(String u, int id){
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

            //Não testado - apagar da tabela 'message' primeiro ?
//            PreparedStatement ps = conn.prepareStatement("DELETE FROM Messagerecipient WHERE sender_id = ? AND recipient_id = ?");
//            ps.setInt(1, id);
//            ps.setInt(2, contactID);
//
//            ps.executeUpdate();
//
//            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM Messagerecipient WHERE sender_id = ? AND recipient_id = ?");
//            ps1.setInt(1, contactID);
//            ps1.setInt(2, id);
//
//            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM UserContact WHERE user_id = ? AND contact_id = ?");
            ps2.setInt(1, id);
            ps2.setInt(2, contactID);

            ps2.executeUpdate();

            PreparedStatement ps3 = conn.prepareStatement("DELETE FROM UserContact WHERE user_id = ? AND contact_id = ?");
            ps3.setInt(1, contactID);
            ps3.setInt(2, id);

            ps3.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    private String listGroups(int id){
        String ans = "FAILURE";
        String grupo;

        listaG.clear();

        try {
            ResultSet rs = stmt.executeQuery("SELECT * FROM UserInGroup WHERE group_user_id = " + id);
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

    private String joinGroup(String n, int id){
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

            PreparedStatement ps = conn.prepareStatement("INSERT INTO UserInGroup (group_group_id, group_admin, group_user_id, accepted) VALUES (?, ?, ?, ?)");
            ps.setInt(1, groupID);
            ps.setInt(2, adminID);
            ps.setInt(3, id);
            ps.setBoolean(4, adminID == id);

            ps.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    private String checkNewContactRequests(int id) {
        String ans = "FAILURE";

        try {
            ResultSet rs = stmt.executeQuery(COUNT_USER_CONTACTS_QUERY);
            rs.next();
            int sizeUC = rs.getInt(1);

            ResultSet rs2 = stmt.executeQuery(GET_CONTACTS_QUERY);

            if (sizeUC > 0) {
                while (rs2.next()) {
                    if (rs2.getInt(2) == id) {
                        if (!rs2.getBoolean(3)) {
                            System.out.println(getUsernameByID(rs2.getInt(1)));
                            pendingContactRequests.add(getUsernameByID(rs2.getInt(1)));
                            ans = "SUCCESS";
                        }
                    }
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return ans;
    }

    private String checkNewGroupRequests(int id) {
        String ans = "FAILURE";

        try {

            ResultSet rs = stmt.executeQuery(COUNT_USERS_IN_GROUP_QUERY);
            rs.next();
            int size = rs.getInt(1);

            ResultSet rs2 = stmt.executeQuery(GET_USERINGROUP_QUERY);

            if (size > 0){
                while (rs2.next()) {
                    if (id == rs2.getInt(2)) {
                        if (!rs2.getBoolean(4)){
                            pendingJoinRequests.add(getUsernameByID(rs2.getInt(3)));
                            pendingJoinRequestsGroupId.add(rs2.getInt(1));
                            ans = "SUCCESS";
                        }
                    }
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return ans;
    }

    public String acceptAllContactRequests(int id) {
        String ans = "FAILURE";

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE UserContact SET accepted = ? WHERE contact_id = ?");
            ps.setBoolean(1, true);
            ps.setInt(2, id);

            ps.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("INSERT INTO UserContact (user_id, contact_id, accepted) values(?, ? , true)");
            ps2.setInt(1, id);

            for(String s: pendingContactRequests){
                ps2.setInt(2, getIDFromDB(s));
                ps2.executeUpdate();
            }

            ans = "SUCCESS";
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ans;
    }

    public String rejectAllContactRequests(int id) {
        String ans = "FAILURE";

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM UserContact WHERE accepted = ? AND contact_id = ?");
            ps.setBoolean(1, false);
            ps.setInt(2, id);

            ps.executeUpdate();
            ans = "SUCCESS";
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ans;
    }

    public String manageContactRequests(int id, ArrayList<Integer> arr, ArrayList<String> req){
        String ans = "FALSE";

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE UserContact SET accepted = ? WHERE user_id = ? AND contact_id = ?");
            ps.setInt(2, id);

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM UserContact WHERE user_id = ? AND contact_id = ?");
            ps2.setInt(1, id);

            for (int i : arr) {
                if (i == 1){
                    ps.setBoolean(1, true);
                    ps.setInt(3, getIDFromDB(req.get(arr.indexOf(i))));
                    addContact(getUsernameByID(id), getIDFromDB(req.get(arr.indexOf(i))), true);
                    ps.executeUpdate();
                }
                else if (i == 2) {
                    ps2.setInt(2, getIDFromDB(req.get(arr.indexOf(i))));
                    ps2.executeUpdate();
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ans;
    }

    public String acceptAllGroupRequests(int id) {
        String ans = "FAILURE";

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE UserInGroup SET accepted = ? WHERE group_admin = ?");
            ps.setBoolean(1, true);
            ps.setInt(2, id);

            ps.executeUpdate();
            ans = "SUCCESS";
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ans;
    }

    public String rejectAllGroupRequests(int id) {
        String ans = "FAILURE";

        try {
            PreparedStatement ps = conn.prepareStatement("DELETE FROM UserInGroup WHERE accepted = ? AND group_admin = ?");
            ps.setBoolean(1, false);
            ps.setInt(2, id);

            ps.executeUpdate();
            ans = "SUCCESS";
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return ans;
    }

    public String manageGroupRequests(int id, ArrayList<Integer> arr, ArrayList<String> req, ArrayList<Integer> gid){
        String ans = "FALSE";

        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE UserInGroup SET accepted = ? WHERE group_user_id = ? AND group_group_id = ?");
            ps.setInt(2, id);

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM UserInGroup WHERE group_user_id = ? AND group_group_id = ?");


            for (int i : arr) {
                if (i == 1){
                    ps.setBoolean(1, true);
                    ps.setInt(2, getIDFromDB(req.get(arr.indexOf(i))));
                    ps.setInt(3, gid.get(arr.indexOf(i)));
                    addContact(getUsernameByID(id), getIDFromDB(req.get(arr.indexOf(i))), true);
                    ps.executeUpdate();
                }
                else if (i == 2) {
                    ps2.setInt(1, getIDFromDB(req.get(arr.indexOf(i))));
                    ps2.setInt(2, gid.get(arr.indexOf(i)));
                    ps2.executeUpdate();
                }
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
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
        String grupo;

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
        String membro;

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
            rs2 = stmt2.executeQuery("SELECT * FROM UserInGroup WHERE group_group_id = " + groupID);

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

    public String removeMember(String u, String n, int id){
        String ans = "FAILURE";
        //boolean encontrouID = false;
        boolean encontrouNoGrupo = false;
        contactID = 0;
        groupID = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_USERS_QUERY);

            while (rs.next()) {
                if (u.equalsIgnoreCase(rs.getString(3))) {
                    contactID = rs.getInt(1);
                    //encontrouID = true;
                    break;
                }
            }

            if(contactID == id){
                ans = "FAILURE - Não se pode remover a si próprio deste gupo.";
                return ans;
            }

            ResultSet rs2 = stmt.executeQuery(GET_GROUPS_QUERY);

            while (rs2.next()) {
                if (n.equalsIgnoreCase(rs2.getString(3)) && id == rs2.getInt(4)) {
                    groupID = rs2.getInt(1);
                    break;
                }
            }

            ResultSet rs3 = stmt.executeQuery(GET_USERINGROUP_QUERY);

            while (rs3.next()) {
                if (groupID == rs3.getInt(1) && contactID == rs3.getInt(3)) {
                    encontrouNoGrupo = true;
                    break;
                }
            }

            if(!encontrouNoGrupo){
                ans = "FAILURE - Não existe nenhum membro com este nome neste grupo.";
                return ans;
            }

            PreparedStatement ps = conn.prepareStatement("DELETE FROM UserInGroup WHERE group_group_id = ? AND group_user_id = ?");
            ps.setInt(1, groupID);
            ps.setInt(2, contactID);

            ps.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public String deleteGroup(String n, int id){
        String ans = "FAILURE";
        groupID = 0;

        try {
            ResultSet rs = stmt.executeQuery(GET_GROUPS_QUERY);

            while (rs.next()){
                if(n.equalsIgnoreCase(rs.getString(3))){
                    groupID = rs.getInt(1);
                }
            }

            PreparedStatement ps = conn.prepareStatement("DELETE FROM Message WHERE group_id = ?");
            ps.setInt(1, groupID);

            ps.execute();

            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM UserInGroup WHERE group_group_id = ?");
            ps1.setInt(1, groupID);

            ps1.executeUpdate();

            PreparedStatement ps2 = conn.prepareStatement("DELETE FROM `Group` WHERE group_id = ?");
            ps2.setInt(1, groupID);

            ps2.executeUpdate();

            ans = "SUCCESS";

        }catch(SQLException e){
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

            PreparedStatement ps = conn.prepareStatement("DELETE FROM Message WHERE User_user_id = ?");
            ps.setInt(1, id);

            ps.executeUpdate();

            PreparedStatement ps1 = conn.prepareStatement("DELETE FROM UserInGroup WHERE group_user_id = ? AND group_group_id = ?");
            ps1.setInt(1, id);
            ps1.setInt(2, groupID);

            ps1.executeUpdate();
            ans = "SUCCESS";

        }catch(SQLException e){
            System.out.println("\n" + e);
        }

        return ans;
    }

    public void logout(int id){
        try{
            PreparedStatement ps = conn.prepareStatement("UPDATE User SET session = ? WHERE user_id = ?");
            ps.setBoolean(1, false);
            ps.setInt(2, id);

            ps.executeUpdate();
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }
}
