package com.servoy.eclipse.firststeps;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class DialogManager
{
	private static DialogManager INSTANCE = new DialogManager();
	
	public static DialogManager getInstance()
	{
		return INSTANCE;
	}
	
	private BrowserDialog browserDialog;
	
	public void show(Shell parentShell)
	{
		try
		{
			browserDialog = new BrowserDialog(parentShell);
			browserDialog.open();
		}
		catch (Exception e)
		{
			ServoyLog.logError(e);
		}
	}
	
	public void close()
	{
		if(browserDialog != null)
		{
			Display.getDefault().syncExec(new Runnable()
			{
				public void run()
				{
					browserDialog.close();	
				}
			});
		}
	}
	
	class BrowserDialog extends Dialog
	{
		protected BrowserDialog(Shell parentShell)
		{
			super(parentShell);
		}
		
		@Override
		protected Control createDialogArea(Composite parent)
		{
			Composite composite = (Composite)super.createDialogArea(parent);

			GridLayout layout = new GridLayout(1, false);
			composite.setLayout(layout);

			GridData data = new GridData(SWT.FILL, SWT.FILL, true, true);
			data.widthHint = 1280;
			data.heightHint = 850;
			composite.setLayoutData(data);


			Browser browser = new Browser(composite, SWT.NONE);
			String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/firststeps/index.html?template=starting&step=1";
			browser.setUrl(url);
			browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			return composite;
		}

		@Override
		protected void createButtonsForButtonBar(Composite parent)
		{
		}

		@Override
		protected void configureShell(Shell newShell)
		{
			super.configureShell(newShell);
			newShell.setText("First Steps");
		}

		@Override
		public void okPressed()
		{
			close();
		}
	}	
}
