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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.ast.parser.SourceParserManager;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IDLTKContributedExtension;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.dialogs.StatusUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.dltk.javascript.core.JavaScriptNature;
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

	private final IEclipsePreferences settingsNode;
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
	private final String ERROR_WARNING_RELATIONS_PROBLEMS = "Relation problems"; //$NON-NLS-1$

	public ServoyErrorWarningPreferencePage()
	{
		settingsNode = new InstanceScope().getNode(ServoyBuilder.ERROR_WARNING_PREFERENCES_NODE);

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

	protected void enableProjectSpecificSettings(boolean useProjectSpecificSettings)
	{
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

		//TODO: implement this for project level
		if (isProjectPreferencePage())
		{
			final IDLTKContributedExtension[] extensions = SourceParserManager.getInstance().getContributions(JavaScriptNature.NATURE_ID);
			if (extensions.length > 1)
			{
				SWTFactory.createLabel(composite, "Parser", 1);
				final String[] ids = new String[extensions.length];
				final String[] names = new String[extensions.length];
				for (int i = 0; i < extensions.length; ++i)
				{
					ids[i] = extensions[i].getId();
					names[i] = extensions[i].getName();
				}
//				bindControl(SWTFactory.createCombo(composite, SWT.READ_ONLY, 1, 0, names), new PreferenceKey(DLTKCore.PLUGIN_ID,
//					DLTKCore.PROJECT_SOURCE_PARSER_ID), ids);
			}
		}

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
				addPreferenceItem(inner, problemItem.problem.getLeft(), problemItem.description, names, ids, problemItem.problem.getRight().name());
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

	protected boolean isProjectPreferencePage()
	{
		return fProject != null;
	}

	protected IProject getProject()
	{
		return fProject;
	}

	private List<String> getProblemSections()
	{
		//create problem section for all marker categories...
		//final IConfigurationElement[] elements = Platform.getExtensionRegistry().getConfigurationElementsFor("org.eclipse.core.resources.markers");
		return problemSections;
	}


	private void fillProblemCategories()
	{
		problemSections.add(ERROR_WARNING_POTENTIAL_DRAWBACKS);
		problemSections.add(ERROR_WARNING_RELATIONS_PROBLEMS);
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

		if (ERROR_WARNING_POTENTIAL_DRAWBACKS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW,
				Messages.ErrorWarningPreferencePage_tooManyColumns));
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.LEVEL_PERFORMANCE_TABS_PORTALS,
				Messages.ErrorWarningPreferencePage_tooManyTabsPortals));
		}
		else if (ERROR_WARNING_RELATIONS_PROBLEMS.equals(problemSection))
		{
			associatedProblemMarkers.add(new ErrorWarningPreferenceItem(ServoyBuilder.RELATIONS_PRIMARY_SERVER_WITH_PROBLEMS,
				Messages.ErrorWarningPreferencePage_relationPrimaryServerWithProblems));
		}

		return associatedProblemMarkers;
	}

	@Override
	protected void performDefaults()
	{
		changes.clear();
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
			if (changes.size() > 0)
			{
				for (Entry<String, String> e : changes.entrySet())
				{
					settingsNode.put(e.getKey(), e.getValue());
				}
				settingsNode.flush();
				changes.clear();
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

	public boolean hasProjectSpecificOptions(IProject project)
	{
//		if (project != null)
//		{
//			IScopeContext projectContext = new ProjectScope(project);
//			PreferenceKey[] allKeys = getPreferenceKeys();
//			for (int i = 0; i < allKeys.length; i++)
//			{
//				if (allKeys[i].getStoredValue(projectContext, fManager) != null)
//				{
//					return true;
//				}
//			}
//		}
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