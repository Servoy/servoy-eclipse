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

package com.servoy.eclipse.model.tests.plainjunit;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

/**
 * The static lookups the model bundle uses (ServoyModelFinder, ApplicationServerRegistry) can be replaced by mocks in a plain unit test,
 * and are removed again by {@link ModelTestAccess#reset()} so tests do not leak into each other.
 */
class ModelTestAccessTest {
	@AfterEach
	void tearDown() {
		ModelTestAccess.reset();
	}

	@Test
	void theInstalledModelAndServiceProviderAreReturnedByTheFinder() {
		IServoyModel model = mock(IServoyModel.class);
		IServiceProvider serviceProvider = mock(IServiceProvider.class);

		ModelTestAccess.installServoyModel(model, serviceProvider);

		assertSame(model, ServoyModelFinder.getServoyModel());
		assertSame(serviceProvider, ServoyModelFinder.getServiceProvider());
		assertSame(model, ServoyModelFinder.getServoyModelProvider().getServoyModel());
	}

	@Test
	void aMockModelCanBeInstalledInOneCall() {
		IServoyModel model = ModelTestAccess.installMockServoyModel();

		assertSame(model, ServoyModelFinder.getServoyModel());
	}

	@Test
	void theInstalledApplicationServerIsReturnedByTheRegistryAndResetRemovesIt() {
		IApplicationServerSingleton server = ModelTestAccess.installMockApplicationServer();

		assertTrue(ApplicationServerRegistry.exists());
		assertSame(server, ApplicationServerRegistry.get());

		ModelTestAccess.reset();

		assertFalse(ApplicationServerRegistry.exists());
	}
}
