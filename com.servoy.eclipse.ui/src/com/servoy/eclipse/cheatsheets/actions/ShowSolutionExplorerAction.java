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

package com.servoy.eclipse.cheatsheets.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.util.Debug;

/**
 * Shows the Solution Explorer view, restoring it in case it is minimized.
 * 
 * @author gerzse
 */
public class ShowSolutionExplorerAction extends Action
{
	@Override
	public void run()
	{
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		IViewReference solex = page.findViewReference(SolutionExplorerView.PART_ID);
		if (solex == null)
		{
			try
			{
				page.showView(SolutionExplorerView.PART_ID);
				solex = page.findViewReference(SolutionExplorerView.PART_ID);
			}
			catch (PartInitException e)
			{
				Debug.log("Failed to show Solution Explorer.", e); //$NON-NLS-1$
			}
		}
		if (solex != null) page.setPartState(solex, IWorkbenchPage.STATE_RESTORED);
	}
}
