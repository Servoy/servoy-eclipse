package com.servoy.eclipse.cypress.actions;

import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.console.ConsolePlugin;
import org.eclipse.ui.console.IConsole;
import org.eclipse.ui.console.IConsoleManager;
import org.eclipse.ui.console.MessageConsole;

public final class CypressConsoleUtil {
	static final String CONSOLE_NAME = "Cypress Form Tests";

	private CypressConsoleUtil() {
	}

	static String getConsoleName() {
		return CONSOLE_NAME;
	}

	static boolean isMatchingConsole(IConsole console) {
		return console != null && CONSOLE_NAME.equals(console.getName()) && console instanceof MessageConsole;
	}

	public static MessageConsole findOrCreateConsole() {
		IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
		for (IConsole existing : consoleManager.getConsoles()) {
			if (isMatchingConsole(existing)) {
				return (MessageConsole) existing;
			}
		}
		MessageConsole console = new MessageConsole(CONSOLE_NAME, null);
		consoleManager.addConsoles(new IConsole[] { console });
		return console;
	}

	public static void showConsole(MessageConsole console) {
		Display.getDefault().asyncExec(() -> {
			IConsoleManager consoleManager = ConsolePlugin.getDefault().getConsoleManager();
			consoleManager.showConsoleView(console);
		});
	}
}
