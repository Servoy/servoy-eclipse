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
package com.servoy.eclipse.core.builder;

import java.io.OutputStream;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.IFileAccess;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.WorkspaceFileAccess;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.IVariable;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

public class DuplicateUuidQuickFix implements IMarkerResolution
{
	private final IPersist persist;
	private final String uuid;
	private final String solutionName;

	public DuplicateUuidQuickFix(IPersist persist, String uuid, String solName)
	{
		super();
		if (SolutionSerializer.isCompositeWithItems(persist.getParent()))
		{
			this.persist = persist.getParent();
		}
		else
		{
			this.persist = persist;
		}
		this.uuid = uuid;
		this.solutionName = solName;
	}

	public String getLabel()
	{
		String fileName = SolutionSerializer.getFileName(persist, false);
		if (fileName != null)
		{
			return "Generate new uuid for '" + SolutionSerializer.getRelativePath(persist, false) + fileName + "'."; //$NON-NLS-1$ //$NON-NLS-2$
		}
		return "Generate new uuid for persist with id '" + persist.getID() + "'."; //$NON-NLS-1$ //$NON-NLS-2$
	}

	public void run(IMarker marker)
	{
		if (uuid != null)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(solutionName);
			if (servoyProject != null)
			{
				try
				{
					// as everything depends on uuid we have to do this low level
					EclipseRepository repository = (EclipseRepository)servoyProject.getSolution().getRepository();
					IFileAccess fileAccess = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
					Pair<String, String> filepathname = SolutionSerializer.getFilePath(persist, false);
					String fileRelativePath = filepathname.getLeft() + filepathname.getRight();
					((AbstractBase)persist).resetUUID();
					if (persist instanceof ISupportChilds && SolutionSerializer.isCompositeWithItems(persist))
					{
						// also do the children
						Iterator<IPersist> allObjects = ((ISupportChilds)persist).getAllObjects();
						while (allObjects.hasNext())
						{
							IPersist child = allObjects.next();
							((AbstractBase)child).resetUUID();
						}
					}
					Pair<String, String> newfilepathname = SolutionSerializer.getFilePath(persist, false);
					String newfileRelativePath = newfilepathname.getLeft() + newfilepathname.getRight();
					if (!newfileRelativePath.equals(fileRelativePath))
					{
						fileAccess.delete(fileRelativePath);
					}
					Object content = null;
					if (persist instanceof IVariable || persist instanceof IScriptProvider)
					{
						content = SolutionSerializer.generateScriptFile(persist.getParent(), repository);
					}
					else
					{
						content = SolutionSerializer.serializePersist(persist, true, repository);
					}

					OutputStream fos = fileAccess.getOutputStream(newfileRelativePath);
					if (content instanceof byte[])
					{
						fos.write((byte[])content);
					}
					else if (content instanceof CharSequence)
					{
						fos.write(content.toString().getBytes("UTF8")); //$NON-NLS-1$
					}
					Utils.closeOutputStream(fos);
					MessageDialog.openInformation(Display.getCurrent().getActiveShell(), "UUID generated sucessfully", //$NON-NLS-1$
						"Restart application in order for the change to be effective."); //$NON-NLS-1$
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}
}