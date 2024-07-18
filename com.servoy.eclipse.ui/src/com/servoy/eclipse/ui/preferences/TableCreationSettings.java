/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2019 Servoy BV

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

package com.servoy.eclipse.ui.preferences;

import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ComboViewer;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPreferencePage;

import com.servoy.eclipse.model.preferences.DbiPreferences;
import com.servoy.eclipse.ui.editors.table.ColumnLabelProvider;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.util.ObjectWrapper;

/**
 * @author vidma
 *
 */
public class TableCreationSettings extends PreferencePage implements IWorkbenchPreferencePage
{

	private ComboViewer primaryKeySequenceTypeCombo;
	private ComboViewer primaryKeyUuidTypeCombo;
	private final ObjectWrapper[] uuidInput = new ObjectWrapper[] { //
		new ObjectWrapper(ColumnLabelProvider.UUID_MEDIA_16, PrimaryKeyType.UUD_BYTE_ARRAY), //
		new ObjectWrapper(ColumnLabelProvider.UUID_TEXT_36, PrimaryKeyType.UUD_STRING_ARRAY), //
		new ObjectWrapper(ColumnLabelProvider.UUID_NATIVE, PrimaryKeyType.UUD_NATIVE) };

	private final ObjectWrapper[] otherTypesInput = new ObjectWrapper[] { //
		new ObjectWrapper("INTEGER", PrimaryKeyType.INTEGER) };

	private ComboViewer dbiSortKeyCombo;
	private final ObjectWrapper[] dbiInput = new ObjectWrapper[] { new ObjectWrapper("COLUMN NAME",
		DbiPreferences.DBI_SORT_BY_NAME), new ObjectWrapper("COLUMN INDEX", DbiPreferences.DBI_SORT_BY_INDEX) };

	@Override
	public void init(IWorkbench workbench)
	{
		// TODO Auto-generated method stub

	}

	@Override
	protected Control createContents(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite rootPanel = new Composite(parent, SWT.NONE);
		rootPanel.setLayout(new GridLayout(1, true));
		rootPanel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));

		Group defaultPrimaryKeySequenceType = new Group(rootPanel, SWT.NONE);
		defaultPrimaryKeySequenceType.setText("Default Primary Key Sequence Type");
		defaultPrimaryKeySequenceType.setLayout(new GridLayout(1, true));

		primaryKeySequenceTypeCombo = new ComboViewer(defaultPrimaryKeySequenceType);
		primaryKeySequenceTypeCombo.setContentProvider(new ArrayContentProvider());
		primaryKeySequenceTypeCombo.setLabelProvider(new LabelProvider());
		primaryKeySequenceTypeCombo.setInput(
			new ObjectWrapper[] { new ObjectWrapper("Database Sequence",
				new Integer(ColumnInfo.DATABASE_SEQUENCE)), new ObjectWrapper("Database Identity",
					new Integer(ColumnInfo.DATABASE_IDENTITY)), new ObjectWrapper("UUID Generator", new Integer(ColumnInfo.UUID_GENERATOR)) });
		primaryKeySequenceTypeCombo.addSelectionChangedListener((event) -> {
			Integer selected = getFirstElementValue(primaryKeySequenceTypeCombo, Integer.valueOf(DesignerPreferences.PK_SEQUENCE_TYPE_DEFAULT));
			primaryKeyUuidTypeCombo.setInput(selected.intValue() == ColumnInfo.UUID_GENERATOR ? uuidInput : otherTypesInput);
			primaryKeyUuidTypeCombo.getCombo().select(0);
		});

		new Label(defaultPrimaryKeySequenceType, SWT.NONE).setText("Column Type");
		primaryKeyUuidTypeCombo = new ComboViewer(defaultPrimaryKeySequenceType);
		primaryKeyUuidTypeCombo.setContentProvider(new ArrayContentProvider());
		primaryKeyUuidTypeCombo.setLabelProvider(new LabelProvider());

		Label myLabel = new Label(defaultPrimaryKeySequenceType, SWT.NONE);
		myLabel.setText("DBI sorting key");
		String tooltipTxt = "Change this setting when you want to change the order of the columns in the database information files.\nNote that the already existing database information files are not affected by this change until they are regenerated.";
		myLabel.setToolTipText(tooltipTxt);
		dbiSortKeyCombo = new ComboViewer(defaultPrimaryKeySequenceType);
		dbiSortKeyCombo.setContentProvider(new ArrayContentProvider());
		dbiSortKeyCombo.setLabelProvider(new LabelProvider());
		dbiSortKeyCombo.setInput(dbiInput);
		dbiSortKeyCombo.getCombo().setToolTipText(tooltipTxt);

		initializeFields();

		return rootPanel;
	}

	protected void initializeFields()
	{
		DesignerPreferences prefs = new DesignerPreferences();
		DbiPreferences dbiPrefs = new DbiPreferences();

		setPrimaryKeySequenceTypeValue(prefs.getPrimaryKeySequenceType());
		primaryKeyUuidTypeCombo.setInput(prefs.getPrimaryKeySequenceType() == ColumnInfo.UUID_GENERATOR ? uuidInput : otherTypesInput);
		setPrimaryKeyUuidTypeValue(prefs.getPrimaryKeyUuidType());
		setDBISortingCriteria(dbiPrefs.getDbiSortingKey());
	}

	@Override
	protected void performDefaults()
	{
		primaryKeyUuidTypeCombo.setInput(uuidInput);
		setPrimaryKeyUuidTypeValue(DesignerPreferences.ARRAY_UTF8_TYPE_DEFAULT);
		setDBISortingCriteria(DbiPreferences.DBI_SORT_DEFAULT);
		super.performDefaults();
	}

	@Override
	public boolean performOk()
	{
		DesignerPreferences prefs = new DesignerPreferences();
		DbiPreferences dbiPrefs = new DbiPreferences();
		prefs.setPrimaryKeySequenceType(
			getFirstElementValue(primaryKeySequenceTypeCombo, Integer.valueOf(DesignerPreferences.PK_SEQUENCE_TYPE_DEFAULT)).intValue());
		prefs.setPrimaryKeyUuidType(getFirstElementValue(primaryKeyUuidTypeCombo, DesignerPreferences.ARRAY_UTF8_TYPE_DEFAULT));
		dbiPrefs.setDbiSortingKey(getFirstElementValue(dbiSortKeyCombo, DbiPreferences.DBI_SORT_DEFAULT));
		prefs.save();
		dbiPrefs.save();

		return true;
	}

	private void setPrimaryKeyUuidTypeValue(PrimaryKeyType pk_type)
	{
		for (ObjectWrapper ow : (ObjectWrapper[])primaryKeyUuidTypeCombo.getInput())
		{
			if (ow.getType().equals(pk_type))
			{
				primaryKeyUuidTypeCombo.setSelection(new StructuredSelection(ow));
				return;
			}
		}
	}

	private void setDBISortingCriteria(String sortingKey)
	{
		for (ObjectWrapper ow : (ObjectWrapper[])dbiSortKeyCombo.getInput())
		{
			if (ow.getType().equals(sortingKey))
			{
				dbiSortKeyCombo.setSelection(new StructuredSelection(ow));
				return;
			}
		}
	}

	private void setPrimaryKeySequenceTypeValue(int pk_seq_type)
	{
		Integer seqType = Integer.valueOf(pk_seq_type);
		for (ObjectWrapper ow : (ObjectWrapper[])primaryKeySequenceTypeCombo.getInput())
		{
			if (ow.getType().equals(seqType))
			{
				primaryKeySequenceTypeCombo.setSelection(new StructuredSelection(ow));
				return;
			}
		}
	}

	/**
	 * @param viewer
	 * @param default
	 * @return
	 */
	protected <T> T getFirstElementValue(ComboViewer viewer, T defaultValue)
	{
		ISelection selection = viewer.getSelection();
		if (selection instanceof IStructuredSelection)
		{
			Object firstElement = ((IStructuredSelection)selection).getFirstElement();
			if (firstElement instanceof ObjectWrapper)
			{
				T type = (T)((ObjectWrapper)firstElement).getType();
				if (type != null)
				{
					return type;
				}
			}
		}
		return defaultValue;
	}

}
