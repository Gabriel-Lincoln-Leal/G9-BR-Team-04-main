import java.nio.file.Files;
import java.nio.file.Path;
import java.security.SecureRandom;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.HexFormat;
import java.util.Properties;

public class DbProvision {
    public static void main(String[] args) throws Exception {
        var properties = new Properties();
        properties.setProperty("user", "ADMIN");
        properties.setProperty("password", Files.readString(Path.of("/etc/techmind/admin-password")).strip().replace("\uFEFF", ""));
        properties.setProperty("oracle.net.CONNECT_TIMEOUT", "10000");
        properties.setProperty("oracle.jdbc.ReadTimeout", "30000");
        var random = new byte[20];
        new SecureRandom().nextBytes(random);
        var password = "Tm1_" + HexFormat.of().formatHex(random);
        try (var connection = DriverManager.getConnection(args[0], properties); var statement = connection.createStatement()) {
            boolean exists;
            try (var result = statement.executeQuery("SELECT COUNT(*) FROM DBA_USERS WHERE USERNAME='TECHMIND'")) {
                result.next(); exists = result.getInt(1) != 0;
            }
            statement.execute((exists ? "ALTER" : "CREATE") + " USER TECHMIND IDENTIFIED BY \"" + password + "\"");
            statement.execute("ALTER USER TECHMIND ACCOUNT UNLOCK");
            statement.execute("GRANT CREATE SESSION, CREATE TABLE, CREATE SEQUENCE TO TECHMIND");
            statement.execute("ALTER USER TECHMIND QUOTA 100M ON DATA");
            var envPath = Path.of("/etc/techmind/techmind.env");
            var env = Files.readString(envPath).replaceAll("(?m)^(DB_URL|DB_USERNAME|DB_PASSWORD|DB_DDL_AUTO|SPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE|SPRING_DATASOURCE_HIKARI_MINIMUM_IDLE|RESILIENCE4J_BULKHEAD_INSTANCES_CLASSIFICADOR_MAX_CONCURRENT_CALLS)=.*\\R?", "");
            env += "\nDB_URL='" + args[0] + "'\nDB_USERNAME=TECHMIND\nDB_PASSWORD='" + password + "'\nDB_DDL_AUTO=update\nSPRING_DATASOURCE_HIKARI_MAXIMUM_POOL_SIZE=2\nSPRING_DATASOURCE_HIKARI_MINIMUM_IDLE=1\nRESILIENCE4J_BULKHEAD_INSTANCES_CLASSIFICADOR_MAX_CONCURRENT_CALLS=1\n";
            Files.writeString(envPath, env);
            System.out.println("TECHMIND user configured; application environment saved without exposing credentials.");
        } catch (SQLException error) {
            System.err.println("Database configuration failed: " + error.getErrorCode());
            System.exit(1);
        }
    }
}
