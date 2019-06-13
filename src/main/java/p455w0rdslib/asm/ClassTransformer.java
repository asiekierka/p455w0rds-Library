package p455w0rdslib.asm;

import java.util.List;
import java.util.Map;

import org.objectweb.asm.*;
import org.objectweb.asm.tree.*;

import net.minecraft.launchwrapper.IClassTransformer;
import net.minecraftforge.fml.relauncher.CoreModManager;

/**
 * @author p455w0rd
 *
 */
public class ClassTransformer implements IClassTransformer {

	private static final String RENDERGLOBAL_CLASS = "net.minecraft.client.renderer.RenderGlobal";
	private static final String CHUNKRENDERCONTAINER_CLASS = "net.minecraft.client.renderer.ChunkRenderContainer";
	private static final String HOOKS_PATH = "p455w0rdslib/asm/Hooks";
	private static boolean init = false;
	private static boolean enabled = true;

	@Override
	public byte[] transform(final String name, final String transformedName, final byte[] basicClass) {
		if (!init) {
			init = true;
			if (Hooks.isOptifineDetected()) {
				enabled = false;
				FMLPlugin.log("Optifine detected; Patching aborted :D");
			}
			for (final Map.Entry<String, List<String>> e : CoreModManager.getTransformers().entrySet()) { //if possible, detect Albedo, and abort
				if (e.getValue().get(0).equals("elucent.albedo.asm.ASMTransformer")) {
					enabled = false;
					Hooks.albedoDetected = true;
					FMLPlugin.log("Albedo detected; Patching aborted :D");
				}
			}
		}
		if (enabled) {
			if (transformedName.equals(RENDERGLOBAL_CLASS)) {
				return patchRenderGlobal(basicClass);
			}
			if (transformedName.equals(CHUNKRENDERCONTAINER_CLASS)) {
				return patchChunkRenderContainer(basicClass);
			}
		}
		return basicClass;
	}

	private static byte[] patchRenderGlobal(final byte[] c) {
		final ClassReader reader = new ClassReader(c);
		final ClassNode node = new ClassNode();
		boolean found = false;
		reader.accept(node, 0);
		for (final MethodNode method : node.methods) {
			if (found) {
				break;
			}
			if ((method.name.equals("func_174982_a") || method.name.equals("renderBlockLayer")) && method.desc.equals("(Lnet/minecraft/util/BlockRenderLayer;)V")) {
				final InsnList instr = method.instructions;
				instr.insert(instr.getFirst(), new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS_PATH, "enableColoredLighting", "()V", false));
				final AbstractInsnNode[] nodeList = instr.toArray();
				for (int i = nodeList.length - 1; i >= 0; i--) {
					final AbstractInsnNode currentNode = nodeList[i];
					if (currentNode.getOpcode() == Opcodes.RETURN) {
						instr.insertBefore(currentNode, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS_PATH, "disableColoredLighting", "()V", false));
						FMLPlugin.log("Successfully patched " + RENDERGLOBAL_CLASS + "#renderBlockLayer(Lnet/minecraft/util/BlockRenderLayer;)V");
						found = true;
						break;
					}
				}
			}
		}
		final SafeClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		node.accept(writer);
		return writer.toByteArray();
	}

	private static byte[] patchChunkRenderContainer(final byte[] c) {
		final ClassReader reader = new ClassReader(c);
		final ClassNode node = new ClassNode();
		reader.accept(node, 0);
		for (final MethodNode method : node.methods) {
			if ((method.name.equals("func_178003_a") || method.name.equals("preRenderChunk")) && method.desc.equals("(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V")) {
				final InsnList instr = method.instructions;
				final AbstractInsnNode[] nodeList = instr.toArray();
				for (int i = nodeList.length - 1; i >= 0; i--) {
					final AbstractInsnNode currentNode = nodeList[i];
					if (currentNode.getOpcode() == Opcodes.RETURN) {
						instr.insertBefore(currentNode, new VarInsnNode(Opcodes.ALOAD, 1));
						instr.insertBefore(currentNode, new MethodInsnNode(Opcodes.INVOKESTATIC, HOOKS_PATH, "preRenderChunk", "(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V", false));
						FMLPlugin.log("Successfully patched " + CHUNKRENDERCONTAINER_CLASS + "#preRenderChunk(Lnet/minecraft/client/renderer/chunk/RenderChunk;)V");
					}
				}
			}
		}
		final SafeClassWriter writer = new SafeClassWriter(ClassWriter.COMPUTE_MAXS | ClassWriter.COMPUTE_FRAMES);
		node.accept(writer);
		return writer.toByteArray();
	}

}
