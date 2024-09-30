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
import java.util.List;

import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.labelproviders.MenuLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Menu;

/**
 * Property controller for selecting value list in Properties view.
 *
 * @author rgansevles
 *
 * @param <P> property type
 */
public class JSMenuPropertyController extends PropertyController<Object, Integer>
{
	private final PersistContext persistContext;

	public JSMenuPropertyController(Object id, String displayName, PersistContext persistContext)
	{
		super(id, displayName);
		this.persistContext = persistContext;
		setLabelProvider(new SolutionContextDelegateLabelProvider(
			new MenuLabelProvider(ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext())),
			persistContext.getContext()));
		setSupportsReadonly(true);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		final FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
		List<Integer> actualList = new ArrayList<Integer>();
		actualList.add(Integer.valueOf(MenuLabelProvider.MENU_NONE));
		flattenedEditingSolution.getMenus(true).forEachRemaining(menu -> actualList.add(Integer.valueOf(menu.getID())));
		return new ListSelectCellEditor(parent, "Select Servoy Menu", getLabelProvider(), new JSMenuValueEditor(flattenedEditingSolution), isReadOnly(),
			actualList.toArray(),
			SWT.NONE, null, "menuDialog");
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see com.servoy.eclipse.ui.property.PropertyController#createConverter()
	 */
	@Override
	protected IPropertyConverter<Object, Integer> createConverter()
	{
		final FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
		return new IPropertyConverter<Object, Integer>()
		{
			public Integer convertProperty(Object id, Object value)
			{
				Menu menu = value != null ? flattenedEditingSolution.getMenu(value.toString()) : null;
				return Integer.valueOf(menu == null ? MenuLabelProvider.MENU_NONE : menu.getID());
			}

			public Object convertValue(Object id, Integer value)
			{
				int menuId = value.intValue();
				if (menuId == MenuLabelProvider.MENU_NONE) return null;
				Menu menu = flattenedEditingSolution.getMenu(menuId);
				return menu == null ? null : menu.getName();
			}
		};
	}

	public static class JSMenuValueEditor implements IValueEditor<Integer>
	{
		private final FlattenedSolution flattenedEditingSolution;

		public JSMenuValueEditor(FlattenedSolution flattenedEditingSolution)
		{
			this.flattenedEditingSolution = flattenedEditingSolution;
		}

		public void openEditor(Integer value)
		{
			EditorUtil.openMenuEditor(flattenedEditingSolution.getMenu(value.intValue()), true);
		}

		public boolean canEdit(Integer value)
		{
			return value != null && value.intValue() != MenuLabelProvider.MENU_NONE &&
				flattenedEditingSolution.getMenu(value.intValue()) != null;
		}
	}
}
