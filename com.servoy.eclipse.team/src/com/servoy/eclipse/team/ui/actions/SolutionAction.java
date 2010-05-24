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
package com.servoy.eclipse.team.ui.actions;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.mapping.ResourceMapping;
import org.eclipse.jface.action.IAction;
import org.eclipse.swt.widgets.Display;
import org.eclipse.team.core.RepositoryProvider;
import org.eclipse.team.internal.ui.actions.TeamAction;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.team.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.SimpleResourceMapping;

public abstract class SolutionAction extends TeamAction
{
	/**
	 * @see org.eclipse.team.internal.ui.actions.TeamAction#isEnabled()
	 */
	@Override
	public boolean isEnabled()
	{
		return getSelectedMappings().length > 0;
	}

	/**
	 * Split the resources into sets associated with their project/provider
	 */
	protected Map getRepositoryProviderMapping()
	{
		HashMap result = new HashMap();
		IResource[] resources = getSelectedResources();
		for (IResource element : resources)
		{
			RepositoryProvider provider = RepositoryProvider.getProvider(element.getProject());
			List list = (List)result.get(provider);
			if (list == null)
			{
				list = new ArrayList();
				result.put(provider, list);
			}
			list.add(new SimpleResourceMapping(element));
		}
		return result;
	}

	/**
	 * Return the selected resource mappings that are associated with the provider.
	 * 
	 * @return the selected resource mappings that are associated with the provider.
	 */
	protected ResourceMapping[] getSelectedMappings()
	{
		return getSelectedResourceMappings(Activator.getTypeId());
	}

	@Override
	protected void execute(IAction action) throws InvocationTargetException, InterruptedException
	{
		if (checkForUnsavedResources()) executeAction(action);
	}

	protected abstract void executeAction(IAction action) throws InvocationTargetException, InterruptedException;

	protected boolean checkForUnsavedResources()
	{
		final boolean[] okToContinue = new boolean[] { true };
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				okToContinue[0] = PlatformUI.getWorkbench().saveAllEditors(true);
			}
		});

		return okToContinue[0];
	}
}
