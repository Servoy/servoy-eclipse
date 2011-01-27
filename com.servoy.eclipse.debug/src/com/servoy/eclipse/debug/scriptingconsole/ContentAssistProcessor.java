package com.servoy.eclipse.debug.scriptingconsole;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.dltk.codeassist.ICompletionEngine;
import org.eclipse.dltk.compiler.env.IModuleSource;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.DLTKLanguageManager;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.javascript.core.JavaScriptLanguageToolkit;
import org.eclipse.dltk.ui.DLTKPluginImages;
import org.eclipse.dltk.ui.DLTKUILanguageManager;
import org.eclipse.dltk.ui.IDLTKUILanguageToolkit;
import org.eclipse.dltk.ui.templates.ScriptTemplateContext;
import org.eclipse.dltk.ui.templates.ScriptTemplateContextType;
import org.eclipse.dltk.ui.templates.ScriptTemplateProposal;
import org.eclipse.dltk.ui.text.completion.AbstractScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.CompletionProposalComparator;
import org.eclipse.dltk.ui.text.completion.IScriptCompletionProposal;
import org.eclipse.dltk.ui.text.completion.ScriptCompletionProposalCollector;
import org.eclipse.dltk.utils.TextUtils;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.text.ITextViewer;
import org.eclipse.jface.text.Region;
import org.eclipse.jface.text.contentassist.ICompletionProposal;
import org.eclipse.jface.text.contentassist.IContentAssistProcessor;
import org.eclipse.jface.text.contentassist.IContextInformation;
import org.eclipse.jface.text.contentassist.IContextInformationValidator;
import org.eclipse.jface.text.templates.Template;
import org.eclipse.jface.text.templates.TemplateBuffer;
import org.eclipse.jface.text.templates.TemplateContext;
import org.eclipse.jface.text.templates.TemplateException;

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

			ArrayList<ICompletionProposal> list = new ArrayList<ICompletionProposal>();
			String prefix = extractPrefix(viewer, offset);
			IRegion region = new Region(offset - prefix.length(), prefix.length());
			ScriptTemplateContextType contextType = new ScriptTemplateContextType()
			{
				/*
				 * (non-Javadoc)
				 * 
				 * @see org.eclipse.dltk.ui.templates.ScriptTemplateContextType#createContext(org.eclipse.jface.text.IDocument, int, int,
				 * org.eclipse.dltk.core.ISourceModule)
				 */
				@Override
				public ScriptTemplateContext createContext(IDocument document, int completionPosition, int length, ISourceModule sourceModule)
				{
					return new ScriptTemplateContext(this, document, completionPosition, length, sourceModule)
					{
						/*
						 * (non-Javadoc)
						 * 
						 * @see org.eclipse.dltk.ui.templates.ScriptTemplateContext#evaluate(org.eclipse.jface.text.templates.Template)
						 */
						@Override
						public TemplateBuffer evaluate(Template template) throws BadLocationException, TemplateException
						{
							if (!canEvaluate(template))
							{
								return null;
							}
							Template copy = template;
							final String[] lines = TextUtils.splitLines(copy.getPattern());
							if (lines.length > 1)
							{
								StringBuilder sb1 = new StringBuilder();
								for (String line : lines)
								{
									sb1.append(line);
								}
								copy = new Template(copy.getName(), copy.getDescription(), copy.getContextTypeId(), sb1.toString(), copy.isAutoInsertable());
							}
							return super.evaluate(copy);
						}
					};
				}
			};
			TemplateContext context = contextType.createContext(viewer.getDocument(), offset - prefix.length(), prefix.length(), (ISourceModule)modelElement);
			IDLTKUILanguageToolkit languageToolkit = DLTKUILanguageManager.getLanguageToolkit(JavaScriptLanguageToolkit.getDefault().getNatureId());
			Template[] templates = languageToolkit.getEditorTemplates().getTemplateStore().getTemplates();
			for (Template template : templates)
			{
				if (template.getName().startsWith(prefix))
				{
					list.add(new ScriptTemplateProposal(template, context, region, DLTKPluginImages.get(DLTKPluginImages.IMG_OBJS_TEMPLATE), 1));
				}
			}


			IScriptCompletionProposal[] scriptCompletionProposals = collector.getScriptCompletionProposals();

			int commandLineStart = ((ScriptConsoleViewer)viewer).getCommandLineOffset() - (sb != null ? sb.length() : 0);
			for (IScriptCompletionProposal scriptCompletionProposal : scriptCompletionProposals)
			{
				int replacementOffset = ((AbstractScriptCompletionProposal)scriptCompletionProposal).getReplacementOffset();
				((AbstractScriptCompletionProposal)scriptCompletionProposal).setReplacementOffset(commandLineStart + replacementOffset);
			}
			list.addAll(Arrays.asList(scriptCompletionProposals));
			Collections.sort(list, new CompletionProposalComparator());
			return list.toArray(new ICompletionProposal[list.size()]);
		}
		return new ICompletionProposal[0];
	}

	protected String extractPrefix(ITextViewer viewer, int offset)
	{
		int i = offset;
		IDocument document = viewer.getDocument();
		if (i > document.getLength()) return ""; //$NON-NLS-1$

		try
		{
			while (i > 0)
			{
				char ch = document.getChar(i - 1);
				if (!Character.isJavaIdentifierPart(ch)) break;
				i--;
			}

			return document.get(i, offset - i);
		}
		catch (BadLocationException e)
		{
			return ""; //$NON-NLS-1$
		}
	}
}