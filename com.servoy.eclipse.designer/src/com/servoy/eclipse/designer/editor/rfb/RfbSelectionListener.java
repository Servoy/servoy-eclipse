/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.designer.editor.rfb;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.views.contentoutline.ContentOutline;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.WebFormComponentChildType;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.util.Utils;

/**
 * @author lvostinar
 *
 */
public class RfbSelectionListener implements ISelectionListener
{
	private final EditorWebsocketSession editorWebsocketSession;
	private List<String> lastSelection = new ArrayList<String>();
	private final Form form;
	private ISelection currentSelection;
	private final ISelectionProvider selectionProvider;

	public RfbSelectionListener(Form form, EditorWebsocketSession editorWebsocketSession, ISelectionProvider selectionProvider)
	{
		this.form = form;
		this.editorWebsocketSession = editorWebsocketSession;
		this.selectionProvider = selectionProvider;
	}

	@SuppressWarnings("unchecked")
	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			currentSelection = selection;

			updateSelection(false);
			// update selection immediately if it comes from outline view, do not wait for client call setSelection
			if (part instanceof ContentOutline)
			{
				selectionProvider.setSelection(selection);
			}
		}
	}

	public void updateSelection(boolean force)
	{
		Display.getCurrent().asyncExec(new Runnable()
		{
			public void run()
			{
				final List<String> uuids = getPersistUUIDS((IStructuredSelection)currentSelection);
				if (uuids != null && (force || (uuids.size() > 0 && (uuids.size() != lastSelection.size() || !uuids.containsAll(lastSelection)))))
				{
					lastSelection = uuids;
					editorWebsocketSession.getEventDispatcher().addEvent(new Runnable()
					{
						@Override
						public void run()
						{
							editorWebsocketSession.getClientService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("updateSelection",
								new Object[] { uuids.toArray() });
						}
					});
				}
				else if (currentSelection == null || currentSelection.isEmpty())
				{ //the components were deleted so the current selection is null; we also need to update the selection to empty
					editorWebsocketSession.getEventDispatcher().addEvent(new Runnable()
					{
						@Override
						public void run()
						{
							editorWebsocketSession.getClientService(EditorWebsocketSession.EDITOR_SERVICE).executeAsyncServiceCall("updateSelection",
								new Object[] { new String[0] });
						}
					});
				}
			}
		});
	}

	/**
	 * @param selection
	 * @return
	 */
	private List<String> getPersistUUIDS(IStructuredSelection selection)
	{
		if (selection == null) return null;
		// ignore persist that are not from the current form.
		boolean forCurrentForm = false;
		final List<String> uuids = new ArrayList<String>();
		for (Object sel : Utils.iterate(selection.iterator()))
		{
			IPersist persist = null;
			PersistContext pc = Platform.getAdapterManager().getAdapter(sel, PersistContext.class);
			if (pc != null)
			{
				// if this is a selection that is not form the current form (of this listener)
				// then ignore it , Sub forms component should be only selected in that subform.
				// not in the base or other subforms that show that same uuid/persist.
				if (pc.getContext() instanceof Form && pc.getContext() != form) continue;
				persist = pc.getPersist();
			}
			else
			{
				persist = Platform.getAdapterManager().getAdapter(sel, IPersist.class);
			}
			if (persist != null)
			{
				if (persist instanceof WebFormComponentChildType)
				{
					String uuid = ((WebFormComponentChildType)persist).getElement().getName();
					if (Character.isDigit(uuid.charAt(0)))
					{
						uuid = "_" + uuid;
					}
					uuid = uuid.replace('-', '_');
					// TODO check if this is really on the form
					uuids.add(uuid);
					forCurrentForm = true;
				}
				else if (persist.getParent() instanceof WebFormComponentChildType)
				{
					String uuid = ((WebFormComponentChildType)persist.getParent()).getElement().getName();
					uuid += "#" + persist.getUUID();
					uuids.add(uuid);
					forCurrentForm = true;
				}
				else
				{
					IPersist ancestor = persist.getAncestor(IRepository.FORMS);
					if (ancestor == null) continue;
					if (form.getUUID().equals(ancestor.getUUID()))
					{
						/*
						 * if (persist instanceof WebCustomType) { WebCustomType ghostBean = (WebCustomType)persist; uuids.add(ghostBean.getUUIDString()); }
						 * else
						 */
						uuids.add(persist.getUUID().toString());
						forCurrentForm = true;
					}
					else
					{
						List<Form> formHierarchy = ServoyModelFinder.getServoyModel().getFlattenedSolution().getFormHierarchy(form);
						if (formHierarchy.contains(ancestor))
						{
							uuids.add(persist.getUUID().toString());
							forCurrentForm = true;
						}
					}
				}
			}
			else
			{
				FormElementGroup formElementGroup = Platform.getAdapterManager().getAdapter(sel, FormElementGroup.class);
				if (formElementGroup != null)
				{
					if (form.getUUID().equals(formElementGroup.getParent().getUUID()))
					{
						uuids.add(formElementGroup.getGroupID());
						forCurrentForm = true;
					}
				}
			}
		}
		return forCurrentForm ? uuids : null;
	}

	/**
	 * @param lastSelection the lastSelection to set
	 */
	public void setLastSelection(IStructuredSelection selection)
	{
		List<String> persistUUIDS = getPersistUUIDS(selection);
		this.lastSelection = persistUUIDS != null ? persistUUIDS : lastSelection;
	}

}
