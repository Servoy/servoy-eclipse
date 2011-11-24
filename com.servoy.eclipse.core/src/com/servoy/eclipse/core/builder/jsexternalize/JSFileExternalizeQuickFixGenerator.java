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
import org.eclipse.dltk.compiler.problem.DefaultProblemIdentifier;
import org.eclipse.dltk.compiler.problem.IProblemIdentifier;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

/**
 * JS file externalize marker resolution generator
 * @author gboros
 *
 */
public class JSFileExternalizeQuickFixGenerator implements IMarkerResolutionGenerator
{
	private static IMarkerResolution[] NONE = new IMarkerResolution[0];

	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		IProblemIdentifier problemId = DefaultProblemIdentifier.getProblemId(marker);

		if (problemId == JSFileExternalizeProblem.NON_EXTERNALIZED_STRING)
		{
			IResource scriptFile = marker.getResource();
			if (scriptFile instanceof IFile)
			{
				int problemStartIdx = marker.getAttribute(IMarker.CHAR_START, -1);

				return new IMarkerResolution[] { new AddMissingNLSResolution((IFile)scriptFile, problemStartIdx), new GenerateSuppressWarningsResolution(
					"nls", (IFile)scriptFile, problemStartIdx) }; //$NON-NLS-1$
			}
		}

		return NONE;
	}
}
