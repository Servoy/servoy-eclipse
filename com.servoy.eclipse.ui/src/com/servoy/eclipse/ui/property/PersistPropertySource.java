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
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.print.PageFormat;
import java.beans.IntrospectionException;
import java.beans.PropertyEditor;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingConstants;
import javax.swing.border.Border;

import org.eclipse.core.runtime.IAdaptable;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ICellEditorValidator;
import org.eclipse.jface.viewers.IFontProvider;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.FontData;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.ServoyProject;
import com.servoy.eclipse.core.repository.EclipseMessages;
import com.servoy.eclipse.core.repository.EclipseRepository;
import com.servoy.eclipse.core.util.CoreUtils;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.editors.BeanCustomCellEditor;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor;
import com.servoy.eclipse.ui.editors.DataProviderCellEditor.DataProviderValueEditor;
import com.servoy.eclipse.ui.editors.FontCellEditor;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.editors.PageFormatEditor;
import com.servoy.eclipse.ui.editors.SortCellEditor;
import com.servoy.eclipse.ui.editors.TagsAndI18NTextCellEditor;
import com.servoy.eclipse.ui.labelproviders.ArrayLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FontLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormInheritenceDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.eclipse.ui.labelproviders.PageFormatLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.TextCutoffLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValidvalueDelegatelabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValuelistLabelProvider;
import com.servoy.eclipse.ui.property.ComplexProperty.ComplexPropertyConverter;
import com.servoy.eclipse.ui.property.MethodWithArguments.UnresolvedMethodWithArguments;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.eclipse.ui.util.DocumentValidatorVerifyListener;
import com.servoy.eclipse.ui.util.IDefaultValue;
import com.servoy.eclipse.ui.util.VerifyingTextCellEditor;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.component.InvisibleBean;
import com.servoy.j2db.dataui.PropertyEditorClass;
import com.servoy.j2db.dataui.PropertyEditorHint;
import com.servoy.j2db.dataui.PropertyEditorOption;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec.Element;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDataProviderLookup;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.ITableDisplay;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.RuntimeProperty;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.scripting.FunctionDefinition;
import com.servoy.j2db.util.ComponentFactoryHelper;
import com.servoy.j2db.util.IDelegate;
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

@SuppressWarnings("nls")
public class PersistPropertySource implements IPropertySource, IAdaptable
{
	private static IPropertyController<Integer, Integer> HORIZONTAL_ALIGNMENT_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> VERTICAL_ALIGNMENT_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> VARIABLE_TYPE_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> ROLLOVER_CURSOR_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> ROTATION_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> SHAPE_TYPE_CONTOLLER = null;
	private static IPropertyController<Integer, Integer> VIEW_TYPE_CONTOLLER = null;
	private static IPropertyController<Integer, Integer> DISPLAY_TYPE_CONTOLLER = null;
	private static IPropertyController<String, Integer> NAMEDFOUNDSET_CONTOLLER = null;
	private static IPropertyController<Integer, Integer> TAB_ORIENTATION_CONTROLLER = null;
	private static IPropertyController<String, String> MNEMONIC_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> TEXT_ORIENTATION_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> SOLUTION_TYPE_CONTROLLER = null;
	private static IPropertyController<Integer, Integer> JOIN_TYPE_CONTROLLER = null;

	private static final String BEAN_PROPERTY_PREFIX_DOT = "bean.";

	public final int BASIC_FILTER = 0;
	public final int ADVANCED_FILTER = 1;
	public final int WEB_FILTER = 2;

	public static final String[] MNEMONIC_VALUES = new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" };
	public static IPropertyConverter<String, String> NULL_STRING_CONVERTER = new IPropertyConverter<String, String>()
	{
		public String convertProperty(Object id, String value)
		{
			return (value == null) ? "" : value;
		}

		public String convertValue(Object id, String value)
		{
			return "".equals(value) ? null : value;
		}
	};

	// remember the font that was used last time the element was painted, use this as default for font cell editors.
	public static final RuntimeProperty<java.awt.Font> LastPaintedFontProperty = new RuntimeProperty<java.awt.Font>()
	{
		private static final long serialVersionUID = 1L;
	};

	private final IPersist persist;
	private final boolean readOnly;

	// Property Descriptors
	private Map<Object, IPropertyDescriptor> propertyDescriptors;
	private Map<Object, IPropertyDescriptor> hiddenPropertyDescriptors;
	private Map<Object, PropertyDescriptorWrapper> beansProperties;
	private final IPersist context; // context when creating PersistProperties, usually the form being edited

	public PersistPropertySource(IPersist persist, IPersist context, boolean readonly)
	{
		this.persist = persist;
		this.context = context;
		this.readOnly = readonly;
	}

	public IPersist getPersist()
	{
		return persist;
	}

	public Object getAdapter(Class adapter)
	{
		if (adapter == IPersist.class)
		{
			return persist;
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
			try
			{
				FlattenedSolution flattenedEditingSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist);
				Form flattenedForm = flattenedEditingSolution.getFlattenedForm(persist);

				java.beans.BeanInfo info = null;
				Object valueObject = null;
				try
				{
					if (persist instanceof Bean) //step into the bean instance
					{
						try
						{
							FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(
								persist);
							valueObject = ComponentFactory.getBeanDesignInstance(Activator.getDefault().getDesignClient(), editingFlattenedSolution,
								(Bean)persist, (Form)persist.getAncestor(IRepository.FORMS));
						}
						catch (Exception e)
						{
							ServoyLog.logError("Could not instantiate bean " + persist, e);
						}
						if (valueObject instanceof InvisibleBean) //step into the invisible bean instance
						{
							valueObject = ((InvisibleBean)valueObject).getDelegate();
						}
					}
					else
					{
						valueObject = persist;
					}
					if (valueObject != null)
					{
						info = java.beans.Introspector.getBeanInfo(valueObject.getClass());
					}
				}
				catch (java.beans.IntrospectionException e)
				{
					ServoyLog.logError(e);
				}
				java.beans.PropertyDescriptor[] properties;
				if (info != null)
				{
					properties = info.getPropertyDescriptors();
				}
				else
				{
					properties = new java.beans.PropertyDescriptor[0];
				}

				propertyDescriptors = new HashMap<Object, IPropertyDescriptor>();
				hiddenPropertyDescriptors = new HashMap<Object, IPropertyDescriptor>();
				beansProperties = new HashMap<Object, PropertyDescriptorWrapper>();

				for (java.beans.PropertyDescriptor element : properties)
				{
					registerProperty(new PropertyDescriptorWrapper(element, valueObject), flattenedForm, flattenedEditingSolution);
				}
				if (valueObject == persist)
				{
					// check for pseudo properties
					String[] pseudoPropertyNames = getPseudoPropertyNames(persist.getClass());
					if (pseudoPropertyNames != null)
					{
						for (String propName : pseudoPropertyNames)
						{
							IPropertyDescriptor desc;
							try
							{
								desc = getPropertiesPropertyDescriptor(propName, getDisplayName(propName), propName, flattenedForm, flattenedEditingSolution);
								if (desc != null)
								{
									setCategory(desc, PropertyCategory.createPropertyCategory(propName));
									propertyDescriptors.put(propName, desc);
								}
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
						for (java.beans.PropertyDescriptor element : java.beans.Introspector.getBeanInfo(persist.getClass()).getPropertyDescriptors())
						{
							registerProperty(new PropertyDescriptorWrapper(element, persist), flattenedForm, flattenedEditingSolution);
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
			catch (RepositoryException ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	private static String[] getPseudoPropertyNames(Class clazz)
	{
		if (Solution.class == clazz)
		{
			return new String[] { "loginSolutionName" };
		}
		return null;
	}

	private void registerProperty(PropertyDescriptorWrapper propertyDescriptor, Form flattenedForm, FlattenedSolution flattenedEditingSolution)
	{
		IPropertyDescriptor pd = null;
		IPropertyDescriptor combinedPropertyDesciptor = null;
		try
		{
			pd = createPropertyDescriptor(propertyDescriptor, flattenedForm, flattenedEditingSolution);
			if (propertyDescriptor.valueObject == persist)
			{
				combinedPropertyDesciptor = createCombinedPropertyDescriptor(propertyDescriptor);
			}
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError("Could not create property descriptor for " + propertyDescriptor.propertyDescriptor.getDisplayName(), e);
		}
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
			((IPropertyController)pd).setReadonly(readOnly);

			if (propertyDescriptor.valueObject == persist &&
				RepositoryHelper.hideForProperties(propertyDescriptor.propertyDescriptor.getName(), persist.getClass()))
			{
				hiddenPropertyDescriptors.put(pd.getId(), pd);
			}
			else
			{
				propertyDescriptors.put(pd.getId(), pd);
			}
			beansProperties.put(pd.getId(), propertyDescriptor);

			if (combinedPropertyDesciptor != null)
			{
				propertyDescriptors.put(combinedPropertyDesciptor.getId(), combinedPropertyDesciptor);
			}
		}
	}

	public IPropertyDescriptor[] getPropertyDescriptors()
	{
		init();
		return propertyDescriptors.values().toArray(new IPropertyDescriptor[propertyDescriptors.size()]);
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

	private Map<Object, PropertyDescriptorWrapper> getBeansProperties()
	{
		init();
		return beansProperties;
	}

	protected IPropertyDescriptor createPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor, Form flattenedForm,
		FlattenedSolution flattenedEditingSolution) throws RepositoryException
	{

		if ((propertyDescriptor.propertyDescriptor.getReadMethod() == null) || propertyDescriptor.propertyDescriptor.getWriteMethod() == null ||
			propertyDescriptor.propertyDescriptor.isExpert() || propertyDescriptor.propertyDescriptor.getPropertyType().equals(Object.class) ||
			propertyDescriptor.propertyDescriptor.isHidden() || !shouldShow(propertyDescriptor))
		{
			return null;
		}

		PropertyCategory category;
		String id;
		if (propertyDescriptor.valueObject == persist)
		{
			category = PropertyCategory.createPropertyCategory(propertyDescriptor.propertyDescriptor.getName());
			id = propertyDescriptor.propertyDescriptor.getName();
		}
		else
		{
			category = PropertyCategory.Beans;
			id = BEAN_PROPERTY_PREFIX_DOT + propertyDescriptor.propertyDescriptor.getName();
		}
		IPropertyDescriptor desc = getPropertyDescriptor(propertyDescriptor, id, getDisplayName(propertyDescriptor.propertyDescriptor.getName()), category,
			flattenedForm, flattenedEditingSolution);
		setCategory(desc, category);
		return desc;
	}

	protected void setCategory(IPropertyDescriptor desc, PropertyCategory category)
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

	protected IPropertyDescriptor createCombinedPropertyDescriptor(PropertyDescriptorWrapper propertyDescriptor)
	{
		PropertyCategory category = PropertyCategory.createPropertyCategory(propertyDescriptor.propertyDescriptor.getName());
		IPropertyDescriptor desc = getCombinedPropertyDescriptor(propertyDescriptor, getDisplayName(propertyDescriptor.propertyDescriptor.getName()), category);
		if (desc instanceof org.eclipse.ui.views.properties.PropertyDescriptor)
		{
			((org.eclipse.ui.views.properties.PropertyDescriptor)desc).setCategory(category.name());
		}
		else if (desc instanceof DelegatePropertyController)
		{
			((DelegatePropertyController)desc).setCategory(category.name());
		}
		return desc;
	}

	private String getDisplayName(String name)
	{
		return RepositoryHelper.getDisplayName(name, persist.getClass());
	}

	private IPropertyDescriptor getPropertyDescriptor(final PropertyDescriptorWrapper propertyDescriptor, final String id, String displayName,
		PropertyCategory category, final Form flattenedForm, final FlattenedSolution flattenedEditingSolution) throws RepositoryException
	{
		/*
		 * Category based property controllers.
		 */

		if (category == PropertyCategory.Events || category == PropertyCategory.Commands)
		{
			return new MethodPropertyController<Integer>(id, displayName, persist, context, true, category == PropertyCategory.Commands, flattenedForm != null,
				true)
			{
				@Override
				protected IPropertyConverter<Integer, Object> createConverter()
				{
					IPropertyConverter<Integer, MethodWithArguments> id2MethodsWithArgumentConverter = new IPropertyConverter<Integer, MethodWithArguments>()
					{
						public MethodWithArguments convertProperty(Object id, Integer value)
						{
							SafeArrayList<Object> args = null;
							if (persist instanceof AbstractBase)
							{
								List<Object> instanceArgs = ((AbstractBase)persist).getInstanceMethodArguments(id.toString());
								if (instanceArgs != null)
								{
									args = new SafeArrayList<Object>(instanceArgs);
								}
							}
							return new MethodWithArguments(value.intValue(), args);
						}

						public Integer convertValue(Object id, MethodWithArguments value)
						{
							setInstancMethodArguments(persist, id, value.arguments);
							return new Integer(value.methodId);
						}
					};

					if (context instanceof Form && ((Form)context).getExtendsFormID() > 0)
					{
						// convert to the actual method called according to form inheritance
						id2MethodsWithArgumentConverter = new ChainedPropertyConverter<Integer, MethodWithArguments, MethodWithArguments>(
							id2MethodsWithArgumentConverter, new FormInheritenceMethodConverter(persist, context));
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
									return new MethodPropertySource(this, persist, context, null, getId().toString(), isReadOnly());
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

		if (category == PropertyCategory.Beans)
		{
			Class< ? > propertyEditorClass = propertyDescriptor.propertyDescriptor.getPropertyEditorClass();
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
								return new BeanCustomCellEditor(parent, propertyDescriptor.getPropertyEditor(), labelProvider,
									propertyDescriptor.propertyDescriptor.getName(), "Bean custom property editor", readOnly);
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
			Object hintValue = propertyDescriptor.propertyDescriptor.getValue(PropertyEditorHint.PROPERTY_EDITOR_HINT);
			final PropertyEditorHint propertyEditorHint;
			if (hintValue instanceof PropertyEditorHint)
			{
				propertyEditorHint = (PropertyEditorHint)hintValue;
			}
			else if (propertyDescriptor.propertyDescriptor.getPropertyType() == FunctionDefinition.class)
			{
				// use defaults for FunctionDefinition property without specified hint
				propertyEditorHint = new PropertyEditorHint(PropertyEditorClass.method);
			}
			else
			{
				propertyEditorHint = null;
			}

			if (propertyEditorHint != null && flattenedForm != null)
			{
				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.method)
				{
					// accept String or FunctionDefinition properties

					Class< ? > propertyType = propertyDescriptor.propertyDescriptor.getPropertyType();
					// convert a FunctionDefinition to MethodWithArguments
					IPropertyConverter<FunctionDefinition, MethodWithArguments> methodsWithArgumentConverter = new IPropertyConverter<FunctionDefinition, MethodWithArguments>()
					{
						public MethodWithArguments convertProperty(Object id, FunctionDefinition value)
						{
							if (value == null) return null;

							FlattenedSolution flattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(
								persist);
							Iterator<ScriptMethod> scriptMethods = null;
							if (value.getFormName() != null)
							{
								// form method
								Form methodForm = flattenedSolution.getForm(value.getFormName());
								if (methodForm != null)
								{
									scriptMethods = methodForm.getScriptMethods(false);
								}
							}
							else
							{
								// global method
								scriptMethods = flattenedSolution.getScriptMethods(false);
							}
							if (scriptMethods != null)
							{
								ScriptMethod scriptMethod = AbstractBase.selectByName(scriptMethods, value.getMethodName());
								if (scriptMethod != null)
								{
									return new MethodWithArguments(scriptMethod.getID(), null);
								}
							}
							return new UnresolvedMethodWithArguments(value.toMethodString());
						}

						public FunctionDefinition convertValue(Object id, MethodWithArguments value)
						{
							IScriptProvider scriptMethod = CoreUtils.getScriptMethod(persist, context, null, value.methodId);
							if (scriptMethod != null)
							{
								String formName = (scriptMethod.getParent() instanceof Form) ? ((Form)scriptMethod.getParent()).getName() : null;
								return new FunctionDefinition(formName, scriptMethod.getName());
							}
							return null;
						}
					};

					if (context instanceof Form && ((Form)context).getExtendsFormID() > 0)
					{
						// convert to the actual method called according to form inheritance
						methodsWithArgumentConverter = new ChainedPropertyConverter<FunctionDefinition, MethodWithArguments, MethodWithArguments>(
							methodsWithArgumentConverter, new FormInheritenceMethodConverter(persist, context));
					}

					final IPropertyConverter<FunctionDefinition, Object> functionDefinitionPropertyConverter = new ChainedPropertyConverter<FunctionDefinition, MethodWithArguments, Object>(
						methodsWithArgumentConverter, PropertyCastingConverter.<MethodWithArguments, Object> propertyCastingConverter());

					if (propertyType == FunctionDefinition.class)
					{
						return new MethodPropertyController<FunctionDefinition>(id, displayName, persist, context,
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), false,
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeForm)),
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobal)))
						{
							@Override
							protected IPropertyConverter<FunctionDefinition, Object> createConverter()
							{
								return functionDefinitionPropertyConverter;
							}
						};
					}

					if (propertyType == String.class)
					{
						// chain with String to FunctionDefinition converter
						return new MethodPropertyController<String>(id, displayName, persist, context,
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)), false,
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeForm)),
							Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeGlobal)))
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

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.dataprovider &&
					propertyDescriptor.propertyDescriptor.getPropertyType() == String.class)
				{
					// String property, select a data provider
					Table table = null;
					try
					{
						table = flattenedForm.getTable();
					}
					catch (Exception ex)
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
					final DataProviderConverter converter = new DataProviderConverter(flattenedEditingSolution, persist, table);
					DataProviderLabelProvider showPrefix = new DataProviderLabelProvider(false);
					showPrefix.setConverter(converter);
					DataProviderLabelProvider hidePrefix = new DataProviderLabelProvider(true);
					hidePrefix.setConverter(converter);

					ILabelProvider labelProviderShowPrefix = new SolutionContextDelegateLabelProvider(
						new FormContextDelegateLabelProvider(showPrefix, context), context);
					final ILabelProvider labelProviderHidePrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(hidePrefix,
						context), context);
					PropertyController<String, String> propertyController = new PropertyController<String, String>(id, displayName, null,
						labelProviderShowPrefix, new ICellEditorFactory()
						{
							public CellEditor createPropertyEditor(Composite parent)
							{
								return new DataProviderCellEditor(parent, labelProviderHidePrefix, getFormInheritanceValueEditor(persist,
									new DataProviderValueEditor(converter), id), flattenedForm, flattenedEditingSolution, readOnly, options, converter);
							}
						});
					propertyController.setSupportsReadonly(true);
					return propertyController;
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.relation &&
					propertyDescriptor.propertyDescriptor.getPropertyType() == String.class)
				{
					// String property, select a relation
					Table primaryTable = null;
					if (flattenedForm != null)
					{
						try
						{
							primaryTable = flattenedForm.getTable();
						}
						catch (Exception ex)
						{
							ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
						}
					}

					return new RelationPropertyController(id, displayName, persist, context, primaryTable, null /* foreignTable */,
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)),
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNestedRelations)));
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.form &&
					propertyDescriptor.propertyDescriptor.getPropertyType() == String.class)
				{
					// String property, select a form
					final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, false),
						context);
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
									return new Integer(f == null ? Form.NAVIGATOR_NONE : f.getID());
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

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.valuelist &&
					propertyDescriptor.propertyDescriptor.getPropertyType() == String.class)
				{
					// String property, select a value list
					return new ValuelistPropertyController<String>(id, displayName, persist, context,
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
									return new Integer(vl == null ? ValuelistLabelProvider.VALUELIST_NONE : vl.getID());
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

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.media &&
					propertyDescriptor.propertyDescriptor.getPropertyType() == String.class)
				{
					// String property, select an image
					return new MediaPropertyController<String>(id, displayName, persist, context,
						Boolean.TRUE.equals(propertyEditorHint.getOption(PropertyEditorOption.includeNone)))
					{
						@Override
						protected IPropertyConverter<String, Integer> createConverter()
						{
							// convert between media ids and image name
							return new IPropertyConverter<String, Integer>()
							{
								public Integer convertProperty(Object id, String value)
								{
									Media media = flattenedEditingSolution.getMedia(value);
									return new Integer(media == null ? MediaLabelProvider.MEDIA_NONE : media.getID());
								}

								public String convertValue(Object id, Integer value)
								{
									int mediaId = value.intValue();
									if (mediaId == MediaLabelProvider.MEDIA_NONE) return null;
									Media media = flattenedEditingSolution.getMedia(mediaId);
									return media == null ? null : media.getName();
								}
							};
						}
					};
				}

				if (propertyEditorHint.getPropertyEditorClass() == PropertyEditorClass.styleclass &&
					propertyDescriptor.propertyDescriptor.getPropertyType() == String.class)
				{
					// String property, select a style class
					Object option = propertyEditorHint.getOption(PropertyEditorOption.styleLookupName);
					if (option instanceof String)
					{
						return new StyleclassPropertyController(id, displayName, persist, (String)option);
					}
				}
			}
		}

		/*
		 * Type based property controllers.
		 */

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == java.awt.Dimension.class)
		{
			java.awt.Dimension dimension = null;
			if (propertyDescriptor.propertyDescriptor.getName().equals("intercellSpacing")) dimension = new Dimension(1, 1);
			else dimension = new Dimension(0, 0);
			final Dimension defaultDimension = dimension;
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

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == java.awt.Point.class)
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

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == java.awt.Insets.class)
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

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == java.awt.Color.class)
		{
			return new ColorPropertyController(id, displayName);
		}

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == java.awt.Font.class)
		{
			final IDefaultValue<FontData[]> getLastPaintedFont;
			if (persist instanceof AbstractBase)
			{
				getLastPaintedFont = new IDefaultValue<FontData[]>()
				{
					public FontData[] getValue()
					{
						return PropertyFontConverter.INSTANCE.convertProperty(id, ((AbstractBase)persist).getRuntimeProperty(LastPaintedFontProperty));
					}
				};
			}
			else
			{
				getLastPaintedFont = null;
			}
			return new PropertyController<java.awt.Font, FontData[]>(id, displayName, PropertyFontConverter.INSTANCE, FontLabelProvider.INSTANCE,
				new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new FontCellEditor(parent, getLastPaintedFont);
					}
				});
		}

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == Border.class)
		{
			return new BorderPropertyController(id, displayName, this, persist);
		}

		if (propertyDescriptor.propertyDescriptor.getPropertyType() == boolean.class ||
			propertyDescriptor.propertyDescriptor.getPropertyType() == Boolean.class)
		{
			return new CheckboxPropertyDescriptor(id, displayName);
		}


		/*
		 * Name based property controllers.
		 */

		String name = propertyDescriptor.propertyDescriptor.getName();

		IPropertyDescriptor retval = getGeneralPropertyDescriptor(id, displayName, name, flattenedForm);
		if (retval != null)
		{
			return retval;
		}
		if (category == PropertyCategory.Properties) // name based props for IPersists only (not beans)
		{
			retval = getPropertiesPropertyDescriptor(id, displayName, name, flattenedForm, flattenedEditingSolution);
			if (retval != null)
			{
				return retval;
			}
		}

		//  defaults, determine editor based on type
		Class< ? > clazz = propertyDescriptor.propertyDescriptor.getPropertyType();
		if (clazz != null)
		{
			if (clazz == String.class)
			{
				return new PropertyController<String, String>(id, displayName, NULL_STRING_CONVERTER, null, new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new TextCellEditor(parent);
					}
				});
			}

			final int[] type = new int[1];
			if (clazz == byte.class || clazz == Byte.class)
			{
				type[0] = NumberCellEditor.BYTE;
			}
			else if (clazz == double.class || clazz == Double.class)
			{
				type[0] = NumberCellEditor.DOUBLE;
			}
			else if (clazz == float.class || clazz == Float.class)
			{
				type[0] = NumberCellEditor.FLOAT;
			}
			else if (clazz == int.class || clazz == Integer.class)
			{
				type[0] = NumberCellEditor.INTEGER;
			}
			else if (clazz == long.class || clazz == Long.class)
			{
				type[0] = NumberCellEditor.LONG;
			}
			else if (clazz == short.class || clazz == Short.class)
			{
				type[0] = NumberCellEditor.SHORT;
			}
			else
			{
				type[0] = -1;
			}
			if (type[0] != -1)
			{
				return new PropertyDescriptor(id, displayName)
				{
					@Override
					public CellEditor createPropertyEditor(Composite parent)
					{
						NumberCellEditor editor = new NumberCellEditor(parent);
						editor.setType(type[0]);
						return editor;
					}
				};
			}
		}

		return null;
	}

	/**
	 * Wrap the label provider with a {@link FormInheritenceDelegateLabelProvider} if needed.
	 * 
	 * @param form
	 * @param labelProvider
	 * @param propertyId
	 * @return
	 */
	public static ILabelProvider getFormInheritanceLabelProvider(IPersist persist, ILabelProvider labelProvider, Object propertyId)
	{
		if (persist instanceof Form && ((Form)persist).getExtendsFormID() > 0)
		{
			return new FormInheritenceDelegateLabelProvider((Form)persist, labelProvider, propertyId);
		}
		return labelProvider;
	}

	/**
	 * Wrap the value editor with a {@link FormInheritanceDelegateValueEditor} if needed.
	 * 
	 * @param form
	 * @param valueEditor
	 * @param propertyId
	 * @return
	 */
	public static <T> IValueEditor<T> getFormInheritanceValueEditor(IPersist persist, IValueEditor<T> valueEditor, Object propertyId)
	{
		if (persist instanceof Form && ((Form)persist).getExtendsFormID() > 0)
		{
			return new FormInheritanceDelegateValueEditor<T>(((Form)persist), valueEditor, propertyId);
		}
		return valueEditor;
	}

	/**
	 * Get property descriptor that maps multiple properties into 1
	 * 
	 * @param propertyDescriptor
	 * @param displayName
	 * @param category
	 * @return
	 */
	private IPropertyDescriptor getCombinedPropertyDescriptor(final PropertyDescriptorWrapper propertyDescriptor, String displayName, PropertyCategory category)
	{
		if (propertyDescriptor.valueObject == persist) // name based props for IPersists only
		{
			String name = propertyDescriptor.propertyDescriptor.getName();

			if (name.equals("containsFormID"))
			{
				return new RelatedTabController("containsForm", "containsForm", "Select tab form", readOnly, (Form)persist.getAncestor(IRepository.FORMS),
					ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist));
			}
		}
		return null;
	}

	/**
	 * Get all solution names other than yourself.
	 * 
	 * @return
	 */
	protected List<String> getSolutionNames()
	{
		List<String> lst = new ArrayList<String>();

		if (persist instanceof Solution)
		{
			String name = ((Solution)persist).getName();
			RootObjectMetaData[] solutionMetaDatas;
			try
			{
				solutionMetaDatas = ServoyModel.getDeveloperRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
				for (RootObjectMetaData element : solutionMetaDatas)
				{
					if (!element.getName().equals(name))
					{
						lst.add(element.getName());
					}
				}
			}
			catch (RemoteException e)
			{
				ServoyLog.logError(e);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		return lst;
	}

	/**
	 * Get the style names usable for the current form.
	 * 
	 * @param persist
	 * @return
	 * @throws RepositoryException
	 */
	protected static String[] getStyleNames() throws RepositoryException
	{
		List<String> styleNames = new ArrayList<String>();
		Iterator<Style> it = ServoyModel.getDeveloperRepository().getActiveRootObjects(IRepository.STYLES).iterator();
		while (it.hasNext())
		{
			Style style = it.next();
			styleNames.add(style.getName());
		}

		Collections.sort(styleNames);
		styleNames.add(0, null);
		return styleNames.toArray(new String[styleNames.size()]);
	}

	//remove some properties based on the 'known' name
	protected boolean shouldShow(PropertyDescriptorWrapper propertyDescriptor) throws RepositoryException
	{
		if (persist != propertyDescriptor.valueObject) // for beans we show all
		{
			return true;
		}

		String name = propertyDescriptor.propertyDescriptor.getName();

		// check for content spec element.
		EclipseRepository repository = (EclipseRepository)persist.getRootObject().getRepository();
		Element element = repository.getContentSpec().getPropertyForObjectTypeByName(persist.getTypeID(), name);

		if (!RepositoryHelper.shouldShow(name, element, persist.getClass()))
		{
			return false;
		}

		if (name.equals("labelFor") && persist instanceof GraphicalComponent)
		{
			return ((GraphicalComponent)persist).getOnActionMethodID() < 1 || !((GraphicalComponent)persist).getShowClick();
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

		if (name.equals("loginFormID") && persist instanceof Solution && ((Solution)persist).getLoginFormID() <= 0)
		{
			try
			{
				if (((Solution)persist).getLoginSolutionName() != null)
				{
					// there is a login solution, do not show the deprecated login form setting
					return false;
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}


		return true;
	}

	protected Object getPersistPropertyValue(Object id)
	{
		init();
		PropertyDescriptorWrapper beanPropertyDescriptor = getBeansProperties().get(id);
		if (beanPropertyDescriptor != null)
		{
			try
			{
				return beanPropertyDescriptor.propertyDescriptor.getReadMethod().invoke(beanPropertyDescriptor.valueObject, new Object[0]);
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
		if (propertyDescriptor instanceof IPropertySetter)
		{
			// a combined property
			return ((IPropertySetter)propertyDescriptor).getProperty(this);
		}
		else
		{
			Object value = getPersistPropertyValue(id);
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
	}

	protected Object getDefaultPersistValue(Object id)
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
			AbstractRepository repository = (EclipseRepository)persist.getRootObject().getRepository();
			try
			{
				Element element = repository.getContentSpec().getPropertyForObjectTypeByName(persist.getTypeID(), (String)id);
				if (element == null)
				{
					// no content spec (example: form.width), try based on property descriptor
					PropertyDescriptorWrapper propertyDescriptor = beansProperties.get(id);
					if (propertyDescriptor != null)
					{
						if (propertyDescriptor.propertyDescriptor.getPropertyType() == int.class)
						{
							return repository.convertArgumentStringToObject(IRepository.INTEGER, null);
						}
						else if (propertyDescriptor.propertyDescriptor.getPropertyType() == boolean.class)
						{
							return repository.convertArgumentStringToObject(IRepository.BOOLEAN, null);
						}
					}
				}
				else
				{
					return repository.convertArgumentStringToObject(element.getTypeID(), element.getDefaultTextualClassValue());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Could not get default value for id " + id, e);
			}
		}
		if (persist == beanPropertyDescriptor.valueObject)
		{
			ServoyLog.logError("Could not find content spec value for id " + id + " type = " + persist.getTypeID(), null);
		}
		// else it is a bean property
		return null;
	}

	/**
	 * Find the actual property value id value using form hierarchy.
	 * <p>
	 * Value is converted using propertyController
	 * 
	 * @param value current or proposed value (when in select list dialog)
	 * @param id
	 * @return
	 */
	public Object getInheritedPropertyValue(Object value, Object id)
	{
		// compare the current value with the default value
		Object defaultValue = getDefaultPersistValue(id);
		IPropertyDescriptor propertyDescriptor = getPropertyDescriptor(id);
		Object propertyValue = value;
		if (propertyDescriptor instanceof IPropertyController)
		{
			// convert to property value before making proper comparison
			IPropertyConverter propertyConverter = ((IPropertyController)propertyDescriptor).getConverter();
			if (propertyConverter != null)
			{
				propertyValue = propertyConverter.convertValue(id, value);
			}
		}
		if (Utils.equalObjects(defaultValue, propertyValue))
		{
			// if it is a form property defined in a super-form, try to find that one
			if (persist instanceof Form && ((Form)persist).getExtendsFormID() > 0)
			{
				// show which default form method is actually called
				FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist);
				for (Form f : editingFlattenedSolution.getFormHierarchy((Form)persist))
				{
					if (f != persist)
					{
						Object superValue = new PersistPropertySource(f, null, true).getPersistPropertyValue(id);
						if (!Utils.equalObjects(defaultValue, superValue))
						{
							if (propertyDescriptor instanceof IPropertyController)
							{
								IPropertyConverter propertyConverter = ((IPropertyController)propertyDescriptor).getConverter();
								if (propertyConverter != null)
								{
									return propertyConverter.convertProperty(id, superValue);
								}
							}
							return superValue;
						}
					}
				}
			}
		}
		return value;
	}


	public boolean isPropertySet(Object id)
	{
		if (readOnly) return false;

		// recursively process a.b.c properties
		if (id instanceof String)
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
		init();

		// recursively process a.b.c properties
		if (id instanceof String)
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

		setPersistPropertyValue(id, getDefaultPersistValue(id));
	}

	private boolean isQuoted(String text)
	{
		return ((text.startsWith("'") && text.endsWith("'")) || (text.startsWith("\"") && text.endsWith("\"")));
	}

	public void setPersistPropertyValue(Object id, Object value)
	{
		init();
		PropertyDescriptorWrapper beanPropertyDescriptor = getBeansProperties().get(id);
		if (beanPropertyDescriptor != null)
		{
			try
			{
				if ("name".equals(id) && beanPropertyDescriptor.valueObject instanceof ISupportUpdateableName)
				{
					if (value instanceof String || value == null)
					{
						((ISupportUpdateableName)beanPropertyDescriptor.valueObject).updateName(
							ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator(), (String)value);
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
					beanPropertyDescriptor.propertyDescriptor.getWriteMethod().invoke(beanPropertyDescriptor.valueObject, new Object[] { value });
					if ("i18nDataSource".equals(id))
					{
						EclipseMessages.showDatasourceWarning();
						ServoyProject servoyProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
						EclipseMessages.writeProjectI18NFiles(servoyProject, false);
					}
				}
				if (persist instanceof Bean)
				{
					if (beanPropertyDescriptor.valueObject == persist)
					{
						// size, location and name are set on persist, not on bean instance
						if ("size".equals(id) && (value == null || value instanceof Dimension))
						{
							Object beanDesignInstance = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist).getBeanDesignInstance(
								(Bean)persist);
							if (beanDesignInstance instanceof Component)
							{
								((Component)beanDesignInstance).setSize((Dimension)value);
							}
						}
					}
					else
					{
						ComponentFactory.updateBeanWithItsXML((Bean)persist, beanPropertyDescriptor.valueObject);
					}
				}
			}
			catch (Exception e)
			{
				MessageDialog.openError(Display.getDefault().getActiveShell(), "Could not update property", e.getMessage());
				ServoyLog.logError("Could not set property value for id " + id + " on object " + beanPropertyDescriptor.valueObject, e);
			}

			if ("groupID".equals(id))
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persist.getParent(), true);
			}
			else
			{
				// fire persist change recursively if the style is changed
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, persist,
					"styleName".equals(id) || "extendsFormID".equals(id));
			}
		}
	}

	public void setPropertyValue(Object id, Object value)
	{
		if (readOnly)
		{
			throw new RuntimeException("Cannot save read-only property");
		}

		init();

		// recursively process a.b.c properties
		if (id instanceof String)
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
		if (propertyDescriptor instanceof IPropertySetter)
		{
			((IPropertySetter)propertyDescriptor).setProperty(this, value);
		}
		else
		{
			if (propertyDescriptor instanceof IPropertyController)
			{
				IPropertyConverter propertyConverter = ((IPropertyController)propertyDescriptor).getConverter();
				if (propertyConverter != null)
				{
					val = propertyConverter.convertValue(id, value);
				}
			}
			setPersistPropertyValue(id, val);
		}
	}

	@Override
	public String toString()
	{
		StringBuilder retval = new StringBuilder();
		if (persist instanceof Portal)
		{
			retval.append("Portal (relation: ").append(((Portal)persist).getRelationName()).append(')');
		}
		else if (persist instanceof GraphicalComponent)
		{
			retval.append("Label / Button");
		}
		else if (persist instanceof RectShape)
		{
			retval.append("Rectangle");
		}
		else
		{
			retval.append(persist.getClass().getName().replaceFirst(".*\\.", ""));
		}

		if (persist instanceof Tab)
		{
			String relationName = ((Tab)persist).getRelationName();
			if (relationName != null)
			{
				retval.append(" (relation: ").append(relationName).append(')');
			}
			else
			{
				retval.append(" (relationless)");
			}
		}
		else if (persist instanceof Bean)
		{
			retval.append(" (class: ");
			String beanClassName = ((Bean)persist).getBeanClassName();
			if (beanClassName != null)
			{
				retval.append(beanClassName.replaceFirst(".*\\.", ""));
			}
			retval.append(')');
		}

		String name = getActualComponentName(persist);
		if (name != null)
		{
			retval.append(" - ").append(name);
		}
		if (persist.getParent() instanceof Portal)
		{
			retval.append(" - (parent: portal)");
		}
		if (persist instanceof ISupportDataProviderID)
		{
			try
			{
				String dataprovider = ((ISupportDataProviderID)persist).getDataProviderID();
				FlattenedSolution editingFlattenedSolution = ServoyModelManager.getServoyModelManager().getServoyModel().getEditingFlattenedSolution(persist);
				Form flattenedForm = editingFlattenedSolution.getFlattenedForm(persist);
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
							retval.append("Global");
						}
					}
					retval.append('(');
					retval.append(dataprovider);
					retval.append(',');
					retval.append(Column.getDisplayTypeString(dp.getDataProviderType()));
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
						switch (dp.getDataProviderType())
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
	private static class PropertyDescriptorWrapper
	{
		public final java.beans.PropertyDescriptor propertyDescriptor;
		public final Object valueObject;
		private PropertyEditor propertyEditor;

		PropertyDescriptorWrapper(java.beans.PropertyDescriptor propertyDescriptor, Object valueObject)
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

	private IPropertyDescriptor getGeneralPropertyDescriptor(final String id, String displayName, String name, final Form form)
	{
		// Some properties apply to both persists and beans

		if (name.equals("dataSource"))
		{
			// cannot change table when we have a super-form
			return new DatasourceController(id, name, "Select table", readOnly, new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL,
				true), getFormInheritanceLabelProvider(persist, DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED, id));
		}

		if (name.equals("i18nDataSource"))
		{
			return new DatasourceController(id, name, "Select I18N table", readOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.I18N, true), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}

		if (name.equals("primaryDataSource"))
		{
			return new DatasourceController(id, name, "Select Primary table", readOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.ALL, false), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}

		if (name.equals("foreignDataSource"))
		{
			return new DatasourceController(id, name, "Select Foreign table", readOnly, new TableContentProvider.TableListOptions(
				TableListOptions.TableListType.ALL, false), DatasourceLabelProvider.INSTANCE_NO_IMAGE_FULLY_QUALIFIED);
		}

		if (name.equals("scrollbars"))
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
			}, getFormInheritanceLabelProvider(persist, ScrollbarSettingLabelProvider.INSTANCE, id), new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new CellEditor(parent, SWT.NONE)
					{
						@Override
						protected Control createControl(Composite parent)
						{
							return new Label(parent, SWT.NONE);
						}

						@Override
						protected Object doGetValue()
						{
							return form.getScrollbars();
						}

						@Override
						protected void doSetFocus()
						{

						}

						@Override
						protected void doSetValue(Object value)
						{
							if (getControl() != null && !getControl().isDisposed()) ((Label)getControl()).setText(ScrollbarSettingLabelProvider.INSTANCE.getText(value));
						}
					};
				}
			});
		}

		if ("horizontalAlignment".equals(name))
		{
			if (HORIZONTAL_ALIGNMENT_CONTROLLER == null)
			{
				HORIZONTAL_ALIGNMENT_CONTROLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(
					new Integer[] { new Integer(-1), new Integer(SwingConstants.LEFT), new Integer(SwingConstants.CENTER), new Integer(SwingConstants.RIGHT) },
					new String[] { Messages.LabelDefault, Messages.AlignLeft, Messages.AlignCenter, Messages.AlignRight }), Messages.LabelUnresolved);
			}
			return HORIZONTAL_ALIGNMENT_CONTROLLER;
		}

		return null;
	}

	private IPropertyDescriptor getPropertiesPropertyDescriptor(final String id, String displayName, String name, final Form flattenedForm,
		final FlattenedSolution flattenedEditingSolution) throws RepositoryException
	{
		if ("tabSeq".equals(name))
		{
			return new PropertyController<String, Integer>(id, displayName);
		}
		if ("verticalAlignment".equals(name))
		{
			if (VERTICAL_ALIGNMENT_CONTROLLER == null)
			{
				VERTICAL_ALIGNMENT_CONTROLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(
					new Integer[] { new Integer(-1), new Integer(SwingConstants.TOP), new Integer(SwingConstants.CENTER), new Integer(SwingConstants.BOTTOM) },
					new String[] { Messages.LabelDefault, Messages.AlignTop, Messages.AlignCenter, Messages.AlignBottom }), Messages.LabelUnresolved);
			}
			return VERTICAL_ALIGNMENT_CONTROLLER;
		}

		if (name.equals("borderType"))
		{
			final BorderPropertyController borderPropertyController = new BorderPropertyController(id, displayName, this, persist);
			borderPropertyController.setReadonly(readOnly);

			// BorderPropertyController handles Border objects, the property is a String.
			IPropertyConverter<String, Border> borderStringConverter = new IPropertyConverter<String, Border>()
			{
				public Border convertProperty(Object property, String value)
				{
					return ComponentFactoryHelper.createBorder(value, true);
				}

				public String convertValue(Object property, Border value)
				{
					return ComponentFactoryHelper.createBorderString(value);
				}
			};
			return new PropertyController<String, Object>(id, displayName, new ChainedPropertyConverter<String, Border, Object>(borderStringConverter,
				borderPropertyController.getConverter()), getFormInheritanceLabelProvider(persist, borderPropertyController.getLabelProvider(), id),
				borderPropertyController);
		}

		if (name.equals("mediaOptions"))
		{
			return new MediaoptionsPropertyController(id, displayName, this);
		}

		if (name.endsWith("printSliding"))
		{
			return new SlidingoptionsPropertyController(id, displayName);
		}

		if (name.equals("fontType"))
		{
			IPropertyConverter<String, java.awt.Font> fontStringConverter = new IPropertyConverter<String, java.awt.Font>()
			{
				public java.awt.Font convertProperty(Object property, String value)
				{
					return PersistHelper.createFont(value);
				}

				public String convertValue(Object property, java.awt.Font value)
				{
					return PersistHelper.createFontString(value);
				}
			};
			final IDefaultValue<FontData[]> getLastPaintedFont;
			if (persist instanceof AbstractBase)
			{
				getLastPaintedFont = new IDefaultValue<FontData[]>()
				{
					public FontData[] getValue()
					{
						return PropertyFontConverter.INSTANCE.convertProperty(id, ((AbstractBase)persist).getRuntimeProperty(LastPaintedFontProperty));
					}
				};
			}
			else
			{
				getLastPaintedFont = null;
			}
			return new PropertyController<String, FontData[]>(id, displayName, new ChainedPropertyConverter<String, java.awt.Font, FontData[]>(
				fontStringConverter, PropertyFontConverter.INSTANCE), FontLabelProvider.INSTANCE, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new FontCellEditor(parent, getLastPaintedFont);
				}
			});
		}

		if (name.endsWith("dataProviderID"))
		{
			Table table = null;
			Portal portal = (Portal)persist.getAncestor(IRepository.PORTALS);
			final DataProviderOptions options;
			if (portal != null)
			{
				Relation[] relations = flattenedEditingSolution.getRelationSequence(portal.getRelationName());
				if (relations == null)
				{
					return null;
				}
				options = new DataProviderTreeViewer.DataProviderOptions(false, false, false, true /* related calcs */, false, false, false, false,
					INCLUDE_RELATIONS.YES, true, true, relations);
			}
			else
			{
				if (flattenedForm == null) return null;
				try
				{
					table = flattenedForm.getTable();
				}
				catch (Exception ex)
				{
					ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
					return null;
				}

				options = new DataProviderTreeViewer.DataProviderOptions(true, table != null, table != null, table != null, true, true, table != null,
					table != null, INCLUDE_RELATIONS.NESTED, true, true, null);
			}
			final DataProviderConverter converter = new DataProviderConverter(flattenedEditingSolution, persist, table);
			DataProviderLabelProvider showPrefix = new DataProviderLabelProvider(false);
			showPrefix.setConverter(converter);
			DataProviderLabelProvider hidePrefix = new DataProviderLabelProvider(true);
			hidePrefix.setConverter(converter);

			ILabelProvider labelProviderShowPrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(showPrefix, context),
				context);
			final ILabelProvider labelProviderHidePrefix = new SolutionContextDelegateLabelProvider(new FormContextDelegateLabelProvider(hidePrefix, context),
				context);
			PropertyController<String, String> propertyController = new PropertyController<String, String>(id, displayName, null, labelProviderShowPrefix,
				new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new DataProviderCellEditor(parent, labelProviderHidePrefix, getFormInheritanceValueEditor(persist, new DataProviderValueEditor(
							converter), id), flattenedForm, flattenedEditingSolution, readOnly, options, converter);
					}
				});
			propertyController.setSupportsReadonly(true);
			return propertyController;
		}

		if (name.equals("relationName"))
		{
			Table primaryTable = null;
			if (flattenedForm != null)
			{
				try
				{
					primaryTable = flattenedForm.getTable();
				}
				catch (Exception ex)
				{
					ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
				}
			}
			Table foreignTable = null;
			boolean incudeNone = false;
			if (persist instanceof Tab)
			{
				Form tabForm = flattenedEditingSolution.getForm(((Tab)persist).getContainsFormID());
				if (tabForm != null)
				{
					try
					{
						foreignTable = tabForm.getTable();
					}
					catch (Exception ex)
					{
						ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
					}
				}
				incudeNone = true; // unrelated tabs
			}
			else if (persist instanceof Portal)
			{
				// only show relations that go to the same table
				Relation[] relationSequence = flattenedEditingSolution.getRelationSequence(((Portal)persist).getRelationName());
				if (relationSequence != null)
				{
					foreignTable = relationSequence[relationSequence.length - 1].getForeignTable();
				}
			}
			return new RelationPropertyController(id, displayName, persist, context, primaryTable, foreignTable, incudeNone, true);
		}

		if (name.endsWith("rowBGColorCalculation"))
		{
			if (flattenedForm == null) return null;
			Table table = null;
			if (persist instanceof Portal)
			{
				Relation[] relations = flattenedEditingSolution.getRelationSequence(((Portal)persist).getRelationName());
				if (relations != null)
				{
					table = relations[relations.length - 1].getForeignTable();
				}
			}
			else
			{
				try
				{
					table = flattenedForm.getTable();
				}
				catch (RepositoryException e)
				{
					ServoyLog.logInfo("Table form not accessible: " + e.getMessage());
				}
			}
			return new ScriptProviderPropertyController(id, displayName, table, persist, context);
		}

		if ("variableType".equals(name) && persist instanceof ScriptVariable)
		{
			if (VARIABLE_TYPE_CONTROLLER == null)
			{
				int[] iTypes = Column.allDefinedTypes;
				Integer[] integerTypes = new Integer[iTypes.length];
				String[] stringTypes = new String[iTypes.length];
				for (int i = 0; i < iTypes.length; i++)
				{
					integerTypes[i] = new Integer(iTypes[i]);
					stringTypes[i] = Column.getDisplayTypeString(iTypes[i]);
				}
				VARIABLE_TYPE_CONTROLLER = new PropertySetterDelegatePropertyController<Integer, Integer>(new ComboboxPropertyController<Integer>(id,
					displayName, new ComboboxPropertyModel<Integer>(integerTypes, stringTypes), Messages.LabelUnresolved), id)
				{
					// handle setting of this property via a IPropertySetter so that we can do additional stuff when the property is set.
					public Integer getProperty(IPropertySource propertySource)
					{
						PersistPropertySource pp = (PersistPropertySource)propertySource;
						return getConverter().convertProperty(id, (Integer)pp.getPersistPropertyValue(id));
					}

					public void setProperty(IPropertySource propertySource, Integer value)
					{
						PersistPropertySource pp = (PersistPropertySource)propertySource;
						Integer convertedValue = getConverter().convertValue(id, value);
						pp.setPersistPropertyValue(id, convertedValue);

						// the variable type is being set; the default value should change if incompatible
						ScriptVariable variable = (ScriptVariable)pp.getPersist();
						String defaultValue = variable.getDefaultValue();
						if (defaultValue != null)
						{
							String newDefault = null;
							int type = convertedValue.intValue();
							if (type == IColumnTypes.TEXT)
							{
								if (!isQuoted(defaultValue))
								{
									newDefault = "'" + defaultValue + "'";
								}
							}
							else if (type == IColumnTypes.INTEGER)
							{
								if (isQuoted(defaultValue))
								{
									newDefault = defaultValue.substring(1, defaultValue.length() - 1);
								}
								else
								{
									newDefault = defaultValue;
								}
								try
								{
									Integer.parseInt(newDefault);
									newDefault = (defaultValue == newDefault) ? null : newDefault;
								}
								catch (NumberFormatException e)
								{
									newDefault = "";
								}
							}
							else if (type == IColumnTypes.NUMBER)
							{
								if (isQuoted(defaultValue))
								{
									newDefault = defaultValue.substring(1, defaultValue.length() - 1);
								}
								else
								{
									newDefault = defaultValue;
								}
								try
								{
									Double.parseDouble(newDefault);
									newDefault = (defaultValue == newDefault) ? null : newDefault;
								}
								catch (NumberFormatException e)
								{
									newDefault = "";
								}
							}

							if (newDefault != null)
							{
								variable.setDefaultValue(newDefault);
							}
						}
					}

					public void resetPropertyValue(IPropertySource propertySource)
					{
						PersistPropertySource pp = (PersistPropertySource)propertySource;
						setPersistPropertyValue(id, pp.getDefaultPersistValue(id));
					}

					public boolean isPropertySet(IPropertySource propertySource)
					{
						PersistPropertySource pp = (PersistPropertySource)propertySource;
						Object defaultValue = pp.getDefaultPersistValue(id);
						Object propertyValue = pp.getPersistPropertyValue(id);
						return defaultValue != propertyValue && (defaultValue == null || !defaultValue.equals(propertyValue));
					}

				};
			}
			return VARIABLE_TYPE_CONTROLLER;
		}

		if ("rolloverCursor".equals(name))
		{
			if (ROLLOVER_CURSOR_CONTROLLER == null)
			{
				ROLLOVER_CURSOR_CONTROLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(
					new Integer[] { new Integer(Cursor.DEFAULT_CURSOR), new Integer(Cursor.HAND_CURSOR) },
					new String[] { Messages.LabelDefault, Messages.CursorHand }), Messages.LabelUnresolved);
			}
			return ROLLOVER_CURSOR_CONTROLLER;
		}

		if (name.endsWith("shapeType"))
		{
			if (SHAPE_TYPE_CONTOLLER == null)
			{
				SHAPE_TYPE_CONTOLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(new Integer[] { new Integer(
					RectShape.BORDER_PANEL), new Integer(RectShape.RECTANGLE), new Integer(RectShape.ROUNDED_RECTANGLE), new Integer(RectShape.OVAL) },
					new String[] { "BORDER_PANEL", "RECTANGLE", "ROUNDED_RECTANGLE", "OVAL" }), Messages.LabelUnresolved);
			}
			return SHAPE_TYPE_CONTOLLER;
		}

		if (name.equals("view"))
		{
			if (VIEW_TYPE_CONTOLLER == null)
			{
				VIEW_TYPE_CONTOLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(
					new Integer[] { new Integer(IForm.RECORD_VIEW), new Integer(IForm.LIST_VIEW), new Integer(IForm.LOCKED_RECORD_VIEW), new Integer(
						FormController.LOCKED_LIST_VIEW), new Integer(FormController.LOCKED_TABLE_VIEW) },
					new String[] { "Record view", "List view", "Record view (locked)", "List view (locked)", "Table view (locked)" }), Messages.LabelUnresolved);
			}
			return VIEW_TYPE_CONTOLLER;
		}

		if (name.equals("textOrientation"))
		{
			if (TEXT_ORIENTATION_CONTROLLER == null)
			{
				TEXT_ORIENTATION_CONTROLLER = new PropertySetterDelegatePropertyController<Integer, Integer>(
					new ComboboxPropertyController<Integer>(
						id,
						displayName,
						new ComboboxPropertyModel<Integer>(
							new Integer[] { new Integer(Solution.TEXT_ORIENTATION_DEFAULT), new Integer(Solution.TEXT_ORIENTATION_LEFT_TO_RIGHT), new Integer(
								Solution.TEXT_ORIENTATION_RIGHT_TO_LEFT), new Integer(Solution.TEXT_ORIENTATION_LOCALE_SPECIFIC) },
							new String[] { Messages.LabelDefault, Messages.OrientationLeftToRight, Messages.OrientationRightToLeft, Messages.OrientationLocaleSpecific }),
						Messages.LabelUnresolved), id)
				{
					private Shell shell;

					// handle setting of this property via a IPropertySetter so that we can do additional stuff when the property is set.
					public Integer getProperty(IPropertySource propertySource)
					{
						return getConverter().convertProperty(id, (Integer)getPersistPropertyValue(id));
					}

					public void setProperty(IPropertySource propertySource, Integer value)
					{
						Integer convertedValue = getConverter().convertValue(id, value);
						setPersistPropertyValue(id, convertedValue);
						MessageDialog.openInformation(shell, "Orientation change", "You must restart servoy clients after changing the orientation");
					}

					public void resetPropertyValue(IPropertySource propertySource)
					{
						setPersistPropertyValue(id, getDefaultPersistValue(id));
					}

					public boolean isPropertySet(IPropertySource propertySource)
					{
						Object defaultValue = getDefaultPersistValue(id);
						Object propertyValue = getPersistPropertyValue(id);
						return defaultValue != propertyValue && (defaultValue == null || !defaultValue.equals(propertyValue));
					}

					@Override
					public CellEditor createPropertyEditor(Composite parent)
					{
						shell = parent.getShell();
						return super.createPropertyEditor(parent);
					}

				};
			}
			return TEXT_ORIENTATION_CONTROLLER;
		}

		if (name.equals("solutionType"))
		{
			if (SOLUTION_TYPE_CONTROLLER == null)
			{
				Integer[] ia = new Integer[SolutionMetaData.solutionTypes.length];
				for (int i = 0; i < ia.length; i++)
				{
					ia[i] = new Integer(SolutionMetaData.solutionTypes[i]);
				}
				SOLUTION_TYPE_CONTROLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(ia,
					SolutionMetaData.solutionTypeNames), Messages.LabelUnresolved);
			}
			return SOLUTION_TYPE_CONTROLLER;
		}

		if (name.equals("joinType"))
		{
			if (JOIN_TYPE_CONTROLLER == null)
			{
				JOIN_TYPE_CONTROLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(

				new Integer[] { new Integer(ISQLJoin.INNER_JOIN), new Integer(ISQLJoin.LEFT_OUTER_JOIN) },
					new String[] { ISQLJoin.JOIN_TYPES_NAMES[ISQLJoin.INNER_JOIN], ISQLJoin.JOIN_TYPES_NAMES[ISQLJoin.LEFT_OUTER_JOIN] }),
					Messages.LabelUnresolved);
			}
			return JOIN_TYPE_CONTROLLER;
		}

		if (name.equals("displayType"))
		{
			if (DISPLAY_TYPE_CONTOLLER == null)
			{
				DISPLAY_TYPE_CONTOLLER = new ComboboxPropertyController<Integer>(
					id,
					displayName,
					new ComboboxPropertyModel<Integer>(
						new Integer[] { new Integer(Field.TEXT_FIELD), new Integer(Field.TEXT_AREA), new Integer(Field.RTF_AREA), new Integer(Field.HTML_AREA), new Integer(
							Field.TYPE_AHEAD), new Integer(Field.COMBOBOX), new Integer(Field.RADIOS), new Integer(Field.CHECKS), new Integer(Field.CALENDAR), new Integer(
							Field.IMAGE_MEDIA), new Integer(Field.PASSWORD) },
						new String[] { "TEXT_FIELD", "TEXT_AREA", "RTF_AREA", "HTML_AREA", "TYPE_AHEAD", "COMBOBOX", "RADIOS", "CHECK", "CALENDAR", "IMAGE_MEDIA", "PASSWORD" }),
					Messages.LabelUnresolved);
			}
			return DISPLAY_TYPE_CONTOLLER;
		}

		if (name.equals("namedFoundSet"))
		{
			if (NAMEDFOUNDSET_CONTOLLER == null)
			{
				NAMEDFOUNDSET_CONTOLLER = new ComboboxPropertyController<String>(id, displayName, new ComboboxPropertyModel<String>(
					new String[] { null, Form.SEPARATE_FLAG }, new String[] { Messages.LabelDefault, Form.SEPARATE_FLAG }), Messages.LabelUnresolved);
			}
			return NAMEDFOUNDSET_CONTOLLER;
		}

		if (name.equals("tabOrientation"))
		{
			if (TAB_ORIENTATION_CONTROLLER == null)
			{
				TAB_ORIENTATION_CONTROLLER = new ComboboxPropertyController<Integer>(
					id,
					displayName,
					new ComboboxPropertyModel<Integer>(
						new Integer[] { new Integer(TabPanel.DEFAULT), new Integer(SwingConstants.TOP), new Integer(SwingConstants.RIGHT), new Integer(
							SwingConstants.BOTTOM), new Integer(SwingConstants.LEFT), new Integer(TabPanel.HIDE), new Integer(TabPanel.SPLIT_HORIZONTAL), new Integer(
							TabPanel.SPLIT_VERTICAL) },
						new String[] { Messages.LabelDefault, Messages.AlignTop, Messages.AlignRight, Messages.AlignBottom, Messages.AlignLeft, "HIDE", "SPLIT HORIZONTAL", "SPLIT VERTICAL" }),
					Messages.LabelUnresolved);
			}
			return TAB_ORIENTATION_CONTROLLER;
		}

		if (name.equals("format"))
		{
			return new FormatPropertyController(id, displayName, persist);
		}
		if (name.equals("labelFor"))
		{
			if (flattenedForm != null)
			{
				List<String> names = new ArrayList<String>();
				Iterator<Field> it = flattenedForm.getFields();
				while (it.hasNext())
				{
					Field field = it.next();
					if (field.getName() != null && !"".equals(field.getName()))
					{
						names.add(field.getName());
					}
				}
				return new EditableComboboxPropertyController(id, displayName, names.toArray(new String[] { }));
			}
		}
		if (name.equals("mnemonic"))
		{
			if (MNEMONIC_CONTROLLER == null)
			{
				MNEMONIC_CONTROLLER = new EditableComboboxPropertyController(id, displayName, MNEMONIC_VALUES);
			}
			return MNEMONIC_CONTROLLER;
		}
		if (name.endsWith("navigatorID"))
		{
			final ILabelProvider formLabelProvider = getFormInheritanceLabelProvider(persist, new SolutionContextDelegateLabelProvider(new FormLabelProvider(
				flattenedEditingSolution, false), context), id);
			PropertyDescriptor pd = new PropertyDescriptor(id, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new ListSelectCellEditor(parent, "Select navigator form", new FormContentProvider(flattenedEditingSolution, (Form)persist),
						formLabelProvider, getFormInheritanceValueEditor(persist, new FormValueEditor(flattenedEditingSolution), id), readOnly,
						new FormContentProvider.FormListOptions(FormListOptions.FormListType.FORMS, Boolean.FALSE, true, true, true), SWT.NONE, null,
						"navigatorFormDialog");
				}
			};
			pd.setLabelProvider(formLabelProvider);
			return pd;
		}

		if (name.equals("firstFormID") || name.equals("loginFormID"))
		{
			final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, false), context);
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

		if (name.equals("extendsFormID"))
		{
			if (flattenedForm == null) return null;
			final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, true), context);
			PropertyDescriptor pd = new PropertyDescriptor(id, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					Form form = flattenedEditingSolution.getForm(flattenedForm.getID());
					return new ListSelectCellEditor(parent, "Select parent form", new FormContentProvider(flattenedEditingSolution, form), formLabelProvider,
						new FormValueEditor(flattenedEditingSolution), readOnly, new FormContentProvider.FormListOptions(
							FormListOptions.FormListType.HIERARCHY, null, false, true, false), SWT.NONE, null, "parentFormDialog");
				}
			};
			pd.setLabelProvider(formLabelProvider);
			return pd;
		}

		if (name.endsWith("valuelistID"))
		{
			return new ValuelistPropertyController<Integer>(id, displayName, persist, context, true);
		}

		if (name.equals("rolloverImageMediaID") || name.equals("imageMediaID"))
		{
			return new MediaPropertyController<Integer>(id, displayName, persist, context, true);
		}

		if (name.equals("styleClass"))
		{
			return new StyleclassPropertyController(id, displayName, persist, StyleClassesComboboxModel.getStyleLookupname(persist));
		}

		if (name.equals("styleName"))
		{
			return new ComboboxPropertyController<String>(id, displayName, new ComboboxPropertyModel<String>(getStyleNames(),
				NullDefaultLabelProvider.LABEL_DEFAULT), Messages.LabelUnresolved)
			{
				@Override
				public ILabelProvider getLabelProvider()
				{
					return getFormInheritanceLabelProvider(persist, super.getLabelProvider(), id);
				}
			};
		}

		if (name.equals("anchors"))
		{
			return new AnchorPropertyController(id, displayName);
		}

		if (name.equals("rotation"))
		{
			if (ROTATION_CONTROLLER == null)
			{
				ROTATION_CONTROLLER = new ComboboxPropertyController<Integer>(id, displayName, new ComboboxPropertyModel<Integer>(
					new Integer[] { new Integer(0), new Integer(90), new Integer(180), new Integer(270) }), Messages.LabelUnresolved);
			}
			return ROTATION_CONTROLLER;
		}

		if (name.equals("groupbyDataProviderIDs"))
		{
			return new PropertyController<String, Object[]>(id, displayName, new StringTokenizerConverter(",", true), null, null);
		}

		if (name.equals("modulesNames"))
		{
			// list the available modules as well as unknown modules that are currently set.
			List<String> availableSolutions = getSolutionNames();
			StringTokenizerConverter converter = new StringTokenizerConverter(",", true);

			String[] modulesNames = null;
			if (persist instanceof Solution)
			{
				modulesNames = converter.convertProperty(id, ((Solution)persist).getModulesNames());
			}

			final List<String> allSolutions = new ArrayList<String>(availableSolutions);
			if (modulesNames != null)
			{
				for (String module : modulesNames)
				{
					if (!allSolutions.contains(module))
					{
						allSolutions.add(module);
					}
				}
			}

			final ILabelProvider labelProvider;
			if (allSolutions.size() == availableSolutions.size())
			{
				// no 'unknown' modules
				labelProvider = new ArrayLabelProvider(converter);
			}
			else
			{
				// found 'unknown' modules in current value, mark them with italic font
				labelProvider = new ValidvalueDelegatelabelProvider(new ArrayLabelProvider(converter), availableSolutions, null, FontResource.getDefaultFont(
					SWT.ITALIC, 0));
				Collections.sort(allSolutions);
			}

			return new PropertyController<String, Object[]>(id, displayName, converter, labelProvider, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new ListSelectCellEditor(parent, "Select modules", labelProvider, null, readOnly, allSolutions.toArray(), SWT.MULTI | SWT.CHECK,
						null, "selectModules");
				}
			});
		}

		if (name.endsWith("initialSort"))
		{
			final ITableDisplay tableDisplay;
			if (persist instanceof Portal)
			{
				tableDisplay = new ITableDisplay()
				{
					public String getInitialSort()
					{
						return ((Portal)persist).getInitialSort();
					}

					public Table getTable() throws RepositoryException
					{
						Table table = null;
						Relation[] relations = flattenedEditingSolution.getRelationSequence(((Portal)persist).getRelationName());
						if (relations != null)
						{
							table = relations[relations.length - 1].getForeignTable();
						}
						return table;
					}
				};
			}
			else if (flattenedForm != null)
			{
				tableDisplay = flattenedForm;
			}
			else if (persist instanceof Relation)
			{
				tableDisplay = new ITableDisplay()
				{
					public String getInitialSort()
					{
						return ((Relation)persist).getInitialSort();
					}

					public Table getTable() throws RepositoryException
					{
						return ((Relation)persist).getForeignTable();
					}
				};
			}
			else
			{
				tableDisplay = null;
			}
			if (tableDisplay == null)
			{
				return null;
			}

			final ILabelProvider labelProvider = getFormInheritanceLabelProvider(persist, NullDefaultLabelProvider.LABEL_DEFAULT, id);

			return new PropertyController<String, String>(id, displayName, NULL_STRING_CONVERTER, labelProvider, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new SortCellEditor(parent, flattenedEditingSolution, tableDisplay, "Select sorting fields", labelProvider);
				}
			});
		}

		if (name.equals("text") || name.equals("toolTipText") || name.equals("titleText"))
		{
			Table table = null;
			if (flattenedForm != null)
			{
				try
				{
					table = flattenedForm.getTable();
				}
				catch (Exception ex)
				{
					ServoyLog.logInfo("Table form not accessible: " + ex.getMessage());
				}
			}
			final Table finalTable = table;
			return new PropertyDescriptor(id, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new TagsAndI18NTextCellEditor(parent, persist, flattenedEditingSolution, TextCutoffLabelProvider.DEFAULT, finalTable,
						"Edit text property", Activator.getDefault().getDesignClient());
				}

				@Override
				public ILabelProvider getLabelProvider()
				{
					return TextCutoffLabelProvider.DEFAULT;
				}
			};
		}

		if (name.equals("name"))
		{
			return new PropertyController<String, String>(id, displayName, NULL_STRING_CONVERTER, null, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					VerifyingTextCellEditor cellEditor = new VerifyingTextCellEditor(parent);
					cellEditor.addVerifyListener(DocumentValidatorVerifyListener.IDENT_SERVOY_VERIFIER);
					cellEditor.setValidator(new ICellEditorValidator()
					{
						public String isValid(Object value)
						{
							if (value instanceof String && ((String)value).length() > 0)
							{
								try
								{
									if (persist instanceof IFormElement)
									{
										ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName((String)value,
											persist.getID(), new ValidatorSearchContext(persist.getParent(), IRepository.ELEMENTS), false);

									}
									else if (persist instanceof Form)
									{
										ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName((String)value,
											persist.getID(), new ValidatorSearchContext(persist, IRepository.FORMS), false);
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
			});
		}

		if (name.equals("defaultPageFormat"))
		{
			final ILabelProvider labelProvider = getFormInheritanceLabelProvider(persist, PageFormatLabelProvider.INSTANCE, id);
			return new PropertyController<String, PageFormat>(id, displayName, new IPropertyConverter<String, PageFormat>()
			{
				public PageFormat convertProperty(Object id, String value)
				{
					return PersistHelper.createPageFormat(value);
				}

				public String convertValue(Object id, PageFormat value)
				{
					return PersistHelper.createPageFormatString(value);
				}

			}, labelProvider, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new PageFormatEditor(parent, flattenedForm, "Page Setup", labelProvider);
				}
			});
		}

		if (name.equals("loginSolutionName"))
		{
			return new LoginSolutionPropertyController(id, displayName);
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
}
