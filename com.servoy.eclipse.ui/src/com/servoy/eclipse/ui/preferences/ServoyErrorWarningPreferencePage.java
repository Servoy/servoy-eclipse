/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.internal.ui.dialogs.StatusUtil;
import org.eclipse.dltk.ui.dialogs.StatusInfo;
import org.eclipse.dltk.ui.util.SWTFactory;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.forms.events.ExpansionAdapter;
import org.eclipse.ui.forms.events.ExpansionEvent;
import org.eclipse.ui.forms.widgets.ExpandableComposite;
import org.eclipse.ui.forms.widgets.SharedScrolledComposite;
import org.osgi.service.prefs.BackingStoreException;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.builder.ServoyBuilder;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.util.Pair;

/**
 * Preference page that lets the user configure the type of Servoy problem markers (IGNORE, INFO, ERROR, WARNING).
 * This is minimal, just made to resemble existing Error/Warnings page.
 *
 * When we need to extend this to work as JS Error/Warnings page does with project/workspace settings and more marker types (SVY-75),
 * have a look at jdt implementation or at org.eclipse.dltk.javascript.internal.ui.preferences.JavaScriptErrorWarningPreferencePage (should be pretty reusable - it's a lot of copy paste from jdt as well)
 * and rewrite it based on that.
 *
 * @author acostescu
 */
public class ServoyErrorWarningPreferencePage extends WorkspaceOrProjectPreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{

	private IEclipsePreferences settingsNode;
	private final HashMap<String, String> changes = new HashMap<String, String>();
	private final List<Pair<Combo, Integer>> defaults = new ArrayList<Pair<Combo, Integer>>();
	private boolean doBuild = false;
	private boolean defaultsPerformed = false;
	private final List<String> problemSections = new ArrayList<String>();
	private final IStatus fBlockStatus;
	private ControlEnableState fBlockEnableState;
	private Control fConfigurationBlockControl;


	private final String ERROR_WARNING_POTENTIAL_DRAWBACKS = Messages.ErrorWarningPreferencePage_potentialDrawBacks;
	private final String ERROR_WARNING_DEVELOPER_PROBLEMS = "Developer problems";
	private final String ERROR_WARNING_RELATIONS_PROBLEMS = "Relation problems";
	private final String ERROR_WARNING_VALUELIST_PROBLEMS = "Valuelist problems";
	private final String ERROR_WARNING_RESOURCE_PROJECT_PROBLEMS = "Resource project problems";
	private final String ERROR_WARNING_STYLES_PROBLEMS = "Styles problems";
	private final String ERROR_WARNING_LOGIN_PROBLEMS = "Login problems";
	private final String ERROR_WARNING_DEPRECATED_PROPERTIES_USAGE_PROBLEMS = "Deprecated elements usage problems";
	private final String ERROR_WARNING_MODULES_PROBLEMS = "Module problems";
	private final String ERROR_WARNING_FORM_PROBLEMS = "Form problems";
	private final String ERROR_WARNING_COLUMNS_PROBLEMS = "Column problems";
	private final String ERROR_WARNING_SORT_PROBLEMS = "Sort problems";
	private final String ERROR_WARNING_SOLUTION_PROBLEMS = "Servoy solution problems";


	public ServoyErrorWarningPreferencePage()
	{
		super();
		settingsNode = InstanceScope.INSTANCE.getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE);

		fBlockEnableState = null;
		fBlockStatus = new StatusInfo();

		fillProblemCategories();
	}

	private class ScrolledPage extends SharedScrolledComposite
	{
		public ScrolledPage(Composite parent)
		{
			super(parent, SWT.V_SCROLL | SWT.H_SCROLL);
			setExpandHorizontal(true);
			setExpandVertical(true);
			Composite body = new Composite(this, SWT.NONE);
			body.setFont(parent.getFont());
			setContent(body);
		}
	}


	private void updateStatus(IStatus status)
	{
		setValid(!status.matches(IStatus.ERROR));
		StatusUtil.applyToStatusLine(this, status);
	}

	protected void doStatusChanged()
	{
		if (!isProjectPreferencePage() || useProjectSettings())
		{
			updateStatus(fBlockStatus);
		}
		else
		{
			updateStatus(new StatusInfo());
		}
	}


	protected void enablePreferenceContent(boolean enable)
	{
		if (enable)
		{
			if (fBlockEnableState != null)
			{
				fBlockEnableState.restore();
				fBlockEnableState = null;
			}
		}
		else
		{
			if (fBlockEnableState == null)
			{
				fBlockEnableState = ControlEnableState.disable(fConfigurationBlockControl);
			}
		}
	}

	@Override
	protected void enablePreferencePageContent(boolean useProjectSpecificSettings)
	{
		enablePreferenceContent(useProjectSpecificSettings);
		getfUseProjectSettings().setSelection(useProjectSpecificSettings);
		doStatusChanged();
	}


	@Override
	public Control createContents(Composite parent)
	{
		changes.clear();

		Label l = new Label(parent, SWT.NONE);
		l.setFont(parent.getFont());
		l.setText(Messages.ErrorWarningPreferencePageDescription);
		GridData gd = new GridData();
		gd.horizontalSpan = 2;
		l.setLayoutData(gd);

		ScrolledPage sc1 = new ScrolledPage(parent);
		gd = new GridData(GridData.FILL_BOTH);
		gd.heightHint = new PixelConverter(parent).convertHeightInCharsToPixels(20);
		gd.verticalIndent = 10;
		sc1.setLayoutData(gd);

		Composite composite = (Composite)sc1.getContent();
		final GridLayout layout = new GridLayout(2, false);
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		composite.setLayout(layout);
		composite.setFont(parent.getFont());

		String[] names1 = new String[] { "Warning", "Error", "Info", "Ignore" };
		List<String> ids1 = new ArrayList<String>(4);
		ids1.add(ProblemSeverity.WARNING.name());
		ids1.add(ProblemSeverity.ERROR.name());
		ids1.add(ProblemSeverity.INFO.name());
		ids1.add(ProblemSeverity.IGNORE.name());

		String[] names2 = new String[] { "Warning", "Error" };
		List<String> ids2 = new ArrayList<String>(4);
		ids2.add(ProblemSeverity.WARNING.name());
		ids2.add(ProblemSeverity.ERROR.name());

		boolean first = true;
		for (String problemSection : getProblemSections())
		{
			Composite inner = addPreferenceComposite(composite, sc1, problemSection, first);
			first = false;
			for (ErrorWarningPreferenceItem problemItem : getAssociatedProblemMarkers(problemSection))
			{
				String defaultValue = problemItem.problem.getRight().name();
				if (isProjectPreferencePage())
				{
					defaultValue = InstanceScope.INSTANCE.getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE).get(problemItem.problem.getLeft(),
						defaultValue);
				}
				if (problemItem.fullyConfigurable)
				{
					addPreferenceItem(inner, problemItem.problem.getLeft(), problemItem.description, names1, ids1, defaultValue);
				}
				else
				{
					addPreferenceItem(inner, problemItem.problem.getLeft(), problemItem.description, names2, ids2, defaultValue);
				}
			}
		}

		applyDialogFont(composite);

		fConfigurationBlockControl = composite;
		if (isProjectPreferencePage())
		{
			boolean useProjectSettings = hasProjectSpecificOptions(getProject());
			enablePreferencePageContent(useProjectSettings);
		}

		return fConfigurationBlockControl;
	}

	protected Composite addPreferenceComposite(Composite parent, ScrolledPage sc, String problemSectionName, boolean first)
	{
		final ExpandableComposite excomposite = createStyleSection(parent, sc, problemSectionName, 2);
		final Composite inner = new Composite(excomposite, SWT.NONE);
		inner.setFont(parent.getFont());
		inner.setLayout(new GridLayout(2, false));
		excomposite.setClient(inner);
		if (first) excomposite.setExpanded(true);

		return inner;
	}

	private void addPreferenceItem(Composite parent, final String key, String description, String[] names, final List<String> ids, final String defaultValue)
	{
		SWTFactory.createLabel(parent, description, 1).setLayoutData(new GridData(GridData.FILL, GridData.CENTER, true, false));
		final Combo combo = SWTFactory.createCombo(parent, SWT.READ_ONLY, 1, 0, names);
		defaults.add(new Pair<Combo, Integer>(combo, Integer.valueOf(ids.indexOf(defaultValue))));
		int idx = ids.indexOf(getPreference(key, defaultValue));
		combo.select(idx >= 0 ? idx : 0);
		combo.addModifyListener(new ModifyListener()
		{
			public void modifyText(ModifyEvent e)
			{
				int index = combo.getSelectionIndex();
				if (ids.get(index) != getPreference(key, defaultValue))
				{
					changes.put(key, ids.get(index));
				}
				else
				{
					changes.remove(key);
				}
			}
		});
	}

	private String getPreference(String key, String defaultValue)
	{
		return settingsNode.get(key, defaultValue);
	}

	protected ExpandableComposite createStyleSection(Composite parent, final ScrolledPage sc1, String label, int nColumns)
	{
		ExpandableComposite excomposite = new ExpandableComposite(parent, SWT.NONE, ExpandableComposite.TWISTIE | ExpandableComposite.CLIENT_INDENT);
		excomposite.setText(label);
		excomposite.setExpanded(false);
		excomposite.setFont(JFaceResources.getFontRegistry().getBold(JFaceResources.DIALOG_FONT));
		excomposite.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, nColumns, 1));
		excomposite.addExpansionListener(new ExpansionAdapter()
		{
			@Override
			public void expansionStateChanged(ExpansionEvent e)
			{
				sc1.reflow(true);
			}
		});
		return excomposite;
	}

	private List<String> getProblemSections()
	{
		return problemSections;
	}


	private void fillProblemCategories()
	{
		problemSections.add(ERROR_WARNING_POTENTIAL_DRAWBACKS);
		problemSections.add(ERROR_WARNING_DEVELOPER_PROBLEMS);
		problemSections.add(ERROR_WARNING_LOGIN_PROBLEMS);
		problemSections.add(ERROR_WARNING_RESOURCE_PROJECT_PROBLEMS);
		problemSections.add(ERROR_WARNING_DEPRECATED_PROPERTIES_USAGE_PROBLEMS);
		problemSections.add(ERROR_WARNING_COLUMNS_PROBLEMS);
		problemSections.add(ERROR_WARNING_SORT_PROBLEMS);
		problemSections.add(ERROR_WARNING_RELATIONS_PROBLEMS);
		problemSections.add(ERROR_WARNING_VALUELIST_PROBLEMS);
		problemSections.add(ERROR_WARNING_MODULES_PROBLEMS);
		problemSections.add(ERROR_WARNING_FORM_PROBLEMS);
		problemSections.add(ERROR_WARNING_STYLES_PROBLEMS);
		problemSections.add(ERROR_WARNING_SOLUTION_PROBLEMS);
		Collections.sort(problemSections);
	}

	private class ErrorWarningPreferenceItem
	{
		Pair<String, ProblemSeverity> problem;
		String description;
		boolean fullyConfigurable;

		public ErrorWarningPreferenceItem(Pair<String, ProblemSeverity> problem, String description, boolean fullyConfigurable)
		{
			this.problem = problem;
			this.description = description;
			this.fullyConfigurable = fullyConfigurable;
		}
	}

	private List<ErrorWarningPreferenceItem> getAssociatedProblemMarkers(String problemSection)
	{
		List<ErrorWarningPreferenceItem> associatedProblemMarkers = new ArrayList<ErrorWarningPreferenceItem>();

		Comparator<ErrorWarningPreferenceItem> descriptionComparator = new Comparator<ErrorWarningPreferenceItem>()
		{
			public int compare(ErrorWarningPreferenceItem o1, ErrorWarningPreferenceItem o2)
			{
				return o1.description.compareToIgnoreCase(o2.description);
			}
		};

		if (ERROR_WARNING_POTENTIAL_DRAWBACKS.equals(problemSection))
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW, Messages.ErrorWarningPreferencePage_tooManyColumns, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.LEVEL_PERFORMANCE_TABS_PORTALS, Messages.ErrorWarningPreferencePage_tooManyTabsPortals, true));
		}
		else if (ERROR_WARNING_DEVELOPER_PROBLEMS.equals(problemSection)) //RENAME or reorganize THIS!!!!!!
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_TABLE_REFERENCE, Messages.ErrorWarningPreferencePage_invalidTableReference, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.METHOD_EVENT_PARAMETERS, Messages.ErrorWarningPreferencePage_methodEventParameters, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.METHOD_NO_RETURN, Messages.ErrorWarningPreferencePage_methodNoReturn, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MEDIA_TIFF, Messages.ErrorWarningPreferencePage_mediaTiff, true));
			associatedProblemMarkers
				.add(new ErrorWarningPreferenceItem(ServoyBuilder.VARIANT_ID_UNRESOLVED, Messages.ErrorWarningPreferencePage_variantId, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.CALCULATION_FORM_ACCESS, Messages.ErrorWarningPreferencePage_calculationFormAccess, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.IMAGE_MEDIA_NOT_SET, Messages.ErrorWarningPreferencePage_imageMediaNotSet, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DATAPROVIDER_MISSING_CONVERTER,
				Messages.ErrorWarningPreferencePage_dataproviderMissingConverter, false));
		}
		else if (ERROR_WARNING_LOGIN_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION,
				Messages.ErrorWarningPreferencePage_loginFormWithDatasourceInLoginSolution, true));
		}
		else if (ERROR_WARNING_RESOURCE_PROJECT_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.PROPERTY_MULTIPLE_METHODS_ON_SAME_TABLE,
				Messages.ErrorWarningPreferencePage_propertyMultipleMethodsOnSameTable, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.SERVER_MISSING_DRIVER, Messages.ErrorWarningPreferencePage_serverMissingDriver, false));
		}
		else if (ERROR_WARNING_DEPRECATED_PROPERTIES_USAGE_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DEPRECATED_PROPERTY_USAGE_PROBLEM,
				Messages.ErrorWarningPreferencePage_deprecatedPropertyUsageProblem, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
				Messages.ErrorWarningPreferencePage_deprecatedScriptElementUsageProblem, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DEPRECATED_ELEMENT_USAGE_PROBLEM,
				Messages.ErrorWarningPreferencePage_deprecatedElementUsageProblem, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.DEPRECATED_SPECIFICATION, Messages.ErrorWarningPreferencePage_deprecatedSpecUsageProblem, true));
		}
		else if (ERROR_WARNING_COLUMNS_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_UUID_FLAG_NOT_SET, Messages.ErrorWarningPreferencePage_columnUUIDFlagNotSet, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_DATABASE_IDENTITY_PROBLEM,
				Messages.ErrorWarningPreferencePage_columnDatabaseIdentityProblem, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_DUPLICATE_NAME_DPID, Messages.ErrorWarningPreferencePage_columnDuplicateNameDPID, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_FOREIGN_TYPE_PROBLEM, Messages.ErrorWarningPreferencePage_columnForeignTypeProblem, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_INCOMPATIBLE_TYPE_FOR_SEQUENCE,
				Messages.ErrorWarningPreferencePage_columnIncompatibleTypeForSequence, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_INCOMPATIBLE_WITH_UUID,
				Messages.ErrorWarningPreferencePage_columnIncompatbleWithUUID, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_LOOKUP_INVALID, Messages.ErrorWarningPreferencePage_columnLookupInvalid, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_VALIDATOR_INVALID, Messages.ErrorWarningPreferencePage_columnValidatorInvalid, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_CONVERTER_INVALID, Messages.ErrorWarningPreferencePage_columnConverterInvalid, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.ROW_IDENT_SHOULD_NOT_BE_NULL,
				Messages.ErrorWarningPreferencePage_columnRowIdentShouldNotAllowNull, false));
		}
		else if (ERROR_WARNING_SORT_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_invalidSortOptionsColumnNotFound, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE,
				Messages.ErrorWarningPreferencePage_invalidSortOptionsRelationDifferentPrimaryDatasource, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_SORT_OPTIONS_RELATION_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_invalidSortOptionsRelationNotFound, false));
		}
		else if (ERROR_WARNING_RELATIONS_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_SERVER_DUPLICATE, Messages.ErrorWarningPreferencePage_relationServerDuplicate, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_TABLE_NOT_FOUND, Messages.ErrorWarningPreferencePage_relationTableNotFound, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_TABLE_WITHOUT_PK, Messages.ErrorWarningPreferencePage_relationTableWithoutPK, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_EMPTY, Messages.ErrorWarningPreferencePage_relationEmpty, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_relationItemDataproviderNotFound, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_UUID_PROBLEM, Messages.ErrorWarningPreferencePage_relationItemUUIDProblem, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM, Messages.ErrorWarningPreferencePage_relationItemTypeProblem, true));
		}
		else if (ERROR_WARNING_VALUELIST_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_CUSTOM_VALUES_WITH_DB_INFO,
				Messages.ErrorWarningPreferencePage_valuelistCustomValuesWithDBInfo, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_INVALID_CUSTOM_VALUES,
				Messages.ErrorWarningPreferencePage_valuelistInvalidCustomValues, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_ENTITY_NOT_FOUND, Messages.ErrorWarningPreferencePage_valuelistEntityNotFound, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_MALFORMED_TABLE_DEFINITION,
				Messages.ErrorWarningPreferencePage_valuelistDBMalformedTableDefinition, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_NOT_TABLE_OR_RELATION,
				Messages.ErrorWarningPreferencePage_valuelistDBNotTableOrRelation, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_SERVER_DUPLICATE,
				Messages.ErrorWarningPreferencePage_valuelistDBServerDuplicate, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_TABLE_NO_PK, Messages.ErrorWarningPreferencePage_valuelistDBTableNoPk, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_WITH_CUSTOM_VALUES,
				Messages.ErrorWarningPreferencePage_valuelistDBWithCustomValues, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_RELATION_SEQUENCE_INCONSISTENT,
				Messages.ErrorWarningPreferencePage_valuelistRelationSequenceInconsistent, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_RELATION_WITH_DATASOURCE,
				Messages.ErrorWarningPreferencePage_valuelistRelationWithDatasource, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DATAPROVIDER_TYPE_MISMATCH,
				Messages.ErrorWarningPreferencePage_valuelistDataproviderTypeMismatch, false));
		}
		else if (ERROR_WARNING_MODULES_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_DIFFERENT_I18N_TABLE, Messages.ErrorWarningPreferencePage_moduleDifferentI18NTable, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_DIFFERENT_RESOURCE_PROJECT,
				Messages.ErrorWarningPreferencePage_moduleDifferentResourceProject, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_MISPLACED, Messages.ErrorWarningPreferencePage_moduleMisplaced, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_NOT_FOUND, Messages.ErrorWarningPreferencePage_moduleNotFound, false));
		}
		else if (ERROR_WARNING_FORM_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_COLUMN_LENGTH_TOO_SMALL, Messages.ErrorWarningPreferencePage_formColumnLengthTooSmall, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE,
				Messages.ErrorWarningPreferencePage_formDataproviderAggregateNotEditable, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_INVALID_DATAPROVIDER, Messages.ErrorWarningPreferencePage_formInvalidDataprovider, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DERIVED_FORM_DIFFERENT_TABLE,
				Messages.ErrorWarningPreferencePage_formDerivedFormDifferentTable, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DERIVED_FORM_REDEFINED_VARIABLE,
				Messages.ErrorWarningPreferencePage_formDerivedFormRedefinedVariable, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DUPLICATE_PART, Messages.ErrorWarningPreferencePage_formDuplicatePart, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_EDITABLE_COMBOBOX_CUSTOM_VALUELIST,
				Messages.ErrorWarningPreferencePage_formEditableComboboxCustomValuelist, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_EXTENDS_CYCLE, Messages.ErrorWarningPreferencePage_formExtendsCycle, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_EXTENDS_FORM_ELEMENT_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formExtendsFormElementNotFound, false));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FILE_NAME_INCONSISTENT, Messages.ErrorWarningPreferencePage_formFileNameInconsistent, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FORMAT_INVALID, Messages.ErrorWarningPreferencePage_formFormatInvalid, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_INCOMPATIBLE_ELEMENT_TYPE,
				Messages.ErrorWarningPreferencePage_formIncompatibleElementType, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_COMPONENT_INVALID_LAYOUT_COMBINATION,
				Messages.ErrorWarningPreferencePage_formComponentInvalidLayoutCombination, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_LABEL_FOR_ELEMENT_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formLabelForElementNotFound, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_ELEMENT_DUPLICATE_TAB_SEQUENCE,
				Messages.ErrorWarningPreferencePage_formElementDuplicateTabSequence, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_ELEMENT_OUTSIDE_BOUNDS, Messages.ErrorWarningPreferencePage_formElementOutsideBounds, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_OBSOLETE_ELEMENT, Messages.ErrorWarningPreferencePage_formObsoleteElement, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_REQUIRED_PROPERTY_MISSING,
				Messages.ErrorWarningPreferencePage_formRequiredPropertyMissing, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FIELD_RELATED_VALUELIST,
				Messages.ErrorWarningPreferencePage_formFieldRelatedValuelist, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PORTAL_INVALID_RELATION_NAME,
				Messages.ErrorWarningPreferencePage_formPortalInvalidRelationName, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_METHOD_NOT_ACCESIBLE,
				Messages.ErrorWarningPreferencePage_formPropertyMethodNotAccessible, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_MULTIPLE_METHODS_ON_SAME_ELEMENT,
				Messages.ErrorWarningPreferencePage_formPropertyMultipleMethodsOnSameElement, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_TABPANEL_TAB_IMAGE_TOO_LARGE,
				Messages.ErrorWarningPreferencePage_formTabPanelTabImageTooLarge, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_RELATED_TAB_DIFFERENT_TABLE,
				Messages.ErrorWarningPreferencePage_formRelatedTabDifferentTable, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_RELATED_TAB_UNSOLVED_RELATION,
				Messages.ErrorWarningPreferencePage_formRelatedTabUnsolvedRelation, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_RELATED_TAB_UNSOLVED_UUID,
				Messages.ErrorWarningPreferencePage_formRelatedTabUnsolvedUuid, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formPropertyTargetNotFound, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_INVALID_TABLE, Messages.ErrorWarningPreferencePage_formInvalidTable, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_TYPEAHEAD_UNSTORED_CALCULATION,
				Messages.ErrorWarningPreferencePage_formTypeAheadUnstoredCalculation, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.FORM_VARIABLE_TYPE_COL, Messages.ErrorWarningPreferencePage_formVariableTableCol, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_REFERENCE_INVALID_PROPERTY,
				Messages.ErrorWarningPreferencePage_formReferenceInvalidProperty, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_REFERENCE_INVALID_SCRIPT,
				Messages.ErrorWarningPreferencePage_formReferenceInvalidScript, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.NON_ACCESSIBLE_PERSIST_IN_MODULE_USED_IN_PARENT_SOLUTION,
				Messages.ErrorWarningPreferencePage_nonAccessibleFormInModuleUsedInParentSolutionForm, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.METHOD_NUMBER_OF_ARGUMENTS_MISMATCH,
				Messages.ErrorWarningPreferencePage_methodNumberOfArgsMismatch, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.ROLLOVER_NOT_WORKING, Messages.ErrorWarningPreferencePage_rolloverImageAndCursorNotWorking, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.TAB_SEQUENCE_NOT_SET, Messages.ErrorWarningPreferencePage_formTabPanelTabSequenceNotSet, true));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.ELEMENT_EXTENDS_DELETED_ELEMENT,
				Messages.ErrorWarningPreferencePage_formElementExtendsDeletedElement, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.METHOD_OVERRIDE_PROBLEM, Messages.ErrorWarningPreferencePage_methodOverrideProblem, true));
		}
		else if (ERROR_WARNING_STYLES_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_CLASS_NO_STYLE, Messages.ErrorWarningPreferencePage_styleClassNoStyle, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_CLASS_NOT_FOUND, Messages.ErrorWarningPreferencePage_styleClassNotFound, true));
			associatedProblemMarkers.add(
				new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_NOT_FOUND, Messages.ErrorWarningPreferencePage_styleNotFound, true));
		}
		else if (ERROR_WARNING_SOLUTION_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER,
				Messages.ErrorWarningPreferencePage_solutionElementNameInvalidIdentifier, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED,
				Messages.ErrorWarningPreferencePage_solutionPropertyFormCannotBeInstantiated, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_solutionPropertyTargetNotFound, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_USED_AS_WEBSERVICE_MUSTAUTHENTICATE_PROBLEM,
				Messages.ErrorWarningPreferencePage_solutionUsedAsWebServiceMustAuthenticateProblem, false));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.ERROR_MISSING_PROJECT_REFERENCE,
				Messages.ErrorWarningPreferencePage_errorMissingProjectReference, false));
		}

		Collections.sort(associatedProblemMarkers, descriptionComparator);

		return associatedProblemMarkers;
	}

	@Override
	protected void performDefaults()
	{
		for (Pair<Combo, Integer> p : defaults)
		{
			p.getLeft().select(p.getRight().intValue() >= 0 ? p.getRight().intValue() : 0);
		}
		doBuild = true;
		defaultsPerformed = true;
		super.performDefaults();
	}

	@Override
	public boolean performCancel()
	{
		doBuild = false;
		changes.clear();
		return super.performCancel();
	}

	@Override
	public boolean performOk()
	{
		try
		{
			if (isProjectPreferencePage() && !isProjectSpecificSettingsChecked() && hasProjectSpecificOptions(getProject()))
			{
				settingsNode.clear();
				doBuild = true;
			}
			else if (changes.size() > 0)
			{
				for (Entry<String, String> e : changes.entrySet())
				{
					settingsNode.put(e.getKey(), e.getValue());
				}
				settingsNode.flush();
				changes.clear();
				doBuild = true;
			}
			if (doBuild)
			{
				if (defaultsPerformed)
				{
					settingsNode.clear();
					defaultsPerformed = false;
				}
				ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
				doBuild = false;
			}
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
		return super.performOk();
	}

	public void init(IWorkbench workbench)
	{
		// not used
	}

	public IAdaptable getElement()
	{
		return getProject();
	}

	/*
	 * @see IWorkbenchPropertyPage#setElement(IAdaptable)
	 */
	public void setElement(IAdaptable element)
	{
		setProject((IProject)element.getAdapter(IResource.class));
		if (getProject() != null)
		{
			settingsNode = new ProjectScope(getProject()).getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE);
		}
	}

	/*
	 * from boolean org.eclipse.dltk.internal.ui.preferences.OptionsConfigurationBlock.hasProjectSpecificOptions(IProject project)
	 */
	@Override
	public boolean hasProjectSpecificOptions(IProject project)
	{
		try
		{
			return new ProjectScope(project).getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE).keys().length > 0;
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
		return false;
	}


	@Override
	protected String getPreferencePageId()
	{
		return "com.servoy.eclipse.ui.preferences.error.warning";
	}

	@Override
	protected String getPropertyPageId()
	{
		return "com.servoy.eclipse.ui.propertyPage.error.warning";
	}
}
