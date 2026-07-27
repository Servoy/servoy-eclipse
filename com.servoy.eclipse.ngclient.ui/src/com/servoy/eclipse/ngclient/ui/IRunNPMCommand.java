package com.servoy.eclipse.ngclient.ui;

import java.io.IOException;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;

public interface IRunNPMCommand
{
	void runCommand(IProgressMonitor monitor) throws IOException, InterruptedException;

	int getExitCode();

	/**
	 * @param b
	 */
	void setUser(boolean b);

	/**
	 *
	 */
	void schedule();

	/**
	 *
	 */
	void join() throws InterruptedException;

	/**
	 * @param unmodifiableMap
	 */
	void setExtraEnvironment(Map<String, String> unmodifiableMap);

	/**
	 * @return
	 */
	Process getProcess();

	/**
	 *
	 */
	boolean cancel();
}
