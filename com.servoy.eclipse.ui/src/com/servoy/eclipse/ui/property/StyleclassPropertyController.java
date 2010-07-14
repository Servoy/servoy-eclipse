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

import org.eclipse.jface.viewers.ILabelProvider;

import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * Property controller for selecting a style class in Properties view.
 * 
 * @author rgansevles
 *
 */
public class StyleclassPropertyController extends ComboboxPropertyController<String>
{
	private final IPersist persist;
	private final String styleLookupname;

	public StyleclassPropertyController(String id, String displayName, IPersist persist, String styleLookupname)
	{
		super(id, displayName, new StyleClassesComboboxModel((Form)persist.getAncestor(IRepository.FORMS), styleLookupname), Messages.LabelUnresolved,
			StyleValueEditor.INSTANCE);
		this.persist = persist;
		this.styleLookupname = styleLookupname;
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return PersistPropertySource.getFormInheritanceLabelProvider(persist, super.getLabelProvider(), getId());
	}

	@Override
	protected String getWarningMessage()
	{
		if (getModel().getRealValues().length == 1)
		{
			// only 1 value (DEFAULT)
			return "No style classes available for lookup '" + styleLookupname + "'";
		}
		return null;
	}

}
