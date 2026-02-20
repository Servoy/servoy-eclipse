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
import java.util.Collection;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyAdapter;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ai.ChatModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.editors.table.ColumnsSorter;
import com.servoy.eclipse.ui.util.FilterDelayJob;
import com.servoy.eclipse.ui.util.FilteredEntity;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.i18n.I18NMessagesModel;
import com.servoy.j2db.i18n.I18NMessagesModel.I18NMessagesModelEntry;
import com.servoy.j2db.persistence.I18NUtil.MessageEntry;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.server.shared.IApplicationServerSingleton;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class I18nComposite extends Composite
{

	private static final String DEFAULT_MESSAGES_TABLE = "defaultMessagesTable";
	private static final String DEFAULT_MESSAGES_SERVER = "defaultMessagesServer";
	private static final int LANG_COMBO_WIDTH = 128;
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

	private static final long FILTER_TYPE_DELAY = 500;

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
	private Text filterText;
	private Label filterLabel;
	private Label languageLabel;
	private ComboViewer languageComboViewer;
	private ComboViewer countryComboViewer;
	private Locale selectedLanguage;
	private I18NMessagesModel messagesModel;
	private final IApplication application;
	private ISelectionChangedListener languageSelectionHandler;
	private ISelectionChangedListener countrySelectionHandler;
	private IDialogSettings dialogSettings;
	private String lastAutoSelectedKey;
	private FilterDelayJob delayedFilterJob;
	private Composite tableContainer;
	private Composite centerPanel;
	private final String i18nDatasource;
	private ISelectionChangedListener selectionChangedListener;

	private final boolean showAISuggestion;

	private Label newAIKeyLabel;
	private Label newKeyLabel;
	private Label newDefaultLabel;
	private Composite newTextFieldsComposite;
	private Text newKeyTextField;
	private Text newDefaultTextField;
	private Button newKeyButton;

	// Cached chat model for AI suggestions (both key generation and translation)
	private ChatModel aiSuggestionModel;

	// Dynamic locale fields
	private org.eclipse.swt.custom.ScrolledComposite localeScrolledComposite;
	private Composite localeFieldsComposite;
	private Map<String, Label> localeLabels;
	private Map<String, Text> localeTextFields;

	// New locale controls
	private Button addLocaleButton;
	private List<Locale> currentLocalesList;

	// Font for italic styling when isNew is true
	private Font italicFont;


	public I18nComposite(Composite parent, int style, IApplication application, boolean addActionColumns)
	{
		this(parent, style, application, addActionColumns, null);
	}


	public I18nComposite(Composite parent, int style, IApplication application, boolean addActionColumns, String i18nDatasource)
	{
		this(parent, style, application, addActionColumns, i18nDatasource, false);
	}

	public I18nComposite(Composite parent, int style, IApplication application, boolean addActionColumns, String i18nDatasource, boolean showAISuggestion)
	{
		super(parent, style);
		this.application = application;
		String vI18NDatasource = i18nDatasource;
		if (vI18NDatasource == null)
		{
			IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
			ServoyProject activeProject = servoyModel.getActiveProject();
			if (activeProject != null)
			{
				Solution activeSolution = activeProject.getEditingSolution();
				String serverName = activeSolution.getI18nServerName();
				String tableName = activeSolution.getI18nTableName();
				if (serverName == null || serverName.trim().length() == 0 || tableName == null || tableName.trim().length() == 0)
				{
					Settings settings = Settings.getInstance();
					serverName = settings.getProperty(DEFAULT_MESSAGES_SERVER);
					tableName = settings.getProperty(DEFAULT_MESSAGES_TABLE);
				}
				if (serverName != null && serverName.trim().length() != 0 && tableName != null && tableName.trim().length() != 0)
				{
					vI18NDatasource = DataSourceUtils.createDBTableDataSource(serverName, tableName);
				}
			}
		}
		this.i18nDatasource = vI18NDatasource;
		this.showAISuggestion = showAISuggestion;
		initialise(addActionColumns);
	}

	private void initialise(boolean addActionColumns)
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
					fillTableOnly("".equals(filterValue) ? null : filterValue);
					selectKey("i18n:" + filterValue);
				}
				else
				{
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							fillTableOnly("".equals(filterValue) ? null : filterValue);
							selectKey("i18n:" + filterValue);
						}
					});
				}
			}

		}, FILTER_TYPE_DELAY, "Filtering");

		centerPanel = new Composite(this, SWT.NONE);
		tableContainer = new Composite(centerPanel, SWT.NONE);
		tableViewer = new TableViewer(tableContainer, SWT.V_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.setContentProvider(new ArrayContentProvider());
		tableViewer.setLabelProvider(new I18nTableLabelProvider());
		filterText = new Text(this, SWT.BORDER);
		filterText.setText(initialFilter);
		filterText.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				handleFilterChanged();
			}
		});
		filterText.addKeyListener(new KeyAdapter()
		{
			@Override
			public void keyReleased(KeyEvent event)
			{
				if (event.keyCode == SWT.CR)
				{
					handleFilterChanged();
				}
			}
		});

		filterLabel = new Label(this, SWT.RIGHT);
		filterLabel.setText("Filter");
		languageLabel = new Label(this, SWT.RIGHT);
		languageLabel.setText("Language/country");

		languageComboViewer = new ComboViewer(this);
		languageComboViewer.setContentProvider(new ArrayContentProvider());


		countryComboViewer = new ComboViewer(this);
		countryComboViewer.setContentProvider(new ArrayContentProvider());

		TableColumn keyColumn = new TableColumn(tableViewer.getTable(), SWT.NONE);
		keyColumn.setText("key");

		TableColumn defaultColumn = new TableColumn(tableViewer.getTable(), SWT.NONE);
		defaultColumn.setText("default");

		TableColumn localeColumn = new TableColumn(tableViewer.getTable(), SWT.NONE);
		localeColumn.setText("locale");
		TableColumn delColumn = null;
		TableColumn copyColumn = null;
		if (addActionColumns)
		{
			copyColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_COPY);
			copyColumn.setToolTipText("Copy key");

			delColumn = new TableColumn(tableViewer.getTable(), SWT.CENTER, CI_DELETE);
			delColumn.setToolTipText("Delete key");
		}
		IDialogSettings tableSettings = dialogSettings.getSection(COLUMN_WIDTHS);
		if (tableSettings == null || addActionColumns)
		{
			final TableColumnLayout layout = new TableColumnLayout();
			tableContainer.setLayout(layout);
			layout.setColumnData(keyColumn, new ColumnWeightData(10, 50, true));
			layout.setColumnData(defaultColumn, new ColumnWeightData(10, 50, true));
			layout.setColumnData(localeColumn, new ColumnWeightData(10, 50, true));
			if (delColumn != null) layout.setColumnData(delColumn, new ColumnPixelData(20, true));
			if (copyColumn != null) layout.setColumnData(copyColumn, new ColumnPixelData(20, true));
		}
		else
		{
			int keyWidth = DEFAULT_WIDTH;
			int defaultWidth = DEFAULT_WIDTH;
			int localeWidth = DEFAULT_WIDTH;
			try
			{
				keyWidth = tableSettings.getInt(KEY_COLUMN);
				defaultWidth = tableSettings.getInt(DEFAULT_COLUMN);
				localeWidth = tableSettings.getInt(LOCALE_COLUMN);
				if (keyWidth < MIN_COLUMN_WIDTH) keyWidth = MIN_COLUMN_WIDTH;
				if (defaultWidth < MIN_COLUMN_WIDTH) defaultWidth = MIN_COLUMN_WIDTH;
				if (localeWidth < MIN_COLUMN_WIDTH) localeWidth = MIN_COLUMN_WIDTH;
			}
			catch (Exception ex)
			{
				// just ignore, an exception just means that the setting wasnt there yet.
			}
			keyColumn.setWidth(keyWidth);
			localeColumn.setWidth(localeWidth);
			defaultColumn.setWidth(defaultWidth);
			tableContainer.setLayout(new FillLayout());
		}

		if (showAISuggestion)
		{
			// Initialize dynamic field maps
			localeLabels = new HashMap<>();
			localeTextFields = new HashMap<>();

			// Ensure all controls are created with 'this' as parent
			newAIKeyLabel = new Label(this, SWT.NONE);
			newAIKeyLabel.setText("Edit or create new key. Words in italic are AI generated.");

			// Create a single composite for all AI input controls with proper alignment
			newTextFieldsComposite = new Composite(this, SWT.NONE);
			GroupLayout aiLayout = new GroupLayout(newTextFieldsComposite);
			newTextFieldsComposite.setLayout(aiLayout);

			// Create labels
			newKeyLabel = new Label(newTextFieldsComposite, SWT.NONE);
			newKeyLabel.setText("key");
			newDefaultLabel = new Label(newTextFieldsComposite, SWT.NONE);
			newDefaultLabel.setText("default");

			// Create text fields and button
			newKeyTextField = new Text(newTextFieldsComposite, SWT.BORDER);
			newDefaultTextField = new Text(newTextFieldsComposite, SWT.BORDER);
			newKeyButton = new Button(newTextFieldsComposite, SWT.PUSH | SWT.CENTER);
			newKeyButton.setText("Save");
			newKeyButton.setAlignment(SWT.CENTER);
			newKeyButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter()
			{
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
				{
					handleNewKeyButtonClick();
				}
			});

			// Horizontal layout: 2 columns (key, default) with equal width, plus button
			aiLayout.setHorizontalGroup(
				aiLayout.createSequentialGroup()
					.addContainerGap()
					.add(aiLayout.createParallelGroup(GroupLayout.LEADING)
						.add(newKeyLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(newKeyTextField, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(aiLayout.createParallelGroup(GroupLayout.LEADING)
						.add(newDefaultLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(newDefaultTextField, GroupLayout.DEFAULT_SIZE, 150, Short.MAX_VALUE))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(newKeyButton, 80, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
					.addContainerGap());

			// Vertical layout: labels on top, text fields below (aligned)
			aiLayout.setVerticalGroup(
				aiLayout.createSequentialGroup()
					.addContainerGap()
					.add(aiLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(newKeyLabel)
						.add(newDefaultLabel))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(aiLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(newKeyTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(newDefaultTextField, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(newKeyButton, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.addContainerGap());

			// Create scrollable composite for dynamic locale fields
			localeScrolledComposite = new org.eclipse.swt.custom.ScrolledComposite(this, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER);
			localeScrolledComposite.setExpandHorizontal(true);
			localeScrolledComposite.setExpandVertical(true);

			// Create container for dynamic locale fields
			localeFieldsComposite = new Composite(localeScrolledComposite, SWT.NONE);
			localeScrolledComposite.setContent(localeFieldsComposite);

			// Create dynamic fields for each locale
			createDynamicLocaleFields();

			addLocaleButton = new Button(this, SWT.PUSH);
			addLocaleButton.setText("Add Locale");
			addLocaleButton.addSelectionListener(new org.eclipse.swt.events.SelectionAdapter()
			{
				@Override
				public void widgetSelected(org.eclipse.swt.events.SelectionEvent e)
				{
					addNewLocale();
				}
			});
		}

		GroupLayout centerPanelLayout = new GroupLayout(centerPanel);
		centerPanel.setLayout(centerPanelLayout);
		centerPanelLayout.setHorizontalGroup(centerPanelLayout.createSequentialGroup().add(tableContainer, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));
		centerPanelLayout.setVerticalGroup(centerPanelLayout.createSequentialGroup().add(tableContainer, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE));

		setLayout(getLayout(centerPanel, languageComboViewer.getCombo(), countryComboViewer.getCombo(), showAISuggestion ? newAIKeyLabel : null,
			showAISuggestion ? newTextFieldsComposite : null,
			showAISuggestion ? localeScrolledComposite : null,
			showAISuggestion ? addLocaleButton : null));

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

		tableViewer.setComparator(new ColumnsSorter(tableViewer, new TableColumn[] { keyColumn, defaultColumn, localeColumn },
			new Comparator[] { I18NEditorKeyColumnComparator.INSTANCE, I18NEditorDefaultColumnComparator.INSTANCE, I18NEditorLocaleColumnComparator.INSTANCE }));

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

				// Dispose the italic font if it was created
				if (italicFont != null && !italicFont.isDisposed())
				{
					italicFont.dispose();
					italicFont = null;
				}

				// Clear cached chat model
				aiSuggestionModel = null;
			}
		});
	}

	private Font getItalicFont()
	{
		if (italicFont == null)
		{
			// Get the default font from one of the text fields (or from Display)
			Font defaultFont = newKeyTextField != null && !newKeyTextField.isDisposed() ? newKeyTextField.getFont() : Display.getCurrent().getSystemFont();

			// Create italic font based on the default font
			italicFont = new Font(Display.getCurrent(), defaultFont.getFontData()[0].getName(),
				defaultFont.getFontData()[0].getHeight(), SWT.ITALIC);
		}
		return italicFont;
	}

	private void updateLocaleValues(String key, String newValue, boolean isNewKey)
	{
		boolean isNew = isNewKey;

		// Clear all text fields and set placeholder text "Loading..." at the beginning
		if (newKeyTextField != null && !newKeyTextField.isDisposed())
		{
			newKeyTextField.setText("");
			newKeyTextField.setMessage("Loading...");
		}

		if (newDefaultTextField != null && !newDefaultTextField.isDisposed())
		{
			newDefaultTextField.setText("");
			newDefaultTextField.setMessage("Loading...");
		}

		if (localeTextFields != null)
		{
			for (Text textField : localeTextFields.values())
			{
				if (textField != null && !textField.isDisposed())
				{
					textField.setText("");
					textField.setMessage("Loading...");
				}
			}
		}

		// Update the newKeyButton text based on whether this is a new or existing key
		if (newKeyButton != null && !newKeyButton.isDisposed())
		{
			newKeyButton.setText(isNew ? "Create" : "Save");
			newKeyButton.setEnabled(false); // Disable while loading
		}

		// Disable addLocaleButton while loading
		if (addLocaleButton != null && !addLocaleButton.isDisposed())
		{
			addLocaleButton.setEnabled(false);
		}

		// Use asyncExec to allow UI to update and show "Loading..." before populating fields
		Display.getDefault().asyncExec(new Runnable()
		{
			@Override
			public void run()
			{
				// Set the key value in newKeyTextField if it exists
				if (newKeyTextField != null && !newKeyTextField.isDisposed())
				{
					if (key != null && !key.trim().isEmpty())
					{
						newKeyTextField.setText(key);
					}
					else
					{
						newKeyTextField.setText("");
					}

					// Apply italic font when isNew is true
					if (isNew)
					{
						newKeyTextField.setFont(getItalicFont());
					}
					else
					{
						newKeyTextField.setFont(null); // Reset to default font
					}
				}

				// Set the default value in newDefaultTextField if it exists
				if (newDefaultTextField != null && !newDefaultTextField.isDisposed())
				{
					if (key != null && !key.trim().isEmpty())
					{
						if (isNew)
						{
							newDefaultTextField.setText(generateAITranslateSuggestion(newValue, new Locale("en")));
						}
						else
						{
							EclipseMessages messagesManager = ServoyModelManager.getServoyModelManager().getServoyModel().getMessagesManager();
							Locale emptyLocale = new Locale("");
							String defaultValue = messagesManager.getI18nMessage(i18nDatasource, "i18n:" + key, emptyLocale);
							newDefaultTextField.setText(defaultValue != null ? defaultValue : "");
						}
					}
					else
					{
						newDefaultTextField.setText("");
					}

					// Apply italic font when isNew is true
					if (isNew)
					{
						newDefaultTextField.setFont(getItalicFont());
					}
					else
					{
						newDefaultTextField.setFont(null); // Reset to default font
					}
				}

				// Populate locale text fields based on whether this is a new or existing entry
				if (key != null && !key.trim().isEmpty() && localeTextFields != null)
				{
					EclipseMessages messagesManager = ServoyModelManager.getServoyModelManager().getServoyModel().getMessagesManager();

					for (Map.Entry<String, Text> entry : localeTextFields.entrySet())
					{
						String localeString = entry.getKey();
						Text textField = entry.getValue();

						if (textField != null && !textField.isDisposed())
						{
							try
							{
								// Convert the locale string back to a Locale object
								Locale locale;
								if (localeString.contains("_"))
								{
									String[] parts = localeString.split("_", 2);
									locale = new Locale(parts[0], parts[1]);
								}
								else
								{
									locale = new Locale(localeString);
								}

								if (isNew)
								{
									// Generate AI translation suggestion for new entries
									String translation = generateAITranslateSuggestion(newValue, locale);
									textField.setText(translation != null ? translation : "");
								}
								else
								{
									// Get existing i18n message for this locale
									String existingMessage = messagesManager.getI18nMessage(i18nDatasource, "i18n:" + key, locale, true);
									if (existingMessage == null)
									{
										String translation = generateAITranslateSuggestion(newDefaultTextField.getText(), locale);
										textField.setText(translation != null ? translation : "");
										// Apply italic font for missing translations
										textField.setFont(getItalicFont());
									}
									else
									{
										textField.setText(existingMessage);
										textField.setFont(null); // Reset to default font
									}
								}

								// Apply italic font when isNew is true
								if (isNew)
								{
									textField.setFont(getItalicFont());
								}
							}
							catch (Exception e)
							{
								// If locale parsing fails, leave the field empty
								textField.setText("");
								System.err.println("Failed to parse locale string or retrieve message: " + localeString);
							}
						}
					}
				}

				// Re-enable buttons after content is loaded
				if (newKeyButton != null && !newKeyButton.isDisposed())
				{
					newKeyButton.setEnabled(true);
				}

				if (addLocaleButton != null && !addLocaleButton.isDisposed())
				{
					addLocaleButton.setEnabled(true);
				}
			}
		});
	}

	private ISelectionChangedListener getLanguageSelectionHandler()
	{
		if (languageSelectionHandler == null)
		{
			languageSelectionHandler = new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleLanguageChanged();
				}
			};
		}
		return languageSelectionHandler;
	}

	private ISelectionChangedListener getCountrySelectionHandler()
	{
		if (countrySelectionHandler == null)
		{
			countrySelectionHandler = new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					handleCountryChanged();
				}
			};
		}
		return countrySelectionHandler;
	}

	// Update getLayout to accept new controls
	private GroupLayout getLayout(Composite centerPanel, Combo languageCombo, Combo countryCombo, Label infoLabel,
		Composite textFieldsComposite, org.eclipse.swt.custom.ScrolledComposite scrollableComposite, Button newLocaleBtn)
	{
		GroupLayout i18NLayout = new GroupLayout(this);

		// Build lists of controls for vertical and horizontal groups
		List<org.eclipse.swt.widgets.Control> extraControls = new ArrayList<>();
		if (infoLabel != null && !infoLabel.isDisposed() && infoLabel.getParent() == this)
		{
			extraControls.add(infoLabel);
		}
		if (textFieldsComposite != null && !textFieldsComposite.isDisposed() && textFieldsComposite.getParent() == this)
		{
			extraControls.add(textFieldsComposite);
		}
		if (scrollableComposite != null && !scrollableComposite.isDisposed() && scrollableComposite.getParent() == this)
		{
			extraControls.add(scrollableComposite);
		}

		// Add new locale button if it exists and has this composite as parent
		if (newLocaleBtn != null && !newLocaleBtn.isDisposed() && newLocaleBtn.getParent() == this)
		{
			extraControls.add(newLocaleBtn);
		}

		// Vertical group
		GroupLayout.SequentialGroup vGroup = i18NLayout.createSequentialGroup().addContainerGap();
		GroupLayout.ParallelGroup topGroup = i18NLayout.createParallelGroup(GroupLayout.CENTER, false)
			.add(filterLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.add(filterText, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
			.add(languageCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.add(languageLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
			.add(countryCombo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE);
		vGroup.add(topGroup)
			.addPreferredGap(LayoutStyle.RELATED);

		// If there are extra controls, centerPanel should share space, otherwise take all
		if (extraControls.isEmpty())
		{
			vGroup.add(centerPanel, 0, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
		}
		else
		{
			vGroup.add(centerPanel, 200, 300, Short.MAX_VALUE);
		}

		for (org.eclipse.swt.widgets.Control c : extraControls)
		{
			vGroup.addPreferredGap(LayoutStyle.RELATED).add(c, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		}
		vGroup.addContainerGap();

		// Horizontal group
		GroupLayout.ParallelGroup hGroup = i18NLayout.createParallelGroup(GroupLayout.LEADING);
		GroupLayout.SequentialGroup hSeq = i18NLayout.createSequentialGroup().addContainerGap();
		GroupLayout.ParallelGroup hInner = i18NLayout.createParallelGroup(GroupLayout.LEADING);
		hInner.add(GroupLayout.TRAILING, centerPanel, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
			.add(i18NLayout.createSequentialGroup()
				.add(filterLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.RELATED)
				.add(filterText, GroupLayout.PREFERRED_SIZE, 0, Short.MAX_VALUE)
				.addPreferredGap(LayoutStyle.RELATED)
				.add(languageLabel, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.RELATED)
				.add(languageCombo, GroupLayout.PREFERRED_SIZE, LANG_COMBO_WIDTH, GroupLayout.PREFERRED_SIZE)
				.addPreferredGap(LayoutStyle.RELATED)
				.add(countryCombo, GroupLayout.PREFERRED_SIZE, LANG_COMBO_WIDTH, GroupLayout.PREFERRED_SIZE));
		for (org.eclipse.swt.widgets.Control c : extraControls)
		{
			hInner.add(c, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE);
		}
		hSeq.add(hInner).addContainerGap();
		hGroup.add(hSeq);
		i18NLayout.setHorizontalGroup(hGroup);
		i18NLayout.setVerticalGroup(vGroup);
		return i18NLayout;
	}

	protected void fill(String filter)
	{
		languageComboViewer.removeSelectionChangedListener(getLanguageSelectionHandler());
		languageComboViewer.setInput(getAvailableLanguages().keySet().toArray(new String[0]));
		languageComboViewer.setSelection(new StructuredSelection(selectedLanguage.getDisplayLanguage()));
		languageComboViewer.addSelectionChangedListener(getLanguageSelectionHandler());

		countryComboViewer.removeSelectionChangedListener(getCountrySelectionHandler());
		countryComboViewer.setInput(getAvailableCountries().keySet().toArray(new String[0]));
		countryComboViewer.setSelection(new StructuredSelection(selectedLanguage.getDisplayCountry()));
		countryComboViewer.addSelectionChangedListener(getCountrySelectionHandler());


		tableViewer.setInput(
			messagesModel.getMessages(filter, null, null, null, ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile(), null));
	}

	/**
	 * Optimized fill method that only updates the table viewer, not the language/country combos.
	 * Use this for filter changes to improve performance.
	 */
	protected void fillTableOnly(String filter)
	{
		tableViewer.setInput(
			messagesModel.getMessages(filter, null, null, null, ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile(), null));
	}


	private TreeMap<String, String> availableLanguages = null;

	private TreeMap<String, String> getAvailableLanguages()
	{
		if (availableLanguages == null)
		{
			availableLanguages = new TreeMap<String, String>();
			Locale[] availableLocales = Locale.getAvailableLocales();
			for (Locale locale : availableLocales)
			{
				if (Utils.stringIsEmpty(locale.getDisplayLanguage())) continue;
				availableLanguages.put(locale.getDisplayLanguage(), locale.getLanguage());
			}
		}
		return availableLanguages;
	}

	private TreeMap<String, String> availableCountries = null;

	private TreeMap<String, String> getAvailableCountries()
	{
		if (availableCountries == null)
		{
			availableCountries = new TreeMap<String, String>();
			Locale[] availableLocales = Locale.getAvailableLocales();
			for (Locale locale : availableLocales)
				availableCountries.put(locale.getDisplayCountry(), locale.getCountry());
		}

		return availableCountries;
	}

	protected void handleLanguageChanged()
	{
		selectedLanguage = new Locale(getAvailableLanguages().get(((IStructuredSelection)languageComboViewer.getSelection()).getFirstElement()),
			getAvailableCountries().get(((IStructuredSelection)countryComboViewer.getSelection()).getFirstElement()));
		messagesModel.setLanguage(selectedLanguage);
		lastAutoSelectedKey = getSelectedKey();
		handleFilterChanged();
	}

	protected void handleCountryChanged()
	{
		selectedLanguage = new Locale(getAvailableLanguages().get(((IStructuredSelection)languageComboViewer.getSelection()).getFirstElement()),
			getAvailableCountries().get(((IStructuredSelection)countryComboViewer.getSelection()).getFirstElement()));
		messagesModel.setLanguage(selectedLanguage);
		lastAutoSelectedKey = getSelectedKey();
		handleFilterChanged();
	}

	protected void handleFilterChanged()
	{
		// only apply filter when user stops typing for 300 ms
		delayedFilterJob.setFilterText(filterText.getText());
	}

	public TableViewer getTableViewer()
	{
		return tableViewer;
	}

	/**
	 * Sets the filter text programmatically and triggers filtering
	 * @param filterValue the text to filter by
	 */
	public void setFilterText(String filterValue)
	{
		if (filterText != null && !filterText.isDisposed())
		{
			String processedFilterValue = filterValue;

			// Remove "i18n:" prefix if it exists at the start
			if (processedFilterValue != null && processedFilterValue.startsWith("i18n:"))
			{
				processedFilterValue = processedFilterValue.substring(5); // Remove "i18n:" (5 characters)
			}

			// Clear the tableViewer immediately to avoid showing old data while the delayed filter job runs
			if (tableViewer != null && !tableViewer.getControl().isDisposed())
			{
				tableViewer.setInput(new ArrayList<>());
			}

			filterText.setText(processedFilterValue != null ? processedFilterValue : "");
			// Trigger the delayed filter job
			delayedFilterJob.setFilterText(filterText.getText());
		}
	}

	/**
	 * Gets or creates the unified AI suggestion model for both key generation and translation
	 * @return the chat model, or null if unavailable
	 */
	private ChatModel getAISuggestionModel()
	{
		if (aiSuggestionModel == null)
		{
			EclipseMessages messagesManager = ServoyModelManager.getServoyModelManager().getServoyModel().getMessagesManager();
			TreeMap<String, MessageEntry> translations = messagesManager.getDatasourceMessages(i18nDatasource);

			aiSuggestionModel = com.servoy.eclipse.core.Activator.getDefault().getChatModel(
				"You are an expert in internationalization (i18n) and translation. " +
					"You help with two types of tasks:\n\n" +
					"1. I18N KEY GENERATION: Generate short, descriptive i18n key names based on provided text. " +
					"Keys should follow these conventions:\n" +
					"   - Use lowercase letters and dots for hierarchy\n" +
					"   - Be concise but descriptive\n" +
					"   - Use common prefixes like 'button.', 'label.', 'message.', 'error.', 'title.' etc.\n" +
					"   - Maximum 50 characters\n\n" +
					"2. TRANSLATION: Generate appropriate translations for i18n keys based on the key name and target language. " +
					"Translations should:\n" +
					"   - Be contextually appropriate for the key name\n" +
					"   - Follow natural language conventions for the target locale\n" +
					"   - Be concise but clear\n" +
					"   - Consider common UI/UX terminology\n\n" +
					"Here you have a list of keys and translations that can help in generating new keys and translations. " +
					"They are between curly brackets, comma separated and of the following pattern: languageCode_countryCode.key=translation, " +
					"where languageCode_countryCode can be missing: " + translations.toString() + "\n\n" +
					"IMPORTANT: Always return ONLY the key name or translated text, no explanations or quotes.");
		}
		return aiSuggestionModel;
	}

	/**
	 * Generates AI suggestions for the key field based on the provided text
	 * @param text the text to base suggestions on
	 * @return the suggested key string, or null if suggestion fails
	 */
	private String generateAIKeySuggestion(String text)
	{
		try
		{
			ChatModel chatModel = getAISuggestionModel();

			if (chatModel != null)
			{
				String prompt = "Generate an i18n key for this text: \"" + text + "\"";
				String suggestion = chatModel.chat(prompt);

				// Clean up the suggestion (remove quotes, trim whitespace)
				if (suggestion != null)
				{
					suggestion = suggestion.trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
					return suggestion;
				}
			}
		}
		catch (Exception e)
		{
			// Silently handle errors - AI suggestions are optional
			System.err.println("AI suggestion failed: " + e.getMessage());
		}

		return null;
	}

	/**
	 * Generates AI translation suggestions for a given key in the specified locale
	 * @param key the i18n key to translate
	 * @param locale the target locale for translation
	 * @return the suggested translation string, or null if suggestion fails
	 */
	private String generateAITranslateSuggestion(String key, Locale locale)
	{
		try
		{
			ChatModel chatModel = getAISuggestionModel();

			if (chatModel != null)
			{
				String languageName = locale.getDisplayLanguage();
				String countryName = locale.getDisplayCountry();
				String localeInfo = !countryName.isEmpty() ? languageName + " (" + countryName + ")" : languageName;

				String prompt = "Translate this i18n key to " + localeInfo + ": \"" + key + "\". " +
					"If you cannot suggest a meaningful translation, just return the key as-is: \"" + key + "\"";
				String suggestion = chatModel.chat(prompt);

				// Clean up the suggestion (remove quotes, trim whitespace)
				if (suggestion != null)
				{
					suggestion = suggestion.trim().replaceAll("^\"|\"$", "").replaceAll("^'|'$", "");
					return suggestion;
				}
			}
		}
		catch (Exception e)
		{
			// Silently handle errors - AI suggestions are optional
			System.err.println("AI translation suggestion failed: " + e.getMessage());
		}

		return null;
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

	public void refresh(I18NMessagesModelEntry selectedEntry)
	{
		String selection = getSelectedKey();
		if (selectionChangedListener != null)
		{
			tableViewer.removeSelectionChangedListener(selectionChangedListener);
		}
		tableViewer.setInput(messagesModel.getMessages(filterText.getText(), null, null, null,
			ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile(), selectedEntry));
		if (selectionChangedListener != null)
		{
			tableViewer.addSelectionChangedListener(selectionChangedListener);
		}
		if (selection != null) selectKey(selection);
	}

	/**
	 * Searches for an entry with the given key in the table viewer and selects it if found.
	 * @param key the key to search for (without "i18n:" prefix)
	 * @return true if the entry was found and selected, false otherwise
	 */
	private boolean findEntryInTableViewer(String key)
	{
		if (key == null)
		{
			return false;
		}

		StructuredSelection currentSelection = (StructuredSelection)tableViewer.getSelection();
		if (currentSelection.size() == 1 && ((I18NMessagesModelEntry)currentSelection.getFirstElement()).key.equals(key))
		{
			return true;
		}

		Collection<I18NMessagesModelEntry> contents = (Collection<I18NMessagesModelEntry>)tableViewer.getInput();
		for (I18NMessagesModelEntry entry : contents)
		{
			if (entry.key.equals(key))
			{
				tableViewer.setSelection(new StructuredSelection(entry));
				tableViewer.reveal(entry);
				return true;
			}
		}

		return false;
	}

	public void selectKey(String value)
	{
		lastAutoSelectedKey = value;
		String key = value;
		boolean found = false;
		if (value != null && value.startsWith("i18n:"))
		{
			key = value.substring(5);
			found = findEntryInTableViewer(key);
		}

		if (!found)
		{
			tableViewer.setSelection(new StructuredSelection());
			if (showAISuggestion)
			{
				key = generateAIKeySuggestion(key);
				// Search again for the AI-generated key
				if (key != null)
				{
					found = findEntryInTableViewer(key);
				}
			}
		}

		if (showAISuggestion && key != null)
		{
			updateLocaleValues(key, value, !found);
		}

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
			return I18nComposite.compareHelper(o1, o2, "key");
		}
	}

	public static class I18NEditorDefaultColumnComparator implements Comparator
	{
		public static final I18NEditorDefaultColumnComparator INSTANCE = new I18NEditorDefaultColumnComparator();

		public int compare(Object o1, Object o2)
		{
			return I18nComposite.compareHelper(o1, o2, "defaultvalue");
		}
	}

	public static class I18NEditorLocaleColumnComparator implements Comparator
	{
		public static final I18NEditorLocaleColumnComparator INSTANCE = new I18NEditorLocaleColumnComparator();

		public int compare(Object o1, Object o2)
		{
			return I18nComposite.compareHelper(o1, o2, "localeValue");
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

	/**
	 * Creates dynamic locale fields based on getCurrentLocales() method
	 */
	private void createDynamicLocaleFields()
	{
		Locale[] locales = getCurrentLocales();
		if (locales == null || locales.length == 0) return;

		// Clear existing fields
		localeLabels.clear();
		localeTextFields.clear();

		// Create a GridLayout for the locale fields composite - 2 columns (label, text field)
		org.eclipse.swt.layout.GridLayout gridLayout = new org.eclipse.swt.layout.GridLayout();
		gridLayout.numColumns = 2; // Two columns: label and text field
		gridLayout.makeColumnsEqualWidth = false; // Label can be smaller than text field
		gridLayout.marginWidth = 5;
		gridLayout.marginHeight = 5;
		gridLayout.horizontalSpacing = 5;
		gridLayout.verticalSpacing = 5;
		localeFieldsComposite.setLayout(gridLayout);

		// Create each locale as a row with label and text field
		for (Locale locale : locales)
		{
			String localeString = locale.toString();

			// Create label for this locale
			Label label = new Label(localeFieldsComposite, SWT.NONE);
			label.setText(locale.getDisplayLanguage());
			org.eclipse.swt.layout.GridData labelData = new org.eclipse.swt.layout.GridData(SWT.BEGINNING, SWT.CENTER, false, false);
			labelData.widthHint = 80; // Fixed width for labels
			label.setLayoutData(labelData);
			localeLabels.put(localeString, label);

			// Create text field for this locale (same row)
			Text textField = new Text(localeFieldsComposite, SWT.BORDER);
			textField.setData("fieldName", "fld_" + localeString);
			org.eclipse.swt.layout.GridData textData = new org.eclipse.swt.layout.GridData(SWT.FILL, SWT.CENTER, true, false);
			textData.widthHint = 200; // Minimum width for text fields
			textField.setLayoutData(textData);
			localeTextFields.put(localeString, textField);
		}

		// Set the minimum size for the scrolled composite - crucial for scrolling to work
		org.eclipse.swt.graphics.Point size = localeFieldsComposite.computeSize(SWT.DEFAULT, SWT.DEFAULT);
		localeFieldsComposite.setSize(size);
		localeScrolledComposite.setMinSize(size);

		// For many locales, the scrolled composite will automatically show scrollbars when needed
		if (locales.length > 8) // If more than 8 rows, add some padding for better visibility
		{
			localeScrolledComposite.setMinSize(size.x, Math.min(size.y, 200)); // Limit height to 200 pixels to force scrolling
		}
	}

	/**
	 * Returns the current locales to create fields for.
	 * @return array of Locale objects
	 */
	private Locale[] getCurrentLocales()
	{
		if (currentLocalesList == null)
		{
			// Initialize with default locales
			currentLocalesList = new ArrayList<>();
			if (i18nDatasource != null)
			{
				String serverName = DataSourceUtils.getDataSourceServerName(i18nDatasource);
				String tableName = DataSourceUtils.getDataSourceTableName(i18nDatasource);

				String[] messagesFileNames = EclipseMessages.getMessageFileNames(serverName, tableName);
				if (messagesFileNames != null && messagesFileNames.length > 0)
				{
					// Extract language from each filename and create Locale objects directly
					// Format: serverName.tableName.language.properties
					String prefix = serverName + "." + tableName + ".";
					String suffix = ".properties";
					for (String fileName : messagesFileNames)
					{
						if (fileName.startsWith(prefix) && fileName.endsWith(suffix))
						{
							// Extract the language part between prefix and suffix
							int startIndex = prefix.length();
							int endIndex = fileName.length() - suffix.length();

							// Check bounds to avoid StringIndexOutOfBoundsException
							if (startIndex <= endIndex && endIndex <= fileName.length())
							{
								String language = fileName.substring(startIndex, endIndex);
								if (!language.isEmpty())
								{
									try
									{
										Locale locale;
										// Handle locale strings that may contain language_country format
										if (language.contains("_"))
										{
											String[] parts = language.split("_", 2);
											locale = new Locale(parts[0], parts[1]);
										}
										else
										{
											locale = new Locale(language);
										}

										// Check if locale already exists
										boolean exists = false;
										for (Locale existingLocale : currentLocalesList)
										{
											if (existingLocale.equals(locale))
											{
												exists = true;
												break;
											}
										}

										if (!exists)
										{
											currentLocalesList.add(locale);
										}
									}
									catch (Exception e)
									{
										// If locale creation fails, skip this locale
										System.err.println("Failed to create locale from string: " + language);
									}
								}
							}
						}
					}
				}
			}
		}
		return currentLocalesList.toArray(new Locale[0]);
	}

	/**
	 * Adds a new locale to the current list and refreshes the dynamic locale fields
	 */
	private void addNewLocale()
	{
		// Open the LocaleSelectionDialog
		LocaleSelectionDialog dialog = new LocaleSelectionDialog(this.getShell());
		if (dialog.open() == org.eclipse.jface.window.Window.OK)
		{
			Locale selectedLocale = dialog.getSelectedLocale();
			if (selectedLocale == null)
			{
				return;
			}

			// Initialize the list if needed
			if (currentLocalesList == null)
			{
				currentLocalesList = new ArrayList<>();
				currentLocalesList.add(new Locale("english"));
				currentLocalesList.add(new Locale("french"));
			}

			// Check if locale already exists
			boolean exists = false;
			for (Locale existingLocale : currentLocalesList)
			{
				if (existingLocale.equals(selectedLocale))
				{
					exists = true;
					break;
				}
			}

			if (exists)
			{
				// Optionally show a message that locale already exists
				return;
			}

			// Add the new locale
			currentLocalesList.add(selectedLocale);

			// Dispose existing locale fields
			if (localeFieldsComposite != null && !localeFieldsComposite.isDisposed())
			{
				// Dispose all children first
				org.eclipse.swt.widgets.Control[] children = localeFieldsComposite.getChildren();
				for (org.eclipse.swt.widgets.Control child : children)
				{
					child.dispose();
				}
			}

			// Recreate the dynamic fields
			createDynamicLocaleFields();

			// Update the layout
			if (localeScrolledComposite != null && !localeScrolledComposite.isDisposed())
			{
				localeScrolledComposite.layout(true, true);
			}

			// Update the parent layout
			this.layout(true, true);

			this.selectKey("i18n:" + this.filterText.getText());
		}
	}

	/**
	 * Handles the click event for the new key button to create or update i18n keys
	 */
	private void handleNewKeyButtonClick()
	{
		if (newKeyTextField == null || newKeyTextField.isDisposed() ||
			newDefaultTextField == null || newDefaultTextField.isDisposed())
		{
			return;
		}

		String key = newKeyTextField.getText().trim();
		String defaultValue = newDefaultTextField.getText().trim();

		if (key.isEmpty())
		{
			return; // Key is required
		}

		EclipseMessages messagesManager = ServoyModelManager.getServoyModelManager().getServoyModel().getMessagesManager();

		try
		{
			// Create or update the default value (empty locale)
			if (!defaultValue.isEmpty())
			{
				messagesManager.addMessage(i18nDatasource, new MessageEntry(null, key, defaultValue));
			}

			// Create or update values for all locale text fields
			if (localeTextFields != null)
			{
				for (Map.Entry<String, Text> entry : localeTextFields.entrySet())
				{
					String localeString = entry.getKey();
					Text textField = entry.getValue();

					if (textField != null && !textField.isDisposed())
					{
						String localeValue = textField.getText().trim();
						if (!localeValue.isEmpty())
						{
							// Set the message for this locale
							messagesManager.addMessage(i18nDatasource, new MessageEntry(localeString, key, localeValue));
						}
					}
				}
			}

			messagesManager.save(this.i18nDatasource);

			// Refresh the table to show the new/updated entry
			refresh(null);

			// Clear the text fields after successful creation/update
			newKeyTextField.setText("");
			newDefaultTextField.setText("");
			if (localeTextFields != null)
			{
				for (Text textField : localeTextFields.values())
				{
					if (textField != null && !textField.isDisposed())
					{
						textField.setText("");
					}
				}
			}

			// Select the newly created/updated key in the table
			fillTableOnly(key);
			selectKey("i18n:" + key);
		}
		catch (Exception e)
		{
			System.err.println("Failed to create/update i18n key: " + key + " - " + e.getMessage());
		}
	}
}
