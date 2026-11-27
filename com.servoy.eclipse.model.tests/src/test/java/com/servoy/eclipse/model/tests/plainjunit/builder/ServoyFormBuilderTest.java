package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;

import org.eclipse.core.resources.IResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyFormBuilder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.ScriptVariable;


class ServoyFormBuilderTest
{
	private SeverityPreferences preferences;
	private WorkspaceMarkers workspace;
	private IServoyModel model;

	@BeforeEach
	void setUp()
	{
		preferences = new SeverityPreferences();
		workspace = new WorkspaceMarkers();
		model = ModelTestAccess.installMockServoyModel();
		ServoyProject activeProject = mock(ServoyProject.class);
		when(activeProject.getProject()).thenReturn(mock(org.eclipse.core.resources.IProject.class));
		when(model.getActiveProject()).thenReturn(activeProject);
	}

	@AfterEach
	void tearDown()
	{
		ModelTestAccess.reset();
		workspace.close();
		preferences.close();
	}

	@Test
	@DisplayName("addFormVariablesHideTableColumn reports a marker when a script variable shadows a table column")
	void hideTableColumn()
	{
		IResource markerResource = workspace.file("/forms/f.frm");
		Form form = mock(Form.class);
		when(form.getName()).thenReturn("f");
		when(form.getTableName()).thenReturn("t");
		com.servoy.j2db.persistence.IRootObject root = mock(com.servoy.j2db.persistence.IRootObject.class);
		when(root.getName()).thenReturn("sol");
		when(form.getRootObject()).thenReturn(root);

		ScriptVariable var = mock(ScriptVariable.class);
		when(var.getName()).thenReturn("col1");
		when(var.getRootObject()).thenReturn(root);
		Iterator<ScriptVariable> it = Arrays.asList(var).iterator();
		when(form.getScriptVariables(false)).thenReturn(it);

		ITable table = mock(ITable.class);
		when(table.getColumn("col1")).thenReturn(mock(com.servoy.j2db.persistence.Column.class));

		ServoyFormBuilder.addFormVariablesHideTableColumn(markerResource, form, table);

		assertEquals(1, workspace.markers().size());
		assertEquals(MarkerMessages.FormVariableTableCol.fill("f", "col1", "t").getText(), workspace.messages().get(0));
	}

	@Test
	@DisplayName("addFormVariablesHideTableColumn does nothing when the table is null or no variable shadows a column")
	void noMarker()
	{
		IResource markerResource = workspace.file("/forms/f.frm");
		Form form = mock(Form.class);
		when(form.getName()).thenReturn("f");

		ScriptVariable var = mock(ScriptVariable.class);
		when(var.getName()).thenReturn("col1");
		when(form.getScriptVariables(false)).thenReturn(Arrays.asList(var).iterator());

		ITable table = mock(ITable.class);
		when(table.getColumn("col1")).thenReturn(null);

		ServoyFormBuilder.addFormVariablesHideTableColumn(markerResource, form, table);
		ServoyFormBuilder.addFormVariablesHideTableColumn(markerResource, form, null);

		assertEquals(0, workspace.markers().size());
	}
}
