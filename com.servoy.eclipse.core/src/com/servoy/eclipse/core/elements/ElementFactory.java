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

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Rectangle;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;
import java.util.Set;

import javax.swing.ImageIcon;
import javax.swing.border.Border;

import org.eclipse.swt.graphics.Point;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.DesignComponentFactory;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.debug.DebugUtils;
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
import com.servoy.j2db.persistence.IAnchorConstants;
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
import com.servoy.j2db.persistence.ISupportSize;
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
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.StringResource;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Template;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.headlessclient.dataui.WebDefaultRecordNavigator;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.IStyleRule;
import com.servoy.j2db.util.IStyleSheet;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Class with static methods for creating elements in Servoy Developer.
 * 
 * @author rgansevles
 *
 */
public class ElementFactory
{
	public static final String NAME_HINT_PROPERTY = "ElementFactory:nameHint";

	private static Random random = new Random();

	public static GraphicalComponent createLabel(ISupportFormElements parent, String text, Point location) throws RepositoryException
	{
		GraphicalComponent label = parent.createNewGraphicalComponent(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		label.setText(text);
		label.setTransparent(true);
		placeElementOnTop(label);
		return label;
	}

	public static GraphicalComponent createButton(ISupportFormElements parent, ScriptMethod method, String text, Point location) throws RepositoryException
	{
		GraphicalComponent button = parent.createNewGraphicalComponent(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		button.setOnActionMethodID(method == null ? -1 : method.getID());
		button.setOnDoubleClickMethodID(-1);
		button.setOnRightClickMethodID(-1);
		button.setText(text);
		placeElementOnTop(button);
		return button;
	}

	/**
	 * @return list items [button, subtext, countBubble, image]
	 */
	public static IPersist[] addFormListItems(Form form, Portal portal /* for inset list */, Point location) throws RepositoryException
	{
		int x = 0;
		int y = 0;
		if (location != null)
		{
			x = location.x;
			y = location.y;
		}
		ISupportFormElements parent = portal == null ? form : portal;

		// image
		Field image = createField(parent, null, new Point(x + 0, y + 10));
		image.putCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName, Boolean.TRUE);
		image.setEditable(false); // for debug in developer

		// button
		GraphicalComponent button = createButton(parent, null, "list", new Point(x + 10, y + 20));
		button.putCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName, Boolean.TRUE);

		// subtext
		GraphicalComponent subtext = createLabel(parent, null, new Point(x + 20, y + 30));
		subtext.putCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName, Boolean.TRUE);

		// countBubble
		Field countBubble = createField(parent, null, new Point(x + 40, y + 40));
		countBubble.putCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName, Boolean.TRUE);
		countBubble.setEditable(false); // for debug in developer

		return new IPersist[] { button, subtext, countBubble, image };
	}

	public static Pair<Field, GraphicalComponent> createMobileFieldWithTitle(Form form, Point location) throws RepositoryException
	{
		// create a label and a text field in a group
		String groupID = UUID.randomUUID().toString();
		Point loc = location == null ? new Point(0, 0) : location;
		GraphicalComponent label = createLabel(form, "Title", loc);
		label.setGroupID(groupID);
		label.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);
		label.putCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName, Boolean.TRUE);
		Field field = createField(form, null, new Point(loc.x, loc.y + 1)); // enforce order by y-pos
		field.setGroupID(groupID);
		field.setAnchors(IAnchorConstants.EAST | IAnchorConstants.WEST | IAnchorConstants.NORTH);

		return new Pair<Field, GraphicalComponent>(field, label);
	}

	public static void placeElementOnTop(IFormElement element)
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
		AbstractBase copy = (AbstractBase)component.cloneObj(parent, false, validator, true, true, true /* when component is an override we want a flattened one */);
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
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(parent);
		ImageIcon ii = ImageLoader.getIcon(ComponentFactory.loadIcon(flattenedSolution, new Integer(media.getID())), -1, -1, true);
		if (ii == null) return null;

		GraphicalComponent label = parent.createNewGraphicalComponent(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));
		label.setText(""); //$NON-NLS-1$
		label.setImageMediaID(media.getID());
		label.setTransparent(ImageLoader.imageHasAlpha(ii.getImage(), 0));
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

	public static IPersist createBean(Form form, String beanClassName, Point location) throws RepositoryException
	{
		Bean bean = form.createNewBean("bean_" + random.nextInt(1024), beanClassName); //$NON-NLS-1$
		bean.setLocation(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));

		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		Object beanInstance = DesignComponentFactory.getBeanDesignInstance(Activator.getDefault().getDesignClient(), flattenedSolution, bean, form);
		// check preferredSize and minimumSize
		Dimension preferredSize = getBeanPrefferredSize(beanInstance);
		if (preferredSize != null)
		{
			bean.setSize(preferredSize);
		}

		placeElementOnTop(bean);
		return bean;
	}

	public static Dimension getBeanPrefferredSize(Object beanInstance)
	{
		try
		{
			if (beanInstance instanceof Component)
			{
				Dimension initSize = ((Component)beanInstance).getPreferredSize();
				if (initSize == null)
				{
					initSize = ((Component)beanInstance).getMinimumSize();
				}
				if (initSize != null && initSize.width > 0 && initSize.height > 0)
				{
					return initSize;
				}
			}
		}
		catch (Exception e)
		{
			Debug.error("Could not get preferred size from bean instance " + beanInstance.getClass().getName(), e);
		}

		return null;
	}

	public static Border getFormBorder(IServiceProvider sp, final Form form)
	{
		if (form == null)
		{
			return null;
		}
		Border border = null;
		//TODO: find better way to handle this for MAC running with java 1.7
		if (Utils.isAppleMacOS() && System.getProperty("java.version").startsWith("1.7")) //$NON-NLS-1$//$NON-NLS-2$
		{
			final Border[] fBorder = { null };
			try
			{
				DebugUtils.invokeAndWaitWhileDispatchingOnSWT(new Runnable()
				{
					public void run()
					{
						fBorder[0] = ComponentFactoryHelper.createBorder(form.getBorderType(), true);
					}
				});
			}
			catch (Exception e)
			{
				ServoyLog.logError("Error creating border from " + form.getBorderType(), e);
			}
			border = fBorder[0];
		}
		else
		{
			border = ComponentFactoryHelper.createBorder(form.getBorderType(), true);
		}

		if (border != null)
		{
			return border;
		}

		// Look into styles.
		Pair<IStyleSheet, IStyleRule> style = ComponentFactory.getCSSPairStyleForForm(sp, form);
		if (style != null && style.getLeft() != null && style.getRight() != null)
		{
			return style.getLeft().getBorder(style.getRight());
		}

		return null;
	}

	public static Field createField(ISupportFormElements parent, IDataProvider dataProvider, Point location) throws RepositoryException
	{
		Field field = parent.createNewField(new java.awt.Point(location == null ? 0 : location.x, location == null ? 0 : location.y));

		if (dataProvider != null)
		{
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

			// use dataprovider type as defined by converter
			ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, dataProvider, Activator.getDefault().getDesignClient());
			if (componentFormat.dpType == IColumnTypes.MEDIA)
			{
				field.setDisplayType(Field.IMAGE_MEDIA);
			}
			else if (componentFormat.dpType == IColumnTypes.DATETIME)
			{
				field.setDisplayType(Field.CALENDAR);
			}
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

		if (dataProviders == null)
		{
			return new IPersist[] { placeAsLabels ? createLabel(parent, null, startLocation) : createField(parent, null, startLocation) };
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
					int offset = lastSize == null || lastSize.width < 80 ? 80 : lastSize.width;
					if (!placeWithLabels)
					{
						// do not overlap large fields, leave 10 px space
						offset += 10;
					}
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
				if (cutofDPID != null)
				{
					int indx = cutofDPID.lastIndexOf('.');
					if (indx != -1)
					{
						cutofDPID = cutofDPID.substring(indx + 1);
					}
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
					labeldim.width = placeHorizontal ? 140 /* field width */: 80;
					label.setSize(labeldim);

					if (fillName)
					{
						label.setLabelFor(name);
						label.setName(name + "_label"); //$NON-NLS-1$
					}

					if (placeHorizontal)
					{
						loc = new Point(loc.x, loc.y + 20);
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
					bc = createLabel(parent, null, loc);
					((GraphicalComponent)bc).setDataProviderID(dataProvider.getDataProviderID());
				}
				else
				{
					bc = createField(parent, dataProvider, loc);
				}
				java.awt.Dimension dim = bc.getSize();
				dim.width = 140;
				bc.setSize(dim);
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

	public static IPersist[] createTabs(IApplication application, ISupportChilds parent, Object[] relatedForms, Point location, int tabOrientation,
		String nameHint) throws RepositoryException
	{
		TabPanel tabPanel;
		List<IPersist> tabs = null;
		if (parent instanceof Form)
		{
			int counter = 0;
			String tabpanelNameHint = nameHint == null ? "tabs" : nameHint;
			String tabpanelName = null;
			while (true)
			{
				Iterator<IFormElement> it = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(parent).getFlattenedForm(
					parent).getFormElementsSortedByFormIndex();
				tabpanelName = tabpanelNameHint + (counter == 0 ? "" : ("_" + counter));
				boolean duplicate = false;
				while (it.hasNext() && !duplicate)
				{
					if (Utils.stringSafeEquals(it.next().getName(), tabpanelName))
					{
						duplicate = true;
					}
				}
				if (duplicate) counter++;
				else break;
			}

			tabPanel = ((Form)parent).createNewTabPanel(tabpanelName);
			tabPanel.setTransparent(true);
			tabPanel.setPrintable(false);
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
		java.awt.Point tabspos;
		// look at the positions of the existing tabls to add more
		Iterator<IPersist> existingTabs = tabPanel.getTabs();
		java.awt.Point loc1 = null;
		java.awt.Point loc2 = null;
		while (existingTabs.hasNext())
		{
			loc1 = loc2;
			loc2 = ((Tab)existingTabs.next()).getLocation();
		}
		if (loc2 == null)
		{
			// no other tabs yet
			java.awt.Point tabloc = tabPanel.getLocation();
			tabspos = new java.awt.Point(tabloc.x - 80, tabloc.y + 30);
		}
		else
		{
			if (loc1 == null)
			{
				// only 1 tab, start from that one
				tabspos = loc2;
			}
			else
			{
				// follow the line of the last 2 existing tabs
				tabspos = new java.awt.Point(2 * loc2.x - loc1.x - 80, 2 * loc2.y - loc1.y);
			}
		}

		if (relatedForms != null)
		{
			for (Object element : relatedForms)
			{
				if (element instanceof RelatedForm)
				{
					RelatedForm rf = (RelatedForm)element;
					try
					{
						tabspos = new java.awt.Point(tabspos.x + 80, tabspos.y);
						Tab tab = tabPanel.createNewTab(rf.form.getName(), Utils.stringJoin(rf.relations, '.'), rf.form);
						tab.setLocation(tabspos);
						tab.setSize(new java.awt.Dimension(80, 20));
						if (tabs != null)
						{
							tabs.add(tab);
						}
						Dimension formSize = calculateFormSize(application, rf.form);
						if (maxDimension == null) maxDimension = formSize;
						else if (maxDimension.width < formSize.width && maxDimension.height < formSize.height) maxDimension = formSize;

					}
					catch (Exception e)
					{
						ServoyLog.logError("Cannot create tab based on form " + rf.form.getName(), e);
					}
				}
			}
		}
		if (parent instanceof Form)
		{
			// set size of the maximum dimension form
			tabPanel.setSize(maxDimension == null ? new Dimension(280, 150) : new Dimension(maxDimension.width, maxDimension.height));
		}
		if (tabs == null || tabs.size() == 0)
		{
			// added tabpanel to form, undo removes entire tab
			return new IPersist[] { tabPanel };
		}

		// added tabs to existing tab, undo removes these tabs
		return tabs.toArray(new IPersist[tabs.size()]);
	}

	/**
	 * Calculate form size, take border and navigator into account.
	 * 
	 * @param form
	 * @return
	 */
	public static Dimension calculateFormSize(IApplication application, Form form)
	{
		return calculateFormSize(application, form, new HashSet<Form>());
	}

	private static Dimension calculateFormSize(IApplication application, Form form, Set<Form> processed)
	{
		if (form == null || !processed.add(form))
		{
			return null;
		}

		// include border size
		final Border border = getFormBorder(application, form);
		Insets borderInsets;
		if (border != null)
		{
			//TODO: find better way to handle this for MAC running with java 1.7
			if (Utils.isAppleMacOS() && System.getProperty("java.version").startsWith("1.7")) //$NON-NLS-1$//$NON-NLS-2$
			{
				final java.awt.Insets[] fInset = { null };
				try
				{
					DebugUtils.invokeAndWaitWhileDispatchingOnSWT(new Runnable()
					{
						public void run()
						{
							fInset[0] = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
						}
					});
				}
				catch (Exception e)
				{
					ServoyLog.logError("Error getting border insets for border  " + border, e);
				}
				borderInsets = fInset[0];
			}
			else
			{
				borderInsets = ComponentFactoryHelper.getBorderInsetsForNoComponent(border);
			}
		}
		else
		{
			borderInsets = new Insets(2, 2, 2, 2);
		}
		Dimension formSize = form.getSize();
		int width = formSize.width + borderInsets.left + borderInsets.right;
		int height = formSize.height + borderInsets.top + borderInsets.bottom;

		// include navigator size
		Dimension navigatorSize = null;
		int navigatorID = form.getNavigatorID();
		if (navigatorID == Form.NAVIGATOR_DEFAULT && form.getView() != FormController.TABLE_VIEW && form.getView() != FormController.LOCKED_TABLE_VIEW)
		{
			navigatorSize = new Dimension(WebDefaultRecordNavigator.DEFAULT_WIDTH, WebDefaultRecordNavigator.DEFAULT_HEIGHT_WEB);
		}
		else if (navigatorID != Form.NAVIGATOR_NONE)
		{
			navigatorSize = calculateFormSize(application,
				ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(form).getForm(navigatorID), processed);
		}

		if (navigatorSize != null)
		{
			width += navigatorSize.width;
			if (height < navigatorSize.height) height = navigatorSize.height;
		}

		return new Dimension(width, height);
	}

	public static Portal createPortal(Form form, Object[] dataProviders, boolean fillText, boolean fillName, boolean placeAsLabels, boolean placeWithLabels,
		Point location, String prefix) throws RepositoryException
	{
		Relation[] relations = dataProviders == null || dataProviders.length == 0 ? null : ((IDataProvider)dataProviders[0]).getColumnWrapper().getRelations();

		int x = location == null ? 0 : location.x;
		int y = location == null ? 0 : location.y;

		StringBuilder relationName = new StringBuilder();
		StringBuilder portalName = new StringBuilder(prefix);
		for (int i = 0; relations != null && i < relations.length; i++)
		{
			if (i > 0) relationName.append('.');
			relationName.append(relations[i].getName());
			portalName.append('_');
			portalName.append(relations[i].getName());
		}
		portalName.append('_').append(y);

		Portal portal = form.createNewPortal(portalName.toString(), new java.awt.Point(x, y));
		if (dataProviders != null && dataProviders.length > 0)
		{
			Dimension portaldim = new Dimension(dataProviders.length * 140, 50);
			portal.setSize(portaldim);

			portal.setRelationName(relationName.toString());

			createFields(portal, dataProviders, placeAsLabels, placeWithLabels, true, fillText, fillName, null, new Point(x, y));
		}
		placeElementOnTop(portal);
		return portal;
	}

	public static String createTemplateContent(EclipseRepository repository, Form form, List<IPersist> persists, int templateType, boolean groupingState)
		throws JSONException, RepositoryException
	{
		java.awt.Point location = Utils.getBounds(persists.iterator()).getLocation();

		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);

		JSONObject json = new JSONObject();
		if (templateType == StringResource.FORM_TEMPLATE)
		{
			json.put(
				Template.PROP_FORM,
				cleanTemplateElement(repository, flattenedSolution, form, SolutionSerializer.generateJSONObject(form, false, false, repository, false, null),
					null));
		}
		json.put(Template.PROP_LOCATION, PersistHelper.createPointString(location));
		JSONArray elements = new JSONArray();

		// sort the elements by location to reduce synchronization conflicts
		IPersist[] array = persists.toArray(new IPersist[persists.size()]);
		Arrays.sort(array, PositionComparator.YX_PERSIST_COMPARATOR);

		for (IPersist persist : array)
		{
			ServoyJSONObject object = SolutionSerializer.generateJSONObject(persist, true, true, repository, false, null);
			if (persist instanceof ISupportSize) // some objects have default size programmed in the getter
			{
				object.put(Template.PROP_SIZE, repository.convertObjectToArgumentString(IRepository.DIMENSION, ((ISupportSize)persist).getSize()));
			}
			elements.put(cleanTemplateElement(repository, flattenedSolution, form, object, location));
		}
		json.put(Template.PROP_ELEMENTS, elements);

		json.put(Template.PROP_GROUPING, groupingState);

		return ServoyJSONObject.toString(json, false, true, false);
	}

	static JSONObject cleanTemplateElement(EclipseRepository repository, FlattenedSolution flattenedSolution, Form form, JSONObject object, java.awt.Point base)
		throws JSONException, RepositoryException
	{
		// remove some properties
		object.remove(SolutionSerializer.PROP_UUID);

		int typeId = object.optInt(SolutionSerializer.PROP_TYPEID);

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
					IScriptProvider scriptMethod = ModelUtils.getScriptMethod(form, null, form.getTable(), methodId);
					if (scriptMethod != null)
					{
						object.put(key,
							scriptMethod.getParent() instanceof IRootObject ? ScopesUtils.getScopeString(scriptMethod) : scriptMethod.getDisplayName());
					}
				}
			}
			else if (key.endsWith("FormID") || (typeId == IRepository.FORMS && StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(key))) //$NON-NLS-1$
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

	public static Object[] applyTemplate(ISupportFormElements parent, TemplateElementHolder templateHolder, Point location, final boolean setFormProperties)
		throws RepositoryException
	{
		IDeveloperRepository repository = (IDeveloperRepository)parent.getRootObject().getRepository();
		final Map<IPersist, String> persists = new HashMap<IPersist, String>(); // created persist -> name
		try
		{
			JSONObject json = new ServoyJSONObject(templateHolder.template.getContent(), false);

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
				if (templateHolder.element >= 0 && templateHolder.element != i)
				{
					// only use the template item
					continue;
				}

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
					SolutionDeserializer.setPersistValues(repository, entry.getKey(),
						resolveCleanedProperties((Form)entry.getKey().getAncestor(IRepository.FORMS), entry.getValue()));
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
								int x, y;
								if (setFormProperties || persists.size() > 1)
								{
									java.awt.Point ploc = ((ISupportBounds)o).getLocation();
									x = ploc.x;
									y = ploc.y;
								}
								else
								{ // if a single element is placed, do not use its relative location
									x = y = 0;
								}
								((ISupportBounds)o).setLocation(new java.awt.Point(awtLocation.x + x, awtLocation.y + y));
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
					SolutionDeserializer.setPersistValues(repository, parent, resolveCleanedProperties((Form)parent, formObject));
				}
			}
			else
			{
				// add elements to existing form
				if (persists.size() > 1 && parent instanceof Form) // group the elements if there is more than 1 and we are not setting up an entire form
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
					String templateName = templateHolder.element < 0 ? templateHolder.template.getName() : "item";
					String groupName = n == 0 ? templateName : (templateName + n);
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
						groupName = templateName + (i + 1);
					}

					boolean grouping = false;
					if (json.has(Template.PROP_GROUPING))
					{
						grouping = json.optBoolean(Template.PROP_GROUPING);
					}
					if (!grouping)
					{
						return persists.keySet().toArray();
					}

					// set the group id
					for (IPersist persist : persists.keySet())
					{
						if (persist instanceof IFormElement)
						{
							((IFormElement)persist).setGroupID(groupName);
						}
					}

					return new Object[] { new FormElementGroup(groupName, ModelUtils.getEditingFlattenedSolution(parent), (Form)parent) };
				}
			}
		}
		catch (JSONException e)
		{
			throw new RepositoryException(e);
		}

		return persists.keySet().toArray();
	}

	public static Dimension getTemplateBoundsize(TemplateElementHolder templateHolder)
	{
		if (templateHolder == null || templateHolder.template == null)
		{
			return null;
		}

		Rectangle box = null;
		try
		{
			JSONObject json = new ServoyJSONObject(templateHolder.template.getContent(), false);

			// elements
			JSONArray elements = (JSONArray)json.opt(Template.PROP_ELEMENTS);
			for (int i = 0; elements != null && i < elements.length(); i++)
			{
				if (templateHolder.element >= 0 && i != templateHolder.element)
				{
					continue;
				}

				JSONObject object = elements.getJSONObject(i);

				if (object.has(SolutionSerializer.PROP_TYPEID) && object.getInt(SolutionSerializer.PROP_TYPEID) == IRepository.PARTS)
				{
					// ignore parts
					continue;
				}

				java.awt.Dimension size = null;
				if (object.has(Template.PROP_SIZE))
				{
					size = PersistHelper.createDimension((String)object.get(Template.PROP_SIZE));
				}
				if (size == null || size.height == 0 || size.width == 0)
				{
					continue;
				}

				java.awt.Point location = null;
				if (object.has(Template.PROP_LOCATION))
				{
					location = PersistHelper.createPoint((String)object.get(Template.PROP_LOCATION));
				}
				if (location == null)
				{
					location = new java.awt.Point(0, 0);
				}

				Rectangle rectangle = new Rectangle(location, size);
				if (box == null)
				{
					box = rectangle;
				}
				else
				{
					box.add(rectangle);
				}
			}
		}
		catch (JSONException e)
		{
			ServoyLog.logError("Error processing template " + templateHolder.template.getName(), e);
		}
		return box == null ? null : box.getSize();
	}

	public static List<JSONObject> getTemplateElements(Template template, int element)
	{
		if (template != null)
		{
			try
			{
				JSONObject json = new ServoyJSONObject(template.getContent(), false);

				// elements
				JSONArray elements = (JSONArray)json.opt(Template.PROP_ELEMENTS);
				if (elements == null)
				{
					return null;
				}
				List<JSONObject> objects = new ArrayList<JSONObject>(elements.length());
				if (element >= 0)
				{
					if (element < elements.length())
					{
						objects.add(elements.getJSONObject(element));
					}
				}
				else
				{
					for (int i = 0; i < elements.length(); i++)
					{
						objects.add(elements.getJSONObject(i));
					}
				}
				return objects;
			}
			catch (JSONException e)
			{
				ServoyLog.logError("Error processing template " + template.getName(), e);
			}
		}
		return null;
	}

	/**
	 * Resolve method names to UUIDs in json object.
	 * 
	 * @param form
	 * @param object
	 * @return
	 * @throws RepositoryException
	 * @throws JSONException
	 */
	public static JSONObject resolveCleanedProperties(Form form, JSONObject object) throws RepositoryException, JSONException
	{
		// replace method names with their UUIDs
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
		Form flattenedForm = flattenedSolution.getFlattenedForm(form);
		Iterator<String> keys = object.keys();
		while (keys.hasNext())
		{
			String key = keys.next();
			if (BaseComponent.isCommandProperty(key) || BaseComponent.isEventProperty(key))
			{
				String name = object.getString(key);
				ScriptMethod scriptMethod = null;
				if (ScopesUtils.isVariableScope(name))
				{
					scriptMethod = flattenedSolution.getScriptMethod(null, name);
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
						array.put(i, resolveCleanedProperties(form, array.getJSONObject(i)));
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
