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

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeSelection;
import org.eclipse.jface.viewers.TreePath;
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
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ScopeWithContext;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.PersistHelper;

/**
 * @author jcompagner
 */
public class AddMethodButtonsComposite extends Composite
{
	private PersistContext persistContext;
	private ScopeWithContext selectedScope = null;
	private String methodKey;

	private TreeSelectDialog dialog;

	private final Button createFoundsetMethodButton;
	private final Button createFormMethodButton;
	private PersistContext initialContext;


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
		createMethodLabel.setText("Create method in");

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
				if (selectedScope == null)
				{
					Collection<String> scopes = ModelUtils.getEditingFlattenedSolution(persistContext.getContext()).getScopeNames();
					if (scopes.size() > 1)
					{
						org.eclipse.jface.dialogs.MessageDialog.openInformation(getShell(), "Please select a scope",
							"No scope is specified for method creation.");
						return;
					}
					else if (scopes.size() == 1)
					{
						selectedScope = new ScopeWithContext(scopes.iterator().next(),
							(Solution)persistContext.getContext().getAncestor(IRepository.SOLUTIONS));
					}
				}
				ScriptMethod method = createMethod(persistContext.getContext().getAncestor(IRepository.SOLUTIONS));
				if (method != null)
				{
					dialog.refreshTree();
					dialog.setSelection(getSelectionObject(method));
					dialog.buttonBar.forceFocus();
				}
			}
		});
		createGlobalMethodButton.setText("Scope");
	}

	private ScriptMethod createMethod(IPersist parentPersist)
	{
		String scopeName = null;
		IPersist parent = parentPersist;
		if (selectedScope != null && parent instanceof Solution)
		{
			parent = selectedScope.getRootObject();
			scopeName = selectedScope.getName();
		}
		//check if we want to override form method
		if (initialContext.getContext() instanceof Form && !initialContext.getContext().equals(persistContext.getContext()) &&
			((Form)initialContext.getContext()).getExtendsID() != null &&
			PersistHelper.getOverrideHierarchy((ISupportExtendsID)initialContext.getContext()).contains(persistContext.getContext()) &&
			((Form)persistContext.getContext()).getScriptMethod(methodKey) != null)
		{
			parent = initialContext.getContext();
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
				if (componentFormat.dpType != -1 && componentFormat.dpType != IColumnTypes.MEDIA)
				{
					substitutions = new HashMap<String, String>();
					substitutions.put("dataproviderType",
						ArgumentType.convertFromColumnType(componentFormat.dpType, Column.getDisplayTypeString(componentFormat.dpType)).getName());
				}
			}
		}
		if (persistContext.getContext() instanceof Form && dataSource == null)
		{
			dataSource = "";
		}
		if (dataSource != null)
		{
			if (substitutions != null) substitutions.put("dataSource", dataSource);
			else substitutions = Collections.singletonMap("dataSource", dataSource);
		}
		return NewMethodAction.createNewMethod(getShell(), parent, methodKey, false, null, scopeName, substitutions, persistContext.getPersist(), null);
	}

	public void setContext(PersistContext persistContext, String methodKey)
	{
		if (this.initialContext == null) this.initialContext = persistContext;
		this.persistContext = persistContext;
		this.methodKey = methodKey;
		Form form = (Form)persistContext.getContext().getAncestor(IRepository.FORMS);
		createFormMethodButton.setEnabled(form != null && !form.isFormComponent());
		TableNode tableNode = (TableNode)persistContext.getContext().getAncestor(IRepository.TABLENODES);
		createFoundsetMethodButton.setEnabled(
			(form != null && form.getDataSource() != null && form.getSolution().getSolutionType() != SolutionMetaData.MOBILE) ||
				(tableNode != null && DataSourceUtils.getViewDataSourceName(tableNode.getDataSource()) == null));
	}

	public void setSelectedScope(ScopeWithContext scope)
	{
		this.selectedScope = scope;
	}

	public void setDialog(TreeSelectDialog dialog)
	{
		this.dialog = dialog;
	}

	protected Object getSelectionObject(ScriptMethod method)
	{
		return MethodWithArguments.create(method, null);
	}

	/**
	 * @param selection
	 */
	public void searchSelectedScope(IStructuredSelection selection)
	{
		setSelectedScope(null);
		if (selection != null && !selection.isEmpty() && selection.getFirstElement() instanceof ScopeWithContext)
		{
			setSelectedScope((ScopeWithContext)selection.getFirstElement());
		}
		else if (selection instanceof ITreeSelection)
		{
			TreePath[] paths = ((ITreeSelection)selection).getPaths();
			if (paths != null && paths.length == 1)
			{
				for (int i = paths[0].getSegmentCount(); i-- > 0;)
				{
					if (paths[0].getSegment(i) instanceof ScopeWithContext)
					{
						setSelectedScope((ScopeWithContext)paths[0].getSegment(i));
						break;
					}
					else if (paths[0].getSegment(i) instanceof Form)
					{
						persistContext = PersistContext.create((Form)paths[0].getSegment(i));
						break;
					}
					else if (paths[0].getSegment(i) instanceof ScriptMethod)
					{
						persistContext = PersistContext.create(((ScriptMethod)paths[0].getSegment(i)).getParent());
						break;
					}
				}
			}
		}

	}
}
