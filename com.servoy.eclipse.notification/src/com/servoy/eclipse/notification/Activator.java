/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2017 Servoy BV

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

package com.servoy.eclipse.notification;


import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.osgi.framework.BundleContext;

import com.rometools.rome.feed.synd.SyndFeed;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.notification.rss.RSSNotification;
import com.servoy.eclipse.notification.rss.RSSNotificationJob;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;


/**
 * The activator class controls the plug-in life cycle, test
 * 
 * @author gboros
 * 
 */
public class Activator extends AbstractUIPlugin
{
	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.notification";

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator()
	{
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#start(org.osgi.framework.BundleContext)
	 */
	@Override
	public void start(BundleContext context) throws Exception
	{
		super.start(context);
		plugin = this;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.plugin.AbstractUIPlugin#stop(org.osgi.framework.BundleContext)
	 */
	@Override
	public void stop(BundleContext context) throws Exception
	{
		stopNotificationJob();
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault()
	{
		return plugin;
	}
	
	private RSSNotificationJob servoyNotificationJob, forumNotificationJob;
	
	void startNotificationJob()
	{
		
		if(servoyNotificationJob == null)
		{
			servoyNotificationJob = new RSSNotificationJob(
				"Servoy notification",
				"https://servoy.com/category/developer-news/feed/",
				false,
				60000 * 120, // 30 min
				"lastNotificationTimestamp"
				);
			servoyNotificationJob.schedule(20000);
		}
		final DesignerPreferences designerPreferences = new DesignerPreferences();
		if(forumNotificationJob == null && designerPreferences.showForumNotifications())
		{
			forumNotificationJob = new RSSNotificationJob(
				"Forum notification",
				"https://forum.servoy.com/rss.php",
				true,
				60000 * 20, // 20 min
				"forumLastNotificationTimestampV2"
				) {
					protected long getNotificationTimestamp(SyndFeed feed, RSSNotification notification)
					{
						long notificationTimestamp = 0;
						String link = notification.getLink();
						if(link != null)
						{
							int idx = link.lastIndexOf("#p");
							if(idx != -1)
							{
								try
								{
									notificationTimestamp = Long.parseLong(link.substring(idx + 2));
								}
								catch(NumberFormatException ex)
								{
									ServoyLog.logError(ex);
								}
							}
						}
						return notificationTimestamp;
					}				
				};
			forumNotificationJob.schedule(40000);
		}		
	}
	
	void stopNotificationJob()
	{
		if(servoyNotificationJob != null)
		{
			servoyNotificationJob.stop();
			servoyNotificationJob = null;
		}
		if(forumNotificationJob != null)
		{
			forumNotificationJob.stop();
			forumNotificationJob = null;
		}
	}
}