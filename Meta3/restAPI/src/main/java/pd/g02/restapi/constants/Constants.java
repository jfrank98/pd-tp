package pd.g02.restapi.constants;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Constants {
    public static ResponseEntity<String> INVALID_TOKEN = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid.");
    public static ResponseEntity<String> OLD_TOKEN = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token emission over 2 minutes ago, please login again.");
    public static ResponseEntity<String> NO_GROUPS = ResponseEntity.status(HttpStatus.CONFLICT).body("You are not a member of any group.");
    public static ResponseEntity<String> INVALID_CONTACT = ResponseEntity.status(HttpStatus.CONFLICT).body("There is no user with the provided username.");
    public static ResponseEntity<String> NO_CONTACTS = ResponseEntity.status(HttpStatus.CONFLICT).body("You have no contacts.");
    public static ResponseEntity<String> NOT_ACCEPTED_CONTACT = ResponseEntity.status(HttpStatus.CONFLICT).body("Provided contact has not accepted you as their contact yet.");


    public static Connection CONNECTION;

    static {
        try {
            CONNECTION = DriverManager.getConnection("jdbc:mysql://localhost/pd_chat", "root", "rootpw");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }


}
