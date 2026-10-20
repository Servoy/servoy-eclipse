package com.servoy.eclipse.designer.tests.integrationpde.outline;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.awt.Dimension;
import java.awt.Point;
import java.util.List;

import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.tests.integration.utils.TestUtilitiesClass;
import com.servoy.eclipse.designer.outline.FormOutlineContentProvider;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Solution;

/**
 * SVY-21202: {@code FormOutlinePage.persistChanges} must refresh the tree without re-applying the default expansion, so
 * nodes the user collapsed stay collapsed. Uses a real {@link FormOutlinePage} on a real form.
 */
@DisplayName("FormOutlinePage.persistChanges keeps the expand state (SVY-21202)")
public class FormOutlinePagePersistChangesIntegrationTest extends TestUtilitiesClass
{
	private static final String TEST_SOLUTION = "test_svy21202_outline";
	private static final String SERVOY_RESOURCES = "servoy_resources";

	private ServoyProject activeProject;

	public FormOutlinePagePersistChangesIntegrationTest()
	{
		super(TEST_SOLUTION, SERVOY_RESOURCES);
	}

	@BeforeAll
	public static void deleteProjectsBeforeClass() throws Exception
	{
		deleteProjects(TEST_SOLUTION, SERVOY_RESOURCES);
		waitForWorkspaceBuildJobs();
	}

	@BeforeEach
	public void setUp() throws Exception
	{
		waitForAppServer();
		ensureTestSolutionInWorkspace(null, null);
		ensureActiveProject();
		activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		assertNotNull(activeProject, "Active project required");
	}

	@Test
	@DisplayName("a node collapsed by the user stays collapsed after persistChanges")
	public void collapsedNodeStaysCollapsedAfterPersistChanges() throws Throwable
	{
		Solution solution = activeProject.getEditingSolution();
		IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();

		Form form = solution.createNewForm(validator, null, "svy21202_form_" + System.nanoTime(), null, true, new Dimension(640, 480));
		form.createNewPart(Part.BODY, 480);
		Field field = form.createNewField(new Point(20, 20));
		field.setName("svy21202_field");
		activeProject.saveEditingSolutionNodes(new IPersist[] { form }, true);
		waitForWorkspaceBuildJobs();

		runOnDisplayThread(() -> {
			Shell shell = new Shell(Display.getCurrent(), SWT.NONE);
			try
			{
				shell.setLayout(new FillLayout());
				TestableFormOutlinePage page = new TestableFormOutlinePage(form);
				page.createControl(shell);

				// createControl applies the default expansion
				assertTrue(page.getTreeViewer().getExpandedState(FormOutlineContentProvider.ELEMENTS),
					"precondition: the default expansion should have expanded the elements node");

				page.getTreeViewer().setExpandedState(FormOutlineContentProvider.ELEMENTS, false);
				assertFalse(page.getTreeViewer().getExpandedState(FormOutlineContentProvider.ELEMENTS), "precondition: node should be collapsed");

				page.persistChanges(List.of(field));

				assertFalse(page.getTreeViewer().getExpandedState(FormOutlineContentProvider.ELEMENTS),
					"persistChanges must not re-apply the default expansion over the user's collapsed state");
			}
			finally
			{
				shell.dispose();
			}
		});
	}

	/** Exposes the protected {@code getTreeViewer()} of {@code ContentOutlinePage}. */
	private static class TestableFormOutlinePage extends FormOutlinePage
	{
		TestableFormOutlinePage(Form form)
		{
			super(form, new ScrollingGraphicalViewer(), null);
		}

		@Override
		public TreeViewer getTreeViewer()
		{
			return super.getTreeViewer();
		}
	}

	@FunctionalInterface
	private interface ThrowingRunnable
	{
		void run() throws Throwable;
	}

	private static void runOnDisplayThread(ThrowingRunnable runnable) throws Throwable
	{
		Display display = Display.getDefault();
		if (display.getThread() == Thread.currentThread())
		{
			runnable.run();
			return;
		}
		Throwable[] failure = new Throwable[1];
		display.syncExec(() -> {
			try
			{
				runnable.run();
			}
			catch (Throwable t)
			{
				failure[0] = t;
			}
		});
		if (failure[0] != null) throw failure[0];
	}
}
