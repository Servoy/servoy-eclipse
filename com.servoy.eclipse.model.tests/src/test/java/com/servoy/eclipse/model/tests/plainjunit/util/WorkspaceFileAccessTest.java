package com.servoy.eclipse.model.tests.plainjunit.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;

class WorkspaceFileAccessTest
{
	private WorkspaceMarkers workspace;
	private WorkspaceFileAccess access;

	@BeforeEach
	void setUp()
	{
		workspace = new WorkspaceMarkers();
		access = new WorkspaceFileAccess(org.eclipse.core.resources.ResourcesPlugin.getWorkspace());
	}

	@AfterEach
	void tearDown()
	{
		workspace.close();
	}

	@Test
	@DisplayName("setUTF8Contents then getUTF8Contents round-trips content")
	void roundTrip() throws Exception
	{
		access.setUTF8Contents("/p/f.txt", "hello");
		assertEquals("hello", access.getUTF8Contents("/p/f.txt"));
	}

	@Test
	@DisplayName("getContents returns the bytes of a workspace file")
	void bytes() throws Exception
	{
		workspace.withFile("/p/f.bin", "abc");
		assertEquals("abc", new String(access.getContents("/p/f.bin"), StandardCharsets.UTF_8));
	}

	@Test
	@DisplayName("exists is true for a file with content")
	void exists() throws Exception
	{
		workspace.withFile("/p/f.txt", "x");
		assertTrue(access.exists("/p/f.txt"));
	}

	@Test
	@DisplayName("delete removes a file with content")
	void delete() throws Exception
	{
		workspace.withFile("/p/f.txt", "x");
		access.delete("/p/f.txt");
		assertNull(workspace.content("/p/f.txt"));
	}
}