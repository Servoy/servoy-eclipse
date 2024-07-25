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

import java.util.Comparator;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.ui.Messages;

/**
 * simple IPropertyController implementation.
 *
 * @author rgansevles
 *
 */
public class PropertyController<P, E> extends PropertyDescriptor implements IPropertyController<P, E>, ICellEditorFactory, IAdaptable,
	Comparable<PropertyController< ? , ? >>, IKeepsTooltip
{
	// comparator based on sequence, see applySequencePropertyComparator
	public static Comparator<PropertyController< ? , ? >> PROPERTY_SEQ_COMPARATOR = new Comparator<PropertyController< ? , ? >>()
	{
		public int compare(PropertyController< ? , ? > o1, PropertyController< ? , ? > o2)
		{
			return (o1.sequence < o2.sequence ? -1 : (o1.sequence == o2.sequence ? 0 : 1));
		}
	};

	private IPropertyConverter<P, E> propertyConverter;
	private final ICellEditorFactory cellEditorFactory;
	private boolean readOnly = false;
	private boolean supportsReadonly = false;
	private Comparator<PropertyController< ? , ? >> comparator;
	private String tooltipText;
	private IProvidesTooltip tooltipProvider;

	private int sequence;

	public PropertyController(Object id, String displayName)
	{
		super(id, displayName);
		this.propertyConverter = null;
		this.cellEditorFactory = null;
	}

	public PropertyController(Object id, String displayName, IPropertyConverter<P, E> propertyConverter, ILabelProvider labelProvider,
		ICellEditorFactory cellEditorFactory)
	{
		super(id, displayName);
		setLabelProvider(labelProvider);
		this.propertyConverter = propertyConverter;
		this.cellEditorFactory = cellEditorFactory;
	}

	public void setTooltipText(String tooltipText)
	{
		this.tooltipText = tooltipText;
	}

	@Override
	public void setTooltipProvider(IProvidesTooltip tooltipProvider)
	{
		this.tooltipProvider = tooltipProvider;
	}

	public String getTooltipText()
	{
		String tooltip = tooltipText;
		if (tooltipProvider != null) tooltip = tooltipProvider.getTooltipText();
		return tooltip;
	}

	public boolean supportsReadonly()
	{
		return supportsReadonly;
	}

	public void setSupportsReadonly(boolean supportsReadonly)
	{
		this.supportsReadonly = supportsReadonly;
	}

	public boolean isReadOnly()
	{
		return readOnly;
	}

	public void setReadonly(boolean readOnly)
	{
		this.readOnly = readOnly;
	}

	@Override
	public String getDescription()
	{
		if (readOnly)
		{
			return Messages.LabelReadonly;
		}
		return super.getDescription();
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		if (cellEditorFactory == null) return null;
		return cellEditorFactory.createPropertyEditor(parent);
	}

	public IPropertyConverter<P, E> getConverter()
	{
		if (propertyConverter == null)
		{
			propertyConverter = createConverter();
		}
		return propertyConverter;
	}

	protected IPropertyConverter<P, E> createConverter()
	{
		return null;
	}

	public void setComparator(Comparator<PropertyController< ? , ? >> comparator)
	{
		this.comparator = comparator;
	}

	public int compareTo(PropertyController< ? , ? > o)
	{
		return comparator.compare(this, o);
	}

	public Object getAdapter(Class adapter)
	{
		if (Comparable.class == adapter && comparator != null)
		{
			return this;
		}
		return null;
	}

	/**
	 * Set a comparator on the property descriptor that keeps the order as defined.
	 *
	 * @param descs
	 * @return
	 */
	public static IPropertyDescriptor[] applySequencePropertyComparator(IPropertyDescriptor[] descs)
	{
		if (descs == null || descs.length < 2)
		{
			// nothing to order
			return descs;
		}
		IPropertyDescriptor[] newdescs = new IPropertyDescriptor[descs.length];
		for (int i = 0; i < descs.length; i++)
		{
			if (descs[i] instanceof PropertyController)
			{
				newdescs[i] = descs[i];
			}
			else
			{
				newdescs[i] = new DelegatePropertyController(descs[i]);
			}
			((PropertyController< ? , ? >)newdescs[i]).setSequence(i);
			((PropertyController< ? , ? >)newdescs[i]).setComparator(PROPERTY_SEQ_COMPARATOR);
		}
		return newdescs;
	}

	protected void setSequence(int i)
	{
		sequence = i;
	}


	@Override
	public String toString()
	{
		return getClass().getSimpleName() + " [" + getDisplayName() + "]";
	}

}
