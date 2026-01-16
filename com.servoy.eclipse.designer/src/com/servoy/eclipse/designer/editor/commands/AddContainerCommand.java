package com.servoy.eclipse.designer.editor.commands;

import static java.util.Arrays.asList;
import static org.eclipse.ui.PlatformUI.getWorkbench;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.IHandler;
import org.eclipse.jface.viewers.ISelectionProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ElementListSelectionDialog;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.ICustomType;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.elements.ElementFactory;
import com.servoy.eclipse.core.util.PersistFinder;
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.RfbVisualFormEditorDesignPage;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.CreateComponentHandler;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.util.ElementUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractContainer;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportBounds;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.ISupportsIndexedChildren;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ServoyJSONObject;
import com.servoy.j2db.util.Utils;


public class AddContainerCommand extends AbstractHandler implements IHandler
{
	public static final String COMMAND_ID = "com.servoy.eclipse.designer.rfb.add";
	private final AtomicInteger id = new AtomicInteger();

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException
	{
		try
		{
			final BaseVisualFormEditor activeEditor = DesignerUtil.getActiveEditor();
			if (activeEditor != null)
			{
				final ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
				PersistContext persistCtxt = null;
				if (DesignerUtil.getContentOutlineSelection() != null)
				{
					persistCtxt = DesignerUtil.getContentOutlineSelection();
				}
				else
				{
					IStructuredSelection sel = (IStructuredSelection)selectionProvider.getSelection();
					if (!sel.isEmpty())
					{
						Object[] selection = sel.toArray();
						persistCtxt = selection[0] instanceof PersistContext ? (PersistContext)selection[0] : PersistContext.create((IPersist)selection[0]);
					}
				}

				if (persistCtxt != null)
				{
					PersistContext persistContext = persistCtxt;

					Object dlgSelection = null;
					if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.template") != null)
					{
						if (persistContext.getPersist() instanceof AbstractContainer)
						{
							TreeSelectDialog dialog = new TreeSelectDialog(activeEditor.getEditorSite().getShell(), true, true, TreePatternFilter.FILTER_LEAFS,
								FlatTreeContentProvider.INSTANCE, new LabelProvider()
								{
									@Override
									public String getText(Object element)
									{
										return ((TemplateElementHolder)element).template.getName();
									};
								}, null, null, SWT.NONE, "Select template",
								DesignerUtil.getResponsiveLayoutTemplates((AbstractContainer)persistContext.getPersist()), null, false, "TemplateDialog", null,
								false);
							if (dialog.open() == Window.CANCEL)
							{
								return null;
							}
							dlgSelection = ((StructuredSelection)dialog.getSelection()).getFirstElement();
						}
					}
					else if (event.getParameters().isEmpty())
					{
						//we need to ask for component spec
						List<WebObjectSpecification> specs = new ArrayList<WebObjectSpecification>();
						WebObjectSpecification[] webComponentSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebObjectSpecifications();
						for (WebObjectSpecification webComponentSpec : webComponentSpecifications)
						{
							if (webComponentSpec.isDeprecated()) continue;
							if (!webComponentSpec.getPackageName().equals("servoydefault"))
							{
								specs.add(webComponentSpec);
							}
						}
						LabelProvider labelProvider = new LabelProvider()
						{
							@Override
							public String getText(Object element)
							{
								String displayName = ((WebObjectSpecification)element).getDisplayName();
								if (Utils.stringIsEmpty(displayName))
								{
									displayName = ((WebObjectSpecification)element).getName();
									int index = displayName.indexOf("-");
									if (index != -1)
									{
										displayName = displayName.substring(index + 1);
									}
								}
								return displayName + " [" + ((WebObjectSpecification)element).getPackageName() + "]";
							};
						};
						Collections.sort(specs, new Comparator<WebObjectSpecification>()
						{

							@Override
							public int compare(WebObjectSpecification o1, WebObjectSpecification o2)
							{
								return NameComparator.INSTANCE.compare(labelProvider.getText(o1), labelProvider.getText(o2));
							}
						});
						TreeSelectDialog dialog = new TreeSelectDialog(activeEditor.getEditorSite().getShell(), true, true, TreePatternFilter.FILTER_LEAFS,
							FlatTreeContentProvider.INSTANCE, labelProvider, null, null, SWT.NONE, "Select spec", specs.toArray(new WebObjectSpecification[0]),
							null, false, "SpecDialog", null, false);
						if (dialog.open() == Window.CANCEL)
						{
							return null;
						}
						dlgSelection = ((StructuredSelection)dialog.getSelection()).getFirstElement();
					}


					Object dialogSelection = dlgSelection;
					final IStructuredSelection[] newSelection = new IStructuredSelection[1];
					final boolean[] fullRefreshNeeded = new boolean[] { false };
					final IPersist[] finalPersist = new IPersist[1];
					activeEditor.getCommandStack().execute(new BaseRestorableCommand("createLayoutContainer")
					{

						@Override
						public void execute()
						{
							try
							{
								if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.customtype.property") != null)
								{
									if (persistContext.getPersist() instanceof IBasicWebComponent)
									{
										IBasicWebComponent parentBean = (IBasicWebComponent)ElementUtil.getOverridePersist(persistContext);
										WebCustomType customType = addCustomType(parentBean,
											event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.customtype.property"), null,
											-1, null);
										finalPersist[0] = customType;
										showDataproviderDialog(customType.getPropertyDescription().getProperties(), customType, activeEditor.getForm());
									}
								}
								else if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.spec") != null)
								{
									String specName = event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.spec");
									String packageName = event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.package");
									finalPersist[0] = addLayoutComponent(persistContext, specName, packageName,
										new JSONObject(event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.config")),
										computeNextLayoutContainerIndex(persistContext.getPersist()));
								}
								else if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.template") != null)
								{
									AbstractContainer parentPersist = (AbstractContainer)ElementUtil.getOverridePersist(persistContext);
									int x = parentPersist.getAllObjectsAsList().size();
									TemplateElementHolder template = (TemplateElementHolder)dialogSelection;
									Object[] applyTemplate = ElementFactory.applyTemplate(parentPersist, template,
										new org.eclipse.swt.graphics.Point(x + 1, x + 1), false);
									if (applyTemplate.length > 0)
									{
										List<IPersist> persists = new ArrayList<>();
										for (Object o : applyTemplate)
										{
											if (o instanceof FormElementGroup)
											{
												FormElementGroup group = (FormElementGroup)o;
												group.getElements().forEachRemaining(persists::add);
											}
											else if (o instanceof IPersist)
											{
												persists.add((IPersist)o);
											}
										}
										ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, persists);
										newSelection[0] = new StructuredSelection(persists.size() > 0 ? persists.get(0) : persists);
										finalPersist[0] = parentPersist;
									}
								}
								else
								{
									AbstractContainer parentPersist = (AbstractContainer)ElementUtil.getOverridePersist(persistContext);
									WebObjectSpecification spec = (WebObjectSpecification)dialogSelection;
									String componentName = spec.getName();
									int index = componentName.indexOf("-");
									if (index != -1)
									{
										componentName = componentName.substring(index + 1);
									}
									componentName = componentName.replaceAll("-", "_");
									String baseName = componentName;
									int i = 1;
									while (!PersistFinder.INSTANCE.checkName(activeEditor.getForm(), componentName))
									{
										componentName = baseName + "_" + i;
										i++;
									}
									WebComponent webComponent = parentPersist.createNewWebComponent(componentName, spec.getName());
									finalPersist[0] = webComponent;

									if (parentPersist instanceof LayoutContainer || activeEditor.getForm().isResponsiveLayout())
									{
										int maxLocation = 0;
										ISupportChilds parent = PersistHelper.getFlattenedPersist(ModelUtils.getEditingFlattenedSolution(webComponent),
											activeEditor.getForm(), parentPersist);
										Iterator<IPersist> it = parent.getAllObjects();
										while (it.hasNext())
										{
											IPersist currentPersist = it.next();
											if (currentPersist != webComponent && currentPersist instanceof ISupportBounds)
											{
												Point location = ((ISupportBounds)currentPersist).getLocation();
												if (location.x > maxLocation) maxLocation = location.x;
												if (location.y > maxLocation) maxLocation = location.y;
											}
										}
										webComponent.setLocation(new Point(maxLocation + 1, maxLocation + 1));
									}
									Collection<String> allPropertiesNames = spec.getAllPropertiesNames();
									for (String string : allPropertiesNames)
									{
										PropertyDescription property = spec.getProperty(string);
										if (property != null)
										{
											if (property.getInitialValue() != null)
											{
												Object initialValue = property.getInitialValue();
												if (initialValue != null)
													webComponent.setProperty(string, ServoyJSONObject.deepCloneJSONArrayOrObj(initialValue));
											}
											if ("autoshow".equals(property.getTag("wizard")))
											{
												CreateComponentHandler.autoshowWizard(parentPersist, spec, webComponent, property,
													activeEditor.getForm(), id);
											}
										}
									}
									AddContainerCommand.showDataproviderDialog(spec.getProperties(), webComponent, activeEditor.getForm());
								}
								if (finalPersist[0] != null)
								{
									List<IPersist> changes = new ArrayList<>();
									if (!persistContext.getPersist().getUUID().equals(finalPersist[0].getParent().getUUID()))
									{
										ISupportChilds parent = finalPersist[0].getParent();
										changes.add(finalPersist[0].getParent());

										FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(finalPersist[0]);
										parent = PersistHelper.getFlattenedPersist(flattenedSolution, activeEditor.getForm(), parent);
										Iterator<IPersist> it = parent.getAllObjects();
										while (it.hasNext())
										{
											// parent is overridden, make sure all children are sent to designer
											changes.add(ElementUtil
												.getOverridePersist(PersistContext.create(it.next(), activeEditor.getForm())));
										}
										fullRefreshNeeded[0] = changes.size() > 2;
									}
									else
									{
										changes.add(finalPersist[0]);
									}
									ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
									Object[] selection = new Object[] { PersistContext.create(finalPersist[0], persistContext.getContext()) };
									newSelection[0] = new StructuredSelection(selection);
								}
							}
							catch (Exception ex)
							{
								Debug.error(ex);
							}
						}

						@Override
						public void undo()
						{
							try
							{
								if (finalPersist[0] != null)
								{
									((IDeveloperRepository)finalPersist[0].getRootObject().getRepository()).deleteObject(finalPersist[0]);
									ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
										asList(new IPersist[] { finalPersist[0] }));
								}
							}
							catch (RepositoryException e)
							{
								ServoyLog.logError("Could not undo create layout container", e);
							}
						}
					});
					if (fullRefreshNeeded[0])
					{
						Display.getDefault().asyncExec(new Runnable()
						{
							@Override
							public void run()
							{
								CreateComponentCommand.doFullFormRefresh(null);
							}
						});
					}
					if (newSelection[0] != null)
					{
						// wait for tree to be refreshed with new element
						Display.getDefault().asyncExec(new Runnable()
						{
							@Override
							public void run()
							{
								Display.getDefault().asyncExec(new Runnable()
								{
									@Override
									public void run()
									{
										CreateComponentCommand.doFullFormRefresh(null);
										if (DesignerUtil.getContentOutline() != null)
										{
											DesignerUtil.getContentOutline().setSelection(newSelection[0]);
										}
										else
										{
											selectionProvider.setSelection(newSelection[0]);
										}

										IStructuredSelection contentOutlineSelection = (IStructuredSelection)DesignerUtil.getContentOutline().getSelection();
										IStructuredSelection selectionProviderSelection = (IStructuredSelection)selectionProvider.getSelection();
										if (contentOutlineSelection.size() == selectionProviderSelection.size() &&
											contentOutlineSelection.getFirstElement() != selectionProviderSelection.getFirstElement())
										{
											selectionProvider.setSelection(newSelection[0]);
										}

										if (finalPersist[0] instanceof LayoutContainer &&
											CSSPositionUtils.isCSSPositionContainer((LayoutContainer)finalPersist[0]))
										{
											if (org.eclipse.jface.dialogs.MessageDialog.openQuestion(UIUtils.getActiveShell(),
												"Edit css position container",
												"Do you want to zoom into the layout container so you can edit it ?"))
											{
												BaseVisualFormEditor editor = DesignerUtil.getActiveEditor();
												if (editor != null)
												{
													BaseVisualFormEditorDesignPage activePage = editor.getGraphicaleditor();
													if (activePage instanceof RfbVisualFormEditorDesignPage)
														((RfbVisualFormEditorDesignPage)activePage).showContainer((LayoutContainer)finalPersist[0]);
												}
											}
										}
									}
								});

							}
						});
					}
				}
			}
		}
		catch (NullPointerException npe)
		{
			Debug.log(npe);
		}
		return null;
	}

	private LayoutContainer addLayoutComponent(PersistContext parentPersist, String specName, String packageName, JSONObject configJson, int index)
	{
		LayoutContainer container;
		try
		{
			if (parentPersist != null && parentPersist.getPersist() instanceof AbstractBase && parentPersist.getPersist() instanceof ISupportChilds)
			{
				AbstractBase parent = (AbstractBase)ElementUtil.getOverridePersist(parentPersist);
				PackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
					packageName);
				container = (LayoutContainer)parent.getRootObject().getChangeHandler().createNewObject(((ISupportChilds)parent), IRepository.LAYOUTCONTAINERS);
				container.setSpecName(specName);
				container.setPackageName(packageName);
				parent.addChild(container);
				container.setLocation(new Point(index, index));
				if (CSSPositionUtils.isCSSPositionContainer(container)) container.setSize(new Dimension(200, 200));
				if (configJson != null)
				{
					Iterator keys = configJson.keys();
					while (keys.hasNext())
					{
						String key = (String)keys.next();
						Object value = configJson.get(key);
						if ("children".equals(key))
						{
							// special key to create children instead of a attribute set.
							JSONArray array = (JSONArray)value;
							for (int i = 0; i < array.length(); i++)
							{
								JSONObject jsonObject = array.getJSONObject(i);
								if (jsonObject.has("layoutName"))
								{
									WebLayoutSpecification spec = specifications.getSpecification(jsonObject.getString("layoutName"));
									addLayoutComponent(PersistContext.create(container, parentPersist.getContext()), spec.getName(), packageName,
										jsonObject.optJSONObject("model"), i + 1);
								}
								else if (jsonObject.has("componentName"))
								{
									WebComponent component = (WebComponent)parent.getRootObject().getChangeHandler().createNewObject(((ISupportChilds)parent),
										IRepository.WEBCOMPONENTS);
									component.setLocation(new Point(i + 1, i + 1));
									component.setTypeName(jsonObject.getString("componentName"));
									container.addChild(component);
								}
							}
						} // children and layoutName are special
						else if (!"layoutName".equals(key))
						{
							container.putAttribute(key, value.toString());
						}
						else if ("layoutName".equals(key))
						{
							container.setSpecName(value.toString());
						}
					}
					return container;
				}
			}
		}
		catch (RepositoryException e)
		{
			Debug.log(e);
		}
		catch (JSONException e)
		{
			Debug.log(e);
		}
		return null;
	}

	private int computeNextLayoutContainerIndex(IPersist parent)
	{
		if (parent.getAncestor(IRepository.CSSPOS_LAYOUTCONTAINERS) != null)
		{
			return ((LayoutContainer)parent).getAllObjectsAsList().size();
		}
		int maxLocation = 0;
		if (parent instanceof ISupportFormElements)
		{
			Iterator<IPersist> allObjects = ((ISupportFormElements)parent).getAllObjects();

			while (allObjects.hasNext())
			{
				IPersist child = allObjects.next();
				if (child instanceof ISupportBounds element)
				{
					Point location = element.getLocation();
					if (location.x > maxLocation) maxLocation = location.x;
					if (location.y > maxLocation) maxLocation = location.y;
				}
			}
		}
		return maxLocation + 1;
	}

	/**
	 * @param activeEditor
	 * @param customType
	 */
	public static void showDataproviderDialog(Map<String, PropertyDescription> properties, AbstractBase webComponent, Form form)
	{
		boolean showDialog = true;
		List<Entry<String, PropertyDescription>> dataproviderProperties = properties.entrySet().stream()
			.filter(entry -> entry.getValue().getType().getName().equals(DataproviderPropertyType.TYPE_NAME) && (entry.getValue().getTag("wizard") != null &&
				(entry.getValue().getTag("wizard").toString().equals("true") || entry.getValue().getTag("wizard").equals("1"))))
			.collect(Collectors.toList());

		ISupportChilds wc = webComponent.getParent();
		while (wc != null && !(wc instanceof Form))
		{
			wc = wc.getParent();
		}
		if (wc instanceof Form frm)
		{
			boolean isFC = frm.isFormComponent().booleanValue();
			boolean hasDB = frm.getDataSource() == null ? false : true;
			if (isFC && !hasDB)
			{
				showDialog = false;
			}
		}

		if (showDialog)
		{
			if (dataproviderProperties.size() == 1)
			{
				Entry<String, PropertyDescription> entry = dataproviderProperties.get(0);
				CreateComponentHandler.autoShowDataProviderSelection(entry.getValue(), form, webComponent,
					entry.getKey());
			}
			else if (dataproviderProperties.size() > 1)
			{
				List<String> collect = dataproviderProperties.stream().map(entry -> entry.getKey()).collect(Collectors.toList());

				ElementListSelectionDialog dialog = new ElementListSelectionDialog(getWorkbench().getActiveWorkbenchWindow().getShell(),
					new LabelProvider()
					{
						@Override
						public String getText(Object element)
						{
							String txt = super.getText(element);
							if (txt.endsWith("ID")) txt = txt.substring(0, txt.length() - 2);
							return txt;
						}
					});
				dialog.setMultipleSelection(true);
				dialog.setMessage("Select the dataprovider properties that you want to configure");
				dialog.setElements(collect.toArray());
				List<String> dataproviderNames = collect.stream().filter(property -> property.toLowerCase().startsWith("dataprovider"))
					.collect(Collectors.toList());
				if (dataproviderNames.size() > 0)
				{
					// this is a bit hard coded to at least select the one that is called dataprovider as the main selection.
					dialog.setInitialSelections(dataproviderNames.toArray());
				}
				dialog.setTitle("Dataprovider properties configuration");
				try
				{
					if (dialog.open() == Window.OK)
					{
						asList(dialog.getResult()).forEach(property -> {
							PropertyDescription propertyDescription = properties.get(property);
							CreateComponentHandler.autoShowDataProviderSelection(propertyDescription, form, webComponent, (String)property);
						});
					}
				}
				catch (Exception e)
				{
					// happens sometimes when active window is not found (only while debugging?)
					ServoyLog.logError(e);
				}
			}
		}
	}

	public static WebCustomType addCustomType(IBasicWebComponent parentBean, String propertyName, String compName, int arrayIndex, WebCustomType template)
	{
		return addCustomType(WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(parentBean.getTypeName()),
			parentBean, propertyName, compName, arrayIndex, template);
	}

	public static WebCustomType addCustomType(WebObjectSpecification componentSpec, IBasicWebObject parentWebObject, String propertyName, String compName,
		int arrayIndex, WebCustomType template)
	{
		int index = arrayIndex;

		boolean isArray;
		PropertyDescription targetPD;
		if (parentWebObject instanceof IBasicWebComponent)
		{
			// For a component we get the property from the spec
			isArray = componentSpec.isArrayReturnType(propertyName);
			targetPD = componentSpec.getProperty(propertyName);
		}
		else
		{
			// for a nested child we get the property from its type
			ICustomType< ? > customType = componentSpec.getDeclaredCustomObjectTypes().get(parentWebObject.getTypeName());
			if (customType != null)
			{
				PropertyDescription customTypePD = customType.getCustomJSONTypeDefinition();
				isArray = customTypePD.isArrayReturnType(propertyName);
				targetPD = customTypePD.getProperty(propertyName);
			}
			else
			{
				return null;
			}
		}

		String typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(targetPD.getType());
		IChildWebObject[] arrayValue = null;
		if (isArray)
		{
			targetPD = ((ICustomType< ? >)targetPD.getType()).getCustomJSONTypeDefinition();
			var value = parentWebObject.getProperty(propertyName);
			if (value instanceof IChildWebObject[])
			{
				arrayValue = (IChildWebObject[])value;
			}
			if (index == -1) index = arrayValue != null ? arrayValue.length : 0;
		}
		if (parentWebObject instanceof ISupportsIndexedChildren)
		{
			ISupportsIndexedChildren parentSupportsChildren = (ISupportsIndexedChildren)parentWebObject;
			WebCustomType customType = WebCustomType.createNewInstance(parentWebObject, targetPD, propertyName, index, true);
			customType.setName(compName);
			customType.setTypeName(typeName);

			if (targetPD.getType() instanceof NGCustomJSONObjectType)
			{
				Collection<String> allPropertiesNames = ((NGCustomJSONObjectType)targetPD.getType()).getCustomJSONTypeDefinition().getAllPropertiesNames();
				for (String string : allPropertiesNames)
				{
					PropertyDescription property = ((NGCustomJSONObjectType)targetPD.getType()).getCustomJSONTypeDefinition().getProperty(string);
					if (template != null && template.hasProperty(string))
					{
						Object propValue = template.getProperty(string);

						if (property.getTag("wizard") != null && property.getTag("wizard") instanceof JSONObject &&
							((JSONObject)property.getTag("wizard")).has("unique") && ((JSONObject)property.getTag("wizard")).get("unique").equals(true))
						{
							propValue = createUniqueID(property, arrayValue, propValue.toString());
						}

						customType.setProperty(string, propValue);
					}
					else if (property != null && property.getInitialValue() != null)
					{
						Object initialValue = property.getInitialValue();
						if (initialValue != null) customType.setProperty(string, ServoyJSONObject.deepCloneJSONArrayOrObj(initialValue));
					}
				}
			}

			parentSupportsChildren.insertChild(customType); // if it is array it will make use of index given above in WebCustomType.createNewInstance(...) when inserting; otherwise it's a simple set to some property name anyway
			return customType;
		}
		return null;
	}

	private static String createUniqueID(PropertyDescription pd, IChildWebObject[] columns, String value)
	{
		String newValue = value;
		if (newValue != null)
		{
			List<String> columnsID = new ArrayList<String>();
			for (IChildWebObject column : columns)
			{
				if (column.getJson().has(pd.getName()))
				{
					columnsID.add(column.getJson().getString(pd.getName()));
				}
			}
			for (int i = 0; i < 100; i++)
			{
				if (columnsID.contains(newValue))
				{
					newValue = generateID(value, i + 1);
				}
				else
				{
					break;
				}
			}
		}
		return newValue;
	}

	private static String generateID(String value, int n)
	{
		StringBuffer sb = new StringBuffer(value.length() + n);
		sb.append(value);
		for (int i = 0; i < n; i++)
		{
			sb.append("_c");
		}
		return sb.toString();
	}
}
