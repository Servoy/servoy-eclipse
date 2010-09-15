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
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.DataRequest;
import com.servoy.eclipse.ui.dialogs.MediaContentProvider;
import com.servoy.eclipse.ui.dialogs.MediaPreview;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.util.IControlFactory;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;

/**
 * Present the user available images via a dialog.
 * <p>
 * The actual command is performed via the selected edit parts' edit policy.
 * 
 * @author rgansevles
 * 
 */
public class AddMediaActionDelegate extends AbstractEditpartActionDelegate
{
	public AddMediaActionDelegate()
	{
		super(VisualFormEditor.REQ_PLACE_MEDIA);
	}

	@Override
	protected Request createRequest(EditPart editPart)
	{
		if (!(editPart.getModel() instanceof IPersist))
		{
			return null;
		}

		final FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(
			(IPersist)editPart.getModel());
		final TreeSelectDialog dialog = new TreeSelectDialog(getShell(), true, false, TreePatternFilter.FILTER_LEAFS, new MediaContentProvider(
			flattenedSolution), new SolutionContextDelegateLabelProvider(new MediaLabelProvider(flattenedSolution), flattenedSolution.getSolution()), null,
			null, SWT.NONE, "Select image", new MediaContentProvider.MediaListOptions(false), null, TreeSelectDialog.MEDIA_DIALOG, null);
		dialog.setOptionsAreaFactory(new IControlFactory()
		{
			public Control createControl(Composite composite)
			{
				return new MediaPreview(composite, SWT.NONE, flattenedSolution, dialog.getTreeViewer(), dialog.getDialogBoundsSettings());
			}
		});
		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL)
		{
			return null;
		}

		// single selection
		return new DataRequest(getRequestType(),
			flattenedSolution.getMedia(((Integer)((IStructuredSelection)dialog.getSelection()).getFirstElement()).intValue()));
	}
}
