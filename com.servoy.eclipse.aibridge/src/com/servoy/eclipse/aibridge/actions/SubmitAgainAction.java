package com.servoy.eclipse.aibridge.actions;

import java.util.stream.Stream;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IStructuredSelection;

import com.servoy.eclipse.aibridge.AiBridgeManager;
import com.servoy.eclipse.aibridge.AiBridgeView;
import com.servoy.eclipse.aibridge.Completion;

public class SubmitAgainAction extends Action {
	
	
	public SubmitAgainAction() {
		super("Submit again");
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
		       .forEach(AiBridgeManager::sendCompletion);

		 AiBridgeManager.saveData(AiBridgeView.getSolutionName());
		 AiBridgeView.refresh();
     }

}