package com.servoy.eclipse.exporter.setuppipeline;

import java.io.File;

import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.DirectoryDialog;
import org.eclipse.swt.widgets.FileDialog;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;

import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class SetupPipelineJenkinsCustomPage extends WizardPage
{
	private Text servoyPropsText;
	private Text webXmlText;
	private Text contextXmlText;
	private Text log4jXmlText;
	private Text faviconDirText;

	protected SetupPipelineJenkinsCustomPage(String pageName)
	{
		super(pageName);
		setTitle("Jenkins-Custom Folder Setup");
		setDescription("Select or provide paths for the required files for Servoy Cloud pipeline.");
	}

	@Override
	public void createControl(Composite parent)
	{
		Composite container = new Composite(parent, SWT.NONE);
		container.setLayout(new GridLayout(3, false));

		Label descriptionLabel = new Label(container, SWT.WRAP);
		descriptionLabel.setText("Provide paths for the required configuration files. \n" +
			"The wizard will use these files to generate the Jenkins-Custom folder for Servoy Cloud pipeline. \n" +
			"The servoy.properties.template will be created based on the selected servoy.properties file.");
		GridData descData = new GridData(SWT.FILL, SWT.TOP, true, false);
		descData.horizontalSpan = 3;
		descriptionLabel.setLayoutData(descData);

		servoyPropsText = createFileSelector(container, "servoy.properties:",
			"Select the servoy.properties file from your application server directory.");
		webXmlText = createFileSelector(container, "web.xml:",
			"Take a web.xml from a generated war for adjustment and include it here, it must be a servoy war generated web.xml file to begin with:");
		contextXmlText = createFileSelector(container, "context.xml:",
			"Add the given Tomcat context.xml file in the war file (META-INF/context.xml)");
		log4jXmlText = createFileSelector(container, "log4j.xml:",
			"Apache Log4j2 configuration file:");

		faviconDirText = createDirectorySelector(container, "Favicons Directory:",
			"Select the directory containing favicon images for your web application.");

		String applicationServerDir = ApplicationServerRegistry.get().getServoyApplicationServerDirectory();

//		servoyPropsText.setText(applicationServerDir + "servoy.properties");

		faviconDirText.setText(applicationServerDir + "server" + File.separator + "webapps" + File.separator + "ROOT");

		//TODO: add another validator that checks if the files exist// or just check them in the perform finish

		//webXmlText.addModifyListener(validationListener);
		//contextXmlText.addModifyListener(validationListener);
		//log4jXmlText.addModifyListener(validationListener);
		
		setPageComplete(true);

		ModifyListener validationListener = e -> setPageComplete(isPageComplete());
		servoyPropsText.addModifyListener(validationListener);
		faviconDirText.addModifyListener(validationListener);

		setControl(container);
	}

	private Text createFileSelector(Composite parent, String labelText, String explanationText)
	{
		// Create a composite for this field
		Composite fieldComposite = new Composite(parent, SWT.NONE);
		fieldComposite.setLayout(new GridLayout(3, false));
		GridData fieldData = new GridData(SWT.FILL, SWT.TOP, true, false);
		fieldData.horizontalSpan = 3; // Span across outer layout
		fieldComposite.setLayoutData(fieldData);

		// Explanation label (full width)
		Label explanationLabel = new Label(fieldComposite, SWT.WRAP);
		explanationLabel.setText(explanationText);
		GridData explanationData = new GridData(SWT.FILL, SWT.TOP, true, false);
		explanationData.horizontalSpan = 3;
		explanationLabel.setLayoutData(explanationData);

		// Main label
		Label label = new Label(fieldComposite, SWT.NONE);
		label.setText(labelText);

		// Text field
		Text textField = new Text(fieldComposite, SWT.BORDER);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		// Browse button
		Button browseButton = new Button(fieldComposite, SWT.PUSH);
		browseButton.setText("...");
		browseButton.addListener(SWT.Selection, e -> {
			FileDialog dialog = new FileDialog(parent.getShell(), SWT.OPEN);
			String selected = dialog.open();
			if (selected != null) textField.setText(selected);
		});

		return textField;
	}

	private Text createDirectorySelector(Composite parent, String labelText, String explanationText)
	{
		Composite fieldComposite = new Composite(parent, SWT.NONE);
		fieldComposite.setLayout(new GridLayout(3, false));
		GridData fieldData = new GridData(SWT.FILL, SWT.TOP, true, false);
		fieldData.horizontalSpan = 3;
		fieldComposite.setLayoutData(fieldData);

		// Explanation label
		Label explanationLabel = new Label(fieldComposite, SWT.WRAP);
		explanationLabel.setText(explanationText);
		GridData explanationData = new GridData(SWT.FILL, SWT.TOP, true, false);
		explanationData.horizontalSpan = 3;
		explanationLabel.setLayoutData(explanationData);

		// Main label
		Label label = new Label(fieldComposite, SWT.NONE);
		label.setText(labelText);

		// Text field
		Text textField = new Text(fieldComposite, SWT.BORDER);
		textField.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

		Button browseButton = new Button(fieldComposite, SWT.PUSH);
		browseButton.setText("...");
		browseButton.addListener(SWT.Selection, e -> {
			DirectoryDialog dialog = new DirectoryDialog(parent.getShell());
			String selected = dialog.open();
			if (selected != null) textField.setText(selected);
		});

		return textField;
	}


	@Override
	public boolean isPageComplete()
	{
		String faviconPath = faviconDirText.getText().trim();
		if (faviconPath.isEmpty())
		{
			setErrorMessage("Favicons directory is required.");
			return false;
		}

		File favDir = new File(faviconPath);
		if (!favDir.exists() || !favDir.isDirectory())
		{
			setErrorMessage("Favicons directory is invalid.");
			return false;
		}

		String[] requiredFavicons = { "favicon.ico", "favicon32x32.png", "favicon192x192.png" };

		for (String f : requiredFavicons)
		{
			if (!new File(favDir, f).exists())
			{
				setErrorMessage("Missing required favicon: " + f);
				return false;
			}
		}

		String propsPath = servoyPropsText.getText().trim();
		if (!propsPath.isEmpty())
		{
			File props = new File(propsPath);
			if (!props.exists())
			{
				setErrorMessage("servoy.properties file does not exist.");
				return false;
			}
			setErrorMessage(null);
			return false; // Next disabled intentionally
		}

		setErrorMessage(null);
		return true;
	}

	// Getters
	public String getServoyPropsPath()
	{
		return servoyPropsText.getText();
	}

	public String getWebXmlPath()
	{
		return webXmlText.getText();
	}

	public String getContextXmlPath()
	{
		return contextXmlText.getText();
	}

	public String getLog4jXmlPath()
	{
		return log4jXmlText.getText();
	}

	public String getFaviconsDir()
	{
		return faviconDirText.getText();
	}
}