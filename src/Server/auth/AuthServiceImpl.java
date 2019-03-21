package Server.auth;

import Server.ClientHandler;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.sql.*;
import java.time.LocalDate;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

public class AuthServiceImpl implements AuthService {

    public static final String DB_URL = "jdbc:sqlite:users.db";
    public static final String DB_Driver = "org.sqlite.JDBC";
    private static final Logger LOGGER = LogManager.getLogger(AuthServiceImpl.class);

    private static Connection connection;
    public Map<String, Client> users = new HashMap<>();

    public AuthServiceImpl() {

        try {
            Class.forName(DB_Driver);
            connection = DriverManager.getConnection(DB_URL);

            Statement statement = connection.createStatement();

            for (Client client : readAllClients(statement)) {
                users.put(client.getUsername(), client);
                LOGGER.info(client);
            }
        } catch (ClassNotFoundException e) {
            e.printStackTrace(); // обработка ошибки  Class.forName
            LOGGER.error("JDBC драйвер для СУБД не найден!", e);
        } catch (SQLException e) {
            e.printStackTrace(); // обработка ошибок  DriverManager.getConnection
            LOGGER.error("Ошибка SQL !", e);
        }
    }

    private static Collection<Client> readAllClients(Statement statement) throws SQLException {
        ResultSet resultSet = statement.executeQuery("SELECT * FROM Users");

        Map<Integer, Client> clientById = new HashMap<>();
        while (resultSet.next()) {
            int id = resultSet.getInt(1);
            if (clientById.get(id) == null) {
                clientById.put(id, createClient(resultSet, id));
            }
        }
        return clientById.values();

    }

    private static Client createClient(ResultSet resultSet, int id) throws SQLException {
        String username = resultSet.getString(2);
        String password = resultSet.getString(3);
        String firstName = resultSet.getString(4);
        String lastName = resultSet.getString(5);
        LocalDate birthDate = LocalDate.parse(resultSet.getString(6));

        Client client = new Client();
        client.setId(id);
        client.setUsername(username);
        client.setPassword(password);
        client.setFirstName(firstName);
        client.setLastName(lastName);
        client.setBirthDay(birthDate);
        return client;
    }

    @Override
    public boolean authUser(String username, String password) {
        String pwd = users.get(username).getPassword();
        return pwd != null && pwd.equals(password);
    }
    @Override
    public void close() throws SQLException {
        connection.close();
    }
}
