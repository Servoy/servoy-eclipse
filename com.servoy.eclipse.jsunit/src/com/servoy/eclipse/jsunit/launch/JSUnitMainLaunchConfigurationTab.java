/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.jsunit.launch;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.jsunit.runner.TestTarget;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * @author obuligan
 *
 */
public class JSUnitMainLaunchConfigurationTab extends AbstractLaunchConfigurationTab
{
	// CONFIG_INVALID is only used in this view , it should not be possible to create an invalid launch configuration from solex view.
	private static final String CONFIG_INVALID = "servoy.jsunit.launchconfig.invalid";
	Text textArea = null;

	@Override
	public void createControl(Composite parent)
	{
		Composite comp = new Composite(parent, SWT.NONE);
		setControl(comp);

		GridLayout topLayout = new GridLayout();
		topLayout.numColumns = 1;
		comp.setLayout(topLayout);

		textArea = new Text(comp, SWT.MULTI | SWT.BORDER | SWT.WRAP | SWT.READ_ONLY);
		textArea.setBounds(30, 30, 300, 50);
	}


	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		//you cannot create new launch configurations manually
		configuration.setAttribute(CONFIG_INVALID, true);
	}


	@Override
	public void initializeFrom(ILaunchConfiguration configuration)
	{
		try
		{
			if (!isValid(configuration))
			{
				textArea.setText("New JSUnit launch configurations can be created from Solution explorer view.\n This one will be ignored.");
			}
			else
			{
				TestTarget target = TestTarget.fromString(configuration.getAttribute(JSUnitLaunchConfigurationDelegate.LAUNCH_CONFIG_INSTANCE, ""));
				textArea.setText(JSUnitLaunchConfigurationDelegate.generateLaunchConfigName(target));
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}


	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		// TODO Auto-generated method stub

	}


	@Override
	public String getName()
	{
		return "JSUnit";
	}

	@Override
	public boolean isValid(ILaunchConfiguration launchConfig)
	{
		try
		{
			if (launchConfig.getAttribute(CONFIG_INVALID, false))
			{
				return false;
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

		return true;
	}

}
