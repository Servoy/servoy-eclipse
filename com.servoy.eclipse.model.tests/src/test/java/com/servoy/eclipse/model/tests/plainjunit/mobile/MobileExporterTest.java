package com.servoy.eclipse.model.tests.plainjunit.mobile;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.mobile.exporter.MobileExporter;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;

class MobileExporterTest
{
	private IApplicationServerSingleton app;

	@BeforeEach
	void setUp()
	{
		app = ModelTestAccess.installMockApplicationServer();
		com.servoy.j2db.persistence.IServerManagerInternal sm = org.mockito.Mockito.mock(com.servoy.j2db.persistence.IServerManagerInternal.class);
		when(app.getServerManager()).thenReturn(sm);
		when(app.getWebServerPort()).thenReturn(8080);
	}

	@AfterEach
	void tearDown()
	{
		ModelTestAccess.reset();
	}

	@Test
	@DisplayName("getDefaultServerURL uses the application server's web port")
	void defaultUrl()
	{
		assertEquals("http://localhost:8080", MobileExporter.getDefaultServerURL());
	}
}
