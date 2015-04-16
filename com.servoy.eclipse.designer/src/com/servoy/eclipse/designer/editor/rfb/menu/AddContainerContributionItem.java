package com.servoy.eclipse.designer.editor.rfb.menu;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.views.contentoutline.ContentOutline;
import org.sablo.specification.WebComponentPackageSpecification;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;

import com.servoy.eclipse.designer.editor.commands.AddContainerCommand;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.LayoutContainer;

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
		IStructuredSelection selection = getSelection();
		if (selection != null)
		{
			Object firstElement = selection.getFirstElement();
			if (firstElement instanceof PersistContext)
			{
				PersistContext persistContext = (PersistContext)firstElement;
				IPersist persist = persistContext.getPersist();
				if (persist instanceof LayoutContainer)
				{
					WebComponentPackageSpecification<WebLayoutSpecification> specifications = WebComponentSpecProvider.getInstance().getLayoutSpecifications().get(
						((LayoutContainer)persist).getPackageName());
					if (specifications != null)
					{
						WebLayoutSpecification layoutSpec = specifications.getSpecification(((LayoutContainer)persist).getSpecName());
						if (layoutSpec != null)
						{
							List<String> allowedChildren = layoutSpec.getAllowedChildren();
							for (String itemName : allowedChildren)
							{
								//if this is still a container
								if (specifications.getSpecification(itemName) != null)
								{
									String config = layoutSpec.getConfig() instanceof String ? (String)specifications.getSpecification(itemName).getConfig()
										: "{}";
									addMenuItem(list, itemName, ((LayoutContainer)persist).getPackageName(), config);
								}
							}
						}
					}
				}
			}
		}
		return list.toArray(new IContributionItem[list.size()]);
	}

	private void addMenuItem(List<IContributionItem> list, String itemName, String packageName, String config)
	{
		final CommandContributionItemParameter commandContributionItemParameter = new CommandContributionItemParameter(
			PlatformUI.getWorkbench().getActiveWorkbenchWindow(), null, AddContainerCommand.COMMAND_ID, CommandContributionItem.STYLE_PUSH);
		commandContributionItemParameter.parameters = new HashMap<String, String>();
		commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.spec", itemName);
		commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.package", packageName);
		commandContributionItemParameter.parameters.put("com.servoy.eclipse.designer.editor.rfb.menu.add.config", config);
		commandContributionItemParameter.label = itemName;
		commandContributionItemParameter.visibleEnabled = true;
		list.add(new CommandContributionItem(commandContributionItemParameter));
	}

	private IStructuredSelection getSelection()
	{
		IWorkbenchWindow active = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (active == null) return null;
		IWorkbenchPage page = active.getActivePage();
		if (page == null) return null;
		IWorkbenchPart part = page.getActivePart();

		if (part instanceof ContentOutline)
		{
			ContentOutline outline = (ContentOutline)part;
			return (IStructuredSelection)outline.getSelection();
		}
		return null;
	}
}
