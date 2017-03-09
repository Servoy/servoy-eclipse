package com.servoy.eclipse.ui.asm.utils;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.FieldVisitor;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;

public class DumpClassFromAsmCode implements Opcodes
{

	public static void main(String[] args)
	{
		File f = new File("dumpedClasses/org/eclipse/jface/resource");
		f.mkdirs();
		try (BufferedOutputStream out = new BufferedOutputStream(new FileOutputStream("dumpedClasses/org/eclipse/jface/resource/ImageDescriptor.class")))
		{
			out.write(dump());
			System.out.println("Full asm created class file was written to dumpedClasses/org/eclipse/jface/resource/ImageDescriptor.class");
		}
		catch (FileNotFoundException e)
		{
			e.printStackTrace();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	public static byte[] dump() throws Exception
	{

		ClassWriter cw = new ClassWriter(0);
		FieldVisitor fv;
		MethodVisitor mv;
		AnnotationVisitor av0;

		cw.visit(V1_7, ACC_PUBLIC + ACC_SUPER + ACC_ABSTRACT, "org/eclipse/jface/resource/ImageDescriptor", null,
			"org/eclipse/jface/resource/DeviceResourceDescriptor", null);

		cw.visitInnerClass("org/eclipse/jface/resource/ImageDescriptor$OriginalCreateFromFile", "org/eclipse/jface/resource/ImageDescriptor",
			"OriginalCreateFromFile", ACC_STATIC);

		cw.visitInnerClass("org/eclipse/jface/resource/ImageDescriptor$OriginalCreateFromURL", "org/eclipse/jface/resource/ImageDescriptor",
			"OriginalCreateFromURL", ACC_STATIC);

		{
			fv = cw.visitField(ACC_PROTECTED + ACC_FINAL + ACC_STATIC, "DEFAULT_IMAGE_DATA", "Lorg/eclipse/swt/graphics/ImageData;", null, null);
			fv.visitEnd();
		}
		{
			fv = cw.visitField(ACC_STATIC, "fileBasedImageReplacementMapper", "Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;", null, null);
			fv.visitEnd();
		}
		{
			fv = cw.visitField(ACC_STATIC, "urlBasedImageReplacementMapper", "Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;", null, null);
			fv.visitEnd();
		}
		{
			fv = cw.visitField(ACC_STATIC, "originalFileBasedImageCreator", "Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;", null, null);
			fv.visitEnd();
		}
		{
			fv = cw.visitField(ACC_STATIC, "originalUrlBasedImageCreator", "Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;", null, null);
			fv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_STATIC, "<clinit>", "()V", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "org/eclipse/swt/graphics/ImageData");
			mv.visitInsn(DUP);
			mv.visitIntInsn(BIPUSH, 6);
			mv.visitIntInsn(BIPUSH, 6);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(NEW, "org/eclipse/swt/graphics/PaletteData");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "org/eclipse/swt/graphics/RGB");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitTypeInsn(NEW, "org/eclipse/swt/graphics/RGB");
			mv.visitInsn(DUP);
			mv.visitIntInsn(SIPUSH, 255);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(ICONST_0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/swt/graphics/RGB", "<init>", "(III)V", false);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/swt/graphics/PaletteData", "<init>", "([Lorg/eclipse/swt/graphics/RGB;)V", false);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/swt/graphics/ImageData", "<init>", "(IIILorg/eclipse/swt/graphics/PaletteData;)V", false);
			mv.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "DEFAULT_IMAGE_DATA", "Lorg/eclipse/swt/graphics/ImageData;");
			mv.visitInsn(RETURN);
			mv.visitMaxs(15, 0);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PROTECTED, "<init>", "()V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/DeviceResourceDescriptor", "<init>", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PRIVATE + ACC_STATIC, "prepareImageReplacementMappersIfNeeded", "()V", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			Label l3 = new Label();
			mv.visitJumpInsn(IFNONNULL, l3);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitJumpInsn(IFNONNULL, l3);
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/ImageDescriptor$OriginalCreateFromFile");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/ImageDescriptor$OriginalCreateFromFile", "<init>", "()V", false);
			mv.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/ImageDescriptor$OriginalCreateFromURL");
			mv.visitInsn(DUP);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/ImageDescriptor$OriginalCreateFromURL", "<init>", "()V", false);
			mv.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalUrlBasedImageCreator",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitLabel(l0);
			mv.visitLdcInsn(Type.getType("Lorg/eclipse/jface/resource/ImageDescriptor;"));
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getClassLoader", "()Ljava/lang/ClassLoader;", false);
			mv.visitVarInsn(ASTORE, 0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(INSTANCEOF, "org/osgi/framework/BundleReference");
			mv.visitJumpInsn(IFEQ, l3);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitTypeInsn(CHECKCAST, "org/osgi/framework/BundleReference");
			mv.visitMethodInsn(INVOKEINTERFACE, "org/osgi/framework/BundleReference", "getBundle", "()Lorg/osgi/framework/Bundle;", true);
			mv.visitVarInsn(ASTORE, 1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitLdcInsn("equinoxContainer");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitVarInsn(ASTORE, 3);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitLdcInsn("packageAdmin");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredField", "(Ljava/lang/String;)Ljava/lang/reflect/Field;", false);
			mv.visitVarInsn(ASTORE, 4);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "setAccessible", "(Z)V", false);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Field", "get", "(Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitVarInsn(ASTORE, 5);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Object", "getClass", "()Ljava/lang/Class;", false);
			mv.visitLdcInsn("getBundles");
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitLdcInsn(Type.getType("Ljava/lang/String;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
				false);
			mv.visitVarInsn(ASTORE, 6);
			mv.visitVarInsn(ALOAD, 6);
			mv.visitVarInsn(ALOAD, 5);
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn("com.servoy.eclipse.ui");
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "[Lorg/osgi/framework/Bundle;");
			mv.visitVarInsn(ASTORE, 7);
			mv.visitVarInsn(ALOAD, 7);
			mv.visitInsn(ICONST_0);
			mv.visitInsn(AALOAD);
			mv.visitLdcInsn("com.servoy.eclipse.ui.tweaks.ImageReplacementMapper");
			mv.visitMethodInsn(INVOKEINTERFACE, "org/osgi/framework/Bundle", "loadClass", "(Ljava/lang/String;)Ljava/lang/Class;", true);
			mv.visitVarInsn(ASTORE, 8);
			mv.visitVarInsn(ALOAD, 8);
			mv.visitLdcInsn("getFileBasedImageReplacer");
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
				false);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "org/eclipse/osgi/service/runnable/ParameterizedRunnable");
			mv.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "fileBasedImageReplacementMapper",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitVarInsn(ALOAD, 8);
			mv.visitLdcInsn("getUrlBasedImageReplacer");
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Class");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitLdcInsn(Type.getType("Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;"));
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getDeclaredMethod", "(Ljava/lang/String;[Ljava/lang/Class;)Ljava/lang/reflect/Method;",
				false);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ICONST_1);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "originalFileBasedImageCreator",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/reflect/Method", "invoke", "(Ljava/lang/Object;[Ljava/lang/Object;)Ljava/lang/Object;", false);
			mv.visitTypeInsn(CHECKCAST, "org/eclipse/osgi/service/runnable/ParameterizedRunnable");
			mv.visitFieldInsn(PUTSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "urlBasedImageReplacementMapper",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitLabel(l1);
			mv.visitJumpInsn(GOTO, l3);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Exception" });
			mv.visitVarInsn(ASTORE, 0);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			mv.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_4);
			mv.visitLdcInsn("org.eclipse.jface");
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitLdcInsn("Cannot find image replacement implementation in Servoy weaved ImageDescriptor: ");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getLocalizedMessage", "()Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
				false);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(RETURN);
			mv.visitMaxs(8, 9);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "createFromFile", "(Ljava/lang/Class;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;",
				"(Ljava/lang/Class<*>;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;", null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "prepareImageReplacementMappersIfNeeded", "()V", false);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "fileBasedImageReplacementMapper",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			Label l3 = new Label();
			mv.visitJumpInsn(IFNULL, l3);
			mv.visitLabel(l0);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "fileBasedImageReplacementMapper",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitInsn(ICONST_2);
			mv.visitTypeInsn(ANEWARRAY, "java/lang/Object");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(AASTORE);
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitInsn(AASTORE);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/osgi/service/runnable/ParameterizedRunnable", "run", "(Ljava/lang/Object;)Ljava/lang/Object;",
				true);
			mv.visitTypeInsn(CHECKCAST, "org/eclipse/jface/resource/ImageDescriptor");
			mv.visitLabel(l1);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Exception" });
			mv.visitVarInsn(ASTORE, 2);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			mv.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_4);
			mv.visitLdcInsn("org.eclipse.jface");
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitLdcInsn("Exception trying to get replacement file image for ");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Class", "getCanonicalName", "()Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitLdcInsn(", ");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitLdcInsn(": ");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getLocalizedMessage", "()Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
				false);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "createFromFileInternal",
				"(Ljava/lang/Class;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(8, 3);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "createFromURL", "(Ljava/net/URL;)Lorg/eclipse/jface/resource/ImageDescriptor;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "java/lang/Exception");
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "prepareImageReplacementMappersIfNeeded", "()V", false);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "urlBasedImageReplacementMapper",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			Label l3 = new Label();
			mv.visitJumpInsn(IFNULL, l3);
			mv.visitLabel(l0);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "urlBasedImageReplacementMapper",
				"Lorg/eclipse/osgi/service/runnable/ParameterizedRunnable;");
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/osgi/service/runnable/ParameterizedRunnable", "run", "(Ljava/lang/Object;)Ljava/lang/Object;",
				true);
			mv.visitTypeInsn(CHECKCAST, "org/eclipse/jface/resource/ImageDescriptor");
			mv.visitLabel(l1);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "java/lang/Exception" });
			mv.visitVarInsn(ASTORE, 1);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/util/Policy", "getLog", "()Lorg/eclipse/jface/util/ILogger;", false);
			mv.visitTypeInsn(NEW, "org/eclipse/core/runtime/Status");
			mv.visitInsn(DUP);
			mv.visitInsn(ICONST_4);
			mv.visitLdcInsn("org.eclipse.jface");
			mv.visitTypeInsn(NEW, "java/lang/StringBuilder");
			mv.visitInsn(DUP);
			mv.visitLdcInsn("Exception trying to get replacement url image for ");
			mv.visitMethodInsn(INVOKESPECIAL, "java/lang/StringBuilder", "<init>", "(Ljava/lang/String;)V", false);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/Object;)Ljava/lang/StringBuilder;", false);
			mv.visitLdcInsn(": ");
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/Exception", "getLocalizedMessage", "()Ljava/lang/String;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "append", "(Ljava/lang/String;)Ljava/lang/StringBuilder;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "java/lang/StringBuilder", "toString", "()Ljava/lang/String;", false);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/core/runtime/Status", "<init>", "(ILjava/lang/String;Ljava/lang/String;Ljava/lang/Throwable;)V",
				false);
			mv.visitMethodInsn(INVOKEINTERFACE, "org/eclipse/jface/util/ILogger", "log", "(Lorg/eclipse/core/runtime/IStatus;)V", true);
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "createFromURLInternal",
				"(Ljava/net/URL;)Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(8, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_STATIC, "createFromFileInternal", "(Ljava/lang/Class;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;",
				"(Ljava/lang/Class<*>;Ljava/lang/String;)Lorg/eclipse/jface/resource/ImageDescriptor;", null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/FileImageDescriptor");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/FileImageDescriptor", "<init>", "(Ljava/lang/Class;Ljava/lang/String;)V", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(4, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_STATIC, "createFromURLInternal", "(Ljava/net/URL;)Lorg/eclipse/jface/resource/ImageDescriptor;", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			Label l0 = new Label();
			mv.visitJumpInsn(IFNONNULL, l0);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/ImageDescriptor", "getMissingImageDescriptor",
				"()Lorg/eclipse/jface/resource/ImageDescriptor;", false);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l0);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/URLImageDescriptor");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/URLImageDescriptor", "<init>", "(Ljava/net/URL;)V", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "createFromImageData",
				"(Lorg/eclipse/swt/graphics/ImageData;)Lorg/eclipse/jface/resource/ImageDescriptor;", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/ImageDataImageDescriptor");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/ImageDataImageDescriptor", "<init>", "(Lorg/eclipse/swt/graphics/ImageData;)V",
				false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "createFromImage", "(Lorg/eclipse/swt/graphics/Image;)Lorg/eclipse/jface/resource/ImageDescriptor;",
				null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/ImageDataImageDescriptor");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/ImageDataImageDescriptor", "<init>", "(Lorg/eclipse/swt/graphics/Image;)V", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "createWithFlags",
				"(Lorg/eclipse/jface/resource/ImageDescriptor;I)Lorg/eclipse/jface/resource/ImageDescriptor;", null, null);
			mv.visitCode();
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/DerivedImageDescriptor");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/DerivedImageDescriptor", "<init>", "(Lorg/eclipse/jface/resource/ImageDescriptor;I)V",
				false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(4, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC + ACC_DEPRECATED, "createFromImage",
				"(Lorg/eclipse/swt/graphics/Image;Lorg/eclipse/swt/graphics/Device;)Lorg/eclipse/jface/resource/ImageDescriptor;", null, null);
			{
				av0 = mv.visitAnnotation("Ljava/lang/Deprecated;", true);
				av0.visitEnd();
			}
			mv.visitCode();
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/ImageDataImageDescriptor");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/ImageDataImageDescriptor", "<init>", "(Lorg/eclipse/swt/graphics/Image;)V", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "createResource", "(Lorg/eclipse/swt/graphics/Device;)Ljava/lang/Object;", null,
				new String[] { "org/eclipse/jface/resource/DeviceResourceException" });
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ICONST_0);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/jface/resource/ImageDescriptor", "createImage",
				"(ZLorg/eclipse/swt/graphics/Device;)Lorg/eclipse/swt/graphics/Image;", false);
			mv.visitVarInsn(ASTORE, 2);
			mv.visitVarInsn(ALOAD, 2);
			Label l0 = new Label();
			mv.visitJumpInsn(IFNONNULL, l0);
			mv.visitTypeInsn(NEW, "org/eclipse/jface/resource/DeviceResourceException");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/jface/resource/DeviceResourceException", "<init>",
				"(Lorg/eclipse/jface/resource/DeviceResourceDescriptor;)V", false);
			mv.visitInsn(ATHROW);
			mv.visitLabel(l0);
			mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "org/eclipse/swt/graphics/Image" }, 0, null);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 3);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "destroyResource", "(Ljava/lang/Object;)V", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 1);
			mv.visitTypeInsn(CHECKCAST, "org/eclipse/swt/graphics/Image");
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/swt/graphics/Image", "dispose", "()V", false);
			mv.visitInsn(RETURN);
			mv.visitMaxs(1, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "createImage", "()Lorg/eclipse/swt/graphics/Image;", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ICONST_1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/jface/resource/ImageDescriptor", "createImage", "(Z)Lorg/eclipse/swt/graphics/Image;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(2, 1);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "createImage", "(Z)Lorg/eclipse/swt/graphics/Image;", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitVarInsn(ILOAD, 1);
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/swt/widgets/Display", "getCurrent", "()Lorg/eclipse/swt/widgets/Display;", false);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/jface/resource/ImageDescriptor", "createImage",
				"(ZLorg/eclipse/swt/graphics/Device;)Lorg/eclipse/swt/graphics/Image;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "createImage", "(Lorg/eclipse/swt/graphics/Device;)Lorg/eclipse/swt/graphics/Image;", null, null);
			mv.visitCode();
			mv.visitVarInsn(ALOAD, 0);
			mv.visitInsn(ICONST_1);
			mv.visitVarInsn(ALOAD, 1);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/jface/resource/ImageDescriptor", "createImage",
				"(ZLorg/eclipse/swt/graphics/Device;)Lorg/eclipse/swt/graphics/Image;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(3, 2);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC, "createImage", "(ZLorg/eclipse/swt/graphics/Device;)Lorg/eclipse/swt/graphics/Image;", null, null);
			mv.visitCode();
			Label l0 = new Label();
			Label l1 = new Label();
			Label l2 = new Label();
			mv.visitTryCatchBlock(l0, l1, l2, "org/eclipse/swt/SWTException");
			Label l3 = new Label();
			Label l4 = new Label();
			mv.visitTryCatchBlock(l3, l4, l2, "org/eclipse/swt/SWTException");
			Label l5 = new Label();
			Label l6 = new Label();
			Label l7 = new Label();
			mv.visitTryCatchBlock(l5, l6, l7, "org/eclipse/swt/SWTException");
			mv.visitVarInsn(ALOAD, 0);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/jface/resource/ImageDescriptor", "getImageData", "()Lorg/eclipse/swt/graphics/ImageData;", false);
			mv.visitVarInsn(ASTORE, 3);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitJumpInsn(IFNONNULL, l0);
			mv.visitVarInsn(ILOAD, 1);
			Label l8 = new Label();
			mv.visitJumpInsn(IFNE, l8);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l8);
			mv.visitFrame(Opcodes.F_APPEND, 1, new Object[] { "org/eclipse/swt/graphics/ImageData" }, 0, null);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "DEFAULT_IMAGE_DATA", "Lorg/eclipse/swt/graphics/ImageData;");
			mv.visitVarInsn(ASTORE, 3);
			mv.visitLabel(l0);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitFieldInsn(GETFIELD, "org/eclipse/swt/graphics/ImageData", "transparentPixel", "I");
			mv.visitJumpInsn(IFLT, l3);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKEVIRTUAL, "org/eclipse/swt/graphics/ImageData", "getTransparencyMask", "()Lorg/eclipse/swt/graphics/ImageData;", false);
			mv.visitVarInsn(ASTORE, 4);
			mv.visitTypeInsn(NEW, "org/eclipse/swt/graphics/Image");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitVarInsn(ALOAD, 4);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/swt/graphics/Image", "<init>",
				"(Lorg/eclipse/swt/graphics/Device;Lorg/eclipse/swt/graphics/ImageData;Lorg/eclipse/swt/graphics/ImageData;)V", false);
			mv.visitLabel(l1);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l3);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitTypeInsn(NEW, "org/eclipse/swt/graphics/Image");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitVarInsn(ALOAD, 3);
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/swt/graphics/Image", "<init>",
				"(Lorg/eclipse/swt/graphics/Device;Lorg/eclipse/swt/graphics/ImageData;)V", false);
			mv.visitLabel(l4);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l2);
			mv.visitFrame(Opcodes.F_SAME1, 0, null, 1, new Object[] { "org/eclipse/swt/SWTException" });
			mv.visitVarInsn(ASTORE, 4);
			mv.visitVarInsn(ILOAD, 1);
			Label l9 = new Label();
			mv.visitJumpInsn(IFEQ, l9);
			mv.visitLabel(l5);
			mv.visitTypeInsn(NEW, "org/eclipse/swt/graphics/Image");
			mv.visitInsn(DUP);
			mv.visitVarInsn(ALOAD, 2);
			mv.visitFieldInsn(GETSTATIC, "org/eclipse/jface/resource/ImageDescriptor", "DEFAULT_IMAGE_DATA", "Lorg/eclipse/swt/graphics/ImageData;");
			mv.visitMethodInsn(INVOKESPECIAL, "org/eclipse/swt/graphics/Image", "<init>",
				"(Lorg/eclipse/swt/graphics/Device;Lorg/eclipse/swt/graphics/ImageData;)V", false);
			mv.visitLabel(l6);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l7);
			mv.visitFrame(Opcodes.F_FULL, 5,
				new Object[] { "org/eclipse/jface/resource/ImageDescriptor", Opcodes.INTEGER, "org/eclipse/swt/graphics/Device", "org/eclipse/swt/graphics/ImageData", "org/eclipse/swt/SWTException" },
				1, new Object[] { "org/eclipse/swt/SWTException" });
			mv.visitVarInsn(ASTORE, 5);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitLabel(l9);
			mv.visitFrame(Opcodes.F_SAME, 0, null, 0, null);
			mv.visitInsn(ACONST_NULL);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(5, 6);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_ABSTRACT, "getImageData", "()Lorg/eclipse/swt/graphics/ImageData;", null, null);
			mv.visitEnd();
		}
		{
			mv = cw.visitMethod(ACC_PUBLIC + ACC_STATIC, "getMissingImageDescriptor", "()Lorg/eclipse/jface/resource/ImageDescriptor;", null, null);
			mv.visitCode();
			mv.visitMethodInsn(INVOKESTATIC, "org/eclipse/jface/resource/MissingImageDescriptor", "getInstance",
				"()Lorg/eclipse/jface/resource/MissingImageDescriptor;", false);
			mv.visitInsn(ARETURN);
			mv.visitMaxs(1, 0);
			mv.visitEnd();
		}
		cw.visitEnd();

		return cw.toByteArray();
	}
}
