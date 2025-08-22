/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.ContentOutlineCommand;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author gganea@servoy.com
 *
 */
public class OpenScriptHandler extends ContentOutlineCommand implements IServerService
{
	private final BaseVisualFormEditor editorPart;
	public static final String OPEN_SUPER_SCRIPT_ID = "com.servoy.eclipse.designer.rfb.openscripteditor";
	public static final String FORM_PARAMETER_NAME = "com.servoy.eclipse.designer.rfb.openscripteditor.form";

	public OpenScriptHandler()
	{
		this.editorPart = null;
	}

	public OpenScriptHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	public Object executeMethod(String methodName, JSONObject args)
	{
		Form form = editorPart != null ? editorPart.getForm() : getEditorPart().getForm();
		if (args != null && args.has("f"))
		{
			form = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getForm(args.optString("f"));
		}
		final Form openForm = form;
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				EditorUtil.openScriptEditor(openForm, null, true);
			}
		});
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.commands.IHandler#execute(org.eclipse.core.commands.ExecutionEvent)
	 */
	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		Form openForm = editorPart != null ? editorPart.getForm() : getEditorPart().getForm();
		if (event.getParameter(FORM_PARAMETER_NAME) != null)
		{
			openForm = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getForm(event.getParameter(FORM_PARAMETER_NAME));
		}
		else
		{
			if (openForm.getExtendsID() != null)
			{
				TreeSelectDialog dialog = new TreeSelectDialog(new Shell(), true, true, TreePatternFilter.FILTER_LEAFS, FlatTreeContentProvider.INSTANCE,
					new LabelProvider()
					{
						@Override
						public String getText(Object element)
						{
							return ((Form)element).getName();
						};
					}, null, null, SWT.NONE, "Open Form in Script Editor", PersistHelper.getOverrideHierarchy(openForm), null, false, "Superform Dialog", null,
					false);
				dialog.open();
				if (dialog.getReturnCode() == Window.OK)
				{
					openForm = (Form)((StructuredSelection)dialog.getSelection()).getFirstElement();
				}
				else
				{
					return null;
				}
			}
		}
		if (openForm != null)
		{
			EditorUtil.openScriptEditor(openForm, null, true);
		}
		return null;
	}


}
