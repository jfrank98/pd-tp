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




//    public void run() {
//        //System.out.println("Comando: ");
//        Scanner sc = new Scanner(System.in);
//        while(true){
//
//            command = sc.nextLine();
//
//            ObjectOutputStream out;
//
//            if (command.equalsIgnoreCase("Y") || grdsClosed) {
//                if (listClientSockets.size() > 0) {
//                    for (Socket cli : listClientSockets) {
//                        try {
//                            out = new ObjectOutputStream(cli.getOutputStream());
//                            if (grdsClosed){
//                                out.writeUnshared("GRDS_CLOSED");
//                            }
//                            else {
//                                out.writeUnshared("SERVER_OFF");
//                            }
//                            Thread.sleep(2000);
//                            listeningSocket.close();
//                        } catch (IOException | InterruptedException e) {
//                            e.printStackTrace();
//                        }
//                    }
//                }
//                return;
//            }
//            else {
//                if (command == "") continue;
//                System.out.println("Comando: ");
//            }
//        }
//    }

}
