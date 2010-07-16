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
package com.servoy.eclipse.ui.preferences;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import org.eclipse.dltk.ui.preferences.AbstractConfigurationBlock;
import org.eclipse.dltk.ui.preferences.OverlayPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.dialogs.I18NServerTableDialog;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Settings;

public class I18NConfigurationBlock extends AbstractConfigurationBlock
{
	private static final String DEFAULT_MESSAGES_TABLE = "defaultMessagesTable"; //$NON-NLS-1$
	private static final String DEFAULT_MESSAGES_SERVER = "defaultMessagesServer"; //$NON-NLS-1$
	private static final String TIMEZONE_DEFAULT = "timezone.default"; //$NON-NLS-1$
	private static final String LOCALE_DEFAULT = "locale.default"; //$NON-NLS-1$
	private static final String LOCALE_INTEGERFORMAT = "locale.integerformat"; //$NON-NLS-1$
	private static final String LOCALE_NUMBERFORMAT = "locale.numberformat"; //$NON-NLS-1$
	private static final String LOCALE_DATEFORMAT = "locale.dateformat"; //$NON-NLS-1$
	private static final String SELECTION_NONE = "<none>"; //$NON-NLS-1$

	private Text dateFormat;
	private Text numberFormat;
	private Text integerFormat;
	private Combo defaultLocale;
	private Combo defaultTimezone;
	private Combo defaultI18NServer;
	private Combo defaultI18NTable;
	private Button btnCreateI18NTable;

	private UserTypingMonitorListener typingListener;

	private Locale[] locales;
	private String[] timeZones;

	private final Settings settings;

	public I18NConfigurationBlock(OverlayPreferenceStore store, PreferencePage mainPreferencePage)
	{
		super(store, mainPreferencePage);
		settings = ServoyModel.getSettings();
	}

	public Control createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(3, false));

		Label labelDateFormat = new Label(composite, SWT.NONE);
		labelDateFormat.setText("Date Format"); //$NON-NLS-1$

		dateFormat = new Text(composite, SWT.BORDER);
		GridData gridData = new GridData(GridData.FILL_HORIZONTAL);
		gridData.horizontalIndent = 10;
		gridData.verticalIndent = 5;
		gridData.horizontalSpan = 2;
		dateFormat.setLayoutData(gridData);

		Label labelNumberFormat = new Label(composite, SWT.NONE);
		labelNumberFormat.setText("Number Format"); //$NON-NLS-1$

		numberFormat = new Text(composite, SWT.BORDER);
		numberFormat.setLayoutData(gridData);

		Label labelIntegerFormat = new Label(composite, SWT.NONE);
		labelIntegerFormat.setText("Integer Format"); //$NON-NLS-1$

		integerFormat = new Text(composite, SWT.BORDER);
		integerFormat.setLayoutData(gridData);

		Label labelDefaultLocale = new Label(composite, SWT.NONE);
		labelDefaultLocale.setText("Default Locale"); //$NON-NLS-1$

		defaultLocale = new Combo(composite, SWT.NULL | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(defaultLocale);
		defaultLocale.setLayoutData(gridData);

		Label labelDefaultTimezone = new Label(composite, SWT.NONE);
		labelDefaultTimezone.setText("Default Timezone"); //$NON-NLS-1$

		defaultTimezone = new Combo(composite, SWT.NULL | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(defaultTimezone);
		defaultTimezone.setLayoutData(gridData);

		Label labelDefaultI18NServer = new Label(composite, SWT.NONE);
		labelDefaultI18NServer.setText("Default I18N server:"); //$NON-NLS-1$

		defaultI18NServer = new Combo(composite, SWT.NULL | SWT.READ_ONLY);
		UIUtils.setDefaultVisibleItemCount(defaultI18NServer);
		defaultI18NServer.setLayoutData(gridData);

		Label labelDefaultI18NTable = new Label(composite, SWT.NONE);
		labelDefaultI18NTable.setText("Default I18N table:"); //$NON-NLS-1$

		defaultI18NTable = new Combo(composite, SWT.NULL);
		UIUtils.setDefaultVisibleItemCount(defaultI18NTable);
		GridData otherData = new GridData(GridData.FILL_HORIZONTAL);
		otherData.horizontalIndent = 10;
		otherData.verticalIndent = 5;
		defaultI18NTable.setLayoutData(otherData);

		btnCreateI18NTable = new Button(composite, SWT.PUSH);
		GridData btnCreateData = new GridData(GridData.HORIZONTAL_ALIGN_END);
		btnCreateData.verticalIndent = 5;
		btnCreateI18NTable.setLayoutData(btnCreateData);
		btnCreateI18NTable.setText("Create"); //$NON-NLS-1$
		btnCreateI18NTable.addListener(SWT.Selection, new Listener()
		{
			public void handleEvent(Event event)
			{
				Table table = I18NServerTableDialog.createDefaultMessagesTable(defaultI18NServer.getText(), defaultI18NTable.getText(), getShell());

				if (table != null)
				{
					// Refresh the list of tables and make sure the new table name is selected.
					populateTablesList(false);
					int index = -1;
					for (int i = 0; i < defaultI18NTable.getItemCount(); i++)
						if (defaultI18NTable.getItem(i).equals(table.getName()))
						{
							index = i;
							break;
						}
					defaultI18NTable.select(index);

					// Save the server and the table, so that they are not lost if user presses Cancel.
					settings.put(DEFAULT_MESSAGES_SERVER, defaultI18NServer.getText());
					settings.put(DEFAULT_MESSAGES_TABLE, defaultI18NTable.getText());

					// Clean up the fact that the user was typing. If the server is changed now,
					// we reset the selected table too.
					typingListener.setUserTyping(false);
				}

			}
		});

		// Prepare the array of locales. We do this only once, here, not in initializeFields,
		// which can be invoked several times.
		ArrayList<Locale> al = new ArrayList<Locale>();
		locales = Locale.getAvailableLocales();
		for (int i = 0; i < locales.length; i++)
		{
			if (locales[i].getCountry() != null && !locales[i].getCountry().equals("")) //$NON-NLS-1$
			al.add(locales[i]);
		}
		locales = new Locale[al.size()];
		locales = al.toArray(locales);
		Arrays.sort(locales, new LocaleSorter());

		// Prepare data about timezones.
		timeZones = TimeZone.getAvailableIDs();

		return composite;
	}

	@Override
	protected void initializeFields()
	{
		// Initialize date format.
		String currentDateFormat = settings.getProperty(LOCALE_DATEFORMAT);
		if (currentDateFormat != null) dateFormat.setText(currentDateFormat);
		else dateFormat.setText(""); //$NON-NLS-1$

		// Initialize number format.
		String currentNumberFormat = settings.getProperty(LOCALE_NUMBERFORMAT);
		if (currentNumberFormat != null) numberFormat.setText(currentNumberFormat);
		else numberFormat.setText(""); //$NON-NLS-1$

		// Initialize integer format
		String currentIntegerFormat = settings.getProperty(LOCALE_INTEGERFORMAT);
		if (currentIntegerFormat != null) integerFormat.setText(currentIntegerFormat);
		else integerFormat.setText(""); //$NON-NLS-1$

		// Initialize locales combobox.
		String defaultLocaleAsString = (String)settings.get(LOCALE_DEFAULT);
		Locale currentDefaultLocale = null;
		if (defaultLocaleAsString != null) currentDefaultLocale = PersistHelper.createLocale(defaultLocaleAsString);
		else currentDefaultLocale = Locale.getDefault();
		defaultLocale.removeAll();
		int selectedLocaleIndex = -1;
		for (int i = 0; i < locales.length; i++)
		{
			String displayName = locales[i].getDisplayName(locales[i]);
			defaultLocale.add(displayName);
			defaultLocale.setData(displayName, locales[i]);
			if (currentDefaultLocale.equals(locales[i])) selectedLocaleIndex = i;
		}
		defaultLocale.select(selectedLocaleIndex);

		// Initialize the time zones list.
		String currentTimeZone = settings.getProperty(TIMEZONE_DEFAULT);
		if (currentTimeZone == null) currentTimeZone = TimeZone.getDefault().getID();
		int selectedTimeZoneIndex = -1;
		for (int i = 0; i < timeZones.length; i++)
		{
			defaultTimezone.add(timeZones[i]);
			if (timeZones[i].equals(currentTimeZone)) selectedTimeZoneIndex = i;
		}
		defaultTimezone.select(selectedTimeZoneIndex);

		// Initialize server names list.
		String currentServerName = settings.getProperty(DEFAULT_MESSAGES_SERVER);
		IServerManagerInternal sm = ServoyModel.getServerManager();
		String[] serverNames = sm.getServerNames(true, true, true, false);
		defaultI18NServer.removeAll();
		defaultI18NServer.add(SELECTION_NONE);
		int selectedServerIndex = 0;
		for (int i = 0; i < serverNames.length; i++)
		{
			defaultI18NServer.add(serverNames[i]);
			if (serverNames[i].equals(currentServerName)) selectedServerIndex = i + 1;
		}
		defaultI18NServer.select(selectedServerIndex);

		typingListener = new UserTypingMonitorListener();
		populateTablesList(true);

		defaultI18NServer.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				populateTablesList(false);
			}
		});

		defaultI18NTable.addModifyListener(typingListener);
		defaultI18NTable.addSelectionListener(typingListener);
	}

	private void populateTablesList(boolean firstTime)
	{
		boolean wasUserTyping = typingListener.isUserTyping();
		String oldText = defaultI18NTable.getText();

		defaultI18NTable.removeAll();
		defaultI18NTable.add(SELECTION_NONE);
		int selectedTableIndex = 0;

		String serverName = defaultI18NServer.getText();
		String tableName = null;
		if (!serverName.equals(SELECTION_NONE) && (serverName.trim().length() > 0))
		{
			try
			{
				if (firstTime) tableName = settings.getProperty(DEFAULT_MESSAGES_TABLE);
				else tableName = oldText;
				IServerManagerInternal sm = ServoyModel.getServerManager();
				IServer server = sm.getServer(serverName);
				List<String> tableNames = server.getTableAndViewNames(true);
				Collections.sort(tableNames);

				int index = 1;
				for (int i = 0; i < tableNames.size(); i++)
				{
					String tName = tableNames.get(i);
					ITable table = server.getTable(tName);
					List<String> allcols = Arrays.asList(table.getColumnNames());
					if (!allcols.contains("message_key") || !allcols.contains("message_value") || !allcols.contains("message_language")) continue; //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$

					defaultI18NTable.add(tName);
					if (tName.equals(tableName)) selectedTableIndex = index;
					index++;
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError("Failed to retrieve table names for server '" + serverName + "'.", ex); //$NON-NLS-1$//$NON-NLS-2$
			}
		}

		if (!wasUserTyping)
		{
			if (selectedTableIndex > 0 || SELECTION_NONE.equals(tableName) || tableName == null)
			{
				defaultI18NTable.select(selectedTableIndex);
			}
			else
			{
				defaultI18NTable.setText(tableName);
			}
			typingListener.setUserTyping(false);
		}
		else
		{
			defaultI18NTable.setText(oldText);
		}
	}

	@Override
	public void performOk()
	{
		// Store the date format.
		String newDateFormat = dateFormat.getText();
		if (newDateFormat.trim().length() == 0) settings.remove(LOCALE_DATEFORMAT);
		else settings.setProperty(LOCALE_DATEFORMAT, newDateFormat);

		// Store the number format.
		String newNumberFormat = numberFormat.getText();
		if (newNumberFormat.trim().length() == 0) settings.remove(LOCALE_NUMBERFORMAT);
		else settings.setProperty(LOCALE_NUMBERFORMAT, newNumberFormat);

		// Store the integer format.
		String newIntegerFormat = integerFormat.getText();
		if (newIntegerFormat.trim().length() == 0) settings.remove(LOCALE_INTEGERFORMAT);
		else settings.setProperty(LOCALE_INTEGERFORMAT, newIntegerFormat);

		// Store the locale.
		Locale newLocale = (Locale)defaultLocale.getData(defaultLocale.getText());
		settings.put(LOCALE_DEFAULT, PersistHelper.createLocaleString(newLocale));

		// Store the time zone.
		String newTimeZone = defaultTimezone.getText();
		settings.put(TIMEZONE_DEFAULT, newTimeZone);

		// Store the default I18N server and table name.
		if (defaultI18NServer.getText().equals(SELECTION_NONE)) settings.put(DEFAULT_MESSAGES_SERVER, ""); //$NON-NLS-1$
		else settings.put(DEFAULT_MESSAGES_SERVER, defaultI18NServer.getText());
		if (defaultI18NTable.getText().equals(SELECTION_NONE)) settings.put(DEFAULT_MESSAGES_TABLE, ""); //$NON-NLS-1$
		else settings.put(DEFAULT_MESSAGES_TABLE, defaultI18NTable.getText());

		// refresh the i18n messages from the design client so that the visual designer will use new messages if changed 
		Activator.getDefault().getDesignClient().refreshI18NMessages();
	}

	private class LocaleSorter implements Comparator<Locale>
	{
		private final Locale displayLocale;

		LocaleSorter()
		{
			displayLocale = Locale.getDefault();
		}

		public int compare(Locale o1, Locale o2)
		{
			String name1 = o1.getDisplayName(displayLocale);
			String name2 = o2.getDisplayName(displayLocale);
			return name1.compareToIgnoreCase(name2);
		}
	}

	private class UserTypingMonitorListener implements SelectionListener, ModifyListener
	{
		private boolean isUserTyping = false;

		public void widgetDefaultSelected(SelectionEvent e)
		{
			isUserTyping = false;
		}

		public void widgetSelected(SelectionEvent e)
		{
			isUserTyping = false;
		}

		public void modifyText(ModifyEvent e)
		{
			isUserTyping = true;
		}

		public boolean isUserTyping()
		{
			return isUserTyping;
		}

		public void setUserTyping(boolean isUserTyping)
		{
			this.isUserTyping = isUserTyping;
		}
	}
}
