package com.servoy.eclipse.exporter.setuppipeline;

import java.io.File;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import org.eclipse.core.resources.IProject;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.storage.file.FileRepositoryBuilder;
import org.eclipse.jgit.transport.RemoteConfig;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONArray;
import org.json.JSONObject;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.ui.dialogs.ServoyLoginDialog;

public class SetupPipelineDetailsPage extends WizardPage
{

	private Text namespaceText;
	private Text applicationJobNameText;

	private Text gitUsernameText;
	private Text gitPasswordText;
	private Text gitUrlText;
	private Text currentBranch;

	private final String[] loginTokenForJson = new String[1];

	private Font boldFont;

	private GitInfo gitInfo;

	private final List<String> names = new ArrayList<>();

	private static final String CROWD = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu") +
		"/servoy-service/rest_ws/api/developer/getApplications?loginToken=";


	protected SetupPipelineDetailsPage(String pageName)
	{
		super(pageName);
		setTitle("Servoy Cloud Pipeline Details");
		setDescription("Provide the necessary details to configure your Servoy Cloud pipeline.");
	}

	private Composite createSection(Composite parent, String title)
	{
		Composite section = new Composite(parent, SWT.NONE);
		section.setLayout(new GridLayout(2, false));
		section.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
		section.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		Label sectionLabel = new Label(section, SWT.NONE);
		sectionLabel.setText(title);
		sectionLabel.setFont(boldFont);
		sectionLabel.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		GridData labelData = new GridData();
		labelData.horizontalSpan = 2;
		sectionLabel.setLayoutData(labelData);

		return section;
	}

	private Text createLabeledText(Composite parent, String labelText)
	{
		return createLabeledText(parent, labelText, false);
	}

	private Text createLabeledText(Composite parent, String labelText, boolean isPassword)
	{
		Label label = new Label(parent, SWT.NONE);
		label.setText(labelText);
		label.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

		label.setBackground(parent.getBackground());

		Text text = new Text(parent, isPassword ? SWT.BORDER | SWT.PASSWORD : SWT.BORDER);
		text.setLayoutData(new GridData(GridData.FILL_HORIZONTAL));
		text.setBackground(parent.getBackground());

		text.addModifyListener(e -> validatePage());

		return text;
	}

	@Override
	public void createControl(Composite parent)
	{
		boldFont = new Font(parent.getDisplay(), "Segoe UI", 10, SWT.BOLD);

		IProject solutionProject = ServoyModelFinder.getServoyModel().getActiveProject().getProject();
		String solutionDir = solutionProject.getLocation().toString();
		File repoRoot = new File(solutionDir).getParentFile();
		gitInfo = getRemoteInfo(repoRoot);

		ServoyLoginDialog.getLoginToken(loginToken -> {
			if (loginToken != null)
			{
				loginTokenForJson[0] = loginToken;
			}
		});

		try
		{
			String url = CROWD + loginTokenForJson[0]; // Append token to the URL
			HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(url))
				.header("Accept", "application/json")
				.GET()
				.build();

			HttpClient client = HttpClient.newHttpClient();
			HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());

			Composite container = new Composite(parent, SWT.NONE);
			container.setLayout(new GridLayout(1, false));
			container.setBackground(parent.getDisplay().getSystemColor(SWT.COLOR_WHITE));

			Composite appSection = createSection(container, "Application Info");
			namespaceText = createLabeledText(appSection, "Namespace:");
			namespaceText.setEditable(false);

			if (response.statusCode() == 200)
			{
				JSONObject jsonObject = new JSONObject(response.body());
				String namespace = (String)jsonObject.get("namespace");

				if (namespace != null && !namespace.trim().isEmpty())
				{
					namespaceText.setText(namespace);
				}
				else
				{
					// Show error message in the wizard page
					setErrorMessage("Namespace is missing or invalid. Please verify your Servoy Cloud configuration.");
					setPageComplete(false); // Prevent moving forward
				}

				Object applicationsObj = jsonObject.get("applications");
				if (applicationsObj instanceof JSONArray applicationsArray)
				{
					for (int i = 0; i < applicationsArray.length(); i++)
					{
						JSONObject appObj = applicationsArray.getJSONObject(i);
						String name = appObj.getString("name");
						names.add(name);
					}
				}

			}

			applicationJobNameText = createLabeledText(appSection, "Application Name:");

			Composite gitSection = createSection(container, "Git Information");
			gitUsernameText = createLabeledText(gitSection, "Git Username:");
			gitUsernameText.setText(gitInfo.userName != null ? gitInfo.userName : "");

			gitPasswordText = createLabeledText(gitSection, "Git Password / Token:", true);

			gitUrlText = createLabeledText(gitSection, "Git Repository URL:");
			gitUrlText.setText(gitInfo.url != null ? gitInfo.url : "");

			currentBranch = createLabeledText(gitSection, "Current Branch:");
			currentBranch.setText(gitInfo.gitBranch != null ? gitInfo.gitBranch : "");

			setControl(container);

			validatePage();

		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	private void validatePage()
	{
		boolean hasAppName = applicationJobNameText != null &&
			!applicationJobNameText.getText().trim().isEmpty();

		boolean hasGitInfo = gitUsernameText != null && gitPasswordText != null && gitUrlText != null &&
			!gitUsernameText.getText().trim().isEmpty() &&
			!gitUrlText.getText().trim().isEmpty();

		if (!hasAppName)
		{
			setErrorMessage("Application job name is required.");
			setPageComplete(false);
			return;
		}

		String enteredName = applicationJobNameText.getText().trim();
		if (names.stream().anyMatch(n -> n.equalsIgnoreCase(enteredName)))
		{
			setErrorMessage("Application name already exists: " + enteredName);
			setPageComplete(false);
			return;
		}

		if (!hasGitInfo)
		{
			setErrorMessage("Git information is incomplete. Please provide username, password/token, and repository URL.");
			setPageComplete(false);
			return;
		}

		// If everything is valid
		setErrorMessage(null);
		setPageComplete(true);
	}

	public String getCurrentBranch()
	{
		return currentBranch.getText();
	}

	public String getLoginToken()
	{
		return loginTokenForJson[0];
	}

	// Getters for retrieving user input
	public String getNamespace()
	{
		return namespaceText.getText();
	}

	public String getApplicationJobName()
	{
		return applicationJobNameText.getText();
	}

	public String getGitUsername()
	{
		return gitUsernameText.getText();
	}

	public String getGitPassword()
	{
		return gitPasswordText.getText();
	}

	public String getGitUrl()
	{
		return gitUrlText.getText();
	}

	public GitInfo getGitInfo()
	{
		return gitInfo;
	}

	@Override
	public void dispose()
	{
		if (boldFont != null && !boldFont.isDisposed()) boldFont.dispose();
		super.dispose();
	}

	private GitInfo getRemoteInfo(File repoRoot)
	{
		try
		{
			FileRepositoryBuilder builder = new FileRepositoryBuilder();
			Repository repository = builder.setGitDir(new File(repoRoot, ".git"))
				.readEnvironment()
				.findGitDir()
				.build();

			String gitBranch = repository.getBranch();

			Collection<RemoteConfig> remotes = RemoteConfig.getAllRemoteConfigs(repository.getConfig());
			for (RemoteConfig remote : remotes)
			{
				for (URIish uri : remote.getURIs())
				{
					String url = uri.toString();
					String token = uri.getPass(); // Token if present
					String repoName = extractRepoName(uri.getPath());
					String userName = uri.getUser();
					String host = uri.getHost();
					return new GitInfo(url, token, repoName, userName, host, gitBranch);
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		return new GitInfo("", "", "", "", "", "");
	}

	private String extractRepoName(String path)
	{
		if (path != null && path.endsWith(".git"))
		{
			String[] parts = path.split("/");
			return parts[parts.length - 1].replace(".git", "");
		}
		return null;
	}

	// Helper class
	class GitInfo
	{
		String url;
		String token;
		String repoName;
		String userName;
		String host;
		String gitBranch;

		GitInfo(String url, String token, String repoName, String userName, String host, String gitBranch)
		{
			this.url = url;
			this.token = token;
			this.repoName = repoName;
			this.userName = userName;
			this.host = host;
			this.gitBranch = gitBranch;
		}
	}
}

