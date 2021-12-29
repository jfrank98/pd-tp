import java.io.File;
import java.io.*;
import java.net.Socket;

public class SendFile implements Runnable{
    private static final int MAX_SIZE = 4096;
    private String fileName;
    private Socket serverSocket;
    public SendFile(String fileName, Socket socket) {
        this.fileName = fileName;
        serverSocket = socket;
    }

    @Override
    public void run() {
        File localDirectory;
        String CanonicalFilePath = null;
        FileInputStream fileInputStream;
        byte []fileChunk = new byte[MAX_SIZE];
        int nbytes;
        localDirectory = new File("./".trim());

        if(!localDirectory.exists()){
            System.out.println("A directoria " + localDirectory + " nao existe!");
            return;
        }

        if(!localDirectory.isDirectory()){
            System.out.println("O caminho " + localDirectory + " nao se refere a uma directoria!");
            return;
        }

        if(!localDirectory.canRead()){
            System.out.println("Sem permissoes de leitura na directoria " + localDirectory);
            return;
        }

        try {
            CanonicalFilePath = new File(localDirectory + File.separator + fileName).getCanonicalPath();

            if (!CanonicalFilePath.startsWith(localDirectory.getCanonicalPath() + File.separator)) {
                System.out.println("Nao e' permitido aceder ao ficheiro " + CanonicalFilePath + "!");
                System.out.println("A directoria de base nao corresponde a " + localDirectory.getCanonicalPath() + "!");
                return;
            }

            fileInputStream = new FileInputStream(CanonicalFilePath);
            OutputStream out = serverSocket.getOutputStream();
            do {
                nbytes = fileInputStream.read(fileChunk);

                if (nbytes > 0) {
                    out.write(fileChunk, 0, nbytes);
                    out.flush();
                }
                    /*try {
                        Thread.sleep(1);
                    } catch (InterruptedException ex) {}*/
            } while (nbytes > 0);


        } catch (FileNotFoundException e) {
            System.out.println("Ficheiro nao encontrado na diretoria local.");
        } catch (IOException e) {
            if(CanonicalFilePath == null){
                System.out.println("Ocorreu a excepcao {" + e +"} ao obter o caminho canonico para o ficheiro local!");
            }else{
                System.out.println("Ocorreu a excepcao {" + e +"} ao tentar criar o ficheiro " + CanonicalFilePath + "!");
            }
        }
    }

}
