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
package com.servoy.eclipse.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.Iterator;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;

import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.IFileAccess;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AbstractScriptProvider;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.TableNode;

public class ServoyUpdatingProject implements IProjectNature
{
	/**
	 * ID of this project nature
	 */
	public static final String NATURE_ID = "com.servoy.eclipse.core.ServoyUpdatingProject"; //$NON-NLS-1$
	public static final int UPDATE_38 = SolutionSerializer.VERSION_38;
	private IProject project;

	public void configure() throws CoreException
	{
		if (project != null)
		{
			try
			{
				if (project.hasNature(ServoyProject.NATURE_ID))
				{
					Solution projectSolution = ((ServoyProject)project.getNature(ServoyProject.NATURE_ID)).getSolution();
					if (projectSolution != null)
					{
						SolutionMetaData solutionMetaData = projectSolution.getSolutionMetaData();
						if (solutionMetaData != null)
						{
							int fileVersion = solutionMetaData.getFileVersion();
							if (fileVersion < UPDATE_38) update_38();
						}
						else ServoyLog.logWarning("servoy updating project solution meta data is null", null); //$NON-NLS-1$
					}
					else ServoyLog.logWarning("servoy updating project solution is null", null); //$NON-NLS-1$

				}
			}
			finally
			{
				IProjectDescription updatingProjectDescription = project.getDescription();
				String[] natures = updatingProjectDescription.getNatureIds();
				ArrayList<String> newNatures = new ArrayList<String>();
				for (String nature : natures)
				{
					if (!nature.equals(ServoyUpdatingProject.NATURE_ID)) newNatures.add(nature);
				}

				updatingProjectDescription.setNatureIds(newNatures.toArray(new String[newNatures.size()]));
				project.setDescription(updatingProjectDescription, null);
			}
		}
		else ServoyLog.logWarning("servoy updating project called with null project", null); //$NON-NLS-1$
	}

	public void deconfigure() throws CoreException
	{
	}

	public IProject getProject()
	{
		return project;
	}

	public void setProject(IProject project)
	{
		this.project = project;
	}

	private void update_38() throws CoreException
	{
		project.refreshLocal(IResource.DEPTH_INFINITE, null);
		ServoyProject updatingServoyProject = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);
		Solution updatingSolution = updatingServoyProject.getSolution();
		project.accept(new IResourceVisitor()
		{
			public boolean visit(IResource resource) throws CoreException
			{
				if (resource.getType() == IResource.FOLDER)
				{
					if (".svn".equalsIgnoreCase(resource.getName())) return false; //$NON-NLS-1$

					IResource methods_js = null;
					boolean needToDelete = false;
					String newName = null;
					if (resource.getParent().getName().equals(SolutionSerializer.FORMS_DIR))
					{
						needToDelete = true;
						methods_js = ((IFolder)resource).findMember(resource.getName() + "_methods" + SolutionSerializer.JS_FILE_EXTENSION); //$NON-NLS-1$
						newName = resource.getName();
					}
					else if (resource.getParent().getParent() != null &&
						resource.getParent().getParent().getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME))
					{
						needToDelete = true;
						methods_js = ((IFolder)resource).findMember(resource.getName() + SolutionSerializer.CALCULATIONS_POSTFIX);
						newName = resource.getName() + SolutionSerializer.CALCULATIONS_POSTFIX_WITHOUT_EXT;
					}

					if (methods_js != null)
					{
						IPath newMethods_js = resource.getParent().getFullPath().append(newName + SolutionSerializer.JS_FILE_EXTENSION);
						methods_js.move(newMethods_js, true, null);
					}
					if (!resource.isTeamPrivateMember() && needToDelete)
					{
						resource.delete(true, null);
						return false;
					}
				}
				else if (resource.getType() == IResource.FILE && resource.getName().endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION))
				{
					String newExtension = null;
					IResource parentOfParent = resource.getParent().getParent();
					if (parentOfParent != null && parentOfParent.getName().equals(SolutionSerializer.DATASOURCES_DIR_NAME)) newExtension = SolutionSerializer.TABLENODE_FILE_EXTENSION;
					else
					{
						String parentName = resource.getParent().getName();

						if (parentName.equals(SolutionSerializer.FORMS_DIR))
						{
							newExtension = SolutionSerializer.FORM_FILE_EXTENSION;
						}
						else if (parentName.equals(SolutionSerializer.DATASOURCES_DIR_NAME))
						{
							newExtension = SolutionSerializer.TABLENODE_FILE_EXTENSION;
						}
						else if (parentName.equals(SolutionSerializer.VALUELISTS_DIR))
						{
							newExtension = SolutionSerializer.VALUELIST_FILE_EXTENSION;
						}
						else if (parentName.equals(SolutionSerializer.RELATIONS_DIR))
						{
							newExtension = SolutionSerializer.RELATION_FILE_EXTENSION;
						}
					}

					if (newExtension != null)
					{
						// change the extension
						String newName = resource.getName();
						newName = newName.substring(0, newName.length() - SolutionSerializer.JSON_FILE_EXTENSION_SIZE) + newExtension;
						IPath newResourcePath = resource.getFullPath().removeLastSegments(1).append(newName);
						resource.move(newResourcePath, true, null);
					}

					return false;
				}

				return true;
			}

		}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);

		project.refreshLocal(IResource.DEPTH_INFINITE, null);

		// check if we still have *.obj files
		IResource formsDir = project.findMember(SolutionSerializer.FORMS_DIR);
		IResource datasourcesDir = project.findMember(SolutionSerializer.DATASOURCES_DIR_NAME);
		IResource valuelistDir = project.findMember(SolutionSerializer.VALUELISTS_DIR);
		IResource relationDir = project.findMember(SolutionSerializer.RELATIONS_DIR);
		if ((formsDir != null && hasResourceWithExtension(formsDir, SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION)) ||
			(datasourcesDir != null && hasResourceWithExtension(datasourcesDir, SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION)) ||
			(valuelistDir != null && hasResourceWithExtension(valuelistDir, SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION)) ||
			(relationDir != null && hasResourceWithExtension(relationDir, SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION)))
		{
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID,
				"Project update error, still obj files under forms/datasources/valuelists/relations"));
		}
		else
		{
			// update solution metadata to latest version
			updatingSolution.getSolutionMetaData().setFileVersion(AbstractRepository.repository_version);
		}


		final ArrayList<String> formsWithElements = new ArrayList<String>();
		final ArrayList<String> tableNodesWithElements = new ArrayList<String>();
		updatingSolution.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				int oType = o.getTypeID();
				if (oType == IRepository.FORMS || oType == IRepository.TABLENODES)
				{
					o.flagChanged();
					boolean hasChild = false;
					Iterator<IPersist> oIte = ((ISupportChilds)o).getAllObjects();
					while (oIte.hasNext())
					{
						hasChild = !(oIte.next() instanceof AbstractScriptProvider); // anything but not script
						if (hasChild) break;
					}
					if (hasChild)
					{
						if (oType == IRepository.FORMS) formsWithElements.add(((Form)o).getName());
						else tableNodesWithElements.add(((TableNode)o).getTableName());
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}

				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});

		final IFileAccess wfa = new WorkspaceFileAccess(ResourcesPlugin.getWorkspace());
		try
		{
			SolutionSerializer.writePersist(updatingSolution, wfa, ServoyModel.getDeveloperRepository(), true, true, true);
			// check if forms and table nodes items were moved
			checkForChildrenItems(wfa, formsDir, SolutionSerializer.FORM_FILE_EXTENSION, formsWithElements);
			checkForChildrenItems(wfa, datasourcesDir, SolutionSerializer.TABLENODE_FILE_EXTENSION, tableNodesWithElements);
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
			throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project update error"));
		}
	}

	private void checkForChildrenItems(final IFileAccess wfa, IResource startDir, final String resourceExtension, final ArrayList<String> parentElements)
		throws CoreException
	{
		if (startDir != null)
		{
			startDir.accept(new IResourceVisitor()
			{
				public boolean visit(IResource resource) throws CoreException
				{
					String resourceName = resource.getName();
					if (resourceName.endsWith(resourceExtension))
					{
						String elementName = resourceName.substring(0, resourceName.indexOf(resourceExtension));
						if (parentElements.indexOf(elementName) != -1) // it must have items
						{
							try
							{
								String resourceContent = wfa.getUTF8Contents(resource.getFullPath().toOSString());
								if (resourceContent.indexOf("items:[") == -1)
								{
									throw new CoreException(new Status(IStatus.ERROR, Activator.PLUGIN_ID, "Project update error, '" + resourceName +
										"' has missing items after project update"));
								}
							}
							catch (IOException ex)
							{
								ServoyLog.logError(ex);
							}
						}
					}
					return true;
				}

			}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);
		}
	}

	private static boolean hasResourceWithExtension(IResource root, String extension) throws CoreException
	{
		final BitSet hasResourceWithExtension = new BitSet(1);
		root.accept(new IResourceVisitor()
		{
			public boolean visit(IResource resource) throws CoreException
			{
				if (resource.getName().endsWith(SolutionSerializer.JSON_DEFAULT_FILE_EXTENSION))
				{
					hasResourceWithExtension.set(0);
					return false;
				}

				return true;
			}
		}, IResource.DEPTH_INFINITE, IContainer.EXCLUDE_DERIVED);

		return hasResourceWithExtension.get(0);
	}


	public static boolean needUpdate(IProject project) throws CoreException
	{
		if (project.hasNature(ServoyProject.NATURE_ID))
		{
			Solution projectSolution = ((ServoyProject)project.getNature(ServoyProject.NATURE_ID)).getSolution();
			if (projectSolution != null)
			{
				SolutionMetaData solutionMetaData = projectSolution.getSolutionMetaData();
				if (solutionMetaData != null)
				{
					int fileVersion = solutionMetaData.getFileVersion();
					return fileVersion < UPDATE_38;
				}
			}
		}

		return false;
	}
}
