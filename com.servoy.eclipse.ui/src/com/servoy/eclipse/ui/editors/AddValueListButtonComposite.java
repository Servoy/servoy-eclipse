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
package com.servoy.eclipse.ui.editors;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;

import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewValueListAction;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValueList;

public class AddValueListButtonComposite extends Composite
{
	private IPersist persist;
	private final Button addValueListMethodButton;
	private TreeSelectDialog dialog;

	public AddValueListButtonComposite(Composite parent, int style)
	{
		super(parent, style);
		addValueListMethodButton = new Button(this, SWT.NONE);
		addValueListMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				ValueList valueList = createValueList(persist.getAncestor(IRepository.SOLUTIONS));
				if (valueList != null)
				{
					dialog.refreshTree();
					dialog.setSelection(new Integer(valueList.getID()));
				}
			}
		});
		addValueListMethodButton.setText("Create Value List");
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).add(addValueListMethodButton).add(13, 13, 13)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addValueListMethodButton)).addContainerGap()));
		setLayout(groupLayout);
	}

	public void setPersist(IPersist persist)
	{
		this.persist = persist;
	}

	private ValueList createValueList(IPersist editingSolution)
	{
		ValueList val = null;
		String name = NewValueListAction.askValueListName(Display.getDefault().getActiveShell());
		if (name != null && editingSolution instanceof Solution)
		{
			val = NewValueListAction.createValueList(name, (Solution)editingSolution);
		}
		return val;
	}

	public void setDialog(TreeSelectDialog dialog)
	{
		this.dialog = dialog;
	}
}
