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

import java.util.Iterator;

import org.eclipse.core.runtime.preferences.IEclipsePreferences.IPreferenceChangeListener;
import org.eclipse.core.runtime.preferences.IEclipsePreferences.PreferenceChangeEvent;
import org.eclipse.draw2d.ColorConstants;
import org.eclipse.draw2d.PositionConstants;
import org.eclipse.draw2d.geometry.Dimension;
import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.SnapToGrid;
import org.eclipse.gef.rulers.RulerProvider;
import org.eclipse.gef.ui.actions.GEFActionConstants;
import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.gef.ui.parts.ScrollingGraphicalViewer;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.IWorkbenchPart;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.designer.actions.AlignmentSortPartsAction;
import com.servoy.eclipse.designer.actions.DistributeAction;
import com.servoy.eclipse.designer.actions.DistributeRequest;
import com.servoy.eclipse.designer.actions.SelectFeedbackmodeAction;
import com.servoy.eclipse.designer.actions.SelectSnapmodeAction;
import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.dnd.FormElementTransferDropTarget;
import com.servoy.eclipse.designer.editor.commands.AddAccordionPaneAction;
import com.servoy.eclipse.designer.editor.commands.AddFieldAction;
import com.servoy.eclipse.designer.editor.commands.AddMediaAction;
import com.servoy.eclipse.designer.editor.commands.AddPortalAction;
import com.servoy.eclipse.designer.editor.commands.AddSplitpaneAction;
import com.servoy.eclipse.designer.editor.commands.AddTabpanelAction;
import com.servoy.eclipse.designer.editor.commands.DesignerActionFactory;
import com.servoy.eclipse.designer.editor.commands.GroupAction;
import com.servoy.eclipse.designer.editor.commands.SameHeightAction;
import com.servoy.eclipse.designer.editor.commands.SameWidthAction;
import com.servoy.eclipse.designer.editor.commands.SaveAsTemplateAction;
import com.servoy.eclipse.designer.editor.commands.SetTabSequenceAction;
import com.servoy.eclipse.designer.editor.commands.UngroupAction;
import com.servoy.eclipse.designer.editor.palette.PaletteItemTransferDropTargetListener;
import com.servoy.eclipse.designer.editor.palette.VisualFormEditorPaletteCustomizer;
import com.servoy.eclipse.designer.editor.palette.VisualFormEditorPaletteFactory;
import com.servoy.eclipse.designer.editor.rulers.FormRulerComposite;
import com.servoy.eclipse.designer.editor.rulers.RulerManager;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.Part;

/**
 * Tab in form editor for designing the form visually.
 * 
 * @author rgansevles
 */
public class VisualFormEditorDesignPage extends BaseVisualFormEditorDesignPage
{
	public static final String COOLBAR_ACTIONS = "Actions";
	public static final String COOLBAR_ALIGN = "Alignment";
	public static final String COOLBAR_DISTRIBUTE = "Distribution";
	public static final String COOLBAR_SAMESIZE = "Sizing";
	public static final String COOLBAR_PREFS = "Editor Preferences";
	public static final String COOLBAR_ELEMENTS = "Place Element Wizards";
	public static final String COOLBAR_LAYERING = "Layering";
	public static final String COOLBAR_GROUPING = "Grouping";

	private RulerManager rulerManager;
	private FormRulerComposite rulerComposite;

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
		com.servoy.eclipse.ui.Activator.getDefault().getEclipsePreferences().addPreferenceChangeListener(preferenceChangeListener);
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

	@Override
	protected void createActions()
	{
		super.createActions();

		IAction action;

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.LEFT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.RIGHT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.TOP);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.BOTTOM);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.CENTER);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AlignmentSortPartsAction((IWorkbenchPart)getEditorPart(), PositionConstants.MIDDLE);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.HORIZONTAL_SPACING);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.HORIZONTAL_CENTERS);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.HORIZONTAL_PACK);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.VERTICAL_SPACING);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.VERTICAL_CENTERS);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new DistributeAction(getEditorPart(), DistributeRequest.Distribution.VERTICAL_PACK);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SameWidthAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SameHeightAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SetTabSequenceAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new GroupAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new UngroupAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new SaveAsTemplateAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddTabpanelAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddSplitpaneAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddAccordionPaneAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddPortalAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddMediaAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new AddFieldAction(getEditorPart());
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_BRING_TO_FRONT_ONE_STEP);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_SEND_TO_BACK);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());

		action = new ZOrderAction(getEditorPart(), ZOrderAction.ID_Z_ORDER_SEND_TO_BACK_ONE_STEP);
		getActionRegistry().registerAction(action);
		getSelectionActions().add(action.getId());
	}

	@Override
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

		addToolbarAction(COOLBAR_PREFS, getActionRegistry().getAction(DesignerActionFactory.TOGGLE_HIDE_INHERITED.getId()));

		refreshToolBars();

		GraphicalViewer viewer = getGraphicalViewer();

		if (getEditorPart().getForm() != null)
		{
			viewer.addDropTargetListener(new FormElementTransferDropTarget(getGraphicalViewer(), getEditorPart()));
			viewer.addDropTargetListener(new PaletteItemTransferDropTargetListener(getGraphicalViewer(), getEditorPart()));
		}

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
		viewer.setProperty(FormBackgroundLayer.PROPERTY_PAINT_PAGEBREAKS, Boolean.valueOf(designerPreferences.getPaintPageBreaks()));
		viewer.setProperty(RulerProvider.PROPERTY_RULER_VISIBILITY, Boolean.valueOf(designerPreferences.getShowRulers()));
		applyGuidePreferences();

		// Show rulers
		refreshRulers();

		Action action = new SelectFeedbackmodeAction(getEditorPart(), viewer);
		getActionRegistry().registerAction(action);

		action = new SelectSnapmodeAction(viewer);
		getActionRegistry().registerAction(action);

		addToolbarAction(COOLBAR_PREFS, getActionRegistry().getAction(DesignerActionFactory.SELECT_FEEDBACK.getId()));
		addToolbarAction(COOLBAR_PREFS, getActionRegistry().getAction(DesignerActionFactory.SELECT_SNAPMODE.getId()));
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
	protected VisualFormEditorPaletteFactory getPaletteFactory()
	{
		return (VisualFormEditorPaletteFactory)super.getPaletteFactory();
	}

	@Override
	protected VisualFormEditorPaletteFactory createPaletteFactory()
	{
		return new VisualFormEditorPaletteFactory();
	}

	@Override
	protected PaletteCustomizer createPaletteCustomizer()
	{
		return new VisualFormEditorPaletteCustomizer(getPaletteFactory(), getPaletteRoot());
	}


	@Override
	public void dispose()
	{
		com.servoy.eclipse.ui.Activator.getDefault().getEclipsePreferences().removePreferenceChangeListener(preferenceChangeListener);
		super.dispose();
	}
}
