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
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.DefaultEditDomain;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.internal.ui.palette.PaletteSelectionTool;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.DirectEditAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.PrintAction;
import org.eclipse.gef.ui.actions.RedoAction;
import org.eclipse.gef.ui.actions.SaveAction;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.actions.UndoAction;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite;
import org.eclipse.gef.ui.palette.FlyoutPaletteComposite.FlyoutPreferences;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.customize.PaletteCustomizerDialog;
import org.eclipse.gef.ui.parts.GraphicalEditorWithFlyoutPalette;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.CoolItem;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.swt.widgets.ToolBar;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.contentoutline.IContentOutlinePage;
import org.eclipse.ui.views.properties.IPropertySheetPage;
import org.eclipse.ui.views.properties.IPropertySourceProvider;
import org.eclipse.ui.views.properties.PropertySheetPage;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.designer.actions.AlignmentSortPartsAction;
import com.servoy.eclipse.designer.actions.DistributeAction;
import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.actions.SelectFeedbackmodeAction;
import com.servoy.eclipse.designer.actions.SelectSnapmodeAction;
import com.servoy.eclipse.designer.actions.ViewerTogglePropertyAction;
import com.servoy.eclipse.designer.dnd.FormElementTransferDropTarget;
import com.servoy.eclipse.designer.editor.commands.AddFieldAction;
import com.servoy.eclipse.designer.editor.commands.AddPortalAction;
import com.servoy.eclipse.designer.editor.commands.AddSplitpaneAction;
import com.servoy.eclipse.designer.editor.commands.AddTabpanelAction;
import com.servoy.eclipse.designer.editor.commands.BringToFrontAction;
import com.servoy.eclipse.designer.editor.commands.CopyAction;
import com.servoy.eclipse.designer.editor.commands.CutAction;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.FixedSelectAllAction;
import com.servoy.eclipse.designer.editor.commands.GroupAction;
import com.servoy.eclipse.designer.editor.commands.PasteAction;
import com.servoy.eclipse.designer.editor.commands.SameHeightAction;
import com.servoy.eclipse.designer.editor.commands.SameWidthAction;
import com.servoy.eclipse.designer.editor.commands.SaveAsTemplateAction;
import com.servoy.eclipse.designer.editor.commands.SendToBackAction;
import com.servoy.eclipse.designer.editor.commands.SetTabSequenceAction;
import com.servoy.eclipse.designer.editor.commands.UngroupAction;
import com.servoy.eclipse.designer.editor.palette.PaletteItemTransferDropTargetListener;
import com.servoy.eclipse.designer.editor.palette.VisualFormEditorPaletteCustomizer;
import com.servoy.eclipse.designer.editor.palette.VisualFormEditorPaletteFactory;
import com.servoy.eclipse.designer.editor.rulers.FormRulerComposite;
import com.servoy.eclipse.designer.editor.rulers.RulerManager;
import com.servoy.eclipse.designer.outline.FormOutlinePage;
import com.servoy.eclipse.designer.property.PersistContext;
import com.servoy.eclipse.designer.property.UndoablePersistPropertySourceProvider;
import com.servoy.eclipse.designer.property.UndoablePropertySheetEntry;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.CoolbarLayout;
import com.servoy.eclipse.ui.util.ActionToolItem;
import com.servoy.eclipse.ui.views.ModifiedPropertySheetPage;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Part;

/**
 * Tab in form editor for designing the form visually.
 * 
 * @author rgansevles
 */

public class VisualFormEditorDesignPage extends GraphicalEditorWithFlyoutPalette
{
	public static final String COOLBAR_REORGANIZE = "reorganize";
	public static final String COOLBAR_ALIGN = "align";
	public static final String COOLBAR_DISTRIBUTE = "distribute";
	public static final String COOLBAR_SAMESIZE = "same size";
	public static final String COOLBAR_TOGGLE = "toggle";
	public static final String COOLBAR_ELEMENTS = "elements";

	/**
	 * A viewer property indicating whether inherited elements are hidden. The value must  be a Boolean.
	 */
	public static final String PROPERTY_HIDE_INHERITED = "Hide.inherited"; //$NON-NLS-1$

	protected GraphicalViewer graphicalViewer;
	private final VisualFormEditor editorPart;
	private PaletteRoot paletteModel;
	private FormRulerComposite rulerComposite;
	private CoolBar coolBar;
	private List<String> hiddenBars;
	private MenuManager toolbarMenuManager;
	private final Map<String, List<IAction>> toolBarActions = new LinkedHashMap<String, List<IAction>>();
	private final Map<String, CoolItem> toolbarCoolItems = new HashMap<String, CoolItem>();
	private final Map<String, IAction> toolMenuBarActions = new HashMap<String, IAction>();

	private Runnable selectionChangedHandler;
	private ISelection currentSelection;

	protected final IPreferenceChangeListener preferenceChangeListener = new IPreferenceChangeListener()
	{

		public void preferenceChange(PreferenceChangeEvent event)
		{
			if (DesignerPreferences.isGuideSetting(event.getKey()))
			{
				applyGuidePreferences();
			}
			else if (DesignerPreferences.isRulersSetting(event.getKey()))
			{
				refreshRulers();
			}
			else if (DesignerPreferences.isCoolbarSetting(event.getKey()))
			{
				refreshToolBars();
			}
			else if (paletteModel != null && DesignerPreferences.isPaletteSetting(event.getKey()))
			{
				VisualFormEditorPaletteFactory.refreshPalette(paletteModel);
			}
		}
	};

	public VisualFormEditorDesignPage(VisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
		FormSelectionTool selectionTool = new FormSelectionTool(editorPart);
		DefaultEditDomain editDomain = new DefaultEditDomain(editorPart);
		editDomain.setDefaultTool(selectionTool);
		editDomain.setActiveTool(selectionTool);
		editDomain.getCommandStack().addCommandStackListener(editorPart);
		setEditDomain(editDomain);
		com.servoy.eclipse.ui.Activator.getDefault().getEclipsePreferences().addPreferenceChangeListener(preferenceChangeListener);
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
	private RulerManager rulerManager;

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection newSelection)
	{
		currentSelection = newSelection;

		// handle the selection when ui thread comes available, in case of may selection changed events, only the last one is handled, the others are skipped
		Display.getCurrent().asyncExec(getSelectionChangedHandler());
	}

	protected Runnable getSelectionChangedHandler()
	{
		if (selectionChangedHandler == null)
		{
			selectionChangedHandler = new Runnable()
			{
				public void run()
				{
					if (editorPart.isClosing())
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
						StructuredSelection editpartSelection = new StructuredSelection(editParts);
						if (!editpartSelection.equals(getGraphicalViewer().getSelection()))
						{
							getGraphicalViewer().setSelection(editpartSelection);
						}
						// reveal the last element, otherwise you have jumpy behavior when in form designer via ctl-click element 2 is 
						// selected whilst selected element 1 is not visible.
						if (getGraphicalViewer().getControl() != null && !getGraphicalViewer().getControl().isDisposed()) getGraphicalViewer().reveal(
							editParts.get(editParts.size() - 1));
					}
				}
			};
		}
		return selectionChangedHandler;
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

		action = new DeleteAction((IWorkbenchPart)editorPart)
		{
			@Override
			protected boolean calculateEnabled()
			{
				if (DesignerUtil.containsInheritedElement(getSelectedObjects())) return false;
				return super.calculateEnabled();
			}
		};
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

		action = new SameWidthAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SameHeightAction(editorPart);
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

		action = new AddTabpanelAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddSplitpaneAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddPortalAction(editorPart);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddFieldAction(editorPart);
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

		if (editorPart.getFlattenedForm() != null)
		{
			viewer.addDropTargetListener(new FormElementTransferDropTarget(getGraphicalViewer()));
			viewer.addDropTargetListener(new PaletteItemTransferDropTargetListener(getGraphicalViewer()));

			getEditorSite().getShell().getDisplay().asyncExec(new Runnable()
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

		viewer.setRootEditPart(new FormGraphicalRootEditPart(editorPart));
		viewer.setContents(createGraphicalViewerContents());
		getEditDomain().addViewer(viewer);

		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));

		// configure the context menu provider
		String id = "#FormDesignerContext";
		VisualFormEditorContextMenuProvider cmProvider = new VisualFormEditorContextMenuProvider(id, viewer, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(id, cmProvider, viewer);

		DesignerPreferences designerPreferences = new DesignerPreferences();
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(designerPreferences.getFeedbackGrid()));
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(designerPreferences.getGridSnapTo()));
		viewer.setProperty(SnapToElementAlignment.PROPERTY_ALIGNMENT_ENABLED, Boolean.valueOf(designerPreferences.getAlignmentSnapTo()));
		viewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_ANCHOR_FEEDBACK_VISIBLE, Boolean.valueOf(designerPreferences.getShowAnchorFeedback()));
		viewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_SAME_SIZE_FEEDBACK_VISIBLE, Boolean.valueOf(designerPreferences.getShowSameSizeFeedback()));
		viewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE, Boolean.valueOf(designerPreferences.getFeedbackAlignment()));
		applyGuidePreferences();

		// Show rulers
		refreshRulers();

		Action action = new SelectFeedbackmodeAction(viewer);
		getActionRegistry().registerAction(action);

		action = new SelectSnapmodeAction(viewer);
		getActionRegistry().registerAction(action);

		action = new ViewerTogglePropertyAction(viewer, DesignerActionFactory.TOGGLE_HIDE_INHERITED.getId(), DesignerActionFactory.TOGGLE_HIDE_INHERITED_TEXT,
			DesignerActionFactory.TOGGLE_HIDE_INHERITED_TOOLTIP, DesignerActionFactory.TOGGLE_HIDE_INHERITED_IMAGE, PROPERTY_HIDE_INHERITED);
		getActionRegistry().registerAction(action);

		addToolbarAction(COOLBAR_TOGGLE, getActionRegistry().getAction(DesignerActionFactory.SELECT_FEEDBACK.getId()));
		addToolbarAction(COOLBAR_TOGGLE, getActionRegistry().getAction(DesignerActionFactory.SELECT_SNAPMODE.getId()));
		addToolbarAction(COOLBAR_TOGGLE, getActionRegistry().getAction(DesignerActionFactory.TOGGLE_HIDE_INHERITED.getId()));

		refreshToolBars();

		viewer.addPropertyChangeListener(new PropertyChangeListener()
		{
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(PROPERTY_HIDE_INHERITED))
				{
					getGraphicalViewer().getRootEditPart().getContents().refresh();
				}
			}
		});
	}

	protected void fillToolbar()
	{
		addToolbarAction(COOLBAR_REORGANIZE, getActionRegistry().getAction(DesignerActionFactory.BRING_TO_FRONT.getId()));
		addToolbarAction(COOLBAR_REORGANIZE, getActionRegistry().getAction(DesignerActionFactory.SEND_TO_BACK.getId()));
		addToolbarAction(COOLBAR_REORGANIZE, getActionRegistry().getAction(DesignerActionFactory.SET_TAB_SEQUENCE.getId()));
		addToolbarAction(COOLBAR_REORGANIZE, getActionRegistry().getAction(DesignerActionFactory.GROUP.getId()));
		addToolbarAction(COOLBAR_REORGANIZE, getActionRegistry().getAction(DesignerActionFactory.UNGROUP.getId()));
		addToolbarAction(COOLBAR_REORGANIZE, getActionRegistry().getAction(DesignerActionFactory.SAVE_AS_TEMPLATE.getId()));

		addToolbarAction(COOLBAR_ALIGN, getActionRegistry().getAction(GEFActionConstants.ALIGN_LEFT));
		addToolbarAction(COOLBAR_ALIGN, getActionRegistry().getAction(GEFActionConstants.ALIGN_RIGHT));
		addToolbarAction(COOLBAR_ALIGN, getActionRegistry().getAction(GEFActionConstants.ALIGN_TOP));
		addToolbarAction(COOLBAR_ALIGN, getActionRegistry().getAction(GEFActionConstants.ALIGN_BOTTOM));
		addToolbarAction(COOLBAR_ALIGN, getActionRegistry().getAction(GEFActionConstants.ALIGN_CENTER));
		addToolbarAction(COOLBAR_ALIGN, getActionRegistry().getAction(GEFActionConstants.ALIGN_MIDDLE));

		addToolbarAction(COOLBAR_DISTRIBUTE, getActionRegistry().getAction(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_SPACING.getId()));
		addToolbarAction(COOLBAR_DISTRIBUTE, getActionRegistry().getAction(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_CENTER.getId()));
		addToolbarAction(COOLBAR_DISTRIBUTE, getActionRegistry().getAction(DesignerActionFactory.DISTRIBUTE_HORIZONTAL_PACK.getId()));
		addToolbarAction(COOLBAR_DISTRIBUTE, getActionRegistry().getAction(DesignerActionFactory.DISTRIBUTE_VERTICAL_SPACING.getId()));
		addToolbarAction(COOLBAR_DISTRIBUTE, getActionRegistry().getAction(DesignerActionFactory.DISTRIBUTE_VERTICAL_CENTER.getId()));
		addToolbarAction(COOLBAR_DISTRIBUTE, getActionRegistry().getAction(DesignerActionFactory.DISTRIBUTE_VERTICAL_PACK.getId()));

		addToolbarAction(COOLBAR_SAMESIZE, getActionRegistry().getAction(DesignerActionFactory.SAME_WIDTH.getId()));
		addToolbarAction(COOLBAR_SAMESIZE, getActionRegistry().getAction(DesignerActionFactory.SAME_HEIGHT.getId()));

		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_FIELD.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_PORTAL.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_SPLITPANE.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_TAB.getId()));
	}

	private void refreshRulers()
	{
		rulerManager.refreshRulers(new DesignerPreferences().getShowRulers());
	}

	protected void saveCoolbarLayout()
	{
		new DesignerPreferences().saveCoolbarLayout(new CoolbarLayout(coolBar.getItemOrder(), coolBar.getWrapIndices(), coolBar.getItemSizes(),
			hiddenBars.toArray(new String[hiddenBars.size()])));
	}


	@Override
	protected void configureGraphicalViewer()
	{
		getGraphicalViewer().getControl().setBackground(ColorConstants.lightGray);
	}

	@Override
	protected Control getGraphicalControl()
	{
		return rulerComposite;
	}

	protected MenuManager getToolbarMenuManager()
	{
		if (toolbarMenuManager == null)
		{
			toolbarMenuManager = createToolbarMenuManager();
		}
		return toolbarMenuManager;
	}

	protected MenuManager createToolbarMenuManager()
	{
		MenuManager menuManager = new MenuManager();
		menuManager.add(new Action("reset")
		{
			@Override
			public void run()
			{
				new DesignerPreferences().saveCoolbarLayout(null);
			}
		});
		menuManager.add(new Separator("bars"));
		return menuManager;
	}

	protected IAction createCheckBarAction(final String bar)
	{
		Action action = new Action(bar, IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				if (!hiddenBars.remove(bar))
				{
					hiddenBars.add(bar);
					createCoolItem(bar);
				}
				else
				{
					disposeCoolItem(bar);
				}
				saveCoolbarLayout();
			}
		};
		toolMenuBarActions.put(bar, action);
		return action;
	}

	private void createCoolItem(String bar)
	{
		if (!toolbarCoolItems.containsKey(bar))
		{
			ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
			CoolItem item = new CoolItem(coolBar, SWT.NONE);
			ToolBar toolBar = toolBarManager.createControl(coolBar);
			item.setControl(toolBar);

			toolBarManager.setContextMenuManager(getToolbarMenuManager());
			for (IAction action : toolBarActions.get(bar))
			{
				new ActionToolItem(toolBar, action);
			}

			toolBar.pack();
			Point size = toolBar.getSize();

			Point preferred = item.computeSize(size.x, size.y);
			item.setPreferredSize(preferred);
			item.setMinimumSize(preferred);

			toolbarCoolItems.put(bar, item);
		}
	}

	private void disposeCoolItem(String bar)
	{
		CoolItem item = toolbarCoolItems.remove(bar);
		if (item != null)
		{
			item.dispose();
		}
	}

	protected void refreshToolBars()
	{
		if (coolBar == null)
		{
			return;
		}

		CoolbarLayout coolbarLayout = new DesignerPreferences().getCoolbarLayout();

		hiddenBars = coolbarLayout == null ? new ArrayList<String>() : new ArrayList<String>(Arrays.asList(coolbarLayout.hiddenBars));

		for (Entry<String, List<IAction>> entry : toolBarActions.entrySet())
		{
			String bar = entry.getKey();

			boolean visible = !hiddenBars.contains(bar);
			toolMenuBarActions.get(bar).setChecked(visible);

			if (visible)
			{
				createCoolItem(bar);
			}
			else
			{
				disposeCoolItem(bar);
			}
		}

		try
		{
			if (coolbarLayout == null)
			{
				// generate default layout:
				int[] itemOrder = new int[toolBarActions.size()];
				Point[] sizes = new Point[toolBarActions.size()];
				int i = 0;
				for (String bar : toolBarActions.keySet())
				{
					itemOrder[i] = i;
					CoolItem item = toolbarCoolItems.get(bar);
					sizes[i] = item == null ? new Point(0, 0) : item.getControl().getSize();
					i++;
				}
				coolBar.setItemLayout(itemOrder, new int[0], sizes);
			}
			else
			{
				coolBar.setItemLayout(coolbarLayout.itemOrder, coolbarLayout.wrapIndices, coolbarLayout.sizes);
			}
		}
		catch (IllegalArgumentException e)
		{
			// ignore, layout not applicable to current coolbar
		}
		coolBar.layout();
	}

	@Override
	public void createPartControl(Composite parent)
	{
		if (!new DesignerPreferences().getFormToolsOnMainToolbar())
		{
			super.createPartControl(parent);
			return;
		}

		Composite c = new Composite(parent, SWT.NONE);
		c.setLayout(new org.eclipse.swt.layout.FormLayout());

		CoolBarManager coolBarManager = new CoolBarManager(SWT.WRAP | SWT.FLAT);
		coolBarManager.setContextMenuManager(getToolbarMenuManager());
		coolBar = coolBarManager.createControl(c);

		FormData formData = new FormData();
		formData.left = new FormAttachment(0);
		formData.right = new FormAttachment(100);
		formData.top = new FormAttachment(0);
		coolBar.setLayoutData(formData);
		coolBar.addListener(SWT.Resize, new Listener()
		{
			public void handleEvent(Event event)
			{
				coolBar.getParent().layout();
			}
		});
		coolBar.addListener(SWT.MouseUp, new Listener()
		{
			public void handleEvent(Event event)
			{
				saveCoolbarLayout();
			}
		});

		Composite composite = new Composite(c, SWT.NONE);
		formData = new FormData();
		formData.left = new FormAttachment(0);
		formData.right = new FormAttachment(100);
		formData.bottom = new FormAttachment(100);
		formData.top = new FormAttachment(coolBar);
		composite.setLayoutData(formData);

		composite.setLayout(new FillLayout());

		fillToolbar();

		refreshToolBars();

		super.createPartControl(composite);
	}

	protected void addToolbarAction(String bar, IAction action)
	{
		List<IAction> list = toolBarActions.get(bar);
		if (list == null)
		{
			list = new ArrayList<IAction>();
			toolBarActions.put(bar, list);
			getToolbarMenuManager().add(createCheckBarAction(bar));
		}
		if (action != null)
		{
			list.add(action);
		}
	}

	/**
	 * Creates the GraphicalViewer on the specified <code>Composite</code>.
	 * 
	 * @param parent the parent composite
	 */
	@Override
	protected void createGraphicalViewer(Composite parent)
	{
		rulerComposite = new FormRulerComposite(parent, SWT.NONE);

		GraphicalViewer viewer = new ModifiedScrollingGraphicalViewer();
		viewer.createControl(rulerComposite);
		setGraphicalViewer(viewer);

		rulerManager = new RulerManager(viewer);

		configureGraphicalViewer();
		hookGraphicalViewer();
		initializeGraphicalViewer();


		rulerComposite.setGraphicalViewer((ScrollingGraphicalViewer)getGraphicalViewer());
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
		com.servoy.eclipse.ui.Activator.getDefault().getEclipsePreferences().removePreferenceChangeListener(preferenceChangeListener);

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

	protected void applyGuidePreferences()
	{
		int guideSize = new DesignerPreferences().getGuideSize();
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

	/*
	 * Set some defaults for palette preferences.
	 */
	@Override
	protected FlyoutPreferences getPalettePreferences()
	{
		FlyoutPreferences palettePreferences = super.getPalettePreferences();
		if ((getEditorInput() instanceof PersistEditorInput && ((PersistEditorInput)getEditorInput()).isNew()) || palettePreferences.getPaletteState() == 0)
		{
			// open palette first time it is shown or when it is a new form
			palettePreferences.setPaletteState(FlyoutPaletteComposite.STATE_PINNED_OPEN);
		}
		if (palettePreferences.getDockLocation() == 0)
		{
			// default dock location to the left
			palettePreferences.setDockLocation(PositionConstants.WEST);
		}
		return palettePreferences;
	}

	@Override
	protected PaletteRoot getPaletteRoot()
	{
		if (paletteModel == null)
		{
			paletteModel = VisualFormEditorPaletteFactory.createPalette();
		}
		return paletteModel;
	}

	@Override
	protected PaletteViewerProvider createPaletteViewerProvider()
	{
		return new PaletteViewerProvider(getEditDomain())
		{
			@Override
			public PaletteViewer createPaletteViewer(Composite parent)
			{
				PaletteViewer pViewer = new PaletteViewer()
				{
					private PaletteCustomizerDialog customizerDialog;

					@Override
					public PaletteCustomizerDialog getCustomizerDialog()
					{
						if (customizerDialog == null)
						{
							customizerDialog = new PaletteCustomizerDialog(getControl().getShell(), getCustomizer(), getPaletteRoot())
							{
								private static final int DEFAULTS_ID = APPLY_ID + 3;

								@Override
								protected void createButtonsForButtonBar(Composite parent)
								{
									super.createButtonsForButtonBar(parent);
									createButton(parent, DEFAULTS_ID, "Defaults", false);
								}

								@Override
								protected void buttonPressed(int buttonId)
								{
									if (DEFAULTS_ID == buttonId)
									{
										handleDefaultsPressed();
									}
									else
									{
										super.buttonPressed(buttonId);
									}
								}

								@Override
								protected VisualFormEditorPaletteCustomizer getCustomizer()
								{
									return (VisualFormEditorPaletteCustomizer)super.getCustomizer();
								}

								protected void handleDefaultsPressed()
								{
									getCustomizer().revertToDefaults();
								}

								@Override
								public int open()
								{
									getCustomizer().initialize();
									return super.open();
								}
							};
						}
						return customizerDialog;
					}
				};
				pViewer.createControl(parent);
				configurePaletteViewer(pViewer);
				hookPaletteViewer(pViewer);
				return pViewer;
			}

			@Override
			protected void configurePaletteViewer(final PaletteViewer viewer)
			{
				super.configurePaletteViewer(viewer);

				viewer.setCustomizer(new VisualFormEditorPaletteCustomizer(getPaletteRoot()));
				viewer.getEditDomain().setDefaultTool(new PaletteSelectionTool()
				{
					@Override
					protected boolean handleKeyDown(KeyEvent e)
					{
						if (e.keyCode == SWT.ESC)
						{
							viewer.setActiveTool(null);
							return true;
						}
						return super.handleKeyDown(e);
					}
				});
				viewer.getEditDomain().loadDefaultTool();

				// native drag-and-drop from 
				viewer.addDragSourceListener(new TemplateTransferDragSourceListener(viewer));

				// refresh templates when templates are added or removed
				final IActiveProjectListener activeProjectListener = new IActiveProjectListener.ActiveProjectListener()
				{
					@Override
					public void activeProjectUpdated(ServoyProject activeProject, int updateInfo)
					{
						if (updateInfo == IActiveProjectListener.TEMPLATES_ADDED_OR_REMOVED && paletteModel != null)
						{
							VisualFormEditorPaletteFactory.refreshPalette(paletteModel);
						}
					}
				};
				ServoyModelManager.getServoyModelManager().getServoyModel().addActiveProjectListener(activeProjectListener);

				viewer.getControl().addDisposeListener(new DisposeListener()
				{
					public void widgetDisposed(DisposeEvent e)
					{
						ServoyModelManager.getServoyModelManager().getServoyModel().removeActiveProjectListener(activeProjectListener);
					}
				});
			}
		};
	}
}
