package com.servoy.eclipse.solution.integration;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.swt.SWT;
import org.eclipse.swt.browser.Browser;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.graphics.Rectangle;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Monitor;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.ICloseable;
import com.servoy.eclipse.core.JSDeveloperSolutionModel;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

public class RunDesignClientDialog extends Dialog implements ICloseable {

	public RunDesignClientDialog(Shell parent) {
		super(parent);
	}

	@Override
	protected IDialogSettings getDialogBoundsSettings() {
		IDialogSettings activatorDialogSettings = Activator.getInstance().getDialogSettings();
		IDialogSettings dialog = activatorDialogSettings.getSection("run_design");
		if (dialog == null)
			dialog = activatorDialogSettings.addNewSection("run_design");
		return dialog;
	}

	@Override
	protected Point getInitialSize() {
		Point initialSize = super.getInitialSize();
		if (initialSize != null && (initialSize.x > 200 || initialSize.y > 200)) {
			return initialSize;
		}
		Composite parent = getShell().getParent();
		Monitor monitor = getShell().getDisplay().getPrimaryMonitor();
		if (parent != null) {
			monitor = parent.getMonitor();
		}
		Rectangle monitorBounds = monitor.getClientArea();
		return new Point( (int)(monitorBounds.width / 1.3), (int)(monitorBounds.height / 1.3));
	}
	
	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point initialLocation = super.getInitialLocation(initialSize);
		if (initialLocation.x == 0 || initialLocation.y == 0) {
			Composite parent = getShell().getParent();
			Monitor monitor = getShell().getDisplay().getPrimaryMonitor();
			if (parent != null) {
				monitor = parent.getMonitor();
			}
			Rectangle monitorBounds = monitor.getClientArea();
			int x = (monitorBounds.width - initialSize.x)/2;
			int y = (monitorBounds.height - initialSize.y)/2;
			initialLocation = new Point(Math.max(0, x), Math.max(0, y));
		}
		return initialLocation;
	}
	
	@Override
	public int open() {
		JSDeveloperSolutionModel.wizard = this;
		return super.open();
	}
	
	@Override
	public boolean close() {
		JSDeveloperSolutionModel.wizard = null;
		return super.close();
	}
	
	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createContents(Composite parent) {
		// create the top level composite for the dialog
		Composite composite = new Composite(parent, 0);
		GridLayout layout = new GridLayout();
		layout.marginHeight = 0;
		layout.marginWidth = 0;
		layout.verticalSpacing = 0;
		composite.setLayout(layout);
		composite.setLayoutData(new GridData(GridData.FILL_BOTH));
		applyDialogFont(composite);
		// initialize the dialog units
		initializeDialogUnits(composite);
		Browser browser = new Browser(composite, SWT.NONE);
		browser.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));
		ServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		String url = "http://localhost:" + ApplicationServerRegistry.get().getWebServerPort() + "/solutions/" + servoyModel.getFlattenedSolution().getName() +
				"/index.html?svy_design=true";
		browser.setUrl(url);
		return composite;
	}

}
