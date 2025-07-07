package com.servoy.eclipse.debug.handlers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.eclipse.jface.action.IContributionItem;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.actions.CompoundContributionItem;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.menus.CommandContributionItem;
import org.eclipse.ui.menus.CommandContributionItemParameter;
import org.eclipse.ui.menus.IWorkbenchContribution;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.eclipse.ui.services.IServiceLocator;

import com.servoy.eclipse.model.util.ServoyLog;

public class CreateBrowserContributions extends CompoundContributionItem implements IWorkbenchContribution
{

	private IServiceLocator mServiceLocator;
	private Map<String, ImageDescriptor> browsersImagesList;

	public CreateBrowserContributions()
	{
	}

	public CreateBrowserContributions(String id)
	{
		super(id);
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.menus.IWorkbenchContribution#initialize(org.eclipse.ui.services.IServiceLocator)
	 */
	@Override
	public void initialize(IServiceLocator serviceLocator)
	{
		mServiceLocator = serviceLocator;
	}

	private ImageDescriptor getImageForName(String name, String location)
	{
		String browserImgFileName = "";
		String browserName = (name != null ? name.toLowerCase() : "");
		String browserLocation = (location != null ? location.toLowerCase() : "");
		if (browserLocation.contains("iexplore") || browserName.contains("explorer")) browserImgFileName = "explorer.png";
		else if (browserLocation.contains("firefox") || browserName.contains("firefox")) browserImgFileName = "firefox.png";
		else if (browserLocation.contains("chrome") || browserName.contains("chrome")) browserImgFileName = "chrome.png";
		else if (browserLocation.contains("safari") || browserName.contains("safari")) browserImgFileName = "safari.png";
		else if (browserLocation.contains("opera") || browserName.contains("opera")) browserImgFileName = "opera.png";
		return getImageForBrowser(browserImgFileName);
	}

	private ImageDescriptor getImageForBrowser(String name)
	{
		if (browsersImagesList == null) browsersImagesList = new HashMap<String, ImageDescriptor>();
		if (!name.equals("") && !browsersImagesList.containsKey(name))
		{
			ImageDescriptor id = AbstractUIPlugin.imageDescriptorFromPlugin(com.servoy.eclipse.ui.Activator.PLUGIN_ID, "icons/" + name);
			browsersImagesList.put(name, id);
		}
		return browsersImagesList.get(name);
	}


	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.ui.actions.CompoundContributionItem#getContributionItems()
	 */
	@Override
	protected IContributionItem[] getContributionItems()
	{

		java.util.List<IContributionItem> contributions = new ArrayList<IContributionItem>();
		try
		{
			for (IBrowserDescriptor ewb : BrowserManager.getInstance().getWebBrowsers())
			{

				if (ewb.getName() != null && (ewb.getName().contains("Internet Explorer") || ewb.getName().contains("InternetExplorer"))) continue;
				final CommandContributionItemParameter contributionParameter = new CommandContributionItemParameter(mServiceLocator, null, getId(),
					CommandContributionItem.STYLE_PUSH);
				contributionParameter.label = ewb.getName();
				contributionParameter.visibleEnabled = true;
				contributionParameter.icon = getImageForName(ewb.getName(), ewb.getLocation());

				contributionParameter.parameters = new HashMap<String, String>();
				contributionParameter.parameters.put("com.servoy.eclipse.debug.browser", ewb.getName());
				CommandContributionItem theContribution = new CommandContributionItem(contributionParameter);
				contributions.add(theContribution);
			}

		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}

		return contributions.toArray(new IContributionItem[contributions.size()]);
	}
}
