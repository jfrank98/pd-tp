import java.util.ArrayList;
import java.util.Scanner;

public class Main {
    public static void main(String args[]) {
        Scanner sc = new Scanner(System.in);
        ArrayList<String> credentials = new ArrayList<>(2);
        int option = 0;
        User user = new User();
        if (!user.isSession()) {
            System.out.println("1 - Iniciar sessão");
            System.out.println("2 - Criar conta");
        }
        System.out.println("3 - Fechar cliente");
        System.out.println();
        System.out.print("Opção: ");
        while (!sc.hasNextInt());
            option = sc.nextInt();

        if (option == 1 && !user.isSession()) {
            getUserCredentials(credentials);
            user.login(credentials.get(0), credentials.get(1));
        }
        else if (option == 2 && !user.isSession()) {
            getUserCredentials(credentials);
            user = new User(credentials.get(0), credentials.get(1));
            user.createAccount();
        }
        else if (option == 3)
            return;
        else {
            System.out.println("Opção inexistente.");
        }

    }

    public static void getUserCredentials(ArrayList<String> cred) {
        Scanner sc = new Scanner(System.in);

        System.out.print("Username: ");
        cred.add(sc.nextLine());

        System.out.print("Password: ");
        cred.add(sc.nextLine());
    }
}
