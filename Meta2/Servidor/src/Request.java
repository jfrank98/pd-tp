import java.io.Serializable;

public class Request implements Serializable {
    private int id;
    private String username;
    private String password;
    private String name;
    private String oldUsername;
    private String groupName;
    private String newContact;
    private boolean session = false;
    private String message;
    private boolean serverIsOnline = false;

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

    public String getPassword() {
        return password;
    }

    public String getMessage() {
        return message;
    }

    public String getGroupName() { return groupName; }

    public String getNewContact() { return newContact; }

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

    public void setNewContact(String username) { this.newContact = username; }

    public void login(String username, String password) {

    }

    public void createAccount() {
    }
}
