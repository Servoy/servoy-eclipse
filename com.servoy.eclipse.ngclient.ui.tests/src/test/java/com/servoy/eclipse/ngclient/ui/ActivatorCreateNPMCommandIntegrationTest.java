package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.io.File;
import java.lang.reflect.Field;
import java.util.List;
import java.util.concurrent.CountDownLatch;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Activator.createNPMCommand - Integration")
class ActivatorCreateNPMCommandIntegrationTest {
	private Activator activator;
	private File originalNodePath;
	private File originalNpmPath;

	@BeforeEach
	void setUp() throws Exception {
		activator = Activator.getInstance();
		assertNotNull(activator, "Activator instance must be available in PDE test environment");

		originalNodePath = getField("nodePath");
		originalNpmPath = getField("npmPath");

		CountDownLatch latch = getField("nodeReady");
		latch.countDown();
	}

	@AfterEach
	void tearDown() throws Exception {
		setField("nodePath", originalNodePath);
		setField("npmPath", originalNpmPath);
	}

	@Nested
	@DisplayName("when nodePath is null (Node.js not available)")
	class WhenNodePathIsNull {
		@BeforeEach
		void setNodePathNull() throws Exception {
			setField("nodePath", null);
			setField("npmPath", null);
		}

		@Test
		@DisplayName("returns NoOpNPMCommand instance")
		void returnsNoOpNPMCommand() {
			IRunNPMCommand command = activator.createNPMCommand(new File("."), List.of("install"));
			assertInstanceOf(NoOpNPMCommand.class, command);
		}
	}

	@Nested
	@DisplayName("when nodePath is set (Node.js available)")
	class WhenNodePathIsSet {
		@BeforeEach
		void setNodePath() throws Exception {
			setField("nodePath", new File("node"));
			setField("npmPath", new File("npm"));
		}

		@Test
		@DisplayName("returns RunNPMCommand instance")
		void returnsRunNPMCommand() {
			IRunNPMCommand command = activator.createNPMCommand(new File("."), List.of("install"));
			assertInstanceOf(RunNPMCommand.class, command);
		}
	}

	@SuppressWarnings("unchecked")
	private <T> T getField(String fieldName) throws Exception {
		Field field = Activator.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		return (T) field.get(activator);
	}

	private void setField(String fieldName, Object value) throws Exception {
		Field field = Activator.class.getDeclaredField(fieldName);
		field.setAccessible(true);
		field.set(activator, value);
	}
}
