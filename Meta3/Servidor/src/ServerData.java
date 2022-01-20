import java.io.Serializable;
import java.net.InetAddress;
import java.util.ArrayList;

public class ServerData implements Serializable {
    private InetAddress serverAddress;
    private int listeningPort = 0;
    private boolean online = false;
    private int id = 0;
    private double timeSinceLastMsg = 0;
    private int periods = 0;
    private ArrayList<String> clients = new ArrayList<>();

    public static final long serialVersionUID = 1L;

    public ServerData(InetAddress addr, int port){
        serverAddress = addr;
        listeningPort = port;
        online = true;
    }

    public ServerData() {
    }

    public ArrayList<String> getClients() {
        return clients;
    }

    public void setClients(String client) {
        clients.add(client);
    }

    public void removeClient(String client) {
        clients.remove(client);
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
