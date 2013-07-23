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
import org.eclipse.swt.events.VerifyEvent;
import org.eclipse.swt.events.VerifyListener;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.layout.FormLayout;
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
import com.servoy.eclipse.exporter.mobile.action.StartMobileClientActionDelegate;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

public class MobileLaunchConfigurationTab extends AbstractLaunchConfigurationTab
{
	// CONFIG_INVALID is only used in this view , it should not be possible to create an invalid launch configuration from solex view.
	private static final String CONFIG_INVALID = "servoy.mobile.launchconfig.invalid";

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
	protected Label lblServiceSolution;
	protected Text txtServiceSolution;
	protected Label lblTimeout;
	protected Text txtTimeout;
	protected Button checkNoDebug;
	protected Label lblNoDebug;
	protected Text txtNoDebugFeedback;

	protected String NODEBUG_CHECK_TXT = "when checked, running this launch configuration will start the service solution without switching to it as an active solution";
	protected String NODEBUG_UNCHECK_TXT = "when unchecked running this launch configuration will switch to service solution as active solution.";

	protected Label lblBrowser;
	protected Combo combo;
	protected String[] browserList = null;
	private Text txtWarDeployTime;

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
		container.setLayout(new FormLayout());

		lblSolution = new Label(container, SWT.NONE);
		lblSolution.setText("Solution");

		lblSolutionname = new Label(container, SWT.NONE);
		lblSolutionname.setText("SolutionName");

		lblStartUrl = new Label(container, SWT.NONE);
		lblStartUrl.setText("Start URL");

		txtStartURL = new Text(container, SWT.BORDER);
		txtStartURL.setText(getDefaultApplicationURL());
		txtStartURL.addModifyListener(modifyListener);

		lblServerURL = new Label(container, SWT.NONE);
		lblServerURL.setText("Application Server Url");

		txtServerURL = new Text(container, SWT.BORDER);
		txtServerURL.setText(IMobileLaunchConstants.DEFAULT_SERVICE_URL);
		txtServerURL.addModifyListener(modifyListener);

		lblServiceSolution = new Label(container, SWT.NONE);
		lblServiceSolution.setText("Service Solution");

		txtServiceSolution = new Text(container, SWT.BORDER);
		txtServiceSolution.addModifyListener(modifyListener);

		lblTimeout = new Label(container, SWT.NONE);
		lblTimeout.setText("Sync Timeout (in seconds)");

		txtTimeout = new Text(container, SWT.BORDER);
		txtTimeout.setText("30");
		txtTimeout.addModifyListener(new ModifyListener()
		{
			@Override
			public void modifyText(ModifyEvent e)
			{
				String txt = ((Text)e.widget).getText();
				if (txt == null || "".equals(txt) || (txt != null && !txt.matches("\\d+")))
				{
					setErrorMessage("No valid timeout specified");
				}
				else
				{
					setErrorMessage(null);
				}
				setDirty(true);
				updateLaunchConfigurationDialog();
			}
		});

		txtNoDebugFeedback = new Text(container, SWT.WRAP | SWT.MULTI);
		txtNoDebugFeedback.setBackground(container.getBackground());
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
					txtNoDebugFeedback.setText(NODEBUG_CHECK_TXT);
				}
				else
				{
					txtNoDebugFeedback.setText(NODEBUG_UNCHECK_TXT);
				}
				setDirty(true);
				updateLaunchConfigurationDialog();
				super.widgetSelected(e);
			}
		});

		lblBrowser = new Label(container, SWT.NONE);
		String toolTip = "You can define external browsers by going to : Window -> Preferences -> General -> Web Bowser";
		lblBrowser.setToolTipText(toolTip);
		lblBrowser.setText("Browser");

		combo = new Combo(container, SWT.NONE);
		browserList = getAvailableBrowsers();
		combo.setItems(browserList);
		combo.setToolTipText(toolTip);
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

		Label separator = new Label(container, SWT.SEPARATOR | SWT.HORIZONTAL);

		Label lblWarDeployTime = new Label(container, SWT.NONE);
		lblWarDeployTime.setText("WAR deployment time");
		toolTip = "The exported servoy mobile .war file will be copied to the output folder (which is normally a deployment folder of the web container (Tomcat)).\nAfter being copied, the launcher will wait for this time to pass before starting the browser - allowing the .war container to discover and re-deploy the mobile application.\n\nPlease increase this time period if you get 404 - NOT Found or old version of the application when the browser opens.";
		lblWarDeployTime.setToolTipText(toolTip);
		txtWarDeployTime = new Text(container, SWT.BORDER);
		txtWarDeployTime.setText(IMobileLaunchConstants.DEFAULT_WAR_DEPLOYMENT_TIME);
		txtWarDeployTime.setToolTipText(toolTip);
		txtWarDeployTime.addModifyListener(modifyListener);
		txtWarDeployTime.addVerifyListener(new VerifyListener()
		{
			@Override
			public void verifyText(VerifyEvent e)
			{
				if (e.text == null || e.text.length() == 0) e.doit = true;
				else
				{
					try
					{
						Integer.valueOf(e.text);
						e.doit = true;
					}
					catch (NumberFormatException ex)
					{
						e.doit = false;
					}
				}
			}
		});
		Label lblWarDeployUnit = new Label(container, SWT.NONE);
		lblWarDeployUnit.setText("(sec)");

		// @formatter:off
		GroupLayout groupLayout = new GroupLayout(container);
		groupLayout.setHorizontalGroup(
			groupLayout.createParallelGroup(GroupLayout.TRAILING)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
						.add(groupLayout.createSequentialGroup()
							.add(groupLayout.createParallelGroup(GroupLayout.TRAILING)
								.add(lblBrowser)
								.add(lblStartUrl)
								.add(lblServerURL)
								.add(lblServiceSolution)
								.add(lblSolution)
								.add(lblTimeout))
							.add(18)
							.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
								.add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
								.add(lblSolutionname, GroupLayout.PREFERRED_SIZE, 364, Short.MAX_VALUE)
								.add(txtServerURL, GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
								.add(txtServiceSolution, GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
								.add(txtStartURL, GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)
								.add(txtTimeout, GroupLayout.DEFAULT_SIZE, 364, Short.MAX_VALUE)))
						.add(separator, GroupLayout.DEFAULT_SIZE, 522, Short.MAX_VALUE)
						.add(groupLayout.createSequentialGroup()
							.add(14)
							.add(groupLayout.createParallelGroup(GroupLayout.TRAILING)
								.add(lblWarDeployTime, GroupLayout.PREFERRED_SIZE, 126, GroupLayout.PREFERRED_SIZE)
								.add(lblNoDebug))
							.add(18)
							.add(groupLayout.createParallelGroup(GroupLayout.LEADING)
								.add(groupLayout.createSequentialGroup()
									.add(txtWarDeployTime, GroupLayout.PREFERRED_SIZE, 66, GroupLayout.PREFERRED_SIZE)
									.addPreferredGap(LayoutStyle.RELATED)
									.add(lblWarDeployUnit, GroupLayout.PREFERRED_SIZE, 26, GroupLayout.PREFERRED_SIZE))
								.add(groupLayout.createSequentialGroup()
									.add(checkNoDebug)
									.addPreferredGap(LayoutStyle.RELATED)
									.add(txtNoDebugFeedback, GroupLayout.PREFERRED_SIZE, 345, GroupLayout.PREFERRED_SIZE)))))
					.addContainerGap())
		);
		groupLayout.setVerticalGroup(
			groupLayout.createParallelGroup(GroupLayout.LEADING)
				.add(groupLayout.createSequentialGroup()
					.addContainerGap()
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(lblSolutionname)
						.add(lblSolution))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(txtStartURL, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(lblStartUrl))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(lblServerURL)
						.add(txtServerURL, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(lblServiceSolution)
						.add(txtServiceSolution, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(lblBrowser)
						.add(combo, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(lblTimeout)
						.add(txtTimeout, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE))
					.add(29)
					.add(separator, GroupLayout.PREFERRED_SIZE, 2, GroupLayout.PREFERRED_SIZE)
					.add(18)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(txtNoDebugFeedback, GroupLayout.PREFERRED_SIZE, 45, GroupLayout.PREFERRED_SIZE)
						.add(checkNoDebug)
						.add(lblNoDebug))
					.addPreferredGap(LayoutStyle.RELATED)
					.add(groupLayout.createParallelGroup(GroupLayout.BASELINE)
						.add(txtWarDeployTime, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE)
						.add(lblWarDeployTime)
						.add(lblWarDeployUnit))
					.addContainerGap(30, Short.MAX_VALUE))
		);
		// @formatter:on
		container.setLayout(groupLayout);
	}

	protected String getDefaultApplicationURL()
	{
		return StartMobileClientActionDelegate.getDefaultApplicationURL(false);
	}

	@Override
	public void setDefaults(ILaunchConfigurationWorkingCopy configuration)
	{
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		final ServoyProject activeProject = servoyModel.getActiveProject();

		configuration.setAttribute(IMobileLaunchConstants.SOLUTION_NAME, activeProject.getSolution().getName());
		configuration.setAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, activeProject.getSolution().getName() + "_service");
		configuration.setAttribute(IMobileLaunchConstants.SERVER_URL, IMobileLaunchConstants.DEFAULT_SERVICE_URL);
		configuration.setAttribute(IMobileLaunchConstants.APPLICATION_URL, getDefaultApplicationURL());
		configuration.setAttribute(IMobileLaunchConstants.TIMEOUT, IMobileLaunchConstants.DEFAULT_TIMEOUT);
		configuration.setAttribute(IMobileLaunchConstants.NODEBUG, "true");
		configuration.setAttribute(IMobileLaunchConstants.BROWSER_ID, "org.eclipse.ui.browser.chrome");
		configuration.setAttribute(IMobileLaunchConstants.WAR_DEPLOYMENT_TIME, IMobileLaunchConstants.DEFAULT_WAR_DEPLOYMENT_TIME);
	}

	@Override
	public void initializeFrom(ILaunchConfiguration configuration)
	{
		try
		{
			String solutionName = configuration.getAttribute(IMobileLaunchConstants.SOLUTION_NAME, "not defined");
			lblSolutionname.setText(solutionName);
			txtServerURL.setText(configuration.getAttribute(IMobileLaunchConstants.SERVER_URL, IMobileLaunchConstants.DEFAULT_SERVICE_URL));
			txtStartURL.setText(configuration.getAttribute(IMobileLaunchConstants.APPLICATION_URL, getDefaultApplicationURL()));
			txtTimeout.setText(configuration.getAttribute(IMobileLaunchConstants.TIMEOUT, IMobileLaunchConstants.DEFAULT_TIMEOUT));
			txtServiceSolution.setText(configuration.getAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, solutionName + "_service"));
			checkNoDebug.setSelection(Boolean.valueOf(configuration.getAttribute(IMobileLaunchConstants.NODEBUG, "true")).booleanValue());
			String browserId = configuration.getAttribute(IMobileLaunchConstants.BROWSER_ID, "default");
			String browserName = (String)possibleBrowsersNames.get(browserId);
			int comboIndexToSelect = Arrays.asList(browserList).indexOf(browserName);
			combo.select(comboIndexToSelect == -1 ? 0 : comboIndexToSelect);
			txtWarDeployTime.setText(configuration.getAttribute(IMobileLaunchConstants.WAR_DEPLOYMENT_TIME, IMobileLaunchConstants.DEFAULT_WAR_DEPLOYMENT_TIME));
			if (txtWarDeployTime.getText().length() == 0) txtWarDeployTime.setText(IMobileLaunchConstants.DEFAULT_WAR_DEPLOYMENT_TIME);
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
		configuration.setAttribute(IMobileLaunchConstants.SERVICE_SOLUTION, txtServiceSolution.getText());
		configuration.setAttribute(IMobileLaunchConstants.APPLICATION_URL, txtStartURL.getText());
		configuration.setAttribute(IMobileLaunchConstants.TIMEOUT, txtTimeout.getText());
		configuration.setAttribute(IMobileLaunchConstants.NODEBUG, Boolean.toString(checkNoDebug.getSelection()));
		String browserId = (String)possibleBrowsersNames.getKey(browserList[combo.getSelectionIndex() == -1 ? 0 : combo.getSelectionIndex()]);
		configuration.setAttribute(IMobileLaunchConstants.BROWSER_ID, browserId);
		configuration.setAttribute(IMobileLaunchConstants.WAR_DEPLOYMENT_TIME, txtWarDeployTime.getText());
	}

	@Override
	public String getName()
	{
		return "Mobile Client Configuration";
	}


	@Override
	public boolean isValid(ILaunchConfiguration launchConfig)
	{
		String txt = txtTimeout.getText();
		if (txt == null || "".equals(txt) || (txt != null && !txt.matches("\\d+")))
		{
			return false;
		}
		return true;
	}

}