package pd.g02.restapi.controller;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import pd.g02.restapi.data.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController
public class SessionController {
    static List<User> userList = new ArrayList<>();

    @PostMapping("session")
    public ResponseEntity login(@RequestBody(required = false) User user) {
        if (user != null) {

            try(Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/pd_chat", "root", "rootpw")) {
                PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE username = ? AND password = ?");
                ps.setString(1, user.getUsername());
                ps.setString(2, user.getPassword());

                ResultSet rs = ps.executeQuery();

                if (!rs.next()) {
                    return ResponseEntity.status(HttpStatus.CONFLICT).body("No user was found with the provided credentials.");
                }

//                PreparedStatement ps1 = conn.prepareStatement("UPDATE User SET session = ? WHERE username = ?");
//                ps1.setBoolean(1, true);
//                ps1.setString(2, user.getUsername());
//
//                ps1.executeUpdate();
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }

           // String token = user.getUsername() + "_123";
            String token = user.getUsername() + "_" + Math.round(Math.random() * (1000 - 100 + 1) + 100);

            boolean exists = false;
            for (User u : userList) {
                if (u.getUsername().equals(user.getUsername())) {
                    u.setNewToken(token);
                    u.setTokenCreationTime();
                    exists = true;
                    break;
                }
            }
            if (!exists)
                userList.add(new User(user.getUsername(), user.getPassword(), token));

            return ResponseEntity.ok(token);
        }

        return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No credentials provided!");
    }
}
