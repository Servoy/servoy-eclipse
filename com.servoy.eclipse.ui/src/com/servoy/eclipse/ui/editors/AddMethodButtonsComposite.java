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
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.dialogs.MethodDialog;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ScriptMethod;

/**
 * @author jcompagner
 */
public class AddMethodButtonsComposite extends Composite
{

	private IPersist persist;
	private String methodKey;

	private MethodDialog dialog;

	private final Button addFormMethodButton;


	/**
	 * @param parent
	 * @param style
	 */
	public AddMethodButtonsComposite(Composite parent, int style)
	{
		super(parent, style);

		addFormMethodButton = new Button(this, SWT.NONE);
		addFormMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				ScriptMethod method = createMethod(persist.getAncestor(IRepository.FORMS));
				if (method != null)
				{
					dialog.refreshTree();
					dialog.expandFormNode();
					dialog.setSelection(new MethodWithArguments(method.getID()));
					dialog.buttonBar.forceFocus();
				}
			}
		});
		addFormMethodButton.setText("Create Form Method");

		Button createGlobalMethodButton;
		createGlobalMethodButton = new Button(this, SWT.NONE);
		createGlobalMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				ScriptMethod method = createMethod(persist.getAncestor(IRepository.SOLUTIONS));
				if (method != null)
				{
					dialog.refreshTree();
					dialog.expandGlobalsNode();
					dialog.setSelection(new MethodWithArguments(method.getID()));
					dialog.buttonBar.forceFocus();
				}
			}
		});
		createGlobalMethodButton.setText("Create Global Method");
		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).add(addFormMethodButton).addPreferredGap(LayoutStyle.RELATED).add(createGlobalMethodButton).add(
				13, 13, 13)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addFormMethodButton).add(createGlobalMethodButton)).addContainerGap(
				GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		setLayout(groupLayout);
	}

	private ScriptMethod createMethod(IPersist parent)
	{
		return NewMethodAction.createNewMethod(getShell(), parent, methodKey, false, null);
	}

	/**
	 * @param form
	 */
	public void setPersist(IPersist persist, String methodKey)
	{
		this.persist = persist;
		this.methodKey = methodKey;
		addFormMethodButton.setEnabled(persist.getAncestor(IRepository.FORMS) != null);
	}

	/**
	 * @param dialog
	 */
	public void setDialog(MethodDialog dialog)
	{
		this.dialog = dialog;
	}

}
