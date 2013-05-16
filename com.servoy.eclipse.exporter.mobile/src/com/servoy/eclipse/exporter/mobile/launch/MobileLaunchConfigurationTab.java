package com.servoy.eclipse.exporter.mobile.launch;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.commons.collections.BidiMap;
import org.apache.commons.collections.bidimap.DualHashBidiMap;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.debug.core.ILaunchConfiguration;
import org.eclipse.debug.core.ILaunchConfigurationWorkingCopy;
import org.eclipse.debug.ui.AbstractLaunchConfigurationTab;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.internal.browser.BrowserManager;
import org.eclipse.ui.internal.browser.IBrowserDescriptor;
import org.eclipse.ui.internal.browser.Messages;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

public class MobileLaunchConfigurationTab extends AbstractLaunchConfigurationTab
{
	// CONFIG_INVALID is only used in this view , it should not be possible to create an invalid launch configuration from solex view.
	private static final String CONFIG_INVALID = "servoy.mobile.launchconfig.invalid";
	String defaultServerURL = "http://localhost:8080";

	// user visible browser name
	static final String FIREFOX = "Mozilla firefox";
	static final String CHROME = "Google Chrome";
	static final String IE = "Internet Explorer";
	static final String SAFARI = "Apple Safari";
	static final String OPERA = "Opera";
	static final String DEFAULT = "Default System browser";

	protected static BidiMap possibleBrowsersNames = new DualHashBidiMap();
	static
	{
		possibleBrowsersNames.put("org.eclipse.ui.browser.ie", IE);
		possibleBrowsersNames.put("org.eclipse.ui.browser.chrome", CHROME);
		possibleBrowsersNames.put("org.eclipse.ui.browser.firefox", FIREFOX);
		possibleBrowsersNames.put("org.eclipse.ui.browser.safari", SAFARI);
		possibleBrowsersNames.put("org.eclipse.ui.browser.opera", OPERA);
		possibleBrowsersNames.put("default", DEFAULT);
	}

	protected Label lblSolution;
	protected Label lblSolutionname;
	protected Label lblStartUrl;
	protected Text txtStartURL;
	protected Label lblServerURL;
	protected Text txtServerURL;
	protected Button checkNoDebug;
	protected Label lblNoDebug;
	protected Text txtNoDebugFeedback;

	protected String NODEBUG_CHECK_TXT = "No debug is checked , running this launch configuration will start the service solution without switching to it as an active solution";
	protected String NODEBUG_UNCHECK_TXT = "running this launch configuration will switch to service solution as active solution.";

	protected Label lblBrowser;
	protected Combo combo;
	protected String[] browserList = null;

	private final ModifyListener modifyListener = new ModifyListener()
	{
		public void modifyText(ModifyEvent e)
		{
			setDirty(true);
			updateLaunchConfigurationDialog();
		}
	};


	protected String[] getAvailableBrowsers()
	{
		ArrayList<String> browserList = new ArrayList<String>();
		Iterator iterator = BrowserManager.getInstance().getWebBrowsers().iterator();
		try
		{
			while (iterator.hasNext())
			{
				final IBrowserDescriptor ewb = (IBrowserDescriptor)iterator.next();
				org.eclipse.ui.internal.browser.IBrowserExt ext = null;
				if (ewb != null && !ewb.getName().equals(Messages.prefSystemBrowser))
				{
					//ext := "org.eclipse.ui.browser." + specifiId 
					ext = org.eclipse.ui.internal.browser.WebBrowserUIPlugin.findBrowsers(ewb.getLocation());
					if (ext != null)
					{
						browserList.add((String)possibleBrowsersNames.get(ext.getId()));
					}
					else
					{
						if (ewb.getLocation() != null)
						{
							String id = ewb.getName().toLowerCase().replace(" ", "_");
							browserList.add((String)possibleBrowsersNames.get("org.eclipse.ui.browser." + id));
						}
					}
				}
				else if (ewb != null && ewb.getName().equals(Messages.prefSystemBrowser))
				{
					// default system browser					
					browserList.add((String)possibleBrowsersNames.get("default"));
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return browserList.toArray(new String[0]);

	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NULL);
		setControl(container);
		parent.setSize(new Point(500, 300));

		lblSolution = new Label(container, SWT.NONE);
		lblSolution.setText("Solution");

		lblSolutionname = new Label(container, SWT.NONE);
		lblSolutionname.setText("SolutionName");

		lblStartUrl = new Label(container, SWT.NONE);
		lblStartUrl.setText("Start URL");

		txtStartURL = new Text(container, SWT.BORDER);
		txtStartURL.setText("http://localhost:8080");
		txtStartURL.addModifyListener(modifyListener);

		lblServerURL = new Label(container, SWT.NONE);
		lblServerURL.setText("Application Server Url");

		txtServerURL = new Text(container, SWT.BORDER);
		txtServerURL.setText("http://localhost:8080");
		txtServerURL.addModifyListener(modifyListener);

		txtNoDebugFeedback = new Text(container, SWT.WRAP | SWT.MULTI);
		txtNoDebugFeedback.setEditable(false);
		txtNoDebugFeedback.setText(NODEBUG_CHECK_TXT);

		checkNoDebug = new Button(container, SWT.CHECK);
		lblNoDebug = new Label(container, SWT.NONE);
		lblNoDebug.setText("No Debug");
		checkNoDebug.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				Button button = (Button)e.widget;
				if (button.getSelection())
				{
					String startURL = txtStartURL.getText();
					int index = startURL.indexOf("?nodebug=true");
					if (index == -1 && startURL.indexOf("?") == -1)
					{
						startURL += "?nodebug=true";
						txtStartURL.setText(startURL);
					}
					txtNoDebugFeedback.setText(NODEBUG_CHECK_TXT);
				}
				else
				{
					String startURL = txtStartURL.getText();
					int index = startURL.indexOf("?nodebug=true");
					if (index + "?nodebug=true".length() == startURL.length())
					{
						txtStartURL.setText(startURL.substring(0, startURL.length() - "?nodebug=true".length()));
					}
					txtNoDebugFeedback.setText(NODEBUG_UNCHECK_TXT);
				}
				setDirty(true);
				updateLaunchConfigurationDialog();
				super.widgetSelected(e);
			}
		});

		lblBrowser = new Label(container, SWT.NONE);
		lblBrowser.setToolTipText("You can define external browsers by going to : Window->Preferences ->General ->Web  Bowser");
		lblBrowser.setText("Browser");

		combo = new Combo(container, SWT.NONE);
		browserList = getAvailableBrowsers();
		combo.setItems(browserList);
		combo.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(SelectionEvent e)
			{
				setDirty(true);
				updateLaunchConfigurationDialog();
				super.widgetSelected(e);
			}
		});


		GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(lblNoDebug).add(lblBrowser).add(lblStartUrl).add(lblServerURL).add(lblSolution)).add(
				18).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createSequentialGroup().add(checkNoDebug).addPreferredGap(LayoutStyle.RELATED).add(txtNoDebugFeedback,
						GroupLayout.PREFERRED_SIZE, 345, Short.MAX_VALUE)).add(txtStartURL, GroupLayout.PREFERRED_SIZE, 345, Short.MAX_VALUE).add(txtServerURL,
					GroupLayout.PREFERRED_SIZE, 345, Short.MAX_VALUE).add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblSolutionname, GroupLayout.PREFERRED_SIZE, 345, Short.MAX_VALUE)).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(lblSolutionname).add(lblSolution)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(txtStartURL, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE).add(lblStartUrl)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(lblServerURL).add(txtServerURL, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(lblBrowser).add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout.createParallelGroup(GroupLayout.BASELINE).add(lblNoDebug).add(checkNoDebug)).add(txtNoDebugFeedback,
					GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)).addContainerGap(92, Short.MAX_VALUE)));
		container.setLayout(groupLayout);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		final ServoyProject activeProject = servoyModel.getActiveProject();

		configuration.setAttribute(IMobileLaunchConstants.SOLUTION_NAME, activeProject.getSolution().getName());
		configuration.setAttribute(IMobileLaunchConstants.SERVER_URL, "http://localhost:8080");
		configuration.setAttribute(IMobileLaunchConstants.APPLICATION_URL, "http://localhost:8080?nodebug=true");
		configuration.setAttribute(IMobileLaunchConstants.NODEBUG, "true");
		configuration.setAttribute(IMobileLaunchConstants.BROWSER_ID, "org.eclipse.ui.browser.chrome");
	}


	@Override
	public void initializeFrom(ILaunchConfiguration configuration)
	{

		try
		{
			lblSolutionname.setText(configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "not defined"));
			txtServerURL.setText(configuration.getAttribute(IMobileLaunchConstants.SERVER_URL, "http://localhost:8080"));
			txtStartURL.setText(configuration.getAttribute(IMobileLaunchConstants.APPLICATION_URL, "http://localhost:8080?nodebug=true"));
			checkNoDebug.setSelection(Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")));
			String browserId = configuration.getAttribute(IMobileLaunchConstants.BROWSER_ID, "default");
			String browserName = (String)possibleBrowsersNames.get(browserId);
			int comboIndexToSelect = Arrays.asList(browserList).indexOf(browserName);
			combo.select(comboIndexToSelect == -1 ? 0 : comboIndexToSelect);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

	}


	@Override
	public void performApply(ILaunchConfigurationWorkingCopy configuration)
	{
		configuration.setAttribute(IMobileLaunchConstants.SOLUTION_NAME, lblSolutionname.getText());
		configuration.setAttribute(IMobileLaunchConstants.SERVER_URL, txtServerURL.getText());
		configuration.setAttribute(IMobileLaunchConstants.APPLICATION_URL, txtStartURL.getText());
		configuration.setAttribute(IMobileLaunchConstants.NODEBUG, Boolean.toString(checkNoDebug.getSelection()));
		String browserId = (String)possibleBrowsersNames.getKey(browserList[combo.getSelectionIndex() == -1 ? 0 : combo.getSelectionIndex()]);
		configuration.setAttribute(IMobileLaunchConstants.BROWSER_ID, browserId);
	}

	@Override
	public String getName()
	{
		return "Mobile Client Configuration";
	}


	@Override
	public boolean isValid(ILaunchConfiguration launchConfig)
	{
		return true;
	}

}