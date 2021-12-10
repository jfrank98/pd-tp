import java.io.Serializable;
import java.net.InetAddress;

public class Servidor implements Serializable {
    private InetAddress serverAddress;
    private int listeningPort;
    private boolean online = false;
    private int id = 0;
    private double timeSinceLastMsg = 0;
    private int periods = 0;
    public static final long serialVersionUID = 1L;

    public Servidor(InetAddress addr, int port){
        serverAddress = addr;
        listeningPort = port;
        online = true;
    }

    public Servidor() {
    }

    public int getPeriods() {
        return periods;
    }

    public void setOnline(boolean online) {
        this.online = online;
    }

    public void setPeriods(int periods) {
        this.periods = periods;
    }

    public double getTimeSinceLastMsg() {
        return timeSinceLastMsg;
    }

    public void setId(int id) { this.id = id; }

    public int getId() {
        return id;
    }

    public void setTimeSinceLastMsg(long timeSinceLastMsg) {
        this.timeSinceLastMsg = timeSinceLastMsg;
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
