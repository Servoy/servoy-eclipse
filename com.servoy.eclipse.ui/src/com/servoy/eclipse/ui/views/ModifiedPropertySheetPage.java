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

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
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
import org.eclipse.ui.views.properties.IPropertySheetEntry;
import org.eclipse.ui.views.properties.IPropertySheetEntryListener;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetSorter;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.DialogCellEditor;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
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
					Comparable comparableA = (Comparable)((IAdaptable)entryA).getAdapter(Comparable.class);
					if (comparableA != null)
					{
						Comparable comparableB = (Comparable)((IAdaptable)entryB).getAdapter(Comparable.class);
						if (comparableB != null)
						{
							return comparableA.compareTo(comparableB);
						}
					}
				}
				return super.compare(entryA, entryB);
			}
		});
		getSite().setSelectionProvider(new SelectionProviderAdapter()
		{
			@Override
			public ISelection getSelection()
			{
				ISaveablePart sourcePart = getSaveablePart();
				return sourcePart == null ? StructuredSelection.EMPTY : new StructuredSelection(sourcePart);
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
			if (Boolean.parseBoolean(ServoyModel.getSettings().getProperty("servoy.developer.slowproperties.workaround", String.valueOf(Utils.isAppleMacOS())))) //$NON-NLS-1$
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
								String text = null;
								if ((RepositoryHelper.getDisplayName(StaticContentSpecLoader.PROPERTY_ONFOCUSLOSTMETHODID.getPropertyName(), null).equals(
									item.getText(0)) || RepositoryHelper.getDisplayName(
									StaticContentSpecLoader.PROPERTY_ONELEMENTFOCUSLOSTMETHODID.getPropertyName(), null).equals(item.getText(0))) ||
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
			};
			tree.addListener(SWT.Dispose, treeListener);
			tree.addListener(SWT.KeyDown, treeListener);
			tree.addListener(SWT.MouseMove, treeListener);
			tree.addListener(SWT.MouseHover, treeListener);

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
		IWorkbenchPart activeEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPartService().getActivePart();
		if (activeEditor != null)
		{
			ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
			if (selectionProvider != null)
			{
				selectionChanged(activeEditor, selectionProvider.getSelection());
			}
		}
	}

	@Override
	public Control getControl()
	{
		return composite;
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
