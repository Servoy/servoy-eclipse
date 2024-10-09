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
package com.servoy.eclipse.ui.views;

import java.util.Arrays;
import java.util.Map;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.ToolTip;
import org.eclipse.swt.SWT;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetEntryListener;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.actions.CopyPropertyValueAction;
import com.servoy.eclipse.ui.actions.PastePropertyValueAction;
import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.property.IProvidesTooltip;
import com.servoy.eclipse.ui.property.PropertyCategory;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.eclipse.ui.views.solutionexplorer.HTMLToolTipSupport;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * PropertySheetPage with additional Servoy features.
 */
public class ModifiedPropertySheetPage extends PropertySheetPage implements IPropertySheetEntryListener
{
	private Composite composite;
	private Label propertiesLabel;
	private final Map<String, IAction> actions;
	private Clipboard clipboard2;
	private CopyPropertyValueAction copyValueAction;
	private PastePropertyValueAction pasteValueAction;

	private IPersist selectedPersist = null;

	public ModifiedPropertySheetPage(Map<String, IAction> actions)
	{
		super();
		this.actions = actions;
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (selection instanceof IStructuredSelection sel && !sel.isEmpty())
		{
			Object firstElement = sel.getFirstElement();
			if (firstElement instanceof IAdaptable adaptable)
			{
				firstElement = adaptable.getAdapter(IPersist.class);
			}
			if (firstElement instanceof IPersist persist)
			{
				selectedPersist = persist;
			}
			else
			{
				selectedPersist = null;
			}
		}
		else
		{
			selectedPersist = null;
		}
		super.selectionChanged(part, selection);
	}

	@Override
	public void createControl(Composite parent)
	{
		composite = new Composite(parent, SWT.NONE);
		propertiesLabel = new Label(composite, SWT.NONE);
		propertiesLabel.setFont(FontResource.getDefaultFont(SWT.NONE, 0));

		super.createControl(composite);
		setSorter(new PropertySheetSorter()
		{
			@Override
			public int compare(IPropertySheetEntry entryA, IPropertySheetEntry entryB)
			{
				// try to delegate the sorting to the entries
				if (entryA instanceof IAdaptable && entryB instanceof IAdaptable)
				{
					Comparable comparableA = ((IAdaptable)entryA).getAdapter(Comparable.class);
					if (comparableA != null)
					{
						Comparable comparableB = ((IAdaptable)entryB).getAdapter(Comparable.class);
						if (comparableB != null)
						{
							return comparableA.compareTo(comparableB);
						}
					}
				}
				return super.compare(entryA, entryB);
			}

			@Override
			public int compareCategories(String categoryA, String categoryB)
			{
				if (selectedPersist instanceof MenuItem)
				{
					if (PropertyCategory.Properties.name().equals(categoryA))
					{
						return -1;
					}
					if (PropertyCategory.Properties.name().equals(categoryB))
					{
						return 1;
					}
				}
				return super.compareCategories(categoryA, categoryB);
			}
		});

		Control control = super.getControl();
		if (control instanceof Tree)
		{
			final Tree tree = (Tree)control;

			// register our custom tooltip; this constructor also registers the listeners it needs
			ToolTip tooltip = new ToolTip(tree, ToolTip.NO_RECREATE, false)
			{

				private TreeItem itemOfLastTooltipText;
				private String lastTooltipText;

				@Override
				protected Composite createToolTipContentArea(Event event, Composite parent)
				{
					TreeItem item = tree.getItem(new Point(event.x, event.y));
					if (item != null)
					{
						if (itemOfLastTooltipText != item)
						{
							itemOfLastTooltipText = item;
							lastTooltipText = getTooltipText(item);
						}

						if (lastTooltipText != null)
						{
							// the contains(">") and "<" below are an attempt to detect if the tooltip text is html based or just a simple String (in which case we need to change \n to <br/>)
							return HTMLToolTipSupport.createBrowserTooltipContentArea(this,
								lastTooltipText.contains("<") && lastTooltipText.contains(">") ? lastTooltipText : lastTooltipText.replaceAll("\\n", "<br/>"),
								parent, false, 600, 450, 250, 150);
						} // else should never happen because of shouldCreateToolTip() below that should have returned false
					}
					return new Composite(parent, SWT.NONE); // no tooltip
				}

				@Override
				protected boolean shouldCreateToolTip(Event event)
				{
					if (super.shouldCreateToolTip(event))
					{
						TreeItem item = tree.getItem(new Point(event.x, event.y));
						if (item != null)
						{
							// see if we have a tooltip text for the item we are hovering above
							if (itemOfLastTooltipText != item)
							{
								itemOfLastTooltipText = item;
								lastTooltipText = getTooltipText(item);
							}
							return lastTooltipText != null;
						}
						else return false;
					}
					else return false;
				}

				@Override
				protected Object getToolTipArea(Event event)
				{
					// identify correctly "the area" for which this tooltip is shown so that NO_RECREATE above works correctly
					return tree.getItem(new Point(event.x, event.y));
				}

				/**
				 * This method will get the tool tip text of the selected property from .spec or legacy documentation classes (as it was set when creating this item as a property descriptor).
				 *
				 * @param item the selected property
				 * @return the tool tip text
				 */
				private String getTooltipText(TreeItem item)
				{
					Object data = item.getData();
					String text = null;
					if (data instanceof ModifiedPropertySheetEntry)
					{
						IPropertyDescriptor descriptor = ((ModifiedPropertySheetEntry)data).getDescriptor();
						if (descriptor instanceof IProvidesTooltip)
						{
							text = ((IProvidesTooltip)descriptor).getTooltipText();
						}
					}

					String hardcodedText = null;
					if ((RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName(), null).equals(
						item.getText(0)) ||
						RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID.getPropertyName(), null).equals(
							item.getText(0))) ||
						RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName(), null).equals(
							item.getText(0)))
					{
						hardcodedText = "Warning: Do not use dialogs, as a dialog will interfere with focus";
					}
					else if (StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName().equals(item.getText(0)))
					{
						hardcodedText = "Set value <empty> for no title text";
					}
					else if (StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName().equals(item.getText(0)))
					{
						hardcodedText = "Set z-index of the element.";
					}
					else if (StaticContentSpecLoader.PROPERTY_ROLLOVERCURSOR.getPropertyName().equals(item.getText(0)) ||
						StaticContentSpecLoader.PROPERTY_ROLLOVERIMAGEMEDIAID.getPropertyName().replace("ID", "").equals(item.getText(0)))
					{
						hardcodedText = "Not supported in smart client for listview/tableview";
					}
					else if ("min-width".equals(item.getText(0)) || "min-height".equals(item.getText(0)))
					{
						ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getSite()
							.getSelectionProvider().getSelection();
						if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() == 1 &&
							Platform.getAdapterManager().getAdapter(((IStructuredSelection)selection).getFirstElement(),
								IPersist.class) instanceof IFormElement)
						{
							if ("min-height".equals(item.getText(0)))
							{
								hardcodedText = "If only one from top and bottom is set this is actual height. If both are set, this is minimum height.";
							}
							if ("min-width".equals(item.getText(0)))
							{
								hardcodedText = "If only one from left and right is set this is actual width. If both are set, this is minimum width.";
							}
						}
					}

					if (hardcodedText != null)
					{
						if (text == null) text = hardcodedText;
						else text += "\n\n(" + hardcodedText + ")";
					}

					return text;
				}

			};
			tooltip.setPopupDelay(500); // additional delay so that the properties view tooltip doesn't become annoying - can be increased if needed
			tooltip.setHideOnMouseDown(false);

			tree.addControlListener(new ControlAdapter()
			{
				@Override
				public void controlResized(ControlEvent e)
				{
					Rectangle area = tree.getClientArea();
					TreeColumn[] columns = tree.getColumns();
					int oldPropertyWidth = columns[0].getWidth();
					int oldValueWidth = columns[1].getWidth();
					if (area.width > 0)
					{
						int newPropertyWidth = area.width * 50 / 100;
						int newValueWidth = area.width * 50 / 100;
						if (oldPropertyWidth != newPropertyWidth) columns[0].setWidth(newPropertyWidth);
						if (oldValueWidth != newValueWidth) columns[1].setWidth(newValueWidth);
					}
				}
			});

			// on the mac, when selecting an item the cell editor gets created and activated immediately, but the next click within
			// approx 1 sec goes to the Tree in stead of the CellEditor.
			// The following is a workaround attempt to send the event to the cell editor.
			if (Boolean.parseBoolean(Settings.getInstance().getProperty("servoy.developer.slowproperties.workaround", String.valueOf(Utils.isAppleMacOS()))))
			{
				tree.addMouseListener(new MouseAdapter()
				{
					private boolean dialogIsOpen = false;

					@Override
					public void mouseDoubleClick(MouseEvent e)
					{
						if (dialogIsOpen) return;

						TreeItem[] selection = tree.getSelection();
						if (selection != null && selection.length > 0 && selection[0].getData() instanceof ModifiedPropertySheetEntry)
						{
							CellEditor createdEditor = ((ModifiedPropertySheetEntry)selection[0].getData()).getCreatedEditor();
							if (createdEditor != null && createdEditor.isActivated() && createdEditor instanceof DialogCellEditor)
							{
								dialogIsOpen = true;
								try
								{
									((DialogCellEditor)createdEditor).contentsMouseDown(e);
								}
								finally
								{
									dialogIsOpen = false;
								}
							}
						}
					}
				});
			}

			tree.addTraverseListener(e -> {

				TreeItem currentSelectedItem = tree.getSelection() != null && tree.getSelection().length > 0 ? tree.getSelection()[0] : null;

				if (e.detail == SWT.TRAVERSE_TAB_NEXT || e.detail == SWT.TRAVERSE_TAB_PREVIOUS)
				{
					if (currentSelectedItem.getItemCount() > 0) tree.setSelection(getNextItem(currentSelectedItem, e.detail == SWT.TRAVERSE_TAB_NEXT));
					else tree.setSelection(getNextSibling(tree, currentSelectedItem, e.detail == SWT.TRAVERSE_TAB_NEXT));
				}

				e.doit = false;
				Event ev = new Event();
				ev.detail = SWT.TRAVERSE_TAB_NEXT;
				ev.item = currentSelectedItem;
				tree.notifyListeners(SWT.Selection, ev);
			});

			composite.setLayout(new FormLayout());

			FormData fd_propertiesLabel = new FormData();
			fd_propertiesLabel.right = new FormAttachment(100, 0);
			fd_propertiesLabel.top = new FormAttachment(0, 0);
			fd_propertiesLabel.bottom = new FormAttachment(0, FontResource.getTextExtent(control, propertiesLabel.getFont(), "X").y);
			fd_propertiesLabel.left = new FormAttachment(0, 0);
			propertiesLabel.setLayoutData(fd_propertiesLabel);

			FormData fd_tree = new FormData();
			fd_tree.right = new FormAttachment(100, 0);
			fd_tree.bottom = new FormAttachment(100, 0);
			fd_tree.top = new FormAttachment(propertiesLabel, 0, SWT.DEFAULT);
			fd_tree.left = new FormAttachment(0, 0);
			tree.setLayoutData(fd_tree);
		}

		IWorkbenchPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor();
		if (activeEditor != null)
		{
			ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
			if (selectionProvider != null)
			{
				selectionChanged(activeEditor, selectionProvider.getSelection());
			}
		}

		Shell shell = control.getShell();
		clipboard2 = new Clipboard(shell.getDisplay());

		MenuManager menuManager = (MenuManager)control.getMenu().getData(MenuManager.MANAGER_KEY);
		menuManager.add(copyValueAction = new CopyPropertyValueAction("Copy Value", clipboard2));
		menuManager.add(pasteValueAction = new PastePropertyValueAction("Paste Value", clipboard2, this));

		Menu menu = menuManager.getMenu();
		if (menu != null)
		{
			menu.dispose();
		}
		menu = menuManager.createContextMenu(control);
		control.setMenu(menu);
	}

	private TreeItem getNextSibling(Tree tree, TreeItem currentItem, boolean forward)
	{
		TreeItem[] siblings = getSiblings(tree, currentItem);
		if (siblings.length < 2) return getNextSibling(tree, currentItem.getParentItem(), forward);
		int index = -1;
		for (int i = 0; i < siblings.length; i++)
		{
			if (siblings[i] == currentItem)
			{
				index = i;
				break;
			}
		}
		if ((forward && index == siblings.length - 1) || (!forward && index == 0))
		{
			TreeItem parent = currentItem.getParentItem() != null ? currentItem.getParentItem() : tree.getItem(tree.indexOf(currentItem));
			siblings = getSiblings(tree, parent);
			index = Arrays.asList(siblings).indexOf(parent);

			if (forward && index == siblings.length - 1) return getNextItem(tree.getItem(0), forward);
			else if (!forward && index == 0) return getNextItem(tree.getItem(siblings.length - 1), forward);
		}

		return forward ? getNextItem(siblings[index + 1], forward) : getNextItem(siblings[index - 1], forward);
	}

	private TreeItem getNextItem(TreeItem item, boolean forward)
	{
		int idx = forward ? 0 : item.getItemCount() - 1;
		TreeItem auxItem = item;

		if (auxItem.getItemCount() == 0) return auxItem;
		while (auxItem.getItemCount() > 0)
		{
			auxItem.getParent().showItem(auxItem.getItem(idx));
			auxItem = auxItem.getItem(idx);
		}

		return auxItem;
	}

	private TreeItem[] getSiblings(Tree tree, TreeItem currentItem)
	{
		TreeItem parentItem = currentItem.getParentItem();
		if (parentItem != null) return parentItem.getItems();
		return tree.getItems();
	}


	@Override
	public Control getControl()
	{
		return composite;
	}

	@Override
	public void init(IPageSite pageSite)
	{
		super.init(pageSite);
		if (actions != null && actions.size() > 0)
		{
			IActionBars bars = pageSite.getActionBars();
			for (String id : actions.keySet())
			{
				bars.setGlobalActionHandler(id, actions.get(id));
			}
		}
		getSite().setSelectionProvider(new SelectionProviderAdapter()
		{
			@Override
			public ISelection getSelection()
			{
				ISaveablePart sourcePart = getSaveablePart();
				return sourcePart == null ? StructuredSelection.EMPTY : new StructuredSelection(sourcePart);
			}
		});
	}

	/**
	 * The <code>PropertySheetPage</code> implementation of this <code>IPage</code> method calls <code>makeContributions</code> for backwards
	 * compatibility with previous versions of <code>IPage</code>.
	 * <p>
	 * Subclasses may reimplement.
	 * </p>
	 */
	@Override
	public void setActionBars(IActionBars actionBars)
	{
		makeContributions(actionBars.getMenuManager(), actionBars.getToolBarManager(), actionBars.getStatusLineManager());
	}

	@Override
	public void setRootEntry(IPropertySheetEntry entry)
	{
		super.setRootEntry(entry);
		entry.addPropertySheetEntryListener(this);
	}

	public void childEntriesChanged(IPropertySheetEntry node)
	{
	}

	public void errorMessageChanged(IPropertySheetEntry entry)
	{
	}

	public void valueChanged(IPropertySheetEntry entry)
	{
		if (entry instanceof ModifiedPropertySheetEntry)
		{
			String text;
			IPropertySource[] propertySources = ((ModifiedPropertySheetEntry)entry).getPropertySources();
			switch (propertySources.length)
			{
				case 0 :
					text = "";
					break;

				case 1 :
					text = propertySources[0] == null ? "" : propertySources[0].toString();
					break;

				default :
					text = Messages.LabelMulipleSelections;
					break;
			}
			propertiesLabel.setText(text);
		}
	}

	@Override
	public void handleEntrySelection(ISelection selection)
	{
		super.handleEntrySelection(selection);
		if (copyValueAction != null)
		{
			copyValueAction.selectionChanged((IStructuredSelection)selection);
		}
		if (pasteValueAction != null)
		{
			pasteValueAction.selectionChanged((IStructuredSelection)selection);
		}
	}

	@Override
	public void dispose()
	{
		super.dispose();
		if (clipboard2 != null)
		{
			clipboard2.dispose();
			clipboard2 = null;
		}
		copyValueAction = null;
		pasteValueAction = null;
	}
}