package com.servoy.eclipse.debug.scriptingconsole;

import java.util.Arrays;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dltk.codeassist.ICompletionEngine;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.javascript.core.JavaScriptLanguageToolkit;
import org.eclipse.dltk.ui.text.completion.AbstractScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.CompletionProposalComparator;
import org.eclipse.dltk.ui.text.completion.IScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalCollector;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;

import com.servoy.eclipse.debug.script.ValueCollectionProvider;
import com.servoy.j2db.ClientState;

/**
 * @author jcompagner
 *
 */
final class ContentAssistProcessor implements IContentAssistProcessor
{
	private final IActiveClientProvider provider;

	public ContentAssistProcessor(IActiveClientProvider provider)
	{
		this.provider = provider;
	}

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
		ClientState selectedClient = provider.getSelectedClient();
		if (selectedClient != null)
		{
			final ICompletionEngine engine = DLTKLanguageManager.getCompletionEngine(JavaScriptLanguageToolkit.getDefault().getNatureId());
			if (engine == null)
			{
				return new ICompletionProposal[0];
			}
			StringBuilder sb = provider.getSelectedClientScript();
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