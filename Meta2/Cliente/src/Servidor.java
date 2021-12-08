import java.io.Serializable;
import java.net.InetAddress;

public class Servidor implements Serializable {
    private InetAddress serverAddress;
    private int listeningPort;
    private boolean online = false;

    public static final long serialVersionUID = 1L;

    public Servidor(InetAddress addr, int port){
        serverAddress = addr;
        listeningPort = port;
        online = true;
    }

    public Servidor() {
    }

    public InetAddress getServerAddress() {
        return serverAddress;
    }

    public int getListeningPort() {
        return listeningPort;
    }

    public boolean isOnline() {
        return online;
    }
}
