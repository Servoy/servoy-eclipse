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
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IScriptProject;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.PreferencesLookupDelegate;
import org.eclipse.dltk.internal.ui.editor.ScriptEditor;
import org.eclipse.dltk.javascript.formatter.JavaScriptFormatter;
import org.eclipse.dltk.ui.formatter.IScriptFormatter;
import org.eclipse.dltk.ui.formatter.IScriptFormatterFactory;
import org.eclipse.dltk.ui.formatter.ScriptFormatterManager;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.Document;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.InputAndListDialog;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Action to create a new form/global method depending on the selection of a solution view.
 * 
 * @author acostescu
 */
public class NewMethodAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;
	private final ImageDescriptor newFormMethodImage;
	private final ImageDescriptor newGlobalMethodImage;


	/**
	 * Creates a new "create new method" action for the given solution view.
	 * 
	 * @param sev the solution view to use.
	 */
	public NewMethodAction(SolutionExplorerView sev)
	{
		viewer = sev;

		newFormMethodImage = Activator.loadImageDescriptorFromBundle("new_form_method.gif"); //$NON-NLS-1$
		newGlobalMethodImage = Activator.loadImageDescriptorFromBundle("new_global_method.gif"); //$NON-NLS-1$
		setText("Create method"); //$NON-NLS-1$
		setToolTipText("Create method"); //$NON-NLS-1$
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() == 1);
		if (state)
		{
			state = false;
			UserNodeType type = ((SimpleUserNode)sel.getFirstElement()).getType();
			if (type == UserNodeType.FORM)
			{
				setImageDescriptor(newFormMethodImage);
				state = true;
			}
			else if (type == UserNodeType.GLOBALS_ITEM)
			{
				setImageDescriptor(newGlobalMethodImage);
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
			if (node.getType() == UserNodeType.FORM)
			{
				// add form method
				createNewMethod(viewer.getSite().getShell(), (Form)node.getRealObject(), null, true, null, null);
			}
			else if (node.getType() == UserNodeType.GLOBALS_ITEM)
			{
				Pair<Solution, String> pair = (Pair<Solution, String>)node.getRealObject();
				// add global method
				createNewMethod(viewer.getSite().getShell(), pair.getLeft(), null, true, null, pair.getRight(), null, null);
			}
		}
	}

	public static ScriptMethod createNewMethod(Shell shell, IPersist parent, String methodKey, boolean activate, String forcedMethodName, String forcedScopeName)
	{
		return createNewMethod(shell, parent, methodKey, activate, forcedMethodName, forcedScopeName, null, null);
	}

	public static ScriptMethod createNewMethod(final Shell shell, IPersist parent, String methodKey, boolean activate, String forcedMethodName,
		String forcedScopeName, Map<String, String> substitutions, IPersist persist)
	{
		String methodType;
		int tagFilter;
		String[] listOptions = null;
		String scopeName = forcedScopeName == null ? ScriptVariable.GLOBAL_SCOPE : forcedScopeName;
		String listDescriptionText = null;
		if (parent instanceof Form)
		{
			methodType = "form";
			tagFilter = MethodTemplate.ALL_TAGS; // form method can only be created from form context
		}
		else if (parent instanceof Solution)
		{
			methodType = "global";
			if (persist instanceof Solution || persist instanceof ValueList /* global method vl */)
			{
				tagFilter = MethodTemplate.PRIVATE_TAG | MethodTemplate.PUBLIC_TAG; // protected is n/a
			}
			else
			{
				tagFilter = MethodTemplate.PUBLIC_TAG; // no private methods for events from outside globals
			}
			if (forcedScopeName == null)
			{
				// let user select from scopes.
				Collection<Pair<String, IRootObject>> scopes = ModelUtils.getEditingFlattenedSolution(parent).getScopes();
				if (scopes.size() == 0)
				{
					listOptions = new String[] { ScriptVariable.GLOBAL_SCOPE };
				}
				else
				{
					listOptions = new String[scopes.size()];
					int i = 0;
					for (Pair<String, IRootObject> scope : scopes)
					{
						listOptions[i++] = scope.getLeft();
					}
				}
			}

			listDescriptionText = "Select scope";
		}
		else if (parent instanceof TableNode)
		{
			methodType = "entity";
			if (persist instanceof TableNode)
			{
				tagFilter = MethodTemplate.PRIVATE_TAG | MethodTemplate.PUBLIC_TAG; // protected is n/a
			}
			else
			{
				tagFilter = MethodTemplate.PUBLIC_TAG; // no private methods for events from outside table node
			}
		}
		else
		{
			return null;
		}
		ServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();

		boolean override = false;
		MethodArgument[] superArguments = null;
		String methodName = forcedMethodName;
		int tagToOutput = MethodTemplate.PUBLIC_TAG;
		if (methodName == null)
		{
			Pair<Pair<String, String>, Integer> methodNameAndPrivateFlag = askForMethodName(methodType, parent, methodKey, shell, tagFilter, listOptions,
				scopeName, listDescriptionText, persist);
			if (methodNameAndPrivateFlag != null)
			{
				scopeName = methodNameAndPrivateFlag.getLeft().getLeft();
				methodName = methodNameAndPrivateFlag.getLeft().getRight();
				tagToOutput = methodNameAndPrivateFlag.getRight().intValue();
			}
		}

		if (methodName != null)
		{
			// create the method as a repository object - to check for duplicates
			// and things like this
			try
			{
				ScriptMethod met = null;
				if (parent instanceof Solution)
				{
					// global method
					met = ((Solution)parent).createNewGlobalScriptMethod(sm.getNameValidator(), scopeName, methodName);
				}
				else if (parent instanceof Form)
				{
					Form form = (Form)parent;
					if (form.getExtendsID() > 0)
					{
						List<Form> formHierarchy = sm.getActiveProject().getEditingFlattenedSolution().getFormHierarchy(form);
						for (Form f : formHierarchy)
						{
							if (f != form)
							{
								ScriptMethod superMethod = f.getScriptMethod(methodName);
								if (superMethod != null)
								{
									if (forcedMethodName == null)
									{
										MessageDialog dialog = new MessageDialog(shell, "Method already exists in the super form " + f.getName(), null, //$NON-NLS-1$
											"Are you sure you want to override that forms method?", MessageDialog.QUESTION, //$NON-NLS-1$
											new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
										int returnCode = dialog.open();
										if (returnCode > 0)
										{
											return null;
										}
									}
									override = true;
									superArguments = superMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
								}
							}
						}
					}
					met = ((Form)parent).createNewScriptMethod(sm.getNameValidator(), methodName);
				}
				else if (parent instanceof TableNode)
				{
					met = ((TableNode)parent).createNewFoundsetMethod(sm.getNameValidator(), methodName);
				}

				if (met != null)
				{
					MethodTemplate template;
					if (override)
					{
						template = MethodTemplate.getFormMethodOverrideTemplate(met.getClass(), methodKey, superArguments);
					}
					else
					{
						template = MethodTemplate.getTemplate(met.getClass(), methodKey);
					}
					String scriptPath = SolutionSerializer.getScriptPath(met, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					// if the file isn't there, create it here so that the formatter sees the js file.
					if (!file.exists())
					{
						// file doesn't exist, create the file and its parent directories
						new WorkspaceFileAccess(ServoyModel.getWorkspace()).setContents(scriptPath, new byte[0]);
					}

					String declaration = template.getMethodDeclaration(met.getName(), null, tagToOutput, substitutions);

					declaration = format(declaration, file, 0);

					met.setDeclaration(declaration);

					String code = SolutionSerializer.serializePersist(met, true, ServoyModel.getDeveloperRepository()).toString();
					((ISupportChilds)parent).removeChild(met);

					final IEditorPart openEditor = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().findEditor(
						FileEditorInputFactory.createFileEditorInput(file));
					if (openEditor instanceof ScriptEditor)
					{
						// the JS file is being edited - we cannot just modify the file
						// on disk
						boolean wasDirty = openEditor.isDirty();

						// add new method to the JS editor
						ScriptEditor scriptEditor = ((ScriptEditor)openEditor);
						ISourceViewer sv = scriptEditor.getScriptSourceViewer();
						StyledText st = sv.getTextWidget();

						st.append("\n"); // for some reason this must be separately //$NON-NLS-1$
						// added
						st.append(code);
						st.setCaretOffset(st.getText().lastIndexOf('}') - 1);
						st.showSelection();

						if (!wasDirty)
						{
							openEditor.doSave(null);
						}

						if (activate || wasDirty)
						{
							st.forceFocus();
							// let the user know the change is added to the already edited
							// file (he will not see it in the solution explorer)
							openEditor.getSite().getPage().activate(openEditor);

							if (wasDirty)
							{
								// let user now that he will not see the new method or be able to select it until editor is saved
								Runnable r = new Runnable()
								{

									public void run()
									{
										if (MessageDialog.openQuestion(shell,
											"Javascript editor not saved", //$NON-NLS-1$
											"The javascript editor for this form is open and dirty.\nThe new method has been appended to it, but\nyou have to save it in order to be able to select/see the new method in Solution Explorer and other places.\n\nDo you want to save the editor?")) //$NON-NLS-1$
										{
											openEditor.doSave(null);
										}
									}

								};
								Display d = Display.getCurrent();
								if (d != null)
								{
									r.run();
								}
								else
								{
									d = (shell != null) ? shell.getDisplay() : Display.getDefault();
									d.asyncExec(r);
								}
							}
						}
					}
					else
					{
						// no JS editor is open for the script file - so simply modify
						// the file on disk
						// TODO check if file.appendContent isn't better..
						InputStream contents = null;
						ByteArrayOutputStream baos = null;
						ByteArrayInputStream bais = null;
						try
						{
							contents = new BufferedInputStream(file.getContents(true));
							baos = new ByteArrayOutputStream();
							Utils.streamCopy(contents, baos);
							baos.write("\n".getBytes("UTF8")); //$NON-NLS-1$ //$NON-NLS-2$
							baos.write(code.getBytes("UTF8")); //$NON-NLS-1$
							baos.close();
							bais = new ByteArrayInputStream(baos.toByteArray());
							file.setContents(bais, true, true, null);
						}
						finally
						{
							if (contents != null)
							{
								contents.close();
							}
							if (baos != null)
							{
								baos.close();
							}
							if (bais != null)
							{
								bais.close();
							}
						}

						if (activate)
						{
							EditorUtil.openScriptEditor(met, null, true);
						}
					}

				}
				return met;
			}
			catch (RepositoryException e)
			{
				MessageDialog.openWarning(shell, "Cannot create the new " + methodType + " method", "Reason: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				ServoyLog.logWarning("Cannot create method", e); //$NON-NLS-1$
			}
			catch (CoreException e)
			{
				MessageDialog.openWarning(shell, "Cannot create the JS file for new " + methodType + " method", "Reason: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				ServoyLog.logWarning("Cannot create method", e); //$NON-NLS-1$ 
			}
			catch (IOException e)
			{
				MessageDialog.openWarning(shell, "Cannot modify JS file contents for new " + methodType + " method", "Reason: " + e.getMessage()); //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
				ServoyLog.logWarning("Cannot create method", e); //$NON-NLS-1$ 
			}
		}
		return null;
	}

	public static String format(String methodCode, IFile file, int indent)
	{
		if (file != null && file.getType() == IResource.FILE && file.exists())
		{
			final ISourceModule module = DLTKCore.createSourceModuleFrom(file);
			if (module != null)
			{
				final IScriptProject project = module.getScriptProject();
				final IScriptFormatterFactory formatterFactory = ScriptFormatterManager.getSelected(project);
				if (formatterFactory != null)
				{
					try
					{
						final String source = module.getSource();
						final Document document = new Document(source);
						final String lineDelimiter = TextUtilities.getDefaultLineDelimiter(document);
						final Map<String, String> preferences = formatterFactory.retrievePreferences(new PreferencesLookupDelegate(project));
						final IScriptFormatter formatter = formatterFactory.createFormatter(lineDelimiter, preferences);
						if (formatter instanceof JavaScriptFormatter)
						{
							return ((JavaScriptFormatter)formatter).format(methodCode, formatter.detectIndentationLevel(document, indent));
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
		return methodCode;
	}

	private static String validateMethodName(final IPersist parent, String newText)
	{
		IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
		try
		{
			if (parent instanceof Solution)
			{
				// global method
				validator.checkName(newText, 0, new ValidatorSearchContext(IRepository.METHODS), false);
			}

			if (parent instanceof Form)
			{
				// form method
				validator.checkName(newText, 0, new ValidatorSearchContext(parent, IRepository.METHODS), false);
			}
		}
		catch (RepositoryException e)
		{
			return e.getMessage();
		}

		if (!IdentDocumentValidator.isJavaIdentifier(newText))
		{
			return "Invalid method name"; //$NON-NLS-1$
		}
		// valid
		return null;
	}

	@SuppressWarnings("nls")
	private static Pair<Pair<String, String>, Integer> askForMethodName(String methodType, final IPersist parent, String methodKey, Shell shell, int tagFilter,
		String[] listOptions, String listValue, String listDescriptionText, IPersist persist)
	{
		String defaultName = ""; //$NON-NLS-1$
		if (methodKey != null)
		{
			MethodTemplate template = MethodTemplate.getTemplate(ScriptMethod.class, methodKey);
			MethodArgument signature = template.getSignature();
			String name;
			if (signature == null || signature.getName() == null)
			{
				// not a servoy method key, probably a bean property
				String[] split = methodKey.split("\\."); //$NON-NLS-1$
				name = split[split.length - 1];
			}
			else
			{
				name = signature.getName();
			}

			//only form and global methods (event handlers on form elements and tables)
			if (parent instanceof Form || parent instanceof Solution)
			{
				if (persist instanceof TableNode) name = getTableEventHandlerName(name, persist);
				else if (!(persist instanceof Solution) && !(persist instanceof Form)) name = getFormEventHandlerName(name, persist);
			}

			for (int i = 0; i < 100; i++)
			{
				defaultName = name;
				if (i > 0) defaultName += i;
				if (validateMethodName(parent, defaultName) == null)
				{
					break;
				}
			}
		}
		NewMethodInputDialog dialog = new NewMethodInputDialog(shell, "New " + methodType + " method", "Specify a method name", defaultName, parent, tagFilter,
			listOptions, listValue, listDescriptionText);
		dialog.setBlockOnOpen(true);
		dialog.open();

		return (dialog.getReturnCode() == Window.CANCEL) ? null : new Pair<Pair<String, String>, Integer>(new Pair<String, String>(dialog.getExtendedValue(),
			dialog.getValue()), Integer.valueOf(dialog.tagToOutput));
	}

	private static String makePrettyName(String simpleMethodName, String elementName)
	{
		if (elementName == null) return simpleMethodName;
		String modifiedElemName = elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
		//include in method name ("on ... Action ")
		return new StringBuffer(simpleMethodName).insert(2, modifiedElemName).toString();
	}

	private static String getTableEventHandlerName(String simpleName, IPersist persist)
	{
		if (persist == null) return simpleName;
		if (new DesignerPreferences().getIncludeTableName()) return makePrettyName(simpleName, ((TableNode)persist).getTableName());
		else return simpleName;
	}

	private static String getFormEventHandlerName(String simpleName, IPersist persist)
	{
		if (persist == null) return simpleName;

		DesignerPreferences dp = new DesignerPreferences();
		if (dp.getDefaultFormEventHandlerNaming()) return simpleName;
		else
		{
			String prettyName = simpleName;
			if (dp.getIncludeFormElementDataProviderName() || dp.getIncludeFormElementDataProviderNameWithFallback())
			{
				if (persist instanceof ISupportDataProviderID)
				{
					String dataProvider = ((ISupportDataProviderID)persist).getDataProviderID();
					if (dataProvider != null) return makePrettyName(prettyName, dataProvider);
				}
			}
			// does not support dataprovider OR dataprovider is null
			if (dp.getIncludeFormElementName() || dp.getIncludeFormElementDataProviderNameWithFallback())
			{
				if (persist instanceof ISupportName) prettyName = makePrettyName(prettyName, ((ISupportName)persist).getName());
			}
			return prettyName;
		}
	}

	/**
	 * @author jcompagner
	 *
	 */
	private static final class NewMethodInputDialog extends InputAndListDialog
	{
		private Button okButton;
		private int tagToOutput;
		private Button createPrivateButton;
		private Button createProtectedButton;
		private final int tagFilter;

		private NewMethodInputDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialValue, final IPersist parent, int tagFilter,
			String[] listOptions, String listValue, String listDescriptionText)
		{
			super(parentShell, dialogTitle, dialogMessage, initialValue, new IInputValidator()
			{
				public String isValid(String newText)
				{
					if (newText.length() == 0) return ""; //$NON-NLS-1$
					return validateMethodName(parent, newText);
				}
			}, listOptions, listValue, listDescriptionText);
			this.tagFilter = tagFilter;
		}

		@SuppressWarnings("nls")
		@Override
		protected void createButtonsForButtonBar(Composite compositeParent)
		{
			// create OK and Cancel buttons by default
			if (tagFilter == MethodTemplate.PUBLIC_TAG || tagFilter == 0)
			{
				okButton = createButton(compositeParent, IDialogConstants.OK_ID, "Create", true);
			}
			else
			{
				if ((tagFilter & MethodTemplate.PUBLIC_TAG) != 0)
				{
					okButton = createButton(compositeParent, IDialogConstants.OK_ID, "Create public", true);
				}
				if ((tagFilter & MethodTemplate.PRIVATE_TAG) != 0)
				{
					createPrivateButton = createButton(compositeParent, IDialogConstants.PROCEED_ID, "Create private", false);
				}
				if ((tagFilter & MethodTemplate.PROTECTED_TAG) != 0)
				{
					createProtectedButton = createButton(compositeParent, IDialogConstants.IGNORE_ID, "Create protected", false);
				}
			}
			createButton(compositeParent, IDialogConstants.CANCEL_ID, IDialogConstants.CANCEL_LABEL, false);
			//do this here because setting the text will set enablement on the ok
			// button
			Text text = getText();
			text.setFocus();
			if (getValue() != null)
			{
				text.setText(getValue());
				text.selectAll();
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.InputDialog#getOkButton()
		 */
		@Override
		protected Button getOkButton()
		{
			return okButton;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.InputDialog#buttonPressed(int)
		 */
		@Override
		protected void buttonPressed(int buttonId)
		{
			if (buttonId == IDialogConstants.PROCEED_ID || buttonId == IDialogConstants.IGNORE_ID)
			{
				tagToOutput = buttonId == IDialogConstants.PROCEED_ID ? MethodTemplate.PRIVATE_TAG : MethodTemplate.PROTECTED_TAG;
				super.buttonPressed(IDialogConstants.OK_ID);
			}
			else
			{
				tagToOutput = MethodTemplate.PUBLIC_TAG;
				super.buttonPressed(buttonId);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.dialogs.InputDialog#setErrorMessage(java.lang.String)
		 */
		@Override
		public void setErrorMessage(String errorMessage)
		{
			super.setErrorMessage(errorMessage);
			if (createPrivateButton != null) createPrivateButton.setEnabled(errorMessage == null);
			if (createProtectedButton != null) createProtectedButton.setEnabled(errorMessage == null);
		}
	}


}
