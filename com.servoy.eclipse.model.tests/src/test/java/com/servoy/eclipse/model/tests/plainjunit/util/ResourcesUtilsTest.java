package com.servoy.eclipse.model.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.runtime.Path;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.util.ResourcesUtils;
import com.servoy.j2db.util.DataSourceUtils;

class ResourcesUtilsTest
{
	@Test
	@DisplayName("createOrWriteFile creates a new file with its parent containers")
	void createFile() throws Exception
	{
		IFile file = mock(IFile.class);
		IContainer parent = mock(IContainer.class);
		org.eclipse.core.resources.IFolder dummyFolder = mock(org.eclipse.core.resources.IFolder.class);
		when(parent.getFolder(org.mockito.ArgumentMatchers.any(org.eclipse.core.runtime.IPath.class))).thenReturn(dummyFolder);
		when(file.getParent()).thenReturn(parent);
		when(file.exists()).thenReturn(Boolean.FALSE);
		ByteArrayInputStream in = new ByteArrayInputStream("x".getBytes(StandardCharsets.UTF_8));
		ResourcesUtils.createOrWriteFile(file, in, true);
		verify(file).create(in, true, null);
	}

	@Test
	@DisplayName("createOrWriteFileUTF8 overwrites an existing file" )
	void writeFile() throws Exception
	{
		IFile file = mock(IFile.class);
		when(file.exists()).thenReturn(Boolean.TRUE);
		ResourcesUtils.createOrWriteFileUTF8(file, "hello", false);
		verify(file).setContents(org.mockito.ArgumentMatchers.any(java.io.InputStream.class), org.mockito.ArgumentMatchers.eq(false),
			org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.isNull());
	}

	@Test
	@DisplayName("createParentContainers does nothing for null, project or existing containers")
	void parentContainers() throws Exception
	{
		IContainer project = mock(IProject.class);
		IContainer existing = mock(IContainer.class);
		when(existing.exists()).thenReturn(Boolean.TRUE);
		ResourcesUtils.createParentContainers(null, true);
		ResourcesUtils.createParentContainers(project, true);
		ResourcesUtils.createParentContainers(existing, true);
		verify(existing, org.mockito.Mockito.never()).getParent();
	}

	@Test
	@DisplayName("getParentDatasource resolves a dbi file to a db:/ datasource")
	void dbiDatasource()
	{
		IFile file = mock(IFile.class);
		when(file.getProjectRelativePath()).thenReturn(new Path("datasources/srv/tbl.dbi"));
		assertEquals(DataSourceUtils.createDBTableDataSource("srv", "tbl"), ResourcesUtils.getParentDatasource(file, false));
	}

	@Test
	@DisplayName("getParentDatasource resolves an inmem and a view datasource")
	void specialDatasources()
	{
		IFile inmem = mock(IFile.class);
		when(inmem.getProjectRelativePath()).thenReturn(new Path("datasources/mem/table.dbi"));
		assertEquals(DataSourceUtils.createInmemDataSource("table"), ResourcesUtils.getParentDatasource(inmem, false));
		IFile view = mock(IFile.class);
		when(view.getProjectRelativePath()).thenReturn(new Path("datasources/view/table.dbi"));
		assertEquals(DataSourceUtils.createViewDataSource("table"), ResourcesUtils.getParentDatasource(view, false));
	}

	@Test
	@DisplayName("getParentDatasource returns null for an unrelated file")
	void noDatasource()
	{
		IFile file = mock(IFile.class);
		when(file.getProjectRelativePath()).thenReturn(new Path("forms/f1.frm"));
		assertNull(ResourcesUtils.getParentDatasource(file, false));
	}
}