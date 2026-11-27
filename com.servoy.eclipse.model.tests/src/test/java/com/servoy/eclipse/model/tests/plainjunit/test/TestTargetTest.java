package com.servoy.eclipse.model.tests.plainjunit.test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.test.TestTarget;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Pair;

class TestTargetTest
{
	private Solution active;
	private FlattenedSolution fs;

	@BeforeEach
	void setUp()
	{
		IServoyModel model = ModelTestAccess.installMockServoyModel();
		ServoyProject project = mock(ServoyProject.class);
		active = mock(Solution.class);
		doReturn("active").when(active).getName();
		when(project.getSolution()).thenReturn(active);
		fs = mock(FlattenedSolution.class);
		when(fs.getSolution()).thenReturn(active);
		when(fs.getModules()).thenReturn(new Solution[0]);
		when(project.getEditingFlattenedSolution()).thenReturn(fs);
		when(model.getActiveProject()).thenReturn(project);
	}

	@AfterEach
	void tearDown()
	{
		ModelTestAccess.reset();
	}

	@Test
	@DisplayName("convertToString round-trips a solution target")
	void solutionTarget()
	{
		Solution module = mock(Solution.class);
		doReturn("mod").when(module).getName();
		when(fs.getModules()).thenReturn(new Solution[] { module });
		TestTarget t = new TestTarget(module);
		assertEquals(t.convertToString(), t.toString());
		TestTarget back = TestTarget.convertFromString("1|active|mod");
		assertEquals(module.getName(), back.getModuleToTest().getName());
	}

	@Test
	@DisplayName("convertToString round-trips a form target")
	void formTarget()
	{
		Form form = mock(Form.class);
		doReturn("f1").when(form).getName();
		when(fs.getForm("f1")).thenReturn(form);
		TestTarget t = new TestTarget(form);
		assertEquals("2|active|f1", t.convertToString());
		TestTarget back = TestTarget.convertFromString("2|active|f1");
		assertEquals("f1", back.getFormToTest().getName());
	}

	@Test
	@DisplayName("convertToString round-trips a global scope target")
	void scopeTarget()
	{
		TestTarget t = new TestTarget(new Pair<Solution, String>(active, "scope1"));
		assertEquals("3|active|active|scope1", t.convertToString());
		TestTarget back = TestTarget.convertFromString("3|active|active|scope1");
		assertEquals("scope1", back.getGlobalScopeToTest().getRight());
	}

	@Test
	@DisplayName("findSolution finds the active solution and modules, else null")
	void findSolution()
	{
		Solution module = mock(Solution.class);
		doReturn("mod").when(module).getName();
		when(fs.getModules()).thenReturn(new Solution[] { module });
		assertEquals(active, TestTarget.findSolution("active"));
		assertEquals(module, TestTarget.findSolution("mod"));
		assertNull(TestTarget.findSolution("missing"));
	}

	@Test
	@DisplayName("activeProjectTarget uses the active project's solution")
	void activeTarget()
	{
		TestTarget t = TestTarget.activeProjectTarget();
		assertEquals(active, t.getActiveSolution());
		assertTrue(t.getGlobalScopeToTest() == null);
		assertEquals(active, t.getModuleToTest());
	}
}