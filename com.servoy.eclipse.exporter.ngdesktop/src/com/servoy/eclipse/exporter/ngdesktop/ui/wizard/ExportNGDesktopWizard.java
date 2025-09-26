package com.servoy.eclipse.exporter.ngdesktop.ui.wizard;

import java.awt.Dimension;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse.ResponseInfo;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.commons.io.IOUtils;
import org.eclipse.jface.dialogs.DialogSettings;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IExportWizard;
import org.eclipse.ui.IWorkbench;
import org.json.JSONException;
import org.json.JSONObject;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.debug.handlers.StartNGDesktopClientHandler;
import com.servoy.eclipse.exporter.ngdesktop.Activator;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;
import com.servoy.j2db.ClientVersion;
import com.servoy.j2db.util.ImageLoader;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.http.StringBodyHandler;

/**
 * @author gboros
 */
public class ExportNGDesktopWizard extends Wizard implements IExportWizard
{
	private ExportPage exportPage;
	public final static int LOGO_SIZE = 256; // KB;
	public final static int IMG_SIZE = 512; // KB;
	public final static int COPYRIGHT_LENGTH = 128; // chars
	public final static int APP_NAME_LENGTH = 20; // chars
	protected static final int STORE_TIMEOUT = 7; //days
	private String service_url = "https://ngdesktop-builder.servoy.com";

	public ExportNGDesktopWizard()
	{
		super();
		setWindowTitle("NG Desktop Export");
		setNeedsProgressMonitor(true);
		final IDialogSettings workbenchSettings = Activator.getDefault().getDialogSettings();
		final ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
		if (activeProject != null)
		{
			final IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings,
				"NGDesktopExportWizard:" + activeProject.getSolution().getName());
			setDialogSettings(section);
		}
		else
		{
			final IDialogSettings section = DialogSettings.getOrCreateSection(workbenchSettings, "NGDesktopExportWizard");
			setDialogSettings(section);
		}

	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
	}

	@Override
	public void createPageControls(Composite pageContainer)
	{
		//CSSSWTConstants.CSS_ID_KEY
		pageContainer.getShell().setData("org.eclipse.e4.ui.css.id", "svydialog");
		super.createPageControls(pageContainer);
	}


	@Override
	public boolean performFinish()
	{
		exportPage.saveState();
		final IDialogSettings exportSettings = this.getDialogSettings();
		final StringBuilder errorMsg = validate(exportSettings);
		if (errorMsg.length() > 0)
		{
			MessageDialog.openError(UIUtils.getActiveShell(), "NG Desktop Export", errorMsg.toString());
			return false;
		}
		final boolean result[] = { false, false };
		ServoyLoginDialog.getLoginToken(loginToken -> {

			if (Utils.stringIsEmpty(loginToken))
			{
				result[1] = true;
				return; //no login
			}

			exportSettings.put("login_token", loginToken);


			errorMsg.delete(0, errorMsg.length());

			try (HttpClient httpClient = HttpClient.newHttpClient())
			{
				exportPage.getSelectedPlatforms().forEach((platform) -> {
					exportSettings.put("platform", platform);

					final String srvAddress = System.getProperty("ngdesktop.service.address");
					if (srvAddress != null && isValidUrl(srvAddress))
						service_url = srvAddress;

					final String input = formatRequest(exportSettings);

					final HttpRequest postRequest = HttpRequest.newBuilder()
						.uri(URI.create(service_url + "/build/start"))
						.header("Content-Type", "application/json")
						.POST(HttpRequest.BodyPublishers.ofString(input))
						.build();

					ServoyLog.logInfo("Send request to " + service_url + "/build/start");

					try
					{
						httpClient.send(postRequest, new StringBodyHandler<Void>()
						{
							@Override
							public Void handleResponse(ResponseInfo responseInfo, String content)
							{
								final int httpStatusCode = responseInfo.statusCode();

								switch (httpStatusCode)
								{
									case HttpURLConnection.HTTP_OK :
										result[0] = true; //at least one platform has been delivered succesfully
										break;
									default :
										final String reasonPhrase = getReasonPhrase(content);
										errorMsg
											.append(String.format("Platform: %s\nError code: %d\n%s", platform, httpStatusCode, reasonPhrase));
										break;
								}
								return null;
							}
						});
					}
					catch (IOException | InterruptedException e)
					{
						errorMsg.append("Can't connect to the remote service.\nTry again later ...");
						ServoyLog.logError(e);
					}
				});
			}
			catch (final Exception e)
			{
				errorMsg.append("Can't connect to the remote service.\nTry again later ...");
				ServoyLog.logError(e);
			}

			final Runnable run = () -> {
				if (errorMsg.length() > 0) MessageDialog.openError(UIUtils.getActiveShell(), "NG Desktop Export", errorMsg.toString());
				else
				{
					final String message = "Your request has been added to the service queue.\nAn email with the download link(s) will be sent to the provided address ...";
					MessageDialog.open(MessageDialog.INFORMATION, UIUtils.getActiveShell(), "NG Desktop Export", message, SWT.None, "OK");
				}
			};
			if (Display.getCurrent() != null) run.run();
			else Display.getDefault().syncExec(run);
			result[1] = true;
		});
		final Shell shell = getShell();
		final Display display = Display.getCurrent();
		while (!shell.isDisposed() && !result[1])
			if (!display.readAndDispatch()) display.sleep();
		return result[0];
	}

	//fail to customize reason phrase into Spring - so extract from the
	private String getReasonPhrase(String content)
	{
		try
		{
			final JSONObject jsonObj = new JSONObject(content);
			return jsonObj.optString("statusMessage", "Unexpected error");
		}
		catch (final JSONException e)
		{
			//Unexpected http response?
			return "Internal server error: " + e.getMessage();

		}
	}

	private String formatRequest(IDialogSettings settings)
	{
		try
		{
			final JSONObject jsonObj = new JSONObject();
			jsonObj.put("platform", settings.get("platform"));
			if (!Utils.stringIsEmpty(settings.get("icon_path")))
				jsonObj.put("icon", getEncodedData(settings.get("icon_path")));
			if (!Utils.stringIsEmpty(settings.get("image_path")))
				jsonObj.put("image", getEncodedData(settings.get("image_path")));
			if (!Utils.stringIsEmpty(settings.get("copyright")))
				jsonObj.put("copyright", settings.get("copyright"));
			if (!Utils.stringIsEmpty(settings.get("app_url")))
				jsonObj.put("url", settings.get("app_url"));
			if (!Utils.stringIsEmpty(settings.get("ngdesktop_width")))
				jsonObj.put("width", settings.get("ngdesktop_width"));
			if (!Utils.stringIsEmpty(settings.get("ngdesktop_height")))
				jsonObj.put("height", settings.get("ngdesktop_height"));
			if (!Utils.stringIsEmpty(settings.get("ngdesktop_version")))
				jsonObj.put("version", getNgDesktopVersion(settings.get("ngdesktop_version")));
			jsonObj.put("includeUpdate", settings.getBoolean("include_update"));
			if (!Utils.stringIsEmpty(settings.get("update_url")))
				jsonObj.put("updateUrl", settings.get("update_url"));
			jsonObj.put("loginToken", settings.get("login_token"));
			jsonObj.put("applicationName", settings.get("application_name"));
			jsonObj.put("devVersion", ClientVersion.getVersion());
			jsonObj.put("emailAddress", settings.get("email_address"));
			jsonObj.put("storageTimeout", settings.getBoolean("store_data") ? STORE_TIMEOUT * 24 : 0); //convert to hours

			return jsonObj.toString();
		}
		catch (final IOException e)
		{
			//
		}
		return null;
	}

	private String getNgDesktopVersion(String selectedVersion)
	{
		return StartNGDesktopClientHandler.getNgDesktopVersion(selectedVersion);
	}


	@Override
	public void addPages()
	{
		exportPage = new ExportPage(this);
		addPage(exportPage);
	}

	private StringBuilder validate(IDialogSettings settings)
	{
		final StringBuilder errorMsg = new StringBuilder();
		final boolean winPlatform = settings.getBoolean("win_export");
		final boolean osxPlatform = settings.getBoolean("osx_export");
		final boolean linuxPlatform = settings.getBoolean("linux_export");
		if (!(winPlatform || osxPlatform || linuxPlatform)) errorMsg.append("At least one platform must be selected\n");

		String strValue = settings.get("icon_path");
		if (strValue != null && strValue.trim().length() > 0)
		{
			final File myFile = new File(strValue);
			if (!myFile.exists() || !myFile.isFile())
			{
				errorMsg.append(myFile.getName() + " doesn't exist. Is a directory?\n");
				return errorMsg;
			}
			if (myFile.length() > LOGO_SIZE * 1024)
			{
				errorMsg.append("Logo file exceeds the maximum allowed limit (" + LOGO_SIZE * 1024 + " KB): " + myFile.length() + "\n");
				return errorMsg;
			}

			final Dimension iconSize = ImageLoader.getSize(myFile);
			if (iconSize.getWidth() < 512 || iconSize.getHeight() < 512)
			{
				errorMsg.append("Image size too small (" + iconSize.getWidth() + " : " + iconSize.getHeight() + ")\n");
				return errorMsg;
			}
		}

		strValue = settings.get("image_path");
		if (strValue != null && strValue.trim().length() > 0)
		{
			final File myFile = new File(strValue);
			if (!myFile.exists() && !myFile.isFile())
			{
				errorMsg.append(myFile.getName() + "  doesn't exist. Is a directory?\n");
				return errorMsg;
			}
			if (myFile.length() > IMG_SIZE * 1024)
			{
				errorMsg.append("Image file exceeds the maximum allowed limit (" + IMG_SIZE * 1024 + " KB): " + myFile.length() + "\n");
				return errorMsg;
			}
		}


		strValue = settings.get("copyright");
		if (strValue != null && strValue.trim().length() > 0 && strValue.toCharArray().length > COPYRIGHT_LENGTH)
		{
			errorMsg.append("Copyright string exceeds the maximum allowed limit (" + COPYRIGHT_LENGTH + " chars): " + strValue.toCharArray().length + "\n");
			return errorMsg;
		}

		int intValue;
		try
		{
			strValue = settings.get("ngdesktop_width");
			if (strValue != null && strValue.trim().length() > 0)
			{
				intValue = Integer.parseInt(strValue);
				if (intValue <= 0)
				{
					errorMsg.append("Invalid width size: " + strValue + "\n");
					return errorMsg;
				}
			}
			strValue = settings.get("ngdesktop_height");
			if (strValue != null && strValue.trim().length() > 0)
			{
				intValue = Integer.parseInt(strValue);
				if (intValue <= 0)
				{
					errorMsg.append("Invalid height size: " + strValue + "\n");
					return errorMsg;
				}
			}
		}
		catch (final NumberFormatException e)
		{
			errorMsg.append("NumberFormatException: " + e.getMessage() + "\n");
			return errorMsg;

		}

		final boolean includeUpdate = settings.getBoolean("include_update");
		strValue = settings.get("update_url");

		if (includeUpdate && (strValue == null || strValue.trim().isEmpty()))
		{
			errorMsg.append("Please provide server address for the update packages\n");
			return errorMsg;
		}

		if (strValue != null && !strValue.trim().isEmpty() && !isValidUrl(strValue))
		{
			final boolean result = MessageDialog.open(MessageDialog.QUESTION, UIUtils.getActiveShell(), "NG Desktop Export",
				"URL can't be validated. Use it anyway? \n" + strValue, SWT.YES);
			if (!result)
			{
				errorMsg.append("Invalid URL: " + strValue + "\n");
				return errorMsg;
			}
		}

		strValue = settings.get("application_name");
		if (strValue == null || strValue.trim().length() == 0)
		{
			errorMsg.append("Provide a name for the application ...\n");
			return errorMsg;
		}
		if (strValue.toCharArray().length > APP_NAME_LENGTH)
		{
			errorMsg
				.append("Application name string exceeds the maximum allowed limit (" + APP_NAME_LENGTH + " chars): " + strValue.toCharArray().length + "\n");
			return errorMsg;
		}

		strValue = settings.get("email_address");
		if (strValue == null || strValue.trim().length() == 0)
		{
			errorMsg.append("Email address is missing ...\n");
			return errorMsg;
		}
		final String regex = "^(.+)@(.+)$";
		//Compile regular expression to get the pattern
		final Pattern pattern = Pattern.compile(regex);
		final Matcher matcher = pattern.matcher(strValue);
		if (!matcher.find())
		{
			errorMsg.append("Email address is not valid: " + strValue + "\n");
			return errorMsg;
		}
		return errorMsg;
	}


	private String getEncodedData(String resourcePath) throws IOException
	{// expect absolute path
		if (resourcePath != null) try (FileInputStream fis = new FileInputStream(new File(resourcePath)))
		{
			return Base64.getEncoder().encodeToString(IOUtils.toByteArray(fis));
		}
		return null;
	}


	boolean isValidUrl(String url)
	{
		try
		{
			new URL(url).toURI();
			return true;
		}
		catch (final MalformedURLException e)
		{
			return false;
		}
		catch (final URISyntaxException e)
		{
			return false;
		}
	}
}