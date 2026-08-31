import java.io.FileInputStream;
import java.io.ObjectInputStream;

public class DeserializeVictim {
    public static void main(String[] args) throws Exception {
        String file = args.length > 0 ? args[0] : "payload.bin";
        System.out.println("[victim] reading + deserializing " + file + " ...");
        try (FileInputStream fis = new FileInputStream(file);
             ObjectInputStream ois = new ObjectInputStream(fis)) {
            Object o = ois.readObject();
            System.out.println("[victim] deserialized object of type: " + o.getClass());
        }
        System.out.println("[victim] done (if a command ran, its output appeared above this line)");
    }
}
