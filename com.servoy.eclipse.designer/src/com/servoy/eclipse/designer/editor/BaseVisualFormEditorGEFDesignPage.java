/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.GraphicalViewer;
import org.eclipse.gef.RootEditPart;
import org.eclipse.gef.dnd.TemplateTransferDragSourceListener;
import org.eclipse.gef.internal.ui.palette.PaletteSelectionTool;
import org.eclipse.gef.palette.PaletteRoot;
import org.eclipse.gef.ui.palette.PaletteCustomizer;
import org.eclipse.gef.ui.palette.PaletteViewer;
import org.eclipse.gef.ui.palette.PaletteViewerProvider;
import org.eclipse.gef.ui.palette.customize.PaletteCustomizerDialog;
import org.eclipse.gef.ui.parts.GraphicalViewerKeyHandler;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.DisposeEvent;
import org.eclipse.swt.events.DisposeListener;
import org.eclipse.swt.events.KeyEvent;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.core.IActiveProjectListener;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.palette.BaseVisualFormEditorPaletteCustomizer;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;

/**
 * Base editor for GEF-based form editor.
 * 
 * @author rgansevles
 *
 */
public abstract class BaseVisualFormEditorGEFDesignPage extends BaseVisualFormEditorDesignPage
{
	private PaletteRoot paletteModel;
	private IPaletteFactory paletteFactory;

	/**
	 * @param editorPart
	 */
	public BaseVisualFormEditorGEFDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	protected PaletteRoot getPaletteRoot()
	{
		if (paletteModel == null)
		{
			paletteModel = getPaletteFactory().createPalette();
		}
		return paletteModel;
	}

	/**
	 * @return
	 */
	protected IPaletteFactory getPaletteFactory()
	{
		if (paletteFactory == null)
		{
			paletteFactory = createPaletteFactory();
		}
		return paletteFactory;
	}

	protected abstract IPaletteFactory createPaletteFactory();

	protected abstract PaletteCustomizer createPaletteCustomizer();

	protected void refreshPalette()
	{
		if (paletteFactory != null && paletteModel != null)
		{
			paletteFactory.refreshPalette(paletteModel);
		}
	}


	@Override
	protected void initializeGraphicalViewer()
	{
		super.initializeGraphicalViewer();

		GraphicalViewer viewer = getGraphicalViewer();

		viewer.setRootEditPart(createRootEditPart());
		viewer.setContents(createGraphicalViewerContents());
		getEditDomain().addViewer(viewer);

		viewer.setKeyHandler(new GraphicalViewerKeyHandler(viewer));
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
								protected BaseVisualFormEditorPaletteCustomizer getCustomizer()
								{
									return (BaseVisualFormEditorPaletteCustomizer)super.getCustomizer();
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

				viewer.setCustomizer(createPaletteCustomizer());
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
						if (updateInfo == IActiveProjectListener.TEMPLATES_ADDED_OR_REMOVED)
						{
							refreshPalette();
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


	protected abstract EditPart createGraphicalViewerContents();

	protected abstract RootEditPart createRootEditPart();

	/**
	 * Refresh the visual form editor that holds the IPersist.
	 * 
	 * @param persist
	 */
	@Override
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
			EditPart ep = rootEditPart.getContents();
			if (ep instanceof FormGraphicalEditPart && persists != null && persists.size() == 1 && persists.get(0) instanceof Form &&
				(editParts != null && editParts.size() == 0))
			{
				((FormGraphicalEditPart)ep).refreshWithoutChildren();
			}
			else
			{
				ep.refresh();
			}
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
	public void refreshAllParts()
	{
		RootEditPart rootEditPart = getGraphicalViewer().getRootEditPart();
		rootEditPart.getContents().refresh();
		Iterator childrenEditPartsIte = rootEditPart.getContents().getChildren().iterator();
		while (childrenEditPartsIte.hasNext())
		{
			Object childEditPart = childrenEditPartsIte.next();
			if (childEditPart instanceof EditPart) ((EditPart)childEditPart).refresh();
		}
	}


}
