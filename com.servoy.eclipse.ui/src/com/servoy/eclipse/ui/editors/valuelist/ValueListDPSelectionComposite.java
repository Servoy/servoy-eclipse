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
package com.servoy.eclipse.ui.editors.valuelist;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderContentProvider;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.editors.ValueListEditor;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;

public class ValueListDPSelectionComposite extends Composite
{


	private final DataProviderTreeViewer tree;
	private final Button returnInDataproviderButton;
	private final Button showInFieldButton;
	private DataBindingContext m_bindingContext;
	private boolean showInField;
	private boolean returnInDataProvider;
	private ValueList valueList;
	private int mask;
	private Table table;
	private int methodNr;

	public ValueListDPSelectionComposite(Composite parent, FlattenedSolution flattenedSolution, int style)
	{
		super(parent, style);

		showInFieldButton = new Button(this, SWT.CHECK);
		showInFieldButton.setText("Show in field / list");

		tree = new DataProviderTreeViewer(this, DataProviderLabelProvider.INSTANCE_HIDEPREFIX, new DataProviderContentProvider(null, flattenedSolution, null),
			new DataProviderTreeViewer.DataProviderOptions(false, true, true, false, false, false, false, false, INCLUDE_RELATIONS.NO, false, true, null),
			false, false, TreePatternFilter.FILTER_LEAFS, SWT.SINGLE);

		returnInDataproviderButton = new Button(this, SWT.CHECK);
		returnInDataproviderButton.setText("Return in dataprovider");
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(tree, GroupLayout.PREFERRED_SIZE, 180, Short.MAX_VALUE).add(
			groupLayout.createSequentialGroup().addContainerGap().add(showInFieldButton).addContainerGap(67, Short.MAX_VALUE)).add(
			groupLayout.createSequentialGroup().addContainerGap().add(returnInDataproviderButton).addContainerGap(41, Short.MAX_VALUE)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(showInFieldButton).addPreferredGap(LayoutStyle.RELATED).add(tree, GroupLayout.PREFERRED_SIZE, 84,
				Short.MAX_VALUE).addPreferredGap(LayoutStyle.RELATED).add(returnInDataproviderButton)));
		setLayout(groupLayout);
	}

	class SelectionChangedListener implements ISelectionChangedListener
	{

		private final ValueListEditor editor;

		public SelectionChangedListener(ValueListEditor editor)
		{
			this.editor = editor;
		}

		public void selectionChanged(SelectionChangedEvent event)
		{
			editor.flagModified();
			Object[] selectedObjects = ((IStructuredSelection)event.getSelection()).toArray();
			String dataProvider = null;
			if (selectedObjects != null && selectedObjects.length > 0 && selectedObjects[0] instanceof IDataProvider) dataProvider = ((IDataProvider)selectedObjects[0]).getDataProviderID();
			switch (methodNr)
			{
				case 1 :
					editor.getValueList().setDataProviderID1(dataProvider);
					break;
				case 2 :
					editor.getValueList().setDataProviderID2(dataProvider);
					break;
				case 3 :
					editor.getValueList().setDataProviderID3(dataProvider);
					break;
				default :
					break;
			}

		}

	}

	private SelectionChangedListener listener = null;

	public void initDataBindings(final Table table)
	{
		((DataProviderContentProvider)tree.getContentProvider()).setTable(table, null);
		tree.refreshTree();
		this.table = table;
		switch (methodNr)
		{
			case 1 :
				if (valueList.getDataProviderID1() != null)
				{
					setSelection(valueList.getDataProviderID1());
				}
				break;
			case 2 :
				if (valueList.getDataProviderID2() != null)
				{
					setSelection(valueList.getDataProviderID2());
				}
				break;
			case 3 :
				if (valueList.getDataProviderID3() != null)
				{
					setSelection(valueList.getDataProviderID3());
				}
				break;
			default :
				break;
		}
	}

	public void setSelection(String selection)
	{
		if (table != null)
		{
			try
			{
				ScriptCalculation calculation = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution().getScriptCalculation(
					selection, table);
				if (calculation != null) tree.setSelection(new StructuredSelection(new Object[] { calculation }));
				else
				{
					Column column = table.getColumn(selection);
					tree.setSelection(new StructuredSelection(new Object[] { column }));
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	public void initValueListBindings(final ValueListEditor editor, final int methodNr, int mask)
	{
		this.methodNr = methodNr;
		valueList = editor.getValueList();
		this.mask = mask;
		setShowInField((valueList.getShowDataProviders() & mask) != 0);
		setReturnInDataProvider((valueList.getReturnDataProviders() & mask) != 0);

		listener = new SelectionChangedListener(editor);
		addSelectionChangedListener();

		m_bindingContext = BindingHelper.dispose(m_bindingContext);

		IObservableValue returnDataProviderFieldTextObserveWidget = SWTObservables.observeSelection(returnInDataproviderButton);
		IObservableValue getValueListReturnDataProviderObserveValue = PojoObservables.observeValue(this, "returnInDataProvider");
		IObservableValue showInFieldSelectionObserveWidget = SWTObservables.observeSelection(showInFieldButton);
		IObservableValue getShowInFieldSelectionObserveValue = PojoObservables.observeValue(this, "showInField");

		m_bindingContext = new DataBindingContext();

		m_bindingContext.bindValue(returnDataProviderFieldTextObserveWidget, getValueListReturnDataProviderObserveValue, null, null);
		m_bindingContext.bindValue(showInFieldSelectionObserveWidget, getShowInFieldSelectionObserveValue, null, null);

		BindingHelper.addGlobalChangeListener(m_bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				editor.flagModified();
			}
		});
	}

	/**
	 * Add the default selection listener; Needed for ValueListEditor;
	 */
	public void addSelectionChangedListener()
	{
		if (listener != null)
		{
			tree.addSelectionChangedListener(listener);
		}
	}

	/**
	 * 
	 */
	public void removeSelectionChangedListener()
	{
		tree.removeSelectionChangedListener(listener);
	}

	public boolean getReturnInDataproviderFlag()
	{
		return returnInDataproviderButton.getSelection();
	}

	public boolean getShowInFieldFlag()
	{
		return showInFieldButton.getSelection();
	}

	public Object getDataProvider()
	{
		return ((IStructuredSelection)tree.getSelection()).getFirstElement();
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	@Override
	public void setEnabled(boolean value)
	{
		tree.setEnabled(value);
		returnInDataproviderButton.setEnabled(value);
		showInFieldButton.setEnabled(value);
	}

	public boolean getShowInField()
	{
		return showInField;
	}

	public void setShowInField(boolean showInField)
	{
		this.showInField = showInField;
		if (valueList != null)
		{
			int showValues = valueList.getShowDataProviders();
			if ((showValues & mask) != 0 && !showInField) showValues = showValues - mask;
			if ((showValues & mask) == 0 && showInField) showValues = showValues + mask;
			valueList.setShowDataProviders(showValues);
		}
	}

	public boolean getReturnInDataProvider()
	{
		return returnInDataProvider;
	}

	public void setReturnInDataProvider(boolean returnInDataProvider)
	{
		this.returnInDataProvider = returnInDataProvider;
		if (valueList != null)
		{
			int returnValues = valueList.getReturnDataProviders();
			if ((returnValues & mask) != 0 && !returnInDataProvider) returnValues = returnValues - mask;
			if ((returnValues & mask) == 0 && returnInDataProvider) returnValues = returnValues + mask;
			valueList.setReturnDataProviders(returnValues);
		}
	}

	public void initDefaultValues()
	{
		returnInDataproviderButton.setSelection(true);
		returnInDataproviderButton.notifyListeners(SWT.Selection, new Event());
		showInFieldButton.setSelection(true);
		showInFieldButton.notifyListeners(SWT.Selection, new Event());
	}
}
