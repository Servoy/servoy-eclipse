package com.servoy.eclipse.aibridge;

import java.util.HashMap;
import java.util.Map;

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
				ISourceModule module = DLTKUIPlugin.getEditorInputModelElement(activeEditor.getEditorInput());
				final Script script = JavaScriptParserUtil.parse(module, null);

				TypeInferencer2 inferencer = new TypeInferencer2();
				final IdentifierCollectingVisitor collector = new IdentifierCollectingVisitor(inferencer, codeSelection.getOffset(), codeSelection.getLength());
				inferencer.setVisitor(collector);
				inferencer.setModelElement(module);
				inferencer.doInferencing(script);

				collector.bindings.forEach((node, reference) -> {
					System.err.println(node);
					JSTypeSet types = reference.getTypes();
					JSTypeSet declaredTypes = reference.getDeclaredTypes();
					if (types.size() > 0) System.err.println(types);
					if (declaredTypes.size() > 0) System.err.println(declaredTypes);
				});
				AiBridgeManager.sendRequest(
					event.getCommand().getName(),
					endpoint,
					codeSelection.getText().trim(),
					filePathSelection,
					codeSelection.getOffset(),
					codeSelection.getLength(),
					getContextData(filePathSelection, codeSelection.getOffset(), codeSelection.getLength()));

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

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		updateSelection(selection);
		if (part instanceof IEditorPart editorPart)
		{

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