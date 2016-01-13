/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.ui.IMarkerResolution;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;

/**
 * @author emera
 */
public class ClearLingeringTableFilesQuickFix implements IMarkerResolution
{

	private final TableNode tableNode;
	private final ServoyProject project;

	/**
	 * @param persist
	 * @param servoyProject
	 */
	public ClearLingeringTableFilesQuickFix(IPersist persist, ServoyProject servoyProject)
	{
		this.tableNode = (TableNode)persist;
		this.project = servoyProject;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#getLabel()
	 */
	@Override
	public String getLabel()
	{
		return "Delete lingering files for table " + tableNode.getTableName();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.IMarkerResolution#run(org.eclipse.core.resources.IMarker)
	 */
	@Override
	public void run(IMarker marker)
	{
		try
		{
			IRootObject rootObject = tableNode.getRootObject();
			if (rootObject instanceof Solution)
			{
				EclipseRepository repository = (EclipseRepository)rootObject.getRepository();
				IPersist editingNode = project.getEditingPersist(tableNode.getUUID());
				repository.deleteObject(editingNode);

				project.saveEditingSolutionNodes(new IPersist[] { editingNode.getParent() }, true);

				IFolder parent = project.getProject().getFolder("datasources/" + tableNode.getServerName());
				parent.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
				deleteResource(parent, tableNode.getTableName() + ".tbl");
				deleteResource(parent, tableNode.getTableName() + "_calculations.js");
				deleteResource(parent, tableNode.getTableName() + "_aggregations.js");
				parent.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

	}

	/**
	 * @param parent
	 * @param name
	 * @throws CoreException
	 */
	private void deleteResource(IFolder parent, String name) throws CoreException
	{
		IFile file = parent.getFile(name);
		if (file != null && file.exists())
		{
			file.refreshLocal(IResource.DEPTH_INFINITE, new NullProgressMonitor());
			file.delete(true, new NullProgressMonitor());
		}
	}

}
