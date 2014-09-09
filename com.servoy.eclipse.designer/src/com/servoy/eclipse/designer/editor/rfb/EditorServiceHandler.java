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
import java.util.Arrays;
import java.util.Iterator;
import java.util.concurrent.atomic.AtomicInteger;

import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.widgets.Display;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONWriter;
import org.sablo.websocket.IServerService;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.FormElementDeleteCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.util.Debug;
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

	public EditorServiceHandler(BaseVisualFormEditor editorPart, ISelectionProvider selectionProvider)
	{
		this.editorPart = editorPart;
		this.selectionProvider = selectionProvider;
	}

	@Override
	public Object executeMethod(String methodName, final JSONObject args)
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

					private int computeX(int index)
					{
						return index * 40;
					}

					private int computeY(int index)
					{
						return -70;
					}

					@Override
					public Object visit(IPersist o)
					{
						if (o instanceof TabPanel)
						{
							try
							{
								writer.object();
								writer.key("left").value(((TabPanel)o).getLocation().getX());
								writer.key("top").value(((TabPanel)o).getLocation().getY());
								writer.key("uuid").value(((TabPanel)o).getUUID());
								writer.key("ghosts");
								writer.array();
								Iterator<IPersist> tabIterator = ((TabPanel)o).getTabs();
								int i = 0;
								while (tabIterator.hasNext())
								{
									IPersist tab = tabIterator.next();
									writer.object();
									writer.key("uuid").value(tab.getUUID());
									writer.key("text").value(((Tab)tab).getText());
									writer.key("location");
									writer.object();
									writer.key("x").value(computeX(i));
									writer.key("y").value(computeY(i));
//									writer.key("x").value(((Tab)tab).getLocation().x);
//									writer.key("y").value(((Tab)tab).getLocation().y);
									writer.endObject();
									writer.endObject();
									i++;
								}
								writer.endArray();
								writer.endObject();
							}
							catch (IllegalArgumentException e)
							{
								e.printStackTrace();
							}
							catch (JSONException e)
							{
								e.printStackTrace();
							}
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
				final Object[] selection = new Object[json.length()];
				for (int i = 0; i < json.length(); i++)
				{
					selection[i] = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(json.getString(i)));
				}
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						selectionProvider.setSelection(selection.length == 0 ? null : new StructuredSelection(selection));
					}
				});
			}
			else if ("keyPressed".equals(methodName))
			{
				if (args.optInt("keyCode") == 46)
				{
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
							String uuid = (String)keys.next();
							final IPersist persist = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(UUID.fromString(uuid));
							if (persist instanceof BaseComponent || persist instanceof Tab)
							{
								JSONObject properties = args.optJSONObject(uuid);
								CompoundCommand cc = new CompoundCommand();
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
								editorPart.getCommandStack().execute(cc);
							}
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
									int x = args.getInt("x");
									int y = args.getInt("y");
									String name = args.getString("name");
									if ("svy-button".equals(name))
									{
										GraphicalComponent gc = editorPart.getForm().createNewGraphicalComponent(new Point(x, y));
										gc.setText("button");
										gc.setOnActionMethodID(-1);
										newPersist = gc;
									}
									else if ("svy-label".equals(name))
									{
										GraphicalComponent gc = editorPart.getForm().createNewGraphicalComponent(new Point(x, y));
										gc.setText("label");
										newPersist = gc;
									}
									else if ("svy-combobox".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.COMBOBOX);
										newPersist = field;
									}
									else if ("svy-textfield".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.TEXT_FIELD);
										newPersist = field;
									}
									else if ("svy-textarea".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.TEXT_AREA);
										newPersist = field;
									}
									else if ("svy-password".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.PASSWORD);
										newPersist = field;
									}
									else if ("svy-calendar".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.CALENDAR);
										newPersist = field;
									}
									else if ("svy-typeahead".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.TYPE_AHEAD);
										newPersist = field;
									}
									else if ("svy-spinner".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.SPINNER);
										newPersist = field;
									}
									else if ("svy-check".equals(name) || "svy-checkgroup".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.CHECKS);
										newPersist = field;
									}
									else if ("svy-radio".equals(name) || "svy-radiogroup".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.RADIOS);
										newPersist = field;
									}
									else if ("svy-imagemedia".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.IMAGE_MEDIA);
										newPersist = field;
									}
									else if ("svy-listbox".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.LIST_BOX);
										newPersist = field;
									}
									else if ("svy-htmlarea".equals(name))
									{
										Field field = editorPart.getForm().createNewField(new Point(x, y));
										field.setDisplayType(Field.HTML_AREA);
										field.setEditable(false);
										newPersist = field;
									}
									else if ("svy-tabpanel".equals(name))
									{
										String compName = "tabpanel_" + id.incrementAndGet();
										while (!checkName(compName))
										{
											compName = "tabpanel_" + id.incrementAndGet();
										}
										TabPanel tabPanel = editorPart.getForm().createNewTabPanel(compName);
										tabPanel.setLocation(new Point(x, y));
										newPersist = tabPanel;
									}
									else if ("svy-splitpane".equals(name))
									{
										String compName = "tabpanel_" + id.incrementAndGet();
										while (!checkName(compName))
										{
											compName = "tabpanel_" + id.incrementAndGet();
										}
										TabPanel tabPanel = editorPart.getForm().createNewTabPanel(compName);
										tabPanel.setLocation(new Point(x, y));
										tabPanel.setTabOrientation(TabPanel.SPLIT_HORIZONTAL);
										newPersist = tabPanel;
									}
									else if ("svy-portal".equals(name))
									{
										String compName = "portal_" + id.incrementAndGet();
										while (!checkName(compName))
										{
											compName = "portal_" + id.incrementAndGet();
										}
										Portal portal = editorPart.getForm().createNewPortal(compName, new Point(x, y));
										newPersist = portal;
									}
									else if ("svy-rectangle".equals(name))
									{
										RectShape shape = editorPart.getForm().createNewRectangle(new Point(x, y));
										shape.setLineSize(1);
										newPersist = shape;
									}
									else
									{
										// bean
										String compName = "bean_" + id.incrementAndGet();
										while (!checkName(compName))
										{
											compName = "bean_" + id.incrementAndGet();
										}
										String packageName = args.getString("packageName");
										Bean bean = editorPart.getForm().createNewBean(compName, packageName + ":" + name);
										bean.setLocation(new Point(x, y));
										newPersist = bean;
									}
									ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
										Arrays.asList(new IPersist[] { newPersist }));
								}
								catch (Exception ex)
								{
									Debug.error(ex);
								}
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
		}
		catch (JSONException e)
		{
			ServoyLog.logError(e);
		}

		return null;
	}
}
