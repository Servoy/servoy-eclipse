/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
*/

package com.servoy.eclipse.ui.tweaks.weaver;

import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.MethodNode;
import org.osgi.framework.hooks.weaving.WovenClass;

/**
 * The weaver that transforms the byte-code of the jface ImageDescriptor class in order to hijack source image URLs/paths,
 * in order to replace images in the IDE that are not contributed by Servoy. This way (hopefully all) the icons can be made to look similar to the Servoy ones.
 *
 * @author acostescu
 */
public class ImageDescriptorWeaver extends BasicWeaver implements Opcodes
{

	@Override
	public void weaveClass(WovenClass wovenClass)
	{
		// this is a bit of a hack, point to the com.servoy.eclipse.ui.tweaks plugin for getting stuff (like the ImageReplacementMapper)
		wovenClass.getDynamicImports().add("com.servoy.eclipse.ui.tweaks");
		super.weaveClass(wovenClass);
	}

	@Override
	protected void weaveClassInternal(ClassNode classNode)
	{
		FieldVisitor fieldVisitor;
		MethodVisitor methodVisitor;

		// rename original method; add "Internal" suffix
		MethodNode methodNode = getMethod(classNode, "createFromFile", "(Ljava/lang/Class;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;");
		methodNode.name = "originalCreateFromFile";

		methodNode = getMethod(classNode, "createFromURL", "(Ljava/net/URL;)Lorg/eclipse/jface/resource/ImageDescriptor;");
		methodNode.name = "originalCreateFromURL";

		{
			fieldVisitor = classNode.visitField(ACC_STATIC, "originalFileBasedImageCreator", "Ljava/lang/reflect/Method;", null, null);
			fieldVisitor.visitEnd();
		}
		{
			fieldVisitor = classNode.visitField(ACC_STATIC, "originalUrlBasedImageCreator", "Ljava/lang/reflect/Method;", null, null);
			fieldVisitor.visitEnd();
		}
		{
			methodNode = getMethod(classNode, "<clinit>", "()V");
			if (methodNode != null)
			{
				methodVisitor = methodNode;

				// remove the return instruction as we will add our content after existing content in the static block - se we want our code to execute as well
				AbstractInsnNode currentFromLast;
				do
				{
					currentFromLast = methodNode.instructions.getLast();
					methodNode.instructions.remove(currentFromLast);
				}
				while (currentFromLast.getOpcode() != RETURN);
			}
			else methodVisitor = classNode.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);

			methodVisitor.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			methodVisitor.visitTryCatchBlock(l0, l1, l2, "java/lang/NoSuchMethodException");
			Label l3 = new Label();
			methodVisitor.visitTryCatchBlock(l0, l1, l3, "java/lang/SecurityException");
			methodVisitor.visitLabel(l0);
			methodVisitor.visitLdcInsn(Type.getType("Lorg/eclipse/jface/resource/ImageDescriptor;"));
			methodVisitor.visitLdcInsn("originalCreateFromFile");
			methodVisitor.visitInsn(ICONST_2);
			methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_0);
			methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/Class;"));
			methodVisitor.visitInsn(AASTORE);
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_1);
			methodVisitor.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			methodVisitor.visitInsn(AASTORE);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
				"(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
			methodVisitor.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Ljava/lang/reflect/Method;");
			methodVisitor.visitLdcInsn(Type.getType("Lorg/eclipse/jface/resource/ImageDescriptor;"));
			methodVisitor.visitLdcInsn("originalCreateFromURL");
			methodVisitor.visitInsn(ICONST_1);
			methodVisitor.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_0);
			methodVisitor.visitLdcInsn(Type.getType("Ljava/net/URL;"));
			methodVisitor.visitInsn(AASTORE);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod",
				"(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;", false);
			methodVisitor.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator", "Ljava/lang/reflect/Method;");
			methodVisitor.visitLabel(l1);
			Label l4 = new Label();
			methodVisitor.visitJumpInsn(GOTO, l4);
			methodVisitor.visitLabel(l2);
			methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/NoSuchMethodException" });
			methodVisitor.visitVarInsn(ASTORE, 0);
			methodVisitor.visitInsn(ACONST_NULL);
			methodVisitor.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Ljava/lang/reflect/Method;");
			methodVisitor.visitInsn(ACONST_NULL);
			methodVisitor.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator", "Ljava/lang/reflect/Method;");
			methodVisitor.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			methodVisitor.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_4);
			methodVisitor.visitLdcInsn("org.eclipse.jface");
			methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitLdcInsn("Exception trying to get original createFrom... method. Should never happen: ");
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/NoSuchMethodException", "getLocalizedMessage", "()Ljava/lang/String;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>",
				"(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V", false);
			methodVisitor.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			methodVisitor.visitJumpInsn(GOTO, l4);
			methodVisitor.visitLabel(l3);
			methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/SecurityException" });
			methodVisitor.visitVarInsn(ASTORE, 0);
			methodVisitor.visitInsn(ACONST_NULL);
			methodVisitor.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Ljava/lang/reflect/Method;");
			methodVisitor.visitInsn(ACONST_NULL);
			methodVisitor.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator", "Ljava/lang/reflect/Method;");
			methodVisitor.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			methodVisitor.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_4);
			methodVisitor.visitLdcInsn("org.eclipse.jface");
			methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitLdcInsn("Exception trying to get original createFrom... method: ");
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/SecurityException", "getLocalizedMessage", "()Ljava/lang/String;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>",
				"(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V", false);
			methodVisitor.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			methodVisitor.visitLabel(l4);
			methodVisitor.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			methodVisitor.visitInsn(RETURN);
			methodVisitor.visitMaxs(15, 1);
			methodVisitor.visitEnd();
		}
		{
			methodVisitor = classNode.visitMethod(ACC_PUBLIC + ACC_STATIC, "createFromFile",
				"(Ljava/lang/Class;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;",
				"(Ljava/lang/Class<*>;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;", null);
			methodVisitor.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			methodVisitor.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
			methodVisitor.visitLabel(l0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator", "Ljava/lang/reflect/Method;");
			methodVisitor.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Ljava/lang/reflect/Method;");
			methodVisitor.visitMethodInsn(INVOKESTATIC, "com/servoy/eclipse/ui/tweaks/ImageReplacementMapper", "getFileBasedImageReplacement",
				"(Ljava/lang/Class;Ljava/lang/String;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			methodVisitor.visitLabel(l1);
			methodVisitor.visitInsn(ARETURN);
			methodVisitor.visitLabel(l2);
			methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Exception" });
			methodVisitor.visitVarInsn(ASTORE, 2);
			methodVisitor.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			methodVisitor.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_4);
			methodVisitor.visitLdcInsn("org.eclipse.jface");
			methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitLdcInsn("Exception trying to get replacement file image for ");
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getCanonicalName", "()Ljava/lang/String;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitLdcInsn(", ");
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitLdcInsn(": ");
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitVarInsn(ALOAD, 2);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getLocalizedMessage", "()Ljava/lang/String;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			methodVisitor.visitVarInsn(ALOAD, 2);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>",
				"(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V", false);
			methodVisitor.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalCreateFromFile",
				"(Ljava/lang/Class;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			methodVisitor.visitInsn(ARETURN);
			methodVisitor.visitMaxs(8, 3);
			methodVisitor.visitEnd();
		}
		{
			methodVisitor = classNode.visitMethod(ACC_PUBLIC + ACC_STATIC, "createFromURL", "(Ljava/net/URL;)Lorg/eclipse/jface/resource/ImageDescriptor;",
				null, null);
			methodVisitor.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			methodVisitor.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
			methodVisitor.visitLabel(l0);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator", "Ljava/lang/reflect/Method;");
			methodVisitor.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Ljava/lang/reflect/Method;");
			methodVisitor.visitMethodInsn(INVOKESTATIC, "com/servoy/eclipse/ui/tweaks/ImageReplacementMapper", "getUrlBasedImageReplacement",
				"(Ljava/net/URL;Ljava/lang/reflect/Method;Ljava/lang/reflect/Method;)Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			methodVisitor.visitLabel(l1);
			methodVisitor.visitInsn(ARETURN);
			methodVisitor.visitLabel(l2);
			methodVisitor.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Exception" });
			methodVisitor.visitVarInsn(ASTORE, 1);
			methodVisitor.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			methodVisitor.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitInsn(ICONST_4);
			methodVisitor.visitLdcInsn("org.eclipse.jface");
			methodVisitor.visitTypeInsn(NEW, "java/lang/StringBuilder");
			methodVisitor.visitInsn(DUP);
			methodVisitor.visitLdcInsn("Exception trying to get replacement url image for ");
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitLdcInsn(": ");
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getLocalizedMessage", "()Ljava/lang/String;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			methodVisitor.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			methodVisitor.visitVarInsn(ALOAD, 1);
			methodVisitor.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>",
				"(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V", false);
			methodVisitor.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			methodVisitor.visitVarInsn(ALOAD, 0);
			methodVisitor.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalCreateFromURL",
				"(Ljava/net/URL;)Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			methodVisitor.visitInsn(ARETURN);
			methodVisitor.visitMaxs(8, 2);
			methodVisitor.visitEnd();
		}
		classNode.visitEnd();
	}

}
