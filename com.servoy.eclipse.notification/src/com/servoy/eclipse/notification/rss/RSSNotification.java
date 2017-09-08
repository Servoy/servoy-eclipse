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

import java.util.Date;

import com.rometools.rome.feed.synd.SyndContent;
import com.rometools.rome.feed.synd.SyndEntry;
import com.servoy.eclipse.notification.INotification;

/**
 * @author gboros
 *
 */
public class RSSNotification implements INotification
{
	private SyndEntry rssEntry;
	
	public RSSNotification(SyndEntry rssEntry)
	{
		this.rssEntry = rssEntry;
	}

	@Override
	public String getTitle()
	{
		return rssEntry.getTitle();
	}

	@Override
	public String getDescription()
	{
		SyndContent description = rssEntry.getDescription();
		return description != null ? description.getValue() : null;
	}

	@Override
	public String getLink()
	{
		return rssEntry.getLink();
	}

	@Override
	public Date getDate()
	{
		return rssEntry.getPublishedDate();
	}
}
