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

import java.awt.Rectangle;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.eclipse.swt.graphics.Image;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IApplication;
import com.servoy.j2db.IServoyBeanFactory;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IValueList;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.plugins.IClientPluginAccess;
import com.servoy.j2db.scripting.IScriptObject;
import com.servoy.j2db.scripting.ScriptObjectRegistry;
import com.servoy.j2db.ui.IScriptAccordionPanelMethods;
import com.servoy.j2db.ui.IScriptDataLabelMethods;
import com.servoy.j2db.ui.IScriptInsetListComponentMethods;
import com.servoy.j2db.ui.IScriptMobileBean;
import com.servoy.j2db.ui.IScriptPortalComponentMethods;
import com.servoy.j2db.ui.IScriptScriptLabelMethods;
import com.servoy.j2db.ui.IScriptSplitPaneMethods;
import com.servoy.j2db.ui.IScriptTabPanelMethods;
import com.servoy.j2db.ui.runtime.IRuntimeButton;
import com.servoy.j2db.ui.runtime.IRuntimeCalendar;
import com.servoy.j2db.ui.runtime.IRuntimeCheck;
import com.servoy.j2db.ui.runtime.IRuntimeChecks;
import com.servoy.j2db.ui.runtime.IRuntimeCombobox;
import com.servoy.j2db.ui.runtime.IRuntimeDataButton;
import com.servoy.j2db.ui.runtime.IRuntimeHtmlArea;
import com.servoy.j2db.ui.runtime.IRuntimeImageMedia;
import com.servoy.j2db.ui.runtime.IRuntimeListBox;
import com.servoy.j2db.ui.runtime.IRuntimePassword;
import com.servoy.j2db.ui.runtime.IRuntimeRadio;
import com.servoy.j2db.ui.runtime.IRuntimeRadios;
import com.servoy.j2db.ui.runtime.IRuntimeRectangle;
import com.servoy.j2db.ui.runtime.IRuntimeRtfArea;
import com.servoy.j2db.ui.runtime.IRuntimeSpinner;
import com.servoy.j2db.ui.runtime.IRuntimeTextArea;
import com.servoy.j2db.ui.runtime.IRuntimeTextField;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Utilities for elements and label providers.
 * 
 * @author rgansevles
 */

public class ElementUtil
{
	public static Pair<String, Image> getPersistNameAndImage(IPersist persist)
	{
		String name = getPersistImageName(persist);
		Image image = null;
		if (name != null)
		{
			image = Activator.getDefault().loadImageFromOldLocation(name);
			if (image == null)
			{
				image = Activator.getDefault().loadImageFromBundle(name);
			}

			name = name.substring(0, name.lastIndexOf('.'));
			if (name.lastIndexOf('s') != name.length() - 1) name = name + 's';
		}

		return new Pair<String, Image>(name, image);
	}

	public static String getPersistImageName(IPersist persist)
	{
		if (persist instanceof Portal && ((Portal)persist).isMobileInsetList()) return "insetlist.gif";
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
			return getImageNameForFormEncapsulation((Form)persist);
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
			return RelationLabelProvider.getImageFileName((Relation)persist);
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
			ScriptVariable sv = (ScriptVariable)persist;
			if (sv.isPrivate()) return "variable_private.gif"; //$NON-NLS-1$
			if (sv.isPublic()) return "variable_public.gif"; //$NON-NLS-1$
			if (sv.getScopeName() != null) return "global_variable.gif"; //$NON-NLS-1$ 
			else return "form_variable.gif"; //$NON-NLS-1$
		}
		if (persist instanceof ScriptMethod)
		{
			ScriptMethod sm = (ScriptMethod)persist;
			if (sm.isPrivate()) return "private_method.gif"; //$NON-NLS-1$
			else if (sm.isProtected()) return "protected_method.gif"; //$NON-NLS-1$
			else return "public_method.gif"; //$NON-NLS-1$
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
	public static IPersist getOverridePersist(PersistContext persistContext) throws RepositoryException
	{
		IPersist persist = persistContext.getPersist();
		IPersist context = persistContext.getContext();
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
		while (PersistHelper.getSuperPersist((ISupportExtendsID)persist) != null)
		{
			persist = PersistHelper.getSuperPersist((ISupportExtendsID)persist);
		}
		final IPersist parentPersist = persist;
		IPersist newPersist = (IPersist)context.acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o instanceof ISupportExtendsID && ((ISupportExtendsID)o).getExtendsID() == parentPersist.getID())
				{
					return o;
				}
				return CONTINUE_TRAVERSAL;
			}
		});

		if (newPersist == null)
		{
			// override does not exist yet, create it
			ISupportChilds parent = (Form)context;
			if (!(parentPersist.getParent() instanceof Form))
			{
				parent = null;
				parent = (ISupportChilds)context.acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist o)
					{
						if (o instanceof ISupportExtendsID && ((ISupportExtendsID)o).getExtendsID() == parentPersist.getParent().getID())
						{
							return o;
						}
						return CONTINUE_TRAVERSAL;
					}
				});
				if (parent == null)
				{
					parent = (ISupportChilds)((AbstractBase)persist.getParent()).cloneObj((Form)context, false, null, false, false, false);
					((AbstractBase)parent).copyPropertiesMap(null, true);
					((ISupportExtendsID)parent).setExtendsID(parentPersist.getParent().getID());
				}
			}
			newPersist = ((AbstractBase)persist).cloneObj(parent, false, null, false, false, false);
			((AbstractBase)newPersist).copyPropertiesMap(null, true);
			((ISupportExtendsID)newPersist).setExtendsID(parentPersist.getID());
		}
		return newPersist;
	}

	private static Map<String, WeakReference<Class< ? >>> beanClassCache = new ConcurrentHashMap<String, WeakReference<Class< ? >>>();

	public static Class< ? > getPersistScriptClass(final IApplication application, Object model)
	{
		if (model instanceof GraphicalComponent)
		{
			GraphicalComponent label = (GraphicalComponent)model;
			if (ComponentFactory.isButton(label))
			{
				if (label.getDataProviderID() == null && !label.getDisplaysTags())
				{
					return IRuntimeButton.class;
				}
				return IRuntimeDataButton.class;
			}

			if (label.getDataProviderID() == null && !label.getDisplaysTags())
			{
				return IScriptScriptLabelMethods.class;
			}
			return IScriptDataLabelMethods.class;
		}

		if (model instanceof Field)
		{
			Field field = (Field)model;

			switch (field.getDisplayType())
			{
				case Field.PASSWORD :
					return IRuntimePassword.class;
				case Field.RTF_AREA :
					return IRuntimeRtfArea.class;
				case Field.HTML_AREA :
					return IRuntimeHtmlArea.class;
				case Field.TEXT_AREA :
					return IRuntimeTextArea.class;
				case Field.CHECKS :
					if (isSingle(application, field))
					{
						return IRuntimeCheck.class;
					}
					return IRuntimeChecks.class;
				case Field.RADIOS :
					if (isSingle(application, field))
					{
						return IRuntimeRadio.class;
					}
					return IRuntimeRadios.class;
				case Field.COMBOBOX :
					return IRuntimeCombobox.class;
				case Field.CALENDAR :
					return IRuntimeCalendar.class;
				case Field.IMAGE_MEDIA :
					return IRuntimeImageMedia.class;
				case Field.LIST_BOX :
				case Field.MULTISELECT_LISTBOX :
					return IRuntimeListBox.class;
				case Field.SPINNER :
					return IRuntimeSpinner.class;
				default :
					return IRuntimeTextField.class;
			}
		}

		if (model instanceof Bean)
		{
			if (ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile()) return IScriptMobileBean.class;
			else
			{
				final Bean bean = (Bean)model;
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
							try
							{
								Form form = (Form)bean.getParent();
								IServoyBeanFactory beanFactory = (IServoyBeanFactory)beanClass.newInstance();
								Object beanInstance = beanFactory.getBeanInstance(application.getApplicationType(),
									(IClientPluginAccess)application.getPluginAccess(),
									new Object[] { ComponentFactory.getWebID(null, bean), form.getName(), form.getStyleName() });
								beanClass = beanInstance.getClass();
								if (beanInstance instanceof IScriptObject)
								{
									ScriptObjectRegistry.registerScriptObjectForClass(beanClass, (IScriptObject)beanInstance);
								}
							}
							catch (Throwable t)
							{
								Debug.error("Error loading bean: " + bean.getName() + " clz: " + beanClass, t); //$NON-NLS-1$ //$NON-NLS-2$
							}
						}
						beanClassCache.put(beanClassName, new WeakReference<Class< ? >>(beanClass));
					}
					catch (Throwable e)
					{
						Debug.error("Error loading bean: " + bean.getName() + " clz: " + beanClassName, e); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				return beanClass;
			}
		}

		if (model instanceof TabPanel)
		{
			int orient = ((TabPanel)model).getTabOrientation();
			if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL) return IScriptSplitPaneMethods.class;
			if (orient == TabPanel.ACCORDION_PANEL) return IScriptAccordionPanelMethods.class;
			return IScriptTabPanelMethods.class;
		}

		if (model instanceof MobileListModel)
		{
			return IScriptInsetListComponentMethods.class;
		}

		if (model instanceof Portal)
		{
			if (((Portal)model).isMobileInsetList())
			{
				return IScriptInsetListComponentMethods.class;
			}
			return IScriptPortalComponentMethods.class;
		}

		if (model instanceof RectShape)
		{
			return IRuntimeRectangle.class;
		}

		if (model instanceof FormElementGroup)
		{
			// find persist class for component in component-with-title
			for (IFormElement elem : Utils.iterate(((FormElementGroup)model).getElements()))
			{
				if (elem instanceof AbstractBase && ((AbstractBase)elem).getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName) == null)
				{
					return getPersistScriptClass(application, elem);
				}
			}
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
			FlattenedSolution flattenedSolution = application.getFlattenedSolution();
			ValueList valuelist = flattenedSolution != null ? flattenedSolution.getValueList(field.getValuelistID()) : null;
			if (valuelist == null) return true;

			if (!(valuelist.getValueListType() == IValueListConstants.DATABASE_VALUES && valuelist.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES) &&
				(valuelist.getAddEmptyValue() != IValueListConstants.EMPTY_VALUE_ALWAYS))
			{
				IValueList realValueList = ComponentFactory.getRealValueList(application, valuelist, false, 0, null, null);
				if (realValueList != null && realValueList.getSize() == 1)
				{
					return true;
				}
			}
			return false;
		}


		return true;
	}

	public static List<IFormElement> getOverlappingFormElements(Form flattenedForm, IFormElement formElement)
	{
		List<IFormElement> overlappingElements = null;

		Rectangle formElementRectangle = new Rectangle(formElement.getLocation(), formElement.getSize());

		Iterator<IPersist> it = flattenedForm.getAllObjects();
		while (it.hasNext())
		{
			IPersist element = it.next();
			if (element instanceof IFormElement)
			{
				IFormElement itFormElement = (IFormElement)element;
				if (element.getUUID().equals(formElement.getUUID()))
				{
					continue;
				}

				if (new Rectangle(itFormElement.getLocation(), itFormElement.getSize()).intersects(formElementRectangle))
				{
					if (overlappingElements == null)
					{
						overlappingElements = new ArrayList<IFormElement>();
					}
					overlappingElements.add((IFormElement)element);
				}
			}
		}

		return overlappingElements;
	}

	public static HashMap<UUID, IFormElement> getNeighbours(Form form, HashMap<UUID, IFormElement> foundList, HashMap<UUID, IFormElement> elementsToVisit,
		boolean recursive)
	{
		if (form == null || foundList == null || elementsToVisit == null || elementsToVisit.isEmpty()) return null;

		HashMap<UUID, IFormElement> newFoundList = new HashMap<UUID, IFormElement>(foundList);

		HashMap<UUID, IFormElement> newElements = new HashMap<UUID, IFormElement>();

		HashMap<UUID, IFormElement> returnList = null;

		Iterator<IPersist> it = form.getAllObjects();
		while (it.hasNext())
		{
			IPersist element = it.next();
			if (element instanceof IFormElement)
			{
				if (newFoundList.containsKey(element.getUUID())) continue;
				for (IFormElement bc : elementsToVisit.values())
				{
					if (elementsIntersect(element, bc))
					{
						newFoundList.put(element.getUUID(), (IFormElement)element);
						newElements.put(element.getUUID(), (IFormElement)element);
						List<IFormElement> groupMemebers = getGroupMembers(form, (IFormElement)element);
						if (groupMemebers == null) break;
						for (IFormElement neighbour : groupMemebers)
						{
							if (!newFoundList.containsKey(neighbour.getUUID()))
							{
								newFoundList.put(neighbour.getUUID(), neighbour);
								newElements.put(neighbour.getUUID(), neighbour);
							}
						}
						break;
					}
				}
			}
		}

		if (!newElements.isEmpty() && recursive) returnList = getNeighbours(form, newFoundList, newElements, true);
		else returnList = newFoundList;

		return returnList;
	}

	public static HashMap<UUID, IFormElement> getImmediateNeighbours(Form form, HashMap<UUID, IFormElement> elementsToVisit)
	{
		if (elementsToVisit == null || form == null || elementsToVisit.isEmpty()) return null;

		HashMap<UUID, IFormElement> returnList = new HashMap<UUID, IFormElement>(elementsToVisit);

		Iterator<IPersist> it = form.getAllObjects();
		while (it.hasNext())
		{
			IPersist element = it.next();
			if (element instanceof IFormElement)
			{
				for (IFormElement bc : elementsToVisit.values())
				{
					if (elementsIntersect(element, bc))
					{
						if (!returnList.containsKey(element.getUUID())) returnList.put(element.getUUID(), (IFormElement)element);
						List<IFormElement> groupMemebers = getGroupMembers(form, (IFormElement)element);
						if (groupMemebers != null)
						{
							for (IFormElement neighbour : groupMemebers)
							{
								if (!returnList.containsKey(neighbour.getUUID())) returnList.put(neighbour.getUUID(), neighbour);
							}
						}
					}
				}
			}
		}

		return returnList;
	}

	public static List<IFormElement> getGroupMembers(Form form, IFormElement element)
	{
		String groupID = element.getGroupID();
		if (groupID == null) return null;

		ArrayList<IFormElement> returnList = new ArrayList<IFormElement>();

		Iterator<IPersist> it = form.getAllObjects();
		while (it.hasNext())
		{
			IPersist currentElement = it.next();
			if (currentElement instanceof IFormElement)
			{
				String currentGroupID = ((IFormElement)currentElement).getGroupID();
				if (currentGroupID != null && !currentElement.getUUID().equals(element) && currentGroupID.equals(groupID)) returnList.add((BaseComponent)currentElement);
			}
		}

		return returnList;
	}

	public static boolean elementsIntersect(IPersist e1, IPersist e2)
	{
		IFormElement element1;
		IFormElement element2;
		if (e1 instanceof IFormElement) element1 = (IFormElement)e1;
		else return false;
		if (e2 instanceof IFormElement) element2 = (IFormElement)e2;
		else return false;

		Rectangle element1Rectangle = new Rectangle(element1.getSize());
		element1Rectangle.setLocation(element1.getLocation());

		Rectangle element2Rectangle = new Rectangle(element2.getSize());
		element2Rectangle.setLocation(element2.getLocation());

		return element1Rectangle.intersects(element2Rectangle);
	}

	public static String getImageRelPathForFormEncapsulation(Form f)
	{
		String relPath = null;
		if (f != null)
		{
			//designer.gif
			int encapsulation = f.getEncapsulation();
			if ((encapsulation & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE) relPath = "icons/designer_protected.gif"; //$NON-NLS-1$
			else if ((encapsulation & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) relPath = "icons/designer_private.gif"; //$NON-NLS-1$
			else if ((encapsulation & DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) == DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) relPath = "icons/designer_public.gif"; //$NON-NLS-1$
			else relPath = "icons/designer.gif"; //$NON-NLS-1$
		}
		return relPath;
	}

	public static String getImageNameForFormEncapsulation(Form form)
	{
		String relPath = getImageRelPathForFormEncapsulation(form);
		return (relPath != null ? relPath.substring(6) : null);
	}

	public static Image getImageForFormEncapsulation(Form form)
	{
		Image image = null;
		if (form != null)
		{
			int encapsulation = (form).getEncapsulation();
			if ((encapsulation & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE) image = Activator.getDefault().loadImageFromBundle(
				"designer_protected.gif"); //$NON-NLS-1$
			else if ((encapsulation & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) image = Activator.getDefault().loadImageFromBundle(
				"designer_private.gif"); //$NON-NLS-1$
			else if ((encapsulation & DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) == DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) image = Activator.getDefault().loadImageFromBundle(
				"designer_public.gif"); //$NON-NLS-1$
			else image = Activator.getDefault().loadImageFromBundle("designer.gif"); //$NON-NLS-1$
		}
		return image;
	}
}
