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

import java.awt.Dimension;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CommandStack;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.swt.graphics.Point;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.elements.IFieldPositioner;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.property.FormElementGroupPropertySource;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.dnd.FormElementDragData.DataProviderDragData;
import com.servoy.eclipse.dnd.FormElementDragData.PersistDragData;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IBasicWebComponent;
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
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Command to place an element in the form designer.
 *
 * @author rgansevles
 */

public abstract class BaseFormPlaceElementCommand extends AbstractModelsCommand
{
	protected ISupportChilds parent;
	protected final IPersist context;
	private IPersist alternativeParent;
	protected final Object object;
	protected final Point defaultLocation;
	protected final IFieldPositioner fieldPositioner;

	protected Object[] models;
	protected final Map<Object, Object> objectProperties;
	protected final Object requestType;
	protected final IApplication application;
	protected final org.eclipse.draw2d.geometry.Dimension size;

	/**
	 * Command to add a field.
	 *
	 * @param parent
	 * @param location
	 * @param object
	 * @param size
	 * @param
	 */
	public BaseFormPlaceElementCommand(IApplication application, ISupportChilds parent, Object object, Object requestType, Map<Object, Object> objectProperties,
		IFieldPositioner fieldPositioner, Point defaultLocation, org.eclipse.draw2d.geometry.Dimension size, IPersist context)
	{
		this.application = application;
		this.parent = parent;
		this.object = object;
		this.requestType = requestType;
		this.objectProperties = objectProperties;
		this.fieldPositioner = fieldPositioner;
		this.defaultLocation = defaultLocation;
		this.size = size;
		this.context = context;
	}

	abstract protected Object[] placeElements(Point location) throws RepositoryException;

	public Object[] getModels()
	{
		return models;
	}


	@Override
	public final void execute()
	{
		if (fieldPositioner != null && defaultLocation != null)
		{
			fieldPositioner.setDefaultLocation(defaultLocation);
		}
		models = null;
		alternativeParent = null;
		try
		{
			parent = (ISupportChilds)ElementUtil.getOverridePersist(PersistContext.create(parent, context));
			Point location = getNextLocation();
			models = placeElements(location);
			if (models == null && object instanceof Object[] && ((Object[])object).length > 0)
			{
				// drag-n-drop or paste
				models = pasteElements(location);
			}

			// set data in request.getExtendedData map as properties in the created persists
			if (models != null)
			{
				if (size != null)
				{
					// resize all created models relative to the current bounding box
					applySizeToModels(size, models);
				}
				setPropertiesOnModels();
				for (Object model : models)
				{
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, model, false);
				}
			}
			else
			{
				ServoyLog.logWarning("command not supported: " + requestType, null);
			}
		}
		catch (RepositoryException ex)
		{
			ServoyLog.logError(ex);
		}
	}

	protected void setPropertiesOnModels()
	{
		if (objectProperties != null && objectProperties.size() > 0)
		{
			for (Object model : models)
			{
				setProperiesOnModel(model, objectProperties);
			}
		}
	}

	public static void setProperiesOnModel(Object model, Map<Object, Object> objectProperties)
	{
		Command setPropertiesCommand = SetValueCommand.createSetPropertiesCommand(Platform.getAdapterManager().getAdapter(model, IPropertySource.class),
			objectProperties);
		if (setPropertiesCommand != null)
		{
			setPropertiesCommand.execute();
		}

	}

	/**
	 * @param size
	 * @param models
	 */
	private static void applySizeToModels(org.eclipse.draw2d.geometry.Dimension size, Object[] models)
	{
		Rectangle oldBounds = Utils.getBounds(Arrays.asList(models).iterator());
		if (size.width == oldBounds.width && size.height == oldBounds.height || oldBounds.width == 0 || oldBounds.height == 0)
		{
			return;
		}

		float factorW = size.width / (float)oldBounds.width;
		float factorH = size.height / (float)oldBounds.height;

		for (Object model : models)
		{
			if (!(model instanceof ISupportBounds))
			{
				continue;
			}
			ISupportBounds element = (ISupportBounds)model;

			Dimension oldElementSize = CSSPositionUtils.getSize(element);
			java.awt.Point oldElementLocation = CSSPositionUtils.getLocation(element);

			Dimension newElementSize = new Dimension((int)(oldElementSize.width * factorW), (int)(oldElementSize.height * factorH));

			int newX;
			if (oldElementLocation.x + oldElementSize.width == oldBounds.x + oldBounds.width)
			{
				// element was attached to the right side, keep it there
				newX = oldBounds.x + size.width - newElementSize.width;
			}
			else
			{
				// move relative to size factor
				newX = oldBounds.x + (int)((oldElementLocation.x - oldBounds.x) * factorW);
			}
			int newY;
			if (oldElementLocation.y + oldElementSize.height == oldBounds.y + oldBounds.height)
			{
				// element was attached to the bottom side, keep it there
				newY = oldBounds.y + size.height - newElementSize.height;
			}
			else
			{
				// move relative to size factor
				newY = oldBounds.y + (int)((oldElementLocation.y - oldBounds.y) * factorH);
			}
			java.awt.Point location = new java.awt.Point(newX, newY);

			if (element instanceof FormElementGroup)
			{
				FormElementGroupPropertySource formElementGroupPropertySource = new FormElementGroupPropertySource((FormElementGroup)element, null);
				formElementGroupPropertySource.setSize(newElementSize);
				formElementGroupPropertySource.setLocation(location);
			}
			else
			{
				CSSPositionUtils.setSize(element, newElementSize.width, newElementSize.height);
				CSSPositionUtils.setLocation(element, location.x, location.y);
			}
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

	public Object[] pasteElements(Point location) throws RepositoryException
	{
		// drag-n-drop or paste
		List<Object> res = new ArrayList<Object>(((Object[])object).length);

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
				Object[] pasted = pastePersist((PersistDragData)o, loc, origLocations, groupMap);
				if (pasted != null && pasted.length > 0 && pasted[0] instanceof IPersist &&
					((!(parent instanceof TabPanel) && ((PersistDragData)o).type == IRepository.TABPANELS) ||
						(!(parent instanceof Portal) && ((PersistDragData)o).type == IRepository.PORTALS)))
				{
					alternativeParent = (IPersist)pasted[0];
				}
				if (pasted != null)
				{
					for (Object obj : pasted)
					{
						res.add(obj);
					}
				}
			}
			else
			{
				ServoyLog.logWarning("paste/drop unsupported class:  " + o.getClass(), null);
			}
		}
		if (location != null && origLocations.size() > 1)
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
				ISupportBounds element = entry.getKey();
				if (element instanceof BaseComponent && parent instanceof Form && ((Form)parent).getUseCssPosition())
				{
					CSSPosition cssPosition = ((BaseComponent)element).getCssPosition();

					((BaseComponent)element).setCssPosition(cssPosition);
				}
				else
				{
					CSSPositionUtils.setLocation(entry.getKey(), location.x + entry.getValue().x - minx, location.y + entry.getValue().y - miny);
				}
			}
		}
		return res.toArray();
	}


	protected static IPersist[] toArrAy(IPersist persist)
	{
		if (persist == null)
		{
			return null;
		}
		return new IPersist[] { persist };
	}

	protected Object[] pastePersist(PersistDragData dragData, Point location, Map<ISupportBounds, java.awt.Point> origLocations, Map<String, String> groupMap)
		throws RepositoryException
	{
		if (dragData.type == IRepository.TEMPLATES)
		{
			for (IRootObject template : ServoyModelManager.getServoyModelManager().getServoyModel().getActiveRootObjects(dragData.type))
			{
				if (template.getUUID().equals(dragData.uuid))
				{
					setLabel("place template");
					return ElementFactory.applyTemplate((ISupportFormElements)parent, new TemplateElementHolder((Template)template, dragData.element), location,
						false);
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
						CommandStack commandStack = editor.getAdapter(CommandStack.class);
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
		if ((draggedPersist == parent) || (draggedPersist instanceof WebComponent && parent instanceof WebComponent))
		{
			parent = draggedPersist.getParent();
		}

		if (parent instanceof ISupportFormElements)
		{
			if (draggedPersist instanceof ScriptMethod)
			{
				setLabel("place method button");
				ScriptMethod sm = (ScriptMethod)draggedPersist;
				return toArrAy(ElementFactory.createButton((ISupportFormElements)parent, sm, sm.getName(), location));
			}

			if (draggedPersist instanceof IDataProvider)
			{
				setLabel("drag-n-drop field");
				return toArrAy(ElementFactory.createField((ISupportFormElements)parent, (IDataProvider)draggedPersist, location));
			}

			if (draggedPersist instanceof Media)
			{
				setLabel("place image");
				return toArrAy(ElementFactory.createImage((ISupportFormElements)parent, (Media)draggedPersist, location));
			}
		}

		if (draggedPersist instanceof IFormElement || draggedPersist instanceof Tab || draggedPersist instanceof LayoutContainer)
		{
			setLabel("paste component");
			ISupportBounds supportBounds = (ISupportBounds)draggedPersist;

			int x, y;
			if (location == null)
			{
				int copyPasteOffset = new DesignerPreferences().getCopyPasteOffset();
				x = CSSPositionUtils.getLocation(supportBounds).x + copyPasteOffset;
				y = CSSPositionUtils.getLocation(supportBounds).y + copyPasteOffset;
			}
			else
			{
				x = location.x;
				y = location.y;
			}

			IPersist persist;
			if (draggedPersist instanceof Tab)
			{
				if (parent instanceof TabPanel)
				{
					persist = ElementFactory.copyComponent(parent, (Tab)draggedPersist, x, y, IRepository.TABS, groupMap);
				}
				else
				{
					if (alternativeParent == null)
					{
						alternativeParent = draggedPersist.getParent();
					}
					if (alternativeParent instanceof TabPanel)
					{
						persist = ElementFactory.copyComponent((ISupportChilds)alternativeParent, (Tab)draggedPersist, x, y, IRepository.TABS, groupMap);
					}
					else
					{
						ServoyLog.logWarning("paste object: cannot paste tab to non-tabpanel", null);
						return null;
					}
				}
			}
			else if (!(parent instanceof Portal) && alternativeParent instanceof Portal)
			{
				persist = ElementFactory.copyComponent((ISupportChilds)alternativeParent, (AbstractBase)draggedPersist, x, y, IRepository.ELEMENTS, groupMap);
			}
			else if (parent instanceof LayoutContainer)
			{
				if (draggedPersist instanceof LayoutContainer) persist = isComponentAllowedForPaste(parent, draggedPersist)
					? ElementFactory.copyComponent(parent, (LayoutContainer)draggedPersist, x, y, IRepository.LAYOUTCONTAINERS, groupMap) : null;

				else persist = isComponentAllowedForPaste(parent, draggedPersist)
					? ElementFactory.copyComponent(parent, (AbstractBase)draggedPersist, x, y, IRepository.ELEMENTS, groupMap) : null;

				if (persist == null)
				{
					String draggedPersistSpecName = draggedPersist instanceof LayoutContainer ? ((LayoutContainer)draggedPersist).getSpecName() : "component";
					ServoyLog.logWarning("paste object: cannot paste " + draggedPersistSpecName + " into " + ((LayoutContainer)parent).getSpecName(), null);
					return null;
				}
			}
			else
			{
				persist = ElementFactory.copyComponent(parent, (AbstractBase)draggedPersist, x, y, IRepository.ELEMENTS, groupMap);
			}
			if (persist instanceof ISupportTabSeq)
			{
				((ISupportTabSeq)persist).setTabSeq(ISupportTabSeq.DEFAULT);
			}
			origLocations.put((ISupportBounds)persist, CSSPositionUtils.getLocation(supportBounds));
			return toArrAy(persist);
		}
		else if (draggedPersist instanceof WebCustomType && parent instanceof IBasicWebComponent)
		{
			WebCustomType iChildWebObject = (WebCustomType)draggedPersist;
			IPersist persist = AddContainerCommand.addCustomType((IBasicWebComponent)parent, iChildWebObject.getJsonKey(), iChildWebObject.getTypeName(), -1,
				iChildWebObject);
			return toArrAy(persist);
		}

		if (draggedPersist instanceof Form && parent instanceof Form)

		{
			return ElementFactory.createTabs(application, parent, new Object[] { new ElementFactory.RelatedForm(null, (Form)draggedPersist) }, location,
				TabPanel.DEFAULT_ORIENTATION, "tab_" + ((Form)draggedPersist).getName());
		}

		ServoyLog.logWarning("place object: dropped object not supported: " + draggedPersist.getClass().getName(), null);
		return null;
	}


	public static boolean isComponentAllowedForPaste(Object parent, Object component)
	{
		if (parent instanceof LayoutContainer)
		{
			if (((LayoutContainer)parent).getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) != null) return true;
			Set<String> allowed = DesignerUtil.getAllowedChildren().get(
				((LayoutContainer)parent).getPackageName() + "." + ((LayoutContainer)parent).getSpecName());
			if (component instanceof LayoutContainer)
				return allowed.contains(((LayoutContainer)component).getPackageName() + "." + ((LayoutContainer)component).getSpecName());
			if (component instanceof WebComponent) return allowed.contains("component");

			return true;
		}
		return true;
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

			if (command instanceof FormElementDeleteCommand)
			{
				for (IPersist persist : ((FormElementDeleteCommand)command).getPersists())
				{
					if (uuid.equals(persist.getUUID()))
					{
						return persist;
					}
					if (persist instanceof ISupportChilds)
					{
						IPersist found = AbstractRepository.searchPersist((ISupportChilds)persist, uuid);
						if (found != null)
						{
							return found;
						}
					}
				}
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
			FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(parent);
			if (ScopesUtils.isVariableScope(dragData.dataProviderId))
			{
				dataProvider = flattenedSolution.getGlobalDataProvider(dragData.dataProviderId);
			}
			else
			{
				ITable table = null;
				Relation[] relations = null;
				if (parent instanceof Form)
				{
					Form form = (Form)parent;
					if (dragData.relationName == null)
					{
						dataProvider = form.getScriptVariable(dragData.dataProviderId);
						if (dataProvider == null)
						{
							table = flattenedSolution.getTable(form.getDataSource());
						}
					}
					else
					{
						relations = flattenedSolution.getRelationSequence(dragData.relationName);
						if (relations == null)
						{
							return null;
						}
						table = flattenedSolution.getTable(relations[relations.length - 1].getForeignDataSource());
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

					table = flattenedSolution.getTable(relations[relations.length - 1].getForeignDataSource());
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
	public final void undo()
	{
		List<IPersist> toDelete = new ArrayList<IPersist>(); // put in toDelete list first, group iterator misses elements otherwise.
		for (Object model : models)
		{
			if (model instanceof IPersist)
			{
				toDelete.add((IPersist)model);
			}
			else if (model instanceof FormElementGroup)
			{
				Iterator<IFormElement> elements = ((FormElementGroup)model).getElements();
				while (elements.hasNext())
				{
					toDelete.add(elements.next());
				}
			}
		}

		try
		{
			for (IPersist del : toDelete)
			{
				((IDeveloperRepository)del.getRootObject().getRepository()).deleteObject(del);
			}
			ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, toDelete);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not undo create elements", e);
		}

		models = null;
	}
}
