package com.servoy.eclipse.exporter.setuppipeline;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.program.Program;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Link;

public class SetupPipelineInfoPage extends WizardPage
{
	private Font titleFont;

	protected SetupPipelineInfoPage(String pageName)
	{
		super(pageName);
		setTitle("Wizard Page Title");
	}

	@Override
	public void createControl(Composite parent)
	{
		final Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(1, false));

		titleFont = new Font(parent.getDisplay(), "Segoe UI", 12, SWT.BOLD);

		final Label titleLabel = new Label(container, SWT.WRAP);
		titleLabel.setText("Welcome to Servoy Cloud Pipeline Setup");
		titleLabel.setFont(titleFont);

		final Link infoLink = new Link(container, SWT.WRAP);
		infoLink.setText(
			"Before you begin, please make sure you have the following ready:\n\n" +
				"1. <a href=\"https://docs.servoy.com/reference/servoy-cloud/cloud-control-center/home/setup-namespace\">Namespace Setup Guide</a>\n" +
				"   - Ensure you have created a namespace in Servoy Cloud.\n\n" +
				"2. Git Repository Access\n" +
				"   - Git URL, username, and password or personal access token.\n\n" +
				"The wizard will automatically create the required 'jenkins-custom' folder and configuration files for your pipeline.\n\n" +
				"Once you have these ready, click 'Next' to begin the setup wizard.");
		infoLink.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		infoLink.addListener(SWT.Selection, event -> {
			Program.launch(event.text);
		});

		setControl(container);
	}

	@Override
	public void dispose()
	{

		if (titleFont != null && !titleFont.isDisposed()) titleFont.dispose();

		super.dispose();

	}
}
