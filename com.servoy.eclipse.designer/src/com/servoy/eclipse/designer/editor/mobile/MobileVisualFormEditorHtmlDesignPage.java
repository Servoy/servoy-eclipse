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

package com.servoy.eclipse.designer.editor.mobile;

import static com.servoy.eclipse.core.ServoyModelManager.getServoyModelManager;

import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.StringTokenizer;

import org.eclipse.core.runtime.FileLocator;
import org.eclipse.core.runtime.Platform;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.gef.ui.actions.DeleteAction;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.SWTError;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.browser.BrowserFunction;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Listener;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.views.properties.IPropertySource;
import org.json.JSONArray;
import org.json.JSONException;
import org.osgi.framework.Bundle;

import com.servoy.base.persistence.IBaseComponent;
import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.IMobileProperties.MobileProperty;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.persistence.constants.IFormConstants;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.core.Activator;
import com.servoy.eclipse.core.resource.DesignPagetype;
import com.servoy.eclipse.designer.editor.AddPartsCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.VisualFormEditor;
import com.servoy.eclipse.designer.editor.commands.AbstractModelsCommand;
import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddBeanCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddFieldCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddInsetListCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.AddLabelCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.ApplyMobileFormElementOrderCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddButtonCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.MobileAddHeaderTitleCommand;
import com.servoy.eclipse.designer.editor.mobile.commands.PutCustomMobilePropertyCommand;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFooterGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileFormGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileHeaderGraphicalEditPart;
import com.servoy.eclipse.designer.editor.mobile.editparts.MobileSnapData.MobileSnapType;
import com.servoy.eclipse.designer.mobile.property.MobilePersistPropertySource;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.property.ComplexProperty;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.property.RetargetToEditorPersistProperties;
import com.servoy.eclipse.ui.util.SelectionProviderAdapter;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.debug.layout.MobileFormLayout;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Bean;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.ServoyJSONArray;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

/**
 * Design page for web based mobile form editor
 *
 * @author rgansevles
 *
 */
public class MobileVisualFormEditorHtmlDesignPage extends BaseVisualFormEditorDesignPage
{
	private static final String ID_KEY = "id";
	private static final String TYPE_KEY = "type";
	private static final String TEXT_KEY = "text";
	private static final String PROPERTIES_KEY = "properties";
	private static final String CHILDREN_KEY = "children";
	private static final String THEME_KEY = "theme";
	private static final String ZONE_KEY = "zone";
	private static final String LABEL_KEY = "label";

	private static final String EMPTY_VALUE = " "; // to display empty, with "" the default of a widget would be used

	private static final String CONTENT_PREFIX = "content_";
	private static final String FOOTER_PREFIX = "footer_";
	private static final String HEADER_PREFIX = "header_";

	private static final Map<String, Integer> DISPLAY_TYPE_MAPPING = new HashMap<String, Integer>();

	static
	{
		DISPLAY_TYPE_MAPPING.put("TextInput", Integer.valueOf(Field.TEXT_FIELD));
		DISPLAY_TYPE_MAPPING.put("TextArea", Integer.valueOf(Field.TEXT_AREA));
		DISPLAY_TYPE_MAPPING.put("Calendar", Integer.valueOf(Field.CALENDAR));
		DISPLAY_TYPE_MAPPING.put("PasswordField", Integer.valueOf(Field.PASSWORD));
		DISPLAY_TYPE_MAPPING.put("SelectMenu", Integer.valueOf(Field.COMBOBOX));
		DISPLAY_TYPE_MAPPING.put("CheckboxGroup", Integer.valueOf(Field.CHECKS));
		DISPLAY_TYPE_MAPPING.put("RadioGroup", Integer.valueOf(Field.RADIOS));
	}

	private static final String PALETTE_ITEMS_RECORDVIEW;
	private static final String PALETTE_ITEMS_LISTFORM;

	static
	{
		JSONArray items = new JSONArray(); // no newlines
		items.put("Header");
		items.put("Footer");
		items.put("Button");
		PALETTE_ITEMS_LISTFORM = items.toString();

		items.put("TextInput");
		items.put("PasswordField");
		items.put("TextArea");
		items.put("Calendar");
		items.put("Bean");
		items.put("SelectMenu");
		items.put("RadioGroup");
		items.put("SingleCheckbox");
		items.put("CheckboxGroup");
		items.put("InsetList");
		items.put("Label");
		PALETTE_ITEMS_RECORDVIEW = items.toString();
	}

	private final ISelectionProvider selectionProvider = new SelectionProviderAdapter();

	private MobileVisualFormEditorContextMenuProvider contextMenuProvider;
	private Browser browser;

	private volatile String lastFormDesign;
	private String lastPaletteItems;

	public MobileVisualFormEditorHtmlDesignPage(BaseVisualFormEditor editorPart)
	{
		super(editorPart);
	}

	@Override
	public DesignPagetype getDesignPagetype()
	{
		return DesignPagetype.Mobile;
	}

	@Override
	public void createPartControl(Composite parent)
	{
		try
		{
			browser = new Browser(parent, SWT.NONE);
		}
		catch (SWTError e)
		{
			ServoyLog.logError(e);
			return;
		}
		browser.addListener(SWT.KeyDown, new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				if (event.character == SWT.DEL && event.doit)
				{
					// DEL is filtered out in swt for Browser
					DeleteAction deleteAction = createDeleteAction();
					if (deleteAction != null && deleteAction.isEnabled())
					{
						ISelection selection = selectionProvider.getSelection();
						if (selection instanceof StructuredSelection && !selection.isEmpty())
						{
							getCommandStack().execute(deleteAction.createDeleteCommand(((StructuredSelection)selection).toList()));
						}
					}
				}
			}
		});

		// create context menu
		contextMenuProvider = new MobileVisualFormEditorContextMenuProvider("#MobileFormDesignerContext", getActionRegistry());
		getSite().registerContextMenu(contextMenuProvider.getId(), contextMenuProvider, selectionProvider);
		contextMenuProvider.createContextMenu(browser);
		Listener mouseListener = new Listener()
		{
			@Override
			public void handleEvent(Event event)
			{
				if (event.button == 3)
				{
					// right-click
					contextMenuProvider.getMenu().setVisible(true);
				}
			}
		};
		// for firefox use mouseup, for ie use mouse down
		if ("mozilla".equals(browser.getBrowserType()))
		{
			browser.addListener(SWT.MouseUp, mouseListener);
		}
		else
		{
			browser.addListener(SWT.MouseDown, mouseListener); //TODO: check why this does not work
		}

		new BrowserFunction(browser, "consoleLog")
		{
			@Override
			public Object function(Object[] arguments)
			{
				if (Debug.tracing())
				{
					Debug.trace("consoleLog: " + Arrays.toString(arguments));
				}
				return null;
			}
		};
		new BrowserFunction(browser, "getFormDesign")
		{
			@Override
			public Object function(Object[] arguments)
			{
				return callGetFormDesign();
			}
		};
		new BrowserFunction(browser, "getChildJson")
		{
			@Override
			public Object function(Object[] arguments)
			{
				return callGetChildJson(asString(arguments[0]));
			}
		};
		new BrowserFunction(browser, "callbackSelectionChanged")
		{
			@Override
			public Object function(Object[] arguments)
			{
				try
				{
					callbackSelectionChanged(asString(arguments[0]));
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				return null;
			}
		};
		new BrowserFunction(browser, "callbackEditingStateChanged")
		{
			@Override
			public Object function(Object[] arguments)
			{
				try
				{
					callbackEditingStateChanged(Boolean.TRUE.equals(arguments[0]));
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				return null;
			}
		};
		new BrowserFunction(browser, "callbackModelUpdated")
		{
			@Override
			public Object function(Object[] arguments)
			{
				try
				{
					return callbackModelUpdated(asString(arguments[0]), asString(arguments[1]), asString(arguments[2]), asString(arguments[3]),
						asInt(arguments[4]), asString(arguments[5]));
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				return null;
			}
		};
		new BrowserFunction(browser, "getPaletteItems")
		{
			@Override
			public Object function(Object[] arguments)
			{
				try
				{
					return getPaletteItems();
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
				return null;
			}
		};

		Bundle bundle = Platform.getBundle("com.servoy.eclipse.designer.rib");
		URL resourceUrl = bundle.getResource("/rib/index.html");
		try
		{
			browser.setUrl(FileLocator.toFileURL(resourceUrl).toURI().toString());
		}
		catch (Exception e)
		{
			ServoyLog.logError("couldn't load the editors html file: " + resourceUrl, e);
		}

		selectionProvider.addSelectionChangedListener(new ISelectionChangedListener()
		{
			@Override
			public void selectionChanged(SelectionChangedEvent event)
			{
				ISelection selection = event.getSelection();
				if (!selection.isEmpty() && selection instanceof IStructuredSelection)
				{
					selectNode(((IStructuredSelection)selection).getFirstElement());
				}
			}
		});

		openViewers();
	}

	private static String asString(Object s)
	{
		return s == null ? null : s.toString();
	}

	private static int asInt(Object i)
	{
		return i instanceof Number ? ((Number)i).intValue() : 0;
	}

	private static <T, E extends T> void addIfNonNull(List<T> list, E element)
	{
		if (element != null) list.add(element);
	}

	protected void callbackSelectionChanged(String properties) throws JSONException
	{
		Object element = findElement(new ServoyJSONObject(properties, false));
		selectionProvider.setSelection(element == null ? StructuredSelection.EMPTY : new StructuredSelection(element));
	}

	protected void callbackEditingStateChanged(boolean state)
	{
		if (state)
		{
			editorPart.deactivateEditorContext();
		}
		else
		{
			editorPart.activateEditorContext();
		}
	}

	private static UUID getUUID(String uuid)
	{
		if (uuid == null || uuid.length() == 0)
		{
			return null;
		}
		int index = uuid.lastIndexOf('_');
		String name = index < 0 ? uuid : uuid.substring(index + 1); // child element indexes are prefixed
		if (name.split("-").length != 5)
		{
			throw new RuntimeException("Form design was not loaded in form editor, receiving uuid '" + uuid + "'");
		}

		return UUID.fromString(name);
	}

	protected String callbackModelUpdated(String eventType, String nodeType, String parentUuid, String zoneName, final int zoneIndex, String properties)
		throws JSONException
	{
		String uuid = parentUuid;
		if (uuid == null)
		{
			return null;
		}

		IPersist pp = ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(uuid);
		if (pp instanceof GraphicalComponent && ((GraphicalComponent)pp).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
		{
			// header title, use header part
			pp = getHeaderPart((GraphicalComponent)pp);
		}

		if (pp == null)
		{
			return null;
		}
		final IPersist parent = pp;
		final Form form = (Form)parent.getAncestor(IRepository.FORMS);

		Object element = null;
		ServoyJSONObject json = properties == null ? null : new ServoyJSONObject(properties, false);
		List<Command> commands = new ArrayList<Command>();
		Object toFireChanged = null;
		if ("nodeAdded".equals(eventType))
		{
			if (json.has(ID_KEY))
			{
				// node was added in refresh
				return null;
			}

			if ("Button".equals(nodeType))
			{
				MobileSnapType snapType = MobileSnapType.ContentItem;
				if (parent instanceof Part)
				{
					if (PersistUtils.isHeaderPart(((Part)parent).getPartType()))
					{
						snapType = null;
						// check for existing header buttons
						for (IFormElement headerItem : MobileHeaderGraphicalEditPart.getHeaderModelChildren(Activator.getDefault().getDesignClient(),
							editorPart.getForm()))
						{
							if (headerItem instanceof AbstractBase)
							{
								if (((AbstractBase)headerItem).getCustomMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON.propertyName) != null)
								{
									if (snapType == null)
									{
										snapType = MobileSnapType.HeaderRightButton;
									}
									else
									{
										// already have 2 buttons
										return null;
									}
								}
								else if (((AbstractBase)headerItem).getCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName) != null)
								{
									if (snapType == null)
									{
										snapType = MobileSnapType.HeaderLeftButton;
									}
									else
									{
										// already have 2 buttons
										return null;
									}
								}
							}
						}
						if (snapType == null)
						{
							// default
							snapType = MobileSnapType.HeaderLeftButton;
						}
					}
					else if (PersistUtils.isFooterPart(((Part)parent).getPartType()))
					{
						snapType = MobileSnapType.FooterItem;
					}
				}
				commands.add(
					new MobileAddButtonCommand(Activator.getDefault().getDesignClient(), form, VisualFormEditor.REQ_PLACE_BUTTON, new Point(0, 0), snapType));
			}
			else if ("Label".equals(nodeType))
			{
				commands.add(new AddLabelCommand(Activator.getDefault().getDesignClient(), form, VisualFormEditor.REQ_PLACE_LABEL, null, null));
			}
			else if ("Header".equals(nodeType) || "Footer".equals(nodeType))
			{
				commands.add(new AddPartsCommand(form, new int[] { "Header".equals(nodeType) ? Part.TITLE_HEADER : Part.TITLE_FOOTER })
				{
					@Override
					protected Part createPart(int partTypeId) throws RepositoryException
					{
						Part part = super.createPart(partTypeId);
						part.setStyleClass("c"); // default theme
						return part;
					}
				});
				if ("Header".equals(nodeType))
				{
					// add header title so we can set header text property
					commands.add(
						new MobileAddHeaderTitleCommand(Activator.getDefault().getDesignClient(), form, VisualFormEditor.REQ_PLACE_HEADER_TITLE, null, null));
				}
			}
			else if ("InsetList".equals(nodeType))
			{
				commands.add(new AddInsetListCommand(Activator.getDefault().getDesignClient(), form, VisualFormEditor.REQ_PLACE_INSET_LIST, null));
			}
			else if ("Bean".equals(nodeType))
			{
				commands.add(new AddBeanCommand(Activator.getDefault().getDesignClient(), form, VisualFormEditor.REQ_PLACE_BEAN, null, null));
			}
			else
			{
				Integer fieldType;
				Map<String, Object> customProperties = null;
				if ("SingleCheckbox".equals(nodeType))
				{
					customProperties = new HashMap<String, Object>();
					customProperties.put(IMobileProperties.COMPONENT_SINGLE_CHECKBOX.propertyName, Boolean.TRUE);
					fieldType = Integer.valueOf(Field.CHECKS);
				}
				else
				{
					fieldType = DISPLAY_TYPE_MAPPING.get(nodeType);
				}
				if (fieldType != null)
				{
					Map<Object, Object> props = new HashMap<Object, Object>();
					props.put(SetValueCommand.REQUEST_PROPERTY_PREFIX + StaticContentSpecLoader.PROPERTY_DISPLAYTYPE.getPropertyName(),
						MobilePersistPropertySource.MOBILE_DISPLAY_TYPE_CONTROLLER.getConverter().convertProperty(
							StaticContentSpecLoader.PROPERTY_DISPLAYTYPE.getPropertyName(), fieldType));
					commands.add(
						new AddFieldCommand(Activator.getDefault().getDesignClient(), form, VisualFormEditor.REQ_PLACE_FIELD, props, null, customProperties));
				}
			}
		}
		else if ("nodeMoved".equals(eventType))
		{
			element = findElement(json);
			if (element == null) return null;

			boolean headerElement = parent instanceof Part && PersistUtils.isHeaderPart(((Part)parent).getPartType()) &&
				element instanceof GraphicalComponent && ComponentFactory.isButton((GraphicalComponent)element);
			if (headerElement)
			{
				// button moved to header
				addIfNonNull(commands, createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.HEADER_ITEM, Boolean.TRUE));
				boolean right = "right".equals(zoneName);
				addIfNonNull(commands,
					createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.HEADER_RIGHT_BUTTON, right ? Boolean.TRUE : null));
				addIfNonNull(commands,
					createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.HEADER_LEFT_BUTTON, right ? null : Boolean.TRUE));
			}
			else
			{
				// not in header
				if (element instanceof GraphicalComponent)
				{
					addIfNonNull(commands, createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.HEADER_ITEM, null));
					addIfNonNull(commands, createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.HEADER_RIGHT_BUTTON, null));
					addIfNonNull(commands, createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.HEADER_LEFT_BUTTON, null));
				}
			}

			if (parent instanceof Part && PersistUtils.isFooterPart(((Part)parent).getPartType()) && element instanceof GraphicalComponent)
			{
				// element moved to footer
				addIfNonNull(commands, createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.FOOTER_ITEM, Boolean.TRUE));
			}
			else
			{
				// not in footer
				if (element instanceof GraphicalComponent)
				{
					addIfNonNull(commands, createPutCustomMobilePropertyCommand((GraphicalComponent)element, IMobileProperties.FOOTER_ITEM, null));
				}
			}

			if (!headerElement)
			{
				// moved to correct place in content or footer
				List<Object> elements = new ArrayList<Object>(getElementsForParent(form, parent));
				// moved within zone, reorder elements
				elements.remove(element);
				elements.add(zoneIndex < 0 ? 0 : zoneIndex > elements.size() ? elements.size() : zoneIndex, element);
				addIfNonNull(commands, ApplyMobileFormElementOrderCommand.applyElementOrder(elements));
			}

			if (commands.size() > 0)
			{
				// fire when updated/restored(undo)
				toFireChanged = element;
			}
		}
		else if ("propertyChanged".equals(eventType))
		{
			element = findElement(json);
			if (element != null)
			{
				for (String key : Utils.iterate(json.keys()))
				{
					Object value = json.opt(key);
					if (value instanceof String && ((String)value).trim().length() > 0)
					{
						String prop = null;
						/* */if (TEXT_KEY.equals(key)) prop = key;
						else if (LABEL_KEY.equals(key)) prop = "title.text";

						if (prop != null)
						{
							addIfNonNull(commands, createSetValueCommand(element, prop, value));
						}
					}
				}
			}
		}
		else
		{
			ServoyLog.logWarning("Unexpected model-updated event type '" + eventType + "'", null);
		}

		if (commands.size() > 0)
		{
			final List<Object> createdModels = new ArrayList<Object>(commands.size());
			Command command = new CompoundCommand();
			for (Command cmd : commands)
			{
				if (cmd instanceof AbstractModelsCommand)
				{
					((CompoundCommand)command).add(new ApplyMobileFormElementOrderCommand((AbstractModelsCommand)cmd, zoneIndex, parent, form, createdModels));
				}
				else
				{
					((CompoundCommand)command).add(cmd);
				}
			}

			if (toFireChanged != null)
			{
				Object toFire = toFireChanged;
				command = new RefreshingCommand<>(command, () -> getServoyModelManager().getServoyModel().firePersistChanged(false, toFire, false));
			}

			getCommandStack().execute(command);

			// commands executed, get the generated id
			if (createdModels.size() > 0)
			{
				return getModelId(createdModels.get(0));
			}

			selectionProvider.setSelection(element == null ? StructuredSelection.EMPTY : new StructuredSelection(element));
		}

		return null;
	}

	private Object findElement(ServoyJSONObject json)
	{
		String servoyuuid = json.optString(ID_KEY);
		if (servoyuuid.length() > 0)
		{
			return getPersistModel(editorPart.getForm(), ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(servoyuuid));
		}
		return null;
	}

	public static List< ? > getElementsForParent(Form form, IPersist parent)
	{
		if (parent == form)
		{
			if (form.getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED)
			{
				return Arrays.asList(MobileListModel.create(form, form));
			}

			FlattenedSolution editingFlattenedSolution = ModelUtils.getEditingFlattenedSolution(form);
			return MobileFormGraphicalEditPart.getModelsForRecordView(editingFlattenedSolution, editingFlattenedSolution.getFlattenedForm(form));
		}

		if (parent instanceof Part && PersistUtils.isFooterPart(((Part)parent).getPartType()))
		{
			return new ArrayList<ISupportBounds>(MobileFooterGraphicalEditPart.getFooterModelChildren(Activator.getDefault().getDesignClient(), form));
		}

		// TODO
		return Collections.emptyList();
	}

	public static Command createSetValueCommand(Object element, String id, Object value)
	{
		IPropertySource propertySource = Platform.getAdapterManager().getAdapter(element, IPropertySource.class);
		if (propertySource == null)
		{
			return null;
		}

		Object oldValue = propertySource.getPropertyValue(id);
		if (oldValue instanceof ComplexProperty< ? > && !(value instanceof ComplexProperty< ? >))
		{
			oldValue = ((ComplexProperty< ? >)oldValue).getValue();
		}

		if (Utils.equalObjects(value, oldValue))
		{
			// nothing changed
			return null;
		}

		if (propertySource instanceof RetargetToEditorPersistProperties)
		{
			// Make sure the setter sets the value directly and does not use a delegate to a command
			propertySource = ((RetargetToEditorPersistProperties)propertySource).getDelegate();
		}
		return SetValueCommand.createSetvalueCommand(null, propertySource, id, value);
	}

	static <T> Command createPutCustomMobilePropertyCommand(final AbstractBase element, MobileProperty<T> property, T value)
	{
		if (Utils.equalObjects(value, element.getCustomMobileProperty(property.propertyName)))
		{
			return null;
		}

		return new RefreshingCommand<>(new PutCustomMobilePropertyCommand<T>(element, property, value),
			() -> getServoyModelManager().getServoyModel().firePersistChanged(false, element, false));
	}

	@Override
	public void init(IEditorSite site, IEditorInput input) throws PartInitException
	{
		super.init(site, input);
		site.setSelectionProvider(selectionProvider);
	}

	@Override
	public void refreshAllParts()
	{
		sendMessage("refreshForm");
	}

	/**
	 * @param string
	 */
	private void sendMessage(String message)
	{
		if (!browser.isDisposed())
		{
			browser.execute("$.servoy.handleMessage('" + message + "')");
		}
	}

	private String getPaletteItems()
	{
		return editorPart.getForm().getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED ? PALETTE_ITEMS_LISTFORM : PALETTE_ITEMS_RECORDVIEW;
	}

	public void setPaletteItems()
	{
		String paletteItems = getPaletteItems();
		if (!paletteItems.equals(lastPaletteItems))
		{
			sendMessage("setPaletteItems:" + paletteItems);
			lastPaletteItems = paletteItems;
		}
	}

	@Override
	public void refreshPersists(List<IPersist> persists, boolean fullRefresh)
	{
		setPaletteItems();
		try
		{
			if (lastFormDesign == null)
			{
				refreshAllParts();
			}
			else
			{
				String newFormDesign = getFormDesign();
				if (!lastFormDesign.equals(newFormDesign))
				{
					if (persists.size() == 1 && persists.get(0) instanceof Form)
					{
						// just the form is changed, refresh it completely
						refreshAllParts();
						return;
					}

					List<IPersist> sortedPersists = new ArrayList<IPersist>(persists);
					// sort so that forms are before parts are before elements
					Collections.sort(sortedPersists, new Comparator<IPersist>()
					{
						private int getValue(IPersist persist)
						{
							if (persist instanceof Form) return 1;
							if (persist instanceof Part) return 2;
							return 3;
						}

						@Override
						public int compare(IPersist o1, IPersist o2)
						{
							return getValue(o1) - getValue(o2);
						}
					});

					// check if elements existed before
					for (IPersist persist : sortedPersists)
					{
						if (persist instanceof Form)
						{
							// Skip form here, form properties are not used in rendering
							continue;
						}

						Object modelId = getModelId(getPersistModel(editorPart.getForm(), persist));
						String searchString = '"' + String.valueOf(modelId) + '"';
						boolean existedBefore = lastFormDesign.indexOf(searchString) >= 0;
						boolean existsNow = newFormDesign.indexOf(searchString) >= 0;
						if (existsNow)
						{
							ElementLocation childLocation = getChildLocation(persist);
							if (childLocation == null)
							{
								// cannot find where to add/update the child
								refreshAllParts();
								return;
							}

							if (existedBefore)
							{
								// was modified
								sendMessage("refreshNode:" + modelId + ':' + childLocation.parentId + ':' + childLocation.zone + ':' + childLocation.zoneIndex);
							}
							else
							{
								// was added
								sendMessage("addNode:" + modelId + ':' + childLocation.parentId + ':' + childLocation.zone + ':' + childLocation.zoneIndex);
							}
						}
						else if (existedBefore)
						{
							// was deleted
							sendMessage("deleteNode:" + modelId);
						}
						// else: ignore
					}
					lastFormDesign = newFormDesign;

				} // else nothing changed
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}

	/**
	 * @param persist
	 * @return
	 */
	private ElementLocation getChildLocation(IPersist persist)
	{
		String modelId = getModelId(persist);
		if (modelId == null)
		{
			return null;
		}

		int contextIndex = 0;
		for (Object child : MobileFormGraphicalEditPart.getFormModelChildren(editorPart.getForm()))
		{
			if (child instanceof Part)
			{
				if (modelId.equals(getModelId(child)))
				{
					// direct child of page
					String pageId = editorPart.getForm().getUUID().toString();
					if (PersistUtils.isHeaderPart(((Part)child).getPartType()))
					{
						return new ElementLocation(pageId, "top", 0);
					}
					if (PersistUtils.isFooterPart(((Part)child).getPartType()))
					{
						return new ElementLocation(pageId, "bottom", 0);
					}
					return null;
				}

				if (PersistUtils.isHeaderPart(((Part)child).getPartType()))
				{
					for (IFormElement headerChild : MobileHeaderGraphicalEditPart.getHeaderModelChildren(Activator.getDefault().getDesignClient(),
						editorPart.getForm()))
					{
						if (modelId.equals(getModelId(headerChild)))
						{
							// header element
							boolean right = headerChild instanceof AbstractBase &&
								((AbstractBase)headerChild).getCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName) != null;

							// use headertext for uuid
							UUID uuid;
							GraphicalComponent headerText = getHeaderText((Part)child);
							if (headerText != null)
							{
								uuid = headerText.getUUID();
							}
							else
							{
								uuid = ((Part)child).getUUID();
							}

							return new ElementLocation(HEADER_PREFIX + uuid.toString(), right ? "right" : "left", 0);
						}
					}
				}
				else if (PersistUtils.isFooterPart(((Part)child).getPartType()))
				{
					int footerIndex = 0;
					for (IFormElement footerChild : MobileFooterGraphicalEditPart.getFooterModelChildren(Activator.getDefault().getDesignClient(),
						editorPart.getForm()))
					{
						if (modelId.equals(getModelId(footerChild)))
						{
							// footer element
							return new ElementLocation(FOOTER_PREFIX + ((Part)child).getUUID().toString(), "default", footerIndex);
						}
						footerIndex++;
					}
				}
			}
			else
			{
				if (modelId.equals(getModelId(child)))
				{
					// content element
					return new ElementLocation(CONTENT_PREFIX + editorPart.getForm().getUUID().toString(), "default", contextIndex);
				}
				contextIndex++;
			}
		}

		// not found
		return null;
	}

	@Override
	public boolean showPersist(IPersist persist)
	{
		return selectNode(persist);
	}

	@Override
	public void selectionChanged(IWorkbenchPart part, ISelection selection)
	{
		super.selectionChanged(part, selection);
		if (!selection.isEmpty() && selection instanceof StructuredSelection)
		{
			Object first = ((StructuredSelection)selection).getFirstElement();
			Object model = Platform.getAdapterManager().getAdapter(first, IPersist.class);
			if (model == null)
			{
				model = Platform.getAdapterManager().getAdapter(first, MobileListModel.class);
			}
			if (model == null)
			{
				model = Platform.getAdapterManager().getAdapter(first, FormElementGroup.class);
			}
			if (model == null)
			{
				model = first;
			}
			selectNode(model);
		}
	}

	protected boolean selectNode(Object object)
	{
		String id = getModelId(object);
		if (id != null)
		{
			sendMessage("selectNode:" + id);
			return true;
		}
		return false;
	}

	@Override
	public void setFocus()
	{
	}

//	var design = {
//		type:'Design',
//		zone:null,
//		children:[
//		{
//			type:'Page',
//			properties:{id:formName},
//			children:[
//				{type:'Header',zone:'top'},
//				{type:'Content',zone:'content',children:[
//					{type:'Button',properties:{text:'knop123'}}]
//				},
//				{type:'Footer',zone:'bottom'}
//			]
//
//		}]
//	}

	private GraphicalComponent getHeaderText(Part headerPart)
	{
		if (PersistUtils.isHeaderPart(headerPart.getPartType()))
		{
			List<IFormElement> partModelChildren = MobileHeaderGraphicalEditPart.getHeaderModelChildren(Activator.getDefault().getDesignClient(),
				editorPart.getForm());

			for (IFormElement el : partModelChildren)
			{
				if (el instanceof GraphicalComponent && ((GraphicalComponent)el).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
				{
					return (GraphicalComponent)el;
				}
			}
		}

		return null;
	}

	private String callGetFormDesign()
	{
		lastFormDesign = null;
		try
		{
			lastFormDesign = getFormDesign();
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return lastFormDesign;
	}

	private String getFormDesign() throws JSONException, RepositoryException
	{
		ServoyJSONArray pages;
		ServoyJSONObject page;
		ServoyJSONObject properties;
		ServoyJSONArray elements;
		ServoyJSONObject element;
		ServoyJSONArray content = null;

		ServoyJSONObject design = new ServoyJSONObject(false, false);
		design.put(TYPE_KEY, "Design");
		design.put(CHILDREN_KEY, pages = new ServoyJSONArray());
		pages.put(page = new ServoyJSONObject(false, false));
		page.put(TYPE_KEY, "Page");
		page.put(PROPERTIES_KEY, properties = new ServoyJSONObject(false, false));
		properties.put(ID_KEY, editorPart.getForm().getUUID().toString());
		properties.put(THEME_KEY, "d"); // white background
		page.put(CHILDREN_KEY, elements = new ServoyJSONArray());

		for (Object child : MobileFormGraphicalEditPart.getFormModelChildren(editorPart.getForm()))
		{
			element = getChildJson(child);
			if (element != null)
			{
				if (child instanceof Part)
				{
					elements.put(element);

					List<IFormElement> partModelChildren = null;
					if (PersistUtils.isHeaderPart(((Part)child).getPartType()))
					{
						partModelChildren = MobileHeaderGraphicalEditPart.getHeaderModelChildren(Activator.getDefault().getDesignClient(),
							editorPart.getForm());

						GraphicalComponent headerText = getHeaderText((Part)child);
						// take out header title, this is not a separate element in this form editor
						if (headerText != null)
						{
							partModelChildren.remove(headerText);
						}
					}
					else if (PersistUtils.isFooterPart(((Part)child).getPartType()))
					{
						content = createContentIfNull(elements, content); // make sure there is always a content part to drop elements on
						partModelChildren = MobileFooterGraphicalEditPart.getFooterModelChildren(Activator.getDefault().getDesignClient(),
							editorPart.getForm());
					}

					if (partModelChildren != null && partModelChildren.size() > 0)
					{
						ServoyJSONArray partChildren = new ServoyJSONArray();
						element.put(CHILDREN_KEY, partChildren);
						for (IFormElement partItem : partModelChildren)
						{
							partChildren.put(getChildJson(partItem));
						}
					}
				}
				else
				{
					content = createContentIfNull(elements, content);
					content.put(element);
				}
			}
		}
		content = createContentIfNull(elements, content); // make sure there is always a content part to drop elements on

		return design.toString();
	}

	private ServoyJSONArray createContentIfNull(ServoyJSONArray elements, ServoyJSONArray cnt) throws JSONException
	{
		ServoyJSONArray content = cnt;
		if (content == null)
		{
			// Content zone for body elements
			ServoyJSONObject contentElement = new ServoyJSONObject(false, false);
			elements.put(contentElement);
			contentElement.put(TYPE_KEY, editorPart.getForm().getView() == IFormConstants.VIEW_TYPE_TABLE_LOCKED ? "ListFormContent" : "Content");
			contentElement.put(ZONE_KEY, "content");
			contentElement.put(CHILDREN_KEY, content = new ServoyJSONArray());
			ServoyJSONObject properties = new ServoyJSONObject(false, false);
			contentElement.put(PROPERTIES_KEY, properties);
			properties.put(ID_KEY, CONTENT_PREFIX + editorPart.getForm().getUUID().toString());
		}
		return content;
	}

	public static Object getModelObject(Form form, Object object)
	{
		if (object instanceof IPersist)
		{
			return getPersistModel(form, (IPersist)object);
		}

		return object;
	}

	private String getModelId(Object object)
	{
		String prefix = "";
		UUID uuid = null;

		Object model = getModelObject(editorPart.getForm(), object);

		if (model instanceof IPersist)
		{
			uuid = ((IPersist)model).getUUID();
		}

		if (model instanceof FormElementGroup)
		{
			Pair<BaseComponent, GraphicalComponent> compWithTitle = getComponentWithTitle((FormElementGroup)model);
			if (compWithTitle != null)
			{
				uuid = compWithTitle.getLeft().getUUID();
			}
		}
		else if (model instanceof Part && PersistUtils.isHeaderPart(((Part)model).getPartType()))
		{
			// use header title
			GraphicalComponent headerText = getHeaderText((Part)model);
			if (headerText != null)
			{
				uuid = headerText.getUUID();
			}
			prefix = HEADER_PREFIX;
		}
		else if (model instanceof GraphicalComponent && ((GraphicalComponent)model).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
		{
			prefix = HEADER_PREFIX;
		}
		else if (model instanceof Part && PersistUtils.isFooterPart(((Part)model).getPartType()))
		{
			prefix = FOOTER_PREFIX;
		}
		else if (model instanceof MobileListModel && ((MobileListModel)model).component instanceof Portal)
		{
			// inset list
			uuid = ((MobileListModel)model).component.getUUID();
		}
		else if (model instanceof MobileListModel && ((MobileListModel)model).component == null && ((MobileListModel)model).button != null)
		{
			uuid = ((MobileListModel)model).button.getUUID();
		}

		return uuid == null ? null : (prefix + uuid.toString());
	}

	private static Object getPersistModel(Form form, IPersist persist)
	{
		if (persist == null)
		{
			return null;
		}

		if (persist instanceof Portal && ((Portal)persist).isMobileInsetList())
		{
			// inset list
			return MobileListModel.create(form, (ISupportChilds)persist);
		}

		if (persist.getParent() instanceof Portal && ((Portal)persist.getParent()).isMobileInsetList())
		{
			// inset list
			return MobileListModel.create(form, persist.getParent());
		}

		if (persist instanceof AbstractBase && (((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.LIST_ITEM_IMAGE.propertyName) != null ||
			((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.LIST_ITEM_BUTTON.propertyName) != null ||
			((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.LIST_ITEM_SUBTEXT.propertyName) != null ||
			((AbstractBase)persist).getCustomMobileProperty(IMobileProperties.LIST_ITEM_COUNT.propertyName) != null))
		{
			// list form
			return MobileListModel.create(form, form);
		}

		if (persist instanceof IFormElement)
		{
			String groupid = ((IFormElement)persist).getGroupID();
			if (groupid != null)
			{
				FormElementGroup group = new FormElementGroup(groupid, ModelUtils.getEditingFlattenedSolution(form), form);
				if (group.getElements().hasNext())
				{
					return new FormElementGroup(groupid, ModelUtils.getEditingFlattenedSolution(form), form);
				} // else group was deleted
			}
		}
		return persist;
	}

	public static Part getHeaderPart(AbstractBase element)
	{
		// header title, use header part
		Form form = (Form)element.getAncestor(IRepository.FORMS);
		for (Part part : Utils.iterate(form.getParts()))
		{
			if (PersistUtils.isHeaderPart(part.getPartType()))
			{
				return part;
			}
		}
		return null;
	}

	public static Part getFooterPart(AbstractBase element)
	{
		// header title, use header part
		Form form = (Form)element.getAncestor(IRepository.FORMS);
		for (Part part : Utils.iterate(form.getParts()))
		{
			if (PersistUtils.isFooterPart(part.getPartType()))
			{
				return part;
			}
		}
		return null;
	}

	private String callGetChildJson(String uuid)
	{
		try
		{
			ServoyJSONObject json = getChildJson(
				getPersistModel(editorPart.getForm(), ModelUtils.getEditingFlattenedSolution(editorPart.getForm()).searchPersist(uuid)));
			if (json != null)
			{
				return json.toString();
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
		return null;
	}

	private ServoyJSONObject getChildJson(Object child) throws JSONException, RepositoryException
	{
		ServoyJSONObject element = new ServoyJSONObject(false, false);
		ServoyJSONObject properties = new ServoyJSONObject(false, false);
		element.put(PROPERTIES_KEY, properties);
		IPersist persist = null;
		String elementType = null;

		if (child instanceof IPersist)
		{
			persist = (IPersist)child;

			GraphicalComponent headerText = null;
			if (persist instanceof GraphicalComponent &&
				((GraphicalComponent)persist).getCustomMobileProperty(IMobileProperties.HEADER_TEXT.propertyName) != null)
			{
				headerText = (GraphicalComponent)persist;
				// header title, use header part
				persist = getHeaderPart(headerText);
			}

			if (persist instanceof Part)
			{
				int partType = ((Part)persist).getPartType();
				if (PersistUtils.isHeaderPart(partType))
				{
					elementType = "Header";
					element.put(ZONE_KEY, "top");
					if (headerText == null)
					{
						headerText = getHeaderText((Part)persist);
						if (headerText == null)
						{
							// make sure one always exists, needed to get all props shown for header
							headerText = MobileAddHeaderTitleCommand.createHeaderTitle(editorPart.getForm());
						}
					}
				}
				else if (PersistUtils.isFooterPart(partType))
				{
					elementType = "Footer";
					element.put(ZONE_KEY, "bottom");
				}
				else
				{
					return null;
				}

				properties.put("position", partType == Part.TITLE_HEADER || partType == Part.TITLE_FOOTER ? "fixed" : "default"); // sticky
				properties.put(THEME_KEY, ((Part)persist).getStyleClass());
				if (PersistUtils.isHeaderPart(partType))
				{
					properties.put(ID_KEY, HEADER_PREFIX + headerText.getUUID().toString()); // use headerText element for selection
					properties.put(TEXT_KEY, headerText.getDataProviderID() == null && headerText.getText() != null ? headerText.getText() : EMPTY_VALUE);
					properties.put("servoydataprovider", headerText.getDataProviderID() == null ? EMPTY_VALUE : headerText.getDataProviderID());
				}
				else
				{
					// Footer
					properties.put(TEXT_KEY, EMPTY_VALUE);
					properties.put(ID_KEY, FOOTER_PREFIX + persist.getUUID().toString());
				}
			}
			else
				// Components

				if (persist instanceof GraphicalComponent)
			{
				GraphicalComponent gc = (GraphicalComponent)persist;
				if (ComponentFactory.isButton(gc))
				{
					elementType = "Button";
					properties.put(TEXT_KEY, gc.getDataProviderID() == null && gc.getText() != null ? gc.getText() : EMPTY_VALUE);
					properties.put("icon", gc.getCustomMobileProperty(IMobileProperties.DATA_ICON.propertyName));

					if (gc.getCustomMobileProperty(IMobileProperties.HEADER_ITEM.propertyName) != null)
					{
						// header button
						boolean right = gc.getCustomMobileProperty(IMobileProperties.HEADER_RIGHT_BUTTON.propertyName) != null;
						element.put(ZONE_KEY, right ? "right" : "left");
						properties.put("right", Boolean.valueOf(right));
					}
				}
				else
				{
					// label
					elementType = "Label";
					setLabelProperties((GraphicalComponent)persist, properties);
				}
			}
				else if (persist instanceof Bean)
			{
				Bean bean = (Bean)persist;
				elementType = "Bean";
				properties.put("name", bean.getName());
			}
		}
		else if (child instanceof FormElementGroup)
		{
			// component with title
			Pair<BaseComponent, GraphicalComponent> compWithTitle = getComponentWithTitle((FormElementGroup)child);
			if (compWithTitle != null)
			{

				GraphicalComponent label = compWithTitle.getRight();
				if (label != null)
				{
					if (label.getDataProviderID() == null)
					{
						properties.put("servoytitledataprovider", EMPTY_VALUE);
						properties.put(LABEL_KEY, label.getText() != null ? label.getText() : EMPTY_VALUE);
					}
					else
					{
						properties.put("servoytitledataprovider", label.getDataProviderID());
						properties.put(LABEL_KEY, EMPTY_VALUE);
					}
					if (compWithTitle.getLeft() instanceof GraphicalComponent && !ComponentFactory.isButton((GraphicalComponent)compWithTitle.getLeft()))
					{
						properties.put("titlevisible", label.getVisible() ? "visibleElement" : "notVisibleElement");
					}
				}

				persist = compWithTitle.getLeft();
				if (persist instanceof Field)
				{
					Field field = (Field)persist;

					for (Entry<String, Integer> mapping : DISPLAY_TYPE_MAPPING.entrySet())
					{
						if (mapping.getValue().intValue() == field.getDisplayType())
						{
							elementType = mapping.getKey();
							break;
						}
					}

					if (elementType == null)
					{
						return null;
					}

					boolean radios = false;
					switch (field.getDisplayType())
					{
						case Field.COMBOBOX :
						{
							ServoyJSONObject comboOptions = new ServoyJSONObject(false, false);
							comboOptions.put(CHILDREN_KEY, new ServoyJSONArray());
							properties.put("options", comboOptions);
							break;
						}

						case Field.RADIOS :
							radios = true;
							properties.put("orientation",
								IMobileProperties.RADIO_STYLE_HORIZONTAL.equals(field.getCustomMobileProperty(IMobileProperties.RADIO_STYLE.propertyName))
									? "horizontal" : "vertical");
							//$FALL-THROUGH$

						case Field.CHECKS :
						{
							if (field.getCustomMobileProperty(IMobileProperties.COMPONENT_SINGLE_CHECKBOX.propertyName) == Boolean.TRUE && !radios)
							{
								elementType = "SingleCheckbox"; // special case
								properties.put("checked", "checked");
							}
							else
							{
								ValueList valuelist = Activator.getDefault().getDesignClient().getFlattenedSolution().getValueList(field.getValuelistID());

								String customValues = null;
								if (valuelist != null && valuelist.getValueListType() == IValueListConstants.CUSTOM_VALUES)
								{
									customValues = valuelist.getCustomValues();
								}

								if (customValues == null || customValues.trim().length() == 0)
								{
									customValues = "One\nTwo\nThree";
								}

								ServoyJSONArray children = new ServoyJSONArray();
								element.put(CHILDREN_KEY, children);

								StringTokenizer tk = new StringTokenizer(customValues, "\r\n");
								for (int i = 1; tk.hasMoreTokens(); i++)
								{
									String line = tk.nextToken();
									String[] str = Utils.stringSplit(line, '|', '\\');

									ServoyJSONObject check = new ServoyJSONObject(false, false);
									children.put(check);
									check.put(TYPE_KEY, radios ? "RadioButton" : "Checkbox");
									ServoyJSONObject checkProperties = new ServoyJSONObject(false, false);
									check.put(PROPERTIES_KEY, checkProperties);
									checkProperties.put(ID_KEY, "child" + i + '_' + field.getUUID().toString());
									checkProperties.put("labelItem", str[0]);
									if (i == 1)
									{
										checkProperties.put("checked", "checked");
										checkProperties.put("servoydataprovider",
											field.getDataProviderID() == null ? EMPTY_VALUE : ((ISupportDataProviderID)persist).getDataProviderID());
									}

									checkProperties.put(THEME_KEY, field.getStyleClass());
								}
							}

							break;
						}
					}
				}
				else if (persist instanceof GraphicalComponent && !ComponentFactory.isButton((GraphicalComponent)persist))
				{
					elementType = "Label";
					setLabelProperties((GraphicalComponent)persist, properties);
				}
				if (persist instanceof BaseComponent)
				{
					properties.put("visibleelement", ((BaseComponent)persist).getVisible() ? "visibleElement" : "notVisibleElement");
				}
			}
		}
		else if (child instanceof MobileListModel)
		{
			MobileListModel model = (MobileListModel)child;
			if (model.component == null)
			{
				// form list
				elementType = "FormList";
				persist = model.button; // Use button for list form
			}
			else
			{
				// inset list
				elementType = "InsetList";
				persist = model.component; // Portal

				properties.put("headertext", model.header.getDataProviderID() == null && model.header.getText() != null ? model.header.getText() : EMPTY_VALUE);
				properties.put("servoytitledataprovider", model.header.getDataProviderID() == null ? EMPTY_VALUE : model.header.getDataProviderID());
				properties.put("headertheme", model.header.getStyleClass());
				properties.put("buttontheme", model.button.getStyleClass());
				properties.put("servoydataprovider", model.button.getDataProviderID() == null ? EMPTY_VALUE : model.button.getDataProviderID());
			}

			properties.put(TEXT_KEY, model.button.getDataProviderID() == null && model.button.getText() != null ? model.button.getText() : EMPTY_VALUE);
			properties.put("icon", model.button.getCustomMobileProperty(IMobileProperties.DATA_ICON.propertyName));
			properties.put("countbubble", model.countBubble.getDataProviderID() == null ? EMPTY_VALUE : "10");
			properties.put("subtext", model.subtext.getDataProviderID() == null && model.subtext.getText() != null ? model.subtext.getText() : EMPTY_VALUE);
			properties.put("servoysubtextdataprovider", model.subtext.getDataProviderID() == null ? EMPTY_VALUE : model.subtext.getDataProviderID());
		}

		if (persist == null || elementType == null)
		{
			// TODO other
			return null;
		}

		element.put(TYPE_KEY, elementType);
		if (!properties.has(ID_KEY)) properties.put(ID_KEY, persist.getUUID().toString());

		if (persist instanceof IBaseComponent)
		{
			properties.put(THEME_KEY, ((IBaseComponent)persist).getStyleClass());
		}
		if (persist instanceof ISupportDataProviderID)
		{
			properties.put("servoydataprovider",
				((ISupportDataProviderID)persist).getDataProviderID() == null ? EMPTY_VALUE : ((ISupportDataProviderID)persist).getDataProviderID());
		}

		return element;
	}

	private static void setLabelProperties(GraphicalComponent labelComp, ServoyJSONObject properties) throws JSONException
	{
		properties.put(TEXT_KEY, labelComp.getDataProviderID() == null && labelComp.getText() != null ? labelComp.getText() : EMPTY_VALUE);
		Object headerSizeProp = labelComp.getCustomMobileProperty(IMobileProperties.HEADER_SIZE.propertyName);
		if (headerSizeProp != null)
		{
			properties.put("labelsize", headerSizeProp);
		}
	}

	private Pair<BaseComponent, GraphicalComponent> getComponentWithTitle(FormElementGroup group)
	{
		List<IFormElement> formElements = MobileFormLayout.getGroupElements(group);
		if (formElements.size() == 1 && formElements.get(0) instanceof BaseComponent)
		{
			// no label
			return new Pair<BaseComponent, GraphicalComponent>((BaseComponent)formElements.get(0), null);
		}

		if (formElements.size() > 1 && formElements.get(0) instanceof GraphicalComponent && formElements.get(1) instanceof BaseComponent)
		{
			// label first
			return new Pair<BaseComponent, GraphicalComponent>((BaseComponent)formElements.get(1), (GraphicalComponent)formElements.get(0));
		}

		return null;
	}

	@Override
	protected DeleteAction createDeleteAction()
	{
		return new com.servoy.eclipse.designer.editor.html.actions.DeleteAction(getEditorPart())
		{
			@Override
			public boolean isEnabled()
			{
				// Do not delete when user is in editing mode
				return editorPart.isDesignerContextActive() && super.isEnabled();
			}
		};
	}

	@Override
	protected IAction createCopyAction()
	{
		return new com.servoy.eclipse.designer.editor.html.actions.CopyAction(editorPart);
	}

	@Override
	protected IAction createCutAction()
	{
		return new com.servoy.eclipse.designer.editor.html.actions.CutAction(editorPart);
	}

	@Override
	protected IAction createPasteAction()
	{
		return new com.servoy.eclipse.designer.editor.html.actions.PasteAction(Activator.getDefault().getDesignClient(), selectionProvider, editorPart);
	}

	private static class ElementLocation
	{
		public final String parentId;
		public final String zone;
		public final int zoneIndex;

		public ElementLocation(String parentId, String zone, int zoneIndex)
		{
			this.parentId = parentId;
			this.zone = zone;
			this.zoneIndex = zoneIndex;
		}
	}
}