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
package com.servoy.eclipse.ui.dialogs;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.NameComparator;

/**
 * Content provider class for forms.
 * 
 * @author rob
 * 
 */

public class FormContentProvider extends FlatTreeContentProvider
{
	private final FlattenedSolution flattenedSolution;
	private final Form form;

	public FormContentProvider(FlattenedSolution flattenedSolution, Form form)
	{
		this.flattenedSolution = flattenedSolution;
		this.form = form;
	}

	@Override
	public Object[] getElements(Object inputElement)
	{
		if (inputElement instanceof FormListOptions)
		{
			FormListOptions options = (FormListOptions)inputElement;

			List<Integer> formIds = new ArrayList<Integer>();
			if (options.includeNone) formIds.add(new Integer(Form.NAVIGATOR_NONE));
			if (options.includeDefault) formIds.add(new Integer(Form.NAVIGATOR_DEFAULT));
			if (options.includeIgnore) formIds.add(new Integer(Form.NAVIGATOR_IGNORE));

			switch (options.type)
			{
				case FORMS :
					Iterator<Form> forms = flattenedSolution.getForms(true);
					while (forms.hasNext())
					{
						Form obj = forms.next();
						if ((options.showInMenu == null || options.showInMenu.booleanValue() == obj.getShowInMenu()) && form != obj)
						{
							formIds.add(new Integer(obj.getID()));
						}
					}
					break;

				case HIERARCHY :
					forms = flattenedSolution.getForms(false);
					Map<Form, Integer> possibleParentForms = new TreeMap<Form, Integer>(NameComparator.INSTANCE);
					while (forms.hasNext())
					{
						Form possibleParentForm = forms.next();
						if (form.getDataSource() == null || possibleParentForm.getDataSource() == null ||
							form.getDataSource().equals(possibleParentForm.getDataSource()))
						{
							// do not add the form if it is already a sub-form, to prevent cycles
							if (!flattenedSolution.getFormHierarchy(possibleParentForm).contains(form))
							{
								possibleParentForms.put(possibleParentForm, new Integer(possibleParentForm.getID()));
							}
						}
					}

					formIds.addAll(possibleParentForms.values());
					break;
			}
			return formIds.toArray();
		}

		return super.getElements(inputElement);
	}

	public static class FormListOptions
	{
		public static enum FormListType
		{
			FORMS, HIERARCHY;
		}

		public final Boolean showInMenu;
		public final boolean includeNone;
		public final boolean includeDefault;
		public final boolean includeIgnore;
		public final FormListType type;

		public FormListOptions(FormListType type, Boolean showInMenu, boolean includeNone, boolean includeDefault, boolean includeIgnore)
		{
			this.type = type;
			this.showInMenu = showInMenu;
			this.includeNone = includeNone;
			this.includeDefault = includeDefault;
			this.includeIgnore = includeIgnore;
		}
	}

}
