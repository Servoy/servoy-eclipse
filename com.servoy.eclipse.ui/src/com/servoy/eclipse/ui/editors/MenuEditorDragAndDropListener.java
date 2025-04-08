/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

package com.servoy.eclipse.ui.editors;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;

import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerDropAdapter;
import org.eclipse.swt.dnd.DragSourceEvent;
import org.eclipse.swt.dnd.DragSourceListener;
import org.eclipse.swt.dnd.TransferData;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.UUID;

/**
 * @author gabi
 *
 */
public class MenuEditorDragAndDropListener extends ViewerDropAdapter implements DragSourceListener
{
	private MenuItem dragMenu;

	private final MenuEditor menuEditor;
	private final ArrayList<Pair<UUID, UUID>> unsavedDragAndDrops = new ArrayList();

	public MenuEditorDragAndDropListener(Viewer viewer, MenuEditor menuEditor)
	{
		super(viewer);
		this.menuEditor = menuEditor;
	}

	/*
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragStart(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragStart(DragSourceEvent event)
	{
		dragMenu = null;
		ISelection selection = getViewer().getSelection();
		if (selection instanceof IStructuredSelection)
		{
			Iterator< ? > iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext())
			{
				Object element = iterator.next();
				if (element instanceof MenuItem)
				{
					dragMenu = (MenuItem)element;
					break;
				}
			}
		}
		event.doit = dragMenu != null;
	}

	/*
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragSetData(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragSetData(DragSourceEvent event)
	{
	}

	/*
	 * @see org.eclipse.swt.dnd.DragSourceListener#dragFinished(org.eclipse.swt.dnd.DragSourceEvent)
	 */
	@Override
	public void dragFinished(DragSourceEvent event)
	{
		dragMenu = null;
	}

	/*
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#performDrop(java.lang.Object)
	 */
	@Override
	public boolean performDrop(Object data)
	{
		Object target = getCurrentTarget();
		if (target instanceof MenuItem && dragMenu != null && !target.equals(dragMenu))
		{
			MenuItem targetMenu = (MenuItem)target;
			moveMenu(dragMenu, targetMenu);
			getViewer().refresh();
			menuEditor.getPersist().flagChanged();
			menuEditor.flagModified();
			unsavedDragAndDrops.add(new Pair<UUID, UUID>(dragMenu.getUUID(), targetMenu.getUUID()));
			return true;
		}
		return false;
	}

	private void moveMenu(MenuItem dragMenu, MenuItem targetMenu)
	{
		((AbstractBase)dragMenu.getParent()).removeChild(dragMenu);
		int currentLocation = getCurrentLocation();
		switch (currentLocation)
		{
			case LOCATION_ON :
				targetMenu.addChild(dragMenu);
				break;
			case LOCATION_BEFORE :
			case LOCATION_AFTER :
				AbstractBase parent = ((AbstractBase)targetMenu.getParent());
				List<IPersist> parentMenus = parent.getAllObjectsAsList();
				int targetIdx = parentMenus.indexOf(targetMenu);
				if (currentLocation == LOCATION_AFTER) targetIdx++;
				parent.addChild(dragMenu, targetIdx);
		}
	}

	public void saveDragAndDrops()
	{
		if (unsavedDragAndDrops.size() > 0)
		{
			String solutionName = ((Solution)menuEditor.getPersist().getRootObject()).getName();
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			Solution solution = servoyProject.getSolution();

			for (Pair<UUID, UUID> unsavedDND : unsavedDragAndDrops)
			{
				try
				{
					MenuItem solutionDragMenu = (MenuItem)solution.searchChild(unsavedDND.getLeft()).get();
					MenuItem solutionTargetMenu = (MenuItem)solution.searchChild(unsavedDND.getRight()).get();
					moveMenu(solutionDragMenu, solutionTargetMenu);
				}
				catch (NoSuchElementException ex)
				{
					// ignore
				}
			}
			unsavedDragAndDrops.clear();
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(true,
				Arrays.asList(new IPersist[] { menuEditor.getPersist() }));
		}
	}

	public void revertDragAndDrops()
	{
		if (unsavedDragAndDrops.size() > 0)
		{
			try
			{
				// remove all child nodes so they can be reverted to their original position
				List<IPersist> firstLevelChildren = new ArrayList<>(this.menuEditor.getPersist().getAllObjectsAsList());
				for (IPersist child : firstLevelChildren)
				{
					((EclipseRepository)this.menuEditor.getPersist().getRootObject().getRepository()).deleteObject(child);
				}
			}
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
			unsavedDragAndDrops.clear();
		}
	}

	/*
	 * @see org.eclipse.jface.viewers.ViewerDropAdapter#validateDrop(java.lang.Object, int, org.eclipse.swt.dnd.TransferData)
	 */
	@Override
	public boolean validateDrop(Object target, int operation, TransferData transferType)
	{
		return target instanceof MenuItem && dragMenu != null;
	}
}
