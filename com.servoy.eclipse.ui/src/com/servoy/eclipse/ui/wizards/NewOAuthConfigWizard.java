package com.servoy.eclipse.ui.wizards;

import java.util.HashMap;
import java.util.Optional;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.json.JSONObject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.OAuthUtils;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyJSONObject;

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
		Object service = OAuthUtils.createOauthService(json, new HashMap<>(), "http://");
		if (service != null)
		{
			JSONObject original = new ServoyJSONObject(solution.getCustomProperties(), true);
			original.put("oauth", json);
			solution.setCustomProperties(ServoyJSONObject.toString(original, true, true, true));
			EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
			repository.updateNodesInWorkspace(new IPersist[] { solution }, false);
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
			JSONObject original = new ServoyJSONObject(solution.getCustomProperties(), true);
			JSONObject jsonConfig = original.optJSONObject(StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES);
			if (jsonConfig != null)
			{
				try
				{
					this.model = (new ObjectMapper()).readValue(jsonConfig.toString(), OAuthApiConfiguration.class);
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
		return model != null && model.isValid();
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
}