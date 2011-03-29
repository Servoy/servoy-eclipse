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

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.PojoObservables;
import org.eclipse.core.databinding.observable.ChangeEvent;
import org.eclipse.core.databinding.observable.IChangeListener;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.jface.databinding.swt.SWTObservables;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.ui.util.BindingHelper;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.util.ValidatingDocument.IDocumentValidator;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.docvalidator.LengthDocumentValidator;

public class ColumnAutoEnterDBSeqComposite extends Composite
{
	private DataBindingContext bindingContext;
	private final Text text;

	private Column column;
	private final Label warningLabel;

	/**
	 * Create the composite
	 * 
	 * @param parent
	 * @param style
	 */
	public ColumnAutoEnterDBSeqComposite(Composite parent, int style)
	{
		super(parent, style);

		Label sequenceNameLabel;
		sequenceNameLabel = new Label(this, SWT.NONE);
		sequenceNameLabel.setText("Sequence name");

		text = new Text(this, SWT.BORDER);
		final DocumentValidatorVerifyListener documentValidatorVerifyListener = new DocumentValidatorVerifyListener(
			new IDocumentValidator[] { new IdentDocumentValidator(IdentDocumentValidator.TYPE_SQL), new LengthDocumentValidator(
				Column.MAX_SQL_OBJECT_NAME_LENGTH) });
		text.addVerifyListener(new VerifyListener()
		{
			public void verifyText(VerifyEvent e)
			{
				if (e.text != null && !"".equals(e.text)) documentValidatorVerifyListener.verifyText(e);
				if (e.doit)
				{
					warningLabel.setText("");
				}
				else
				{
					warningLabel.setText("Warning: discouraged sequence name, may not be portable across database vendors");
					e.doit = true;
				}
			}
		});

		warningLabel = new Label(this, SWT.NONE);
		warningLabel.setForeground(Display.getCurrent().getSystemColor(SWT.COLOR_RED));
		warningLabel.setText("");

		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(warningLabel, GroupLayout.DEFAULT_SIZE, 476, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(sequenceNameLabel).addPreferredGap(LayoutStyle.RELATED).add(text, GroupLayout.PREFERRED_SIZE, 368,
						Short.MAX_VALUE))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(sequenceNameLabel).add(text, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(warningLabel).addContainerGap()));
		setLayout(groupLayout);
		//
	}

	public void initDataBindings(final Column c)
	{
		this.column = c;
		bindingContext = BindingHelper.dispose(bindingContext);

		ColumnInfoBean columnInfoBean = new ColumnInfoBean(c.getColumnInfo());
		IObservableValue getCIDBSequenceObserveValue = PojoObservables.observeValue(columnInfoBean, "databaseSequenceName");
		IObservableValue dbSequenceTextObserveWidget = SWTObservables.observeText(text, SWT.Modify);

		bindingContext = new DataBindingContext();

		bindingContext.bindValue(dbSequenceTextObserveWidget, getCIDBSequenceObserveValue, null, null);

		BindingHelper.addGlobalChangeListener(bindingContext, new IChangeListener()
		{
			public void handleChange(ChangeEvent event)
			{
				c.flagColumnInfoChanged();
			}
		});
	}

	@Override
	protected void checkSubclass()
	{
		// Disable the check that prevents subclassing of SWT components
	}

}
