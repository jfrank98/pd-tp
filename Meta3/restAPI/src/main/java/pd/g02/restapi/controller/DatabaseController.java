package pd.g02.restapi.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import pd.g02.restapi.constants.Constants;
import pd.g02.restapi.data.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

@RestController
public class DatabaseController {
    private final Connection conn = Constants.CONNECTION;

    @PostMapping(value = {"change_username","change_username/{lang}"})
    public ResponseEntity changeUsername(@RequestParam("new_username") String newUsername,
                                         @RequestHeader("Authorization") String token,
                                         @PathVariable(value = "lang", required = false) String lang) {
        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        String currentUsername;

        User user = getUserInfo(token);
        if (user == null) return Constants.INVALID_TOKEN(lang);


        if ((System.currentTimeMillis() - user.getTokenCreationTime()) / 1000 >= 120) {
            SessionController.userList.remove(user);
            return Constants.OLD_TOKEN(lang);
        }
        currentUsername = user.getUsername();


        try {
            PreparedStatement ps = conn.prepareStatement("UPDATE User SET username = ? WHERE username = ?");

            ps.setString(2, currentUsername);
            ps.setString(1, newUsername);

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM User WHERE username = ?");
            ps1.setString(1, newUsername);

            if (ps1.executeQuery().next())
                return Constants.NEW_USERNAME_INVALID(lang);

            ps.executeUpdate();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        return Constants.USERNAME_CHANGED(lang);
    }

    @GetMapping(value = {"contacts", "contacts/{lang}"})
    public ResponseEntity getContacts(@RequestHeader("Authorization") String token, @PathVariable(value = "lang", required = false) String lang) {

        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        User user = getUserInfo(token);
        if (user == null) return Constants.INVALID_TOKEN(lang);

        if (user.tokenIsOld()) {
            return Constants.OLD_TOKEN(lang);
        }
        List<String> userContactsList = new ArrayList<>();

        try {

            int userId = getUserId(user.getUsername());

            PreparedStatement ps_contacts =
                    conn.prepareStatement("" +
                            "SELECT * " +
                            "FROM UserContact " +
                            "WHERE user_id = ? " +
                            "AND accepted = true"
                    );
            ps_contacts.setInt(1, userId);
            ResultSet contacts = ps_contacts.executeQuery();

            PreparedStatement psUsername = conn.prepareStatement("SELECT * FROM User WHERE user_id = ?");

            while (contacts.next()) {
                psUsername.setInt(1, contacts.getInt(2));
                ResultSet username = psUsername.executeQuery();
                username.next();
                String contact = username.getString(3);
                userContactsList.add(contact);
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (userContactsList.isEmpty()) return Constants.NO_CONTACTS(lang);
        return ResponseEntity.ok(userContactsList);
    }

    @PostMapping(value = {"delete_contact", "delete_contact/{lang}"})
    public ResponseEntity deleteContact(@RequestParam("contact") String contact,
                                        @RequestHeader("Authorization") String token,
                                        @PathVariable(value = "lang", required = false) String lang) {

        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        User user = getUserInfo(token);
        if (user == null) return Constants.INVALID_TOKEN(lang);

        if (user.tokenIsOld()) {
            return Constants.OLD_TOKEN(lang);
        }

        int userID = getUserId(user.getUsername());
        int contactID = getUserId(contact);

        if (contactID == -1) return Constants.INVALID_CONTACT(lang);

        try {
            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM UserContact");
            if (!ps1.executeQuery().next()) {
                return Constants.NO_CONTACTS(lang);
            }

            PreparedStatement ps = conn.prepareStatement("DELETE FROM UserContact WHERE user_id in (?,?) AND contact_id in (?,?) AND accepted = true");
            ps.setInt(1, contactID);
            ps.setInt(2, userID);
            ps.setInt(3, userID);
            ps.setInt(4, contactID);

            ps.executeUpdate();

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return Constants.CONTACT_DELETED(lang);
    }

    @GetMapping(value = {"groups", "groups/{lang}"})
    public ResponseEntity listGroups(@RequestHeader("Authorization") String token, @PathVariable(value = "lang", required = false) String lang) {
        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        User user = getUserInfo(token);
        if (user == null) return Constants.INVALID_TOKEN(lang);

        if (user.tokenIsOld()) {
            return Constants.OLD_TOKEN(lang);
        }

        List<String> userGroupsList = new ArrayList<>();
        int userID = getUserId(user.getUsername());

        try {
            PreparedStatement ps = conn.prepareStatement(
                    "SELECT * FROM UserInGroup WHERE group_user_id = ?",
                    ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY
            );
            ps.setInt(1, userID);

            ResultSet userInGroups = ps.executeQuery();
            if (!userInGroups.next()) return Constants.NO_GROUPS(lang);
            userInGroups.beforeFirst();

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM `Group` WHERE group_id = ?");

            while (userInGroups.next()) {
                ps1.setInt(1, userInGroups.getInt(1));
                ResultSet group = ps1.executeQuery();
                group.next();
                userGroupsList.add(group.getString(3));
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return ResponseEntity.ok(userGroupsList);
    }

    @GetMapping(value = {"messages", "messages/{lang}"})
    public ResponseEntity getMessagesContact(@RequestHeader("Authorization") String token,
                                             @RequestParam("contact") String contact,
                                             @PathVariable(value = "lang", required = false) String lang) {
        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        User user = getUserInfo(token);
        if (user == null) return Constants.INVALID_TOKEN(lang);

        if (user.tokenIsOld()) {
            return Constants.OLD_TOKEN(lang);
        }

        int userID = getUserId(user.getUsername());
        int contactID = getUserId(contact);
        if (contactID == -1) return Constants.INVALID_CONTACT(lang);

        List<String> messageHistory = new ArrayList<>();

        try {
            PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM UserContact WHERE user_id = ? AND contact_id = ? AND accepted = true", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps3.setInt(1, userID);
            ps3.setInt(2, contactID);

            ResultSet contacts = ps3.executeQuery();

            if (!contacts.next())
                return Constants.NO_CONTACTS(lang);

            contacts.beforeFirst();

            PreparedStatement ps = conn.prepareStatement("SELECT * FROM MessageRecipient WHERE recipient_id = ? AND sender_id = ?", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps.setInt(1, userID);
            ps.setInt(2, contactID);

            ResultSet allMessages = ps.executeQuery();

            PreparedStatement ps1 = conn.prepareStatement("SELECT * FROM Message ORDER BY timestamp", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ResultSet messages = ps1.executeQuery();

            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM File WHERE Message_message_id = ?");

            while (messages.next()) {
                while (allMessages.next()) {
                    if (messages.getInt(1) == allMessages.getInt(3)) {
                        ps2.setInt(1, messages.getInt(1));
                        ResultSet isfile = ps2.executeQuery();
                        if (!isfile.next())
                            messageHistory.add(messages.getString(2));
                    }
                }
                allMessages.beforeFirst();
            }
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }

        if (messageHistory.isEmpty())
            return Constants.NO_MESSAGES(lang);
        return ResponseEntity.ok(messageHistory);
    }

    @GetMapping(value = {"messages_group","messages_group/{lang}"})
    public ResponseEntity getMessagesGroup(@RequestHeader("Authorization") String token,
                                           @RequestParam("group") int group,
                                           @PathVariable(value = "lang", required = false) String lang) {
        User user = getUserInfo(token);

        if (lang == null) lang = "en";
        if (!lang.equalsIgnoreCase("en") && !lang.equalsIgnoreCase("pt")) return Constants.INVALID_LANGUAGE(lang);

        if (user == null) return Constants.INVALID_TOKEN(lang);

        if (user.tokenIsOld()) {
            return Constants.OLD_TOKEN(lang);
        }

        int userID = getUserId(user.getUsername());

        List<String> groupHistory = new ArrayList<>();

        try {
            PreparedStatement ps3 = conn.prepareStatement("SELECT * FROM UserInGroup WHERE group_group_id = ? AND accepted = true AND group_user_id = ? ", ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
            ps3.setInt(1, group);
            ps3.setInt(2, userID);

            ResultSet groupState = ps3.executeQuery();
            if (!groupState.next()) return Constants.NOT_IN_GROUP(lang);
            groupState.beforeFirst();

            PreparedStatement ps = conn.prepareStatement("SELECT * FROM Message WHERE group_id = ? AND NOT User_user_id = ?");
            ps.setInt(1, group);
            ps.setInt(2, userID);

            ResultSet rs = ps.executeQuery();

            PreparedStatement ps2 = conn.prepareStatement("SELECT * FROM File WHERE Message_message_id = ?");

            while (rs.next()) {
                ps2.setInt(1, rs.getInt(1));
                ResultSet isfile = ps2.executeQuery();
                if (!isfile.next())
                    groupHistory.add(rs.getString(3) + rs.getString(2));
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }

        if (groupHistory.isEmpty())
            return Constants.NO_GROUP_MESSAGES(lang);
        return ResponseEntity.ok(groupHistory);
    }

    private User getUserInfo(String token) {
        for (User user : SessionController.userList) {
            if (token.equalsIgnoreCase(user.getToken())) {

                return user;
            }
        }
        return null;
    }

    private int getUserId(String username) {
        try {
            PreparedStatement ps = conn.prepareStatement("SELECT * FROM User WHERE username = ?");
            ps.setString(1, username);

            ResultSet rs = ps.executeQuery();
            rs.next();

            return rs.getInt(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return -1;
    }

}
