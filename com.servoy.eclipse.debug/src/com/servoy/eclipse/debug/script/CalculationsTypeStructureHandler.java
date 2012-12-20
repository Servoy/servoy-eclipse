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

import java.util.Set;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.compiler.problem.IProblemCategory;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.javascript.ast.VariableStatement;
import org.eclipse.dltk.javascript.structure.IParentNode;
import org.eclipse.dltk.javascript.structure.IStructureHandler;
import org.eclipse.dltk.javascript.structure.IStructureNode;
import org.eclipse.dltk.javascript.structure.IStructureVisitor;
import org.eclipse.dltk.javascript.structure.VariableNode;
import org.eclipse.dltk.javascript.typeinference.ReferenceLocation;
import org.eclipse.dltk.javascript.typeinfo.IModelBuilder.IVariable;
import org.eclipse.dltk.javascript.typeinfo.ReferenceSource;
import org.eclipse.dltk.javascript.typeinfo.model.JSType;
import org.eclipse.dltk.javascript.typeinfo.model.Visibility;

/**
 * @author jcompagner
 *
 */
public class CalculationsTypeStructureHandler implements IStructureHandler
{
	/**
	 * @author jcompagner
	 *
	 */
	private static final class CalcVariable implements IVariable
	{
		private JSType type;
		private String name;
		private ReferenceLocation location;

		public void setType(JSType type)
		{
			this.type = type;
		}

		public void setName(String name)
		{
			this.name = name;
		}

		public void setLocation(ReferenceLocation location)
		{
			this.location = location;
		}

		public JSType getType()
		{
			return type;
		}

		public Set<IProblemCategory> getSuppressedWarnings()
		{
			return null;
		}

		public String getName()
		{
			return name;
		}

		public ReferenceLocation getLocation()
		{
			return location;
		}

		public void addSuppressedWarning(IProblemCategory warningCategoryId)
		{
		}

		public void setVisibility(Visibility visibility)
		{
		}

		public void setDeprecated(boolean deprecated)
		{
		}

		public boolean isDeprecated()
		{
			return false;
		}

		public Visibility getVisibility()
		{
			return Visibility.PUBLIC;
		}
	}


	private final IStructureVisitor visitor;
	private final ReferenceSource rs;

	/**
	 * @param rs 
	 * @param context
	 * @param visitor
	 */
	public CalculationsTypeStructureHandler(ReferenceSource rs, IStructureVisitor visitor)
	{
		this.rs = rs;
		this.visitor = visitor;
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.javascript.typeinfo.ITypeInferenceHandler#handle(org.eclipse.dltk.ast.ASTNode)
	 */
	public IStructureNode handle(ASTNode node)
	{
		if (node instanceof FunctionStatement && (((FunctionStatement)node).getParent() instanceof Script))
		{
			IParentNode parent = visitor.peek();
			Identifier name = ((FunctionStatement)node).getName();
			IVariable variable = new CalcVariable();
			variable.setName(name.getName());
			variable.setLocation(ReferenceLocation.create(rs, name.sourceStart(), name.sourceEnd()));
			VariableDeclaration declaration = new VariableDeclaration(new VariableStatement((FunctionStatement)node));
			declaration.setIdentifier(name);
			visitor.visit(((FunctionStatement)node).getBody()); // do visit the body so that calls are reported.
			return new VariableNode(parent, declaration, variable);
		}

		return IStructureHandler.CONTINUE;
	}
}
