/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.debug.scriptingconsole;

import java.io.IOException;

import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.servoy.j2db.IDebugClient;

/**
 * @author jcompagner
 * @since 6.0
 */
public class CommandHandler implements ICommandHandler
{
	private final IActiveClientProvider provider;

	public CommandHandler(IActiveClientProvider provider)
	{
		this.provider = provider;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.debug.scriptingconsole.ICommandHandler#handleCommand(java.lang.String)
	 */
	public IScriptExecResult handleCommand(final String userInput) throws IOException
	{
		IDebugClient selectedClient = provider.getSelectedClient();
		if (selectedClient != null)
		{
			final IDebugClient state = selectedClient;
			final Object[] retVal = new Object[1];
			state.invokeAndWait(new Runnable()
			{
				public void run()
				{
					Object eval = null;
					Context cx = Context.enter();
					try
					{
						Scriptable scope = ScriptConsole.getScope(state, true);
						eval = cx.evaluateString(scope, userInput, "internal_anon", 1, null);
						if (eval instanceof Wrapper)
						{
							eval = ((Wrapper)eval).unwrap();
						}
						if (eval == Scriptable.NOT_FOUND || eval == Undefined.instance)
						{
							eval = null;
						}

						StringBuilder stringBuilder = provider.getSelectedClientScript();
						stringBuilder.append(userInput);
						stringBuilder.append('\n');
					}
					catch (Exception ex)
					{
						eval = ex;
					}
					finally
					{
						Context.exit();
					}
					retVal[0] = eval;
				}
			});
			return new ScriptResult(retVal[0]);
		}
		return null;
	}

}
