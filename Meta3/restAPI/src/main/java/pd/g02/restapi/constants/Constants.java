package pd.g02.restapi.constants;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;

public class Constants {
    //Respostas em Inglês
    public static ResponseEntity<String> INVALID_TOKEN = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token is invalid.");
    public static ResponseEntity<String> OLD_TOKEN = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token emission over 2 minutes ago, please login again.");
    public static ResponseEntity<String> NO_GROUPS = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are not a member of any group.");
    public static ResponseEntity<String> INVALID_CONTACT = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is no user with the provided username.");
    public static ResponseEntity<String> NO_CONTACTS = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You have no contacts.");
    public static ResponseEntity<String> NOT_IN_GROUP = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("You are not a member of the provided group.");
    public static ResponseEntity<String> USER_NOT_FOUND = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("No user was found with the provided credentials.");
    public static ResponseEntity<String> NO_CREDENTIALS = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("No credentials provided!");
    public static ResponseEntity<String> USERNAME_CHANGED = ResponseEntity.ok("Username changed successfully.");
    public static ResponseEntity<String> CONTACT_DELETED = ResponseEntity.ok("Contact deleted with success.");
    public static ResponseEntity<String> NO_MESSAGES = ResponseEntity.status(HttpStatus.CONFLICT).body("You don't have any messages.");
    public static ResponseEntity<String> NO_GROUP_MESSAGES = ResponseEntity.status(HttpStatus.CONFLICT).body("There are no messages from the provided group.");
    public static ResponseEntity<String> NEW_USERNAME_INVALID = ResponseEntity.status(HttpStatus.CONFLICT).body("Provided new username already exists.");

    //Respostas em Português
    public static ResponseEntity<String> INVALID_TOKEN_PT = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido.");
    public static ResponseEntity<String> OLD_TOKEN_PT = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Emissão do token ocorreu há mais de 2 minutos, por favor inicie sessão de novo.");
    public static ResponseEntity<String> NO_GROUPS_PT = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é membro de nenhum grupo.");
    public static ResponseEntity<String> INVALID_CONTACT_PT = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não existem utilizadores com o username fornecido.");
    public static ResponseEntity<String> NO_CONTACTS_PT = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não tem contactos.");
    public static ResponseEntity<String> NOT_IN_GROUP_PT = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não é membro do grupo fornecido.");
    public static ResponseEntity<String> USER_NOT_FOUND_PT = ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Não foi encontrado um utilizador com as credencias fornecidas.");
    public static ResponseEntity<String> NO_CREDENTIALS_PT = ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Credenciais não fornecidas!");
    public static ResponseEntity<String> USERNAME_CHANGED_PT = ResponseEntity.ok("Username modificado com sucesso.");
    public static ResponseEntity<String> CONTACT_DELETED_PT = ResponseEntity.ok("Contacto apagado com sucesso.");
    public static ResponseEntity<String> NO_MESSAGES_PT = ResponseEntity.status(HttpStatus.CONFLICT).body("Não recebeu nenhuma mensagem.");
    public static ResponseEntity<String> NO_GROUP_MESSAGES_PT = ResponseEntity.status(HttpStatus.CONFLICT).body("Não há mensagens do grupo fornecido.");
    public static ResponseEntity<String> NEW_USERNAME_INVALID_PT = ResponseEntity.status(HttpStatus.CONFLICT).body("Novo username já está a ser utilizado.");

    public static Connection CONNECTION;

    static {
        try {
            CONNECTION = DriverManager.getConnection("jdbc:mysql://localhost/pd_chat", "root", "rootpw");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    }

    public static ResponseEntity<String> NEW_USERNAME_INVALID(String lang) {
        return lang.equalsIgnoreCase("pt") ? NEW_USERNAME_INVALID_PT : NEW_USERNAME_INVALID;
    }

    public static ResponseEntity<String> NO_GROUP_MESSAGES(String lang) {
        return lang.equalsIgnoreCase("pt") ? NO_GROUP_MESSAGES_PT : NO_GROUP_MESSAGES;
    }

    public static ResponseEntity<String> NO_MESSAGES(String lang) {
        return lang.equalsIgnoreCase("pt") ? NO_MESSAGES_PT : NO_MESSAGES;
    }

    public static ResponseEntity<String> CONTACT_DELETED(String lang) {
        return lang.equalsIgnoreCase("pt") ? CONTACT_DELETED_PT : CONTACT_DELETED;
    }

    public static ResponseEntity<String> INVALID_TOKEN(String lang) {
        return lang.equalsIgnoreCase("pt") ? INVALID_TOKEN_PT : INVALID_TOKEN;
    }

    public static ResponseEntity<String> USER_NOT_FOUND(String lang) {
        return lang.equalsIgnoreCase("pt") ? USER_NOT_FOUND_PT : USER_NOT_FOUND;
    }

    public static ResponseEntity<String> NO_CREDENTIALS(String lang) {
        return lang.equalsIgnoreCase("pt") ? NO_CREDENTIALS_PT : NO_CREDENTIALS;
    }

    public static ResponseEntity<String> OLD_TOKEN(String lang) {
        return lang.equalsIgnoreCase("pt") ? OLD_TOKEN_PT : OLD_TOKEN;
    }

    public static ResponseEntity<String> USERNAME_CHANGED(String lang) {
        return lang.equalsIgnoreCase("pt") ? USERNAME_CHANGED_PT : USERNAME_CHANGED;
    }

    public static ResponseEntity<String> NO_GROUPS(String lang) {
        return lang.equalsIgnoreCase("pt") ? NO_GROUPS_PT : NO_GROUPS;
    }

    public static ResponseEntity<String> INVALID_CONTACT(String lang) {
        return lang.equalsIgnoreCase("pt") ? INVALID_CONTACT_PT : INVALID_CONTACT;
    }

    public static ResponseEntity<String> NO_CONTACTS(String lang) {
        return lang.equalsIgnoreCase("pt") ? NO_CONTACTS_PT : NO_CONTACTS;
    }

    public static ResponseEntity<String> NOT_IN_GROUP(String lang) {
        return lang.equalsIgnoreCase("pt") ? NOT_IN_GROUP_PT : NOT_IN_GROUP;
    }

    public static ResponseEntity<String> INVALID_LANGUAGE(String lang) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("There is no language \"" + lang + "\" avaiable.");
    }
}
