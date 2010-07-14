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
package com.servoy.eclipse.designer.editor.commands;

import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.gef.Request;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.dnd.FormElementDragData.DataProviderDragData;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.util.UUID;

/**
 * Command to place an element in the form designer.
 * 
 * @author rgansevles
 */

public class FormPlaceElementCommand extends Command implements ISupportModels
{
	protected final ISupportChilds parent;
	protected IPersist alternativeParent;
	protected final Object object;
	protected Point defaultLocation;
	private final IFieldPositioner fieldPositioner;

	protected Object[] models;
	protected final Request request;

	/**
	 * Command to add a field.
	 * 
	 * @param parent
	 * @param location
	 * @param object
	 * @param
	 */
	public FormPlaceElementCommand(Request request, ISupportChilds parent, Object object, IFieldPositioner fieldPositioner, Point defaultLocation)
	{
		this.request = request;
		this.parent = parent;
		this.object = object;
		this.fieldPositioner = fieldPositioner;
		this.defaultLocation = defaultLocation;
	}

	public Object[] getModels()
	{
		return models;
	}

	@Override
	public void execute()
	{
		if (fieldPositioner != null && defaultLocation != null)
		{
			fieldPositioner.setDefaultLocation(defaultLocation);
		}
		models = null;
		alternativeParent = null;
		try
		{
			models = placeElements(getNextLocation());
			// set data in request.getExtendedData map as properties in the created persists
			if (models != null)
			{
				for (Object model : models)
				{
					if (model instanceof IPersist && request.getExtendedData() != null && request.getExtendedData().size() > 0)
					{
						IPersist persist = (IPersist)model;
						PersistPropertySource persistProperties = new PersistPropertySource(persist, persist, false);
						Iterator<Map.Entry<Object, Object>> iterator = request.getExtendedData().entrySet().iterator();
						while (iterator.hasNext())
						{
							Map.Entry<Object, Object> entry = iterator.next();
							persistProperties.setPersistPropertyValue(entry.getKey(), entry.getValue());
						}
					}
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, model, true);
				}
			}
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
	}

	protected Point getNextLocation()
	{
		if (fieldPositioner == null)
		{
			return defaultLocation;
		}
		return fieldPositioner.getNextLocation(null);
	}

	protected IPersist[] placeElements(Point location) throws RepositoryException
	{
		Object requestType = request.getType();

		if (VisualFormEditor.REQ_PLACE_TAB.equals(requestType))
		{
			setLabel("place tabpanel");
			return ElementFactory.createTabs(parent, (Object[])object, location, TabPanel.DEFAULT);
		}

		if (VisualFormEditor.REQ_PLACE_SPLIT_PANE.equals(requestType))
		{
			setLabel("place splitpane");
			return ElementFactory.createTabs(parent, (Object[])object, location, TabPanel.SPLIT_HORIZONTAL);
		}

		if (parent instanceof ISupportFormElements)
		{
			if (VisualFormEditor.REQ_PLACE_MEDIA.equals(requestType))
			{
				setLabel("place image");
				return new IPersist[] { ElementFactory.createImage((ISupportFormElements)parent, (Media)object, location) };
			}

			if (VisualFormEditor.REQ_PLACE_BUTTON.equals(requestType))
			{
				setLabel("place button");
				return new IPersist[] { ElementFactory.createButton((ISupportFormElements)parent, null, (object instanceof String) ? (String)object : "button",
					location) };
			}

			if (VisualFormEditor.REQ_PLACE_LABEL.equals(requestType) || object instanceof String)
			{
				setLabel("place label");
				return new IPersist[] { ElementFactory.createLabel((ISupportFormElements)parent, (object instanceof String) ? (String)object : "type", location) };
			}

			if (VisualFormEditor.REQ_PLACE_RECT_SHAPE.equals(requestType))
			{
				setLabel("place shape");
				return new IPersist[] { ElementFactory.createRectShape((ISupportFormElements)parent, location) };
			}
		}

		if (parent instanceof Form && VisualFormEditor.REQ_PLACE_BEAN.equals(requestType))
		{
			setLabel("place bean");
			return new IPersist[] { ElementFactory.createBean((Form)parent, (BeanInfo)object, location) };
		}

		if (parent instanceof Form && VisualFormEditor.REQ_PLACE_TEMPLATE.equals(requestType))
		{
			setLabel("place template");
			return ElementFactory.applyTemplate((Form)parent, (Template)object, location, false);
		}

		if (object instanceof Object[] && ((Object[])object).length > 0)
		{
			// drag-n-drop or paste
			List<IPersist> res = new ArrayList<IPersist>(((Object[])object).length);

			Map<ISupportBounds, java.awt.Point> origLocations = new HashMap<ISupportBounds, java.awt.Point>();
			Point loc = location;
			Map<String, String> groupMap = new HashMap<String, String>(); // holds mapping between old and new group ids for copied elements
			for (int i = 0; i < ((Object[])object).length; i++)
			{
				Object o = ((Object[])object)[i];
				if (o instanceof DataProviderDragData)
				{
					IPersist persist = pasteDataProvider((DataProviderDragData)o, loc);
					if (persist != null)
					{
						res.add(persist);
					}
				}
				else if (o instanceof PersistDragData)
				{
					IPersist[] persists = pastePersist((PersistDragData)o, loc, origLocations, groupMap);
					if (persists != null &&
						persists.length > 0 &&
						((!(parent instanceof TabPanel) && ((PersistDragData)o).type == IRepository.TABPANELS) || (!(parent instanceof Portal) && ((PersistDragData)o).type == IRepository.PORTALS)))
					{
						alternativeParent = persists[0];
					}
					if (persists != null)
					{
						for (IPersist persist : persists)
						{
							res.add(persist);
						}
					}
				}
				else
				{
					ServoyLog.logWarning("paste/drop unsupported class:  " + o.getClass(), null);
				}
			}
			if (origLocations.size() > 1)
			{
				// update the locations of the pasted persists to place them relative to each other same as in original position
				Set<Entry<ISupportBounds, java.awt.Point>> entrySet = origLocations.entrySet();

				// find the minimum x and y (upper-left corner of the original selection)
				int minx = Integer.MAX_VALUE;
				int miny = Integer.MAX_VALUE;
				for (Entry<ISupportBounds, java.awt.Point> entry : entrySet)
				{
					minx = minx < entry.getValue().x ? minx : entry.getValue().x;
					miny = miny < entry.getValue().y ? miny : entry.getValue().y;
				}
				// relocate relative to the upper-left corner of the original selection
				for (Entry<ISupportBounds, java.awt.Point> entry : entrySet)
				{
					entry.getKey().setLocation(new java.awt.Point(location.x + entry.getValue().x - minx, location.y + entry.getValue().y - miny));
				}
			}
			return res.toArray(new IPersist[res.size()]);
		}

		ServoyLog.logWarning("command not supported: " + requestType, null);
		return null;
	}

	protected IPersist[] pastePersist(PersistDragData dragData, Point location, Map<ISupportBounds, java.awt.Point> origLocations, Map<String, String> groupMap)
		throws RepositoryException
	{
		if (dragData.type == IRepository.TEMPLATES)
		{
			for (IRootObject template : ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(dragData.type))
			{
				if (template.getUUID().equals(dragData.uuid))
				{
					setLabel("place template");
					return ElementFactory.applyTemplate((ISupportFormElements)parent, (Template)template, location, false);
				}
			}
			ServoyLog.logWarning("place template: template " + dragData.uuid + " not found", null);
			return null;
		}

		ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(dragData.solutionName);
		if (servoyProject == null)
		{
			ServoyLog.logWarning("place method button: project " + dragData.solutionName + " not found", null);
			return null;
		}
		IPersist draggedPersist = AbstractRepository.searchPersist(servoyProject.getEditingSolution(), dragData.uuid);
		if (draggedPersist == null)
		{
			// maybe it was deleted by a CutAction, possibly by another editor
			for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages())
			{
				for (IEditorReference editorRef : page.getEditorReferences())
				{
					IEditorPart editor = editorRef.getEditor(false);
					if (editor != null)
					{
						CommandStack commandStack = (CommandStack)editor.getAdapter(CommandStack.class);
						if (commandStack != null)
						{
							draggedPersist = findDeletedPersist(commandStack.getCommands(), dragData.uuid);
							if (draggedPersist != null)
							{
								break;
							}
						}
					}
				}
				if (draggedPersist != null)
				{
					break;
				}
			}

			if (draggedPersist == null)
			{
				ServoyLog.logWarning("place object: dropped object not found: " + dragData.uuid, null);
				return null;
			}
		}

		if (parent instanceof ISupportFormElements)
		{
			if (draggedPersist instanceof ScriptMethod)
			{
				setLabel("place method button");
				ScriptMethod sm = (ScriptMethod)draggedPersist;
				return new IPersist[] { ElementFactory.createButton((ISupportFormElements)parent, sm, sm.getName(), location) };
			}

			if (draggedPersist instanceof IDataProvider)
			{
				setLabel("drag-n-drop field");
				return new IPersist[] { ElementFactory.createField((ISupportFormElements)parent, (IDataProvider)draggedPersist, location) };
			}

			if (draggedPersist instanceof Media)
			{
				setLabel("place image");
				return new IPersist[] { ElementFactory.createImage((ISupportFormElements)parent, (Media)draggedPersist, location) };
			}
		}

		if (draggedPersist instanceof IFormElement || draggedPersist instanceof Tab)
		{
			setLabel("paste component");
			ISupportBounds supportBounds = (ISupportBounds)draggedPersist;

			int x, y;
			if (location == null)
			{
				DesignerPreferences designerPreferences = new DesignerPreferences(ServoyModel.getSettings());
				int copyPasteOffset = designerPreferences.getCopyPasteOffset();
				x = supportBounds.getLocation().x + copyPasteOffset;
				y = supportBounds.getLocation().y + copyPasteOffset;
			}
			else
			{
				x = location.x;
				y = location.y;
			}

			IPersist persist;
			if (draggedPersist instanceof Tab)
			{
				if (!(parent instanceof TabPanel) && alternativeParent instanceof TabPanel)
				{
					persist = ElementFactory.copyComponent((ISupportChilds)alternativeParent, (Tab)draggedPersist, x, y, IRepository.TABS, groupMap);
				}
				else
				{
					persist = ElementFactory.copyComponent(parent, (Tab)draggedPersist, x, y, IRepository.TABS, groupMap);
				}
			}
			else if (draggedPersist instanceof Field && !(parent instanceof Portal) && alternativeParent instanceof Portal)
			{
				persist = ElementFactory.copyComponent((ISupportChilds)alternativeParent, (Field)draggedPersist, x, y, IRepository.ELEMENTS, groupMap);
			}
			else
			{
				persist = ElementFactory.copyComponent(parent, (AbstractBase)draggedPersist, x, y, IRepository.ELEMENTS, groupMap);
			}
			if (persist instanceof ISupportTabSeq)
			{
				((ISupportTabSeq)persist).setTabSeq(ISupportTabSeq.DEFAULT);
			}
			origLocations.put((ISupportBounds)persist, supportBounds.getLocation());
			return new IPersist[] { persist };
		}

		ServoyLog.logWarning("place object: dropped object not supported: " + draggedPersist.getClass().getName(), null);
		return null;
	}

	/**
	 * Look for a command that deleted the persist with the uuid.
	 * 
	 * @param commands
	 * @param uuid
	 * @return
	 */
	public static IPersist findDeletedPersist(Object[] commands, UUID uuid)
	{
		for (int i = commands.length - 1; i >= 0; i--)
		{
			Object command = commands[i];
			while (command instanceof ICommandWrapper)
			{
				command = ((ICommandWrapper)command).getCommand();
			}
			if (command instanceof CompoundCommand)
			{
				IPersist persist = findDeletedPersist(((CompoundCommand)command).getCommands().toArray(), uuid);
				if (persist != null)
				{
					return persist;
				}
			}

			if ((command instanceof FormElementDeleteCommand && uuid.equals(((FormElementDeleteCommand)command).getPersist().getUUID())))
			{
				return ((FormElementDeleteCommand)command).getPersist();
			}
		}

		// not found
		return null;
	}

	protected IPersist pasteDataProvider(DataProviderDragData dragData, Point location) throws RepositoryException
	{
		if (parent instanceof ISupportFormElements)
		{
			IDataProvider dataProvider = null;
			FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(parent);
			if (!dragData.dataProviderId.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
			{
				Table table = null;
				Relation[] relations = null;
				if (parent instanceof Form)
				{
					Form form = (Form)parent;
					if (dragData.relationName == null)
					{
						dataProvider = form.getScriptVariable(dragData.dataProviderId);
						if (dataProvider == null)
						{
							table = form.getTable();
						}
					}
					else
					{
						relations = flattenedSolution.getRelationSequence(dragData.relationName);
						if (relations == null)
						{
							return null;
						}
						table = relations[relations.length - 1].getForeignTable();
					}
					if (dataProvider == null)
					{
						if (dragData.serverName == null || !dragData.serverName.equals(form.getServerName())) return null;
						if (dragData.baseTableName == null || !dragData.baseTableName.equals(form.getTableName())) return null;
					}
				}
				else if (parent instanceof Portal)
				{
					Portal portal = (Portal)parent;
					if (portal.getRelationName() == null || !portal.getRelationName().equals(dragData.relationName))
					{
						return null;
					}
					relations = flattenedSolution.getRelationSequence(dragData.relationName);
					if (relations == null)
					{
						return null;
					}

					table = relations[relations.length - 1].getForeignTable();
				}
				else
				{
					return null;
				}

				if (dataProvider == null)
				{
					if (table == null)
					{
						return null;
					}
					IColumn column = table.getColumn(dragData.dataProviderId);

					if (column == null)
					{
						column = AbstractBase.selectByName(flattenedSolution.getAggregateVariables(table, false), dragData.dataProviderId);
					}
					if (column == null)
					{
						column = AbstractBase.selectByName(flattenedSolution.getScriptCalculations(table, false), dragData.dataProviderId);
					}
					if (column != null)
					{
						if (relations == null)
						{
							dataProvider = column;
						}
						else
						{
							dataProvider = new ColumnWrapper(column, relations);
						}
					}
				}
			}
			else
			{
				dataProvider = flattenedSolution.getGlobalDataProvider(dragData.dataProviderId);
			}
			if (dataProvider != null)
			{
				setLabel("drag-n-drop field");
				return ElementFactory.createField((ISupportFormElements)parent, dataProvider, location);
			}
		}
		return null;
	}

	@Override
	public boolean canUndo()
	{
		return models != null && models.length > 0;
	}

	@Override
	public void undo()
	{
		for (Object model : models)
		{
			if (model instanceof IPersist)
			{
				IPersist persist = (IPersist)model;
				try
				{
					((IDeveloperRepository)persist.getRootObject().getRepository()).deleteObject(persist);
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, model, true);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not undo create element " + model, e);
				}
			}
		}
		models = null;
	}
}
