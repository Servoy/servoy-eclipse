package com.servoy.eclipse.designer.editor.commands;

import java.awt.Dimension;
import java.awt.Point;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

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
import org.eclipse.swt.widgets.Shell;
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
import com.servoy.eclipse.core.util.TemplateElementHolder;
import com.servoy.eclipse.designer.editor.BaseRestorableCommand;
import com.servoy.eclipse.designer.editor.BaseVisualFormEditor;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.PersistFinder;
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
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IBasicWebComponent;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportFormElements;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.NameComparator;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.property.types.NGCustomJSONObjectType;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.PersistHelper;


public class AddContainerCommand extends AbstractHandler implements IHandler
{
	public static final String COMMAND_ID = "com.servoy.eclipse.designer.rfb.add";

	@Override
	public Object execute(final ExecutionEvent event) throws ExecutionException
	{
		try
		{
			final BaseVisualFormEditor activeEditor = DesignerUtil.getActiveEditor();
			if (activeEditor != null)
			{
				activeEditor.getCommandStack().execute(new BaseRestorableCommand("createLayoutContainer")
				{
					private IPersist persist;

					@Override
					public void execute()
					{
						try
						{
							final ISelectionProvider selectionProvider = activeEditor.getSite().getSelectionProvider();
							PersistContext persistContext = null;
							if (DesignerUtil.getContentOutlineSelection() != null)
							{
								persistContext = DesignerUtil.getContentOutlineSelection();
							}
							else
							{
								IStructuredSelection sel = (IStructuredSelection)selectionProvider.getSelection();
								if (!sel.isEmpty())
								{
									Object[] selection = sel.toArray();
									persistContext = selection[0] instanceof PersistContext ? (PersistContext)selection[0]
										: PersistContext.create((IPersist)selection[0]);
								}
							}
							if (persistContext != null)
							{
								if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.customtype.property") != null)
								{
									if (persistContext.getPersist() instanceof IBasicWebComponent)
									{
										IBasicWebComponent parentBean = (IBasicWebComponent)ElementUtil.getOverridePersist(persistContext);
										addCustomType(parentBean, event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.customtype.property"), null,
											-1);
										persist = parentBean;
									}
								}
								else if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.spec") != null)
								{
									String specName = event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.spec");
									String packageName = event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.package");
									persist = addLayoutComponent(persistContext, specName, packageName,
										new JSONObject(event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.config")),
										computeNextLayoutContainerIndex(persistContext.getPersist()));
								}
								else if (event.getParameter("com.servoy.eclipse.designer.editor.rfb.menu.add.template") != null)
								{
									if (persistContext.getPersist() instanceof AbstractContainer)
									{
										TreeSelectDialog dialog = new TreeSelectDialog(new Shell(), true, true, TreePatternFilter.FILTER_LEAFS,
											FlatTreeContentProvider.INSTANCE, new LabelProvider()
											{
												@Override
												public String getText(Object element)
												{
													return ((TemplateElementHolder)element).template.getName();
												};
											}, null, null, SWT.NONE, "Select template",
											DesignerUtil.getResponsiveLayoutTemplates((AbstractContainer)persistContext.getPersist()), null, false,
											"TemplateDialog", null);
										dialog.open();
										if (dialog.getReturnCode() == Window.OK)
										{
											AbstractContainer parentPersist = (AbstractContainer)ElementUtil.getOverridePersist(persistContext);
											int x = parentPersist.getAllObjectsAsList().size();
											TemplateElementHolder template = (TemplateElementHolder)((StructuredSelection)dialog.getSelection()).getFirstElement();
											Object[] applyTemplate = ElementFactory.applyTemplate(parentPersist, template,
												new org.eclipse.swt.graphics.Point(x + 1, x + 1), false);
											if (applyTemplate.length > 0)
											{
												persist = parentPersist;
											}
										}
									}
								}
								else
								{
									List<WebObjectSpecification> specs = new ArrayList<WebObjectSpecification>();
									WebObjectSpecification[] webComponentSpecifications = WebComponentSpecProvider.getSpecProviderState().getAllWebComponentSpecifications();
									for (WebObjectSpecification webComponentSpec : webComponentSpecifications)
									{
										if (!webComponentSpec.getPackageName().equals("servoydefault"))
										{
											specs.add(webComponentSpec);
										}
									}
									Collections.sort(specs, new Comparator<WebObjectSpecification>()
									{

										@Override
										public int compare(WebObjectSpecification o1, WebObjectSpecification o2)
										{
											return NameComparator.INSTANCE.compare(o1.getName(), o2.getName());
										}
									});
									TreeSelectDialog dialog = new TreeSelectDialog(new Shell(), true, true, TreePatternFilter.FILTER_LEAFS,
										FlatTreeContentProvider.INSTANCE, new LabelProvider()
										{
											@Override
											public String getText(Object element)
											{
												String componentName = ((WebObjectSpecification)element).getName();
												int index = componentName.indexOf("-");
												if (index != -1)
												{
													componentName = componentName.substring(index + 1);
												}
												return componentName + " [" + ((WebObjectSpecification)element).getPackageName() + "]";
											};
										}, null, null, SWT.NONE, "Select spec", specs.toArray(new WebObjectSpecification[0]), null, false, "SpecDialog", null);
									dialog.open();
									if (dialog.getReturnCode() == Window.OK)
									{
										if (persistContext.getPersist() instanceof AbstractContainer)
										{
											AbstractContainer parentPersist = (AbstractContainer)ElementUtil.getOverridePersist(persistContext);
											WebObjectSpecification spec = (WebObjectSpecification)((StructuredSelection)dialog.getSelection()).getFirstElement();
											String componentName = spec.getName();
											int index = componentName.indexOf("-");
											if (index != -1)
											{
												componentName = componentName.substring(index + 1);
											}
											componentName = componentName.replaceAll("-", "_");
											String baseName = componentName;
											int i = 1;
											while (!PersistFinder.INSTANCE.checkName(activeEditor, componentName))
											{
												componentName = baseName + "_" + i;
												i++;
											}
											persist = parentPersist.createNewWebComponent(componentName, spec.getName());

											Collection<String> allPropertiesNames = spec.getAllPropertiesNames();
											for (String string : allPropertiesNames)
											{
												PropertyDescription property = spec.getProperty(string);
												if (property != null && property.getInitialValue() != null)
												{
													Object initialValue = property.getInitialValue();
													if (initialValue != null) ((WebComponent)persist).setProperty(string, initialValue);
												}
											}
										}
									}
								}
								if (persist != null)
								{
									List<IPersist> changes = new ArrayList<>();
									if (!persistContext.getPersist().getUUID().equals(persist.getParent().getUUID()))
									{
										ISupportChilds parent = persist.getParent();
										changes.add(persist.getParent());

										FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
										parent = PersistHelper.getFlattenedPersist(flattenedSolution, activeEditor.getForm(), parent);
										Iterator<IPersist> it = parent.getAllObjects();
										while (it.hasNext())
										{
											IPersist next = it.next();
											IPersist child = ElementUtil.getOverridePersist(PersistContext.create(next, activeEditor.getForm()));
											if (child.getParent() instanceof Form)
											{
												child.getParent().removeChild(child);
											}
											changes.add(child);
											if (child.equals(next)) continue;
											parent.removeChild(next);
											parent.addChild(child);
										}
									}
									else
									{
										changes.add(persist);
									}
									ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false, changes);
									Object[] selection = new Object[] { PersistContext.create(persist, persistContext.getContext()) };
									final IStructuredSelection structuredSelection = new StructuredSelection(selection);
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
													if (DesignerUtil.getContentOutline() != null)
													{
														DesignerUtil.getContentOutline().setSelection(structuredSelection);
													}
													else
													{
														selectionProvider.setSelection(structuredSelection);
													}
												}
											});

										}
									});
								}
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
							if (persist != null)
							{
								((IDeveloperRepository)persist.getRootObject().getRepository()).deleteObject(persist);
								ServoyModelManager.getServoyModelManager().getServoyModel().firePersistsChanged(false,
									Arrays.asList(new IPersist[] { persist }));
							}
						}
						catch (RepositoryException e)
						{
							ServoyLog.logError("Could not undo create layout container", e);
						}
					}
				});
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
				if (PersistHelper.isCSSPositionContainer(container)) container.setSize(new Dimension(200, 200));
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
									((AbstractBase)container).addChild(component);
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
		int i = 1;
		if (parent instanceof ISupportFormElements)
		{
			Iterator<IPersist> allObjects = ((ISupportFormElements)parent).getAllObjects();

			while (allObjects.hasNext())
			{
				IPersist child = allObjects.next();
				if (child instanceof AbstractContainer)
				{
					i++;
				}
			}
			return i;
		}
		return i;
	}

	public static WebCustomType addCustomType(IBasicWebComponent parentBean, String propertyName, String compName, int arrayIndex)
	{
		int index = arrayIndex;
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(parentBean.getTypeName());
		boolean isArray = spec.isArrayReturnType(propertyName);
		PropertyDescription targetPD = spec.getProperty(propertyName);
		String typeName = PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(targetPD.getType());
		IChildWebObject[] arrayValue = null;
		if (isArray)
		{
			targetPD = ((ICustomType< ? >)targetPD.getType()).getCustomJSONTypeDefinition();
			if (parentBean instanceof WebComponent)
			{
				arrayValue = (IChildWebObject[])((WebComponent)parentBean).getProperty(propertyName);
			}
			if (index == -1) index = arrayValue != null ? arrayValue.length : 0;
		}
		if (parentBean instanceof WebComponent)
		{
			WebCustomType bean = WebCustomType.createNewInstance(parentBean, targetPD, propertyName, index, true);
			bean.setName(compName);
			bean.setTypeName(typeName);

			if (targetPD.getType() instanceof NGCustomJSONObjectType)
			{
				Collection<String> allPropertiesNames = ((NGCustomJSONObjectType)targetPD.getType()).getCustomJSONTypeDefinition().getAllPropertiesNames();
				for (String string : allPropertiesNames)
				{
					PropertyDescription property = ((NGCustomJSONObjectType)targetPD.getType()).getCustomJSONTypeDefinition().getProperty(string);
					if (property != null && property.getInitialValue() != null)
					{
						Object initialValue = property.getInitialValue();
						if (initialValue != null) bean.setProperty(string, initialValue);
					}
				}
			}

			if (isArray)
			{
				if (arrayValue == null)
				{
					arrayValue = new IChildWebObject[] { bean };
				}
				else
				{
					if (index > -1)
					{
						ArrayList<IChildWebObject> arrayList = new ArrayList<IChildWebObject>();
						arrayList.addAll(Arrays.asList(arrayValue));
						arrayList.add(index, bean);
						// update index for all elements that are after the inserted one
						for (int i = index + 1; i < arrayList.size(); i++)
						{
							arrayList.get(i).setIndex(i);
						}
						arrayValue = arrayList.toArray(new IChildWebObject[arrayList.size()]);
					}
					else
					{
						IChildWebObject[] newArrayValue = new IChildWebObject[arrayValue.length + 1];
						System.arraycopy(arrayValue, 0, newArrayValue, 0, arrayValue.length);
						newArrayValue[arrayValue.length] = bean;
						arrayValue = newArrayValue;
					}
				}
				((WebComponent)parentBean).setProperty(propertyName, arrayValue);
			}
			else((WebComponent)parentBean).setProperty(propertyName, bean);

			return bean;
		}
		return null;
	}
}
