/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ui.editors;

import org.eclipse.dltk.javascript.ui.IJsExtendsScope;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Form;

/**
 * @author vidmarian
 *
 */
public class JSExtendsScope implements IJsExtendsScope
{
	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.dltk.javascript.scriptdoc.IJsExtendsScope#getInteritedJsDoc(org.eclipse.dltk.core.IMethod)
	 */
	@Override
	public String getExtendsScope(String scopeName)
	{

		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		Form myForm = servoyProject.getEditingFlattenedSolution().getForm(scopeName);
		Form extendsForm = myForm.getExtendsForm();
		if (extendsForm != null)
		{
			return extendsForm.getName();
		}
		return null;
	}

}
