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
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.SortedList;
import com.servoy.j2db.util.StringComparator;

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
		List<String> displayValues = new ArrayList<String>();
		displayValues.add(Messages.LabelDefault);
		displayValues.add(Messages.LabelSeparate);
		displayValues.add(Messages.LabelEmpty);
		displayValues.addAll(Arrays.asList(getCompatibleGlobalRelations()));
		displayValues.addAll(getCompatibleNamedFoundsets());
		return displayValues.toArray(new String[0]);
	}

	public String[] getRealValues()
	{
		List<String> realValues = new ArrayList<String>();
		realValues.add(null);
		realValues.add(Form.NAMED_FOUNDSET_SEPARATE);
		realValues.add(Form.NAMED_FOUNDSET_EMPTY);
		for (String relation : getCompatibleGlobalRelations())
		{
			realValues.add(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX + relation);
		}
		for (String namedFoundset : getCompatibleNamedFoundsets())
		{
			realValues.add(Form.NAMED_FOUNDSET_SEPARATE_PREFIX + namedFoundset);
		}
		return realValues.toArray(new String[0]);
	}

	@Override
	public int getDefaultValueIndex()
	{
		return 0;
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
			FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(form);
			relations = fs.getRelations(fs.getTable(form.getDataSource()), false, true, true, false, true);
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

	protected List<String> getCompatibleNamedFoundsets()
	{
		SortedList<String> namedFoundsets = new SortedList<String>(StringComparator.INSTANCE);
		FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(form);
		Iterator<Form> it = fs.getForms(fs.getTable(form.getDataSource()), false);
		while (it.hasNext())
		{
			Form form = it.next();
			if (form.getNamedFoundSet() != null && form.getNamedFoundSet().startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
			{
				String name = form.getNamedFoundSet().substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
				if (!namedFoundsets.contains(name))
				{
					namedFoundsets.add(name);
				}
			}
		}
		return namedFoundsets;
	}
}
