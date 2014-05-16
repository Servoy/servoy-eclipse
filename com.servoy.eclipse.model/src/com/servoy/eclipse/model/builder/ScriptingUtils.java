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
package com.servoy.eclipse.model.builder;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.ast.ASTVisitor;
import org.eclipse.dltk.compiler.problem.IProblem;
import org.eclipse.dltk.compiler.problem.IProblemReporter;
import org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor;
import org.eclipse.dltk.javascript.ast.DecimalLiteral;
import org.eclipse.dltk.javascript.ast.Expression;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.GetArrayItemExpression;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.ObjectInitializer;
import org.eclipse.dltk.javascript.ast.ObjectInitializerPart;
import org.eclipse.dltk.javascript.ast.PropertyInitializer;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.Statement;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.ast.VoidExpression;
import org.eclipse.dltk.javascript.parser.JavaScriptParser;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Debug;

public class ScriptingUtils
{
	private static final JavaScriptParser javascriptParser = new JavaScriptParser();
	private static final IProblemReporter dummyReporter = new IProblemReporter()
	{
		public void reportProblem(IProblem problem)
		{
			// do nothing
		}

		public Object getAdapter(Class adapter)
		{
			return null;
		}

	};

	public static int getArgumentsUsage(final String declaration)
	{
		final Script script = javascriptParser.parse(declaration, dummyReporter);
		List<Statement> statements = script.getStatements();
		if (statements != null && statements.size() == 1 && (statements.get(0) instanceof VoidExpression))
		{
			Expression exp = ((VoidExpression)statements.get(0)).getExpression();
			if (exp instanceof FunctionStatement)
			{
				FunctionStatement function = (FunctionStatement)exp;
				final int functionStart = function.sourceStart();
				try
				{
					final int[] offset = new int[] { -1 };
					function.getBody().traverse(new ASTVisitor()
					{
						@Override
						public boolean visitGeneral(ASTNode node) throws Exception
						{
							if (node instanceof GetArrayItemExpression && (((GetArrayItemExpression)node).getArray() instanceof Identifier) &&
								"arguments".equals(((Identifier)((GetArrayItemExpression)node).getArray()).getName()))
							{
								offset[0] = 0;
								for (int i = functionStart; i < node.sourceStart(); i++)
								{
									if (declaration.charAt(i) == '\n') offset[0]++;
								}
								return false;
							}
							return true;
						}
					});
					return offset[0];
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		return -1;
	}

	/**
	 * @param global
	 * @param retval
	 */
	public static List<EnumDataProvider> getEnumDataProviders(final ScriptVariable global)
	{
		final List<EnumDataProvider> retval = new ArrayList<EnumDataProvider>();
		String defaultValue = global.getDefaultValue();
		JavaScriptParser parser = new JavaScriptParser();
		try
		{
			Script script = parser.parse(global.getName() + '=' + defaultValue, dummyReporter);
			script.visitAll(new AbstractNavigationVisitor<Object>()
			{
				@Override
				public Object visitObjectInitializer(ObjectInitializer node)
				{
					for (ObjectInitializerPart part : node.getInitializers())
					{
						if (part instanceof PropertyInitializer)
						{
							Object type = visit(((PropertyInitializer)part).getValue());
							int typeid = IColumnTypes.MEDIA;
							if (type instanceof Integer) typeid = ((Integer)type).intValue();
							retval.add(new EnumDataProvider(global.getDataProviderID() + '.' + ((PropertyInitializer)part).getNameAsString(), typeid));
						}
						else
						{
							Debug.error("unsuported enum constant: " + part);
						}
					}
					return null;
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor#visitDecimalLiteral(org.eclipse.dltk.javascript.ast.DecimalLiteral)
				 */
				@Override
				public Object visitDecimalLiteral(DecimalLiteral node)
				{
					return Integer.valueOf(IColumnTypes.NUMBER);
				}

				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.dltk.javascript.ast.AbstractNavigationVisitor#visitStringLiteral(org.eclipse.dltk.javascript.ast.StringLiteral)
				 */
				@Override
				public Object visitStringLiteral(StringLiteral node)
				{
					return Integer.valueOf(IColumnTypes.TEXT);
				}
			});
		}
		catch (Throwable throwable)
		{
			Debug.error(throwable);
		}
		return retval;
	}

}
