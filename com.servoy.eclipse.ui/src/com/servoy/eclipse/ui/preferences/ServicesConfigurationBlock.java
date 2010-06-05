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
package com.servoy.eclipse.ui.preferences;

import java.io.InputStream;
import java.net.URL;
import java.rmi.registry.Registry;

import org.eclipse.dltk.ui.preferences.AbstractConfigurationBlock;
import org.eclipse.dltk.ui.preferences.OverlayPreferenceStore;
import org.eclipse.jface.preference.PreferencePage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.eclipse.core.ServoyLog;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.Utils;

public class ServicesConfigurationBlock extends AbstractConfigurationBlock
{
	private Text sslPassphrase;

	private Text sslFilename;

	private Text rmiStartPort;

	private Text hostname;

	private Text httpPort;

	private Button twoWaySupport;

	private Button sslSupport;

	private Button startServices;

	public ServicesConfigurationBlock(OverlayPreferenceStore store, PreferencePage mainPreferencePage)
	{
		super(store, mainPreferencePage);
	}

	public Control createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite composite = new Composite(parent, SWT.NONE);

		final Label labelIsRunning;
		labelIsRunning = new Label(composite, SWT.NONE);
		labelIsRunning.setText("HttpService is not tested");

		Label httpPortLabel;
		httpPortLabel = new Label(composite, SWT.NONE);
		httpPortLabel.setText("Http Port");

		Label dataServiceFirewallLabel;
		dataServiceFirewallLabel = new Label(composite, SWT.NONE);
		dataServiceFirewallLabel.setText("Data service, firewall hostname or IP (for external usage)");

		Label rmiStartPortLabel;
		rmiStartPortLabel = new Label(composite, SWT.NONE);
		rmiStartPortLabel.setText("RMI start port");

		Label startWithTwowayLabel;
		startWithTwowayLabel = new Label(composite, SWT.NONE);
		startWithTwowayLabel.setText("Start with two-way socket (clients behind firewalls)");

		httpPort = new Text(composite, SWT.BORDER);

		hostname = new Text(composite, SWT.BORDER);

		rmiStartPort = new Text(composite, SWT.BORDER);

		Button testButton;
		testButton = new Button(composite, SWT.NONE);
		testButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				if (testServer())
				{
					labelIsRunning.setText("HttpService is running");
				}
				else
				{
					labelIsRunning.setText("HttpService is not running");
				}
			}
		});
		testButton.setText("Test");

		Label startWithSslLabel;
		startWithSslLabel = new Label(composite, SWT.NONE);
		startWithSslLabel.setText("Start with ssl support");

		twoWaySupport = new Button(composite, SWT.CHECK);

		sslSupport = new Button(composite, SWT.CHECK);

		Label sslKeystoreFilenameLabel;
		sslKeystoreFilenameLabel = new Label(composite, SWT.NONE);
		sslKeystoreFilenameLabel.setText("SSL Keystore filename");

		Label sslKeystorePassphraseLabel;
		sslKeystorePassphraseLabel = new Label(composite, SWT.NONE);
		sslKeystorePassphraseLabel.setText("SSL keystore passphrase");

		Label startDataAndLabel;
		startDataAndLabel = new Label(composite, SWT.NONE);
		startDataAndLabel.setText("Start data and http service in this developer");

		sslFilename = new Text(composite, SWT.BORDER);

		sslPassphrase = new Text(composite, SWT.BORDER);
		sslPassphrase.setEchoChar('*');

		startServices = new Button(composite, SWT.CHECK);

		Label forUsingExternallyLabel;
		forUsingExternallyLabel = new Label(composite, SWT.NONE);
		forUsingExternallyLabel.setText("For using externally, you must forward specified RMI port and http port on the firewall to this machine");
		final GroupLayout groupLayout = new GroupLayout(composite);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			GroupLayout.TRAILING,
			groupLayout.createSequentialGroup().addContainerGap().add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(forUsingExternallyLabel, GroupLayout.DEFAULT_SIZE, 565, Short.MAX_VALUE).add(
					groupLayout.createSequentialGroup().add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(labelIsRunning).add(httpPortLabel).add(dataServiceFirewallLabel).add(
							rmiStartPortLabel).add(startWithSslLabel).add(sslKeystoreFilenameLabel).add(sslKeystorePassphraseLabel).add(startDataAndLabel).add(
							startWithTwowayLabel)).addPreferredGap(LayoutStyle.RELATED).add(
						groupLayout.createParallelGroup(GroupLayout.LEADING).add(startServices).add(sslPassphrase, GroupLayout.DEFAULT_SIZE, 179,
							Short.MAX_VALUE).add(sslFilename, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE).add(sslSupport).add(twoWaySupport).add(
							rmiStartPort, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE).add(httpPort, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE).add(
							hostname, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE).add(testButton, GroupLayout.DEFAULT_SIZE, 179, Short.MAX_VALUE)))).addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout.createSequentialGroup().addContainerGap().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(labelIsRunning).add(testButton)).addPreferredGap(
				LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(httpPortLabel).add(httpPort, GroupLayout.PREFERRED_SIZE, 25,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(dataServiceFirewallLabel).add(hostname, GroupLayout.PREFERRED_SIZE, 25,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(rmiStartPortLabel).add(rmiStartPort, GroupLayout.PREFERRED_SIZE, 25,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(twoWaySupport).add(startWithTwowayLabel)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(startWithSslLabel).add(sslSupport)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(sslKeystoreFilenameLabel).add(sslFilename, GroupLayout.PREFERRED_SIZE, 25,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(sslKeystorePassphraseLabel).add(sslPassphrase, GroupLayout.PREFERRED_SIZE, 25,
					GroupLayout.PREFERRED_SIZE)).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(startDataAndLabel).add(startServices)).addPreferredGap(LayoutStyle.RELATED, 72,
				Short.MAX_VALUE).add(forUsingExternallyLabel).addContainerGap()));
		composite.setLayout(groupLayout);

		return composite;
	}

	/**
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlock#performDefaults()
	 */
	@Override
	public void performDefaults()
	{
		super.performDefaults();

	}

	/**
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlock#initializeFields()
	 */
	@Override
	protected void initializeFields()
	{
		super.initializeFields();

		httpPort.setText(Integer.toString(ApplicationServerSingleton.get().getWebServerPort()));

		Settings settings = ServoyModel.getSettings();
		hostname.setText(settings.getProperty("java.rmi.server.hostname", ""));
		rmiStartPort.setText(settings.getProperty("servoy.rmiStartPort", Integer.toString(Registry.REGISTRY_PORT)));
		twoWaySupport.setSelection(Utils.getAsBoolean(settings.getProperty("SocketFactory.useTwoWaySocket", "true")));
		sslSupport.setSelection(Utils.getAsBoolean(settings.getProperty("SocketFactory.useSSL", "false")));
		startServices.setSelection(Utils.getAsBoolean(settings.getProperty("startServices", "true")));
		sslFilename.setText(settings.getProperty("SocketFactory.SSLKeystorePath", ""));
		sslPassphrase.setText(settings.getProperty("SocketFactory.SSLKeystorePassphrase", ""));

	}

	/**
	 * @see org.eclipse.dltk.ui.preferences.AbstractConfigurationBlock#performOk()
	 */
	@Override
	public void performOk()
	{
		super.performOk();
		String sHost = hostname.getText();

		Settings settings = ServoyModel.getSettings();
		settings.put("java.rmi.server.hostname", sHost);
		settings.put("servoy.rmiStartPort", rmiStartPort.getText());
		settings.put("SocketFactory.useTwoWaySocket", Boolean.toString(twoWaySupport.getSelection()));
		settings.put("SocketFactory.useSSL", Boolean.toString(sslSupport.getSelection()));
		settings.put("startServices", Boolean.toString(startServices.getSelection()));
		settings.put("SocketFactory.SSLKeystorePath", sslFilename.getText());
		settings.put("SocketFactory.SSLKeystorePassphrase", sslPassphrase.getText());
		System.setProperty("java.rmi.server.hostname", sHost);

		try
		{
			ApplicationServerSingleton.get().setWebServerPort(Integer.parseInt(httpPort.getText()));
		}
		catch (Exception e)
		{
			ServoyLog.logError("error parsing http port", e);
		}

	}

	private boolean testServer()
	{
		int port = ApplicationServerSingleton.get().getWebServerPort();

		URL url;
		try
		{
			url = new URL("http", "localhost", port, "/");
			Object o = url.getContent();
			if (o != null && (o instanceof InputStream))
			{
				((InputStream)o).close();
			}
		}
		catch (Exception e)
		{
			Debug.error(e);
			return false;
		}
		return true;
	}
}
