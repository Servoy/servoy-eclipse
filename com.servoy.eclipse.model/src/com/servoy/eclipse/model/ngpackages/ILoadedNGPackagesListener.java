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
 * Should be implemented by classes interested in knowing when active (loaded) ng packages change.<br/>
 * The packages can be package projects referenced by active solution/modules + any binary web package zips from active solution/modules. (or legacy locations in resources project)

 * @author jcompagner
 */
public interface ILoadedNGPackagesListener
{

	// TODO here we could provide exactly what packages were changed/added/deleted and maybe the types of packages affected - it could be useful in a few places
	// and I think we have that info
	/**
	 * Called when packages were loaded or unloaded from active modules and their references.
	 * If loaded modules remain the same but for example a package is now loaded by another module, it will still get called but with loadedPackagesAreTheSameAlthoughReferencingModulesChanged == true.
	 *
	 * @param loadedPackagesAreTheSameAlthoughReferencingModulesChanged see method description.
	 */
	void ngPackagesChanged(boolean loadedPackagesAreTheSameAlthoughReferencingModulesChanged);

}
