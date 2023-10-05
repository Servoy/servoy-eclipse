package com.servoy.eclipse.aibridge;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.core.resources.IFile;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.internal.javascript.ti.TypeInferencer2;
import org.eclipse.dltk.javascript.ast.Script;
import org.eclipse.dltk.javascript.parser.JavaScriptParserUtil;
import org.eclipse.dltk.javascript.typeinference.IValueReference;
import org.eclipse.dltk.javascript.typeinfo.IRClassType;
import org.eclipse.dltk.javascript.typeinfo.IRType;
import org.eclipse.dltk.javascript.typeinfo.JSTypeSet;
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

	private static final Map<String, String> COMMAND_TO_ENDPOINT_MAP = new HashMap<>();
	private IEditorPart activeEditor;

	static
	{
		COMMAND_TO_ENDPOINT_MAP.put("com.servoy.eclipse.aibridge.explain_command",
			"https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/llm/explainCode");
		//COMMAND_TO_ENDPOINT_MAP.put("com.servoy.eclipse.aibridge.add_inline_comments", "https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/llm/inlineComments");
		COMMAND_TO_ENDPOINT_MAP.put("com.servoy.eclipse.aibridge.add_inline_comments",
			"https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/llm/findBugs");
		COMMAND_TO_ENDPOINT_MAP.put("com.servoy.eclipse.aibridge.find_errors",
			"https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/llm/findBugs");
	}

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
			String endpoint = COMMAND_TO_ENDPOINT_MAP.get(event.getCommand().getId());
			if (endpoint != null && codeSelection != null)
			{
				final StringBuilder sb = new StringBuilder();
				sb.append("\nSpecifications:\n");
				ISourceModule module = DLTKUIPlugin.getEditorInputModelElement(activeEditor.getEditorInput());
				final Script script = JavaScriptParserUtil.parse(module, null);

				TypeInferencer2 inferencer = new TypeInferencer2();
				final IdentifierCollectingVisitor collector = new IdentifierCollectingVisitor(inferencer, codeSelection.getOffset(), codeSelection.getLength());
				inferencer.setVisitor(collector);
				inferencer.setModelElement(module);
				inferencer.doInferencing(script);

				collector.identifiers.forEach((node, reference) -> {
					JSTypeSet types = reference.getTypes();
					JSTypeSet declaredTypes = reference.getDeclaredTypes();
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
						sb.append('\n');
						sb.append(node);
						sb.append(" is of type ");
						sb.append(type);
						sb.append('\n');
						ITypedScriptObject scriptObject = ScriptObjectRegistry.getScriptObjectByName(type);
						List<IValueReference> callsOrProperties = collector.propertiesOrCalls.get(node);
						if (scriptObject != null && callsOrProperties != null && scriptObject.getObjectDocumentation() != null)
						{
							IObjectDocumentation docFile = scriptObject.getObjectDocumentation();
							callsOrProperties.forEach(action -> {
								String name = action.getName();
								sb.append(node);
								sb.append('.');
								List<IFunctionDocumentation> functions = docFile.getFunctions().stream().filter(function -> function.getMainName().equals(name))
									.sorted((func1, func2) -> func1.getArguments().size() - func2.getArguments().size())
									.collect(Collectors.toList());
								if (functions.size() == 1)
								{
									IFunctionDocumentation function = functions.get(0);
									if (function.getType() == IFunctionDocumentation.TYPE_FUNCTION)
									{
										sb.append(function.getFullJSTranslatedSignature(true, false).split(" ", 2)[1]);
										sb.append(":\n");
										generateDescription(function, function.getArgumentsTypes().length, sb);
									}
									else if (function.getType() == IFunctionDocumentation.TYPE_CONSTANT ||
										function.getType() == IFunctionDocumentation.TYPE_PROPERTY)
									{
										sb.append(function.getMainName());
										sb.append(":\n");
										sb.append(function.getDescription(ClientSupport.ng));
									}

								}
								else if (functions.size() > 1)
								{
									IFunctionDocumentation function = functions.get(functions.size() - 1);
									sb.append(function.getFullJSTranslatedSignature(true, false).split(" ", 2)[1]);
									sb.append(":\n");
									generateDescription(function, functions.get(0).getArgumentsTypes().length, sb);
								}
							});
						}
						sb.append('\n');
					}
				});
				AiBridgeManager.sendRequest(
					event.getCommand().getName(),
					endpoint,
					codeSelection.getText().trim(),
					filePathSelection,
					codeSelection.getOffset(),
					codeSelection.getLength(),
					sb.toString());

			}

			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().showView("com.servoy.eclipse.aibridge.aibridgeviewid");
		}
		catch (PartInitException | NotDefinedException e)
		{
			e.printStackTrace();
		}
		return null;
	}

	private String getContextData(String filePath, int offset, int length)
	{
		//TODO: implement something like dltk.getContextData(inputData); this require dltk support
		return "";
	}

	private void generateDescription(IFunctionDocumentation fdoc, int mandatoryParams, StringBuilder sb)
	{
		Class< ? > returnType = fdoc.getReturnedType();
		String returnDescription = fdoc.getReturnDescription();
		LinkedHashMap<String, IParameterDocumentation> parameters = fdoc.getArguments();
		String tooltip = fdoc.getDescription(ClientSupport.ng);

		sb.append(tooltip);
		if (parameters != null)
		{
			int paramCount = 0;
			for (IParameterDocumentation parameter : parameters.values())
			{
				sb.append("\n@param {");
				sb.append(DocumentationUtil.getJavaToJSTypeTranslator().translateJavaClassToJSTypeName(parameter.getType()));
				sb.append("} ");
				if (paramCount >= mandatoryParams) sb.append('[');
				sb.append(parameter.getName());
				if (paramCount >= mandatoryParams) sb.append(']');
				sb.append(" ");
				sb.append((parameter.getDescription() != null ? parameter.getDescription() : ""));
				paramCount++;
			}
		}
		if (returnType != null && returnType != Void.class && returnType != void.class)
		{
			if (fdoc.getType() == IFunctionDocumentation.TYPE_FUNCTION)
			{
				sb.append("\n@return {");
				sb.append(XMLScriptObjectAdapter.getReturnTypeString(returnType));
				sb.append("} ");
				if (returnDescription != null) sb.append(returnDescription);
			}
		}

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
}