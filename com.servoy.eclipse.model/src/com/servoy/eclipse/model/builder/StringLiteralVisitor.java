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

package com.servoy.eclipse.model.builder;

import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.javascript.ast.CallExpression;
import org.eclipse.dltk.javascript.ast.Identifier;
import org.eclipse.dltk.javascript.ast.PropertyExpression;
import org.eclipse.dltk.javascript.ast.StringLiteral;

/**
 * JS file externalizable string literals visitor
 * @author gboros
 */
public abstract class StringLiteralVisitor extends org.eclipse.dltk.ast.ASTVisitor
{
	public static final String I18N_EXTERNALIZE_CALLBACK = "i18n.getI18NMessage";

	private boolean isEvaluatingI18N;
	private int parsingLineStartIdx, parsingLineEndIdx = -1;
	private int stringLiteralIdxInLine;

	private final char[] scriptContent;

	public StringLiteralVisitor(char[] scriptContent)
	{
		this.scriptContent = scriptContent;
	}

	public abstract boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx);

	@Override
	public boolean visitGeneral(ASTNode node) throws Exception
	{
		if (node instanceof StringLiteral)
		{
			if (!isEvaluatingI18N)
			{
				StringLiteral snode = (StringLiteral)node;
				String value = snode.getValue();
				if (value != null && (value.length() == 0 || value.startsWith("i18n:")))
				{
					isEvaluatingI18N = true;
				}
				else
				{
					int nodeLineStartIdx = getNodeLineStartIdx(snode);
					if (parsingLineEndIdx < 0)
					{
						parsingLineEndIdx = getNodeLineEndIdx(snode);
					}
					if (parsingLineStartIdx != nodeLineStartIdx)
					{
						parsingLineStartIdx = nodeLineStartIdx;
						parsingLineEndIdx = getNodeLineEndIdx(snode);
						stringLiteralIdxInLine = 0;
					}

					stringLiteralIdxInLine++;
					return visitStringLiteral(snode, parsingLineStartIdx, parsingLineEndIdx, stringLiteralIdxInLine);
				}
			}
		}
		else if (node instanceof CallExpression)
		{
			CallExpression mc = (CallExpression)node;
			if ((mc.getExpression() instanceof PropertyExpression) && ((PropertyExpression)mc.getExpression()).getObject() instanceof Identifier &&
				I18N_EXTERNALIZE_CALLBACK.equals(mc.getExpression().toString()))
			{
				isEvaluatingI18N = true;
			}
		}

		return true;
	}

	@Override
	public void endvisitGeneral(ASTNode node) throws Exception
	{
		if (isEvaluatingI18N)
		{
			if (node instanceof StringLiteral)
			{
				StringLiteral snode = (StringLiteral)node;
				String value = snode.getValue();
				if (value != null && (value.length() == 0 || value.startsWith("i18n:")))
				{
					isEvaluatingI18N = false;
				}
			}

			else if (node instanceof CallExpression)
			{
				CallExpression mc = (CallExpression)node;
				if ((mc.getExpression() instanceof PropertyExpression) && ((PropertyExpression)mc.getExpression()).getObject() instanceof Identifier &&
					I18N_EXTERNALIZE_CALLBACK.equals(mc.getExpression().toString()))
				{
					isEvaluatingI18N = false;
				}
			}
		}
	}

	private int getNodeLineStartIdx(StringLiteral node)
	{
		int startIdx = node.sourceStart();
		while (startIdx > -1 && scriptContent[startIdx] != '\n')
			startIdx--;

		return startIdx + 1;
	}

	private int getNodeLineEndIdx(StringLiteral node)
	{
		int endIdx = node.sourceEnd();
		while (endIdx < scriptContent.length && scriptContent[endIdx] != '\n')
			endIdx++;

		return endIdx - 1;
	}
}