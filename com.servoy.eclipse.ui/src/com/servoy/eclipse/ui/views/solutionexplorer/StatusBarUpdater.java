/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.views.solutionexplorer;



import org.eclipse.jface.action.ControlContribution;
import org.eclipse.jface.action.IStatusLineManager;
import org.eclipse.jface.action.StatusLineLayoutData;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.forms.widgets.ScrolledFormText;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.util.Utils;

/**
 * Add the <code>StatusBarUpdater</code> to your ViewPart to have the statusbar describing the selected elements.
 */
public class StatusBarUpdater implements ISelectionChangedListener
{
	private final IStatusLineManager fStatusLineManager;
	private ScrolledFormText statusBarContribution;
	private final ControlContribution controlContribution;

	public StatusBarUpdater(IStatusLineManager statusLineManager)
	{
		fStatusLineManager = statusLineManager;
		controlContribution = new ControlContribution("html_status")
		{

			@Override
			protected Control createControl(Composite parent)
			{
				statusBarContribution = new ScrolledFormText(parent, SWT.V_SCROLL, true)
				{
					/**
					 * @see org.eclipse.swt.widgets.Widget#dispose()
					 */
					@Override
					public void dispose()
					{
						statusBarContribution = null;
						super.dispose();
					}
				};
				StatusLineLayoutData layoutData = new StatusLineLayoutData();
				layoutData.widthHint = 500000; // as wide as permitted by the status bar layout
				layoutData.heightHint = parent.getSize().y;
				statusBarContribution.setLayoutData(layoutData);
				return statusBarContribution;
			}
		};
		fStatusLineManager.add(controlContribution);
	}


	/*
	 * @see ISelectionChangedListener#selectionChanged
	 */
	public void selectionChanged(SelectionChangedEvent event)
	{
		if (statusBarContribution != null)
		{
			String statusBarMessage = formatMessage(event.getSelection());
			try
			{
				statusBarContribution.setText(statusBarMessage);
				fStatusLineManager.setMessage("");
			}
			catch (IllegalArgumentException e)
			{
				// the text contains unsupported HTML... so add it as simple status bar text
				statusBarContribution.setText(formatStandardSupportedMessage(event.getSelection()));
			}
		}
	}


	protected String formatMessage(ISelection sel)
	{
		String result = null;
		if (sel instanceof IStructuredSelection && !sel.isEmpty())
		{
			IStructuredSelection selection = (IStructuredSelection)sel;

			int nElements = selection.size();
			if (nElements <= 1)
			{
				Object selObj = selection.getFirstElement();
				if (selObj instanceof SimpleUserNode)
				{
					result = ((SimpleUserNode)selObj).getToolTipText();
				}
				else
				{
					result = selObj.toString();
				}
			}
		}
		if (result == null)
		{
			result = "<form></form>";
		}
		else
		{
			result = "<form><p>" + result + "</p></form>";
		}
		// as the SWT control uses XHTML, we must change the "<BR>" tags to "<BR/>" ("<BR>" alone is no longer valid in XHTML)
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "<pre>", " "); //$NON-NLS-1$ //$NON-NLS-2$
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "<br>", " "); //$NON-NLS-1$ //$NON-NLS-2$
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "</pre>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "<html>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "</html>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "<body>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		result = Utils.stringReplaceCaseInsensitiveSearch(result, "</body>", ""); //$NON-NLS-1$ //$NON-NLS-2$
		return result;
	}

	protected String formatStandardSupportedMessage(ISelection sel)
	{
		String result = "";
		if (sel instanceof IStructuredSelection && !sel.isEmpty())
		{
			IStructuredSelection selection = (IStructuredSelection)sel;

			int nElements = selection.size();
			if (nElements <= 1)
			{
				Object selObj = selection.getFirstElement();
				if (selObj instanceof SimpleUserNode)
				{
					SimpleUserNode node = (SimpleUserNode)selObj;
					StringBuffer standard = new StringBuffer("<form><p><b>");
					standard.append(node.getName());
					standard.append("</b> ");
					if (node.getType() == UserNodeType.GLOBAL_VARIABLE_ITEM)
					{
						// variable...
						standard.append(Column.getDisplayTypeString(((ScriptVariable)node.getRealObject()).getDataProviderType()));
						standard.append(" defaultvalue is not displayed (it's content is not suitable for the status bar)</p></form>");
					}
					else
					{
						ServoyLog.logWarning("Cannot display status message properly for node " + node.getName(), null);
						standard.append("</p></form>");
					}
					result = standard.toString();
				}
			}
		}
		return result;
	}


	/**
	 * 
	 */
	public void dispose()
	{
		if (statusBarContribution != null)
		{
			statusBarContribution.dispose();
		}
		fStatusLineManager.remove(controlContribution);
	}
}