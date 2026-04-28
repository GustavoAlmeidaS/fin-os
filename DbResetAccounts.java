import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbResetAccounts {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/finos", "postgres", "bronzearte");
            Statement stmt = conn.createStatement();
            stmt.execute("UPDATE accounts SET current_balance = 0;");
            System.out.println("Accounts balance reset successful.");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
