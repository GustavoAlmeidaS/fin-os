import java.net.HttpURLConnection;
import java.net.URL;

public class TestLocalhost {
    public static void main(String[] args) {
        try {
            URL url = new URL("http://localhost:11434/api/tags");
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            int status = con.getResponseCode();
            System.out.println("Ollama status: " + status);
        } catch (Exception e) {
            System.out.println("Ollama not running: " + e.getMessage());
        }
    }
}
