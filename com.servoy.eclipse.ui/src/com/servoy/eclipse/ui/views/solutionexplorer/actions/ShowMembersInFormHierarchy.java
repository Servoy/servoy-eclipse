package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.views.solutionexplorer.FormHierarchyView;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ScriptMethod;

public class ShowMembersInFormHierarchy extends Action implements ISelectionChangedListener
{

	protected IPersist selection;
	private final FormHierarchyView view;
	boolean on;

	public ShowMembersInFormHierarchy(FormHierarchyView view, boolean initValue)
	{
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("show_members.png"));
		setText("Show members in Form Hierarchy");
		setToolTipText(getText());
		this.view = view;
		setChecked(initValue);
		on = initValue;
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (event.getSelection() instanceof IStructuredSelection)
		{
			IStructuredSelection sel = (IStructuredSelection)event.getSelection();
			Iterator< ? > it = sel.iterator();
			if (it.hasNext())
			{
				Object next = it.next();
				if (next instanceof ScriptMethod || next instanceof BaseComponent)
				{
					selection = (IPersist)next;
					if (!isChecked()) return;
					view.setSelection(selection, false);
				}
			}
		}
	}

	@Override
	public void run()
	{
		if (on != isChecked())
		{
			on = isChecked();
			if (on)
			{
				view.setSelection(selection);
			}
			else
			{
				view.setSelection(null);
			}
		}
		setChecked(on);
	}

	public void clearSelection()
	{
		selection = null;
	}

}
