/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.StringTokenizer;

import org.eclipse.dltk.console.ScriptConsoleHistory;
import org.eclipse.dltk.console.ScriptConsolePrompt;
import org.eclipse.dltk.console.ui.AnsiColorHelper;
import org.eclipse.dltk.console.ui.AnsiColorHelper.IAnsiColorHandler;
import org.eclipse.dltk.console.ui.IScriptConsoleViewer;
import org.eclipse.dltk.console.ui.ScriptConsolePartitioner;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.DocumentEvent;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IDocumentListener;
import org.eclipse.jface.text.IDocumentPartitioner;
import org.eclipse.jface.text.TextUtilities;
import org.eclipse.jface.text.hyperlink.HyperlinkManager;
import org.eclipse.jface.text.source.SourceViewerConfiguration;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ST;
import org.eclipse.swt.custom.StyleRange;
import org.eclipse.swt.custom.StyledText;
import org.eclipse.swt.custom.VerifyKeyListener;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.DND;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.console.TextConsole;
import org.eclipse.ui.console.TextConsoleViewer;

import com.servoy.eclipse.core.util.UIUtils;

public class ScriptConsoleViewer extends TextConsoleViewer implements IScriptConsoleViewer
{
	private static final String TEXT_SETTING = "TEXT";
	private static final String HISTORY_SETTING = "HISTORY";

	private class ConsoleDocumentListener implements IDocumentListener
	{
		private String leftOverString;
		private boolean bEnabled = true;
		private boolean handleSynchronously;

		private final ScriptConsolePrompt prompt;

		private int inviteStart = 0;
		private int inviteEnd = 0;

		private IDocument doc;

		private final AnsiColorHelper ansiHelper = new AnsiColorHelper();

		private final List<ScriptConsoleViewer> viewerList = new ArrayList<ScriptConsoleViewer>();

		private void addViewer(ScriptConsoleViewer viewer)
		{
			viewerList.add(viewer);
		}

		private void removeViewer(ScriptConsoleViewer viewer)
		{
			viewerList.remove(viewer);
		}

		protected void connectListener()
		{
			doc.addDocumentListener(this);
		}

		protected void disconnectListener()
		{
			doc.removeDocumentListener(this);
		}

		public void clear()
		{
			try
			{
				disconnectListener();
				doc.set("");
				ScriptConsoleViewer viewer;
				for (Object element : viewerList)
				{
					viewer = (ScriptConsoleViewer)element;
					IDocumentPartitioner partitioner = viewer.getDocument().getDocumentPartitioner();
					if (partitioner instanceof ScriptConsolePartitioner)
					{
						ScriptConsolePartitioner scriptConsolePartitioner = (ScriptConsolePartitioner)partitioner;
						scriptConsolePartitioner.clearRanges();
					}
				}
				appendInvitation();
				for (Object element : viewerList)
				{
					((ScriptConsoleViewer)element).setCaretPosition(doc.getLength());
				}
			}
			catch (BadLocationException e)
			{
				e.printStackTrace();
			}
			finally
			{
				connectListener();
			}
		}

		public ConsoleDocumentListener(ScriptConsolePrompt prompt)
		{
			this.prompt = prompt;
			this.doc = null;
		}

		public void setDocument(IDocument doc)
		{
			if (this.doc != null)
			{
				disconnectListener();
			}

			this.doc = doc;

			if (this.doc != null)
			{
				connectListener();
			}
		}

		public void documentAboutToBeChanged(DocumentEvent event)
		{

		}

		protected void handleCommandLine(final String command) throws IOException
		{
			if (handleSynchronously)
			{
				IScriptExecResult result = commandHandler.handleCommand(command);
				processResult(result);
				return;
			}

			Thread handlerThread = new Thread("Command Line Handler")
			{

				@Override
				public void run()
				{
					try
					{
						final IScriptExecResult result = commandHandler.handleCommand(command);

						getControl().getDisplay().asyncExec(new Runnable()
						{

							public void run()
							{
								processResult(result);
							}

						});
					}
					catch (IOException ixcn)
					{
						ixcn.printStackTrace();
					}
				}

			};
			handlerThread.setDaemon(true);
			handlerThread.setPriority(Thread.MIN_PRIORITY);
			handlerThread.start();
		}

		protected void appendText(int offset, String text) throws BadLocationException
		{
			doc.replace(offset, 0, text);
		}

		protected void processText(final int originalOffset, String content, boolean isInput, boolean isError, final boolean shouldReveal,
			final boolean shouldRedraw) throws BadLocationException
		{
			ansiHelper.processText(originalOffset == -1 ? doc.getLength() : originalOffset, content, isInput, isError, new IAnsiColorHandler()
			{

				public void handleText(int start, String cntnt, boolean input, boolean error) throws BadLocationException
				{
					appendText(start, cntnt);
					addToPartitioner(start, cntnt, input, error);
				}

				public void processingComplete(int start, int length)
				{
					for (Object element : viewerList)
					{
						final ScriptConsoleViewer viewer = (ScriptConsoleViewer)element;
						if (shouldReveal == true)
						{
							viewer.setCaretPosition(doc.getLength());
							viewer.revealEndOfDocument();
						}

						if (shouldRedraw == true)
						{
							if (viewer.getTextWidget() != null)
							{
								viewer.getTextWidget().redrawRange(start, length, true);
							}
						}
					}
				}

			});
		}

		protected void processResult(final IScriptExecResult result)
		{
			disconnectListener();
			try
			{
				if (result != null)
				{
					String output = result.getOutput();
					if (output != null && output.length() != 0)
					{
						ansiHelper.reset();
						processText(-1, output, false, result.isError(), false, true);
						appendDelimeter();
					}
				}
				appendInvitation();
				appendLeftOverStrings();
			}
			catch (BadLocationException bxcn)
			{
				bxcn.printStackTrace();
			}
			finally
			{
				connectListener();
			}
		}

		private void addToPartitioner(ScriptConsoleViewer viewer, StyleRange style)
		{
			IDocumentPartitioner partitioner = viewer.getDocument().getDocumentPartitioner();
			if (partitioner instanceof ScriptConsolePartitioner)
			{
				ScriptConsolePartitioner scriptConsolePartitioner = (ScriptConsolePartitioner)partitioner;
				scriptConsolePartitioner.addRange(style);
			}
		}

		protected void addToPartitioner(int start, String content, boolean isInput, boolean isError)
		{
			// ssanders: Content has to be tokenized in order for style and
			// hyperlinks to display correctly
			StringTokenizer tokenizer = new StringTokenizer(content, " \t\n\r\f@#=|,()[]{}<>'\"", true);
			String token;
			int tokenStart = start;
			ScriptConsoleViewer viewer;
			while (tokenizer.hasMoreTokens() == true)
			{
				token = tokenizer.nextToken();

				for (Object element : viewerList)
				{
					viewer = (ScriptConsoleViewer)element;
					boolean isDarkTheme = UIUtils.isDarkThemeSelected(false);
					if (isDarkTheme)
					{
						if (isInput == true)
						{
							addToPartitioner(viewer, new StyleRange(tokenStart, token.length(), AnsiColorHelper.COLOR_WHITE, null, SWT.BOLD));
						}
						else if (isError)
						{
							addToPartitioner(viewer, new StyleRange(tokenStart, token.length(), AnsiColorHelper.COLOR_RED, null, SWT.BOLD));
						}
						else
						{
							addToPartitioner(viewer, new StyleRange(tokenStart, token.length(), AnsiColorHelper.COLOR_CYAN, null, SWT.BOLD));
						}
					}
					else
					{
						if (isInput == true)
						{
							addToPartitioner(viewer, new StyleRange(tokenStart, token.length(), AnsiColorHelper.COLOR_BLACK, null, SWT.BOLD));
						}
						else
						{
							addToPartitioner(viewer, ansiHelper.resolveStyleRange(tokenStart, token.length(), isError));
						}
					}
				}

				tokenStart += token.length();
			}
			for (Object element : viewerList)
			{
				viewer = (ScriptConsoleViewer)element;
				viewer.getTextWidget().redraw();
			}

		}

		protected void processAddition(int offset)
		{
			if (!bEnabled)
			{
				return;
			}
			try
			{
				final String delim = TextUtilities.getDefaultLineDelimiter(doc);
				String text = doc.get(offset, doc.getLength() - offset);
				doc.replace(offset, text.length(), "");
				text = text.replaceAll("\r\n|\n|\r", delim);
				int start = 0;
				int index;
				boolean commandHandled = false;
				while ((index = text.indexOf(delim, start)) != -1)
				{
					if (index > start)
					{
						processText(getCommandLineOffset(), text.substring(start, index), true, false, false, true);
					}
					final String commandLine = getCommandLine();
					processText(-1, delim, true, false, false, true);
					inviteStart = inviteEnd = doc.getLength();
					history.add(commandLine);
					start = index + delim.length();
					handleCommandLine(commandLine);
					commandHandled = true;
				}
				if (start < text.length())
				{
					if (commandHandled)
					{
						leftOverString = text.substring(start, text.length());
					}
					else
					{
						processText(-1, text.substring(start, text.length()), true, false, false, true);
					}
				}
			}
			catch (BadLocationException e)
			{
				if (DLTKCore.DEBUG)
				{
					e.printStackTrace();
				}
			}
			catch (IOException e)
			{
				if (DLTKCore.DEBUG)
				{
					e.printStackTrace();
				}
			}
		}

		public void documentChanged(final DocumentEvent event)
		{
			ansiHelper.disableWhile(new Runnable()
			{

				public void run()
				{
					disconnectListener();
					try
					{
						processAddition(event.getOffset());
					}
					finally
					{
						connectListener();
					}
				}

			});
		}

		public void appendInvitation() throws BadLocationException
		{
			inviteStart = doc.getLength();
			processText(inviteStart, prompt.toString(), true, false, true, true);
			inviteEnd = doc.getLength();
		}

		public void appendDelimeter() throws BadLocationException
		{
			processText(-1, TextUtilities.getDefaultLineDelimiter(doc), false, false, false, true);
		}

		public void appendLeftOverStrings() throws BadLocationException
		{
			if (leftOverString != null)
			{
				processText(-1, leftOverString, true, false, false, true);
				leftOverString = null;
			}
		}

		public int getCommandLineOffset()
		{
			return inviteEnd;
		}

		public int getCommandLineLength()
		{
			return doc.getLength() - inviteEnd;
		}

		public String getCommandLine() throws BadLocationException
		{
			return doc.get(getCommandLineOffset(), getCommandLineLength());
		}

		public void setCommandLine(final String command)
		{
			ansiHelper.disableWhile(new Runnable()
			{

				public void run()
				{
					try
					{
						doc.replace(getCommandLineOffset(), getCommandLineLength(), command);
					}
					catch (BadLocationException bxcn)
					{
						bxcn.printStackTrace();
					}
				}

			});
		}
	}

	/**
	 * @since 2.0
	 */
	public class ScriptConsoleStyledText extends StyledText
	{

		public ScriptConsoleStyledText(Composite parent, int style)
		{
			super(parent, (style | SWT.WRAP));
		}

		@Override
		public void invokeAction(int action)
		{
			if (isEditable() && isCaretOnLastLine())
			{
				switch (action)
				{
					case ST.LINE_UP :
						updateSelectedLine();
						if (history.prev())
						{
							getDocumentListener().setCommandLine(history.get());
							setCaretOffset(getDocument().getLength());
						}
						else
						{
							beep();
						}
						return;

					case ST.LINE_DOWN :
						updateSelectedLine();
						if (history.next())
						{
							getDocumentListener().setCommandLine(history.get());
							setCaretOffset(getDocument().getLength());
						}
						else
						{
							beep();
						}
						return;

					case ST.DELETE_PREVIOUS :
						if (getCaretOffset() <= getCommandLineOffset() && getSelectionCount() == 0)
						{
							return;
						}
						break;

					case ST.DELETE_NEXT :
						if (getCaretOffset() < getCommandLineOffset())
						{
							return;
						}
						break;

					case ST.DELETE_WORD_PREVIOUS :
						return;

					case ST.SELECT_LINE_START :
						if (isCaretOnLastLine())
						{
							final int prevCaret = getCaretOffset();
							final Point prevSelection = getSelection();
							final int caret = getCommandLineOffset();
							if (prevCaret == prevSelection.x)
							{
								setSelection(prevSelection.y, caret);
							}
							else if (prevCaret == prevSelection.y)
							{
								setSelection(prevSelection.x, caret);
							}
							else
							{
								setCaretOffset(caret);
							}
							return;
						}
						break;
					case ST.LINE_START :
						if (isCaretOnLastLine())
						{
							setCaretOffset(getCommandLineOffset());
							return;
						}
						break;
					case ST.COLUMN_PREVIOUS :
					case ST.SELECT_COLUMN_PREVIOUS :
						if (isCaretOnLastLine() && getCaretOffset() == getCommandLineOffset())
						{
							return;
						}
				}

				super.invokeAction(action);

				if (isCaretOnLastLine() && getCaretOffset() <= getCommandLineOffset())
				{
					setCaretOffset(getCommandLineOffset());
				}
			}
			else
			{

				super.invokeAction(action);
			}
		}

		private void updateSelectedLine()
		{
			try
			{
				history.updateSelectedLine(getDocumentListener().getCommandLine());
			}
			catch (BadLocationException e)
			{
				if (DLTKCore.DEBUG)
				{
					e.printStackTrace();
				}
			}
		}

		private void beep()
		{
			getDisplay().beep();
		}

		@Override
		public void paste()
		{
			if (isCaretOnLastLine())
			{
				getDocumentListener().ansiHelper.disableWhile(new Runnable()
				{

					public void run()
					{
						// ssanders: Process the lines one-by-one in
						// order to have the proper prompting
						getDocumentListener().handleSynchronously = true;
						try
						{
							checkWidget();
							Clipboard clipboard = new Clipboard(getDisplay());
							TextTransfer plainTextTransfer = TextTransfer.getInstance();
							String text = (String)clipboard.getContents(plainTextTransfer, DND.CLIPBOARD);
							clipboard.dispose();
							if (text != null && text.length() > 0)
							{
								if (text.indexOf("\n") == -1)
								{
									Point selectedRange = getSelectedRange();
									getTextWidget().insert(text);
									setCaretOffset(selectedRange.x + text.length());

								}
								else
								{
									StringTokenizer tokenizer = new StringTokenizer(text, "\n\r");
									while (tokenizer.hasMoreTokens() == true)
									{
										final String finText = tokenizer.nextToken();
										insertText(finText + "\n");
									}
								}
							}
						}
						finally
						{
							getDocumentListener().handleSynchronously = false;
						}
					}

				});
			}
		}

	}

	private final ScriptConsoleHistory history;
	private ConsoleDocumentListener documentListener;
	private final ICommandHandler commandHandler;

	public int getCaretPosition()
	{
		return getTextWidget().getCaretOffset();
	}

	public void enableProcessing()
	{
		getDocumentListener().bEnabled = true;
	}

	public void disableProcessing()
	{
		getDocumentListener().bEnabled = false;
	}

	public void setCaretPosition(final int offset)
	{
		if (getTextWidget() != null)
		{
			getTextWidget().getDisplay().asyncExec(new Runnable()
			{

				public void run()
				{
					if (getTextWidget() != null)
					{
						getTextWidget().setCaretOffset(offset);
					}
				}
			});
		}
	}

	// public int beginLineOffset() throws BadLocationException {
	// IDocument doc = getDocument();
	// int offset = getCaretPosition();
	// int line = doc.getLineOfOffset(offset);
	// return offset - doc.getLineOffset(line);
	// }

	protected boolean isCaretOnLastLine()
	{
		try
		{
			IDocument doc = getDocument();
			int line = doc.getLineOfOffset(getCaretPosition());
			return line == doc.getNumberOfLines() - 1;
		}
		catch (BadLocationException e)
		{
			if (DLTKCore.DEBUG)
			{
				e.printStackTrace();
			}
			return false;
		}
	}

	@Override
	protected StyledText createTextWidget(Composite parent, int styles)
	{
		return new ScriptConsoleStyledText(parent, styles | SWT.BORDER);
	}

	private ConsoleDocumentListener getDocumentListener()
	{
		if (documentListener == null)
		{
			documentListener = new ConsoleDocumentListener(new ScriptConsolePrompt("=>", "->"));
		}
		return documentListener;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.text.source.SourceViewer#setDocument(org.eclipse.jface.text.IDocument)
	 */
	@Override
	public void setDocument(IDocument document)
	{
		super.setDocument(document);
	}

	public ScriptConsoleViewer(Composite parent, final TextConsole console, ICommandHandler commandHandler)
	{
		super(parent, console);
		this.commandHandler = commandHandler;
		this.history = new ScriptConsoleHistory();

		getDocument().addDocumentListener(getDocumentListener());
		getDocumentListener().addViewer(this);
		getDocumentListener().setDocument(getDocument());

		final StyledText styledText = getTextWidget();

		// styledText.setEditable(false);

		// Correct keyboard actions
		styledText.addFocusListener(new FocusListener()
		{

			public void focusGained(FocusEvent e)
			{
				setCaretPosition(getDocument().getLength());
				styledText.removeFocusListener(this);
			}

			public void focusLost(FocusEvent e)
			{

			}
		});

		styledText.setKeyBinding('X' | SWT.MOD1, ST.COPY);


		if (getDocumentListener().viewerList.size() == 1)
		{
			clear();
		}
	}

	// IConsoleTextViewer
	public String getCommandLine()
	{
		try
		{
			return getDocumentListener().getCommandLine();
		}
		catch (BadLocationException e)
		{
			return null;
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.text.source.SourceViewer#configure(org.eclipse.jface.text.source.SourceViewerConfiguration)
	 */
	@Override
	public void configure(SourceViewerConfiguration configuration)
	{
		super.configure(configuration);
		appendVerifyKeyListener(new VerifyKeyListener()
		{
			public void verifyKey(VerifyEvent event)
			{
				try
				{
					if (event.character != '\0')
					{
						if ((event.stateMask & SWT.MOD1) == 0)
						{
							// Printable character
							// ssanders: Ensure selection is on last line
							ConsoleDocumentListener listener = getDocumentListener();
							int selStart = getSelectedRange().x;
							int selEnd = (getSelectedRange().x + getSelectedRange().y);
							int clOffset = listener.getCommandLineOffset();
							int clLength = listener.getCommandLineLength();
							if (selStart < clOffset)
							{
								int selLength;

								if (selEnd < clOffset)
								{
									selStart = (clOffset + clLength);
									selLength = 0;
								}
								else
								{
									selStart = clOffset;
									selLength = (selEnd - selStart);
								}

								setSelectedRange(selStart, selLength);
							}

							if (getCaretPosition() < getDocumentListener().getCommandLineOffset())
							{
								event.doit = false;
								return;
							}
						}

						if (event.character == SWT.CR)
						{
							getTextWidget().setCaretOffset(getDocument().getLength());
							return;
						}

						// ssanders: Avoid outputting " " when invoking
						// completion on Mac OS X
						if (event.keyCode == 32 && (event.stateMask & SWT.CTRL) > 0)
						{
							event.doit = false;
							return;
						}

						// ssanders: Avoid outputting "<Tab>" when invoking
						// completion on Mac OS X
						if (event.keyCode == SWT.TAB)
						{
							event.doit = false;
							return;
						}
					}
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		});
	}

	public int getCommandLineOffset()
	{
		return getDocumentListener().getCommandLineOffset();
	}

	public void clear()
	{
		getDocumentListener().clear();
	}

	public void insertText(String text)
	{
		getTextWidget().append(text);
	}

	@Override
	public boolean canDoOperation(int operation)
	{
		boolean canDoOperation = super.canDoOperation(operation);

		if (canDoOperation)
		{
			switch (operation)
			{
				case CUT :
				case DELETE :
//				case PASTE :
				case SHIFT_LEFT :
				case SHIFT_RIGHT :
				case PREFIX :
				case STRIP_PREFIX :
					canDoOperation = isCaretOnLastLine();
			}
		}

		return canDoOperation;
	}

	@Override
	public void activatePlugins()
	{
		fHyperlinkManager = new HyperlinkManager(HyperlinkManager.LONGEST_REGION_FIRST);
		fHyperlinkManager.install(this, fHyperlinkPresenter, fHyperlinkDetectors, fHyperlinkStateMask);

		super.activatePlugins();
	}

	public void dispose()
	{
		getDocumentListener().removeViewer(this);
		getDocument().removeDocumentListener(getDocumentListener());
	}

	/**
	 * @param memento
	 */
	public void saveState(IMemento memento)
	{
		memento.putString(HISTORY_SETTING, history.saveState());
//		memento.putString(TEXT_SETTING, getDocument().get());
	}

	/**
	 * @param memento
	 */
	public void restoreState(IMemento memento)
	{
		history.restoreState(memento.getString(HISTORY_SETTING));
//		getDocument().set(memento.getString(TEXT_SETTING));
	}

}
