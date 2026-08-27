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
package com.servoy.eclipse.mcp;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.sql.Connection;
import java.sql.Statement;
import java.util.function.BooleanSupplier;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.osgi.framework.Bundle;
import org.osgi.framework.FrameworkUtil;
// the bundle is located through OSGi, so these two come from the framework rather than from Eclipse

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Puts the sample solution into the running developer's workspace, and the data its tools read into
 * the database.
 *
 * <p>The solution is not built here from an API, it is copied: <code>testresources/workspace</code>
 * holds the same <code>mcp_sample</code> that the server is exercised against by hand, so what the
 * tests run against and what a person tries in the IDE cannot drift apart.</p>
 *
 * <p>Everything is done once and left in place. Each test then reads through the servlet, which is
 * cheap, and none of them writes, so there is nothing to undo between them.</p>
 *
 * @author Servoy
 */
public final class McpTestWorkspace
{
	public static final String SOLUTION = "mcp_sample";
	public static final String AUTHENTICATOR = "mcp_sample_auth";
	public static final String RESOURCES = "resources";

	/** The database server the sample solution reads from. */
	public static final String SERVER = "example_data";

	/** The tokens the sample authenticator module knows. They are demo values, not credentials. */
	public static final String ALICE = "mcp-demo-token-alice";
	public static final String BOB = "mcp-demo-token-bob";

	/**
	 * How long to wait for the application server to come up before giving up on it.
	 *
	 * <p>Generous on purpose. A developer starts in seconds on a fast machine and in minutes on a
	 * loaded build agent, and a test that fails because the agent was slow teaches nobody anything.
	 * Waiting longer costs nothing when things are working, since the wait ends as soon as the server
	 * is there.</p>
	 */
	private static final long APP_SERVER_TIMEOUT_MS = 300_000;

	/** How long to keep asking whether the activated solution can be served - see above. */
	private static final long ACTIVATION_TIMEOUT_MS = 300_000;

	/** How often to ask. Short enough not to add noticeable delay, long enough not to spin. */
	private static final long POLL_INTERVAL_MS = 500;

	private static boolean prepared;

	private McpTestWorkspace()
	{
	}

	/**
	 * Makes the workspace ready, once per run.
	 *
	 * @throws Exception when the developer never came up or the workspace could not be built, which
	 *         is a broken environment - the tests that follow would only fail obscurely
	 */
	public static synchronized void prepare() throws Exception
	{
		if (prepared) return;

		waitForApplicationServer();

		copyProject(SOLUTION);
		copyProject(AUTHENTICATOR);
		copyProject(RESOURCES);

		createTenantTable();
		activateSolution();

		prepared = true;
	}

	/**
	 * The port the test developer serves on, for the one test that goes over HTTP.
	 */
	public static int serverPort()
	{
		return ApplicationServerRegistry.get().getWebServerPort();
	}

	private static void waitForApplicationServer() throws Exception
	{
		waitUntil(() -> ApplicationServerRegistry.exists() && ApplicationServerRegistry.get() != null,
			APP_SERVER_TIMEOUT_MS, "the application server to start");
	}

	/**
	 * Copies one project out of testresources and into the workspace, then opens it.
	 */
	private static void copyProject(final String name) throws Exception
	{
		File source = new File(testresources(), "workspace/" + name);
		if (!source.isDirectory())
		{
			throw new IllegalStateException("missing test resource: " + source.getAbsolutePath());
		}

		final File target = new File(ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile(), name);
		copyTree(source.toPath(), target.toPath());

		// the workspace lock is taken in one go: Servoy's own startup jobs hold it too, and a
		// half-created project is worse than a slow one
		ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable)monitor -> {
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (!project.exists())
			{
				// the description is read from the copied .project rather than made fresh: a fresh one
				// carries no natures, and writing it back would strip the solution of the very thing
				// that makes the workspace recognise it as one
				IPath descriptionFile = org.eclipse.core.runtime.Path.fromOSString(
					new File(target, ".project").getAbsolutePath());
				IProjectDescription description = ResourcesPlugin.getWorkspace()
					.loadProjectDescription(descriptionFile);
				project.create(description, monitor);
			}
			if (!project.isOpen()) project.open(monitor);
			project.refreshLocal(IResource.DEPTH_INFINITE, monitor);
		}, new NullProgressMonitor());
	}

	/**
	 * Creates the table the tenant tools read, with the three acme rows and the two globex ones.
	 *
	 * <p>Written as SQL rather than shipped as data so the test says out loud what it assumes: a
	 * table with a tenant column, and rows belonging to two different tenants.</p>
	 */
	private static void createTenantTable() throws Exception
	{
		// through Servoy's own server rather than through DriverManager: the database behind
		// example_data is whatever the developer is configured against, and its driver is on the
		// server's class path, not on this bundle's
		IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(SERVER);
		if (server == null)
		{
			throw new IllegalStateException("no database server named '" + SERVER +
				"' - the tests need one, and the developer they run in decides which");
		}

		try (Connection connection = server.getConnection();
			Statement statement = connection.createStatement())
		{
			// created rather than dropped and recreated: the table may already be there, carrying the
			// tenant flag in its .dbi, and dropping it would take that with it. Emptying it is enough
			// to make the rows below the only ones, which is all these tests need.
			statement.execute("create table if not exists mcp_tenant_demo (" +
				"item_id integer generated by default as identity primary key, " +
				"tenant_name varchar(50), " +
				"item_name varchar(100))");
			statement.execute("delete from mcp_tenant_demo");

			statement.execute("insert into mcp_tenant_demo (tenant_name, item_name) values ('acme', 'Acme anvil')");
			statement.execute("insert into mcp_tenant_demo (tenant_name, item_name) values ('acme', 'Acme rocket')");
			statement.execute("insert into mcp_tenant_demo (tenant_name, item_name) values ('acme', 'Acme rope')");
			statement.execute("insert into mcp_tenant_demo (tenant_name, item_name) values ('globex', 'Globex gadget')");
			statement.execute("insert into mcp_tenant_demo (tenant_name, item_name) values ('globex', 'Globex widget')");
		}
	}

	private static void activateSolution() throws Exception
	{
		IDeveloperServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();

		// the model rebuilds its list of solutions on its own schedule after the projects appear, so
		// it is waited for rather than asked once
		waitUntil(() -> servoyProject(model, SOLUTION) != null, ACTIVATION_TIMEOUT_MS,
			"the workspace to recognise '" + SOLUTION + "' as a solution");

		ServoyProject project = servoyProject(model, SOLUTION);

		model.setActiveProject(project, true);

		// activation finishes when the solution can actually be served, not after any particular
		// number of seconds: asking is cheap, and a fixed wait is wrong in both directions - it
		// wastes time on a fast machine and still runs out on a slow one
		waitUntil(() -> {
			try
			{
				return McpCall.toolsList(SOLUTION).contains("myScope_");
			}
			catch (Exception e)
			{
				return false;
			}
		}, ACTIVATION_TIMEOUT_MS, "solution '" + SOLUTION + "' to be served over MCP");
	}

	/**
	 * The model's project of that name, or <code>null</code> while it has not noticed it yet.
	 */
	private static ServoyProject servoyProject(IDeveloperServoyModel model, String name)
	{
		ServoyProject[] projects = model.getServoyProjects();
		if (projects == null) return null;

		for (ServoyProject candidate : projects)
		{
			if (name.equals(candidate.getProject().getName())) return candidate;
		}

		return null;
	}

	/**
	 * Waits for something to become true, or says what never did.
	 *
	 * @param condition asked repeatedly; it must not throw, and must be cheap
	 * @param timeoutMs how long to keep asking
	 * @param what named in the failure, so a timeout in the build log is readable on its own
	 */
	public static void waitUntil(BooleanSupplier condition, long timeoutMs, String what) throws InterruptedException
	{
		long deadline = System.currentTimeMillis() + timeoutMs;

		while (System.currentTimeMillis() < deadline)
		{
			if (condition.getAsBoolean()) return;
			Thread.sleep(POLL_INTERVAL_MS);
		}

		throw new IllegalStateException("gave up after " + (timeoutMs / 1000) + "s waiting for " + what);
	}

	/**
	 * The bundle's own <code>testresources</code> folder.
	 *
	 * <p>Found through the bundle rather than through a system property on purpose:
	 * <code>servoy.application_server.dir</code> points at the installed application server, which is
	 * a different thing and would send this looking in the wrong place.</p>
	 */
	private static File testresources()
	{
		try
		{
			Bundle bundle = FrameworkUtil.getBundle(McpTestWorkspace.class);
			if (bundle != null)
			{
				URL entry = bundle.getEntry("testresources");
				if (entry != null) return new File(FileLocator.toFileURL(entry).getPath());
			}
		}
		catch (Exception e)
		{
			// not running in OSGi, or the folder is not in the bundle: the working directory is the
			// project when the tests are run as an ordinary JUnit launch, so that is worth a try
		}

		return new File("testresources");
	}

	private static void copyTree(Path source, Path target) throws IOException
	{
		Files.walkFileTree(source, new SimpleFileVisitor<Path>()
		{
			@Override
			public FileVisitResult preVisitDirectory(Path directory, BasicFileAttributes attributes) throws IOException
			{
				Files.createDirectories(target.resolve(source.relativize(directory)));
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attributes) throws IOException
			{
				Files.copy(file, target.resolve(source.relativize(file)), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}
}
