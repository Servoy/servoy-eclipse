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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.swing.SwingUtilities;

import org.eclipse.swt.graphics.Image;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.IParentOverridable;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
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
import com.servoy.j2db.documentation.persistence.docs.DocsButton;
import com.servoy.j2db.documentation.persistence.docs.DocsCalendar;
import com.servoy.j2db.documentation.persistence.docs.DocsCheckBoxes;
import com.servoy.j2db.documentation.persistence.docs.DocsComboBox;
import com.servoy.j2db.documentation.persistence.docs.DocsImage;
import com.servoy.j2db.documentation.persistence.docs.DocsLabel;
import com.servoy.j2db.documentation.persistence.docs.DocsListForm;
import com.servoy.j2db.documentation.persistence.docs.DocsPassword;
import com.servoy.j2db.documentation.persistence.docs.DocsPortal;
import com.servoy.j2db.documentation.persistence.docs.DocsRadioButtons;
import com.servoy.j2db.documentation.persistence.docs.DocsRectShape;
import com.servoy.j2db.documentation.persistence.docs.DocsTabPanel;
import com.servoy.j2db.documentation.persistence.docs.DocsTextArea;
import com.servoy.j2db.documentation.persistence.docs.DocsTextField;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.IWebComponent;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
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

			if (persist instanceof IWebComponent)
			{
				IWebComponent iw = (IWebComponent)persist;
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(iw.getTypeName());
				name = spec != null && spec.getCategoryName() != null ? spec.getCategoryName() : "components";
			}
			else if (name.startsWith("combobox"))
			{
				name = "comboboxes";
			}
			else
			{
				name = name.substring(0, name.lastIndexOf('.'));
				if (name.lastIndexOf('s') != name.length() - 1) name = name + 's';
			}
		}

		return new Pair<String, Image>(name, image);
	}

	public static String getPersistImageName(IPersist persist)
	{
		if (persist instanceof Portal && ((Portal)persist).isMobileInsetList()) return "insetlist.png";
		if (persist instanceof BaseComponent)
		{
			String lookupName = ComponentFactory.getLookupName((BaseComponent)persist);
			if ("button".equals(lookupName))
			{
				return "button.png";
			}
			if ("check".equals(lookupName))
			{
				return "checkbox.png";
			}
			if ("combobox".equals(lookupName))
			{
				return "combobox.png";
			}
			if ("field".equals(lookupName))
			{
				return "textfield.png";
			}
			if ("portal".equals(lookupName))
			{
				return "portal.png";
			}
			if ("tabpanel".equals(lookupName))
			{
				return "tab.png";
			}
			if ("label".equals(lookupName))
			{
				return "label.png";
			}
			if ("radio".equals(lookupName))
			{
				return "radiobutton.png";
			}
			if ("listbox".equals(lookupName))
			{
				return "listbox.png";
			}
			if ("spinner".equals(lookupName))
			{
				return "spinner.png";
			}
		}
		if (persist instanceof Form)
		{
			return getImageNameForFormEncapsulation((Form)persist);
		}
		if (persist instanceof Bean)
		{
			return "ng_component.png";
		}
		if (persist instanceof RectShape)
		{
			return "rectangle.png";
		}
		if (persist instanceof Relation)
		{
			return RelationLabelProvider.getImageFileName((Relation)persist);
		}
		if (persist instanceof Media)
		{
			return "media.png";
		}
		if (persist instanceof ValueList)
		{
			return "valuelists.png";
		}
		if (persist instanceof ScriptCalculation)
		{
			return "columncalc.png";
		}
		if (persist instanceof ScriptVariable)
		{
			ScriptVariable sv = (ScriptVariable)persist;
			if (sv.isPrivate()) return "variable_private.png";
			if (sv.isPublic()) return "variable_public.png";
			if (sv.getScopeName() != null) return "variable_global.png";
			else return "form_variable.png";
		}
		if (persist instanceof ScriptMethod)
		{
			ScriptMethod sm = (ScriptMethod)persist;
			if (sm.isPrivate()) return "method_private.png";
			else if (sm.isProtected()) return "method_protected.png";
			else return "method_public.png";
		}
		if (persist instanceof LayoutContainer)
		{
			return "layoutcontainer.png";
		}
		if (persist instanceof Part)
		{
			return "parts.png";
		}
		if (persist instanceof IWebComponent)
		{
			return "ng_component.png";
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
			|| !(context instanceof Form) //
			|| ancestorForm == null // persist is something else, like a relation or a solution
			|| ancestorForm == context // already in same form
			|| !ModelUtils.getEditingFlattenedSolution(context).getFormHierarchy((Form)context).contains(ancestorForm)// check that the persist is in a parent form of the context
		)
		{
			// no override
			return persist;
		}
		IChildWebObject webObject = null;
		IParentOverridable childOfParent = null;
		if (persist instanceof IChildWebObject)
		{
			webObject = (IChildWebObject)persist;
			ISupportChilds parent = persist.getParent();
			if (parent instanceof IChildWebObject)
			{
				// Nested web objects
				parent = (ISupportChilds)getOverridePersist(PersistContext.create(parent, persistContext.getContext()));
				return getWebObjectChild(parent, webObject);
			}

			persist = parent;
		}
		else if (persist instanceof IParentOverridable)
		{
			childOfParent = (IParentOverridable)persist;
			persist = childOfParent.getParentToOverride();
		}

		persist = PersistHelper.getBasePersist((ISupportExtendsID)persist);
		IPersist parentPersist = persist;
		IPersist newPersist = (IPersist)context.acceptVisitor(o -> {
			if (o instanceof ISupportExtendsID && ((ISupportExtendsID)o).getExtendsID() == parentPersist.getID())
			{
				return o;
			}
			return IPersistVisitor.CONTINUE_TRAVERSAL;
		});

		if (newPersist == null)
		{
			// override does not exist yet, create it
			ISupportChilds parent = (Form)context;
			if (!((Form)ancestorForm).isResponsiveLayout() && !(parentPersist.getParent() instanceof Form))
			{
				parent = null;
				parent = (ISupportChilds)context.acceptVisitor(o -> {
					if (o instanceof ISupportExtendsID && ((ISupportExtendsID)o).getExtendsID() == parentPersist.getParent().getID())
					{
						return o;
					}
					return IPersistVisitor.CONTINUE_TRAVERSAL;
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
			if (CSSPositionUtils.useCSSPosition(persist))
			{
				ISupportBounds iSupportBounds = (ISupportBounds)persist;
				Point location = CSSPositionUtils.getLocation(iSupportBounds);
				Dimension size = CSSPositionUtils.getSize(iSupportBounds);
				CSSPositionUtils.setLocation((ISupportBounds)newPersist, location.x, location.y);
				if (size.width > 0 && size.height > 0) CSSPositionUtils.setSize((ISupportBounds)newPersist, size.width, size.height);
			}

		}
		if (webObject != null)
		{
			return getWebObjectChild(newPersist, webObject);
		}
		if (childOfParent != null)
		{
			newPersist = childOfParent.newOverwrittenParent(newPersist);
		}
		return newPersist;
	}

	private static IPersist getWebObjectChild(IPersist parent, IChildWebObject webObject)
	{
		Object newWebObject = ((AbstractBase)parent).getProperty(webObject.getJsonKey());
		if (newWebObject instanceof IChildWebObject)
		{
			return (IChildWebObject)newWebObject;
		}
		if (newWebObject instanceof Object[])
		{
			return (IPersist)((Object[])newWebObject)[webObject.getIndex()];
		}

		ServoyLog.logError("Cannot find the override custom type in: " + newWebObject, null);
		return webObject;
	}

	private static Map<String, WeakReference<Class< ? >>> beanClassCache = new ConcurrentHashMap<String, WeakReference<Class< ? >>>();

	public static Class< ? > getPersistClassForDesignDoc(final IApplication application, Object persist)
	{
		return getPersistClassForDocumentation(application, persist, false);
	}

	public static Class< ? > getPersistScriptClass(final IApplication application, Object model)
	{
		return getPersistClassForDocumentation(application, model, true);
	}

	/**
	 * Maps a given persist class to the appropriate design or scripting documentation class.<br/>
	 * For example for a button (GraphicalComponent persist) it might map it to IRuntimeButton if forScripting is true and to DocsButton is forScripting is false.
	 *
	 * @param model the persist object
	 * @param forScripting true if it should return the class for scripting documentation; false if it should return the design documentation class.
	 */
	private static Class< ? > getPersistClassForDocumentation(final IApplication application, Object model, boolean forScripting)
	{
		if (model instanceof GraphicalComponent)
		{
			GraphicalComponent label = (GraphicalComponent)model;
			if (ComponentFactory.isButton(label))
			{
				if (label.getDataProviderID() == null && !label.getDisplaysTags())
				{
					return forScripting ? IRuntimeButton.class : DocsButton.class;
				}
				return forScripting ? IRuntimeDataButton.class : DocsButton.class;
			}

			if (label.getDataProviderID() == null && !label.getDisplaysTags())
			{
				return forScripting ? IScriptScriptLabelMethods.class : DocsLabel.class;
			}
			return forScripting ? IScriptDataLabelMethods.class : DocsLabel.class;
		}

		if (model instanceof Field)
		{
			Field field = (Field)model;

			switch (field.getDisplayType())
			{
				case Field.PASSWORD :
					return forScripting ? IRuntimePassword.class : DocsPassword.class;
				case Field.RTF_AREA :
					return forScripting ? IRuntimeRtfArea.class : DocsTextField.class; // don't have a dedicated class for rtf area so use the basic field docs
				case Field.HTML_AREA :
					return forScripting ? IRuntimeHtmlArea.class : DocsTextField.class; // don't have a dedicated class for rtf area so use the basic field docs
				case Field.TEXT_AREA :
					return forScripting ? IRuntimeTextArea.class : DocsTextArea.class;
				case Field.CHECKS :
					if (isSingle(application, field))
					{
						return forScripting ? IRuntimeCheck.class : DocsCheckBoxes.class;
					}
					return forScripting ? IRuntimeChecks.class : DocsCheckBoxes.class;
				case Field.RADIOS :
					if (isSingle(application, field))
					{
						return forScripting ? IRuntimeRadio.class : DocsRadioButtons.class;
					}
					return forScripting ? IRuntimeRadios.class : DocsRadioButtons.class;
				case Field.COMBOBOX :
					return forScripting ? IRuntimeCombobox.class : DocsComboBox.class;
				case Field.CALENDAR :
					return forScripting ? IRuntimeCalendar.class : DocsCalendar.class;
				case Field.IMAGE_MEDIA :
					return forScripting ? IRuntimeImageMedia.class : DocsImage.class;
				case Field.LIST_BOX :
				case Field.MULTISELECT_LISTBOX :
					return forScripting ? IRuntimeListBox.class : DocsTextField.class; // don't have a dedicated class for list box so use the basic field docs;
				case Field.SPINNER :
					return forScripting ? IRuntimeSpinner.class : DocsTextField.class; // don't have a dedicated class for list box so use the basic field docs;
				default :
					return forScripting ? IRuntimeTextField.class : DocsTextField.class;
			}
		}

		if (model instanceof Bean)
		{
			if (ServoyModelManager.getServoyModelManager().getServoyModel().isActiveSolutionMobile())
			{
				return forScripting ? IScriptMobileBean.class : model.getClass(); // currently we don't support design time docs (properties view tooltips for example) for smart client/web client/mobile beans
			}

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
				ClassLoader bcl = application.getPluginManager().getClassLoader();
				try
				{
					beanClass = bcl.loadClass(beanClassName);
					if (IServoyBeanFactory.class.isAssignableFrom(beanClass))
					{
						// these beans are always swing and or web, try to make them in the even thread then
						final Class< ? >[] retValue = new Class[1];
						final Class< ? > bc = beanClass;
						SwingUtilities.invokeAndWait(() -> {
							try
							{
								Form form = (Form)bean.getParent();
								IServoyBeanFactory beanFactory = (IServoyBeanFactory)bc.newInstance();
								Object beanInstance = beanFactory.getBeanInstance(application.getApplicationType(),
									(IClientPluginAccess)application.getPluginAccess(),
									new Object[] { ComponentFactory.getWebID(null, bean), form.getName(), form.getStyleName() });
								retValue[0] = beanInstance.getClass();
								if (beanInstance instanceof IScriptObject)
								{
									ScriptObjectRegistry.registerScriptObjectForClass(retValue[0], (IScriptObject)beanInstance);
								}
							}
							catch (Throwable t)
							{
								Debug.error("Error loading bean: " + bean.getName() + " clz: " + bc, t);
							}
						});
						beanClass = retValue[0];
					}
					beanClassCache.put(beanClassName, new WeakReference<Class< ? >>(beanClass));
				}
				catch (Throwable e)
				{
					Debug.error("Error loading bean: " + bean.getName() + " clz: " + beanClassName, e);
				}
			}
			return forScripting ? beanClass : model.getClass();
		}

		if (model instanceof TabPanel)
		{
			int orient = ((TabPanel)model).getTabOrientation();
			if (orient == TabPanel.SPLIT_HORIZONTAL || orient == TabPanel.SPLIT_VERTICAL)
				return forScripting ? IScriptSplitPaneMethods.class : DocsTabPanel.class;
			if (orient == TabPanel.ACCORDION_PANEL) return forScripting ? IScriptAccordionPanelMethods.class : DocsTabPanel.class;
			return forScripting ? IScriptTabPanelMethods.class : DocsTabPanel.class;
		}

		if (model instanceof MobileListModel)
		{
			return forScripting ? IScriptInsetListComponentMethods.class : DocsListForm.class;
		}

		if (model instanceof Portal)
		{
			if (((Portal)model).isMobileInsetList())
			{
				return forScripting ? IScriptInsetListComponentMethods.class : model.getClass(); // don't have a dedicated class for mobile list component...
			}
			return forScripting ? IScriptPortalComponentMethods.class : DocsPortal.class;
		}

		if (model instanceof RectShape)
		{
			return forScripting ? IRuntimeRectangle.class : DocsRectShape.class;
		}

		if (model instanceof FormElementGroup)
		{
			// find persist class for component in component-with-title
			for (IFormElement elem : Utils.iterate(((FormElementGroup)model).getElements()))
			{
				if (elem instanceof AbstractBase && ((AbstractBase)elem).getCustomMobileProperty(IMobileProperties.COMPONENT_TITLE.propertyName) == null)
				{
					return getPersistClassForDocumentation(application, elem, forScripting);
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

			if (!(valuelist.getValueListType() == IValueListConstants.DATABASE_VALUES &&
				valuelist.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES) &&
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

		Rectangle formElementRectangle = new Rectangle(CSSPositionUtils.getLocation(formElement), CSSPositionUtils.getSize(formElement));

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

				if (new Rectangle(CSSPositionUtils.getLocation(itFormElement), CSSPositionUtils.getSize(itFormElement)).intersects(formElementRectangle))
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
				if (currentGroupID != null && !currentElement.getUUID().equals(element) && currentGroupID.equals(groupID))
					returnList.add((BaseComponent)currentElement);
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

		Rectangle element1Rectangle = new Rectangle(CSSPositionUtils.getSize(element1));
		element1Rectangle.setLocation(CSSPositionUtils.getLocation(element1));

		Rectangle element2Rectangle = new Rectangle(CSSPositionUtils.getSize(element2));
		element2Rectangle.setLocation(CSSPositionUtils.getLocation(element2));

		return element1Rectangle.intersects(element2Rectangle);
	}

	public static String getImageNameForFormEncapsulation(Form f)
	{
		String relPath = null;
		if (f != null)
		{
			//designer.png
			int encapsulation = f.getEncapsulation();
			if ((encapsulation & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE) relPath = "designer_protected.png";
			else if ((encapsulation & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE)
				relPath = "designer_private.png";
			else if ((encapsulation & DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) == DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL)
				relPath = "designer_public.png";
			else relPath = "designer.png";
		}
		return relPath;
	}

	public static Image getImageForFormEncapsulation(Form form)
	{
		Image image = null;
		if (form != null)
		{
			int encapsulation = (form).getEncapsulation();
			if ((encapsulation & PersistEncapsulation.MODULE_SCOPE) == PersistEncapsulation.MODULE_SCOPE)
				image = Activator.getDefault().loadImageFromBundle("designer_protected.png");
			else if ((encapsulation & PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE) == PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE)
				image = Activator.getDefault().loadImageFromBundle("designer_private.png");
			else if ((encapsulation & DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL) == DesignerPreferences.ENCAPSULATION_PUBLIC_HIDE_ALL)
				image = Activator.getDefault().loadImageFromBundle("designer_public.png");
			else image = Activator.getDefault().loadImageFromBundle("designer.png");
		}
		return image;
	}

	public static void convertToCSSPosition(List<Form> toConvert)
	{
		List<String> solutionNames = new ArrayList<String>();
		if (toConvert != null && ServoyModelFinder.getServoyModel().getActiveProject() != null)
		{
			FlattenedSolution fs = ServoyModelFinder.getServoyModel().getActiveProject().getEditingFlattenedSolution();
			// add all parent forms for conversion
			for (Form form : toConvert.toArray(new Form[0]))
			{
				List<Form> hierarchyForms = fs.getFormHierarchy(form);
				for (Form parentForm : hierarchyForms)
				{
					if (!toConvert.contains(parentForm))
						toConvert.add(parentForm);
				}
			}

			// add all sibblings for conversion
			Iterator<Form> it = fs.getForms(false);
			while (it.hasNext())
			{
				Form currentForm = it.next();
				if (!toConvert.contains(currentForm))
				{
					Form parentForm = currentForm.getExtendsForm();
					while (parentForm != null)
					{
						if (toConvert.contains(parentForm))
						{
							toConvert.add(currentForm);
							break;
						}
						parentForm = parentForm.getExtendsForm();
					}
				}
			}

			for (Form form : toConvert)
			{
				CSSPositionUtils.convertToCSSPosition(form);
				String solutionName = form.getSolution().getName();
				if (!solutionNames.contains(solutionName))
				{
					solutionNames.add(solutionName);
				}
			}
		}
		for (String solutionName : solutionNames)
		{
			ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(
				solutionName);
			try
			{
				servoyProject.saveEditingSolutionNodes(new IPersist[] { servoyProject.getEditingSolution() }, true);
			}
			catch (RepositoryException e)
			{
				Debug.error(e);
			}
		}
	}
}
