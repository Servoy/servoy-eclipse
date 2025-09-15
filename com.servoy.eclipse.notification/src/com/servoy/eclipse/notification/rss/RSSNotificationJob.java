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

import java.io.FileNotFoundException;
import java.net.URL;
import java.net.UnknownHostException;
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
	private boolean running = true;
	private String title;
	private String rss;
	private boolean isHtmlContent;
	private long checkInterval;
	private String propertyLastNotificationTimestamp;
	private long lastNotificationTimestamp;

	/**
	 * @param name
	 */
	public RSSNotificationJob(
		String title,
		String rss,
		boolean isHtmlContent,
		long checkInterval,
		String propertyLastNotificationTimestamp)
	{
		super(title + " job");
		setSystem(true);
		this.title = title;
		this.rss = rss;
		this.isHtmlContent = isHtmlContent;
		this.checkInterval = checkInterval;
		this.propertyLastNotificationTimestamp = propertyLastNotificationTimestamp;
		lastNotificationTimestamp = Activator.getDefault().getPreferenceStore().getLong(this.propertyLastNotificationTimestamp);
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
					NotificationPopUpUI notificationPopUpUI = new NotificationPopUpUI(
						RSSNotificationJob.this.title,
						RSSNotificationJob.this.isHtmlContent,
						Display.getCurrent(), rssNotifications, new OnNotificationClose() {							
						@Override
						public void onClose()
						{
							schedule(RSSNotificationJob.this.checkInterval);
						}
					});
					notificationPopUpUI.setDelayClose(0);
					notificationPopUpUI.open();
				}
			});
		}
		else
		{
			schedule(this.checkInterval);
		}

		return Status.OK_STATUS;
	}

	private ArrayList<INotification> getRSSNotifications()
	{
		ArrayList<INotification> rssNotifications = new ArrayList<INotification>();
		
		try
		{
			SyndFeed feed = new SyndFeedInput().build(new XmlReader(new URL(this.rss)));		
			Iterator<SyndEntry> feedEntriesIte = feed.getEntries().iterator();
			long topNotificationTimestamp = 0;
			while(feedEntriesIte.hasNext())
			{
				RSSNotification notification = new RSSNotification(feedEntriesIte.next());
				long notificationTimestamp = getNotificationTimestamp(feed, notification);  
				if(notificationTimestamp != 0)
				{
					if(lastNotificationTimestamp != 0 && notificationTimestamp <= lastNotificationTimestamp)
					{
						break;	
					}
					if(topNotificationTimestamp == 0)
					{
						topNotificationTimestamp = notificationTimestamp;
					}
				}
				rssNotifications.add(notification);
			}
			if(topNotificationTimestamp != 0)
			{
				lastNotificationTimestamp = topNotificationTimestamp;
				IPreferenceStore pref = Activator.getDefault().getPreferenceStore();
				pref.setValue(this.propertyLastNotificationTimestamp, lastNotificationTimestamp);
				if(pref instanceof IPersistentPreferenceStore) // save it asap if possible
				{
					try
					{
						((IPersistentPreferenceStore)pref).save();
					}
					catch(Exception ex)
					{
						ServoyLog.logError("Error saving notification timestamp for '" + this.title + "'", ex);
					}
				}
			}
		}
		catch (UnknownHostException e)
		{
			ServoyLog.logInfo("Cannot get RSS notifications for '" + this.title + "'. It's likely that either the developer or the remote site is offline: " + e.getLocalizedMessage());
		}
		catch (FileNotFoundException e)
		{
			// TODO DELETE THIS CATCH; IT WAS ONLY ADDED TO AVOID EXCEPTIONS IN THE LOG WHEN DEBUGGING OLD CODE THAT WAS LOOKING FOR OBSOLETE FORMUL CONTENT
			ServoyLog.logInfo("Cannot get RSS notifications for '" + this.title + "'. It's likely that either the developer or the remote site is offline: " + e.getLocalizedMessage());
		}
		catch (Exception ex)
		{
			ServoyLog.logError("Error getting RSS notifications for '" + this.title + "'", ex);			
		}
		
		return rssNotifications;
	}
	
	
	protected long getNotificationTimestamp(SyndFeed feed, RSSNotification notification)
	{
		Date notificationDate = notification.getDate();
		if(notificationDate == null)
		{
			notificationDate = feed.getPublishedDate();
		}	
		return notificationDate != null ? notificationDate.getTime() : 0;	
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