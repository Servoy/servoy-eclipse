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
package com.servoy.eclipse.ui.editors.relation;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.UpdateValueStrategy;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.SortDialog;
import com.servoy.eclipse.ui.editors.RelationEditor;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.EditorUtil.Encapsulation2StringConverter;
import com.servoy.eclipse.ui.util.EditorUtil.String2EncapsulationConverter;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.query.ISQLJoin;

public class OptionsComposite extends Group
{
	private final Text initalSort;
	private DataBindingContext m_bindingContext;
	private final Combo joinCombo;
	private final Button allowCreationOfButton;
	private final Button allowParentDeleteButton;
	private final Button deleteRelatedRecordsButton;
	private RelationEditor relationEditor;
	private final Text deprecated;
	private final Combo encapsulation;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public OptionsComposite(Composite parent, int style)
	{
		super(parent, style);
		setText("Options");

		allowCreationOfButton = new Button(this, SWT.CHECK);
		allowCreationOfButton.setText("Allow creation of related records");

		allowParentDeleteButton = new Button(this, SWT.CHECK);
		allowParentDeleteButton.setText("Allow parent delete when having related records");

		deleteRelatedRecordsButton = new Button(this, SWT.CHECK);
		deleteRelatedRecordsButton.setText("Delete related records");

		Label joinTypeLabel;
		joinTypeLabel = new Label(this, SWT.NONE);
		joinTypeLabel.setText("Join type");

		joinCombo = new Combo(this, SWT.READ_ONLY);
		joinCombo.setItems(new String[] { ISQLJoin.JOIN_TYPES_NAMES[ISQLJoin.INNER_JOIN], ISQLJoin.JOIN_TYPES_NAMES[ISQLJoin.LEFT_OUTER_JOIN] });

		Label initialSortLabel;
		initialSortLabel = new Label(this, SWT.NONE);
		initialSortLabel.setText("Initial Sort");

		Button button;
		button = new Button(this, SWT.NONE);
		button.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				Relation relation = relationEditor.getRelation();
				SortDialog dialog;
				try
				{
					if (relation.getForeignTableName() != null)
					{
						FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(relation);
						dialog = new SortDialog(getShell(), editingFlattenedSolution, relation.getForeignTable(), relation.getInitialSort(), "Sort options");
						dialog.open();

						if (dialog.getReturnCode() != Window.CANCEL)
						{
							String sort = dialog.getValue().toString();
							relation.setInitialSort(sort);
							initalSort.setText(sort);
							relationEditor.flagModified(false);
						}
					}
				}
				catch (RepositoryException e1)
				{
					ServoyLog.logError("Error showing initalsort dialog of relation " + relation, e1);
				}
			}
		});
		button.setText("...");

		initalSort = new Text(this, SWT.BORDER);
		initalSort.setEditable(false);

		Label deprecatedLabel;
		deprecatedLabel = new Label(this, SWT.NONE);
		deprecatedLabel.setText("Deprecated");
		deprecated = new Text(this, SWT.BORDER);
		Label encapsulationLabel;
		encapsulationLabel = new Label(this, SWT.NONE);
		encapsulationLabel.setText("Encapsulation");
		encapsulation = new Combo(this, SWT.READ_ONLY);
		encapsulation.setItems(new String[] { Messages.Public, Messages.HideInScriptingModuleScope, Messages.ModuleScope });

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(7, 7, 7).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(initialSortLabel).add(joinTypeLabel).add(deprecatedLabel).add(encapsulationLabel)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(initalSort, GroupLayout.PREFERRED_SIZE, 228, GroupLayout.PREFERRED_SIZE).add(
					joinCombo, GroupLayout.PREFERRED_SIZE, 228, GroupLayout.PREFERRED_SIZE).add(deprecated, GroupLayout.PREFERRED_SIZE, 228,
					GroupLayout.PREFERRED_SIZE).add(encapsulation, GroupLayout.PREFERRED_SIZE, 228, GroupLayout.PREFERRED_SIZE)).addPreferredGap(
				LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.LEADING).add(button)).add(22, 22, 22).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(allowCreationOfButton).add(allowParentDeleteButton).add(deleteRelatedRecordsButton)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(9, 9, 9).add(
						groupLayout.createParallelGroup(GroupLayout.BASELINE).add(joinTypeLabel).add(joinCombo, GroupLayout.PREFERRED_SIZE,
							GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.BASELINE).add(initialSortLabel).add(initalSort).add(button)).addPreferredGap(
						LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(deprecatedLabel).add(deprecated)).addPreferredGap(
						LayoutStyle.RELATED).add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(encapsulationLabel).add(encapsulation))).add(
					groupLayout.createSequentialGroup().addContainerGap().add(allowCreationOfButton).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createSequentialGroup().add(allowParentDeleteButton).addPreferredGap(LayoutStyle.RELATED).add(deleteRelatedRecordsButton)))).addContainerGap(
				41, Short.MAX_VALUE)));
		setLayout(groupLayout);
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public void initDataBindings(final RelationEditor relationEditor)
	{
		this.relationEditor = relationEditor;
		m_bindingContext = BindingHelper.dispose(m_bindingContext);

		IObservableValue allowCreationOfButtonObserveWidget = SWTObservables.observeSelection(allowCreationOfButton);
		IObservableValue allowCreationRelatedRecordsObserveValue = PojoObservables.observeValue(relationEditor.getRelation(), "allowCreationRelatedRecords");

		IObservableValue allowParentDeleteButtonObserveWidget = SWTObservables.observeSelection(allowParentDeleteButton);
		IObservableValue allowParentDeleteWhenHavingRelatedRecordsObserveValue = PojoObservables.observeValue(relationEditor.getRelation(),
			"allowParentDeleteWhenHavingRelatedRecords");

		IObservableValue deleteRelatedRecordsButtonObserveWidget = SWTObservables.observeSelection(deleteRelatedRecordsButton);
		IObservableValue deleteRelatedRecordsObserveValue = PojoObservables.observeValue(relationEditor.getRelation(), "deleteRelatedRecords");

		IObservableValue joinComboObserveWidget = SWTObservables.observeSelection(joinCombo);
		IObservableValue joinTypeObserveValue = PojoObservables.observeValue(relationEditor.getRelation(), "joinType");

		IObservableValue initialSortObserveWidget = SWTObservables.observeText(initalSort, SWT.Modify);
		IObservableValue initialSortObserveValue = PojoObservables.observeValue(relationEditor.getRelation(), "initialSort");

		IObservableValue deprecatedObserveWidget = SWTObservables.observeText(deprecated, SWT.Modify);
		IObservableValue deprecatedObserveValue = PojoObservables.observeValue(relationEditor.getRelation(), "deprecated");

		IObservableValue encapsulationObserveWidget = SWTObservables.observeSelection(encapsulation);
		IObservableValue encapsulationObserveValue = PojoObservables.observeValue(relationEditor.getRelation(), "encapsulation");

		m_bindingContext = new DataBindingContext();

		m_bindingContext.bindValue(allowCreationOfButtonObserveWidget, allowCreationRelatedRecordsObserveValue, null, null);
		m_bindingContext.bindValue(allowParentDeleteButtonObserveWidget, allowParentDeleteWhenHavingRelatedRecordsObserveValue, null, null);
		m_bindingContext.bindValue(deleteRelatedRecordsButtonObserveWidget, deleteRelatedRecordsObserveValue, null, null);
		m_bindingContext.bindValue(initialSortObserveWidget, initialSortObserveValue, new UpdateValueStrategy(), new UpdateValueStrategy());
		m_bindingContext.bindValue(joinComboObserveWidget, joinTypeObserveValue, new UpdateValueStrategy().setConverter(String2JoinTypeConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(JoinType2StringConverter.INSTANCE));
		m_bindingContext.bindValue(deprecatedObserveWidget, deprecatedObserveValue,
			new UpdateValueStrategy().setConverter(EmptyStringToNullConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(EmptyStringToNullConverter.INSTANCE));
		m_bindingContext.bindValue(encapsulationObserveWidget, encapsulationObserveValue,
			new UpdateValueStrategy().setConverter(String2EncapsulationConverter.INSTANCE),
			new UpdateValueStrategy().setConverter(Encapsulation2StringConverter.INSTANCE));

		BindingHelper.addGlobalChangeListener(m_bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				relationEditor.flagModified(false);
			}
		});
	}

	public void refresh()
	{
		m_bindingContext.updateTargets();
	}

	public static class JoinType2StringConverter extends Converter
	{
		public static final JoinType2StringConverter INSTANCE = new JoinType2StringConverter();

		private JoinType2StringConverter()
		{
			super(int.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject instanceof Integer)
			{
				int jt = ((Integer)fromObject).intValue();
				if (jt >= 0 && jt < ISQLJoin.JOIN_TYPES_NAMES.length)
				{
					return ISQLJoin.JOIN_TYPES_NAMES[jt];
				}
			}
			return Messages.LabelUnresolved;
		}
	}
	public static class String2JoinTypeConverter extends Converter
	{
		public static final String2JoinTypeConverter INSTANCE = new String2JoinTypeConverter();

		private String2JoinTypeConverter()
		{
			super(String.class, int.class);
		}

		public Object convert(Object fromObject)
		{
			for (int jt : ISQLJoin.ALL_JOIN_TYPES)
			{
				if (ISQLJoin.JOIN_TYPES_NAMES[jt].equals(fromObject))
				{
					return new Integer(jt);
				}
			}
			return new Integer(-1);
		}
	}
	public static class EmptyStringToNullConverter extends Converter
	{
		public static final EmptyStringToNullConverter INSTANCE = new EmptyStringToNullConverter();

		private EmptyStringToNullConverter()
		{
			super(String.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject.toString().equals("")) return null;
			return fromObject;
		}
	}
}
