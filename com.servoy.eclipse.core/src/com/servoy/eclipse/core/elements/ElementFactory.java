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
package com.servoy.eclipse.core.elements;

import java.awt.Dimension;
import java.beans.BeanInfo;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import javax.swing.BorderFactory;
import javax.swing.ImageIcon;

import org.eclipse.swt.graphics.Point;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.core.repository.SolutionDeserializer;
import com.servoy.eclipse.core.repository.SolutionSerializer;
import com.servoy.eclipse.core.util.CoreUtils;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.DummyValidator;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportText;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.PositionComparator;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class ElementFactory
{
	private static Random random = new Random();

	public static GraphicalComponent createLabel(ISupportFormElements parent, String text, Point location) throws RepositoryException
	{
		GraphicalComponent label = parent.createNewGraphicalComponent(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		label.setText(text);
		label.setTransparent(true);
		placeElementOnTop(label);
		return label;
	}

	public static GraphicalComponent createLabel(ISupportFormElements parent, IDataProvider dataProvider, String text, Point location)
		throws RepositoryException
	{
		GraphicalComponent label = createLabel(parent, text, location);
		label.setDataProviderID(dataProvider.getDataProviderID());
		return label;
	}

	public static IPersist createButton(ISupportFormElements parent, ScriptMethod method, String text, Point location) throws RepositoryException
	{
		GraphicalComponent button = parent.createNewGraphicalComponent(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		button.setOnActionMethodID(method == null ? -1 : method.getID());
		button.setOnDoubleClickMethodID(-1);
		button.setOnRightClickMethodID(-1);
		button.setText(text);
		placeElementOnTop(button);
		return button;
	}

	public static void placeElementOnTop(IFormElement element)
	{
		if (element instanceof IPersist)
		{
			int maxFormIndex = 0;
			Iterator<IPersist> bros = ((IPersist)element).getParent().getAllObjects();
			while (bros.hasNext())
			{
				IPersist bro = bros.next();
				if (bro != element && bro instanceof IFormElement)
				{
					maxFormIndex = Math.max(maxFormIndex, ((IFormElement)bro).getFormIndex());
				}
			}
			if (maxFormIndex > 0)
			{
				element.setFormIndex(maxFormIndex + 1);
			}
		}
	}

	public static IPersist createRectShape(ISupportFormElements parent, Point location) throws RepositoryException
	{
		RectShape rectShape = parent.createNewRectangle(new java.awt.Point(location.x, location.y));
		rectShape.setLineSize(1);
		rectShape.setSize(new java.awt.Dimension(70, 70));
		placeElementOnTop(rectShape);
		return rectShape;
	}

	public static String createUniqueName(ISupportChilds parent, int type, String name) throws RepositoryException
	{
		String newName = name;
		if (name != null)
		{
			IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
			int i = 0;
			while (true)
			{
				try
				{
					validator.checkName(newName, 0, new ValidatorSearchContext(parent, type), false);
					break;
				}
				catch (RepositoryException e)
				{
					if (++i == 100) //can't go on forever...
					{
						throw new RepositoryException("Cannot create new unique name for '" + name + "' in parent " + parent, e);
					}
					newName += 'c';
				}
			}
		}
		return newName;
	}

	public static IPersist copyComponent(ISupportChilds parent, AbstractBase component, int x, int y, int type, Map<String, String> groupMap)
		throws RepositoryException
	{
		String name = (component instanceof ISupportUpdateableName) ? createUniqueName(parent, type, ((ISupportUpdateableName)component).getName()) : null;
		// place copied group
		String groupId = (component instanceof IFormElement) ? ((IFormElement)component).getGroupID() : null;
		String newGroupId = null;
		if (groupId != null && groupMap != null)
		{
			newGroupId = groupMap.get(groupId);
			if (newGroupId == null)
			{
				// have not seen this group before
				String groupName = FormElementGroup.getName(groupId);
				if (groupName == null)
				{
					// anonymous group
					newGroupId = UUID.randomUUID().toString();
				}
				else
				{
					// create a new group name
					newGroupId = createUniqueName(parent, type, groupId);
				}
				groupMap.put(groupId, newGroupId);
			}
		}
		IValidateName validator = ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator();
		AbstractBase copy = (AbstractBase)component.cloneObj(parent, false, validator, true, true);
		if (copy instanceof ISupportBounds)
		{
			((ISupportBounds)copy).setLocation(new java.awt.Point(x, y));
		}
		if (name != null)
		{
			((ISupportUpdateableName)copy).updateName(validator, name);
			copy.setRuntimeProperty(AbstractBase.NameChangeProperty, ""); //$NON-NLS-1$
		}
		if (copy instanceof IFormElement)
		{
			((IFormElement)copy).setGroupID(newGroupId);
		}
		return copy;
	}

	public static IPersist createImage(ISupportFormElements parent, Media media, Point location) throws RepositoryException
	{
		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(parent);
		ImageIcon ii = ImageLoader.getIcon(ComponentFactory.loadIcon(flattenedSolution, new Integer(media.getID())), -1, -1, true);
		if (ii == null) return null;

		GraphicalComponent label = parent.createNewGraphicalComponent(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		label.setText(""); //$NON-NLS-1$
		label.setImageMediaID(media.getID());
		Dimension labeldim = new Dimension(ii.getIconWidth(), ii.getIconHeight());
		if (labeldim.width < 10 || labeldim.height < 10)
		{
			int dif = 0;
			if (labeldim.width >= labeldim.height)
			{
				dif = labeldim.width - labeldim.height;
				labeldim = new Dimension(10 + dif, 10);
			}
			else
			{
				dif = labeldim.height - labeldim.width;
				labeldim = new Dimension(10, 10 + dif);
			}
		}
		label.setSize(labeldim);
		placeElementOnTop(label);
		return label;
	}

	public static IPersist createBean(Form form, BeanInfo beanInfo, Point location) throws RepositoryException
	{
		Bean bean = form.createNewBean("bean_" + random.nextInt(1024), beanInfo.getBeanDescriptor().getBeanClass().getName()); //$NON-NLS-1$
		bean.setLocation(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		placeElementOnTop(bean);
		return bean;
	}

	public static Field createField(ISupportFormElements parent, IDataProvider dataProvider, Point location) throws RepositoryException
	{
		Field field = parent.createNewField(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));

		field.setDataProviderID(dataProvider.getDataProviderID());

		field.setEditable(dataProvider.isEditable());
		if (dataProvider instanceof Column)
		{
			ColumnInfo ci = ((Column)dataProvider).getColumnInfo();
			if (ci != null && ci.getTitleText() != null && ci.getTitleText().length() > 0)
			{
				field.setText(ci.getTitleText());
			}
		}
		int type = dataProvider.getDataProviderType();
		if (type == IColumnTypes.MEDIA)
		{
			field.setDisplayType(Field.IMAGE_MEDIA);
		}
		else if (type == IColumnTypes.DATETIME)
		{
			field.setDisplayType(Field.CALENDAR);

		}
		placeElementOnTop(field);
		return field;
	}

	public static IPersist[] createFields(ISupportFormElements parent, Object[] dataProviders, boolean placeAsLabels, boolean placeWithLabels,
		boolean placeHorizontal, boolean fillText, boolean fillName, IFieldPositioner fieldPositioner, Point location) throws RepositoryException
	{
		Point startLocation = location;
		if (fieldPositioner != null)
		{
			startLocation = fieldPositioner.getNextLocation(startLocation);
		}
		if (startLocation == null)
		{
			startLocation = new Point(0, 0);
		}

		Point loc = null;
		List<IPersist> lst = new ArrayList<IPersist>();
		Dimension lastSize = null;
		for (Object o : dataProviders)
		{
			if (o instanceof IDataProvider)
			{
				IDataProvider dataProvider = (IDataProvider)o;

				if (loc == null)
				{
					loc = startLocation;
				}
				else if (placeHorizontal)
				{
					int offset = lastSize == null || lastSize.width < 80 ? 90 : lastSize.width + 10; // do not overlap large fields, leave 10 px space
					loc = new Point(loc.x + offset, startLocation.y);
				}
				else
				{
					loc = new Point(startLocation.x, loc.y + 30);
				}
				if (fieldPositioner != null)
				{
					// snap to grid
					loc = fieldPositioner.getNextLocation(loc);
				}

				String cutofDPID = dataProvider.getDataProviderID();
				int indx = cutofDPID.lastIndexOf('.');
				if (indx != -1)
				{
					cutofDPID = cutofDPID.substring(indx + 1);
				}
				String labelText = Utils.stringInitCap(Utils.stringReplace(cutofDPID, "_", " ")); //$NON-NLS-1$ //$NON-NLS-2$
				if (dataProvider.getColumnWrapper() != null)
				{
					IColumn c = dataProvider.getColumnWrapper().getColumn();
					if (c instanceof Column)
					{
						ColumnInfo ci = ((Column)c).getColumnInfo();
						if (ci != null && ci.getTitleText() != null && ci.getTitleText().length() > 0)
						{
							labelText = ci.getTitleText();
						}
					}
				}
				String name = getCorrectName(parent, cutofDPID);
				if (placeWithLabels)
				{
					GraphicalComponent label = createLabel(parent, labelText, loc);
					lst.add(label);

					java.awt.Dimension labeldim = label.getSize();
					labeldim.width = 80;
					label.setSize(labeldim);

					if (fillName) label.setLabelFor(name);

					if (placeHorizontal)
					{
						loc = new Point(loc.x, loc.y + 30);
					}
					else
					{
						loc = new Point(loc.x + 90, loc.y);
					}
					if (fieldPositioner != null)
					{
						// snap to grid
						loc = fieldPositioner.getNextLocation(loc);
					}
				}

				BaseComponent bc;
				if (placeAsLabels)
				{
					bc = createLabel(parent, dataProvider, null, loc);
				}
				else
				{
					bc = createField(parent, dataProvider, loc);

				}
				lst.add(bc);
				if (fillName)
				{
					bc.setName(name);
				}
				if (fillText && bc instanceof ISupportText)
				{
					((ISupportText)bc).setText(labelText);
				}
				lastSize = bc.getSize();
			}
		}

		if (lst.size() == 0)
		{
			return null;
		}
		return lst.toArray(new IPersist[lst.size()]);
	}

	private static String getCorrectName(ISupportFormElements parent, String initialName)
	{
		if (initialName == null) return null;
		String name = initialName;
		int i = 1;
		boolean exists = true;
		while (exists)
		{
			exists = false;
			Iterator<IPersist> it = parent.getAllObjects();
			while (it.hasNext())
			{
				IPersist persist = it.next();
				if (persist instanceof IFormElement && name.equalsIgnoreCase(((IFormElement)persist).getName()))
				{
					exists = true;
					name = initialName + i;
					i++;
					break;
				}
			}
		}
		return name;
	}

	public static IPersist[] createTabs(ISupportChilds parent, Object[] relatedForms, Point location, int tabOrientation) throws RepositoryException
	{
		TabPanel tabPanel;
		List<IPersist> tabs = null;
		if (parent instanceof Form)
		{
			tabPanel = ((Form)parent).createNewTabPanel("tabs_" + (location == null ? 0 : location.y));
			tabPanel.setPrintable(false);
			Dimension tabsdim = new Dimension((relatedForms.length * 80) + 200, 150);
			tabPanel.setSize(tabsdim);
			tabPanel.setTabOrientation(tabOrientation);
			tabPanel.setLocation(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
			placeElementOnTop(tabPanel);
		}
		else if (parent instanceof TabPanel)
		{
			tabPanel = (TabPanel)parent;
			// collect created tabs for undo
			tabs = new ArrayList<IPersist>(relatedForms.length);
		}
		else
		{
			return null;
		}
		Dimension maxDimension = null;
		java.awt.Point tabspos = tabPanel.getLocation();
		tabspos = new java.awt.Point(tabspos.x, tabspos.y + 30);
		for (Object element : relatedForms)
		{
			if (element instanceof RelatedForm)
			{
				RelatedForm rf = (RelatedForm)element;
				try
				{
					Tab tab = tabPanel.createNewTab(rf.form.getName(), Utils.stringJoin(rf.relations, '.'), rf.form);
					tab.setLocation(tabspos);
					tabspos = new java.awt.Point(tabspos.x + 80, tabspos.y);
					tab.setSize(new java.awt.Dimension(80, 20));
					if (tabs != null)
					{
						tabs.add(tab);
					}
					if (maxDimension == null) maxDimension = rf.form.getSize();
					else if (maxDimension.width < rf.form.getSize().width && maxDimension.height < rf.form.getSize().height) maxDimension = rf.form.getSize();

				}
				catch (Exception e)
				{
					ServoyLog.logError("Cannot create tab based on form " + rf.form.getName(), e);
				}
			}
		}
		if (maxDimension != null && parent instanceof Form)
		{
			// set size of the maximum dimension form
			tabPanel.setSize(new Dimension(maxDimension.width + 4, maxDimension.height + 4));
		}
		if (tabs == null || tabs.size() == 0)
		{
			// added tabpanel to form, undo removes entire tab
			return new IPersist[] { tabPanel };
		}

		// added tabs to existing tab, undo removes these tabs
		return tabs.toArray(new IPersist[tabs.size()]);
	}


	public static IPersist createPortal(Form form, Object[] dataProviders, boolean fillText, boolean fillName, Point location) throws RepositoryException
	{
		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);
		Portal portal = null;
		if (dataProviders.length > 0)
		{
			Relation[] relations = ((IDataProvider)dataProviders[0]).getColumnWrapper().getRelations();
			if (relations == null)
			{
				return null;
			}

			int x = location == null ? 0 : location.x;
			int y = location == null ? 0 : location.y;

			StringBuffer relationName = new StringBuffer();
			StringBuffer portalName = new StringBuffer("portal");
			for (int i = 0; i < relations.length; i++)
			{
				if (i > 0) relationName.append('.');
				relationName.append(relations[i].getName());
				portalName.append('_');
				portalName.append(relations[i].getName());
			}
			portalName.append('_').append(y);

			portal = form.createNewPortal(portalName.toString(), new java.awt.Point(x, y));
			Dimension portaldim = new Dimension(dataProviders.length * 80, 50);
			portal.setSize(portaldim);

			portal.setRelationName(relationName.toString());

			for (int r = 0; r < dataProviders.length; r++)
			{
				if (dataProviders[r] == null) continue;

				String dataProviderID = ((IDataProvider)dataProviders[r]).getDataProviderID();

				IDataProvider dp = flattenedSolution.getDataproviderLookup(null, form).getDataProvider(dataProviderID);
				java.awt.Point fieldpos = new java.awt.Point(x + (r * 80), y);
				Field field = portal.createNewField(fieldpos);
				field.setEditable(dp.isEditable());
				field.setBorderType(ComponentFactoryHelper.createBorderString(BorderFactory.createEmptyBorder()));
				field.setDataProviderID(dataProviderID);
				String cutofDPID = dataProviderID;
				int indx = cutofDPID.lastIndexOf('.');
				if (indx != -1)
				{
					cutofDPID = cutofDPID.substring(indx + 1);
				}
				if (fillName) field.setName(getCorrectName(form, portalName + "_" + cutofDPID)); //$NON-NLS-1$
				if (fillText) field.setText(cutofDPID);
			}
			placeElementOnTop(portal);
		}
		return portal;
	}

	public static String createTemplateContent(EclipseRepository repository, Form form, List<IPersist> persists) throws JSONException, RepositoryException
	{
		java.awt.Point location = Utils.getBounds(persists.iterator()).getLocation();

		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);

		JSONObject json = new JSONObject();
		json.put(Template.PROP_FORM,
			cleanTemplateElement(repository, flattenedSolution, form, SolutionSerializer.generateJSONObject(form, false, repository), null));
		json.put(Template.PROP_LOCATION, PersistHelper.createPointString(location));
		JSONArray elements = new JSONArray();

		// sort the elements by location to reduce synchronization conflicts
		IPersist[] array = persists.toArray(new IPersist[persists.size()]);
		Arrays.sort(array, PositionComparator.YX_PERSIST_COMPARATOR);

		for (IPersist persist : array)
		{
			ServoyJSONObject object = SolutionSerializer.generateJSONObject(persist, true, repository);
			elements.put(cleanTemplateElement(repository, flattenedSolution, form, object, location));
		}
		json.put(Template.PROP_ELEMENTS, elements);
		return ServoyJSONObject.toString(json, false, true, false);
	}

	static JSONObject cleanTemplateElement(EclipseRepository repository, FlattenedSolution flattenedSolution, Form form, JSONObject object, java.awt.Point base)
		throws JSONException, RepositoryException
	{
		// remove some properties
		object.remove(SolutionSerializer.PROP_UUID);

		// adjust location, make relative to base
		if (base != null && object.has("location")) //$NON-NLS-1$
		{
			java.awt.Point location = PersistHelper.createPoint((String)object.opt("location")); //$NON-NLS-1$
			object.put("location", PersistHelper.createPointString(new java.awt.Point(location.x - base.x, location.y - base.y))); //$NON-NLS-1$
		}

		Iterator<String> keys = object.keys();
		while (keys.hasNext())
		{
			String key = keys.next();
			if (BaseComponent.isCommandProperty(key) || BaseComponent.isEventProperty(key))
			{
				// replace method references with their string name to be resolved when template is applied
				UUID uuid;
				try
				{
					uuid = UUID.fromString(object.getString(key));
				}
				catch (IllegalArgumentException e)
				{
					// not a uuid
					continue;
				}

				int methodId = repository.getElementIdForUUID(uuid);
				if (methodId > 0)
				{
					IScriptProvider scriptMethod = CoreUtils.getScriptMethod(form, null, null, methodId);
					if (scriptMethod != null)
					{
						object.put(key, scriptMethod.getParent() instanceof IRootObject ? ScriptVariable.GLOBAL_DOT_PREFIX + scriptMethod.getDisplayName()
							: scriptMethod.getDisplayName());
					}
				}
			}
			else if (key.endsWith("FormID")) //$NON-NLS-1$
			{
				// replace form references with their string name to be resolved when template is applied
				UUID uuid;
				try
				{
					uuid = UUID.fromString(object.getString(key));
				}
				catch (IllegalArgumentException e)
				{
					// not a uuid
					continue;
				}
				IPersist persist = flattenedSolution.searchPersist(uuid);
				if (persist instanceof ISupportName)
				{
					object.put(key, ((ISupportName)persist).getName());
				}
			}
			else if (SolutionSerializer.PROP_ITEMS.equals(key))
			{
				// recursively clean items
				Object items = object.get(key);
				if (items instanceof JSONArray)
				{
					JSONArray array = ((JSONArray)items);
					for (int i = 0; i < array.length(); i++)
					{
						array.put(i, cleanTemplateElement(repository, flattenedSolution, form, array.getJSONObject(i), base));
					}
				}
			}
		}

		return object;
	}

	public static IPersist[] applyTemplate(ISupportFormElements parent, Template template, Point location, boolean setFormProperties)
		throws RepositoryException
	{
		IDeveloperRepository repository = (IDeveloperRepository)parent.getRootObject().getRepository();
		Map<IPersist, String> persists = new HashMap<IPersist, String>(); // created persist -> name
		try
		{
			JSONObject json = new ServoyJSONObject(template.getContent(), false);

			// location
			final java.awt.Point awtLocation;
			if (location == null)
			{
				if (json.has(Template.PROP_LOCATION))
				{
					awtLocation = PersistHelper.createPoint((String)json.get(Template.PROP_LOCATION));
				}
				else
				{
					awtLocation = null;
				}
			}
			else
			{
				awtLocation = new java.awt.Point(location.x, location.y);
			}

			// elements
			JSONArray elements = (JSONArray)json.opt(Template.PROP_ELEMENTS);
			for (int i = 0; elements != null && i < elements.length(); i++)
			{
				JSONObject object = elements.getJSONObject(i);

				if (!setFormProperties && object.has(SolutionSerializer.PROP_TYPEID) && object.getInt(SolutionSerializer.PROP_TYPEID) == IRepository.PARTS)
				{
					// do not add parts in the template to an existing form
					continue;
				}

				String name = null;
				if (object.has(SolutionSerializer.PROP_NAME))
				{
					name = (String)object.remove(SolutionSerializer.PROP_NAME);
				}
				Map<IPersist, JSONObject> persist_json_map = new HashMap<IPersist, JSONObject>();
				IPersist persist = SolutionDeserializer.deserializePersist(repository, parent, persist_json_map, object, null, null, null, false);
				for (Map.Entry<IPersist, JSONObject> entry : persist_json_map.entrySet())
				{
					SolutionDeserializer.updatePersistWithValues(repository, entry.getKey(), resolveCleanedProperties(parent, entry.getValue()));
				}
				persists.put(persist, name);
			}

			// set the name property, find which number can be postfixed to allow all names in the set
			int n;
			boolean ok = false;
			for (n = 0; !ok && n <= 100; n++)
			{
				ok = true;
				for (Entry<IPersist, String> entry : persists.entrySet())
				{
					String name = entry.getValue();
					if (name != null)
					{
						try
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(n == 0 ? name : (name + n), -1,
								new ValidatorSearchContext(parent, IRepository.ELEMENTS), false);
						}
						catch (RepositoryException e)
						{
							ok = false;
							break;
						}
					}
				}
			}
			n--; // got 1 after valid n

			// set the names and the relative location
			for (Entry<IPersist, String> entry : persists.entrySet())
			{
				String name = entry.getValue();
				IPersist persist = entry.getKey();
				if (name != null && persist instanceof ISupportUpdateableName)
				{
					((ISupportUpdateableName)persist).updateName(DummyValidator.INSTANCE, n == 0 ? name : (name + n));
				}
				if (awtLocation != null)
				{
					persist.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof ISupportBounds)
							{
								java.awt.Point ploc = ((ISupportBounds)o).getLocation();
								((ISupportBounds)o).setLocation(new java.awt.Point(awtLocation.x + ploc.x, awtLocation.y + ploc.y));
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
			}

			if (setFormProperties)
			{
				// setup entire form
				if (parent instanceof Form && json.has(Template.PROP_FORM))
				{
					JSONObject formObject = (JSONObject)json.opt(Template.PROP_FORM);
					formObject.remove(SolutionSerializer.PROP_NAME);
					SolutionDeserializer.updatePersistWithValues(repository, parent, resolveCleanedProperties(parent, formObject));
				}
			}
			else
			{ // add elements to existing form
				if (persists.size() > 1) // group the elements if there is more than 1 and we are not setting up an entire form
				{
					// clear the groupID first, to make it not clash with the current template
					for (IPersist persist : persists.keySet())
					{
						if (persist instanceof IFormElement)
						{
							((IFormElement)persist).setGroupID(null);
						}
					}

					// determine a group name (do this after elements are placed in case they are named the same as the template)
					String groupName = n == 0 ? template.getName() : (template.getName() + n);
					for (int i = n; i <= 100; i++)
					{
						try
						{
							ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(groupName, -1,
								new ValidatorSearchContext(parent, IRepository.ELEMENTS), false);
							// groupName is ok
							break;
						}
						catch (RepositoryException e)
						{
						}
						groupName = template.getName() + (i + 1);
					}

					// set the group id
					for (IPersist persist : persists.keySet())
					{
						if (persist instanceof IFormElement)
						{
							((IFormElement)persist).setGroupID(groupName);
						}
					}
				}
			}
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}

		return persists.keySet().toArray(new IPersist[persists.size()]);
	}

	/**
	 * Resolve method names to UUIDs in json object.
	 * 
	 * @param parent
	 * @param object
	 * @return
	 * @throws RepositoryException
	 * @throws JSONException
	 */
	static JSONObject resolveCleanedProperties(ISupportFormElements parent, JSONObject object) throws RepositoryException, JSONException
	{
		// replace method names with their UUIDs
		Form form = (Form)parent.getAncestor(IRepository.FORMS);
		FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form);
		Form flattenedForm = flattenedSolution.getFlattenedForm(form);
		Iterator<String> keys = object.keys();
		while (keys.hasNext())
		{
			String key = keys.next();
			if (BaseComponent.isCommandProperty(key) || BaseComponent.isEventProperty(key))
			{
				String name = object.getString(key);
				ScriptMethod scriptMethod = null;
				if (name.startsWith(ScriptVariable.GLOBAL_DOT_PREFIX))
				{
					scriptMethod = flattenedSolution.getScriptMethod(name.substring(ScriptVariable.GLOBAL_DOT_PREFIX.length()));
				}
				else if (!name.equals("-1") && !name.equals("0")) //$NON-NLS-1$ //$NON-NLS-2$
				{
					scriptMethod = flattenedForm.getScriptMethod(name);
				}
				if (scriptMethod != null)
				{
					object.put(key, scriptMethod.getUUID().toString());
				}
			}
			else if (key.endsWith("FormID")) //$NON-NLS-1$
			{
				// find form from name replace form references with their string name to be resolved when template is applied
				String formName = object.getString(key);
				Form referredForm = flattenedSolution.getForm(formName);
				if (referredForm != null)
				{
					object.put(key, referredForm.getUUID().toString());
				}
			}
			else if (SolutionSerializer.PROP_ITEMS.equals(key))
			{
				// recursively resolve items
				Object items = object.get(key);
				if (items instanceof JSONArray)
				{
					JSONArray array = ((JSONArray)items);
					for (int i = 0; i < array.length(); i++)
					{
						array.put(i, resolveCleanedProperties(parent, array.getJSONObject(i)));
					}
				}
			}
		}

		return object;
	}

	/**
	 * Container for selected relation sequence (optional) and form.
	 * 
	 * @author rgansevles
	 * 
	 */
	public static class RelatedForm
	{
		public final Relation[] relations;
		public final Form form;

		public RelatedForm(Relation[] relations, Form form)
		{
			this.form = form;
			this.relations = relations;
		}

		@Override
		public int hashCode()
		{
			final int prime = 31;
			int result = 1;
			result = prime * result + ((form == null) ? 0 : form.hashCode());
			result = prime * result + Arrays.hashCode(relations);
			return result;
		}

		@Override
		public boolean equals(Object obj)
		{
			if (this == obj) return true;
			if (obj == null) return false;
			if (getClass() != obj.getClass()) return false;
			RelatedForm other = (RelatedForm)obj;
			if (form == null)
			{
				if (other.form != null) return false;
			}
			else if (!form.equals(other.form)) return false;
			if (!Arrays.equals(relations, other.relations)) return false;
			return true;
		}

	}

}
