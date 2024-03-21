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

import org.eclipse.core.commands.Command;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.osgi.util.NLS;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.StackLayout;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Event;
import org.eclipse.swt.widgets.Label;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.commands.ICommandService;
import org.eclipse.ui.forms.events.HyperlinkAdapter;
import org.eclipse.ui.forms.events.HyperlinkEvent;
import org.eclipse.ui.handlers.IHandlerService;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.notification.mylyn.AbstractNotificationPopup;
import com.servoy.eclipse.notification.mylyn.ScalingHyperlink;

/**
 * @author gboros
 *
 */
public class NotificationPopUpUI extends AbstractNotificationPopup
{
	private static final int NUM_NOTIFICATIONS_TO_DISPLAY = 4;
	
	private Image servoyLogoImg;
	
	private Color linkColor;
	
	private ArrayList<INotification> notifications;
	
	private OnNotificationClose onCloseCallback;
	
	private String title;
	
	private boolean escapeHtmlTags;
	
	public NotificationPopUpUI(Display display, ArrayList<INotification> notifications, OnNotificationClose onCloseCallback)
	{
		this("Servoy notification", false, display, notifications, onCloseCallback);
	}
	
	public NotificationPopUpUI(
		String title, boolean escapeHtmlTags, Display display, ArrayList<INotification> notifications, OnNotificationClose onCloseCallback)
	{
		super(display);
		this.title = title;
		this.escapeHtmlTags = escapeHtmlTags;
		servoyLogoImg = Activator.imageDescriptorFromPlugin(Activator.PLUGIN_ID, "icons/windowicon.png").createImage();
		linkColor = Display.getDefault().getSystemColor(SWT.COLOR_LINK_FOREGROUND);
		this.notifications = notifications;
		this.onCloseCallback = onCloseCallback;
	}
	
	private ArrayList<Composite> stacks = new ArrayList<Composite>();
	
	private Composite createStack(Composite parent)
	{
		Composite stack = new Composite(parent, SWT.NO_FOCUS);
		stack.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		GridLayout layout = new GridLayout(1, false);
		layout.marginWidth = 0;
		layout.marginHeight = 0;
		layout.verticalSpacing = 0;
		stack.setLayout(layout);
		
		stacks.add(stack);
		
		return stack;
	}
	
	private Composite createNavigator(Composite parent)
	{
		Composite navigatorComposite = new Composite(parent, SWT.NO_FOCUS);
		GridLayout gridLayout = new GridLayout(2, false);
		GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(navigatorComposite);
		navigatorComposite.setLayout(gridLayout);
		navigatorComposite.setBackground(parent.getBackground());
		
		return navigatorComposite;
	}
	
	@Override
	protected void createContentArea(Composite parent)
	{
		final Composite stacksParent = new Composite(parent, SWT.NO_FOCUS);
		stacksParent.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		final StackLayout layout = new StackLayout();
		stacksParent.setLayout(layout);
		
		Composite stack = createStack(stacksParent);
		
		int count = 1;
		for (final INotification notification : notifications)
		{
			Composite notificationComposite = new Composite(stack, SWT.NO_FOCUS);
			GridLayout gridLayout = new GridLayout(1, false);
			GridDataFactory.fillDefaults().grab(true, false).align(SWT.FILL, SWT.TOP).applyTo(notificationComposite);
			notificationComposite.setLayout(gridLayout);
			notificationComposite.setBackground(parent.getBackground());
						
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
						if(notification.isCommand())
						{
							Command command = PlatformUI.getWorkbench().getService(ICommandService.class).getCommand(notification.getLink());
							ExecutionEvent executionEvent = (PlatformUI.getWorkbench().getService(IHandlerService.class)).createExecutionEvent(command, new Event());
							command.executeWithChecks(executionEvent);							
						}
						else
						{
							PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(notification.getLink()));
						}						
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
				if(this.escapeHtmlTags)
				{
					descriptionText = descriptionText.replaceAll("(?s)<[^>]*>(\\s*<[^>]*>)*", " ");
					if(descriptionText.length() > 50) descriptionText = descriptionText.substring(0, 47) + "...";					
				}
				descriptionLabel.setText(descriptionText);
				
				//descriptionLabel.setBackground(parent.getBackground());
				GridDataFactory.fillDefaults()
					.grab(true, false)
					.align(SWT.FILL, SWT.TOP)
					.applyTo(descriptionLabel);
			}

			boolean hasPrev = count > NUM_NOTIFICATIONS_TO_DISPLAY && ((count % NUM_NOTIFICATIONS_TO_DISPLAY == 0) || (count == notifications.size())); 
			boolean hasNext = count % NUM_NOTIFICATIONS_TO_DISPLAY == 0 && count < notifications.size();
			
			if(hasPrev || hasNext)
			{
				Composite navigatorComposite = createNavigator(notificationComposite);
				if(hasPrev)
				{
					final int prevStackIdx = stacks.size() - 2;
					int numNotificationsNewer = (prevStackIdx + 1) * NUM_NOTIFICATIONS_TO_DISPLAY;
					ScalingHyperlink newerLink = new ScalingHyperlink(navigatorComposite, SWT.NO_FOCUS);
					newerLink.setForeground(linkColor);
					newerLink.registerMouseTrackListener();
					newerLink.setBackground(parent.getBackground());

					newerLink.setText(NLS.bind("<< {0} newer", numNotificationsNewer)); //$NON-NLS-1$
					GridDataFactory.fillDefaults().span(hasNext ? 1 : 2, 1).applyTo(newerLink);

					newerLink.addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							layout.topControl = stacks.get(prevStackIdx);
							stacksParent.layout();
						}
					});
				}			
				
				if(hasNext)
				{
					final int nextStackIdx = count / NUM_NOTIFICATIONS_TO_DISPLAY;
					int numNotificationsRemain = notifications.size() - count;
					ScalingHyperlink remainingLink = new ScalingHyperlink(navigatorComposite, SWT.NO_FOCUS);
					remainingLink.setForeground(linkColor);
					remainingLink.registerMouseTrackListener();
					remainingLink.setBackground(parent.getBackground());

					remainingLink.setText(NLS.bind("{0} more >>", numNotificationsRemain)); //$NON-NLS-1$
					GridDataFactory.fillDefaults().applyTo(remainingLink);
					remainingLink.addHyperlinkListener(new HyperlinkAdapter() {
						@Override
						public void linkActivated(HyperlinkEvent e) {
							layout.topControl = stacks.get(nextStackIdx);
							stacksParent.layout();
						}
					});
					stack = createStack(stacksParent);
				}
			}
			
			count++;
		}
		layout.topControl = stacks.get(0);
	}	
	
	@Override
	protected String getPopupShellTitle()
	{
		return this.title;
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
		if(onCloseCallback != null)
		{
			onCloseCallback.onClose();
		}
		
		return super.close();
	}
}