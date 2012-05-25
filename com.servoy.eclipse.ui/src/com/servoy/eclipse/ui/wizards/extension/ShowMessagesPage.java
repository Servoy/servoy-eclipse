/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.wizards.extension;

import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.TableItem;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.ui.util.GrabExcessSpaceIn1ColumnTableListener;
import com.servoy.extension.Message;

/**
 * Customisable page for showing a list of messages to the user. It can allow next or be final, it can allow previous or not.
 * @author acostescu
 */
public class ShowMessagesPage extends WizardPage
{

	protected IWizardPage nextPage;
	protected boolean allowBack;
	protected final UIMessage[] messages;
	protected String[] headers;

	/**
	 * Convenience constructor for when you want to show no messages.
	 */
	public ShowMessagesPage(String pageName, String title, String description, boolean allowBack, IWizardPage nextPage)
	{
		this(pageName, title, description, null, (UIMessage[])null, allowBack, nextPage);
	}

	/**
	 * Creates a new page for showing messages.
	 * @param pageName see super.
	 * @param title the page title.
	 * @param description the description of the page.
	 * @param headers message table header strings.
	 * @param messages the messages to be shown in the messages table.
	 * @param allowBack if false, it will not allow going back to previous pages in the wizard.
	 * @param nextPage can be null or some other page that should be shown meanwhile.
	 */
	public ShowMessagesPage(String pageName, String title, String description, String headers[], Message[] messages, boolean allowBack, IWizardPage nextPage)
	{
		this(pageName, title, description, headers, getUIMessages(messages), allowBack, nextPage);
	}

	/**
	 * Creates a new page for showing messages.
	 * @param pageName see super.
	 * @param title the page title.
	 * @param description the description of the page.
	 * @param headers message table header strings.
	 * @param messages the messages to be shown in the messages table, with custom icons on the first column.
	 * @param allowBack if false, it will not allow going back to previous pages in the wizard.
	 * @param nextPage can be null or some other page that should be shown meanwhile.
	 */
	public ShowMessagesPage(String pageName, String title, String description, String headers[], UIMessage[] messages, boolean allowBack, IWizardPage nextPage)
	{
		super(pageName);
		this.headers = headers;
		this.messages = messages;
		this.nextPage = nextPage;
		this.allowBack = allowBack;

		setTitle(title);
		setDescription(description);
		setPageComplete(nextPage != null);
	}

	protected static UIMessage[] getUIMessages(Message[] textMessages)
	{
		UIMessage[] uiMessages = null;
		Image img;
		if (textMessages != null && textMessages.length > 0)
		{
			uiMessages = new UIMessage[textMessages.length];
			for (int i = textMessages.length - 1; i >= 0; i--)
			{
				switch (textMessages[i].severity)
				{
					case Message.INFO :
						img = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_INFO_TSK);
						break;
					case Message.WARNING :
						img = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_WARN_TSK);
						break;
					case Message.ERROR :
						img = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_OBJS_ERROR_TSK);
						break;
					default :
						img = null;
				}
				uiMessages[i] = new UIMessage(img, new String[] { textMessages[i].message });
			}
		}
		return uiMessages;
	}

	@Override
	public IWizardPage getPreviousPage()
	{
		return allowBack ? super.getPreviousPage() : null;
	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite topLevel = new Composite(parent, SWT.NONE);
		GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
		topLevel.setLayoutData(data);

		GridLayout gl = new GridLayout(1, false);
		gl.marginHeight = gl.marginWidth = 10;
		gl.verticalSpacing = 10;
		topLevel.setLayout(gl);
		setControl(topLevel);

		if (messages != null && messages.length > 0)
		{
			final Table table = new Table(topLevel, SWT.BORDER | SWT.HIDE_SELECTION | SWT.V_SCROLL | SWT.H_SCROLL | SWT.FULL_SELECTION);
			table.setLinesVisible(true);
			table.setHeaderVisible(headers != null);

			final int colCount = messages[0].messages.length + 1;
			final TableColumn[] columns = new TableColumn[colCount];
			for (int i = 0; i < colCount; i++)
			{
				columns[i] = new TableColumn(table, SWT.NONE);
				if (headers != null)
				{
					columns[i].setText(headers[i]);
				}
			}

			for (UIMessage msg : messages)
			{
				TableItem item = new TableItem(table, SWT.NONE);
				item.setImage(0, msg.img);
				for (int i = 1; i < colCount; i++)
				{
					item.setText(i, msg.messages[i - 1]);
				}
			}

			for (int i = 0; i < colCount - 1; i++)
			{
				columns[i].pack();
			}

			table.addControlListener(new GrabExcessSpaceIn1ColumnTableListener(table, colCount - 1));

			data = new GridData(SWT.FILL, SWT.FILL, true, true);
			table.setLayoutData(data);
		}

		createCustomControl(topLevel);
	}

	/**
	 * Assume that parent control has a GridLayout layout with 1 column.
	 * The custom controll will appear after the messages section (if it exists).
	 * @param parent the parent control.
	 */
	protected void createCustomControl(Composite parent)
	{
		// does nothing by default
	}

	@Override
	public IWizardPage getNextPage()
	{
		return nextPage;
	}

	public static class UIMessage
	{
		public final Image img;
		public final String messages[];

		/**
		 * The caller is responsible for disposing img resource if needed.
		 * @param img the icon for the message.
		 * @param message the message.
		 */
		public UIMessage(Image img, String[] messages)
		{
			this.img = img;
			this.messages = messages;
		}
	}

}
