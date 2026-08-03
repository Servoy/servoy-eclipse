package com.servoy.eclipse.designer.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.VfeCommandStackEventListener;

@DisplayName("BaseVisualFormEditor.persistChanges structural verification (SVY-21249)")
class BaseVisualFormEditorPersistChangesTest {
	private static final String SOURCE_RELATIVE_PATH = "src/com/servoy/eclipse/designer/editor/BaseVisualFormEditor.java";

	@Nested
	@DisplayName("source-inspection: persistChanges guards flagModified with isRunningCommand check")
	class PersistChangesFlagModifiedGuard {
		@Test
		@DisplayName("persistChanges contains flagModified() guarded by !commandStackEventListener.isRunningCommand()")
		void persistChangesGuardsFlagModifiedWithIsRunningCommand() throws URISyntaxException, IOException {
			String methodBody = extractPersistChangesMethodBody();

			assertTrue(methodBody.contains("flagModified()"),
					"persistChanges must call flagModified() to mark the editor dirty");

			assertTrue(methodBody.contains("!commandStackEventListener.isRunningCommand()"),
					"persistChanges must guard flagModified() with !commandStackEventListener.isRunningCommand()");
		}

		@Test
		@DisplayName("AC3: flagModified is only called inside the isRunningCommand guard block (not called when command stack IS running)")
		void flagModifiedOnlyCalledWhenCommandStackNotRunning() throws URISyntaxException, IOException {
			String methodBody = extractPersistChangesMethodBody();

			int guardIndex = methodBody.indexOf("!commandStackEventListener.isRunningCommand()");
			assumeTrue(guardIndex >= 0, "Guard condition not found in persistChanges");

			int flagModifiedIndex = methodBody.indexOf("flagModified()", guardIndex);
			assertTrue(flagModifiedIndex > guardIndex,
					"flagModified() must appear AFTER the !isRunningCommand() guard check");

			String beforeGuard = methodBody.substring(0, guardIndex);
			assertFalse(beforeGuard.contains("flagModified()"),
					"flagModified() must NOT be called before the isRunningCommand guard - "
							+ "when the command stack IS running (PRE state), flagModified() must NOT be invoked");
		}

		@Test
		@DisplayName("the guard is inside an if-block that wraps flagModified (structural nesting)")
		void guardWrappsFlagModifiedInIfBlock() throws URISyntaxException, IOException {
			String methodBody = extractPersistChangesMethodBody();

			int guardIndex = methodBody.indexOf("if (!commandStackEventListener.isRunningCommand())");
			assumeTrue(guardIndex >= 0, "if-guard pattern not found in persistChanges");

			int openBrace = methodBody.indexOf("{", guardIndex);
			assumeTrue(openBrace >= 0, "Opening brace after guard not found");

			int closeBrace = findMatchingBrace(methodBody, openBrace);
			assumeTrue(closeBrace > openBrace, "Could not find matching closing brace for guard block");

			String guardBlock = methodBody.substring(openBrace, closeBrace + 1);
			assertTrue(guardBlock.contains("flagModified()"),
					"flagModified() must be inside the if(!isRunningCommand()) block");
		}
	}

	@Nested
	@DisplayName("structural: VfeCommandStackEventListener is accessible from BaseVisualFormEditor")
	class VfeCommandStackEventListenerAccessibility {
		@Test
		@DisplayName("BaseVisualFormEditor has a field of type VfeCommandStackEventListener")
		void hasCommandStackEventListenerField() throws NoSuchFieldException {
			Field field = BaseVisualFormEditor.class.getDeclaredField("commandStackEventListener");
			assertNotNull(field);
			assertTrue(VfeCommandStackEventListener.class.isAssignableFrom(field.getType()),
					"commandStackEventListener field must be of type VfeCommandStackEventListener");
		}

		@Test
		@DisplayName("VfeCommandStackEventListener is a public static inner class of BaseVisualFormEditor")
		void vfeCommandStackEventListenerIsPublicStaticInnerClass() {
			assertAll(
					() -> assertTrue(Modifier.isPublic(VfeCommandStackEventListener.class.getModifiers()),
							"VfeCommandStackEventListener must be public"),
					() -> assertTrue(Modifier.isStatic(VfeCommandStackEventListener.class.getModifiers()),
							"VfeCommandStackEventListener must be static"),
					() -> assertTrue(
							VfeCommandStackEventListener.class.getDeclaringClass() == BaseVisualFormEditor.class,
							"VfeCommandStackEventListener must be declared inside BaseVisualFormEditor"));
		}

		@Test
		@DisplayName("VfeCommandStackEventListener exposes isRunningCommand() method")
		void exposesIsRunningCommandMethod() throws NoSuchMethodException {
			var method = VfeCommandStackEventListener.class.getMethod("isRunningCommand");
			assertNotNull(method);
			assertTrue(method.getReturnType() == boolean.class, "isRunningCommand() must return boolean");
		}
	}

	private String extractPersistChangesMethodBody() throws URISyntaxException, IOException {
		Path sourceFile = locateSourceFile();
		assumeTrue(sourceFile != null && Files.exists(sourceFile),
				"Source file not available - skipping source scan test");

		String source = Files.readString(sourceFile, StandardCharsets.UTF_8);

		int persistChangesStart = source.indexOf("public void persistChanges(");
		assumeTrue(persistChangesStart >= 0, "Could not locate persistChanges method in source");

		int methodBodyStart = source.indexOf("{", persistChangesStart);
		int methodBodyEnd = findMatchingBrace(source, methodBodyStart);
		assumeTrue(methodBodyEnd > 0, "Could not determine end of persistChanges method");

		return source.substring(persistChangesStart, methodBodyEnd + 1);
	}

	private Path locateSourceFile() throws URISyntaxException {
		URL classUrl = BaseVisualFormEditor.class.getProtectionDomain().getCodeSource().getLocation();
		if (classUrl == null)
			return null;

		Path binDir = Path.of(classUrl.toURI());
		Path projectRoot = binDir.getParent();
		Path sourceFile = projectRoot.resolve(SOURCE_RELATIVE_PATH);
		if (Files.exists(sourceFile)) {
			return sourceFile;
		}

		sourceFile = binDir.resolve("..").resolve(SOURCE_RELATIVE_PATH).normalize();
		if (Files.exists(sourceFile)) {
			return sourceFile;
		}

		return null;
	}

	private static int findMatchingBrace(String source, int openBraceIndex) {
		int braceCount = 0;
		for (int i = openBraceIndex; i < source.length(); i++) {
			char c = source.charAt(i);
			if (c == '{')
				braceCount++;
			else if (c == '}') {
				braceCount--;
				if (braceCount == 0)
					return i;
			}
		}
		return -1;
	}
}
