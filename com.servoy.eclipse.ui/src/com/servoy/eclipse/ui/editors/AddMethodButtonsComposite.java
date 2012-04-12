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

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.Scope;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;

/**
 * @author jcompagner
 */
public class AddMethodButtonsComposite extends Composite
{
	private PersistContext persistContext;
	private Scope selectedScope = null;
	private String methodKey;

	private TreeSelectDialog dialog;

	private final Button createFoundsetMethodButton;
	private final Button createFormMethodButton;


	/**
	 * @param parent
	 * @param style
	 * @param iPersist 
	 */
	public AddMethodButtonsComposite(Composite parent, int style)
	{
		super(parent, style);
		setLayout(new GridLayout(4, false));

		Button createGlobalMethodButton;

		Label createMethodLabel = new Label(this, SWT.NONE);
		createMethodLabel.setText("Create method in"); //$NON-NLS-1$

		createFormMethodButton = new Button(this, SWT.NONE);
		createFormMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				ScriptMethod method = createMethod(persistContext.getContext().getAncestor(IRepository.FORMS));
				if (method != null)
				{
					dialog.refreshTree();
					dialog.setSelection(getSelectionObject(method));
					dialog.buttonBar.forceFocus();
				}
			}
		});
		createFormMethodButton.setText("Form");

		createFoundsetMethodButton = new Button(this, SWT.NONE);
		createFoundsetMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				TableNode tableNode = (TableNode)persistContext.getContext().getAncestor(IRepository.TABLENODES);
				if (tableNode == null)
				{
					// try forms table node
					Form form = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
					if (form != null && form.getDataSource() != null)
					{
						try
						{
							tableNode = form.getSolution().getOrCreateTableNode(form.getDataSource());
						}
						catch (RepositoryException ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
				ScriptMethod method = createMethod(tableNode);
				if (method != null)
				{
					dialog.refreshTree();
					dialog.setSelection(getSelectionObject(method));
					dialog.buttonBar.forceFocus();
				}
			}
		});
		createFoundsetMethodButton.setText("Entity");

		createGlobalMethodButton = new Button(this, SWT.NONE);
		createGlobalMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				ScriptMethod method = createMethod(persistContext.getContext().getAncestor(IRepository.SOLUTIONS));
				if (method != null)
				{
					dialog.refreshTree();
					dialog.setSelection(getSelectionObject(method));
					dialog.buttonBar.forceFocus();
				}
			}
		});
		createGlobalMethodButton.setText("Global");
	}

	@SuppressWarnings("nls")
	private ScriptMethod createMethod(IPersist parent)
	{
		String scopeName = null;
		if (selectedScope != null && parent instanceof Solution)
		{
			parent = selectedScope.getRootObject();
			scopeName = selectedScope.getName();
		}
		String dataSource = null;
		if (persistContext.getContext() instanceof AbstractBase)
		{
			dataSource = (String)((AbstractBase)persistContext.getContext()).getProperty(StaticContentSpecLoader.PROPERTY_DATASOURCE.getPropertyName());
		}
		Map<String, String> substitutions = null;
		if (persistContext.getPersist() instanceof Field)
		{
			Field field = (Field)persistContext.getPersist();
			if (field.getDataProviderID() != null)
			{
				// use dataprovider type as defined by column converter
				ComponentFormat componentFormat = ComponentFormat.getComponentFormat(field.getFormat(), field.getDataProviderID(),
					ModelUtils.getEditingFlattenedSolution(parent).getDataproviderLookup(null, persistContext.getContext()),
					Activator.getDefault().getDesignClient());
				if (componentFormat.dpType != -1 && componentFormat.dpType != -4)
				{
					substitutions = new HashMap<String, String>();
					substitutions.put("dataproviderType",
						ArgumentType.convertFromColumnType(componentFormat.dpType, Column.getDisplayTypeString(componentFormat.dpType)).getName());
				}
			}
		}
		if (dataSource != null)
		{
			if (substitutions != null) substitutions.put("datasource", dataSource);
			else substitutions = Collections.singletonMap("dataSource", dataSource);
		}
		return NewMethodAction.createNewMethod(getShell(), parent, methodKey, false, null, scopeName, substitutions, persistContext.getPersist());
	}

	/**
	 * @param form
	 */
	public void setContext(PersistContext persistContext, String methodKey)
	{
		this.persistContext = persistContext;
		this.methodKey = methodKey;
		Form form = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
		createFormMethodButton.setEnabled(form != null);
		createFoundsetMethodButton.setEnabled((form != null && form.getDataSource() != null) ||
			persistContext.getContext().getAncestor(IRepository.TABLENODES) != null);
	}

	public void setSelectedScope(Scope scope)
	{
		this.selectedScope = scope;
	}

	/**
	 * @param dialog
	 */
	public void setDialog(TreeSelectDialog dialog)
	{
		this.dialog = dialog;
	}

	protected Object getSelectionObject(ScriptMethod method)
	{
		return MethodWithArguments.create(method, null);
	}
}
