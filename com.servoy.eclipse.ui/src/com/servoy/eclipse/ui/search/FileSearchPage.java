/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.search;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.resources.IContainer;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerComparator;
import org.eclipse.search.ui.IContextMenuConstants;
import org.eclipse.search.ui.ISearchResultPage;
import org.eclipse.search.ui.ISearchResultViewPart;
import org.eclipse.search.ui.text.AbstractTextSearchResult;
import org.eclipse.search.ui.text.AbstractTextSearchViewPage;
import org.eclipse.search.ui.text.Match;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageLayout;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.part.IShowInSource;
import org.eclipse.ui.part.IShowInTargetList;
import org.eclipse.ui.part.ResourceTransfer;
import org.eclipse.ui.part.ShowInContext;
import org.eclipse.ui.views.navigator.NavigatorDragAdapter;

import com.ibm.icu.text.MessageFormat;

/**
 * The {@link ISearchResultPage} for displaying the file based search results.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class FileSearchPage extends AbstractTextSearchViewPage implements IAdaptable
{

	public static class DecoratorIgnoringViewerSorter extends ViewerComparator
	{
		private final ILabelProvider fLabelProvider;

		public DecoratorIgnoringViewerSorter(ILabelProvider labelProvider)
		{
			fLabelProvider = labelProvider;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ViewerComparator#category(java.lang.Object)
		 */
		@Override
		public int category(Object element)
		{
			if (element instanceof IContainer)
			{
				return 1;
			}
			return 2;
		}

		@Override
		public int compare(Viewer viewer, Object e1, Object e2)
		{
			int cat1 = category(e1);
			int cat2 = category(e2);

			if (cat1 != cat2)
			{
				return cat1 - cat2;
			}

			if (e1 instanceof LineElement && e2 instanceof LineElement)
			{
				LineElement m1 = (LineElement)e1;
				LineElement m2 = (LineElement)e2;
				return m1.getOffset() - m2.getOffset();
			}

			String name1 = fLabelProvider.getText(e1);
			String name2 = fLabelProvider.getText(e2);
			if (name1 == null) name1 = "";//$NON-NLS-1$
			if (name2 == null) name2 = "";//$NON-NLS-1$
			return getComparator().compare(name1, name2);
		}
	}

	private static final String KEY_SORTING = "org.eclipse.search.resultpage.sorting"; //$NON-NLS-1$
	private static final String KEY_LIMIT = "org.eclipse.search.resultpage.limit"; //$NON-NLS-1$

	private static final int DEFAULT_ELEMENT_LIMIT = 1000;

	private IFileSearchContentProvider fContentProvider;
	private int fCurrentSortOrder;
	private final SortAction fSortByNameAction;
	private final SortAction fSortByPathAction;


	private static final String[] SHOW_IN_TARGETS = new String[] { IPageLayout.ID_RES_NAV };
	private static final IShowInTargetList SHOW_IN_TARGET_LIST = new IShowInTargetList()
	{
		public String[] getShowInTargetIds()
		{
			return SHOW_IN_TARGETS;
		}
	};

	public FileSearchPage()
	{
		fSortByNameAction = new SortAction(SearchMessages.FileSearchPage_sort_name_label, this, FileLabelProvider.SHOW_LABEL_PATH);
		fSortByPathAction = new SortAction(SearchMessages.FileSearchPage_sort_path_label, this, FileLabelProvider.SHOW_PATH_LABEL);

		setElementLimit(new Integer(DEFAULT_ELEMENT_LIMIT));
	}

	@Override
	public void setElementLimit(Integer elementLimit)
	{
		super.setElementLimit(elementLimit);
		int limit = elementLimit.intValue();
		getSettings().put(KEY_LIMIT, limit);
	}

	@Override
	public StructuredViewer getViewer()
	{
		return super.getViewer();
	}

	private void addDragAdapters(StructuredViewer viewer)
	{
		Transfer[] transfers = new Transfer[] { ResourceTransfer.getInstance() };
		int ops = DND.DROP_COPY | DND.DROP_LINK;
		viewer.addDragSupport(ops, transfers, new NavigatorDragAdapter(viewer));
	}

	@Override
	protected void configureTableViewer(TableViewer viewer)
	{
		viewer.setUseHashlookup(true);
		FileLabelProvider innerLabelProvider = new FileLabelProvider(this, fCurrentSortOrder);
		viewer.setLabelProvider(new DecoratingFileSearchLabelProvider(innerLabelProvider));
		viewer.setContentProvider(new FileTableContentProvider(this));
		viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
		fContentProvider = (IFileSearchContentProvider)viewer.getContentProvider();
		addDragAdapters(viewer);
	}

	@Override
	protected void configureTreeViewer(TreeViewer viewer)
	{
		viewer.setUseHashlookup(true);
		FileLabelProvider innerLabelProvider = new FileLabelProvider(this, FileLabelProvider.SHOW_LABEL);
		viewer.setLabelProvider(new DecoratingFileSearchLabelProvider(innerLabelProvider));
		viewer.setContentProvider(new FileTreeContentProvider(this, viewer));
		viewer.setComparator(new DecoratorIgnoringViewerSorter(innerLabelProvider));
		fContentProvider = (IFileSearchContentProvider)viewer.getContentProvider();
		addDragAdapters(viewer);
	}

	@Override
	protected void showMatch(Match match, int offset, int length, boolean activate) throws PartInitException
	{
		IFile file = (IFile)match.getElement();
		IWorkbenchPage page = getSite().getPage();
		if (offset >= 0 && length != 0)
		{
			openAndSelect(page, file, offset, length, activate);
		}
		else
		{
			open(page, file, activate);
		}
	}

	@Override
	protected void handleOpen(OpenEvent event)
	{
		if (showLineMatches())
		{
			Object firstElement = ((IStructuredSelection)event.getSelection()).getFirstElement();
			if (firstElement instanceof IFile)
			{
				if (getDisplayedMatchCount(firstElement) == 0)
				{
					try
					{
						open(getSite().getPage(), (IFile)firstElement, false);
					}
					catch (PartInitException e)
					{
						ErrorDialog.openError(getSite().getShell(), SearchMessages.FileSearchPage_open_file_dialog_title,
							SearchMessages.FileSearchPage_open_file_failed, e.getStatus());
					}
					return;
				}
			}
		}
		super.handleOpen(event);
	}

	@Override
	protected void fillContextMenu(IMenuManager mgr)
	{
		super.fillContextMenu(mgr);
		addSortActions(mgr);
	}

	private void addSortActions(IMenuManager mgr)
	{
		if (getLayout() != FLAG_LAYOUT_FLAT) return;
		MenuManager sortMenu = new MenuManager(SearchMessages.FileSearchPage_sort_by_label);
		sortMenu.add(fSortByNameAction);
		sortMenu.add(fSortByPathAction);

		fSortByNameAction.setChecked(fCurrentSortOrder == fSortByNameAction.getSortOrder());
		fSortByPathAction.setChecked(fCurrentSortOrder == fSortByPathAction.getSortOrder());

		mgr.appendToGroup(IContextMenuConstants.GROUP_VIEWER_SETUP, sortMenu);
	}

	@Override
	public void setViewPart(ISearchResultViewPart part)
	{
		super.setViewPart(part);
	}

	@Override
	public void init(IPageSite site)
	{
		super.init(site);
	}

	@Override
	public void dispose()
	{
		super.dispose();
	}

	@Override
	protected void elementsChanged(Object[] objects)
	{
		if (fContentProvider != null) fContentProvider.elementsChanged(objects);
	}

	@Override
	protected void clear()
	{
		if (fContentProvider != null) fContentProvider.clear();
	}

	public void setSortOrder(int sortOrder)
	{
		fCurrentSortOrder = sortOrder;
		DecoratingFileSearchLabelProvider lpWrapper = (DecoratingFileSearchLabelProvider)getViewer().getLabelProvider();
		((FileLabelProvider)lpWrapper.getStyledStringProvider()).setOrder(sortOrder);
		getViewer().refresh();
		getSettings().put(KEY_SORTING, fCurrentSortOrder);
	}

	@Override
	public void restoreState(IMemento memento)
	{
		super.restoreState(memento);
		try
		{
			fCurrentSortOrder = getSettings().getInt(KEY_SORTING);
		}
		catch (NumberFormatException e)
		{
			fCurrentSortOrder = fSortByNameAction.getSortOrder();
		}
		int elementLimit = DEFAULT_ELEMENT_LIMIT;
		try
		{
			elementLimit = getSettings().getInt(KEY_LIMIT);
		}
		catch (NumberFormatException e)
		{
		}
		if (memento != null)
		{
			Integer value = memento.getInteger(KEY_SORTING);
			if (value != null) fCurrentSortOrder = value.intValue();

			value = memento.getInteger(KEY_LIMIT);
			if (value != null) elementLimit = value.intValue();
		}
		setElementLimit(new Integer(elementLimit));
	}

	@Override
	public void saveState(IMemento memento)
	{
		super.saveState(memento);
		memento.putInteger(KEY_SORTING, fCurrentSortOrder);
		memento.putInteger(KEY_LIMIT, getElementLimit().intValue());
	}

	public Object getAdapter(Class adapter)
	{
		if (IShowInTargetList.class.equals(adapter))
		{
			return SHOW_IN_TARGET_LIST;
		}

		if (adapter == IShowInSource.class)
		{
			ISelectionProvider selectionProvider = getSite().getSelectionProvider();
			if (selectionProvider == null) return null;

			ISelection selection = selectionProvider.getSelection();
			if (selection instanceof IStructuredSelection)
			{
				IStructuredSelection structuredSelection = ((StructuredSelection)selection);
				final Set newSelection = new HashSet(structuredSelection.size());
				Iterator iter = structuredSelection.iterator();
				while (iter.hasNext())
				{
					Object element = iter.next();
					if (element instanceof LineElement) element = ((LineElement)element).getParent();
					newSelection.add(element);
				}

				return new IShowInSource()
				{
					public ShowInContext getShowInContext()
					{
						return new ShowInContext(null, new StructuredSelection(new ArrayList(newSelection)));
					}
				};
			}
			return null;
		}

		return null;
	}

	@Override
	public String getLabel()
	{
		String label = super.getLabel();
		StructuredViewer viewer = getViewer();
		if (viewer instanceof TableViewer)
		{
			TableViewer tv = (TableViewer)viewer;

			AbstractTextSearchResult result = getInput();
			if (result != null)
			{
				int itemCount = ((IStructuredContentProvider)tv.getContentProvider()).getElements(getInput()).length;
				if (showLineMatches())
				{
					int matchCount = getInput().getMatchCount();
					if (itemCount < matchCount)
					{
						return MessageFormat.format(SearchMessages.FileSearchPage_limited_format_matches,
							new Object[] { label, new Integer(itemCount), new Integer(matchCount) });
					}
				}
				else
				{
					int fileCount = getInput().getElements().length;
					if (itemCount < fileCount)
					{
						return MessageFormat.format(SearchMessages.FileSearchPage_limited_format_files,
							new Object[] { label, new Integer(itemCount), new Integer(fileCount) });
					}
				}
			}
		}
		return label;
	}

	@Override
	public int getDisplayedMatchCount(Object element)
	{
		if (showLineMatches())
		{
			if (element instanceof LineElement)
			{
				LineElement lineEntry = (LineElement)element;
				return lineEntry.getNumberOfMatches(getInput());
			}
			return 0;
		}
		return super.getDisplayedMatchCount(element);
	}

	@Override
	public Match[] getDisplayedMatches(Object element)
	{
		if (showLineMatches())
		{
			if (element instanceof LineElement)
			{
				LineElement lineEntry = (LineElement)element;
				return lineEntry.getMatches(getInput());
			}
			return new Match[0];
		}
		return super.getDisplayedMatches(element);
	}

	@Override
	protected void evaluateChangedElements(Match[] matches, Set changedElements)
	{
		if (showLineMatches())
		{
			for (Match matche : matches)
			{
				changedElements.add(((FileMatch)matche).getLineElement());
			}
		}
		else
		{
			super.evaluateChangedElements(matches, changedElements);
		}
	}

	private boolean showLineMatches()
	{
		AbstractTextSearchResult input = getInput();
		return getLayout() == FLAG_LAYOUT_TREE && input != null;
	}

}
