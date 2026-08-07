package com.servoy.eclipse.ui;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.ui.IFolderLayout;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.console.IConsoleConstants;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

public class DesignPerspectiveTest {

	private static final String OPENCODE_VIEW_ID = "com.servoy.eclipse.opencode.OpenCodeView";

	private List<String> bottomAddViewCalls;
	private List<String> bottomAddPlaceholderCalls;
	private List<String> rightAddViewCalls;
	private List<String> rightAddPlaceholderCalls;

	private void exercisePerspective() {
		bottomAddViewCalls = new ArrayList<>();
		bottomAddPlaceholderCalls = new ArrayList<>();
		rightAddViewCalls = new ArrayList<>();
		rightAddPlaceholderCalls = new ArrayList<>();

		Map<String, IFolderLayout> folders = new HashMap<>();
		folders.put("left", createFolderStub(new ArrayList<>(), new ArrayList<>()));
		folders.put("rightmost", createFolderStub(new ArrayList<>(), new ArrayList<>()));
		folders.put("right", createFolderStub(rightAddViewCalls, rightAddPlaceholderCalls));
		folders.put("bottom", createFolderStub(bottomAddViewCalls, bottomAddPlaceholderCalls));

		IPageLayout layout = createPageLayoutStub(folders);

		DesignPerspective perspective = new DesignPerspective();
		try {
			perspective.createInitialLayout(layout);
		} catch (Exception e) {
			// PlatformUI may not be fully initialized in test context
		}
	}

	@Test
	@DisplayName("Console view is added as visible view in bottom folder")
	public void consoleViewIsAddedAsView() {
		exercisePerspective();
		assertTrue(bottomAddViewCalls.contains(IConsoleConstants.ID_CONSOLE_VIEW),
				"Console view should be added via addView");
	}

	@Test
	@DisplayName("Console view is not added as placeholder")
	public void consoleViewIsNotPlaceholder() {
		exercisePerspective();
		assertFalse(bottomAddPlaceholderCalls.contains(IConsoleConstants.ID_CONSOLE_VIEW),
				"Console view should not be added as placeholder");
	}

	@Test
	@DisplayName("Servoy AI view is added as visible view in right folder")
	public void aiViewIsAddedAsView() {
		exercisePerspective();
		assertTrue(rightAddViewCalls.contains(OPENCODE_VIEW_ID),
				"Servoy AI view should be added via addView in right folder");
	}

	@Test
	@DisplayName("Servoy AI view is not added as placeholder")
	public void aiViewIsNotPlaceholder() {
		exercisePerspective();
		assertFalse(rightAddPlaceholderCalls.contains(OPENCODE_VIEW_ID),
				"Servoy AI view should not be added as placeholder");
	}

	@Test
	@DisplayName("Bottom folder contains all expected views as tabs")
	public void bottomFolderContainsAllExpectedViews() {
		exercisePerspective();
		assertAll(
				() -> assertTrue(bottomAddViewCalls.contains(IPageLayout.ID_PROBLEM_VIEW),
						"should contain Problems view"),
				() -> assertTrue(bottomAddViewCalls.contains(IConsoleConstants.ID_CONSOLE_VIEW),
						"should contain Console view"),
				() -> assertTrue(bottomAddViewCalls.contains(IPageLayout.ID_TASK_LIST), "should contain Tasks view"),
				() -> assertTrue(bottomAddViewCalls.contains(IPageLayout.ID_BOOKMARKS),
						"should contain Bookmarks view"),
				() -> assertTrue(bottomAddViewCalls.contains("com.servoy.eclipse.debug.scriptingconsole"),
						"should contain Scripting Console"),
				() -> assertTrue(bottomAddViewCalls.contains("org.eclipse.search.ui.views.SearchView"),
						"should contain Search view"));
	}

	private IFolderLayout createFolderStub(List<String> addViewCalls, List<String> addPlaceholderCalls) {
		return (IFolderLayout) Proxy.newProxyInstance(getClass().getClassLoader(),
				new Class<?>[] { IFolderLayout.class }, new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("addView".equals(method.getName()) && args != null && args.length == 1) {
							addViewCalls.add((String) args[0]);
						} else if ("addPlaceholder".equals(method.getName()) && args != null && args.length == 1) {
							addPlaceholderCalls.add((String) args[0]);
						}
						return null;
					}
				});
	}

	private IPageLayout createPageLayoutStub(Map<String, IFolderLayout> folders) {
		return (IPageLayout) Proxy.newProxyInstance(getClass().getClassLoader(), new Class<?>[] { IPageLayout.class },
				new InvocationHandler() {
					@Override
					public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
						if ("getEditorArea".equals(method.getName())) {
							return "editorArea";
						} else if ("createFolder".equals(method.getName()) && args != null && args.length == 4) {
							String folderId = (String) args[0];
							return folders.getOrDefault(folderId,
									createFolderStub(new ArrayList<>(), new ArrayList<>()));
						}
						return null;
					}
				});
	}
}
