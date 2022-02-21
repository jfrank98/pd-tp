import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReceiveFileReplica implements Runnable{

    private static final int MAX_SIZE = 4096;
    private String fileName;
    private Socket serverSocket;

    public ReceiveFileReplica(String name, Socket fileSocket) {
        fileName = name;
        serverSocket = fileSocket;
    }

    @Override
    public void run() {
        System.out.println(fileName);
        File localDirectory;
        String localFilePath = null;
        InputStream in = null;
        Socket client;
        byte [] buffer = new byte[MAX_SIZE];
        FileOutputStream localFileOutputStream = null;

        localDirectory = new File(("." + File.separator + "DownloadsChat").trim());

        if(!localDirectory.exists()){
            Path path = Paths.get(".\\DownloadsChat");
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
            //System.out.println("A directoria " + localDirectory + " nao existe!");
            //return;
        }

        if(!localDirectory.isDirectory()){
            System.out.println("O caminho " + localDirectory + " nao se refere a uma directoria!");
            return;
        }

        if(!localDirectory.canWrite()){
            System.out.println("Sem permissoes de escrita na directoria " + localDirectory);
            return;
        }

        try{

            localFilePath = localDirectory.getCanonicalPath()+File.separator+fileName;
            localFileOutputStream = new FileOutputStream(localFilePath);

            in = serverSocket.getInputStream();

            int countBytes = 0;
            int nbytes;
            do{
                nbytes = in.read(buffer);

                if (nbytes > 0) {
                    localFileOutputStream.write(buffer, 0, nbytes);
                }
            }while(nbytes >= 0);

            System.out.println("Ficheiro recebido com sucesso.");
        }catch(IOException e) {
            if (localFilePath == null) {
                System.out.println("Ocorreu a excepcao {" + e + "} ao obter o caminho canonico para o ficheiro local!");
            } else {
                System.out.println("Ocorreu a excepcao {" + e + "} ao tentar criar o ficheiro " + localFilePath + "!");
            }
        } finally {
            try {
                if (serverSocket != null)
                    serverSocket.close();
                if (in != null)
                    in.close();
                if (localFileOutputStream != null)
                    localFileOutputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
