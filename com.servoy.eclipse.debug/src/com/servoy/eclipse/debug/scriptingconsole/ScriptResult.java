package com.servoy.eclipse.debug.scriptingconsole;

import java.util.Arrays;

import org.mozilla.javascript.Scriptable;

/**
 * @author jcompagner
 *
 */
final class ScriptResult implements IScriptExecResult
{
	private final Object eval;

	/**
	 * @param eval
	 */
	public ScriptResult(Object eval)
	{
		this.eval = eval;
	}

	public boolean isError()
	{
		return eval instanceof Exception;
	}

	public String getOutput()
	{
		if (eval instanceof Scriptable)
		{
			return (String)((Scriptable)eval).getDefaultValue(String.class);
		}
		else if (eval instanceof Object[])
		{
			return Arrays.toString((Object[])eval);
		}
		return eval != null ? eval.toString() : null;
	}
}