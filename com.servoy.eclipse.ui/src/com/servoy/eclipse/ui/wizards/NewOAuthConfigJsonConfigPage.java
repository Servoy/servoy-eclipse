package com.servoy.eclipse.ui.wizards;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.json.JSONObject;

import com.servoy.eclipse.ui.Activator;

/**
 * @author emera
 */
public class NewOAuthConfigJsonConfigPage extends WizardPage
{

	private Text jsonTextArea;
	private final NewOAuthConfigWizard wizard;

	protected NewOAuthConfigJsonConfigPage(NewOAuthConfigWizard wizard)
	{
		super("OAuth JSON");
		setTitle("OAuth JSON Configuration");
		setDescription("Review and modify the OAuth JSON configuration.");
		this.wizard = wizard;
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NONE);
		GridLayout layout = new GridLayout(1, false);
		container.setLayout(layout);

		Image warn = Activator.getDefault().loadImageFromBundle("warning.png");
		CLabel warnLabel = new CLabel(container, SWT.ICON);
		warnLabel.setImage(warn);
		GridData data = new GridData(GridData.FILL_HORIZONTAL);
		warnLabel.setLayoutData(data);
		warnLabel.setText("Careful");
		Label warn_ = new Label(container, SWT.WRAP);
		warn_.setLayoutData(data);
		warn_.setText("Adjustments to the configuration must be made according to the documentation of the selected provider.\n");

		jsonTextArea = new Text(container, SWT.BORDER | SWT.MULTI | SWT.V_SCROLL | SWT.H_SCROLL);
		jsonTextArea.setLayoutData(new GridData(GridData.FILL_BOTH));
		jsonTextArea.addModifyListener(e -> {
			wizard.updateConfig(new JSONObject(jsonTextArea.getText()));
		});

		setControl(container);
	}

	public String getOAuthJson()
	{
		return jsonTextArea.getText();
	}

	public void updateJsonContent()
	{
		if (jsonTextArea != null)
		{
			jsonTextArea.setText(((NewOAuthConfigWizard)getWizard()).getJSON().toString(4));
		}
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		updateJsonContent();
	}
}