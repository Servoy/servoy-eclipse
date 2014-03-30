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

import java.awt.Cursor;
import java.awt.Point;
import java.awt.print.PageFormat;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.swing.SwingConstants;

import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.PropertyDescriptor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.FormContentProvider;
import com.servoy.eclipse.ui.dialogs.FormContentProvider.FormListOptions;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.editors.PageFormatEditor;
import com.servoy.eclipse.ui.editors.SortCellEditor;
import com.servoy.eclipse.ui.editors.TabSeqDialogCellEditor;
import com.servoy.eclipse.ui.editors.TabSeqDialogCellEditor.TabSeqDialogValueEditor;
import com.servoy.eclipse.ui.labelproviders.FormLabelProvider;
import com.servoy.eclipse.ui.labelproviders.PageFormatLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PersistPropertySource.NullDefaultLabelProvider;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IContentSpecConstants;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ITableDisplay;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RectShape;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.query.ISQLJoin;
import com.servoy.j2db.server.ngclient.property.PropertyDescription;
import com.servoy.j2db.server.ngclient.property.PropertyType;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;


/**
 * Property handler for persist objects.
 * 
 * @author rgansevles
 *
 */
public class PersistPropertyHandler extends BasePropertyHandler
{
	public static final PropertyDescription ROTATION_VALUES = new PropertyDescription("rotation", PropertyType.values,
		new Integer[] { Integer.valueOf(0), Integer.valueOf(90), Integer.valueOf(180), Integer.valueOf(270) });

	public static final PropertyDescription HORIZONTAL_ALIGNMENT_VALUES = new PropertyDescription("horizontalAlignment", PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(SwingConstants.LEFT), Integer.valueOf(SwingConstants.CENTER), Integer.valueOf(SwingConstants.RIGHT) },
			new String[] { Messages.AlignLeft, Messages.AlignCenter, Messages.AlignRight }).addDefault(Integer.valueOf(-1), null));

	public static final PropertyDescription SOLUTION_TYPE_VALUES;
	static
	{
		Integer[] ia = new Integer[SolutionMetaData.solutionTypes.length];
		for (int i = 0; i < ia.length; i++)
		{
			ia[i] = Integer.valueOf(SolutionMetaData.solutionTypes[i]);
		}
		SOLUTION_TYPE_VALUES = new PropertyDescription("solutionType", PropertyType.values, new PropertyType.ValuesConfig().setValues(ia,
			SolutionMetaData.solutionTypeNames));
	}

	public static final PropertyDescription TEXT_ORIENTATION_VALUES = new PropertyDescription(
		"textOrientation",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(Solution.TEXT_ORIENTATION_LEFT_TO_RIGHT), Integer.valueOf(Solution.TEXT_ORIENTATION_RIGHT_TO_LEFT), Integer.valueOf(Solution.TEXT_ORIENTATION_LOCALE_SPECIFIC) },
			new String[] { Messages.OrientationLeftToRight, Messages.OrientationRightToLeft, Messages.OrientationLocaleSpecific }).addDefault(
			Integer.valueOf(Solution.TEXT_ORIENTATION_DEFAULT), null));

	public static final PropertyDescription ROLLOVER_CURSOR_VALUES = new PropertyDescription("rolloverCursor", PropertyType.values,
		new PropertyType.ValuesConfig().setValues(new Integer[] { Integer.valueOf(Cursor.HAND_CURSOR) }, new String[] { Messages.CursorHand }).addDefault(
			Integer.valueOf(Cursor.DEFAULT_CURSOR), null));

	public static final PropertyDescription SHAPE_TYPE_VALUES = new PropertyDescription(
		"shapeType",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(RectShape.BORDER_PANEL), Integer.valueOf(RectShape.RECTANGLE), Integer.valueOf(RectShape.ROUNDED_RECTANGLE), Integer.valueOf(RectShape.OVAL) },
			new String[] { "BORDER_PANEL", "RECTANGLE", "ROUNDED_RECTANGLE", "OVAL" }));

	public static final PropertyDescription VIEW_TYPE_VALUES = new PropertyDescription(
		"view",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(IForm.RECORD_VIEW), Integer.valueOf(IForm.LIST_VIEW), Integer.valueOf(IForm.LOCKED_RECORD_VIEW), Integer.valueOf(FormController.LOCKED_LIST_VIEW), Integer.valueOf(FormController.LOCKED_TABLE_VIEW) },
			new String[] { "Record view", "List view", "Record view (locked)", "List view (locked)", "Table view (locked)" }));

	public static final PropertyDescription SELECTION_MODE_VALUES = new PropertyDescription("selectionMode", PropertyType.values,
		new PropertyType.ValuesConfig().setValues(new Integer[] { Integer.valueOf(IForm.SELECTION_MODE_SINGLE), Integer.valueOf(IForm.SELECTION_MODE_MULTI) },
			new String[] { Messages.SelectionModeSingle, Messages.SelectionModeMulti }).addDefault(Integer.valueOf(IForm.SELECTION_MODE_DEFAULT), null));

	public static final PropertyDescription JOIN_TYPE_VALUES = new PropertyDescription("joinType", PropertyType.values,
		new PropertyType.ValuesConfig().setValues(new Integer[] { Integer.valueOf(ISQLJoin.INNER_JOIN), Integer.valueOf(ISQLJoin.LEFT_OUTER_JOIN) },
			new String[] { ISQLJoin.JOIN_TYPES_NAMES[ISQLJoin.INNER_JOIN], ISQLJoin.JOIN_TYPES_NAMES[ISQLJoin.LEFT_OUTER_JOIN] }));

	public static final PropertyDescription DISPLAY_TYPE_VALUES = new PropertyDescription(
		"displayType",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(Field.TEXT_FIELD), Integer.valueOf(Field.TEXT_AREA), Integer.valueOf(Field.RTF_AREA), Integer.valueOf(Field.HTML_AREA), Integer.valueOf(Field.TYPE_AHEAD), Integer.valueOf(Field.COMBOBOX), Integer.valueOf(Field.RADIOS), Integer.valueOf(Field.CHECKS), Integer.valueOf(Field.CALENDAR), Integer.valueOf(Field.IMAGE_MEDIA), Integer.valueOf(Field.PASSWORD), Integer.valueOf(Field.LIST_BOX), Integer.valueOf(Field.MULTISELECT_LISTBOX), Integer.valueOf(Field.SPINNER) },
			new String[] { "TEXT_FIELD", "TEXT_AREA", "RTF_AREA", "HTML_AREA", "TYPE_AHEAD", "COMBOBOX", "RADIOS", "CHECK", "CALENDAR", "IMAGE_MEDIA", "PASSWORD", "LISTBOX", "MULTISELECT_LISTBOX", "SPINNER" }));

	public static final PropertyDescription TAB_ORIENTATION_VALUES = new PropertyDescription(
		"tabOrientation",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(SwingConstants.TOP), Integer.valueOf(SwingConstants.RIGHT), Integer.valueOf(SwingConstants.BOTTOM), Integer.valueOf(SwingConstants.LEFT), Integer.valueOf(TabPanel.HIDE), Integer.valueOf(TabPanel.SPLIT_HORIZONTAL), Integer.valueOf(TabPanel.SPLIT_VERTICAL), Integer.valueOf(TabPanel.ACCORDION_PANEL) },
			new String[] { Messages.AlignTop, Messages.AlignRight, Messages.AlignBottom, Messages.AlignLeft, "HIDE", "SPLIT HORIZONTAL", "SPLIT VERTICAL", "ACCORDION PANE" }).addDefault(
			Integer.valueOf(TabPanel.DEFAULT_ORIENTATION), null));

	public static final PropertyDescription MNEMONIC_VALUES = new PropertyDescription(
		"mnemonic",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new String[] { "a", "b", "c", "d", "e", "f", "g", "h", "i", "j", "k", "l", "m", "n", "o", "p", "q", "r", "s", "t", "u", "v", "w", "x", "y", "z" }).setEditable(
			true));


	// null type: use property controller internally
	public static final PropertyDescription SLIDING_OPTIONS_DESCRIPTION = new PropertyDescription("printSliding", null, new SlidingoptionsPropertyController(
		"printSliding", RepositoryHelper.getDisplayName("printSliding", GraphicalComponent.class)));


	// null type: use property controller internally
	public static final PropertyDescription FORM_ENCAPSULATION_DESCRIPTION = new PropertyDescription("encapsulation", null,
		new EncapsulationPropertyController("encapsulation", RepositoryHelper.getDisplayName("encapsulation", Form.class)));
	public static final PropertyDescription RELATION_ENCAPSULATION_VALUES = new PropertyDescription(
		"encapsulation",
		PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(PersistEncapsulation.DEFAULT), Integer.valueOf(PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE), Integer.valueOf(PersistEncapsulation.MODULE_SCOPE) },
			new String[] { Messages.Public, Messages.HideInScriptingModuleScope, Messages.ModuleScope }));
	public static final PropertyDescription OTHER_ENCAPSULATION_VALUES = new PropertyDescription("encapsulation", PropertyType.values,
		new PropertyType.ValuesConfig().setValues(
			new Integer[] { Integer.valueOf(PersistEncapsulation.DEFAULT), Integer.valueOf(PersistEncapsulation.MODULE_SCOPE) },
			new String[] { Messages.Public, Messages.ModuleScope }));


	// null type: use property controller internally
	public static final PropertyDescription PAGE_FORMAT_DESCRIPTION = new PropertyDescription("defaultPageFormat", null,
		new PropertyController<String, PageFormat>("defaultPageFormat", RepositoryHelper.getDisplayName("defaultPageFormat", Form.class),
			new IPropertyConverter<String, PageFormat>()
			{
				public PageFormat convertProperty(Object id, String value)
				{
					return PersistHelper.createPageFormat(value);
				}

				public String convertValue(Object id, PageFormat value)
				{
					return PersistHelper.createPageFormatString(value);
				}

			}, PageFormatLabelProvider.INSTANCE, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new PageFormatEditor(parent, "Page Setup", PageFormatLabelProvider.INSTANCE);
				}
			}));


	// null type: use property controller internally
	public static final PropertyDescription GROUP_BY_DESCRIPTION = new PropertyDescription("groupbyDataProviderIDs", null,
		new PropertyController<String, Object[]>("groupbyDataProviderIDs", RepositoryHelper.getDisplayName("groupbyDataProviderIDs", Part.class),
			new StringTokenizerConverter(",", true), null, null));


	/**
	 * @param element
	 */
	public PersistPropertyHandler(java.beans.PropertyDescriptor propertyDescriptor)
	{
		super(propertyDescriptor);
	}

	@Override
	public PropertyDescription getPropertyDescription(Object obj, IPropertySource propertySource, final PersistContext persistContext)
	{
		final String name = propertyDescriptor.getName();
		String displayName = RepositoryHelper.getDisplayName(name, obj.getClass());

		PropertyCategory category = PropertyCategory.createPropertyCategory(name);
		if (category == PropertyCategory.Events || category == PropertyCategory.Commands)
		{
			return new PropertyDescription(name, PropertyType.function, Boolean.valueOf(category == PropertyCategory.Commands));
		}

		final Form form = persistContext.getContext() instanceof Form ? (Form)persistContext.getContext() : null;
		final FlattenedSolution flattenedEditingSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist(), persistContext.getContext());

		// name-based types
		if (name.equals("mediaOptions"))
		{
			// null type: use property controller internally
			return new PropertyDescription(name, null, new MediaoptionsPropertyController(name, displayName, propertySource));
		}

		if (name.endsWith("printSliding"))
		{
			return SLIDING_OPTIONS_DESCRIPTION;
		}


		if (obj instanceof Media && ("mimeType".equals(name) || "name".equals(name)))
		{
			// null type: use property controller internally
			return new PropertyDescription(name, null, new PropertyController<String, String>(name, displayName, PersistPropertySource.NULL_STRING_CONVERTER,
				null, null));
		}

		if ("tabSeq".equals(name))
		{
			// null type: use property controller internally
			return new PropertyDescription(name, null, new PropertyController<String, String>(name, displayName, null, null, new ICellEditorFactory()
			{
				public CellEditor createPropertyEditor(Composite parent)
				{
					if (form != null) return new TabSeqDialogCellEditor(parent, null, new TabSeqDialogValueEditor(form), true, SWT.NONE);
					else return new TabSeqDialogCellEditor(parent, null, new TabSeqDialogValueEditor(null), true, SWT.NONE);
				}
			}));
		}

		if (name.equals("encapsulation"))
		{
			if (persistContext.getPersist() instanceof Form)
			{
				// null type: use property controller internally
				return FORM_ENCAPSULATION_DESCRIPTION;
			}

			if (obj instanceof Relation)
			{
				return RELATION_ENCAPSULATION_VALUES;
			}

			return OTHER_ENCAPSULATION_VALUES;
		}

		if (name.equals(IContentSpecConstants.PROPERTY_STYLESHEET))
		{
			if (persistContext.getPersist() instanceof Solution)
			{
				return new PropertyDescription(name, PropertyType.string);
			}
		}

		if (name.equals("namedFoundSet"))
		{
			if (form != null)
			{
				NamedFoundsetComboboxModel model = new NamedFoundsetComboboxModel(form);
				// null type: use property controller internally
				return new PropertyDescription("namedFoundSet", null, new ComboboxPropertyController<String>(name, displayName, model,
					Messages.LabelUnresolved, new ComboboxDelegateValueEditor<String>(new NamedFoundsetRelationValueEditor(form), model)));
			}
		}

		if (name.endsWith("rowBGColorCalculation"))
		{
			if (form == null) return null;
			Table table = null;
			if (persistContext.getPersist() instanceof Portal)
			{
				Relation[] relations = flattenedEditingSolution.getRelationSequence(((Portal)persistContext.getPersist()).getRelationName());
				if (relations != null)
				{
					try
					{
						table = relations[relations.length - 1].getForeignTable();
					}
					catch (RepositoryException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			else
			{
				try
				{
					table = flattenedEditingSolution.getFlattenedForm(form).getTable();
				}
				catch (RepositoryException e)
				{
					ServoyLog.logInfo("Table form not accessible: " + e.getMessage());
				}
			}

			// null type: use property controller internally
			return new PropertyDescription(name, null, new ScriptProviderPropertyController(name, displayName, table, persistContext));
		}

		if ("variableType".equals(name) && persistContext.getPersist() instanceof ScriptVariable && propertySource instanceof PersistPropertySource)
		{
			int[] iTypes = Column.allDefinedTypes;
			Integer[] integerTypes = new Integer[iTypes.length];
			String[] stringTypes = new String[iTypes.length];
			for (int i = 0; i < iTypes.length; i++)
			{
				integerTypes[i] = Integer.valueOf(iTypes[i]);
				stringTypes[i] = Column.getDisplayTypeString(iTypes[i]);
			}


			// null type: use property controller internally
			return new PropertyDescription(name, null, new PropertySetterDelegatePropertyController<Integer, PersistPropertySource>(
				new ComboboxPropertyController<Integer>(name, displayName, new ComboboxPropertyModel<Integer>(integerTypes, stringTypes),
					Messages.LabelUnresolved), name)
			{
				// handle setting of this property via a IPropertySetter so that we can do additional stuff when the property is set.
				public void setProperty(PersistPropertySource propertySource, Integer value)
				{
					propertySource.setPersistPropertyValue(name, value);

					// the variable type is being set; the default value should change if incompatible
					ScriptVariable variable = (ScriptVariable)propertySource.getPersist();
					String defaultValue = variable.getDefaultValue();
					if (defaultValue != null)
					{
						String newDefault = null;
						int type = value.intValue();
						if (type == IColumnTypes.TEXT)
						{
							if (!isQuoted(defaultValue))
							{
								newDefault = Utils.makeJSExpression(defaultValue);
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
			});
		}


		if (name.equals("rotation"))
		{
			return ROTATION_VALUES;
		}

		if (name.equals("horizontalAlignment"))
		{
			return HORIZONTAL_ALIGNMENT_VALUES;
		}

		if (name.equals("solutionType"))
		{
			return SOLUTION_TYPE_VALUES;
		}

		if (name.equals("textOrientation"))
		{
			return TEXT_ORIENTATION_VALUES;
		}

		if (name.endsWith("navigatorID") && form != null)
		{
			final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, false),
				persistContext.getContext());
			PropertyDescriptor pd = new PropertyDescriptor(name, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					// TODO: use PropertyType.form with options
					boolean isMobile = form.getSolution().getSolutionMetaData().getSolutionType() == SolutionMetaData.MOBILE;
					return new ListSelectCellEditor(parent, "Select navigator form", new FormContentProvider(flattenedEditingSolution, form),
						formLabelProvider, new FormValueEditor(flattenedEditingSolution), false, new FormContentProvider.FormListOptions(
							FormListOptions.FormListType.FORMS, Boolean.valueOf(isMobile), true, !isMobile, true), SWT.NONE, null, "navigatorFormDialog");
				}
			};
			pd.setLabelProvider(formLabelProvider);

			// null type: use property controller internally
			return new PropertyDescription(name, null, pd);
		}


		if (name.equals("extendsID") && form != null)
		{
			final ILabelProvider formLabelProvider = new SolutionContextDelegateLabelProvider(new FormLabelProvider(flattenedEditingSolution, true),
				persistContext.getContext());
			PropertyDescriptor pd = new PropertyDescriptor(name, displayName)
			{
				@Override
				public CellEditor createPropertyEditor(Composite parent)
				{
					return new ListSelectCellEditor(parent, "Select parent form", new FormContentProvider(flattenedEditingSolution, form), formLabelProvider,
						new FormValueEditor(flattenedEditingSolution), false, new FormContentProvider.FormListOptions(FormListOptions.FormListType.HIERARCHY,
							null, true, false, false), SWT.NONE, null, "parentFormDialog")
					{
						@Override
						protected Object openDialogBox(Control cellEditorWindow)
						{
							Object returnValue = super.openDialogBox(cellEditorWindow);
							if (returnValue != null)
							{
								Form f = (Form)persistContext.getPersist();
								if (!Utils.equalObjects(Integer.valueOf(f.getExtendsID()), returnValue))
								{
									List<IPersist> overridePersists = new ArrayList<IPersist>();
									for (IPersist child : f.getAllObjectsAsList())
									{
										if (child instanceof ISupportExtendsID && PersistHelper.isOverrideElement((ISupportExtendsID)child)) overridePersists.add(child);
									}
									if (overridePersists.size() > 0)
									{
										if (MessageDialog.openConfirm(Display.getDefault().getActiveShell(), "Delete override elements",
											"You are about to change the parent form, this will also delete all override elements (this action cannot be undone). Are you sure you want to proceed?"))
										{
											for (IPersist child : overridePersists)
											{
												f.removeChild(child);
											}
										}
										else
										{
											returnValue = null;
										}
									}
									if (returnValue != null)
									{
										// here we decide what happens to the parts that are declared in this form; if new extended form
										// has those parts, mark them as overriden; if we wouldn't do this you could end up with 2 body parts in the same form: one overriden and one not�
										// an alternative would be to simply delete these parts
										ArrayList<Part> inheritedParts = new ArrayList<Part>();
										int newExtendsId = ((Integer)returnValue).intValue();
										if (newExtendsId != Form.NAVIGATOR_NONE && newExtendsId != Form.NAVIGATOR_DEFAULT) // NAVIGATOR_NONE (0 default value as in contentspec) should be used but in the past either one or the other was used
										{
											Form newSuperForm = flattenedEditingSolution.getForm(((Integer)returnValue).intValue());
											if (newSuperForm != null)
											{
												Iterator<Part> it = newSuperForm.getParts();
												while (it.hasNext())
													inheritedParts.add(it.next());
												List<Form> ancestorForms = flattenedEditingSolution.getFormHierarchy(newSuperForm);
												for (Form frm : ancestorForms)
												{
													it = frm.getParts();
													while (it.hasNext())
														inheritedParts.add(it.next());
												}
											}
										}

										Iterator<Part> it = f.getParts();
										while (it.hasNext())
										{
											Part p = it.next();
											Part ancestorP = null;
											// find first ancestor of the same part type for parts that cannot be duplicate in a form
											for (Part aP : inheritedParts)
											{
												if (aP.getPartType() == p.getPartType() && !Part.canBeMoved(p.getPartType()))
												{
													ancestorP = aP;
													break;
												}
											}
											if (ancestorP != null)
											{
												// remove part type as it is inherited
												Map<String, Object> pMap = p.getPropertiesMap();
												pMap.remove(StaticContentSpecLoader.PROPERTY_PARTTYPE.getPropertyName());
												p.copyPropertiesMap(pMap, true);

												p.setExtendsID(ancestorP.getID());
											}
										}
									}
								}
							}
							return returnValue;
						}
					};
				}
			};

			pd.setLabelProvider(formLabelProvider);

			// null type: use property controller internally
			return new PropertyDescription(name, null, pd);
		}


		if (name.equals("firstFormID") || name.equals("loginFormID"))
		{
			return new PropertyDescription(name, PropertyType.form);
		}

		if (name.equals("rolloverImageMediaID") || name.equals("imageMediaID"))
		{
			return new PropertyDescription(name, PropertyType.media);
		}

		if (name.equals("text") || name.equals("toolTipText") || name.equals("titleText") || name.equals("innerHTML"))
		{
			return new PropertyDescription(name, PropertyType.tagstring, Boolean.valueOf(name.equals("innerHTML")));
		}

		if (name.equals("styleClass"))
		{
			// null type: use property controller internally
			return new PropertyDescription(name, null, PersistPropertySource.createStyleClassPropertyController(persistContext.getPersist(), name, displayName,
				ModelUtils.getStyleLookupname(persistContext.getPersist()), form));
		}


		if (name.equals("styleName"))
		{
			List<String> styleNames = new ArrayList<String>();
			try
			{
				for (Style style : (List<Style>)ServoyModel.getDeveloperRepository().getActiveRootObjects(IRepository.STYLES))
				{
					styleNames.add(style.getName());
				}
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
			Collections.sort(styleNames);
			styleNames.add(0, null);

			ComboboxPropertyModel<String> model = new ComboboxPropertyModel<String>(styleNames.toArray(new String[styleNames.size()]),
				PersistPropertySource.NullDefaultLabelProvider.LABEL_DEFAULT);

			// null type: use property controller internally
			return new PropertyDescription(name, null, new ComboboxPropertyController<String>(name, displayName, model, Messages.LabelUnresolved,
				new ComboboxDelegateValueEditor<String>(StyleValueEditor.INSTANCE, model)));
		}

		if (name.endsWith("initialSort"))
		{
			final ITableDisplay tableDisplay;
			if (persistContext != null && persistContext.getPersist() instanceof Portal)
			{
				tableDisplay = new ITableDisplay()
				{
					public String getInitialSort()
					{
						return ((Portal)persistContext.getPersist()).getInitialSort();
					}

					public Table getTable() throws RepositoryException
					{
						Table table = null;
						Relation[] relations = flattenedEditingSolution.getRelationSequence(((Portal)persistContext.getPersist()).getRelationName());
						if (relations != null)
						{
							table = relations[relations.length - 1].getForeignTable();
						}
						return table;
					}
				};
			}
			else if (form != null)
			{
				tableDisplay = flattenedEditingSolution.getFlattenedForm(form);
			}
			else if (persistContext.getPersist() instanceof Relation)
			{
				tableDisplay = new ITableDisplay()
				{
					public String getInitialSort()
					{
						return ((Relation)persistContext.getPersist()).getInitialSort();
					}

					public Table getTable() throws RepositoryException
					{
						return ((Relation)persistContext.getPersist()).getForeignTable();
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

			// null type: use property controller internally
			return new PropertyDescription(name, null, new PropertyController<String, String>(name, displayName, PersistPropertySource.NULL_STRING_CONVERTER,
				NullDefaultLabelProvider.LABEL_DEFAULT, new ICellEditorFactory()
				{
					public CellEditor createPropertyEditor(Composite parent)
					{
						return new SortCellEditor(parent, flattenedEditingSolution, tableDisplay, "Select sorting fields",
							NullDefaultLabelProvider.LABEL_DEFAULT);
					}
				}));
		}


		if (name.equals("defaultPageFormat"))
		{
			return PAGE_FORMAT_DESCRIPTION;
		}

		if (name.equals("groupbyDataProviderIDs"))
		{
			return GROUP_BY_DESCRIPTION;
		}

		if (name.equals("borderType"))
		{
			return new PropertyDescription(name, PropertyType.border, Boolean.TRUE);
		}

		if (name.endsWith("dataProviderID"))
		{
			return new PropertyDescription(name, PropertyType.dataprovider);
		}

		if (name.equals("format"))
		{
			return new PropertyDescription(name, PropertyType.format, StaticContentSpecLoader.PROPERTY_DATAPROVIDERID.getPropertyName());
		}

		if (name.equals("relationName"))
		{
			return new PropertyDescription(name, PropertyType.relation);
		}

		if (name.equals("fontType"))
		{
			return new PropertyDescription(name, PropertyType.font, Boolean.TRUE);
		}

		if (name.endsWith("valuelistID"))
		{
			return new PropertyDescription(name, PropertyType.valuelist);
		}

		if ("rolloverCursor".equals(name))
		{
			return ROLLOVER_CURSOR_VALUES;
		}

		if (name.endsWith("shapeType"))
		{
			return SHAPE_TYPE_VALUES;
		}

		if (name.equals("view"))
		{
			return VIEW_TYPE_VALUES;
		}

		if ("selectionMode".equals(name))
		{
			return SELECTION_MODE_VALUES;
		}

		if (name.equals("joinType"))
		{
			return JOIN_TYPE_VALUES;
		}

		if (name.equals("displayType"))
		{
			return DISPLAY_TYPE_VALUES;
		}

		if (name.equals("tabOrientation"))
		{
			return TAB_ORIENTATION_VALUES;
		}

		if (name.equals("mnemonic"))
		{
			return MNEMONIC_VALUES;
		}

		if (name.equals("labelFor"))
		{
			if (form != null)
			{
				Form flattenedForm = flattenedEditingSolution.getFlattenedForm(form);
				int bodyStart = -1, bodyEnd = -1;
				if (flattenedForm.getView() == FormController.TABLE_VIEW || flattenedForm.getView() == FormController.LOCKED_TABLE_VIEW)
				{
					bodyStart = 0;
					Iterator<Part> parts = flattenedForm.getParts();
					while (parts.hasNext())
					{
						Part part = parts.next();
						if (part.getPartType() == Part.BODY)
						{
							bodyEnd = part.getHeight();
							break;
						}
						bodyStart = part.getHeight();
					}
				}

				List<String> names = new ArrayList<String>();
				for (IPersist object : flattenedForm.getAllObjectsAsList())
				{
					if (object instanceof IFormElement && ((IFormElement)object).getName() != null && ((IFormElement)object).getName().length() > 0)
					{
						boolean add = ((IFormElement)object).getTypeID() == IRepository.FIELDS;
						if (!add && bodyStart >= 0 && (!(object instanceof GraphicalComponent) || ((GraphicalComponent)object).getLabelFor() == null))
						{
							// TableView, add elements in the body
							Point location = ((IFormElement)object).getLocation();
							add = (location != null && location.y >= bodyStart && location.y < bodyEnd);
						}
						if (add)
						{
							names.add(((IFormElement)object).getName());
						}
					}
				}
				String[] array = names.toArray(new String[names.size()]);
				Arrays.sort(array, String.CASE_INSENSITIVE_ORDER);
				return new PropertyDescription(name, PropertyType.values, new PropertyType.ValuesConfig().setValues(array).setEditable(true));
			}
		}

		if (name.equals("modulesNames"))
		{
			List<String> availableSolutions = new ArrayList<String>();
			if (obj instanceof Solution)
			{
				String solName = ((Solution)obj).getName();
				RootObjectMetaData[] solutionMetaDatas;
				try
				{
					solutionMetaDatas = ServoyModel.getDeveloperRepository().getRootObjectMetaDatasForType(IRepository.SOLUTIONS);
					for (RootObjectMetaData element : solutionMetaDatas)
					{
						if (!element.getName().equals(solName))
						{
							availableSolutions.add(element.getName());
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

			return new PropertyDescription(name, PropertyType.values, new PropertyType.ValuesConfig().setValues(availableSolutions.toArray()).setMultiple(true));
		}

		return super.getPropertyDescription(obj, propertySource, persistContext);
	}

	private static boolean isQuoted(String text)
	{
		return (text.startsWith("'") && text.endsWith("'")) || (text.startsWith("\"") && text.endsWith("\""));
	}

	@Override
	public void setValue(Object obj, Object value, PersistContext persistContext)
	{
		if (propertyDescriptor.getName().equals("textOrientation") && !Utils.equalObjects(getValue(obj, persistContext), value))
		{
			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					MessageDialog.openInformation(Display.getDefault().getActiveShell(), "Orientation change",
						"Running clients need to be restarted to make text orientation changes effective");
				}
			});
		}

		super.setValue(obj, value, persistContext);
	}

}
