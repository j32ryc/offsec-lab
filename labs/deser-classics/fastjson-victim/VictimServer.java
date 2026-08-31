import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;
import com.alibaba.fastjson.JSON;

import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import java.net.InetSocketAddress;

public class VictimServer {
    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8090), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                ByteArrayOutputStream buf = new ByteArrayOutputStream();
                byte[] tmp = new byte[4096];
                int n;
                while ((n = exchange.getRequestBody().read(tmp)) != -1) buf.write(tmp, 0, n);
                String body = buf.toString("UTF-8");
                System.out.println("[victim-app] POST body: " + body);
                String resp;
                try {
                    Object parsed = JSON.parseObject(body);
                    resp = "parsed: " + parsed + "\n";
                } catch (Exception e) {
                    resp = "error: " + e + "\n";
                    System.out.println("[victim-app] parse threw: " + e);
                }
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(resp.getBytes());
                os.close();
            }
        });
        System.out.println("[victim-app] fastjson 1.2.24 demo listening on :8090 (POST raw JSON)");
        server.start();
    }
}
