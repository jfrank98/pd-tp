import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.sql.*;

public class SGBD {

    static final int TIMEOUT = 10000; //10 seconds

    static final int TABLE_ENTRY_TIMEOUT = 60000; //60 seconds

    static final String JDBC_DRIVER = "com.mysql.cj.jdbc.Driver";
    //static final String DB_URL = "jdbc:mysql://localhost/pd_chat";
    //static final String USER = "root";
//static final String PASS = "root";
    static final String GET_WORKERS_QUERY = "SELECT * FROM pi_workers;";

    private static int getWorkers(String sgbdAddress, String bdName, String user, String pass, List<Socket> workers) {

        int id;
        String workerName;
        int workerPort;
        Timestamp timestamp;
        Socket socketToWorker;

        workers.clear();

        String dbUrl = "jdbc:mysql://"+sgbdAddress+"/"+bdName;

        try {
            Class.forName(JDBC_DRIVER);
        } catch (ClassNotFoundException ex) {
            System.out.println(ex);
        }

        /**
         * - Estabeleca uma liga��o com o servidor de base de dados indicado.
         *   Use as credenciais e a base de dados fornecidas.
         * - Invoque a query definida em GET_WORKERS_QUERY para obter a lista de workers
         *   registados na tabela pi_workers da BD
         */
        try(Connection conn = DriverManager.getConnection(dbUrl);
            Statement stmt = conn.createStatement();
            ResultSet rs = stmt.executeQuery(GET_WORKERS_QUERY)) {

            /* Para cada registo devolvido... */
            while (rs.next()) {

                try {

                    /**
                     * Extraia o valor de cada coluna/campo
                     */
                    id = rs.getInt("id");
                    workerName = rs.getString("address");
                    workerPort = rs.getInt("port");
                    timestamp = rs.getTimestamp("timestamp");

                    System.out.println("> DB entry: [" + workerName + ":" + workerPort + "]");

                    long elapsedTime = Calendar.getInstance().getTimeInMillis() - timestamp.getTime();
                    //System.out.println("Timestamp: " +  timestamp);
                    System.out.println("\t... Entry created/updated " + elapsedTime/1000 + " seconds ago");

                    if(elapsedTime > TABLE_ENTRY_TIMEOUT){
                        /*System.out.println("\t... Entry will be ignored!");
                        continue;*/
                        System.out.println("\t... Entry will be deleted!");
                        try(Statement stmt2 = conn.createStatement()){
                            /* Elimine da tabela pi_workers o registo identificando-o atrav�s do campo id */
                            stmt2.executeUpdate("delete * from pi_workers where id = " + id); //Se usarmos stmt, rs � encerrado, o que resulta numa excepcao no while
                        }
                        continue;
                    }

                    System.out.print("> Connecting to worker " + (workers.size() + 1));
                    System.out.println(" [" + workerName + ":" + workerPort + "]... ");

                    socketToWorker = new Socket(workerName, workerPort);
                    socketToWorker.setSoTimeout(TIMEOUT);
                    workers.add(socketToWorker);
                    System.out.println("\t... connection established!");

                }catch (IOException e) {
                    System.out.println("\r\n> Cannot connect to host!\r\n\t " + e + "\r\n");
                }

            } //while

        } catch (SQLException ex) {
            System.out.println(ex);
        }

        return workers.size();
    }

    public static void main(String[] args) throws InterruptedException {
        long nIntervals;

        List<Socket> workers = new ArrayList<>();
        ObjectOutputStream output;
        ObjectInputStream input;

        int i, nWorkers;
        double workerResult;
        double pi = 0;

        Calendar t1, t2;

        System.out.println();

        if (args.length != 5) {
            System.out.println("> Syntax: java ParallelPi <number of intrevals> "
                    + "<SGBD address> <BD name> <usename> <password>");
            return;
        }

        nIntervals = Long.parseLong(args[0]);

        t1 = GregorianCalendar.getInstance();
        nWorkers = getWorkers(args[1], args[2], args[3], args[4], workers);

        if (nWorkers <= 0) {
            return;
        }

        try {

            for (i = 0; i < nWorkers; i++) {
                output = new ObjectOutputStream(workers.get(i).getOutputStream());
                output.writeObject(new RequestToWorker(i + 1, nWorkers, nIntervals));
                output.flush();
            }

            System.out.println();

            for (i = 0; i < nWorkers; i++) {
                input = new ObjectInputStream(workers.get(i).getInputStream());
                workerResult = (Double) input.readObject();
                System.out.println("> Worker " + (i + 1) + ": " + workerResult);
                pi += workerResult;
            }

        } catch (IOException e) {
            System.err.println("> Erro ao aceder ao socket\r\n\t" + e);
        } catch (ClassNotFoundException e) {
            System.err.println("> Recebido objecto de tipo inesperado\r\n\t" + e);
        } finally {
            for (Socket s : workers) {
                try {
                    s.close();
                } catch (IOException e) {
                }
            }

            workers.clear();
        }

        t2 = GregorianCalendar.getInstance();

        System.out.println();
        System.out.println("> Valor aproximado do pi: " + pi + " (calculado em "
                + (t2.getTimeInMillis() - t1.getTimeInMillis()) + " msec.)");

    }
}
