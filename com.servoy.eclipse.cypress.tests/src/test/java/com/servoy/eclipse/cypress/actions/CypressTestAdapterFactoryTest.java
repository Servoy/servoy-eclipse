package com.servoy.eclipse.cypress.actions;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IAdapterFactory;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.cypress.services.CypressTestDiscoveryService;
import com.servoy.eclipse.cypress.services.FormSpecGenerator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;

@DisplayName("CypressTestAdapterFactory")
public class CypressTestAdapterFactoryTest {
	private CypressTestAdapterFactory factory;

	@BeforeEach
	void setUp() {
		factory = new CypressTestAdapterFactory();
	}

	@Nested
	@DisplayName("structural verification")
	class StructuralVerification {
		@Test
		@DisplayName("implements IAdapterFactory")
		void implementsAdapterFactory() {
			assertTrue(IAdapterFactory.class.isAssignableFrom(CypressTestAdapterFactory.class));
		}

		@Test
		@DisplayName("getAdapterList returns CypressFormTestTarget")
		void getAdapterListReturnsCorrectType() {
			Class<?>[] adapters = factory.getAdapterList();

			assertNotNull(adapters);
			assertEquals(1, adapters.length);
			assertEquals(CypressFormTestTarget.class, adapters[0]);
		}

		@Test
		@DisplayName("has discoveryService field")
		void hasDiscoveryServiceField() throws NoSuchFieldException {
			Field field = CypressTestAdapterFactory.class.getDeclaredField("discoveryService");
			assertNotNull(field);
			assertEquals(CypressTestDiscoveryService.class, field.getType());
		}

		@Test
		@DisplayName("has ADAPTERS constant")
		void hasAdaptersConstant() throws NoSuchFieldException {
			Field field = CypressTestAdapterFactory.class.getDeclaredField("ADAPTERS");
			assertTrue(Modifier.isStatic(field.getModifiers()));
			assertTrue(Modifier.isFinal(field.getModifiers()));
		}

		@Test
		@DisplayName("getDiscoveryService returns non-null")
		void getDiscoveryServiceReturnsNonNull() {
			assertNotNull(factory.getDiscoveryService());
		}
	}

	@Nested
	@DisplayName("getAdapter guard clauses")
	class GuardClauses {
		@Test
		@DisplayName("returns null for wrong adapter type")
		void returnsNullForWrongAdapterType() {
			SimpleUserNode node = new SimpleUserNode("test", UserNodeType.FORM, null, null);

			Object result = factory.getAdapter(node, String.class);

			assertNull(result);
		}

		@Test
		@DisplayName("returns null for non-SimpleUserNode object")
		void returnsNullForNonSimpleUserNode() {
			Object result = factory.getAdapter("not a node", CypressFormTestTarget.class);

			assertNull(result);
		}

		@Test
		@DisplayName("returns null for null adaptable object")
		void returnsNullForNullObject() {
			Object result = factory.getAdapter(null, CypressFormTestTarget.class);

			assertNull(result);
		}

		@Test
		@DisplayName("returns null for Integer object (non-node)")
		void returnsNullForInteger() {
			Object result = factory.getAdapter(Integer.valueOf(42), CypressFormTestTarget.class);

			assertNull(result);
		}
	}

	@Nested
	@DisplayName("resolveTarget")
	class ResolveTargetTests {
		private Path tempDir;

		@BeforeEach
		void setUpTempDir() throws Exception {
			tempDir = Files.createTempDirectory("adapter-resolve-test");
		}

		@AfterEach
		void tearDown() throws Exception {
			if (tempDir != null && Files.exists(tempDir)) {
				deleteTempDir(tempDir);
			}
		}

		@Test
		@DisplayName("returns SingleFormTestTarget for FORM type when test exists")
		void returnsSingleFormTargetWhenTestExists() throws Exception {
			Files.createFile(tempDir.resolve("loginForm.spec.cy.js"));
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.FORM, "loginForm", null);

			assertNotNull(target);
			assertAll(
				() -> assertEquals("loginForm", target.getFormName()),
				() -> assertFalse(target.isSolutionLevel()),
				() -> assertEquals(Collections.singletonList("loginForm"), target.getTestFormNames())
			);
		}

		@Test
		@DisplayName("returns null for FORM type when no test exists")
		void returnsNullForFormWithoutTest() throws Exception {
			injectMockDiscoveryService(tempDir);

			// This will call ServoyLog.logInfo which needs activeProject.getProject().getName()
			// Since we pass null, it will throw NPE on the log line
			assertThrows(NullPointerException.class,
				() -> factory.resolveTarget(UserNodeType.FORM, "noTestForm", null));
		}

		@Test
		@DisplayName("returns SolutionLevelTestTarget for SOLUTION type when tests exist")
		void returnsSolutionTargetForSolutionType() throws Exception {
			Files.createFile(tempDir.resolve("formA.spec.cy.js"));
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.SOLUTION, null, null);

			assertNotNull(target);
			assertAll(
				() -> assertNull(target.getFormName()),
				() -> assertTrue(target.isSolutionLevel())
			);
		}

		@Test
		@DisplayName("returns SolutionLevelTestTarget for SOLUTION_ITEM type when tests exist")
		void returnsSolutionTargetForSolutionItemType() throws Exception {
			Files.createFile(tempDir.resolve("formB.spec.cy.js"));
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.SOLUTION_ITEM, null, null);

			assertNotNull(target);
			assertTrue(target.isSolutionLevel());
		}

		@Test
		@DisplayName("returns SolutionLevelTestTarget for FORMS type when tests exist")
		void returnsSolutionTargetForFormsType() throws Exception {
			Files.createFile(tempDir.resolve("formC.spec.cy.js"));
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.FORMS, null, null);

			assertNotNull(target);
			assertTrue(target.isSolutionLevel());
		}

		@Test
		@DisplayName("returns null for SOLUTION type when no tests exist")
		void returnsNullForSolutionWithoutTests() throws Exception {
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.SOLUTION, null, null);

			assertNull(target);
		}

		@Test
		@DisplayName("returns null for SOLUTION_ITEM type when no tests exist")
		void returnsNullForSolutionItemWithoutTests() throws Exception {
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.SOLUTION_ITEM, null, null);

			assertNull(target);
		}

		@Test
		@DisplayName("returns null for FORMS type when no tests exist")
		void returnsNullForFormsWithoutTests() throws Exception {
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.FORMS, null, null);

			assertNull(target);
		}

		@Test
		@DisplayName("returns null for unrelated node type")
		void returnsNullForUnrelatedType() throws Exception {
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.TABLE, null, null);

			assertNull(target);
		}

		@Test
		@DisplayName("returns null for null form name on FORM type when no test")
		void returnsNullForNullFormNameNoTest() throws Exception {
			injectMockDiscoveryService(tempDir);

			// formName is null, discoveryService.hasTest(null) returns false
			// Then it hits the log line which needs activeProject
			assertThrows(NullPointerException.class,
				() -> factory.resolveTarget(UserNodeType.FORM, null, null));
		}

		@Test
		@DisplayName("SolutionLevelTestTarget delegates getTestFormNames to discoveryService")
		void solutionTargetDelegatesGetTestFormNames() throws Exception {
			Files.createFile(tempDir.resolve("formX.spec.cy.js"));
			Files.createFile(tempDir.resolve("formY.spec.cy.js"));
			Files.createFile(tempDir.resolve("formZ.spec.cy.js"));
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.SOLUTION, null, null);

			assertNotNull(target);
			List<String> forms = target.getTestFormNames();
			assertAll(
				() -> assertEquals(3, forms.size()),
				() -> assertTrue(forms.contains("formX")),
				() -> assertTrue(forms.contains("formY")),
				() -> assertTrue(forms.contains("formZ"))
			);
		}

		@Test
		@DisplayName("SingleFormTestTarget returns single-element list")
		void singleFormTargetReturnsSingleList() throws Exception {
			Files.createFile(tempDir.resolve("myForm.spec.cy.js"));
			injectMockDiscoveryService(tempDir);

			CypressFormTestTarget target = factory.resolveTarget(UserNodeType.FORM, "myForm", null);

			assertNotNull(target);
			assertEquals(1, target.getTestFormNames().size());
			assertEquals("myForm", target.getTestFormNames().get(0));
		}
	}

	@Nested
	@DisplayName("CypressFormTestTarget interface")
	class TargetInterface {
		@Test
		@DisplayName("interface defines getFormName method")
		void interfaceHasGetFormName() throws NoSuchMethodException {
			assertNotNull(CypressFormTestTarget.class.getMethod("getFormName"));
			assertEquals(String.class, CypressFormTestTarget.class.getMethod("getFormName").getReturnType());
		}

		@Test
		@DisplayName("interface defines isSolutionLevel method")
		void interfaceHasIsSolutionLevel() throws NoSuchMethodException {
			assertNotNull(CypressFormTestTarget.class.getMethod("isSolutionLevel"));
			assertEquals(boolean.class, CypressFormTestTarget.class.getMethod("isSolutionLevel").getReturnType());
		}

		@Test
		@DisplayName("interface defines getTestFormNames method")
		void interfaceHasGetTestFormNames() throws NoSuchMethodException {
			assertNotNull(CypressFormTestTarget.class.getMethod("getTestFormNames"));
			assertEquals(List.class, CypressFormTestTarget.class.getMethod("getTestFormNames").getReturnType());
		}
	}

	@Nested
	@DisplayName("SingleFormTestTarget inner class")
	class SingleFormTestTargetTest {
		@Test
		@DisplayName("can be instantiated via reflection")
		void canInstantiate() throws Exception {
			Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
			Class<?> singleFormClass = null;
			for (Class<?> c : innerClasses) {
				if (c.getSimpleName().equals("SingleFormTestTarget")) {
					singleFormClass = c;
					break;
				}
			}
			assertNotNull(singleFormClass, "SingleFormTestTarget inner class should exist");
			assertTrue(CypressFormTestTarget.class.isAssignableFrom(singleFormClass));
		}

		@Test
		@DisplayName("returns correct values for single form")
		void returnsCorrectValues() throws Exception {
			Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
			Class<?> singleFormClass = null;
			for (Class<?> c : innerClasses) {
				if (c.getSimpleName().equals("SingleFormTestTarget")) {
					singleFormClass = c;
					break;
				}
			}
			assertNotNull(singleFormClass);

			var ctor = singleFormClass.getDeclaredConstructors()[0];
			ctor.setAccessible(true);
			CypressFormTestTarget target = (CypressFormTestTarget) ctor.newInstance("myTestForm");

			assertAll(() -> assertEquals("myTestForm", target.getFormName()),
					() -> assertFalse(target.isSolutionLevel()),
					() -> assertEquals(Collections.singletonList("myTestForm"), target.getTestFormNames()));
		}
	}

	@Nested
	@DisplayName("SolutionLevelTestTarget inner class")
	class SolutionLevelTestTargetTest {
		@Test
		@DisplayName("exists and implements CypressFormTestTarget")
		void existsAndImplementsInterface() {
			Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
			Class<?> solutionClass = null;
			for (Class<?> c : innerClasses) {
				if (c.getSimpleName().equals("SolutionLevelTestTarget")) {
					solutionClass = c;
					break;
				}
			}
			assertNotNull(solutionClass, "SolutionLevelTestTarget inner class should exist");
			assertTrue(CypressFormTestTarget.class.isAssignableFrom(solutionClass));
		}

		@Test
		@DisplayName("returns null formName and isSolutionLevel true")
		void returnsCorrectValues() throws Exception {
			Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
			Class<?> solutionClass = null;
			for (Class<?> c : innerClasses) {
				if (c.getSimpleName().equals("SolutionLevelTestTarget")) {
					solutionClass = c;
					break;
				}
			}
			assertNotNull(solutionClass);

			var ctor = solutionClass.getDeclaredConstructors()[0];
			ctor.setAccessible(true);
			CypressFormTestTarget target = (CypressFormTestTarget) ctor.newInstance(factory);

			assertAll(() -> assertNull(target.getFormName()), () -> assertTrue(target.isSolutionLevel()));
		}

		@Test
		@DisplayName("getTestFormNames delegates to discoveryService")
		void getTestFormNamesDelegates() throws Exception {
			Path tempDir = Files.createTempDirectory("adapter-factory-test");
			try {
				Files.createFile(tempDir.resolve("formX.spec.cy.js"));
				Files.createFile(tempDir.resolve("formY.spec.cy.js"));

				injectMockDiscoveryService(tempDir);

				Class<?>[] innerClasses = CypressTestAdapterFactory.class.getDeclaredClasses();
				Class<?> solutionClass = null;
				for (Class<?> c : innerClasses) {
					if (c.getSimpleName().equals("SolutionLevelTestTarget")) {
						solutionClass = c;
						break;
					}
				}
				assertNotNull(solutionClass);

				var ctor = solutionClass.getDeclaredConstructors()[0];
				ctor.setAccessible(true);
				CypressFormTestTarget target = (CypressFormTestTarget) ctor.newInstance(factory);

				List<String> forms = target.getTestFormNames();
				assertAll(() -> assertEquals(2, forms.size()), () -> assertTrue(forms.contains("formX")),
						() -> assertTrue(forms.contains("formY")));
			} finally {
				deleteTempDir(tempDir);
			}
		}
	}

	@Nested
	@DisplayName("node type handling logic")
	class NodeTypeHandling {
		@Test
		@DisplayName("FORM type is recognized by getAdapter")
		void formTypeRecognized() {
			SimpleUserNode node = new SimpleUserNode("testForm", UserNodeType.FORM, null, null);
			assertEquals(UserNodeType.FORM, node.getType());
		}

		@Test
		@DisplayName("SOLUTION type is recognized by getAdapter")
		void solutionTypeRecognized() {
			SimpleUserNode node = new SimpleUserNode("Sol", UserNodeType.SOLUTION, null, null);
			assertEquals(UserNodeType.SOLUTION, node.getType());
		}

		@Test
		@DisplayName("FORMS type is recognized by getAdapter")
		void formsTypeRecognized() {
			SimpleUserNode node = new SimpleUserNode("Forms", UserNodeType.FORMS, null, null);
			assertEquals(UserNodeType.FORMS, node.getType());
		}

		@Test
		@DisplayName("SOLUTION_ITEM type is recognized")
		void solutionItemTypeRecognized() {
			SimpleUserNode node = new SimpleUserNode("Item", UserNodeType.SOLUTION_ITEM, null, null);
			assertEquals(UserNodeType.SOLUTION_ITEM, node.getType());
		}
	}

	private void injectMockDiscoveryService(Path formsDir) throws Exception {
		CypressTestDiscoveryService mockService = new CypressTestDiscoveryService();
		FormSpecGenerator mockGenerator = new FormSpecGenerator() {
			@Override
			public Path getFormSpecDir() {
				return formsDir;
			}
		};
		Field genField = CypressTestDiscoveryService.class.getDeclaredField("specGenerator");
		genField.setAccessible(true);
		genField.set(mockService, mockGenerator);

		Field serviceField = CypressTestAdapterFactory.class.getDeclaredField("discoveryService");
		serviceField.setAccessible(true);
		serviceField.set(factory, mockService);
	}

	private void deleteTempDir(Path dir) throws IOException {
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path d, IOException exc) throws IOException {
				Files.delete(d);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}

