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
import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.Pair;

/**
 * Adapter factory that allows the "Run JS Unit Tests" action to appear on solution nodes.
 * @author acostescu
 */
public class AdapterFactory implements IAdapterFactory
{

	// this class is used for attaching
	// the "run unit tests" action to the correct node
	private class NodeSolutionUnitTestTarget implements SolutionUnitTestTarget
	{

		private final SimpleUserNode node;

		public NodeSolutionUnitTestTarget(SimpleUserNode node)
		{
			this.node = node;
		}

		public TestTarget getTestTarget()
		{
			TestTarget retVal = null;
			switch (node.getType())
			{
				case SOLUTION_ITEM :
				case SOLUTION :
					retVal = new TestTarget(((ServoyProject)node.getRealObject()).getSolution());
					break;
				case FORM :
					retVal = new TestTarget((Form)node.getRealObject());
					break;
				case GLOBALS_ITEM :
					retVal = new TestTarget((Pair<Solution, String>)node.getRealObject());
					break;
				case FORM_METHOD :
					Form form = (Form)(((ScriptMethod)node.getRealObject()).getParent());
					retVal = new TestTarget(form, (ScriptMethod)node.getRealObject());
					break;
				case GLOBAL_METHOD_ITEM :
					String scope = (((ScriptMethod)node.getRealObject()).getScopeName());
					retVal = new TestTarget(scope, (ScriptMethod)node.getRealObject());
					break;
				default : // remains null
			}
			return retVal;
		}

	}

	private static Class< ? >[] ADAPTERS = new Class[] { SolutionUnitTestTarget.class };

	public Object getAdapter(Object object, Class adapterType)
	{
		Object retVal = null;
		if (adapterType == SolutionUnitTestTarget.class)
		{
			if (object instanceof SimpleUserNode)
			{
				SimpleUserNode node = (SimpleUserNode)object;
				UserNodeType type = node.getType();

				ServoyProject ap = ServoyModelFinder.getServoyModel().getActiveProject();
				if (ap != null && !SolutionMetaData.isServoyMobileSolution(ap.getSolution()))
				{
					if (type == UserNodeType.SOLUTION || type == UserNodeType.SOLUTION_ITEM)
					{
						ServoyProject sp = (ServoyProject)node.getRealObject();
						if (sp != null && ServoyModelManager.getServoyModelManager().getServoyModel().isSolutionActive(sp.getProject().getName()))
						{
							retVal = new NodeSolutionUnitTestTarget(node);
						}
					}
					else if (type == UserNodeType.FORM || type == UserNodeType.GLOBALS_ITEM || type == UserNodeType.FORM_METHOD ||
						type == UserNodeType.GLOBAL_METHOD_ITEM)
					{
						retVal = new NodeSolutionUnitTestTarget(node);
					}
				}
			}
		}
		return retVal;
	}

	public Class[] getAdapterList()
	{
		return ADAPTERS;
	}

}
