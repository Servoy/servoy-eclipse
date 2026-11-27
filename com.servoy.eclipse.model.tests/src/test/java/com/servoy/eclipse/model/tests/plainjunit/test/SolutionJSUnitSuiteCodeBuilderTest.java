package com.servoy.eclipse.model.tests.plainjunit.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.test.SolutionJSUnitSuiteCodeBuilder;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectReference;
import com.servoy.j2db.persistence.Solution;

class SolutionJSUnitSuiteCodeBuilderTest
{
	@Test
	@DisplayName("initializeWithError generates a failing suite")
	void errorSuite()
	{
		SolutionJSUnitSuiteCodeBuilder b = new SolutionJSUnitSuiteCodeBuilder();
		b.initializeWithError("boom");
		assertNotNull(b.getCode());
		assertTrue(b.getCode().contains("CannotRunJSUnitTests"));
		assertTrue(b.getCode().contains(".fail("));
	}

	@Test
	@DisplayName("a solution with no tests produces the no-tests error suite")
	void noTests() throws Exception
	{
		Solution solution = solution("sol");
		FlattenedSolution fs = mock(FlattenedSolution.class);
		when(fs.getForms(false)).thenReturn(Collections.<Form> emptyList().iterator());
		SolutionJSUnitSuiteCodeBuilder b = new SolutionJSUnitSuiteCodeBuilder();
		b.initializeWithSolution(solution, fs, null, false);
		assertNotNull(b.getCode());
		assertTrue(b.getCode().contains("CannotRunJSUnitTests"));
	}

	private static Solution solution(String name) throws RepositoryException
	{
		Solution s = mock(Solution.class);
		doReturn(name).when(s).getName();
		doReturn(Collections.emptyList()).when(s).getScopeNames();
		when(s.getForms(null, true)).thenReturn(Collections.<Form> emptyList().iterator());
		when(s.getReferencedModules(null)).thenReturn(List.<RootObjectReference> of());
		return s;
	}
}