package pd.g02.restapi.data;

import java.time.Instant;

public class User {
    private String username;
    private String password;
    private String token;
    private Long tokenCreationTime;
    public User(String username, String password, String token) {
        this.username = username;
        this.password = password;
        this.token = "Bearer " + token;
        tokenCreationTime = System.currentTimeMillis();
    }

    public Long getTokenCreationTime() { return tokenCreationTime; }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getToken() {
        return token;
    }

    public boolean tokenIsOld() {
        return ((System.currentTimeMillis() - tokenCreationTime) / 1000 >= 120);
    }

    public void setTokenCreationTime() {
        tokenCreationTime = System.currentTimeMillis();
    }

    public void setNewToken(String token) {
        this.token = "Bearer " + token;
    }
}
