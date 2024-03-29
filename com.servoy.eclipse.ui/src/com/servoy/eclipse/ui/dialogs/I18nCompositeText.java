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

import java.util.Collection;
import java.util.Comparator;
import java.util.Iterator;
import java.util.Locale;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.table.ColumnsSorter;
import com.servoy.eclipse.ui.util.FilterDelayJob;
import com.servoy.eclipse.ui.util.FilteredEntity;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.i18n.I18NMessagesModel;
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Settings;

public class I18nCompositeText extends Composite
{
	private static final String DIALOGSTORE_FILTER = "I18NComposite.filter";
	private static final String DIALOGSTORE_LANG_COUNTRY = "I18NComposite.lang_country";
	private static final String COLUMN_WIDTHS = "I18NComposite.table.widths";
	private static final String KEY_COLUMN = "key.column";
	private static final String DEFAULT_COLUMN = "default.column";
	private static final String LOCALE_COLUMN = "locale.column";

	private static final int DEFAULT_WIDTH = 100;
	private static final int MIN_COLUMN_WIDTH = 10;

	public static final int CI_KEY = 0;
	public static final int CI_DEFAULT = 1;
	public static final int CI_LOCALE = 2;
	public static final int CI_COPY = 3;
	public static final int CI_DELETE = 4;

	private static final long FILTER_TYPE_DELAY = 300;

	public class I18nTableLabelProvider extends LabelProvider implements ITableLabelProvider
	{
		public Image getColumnImage(Object element, int columnIndex)
		{
			if (columnIndex == CI_COPY)
			{
				return PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_COPY);
			}
			if (columnIndex == CI_DELETE)
			{
				return Activator.getDefault().loadImageFromBundle("delete.png");
			}
			return null;
		}

		public String getColumnText(Object element, int columnIndex)
		{
			I18NMessagesModelEntry val = (I18NMessagesModelEntry)element;
			switch (columnIndex)
			{
				case CI_KEY :
					return val.key;

				case CI_DEFAULT :
					return val.defaultvalue;

				case CI_LOCALE :
					return val.localeValue;
				case CI_DELETE :
					return "";
				case CI_COPY :
					return "";
			}
			return "";
		}
	}

	private TableViewer tableViewer;
	private Locale selectedLanguage;
	private I18NMessagesModel messagesModel;
	private final IApplication application;
	private IDialogSettings dialogSettings;
	private String lastAutoSelectedKey;
	private FilterDelayJob delayedFilterJob;
	private Composite tableContainer;
	private final String i18nDatasource;
	private ISelectionChangedListener selectionChangedListener;

	public I18nCompositeText(Composite parent, int style, IApplication application)
	{
		this(parent, style, application, null);
	}


	public I18nCompositeText(Composite parent, int style, IApplication application, String i18nDatasource)
	{
		super(parent, style);
		this.application = application;
		this.i18nDatasource = i18nDatasource;
		initialise();
	}

	private void initialise()
	{
		dialogSettings = Activator.getDefault().getDialogSettings();
		String initialFilter = dialogSettings.get(DIALOGSTORE_FILTER);
		if (initialFilter == null) initialFilter = "";
		delayedFilterJob = new FilterDelayJob(new FilteredEntity()
		{

			public void filter(final String filterValue, IProgressMonitor monitor)
			{
				dialogSettings.put(DIALOGSTORE_FILTER, filterValue);
				if (Display.getCurrent() != null)
				{
					fill("".equals(filterValue) ? null : filterValue);
					selectKey(lastAutoSelectedKey);
				}
				else
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							fill("".equals(filterValue) ? null : filterValue);
							selectKey(lastAutoSelectedKey);
						}
					});
				}
			}

		}, FILTER_TYPE_DELAY, "Filtering");

		tableContainer = new Composite(this, SWT.NONE);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new I18nTableLabelProvider());

		TableColumn keyColumn = new TableColumn(tableViewer.getTable(), SWT.NONE);
		keyColumn.setText("key");

		TableColumn defaultColumn = new TableColumn(tableViewer.getTable(), SWT.NONE);
		defaultColumn.setText("default");

		keyColumn.setWidth(263);
		defaultColumn.setWidth(263);
		tableContainer.setLayout(new FillLayout());

		setLayout(new FillLayout());

		String lang_country = dialogSettings.get(DIALOGSTORE_LANG_COUNTRY);
		if (lang_country != null)
		{
			String[] split = lang_country.split("_");
			if (split.length == 1)
			{
				selectedLanguage = new Locale(split[0]);
			}
			else if (split.length == 2)
			{
				selectedLanguage = new Locale(split[0], split[1]);
			}
		}
		selectedLanguage = selectedLanguage != null ? selectedLanguage
			: new Locale(application.getLocale().getLanguage(), "", application.getLocale().getVariant());

		IApplicationServerSingleton appServer = ApplicationServerRegistry.get();
		ServoyProject ap = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		Solution appSolution = ap != null ? ap.getEditingSolution() : null;
		messagesModel = new I18NMessagesModel(i18nDatasource != null ? i18nDatasource : DataSourceUtils.getI18NDataSource(appSolution, Settings.getInstance()),
			appServer.getClientId(), Settings.getInstance(), appServer.getDataServer(), appServer.getLocalRepository());
		messagesModel.setLanguage(selectedLanguage);

		fill("".equals(initialFilter) ? null : initialFilter);

		tableViewer.setComparator(new ColumnsSorter(tableViewer, new TableColumn[] { keyColumn, defaultColumn },
			new Comparator[] { I18NEditorKeyColumnComparator.INSTANCE, I18NEditorDefaultColumnComparator.INSTANCE }));

		addDisposeListener(new DisposeListener()
		{

			@Override
			public void widgetDisposed(DisposeEvent e)
			{
				if (delayedFilterJob != null)
				{
					delayedFilterJob.cancel();
					delayedFilterJob = null;
				}
				dialogSettings.put(DIALOGSTORE_LANG_COUNTRY, selectedLanguage.toString());
			}
		});
	}

	protected void fill(String filter)
	{
		tableViewer.setInput(messagesModel.getMessages(filter, null, null, null,
			ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile(), null));
	}

	public void handleFilterChanged(String text)
	{
		// only apply filter when user stops typing for 300 ms
		delayedFilterJob.setFilterText(text);
	}

	public TableViewer getTableViewer()
	{
		return tableViewer;
	}

	public String getSelectedKey()
	{
		I18NMessagesModelEntry row = (I18NMessagesModelEntry)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
		return row == null ? null : "i18n:" + row.key;
	}

	public I18NMessagesModelEntry getSelectedRow()
	{
		return (I18NMessagesModelEntry)((IStructuredSelection)tableViewer.getSelection()).getFirstElement();
	}

	public Locale getSelectedLanguage()
	{
		return selectedLanguage;
	}


	public void setSelectionChangedListener(ISelectionChangedListener selectionChangedListener)
	{
		if (this.selectionChangedListener != null)
		{
			tableViewer.removeSelectionChangedListener(this.selectionChangedListener);
		}
		this.selectionChangedListener = selectionChangedListener;
		if (this.selectionChangedListener != null)
		{
			tableViewer.addSelectionChangedListener(this.selectionChangedListener);
		}
	}

	public void selectKey(String value)
	{
		lastAutoSelectedKey = value;
		boolean found = false;
		if (value != null && value.startsWith("i18n:"))
		{
			String key = value.substring(5);

			StructuredSelection currentSelection = (StructuredSelection)tableViewer.getSelection();
			if (currentSelection.size() != 1 || !((I18NMessagesModelEntry)currentSelection.getFirstElement()).key.equals(key))
			{
				Collection<I18NMessagesModelEntry> contents = (Collection<I18NMessagesModelEntry>)tableViewer.getInput();
				Iterator<I18NMessagesModelEntry> it = contents.iterator();
				while (it.hasNext() && !found)
				{
					I18NMessagesModelEntry entry = it.next();
					if (entry.key.equals(key))
					{
						found = true;
						tableViewer.setSelection(new StructuredSelection(entry));
						tableViewer.reveal(entry);
					}
				}
			}
			else
			{
				found = true;
			}
		}

		// don't reset the selection, then keep it what it is (for the cell editor )
//		if (!found)
//		{
//			tableViewer.setSelection(new StructuredSelection());
//		}
	}

	public void saveTableColumnWidths()
	{
		IDialogSettings tableSettings = dialogSettings.getSection(COLUMN_WIDTHS);
		if (tableSettings == null) tableSettings = dialogSettings.addNewSection(COLUMN_WIDTHS);
		int val = tableViewer.getTable().getColumn(CI_KEY).getWidth();
		if (val >= MIN_COLUMN_WIDTH)
		{
			tableSettings.put(KEY_COLUMN, val);
		}
		val = tableViewer.getTable().getColumn(CI_DEFAULT).getWidth();
		if (val >= MIN_COLUMN_WIDTH)
		{
			tableSettings.put(DEFAULT_COLUMN, val);
		}
		val = tableViewer.getTable().getColumn(CI_LOCALE).getWidth();
		if (val >= MIN_COLUMN_WIDTH)
		{
			tableSettings.put(LOCALE_COLUMN, val);
		}
	}

	public static class I18NEditorKeyColumnComparator implements Comparator
	{
		public static final I18NEditorKeyColumnComparator INSTANCE = new I18NEditorKeyColumnComparator();

		public int compare(Object o1, Object o2)
		{
			return I18nCompositeText.compareHelper(o1, o2, "key");
		}
	}

	public static class I18NEditorDefaultColumnComparator implements Comparator
	{
		public static final I18NEditorDefaultColumnComparator INSTANCE = new I18NEditorDefaultColumnComparator();

		public int compare(Object o1, Object o2)
		{
			return I18nCompositeText.compareHelper(o1, o2, "defaultvalue");
		}
	}

	public static class I18NEditorLocaleColumnComparator implements Comparator
	{
		public static final I18NEditorLocaleColumnComparator INSTANCE = new I18NEditorLocaleColumnComparator();

		public int compare(Object o1, Object o2)
		{
			return I18nCompositeText.compareHelper(o1, o2, "localeValue");
		}
	}

	private static String[] getI18NEntryValues(I18NMessagesModel.I18NMessagesModelEntry e1, I18NMessagesModel.I18NMessagesModelEntry e2, String what2get)
	{
		String s1 = null;
		String s2 = null;
		if (what2get.equalsIgnoreCase("key"))
		{
			s1 = e1.key;
			s2 = e2.key;
		}
		else if (what2get.equalsIgnoreCase("localeValue"))
		{
			s1 = e1.localeValue;
			s2 = e2.localeValue;
		}
		else if (what2get.equalsIgnoreCase("defaultvalue"))
		{
			s1 = e1.defaultvalue;
			s2 = e2.defaultvalue;
		}
		return new String[] { s1, s2 };
	}

	private static int compareHelper(Object o1, Object o2, String what2compare)
	{
		if (o1 instanceof I18NMessagesModel.I18NMessagesModelEntry && o2 instanceof I18NMessagesModel.I18NMessagesModelEntry)
		{
			I18NMessagesModel.I18NMessagesModelEntry entry1 = (I18NMessagesModel.I18NMessagesModelEntry)o1;
			I18NMessagesModel.I18NMessagesModelEntry entry2 = (I18NMessagesModel.I18NMessagesModelEntry)o2;

			String[] values = getI18NEntryValues(entry1, entry2, what2compare);
			String locale1 = values[0];
			String locale2 = values[1];

			if (locale1 == null && locale2 == null)
			{
				return 0;
			}
			else if (locale1 == null)
			{
				return -1;
			}
			else if (locale2 == null)
			{
				return 1;
			}
			return locale1.compareToIgnoreCase(locale2);
		}
		else if (o1 == null && o2 == null)
		{
			return 0;
		}
		else if (o1 == null)
		{
			return -1;
		}
		else
		{
			return 1;
		}
	}
}
