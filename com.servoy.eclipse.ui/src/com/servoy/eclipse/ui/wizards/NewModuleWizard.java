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

package com.servoy.eclipse.ui.wizards;

import org.eclipse.swt.widgets.Composite;

import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * The new module wizard is similar to the new solution wizard: it
 * creates a module type solution by default.
 * 
 * @author acostache
 *
 */
@SuppressWarnings("nls")
public class NewModuleWizard extends NewSolutionWizard
{
	public static final String ID = "com.servoy.eclipse.ui.NewModuleWizard"; //$NON-NLS-1$

	/**
	 * Creates a new wizard.
	 */
	public NewModuleWizard()
	{
		super();
		setWindowTitle("New module");
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		super.createPageControls(pageContainer);
		page1.setSolutionTypes(new int[] { SolutionMetaData.MODULE, SolutionMetaData.PRE_IMPORT_HOOK, SolutionMetaData.POST_IMPORT_HOOK }, 0, false);
	}

}
