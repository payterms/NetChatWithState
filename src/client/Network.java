package client;

import Server.ChatServer;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.*;
import java.net.Socket;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Network implements Closeable {

    private static final String AUTH_PATTERN = "/auth %s %s";
    private static final String UPDATE_PATTERN = "/userupdate %s %s";
    private static final String MESSAGE_SEND_PATTERN = "/w %s %s";
    private static final String USER_LIST_PATTERN = "/userlist";
    private static final String USER_CONNECTED_PATTERN = "/userconn";
    private static final String USER_DISCONN_PATTERN = "/userdissconn";
    private static final String USER_UPDATE_PATTERN = "/userupdate";
    private static final Pattern MESSAGE_PATTERN = Pattern.compile("^/w (\\w+) (.+)", Pattern.MULTILINE);
    private static final Pattern NOTIFY_PATTERN = Pattern.compile("^/(\\w+) ((\\w+(\\s|$))+)", Pattern.MULTILINE);
    private static final Logger LOGGER = LogManager.getLogger(Network.class);

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
                        LOGGER.info("New message " + text);
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

                        } else if (text.startsWith(USER_UPDATE_PATTERN)) {
                            if (matcherNotify.matches()) {
                                String userList = matcherNotify.group(2);
                                LOGGER.info("Modify User Rcv");
                                messageSender.updateUserList(userList, updateUserListMode.UPDATEUSER);
                            }

                        } else if (text.equals("/upd successful")) {
                            LOGGER.info("new name is accepted by server");
                        }

                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
                LOGGER.info(String.format("client.Network connection is closed for user %s", username));
            }
        });
    }

    public void sendMessageToUser(Message message) {
        sendMessage(String.format(MESSAGE_SEND_PATTERN, message.getUserTo(), message.getText()));
    }

    private void sendMessage(String msg) {
        try {
            LOGGER.debug("Try to Send Message " + msg);
            out.writeUTF(msg);
            out.flush();
            LOGGER.debug("Message is sent");
        } catch (IOException e) {
            LOGGER.error("Send Message Err", e);
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
            LOGGER.info("Client auth successful");
            this.username = username;
            receiver.start();
            sendMessage(USER_LIST_PATTERN);
        } else {
            throw new AuthException();
        }
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public void updateUsername(String oldUsername, String newUserName) {
        try {
            LOGGER.info("Client is trying to change nickname to " + newUserName);
            out.writeUTF(String.format(UPDATE_PATTERN, oldUsername, newUserName));
            out.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }


    }

    public String getUsername() {
        return username;
    }

    @Override
    public void close() {
        try {
            LOGGER.info("Closing socket");
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        LOGGER.info("Chat receiver interrupt");
        receiver.interrupt();
        try {
            receiver.join();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }
}
