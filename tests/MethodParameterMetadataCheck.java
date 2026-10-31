import dev.lvstrng.aidsfuscator.context.Context;
import dev.lvstrng.aidsfuscator.transform.impl.salt.MethodSaltTransformer;
import dev.lvstrng.aidsfuscator.transform.impl.strip.LocalVariableNameTransformer;
import dev.lvstrng.aidsfuscator.tree.impl.JClass;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.VarInsnNode;

/** Checks that stripped and salted methods retain valid JVM parameter metadata. */
public final class MethodParameterMetadataCheck {
    /** Runs the transformations, loads their output, and checks reflection and execution. */
    public static void main(String[] args) throws Exception {
        for (boolean named : new boolean[]{true, false}) {
            var node = new ClassNode();
            node.version = Opcodes.V25;
            node.access = Opcodes.ACC_PUBLIC;
            node.name = "test/Parameters";
            node.superName = "java/lang/Object";
            node.methods.add(identity("echo", "I", Opcodes.ILOAD, Opcodes.IRETURN, 1, named));
            node.methods.add(identity("wide", "J", Opcodes.LLOAD, Opcodes.LRETURN, 2, named));

            var context = Context.newInstance();
            var type = new JClass(node);
            context.add(type);
            new LocalVariableNameTransformer().transform(context);
            new MethodSaltTransformer().transform(context);
            var writer = new ClassWriter(ClassWriter.COMPUTE_MAXS);
            type.core().accept(writer);
            var loaded = new OutputLoader().load(writer.toByteArray());

            for (var originalType : new Class<?>[]{int.class, long.class}) {
                var method = loaded.getDeclaredMethod(originalType == int.class ? "echo" : "wide", originalType, int.class);
                var parameters = method.getParameters();
                if (named && (!parameters[0].isNamePresent() || !parameters[0].getName().equals("value")
                        || !parameters[1].isSynthetic())) {
                    throw new AssertionError("Named parameter or synthetic salt metadata was lost");
                }
                if (!named && parameters[0].isNamePresent()) {
                    throw new AssertionError("Unexpected name for an unnamed input parameter");
                }
                Object value = originalType == int.class ? (Object) 42 : 42L;
                if (!value.equals(method.invoke(null, value, 123))) {
                    throw new AssertionError("Salted method returned a different value");
                }
            }
        }
        System.out.println("PARAMETER_METADATA_OK");
    }

    private static MethodNode identity(String name, String type, int load, int ret, int slots, boolean named) {
        var method = new MethodNode(Opcodes.ACC_PUBLIC | Opcodes.ACC_STATIC, name, "(" + type + ")" + type, null, null);
        if (named) method.visitParameter("value", 0);
        method.instructions.add(new VarInsnNode(load, 0));
        method.instructions.add(new InsnNode(ret));
        method.maxLocals = slots;
        method.maxStack = slots;
        return method;
    }

    private static final class OutputLoader extends ClassLoader {
        Class<?> load(byte[] bytes) {
            return defineClass(null, bytes, 0, bytes.length);
        }
    }
}
