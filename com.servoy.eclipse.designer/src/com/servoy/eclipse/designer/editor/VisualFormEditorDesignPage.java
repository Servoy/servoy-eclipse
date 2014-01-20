/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.ActionRegistry;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.CoolBarManager;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.action.ToolBarContributionItem;
import org.eclipse.jface.action.ToolBarManager;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.CoolBar;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.ISelectionListener;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.designer.actions.SelectFeedbackmodeAction;
import com.servoy.eclipse.designer.actions.SelectSnapmodeAction;
import com.servoy.eclipse.designer.actions.ViewerTogglePropertyAction;
import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.dnd.FormElementTransferDropTarget;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.palette.PaletteItemTransferDropTargetListener;
import com.servoy.eclipse.designer.editor.palette.VisualFormEditorPaletteCustomizer;
import com.servoy.eclipse.designer.editor.palette.VisualFormEditorPaletteFactory;
import com.servoy.eclipse.designer.editor.rulers.FormRulerComposite;
import com.servoy.eclipse.designer.editor.rulers.RulerManager;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.preferences.DesignerPreferences.CoolbarLayout;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;

/**
 * Tab in form editor for designing the form visually.
 * 
 * @author rgansevles
 */
public class VisualFormEditorDesignPage extends BaseVisualFormEditorGEFDesignPage
{
	/**
	 * A viewer property indicating whether inherited elements are hidden. The value must  be a Boolean.
	 */
	public static final String PROPERTY_HIDE_INHERITED = "Hide.inherited"; //$NON-NLS-1$

	public static final String COOLBAR_ACTIONS = "Actions";
	public static final String COOLBAR_ALIGN = "Alignment";
	public static final String COOLBAR_DISTRIBUTE = "Distribution";
	public static final String COOLBAR_SAMESIZE = "Sizing";
	public static final String COOLBAR_PREFS = "Editor Preferences";
	public static final String COOLBAR_ELEMENTS = "Place Element Wizards";
	public static final String COOLBAR_LAYERING = "Layering";
	public static final String COOLBAR_GROUPING = "Grouping";

	private IPaletteFactory paletteFactory;
	private PaletteRoot paletteModel;
	private CoolBarManager coolBarManager;
	private List<String> hiddenBars;
	private MenuManager toolbarMenuManager;
	private final Map<String, List<IAction>> toolBarActions = new LinkedHashMap<String, List<IAction>>();
	private final Map<String, ToolBarContributionItem> toolbarContributionItems = new HashMap<String, ToolBarContributionItem>();
	private final Map<String, IAction> toolMenuBarActions = new HashMap<String, IAction>();

	private RulerManager rulerManager;
	private FormRulerComposite rulerComposite;

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

	protected final IPreferenceChangeListener preferenceChangeListener = new IPreferenceChangeListener()
	{
		public void preferenceChange(PreferenceChangeEvent event)
		{
			if (DesignerPreferences.isGuideSetting(event.getKey()))
			{
				applyGuidePreferences();
			}
			else if (DesignerPreferences.isMetricsSetting(event.getKey()))
			{
				refreshRulers();
			}
			else if (DesignerPreferences.isCoolbarSetting(event.getKey()))
			{
				refreshToolBars();
			}
			else if (DesignerPreferences.isPaletteSetting(event.getKey()))
			{
				refreshPalette();
			}
		}
	};


	/**
	 * @param editorPart
	 */
	public VisualFormEditorDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
		FormSelectionTool selectionTool = new FormSelectionTool(editorPart);
		getEditDomain().setDefaultTool(selectionTool);
		getEditDomain().setActiveTool(selectionTool);
		com.servoy.eclipse.ui.Activator.getDefault().getEclipsePreferences().addPreferenceChangeListener(preferenceChangeListener);
	}

	@Override
	protected DeleteAction createDeleteAction()
	{
		return new DeleteAction((IWorkbenchPart)editorPart)
		{
			@Override
			protected boolean calculateEnabled()
			{
				return !DesignerUtil.containsInheritedElement(getSelectedObjects()) && super.calculateEnabled();
			}
		};
	}

	@Override
	protected ISelectionListener createSelectionChangedHandler()
	{
		return new ISelectionListener()
		{
			@Override
			public void selectionChanged(IWorkbenchPart part, ISelection selection)
			{
				List<EditPart> editParts = new ArrayList<EditPart>();
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
						if (model == null)
						{
							model = Platform.getAdapterManager().getAdapter(sel, MobileListModel.class);
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

	protected void saveCoolbarLayout()
	{
		coolBarManager.update(false);
		CoolBar coolBar = coolBarManager.getControl();
		IContributionItem[] items = coolBarManager.getItems();
		String[] ids = new String[items.length];
		for (int i = 0; i < items.length; i++)
		{
			ids[i] = items[i].getId();
		}
		new DesignerPreferences().saveCoolbarLayout(new CoolbarLayout(coolBar.getItemOrder(), coolBar.getWrapIndices(), coolBar.getItemSizes(),
			hiddenBars.toArray(new String[hiddenBars.size()]), ids));
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
				if (hiddenBars.remove(bar))
				{
					createContributionItem(bar);
				}
				else
				{
					hiddenBars.add(bar);
					disposeContributionItem(bar);
				}
				saveCoolbarLayout();
			}
		};
		toolMenuBarActions.put(bar, action);
		return action;
	}

	private void createContributionItem(final String bar)
	{
		if (!toolbarContributionItems.containsKey(bar))
		{
			ToolBarManager toolBarManager = new ToolBarManager(SWT.FLAT);
			ToolBarContributionItem item = new ToolBarContributionItem(toolBarManager, bar);
			coolBarManager.add(item);

			for (IAction action : toolBarActions.get(bar))
			{
				toolBarManager.add(action);
			}

			toolbarContributionItems.put(bar, item);
		}
	}

	private void disposeContributionItem(String bar)
	{
		ToolBarContributionItem item = toolbarContributionItems.remove(bar);
		if (item != null)
		{
			coolBarManager.remove(item);
		}
	}

	protected void refreshToolBars()
	{
		CoolbarLayout coolbarLayout = new DesignerPreferences().getCoolbarLayout();

		if (coolBarManager == null && !new DesignerPreferences().getFormToolsOnMainToolbar()) return;

		if (coolbarLayout == null)
		{
			// reset
			coolBarManager.removeAll();
			toolbarContributionItems.clear();
			hiddenBars = new ArrayList<String>();
			coolBarManager.update(false);
		}
		else
		{
			hiddenBars = new ArrayList<String>(Arrays.asList(coolbarLayout.hiddenBars));
		}

		// determine the order to create the items
		List<String> bars = new ArrayList<String>();
		if (coolbarLayout != null && coolBarManager.isEmpty())
		{
			// use order from coolbarLayout
			for (String id : coolbarLayout.ids)
			{
				bars.add(id);
			}
		}
		// add default order (duplicates will be ignored later)
		bars.addAll(toolBarActions.keySet());

		// create or dispose items
		for (String bar : bars)
		{
			boolean visible = !hiddenBars.contains(bar);
			if (toolMenuBarActions.containsKey(bar))
			{
				toolMenuBarActions.get(bar).setChecked(visible);
			}

			if (visible)
			{
				createContributionItem(bar);
			}
			else
			{
				disposeContributionItem(bar);
			}
		}

		coolBarManager.update(false);

		if (coolbarLayout != null)
		{
			try
			{
				coolBarManager.getControl().setItemLayout(coolbarLayout.itemOrder, coolbarLayout.wrapIndices, coolbarLayout.sizes);
			}
			catch (IllegalArgumentException e)
			{
				// ignore, layout not applicable to current coolbar
			}
		}
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

		coolBarManager = new CoolBarManager(SWT.WRAP | SWT.FLAT);
		coolBarManager.setContextMenuManager(getToolbarMenuManager());
		CoolBar coolBar = coolBarManager.createControl(c);

		FormData formData = new FormData();
		formData.left = new FormAttachment(0);
		formData.right = new FormAttachment(100);
		formData.top = new FormAttachment(0);
		coolBar.setLayoutData(formData);
		coolBar.addListener(SWT.Resize, new Listener()
		{
			public void handleEvent(Event event)
			{
				coolBarManager.getControl().getParent().layout();
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

	@Override
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

	/**
	 * @see AbstractGraphicalEditor#createGraphicalViewerContents()
	 */
	@Override
	protected EditPart createGraphicalViewerContents()
	{
		return new FormGraphicalEditPart(Activator.getDefault().getDesignClient(), getEditorPart());
	}

	@Override
	protected void configureGraphicalViewer()
	{
		getGraphicalViewer().getControl().setBackground(ColorConstants.lightGray);
		getGraphicalViewer().addSelectionChangedListener(new ISelectionChangedListener()
		{
			// Show the size and location of the current selection in the status line
			public void selectionChanged(SelectionChangedEvent event)
			{
				StringBuilder sb = null;
				if (event.getSelection() instanceof IStructuredSelection)
				{
					Iterator< ? > iterator = ((IStructuredSelection)event.getSelection()).iterator();
					while (iterator.hasNext())
					{
						Object next = iterator.next();
						if (next instanceof EditPart)
						{
							if (((EditPart)next).getModel() instanceof ISupportBounds)
							{
								ISupportBounds supportBounds = (ISupportBounds)((EditPart)next).getModel();
								java.awt.Point location = supportBounds.getLocation();
								java.awt.Dimension size = supportBounds.getSize();
								if (location != null && size != null)
								{
									if (sb == null)
									{
										sb = new StringBuilder();
									}
									else
									{
										if (sb.length() > 50)
										{
											// too much information
											break;
										}
										sb.append(' ');
									}
									sb.append('(')//
									.append(location.x).append(',').append(location.y)//
									.append(' ')//
									.append(size.width).append('x').append(size.height)//
									.append(' ')//
									.append(location.x + size.width).append(',').append(location.y + size.height)//
									.append(')');
								}
							}
							else if (((EditPart)next).getModel() instanceof Part)
							{
								Part part = (Part)((EditPart)next).getModel();
								if (sb == null)
								{
									sb = new StringBuilder();
								}
								else
								{
									sb.append(' ');
								}

								sb.append(part.getEditorName());
								sb.append(' ').append(part.getHeight());
								Part prev = DesignerUtil.getPreviousPart(part);
								if (prev != null && prev.getHeight() > 0)
								{
									sb.append(" (").append(part.getHeight() - prev.getHeight()).append(')'); //$NON-NLS-1$
								}
							}
						}
					}
					EditorUtil.setStatuslineMessage(sb == null ? null : sb.toString());
				}
			}
		});
	}

	@Override
	protected RootEditPart createRootEditPart()
	{
		return new FormGraphicalRootEditPart(getEditorPart());
	}

	protected void fillToolbar()
	{
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_FIELD.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_MEDIA.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_PORTAL.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_SPLITPANE.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_TAB.getId()));
		addToolbarAction(COOLBAR_ELEMENTS, getActionRegistry().getAction(DesignerActionFactory.ADD_ACCORDION.getId()));

		addToolbarAction(COOLBAR_LAYERING, getActionRegistry().getAction(ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP));
		addToolbarAction(COOLBAR_LAYERING, getActionRegistry().getAction(ZOrderAction.ID_Z_ORDER_SEND_TO_BACK_ONE_STEP));
		addToolbarAction(COOLBAR_LAYERING, getActionRegistry().getAction(ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT));
		addToolbarAction(COOLBAR_LAYERING, getActionRegistry().getAction(ZOrderAction.ID_Z_ORDER_SEND_TO_BACK));

		addToolbarAction(COOLBAR_GROUPING, getActionRegistry().getAction(DesignerActionFactory.GROUP.getId()));
		addToolbarAction(COOLBAR_GROUPING, getActionRegistry().getAction(DesignerActionFactory.UNGROUP.getId()));

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

		addToolbarAction(COOLBAR_ACTIONS, getActionRegistry().getAction(DesignerActionFactory.SET_TAB_SEQUENCE.getId()));
		addToolbarAction(COOLBAR_ACTIONS, getActionRegistry().getAction(DesignerActionFactory.SAVE_AS_TEMPLATE.getId()));
	}

	@Override
	protected void initializeGraphicalViewer()
	{
		super.initializeGraphicalViewer();

		GraphicalViewer viewer = getGraphicalViewer();

		IAction action = new ViewerTogglePropertyAction(editorPart, viewer, DesignerActionFactory.TOGGLE_HIDE_INHERITED.getId(),
			DesignerActionFactory.TOGGLE_HIDE_INHERITED_TEXT, DesignerActionFactory.TOGGLE_HIDE_INHERITED_TOOLTIP,
			DesignerActionFactory.TOGGLE_HIDE_INHERITED_IMAGE, PROPERTY_HIDE_INHERITED)
		{
			@Override
			protected boolean calculateEnabled()
			{
				return editorPart.getForm().getExtendsID() > 0;
			}
		};
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		viewer.addPropertyChangeListener(new PropertyChangeListener()
		{
			@Override
			public void propertyChange(PropertyChangeEvent evt)
			{
				if (evt.getPropertyName().equals(PROPERTY_HIDE_INHERITED))
				{
					getGraphicalViewer().getRootEditPart().getContents().refresh();
				}
			}
		});

		addToolbarAction(COOLBAR_PREFS, getActionRegistry().getAction(DesignerActionFactory.TOGGLE_HIDE_INHERITED.getId()));

		if (getEditorPart().getForm() != null)
		{
			viewer.addDropTargetListener(new FormElementTransferDropTarget(getGraphicalViewer(), getEditorPart()));
			viewer.addDropTargetListener(new PaletteItemTransferDropTargetListener(getGraphicalViewer(), getEditorPart()));
		}

		// configure the context menu provider
		String id = "#FormDesignerContext";
		VisualFormEditorContextMenuProvider cmProvider = new VisualFormEditorContextMenuProvider(id, getActionRegistry());
		viewer.setContextMenu(cmProvider);
		getSite().registerContextMenu(id, cmProvider, viewer);

		DesignerPreferences designerPreferences = new DesignerPreferences();
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_VISIBLE, Boolean.valueOf(designerPreferences.getFeedbackGrid()));
		viewer.setProperty(SnapToGrid.PROPERTY_GRID_ENABLED, Boolean.valueOf(designerPreferences.getGridSnapTo()));
		viewer.setProperty(SnapToElementAlignment.PROPERTY_ALIGNMENT_ENABLED, Boolean.valueOf(designerPreferences.getAlignmentSnapTo()));
		viewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_ANCHOR_FEEDBACK_VISIBLE, Boolean.valueOf(designerPreferences.getShowAnchorFeedback()));
		viewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_SAME_SIZE_FEEDBACK_VISIBLE, Boolean.valueOf(designerPreferences.getShowSameSizeFeedback()));
		viewer.setProperty(AlignmentfeedbackEditPolicy.PROPERTY_ALIGMENT_FEEDBACK_VISIBLE, Boolean.valueOf(designerPreferences.getFeedbackAlignment()));
		viewer.setProperty(FormBackgroundLayer.PROPERTY_PAINT_PAGEBREAKS, Boolean.valueOf(designerPreferences.getPaintPageBreaks()));
		viewer.setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, Boolean.valueOf(designerPreferences.getShowRulers()));
		applyGuidePreferences();

		// Show rulers
		refreshRulers();

		action = new SelectFeedbackmodeAction(getEditorPart(), viewer);
		getActionRegistry().registerAction(action);

		action = new SelectSnapmodeAction(viewer);
		getActionRegistry().registerAction(action);

		addToolbarAction(COOLBAR_PREFS, getActionRegistry().getAction(DesignerActionFactory.SELECT_FEEDBACK.getId()));
		addToolbarAction(COOLBAR_PREFS, getActionRegistry().getAction(DesignerActionFactory.SELECT_SNAPMODE.getId()));

		refreshToolBars();
	}

	@Override
	protected Control getGraphicalControl()
	{
		return rulerComposite;
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

	protected void applyGuidePreferences()
	{
		int guideSize = new DesignerPreferences().getGuideSize();
		getGraphicalViewer().setProperty(SnapToGrid.PROPERTY_GRID_SPACING, new Dimension(guideSize, guideSize));
	}


	protected void refreshRulers()
	{
		rulerManager.refreshRulers();
	}


	@Override
	protected VisualFormEditorPaletteFactory createPaletteFactory()
	{
		return new VisualFormEditorPaletteFactory();
	}

	@Override
	protected PaletteCustomizer createPaletteCustomizer()
	{
		return new VisualFormEditorPaletteCustomizer((VisualFormEditorPaletteFactory)getPaletteFactory(), getPaletteRoot());
	}


	@Override
	public void dispose()
	{
		getEditDomain().getCommandStack().removeCommandStackListener(editorPart);
		com.servoy.eclipse.ui.Activator.getDefault().getEclipsePreferences().removePreferenceChangeListener(preferenceChangeListener);
		super.dispose();
	}
}
