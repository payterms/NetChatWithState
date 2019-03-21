package Server.auth;

public interface AuthService extends AutoCloseable {

    boolean authUser(String username, String password);
}
