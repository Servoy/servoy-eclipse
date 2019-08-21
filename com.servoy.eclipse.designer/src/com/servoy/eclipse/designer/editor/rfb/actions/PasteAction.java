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

package com.servoy.eclipse.designer.editor.rfb.actions;

import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.internal.GEFMessages;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.actions.PasteCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.mobile.commands.SelectModelsCommandWrapper;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportChilds;

/** Command to past previously copied editor models.
 *
 * @author rgansevles
 *
 */
public class PasteAction extends SelectionAction
{
	private final IApplication application;
	private final ISelectionProvider selectionProvider;
	private final IFieldPositioner fieldPositioner;
	private final Form context;

	/**
	 * Constructs a <code>CopyAction</code> using the specified part.
	 *
	 * @param part
	 *            The part for this action
	 */
	public PasteAction(IApplication application, ISelectionProvider selectionProvider, BaseVisualFormEditor part, IFieldPositioner fieldPositioner)
	{
		super(part);
		this.application = application;
		this.selectionProvider = selectionProvider;
		this.fieldPositioner = fieldPositioner;
		this.context = part.getForm();
	}

	@Override
	protected void init()
	{
		super.init();
		setText(GEFMessages.PasteAction_Label);
		setToolTipText(GEFMessages.PasteAction_Tooltip);
		setId(ActionFactory.PASTE.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE_DISABLED));
	}

	/**
	 * Create a command to paste the clipboard.
	 */
	public Command createPasteCommand(List< ? > objects)
	{
		final Object selected = objects.get(0);

		ISupportChilds parent = null;
		IPersist persist = Platform.getAdapterManager().getAdapter(selected, IPersist.class);
		if (persist != null)
		{
			if (persist instanceof ISupportChilds)
			{
				parent = (ISupportChilds)persist;
			}
			else
			{
				parent = context;
			}
		}

		return new SelectModelsCommandWrapper(selectionProvider, new PasteCommand(application, parent, Collections.emptyMap(), context, fieldPositioner));
	}

	@Override
	protected boolean calculateEnabled()
	{
		boolean isDesignerContextActive = getWorkbenchPart() instanceof BaseVisualFormEditor &&
			((BaseVisualFormEditor)getWorkbenchPart()).isDesignerContextActive();
		return isDesignerContextActive ? getSelectedObjects().size() > 0 : false;
	}

	@Override
	public void run()
	{
		execute(createPasteCommand(getSelectedObjects()));
	}
}