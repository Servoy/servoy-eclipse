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
package com.servoy.eclipse.designer.editor;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectAllAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.ActionFactory;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.designer.actions.AlignmentSortPartsAction;
import com.servoy.eclipse.designer.actions.DistributeAction;
import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.editor.commands.AddAccordionPaneAction;
import com.servoy.eclipse.designer.editor.commands.AddFieldAction;
import com.servoy.eclipse.designer.editor.commands.AddMediaAction;
import com.servoy.eclipse.designer.editor.commands.AddPortalAction;
import com.servoy.eclipse.designer.editor.commands.AddSplitpaneAction;
import com.servoy.eclipse.designer.editor.commands.AddTabpanelAction;
import com.servoy.eclipse.designer.editor.commands.FixedSelectAllAction;
import com.servoy.eclipse.designer.editor.commands.GroupAction;
import com.servoy.eclipse.designer.editor.commands.SameHeightAction;
import com.servoy.eclipse.designer.editor.commands.SameWidthAction;
import com.servoy.eclipse.designer.editor.commands.SaveAsTemplateAction;
import com.servoy.eclipse.designer.editor.commands.SetTabSequenceAction;
import com.servoy.eclipse.designer.editor.commands.SwitchToRfbEditorAction;
import com.servoy.eclipse.designer.editor.commands.UngroupAction;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.designer.property.UndoablePropertySheetEntry;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetPage;
import com.servoy.j2db.persistence.IPersist;

/**
 * Tab in form editor for designing the form visually.
 *
 * @author rgansevles
 */

public abstract class BaseVisualFormEditorDesignPage extends GraphicalEditorWithFlyoutPalette
{
	protected final BaseVisualFormEditor editorPart;
	private ISelectionListener selectionChangedHandler;
	private ISelection currentSelection;


	public BaseVisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
		DefaultEditDomain editDomain = new DefaultEditDomain(editorPart);
		editDomain.setCommandStack(editorPart.getCommandStack()); // used shared command stack from editorPart, design page may be recreated
		setEditDomain(editDomain);
	}

	public abstract DesignPagetype getDesignPagetype();

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		getEditDomain().getCommandStack().markSaveLocation();
	}

	/**
	 * @return the editorPart
	 */
	public BaseVisualFormEditor getEditorPart()
	{
		return editorPart;
	}

	@Override
	protected void initializeGraphicalViewer()
	{
		openViewers();
	}

	protected void openViewers()
	{
		if (editorPart.getForm() != null)
		{
			getEditorSite().getShell().getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					try
					{
						// So that we can get the editor up and displaying as soon as possible we will
						// push this off to
						// the next async cycle.
						if (!PlatformUI.getWorkbench().isClosing()) openViewers(getEditorSite()); // maybe workbench was closed meanwhile
					}
					catch (PartInitException e)
					{
						ServoyLog.logError(e);
					}
				}
			});
		}
	}

	/*
	 * Open the VCE Viewers if required to.
	 */
	protected void openViewers(IEditorSite site) throws PartInitException
	{
		// // Open the properties and Java beans viewer if we are in the Java or Java Browsing
		// perspective
		IWorkbenchPage page = site.getWorkbenchWindow().getActivePage();
		if (page == null)
		{
			return;
		}
		//
		// // Calling showView will open the editor if it isn't already opened, and bring it to the
		// front
		// // if it already is
		site.getPage().showView("org.eclipse.ui.views.PropertySheet", null, IWorkbenchPage.VIEW_VISIBLE);
	}

	protected abstract IAction createCopyAction();

	protected abstract IAction createCutAction();

	protected abstract IAction createPasteAction();

	@Override
	protected void createActions()
	{
		ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new UndoAction(editorPart)
		{
			@Override
			public void run()
			{
				//RAGTEST
				PersistPropertySource.refreshPropertiesView();
				super.run();
			}
		};
		registry.registerAction(action);
		getStackActions().add(action.getId());
		action = new RedoAction(editorPart)
		{
			@Override
			public void run()
			{
				//RAGTEST
				PersistPropertySource.refreshPropertiesView();
				super.run();
			}
		};
		registry.registerAction(action);
		getStackActions().add(action.getId());

		action = createSelectAllAction();
		registry.registerAction(action);

		action = createDeleteAction();
		if (action != null)
		{
			registry.registerAction(action);
			getSelectionActions().add(action.getId());
		}

		action = new SaveAction(editorPart);
		registry.registerAction(action);
		getPropertyActions().add(action.getId());

		registry.registerAction(new PrintAction(editorPart));

		registry.registerAction(new SwitchToRfbEditorAction(editorPart));

		action = createCopyAction();
		if (action != null)
		{
			registry.registerAction(action);
			getSelectionActions().add(action.getId());
		}

		action = createCutAction();
		if (action != null)
		{
			registry.registerAction(action);
			getSelectionActions().add(action.getId());
		}

		action = createPasteAction();
		if (action != null)
		{
			registry.registerAction(action);
			getSelectionActions().add(action.getId());
		}

		action = new DirectEditAction(editorPart);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.LEFT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.RIGHT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.TOP);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.BOTTOM);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.CENTER);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.MIDDLE);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.HORIZONTAL_SPACING);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.HORIZONTAL_CENTERS);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.HORIZONTAL_PACK);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.VERTICAL_SPACING);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.VERTICAL_CENTERS);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.VERTICAL_PACK);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SameWidthAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SameHeightAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SetTabSequenceAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new GroupAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new UngroupAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SaveAsTemplateAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddTabpanelAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddSplitpaneAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddAccordionPaneAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddPortalAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddMediaAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddFieldAction(getEditorPart());
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_SEND_TO_BACK);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_SEND_TO_BACK_ONE_STEP);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());
	}

	/**
	 * @return
	 */
	protected SelectAllAction createSelectAllAction()
	{
		return new FixedSelectAllAction(editorPart);
	}

	protected abstract DeleteAction createDeleteAction();

	@Override
	public Object getAdapter(Class type)
	{
		if (type == IPropertySheetPage.class)
		{
			Map<String, IAction> actions = new HashMap<String, IAction>();
			actions.put(ActionFactory.UNDO.getId(), getActionRegistry().getAction(ActionFactory.UNDO.getId()));
			actions.put(ActionFactory.REDO.getId(), getActionRegistry().getAction(ActionFactory.REDO.getId()));
			PropertySheetPage page = new ModifiedPropertySheetPage(actions);
			page.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
			return page;
		}
		if (type.equals(IContentOutlinePage.class))
		{
			return new FormOutlinePage(editorPart.getForm(), getGraphicalViewer(), getActionRegistry());
		}
		return super.getAdapter(type);
	}

	public abstract void refreshAllParts();

	public abstract boolean showPersist(IPersist persist);

	public abstract void refreshPersists(List<IPersist> persists, boolean fullRefresh);

	@Override
	public void selectionChanged(final IWorkbenchPart part, ISelection newSelection)
	{
		// handle the selection when ui thread comes available, in case of may selection changed events, only the last one is handled, the others are skipped
		if (selectionChangedHandler == null)
		{
			selectionChangedHandler = createSelectionChangedHandler();
		}
		currentSelection = newSelection;

		Display.getCurrent().asyncExec(new Runnable()
		{
			public void run()
			{
				if (editorPart.isClosing() || editorPart.getSite() == null || editorPart.getSite().getWorkbenchWindow() == null || getEditorSite() == null ||
					getEditorSite().getShell() == null || getEditorSite().getShell().isDisposed())
				{
					return;
				}

				ISelection selection = currentSelection;
				if (selection == null)
				{
					// already handled
					return;
				}
				currentSelection = null;

				selectionChangedHandler.selectionChanged(part, selection);
			}
		});
	}

	protected ISelectionListener createSelectionChangedHandler()
	{
		return new ISelectionListener()
		{
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection)
			{
				updateActions(getSelectionActions());
			}
		};
	}

	@Override
	protected PaletteRoot getPaletteRoot()
	{
		return null;
	}
}
