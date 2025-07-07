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

package com.servoy.eclipse.model.extensions;

import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;

import com.servoy.eclipse.model.IFormComponentListener;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.AtomicIntegerWithListener;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IServerInternal;

/**
 * Aware of active solution/resources projects and other things is the current environment needed by the com.servoy.eclipse.model.repository classes.<br>
 * Similar in Servoy terms to IJavaModel in java environment terms.
 * @author acostescu
 */
public interface IServoyModel
{
	AbstractServoyModel refreshServoyProjects();

	ServoyProject getServoyProject(String name);

	ServoyProject getActiveProject();

	/**
	 * Returns the active resources project. This is the only resources project referenced by the current active project. Will return null if the active project
	 * is null or if the number of resources projects referenced by the active project != 1.
	 *
	 * @return the active resources project.
	 */
	ServoyResourcesProject getActiveResourcesProject();

	/**
	 * Returns an array containing the modules of the active project (including the active project). If there is no active project, will return an array of size
	 * 0.<br>
	 *
	 * The result does also include the active solution's import hooks (which are not part of the flattened solution).
	 *
	 * @return an array containing the modules of the active project.
	 */
	ServoyProject[] getModulesOfActiveProject();

	/**
	 * Returns a treeset containing the sorted modules of the active project (including the active project). If there is no active project, will return an array of size
	 * 0.<br>
	 *
	 * The result does also include the active solution's import hooks (which are not part of the flattened solution).
	 *
	 * @param boolean needSortedModules - flag in order to retrieve sorted elements
	 * @return an array containing the modules of the active project.
	 */
	ServoyProject[] getModulesOfActiveProject(boolean needSortedModules);

	/**
	 * Returns the flattened solution for the active project.
	 * @return the flattened solution for the active project.
	 */
	FlattenedSolution getFlattenedSolution();

	DataModelManager getDataModelManager();

	BaseNGPackageManager getNGPackageManager();

	boolean isSolutionActive(String solutionName);

	/**
	 * Checks whether or not the solution with given name is or should be a module of the active solution.<br>
	 * It checks modules listed in all current modules of flattened solution; it is able to detect modules that are not part of the actual flattened solution yet, without actually loading them (so for example solutions that the active solution or it's modules listed as a module but was not valid/present previously).
	 */
	boolean shouldBeModuleOfActiveSolution(String searchForName);

	void reportSaveError(Exception ex);

	public EclipseMessages getMessagesManager();

	/**
	 * Handler for collecting changes, delay picking up of changes until handler goes to zero.
	 */
	AtomicIntegerWithListener getResourceChangesHandlerCounter();

	/**
	 * The datasource manger handles all the different kind of datasources a solution can have.
	 *
	 */
	IDataSourceManager getDataSourceManager();

	/**
	 * @param tablename
	 * @return
	 */
	IServerInternal getMemServer(String tablename);

	/**
	 * Fire that a form component is added, deleted or its name/datasource updated.
	 */
	void fireFormComponentChanged();

	void addFormComponentListener(IFormComponentListener listener);

	void removeFormComponentListener(IFormComponentListener listener);

	/**
	 * Equivalent to addResourceChangeListener(resourceChangeListener, IResourceChangeEvent.PRE_CLOSE | IResourceChangeEvent.PRE_DELETE | IResourceChangeEvent.POST_CHANGE).
	 *
	 * @see #addResourceChangeListener(IResourceChangeListener, int)
	 */
	public void addResourceChangeListener(IResourceChangeListener resourceChangeListener);

	/**
	 * Adds a (proxy) listener to eclipse workspace resources changes (POST_CHANGE if requested will only get triggered once ServoyModel has had a chance to handle these changes; all other types of events will be registered normally to the workspace).<br/>
	 * This is useful if these registered listeners that are POST_CHANGE depend on things being already updated in ServoyModel (for example that all ServoyProject instances are the new ones in case the .project files have changed).
	 *
	 * @param resourceChangeListener the listener that will get triggered only after ServoyModel has already had a chance to handle the POST_CHANGE resource changes if it is a POST_CHANGE; other types of events are registered directly to the workspace.
	 * @param eventMask the bit-wise OR of all event types of interest to the listener; Use IResourceChangeEvent constants.
	 *
	 * @see IWorkspace#addResourceChangeListener(IResourceChangeListener, int) for more information on possible eventMasks and what they do.
	 */
	void addResourceChangeListener(IResourceChangeListener resourceChangeListener, int eventMask);

	void removeResourceChangeListener(IResourceChangeListener resourceChangeListener);
}