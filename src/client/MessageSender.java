package client;

/**
 * Интерфейс для взаимодействия класса сети {@link Network}
 * с пользовательским интерфейсом на
 */

public interface MessageSender {
    /**
     * Метод вызывается классом сети при получении нового сообщения
     * @param msg новое сообщение
     */
    void submitMessage(Message msg);

    // добавить метод для оповещения о новом пользователе
    void updateUserList(String usrList, updateUserListMode mode);

}
