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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.nio.charset.Charset;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IViewPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.FileEditorInput;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Utils;

/**
 * @author jcompagner
 */
public class DeleteScriptAction extends DeletePersistAction
{

	private final IViewPart viewPart;

	/**
	 * @param type
	 * @param text
	 */
	public DeleteScriptAction(UserNodeType type, String text, IViewPart viewPart)
	{
		super(type, text);
		this.viewPart = viewPart;
	}

	@Override
	public void run()
	{
		if (selectedPersists == null) return;
		if (scriptsAreEditedByEditors(selectedPersists.toArray(new IPersist[selectedPersists.size()]), viewPart.getSite().getPage().getDirtyEditors()))
		{
			MessageDialog.openWarning(viewPart.getSite().getShell(), "Cannot delete",
				"There are unsaved open editors that would be affected by this delete.\nPlease save or discard changes in these editors first.");
		}
		else
		{
			super.run();
		}
	}


	/**
	 * Checks to see whether any of the given scripts is being edited by and of the given editors or not.
	 *
	 * @param scripts the scripts.
	 * @param editors the editors.
	 * @return true if any of the given scripts is being edited by and of the given editors; false otherwise.
	 */
	public static boolean scriptsAreEditedByEditors(IPersist[] scripts, IEditorPart[] editors)
	{
		boolean edited = false;
		for (int i = editors.length - 1; i >= 0 && !edited; i--)
		{
			IEditorInput input = editors[i].getEditorInput();
			if (input instanceof FileEditorInput)
			{
				IFile editedFile = ((FileEditorInput)input).getFile();
				// ok we have a dirty edited file - see if any of the scripts are in it
				for (int j = scripts.length - 1; j >= 0 && !edited; j--)
				{
					IFile scriptFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getScriptPath(scripts[j], false)));
					if (editedFile.equals(scriptFile))
					{
						edited = true;
					}
				}
			}
		}
		return edited;
	}

	@Override
	protected void performDeletion(List<IPersist> selectedPersists)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		for (IPersist persist : selectedPersists)
		{
			String relativePath = SolutionSerializer.getScriptPath(persist, false);
			Path path = new Path(relativePath);
			IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
			if (file == null || !file.exists() || file.getType() != IResource.FILE) continue;
			String txt = null;
			IEditorPart openEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(
				FileEditorInputFactory.createFileEditorInput(file));
			try
			{
				if (openEditor instanceof ScriptEditor)
				{
					txt = ((ScriptEditor)openEditor).getViewer().getDocument().get();
				}
				else
				{
					txt = Utils.getTXTFileContent(file.getContents(true), Charset.forName("UTF8"));
				}
				if (txt == null || txt.length() == 0) continue;

				// filtering out "\r"
				StringBuilder sbfileContent = null;
				int lastIndex = 0;
				for (int i = 0; i < txt.length(); i++)
				{
					if (txt.charAt(i) == '\r')
					{
						if (sbfileContent == null)
						{
							sbfileContent = new StringBuilder(txt.length());
						}
						sbfileContent.append(txt.substring(lastIndex, i));
						lastIndex = i + 1;
					}
				}
				if (sbfileContent != null)
				{
					sbfileContent.append(txt.substring(lastIndex));
					txt = sbfileContent.toString();
				}

				if (!txt.endsWith("\n")) txt += "\n";
				int startLine = -1;
				int endLine = -1;
				if (persist instanceof ScriptMethod)
				{
					ScriptMethod sm = (ScriptMethod)persist;

					String searchString = sm.getDeclaration();

					int index = txt.indexOf(searchString);

					startLine = countLines(txt.substring(0, index)) + 1; // 1 based.
					endLine = startLine + countLines(sm.getDeclaration()); // begin and end line and last }
				}
				else if (persist instanceof ScriptVariable)
				{
					ScriptVariable sv = (ScriptVariable)persist;

					String searchString = SolutionSerializer.VAR_KEYWORD + ' ' + sv.getName();

					int index = txt.indexOf(searchString);

					startLine = countLines(txt.substring(0, index)) + 1; // 1 based.
					endLine = startLine;
				}
				IMarker[] findMarkers = file.findMarkers(null, true, IResource.DEPTH_INFINITE);
				for (IMarker marker : findMarkers)
				{
					int lineNumber = marker.getAttribute(IMarker.LINE_NUMBER, -1);
					if (lineNumber != -1 && startLine <= lineNumber && endLine >= lineNumber)
					{
						marker.delete();
					}
				}
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
		super.performDeletion(selectedPersists);
	}

	private int countLines(String txt)
	{
		int lines = 0;
		int index = txt.indexOf('\n');
		while (index != -1)
		{
			lines++;
			index = txt.indexOf('\n', index + 1);
		}
		return lines;
	}
}
