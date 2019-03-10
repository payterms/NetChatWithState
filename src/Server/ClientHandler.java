package Server;

import Server.auth.AuthServiceImpl;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;


public class ClientHandler {

    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^/w (\\w+) (.+)", Pattern.MULTILINE);
    private static final Pattern USER_UPDATE_PATTERN = Pattern.compile("^/userupdate (\\w+) (\\w+)$");
    private static final String USER_LIST_PATTERN = "/userlist";
    private static final String UPDATE_PATTERN = "/userupdate";
    private static final String MESSAGE_SEND_PATTERN = "/w %s %s";


    private final Thread handleThread;
    private final DataInputStream inp;
    private final DataOutputStream out;
    private final ChatServer server;
    private String username;
    private final Socket socket;

    public ClientHandler(String username, Socket socket, ChatServer server) throws IOException {
        this.username = username;
        this.socket = socket;
        this.server = server;
        this.inp = new DataInputStream(socket.getInputStream());
        this.out = new DataOutputStream(socket.getOutputStream());

        this.handleThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    while (!Thread.currentThread().isInterrupted()) {
                        String msg = inp.readUTF();
                        System.out.printf("Message from user %s: %s%n", ClientHandler.this.username, msg);

                        Matcher matcher = MESSAGE_PATTERN.matcher(msg);
                        if (matcher.matches()) {
                            String userTo = matcher.group(1);
                            String message = matcher.group(2);
                            server.sendMessage(userTo, ClientHandler.this.username, message);
                        } else if (msg.startsWith(USER_LIST_PATTERN)) {
                            List<String> usrList = server.getUserList();
                            String msgToSend = USER_LIST_PATTERN;
                            for (int i = 0; i < usrList.size(); i++) {
                                msgToSend += " " + usrList.get(i);
                            }
                            System.out.printf("Sending user list to user %s: %s%n", ClientHandler.this.username, msgToSend);
                            out.writeUTF(msgToSend);
                        } else if (msg.startsWith(UPDATE_PATTERN)) {
                            matcher = USER_UPDATE_PATTERN.matcher(msg);
                            if (matcher.matches()) {
                                String oldUsername = matcher.group(1);
                                String newUsername = matcher.group(2);
                                if (server.updateUsername(oldUsername, newUsername)) {
                                    out.writeUTF("/upd successful");
                                    out.flush();
                                    server.broadcastUserUpdate(oldUsername, newUsername);
                                    ClientHandler.this.setUsername(newUsername);

                                }else{
                                    out.writeUTF("/upd failed");
                                    out.flush();
                                }
                            }
                        }
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                } finally {
                    System.out.printf("Client %s disconnected%n", username);
                    try {
                        socket.close();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    server.unsubscribeClient(ClientHandler.this);
                }
            }
        });
        handleThread.start();
    }

    public void sendMessage(String userFrom, String msg) throws IOException {
        out.writeUTF(String.format(MESSAGE_SEND_PATTERN, userFrom, msg));
    }

    public void notifyUser(String msg) throws IOException {
        out.writeUTF(msg);
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
}
