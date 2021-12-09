import java.io.*;
import java.net.*;
import java.rmi.AlreadyBoundException;
import java.rmi.Naming;
import java.rmi.RemoteException;
import java.rmi.registry.LocateRegistry;
import java.rmi.server.UnicastRemoteObject;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.sql.*;

public class SGBD extends UnicastRemoteObject implements RemoteInterface {

    static final int TIMEOUT = 10000; //10 seconds

    static final int TABLE_ENTRY_TIMEOUT = 60000; //60 seconds

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    static final String DB_URL = "jdbc:mysql://localhost/pd_chat";
    static final String USER = "root";
    static final String PASS = "root";
    static final String GET_USERS_QUERY = "SELECT * FROM user;";
    static final String COUNT_USERS_QUERY = "SELECT COUNT(*) FROM user;";
    public SGBD() throws RemoteException { }

    public String createAccount(String u, String p, String n) throws RemoteException {
        String ans = null;
        String sql;
        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException ex) {
            System.out.println(ex);
        }

        try (Connection conn = DriverManager.getConnection(DB_URL, USER, PASS);
             Statement stmt = conn.createStatement (ResultSet.TYPE_SCROLL_INSENSITIVE, ResultSet.CONCUR_READ_ONLY);
             ResultSet rs = stmt.executeQuery(GET_USERS_QUERY)) {

            int size = 0;
            if (rs != null)
            {
                rs.beforeFirst();
                rs.last();
                size = rs.getRow();
            }
            if (size == 0){
                System.out.println("nice");
                sql = "INSERT INTO user (password, username, name) " +
                        "VALUES (" + "\""+ p + "\""+","+"\""+u+"\""+","+"\""+n+"\""+")";
                stmt.executeUpdate(sql);
                ans = "SUCCESS";
            }

        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return ans;
    }

        public void checkServerConnection(String s) throws RemoteException {
        System.out.println(s);
    }


    public static void main(String[] args) throws InterruptedException {
        try {
            try {
                System.out.println("Tentativa de lancamento do registry no porto 1099...");
                LocateRegistry.createRegistry(1099);
                System.out.println("Registry lancado!");
            } catch (RemoteException var2) {
                System.out.println("Registry provavelmente ja' em execucao!");
            }

            SGBD var1 = new SGBD();
            System.out.println("Servico RemoteTime criado e em execucao (" + var1.getRef().remoteToString() + "...");
            Naming.bind("rmi://127.0.0.1/chatdb", var1);
            System.out.println("Servico RemoteTime registado no registry...");
        } catch (RemoteException var3) {
            System.out.println("Erro remoto - " + var3);
            System.exit(1);
        } catch (AlreadyBoundException | MalformedURLException var4) {
            System.out.println("Erro - " + var4);
            System.exit(1);
        }

    }

}
