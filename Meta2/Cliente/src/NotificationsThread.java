import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class NotificationsThread extends Thread{
    private ServerSocket socket;
    private boolean inChat = false;
    private boolean inGroupChat = false;
    private String currentContact = "";
    private String currentGroup = "";
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
                } else if (type.equalsIgnoreCase("MESSAGE_GROUP")){
                    if (inGroupChat && req.getGroupName().equalsIgnoreCase(currentGroup)) {
                        System.out.println("\n" + req.getNotificationMessage());
                    } else {
                        System.out.println("\nRecebeu uma nova mensagem de " + req.getUsername() + " no grupo " + req.getGroupName());
                    }
                } else if (type.equalsIgnoreCase("FILE")){
                    System.out.println("\nNovo ficheiro \"" + req.getF().getUniqueName() + "\" disponibilizdo por " + req.getUsername());
                } else if (type.equalsIgnoreCase("FILE_GROUP")) {
                    System.out.println("\nNovo ficheiro \"" + req.getF().getUniqueName() + "\" disponibilizdo por " + req.getUsername() + " no grupo " + req.getGroupName());
                }
                System.out.print(" >> ");
            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }while(true);
    }

    public void setInChat(boolean a) { inChat = a; }

    public void setInGroupChat(boolean a) { inGroupChat = a;}

    public void setCurrentContact(String a) { currentContact = a; }

    public void setCurrentGroup(String groupName) { currentGroup = groupName; }
}
