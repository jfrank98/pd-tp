import java.io.*;
import java.net.*;
import java.rmi.Naming;
import java.rmi.NotBoundException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.sql.*;
import java.util.*;

public class Servidor {

    public Servidor() {
    }

    public static void main(String[] args) {
        StartServer startServer = new StartServer();
        startServer.startServer(args);
    }
}
