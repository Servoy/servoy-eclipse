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

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONObject;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.FormEditPolicy;
import com.servoy.eclipse.designer.editor.FormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.PersistEditPolicy;
import com.servoy.eclipse.designer.editor.PersistGraphicalEditPart;
import com.servoy.eclipse.designer.editor.PersistGraphicalEditPartFigureFactory;
import com.servoy.eclipse.designer.editor.commands.AddAccordionPaneAction;
import com.servoy.eclipse.designer.editor.commands.AddFieldAction;
import com.servoy.eclipse.designer.editor.commands.AddMediaAction;
import com.servoy.eclipse.designer.editor.commands.AddPortalAction;
import com.servoy.eclipse.designer.editor.commands.AddSplitpaneAction;
import com.servoy.eclipse.designer.editor.commands.AddTabpanelAction;
import com.servoy.eclipse.designer.editor.commands.SaveAsTemplateAction;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.util.Utils;

/**
 * @author user
 *
 */
public class OpenElementWizardHandler implements IServerService
{

	private final OpenElementWizard openElementWizard;

	private final IFieldPositioner fieldPositioner;
	private final ISelectionProvider selectionProvider;
	private final BaseVisualFormEditor editorPart;

	/**
	 * @param editorPart
	 * @param selectionListener
	 * @param selectionProvider
	 */
	public OpenElementWizardHandler(BaseVisualFormEditor editorPart, IFieldPositioner fieldPositioner, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.fieldPositioner = fieldPositioner;
		this.selectionProvider = selectionProvider;
		openElementWizard = new OpenElementWizard();
	}

	class OpenElementWizard
	{
		SelectionAction fieldA, imageA, portalA, splitA, tabsA, accordionA, saveAsTemplateA;
		FormGraphicalEditPart formEditPart;

		OpenElementWizard()
		{
			formEditPart = new FormGraphicalEditPart(Activator.getDefault().getDesignClient(), editorPart);
			formEditPart.installEditPolicy(EditPolicy.COMPONENT_ROLE, new FormEditPolicy(Activator.getDefault().getDesignClient(), fieldPositioner));
		}

		void run(String wizardType)
		{
			if ("field".equals(wizardType))
			{
				getFieldAction().run();
			}
			else if ("image".equals(wizardType))
			{
				getImageAction().run();
			}
			else if ("portal".equals(wizardType))
			{
				getPortalAction().run();
			}
			else if ("tabpanel".equals(wizardType))
			{
				getTabsA().run();
			}
			else if ("splitpane".equals(wizardType))
			{
				getSplitA().run();
			}
			else if ("accordion".equals(wizardType))
			{
				getAccordionA().run();
			}
			else if ("saveastemplate".equals(wizardType))
			{
				getSaveAsTemplateA().run();
			}
		}

		SelectionAction getFieldAction()
		{
			if (fieldA == null)
			{
				fieldA = new AddFieldAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						return OpenElementWizard.this.getSelection();
					}

					@Override
					protected IPersist getContext(EditPart editPart, int typeId)
					{
						return OpenElementWizard.this.getContext(typeId);
					}
				};
			}
			return fieldA;
		}

		SelectionAction getImageAction()
		{
			if (imageA == null)
			{
				imageA = new AddMediaAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						return OpenElementWizard.this.getSelection();
					}

					@Override
					protected IPersist getContext(EditPart editPart, int typeId)
					{
						return OpenElementWizard.this.getContext(typeId);
					}
				};
			}
			return imageA;
		}

		SelectionAction getPortalAction()
		{
			if (portalA == null)
			{
				portalA = new AddPortalAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						return OpenElementWizard.this.getSelection();
					}

					@Override
					protected IPersist getContext(EditPart editPart, int typeId)
					{
						return OpenElementWizard.this.getContext(typeId);
					}
				};
			}
			return portalA;

		}

		SelectionAction getSplitA()
		{
			if (splitA == null)
			{
				splitA = new AddSplitpaneAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						return OpenElementWizard.this.getSelection();
					}

					@Override
					protected IPersist getContext(EditPart editPart, int typeId)
					{
						return OpenElementWizard.this.getContext(typeId);
					}
				};
			}
			return splitA;
		}

		SelectionAction getTabsA()
		{
			if (tabsA == null)
			{
				tabsA = new AddTabpanelAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						ISelection selection = selectionProvider.getSelection();
						if (selection instanceof StructuredSelection && ((StructuredSelection)selection).size() == 1)
						{
							Object selectedElement = ((StructuredSelection)selection).getFirstElement();
							if (selectedElement instanceof TabPanel)
							{
								IApplication application = Activator.getDefault().getDesignClient();
								Form form = formEditPart.getPersist();
								PersistGraphicalEditPart persistGraphicalEditPart = new PersistGraphicalEditPart(application, (TabPanel)selectedElement, form,
									Utils.isInheritedFormElement(selectedElement, form), new PersistGraphicalEditPartFigureFactory(application, form));
								persistGraphicalEditPart.installEditPolicy(EditPolicy.COMPONENT_ROLE, new PersistEditPolicy(application, fieldPositioner));

								return new StructuredSelection(persistGraphicalEditPart);
							}
						}
						return OpenElementWizard.this.getSelection();
					}

					@Override
					protected IPersist getContext(EditPart editPart, int typeId)
					{
						return OpenElementWizard.this.getContext(typeId);
					}
				};
			}
			return tabsA;
		}

		SelectionAction getAccordionA()
		{
			if (accordionA == null)
			{
				accordionA = new AddAccordionPaneAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						return OpenElementWizard.this.getSelection();
					}

					@Override
					protected IPersist getContext(EditPart editPart, int typeId)
					{
						return OpenElementWizard.this.getContext(typeId);
					}
				};
			}
			return accordionA;
		}


		SelectionAction getSaveAsTemplateA()
		{
			if (saveAsTemplateA == null)
			{
				saveAsTemplateA = new SaveAsTemplateAction(editorPart)
				{
					@Override
					protected ISelection getSelection()
					{
						return OpenElementWizard.this.getSelection();
					}
				};
			}
			return saveAsTemplateA;
		}

		ISelection getSelection()
		{
			return new StructuredSelection(formEditPart);
		}

		IPersist getContext(int typeId)
		{
			return typeId == IRepository.FORMS ? editorPart.getForm() : null;
		}
	}

	/**
	 * @param methodName
	 * @param args
	 */
	public Object executeMethod(String methodName, final JSONObject args)
	{
		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				openElementWizard.run(args.optString("elementType"));
			}
		});
		return null;
	}
}
