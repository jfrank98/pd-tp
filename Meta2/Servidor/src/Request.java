import java.io.Serializable;

public class Request implements Serializable {
    private String username;
    private String password;
    private String name;
    private boolean session = false;
    private String request;

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

    public String getUsername() {
        return username;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getPassword() {
        return password;
    }

    public String getRequest() {
        return request;
    }

    public void setRequest(String request) {
        this.request = request;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void login(String username, String password) {

    }

    public void createAccount() {
    }
}
