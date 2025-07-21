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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IEditorPart;
import org.mozilla.javascript.Function;

import com.servoy.eclipse.developersolution.DeveloperNGClient;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.ISupportDeveloperMenu;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.scripting.solutionmodel.developer.JSDeveloperMenu;

/**
 * @author jcompagner
 *
 * @since 2025.09
 *
 */
public class DeveloperSolutionAction extends Action
{

	private final Function function;
	private final String solutionName;
	private final Form[] forms;
	private final BaseComponent[] components;

	/**
	 * @param key
	 * @param value
	 */
	public DeveloperSolutionAction(JSDeveloperMenu key, Function function, String solutionName, Form[] forms, BaseComponent[] components)
	{
		this.function = function;
		this.solutionName = solutionName;
		this.forms = forms;
		this.components = components;
		setText(key.getText());
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("developer_bridge.png"));
	}


	@Override
	public void run()
	{
		if (forms != null && forms.length > 0)
		{
			IEditorPart editorPart = EditorUtil.openFormDesignEditor(this.forms[0]);
			if (editorPart instanceof ISupportDeveloperMenu formEditor)
			{
				formEditor.executeDeveloperMenuCommand(function, forms, components);
			}
			else
			{
				ServoyLog.logInfo("DeveloperSolutionAction form editor does not implement ISupportDeveloperMenu - skipping execution");
			}
		}
		else if (solutionName != null)
		{
			SolutionScope solutionScope = DeveloperNGClient.INSTANCE.getScriptEngine().getSolutionScope();
			DeveloperNGClient.INSTANCE.getWebsocketSession().getEventDispatcher().addEvent(() -> {
				try
				{
					DeveloperNGClient.INSTANCE.getScriptEngine().executeFunction(function, solutionScope, solutionScope, new Object[] { solutionName }, false,
						false);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			});
		}
	}

}
