import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NotificationsThread extends Thread{
    private ServerSocket socket;
    private boolean inChat = false;
    private String currentContact = "";
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

                String type = req.getNotificationType();

                if (type.equalsIgnoreCase("MESSAGE")) {
                    if (inChat && req.getUsername().equalsIgnoreCase(currentContact)) {
                        System.out.println("\n" + req.getNotificationMessage());
                    } else {
                        System.out.println("\nRecebeu uma nova mensagem de " + req.getUsername());
                    }
                } else if (type.equalsIgnoreCase("FILE")){
                    System.out.println("\nNovo ficheiro disponibilizdo por " + req.getContact());
                }
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }while(true);
    }

    public void setInChat(boolean a) { inChat = a; }

    public void setCurrentContact(String a) { currentContact = a; }
}
