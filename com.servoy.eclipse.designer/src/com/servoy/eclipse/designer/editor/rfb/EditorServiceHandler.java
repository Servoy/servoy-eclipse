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

package com.servoy.eclipse.designer.editor.rfb;

import java.awt.Dimension;
import java.awt.Point;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.gef.EditPart;
import org.eclipse.gef.EditPolicy;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.SelectionAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.internal.WorkbenchPage;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.TypesRegistry;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.actions.DistributeRequest.Distribution;
import com.servoy.eclipse.designer.actions.ZOrderAction;
import com.servoy.eclipse.designer.actions.ZOrderAction.OrderableElement;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.FormEditPolicy;
import com.servoy.eclipse.designer.editor.FormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.FormXYLayoutPolicy;
import com.servoy.eclipse.designer.editor.commands.AddAccordionPaneAction;
import com.servoy.eclipse.designer.editor.commands.AddFieldAction;
import com.servoy.eclipse.designer.editor.commands.AddMediaAction;
import com.servoy.eclipse.designer.editor.commands.AddPortalAction;
import com.servoy.eclipse.designer.editor.commands.AddSplitpaneAction;
import com.servoy.eclipse.designer.editor.commands.AddTabpanelAction;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.designer.editor.commands.SaveAsTemplateAction;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.server.ngclient.template.PartWrapper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Handle requests from the rfb html editor.
 *
 * @author rgansevles
 *
 */
public class EditorServiceHandler implements IServerService
{
	private final class SetPropertyCommand extends BaseRestorableCommand
	{
		private final Object value;
		private final PersistPropertySource source;
		private final String propertyName;

		/**
		 * @param label
		 * @param newLocation
		 * @param persist
		 */
		private SetPropertyCommand(String label, PersistPropertySource source, String propertyName, Object value)
		{
			super(label);
			this.source = source;
			this.propertyName = propertyName;
			this.value = value;
		}

		@Override
		public void execute()
		{
			setPropertyValue(source, propertyName, value);
		}
	}

	private final BaseVisualFormEditor editorPart;
	private final ISelectionProvider selectionProvider;
	private final AtomicInteger id = new AtomicInteger();
	private final RfbSelectionListener selectionListener;
	private final OpenElementWizard openElementWizard;
	private final IFieldPositioner fieldPositioner;

	public EditorServiceHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider, RfbSelectionListener selectionListener,
		IFieldPositioner fieldPositioner)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
		this.selectionListener = selectionListener;
		this.fieldPositioner = fieldPositioner;
		openElementWizard = new OpenElementWizard();
	}


	/**
	 * @param uuid
	 * @return
	 */
	private IPersist searchForPersist(String uuid)
	{
		String searchFor = uuid;
		if (searchFor.contains("_"))
		{
			String[] split = searchFor.split("_");
			if (split.length != 3) return null;
			String parentUUID = split[0];
			String fieldName = split[1];
			String typeName = split[2];
			int index = -1;
			Bean parentBean = (Bean)ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(parentUUID));
			WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(parentBean.getBeanClassName());
			if (fieldName.indexOf('[') > 0)
			{
				index = extractIndex(fieldName);
				fieldName = fieldName.substring(0, fieldName.indexOf('['));
			}
			boolean arrayReturnType = spec.isArrayReturnType(fieldName);

			Bean bean = new GhostBean(parentBean, fieldName, typeName, index, arrayReturnType, false);
			String compName = "bean_" + id.incrementAndGet();
			while (!checkName(compName))
			{
				compName = "bean_" + id.incrementAndGet();
			}
			bean.setName(compName);
			bean.setBeanClassName(typeName);
			return bean;
		}
		return ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(searchFor));
	}

	@Override
	public Object executeMethod(final String methodName, final JSONObject args)
	{
		try
		{
			if ("getGhostComponents".equals(methodName))
			{
				StringWriter stringWriter = new StringWriter();
				final JSONWriter writer = new JSONWriter(stringWriter);
				writer.object();
				writer.key("ghostContainers");
				writer.array();
				editorPart.getForm().acceptVisitor(new IPersistVisitor()
				{

					private String computeGhostUUID(Bean bean, PropertyDescription pd, String simpleTypeName, int index)
					{
						if (index < 0) return bean.getUUID() + "_" + pd.getName() + "_" + simpleTypeName;
						return bean.getUUID() + "_" + pd.getName() + "[" + index + "]" + "_" + simpleTypeName;
					}

					private void writeGhostsForWebcomponentBeans(JSONWriter writer, Bean bean)
					{
						if (FormTemplateGenerator.isWebcomponentBean(bean))
						{
							WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(bean.getBeanClassName());
							Map<String, PropertyDescription> properties = spec.getProperties();
							try
							{
								writer.object();
								writer.key("style");
								{
									writer.object();
									writer.key("left").value(bean.getLocation().getX());
									writer.key("top").value(bean.getLocation().getY());
									writer.endObject();
								}
								writer.key("uuid").value(bean.getUUID());
								writer.key("ghosts");
								{
									writer.array();
									for (PropertyDescription pd : properties.values())
									{
										String simpleTypeName = pd.getType().getName().replaceFirst(spec.getName() + ".", "");
										if (spec.getFoundTypes().containsKey(simpleTypeName))
										{
											try
											{
												if (bean.getBeanXML() != null)
												{
													JSONObject webComponentModelJson = new JSONObject(bean.getBeanXML());
													if (webComponentModelJson.has(pd.getName()))
													{
														if (webComponentModelJson.get(pd.getName()).toString().startsWith("["))
														{
															JSONArray jsonArray = webComponentModelJson.getJSONArray(pd.getName());
															for (int i = 0; i < jsonArray.length(); i++)
															{
																writeGhostToJSON(writer, bean, pd, simpleTypeName, i);
															}
														}
														else
														{
															writeGhostToJSON(writer, bean, pd, simpleTypeName, -1);// -1 does not add a [0] at the end of the name
														}
													}
												}
											}
											catch (JSONException e)
											{
												Debug.error(e);
											}
										}
									}
									writer.endArray();
								}
								writer.endObject();
							}
							catch (JSONException e1)
							{
								Debug.error(e1);
							}
						}
					}

					/**
					 * @param jsonWriter
					 * @param bean
					 * @param pd
					 * @param simpleTypeName
					 * @param i
					 * @throws JSONException
					 */
					private void writeGhostToJSON(JSONWriter jsonWriter, Bean bean, PropertyDescription pd, String simpleTypeName, int i) throws JSONException
					{
						jsonWriter.object();
						jsonWriter.key("uuid").value(computeGhostUUID(bean, pd, simpleTypeName, i));
						jsonWriter.key("type").value(pd.getType().getName());
						jsonWriter.key("text").value(pd.getName());
						jsonWriter.key("location");
						{
							jsonWriter.object();
							int position = i;
							if (position < 0) position = 0;
							jsonWriter.key("x").value(computeX(position));
							jsonWriter.key("y").value(computeY(position));
							jsonWriter.endObject();
						}
						jsonWriter.key("size");
						jsonWriter.object();
						jsonWriter.key("width").value(bean.getSize().width);
						jsonWriter.key("height").value(bean.getSize().height);
						jsonWriter.endObject();
						jsonWriter.endObject();
					}

					private int computeX(int index)
					{
						return 15 + index * 40;
					}

					private int computeY(int index)
					{
						return 50;
					}

					@Override
					public Object visit(IPersist o)
					{
						if (o instanceof TabPanel)
						{
							try
							{
								writer.object();
								writer.key("style");
								TabPanel panel = (TabPanel)o;
								{
									writer.object();
									writer.key("left").value(panel.getLocation().getX());
									writer.key("top").value(panel.getLocation().getY());
									writer.endObject();
								}
								writer.key("uuid").value(panel.getUUID());
								writer.key("ghosts");
								{
									writer.array();
									Iterator<IPersist> tabIterator = panel.getTabs();
									while (tabIterator.hasNext())
									{
										Tab tab = (Tab)tabIterator.next();
										writer.object();
										writer.key("uuid").value(tab.getUUID());
										writer.key("type").value(tab.getTypeID());
										writer.key("text").value(tab.getText());
										writer.key("location");
										writer.object();
										writer.key("x").value(tab.getLocation().x);
										writer.key("y").value(tab.getLocation().y);
										writer.endObject();
										writer.key("size");
										writer.object();
										writer.key("width").value(tab.getSize().width);
										writer.key("height").value(tab.getSize().height);
										writer.endObject();
										writer.endObject();
									}
									writer.endArray();
								}
								writer.endObject();
							}
							catch (IllegalArgumentException e)
							{
								Debug.error(e);
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}
						}
						else if (o instanceof Form)
						{
							try
							{
								writer.object();
								writer.key("uuid").value(o.getUUID());
								writer.key("ghosts");
								writer.array();
								Iterator<Part> partIterator = ((Form)o).getParts();
								ArrayList<Part> parts = new ArrayList<Part>();
								while (partIterator.hasNext())
									parts.add(partIterator.next());

								for (int i = 0; i < parts.size(); i++)
								{
									writer.object();
									writer.key("uuid").value(parts.get(i).getUUID());
									writer.key("type").value(parts.get(i).getTypeID());
									writer.key("text").value(Part.getDisplayName(parts.get(i).getPartType()));
									writer.key("location");
									writer.object();
									writer.key("x").value(0);
									writer.key("y").value(parts.get(i).getSize().getHeight());
									writer.endObject();
									writer.key("parttype").value(parts.get(i).getPartType());
									if (i > 0) writer.key("partprev").value(parts.get(i - 1).getUUID());
									if (i < parts.size() - 1) writer.key("partnext").value(parts.get(i + 1).getUUID());
									writer.endObject();
								}
								writer.endArray();
								writer.endObject();
							}
							catch (IllegalArgumentException e)
							{
								Debug.error(e);
							}
							catch (JSONException e)
							{
								Debug.error(e);
							}
						}
						else if (o instanceof Bean)
						{
							writeGhostsForWebcomponentBeans(writer, (Bean)o);
						}

						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				});
				writer.endArray();
				writer.endObject();
				return new JSONObject(stringWriter.getBuffer().toString());
			}

//			if ("getFormLayoutGrid".equals(methodName))
//			{
//				return editorPart.getForm().getLayoutGrid();
//			}

			// void methods
			if ("setSelection".equals(methodName))
			{
				JSONArray json = args.getJSONArray("selection");
				final List<Object> selection = new ArrayList<Object>();
				for (int i = 0; i < json.length(); i++)
				{
					IPersist searchPersist = searchForPersist(json.getString(i));
					if (searchPersist != null) selection.add(searchPersist);
				}
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						IStructuredSelection structuredSelection = new StructuredSelection(selection);
						selectionListener.setLastSelection(structuredSelection);
						selectionProvider.setSelection(selection.size() == 0 ? null : structuredSelection);
					}
				});
			}
			else if ("setTabSequence".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						IPersist[] selection = (IPersist[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new IPersist[0]);
						if (selection.length > 0)
						{
							int tabIndex = 1;
							CompoundCommand cc = new CompoundCommand();
							for (IPersist persist : selection)
							{
								if (persist instanceof Bean)
								{
									WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
										((Bean)persist).getBeanClassName());
									Map<String, PropertyDescription> tabSeqProps = spec.getProperties(TypesRegistry.getType("tabseq"));
									for (PropertyDescription pd : tabSeqProps.values())
									{
										cc.add(new SetPropertyCommand("tabSeq", PersistPropertySource.createPersistPropertySource(persist, false),
											pd.getName(), Integer.valueOf(tabIndex)));
										tabIndex++;
									}
								}
								else
								{
									cc.add(new SetPropertyCommand("tabSeq", PersistPropertySource.createPersistPropertySource(persist, false),
										StaticContentSpecLoader.PROPERTY_TABSEQ.getPropertyName(), Integer.valueOf(tabIndex)));
									tabIndex++;
								}
							}
							editorPart.getCommandStack().execute(cc);
						}
					}
				});
			}
			else if ("z_order_bring_to_front_one_step".equals(methodName) || "z_order_send_to_back_one_step".equals(methodName) ||
				"z_order_bring_to_front".equals(methodName) || "z_order_send_to_back".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						List selection = ((IStructuredSelection)selectionProvider.getSelection()).toList();
						if (selection.size() > 0)
						{
							List<OrderableElement> elements = ZOrderAction.calculateNewZOrder(editorPart.getForm(), selection, methodName);
							CompoundCommand cc = new CompoundCommand();
							for (OrderableElement element : elements)
							{
								cc.add(new SetPropertyCommand("zindex", PersistPropertySource.createPersistPropertySource(element.getFormElement(), false),
									StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName(), element.zIndex));
							}
							editorPart.getCommandStack().execute(cc);
							ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, new ArrayList<IPersist>(selection));
						}
					}
				});
			}
			else if ("horizontal_spacing".equals(methodName) || "vertical_spacing".equals(methodName) || "horizontal_centers".equals(methodName) ||
				"vertical_centers".equals(methodName) || "horizontal_pack".equals(methodName) || "vertical_pack".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						IPersist[] selection = (IPersist[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new IPersist[0]);
						if (selection.length > 0)
						{
							List<ISupportBounds> elements = new ArrayList<ISupportBounds>();
							for (IPersist persist : selection)
							{
								if (persist instanceof ISupportBounds)
								{
									elements.add((ISupportBounds)persist);
								}
								else
								{
									Debug.error("Unexpected selection element for distribution:" + persist);
									return;
								}
							}
							List<Point> deltas = FormXYLayoutPolicy.getDistributeChildrenDeltas(elements, Distribution.valueOf(methodName.toUpperCase()));
							CompoundCommand cc = new CompoundCommand();
							for (int i = 0; i < selection.length; i++)
							{
								Point newLocation = new Point(((ISupportBounds)selection[i]).getLocation());
								newLocation.x += deltas.get(i).x;
								newLocation.y += deltas.get(i).y;
								cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource(selection[i], false),
									StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), newLocation));
							}
							editorPart.getCommandStack().execute(cc);
							ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, new ArrayList<IPersist>((List)elements));
						}
					}
				});
			}
			else if ("keyPressed".equals(methodName))
			{
				int keyCode = args.optInt("keyCode");
				boolean isCtrl = args.optBoolean("ctrl");
				boolean isShift = args.optBoolean("shift");

				switch (keyCode)
				{
					case 46 : // delete
						Display.getDefault().asyncExec(new Runnable()
						{
							public void run()
							{
								IPersist[] selection = (IPersist[])((IStructuredSelection)selectionProvider.getSelection()).toList().toArray(new IPersist[0]);
								if (selection.length > 0)
								{
									editorPart.getCommandStack().execute(new FormElementDeleteCommand(selection));
								}
							}
						});
						break;
					case 83 : // s
						if (isCtrl && isShift) // ctrl+shift+s (save all)
						{
							Display.getDefault().asyncExec(new Runnable()
							{
								public void run()
								{
									IWorkbenchPage page = editorPart.getSite().getPage();
									((WorkbenchPage)page).saveAllEditors(false, false, true);
								}
							});
						}
						break;
				}
			}
			else if ("setProperties".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						Iterator keys = args.keys();
						while (keys.hasNext())
						{
							CompoundCommand cc = null;
							String uuid = (String)keys.next();
							final IPersist persist = searchForPersist(uuid);
							if ((persist instanceof BaseComponent || persist instanceof Tab) && !(persist instanceof GhostBean))
							{
								JSONObject properties = args.optJSONObject(uuid);
								cc = new CompoundCommand();
								if (properties.has("x") && properties.has("y"))
								{
									cc.add(new SetPropertyCommand("move", PersistPropertySource.createPersistPropertySource(persist, false),
										StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName(), new Point(properties.optInt("x"), properties.optInt("y"))));
								}
								if (properties.has("width") && properties.has("height"))
								{
									cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(persist, false),
										StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName(), new Dimension(properties.optInt("width"),
											properties.optInt("height"))));
								}
								if (properties.has("anchors"))
								{
									cc.add(new SetPropertyCommand("anchor", PersistPropertySource.createPersistPropertySource(persist, false),
										StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName(), properties.optInt("anchors")));
								}
								editorPart.getCommandStack().execute(cc);
							}
							else if (persist instanceof Part)
							{
								JSONObject properties = args.optJSONObject(uuid);
								cc = new CompoundCommand();
								if (properties.has("y"))
								{
									cc.add(new SetPropertyCommand("resize", PersistPropertySource.createPersistPropertySource(persist, false),
										StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName(), new Integer(properties.optInt("y"))));
								}
							}
							if (cc != null) editorPart.getCommandStack().execute(cc);
						}
					}
				});
			}
			else if ("createComponent".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						editorPart.getCommandStack().execute(new BaseRestorableCommand("createComponent")
						{
							private IPersist newPersist;

							@Override
							public void execute()
							{
								try
								{
									newPersist = createComponent(args);
									if (newPersist != null)
									{
										ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
											Arrays.asList(new IPersist[] { newPersist }));
										Object[] selection = new Object[] { newPersist };
										IStructuredSelection structuredSelection = new StructuredSelection(selection);
										selectionProvider.setSelection(structuredSelection);
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
										((IDeveloperRepository)newPersist.getRootObject().getRepository()).deleteObject(newPersist);
										ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
											Arrays.asList(new IPersist[] { newPersist }));
									}
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError("Could not undo create elements", e);
								}
							}

						});
					}
				});
			}
			else if ("getPartsStyles".equals(methodName))
			{
				StringWriter stringWriter = new StringWriter();
				final JSONWriter writer = new JSONWriter(stringWriter);

				writer.array();

				Iterator<Part> partsIte = editorPart.getForm().getParts();
				while (partsIte.hasNext())
				{
					PartWrapper partWrapper = new PartWrapper(partsIte.next(), editorPart.getForm(), null);
					writer.object();
					writer.key("name").value(partWrapper.getName());
					writer.key("style").value(new JSONObject(partWrapper.getStyle()));
					writer.endObject();
				}

				writer.endArray();

				return new JSONArray(stringWriter.getBuffer().toString());
			}
			else if ("createComponents".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						editorPart.getCommandStack().execute(new BaseRestorableCommand("createComponents")
						{
							private List<IPersist> newPersists;

							@Override
							public void execute()
							{
								try
								{
									if (args.has("components"))
									{
										JSONArray components = args.getJSONArray("components");
										newPersists = new ArrayList<IPersist>();
										for (int i = 0; i < components.length(); i++)
										{
											IPersist persist = createComponent(components.getJSONObject(i));
											if (persist != null)
											{
												newPersists.add(persist);
											}
											else
											{
												Debug.error("Could not create the component " + components.getJSONObject(i).toString());
											}
										}
									}
									if (newPersists != null)
									{
										ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, newPersists);
										IStructuredSelection structuredSelection = new StructuredSelection(newPersists);
										selectionProvider.setSelection(structuredSelection);
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
									if (newPersists != null)
									{
										for (IPersist persist : newPersists)
										{
											((IDeveloperRepository)persist.getRootObject().getRepository()).deleteObject(persist);
										}
										ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, newPersists);
									}
								}
								catch (RepositoryException e)
								{
									ServoyLog.logError("Could not undo create elements", e);
								}
							}

						});
					}
				});
			}
			else if ("openElementWizard".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						openElementWizard.run(args.optString("elementType"));
					}
				});
			}
			else if ("updateFieldPositioner".equals(methodName))
			{
				JSONObject location = args.optJSONObject("location");
				fieldPositioner.setDefaultLocation(new org.eclipse.swt.graphics.Point(location.optInt("x"), location.optInt("y")));
			}
			else if ("openScript".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						EditorUtil.openScriptEditor(editorPart.getForm(), null, true);
					}
				});
			}
			else if ("openContainedForm".equals(methodName))
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						if (args.has("uuid"))
						{
							try
							{
								IPersist persist = searchForPersist(args.getString("uuid"));
								Solution s = (Solution)editorPart.getForm().getParent();
								boolean open = false;
								if (persist != null)
								{

									if (persist instanceof Tab)
									{
										open = openFormDesignEditor(s, ((Tab)persist).getContainsFormID());
										Debug.log("Cannot open form with id " + ((Tab)persist).getContainsFormID() + "in design editor (Tab uuid " +
											args.getString("uuid") + ")");
									}
									else if (persist instanceof GhostBean)
									{
										GhostBean ghost = (GhostBean)persist;
										JSONObject beanXML = new ServoyJSONObject(ghost.getBeanXML(), false);


										WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
											ghost.getParentBean().getBeanClassName());
										if (spec != null)
										{
											Map<String, PropertyDescription> forms = null;
											IPropertyType< ? > iPropertyType = spec.getFoundTypes().get(ghost.getTypeName());
											if (iPropertyType instanceof ICustomType)
											{
												forms = ((ICustomType< ? >)iPropertyType).getCustomJSONTypeDefinition().getProperties(FormPropertyType.INSTANCE);
											}
											else
											{
												Debug.warn("Unexpected propertyType " + iPropertyType.getName());
											}

											if (forms != null)
											{
												for (PropertyDescription pd : forms.values())
												{
													open = openFormDesignEditor(s, beanXML.opt(pd.getName()));
													if (!open)
													{
														Debug.log("Cannot open form with id " + beanXML.opt(pd.getName()) +
															"in design editor (Container uuid " + args.getString("uuid") + ")");
													}
												}
											}
										}
									}
									else
									{
										Bean bean = (Bean)persist;
										WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(
											((Bean)persist).getBeanClassName());
										if (spec != null)
										{
											Map<String, PropertyDescription> forms = spec.getProperties(FormPropertyType.INSTANCE);
											for (PropertyDescription pd : forms.values())
											{
												open = openFormDesignEditor(s, bean.getProperty(pd.getName()));
												if (!open)
												{
													Debug.log("Cannot open form with id " + bean.getProperty(pd.getName()) +
														"in design editor (container uuid " + args.getString("uuid") + ")");
												}
											}
										}
									}

								}
								else
								{
									Debug.log("Cannot open form in design editor. Container uuid " + args.getString("uuid") + " was not found");
								}
							}
							catch (JSONException ex)
							{
								Debug.error(ex);
							}
						}
					}

					/**
					 * @param s
					 * @param value
					 * @return
					 */
					private boolean openFormDesignEditor(Solution s, Object value)
					{
						if (value != null)
						{
							Form toOpen = s.getForm((Integer)value);
							if (toOpen != null)
							{
								return EditorUtil.openFormDesignEditor(toOpen) != null;
							}
						}
						return false;
					}
				});
			}

		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}

		return null;
	}

	private boolean isCorrectTarget(BaseComponent baseComponent, String uuid)
	{
		return baseComponent.getUUID().toString().equals(uuid);
	}

	/**
	 * @param compName
	 */
	private boolean checkName(String compName)
	{
		Iterator<IFormElement> fields = editorPart.getForm().getFormElementsSortedByFormIndex();
		for (IFormElement element : Utils.iterate(fields))
		{
			if (compName.equals(element.getName()))
			{
				return false;
			}
		}
		return true;
	}

	/**
	 * @param args
	 * @throws JSONException
	 * @throws RepositoryException
	 */
	private IPersist createComponent(final JSONObject args) throws JSONException, RepositoryException
	{
		int x = args.getInt("x");
		int y = args.getInt("y");
		if (args.has("type"))
		{//a ghost dragged from the pallete. it is defined in the "types" section of the .spec file
			Iterator<IPersist> allPersists = editorPart.getForm().getAllObjects();
			while (allPersists.hasNext())
			{
				IPersist next = allPersists.next();
				if (next instanceof BaseComponent)
				{
					if (isCorrectTarget(((BaseComponent)next), (String)args.get("dropTargetUUID")))
					{
						if (args.getString("type").equals("tab"))
						{
							if (next instanceof ISupportChilds)
							{
								ISupportChilds iSupportChilds = (ISupportChilds)next;
								Tab newTab = (Tab)editorPart.getForm().getRootObject().getChangeHandler().createNewObject(iSupportChilds, IRepository.TABS);
								String tabName = "tab_" + id.incrementAndGet();
								while (!checkName(tabName))
								{
									tabName = "tab_" + id.incrementAndGet();
								}
								newTab.setText(tabName);
								newTab.setLocation(new Point(x - ((BaseComponent)next).getLocation().x, y - ((BaseComponent)next).getLocation().y));
								iSupportChilds.addChild(newTab);
								return next;
							}
						}
						if (next instanceof Bean)
						{
							Bean parentBean = (Bean)next;
							String typeName = args.getString("type");
							String compName = "bean_" + id.incrementAndGet();
							while (!checkName(compName))
							{
								compName = "bean_" + id.incrementAndGet();
							}
							String dropTargetFieldName = getFirstFieldWithType(parentBean, typeName);
							int index = -1;
							boolean isArray = false;
							WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(parentBean.getBeanClassName());
							Object config = spec.getProperty(dropTargetFieldName).getConfig();
							JSONObject configObject = new JSONObject(config.toString());
							if (configObject.getString("type").endsWith("]")) isArray = true;
							Bean bean = new GhostBean(parentBean, dropTargetFieldName, typeName, index, isArray, true);
							bean.setName(compName);
							bean.setBeanClassName(typeName);
							return bean;
						}
					}
				}
			}
		}
		else if (args.has("name"))
		{
			String name = args.getString("name");
			if ("servoydefault-button".equals(name))
			{
				GraphicalComponent gc = editorPart.getForm().createNewGraphicalComponent(new Point(x, y));
				gc.setText("button");
				gc.setOnActionMethodID(-1);
				return gc;
			}
			else if ("servoydefault-label".equals(name))
			{
				GraphicalComponent gc = editorPart.getForm().createNewGraphicalComponent(new Point(x, y));
				gc.setText("label");
				return gc;
			}
			else if ("servoydefault-combobox".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.COMBOBOX);
				return field;
			}
			else if ("servoydefault-textfield".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.TEXT_FIELD);
				return field;
			}
			else if ("servoydefault-textarea".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.TEXT_AREA);
				return field;
			}
			else if ("servoydefault-password".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.PASSWORD);
				return field;
			}
			else if ("servoydefault-calendar".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.CALENDAR);
				return field;
			}
			else if ("servoydefault-typeahead".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.TYPE_AHEAD);
				return field;
			}
			else if ("servoydefault-spinner".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.SPINNER);
				return field;
			}
			else if ("servoydefault-check".equals(name) || "servoydefault-checkgroup".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.CHECKS);
				return field;
			}
			else if ("servoydefault-radio".equals(name) || "servoydefault-radiogroup".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.RADIOS);
				return field;
			}
			else if ("servoydefault-imagemedia".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.IMAGE_MEDIA);
				return field;
			}
			else if ("servoydefault-listbox".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.LIST_BOX);
				return field;
			}
			else if ("servoydefault-htmlarea".equals(name))
			{
				Field field = editorPart.getForm().createNewField(new Point(x, y));
				field.setDisplayType(Field.HTML_AREA);
				field.setEditable(false);
				return field;
			}
			else if ("servoydefault-tabpanel".equals(name))
			{
				String compName = "tabpanel_" + id.incrementAndGet();
				while (!checkName(compName))
				{
					compName = "tabpanel_" + id.incrementAndGet();
				}
				TabPanel tabPanel = editorPart.getForm().createNewTabPanel(compName);
				tabPanel.setLocation(new Point(x, y));
				return tabPanel;
			}
			else if ("servoydefault-splitpane".equals(name))
			{
				String compName = "tabpanel_" + id.incrementAndGet();
				while (!checkName(compName))
				{
					compName = "tabpanel_" + id.incrementAndGet();
				}
				TabPanel tabPanel = editorPart.getForm().createNewTabPanel(compName);
				tabPanel.setLocation(new Point(x, y));
				tabPanel.setTabOrientation(TabPanel.SPLIT_HORIZONTAL);
				return tabPanel;
			}
			else if ("servoydefault-portal".equals(name))
			{
				String compName = "portal_" + id.incrementAndGet();
				while (!checkName(compName))
				{
					compName = "portal_" + id.incrementAndGet();
				}
				Portal portal = editorPart.getForm().createNewPortal(compName, new Point(x, y));
				return portal;
			}
			else if ("servoydefault-rectangle".equals(name))
			{
				RectShape shape = editorPart.getForm().createNewRectangle(new Point(x, y));
				shape.setLineSize(1);
				return shape;
			}
			else
			{
				// bean
				String compName = "bean_" + id.incrementAndGet();
				while (!checkName(compName))
				{
					compName = "bean_" + id.incrementAndGet();
				}
				Bean bean = editorPart.getForm().createNewBean(compName, name);
				bean.setLocation(new Point(x, y));
				return bean;
			}
		}
		else if (args.has("uuid"))
		{
			IPersist persist = editorPart.getForm().getChild(UUID.fromString(args.getString("uuid")));
			if (persist instanceof AbstractBase)
			{
				IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
				IPersist newPersist = ((AbstractBase)persist).cloneObj(persist.getParent(), true, validator, true, true, true);
				((ISupportBounds)newPersist).setLocation(new Point(x, y));
				return newPersist;
			}
		}
		return null;
	}

	/**
	 * @param dropTargetFieldName
	 * @return
	 */
	private int extractIndex(String dropTargetFieldName)
	{
		int index = -1;
		if (dropTargetFieldName.indexOf('[') > 0)
		{
			index = Integer.parseInt(dropTargetFieldName.substring(dropTargetFieldName.indexOf('[') + 1, dropTargetFieldName.indexOf(']')));
		}
		return index;
	}

	/**
	 * @param parentBean
	 * @param typeName
	 * @return the first key name in the model that has a value of type @param typeName
	 */
	private String getFirstFieldWithType(Bean parentBean, String typeName)
	{
		WebComponentSpecification spec = WebComponentSpecProvider.getInstance().getWebComponentSpecification(parentBean.getBeanClassName());
		Map<String, PropertyDescription> properties = spec.getProperties();
		for (PropertyDescription pd : properties.values())
		{
			if (pd.getType().getName().replaceFirst(spec.getName() + ".", "").equals(typeName))
			{
				return pd.getName();
			}
		}
		return null;
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
}
