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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;


import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.StringInCodeSerializer;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Action to create a new form/global variable depending on the selection of a solution view.
 *
 * @author acostescu
 */
public class NewVariableAction extends Action implements ISelectionChangedListener
{
	private static Object context;
	private final SolutionExplorerView viewer;
	private final ImageDescriptor newGlobalVariableImage;
	private final ImageDescriptor newFormVariableImage;

	/**
	 * Creates a new "create new variable" action for the given solution view.
	 *
	 * @param sev the solution view to use.
	 */
	public NewVariableAction(SolutionExplorerView sev)
	{
		viewer = sev;

		newFormVariableImage = Activator.loadImageDescriptorFromBundle("new_form_variable.png");
		newGlobalVariableImage = Activator.loadImageDescriptorFromBundle("new_global_variable.png");
		setText("Create variable");
		setToolTipText("Create variable");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			state = false;
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.FORM_VARIABLES)
			{
				setImageDescriptor(newFormVariableImage);
				state = true;
			}
			else if (type == UserNodeType.GLOBAL_VARIABLES)
			{
				setImageDescriptor(newGlobalVariableImage);
				state = true;
			}
		}
		setEnabled(state);
	}

	@Override
	public void run()
	{
		SimpleUserNode node = viewer.getSelectedTreeNode();
		if (node != null)
		{
			final IPersist parent;
			String variableScopeType;
			String scopeName = null;
			final Object validationContext;
			if (node.getType() == UserNodeType.FORM_VARIABLES)
			{
				// add form variable
				parent = (Form)node.getRealObject();
				variableScopeType = "form";
				validationContext = parent;
			}
			else if (node.getType() == UserNodeType.GLOBAL_VARIABLES)
			{
				// add global variable
				Pair<Solution, String> pair = (Pair<Solution, String>)node.getRealObject();
				parent = pair.getLeft();
				scopeName = pair.getRight();
				validationContext = scopeName;
				variableScopeType = "global";
			}
			else
			{
				return;
			}

			VariableEditDialog askUserDialog = showVariableEditDialog(viewer.getSite().getShell(), validationContext, variableScopeType);

			String variableName = askUserDialog.getVariableName();
			int variableType = askUserDialog.getVariableType();
			String defaultValue = askUserDialog.getVariableDefaultValue();

			if (variableName != null)
			{
				createNewVariable(viewer.getSite().getShell(), parent, scopeName, variableName, variableType, defaultValue);
			}
		}
	}

	public static VariableEditDialog showVariableEditDialog(Shell shell, final Object validationContext, String variableScopeType)
	{
		context = validationContext;
		VariableEditDialog askUserDialog = new VariableEditDialog(shell, "Create a new " + variableScopeType + " variable", new IInputValidator()
		{
			public String isValid(String newText)
			{
				String message = null;
				if (newText.length() == 0)
				{
					message = "";
				}
				else if (!IdentDocumentValidator.isJavaIdentifier(newText))
				{
					message = "Invalid variable name";
				}
				else
				{
					try
					{
						ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(newText, null,
							new ValidatorSearchContext(context, IRepository.SCRIPTVARIABLES), false);
					}
					catch (RepositoryException e)
					{
						message = e.getMessage();
					}
				}
				return message;
			}
		});
		askUserDialog.open();
		return askUserDialog;
	}

	public static ScriptVariable createNewVariable(Shell shell, IPersist parent, String scopeName, String variableName, int variableType, String defaultValue)
	{
		return createNewVariable(shell, parent, scopeName, variableName, variableType, defaultValue, true);
	}

	public static ScriptVariable createNewVariable(Shell shell, IPersist parent, String scopeName, String variableName, int variableType, String defaultValue,
		boolean showEditor)
	{
		try
		{
			// create the repository object to make sure there are no name conflicts
			ScriptVariable var;
			if (parent instanceof Solution)
			{
				var = ((Solution)parent).createNewScriptVariable(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(),
					scopeName == null ? ScriptVariable.GLOBAL_SCOPE : scopeName, variableName, variableType);
			}
			else
			{
				var = ((Form)parent).createNewScriptVariable(ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), variableName,
					variableType);
			}
			var.setDefaultValue(defaultValue);
			Solution solution = (Solution)parent.getAncestor(IRepository.SOLUTIONS);
			ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
			String userTemplate = JSDocScriptTemplates.getTemplates(servoyProject.getProject(), true).getVariableTemplate();

			String code = SolutionSerializer.serializePersist(var, true, ApplicationServerRegistry.get().getDeveloperRepository(), userTemplate).toString();

			String scriptPath = SolutionSerializer.getScriptPath(var, false);
			IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
			IEditorPart openEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(
				FileEditorInputFactory.createFileEditorInput(file));
			if (openEditor instanceof ScriptEditor)
			{
				// the JS file is being edited - we cannot just modify the file on disk
				boolean wasDirty = openEditor.isDirty();

				// add new variable to the JS editor
				ISourceViewer sv = ((ScriptEditor)openEditor).getScriptSourceViewer();
				StyledText st = sv.getTextWidget();

				st.setCaretOffset(0);
				st.insert(""); // if you have a folded comment for example (folded region) at the beginning of the file
				// trying to alter it will result in a deny and expansion of that area - so in order to make sure content
				// is really added do this insert that either does nothing or expands the collapsed text at char 0...
				st.insert(code + '\n');
				st.setCaretOffset(code.length());
				st.showSelection();
				st.forceFocus();

				if (!wasDirty)
				{
					openEditor.doSave(null);
				}

				st.forceFocus();
				// let the user know the change is added to the already edited
				// file (he will not see it in the solution explorer)
				if (showEditor) openEditor.getSite().getPage().activate(openEditor);
			}
			else
			{
				InputStream contents = null;
				ByteArrayOutputStream baos = null;
				try
				{
					if (file.exists())
					{
						contents = new BufferedInputStream(file.getContents(true));
					}
					baos = new ByteArrayOutputStream();
					baos.write(code.getBytes("UTF8"));
					baos.write("\n".getBytes("UTF8"));
					Utils.streamCopy(contents, baos);
					new WorkspaceFileAccess(ServoyModel.getWorkspace()).setContents(scriptPath, baos.toByteArray()); // will also create file and parent folders if needed
				}
				finally
				{
					Utils.closeInputStream(contents);
					Utils.closeOutputStream(baos);
				}
				if (showEditor) EditorUtil.openScriptEditor(var, null, true);
			}
			return var;
		}
		catch (RepositoryException e)
		{
			MessageDialog.openWarning(shell, "Cannot create the new " + variableType + " variable", "Reason: " + e.getMessage());
			ServoyLog.logWarning("Cannot create variable", e);
		}
		catch (CoreException e)
		{
			MessageDialog.openWarning(shell, "Cannot create the JS file for new " + variableType + " variable", "Reason: " + e.getMessage());
			ServoyLog.logWarning("Cannot create variable", e);
		}
		catch (IOException e)
		{
			MessageDialog.openWarning(shell, "Cannot modify the JS file for new " + variableType + " variable", "Reason: " + e.getMessage());
			ServoyLog.logWarning("Cannot create variable", e);
		}
		return null;
	}

	/**
	 * This is a dialog that allows you to specify a name/type/default value for a variable.
	 *
	 * @author acostescu
	 */
	public static class VariableEditDialog extends Dialog
	{
		private static final int JS_EXPRESSION = 0;
		private static final int VALUE = 1;
		private final String title;
		private Text name;
		private Text defaultValue;
		private Combo type;
		private final IInputValidator inputValidator;
		private Label warningLabel;
		private String dv;
		private int tv;
		private String nv;
		private String initialName = null;
		private int initialType = -1;
		private String initialDefaultValue = "null";
		private String quotationSign = null;
		private int defaultValueType = JS_EXPRESSION;
		private Button radioExactExpression;
		private Button radioValue;

		private Combo parentCombo;
		private String pc;


		/**
		 * Creates a new Dialog for new variable name, type and default value.
		 *
		 * @param parentShell the parent shell for the dialog.
		 * @param title the title of the dialog.
		 * @param inputValidator
		 */
		protected VariableEditDialog(Shell parentShell, String title, IInputValidator inputValidator)
		{
			super(parentShell);
			this.title = title;
			this.inputValidator = inputValidator;
		}

		/**
		 * Creates a new Dialog for editing a variable name, type and default value.
		 *
		 * @param parentShell the parent shell for the dialog.
		 * @param title the title of the dialog.
		 * @param inputValidator
		 */
		protected VariableEditDialog(Shell parentShell, String title, IInputValidator inputValidator, String initialName, int initialType,
			String initialDefaultValue)
		{
			super(parentShell);
			this.title = title;
			this.inputValidator = inputValidator;
			this.initialName = initialName;
			this.initialType = initialType;
			this.initialDefaultValue = initialDefaultValue;
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite mainArea = (Composite)super.createDialogArea(parent);
			GridLayout layout = (GridLayout)mainArea.getLayout();

			layout.numColumns = 4;
			Label nameLabel = new Label(mainArea, SWT.NONE);
			name = new Text(mainArea, SWT.BORDER);
			Label typeLabel = new Label(mainArea, SWT.NONE);
			type = new Combo(mainArea, SWT.DROP_DOWN | SWT.READ_ONLY);
			UIUtils.setDefaultVisibleItemCount(type);

			if (context instanceof Form frm)
			{
				List<AbstractBase> forms = PersistHelper.getOverrideHierarchy(frm);
				if (forms.size() >= 2)
				{
					Label parentComboLabel = new Label(mainArea, SWT.NONE);
					parentComboLabel.setText("Parent");
					parentCombo = new Combo(mainArea, SWT.DROP_DOWN | SWT.READ_ONLY);

					for (AbstractBase form : forms)
					{
						parentCombo.add(form.toString());
					}

					parentCombo.select(0);

					parentCombo.addSelectionListener(new SelectionAdapter()
					{
						@Override
						public void widgetSelected(SelectionEvent event)
						{
							context = ModelUtils.getEditingFlattenedSolution((IPersist)context).getForm(parentCombo.getText());
							validateDialogData();
						}
					});
				}
			}

			Group defaultValueRadioGroup = new Group(mainArea, SWT.NONE);
			defaultValueRadioGroup.setText("Default value");
			GridLayout gridLayout = new GridLayout();
			gridLayout.horizontalSpacing = 8;
			gridLayout.numColumns = 2;
			defaultValueRadioGroup.setLayout(gridLayout);

			defaultValue = new Text(defaultValueRadioGroup, SWT.BORDER | SWT.MULTI);

			radioExactExpression = new Button(defaultValueRadioGroup, SWT.RADIO | SWT.LEFT);
			radioExactExpression.setText("js expression");
			radioExactExpression.setData(new Integer(JS_EXPRESSION));

			radioValue = new Button(defaultValueRadioGroup, SWT.RADIO | SWT.LEFT);
			radioValue.setText("value");
			radioValue.setData(new Integer(VALUE));

			SelectionAdapter sl = new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent event)
				{
					setDefaultValueType((Integer)event.widget.getData());
				}

			};
			radioExactExpression.addSelectionListener(sl);
			radioValue.addSelectionListener(sl);

			warningLabel = new Label(mainArea, SWT.NONE);
			Color red = new Color(null, 255, 0, 0);
			warningLabel.setForeground(red);
			red.dispose();

			nameLabel.setText("Name");
			typeLabel.setText("Type");

			GridData data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = 150;
			name.setLayoutData(data);

			data = new GridData(GridData.FILL_HORIZONTAL);
			data.widthHint = 75;
			type.setLayoutData(data);

			data = new GridData(GridData.FILL_BOTH);
			data.verticalSpan = 2;
			data.heightHint = 2 * defaultValue.getLineHeight();
			data.widthHint = 300;
			defaultValue.setLayoutData(data);

//			data = new GridData(GridData.FILL_HORIZONTAL);
//			data.horizontalAlignment = SWT.END;
//			radioExactExpression.setLayoutData(data);

			data = new GridData(GridData.FILL_BOTH);
			data.horizontalSpan = 4;
			defaultValueRadioGroup.setLayoutData(data);

			data = new GridData(GridData.FILL_HORIZONTAL);
			data.horizontalSpan = 4;
			warningLabel.setLayoutData(data);

			getShell().setText(title);
			setupTypeCombo();

			return mainArea;
		}

		@Override
		protected Control createContents(Composite parent)
		{
			Control c = super.createContents(parent);
			setupValidationBehavior();
			setupInitialValues();
			return c;
		}

		private void setupInitialValues()
		{
			if (initialName != null) name.setText(initialName);
			if (initialType != -1) type.setText(Column.getDisplayTypeString(initialType));

			int type = JS_EXPRESSION;
			if (initialDefaultValue != null)
			{
				if (!"null".equals(initialDefaultValue))
				{
					if (initialType == IColumnTypes.TEXT)
					{
						if ((initialDefaultValue.startsWith("'") && initialDefaultValue.endsWith("'")) ||
							(initialDefaultValue.startsWith("\"") && initialDefaultValue.endsWith("\"")))
						{
							// remove ' or " surrounding the default value
							type = VALUE;
							quotationSign = initialDefaultValue.substring(0, 1);
							this.initialDefaultValue = StringInCodeSerializer.getMemoryString(initialDefaultValue);
						}
					}
					else if (initialType == IColumnTypes.NUMBER)
					{
						if (validateNumber(initialDefaultValue) == null)
						{
							type = VALUE;
						}
					}
					else if (initialType == IColumnTypes.INTEGER)
					{
						if (validateInt(initialDefaultValue) == null)
						{
							type = VALUE;
						}
					}
				}
			}
			else
			{
				initialDefaultValue = "";
			}

			defaultValue.setText(initialDefaultValue);
			setDefaultValueType(type);
			if (type == JS_EXPRESSION)
			{
				radioExactExpression.setSelection(true);
			}
			else
			{
				radioValue.setSelection(true);
			}
		}

		private void setupValidationBehavior()
		{
			ModifyListener listener = new ModifyListener()
			{
				public void modifyText(ModifyEvent modifyevent)
				{
					validateDialogData();
				}
			};
			name.addModifyListener(listener);
			type.addModifyListener(listener);
			defaultValue.addModifyListener(listener);
		}

		private void validateDialogData()
		{
			String result = inputValidator.isValid(name.getText());

			if (result == null && defaultValueType == VALUE)
			{
				// validate default value
				int varType = Column.allDefinedTypes[type.getSelectionIndex()];
				String varDefaultValue = defaultValue.getText();
				if (varDefaultValue.length() > 0)
				{
					if (varType == IColumnTypes.INTEGER)
					{
						result = validateInt(varDefaultValue);
					}
					else if (varType == IColumnTypes.NUMBER)
					{
						result = validateNumber(varDefaultValue);
					}
				}
				else if ((varType == IColumnTypes.NUMBER) || (varType == IColumnTypes.INTEGER))
				{
					result = "Please specify a default value";
				}
			}
			if (result != null)
			{
				getButton(IDialogConstants.OK_ID).setEnabled(false);
				warningLabel.setText(result);
			}
			else
			{
				getButton(IDialogConstants.OK_ID).setEnabled(true);
				warningLabel.setText("");
			}
		}

		private String validateNumber(String varDefaultValue)
		{
			try
			{
				Double.parseDouble(varDefaultValue);
			}
			catch (NumberFormatException e)
			{
				return "The default value is not a valid number.";
			}
			return null;
		}

		private String validateInt(String varDefaultValue)
		{
			try
			{
				Integer.parseInt(varDefaultValue);
			}
			catch (NumberFormatException e)
			{
				return "The default value is not a valid integer.";
			}
			return null;
		}

		private void setupTypeCombo()
		{
			int[] iTypes = Column.allDefinedTypes;
			String[] types = new String[iTypes.length];
			for (int i = 0; i < iTypes.length; i++)
			{
				types[i] = Column.getDisplayTypeString(iTypes[i]);
			}
			type.setItems(types);
			type.select(0);
		}

		private void setDefaultValueType(int type)
		{
			defaultValueType = type;
			validateDialogData();
		}

		@Override
		protected void okPressed()
		{
			nv = name.getText();
			tv = Column.allDefinedTypes[type.getSelectionIndex()];
			dv = defaultValue.getText();
			if (parentCombo != null)
			{
				pc = parentCombo.getText();
			}
			super.okPressed();
		}

		/**
		 * Returns the name of the variable as given by the user or null if the user canceled the dialog.
		 *
		 * @return the name of the variable as given by the user or null if the user canceled the dialog.
		 */
		public String getVariableName()
		{
			return nv;
		}

		/**
		 * Returns the default value of the variable as given by the user.
		 *
		 * @return the default value of the variable as given by the user.
		 */
		public String getVariableDefaultValue()
		{
			if (defaultValueType == JS_EXPRESSION)
			{
				return ("".equals(dv)) ? null : dv;
			}

			if (tv == IColumnTypes.TEXT)
			{
				if (quotationSign == null) quotationSign = "'";
				{
					return StringInCodeSerializer.getSourceCodeString(dv, quotationSign);
				}
			}
			return dv;
		}

		/**
		 * Returns the type of the variable as given by the user.
		 *
		 * @return the type of the variable as given by the user.
		 */
		public int getVariableType()
		{
			return tv;
		}

		/**
		 * Returns the parent of the variable as given by the user.
		 *
		 * @return the parent of the variable as given by the user.
		 */
		public String getParentName()
		{
			return pc;
		}

	}

}