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

import org.eclipse.core.resources.IResourceChangeListener;

import com.servoy.eclipse.core.repository.EclipseUserManager;
import com.servoy.eclipse.model.IFormComponentListener;
import com.servoy.eclipse.model.extensions.AbstractServoyModel;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.ngpackages.BaseNGPackageManager;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.util.AtomicIntegerWithListener;
import com.servoy.eclipse.model.util.IWorkingSetChangedListener;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.IActiveSolutionHandler;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistChangeListener;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * @author jcompagner
 *
 */
public class DelegatingServoyModel implements IDeveloperServoyModel
{
	private final ServoyModel realModel;

	/**
	 * @param servoyModel
	 */
	public DelegatingServoyModel(ServoyModel servoyModel)
	{
		realModel = servoyModel;
	}

	/**
	 * @param name
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getServoyProject(java.lang.String)
	 */
	public ServoyProject getServoyProject(String name)
	{
		return realModel.getServoyProject(name);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getActiveProject()
	 */
	public ServoyProject getActiveProject()
	{
		if (!realModel.isFlattenedSolutionLoaded()) return null;
		return realModel.getActiveProject();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getNameValidator()
	 */
	public IValidateName getNameValidator()
	{
		return realModel.getNameValidator();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getActiveResourcesProject()
	 */
	public ServoyResourcesProject getActiveResourcesProject()
	{
		return realModel.getActiveResourcesProject();
	}

	/**
	 *
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#dispose()
	 */
	public void dispose()
	{
		realModel.dispose();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#refreshServoyProjects()
	 */
	public AbstractServoyModel refreshServoyProjects()
	{
		return realModel.refreshServoyProjects();
	}

	/**
	 *
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#buildActiveProjectsInJob()
	 */
	public void buildActiveProjectsInJob()
	{
		realModel.buildActiveProjectsInJob();
	}

	/**
	 * @param type
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getActiveRootObjects(int)
	 */
	public List<IRootObject> getActiveRootObjects(int type)
	{
		return realModel.getActiveRootObjects(type);
	}

	/**
	 * @param realSolution
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#addPersistChangeListener(boolean, com.servoy.j2db.persistence.IPersistChangeListener)
	 */
	public void addPersistChangeListener(boolean realSolution, IPersistChangeListener listener)
	{
		realModel.addPersistChangeListener(realSolution, listener);
	}

	/**
	 * @param realSolution
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#removePersistChangeListener(boolean, com.servoy.j2db.persistence.IPersistChangeListener)
	 */
	public void removePersistChangeListener(boolean realSolution, IPersistChangeListener listener)
	{
		realModel.removePersistChangeListener(realSolution, listener);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getModulesOfActiveProject()
	 */
	public ServoyProject[] getModulesOfActiveProject()
	{
		return getModulesOfActiveProject(false);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getResourceProjects()
	 */
	public ServoyResourcesProject[] getResourceProjects()
	{
		return realModel.getResourceProjects();
	}

	/**
	 * @param persist
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getEditingFlattenedSolution(com.servoy.j2db.persistence.IPersist)
	 */
	public FlattenedSolution getEditingFlattenedSolution(IPersist persist)
	{
		return realModel.getEditingFlattenedSolution(persist);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getUserManager()
	 */
	public EclipseUserManager getUserManager()
	{
		return realModel.getUserManager();
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#addActiveProjectListener(com.servoy.eclipse.core.IActiveProjectListener)
	 */
	public void addActiveProjectListener(IActiveProjectListener listener)
	{
		realModel.addActiveProjectListener(listener);
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#removeActiveProjectListener(com.servoy.eclipse.core.IActiveProjectListener)
	 */
	public void removeActiveProjectListener(IActiveProjectListener listener)
	{
		realModel.removeActiveProjectListener(listener);
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#addI18NChangeListener(com.servoy.eclipse.core.I18NChangeListener)
	 */
	public void addI18NChangeListener(I18NChangeListener listener)
	{
		realModel.addI18NChangeListener(listener);
	}

	/**
	 * @param needSortedModules
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getModulesOfActiveProject(boolean)
	 */
	public ServoyProject[] getModulesOfActiveProject(boolean needSortedModules)
	{
		if (!realModel.isFlattenedSolutionLoaded()) return new ServoyProject[0];
		return realModel.getModulesOfActiveProject(needSortedModules);
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#removeI18NChangeListener(com.servoy.eclipse.core.I18NChangeListener)
	 */
	public void removeI18NChangeListener(I18NChangeListener listener)
	{
		realModel.removeI18NChangeListener(listener);
	}

	/**
	 * @param project
	 * @param buildProject
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#setActiveProject(com.servoy.eclipse.model.nature.ServoyProject, boolean)
	 */
	public void setActiveProject(ServoyProject project, boolean buildProject)
	{
		realModel.setActiveProject(project, buildProject);
	}

	/**
	 *
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#flushAllCachedData()
	 */
	public void flushAllCachedData()
	{
		realModel.flushAllCachedData();
	}

	/**
	 * @param table
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#flushDataProvidersForTable(com.servoy.j2db.persistence.ITable)
	 */
	public void flushDataProvidersForTable(ITable table)
	{
		realModel.flushDataProvidersForTable(table);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getActiveSolutionHandler()
	 */
	public IActiveSolutionHandler getActiveSolutionHandler()
	{
		return realModel.getActiveSolutionHandler();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#isActiveSolutionWeb()
	 */
	public boolean isActiveSolutionWeb()
	{
		return realModel.isActiveSolutionWeb();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#isActiveSolutionMobile()
	 */
	public boolean isActiveSolutionMobile()
	{
		return realModel.isActiveSolutionMobile();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#isActiveSolutionNGClient()
	 */
	public boolean isActiveSolutionNGClient()
	{
		return realModel.isActiveSolutionNGClient();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#isActiveSolutionSmartClient()
	 */
	public boolean isActiveSolutionSmartClient()
	{
		return realModel.isActiveSolutionSmartClient();
	}

	/**
	 * @param realSolution
	 * @param obj
	 * @param recursive
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#firePersistChanged(boolean, java.lang.Object, boolean)
	 */
	public void firePersistChanged(boolean realSolution, Object obj, boolean recursive)
	{
		realModel.firePersistChanged(realSolution, obj, recursive);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getFlattenedSolution()
	 */
	public FlattenedSolution getFlattenedSolution()
	{
		return realModel.getFlattenedSolution();
	}

	/**
	 * @param realSolution
	 * @param changes
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#firePersistsChanged(boolean, java.util.Collection)
	 */
	public void firePersistsChanged(boolean realSolution, Collection<IPersist> changes)
	{
		realModel.firePersistsChanged(realSolution, changes);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getActiveSolutionClientType()
	 */
	public ClientSupport getActiveSolutionClientType()
	{
		return realModel.getActiveSolutionClientType();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getDataModelManager()
	 */
	public DataModelManager getDataModelManager()
	{
		return realModel.getDataModelManager();
	}

	/**
	 * @param name
	 * @param objectTypeId
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getActiveRootObject(java.lang.String, int)
	 */
	public IRootObject getActiveRootObject(String name, int objectTypeId)
	{
		return realModel.getActiveRootObject(name, objectTypeId);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getNGPackageManager()
	 */
	public BaseNGPackageManager getNGPackageManager()
	{
		return realModel.getNGPackageManager();
	}

	/**
	 * @param solutionName
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#isSolutionActive(java.lang.String)
	 */
	public boolean isSolutionActive(String solutionName)
	{
		return realModel.isSolutionActive(solutionName);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getServoyProjects()
	 */
	public ServoyProject[] getServoyProjects()
	{
		return realModel.getServoyProjects();
	}

	/**
	 * @param searchForName
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#shouldBeModuleOfActiveSolution(java.lang.String)
	 */
	public boolean shouldBeModuleOfActiveSolution(String searchForName)
	{
		return realModel.shouldBeModuleOfActiveSolution(searchForName);
	}

	/**
	 * @param sp
	 * @param persist
	 * @throws RepositoryException
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#revertEditingPersist(com.servoy.eclipse.model.nature.ServoyProject, com.servoy.j2db.persistence.IPersist)
	 */
	public void revertEditingPersist(ServoyProject sp, IPersist persist) throws RepositoryException
	{
		realModel.revertEditingPersist(sp, persist);
	}

	/**
	 * @param servoyProject
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#isProjectActive(com.servoy.eclipse.model.nature.ServoyProject)
	 */
	public boolean isProjectActive(ServoyProject servoyProject)
	{
		// as long as the fs is not loaded all projects are for now active, else we will close editors that shouldn't be closed.
		if (!realModel.isFlattenedSolutionLoaded())
		{
			return true;
		}
		return realModel.isProjectActive(servoyProject);
	}

	/**
	 * @param workingSetChangedListener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#addWorkingSetChangedListener(com.servoy.eclipse.model.util.IWorkingSetChangedListener)
	 */
	public void addWorkingSetChangedListener(IWorkingSetChangedListener workingSetChangedListener)
	{
		realModel.addWorkingSetChangedListener(workingSetChangedListener);
	}

	/**
	 * @param workingSetChangedListener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#removeWorkingSetChangedListener(com.servoy.eclipse.model.util.IWorkingSetChangedListener)
	 */
	public void removeWorkingSetChangedListener(IWorkingSetChangedListener workingSetChangedListener)
	{
		realModel.removeWorkingSetChangedListener(workingSetChangedListener);
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#addSolutionMetaDataChangeListener(com.servoy.eclipse.core.ISolutionMetaDataChangeListener)
	 */
	public void addSolutionMetaDataChangeListener(ISolutionMetaDataChangeListener listener)
	{
		realModel.addSolutionMetaDataChangeListener(listener);
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#removeSolutionMetaDataChangeListener(com.servoy.eclipse.core.ISolutionMetaDataChangeListener)
	 */
	public void removeSolutionMetaDataChangeListener(ISolutionMetaDataChangeListener listener)
	{
		realModel.removeSolutionMetaDataChangeListener(listener);
	}

	/**
	 * @param ex
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#reportSaveError(java.lang.Exception)
	 */
	public void reportSaveError(Exception ex)
	{
		realModel.reportSaveError(ex);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getMessagesManager()
	 */
	public EclipseMessages getMessagesManager()
	{
		return realModel.getMessagesManager();
	}

	/**
	 * @param l
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#addSolutionImportInProgressListener(com.servoy.eclipse.core.ISolutionImportListener)
	 */
	public void addSolutionImportInProgressListener(ISolutionImportListener l)
	{
		realModel.addSolutionImportInProgressListener(l);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getResourceChangesHandlerCounter()
	 */
	public AtomicIntegerWithListener getResourceChangesHandlerCounter()
	{
		return realModel.getResourceChangesHandlerCounter();
	}

	/**
	 * @param l
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#removeSolutionImportInProgressListener(com.servoy.eclipse.core.ISolutionImportListener)
	 */
	public void removeSolutionImportInProgressListener(ISolutionImportListener l)
	{
		realModel.removeSolutionImportInProgressListener(l);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#isSolutionImportInProgress()
	 */
	public boolean isSolutionImportInProgress()
	{
		return realModel.isSolutionImportInProgress();
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getDataSourceManager()
	 */
	public IDataSourceManager getDataSourceManager()
	{
		return realModel.getDataSourceManager();
	}

	/**
	 * @param realSolution
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#startCollectingPersistChanges(boolean)
	 */
	public void startCollectingPersistChanges(boolean realSolution)
	{
		realModel.startCollectingPersistChanges(realSolution);
	}

	/**
	 * @param realSolution
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#stopCollectingPersistChanges(boolean)
	 */
	public void stopCollectingPersistChanges(boolean realSolution)
	{
		realModel.stopCollectingPersistChanges(realSolution);
	}

	/**
	 * @return
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#getTeamShareMonitor()
	 */
	public TeamShareMonitor getTeamShareMonitor()
	{
		return realModel.getTeamShareMonitor();
	}

	/**
	 * @param tablename
	 * @return
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#getMemServer(java.lang.String)
	 */
	public IServerInternal getMemServer(String tablename)
	{
		return realModel.getMemServer(tablename);
	}

	/**
	 * @param updateInfo
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#fireActiveProjectUpdated(int)
	 */
	public void fireActiveProjectUpdated(int updateInfo)
	{
		realModel.fireActiveProjectUpdated(updateInfo);
	}

	/**
	 * @param solutionImportInProgress
	 * @see com.servoy.eclipse.core.IDeveloperServoyModel#setSolutionImportInProgressFlag(boolean)
	 */
	public void setSolutionImportInProgressFlag(boolean solutionImportInProgress)
	{
		realModel.setSolutionImportInProgressFlag(solutionImportInProgress);
	}

	/**
	 *
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#fireFormComponentChanged()
	 */
	public void fireFormComponentChanged()
	{
		realModel.fireFormComponentChanged();
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#addFormComponentListener(com.servoy.eclipse.model.IFormComponentListener)
	 */
	public void addFormComponentListener(IFormComponentListener listener)
	{
		realModel.addFormComponentListener(listener);
	}

	/**
	 * @param listener
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#removeFormComponentListener(com.servoy.eclipse.model.IFormComponentListener)
	 */
	public void removeFormComponentListener(IFormComponentListener listener)
	{
		realModel.removeFormComponentListener(listener);
	}

	/**
	 * @param resourceChangeListener
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#addResourceChangeListener(org.eclipse.core.resources.IResourceChangeListener)
	 */
	public void addResourceChangeListener(IResourceChangeListener resourceChangeListener)
	{
		realModel.addResourceChangeListener(resourceChangeListener);
	}

	/**
	 * @param resourceChangeListener
	 * @param eventMask
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#addResourceChangeListener(org.eclipse.core.resources.IResourceChangeListener, int)
	 */
	public void addResourceChangeListener(IResourceChangeListener resourceChangeListener, int eventMask)
	{
		realModel.addResourceChangeListener(resourceChangeListener, eventMask);
	}

	/**
	 * @param resourceChangeListener
	 * @see com.servoy.eclipse.model.extensions.IServoyModel#removeResourceChangeListener(org.eclipse.core.resources.IResourceChangeListener)
	 */
	public void removeResourceChangeListener(IResourceChangeListener resourceChangeListener)
	{
		realModel.removeResourceChangeListener(resourceChangeListener);
	}
}
