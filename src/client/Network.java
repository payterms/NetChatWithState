package client;

import java.io.Closeable;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Network implements Closeable {

    private static final String AUTH_PATTERN = "/auth %s %s";
    private static final String MESSAGE_SEND_PATTERN = "/w %s %s";
    private static final String USER_LIST_PATTERN = "/userlist";
    private static final String USER_CONNECTED_PATTERN = "/userconn";
    private static final String USER_DISCONN_PATTERN = "/userdissconn";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^/w (\\w+) (.+)", Pattern.MULTILINE);
    private static final Pattern NOTIFY_PATTERN = Pattern.compile("^/(\\w+) ((\\w+(\\s|$))+)", Pattern.MULTILINE);

    private Socket socket;
    private DataOutputStream out;
    private DataInputStream in;
    private final MessageSender messageSender;
    private final Thread receiver;

    private String username;
    private final String hostName;
    private final int port;

    public Network(String hostName, int port, MessageSender messageSender) {
        this.hostName = hostName;
        this.port = port;
        this.messageSender = messageSender;

        this.receiver = createReceiverThread();
    }

    private Thread createReceiverThread() {
        return new Thread(new Runnable() {
            @Override
            public void run() {
                while (!Thread.currentThread().isInterrupted()) {
                    try {
                        String text = in.readUTF();
                        System.out.println("New message " + text);
                        Matcher matcher = MESSAGE_PATTERN.matcher(text);
                        Matcher matcherNotify = NOTIFY_PATTERN.matcher(text);
                        if (matcher.matches()) {
                            Message msg = new Message(matcher.group(1), username,
                                    matcher.group(2));
                            messageSender.submitMessage(msg);
                        } else if (text.startsWith(USER_LIST_PATTERN)) {
                            // обновить список подключенных пользователей
                            if (matcherNotify.matches()) {
                                String userList = matcherNotify.group(2);
                                messageSender.updateUserList(userList, updateUserListMode.UPDATEALL);

                            }
                        } else if (text.startsWith(USER_CONNECTED_PATTERN)) {
                            if (matcherNotify.matches()) {
                                Message msg = new Message(matcherNotify.group(2), username,
                                        "Is online now");
                                messageSender.updateUserList(matcherNotify.group(2), updateUserListMode.ADDUSER);
                                messageSender.submitMessage(msg);

                            }
                        } else if (text.startsWith(USER_DISCONN_PATTERN)) {
                            if (matcherNotify.matches()) {
                                Message msg = new Message(matcherNotify.group(2), username,
                                        "Is offline now");
                                messageSender.updateUserList(matcherNotify.group(2), updateUserListMode.DELETEUSER);
                                messageSender.submitMessage(msg);

                            }
                        }
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                System.out.printf("client.Network connection is closed for user %s%n", username);
            }
        });
    }

    public void sendMessageToUser(Message message) {
        sendMessage(String.format(MESSAGE_SEND_PATTERN, message.getUserTo(), message.getText()));
    }

    private void sendMessage(String msg) {
        try {
            out.writeUTF(msg);
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void authorize(String username, String password) throws IOException {
        socket = new Socket(hostName, port);
        out = new DataOutputStream(socket.getOutputStream());
        in = new DataInputStream(socket.getInputStream());

        out.writeUTF(String.format(AUTH_PATTERN, username, password));
        String response = in.readUTF();
        if (response.equals("/auth successful")) {
            this.username = username;
            receiver.start();
            sendMessage(USER_LIST_PATTERN);
        } else {
            throw new AuthException();
        }
    }

    public String getUsername() {
        return username;
    }

    @Override
    public void close() {
        try {
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        receiver.interrupt();
        try {
            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
