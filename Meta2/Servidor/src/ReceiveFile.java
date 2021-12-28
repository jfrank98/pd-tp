import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class ReceiveFile implements Runnable{

    private static final int MAX_SIZE = 4096;
    private String fileName;
    private ServerSocket serverSocket;

    public ReceiveFile(String fileName, ServerSocket socket) {
        this.fileName = fileName;
        serverSocket = socket;
    }

    @Override
    public void run() {
        System.out.println(fileName);
        File localDirectory;
        String localFilePath = null;
        InputStream in;
        byte [] buffer = new byte[MAX_SIZE];
        FileOutputStream localFileOutputStream;

        localDirectory = new File(("." + File.separator + "files").trim());

        if(!localDirectory.exists()){
            System.out.println("A directoria " + localDirectory + " nao existe!");
            return;
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

            Socket client = serverSocket.accept();

            in = client.getInputStream();
            System.out.println("work?");
            int nbytes;
            do{
                nbytes = in.read(buffer);

                if (nbytes > 0) {
                    localFileOutputStream.write(buffer, 0, nbytes);
                }

            }while(nbytes > 0);

            System.out.println("Ficheiro recebido com sucesso.");

            in.close();
            localFileOutputStream.close();
        }catch(IOException e) {
            if (localFilePath == null) {
                System.out.println("Ocorreu a excepcao {" + e + "} ao obter o caminho canonico para o ficheiro local!");
            } else {
                System.out.println("Ocorreu a excepcao {" + e + "} ao tentar criar o ficheiro " + localFilePath + "!");
            }
        }
        finally {
            try {
                serverSocket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
