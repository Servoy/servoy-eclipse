package com.servoy.eclipse.core.extension;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.eclipse.jface.dialogs.MessageDialog;

import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.extension.ExtensionUtils;
import com.servoy.extension.FileBasedExtensionProvider;
import com.servoy.extension.Message;
import com.servoy.extension.dependency.InstallStep;
import com.servoy.extension.dependency.MaxVersionLibChooser;
import com.servoy.extension.install.CopyZipEntryImporter;
import com.servoy.extension.install.LibActivationHandler;
import com.servoy.extension.install.LibChoiceHandler;
import com.servoy.extension.install.UninstallZipEntries;
import com.servoy.extension.parser.EXPParser;
import com.servoy.extension.parser.ExtensionConfiguration;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;

public class ProcessPendingInstall implements Runnable
{

	private static ProcessPendingInstall needsUserAttention;
	private final List<Message> allMessages = new ArrayList<Message>();
	private String error = null;

	public static ProcessPendingInstall getAndClearUINeedingInstance()
	{
		ProcessPendingInstall tmp = needsUserAttention;
		needsUserAttention = null;
		return tmp;
	}

	public void run()
	{
		File installDir = getInstallDir();
		File f = new File(new File(installDir, ExtensionUtils.EXPFILES_FOLDER), InstalledWithPendingExtensionProvider.PENDING_FOLDER);

		if (f.exists())
		{
			continueInstall(installDir);
		}
	}

	protected File getInstallDir()
	{
		// we are not using ApplicationServerSingleton.get().getServoyApplicationServerDirectory() cause that would init the app. server and load all plugins, beans...
		// which negates the purpose of this restart-install
		File f = null;
		String location = System.getProperty("servoy.application_server.dir"); //$NON-NLS-1$
		if (location == null)
		{
			location = System.getProperty("eclipse.home.location"); //$NON-NLS-1$
			if (location != null && location.startsWith("file:")) //$NON-NLS-1$
			{
				location = location.substring(5) + "../"; //$NON-NLS-1$
				if (location != null && Utils.getPlatform() == Utils.PLATFORM_WINDOWS)
				{
					if (location.startsWith("/")) location = location.substring(1); //$NON-NLS-1$
					location = location.replaceAll("/", "\\\\"); //$NON-NLS-1$//$NON-NLS-2$
				}
				f = new File(location);
			}
		}
		else
		{
			f = new File(location + "../"); //$NON-NLS-1$
		}

		if (f != null && f.exists()) return f;

		// should never happen
		RuntimeException ex = new RuntimeException("eclipseLocation='" + location + '\''); //$NON-NLS-1$
		ServoyLog.logError("Could not determine servoy base location", ex); //$NON-NLS-1$ 
		MessageDialog.openError(null, "Install", "A pending installation has failed: base location unknown."); //$NON-NLS-1$//$NON-NLS-2$
		throw ex;
	}

	public void continueInstall(File installDir)
	{
		RestartState state = new RestartState();
		state.installDir = installDir;

		File extDir = new File(state.installDir, ExtensionUtils.EXPFILES_FOLDER);
		if (!extDir.exists()) extDir.mkdir();
		if (extDir.exists() && extDir.canRead() && extDir.isDirectory())
		{
			state.installedExtensionsProvider = new FileBasedExtensionProvider(extDir, true, state);
		}

		if (state.installedExtensionsProvider != null)
		{
			try
			{
				File[] pendingDirs = InstalledWithPendingExtensionProvider.getPendingDirsAscending(extDir);

				for (int i = 0; i < pendingDirs.length && error == null; i++) // ascending
				{
					error = state.recreateFromPending(pendingDirs[i]);

					if (error == null)
					{
						// start installing!
						doInstall(state);
					}
				}
			}
			finally
			{
				FileUtils.deleteQuietly(new File(extDir, InstalledWithPendingExtensionProvider.PENDING_FOLDER));
			}
		}
		else
		{
			// should never happen
			error = "Cannot access installed extensions."; //$NON-NLS-1$
		}

		if (error != null || allMessages.size() > 0)
		{
			// we need to show errors/warnings/info to the user later on, when UI starts...
			needsUserAttention = this;
		}
	}

	protected void doInstall(RestartState state)
	{
		for (InstallStep step : state.chosenPath.installSequence)
		{
			if (step.type == InstallStep.INSTALL)
			{
				File f = state.extensionProvider.getEXPFile(step.extension.id, step.extension.version, null);

				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();

				// default install
				CopyZipEntryImporter defaultInstaller = new CopyZipEntryImporter(f, state.installDir, step.extension.id, step.extension.version, whole);
				defaultInstaller.handleFile();
				allMessages.addAll(Arrays.asList(defaultInstaller.getMessages()));
			}
			else if (step.type == InstallStep.UNINSTALL)
			{
				File f = state.installedExtensionsProvider.getEXPFile(step.extension.id, step.extension.installedVersion, null);

				EXPParser parser = state.getOrCreateParser(f);
				ExtensionConfiguration whole = parser.parseWholeXML();

				UninstallZipEntries uninstaller = new UninstallZipEntries(f, state.installDir, step.extension.id, step.extension.installedVersion, whole);
				uninstaller.handleFile();

				allMessages.addAll(Arrays.asList(uninstaller.getMessages()));
			}
			else
			{
				// should never happen; if it does it's an implementation error
				allMessages.add(new Message("Internal error [uist]...", Message.ERROR)); //$NON-NLS-1$
				ServoyLog.logError("Unknown install step type...", null); //$NON-NLS-1$
			}
		}

		if (state.chosenPath.libChoices != null)
		{
			LibChoiceHandler libHandler = new LibChoiceHandler(state.installedExtensionsProvider, state.extensionProvider, state);
			LibActivationHandler activator = new LibActivationHandler(state.installDir);
			libHandler.handleChoices(state.chosenPath.libChoices, new MaxVersionLibChooser(), activator);
			allMessages.addAll(Arrays.asList(libHandler.getMessages()));
			allMessages.addAll(Arrays.asList(activator.getMessages()));
		}
	}

	public Pair<String, Message[]> getErrorAndMessages()
	{
		return new Pair<String, Message[]>(error, allMessages.toArray(new Message[allMessages.size()]));
	}

}