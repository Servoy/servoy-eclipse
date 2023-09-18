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
import com.servoy.eclipse.aibridge.actions.SplitHViewAction;
import com.servoy.eclipse.aibridge.actions.SubmitAgainAction;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.actions.OpenSourceAction;


public class AiBridgeView extends ViewPart implements IPersistable, IActiveProjectListener {

    private static TableViewer viewer;
    private static AiBridgeView currentInstance;
    private String[] titles = { "Type", "Status", "Date", "Request", "Response", "Duration"};
    private int[] columnWidths = new int[titles.length];
    private Menu contextMenu;
    private static volatile String solutionName = null;
    
    int[] defaultWidth = { 100, 100, 150, 300, 300, 100};
    
    private Action deleteAction = null;
    private Action submitAction = null;
    private SplitHViewAction hSplitAction = null;
    private Action bringToFrontAction = null;
    private OpenSourceAction openSourceAction = null;
    
    @Override
    public void init(IViewSite site, IMemento memento) throws PartInitException {
        super.init(site, memento);
        currentInstance = this;
        if (memento == null) return;

        for (int i = 0; i < titles.length; i++) {
            Integer width = memento.getInteger("COLUMN_WIDTH_" + i);
            columnWidths[i] = width != null ? width : (i < defaultWidth.length ? defaultWidth[i] : 100);
        }
        
        IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel(); 
        servoyModel.addActiveProjectListener(this);
        
        if (servoyModel.getActiveProject() != null) {
        	solutionName = servoyModel.getActiveProject().getSolution().getName();
        	AiBridgeManager.loadData(solutionName);
        }
    }
    
    @Override
    public void createPartControl(Composite parent) {
        initializeActions(parent);
        configureViewer(parent);
        setupTableColumns();
        setupTableAppearance();
        setupTableListeners();
        setupToolbar();
        AiBridgeView.refresh();
    }

    private void initializeActions(Composite parent) {
        deleteAction = new DeleteAction();
        submitAction = new SubmitAgainAction();
        hSplitAction = new SplitHViewAction(parent);
        bringToFrontAction = new ActivateOnCompletion();
        openSourceAction = new OpenSourceAction();
    }

    private void configureViewer(Composite parent) {
        viewer = new TableViewer(hSplitAction.getSashForm(), SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION | SWT.BORDER);
        viewer.setContentProvider(new ContentProvider()); //keep this line before setInput()
        viewer.setInput(AiBridgeManager.getRequestMap());
        viewer.setComparator(new StartTimeComparator());
        hSplitAction.initializeHtmlViewer();
        createContextMenu();
    }

    private void setupTableColumns() {
        for (int i = 0; i < titles.length; i++) {
            TableViewerColumn viewerColumn = new TableViewerColumn(viewer, SWT.NONE);
            viewerColumn.getColumn().setText(titles[i]);
            viewerColumn.getColumn().setWidth(i < columnWidths.length ? columnWidths[i] : i < defaultWidth.length ? defaultWidth[i] : 100);
            viewerColumn.getColumn().setResizable(true);
            viewerColumn.getColumn().setMoveable(true);
            viewerColumn.setLabelProvider(getColumnProvider(i));
        }
    }

    private void setupTableAppearance() {
        viewer.getTable().setHeaderVisible(true);
        viewer.getTable().setLinesVisible(true);
    }

    private void setupTableListeners() {
        setupTableRowColoring();
        setupTableResizeListener();
        setupDoubleClickAction();
        setupPostSelectionListener();
    }

    private void setupTableRowColoring() {
        viewer.getTable().addListener(SWT.EraseItem, event -> {
            if ((event.detail & SWT.SELECTED) == 0) {
                int index = viewer.getTable().indexOf((TableItem) event.item);
                if ((index % 2) == 0) {
                    event.gc.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_GRAY));
                    event.detail &= ~SWT.HOT;
                    event.detail &= ~SWT.SELECTED;
                    event.detail &= ~SWT.FOCUSED;
                    event.doit = true;
                }
            }
        });
    }

    private void setupTableResizeListener() {
        viewer.getTable().addListener(SWT.Resize, new Listener() {
            public void handleEvent(Event e) {
                setPartProperty("COLUMN_WIDTH_CHANGED", String.valueOf(System.currentTimeMillis()));
            }
        });
    }

    private void setupDoubleClickAction() {
        viewer.addDoubleClickListener(new IDoubleClickListener() {
            @Override
            public void doubleClick(DoubleClickEvent event) {
                openSourceAction.run();
            }
        });
    }

    private void setupPostSelectionListener() {
        viewer.addPostSelectionChangedListener(event -> {
            IStructuredSelection selection = (IStructuredSelection) event.getSelection();
            if (selection.isEmpty()) {
                hSplitAction.setText("");
                return;
            }

            Completion selectedCompletion = (Completion) selection.getFirstElement();
            String responseMessage = Optional.ofNullable(selectedCompletion)
                                            .map(Completion::getResponse)
                                            .map(Response::getResponseMessage)
                                            .orElse("");
            hSplitAction.setText(responseMessage);
            
            openSourceAction.processDataForScripEditor(selectedCompletion.getSourcePath());
        });
    }

    private void setupToolbar() {
        IToolBarManager toolbarManager = getViewSite().getActionBars().getToolBarManager();
        addActionToToolbar(toolbarManager);
    }
    
    private void addActionToToolbar(IToolBarManager toolbarManager) {
        toolbarManager.add(bringToFrontAction);
        toolbarManager.add(hSplitAction);
        toolbarManager.add(openSourceAction);
    }
    
    private String stripHtmlTags(String source) {
        ParserDelegator delegator = new ParserDelegator();
        final StringBuilder result = new StringBuilder();
        
        HTMLEditorKit.ParserCallback callback = new HTMLEditorKit.ParserCallback() {
            public void handleText(char[] data, int pos) {
                result.append(data);
            }
        };
        try {
            delegator.parse(new java.io.StringReader(source), callback, false);
        } catch (java.io.IOException e) {
            e.printStackTrace();
        }
        return result.toString();
    }
    
    private void createContextMenu() {
        MenuManager menuManager = new MenuManager("#AiBridgeContextMenu");
        menuManager.add(submitAction);
        menuManager.add(deleteAction);

        contextMenu = menuManager.createContextMenu(viewer.getTable());
        viewer.getTable().addListener(SWT.MouseDown, event -> {
        	viewer.getTable().setMenu(getContextMenu()); //no initial menu
        });

    }
    
    private Menu getContextMenu() {
    	 @SuppressWarnings("unchecked")
			Map<UUID, Completion> input = (Map<UUID, Completion>) viewer.getInput();
         if (input == null || input.isEmpty()) {
         	return null;
         } else {
        	 IStructuredSelection selection = viewer.getStructuredSelection();
             if (selection.isEmpty()) {
                 return null;
             }
         	return contextMenu;
         }
    }
    
    public static IStructuredSelection getSelection() {
    	return viewer.getStructuredSelection();
    }
    
    private ColumnLabelProvider getColumnProvider(final int columnIndex) {
        return new ColumnLabelProvider() {
            @Override
            public String getText(Object element) {
                if (element instanceof Completion completion) {
                    return switch (columnIndex) {
                        case 0 -> Optional.ofNullable(completion.getCmdName()).orElse("");
                        case 1 -> Optional.ofNullable(completion.getStatus()).orElse("");
                        case 2 -> Optional.ofNullable(completion.getStartTime()).map(currentInstance::formatDate).orElse("");
                        case 3 -> Optional.ofNullable(completion.getSelection()).orElse("");
                        case 4 -> Optional.ofNullable(completion.getResponse()).map(resp -> stripHtmlTags(resp.getResponseMessage())).orElse("");
                        case 5 -> formatTimeDifference(completion);
                        default -> "";
                    };
                }
                return super.getText(element);
            }
        };
    }
    
    private String formatDate(Date date) {
        return date.toInstant()
                   .atZone(ZoneId.systemDefault())
                   .toLocalDateTime()
                   .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
    }

    private String formatTimeDifference(Completion completion) {
        if (completion.getEndTime() != null && completion.getStartTime() != null) {
            double differenceInSeconds = (double) (completion.getEndTime().getTime() - completion.getStartTime().getTime()) / 1000;
            return String.format("%.2f s", Math.round(differenceInSeconds * 100.0) / 100.0);
        }
        return "";
    }



    @Override
    public void setFocus() {
        viewer.getControl().setFocus();
    }
    
    public static void refresh() {
        if (viewer == null || viewer.getControl().isDisposed()) return;

        Display.getDefault().asyncExec(() -> {
            if (currentInstance != null) {
                if (currentInstance.bringToFrontAction.isChecked()) {
                    currentInstance.getSite().getPage().bringToTop(currentInstance);
                }
                currentInstance.hSplitAction.refresh();
            }
            viewer.refresh();
        });
    }
    
    @Override
    public void saveState(IMemento memento) {
        TableColumn[] columns = viewer.getTable().getColumns();
        for (int i = 0; i < columns.length; i++) {
            Integer width = columns[i].getWidth();
            memento.putInteger("COLUMN_WIDTH_" + i, width);
        }
    }
    
    @Override
    public void dispose() {
       ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(this);
        super.dispose();
    }



    class ContentProvider implements IStructuredContentProvider {
        @Override
        public Object[] getElements(Object inputElement) {
        	Object result[] = ((ConcurrentMap<?, ?>) inputElement).values().toArray();    	
            return result;
        }
    }
    
    public class StartTimeComparator extends ViewerComparator {
        @Override
        public int compare(Viewer viewer, Object e1, Object e2) {
            if (e1 instanceof Completion && e2 instanceof Completion) {
                Completion c1 = (Completion) e1;
                Completion c2 = (Completion) e2;
                return c2.getStartTime().compareTo(c1.getStartTime());
            }
            return super.compare(viewer, e1, e2);
        }
    }

	@Override
	public boolean activeProjectWillChange(ServoyProject activeProject, ServoyProject toProject) {
		if (activeProject != null)
			AiBridgeManager.saveData(activeProject.getSolution().getName());
		return true;
	}

	@Override
	public void activeProjectChanged(ServoyProject activeProject) {
		System.out.println("######## Active project changed: " + activeProject.getSolution().getName());
		solutionName = activeProject.getSolution().getName();
		AiBridgeManager.loadData(solutionName);
        AiBridgeView.refresh();
	}

	@Override
	public void activeProjectUpdated(ServoyProject activeProject, int updateInfo) {
	}
	
	public static String getSolutionName() {
		return solutionName;
	}
}
