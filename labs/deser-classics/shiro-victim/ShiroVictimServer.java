import com.sun.net.httpserver.HttpServer;
import com.sun.net.httpserver.HttpHandler;
import com.sun.net.httpserver.HttpExchange;

import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayInputStream;
import java.io.ObjectInputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.util.Base64;
import java.util.List;

/**
 * Minimal stand-in for Apache Shiro's CookieRememberMeManager, reimplementing
 * the exact vulnerable code path behind CVE-2016-4437 (Shiro-550): decrypt
 * the "rememberMe" cookie with Shiro's well-known hardcoded default AES key,
 * then hand the decrypted bytes straight to ObjectInputStream.readObject().
 * Real Shiro does the same thing internally in
 * AbstractRememberMeManager.convertBytesToPrincipals().
 */
public class ShiroVictimServer {
    // Shiro's famous hardcoded default cipher key (public knowledge since 2016)
    static final byte[] DEFAULT_KEY = Base64.getDecoder().decode("kPH+bIxk5D2deZiIxcaaaA==");

    public static void main(String[] args) throws Exception {
        HttpServer server = HttpServer.create(new InetSocketAddress(8100), 0);
        server.createContext("/", new HttpHandler() {
            @Override
            public void handle(HttpExchange exchange) throws java.io.IOException {
                String cookieHeader = exchange.getRequestHeaders().getFirst("Cookie");
                String resp;
                String rememberMe = extractCookie(cookieHeader, "rememberMe");
                if (rememberMe == null) {
                    resp = "no rememberMe cookie set, nothing to do\n";
                } else {
                    try {
                        byte[] raw = Base64.getDecoder().decode(rememberMe);
                        byte[] iv = new byte[16];
                        byte[] ciphertext = new byte[raw.length - 16];
                        System.arraycopy(raw, 0, iv, 0, 16);
                        System.arraycopy(raw, 16, ciphertext, 0, ciphertext.length);

                        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
                        cipher.init(Cipher.DECRYPT_MODE, new SecretKeySpec(DEFAULT_KEY, "AES"), new IvParameterSpec(iv));
                        byte[] plain = cipher.doFinal(ciphertext);

                        System.out.println("[victim] rememberMe cookie decrypted OK (" + plain.length + " bytes) -- deserializing...");
                        ObjectInputStream ois = new ObjectInputStream(new ByteArrayInputStream(plain));
                        Object o = ois.readObject();
                        resp = "deserialized: " + o.getClass() + "\n";
                    } catch (Exception e) {
                        resp = "error: " + e + "\n";
                        System.out.println("[victim] " + e);
                    }
                }
                exchange.sendResponseHeaders(200, resp.getBytes().length);
                OutputStream os = exchange.getResponseBody();
                os.write(resp.getBytes());
                os.close();
            }
        });
        System.out.println("[victim] shiro-550 demo listening on :8100 (send a crafted rememberMe cookie)");
        server.start();
    }

    static String extractCookie(String header, String name) {
        if (header == null) return null;
        for (String part : header.split(";")) {
            String[] kv = part.trim().split("=", 2);
            if (kv.length == 2 && kv[0].equals(name)) return kv[1];
        }
        return null;
    }
}
