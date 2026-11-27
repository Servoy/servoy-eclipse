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

package com.servoy.eclipse.model;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.servoy.eclipse.model.extensions.IServoyEnvironmentProvider;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

/**
 * Test-side access to the static lookups of the model bundle, for plain JUnit tests that run without an OSGi runtime.
 * <p>
 * This test bundle is a fragment of com.servoy.eclipse.model, so a class in the same package sees the package-private
 * {@link ServoyModelFinder#setServoyModelProvider(IServoyEnvironmentProvider)}. Tests in other packages use this helper instead of
 * reflection. Always pair an install with {@link #reset()} (e.g. in {@code @AfterEach}): both lookups are static and shared by all
 * tests in the JVM.
 * </p>
 */
public final class ModelTestAccess
{
	private ModelTestAccess()
	{
	}

	/**
	 * Makes {@link ServoyModelFinder#getServoyModel()} return the given model (and {@link ServoyModelFinder#getServiceProvider()} the
	 * given service provider, may be null).
	 */
	public static void installServoyModel(IServoyModel model, IServiceProvider serviceProvider)
	{
		IServoyEnvironmentProvider provider = mock(IServoyEnvironmentProvider.class);
		when(provider.getServoyModel()).thenReturn(model);
		when(provider.getServiceProvider()).thenReturn(serviceProvider);
		ServoyModelFinder.setServoyModelProvider(provider);
	}

	/**
	 * Installs a Mockito mock as the model returned by {@link ServoyModelFinder#getServoyModel()} and returns it for stubbing.
	 */
	public static IServoyModel installMockServoyModel()
	{
		IServoyModel model = mock(IServoyModel.class);
		installServoyModel(model, null);
		return model;
	}

	/**
	 * Installs a Mockito mock as the application server returned by {@link ApplicationServerRegistry#get()} and returns it for stubbing.
	 */
	public static IApplicationServerSingleton installMockApplicationServer()
	{
		IApplicationServerSingleton server = mock(IApplicationServerSingleton.class);
		ApplicationServerRegistry.destroy();
		ApplicationServerRegistry.setApplicationServerSingleton(server);
		return server;
	}

	/**
	 * Adds the persist to the index as the visit in createIndex() does (DeveloperPersistIndex#putInCache is protected; this class is in
	 * its package). itemCreated() is no alternative: it ignores items while the uuid cache is still empty.
	 */
	public static void putInCache(DeveloperPersistIndex index, IPersist persist)
	{
		index.putInCache(persist);
	}

	/**
	 * Removes what the install methods put in place.
	 */
	public static void reset()
	{
		ServoyModelFinder.setServoyModelProvider(null);
		ApplicationServerRegistry.destroy();
	}
}
