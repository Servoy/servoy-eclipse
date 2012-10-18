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

package com.servoy.eclipse.debug.script;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor;

/**
 * @author jcompagner
 *
 */
public class CalculationsTypeInferenceHandler implements ITypeInferenceHandler
{
	private final ITypeInferencerVisitor visitor;

	/**
	 * @param context
	 * @param visitor
	 */
	public CalculationsTypeInferenceHandler(ITypeInferencerVisitor visitor)
	{
		this.visitor = visitor;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler#handle(org.eclipse.dltk.ast.ASTNode)
	 */
	public IValueReference handle(ASTNode node)
	{
		if (node instanceof FunctionStatement && (((FunctionStatement)node).getParent() instanceof Script))
		{
			IValueCollection parent = visitor.peekContext();
			visitor.enterContext(ValueCollectionFactory.createScopeValueCollection(parent));
			try
			{
				visitor.visitFunctionBody((FunctionStatement)node);
			}
			finally
			{
				visitor.leaveContext();
			}
			return parent.getChild(((FunctionStatement)node).getName().getName());
		}

		return ITypeInferenceHandler.CONTINUE;
	}

}
