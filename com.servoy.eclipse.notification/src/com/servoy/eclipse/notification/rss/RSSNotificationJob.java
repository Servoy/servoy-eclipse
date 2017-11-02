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

package com.servoy.eclipse.notification.rss;

import java.net.URL;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.preference.IPersistentPreferenceStore;
import org.eclipse.jface.preference.IPreferenceStore;
import org.eclipse.swt.widgets.Display;

import com.rometools.rome.feed.synd.SyndEntry;
import com.rometools.rome.feed.synd.SyndFeed;
import com.rometools.rome.io.SyndFeedInput;
import com.rometools.rome.io.XmlReader;
import com.servoy.eclipse.notification.Activator;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.notification.INotification;
import com.servoy.eclipse.notification.NotificationPopUpUI;
import com.servoy.eclipse.notification.OnNotificationClose;

/**
 * @author gboros
 *
 */
public class RSSNotificationJob extends Job
{
	private static final long CHECK_INTERVAL = 60000 * 30; // 30 min
	
	private static final String RSS = "https://servoy.com/category/developer-news/feed/";
	
	private boolean running = true;
	
	private static final String PROPERTY_LAST_NOTIFICATION_TIMESTAMP = "lastNotificationTimestamp";
	private Date lastNotificationTimestamp;

	/**
	 * @param name
	 */
	public RSSNotificationJob()
	{
		super("Servoy notification job");
		setSystem(true);
		lastNotificationTimestamp = new Date(Activator.getDefault().getPreferenceStore().getLong(PROPERTY_LAST_NOTIFICATION_TIMESTAMP));
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.core.runtime.jobs.Job#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@Override
	protected IStatus run(IProgressMonitor monitor)
	{
		final ArrayList<INotification> rssNotifications = getRSSNotifications();
		if(rssNotifications.size() > 0)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				@Override
				public void run()
				{
					NotificationPopUpUI notificationPopUpUI = new NotificationPopUpUI(Display.getCurrent(), rssNotifications, new OnNotificationClose() {							
						@Override
						public void onClose()
						{
							schedule(CHECK_INTERVAL);
						}
					});
					notificationPopUpUI.setDelayClose(0);
					notificationPopUpUI.open();
				}
			});
		}
		else
		{
			schedule(CHECK_INTERVAL);
		}

		return Status.OK_STATUS;
	}

	private ArrayList<INotification> getRSSNotifications()
	{
		ArrayList<INotification> rssNotifications = new ArrayList<INotification>();
		
		try
		{
			SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(RSS)));		
			Iterator<SyndEntry> feedEntriesIte = feed.getEntries().iterator();
			Date topNotificationTimestamp = null;
			while(feedEntriesIte.hasNext())
			{
				RSSNotification notification = new RSSNotification(feedEntriesIte.next());
				Date notificationDate = notification.getDate();
				if(notificationDate == null)
				{
					notificationDate = feed.getPublishedDate();
				}
				if(notificationDate != null)
				{
					if(lastNotificationTimestamp != null && (notificationDate.equals(lastNotificationTimestamp) || notificationDate.before(lastNotificationTimestamp)))
					{
						break;	
					}
					if(topNotificationTimestamp == null)
					{
						topNotificationTimestamp = notificationDate;
					}
				}
				rssNotifications.add(notification);
			}
			if(topNotificationTimestamp != null)
			{
				lastNotificationTimestamp = topNotificationTimestamp;
				IPreferenceStore pref = Activator.getDefault().getPreferenceStore();
				pref.setValue(PROPERTY_LAST_NOTIFICATION_TIMESTAMP, lastNotificationTimestamp.getTime());
				if(pref instanceof IPersistentPreferenceStore) // save it asap if possible
				{
					try
					{
						((IPersistentPreferenceStore)pref).save();
					}
					catch(Exception ex)
					{
						ServoyLog.logError("Error saving notification timestamp", ex);
					}
				}
			}
		}
		catch(Exception ex)
		{
			ServoyLog.logError("Error getting RSS notifications", ex);			
		}
		
		return rssNotifications;
	}
	
	@Override
	public boolean shouldSchedule()
	{
		return running;
	}

	public void stop()
	{
		running = false;
	}
}