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
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.labelproviders.RelatedFormsLabelProvider;
import com.servoy.eclipse.ui.property.RelatedFormsContentProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;

/**
 * Present the user related tabs via a dialog.
 * <p>
 * The actual command is performed via the selected edit parts' edit policy.
 * 
 */
public class AddSplitPaneActionDelegate extends AbstractEditpartActionDelegate
{
	public AddSplitPaneActionDelegate()
	{
		super(VisualFormEditor.REQ_PLACE_SPLIT_PANE);
	}

	/**
	 * Check for access to a form.
	 */
	@Override
	protected boolean checkApplicable(EditPart editPart)
	{
		return getModel(editPart, IRepository.FORMS) != null && super.checkApplicable(editPart);
	}

	@Override
	protected Request createRequest(EditPart editPart)
	{
		Form form = (Form)getModel(editPart, IRepository.FORMS);
		if (form == null)
		{
			return null;
		}

		RelatedFormsContentProvider contentProvider = new RelatedFormsContentProvider(form)
		{
			@Override
			public Object[] getElements(Object inputElement)
			{
				Object[] initialElements = super.getElements(inputElement);
				Object[] modifiedElements = new Object[initialElements.length + 1];
				modifiedElements[0] = RelationContentProvider.NONE;
				System.arraycopy(initialElements, 0, modifiedElements, 1, initialElements.length);
				return modifiedElements;
			}

			@Override
			public boolean hasChildren(Object element)
			{
				return super.hasChildren(element) && !RelationContentProvider.NONE.equals(element);
			}
		};
		TreeSelectDialog dialog = new TreeSelectDialog(getShell(), true, true, TreePatternFilter.FILTER_LEAFS, contentProvider,
			RelatedFormsLabelProvider.INSTANCE, null, new LeafnodesSelectionFilter(contentProvider), SWT.MULTI, "Select split pane form", form, null,
			TreeSelectDialog.TAB_DIALOG, null);
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}

		return new DataRequest(getRequestType(), ((IStructuredSelection)dialog.getSelection()).toArray());
	}
}
