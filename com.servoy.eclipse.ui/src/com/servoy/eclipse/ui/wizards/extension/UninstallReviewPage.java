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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.wizard.IWizardPage;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.ui.ISharedImages;
import org.eclipse.ui.PlatformUI;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.wizards.extension.ShowMessagesPage.UIMessage;
import com.servoy.extension.DependencyMetadata;
import com.servoy.extension.ExtensionDependencyDeclaration;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.ExtensionUtils.EntryInputStreamRunner;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.ExtensionNode;
import com.servoy.extension.dependency.UninstallDependencyResolver;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.WrappedObjectReference;

/**
 * This page shows information on the extension that is about to be uninstalled.
 * When next is pressed, it will do the actual dependency resolve (with progress) and depending on it's results choose the next page to show.
 * @author acostescu
 */
public class UninstallReviewPage extends ReviewOperationPage
{

	/**
	 * Creates a new dependency resolving page.
	 * @param pageName see super.
	 * @param state the state of the install extension process. It contains the information needed by this page. It will also be filled with info retrieved from this page in order for the wizard to go forward.
	 */
	public UninstallReviewPage(String pageName, InstallExtensionState state)
	{
		super(pageName, "The following extension will be uninstalled:", state); //$NON-NLS-1$
	}

	@Override
	protected void addMoreContent(final Composite topLevel)
	{
		// nothing more needed
	}

	@Override
	protected void fillData()
	{
		DependencyMetadata[] dmds = state.installedExtensionsProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(state.extensionID,
			state.version, state.version));
		if (dmds != null && dmds.length == 1)
		{
			final String name = dmds[0].extensionName;
			if (name != null)
			{
				setDescription(name);
//							getWizard().getContainer().updateTitleBar();
			}
		}

		File f = state.installedExtensionsProvider.getEXPFile(state.extensionID, state.version, null);

		EXPParser parser = state.getOrCreateParser(f);
		final ExtensionConfiguration xml;
		if (parser != null)
		{
			xml = parser.parseWholeXML();
		}
		else
		{
			xml = null;
		}

		if (xml != null && xml.getInfo() != null)
		{
			if (xml.getInfo().description != null || xml.getInfo().url != null)
			{
				if (xml.getInfo().description != null)
				{
					setExtensionDescription(xml.getInfo().description, false);
				}
				if (xml.getInfo().url != null)
				{
					setExtensionProductUrl("<a href=\"" + xml.getInfo().url + "\">" + xml.getInfo().url + "</a>", false); //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$
				}
			}

			if (xml.getInfo().iconPath != null)
			{
				try
				{
					Image image = ExtensionUtils.runOnEntry(f, xml.getInfo().iconPath, new EntryInputStreamRunner<Image>()
					{
						public Image runOnEntryInputStream(InputStream is) throws IOException
						{
							return new Image(state.display, is);
						}
					}).getRight();

					if (image != null)
					{
						state.allocatedImages.add(image);
						// switch back to display thread to update UI
						final Image img = image;
						setExtensionIcon(img, false);
					}
				}
				catch (IOException e)
				{
					// we can't get the image for some reason
					Debug.warn(e);
				}
			}
		}
	}

	@Override
	public IWizardPage getNextPage()
	{
		final WrappedObjectReference<String> failMessage = new WrappedObjectReference<String>(null);
		final WrappedObjectReference<Message[]> warnings = new WrappedObjectReference<Message[]>(null);
		state.chosenPath = null;

		// prepare & start dependency resolving

		// acquire already installed extensions
		if (state.installDir != null)
		{
			try
			{
				getWizard().getContainer().run(true, false, new IRunnableWithProgress()
				{
					public void run(IProgressMonitor monitor) throws InvocationTargetException, InterruptedException
					{
						monitor.beginTask("Preparing to uninstall", 2); //$NON-NLS-1$
						monitor.subTask("Checking installed extensions..."); //$NON-NLS-1$

						if (state.installedExtensionsProvider != null)
						{
							final UninstallDependencyResolver resolver = new UninstallDependencyResolver(state.installedExtensionsProvider);
							monitor.worked(1);

							monitor.subTask("Checking for dependent extensions..."); //$NON-NLS-1$
							resolver.resolveDependencies(state.extensionID, state.version);
							Message[] resolveWarnings = resolver.getMessages();
							if (resolveWarnings.length > 0) Debug.trace(Arrays.asList(resolveWarnings).toString());
							monitor.worked(1);

							state.chosenPath = resolver.getResults();
							if (state.chosenPath == null)
							{
								// no uninstall dependency path found; normally this won't happen
								failMessage.o = "Cannot prepare uninstall. Reason(s):)"; //$NON-NLS-1$
								warnings.o = resolveWarnings;
							}
						}
						else
						{
							// should never happen
							failMessage.o = "Cannot access installed extensions."; //$NON-NLS-1$
						}
						monitor.done();
					}
				});
			}
			catch (InvocationTargetException e)
			{
				ServoyLog.logError(e);
				failMessage.o = e.getMessage();
			}
			catch (InterruptedException e)
			{
				ServoyLog.logError(e);
				failMessage.o = e.getMessage();
			}
		}
		else
		{
			// should never happen
			failMessage.o = "Problem accessing install directory."; //$NON-NLS-1$
		}

		IWizardPage nextPage;
		// show correct next page based on 'failMessage' and 'warnings'
		Message[] expW = state.installedExtensionsProvider.getMessages();
		if (failMessage.o != null || expW.length > 0 || state.chosenPath == null)
		{
			if (failMessage.o == null)
			{
				failMessage.o = "Uninstall preparations failed."; //$NON-NLS-1$
			}
			// show problems page with failMessage as description and warnings as list (which could be null)
			List<Message> allWarnings = new ArrayList<Message>((warnings.o != null ? warnings.o.length : 0) + expW.length);
			if (warnings.o != null) allWarnings.addAll(Arrays.asList(warnings.o));
			allWarnings.addAll(Arrays.asList(expW));

			Message[] messages;
			if (allWarnings.size() > 0)
			{
				messages = allWarnings.toArray(new Message[allWarnings.size()]);
			}
			else
			{
				messages = null;
			}
			nextPage = new ShowMessagesPage("DepWarnings", "Cannot uninstall extension", failMessage.o, null, messages, true, null); //$NON-NLS-1$//$NON-NLS-2$
			nextPage.setWizard(getWizard());
		}
		else
		{
			// uninstall dependency resolving succeeded

			// prepare uninstall page; afterwards, we might postpone it for after some dummy info/warning page
			nextPage = new ActualUninstallPage("DoUninstall", state); //$NON-NLS-1$
			nextPage.setWizard(getWizard());

			if (state.chosenPath.installSequence.length > 1)
			{
				// more extensions are to be installed/replaced (or one is down-graded); tell the user
				UIMessage[] messages;
				Image removeIcon = PlatformUI.getWorkbench().getSharedImages().getImage(ISharedImages.IMG_TOOL_DELETE);

				String[] header = new String[] { "", "Version", "Name", "Id" }; //$NON-NLS-1$//$NON-NLS-2$//$NON-NLS-3$//$NON-NLS-4$
				messages = new UIMessage[state.chosenPath.installSequence.length];
				for (int i = state.chosenPath.installSequence.length - 1; i >= 0; i--)
				{
					ExtensionNode ext = state.chosenPath.installSequence[i].extension;
					DependencyMetadata[] extMeta = state.installedExtensionsProvider.getDependencyMetadata(new ExtensionDependencyDeclaration(ext.id,
						ext.version, ext.version));
					String name = ""; //$NON-NLS-1$
					if (extMeta != null && extMeta.length == 1)
					{
						name = extMeta[0].extensionName;
					}

					messages[i] = new UIMessage(removeIcon, new String[] { ext.installedVersion, name, ext.id });
				}

				nextPage = new ShowMessagesPage(
					"UniReview", "Dependent extensions will be uninstalled as well", "Please review uninstall changes.", header, messages, true, nextPage); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				nextPage.setWizard(getWizard());
			} // else just use nextPage to uninstall

			// check to see if extension provider has any warnings
			if (expW.length > 0)
			{
				// user should know about these; or should we just consider this step failed directly?
				nextPage = new ShowMessagesPage(
					"UniWarnings", "Some items require your attention", "However, you can continue with the uninstall process.", null, expW, true, nextPage); //$NON-NLS-1$//$NON-NLS-2$ //$NON-NLS-3$
				nextPage.setWizard(getWizard());
			}
		}

		return nextPage;
	}
}
