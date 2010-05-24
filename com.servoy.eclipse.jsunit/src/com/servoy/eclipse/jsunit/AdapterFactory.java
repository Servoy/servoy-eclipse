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
package com.servoy.eclipse.jsunit;

import org.eclipse.core.runtime.IAdapterFactory;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;

public class AdapterFactory implements IAdapterFactory
{

	private class DummySolutionUnitTestTarget implements SolutionUnitTestTarget
	{
		// for now only active solutions can be the test target of such a run, so this class is only for attaching
		// the "run unit tests" action to the correct node - no other info is needed 
	}

	private static Class[] ADAPTERS = new Class[] { SolutionUnitTestTarget.class };

	public Object getAdapter(Object object, Class adapterType)
	{
		if (adapterType == SolutionUnitTestTarget.class)
		{
			if (object instanceof SimpleUserNode)
			{
				SimpleUserNode node = (SimpleUserNode)object;
				if (node.getRealObject() == ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject())
				{
					ServoyProject sp = (ServoyProject)node.getRealObject();
					if (sp != null)
					{
						// if other nodes can be targeted in the future, the SolutionUnitTestTarget obj. created here
						// should be able to identify the target (should contain some data)
						return new DummySolutionUnitTestTarget();
					}
				}
			}
		}
		return null;
	}

	public Class[] getAdapterList()
	{
		return ADAPTERS;
	}

}
