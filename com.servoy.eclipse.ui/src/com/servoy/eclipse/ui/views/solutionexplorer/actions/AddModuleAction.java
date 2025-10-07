/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2011 Servoy BV

 This program is free software; you can redistribute it and/or modify it under
 the terms of the GNU Affero General Public License as published by the Free
 Software Foundation; either version 3 of the License, or (at your option) any
 later version.

 This program is distributed in the hope that it will be useful, but WITHOUT
 ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS
 FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License for more details.

 You should have received a copy of the GNU Affero General Public License along
 with this program; if not, see http://www.gnu.org/licenses or write to the Free
 Software Foundation,Inc., 51 Franklin Street, Fifth Floor, Boston, MA 02110-1301
 */

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Shell;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.labelproviders.ArrayLabelProvider;
import com.servoy.eclipse.ui.labelproviders.ValidvalueDelegatelabelProvider;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.node.UserNodeType;
import com.servoy.eclipse.ui.property.StringTokenizerConverter;
import com.servoy.eclipse.ui.resource.FontResource;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.Utils;

/**
 * Class that allows the selection of modules to be added to the currently active solution.
 *
 * @author acostache
 *
 */
public class AddModuleAction extends Action implements ISelectionChangedListener
{
	private final Shell shell;

	public AddModuleAction(Shell shell)
	{
		this.shell = shell;
		setText("Add Module");
		setToolTipText("Add a module to the solution");
		setImageDescriptor(Activator.loadImageDescriptorFromBundle("add_as_module.png"));
	}

	@Override
	public void run()
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		List<String> availableSolutions = new ArrayList<String>();
		try
		{
			for (RootObjectMetaData rootObject : ApplicationServerRegistry.get().getDeveloperRepository().getRootObjectMetaDatas())
			{
				if (rootObject.getObjectTypeId() == IRepository.SOLUTIONS &&
					!rootObject.getName().equals(servoyModel.getActiveProject().getSolution().getName()))
				{
					availableSolutions.add(rootObject.getName());
				}
			}
		}
		catch (Exception ex)
		{
			Debug.error(ex);
		}

		StringTokenizerConverter converter = new StringTokenizerConverter(",", true);
		List<String> allSolutions = new ArrayList<String>(availableSolutions);

		String[] modulesNames = converter.convertProperty("moduleNames", servoyModel.getActiveProject().getSolution().getModulesNames());

		StructuredSelection theSelection = null;
		if (modulesNames == null) theSelection = StructuredSelection.EMPTY;
		else
		{
			theSelection = new StructuredSelection(modulesNames);
			for (String module : modulesNames)
			{
				if (!allSolutions.contains(module))
				{
					allSolutions.add(module);
				}
			}
		}
		Collections.sort(allSolutions, String.CASE_INSENSITIVE_ORDER);

		ILabelProvider labelProvider;
		if (allSolutions.size() == availableSolutions.size())
		{
			labelProvider = new ArrayLabelProvider(converter);
		}
		else
		{
			labelProvider = new ValidvalueDelegatelabelProvider(new ArrayLabelProvider(converter), availableSolutions, null,
				FontResource.getDefaultFont(SWT.ITALIC, 0));
		}

		ITreeContentProvider contentProvider = FlatTreeContentProvider.INSTANCE;
		IFilter selectionFilter = new LeafnodesSelectionFilter(contentProvider);

		int treeStyle = SWT.MULTI | SWT.CHECK;

		TreeSelectDialog dialog = new TreeSelectDialog(shell, false, false, TreePatternFilter.FILTER_LEAFS, contentProvider, labelProvider, null,
			selectionFilter, treeStyle, "Select modules", allSolutions.toArray(), theSelection, true, "Select modules", null, false);

		dialog.open();

		if (dialog.getReturnCode() == Window.CANCEL) return;

		Object[] selectedProjsArray = ((IStructuredSelection)dialog.getSelection()).toArray();

		ServoyProject activeSolution = servoyModel.getActiveProject();
		if (activeSolution == null) return;

		Solution editingSolution = activeSolution.getEditingSolution();
		if (editingSolution != null)
		{
			String modulesTokenized = Utils.getTokenValue(selectedProjsArray, ",");
			editingSolution.setModulesNames(modulesTokenized);
			try
			{
				activeSolution.saveEditingSolutionNodes(new IPersist[] { editingSolution }, false);
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError("Cannot save new module list for active module " + activeSolution.getProject().getName(), e);
			}
		}
	}

	public void selectionChanged(SelectionChangedEvent event)
	{
		IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = (sel.size() > 0) && (servoyModel.getActiveProject() != null);
		if (state)
		{
			SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
			state = (node.getType() == UserNodeType.MODULES);
		}
		setEnabled(state);
	}
}
