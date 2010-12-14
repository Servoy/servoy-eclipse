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

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFactory;
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
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
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
import com.servoy.j2db.ui.IScriptScriptButtonMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTextAreaMethods;
import com.servoy.j2db.ui.IScriptTextEditorMethods;
import com.servoy.j2db.util.Debug;

/**
 * Utilities for elements and label providers.
 * 
 * @author rgansevles
 */

public class ElementUtil
{
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

	public static boolean isInheritedFormElement(IPersist context, Object element)
	{
		if (element instanceof Form)
		{
			return false;
		}
		if (context instanceof Form && element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
		{
			if (element instanceof IPersist && (((IPersist)element).getAncestor(IRepository.FORMS) != context))
			{
				// child of super-form, readonly
				return true;
			}
		}
		if (element instanceof FormElementGroup)
		{
			Iterator<IFormElement> elements = ((FormElementGroup)element).getElements();
			while (elements.hasNext())
			{
				if (isInheritedFormElement(context, elements.next()))
				{
					return true;
				}
			}
		}
		if (element instanceof AbstractBase)
		{
			return ((AbstractBase)element).isOverrideElement();
		}
		// child of this form, not of a inherited form
		return false;
	}

	public static IPersist getOverridePersist(IPersist context, IPersist persist)
	{
		if (persist instanceof AbstractBase && context instanceof Form && persist.getAncestor(IRepository.FORMS) != context)
		{
			IPersist newPersist = AbstractRepository.searchPersist((Form)context, persist.getUUID());
			if (newPersist == null)
			{
				ISupportChilds parent = (Form)context;
				if (!(persist.getParent() instanceof Form))
				{
					parent = null;
					parent = (ISupportChilds)AbstractRepository.searchPersist((Form)context, persist.getParent().getUUID());
					if (parent == null)
					{
						try
						{
							parent = (ISupportChilds)((AbstractBase)persist.getParent()).cloneObj((Form)context, false, null, false, false);
							((AbstractBase)parent).resetUUID(persist.getParent().getUUID());
							((AbstractBase)parent).copyPropertiesMap(null);
							((AbstractBase)parent).putOverrideProperty(((Form)persist.getAncestor(IRepository.FORMS)).getName());
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				}
				try
				{
					newPersist = ((AbstractBase)persist).cloneObj(parent, false, null, false, false);
					((AbstractBase)newPersist).resetUUID(persist.getUUID());
					((AbstractBase)newPersist).copyPropertiesMap(null);
					((AbstractBase)newPersist).putOverrideProperty(((Form)persist.getAncestor(IRepository.FORMS)).getName());
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
			return newPersist;
		}
		return persist;
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
					if (field.getValuelistID() > 0)
					{
//						IValueList list = getRealValueList(application, valuelist, true, type, format, field.getDataProviderID());
//						if (!(valuelist.getValueListType() == ValueList.DATABASE_VALUES && valuelist.getDatabaseValuesType() == ValueList.RELATED_VALUES) &&
//							list.getSize() == 1 && valuelist.getAddEmptyValue() != ValueList.EMPTY_VALUE_ALWAYS)
//						{
//							fl = application.getItemFactory().createDataCheckBox(getWebID(field), application.getI18NMessageIfPrefixed(field.getText()), list);
//						}
//						else
						// 0 or >1
						return IScriptChoiceMethods.class;
					}
					else
					{
						return IScriptCheckBoxMethods.class;
					}
				case Field.RADIOS :
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
		return null;

	}

}
