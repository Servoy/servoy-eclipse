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

package com.servoy.eclipse.ui.property;

import org.eclipse.ui.views.properties.IPropertyDescriptor;

import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * RAGTEST doc
 *
 * @author rgansevles
 *
 */
public interface IRAGTEST extends IModelSavePropertySource
{
	IPropertyDescriptor getPropertyDescriptor(Object id);

	Object getPersistPropertyValue(Object id);

	void setPersistPropertyValue(Object id, Object value);

	PersistContext getPersistContext();

	void setPersistContext(PersistContext persistContext);

	boolean createOverrideElementIfNeeded() throws RepositoryException;

	void setReadOnly(boolean readOnly);

	default IPersist getPersist()
	{
		return getPersistContext().getPersist();
	}

	default IPersist getContext()
	{
		return getPersistContext().getContext();
	}

	default IPersist getSaveModel()
	{
		return getPersistContext().getPersist();
	}

}
