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

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.NotEnabledException;
import org.eclipse.core.commands.NotHandledException;
import org.eclipse.core.commands.common.NotDefinedException;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.handlers.IHandlerService;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.specification.property.CustomJSONObjectType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.rfb.palette.PaletteCommonsHandler;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.autowizard.FormComponentTreeSelectDialog;
import com.servoy.eclipse.ui.dialogs.autowizard.PropertyWizardDialogConfigurator;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionLayoutContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.ChildWebComponent;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistCloneable;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;

/**
 * @author user
 *
 */
public class CreateComponentHandler implements IServerService
{

	protected final ISelectionProvider selectionProvider;
	protected final BaseVisualFormEditor editorPart;
	private final AtomicInteger id = new AtomicInteger();

	public CreateComponentHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	public Object executeMethod(String methodName, final JSONObject args)
	{
		if (editorPart.getGraphicaleditor() instanceof RfbVisualFormEditorDesignPage &&
			((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).getShowedContainer() != null && args.has("name") && !args.has("dropTargetUUID"))
		{
			// if we drop directly on form while zoomed in , do nothing
			return null;
		}
		if (args.has("name") && ("component".equals(args.getString("name")) || "template".equals(args.getString("name")) || "*".equals(args.getString("name"))))
		{
			String name = args.getString("name");
			IHandlerService handlerService = PlatformUI.getWorkbench().getService(IHandlerService.class);
			Command command = (PlatformUI.getWorkbench().getService(ICommandService.class)).getCommand(AddContainerCommand.COMMAND_ID);
			ExecutionEvent executionEvent = null;
			try
			{
				if ("*".equals(name) || "component".equals(name))
				{
					final Event trigger = new Event();
					executionEvent = handlerService.createExecutionEvent(command, trigger);
				}
				else if ("template".equals(name))
				{

					Map<String, String> parameters = new HashMap<>();
					parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.template", "*");
					executionEvent = new ExecutionEvent(command, parameters, new Event(), null);
				}
				command.executeWithChecks(executionEvent);
			}
			catch (ExecutionException | NotDefinedException | NotEnabledException | NotHandledException e)
			{
				Debug.log(e);
			}
			return null;
		}

		Display.getDefault().asyncExec(new Runnable()
		{
			public void run()
			{
				final IStructuredSelection[] newSelection = new IStructuredSelection[1];
				editorPart.getCommandStack().execute(new BaseRestorableCommand("createComponent")
				{
					private IPersist[] newPersist;

					@Override
					public void execute()
					{
						try
						{
							List<IPersist> changedPersists = new ArrayList<IPersist>();
							newPersist = createComponent(args, changedPersists);
							if (newPersist != null)
							{
								changedPersists.addAll(Arrays.asList(newPersist));
								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changedPersists);
								if (!args.optBoolean("keepOldSelection", false))
								{
									newSelection[0] = new StructuredSelection(
										newPersist.length > 0 ? PersistContext.create(newPersist[0], editorPart.getForm()) : newPersist);
								}
								if (newPersist.length == 1 && newPersist[0] instanceof LayoutContainer &&
									CSSPositionUtils.isCSSPositionContainer((LayoutContainer)newPersist[0]))
								{
									if (org.eclipse.jface.dialogs.MessageDialog.openQuestion(UIUtils.getActiveShell(), "Edit css position container",
										"Do you want to zoom into the layout container so you can edit it ?"))
									{
										BaseVisualFormEditor editor = DesignerUtil.getActiveEditor();
										if (editor != null)
										{
											BaseVisualFormEditorDesignPage activePage = editor.getGraphicaleditor();
											if (activePage instanceof RfbVisualFormEditorDesignPage)
												((RfbVisualFormEditorDesignPage)activePage).showContainer((LayoutContainer)newPersist[0]);
										}
									}
								}
							}
						}
						catch (Exception ex)
						{
							Debug.error(ex);
						}
					}

					@Override
					public void undo()
					{
						try
						{
							if (newPersist != null)
							{
								for (IPersist iPersist : newPersist)
								{
									((IDeveloperRepository)iPersist.getRootObject().getRepository()).deleteObject(iPersist);
								}

								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, Arrays.asList(newPersist));
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError("Could not undo create elements", e);
						}
					}

				});

				if (newSelection[0] != null) selectionProvider.setSelection(newSelection[0]);
			}
		});
		return null;
	}

	protected IPersist[] createComponent(final JSONObject args, final List<IPersist> extraChangedPersists) throws JSONException, RepositoryException
	{
		int x = args.optInt("x");
		int y = args.optInt("y");
		int w = args.optInt("w");
		int h = args.optInt("h");
		if (args.has("type"))
		{
			// a ghost dragged from the palette. it is defined in the "types" section of the .spec file
			IPersist next = PersistFinder.INSTANCE.searchForPersist(editorPart, (String)args.get("dropTargetUUID"));
			int arrayIndex = -1;
			if (next instanceof IChildWebObject)
			{
				arrayIndex = ((IChildWebObject)next).getIndex();
				next = next.getParent();
			}
			if (next instanceof BaseComponent)
			{
				if (next instanceof IBasicWebComponent)
				{
					IBasicWebComponent parentBean = (IBasicWebComponent)next;
					String propertyName = args.getString("ghostPropertyName");
					String compName = "component_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "component_" + id.incrementAndGet();
					}
					parentBean = (IBasicWebComponent)ElementUtil.getOverridePersist(PersistContext.create(parentBean, editorPart.getForm()));
					WebCustomType bean = AddContainerCommand.addCustomType(parentBean, propertyName, compName, arrayIndex, null);
					return new IPersist[] { bean };
				}
				else if (args.getString("type").equals("tab"))
				{
					if (next instanceof ISupportChilds)
					{
						ISupportChilds iSupportChilds = (ISupportChilds)next;
						iSupportChilds = (ISupportChilds)ElementUtil.getOverridePersist(PersistContext.create(iSupportChilds, editorPart.getForm()));
						Tab newTab = (Tab)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(iSupportChilds, IRepository.TABS);
						String tabName = "tab_" + id.incrementAndGet();
						while (!PersistFinder.INSTANCE.checkName(editorPart, tabName))
						{
							tabName = "tab_" + id.incrementAndGet();
						}
						newTab.setText(tabName);
						newTab.setLocation(new Point(x, y));
						iSupportChilds.addChild(newTab);
						return new IPersist[] { newTab };
					}
				}
			}
		}
		else if (args.has("name") || args.has("uuid"))
		{
			ISupportFormElements parentSupportingElements = editorPart.getForm();
			IPersist dropTarget = null;
			IPersist initialDropTarget = null;
			if (args.has("dropTargetUUID"))
			{
				dropTarget = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getString("dropTargetUUID"));
				if (dropTarget != null)
				{
					initialDropTarget = dropTarget;
					dropTarget = ElementUtil.getOverridePersist(PersistContext.create(dropTarget, editorPart.getForm()));
				}
				if (dropTarget != null)
				{
					IPersist p = dropTarget;
					while (!(p instanceof ISupportFormElements) && p != null)
					{
						p = p.getParent();
					}
					if (p instanceof ISupportFormElements)
					{
						parentSupportingElements = (ISupportFormElements)p;
					}
				}
			}

			if (args.has("name"))
			{
				String name = args.getString("name");
				if (args.has("packageName")) //ghost components has no packageName
				{
					PaletteCommonsHandler.getInstance().updateComponentCounter(name);
				}
				if (dropTarget instanceof WebComponent)
				{
					// see if target has a 'component' or 'component[]' typed property
					WebComponent parentWebComponent = (WebComponent)dropTarget;
					PropertyDescription propertyDescription = ((WebObjectImpl)parentWebComponent.getImplementation()).getPropertyDescription();

					// TODO add a visual way for the user to drop to a specific property (if there is more then one property that supports components)
					// TODO also add a way of adding to a specific index in a component array and also just moving component ghosts in a component array property
					for (String propertyName : new TreeSet<String>(propertyDescription.getAllPropertiesNames()))
					{
						PropertyDescription property = propertyDescription.getProperty(propertyName);
						if (property != null)
						{
							if (property.getType() instanceof ComponentPropertyType)
							{
								// simple component type
								return new IPersist[] { createNestedWebComponent(parentWebComponent, property, name, propertyName, -1, x, y, w, h) };
							}
							else if (PropertyUtils.isCustomJSONArrayPropertyType(property.getType()) &&
								((CustomJSONArrayType< ? , ? >)property.getType()).getCustomJSONTypeDefinition().getType() instanceof ComponentPropertyType)
							{
								// array of component types
								int index = 0;
								IChildWebObject[] arrayOfChildComponents = (IChildWebObject[])parentWebComponent.getProperty(propertyName);
								if (arrayOfChildComponents != null) index = arrayOfChildComponents.length;
								return new IPersist[] { createNestedWebComponent(parentWebComponent,
									((CustomJSONArrayType< ? , ? >)property.getType()).getCustomJSONTypeDefinition(), name, propertyName, index, x, y, w, h) };
							}
						}
					} // if we found no property to drop to, just continue with code below - it will be dropped on form
				}

				if ("servoydefault-button".equals(name))
				{
					GraphicalComponent gc = parentSupportingElements.createNewGraphicalComponent(new Point(x, y));
					gc.setText("button");
					gc.setOnActionMethodID(-1);
					gc.setRolloverCursor(Cursor.HAND_CURSOR);
					CSSPositionUtils.setLocation(gc, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(gc, w, h);
					if (args.has("styleClass"))
					{
						gc.setStyleClass(args.getString("styleClass"));
					}
					return new IPersist[] { gc };
				}
				else if ("servoydefault-label".equals(name))
				{
					GraphicalComponent gc = parentSupportingElements.createNewGraphicalComponent(new Point(x, y));
					gc.setText(args.has("text") ? args.getString("text") : "label");
					CSSPositionUtils.setLocation(gc, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(gc, w, h);
					if (args.has("styleClass"))
					{
						gc.setStyleClass(args.getString("styleClass"));
					}
					return new IPersist[] { gc };
				}
				else if ("servoydefault-combobox".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.COMBOBOX);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-textfield".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.TEXT_FIELD);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-textarea".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.TEXT_AREA);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-password".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.PASSWORD);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-calendar".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.CALENDAR);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-typeahead".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.TYPE_AHEAD);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-spinner".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.SPINNER);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-check".equals(name) || "servoydefault-checkgroup".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.CHECKS);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-radio".equals(name) || "servoydefault-radiogroup".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.RADIOS);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-imagemedia".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.IMAGE_MEDIA);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-listbox".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.LIST_BOX);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-htmlarea".equals(name))
				{
					Field field = parentSupportingElements.createNewField(new Point(x, y));
					field.setDisplayType(Field.HTML_AREA);
					field.setEditable(true);
					CSSPositionUtils.setLocation(field, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(field, w, h);
					return new IPersist[] { field };
				}
				else if ("servoydefault-tabpanel".equals(name))
				{
					String compName = "tabpanel_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "tabpanel_" + id.incrementAndGet();
					}
					TabPanel tabPanel = null;
					if (parentSupportingElements instanceof AbstractContainer)
					{
						tabPanel = ((AbstractContainer)parentSupportingElements).createNewTabPanel(compName);
					}
					else
					{
						tabPanel = editorPart.getForm().createNewTabPanel(compName);
					}
					CSSPositionUtils.setLocation(tabPanel, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(tabPanel, w, h);
					return new IPersist[] { tabPanel };
				}
				else if ("servoydefault-splitpane".equals(name))
				{
					String compName = "tabpanel_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "tabpanel_" + id.incrementAndGet();
					}
					TabPanel tabPanel = null;
					if (parentSupportingElements instanceof AbstractContainer)
					{
						tabPanel = ((AbstractContainer)parentSupportingElements).createNewTabPanel(compName);
					}
					else
					{
						tabPanel = editorPart.getForm().createNewTabPanel(compName);
					}
					tabPanel.setTabOrientation(TabPanel.SPLIT_HORIZONTAL);
					CSSPositionUtils.setLocation(tabPanel, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(tabPanel, w, h);
					return new IPersist[] { tabPanel };
				}
				else if ("servoycore-portal".equals(name))
				{
					String compName = "portal_" + id.incrementAndGet();
					while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
					{
						compName = "portal_" + id.incrementAndGet();
					}
					Portal portal = null;
					if (parentSupportingElements instanceof AbstractContainer)
					{
						portal = ((AbstractContainer)parentSupportingElements).createNewPortal(compName, new Point(x, y));
					}
					else
					{
						portal = editorPart.getForm().createNewPortal(compName, new Point(x, y));
					}
					CSSPositionUtils.setLocation(portal, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(portal, w, h);
					return new IPersist[] { portal };
				}
				else if ("servoydefault-rectangle".equals(name))
				{
					RectShape shape = editorPart.getForm().createNewRectangle(new Point(x, y));
					shape.setLineSize(1);
					CSSPositionUtils.setLocation(shape, x, y);
					if (w != 0 || h != 0) CSSPositionUtils.setSize(shape, w, h);
					return new IPersist[] { shape };
				}
				else
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(name);
					if (spec != null)
					{
						String compName = null;
						String componentName = spec.getDisplayName().replaceAll("\\s", "").toLowerCase();
						componentName = componentName.replaceAll("-", "_");
						compName = componentName + "_" + id.incrementAndGet();
						while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
						{
							compName = componentName + "_" + id.incrementAndGet();
						}

						WebComponent webComponent = null;
						if (parentSupportingElements instanceof Portal)
						{
							Portal portal = (Portal)parentSupportingElements;
							webComponent = (WebComponent)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(portal,
								IRepository.WEBCOMPONENTS);
							webComponent.setProperty("text", compName); //default
							if (args.has("text"))
							{
								webComponent.setProperty("text", args.getString("text"));
							}

							webComponent.setTypeName(name);
							portal.addChild(webComponent);
						}
						else if (parentSupportingElements instanceof AbstractContainer)
						{
							webComponent = ((AbstractContainer)parentSupportingElements).createNewWebComponent(compName, name);

						}
						CSSPositionUtils.setLocation(webComponent, x, y);
						CSSPositionUtils.setSize(webComponent, w, h);
						PropertyDescription description = spec.getProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
						if ((w == 0 && h == 0) && description != null && description.getDefaultValue() instanceof JSONObject)
						{
							CSSPositionUtils.setSize(webComponent, ((JSONObject)description.getDefaultValue()).optInt("width", 80),
								((JSONObject)description.getDefaultValue()).optInt("height", 80));
						}
						else if (w == 0 && h == 0)
						{
							CSSPositionUtils.setSize(webComponent, 200, 100);
						}
						Collection<String> allPropertiesNames = spec.getAllPropertiesNames();
						for (String propertyName : allPropertiesNames)
						{
							PropertyDescription property = spec.getProperty(propertyName);
							if (property != null)
							{
								if (args.has(propertyName) && webComponent.getProperty(propertyName) == null)
								{
									webComponent.setProperty(propertyName, args.opt(propertyName));
									if (property.getType() == FormComponentPropertyType.INSTANCE)
									{
										FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
										Form form = FormComponentPropertyType.INSTANCE.getForm(args.opt(propertyName), flattenedSolution);
										if (form != null)
										{
											Dimension size = form.getSize();
											if (size.height == 0)
											{
												size.height = CSSPositionUtils.getSize(webComponent).height;
											}
											CSSPositionUtils.setSize(webComponent, size.width, size.height);
										}
									}
								}
								else if (property.getInitialValue() != null)
								{
									Object initialValue = property.getInitialValue();
									if (initialValue != null) webComponent.setProperty(propertyName, initialValue);
								}
								if ("autoshow".equals(property.getTag("wizard")))
								{
									if (property.getType() == FormComponentPropertyType.INSTANCE && property.getConfig() instanceof ComponentTypeConfig &&
										((ComponentTypeConfig)property.getConfig()).forFoundset != null)
									{
										// list form component
										FormComponentTreeSelectDialog.selectFormComponent(webComponent, editorPart.getForm());
									}
									else
									{
										autoshowWizard(parentSupportingElements, spec, webComponent, property, editorPart, id);
									}
								}
							}
						}
						List<IPersist> changes = new ArrayList<>();
						boolean addSiblingsToChanges = true;
						if (editorPart.getForm().isResponsiveLayout() || webComponent.getParent() instanceof CSSPositionLayoutContainer)
						{
							if (initialDropTarget != null &&
								!initialDropTarget.getUUID().equals(webComponent.getParent().getUUID()))
							{
								ISupportChilds parent = webComponent.getParent();
								changes.add(webComponent.getParent());
								addSiblingsToChanges = false;//no need to mark the siblings as changed because the whole parent was overridden

								FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
								parent = PersistHelper.getFlattenedPersist(flattenedSolution, editorPart.getForm(), parent);
								Iterator<IPersist> it = parent.getAllObjects();
								while (it.hasNext())
								{
									IPersist child = it.next();
									IPersist overridePersist = ElementUtil.getOverridePersist(PersistContext.create(child, editorPart.getForm()));
									if (!overridePersist.getUUID().equals(child.getUUID()))
									{
										parent.removeChild(child);
										// do not add the override again, the getOverridePersist should already create it in the right place (probably directly on form)
										//parent.addChild(overridePersist);
									}
								}
							}
							else
							{
								changes.add(webComponent);
							}
							webComponent.setLocation(getLocationAndShiftSiblings(webComponent.getParent(), args, extraChangedPersists));
							// we don't need to add the changed components
							//if (addSiblingsToChanges) changes.addAll(extraChangedPersists);
						}
						else
						{
							changes.add(webComponent);
						}
						return changes.toArray(new IPersist[changes.size()]);
					}
					else
					{
						PackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications()
							.get(
								args.optString("packageName"));
						if (specifications != null)
						{
							WebLayoutSpecification layoutSpec = specifications.getSpecification(name);
							if (layoutSpec != null)
							{
								Iterator<IPersist> childContainersIte = parentSupportingElements.getObjects(IRepositoryConstants.LAYOUTCONTAINERS);
								LayoutContainer sameTypeChildContainer = null;
								if (!"div".equals(layoutSpec.getName()))
								{
									while (childContainersIte.hasNext())
									{
										LayoutContainer childContainer = (LayoutContainer)childContainersIte.next();
										if (layoutSpec.getName().equals(childContainer.getSpecName()))
										{
											sameTypeChildContainer = childContainer;
										}
									}
								}
								JSONObject config = layoutSpec.getConfig() instanceof String ? new JSONObject((String)layoutSpec.getConfig()) : null;
								boolean fullRefreshNeeded = initialDropTarget != null && !initialDropTarget.equals(dropTarget) &&
									initialDropTarget.getParent() instanceof Form;
								// this is a fix for dropping the responsive container on csspos
								Point p = new Point(x, y);
								List<IPersist> res = createLayoutContainer(parentSupportingElements, layoutSpec, sameTypeChildContainer, config, p,
									specifications, args.optString("packageName"));
								if (dropTarget != null && !dropTarget.equals(initialDropTarget))
								{
									res.add(dropTarget);
								}
//								else if (!fullRefreshNeeded && !res.isEmpty() && res.get(0).getParent() instanceof Form)
//								{
//									LayoutContainer layoutContainer = (LayoutContainer)res.get(0);
//									List<IPersist> children = new ArrayList<>(((AbstractContainer)layoutContainer.getParent()).getAllObjectsAsList());
//									Collections.sort(children, PositionComparator.XY_PERSIST_COMPARATOR);
//									//only refresh if it's not the last element
//									fullRefreshNeeded = !layoutContainer.getUUID().equals(children.get(children.size() - 1).getUUID());
//								}
								IPersist[] result = res.toArray(new IPersist[0]);
								if (fullRefreshNeeded)
								{
									IEditorReference[] editorRefs = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getEditorReferences();
									for (IEditorReference editorRef : editorRefs)
									{
										IEditorPart editor = editorRef.getEditor(false);
										if (editor instanceof BaseVisualFormEditor)
										{
											BaseVisualFormEditorDesignPage activePage = ((BaseVisualFormEditor)editor).getGraphicaleditor();
											if (activePage instanceof RfbVisualFormEditorDesignPage)
												((RfbVisualFormEditorDesignPage)activePage).refreshContent();
											break;
										}
									}
								}
								return result;
							}
						}
						else
						{
							for (IRootObject template : ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(IRepository.TEMPLATES))
							{
								if (template.getName().equals(name))
								{
									Point p = getLocationAndShiftSiblings(parentSupportingElements, args, extraChangedPersists);
									Object[] applyTemplate = ElementFactory.applyTemplate(parentSupportingElements,
										new TemplateElementHolder((Template)template), new org.eclipse.swt.graphics.Point(p.x, p.y), false);
									if (applyTemplate.length > 0)
									{
										if (applyTemplate[0] instanceof FormElementGroup)
										{
											Iterator<IFormElement> elements = ((FormElementGroup)applyTemplate[0]).getElements();
											//convert iterator to []
											ArrayList<IFormElement> list = new ArrayList<>();
											while (elements.hasNext())
											{
												IFormElement next = elements.next();
												list.add(next);
											}
											return list.toArray(new IPersist[list.size()]);
										}
										else
										{ //Object[] to IPersist[]
											return Arrays.asList(applyTemplate).toArray(new IPersist[applyTemplate.length]);
										}
									}
								}
							}
						}
					}
				}
			}
			else if (args.has("uuid"))
			{
				IPersist persist = PersistFinder.INSTANCE.searchForPersist(editorPart, args.getString("uuid"));
				if (persist instanceof AbstractBase)
				{
					IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
					ISupportChilds parent = dropTarget instanceof ISupportChilds ? (ISupportChilds)dropTarget : persist.getParent();
					IPersist newPersist = ((AbstractBase)persist).cloneObj(parent, true, validator, true, true, true);
					Point p = getLocationAndShiftSiblings(parent, args, extraChangedPersists);
					CSSPositionUtils.setLocation((ISupportBounds)newPersist, p.x, p.y);
					if (w > 0 && h > 0) CSSPositionUtils.setSize((ISupportBounds)newPersist, w, h);

					final ArrayList<IPersist> newPersists = new ArrayList<IPersist>();
					newPersist.acceptVisitor(new IPersistVisitor()
					{
						@Override
						public Object visit(IPersist o)
						{
							if (o instanceof IPersistCloneable)
							{
								newPersists.add(o);
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});

					return newPersists.toArray(new IPersist[newPersists.size()]);
				}
			}
		}

		return null;
	}

	public static void autoshowWizard(ISupportFormElements parentSupportingElements, WebObjectSpecification spec,
		WebComponent webComponent, PropertyDescription property, BaseVisualFormEditor editorPart, AtomicInteger id)
	{

		// prop type should be an array of a custom type..
		IPropertyType< ? > propType = property.getType();
		if (propType instanceof CustomJSONArrayType< ? , ? >)
		{

			CustomJSONObjectType< ? , ? > customObjectType = (CustomJSONObjectType< ? , ? >)((CustomJSONArrayType< ? , ? >)propType)
				.getCustomJSONTypeDefinition().getType();
			PropertyDescription customObjectDefinition = customObjectType.getCustomJSONTypeDefinition();
			Collection<PropertyDescription> wizardProperties = customObjectDefinition.getTaggedProperties("wizard");
			if (wizardProperties.size() > 0)
			{
				// feed this wizardProperties into the wizard
				System.err.println(wizardProperties);
				Display current = Display.getCurrent();
				if (current == null) current = Display.getDefault();

				PersistContext context = PersistContext.create(webComponent, parentSupportingElements);
				FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(webComponent);
				ITable table = ServoyModelFinder.getServoyModel().getDataSourceManager()
					.getDataSource(flattenedSolution.getFlattenedForm(editorPart.getForm()).getDataSource());

				PropertyWizardDialogConfigurator dialogConfigurator = new PropertyWizardDialogConfigurator(current.getActiveShell(),
					context,
					flattenedSolution, property).withTable(table).withProperties(wizardProperties);
				if (dialogConfigurator.open() == Window.OK)
				{
					List<Map<String, Object>> result = dialogConfigurator.getResult();
					String typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(customObjectType.getName());
					for (int i = 0; i < result.size(); i++)
					{
						Map<String, Object> row = result.get(i);
						String customTypeName = typeName + "_" + id.incrementAndGet();
						while (!PersistFinder.INSTANCE.checkName(editorPart, customTypeName))
						{
							customTypeName = typeName + "_" + id.incrementAndGet();
						}
						WebCustomType bean = AddContainerCommand.addCustomType(webComponent, property.getName(), customTypeName, i, null);
						row.forEach((key, value) -> bean.setProperty(key, value));
					}
				}
			}
			else
			{
				ServoyLog.logWarning("auto show wizard property " + property + " of custom type " + customObjectType +
					"\nhas no wizard properties\n" + propType, null);
			}
		}
		else
		{
			ServoyLog.logWarning("wizard:autoshow enabled for property " + property + " of component " + spec +
				" that is not an custom array type " + propType, null);
		}
	}

	private Point getLocationAndShiftSiblings(ISupportChilds parent, JSONObject args, List<IPersist> extraChangedPersists) throws RepositoryException
	{
		if ((editorPart.getForm().isResponsiveLayout() || parent instanceof CSSPositionLayoutContainer) && !CSSPositionUtils.isCSSPositionContainer(
			parent instanceof LayoutContainer ? (LayoutContainer)parent : null))
		{
			List<IPersist> children = new ArrayList<IPersist>();
			Iterator<IPersist> it = PersistHelper.getFlattenedPersist(ModelUtils.getEditingFlattenedSolution(editorPart.getForm()), editorPart.getForm(),
				parent).getAllObjects();
			while (it.hasNext())
			{
				IPersist persist = it.next();
				if (persist instanceof ISupportBounds)
				{
					children.add(persist);
				}
			}

			// default place it as the first element.
			int x = 1;
			int y = 1;
			if (children.size() > 0)
			{
				IPersist[] childArray = children.toArray(new IPersist[0]);
				Arrays.sort(childArray, PositionComparator.XY_PERSIST_COMPARATOR);
				if (args.has("rightSibling"))
				{
					IPersist rightSibling = PersistFinder.INSTANCE.searchForPersist(editorPart, args.optString("rightSibling", null));
					if (rightSibling == null && editorPart.getForm().getExtendsForm() != null)
					{

						Form f = editorPart.getForm();
						do
						{
							f = f.getExtendsForm();
							rightSibling = AbstractRepository.searchPersist(f, UUID.fromString(args.optString("rightSibling", null)));
						}
						while (f.getExtendsForm() != null);
						if (rightSibling != null)
						{
							rightSibling = ElementUtil.getOverridePersist(PersistContext.create(rightSibling, editorPart.getForm()));
						}
					}
					if (rightSibling != null)
					{
						int counter = 1;
						for (IPersist element : childArray)
						{
							if (element.getUUID().equals(rightSibling.getUUID()))
							{
								x = counter;
								y = counter;
								// i don't think we need this, because we are doing the increment at line 940
								counter++;
							}
							((ISupportBounds)element).setLocation(new Point(counter, counter));
							if (extraChangedPersists != null && element.isChanged()) extraChangedPersists.add(element);
							counter++;
						}
					}
					else
					{
						Debug.log("Could not find rightsibling with uuid '" + args.optString("rightSibling", null) + "', inserting on last position.");
						Point location = ((ISupportBounds)childArray[childArray.length - 1]).getLocation();
						x = location.x + 1;
						y = location.y + 1;
					}
				}
				else
				{
					// insert as last element in flow layout because no right/bottom sibling was given
					Point location = ((ISupportBounds)childArray[childArray.length - 1]).getLocation();
					x = location.x + 1;
					y = location.y + 1;
				}
			}
			return new Point(x, y);
		}
		return new Point(args.optInt("x", 1), args.optInt("y", 1));
	}

	protected ChildWebComponent createNestedWebComponent(WebComponent parentWebComponent, PropertyDescription pd, String componentSpecName, String propertyName,
		int indexIfInArray, int x, int y, int width, int height)
	{
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentSpecName);
		if (spec != null)
		{
			String compName = null;
			String componentName = spec.getDisplayName().replaceAll("\\s", "").toLowerCase();
			componentName = componentName.replaceAll("-", "_");
			compName = componentName + "_" + id.incrementAndGet();
			while (!PersistFinder.INSTANCE.checkName(editorPart, compName))
			{
				compName = componentName + "_" + id.incrementAndGet();
			}

			ChildWebComponent webComponent = ChildWebComponent.createNewInstance(parentWebComponent, pd, propertyName, indexIfInArray);
			webComponent.setTypeName(componentSpecName);

			// not sure if location and size are still needed to be set in children here... maybe it is (if parent wants to use them at runtime)
			int xRelativeToParent = Math.max(0, (int)(x - parentWebComponent.getLocation().getX()));
			int yRelativeToParent = Math.max(0, (int)(y - parentWebComponent.getLocation().getY()));
			webComponent.setLocation(new Point(xRelativeToParent, yRelativeToParent));
			webComponent.setSize(new Dimension(width, height));
			PropertyDescription description = spec.getProperty(StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName());
			if (description != null && description.getDefaultValue() instanceof JSONObject)
			{
				webComponent.setSize(new Dimension(((JSONObject)description.getDefaultValue()).optInt("width", 80),
					((JSONObject)description.getDefaultValue()).optInt("height", 80)));
			}
			parentWebComponent.insertChild(webComponent);

			return webComponent;
		}
		return null;
	}

	protected List<IPersist> createLayoutContainer(ISupportFormElements parent, WebLayoutSpecification layoutSpec, LayoutContainer sameTypeChildContainer,
		JSONObject config, Point location, PackageSpecification<WebLayoutSpecification> specifications, String packageName)
		throws RepositoryException, JSONException
	{
		List<IPersist> newPersists = new ArrayList<IPersist>();
		int type = parent.getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) == null && layoutSpec.getName().equals("servoycore-responsivecontainer")
			? IRepository.CSSPOS_LAYOUTCONTAINERS : IRepository.LAYOUTCONTAINERS;
		LayoutContainer container = (LayoutContainer)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(parent,
			type);
		container.setSpecName(layoutSpec.getName());
		container.setPackageName(packageName);
		parent.addChild(container);
		if (container instanceof CSSPositionLayoutContainer)
		{
			((CSSPositionLayoutContainer)container)
				.setCssPosition(new CSSPosition(Integer.toString(location.y), "-1", "-1", Integer.toString(location.x), "200", "200"));
		}
		else
		{
			container.setLocation(new Point(location.x, location.x));
			if (CSSPositionUtils.isCSSPositionContainer(layoutSpec)) container.setSize(new Dimension(200, 200));
		}
		newPersists.add(container);
		if (config != null)
		{
			// if this is a composite try to set the actual layoutname (so a row combination with columns becomes here just a row)
			String layoutName = config.optString("layoutName", null);
			if (layoutName != null) container.setSpecName(layoutName);
			Iterator keys = config.keys();
			while (keys.hasNext())
			{
				String key = (String)keys.next();
				Object value = config.get(key);
				if ("children".equals(key))
				{
					// special key to create children instead of a attribute set.
					JSONArray array = (JSONArray)value;
					for (int i = 0; i < array.length(); i++)
					{
						JSONObject jsonObject = array.getJSONObject(i);
						if (jsonObject.has("layoutName"))
						{
							WebLayoutSpecification spec = specifications.getSpecification(jsonObject.getString("layoutName"));
							newPersists.addAll(
								createLayoutContainer(container, spec, null, jsonObject.optJSONObject("model"), new Point(i + 1, i + 1), specifications,
									packageName));
						}
						else if (jsonObject.has("componentName"))
						{
							String compName = "component_" + id.incrementAndGet();
							WebComponent component = container.createNewWebComponent(compName, jsonObject.getString("componentName"));
							newPersists.add(component);
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
								jsonObject.getString("componentName"));
							if (spec != null)
							{
								Collection<String> allPropertiesNames = spec.getAllPropertiesNames();
								for (String string : allPropertiesNames)
								{
									PropertyDescription property = spec.getProperty(string);
									if (property != null && property.getInitialValue() != null)
									{
										Object initialValue = property.getInitialValue();
										if (initialValue != null) component.setProperty(string, initialValue);
									}
								}
							}
						}
					}
				} // children and layoutName are special
				else if (!"layoutName".equals(key))
				{
					container.putAttribute(key, sameTypeChildContainer != null ? sameTypeChildContainer.getAttribute(key) : value.toString());
				}
			}
		}
		return newPersists;
	}

}
