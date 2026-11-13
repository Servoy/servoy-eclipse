package com.servoy.eclipse.model.tests.integrationpde.nature;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Arrays;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;

import com.servoy.eclipse.model.nature.ServoyProject;

/**
 * PDE integration tests for
 * {@link ServoyProject#removeDeveloperProject(IProject)} and its inverse
 * {@link ServoyProject#addDeveloperProject(IProject)} (SVY-20602).
 *
 * <p>
 * Both methods operate purely on the active-solution project's
 * {@link IProjectDescription} referenced-projects array, so the test builds a
 * small set of real workspace {@link IProject}s (a stand-in active-solution
 * project, a developer-solution project and an unrelated referenced project),
 * wraps the active one in a {@link ServoyProject}, and asserts the
 * referenced-set transformations. This needs a live Eclipse workspace/OSGi
 * container, hence it lives in {@code integrationpde}.
 */
@TestInstance(Lifecycle.PER_CLASS)
class ServoyProjectDeveloperReferenceTest {
	private static final String ACTIVE = "svy20602_active_solution";
	private static final String DEV = "svy20602_developer_solution";
	private static final String OTHER = "svy20602_other_referenced";

	private IProject activeProject;
	private IProject developerProject;
	private IProject otherProject;
	private ServoyProject servoyProject;

	@BeforeAll
	void deleteLeftoverProjects() throws CoreException {
		for (String name : new String[] { ACTIVE, DEV, OTHER }) {
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (p.exists()) {
				p.delete(true, true, null);
			}
		}
	}

	@AfterAll
	void cleanupProjects() throws CoreException {
		for (String name : new String[] { ACTIVE, DEV, OTHER }) {
			IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
			if (p.exists()) {
				p.delete(true, true, null);
			}
		}
	}

	@BeforeEach
	void createFreshProjects() throws CoreException {
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IWorkspaceRoot root = workspace.getRoot();

		activeProject = recreate(root.getProject(ACTIVE));
		developerProject = recreate(root.getProject(DEV));
		otherProject = recreate(root.getProject(OTHER));

		// start the active solution with exactly one (non-developer) referenced project
		IProjectDescription description = activeProject.getDescription();
		description.setReferencedProjects(new IProject[] { otherProject });
		activeProject.setDescription(description, null);

		servoyProject = new ServoyProject();
		servoyProject.setProject(activeProject);
	}

	private static IProject recreate(IProject project) throws CoreException {
		if (project.exists()) {
			project.delete(true, true, null);
		}
		project.create(null);
		project.open(null);
		return project;
	}

	private List<IProject> referencedProjectsOf(IProject project) throws CoreException {
		return Arrays.asList(project.getDescription().getReferencedProjects());
	}

	@Test
	@DisplayName("removeDeveloperProject un-references the developer project from the active solution")
	void removesDeveloperProjectReference() throws CoreException {
		servoyProject.addDeveloperProject(developerProject);
		assertTrue(referencedProjectsOf(activeProject).contains(developerProject),
				"precondition: developer project must be referenced before the remove");

		servoyProject.removeDeveloperProject(developerProject);

		assertFalse(referencedProjectsOf(activeProject).contains(developerProject),
				"developer project must no longer be a referenced project of the active solution");
	}

	@Test
	@DisplayName("removeDeveloperProject leaves the developer-solution project itself on disk")
	void leavesDeveloperProjectOnDisk() throws CoreException {
		servoyProject.addDeveloperProject(developerProject);

		servoyProject.removeDeveloperProject(developerProject);

		assertAll(
				() -> assertTrue(developerProject.exists(),
						"developer-solution project must still exist in the workspace"),
				() -> assertTrue(developerProject.isOpen(),
						"developer-solution project must still be open (not deleted)"));
	}

	@Test
	@DisplayName("add then remove round-trips back to the original referenced set (exact inverse of add)")
	void addThenRemoveIsIdentity() throws CoreException {
		IProject[] before = activeProject.getDescription().getReferencedProjects();

		servoyProject.addDeveloperProject(developerProject);
		servoyProject.removeDeveloperProject(developerProject);

		IProject[] after = activeProject.getDescription().getReferencedProjects();
		assertArrayEquals(before, after, "removeDeveloperProject must be the exact inverse of addDeveloperProject");
	}

	@Test
	@DisplayName("removeDeveloperProject leaves non-developer referenced projects untouched")
	void keepsOtherReferencedProjects() throws CoreException {
		servoyProject.addDeveloperProject(developerProject);

		servoyProject.removeDeveloperProject(developerProject);

		List<IProject> referenced = referencedProjectsOf(activeProject);
		assertAll(() -> assertEquals(1, referenced.size(), "only the single non-developer reference must remain"),
				() -> assertTrue(referenced.contains(otherProject),
						"the unrelated referenced project must be preserved"));
	}

	@Test
	@DisplayName("removeDeveloperProject is a no-op when the project is not referenced")
	void noOpWhenNotReferenced() throws CoreException {
		IProject[] before = activeProject.getDescription().getReferencedProjects();

		servoyProject.removeDeveloperProject(developerProject);

		IProject[] after = activeProject.getDescription().getReferencedProjects();
		assertArrayEquals(before, after,
				"removing a project that is not referenced must leave the referenced set unchanged");
	}
}
