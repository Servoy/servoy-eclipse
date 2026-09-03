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

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.swt.widgets.Display;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.query.IBaseSQLCondition;
import com.servoy.base.query.IQueryConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyBuilderUtils;
import com.servoy.eclipse.model.builder.ServoyFormBuilder;
import com.servoy.eclipse.model.builder.ServoyRelationBuilder;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.AbstractTable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.ColumnType;
import com.servoy.j2db.util.UUID;

/**
 * PDE plug-in integration tests for the two builder marker checks added by SVY-21356:
 * <ul>
 * <li>{@link ServoyBuilder#FORM_CSS_POSITION_NO_BODY_PART} - a CSS-position,
 * non-responsive form whose flattened form has no {@link Part#BODY} now gets an ERROR
 * marker (see {@code ServoyFormBuilder.addFormMarkers}).</li>
 * <li>{@link ServoyBuilder#RELATION_ITEM_TYPE_PROBLEM} - a relation item with a
 * {@code Relation.checkKeyTypes()} mismatch is now ERROR severity (was WARNING), with
 * {@code IMarker.PRIORITY_NORMAL} (see {@code ServoyRelationBuilder.checkRelation}).</li>
 * </ul>
 * <p>
 * Ported from the Servoy-Copilot repo (originally added under SVY-21356, removed there
 * in {@code 1edba806} because it exercises servoy-eclipse builder code) into
 * {@code com.servoy.eclipse.ui.tests}, which already inherits the full UI/core/ngclient
 * dependency graph the harness needs. Covers spec acceptance criteria
 * (docs/SVY-21356-css-form-body-relation-severity.spec.md) plus the NO-GAP regression
 * scenarios from the investigation.
 */
public class FormCssPositionAndRelationSeverityIntegrationTest extends BuilderMarkerTestBase
{
	private static final String TEST_SOLUTION = "test_svy21356_suite";
	private static final String SERVOY_RESOURCES = "servoy_resources";

	private ServoyProject activeProject;
	private Solution solution;
	private IValidateName validator;

	public FormCssPositionAndRelationSeverityIntegrationTest()
	{
		super(TEST_SOLUTION, SERVOY_RESOURCES);
	}

	@BeforeClass
	public static void deleteProjectsBeforeClass() throws Exception
	{
		deleteProjects(TEST_SOLUTION, SERVOY_RESOURCES);
		waitForWorkspaceBuildJobs();
	}

	@Before
	public void setUp() throws Exception
	{
		assertNotNull("No Display available - test requires a running Eclipse UI", Display.getDefault());
		waitForAppServer();
		ensureTestSolutionInWorkspace();
		ensureActiveProject();

		activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		assertNotNull("Active project required", activeProject);
		solution = activeProject.getEditingSolution();
		validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
	}

	// -----------------------------------------------------------------------
	// Shared helpers
	// -----------------------------------------------------------------------

	private static String unique(String prefix)
	{
		return prefix + "_" + System.nanoTime();
	}

	/**
	 * Saves the given persists to the editing solution, waits for the write to settle, then invokes the
	 * real form marker-creation code ({@link ServoyFormBuilder#addFormMarkers}) directly on each saved
	 * {@link Form}.
	 * <p>
	 * A full {@code servoyBuilder} pass visits {@code servoyProject.getSolution()} - the active solution
	 * reloaded from disk - not the editing solution the test wrote to. That reload is driven
	 * asynchronously by the workspace resource-change listener and does not reliably land before a
	 * synchronous {@code build(FULL_BUILD)} in-test, so the builder would traverse a model that does not
	 * yet contain the freshly saved form and create no marker. Calling {@code addFormMarkers} directly on
	 * the editing form - after clearing any markers a background build may already have added - exercises
	 * exactly the same marker-creation code against a form that is guaranteed to be in the model. This is
	 * the same pattern the product itself uses in {@code ServoyValuelistBuilder}/{@code ServoyRelationBuilder}
	 * (deleteMarkers + addFormMarkers) and that {@link #saveAndCheckRelation} already relies on for relations.
	 */
	private void saveAndBuild(IPersist... persists) throws Exception
	{
		activeProject.saveEditingSolutionNodes(persists, true);
		waitForWorkspaceBuildJobs();

		Set<UUID> methodsParsed = new HashSet<>();
		Map<Form, Boolean> formsAbstractChecked = new HashMap<>();
		for (IPersist persist : persists)
		{
			if (persist instanceof Form)
			{
				Form form = (Form)persist;
				ServoyFormBuilder.deleteMarkers(form); // clear any markers the background build may already have added
				ServoyFormBuilder.addFormMarkers(activeProject, form, methodsParsed, formsAbstractChecked);
			}
		}
	}

	/**
	 * Saves the relation and then directly invokes
	 * {@link ServoyRelationBuilder#checkRelation(Relation)}, bypassing
	 * {@code ServoyBuilder}'s server-missing guard.
	 * <p>
	 * The guard in {@code ServoyBuilder} skips {@code checkRelation} for
	 * {@code mem:} datasource relations because
	 * {@link com.servoy.j2db.persistence.Relation#getPrimaryServerName()} returns
	 * {@link com.servoy.j2db.persistence.IServer#INMEM_SERVER} ("_sv_inmem") while
	 * {@code addMissingServer} only excludes
	 * {@link com.servoy.j2db.util.DataSourceUtils#INMEM_DATASOURCE} ("mem"). The two
	 * strings differ so "_sv_inmem" is added to {@code missingServers} and
	 * {@code checkRelation} is never reached from a full builder pass. Calling it
	 * directly bypasses this guard while still exercising the real marker-creation code
	 * under test.
	 */
	private void saveAndCheckRelation(Relation relation) throws Exception
	{
		activeProject.saveEditingSolutionNodes(new IPersist[] { relation }, true);
		waitForWorkspaceBuildJobs();
		ServoyRelationBuilder.deleteMarkers(relation); // clear any markers the background build may already have added
		ServoyRelationBuilder.checkRelation(relation);
	}

	/**
	 * Regenerates the form markers for a single already-saved {@link Form} (delete + addFormMarkers) and
	 * returns the resulting "no body part" markers. Runs synchronously on the caller's thread with no event
	 * pumping, so a pending background build cannot interleave and wipe the markers before they are asserted.
	 */
	private List<IMarker> regenerateAndFindNoBodyMarkers(Form form) throws CoreException
	{
		ServoyFormBuilder.deleteMarkers(form);
		ServoyFormBuilder.addFormMarkers(activeProject, form, new HashSet<>(), new HashMap<>());
		return findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
	}

	private List<IMarker> findMarkersContaining(IPersist persist, String markerType, String messageSubstring) throws CoreException
	{
		IResource resource = ServoyBuilderUtils.getPersistResource(persist);
		IMarker[] markers = resource.findMarkers(markerType, false, IResource.DEPTH_ZERO);
		List<IMarker> matched = new ArrayList<>();
		for (IMarker marker : markers)
		{
			Object message = marker.getAttribute(IMarker.MESSAGE);
			if (message != null && message.toString().contains(messageSubstring))
			{
				matched.add(marker);
			}
		}
		return matched;
	}

	private Form createPlainCssPositionForm(String name) throws RepositoryException
	{
		Form form = solution.createNewForm(validator, null, name, null, true, new Dimension(640, 480));
		form.setUseCssPosition(Boolean.TRUE);
		return form;
	}

	private ITable createMemTableWithColumn(String tableName, String columnName, int columnTypeId) throws RepositoryException, java.sql.SQLException
	{
		MemServer memServer = activeProject.getMemServer();
		ITable table = memServer.createNewTable(validator, tableName);
		((AbstractTable)table).createNewColumn(validator, columnName, ColumnType.getInstance(columnTypeId, 0, 0), true);
		memServer.syncTableObjWithDB(table, false, true);

		TableNode tableNode = solution.getOrCreateTableNode(table.getDataSource());
		activeProject.saveEditingSolutionNodes(new IPersist[] { tableNode }, true);
		return table;
	}

	// -----------------------------------------------------------------------
	// CSS-position-no-body-part marker
	// -----------------------------------------------------------------------

	@Test
	public void testCssPositionFormWithoutBodyPart_getsErrorMarker() throws Exception
	{
		String formName = unique("svy21356_cssNoBody");
		Form form = createPlainCssPositionForm(formName);

		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
		assertEquals("Expected exactly one formCssPositionNoBodyPart marker", 1, markers.size());
		IMarker marker = markers.get(0);
		assertEquals("formCssPositionNoBodyPart marker must be ERROR severity",
			IMarker.SEVERITY_ERROR, marker.getAttribute(IMarker.SEVERITY, -1));
		Object message = marker.getAttribute(IMarker.MESSAGE);
		assertNotNull(message);
		assertTrue("Marker message should reference the form by name: " + message, message.toString().contains(formName));
	}

	@Test
	public void testCssPositionFormWithBodyPart_noMarker() throws Exception
	{
		String formName = unique("svy21356_cssWithBody");
		Form form = createPlainCssPositionForm(formName);
		form.createNewPart(Part.BODY, 480);

		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
		assertTrue("Form with a body part must not get the marker: " + markers, markers.isEmpty());
	}

	@Test
	public void testCssPositionFormInheritingBodyFromDirectSuper_noMarker() throws Exception
	{
		String superName = unique("svy21356_super1");
		Form superForm = createPlainCssPositionForm(superName);
		superForm.createNewPart(Part.BODY, 480);
		saveAndBuild(superForm);

		String childName = unique("svy21356_child1");
		Form childForm = createPlainCssPositionForm(childName);
		childForm.setExtendsForm(superForm);
		childForm.setExtendsID(superForm.getUUID().toString());
		saveAndBuild(childForm);

		List<IMarker> markers = findMarkersContaining(childForm, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
		assertTrue("Form inheriting a body part from its super form must not get the marker: " + markers, markers.isEmpty());
	}

	@Test
	public void testCssPositionFormInheritingBodyThroughTwoLevels_noMarker() throws Exception
	{
		String grandSuperName = unique("svy21356_grandSuper");
		Form grandSuperForm = createPlainCssPositionForm(grandSuperName);
		grandSuperForm.createNewPart(Part.BODY, 480);
		saveAndBuild(grandSuperForm);

		String superName = unique("svy21356_super2");
		Form superForm = createPlainCssPositionForm(superName);
		superForm.setExtendsForm(grandSuperForm);
		superForm.setExtendsID(grandSuperForm.getUUID().toString());
		saveAndBuild(superForm);

		String childName = unique("svy21356_child2");
		Form childForm = createPlainCssPositionForm(childName);
		childForm.setExtendsForm(superForm);
		childForm.setExtendsID(superForm.getUUID().toString());
		saveAndBuild(childForm);

		List<IMarker> markers = findMarkersContaining(childForm, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
		assertTrue("Form inheriting a body part through a 2-level extends chain must not get the marker: " + markers, markers.isEmpty());
	}

	@Test
	public void testCssPositionInheritanceChainWithoutBodyAnywhere_markerOnEveryForm() throws Exception
	{
		String rootName = unique("svy21356_chainRoot");
		Form rootForm = createPlainCssPositionForm(rootName);

		String midName = unique("svy21356_chainMid");
		Form midForm = createPlainCssPositionForm(midName);
		midForm.setExtendsForm(rootForm);
		midForm.setExtendsID(rootForm.getUUID().toString());

		String leafName = unique("svy21356_chainLeaf");
		Form leafForm = createPlainCssPositionForm(leafName);
		leafForm.setExtendsForm(midForm);
		leafForm.setExtendsID(midForm.getUUID().toString());

		// Persist the whole chain first (one save). A full builder pass clears form markers project-wide and
		// only recreates them for forms already in the active (asynchronously reloaded) solution, so markers
		// created for one form can be wiped when a later form triggers a background build. Assert each form by
		// regenerating its markers immediately before checking it, synchronously, so no background build can
		// interleave between creation and assertion.
		saveAndBuild(rootForm, midForm, leafForm);

		assertEquals("Root form in the chain should have exactly one marker", 1, regenerateAndFindNoBodyMarkers(rootForm).size());
		assertEquals("Middle form in the chain should have exactly one marker", 1, regenerateAndFindNoBodyMarkers(midForm).size());
		assertEquals("Leaf form in the chain should have exactly one marker", 1, regenerateAndFindNoBodyMarkers(leafForm).size());
	}

	@Test
	public void testResponsiveFormWithoutBodyPart_noMarker() throws Exception
	{
		String formName = unique("svy21356_responsive");
		Form form = solution.createNewForm(validator, null, formName, null, true, new Dimension(640, 480));
		form.setResponsiveLayout(true);

		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
		assertTrue("Responsive-layout form must not get the marker: " + markers, markers.isEmpty());
	}

	@Test
	public void testPlainAnchoredAbstractFormWithoutBodyPart_noMarker() throws Exception
	{
		String formName = unique("svy21356_abstract");
		solution.createNewForm(validator, null, formName, null, true, new Dimension(640, 480));
		// useCssPosition left at default (false), responsive left at default (false):
		// exactly the state of a wizard-created "Abstract (no UI)" form.

		Form form = solution.getForm(formName);
		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "no body part");
		assertTrue("Plain anchored abstract form must not get the marker: " + markers, markers.isEmpty());
	}

	// -----------------------------------------------------------------------
	// Relation severity
	// -----------------------------------------------------------------------

	@Test
	public void testRelationWithMismatchedKeyTypes_getsErrorMarker() throws Exception
	{
		ITable primaryTable = createMemTableWithColumn(unique("svy21356_dtTable"), "dt_col", IColumnTypes.DATETIME);
		ITable foreignTable = createMemTableWithColumn(unique("svy21356_intTable"), "int_col", IColumnTypes.INTEGER);

		Relation relation = solution.createNewRelation(validator, unique("svy21356_mismatchRel"),
			primaryTable.getDataSource(), foreignTable.getDataSource(), IQueryConstants.LEFT_OUTER_JOIN);
		relation.setAllowCreationRelatedRecords(true);
		relation.createNewRelationItems(
			new IDataProvider[] { primaryTable.getColumn("dt_col") },
			new int[] { IBaseSQLCondition.EQUALS_OPERATOR },
			new Column[] { (Column)foreignTable.getColumn("int_col") });

		saveAndCheckRelation(relation);

		List<IMarker> markers = findMarkersContaining(relation, ServoyBuilder.PROJECT_RELATION_MARKER_TYPE, "mismatched keys");
		assertEquals("Expected exactly one relationItemTypeProblem marker", 1, markers.size());
		IMarker marker = markers.get(0);
		assertEquals("relationItemTypeProblem marker must now be ERROR severity (was WARNING)",
			IMarker.SEVERITY_ERROR, marker.getAttribute(IMarker.SEVERITY, -1));
		assertEquals("relationItemTypeProblem marker must use PRIORITY_NORMAL",
			IMarker.PRIORITY_NORMAL, marker.getAttribute(IMarker.PRIORITY, -1));
	}

	@Test
	public void testRelationWithOnlyCorrectlyTypedItems_noMarker() throws Exception
	{
		ITable primaryTable = createMemTableWithColumn(unique("svy21356_intTableA"), "int_col_a", IColumnTypes.INTEGER);
		ITable foreignTable = createMemTableWithColumn(unique("svy21356_intTableB"), "int_col_b", IColumnTypes.INTEGER);

		Relation relation = solution.createNewRelation(validator, unique("svy21356_okRel"),
			primaryTable.getDataSource(), foreignTable.getDataSource(), IQueryConstants.LEFT_OUTER_JOIN);
		relation.setAllowCreationRelatedRecords(true);
		relation.createNewRelationItems(
			new IDataProvider[] { primaryTable.getColumn("int_col_a") },
			new int[] { IBaseSQLCondition.EQUALS_OPERATOR },
			new Column[] { (Column)foreignTable.getColumn("int_col_b") });

		saveAndCheckRelation(relation);

		List<IMarker> markers = findMarkersContaining(relation, ServoyBuilder.PROJECT_RELATION_MARKER_TYPE, "mismatched keys");
		assertTrue("Relation with only correctly-typed items must not get the marker: " + markers, markers.isEmpty());
	}

	@Test
	public void testRelationWithOneCorrectAndOneMismatchedItem_exactlyOneMarker() throws Exception
	{
		ITable primaryTable = createMemTableWithColumn(unique("svy21356_mixedPrimary"), "int_col", IColumnTypes.INTEGER);
		((AbstractTable)primaryTable).createNewColumn(validator, "dt_col", ColumnType.getInstance(IColumnTypes.DATETIME, 0, 0), true);
		ITable foreignTable = createMemTableWithColumn(unique("svy21356_mixedForeign"), "int_col", IColumnTypes.INTEGER);
		((AbstractTable)foreignTable).createNewColumn(validator, "int_col2", ColumnType.getInstance(IColumnTypes.INTEGER, 0, 0), true);
		MemServer memServer = activeProject.getMemServer();
		memServer.syncTableObjWithDB(primaryTable, false, true);
		memServer.syncTableObjWithDB(foreignTable, false, true);

		Relation relation = solution.createNewRelation(validator, unique("svy21356_mixedRel"),
			primaryTable.getDataSource(), foreignTable.getDataSource(), IQueryConstants.LEFT_OUTER_JOIN);
		relation.setAllowCreationRelatedRecords(true);
		// item 0: correct (INTEGER = INTEGER), item 1: mismatched (DATETIME = INTEGER)
		relation.createNewRelationItems(
			new IDataProvider[] { primaryTable.getColumn("int_col"), primaryTable.getColumn("dt_col") },
			new int[] { IBaseSQLCondition.EQUALS_OPERATOR, IBaseSQLCondition.EQUALS_OPERATOR },
			new Column[] { (Column)foreignTable.getColumn("int_col2"), (Column)foreignTable.getColumn("int_col") });

		saveAndCheckRelation(relation);

		List<IMarker> markers = findMarkersContaining(relation, ServoyBuilder.PROJECT_RELATION_MARKER_TYPE, "mismatched keys");
		assertEquals("A relation with one correct and one mismatched item must produce exactly one marker (not one per item): " + markers,
			1, markers.size());
	}

	@Test
	public void testRelationUsedByValuelistOnFormField_stillGetsErrorMarkerOnRelation() throws Exception
	{
		ITable primaryTable = createMemTableWithColumn(unique("svy21356_vlRelPrimary"), "dt_col", IColumnTypes.DATETIME);
		ITable foreignTable = createMemTableWithColumn(unique("svy21356_vlRelForeign"), "int_col", IColumnTypes.INTEGER);

		Relation relation = solution.createNewRelation(validator, unique("svy21356_vlMismatchRel"),
			primaryTable.getDataSource(), foreignTable.getDataSource(), IQueryConstants.LEFT_OUTER_JOIN);
		relation.setAllowCreationRelatedRecords(true);
		relation.createNewRelationItems(
			new IDataProvider[] { primaryTable.getColumn("dt_col") },
			new int[] { IBaseSQLCondition.EQUALS_OPERATOR },
			new Column[] { (Column)foreignTable.getColumn("int_col") });

		ValueList valuelist = solution.createNewValueList(validator, unique("svy21356_relatedVl"));
		valuelist.setValueListType(IValueListConstants.RELATED_VALUES);
		valuelist.setRelationName(relation.getName());
		valuelist.setDataProviderID1(foreignTable.getColumn("int_col").getName());

		Form form = createPlainCssPositionForm(unique("svy21356_vlRelForm"));
		form.createNewPart(Part.BODY, 480);
		form.setDataSource(primaryTable.getDataSource());
		Field field = form.createNewField(new Point(20, 20));
		field.setName(unique("svy21356_vlField"));
		field.setValuelistID(valuelist.getUUID().toString());

		saveAndBuild(form, valuelist);
		saveAndCheckRelation(relation);

		List<IMarker> markers = findMarkersContaining(relation, ServoyBuilder.PROJECT_RELATION_MARKER_TYPE, "mismatched keys");
		assertEquals("Expected exactly one relationItemTypeProblem marker on the relation, even though it is used by " +
			"a valuelist attached to a form field", 1, markers.size());
		IMarker marker = markers.get(0);
		assertEquals("relationItemTypeProblem marker must be ERROR severity even when the relation backs a valuelist",
			IMarker.SEVERITY_ERROR, marker.getAttribute(IMarker.SEVERITY, -1));
	}

	// -----------------------------------------------------------------------
	// NO-GAP regression scenarios (confirmed already covered by generic checks)
	// -----------------------------------------------------------------------

	@Test
	public void testFieldWithDanglingValuelistId_getsPropertyTargetNotFoundMarker() throws Exception
	{
		String formName = unique("svy21356_danglingVl");
		Form form = createPlainCssPositionForm(formName);
		form.createNewPart(Part.BODY, 480);

		Field field = form.createNewField(new Point(20, 20));
		field.setName(unique("svy21356_field"));
		// a syntactically valid UUID that does not resolve to any persist in the solution
		field.setValuelistID(UUID.randomUUID().toString());

		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "is linked to an entity that does not exist");
		assertEquals("Expected exactly one dangling-valuelistID marker", 1, markers.size());
		IMarker marker = markers.get(0);
		assertEquals("Dangling valuelistID marker must be ERROR severity (formPropertyTargetNotFound)",
			IMarker.SEVERITY_ERROR, marker.getAttribute(IMarker.SEVERITY, -1));
	}

	@Test
	public void testFieldWithNoValuelistId_noPropertyTargetNotFoundMarker() throws Exception
	{
		String formName = unique("svy21356_noVl");
		Form form = createPlainCssPositionForm(formName);
		form.createNewPart(Part.BODY, 480);

		Field field = form.createNewField(new Point(20, 20));
		field.setName(unique("svy21356_field"));
		// valuelistID left unset entirely

		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "is linked to an entity that does not exist");
		assertTrue("Field with no valuelistID set must not get the dangling-reference marker: " + markers, markers.isEmpty());
	}

	@Test
	public void testValuelistDatabaseValuesType_tracksRelationNamePresence() throws Exception
	{
		ValueList tableVl = solution.createNewValueList(validator, unique("svy21356_tableVl"));
		tableVl.setValueListType(IValueListConstants.TABLE_VALUES);
		ITable table = createMemTableWithColumn(unique("svy21356_vlTable"), "col_a", IColumnTypes.INTEGER);
		tableVl.setDataSource(table.getDataSource());

		assertEquals("With no relationName set, getDatabaseValuesType() must report TABLE_VALUES",
			IValueListConstants.TABLE_VALUES, tableVl.getDatabaseValuesType());

		ITable primaryTable = createMemTableWithColumn(unique("svy21356_relPrimary"), "int_col", IColumnTypes.INTEGER);
		ITable foreignTable = createMemTableWithColumn(unique("svy21356_relForeign"), "int_col", IColumnTypes.INTEGER);
		Relation relation = solution.createNewRelation(validator, unique("svy21356_vlRel"), primaryTable.getDataSource(),
			foreignTable.getDataSource(), IQueryConstants.LEFT_OUTER_JOIN);
		relation.setAllowCreationRelatedRecords(true);
		relation.createNewRelationItems(
			new IDataProvider[] { primaryTable.getColumn("int_col") },
			new int[] { IBaseSQLCondition.EQUALS_OPERATOR },
			new Column[] { (Column)foreignTable.getColumn("int_col") });
		activeProject.saveEditingSolutionNodes(new IPersist[] { relation }, true);

		ValueList relatedVl = solution.createNewValueList(validator, unique("svy21356_relatedVl"));
		relatedVl.setValueListType(IValueListConstants.RELATED_VALUES);
		relatedVl.setRelationName(relation.getName());

		assertEquals("Once relationName is set, getDatabaseValuesType() must report RELATED_VALUES",
			IValueListConstants.RELATED_VALUES, relatedVl.getDatabaseValuesType());
	}

	@Test
	public void testFormWithUnresolvableDataSource_getsInvalidTableMarker() throws Exception
	{
		String formName = unique("svy21356_badDs");
		Form form = solution.createNewForm(validator, null, formName, null, true, new Dimension(640, 480));
		form.setDataSource("totally:bogus/not-a-real-datasource");

		saveAndBuild(form);

		List<IMarker> markers = findMarkersContaining(form, ServoyBuilder.PROJECT_FORM_MARKER_TYPE, "not accessible");
		assertEquals("Expected exactly one formInvalidTable marker for an unresolvable dataSource", 1, markers.size());
		IMarker marker = markers.get(0);
		assertEquals("formInvalidTable marker must be ERROR severity", IMarker.SEVERITY_ERROR, marker.getAttribute(IMarker.SEVERITY, -1));
	}
}
