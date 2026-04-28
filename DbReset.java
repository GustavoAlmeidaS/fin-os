import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;

public class DbReset {
    public static void main(String[] args) {
        try {
            Connection conn = DriverManager.getConnection("jdbc:postgresql://localhost:5432/finos", "postgres", "bronzearte");
            Statement stmt = conn.createStatement();
            stmt.execute("DELETE FROM ai_rule_suggestion;");
            stmt.execute("DELETE FROM transaction_splits;");
            stmt.execute("DELETE FROM transactions;");
            stmt.execute("DELETE FROM import_batches;");
            stmt.execute("UPDATE accounts SET current_balance = 0;");
            System.out.println("Database reset successful.");
            conn.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
