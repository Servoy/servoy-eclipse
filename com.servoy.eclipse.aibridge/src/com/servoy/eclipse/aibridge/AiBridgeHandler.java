package com.servoy.eclipse.aibridge;

import java.io.IOException;
import java.io.Reader;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IMember;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.IModelElementVisitor;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.ISourceRange;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.core.ScriptModelUtil;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencer2;
import org.eclipse.dltk.javascript.ast.JSNode;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinference.ReferenceLocation;
import org.eclipse.dltk.javascript.typeinfo.IRClassType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.JSTypeSet;
import org.eclipse.dltk.javascript.ui.scriptdoc.ScriptdocContentAccess;
import org.eclipse.dltk.javascript.ui.scriptdoc.StringJavaDocCommentReader;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IFileEditorInput;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.ISelectionService;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.IFunctionParameters;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.WebServiceSpecProvider;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.documentation.DocumentationUtil;
import com.servoy.j2db.documentation.IFunctionDocumentation;
import com.servoy.j2db.documentation.IObjectDocumentation;
import com.servoy.j2db.documentation.IParameterDocumentation;
import com.servoy.j2db.documentation.XMLScriptObjectAdapter;
import com.servoy.j2db.scripting.ITypedScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;

public class AiBridgeHandler extends AbstractHandler implements ISelectionListener
{

	private boolean isEnabled = false;
	ISelectionService selectionService = null;
	ITextSelection codeSelection = null;
	String filePathSelection = null;

	private IEditorPart activeEditor;

	public AiBridgeHandler()
	{
		initialize();
	}

	private void initialize()
	{
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench == null) return;

		IWorkbenchWindow window = workbench.getActiveWorkbenchWindow();
		if (window == null) return;

		selectionService = window.getSelectionService();
		if (selectionService == null) return;

		selectionService.addSelectionListener(this);

		IWorkbenchPage activePage = window.getActivePage();
		if (activePage != null)
		{
			activeEditor = activePage.getActiveEditor();
			if (activeEditor != null)
			{
				IEditorInput input = activeEditor.getEditorInput();
				if (input instanceof IFileEditorInput)
				{

					IFile file = ((IFileEditorInput)input).getFile();
					filePathSelection = file.getFullPath().toString();
				}
			}
		}

		updateSelection(selectionService.getSelection());
	}

	private void updateSelection(ISelection currentSelection)
	{
		isEnabled = false;
		if (currentSelection instanceof ITextSelection textSelection)
		{
			String text = textSelection.getText();
			if (text != null && !text.trim().isEmpty())
			{
				isEnabled = true;
				codeSelection = textSelection;
			}
		}
		fireHandlerChanged(new HandlerEvent(this, true, false));
	}

	@Override
	public Object execute(ExecutionEvent event) throws ExecutionException
	{
		try
		{
			if (codeSelection != null)
			{
				AiBridgeManager.getInstance().sendRequest(
					event.getCommand().getName(),
					codeSelection.getText().trim(),
					filePathSelection,
					codeSelection.getOffset(),
					codeSelection.getLength(),
					getContextData());

			}

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.servoy.eclipse.aibridge.aibridgeview");
		}
		catch (PartInitException | NotDefinedException e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	/**
	 * @param sb
	 * @param node
	 * @param callsOrProperties
	 * @param webObjectSpecification
	 */
	private void generateApiOrPropertySpec(final StringBuilder sb, JSNode node, List<IValueReference> callsOrProperties,
		WebObjectSpecification webObjectSpecification)
	{
		if (webObjectSpecification != null)
		{
			callsOrProperties.forEach(action -> {
				appendData(node + ".", sb);

				WebObjectFunctionDefinition apiFunction = webObjectSpecification.getApiFunction(action.getName());
				if (apiFunction != null)
				{
					appendData(action.getName() + "(", sb);
					IFunctionParameters parameters = apiFunction.getParameters();
					for (int i = 0; i < parameters.getDefinedArgsCount(); i++)
					{
						PropertyDescription parameterDefinition = parameters.getParameterDefinition(i);
						if (parameterDefinition.isOptional())
						{
						}
						appendData(parameterDefinition.getName(), sb);
						if (parameterDefinition.isOptional())
						{
							appendData("?", sb);
						}
						if (i < parameters.getDefinedArgsCount() - 1) appendData(", ", sb);
					}
					appendData("):\n", sb);
					String functionDoc = apiFunction.getDocumentation();
					if (functionDoc != null && functionDoc.trim().length() > 0)
					{
						StringJavaDocCommentReader reader = new StringJavaDocCommentReader(apiFunction.getDocumentation());
						String doc;
						try
						{
							doc = IOUtils.toString(reader).trim();
							appendData(doc.replace('\r', '\n').replace("\n ", "\n").replace("\n\n", "\n"), sb);
						}
						catch (IOException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
				else
				{
					PropertyDescription property = webObjectSpecification.getProperty(action.getName());
					if (property != null)
					{
						appendData(action.getName() + ":\n", sb);
						appendData(property.getDocumentation(), sb);
					}
				}
			});
		}
	}

	private String getContextData()
	{
		final StringBuilder sb = new StringBuilder();

		ISourceModule module = DLTKUIPlugin.getEditorInputModelElement(activeEditor.getEditorInput());
		final Script script = JavaScriptParserUtil.parse(module, null);

		TypeInferencer2 inferencer = new TypeInferencer2();
		final IdentifierCollectingVisitor collector = new IdentifierCollectingVisitor(inferencer, codeSelection.getOffset(), codeSelection.getLength());
		inferencer.setVisitor(collector);
		inferencer.setModelElement(module);
		inferencer.doInferencing(script);

		collector.identifiers.forEach((node, pair) -> {
			JSTypeSet types = pair.getLeft().getTypes();
			JSTypeSet declaredTypes = pair.getLeft().getDeclaredTypes();
			String type = null;
			if (types.size() > 0)
			{
				IRType irType = types.iterator().next();
				if (irType instanceof IRClassType clsType)
				{
					irType = clsType.toItemType();
				}
				type = irType.getName();
			}
			if (declaredTypes.size() > 0)
			{
				IRType irType = declaredTypes.iterator().next();
				if (irType instanceof IRClassType clsType)
				{
					irType = clsType.toItemType();
				}
				type = irType.getName();
			}
			if (type != null)
			{
				if ("Function".equals(type))
				{
					ReferenceLocation location = pair.getLeft().getLocation();

					IModelElement element = locateModelElement(location);
					try (Reader reader = ScriptdocContentAccess.getContentReader((IMember)element, true))
					{
						String doc = IOUtils.toString(reader);
						if (doc != null)
						{
							appendData(node.getParent() + ":\n", sb);
							String[] lines = doc.split("\n");
							for (String line : lines)
							{
								line = line.trim();
								if (line.startsWith("@properties=")) continue;
								if (!line.isBlank())
								{
									appendData(line + "\n", sb);
								}
							}
							appendData("\n", sb);
						}

					}
					catch (ModelException | IOException e)
					{
						ServoyLog.logError(e);
					}
				}
				else
				{
					String typeLine = "<type>" + pair.getRight() + ": " + type + "</type>";
					appendData(typeLine, sb);

				}
				ITypedScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectByName(type);
				List<IValueReference> callsOrProperties = collector.propertiesOrCalls.get(node);
				if (scriptObject != null && callsOrProperties != null && scriptObject.getObjectDocumentation() != null)
				{
					IObjectDocumentation docFile = scriptObject.getObjectDocumentation();
					callsOrProperties.forEach(action -> {
						String name = action.getName();
						appendData(node + ".", sb);
						List<IFunctionDocumentation> functions = docFile.getFunctions().stream().filter(function -> function.getMainName().equals(name))
							.sorted((func1, func2) -> func1.getArguments().size() - func2.getArguments().size())
							.collect(Collectors.toList());
						if (functions.size() == 1)
						{
							IFunctionDocumentation function = functions.get(0);
							if (function.getType() == IFunctionDocumentation.TYPE_FUNCTION)
							{
								appendData(function.getFullJSTranslatedSignature(true, false).split(" ", 2)[1] + ":\n", sb);
								generateDescription(function, function.getArgumentsTypes().length, sb);
								closeContext(sb);
							}
							else if (function.getType() == IFunctionDocumentation.TYPE_CONSTANT ||
								function.getType() == IFunctionDocumentation.TYPE_PROPERTY)
							{
								appendData(function.getMainName() + ":\n", sb);
								appendData(function.getDescription(ClientSupport.ng), sb);
								closeContext(sb);
							}

						}
						else if (functions.size() > 1)
						{
							IFunctionDocumentation function = functions.get(functions.size() - 1);
							appendData(function.getFullJSTranslatedSignature(true, false).split(" ", 2)[1] + ":\n", sb);
							generateDescription(function, functions.get(0).getArgumentsTypes().length, sb);
							closeContext(sb);
						}
					});
				}
				else if (type.startsWith("WebService") && callsOrProperties != null && callsOrProperties.size() > 0)
				{
					// code for service plugins
					String serviceName = type.substring("WebService<".length(), type.length() - 1);
					WebObjectSpecification webObjectSpecification = WebServiceSpecProvider.getSpecProviderState()
						.getWebObjectSpecification(serviceName);
					generateApiOrPropertySpec(sb, node, callsOrProperties, webObjectSpecification);
					closeContext(sb);
				}
				else if (type.startsWith("RuntimeWebComponent") && callsOrProperties != null && callsOrProperties.size() > 0)
				{
					String componentName = type.substring("RuntimeWebComponent<".length(), type.length() - 1);
					if (componentName.endsWith("_abs")) componentName = componentName.substring(0, componentName.length() - 4);
					WebObjectSpecification componentSpec = WebComponentSpecProvider.getSpecProviderState()
						.getWebObjectSpecification(componentName);
					generateApiOrPropertySpec(sb, node, callsOrProperties, componentSpec);
					closeContext(sb);
				}
			}
		});
		String returnValue = sb.toString(); //avoid trim(); this will cut also /n's from the start / end
		return returnValue.replaceAll("\\n+</description>", "</description>").replaceAll("\\n+<type>", "\n<type>").trim();
	}

	private void generateDescription(IFunctionDocumentation fdoc, int mandatoryParams, StringBuilder sb)
	{
		Class< ? > returnType = fdoc.getReturnedType();
		String returnDescription = fdoc.getReturnDescription();
		LinkedHashMap<String, IParameterDocumentation> parameters = fdoc.getArguments();
		String tooltip = fdoc.getDescription(ClientSupport.ng);

		appendData(tooltip, sb);
		if (parameters != null)
		{
			int paramCount = 0;
			for (IParameterDocumentation parameter : parameters.values())
			{
				appendData("\n@param {", sb);
				appendData(DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(parameter.getType()), sb);
				appendData("} ", sb);
				if (paramCount >= mandatoryParams) appendData("[", sb);
				appendData(parameter.getName(), sb);
				if (paramCount >= mandatoryParams) appendData("] optional", sb);
				appendData(" ", sb);
				appendData(parameter.getDescription() != null ? parameter.getDescription() : "", sb);
				paramCount++;
			}
		}
		if (returnType != null && returnType != Void.class && returnType != void.class)
		{
			if (fdoc.getType() == IFunctionDocumentation.TYPE_FUNCTION)
			{
				appendData("\n@return {", sb);
				appendData(XMLScriptObjectAdapter.getReturnTypeString(returnType), sb);
				appendData("} ", sb);
				if (returnDescription != null) appendData(returnDescription, sb);
				appendData("\n", sb);
			}
		}

	}

	private void closeContext(StringBuilder sb)
	{
		String typeEnd = "</type>";
		String descriptionEnd = "</description>";
		int typeEndIndex = sb.lastIndexOf(typeEnd) < 0 ? 0 : sb.lastIndexOf(typeEnd);
		boolean isTypeEnding = sb.length() > 0 && typeEndIndex == sb.length() - typeEnd.length();
		boolean isDescriptionEnding = (sb.length() > 0 && sb.lastIndexOf(descriptionEnd) == sb.length() - descriptionEnd.length()) ? true : false;
		if (isTypeEnding || isDescriptionEnding) return;

		int descriptionStartIndex = sb.indexOf("<description>", typeEndIndex);
		if (descriptionStartIndex >= 0 && sb.indexOf(descriptionEnd, descriptionStartIndex) < 0)
		{
			sb.append(descriptionEnd);
		}
	}


	private void appendData(String data, StringBuilder sb)
	{

		// data must be separated using xml tags
		// data may be a type line: <type>...</type>\n
		// or a non empty line. Lines between two type lines must be enclosed in <description> ... </description>\n tags
		// avoid starting and ending \n characters
		if (data == null) return;
		String typeEnd = "</type>";
		String descriptionStart = "<description>";
		String descriptionEnd = "</description>";
		boolean isTypeLine = data.endsWith("</type>");
		boolean isTypeEnding = (sb.length() > 0 && sb.lastIndexOf(typeEnd) == sb.length() - typeEnd.length() - 1) ? true : false;
		boolean isDescriptionEnding = (sb.length() > 0 && sb.lastIndexOf(descriptionEnd) == sb.length() - descriptionEnd.length() - 1) ? true : false;
		int typeEndIndex = sb.lastIndexOf(typeEnd) < 0 ? 0 : sb.lastIndexOf(typeEnd);
		int descriptionStartIndex = sb.indexOf(descriptionStart, typeEndIndex);
		int descriptionEndIndex = sb.indexOf(descriptionEnd, descriptionStartIndex);

		if (isTypeEnding || isDescriptionEnding)
		{
			sb.append("\n");
		}

		if (!isTypeLine)
		{//not a type means (partial) description
			if (sb.length() == 0 || descriptionStartIndex < 0 || isDescriptionEnding || isTypeEnding)
			{
				//description has not started
				sb.append(descriptionStart);
			}
		}
		else
		{
			//type line (avoid duplicate types)
			if (sb.indexOf(data) >= 0) return;
			if (descriptionStartIndex >= 0 && descriptionEndIndex < 0)
			{
				//type line following description
				sb.append(descriptionEnd);
			}
		}
		sb.append(data);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		updateSelection(selection);
		if (part instanceof IEditorPart editorPart)
		{
			activeEditor = editorPart;
			IEditorInput input = editorPart.getEditorInput();
			if (input instanceof IFileEditorInput fileEditorInput)
			{
				IFile file = fileEditorInput.getFile();
				filePathSelection = file.getFullPath().toString();
			}
		}
	}

	@Override
	public boolean isEnabled()
	{
		return this.isEnabled;
	}

	@Override
	public void dispose()
	{
		if (selectionService != null)
		{
			selectionService.removeSelectionListener(this);
		}
	}

	private IModelElement locateModelElement(final ReferenceLocation location)
	{
		ISourceModule module = location.getSourceModule();
		if (module != null)
		{
			try
			{
				ScriptModelUtil.reconcile(module);
				module.accept(new Visitor(location.getNameStart(), location
					.getNameEnd()));
			}
			catch (ModelException e)
			{
				if (DLTKCore.DEBUG)
				{
					ServoyLog.logError(e);
				}
			}
			catch (ModelElementFound e)
			{
				return e.element;
			}
		}
		return null;
	}

	private static class ModelElementFound extends RuntimeException
	{
		final IModelElement element;

		public ModelElementFound(IModelElement element)
		{
			this.element = element;
		}

	}

	private static class Visitor implements IModelElementVisitor
	{

		private final int nameStart;
		private final int nameEnd;

		public Visitor(int nameStart, int nameEnd)
		{
			this.nameStart = nameStart;
			this.nameEnd = nameEnd;
		}

		public boolean visit(IModelElement element)
		{
			if (element instanceof IMember)
			{
				IMember member = (IMember)element;
				try
				{
					ISourceRange range = member.getNameRange();
					if (range.getOffset() == nameStart && range.getLength() == nameEnd - nameStart)
					{
						throw new ModelElementFound(element);
					}
				}
				catch (ModelException e)
				{
					//
				}
			}
			return true;
		}

	}
}