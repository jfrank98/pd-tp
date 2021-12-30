import java.io.Serializable;
import java.net.InetAddress;

public class ClientData implements Serializable {
    private InetAddress serverAddress;
    private int serverPort;

    public static final long serialVersionUID = 1L;

    public ClientData(InetAddress serverAddress, Integer serverPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
    }

    public InetAddress getAddr() {
        return serverAddress;
    }

    public void setAddr(InetAddress addr) {
        serverAddress = addr;
    }

    public int getPort() {
        return serverPort;
    }

    public void setPort(int port) {
        serverPort = port;
    }

}
