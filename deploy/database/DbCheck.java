import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DbCheck {
    public static void main(String[] args) throws Exception {
        var properties = new Properties();
        properties.setProperty("user", "ADMIN");
        properties.setProperty("password", args.length > 1 && args[1].equals("probe") ? "ConnectionProbeNotACredential" : Files.readString(Path.of("/etc/techmind/admin-password")).strip().replace("\uFEFF", ""));
        properties.setProperty("oracle.net.CONNECT_TIMEOUT", "10000");
        properties.setProperty("oracle.jdbc.ReadTimeout", "15000");
        try (var connection = DriverManager.getConnection(args[0], properties)) {
            System.out.println("Database connection OK as ADMIN");
        } catch (SQLException error) {
            System.err.println("Database connection failed: " + error.getErrorCode() + " " + error.getMessage());
            System.exit(1);
        }
    }
}
