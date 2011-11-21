/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

package com.servoy.eclipse.ui.quickfix;

import org.eclipse.core.resources.IMarker;
import org.eclipse.ui.IMarkerResolution;
import org.eclipse.ui.IMarkerResolutionGenerator;

import com.servoy.eclipse.ui.actions.ShowI18NDialogActionDelegate;

/**
 * Quick fix that starts the I18N externalize wizard
 * @author gboros
 */
public class StartI18NExternalizeWizard implements IMarkerResolutionGenerator
{
	/*
	 * @see org.eclipse.ui.IMarkerResolutionGenerator#getResolutions(org.eclipse.core.resources.IMarker)
	 */
	public IMarkerResolution[] getResolutions(IMarker marker)
	{
		return new IMarkerResolution[] { new IMarkerResolution()
		{

			public String getLabel()
			{
				return "Run I18N externalize wizard";
			}

			public void run(IMarker marker)
			{
				ShowI18NDialogActionDelegate delegate = new ShowI18NDialogActionDelegate();
				delegate.run(ShowI18NDialogActionDelegate.ACTION_EXTERNALIZE);
			}
		} };
	}
}
