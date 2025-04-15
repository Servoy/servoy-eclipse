package com.servoy.eclipse.cloud;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IProjectDescription;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.egit.core.credentials.UserPasswordCredentials;
import org.eclipse.equinox.security.storage.StorageException;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.window.Window;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.URIish;
import org.eclipse.jgit.util.FileUtils;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.dialogs.ListDialog;
import org.eclipse.ui.plugin.AbstractUIPlugin;
import org.json.JSONArray;
import org.json.JSONObject;
import org.osgi.framework.BundleContext;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.ServoyMessageDialog;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;

/**
 * The activator class controls the plug-in life cycle
 */
public class Activator extends AbstractUIPlugin {
	// private static final String CROWD =
	// "https://middleware-dev.unifiedui.servoy-cloud.eu/servoy-service/rest_ws/api/developer/getApplications?loginToken=";
	private static final String CROWD = System.getProperty("servoy.api.url", "https://middleware-prod.unifiedui.servoy-cloud.eu") +
		"/servoy-service/rest_ws/api/developer/getApplications?loginToken=";

	// The plug-in ID
	public static final String PLUGIN_ID = "com.servoy.eclipse.cloud"; //$NON-NLS-1$

	// The shared instance
	private static Activator plugin;

	/**
	 * The constructor
	 */
	public Activator() {
	}

	@Override
	public void start(BundleContext context) throws Exception {
		super.start(context);
		plugin = this;
	}

	public void checkoutFromCloud(String loginToken, String username, String password) {
		Job.create("Checkout from ServoyCloud", (monitor) -> {
			String uri = null;
			try {
				IProject[] projects = ResourcesPlugin.getWorkspace().getRoot().getProjects();
				if (projects.length == 0) {
					JSONObject selected = null;
					try {
						var send = HttpClient.newHttpClient().send(
								HttpRequest.newBuilder(new URI(CROWD + loginToken)).build(),
								HttpResponse.BodyHandlers.ofString());
						var body = send.body();
						if (body == null) {
							ServoyLog.logInfo("The Servoy Cloud didn't give back any applications");
							return;
						}
 						JSONObject json = new JSONObject(body);
						JSONArray jsonArray = json.optJSONArray("applications");
						if (jsonArray != null && jsonArray.length() > 0) {
							// we need to ask which one to install.
							List<JSONObject> applications = new ArrayList<>();
							jsonArray.forEach(object -> {
								JSONObject jsonObject = (JSONObject) object;
								String name = jsonObject.optString("name");
								String repository_url = jsonObject.optString("repository_url");
								String branch = jsonObject.optString("branch");
								String main_solution = jsonObject.optString("main_solution");

								if (!StringUtils.isAnyEmpty(name, repository_url, branch, main_solution))
									applications.add((JSONObject) object);
							});

							if (applications.size() > 1) {
								ListDialog dialog = new ListDialog(UIUtils.getActiveShell()) {
									protected void setShellStyle(int newShellStyle) {
										super.setShellStyle(
												(newShellStyle & ~SWT.APPLICATION_MODAL) | SWT.PRIMARY_MODAL);
									}
								};
								dialog.setTitle("Select the Application that you want to checkout");
								dialog.setMessage("Appplication list in servoy cloud");
								dialog.setAddCancelButton(true);
								dialog.setInput(applications);
								dialog.setContentProvider(ArrayContentProvider.getInstance());
								dialog.setLabelProvider(new LabelProvider() {
									public String getText(Object value) {
										if (value instanceof JSONObject json) {
											return json.optString("name");
										}
										return "<novalue>";
									}
								});
								Object[] result = Display.getDefault().syncCall(() -> {
									if (dialog.open() == Window.OK) {
										return dialog.getResult();
									}
									return null;
								});
								if (result != null && result.length > 0)
									selected = (JSONObject) result[0];
							} else if (applications.size() == 1) {
								selected = applications.get(0);
								String name = selected.optString("name");
								if (!Display.getDefault().syncCall(() -> {
									return ServoyMessageDialog.openConfirm(UIUtils.getActiveShell(),
											"Servoy Cloud application " + name + " found.",
											"Do you want to check it out in the current workspace?");
								})) {
									selected = null;
								}
							}
						}
					} catch (IOException | InterruptedException | URISyntaxException e) {
						ServoyLog.logError(e);
					}
					if (selected != null) {
						uri = selected.optString("repository_url");
						String branch = selected.optString("branch");
						String mainSolutionName = selected.optString("main_solution");

						org.eclipse.egit.core.Activator.getDefault().getCredentialsStore()
								.putCredentials(new URIish(uri), new UserPasswordCredentials(username, password));
						monitor.beginTask("Checking out ", 100);
						IPath workspaceRoot = ResourcesPlugin.getWorkspace().getRoot().getLocation();
						File workspaceFile = workspaceRoot.toFile();
						CloneCommand cloneRepository = Git.cloneRepository();
						cloneRepository.setURI(uri);
						File tmpDir = new File(workspaceFile, ".servoy_tmp");
						tmpDir.deleteOnExit();
						cloneRepository.setDirectory(tmpDir);
						cloneRepository.setBranch(branch);
						Git git = cloneRepository.call();
						git.close();
						monitor.worked(90);
						monitor.subTask("Moving files to the workspace");
						for (File file : tmpDir.listFiles()) {
							Files.move(file.toPath(), new File(workspaceFile, file.getName()).toPath(),
									StandardCopyOption.ATOMIC_MOVE);
						}
						if (!tmpDir.delete()) {
							FileUtils.delete(workspaceFile, FileUtils.RECURSIVE | FileUtils.RETRY | FileUtils.IGNORE_ERRORS);
						}
						monitor.subTask("Importing the solution");
						List<File> projectsFiles = new ArrayList<>();
						Files.list(workspaceFile.toPath()).filter(
								path -> !path.toFile().getName().equals(".metadata") && path.toFile().isDirectory())
								.forEach(path -> visitDir(path, projectsFiles));
						projectsFiles.forEach(projectsDir -> {
							IProjectDescription description = ResourcesPlugin.getWorkspace()
									.newProjectDescription(projectsDir.getName());
							description.setLocation(
									org.eclipse.core.runtime.Path.fromOSString(projectsDir.toPath().toString()));
							IProject project = ResourcesPlugin.getWorkspace().getRoot()
									.getProject(projectsDir.getName());
							try {
								project.create(description, monitor);
								project.open(monitor);
							} catch (CoreException e) {
								e.printStackTrace();
							}
						});
						monitor.done();

						IProject project = ResourcesPlugin.getWorkspace().getRoot().getProject(mainSolutionName);
						ServoyProject sp = (ServoyProject) project.getNature(ServoyProject.NATURE_ID);

						ServoyModelManager.getServoyModelManager().getServoyModel().setActiveProject(sp, true);
					}
				}
			} catch (GitAPIException | IOException | StorageException | URISyntaxException e) {
				ServoyLog.logError("Error checking out cloud solution from git " + uri, e);
			}
		}).schedule();
	}

	private static void visitDir(Path dir, List<File> projectsFiles) {
		if (new File(dir.toFile(), ".project").exists()) {
			projectsFiles.add(dir.toFile());
		} else
			try {
				Files.list(dir).filter(path -> path.toFile().isDirectory())
						.forEach(path -> visitDir(path, projectsFiles));
			} catch (IOException e) {
				ServoyLog.logError(e);
			}
	}

	@Override
	public void stop(BundleContext context) throws Exception {
		plugin = null;
		super.stop(context);
	}

	/**
	 * Returns the shared instance
	 *
	 * @return the shared instance
	 */
	public static Activator getDefault() {
		return plugin;
	}

}
