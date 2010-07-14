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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.ContextMenuProvider;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.parts.GraphicalEditor;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.gef.ui.rulers.RulerComposite;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.actions.AlignmentSortPartsAction;
import com.servoy.eclipse.designer.actions.DistributeAction;
import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.actions.ToggleShowGridAction;
import com.servoy.eclipse.designer.actions.ToggleSnapToGridAction;
import com.servoy.eclipse.designer.dnd.FormElementTransferDropTarget;
import com.servoy.eclipse.designer.editor.commands.BringToFrontAction;
import com.servoy.eclipse.designer.editor.commands.CopyAction;
import com.servoy.eclipse.designer.editor.commands.CutAction;
import com.servoy.eclipse.designer.editor.commands.FixedSelectAllAction;
import com.servoy.eclipse.designer.editor.commands.GroupAction;
import com.servoy.eclipse.designer.editor.commands.PasteAction;
import com.servoy.eclipse.designer.editor.commands.SaveAsTemplateAction;
import com.servoy.eclipse.designer.editor.commands.SendToBackAction;
import com.servoy.eclipse.designer.editor.commands.SetTabSequenceAction;
import com.servoy.eclipse.designer.editor.commands.ToggleAnchoringAction;
import com.servoy.eclipse.designer.editor.commands.UngroupAction;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.designer.property.PersistContext;
import com.servoy.eclipse.designer.property.UndoablePersistPropertySourceProvider;
import com.servoy.eclipse.designer.property.UndoablePropertySheetEntry;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.AnchorPropertyController.AnchorPropertySource;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetPage;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.util.Settings;

/**
 * Tab in form editor for designing the form visually.
 * 
 * @author rgansevles
 */

public class VisualFormEditorDesignPage extends GraphicalEditor implements PropertyChangeListener
{
	protected GraphicalViewer graphicalViewer;
	private final VisualFormEditor editorPart;

	public VisualFormEditorDesignPage(VisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
		FormSelectionTool selectionTool = new FormSelectionTool();
		DefaultEditDomain editDomain = new DefaultEditDomain(editorPart);
		editDomain.setDefaultTool(selectionTool);
		editDomain.setActiveTool(selectionTool);
		editDomain.getCommandStack().addCommandStackListener(editorPart);
		setEditDomain(editDomain);
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = ServoyModel.getSettings();
		settings.addPropertyChangeListener(this, DesignerPreferences.GUIDE_SIZE_SETTING);
		settings.addPropertyChangeListener(this, DesignerPreferences.METRICS_SETTING);
	}

	private final ISelectionProvider provider = new ISelectionProvider()
	{
		private ISelection selection;

		public void setSelection(ISelection selection)
		{
			this.selection = selection;
		}

		public void removeSelectionChangedListener(ISelectionChangedListener listener)
		{

		}

		public ISelection getSelection()
		{
			return selection;
		}

		public void addSelectionChangedListener(ISelectionChangedListener listener)
		{

		}
	};

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		final List<EditPart> editParts = new ArrayList<EditPart>();
		boolean persistContext = !selection.isEmpty();

		// set selection if persists are selected
		if (selection instanceof IStructuredSelection)
		{
			Iterator<Object> iterator = ((IStructuredSelection)selection).iterator();
			while (iterator.hasNext())
			{
				Object sel = iterator.next();
				if (!(sel instanceof PersistContext) || (((PersistContext)sel).getPersist() instanceof Part)) persistContext = false;
				Object model = Platform.getAdapterManager().getAdapter(sel, IPersist.class);
				if (model == null)
				{
					model = Platform.getAdapterManager().getAdapter(sel, FormElementGroup.class);
				}
				if (model != null)
				{
					// if we have an editpart for the model, it is on the form
					EditPart ep = (EditPart)getGraphicalViewer().getEditPartRegistry().get(model);
					if (ep != null)
					{
						editParts.add(ep);
					}
				}
			}
		}

		// If not the active editor, ignore selection changed.
		if (editorPart.equals(getSite().getPage().getActiveEditor()))
		{
			List actions = getSelectionActions();
			if (persistContext)
			{
				provider.setSelection(new StructuredSelection(editParts));
				ActionRegistry registry = getActionRegistry();
				Iterator iter = actions.iterator();
				while (iter.hasNext())
				{
					IAction action = registry.getAction(iter.next());
					if (action instanceof SelectionAction) ((SelectionAction)action).setSelectionProvider(provider);
				}
			}
			updateActions(actions);
			if (persistContext)
			{
				ActionRegistry registry = getActionRegistry();
				Iterator iter = actions.iterator();
				while (iter.hasNext())
				{
					IAction action = registry.getAction(iter.next());
					if (action instanceof SelectionAction) ((SelectionAction)action).setSelectionProvider(null);
				}
			}
		}

		if (editParts.size() > 0)
		{
			StructuredSelection newSelection = new StructuredSelection(editParts);
			if (!newSelection.equals(getGraphicalViewer().getSelection()))
			{
				getGraphicalViewer().setSelection(newSelection);
			}
			// reveal the last element, otherwise you have jumpy behavior when in form designer via ctl-click element 2 is 
			// selected whilst selected element 1 is not visible.
			getGraphicalViewer().reveal(editParts.get(editParts.size() - 1));
		}
	}

	@Override
	protected void createActions()
	{
		ActionRegistry registry = getActionRegistry();
		IAction action;

		action = new UndoAction(editorPart);
		registry.registerAction(action);
		getStackActions().add(action.getId());

		action = new RedoAction(editorPart);
		registry.registerAction(action);
		getStackActions().add(action.getId());

		action = new FixedSelectAllAction(editorPart);
		registry.registerAction(action);

		action = new DeleteAction((IWorkbenchPart)editorPart);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SaveAction(editorPart);
		registry.registerAction(action);
		getPropertyActions().add(action.getId());

		registry.registerAction(new PrintAction(editorPart));

		action = new CopyAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new CutAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new PasteAction(editorPart);
		registry.registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DirectEditAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new BringToFrontAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SendToBackAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)editorPart, PositionConstants.LEFT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)editorPart, PositionConstants.RIGHT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)editorPart, PositionConstants.TOP);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)editorPart, PositionConstants.BOTTOM);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)editorPart, PositionConstants.CENTER);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)editorPart, PositionConstants.MIDDLE);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(editorPart, DistributeRequest.Distribution.HORIZONTAL_SPACING);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(editorPart, DistributeRequest.Distribution.HORIZONTAL_CENTERS);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(editorPart, DistributeRequest.Distribution.HORIZONTAL_PACK);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(editorPart, DistributeRequest.Distribution.VERTICAL_SPACING);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(editorPart, DistributeRequest.Distribution.VERTICAL_CENTERS);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(editorPart, DistributeRequest.Distribution.VERTICAL_PACK);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ToggleAnchoringAction(editorPart, AnchorPropertySource.TOP);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ToggleAnchoringAction(editorPart, AnchorPropertySource.RIGHT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ToggleAnchoringAction(editorPart, AnchorPropertySource.BOTTOM);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ToggleAnchoringAction(editorPart, AnchorPropertySource.LEFT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SetTabSequenceAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new GroupAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new UngroupAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SaveAsTemplateAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());
	}

	/**
	 * @see org.eclipse.gef.ui.parts.GraphicalEditor#initializeGraphicalViewer()
	 */
	@Override
	protected void initializeGraphicalViewer()
	{
		GraphicalViewer viewer = getGraphicalViewer();

		viewer.setRootEditPart(new FormGraphicalRootEditPart(editorPart));
		viewer.setContents(createGraphicalViewerContents());
		getEditDomain().addViewer(viewer);

		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

		// configure the context menu provider
		ContextMenuProvider cmProvider = new VisualFormEditorContextMenuProvider(viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(cmProvider, viewer);

		ServoyModelManager.getServoyModelManager().getServoyModel();
		DesignerPreferences designerPreferences = new DesignerPreferences(ServoyModel.getSettings());
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(designerPreferences.getGridShow()));
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(designerPreferences.getGridSnapTo()));

		// Show rulers
		refreshRulers();

		Action action = new ToggleShowGridAction(viewer);
		getActionRegistry().registerAction(action);

		action = new ToggleSnapToGridAction(viewer);
		getActionRegistry().registerAction(action);
	}

	private void refreshRulers()
	{
		GraphicalViewer viewer = getGraphicalViewer();
		viewer.setProperty(RulerProvider.PROPERTY_HORIZONTAL_RULER, new FormRulerProvider(viewer.getContents(), true));
		viewer.setProperty(RulerProvider.PROPERTY_VERTICAL_RULER, new FormRulerProvider(viewer.getContents(), false));
		viewer.setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, Boolean.TRUE);
	}

	@Override
	protected void configureGraphicalViewer()
	{
		getGraphicalViewer().getControl().setBackground(ColorConstants.lightGray);
	}

	/**
	 * Creates the GraphicalViewer on the specified <code>Composite</code>.
	 * 
	 * @param parent the parent composite
	 */
	@Override
	protected void createGraphicalViewer(Composite parent)
	{
		RulerComposite rulerComposite = new RulerComposite(parent, SWT.NONE);

		GraphicalViewer viewer = new ModifiedScrollingGraphicalViewer();
		viewer.createControl(rulerComposite);
		setGraphicalViewer(viewer);
		configureGraphicalViewer();
		hookGraphicalViewer();
		initializeGraphicalViewer();

		if (editorPart.getFlattenedForm() != null)
		{
			rulerComposite.setGraphicalViewer((ScrollingGraphicalViewer)getGraphicalViewer());
			getGraphicalViewer().addDropTargetListener(new FormElementTransferDropTarget(getGraphicalViewer()));
			applyGuidePreferences();

			parent.getDisplay().asyncExec(new Runnable()
			{
				public void run()
				{
					try
					{
						// So that we can get the editor up and displaying as soon as possible we will
						// push this off to
						// the next async cycle.
						openViewers(getEditorSite());
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
		site.getPage().showView("org.eclipse.ui.views.PropertySheet", null, IWorkbenchPage.VIEW_VISIBLE); //$NON-NLS-1$	
	}

	/**
	 * @see AbstractGraphicalEditor#createGraphicalViewerContents()
	 */
	protected EditPart createGraphicalViewerContents()
	{
		return new FormGraphicalEditPart(Activator.getDefault().getDesignClient(), editorPart);
	}

	@Override
	public void dispose()
	{
		getEditDomain().getCommandStack().removeCommandStackListener(editorPart);
		ServoyModelManager.getServoyModelManager().getServoyModel();
		Settings settings = ServoyModel.getSettings();
		settings.removePropertyChangeListener(this, DesignerPreferences.GUIDE_SIZE_SETTING);
		settings.removePropertyChangeListener(this, DesignerPreferences.METRICS_SETTING);

		super.dispose();
	}

	@Override
	public void doSave(IProgressMonitor monitor)
	{
		getEditDomain().getCommandStack().markSaveLocation();
	}

	public void refreshAllParts()
	{
		RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
		rootEditPart.getContents().refresh();
		refreshRulers();
		Iterator childrenEditPartsIte = rootEditPart.getContents().getChildren().iterator();
		while (childrenEditPartsIte.hasNext())
		{
			Object childEditPart = childrenEditPartsIte.next();
			if (childEditPart instanceof EditPart) ((EditPart)childEditPart).refresh();
		}
	}

	/**
	 * Refresh the visual form editor that holds the IPersist.
	 * 
	 * @param persist
	 */
	public void refreshPersists(List<IPersist> persists)
	{
		RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();

		boolean full_refresh = false;
		List<EditPart> editParts = null;
		if (persists == null)
		{
			// child was add/removed
			full_refresh = true;
		}
		else
		{
			// children were modified
			editParts = new ArrayList<EditPart>(persists.size());
			for (IPersist persist : persists)
			{
				EditPart ep = (EditPart)rootEditPart.getViewer().getEditPartRegistry().get(persist);
				if (ep != null && rootEditPart.getContents() != ep)
				{
					editParts.add(ep);
				}
				else
				{
					// no editPart for this child yet or root edit part
					full_refresh = true;
				}
			}
		}

		if (full_refresh)
		{
			rootEditPart.getContents().refresh();
			refreshRulers();
		}

		if (editParts != null)
		{
			for (EditPart ep : editParts)
			{
				ep.refresh();
			}
		}
	}

	@Override
	public Object getAdapter(Class type)
	{
		if (type == IPropertySheetPage.class)
		{
			PropertySheetPage page = new ModifiedPropertySheetPage();
			page.setRootEntry(new UndoablePropertySheetEntry(getCommandStack()));
			return page;
		}
		if (type == IPropertySourceProvider.class)
		{
			return new UndoablePersistPropertySourceProvider(editorPart, getCommandStack());
		}
		if (type.equals(IContentOutlinePage.class))
		{
			return new FormOutlinePage(editorPart.getForm(), getGraphicalViewer(), getActionRegistry());
		}
		return super.getAdapter(type);
	}

	public void propertyChange(PropertyChangeEvent evt)
	{
		if (DesignerPreferences.GUIDE_SIZE_SETTING.equals(evt.getPropertyName()))
		{
			applyGuidePreferences();
		}
		if (DesignerPreferences.METRICS_SETTING.equals(evt.getPropertyName()))
		{
			refreshRulers();
		}
	}

	protected void applyGuidePreferences()
	{
		ServoyModelManager.getServoyModelManager().getServoyModel();
		DesignerPreferences designerPreferences = new DesignerPreferences(ServoyModel.getSettings());
		int guideSize = designerPreferences.getGuideSize();
		getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(guideSize, guideSize));
	}

	public boolean showPersist(IPersist persist)
	{
		Object editPart = getGraphicalViewer().getRootEditPart().getViewer().getEditPartRegistry().get(persist);
		if (editPart instanceof EditPart)
		{
			// select the marked element
			getGraphicalViewer().setSelection(new StructuredSelection(editPart));
			getGraphicalViewer().reveal((EditPart)editPart);
			return true;
		}
		return false;
	}
}
