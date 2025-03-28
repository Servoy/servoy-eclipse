/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.jface.dialogs.IInputValidator;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.TraverseEvent;
import org.eclipse.swt.events.TraverseListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.views.properties.IPropertyDescriptor;
import org.eclipse.ui.views.properties.IPropertySource;
import org.eclipse.ui.views.properties.TextPropertyDescriptor;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;

import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.editors.TextDialogCellEditor;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.server.ngclient.property.types.ClientFunctionPropertyType;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Property controller for key(string)/values in a map, entries are shown as subproperties.
 *
 * @author rgansevles
 *
 * @since 6.1
 */
public class MapEntriesPropertyController extends PropertyController<Map<String, Object>, Object>
{
	private final Map<String, PropertyDescription> attributesMap;
	public static LabelProvider MAP_LABEL_PROVIDER = new LabelProvider()
	{
		@Override
		public String getText(Object element)
		{
			if (element instanceof Map map)
			{
				return map.keySet().toString();
			}
			return "click to add";
		}
	};
	private final boolean addDialogCellEditor;
	private final JSONObject valueTypes;

	public MapEntriesPropertyController(Object id, String displayName, Map<String, PropertyDescription> map, boolean addDialogCellEditor)
	{
		this(id, displayName, map, addDialogCellEditor, null);
	}

	public MapEntriesPropertyController(Object id, String displayName, Map<String, PropertyDescription> map, boolean addDialogCellEditor,
		JSONObject valueTypes)
	{
		super(id, displayName);
		setLabelProvider(MAP_LABEL_PROVIDER);
		this.attributesMap = map;
		this.addDialogCellEditor = addDialogCellEditor;
		this.valueTypes = valueTypes;
	}

	@Override
	public CellEditor createPropertyEditor(Composite parent)
	{
		if (attributesMap != null && attributesMap.size() > 0)
		{
			return new AddEntryComboCellEditor(parent);
		}
		return new AddEntryCellEditor(parent);
	}

	@Override
	protected IPropertyConverter<Map<String, Object>, Object> createConverter()
	{
		return new ComplexProperty.ComplexPropertyConverter<Map<String, Object>>()
		{
			@Override
			public Object convertProperty(final Object id, Map<String, Object> value)
			{
				return new ComplexProperty<Map<String, Object>>(value)
				{
					@Override
					public IPropertySource getPropertySource()
					{
						return new MapPropertySource(this);
					}
				};
			}
		};
	}

	protected class MapPropertySource extends ComplexPropertySourceWithStandardReset<Map<String, Object>>
	{
		protected static final String REMOVE_VALUE = "<removed>&*^&^%&$#@^%$&%%^#$*$($";

		public MapPropertySource(ComplexProperty<Map<String, Object>> complexProperty)
		{
			super(complexProperty);
		}

		@Override
		public IPropertyDescriptor[] createPropertyDescriptors()
		{
			final Map<String, Object> map = getEditableValue();
			if (map == null)
			{
				return IPropertyController.NO_DESCRIPTORS;
			}

			TextPropertyDescriptor[] descs = new TextPropertyDescriptor[map.size()];
			int i = 0;
			for (final String key : map.keySet())
			{
				descs[i] = new TextPropertyDescriptor(key, key)
				{
					@Override
					public CellEditor createPropertyEditor(Composite parent)
					{

						if (attributesMap != null && attributesMap.size() > 0 && attributesMap.containsKey(key) &&
							!attributesMap.get(key).getValues().isEmpty())
						{
							List<Object> values = attributesMap.get(key).getValues();
							return new StringListWithContentProposalsPropertyController.AbstractWordsWithContentProposalCellEditor(parent,
								values.toArray(new String[values.size()]), null)
							{

								@Override
								protected Object doGetValue()
								{
									return text.getText();
								}

								@Override
								protected void doSetValue(Object val)
								{
									text.setText(val instanceof String ? (String)val : "");
								}

								@Override
								protected void addButton(Composite composite)
								{
									button = new Button(composite, SWT.FLAT);
									button.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
									button.addSelectionListener(new SelectionAdapter()
									{
										@Override
										public void widgetSelected(SelectionEvent e)
										{
											markDirty();
											doSetValue(REMOVE_VALUE);
											fireApplyEditorValue();
										}
									});
									button.setEnabled(true);
								}

								@Override
								protected void addListeners()
								{
								}
							};
						}
						else
						{
							return new TextDialogCellEditor(parent, SWT.NONE, new LabelProvider())
							{
								@Override
								protected Button createButton(Composite composite)
								{
									Button button = new Button(composite, SWT.FLAT);
									button.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_ETOOL_DELETE));
									return button;
								}

								@Override
								public Object openDialogBox(Control cellEditorWindow)
								{
									// button is hit
									return REMOVE_VALUE;
								}
							};
						}
					}
				};

				ILabelProvider labelProvider = createLabelProvider(key);
				if (labelProvider != null)
				{
					descs[i].setLabelProvider(labelProvider);
				}
				i++;
			}

			return descs;
		}

		protected ILabelProvider createLabelProvider(@SuppressWarnings("unused") String mapKey)
		{
			return null;
		}

		@Override
		public Object getPropertyValue(Object id)
		{
			Map<String, Object> map = getEditableValue();
			if (map == null)
			{
				return null;
			}
			return map.get(id);
		}

		@Override
		protected Map<String, Object> setComplexPropertyValue(Object id, Object v)
		{
			Map<String, Object> map = getEditableValue();
			if (REMOVE_VALUE.equals(v))
			{
				if (map != null)
				{
					map.remove(id);
				}
			}
			else
			{
				if (map == null)
				{
					map = new HashMap<String, Object>();
				}
				String value;
				if (v instanceof String && valueTypes != null && ClientFunctionPropertyType.CLIENT_FUNCTION_TYPE_NAME.equals(valueTypes.optString((String)id)))
				{
					value = v.toString();
				}
				else
				{
					value = toJSExpression(v);
				}
				map.put((String)id, value);
			}
			return map.size() == 0 ? null : map;
		}

		/**
		 * @param v
		 * @return
		 */
		protected String toJSExpression(Object v)
		{
			String value;
			if (v instanceof String && ((String)v).length() > 0)
			{
				Object parsed = Utils.parseJSExpression(v);
				if (parsed == null)
				{
					// not a bool, number or string, convert to quoted string
					value = Utils.makeJSExpression(v);
				}
				else
				{
					value = (String)v;
				}
			}
			else
			{
				value = null;
			}
			return value;
		}
	}

	protected abstract class PersistMapPropertySourceWithOverrideSupport extends MapPropertySource
	{
		protected final IPersist p;

		/**
		 * @param complexProperty
		 */
		public PersistMapPropertySourceWithOverrideSupport(IPersist p, ComplexProperty<Map<String, Object>> complexProperty)
		{
			super(complexProperty);
			this.p = p;
		}

		public boolean isOverriden(String mapKey)
		{
			if (PersistHelper.isOverrideElement(p))
			{
				Map<String, ? > attributes = getPersistMap(p);
				if (attributes != null && attributes.containsKey(mapKey))
				{
					List<AbstractBase> pOverrideHierarchy = PersistHelper.getOverrideHierarchy((ISupportExtendsID)p);
					if (pOverrideHierarchy.size() > 1)
					{
						for (int i = 1; i < pOverrideHierarchy.size(); i++)
						{
							attributes = getPersistMap(pOverrideHierarchy.get(i));
							if (attributes != null && attributes.containsKey(mapKey))
							{
								return true;
							}
						}
					}

				}
			}
			return false;
		}

		@Override
		public boolean isPropertySet(Object id)
		{
			Map<String, ? > persistMap = getPersistMap(p);
			return persistMap != null && persistMap.containsKey(id);
		}

		public abstract Map<String, ? > getPersistMap(IPersist persist);

		@Override
		protected ILabelProvider createLabelProvider(final String mapKey)
		{
			return new LabelProvider()
			{
				@Override
				public String getText(Object element)
				{
					String labelText = (String)element;
					if (isOverriden(mapKey))
					{
						return Messages.labelOverride(labelText);
					}
					return labelText;
				}
			};
		}

		@Override
		protected Map<String, Object> setComplexPropertyValue(Object cpid, Object cpv)
		{
			return super.setComplexPropertyValue(cpid, cpv == null && isOverriden((String)cpid) ? REMOVE_VALUE : cpv);
		}

	}

	protected abstract class AbstractAddEntryCellEditor extends CellEditor
	{
		private Map<String, Object> value;
		protected Button button;

		public AbstractAddEntryCellEditor(Composite parent)
		{
			super(parent);
		}

		protected Pair<String, String> parseText(String txt)
		{
			if (txt == null)
			{
				return null;
			}

			String key;
			String val;
			int index = txt.indexOf('=');
			if (index > 0)
			{
				key = txt.substring(0, index).trim();
				String str = txt.substring(index + 1).trim();
				if (str.length() == 0)
				{
					val = null;
				}
				else
				{
					Object parsed = Utils.parseJSExpression(str);
					if (parsed == null)
					{
						// not a bool, number or string, convert to quoted string
						val = Utils.makeJSExpression(str);
					}
					else
					{
						val = str;
					}
				}
			}
			else
			{
				key = txt.trim();
				val = null;
			}
			return new Pair<String, String>(key, val);
		}

		protected boolean allowNewKey(String txt)
		{
			return txt != null && txt.length() > 0 && (value == null || !value.containsKey(parseText(txt).getLeft()));
		}

		protected void addEntry(String txt)
		{
			Pair<String, String> keyval = parseText(txt);
			// create a copy-map so that value is seen as modified
			value = value == null ? new HashMap<String, Object>() : new HashMap<String, Object>(value);
			value.put(keyval.getLeft(), keyval.getRight() == null ? "" : keyval.getRight());
			setInputText("");
			markDirty();
			fireApplyEditorValue();
		}

		@Override
		protected Control createControl(Composite parent)
		{
			Composite composite = new Composite(parent, SWT.NONE);
			addInput(composite);

			button = new Button(composite, SWT.PUSH);
			button.setImage(PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJ_ADD));
			addSelectionListener();
			button.setEnabled(false);

			Button openDialogButton = null;
			if (addDialogCellEditor)
			{
				openDialogButton = new Button(composite, SWT.DOWN);
				openDialogButton.setText("...");
				openDialogButton.addSelectionListener(new SelectionAdapter()
				{
					@Override
					public void widgetSelected(SelectionEvent event)
					{
						Shell shell = new Shell(parent.getDisplay());
						InputDialog dialog = new InputDialog(shell, "Edit JSON",
							"JSON Editor", new JSONObject(value).toString(3), new IInputValidator()
							{

								@Override
								public String isValid(String newText)
								{
									try
									{
										new JSONObject(newText);
									}
									catch (Exception ex)
									{
										return "Invalid json, cannot be parsed!";
									}
									return null;
								}

							})
						{

							@Override
							protected int getInputTextStyle()
							{
								return SWT.MULTI | SWT.BORDER | SWT.H_SCROLL | SWT.V_SCROLL;
							}

							@Override
							protected Control createDialogArea(Composite parent)
							{
								Control result = super.createDialogArea(parent);

								Text text = getText(); // The input text

								GridData data = new GridData(SWT.FILL, SWT.TOP, true, false);
								data.heightHint = convertHeightInCharsToPixels(30); // number of rows
								text.setLayoutData(data);

								return result;
							}

						};
						if (dialog.open() != Window.OK)
							return;
						JSONObject json = new JSONObject(dialog.getValue());
						value = new HashMap<String, Object>();
						for (String key : json.keySet())
						{
							value.put(key, json.get(key));
						}
						markDirty();
						fireApplyEditorValue();
					}
				});
			}

			// layout
			GroupLayout groupLayout = new GroupLayout(composite);
			SequentialGroup sequentialGroup = groupLayout.createSequentialGroup();
			sequentialGroup.add(getInput(), GroupLayout.PREFERRED_SIZE, 135, Integer.MAX_VALUE);
			sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(button);
			if (openDialogButton != null)
			{
				sequentialGroup.addPreferredGap(LayoutStyle.RELATED).add(openDialogButton);
			}
			groupLayout.setHorizontalGroup(sequentialGroup);

			ParallelGroup parallelGroup = groupLayout.createParallelGroup(GroupLayout.CENTER, false);
			if (openDialogButton != null)
			{
				parallelGroup.add(openDialogButton, 0, 0, Integer.MAX_VALUE);
			}
			parallelGroup.add(button, 0, 0, Integer.MAX_VALUE);
			parallelGroup.add(getInput(), GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			groupLayout.setVerticalGroup(parallelGroup);

			composite.setLayout(groupLayout);

			return composite;
		}

		protected void addSelectionListener()
		{
			button.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					addEntry(getInputText());
				}
			});
		}

		@Override
		protected Map<String, Object> doGetValue()
		{
			return value;
		}

		@Override
		protected void doSetValue(Object val)
		{
			this.value = val instanceof Map< ? , ? > ? (Map<String, Object>)val : null;
		}

		@Override
		protected void doSetFocus()
		{
			getInput().forceFocus();
		}

		protected abstract Control getInput();

		protected abstract void addInput(Composite composite);

		protected abstract void setInputText(String string);

		protected abstract String getInputText();

	}

	protected class AddEntryCellEditor extends AbstractAddEntryCellEditor
	{

		private Text text;

		public AddEntryCellEditor(Composite parent)
		{
			super(parent);
		}

		@Override
		protected void setInputText(String string)
		{
			text.setText(string);
		}

		@Override
		protected Control getInput()
		{
			return text;
		}

		@Override
		protected String getInputText()
		{
			return text.getText();
		}

		@Override
		protected void addInput(Composite composite)
		{
			text = new Text(composite, SWT.BORDER);
			text.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					button.setEnabled(allowNewKey(text.getText()));
				}
			});
			text.addTraverseListener(new TraverseListener()
			{
				public void keyTraversed(TraverseEvent e)
				{
					if (e.detail == SWT.TRAVERSE_RETURN && allowNewKey(text.getText()))
					{
						e.doit = false;
						addEntry(text.getText());
					}
				}
			});
		}

	}

	protected class AddEntryComboCellEditor extends AbstractAddEntryCellEditor
	{
		private Combo combo;

		public AddEntryComboCellEditor(Composite parent)
		{
			super(parent);
		}

		@Override
		protected Control getInput()
		{
			return combo;
		}

		@Override
		protected String getInputText()
		{
			return combo.getText();
		}

		@Override
		protected void setInputText(String string)
		{
			combo.setText(string);
		}

		@Override
		protected void addInput(Composite composite)
		{
			combo = new Combo(composite, SWT.BORDER);
			combo.setItems(attributesMap.keySet().toArray(new String[attributesMap.size()]));
			combo.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					button.setEnabled(allowNewKey(combo.getText()));
				}
			});
			combo.addTraverseListener(new TraverseListener()
			{
				public void keyTraversed(TraverseEvent e)
				{
					if (e.detail == SWT.TRAVERSE_RETURN && allowNewKey(combo.getText()))
					{
						e.doit = false;
						addEntry(combo.getText());
					}
				}
			});
		}
	}

}
