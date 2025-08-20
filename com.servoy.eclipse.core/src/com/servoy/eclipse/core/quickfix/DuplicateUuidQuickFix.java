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

import java.io.OutputStream;
import java.util.Iterator;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.Relation;
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
		this.persist = persist;
		this.uuid = uuid;
		this.solutionName = solName;
	}

	public String getLabel()
	{
		String fileName = SolutionSerializer.getFileName(persist, false);
		if (fileName != null)
		{
			return "Generate new uuid for '" + SolutionSerializer.getRelativePath(persist, false) + fileName + "' (" + persist + ").";
		}
		return "Generate new uuid for persist with uuid '" + persist.getUUID() + "' (" + persist + ").";
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
					if (persist.getParent() instanceof Relation)
					{
						// also do the children
						Iterator<IPersist> allObjects = persist.getParent().getAllObjects();
						while (allObjects.hasNext())
						{
							IPersist child = allObjects.next();
							((AbstractBase)child).resetUUID();
						}
					}
					else
					{
						((AbstractBase)persist).resetUUID();
					}
					Pair<String, String> newfilepathname = SolutionSerializer.getFilePath(persist, false);
					String newfileRelativePath = newfilepathname.getLeft() + newfilepathname.getRight();
					if (!newfileRelativePath.equals(fileRelativePath))
					{
						fileAccess.delete(fileRelativePath);
					}
					Object content = null;
					if (persist instanceof IScriptElement)
					{
						content = SolutionSerializer.generateScriptFile(persist.getParent(), SolutionSerializer.getScriptPath(persist, false), repository,
							null);
					}
					else
					{
						Form form = (Form)persist.getAncestor(IRepository.FORMS);
						if (form != null && SolutionSerializer.isCompositeWithItems(form))
						{
							content = SolutionSerializer.serializePersist(form, true, repository, null);
						}
						else if (SolutionSerializer.isCompositeWithItems(persist.getParent()))
						{
							content = SolutionSerializer.serializePersist(persist.getParent(), true, repository, null);
						}
						else
						{
							content = SolutionSerializer.serializePersist(persist, true, repository, null);
						}
					}

					OutputStream fos = fileAccess.getOutputStream(newfileRelativePath);
					if (content instanceof byte[])
					{
						fos.write((byte[])content);
					}
					else if (content instanceof CharSequence)
					{
						fos.write(content.toString().getBytes("UTF8"));
					}
					Utils.closeOutputStream(fos);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

	}
}