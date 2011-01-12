package com.servoy.eclipse.debug.scriptingconsole;

import java.io.IOException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.codeassist.ICompletionEngine;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.console.ui.ScriptConsoleSourceViewerConfiguration;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.javascript.core.JavaScriptLanguageToolkit;
import org.eclipse.dltk.ui.text.completion.AbstractScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.CompletionProposalComparator;
import org.eclipse.dltk.ui.text.completion.IScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalCollector;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;
import org.mozilla.javascript.Undefined;
import org.mozilla.javascript.Wrapper;

import com.servoy.eclipse.debug.script.ValueCollectionProvider;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.IDebugWebClient;
import com.servoy.j2db.debug.DebugWebClient;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.server.shared.IDebugHeadlessClient;

public class ScriptingConsole extends ViewPart implements ICommandHandler
{
	private static final String TEST_SCOPE = "____TEST_SCOPE____"; //$NON-NLS-1$

	private ListViewer clientsList;
	private ClientState selectedClient;
	private ScriptConsoleViewer text;

	private TextViewerAction proposalsAction;
	private ActionHandler proposalsHandler;

	private IContentAssistProcessor processor;

	private ITextHover hover;
	private IHandlerActivation activateHandler;
	private TestClientsJob testClientsJob;

	private final HashMap<Scriptable, StringBuilder> previousScripts = new HashMap<Scriptable, StringBuilder>();

	@Override
	public void createPartControl(Composite parent)
	{
		SashForm form = new SashForm(parent, SWT.NONE);
		Composite list = new Composite(form, SWT.NONE);
		list.setLayout(new GridLayout());
		new Label(list, SWT.NONE).setText(Messages.ScriptingConsole_activeClients);
		clientsList = new ListViewer(list, SWT.SINGLE | SWT.H_SCROLL | SWT.V_SCROLL | SWT.BORDER);
		clientsList.getList().setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		clientsList.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection.getFirstElement() instanceof ClientState)
				{
					selectedClient = (ClientState)selection.getFirstElement();
				}
				else selectedClient = null;

				text.setEditable(selectedClient != null);
				if (selectedClient != null)
				{
					text.getTextWidget().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
				}
				else
				{
					text.getTextWidget().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
				}

			}
		});
		clientsList.setContentProvider(new ClientsContentProvider());
		clientsList.setLabelProvider(new ClientsLabelProvider());
		processor = new ContentAssistProcessor();
		hover = new TextHover();
		SourceViewerConfiguration cfg = new ScriptConsoleSourceViewerConfiguration(processor, hover);

		text = new ScriptConsoleViewer(form, new ScriptConsole(), this);
		text.setEditable(false);
		text.configure(cfg);

		IHandlerService handlerService = (IHandlerService)getSite().getService(IHandlerService.class);
		proposalsAction = new TextViewerAction(text, ISourceViewer.CONTENTASSIST_PROPOSALS);
		proposalsAction.setEnabled(true);
		proposalsAction.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
		proposalsHandler = new ActionHandler(proposalsAction);
		activateHandler = handlerService.activateHandler(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, proposalsHandler);

		form.setWeights(new int[] { 20, 80 });

		testClientsJob = new TestClientsJob(Messages.ScriptingConsole_testClientsJobName);
		testClientsJob.schedule(20 * 1000);

		Object[] clients = ApplicationServerSingleton.get().getDebugClientHandler().getActiveDebugClients().toArray();
		clientsList.setInput(clients);
		if (clients.length > 0)
		{
			clientsList.setSelection(new StructuredSelection(clients[0]));
		}
		else
		{
			text.setEditable(false);
			text.getTextWidget().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
		}


	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();
		IHandlerService handlerService = (IHandlerService)getSite().getService(IHandlerService.class);
		handlerService.deactivateHandler(activateHandler);
		proposalsHandler.dispose();
		testClientsJob.cancel();
	}

	@Override
	public void setFocus()
	{
		text.getTextWidget().setFocus();
	}


	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.dltk.console.ui.internal.ICommandHandler#handleCommand(java.lang.String)
	 */
	public IScriptExecResult handleCommand(final String userInput) throws IOException
	{
		if (selectedClient != null)
		{
			final ClientState state = selectedClient;
			final Object[] retVal = new Object[1];
			if (state instanceof DebugWebClient) ((DebugWebClient)state).addEventDispatchThread();
			try
			{
				state.invokeAndWait(new Runnable()
				{
					public void run()
					{
						Object eval = null;
						Context cx = Context.enter();
						try
						{
							Scriptable scope = getScope(state, true);
							eval = cx.evaluateString(scope, userInput, "internal_anon", 1, null); //$NON-NLS-1$
							if (eval instanceof Wrapper)
							{
								eval = ((Wrapper)eval).unwrap();
							}
							if (eval == Scriptable.NOT_FOUND || eval == Undefined.instance)
							{
								eval = null;
							}

							StringBuilder stringBuilder = previousScripts.get(scope);
							if (stringBuilder == null)
							{
								stringBuilder = new StringBuilder();
								previousScripts.put(scope, stringBuilder);
							}
							stringBuilder.append(userInput);
							stringBuilder.append('\n');
						}
						catch (Exception ex)
						{
							eval = ex;
						}
						finally
						{
							Context.exit();
						}
						retVal[0] = eval;
					}
				});
			}
			finally
			{
				if (state instanceof DebugWebClient) ((DebugWebClient)state).removeEventDispatchThread();
			}
			return new ScriptResult(retVal[0]);
		}
		return null;
	}

	private Scriptable getScope(ClientState state, boolean create)
	{
		GlobalScope ss = state.getScriptEngine().getGlobalScope();
		Scriptable scope = null;
		if (ss.has(TEST_SCOPE, ss))
		{
			scope = (Scriptable)ss.get(TEST_SCOPE, ss);
		}
		else if (create)
		{
			scope = Context.enter().newObject(ss);
			ss.putWithoutFireChange(TEST_SCOPE, scope);
			scope.setParentScope(ss);
		}
		return scope;
	}

	/**
	 * @author jcompagner
	 *
	 */
	private final class TextHover implements ITextHover
	{
		public IRegion getHoverRegion(ITextViewer textViewer, int offset)
		{
			// TODO Auto-generated method stub
			return null;
		}

		public String getHoverInfo(ITextViewer textViewer, IRegion hoverRegion)
		{
			// TODO Auto-generated method stub
			return null;
		}
	}

	/**
	 * @author jcompagner
	 *
	 */
	private final class ContentAssistProcessor implements IContentAssistProcessor
	{
		public String getErrorMessage()
		{
			return null;
		}

		public IContextInformationValidator getContextInformationValidator()
		{
			return null;
		}

		public char[] getContextInformationAutoActivationCharacters()
		{
			return null;
		}

		public char[] getCompletionProposalAutoActivationCharacters()
		{
			return new char[] { '.' };
		}

		public IContextInformation[] computeContextInformation(ITextViewer viewer, int offset)
		{
			return null;
		}

		public ICompletionProposal[] computeCompletionProposals(ITextViewer viewer, int offset)
		{
			if (selectedClient != null)
			{
				final ICompletionEngine engine = DLTKLanguageManager.getCompletionEngine(JavaScriptLanguageToolkit.getDefault().getNatureId());
				if (engine == null)
				{
					return new ICompletionProposal[0];
				}

				StringBuilder sb = previousScripts.get(getScope(selectedClient, true));
				String commandLine = ((ScriptConsoleViewer)viewer).getCommandLine();
				final String input = sb == null ? commandLine : sb.toString() + commandLine;

				String solutionName = selectedClient.getSolutionName();
				IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(solutionName);
				final IFile file = project.getFile("globals.js"); //$NON-NLS-1$
				final IModelElement modelElement = DLTKCore.create(file);

				IModuleSource source = new IModuleSource()
				{

					public String getFileName()
					{
						return file.getName();
					}

					public String getSourceContents()
					{
						return input;
					}

					public IModelElement getModelElement()
					{
						return modelElement;
					}

					public char[] getContentsAsCharArray()
					{
						return input.toCharArray();
					}
				};

				ScriptCompletionProposalCollector collector = new ScriptCompletionProposalCollector(DLTKCore.create(project))
				{
					@Override
					protected String getNatureId()
					{
						return JavaScriptLanguageToolkit.getDefault().getNatureId();
					}
				};
				engine.setRequestor(collector);
				// a bit of a hack to get full globals completion.
				ValueCollectionProvider.setGenerateFullGlobalCollection(Boolean.TRUE);
				try
				{
					int caretPosition = ((ScriptConsoleViewer)viewer).getCaretPosition();
					caretPosition = caretPosition - ((ScriptConsoleViewer)viewer).getCommandLineOffset();
					if (sb != null) caretPosition += sb.length();
					engine.complete(source, caretPosition, 0);
				}
				finally
				{
					ValueCollectionProvider.setGenerateFullGlobalCollection(Boolean.FALSE);
				}
				IScriptCompletionProposal[] scriptCompletionProposals = collector.getScriptCompletionProposals();

				int commandLineStart = ((ScriptConsoleViewer)viewer).getCommandLineOffset() - (sb != null ? sb.length() : 0);
				for (IScriptCompletionProposal scriptCompletionProposal : scriptCompletionProposals)
				{
					int replacementOffset = ((AbstractScriptCompletionProposal)scriptCompletionProposal).getReplacementOffset();
					((AbstractScriptCompletionProposal)scriptCompletionProposal).setReplacementOffset(commandLineStart + replacementOffset);
				}
				Arrays.sort(scriptCompletionProposals, new CompletionProposalComparator());
				return scriptCompletionProposals;
			}
			return new ICompletionProposal[0];
		}
	}

	/**
	 * @author jcompagner
	 *
	 */
	private final class ScriptResult implements IScriptExecResult
	{
		private final Object eval;

		/**
		 * @param eval
		 */
		public ScriptResult(Object eval)
		{
			this.eval = eval;
		}

		public boolean isError()
		{
			return eval instanceof Exception;
		}

		public String getOutput()
		{
			if (eval instanceof Scriptable)
			{
				return (String)((Scriptable)eval).getDefaultValue(String.class);
			}
			else if (eval instanceof Object[])
			{
				return Arrays.toString((Object[])eval);
			}
			return eval != null ? eval.toString() : null;
		}
	}

	private static class ClientsLabelProvider implements ILabelProvider
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose()
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object, java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property)
		{
			return false;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ILabelProvider#getImage(java.lang.Object)
		 */
		public Image getImage(Object element)
		{
			return null;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.ILabelProvider#getText(java.lang.Object)
		 */
		public String getText(Object element)
		{
			if (element instanceof IDebugJ2DBClient) return Messages.ScriptingConsole_smartClientName;
			if (element instanceof IDebugWebClient) return Messages.ScriptingConsole_webClientName;
			if (element instanceof IDebugHeadlessClient) return Messages.ScriptingConsole_headlessClientName;
			return null;
		}
	}

	private static class ClientsContentProvider implements IStructuredContentProvider
	{

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose()
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement)
		{
			return (Object[])inputElement;
		}
	}
	private class TestClientsJob extends Job
	{
		private List<ClientState> activeClients;
		private boolean canceled;

		/**
		 * @param name
		 */
		public TestClientsJob(String name)
		{
			super(name);
			setSystem(true);
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
		 */
		@Override
		protected IStatus run(IProgressMonitor monitor)
		{
			List<ClientState> activeDebugClients = ApplicationServerSingleton.get().getDebugClientHandler().getActiveDebugClients();
			if (!activeDebugClients.equals(this.activeClients))
			{
				this.activeClients = activeDebugClients;

				@SuppressWarnings("unchecked")
				Map<Scriptable, StringBuilder> clone = (Map<Scriptable, StringBuilder>)previousScripts.clone();
				previousScripts.clear();
				for (ClientState clientState : activeDebugClients)
				{
					Scriptable scope = getScope(clientState, false);
					if (scope != null)
					{
						StringBuilder sb = clone.get(scope);
						if (sb != null)
						{
							previousScripts.put(scope, sb);
						}
					}
				}
				getSite().getShell().getDisplay().asyncExec(new Runnable()
				{

					public void run()
					{
						ISelection selection = clientsList.getSelection();
						clientsList.setInput(activeClients.toArray());
						clientsList.setSelection(selection);
						if (clientsList.getSelection().isEmpty() && activeClients.size() > 0)
						{
							clientsList.setSelection(new StructuredSelection(activeClients.get(0)));
						}
					}
				});
			}
			if (!canceled)
			{
				schedule(10 * 1000);
			}
			return Status.OK_STATUS;
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.core.runtime.jobs.Job#canceling()
		 */
		@Override
		protected void canceling()
		{
			super.canceling();
			canceled = true;
		}
	}
}
