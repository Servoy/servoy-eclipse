package com.servoy.eclipse.ui.preferences;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.DialogField;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.IDialogFieldListener;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.LayoutUtil;
import org.eclipse.dltk.internal.ui.wizards.dialogfields.SelectionButtonDialogField;
import org.eclipse.dltk.ui.dialogs.ProjectSelectionDialog;
import org.eclipse.dltk.ui.preferences.PreferencesMessages;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.dialogs.PreferencesUtil;

/**
 * This class provides an extra link to workspace preference page to configure project specific settings.
 * <br/>
 * An "Enable project specific settings" checkbox is provided if the user goes to project preferences instead of workspace preferences (extender must control the enabled state of the checkbox )
 * 
 * <br/>
 * Extend this class and provide your own controls . (for ex have a looka at {@link JSDocScriptTemplatesPreferencePage} which implements 2 text properties)
 * 
 * 
 * @author obuligan
 *
 */
abstract class WorkspaceOrProjectPreferencePage extends PreferencePage
{
	private SelectionButtonDialogField fUseProjectSettings;
	private Link fChangeWorkspaceSettings;
	private boolean projectSpecificSetting = false;
	private IProject fProject; // project or null
	public static final String DATA_NO_LINK = "PropertyAndPreferencePage.nolink";
	private final Map fData; // page data

	protected abstract String getPreferencePageId();

	protected abstract String getPropertyPageId();

	protected abstract boolean hasProjectSpecificOptions(IProject project);

	/**
	 * enable controls
	 * @param bool
	 */
	protected abstract void enablePreferencePageContent(boolean useProjectSpecificSettings);


	public WorkspaceOrProjectPreferencePage()
	{
		fProject = null;
		fData = null;
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
					boolean isSelected = ((SelectionButtonDialogField)field).isSelected();
					projectSpecificSetting = isSelected;
					//fUseProjectSettings.setSelection(isSelected); -- useles because it is only fired on change , on initialisation the extender must set the enabled state of the checkbox 
					enablePreferencePageContent(isSelected);
					updateLinkVisibility();
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


	public SelectionButtonDialogField getfUseProjectSettings()
	{
		return fUseProjectSettings;
	}


	private Link createLink(Composite composite, String text)
	{
		Link link = new Link(composite, SWT.NONE);
		link.setFont(composite.getFont());
		link.setText("<A>" + text + "</A>");
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

	protected boolean isProjectSpecificSettingsChecked()
	{
		return projectSpecificSetting;
	}

	protected boolean isProjectPreferencePage()
	{
		return fProject != null;
	}

	protected IProject getProject()
	{
		return fProject;
	}

	protected void setProject(IProject project)
	{
		fProject = project;
	}


	protected boolean supportsProjectSpecificOptions()
	{
		return getPropertyPageId() != null;
	}


	protected boolean offerLink()
	{
		return fData == null || !Boolean.TRUE.equals(fData.get(DATA_NO_LINK));
	}

	protected void updateLinkVisibility()
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


}