import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

public class ReceiveFile implements Runnable{

    private static final int MAX_SIZE = 4096;
    private String fileName;
    private ServerSocket serverSocket;
    private boolean replicateFile = false;

    public ReceiveFile(String name, ServerSocket fileSocket) {
        fileName = name;
        serverSocket = fileSocket;
    }

    @Override
    public void run() {
        System.out.println(fileName);
        File localDirectory;
        String localFilePath = null;
        InputStream in = null;
        Socket server;
        byte [] buffer = new byte[MAX_SIZE];
        FileOutputStream localFileOutputStream = null;

        localDirectory = new File(("." + File.separator + "DownloadsLocal").trim());

        if(!localDirectory.exists()){
            Path path = Paths.get(".\\DownloadsLocal");
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

            server = serverSocket.accept();

            in = server.getInputStream();


            int nbytes;
            do{
                nbytes = in.read(buffer);

                //System.out.println("bytes lidos: " + nbytes);
                if (nbytes > 0) {
                    localFileOutputStream.write(buffer, 0, nbytes);
                }
                if (nbytes == -1) break;
            }while(true);

            System.out.println("Ficheiro descarregado com sucesso.");
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
