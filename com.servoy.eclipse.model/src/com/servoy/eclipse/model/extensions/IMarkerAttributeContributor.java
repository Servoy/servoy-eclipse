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

import org.eclipse.core.resources.IMarker;

import com.servoy.eclipse.model.Activator;
import com.servoy.j2db.persistence.IPersist;

/**
 * Extensions that implement this want to contribute attributes to markers created by Servoy builder.
 * @author acostescu
 */
public interface IMarkerAttributeContributor
{

	static final String EXTENSION_ID = Activator.PLUGIN_ID + ".builder.attributeContributor"; //$NON-NLS-1$

	/**
	 * Contrbutes attributes to given marker that is associated with the given persist.
	 * @param marker marker that is added by Servoy builder.
	 * @param persist the persist this marker is associated with.
	 */
	void contributeToMarker(IMarker marker, IPersist persist);

}
