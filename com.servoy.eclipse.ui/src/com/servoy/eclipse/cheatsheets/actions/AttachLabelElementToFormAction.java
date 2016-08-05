/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.cheatsheets.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.json.JSONObject;

/**
 * @author gboros
 *
 */
public class AttachLabelElementToFormAction extends Action
{
	@Override
	public void run()
	{
		for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages())
		{
			for (IEditorReference editorReference : page.getEditorReferences())
			{
				final IEditorPart editor = editorReference.getEditor(false);
				if (editor instanceof ISupportCheatSheetActions)
				{
					((ISupportCheatSheetActions)editor).createNewComponent(
						new JSONObject("{'packageName':'servoydefault','name':'servoydefault-label','h':20,'w':100,'y':164,'x':32, 'text':'Hello World'}"));
					Display.getDefault().asyncExec(new Runnable()
					{
						@Override
						public void run()
						{
							editor.doSave(null);
						}
					});
					return;
				}
			}
		}
	}
}
