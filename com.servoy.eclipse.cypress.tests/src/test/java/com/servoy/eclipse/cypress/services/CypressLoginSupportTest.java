package com.servoy.eclipse.cypress.services;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.servoy.eclipse.cypress.services.CypressLoginSupport.AuthKind;
import com.servoy.eclipse.cypress.services.CypressLoginSupport.AuthRequirement;

public class CypressLoginSupportTest {

	private CypressLoginSupport support;
	private Path tempDir;

	@Before
	public void setUp() throws Exception {
		support = new CypressLoginSupport();
		tempDir = Files.createTempDirectory("cypress-login-test");
	}

	@After
	public void tearDown() throws Exception {
		if (tempDir != null && Files.exists(tempDir)) {
			Files.walkFileTree(tempDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
					Files.delete(file);
					return FileVisitResult.CONTINUE;
				}

				@Override
				public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
					Files.delete(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		}
	}

	// --- detectAuth ---

	@Test
	public void testDetectAuth_nullProject_returnsNone() {
		AuthRequirement result = support.detectAuth(null);

		assertFalse(result.required());
		assertEquals(AuthKind.NONE, result.kind());
		assertNull(result.loginFormName());
		assertNull(result.loginSolutionName());
	}

	// --- writeLoginCommand ---

	@Test
	public void testWriteLoginCommand_createsNewJsFile() throws Exception {
		Path supportDir = tempDir.resolve("support");

		boolean written = support.writeLoginCommand(supportDir);

		assertTrue(written);
		Path commandsFile = supportDir.resolve("commands.js");
		assertTrue(Files.exists(commandsFile));
		String content = Files.readString(commandsFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("Cypress.Commands.add('login'"));
		assertTrue(content.contains("cy.session"));
		assertTrue(content.contains("{ log: false }"));
	}

	@Test
	public void testWriteLoginCommand_appendsToExistingJsFile() throws Exception {
		Path supportDir = tempDir.resolve("support");
		Files.createDirectories(supportDir);
		Path commandsFile = supportDir.resolve("commands.js");
		Files.writeString(commandsFile, "// existing custom commands\n", StandardCharsets.UTF_8);

		boolean written = support.writeLoginCommand(supportDir);

		assertTrue(written);
		String content = Files.readString(commandsFile, StandardCharsets.UTF_8);
		assertTrue(content.startsWith("// existing custom commands"));
		assertTrue(content.contains("Cypress.Commands.add('login'"));
	}

	@Test
	public void testWriteLoginCommand_idempotentWhenMarkerPresent() throws Exception {
		Path supportDir = tempDir.resolve("support");
		Files.createDirectories(supportDir);
		Path commandsFile = supportDir.resolve("commands.js");
		Files.writeString(commandsFile, "// stuff\nCypress.Commands.add('login', () => {});\n",
				StandardCharsets.UTF_8);

		boolean written = support.writeLoginCommand(supportDir);

		assertFalse(written);
		String content = Files.readString(commandsFile, StandardCharsets.UTF_8);
		assertEquals(1, content.split("Cypress\\.Commands\\.add\\('login'").length - 1);
	}

	@Test
	public void testWriteLoginCommand_writesToTsFileWhenPresent() throws Exception {
		Path supportDir = tempDir.resolve("support");
		Files.createDirectories(supportDir);
		Path tsFile = supportDir.resolve("commands.ts");
		Files.writeString(tsFile, "// TypeScript commands\n", StandardCharsets.UTF_8);

		boolean written = support.writeLoginCommand(supportDir);

		assertTrue(written);
		String tsContent = Files.readString(tsFile, StandardCharsets.UTF_8);
		assertTrue("Should write to commands.ts", tsContent.contains("Cypress.Commands.add('login'"));
		assertFalse("commands.js should NOT be created", Files.exists(supportDir.resolve("commands.js")));
	}

	@Test
	public void testWriteLoginCommand_prefersTsOverJs() throws Exception {
		Path supportDir = tempDir.resolve("support");
		Files.createDirectories(supportDir);
		Path tsFile = supportDir.resolve("commands.ts");
		Path jsFile = supportDir.resolve("commands.js");
		Files.writeString(tsFile, "// TS file\n", StandardCharsets.UTF_8);
		Files.writeString(jsFile, "// JS file\n", StandardCharsets.UTF_8);

		boolean written = support.writeLoginCommand(supportDir);

		assertTrue(written);
		String tsContent = Files.readString(tsFile, StandardCharsets.UTF_8);
		String jsContent = Files.readString(jsFile, StandardCharsets.UTF_8);
		assertTrue("Login command should be in .ts", tsContent.contains("Cypress.Commands.add('login'"));
		assertFalse("Login command should NOT be in .js", jsContent.contains("Cypress.Commands.add('login'"));
	}

	@Test
	public void testWriteLoginCommand_idempotentWhenMarkerInTsFile() throws Exception {
		Path supportDir = tempDir.resolve("support");
		Files.createDirectories(supportDir);
		Path tsFile = supportDir.resolve("commands.ts");
		Files.writeString(tsFile, "// existing\nCypress.Commands.add('login', () => {});\n",
				StandardCharsets.UTF_8);

		boolean written = support.writeLoginCommand(supportDir);

		assertFalse(written);
	}

	@Test
	public void testWriteLoginCommand_idempotentCrossFileCheck() throws Exception {
		Path supportDir = tempDir.resolve("support");
		Files.createDirectories(supportDir);
		Path tsFile = supportDir.resolve("commands.ts");
		Path jsFile = supportDir.resolve("commands.js");
		Files.writeString(tsFile, "// clean TS file\n", StandardCharsets.UTF_8);
		Files.writeString(jsFile, "Cypress.Commands.add('login', () => {});\n", StandardCharsets.UTF_8);

		boolean written = support.writeLoginCommand(supportDir);

		assertFalse("Should detect marker in .js even when .ts is target", written);
	}

	@Test
	public void testWriteLoginCommand_includesModalDismissalCode() throws Exception {
		Path supportDir = tempDir.resolve("support");

		support.writeLoginCommand(supportDir);

		Path commandsFile = supportDir.resolve("commands.js");
		String content = Files.readString(commandsFile, StandardCharsets.UTF_8);
		assertTrue("Should handle cookie consent modals", content.contains("cookieConsent"));
		assertTrue("Should wait for modal-backdrop to disappear", content.contains(".modal-backdrop"));
		assertTrue("Should look for buttons in active modals", content.contains("modal.in button"));
		assertTrue("Should wait briefly for modals to appear", content.contains("cy.wait(500)"));
	}

	// --- writeCypressEnvJson ---

	@Test
	public void testWriteCypressEnvJson_createsNewFile() throws Exception {
		support.writeCypressEnvJson(tempDir, "http://localhost:8080", "admin", "secret", "#dashboard");

		Path envFile = tempDir.resolve("cypress.env.json");
		assertTrue(Files.exists(envFile));
		String content = Files.readString(envFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("\"LOGIN_URL\" : \"http://localhost:8080\""));
		assertTrue(content.contains("\"TEST_USERNAME\" : \"admin\""));
		assertTrue(content.contains("\"TEST_PASSWORD\" : \"secret\""));
		assertTrue(content.contains("\"LOGIN_SUCCESS_SELECTOR\" : \"#dashboard\""));
	}

	@Test
	public void testWriteCypressEnvJson_mergesExistingKeys() throws Exception {
		Path envFile = tempDir.resolve("cypress.env.json");
		Files.writeString(envFile, "{\n  \"MY_CUSTOM_KEY\": \"keep_me\"\n}", StandardCharsets.UTF_8);

		support.writeCypressEnvJson(tempDir, "http://localhost:8080", "user", "pass", "body");

		String content = Files.readString(envFile, StandardCharsets.UTF_8);
		assertTrue("Should preserve custom key", content.contains("MY_CUSTOM_KEY"));
		assertTrue("Should add login key", content.contains("\"TEST_USERNAME\" : \"user\""));
	}

	@Test
	public void testWriteCypressEnvJson_updatesExistingLoginKeys() throws Exception {
		support.writeCypressEnvJson(tempDir, "http://old:8080", "olduser", "oldpass", "#old");
		support.writeCypressEnvJson(tempDir, "http://new:9090", "newuser", "newpass", "#new");

		Path envFile = tempDir.resolve("cypress.env.json");
		String content = Files.readString(envFile, StandardCharsets.UTF_8);
		assertTrue(content.contains("\"LOGIN_URL\" : \"http://new:9090\""));
		assertTrue(content.contains("\"TEST_USERNAME\" : \"newuser\""));
		assertFalse(content.contains("olduser"));
	}

	// --- ensureGitignoreEntry ---

	@Test
	public void testEnsureGitignoreEntry_createsNewGitignore() throws Exception {
		support.ensureGitignoreEntry(tempDir);

		Path gitignore = tempDir.resolve(".gitignore");
		assertTrue(Files.exists(gitignore));
		String content = Files.readString(gitignore, StandardCharsets.UTF_8);
		assertEquals("cypress.env.json\n", content);
	}

	@Test
	public void testEnsureGitignoreEntry_appendsToExisting() throws Exception {
		Path gitignore = tempDir.resolve(".gitignore");
		Files.writeString(gitignore, "node_modules/\n", StandardCharsets.UTF_8);

		support.ensureGitignoreEntry(tempDir);

		String content = Files.readString(gitignore, StandardCharsets.UTF_8);
		assertTrue(content.contains("node_modules/"));
		assertTrue(content.contains("cypress.env.json"));
	}

	@Test
	public void testEnsureGitignoreEntry_idempotent() throws Exception {
		Path gitignore = tempDir.resolve(".gitignore");
		Files.writeString(gitignore, "cypress.env.json\n", StandardCharsets.UTF_8);

		support.ensureGitignoreEntry(tempDir);

		String content = Files.readString(gitignore, StandardCharsets.UTF_8);
		assertEquals(1, content.lines().filter(l -> l.trim().equals("cypress.env.json")).count());
	}

	@Test
	public void testEnsureGitignoreEntry_handlesNoTrailingNewline() throws Exception {
		Path gitignore = tempDir.resolve(".gitignore");
		Files.writeString(gitignore, "node_modules/", StandardCharsets.UTF_8);

		support.ensureGitignoreEntry(tempDir);

		String content = Files.readString(gitignore, StandardCharsets.UTF_8);
		assertTrue(content.contains("node_modules/\n"));
		assertTrue(content.endsWith("cypress.env.json\n"));
	}

	// --- buildAuthRequiredMessage ---

	@Test
	public void testBuildAuthRequiredMessage_includesKind() {
		AuthRequirement auth = new AuthRequirement(true, AuthKind.STATELESS, null, null);
		String msg = support.buildAuthRequiredMessage(auth);
		assertTrue(msg.contains("STATELESS"));
	}

	@Test
	public void testBuildAuthRequiredMessage_includesLoginFormName() {
		AuthRequirement auth = new AuthRequirement(true, AuthKind.LOGIN_FORM, "myLoginForm", null);
		String msg = support.buildAuthRequiredMessage(auth);
		assertTrue(msg.contains("LOGIN_FORM"));
		assertTrue(msg.contains("'myLoginForm'"));
	}

	@Test
	public void testBuildAuthRequiredMessage_includesLoginSolutionName() {
		AuthRequirement auth = new AuthRequirement(true, AuthKind.LOGIN_FORM, null, "loginSolution");
		String msg = support.buildAuthRequiredMessage(auth);
		assertTrue(msg.contains("login solution 'loginSolution'"));
	}

	@Test
	public void testBuildAuthRequiredMessage_mentionsInstructions() {
		AuthRequirement auth = new AuthRequirement(true, AuthKind.LOGIN_FORM, "form1", null);
		String msg = support.buildAuthRequiredMessage(auth);
		assertTrue(msg.contains("Re-run generateCypressE2ETest"));
		assertTrue(msg.contains("loginUrl"));
		assertTrue(msg.contains("cypress.env.json"));
	}

	@Test
	public void testBuildAuthRequiredMessage_doesNotEchoPassword() {
		AuthRequirement auth = new AuthRequirement(true, AuthKind.LOGIN_FORM, "form1", null);
		String msg = support.buildAuthRequiredMessage(auth);
		assertFalse(msg.toLowerCase().contains("password\":"));
	}
}

