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

package com.servoy.eclipse.designer.editor.html.actions;

import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;

/** Command to cut selected editor models.
 * 
 * @author rgansevles
 *
 */
public class CutAction extends SelectionAction
{
	private final CopyAction copyAction;
	private final DeleteAction deleteAction;

	/**
	 * Constructs a <code>CopyAction</code> using the specified part.
	 * 
	 * @param part
	 *            The part for this action
	 */
	public CutAction(IWorkbenchPart part)
	{
		super(part);
		copyAction = new CopyAction(part);
		deleteAction = new DeleteAction(part);
	}

	@Override
	protected void init()
	{
		super.init();
		setText("Cut");
		setToolTipText("Cut selected objects");
		setId(ActionFactory.CUT.getId());
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();
		setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		setDisabledImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_CUT_DISABLED));
	}

	@Override
	public void setSelectionProvider(ISelectionProvider provider)
	{
		super.setSelectionProvider(provider);
		copyAction.setSelectionProvider(provider);
		deleteAction.setSelectionProvider(provider);
	}

	@Override
	public void update()
	{
		copyAction.update();
		deleteAction.update();
		super.update();
	}

	@Override
	protected boolean calculateEnabled()
	{
		return copyAction.calculateEnabled() && deleteAction.calculateEnabled();
	}

	@Override
	public void run()
	{
		copyAction.run();
		deleteAction.run();
	}
}