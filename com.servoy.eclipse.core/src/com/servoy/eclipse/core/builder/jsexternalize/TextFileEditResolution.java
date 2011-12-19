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
import org.eclipse.dltk.ui.editor.IScriptAnnotation;
import org.eclipse.dltk.ui.text.IAnnotationResolution2;
import org.eclipse.jface.text.IDocument;
import org.eclipse.ui.IMarkerResolution;

/**
 * Base resolution class when file modification is needed
 * @author gboros
 *
 */
public abstract class TextFileEditResolution implements IMarkerResolution, IAnnotationResolution2
{
	protected IFile scriptFile;
	protected int problemStartIdx;

	public TextFileEditResolution(IFile scriptFile, int problemStartIdx)
	{
		this.scriptFile = scriptFile;
		this.problemStartIdx = problemStartIdx;
	}

	public abstract void run();

	/*
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	public void run(final IMarker marker)
	{
		run();
	}

	/*
	 * @see org.eclipse.dltk.ui.text.IAnnotationResolution#run(org.eclipse.dltk.ui.editor.IScriptAnnotation, org.eclipse.jface.text.IDocument)
	 */
	public void run(IScriptAnnotation annotation, IDocument document)
	{
		run();
	}
}