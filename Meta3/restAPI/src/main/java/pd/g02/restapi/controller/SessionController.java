package pd.g02.restapi.controller;

import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pd.g02.restapi.constants.Constants;
import pd.g02.restapi.data.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController
public class SessionController {
    static List<User> userList = new ArrayList<>();

    @PostMapping(value = {"session", "session/{lang}"})
    public ResponseEntity<String> login(@RequestBody(required = false) User user, @PathVariable(value = "lang", required = false) String lang) {
        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        if (user == null) return Constants.NO_CREDENTIALS(lang);

        try (Connection conn = DriverManager.getConnection("jdbc:mysql://localhost/pd_chat", "root", "rootpw")) {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE username = ? AND password = ?");
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());

            ResultSet rs = ps.executeQuery();

            if (!rs.next()) {
                return Constants.USER_NOT_FOUND(lang);
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

        //Número de 3 digitos aleatório a seguir ao username para alguma "proteção"
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

        return ResponseEntity.ok("Token: " + token);
    }
}
