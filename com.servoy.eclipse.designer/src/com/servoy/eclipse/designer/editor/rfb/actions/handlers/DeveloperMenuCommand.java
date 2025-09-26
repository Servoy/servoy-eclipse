/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.util.JSMethodCallInterceptor;
import com.servoy.eclipse.designer.util.JSMethodCallInterceptor.JSComponentMethodCallListener;
import com.servoy.eclipse.designer.util.JSMethodCallInterceptor.JSFormMethodCallListener;
import com.servoy.eclipse.developersolution.DeveloperNGClient;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.scripting.solutionmodel.JSComponent;
import com.servoy.j2db.scripting.solutionmodel.JSForm;

/**
 * @author gabi
 *
 */
public class DeveloperMenuCommand extends BaseRestorableCommand implements JSComponentMethodCallListener, JSFormMethodCallListener
{
	private final FunctionDefinition callback;
	private final Form[] forms;
	private final BaseComponent[] components;

	public DeveloperMenuCommand(FunctionDefinition callback, Form forms[], BaseComponent[] components)
	{
		super("developerMenu");
		this.callback = callback;
		this.forms = forms;
		this.components = components;
	}

	@Override
	public void execute()
	{
		try
		{
			final Object args[];
			if (forms != null && forms.length > 0)
			{
				JSForm jsForms[] = new JSForm[forms.length];
				for (int i = 0; i < forms.length; i++)
				{
					jsForms[i] = JSMethodCallInterceptor.getInstance().createForm(forms[i]);
				}
				if (components != null && components.length > 0)
				{
					JSComponent[] jsComponents = new JSComponent[components.length];

					for (int i = 0; i < components.length; i++)
					{
						jsComponents[i] = JSMethodCallInterceptor.getInstance().createComponent(jsForms[0], components[i]);
					}
					args = new Object[] { jsForms, jsComponents };
				}
				else
				{
					args = new Object[] { jsForms };
				}
			}
			else args = null;

			DeveloperNGClient.INSTANCE.getWebsocketSession().getEventDispatcher().addEvent(() -> {
				try
				{
					JSMethodCallInterceptor.getInstance().setJSFormMethodCallListener(DeveloperMenuCommand.this);
					JSMethodCallInterceptor.getInstance().setJSComponentMethodCallListener(DeveloperMenuCommand.this);
					callback.executeSync((IClientPluginAccess)DeveloperNGClient.INSTANCE.getPluginAccess(), args);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				finally
				{
					JSMethodCallInterceptor.getInstance().setJSFormMethodCallListener(null);
					JSMethodCallInterceptor.getInstance().setJSComponentMethodCallListener(null);
				}
			});
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/*
	 * @see com.servoy.eclipse.designer.util.JSMethodCallInterceptor.JSFormMethodCallListener#onJSFormMethodCall(com.servoy.j2db.scripting.solutionmodel.JSForm)
	 */
	@Override
	public void onJSFormMethodCall(JSForm form)
	{
		this.saveState(form.getContainer());
	}

	/*
	 * @see com.servoy.eclipse.designer.util.JSMethodCallInterceptor.JSComponentMethodCallListener#onJSComponentMethodCall(com.servoy.j2db.scripting.
	 * solutionmodel.JSComponent)
	 */
	@Override
	public void onJSComponentMethodCall(JSComponent component)
	{
		this.saveState(component.getBaseComponent(false));
	}
}
