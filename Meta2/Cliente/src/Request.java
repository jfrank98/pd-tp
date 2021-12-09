public class Request {
    private String username;
    private String password;
    private boolean session = false;
    private String request;
    public void setSession(boolean session) {
        this.session = session;
    }

    public boolean isSession() {
        return session;
    }

    public Request() {
    }

    public Request(String username, String password) {
        this.username = username;
        this.password = password;
    }

    public String getUsername() {
        return username;
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
