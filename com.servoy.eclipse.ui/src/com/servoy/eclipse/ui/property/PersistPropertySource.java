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
package com.servoy.eclipse.ui.property;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Insets;
import java.beans.IntrospectionException;
import java.beans.PropertyEditor;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import javax.swing.border.Border;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.core.runtime.Platform;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.IViewReference;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;
import org.eclipse.ui.views.properties.PropertySheetPage;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.ValuesConfig;
import org.sablo.specification.property.IPropertyType;
import org.sablo.specification.property.types.BooleanPropertyType;
import org.sablo.specification.property.types.BytePropertyType;
import org.sablo.specification.property.types.ColorPropertyType;
import org.sablo.specification.property.types.DimensionPropertyType;
import org.sablo.specification.property.types.DoublePropertyType;
import org.sablo.specification.property.types.FloatPropertyType;
import org.sablo.specification.property.types.FontPropertyType;
import org.sablo.specification.property.types.FunctionPropertyType;
import org.sablo.specification.property.types.InsetsPropertyType;
import org.sablo.specification.property.types.IntPropertyType;
import org.sablo.specification.property.types.LongPropertyType;
import org.sablo.specification.property.types.PointPropertyType;
import org.sablo.specification.property.types.ScrollbarsPropertyType;
import org.sablo.specification.property.types.StringPropertyType;
import org.sablo.specification.property.types.StyleClassPropertyType;
import org.sablo.specification.property.types.TabSeqPropertyType;
import org.sablo.specification.property.types.ValuesPropertyType;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.DesignComponentFactory;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.repository.I18NMessagesUtil;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseMessages;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.dialogs.MethodDialog.MethodListOptions;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.editors.BeanCustomCellEditor;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor.DataProviderValueEditor;
import com.servoy.eclipse.ui.editors.FontCellEditor;
import com.servoy.eclipse.ui.editors.FormatCellEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.editors.TagsAndI18NTextCellEditor;
import com.servoy.eclipse.ui.labelproviders.ArrayLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FontLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.PersistInheritenceDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValidvalueDelegatelabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.MediaPropertyController.MediaPropertyControllerConfig;
import com.servoy.eclipse.ui.property.MethodWithArguments.UnresolvedMethodWithArguments;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.eclipse.ui.util.IDefaultValue;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.eclipse.ui.views.properties.IMergeablePropertyDescriptor;
import com.servoy.eclipse.ui.views.properties.IMergedPropertyDescriptor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IBasicFormManager;
import com.servoy.j2db.component.ComponentFormat;
import com.servoy.j2db.dataui.PropertyEditorClass;
import com.servoy.j2db.dataui.PropertyEditorHint;
import com.servoy.j2db.dataui.PropertyEditorOption;
import com.servoy.j2db.debug.DebugUtils;
import com.servoy.j2db.documentation.ClientSupport;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.types.BorderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormatPropertyType;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
import com.servoy.j2db.server.ngclient.property.types.RelationPropertyType;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.smart.dataui.InvisibleBean;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.IDelegate;
import com.servoy.j2db.util.JavaVersion;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.SafeArrayList;
import com.servoy.j2db.util.Utils;

/**
 * Property source for IPersist objects.
 * This class manages contents of the properties view by returning a list of
 * property descriptors depending on value and context of the persist object.
 *
 * @author rgansevles
 */

public class PersistPropertySource implements IPropertySource, IAdaptable, IModelSavePropertySource
{
	public static final Comparator<IPropertyHandler> BEANS_PROPERTY_COMPARATOR = new Comparator<IPropertyHandler>()
	{
		public int compare(IPropertyHandler p1, IPropertyHandler p2)
		{
			Object hint1 = null;
			if (p1 instanceof BeanPropertyHandler)
			{
				hint1 = ((BeanPropertyHandler)p1).getAttributeValue(PropertyEditorHint.PROPERTY_EDITOR_HINT);
			}
			Object hint2 = null;
			if (p2 instanceof BeanPropertyHandler)
			{
				hint2 = ((BeanPropertyHandler)p2).getAttributeValue(PropertyEditorHint.PROPERTY_EDITOR_HINT);
			}

			Object comp1 = null;
			Object comp2 = null;
			if (hint1 instanceof PropertyEditorHint)
			{
				comp1 = ((PropertyEditorHint)hint1).getOption(PropertyEditorOption.propertyOrder);
				if (comp1 instanceof Comparable< ? >)
				{
					if (hint2 instanceof PropertyEditorHint)
					{
						comp2 = ((PropertyEditorHint)hint2).getOption(PropertyEditorOption.propertyOrder);
					}
					if (!(comp2 instanceof Comparable< ? >))
					{
						// not both comparable, compare on displayName
						comp1 = comp2 = null;
					}
				}
			}
			if (!(comp1 instanceof Comparable< ? >))
			{
				comp1 = p1.getDisplayName();
				comp2 = p2.getDisplayName();
			}
			return comp1 == null ? (comp2 == null ? 0 : -1) : (comp2 == null ? 1 : ((Comparable)comp1).compareTo(comp2));
		}
	};


	public static final IPropertyConverter<String, Border> BORDER_STRING_CONVERTER = new IPropertyConverter<String, Border>()
	{
		public Border convertProperty(Object property, final String value)
		{
			Border border = null;
			//TODO: find better way to handle this for MAC running with java 1.7
			if (Utils.isAppleMacOS() && JavaVersion.CURRENT_JAVA_VERSION.major >= 7)
			{
				final Border[] fBorder = { null };
				try
				{
					DebugUtils.invokeAndWaitWhileDispatchingOnSWT(new Runnable()
					{
						public void run()
						{
							fBorder[0] = ComponentFactoryHelper.createBorder(value, true);
						}
					});
				}
				catch (Exception e)
				{
					ServoyLog.logError("Error creating border from " + value, e);
				}
				border = fBorder[0];
			}
			else
			{
				border = ComponentFactoryHelper.createBorder(value, true);
			}

			return border;
		}

		public String convertValue(Object property, Border value)
		{
			return ComponentFactoryHelper.createBorderString(value);
		}
	};

	protected static final String BEAN_PROPERTY_PREFIX_DOT = "bean.";

	public static final IPropertyConverter<String, String> NULL_STRING_CONVERTER = new IPropertyConverter<String, String>()
	{
		public String convertProperty(Object id, String value)
		{
			return value == null ? "" : value;
		}

		public String convertValue(Object id, String value)
		{
			return "".equals(value) ? null : value;
		}
	};
	public static IPropertyConverter<String, String> DEFAULT_STRING_CONVERTER = new IPropertyConverter<String, String>()
	{
		public String convertProperty(Object id, String value)
		{
			return value == null ? "DEFAULT" : value;
		}

		public String convertValue(Object id, String value)
		{
			return "DEFAULT".equals(value) ? null : value;
		}
	};

	// remember the font that was used last time the element was painted, use this as default for font cell editors.
	public static final RuntimeProperty<java.awt.Font> LastPaintedFontProperty = new RuntimeProperty<java.awt.Font>()
	{
	};

	// remember the background set for the object when painted
	public static final RuntimeProperty<java.awt.Color> LastPaintedBackgroundProperty = new RuntimeProperty<java.awt.Color>()
	{
	};

	protected PersistContext persistContext;
	protected boolean readOnly;

	// Property Descriptors
	private Map<Object, IPropertyDescriptor> propertyDescriptors;
	private Map<Object, IPropertyDescriptor> hiddenPropertyDescriptors;
	private Map<Object, PropertyDescriptorWrapper> beansProperties;

	public static PersistPropertySource createPersistPropertySource(IPersist persist, IPersist context, boolean readonly)
	{
		return createPersistPropertySource(PersistContext.create(persist, context), readonly);
	}

	public static PersistPropertySource createPersistPropertySource(IPersist persist, boolean readonly)
	{
		return createPersistPropertySource(PersistContext.create(persist), readonly);
	}

	public static PersistPropertySource createPersistPropertySource(PersistContext persistContext, boolean readonly)
	{
		PersistPropertySource persistPropertySource = (PersistPropertySource)Platform.getAdapterManager().getAdapter(persistContext, IPropertySource.class);
		if (persistPropertySource != null) persistPropertySource.setReadOnly(readonly);
		return persistPropertySource;
	}

	/*
	 * This constructor should only be called from the adapter factory, all other code should use createPersistPropertySource()
	 */
	public PersistPropertySource(PersistContext persistContext, boolean readonly)
	{
		this.persistContext = persistContext;
		this.readOnly = readonly;
	}

	public void setReadOnly(boolean readOnly)
	{
		if (this.readOnly != readOnly)
		{
			this.readOnly = readOnly;
			propertyDescriptors = null;
		}
	}

	public IPersist getPersist()
	{
		return persistContext.getPersist();
	}

	public IPersist getContext()
	{
		return persistContext.getContext();
	}

	public Object getSaveModel()
	{
		return persistContext.getPersist();
	}

	public Object getAdapter(Class adapter)
	{
		if (adapter == IPersist.class)
		{
			return persistContext.getPersist();
		}
		return null;
	}

	public Object getEditableValue()
	{
		return null;
	}

	protected void init()
	{
		if (propertyDescriptors == null)
		{
			FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());
			Form form = (Form)(Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()) ? persistContext.getContext()
				: persistContext.getPersist()).getAncestor(IRepository.FORMS);

			propertyDescriptors = new LinkedHashMap<Object, IPropertyDescriptor>();// defines order
			hiddenPropertyDescriptors = new HashMap<Object, IPropertyDescriptor>();
			beansProperties = new HashMap<Object, PropertyDescriptorWrapper>();

			Object valueObject = getValueObject(flattenedEditingSolution, form);
			for (IPropertyHandler element : sortBeansPropertyDescriptors(createPropertyHandlers(valueObject)))
			{
				try
				{
					registerProperty(new PropertyDescriptorWrapper(element, valueObject), flattenedEditingSolution, form);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not register property " + element.getName(), e);
				}
			}
			if (!(persistContext.getPersist() instanceof Bean))
			{
				// check for pseudo properties
				IPropertyHandler[] pseudoProperties = getPseudoProperties(persistContext.getPersist().getClass());
				if (pseudoProperties != null)
				{
					for (IPropertyHandler prop : pseudoProperties)
					{
						try
						{
							registerProperty(new PropertyDescriptorWrapper(prop, valueObject), flattenedEditingSolution, form);
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
			else
			{
				// bean: use size, location and name descriptors from the persist (Bean)
				try
				{
					for (java.beans.PropertyDescriptor element : java.beans.Introspector.getBeanInfo(persistContext.getPersist().getClass()).getPropertyDescriptors())
					{
						try
						{
							registerProperty(new PropertyDescriptorWrapper(new BeanPropertyHandler(element), persistContext.getPersist()),
								flattenedEditingSolution, form);
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError("Could not register property " + element.getName(), e);
						}


						if (propertyDescriptors.containsKey(element.getName()))
						{
							// if same property is defined in bean and persist, override with persist version
							propertyDescriptors.remove(BEAN_PROPERTY_PREFIX_DOT + element.getName());
						}
					}
				}
				catch (IntrospectionException e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	/**
	 * @param form
	 * @param flattenedEditingSolution
	 * @return
	 */
	protected Object getValueObject(FlattenedSolution flattenedEditingSolution, Form form)
	{
		Object valueObject = null;
		if (persistContext.getPersist() instanceof Bean) //step into the bean instance
		{
			valueObject = DesignComponentFactory.getBeanDesignInstance(Activator.getDefault().getDesignClient(), flattenedEditingSolution,
				(Bean)persistContext.getPersist(), form);
			if (valueObject instanceof InvisibleBean) //step into the invisible bean instance
			{
				valueObject = ((InvisibleBean)valueObject).getDelegate();
			}
		}
		else
		{
			valueObject = persistContext.getPersist();
		}

		return valueObject;
	}

	protected IPropertyHandler[] createPropertyHandlers(Object valueObject)
	{
		java.beans.BeanInfo info = null;
		try
		{
			if (valueObject != null)
			{
				info = java.beans.Introspector.getBeanInfo(valueObject.getClass());
			}
		}
		catch (java.beans.IntrospectionException e)
		{
			ServoyLog.logError(e);
		}

		if (info == null)
		{
			return new IPropertyHandler[0];
		}

		java.beans.PropertyDescriptor[] descs = info.getPropertyDescriptors();
		if (descs == null)
		{
			return new IPropertyHandler[0];
		}

		IPropertyHandler[] handlers = new IPropertyHandler[descs.length];
		for (int i = 0; i < descs.length; i++)
		{
			handlers[i] = valueObject == persistContext.getPersist() ? new PersistPropertyHandler(descs[i]) : new BeanPropertyHandler(descs[i]);
		}
		return handlers;
	}

	protected IPropertyHandler[] getPseudoProperties(Class< ? > clazz)
	{
		if (Solution.class == clazz)
		{
			return new IPropertyHandler[] { new PseudoPropertyHandler("loginSolutionName") };
		}
		if (Form.class == clazz || IFormElement.class.isAssignableFrom(clazz))
		{
			return new IPropertyHandler[] { new PseudoPropertyHandler("designTimeProperties") };
		}
		return null;
	}

	private void registerProperty(PropertyDescriptorWrapper propertyDescriptor, FlattenedSolution flattenedEditingSolution, Form form)
		throws RepositoryException
	{
		if (persistContext.getPersist() == propertyDescriptor.valueObject // for beans we show all
			&&
			!shouldShow(propertyDescriptor))
		{
			return;
		}

		PropertyCategory category;
		String id;
		if (propertyDescriptor.valueObject == persistContext.getPersist())
		{
			category = createPropertyCategory(propertyDescriptor);
			id = propertyDescriptor.propertyDescriptor.getName();
		}
		else
		{
			category = PropertyCategory.Component;
			id = BEAN_PROPERTY_PREFIX_DOT + propertyDescriptor.propertyDescriptor.getName();
		}

		IPropertyDescriptor pd = createPropertyDescriptor(propertyDescriptor, flattenedEditingSolution, form, id);
		setCategory(pd, category);
		IPropertyDescriptor combinedPropertyDesciptor = null;
		if (propertyDescriptor.valueObject == persistContext.getPersist())
		{
			combinedPropertyDesciptor = createCombinedPropertyDescriptor(propertyDescriptor, form);
		}
		beansProperties.put(pd != null ? pd.getId() : propertyDescriptor.propertyDescriptor.getName(), propertyDescriptor);
		if (pd != null)
		{
			if (readOnly)
			{
				// Add a wrapper to enforce readonly unless the IPropertyController handles read-only self
				if (!(pd instanceof IPropertyController) || !((IPropertyController< ? , ? >)pd).supportsReadonly())
				{
					pd = new ReadonlyPropertyController(pd);
				}
			}
			else
			{
				// Add a wrapper that checks readonly of the other pd in isCompatibleWith()
				pd = new DelegatePropertyController(pd, pd.getId());
			}

			if (propertyDescriptor.valueObject == persistContext.getPersist() && hideForProperties(propertyDescriptor))
			{
				hiddenPropertyDescriptors.put(pd.getId(), pd);
			}
			else
			{
				propertyDescriptors.put(pd.getId(), pd);
			}

			if (combinedPropertyDesciptor != null)
			{
				propertyDescriptors.put(combinedPropertyDesciptor.getId(), combinedPropertyDesciptor);
			}
		}
	}

	/**
	 * @param propertyDescriptor
	 * @return
	 */
	protected PropertyCategory createPropertyCategory(PropertyDescriptorWrapper propertyDescriptor)
	{
		return PropertyCategory.createPropertyCategory(propertyDescriptor.propertyDescriptor.getName());
	}

	/**
	 * @param propertyDescriptor
	 * @param flattenedEditingSolution
	 * @param form
	 * @param category
	 * @param id
	 * @return
	 * @throws RepositoryException
	 */
	protected IPropertyDescriptor createPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptorWrapper, FlattenedSolution flattenedEditingSolution,
		Form form, String id) throws RepositoryException
	{
		return createPropertyDescriptor(this, id, persistContext, readOnly, propertyDescriptorWrapper,
			RepositoryHelper.getDisplayName(propertyDescriptorWrapper.propertyDescriptor.getName(), persistContext.getPersist().getClass()),
			flattenedEditingSolution, form);
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		init();
		IPropertyDescriptor[] descs = propertyDescriptors.values().toArray(new IPropertyDescriptor[propertyDescriptors.size()]);
		if (persistContext.getPersist() instanceof Bean)
		{
			return PropertyController.applySequencePropertyComparator(descs);
		}
		return descs;
	}

	public IPropertyDescriptor getPropertyDescriptor(Object id)
	{
		init();

		// recursively process a.b.c properties
		if (id instanceof String && !((String)id).startsWith(BEAN_PROPERTY_PREFIX_DOT))
		{
			String propId = (String)id;
			int dot = propId.indexOf('.');
			if (dot > 0)
			{
				Object propertyValue = getPropertyValue(propId.substring(0, dot));
				if (propertyValue instanceof IAdaptable)
				{
					IPropertySource propertySource = (IPropertySource)((IAdaptable)propertyValue).getAdapter(IPropertySource.class);
					if (propertySource != null)
					{
						String subProp = propId.substring(dot + 1);
						IPropertyDescriptor[] descs = propertySource.getPropertyDescriptors();
						if (descs == null)
						{
							return null;
						}
						for (IPropertyDescriptor desc : descs)
						{
							if (subProp.equals(desc.getId()))
							{
								return desc;
							}
						}
					}
				}
			}
		}

		IPropertyDescriptor propertyDescriptor = propertyDescriptors.get(id);
		if (propertyDescriptor == null)
		{
			propertyDescriptor = hiddenPropertyDescriptors.get(id);
		}
		return propertyDescriptor;
	}

	protected Map<Object, PropertyDescriptorWrapper> getBeansProperties()
	{
		init();
		return beansProperties;
	}

	public static IPropertyDescriptor createPropertyDescriptor(IPropertySource propertySource, final String id, final PersistContext persistContext,
		boolean readOnly, PropertyDescriptorWrapper propertyDescriptor, String displayName, FlattenedSolution flattenedEditingSolution, Form form)
		throws RepositoryException
	{
		if (!propertyDescriptor.propertyDescriptor.isProperty())
		{
			return null;
		}

		IPropertyDescriptor desc = createPropertyDescriptor(propertySource, persistContext, readOnly, propertyDescriptor, id, displayName,
			flattenedEditingSolution, form);
		if (desc != null //
			&&
			persistContext != null &&
			persistContext.getPersist() != null &&
			(persistContext.getPersist().getParent() == persistContext.getContext() || (persistContext.getPersist().getParent() instanceof TabPanel && persistContext.getPersist().getParent().getParent() == persistContext.getContext())) // only show overrides when element is shown in its 'own' form or in a tabPanel of the same form
			&&
			// skip some specific properties
			!(persistContext.getPersist() instanceof Form && StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(id)) &&
			!StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(id))
		{
			return createDelegatePropertyControllerForInheritance(persistContext.getPersist(), desc, desc.getId().toString());
		}
		return desc;
	}

	private static DelegatePropertyController createDelegatePropertyControllerForInheritance(final IPersist persist, final IPropertyDescriptor desc,
		final String id)
	{
		return new DelegatePropertyController(desc, desc.getId())
		{
			@Override
			public ILabelProvider getLabelProvider()
			{
				String propertyId = id;
				if (persist instanceof Form && StaticContentSpecLoader.PROPERTY_WIDTH.getPropertyName().equals(propertyId))
				{
					propertyId = StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName();
				}
				return new PersistInheritenceDelegateLabelProvider(persist, super.getLabelProvider(), propertyId);
			}
		};
	}

	private static void setCategory(IPropertyDescriptor desc, PropertyCategory category)
	{
		if (category != null)
		{
			if (desc instanceof org.eclipse.ui.views.properties.PropertyDescriptor)
			{
				((org.eclipse.ui.views.properties.PropertyDescriptor)desc).setCategory(category.name());
			}
			else if (desc instanceof DelegatePropertyController)
			{
				((DelegatePropertyController)desc).setCategory(category.name());
			}
		}
	}

	protected IPropertyDescriptor createCombinedPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor, Form form)
	{
		PropertyCategory category = PropertyCategory.createPropertyCategory(propertyDescriptor.propertyDescriptor.getName());
		DelegatePropertyController desc = getCombinedPropertyDescriptor(propertyDescriptor, getDisplayName(propertyDescriptor.propertyDescriptor.getName()),
			category, form);
		if (desc != null)
		{
			desc.setCategory(category.name());
		}

		return desc;
	}

	private String getDisplayName(String name)
	{
		return RepositoryHelper.getDisplayName(name, persistContext.getPersist().getClass());
	}

	private static IPropertyDescriptor createPropertyDescriptor(IPropertySource propertySource, final PersistContext persistContext, final boolean readOnly,
		final PropertyDescriptorWrapper propertyDescriptor, final String id, String displayName, final FlattenedSolution flattenedEditingSolution,
		final Form form) throws RepositoryException
	{
		PropertyDescription propertyDescription = propertyDescriptor.propertyDescriptor.getPropertyDescription(propertyDescriptor.valueObject, propertySource,
			persistContext);
		if (propertyDescription != null && propertyDescription.getType() == null && propertyDescription.getConfig() instanceof IPropertyDescriptor)
		{
			// internal properties, config is propertycontroller
			return (IPropertyDescriptor)propertyDescription.getConfig();
		}

		IPropertyType< ? > propertyType = (propertyDescription == null ? null : propertyDescription.getType());

		/*
		 * Category based property controllers.
		 */
		if (propertyType != null && FunctionPropertyType.INSTANCE.getClass().isAssignableFrom(propertyType.getClass()))
		{
			final Table table = form == null ? null : form.getTable();
			return new MethodPropertyController<Integer>(id, displayName, persistContext, new MethodListOptions(true,
				Boolean.TRUE.equals(propertyDescription.getConfig()), form != null, true, allowFoundsetMethods(persistContext, id) && table != null, table))
			{
				@Override
				protected IPropertyConverter<Integer, Object> createConverter()
				{
					IPropertyConverter<Integer, MethodWithArguments> id2MethodsWithArgumentConverter = new IPropertyConverter<Integer, MethodWithArguments>()
					{
						public MethodWithArguments convertProperty(Object id, Integer value)
						{
							if (value == null) return null;

							SafeArrayList<Object> args = null;
							SafeArrayList<Object> params = null;
							if (persistContext != null && persistContext.getPersist() instanceof AbstractBase)
							{
								Pair<List<Object>, List<Object>> instanceParamsArgs = ((AbstractBase)persistContext.getPersist()).getInstanceMethodParametersLocal(id.toString());
								if (instanceParamsArgs != null)
								{
									if (instanceParamsArgs.getLeft() == null)
									{ //solution is transitioning to updated frm version which includes parameter names in the frm json
										IScriptProvider scriptMethod = ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(),
											table, value);
										if (scriptMethod != null)
										{
											MethodArgument[] formalArguments = ((AbstractBase)scriptMethod).getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
											params = new SafeArrayList<Object>();
											for (MethodArgument methodArgument : formalArguments)
											{
												params.add(methodArgument.getName());
											}
										}
										else
										{
											params = new SafeArrayList<Object>(new ArrayList<Object>());
										}
									}
									else
									{
										params = new SafeArrayList<Object>(instanceParamsArgs.getLeft());
									}
									args = new SafeArrayList<Object>(instanceParamsArgs.getRight() == null ? new ArrayList<Object>()
										: instanceParamsArgs.getRight());
								}
							}
							return new MethodWithArguments(value.intValue(), params, args, table);
						}

						public Integer convertValue(Object id, MethodWithArguments value)
						{
							if (persistContext != null) setInstancMethodArguments(persistContext.getPersist(), id, value.paramNames, value.arguments);
							return Integer.valueOf(value.methodId);
						}
					};

					if (persistContext != null && persistContext.getContext() instanceof Form && ((Form)persistContext.getContext()).getExtendsID() > 0)
					{
						// convert to the actual method called according to form inheritance
						id2MethodsWithArgumentConverter = new ChainedPropertyConverter<Integer, MethodWithArguments, MethodWithArguments>(
							id2MethodsWithArgumentConverter, new FormInheritenceMethodConverter(persistContext));
					}

					ComplexProperty.ComplexPropertyConverter<MethodWithArguments> mwa2complexConverter = new ComplexProperty.ComplexPropertyConverter<MethodWithArguments>()
					{
						@Override
						public Object convertProperty(Object id, MethodWithArguments value)
						{
							return new ComplexProperty<MethodWithArguments>(value)
							{
								@Override
								public IPropertySource getPropertySource()
								{
									return new MethodPropertySource(this, persistContext, table, getId().toString(), isReadOnly());
								}
							};
						}

						@Override
						public MethodWithArguments convertValue(Object id, Object value)
						{
							if (value == null || value instanceof MethodWithArguments)
							{
								return (MethodWithArguments)value;
							}
							return ((ComplexProperty<MethodWithArguments>)value).getValue();
						}
					};

					return new ChainedPropertyConverter<Integer, MethodWithArguments, Object>(id2MethodsWithArgumentConverter, mwa2complexConverter);
				}
			};
		}

		/*
		 * Property editors defined by beans
		 */

		if (propertyDescriptor.propertyDescriptor instanceof BeanPropertyHandler)
		{
			final BeanPropertyHandler beanHandler = (BeanPropertyHandler)propertyDescriptor.propertyDescriptor;
			Class< ? > propertyEditorClass = beanHandler.getPropertyEditorClass();
			try
			{
				if (propertyEditorClass != null && PropertyEditor.class.isAssignableFrom(propertyEditorClass))
				{
					if (propertyDescriptor.getPropertyEditor() == null)
					{
						propertyDescriptor.setPropertyEditor((PropertyEditor)propertyEditorClass.newInstance());
					}

					String[] tags = propertyDescriptor.getPropertyEditor().getTags();
					if (tags != null && tags.length > 0)
					{
						// list of values
						return new BeanTagsPropertyController(id, displayName, propertyDescriptor.getPropertyEditor(), Messages.LabelUnresolved);
					}

					if (propertyDescriptor.getPropertyEditor().supportsCustomEditor())
					{
						// has its own editor
						final LabelProvider labelProvider = new LabelProvider()
						{
							@Override
							public String getText(Object element)
							{
								String text = null;
								try
								{
									propertyDescriptor.getPropertyEditor().setValue(element);
									text = propertyDescriptor.getPropertyEditor().getAsText();
								}
								catch (RuntimeException e)
								{
									ServoyLog.logError("Could not create get property value " + id, e);
								}
								if (text == null)
								{
									return super.getText(element);
								}
								return text;
							}
						};
						PropertyDescriptor beanPropertyDescriptor = new PropertyDescriptor(id, displayName)
						{
							@Override
							public CellEditor createPropertyEditor(Composite parent)
							{
								return new BeanCustomCellEditor(parent, propertyDescriptor.getPropertyEditor(), labelProvider, beanHandler.getName(),
									"Bean custom property editor", readOnly);
							}
						};
						beanPropertyDescriptor.setLabelProvider(labelProvider);
						return beanPropertyDescriptor;
					}

					try
					{
						if (propertyDescriptor.getPropertyEditor().getAsText() != null)
						{
							// can be edited as text
							return new PropertyController<Object, String>(id, displayName, new BeanAsTextPropertyConverter(
								propertyDescriptor.getPropertyEditor()), null, new ICellEditorFactory()
							{
								public CellEditor createPropertyEditor(Composite parent)
								{
									return new TextCellEditor(parent);
								}
							});
						}
					}
					catch (RuntimeException e)
					{
						// can not be edited as text
						// TODO: trace message ?
					}
				}
			}
			catch (InstantiationException e)
			{
				ServoyLog.logError("Could not create property editor " + propertyEditorClass.getName(), e);
			}
			catch (IllegalAccessException e)
			{
				ServoyLog.logError("Could not create property editor " + propertyEditorClass.getName(), e);
			}

			// property editors for beans, use property editor hints if given
			Object hintValue = beanHandler.getAttributeValue(PropertyEditorHint.PROPERTY_EDITOR_HINT);
			final PropertyEditorHint propertyEditorHint;
			if (hintValue instanceof PropertyEditorHint)
			{
				propertyEditorHint = (PropertyEditorHint)hintValue;
			}
			else if (beanHandler.getPropertyType() == FunctionDefinition.class)
			{
				// use defaults for FunctionDefinition property without specified hint
				propertyEditorHint = new PropertyEditorHint(PropertyEditorClass.method);
			}
			else
			{
				propertyEditorHint = null;
			}

			if (propertyEditorHint != null && form != null)
			{
				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.method)
				{
					// accept String or FunctionDefinition properties

					Class< ? > propertyClass = beanHandler.getPropertyType();
					// convert a FunctionDefinition to MethodWithArguments
					IPropertyConverter<FunctionDefinition, MethodWithArguments> methodsWithArgumentConverter = new IPropertyConverter<FunctionDefinition, MethodWithArguments>()
					{
						public MethodWithArguments convertProperty(Object id, FunctionDefinition value)
						{
							if (value == null) return null;

							FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(),
								persistContext.getContext());
							Iterator<ScriptMethod> scriptMethods = null;
							String scopeName = value.getScopeName();
							if (scopeName != null)
							{
								// global method
								scriptMethods = flattenedSolution.getScriptMethods(scopeName, false);
							}
							else
							{
								// form method
								Form methodForm = flattenedSolution.getForm(value.getContextName());
								if (methodForm != null)
								{
									scriptMethods = methodForm.getScriptMethods(false);
								}
							}
							if (scriptMethods != null)
							{
								ScriptMethod scriptMethod = AbstractBase.selectByName(scriptMethods, value.getMethodName());
								if (scriptMethod != null)
								{
									return MethodWithArguments.create(scriptMethod, null);
								}
							}
							return new UnresolvedMethodWithArguments(value.toMethodString());
						}

						public FunctionDefinition convertValue(Object id, MethodWithArguments value)
						{
							IScriptProvider scriptMethod = ModelUtils.getScriptMethod(persistContext.getPersist(), persistContext.getContext(), value.table,
								value.methodId);
							if (scriptMethod != null)
							{
								String formName = (scriptMethod.getParent() instanceof Form) ? ((Form)scriptMethod.getParent()).getName() : null;
								return new FunctionDefinition(formName, scriptMethod.getName());
							}
							return null;
						}
					};

					if (persistContext.getContext() instanceof Form && ((Form)persistContext.getContext()).getExtendsID() > 0)
					{
						// convert to the actual method called according to form inheritance
						methodsWithArgumentConverter = new ChainedPropertyConverter<FunctionDefinition, MethodWithArguments, MethodWithArguments>(
							methodsWithArgumentConverter, new FormInheritenceMethodConverter(persistContext));
					}

					final IPropertyConverter<FunctionDefinition, Object> functionDefinitionPropertyConverter = new ChainedPropertyConverter<FunctionDefinition, MethodWithArguments, Object>(
						methodsWithArgumentConverter, PropertyCastingConverter.<MethodWithArguments, Object> propertyCastingConverter());

					if (propertyClass == FunctionDefinition.class)
					{
						return new MethodPropertyController<FunctionDefinition>(id, displayName, persistContext, new MethodListOptions(
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), false,
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeForm)),
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobal)),
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeFoundset)), form == null ? null : form.getTable()))
						{
							@Override
							protected IPropertyConverter<FunctionDefinition, Object> createConverter()
							{
								return functionDefinitionPropertyConverter;
							}
						};
					}

					if (propertyClass == String.class)
					{
						// chain with String to FunctionDefinition converter
						return new MethodPropertyController<String>(id, displayName, persistContext, new MethodListOptions(
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), false,
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeForm)),
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobal)),
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeFoundset)), form == null ? null : form.getTable()))
						{
							@Override
							protected IPropertyConverter<String, Object> createConverter()
							{
								IPropertyConverter<String, FunctionDefinition> string2FunctionDefinitionConverter = new IPropertyConverter<String, FunctionDefinition>()
								{
									public String convertValue(Object id, FunctionDefinition value)
									{
										if (value == null) return null;
										return value.toMethodString();
									}

									public FunctionDefinition convertProperty(Object id, String value)
									{
										if (value == null) return null;
										return new FunctionDefinition(value);
									}
								};
								return new ChainedPropertyConverter<String, FunctionDefinition, Object>(string2FunctionDefinitionConverter,
									functionDefinitionPropertyConverter);
							}
						};
					}

					// not String or FunctionDefinition
					return null;
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.dataprovider && beanHandler.getPropertyType() == String.class)
				{
					// String property, select a data provider
					Table table = null;
					try
					{
						table = flattenedEditingSolution.getFlattenedForm(form).getTable();
					}
					catch (RepositoryException ex)
					{
						ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
						return null;
					}

					INCLUDE_RELATIONS includeRelations;
					if (Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeRelations)))
					{
						if (Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNestedRelations)))
						{
							includeRelations = INCLUDE_RELATIONS.NESTED;
						}
						else
						{
							includeRelations = INCLUDE_RELATIONS.YES;
						}
					}
					else
					{
						includeRelations = INCLUDE_RELATIONS.NO;
					}
					boolean includeGlobalRelations;
					if (propertyEditorHint.getOption(PropertyEditorOption.includeGlobalRelations) != null)
					{
						includeGlobalRelations = Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobalRelations));
					}
					else
					{
						includeGlobalRelations = Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobal));
					}


					final DataProviderOptions options = new DataProviderTreeViewer.DataProviderOptions(
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), table != null &&
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeColumns)), table != null &&
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeCalculations)), table != null &&
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeRelatedCalculations)),
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeForm)),
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobal)), table != null &&
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeAggregates)), table != null &&
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeRelatedAggregates)), includeRelations,
						includeGlobalRelations, true, null);
					final DataProviderConverter converter = new DataProviderConverter(flattenedEditingSolution, persistContext.getPersist(), table);
					DataProviderLabelProvider showPrefix = new DataProviderLabelProvider(false);
					showPrefix.setConverter(converter);
					DataProviderLabelProvider hidePrefix = new DataProviderLabelProvider(true);
					hidePrefix.setConverter(converter);

					ILabelProvider labelProviderShowPrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(showPrefix,
						persistContext.getContext()), persistContext.getContext());
					final ILabelProvider labelProviderHidePrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(hidePrefix,
						persistContext.getContext()), persistContext.getContext());
					PropertyController<String, String> propertyController = new PropertyController<String, String>(id, displayName, null,
						labelProviderShowPrefix, new ICellEditorFactory()
						{
							public CellEditor createPropertyEditor(Composite parent)
							{
								return new DataProviderCellEditor(parent, labelProviderHidePrefix, new DataProviderValueEditor(converter), form,
									flattenedEditingSolution, readOnly, options, converter);
							}
						});
					propertyController.setSupportsReadonly(true);
					return propertyController;
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.relation && beanHandler.getPropertyType() == String.class)
				{
					// String property, select a relation
					Table primaryTable = null;
					if (form != null)
					{
						try
						{
							primaryTable = flattenedEditingSolution.getFlattenedForm(form).getTable();
						}
						catch (RepositoryException ex)
						{
							ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
						}
					}

					return new RelationPropertyController(id, displayName, persistContext, primaryTable, null /* foreignTable */,
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)),
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNestedRelations)));
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.form && beanHandler.getPropertyType() == String.class)
				{
					// String property, select a form
					final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, false),
						persistContext.getContext());
					PropertyDescriptor pd = new PropertyController<String, Integer>(id, displayName)
					{
						@Override
						public CellEditor createPropertyEditor(Composite parent)
						{
							return new ListSelectCellEditor(parent, "Select form",
								new FormContentProvider(flattenedEditingSolution, null /* persist is solution */), formLabelProvider, new FormValueEditor(
									flattenedEditingSolution), readOnly, new FormContentProvider.FormListOptions(FormListOptions.FormListType.FORMS, null,
									Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), false, false), SWT.NONE, null,
								"Select form dialog");
						}

						@Override
						protected IPropertyConverter<String, Integer> createConverter()
						{
							// convert between form ids and form name
							return new IPropertyConverter<String, Integer>()
							{
								public Integer convertProperty(Object id, String value)
								{
									Form f = flattenedEditingSolution.getForm(value);
									return Integer.valueOf(f == null ? Form.NAVIGATOR_NONE : f.getID());
								}

								public String convertValue(Object id, Integer value)
								{
									int formId = value.intValue();
									if (formId == Form.NAVIGATOR_NONE) return null;
									Form f = flattenedEditingSolution.getForm(formId);
									return f == null ? null : f.getName();
								}
							};
						}
					};
					pd.setLabelProvider(formLabelProvider);
					return pd;
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.valuelist && beanHandler.getPropertyType() == String.class)
				{
					// String property, select a value list
					return new ValuelistPropertyController<String>(id, displayName, persistContext,
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)))
					{
						@Override
						protected IPropertyConverter<String, Integer> createConverter()
						{
							// convert between value list ids and value list name
							return new IPropertyConverter<String, Integer>()
							{
								public Integer convertProperty(Object id, String value)
								{
									ValueList vl = flattenedEditingSolution.getValueList(value);
									return Integer.valueOf(vl == null ? ValuelistLabelProvider.VALUELIST_NONE : vl.getID());
								}

								public String convertValue(Object id, Integer value)
								{
									int vlId = value.intValue();
									if (vlId == ValuelistLabelProvider.VALUELIST_NONE) return null;
									ValueList vl = flattenedEditingSolution.getValueList(vlId);
									return vl == null ? null : vl.getName();
								}
							};
						}
					};
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.media && beanHandler.getPropertyType() == String.class)
				{
					// String property, select an image
					MediaPropertyControllerConfig config = null;
					if (propertyDescription != null && propertyDescription.getConfig() instanceof MediaPropertyControllerConfig) config = (MediaPropertyControllerConfig)propertyDescription.getConfig();
					return new MediaNamePropertyController(id, displayName, persistContext, flattenedEditingSolution,
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), config);
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.styleclass && beanHandler.getPropertyType() == String.class)
				{
					// String property, select a style class
					Object option = propertyEditorHint.getOption(PropertyEditorOption.styleLookupName);
					if (option instanceof String)
					{
						return createStyleClassPropertyController(persistContext.getPersist(), id, displayName, (String)option, form);
					}
				}
			}
		}

		if (propertyType != null)
		{
			/*
			 * Type based property controllers.
			 */
			if (DimensionPropertyType.TYPE_NAME.equals(propertyType.getName()))
			{
				final java.awt.Dimension defaultDimension = id.equals("intercellSpacing") ? new Dimension(1, 1) : new Dimension(0, 0);
				return new PropertyController<java.awt.Dimension, Object>(id, displayName, new ComplexPropertyConverter<java.awt.Dimension>()
				{
					@Override
					public Object convertProperty(Object id, java.awt.Dimension value)
					{
						return new ComplexProperty<java.awt.Dimension>(value)
						{
							@Override
							public IPropertySource getPropertySource()
							{
								DimensionPropertySource dimensionPropertySource = new DimensionPropertySource(this, defaultDimension);
								dimensionPropertySource.setReadonly(readOnly);
								return dimensionPropertySource;
							}
						};
					}

				}, DimensionPropertySource.getLabelProvider(), new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return DimensionPropertySource.createPropertyEditor(parent);
					}
				});
			}

			if (PointPropertyType.TYPE_NAME.equals(propertyType.getName()))
			{
				return new PropertyController<java.awt.Point, Object>(id, displayName, new ComplexPropertyConverter<java.awt.Point>()
				{
					@Override
					public Object convertProperty(Object id, java.awt.Point value)
					{
						return new ComplexProperty<java.awt.Point>(value)
						{
							@Override
							public IPropertySource getPropertySource()
							{
								PointPropertySource pointPropertySource = new PointPropertySource(this);
								pointPropertySource.setReadonly(readOnly);
								return pointPropertySource;
							}
						};
					}
				}, PointPropertySource.getLabelProvider(), new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return PointPropertySource.createPropertyEditor(parent);
					}
				});
			}

			if (InsetsPropertyType.TYPE_NAME.equals(propertyType.getName()))
			{
				return new PropertyController<java.awt.Insets, Object>(id, displayName, new ComplexPropertyConverter<java.awt.Insets>()
				{
					@Override
					public Object convertProperty(Object property, java.awt.Insets value)
					{
						return new ComplexProperty<Insets>(value)
						{
							@Override
							public IPropertySource getPropertySource()
							{
								InsetsPropertySource insetsPropertySource = new InsetsPropertySource(this);
								insetsPropertySource.setReadonly(readOnly);
								return insetsPropertySource;
							}
						};
					}
				}, InsetsPropertySource.getLabelProvider(), new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return InsetsPropertySource.createPropertyEditor(parent);
					}
				});
			}

			if (ColorPropertyType.TYPE_NAME.equals(propertyType.getName()))
			{
				return new ColorPropertyController(id, displayName);
			}

			if (propertyType == BorderPropertyType.INSTANCE)
			{
				BorderPropertyController borderPropertyController = new BorderPropertyController(id, displayName, propertySource, persistContext);
				borderPropertyController.setReadonly(readOnly);

				if (Boolean.TRUE.equals(propertyDescription.getConfig()))
				{
					// BorderPropertyController handles Border objects, the property is a String.
					return new PropertyController<String, Object>(id, displayName, new ChainedPropertyConverter<String, Border, Object>(
						BORDER_STRING_CONVERTER, borderPropertyController.getConverter()), borderPropertyController.getLabelProvider(),
						borderPropertyController);
				}

				return borderPropertyController;
			}

			if (propertyType == BooleanPropertyType.INSTANCE || propertyType.isProtecting())
			{
				return new CheckboxPropertyDescriptor(id, displayName);
			}

			IPropertyDescriptor retval = getGeneralPropertyDescriptor(persistContext, readOnly, id, displayName, flattenedEditingSolution);
			if (retval != null)
			{
				return retval;
			}

			if (propertyType == ScrollbarsPropertyType.INSTANCE)
			{
				return new PropertyController<Integer, Object>(id, displayName, new ComplexProperty.ComplexPropertyConverter<Integer>()
				{
					@Override
					public Object convertProperty(Object property, Integer value)
					{
						return new ComplexProperty<Integer>(value)
						{
							@Override
							public IPropertySource getPropertySource()
							{
								ScrollbarSettingPropertySource scrollbarSettingPropertySource = new ScrollbarSettingPropertySource(this);
								scrollbarSettingPropertySource.setReadonly(readOnly);
								return scrollbarSettingPropertySource;
							}
						};
					}
				}, ScrollbarSettingLabelProvider.INSTANCE, new DummyCellEditorFactory(ScrollbarSettingLabelProvider.INSTANCE));
			}

			if (propertyType == StyleClassPropertyType.INSTANCE)
			{
				return createStyleClassPropertyController(persistContext.getPersist(), id, displayName, null, form);
			}

			retval = getPropertiesPropertyDescriptor(propertyDescriptor, persistContext, readOnly, id, displayName, propertyDescription,
				flattenedEditingSolution, form);
			if (retval != null)
			{
				return retval;
			}

			if (propertyType == StringPropertyType.INSTANCE)
			{
				return new PropertyController<String, String>(id, displayName, NULL_STRING_CONVERTER, null, new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new TextCellEditor(parent);
					}
				});
			}

			final int type;
			if (propertyType == BytePropertyType.INSTANCE)
			{
				type = NumberCellEditor.BYTE;
			}
			else if (propertyType == DoublePropertyType.INSTANCE)
			{
				type = NumberCellEditor.DOUBLE;
			}
			else if (propertyType == FloatPropertyType.INSTANCE)
			{
				type = NumberCellEditor.FLOAT;
			}
			else if (propertyType == IntPropertyType.INSTANCE)
			{
				type = NumberCellEditor.INTEGER;
			}
			else if (propertyType == TabSeqPropertyType.INSTANCE)
			{
				type = NumberCellEditor.INTEGER;
			}
			else if (propertyType == LongPropertyType.INSTANCE)
			{
				type = NumberCellEditor.LONG;
			}
//		else if (propertyType == TypesRegistry.shortnumber)
//		{
//			type = NumberCellEditor.SHORT;
//		}
			else
			{
				type = -1;
			}
			if (type != -1)
			{
				return new PropertyDescriptor(id, displayName)
				{
					@Override
					public CellEditor createPropertyEditor(Composite parent)
					{
						NumberCellEditor editor = new NumberCellEditor(parent);
						editor.setType(type);
						return editor;
					}
				};
			}
		}
		// other bean properties
		if (propertyDescriptor.propertyDescriptor instanceof BeanPropertyHandler)
		{
			return new PropertyController<Object, Object>(id, displayName, new ComplexPropertyConverter<Object>()
			{
				@Override
				public Object convertProperty(Object id, Object value)
				{
					return new ComplexProperty<Object>(value)
					{
						@Override
						public IPropertySource getPropertySource()
						{
							BeanSubpropertyPropertySource ps = new BeanSubpropertyPropertySource(this, propertyDescriptor.valueObject,
								(BeanPropertyHandler)propertyDescriptor.propertyDescriptor, flattenedEditingSolution, form);
							ps.setReadonly(readOnly);
							return ps;
						}
					};
				}

			}, null, new DummyCellEditorFactory(NullDefaultLabelProvider.LABEL_DEFAULT));
		}

		return null;
	}

	/**
	 * For which properties should foundset methods be allowed.
	 *
	 * @param persistContext
	 * @param id
	 * @return
	 */
	private static boolean allowFoundsetMethods(PersistContext persistContext, String id)
	{
		if (persistContext.getPersist() instanceof Form && StaticContentSpecLoader.PROPERTY_ONLOADMETHODID.getPropertyName().equals(id))
		{
			return false; // in onload the foundset is not initialized yet
		}

		Object ancestor = persistContext.getContext().getAncestor(IRepository.FORMS);
		if (ancestor instanceof Form)
		{
			Form f = (Form)ancestor;
			return (f != null && f.getSolution().getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE);
		}

		return true;
	}

	/**
	 * Create a property controller for selecting a style class in Properties view.
	 *
	 * @param id
	 * @param displayName
	 * @param styleLookupname
	 * @return
	 */
	public static PropertyController<String, ? > createStyleClassPropertyController(IPersist persist, String id, String displayName,
		final String styleLookupname, Form form)
	{
		final StyleClassesComboboxModel model = new StyleClassesComboboxModel(form, persist instanceof IFormElement ? (IFormElement)persist : null, id,
			styleLookupname);
		final boolean newBehavior = ((Solution)persist.getRootObject()).getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE &&
			((Solution)persist.getRootObject()).getStyleSheetID() > 0;
		return new PropertyController<String, String>(id, displayName, NULL_STRING_CONVERTER, NullDefaultLabelProvider.LABEL_DEFAULT, new ICellEditorFactory()
		{
			public CellEditor createPropertyEditor(Composite parent)
			{
				return new StyleClassCellEditor(parent, model, newBehavior);
			}
		});
	}

	/**
	 * Get property descriptor that maps multiple properties into 1
	 *
	 * @param propertyDescriptor
	 * @param displayName
	 * @param category
	 * @return
	 */
	private DelegatePropertyController getCombinedPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor, String displayName,
		PropertyCategory category, Form form)
	{
		if (propertyDescriptor.valueObject == persistContext.getPersist()) // name based props for IPersists only
		{
			String name = propertyDescriptor.propertyDescriptor.getName();

			if (name.equals("containsFormID"))
			{
				return createDelegatePropertyControllerForInheritance(persistContext.getPersist(), new RelatedTabController("containsForm", "containsForm",
					"Select tab form", readOnly, form, ModelUtils.getEditingFlattenedSolution(persistContext.getPersist())), "containsFormID");
			}
		}
		return null;
	}


	//remove some properties based on the 'known' name
	protected boolean shouldShow(PropertyDescriptorWrapper propertyDescriptor)
	{
		if (((SolutionMetaData)persistContext.getPersist().getRootObject().getMetaData()).getSolutionType() == SolutionMetaData.NG_CLIENT_ONLY &&
			!propertyDescriptor.propertyDescriptor.hasSupportForClientType(persistContext.getPersist(), ClientSupport.ng))
		{
			return false;
		}
		return propertyDescriptor.propertyDescriptor.shouldShow(persistContext.getPersist());
	}

	protected boolean hideForProperties(PropertyDescriptorWrapper propertyDescriptor)
	{
		return RepositoryHelper.hideForProperties(propertyDescriptor.propertyDescriptor.getName(), persistContext.getPersist().getClass(),
			persistContext.getPersist());
	}

	public Object getPersistPropertyValue(Object id)
	{
		init();
		PropertyDescriptorWrapper beanPropertyDescriptor = getBeansProperties().get(id);
		if (beanPropertyDescriptor != null)
		{
			try
			{
				return beanPropertyDescriptor.propertyDescriptor.getValue(beanPropertyDescriptor.valueObject, persistContext);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Could not get property value for id " + id + " on object " + beanPropertyDescriptor.valueObject, e);
			}
		}
		return null;
	}

	public Object getPropertyValue(Object id)
	{
		// recursively process a.b.c properties
		if (id instanceof String && !((String)id).startsWith(BEAN_PROPERTY_PREFIX_DOT))
		{
			String propId = (String)id;
			int dot = propId.indexOf('.');
			if (dot > 0)
			{
				Object propertyValue = getPropertyValue(propId.substring(0, dot));
				if (propertyValue instanceof IAdaptable)
				{
					IPropertySource propertySource = (IPropertySource)((IAdaptable)propertyValue).getAdapter(IPropertySource.class);
					if (propertySource != null)
					{
						return propertySource.getPropertyValue(propId.substring(dot + 1));
					}
				}
			}
		}

		IPropertyDescriptor propertyDescriptor = getPropertyDescriptor(id);
		while (propertyDescriptor instanceof IDelegate && !(propertyDescriptor instanceof IPropertySetter))
		{
			Object delegate = ((IDelegate)propertyDescriptor).getDelegate();
			if (delegate instanceof IPropertyDescriptor)
			{
				propertyDescriptor = (IPropertyDescriptor)delegate;
			}
			else
			{
				break;
			}
		}
		Object propertyValue;
		if (propertyDescriptor instanceof IPropertySetter)
		{
			// a combined property
			propertyValue = ((IPropertySetter)propertyDescriptor).getProperty(this);
		}
		else
		{
			propertyValue = getPersistPropertyValue(id);
		}
		return convertGetPropertyValue(id, propertyDescriptor, propertyValue);
	}

	public static Object convertGetPropertyValue(Object id, IPropertyDescriptor propertyDescriptor, Object rawValue)
	{
		Object value = rawValue;
		if (propertyDescriptor instanceof IPropertyController)
		{
			IPropertyConverter propertyConverter = ((IPropertyController)propertyDescriptor).getConverter();
			if (propertyConverter != null)
			{
				return propertyConverter.convertProperty(id, value);
			}
		}
		return value;
	}

	public static Object convertSetPropertyValue(Object id, IPropertyDescriptor propertyDescriptor, Object rawValue)
	{
		if (propertyDescriptor instanceof IPropertyController)
		{
			IPropertyConverter propertyConverter = ((IPropertyController)propertyDescriptor).getConverter();
			if (propertyConverter != null)
			{
				return propertyConverter.convertValue(id, rawValue);
			}
		}
		return rawValue;
	}

	public Object getDefaultPersistValue(Object id)
	{
		init();
		PropertyDescriptorWrapper beanPropertyDescriptor = getBeansProperties().get(id);
		if (beanPropertyDescriptor == null)
		{
			ServoyLog.logError("Unknown property id " + id, null);
			return null;
		}

		if (id instanceof String)
		{
			AbstractRepository repository = (EclipseRepository)persistContext.getPersist().getRootObject().getRepository();
			try
			{
				Element element = repository.getContentSpec().getPropertyForObjectTypeByName(persistContext.getPersist().getTypeID(), (String)id);
				if (element != null)
				{
					return repository.convertArgumentStringToObject(element.getTypeID(), element.getDefaultTextualClassValue());
				}

				// no content spec (example: form.width), try based on property descriptor
				PropertyDescription desc = beanPropertyDescriptor.propertyDescriptor.getPropertyDescription(beanPropertyDescriptor.valueObject, this,
					persistContext);
				if (desc != null)
				{
					if (desc.getDefaultValue() != null) return desc.getDefaultValue();
					if (desc.getType() != null) return desc.getType().defaultValue(desc);
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get default value for id " + id, e);
			}
		}
		if (persistContext.getPersist() == beanPropertyDescriptor.valueObject)
		{
			ServoyLog.logError("Could not find content spec value for id " + id + " type = " + persistContext.getPersist().getTypeID(), null);
		}
		// else it is a bean property
		return null;
	}


	public boolean isPropertySet(Object id)
	{
		if (readOnly) return false;

		// recursively process a.b.c properties
		if (id instanceof String && !((String)id).startsWith(BEAN_PROPERTY_PREFIX_DOT))
		{
			String propId = (String)id;
			int dot = propId.indexOf('.');
			if (dot > 0)
			{
				Object propertyValue = getPropertyValue(propId.substring(0, dot));
				if (propertyValue instanceof IAdaptable)
				{
					IPropertySource propertySource = (IPropertySource)((IAdaptable)propertyValue).getAdapter(IPropertySource.class);
					if (propertySource != null)
					{
						return propertySource.isPropertySet(propId.substring(dot + 1));
					}
				}
			}
		}

		init();
		IPropertyDescriptor pd = propertyDescriptors.get(id);
		if (pd == null)
		{
			pd = hiddenPropertyDescriptors.get(id);
		}
		while (pd instanceof IDelegate && !(pd instanceof IPropertySetter))
		{
			Object delegate = ((IDelegate)pd).getDelegate();
			if (delegate instanceof IPropertyDescriptor)
			{
				pd = (IPropertyDescriptor)delegate;
			}
			else
			{
				break;
			}
		}
		if (pd instanceof IPropertySetter)
		{
			return ((IPropertySetter)pd).isPropertySet(this);
		}

		if (persistContext.getContext() instanceof Form && (persistContext.getPersist().getAncestor(IRepository.FORMS) != persistContext.getContext()))
		{
			return false;
		}

		if (((AbstractBase)persistContext.getPersist()).hasProperty((String)id))
		{
			return true;
		}

		Object defaultValue = getDefaultPersistValue(id);
		Object propertyValue = getPersistPropertyValue(id);
		return defaultValue != propertyValue && (defaultValue == null || !defaultValue.equals(propertyValue));
	}

	public void resetPropertyValue(Object id)
	{
		if (readOnly)
		{
			throw new RuntimeException("Cannot reset read-only property");
		}
		try
		{
			init();

			// recursively process a.b.c properties
			if (id instanceof String && !((String)id).startsWith(BEAN_PROPERTY_PREFIX_DOT))
			{
				String propId = (String)id;
				int dot = propId.indexOf('.');
				if (dot > 0)
				{
					Object propertyValue = getPropertyValue(propId.substring(0, dot));
					if (propertyValue instanceof IAdaptable)
					{
						IPropertySource propertySource = (IPropertySource)((IAdaptable)propertyValue).getAdapter(IPropertySource.class);
						if (propertySource != null)
						{
							propertySource.resetPropertyValue(propId.substring(dot + 1));
							return;
						}
					}
				}
			}

			IPropertyDescriptor pd = propertyDescriptors.get(id);
			if (pd == null)
			{
				pd = hiddenPropertyDescriptors.get(id);
			}
			while (pd instanceof IDelegate && !(pd instanceof IPropertySetter))
			{
				Object delegate = ((IDelegate)pd).getDelegate();
				if (delegate instanceof IPropertyDescriptor)
				{
					pd = (IPropertyDescriptor)delegate;
				}
				else
				{
					break;
				}
			}
			if (pd instanceof IPropertySetter)
			{
				((IPropertySetter)pd).resetPropertyValue(this);
				return;
			}

			PropertyDescriptorWrapper beanPropertyDescriptor = getBeansProperties().get(id);
			if (beanPropertyDescriptor != null)
			{
				createOverrideElementIfNeeded();

				if (persistContext.getPersist() instanceof AbstractBase && beanPropertyDescriptor.valueObject == persistContext.getPersist() /*
																																			 * not a bean
																																			 * property
																																			 */)
				{
					((AbstractBase)persistContext.getPersist()).clearProperty((String)id);
					if (persistContext.getPersist() instanceof ISupportExtendsID &&
						PersistHelper.isOverrideElement((ISupportExtendsID)persistContext.getPersist()) &&
						!((AbstractBase)persistContext.getPersist()).hasOverrideProperties() && !PersistHelper.hasOverrideChildren(persistContext.getPersist()))
					{
						// last property was reset, remove overriding persist
						IPersist superPersist = PersistHelper.getSuperPersist((ISupportExtendsID)persistContext.getPersist());
						try
						{
							((IDeveloperRepository)((AbstractBase)persistContext.getPersist()).getRootObject().getRepository()).deleteObject(persistContext.getPersist());
							persistContext.getPersist().getParent().removeChild(persistContext.getPersist());
							persistContext = PersistContext.create(superPersist, persistContext.getContext());
							beansProperties = null;
							propertyDescriptors = null;
							hiddenPropertyDescriptors = null;
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError(e);
						}
					}

					if (persistContext.getPersist() instanceof Bean && "size".equals(id))
					{
						// size, location and name are set on persist, not on bean instance
						Object beanDesignInstance = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist()).getBeanDesignInstance(
							(Bean)persistContext.getPersist());
						if (beanDesignInstance instanceof Component)
						{
							((Component)beanDesignInstance).setSize((Dimension)getPersistPropertyValue(id));
						}
					}
				}
				else
				{
					setPersistPropertyValue(id, getDefaultPersistValue(id));
				}

				if ("groupID".equals(id))
				{
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persistContext.getPersist().getParent(), true);
				}
				else
				{
					// fire persist change recursively if the style is changed
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persistContext.getPersist(),
						"styleName".equals(id) || "extendsID".equals(id));
				}
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not rest set property value for id " + id + " on object " + persistContext.getPersist(), e);
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Could not reset property", e.getMessage());
		}
	}

	public void setPersistPropertyValue(Object id, Object value)
	{
		init();
		PropertyDescriptorWrapper beanPropertyDescriptor = getBeansProperties().get(id);
		if (beanPropertyDescriptor != null)
		{
			boolean changed = false;
			try
			{
				if (createOverrideElementIfNeeded())
				{
					beanPropertyDescriptor = getBeansProperties().get(id);
					changed = true;
				}

				if ("name".equals(id) && beanPropertyDescriptor.valueObject instanceof ISupportUpdateableName)
				{
					if (value instanceof String || value == null)
					{
						changed |= !Utils.equalObjects(value, ((ISupportUpdateableName)beanPropertyDescriptor.valueObject).getName());
						((ISupportUpdateableName)beanPropertyDescriptor.valueObject).updateName(
							ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), (String)value);
						if (changed)
						{
							IViewReference[] iv = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().getViewReferences();
							for (IViewReference ref : iv)
							{
								if (ref.getId().equals("org.eclipse.ui.views.PropertySheet"))
								{
									PropertySheetPage psp = (PropertySheetPage)ref.getView(false).getAdapter(PropertySheetPage.class);
									if (psp != null) psp.refresh();
								}
							}
						}
					}
					else
					{
						// value not a string
						ServoyLog.logWarning(
							"Cannot set " + id + " property on object " + beanPropertyDescriptor.valueObject + " with type " + value.getClass(), null);
					}
				}
				else
				{
					changed |= !Utils.equalObjects(value,
						beanPropertyDescriptor.propertyDescriptor.getValue(beanPropertyDescriptor.valueObject, persistContext));

					beanPropertyDescriptor.propertyDescriptor.setValue(beanPropertyDescriptor.valueObject, value, persistContext);
					if ("i18nDataSource".equals(id))
					{
						I18NMessagesUtil.showDatasourceWarning();
						ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
						EclipseMessages.writeProjectI18NFiles(servoyProject, false, false);
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("Could not set property value for id " + id + " on object " + beanPropertyDescriptor.valueObject, e);
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Could not update property", e.getMessage());
			}

			if (changed)
			{
				if ("groupID".equals(id))
				{
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persistContext.getPersist().getParent(), true);
				}
				else
				{
					// fire persist change recursively if the style (or other ui related property) is changed
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persistContext.getPersist(),
						"styleName".equals(id) || "extendsID".equals(id));
				}
			}
		}
	}

	/**
	 * Create an override element of the current element if this is the first override
	 * @throws RepositoryException
	 */
	private boolean createOverrideElementIfNeeded() throws RepositoryException
	{
		IPersist overridePersist = ElementUtil.getOverridePersist(persistContext);
		if (overridePersist == persistContext.getPersist())
		{
			return false;
		}

		persistContext = PersistContext.create(overridePersist, persistContext.getContext());
		beansProperties = null;
		propertyDescriptors = null;
		hiddenPropertyDescriptors = null;
		init();
		return true;
	}

	public void setPropertyValue(Object id, Object value)
	{
		if (readOnly)
		{
			throw new RuntimeException("Cannot save read-only property");
		}

		try
		{
			createOverrideElementIfNeeded();
			init();

			// recursively process a.b.c properties
			if (id instanceof String && !((String)id).startsWith(BEAN_PROPERTY_PREFIX_DOT))
			{
				String propId = (String)id;
				int dot = propId.indexOf('.');
				if (dot > 0)
				{
					String subPropId = propId.substring(0, dot);
					Object propertyValue = getPropertyValue(subPropId);
					if (propertyValue instanceof IAdaptable)
					{
						IPropertySource propertySource = (IPropertySource)((IAdaptable)propertyValue).getAdapter(IPropertySource.class);
						if (propertySource != null)
						{
							propertySource.setPropertyValue(propId.substring(dot + 1), value);
							setPropertyValue(subPropId, propertyValue);
							return;
						}
					}
				}
			}

			Object val = value;
			IPropertyDescriptor propertyDescriptor = propertyDescriptors.get(id);
			if (propertyDescriptor == null)
			{
				propertyDescriptor = hiddenPropertyDescriptors.get(id);
			}
			while (propertyDescriptor instanceof IDelegate && !(propertyDescriptor instanceof IPropertySetter))
			{
				Object delegate = ((IDelegate)propertyDescriptor).getDelegate();
				if (delegate instanceof IPropertyDescriptor)
				{
					propertyDescriptor = (IPropertyDescriptor)delegate;
				}
				else
				{
					break;
				}
			}
			Object convertedValue = convertSetPropertyValue(id, propertyDescriptor, val);
			if (propertyDescriptor instanceof IPropertySetter)
			{
				((IPropertySetter)propertyDescriptor).setProperty(this, convertedValue);
			}
			else
			{
				setPersistPropertyValue(id, convertedValue);
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Could not set property value for id " + id + " on object " + persistContext.getPersist(), e);
			MessageDialog.openError(Display.getDefault().getActiveShell(), "Could not update property", e.getMessage());
		}
	}

	@Override
	public String toString()
	{
		StringBuilder retval = new StringBuilder();
		if (persistContext.getPersist() instanceof Portal)
		{
			retval.append("Portal (relation: ").append(((Portal)persistContext.getPersist()).getRelationName()).append(')');
		}
		else if (persistContext.getPersist() instanceof GraphicalComponent)
		{
			retval.append("Label / Button");
		}
		else if (persistContext.getPersist() instanceof RectShape)
		{
			retval.append("Rectangle");
		}
		else
		{
			retval.append(persistContext.getPersist().getClass().getName().replaceFirst(".*\\.", ""));
		}

		if (persistContext.getPersist() instanceof Tab)
		{
			String relationName = ((Tab)persistContext.getPersist()).getRelationName();
			if (relationName != null)
			{
				retval.append(" (relation: ").append(relationName).append(')');
			}
			else
			{
				retval.append(" (relationless)");
			}
		}
		else if (persistContext.getPersist() instanceof Bean)
		{
			retval.append(" (class: ");
			String beanClassName = ((Bean)persistContext.getPersist()).getBeanClassName();
			if (beanClassName != null)
			{
				retval.append(beanClassName.replaceFirst(".*\\.", ""));
			}
			retval.append(')');
		}

		String name = getActualComponentName();
		if (name != null)
		{
			retval.append(" - ").append(name);
		}
		if (persistContext.getPersist().getParent() instanceof Portal)
		{
			retval.append(" - (parent: portal)");
		}
		if (persistContext.getPersist() instanceof ISupportDataProviderID)
		{
			try
			{
				String dataprovider = ((ISupportDataProviderID)persistContext.getPersist()).getDataProviderID();
				FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist());
				Form flattenedForm = editingFlattenedSolution.getFlattenedForm(persistContext.getPersist());
				IDataProviderLookup dataproviderLookup = editingFlattenedSolution.getDataproviderLookup(null, flattenedForm);
				IDataProvider dp = dataproviderLookup.getDataProvider(dataprovider);
				if (dp != null)
				{
					retval.append(" - ");
					if (dp instanceof ColumnWrapper)
					{
						dp = ((ColumnWrapper)dp).getColumn();
					}
					if (dp instanceof Column)
					{
						retval.append("Column");
					}
					else if (dp instanceof ScriptCalculation)
					{
						retval.append("Calculation");
					}
					else if (dp instanceof AggregateVariable)
					{
						retval.append("Aggregate[");
						retval.append(((AggregateVariable)dp).getTypeAsString());
						retval.append('(');
						retval.append(((AggregateVariable)dp).getColumnNameToAggregate());
						retval.append(")]");
					}
					else if (dp instanceof ScriptVariable)
					{
						if (((ScriptVariable)dp).getParent() instanceof Form)
						{
							retval.append("FormVariable");
						}
						else
						{
							retval.append("Global[").append(((ScriptVariable)dp).getScopeName()).append(']');
						}
					}
					retval.append('(');
					retval.append(dataprovider);
					retval.append(',');
					// use dataprovider type as defined by converter
					ComponentFormat componentFormat = ComponentFormat.getComponentFormat(null, dp, Activator.getDefault().getDesignClient());
					retval.append(Column.getDisplayTypeString(componentFormat.dpType));
					Column column = null;
					if (dp instanceof ScriptCalculation)
					{
						column = ((ScriptCalculation)dp).getTable().getColumn(dataprovider);
					}
					else if (dp instanceof AggregateVariable)
					{
						column = ((AggregateVariable)dp).getTable().getColumn(dataprovider);
					}
					else if (dp instanceof Column)
					{
						column = (Column)dp;
					}
					if (dp.getLength() > 0)
					{
						switch (componentFormat.dpType)
						{
							case IColumnTypes.TEXT :
							case IColumnTypes.MEDIA :
								retval.append(',');
								retval.append(dp.getLength());
								break;
							case IColumnTypes.NUMBER :
								if (column != null)
								{
									retval.append(',');
									retval.append(column.getLength());
									retval.append(',');
									retval.append(column.getScale());
								}
						}
					}
					if (column != null && !column.getAllowNull())
					{
						retval.append(",not nullable");
					}
					retval.append(')');
//				retval.append(",");
//				if (dp.getColumnWrapper().getColumn().get)
//				retval.append("nullable");

				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return retval.toString();
	}

	protected String getActualComponentName()
	{
		return getActualComponentName(persistContext.getPersist());
	}

	public static String getActualComponentName(IPersist persist)
	{
		String name = null;
		if (persist instanceof IFormElement)
		{
			name = ((IFormElement)persist).getName();
		}

		if (persist instanceof ISupportDataProviderID && (name == null || name.length() == 0))
		{
			name = ((ISupportDataProviderID)persist).getDataProviderID();
		}

		if (persist instanceof ISupportName && (name == null || name.length() == 0))
		{
			name = ((ISupportName)persist).getName();
		}

		if (persist instanceof IRootObject && (name == null || name.length() == 0))
		{
			name = ((IRootObject)persist).getName();
		}

		if (persist instanceof Part && (name == null || name.length() == 0))
		{
			name = ((Part)persist).getEditorName();
		}

		if (name != null && !(persist instanceof Media))
		{
			int index = name.indexOf('.');
			if (index != -1)
			{
				name = name.substring(index + 1);
			}
		}
		return name;
	}

	/**
	 * Wrapper class for PropertyDescriptor and value object.
	 *
	 * @author rgansevles
	 *
	 */
	public static class PropertyDescriptorWrapper
	{
		public final IPropertyHandler propertyDescriptor;
		public Object valueObject;
		private PropertyEditor propertyEditor;

		PropertyDescriptorWrapper(IPropertyHandler propertyDescriptor, Object valueObject)
		{
			this.propertyDescriptor = propertyDescriptor;
			this.valueObject = valueObject;
		}

		public void setPropertyEditor(PropertyEditor propertyEditor)
		{
			this.propertyEditor = propertyEditor;
		}

		public PropertyEditor getPropertyEditor()
		{
			return propertyEditor;
		}
	}

	private static IPropertyDescriptor getGeneralPropertyDescriptor(PersistContext persistContext, final boolean readOnly, String id, String displayName,
		FlattenedSolution flattenedEditingSolution)
	{
		// Some property types are hard-coded

		if (id.equals("dataSource"))
		{
			// cannot change table when we have a super-form that already has a data source
			boolean propertyReadOnly = false;
			if (!readOnly && persistContext != null && persistContext.getPersist() instanceof Form && ((Form)persistContext.getPersist()).getExtendsID() > 0)
			{
				Form flattenedSuperForm = flattenedEditingSolution.getFlattenedForm(flattenedEditingSolution.getForm(((Form)persistContext.getPersist()).getExtendsID()));
				propertyReadOnly = flattenedSuperForm == null /* superform not found? make readonly for safety */
					|| flattenedSuperForm.getDataSource() != null; /* superform has a data source */

				if (propertyReadOnly && flattenedSuperForm != null && ((Form)persistContext.getPersist()).getDataSource() != null &&
					!((Form)persistContext.getPersist()).getDataSource().equals(flattenedSuperForm.getDataSource()))
				{
					// current form has invalid data source (overrides with different value), allow user to correct
					String[] dbServernameTablename = DataSourceUtilsBase.getDBServernameTablename(flattenedSuperForm.getDataSource());
					if (dbServernameTablename != null)
					{
						return new DatasourceController(id, displayName, "Select table", false,
							new Object[] { TableContentProvider.TABLE_NONE, new TableWrapper(dbServernameTablename[0], dbServernameTablename[1],
								EditorUtil.isViewTypeTable(dbServernameTablename[0], dbServernameTablename[1])) },
							DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
					}
				}
			}
			return new DatasourceController(id, displayName, "Select table", readOnly || propertyReadOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.ALL, true), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}

		if (id.equals("i18nDataSource"))
		{
			return new DatasourceController(id, displayName, "Select I18N table", readOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.I18N, true), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}

		if (id.equals("primaryDataSource"))
		{
			return new DatasourceController(id, displayName, "Select Primary table", readOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.ALL, false), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}

		if (id.equals("foreignDataSource"))
		{
			return new DatasourceController(id, displayName, "Select Foreign table", readOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.ALL, false), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}


		return null;
	}

	protected IPropertyDescriptor getPropertiesPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor, String id, String displayName,
		PropertyDescription propertyDescription, FlattenedSolution flattenedEditingSolution, Form form) throws RepositoryException
	{
		return getPropertiesPropertyDescriptor(propertyDescriptor, persistContext, readOnly, id, displayName, propertyDescription, flattenedEditingSolution,
			form);
	}

	private static IPropertyDescriptor getPropertiesPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor, final PersistContext persistContext,
		final boolean readOnly, final String id, final String displayName, final PropertyDescription propertyDescription,
		final FlattenedSolution flattenedEditingSolution, final Form form) throws RepositoryException
	{
		if (propertyDescription == null) return null;

		IPropertyType< ? > propertyType = propertyDescription.getType();
		if (propertyType == ValuesPropertyType.INSTANCE)
		{
			final ValuesConfig config = (ValuesConfig)propertyDescription.getConfig();
			if (config.isEditable())
			{
				// only display values
				return new EditableComboboxPropertyController(id, displayName, config.getDisplay());
			}

			if (config.isMultiple())
			{
				// list the available values as well as unknown values that are currently set.
				List<String> availableValues = Arrays.asList(config.getDisplay());
				final List<String> allValues = new ArrayList<String>(availableValues);
				StringTokenizerConverter converter = new StringTokenizerConverter(",", true);

				Object value = propertyDescriptor.propertyDescriptor.getValue(propertyDescriptor.valueObject, persistContext);
				if (value instanceof String)
				{
					for (String val : converter.convertProperty(id, (String)value))
					{
						if (!allValues.contains(val))
						{
							allValues.add(val);
						}
					}
				}
				final ILabelProvider labelProvider;
				if (allValues.size() == availableValues.size())
				{
					// no 'unknown' values
					labelProvider = new ArrayLabelProvider(converter);
				}
				else
				{
					// found 'unknown' value in current value, mark them with italic font
					labelProvider = new ValidvalueDelegatelabelProvider(new ArrayLabelProvider(converter), availableValues, null, FontResource.getDefaultFont(
						SWT.ITALIC, 0));
					Collections.sort(allValues);
				}
				return new PropertyController<String, Object[]>(id, displayName, converter, labelProvider, new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new ListSelectCellEditor(parent, "Select " + displayName, labelProvider, null, readOnly, allValues.toArray(), SWT.MULTI |
							SWT.CHECK, null, id + "SelectDialog");
					}
				});
			}

			// a dropdown with read and display values
			ComboboxPropertyModel<Object> model = new ComboboxPropertyModel<Object>(config.getReal(), config.getDisplay());
			if (config.hasDefault())
			{
				model.addDefaultValue(config.getRealDefault(), config.getDisplayDefault() == null ? Messages.LabelDefault : config.getDisplayDefault());
			}
			return new ComboboxPropertyController<Object>(id, displayName, model, Messages.LabelUnresolved);
		}

		if (FontPropertyType.TYPE_NAME.equals(propertyType.getName()))
		{
			final IDefaultValue<String> getLastPaintedFont;
			if (persistContext != null && persistContext.getPersist() instanceof AbstractBase)
			{
				getLastPaintedFont = new IDefaultValue<String>()
				{
					public String getValue()
					{
						return PropertyFontConverter.INSTANCE.convertProperty(id,
							((AbstractBase)persistContext.getPersist()).getRuntimeProperty(LastPaintedFontProperty));
					}
				};
			}
			else
			{
				getLastPaintedFont = null;
			}
			if (Boolean.TRUE.equals(propertyDescription.getConfig()))
			{
				// Both property (P) and edit (E) types are font strings, parse the string as awt font and convert back so that guessed fonts are mapped to
				// the correct string for that font
				return new PropertyController<String, String>(id, displayName, new ChainedPropertyConverter<String, java.awt.Font, String>(
					new InversedPropertyConverter<String, java.awt.Font>(PropertyFontConverter.INSTANCE), PropertyFontConverter.INSTANCE),
					FontLabelProvider.INSTANCE, new ICellEditorFactory()
					{
						public CellEditor createPropertyEditor(Composite parent)
						{
							return new FontCellEditor(parent, getLastPaintedFont);
						}
					});
			}
			else
			{
				return new PropertyController<java.awt.Font, String>(id, displayName, PropertyFontConverter.INSTANCE, new LabelProvider(),
					new ICellEditorFactory()
					{
						public CellEditor createPropertyEditor(Composite parent)
						{
							return new FontCellEditor(parent, getLastPaintedFont);
						}
					});
			}
		}

		if (propertyType == DataproviderPropertyType.INSTANCE)
		{
			Table table = null;
			boolean listItemHeader = persistContext != null && persistContext.getPersist() instanceof AbstractBase &&
				((AbstractBase)persistContext.getPersist()).getCustomMobileProperty(IMobileProperties.LIST_ITEM_HEADER.propertyName) != null;

			// treat list item as field on form
			Portal portal = listItemHeader ? null : (Portal)persistContext.getPersist().getAncestor(IRepository.PORTALS);
			final DataProviderOptions options;
			if (portal != null)
			{
				Relation[] relations = flattenedEditingSolution.getRelationSequence(portal.getRelationName());
				if (relations == null)
				{
					boolean listItem = persistContext != null &&
						persistContext.getPersist() instanceof AbstractBase &&
						(((AbstractBase)persistContext.getPersist()).getCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName) != null ||
							((AbstractBase)persistContext.getPersist()).getCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName) != null ||
							((AbstractBase)persistContext.getPersist()).getCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName) != null || ((AbstractBase)persistContext.getPersist()).getCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName) != null);
					if (!listItem) return null;
				}
				options = new DataProviderTreeViewer.DataProviderOptions(true, false, false, true /* related calcs */, false, false, false, false,
					INCLUDE_RELATIONS.NESTED, false, true, relations);
			}
			else
			{
				if (form == null) return null;
				try
				{
					table = flattenedEditingSolution.getFlattenedForm(form).getTable();
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
					return null;
				}

				options = new DataProviderTreeViewer.DataProviderOptions(true, table != null, table != null, table != null, true, true, table != null,
					table != null, INCLUDE_RELATIONS.NESTED, true, true, null);

			}
			final DataProviderConverter converter = new DataProviderConverter(flattenedEditingSolution, persistContext.getPersist(), table);
			DataProviderLabelProvider showPrefix = new DataProviderLabelProvider(false);
			showPrefix.setConverter(converter);
			DataProviderLabelProvider hidePrefix = new DataProviderLabelProvider(true);
			hidePrefix.setConverter(converter);

			ILabelProvider labelProviderShowPrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(showPrefix,
				persistContext.getContext()));
			final ILabelProvider labelProviderHidePrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(hidePrefix,
				persistContext.getContext()));
			PropertyController<String, String> propertyController = new PropertyController<String, String>(id, displayName, null, labelProviderShowPrefix,
				new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new DataProviderCellEditor(parent, labelProviderHidePrefix, new DataProviderValueEditor(converter), form,
							flattenedEditingSolution, readOnly, options, converter);
					}
				});
			propertyController.setSupportsReadonly(true);
			return propertyController;
		}

		if (propertyType == RelationPropertyType.INSTANCE)
		{
			Table primaryTable = null;
			if (form != null)
			{
				try
				{
					primaryTable = flattenedEditingSolution.getFlattenedForm(form).getTable();
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
				}
			}
			Table foreignTable = null;
			boolean incudeNone = false;
			if (persistContext != null && persistContext.getPersist() instanceof Tab)
			{
				Form tabForm = flattenedEditingSolution.getForm(((Tab)persistContext.getPersist()).getContainsFormID());
				if (tabForm != null)
				{
					try
					{
						foreignTable = tabForm.getTable();
					}
					catch (RepositoryException ex)
					{
						ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
					}
				}
				incudeNone = true; // unrelated tabs
			}
			else if (persistContext.getPersist() instanceof Portal)
			{
				// only show relations that go to the same table
				Relation[] relationSequence = flattenedEditingSolution.getRelationSequence(((Portal)persistContext.getPersist()).getRelationName());
				if (relationSequence != null)
				{
					foreignTable = relationSequence[relationSequence.length - 1].getForeignTable();
				}
			}
			return new RelationPropertyController(id, displayName, persistContext, primaryTable, foreignTable, incudeNone, true);
		}

		if (propertyType == FormatPropertyType.INSTANCE)
		{
			return new PropertyDescriptor(id, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new FormatCellEditor(parent, persistContext.getPersist(), (String)propertyDescription.getConfig());
				}
			};
		}

		if (propertyType == FormPropertyType.INSTANCE)
		{
			final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, false),
				persistContext.getContext());
			PropertyDescriptor pd = new PropertyDescriptor(id, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new ListSelectCellEditor(parent, "Select form", new FormContentProvider(flattenedEditingSolution, null /* persist is solution */),
						formLabelProvider, new FormValueEditor(flattenedEditingSolution), readOnly, new FormContentProvider.FormListOptions(
							FormListOptions.FormListType.FORMS, null, true, false, false), SWT.NONE, null, "Select form dialog");
				}
			};
			pd.setLabelProvider(formLabelProvider);
			return pd;
		}
		if (propertyType == ValueListPropertyType.INSTANCE)
		{
			return new ValuelistPropertyController<Integer>(id, displayName, persistContext, true);
		}

		if (propertyType == MediaPropertyType.INSTANCE)
		{
			MediaPropertyControllerConfig config = null;
			if (propertyDescription != null && propertyDescription.getConfig() instanceof MediaPropertyControllerConfig) config = (MediaPropertyControllerConfig)propertyDescription.getConfig();
			return new MediaIDPropertyController(id, displayName, persistContext, flattenedEditingSolution, true, config);
		}


		if (propertyType == TagStringPropertyType.INSTANCE)
		{
			Table table = null;
			if (form != null)
			{
				try
				{
					table = flattenedEditingSolution.getFlattenedForm(form).getTable();
				}
				catch (RepositoryException ex)
				{
					ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
				}
			}
			final Table finalTable = table;
			return new PropertyController<String, String>(id, displayName, new IPropertyConverter<String, String>()
			{
				public String convertProperty(Object id, String value)
				{
					return value;
				}

				public String convertValue(Object id, String value)
				{
					if ("titleText".equals(id) && value != null && value.length() > 0 && value.trim().length() == 0)
					{
						return IBasicFormManager.NO_TITLE_TEXT;
					}
					return value;
				}

			}, TextCutoffLabelProvider.DEFAULT, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new TagsAndI18NTextCellEditor(parent, persistContext, flattenedEditingSolution, TextCutoffLabelProvider.DEFAULT, finalTable,
						"Edit text property", Activator.getDefault().getDesignClient(), Boolean.TRUE.equals(propertyDescription.getConfig()));
				}
			});
		}

		if (propertyType == FoundsetPropertyType.INSTANCE)
		{
			return new FoundsetPropertyController(id, displayName, flattenedEditingSolution, persistContext, (JSONObject)propertyDescription.getConfig());
		}

		if (id.equals("name"))
		{
			return new DelegatePropertyController<String, String>(new PropertyController<String, String>(id, displayName, NULL_STRING_CONVERTER, null,
				new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						VerifyingTextCellEditor cellEditor = new VerifyingTextCellEditor(parent);
						cellEditor.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
						cellEditor.setValidator(new ICellEditorValidator()
						{
							public String isValid(Object value)
							{
								if (Utils.isInheritedFormElement(persistContext.getPersist(), persistContext.getContext()))
								{
									return "Cannot change name of an override element.";
								}
								if (value instanceof String && ((String)value).length() > 0)
								{
									try
									{
										if (persistContext.getPersist() instanceof IFormElement)
										{
											ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(
												(String)value,
												persistContext.getPersist().getID(),
												new ValidatorSearchContext(flattenedEditingSolution.getFlattenedForm(persistContext.getPersist()),
													IRepository.ELEMENTS), false);

										}
										else if (persistContext.getPersist() instanceof Form)
										{
											ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName((String)value,
												persistContext.getPersist().getID(),
												new ValidatorSearchContext(persistContext.getPersist(), IRepository.FORMS), false);
										}
									}
									catch (RepositoryException e)
									{
										return e.getMessage();
									}
								}
								return null;
							}
						});

						return cellEditor;
					}
				}))
			{
				@Override
				public IMergedPropertyDescriptor createMergedPropertyDescriptor(IMergeablePropertyDescriptor pd)
				{
					return null;
				}

				@Override
				public boolean isMergeableWith(IMergeablePropertyDescriptor pd)
				{
					return true;
				}
			};
		}


		return null;
	}

	public static class NullDefaultLabelProvider extends LabelProvider implements IFontProvider
	{
		public static final NullDefaultLabelProvider LABEL_NONE = new NullDefaultLabelProvider(Messages.LabelNone);
		public static final NullDefaultLabelProvider LABEL_DEFAULT = new NullDefaultLabelProvider(Messages.LabelDefault);
		private final String defaultLabel;

		private NullDefaultLabelProvider(String defaultLabel)
		{
			this.defaultLabel = defaultLabel;
		}

		@Override
		public String getText(Object element)
		{
			return (element == null || "".equals(element)) ? defaultLabel : element.toString();
		}

		public Font getFont(Object element)
		{
			if (element == null || "".equals(element))
			{
				return FontResource.getDefaultFont(SWT.BOLD, -1);
			}
			return FontResource.getDefaultFont(SWT.NONE, 0);
		}
	}

	/** Sort property descriptors by PropertyEditorOption.propertyOrder if defined, else by displayName.
	 *
	 * @param propertyDescriptors2
	 * @return
	 */
	public static <T extends IPropertyHandler> T[] sortBeansPropertyDescriptors(T[] descs)
	{
		if (descs != null && descs.length > 1)
		{
			Arrays.sort(descs, BEANS_PROPERTY_COMPARATOR);
		}
		return descs;
	}

}
