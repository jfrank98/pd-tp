import java.io.Serializable;
import java.util.ArrayList;

public class Request implements Serializable {
    private int id;
    private String username;
    private String password;
    private String name;
    private String oldUsername;
    private String oldPassword;
    private String groupName;
    private String oldGroupName;
    private String Contact;
    private boolean session = false;
    private String message;
    private boolean serverIsOnline = false;
    private ArrayList<String> listaContactos = new ArrayList<>();
    private ArrayList<String> listaGrupos = new ArrayList<>();
    private ArrayList<String> listaGruposAdmin = new ArrayList<>();
    private ArrayList<String> listaMembros = new ArrayList<>();
    public static final long serialVersionUID = 1L;

    public Request(String s, String s1) {
        username = s;
        password = s1;
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

    public void createAccount() {
    }
}
