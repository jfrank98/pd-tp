import java.io.Serializable;
import java.net.InetAddress;
import java.net.Socket;

public class ClientData implements Serializable {
    private InetAddress serverAddress, clientAddress, notifSocketAddress;
    private int serverPort, clientPort, notifSocketPort;
    public static final long serialVersionUID = 1L;

    public ClientData(InetAddress serverAddress, Integer serverPort, InetAddress clientAddress, Integer clientPort) {
        this.serverAddress = serverAddress;
        this.serverPort = serverPort;
        this.clientAddress = clientAddress;
        this.clientPort = clientPort;
    }

    public ClientData() {

    }

    public InetAddress getNotifSocketAddress() {
        return notifSocketAddress;
    }

    public int getNotifSocketPort() {
        return notifSocketPort;
    }

    public InetAddress getServerAddress() {
        return serverAddress;
    }

    public int getPort() {
        return serverPort;
    }

    public int getClientPort() { return clientPort; }

    public InetAddress getClientAddress() { return clientAddress; }

    public void setClientNotifSocketAddressPort(InetAddress addr, int port) {
        notifSocketAddress = addr;
        notifSocketPort = port;
    }
}
