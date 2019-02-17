package client;

import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainWindow extends JFrame implements MessageSender {

    private JTextField textField;
    private JButton button;
    private JScrollPane scrollPane;
    private JList<Message> messageList;
    private DefaultListModel<Message> messageListModel;
    private JList<String> userList;
    private DefaultListModel<String> userListModel;
    private JPanel panel;

    private Network network;

    public MainWindow() {
        setTitle("Сетевой чат");
        setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        setBounds(200, 200, 500, 500);

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
        // добавить класс Model для userList по аналогии с messageListModel

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
    }

    @Override
    public void submitMessage(Message msg) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                messageListModel.add(messageListModel.size(), msg);
                messageList.ensureIndexIsVisible(messageListModel.size() - 1);
            }
        });
    }

    public void updateUserList(String usrList, updateUserListMode mode) {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                switch (mode){
                    case UPDATEALL:{
                        Pattern pattern = Pattern.compile("(\\w+)");
                        Matcher matcher = pattern.matcher(usrList);
                        int start = 0;
                        while (matcher.find(start)) {
                            String value = usrList.substring(matcher.start(), matcher.end());
                            userListModel.add(userListModel.size(), value);
                            System.out.println(value);
                            start = matcher.end();
                        }
                        userList.ensureIndexIsVisible(userListModel.size() - 1);
                    }
                    break;
                    case ADDUSER:{
                        if(!userListModel.contains(usrList)){
                            userListModel.add(userListModel.size(), usrList);
                        }
                    }
                    break;
                    case DELETEUSER:{
                        if(userListModel.contains(usrList)){
                            userListModel.remove(userListModel.indexOf(usrList));
                        }
                    }
                    break;
                }
            }
        });
    }
}
