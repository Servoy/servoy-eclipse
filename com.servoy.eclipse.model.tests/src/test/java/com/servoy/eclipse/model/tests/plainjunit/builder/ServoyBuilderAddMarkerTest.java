package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.resources.IMarker;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.j2db.util.Pair;

class ServoyBuilderAddMarkerTest
{
	private SeverityPreferences preferences;
	private WorkspaceMarkers workspace;

	@BeforeEach
	void setUp()
	{
		preferences = new SeverityPreferences();
		workspace = new WorkspaceMarkers();
		var model = ModelTestAccess.installMockServoyModel();
		ServoyProject project = mock(ServoyProject.class);
		var wsProject = workspace.project("sol");
		when(project.getProject()).thenReturn(wsProject);
		when(model.getActiveProject()).thenReturn(project);
	}

	@AfterEach
	void tearDown()
	{
		ModelTestAccess.reset();
		workspace.close();
		preferences.close();
	}

	@Test
	@DisplayName("addMarker with a null severity pair records no marker")
	void nullSeverity()
	{
		assertNull(ServoyBuilder.addMarker(workspace.file("/f.frm"), "type", "msg", -1,
			(Pair<String, ProblemSeverity>)null, IMarker.PRIORITY_NORMAL, null, null));
		assertEquals(0, workspace.markers().size());
	}

	@Test
	@DisplayName("addMarker with IGNORE severity records no marker")
	void ignored()
	{
		preferences.set("test", "IGNORE");
		var persist = mock(com.servoy.j2db.persistence.IPersist.class);
		when(persist.getRootObject()).thenReturn(mock(com.servoy.j2db.persistence.IRootObject.class));
		assertNull(ServoyBuilder.addMarker(workspace.file("/f.frm"), "type", "msg", -1,
			new Pair<String, ProblemSeverity>("test", ProblemSeverity.WARNING), IMarker.PRIORITY_NORMAL, null, persist));
		assertEquals(0, workspace.markers().size());
	}
}