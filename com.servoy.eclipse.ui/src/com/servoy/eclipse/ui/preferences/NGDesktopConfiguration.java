/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.core.util.SemVerComparator;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.ClientVersion;

/**
 * @author vidmarian
 *
 */
public class NGDesktopConfiguration extends PreferencePage implements IWorkbenchPreferencePage
{

	private final String versionsUrl = "https://download.servoy.com/ngdesktop/versions.txt";
	private List<String> remoteVersions = new ArrayList<String>();

	private Combo srcVersionCombo;


	@Override
	public void init(IWorkbench workbench)
	{
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.preference.PreferencePage#createContents(org.eclipse.swt.widgets.Composite)
	 */
	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);
		composite.setLayout(new GridLayout(2, true));
		composite.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		Label launchVersionLabel = new Label(composite, SWT.NONE);
		launchVersionLabel.setText("Default NG Desktop version:");

		srcVersionCombo = new Combo(composite, SWT.READ_ONLY);

		initializeFields();

		return composite;
	}

	protected void initializeFields()
	{
		NgDesktopPreferences prefs = new NgDesktopPreferences();

		remoteVersions = getAvailableVersions();
		if (remoteVersions.isEmpty())
		{
			remoteVersions.add(prefs.getNgDesktopVersionKey());
		}

		srcVersionCombo.removeAll();

		remoteVersions.forEach((s) -> {
			srcVersionCombo.add(s);
		});

		srcVersionCombo.select(getVersionIndex(prefs.getNgDesktopVersionKey()));
	}

	private List<String> getAvailableVersions()
	{
		try
		{
			final URL url = new URL(versionsUrl);
			final StringBuffer sb = new StringBuffer();
			try (BufferedReader br = new BufferedReader(new InputStreamReader(url.openConnection().getInputStream())))
			{
				String line = null;
				while ((line = br.readLine()) != null)
					sb.append(line);
				final JSONObject jsonObj = new JSONObject(sb.toString());
				final JSONArray value = (JSONArray)jsonObj.get("versions");
				if (value != null && value.length() > 0)
				{
					final List<String> result = new ArrayList<String>();
					value.forEach((item) -> {
						final String servoyVersion = ((JSONObject)item).getString("servoyVersion");
						final String devVersion = ClientVersion.getMajorVersion() + "." + ClientVersion.getMiddleVersion();
						if (SemVerComparator.compare(devVersion, servoyVersion) < 0)
							return;
						result.add(((JSONObject)item).getString("ngDesktopVersion"));
					});
					if (result.size() > 0)
					{
						result.sort(Comparator.naturalOrder());
						return result;
					}
				}
			}
		}
		catch (final IOException e)
		{
			ServoyLog.logError(e);
		}

		return remoteVersions;
	}

	private int getVersionIndex(String value)
	{
		//we need an index from a sorted list
		final int result = remoteVersions.indexOf(value);
		return result < 0 ? 0 : result;
	}

	@Override
	public boolean performOk()
	{
		NgDesktopPreferences prefs = new NgDesktopPreferences();
		prefs.setNgDesktopVersion(srcVersionCombo.getText());
		prefs.save();
		return true;
	}

	@Override
	protected void performDefaults()
	{
		NgDesktopPreferences prefs = new NgDesktopPreferences();
		prefs.setNgDesktopVersion(NgDesktopPreferences.NGDESKTOP_VERSION_DEFAULT);
		prefs.save();
		initializeFields();
		super.performDefaults();
	}

}
