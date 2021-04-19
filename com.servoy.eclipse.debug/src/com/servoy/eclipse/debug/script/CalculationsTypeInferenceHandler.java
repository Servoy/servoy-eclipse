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
import org.eclipse.dltk.internal.javascript.ti.ChildReference;
import org.eclipse.dltk.internal.javascript.ti.TopValueCollection;
import org.eclipse.dltk.javascript.ast.ASTVisitor;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.ReturnStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.typeinference.IValueCollection;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinference.ValueCollectionFactory;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler;
import org.eclipse.dltk.javascript.typeinfo.ITypeInferencerVisitor;
import org.eclipse.dltk.javascript.typeinfo.JSTypeSet;

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
			JSTypeSet types = null;
			IValueCollection parent = visitor.peekContext();
			visitor.enterContext(ValueCollectionFactory.createScopeValueCollection(parent));
			try
			{
				visitor.visitFunctionBody((FunctionStatement)node);
				types = (JSTypeSet)visitor.getContext().getValue("returnTypes");
			}
			finally
			{
				visitor.leaveContext();
			}
			IValueReference child = parent.getChild(((FunctionStatement)node).getName().getName());
			if (types != null)
			{
				child.setAttribute("returnTypes", types);
			}
			return child;
		}
		if (node instanceof ReturnStatement && visitor instanceof ASTVisitor)
		{
			IValueReference ret = ((ASTVisitor<IValueReference>)visitor).visitReturnStatement((ReturnStatement)node);
			if (ret != null)
			{
				IRType declaredType = ret.getDeclaredType();
				if (declaredType != null && "Function".equals(declaredType.getName().toString()))
				{
					IValueCollection collection = this.visitor.peekContext();
					if (collection != null && collection.getParent() instanceof TopValueCollection)
					{
						TopValueCollection topValueCollection = (TopValueCollection)collection.getParent();
						if (topValueCollection.getChild(ret.getName()) instanceof ChildReference &&
							((ChildReference)topValueCollection.getChild(ret.getName()).getAttribute("returnedTypes")) != null)
						{
							visitor.getContext().setValue("returnTypes", topValueCollection.getChild(ret.getName()).getAttribute("returnedTypes"));
						}
					}
				}
				else
				{
					visitor.getContext().setValue("returnTypes", declaredType != null ? JSTypeSet.create(declaredType) : ret.getTypes());
				}
			}

		}
		return ITypeInferenceHandler.CONTINUE;
	}

}
