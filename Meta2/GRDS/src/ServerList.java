import java.io.Serializable;
import java.util.ArrayList;

public class ServerList implements Serializable {

    public static final long serialVersionUID = 1L;

    private ArrayList<ServerData> serverData = new ArrayList<>();

    public ArrayList<ServerData> getServerData() { return serverData; }

}
