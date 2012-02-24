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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.dialogs.StatusUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.dltk.ui.dialogs.ProjectSelectionDialog;
import org.eclipse.dltk.ui.dialogs.StatusInfo;
import org.eclipse.dltk.ui.preferences.PreferencesMessages;
import org.eclipse.dltk.ui.util.SWTFactory;
import org.eclipse.jface.dialogs.ControlEnableState;
import org.eclipse.jface.layout.PixelConverter;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.resource.JFaceResources;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.eclipse.ui.IWorkbenchPropertyPage;
import org.eclipse.ui.dialogs.PreferencesUtil;
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
public class ServoyErrorWarningPreferencePage extends PreferencePage implements IWorkbenchPreferencePage, IWorkbenchPropertyPage
{

	private IEclipsePreferences settingsNode;
	private final HashMap<String, String> changes = new HashMap<String, String>();
	private final List<Pair<Combo, Integer>> defaults = new ArrayList<Pair<Combo, Integer>>();
	private final List<String> problemSections = new ArrayList<String>();
	private final IStatus fBlockStatus;
	private SelectionButtonDialogField fUseProjectSettings;
	private ControlEnableState fBlockEnableState;
	private Control fConfigurationBlockControl;
	private IProject fProject; // project or null
	private final Map fData; // page data
	public static final String DATA_NO_LINK = "PropertyAndPreferencePage.nolink"; //$NON-NLS-1$
	private Link fChangeWorkspaceSettings;

	private final String ERROR_WARNING_POTENTIAL_DRAWBACKS = Messages.ErrorWarningPreferencePage_potentialDrawBacks;
	private final String ERROR_WARNING_DEVELOPER_PROBLEMS = "Developer problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_RELATIONS_PROBLEMS = "Relation problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_VALUELIST_PROBLEMS = "Valuelist problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_RESOURCE_PROJECT_PROBLEMS = "Resource project problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_STYLES_PROBLEMS = "Styles problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_LOGIN_PROBLEMS = "Login problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_DEPRECATED_PROPERTIES_USAGE_PROBLEMS = "Deprecated properties usage problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_DUPLICATION_PROBLEMS = "Duplication problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_DATABASE_INFORMATION_PROBLEMS = "Database information problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_MODULES_PROBLEMS = "Module problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_FORM_PROBLEMS = "Form problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_COLUMNS_PROBLEMS = "Columns problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_SORT_PROBLEMS = "Sort problems"; //$NON-NLS-1$
	private final String ERROR_WARNING_SOLUTION_PROBLEMS = "Servoy solution problems"; //$NON-NLS-1$


	public ServoyErrorWarningPreferencePage()
	{
		settingsNode = InstanceScope.INSTANCE.getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE);
		fProject = null;
		fData = null;
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

	protected boolean offerLink()
	{
		return fData == null || !Boolean.TRUE.equals(fData.get(DATA_NO_LINK));
	}

	private void updateLinkVisibility()
	{
		if (fChangeWorkspaceSettings == null || fChangeWorkspaceSettings.isDisposed())
		{
			return;
		}

		if (isProjectPreferencePage())
		{
			fChangeWorkspaceSettings.setEnabled(!useProjectSettings());
		}
	}

	protected boolean useProjectSettings()
	{
		return isProjectPreferencePage() && fUseProjectSettings != null && fUseProjectSettings.isSelected();
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

	private boolean projectSpecificSetting = false;

	protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings)
	{
		projectSpecificSetting = useProjectSpecificSettings;
		fUseProjectSettings.setSelection(useProjectSpecificSettings);
		enablePreferenceContent(useProjectSpecificSettings);
		updateLinkVisibility();
		doStatusChanged();
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

	protected boolean supportsProjectSpecificOptions()
	{
		return getPropertyPageId() != null;
	}

	public IAdaptable getElement()
	{
		return fProject;
	}

	/*
	 * @see IWorkbenchPropertyPage#setElement(IAdaptable)
	 */
	public void setElement(IAdaptable element)
	{
		fProject = (IProject)element.getAdapter(IResource.class);
		if (fProject != null)
		{
			settingsNode = new ProjectScope(fProject).getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE);
		}
	}

	protected boolean isProjectPreferencePage()
	{
		return fProject != null;
	}

	protected IProject getProject()
	{
		return fProject;
	}

	@Override
	protected Label createDescriptionLabel(Composite parent)
	{
		if (isProjectPreferencePage())
		{
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setFont(parent.getFont());
			GridLayout layout = new GridLayout();
			layout.marginHeight = 0;
			layout.marginWidth = 0;
			layout.numColumns = 2;
			composite.setLayout(layout);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

			IDialogFieldListener listener = new IDialogFieldListener()
			{
				public void dialogFieldChanged(DialogField field)
				{
					enableProjectSpecificSettings(((SelectionButtonDialogField)field).isSelected());
				}
			};

			fUseProjectSettings = new SelectionButtonDialogField(SWT.CHECK);
			fUseProjectSettings.setDialogFieldListener(listener);
			fUseProjectSettings.setLabelText(PreferencesMessages.PropertyAndPreferencePage_useprojectsettings_label);
			fUseProjectSettings.doFillIntoGrid(composite, 1);
			LayoutUtil.setHorizontalGrabbing(fUseProjectSettings.getSelectionButton(null));

			if (offerLink())
			{
				fChangeWorkspaceSettings = createLink(composite, PreferencesMessages.PropertyAndPreferencePage_useworkspacesettings_change);
				fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, false, false));
			}
			else
			{
				LayoutUtil.setHorizontalSpan(fUseProjectSettings.getSelectionButton(null), 2);
			}

			Label horizontalLine = new Label(composite, SWT.SEPARATOR | SWT.HORIZONTAL);
			horizontalLine.setLayoutData(new GridData(GridData.FILL, GridData.FILL, true, false, 2, 1));
			horizontalLine.setFont(composite.getFont());
		}
		else if (supportsProjectSpecificOptions() && offerLink())
		{
			fChangeWorkspaceSettings = createLink(parent, PreferencesMessages.PropertyAndPreferencePage_showprojectspecificsettings_label);
			fChangeWorkspaceSettings.setLayoutData(new GridData(SWT.END, SWT.CENTER, true, false));
		}

		return super.createDescriptionLabel(parent);
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

		String[] names = new String[] { "Warning", "Error", "Info", "Ignore" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
		List<String> ids = new ArrayList<String>(4);
		ids.add(ProblemSeverity.WARNING.name());
		ids.add(ProblemSeverity.ERROR.name());
		ids.add(ProblemSeverity.INFO.name());
		ids.add(ProblemSeverity.IGNORE.name());

		for (String problemSection : getProblemSections())
		{
			Composite inner = addPreferenceComposite(composite, sc1, problemSection);
			for (ErrorWarningPreferenceItem problemItem : getAssociatedProblemMarkers(problemSection))
			{
				String defaultValue = problemItem.problem.getRight().name();
				if (isProjectPreferencePage())
				{
					defaultValue = InstanceScope.INSTANCE.getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE).get(problemItem.problem.getLeft(), defaultValue);
				}
				addPreferenceItem(inner, problemItem.problem.getLeft(), problemItem.description, names, ids, defaultValue);
			}
		}

		applyDialogFont(composite);

		fConfigurationBlockControl = composite;
		if (isProjectPreferencePage())
		{
			boolean useProjectSettings = hasProjectSpecificOptions(getProject());
			enableProjectSpecificSettings(useProjectSettings);
		}

		return fConfigurationBlockControl;
	}

	protected Composite addPreferenceComposite(Composite parent, ScrolledPage sc, String problemSectionName)
	{
		final ExpandableComposite excomposite = createStyleSection(parent, sc, problemSectionName, 2);
		final Composite inner = new Composite(excomposite, SWT.NONE);
		inner.setFont(parent.getFont());
		inner.setLayout(new GridLayout(2, false));
		excomposite.setClient(inner);

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
		problemSections.add(ERROR_WARNING_DUPLICATION_PROBLEMS);
		problemSections.add(ERROR_WARNING_DATABASE_INFORMATION_PROBLEMS);
		problemSections.add(ERROR_WARNING_COLUMNS_PROBLEMS);
		problemSections.add(ERROR_WARNING_SORT_PROBLEMS);
		problemSections.add(ERROR_WARNING_RELATIONS_PROBLEMS);
		problemSections.add(ERROR_WARNING_VALUELIST_PROBLEMS);
		problemSections.add(ERROR_WARNING_MODULES_PROBLEMS);
		problemSections.add(ERROR_WARNING_FORM_PROBLEMS);
		problemSections.add(ERROR_WARNING_STYLES_PROBLEMS);
		Collections.sort(problemSections);
	}

	private class ErrorWarningPreferenceItem
	{
		Pair<String, ProblemSeverity> problem;
		String description;

		public ErrorWarningPreferenceItem(Pair<String, ProblemSeverity> problem, String description)
		{
			this.problem = problem;
			this.description = description;
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
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW,
				Messages.ErrorWarningPreferencePage_tooManyColumns));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.LEVEL_PERFORMANCE_TABS_PORTALS,
				Messages.ErrorWarningPreferencePage_tooManyTabsPortals));
		}
		else if (ERROR_WARNING_DEVELOPER_PROBLEMS.equals(problemSection)) //RENAME or reorganize THIS!!!!!!
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.TABLE_MARKED_AS_HIDDEN_BUT_USED_IN,
				Messages.ErrorWarningPreferencePage_tableMarkedAsHiddedButUsedIn));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.ITEM_REFERENCES_INVALID_TABLE,
				Messages.ErrorWarningPreferencePage_itemReferencesInvalidTable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.METHOD_EVENT_PARAMETERS,
				Messages.ErrorWarningPreferencePage_methodEventParameters));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MEDIA_TIFF, Messages.ErrorWarningPreferencePage_mediaTiff));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.CALCULATION_FORM_ACCESS,
				Messages.ErrorWarningPreferencePage_calculationFormAccess));
		}
		else if (ERROR_WARNING_LOGIN_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION,
				Messages.ErrorWarningPreferencePage_loginFormWithDatasourceInLoginSolution));
		}
		else if (ERROR_WARNING_RESOURCE_PROJECT_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.NO_RESOURCE_REFERENCE,
				Messages.ErrorWarningPreferencePage_noResourceReference));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.REFERENCES_TO_MULTIPLE_RESOURCES,
				Messages.ErrorWarningPreferencePage_referencesToMultipleResources));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.PROPERTY_MULTIPLE_METHODS_ON_SAME_TABLE,
				Messages.ErrorWarningPreferencePage_propertyMultipleMethodsOnSameTable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SERVER_MISSING_DRIVER,
				Messages.ErrorWarningPreferencePage_serverMissingDriver));
		}
		else if (ERROR_WARNING_DEPRECATED_PROPERTIES_USAGE_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DEPRECATED_PROPERTY_USAGE_PROBLEM,
				Messages.ErrorWarningPreferencePage_deprecatedPropertyUsageProblem));
		}
		else if (ERROR_WARNING_DUPLICATION_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DUPLICATION_UUID_DUPLICATE,
				Messages.ErrorWarningPreferencePage_duplicationUUIDDuplicate));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DUPLICATION_DUPLICATE_ENTITY_FOUND,
				Messages.ErrorWarningPreferencePage_duplicationDuplicateEntityFound));
		}
		else if (ERROR_WARNING_DATABASE_INFORMATION_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DBI_BAD_INFO, Messages.ErrorWarningPreferencePage_DBIBadDBInfo));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DBI_COLUMN_CONFLICT,
				Messages.ErrorWarningPreferencePage_DBIColumnConflict));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DBI_COLUMN_MISSING_FROM_DB,
				Messages.ErrorWarningPreferencePage_DBIColumnMissingFromDB));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DBI_COLUMN_MISSING_FROM_DB_FILE,
				Messages.ErrorWarningPreferencePage_DBIColumnMissingFromDBIFile));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DBI_FILE_MISSING, Messages.ErrorWarningPreferencePage_DBIFileMissing));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.DBI_TABLE_MISSING, Messages.ErrorWarningPreferencePage_DBITableMissing));
		}
		else if (ERROR_WARNING_COLUMNS_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_UUID_FLAG_NOT_SET,
				Messages.ErrorWarningPreferencePage_columnUUIDFlagNotSet));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_DATABASE_IDENTITY_PROBLEM,
				Messages.ErrorWarningPreferencePage_columnDatabaseIdentityProblem));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_DUPLICATE_NAME_DPID,
				Messages.ErrorWarningPreferencePage_columnDuplicateNameDPID));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_FOREIGN_TYPE_PROBLEM,
				Messages.ErrorWarningPreferencePage_columnForeignTypeProblem));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_INCOMPATIBLE_TYPE_FOR_SEQUENCE,
				Messages.ErrorWarningPreferencePage_columnIncompatibleTypeForSequence));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_INSUFFICIENT_LENGTH_FOR_SEQUENCE,
				Messages.ErrorWarningPreferencePage_columnInsufficientLengthForSequence));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.COLUMN_LOOKUP_INVALID,
				Messages.ErrorWarningPreferencePage_columnLookupInvalid));
		}
		else if (ERROR_WARNING_SORT_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_invalidSortOptionsColumnNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE,
				Messages.ErrorWarningPreferencePage_invalidSortOptionsRelationDifferentPrimaryDatasource));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.INVALID_SORT_OPTIONS_RELATION_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_invalidSortOptionsRelationNotFound));
		}
		else if (ERROR_WARNING_RELATIONS_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_PRIMARY_SERVER_WITH_PROBLEMS,
				Messages.ErrorWarningPreferencePage_relationPrimaryServerWithProblems));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_PRIMARY_SERVER_DUPLICATE,
				Messages.ErrorWarningPreferencePage_relationPrimaryServerDuplicate));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_PRIMARY_TABLE_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_relationPrimaryTableNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_PRIMARY_TABLE_WITHOUT_PK,
				Messages.ErrorWarningPreferencePage_relationPrimaryTableWithoutPK));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_FOREIGN_SERVER_WITH_PROBLEMS,
				Messages.ErrorWarningPreferencePage_relationForeignServerWithProblems));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_FOREIGN_SERVER_DUPLICATE,
				Messages.ErrorWarningPreferencePage_relationForeignServerDuplicate));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_FOREIGN_TABLE_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_relationForeignTableNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_FOREIGN_TABLE_WITHOUT_PK,
				Messages.ErrorWarningPreferencePage_relationForeignTableWithoutPK));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_EMPTY, Messages.ErrorWarningPreferencePage_relationEmpty));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_NO_PRIMARY_DATAPROVIDER,
				Messages.ErrorWarningPreferencePage_relationItemNoPrimaryDataprovider));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_PRIMARY_DATAPROVIDER_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_relationItemPrimaryDataproviderNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_NO_FOREIGN_DATAPROVIDER,
				Messages.ErrorWarningPreferencePage_relationItemNoForeignDataprovider));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_FOREIGN_DATAPROVIDER_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_relationItemForeignDataproviderNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_UUID_PROBLEM,
				Messages.ErrorWarningPreferencePage_relationItemUUIDProblem));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_ITEM_TYPE_PROBLEM,
				Messages.ErrorWarningPreferencePage_relationItemTypeProblem));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATION_GENERIC_ERROR,
				Messages.ErrorWarningPreferencePage_relationGenericError));
		}
		else if (ERROR_WARNING_VALUELIST_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_CUSTOM_VALUES_WITH_DB_INFO,
				Messages.ErrorWarningPreferencePage_valuelistCustomValuesWithDBInfo));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_INVALID_CUSTOM_VALUES,
				Messages.ErrorWarningPreferencePage_valuelistInvalidCustomValues));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_DATASOURCE_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_valuelistDBDatasourceNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_MALFORMED_TABLE_DEFINITION,
				Messages.ErrorWarningPreferencePage_valuelistDBMalformedTableDefinition));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_NOT_TABLE_OR_RELATION,
				Messages.ErrorWarningPreferencePage_valuelistDBNotTableOrRelation));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_SERVER_DUPLICATE,
				Messages.ErrorWarningPreferencePage_valuelistDBServerDuplicate));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_TABLE_NOT_ACCESSIBLE,
				Messages.ErrorWarningPreferencePage_valuelistDBTableNotAccessible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_DB_WITH_CUSTOM_VALUES,
				Messages.ErrorWarningPreferencePage_valuelistDBWithCustomValues));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_GENERIC_ERROR,
				Messages.ErrorWarningPreferencePage_valuelistGenericError));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_GLOBAL_METHOD_NOT_ACCESSIBLE,
				Messages.ErrorWarningPreferencePage_valuelistGlobalMethodNotAccessible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_GLOBAL_METHOD_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_valuelistGlobalMethodNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_RELATION_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_valuelistRelationNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_RELATION_SEQUENCE_INCONSISTENT,
				Messages.ErrorWarningPreferencePage_valuelistRelationSequenceInconsistent));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_RELATION_WITH_DATASOURCE,
				Messages.ErrorWarningPreferencePage_valuelistRelationWithDatasource));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.VALUELIST_TYPE_UNKNOWN,
				Messages.ErrorWarningPreferencePage_valuelistTypeUnknown));
		}
		else if (ERROR_WARNING_MODULES_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_DIFFERENT_I18N_TABLE,
				Messages.ErrorWarningPreferencePage_moduleDifferentI18NTable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_DIFFERENT_RESOURCE_PROJECT,
				Messages.ErrorWarningPreferencePage_moduleDifferentResourceProject));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_MISPLACED, Messages.ErrorWarningPreferencePage_moduleMisplaced));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.MODULE_NOT_FOUND, Messages.ErrorWarningPreferencePage_moduleNotFound));
		}
		else if (ERROR_WARNING_FORM_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_COLUMN_LENGTH_TOO_SMALL,
				Messages.ErrorWarningPreferencePage_formColumnLengthTooSmall));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE,
				Messages.ErrorWarningPreferencePage_formDataproviderAggregateNotEditable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DATAPROVIDER_NOT_BASED_ON_FORM_TABLE,
				Messages.ErrorWarningPreferencePage_formDataproviderNotBasedOnFormTable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DATAPROVIDER_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formDataproviderNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DERIVED_FORM_DIFFERENT_TABLE,
				Messages.ErrorWarningPreferencePage_formDerivedFormDifferentTable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DERIVED_FORM_REDEFINED_VARIABLE,
				Messages.ErrorWarningPreferencePage_formDerivedFormRedefinedVariable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_DUPLICATE_PART,
				Messages.ErrorWarningPreferencePage_formDuplicatePart));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_EDITABLE_COMBOBOX_CUSTOM_VALUELIST,
				Messages.ErrorWarningPreferencePage_formEditableComboboxCustomValuelist));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_EXTENDS_CYCLE, Messages.ErrorWarningPreferencePage_formExtendsCycle));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FILE_NAME_INCONSISTENT,
				Messages.ErrorWarningPreferencePage_formFileNameInconsistent));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FORMAT_INCOMPATIBLE,
				Messages.ErrorWarningPreferencePage_formFormatIncompatible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FORMAT_INVALID,
				Messages.ErrorWarningPreferencePage_formFormatInvalid));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_INCOMPATIBLE_ELEMENT_TYPE,
				Messages.ErrorWarningPreferencePage_formIncompatibleElementType));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_LABEL_FOR_ELEMENT_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formLabelForElementNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_ELEMENT_DUPLICATE_TAB_SEQUENCE,
				Messages.ErrorWarningPreferencePage_formElementDuplicateTabSequence));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_ELEMENT_OUTSIDE_BOUNDS_OF_FORM,
				Messages.ErrorWarningPreferencePage_formElementOutsideBoundsOfForm));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_ELEMENT_OUTSIDE_BOUNDS_OF_PART,
				Messages.ErrorWarningPreferencePage_formElementOutsideBoundsOfPart));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_OBSOLETE_ELEMENT,
				Messages.ErrorWarningPreferencePage_formObsoleteElement));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FIELD_FALLBACK_RELATED_VALUELIST,
				Messages.ErrorWarningPreferencePage_formFieldFallbackRelatedValuelist));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FIELD_RELATED_VALUELIST,
				Messages.ErrorWarningPreferencePage_formFieldRelatedValuelist));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_FOUNDSET_INCORRECT_VALUE,
				Messages.ErrorWarningPreferencePage_formFoundsetIncorrectValue));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PORTAL_INVALID_RELATION_NAME,
				Messages.ErrorWarningPreferencePage_formPortalInvalidRelationName));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PORTAL_ELEMENT_MISMATCHED_RELATION,
				Messages.ErrorWarningPreferencePage_formPortalElementMismatchedRelation));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_METHOD_NOT_ACCESIBLE,
				Messages.ErrorWarningPreferencePage_formPropertyMethodNotAccessible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_MULTIPLE_METHODS_ON_SAME_ELEMENT,
				Messages.ErrorWarningPreferencePage_formPropertyMultipleMethodsOnSameElement));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_RELATED_TAB_DIFFERENT_TABLE,
				Messages.ErrorWarningPreferencePage_formRelatedTabDifferentTable));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_RELATED_TAB_UNSOLVED_RELATION,
				Messages.ErrorWarningPreferencePage_formRelatedTabUnsolvedRelation));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_RELATED_TAB_UNSOLVED_UUID,
				Messages.ErrorWarningPreferencePage_formRelatedTabUnsolvedUuid));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_ROW_BG_CALC_TARGET_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formRowBGCalcTargetNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_TABLE_NO_PK, Messages.ErrorWarningPreferencePage_formTableNoPK));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_TABLE_NOT_ACCESSIBLE,
				Messages.ErrorWarningPreferencePage_formTableNotAccessible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_TYPEAHEAD_UNSTORED_CALCULATION,
				Messages.ErrorWarningPreferencePage_formTypeAheadUnstoredCalculation));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_VARIABLE_TYPE_COL,
				Messages.ErrorWarningPreferencePage_formVariableTableCol));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_IN_FORM_TARGET_NOT_ACCESIBLE,
				Messages.ErrorWarningPreferencePage_formPropertyInFormTargetNotAccessible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.FORM_PROPERTY_IN_FORM_TARGET_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_formPropertyInFormTargetNotFound));
		}
		else if (ERROR_WARNING_STYLES_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_ELEMENT_CLASS_NO_STYLE,
				Messages.ErrorWarningPreferencePage_styleElementClassNoStyle));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_ELEMENT_CLASS_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_styleElementClassNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_FORM_CLASS_NO_STYLE,
				Messages.ErrorWarningPreferencePage_styleFormClassNoStyle));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_FORM_CLASS_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_styleFormClassNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.STYLE_NOT_FOUND, Messages.ErrorWarningPreferencePage_styleNotFound));
		}
		else if (ERROR_WARNING_SOLUTION_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_BAD_STRUCTURE,
				Messages.ErrorWarningPreferencePage_solutionBadStructure));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_DESERIALIZE_ERROR,
				Messages.ErrorWarningPreferencePage_solutionDeserializeError));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER,
				Messages.ErrorWarningPreferencePage_solutionElementNameInvalidIdentifier));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED,
				Messages.ErrorWarningPreferencePage_solutionPropertyFormCannotBeInstantiated));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_ACCESSIBLE,
				Messages.ErrorWarningPreferencePage_solutionPropertyTargetNotAccessible));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SOLUTION_PROPERTY_TARGET_NOT_FOUND,
				Messages.ErrorWarningPreferencePage_solutionPropertyTargetNotFound));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.SERVER_NOT_ACCESSIBLE_FIRST_OCCURENCE,
				Messages.ErrorWarningPreferencePage_serverNotAccessibleFirstOccurence));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.CONSTANTS_USED, Messages.ErrorWarningPreferencePage_constantsUsed));
		}

		Collections.sort(associatedProblemMarkers, descriptionComparator);

		return associatedProblemMarkers;
	}

	@Override
	protected void performDefaults()
	{
		changes.clear();
		try
		{
			settingsNode.clear();
		}
		catch (BackingStoreException e)
		{
			ServoyLog.logError(e);
		}
		for (Pair<Combo, Integer> p : defaults)
		{
			p.getLeft().select(p.getRight().intValue() >= 0 ? p.getRight().intValue() : 0);
		}
		super.performDefaults();
	}

	@Override
	public boolean performCancel()
	{
		changes.clear();
		return super.performCancel();
	}

	@Override
	public boolean performOk()
	{
		try
		{
			boolean doBuild = false;
			if (isProjectPreferencePage() && !projectSpecificSetting && hasProjectSpecificOptions(fProject))
			{
				try
				{
					settingsNode.clear();
				}
				catch (BackingStoreException e)
				{
					ServoyLog.logError(e);
				}
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
				ServoyModelManager.getServoyModelManager().getServoyModel().buildActiveProjectsInJob();
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

	private Link createLink(Composite composite, String text)
	{
		Link link = new Link(composite, SWT.NONE);
		link.setFont(composite.getFont());
		link.setText("<A>" + text + "</A>"); //$NON-NLS-1$//$NON-NLS-2$
		link.addSelectionListener(new SelectionListener()
		{
			public void widgetSelected(SelectionEvent e)
			{
				doLinkActivated((Link)e.widget);
			}

			public void widgetDefaultSelected(SelectionEvent e)
			{
				doLinkActivated((Link)e.widget);
			}
		});
		return link;
	}

	/*
	 * from boolean org.eclipse.dltk.internal.ui.preferences.OptionsConfigurationBlock.hasProjectSpecificOptions(IProject project)
	 */
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

	final void doLinkActivated(Link link)
	{
		Map data = new HashMap();
		data.put(DATA_NO_LINK, Boolean.TRUE);

		if (isProjectPreferencePage())
		{
			openWorkspacePreferences(data);
		}
		else
		{
			HashSet projectsWithSpecifics = new HashSet();
			try
			{
				IScriptProject[] projects = DLTKCore.create(ResourcesPlugin.getWorkspace().getRoot()).getScriptProjects();
				for (IScriptProject curr : projects)
				{
					if (hasProjectSpecificOptions(curr.getProject()))
					{
						projectsWithSpecifics.add(curr);
					}
				}
			}
			catch (ModelException e)
			{
				// ignore
			}
			ProjectSelectionDialog dialog = new ProjectSelectionDialog(getShell(), projectsWithSpecifics, null);
			if (dialog.open() == Window.OK)
			{
				IScriptProject res = (IScriptProject)dialog.getFirstResult();
				openProjectProperties(res.getProject(), data);
			}
		}
	}

	protected final void openWorkspacePreferences(Object data)
	{
		String id = getPreferencePageId();
		PreferencesUtil.createPreferenceDialogOn(getShell(), id, new String[] { id }, data).open();
	}

	protected final void openProjectProperties(IProject project, Object data)
	{
		String id = getPropertyPageId();
		if (id != null)
		{
			PreferencesUtil.createPropertyDialogOn(getShell(), project, id, new String[] { id }, data).open();
		}
	}

	protected String getPreferencePageId()
	{
		return "com.servoy.eclipse.ui.preferences.error.warning";
	}

	protected String getPropertyPageId()
	{
		return "com.servoy.eclipse.ui.propertyPage.error.warning";
	}
}