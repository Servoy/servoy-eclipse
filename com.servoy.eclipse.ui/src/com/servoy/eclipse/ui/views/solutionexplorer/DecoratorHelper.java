/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.core.resources.IMarker;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.builder.ISourceLineTracker;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.JSDeclaration;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.VariableDeclaration;
import org.eclipse.dltk.utils.TextUtils;

/**
 * Provides useful methods for problem decorators in solex and form hierarchy views.
 * @author emera
 */
public class DecoratorHelper
{
	/**
	 * @param problemLevel
	 * @param jsMarkers
	 * @param sourceModule
	 * @param node
	 * @return
	 * @throws ModelException
	 */
	public static int getProblemLevel(IMarker[] jsMarkers, ISourceModule sourceModule, ASTNode node) throws ModelException
	{
		int problemLevel = -1;
		if (jsMarkers == null || node == null) return problemLevel;
		ISourceLineTracker sourceLineTracker = null;
		for (IMarker marker : jsMarkers)
		{
			if (marker.getAttribute(IMarker.SEVERITY, -1) > problemLevel)
			{
				int start = marker.getAttribute(IMarker.CHAR_START, -1);
				if (start != -1)
				{
					if (node.sourceStart() <= start && start <= node.sourceEnd())
					{
						problemLevel = marker.getAttribute(IMarker.SEVERITY, -1);
					}
				}
				else
				{
					int line = marker.getAttribute(IMarker.LINE_NUMBER, -1); // 1 based
					if (line != -1)
					{
						if (sourceLineTracker == null) sourceLineTracker = TextUtils.createLineTracker(sourceModule.getSource());
						// getLineNumberOfOffset == 0 based so +1 to match the markers line
						if (sourceLineTracker.getLineNumberOfOffset(node.sourceStart()) + 1 <= line &&
							line <= sourceLineTracker.getLineNumberOfOffset(node.sourceEnd()) + 1)
						{
							problemLevel = marker.getAttribute(IMarker.SEVERITY, -1);
						}
					}
				}

			}
		}
		return problemLevel;
	}

	public static FunctionStatement getFunctionStatementForName(Script script, String metName)
	{
		for (JSDeclaration dec : script.getDeclarations())
		{
			if (dec instanceof FunctionStatement)
			{
				FunctionStatement fstmt = (FunctionStatement)dec;
				if (fstmt.getFunctionName().equals(metName))
				{
					return fstmt;
				}
			}
		}
		return null;
	}

	public static VariableDeclaration getVariableDeclarationForName(Script script, String varName)
	{
		for (JSDeclaration dec : script.getDeclarations())
		{
			if (dec instanceof VariableDeclaration)
			{
				VariableDeclaration varDec = (VariableDeclaration)dec;
				if (varDec.getVariableName().equals(varName))
				{
					return varDec;
				}
			}
		}
		return null;
	}
}
