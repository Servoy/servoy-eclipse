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
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils.InputAndListDialog;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.preferences.JSDocScriptTemplates;
import com.servoy.eclipse.model.repository.EclipseRepository;
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
import com.servoy.j2db.persistence.ArgumentType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.MethodTemplate;
import com.servoy.j2db.persistence.MethodTemplatesFactory;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.info.EventType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
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

		newFormMethodImage = Activator.loadImageDescriptorFromBundle("new_form_method.png");
		newGlobalMethodImage = Activator.loadImageDescriptorFromBundle("new_global_method.png");
		setText("Create method");
		setToolTipText("Create method");
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
				if (((SimpleUserNode)sel.getFirstElement()).getRealObject() instanceof Form &&
					Utils.getAsBoolean(((Form)(((SimpleUserNode)sel.getFirstElement()).getRealObject())).isFormComponent()))
				{
					state = false;
				}
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
				createNewMethod(viewer.getSite().getShell(), (Form)node.getRealObject(), null, true, null, null, null);
			}
			else if (node.getType() == UserNodeType.GLOBALS_ITEM)
			{
				Pair<Solution, String> pair = (Pair<Solution, String>)node.getRealObject();
				// add global method
				createNewMethod(viewer.getSite().getShell(), pair.getLeft(), null, true, null, pair.getRight(), null, null, null);
			}
		}
	}

	public static ScriptMethod createNewMethod(Shell shell, IPersist parent, String methodKey, boolean activate, String forcedMethodName,
		String forcedScopeName, MethodArgument forcedReturnType)
	{
		return createNewMethod(shell, parent, methodKey, activate, forcedMethodName, forcedScopeName, null, null, forcedReturnType);
	}

	public static ScriptMethod createNewMethod(final Shell shell, IPersist parent, String methodKey, boolean activate, String forcedMethodName,
		String forcedScopeName, Map<String, String> substitutions, IPersist persist, MethodArgument forcedReturnType)
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
			methodType = "[" + ((Solution)parent).getName() + "] globals scope";
			tagFilter = MethodTemplate.PUBLIC_TAG; // protected is n/a, private methods are local to the scope js file
			if (forcedScopeName == null)
			{
				// let user select from scopes.
				Collection<String> scopeNames = ModelUtils.getEditingFlattenedSolution(parent).getScopeNames();
				listOptions = scopeNames.toArray(new String[scopeNames.size()]);
			}
			else
			{
				methodType = "[" + ((Solution)parent).getName() + "] " + forcedScopeName + " scope";
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
		IDeveloperServoyModel sm = ServoyModelManager.getServoyModelManager().getServoyModel();

		boolean override = false;
		MethodArgument[] superArguments = null;
		String methodName = forcedMethodName;
		int tagToOutput = MethodTemplate.PUBLIC_TAG;
		if (methodName == null)
		{
			Pair<Pair<String, String>, Integer> methodNameAndPrivateFlag = askForMethodName(methodType, parent, methodKey, shell, tagFilter, listOptions,
				scopeName, listDescriptionText, persist, scopeName);
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
					met = ((Solution)parent).createNewGlobalScriptMethod(getValidator(parent), scopeName, methodName);
				}
				else if (parent instanceof Form)
				{
					Form form = (Form)parent;
					if (form.getExtendsID() != null)
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
										MessageDialog dialog = new MessageDialog(shell, "Method already exists in the super form " + f.getName(), null,
											"Are you sure you want to override that forms method?", MessageDialog.QUESTION,
											new String[] { IDialogConstants.YES_LABEL, IDialogConstants.NO_LABEL }, 0);
										int returnCode = dialog.open();
										if (returnCode > 0)
										{
											return null;
										}
									}
									override = true;
									superArguments = superMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
									break;
								}
							}
						}
					}
					met = ((Form)parent).createNewScriptMethod(sm.getNameValidator(), methodName);
				}
				else if (parent instanceof TableNode)
				{
					met = ((TableNode)parent).createNewFoundsetMethod(sm.getNameValidator(), methodName, null);
				}

				if (met != null)
				{
					MethodTemplate template = null;
					if (override)
					{
						template = MethodTemplate.getFormMethodOverrideTemplate(met.getClass(), methodKey, superArguments, forcedReturnType);
					}
					else
					{
						if (persist instanceof IBasicWebObject)
						{
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
								((IBasicWebObject)persist).getTypeName());
							WebObjectFunctionDefinition handler;
							if (spec != null && (handler = spec.getHandler(methodKey)) != null)
							{
								template = MethodTemplate.DEFAULT_TEMPLATE;
								PropertyDescription handlerLowLevelDef = handler.getAsPropertyDescription();
								if (handlerLowLevelDef != null && handlerLowLevelDef.getConfig() instanceof JSONObject)
								{
									JSONObject config = (JSONObject)handlerLowLevelDef.getConfig();
									ArgumentType returnType = null;
									String defaultMethodCode = config.optString("code", "");
									String returnTypeDescription = "";
									boolean hasDefaultValue = false;
									Object defaultValue = null;
									if (config.has("returns"))
									{
										PropertyDescription returnTypeUnprocessed = handler.getReturnType();
										if (config.get("returns") instanceof JSONObject)
										{
											JSONObject returns = config.getJSONObject("returns");
											returnType = ArgumentType.valueOf(returns.optString("type", ""));
											returnTypeDescription = returns.optString(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
												returns.optString("description", ""));
											if (returns.has("default"))
											{
												hasDefaultValue = true;
												defaultValue = returns.opt("default");
											}
										}
										else
										{
											returnType = ArgumentType.valueOf(config.getString("returns"));
										}

										if (!hasDefaultValue)
										{
											if (returnTypeUnprocessed != null && returnTypeUnprocessed.getType() != null && defaultMethodCode.equals(""))
											{
												hasDefaultValue = true;
												defaultValue = returnTypeUnprocessed.getType().defaultValue(returnTypeUnprocessed);
											}
										}
										if (hasDefaultValue)
										{
											defaultMethodCode = "return " + defaultValue + ";";
										}
									}
									List<MethodArgument> arguments = new ArrayList<MethodArgument>();
									if (config.has("parameters") && config.get("parameters") instanceof JSONArray)
									{
										JSONArray parameters = config.getJSONArray("parameters");
										for (int i = 0; i < parameters.length(); i++)
										{
											JSONObject parameter = parameters.getJSONObject(i);
											String argumentType = parameter.optString("type");
											boolean isArray = false;
											if (argumentType.endsWith("[]"))
											{
												argumentType = argumentType.substring(0, argumentType.length() - 2);
												isArray = true;
											}
											if ("int".equals(argumentType))
											{
												argumentType = "number";
											}
											if ("record".equals(argumentType))
											{
												argumentType = "JSRecord";
											}
											if (spec.getDeclaredCustomObjectTypes().containsKey(argumentType))
											{
												argumentType = "CustomType<" + spec.getName() + "." + argumentType + ">";
											}
											if (isArray)
											{
												argumentType = "Array<" + argumentType + ">";
											}
											arguments.add(new MethodArgument(parameter.optString("name"), ArgumentType.valueOf(argumentType),
												parameter.optString(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
													parameter.optString("description", "")),
												parameter.optBoolean("optional", false)));
										}
									}
									template = (MethodTemplate)MethodTemplatesFactory.getInstance().createMethodTemplate(methodKey,
										config.optString(PropertyDescription.DOCUMENTATION_TAG_FOR_PROP_OR_KEY_FOR_HANDLERS,
											config.optString("description", "")),
										returnType, returnTypeDescription,
										arguments.toArray(new MethodArgument[arguments.size()]), defaultMethodCode, true);
								}
							}
						}
						if (template == null)
						{
							EventType eventType = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(parent)
								.getEventType(methodKey);
							if (eventType != null)
							{
								template = eventType.getMethodTemplate();
							}
						}
						if (template == null) template = MethodTemplate.getTemplate(met.getClass(), methodKey);
					}
					String scriptPath = SolutionSerializer.getScriptPath(met, false);
					IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
					// if the file isn't there, create it here so that the formatter sees the js file.
					if (!file.exists())
					{
						if (met.getParent() instanceof TableNode &&
							!ResourcesPlugin.getWorkspace().getRoot().getFile(new Path(SolutionSerializer.getRelativeFilePath(met.getParent(), true))).exists())
						{
							// js file does not exist yet, table node may have been created in memory in editing solution, make sure the table node is saved,
							// otherwise the same table node (but with different uuid) will be created in the real solution when the js file is read.
							((EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository()).updateNode(met.getParent(), false);
						}

						// file doesn't exist, create the file and its parent directories
						new WorkspaceFileAccess(ServoyModel.getWorkspace()).setContents(scriptPath, new byte[0]);
					}

					Solution solution = (Solution)parent.getAncestor(IRepository.SOLUTIONS);
					ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solution.getName());
					JSDocScriptTemplates templates = JSDocScriptTemplates.getTemplates(servoyProject.getProject(), true);
					String userTemplate = templates.getMethodTemplate();
					String declaration = template.getMethodDeclaration(met.getName(), null, tagToOutput, userTemplate, substitutions,
						templates.getCleanMethodTemplateProperty(), override);

					declaration = format(declaration, file, 0);

					met.setDeclaration(declaration);

					String code = SolutionSerializer.serializePersist(met, true, ApplicationServerRegistry.get().getDeveloperRepository(), null).toString();
					final IEditorPart openEditor = PlatformUI.getWorkbench().getWorkbenchWindows()[0].getActivePage().findEditor(
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

						st.append("\n"); // for some reason this must be separately
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
										if (MessageDialog.openQuestion(shell, "Javascript editor not saved",
											"The javascript editor for this form is open and dirty.\nThe new method has been appended to it, but\nyou have to save it in order to be able to select/see the new method in Solution Explorer and other places.\n\nDo you want to save the editor?"))
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
							baos.write("\n".getBytes("UTF8"));
							baos.write(code.getBytes("UTF8"));
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
				MessageDialog.openWarning(shell, "Cannot create the new " + methodType + " method", "Reason: " + e.getMessage());
				ServoyLog.logWarning("Cannot create method", e);
			}
			catch (CoreException e)
			{
				MessageDialog.openWarning(shell, "Cannot create the JS file for new " + methodType + " method", "Reason: " + e.getMessage());
				ServoyLog.logWarning("Cannot create method", e);
			}
			catch (IOException e)
			{
				MessageDialog.openWarning(shell, "Cannot modify JS file contents for new " + methodType + " method", "Reason: " + e.getMessage());
				ServoyLog.logWarning("Cannot create method", e);
			}
			catch (JSONException e)
			{
				MessageDialog.openWarning(shell, "Cannot create the new " + methodType + " method", "Reason: " + e.getMessage());
				ServoyLog.logWarning("Cannot create method", e);
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

	private static String validateMethodName(final IPersist parent, String scopeName, String newText)
	{
		IValidateName validator = getValidator(parent);
		try
		{
			if (parent instanceof Solution)
			{
				// global method
				validator.checkName(newText, null, new ValidatorSearchContext(scopeName, IRepository.METHODS), false);
			}

			if (parent instanceof Form)
			{
				// form method
				validator.checkName(newText, null, new ValidatorSearchContext(parent, IRepository.METHODS), false);
			}
			if (parent instanceof TableNode)
			{
				// foundset method
				validator.checkName(newText, null, new ValidatorSearchContext(parent, IRepository.METHODS), false);
			}
		}
		catch (RepositoryException e)
		{
			return e.getMessage();
		}

		if (!IdentDocumentValidator.isJavaIdentifier(newText))
		{
			return "Invalid method name";
		}
		// valid
		return null;
	}

	private static IValidateName getValidator(final IPersist parent)
	{
		IValidateName validator = null;
		if (parent instanceof Solution && (((Solution)parent).getSolutionType() == SolutionMetaData.PRE_IMPORT_HOOK ||
			((Solution)parent).getSolutionType() == SolutionMetaData.POST_IMPORT_HOOK))
		{
			validator = new ScriptNameValidator(ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(parent));
		}
		else
		{
			validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
		}
		return validator;
	}

	private static Pair<Pair<String, String>, Integer> askForMethodName(String methodType, final IPersist parent, String methodKey, Shell shell, int tagFilter,
		String[] listOptions, String listValue, String listDescriptionText, IPersist persist, String scopeName)
	{
		String defaultName = "";
		if (methodKey != null)
		{
			MethodTemplate template = persist instanceof WebComponent ? MethodTemplate.DEFAULT_TEMPLATE
				: MethodTemplate.getTemplate(ScriptMethod.class, methodKey);
			MethodArgument signature = template.getSignature();
			String name;
			if (signature == null || signature.getName() == null)
			{
				// not a servoy method key, probably a bean property
				String[] split = methodKey.split("\\.");
				name = split[split.length - 1];
				if (name.endsWith("MethodID"))
				{
					name = name.substring(0, name.indexOf("MethodID"));
				}
			}
			else
			{
				name = signature.getName();
			}

			if (persist instanceof TableNode) name = getTableEventHandlerName(name, persist);
			else if (!(persist instanceof Solution) && !(persist instanceof Form)) name = getFormEventHandlerName(name, persist);

			for (int i = 0; i < 100; i++)
			{
				defaultName = name;
				if (i > 0) defaultName += i;
				if (validateMethodName(parent, scopeName, defaultName) == null)
				{
					break;
				}
			}
		}
		NewMethodInputDialog dialog = new NewMethodInputDialog(shell, "New " + methodType + " method", "Specify a method name", defaultName, parent, scopeName,
			tagFilter, listOptions, listValue, listDescriptionText);
		dialog.setBlockOnOpen(true);
		dialog.open();

		return (dialog.getReturnCode() == Window.CANCEL) ? null : new Pair<Pair<String, String>, Integer>(
			new Pair<String, String>(dialog.getExtendedValue(), dialog.getValue()), Integer.valueOf(dialog.tagToOutput));
	}

	private static String makePrettyName(String simpleMethodName, String elementName)
	{
		if (simpleMethodName == null) return null; //this should never happen
		if (elementName == null) return simpleMethodName;
		String modifiedElemName = elementName.substring(0, 1).toUpperCase() + elementName.substring(1);
		//include in method name ("on/after ... Action ")
		if (simpleMethodName.startsWith("after")) return new StringBuffer(simpleMethodName).insert(5, modifiedElemName).toString();
		else return new StringBuffer(simpleMethodName).insert(2, modifiedElemName).toString();
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
		if (dp.getFormEventHandlerNamingDefault()) return simpleName;

		if (persist instanceof ISupportDataProviderID && ((ISupportDataProviderID)persist).getDataProviderID() != null &&
			(dp.getIncludeFormElementDataProviderName() || dp.getIncludeFormElementDataProviderNameWithFallback()))
		{
			return makePrettyName(simpleName, ((ISupportDataProviderID)persist).getDataProviderID());
		}
		// does not support dataprovider OR dataprovider is null
		if ((persist instanceof ISupportName) && (dp.getIncludeFormElementName() || dp.getIncludeFormElementDataProviderNameWithFallback()))
		{
			return makePrettyName(simpleName, ((ISupportName)persist).getName());
		}
		return simpleName;
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

		private NewMethodInputDialog(Shell parentShell, String dialogTitle, String dialogMessage, String initialValue, final IPersist parent,
			final String scopeName, int tagFilter, String[] listOptions, String listValue, String listDescriptionText)
		{
			super(parentShell, dialogTitle, dialogMessage, initialValue, new IInputValidator()
			{
				public String isValid(String newText)
				{
					if (newText.length() == 0) return "";
					return validateMethodName(parent, scopeName, newText);
				}
			}, listOptions, listValue, listDescriptionText);
			this.tagFilter = tagFilter;
		}

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
