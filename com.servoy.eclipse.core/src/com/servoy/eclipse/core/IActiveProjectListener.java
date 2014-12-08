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

import com.servoy.eclipse.model.nature.ServoyProject;


/**
 * @author jcompagner
 *
 */
public interface IActiveProjectListener
{

	/**
	 * Used by activeProjectUpdated(...) to notify that the modules of the active project changed.
	 */
	public static final int MODULES_UPDATED = 0;

	/**
	 * Used by activeProjectUpdated(...) to notify that the resources determined by the active project changed because of some reason different than
	 * activeProject being changed. For example, the referenced resources project changed.
	 */
	public static final int RESOURCES_UPDATED_ON_ACTIVE_PROJECT = 1;

	/**
	 * Used by activeProjectUpdated(...) to notify that the resources determined by the active project changed because the activeProject changed.
	 */
	public static final int RESOURCES_UPDATED_BECAUSE_ACTIVE_PROJECT_CHANGED = 2;

	/**
	 * Used by activeProjectUpdated(...) to notify that styles were added/removed from the attached resources project.
	 */
	public static final int STYLES_ADDED_OR_REMOVED = 3;

	/**
	 * Used by activeProjectUpdated(...) to notify that the column info from the active resource project has changed.
	 */
	public static final int COLUMN_INFO_CHANGED = 4;

	/**
	 * Used by activeProjectUpdated(...) to notify that the security/user/group info from the active resource project/active solution has changed.
	 */
	public static final int SECURITY_INFO_CHANGED = 5;

	/**
	 * Used by activeProjectUpdated(...) to notify that templates were added/removed from the attached resources project.
	 */
	public static final int TEMPLATES_ADDED_OR_REMOVED = 6;

	/**
	 * Used by activeProjectUpdated(...) to notify that scope names were added/removed from one of the active solutions/modules.
	 */
	public static final int SCOPE_NAMES_CHANGED = 7;

	/**
	 * Notifies the change of the active project - another project (or null) becomes active.
	 *
	 * @param activeProject The current active project
	 * @param toProject the project that will become the active project
	 * @return true if the change can happen. false if the current active project should stay the active.
	 */
	boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject);

	/**
	 * Notifies the change of the active project - another project (or null) becomes active.
	 *
	 * @param activeProject the new active project.
	 */
	void activeProjectChanged(ServoyProject activeProject);

	/**
	 * Notifies the fact that the currently active project was updated (it's contents or things related to it changed; things such as modules, styles...)
	 *
	 * @param activeProject the currently active project.
	 * @param updateInfo one of the constants declared in this interface identifying the event type.
	 */
	void activeProjectUpdated(ServoyProject activeProject, int updateInfo);

	/**
	 * Abstract base implementation.
	 *
	 * @author rgansevles
	 *
	 */
	public static abstract class ActiveProjectListener implements IActiveProjectListener
	{

		public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
		{
			return true;
		}

		public void activeProjectChanged(ServoyProject activeProject)
		{
		}

		public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
		{
		}
	}

}
