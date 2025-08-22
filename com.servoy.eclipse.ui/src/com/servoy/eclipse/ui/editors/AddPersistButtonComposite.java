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

import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;

public abstract class AddPersistButtonComposite extends Composite
{
	private IPersist persist;
	private final Button addPersistMethodButton;
	private TreeSelectDialog dialog;

	public AddPersistButtonComposite(Composite parent, int style, String text)
	{
		super(parent, style);
		addPersistMethodButton = new Button(this, SWT.NONE);
		addPersistMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				IPersist newPersist = createPersist((Solution)persist.getAncestor(IRepository.SOLUTIONS));
				if (newPersist != null)
				{
					dialog.refreshTree();
					dialog.setSelection(newPersist.getUUID().toString());
				}
			}
		});
		addPersistMethodButton.setText(text);
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).add(addPersistMethodButton).add(13, 13, 13)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addPersistMethodButton)).addContainerGap()));
		setLayout(groupLayout);
	}

	public void setPersist(IPersist persist)
	{
		this.persist = persist;
	}

	protected abstract IPersist createPersist(Solution editingSolution);

	public void setDialog(TreeSelectDialog dialog)
	{
		this.dialog = dialog;
	}
}
