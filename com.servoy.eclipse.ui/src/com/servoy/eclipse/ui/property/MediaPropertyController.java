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
package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.dialogs.MediaContentProvider;
import com.servoy.eclipse.ui.dialogs.MediaPreview;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.j2db.FlattenedSolution;

/**
 * Property controller for selecting media in Properties view.
 * 
 * @author rgansevles
 *
 * @param <P> property type
 */
public class MediaPropertyController<P> extends PropertyController<P, MediaNode>
{

	private final PersistContext persistContext;
	private final boolean includeNone;

	public MediaPropertyController(Object id, String displayName, PersistContext persistContext, boolean includeNone)
	{
		super(id, displayName);
		this.persistContext = persistContext;
		this.includeNone = includeNone;
		setLabelProvider(new SolutionContextDelegateLabelProvider(new MediaLabelProvider(), persistContext.getContext()));
		setSupportsReadonly(true);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		final FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
		return new ListSelectCellEditor(parent, "Select image", new MediaContentProvider(flattenedEditingSolution, persistContext.getContext()),
			getLabelProvider(), new IValueEditor<MediaNode>()
			{
				public void openEditor(MediaNode value)
				{
					EditorUtil.openMediaViewer(value.getMedia());
				}

				public boolean canEdit(MediaNode value)
				{
					return value != null && value.getMedia() != null;
				}
			}, isReadOnly(), new MediaContentProvider.MediaListOptions(includeNone), SWT.NONE, new ListSelectCellEditor.ListSelectControlFactory()
			{
				private TreeSelectDialog dialog;

				public void setTreeSelectDialog(TreeSelectDialog dialog)
				{
					this.dialog = dialog;
				}

				public Control createControl(Composite composite)
				{
					return new MediaPreview(composite, SWT.NONE, flattenedEditingSolution, dialog.getTreeViewer());
				}
			}, "selectImageDialog");
	}
}
