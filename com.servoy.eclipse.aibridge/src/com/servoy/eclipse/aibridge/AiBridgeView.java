package com.servoy.eclipse.aibridge;

import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;

import javax.swing.text.html.HTMLEditorKit;
import javax.swing.text.html.parser.ParserDelegator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
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
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.aibridge.actions.ActivateOnCompletion;
import com.servoy.eclipse.aibridge.actions.DeleteAction;
//import com.servoy.eclipse.aibridge.actions.OpenDualEditorAction;
import com.servoy.eclipse.aibridge.actions.OpenSourceAction;
import com.servoy.eclipse.aibridge.actions.ShowResponseAction;
import com.servoy.eclipse.aibridge.actions.SubmitAgainAction;
import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.tweaks.IconPreferences;


public class AiBridgeView extends ViewPart implements IPersistable, IActiveProjectListener
{

	private SashForm sashForm;
	private static TableViewer viewer;
	private static AiBridgeView currentInstance;
	private final String[] titles = { "Type", "Request", "Response", "Status", "Duration", "Date" };
	private final int[] columnWidths = new int[titles.length];
	private Menu contextMenu;
	private static volatile String solutionName = null;
	private static volatile UUID selectionId = null;

	int[] defaultWidth = { 200, 250, 250, 150, 60, 150 };

	private Action deleteAction = null;
	private Action submitAction = null;
	private ShowResponseAction showResponseAction = null;
//	private final OpenDualEditorAction openDualEditorAction = null;
	private Action activateAction = null;
	private OpenSourceAction openSourceAction = null;

	private Image titleImage = null;

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
			AiBridgeManager.getInstance().loadData(solutionName);
		}

		boolean isDarkTheme = IconPreferences.getInstance().getUseDarkThemeIcons();
		String iconPath = isDarkTheme ? "darkicons/aibridge.png" : "icons/aibridge.png";
		ImageDescriptor imageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", iconPath);
		titleImage = imageDescriptor.createImage();
		setTitleImage(titleImage);
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
		activateAction.setChecked(true);
//		openDualEditorAction = new OpenDualEditorAction();
	}

	private void configureViewer(Composite parent)
	{
		sashForm = new SashForm(parent, SWT.VERTICAL);
		viewer = new TableViewer(sashForm, SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
		viewer.setContentProvider(new ContentProvider()); //keep this line before setInput()
		viewer.setInput(AiBridgeManager.getInstance().getRequestMap());
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
				openSourceAction.run();
			}
		});
	}

	private void setupToolbar()
	{
		IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
		addActionToToolbar(toolbarManager);
	}

	private void addActionToToolbar(IToolBarManager toolbarManager)
	{
		toolbarManager.add(showResponseAction);
//		toolbarManager.add(openDualEditorAction);
		toolbarManager.add(openSourceAction);
		toolbarManager.add(activateAction);
	}

	private void registerListeners()
	{
//		getSite().getPage().addSelectionListener(openDualEditorAction);
		getSite().getPage().addSelectionListener(showResponseAction);
		getSite().getPage().addSelectionListener(openSourceAction);
	}

	private void unregisterListeners()
	{
//		getSite().getPage().removeSelectionListener(openDualEditorAction);
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
			ServoyLog.logError(e);
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
						case 0 -> Optional.ofNullable(completion.getCmdName()).orElse("");
						case 1 -> Optional.ofNullable(completion.getSelection()).orElse("");
						case 2 -> Optional.ofNullable(completion.getResponse()).map(resp -> {
							String message = stripHtmlTags(resp.getResponseMessage());
							return message.length() > 250 ? message.substring(0, 250) : message;
						})
							.orElse("");
						case 3 -> Optional.ofNullable(completion.getStatus()).orElse("");
						case 4 -> formatTimeDifference(completion);
						case 5 -> Optional.ofNullable(completion.getStartTime()).map(currentInstance::formatDate).orElse("");

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

					IWorkbenchPage page = currentInstance.getSite().getPage();
					IWorkbenchPart activePart = page.getActivePart();

					if (!currentInstance.equals(activePart))
					{
						page.bringToTop(currentInstance);
					}
					currentInstance.getSite().getPage().bringToTop(currentInstance);
				}
				currentInstance.showResponseAction.refresh();
			}
			viewer.refresh();
			if (viewer.getTable().getItemCount() > 0)
			{

				IStructuredSelection initialSelection = new StructuredSelection(getViewerElement());
				viewer.setSelection(initialSelection, true);
				//showResonseAction is receive no selectionChangeEvent from Eclipses' SelectionService (??)
				currentInstance.showResponseAction.selectionChanged(currentInstance, initialSelection);
			}
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
		if (currentInstance != null)
		{
			currentInstance.titleImage.dispose();
			currentInstance = null;
		}
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
			AiBridgeManager.getInstance().saveData(activeProject.getSolution().getName());
		return true;
	}

	@Override
	public void activeProjectChanged(ServoyProject activeProject)
	{
		solutionName = activeProject.getSolution().getName();
		AiBridgeManager.getInstance().loadData(solutionName);
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

	public static void setSelectionId(UUID id)
	{
		selectionId = id;
	}

	private static int findViewerIndexByUUID(UUID targetUUID)
	{
		TableItem[] items = viewer.getTable().getItems();
		for (int i = 0; i < items.length; i++)
		{
			Object data = items[i].getData();
			if (data instanceof Completion)
			{
				Completion completion = (Completion)data;
				if (completion.getId() != null && completion.getId().equals(targetUUID))
				{
					return i; // Return the index in the viewer
				}
			}
		}
		return -1; // Return -1 if no matching element is found
	}


	private static Object getViewerElement()
	{
		int index = findViewerIndexByUUID(selectionId);
		if (index != -1)
		{
			return viewer.getElementAt(index);
		}
		if (viewer.getTable().getItemCount() > 0)
		{
			return viewer.getElementAt(0);
		}
		return null;
	}

	public static List<UUID> getAllItemUUIDs()
	{
		List<UUID> uuids = new ArrayList<>();
		TableItem[] items = viewer.getTable().getItems();
		for (TableItem item : items)
		{
			Object data = item.getData();
			if (data instanceof Completion completion)
			{
				uuids.add(completion.getId());
			}
		}
		return uuids;
	}

	public static int getNewestSelectionIndex()
	{
		return viewer.getTable().getSelectionIndices()[0];
	}

	public static void setPostDeleteSelectionId(int deletedIndex)
	{
		if (viewer.getTable().getItemCount() > 0)
		{
			int newIndex = Math.min(deletedIndex, viewer.getTable().getItemCount() - 1);
			UUID selectedUuid = ((Completion)viewer.getTable().getItem(newIndex).getData()).getId();
			setSelectionId(selectedUuid);
			refresh();
		}
	}

	public static UUID getNewSelectionIdAfterDelete(int deletedIndex)
	{
		if (viewer.getTable().getItemCount() > 0)
		{
			int newIndex = Math.min(deletedIndex, viewer.getTable().getItemCount() - 1);
			return ((Completion)viewer.getTable().getItem(newIndex).getData()).getId();
		}
		return null;
	}
}
