/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2024 Servoy BV

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

package com.servoy.eclipse.model.war.exporter;

import java.util.Set;

/**
 * @author jcompagner
 *
 */
public interface ITiNGExportModel
{

	/**
	 * Return the string what to npm script should be build.
	 *
	 * @return
	 */
	String exportNG2Mode();

	public Set<String> getExportedPackagesExceptSablo();

	/**
	 * Gets all components that are to be exported. This includes under-the-hood components, components that are explicitly used by solution and any
	 * optional components that the user picked during export.
	 */
	Set<String> getAllExportedComponents();

	/**
	 * Gets almost all services that are to be exported. This includes under-the-hood services, services that are explicitly used by solution and any
	 * optional services that the user picked during export.<br/><br/>
	 *
	 * This currently won't include any sablo content as all needed sablo js files are referenced statically from the page (pointing to jar file inside war) when serving ng clients.<br/>
	 * And here we want only the contents that will be wro grouped/optimized to be included.<br/><br/>
	 *
	 * {@link #getComponentsNeededUnderTheHood()} would be the same if sablo would have components, but it does not have any so sablo does not matter there...
	 */
	Set<String> getAllExportedServicesWithoutSabloServices();
}
