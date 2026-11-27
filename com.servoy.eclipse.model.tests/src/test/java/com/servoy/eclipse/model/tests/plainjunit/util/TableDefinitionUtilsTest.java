package com.servoy.eclipse.model.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.eclipse.model.util.TableDefinitionUtils;
import com.servoy.j2db.dataprocessing.IDataSet;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.server.shared.SecurityInfo;
import com.servoy.j2db.util.xmlxport.ITableDefinitionsAndSecurityBasedOnWorkspaceFiles;
import com.servoy.j2db.util.xmlxport.TableDef;


class TableDefinitionUtilsTest
{
	private WorkspaceMarkers workspace;

	@BeforeEach
	void setUp()
	{
		workspace = new WorkspaceMarkers();
		ModelTestAccess.installMockServoyModel();
		ModelTestAccess.installMockApplicationServer();
	}

	@AfterEach
	void tearDown()
	{
		workspace.close();
		ModelTestAccess.reset();
	}

	@Test
	@DisplayName("loadTableDef returns null when the file does not exist")
	void loadMissing() throws Exception
	{
		IFile file = workspace.file("/p/missing.dbi");
		org.mockito.Mockito.when(file.exists()).thenReturn(Boolean.FALSE);
		assertNull(TableDefinitionUtils.loadTableDef(file));
	}

	@Test
	@DisplayName("loadTableDef deserializes an existing dbi file")
	void loadExisting() throws Exception
	{
		IFile file = workspace.file("/p/t.dbi");
		workspace.withFile("/p/t.dbi", "{\n    \"columns\":[\n        {\"allowNull\":false,\"autoEnterSubType\":2,\"autoEnterType\":2,\"creationOrderIndex\":-1,\"dataType\":4,\"flags\":1,\"name\":\"mcp_tenant_demo_id\"},\n        {\"allowNull\":false,\"creationOrderIndex\":-1,\"dataType\":12,\"length\":100,\"name\":\"item_name\"}\n    ],\n    \"name\":\"mcp_tenant_demo\",\n    \"tableType\":0\n}");
		TableDef def = TableDefinitionUtils.loadTableDef(file);
		assertNotNull(def);
		assertEquals("mcp_tenant_demo", def.name);
	}

	@Test
	@DisplayName("getServerTableinfo filters by extension and table name, or returns all when exportAll")
	void serverTableinfo() throws Exception
	{
		DataModelManager dmm = mock(DataModelManager.class);
		org.mockito.Mockito.when(com.servoy.eclipse.model.ServoyModelFinder.getServoyModel().getDataModelManager()).thenReturn(dmm);

		IFile a = workspace.file("/res/srv/a.dbi");
		IFile b = workspace.file("/res/srv/b.dbi");
		workspace.withFile("/res/srv/a.dbi", "x");
		workspace.withFile("/res/srv/b.dbi", "x");
		IFolder folder = workspace.folder(workspace.project("res"), "srv", a, b);
		when(dmm.getServerInformationFolder("srv")).thenReturn(folder);

		List<IFile> filtered = TableDefinitionUtils.getServerTableinfo("srv", "dbi", List.of("a"), false);
		assertEquals(1, filtered.size());
		assertEquals("a.dbi", filtered.get(0).getName());

		List<IFile> all = TableDefinitionUtils.getServerTableinfo("srv", "dbi", null, true);
		assertEquals(2, all.size());
	}

	@Test
	@DisplayName("hasDbDownErrorMarkersThatCouldBeIgnoredOnExport is true only when an enabled server is down")
	void dbDownMarkers() throws Exception
	{
		ServoyProject project = mock(ServoyProject.class);
		IProject iproject = workspace.project("sol");
		when(project.getProject()).thenReturn(iproject);
		when(com.servoy.eclipse.model.ServoyModelFinder.getServoyModel().getServoyProject("sol")).thenReturn(project);

		IMarker marker = mock(IMarker.class);
		when(marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_INFO)).thenReturn(IMarker.SEVERITY_ERROR);
		when(marker.getType()).thenReturn(ServoyBuilder.MISSING_SERVER);
		when(marker.getAttribute("missingServer")).thenReturn("srv");
		when(iproject.findMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE)).thenReturn(new IMarker[] { marker });

		IApplicationServerSingleton app = ApplicationServerRegistry.get();
		com.servoy.j2db.persistence.IServerManagerInternal sm = mock(com.servoy.j2db.persistence.IServerManagerInternal.class);
		when(app.getServerManager()).thenReturn(sm);
		IServerInternal server = (IServerInternal) mock(com.servoy.j2db.persistence.IServer.class,
			org.mockito.Mockito.withSettings().extraInterfaces(IServerInternal.class));
		com.servoy.j2db.persistence.ServerConfig cfg = mock(com.servoy.j2db.persistence.ServerConfig.class);
		when(cfg.isEnabled()).thenReturn(Boolean.TRUE);
		when(((IServerInternal) server).getConfig()).thenReturn(cfg);
		when(sm.getServer("srv", false, false)).thenReturn((com.servoy.j2db.persistence.IServer) server);

		assertTrue(TableDefinitionUtils.hasDbDownErrorMarkersThatCouldBeIgnoredOnExport(new String[] { "sol" }));

		// disabled server -> false
		when(cfg.isEnabled()).thenReturn(Boolean.FALSE);
		assertFalse(TableDefinitionUtils.hasDbDownErrorMarkersThatCouldBeIgnoredOnExport(new String[] { "sol" }));
	}

	@Test
	@DisplayName("getTableDefinitionsFromDBI builds table defs and security info by group")
	void tableDefsFromDbi() throws Exception
	{
		// DataModelManager mocks
		DataModelManager dmm = mock(DataModelManager.class);
		when(com.servoy.eclipse.model.ServoyModelFinder.getServoyModel().getDataModelManager()).thenReturn(dmm);

		IFile dbiFile = workspace.file("/res/srv/mcp_tenant_demo.dbi");
		workspace.withFile("/res/srv/mcp_tenant_demo.dbi", "{\n    \"columns\":[\n        {\"allowNull\":false,\"autoEnterSubType\":2,\"autoEnterType\":2,\"creationOrderIndex\":-1,\"dataType\":4,\"flags\":1,\"name\":\"mcp_tenant_demo_id\"},\n        {\"allowNull\":false,\"creationOrderIndex\":-1,\"dataType\":12,\"length\":100,\"name\":\"item_name\"}\n    ],\n    \"name\":\"mcp_tenant_demo\",\n    \"tableType\":0\n}");
		IFolder infoFolder = workspace.folder(workspace.project("res"), "srv", dbiFile);
		when(dmm.getServerInformationFolder("srv")).thenReturn(infoFolder);

		IFile serverDbi = workspace.file("/res/srv_server.dbi");
		when(dmm.getServerDBIFile("srv")).thenReturn(serverDbi);
		when(serverDbi.exists()).thenReturn(Boolean.FALSE);

		IFile securityFile = workspace.file("/res/srv/mcp_tenant_demo.sec");
		workspace.withFile("/res/srv/mcp_tenant_demo.sec", "{\"administrators\":{\"x\":4}}");
		when(dmm.getSecurityFile(anyString(), anyString())).thenReturn(securityFile);

		// Application server user manager with one group
		IApplicationServerSingleton app = ApplicationServerRegistry.get();
		com.servoy.j2db.persistence.IServerManagerInternal sm = mock(com.servoy.j2db.persistence.IServerManagerInternal.class);
		when(app.getServerManager()).thenReturn(sm);
		when(sm.getServerNames(true, true, false, false)).thenReturn(new String[] { "srv" });
		com.servoy.j2db.server.shared.IUserManager um = mock(com.servoy.j2db.server.shared.IUserManager.class);
		when(app.getUserManager()).thenReturn(um);
		IDataSet groups = mock(IDataSet.class);
		when(groups.getRows()).thenReturn(java.util.Collections.singletonList(new Object[] { 1, "administrators" }));
		when(um.getGroups(null)).thenReturn(groups);

		ITableDefinitionsAndSecurityBasedOnWorkspaceFiles result =
			TableDefinitionUtils.getTableDefinitionsFromDBI(null, false, false, true, true).getLeft();
		assertNotNull(result);
		List<SecurityInfo> infos = result.getDatabaseSecurityInfoByGroup("administrators");
		assertNotNull(infos);
		assertFalse(infos.isEmpty());
	}
}
