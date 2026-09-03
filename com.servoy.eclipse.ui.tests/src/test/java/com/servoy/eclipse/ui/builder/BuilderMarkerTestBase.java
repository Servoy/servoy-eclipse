/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 2026 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301 USA.
*/
package com.servoy.eclipse.ui.builder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;

import org.eclipse.core.resources.ICommand;
import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ngclient.ui.CopySourceFolderAction;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.UUID;

/**
 * Shared PDE integration harness for tests that create Servoy artifacts in a
 * synthetic solution and run the {@code com.servoy.eclipse.core.servoyBuilder}
 * to assert the resulting Problems-view markers.
 * <p>
 * Ported from the SVY-21356 integration test that originally lived in the
 * Servoy-Copilot repo (removed there in {@code 1edba806} because it exercises
 * servoy-eclipse builder code and does not belong in Copilot). Trimmed to the
 * helpers the builder-marker tests actually use.
 * <p>
 * Written against JUnit 4 (not Jupiter): this fragment inherits
 * {@code com.servoy.eclipse.ui}'s full dependency graph via Fragment-Host, which
 * transitively pulls in mismatched JUnit 5/6 jars that break the JUnit 5 engine
 * in this workspace (same reason as {@code RetargetToEditorPersistPropertiesIntegrationTest}).
 */
public abstract class BuilderMarkerTestBase
{
	private static final long APP_SERVER_POLL_MS = 15_000;
	private static final long ACTIVATE_SETTLE_MS = 10_000;

	private static Boolean appServerAvailableCache;

	protected final String testSolutionName;
	private final String servoyResourcesProjectName;
	protected final String solutionUUID;

	protected BuilderMarkerTestBase(String testSolutionName, String servoyResourcesProjectName)
	{
		this.testSolutionName = testSolutionName;
		this.servoyResourcesProjectName = servoyResourcesProjectName;
		this.solutionUUID = UUID.randomUUID().toString();
	}

	protected static void waitForAppServer() throws InterruptedException
	{
		if (appServerAvailableCache == null)
		{
			long deadline = System.currentTimeMillis() + APP_SERVER_POLL_MS;
			while (!ApplicationServerRegistry.exists() && System.currentTimeMillis() < deadline)
				Thread.sleep(500);
			appServerAvailableCache = ApplicationServerRegistry.exists();
		}
		assertTrue("Servoy application server not started - skipping", appServerAvailableCache);
	}

	protected void ensureTestSolutionInWorkspace() throws Exception
	{
		ensureSolutionInWorkspace(testSolutionName, solutionUUID, servoyResourcesProjectName);
	}

	protected static void ensureSolutionInWorkspace(String solutionName, String solutionUUID, String resPrjName) throws Exception
	{
		ResourcesPlugin.getWorkspace().run((IWorkspaceRunnable)monitor -> {
			IProject res = ResourcesPlugin.getWorkspace().getRoot().getProject(resPrjName);
			if (!res.exists())
			{
				IProjectDescription d = ResourcesPlugin.getWorkspace().newProjectDescription(resPrjName);
				d.setNatureIds(new String[] { "com.servoy.eclipse.core.ServoyResources" });
				res.create(d, monitor);
			}
			if (!res.isOpen()) res.open(monitor);

			IProject sol = ResourcesPlugin.getWorkspace().getRoot().getProject(solutionName);
			boolean initializeIt = false;
			if (!sol.exists())
			{
				IProjectDescription d = ResourcesPlugin.getWorkspace().newProjectDescription(solutionName);
				d.setNatureIds(new String[] { "com.servoy.eclipse.core.ServoyProject", "org.eclipse.dltk.javascript.core.nature" });
				ICommand sc = d.newCommand();
				sc.setBuilderName("org.eclipse.dltk.core.scriptbuilder");
				ICommand sb = d.newCommand();
				sb.setBuilderName("com.servoy.eclipse.core.servoyBuilder");
				d.setBuildSpec(new ICommand[] { sc, sb });
				d.setReferencedProjects(new IProject[] { res });
				sol.create(d, monitor);
				initializeIt = true;
			}
			if (!sol.isOpen())
			{
				sol.open(monitor);
				initializeIt = true;
			}

			if (initializeIt)
			{
				writeProjectFile(sol, "rootmetadata.obj",
					"fileVersion:" + AbstractRepository.repository_version + ",\nmustAuthenticate:false,\nname:\"" +
						solutionName + "\",\nsolutionType:1024,\ntypeid:43,\nuuid:\"" + solutionUUID + "\"\n",
					monitor);
				writeProjectFile(sol, "solution_settings.obj",
					"typeid:43,\nuuid:\"" + solutionUUID + "\",\nversion:\"1.0\"\n", monitor);
				writeProjectFile(sol, ".buildpath",
					"<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n<buildpath>\n\t<buildpathentry excluding=\".stp/|medias/\" kind=\"src\" path=\"\"/>\n</buildpath>\n",
					monitor);
			}
		}, new NullProgressMonitor());
	}

	protected void ensureActiveProject() throws Exception
	{
		IDeveloperServoyModel model = ServoyModelManager.getServoyModelManager().getServoyModel();
		model.refreshServoyProjects();
		ServoyProject[] servoyProjectForSolution = { null };
		pumpEventsUntil(2000, () -> {
			ServoyProject[] projects = model.getServoyProjects();
			assertTrue("No ServoyProject found in workspace", projects != null && projects.length > 0);
			for (ServoyProject p : projects)
			{
				if (testSolutionName.equals(p.getProject().getName()))
				{
					servoyProjectForSolution[0] = p;
					break;
				}
			}
			assertNotNull("Cannot find test solution's project in order to activate it", servoyProjectForSolution[0]);
		});

		ServoyProject active = model.getActiveProject();
		if (active == null || !testSolutionName.equals(active.getProject().getName()))
			model.setActiveProject(servoyProjectForSolution[0], true);

		pumpEventsUntil(ACTIVATE_SETTLE_MS, () -> {
			assertNotNull("Active project is null", model.getActiveProject());
			assertNotNull("Solution should be loaded after activation", model.getActiveProject().getSolution());
			assertNotNull("Editing solution should be resolved after activation", model.getActiveProject().getEditingSolution());
			assertEquals("Project '" + testSolutionName + "' was not activated successfully", testSolutionName,
				model.getActiveProject().getSolution().getName());
		});

		waitForWorkspaceBuildJobs();
	}

	protected static void writeProjectFile(IProject project, String fileName, String content, IProgressMonitor monitor) throws CoreException
	{
		IFile file = project.getFile(fileName);
		byte[] bytes = content.getBytes(StandardCharsets.UTF_8);
		if (file.exists())
		{
			file.setContents(new ByteArrayInputStream(bytes), true, true, monitor);
		}
		else
		{
			IContainer parent = file.getParent();
			if (parent instanceof IFolder && !parent.exists())
			{
				createFolderHierarchy((IFolder)parent, monitor);
			}
			file.create(new ByteArrayInputStream(bytes), true, monitor);
		}
	}

	private static void createFolderHierarchy(IFolder folder, IProgressMonitor monitor) throws CoreException
	{
		if (!folder.getParent().exists() && folder.getParent() instanceof IFolder)
		{
			createFolderHierarchy((IFolder)folder.getParent(), monitor);
		}
		if (!folder.exists())
		{
			folder.create(true, true, monitor);
		}
	}

	protected static void pumpEventsUntil(long forMaxMs, Runnable untilAssertionsPass)
	{
		boolean success = false;
		try
		{
			Display display = Display.getDefault();
			long end = System.currentTimeMillis() + forMaxMs;
			if (display.getThread() == Thread.currentThread())
			{
				while (!(success = justCheckCall(untilAssertionsPass)) && System.currentTimeMillis() < end)
					display.readAndDispatch();
			}
			else
			{
				while (!(success = justCheckCall(untilAssertionsPass)) && System.currentTimeMillis() < end)
					Thread.sleep(100);
			}
			if (!success) untilAssertionsPass.run();
		}
		catch (Exception e)
		{
			throw new RuntimeException(e);
		}
	}

	private static boolean justCheckCall(Runnable untilAssertionsPass)
	{
		try
		{
			untilAssertionsPass.run();
			return true;
		}
		catch (AssertionError e)
		{
			return false;
		}
	}

	protected static void deleteProjects(String... projectNames) throws CoreException
	{
		NullProgressMonitor monitor = new NullProgressMonitor();
		for (String name : projectNames)
		{
			IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (project.exists())
			{
				if (!project.isOpen()) project.open(monitor);
				project.delete(true, true, monitor);
			}
		}
	}

	protected static void waitForWorkspaceBuildJobs()
	{
		IJobManager jm = Job.getJobManager();
		pumpEventsUntil(ACTIVATE_SETTLE_MS, () -> {
			try
			{
				jm.join(ResourcesPlugin.FAMILY_AUTO_BUILD, null);
				jm.join(ResourcesPlugin.FAMILY_MANUAL_BUILD, null);
				jm.join(CopySourceFolderAction.JOB_FAMILY, null);
			}
			catch (Exception e)
			{
				// ignore - best-effort drain
			}
			if (jm.find(ResourcesPlugin.FAMILY_AUTO_BUILD).length != 0 || jm.find(ResourcesPlugin.FAMILY_MANUAL_BUILD).length != 0 ||
				jm.find(CopySourceFolderAction.JOB_FAMILY).length != 0)
			{
				fail("Build jobs still running after " + (ACTIVATE_SETTLE_MS / 1000) + " sec.");
			}
		});
	}
}
