/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.IJobManager;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.util.SafeRunnable;
import org.eclipse.jface.viewers.CheckboxTreeViewer;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IBaseLabelProvider;
import org.eclipse.jface.viewers.IColorProvider;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreePath;
import org.eclipse.jface.viewers.TreeSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.internal.IWorkbenchGraphicConstants;
import org.eclipse.ui.internal.WorkbenchImages;
import org.eclipse.ui.progress.WorkbenchJob;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.labelproviders.DelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DeprecationDecoratingStyledCellLabelProvider;

/**
 * JFace-like viewer for selecting a value from a tree. A filter is built-in.
 *
 * @author rgansevles
 */

public class FilteredTreeViewer extends FilteredTree implements ISelectionProvider, IDoubleClickListener
{
	private IBaseLabelProvider labelProvider;
	private ITreeContentProvider contentProvider;
	private ViewerComparator comparator;

	/**
	 * List of open listeners (element type: <code>ISelectionActivateListener</code>).
	 *
	 * @see #fireOpen
	 */
	private final ListenerList openListeners = new ListenerList();


	private WorkbenchJob refreshJob;
	private boolean narrowingDown;
	private String previousFilterText;
	private boolean initialised = false;
	private boolean closing;
	private int searching = 0;
	private final IFilter selectionFilter;
	private MenuManager menuManager;
	private ToolItem toolItem;
	private ToolBar toolBar;
	private final boolean showMenu;
	private List<Object> orderedSelection;

	public static final String CONTENT_LOADING_JOB_FAMILY = "svyContentLoadingJobFamily";

	public FilteredTreeViewer(Composite parent, boolean showFilter, boolean showMenu, ITreeContentProvider contentProvider, IBaseLabelProvider labelProvider,
		ViewerComparator comparator, int treeStyle, TreePatternFilter treePatternFilter, IFilter selectionFilter)
	{
		super(parent, SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER | treeStyle, treePatternFilter, true);
		this.showFilterControls = showFilter;
		this.showMenu = showMenu;
		this.selectionFilter = selectionFilter;
		if (!showFilter)
		{
			filterComposite.dispose();
			filterComposite = null;
			filterToolBar = null;
//			filterText = null; // do not set null (causes NPE later)
		}
		this.contentProvider = contentProvider;
		this.labelProvider = labelProvider;
		this.comparator = comparator;
		initialise();
		initialised = true;
	}

	protected void initialise()
	{
		createFixedRefreshJob();
		setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));

		treeViewer.setLabelProvider(new DeprecationDecoratingStyledCellLabelProvider(labelProvider));
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setComparator(comparator);
		treeViewer.addDoubleClickListener(this);
		treeViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				updateOrderedSelection(getSelection());
			}
		});

		if (!filterText.isDisposed() && filterComposite != null)
		{
			filterText.setEnabled(false);
			filterComposite.setVisible(showFilterControls);
			if (showMenu)
			{
				createViewMenu(filterComposite);
			}
		}

		addDisposeListener(new DisposeListener()
		{
			public void widgetDisposed(DisposeEvent event)
			{
				handleDispose(event);
			}
		});
	}

	public void setLabelProvider(ILabelProvider labelProvider)
	{
		treeViewer.setLabelProvider(labelProvider);
	}

	@Override
	protected void createControl(Composite composite, int treeStyle)
	{
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		setLayout(layout);
		setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		if (showFilterControls)
		{
			filterComposite = new Composite(this, SWT.NONE);
			GridLayout filterLayout = new GridLayout(3, false);
			filterLayout.marginHeight = 0;
			filterLayout.marginWidth = 0;
			filterComposite.setLayout(filterLayout);
			filterComposite.setFont(composite.getFont());

			createFilterControls(filterComposite);
			filterComposite.setLayoutData(new GridData(SWT.FILL, SWT.BEGINNING, true, false));
		}

		treeComposite = new Composite(this, SWT.NONE);
		GridLayout treeCompositeLayout = new GridLayout();
		treeCompositeLayout.marginHeight = 0;
		treeCompositeLayout.marginWidth = 0;
		treeComposite.setLayout(treeCompositeLayout);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		treeComposite.setLayoutData(data);
		createTreeControl(treeComposite, treeStyle);
	}

	@Override
	protected Control createTreeControl(Composite p, int style)
	{
		Control treeControl = super.createTreeControl(p, style);
		treeControl.addDisposeListener(new DisposeListener()
		{
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.swt.events.DisposeListener#widgetDisposed(org.eclipse.swt.events.DisposeEvent)
			 */
			public void widgetDisposed(DisposeEvent e)
			{
				refreshJob.cancel();
			}
		});
		return treeControl;
	}

	@Override
	protected TreeViewer doCreateTreeViewer(Composite treeParent, int style)
	{
		if ((style & SWT.CHECK) == 0)
		{
			return super.doCreateTreeViewer(treeParent, style);
		}

		// create tree with checkboxes
		return new CheckboxTreeViewer(treeParent, style & ~SWT.CHECK)
		{
			@Override
			public void setSelection(ISelection selection)
			{
				setCheckedElements(((IStructuredSelection)selection).toArray());
			}

			@Override
			public void setSelection(ISelection selection, boolean reveal)
			{
				setCheckedElements(((IStructuredSelection)selection).toArray());
				if (reveal && !selection.isEmpty())
				{
					reveal(((IStructuredSelection)selection).getFirstElement());
				}
			}

			@Override
			public ISelection getSelection()
			{
				return new StructuredSelection(getCheckedElements());
			}

			@Override
			public ITreeSelection getStructuredSelection() throws ClassCastException
			{
				List<TreePath> paths = new ArrayList<TreePath>();
				Object[] selection = getCheckedElements();
				if (selection != null)
				{
					for (Object select : selection)
					{
						paths.add(new TreePath(new Object[] { select }));
					}
				}
				return new TreeSelection(paths.toArray(new TreePath[0]));
			}
		};
	}

	/**
	 * Create the refresh job for the receiver.
	 *
	 */
	private void createFixedRefreshJob()
	{
		refreshJob = new WorkbenchJob("Refresh Filter")
		{
			/*
			 * (non-Javadoc)
			 *
			 * @see org.eclipse.ui.progress.UIJob#runInUIThread(org.eclipse.core.runtime.IProgressMonitor)
			 */
			@Override
			public IStatus runInUIThread(IProgressMonitor monitor)
			{
				if (treeViewer.getControl().isDisposed())
				{
					return Status.CANCEL_STATUS;
				}

				String text = getFilterString();
				if (text == null)
				{
					return Status.OK_STATUS;
				}
				boolean initial = initialText != null && initialText.equals(text);
				if (initial)
				{
					getPatternFilter().setPattern(null);
				}
				else if (text != null)
				{
					getPatternFilter().setPattern(text);
				}

				Control redrawFalseControl = treeComposite != null ? treeComposite : treeViewer.getControl();
				try
				{
					// don't want the user to see updates that will be made to the tree
					// we are setting redraw(false) on the composite to avoid dancing scrollbar
					redrawFalseControl.setRedraw(false);
					if (!narrowingDown)
					{
						// collapse all
						TreeItem[] is = treeViewer.getTree().getItems();
						for (TreeItem item : is)
						{
							if (item.getExpanded())
							{
								treeViewer.setExpandedState(item.getData(), false);
							}
						}
					}
					treeViewer.refresh(true);

					if (text.length() > 0 && !initial)
					{
						/*
						 * Expand elements one at a time. After each is expanded, check to see if the filter text has been modified. If it has, then cancel the
						 * refresh job so the user doesn't have to endure expansion of all the nodes.
						 */
						Object[] items = getContentProvider().getElements(getInput());
						List<TreePath> lst = new ArrayList<TreePath>(128);
						recursiveExpand(null, items, monitor, lst);
						for (TreePath treePath : lst)
						{
							treeViewer.expandToLevel(treePath, 4);
						}
						// enabled toolbar - there is text to clear
						// and the list is currently being filtered
						updateToolbar(true);
					}
					else
					{
						// disabled toolbar - there is no text to clear
						// and the list is currently not filtered
						updateToolbar(false);
					}

					if (!getInitialText().equals(filterText.getText().trim()) && filterText.getText().trim().length() > 0)
					{
						TreeItem item = ((TreePatternFilter)getPatternFilter()).getFirstMatchingItem(getViewer(), getViewer().getTree().getItems());
						if (item != null)
						{
							getViewer().getTree().setSelection(new TreeItem[] { item });
							getViewer().setSelection(getSelection(), true);
						}
					}
				}
				finally
				{

					// done updating the tree - set redraw back to true
					redrawFalseControl.setRedraw(true);

					searching--;
					openSelectedNodeWhenRefreshNodeFinished();
				}
				return Status.OK_STATUS;
			}

			/**
			 * Returns true if the job should be canceled (because of timeout or actual cancellation).
			 *
			 * @param items
			 * @param provider
			 * @param monitor
			 * @param cancelTime
			 * @param numItemsLeft
			 * @return true if canceled
			 */
			private boolean recursiveExpand(TreePath path, Object[] items, IProgressMonitor monitor, List<TreePath> lst)
			{
//				if (path != null && path.getSegmentCount() > 5) return false;
//				boolean expanded = false;
//				ISearchKeyAdapter searchKeyAdapter = null;
//				if (getContentProvider() instanceof ISearchKeyAdapter)
//				{
//					searchKeyAdapter = (ISearchKeyAdapter)getContentProvider();
//				}
				TreePatternFilter treePatternFilter = (TreePatternFilter)getPatternFilter();
				List<Object> foundElements = treePatternFilter.getFoundElements();

				for (Object object : foundElements)
				{
					ArrayList<Object> objectPath = new ArrayList<Object>();
					objectPath.add(object);
					Object p = getContentProvider().getParent(object);
					while (p != null)
					{
						objectPath.add(p);
						p = getContentProvider().getParent(p);
					}
					Collections.reverse(objectPath);
					lst.add(new TreePath(objectPath.toArray()));
					if (lst.size() > 10) return true;
				}
				return lst.size() > 0;
//				outer : for (Object item : items)
//				{
//					if (!treePatternFilter.isElementVisible(treeViewer, item)) continue;
//					if (path != null && searchKeyAdapter != null)
//					{
//						Object currentSearchKey = searchKeyAdapter.getSearchKey(item);
//						if (currentSearchKey != null)
//						{
//							for (int i = 0; i < path.getSegmentCount(); i++)
//							{
//								Object pathSearchKey = searchKeyAdapter.getSearchKey(path.getSegment(i));
//								if (currentSearchKey.equals(pathSearchKey)) continue outer;
//							}
//						}
//					}
//					TreePath itemPath = null;
//					if (path == null)
//					{
//						itemPath = new TreePath(new Object[] { item });
//					}
//					else
//					{
//						itemPath = path.createChildPath(item);
//					}
//					if (getContentProvider().hasChildren(item))
//					{
//						Object[] children = getContentProvider().getChildren(item);
//						if (children.length > 0)
//						{
//							expanded = recursiveExpand(itemPath, children, monitor, lst) || expanded;
//						}
//						if (!expanded && treePatternFilter.shouldExpandNodeForMatch(treeViewer, item))
//						{
//							lst.add(itemPath);
//							expanded = true;
//						}
//					}
//					if (lst.size() > 10)
//					{
//						return expanded;
//					}
//				}
//				return expanded;
			}

		};
		refreshJob.setSystem(true);
	}

	@Override
	protected void createFilterText(Composite composite)
	{
		super.createFilterText(composite);
		// enter key set focus to tree
		filterText.addTraverseListener(new TraverseListener()
		{
			public void keyTraversed(TraverseEvent e)
			{
				if (e.detail == SWT.TRAVERSE_RETURN)
				{
					e.doit = false;
					e.detail = SWT.NONE; // prevent other listeners to pick it up
					if (getViewer().getTree().getItemCount() == 0)
					{
						Display.getCurrent().beep();
					}
					else
					{
						// if the initial filter text hasn't changed, do not try to match
						closing = true;
						openSelectedNodeWhenRefreshNodeFinished();
					}
				}
			}

		});
	}

	private void createViewMenu(Composite composite)
	{
		toolBar = new ToolBar(composite, SWT.FLAT);
		toolItem = new ToolItem(toolBar, SWT.PUSH, 0);

		GridData data = new GridData();
		data.horizontalAlignment = GridData.END;
		toolBar.setLayoutData(data);

		toolBar.addMouseListener(new MouseAdapter()
		{
			@Override
			public void mouseDown(MouseEvent e)
			{
				showViewMenu();
			}
		});

		toolItem.setImage(WorkbenchImages.getImage(IWorkbenchGraphicConstants.IMG_LCL_VIEW_MENU));
		toolItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				showViewMenu();
			}
		});

		menuManager = new MenuManager();

		fillViewMenu();
	}


	private void showViewMenu()
	{
		Menu menu = menuManager.createContextMenu(getShell());
		Rectangle bounds = toolItem.getBounds();
		Point topLeft = new Point(bounds.x, bounds.y + bounds.height);
		topLeft = toolBar.toDisplay(topLeft);
		menu.setLocation(topLeft.x, topLeft.y);
		menu.setVisible(true);
	}

	protected void fillViewMenu()
	{
		menuManager.add(new ToggleFiltermodeAction());
	}

	/**
	 * Update the receiver after the text has changed.
	 */
	@Override
	protected void textChanged()
	{
		if (initialised)
		{
			synchronized (refreshJob)
			{
				if (closing)
				{
					// already pressed RETURN
					return;
				}

				narrowingDown = previousFilterText == null || getFilterString().startsWith(previousFilterText);
				previousFilterText = getFilterString();
				// cancel currently running job first, to prevent unnecessary redraw
				searching++;
				if (searching > 1 && refreshJob.cancel())
				{
					searching--;
				}
				refreshJob.schedule(400);
			}
		}
	}

	protected void openSelectedNodeWhenRefreshNodeFinished()
	{
		synchronized (refreshJob)
		{
			if (searching > 0 || !closing)
			{
				// Still searching or not closing yet
				return;
			}

			ISelection selection = getSelection();
			if (selection.isEmpty())
			{
				closing = false;
			}
			else
			{
				fireOpen(new OpenEvent(treeViewer, selection));
			}
		}
	}

	public void setInput(Object input)
	{
		treeViewer.setInput(input);
		// if the tree has only one or zero views, disable the filter text control
		if (!filterText.isDisposed())
		{
			filterText.setEnabled(!hasAtMostOneView(treeViewer));
		}
	}

	public Object getInput()
	{
		return treeViewer.getInput();
	}

	/**
	 * Return whether or not there are less than two views in the list.
	 *
	 * @param tree
	 * @return <code>true</code> if there are less than two views in the list.
	 */
	private boolean hasAtMostOneView(TreeViewer tree)
	{
		IJobManager jobManager = Job.getJobManager();
		try
		{
			//wait for content jobs to finish if it's the case
			jobManager.join(CONTENT_LOADING_JOB_FAMILY, null);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		ITreeContentProvider tcp = (ITreeContentProvider)tree.getContentProvider();
		Object[] children = tcp.getElements(tree.getInput());
		if (children.length <= 1)
		{
			if (children.length == 0)
			{
				return true;
			}
			return !tcp.hasChildren(children[0]);
		}
		return false;
	}


	public IBaseLabelProvider getLabelProvider()
	{
		return labelProvider;
	}

	public ITreeContentProvider getContentProvider()
	{
		return contentProvider;
	}

	/**
	 * Adds a listener for selection-open in this viewer. Has no effect if an identical listener is already registered.
	 *
	 * @param listener an open listener
	 */
	public void addOpenListener(IOpenListener listener)
	{
		openListeners.add(listener);
	}


	/**
	 * Removes the given open listener from this viewer. Has no affect if an identical listener is not registered.
	 *
	 * @param listener an open listener
	 */
	public void removeOpenListener(IOpenListener listener)
	{
		openListeners.remove(listener);
	}

	/**
	 * Notifies any open event listeners that a open event has been received. Only listeners registered at the time this method is called are notified.
	 *
	 * @param event an open
	 *
	 * @see IOpenListener#open(OpenEvent)
	 */
	protected void fireOpen(final OpenEvent event)
	{
		Object[] listeners = openListeners.getListeners();
		for (Object listener : listeners)
		{
			final IOpenListener l = (IOpenListener)listener;
			SafeRunnable.run(new SafeRunnable()
			{
				public void run()
				{
					l.open(event);
				}
			});
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.viewers.IDoubleClickListener#doubleClick(org.eclipse.jface.viewers.DoubleClickEvent)
	 */
	public void doubleClick(DoubleClickEvent event)
	{
		IStructuredSelection s = (IStructuredSelection)event.getSelection();
		Object element = s.getFirstElement();
		if (treeViewer.isExpandable(element))
		{
			treeViewer.setExpandedState(element, !treeViewer.getExpandedState(element));
		}
		else
		{
			ISelection selection = getSelection();
			if (!selection.isEmpty())
			{
				fireOpen(new OpenEvent(treeViewer, selection));
			}
		}
	}

	protected void handleDispose(@SuppressWarnings("unused") DisposeEvent event)
	{
		openListeners.clear();
		labelProvider = null;
		contentProvider = null;
		comparator = null;
	}


	/**
	 * Adds a listener for selection changes in this selection provider. Has no effect if an identical listener is already registered.
	 *
	 * @param listener a selection changed listener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener)
	{
		treeViewer.addSelectionChangedListener(listener);
	}

	/**
	 * Returns the current selection for this provider.
	 *
	 * @return the current selection
	 */
	public ISelection getSelection()
	{
		IStructuredSelection treeSelection = (IStructuredSelection)treeViewer.getSelection();

		if (selectionFilter == null)
		{
			return treeSelection;
		}
		// filter out parent nodes when they are selected
		List<Object> selectedObjects = new ArrayList<Object>();
		for (Object selected : treeSelection.toArray())
		{
			if (selectionFilter.select(selected))
			{
				selectedObjects.add(selected);
			}
		}
		return new StructuredSelection(selectedObjects);
	}

	/**
	 * Update the selection object, keep the selection order.
	 */
	protected void updateOrderedSelection(ISelection treeSelection)
	{
		List<Object> newSelection = new ArrayList<Object>();
		List<Object> treeList = null;
		if (treeSelection instanceof IStructuredSelection)
		{
			treeList = new ArrayList<Object>(((IStructuredSelection)treeSelection).toList());
		}
		if (treeList != null)
		{
			if (orderedSelection != null)
			{
				for (Object o : orderedSelection)
				{
					if (treeList.remove(o))
					{
						newSelection.add(o);
					}
				}
			}
			newSelection.addAll(treeList);
		}
		orderedSelection = newSelection;
	}

	public void clearOrderedSelection()
	{
		orderedSelection = null;
	}

	public List<Object> getOrderedSelection()
	{
		return orderedSelection;
	}

	/**
	 * Removes the given selection change listener from this selection provider. Has no affect if an identical listener is not registered.
	 *
	 * @param listener a selection changed listener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener)
	{
		treeViewer.removeSelectionChangedListener(listener);
	}

	/**
	 * Sets the current selection for this selection provider.
	 *
	 * @param selection the new selection
	 */
	public void setSelection(ISelection selection)
	{
		treeViewer.setSelection(selection, true);
		updateOrderedSelection(selection);
	}

	/**
	 * Toggle the filter mode
	 * @author rgansevles
	 *
	 */
	private class ToggleFiltermodeAction extends Action
	{
		public ToggleFiltermodeAction()
		{
			super("Filter on parent nodes", IAction.AS_CHECK_BOX);
			setToolTipText("Toggle filtering on leaf nodes or parent nodes");
			setChecked(((TreePatternFilter)getPatternFilter()).getFilterMode() == TreePatternFilter.FILTER_PARENTS);
		}

		/**
		 * @see org.eclipse.jface.action.Action#run()
		 */
		@Override
		public void run()
		{
			((TreePatternFilter)getPatternFilter()).setFilterMode(isChecked() ? TreePatternFilter.FILTER_PARENTS : TreePatternFilter.FILTER_LEAFS);
			textChanged();
		}
	}

	/**
	 * Label provider to provide default folder images on expandable items
	 *
	 * @author rgansevles
	 *
	 */
	public static class TreeFolderLabelProvider extends DelegateLabelProvider implements IFontProvider, IColorProvider
	{
		private final TreeViewer treeViewer;

		public TreeFolderLabelProvider(TreeViewer treeViewer, IBaseLabelProvider labelProvider)
		{
			super(labelProvider);
			this.treeViewer = treeViewer;
		}

		/**
		 * Default image for expandable items: folder
		 */
		@Override
		public Image getImage(Object element)
		{
			Image image = super.getImage(element);
			if (image == null && treeViewer.isExpandable(element))
			{
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_FOLDER);
			}
			return image;
		}

		@Override
		public Font getFont(Object element)
		{
			if (getLabelProvider() instanceof IFontProvider)
			{
				return ((IFontProvider)getLabelProvider()).getFont(element);
			}
			return null;
		}

		@Override
		public Color getBackground(Object element)
		{
			if (getLabelProvider() instanceof IColorProvider)
			{
				return ((IColorProvider)getLabelProvider()).getBackground(element);
			}
			return null;
		}

		@Override
		public Color getForeground(Object element)
		{
			if (getLabelProvider() instanceof IColorProvider)
			{
				return ((IColorProvider)getLabelProvider()).getForeground(element);
			}
			return null;
		}
	}
}
