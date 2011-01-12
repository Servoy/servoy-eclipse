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

package com.servoy.eclipse.marketplace;

import java.net.URI;
import java.net.URL;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.equinox.p2.core.IProvisioningAgent;
import org.eclipse.equinox.p2.query.QueryUtil;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepository;
import org.eclipse.equinox.p2.repository.metadata.IMetadataRepositoryManager;
import org.eclipse.equinox.p2.ui.ProvisioningUI;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.swt.widgets.Display;
import org.w3c.dom.Node;

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.ui.Activator;

/**
 * Class representing an installable update URL from the Servoy Marketplace
 * @author gabi
 *
 */
public class UpdateURLInstall extends InstallItem
{
	public UpdateURLInstall(Node entryNode)
	{
		super(entryNode);
	}

	@Override
	public void install(final IProgressMonitor monitor) throws Exception
	{
		IProvisioningAgent provisioningAgent = Activator.getDefault().getProvisioningAgent();
		if (provisioningAgent != null)
		{
			IMetadataRepositoryManager manager = (IMetadataRepositoryManager)provisioningAgent.getService(IMetadataRepositoryManager.SERVICE_NAME);
			final URI installURI = new URL(getURL()).toURI();
			manager.addRepository(installURI);

			Display.getDefault().asyncExec(new Runnable()
			{
				public void run()
				{
					ProvisioningUI ui = ProvisioningUI.getDefaultUI();
					try
					{
						IMetadataRepository metadataRepository = ui.loadMetadataRepository(installURI, false, null);
						ui.openInstallWizard(metadataRepository.query(QueryUtil.ALL_UNITS, null).toUnmodifiableSet(), null, null);
					}
					catch (Exception ex)
					{
						MessageDialog.openError(UIUtils.getActiveShell(), "Servoy Marketplace", "Error installing " + UpdateURLInstall.this.getName() +
							".\n\n" + ex.getMessage());
					}
				}
			});
		}
	}
}
