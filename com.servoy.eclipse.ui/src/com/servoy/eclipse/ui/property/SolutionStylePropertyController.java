/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2016 Servoy BV

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

package com.servoy.eclipse.ui.property;

import java.util.EnumSet;

import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.eclipse.ui.dialogs.MediaContentProvider;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor.ListSelectControlFactory;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.wizards.NewStylesheetWizard;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Solution;

/**
 * @author emera
 */
public class SolutionStylePropertyController extends MediaIDPropertyController
{
	public SolutionStylePropertyController(Object id, String displayName, PersistContext persistContext, FlattenedSolution flattenedEditingSolution,
		boolean includeNone, com.servoy.eclipse.ui.property.MediaPropertyController.MediaPropertyControllerConfig config)
	{
		super(id, displayName, persistContext, flattenedEditingSolution, includeNone, config);
	}


	@Override
	protected ListSelectControlFactory getListSelectControlFactory()
	{
		return new ListSelectCellEditor.ListSelectControlFactory()
		{
			private TreeSelectDialog dialog;

			public void setTreeSelectDialog(TreeSelectDialog dialog)
			{
				this.dialog = dialog;
			}

			@Override
			public Control createControl(Composite composite)
			{
				return new StylesheetComposite(composite, dialog, SWT.NONE);
			}
		};
	}

	private static class StylesheetComposite extends Composite
	{

		private final Button addStyleSheetButton;

		StylesheetComposite(Composite parent, final TreeSelectDialog dialog, int style)
		{
			super(parent, style);
			addStyleSheetButton = new Button(this, SWT.NONE);
			addStyleSheetButton.addSelectionListener(new SelectionAdapter()
			{
				@Override
				public void widgetSelected(final SelectionEvent e)
				{
					NewStylesheetWizard wizard = new NewStylesheetWizard();
					IStructuredSelection selection = StructuredSelection.EMPTY;
					ISelection windowSelection = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getSelectionService().getSelection();
					if (windowSelection instanceof IStructuredSelection)
					{
						selection = (IStructuredSelection)windowSelection;
					}
					wizard.init(PlatformUI.getWorkbench(), selection);

					WizardDialog dlg = new WizardDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(), wizard);
					if (wizard instanceof IPageChangedListener)
					{
						dlg.addPageChangedListener((IPageChangedListener)wizard);
					}
					dlg.create();
					if (dlg.open() == Window.OK)
					{
						String mediaName = wizard.getMediaName();
						String path = "".equals(wizard.getPath()) ? "" : wizard.getPath() + "/";
						String solution = wizard.getSolution();

						dialog.refreshTree();
						MediaContentProvider provider = (MediaContentProvider)dialog.getTreeViewer().getContentProvider();
						MediaNode selected = getSelectedMediaNode((MediaNode[])provider.getElements(new MediaContentProvider.MediaListOptions(false)),
							mediaName, path, solution);
						dialog.setSelection(selected);
					}
					wizard.dispose();
				}

				private MediaNode getSelectedMediaNode(MediaNode[] elements, String mediaName, String path, String solution)
				{
					for (MediaNode node : elements)
					{
						if (node.getType() == MediaNode.TYPE.FOLDER && path.startsWith(node.getPath()))
						{
							MediaNode n = getSelectedMediaNode(node.getChildren(EnumSet.of(MediaNode.TYPE.IMAGE, MediaNode.TYPE.FOLDER)), mediaName, path,
								solution);
							if (n != null) return n;
						}
						else if (node.getName().equals(mediaName) && node.getPath().equals(path + mediaName) &&
							((Solution)node.getMedia().getAncestor(IRepositoryConstants.SOLUTIONS)).getName().equals(solution))
						{
							return node;
						}
					}
					return null;
				}
			});

			addStyleSheetButton.setText("Create New Stylesheet");
			final GroupLayout groupLayout = new GroupLayout(this);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().add(10, 10, 10).add(addStyleSheetButton).add(13, 13, 13)));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().add(groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addStyleSheetButton)).addContainerGap()));
			this.setLayout(groupLayout);
		}

	}
}
