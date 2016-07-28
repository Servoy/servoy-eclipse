/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.model.ngpackages;

/**
 * Should be implemented by classes interested in knowing when ng package projects (active or not) appear or dissapear (become available or unavailable) in the workspace.
 *
 * @author acostescu
 */
public interface IAvailableNGPackageProjectsListener
{

	/**
	 * Gets called when the list of referenced ng package projects changes (one is added or removed for example).
	 * This only checks project accessibility and project nature to determine if an ngpackage project is available or not.
	 *
	 * @param activePackageProjectsChanged true if the one of the active (referenced by active solution) packages changed availability,
	 * false if only non-referenced package projects changed availability.
	 */
	void ngPackageProjectListChanged(boolean activePackageProjectsChanged); // TODO should this be part of IActiveProjectListener instead?

}
