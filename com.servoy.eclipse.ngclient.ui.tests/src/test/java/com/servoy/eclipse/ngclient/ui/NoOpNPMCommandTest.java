package com.servoy.eclipse.ngclient.ui;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.NullProgressMonitor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("NoOpNPMCommand")
class NoOpNPMCommandTest
{
	@Nested
	@DisplayName("runCommand")
	class RunCommand
	{
		@Test
		@DisplayName("completes without throwing for empty arguments")
		void completesWithoutThrowingForEmptyArguments()
		{
			NoOpNPMCommand command = new NoOpNPMCommand(Collections.emptyList());
			assertDoesNotThrow(() -> command.runCommand(new NullProgressMonitor()));
		}

		@Test
		@DisplayName("completes without throwing for non-empty arguments")
		void completesWithoutThrowingForNonEmptyArguments()
		{
			NoOpNPMCommand command = new NoOpNPMCommand(List.of("install", "--legacy-peer-deps"));
			assertDoesNotThrow(() -> command.runCommand(new NullProgressMonitor()));
		}

		@Test
		@DisplayName("completes without throwing when monitor is null")
		void completesWithNullMonitor()
		{
			NoOpNPMCommand command = new NoOpNPMCommand(List.of("run", "build"));
			assertDoesNotThrow(() -> command.runCommand(null));
		}
	}

	@Nested
	@DisplayName("getExitCode")
	class GetExitCode
	{
		@Test
		@DisplayName("returns 0 before runCommand is called")
		void returnsZeroBeforeRun()
		{
			NoOpNPMCommand command = new NoOpNPMCommand(List.of("install"));
			assertEquals(0, command.getExitCode());
		}

		@Test
		@DisplayName("returns 0 after runCommand is called")
		void returnsZeroAfterRun() throws Exception
		{
			NoOpNPMCommand command = new NoOpNPMCommand(List.of("run", "build"));
			command.runCommand(new NullProgressMonitor());
			assertEquals(0, command.getExitCode());
		}
	}

	@Nested
	@DisplayName("IRunNPMCommand contract")
	class IRunNPMCommandContract
	{
		@Test
		@DisplayName("is an instance of IRunNPMCommand")
		void isInstanceOfIRunNPMCommand()
		{
			NoOpNPMCommand command = new NoOpNPMCommand(List.of("install"));
			assertInstanceOf(IRunNPMCommand.class, command);
		}

		@Test
		@DisplayName("can be used polymorphically as IRunNPMCommand")
		void canBeUsedAsInterface() throws Exception
		{
			IRunNPMCommand command = new NoOpNPMCommand(List.of("run", "build_solution"));
			command.runCommand(new NullProgressMonitor());
			assertEquals(0, command.getExitCode());
		}
	}
}
