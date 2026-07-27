package com.servoy.eclipse.ngclient.ui;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

import com.servoy.eclipse.model.util.ServoyLog;

class NoOpNPMCommand implements IRunNPMCommand
{
	private final List<String> commandArguments;

	NoOpNPMCommand(List<String> commandArguments)
	{
		this.commandArguments = commandArguments;
	}

	@Override
	public void runCommand(IProgressMonitor monitor) throws IOException, InterruptedException
	{
		ServoyLog.logWarning("Node.js is not available, skipping npm command: " + RunNPMCommand.commandArgsToString(commandArguments), null);
	}

	@Override
	public int getExitCode()
	{
		return 0;
	}

	@Override
	public void setUser(boolean b)
	{
	}

	@Override
	public void schedule()
	{
	}

	@Override
	public void join() throws InterruptedException
	{
	}

	@Override
	public void setExtraEnvironment(Map<String, String> unmodifiableMap)
	{
	}

	@Override
	public Process getProcess()
	{
		return null;
	}

	@Override
	public boolean cancel()
	{
		return true;
	}
}
