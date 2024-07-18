/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2013 Servoy BV

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

package com.servoy.eclipse.ui.property;

import java.awt.Dimension;
import java.awt.Point;
import java.beans.PropertyDescriptor;

import javax.swing.border.Border;

import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.PropertyDescriptionBuilder;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.BytePropertyType;
import org.sablo.specification.property.types.ColorPropertyType;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.DoublePropertyType;
import org.sablo.specification.property.types.FloatPropertyType;
import org.sablo.specification.property.types.FontPropertyType;
import org.sablo.specification.property.types.InsetsPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.LongPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.specification.property.types.TypesRegistry;

import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPosition;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.scripting.annotations.AnnotationManagerReflection;
import com.servoy.j2db.server.ngclient.property.types.BorderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.CSSPositionPropertyType;

/**
 * Base class for property handlers base on java beans/introspection.
 *
 * @author rgansevles
 *
 */
public class BasePropertyHandler implements IPropertyHandler
{

	/**
	 * This is a HACK for sablo PropertyDescription objects returned by {@link #getPropertyDescription(Object, IPropertySource, PersistContext)} that
	 * are static / constants declared in this class and that do not give in their "config" a PropertyController but might need to show tooltips.<br/><br/>
	 *
	 * So when this happens we cannot set the tooltip directly on a new instance of PropertyController so that it's shown in the UI, instead the code that called this method will/might
	 * look at the PropertyDescription's "doc" tag to generate a tooltip when it creates the PropertyController. The problem is here that some of these constants can be
	 * used for multiple properties of the same type - and might need different tooltips.<br/><br/>
	 *
	 * So although it's strange, for all these constants (PDs) in this class that we described we use the same "tags" object reference; and we can modify then
	 * the "doc" in it to the value we find in the .spec file of this legacy component (if it runs single threaded then it will have the correct tooltip value for long enough - until
	 * the PropertyController is created and then it won't matter that we change it for a property that follows in a next call).<br/><br/>
	 *
	 * We use it as a convenience even for new (non constant) PDs that don't have a PropertyController as "config". Which are basically the same, but could instead be given separate "tags" obj. instances.
	 */
	protected static final JSONObject setTooltipOnTagsJSONObjectHack = new JSONObject();

	// null type: use property controller internally
	public static final PropertyDescription ANCHORS_DESCRIPTION = new PropertyDescriptionBuilder().withName("anchors")
		.withConfig(
			new AnchorPropertyController("anchors", RepositoryHelper.getDisplayName("anchors", GraphicalComponent.class)))
		.withTags(setTooltipOnTagsJSONObjectHack).build();

	protected final PropertyDescriptor propertyDescriptor;

	public BasePropertyHandler(java.beans.PropertyDescriptor propertyDescriptor)
	{
		this.propertyDescriptor = propertyDescriptor;
	}

	@Override
	public String getName()
	{
		return propertyDescriptor.getName();
	}

	@Override
	public boolean isProperty()
	{
		return propertyDescriptor.getReadMethod() != null && propertyDescriptor.getWriteMethod() != null && !propertyDescriptor.isExpert() &&
			!propertyDescriptor.getPropertyType().equals(Object.class) && !propertyDescriptor.isHidden();
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, PersistContext persistContext)
	{
		// Some properties apply to both persists and beans

		String name = propertyDescriptor.getName();

		// name based
		if (name.equals("anchors"))
		{
			return ANCHORS_DESCRIPTION;
		}

		// type based
		Class< ? > clazz = propertyDescriptor.getPropertyType();

		if (clazz == java.awt.Dimension.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(TypesRegistry.getType(DimensionPropertyType.TYPE_NAME))
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == java.awt.Point.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(TypesRegistry.getType(PointPropertyType.TYPE_NAME))
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == java.awt.Insets.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(TypesRegistry.getType(InsetsPropertyType.TYPE_NAME))
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == java.awt.Color.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(TypesRegistry.getType(ColorPropertyType.TYPE_NAME))
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == java.awt.Font.class)
		{
			return new PropertyDescriptionBuilder().withName(name)
				.withType(TypesRegistry.getType(FontPropertyType.TYPE_NAME))
				.withConfig(
					Boolean.FALSE)
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == Border.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(BorderPropertyType.INSTANCE).withConfig(Boolean.FALSE)
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == CSSPosition.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(CSSPositionPropertyType.INSTANCE).withConfig(Boolean.FALSE)
				.withTags(setTooltipOnTagsJSONObjectHack).build();
		}


		if (clazz == boolean.class || clazz == Boolean.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(BooleanPropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == String.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(StringPropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == byte.class || clazz == Byte.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(BytePropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == double.class || clazz == Double.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(DoublePropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == float.class || clazz == Float.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(FloatPropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == int.class || clazz == Integer.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(IntPropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == long.class || clazz == Long.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(LongPropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		if (clazz == short.class || clazz == Short.class)
		{
			return new PropertyDescriptionBuilder().withName(name).withType(IntPropertyType.INSTANCE).withTags(setTooltipOnTagsJSONObjectHack).build();
		}

		// setTooltipOnTagsJSONObjectHack is used by child class PersistPropertyHandler when it's a property also listed in the .spec file of legacy form elements;
		// no use trying to get that here as another child class is BeanPropertyHandler (smart client beans that don't have .spec files)

		return null;
	}

	@Override
	public String getDisplayName()
	{
		return propertyDescriptor.getDisplayName();
	}

	public Class< ? > getPropertyType()
	{
		return propertyDescriptor.getPropertyType();
	}

	@Override
	public boolean hasSupportForClientType(Object obj, ClientSupport csp)
	{
		return AnnotationManagerReflection.getInstance()
			.hasSupportForClientType(propertyDescriptor.getReadMethod(), obj.getClass(), csp,
				ClientSupport.Default);
	}

	@Override
	public Object getValue(Object obj, PersistContext persistContext)
	{
		try
		{
			return propertyDescriptor.getReadMethod().invoke(obj, new Object[0]);
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		try
		{
			if (StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(getName()) && value instanceof Point && persistContext != null &&
				!(persistContext.getPersist() instanceof Tab) &&
				((persistContext.getContext() instanceof Form && ((Form)persistContext.getContext()).getUseCssPosition()) ||
					CSSPositionUtils.useCSSPosition(persistContext.getPersist()) ||
					CSSPositionUtils.isInAbsoluteLayoutMode(persistContext.getPersist())))
			{
				// for tab we always use location
				CSSPositionUtils.setLocation((ISupportBounds)obj, ((Point)value).x, ((Point)value).y);
			}
			else if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(getName()) && value instanceof Dimension && persistContext != null &&
				((persistContext.getContext() instanceof Form && ((Form)persistContext.getContext()).getUseCssPosition()) ||
					CSSPositionUtils.isInAbsoluteLayoutMode(persistContext.getPersist())))
			{
				CSSPositionUtils.setSize((ISupportBounds)obj, ((Dimension)value).width, ((Dimension)value).height);
			}
			else
			{
				propertyDescriptor.getWriteMethod().invoke(obj, new Object[] { value });
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	@Override
	public boolean shouldShow(PersistContext persistContext)
	{
		try
		{
			String name = getName();
			// check for content spec element.
			IPersist persist = persistContext.getPersist();
			EclipseRepository repository = (EclipseRepository)persist.getRootObject().getRepository();
			Element element = repository.getContentSpec().getPropertyForObjectTypeByName(persist.getTypeID(), name);

			int dispType = -1;
			if (persist instanceof Field)
			{
				dispType = ((Field)persist).getDisplayType();
			}

			if (IContentSpecConstants.PROPERTY_NG_READONLY_MODE.equals(name) && persist instanceof Form)
			{
				Form form = (Form)persist;
				return SolutionMetaData.isServoyNGSolution(form.getSolution()) && form.getView() != IFormConstants.VIEW_TYPE_RECORD &&
					form.getView() != IFormConstants.VIEW_TYPE_RECORD_LOCKED;
			}
			if (persist instanceof Form && ((Form)persist).isFormComponent() && BaseComponent.isEventOrCommandProperty(name))
			{
				return false;
			}
			if (IContentSpecConstants.PROPERTY_CSS_POSITION.equals(name) && persistContext.getContext() instanceof Form &&
				!((Form)persistContext.getContext()).getUseCssPosition() && !CSSPositionUtils.isInAbsoluteLayoutMode(persistContext.getPersist()) &&
				!CSSPositionUtils.useCSSPosition(persistContext.getPersist()))
			{
				return false;
			}
			if (IContentSpecConstants.PROPERTY_ATTRIBUTES.equals(name))
			{
				if (!(persist instanceof IFormElement)) return false;
				Form form = (Form)((IFormElement)persist).getAncestor(IRepository.FORMS);
				int type = form.getSolution().getSolutionType();
				return (persist instanceof WebComponent || SolutionMetaData.isNGOnlySolution(type));
			}
			if (!RepositoryHelper.shouldShow(name, element, persist.getClass(), dispType))
			{
				return false;
			}
			if (persist instanceof Form frm && frm.isResponsiveLayout() && (name.equals("height") || name.equals("useMinHeight") || name.equals("useMinWidth")))
			{
				return false;
			}

			if (name.equals("labelFor") && persist instanceof GraphicalComponent)
			{
				GraphicalComponent gc = (GraphicalComponent)persist;
				if (ComponentFactory.isButton(gc))
				{
					//if it's a button, then we only show the property if it has a value (probably to be cleared via quickfix)
					return gc.getLabelFor() != null && !gc.getLabelFor().equals("");
				}
				else return true;
			}
			if (name.endsWith("printSliding") && !(persist.getParent() instanceof Form))
			{
				return false;//if not directly on form it can not slide
			}
			if ((name.equals("onTabChangeMethodID") || name.equals("scrollTabs")) && persist instanceof TabPanel &&
				(((TabPanel)persist).getTabOrientation() == TabPanel.SPLIT_HORIZONTAL || ((TabPanel)persist).getTabOrientation() == TabPanel.SPLIT_VERTICAL))
			{
				return false; // not applicable for splitpanes
			}

//			if (name.equals("loginFormID") && persist instanceof Solution && ((Solution)persist).getLoginFormID() <= 0)
//			{
//				if (((Solution)persist).getLoginSolutionName() != null)
//				{
//					// there is a login solution, do not show the deprecated login form setting
//					return false;
//				}
//			}
			// don't show the must authenticate property for a ngclient solution that one has now the authenticator property
			if (name.equals(StaticContentSpecLoader.PROPERTY_MUSTAUTHENTICATE.getPropertyName()) && persist instanceof Solution &&
				SolutionMetaData.isNGOnlySolution(((SolutionMetaData)persistContext.getPersist().getRootObject().getMetaData()).getSolutionType()))
			{
				return false;
			}

			if (persist instanceof Form && ((Form)persist).isResponsiveLayout() &&
				(name.equals(StaticContentSpecLoader.PROPERTY_TRANSPARENT.getPropertyName()) ||
					name.equals(StaticContentSpecLoader.PROPERTY_BORDERTYPE.getPropertyName())))
			{
				return false;
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}

		return true;
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see java.lang.Object#toString()
	 */
	@Override
	public String toString()
	{
		return "PropertyHandler[" + propertyDescriptor + "]";
	}
}
