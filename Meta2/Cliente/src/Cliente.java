import java.io.*;
import java.io.File;
import java.net.*;
import java.util.ArrayList;
import java.util.Scanner;

public class Cliente implements Runnable {
    public static final int MAX_SIZE = 1024;
    public static final String ADDR_PORT_REQUEST = "GET_ADDR_PORT_TCP";
    public static final String SERVER_REQUEST = "SERVER_REQUEST";
    private static Socket socket = null;
    private static Request request = new Request();
    private static DatagramSocket SocketGRDS = null;
    private static InetAddress AddrGRDS, serverAddress = null;
    private static int PortGRDS, serverPort;
    private static ObjectInputStream oin, oinS;
    private static ObjectOutputStream oout;
    private static boolean getRequests = false;
    private static boolean newServer = false;
    private int lastContactReqSize = 0;
    private int lastGroupReqSize = 0;
    private static boolean inChat = false;
    private static int lastMessageHistorySize = 0;

    public static void main(String args[]) {
        DatagramPacket packet;
        ServerData server;
        ByteArrayInputStream bin;
        boolean connected = false;
        String ans;

        //Verifica se recebeu os argumentos necessários: endereço IP e porto de escuta do GRDS
        if (args.length != 2) {
            System.out.println("Sintaxe: java Cliente serverAddress serverUdpPort");
            return;
        }

        //Tenta contactar o GRDS para receber os dados de um servidor ativo
        try {

            //Cria um DatagramSocket e guarda o IP e porto de escuta do GRDS
            SocketGRDS = new DatagramSocket();
            AddrGRDS = InetAddress.getByName(args[0]);
            PortGRDS = Integer.parseInt(args[1]);
            SocketGRDS.setSoTimeout(5000);

            request.setMessage(ADDR_PORT_REQUEST);

            byte [] data = serialize(request);
            //Cria um DatagramPacket e envia-o ao GRDS através do DatagramSocket criado anteriormente
            packet = new DatagramPacket(data, data.length, AddrGRDS, PortGRDS);
            SocketGRDS.send(packet);

            //Limpa o packet e recebe resposta do GRDS
            packet.setData(new byte[MAX_SIZE], 0, MAX_SIZE);
            SocketGRDS.receive(packet);


            //Lê o packet com os dados de um servidor ativo e cria um objeto

            server = (ServerData) deserialize(packet.getData());

            System.out.println("\nEndereço IP: " + server.getServerAddress().toString());
            System.out.println("Porto de escuta: " + server.getListeningPort());

        } catch (UnknownHostException e) {
            System.out.println("\nDestino desconhecido:\n\t" + e);
            return;
        } catch (NumberFormatException e) {
            System.out.println("\nO porto de escuta do servidor deve ser um inteiro positivo:\n\t" + e);
            return;
        } catch (SocketTimeoutException e) {
            System.out.println("\nNão foi recebida qualquer resposta do GRDS:\n\t" + e);
            return;
        } catch (SocketException e) {
            System.out.println("\nOcorreu um erro ao nível do socket:\n\t" + e);
            return;
        } catch (IOException e) {
            System.out.println("\nOcorreu um erro no acesso ao socket:\n\t" + e);
            return;
        } catch (NullPointerException e) {
            System.out.println("\nNão foi encontrado um servidor ativo:\n\t" + e);
            return;
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
            return;
        }

        Scanner sc = new Scanner(System.in);
        ArrayList<String> credentials = new ArrayList<>(3);
        int option = 0;
        int option2 = 0;
        int option3 = 0;

        //Tenta conectar-se ao servidor
        try {

            serverAddress = server.getServerAddress();
            serverPort = server.getListeningPort();

            //Cria um Socket para comuninar com o servidor
            socket = new Socket(serverAddress, serverPort);

            oout = new ObjectOutputStream(socket.getOutputStream());
            oinS = new ObjectInputStream(socket.getInputStream());

            //Cria socket dedicado a notificações
            ServerSocket notificationsSocket = new ServerSocket(0);
            new NotificationsThread(notificationsSocket).start();
            ClientData clientData = new ClientData(serverAddress, serverPort, socket.getLocalAddress(), socket.getLocalPort());
            clientData.setClientNotifSocketAddressPort(notificationsSocket.getInetAddress(), notificationsSocket.getLocalPort());
            request.addConnectedClient(clientData);

            request.setMessage("UPDATE_CLIENT_LIST");

            byte [] data = serialize(request);
            packet.setData(data, 0, data.length);
            SocketGRDS.send(packet);

            //Envia pedido ao servidor
            request.setMessage(SERVER_REQUEST);
            oout.writeUnshared(request);

            request = (Request) oinS.readObject();

            System.out.println(request.getMessage() + "\n");
            System.out.println("My port: " + SocketGRDS.getLocalPort() + " tcp port: " + socket.getLocalPort());
//            Runnable r = new Cliente();
//            new Thread(r).start();



            while (true) {
                newServer = true;

                if (!request.isSession()) {
                    System.out.println("\n1 - Iniciar sessão");
                    System.out.println("2 - Criar conta");
                } else {
                    System.out.println("\n\n1 - Contactos");
                    System.out.println("2 - Grupos");
                    System.out.println("3 - Definições");
                }

                System.out.println("0 - Sair");
                System.out.print("\nOpção: ");


                while (!sc.hasNextInt()) ;
                option = sc.nextInt();

                if (request.isSession()) {

                    if (option == 1) {
                        //Contactos
                        do {
                            System.out.println("\n\n1 - Lista de contactos");
                            System.out.println("2 - Adicionar contacto");
                            System.out.println("3 - Eliminar contacto");
                            System.out.println("4 - Pedidos de contacto pendentes");
                            System.out.println("5 - Conversas privadas");
                            System.out.println("0 - Voltar");

                            System.out.print("\nOpção: ");
                            while (!sc.hasNextInt()) {
                                System.out.println("\nOpção inválida");
                                sc.nextLine();
                            }
                            option2 = sc.nextInt();

                            if (option2 == 1) {
                                //Ver lista de contactos
                                request.setMessage("LIST_CONTACTS");

                                //Tentar enviar pedido de LIST_CONTACTS ao servidor
                                if (sendMessage(request, oout) == 0) continue;


                                request = (Request) oinS.readObject();

                                if (request.getListaContactos().size() == 0) {
                                    System.out.println("\nNão tem contactos na sua lista.\n");
                                } else {
                                    System.out.println("\nLista de contactos:");
                                    for (String contacto : request.getListaContactos()) {
                                        System.out.println("-" + contacto);
                                    }
                                }

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 == 2) {
                                //Adicionar contacto
                                request.setMessage("ADD_CONTACT");
                                request.setContact(getNewUsername());

                                //Tentar enviar pedido de ADD_CONTACT ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 == 3) {
                                //Eliminar contacto
                                request.setMessage("REMOVE_CONTACT");
                                request.setContact(getNewUsername());

                                //Tentar enviar pedido de REMOVE_CONTACT ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 == 4) {
                                //Pedidos pendentes
                                request.setMessage("GET_PENDING_CONTACT_REQUESTS");
                                request.getPendingContactRequests().clear();

                                if (sendMessage(request, oout) == 0) continue;
                                request = (Request) oinS.readObject();

                                if (request.getPendingContactRequests().isEmpty()) {
                                    System.out.println("\nNão tem pedidos de contacto pendentes.");
                                    continue;
                                }

                                if (!request.getPendingContactRequests().isEmpty()) {
                                    System.out.println("\nPedidos de contacto pendentes:");
                                    for (String s : request.getPendingContactRequests()) {
                                        System.out.println("-" + s);
                                    }
                                    request.getAcceptRejectIgnoreRequests().clear();

                                    System.out.println("\n1 - Aceitar pedido");
                                    System.out.println("2 - Rejeitar pedido");
                                    System.out.println("3 - Aceitar todos");
                                    System.out.println("4 - Rejeitar todos");
                                    System.out.println("0 - Voltar");

                                    System.out.print("\nOpção: ");
                                    while (!sc.hasNextInt()) ;

                                    option = sc.nextInt();

                                    if (option == 1) {
                                        request.setMessage("ACCEPT_CONTACT_REQUEST");
                                        request.setContact(getNewUsername());
                                    } else if (option == 2) {
                                        request.setMessage("REJECT_CONTACT_REQUEST");
                                        request.setContact(getNewUsername());
                                    } else if (option == 3) {
                                        request.setMessage("ACCEPT_ALL_CONTACT_REQUESTS");
                                    } else if (option == 4) {
                                        request.setMessage("REJECT_ALL_CONTACT_REQUESTS");
                                    } else if (option == 0) {
                                        continue;
                                    } else {
                                        System.out.println("\nOpção inválida.");
                                        continue;
                                    }

                                    if (sendMessage(request, oout) == 0) continue;

                                    request = (Request) oinS.readObject();
                                    System.out.println("\n" + request.getMessage());
                                }
                            }
                            else if (option2 == 5) {
                                //Ver chat privado
                                request.setMessage("LIST_CONTACTS");

                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();

                                if (request.getListaContactos().size() == 0) {
                                    System.out.println("\nNão tem contactos na sua lista.");
                                }
                                else {
                                    System.out.println("\nConversas privadas: ");
                                    int i = 1;
                                    for (String contacto : request.getListaContactos()) {
                                        System.out.println(i + " - " + contacto);
                                        i++;
                                    }

                                    System.out.println("0 - Sair");
                                    System.out.print("\nOpção: ");

                                    while (!sc.hasNextInt()) {
                                        System.out.println("\nOpção inválida");
                                        sc.nextLine();
                                    }
                                    int opt = sc.nextInt();

                                    if(opt == 0){
                                        continue;
                                    }

                                    request.getHistoricoMensagens().clear();
                                    request.setContact(request.getListaContactos().get(opt - 1));
                                    request.setMessage("GET_MESSAGES_FROM");
                                    if (sendMessage(request, oout) == 0) continue;

                                    request = (Request) oinS.readObject();

                                    inChat = true;
                                    boolean enteredChat = true;
                                    String fileName = null;

                                    do {
                                        if (enteredChat) {
                                            System.out.println("\n\nNota: utilize os comandos !sendfile e !getfile para enviar e receber ficheiros e !sair para sair da conversa privada.\n");

                                            lastMessageHistorySize = request.getHistoricoMensagens().size();
                                            for (String message : request.getHistoricoMensagens()) {
                                                System.out.println(message);
                                            }
                                            enteredChat = false;
                                            //Runnable r = new Cliente();
                                            //new Thread(r).start();
                                        }
                                        //Thread.sleep(500);

                                        //não aceita numeros !!
                                        System.out.print(" >> ");
                                        while (!sc.hasNextLine());

                                        String msg = sc.nextLine();

                                        if (msg.equalsIgnoreCase("!sendfile")) {
                                            request.setSendFile(true);
                                            System.out.print("Nome do ficheiro a enviar: ");
                                            while (!sc.hasNextLine());
                                            fileName = sc.nextLine();

                                            request.getF().setName(fileName);
                                            //request.setMessageContent("--- Ficheiro \"" + fileName + "\" enviado por " + request.getUsername() + " ---");
                                        }
                                        else if (msg.equalsIgnoreCase("!getfile")) {
                                            request.setReceiveFile(true);
                                            System.out.print("Nome do ficheiro a receber: ");
                                            while (!sc.hasNextLine());
                                            fileName = sc.nextLine();
                                            Runnable r = new ReceiveFile(fileName, socket);
                                            new Thread(r).start();
                                        }
                                        else if (msg.equalsIgnoreCase("!sair")) {
                                            inChat = false;
                                            break;
                                        }
                                        else {
                                            request.setMessageContent(sc.nextLine());
                                        }
                                        request.setMessage("SEND_MESSAGE");

                                        if (sendMessage(request, oout) == 0) continue;

                                        request = (Request) oinS.readObject();

                                        if (request.isSendFile()) {
                                            Socket socket = new Socket(request.getFileSocketAddress(), request.getFileSocketPort());

                                            Runnable r = new SendFile(fileName, socket);
                                            new Thread(r).start();
                                            request.setSendFile(false);
                                        }

                                    } while (true);
                                }

                            }
                            else if (option2 != 0) {
                                //Opção inválida
                                System.out.println("\nOpção inválida.");
                            }
                        } while (option2 != 0);
                    }
                    else if (option == 2) {
                        //Grupos
                        do {
                            System.out.println("\n1 - Meus grupos");
                            System.out.println("2 - Aderir a grupo");
                            System.out.println("3 - Criar grupo");
                            System.out.println("4 - Editar grupo");
                            System.out.println("5 - Pedidos de adesão pendentes");
                            System.out.println("6 - Sair de um grupo");
                            System.out.println("0 - Voltar");

                            System.out.print("\nOpção: ");

                            while (!sc.hasNextInt()) {
                                System.out.println("\nOpção inválida");
                                sc.nextLine();
                            }
                            option2 = sc.nextInt();

                            if (option2 == 1) {
                                //Listar os meus grupos
                                request.setMessage("LIST_GROUPS");

                                //Tentar enviar pedido de LIST_GROUPS ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();

                                if (request.getListaGrupos().size() == 0) {
                                    System.out.println("\n\nNão pertence a nenhum grupo.\n");
                                } else {
                                    System.out.println("\n\nOs meus grupos:");
                                    for (String grupo : request.getListaGrupos()) {
                                        System.out.println("-" + grupo);
                                    }
                                    System.out.println();
                                }

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 == 2) {
                                //Aderir a um grupo
                                request.setMessage("JOIN_GROUP");
                                request.setGroupName(getNewGroupName());

                                //Tenta enviar pedido de JOIN_GROUP ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 == 3) {
                                //Criar grupo
                                request.setMessage("CREATE_GROUP");
                                request.setGroupName(getNewGroupName());

                                //Tenta enviar pedido de CREATE_GROUP ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 == 4) {
                                //Editar grupo
                                //limpar listas
                                request.getListaGruposAdmin().clear();
                                request.getListaMembros().clear();

                                //Listar os grupos dos quais é admin
                                request.setMessage("LIST_ADMIN_GROUPS");

                                //Tentar enviar pedido de LIST_ADMIN_GROUPS ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();

                                if (request.getListaGruposAdmin().size() == 0) {
                                    System.out.println("\n\nNão é administrador de nenhum grupo.\n");
                                    continue;
                                } else {
                                    System.out.println("\n\nAdministrador dos grupos:");
                                    for (String grupo : request.getListaGruposAdmin()) {
                                        System.out.println("-" + grupo);
                                    }
                                }

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }

                                //Listar membros do grupo
                                request.setMessage("LIST_MEMBERS_GROUP");
                                request.setGroupName(getNewGroupName());

                                //Tentar enviar pedido de LIST_MEMBERS_GROUP ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();

                                if (request.getMessage().equals("FAILURE - Não é administrador de nenhum grupo com esse nome.")) {
                                    System.out.println("\n" + request.getMessage());
                                    continue;
                                } else {
                                    System.out.println("\n\nMembros do grupo '" + request.getGroupName() + "':");

                                    for (String membro : request.getListaMembros()) {
                                        System.out.println("-" + membro);
                                    }
                                    System.out.println();
                                }

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }

                                do {
                                    System.out.println("\n\n1 - Alterar nome do grupo");
                                    System.out.println("2 - Remover membro");
                                    System.out.println("3 - Eliminar grupo");
                                    System.out.println("0 - Voltar");

                                    System.out.print("\nOpção: ");
                                    while (!sc.hasNextInt()) ;
                                    option3 = sc.nextInt();

                                    if (option3 == 1) {
                                        //Alterar nome do grupo
                                        request.setMessage("CHANGE_GROUP_NAME");
                                        request.setOldGroupName(request.getGroupName());
                                        request.setGroupName(getNewGroupName());

                                        //Tenta enviar pedido de CHANGE_GROUP_NAME ao servidor
                                        if (sendMessage(request, oout) == 0) continue;

                                        request = (Request) oinS.readObject();
                                        System.out.println("\n" + request.getMessage());

                                        if (request.getMessage().equals("FAILURE - Já tem um grupo com esse nome.")) {
                                            request.setGroupName(request.getOldGroupName());
                                        } else if (request.getMessage().equals("SERVER_OFF")) {
                                            getNewServer();
                                        }
                                        break;
                                    } else if (option3 == 2) {
                                        //Remover membro
                                        request.setMessage("REMOVE_MEMBER");
                                        request.setContact(getNewUsername());

                                        //Tentar enviar pedido de REMOVE_MEMBER ao servidor
                                        if (sendMessage(request, oout) == 0) continue;

                                        request = (Request) oinS.readObject();
                                        System.out.println("\n" + request.getMessage());

                                        if (request.getMessage().equals("SERVER_OFF")) {
                                            getNewServer();
                                        }
                                        break;
                                    } else if (option3 == 3) {
                                        //Eliminar grupo
                                        ans = getConfirmation();

                                        if (ans.equalsIgnoreCase("N")) {
                                            continue;
                                        } else if (ans.equalsIgnoreCase("S")) {
                                            request.setMessage("DELETE_GROUP");

                                            //Tentar enviar pedido de DELETE_GROUP ao servidor
                                            if (sendMessage(request, oout) == 0) continue;

                                            request = (Request) oinS.readObject();
                                            System.out.println("\n" + request.getMessage());

                                            if (request.getMessage().equals("SERVER_OFF")) {
                                                getNewServer();
                                            }
                                        } else {
                                            System.out.println("\nResposta inválida.");
                                            continue;
                                        }
                                        break;
                                    } else if (option3 != 0) {
                                        //Opção inválida
                                        System.out.println("\nOpção inválida.");
                                    }
                                } while (option3 != 0);
                            }
                            else if (option2 == 5) {
                                request.setMessage("GET_PENDING_GROUP_REQUESTS");
                                request.getPendingJoinRequests().clear();

                                if (sendMessage(request, oout) == 0) continue;
                                request = (Request) oinS.readObject();

                                if (request.getPendingJoinRequests().isEmpty()) {
                                    System.out.println("\nNão tem pedidos de adesão pendentes.");
                                    continue;
                                }

                                if (request.getMessage().equalsIgnoreCase("SUCCESS")) {
                                    if (request.isGroupOwner()) {
                                        request.getAcceptRejectIgnoreRequests().clear();

                                        System.out.println("\nPedidos de adesão pendentes:");
                                        for (String n : request.getPendingJoinRequests()) {
                                            System.out.println("-" + n);
                                        }

                                        System.out.println("\n1 - Aceitar pedido");
                                        System.out.println("2 - Rejeitar pedido");
                                        System.out.println("3 - Aceitar todos");
                                        System.out.println("4 - Rejeitar todos");
                                        System.out.println("0 - Voltar");

                                        System.out.print("\nOpção: ");
                                        while (!sc.hasNextInt()) ;

                                        option = sc.nextInt();

                                        if (option == 1) {
                                            request.setMessage("ACCEPT_GROUP_REQUEST");
                                            request.setContact(getNewUsername());
                                        }
                                        else if (option == 2) {
                                            request.setMessage("REJECT_GROUP_REQUEST");
                                            request.setContact(getNewUsername());
                                        }
                                        else if (option == 3) {
                                            request.setMessage("ACCEPT_ALL_GROUP_REQUESTS");
                                        }
                                        else if (option == 4) {
                                            request.setMessage("REJECT_ALL_GROUP_REQUESTS");
                                        }
                                        else if (option == 0) {
                                            continue;
                                        }
                                        else {
                                            System.out.println("\nOpção inválida.");
                                            continue;
                                        }
                                    }
                                    if (sendMessage(request, oout) == 0) continue;

                                    request = (Request) oinS.readObject();
                                    System.out.println("\n" + request.getMessage());
                                }
                            }
                            else if (option2 == 6) {
                                //Sair de um grupo
                                request.setMessage("LEAVE_GROUP");
                                request.setGroupName(getNewGroupName());

                                //Tentar enviar pedido de LEAVE_GROUP ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            }
                            else if (option2 != 0) {
                                //Opção inválida
                                System.out.println("\nOpção inválida.");
                            }
                        } while (option2 != 0);
                    }
                    else if (option == 3) {
                        //Definições
                        do {
                            System.out.println("\n\n1 - Alterar username");
                            System.out.println("2 - Alterar password");
                            System.out.println("0 - Voltar");

                            System.out.print("\nOpção: ");
                            while (!sc.hasNextInt()) ;
                            option2 = sc.nextInt();

                            if (option2 == 1) {
                                //Alterar username
                                request.setMessage("CHANGE_USERNAME");
                                request.setOldUsername(request.getUsername());
                                request.setUsername(getNewUsername());

                                //Tenta enviar pedido de CHANGE_USERNAME ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("FAILURE - Esse username já está a ser usado")) {
                                    request.setUsername(request.getOldUsername());
                                } else if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            } else if (option2 == 2) {
                                //Alterar password
                                request.setMessage("CHANGE_PASSWORD");
                                request.setOldPassword(request.getPassword());
                                request.setPassword(getNewPassword());

                                //Tenta enviar pedido de CHANGE_PASSWORD ao servidor
                                if (sendMessage(request, oout) == 0) continue;

                                request = (Request) oinS.readObject();
                                System.out.println("\n" + request.getMessage());

                                if (request.getMessage().equals("FAILURE")) {
                                    request.setPassword(request.getOldPassword());
                                } else if (request.getMessage().equals("SERVER_OFF")) {
                                    getNewServer();
                                }
                            } else if (option2 != 0) {
                                //Opção inválida
                                System.out.println("\nOpção inválida.");
                            }
                        } while (option2 != 0);
                    }
                    else if (option == 0) {
                        //Sair
                        SocketGRDS.close();
                        socket.close();
                        return;
                    }
                    else {
                        //Opção inválida
                        System.out.println("\nOpção inválida.\n");
                    }
                }
                else {
                    if (option == 1) {
                        //Login
                        request.setMessage("LOGIN");
                        getUserCredentials(credentials, request.getMessage());
                        request.setUsername(credentials.get(0));
                        request.setPassword(credentials.get(1));
                        request.setAddress(socket.getInetAddress().toString());
                        request.setPort(socket.getPort());

                        //Tenta enviar pedido de LOGIN ao servidor
                        if (sendMessage(request, oout) == 0) continue;

                        request = (Request) oinS.readObject();
                        System.out.println("\n" + request.getMessage());

                        if (request.getMessage().equals("SUCCESS")) {
                            request.setSession(true);
                        } else if (request.getMessage().equals("SERVER_OFF")) {
                            getNewServer();
                        }
                    } else if (option == 2) {
                        //Criar conta
                        request.setMessage("CREATE_ACCOUNT");
                        getUserCredentials(credentials, request.getMessage());
                        request.setUsername(credentials.get(0));
                        request.setPassword(credentials.get(1));
                        request.setName(credentials.get(2));
                        request.setAddress(socket.getLocalAddress().toString());
                        request.setPort(socket.getLocalPort());

                        //Tenta enviar pedido de CREATE_ACCOUNT ao servidor
                        if (sendMessage(request, oout) == 0) continue;

                        request = (Request) oinS.readObject();
                        System.out.println("\n" + request.getMessage() + "\n");

                        if (request.getMessage().equals(null)) {
                            System.out.println("\nErro ao tentar criar conta.\n");
                        } else if (request.getMessage().equals("SERVER_OFF")) {
                            getNewServer();
                        } else if (request.getMessage().equals("SUCCESS")) {
                            request.setSession(true);
                        }
                    } else if (option == 0) {
                        //Sair
                        SocketGRDS.close();
                        socket.close();
                        return;
                    } else {
                        //Opção inválida
                        System.out.println("\nOpção inválida.\n");
                    }
                }
            }
        } catch (SocketTimeoutException e) {

        } catch (IOException | ClassNotFoundException e) {
            e.printStackTrace();
        } /*catch (InterruptedException e) {
            e.printStackTrace();
        }*/
    }


    public static synchronized int sendMessage(Request req, ObjectOutputStream oout) throws IOException {
        try {
            oout.writeUnshared(req);
            return 1;
        } catch (SocketException e) {
            System.out.println("\nLigação com o servidor perdida.\nA procurar novo servidor...");
            try {
                Thread.sleep(2000);

                if (!getNewServer()) {
                    System.out.println("\nNão foi possível encontrar um novo servidor/o GRDS fechou.\nA fechar o cliente...\n");
                    Thread.sleep(2000);
                    System.exit(0);
                }

            } catch (InterruptedException interruptedException) {
                interruptedException.printStackTrace();
            }
            return 0;
        }
    }

    public static void getUserCredentials(ArrayList<String> cred, String message) {
        cred.clear();
        Scanner sc = new Scanner(System.in);

        System.out.print("\nUsername: ");
        cred.add(sc.nextLine());

        System.out.print("Password: ");
        cred.add(sc.nextLine());

        if (message.equalsIgnoreCase("CREATE_ACCOUNT")) {
            System.out.print("Nome: ");
            cred.add(sc.nextLine());
        }
    }

    public static String getNewUsername() {
        Scanner sc = new Scanner(System.in);

        if (request.getMessage().equalsIgnoreCase("ADD_CONTACT") || request.getMessage().equalsIgnoreCase("REMOVE_CONTACT") || request.getMessage().equalsIgnoreCase("REMOVE_MEMBER"))
            System.out.print("\nNome contacto: ");
        else
            System.out.print("\nNovo username: ");

        return sc.nextLine();
    }

    public static String getNewPassword() {
        Scanner sc = new Scanner(System.in);

        System.out.print("\nNova password: ");
        return sc.nextLine();
    }

    public static String getNewGroupName() {
        Scanner sc = new Scanner(System.in);

        if (request.getMessage().equalsIgnoreCase("LIST_MEMBERS_GROUP"))
            System.out.print("\nGrupo a editar: ");
        else if (request.getMessage().equalsIgnoreCase("CHANGE_GROUP_NAME"))
            System.out.print("\nNovo nome para o grupo: ");
        else
            System.out.print("\nNome do grupo: ");

        return sc.nextLine();
    }

    public static String getConfirmation() {
        Scanner sc = new Scanner(System.in);

        System.out.println("\nTem a certeza que pretende eliminar este grupo? (S/N)");
        return sc.nextLine();
    }

    public static boolean getNewServer() {
        ObjectInputStream oin;
        DatagramPacket packet;
        ByteArrayInputStream bin;
        ServerData newServer;
        int attempt = 0;

        try {
            socket.close();
            SocketGRDS.setSoTimeout(5000);
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

        while (attempt != 3) {
            try {
                request.setMessage(ADDR_PORT_REQUEST);
                byte [] data = serialize(request);

                packet = new DatagramPacket(data, data.length, AddrGRDS, PortGRDS);
                SocketGRDS.send(packet);

                packet.setData(new byte[MAX_SIZE], 0, MAX_SIZE);

                try {
                    SocketGRDS.receive(packet);
                } catch (SocketTimeoutException e) {
                    attempt++;
                    System.out.println("\nNão foi possível conectar ao GRDS. Tentativas restantes: " + (3 - attempt));
                    continue;
                }

                newServer = (ServerData) deserialize(packet.getData());

                if (newServer.getListeningPort() != 0) {
                    System.out.println("\nNovo endereço IP: " + newServer.getServerAddress().toString());
                    System.out.println("Novo porto de escuta: " + newServer.getListeningPort());
                    socket = new Socket(newServer.getServerAddress(), newServer.getListeningPort());

                    oout = new ObjectOutputStream(socket.getOutputStream());
                    oinS = new ObjectInputStream(socket.getInputStream());

                    request.setMessage(SERVER_REQUEST);
                    oout.writeUnshared(request);
                    oout.flush();

                    request = (Request) oinS.readObject();

                    System.out.println(request.getMessage() + "\n");
                    return true;
                }

            } catch (IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    @Override
    public void run() {
        while (inChat) {
            int historySize = request.getHistoricoMensagens().size();
            final ArrayList<String> message = request.getHistoricoMensagens();
            if (historySize > lastMessageHistorySize) {
                int index = historySize - lastMessageHistorySize;
                for (int i = index; i < historySize; i++) {
                    System.out.println(message.get(i) + "\n");
                }
                lastMessageHistorySize = historySize;
            }
        }
        while (!inChat) {

        }
        /*
        while(true) {
            try {
                    Thread.sleep(10000);
                    request.setMessage("GET_PENDING_CONTACT_REQUESTS");
                    if (sendMessage(request, oout) == 0) break;

                    synchronized(oinS) {
                        request = (Request) oinS.readObject();
                    }

                    if (request.getPendingContactRequests().size() > lastContactReqSize) {
                        lastContactReqSize = request.getPendingContactRequests().size();
                        System.out.println("----- Novo pedido de contacto -----");
                    }

                    request.setMessage("GET_PENDING_GROUP_REQUESTS");
                    if (sendMessage(request, oout) == 0) break;

                    synchronized(oinS) {
                        request = (Request) oinS.readObject();
                    }

                    if (request.getPendingJoinRequests().size() > lastGroupReqSize) {
                        lastGroupReqSize = request.getPendingJoinRequests().size();
                        System.out.println("----- Novo pedido de entrada em grupo -----");
                    }
            } catch (InterruptedException | IOException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        }*/
    }
    public static byte[] serialize(Object obj) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ObjectOutputStream os = new ObjectOutputStream(out);
        os.writeObject(obj);
        return out.toByteArray();
    }

    public static Object deserialize(byte[] data) throws IOException,   ClassNotFoundException {
        ByteArrayInputStream in = new ByteArrayInputStream(data);
        ObjectInputStream is = new ObjectInputStream(in);
        return is.readObject();
    }
}
