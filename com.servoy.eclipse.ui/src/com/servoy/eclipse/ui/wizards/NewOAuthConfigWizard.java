package com.servoy.eclipse.ui.wizards;

import java.util.HashMap;

import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchWizard;
import org.json.JSONObject;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.ngclient.StatelessLoginHandler;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 *
 * @author emera
 */
public class NewOAuthConfigWizard extends Wizard implements IWorkbenchWizard
{
	private NewOAuthApiPage page;
	private JSONObject jsonConfig;
	private Solution solution;

	public NewOAuthConfigWizard()
	{
		setWindowTitle("OAuth Configuration Wizard");
	}

	@Override
	public void addPages()
	{
		page = new NewOAuthApiPage();
		addPage(page);
	}

	@Override
	public boolean performFinish()
	{
		Object service = StatelessLoginHandler.createOauthService(jsonConfig, new HashMap<>(), "http://");
		if (service != null)
		{
			JSONObject original = new ServoyJSONObject(solution.getCustomProperties(), true);
			original.put("oauth", jsonConfig);
			solution.setCustomProperties(ServoyJSONObject.toString(original, true, true, true));
			EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
			repository.updateNodesInWorkspace(new IPersist[] { solution }, false);
			return true;
		}
		//TODO set error
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
			jsonConfig = original.optJSONObject(StatelessLoginHandler.OAUTH_CUSTOM_PROPERTIES);
			if (jsonConfig == null) jsonConfig = new JSONObject();
		}
	}

	public JSONObject getJSON()
	{
		return jsonConfig;
	}

	@Override
	public boolean canFinish()
	{
		// common fields for all APIs
		if (jsonConfig.optString(StatelessLoginHandler.CLIENT_ID, "").isEmpty() || jsonConfig.optString(StatelessLoginHandler.API_SECRET, "").isEmpty() ||
			jsonConfig.optString(StatelessLoginHandler.JWKS_URI, "").isEmpty())
		{
			return false;
		}

		if ("Custom".equals(page.getApiSelection()))
		{
			if (jsonConfig.optString(StatelessLoginHandler.AUTHORIZATION_BASE_URL, "").isEmpty() ||
				jsonConfig.optString(StatelessLoginHandler.ACCESS_TOKEN_ENDPOINT, "").isEmpty())
			{
				return false;
			}
		}

		return true;
	}
}