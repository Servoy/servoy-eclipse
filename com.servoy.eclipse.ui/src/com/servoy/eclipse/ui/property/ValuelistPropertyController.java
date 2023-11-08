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
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.ValuelistContentProvider;
import com.servoy.eclipse.ui.editors.AddPersistButtonComposite;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor.ListSelectControlFactory;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.wizards.NewValueListWizard;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.util.Utils;

/**
 * Property controller for selecting value list in Properties view.
 *
 * @author rgansevles
 *
 * @param <P> property type
 */
public class ValuelistPropertyController<P> extends PropertyController<P, Integer>
{
	private final boolean includeNone;
	private final PersistContext persistContext;

	public ValuelistPropertyController(Object id, String displayName, PersistContext persistContext, boolean includeNone)
	{
		super(id, displayName);
		this.persistContext = persistContext;
		this.includeNone = includeNone;
		setLabelProvider(new SolutionContextDelegateLabelProvider(
			new ValuelistLabelProvider(ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext()),
				persistContext.getPersist()),
			persistContext.getContext()));
		setSupportsReadonly(true);
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		Object defaultValue = null;
		if (includeNone && persistContext.getPersist() instanceof IBasicWebObject wo)
		{
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState()
				.getWebObjectSpecification(wo.getTypeName());
			if (spec != null)
			{
				PropertyDescription property = spec.getProperty(getId().toString());
				if (property.hasDefault())
				{
					defaultValue = property.getDefaultValue();
				}
			}
		}
		final FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
		return new ValuelistCellEditor<P>(parent, "Select value list", new ValuelistContentProvider(flattenedEditingSolution, persistContext.getContext()),
			getLabelProvider(), new ValueListValueEditor(flattenedEditingSolution), isReadOnly(),
			new ValuelistContentProvider.ValuelistListOptions(includeNone), SWT.NONE, new ListSelectControlFactory()
			{
				private TreeSelectDialog dialog = null;

				public void setTreeSelectDialog(TreeSelectDialog dialog)
				{
					this.dialog = dialog;
				}

				public Control createControl(Composite composite)
				{
					AddPersistButtonComposite buttons = new AddPersistButtonComposite(composite, SWT.NONE, "Create Value List")
					{
						@Override
						protected IPersist createPersist(Solution editingSolution)
						{
							NewValueListWizard newValueListWizard = new NewValueListWizard(flattenedEditingSolution.getName());

							IStructuredSelection selection = StructuredSelection.EMPTY;
							newValueListWizard.init(PlatformUI.getWorkbench(), selection);

							WizardDialog valDialog = new WizardDialog(dialog.getShell(), newValueListWizard);
							valDialog.create();
							valDialog.open();
							return newValueListWizard.getCreatedValueList();
						}
					};
					buttons.setDialog(dialog);
					buttons.setPersist(Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()) ? persistContext.getContext()
						: persistContext.getPersist());
					return buttons;
				}
			}, "valuelistDialog", defaultValue);
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
			EditorUtil.openValueListEditor(flattenedEditingSolution.getValueList(value.intValue()));
		}

		public boolean canEdit(Integer value)
		{
			return value != null && value.intValue() != ValuelistLabelProvider.VALUELIST_NONE &&
				flattenedEditingSolution.getValueList(value.intValue()) != null;
		}
	}
}
