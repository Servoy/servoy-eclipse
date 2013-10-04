/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.mobile;

import com.servoy.eclipse.jsunit.actions.OpenEditorAtLineAction;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * Action that is able to point to a method based on information from a mobile client generated AssertionFailedError stack entry.
 * @author acostescu
 */
public class MobileStackOpenEditorAction extends OpenEditorAtLineAction
{

	private final String scope1Name;
	private final String scope2Name;
	private final String methodName;

	/**
	 * Currently used for mobile - points to the beginning of a method.
	 * @param scope1Name "scopes"/"forms"
	 * @param scope2Name "[scopeName]"/"[formName]"
	 */
	public MobileStackOpenEditorAction(String scope1Name, String scope2Name, String methodName, String filePath, int lineNo)
	{
		super(filePath, false, lineNo);
		this.scope1Name = scope1Name;
		this.scope2Name = scope2Name;
		this.methodName = methodName;
	}

	@Override
	public void run()
	{
		if (lineNumber == 0)
		{
			// then we have no line info; logs probably contain traces that would explain this; no browser native stack info or something else went wrong
			FlattenedSolution flattenedActiveSolution = ServoyModelFinder.getServoyModel().getFlattenedSolution();
			if (flattenedActiveSolution != null)
			{
				ScriptMethod sm = null;
				if ("scopes".equals(scope1Name))
				{
					sm = flattenedActiveSolution.getScriptMethod(scope2Name, methodName);
				}
				else if ("forms".equals(scope1Name))
				{
					Form form = flattenedActiveSolution.getForm(scope2Name);
					if (form != null) sm = form.getScriptMethod(methodName);
				}
				EditorUtil.openScriptEditor(sm, sm.getScopeName(), true);
			}
		}
		else
		{
			super.run();
		}
	}

}
