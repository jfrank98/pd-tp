import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;

public class Request implements Serializable {
    private int id = -1;
    private String username;
    private String password;
    private String name;
    private String oldUsername;
    private String oldPassword;
    private String groupName;
    private String oldGroupName;
    private String Contact;
    private boolean session = false;
    private boolean groupOwner = false;
    private String message;
    private ArrayList<String> listaContactos = new ArrayList<>();
    private ArrayList<String> listaGrupos = new ArrayList<>();
    private ArrayList<String> listaGruposAdmin = new ArrayList<>();
    private ArrayList<String> listaMembros = new ArrayList<>();
    private ArrayList<String> pendingJoinRequests = new ArrayList<>();
    private ArrayList<Integer> pendingJoinRequestsGroupId = new ArrayList<>();
    private ArrayList<String> pendingContactRequests = new ArrayList<>();
    private ArrayList<Integer> acceptRejectIgnoreRequests = new ArrayList<>();
    private int port = 0;
    private String address;
    private ArrayList<String> historicoMensagens = new ArrayList<>();
    private ArrayList<String> historicoGrupo = new ArrayList<>();
    private ArrayList<ClientData> clientsToNotify = new ArrayList<>();
    private ArrayList<ClientData> connectedClients = new ArrayList<>();
    private String messageContent;
    private File f = new File();
    private InetAddress fileSocketAddress;
    private int fileSocketPort;
    private boolean receiveFile;
    private boolean sendFile;
    private ClientData userToNotify;
    private String notificationMessage;
    private InetAddress notificationSocketAddress;
    private int notificationSocketPort;
    private String notificationType;

    public String getNotificationType() {
        return notificationType;
    }

    public void setNotificationType(String notificationType) {
        this.notificationType = notificationType;
    }

    public static final long serialVersionUID = 1L;

    public ArrayList<ClientData> getConnectedClients() {
        return connectedClients;
    }

    public void addConnectedClient(ClientData connectedClient) {
        connectedClients.add(connectedClient);
    }

    public InetAddress getNotificationSocketAddress() {
        return notificationSocketAddress;
    }

    public void setNotificationSocketAddress(InetAddress notificationSocketAddress) {
        this.notificationSocketAddress = notificationSocketAddress;
    }

    public int getNotificationSocketPort() {
        return notificationSocketPort;
    }

    public void setNotificationSocketPort(int notificationSocketPort) {
        this.notificationSocketPort = notificationSocketPort;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public InetAddress getFileSocketAddress() {
        return fileSocketAddress;
    }

    public void setFileSocketAddress(InetAddress fileSocketAddress) {
        this.fileSocketAddress = fileSocketAddress;
    }

    public int getFileSocketPort() {
        return fileSocketPort;
    }

    public void setFileSocketPort(int fileSocketPort) {
        this.fileSocketPort = fileSocketPort;
    }

    public Request(String s, String s1) {
        username = s;
        password = s1;
    }

    public File getF() {
        return f;
    }

    public void setF(File f) {
        this.f = f;
    }

    public void setAcceptRejectIgnoreRequests(Integer i){ acceptRejectIgnoreRequests.add(i); }

    public ArrayList<Integer> getAcceptRejectIgnoreRequests() {
        return acceptRejectIgnoreRequests;
    }

    public boolean isGroupOwner() {
        return groupOwner;
    }

    public void setGroupOwner(boolean groupOwner) {
        this.groupOwner = groupOwner;
    }

    public void setSession(boolean session) {
        this.session = session;
    }

    public boolean isSession() {
        return session;
    }

    public Request() {
    }

    public Request(String username, String password, String name) {
        this.username = username;
        this.password = password;
        this.name = name;
    }

    public int getID() {return id; }

    public String getUsername() {
        return username;
    }

    public String getMessageContent() {
        return messageContent;
    }

    public void setMessageContent(String messageContent) {
        this.messageContent = messageContent;
    }

    public String getName() {
        return name;
    }

    public String getOldUsername() { return oldUsername; }

    public String getOldPassword() {return oldPassword; }

    public String getPassword() {
        return password;
    }

    public String getMessage() {
        return message;
    }

    public String getGroupName() { return groupName; }

    public String getOldGroupName() { return oldGroupName; }

    public String getContact() { return Contact; }

    public ArrayList<String> getListaContactos() { return listaContactos; }

    public ArrayList<String> getListaGrupos() { return listaGrupos; }

    public ArrayList<String> getListaGruposAdmin() { return  listaGruposAdmin; }

    public ArrayList<String> getListaMembros() { return listaMembros; }

    public ArrayList<String> getHistoricoGrupo() { return historicoGrupo; }

    public ArrayList<String> getHistoricoMensagens() { return historicoMensagens; }

    public void setID(int id) { this.id = id; }

    public void setName(String name) {
        this.name = name;
    }

    public void setMessage(String request) {
        this.message = request;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public ArrayList<Integer> getPendingJoinRequestsGroupId() {
        return pendingJoinRequestsGroupId;
    }

    public void setOldUsername(String oldUsername) { this.oldUsername = oldUsername; }

    public void setOldPassword(String oldPassword) {this.oldPassword = oldPassword; }

    public void setGroupName(String groupName) { this.groupName = groupName; }

    public void setOldGroupName(String oldGroupName) { this.oldGroupName = oldGroupName; }

    public void setContact(String username) { this.Contact = username; }

    public void login(String username, String password) {

    }

    public void addContactSuccess(String newContact) {
        listaContactos.add(newContact);
    }

    public void removeContactSuccess(String contact) { listaContactos.remove(contact); }

    public void addGroupSuccess(String newGroup) { listaGrupos.add(newGroup); }

    public void leaveGroupSuccess(String group) { listaGrupos.remove(group); }

    public void addGroupAdminSuccess(String groupName) { listaGruposAdmin.add(groupName); }

    public void deleteGroupSuccess(String groupName) { listaGruposAdmin.remove(groupName); }

    public void addMemberSuccess(String member) { listaMembros.add(member); }

    public void deleteMemberSuccess(String member) { listaMembros.remove(member); }

    public void addMessageSuccess(String message) { historicoMensagens.add(message); }

    public void addGroupHistorySuccess(String message) { historicoGrupo.add(message); }

    public void setPendingJoinRequests(String s){
        pendingJoinRequests.add(s);
    }

    public void setPendingContactRequests(String s){
        pendingContactRequests.add(s);
    }

    public ArrayList<String> getPendingJoinRequests() {
        return pendingJoinRequests;
    }

    public ArrayList<String> getPendingContactRequests() {
        return pendingContactRequests;
    }

    public void setPendingJoinRequestsGroupId(int a) {
        pendingJoinRequestsGroupId.add(a);
    }

    public boolean isSendFile() {
        return sendFile;
    }

    public void setSendFile(boolean file) {
        this.sendFile = file;
    }

    public boolean isReceiveFile() {
        return receiveFile;
    }

    public void setReceiveFile(boolean file) {
        this.receiveFile = file;
    }

    public void setUserToNotify(ClientData userAffectedByNotification) {
        userToNotify = userAffectedByNotification;
        clientsToNotify.add(userAffectedByNotification);
    }

    public void setNotificationMessage(String s) {
        notificationMessage = s;
    }

    public String getNotificationMessage() { return notificationMessage; }

    public ArrayList<ClientData> getClientsToNotify() {
        return clientsToNotify;
    }
}