package com.servoy.eclipse.aibridge.actions;

import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.servoy.eclipse.aibridge.AiBridgeManager;
import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.Completion;

public class DeleteAction extends Action {
	
	public DeleteAction() {
		super("Delete");
		setEnabled(true);
	}
	
	 @Override
     public void run() {
		 IStructuredSelection selection = AiBridgeView.getSelection();
		 if (selection.isEmpty()) {
		     return;
		 }

		 Stream.of(selection.toArray())
		       .filter(obj -> obj instanceof Completion)
		       .map(obj -> (Completion) obj)
		       .forEach(completion -> AiBridgeManager.getRequestMap().remove(completion.getId()));

		 AiBridgeView.refresh();
     }

}