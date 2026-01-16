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
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
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
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
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
	private static final String AUTOMATIC_I18N_PROPERTY = "AUTOMATIC_I18N";
	private static final String I18N_PREFIX_PROPERTY = "I18N_PREFIX";

	private static final String CONFIGURATION_SELECTION_SETTING = "CONFIGURATION_SELECTION";
	private static final String SINGLE_CLICK_SETTING = "SINGLE_CLICK";
	private static final String ADVANCED_MODE_SETTING = "ADVANCED_MODE_SETTING";

	private final TableViewer tableViewer;
	private final JSONObject preferences;
	private final ComboViewer stringCombo;
	private final ComboViewer intCombo;
	private final ComboViewer numberCombo;
	private final ComboViewer mediaCombo;
	private final ComboViewer dateCombo;
	private final List<Pair<IDataProvider, Object>> input = new ArrayList<>();
	private ComboViewer configurationViewer;
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
	private Button singleClick;
	private final List<IReadyListener> readyListeners = new ArrayList<>();
	private final Button automaticI18N;
	private final Text i18nPrefix;

	private JSONObject currentSelection;
	private String configurationSelection = null;
	private final Text fieldWidth;
	private final Text fieldHeight;
	private final Text labelWidth;
	private final Text labelHeight;
	private final Composite configurationComposite;
	private final GridData configurationGridData;
	private Button simpleAdvanced;


	public PlaceDataprovidersComposite(final Composite parent, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final IDialogSettings settings)
	{
		super(parent, SWT.None);
		this.settings = settings;

		final ServoyResourcesProject resourcesProject = ServoyModelFinder.getServoyModel().getActiveResourcesProject();

		JSONObject prefs;

		if (resourcesProject != null)
		{
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
		}
		else
		{
			prefs = new ServoyJSONObject();
			((ServoyJSONObject)prefs).setNoQuotes(false);
			((ServoyJSONObject)prefs).setNewLines(true);
			((ServoyJSONObject)prefs).setNoBrackets(false);
		}
		preferences = prefs;

		JSONObject mainPref = preferences.optJSONObject("_");
		if (mainPref == null)
		{
			mainPref = new JSONObject();
			mainPref.put(TEXT_PROPERTY, "bootstrapcomponents-textbox");
			mainPref.put(INTEGER_PROPERTY, "bootstrapcomponents-textbox");
			mainPref.put(NUMBER_PROPERTY, "bootstrapcomponents-textbox");
			mainPref.put(DATETIME_PROPERTY, "bootstrapcomponents-calendar");
			mainPref.put(MEDIA_PROPERTY, "bootstrapcomponents-imagemedia");
			preferences.put("_", mainPref);
		}
		else
		{
			Form form = persistContext != null && persistContext.getContext() != null ? (Form)persistContext.getContext().getAncestor(IRepository.FORMS) : null;
			boolean skipDefault = EditorUtil.hideDefaultComponents(form);
			if (skipDefault)
			{
				if ("servoydefault-textfield".equals(mainPref.optString(TEXT_PROPERTY)))
				{
					mainPref.put(TEXT_PROPERTY, "bootstrapcomponents-textbox");
				}
				if ("servoydefault-textfield".equals(mainPref.optString(INTEGER_PROPERTY)))
				{
					mainPref.put(INTEGER_PROPERTY, "bootstrapcomponents-textbox");
				}
				if ("servoydefault-textfield".equals(mainPref.optString(NUMBER_PROPERTY)))
				{
					mainPref.put(NUMBER_PROPERTY, "servoydefault-textfield");
				}
				if ("servoydefault-calendar".equals(mainPref.optString(DATETIME_PROPERTY)))
				{
					mainPref.put(DATETIME_PROPERTY, "bootstrapcomponents-textbox");
				}
				if ("servoydefault-imagemedia".equals(mainPref.optString(MEDIA_PROPERTY)))
				{
					mainPref.put(MEDIA_PROPERTY, "bootstrapcomponents-textbox");
				}
			}
		}
		currentSelection = mainPref;
		configurationSelection = "_";

		final Object[] inputs = getComponentsAndTemplatesInput(true, persistContext);

		this.setLayout(new FillLayout());
		SashForm form = new SashForm(this, SWT.HORIZONTAL);
		form.setLayout(new FillLayout());

		configurationComposite = new Composite(form, SWT.NONE);
		GridLayout configurationLayout = new GridLayout(2, false);
		configurationLayout.marginHeight = 0;
		configurationLayout.marginWidth = 0;
		configurationLayout.marginRight = 5;
		configurationComposite.setLayout(configurationLayout);

		configurationGridData = new GridData();
		configurationGridData.verticalAlignment = GridData.FILL;
		configurationGridData.horizontalSpan = 1;
		configurationGridData.verticalSpan = 1;
		configurationGridData.grabExcessHorizontalSpace = true;
		configurationGridData.grabExcessVerticalSpace = true;
		configurationGridData.horizontalAlignment = GridData.FILL;
		configurationGridData.minimumWidth = 370;
		configurationComposite.setLayoutData(configurationGridData);


		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(configurationComposite, SWT.NONE).setLayoutData(gd);


		stringCombo = generateTypeCombo(configurationComposite, "String", inputs, TEXT_PROPERTY,
			"The component/template to use for the STRING type dataprovider");
		intCombo = generateTypeCombo(configurationComposite, "Integer", inputs, INTEGER_PROPERTY,
			"The component/template to use for the INTEGER type dataprovider");
		numberCombo = generateTypeCombo(configurationComposite, "Number", inputs, NUMBER_PROPERTY,
			"The component/template to use for the NUMBER type dataprovider");
		dateCombo = generateTypeCombo(configurationComposite, "Date", inputs, DATETIME_PROPERTY,
			"The component/template to use for the DATE type dataprovider");
		mediaCombo = generateTypeCombo(configurationComposite, "Media", inputs, MEDIA_PROPERTY,
			"The component/template to use for the MEDIA type dataprovider");


		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(configurationComposite, SWT.NONE).setLayoutData(gd);

		fieldSpacing = createLabelAndField(configurationComposite, "Field spacing", FIELD_SPACING_PROPERTY,
			"How much space must there be between the placed fields in pixels");
		Label lbl = new Label(configurationComposite, SWT.NONE);
		lbl.setText("Default size (w,h)");
		lbl.setToolTipText("The default size used from components (templates will use the template size)");
		Composite sizeComposite = new Composite(configurationComposite, SWT.NONE);
		GridLayout layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		sizeComposite.setLayout(layout);
		sizeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fieldWidth = createField(sizeComposite, FIELD_WIDTH_PROPERTY, "Default width of the placed field (ignored for templates)");
		fieldHeight = createField(sizeComposite, FIELD_HEIGTH_PROPERTY, "Default height of the placed field (ignored for templates)");

		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(configurationComposite, SWT.NONE).setLayoutData(gd);

		placeWithLabelsButton = createLabelAndCheck(configurationComposite, "Place with labels", PLACE_WITH_LABELS_PROPERTY,
			"Place a label component together with the field");
		labelComponentCombo = generateTypeCombo(configurationComposite, "Label Component", getComponentsAndTemplatesInput(false, persistContext),
			LABEL_COMPONENT_PROPERTY,
			"The component/template to use for the label component");
		labelComponentCombo.getCCombo().setEnabled(placeWithLabelsButton.getSelection());
		placeWithLabelsButton.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updatePlaceWithLabelState();
				updateI18NEnableState();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetSelected(e);
			}
		});
		labelSpacing = createLabelAndField(configurationComposite, "Label spacing", LABEL_SPACING_PROPERTY,
			"How much space must there be between the label and the field in pixels");
		labelSpacing.setEnabled(placeWithLabelsButton.getSelection());

		lbl = new Label(configurationComposite, SWT.NONE);
		lbl.setText("Label size (w,h)");
		lbl.setToolTipText("The default size used from labels (templates will use the template size)");
		sizeComposite = new Composite(configurationComposite, SWT.NONE);
		layout = new GridLayout(2, true);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		sizeComposite.setLayout(layout);
		sizeComposite.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		labelWidth = createField(sizeComposite, LABEL_WIDTH_PROPERTY, "Default width of the placed label (ignored for templates)");
		labelHeight = createField(sizeComposite, LABEL_HEIGTH_PROPERTY, "Default height of the placed label (ignored for templates)");

		onTopButton = createLabelAndCheck(configurationComposite, "Place label on top", PLACE_ON_TOP_PROPERTY,
			"Place the label on top of the field (instead of in front)");
		onTopButton.setEnabled(placeWithLabelsButton.getSelection());

		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(configurationComposite, SWT.NONE).setLayoutData(gd);
		fillName = createLabelAndCheck(configurationComposite, "Fill name property", FILL_NAME_PROPERTY,
			"Fill the name property of the field (based on the dataprovider)");
		fillText = createLabelAndCheck(configurationComposite, "Fill text property", FILL_TEXT_PROPERTY,
			"Fill the text property of the field based on dataprovder or column label property of the table");
		fillText.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				updateI18NEnableState();
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});

		placeHorizontally = createLabelAndCheck(configurationComposite, "Place Horizontally", PLACE_HORIZONTALLY_PROPERTY,
			"Place the field horizontal (for tableview mode)");

		gd = new GridData();
		gd.horizontalSpan = 2;
		new Label(configurationComposite, SWT.NONE).setLayoutData(gd);
		automaticI18N = createLabelAndCheck(configurationComposite, "Automatic i18n text property", AUTOMATIC_I18N_PROPERTY,
			"Fill the text property of the field or text of the label component through a generated i18n key");
		automaticI18N.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				i18nPrefix.setEnabled(automaticI18N.getSelection());
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
			}
		});

		i18nPrefix = createLabelAndField(configurationComposite, "I18N prefix", I18N_PREFIX_PROPERTY,
			"The prefix to use when generating the i18nkey (i18n:prefix_dataprovider}");

		Label i18nInfo = new Label(configurationComposite, SWT.NONE);
		gd = new GridData();
		gd.horizontalSpan = 2;
		i18nInfo.setLayoutData(gd);

		if (ServoyModelFinder.getServoyModel().getActiveProject().getSolution().getI18nDataSource() == null)
		{
			automaticI18N.setEnabled(false);
			i18nPrefix.setEnabled(false);
			i18nInfo.setText("(for I18N you need to set i18nDataSource on the solution)");
		}

		dataproviderTreeViewer = createDataproviderTree(form, persistContext, flattenedSolution, table, dataproviderOptions, inputs);

		tableViewer = createTableViewer(form, persistContext);


		tableViewer.setInput(input);

		String selection = settings.get(CONFIGURATION_SELECTION_SETTING);
		if (selection != null && preferences.has(selection))
		{
			configurationViewer.setSelection(new StructuredSelection(selection));
		}

		form.setWeights(new int[] { 40, 30, 30 });
	}

	private TableViewer createTableViewer(SashForm form, PersistContext persistContext)
	{
		final Composite container = new Composite(form, SWT.NONE);
		// define layout for the viewer

		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
		gridData.minimumWidth = 250;

		container.setLayoutData(gridData);

		TableColumnLayout tableColumnLayout = new TableColumnLayout();
		container.setLayout(tableColumnLayout);
		final TableViewer viewer = new TableViewer(container, SWT.V_SCROLL | SWT.H_SCROLL | SWT.BORDER | SWT.SINGLE | SWT.FULL_SELECTION);
		viewer.getTable().setLinesVisible(true);
		viewer.getTable().setHeaderVisible(true);
		viewer.getTable().setToolTipText("The selected dataproviders and the component or template that will be used to place a field");
		viewer.setContentProvider(ArrayContentProvider.getInstance());
		TableColumn dataproviderColumn = new TableColumn(viewer.getTable(), SWT.LEFT);
		dataproviderColumn.setText("Name");
		dataproviderColumn.setToolTipText("The dataprovider for which a field must be placed");

		TableColumn templateColumn = new TableColumn(viewer.getTable(), SWT.LEFT);
		templateColumn.setText("Component/Template");
		templateColumn.setToolTipText("The component or template that will be used for the placed field, you can override this default");

		TableColumn removeColumn = new TableColumn(viewer.getTable(), SWT.LEFT);
		removeColumn.setText("");

		TableViewerColumn dataproviderViewerColumn = new TableViewerColumn(viewer, dataproviderColumn);
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


		TableViewerColumn templateViewerColumn = new TableViewerColumn(viewer, templateColumn);
		templateViewerColumn.setEditingSupport(new TemplateEditingSupport(viewer, persistContext));
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

		final TableViewerColumn removeViewerColumn = new TableViewerColumn(viewer, removeColumn);
		removeViewerColumn.setLabelProvider(new ColumnLabelProvider()
		{
			@Override
			public Image getImage(Object element)
			{
				return Activator.getDefault().loadImageFromBundle("delete.png");
			}

			@Override
			public String getText(Object element)
			{
				return "";
			}
		});

		viewer.getTable().addListener(SWT.MouseDown, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				try
				{
					Point pt = new Point(event.x, event.y);
					ViewerCell cell = viewer.getCell(pt);
					if (cell != null && cell.getColumnIndex() == 2)
					{
						Object data = cell.getViewerRow().getElement();
						int index = input.indexOf(data);
						if (index >= 0)
						{
							input.remove(index);
						}
						if (input.size() == 0)
						{
							for (IReadyListener rl : readyListeners)
							{
								rl.isReady(false);
							}
						}
						viewer.refresh();
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		});

		tableColumnLayout.setColumnData(dataproviderColumn, new ColumnWeightData(40, 100, true));
		tableColumnLayout.setColumnData(templateColumn, new ColumnWeightData(60, 100, true));
		tableColumnLayout.setColumnData(removeColumn, new ColumnWeightData(16, 16, false));
		return viewer;
	}

	/**
	 * @param inputs
	 * @param dataproviderOptions
	 * @param table
	 * @param flattenedSolution
	 * @param persistContext
	 *
	 */
	private DataProviderTreeViewer createDataproviderTree(SashForm form, PersistContext persistContext, FlattenedSolution flattenedSolution, ITable table,
		DataProviderOptions dataproviderOptions, final Object[] inputs)
	{
		GridData gridData = new GridData();
		gridData.verticalAlignment = GridData.FILL;
		gridData.horizontalSpan = 1;
		gridData.verticalSpan = 1;
		gridData.grabExcessHorizontalSpace = true;
		gridData.grabExcessVerticalSpace = true;
		gridData.horizontalAlignment = GridData.FILL;
//		gridData.minimumWidth = 400;
		gridData.heightHint = 600;

		Composite parent = new Composite(form, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.marginRight = 5;
		parent.setLayout(layout);
		parent.setLayoutData(gridData);

		Composite confAndDelete = new Composite(parent, SWT.NONE);
		GridLayout confAndDeleteLayout = new GridLayout(3, false);
		confAndDeleteLayout.marginHeight = 0;
		confAndDeleteLayout.marginWidth = 0;
		confAndDelete.setLayout(confAndDeleteLayout);
		confAndDelete.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));

		simpleAdvanced = new Button(confAndDelete, SWT.PUSH);
		showOrHideConfiguration();
		simpleAdvanced.addSelectionListener(new SelectionListener()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				settings.put(ADVANCED_MODE_SETTING, !settings.getBoolean(ADVANCED_MODE_SETTING));
				showOrHideConfiguration();
				configurationComposite.getShell().pack(true);
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetSelected(e);
			}
		});
		final CCombo configuration = new CCombo(confAndDelete, SWT.BORDER);
		configuration.setLayoutData(new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL));
		configurationViewer = new ComboViewer(configuration);
		configurationViewer.getCCombo().setToolTipText("Select the configuration, or type in new string to save the current configuration below");
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
				if (configurationSelection == null)
				{
					configurationSelection = configurationViewer.getCCombo().getText();
				}
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
				automaticI18N.setSelection(currentSelection.optBoolean(AUTOMATIC_I18N_PROPERTY));
				i18nPrefix.setText(currentSelection.optString(I18N_PREFIX_PROPERTY));

				updatePlaceWithLabelState();
				updateI18NEnableState();
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
		deleteConf.setText("");
		deleteConf.setImage(Activator.getDefault().loadImageFromBundle("delete.png"));
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


		DataProviderTreeViewer treeviewer = new DataProviderTreeViewer(parent, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, // label provider will be overwritten when superform is known
			new DataProviderContentProvider(persistContext, flattenedSolution, table), dataproviderOptions, true, true,
			TreePatternFilter.getSavedFilterMode(settings, TreePatternFilter.FILTER_LEAFS), SWT.MULTI);
		treeviewer.setLayoutData(gridData);
		treeviewer.addSelectionChangedListener(new ISelectionChangedListener()
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

		treeviewer.getViewer().getTree().setToolTipText("Select the dataprovders for which you want to place fields");

		treeviewer.addOpenListener(new IOpenListener()
		{
			@Override
			public void open(OpenEvent event)
			{
				moveDataproviderSelection(inputs);
			}
		});
		singleClick = new Button(parent, SWT.CHECK);
		boolean singleClickSelection = settings.getBoolean(SINGLE_CLICK_SETTING);
		singleClick.setText("Use single click");
		singleClick.setSelection(singleClickSelection);
		GridData gd = new GridData();
		gd.horizontalSpan = 1;
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
			}

			@Override
			public void widgetDefaultSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}
		});

		return treeviewer;
	}

	/**
	 * @return
	 *
	 */
	private Button createLabelAndCheck(Composite parent, String label, final String property, String tooltip)
	{
		GridData gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(label);
		lbl.setLayoutData(gd);
		lbl.setToolTipText(tooltip);
		final Button button = new Button(parent, SWT.CHECK);
		gd = new GridData();
		gd.verticalAlignment = SWT.TOP;
		button.setLayoutData(gd);
		button.setToolTipText(tooltip);
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
	private Text createLabelAndField(Composite parent, String label, final String property, String tooltip)
	{
		Label lbl = new Label(parent, SWT.NONE);
		lbl.setText(label);
		lbl.setToolTipText(tooltip);
		return createField(parent, property, tooltip);
	}

	/**
	 * @param property
	 * @return
	 */
	private Text createField(Composite parent, final String property, String tooltip)
	{
		final Text text = new Text(parent, SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.setText(currentSelection.optString(property));
		text.setToolTipText(tooltip);
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
	private ComboViewer generateTypeCombo(Composite parent, String label, Object[] viewerInput, final String type, String tooltip)
	{
		Label stringLabel = new Label(parent, SWT.NONE);
		stringLabel.setText(label);
		stringLabel.setToolTipText(tooltip);
		final CCombo stringCC = new CCombo(parent, SWT.BORDER | SWT.SEARCH);
		stringCC.setToolTipText(tooltip);
		GridData layoutData = new GridData(GridData.GRAB_HORIZONTAL | GridData.HORIZONTAL_ALIGN_FILL);
		layoutData.minimumWidth = 200;
		stringCC.setLayoutData(layoutData);
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
		if (ServoyModelFinder.getServoyModel().getActiveResourcesProject() != null)
		{
			ServoyModelFinder.getServoyModel().getActiveResourcesProject().savePlaceDataproviderPreferences(preferences);
		}

		Dimension fieldSize = new Dimension(currentSelection.optInt(FIELD_WIDTH_PROPERTY, -1), currentSelection.optInt(FIELD_HEIGTH_PROPERTY, -1));
		Dimension labelSize = new Dimension(currentSelection.optInt(LABEL_WIDTH_PROPERTY, -1), currentSelection.optInt(LABEL_HEIGTH_PROPERTY, -1));

		return new PlaceDataProviderConfiguration(input, currentSelection.optInt(FIELD_SPACING_PROPERTY, -1),
			currentSelection.optBoolean(PLACE_WITH_LABELS_PROPERTY), currentSelection.optString(LABEL_COMPONENT_PROPERTY),
			currentSelection.optInt(LABEL_SPACING_PROPERTY, -1), currentSelection.optBoolean(PLACE_ON_TOP_PROPERTY),
			currentSelection.optBoolean(FILL_NAME_PROPERTY), currentSelection.optBoolean(FILL_TEXT_PROPERTY),
			currentSelection.optBoolean(PLACE_HORIZONTALLY_PROPERTY), fieldSize, labelSize, currentSelection.optBoolean(AUTOMATIC_I18N_PROPERTY),
			currentSelection.optString(I18N_PREFIX_PROPERTY));
	}

	/**
	 * @param persistContext
	 * @return
	 */
	private Object[] getComponentsAndTemplatesInput(boolean filterOnDataProviderProperty, PersistContext persistContext)
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
					if (elements != null && elements.length() == 1 && (!filterOnDataProviderProperty || hasDataproviderProperty(elements.getJSONObject(0))))
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
		Form form = persistContext != null && persistContext.getContext() != null ? (Form)persistContext.getContext().getAncestor(IRepository.FORMS) : null;

		boolean skipDefault = EditorUtil.hideDefaultComponents(form);
		for (PackageSpecification<WebObjectSpecification> pck : WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecifications().values())
		{
			if (pck.getPackageName().equals("servoycore") || (skipDefault && pck.getPackageName().equals("servoydefault"))) continue;
			for (WebObjectSpecification wos : pck.getSpecifications().values())
			{
				if (!filterOnDataProviderProperty || wos.getProperties(DataproviderPropertyType.INSTANCE).size() > 0)
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
		if (dataprovider != null)
		{
			Pair<IDataProvider, Object> row = new Pair<>(dataprovider,
				findSelectedObject(inputs, currentSelection.optString(Column.getDisplayTypeString(dataprovider.getDataProviderType()))));
			input.add(row);
			for (IReadyListener rl : readyListeners)
			{
				rl.isReady(true);
			}
			tableViewer.refresh();
		}
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
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(typeName);
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

		public TemplateEditingSupport(TableViewer viewer, PersistContext persistContext)
		{
			super(viewer);
			this.viewer = viewer;
			this.editor = new ComboBoxViewerCellEditor(viewer.getTable());
			editor.setLabelProvider(new TemplateOrWebObjectLabelProvider());
			editor.setContentProvider(ArrayContentProvider.getInstance());
			editor.setInput(getComponentsAndTemplatesInput(true, persistContext));
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

	private void updateI18NEnableState()
	{
		if (ServoyModelFinder.getServoyModel().getActiveProject().getSolution().getI18nDataSource() != null)
		{
			boolean i18nEnableState = placeWithLabelsButton.getSelection() || fillText.getSelection();
			automaticI18N.setEnabled(i18nEnableState);
			i18nPrefix.setEnabled(i18nEnableState && automaticI18N.getSelection());
		}
	}

	private void showOrHideConfiguration()
	{
		boolean advancedMode = settings.getBoolean(ADVANCED_MODE_SETTING);
		if (advancedMode)
		{
			simpleAdvanced.setText(">> Simple configuration");
			simpleAdvanced.setToolTipText("Closed the advanced configuration dialog, it will still be saved with the given name on of the right textfield");
		}
		else
		{
			simpleAdvanced.setText("<< Advanced configuration");
			simpleAdvanced.setToolTipText(
				"Opens the advanced configuration block to adjust a configuration that will be saved with the given name of the right text field");
		}
		configurationGridData.exclude = !advancedMode;
		configurationComposite.setVisible(advancedMode);
	}
}
