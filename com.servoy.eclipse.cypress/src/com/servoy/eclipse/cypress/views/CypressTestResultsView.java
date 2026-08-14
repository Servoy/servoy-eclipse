package com.servoy.eclipse.cypress.views;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.eclipse.core.filesystem.EFS;
import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.swt.widgets.Menu;

import com.servoy.eclipse.cypress.actions.RunSingleTestHandler;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Text;
import org.eclipse.swt.widgets.TableColumn;

import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Table;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.IDE;
import org.eclipse.ui.part.ViewPart;

import com.servoy.eclipse.cypress.actions.CypressTestResult;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestStatus;
import com.servoy.eclipse.cypress.actions.CypressTestResult.TestType;
import com.servoy.eclipse.cypress.actions.CypressTestSessionManager;
import com.servoy.eclipse.cypress.actions.RunAllCypressFormTestsHandler;
import com.servoy.eclipse.cypress.actions.RunAllE2ETestsHandler;

public class CypressTestResultsView extends ViewPart implements PropertyChangeListener {
	public static final String VIEW_ID = "com.servoy.eclipse.cypress.views.cypressTestResults";

	private TableViewer tableViewer;
	private Canvas progressCanvas;
	private int progressMax = 1;
	private int progressSelection = 0;
	private Color progressColor;
	private Label countersLabel;
	private StyledText detailText;
	private Action filterFailuresAction;
	private boolean filterFailuresOnly;
	private CypressTestSessionManager sessionManager;
	private Action rerunAction;
	private Color passedBg;
	private Color failedBg;
	private Color errorBg;
	private String nameFilterText = "";
	private final ViewerFilter nameFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			CypressTestResult r = (CypressTestResult) element;
			return r.testName().toLowerCase().contains(nameFilterText);
		}
	};


	@Override
	public void createPartControl(Composite parent) {
		sessionManager = CypressTestSessionManager.getInstance();
		sessionManager.addPropertyChangeListener(this);

		progressColor = parent.getDisplay().getSystemColor(SWT.COLOR_DARK_GREEN);

		// Cache row background colors (disposed in dispose())
		passedBg = new Color(parent.getDisplay(), 232, 255, 232); // subtle green
		failedBg = new Color(parent.getDisplay(), 232, 240, 255); // subtle blue
		errorBg = new Color(parent.getDisplay(), 255, 232, 232); // subtle red

		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		container.setLayout(layout);

		Composite headerComposite = new Composite(container, SWT.NONE);
		headerComposite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		GridLayout headerLayout = new GridLayout(2, false);
		headerLayout.marginHeight = 4;
		headerLayout.marginWidth = 4;
		headerComposite.setLayout(headerLayout);

		countersLabel = new Label(headerComposite, SWT.NONE);
		countersLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		countersLabel.setText("Runs: 0/0, Failures: 0, Errors: 0");

		progressCanvas = new Canvas(headerComposite, SWT.BORDER);
		GridData progressData = new GridData(SWT.FILL, SWT.CENTER, true, false);
		progressData.widthHint = 200;
		progressData.heightHint = 16;
		progressCanvas.setLayoutData(progressData);
		progressCanvas.addPaintListener(e -> paintProgressBar(e.gc));

		// Filter text box
		Text filterText = new Text(container, SWT.BORDER | SWT.SEARCH | SWT.ICON_SEARCH | SWT.ICON_CANCEL);
		filterText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
		filterText.setMessage("Filter tests...");
		filterText.addModifyListener(e -> {
			String text = filterText.getText().trim().toLowerCase();
			if (text.isEmpty()) {
				tableViewer.removeFilter(nameFilter);
			} else {
				nameFilterText = text;
				tableViewer.removeFilter(nameFilter);
				tableViewer.addFilter(nameFilter);
			}
		});


		SashForm sashForm = new SashForm(container, SWT.VERTICAL);
		sashForm.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		createTableViewer(sashForm);

		detailText = new StyledText(sashForm, SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL | SWT.READ_ONLY);
		detailText.setFont(parent.getDisplay().getSystemFont());

		sashForm.setWeights(new int[] { 70, 30 });

		createToolbar();
		refreshView();
	}

	private void paintProgressBar(GC gc) {
		Rectangle bounds = progressCanvas.getClientArea();
		gc.setBackground(progressCanvas.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		gc.fillRectangle(bounds);

		if (progressMax > 0 && progressSelection > 0) {
			int fillWidth = (int) ((double) progressSelection / progressMax * bounds.width);
			gc.setBackground(progressColor);
			gc.fillRectangle(0, 0, fillWidth, bounds.height);
		}
	}

	private void createTableViewer(Composite parent) {
		tableViewer = new TableViewer(parent, SWT.FULL_SELECTION | SWT.BORDER | SWT.SINGLE);
		Table table = tableViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		TableViewerColumn statusCol = new TableViewerColumn(tableViewer, SWT.NONE);
		statusCol.getColumn().setText("");
		statusCol.getColumn().setWidth(30);
		statusCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				CypressTestResult r = (CypressTestResult) element;
				return switch (r.status()) {
				case PENDING -> "\u25CB";
				case RUNNING -> "\u27F3";
				case PASSED -> "\u2713";
				case FAILED -> "\u2717";
				case ERROR -> "\u2717";
				};
			}

			@Override
			public Color getForeground(Object element) {
				CypressTestResult r = (CypressTestResult) element;
				Display display = tableViewer.getTable().getDisplay();
				return switch (r.status()) {
				case PASSED -> display.getSystemColor(SWT.COLOR_DARK_GREEN);
				case FAILED -> display.getSystemColor(SWT.COLOR_BLUE);
				case ERROR -> display.getSystemColor(SWT.COLOR_RED);
				default -> null;
				};
			}
		});

		TableViewerColumn nameCol = new TableViewerColumn(tableViewer, SWT.NONE);
		nameCol.getColumn().setText("Test");
		nameCol.getColumn().setWidth(300);
		nameCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((CypressTestResult) element).testName();
			}

			@Override
			public Color getBackground(Object element) {
				return getRowBackground((CypressTestResult) element);
			}
		});

		TableViewerColumn typeCol = new TableViewerColumn(tableViewer, SWT.NONE);
		typeCol.getColumn().setText("Type");
		typeCol.getColumn().setWidth(60);
		typeCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((CypressTestResult) element).testType().name();
			}
		});

		TableViewerColumn durationCol = new TableViewerColumn(tableViewer, SWT.NONE);
		durationCol.getColumn().setText("Duration");
		durationCol.getColumn().setWidth(80);
		durationCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				CypressTestResult r = (CypressTestResult) element;
				if (r.durationMs() <= 0)
					return "\u2014";
				return String.format("%.1fs", r.durationMs() / 1000.0);
			}
		});

		// Column sorting
		addColumnSorter(nameCol.getColumn(), 0);
		addColumnSorter(typeCol.getColumn(), 1);
		addColumnSorter(durationCol.getColumn(), 2);
		addColumnSorter(statusCol.getColumn(), 3);


		TableViewerColumn videoCol = new TableViewerColumn(tableViewer, SWT.CENTER);
		videoCol.getColumn().setText("Video");
		videoCol.getColumn().setWidth(55);
		videoCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((CypressTestResult) element).hasVideo() ? "\u25B6 play" : "";
			}

			@Override
			public Color getForeground(Object element) {
				return ((CypressTestResult) element).hasVideo()
						? tableViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND) : null;
			}

			@Override
			public Color getBackground(Object element) {
				return getRowBackground((CypressTestResult) element);
			}
		});

		TableViewerColumn shotCol = new TableViewerColumn(tableViewer, SWT.CENTER);
		shotCol.getColumn().setText("Screenshot");
		shotCol.getColumn().setWidth(75);
		shotCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return ((CypressTestResult) element).hasScreenshot() ? "\u25A6 view" : "";
			}

			@Override
			public Color getForeground(Object element) {
				return ((CypressTestResult) element).hasScreenshot()
						? tableViewer.getTable().getDisplay().getSystemColor(SWT.COLOR_LINK_FOREGROUND) : null;
			}

			@Override
			public Color getBackground(Object element) {
				return getRowBackground((CypressTestResult) element);
			}
		});
		// Column indices: 0=status,1=name,2=type,3=duration,4=video,5=shot
		final int VIDEO_COL_INDEX = 4;
		final int SHOT_COL_INDEX = 5;
		tableViewer.getTable().addListener(SWT.MouseDown, e -> {
			org.eclipse.swt.graphics.Point pt = new org.eclipse.swt.graphics.Point(e.x, e.y);
			org.eclipse.swt.widgets.TableItem item = tableViewer.getTable().getItem(pt);
			if (item == null)
				return;
			CypressTestResult r = (CypressTestResult) item.getData();
			for (int col = 0; col < tableViewer.getTable().getColumnCount(); col++) {
				if (item.getBounds(col).contains(pt)) {
					if (col == VIDEO_COL_INDEX && r.hasVideo()) {
						openMediaInBrowser(r.videoPath(), r.testName() + " - video");
					} else if (col == SHOT_COL_INDEX && r.hasScreenshot()) {
						openMediaInBrowser(r.screenshotPath(), r.testName() + " - screenshot");
					}
					break;
				}
			}
		});


		TableViewerColumn errorCol = new TableViewerColumn(tableViewer, SWT.NONE);
		errorCol.getColumn().setText("Error");
		errorCol.getColumn().setWidth(400);
		errorCol.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				CypressTestResult r = (CypressTestResult) element;
				return r.errorSummary() != null ? r.errorSummary() : "";
			}
		});

		tableViewer.setContentProvider(ArrayContentProvider.getInstance());

		tableViewer.addSelectionChangedListener(event -> {
			IStructuredSelection sel = event.getStructuredSelection();
			if (sel.isEmpty()) {
				detailText.setText("");
				return;
			}
			CypressTestResult r = (CypressTestResult) sel.getFirstElement();
			detailText.setText(r.rawOutput() != null ? r.rawOutput() : "");
		});

		tableViewer.getTable().addListener(SWT.MouseDoubleClick, e -> {
			IStructuredSelection sel = tableViewer.getStructuredSelection();
			if (!sel.isEmpty()) {
				CypressTestResult r = (CypressTestResult) sel.getFirstElement();
				openTestFile(r);
			}
		});

		// Context menu
		MenuManager menuMgr = new MenuManager();
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(manager -> {
			IStructuredSelection sel = tableViewer.getStructuredSelection();
			if (sel.isEmpty()) return;
			CypressTestResult r = (CypressTestResult) sel.getFirstElement();

			manager.add(new Action("Re-run Test") {
				@Override
				public void run() {
					new RunSingleTestHandler().execute(r.testName(), r.testType());
				}
			});
			manager.add(new Action("Open Test File") {
				@Override
				public void run() {
					openTestFile(r);
				}
			});
			if (r.errorSummary() != null && !r.errorSummary().isEmpty()) {
				manager.add(new Separator());
				manager.add(new Action("Copy Error to Clipboard") {
					@Override
					public void run() {
						org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(Display.getCurrent());
						clipboard.setContents(new Object[] { r.errorSummary() },
								new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
						clipboard.dispose();
					}
				});
			}
			if (r.rawOutput() != null && !r.rawOutput().isEmpty()) {
				manager.add(new Action("Copy Full Output to Clipboard") {
					@Override
					public void run() {
						org.eclipse.swt.dnd.Clipboard clipboard = new org.eclipse.swt.dnd.Clipboard(Display.getCurrent());
						clipboard.setContents(new Object[] { r.rawOutput() },
								new org.eclipse.swt.dnd.Transfer[] { org.eclipse.swt.dnd.TextTransfer.getInstance() });
						clipboard.dispose();
					}
				});
			}
				// Open Video/Screenshot if available
				if (r.hasVideo()) {
					manager.add(new Separator());
					manager.add(new Action("Open Video") {
						@Override
						public void run() {
							openMediaInBrowser(r.videoPath(), r.testName() + " - video");
						}
					});
				}
				if (r.hasScreenshot()) {
					if (!r.hasVideo()) manager.add(new Separator());
					manager.add(new Action("Open Screenshot") {
						@Override
						public void run() {
							openMediaInBrowser(r.screenshotPath(), r.testName() + " - screenshot");
						}
					});
				}
		});
		Menu menu = menuMgr.createContextMenu(tableViewer.getControl());
		tableViewer.getControl().setMenu(menu);
	}


	private void createToolbar() {
		IToolBarManager toolbar = getViewSite().getActionBars().getToolBarManager();
		ISharedImages sharedImages = PlatformUI.getWorkbench().getSharedImages();

		rerunAction = new Action("Re-run Last") {
			@Override
			public void run() {
				List<CypressTestResult> results = sessionManager.getResults();
				if (results.isEmpty())
					return;
				TestType type = results.get(0).testType();
				List<String> testNames = results.stream().map(CypressTestResult::testName).toList();
				if (testNames.size() == 1) {
					new RunSingleTestHandler().execute(testNames.get(0), type);
				} else if (type == TestType.FORM) {
					new RunAllCypressFormTestsHandler().runFormTests(testNames);
				} else {
					new RunAllE2ETestsHandler(testNames).execute();
				}
			}
		};
		rerunAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
		rerunAction.setToolTipText("Re-run the last test session");
		rerunAction.setEnabled(!sessionManager.getResults().isEmpty());
		toolbar.add(rerunAction);

		Action rerunFailedAction = new Action("Re-run Failed") {
			@Override
			public void run() {
				List<CypressTestResult> results = sessionManager.getResults();
				List<CypressTestResult> failed = results.stream()
						.filter(r -> r.status() == TestStatus.FAILED || r.status() == TestStatus.ERROR)
						.toList();
				if (failed.isEmpty())
					return;
				TestType type = failed.get(0).testType();
				List<String> failedNames = failed.stream().map(CypressTestResult::testName).toList();
				if (failedNames.size() == 1) {
					new RunSingleTestHandler().execute(failedNames.get(0), type);
				} else if (type == TestType.FORM) {
					new RunAllCypressFormTestsHandler().runFormTests(failedNames);
				} else {
					new RunAllE2ETestsHandler(failedNames).execute();
				}
			}
		};
		rerunFailedAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_REDO));
		rerunFailedAction.setToolTipText("Re-run only failed/errored tests");
		toolbar.add(rerunFailedAction);

		Action runAllFormAction = new Action("Run All Form Tests") {
			@Override
			public void run() {
				new RunAllCypressFormTestsHandler().runAllFormTests();
			}
		};
		runAllFormAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		runAllFormAction.setToolTipText("Run all Cypress form tests");
		toolbar.add(runAllFormAction);

		Action runAllE2EAction = new Action("Run All E2E Tests") {
			@Override
			public void run() {
				new RunAllE2ETestsHandler().execute();
			}
		};
		runAllE2EAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_FORWARD));
		runAllE2EAction.setToolTipText("Run all Cypress E2E tests");
		toolbar.add(runAllE2EAction);

		toolbar.add(new Separator());

		Action stopAction = new Action("Stop") {
			@Override
			public void run() {
				sessionManager.stop();
			}
		};
		stopAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_STOP));
		stopAction.setToolTipText("Stop the current test run");
		toolbar.add(stopAction);

		Action clearAction = new Action("Clear") {
			@Override
			public void run() {
				sessionManager.clear();
				refreshView();
			}
		};
		clearAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_ELCL_REMOVEALL));
		clearAction.setToolTipText("Clear test results");
		toolbar.add(clearAction);

		toolbar.add(new Separator());

		filterFailuresAction = new Action("Show Failures Only", Action.AS_CHECK_BOX) {
			@Override
			public void run() {
				filterFailuresOnly = isChecked();
				if (filterFailuresOnly) {
					tableViewer.addFilter(failuresFilter);
				} else {
					tableViewer.removeFilter(failuresFilter);
				}
			}
		};
		filterFailuresAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_OBJS_WARN_TSK));
		filterFailuresAction.setToolTipText("Show only failed and errored tests");
		toolbar.add(filterFailuresAction);

		toolbar.add(new Separator());

		Action historyAction = new Action("Test Run History...") {
			@Override
			public void run() {
				showHistoryDialog();
			}
		};
		historyAction.setImageDescriptor(sharedImages.getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		historyAction.setToolTipText("Test Run History...");
		toolbar.add(historyAction);
	}

	private final ViewerFilter failuresFilter = new ViewerFilter() {
		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			CypressTestResult r = (CypressTestResult) element;
			return r.status() == TestStatus.FAILED || r.status() == TestStatus.ERROR;
		}
	};

	@Override
	public void propertyChange(PropertyChangeEvent evt) {
		Display display = Display.getDefault();
		if (display.isDisposed())
			return;
		display.asyncExec(this::refreshView);
	}

	private void refreshView() {
		if (tableViewer == null || tableViewer.getControl().isDisposed())
			return;

		List<CypressTestResult> results = sessionManager.getResults();
		tableViewer.setInput(results);

		int total = sessionManager.getTotalCount();
		int completed = sessionManager.getCompletedCount();
		int failures = sessionManager.getFailureCount();
		int errors = sessionManager.getErrorCount();

		countersLabel.setText("Runs: " + completed + "/" + total + ", Failures: " + failures + ", Errors: " + errors);

		progressMax = Math.max(total, 1);
		progressSelection = completed;
		updateProgressBarColor(failures, errors);
		progressCanvas.redraw();

		// Update tab title with progress
		if (total > 0 && completed < total) {
			setContentDescription("(" + completed + "/" + total + ")");
		} else if (total > 0) {
			setContentDescription(failures + errors > 0 ? "(" + (failures + errors) + " failed)" : "(all passed)");
		} else {
			setContentDescription("");
		}

		if (rerunAction != null) {
			rerunAction.setEnabled(!results.isEmpty());
		}
	}

	private void updateProgressBarColor(int failures, int errors) {
		Display display = progressCanvas.getDisplay();
		if (errors > 0) {
			progressColor = display.getSystemColor(SWT.COLOR_RED);
		} else if (failures > 0) {
			progressColor = display.getSystemColor(SWT.COLOR_BLUE);
		} else {
			progressColor = display.getSystemColor(SWT.COLOR_DARK_GREEN);
		}
	}

	private void openTestFile(CypressTestResult result) {
		Path workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation().toFile().toPath();
		Path testFile = resolveTestFile(workspaceRoot, result);
		if (testFile == null || !Files.exists(testFile))
			return;

		IFileStore fileStore = EFS.getLocalFileSystem().getStore(testFile.toUri());
		IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
		try {
			IDE.openEditorOnFileStore(page, fileStore);
		} catch (PartInitException e) {
			// ignore
		}
	}

	private Path resolveTestFile(Path workspaceRoot, CypressTestResult result) {
		Path base = workspaceRoot.resolve("jenkins-custom").resolve("e2e-test-scripts").resolve("cypress");
		if (result.testType() == TestType.FORM) {
			return base.resolve("cy-form").resolve(result.testName() + ".spec.cy.js");
		}
		Path e2eDir = base.resolve("e2e");
		Path direct = e2eDir.resolve(result.testName() + ".cy.js");
		if (Files.exists(direct))
			return direct;
		Path directTs = e2eDir.resolve(result.testName() + ".cy.ts");
		if (Files.exists(directTs))
			return directTs;
		try (Stream<Path> walk = Files.walk(e2eDir)) {
			return walk.filter(p -> {
				String name = p.getFileName().toString();
				return name.equals(result.testName() + ".cy.js") || name.equals(result.testName() + ".cy.ts");
			}).findFirst().orElse(null);
		} catch (IOException e) {
			return null;
		}
	}

	public static void reveal() {
		Display.getDefault().asyncExec(() -> {
			try {
				IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();
				page.showView(VIEW_ID);
			} catch (PartInitException e) {
				// ignore
			}
		});
	}

	@Override
	public void setFocus() {
		if (tableViewer != null && !tableViewer.getControl().isDisposed()) {
			tableViewer.getControl().setFocus();
		}
	}

	private void addColumnSorter(TableColumn column, int columnIndex) {
		column.addSelectionListener(new SelectionAdapter() {
			@Override
			public void widgetSelected(SelectionEvent e) {
				Table table = tableViewer.getTable();
				int dir = (table.getSortColumn() == column && table.getSortDirection() == SWT.UP) ? SWT.DOWN : SWT.UP;
				table.setSortColumn(column);
				table.setSortDirection(dir);
				tableViewer.setComparator(new ViewerComparator() {
					@Override
					public int compare(Viewer viewer, Object e1, Object e2) {
						CypressTestResult r1 = (CypressTestResult) e1;
						CypressTestResult r2 = (CypressTestResult) e2;
						int cmp = switch (columnIndex) {
						case 0 -> r1.testName().compareToIgnoreCase(r2.testName());
						case 1 -> r1.testType().name().compareTo(r2.testType().name());
						case 2 -> Long.compare(r1.durationMs(), r2.durationMs());
						case 3 -> r1.status().ordinal() - r2.status().ordinal();
						default -> 0;
						};
						return dir == SWT.DOWN ? -cmp : cmp;
					}
				});
			}
		});
	}



	private void showHistoryDialog() {
		List<CypressTestSessionManager.HistoryEntry> history = sessionManager.getHistory();
		if (history.isEmpty()) {
			org.eclipse.jface.dialogs.MessageDialog.openInformation(
					getSite().getShell(), "Test Run History", "No previous test runs recorded.");
			return;
		}
		String[] items = history.stream().map(CypressTestSessionManager.HistoryEntry::toString).toArray(String[]::new);
		org.eclipse.jface.dialogs.InputDialog dlg = null;
		org.eclipse.ui.dialogs.ListDialog listDialog = new org.eclipse.ui.dialogs.ListDialog(getSite().getShell());
		listDialog.setTitle("Test Run History");
		listDialog.setMessage("Select a previous test run to view:");
		listDialog.setContentProvider(ArrayContentProvider.getInstance());
		listDialog.setLabelProvider(new org.eclipse.jface.viewers.LabelProvider());
		listDialog.setInput(items);
		if (listDialog.open() == org.eclipse.jface.window.Window.OK) {
			Object[] result = listDialog.getResult();
			if (result != null && result.length > 0) {
				String selected = (String) result[0];
				for (int i = 0; i < items.length; i++) {
					if (items[i].equals(selected)) {
						sessionManager.restoreFromHistory(history.get(i));
						break;
					}
				}
			}
		}
	}

	private Color getRowBackground(CypressTestResult r) {
		return switch (r.status()) {
		case PASSED -> passedBg;
		case FAILED -> failedBg;
		case ERROR -> errorBg;
		default -> null;
		};
	}

	/**
	 * Opens a media file (video or screenshot) in the Eclipse internal browser, which can
	 * render both .mp4 and .png. Falls back to the OS default application if the browser
	 * can't be opened.
	 */
	private void openMediaInBrowser(String path, String title) {
		if (path == null)
			return;
		java.io.File file = new java.io.File(path);
		if (!file.exists())
			return;
		try {
			org.eclipse.ui.browser.IWorkbenchBrowserSupport support = PlatformUI.getWorkbench().getBrowserSupport();
			org.eclipse.ui.browser.IWebBrowser browser = support.createBrowser(
					org.eclipse.ui.browser.IWorkbenchBrowserSupport.AS_EDITOR
							| org.eclipse.ui.browser.IWorkbenchBrowserSupport.LOCATION_BAR,
					"cypressMedia", title, path);
			browser.openURL(file.toURI().toURL());
		} catch (Exception e) {
			// Fall back to the OS default application
			try {
				java.awt.Desktop.getDesktop().open(file);
			} catch (Exception ignored) {
				// ignore
			}
		}
	}

	@Override
	public void dispose() {
		if (sessionManager != null) {
			sessionManager.removePropertyChangeListener(this);
		}
		if (passedBg != null) passedBg.dispose();
		if (failedBg != null) failedBg.dispose();
		if (errorBg != null) errorBg.dispose();
		super.dispose();
	}
}

