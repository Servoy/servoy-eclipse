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
package com.servoy.eclipse.designer.actions;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.Request;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.ui.dialogs.TemplateContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.views.PlaceFieldOptionGroup;

/**
 * Present the user available templates via a dialog.
 * <p>
 * The actual command is performed via the selected edit parts' edit policy.
 * 
 * @author rgansevles
 * 
 */
public class ApplyTemplateActionDelegate extends AbstractEditpartActionDelegate
{
	protected PlaceFieldOptionGroup optionsGroup;

	public ApplyTemplateActionDelegate()
	{
		super(VisualFormEditor.REQ_PLACE_TEMPLATE);
	}

	@Override
	protected Request createRequest(EditPart editPart)
	{
		TreeSelectDialog dialog = new TreeSelectDialog(getShell(), true, false, TreePatternFilter.FILTER_LEAFS, TemplateContentProvider.DEFAULT,
			new LabelProvider(), null, null, SWT.NONE, "Select template", TemplateContentProvider.TEMPLATES_DUMMY_INPUT, null, false,
			TreeSelectDialog.TEMPLATE_DIALOG);
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}

		// single selection
		return new DataRequest(getRequestType(), ((IStructuredSelection)dialog.getSelection()).getFirstElement());
	}
}
