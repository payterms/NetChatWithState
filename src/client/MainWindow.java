package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static javafx.application.ConditionalFeature.SWT;

public class MainWindow extends JFrame implements MessageSender {

    private JTextField textField;
    private JButton button;
    private JScrollPane scrollPane;
    private JList<Message> messageList;
    private DefaultListModel<Message> messageListModel;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JPanel panel;
    private ChatLogger logger;
    private final static int MSG_BUFFER_SIZE = 100;

    private Network network;

    public MainWindow() {
        setTitle("Сетевой чат");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(200, 200, 500, 500);
        JMenuBar mainMenu = new JMenuBar();
        setJMenuBar(mainMenu);
        JMenu fileMenu = new JMenu("Chat");
        JMenu helpMenu = new JMenu("Help");
        JMenuItem clearItem = new JMenuItem("Change nickname");
        fileMenu.add(clearItem);
        JMenuItem exitItem = new JMenuItem("Exit");
        fileMenu.add(exitItem);
        JMenuItem aboutItem = new JMenuItem("About");
        helpMenu.add(aboutItem);
        mainMenu.add(fileMenu);
        mainMenu.add(helpMenu);

        setLayout(new BorderLayout());   // выбор компоновщика элементов

        messageListModel = new DefaultListModel<>();
        messageList = new JList<>(messageListModel);
        messageList.setCellRenderer(new MessageCellRenderer());

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(messageList, BorderLayout.SOUTH);
        panel.setBackground(messageList.getBackground());
        scrollPane = new JScrollPane(panel);
        add(scrollPane, BorderLayout.CENTER);

        userListModel = new DefaultListModel<>();
        userList = new JList<>(userListModel);
        userList.setPreferredSize(new Dimension(100, 0));
        add(userList, BorderLayout.WEST);

        textField = new JTextField();
        button = new JButton("Отправить");
        button.addActionListener(new AbstractAction() {
            @Override
            public void actionPerformed(ActionEvent e) {
                String userTo = userList.getSelectedValue();
                if (userTo == null) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Не указан получатель",
                            "Отправка сообщения",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                String text = textField.getText();
                if (text == null || text.trim().isEmpty()) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Нельзя отправить пустое сообщение",
                            "Отправка сообщения",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }

                Message msg = new Message(network.getUsername(), userTo, text.trim());
                submitMessage(msg);

                textField.setText(null);
                textField.requestFocus();

                network.sendMessageToUser(msg);
            }
        });

        this.addComponentListener(new ComponentAdapter() {
            @Override
            public void componentResized(ComponentEvent evt) {
                messageList.ensureIndexIsVisible(messageListModel.size() - 1);
            }
        });

        panel = new JPanel();
        panel.setLayout(new BorderLayout());
        panel.add(button, BorderLayout.EAST);
        panel.add(textField, BorderLayout.CENTER);

        add(panel, BorderLayout.SOUTH);

        addWindowListener(new WindowAdapter() {
            @Override
            public void windowClosing(WindowEvent e) {
                if (network != null) {
                    network.close();
                }
                super.windowClosing(e);
            }
        });

        setVisible(true);

        network = new Network("localhost", 7777, this);

        LoginDialog loginDialog = new LoginDialog(this, network);
        loginDialog.setVisible(true);

        if (!loginDialog.isConnected()) {
            System.exit(0);
        }

        setTitle("Сетевой чат. Пользователь " + network.getUsername());
        Message[] msgList = new Message[MSG_BUFFER_SIZE];
        try {
            // создаем логгер
            logger = new ChatLogger("history_["+ network.getUsername() + "].txt");
            // восстанавливаем последние сообщения
            for (int i = 0; i < MSG_BUFFER_SIZE ; i++) {
                msgList[MSG_BUFFER_SIZE-i-1] = logger.restoreMsgFromLog();
                if(msgList[MSG_BUFFER_SIZE-i-1] == null) break;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        // Забрасываем подошедшие восстановленные сообщения в список для отображения
        for (int i = 0; i < MSG_BUFFER_SIZE ; i++) {
            if(msgList[i] != null){
                messageListModel.add(messageListModel.size(), msgList[i]);
            }
        }
        exitItem.addActionListener(new ExitActionListener());
        aboutItem.addActionListener(new AboutActionListener());
        clearItem.addActionListener(new ClearActionListener());

        SwingUtilities.updateComponentTreeUI(mainMenu);
    }

    @Override
    public void submitMessage(Message msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageListModel.add(messageListModel.size(), msg);
                try {
                    logger.logWriteMsg(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                messageList.ensureIndexIsVisible(messageListModel.size() - 1);
            }
        });
    }

    public void updateUserList(String usrList, updateUserListMode mode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switch (mode) {
                    case UPDATEALL: {
                        Pattern pattern = Pattern.compile("(\\w+)");
                        Matcher matcher = pattern.matcher(usrList);
                        int start = 0;
                        while (matcher.find(start)) {
                            String value = usrList.substring(matcher.start(), matcher.end());
                            if(network.getUsername().equals(value)){
                                userListModel.add(userListModel.size(), value);
                            }else{
                                userListModel.add(userListModel.size(), value);
                            }
                            System.out.println(value);
                            start = matcher.end();
                        }
                        userList.ensureIndexIsVisible(userListModel.size() - 1);
                    }
                    break;
                    case ADDUSER: {
                        if (!userListModel.contains(usrList)) {
                            userListModel.add(userListModel.size(), usrList);
                        }
                    }
                    break;
                    case DELETEUSER: {
                        if (userListModel.contains(usrList)) {
                            userListModel.remove(userListModel.indexOf(usrList));
                        }
                    }
                    break;
                    case UPDATEUSER: {
                        //if (userListModel.contains(usrList)) {
                            Pattern pattern = Pattern.compile("(\\w+)");
                            Matcher matcher = pattern.matcher(usrList);
                            int start = 0;
                            while (matcher.find(start)) {
                                String oldUsername = usrList.substring(matcher.start(), matcher.end());
                                matcher.find(matcher.end());
                                String newUsername = usrList.substring(matcher.start(), matcher.end());
                                userListModel.remove(userListModel.indexOf(oldUsername));
                                userListModel.add(userListModel.size(), newUsername);
                                System.out.println("" + oldUsername + "-> " + newUsername);
                                start = matcher.end();
                            }
                            userList.ensureIndexIsVisible(userListModel.size() - 1);
                        //}
                    }
                    break;
                }
            }
        });
    }
    private class ExitActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            System.exit(0);
        }
    }


    private class ClearActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            String oldNickName = network.getUsername();
            String newNickName;
            newNickName = JOptionPane.showInputDialog(MainWindow.this, "New nickname", oldNickName , JOptionPane.INFORMATION_MESSAGE);
            if (!newNickName.equals(oldNickName)){
                // заправшиваем сервер на изменение
                System.out.println("Query to server for new nickname: " + newNickName);
                try {
                    network.updateUsername(oldNickName,newNickName);
                } catch (AuthException ex) {
                    JOptionPane.showMessageDialog(MainWindow.this,
                            "Ошибка смены имени!",
                            "Авторизация",
                            JOptionPane.ERROR_MESSAGE);
                    return;
                }
                setTitle("Сетевой чат. Пользователь " + newNickName);
                network.setUsername(newNickName);
                try {
                    ChatLogger.logClose();// закрываем старый лог
                    logger = new ChatLogger("history_["+ network.getUsername() + "].txt");// открываем лог с новым ником
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (userListModel.contains(oldNickName)) {
                    userListModel.remove(userListModel.indexOf(oldNickName));
                    userListModel.add(userListModel.size(), newNickName);
                }


            }
            else {
                JOptionPane.showMessageDialog(MainWindow.this, "Имя пользователя совпадает с текущим!", "Внимание!", JOptionPane.WARNING_MESSAGE);
            }
        }
    }


    private class AboutActionListener implements ActionListener {
        public void actionPerformed(ActionEvent event) {
            JOptionPane.showMessageDialog(MainWindow.this, "Сетевой чат вер. 1.0", "О программе", JOptionPane.INFORMATION_MESSAGE);

        }
    }
}
