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
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.ast.StringLiteral;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.text.edits.InsertEdit;

import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Resolution to add missing NLS to the source line
 * @author gboros
 */
public class AddMissingNLSResolution extends TextFileEditResolution
{
	public AddMissingNLSResolution(IFile scriptFile, int problemStartIdx)
	{
		super(scriptFile, problemStartIdx);
	}

	/*
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	public String getLabel()
	{
		return "Insert missing $NON-NLS-<n>$";
	}

	/*
	 * @see com.servoy.eclipse.core.builder.jsexternalize.TextFileEditResolution#run(org.eclipse.core.resources.IFile, int)
	 */
	@Override
	public void run()
	{
		ISourceModule scriptFileSourceModule = DLTKCore.createSourceModuleFrom(scriptFile);
		Script script = JavaScriptParserUtil.parse(scriptFileSourceModule);

		try
		{
			script.traverse(new StringLiteralVisitor(scriptFileSourceModule.getSourceAsCharArray())
			{

				@Override
				public boolean visitStringLiteral(StringLiteral node, int lineStartIdx, int lineEndIdx, int stringLiteralIdx)
				{
					int nodeOffset = node.getRange().getOffset();
					if (nodeOffset <= problemStartIdx && problemStartIdx < nodeOffset + node.getRange().getLength())
					{
						InsertEdit nonNLSTextEdit = new InsertEdit(lineEndIdx + 1, " //$NON-NLS-" + stringLiteralIdx + "$"); //$NON-NLS-1$ //$NON-NLS-2$
						applyTextEdit(scriptFile, nonNLSTextEdit);
						return false;
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
}
