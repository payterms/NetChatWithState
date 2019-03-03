package Server;

import Server.auth.AuthService;
import Server.auth.AuthServiceImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.sql.SQLException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class ChatServer {

    private static final String USER_CONNECTED_PATTERN = "/userconn %s";
    private static final String USER_DISCONN_PATTERN = "/userdissconn %s";
    private static final Pattern AUTH_PATTERN = Pattern.compile("^/auth (\\w+) (\\w+)$");

    private AuthService authService;

    private Map<String, ClientHandler> clientHandlerMap = Collections.synchronizedMap(new HashMap<>());

    public ChatServer() throws SQLException, ClassNotFoundException {
        this.authService = new AuthServiceImpl();
    }


    public static void main(String[] args) throws SQLException, ClassNotFoundException {
        ChatServer chatServer = new ChatServer();
        chatServer.start(7777);
    }

    public void start(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server started!");
            while (true) {
                Socket socket = serverSocket.accept();
                DataInputStream inp = new DataInputStream(socket.getInputStream());
                DataOutputStream out = new DataOutputStream(socket.getOutputStream());
                System.out.println("New client connected!");

                try {
                    String authMessage = inp.readUTF();
                    Matcher matcher = AUTH_PATTERN.matcher(authMessage);
                    if (matcher.matches()) {
                        String username = matcher.group(1);
                        String password = matcher.group(2);
                        if (authService.authUser(username, password)) {
                            clientHandlerMap.put(username, new ClientHandler(username, socket, this));
                            out.writeUTF("/auth successful");
                            out.flush();
                            broadcastUserConnected(username);

                            System.out.printf("Authorization for user %s successful%n", username);
                        } else {
                            System.out.printf("Authorization for user %s failed%n", username);
                            out.writeUTF("/auth fails");
                            out.flush();
                            socket.close();
                        }
                    } else {
                        System.out.printf("Incorrect authorization message %s%n", authMessage);
                        out.writeUTF("/auth fails");
                        out.flush();
                        socket.close();
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void sendMessage(String userTo, String userFrom, String msg) throws IOException {
        ClientHandler userToClientHandler = clientHandlerMap.get(userTo);
        if (userToClientHandler != null) {
            userToClientHandler.sendMessage(userFrom, msg);
        } else {
            System.out.printf("User %s not found. client.Message from %s is lost.%n", userTo, userFrom);
        }
    }

    public List<String> getUserList() {
        return new ArrayList<>(clientHandlerMap.keySet());
    }

    public void unsubscribeClient(ClientHandler clientHandler) {
        clientHandlerMap.remove(clientHandler.getUsername());
        broadcastUserDisconnected(clientHandler.getUsername());
    }


    public void broadcastUserConnected(String username) {
        // сообщать о том, что конкретный пользователь подключился
        Iterator it = clientHandlerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(!pair.getKey().equals(username)){
                ClientHandler userToClientHandler = clientHandlerMap.get(pair.getKey());
                System.out.println("Send notify to " + pair.getKey());
                try {
                    userToClientHandler.notifyUser(String.format(USER_CONNECTED_PATTERN, username));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void broadcastUserDisconnected(String username) {
        // сообщать о том, что конкретный пользователь отключился
        Iterator it = clientHandlerMap.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            if(!pair.getKey().equals(username)){
                ClientHandler userToClientHandler = clientHandlerMap.get(pair.getKey());
                System.out.println("Send notify to " + pair.getKey());
                try {
                    userToClientHandler.notifyUser(String.format(USER_DISCONN_PATTERN, username));
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
}
