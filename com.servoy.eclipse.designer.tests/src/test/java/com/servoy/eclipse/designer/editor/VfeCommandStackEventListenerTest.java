package com.servoy.eclipse.designer.editor;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CommandStackEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor.VfeCommandStackEventListener;

@DisplayName("VfeCommandStackEventListener")
class VfeCommandStackEventListenerTest
{
	private VfeCommandStackEventListener listener;
	private CommandStack commandStack;

	@BeforeEach
	void setUp()
	{
		listener = new VfeCommandStackEventListener();
		commandStack = new CommandStack();
	}

	@Nested
	@DisplayName("isRunningCommand()")
	class IsRunningCommand
	{
		@Test
		@DisplayName("returns false initially (default state is POST_EXECUTE)")
		void returnsFalseInitially()
		{
			assertFalse(listener.isRunningCommand());
		}

		@ParameterizedTest
		@ValueSource(ints = { CommandStack.PRE_EXECUTE, CommandStack.PRE_UNDO, CommandStack.PRE_REDO })
		@DisplayName("returns true after a PRE event")
		void returnsTrueAfterPreEvent(int preDetail)
		{
			listener.stackChanged(new CommandStackEvent(commandStack, null, preDetail));

			assertTrue(listener.isRunningCommand());
		}

		@ParameterizedTest
		@ValueSource(ints = { CommandStack.POST_EXECUTE, CommandStack.POST_UNDO, CommandStack.POST_REDO })
		@DisplayName("returns false after a POST event")
		void returnsFalseAfterPostEvent(int postDetail)
		{
			listener.stackChanged(new CommandStackEvent(commandStack, null, CommandStack.PRE_EXECUTE));
			listener.stackChanged(new CommandStackEvent(commandStack, null, postDetail));

			assertFalse(listener.isRunningCommand());
		}

		@Test
		@DisplayName("correctly transitions between PRE and POST states")
		void transitionsBetweenStates()
		{
			assertAll(
				() -> assertFalse(listener.isRunningCommand(), "should be false initially"),
				() -> {
					listener.stackChanged(new CommandStackEvent(commandStack, null, CommandStack.PRE_EXECUTE));
					assertTrue(listener.isRunningCommand(), "should be true after PRE_EXECUTE");
				},
				() -> {
					listener.stackChanged(new CommandStackEvent(commandStack, null, CommandStack.POST_EXECUTE));
					assertFalse(listener.isRunningCommand(), "should be false after POST_EXECUTE");
				},
				() -> {
					listener.stackChanged(new CommandStackEvent(commandStack, null, CommandStack.PRE_UNDO));
					assertTrue(listener.isRunningCommand(), "should be true after PRE_UNDO");
				},
				() -> {
					listener.stackChanged(new CommandStackEvent(commandStack, null, CommandStack.POST_UNDO));
					assertFalse(listener.isRunningCommand(), "should be false after POST_UNDO");
				}
			);
		}
	}
}
