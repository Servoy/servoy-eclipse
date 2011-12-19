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

package com.servoy.eclipse.core.builder.jsexternalize;

import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.ast.ASTNode;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.javascript.ast.Comment;
import org.eclipse.dltk.javascript.ast.FunctionStatement;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.text.edits.InsertEdit;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Resolution to add suppress warning on the enclosing method
 * @author gboros
 */
class GenerateSuppressWarningsResolution extends TextFileEditResolution
{
	private final String type;

	public GenerateSuppressWarningsResolution(String type, IFile scriptFile, int problemStartIdx)
	{
		super(scriptFile, problemStartIdx);
		this.type = type;
	}

	public String getLabel()
	{
		FunctionStatement f = getFunction();
		return "Add SuppressWarnings(" + type + ") to '" + (f != null ? f.getFunctionName() : "") + "'";
	}

	private String getAnnotation()
	{
		return "@SuppressWarnings(" + type + ")"; //$NON-NLS-1$ //$NON-NLS-2$
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.ui.text.IAnnotationResolution2#getDescription()
	 */
	@SuppressWarnings("nls")
	public String getDescription()
	{
		FunctionStatement f = getFunction();
		ISourceModule sourceModule = DLTKCore.createSourceModuleFrom(scriptFile);
		try
		{
			String functionString = sourceModule.getBuffer().getText(f.sourceStart(), f.sourceEnd() - f.sourceStart());
			int lineEnd = functionString.indexOf('\n');
			StringBuilder sb = new StringBuilder(lineEnd + 100);
			sb.append("<html><body> *<b>");
			sb.append(getAnnotation());
			sb.append("</b><br/> */<br/>");
			sb.append(functionString.substring(0, lineEnd));
			sb.append("</body></html>");
			return sb.toString();
		}
		catch (ModelException e)
		{
		}
		return getLabel();
	}

	private FunctionStatement functionAtPosition;

	FunctionStatement getFunction()
	{
		if (functionAtPosition == null)
		{
			Script script = JavaScriptParserUtil.parse(DLTKCore.createSourceModuleFrom(scriptFile));

			try
			{
				script.traverse(new org.eclipse.dltk.ast.ASTVisitor()
				{
					@Override
					public boolean visitGeneral(ASTNode node) throws Exception
					{
						if (node instanceof FunctionStatement)
						{
							FunctionStatement fs = (FunctionStatement)node;
							if (fs.sourceStart() < problemStartIdx && problemStartIdx < fs.sourceEnd())
							{
								functionAtPosition = (FunctionStatement)node;
								return false;
							}
						}
						return true;
					}
				});
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}

		return functionAtPosition;
	}

	/*
	 * @see com.servoy.eclipse.core.builder.jsexternalize.TextFileEditResolution#run(org.eclipse.core.resources.IFile, int)
	 */
	@Override
	public void run()
	{
		FunctionStatement fs = getFunction();
		if (fs != null)
		{
			int insertOffset = -1;
			Comment documentation = fs.getDocumentation();
			if (documentation.isDocumentation()) // it must have doc
			{
				insertOffset = documentation.sourceEnd() - 2;
				InsertEdit suppressTextEdit = new InsertEdit(insertOffset, "* " + getAnnotation() + "\n "); //$NON-NLS-1$ //$NON-NLS-2$
				TextFileEditUtil.applyTextEdit(scriptFile, suppressTextEdit);
			}
		}
	}
}
