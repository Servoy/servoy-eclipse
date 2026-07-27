package com.servoy.eclipse.ui.wizards;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import org.eclipse.jface.dialogs.IDialogConstants;
import org.eclipse.jface.dialogs.TitleAreaDialog;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.ngclient.auth.OAuthPropertyResolver;
import com.servoy.j2db.server.ngclient.auth.OAuthUtils;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

/**
 * Wizard to setup an oauth configuration for stateless login.
 * @author emera
 */
public class NewOAuthConfigWizard extends Wizard implements IWorkbenchWizard
{
	private NewOAuthApiPage apiPage;
	private Solution solution;
	private NewOAuthConfigJsonConfigPage advancedPage;
	private OAuthApiConfiguration model;

	public NewOAuthConfigWizard()
	{
		setWindowTitle("OAuth Configuration Wizard");
	}

	@Override
	public void addPages()
	{
		apiPage = new NewOAuthApiPage(this);
		advancedPage = new NewOAuthConfigJsonConfigPage(this);
		addPage(apiPage);
		addPage(advancedPage);
	}

	@Override
	public boolean performFinish()
	{
		JSONObject json = getJSON();

		// If the config contains %%key%% placeholders, check whether they are
		// defined in servoy.properties. If not, warn the user and tell them
		// where to configure them — but still allow saving.
		if (OAuthPropertyResolver.containsPlaceholder(json))
		{
			try
			{
				Properties settings = ApplicationServerRegistry.get().getServerAccess().getSettings();
				List<String> missing = OAuthPropertyResolver.findUnresolved(json, settings);
				if (!missing.isEmpty())
				{
					openMissingPropertiesDialog(getShell(), missing);
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError("NewOAuthConfigWizard: could not check servoy.properties for OAuth placeholders", e);
			}
			// Placeholder configs cannot be validated via createOauthService — save directly
			ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			Solution editingSolution = activeProject.getEditingSolution();
			editingSolution.putCustomProperty(new String[] { StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES }, json.toString());
			EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
			repository.updateNodesInWorkspace(new IPersist[] { editingSolution }, false);
			return true;
		}

		Object service = OAuthUtils.createOauthService(json, new HashMap<>(), "http://");
		if (service != null)
		{
			ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
			Solution editingSolution = activeProject.getEditingSolution();
			editingSolution.putCustomProperty(new String[] { StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES }, json.toString());
			EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
			repository.updateNodesInWorkspace(new IPersist[] { editingSolution }, false);
			return true;
		}
		((WizardPage)getContainer().getCurrentPage()).setErrorMessage("The configuration is wrong. Please check if it follows the documentation.");
		return false;
	}

	@Override
	public void init(IWorkbench workbench, IStructuredSelection selection)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		ServoyProject activeProject = servoyModel.getActiveProject();
		if (activeProject != null)
		{
			solution = activeProject.getEditingSolution();
			Object property = solution.getCustomProperty(new String[] { StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES });
			if (property instanceof String)
			{
				try
				{
					this.model = (new ObjectMapper()).readValue((String)property, OAuthApiConfiguration.class);
				}
				catch (Exception e)
				{
					((WizardPage)getContainer().getCurrentPage()).setErrorMessage("Could not load the configuration.");
				}
			}
		}
	}

	public JSONObject getJSON()
	{
		return model != null ? new JSONObject(model.toJson()) : new JSONObject();
	}

	@Override
	public boolean canFinish()
	{
		if (model == null) return false;
		String clientId = model.getClientId();
		String clientSecret = model.getClientSecret();
		if (clientId == null || clientId.trim().isEmpty()) return false;
		if (clientSecret == null || clientSecret.trim().isEmpty()) return false;
		return model.isValid();
	}

	public void updateConfig(JSONObject jsonObject)
	{
		try
		{
			this.model = (new ObjectMapper()).readValue(jsonObject.toString(), OAuthApiConfiguration.class);
			getContainer().updateButtons();
		}
		catch (Exception e)
		{
			//ignore exception on edit
		}
	}

	public Optional<OAuthApiConfiguration> getModel()
	{
		return Optional.ofNullable(this.model);
	}

	public void setModel(OAuthApiConfiguration newModel)
	{
		this.model = newModel;
	}

	private static void openMissingPropertiesDialog(Shell shell, List<String> missing)
	{
		TitleAreaDialog dialog = new TitleAreaDialog(shell)
		{
			@Override
			public void create()
			{
				super.create();
				setTitle("Missing OAuth properties");
				setMessage("The following properties need to be defined on the Servoy Admin Panel.\n" +
					"Go to 'Servoy Server Home' and add them in the 'oauth.properties' section.\n" +
					"The config has been saved and will work once these properties are configured.");
			}

			@Override
			protected Control createDialogArea(Composite parent)
			{
				Composite area = (Composite)super.createDialogArea(parent);
				Composite container = new Composite(area, SWT.NONE);
				container.setLayout(new GridLayout(1, false));
				container.setLayoutData(new GridData(GridData.FILL_BOTH));

				Label label = new Label(container, SWT.NONE);
				label.setText("Copy the property keys below and paste them in the admin page:");

				Text text = new Text(container, SWT.BORDER | SWT.MULTI | SWT.READ_ONLY | SWT.V_SCROLL);
				GridData gd = new GridData(GridData.FILL_BOTH);
				gd.heightHint = 80;
				text.setLayoutData(gd);
				StringBuilder sb = new StringBuilder();
				for (String key : missing)
				{
					sb.append(key).append("=\n");
				}
				text.setText(sb.toString());
				text.selectAll();
				return area;
			}

			@Override
			protected void createButtonsForButtonBar(Composite parent)
			{
				createButton(parent, IDialogConstants.OK_ID, "OK", true);
			}
		};
		dialog.open();
	}
}