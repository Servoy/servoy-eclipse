/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;

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
 * @author lvostinar
 *
 */
public class NamedFoundSetPropertyController extends EditableComboboxPropertyController
{
	private final Form form;

	public NamedFoundSetPropertyController(String name, String displayName, String[] displayValues, Form form)
	{
		super(name, displayName, displayValues, new NamedFoundsetRelationValueEditor(form));
		this.form = form;
	}

	@Override
	protected IPropertyConverter<String, String> createConverter()
	{
		return new IPropertyConverter<String, String>()
		{

			@Override
			public String convertValue(Object id, String value)
			{
				if (Messages.LabelSeparate.equals(value))
				{
					return Form.NAMED_FOUNDSET_SEPARATE;
				}
				if (Messages.LabelEmpty.equals(value))
				{
					return Form.NAMED_FOUNDSET_EMPTY;
				}
				if (value == null || "".equals(value) || Messages.LabelDefault.equals(value))
				{
					return null;
				}
				try
				{
					FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(form);
					Iterator<Relation> relations = fs.getRelations(fs.getTable(form.getDataSource()), false, true, true, false, true);
					while (relations.hasNext())
					{
						if (relations.next().getName().equals(value))
						{
							return Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX + value;
						}
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
				return Form.NAMED_FOUNDSET_SEPARATE_PREFIX + value;
			}

			@Override
			public String convertProperty(Object id, String value)
			{
				if (value == null) return null;
				if (Form.NAMED_FOUNDSET_SEPARATE.equals(value))
				{
					return Messages.LabelSeparate;
				}
				if (Form.NAMED_FOUNDSET_EMPTY.equals(value))
				{
					return Messages.LabelEmpty;
				}
				if (value.startsWith(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX))
				{
					return value.substring(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH);
				}
				if (value.startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
				{
					return value.substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
				}
				return value;
			}
		};
	}

	@Override
	public ILabelProvider getLabelProvider()
	{
		return new LabelProvider()
		{
			@Override
			public String getText(Object element)
			{
				if (element == null)
				{
					return Messages.LabelDefault;
				}
				if (Form.NAMED_FOUNDSET_SEPARATE.equals(element))
				{
					return Messages.LabelSeparate;
				}
				if (Form.NAMED_FOUNDSET_EMPTY.equals(element))
				{
					return Messages.LabelEmpty;
				}
				if (element.toString().startsWith(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX))
				{
					return element.toString().substring(Form.NAMED_FOUNDSET_GLOBAL_RELATION_PREFIX_LENGTH);
				}
				if (element.toString().startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
				{
					return element.toString().substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
				}
				return super.getText(element);
			}
		};
	}

	public static String[] getDisplayValues(Form form)
	{
		List<String> displayValues = new ArrayList<String>();
		displayValues.add(Messages.LabelDefault);
		displayValues.add(Messages.LabelSeparate);
		displayValues.add(Messages.LabelEmpty);
		displayValues.addAll(Arrays.asList(getCompatibleGlobalRelations(form)));
		displayValues.addAll(getCompatibleNamedFoundsets(form));
		return displayValues.toArray(new String[0]);
	}

	private static String[] getCompatibleGlobalRelations(Form form)
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

	private static List<String> getCompatibleNamedFoundsets(Form form)
	{
		SortedList<String> namedFoundsets = new SortedList<String>(StringComparator.INSTANCE);
		FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(form);
		Iterator<Form> it = fs.getForms(fs.getTable(form.getDataSource()), false);
		while (it.hasNext())
		{
			Form f = it.next();
			if (f.getNamedFoundSet() != null && f.getNamedFoundSet().startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
			{
				String name = f.getNamedFoundSet().substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH);
				if (!namedFoundsets.contains(name))
				{
					namedFoundsets.add(name);
				}
			}
		}
		return namedFoundsets;
	}
}
