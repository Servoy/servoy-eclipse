/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

import java.util.Collection;
import java.util.List;

import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.util.IWorkingSetChangedListener;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * @author jcompagner
 *
 */
public interface IDeveloperServoyModel extends IServoyModel
{
	public IValidateName getNameValidator();

	public void dispose();

	public void buildActiveProjectsInJob();

	public List<IRootObject> getActiveRootObjects(int type);

	public void addPersistChangeListener(boolean realSolution, IPersistChangeListener listener);

	public void removePersistChangeListener(boolean realSolution, IPersistChangeListener listener);

	public ServoyResourcesProject[] getResourceProjects();

	public FlattenedSolution getEditingFlattenedSolution(IPersist persist);

	public EclipseUserManager getUserManager();

	public void addActiveProjectListener(IActiveProjectListener listener);

	public void removeActiveProjectListener(IActiveProjectListener listener);

	/**
	 * Will call the listener when the model is done loading, will call it right away when it is already loaded.
	 * Will remove the listener itself after it is called.
	 * @param listener
	 */
	public void addDoneListener(IModelDoneListener listener);

	public void addI18NChangeListener(I18NChangeListener listener);

	public void removeI18NChangeListener(I18NChangeListener listener);

	public void setActiveProject(final ServoyProject project, final boolean buildProject);

	public void flushAllCachedData();

	public void flushDataProvidersForTable(ITable table);

	public IActiveSolutionHandler getActiveSolutionHandler();

	public boolean isActiveSolutionWeb();

	public boolean isActiveSolutionMobile();

	public boolean isActiveSolutionNGClient();

	public boolean isActiveSolutionSmartClient();

	public void firePersistChanged(boolean realSolution, Object obj, boolean recursive);

	public void firePersistsChanged(boolean realSolution, Collection<IPersist> changes);

	public ClientSupport getActiveSolutionClientType();

	public IRootObject getActiveRootObject(String name, int objectTypeId);

	public ServoyProject[] getServoyProjects();

	public void revertEditingPersist(ServoyProject sp, IPersist persist) throws RepositoryException;

	public boolean isProjectActive(ServoyProject servoyProject);

	public void addWorkingSetChangedListener(IWorkingSetChangedListener workingSetChangedListener);

	public void removeWorkingSetChangedListener(IWorkingSetChangedListener workingSetChangedListener);

	public void addSolutionMetaDataChangeListener(ISolutionMetaDataChangeListener listener);

	public void removeSolutionMetaDataChangeListener(ISolutionMetaDataChangeListener listener);

	public void addSolutionImportInProgressListener(ISolutionImportListener l);

	public void removeSolutionImportInProgressListener(ISolutionImportListener l);

	public boolean isSolutionImportInProgress();

	public void startCollectingPersistChanges(boolean realSolution);

	public void stopCollectingPersistChanges(boolean realSolution);

	public TeamShareMonitor getTeamShareMonitor();

	public void fireActiveProjectUpdated(int updateInfo);

	public void setSolutionImportInProgressFlag(boolean solutionImportInProgress);

}
