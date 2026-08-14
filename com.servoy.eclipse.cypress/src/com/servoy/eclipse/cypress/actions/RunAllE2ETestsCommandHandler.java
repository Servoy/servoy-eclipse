package com.servoy.eclipse.cypress.actions;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;

public class RunAllE2ETestsCommandHandler extends AbstractHandler {
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		new RunAllE2ETestsHandler(true).execute();
		return null;
	}
}
