import org.graalvm.nativeimage.ImageGeneratorraalvm.native;
import org.gimage.c.type.CTypePointer;

public class BuildNativeImage {
    public static void main(String[] args) {
        System.out.println("GraalVM Native Image Builder");
        System.out.println("Java: " + System.getProperty("java.version"));
        System.out.println("VM: " + System.getProperty("java.vm.name"));
    }
}
