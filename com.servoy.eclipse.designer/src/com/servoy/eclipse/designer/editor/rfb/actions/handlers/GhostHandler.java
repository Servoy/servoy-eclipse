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

import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebComponentSpecification;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.server.ngclient.template.FormTemplateGenerator;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * @author user
 *
 */
public class GhostHandler implements IServerService
{
	private final BaseVisualFormEditor editorPart;

	/**
	 * @param editorPart2
	 */
	public GhostHandler(BaseVisualFormEditor editorPart)
	{
		this.editorPart = editorPart;
	}

	/**
	 * @param methodName
	 * @param args
	 * @return
	 * @throws JSONException
	 */
	public Object executeMethod(String methodName, final JSONObject args) throws JSONException
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
				jsonWriter.key("width").value(80);
				jsonWriter.key("height").value(20);
				jsonWriter.endObject();
				jsonWriter.endObject();

			}

			private int computeX(int index)
			{
				return 15 + index * 80;
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
								int x = tab.getLocation().x;
								int y = tab.getLocation().y;
								if (args != null && args.has("resetPosition"))
								{
									x -= panel.getLocation().x;
									y -= panel.getLocation().y;
								}
								writer.object();
								writer.key("uuid").value(tab.getUUID());
								writer.key("type").value(tab.getTypeID());
								writer.key("text").value(tab.getText());
								writer.key("location");
								writer.object();
								writer.key("x").value(x);
								writer.key("y").value(y);
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
						Form f = (Form)o;
						writer.object();
						writer.key("uuid").value(UUID.randomUUID()/* f.getUUID() */);
						writer.key("ghosts");
						writer.array();
						Iterator<Part> partIterator = f.getParts();
						ArrayList<Part> parts = new ArrayList<Part>();
						while (partIterator.hasNext())
							parts.add(partIterator.next());

						// the parts
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

						// the form itself
						writer.object();
						writer.key("uuid").value(f.getUUID());
						writer.key("type").value(f.getTypeID());
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
						writer.endObject();

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
				else if (o instanceof Portal)
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
							Iterator<IPersist> partIterator = ((Portal)o).getAllObjects();
							ArrayList<BaseComponent> persists = new ArrayList<BaseComponent>();
							while (partIterator.hasNext())
							{
								IPersist next = partIterator.next();
								if (next instanceof BaseComponent) persists.add((BaseComponent)next);
							}

							for (int i = 0; i < persists.size(); i++)
							{
								writer.object();
								{

									BaseComponent baseComponent = persists.get(i);
									int x = baseComponent.getLocation().x;
									int y = baseComponent.getLocation().y;
									writer.key("uuid").value(persists.get(i).getUUID());
									writer.key("type").value(persists.get(i).getTypeID());
									Object label = baseComponent.getProperty("text");
									if (label == null || label.toString().trim().equals(""))
									{
										label = baseComponent.getProperty("name");
									}
									writer.key("text").value(label);

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
}
