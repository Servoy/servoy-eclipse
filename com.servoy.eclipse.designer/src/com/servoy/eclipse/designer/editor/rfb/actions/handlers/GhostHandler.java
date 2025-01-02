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

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONArrayType;
import org.sablo.websocket.IServerService;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.rfb.startup.RFBDesignerUtils;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.util.DeveloperUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.property.ComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * @author gganea@servoy.com
 *
 */
public class GhostHandler implements IServerService
{
	public static final String GHOST_TYPE_CONFIGURATION = "config";
	public static final String GHOST_TYPE_COMPONENT = "comp";
	public static final String GHOST_TYPE_PART = "part";
	public static final String GHOST_TYPE_FORM = "form";
	public static final String GHOST_TYPE_GROUP = "group";

	private final BaseVisualFormEditor editorPart;

	public GhostHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	public Object executeMethod(String methodName, final JSONObject args) throws JSONException
	{
		StringWriter stringWriter = new StringWriter();
		final JSONWriter writer = new JSONWriter(stringWriter);
		writer.object();
		writer.key("ghostContainers");
		writer.array();

		List<IFormElement> outsideElements = new ArrayList<IFormElement>();
		Map<String, FormElementGroup> groups = new HashMap<>();
		Form form = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm());
		int formHeight = form.getParts().hasNext() ? form.getSize().height : 0;
		Iterator<IPersist> it = form.getAllObjects();
		while (it.hasNext())
		{
			IPersist persist = it.next();
			if (persist instanceof IFormElement && !PersistHelper.isOverrideOrphanElement((IFormElement)persist))
			{
				IFormElement fe = (IFormElement)persist;
				Point location = CSSPositionUtils.getLocation(fe);
				if (!editorPart.getForm().isResponsiveLayout() && ((location.x > editorPart.getForm().getWidth()) || (location.y > formHeight)))
				{
					outsideElements.add(fe);
				}
				if (fe.getGroupID() != null)
				{
					String groupID = fe.getGroupID();
					if (!groups.containsKey(groupID))
						groups.put(groupID, new FormElementGroup(groupID, ModelUtils.getEditingFlattenedSolution(editorPart.getForm()), editorPart.getForm()));
				}
			}
		}
		if (outsideElements.size() > 0 || groups.size() > 0)
		{
			writer.object();
			writer.key("style");
			{
				writer.object();
				writer.key("left").value(0);
				writer.key("top").value(0);
				writer.endObject();
			}
			writer.key("ghosts");
			writer.array();
			printGhostFormElements(writer, outsideElements.iterator(), GHOST_TYPE_COMPONENT);

			for (FormElementGroup group : groups.values())
			{
				writer.object();
				writer.key("uuid").value(group.getGroupID());
				writer.key("type").value(GHOST_TYPE_GROUP);
				Rectangle rectangle = group.getBounds();
				writer.key("location");
				writer.object();
				writer.key("x").value(rectangle.getX());
				writer.key("y").value(rectangle.getY());
				writer.endObject();
				writer.key("size");
				writer.object();
				writer.key("width").value(rectangle.getWidth());
				writer.key("height").value(rectangle.getHeight());
				writer.endObject();
				writer.endObject();
			}

			writer.endArray();
			writer.endObject();
		}

		ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).getFlattenedForm(editorPart.getForm()).acceptVisitor(new IPersistVisitor()
		{
			private Point getGhostContainerLocation(ISupportBounds bean, ArrayList<IBasicWebComponent> parentFormComponentPath)
			{
				Point location = CSSPositionUtils.getLocation(bean);
				if (parentFormComponentPath != null)
				{
					for (IBasicWebComponent parentFormComponent : parentFormComponentPath)
					{
						location.setLocation(location.getX() + CSSPositionUtils.getLocation(parentFormComponent).getX(),
							location.getY() + CSSPositionUtils.getLocation(parentFormComponent).getY());
					}
				}
				return location;
			}

			private void writeGhostsForWebcomponentBeans(IBasicWebComponent basicWebComponent, String parentID,
				ArrayList<IBasicWebComponent> parentFormComponentPath)
			{
				if (FormTemplateGenerator.isWebcomponentBean(basicWebComponent))
				{
					SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();
					WebObjectSpecification spec = componentsSpecProviderState.getWebObjectSpecification(basicWebComponent.getTypeName());
					if (spec == null)
					{
						//error bean
						spec = componentsSpecProviderState.getWebObjectSpecification(FormElement.ERROR_BEAN);
					}
					Map<String, PropertyDescription> properties = spec.getProperties();
					if (basicWebComponent instanceof WebComponent && !FormElement.ERROR_BEAN.equals(spec.getName())) // could be legacy Bean (was used in alphas/betas) - that is unlikely though
					{
						for (String key : properties.keySet())
						{
							if (((WebObjectImpl)((WebComponent)basicWebComponent).getImplementation()).getPropertyDescription().isArrayReturnType(key))
							{
								LocationCache.getINSTANCE().clearParent(basicWebComponent.getUUID() + key);
							}
						}
					}

					//is inherited but not overridden
					boolean inherited = Utils.isInheritedFormElement(basicWebComponent, editorPart.getForm()) &&
						editorPart.getForm().getChild(basicWebComponent.getUUID()) == null;
					if (basicWebComponent instanceof Bean && ((Bean)basicWebComponent).getBeanXML() != null)
					{
						// these things (Bean instead of WebComponent) are deprecated right?
						startNewGhostContainer(writer, basicWebComponent, 0, 1, "", true, parentFormComponentPath, inherited);

						try
						{
							JSONObject webComponentModelJson = new JSONObject(((Bean)basicWebComponent).getBeanXML());
							for (PropertyDescription pd : properties.values())
							{
								String simpleTypeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(pd.getType());
								if (webComponentModelJson.has(pd.getName()))
								{
									Object configObject = pd.getConfig();
									if (PropertyUtils.isCustomJSONObjectProperty(pd.getType()))
									{
										if (pd.getType() instanceof ComponentPropertyType ||
											(configObject instanceof JSONObject && Boolean.TRUE.equals(((JSONObject)configObject).opt(FormElement.DROPPABLE))))
										{
											writeGhostToJSON(basicWebComponent.getUUID() + pd.getName(), pd, simpleTypeName, -1, inherited);// -1 does not add a [0] at the end of the name
										}
									}
									else if (PropertyUtils.isCustomJSONArrayPropertyType(pd.getType()))
									{
										JSONArray jsonArray = webComponentModelJson.getJSONArray(pd.getName());
										for (int i = 0; i < jsonArray.length(); i++)
										{
											if (((CustomJSONArrayType)pd.getType()).getCustomJSONTypeDefinition().getType() instanceof ComponentPropertyType ||
												(configObject instanceof JSONObject &&
													Boolean.TRUE.equals(((JSONObject)configObject).opt(FormElement.DROPPABLE))))
											{
												writeGhostToJSON(basicWebComponent.getUUID() + pd.getName(), writer,
													pd.getName() + (i >= 0 ? "[" + i + "]" : ""), UUID.randomUUID().toString(), i, simpleTypeName, inherited);
											}
										}
									}
								}
							}
						}
						catch (JSONException e)
						{
							Debug.error(e);
						}
						endGhostContainer(writer);
					}
					else if (basicWebComponent instanceof WebComponent)
					{
						SortedMap<String, Object> sortedDroppableProperties = filterAndSortDroppableProperties((WebComponent)basicWebComponent);

						int droppablePropCount = sortedDroppableProperties.size(), i = 0;

						for (Entry<String, Object> dropPropEntry : sortedDroppableProperties.entrySet())
						{
							PropertyDescription propertyPD = spec.getProperty(dropPropEntry.getKey());

							startNewGhostContainer(writer, basicWebComponent, i++, droppablePropCount, dropPropEntry.getKey(),
								PropertyUtils.isCustomJSONArrayPropertyType(propertyPD.getType()), parentFormComponentPath, inherited);

							if (dropPropEntry.getValue() != null)
							{
								// the value is either an IChildWebObject or an array of IChildWebObjects; we have to send all of them anyway
								IChildWebObject[] ghostsOfThisProp = (IChildWebObject[])(dropPropEntry.getValue() instanceof IChildWebObject[]
									? dropPropEntry.getValue() : new IChildWebObject[] { (IChildWebObject)dropPropEntry.getValue() });

								for (IChildWebObject ghostWebObject : ghostsOfThisProp)
								{
									if (ghostWebObject != null)
									{
										String ghostCaptionText = null;

										if (ghostWebObject instanceof WebCustomType)
										{
											ghostCaptionText = DeveloperUtils.getCustomObjectTypeCaptionFromTaggedSubproperties((WebCustomType)ghostWebObject);
										}

										if (ghostCaptionText == null)
										{
											ghostCaptionText = dropPropEntry.getKey() +
												(ghostWebObject.getIndex() >= 0 ? "[" + ghostWebObject.getIndex() + "]" : "");

											// special case for tabPanels - text subproperty should be shown as label instead of tabs[0]...
											if (ghostWebObject instanceof WebCustomType && ghostWebObject.hasProperty("text"))
											{
												Object val = ghostWebObject.getProperty("text");
												if (val instanceof String && ((String)val).trim().length() > 0) ghostCaptionText = (String)val;
											}
										}

										try
										{
											String parentKey = parentID != null ? parentID + ghostWebObject.getJsonKey()
												: basicWebComponent.getUUID() + ghostWebObject.getJsonKey();
											String ghostID = parentID != null ? parentID + "#" + ghostWebObject.getUUID() : ghostWebObject.getUUID().toString();
											writeGhostToJSON(parentKey, writer, ghostCaptionText, ghostID, ghostWebObject.getIndex(),
												ghostWebObject.getTypeName(), inherited);
										}
										catch (JSONException e1)
										{
											Debug.error(e1);
										}
									}
								}
							}

							endGhostContainer(writer);
						}
					}
				}
			}

			/**
			 * Creates a sorted map out of all droppable properties for this web component. These will be used to generate a ghost container for each such property of the web component.
			 *
			 * @return a map with root prop. name and value being either a IChildWebObject or an array of IChildWebObject.
			 */
			private SortedMap<String, Object> filterAndSortDroppableProperties(WebComponent webComponent)
			{
//				Iterator<Entry<String, Object>> childPersistMappedPropertiesIterator = webComponent.getPersistMappedPropertiesReadOnly().entrySet().iterator();
				TreeMap<String, Object> sortedAndDroppableProps = new TreeMap<>();

				PropertyDescription spec = ((WebObjectImpl)webComponent.getImplementation()).getPropertyDescription();

				if (spec != null) // can be null if the developer introduced a syntax error for example in the spec file while editing it
				{
					for (Entry<String, PropertyDescription> propEntry : spec.getProperties().entrySet())
					{
						if (isDroppable(propEntry.getValue(), propEntry.getValue().getConfig()))
						{
							Object value = webComponent.getProperty(propEntry.getKey());
							if (propEntry.getValue().getDeprecated() == null || value != null)
							{
								sortedAndDroppableProps.put(propEntry.getKey(), value);
							}
						}
					}
				}

				return sortedAndDroppableProps;
			}

			private void startNewGhostContainer(final JSONWriter writer, IBasicWebComponent basicWebComponent, int propYCount, int totalGhostContainersOfComp,
				String propertyName, boolean isArray, ArrayList<IBasicWebComponent> parentFormComponentPath, boolean inherited)
			{
				writer.object();
				writer.key("parentCompBounds");
				{
					// these will only be useful in anchor forms (in responsive the actual location of the parent component needs to be determined on client)
					Point ghostContainerLocation = getGhostContainerLocation(basicWebComponent, parentFormComponentPath);
					writer.object();
					writer.key("left").value(ghostContainerLocation.getX());
					writer.key("top").value(ghostContainerLocation.getY());
					writer.key("width").value(basicWebComponent.getSize().width);
					writer.key("height").value(basicWebComponent.getSize().height);
					writer.endObject();
				}
				String uuid = parentFormComponentPath != null && !parentFormComponentPath.isEmpty() ? basicWebComponent.getName().replaceAll("-", "_")
					: basicWebComponent.getUUID().toString();
				if (parentFormComponentPath != null && !parentFormComponentPath.isEmpty() && Character.isDigit(uuid.charAt(0)))
				{
					uuid = "_" + uuid;
				}
				writer.key("uuid").value(uuid);
				writer.key("class").value(inherited ? " inherited_element" : "");
				writer.key("containerPositionInComp").value(propYCount);
				writer.key("totalGhostContainersOfComp").value(totalGhostContainersOfComp);
				writer.key("propertyName").value(propertyName);
				writer.key("isArray").value(isArray);

				writer.key("ghosts");
				writer.array();
			}

			private void endGhostContainer(final JSONWriter writer)
			{
				writer.endArray();
				writer.endObject();
			}

			private void writeGhostToJSON(String parentKey, PropertyDescription pd, String simpleTypeName, int indexForPositioning, boolean inherited)
				throws JSONException
			{
				writeGhostToJSON(parentKey, writer, pd.getName() + (indexForPositioning >= 0 ? "[" + indexForPositioning + "]" : ""),
					UUID.randomUUID().toString(), indexForPositioning, simpleTypeName, inherited);
			}

			private void writeGhostToJSON(String parentKey, JSONWriter jsonWriter, String text, String uuid, int indexForPositioning, String simpleTypeName,
				boolean inherited) throws JSONException
			{
				jsonWriter.object();
				jsonWriter.key("uuid").value(uuid);
				jsonWriter.key("type").value(GHOST_TYPE_CONFIGURATION);
				jsonWriter.key("propertyType").value(simpleTypeName);
				jsonWriter.key("text").value(text);
				writer.key("class").value(inherited ? "inherited_element" : "");
				jsonWriter.key("location");
				{
					jsonWriter.object();
					int position = indexForPositioning;
					if (position < 0) position = 0;
					int computeX = computeX(position);
					int computeY = computeY(position);
					LocationCache.getINSTANCE().putLocation(parentKey, uuid, new Point(computeX, computeY));
					jsonWriter.key("x").value(computeX);
					jsonWriter.key("y").value(computeY);
					jsonWriter.endObject();
				}
				jsonWriter.key("size");
				jsonWriter.object();
				jsonWriter.key("width").value(80);
				jsonWriter.key("height").value(22); // this value is also set in editor.css for .ghost-dnd-mode; update it there as well if it needs to change
				jsonWriter.endObject();
				jsonWriter.endObject();

			}

			private int computeX(int index)
			{
				return 0 + index * 80;
			}

			private int computeY(int index)
			{
				return 0;
			}

			private void writeGhostForPersist(IPersist o)
			{
				writeGhostForPersist(o, null, null, new HashSet<String>());
			}

			private boolean isInShowedContainer(IPersist o)
			{
				IPersist parent = o;
				AbstractContainer container = ((RfbVisualFormEditorDesignPage)editorPart.getGraphicaleditor()).getShowedContainer();
				if (container != null)
				{
					do
					{
						parent = PersistHelper.getRealParent(parent);
					}
					while (parent != null && parent != container);
				}
				return parent != null;
			}

			private void writeGhostForPersist(IPersist o, String parentID, ArrayList<IBasicWebComponent> parentFormComponentPath,
				HashSet<String> recursiveCheck)
			{
				if (o instanceof TabPanel panel && isInShowedContainer(o))
				{
					try
					{
						writer.object();
						Point ghostContainerLocation = getGhostContainerLocation(panel, parentFormComponentPath);
						writer.key("parentCompBounds");
						{
							// these will only be useful in anchor forms (in responsive the actual location of the parent component needs to be determined on client)
							writer.object();
							writer.key("left").value(ghostContainerLocation.getX());
							writer.key("top").value(ghostContainerLocation.getY());
							writer.key("width").value(panel.getSize().width);
							writer.key("height").value(panel.getSize().height);
							writer.endObject();
						}
						writer.key("containerPositionInComp").value(0);
						writer.key("totalGhostContainersOfComp").value(1);
						writer.key("style");
						{
							writer.object();
							writer.key("left").value(0);
							writer.key("top").value(0);
							writer.endObject();
						}
						writer.key("uuid").value(panel.getUUID());
						writer.key("ghosts");
						{
							writer.array();
							TabPanel super_tabPanel = (TabPanel)PersistHelper.getSuperPersist(panel);
							boolean inherited = Utils.isInheritedFormElement(panel, editorPart.getForm()) && super_tabPanel == null;
							Iterator<IPersist> tabIterator = panel.getTabs();
							int i = 0;
							while (tabIterator.hasNext())
							{
								Tab tab = (Tab)tabIterator.next();
								int x = i * 80; //tab.getLocation().x;
								int y = 0; //tab.getLocation().y;
								writer.object();
								writer.key("uuid").value(tab.getUUID());
								writer.key("type").value(GHOST_TYPE_CONFIGURATION);
								String text = tab.getText();
								writer.key("text").value(text != null && text.trim().length() > 0 ? text : "tabs[" + i + "]");
								writer.key("class").value(
									inherited || super_tabPanel != null && super_tabPanel.getChild(tab.getUUID()) != null ? "inherited_element" : "");
								writer.key("location");
								writer.object();
								writer.key("x").value(x);
								writer.key("y").value(y);
								writer.endObject();
								writer.key("size");
								writer.object();
								writer.key("width").value(80);
								writer.key("height").value(20);
								writer.endObject();
								writer.endObject();
								i++;
							}
							writer.endArray();
						}
						writer.endObject();
					}
					catch (IllegalArgumentException | JSONException e)
					{
						Debug.error(e);
					}
				}
				else if (o instanceof Form)
				{
					Form f = ModelUtils.getEditingFlattenedSolution(o).getFlattenedForm(o);
					if (!((Form)o).isResponsiveLayout() && ((Form)o).getParts().hasNext()) // absolute layout
					{
						writePartGhosts(writer, f);
					}
				}
				else if (o instanceof Portal && isInShowedContainer(o))
				{
					try
					{
						writer.object();
						writer.key("style");
						Portal portal = (Portal)o;
						{
							writer.object();
							writer.key("left").value(0);
							writer.key("top").value(0);
							writer.endObject();
						}
						writer.key("uuid").value(o.getUUID());
						writer.key("ghosts");
						writer.array();
						{
							Iterator<IPersist> partIterator = portal.getAllObjects();
							ArrayList<BaseComponent> persists = new ArrayList<BaseComponent>();
							while (partIterator.hasNext())
							{
								IPersist next = partIterator.next();
								if (next instanceof BaseComponent) persists.add((BaseComponent)next);
							}

							Portal super_portal = (Portal)PersistHelper.getSuperPersist(portal);
							boolean inherited = Utils.isInheritedFormElement(portal, editorPart.getForm()) && super_portal == null;
							for (int i = 0; i < persists.size(); i++)
							{
								writer.object();
								{
									BaseComponent baseComponent = persists.get(i);
									int x = baseComponent.getLocation().x;
									int y = baseComponent.getLocation().y;
									writer.key("uuid").value(persists.get(i).getUUID());
									writer.key("type").value(GHOST_TYPE_COMPONENT);
									Object label = getGhostLabel(baseComponent, "col" + i);
									writer.key("text").value(label);
									writer.key("class").value(
										inherited || super_portal != null && super_portal.getChild(baseComponent.getUUID()) != null ? "inherited_element" : "");
									writer.key("location");
									writer.object();
									{
										writer.key("x").value(x);
										writer.key("y").value(y);
									}
									writer.endObject();
									writer.key("size");
									writer.object();
									{
										writer.key("width").value(persists.get(i).getSize().width);
										writer.key("height").value(persists.get(i).getSize().height);
									}
									writer.endObject();
								}
								writer.endObject();
							}
						}
						endGhostContainer(writer);
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
				else if (o instanceof IBasicWebComponent && isInShowedContainer(o))
				{
					writeGhostsForWebcomponentBeans((IBasicWebComponent)o, parentID, parentFormComponentPath);

					String componentType = FormTemplateGenerator.getComponentTypeName((IBasicWebComponent)o);
					WebObjectSpecification specification = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(componentType);
					if (specification != null)
					{
						Collection<PropertyDescription> properties = specification.getProperties(FormComponentPropertyType.INSTANCE);
						if (properties.size() > 0)
						{
							FlattenedSolution fs = ModelUtils.getEditingFlattenedSolution(editorPart.getForm());
							FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement((IBasicWebComponent)o, fs, null, true);
							for (PropertyDescription pd : properties)
							{
								Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
								Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
								if (frm == null) continue;
								if (!recursiveCheck.add(frm.getName()))
								{
									Debug.error("recursive reference found between (List)FormComponents: " + recursiveCheck);
									continue;
								}
								FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentEl, pd, (JSONObject)propertyValue, frm,
									fs);
								ArrayList<IBasicWebComponent> newParentFormComponentPath;
								if (parentFormComponentPath == null)
								{
									newParentFormComponentPath = new ArrayList<IBasicWebComponent>();
								}
								else
								{
									newParentFormComponentPath = parentFormComponentPath;
								}
								newParentFormComponentPath.add((IBasicWebComponent)o);
								for (FormElement element : cache.getFormComponentElements())
								{
									IPersist p = element.getPersistIfAvailable();
									if (p instanceof IFormElement)
									{
										writeGhostForPersist(p, ((IFormElement)p).getName(), newParentFormComponentPath, recursiveCheck);
									}
								}
								recursiveCheck.remove(frm.getName());
							}
						}
					}
				}
			}

			@Override
			public Object visit(IPersist o)
			{
				writeGhostForPersist(o);
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}

			private void writePartGhosts(final JSONWriter writer, Form f)
			{
				try
				{
					writer.object();
					{
						writer.key("uuid").value(UUID.randomUUID()/* f.getUUID() */);
						writer.key("ghosts");
						writer.array();
						{
							Iterator<Part> partIterator = f.getParts();
							ArrayList<Part> parts = new ArrayList<Part>();
							while (partIterator.hasNext())
								parts.add(partIterator.next());

							// the parts
							for (int i = 0; i < parts.size(); i++)
							{
								writer.object();
								{
									writer.key("uuid").value(parts.get(i).getUUID());
									writer.key("type").value(GHOST_TYPE_PART);
									writer.key("text").value(Part.getDisplayName(parts.get(i).getPartType()));
									writer.key("location");
									writer.object();
									{
										writer.key("x").value(0);
										writer.key("y").value(parts.get(i).getSize().getHeight());
									}
									writer.endObject();
									writer.key("parttype").value(parts.get(i).getPartType());
									if (i > 0) writer.key("partprev").value(parts.get(i - 1).getUUID());
									if (i < parts.size() - 1) writer.key("partnext").value(parts.get(i + 1).getUUID());
								}
								writer.endObject();
							}

							// the form itself
							writer.object();
							{
								writer.key("uuid").value(f.getUUID());
								writer.key("type").value(GHOST_TYPE_FORM);
								writer.key("text").value("");
								writer.key("location");
								writer.object();
								writer.key("x").value(0);
								writer.key("y").value(0);
								writer.endObject();
								writer.key("size");
								writer.object();
								writer.key("width").value(f.getSize().width);
								writer.key("height").value(f.getSize().height);
								writer.endObject();
							}
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
					Debug.error(e);
				}
			}
		});
		writer.endArray();
		writer.endObject();
		return new JSONObject(stringWriter.getBuffer().toString());
	}

	private void printGhostFormElements(final JSONWriter writer, Iterator<IFormElement> elementsIterator, String type) throws JSONException
	{
		while (elementsIterator.hasNext())
		{
			IFormElement persist = elementsIterator.next();
			writer.object();
			writer.key("uuid").value(persist.getUUID());
			writer.key("type").value(type);
			writer.key("text").value(getGhostLabel(persist, "element"));
			writer.key("location");
			Point location = CSSPositionUtils.getLocation(persist);
			writer.object();
			writer.key("x").value(location.x);
			writer.key("y").value(location.y);
			writer.endObject();
			writer.key("size");
			Dimension size = CSSPositionUtils.getSize(persist);
			writer.object();
			writer.key("width").value(size.width);
			writer.key("height").value(size.height);
			writer.endObject();
			writer.endObject();
		}
	}

	private String getGhostLabel(IPersist next, String defaultLabel)
	{
		if (next instanceof ISupportName)
		{
			String name = ((ISupportName)next).getName();
			if (name != null) return name;
		}
		if (next instanceof AbstractBase)
		{
			Object label = ((AbstractBase)next).getProperty("text");
			if (label != null) return label.toString();
		}
		if (next instanceof IBasicWebComponent)
		{
			return ((IBasicWebComponent)next).getTypeName();
		}
		if (next instanceof ISupportDataProviderID)
		{
			String dp = ((ISupportDataProviderID)next).getDataProviderID();
			if (dp != null) return dp;
		}
		return defaultLabel;
	}

	public static boolean isDroppable(PropertyDescription propertyDescription, Object rootPropConfigObject)
	{
		return RFBDesignerUtils.isDroppable(propertyDescription, rootPropConfigObject);
	}

}
