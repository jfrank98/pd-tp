import java.io.Serializable;
import java.net.InetAddress;

public class File implements Serializable {
    private String unique_name;
    private int id;
    private int message_id;
    private String name;
    private InetAddress locationAddress;
    private int locationPort;

    public static final long serialVersionUID = 1L;

    public String getUniqueName() {
        return unique_name;
    }

    public void setUniqueName(String unique_name) {
        this.unique_name = unique_name;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public int getMessageId() {
        return message_id;
    }

    public void setMessageId(int message_id) {
        this.message_id = message_id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public InetAddress getLocationAddress() {
        return locationAddress;
    }

    public void setLocationAddress(InetAddress locationAddress) {
        this.locationAddress = locationAddress;
    }

    public int getLocationPort() {
        return locationPort;
    }

    public void setLocationPort(int locationPort) {
        this.locationPort = locationPort;
    }
}
