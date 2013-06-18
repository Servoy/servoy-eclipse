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
package com.servoy.eclipse.designer.outline;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.part.IPageSite;
import org.eclipse.ui.views.contentoutline.ContentOutlinePage;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.IPersistChangeListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileListGraphicalEditPart;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;

/**
 * ContentOutlinePage for Servoy form in outline view.
 * 
 * @author rgansevles
 */

public class FormOutlinePage extends ContentOutlinePage implements ISelectionListener, IPersistChangeListener
{

	private final Form form;
	private final GraphicalViewer viewer;
	private final ActionRegistry registry;
	private volatile boolean refreshing;

	public FormOutlinePage(Form form, GraphicalViewer viewer, ActionRegistry registry)
	{
		this.form = form;
		this.viewer = viewer;
		this.registry = registry;
	}

	@Override
	public void createControl(Composite parent)
	{
		super.createControl(parent);
		boolean mobile = form != null && form.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) != null;

		getTreeViewer().setContentProvider(mobile ? new MobileFormOutlineContentProvider(form) : new FormOutlineContentProvider(form));
		getTreeViewer().setLabelProvider(
			new FormContextDelegateLabelProvider(mobile ? MobileFormOutlineLabelprovider.MOBILE_FORM_OUTLINE_LABEL_PROVIDER_INSTANCE
				: FormOutlineLabelprovider.FORM_OUTLINE_LABEL_PROVIDER_INSTANCE, form));
		getTreeViewer().setInput(form);

		// when the outline view is reparented to another shell, you cannot use the form editor context menu here
		MenuManager menuManager = viewer.getContextMenu();
		if (menuManager != null && menuManager.getMenu() != null && !menuManager.getMenu().isDisposed() &&
			getTreeViewer().getTree().getShell() == menuManager.getMenu().getParent().getShell())
		{
			getTreeViewer().getTree().setMenu(menuManager.createContextMenu(getTreeViewer().getTree()));
		}
	}

	@Override
	public void init(IPageSite pageSite)
	{
		super.init(pageSite);
		getSite().getWorkbenchWindow().getSelectionService().addSelectionListener(this);
		ServoyModelManager.getServoyModelManager().getServoyModel().addPersistChangeListener(false, this);
		IActionBars bars = pageSite.getActionBars();
		String id = ActionFactory.UNDO.getId();
		bars.setGlobalActionHandler(id, registry.getAction(id));
		id = ActionFactory.REDO.getId();
		bars.setGlobalActionHandler(id, registry.getAction(id));
		id = ActionFactory.DELETE.getId();
		bars.setGlobalActionHandler(id, registry.getAction(id));
		id = ActionFactory.COPY.getId();
		bars.setGlobalActionHandler(id, registry.getAction(id));
		id = ActionFactory.CUT.getId();
		bars.setGlobalActionHandler(id, registry.getAction(id));
		id = ActionFactory.PASTE.getId();
		bars.setGlobalActionHandler(id, registry.getAction(id));
	}

	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		if (selection instanceof IStructuredSelection)
		{
			FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
			if (editingFlattenedSolution == null)
			{
				return;
			}
			List<Form> formHierarchy = editingFlattenedSolution.getFormHierarchy(form);
			List<Object> selectionPath = new ArrayList<Object>();
			Iterator< ? > iterator = ((IStructuredSelection)selection).iterator();
			Object selectionObject;
			while (iterator.hasNext())
			{
				selectionObject = iterator.next();
				IPersist persist = (IPersist)Platform.getAdapterManager().getAdapter(selectionObject, IPersist.class);
				if (persist != null)
				{
					IPersist f = persist.getAncestor(IRepository.FORMS);
					if (f != null && formHierarchy.contains(f))
					{
						selectionPath.add(PersistContext.create(persist, form));
					}
				}
				else
				{
					FormElementGroup formElementGroup = (FormElementGroup)Platform.getAdapterManager().getAdapter(selectionObject, FormElementGroup.class);
					if (formElementGroup != null) selectionPath.add(formElementGroup);
					else
					{
						MobileListModel mobileListModel = (MobileListModel)Platform.getAdapterManager().getAdapter(selectionObject, MobileListModel.class);
						if (mobileListModel == null && selectionObject instanceof MobileListGraphicalEditPart) mobileListModel = ((MobileListGraphicalEditPart)selectionObject).getModel();
						if (mobileListModel != null) selectionPath.add(mobileListModel);
					}
				}
			}
			if (selectionPath.size() > 0)
			{
				StructuredSelection newSelection = new StructuredSelection(selectionPath);
				if (!newSelection.equals(getTreeViewer().getSelection()))
				{
					getTreeViewer().setSelection(newSelection);
				}
			}
		}
	}

	@Override
	public void dispose()
	{
		getSite().getWorkbenchWindow().getSelectionService().removeSelectionListener(this);
		ServoyModelManager.getServoyModelManager().getServoyModel().removePersistChangeListener(false, this);
		super.dispose();
	}

	public void persistChanges(Collection<IPersist> changes)
	{
		if (refreshing)
		{
			// Do not stack multiple refresh actions
			return;
		}

		List<Form> formHierarchy = ModelUtils.getEditingFlattenedSolution(form).getFormHierarchy(form);
		for (IPersist changed : changes)
		{
			IPersist parentForm = changed.getAncestor(IRepository.FORMS);
			if (parentForm != null && formHierarchy.contains(parentForm))
			{
				refreshing = true;
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						try
						{
							Control control = getControl();
							if (control != null && !control.isDisposed())
							{
								getTreeViewer().refresh();
							}
						}
						finally
						{
							refreshing = false;
						}
					}
				});
				return;
			}
		}
	}

	@Override
	public void setActionBars(IActionBars actionBars)
	{
		super.setActionBars(actionBars);
		IMenuManager menuManager = actionBars.getMenuManager();
		menuManager.add(GroupedOutlineViewToggleAction.addListener(getTreeViewer()));
	}
}
