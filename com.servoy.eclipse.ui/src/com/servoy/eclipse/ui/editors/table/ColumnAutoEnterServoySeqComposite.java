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

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnInfoBasedSequenceProvider;
import com.servoy.j2db.persistence.ISequenceProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Utils;

public class ColumnAutoEnterServoySeqComposite extends Composite implements SelectionListener
{

	private final Label stepSizeText;
	private final Label nextValueText;

	private final Button updateRepositoryButton;
	private final Button calculateFromDataButton;
	private final Button refreshFromRepositoryButton;

	private Column column;
	private final Label devInfo;

	/**
	 * Create the composite
	 *
	 * @param parent
	 * @param style
	 */
	public ColumnAutoEnterServoySeqComposite(Composite parent, int style)
	{
		super(parent, style);

		Label nextValueLabel;
		GridLayout gridLayout = new GridLayout(4, false);
		gridLayout.marginTop = 5;
		setLayout(gridLayout);
		nextValueLabel = new Label(this, SWT.NONE);
		nextValueLabel.setText("Next value");

		nextValueText = new Label(this, SWT.BORDER);
		nextValueText.setText("test");
		nextValueText.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));

		Label stepSizeLabel;
		stepSizeLabel = new Label(this, SWT.NONE);
		stepSizeLabel.setText("Step size");

		stepSizeText = new Label(this, SWT.BORDER);
		stepSizeText.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, false, false, 3, 1));
//		stepSizeText.addModifyListener(new ModifyListener()
//		{
//			@Override
//			public void modifyText(ModifyEvent e)
//			{
//				if (column != null && column.getColumnInfo() != null)
//				{
//					int stepSize = Utils.getAsInteger(stepSizeText.getText());
//					if (stepSize <= 0) stepSize = 1;
//					column.getColumnInfo().setSequenceStepSize(stepSize);
//					column.flagColumnInfoChanged();
//				}
//			}
//		});
		new Label(this, SWT.NONE);

		refreshFromRepositoryButton = new Button(this, SWT.NONE);
		refreshFromRepositoryButton.setText("Refresh from repository");
		refreshFromRepositoryButton.addSelectionListener(this);

		calculateFromDataButton = new Button(this, SWT.NONE);
		calculateFromDataButton.setText("Calculate from data");
		calculateFromDataButton.addSelectionListener(this);

		updateRepositoryButton = new Button(this, SWT.NONE);
		updateRepositoryButton.setLayoutData(new GridData(SWT.LEFT, SWT.CENTER, true, false, 1, 1));
		updateRepositoryButton.setText("Update repository");
		updateRepositoryButton.addSelectionListener(this);
		new Label(this, SWT.NONE);

		devInfo = new Label(this, SWT.NONE);
		devInfo.setText("test");
		devInfo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, false, false, 3, 1));
		//
	}

	public void initDataBindings(Column c)
	{
		this.column = c;
	}

	public void initDataValues()
	{
		if (column != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();
			try
			{
				ISequenceProvider sp = ApplicationServerRegistry.get().getServerManager().getSequenceProvider();
				Object sq = null;
				if (sp != null && column.getExistInDB())
				{
					sq = sp.getNextSequence(column, false);
				}
				nextValueText.setText(sq == null ? "" : sq.toString());
				if (!(sp instanceof IColumnInfoBasedSequenceProvider))
				{
					nextValueText.setToolTipText("Developer uses 'select max(pk)' to get a new pk, so this can't be controlled in the developer");
					devInfo.setText("Developer uses 'select max(pk)' to get a new pk, so this can't be controlled in the developer");
					updateRepositoryButton.setEnabled(false);
					refreshFromRepositoryButton.setEnabled(false);
					calculateFromDataButton.setEnabled(false);
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}

			stepSizeText.setText(new Integer(columnInfo.getSequenceStepSize()).toString());
		}
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

	public void widgetDefaultSelected(SelectionEvent e)
	{
		// nothing to do here

	}

	public void widgetSelected(SelectionEvent e)
	{
		if (e.getSource().equals(updateRepositoryButton))
		{
			if (column != null)
			{
				try
				{
					ColumnInfo columnInfo = column.getColumnInfo();
					String val = nextValueText.getText();
					String last = Utils.findLastNumber(val);
					int index = val.indexOf(last);
					if (index > 0)
					{
						columnInfo.setPreSequenceChars(val.substring(0, index));
						columnInfo.setNextSequence(Utils.getAsLong(last));
						columnInfo.setPostSequenceChars(val.substring(index + last.length()));
					}
					else
					{
						columnInfo.setPreSequenceChars(null);
						columnInfo.setNextSequence(Utils.getAsLong(val));
						columnInfo.setPostSequenceChars(null);
					}
					int step = 1;
					try
					{
						step = Integer.parseInt(stepSizeText.getText());
					}
					catch (Exception ex)
					{

					}
					columnInfo.setSequenceStepSize(step);
					ISequenceProvider sp = ApplicationServerRegistry.get().getServerManager().getSequenceProvider();
					if (sp instanceof IColumnInfoBasedSequenceProvider columnInfoBasedSequenceProvider)
					{
						columnInfoBasedSequenceProvider.setNextSequence(column);
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
		else if (e.getSource().equals(calculateFromDataButton))
		{
			if (column != null)
			{
				try
				{
					nextValueText.setText(
						ApplicationServerRegistry.get().getServerManager().getSequenceProvider().syncSequence(column).toString());
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
		else if (e.getSource().equals(refreshFromRepositoryButton))
		{
			if (column != null)
			{
				try
				{
					ServoyModelManager.getServoyModelManager().getServoyModel();
					Object seq = ((IServerInternal)ApplicationServerRegistry.get().getDeveloperRepository().getServer(column.getTable().getServerName()))
						.getNextSequence(
							column.getTable().getName(), column.getName());
					nextValueText.setText(seq.toString());
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}
		if (column != null) column.flagColumnInfoChanged();
	}
}
