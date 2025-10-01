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

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.MethodWithArguments;
import com.servoy.eclipse.ui.views.solutionexplorer.actions.NewMethodAction;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Composite for creating script providers (calc or global) for rowBGColorCalculation select dialog in Properties View.
 *
 * @author gboros
 */

public class AddScriptProviderButtonsComposite extends Composite
{
	private ITable table;
	private IPersist persist;
	private ScriptProviderCellEditor.ScriptDialog dialog;

	private final Button createCalculationButton;
	private final String methodKey;

	/**
	 * @param parent
	 * @param style
	 */
	public AddScriptProviderButtonsComposite(Composite parent, String methodKey, int style)
	{
		super(parent, style);
		this.methodKey = methodKey;

		createCalculationButton = new Button(this, SWT.NONE);
		createCalculationButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				String calcName = AddScriptProviderButtonsComposite.this.askForCalculationName();
				if (calcName != null)
				{
					ScriptCalculation calculation = createCalculation(persist.getAncestor(IRepository.SOLUTIONS), calcName, table,
						AddScriptProviderButtonsComposite.this.methodKey);
					if (calculation != null)
					{
						dialog.refreshTree();
						dialog.expandCalculationNode();
						dialog.setSelection(MethodWithArguments.create(calculation, null));
					}
				}
			}
		});
		createCalculationButton.setText("Create Calculation");
		createCalculationButton.setEnabled(table != null); // until the table is set

		Button createGlobalMethodButton = new Button(this, SWT.NONE);
		createGlobalMethodButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				ScriptMethod method = NewMethodAction.createNewMethod(getShell(), persist.getAncestor(IRepository.SOLUTIONS),
					AddScriptProviderButtonsComposite.this.methodKey, false, null, null, null);
				if (method != null)
				{
					dialog.refreshTree();
					dialog.expandGlobalsNode();
					dialog.setSelection(MethodWithArguments.create(method, null));
				}
			}
		});
		createGlobalMethodButton.setText("Create Global Method");


		final GroupLayout groupLayout = new GroupLayout(this);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().add(10, 10, 10).addPreferredGap(LayoutStyle.RELATED).add(createCalculationButton).add(10, 10, 10).add(
				createGlobalMethodButton).add(13, 13, 13)));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(
			groupLayout.createParallelGroup(GroupLayout.BASELINE).add(createCalculationButton).add(createGlobalMethodButton)).addContainerGap(
				GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		setLayout(groupLayout);
	}

	private static ScriptCalculation createCalculation(IPersist parent, String calcName, ITable table, String methodKey)
	{
		if (parent instanceof Solution && table != null)
		{
			try
			{
				Solution solution = (Solution)parent;
				IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();

				IValidateName nameValidator = servoyModel.getNameValidator();
				ScriptCalculation calc = solution.createNewScriptCalculation(nameValidator, table, calcName, null);
				if (calc != null)
				{
					calc.setType(IColumnTypes.TEXT);
					MethodTemplate template = MethodTemplate.getTemplate(calc.getClass(), methodKey);
					String calcCode = template.getDefaultMethodCode();
					if (calcCode == null || calcCode.trim().length() == 0) calcCode = "\treturn \"\";";

					ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
					String userTemplate = JSDocScriptTemplates.getTemplates(servoyProject.getProject(), true).getMethodTemplate();
					calc.setDeclaration(template.getMethodDeclaration(calcName, calcCode, userTemplate));

					ServoyProject servoyActiveProject = servoyModel.getActiveProject();
					if (servoyProject != null)
					{
						servoyActiveProject.saveEditingSolutionNodes(Utils.asArray(solution.getTableNodes(table), IPersist.class), true);
					}
				}

				return calc;
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}

		return null;
	}

	private String validateCalculationName(String name)
	{
		try
		{
			ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(name, null,
				new ValidatorSearchContext(table, IRepository.SCRIPTCALCULATIONS), false);
		}
		catch (RepositoryException e)
		{
			return e.getMessage();
		}
		if (!IdentDocumentValidator.isJavaIdentifier(name))
		{
			return "Invalid calculation name";
		}
		// valid
		return null;
	}

	private String askForCalculationName()
	{
		MethodTemplate template = MethodTemplate.getTemplate(ScriptCalculation.class, methodKey);
		MethodArgument signature = template.getSignature();
		String defaultName = "";
		if (signature != null && signature.getName() != null)
		{
			for (int i = 0; i < 100; i++)
			{
				defaultName = signature.getName();
				if (i > 0) defaultName += i;
				if (validateCalculationName(defaultName) == null)
				{
					break;
				}
			}
		}
		InputDialog askdialog = new InputDialog(getShell(), "New calculation", "Specify a calculation name", defaultName, new IInputValidator()
		{
			public String isValid(String newText)
			{
				if (newText.length() == 0) return "";
				return validateCalculationName(newText);
			}
		});
		askdialog.setBlockOnOpen(true);
		askdialog.open();

		return (askdialog.getReturnCode() == Window.CANCEL) ? null : askdialog.getValue();
	}


	public void setTable(ITable table)
	{
		this.table = table;
		createCalculationButton.setEnabled(table != null);
	}

	public void setPersist(IPersist persist)
	{
		this.persist = persist;
	}

	/**
	 * @param dialog
	 */
	public void setDialog(ScriptProviderCellEditor.ScriptDialog dialog)
	{
		this.dialog = dialog;
	}
}
