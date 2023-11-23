package com.servoy.eclipse.aibridge.editors;

import java.io.BufferedReader;
import java.io.InputStreamReader;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.internal.ui.editor.JavaScriptEditor;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.ITextSelection;
import org.eclipse.jface.text.TextSelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.ProgressAdapter;
import org.eclipse.swt.browser.ProgressEvent;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.swt.widgets.ToolItem;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.texteditor.ITextEditor;

import com.servoy.eclipse.model.util.ServoyLog;

public class DualEditor extends MultiPageEditorPart
{

	public static final String ID = "com.servoy.eclipse.aibridge.dualeditor";

	private SashForm sashForm;
	private Composite jsComposite;
	private Browser browser;
	private IEditorPart editor;

	private Composite leftComposite;
	private Composite rightComposite;
	private ToolBar toolBar;
	private ToolItem copySelectionItem;

	@Override
	protected void createPages()
	{
		createSashForm();
		configureEditor();
		configureBrowser();
		configureTabFolder();
	}

	private void createSashForm()
	{
		DualEditorInput editorInput = (DualEditorInput)getEditorInput();
		sashForm = new SashForm(getContainer(), SWT.HORIZONTAL);

		leftComposite = new Composite(sashForm, SWT.NONE);
		rightComposite = new Composite(sashForm, SWT.NONE);
		leftComposite.setLayout(new GridLayout(1, false));
		rightComposite.setLayout(new GridLayout(1, false));
		toolBar = new ToolBar(rightComposite, SWT.NONE);

		GridData gd = new GridData();
		toolBar.setLayoutData(gd);
		gd.heightHint = SWT.DEFAULT;

		copySelectionItem = new ToolItem(toolBar, SWT.PUSH);

		ImageDescriptor copyImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", "icons/leftarrow.png");
		copySelectionItem.setImage(copyImageDescriptor.createImage());
		ImageDescriptor leftArrowPressedImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge",
			"icons/leftarrow_pressed.png");
		final Image pressedImage = leftArrowPressedImageDescriptor.createImage();
		copySelectionItem.setToolTipText("Copy selection to editor");
		copySelectionItem.addListener(SWT.Selection, event -> handleCopyAction());
		copySelectionItem.setEnabled(false);

		copySelectionItem.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				if (!copySelectionItem.getEnabled()) return;
				copySelectionItem.setImage(pressedImage);
				Display.getCurrent().timerExec(100, new Runnable()
				{
					@Override
					public void run()
					{
						copySelectionItem.setImage(copyImageDescriptor.createImage());
					}
				});
			}
		});
		Label leftTitle = new Label(leftComposite, SWT.NONE);
		leftTitle.setText(editorInput.getLeftTitle());
		Label rightTitle = new Label(rightComposite, SWT.NONE);
		rightTitle.setText(editorInput.getRightTitle());

		jsComposite = new Composite(leftComposite, SWT.NONE);
		jsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

		toolBar.setVisible(false);
	}

	private void configureEditor()
	{
		DualEditorInput editorInput = (DualEditorInput)getEditorInput();
		IFile file = editorInput.getInputFile();

		String fileContent = readFileContent(file);
		String selectionContent = editorInput.getSelectionContent();

		try
		{
			if (file != null && fileContent.contains(selectionContent))
			{
				editor = new JavaScriptEditor();
				IPath jsPath = Path.fromOSString(file.getName().substring(0, file.getName().lastIndexOf('.')) + ".js");
				IFile jsFile = file.getParent().getFile(jsPath);
				FileEditorInput fei = new FileEditorInput(jsFile);
				setPageText(addPage(editor, fei), "");
				scrollToSelection(fileContent, selectionContent, false);
				toolBar.setVisible(true);
				GridData gd = (GridData)toolBar.getLayoutData();
				gd.heightHint = SWT.DEFAULT;
				rightComposite.layout();
			}
			else
			{
				editor = new ReadOnlyTextEditor(editorInput.getSelectionContent());
				setPageText(addPage(editor, editorInput), "");
				toolBar.setVisible(false);
				GridData gd = (GridData)toolBar.getLayoutData();
				gd.heightHint = 0; // Set the height to zero
				rightComposite.layout();
			}
		}
		catch (PartInitException e)
		{
			ServoyLog.logError(e);
		}
	}


	private void configureBrowser()
	{
		DualEditorInput editorInput = (DualEditorInput)getEditorInput();
		browser = new Browser(rightComposite, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		browser.setText(editorInput.getHtmlContent());
		addBrowserListeners();
		addPage(sashForm);
	}

	private void addBrowserListeners()
	{
		browser.addProgressListener(new ProgressAdapter()
		{
			@Override
			public void completed(ProgressEvent event)
			{
				browser.execute(
					"document.onselectionchange = function() {" +
						"    setTimeout(function() {" + // Adding a delay to ensure the final text is selected
						"        window.status = 'selected:' + window.getSelection().toString().trim();" +
						"    }, 50);" +
						"};");

				browser.execute(
					"document.onmousedown = function() {" +
						"    if(window.getSelection().toString().trim() === '') {" +
						"        window.status = 'selected:';" +
						"    }" +
						"};");
			}
		});

		browser.addStatusTextListener(event -> {
			if (event.text.startsWith("selected:"))
			{
				String selectedText = event.text.replace("selected:", "");
				copySelectionItem.setEnabled(selectedText != null && !selectedText.trim().isEmpty());
			}
		});
	}

	private String getBrowserSelection()
	{
		return (String)browser.evaluate("return window.getSelection().toString();");
	}

	private void configureTabFolder()
	{
		CTabFolder tabFolder = (CTabFolder)getContainer();
		tabFolder.setBorderVisible(false);
		tabFolder.setTabHeight(0);
		tabFolder.layout();
		tabFolder.redraw();
	}

	@Override
	public boolean isDirty()
	{
		if (editor instanceof JavaScriptEditor javaScriptEditor)
		{
			return javaScriptEditor.isDirty();
		}
		return false;
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		if (editor instanceof JavaScriptEditor javaScriptEditor)
		{
			javaScriptEditor.doSave(monitor);
		}
	}

	@Override
	public void doSaveAs()
	{
		if (editor instanceof JavaScriptEditor javaScriptEditor)
		{
			javaScriptEditor.doSaveAs();
		}
	}

	@Override
	public boolean isSaveAsAllowed()
	{
		if (editor instanceof JavaScriptEditor javaScriptEditor)
		{
			return javaScriptEditor.isSaveAsAllowed();
		}
		return false;
	}

	@Override
	public void addPage(int index, IEditorPart editor, IEditorInput input) throws PartInitException
	{
		IEditorSite site = createSite(editor);
		editor.init(site, input);
		jsComposite.setLayout(new FillLayout());
		editor.createPartControl(jsComposite);
		Item item = createItem(index, sashForm);
		item.setData(editor);
	}


	@Override
	public void dispose()
	{

		if (editor != null)
		{
			editor.dispose();
		}
		if (jsComposite != null)
		{
			jsComposite.dispose();
		}
		if (sashForm != null)
		{
			sashForm.dispose();
		}
		if (toolBar != null && !toolBar.isDisposed())
		{
			for (ToolItem item : toolBar.getItems())
			{
				if (item.getImage() != null)
				{
					item.getImage().dispose();
				}
			}
			toolBar.dispose();
		}
		super.dispose();
	}

	private String readFileContent(IFile file)
	{
		if (file == null) return "";

		StringBuilder contentBuilder = new StringBuilder();

		try (
			InputStreamReader isReader = new InputStreamReader(file.getContents(), file.getCharset());
			BufferedReader reader = new BufferedReader(isReader))
		{
			String line;
			while ((line = reader.readLine()) != null)
			{
				contentBuilder.append(line);
				contentBuilder.append(System.lineSeparator());
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return contentBuilder.toString();
	}

	private int[] findSelectionInRange(String fileContent, String selection)
	{
		int offset = fileContent.indexOf(selection);
		if (offset != -1)
		{
			return new int[] { offset, selection.length() };
		}
		return null;
	}

	private void scrollToSelection(String fileContent, String selection, boolean highlightSelection)
	{
		int[] selectionRange = findSelectionInRange(fileContent, selection);
		if (selectionRange != null)
		{
			IDocument document = ((ITextEditor)editor).getDocumentProvider().getDocument(editor.getEditorInput());
			int offset = document.get().indexOf(selection);
			if (offset == -1) return;
			((ITextEditor)editor).setHighlightRange(offset, selection.length(), true);
			if (highlightSelection)
			{
				ISelectionProvider selectionProvider = editor.getSite().getSelectionProvider();
				ITextSelection textSelection = new TextSelection(document, offset, selection.length());
				selectionProvider.setSelection(textSelection);
			}

		}
	}

	private void handleCopyAction()
	{
		String selectedText = getBrowserSelection();
		IDocument doc = ((ITextEditor)editor).getDocumentProvider().getDocument(editor.getEditorInput());

		ITextSelection textSelection = (ITextSelection)((ITextEditor)editor).getSelectionProvider().getSelection();
		int offset = textSelection.getOffset();

		try
		{
			if (textSelection.getLength() > 0)
			{
				doc.replace(offset, textSelection.getLength(), selectedText);
			}
			else
			{
				doc.replace(offset, 0, selectedText);
			}
		}
		catch (BadLocationException e)
		{
			ServoyLog.logError(e);
		}
	}
}
