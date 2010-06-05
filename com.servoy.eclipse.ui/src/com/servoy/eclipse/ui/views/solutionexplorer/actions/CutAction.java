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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

/**
 * Cuts the selected objects - if it is a valid selection from a "copy" point of view.
 * 
 * @author Andrei Costescu
 */
public class CutAction extends Action implements ISelectionChangedListener
{

	private final Display display;

	/**
	 * New cut action.
	 * 
	 * @param display the display used to create a clipboard.
	 */
	public CutAction(Display display)
	{
		this.display = display;
		setEnabled(false);

		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_CUT));
		setText("Cut");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		// TODO
	}

	@Override
	public void run()
	{
		// TODO
	}

}