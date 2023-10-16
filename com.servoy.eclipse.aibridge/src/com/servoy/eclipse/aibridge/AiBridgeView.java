package com.servoy.eclipse.aibridge;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPersistable;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;

import com.servoy.eclipse.aibridge.actions.ActivateOnCompletion;
import com.servoy.eclipse.aibridge.actions.DeleteAction;
import com.servoy.eclipse.aibridge.actions.OpenDualEditorAction;
import com.servoy.eclipse.aibridge.actions.OpenSourceAction;
import com.servoy.eclipse.aibridge.actions.ShowResponseAction;
import com.servoy.eclipse.aibridge.actions.SubmitAgainAction;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;


public class AiBridgeView extends ViewPart implements IPersistable, IActiveProjectListener
{

	private SashForm sashForm;
	private static TableViewer viewer;
	private static AiBridgeView currentInstance;
	private final String[] titles = { "Id", "Type", "Status", "Date", "Request", "Response", "Duration" };
	private final int[] columnWidths = new int[titles.length];
	private Menu contextMenu;
	private static volatile String solutionName = null;

	int[] defaultWidth = { 60, 100, 100, 150, 300, 300, 100 };

	private Action deleteAction = null;
	private Action submitAction = null;
	private ShowResponseAction showResponseAction = null;
	private OpenDualEditorAction openDualEditorAction = null;
	private Action activateAction = null;
	private OpenSourceAction openSourceAction = null;

	@Override
	public void init(IViewSite site, IMemento memento) throws PartInitException
	{
		super.init(site, memento);
		currentInstance = this;

		for (int i = 0; i < titles.length; i++)
		{
			Integer width = memento != null ? memento.getInteger("COLUMN_WIDTH_" + i) != null ? memento.getInteger("COLUMN_WIDTH_" + i) : defaultWidth[i]
				: defaultWidth[i];
			columnWidths[i] = (width != null && width.intValue() > 0) ? width : defaultWidth[i];
		}

		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		servoyModel.addActiveProjectListener(this);

		if (servoyModel.getActiveProject() != null)
		{
			solutionName = servoyModel.getActiveProject().getSolution().getName();
			AiBridgeManager.loadData(solutionName);
		}
	}

	@Override
	public void createPartControl(Composite parent)
	{
		configureViewer(parent);
		initializeActions(parent);
		createContextMenu();
		setupTableColumns();
		setupTableAppearance();
		setupTableListeners();
		setupToolbar();
		registerListeners();
		AiBridgeView.refresh();
	}

	private void initializeActions(Composite parent)
	{
		deleteAction = new DeleteAction();
		submitAction = new SubmitAgainAction();
		showResponseAction = new ShowResponseAction(sashForm);
		activateAction = new ActivateOnCompletion();
		openSourceAction = new OpenSourceAction();
		openDualEditorAction = new OpenDualEditorAction();
	}

	private void configureViewer(Composite parent)
	{
		sashForm = new SashForm(parent, SWT.VERTICAL);
		viewer = new TableViewer(sashForm, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider()); //keep this line before setInput()
		viewer.setInput(AiBridgeManager.getRequestMap());
		viewer.setComparator(new StartTimeComparator());
		getSite().setSelectionProvider(viewer);
	}

	private void setupTableColumns()
	{
		for (int i = 0; i < titles.length; i++)
		{
			TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
			viewerColumn.getColumn().setText(titles[i]);
			viewerColumn.getColumn().setWidth(i < columnWidths.length ? columnWidths[i] : i < defaultWidth.length ? defaultWidth[i] : 100);
			viewerColumn.getColumn().setResizable(true);
			viewerColumn.getColumn().setMoveable(true);
			viewerColumn.setLabelProvider(getColumnProvider(i));
		}
	}

	private void setupTableAppearance()
	{
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setLinesVisible(true);
	}

	private void setupTableListeners()
	{
		setupTableRowColoring();
		setupTableResizeListener();
		setupDoubleClickAction();
	}

	private void setupTableRowColoring()
	{
		viewer.getTable().addListener(SWT.EraseItem, event -> {
			if ((event.detail & SWT.SELECTED) == 0)
			{
				int index = viewer.getTable().indexOf((TableItem)event.item);
				if ((index % 2) == 0)
				{
					event.gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
					event.detail &= ~SWT.HOT;
					event.detail &= ~SWT.SELECTED;
					event.detail &= ~SWT.FOCUSED;
					event.doit = true;
				}
			}
		});
	}

	private void setupTableResizeListener()
	{
		viewer.getTable().addListener(SWT.Resize, new Listener()
		{
			public void handleEvent(Event e)
			{
				setPartProperty("COLUMN_WIDTH_CHANGED", String.valueOf(System.currentTimeMillis()));
			}
		});
	}

	private void setupDoubleClickAction()
	{
		viewer.addDoubleClickListener(new IDoubleClickListener()
		{
			@Override
			public void doubleClick(DoubleClickEvent event)
			{
				openDualEditorAction.run();
			}
		});
	}

//    private void setupPostSelectionListener() {
//        viewer.addPostSelectionChangedListener(event -> {
//            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
//            Completion selectedCompletion = (Completion) selection.getFirstElement();
//            openSourceAction.processDataForScripEditor(selectedCompletion.getSourcePath());
//        });
//    }

	private void setupToolbar()
	{
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		addActionToToolbar(toolbarManager);
	}

	private void addActionToToolbar(IToolBarManager toolbarManager)
	{
		toolbarManager.add(showResponseAction);
		toolbarManager.add(openDualEditorAction);
		toolbarManager.add(openSourceAction);
		toolbarManager.add(activateAction);
	}

	private void registerListeners()
	{
		getSite().getPage().addSelectionListener(openDualEditorAction);
		getSite().getPage().addSelectionListener(showResponseAction);
		getSite().getPage().addSelectionListener(openSourceAction);
	}

	private void unregisterListeners()
	{
		getSite().getPage().removeSelectionListener(openDualEditorAction);
		getSite().getPage().removeSelectionListener(showResponseAction);
		getSite().getPage().removeSelectionListener(openSourceAction);
	}

	private String stripHtmlTags(String source)
	{
		ParserDelegator delegator = new ParserDelegator();
		final StringBuilder result = new StringBuilder();

		HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback()
		{
			@Override
			public void handleText(char[] data, int pos)
			{
				result.append(data);
			}
		};
		try
		{
			delegator.parse(new java.io.StringReader(source), callback, false);
		}
		catch (java.io.IOException e)
		{
			e.printStackTrace();
		}
		return result.toString();
	}

	private void createContextMenu()
	{
		MenuManager menuManager = new MenuManager("#AiBridgeContextMenu");
		menuManager.add(submitAction);
		menuManager.add(deleteAction);

		contextMenu = menuManager.createContextMenu(viewer.getTable());
		viewer.getTable().addListener(SWT.MouseDown, event -> {
			viewer.getTable().setMenu(getContextMenu()); //no initial menu
		});

	}

	private Menu getContextMenu()
	{
		@SuppressWarnings("unchecked")
		Map<UUID, Completion> input = (Map<UUID, Completion>)viewer.getInput();
		if (input == null || input.isEmpty())
		{
			return null;
		}
		else
		{
			IStructuredSelection selection = viewer.getStructuredSelection();
			if (selection.isEmpty())
			{
				return null;
			}
			return contextMenu;
		}
	}

	public static IStructuredSelection getSelection()
	{
		return viewer.getStructuredSelection();
	}

	private ColumnLabelProvider getColumnProvider(final int columnIndex)
	{
		return new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if (element instanceof Completion completion)
				{
					return switch (columnIndex)
					{
						case 0 -> Optional.ofNullable(completion.getId())
							.map(uuid -> uuid.toString().replaceAll("(.{2}).{6}-(.{2}).{2}-(.{2}).{2}-(.{2}).*", "$1$2$3$4")).orElse("");
						case 1 -> Optional.ofNullable(completion.getCmdName()).orElse("");
						case 2 -> Optional.ofNullable(completion.getStatus()).orElse("");
						case 3 -> Optional.ofNullable(completion.getStartTime()).map(currentInstance::formatDate).orElse("");
						case 4 -> Optional.ofNullable(completion.getSelection()).orElse("");
						case 5 -> Optional.ofNullable(completion.getResponse()).map(resp -> stripHtmlTags(resp.getResponseMessage())).orElse("");
						case 6 -> formatTimeDifference(completion);
						default -> "";
					};
				}
				return super.getText(element);
			}
		};
	}

	private String formatDate(Date date)
	{
		return date.toInstant()
			.atZone(ZoneId.systemDefault())
			.toLocalDateTime()
			.format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
	}

	private String formatTimeDifference(Completion completion)
	{
		if (completion.getEndTime() != null && completion.getStartTime() != null)
		{
			double differenceInSeconds = (double)(completion.getEndTime().getTime() - completion.getStartTime().getTime()) / 1000;
			return String.format("%.2f s", Math.round(differenceInSeconds * 100.0) / 100.0);
		}
		return "";
	}


	@Override
	public void setFocus()
	{
		viewer.getControl().setFocus();
	}

	public static void refresh()
	{
		if (viewer == null || viewer.getControl().isDisposed()) return;

		Display.getDefault().asyncExec(() -> {
			if (currentInstance != null)
			{
				if (currentInstance.activateAction.isChecked())
				{
					currentInstance.getSite().getPage().bringToTop(currentInstance);
				}
				currentInstance.showResponseAction.refresh();
			}
			viewer.refresh();
		});
	}

	@Override
	public void saveState(IMemento memento)
	{
		TableColumn[] columns = viewer.getTable().getColumns();
		for (int i = 0; i < columns.length; i++)
		{
			Integer width = columns[i].getWidth();
			memento.putInteger("COLUMN_WIDTH_" + i, width);
		}
	}

	@Override
	public void dispose()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
		unregisterListeners();
		super.dispose();
	}


	class ContentProvider implements IStructuredContentProvider
	{
		@Override
		public Object[] getElements(Object inputElement)
		{
			Object result[] = ((ConcurrentMap< ? , ? >)inputElement).values().toArray();
			return result;
		}
	}

	public class StartTimeComparator extends ViewerComparator
	{
		@Override
		public int compare(Viewer viewer, Object e1, Object e2)
		{
			if (e1 instanceof Completion && e2 instanceof Completion)
			{
				Completion c1 = (Completion)e1;
				Completion c2 = (Completion)e2;
				return c2.getStartTime().compareTo(c1.getStartTime());
			}
			return super.compare(viewer, e1, e2);
		}
	}

	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject)
	{
		if (activeProject != null)
			AiBridgeManager.saveData(activeProject.getSolution().getName());
		return true;
	}

	@Override
	public void activeProjectChanged(ServoyProject activeProject)
	{
		solutionName = activeProject.getSolution().getName();
		AiBridgeManager.loadData(solutionName);
		AiBridgeView.refresh();
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
	{
	}

	public static String getSolutionName()
	{
		return solutionName;
	}
}
