package com.servoy.eclipse.aibridge.actions;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.plugin.AbstractUIPlugin;

import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.Completion;

public class SplitHViewAction extends Action {
    private Browser htmlViewer;
    private SashForm sashForm;
    
    public SplitHViewAction(Composite parent) {	
        super("Split horizontal view", SWT.TOGGLE);
        ImageDescriptor splitViewImageDescriptor = AbstractUIPlugin.imageDescriptorFromPlugin("com.servoy.eclipse.aibridge", "icons/hsplit.png");
        setImageDescriptor(splitViewImageDescriptor);        
        this.sashForm = new SashForm(parent, SWT.VERTICAL);
    }
    
    public void initializeHtmlViewer() {
        if (htmlViewer == null) {
            htmlViewer = new Browser(sashForm, SWT.NONE);
            htmlViewer.setVisible(false);
        }
    }
	
	public void setText(String text) {
		if (isChecked())
			htmlViewer.setText(text);
	}
	
	 @Override
     public void run() {
		 if (htmlViewer == null) return;
		 htmlViewer.setVisible(isChecked());

		 refreshView();
		 AiBridgeView.refresh();

     }
	 
	 public void refresh() {
		 if (htmlViewer == null) return;
		 refreshView();
		 htmlViewer.setVisible(isChecked());
		 
	 }
	 
	 private void refreshView() {
		 IStructuredSelection selection = AiBridgeView.getSelection();
		 String textToSet = "";

		 if (!selection.isEmpty()) {
		     Completion selectedCompletion = (Completion) selection.getFirstElement();
		     if (selectedCompletion != null && selectedCompletion.getResponse() != null) {
		         textToSet = selectedCompletion.getResponse().getResponseMessage();
		     }
		 }

		 setText(textToSet);
		 sashForm.layout();
	 }
	 
	 public SashForm getSashForm() {
	     return sashForm;
	 }
}
