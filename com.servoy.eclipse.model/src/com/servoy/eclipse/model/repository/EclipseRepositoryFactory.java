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
package com.servoy.eclipse.model.repository;

import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRemoteRepository;
import com.servoy.j2db.persistence.IRepositoryFactory;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.util.Settings;

/**
 * @author jcompagner
 * 
 */
public class EclipseRepositoryFactory implements IRepositoryFactory
{
	protected EclipseRepository repository = null;

	protected void init(IServerManagerInternal serverManager, Settings settings)
	{
		if (repository == null)
		{
			repository = new EclipseRepository(serverManager, settings);
		}
	}

	/**
	 * @see com.servoy.j2db.server.IRepositoryFactory#getDeveloperRepository(com.servoy.j2db.persistence.IServerManagerInternal, com.servoy.j2db.util.Settings)
	 */
	public IDeveloperRepository getDeveloperRepository(IServerManagerInternal serverManager, Settings settings)
	{
		init(serverManager, settings);
		return repository;
	}

	/**
	 * @see com.servoy.j2db.server.IRepositoryFactory#getRemoteRepository(com.servoy.j2db.persistence.IServerManagerInternal, com.servoy.j2db.util.Settings)
	 */
	public IRemoteRepository getRemoteRepository(IServerManagerInternal serverManager, Settings settings)
	{
		init(serverManager, settings);
		return repository;
	}
}