/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.property;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.SolutionContentProvider;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;

/**
 * Property controller for pseudo loginSolutionName property (is stored in modulesNames).
 * 
 * @author rgansevles
 *
 */

public class LoginSolutionPropertyController extends PropertySetterController<String, String, PersistPropertySource>
{
	/**
	 * @param id
	 * @param displayName
	 * @param propertyConverter
	 * @param labelProvider
	 * @param cellEditorFactory
	 */
	public LoginSolutionPropertyController(Object id, String displayName)
	{
		super(id, displayName, PersistPropertySource.NULL_STRING_CONVERTER, PersistPropertySource.NullDefaultLabelProvider.LABEL_NONE, null);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		return new ListSelectCellEditor(parent, "Select login solution", SolutionContentProvider.SOLUTION_CONTENT_PROVIDER, getLabelProvider(), null,
			isReadOnly(), new SolutionContentProvider.SolutionListOptions(SolutionMetaData.LOGIN_SOLUTION, true), SWT.NONE, null, "loginSolutionDialog");
	}

	/**
	 * Remove all modules that are login-solutions and add the new value as module
	 */
	public void setProperty(PersistPropertySource propertySource, String value)
	{
		IPersist persist = propertySource.getPersist();
		if (persist instanceof Solution)
		{
			try
			{
				((Solution)persist).setLoginSolutionName(value);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public String getProperty(PersistPropertySource propertySource)
	{
		IPersist persist = propertySource.getPersist();
		if (persist instanceof Solution)
		{
			try
			{
				return ((Solution)persist).getLoginSolutionName();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}
}
