package com.hyzalia.paint.early;

import com.hypixel.hytale.plugin.early.ClassTransformer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Early-plugin : rend {@code ToolOperation#executeBlock(III)Z} {@code public} dans le bytecode
 * au chargement (remplace l’ancien patch {@code execute0} des versions antérieures).
 *
 * <p>Sans cela, une sous-classe chargée par {@code PluginClassLoader} ne se trouve pas dans le
 * même <em>runtime package</em> que {@code ToolOperation} ({@code AppClassLoader}) : une méthode
 * {@code package-private} du parent n’est alors pas considérée comme overridée →
 * {@link java.lang.AbstractMethodError} à l’invocation depuis le code serveur, même si
 * {@code getSuperclass() == ToolOperation.class}.
 *
 * <p>Déployer le JAR {@code HyzaliaPaint-EarlyBridge-execute0-*.jar} (tâche Gradle {@code :early-bridge:jar})
 * dans {@code earlyplugins/} (ou {@code --early-plugins}) — <strong>pas</strong> le JAR mod
 * {@code hyzalia-paint-*.jar}. Lancer avec {@code --accept-early-plugins}.
 */
public final class ToolOperationExecute0PublicTransformer implements ClassTransformer {

    private static final int ASM_API = Opcodes.ASM9;

    private static final String TOOL_OPERATION_INTERNAL =
            "com/hypixel/hytale/builtin/buildertools/tooloperations/ToolOperation";

    @Override
    public int priority() {
        return 10_000;
    }

    @Nullable
    @Override
    public byte[] transform(
            @Nonnull String className,
            @Nonnull String internalName,
            @Nonnull byte[] classBytes) {
        if (!TOOL_OPERATION_INTERNAL.equals(internalName)) {
            return null;
        }
        ClassReader cr = new ClassReader(classBytes);
        ClassWriter cw = new ClassWriter(cr, ClassWriter.COMPUTE_MAXS);
        ClassVisitor cv = new ClassVisitor(ASM_API, cw) {
            @Override
            public MethodVisitor visitMethod(
                    int access,
                    String name,
                    String descriptor,
                    String signature,
                    String[] exceptions) {
                int out = access;
                if ("executeBlock".equals(name) && "(III)Z".equals(descriptor)) {
                    out = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                } else if ("execute0".equals(name) && "(III)Z".equals(descriptor)) {
                    out = (access & ~(Opcodes.ACC_PRIVATE | Opcodes.ACC_PROTECTED)) | Opcodes.ACC_PUBLIC;
                }
                return super.visitMethod(out, name, descriptor, signature, exceptions);
            }
        };
        cr.accept(cv, ClassReader.SKIP_FRAMES);
        return cw.toByteArray();
    }
}
