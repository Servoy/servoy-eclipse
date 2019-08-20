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

package com.servoy.eclipse.designer.editor.rfb.actions.handlers;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.jface.bindings.Binding;
import org.eclipse.jface.bindings.keys.KeySequence;
import org.eclipse.jface.bindings.keys.KeyStroke;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.internal.WorkbenchPage;
import org.eclipse.ui.keys.IBindingService;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.rfb.ChromiumVisualFormEditorDesignPage;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea@servoy.com
 *
 */
public class KeyPressedHandler implements IServerService
{

	private final ISelectionProvider selectionProvider;
	private final BaseVisualFormEditor editorPart;

	public KeyPressedHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	private boolean containsInheritedElements(List<IPersist> selection)
	{
		Form form = editorPart.getForm();
		for (IPersist iPersist : selection)
		{
			if (Utils.isInheritedFormElement(iPersist, form)) return true;
		}
		return false;
	}

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(String methodName, final JSONObject args)
	{
		int keyCode = args.optInt("keyCode");
		boolean isCtrl = args.optBoolean("ctrl");
		boolean isShift = args.optBoolean("shift");
		// if default browser do not handle all actions, eclipse should do that; for chromium handle everything
		if (new DesignerPreferences().useChromiumBrowser())
		{
			IBindingService bindingService = editorPart.getSite().getService(IBindingService.class);
			int modifier = 0;
			if (isCtrl) modifier = SWT.CTRL;
			if (isShift) modifier = SWT.SHIFT | modifier;
			if (keyCode == 46 || keyCode == 8) keyCode = SWT.DEL;
			else if (keyCode == 115) keyCode = SWT.F4;
			else if (keyCode == 116)
			{
				// refresh the editor
				if (editorPart.getGraphicaleditor() instanceof ChromiumVisualFormEditorDesignPage)
				{
					((ChromiumVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).refresh();
				}
			}
			Binding perfectMatch = bindingService.getPerfectMatch(KeySequence.getInstance(KeyStroke.getInstance(modifier, keyCode)));
			if (perfectMatch == null)
			{
				@SuppressWarnings("unchecked")
				Collection<Binding> partialMatches = bindingService.getConflictsFor(KeySequence.getInstance(KeyStroke.getInstance(modifier, keyCode)));
				if (partialMatches != null) for (Binding bid : partialMatches)
				{
					if (bid.getParameterizedCommand().getCommand().isEnabled())
					{
						perfectMatch = bid;
					}
				}
			}
			if (perfectMatch != null && perfectMatch.getParameterizedCommand().getCommand().isEnabled())
			{
				try
				{
					perfectMatch.getParameterizedCommand().executeWithChecks(null, null);
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
		else
		{
			switch (keyCode)
			{
				case 46 : // delete
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							List<Object> contextSelection = new ArrayList<>(((IStructuredSelection)selectionProvider.getSelection()).toList());
							List<IPersist> selection = new ArrayList<IPersist>();
							for (Object selected : contextSelection)
							{
								IPersist persist = null;
								if (selected instanceof IPersist)
								{
									persist = (IPersist)selected;
								}
								else if (selected instanceof PersistContext)
								{
									persist = ((PersistContext)selected).getPersist();
								}
								if (persist != null && !(persist instanceof Form))
								{
									selection.add(persist);
								}
							}
							if (selection.size() > 0 && !containsInheritedElements(selection))
							{
								editorPart.getCommandStack().execute(new FormElementDeleteCommand(selection.toArray(new IPersist[selection.size()])));
								selectionProvider.setSelection(new StructuredSelection(editorPart.getForm()));
							}
						}
					});
					break;

				case 83 : // s
					if (isCtrl && isShift) // ctrl+shift+s (save all)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								IWorkbenchPage page = editorPart.getSite().getPage();
								((WorkbenchPage)page).saveAllEditors(false, false, true);
							}
						});
					}
					break;

				case 90 : // z
					if (isCtrl && isShift)
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								EditorUtil.openScriptEditor(editorPart.getForm(), null, true);
							}
						});
					}
					break;
				case 115 : //f4
					Display.getDefault().asyncExec(new Runnable()
					{
						public void run()
						{
							try
							{
								new OpenFormHierarchyHandler(selectionProvider).executeMethod(null, null);
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
					});
			}
		}
		return null;
	}
}
