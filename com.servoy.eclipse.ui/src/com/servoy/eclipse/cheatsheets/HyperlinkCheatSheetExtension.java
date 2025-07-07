package com.servoy.eclipse.cheatsheets;

import java.net.MalformedURLException;
import java.net.URL;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.cheatsheets.AbstractItemExtensionElement;

import com.servoy.eclipse.model.util.ServoyLog;

public class HyperlinkCheatSheetExtension extends AbstractItemExtensionElement
{
	private String type;
	private String value;

	public HyperlinkCheatSheetExtension(String attributeName)
	{
		super(attributeName);
	}

	@Override
	public void handleAttribute(String attributeValue)
	{
		if (attributeValue != null)
		{
			int separatorIdx = attributeValue.indexOf(":");
			if (separatorIdx != -1)
			{
				type = attributeValue.substring(0, separatorIdx);
				if (attributeValue.length() > separatorIdx + 1)
				{
					value = attributeValue.substring(separatorIdx + 1);
				}
			}
		}
	}

	@Override
	public void createControl(Composite composite)
	{
		if ("link".equals(type))
		{
			Link link = new Link(composite, SWT.NONE);
			link.setText(value);
			link.setSize(400, 100);
			link.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(SelectionEvent e)
				{
					try
					{
						//  Open default external browser
						PlatformUI.getWorkbench().getBrowserSupport().getExternalBrowser().openURL(new URL(e.text));
					}
					catch (PartInitException | MalformedURLException ex)
					{
						ServoyLog.logError(ex);
					}
				}
			});
		}
		else if ("bigimage".equals(type))
		{
			Label label = new Label(composite, SWT.NONE);
			label.setImage(com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle(value));
		}
		else
		{
			Label label = new Label(composite, SWT.NONE);
			label.setImage(com.servoy.eclipse.ui.Activator.getDefault().loadImageFromBundle(value));
		}
	}

	@Override
	public void dispose()
	{

	}
}
