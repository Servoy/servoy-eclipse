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

package com.servoy.eclipse.debug.scriptingconsole;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.console.ui.ScriptConsolePartitioner;
import org.eclipse.dltk.console.ui.ScriptConsoleSourceViewerConfiguration;
import org.eclipse.dltk.debug.ui.display.IEvaluateConsole;
import org.eclipse.jface.commands.ActionHandler;
import org.eclipse.jface.text.ITextHover;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.source.ISourceViewer;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.console.IConsoleDocumentPartitioner;
import org.eclipse.ui.console.IConsoleView;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsolePage;
import org.eclipse.ui.console.TextConsoleViewer;
import org.eclipse.ui.console.actions.TextViewerAction;
import org.eclipse.ui.handlers.IHandlerActivation;
import org.eclipse.ui.handlers.IHandlerService;
import org.eclipse.ui.part.IPageBookViewPage;
import org.eclipse.ui.texteditor.ITextEditorActionDefinitionIds;
import org.mozilla.javascript.Context;
import org.mozilla.javascript.Scriptable;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.ClientState;
import com.servoy.j2db.IDebugClient;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.scripting.GlobalScope;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Pair;

/**
 * @author jcompagner
 *
 */
public class ScriptConsole extends TextConsole implements IEvaluateConsole
{
	private static final String TEST_SCOPE = "____TEST_SCOPE____";
	private ScriptConsolePage page;
	private IMemento memento;

	/**
	 * @param name
	 * @param consoleType
	 * @param imageDescriptor
	 * @param autoLifecycle
	 */
	public ScriptConsole()
	{
		super("Command Console", "CommandConsole", null, true);

		ScriptConsolePartitioner partitioner = new ScriptConsolePartitioner();
		getDocument().setDocumentPartitioner(partitioner);
		partitioner.connect(getDocument());

	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.console.TextConsole#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.console.TextConsole#getPartitioner()
	 */
	@Override
	protected IConsoleDocumentPartitioner getPartitioner()
	{
		return null;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.console.TextConsole#clearConsole()
	 */
	@Override
	public void clearConsole()
	{
		page.clearConsole();
	}

	public StyledText getTextWidget()
	{
		return page.getViewer().getTextWidget();
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.console.TextConsole#createPage(org.eclipse.ui.console.IConsoleView)
	 */
	@Override
	public IPageBookViewPage createPage(IConsoleView view)
	{
		page = new ScriptConsolePage(this, view);
		return page;
	}

	public static Pair<String, IRootObject> getGlobalScope()
	{
		// get globals scope from main solution or from modules or any global scope
		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (servoyProject != null)
		{
			Pair<String, IRootObject> selectedScope = null;

			for (Pair<String, IRootObject> currentScope : servoyProject.getEditingFlattenedSolution().getScopes())
			{
				if (selectedScope == null) selectedScope = currentScope;
				if (ScriptVariable.GLOBAL_SCOPE.equals(currentScope.getLeft()))
				{
					if (servoyProject.getSolution().getName().equals(currentScope.getRight().getName()))
					{
						return currentScope;
					}
					else
					{
						selectedScope = currentScope;
					}
				}
			}
			return selectedScope;
		}
		return null;
	}

	public static Scriptable getScope(IDebugClient clientState, boolean create)
	{
		Pair<String, IRootObject> scopePair = getGlobalScope();
		String scopeName = scopePair != null ? scopePair.getLeft() : ScriptVariable.GLOBAL_SCOPE;
		GlobalScope ss = clientState.getScriptEngine().getScopesScope().getGlobalScope(scopeName);
		Scriptable scope = null;
		if (ss.has(TEST_SCOPE, ss))
		{
			scope = (Scriptable)ss.get(TEST_SCOPE, ss);
		}
		else if (create)
		{
			try
			{
				scope = Context.enter().newObject(ss);
				ss.putWithoutFireChange(TEST_SCOPE, scope);
				scope.setParentScope(ss);
			}
			finally
			{
				Context.exit();
			}
		}
		return scope;
	}


	/**
	 * @param mem
	 */
	public void saveState(IMemento mem)
	{
		if (page != null)
		{
			page.saveState(mem);
		}
	}


	/**
	 * @param mem
	 */
	public void restoreState(IMemento mem)
	{
		this.memento = mem;
	}


	/**
	 * @author jcompagner
	 *
	 */
	private final class ScriptConsolePage extends TextConsolePage
	{
		private ListViewer clientsList;
		private SashForm form;
		private ScriptConsoleViewer text;
		private IDebugClient selectedClient;
		private final HashMap<Scriptable, StringBuilder> previousScripts = new HashMap<Scriptable, StringBuilder>();
		private TextViewerAction proposalsAction;
		private ActionHandler proposalsHandler;

		private IHandlerActivation activateHandler;
		private TestClientsJob testClientsJob;

		/**
		 * @param console
		 * @param view
		 */
		private ScriptConsolePage(TextConsole console, IConsoleView view)
		{
			super(console, view);
		}

		/**
		 * @param memento
		 */
		public void saveState(IMemento memento)
		{
			if (text != null) text.saveState(memento);
		}

		public void clearConsole()
		{
			text.clear();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.console.TextConsolePage#createControl(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		public void createControl(Composite parent)
		{
			form = new SashForm(parent, SWT.NONE);
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
						selectedClient = (IDebugClient)selection.getFirstElement();
					}
					else selectedClient = null;

					text.setEditable(selectedClient != null);
					if (!UIUtils.isDarkThemeSelected(false))
					{
						if (selectedClient != null)
						{
							text.getTextWidget().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_LIST_BACKGROUND));
						}
						else
						{
							text.getTextWidget().setBackground(Display.getDefault().getSystemColor(SWT.COLOR_WIDGET_BACKGROUND));
						}
					}

				}
			});
			clientsList.setContentProvider(new ClientsContentProvider());
			clientsList.setLabelProvider(new ClientsLabelProvider());
			super.createControl(form);
			form.setWeights(new int[] { 20, 80 });
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.console.TextConsolePage#dispose()
		 */
		@Override
		public void dispose()
		{
			super.dispose();
			IHandlerService handlerService = getSite().getService(IHandlerService.class);
			handlerService.deactivateHandler(activateHandler);
			proposalsHandler.dispose();
			testClientsJob.cancel();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.console.TextConsolePage#getControl()
		 */
		@Override
		public Control getControl()
		{
			return form != null ? form : super.getControl();
		}

		/*
		 * (non-Javadoc)
		 *
		 * @see org.eclipse.ui.console.TextConsolePage#createViewer(org.eclipse.swt.widgets.Composite)
		 */
		@Override
		protected TextConsoleViewer createViewer(Composite parent)
		{
			IActiveClientProvider provider = new IActiveClientProvider()
			{
				public StringBuilder getSelectedClientScript()
				{
					Scriptable scope = getScope(selectedClient, true);
					StringBuilder stringBuilder = previousScripts.get(scope);
					if (stringBuilder == null)
					{
						stringBuilder = new StringBuilder();
						previousScripts.put(scope, stringBuilder);
					}
					return stringBuilder;
				}

				public IDebugClient getSelectedClient()
				{
					return selectedClient;
				}
			};
			IContentAssistProcessor processor = new ContentAssistProcessor(provider);
			ITextHover hover = new TextHover();
			SourceViewerConfiguration cfg = new ScriptConsoleSourceViewerConfiguration(processor, hover);

			text = new ScriptConsoleViewer(parent, ScriptConsole.this, new CommandHandler(provider));
			text.setEditable(false);
			text.configure(cfg);
			if (memento != null) text.restoreState(memento);

			IHandlerService handlerService = getSite().getService(IHandlerService.class);
			proposalsAction = new TextViewerAction(text, ISourceViewer.CONTENTASSIST_PROPOSALS);
			proposalsAction.setEnabled(true);
			proposalsAction.setActionDefinitionId(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS);
			proposalsHandler = new ActionHandler(proposalsAction);
			activateHandler = handlerService.activateHandler(ITextEditorActionDefinitionIds.CONTENT_ASSIST_PROPOSALS, proposalsHandler);

			testClientsJob = new TestClientsJob(Messages.ScriptingConsole_testClientsJobName);
			testClientsJob.schedule(20 * 1000);

			Object[] clients = new Object[0];
			if (ApplicationServerRegistry.get() != null && ApplicationServerRegistry.get().getDebugClientHandler() != null)
			{
				clients = ApplicationServerRegistry.get().getDebugClientHandler().getActiveDebugClients().toArray();
			}
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
			return text;
		}

		private class TestClientsJob extends Job
		{
			private List<IDebugClient> activeClients;
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
				List<IDebugClient> activeDebugClients = ApplicationServerRegistry.get().getDebugClientHandler().getActiveDebugClients();
				if (!activeDebugClients.equals(this.activeClients))
				{
					this.activeClients = activeDebugClients;

					@SuppressWarnings("unchecked")
					Map<Scriptable, StringBuilder> clone = (Map<Scriptable, StringBuilder>)previousScripts.clone();
					previousScripts.clear();
					for (IDebugClient clientState : activeDebugClients)
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
							page.updateSelectionDependentActions();
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
}
