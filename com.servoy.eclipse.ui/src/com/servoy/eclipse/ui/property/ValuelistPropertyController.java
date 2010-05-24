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
import org.eclipse.swt.widgets.Control;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.ValuelistContentProvider;
import com.servoy.eclipse.ui.editors.AddValueListButtonComposite;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor.ListSelectControlFactory;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;

/**
 * Property controller for selecting value list in Properties view.
 * 
 * @author rob
 *
 * @param <P> property type
 */
public class ValuelistPropertyController<P> extends PropertyController<P, Integer>
{
	private final boolean includeNone;
	private final IPersist persist;

	public ValuelistPropertyController(Object id, String displayName, IPersist persist, IPersist context, boolean includeNone)
	{
		super(id, displayName);
		this.persist = persist;
		this.includeNone = includeNone;
		setLabelProvider(new SolutionContextDelegateLabelProvider(new ValuelistLabelProvider(
			ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist)), context));
		setSupportsReadonly(true);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{

		final FlattenedSolution flattenedEditingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist);
		return new ListSelectCellEditor(parent, "Select value list", new ValuelistContentProvider(flattenedEditingSolution), getLabelProvider(),
			new ValueListValueEditor(flattenedEditingSolution), isReadOnly(), new ValuelistContentProvider.ValuelistListOptions(includeNone), SWT.NONE,
			new ListSelectControlFactory()
			{
				private TreeSelectDialog dialog = null;

				public void setTreeSelectDialog(TreeSelectDialog dialog)
				{
					this.dialog = dialog;
				}

				public Control createControl(Composite composite)
				{
					AddValueListButtonComposite buttons = new AddValueListButtonComposite(composite, SWT.NONE);
					buttons.setDialog(dialog);
					buttons.setPersist(persist);
					return buttons;
				}
			}, "valuelistDialog"); //$NON-NLS-1$
	}

	public static class ValueListValueEditor implements IValueEditor<Integer>
	{
		private final FlattenedSolution flattenedEditingSolution;

		public ValueListValueEditor(FlattenedSolution flattenedEditingSolution)
		{
			this.flattenedEditingSolution = flattenedEditingSolution;
		}

		public void openEditor(Integer value)
		{
			EditorUtil.openValueListEditor(flattenedEditingSolution, value.intValue());
		}

		public boolean canEdit(Integer value)
		{
			return value != null && value.intValue() != ValuelistLabelProvider.VALUELIST_NONE &&
				AbstractBase.selectById(flattenedEditingSolution.getValueLists(false), value.intValue()) != null;
		}
	}

}
