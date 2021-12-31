import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NotificationsThread extends Thread{
    private ServerSocket socket;

    public NotificationsThread(ServerSocket notificationsSocket) {
        socket = notificationsSocket;
    }

    @Override
    public void run() {
        do{
            try {
                Socket nextNotification = socket.accept();

                ObjectInputStream in = new ObjectInputStream(nextNotification.getInputStream());

                Request req = (Request) in.readObject();

                System.out.println("\n" + req.getNotificationMessage());
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }while(true);
    }
}
