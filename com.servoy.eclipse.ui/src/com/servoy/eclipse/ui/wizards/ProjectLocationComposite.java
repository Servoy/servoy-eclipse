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

package com.servoy.eclipse.ui.wizards;

import java.io.File;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Activator;


/**
 * Enables the user to select a different location when creating a new project and when importing as project.
 * @author emera
 */
public class ProjectLocationComposite extends Composite
{
	private final Button useDefaultLocationButton;
	private final Text locationText;
	private final Button browseButton;
	private final IEclipsePreferences preferences = Activator.getDefault().getEclipsePreferences();

	private String location;
	private boolean useDefaultLocation;
	private final String prefix;

	public ProjectLocationComposite(Composite parent, int style, String settingsPrefix)
	{
		super(parent, style);
		this.prefix = settingsPrefix;
		useDefaultLocation = preferences.getBoolean(prefix + ".useDefaultLocation", true);
		location = preferences.get(prefix + ".location", "");

		setLayout(new FillLayout());
		Group group = new Group(this, SWT.NONE);
		GridLayout gridLayout = new GridLayout();
		gridLayout.numColumns = 1;
		group.setLayout(gridLayout);

		useDefaultLocationButton = new Button(group, SWT.CHECK);
		useDefaultLocationButton.setText("Use default location");
		useDefaultLocationButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				locationText.setEditable(!useDefaultLocationButton.getSelection());
				locationText.setEnabled(!useDefaultLocationButton.getSelection());
				browseButton.setEnabled(!useDefaultLocationButton.getSelection());
				useDefaultLocation = useDefaultLocationButton.getSelection();
				preferences.putBoolean(prefix + ".useDefaultLocation", useDefaultLocation);
			}
		});
		useDefaultLocationButton.setSelection(useDefaultLocation);
		useDefaultLocationButton.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));

		Composite fileBrowsePanel = new Composite(group, SWT.NONE);
		fileBrowsePanel.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		fileBrowsePanel.setLayout(new GridLayout(3, false));
		Label locationLabel = new Label(fileBrowsePanel, SWT.NONE);
		locationLabel.setText("Location: ");
		locationText = new Text(fileBrowsePanel, SWT.BORDER);
		locationText.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		locationText.setEditable(!useDefaultLocationButton.getSelection());
		locationText.setEnabled(!useDefaultLocationButton.getSelection());
		locationText.setText(location);
		locationText.addListener(SWT.FocusOut, new Listener()
		{

			@Override
			public void handleEvent(Event event)
			{
				File f = new File(locationText.getText());
				if (!useDefaultLocationButton.getSelection() && !f.isDirectory())
				{
					locationText.setText("");
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							MessageDialog.openError(getShell(), "Project Location Error", "Please select a folder for location.");
						}
					});
				}

			}
		});

		browseButton = new Button(fileBrowsePanel, SWT.PUSH);
		browseButton.setText("Browse...");
		browseButton.setEnabled(!useDefaultLocationButton.getSelection());
		browseButton.addListener(SWT.Selection, new Listener()
		{

			@Override
			public void handleEvent(Event event)
			{
				DirectoryDialog dlg = new DirectoryDialog(UIUtils.getActiveShell(), SWT.SAVE);
				dlg.setFilterPath("".equals(location) ? ResourcesPlugin.getWorkspace().getRoot().getLocation().toString() : location);
				String chosenFileName = dlg.open();
				if (chosenFileName != null)
				{
					locationText.setText(chosenFileName);
					location = chosenFileName;
					preferences.put(prefix + ".location", location);
				}
			}
		});
	}

	public String getProjectLocation()
	{
		return !useDefaultLocation && !"".equals(location) ? location : null;
	}

}