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
package com.servoy.eclipse.core.quickfix;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * Quick fix that opens the file using a kind of editor, so user can fix the problem using that editor.
 * 
 * @author acostescu
 */
public class OpenUsingEditor implements IMarkerResolution
{

	private final String editorId;
	private String quickFixLabel;

	/**
	 * Create a new instance of this quick fix.
	 * 
	 * @param editorId the editor type used to open the file.
	 */
	public OpenUsingEditor(String editorId, String quickFixLabel)
	{
		if (editorId == null)
		{
			this.editorId = "org.eclipse.ui.DefaultTextEditor";
		}
		else
		{
			this.editorId = editorId;
		}
		if (quickFixLabel == null)
		{
			this.quickFixLabel = "Open file in editor in order to manually fix the problem.";
		}
		else
		{
			this.quickFixLabel = quickFixLabel;
		}
	}

	public void run(final IMarker marker)
	{
		if (!marker.exists()) return;
		UIUtils.runInUI(new Runnable()
		{
			public void run()
			{
				IEditorPart part = null;
				IResource resource = marker.getResource();
				if (resource instanceof IFile)
				{
					try
					{
						part = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new FileEditorInput((IFile)resource), editorId,
							true, IWorkbenchPage.MATCH_ID | IWorkbenchPage.MATCH_INPUT);
					}
					catch (PartInitException e)
					{
						ServoyLog.logError(e);
					}
				}
				if (part == null)
				{
					UIUtils.reportWarning("Quick fix encountered a problem", "Cannot open file using this editor type.");
				}
			}
		}, false);
	}

	public String getLabel()
	{
		return quickFixLabel;
	}

}