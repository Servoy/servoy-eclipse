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
package com.servoy.eclipse.ui.editors.table;

import static java.util.Arrays.stream;

import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;

import com.servoy.eclipse.ui.util.FixedComboBoxCellEditor;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.query.QueryAggregate;


public class AggregationTypeEditingSupport extends EditingSupport
{
	private final CellEditor editor;
	private final ChangeSupportObservable observable;

	private enum AggregateTypes
	{
		count(QueryAggregate.COUNT),
		countDistinct(QueryAggregate.COUNT, QueryAggregate.DISTINCT, "count(distinct)"),
		maximun(QueryAggregate.MAX),
		minimum(QueryAggregate.MIN),
		average(QueryAggregate.AVG),
		sum(QueryAggregate.SUM);

		final int type;
		final int aggregateQuantifier;
		final String display;

		AggregateTypes(int type)
		{
			this.type = type;
			this.aggregateQuantifier = QueryAggregate.ALL;
			this.display = name();
		}

		AggregateTypes(int type, int aggregateQuantifier, String display)
		{
			this.type = type;
			this.aggregateQuantifier = aggregateQuantifier;
			this.display = display;
		}
	}

	public AggregationTypeEditingSupport(TreeViewer tv)
	{
		super(tv);
		String[] types = stream(AggregateTypes.values()).map(at -> at.display).toArray(String[]::new);
		editor = new FixedComboBoxCellEditor(tv.getTree(), types, SWT.READ_ONLY);
		observable = new ChangeSupportObservable(new SimpleChangeSupport());
	}

	public void addChangeListener(IChangeListener listener)
	{
		observable.addChangeListener(listener);
	}

	public void removeChangeListener(IChangeListener listener)
	{
		observable.removeChangeListener(listener);
	}

	@Override
	protected void setValue(Object element, Object value)
	{
		if (element instanceof AggregateVariable)
		{
			AggregateVariable aggregateVariable = (AggregateVariable)element;
			AggregateTypes agType = AggregateTypes.values()[Integer.parseInt(value.toString())];
			if (agType.type != aggregateVariable.getType() || agType.aggregateQuantifier != aggregateVariable.getAggregateQuantifier())
			{
				aggregateVariable.setType(agType.type);
				aggregateVariable.setAggregateQuantifier(agType.aggregateQuantifier);
				getViewer().update(element, null);
				observable.fireChangeEvent();
			}
		}
	}

	@Override
	protected Object getValue(Object element)
	{
		if (element instanceof AggregateVariable)
		{
			AggregateTypes agType = getAggregateType((AggregateVariable)element);
			if (agType != null)
			{
				return Integer.valueOf(agType.ordinal());
			}
		}
		return Integer.valueOf(0);
	}

	@Override
	protected CellEditor getCellEditor(Object element)
	{
		return editor;
	}

	@Override
	protected boolean canEdit(Object element)
	{
		return element instanceof AggregateVariable;
	}

	public static String getDisplay(AggregateVariable aggregateVariable)
	{
		AggregateTypes agType = getAggregateType(aggregateVariable);
		if (agType != null)
		{
			return agType.display;
		}
		return null;
	}

	private static AggregateTypes getAggregateType(AggregateVariable aggregateVariable)
	{
		for (int i = 0; i < AggregateTypes.values().length; i++)
		{
			AggregateTypes agType = AggregateTypes.values()[i];
			if (agType.type == aggregateVariable.getType() && agType.aggregateQuantifier == aggregateVariable.getAggregateQuantifier())
			{
				return agType;
			}
		}
		return null;
	}

}
