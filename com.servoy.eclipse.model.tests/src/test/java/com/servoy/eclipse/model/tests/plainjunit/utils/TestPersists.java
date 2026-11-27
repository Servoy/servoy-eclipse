/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2026 Servoy BV

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

package com.servoy.eclipse.model.tests.plainjunit.utils;

import com.servoy.j2db.persistence.AbstractPersistFactory;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.UUID;

/**
 * Creates real persists for plain unit tests. The persist constructors are package-private in servoy_shared (another bundle, so not
 * reachable from this fragment), so this goes through the public {@link AbstractPersistFactory#createObject(ISupportChilds, int, UUID)}
 * of a minimal factory instead of reflection.
 */
public final class TestPersists
{
	private static final AbstractPersistFactory FACTORY = new AbstractPersistFactory()
	{
		@Override
		public void initClone(IPersist clone, IPersist objToClone, boolean flattenOverrides)
		{
		}

		@Override
		protected IPersist createRootObject(UUID rootObjectUUID)
		{
			return null;
		}

		@Override
		protected ContentSpec loadContentSpec()
		{
			return null;
		}
	};

	private TestPersists()
	{
	}

	/**
	 * A persist of the given repository type (IRepository.FORMS, RELATIONS, METHODS, ...) with the given parent (may be null) and uuid.
	 */
	@SuppressWarnings("unchecked")
	public static <T extends IPersist> T create(ISupportChilds parent, int objectTypeId, UUID uuid)
	{
		try
		{
			return (T)FACTORY.createObject(parent, objectTypeId, uuid);
		}
		catch (RepositoryException e)
		{
			throw new IllegalArgumentException("cannot create a persist of type " + objectTypeId, e);
		}
	}

	/** A relation without parent, with the given uuid. */
	public static Relation relation(UUID uuid)
	{
		return create(null, IRepository.RELATIONS, uuid);
	}
}
