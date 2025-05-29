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
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.mozilla.javascript.Function;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.developersolution.DeveloperNGClient;
import com.servoy.eclipse.ui.editors.IFlagChangeEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.scripting.SolutionScope;
import com.servoy.j2db.scripting.solutionmodel.JSForm;
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
	private final Object[] args;

	/**
	 * @param key
	 * @param value
	 */
	public DeveloperSolutionAction(JSDeveloperMenu key, Function function, Object[] args)
	{
		this.function = function;
		this.args = args;
		setText(key.getText());
	}


	@Override
	public void run()
	{
		SolutionScope solutionScope = DeveloperNGClient.INSTANCE.getScriptEngine().getSolutionScope();
		DeveloperNGClient.INSTANCE.getWebsocketSession().getEventDispatcher().addEvent(() -> {
			try
			{
				DeveloperNGClient.INSTANCE.getScriptEngine().executeFunction(function, solutionScope, solutionScope, args, false, false);
				if (args[0] instanceof JSForm jsform)
				{
					Display.getDefault().asyncExec(() -> {
						ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, jsform.getContainer(), true);
						IEditorPart editorPart = EditorUtil.openFormDesignEditor((Form)jsform.getContainer());
						if (editorPart instanceof IFlagChangeEditor formEditor)
						{
							formEditor.flagModified();
						}
					});
				}
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
		});
	}

}
