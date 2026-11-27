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

package com.servoy.eclipse.model.tests.plainjunit.builder;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import com.servoy.eclipse.model.ModelTestAccess;
import com.servoy.eclipse.model.builder.MarkerMessages;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.builder.ServoyBuilderUtils;
import com.servoy.eclipse.model.builder.ServoyMediaBuilder;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.tests.plainjunit.utils.SeverityPreferences;
import com.servoy.eclipse.model.tests.plainjunit.utils.TestPersists;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers;
import com.servoy.eclipse.model.tests.plainjunit.utils.WorkspaceMarkers.RecordedMarker;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.util.UUID;

/**
 * {@link ServoyBuilderUtils} (incremental build routing and the shared persist marker helpers) and {@link ServoyMediaBuilder#checkMedia}.
 * The workspace is {@link WorkspaceMarkers}, severities come from {@link SeverityPreferences}.
 */
class ServoyBuilderUtilsTest
{
	private SeverityPreferences preferences;
	private WorkspaceMarkers workspace;
	private IServoyModel model;
	private FlattenedSolution fs;
	private ISupportChilds root;
	private IFile markerResource;

	@BeforeEach
	void setUp()
	{
		preferences = new SeverityPreferences();
		workspace = new WorkspaceMarkers();
		model = ModelTestAccess.installMockServoyModel();
		fs = mock(FlattenedSolution.class);
		when(model.getFlattenedSolution()).thenReturn(fs);
		ServoyProject activeProject = mock(ServoyProject.class);
		when(activeProject.getProject()).thenReturn(mock(IProject.class));
		when(model.getActiveProject()).thenReturn(activeProject);
		// a parent that is not a Solution: no project scope preferences, no editing flattened solution
		root = mock(ISupportChilds.class);
		when(root.getRootObject()).thenReturn(mock(IRootObject.class));
		markerResource = workspace.file("/forms/f1.frm");
	}

	@AfterEach
	void tearDown()
	{
		ModelTestAccess.reset();
		workspace.close();
		preferences.close();
	}

	private Form form(String name)
	{
		Form form = TestPersists.create(root, IRepository.FORMS, UUID.randomUUID());
		form.setName(name);
		return form;
	}

	private GraphicalComponent button(Form form)
	{
		GraphicalComponent button = TestPersists.create(form, IRepository.GRAPHICALCOMPONENTS, UUID.randomUUID());
		button.setName("btn");
		form.addChild(button);
		return button;
	}

	private ScriptMethod method(ISupportChilds parent, String declaration)
	{
		ScriptMethod method = TestPersists.create(parent, IRepository.METHODS, UUID.randomUUID());
		method.setName("m");
		method.setDeclaration(declaration);
		if (parent instanceof Form) ((Form)parent).addChild(method);
		return method;
	}

	private static ContentSpec.Element element(String name)
	{
		ContentSpec.Element element = mock(ContentSpec.Element.class);
		when(element.getName()).thenReturn(name);
		return element;
	}

	private RecordedMarker single()
	{
		assertEquals(1, workspace.markers().size(), () -> "expected exactly one marker, got " + workspace.messages());
		return workspace.markers().get(0);
	}

	@Nested
	@DisplayName("canBuildIncremental")
	class CanBuildIncremental
	{
		private final IProject project = mock(IProject.class);

		private IFolder folder(String name)
		{
			IFolder folder = mock(IFolder.class);
			when(folder.getName()).thenReturn(name);
			return folder;
		}

		private IFile file(String name, String extension)
		{
			IFile file = mock(IFile.class);
			when(file.getName()).thenReturn(name);
			when(file.getFileExtension()).thenReturn(extension);
			return file;
		}

		@Test
		@DisplayName("form .js and .frm files, media, valuelist and relation files, scope .js files and datasource files build incrementally")
		void incremental()
		{
			assertAll(() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("forms"), file("f1.js", "js")))),
				() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("forms"), file("f1.frm", "frm")))),
				() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("medias"), file("a.png", "png")))),
				() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("valuelists"), file("vl.val", "val")))),
				() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("relations"), file("r.rel", "rel")))),
				() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, file("globals.js", "js")))),
				() -> assertTrue(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("datasources"), folder("db"), file("t.dbi", "dbi")))));
		}

		@Test
		@DisplayName("other files and folders need a full build")
		void notIncremental()
		{
			assertAll(() -> assertFalse(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("forms"), file("f1.less", "less")))),
				() -> assertFalse(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("other"), file("a.png", "png")))),
				() -> assertFalse(ServoyBuilderUtils.canBuildIncremental(List.of(project, file("solution_settings.obj", "obj")))),
				() -> assertFalse(ServoyBuilderUtils.canBuildIncremental(List.of(project, file("noext", null)))),
				() -> assertFalse(ServoyBuilderUtils.canBuildIncremental(List.of(project, folder("other"), folder("db"), file("t.dbi", "dbi")))),
				() -> assertFalse(ServoyBuilderUtils.canBuildIncremental(List.of(project))));
		}

		@Test
		@DisplayName("checkIncrementalBuild returns false for a media/valuelist/relation file whose persist is not in the solution, and for inactive solutions")
		void checkIncrementalBuildUnknownPersist()
		{
			when(project.getName()).thenReturn("sol");
			ServoyProject servoyProject = mock(ServoyProject.class);
			Solution solution = mock(Solution.class);
			doReturn(null).when(solution).getSolutionMetaData();
			when(servoyProject.getSolution()).thenReturn(solution);
			when(model.getServoyProject("sol")).thenReturn(servoyProject);
			when(model.isSolutionActive("sol")).thenReturn(Boolean.TRUE);

			assertAll(() -> assertFalse(ServoyBuilderUtils.checkIncrementalBuild(List.of(project, folder("medias"), file("a.png", "png")))),
				() -> assertFalse(ServoyBuilderUtils.checkIncrementalBuild(List.of(project, folder("valuelists"), file("vl.val", "val")))),
				() -> assertFalse(ServoyBuilderUtils.checkIncrementalBuild(List.of(project, folder("relations"), file("r.rel", "rel")))),
				() -> assertFalse(ServoyBuilderUtils.checkIncrementalBuild(List.of(project, folder("other"), file("a.png", "png")))));

			when(model.isSolutionActive("sol")).thenReturn(Boolean.FALSE);
			assertFalse(ServoyBuilderUtils.checkIncrementalBuild(List.of(project, folder("relations"), file("r.rel", "rel"))));
			verify(fs, times(1)).getMedia("a.png");
		}
	}

	@Nested
	@DisplayName("addNullReferenceMarker")
	class NullReference
	{
		@Test
		@DisplayName("a found persist or the extendsID property gives no marker")
		void noMarker()
		{
			Form f1 = form("f1");
			ServoyBuilderUtils.addNullReferenceMarker(markerResource, f1, f1, null, element("onShowMethodID"));
			ServoyBuilderUtils.addNullReferenceMarker(markerResource, f1, null, null, element("extendsID"));

			assertEquals(List.of(), workspace.markers());
		}

		@Test
		@DisplayName("a nameless/form persist outside a form context: solution problem, warning by default")
		void solutionLevel()
		{
			ServoyBuilderUtils.addNullReferenceMarker(markerResource, form("f1"), null, null, element("dataProviderID"));

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.PropertyTargetNotFound.fill("dataProviderID").getText(), marker.message()),
				() -> assertEquals(ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE, marker.type()),
				() -> assertEquals(Integer.valueOf(IMarker.SEVERITY_WARNING), marker.severity()),
				() -> assertEquals(Integer.valueOf(IMarker.PRIORITY_LOW), marker.priority()));
		}

		@Test
		@DisplayName("a form property in a form context: form problem, error by default")
		void formLevel()
		{
			Form f1 = form("f1");
			ServoyBuilderUtils.addNullReferenceMarker(markerResource, f1, null, f1, element("dataSource"));

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.PropertyInFormTargetNotFound.fill("dataSource", "f1").getText(), marker.message()),
				() -> assertEquals(ServoyBuilder.PROJECT_FORM_MARKER_TYPE, marker.type()),
				() -> assertEquals(Integer.valueOf(IMarker.SEVERITY_ERROR), marker.severity()));
		}

		@Test
		@DisplayName("a named element in a form context names element and form")
		void elementInForm()
		{
			Form f1 = form("f1");
			ServoyBuilderUtils.addNullReferenceMarker(markerResource, button(f1), null, f1, element("dataProviderID"));

			assertEquals(MarkerMessages.PropertyOnElementInFormTargetNotFound.fill("dataProviderID", "btn", "f1").getText(), single().message());
		}

		@Test
		@DisplayName("a named element outside a form context gets uuid, property and display name attributes")
		void elementOutsideForm()
		{
			GraphicalComponent btn = button(form("f1"));

			ServoyBuilderUtils.addNullReferenceMarker(markerResource, btn, null, null, element("dataProviderID"));

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.PropertyOnElementTargetNotFound.fill("dataProviderID", "btn").getText(), marker.message()),
				() -> assertEquals(btn.getUUID().toString(), marker.attributes().get("Uuid")),
				() -> assertEquals("dataProviderID", marker.attributes().get("PropertyName")),
				() -> assertEquals(RepositoryHelper.getDisplayName("dataProviderID", GraphicalComponent.class), marker.attributes().get("DisplayName")),
				// pins current behaviour: the element name is stored as SolutionName
				() -> assertEquals("btn", marker.attributes().get("SolutionName")));
		}

		@Test
		@DisplayName("an event property becomes an invalid event method marker with event name, datasource and context type")
		void eventProperty()
		{
			Form f1 = form("f1");
			f1.setDataSource("db:/example_data/orders");

			ServoyBuilderUtils.addNullReferenceMarker(markerResource, button(f1), null, f1, element("onActionMethodID"));

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(ServoyBuilder.INVALID_EVENT_METHOD, marker.type()),
				() -> assertEquals(MarkerMessages.PropertyOnElementInFormTargetNotFound.fill("onActionMethodID", "btn", "f1").getText(), marker.message()),
				() -> assertEquals("onActionMethodID", marker.attributes().get("EventName")),
				() -> assertEquals("db:/example_data/orders", marker.attributes().get("DataSource")),
				() -> assertEquals(Integer.valueOf(IRepository.FORMS), marker.attributes().get("ContextTypeId")),
				() -> assertEquals(WorkspaceMarkers.PROJECT_NAME, marker.attributes().get("SolutionName")));
		}

		@Test
		@DisplayName("a command property becomes an invalid command method marker")
		void commandProperty()
		{
			Form f1 = form("f1");

			ServoyBuilderUtils.addNullReferenceMarker(markerResource, f1, null, f1, element("onDoubleClickCmdMethodID"));

			assertEquals(ServoyBuilder.INVALID_COMMAND_METHOD, single().type());
		}

		@Test
		@DisplayName("IGNORE suppresses the marker")
		void ignored()
		{
			preferences.set(ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND.getLeft(), "IGNORE");
			Form f1 = form("f1");

			ServoyBuilderUtils.addNullReferenceMarker(markerResource, f1, null, f1, element("dataSource"));

			assertEquals(List.of(), workspace.markers());
		}
	}

	@Nested
	@DisplayName("addNotAccessibleMethodMarkers")
	class NotAccessibleMethod
	{
		@Test
		@DisplayName("no marker for something that is not a method, without context, or for an accessible method")
		void noMarker()
		{
			Form f1 = form("f1");
			ScriptMethod own = method(f1, "function m() {}");
			when(fs.getFormHierarchy(f1)).thenReturn(List.of(f1));

			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, f1, f1, f1, element("onShowMethodID"), fs);
			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, f1, own, null, element("onShowMethodID"), fs);
			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, button(f1), own, f1, element("onActionMethodID"), fs);

			assertEquals(List.of(), workspace.markers());
		}

		@Test
		@DisplayName("a private scope method used outside its parent names the scope")
		void privateScopeMethod()
		{
			ScriptMethod global = method(root, "/** @private */\nfunction m() {}");
			global.setScopeName("globals");

			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, form("f1"), global, mock(IPersist.class), element("onOpenMethodID"), fs);

			assertEquals(MarkerMessages.PropertyTargetNotAccessible.fill("onOpenMethodID", "globals.m").getText(), single().message());
		}

		@Test
		@DisplayName("a private method of another form used on an element names element, form and method")
		void privateFormMethodOnElement()
		{
			Form f1 = form("f1");
			ScriptMethod other = method(form("f2"), "/** @private */\nfunction m() {}");

			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, button(f1), other, f1, element("onActionMethodID"), fs);

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.PropertyOnElementInFormTargetNotAccessible.fill("onActionMethodID", "btn", "f1", "m").getText(),
				marker.message()), () -> assertEquals(Integer.valueOf(IMarker.SEVERITY_ERROR), marker.severity()));
		}

		@Test
		@DisplayName("a public method of a form outside the hierarchy is not accessible")
		void methodOutsideHierarchy()
		{
			Form f1 = form("f1");
			ScriptMethod other = method(form("f2"), "function m() {}");
			when(fs.getFormHierarchy(f1)).thenReturn(List.of(f1));

			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, button(f1), other, f1, element("onActionMethodID"), fs);
			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, f1, other, f1, element("onShowMethodID"), fs);

			assertEquals(List.of(MarkerMessages.FormPropertyOnElementMethodNotAccessible.fill("onActionMethodID", "btn", "f1", "f2").getText(),
				MarkerMessages.FormPropertyMethodNotAccessible.fill("onShowMethodID", "f1", "f2").getText()), workspace.messages());
		}

		@Test
		@DisplayName("a deprecated accessible method is reported as deprecated usage, in a form or solution context")
		void deprecated()
		{
			Form f1 = form("f1");
			ScriptMethod own = method(f1, "/** @deprecated */\nfunction m() {}");
			when(fs.getFormHierarchy(f1)).thenReturn(List.of(f1));
			when(fs.getName()).thenReturn("sol");

			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, f1, own, f1, element("onShowMethodID"), fs);
			ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, f1, own, mock(IPersist.class), element("onOpenMethodID"), fs);

			assertEquals(List.of(MarkerMessages.ElementUsingDeprecatedFunction.fill("m()", "form f1", "onShowMethodID").getText(),
				MarkerMessages.ElementUsingDeprecatedFunction.fill("m()", "solution sol", "onOpenMethodID").getText()), workspace.messages());
		}
	}

	@Nested
	@DisplayName("addMethodParseErrorMarkers")
	class MethodParseErrors
	{
		private final Set<UUID> methodsParsed = new HashSet<>();
		private final Map<IPersist, Boolean> methodsReferences = new HashMap<>();

		@Test
		@DisplayName("an event method without declared parameters that uses arguments[] gets a marker on the line of that use, once")
		void argumentsUsage()
		{
			Form f1 = form("f1");
			ScriptMethod handler = method(f1, "function m() {\n var e = arguments[0];\n}");
			handler.setLineNumberOffset(10);
			ContentSpec.Element onAction = element("onActionMethodID");

			ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, f1, handler, f1, onAction, methodsParsed, methodsReferences);
			ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, f1, handler, f1, onAction, methodsParsed, new HashMap<>());

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.MethodEventParameters.getText(), marker.message()),
				() -> assertEquals(Integer.valueOf(11), marker.attributes().get(IMarker.LINE_NUMBER)),
				() -> assertEquals("onActionMethodID", marker.attributes().get("EventName")),
				() -> assertTrue(methodsParsed.contains(handler.getUUID())));
		}

		@Test
		@DisplayName("onOpenMethodID and methods with declared arguments are not checked for arguments[] use")
		void argumentsUsageSkipped()
		{
			Form f1 = form("f1");
			ScriptMethod handler = method(f1, "function m() { return arguments[0]; }");
			ScriptMethod declared = method(f1, "function m(e) { return arguments[0]; }");
			declared.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, new MethodArgument[] { new MethodArgument("e", ArgumentType.Object, null) });

			ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, f1, handler, f1, element("onOpenMethodID"), methodsParsed, methodsReferences);
			ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, f1, declared, f1, element("onShowMethodID"), methodsParsed, new HashMap<>());

			assertEquals(List.of(), workspace.markers());
		}

		@Test
		@DisplayName("the same method on a second event of a named element is reported once, as info")
		void sameMethodTwice()
		{
			Form f1 = form("f1");
			GraphicalComponent btn = button(f1);
			ScriptMethod handler = method(f1, "function m(e) {}");
			handler.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, new MethodArgument[] { new MethodArgument("e", ArgumentType.Object, null) });

			for (String event : new String[] { "onActionMethodID", "onDoubleClickMethodID", "onRightClickMethodID" })
			{
				ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, btn, handler, f1, element(event), methodsParsed, methodsReferences);
			}

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.PropertyMultipleMethodsOnSameElement.fill("btn").getText(), marker.message()),
				() -> assertEquals(Integer.valueOf(IMarker.SEVERITY_INFO), marker.severity()),
				() -> assertEquals(Boolean.FALSE, methodsReferences.get(handler)));
		}

		@Test
		@DisplayName("instance parameters that do not match the method arguments give a signature mismatch")
		void signatureMismatch()
		{
			Form f1 = form("f1");
			GraphicalComponent btn = button(f1);
			ScriptMethod handler = method(f1, "function m(e) {}");
			handler.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, new MethodArgument[] { new MethodArgument("e", ArgumentType.Object, null) });
			btn.putMethodParameters("onActionMethodID", List.of("e", "extra"), List.of("a", "b"));

			ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, btn, handler, f1, element("onActionMethodID"), methodsParsed, methodsReferences);

			assertEquals(MarkerMessages.EventHandlerSignatureMismatch
				.fill("forms.f1.m", "onAction", RepositoryHelper.getObjectTypeName(IRepository.GRAPHICALCOMPONENTS), " \"btn\"").getText(),
				single().message());
		}

		@Test
		@DisplayName("instance parameters with the method's argument names give no mismatch")
		void signatureMatch()
		{
			Form f1 = form("f1");
			GraphicalComponent btn = button(f1);
			ScriptMethod handler = method(f1, "function m(e) {}");
			handler.setRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS, new MethodArgument[] { new MethodArgument("e", ArgumentType.Object, null) });
			btn.putMethodParameters("onActionMethodID", List.of("e"), List.of("a"));

			ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, btn, handler, f1, element("onActionMethodID"), methodsParsed, methodsReferences);

			assertEquals(List.of(), workspace.markers());
		}
	}

	@Nested
	@DisplayName("other helpers")
	class Others
	{
		@Test
		@DisplayName("addScriptMethodErrorMarkers reports a missing @return with the method's line and uuid")
		void missingReturn()
		{
			ScriptMethod m = method(form("f1"), "function m() { return 1; }");
			m.setLineNumberOffset(7);

			ServoyBuilderUtils.addScriptMethodErrorMarkers(markerResource, m);
			ServoyBuilderUtils.addScriptMethodErrorMarkers(markerResource, method(form("f2"), "function m() { }"));

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.MethodNoReturn.fill("m").getText(), marker.message()),
				() -> assertEquals(Integer.valueOf(7), marker.attributes().get(IMarker.LINE_NUMBER)),
				() -> assertEquals(m.getUUID().toString(), marker.attributes().get("Uuid")));
		}

		@Test
		@DisplayName("formCanBeInstantiated asks the flattened solution once per form")
		void formCanBeInstantiatedIsCached()
		{
			Form f1 = form("f1");
			when(fs.formCanBeInstantiated(f1)).thenReturn(Boolean.TRUE);
			Map<Form, Boolean> checked = new HashMap<>();

			assertAll(() -> assertTrue(ServoyBuilderUtils.formCanBeInstantiated(f1, fs, checked)),
				() -> assertTrue(ServoyBuilderUtils.formCanBeInstantiated(f1, fs, checked)));
			verify(fs, times(1)).formCanBeInstantiated(f1);
		}

		@Test
		@DisplayName("getReferenceFlattenedSolution uses the project's own flattened solution for import hooks, else the model's")
		void referenceFlattenedSolution()
		{
			SolutionMetaData hookMeta = new SolutionMetaData(UUID.randomUUID(), "hook", IRepository.SOLUTIONS, 1, 1);
			hookMeta.setSolutionType(SolutionMetaData.PRE_IMPORT_HOOK);
			Solution hook = mock(Solution.class);
			doReturn(hookMeta).when(hook).getSolutionMetaData();
			doReturn("hook").when(hook).getName();
			FlattenedSolution hookFs = mock(FlattenedSolution.class);
			ServoyProject hookProject = mock(ServoyProject.class);
			when(hookProject.getFlattenedSolution()).thenReturn(hookFs);
			when(model.getServoyProject("hook")).thenReturn(hookProject);
			Solution normal = mock(Solution.class);
			doReturn(new SolutionMetaData(UUID.randomUUID(), "normal", IRepository.SOLUTIONS, 1, 1)).when(normal).getSolutionMetaData();

			assertAll(() -> assertSame(hookFs, ServoyBuilderUtils.getReferenceFlattenedSolution(hook)),
				() -> assertSame(fs, ServoyBuilderUtils.getReferenceFlattenedSolution(normal)));
		}

		@Test
		@DisplayName("a reserved browser window name of a variable is reported in a mobile solution only")
		void mobileReservedWord()
		{
			ScriptVariable location = TestPersists.create(root, IRepository.SCRIPTVARIABLES, UUID.randomUUID());
			location.setName("location");
			ScriptVariable ok = TestPersists.create(root, IRepository.SCRIPTVARIABLES, UUID.randomUUID());
			ok.setName("orderCount");
			Solution solution = mock(Solution.class);
			doReturn(Integer.valueOf(SolutionMetaData.MOBILE)).when(solution).getSolutionType();
			when(fs.getSolution()).thenReturn(solution);
			IProject project = mock(IProject.class);

			ServoyBuilderUtils.addMobileReservedWordsVariable(project, location);
			ServoyBuilderUtils.addMobileReservedWordsVariable(project, ok);
			doReturn(Integer.valueOf(SolutionMetaData.SOLUTION)).when(solution).getSolutionType();
			ServoyBuilderUtils.addMobileReservedWordsVariable(project, location);

			assertEquals(List.of(MarkerMessages.ReservedWindowObjectProperty.fill("location").getText()), workspace.messages());
		}

		@Test
		@DisplayName("an authenticating active solution with ws_* form methods is a web service that must not authenticate")
		void serviceMustAuthenticate()
		{
			Form service = form("service");
			method(service, "function ws_read() {}").setName("ws_read");
			Solution solution = mock(Solution.class);
			doReturn("sol").when(solution).getName();
			doReturn(Boolean.TRUE).when(solution).getMustAuthenticate();
			doReturn(List.of(service)).when(solution).getAllObjectsAsList();
			when(model.getActiveProject().getSolution()).thenReturn(solution);
			IProject project = mock(IProject.class);

			ServoyBuilderUtils.checkServiceSolutionMustAuthenticate(model, solution, project);
			doReturn(Boolean.FALSE).when(solution).getMustAuthenticate();
			ServoyBuilderUtils.checkServiceSolutionMustAuthenticate(model, solution, project);

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.SolutionUsedAsWebServiceMustAuthenticateProblem.fill("sol").getText(), marker.message()),
				() -> assertEquals(Integer.valueOf(IMarker.PRIORITY_HIGH), marker.priority()));
			try
			{
				verify(project, times(2)).deleteMarkers(ServoyBuilder.SERVICE_MUST_AUTHENTICATE_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			}
			catch (Exception e)
			{
				throw new AssertionError(e);
			}
		}

		@Test
		@DisplayName("checkMedia reports TIFF media (any case) with their path, other media not")
		void tiffMedia()
		{
			Media tiff = TestPersists.create(root, IRepository.MEDIA, UUID.randomUUID());
			tiff.setName("scan.TIFF");
			Media png = TestPersists.create(root, IRepository.MEDIA, UUID.randomUUID());
			png.setName("logo.png");

			ServoyMediaBuilder.checkMedia(tiff);
			ServoyMediaBuilder.checkMedia(png);

			RecordedMarker marker = single();
			assertAll(() -> assertEquals(MarkerMessages.MediaTIFF.fill("/medias/scan.TIFF").getText(), marker.message()),
				() -> assertEquals(ServoyBuilder.MEDIA_MARKER_TYPE, marker.type()));
		}
	}
}
