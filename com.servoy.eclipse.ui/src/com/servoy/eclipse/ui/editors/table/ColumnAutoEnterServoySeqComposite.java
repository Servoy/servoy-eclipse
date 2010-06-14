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

import java.rmi.RemoteException;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.IColumnInfoBasedSequenceProvider;
import com.servoy.j2db.persistence.ISequenceProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.util.Utils;

public class ColumnAutoEnterServoySeqComposite extends Composite implements SelectionListener
{

	private final Text stepSizeText;
	private final Text nextValueText;

	private final Button updateRepositoryButton;
	private final Button calculateFromDataButton;
	private final Button refreshFromRepositoryButton;

	private Column column;

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
		nextValueLabel = new Label(this, SWT.NONE);
		nextValueLabel.setText("Next value");

		nextValueText = new Text(this, SWT.BORDER);

		Label stepSizeLabel;
		stepSizeLabel = new Label(this, SWT.NONE);
		stepSizeLabel.setText("Step size");

		stepSizeText = new Text(this, SWT.BORDER);

		updateRepositoryButton = new Button(this, SWT.NONE);
		updateRepositoryButton.setText("Update repository");
		updateRepositoryButton.addSelectionListener(this);

		calculateFromDataButton = new Button(this, SWT.NONE);
		calculateFromDataButton.setText("Calculate from data");
		calculateFromDataButton.addSelectionListener(this);

		refreshFromRepositoryButton = new Button(this, SWT.NONE);
		refreshFromRepositoryButton.setText("Refresh from repository");
		refreshFromRepositoryButton.addSelectionListener(this);

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().addContainerGap().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(nextValueLabel).add(stepSizeLabel)).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(stepSizeText, GroupLayout.PREFERRED_SIZE, 326, Short.MAX_VALUE).add(
							nextValueText, GroupLayout.PREFERRED_SIZE, 326, Short.MAX_VALUE))).add(
					groupLayout.createSequentialGroup().addContainerGap(43, Short.MAX_VALUE).add(refreshFromRepositoryButton).addPreferredGap(
						LayoutStyle.RELATED).add(calculateFromDataButton).addPreferredGap(LayoutStyle.RELATED).add(updateRepositoryButton))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(nextValueLabel).add(nextValueText, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(stepSizeLabel).add(stepSizeText, GroupLayout.PREFERRED_SIZE,
					GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(calculateFromDataButton).add(refreshFromRepositoryButton).add(updateRepositoryButton)).addContainerGap()));
		setLayout(groupLayout);
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
			IServer server;
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel();
				server = ServoyModel.getDeveloperRepository().getServer(column.getTable().getServerName());
				if (server != null)
				{
					ISequenceProvider sp = ServoyModel.getServerManager().getSequenceProvider(server.getName());
					Object sq = null;
					if (sp != null && column.getExistInDB())
					{
						sq = sp.getNextSequence(column, false);
					}
					nextValueText.setText(sq == null ? "" : sq.toString()); //$NON-NLS-1$
					if (!(sp instanceof IColumnInfoBasedSequenceProvider))
					{
						updateRepositoryButton.setEnabled(false);
						refreshFromRepositoryButton.setEnabled(false);
						calculateFromDataButton.setEnabled(false);
						nextValueText.setEditable(false);
						stepSizeText.setEditable(false);
					}
				}
				else
				{
					nextValueText.setText(""); //$NON-NLS-1$
					ServoyLog.logError("Cannot find server (for next sequence)" + column.getTable().getServerName(), null); //$NON-NLS-1$
				}
			}
			catch (RemoteException e)
			{
				ServoyLog.logError(e);
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
					ISequenceProvider sp = ServoyModel.getServerManager().getSequenceProvider(IServer.REPOSITORY_SERVER);
					if (sp instanceof IColumnInfoBasedSequenceProvider)
					{
						((IColumnInfoBasedSequenceProvider)sp).setNextSequence(columnInfo);
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
					nextValueText.setText(ServoyModel.getServerManager().getSequenceProvider(IServer.REPOSITORY_SERVER).syncSequence(column).toString());
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
					Object seq = ((IServerInternal)ServoyModel.getDeveloperRepository().getServer(column.getTable().getServerName())).getNextSequence(
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
