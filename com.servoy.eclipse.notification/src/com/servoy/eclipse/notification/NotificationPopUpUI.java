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

import java.net.URL;
import java.util.ArrayList;


import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.notification.mylyn.AbstractNotificationPopup;
import com.servoy.eclipse.notification.mylyn.ScalingHyperlink;

/**
 * @author gboros
 *
 */
public class NotificationPopUpUI extends AbstractNotificationPopup
{
	private static final int NUM_NOTIFICATIONS_TO_DISPLAY = 6;
	
	private Image servoyLogoImg;
	
	private Color linkColor;
	
	private ArrayList<INotification> notifications;
	
	private OnNotificationClose onCloseCallback;
	
	public NotificationPopUpUI(Display display, ArrayList<INotification> notifications, OnNotificationClose onCloseCallback)
	{
		super(display);
		servoyLogoImg = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/servoy_donut16x16.png").createImage();
		linkColor = new Color(Display.getDefault(), 12, 81, 172);
		this.notifications = notifications;
		this.onCloseCallback = onCloseCallback;
	}
	
	@Override
	protected void createContentArea(Composite parent)
	{
		int count = 0;
		for (final INotification notification : notifications)
		{
			Composite notificationComposite = new Composite(parent, SWT.NO_FOCUS);
			GridLayout gridLayout = new GridLayout(1, false);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(notificationComposite);
			notificationComposite.setLayout(gridLayout);
			notificationComposite.setBackground(parent.getBackground());

			if (count < NUM_NOTIFICATIONS_TO_DISPLAY)
			{
				final ScalingHyperlink itemLink = new ScalingHyperlink(notificationComposite, SWT.NO_FOCUS);
				GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(itemLink);
				itemLink.setForeground(linkColor);
				itemLink.registerMouseTrackListener();
				itemLink.setText(notification.getTitle());
				itemLink.setBackground(parent.getBackground());
				itemLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
						try
						{
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(notification.getLink()));
						}
						catch(Exception ex)
						{
							ServoyLog.logError(ex);
						}
					}
				});

				String descriptionText = null;
				if (notification.getDescription() != null)
				{
					descriptionText = notification.getDescription();
				}
				if (descriptionText != null && !descriptionText.trim().equals("")) //$NON-NLS-1$
				{
					Label descriptionLabel = new Label(notificationComposite, SWT.NO_FOCUS);
					descriptionLabel.setText(descriptionText);
					descriptionLabel.setBackground(parent.getBackground());
					GridDataFactory.fillDefaults()
							.grab(true, false)
							.align(SWT.FILL, SWT.TOP)
							.applyTo(descriptionLabel);
				}
			}
			else
			{
				int numNotificationsRemain = notifications.size() - count;
				ScalingHyperlink remainingLink = new ScalingHyperlink(notificationComposite, SWT.NO_FOCUS);
				remainingLink.setForeground(linkColor);
				remainingLink.registerMouseTrackListener();
				remainingLink.setBackground(parent.getBackground());

				remainingLink.setText(NLS.bind("{0} more", numNotificationsRemain)); //$NON-NLS-1$
				GridDataFactory.fillDefaults().applyTo(remainingLink);
				remainingLink.addHyperlinkListener(new HyperlinkAdapter() {
					@Override
					public void linkActivated(HyperlinkEvent e) {
//						// FIXME
//						//						TasksUiUtil.openTasksViewInActivePerspective().setFocus();
//						IWorkbenchWindow window = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
//						if (window != null) {
//							Shell windowShell = window.getShell();
//							if (windowShell != null) {
//								windowShell.setMaximized(true);
//								windowShell.open();
//							}
//						}
					}
				});
				break;
			}
			count++;
		}
	}	
	

	@Override
	protected String getPopupShellTitle()
	{
		return "Servoy notification";
	}

	@Override
	protected Image getPopupShellImage(int maximumHeight)
	{
		return servoyLogoImg;
	}
	
	@Override
	public boolean close()
	{
		if(servoyLogoImg != null)
		{
			servoyLogoImg.dispose();
		}
		if(linkColor != null)
		{
			linkColor.dispose();
		}
		if(onCloseCallback != null)
		{
			onCloseCallback.onClose();
		}
		
		return super.close();
	}
}