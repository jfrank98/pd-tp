import java.io.IOException;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class SendNotification extends Thread{
    private ClientData cli;
    private Request notificationMessage;
    public SendNotification(Request msg, ClientData cli) {
        this.cli = cli;
        notificationMessage = msg;
    }

    @Override
    public void run() {
        try {
            Socket s = new Socket(cli.getNotifSocketAddress(), cli.getNotifSocketPort());

            ObjectOutputStream out = new ObjectOutputStream(s.getOutputStream());

            out.writeUnshared(notificationMessage);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
