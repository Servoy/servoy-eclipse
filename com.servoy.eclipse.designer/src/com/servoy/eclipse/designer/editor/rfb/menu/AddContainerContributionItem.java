package com.servoy.eclipse.designer.editor.rfb.menu;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.json.JSONException;
import org.json.JSONObject;
import org.sablo.specification.WebComponentPackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.designer.util.DesignerUtil;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.LayoutContainer;
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
		Object persist = DesignerUtil.getContentOutlineSelection();
		if (persist instanceof LayoutContainer)
		{
			WebComponentPackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getInstance().getLayoutSpecifications().get(
				((LayoutContainer)persist).getPackageName());
			if (specifications != null)
			{
				WebLayoutSpecification layoutSpec = specifications.getSpecification(((LayoutContainer)persist).getSpecName());
				if (layoutSpec != null)
				{
					//for the right-clicked persist we iterate through all it's possible children
					List<String> allowedChildren = layoutSpec.getAllowedChildren();
					for (String allowedChildName : allowedChildren)
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
								if (allowedChildName.equals(layoutName))
								{
									String config = specification.getConfig() instanceof String ? specification.getConfig().toString() : "{}";
									addMenuItem(list, specification, config);
								}
							}
							catch (JSONException e)
							{
								Debug.log(e);
							}
						}
					}
					if (allowedChildren.contains("component"))
					{
						addMenuItem(list, null, null);
					}
				}
			}
		}
		else if (persist instanceof Form)
		{
			Collection<WebComponentPackageSpecification<WebLayoutSpecification>> values = WebComponentSpecProvider.getInstance().getLayoutSpecifications().values();
			for (WebComponentPackageSpecification<WebLayoutSpecification> specifications : values)
			{
				for (WebLayoutSpecification specification : specifications.getSpecifications().values())
				{
					if (specification.isTopContainer())
					{
						String config = specification.getConfig() instanceof String ? specification.getConfig().toString() : "{}";
						addMenuItem(list, specification, config);
					}
				}
			}
		}
		return list.toArray(new IContributionItem[list.size()]);
	}

	private void addMenuItem(List<IContributionItem> list, WebLayoutSpecification specification, String config)
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
		else
		{
			// add a new web component
			commandContributionItemParameter.label = "Component";
		}
		commandContributionItemParameter.visibleEnabled = true;
		list.add(new CommandContributionItem(commandContributionItemParameter));
	}
}
