import java.io.ObjectInputStream;
import java.net.ServerSocket;
import java.net.Socket;

/**
 * Minimal stand-in for the many real-world Java services that expose raw
 * Java serialization over a socket (old WebLogic T3, JBoss/JMX invokers,
 * RMI registries, Jenkins remoting, etc.): accept bytes, call readObject().
 * That single call is the entire vulnerability -- no gadget-specific code
 * lives here, exactly like the real thing.
 */
public class DeserializeServer {
    public static void main(String[] args) throws Exception {
        int port = 7070;
        ServerSocket server = new ServerSocket(port);
        System.out.println("[victim] raw Java deserialization service listening on :" + port);
        while (true) {
            try (Socket client = server.accept()) {
                System.out.println("[victim] connection from " + client.getRemoteSocketAddress());
                ObjectInputStream ois = new ObjectInputStream(client.getInputStream());
                Object o = ois.readObject();
                System.out.println("[victim] deserialized: " + o.getClass());
            } catch (Exception e) {
                System.out.println("[victim] deserialize error (expected for non-gadget input): " + e);
            }
        }
    }
}
