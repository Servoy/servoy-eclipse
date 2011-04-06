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
package com.servoy.eclipse.ui.util;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.Comparator;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.ui.IDepricatedScriptTabPanelMethods;
import com.servoy.j2db.ui.IScriptCheckBoxMethods;
import com.servoy.j2db.ui.IScriptChoiceMethods;
import com.servoy.j2db.ui.IScriptDataButtonMethods;
import com.servoy.j2db.ui.IScriptDataCalendarMethods;
import com.servoy.j2db.ui.IScriptDataComboboxMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptDataPasswordMethods;
import com.servoy.j2db.ui.IScriptFieldMethods;
import com.servoy.j2db.ui.IScriptMediaInputFieldMethods;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptRadioMethods;
import com.servoy.j2db.ui.IScriptRectMethods;
import com.servoy.j2db.ui.IScriptScriptButtonMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTextAreaMethods;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.UUID;

/**
 * Utilities for elements and label providers.
 * 
 * @author rgansevles
 */

public class ElementUtil
{
	private static final class FormElementComparator implements Comparator<IFormElement>
	{
		public final static FormElementComparator INSTANCE = new FormElementComparator();

		/*
		 * (non-Javadoc)
		 * 
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(IFormElement o1, IFormElement o2)
		{
			return o1.getFormIndex() - o2.getFormIndex();
		}
	}

	public static String getPersistImageName(IPersist persist)
	{
		if (persist instanceof BaseComponent)
		{
			String lookupName = ComponentFactory.getLookupName((BaseComponent)persist);
			if ("button".equals(lookupName))
			{
				return "button.gif";
			}
			if ("check".equals(lookupName))
			{
				return "chk_on_icon.gif";
			}
			if ("combobox".equals(lookupName))
			{
				return "field.gif"; // todo: combobox.gif
			}
			if ("field".equals(lookupName))
			{
				return "field.gif";
			}
			if ("portal".equals(lookupName))
			{
				return "portal.gif";
			}
			if ("tabpanel".equals(lookupName))
			{
				return "tabs.gif";
			}
			if ("label".equals(lookupName))
			{
				return "text.gif";
			}
			if ("radio".equals(lookupName))
			{
				return "radio_on.gif";
			}
		}
		if (persist instanceof Form)
		{
			return "designer.gif";
		}
		if (persist instanceof Bean)
		{
			return "bean.gif";
		}
		if (persist instanceof RectShape)
		{
			return "rectangle.gif";
		}
		if (persist instanceof Relation)
		{
			return "relation.gif";
		}
		if (persist instanceof Media)
		{
			return "image.gif";
		}
		if (persist instanceof ValueList)
		{
			return "valuelists.gif";
		}
		if (persist instanceof ScriptCalculation)
		{
			return "columncalc.gif";
		}
		if (persist instanceof ScriptVariable)
		{
			if (persist.getParent() instanceof Form)
			{
				return "form_variable.gif";
			}
			return "global_variable.gif";
		}
		if (persist instanceof ScriptMethod)
		{
			if (persist.getParent() instanceof Form)
			{
				return "form_method.gif";
			}
			return "global_method.gif";
		}

		return null;
	}

	/**
	 * Get or create the persist object that is used to override properties in the context form.
	 * <p>
	 * When the context form is a subform of the persist's parent, use (create when non-existent) an empty
	 * clone of the persist and add it to the context form.
	 * @param context form
	 * @param persist
	 * @return
	 * @throws RepositoryException 
	 */
	public static IPersist getOverridePersist(IPersist context, IPersist persist) throws RepositoryException
	{
		IPersist ancestorForm = persist.getAncestor(IRepository.FORMS);
		if (!(persist instanceof AbstractBase)//
			||
			!(context instanceof Form) //
			|| ancestorForm == null // persist is something else, like a relation or a solution
			|| ancestorForm == context // already in same form
			|| !ModelUtils.getEditingFlattenedSolution(context).getFormHierarchy((Form)context).contains(ancestorForm)// check that the persist is in a parent form of the context
		)
		{
			// no override
			return persist;
		}

		IPersist newPersist = AbstractRepository.searchPersist((Form)context, persist.getUUID());
		if (newPersist == null)
		{
			// override does not exist yet, create it
			ISupportChilds parent = (Form)context;
			if (!(persist.getParent() instanceof Form))
			{
				parent = null;
				parent = (ISupportChilds)AbstractRepository.searchPersist((Form)context, persist.getParent().getUUID());
				if (parent == null)
				{
					parent = (ISupportChilds)((AbstractBase)persist.getParent()).cloneObj((Form)context, false, null, false, false);
					((AbstractBase)parent).resetUUID(persist.getParent().getUUID());
					((AbstractBase)parent).copyPropertiesMap(null, true);
					((AbstractBase)parent).putOverrideProperty(((Form)ancestorForm).getName());
				}
			}
			newPersist = ((AbstractBase)persist).cloneObj(parent, false, null, false, false);
			((AbstractBase)newPersist).resetUUID(persist.getUUID());
			((AbstractBase)newPersist).copyPropertiesMap(null, true);
			((AbstractBase)newPersist).putOverrideProperty(((Form)ancestorForm).getName());
		}
		return newPersist;
	}

	private static Map<String, WeakReference<Class< ? >>> beanClassCache = new ConcurrentHashMap<String, WeakReference<Class< ? >>>();

	public static Class< ? > getPersistScriptClass(IApplication application, IPersist persist)
	{
		if (persist instanceof GraphicalComponent)
		{
			GraphicalComponent label = (GraphicalComponent)persist;
			if (label.getOnActionMethodID() != 0 && label.getShowClick())
			{
				if (label.getDataProviderID() == null && !label.getDisplaysTags())
				{
					return IScriptScriptButtonMethods.class;
				}
				else
				{
					return IScriptDataButtonMethods.class;
				}
			}
			else
			{
				if (label.getDataProviderID() == null && !label.getDisplaysTags())
				{
					return IScriptScriptLabelMethods.class;
				}
				else
				{
					return IScriptDataLabelMethods.class;
				}
			}
		}
		else if (persist instanceof Field)
		{
			Field field = (Field)persist;

			switch (field.getDisplayType())
			{
				case Field.PASSWORD :
					return IScriptDataPasswordMethods.class;
				case Field.RTF_AREA :
				case Field.HTML_AREA :
					return IScriptTextEditorMethods.class;
				case Field.TEXT_AREA :
					return IScriptTextAreaMethods.class;
				case Field.CHECKS :
					if (isSingle(application, field))
					{
						return IScriptCheckBoxMethods.class;
					}
					return IScriptChoiceMethods.class;
				case Field.RADIOS :
					if (isSingle(application, field))
					{
						return IScriptRadioMethods.class;
					}
					return IScriptChoiceMethods.class;
				case Field.COMBOBOX :
					return IScriptDataComboboxMethods.class;
				case Field.CALENDAR :
					return IScriptDataCalendarMethods.class;
				case Field.IMAGE_MEDIA :
					return IScriptMediaInputFieldMethods.class;
				default :
					return IScriptFieldMethods.class;
			}

		}
		else if (persist instanceof Bean)
		{
			Bean bean = (Bean)persist;
			String beanClassName = bean.getBeanClassName();
			WeakReference<Class< ? >> beanClassRef = beanClassCache.get(beanClassName);
			Class< ? > beanClass = null;
			if (beanClassRef != null)
			{
				beanClass = beanClassRef.get();
			}
			if (beanClass == null)
			{
				ClassLoader bcl = application.getBeanManager().getClassLoader();
				try
				{
					beanClass = bcl.loadClass(beanClassName);
					if (IServoyBeanFactory.class.isAssignableFrom(beanClass))
					{
						Form form = (Form)bean.getParent();
						IServoyBeanFactory beanFactory = (IServoyBeanFactory)beanClass.newInstance();
						Object beanInstance = beanFactory.getBeanInstance(application.getApplicationType(), (IClientPluginAccess)application.getPluginAccess(),
							new Object[] { ComponentFactory.getWebID(null, bean), form.getName(), form.getStyleName() });
						beanClass = beanInstance.getClass();
						if (beanInstance instanceof IScriptObject)
						{
							ScriptObjectRegistry.registerScriptObjectForClass(beanClass, (IScriptObject)beanInstance);
						}
					}
					beanClassCache.put(beanClassName, new WeakReference<Class< ? >>(beanClass));
				}
				catch (Exception e)
				{
					Debug.error("Error loading bean: " + bean.getName() + " clz: " + beanClassName, e); //$NON-NLS-1$ //$NON-NLS-2$
				}
			}
			return beanClass;
		}
		else if (persist instanceof TabPanel)
		{
			int orient = ((TabPanel)persist).getTabOrientation();
			if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) return IScriptSplitPaneMethods.class;
			else return IDepricatedScriptTabPanelMethods.class;
		}
		else if (persist instanceof Portal)
		{
			return IScriptPortalComponentMethods.class;
		}
		else if (persist instanceof RectShape)
		{
			return IScriptRectMethods.class;
		}
		return null;

	}

	/**
	 * @param application
	 * @param field
	 * @return
	 */
	private static boolean isSingle(IApplication application, Field field)
	{
		if (field.getValuelistID() > 0)
		{
			ValueList valuelist = application.getFlattenedSolution().getValueList(field.getValuelistID());
			if (!(valuelist.getValueListType() == ValueList.DATABASE_VALUES && valuelist.getDatabaseValuesType() == ValueList.RELATED_VALUES) &&
				(valuelist.getAddEmptyValue() != ValueList.EMPTY_VALUE_ALWAYS))
			{
				IValueList realValueList = ComponentFactory.getRealValueList(application, valuelist, false, 0, null, null);
				if (realValueList != null && realValueList.getSize() == 1)
				{
					return true;
				}
			}
			return false;
		}
		else
		{
			return true;
		}
	}

	public static List<IFormElement> getAllOverlappingFormElements(Form form, IFormElement formElement)
	{
		List<IFormElement> overlapingElements = new LinkedList<IFormElement>();
		List<IFormElement> groupElements = new LinkedList<IFormElement>();
		List<UUID> uuids = new LinkedList<UUID>();

		groupElements.add(formElement);
		String groupID = (String)((BaseComponent)formElement).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
		if (groupID != null)
		{
			Iterator<IPersist> it = form.getAllObjects();
			while (it.hasNext())
			{
				IPersist element = it.next();
				if (element instanceof BaseComponent)
				{
					BaseComponent itFormElement = (BaseComponent)element;
					if (element.getUUID() == formElement.getUUID())
					{
						continue;
					}
					String elementGroupID = (String)((BaseComponent)element).getProperty(StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName());
					if (elementGroupID != null && elementGroupID.equals(groupID))
					{
						groupElements.add(itFormElement);
					}
				}
			}
		}

		for (IFormElement groupElement : groupElements)
		{
			List<IFormElement> elementList = getOverlappingFormElements(form, groupElement);
			for (IFormElement fe : elementList)
			{
				if (!uuids.contains(fe.getUUID()))
				{
					uuids.add(fe.getUUID());
					overlapingElements.add(fe);
				}
			}
		}

		return overlapingElements;
	}

	public static List<IFormElement> getOverlappingFormElements(Form form, IFormElement formElement)
	{
		List<IFormElement> overlapingElements = new LinkedList<IFormElement>();

		Dimension formElementDimension = null;
		Point formElementUpLeft = null;

		formElementDimension = formElement.getSize();
		formElementUpLeft = formElement.getLocation();

		Rectangle formElementRectangle = new Rectangle(formElementDimension);
		formElementRectangle.setLocation(formElementUpLeft);

		Iterator<IPersist> it = form.getAllObjects();
		while (it.hasNext())
		{
			IPersist element = it.next();
			if (element instanceof IFormElement)
			{
				IFormElement itFormElement = (IFormElement)element;
				if (element.getUUID() == (formElement).getUUID())
				{
					continue;
				}
				Dimension elementDimension = itFormElement.getSize();
				Point elementUpLeft = itFormElement.getLocation();

				Rectangle elementRectangle = new Rectangle(elementDimension);
				elementRectangle.setLocation(elementUpLeft);

				if (elementRectangle.intersects(formElementRectangle))
				{
					overlapingElements.add((IFormElement)element);
				}
			}
		}


		if (overlapingElements.size() != 0) return overlapingElements;

		return null;
	}

	public static IFormElement getElementWithHighestZIndex(FormElementGroup formElementgroup)
	{
		int highestFormIndex = -1000000;
		IFormElement formElement = null;
		Iterator<IFormElement> groupElementIterator = (formElementgroup).getElements();
		while (groupElementIterator.hasNext())
		{
			IFormElement fe = groupElementIterator.next();
			if (fe.getFormIndex() > highestFormIndex)
			{
				highestFormIndex = fe.getFormIndex();
				formElement = fe;
			}
		}

		return formElement;
	}

//	public static List<IFormElements> getElementsWithInGroup()
//	{
//		
//	}
}
