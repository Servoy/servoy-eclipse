package com.servoy.eclipse.aibridge.editors;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Path;
import org.eclipse.dltk.javascript.internal.ui.editor.JavaScriptEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.CTabFolder;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Item;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.ui.part.MultiPageEditorPart;

public class DualEditor extends MultiPageEditorPart {

    public static final String ID = "com.servoy.eclipse.aibridge.sbsme";

    private SashForm sashForm;
    private Composite jsComposite;
    private Browser browser;
    private JavaScriptEditor editor;
    
    Composite leftComposite;
    Composite rightComposite;

    @Override
    protected void createPages() {
        createSashForm();
        configureJavaScriptEditor();
        configureBrowser();
        configureTabFolder();
    }

    private void createSashForm() {
    	DualEditorInput editorInput = (DualEditorInput) getEditorInput();
        sashForm = new SashForm(getContainer(), SWT.HORIZONTAL);
        
        leftComposite = new Composite(sashForm, SWT.NONE);
        rightComposite = new Composite(sashForm, SWT.NONE);
        leftComposite.setLayout(new GridLayout(1, false));
        rightComposite.setLayout(new GridLayout(1, false));

        Label leftTitle = new Label(leftComposite, SWT.NONE);
        leftTitle.setText(editorInput.getLeftTitle());
        Label rightTitle = new Label(rightComposite, SWT.NONE);
        rightTitle.setText(editorInput.getRightTitle());
        
        jsComposite = new Composite(leftComposite, SWT.NONE);
        jsComposite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
    }

    private void configureJavaScriptEditor() {
        editor = new JavaScriptEditor();
        try {
            DualEditorInput editorInput = (DualEditorInput) getEditorInput();
            IFile file = editorInput.getInputFile();
            if (file != null) {
            	IPath jsPath = Path.fromOSString(file.getName().substring(0, file.getName().lastIndexOf('.')) + ".js");
                IFile jsFile = file.getParent().getFile(jsPath);
                FileEditorInput fei = new FileEditorInput(jsFile);
                setPageText(addPage((IEditorPart) editor, fei), "");
            } else {
            	//TODO: still not working, need to complete several parts in the DualEditorInput
            	setPageText(addPage((IEditorPart) editor, editorInput), "");
            }
            
            
        } catch (PartInitException e) {
            e.printStackTrace();
        }
    }

    private void configureBrowser() {
        DualEditorInput editorInput = (DualEditorInput) getEditorInput();
        browser = new Browser(rightComposite, SWT.NONE);
        browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
        browser.setText(editorInput.getHtmlContent());
        addPage(sashForm);
    }

    private void configureTabFolder() {
        CTabFolder tabFolder = (CTabFolder) getContainer();
        tabFolder.setBorderVisible(false);
        tabFolder.setTabHeight(0);
        tabFolder.layout();
        tabFolder.redraw();
    }

    @Override
    public void doSave(IProgressMonitor monitor) {
        // Implement saving logic here
    }

    @Override
    public void doSaveAs() {
        // Implement "Save As" logic if supported
    }

    @Override
    public boolean isSaveAsAllowed() {
        return false; // or `true` if "Save As" should be allowed
    }

    @Override
    public void addPage(int index, IEditorPart editor, IEditorInput input) throws PartInitException {
        IEditorSite site = createSite(editor);
        editor.init(site, input);
        jsComposite.setLayout(new FillLayout());
        editor.createPartControl(jsComposite);
        Item item = createItem(index, sashForm);
        item.setData(editor);
    }

    
    @Override
    public void dispose() {
        if (editor != null) {
            editor.dispose();
        }
        if (jsComposite != null) {
            jsComposite.dispose();
        }
        if (sashForm != null) {
            sashForm.dispose();
        }
        super.dispose();
    }
}
