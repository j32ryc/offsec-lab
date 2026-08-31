import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.OutputStream;
import java.net.InetSocketAddress;

public class VictimServer {
    static final Logger logger = LogManager.getLogger(VictimServer.class);

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8080), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                String ua = exchange.getRequestHeaders().getFirst("User-Agent");
                if (ua == null) ua = "unknown";
                System.out.println("[victim-app] incoming request, User-Agent = " + ua);
                logger.error("Request from User-Agent: {}", ua);
                String resp = "vulnerable log4shell demo app - see server log\n";
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(resp.getBytes());
                os.close();
            }
        });
        System.out.println("[victim-app] listening on :8080  (log4j-core 2.14.1, JDK8u181)");
        server.start();
    }
}
