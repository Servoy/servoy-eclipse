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
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.dltk.ui.editor.IScriptAnnotation;
import org.eclipse.dltk.ui.text.IScriptCorrectionContext;
import org.eclipse.dltk.ui.text.IScriptCorrectionProcessor;

/**
 * Script corrector for externalize strings
 * @author gboros
 */
public class ExternalizeScriptCorrectionProcessor implements IScriptCorrectionProcessor
{

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#canFix(org.eclipse.dltk.ui.editor.IScriptAnnotation)
	 */
	public boolean canFix(IScriptAnnotation annotation)
	{
		return annotation.getId() == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#canFix(org.eclipse.core.resources.IMarker)
	 */
	public boolean canFix(IMarker marker)
	{
		return false;
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#computeQuickAssistProposals(org.eclipse.dltk.ui.editor.IScriptAnnotation,
	 * org.eclipse.dltk.ui.text.IScriptCorrectionContext)
	 */
	public void computeQuickAssistProposals(IScriptAnnotation annotation, IScriptCorrectionContext context)
	{
		if (annotation.getId() == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING)
		{
			IResource scriptFile = annotation.getSourceModule().getResource();
			if (scriptFile instanceof IFile)
			{
				int problemStartIdx = context.getInvocationContext().getOffset();

				context.addResolution(new AddMissingNLSResolution((IFile)scriptFile, problemStartIdx), annotation);
				context.addResolution(new GenerateSuppressWarningsResolution("nls", (IFile)scriptFile, problemStartIdx), annotation); //$NON-NLS-1$
			}
		}
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IScriptCorrectionProcessor#computeQuickAssistProposals(org.eclipse.core.resources.IMarker,
	 * org.eclipse.dltk.ui.text.IScriptCorrectionContext)
	 */
	public void computeQuickAssistProposals(IMarker marker, IScriptCorrectionContext context)
	{
	}

}
