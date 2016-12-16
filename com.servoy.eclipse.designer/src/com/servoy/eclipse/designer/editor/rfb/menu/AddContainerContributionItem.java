package com.servoy.eclipse.designer.editor.rfb.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.websocket.utils.PropertyUtils;

import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.designer.editor.rfb.actions.handlers.GhostHandler;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.util.Debug;

public class AddContainerContributionItem extends CompoundContributionItem
{

	/**
	 *
	 */
	public AddContainerContributionItem()
	{
	}

	public AddContainerContributionItem(String id)
	{
		super(id);
	}

	@Override
	protected IContributionItem[] getContributionItems()
	{
		List<IContributionItem> list = new ArrayList<IContributionItem>();
		PersistContext persistContext = DesignerUtil.getContentOutlineSelection();
		IPersist persist = null;
		if (persistContext != null) persist = persistContext.getPersist();
		if (persist instanceof LayoutContainer)
		{
			PackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().get(
				((LayoutContainer)persist).getPackageName());
			if (specifications != null)
			{
				WebLayoutSpecification layoutSpec = specifications.getSpecification(((LayoutContainer)persist).getSpecName());
				if (layoutSpec != null)
				{
					//for the right-clicked persist we iterate through all it's possible children
					Set<String> allowedChildren = DesignerUtil.getAllowedChildren().get(layoutSpec.getPackageName() + "." + layoutSpec.getName());
					boolean isComponentAdded = false;
					for (String allowedChildName : allowedChildren)
					{
						if ("component".equalsIgnoreCase(allowedChildName))
						{
							addMenuItem(list, null, null, null);
							isComponentAdded = true;
						}
						else
						{
							//then we iterate through all the layouts that we have and check if the layoutName matches the current allowedChildName
							for (WebLayoutSpecification specification : specifications.getSpecifications().values())
							{
								String layoutName;
								try
								{
									layoutName = new JSONObject((String)specification.getConfig()).optString("layoutName", null);
									if (layoutName == null)
									{
										layoutName = specification.getName();
									}

									//if the layoutName matches the current allowedChildName then we add this container as a menu entry
									if (allowedChildName.equals(((LayoutContainer)persist).getPackageName() + "." + layoutName))
									{
										String config = specification.getConfig() instanceof String ? specification.getConfig().toString() : "{}";
										addMenuItem(list, specification, config, null);
									}

								}
								catch (JSONException e)
								{
									Debug.log(e);
								}
							}
						}
					}
					if (!isComponentAdded)
					{
						for (String allowedChildName : allowedChildren)
						{
							if (allowedChildName.endsWith(".*"))
							{
								addMenuItem(list, null, null, null);
								break;
							}
						}
					}
				}
			}
		}
		else if (persist instanceof Form)
		{
			Collection<PackageSpecification<WebLayoutSpecification>> values = WebComponentSpecProvider.getSpecProviderState().getLayoutSpecifications().values();
			for (PackageSpecification<WebLayoutSpecification> specifications : values)
			{
				for (WebLayoutSpecification specification : specifications.getSpecifications().values())
				{
					if (specification.isTopContainer())
					{
						String config = specification.getConfig() instanceof String ? specification.getConfig().toString() : "{}";
						addMenuItem(list, specification, config, null);
					}
				}
			}
		}
		else if (persist instanceof WebComponent)
		{
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebComponentSpecification(
				((WebComponent)persist).getTypeName());
			Map<String, PropertyDescription> properties = spec.getProperties();
			for (PropertyDescription propertyDescription : properties.values())
			{
				if (GhostHandler.isDroppable(propertyDescription, propertyDescription.getConfig()))
				{
					addMenuItem(list, null, propertyDescription.getName(),
						PropertyUtils.getSimpleNameOfCustomJSONTypeProperty(propertyDescription.getType()) + " -> " + propertyDescription.getName());
				}
			}
		}
		return list.toArray(new IContributionItem[list.size()]);
	}

	private void addMenuItem(List<IContributionItem> list, WebLayoutSpecification specification, String config, String displayName)
	{
		final CommandContributionItemParameter commandContributionItemParameter = new CommandContributionItemParameter(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow(), null, AddContainerCommand.COMMAND_ID, CommandContributionItem.STYLE_PUSH);
		if (specification != null)
		{
			commandContributionItemParameter.parameters = new HashMap<String, String>();
			commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.spec", specification.getName());
			commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.package", specification.getPackageName());
			commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.config", config);
			commandContributionItemParameter.label = specification.getDisplayName();
		}
		else if (config != null)
		{
			commandContributionItemParameter.parameters = new HashMap<String, String>();
			commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.customtype.property", config);
			commandContributionItemParameter.label = displayName;
		}
		else
		{
			// add a new web component
			commandContributionItemParameter.label = "Component";
		}
		commandContributionItemParameter.visibleEnabled = true;
		list.add(new CommandContributionItem(commandContributionItemParameter));
	}
}
