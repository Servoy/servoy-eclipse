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
package com.servoy.eclipse.core.repository;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.util.ReturnValueRunnable;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.extensions.IUnexpectedSituationHandler;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Utils;

/**
 * Handles unexpected situations that occur in model classes. Some require user decisions.
 * @author acostescu
 */
public class UnexpectedSituationHandler implements IUnexpectedSituationHandler
{

	public boolean allowUnexpectedDBIWrite(final ITable t)
	{
		// we will ask the user if he really wants to do this
		// normally difference markers will be solved by quick fixes - not by editing database information from
		// memory and then saving it...
		ReturnValueRunnable asker = new ReturnValueRunnable()
		{
			public void run()
			{
				returnValue = new Boolean(MessageDialog.openQuestion(UIUtils.getActiveShell(), "Unexpected database information file write",
					"The database information file (.dbi) contents for table '" + t.getName() + "' of server '" + t.getServerName() +
						"' are about to be written. This table currently has associated error or warning markers for problems that might have prevented the loading of .dbi information in the first place. This means that you could be overwriting the current .dbi file contents with defaults.\nIf you are not sure why this happened, you should choose 'No', check the 'Problems' view for these error markers and try to solve them (see if context menu - Quick Fix is enabled).\n\nDo you wish to continue with the write?"));
			}
		};
		if (Display.getCurrent() != null)
		{
			asker.run();
		}
		else
		{
			Display.getDefault().syncExec(asker);
		}
		return ((Boolean)asker.getReturnValue()).booleanValue();
	}

	public void cannotFindRepository()
	{
		MessageDialog.openError(UIUtils.getActiveShell(), "Repository error", "Cannot find Servoy Eclipse repository.");
	}

	public void cannotWriteI18NFiles(final Exception ex)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				MessageDialog.openError(UIUtils.getActiveShell(), "Error", "Cannot write project I18N files.\n" + ex.getMessage());
			}
		});
	}

	public void writeOverExistingScriptFile(final IFile scriptFile, final String fileContent)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					IWorkbenchWindow[] workbenchWindows = PlatformUI.getWorkbench().getWorkbenchWindows();
					for (IWorkbenchWindow workbenchWindow : workbenchWindows)
					{
						if (workbenchWindow.getActivePage() == null) continue;
						IEditorReference[] editorReferences = workbenchWindow.getActivePage().getEditorReferences();
						for (IEditorReference reference : editorReferences)
						{
							IEditorInput editorInput = reference.getEditorInput();
							if (editorInput instanceof IFileEditorInput)
							{
								if (((IFileEditorInput)editorInput).getFile().equals(scriptFile))
								{

									IEditorPart editor = reference.getEditor(false);
									if (editor != null && editor.isDirty())
									{
										if (!MessageDialog.openQuestion(UIUtils.getActiveShell(), "Saving script changes with dirty editor",
											"Overwrite editor changes? (if not then (property) changes could be ignored)"))
										{
											return;
										}
									}
								}
							}
						}
					}
					scriptFile.setContents(Utils.getUTF8EncodedStream(fileContent), IResource.FORCE, null);
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
		});
	}

}
