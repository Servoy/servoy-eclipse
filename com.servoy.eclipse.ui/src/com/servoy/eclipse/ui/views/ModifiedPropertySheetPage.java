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
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ControlAdapter;
import org.eclipse.swt.events.ControlEvent;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.swt.widgets.TreeItem;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISaveablePart;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetEntryListener;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

/**
 * PropertySheetPage with additional Servoy features.
 *
 * @author acostescu
 */

public class ModifiedPropertySheetPage extends PropertySheetPage implements IPropertySheetEntryListener
{
	private Composite composite;
	private Label propertiesLabel;
	private final Map<String, IAction> actions;

	public ModifiedPropertySheetPage(Map<String, IAction> actions)
	{
		super();
		this.actions = actions;
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
		});

		Control control = super.getControl();
		if (control instanceof Tree)
		{
			final Tree tree = (Tree)control;
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

			final Listener labelListener = new Listener()
			{
				public void handleEvent(Event event)
				{
					Label label = (Label)event.widget;
					Shell shell = label.getShell();
					switch (event.type)
					{
						case SWT.MouseDown :
							Event e = new Event();
							e.item = (TreeItem)label.getData("_TREEITEM");
							// set the selection as if
							// the mouse down event went through to the tree
							tree.setSelection(new TreeItem[] { (TreeItem)e.item });
							tree.notifyListeners(SWT.Selection, e);
							shell.dispose();
							tree.setFocus();
							break;
						case SWT.MouseExit :
							shell.dispose();
							break;
					}
				}
			};

			Listener treeListener = new Listener()
			{
				Shell tip = null;
				Label label = null;

				public void handleEvent(Event event)
				{
					switch (event.type)
					{
						case SWT.Dispose :
						case SWT.KeyDown :
						case SWT.MouseMove :
						{
							if (tip == null) break;
							tip.dispose();
							tip = null;
							label = null;
							break;
						}
						case SWT.MouseHover :
						{
							TreeItem item = tree.getItem(new Point(event.x, event.y));
							if (item != null)
							{
								String text = getTooltipText(item);

								if ((RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName(), null).equals(
									item.getText(0)) ||
									RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID.getPropertyName(), null).equals(
										item.getText(0))) ||
									RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONDATACHANGEMETHODID.getPropertyName(), null).equals(
										item.getText(0)))
								{
									text = "Warning: Do not use dialogs, as a dialog will interfere with focus";
								}
								if (StaticContentSpecLoader.PROPERTY_TITLETEXT.getPropertyName().equals(item.getText(0)))
								{
									text = "Set value <empty> for no title text";
								}
								if (StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName().equals(item.getText(0)))
								{
									text = "Set z-index of the element.";
								}
								if (StaticContentSpecLoader.PROPERTY_ROLLOVERCURSOR.getPropertyName().equals(item.getText(0)) ||
									StaticContentSpecLoader.PROPERTY_ROLLOVERIMAGEMEDIAID.getPropertyName().replace("ID", "").equals(item.getText(0)))
								{
									text = "Not supported in smart client for listview/tableview";
								}
								if ("width".equals(item.getText(0)) || "height".equals(item.getText(0)))
								{
									ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getSite()
										.getSelectionProvider().getSelection();
									if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() == 1 &&
										Platform.getAdapterManager().getAdapter(((IStructuredSelection)selection).getFirstElement(),
											IPersist.class) instanceof IFormElement)
									{
										if ("height".equals(item.getText(0)))
										{
											text = "If top and bottom are set (anchored) this is minimum height.";
										}
										if ("width".equals(item.getText(0)))
										{
											text = "If left and right are set (anchored) this is minimum width.";
										}
									}
								}
								if (text != null)
								{
									if (tip != null && !tip.isDisposed()) tip.dispose();
									tip = new Shell(tree.getShell(), SWT.ON_TOP | SWT.NO_FOCUS | SWT.TOOL);
									tip.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
									FillLayout layout = new FillLayout();
									layout.marginWidth = 2;
									tip.setLayout(layout);
									label = new Label(tip, SWT.NONE);
									label.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_FOREGROUND));
									label.setBackground(Display.getCurrent().getSystemColor(SWT.COLOR_INFO_BACKGROUND));
									label.setData("_TREEITEM", item);
									label.setText(text);
									label.addListener(SWT.MouseExit, labelListener);
									label.addListener(SWT.MouseDown, labelListener);
									Point size = tip.computeSize(SWT.DEFAULT, SWT.DEFAULT);
									Rectangle rect = item.getBounds(0);
									Point pt = tree.toDisplay(rect.x, rect.y);
									tip.setBounds(pt.x, pt.y, size.x, size.y);
									tip.setVisible(true);
								}
							}
						}
					}
				}

				/**
				 * This method will get the tool tip text of the selected property.
				 *
				 * @param item the selected property
				 * @return the tool tip text
				 */
				private String getTooltipText(TreeItem item)
				{
					Object selectionObject;
					ISelection selection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getActiveEditor().getSite()
						.getSelectionProvider().getSelection();
					Iterator< ? > iterator = ((IStructuredSelection)selection).iterator();
					// iterate over all selected components
					while (iterator.hasNext())
					{
						selectionObject = iterator.next();
						IPersist persist = Platform.getAdapterManager().getAdapter(selectionObject, IPersist.class);
						IPersist finalPersist = (persist == null || persist instanceof IFormElement) ? persist : persist.getParent();
						if (finalPersist instanceof IFormElement)
						{
							// get the specification file
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
								.getWebComponentSpecification(FormTemplateGenerator.getComponentTypeName((IFormElement)finalPersist));

							String tooltipText = null;

							// search for a model property to match the selected item
							PropertyDescription pdModel = spec.getProperty(item.getText());
							if (pdModel != null)
							{
								Object tag = pdModel.getTag("tooltipText");
								if (tag != null)
								{
									return tag.toString();
								}
							}

							// check custom properties
							Map<String, PropertyDescription> customProperties = spec.getCustomJSONProperties();
							if (customProperties != null)
							{
								tooltipText = getPropertyTooltipText(customProperties, item);
								if (tooltipText != null)
								{
									return tooltipText;
								}
							}

							// check functions and handlers
							tooltipText = tooltipTextForFunctionsAndHandlers(item, spec.getApiFunctions());
							return (tooltipText != null) ? tooltipText : tooltipTextForFunctionsAndHandlers(item, spec.getHandlers());
						}
					}
					return null;
				}

				/**
				 * Iterates over the custom properties and gets the tool tip text (if defined)
				 *
				 * @param properties the custom properties
				 * @param item the selected property
				 * @return the tool tip text
				 */
				private String getPropertyTooltipText(Map<String, PropertyDescription> properties, TreeItem item)
				{
					String result = null;
					Iterator<Entry<String, PropertyDescription>> it = properties.entrySet().iterator();
					while (it.hasNext())
					{
						PropertyDescription propertyDescription = it.next().getValue();
						if (propertyDescription.getName().equals(item.getText()))
						{
							Object tag = propertyDescription.getTag("tooltipText");
							if (tag != null)
							{
								return tag.toString();
							}
						}

						// check for other properties recursively
						Map<String, PropertyDescription> internalProperties = propertyDescription.getProperties();
						if (internalProperties != null && !internalProperties.isEmpty())
						{
							result = getPropertyTooltipText(propertyDescription.getProperties(), item);
							if (result != null)
							{
								return result;
							}
						}
					}
					return null;
				}

				/**
				 * Iterates over functions or handlers and gets the tool tip text.
				 *
				 * @param item the selected property
				 * @param it the iterator for functions or handlers
				 * @return the tool tip text
				 */
				private String tooltipTextForFunctionsAndHandlers(TreeItem item, Map<String, WebObjectFunctionDefinition> values)
				{
					if (values != null)
					{
						Iterator<Entry<String, WebObjectFunctionDefinition>> it = values.entrySet().iterator();
						while (it.hasNext())
						{
							PropertyDescription propertyDescription = it.next().getValue().getAsPropertyDescription();
							if (propertyDescription.getName().contains(item.getText()))
							{
								try
								{
									JSONObject jsonObject = new JSONObject(propertyDescription.getConfig().toString());
									return jsonObject.getString("tooltipText");
								}
								catch (JSONException err)
								{
									Debug.log("Exception while parsing the json object: " + err);
								}
							}
						}
					}

					return null;
				}
			};
			tree.addListener(SWT.Dispose, treeListener);
			tree.addListener(SWT.KeyDown, treeListener);
			tree.addListener(SWT.MouseMove, treeListener);
			tree.addListener(SWT.MouseHover, treeListener);

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
}