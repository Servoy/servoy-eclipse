/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.views;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.dialogs.PropertyPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.IRestorer;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SimplePersistFactory;
import com.servoy.j2db.persistence.Solution;


/**
 * This class represents a property page that display the currently selected solution
 * properties. 
 * 
 * @author acostache
 *
 */
public class SolutionPropertiesPage extends PropertyPage
{
	private ModifiedPropertySheetPage page;
	private Solution original;

	@Override
	protected Control createContents(Composite parent)
	{
		page = new ModifiedPropertySheetPage();
		page.createControl(parent);

		IWorkbenchPart part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
		IPersist persist = (IPersist)getElement().getAdapter(IPersist.class);
		if (persist instanceof Solution)
		{
			original = SimplePersistFactory.createDummyCopy((Solution)persist);
			original.copyPropertiesMap(((Solution)persist).getPropertiesMap(), true);
			page.selectionChanged(part, new StructuredSelection(persist));
		}

		return page.getControl();
	}

	@Override
	public boolean performCancel()
	{
		IPersist persist = (IPersist)getElement().getAdapter(IPersist.class);//selected element
		if (persist instanceof Solution)
		{
			IRestorer restorer = (IRestorer)Platform.getAdapterManager().getAdapter(original, IRestorer.class);
			Object state = restorer.getState(original);
			restorer.restoreState(persist, state);
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(original.getName());
			try
			{
				servoyProject.saveEditingSolutionNodes(new IPersist[] { persist }, false);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return super.performCancel();
	}

	@Override
	public boolean performOk()
	{
		//refresh the properties sheet page
		IViewReference[] iv = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
		for (IViewReference ref : iv)
		{
			if (ref.getId().equals("org.eclipse.ui.views.PropertySheet"))
			{
				PropertySheetPage psp = (PropertySheetPage)ref.getView(false).getAdapter(PropertySheetPage.class);
				if (psp != null) psp.refresh();
			}
		}
		return true;
	}

}