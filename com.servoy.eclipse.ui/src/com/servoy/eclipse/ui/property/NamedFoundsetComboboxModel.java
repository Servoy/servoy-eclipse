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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;

/**
 * IComboboxPropertyModel model for namedFoundset.
 * 
 * @author acostescu
 * 
 */
public class NamedFoundsetComboboxModel implements IComboboxPropertyModel<String>
{
	private final Form form;

	public NamedFoundsetComboboxModel(Form form)
	{
		this.form = form;
	}

	public String[] getDisplayValues()
	{
		String[] globalRelations = getCompatibleGlobalRelations();
		String[] displayValues = new String[globalRelations.length + 3]; // 3 constant values
		displayValues[0] = Messages.LabelDefault;
		displayValues[1] = Messages.LabelSeparate;
		displayValues[2] = Messages.LabelEmpty;
		for (int i = 3; i < displayValues.length; i++)
		{
			displayValues[i] = globalRelations[i - 3];
		}
		return displayValues;
	}

	public String[] getRealValues()
	{
		String[] globalRelations = getCompatibleGlobalRelations();
		String[] realValues = new String[globalRelations.length + 3]; // 3 constant values
		realValues[0] = null;
		realValues[1] = Form.NAMED_FOUNDSET_SEPARATE;
		realValues[2] = Form.NAMED_FOUNDSET_EMPTY;
		for (int i = 3; i < realValues.length; i++)
		{
			realValues[i] = Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX + globalRelations[i - 3];
		}
		return realValues;
	}

	/**
	 * Get the compatible global relations that can be used with this form and merge them with other constant values.
	 */
	protected String[] getCompatibleGlobalRelations()
	{
		List<String> gr = new ArrayList<String>();
		Iterator<Relation> relations;
		try
		{
			relations = ModelUtils.getEditingFlattenedSolution(form).getRelations(form.getTable(), false, true, true);
			while (relations.hasNext())
			{
				gr.add(relations.next().getName());
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
		return gr.toArray(new String[gr.size()]);
	}
}
