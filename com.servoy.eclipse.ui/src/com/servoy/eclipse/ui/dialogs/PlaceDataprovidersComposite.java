/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.layout.TableColumnLayout;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.ComboBoxViewerCellEditor;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.ViewerCell;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CCombo;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * @author jcompagner
 * @since 8.2
 *
 */
public class PlaceDataprovidersComposite extends Composite
{
	private static final String MEDIA_PROPERTY = "MEDIA";
	private static final String DATETIME_PROPERTY = "DATETIME";
	private static final String NUMBER_PROPERTY = "NUMBER";
	private static final String INTEGER_PROPERTY = "INTEGER";
	private static final String TEXT_PROPERTY = "TEXT";
	private static final String FIELD_SPACING_PROPERTY = "FIELD_SPACING";
	private static final String PLACE_WITH_LABELS_PROPERTY = "PLACE_WITH_LABELS";
	private static final String LABEL_COMPONENT_PROPERTY = "LABEL_COMPONENT";
	private static final String LABEL_SPACING_PROPERTY = "LABEL_SPACING";
	private static final String PLACE_ON_TOP_PROPERTY = "PLACE_ON_TOP";
	private static final String FILL_NAME_PROPERTY = "FILL_NAME";
	private static final String FILL_TEXT_PROPERTY = "FILL_TEXT";
	private static final String PLACE_HORIZONTALLY_PROPERTY = "PLACE_HORIZONTALLY";
	private static final String FIELD_WIDTH_PROPERTY = "FIELD_WIDTH";
	private static final String FIELD_HEIGTH_PROPERTY = "FIELD_HEIGTH";
	private static final String LABEL_WIDTH_PROPERTY = "LABEL_WIDTH";
	private static final String LABEL_HEIGTH_PROPERTY = "LABEL_HEIGTH";

	private static final String CONFIGURATION_SELECTION_SETTING = "CONFIGURATION_SELECTION";
	private static final String SINGLE_CLICK_SETTING = "SINGLE_CLICK";

	private final TableViewer tableViewer;
	private final TableColumn dataproviderColumn;
	private final TableColumn templateColumn;
	private final JSONObject preferences;
	private final ComboViewer stringCombo;
	private final ComboViewer intCombo;
	private final ComboViewer numberCombo;
	private final ComboViewer mediaCombo;
	private final ComboViewer dateCombo;
	private final List<Pair<IDataProvider, Object>> input = new ArrayList<>();
	private final ComboViewer configurationViewer;
	private final ComboViewer labelComponentCombo;
	private final Text labelSpacing;
	private final Text fieldSpacing;
	private final Button placeWithLabelsButton;
	private final Button onTopButton;
	private final Button fillName;
	private final Button fillText;
	private final DataProviderTreeViewer dataproviderTreeViewer;
	private final IDialogSettings settings;
	private final Button placeHorizontally;
	private final Button singleClick;
	private final List<IReadyListener> readyListeners = new ArrayList<>();

	private JSONObject currentSelection;
	private String configurationSelection = null;
	private Text fieldWidth;
	private Text fieldHeight;
	private Text labelWidth;
	private Text labelHeight;


	public PlaceDataprovidersComposite(final Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final IDialogSettings settings)
	{
		super(parent, SWT.None);
		this.settings = settings;

		final ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();

		JSONObject prefs;

		try
		{
			prefs = resourcesProject.getPlaceDataproviderPreferences();
		}
		catch (JSONException e)
		{
			prefs = new ServoyJSONObject();
			((ServoyJSONObject)prefs).setNoQuotes(false); // important, as configuration names are stored as keys - and without the quotes we would store invalid json (or they would need to be restrictions on spaces and so on)
			((ServoyJSONObject)prefs).setNewLines(true);
			((ServoyJSONObject)prefs).setNoBrackets(false);

			UIUtils.runInUI(new Runnable()
			{

				@Override
				public void run()
				{
					MessageDialog errorDialog = new MessageDialog(parent.getShell(), "Error reading the place field configuration", null,
						"The file 'placedataprovider.preferences' from the active resources project contains invalid JSON. It will be ignored and the place field preferences will revert to default.",
						MessageDialog.ERROR, new String[] { "Backup and &Discard corrupt preferences", "Ok" }, 0);
					int opt = errorDialog.open();
					if (opt == 0)
					{
						resourcesProject.backupCorruptedPlaceDataproivderPreferences();
					} // else just do nothing - the file content will be overwritten anyway
				}
			}, false);
		}
		preferences = prefs;

		JSONObject mainPref = preferences.optJSONObject("_");
		if (mainPref == null)
		{
			mainPref = new JSONObject();
			mainPref.put(TEXT_PROPERTY, "servoydefault-textfield");
			mainPref.put(INTEGER_PROPERTY, "servoydefault-textfield");
			mainPref.put(NUMBER_PROPERTY, "servoydefault-textfield");
			mainPref.put(DATETIME_PROPERTY, "servoydefault-calendar");
			mainPref.put(MEDIA_PROPERTY, "servoydefault-imagemedia");
			preferences.put("_", mainPref);
		}
		currentSelection = mainPref;
		configurationSelection = "_";

		final Object[] inputs = getComponentsAndTemplatesInput();

		GridLayout layout = new GridLayout(6, false);
		this.setLayout(layout);
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 2;
		gridData.verticalSpan = 20;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;

		Label confLabel = new Label(this, SWT.NONE);
		confLabel.setText("Configuration: ");
		Composite confAndDelete = new Composite(this, SWT.NONE);
		confAndDelete.setLayout(new GridLayout(2, false));
		confAndDelete.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		final CCombo configuration = new CCombo(confAndDelete, SWT.BORDER);
		configuration.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		configurationViewer = new ComboViewer(configuration);
		configurationViewer.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if ("_".equals(element)) return "";
				return super.getText(element);
			}
		});
		configurationViewer.setContentProvider(ArrayContentProvider.getInstance());
		Set<String> keySet = preferences.keySet();
		configurationViewer.setInput(new ArrayList<>(keySet));
		configurationViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				configurationSelection = (String)((StructuredSelection)event.getSelection()).getFirstElement();
				currentSelection = preferences.optJSONObject(configurationSelection);
				if (currentSelection == null)
				{
					currentSelection = new JSONObject();
					preferences.put(configurationSelection, currentSelection);
				}
				setSelection(stringCombo, TEXT_PROPERTY);
				setSelection(intCombo, INTEGER_PROPERTY);
				setSelection(numberCombo, NUMBER_PROPERTY);
				setSelection(dateCombo, DATETIME_PROPERTY);
				setSelection(mediaCombo, MEDIA_PROPERTY);
				setSelection(labelComponentCombo, LABEL_COMPONENT_PROPERTY);
				labelSpacing.setText(currentSelection.optString(LABEL_SPACING_PROPERTY));
				fieldSpacing.setText(currentSelection.optString(FIELD_SPACING_PROPERTY));
				placeWithLabelsButton.setSelection(currentSelection.optBoolean(PLACE_WITH_LABELS_PROPERTY));
				onTopButton.setSelection(currentSelection.optBoolean(PLACE_ON_TOP_PROPERTY));
				fillName.setSelection(currentSelection.optBoolean(FILL_NAME_PROPERTY));
				fillText.setSelection(currentSelection.optBoolean(FILL_TEXT_PROPERTY));
				placeHorizontally.setSelection(currentSelection.optBoolean(PLACE_HORIZONTALLY_PROPERTY));
				fieldWidth.setText(currentSelection.optString(FIELD_WIDTH_PROPERTY));
				fieldHeight.setText(currentSelection.optString(FIELD_HEIGTH_PROPERTY));
				labelWidth.setText(currentSelection.optString(LABEL_WIDTH_PROPERTY));
				labelHeight.setText(currentSelection.optString(LABEL_HEIGTH_PROPERTY));

				updatePlaceWithLabelState();
			}
		});
		configurationViewer.getCCombo().addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				configurationSelection = configurationViewer.getCCombo().getText();
				currentSelection = new JSONObject(currentSelection, JSONObject.getNames(currentSelection));
			}
		});
		Button deleteConf = new Button(confAndDelete, SWT.PUSH);
		deleteConf.setText("X");
		deleteConf.setToolTipText("Removes the current selected configuration");
		deleteConf.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (!"_".equals(configurationSelection) && configurationSelection != null)
				{
					preferences.remove(configurationSelection);
					((List)configurationViewer.getInput()).remove(configurationSelection);
					configurationViewer.setSelection(new StructuredSelection("_"));
					configurationViewer.refresh();
				}
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetSelected(e);
			}
		});

		dataproviderTreeViewer = new DataProviderTreeViewer(this, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, // label provider will be overwritten when superform is known
			new DataProviderContentProvider(persistContext, flattenedSolution, table), dataproviderOptions, true, true,
			TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS), SWT.MULTI);
		dataproviderTreeViewer.setLayoutData(gridData);
		dataproviderTreeViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				if (singleClick.getSelection())
				{
					moveDataproviderSelection(inputs);
				}
			}
		});

		dataproviderTreeViewer.addOpenListener(new IOpenListener()
		{
			@Override
			public void open(OpenEvent event)
			{
				moveDataproviderSelection(inputs);
			}
		});

		final Composite container = new Composite(this, SWT.NONE);
		// define layout for the viewer

		container.setLayoutData(gridData);

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		container.setLayout(tableColumnLayout);
		tableViewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		tableViewer.getTable().setLinesVisible(true);
		tableViewer.getTable().setHeaderVisible(true);
		tableViewer.setContentProvider(ArrayContentProvider.getInstance());
		dataproviderColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
		dataproviderColumn.setText("Name");

		templateColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
		templateColumn.setText("Component/Template");

		TableColumn removeColumn = new TableColumn(tableViewer.getTable(), SWT.LEFT);
		removeColumn.setText("X");

		TableViewerColumn dataproviderViewerColumn = new TableViewerColumn(tableViewer, dataproviderColumn);
		dataproviderViewerColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				@SuppressWarnings("unchecked")
				Pair<IDataProvider, Object> array = (Pair<IDataProvider, Object>)element;
				return array.getLeft().toString();
			}
		});


		TableViewerColumn templateViewerColumn = new TableViewerColumn(tableViewer, templateColumn);
		templateViewerColumn.setEditingSupport(new TemplateEditingSupport(tableViewer));
		templateViewerColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				@SuppressWarnings("unchecked")
				Pair<IDataProvider, Object> array = (Pair<IDataProvider, Object>)element;
				if (array.getRight() instanceof WebObjectSpecification)
				{
					return ((WebObjectSpecification)array.getRight()).getDisplayName();
				}
				if (array.getRight() instanceof IRootObject)
				{
					return ((IRootObject)array.getRight()).getName() + " (template)";
				}
				return (String)array.getRight();
			}
		});

		final TableViewerColumn removeViewerColumn = new TableViewerColumn(tableViewer, removeColumn);
		removeViewerColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				return "X";
			}
		});

		tableViewer.getTable().addListener(SWT.MouseDown, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				Point pt = new Point(event.x, event.y);
				ViewerCell cell = tableViewer.getCell(pt);
				if (cell != null && cell.getColumnIndex() == 2)
				{
					int index = tableViewer.getTable().getSelectionIndex();
					input.remove(index);
					if (input.size() == 0)
					{
						for (IReadyListener rl : readyListeners)
						{
							rl.isReady(false);
						}
					}
					tableViewer.refresh();
				}
			}
		});

		tableColumnLayout.setColumnData(dataproviderColumn, new ColumnWeightData(40, 100, true));
		tableColumnLayout.setColumnData(templateColumn, new ColumnWeightData(60, 100, true));
		tableColumnLayout.setColumnData(removeColumn, new ColumnWeightData(16, 16, false));


		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(this, SWT.NONE).setLayoutData(gd);


		stringCombo = generateTypeCombo("String", inputs, TEXT_PROPERTY);
		intCombo = generateTypeCombo("Integer", inputs, INTEGER_PROPERTY);
		numberCombo = generateTypeCombo("Number", inputs, NUMBER_PROPERTY);
		dateCombo = generateTypeCombo("Date", inputs, DATETIME_PROPERTY);
		mediaCombo = generateTypeCombo("Media", inputs, MEDIA_PROPERTY);


		tableViewer.setInput(input);

		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(this, SWT.NONE).setLayoutData(gd);

		fieldSpacing = createLabelAndField("Field spacing", FIELD_SPACING_PROPERTY);
		Label lbl = new Label(this, SWT.NONE);
		lbl.setText("Default size (w,h)");
		lbl.setToolTipText("The default size used from components (templates will use the template size");
		Composite sizeComposite = new Composite(this, SWT.NONE);
		layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		sizeComposite.setLayout(layout);
		sizeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fieldWidth = createField(sizeComposite, FIELD_WIDTH_PROPERTY);
		fieldHeight = createField(sizeComposite, FIELD_HEIGTH_PROPERTY);

		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(this, SWT.NONE).setLayoutData(gd);

		placeWithLabelsButton = createLabelAndCheck("Place with labels", PLACE_WITH_LABELS_PROPERTY);
		labelComponentCombo = generateTypeCombo("Label Component", inputs, LABEL_COMPONENT_PROPERTY);
		labelComponentCombo.getCCombo().setEnabled(placeWithLabelsButton.getSelection());
		placeWithLabelsButton.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updatePlaceWithLabelState();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetSelected(e);
			}
		});
		labelSpacing = createLabelAndField("Label spacing", LABEL_SPACING_PROPERTY);
		labelSpacing.setEnabled(placeWithLabelsButton.getSelection());

		lbl = new Label(this, SWT.NONE);
		lbl.setText("Label size (w,h)");
		lbl.setToolTipText("The default size used from labels (templates will use the template size");
		sizeComposite = new Composite(this, SWT.NONE);
		layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		sizeComposite.setLayout(layout);
		sizeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		labelWidth = createField(sizeComposite, LABEL_WIDTH_PROPERTY);
		labelHeight = createField(sizeComposite, LABEL_HEIGTH_PROPERTY);

		onTopButton = createLabelAndCheck("Place label on top", PLACE_ON_TOP_PROPERTY);
		onTopButton.setEnabled(placeWithLabelsButton.getSelection());

		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(this, SWT.NONE).setLayoutData(gd);
		fillName = createLabelAndCheck("Fill name property", FILL_NAME_PROPERTY);
		fillText = createLabelAndCheck("Fill text property", FILL_TEXT_PROPERTY);

		placeHorizontally = createLabelAndCheck("Place Horizontally", PLACE_HORIZONTALLY_PROPERTY);

		new Label(this, SWT.NONE);
		new Label(this, SWT.NONE);
		singleClick = new Button(this, SWT.CHECK);
		boolean singleClickSelection = settings.getBoolean(SINGLE_CLICK_SETTING);
		if (singleClickSelection)
		{
			singleClick.setText("Single click selects/moves the provider");
		}
		else
		{
			singleClick.setText("Single click selects/moves the provider (use double click)");
		}
		singleClick.setSelection(singleClickSelection);
		gd = new GridData();
		gd.horizontalSpan = 4;
		gd.grabExcessHorizontalSpace = true;
		gd.horizontalAlignment = SWT.FILL;
		singleClick.setLayoutData(gd);
		singleClick.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				boolean selection = singleClick.getSelection();
				settings.put(SINGLE_CLICK_SETTING, selection);
				if (selection)
				{
					singleClick.setText("Single click selects/moves the provider");
				}
				else
				{
					singleClick.setText("Single click selects/moves the provider (use double click)");
				}
//				singleClick.getParent().layout();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}
		});


		String selection = settings.get(CONFIGURATION_SELECTION_SETTING);
		if (selection != null && preferences.has(selection))
		{
			configurationViewer.setSelection(new StructuredSelection(selection));
		}
	}

	/**
	 * @return
	 *
	 */
	private Button createLabelAndCheck(String label, final String property)
	{
		GridData gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		Label lbl = new Label(this, SWT.NONE);
		lbl.setText(label);
		lbl.setLayoutData(gd);
		final Button button = new Button(this, SWT.CHECK);
		gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		button.setLayoutData(gd);
		button.setSelection(currentSelection.optBoolean(property));
		button.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				currentSelection.put(property, button.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetSelected(e);
			}
		});
		return button;
	}

	/**
	 * @return
	 *
	 */
	private Text createLabelAndField(String label, final String property)
	{
		new Label(this, SWT.NONE).setText(label);
		return createField(this, property);
	}

	/**
	 * @param property
	 * @return
	 */
	private Text createField(Composite parent, final String property)
	{
		final Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.setText(currentSelection.optString(property));
		text.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				currentSelection.put(property, text.getText());
			}
		});
		return text;
	}

	/**
	 * @return
	 *
	 */
	private ComboViewer generateTypeCombo(String label, Object[] viewerInput, final String type)
	{
		Label stringLabel = new Label(this, SWT.NONE);
		stringLabel.setText(label);
		final CCombo stringCC = new CCombo(this, SWT.BORDER | SWT.SEARCH);
		stringCC.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		ComboViewer comboViewer = new ComboViewer(stringCC);
		comboViewer.setLabelProvider(new TemplateOrWebObjectLabelProvider());
		comboViewer.setContentProvider(ArrayContentProvider.getInstance());
		comboViewer.setInput(viewerInput);
		setSelection(comboViewer, type);
		comboViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				Object sel = ((StructuredSelection)event.getSelection()).getFirstElement();
				currentSelection.put(type, PlaceDataprovidersComposite.this.toString(sel));
				@SuppressWarnings("unchecked")
				List<Pair<IDataProvider, Object>> tableInput = (List<Pair<IDataProvider, Object>>)tableViewer.getInput();
				for (Pair<IDataProvider, Object> row : tableInput)
				{
					if (Column.getDisplayTypeString(row.getLeft().getDataProviderType()).equals(type))
					{
						row.setRight(sel);
					}
				}
				tableViewer.refresh();

			}
		});
		return comboViewer;
	}

	/**
	 * @param comboViewer
	 * @param type
	 * @param input
	 */
	private void setSelection(ComboViewer comboViewer, final String type)
	{
		Object[] viewerInput = (Object[])comboViewer.getInput();
		String selection = currentSelection.optString(type);
		Object selectedCompOrTemplate = findSelectedObject(viewerInput, selection);
		if (selectedCompOrTemplate != null) comboViewer.setSelection(new StructuredSelection(selectedCompOrTemplate));
		else comboViewer.setSelection(StructuredSelection.EMPTY);
	}

	/**
	 * @param data
	 * @param selection
	 * @param selectedCompOrTemplate
	 * @return
	 */
	private Object findSelectedObject(Object[] data, String selection)
	{
		Object selectedCompOrTemplate = null;
		if (selection != null)
		{
			for (Object object : data)
			{
				if (object instanceof Template && (((Template)object).getName().equals(selection)))
				{
					selectedCompOrTemplate = object;
					break;
				}
				else if (object instanceof WebObjectSpecification && ((WebObjectSpecification)object).getName().equals(selection))
				{
					selectedCompOrTemplate = object;
					break;
				}
			}
		}
		return selectedCompOrTemplate;
	}

	private String toString(Object object)
	{
		if (object instanceof Template)
		{
			return ((Template)object).getName();
		}
		else if (object instanceof WebObjectSpecification)
		{
			return ((WebObjectSpecification)object).getName();
		}
		return null;
	}

	/**
	 * @param table
	 * @param create
	 */
	public void setTable(ITable table, PersistContext context)
	{
		((DataProviderContentProvider)dataproviderTreeViewer.getContentProvider()).setTable(table, context);
		dataproviderTreeViewer.refreshTree();
	}

	public PlaceDataProviderConfiguration getDataProviderConfiguration()
	{
		settings.put(CONFIGURATION_SELECTION_SETTING, configurationSelection);
		((TreePatternFilter)dataproviderTreeViewer.getPatternFilter()).saveSettings(settings);
		preferences.put(configurationSelection, currentSelection);
		ServoyModelFinder.getServoyModel().getActiveResourcesProject().savePlaceDataproviderPreferences(preferences);

		Dimension fieldSize = new Dimension(currentSelection.optInt(FIELD_WIDTH_PROPERTY, -1), currentSelection.optInt(FIELD_HEIGTH_PROPERTY, -1));
		Dimension labelSize = new Dimension(currentSelection.optInt(LABEL_WIDTH_PROPERTY, -1), currentSelection.optInt(LABEL_HEIGTH_PROPERTY, -1));

		return new PlaceDataProviderConfiguration(input, currentSelection.optInt(FIELD_SPACING_PROPERTY, -1),
			currentSelection.optBoolean(PLACE_WITH_LABELS_PROPERTY), currentSelection.optString(LABEL_COMPONENT_PROPERTY),
			currentSelection.optInt(LABEL_SPACING_PROPERTY, -1), currentSelection.optBoolean(PLACE_ON_TOP_PROPERTY),
			currentSelection.optBoolean(FILL_NAME_PROPERTY), currentSelection.optBoolean(FILL_TEXT_PROPERTY),
			currentSelection.optBoolean(PLACE_HORIZONTALLY_PROPERTY), fieldSize, labelSize);
	}

	/**
	 * @return
	 */
	private Object[] getComponentsAndTemplatesInput()
	{
		ArrayList<Object> specs = new ArrayList<>();
		List<IRootObject> templates = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES);
		for (IRootObject template : templates)
		{
			if (template instanceof StringResource)
			{
				StringResource stringResource = (StringResource)template;
				String content = stringResource.getContent();
				try
				{
					JSONObject templateJSON = new ServoyJSONObject(content, false);
					JSONArray elements = templateJSON.optJSONArray("elements");
					if (elements != null && elements.length() == 1 && hasDataproviderProperty(elements.getJSONObject(0)))
					{
						specs.add(template);
					}
				}
				catch (Exception e)
				{
					Debug.error("error parsing " + content, e);
				}
			}
		}
		for (PackageSpecification<WebObjectSpecification> pck : WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecifications().values())
		{
			if (pck.getPackageName().equals("servoycore")) continue;
			for (WebObjectSpecification wos : pck.getSpecifications().values())
			{
				if (wos.getProperties(DataproviderPropertyType.INSTANCE).size() > 0)
				{
					specs.add(wos);
				}
			}
		}
		Collections.sort(specs, new Comparator<Object>()
		{
			@Override
			public int compare(Object o1, Object o2)
			{
				if (o1 instanceof Template && o2 instanceof Template)
				{
					return ((Template)o1).getName().compareTo(((Template)o2).getName());
				}
				else if (o1 instanceof Template) return -1;
				else if (o2 instanceof Template) return 1;

				if (((WebObjectSpecification)o1).getPackageName().equals(((WebObjectSpecification)o2).getPackageName()))
				{
					return ((WebObjectSpecification)o1).getDisplayName().compareTo(((WebObjectSpecification)o2).getDisplayName());
				}

				return ((WebObjectSpecification)o1).getPackageName().compareTo(((WebObjectSpecification)o2).getPackageName());
			}
		});
		return specs.toArray();
	}

	/**
	 *
	 */
	private void updatePlaceWithLabelState()
	{
		boolean sel = placeWithLabelsButton.getSelection();
		labelComponentCombo.getCCombo().setEnabled(sel);
		labelSpacing.setEnabled(sel);
		onTopButton.setEnabled(sel);
		labelHeight.setEnabled(sel);
		labelWidth.setEnabled(sel);
	}

	/**
	 * @param inputs
	 */
	private void moveDataproviderSelection(final Object[] inputs)
	{
		IDataProvider dataprovider = (IDataProvider)((StructuredSelection)dataproviderTreeViewer.getSelection()).getFirstElement();

		Pair<IDataProvider, Object> row = new Pair<>(dataprovider,
			findSelectedObject(inputs, currentSelection.optString(Column.getDisplayTypeString(dataprovider.getDataProviderType()))));
		input.add(row);
		for (IReadyListener rl : readyListeners)
		{
			rl.isReady(true);
		}
		tableViewer.refresh();
	}

	private static boolean hasDataproviderProperty(JSONObject object)
	{
		int typeId = object.optInt(SolutionSerializer.PROP_TYPEID);

		switch (typeId)
		{
			case IRepositoryConstants.FIELDS :
			case IRepositoryConstants.GRAPHICALCOMPONENTS :
				return true;
			case IRepositoryConstants.WEBCOMPONENTS :
			{
				String typeName = object.optString(IContentSpecConstants.PROPERTY_TYPENAME);
				if (typeName != null)
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(typeName);
					return spec != null && !spec.getProperties(DataproviderPropertyType.INSTANCE).isEmpty();
				}
			}

		}
		return false;

	}


	/**
	 * @param iReadyListener
	 */
	public void addReadyListener(IReadyListener readyListener)
	{
		readyListeners.add(readyListener);
	}

	class TemplateEditingSupport extends EditingSupport
	{

		private final TableViewer viewer;
		private final ComboBoxViewerCellEditor editor;

		public TemplateEditingSupport(TableViewer viewer)
		{
			super(viewer);
			this.viewer = viewer;
			this.editor = new ComboBoxViewerCellEditor(viewer.getTable());
			editor.setLabelProvider(new TemplateOrWebObjectLabelProvider());
			editor.setContentProvider(ArrayContentProvider.getInstance());
			editor.setInput(getComponentsAndTemplatesInput());
		}

		@Override
		protected CellEditor getCellEditor(Object element)
		{
			return editor;
		}

		@Override
		protected boolean canEdit(Object element)
		{
			return true;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected Object getValue(Object element)
		{
			return ((Pair<IDataProvider, Object>)element).getRight();
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void setValue(Object element, Object userInputValue)
		{
			((Pair<IDataProvider, Object>)element).setRight(userInputValue);
			viewer.update(element, null);
		}
	}

	private final class TemplateOrWebObjectLabelProvider extends ColumnLabelProvider
	{
		@Override
		public String getText(Object element)
		{
			if (element instanceof Template)
			{
				return ((Template)element).getName() + " (template)";
			}
			else if (element instanceof WebObjectSpecification)
			{
				return ((WebObjectSpecification)element).getDisplayName() + " (" + ((WebObjectSpecification)element).getPackageName() + ")";
			}
			return super.getText(element);
		}
	}

	public static interface IReadyListener
	{
		void isReady(boolean ready);
	}
}
