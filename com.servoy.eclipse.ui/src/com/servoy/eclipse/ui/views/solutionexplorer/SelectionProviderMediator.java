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
package com.servoy.eclipse.ui.views.solutionexplorer;

import org.eclipse.core.runtime.Assert;
import org.eclipse.core.runtime.ListenerList;
import org.eclipse.jface.viewers.IPostSelectionProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.StructuredViewer;
import org.eclipse.swt.events.FocusEvent;
import org.eclipse.swt.events.FocusListener;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Widget;

/**
 * A selection provider for view parts with more that one viewer.
 * Tracks the focus of the viewers to provide the correct selection.
 */
public class SelectionProviderMediator implements IPostSelectionProvider {

	private class InternalListener implements ISelectionChangedListener, FocusListener {
		/*
	 	 * @see ISelectionChangedListener#selectionChanged
	 	 */		
		public void selectionChanged(SelectionChangedEvent event) {
			doSelectionChanged(event);
		}
		
	    /*
	     * @see FocusListener#focusGained
	     */
	    public void focusGained(FocusEvent e) {    	
	    	doFocusChanged(e.widget);
	    }
	
	    /*
	     * @see FocusListener#focusLost
	     */
	    public void focusLost(FocusEvent e) {
	    	// do not reset due to focus behavior on GTK
	    	//fViewerInFocus= null;
	    }
	}
	
	private class InternalPostSelectionListener implements ISelectionChangedListener {
		public void selectionChanged(SelectionChangedEvent event) {
			doPostSelectionChanged(event);
		}
		
	}
	
	private StructuredViewer[] fViewers;

	private StructuredViewer fViewerInFocus;
	private ListenerList fSelectionChangedListeners;
	private ListenerList fPostSelectionChangedListeners;
	
	/**
	 * @param viewers All viewers that can provide a selection
	 * @param viewerInFocus the viewer currently in focus or <code>null</code> 
	 */
	public SelectionProviderMediator(StructuredViewer[] viewers, StructuredViewer viewerInFocus) {
		Assert.isNotNull(viewers);
		fViewers= viewers;
		InternalListener listener= new InternalListener();
		fSelectionChangedListeners= new ListenerList();
		fPostSelectionChangedListeners= new ListenerList();
		fViewerInFocus= viewerInFocus;	

		for (int i= 0; i < fViewers.length; i++) {
			StructuredViewer viewer= fViewers[i];
			viewer.addSelectionChangedListener(listener);
			viewer.addPostSelectionChangedListener(new InternalPostSelectionListener());
			Control control= viewer.getControl();
			control.addFocusListener(listener);	
		}
	}
	
	private void doFocusChanged(Widget control) {
		for (int i= 0; i < fViewers.length; i++) {
			if (fViewers[i].getControl() == control) {
				propagateFocusChanged(fViewers[i]);
				return;
			}
		}		
	}
	
	final void doPostSelectionChanged(SelectionChangedEvent event) {
		ISelectionProvider provider= event.getSelectionProvider();
		if (provider == fViewerInFocus) {
			firePostSelectionChanged();
		}
	}
	
	final void doSelectionChanged(SelectionChangedEvent event) {
		ISelectionProvider provider= event.getSelectionProvider();
		if (provider == fViewerInFocus) {
			fireSelectionChanged();
		}
	}
	
	final void propagateFocusChanged(StructuredViewer viewer) {
		if (viewer != fViewerInFocus) { // OK to compare by identity
			fViewerInFocus= viewer;
			fireSelectionChanged();
			firePostSelectionChanged();
		}
	}
	
	private void fireSelectionChanged() {
		if (fSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());
			
			Object[] listeners= fSelectionChangedListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				ISelectionChangedListener listener= (ISelectionChangedListener) listeners[i];
				listener.selectionChanged(event);
			}
		}
	}
	
	private void firePostSelectionChanged() {
		if (fPostSelectionChangedListeners != null) {
			SelectionChangedEvent event= new SelectionChangedEvent(this, getSelection());
			
			Object[] listeners= fPostSelectionChangedListeners.getListeners();
			for (int i= 0; i < listeners.length; i++) {
				ISelectionChangedListener listener= (ISelectionChangedListener) listeners[i];
				listener.selectionChanged(event);
			}
		}
	}	
	
	/*
	 * @see ISelectionProvider#addSelectionChangedListener
	 */
	public void addSelectionChangedListener(ISelectionChangedListener listener) {	
		fSelectionChangedListeners.add(listener);
	}
	
	/*
	 * @see ISelectionProvider#removeSelectionChangedListener
	 */
	public void removeSelectionChangedListener(ISelectionChangedListener listener) {
		fSelectionChangedListeners.remove(listener);
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.jface.viewers.IPostSelectionProvider#addPostSelectionChangedListener(org.eclipse.jface.viewers.ISelectionChangedListener)
	 */
	public void addPostSelectionChangedListener(ISelectionChangedListener listener) {
		fPostSelectionChangedListeners.add(listener);
	}

	public void removePostSelectionChangedListener(ISelectionChangedListener listener) 
	{
		fPostSelectionChangedListeners.remove(listener);
	}
		
	public ISelection getSelection()
	{
		ISelection selection = StructuredSelection.EMPTY;
		if (fViewerInFocus != null) {
			selection =  fViewerInFocus.getSelection();
		}
//		if (!selection.isEmpty()) 
//		{
//	        if (selection instanceof IStructuredSelection) {
//	            IStructuredSelection structuredSelection= (IStructuredSelection) selection;
//	            List javaElements= new ArrayList();
//	            for (Iterator iter= structuredSelection.iterator(); iter.hasNext();) {
//	                Object element= iter.next();
//	            }
//	            return new StructuredSelection(javaElements);
//	        }
//	        return StructuredSelection.EMPTY; 
//		}
		return selection;
	}

	/*
	 * @see ISelectionProvider#setSelection
	 */
	public void setSelection(ISelection selection) {
		if (fViewerInFocus != null) {
			fViewerInFocus.setSelection(selection);
		}
	}

	public void setSelection(ISelection selection, boolean reveal) {
		if (fViewerInFocus != null) {
			fViewerInFocus.setSelection(selection, reveal);
		}
	}
	
	/**
	 * Returns the viewer in focus or null if no viewer has the focus
	 */	
	public StructuredViewer getViewerInFocus() {
		return fViewerInFocus;
	}
}
