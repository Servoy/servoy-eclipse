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
package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TransferData;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.dnd.FormElementTransfer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.WebComponent;

/**
 * Action that will try to paste the contents of the clipboard in the solution explorer. The location of the objects being pasted and the behavior depends on
 * the current selection and the type of objects found in the clipboard.
 *
 * @author acostescu
 */
public class PasteAction extends Action implements ISelectionChangedListener
{

	private final Display display;
	private ISelection validSelection;

	/**
	 * New paste action.
	 *
	 * @param display the display used to create a clipboard.
	 */
	public PasteAction(Display display)
	{
		this.display = display;
		setEnabled(false);

		setImageDescriptor(PlatformUI.getWorkbench().getSharedImages().getImageDescriptor(ISharedImages.IMG_TOOL_PASTE));
		setText("Paste");
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		boolean enabled = true;
		ISelection sel = event.getSelection();
		if (sel instanceof StructuredSelection)
		{
			StructuredSelection s = (StructuredSelection)sel;
			SimpleUserNode pasteNode = getPasteNode(s);
			if (pasteNode != null)
			{
				// see what we have in the clipboard...
				Clipboard clipboard = new Clipboard(display);
				TransferData[] transferTypes = clipboard.getAvailableTypes();
				boolean foundCompatibleType = false;
				for (int i = transferTypes.length - 1; (i >= 0 && (!foundCompatibleType)); i--)
				{
					TransferData transferData = transferTypes[i];
					if (FormElementTransfer.getInstance().isSupportedType(transferData))
					{
						Object[] allPersistDragData = (Object[])clipboard.getContents(FormElementTransfer.getInstance());
						foundCompatibleType = isValidFormsPaste(pasteNode, allPersistDragData);
					}
				}
				enabled = foundCompatibleType;
				clipboard.dispose();
			}
			else
			{
				enabled = false;
			}
		}
		else
		{
			enabled = false;
		}

		if (enabled)
		{
			validSelection = sel;
		}
		else
		{
			validSelection = null;
		}
		setEnabled(enabled);
	}

	private SimpleUserNode getPasteNode(StructuredSelection s)
	{
		SimpleUserNode pasteNode = null;
		if (s.size() == 1)
		{
			pasteNode = (SimpleUserNode)s.getFirstElement();
		}
		else
		{
			// in case of multiple selection with only one parent node for all selected nodes,
			// we will consider that the targeted node is the parent node
			Iterator<SimpleUserNode> it = s.iterator();
			while (it.hasNext())
			{
				SimpleUserNode node = it.next();
				if (pasteNode != node.parent && pasteNode != null)
				{
					pasteNode = null;
					break;
				}
				else
				{
					pasteNode = node.parent;
				}
			}
		}
		return pasteNode;
	}

	private boolean isValidFormsPaste(SimpleUserNode pasteNode, Object[] allPersistDragData)
	{
		boolean itIs = true;
		// see if it's about forms
		if (pasteNode.getType() == UserNodeType.FORMS || pasteNode.getType() == UserNodeType.FORM)
		{
			for (int i = allPersistDragData.length - 1; (i >= 0 && itIs); i--)
			{
				if (allPersistDragData[i] instanceof PersistDragData)
				{
					PersistDragData persistDragData = (PersistDragData)allPersistDragData[i];
					if (persistDragData.type != IRepository.FORMS)
					{
						itIs = false;
					}
				}
				else
				{
					itIs = false;
				}
			}
		}
		else
		{
			itIs = false;
		}
		return itIs;
	}

	@Override
	public void run()
	{
		if (validSelection != null)
		{
			if (validSelection instanceof StructuredSelection)
			{
				StructuredSelection s = (StructuredSelection)validSelection;
				SimpleUserNode pasteNode = getPasteNode(s);
				if (pasteNode != null)
				{
					if (pasteNode.getType() == UserNodeType.FORMS || pasteNode.getType() == UserNodeType.FORM)
					{
						Clipboard clipboard = new Clipboard(display);
						pasteForms(pasteNode, clipboard.getContents(FormElementTransfer.getInstance()));
						clipboard.dispose();
					}
				}
			}
		}
	}

	private void pasteForms(SimpleUserNode pasteNode, Object contents)
	{
		if (contents != null && contents instanceof Object[])
		{
			Object[] persistData = (Object[])contents;
			for (int i = persistData.length - 1; i >= 0; i--)
			{
				if (persistData[i] instanceof PersistDragData)
				{
					PersistDragData data = (PersistDragData)persistData[i];
					ServoyProject sp = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(data.solutionName);
					if (sp != null)
					{
						IPersist sourcePersist = sp.getEditingPersist(data.uuid);
						if (sourcePersist instanceof Form)
						{
							Form f = (Form)sourcePersist;
							ServoyProject destinationProject = ((ServoyProject)pasteNode.getAncestorOfType(ServoyProject.class).getRealObject());

							//check if form contains unknown components, they should install package first
							if (f.isResponsiveLayout())
							{
								Object unkownSpec = f.acceptVisitor(new IPersistVisitor()
								{

									@Override
									public Object visit(IPersist o)
									{
										if (o instanceof WebComponent)
										{
											WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
												((WebComponent)o).getTypeName());
											if (spec == null)
											{
												return ((WebComponent)o).getTypeName();
											}
										}
										return IPersistVisitor.CONTINUE_TRAVERSAL;
									}
								});
								if (unkownSpec instanceof String)
								{
									MessageDialog.openError(new Shell(), "Cannot paste form",
										"Form contains unkown specification: " + unkownSpec + ", have to install package first.");
									return;
								}
							}
							// we have the source form and the destination solution; perform the paste
							IValidateName nameValidator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
							String newName = getCopyName(f.getName(), nameValidator);
							try
							{
								PersistCloner.intelligentClonePersist(f, newName, destinationProject, nameValidator, true);
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError(e);
							}
						}
						else
						{
							ServoyLog.logError("Paste forms wrong - form not found for clipboard data", null);
						}
					}
					else
					{
						ServoyLog.logError("Paste forms wrong - project not found for clipboard data", null);
					}
				}
				else
				{
					ServoyLog.logError("Paste forms wrong - clipboard data of incorrect type", null);
					return;
				}
			}
		}
		else
		{
			ServoyLog.logError("Paste forms failed; clipboard contents wrong " + contents, null);
		}
	}

	private String getCopyName(String name, IValidateName nameValidator)
	{
		String base = name + "_copy";
		String newName = base;
		int i = 1;
		boolean ok = false;

		do
		{
			try
			{
				nameValidator.checkName(newName, null, new ValidatorSearchContext(IRepository.FORMS), false);
				ok = true;
			}
			catch (RepositoryException e)
			{
				newName = base + (i++);
			}
		}
		while (!ok);

		return newName;
	}

}