package com.servoy.eclipse.debug.scriptingconsole;

import java.io.IOException;


public interface ICommandHandler
{
	IScriptExecResult handleCommand(String userInput) throws IOException;
}
