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
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.MediaContentProvider;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor.ListSelectControlFactory;
import com.servoy.eclipse.ui.labelproviders.MediaLabelProvider;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.eclipse.ui.util.MediaNode;
import com.servoy.eclipse.ui.wizards.NewStylesheetWizard;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.ValidatorSearchContext;
import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;

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

	private class StylesheetComposite extends Composite
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
			});
			addStyleSheetButton.setText("Create New Stylesheet");

			Button addLessThemeButton = new Button(this, SWT.NONE);
			addLessThemeButton.setText("Add Less Theme");
			enableDisableLessButton(dialog, addLessThemeButton);
			dialog.getTreeViewer().addSelectionChangedListener(e -> {
				enableDisableLessButton(dialog, addLessThemeButton);
			});
			MediaContentProvider provider = (MediaContentProvider)dialog.getTreeViewer().getContentProvider();
			addLessThemeButton.addListener(SWT.Selection, event -> {
				try
				{
					StructuredSelection ss = (StructuredSelection)dialog.getSelection();
					Object sel = ss.getFirstElement();

					MediaNode selected = null;
					if (sel == MediaLabelProvider.MEDIA_NODE_NONE)
					{
						Media defaultTheme = addMediaFile(editingFlattenedSolution.getSolution(), ThemeResourceLoader.getDefaultSolutionLess(),
							editingFlattenedSolution.getName() + ".less");
						dialog.refreshTree();
						selected = getSelectedMediaNode((MediaNode[])provider.getElements(new MediaContentProvider.MediaListOptions(false)),
							defaultTheme.getName(), "", editingFlattenedSolution.getName());
					}
					else if (sel instanceof MediaNode)
					{
						selected = (MediaNode)sel;
						boolean changed = false;
						Media media = selected.getMedia();
						if (selected.getName().endsWith(".css"))
						{
							String newName = selected.getName().replace(".css", ".less");
							String oldName = selected.getName();
							String path = selected.getPath().replace(oldName, "");
							if (!path.equals("")) path += "/";

							changed = renameCssToLess(media, newName, oldName, path);
							if (!changed) return;

							dialog.refreshTree();
							selected = getSelectedMediaNode((MediaNode[])provider.getElements(new MediaContentProvider.MediaListOptions(false)), newName, path,
								media.getRootObject().getName());
							changed = true;
						}

						changed = addCustomPropertiesImport(media) || changed;
						if (changed) EditorUtil.openMediaViewer(media);
					}

					if (selected != null)
					{
						dialog.setSelection(selected);
						addLessThemeButton.setEnabled(false);
						if (editingFlattenedSolution.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS) == null)
						{
							addMediaFile((Solution)selected.getMedia().getRootObject(), ThemeResourceLoader.getCustomProperties(),
								ThemeResourceLoader.CUSTOM_PROPERTIES_LESS);
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
			});

			final GroupLayout groupLayout = new GroupLayout(this);
			groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(
				groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(addStyleSheetButton).addPreferredGap(LayoutStyle.RELATED).add(
					addLessThemeButton).addContainerGap()));
			groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().add(
				groupLayout.createParallelGroup(GroupLayout.BASELINE).add(addStyleSheetButton).add(addLessThemeButton)).addContainerGap()));
			this.setLayout(groupLayout);
		}

		protected void enableDisableLessButton(final TreeSelectDialog dialog, Button addLessThemeButton)
		{
			if (editingFlattenedSolution.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS) == null)
			{
				addLessThemeButton.setEnabled(true);
			}
			else
			{
				StructuredSelection ss = (StructuredSelection)dialog.getSelection();
				Object sel = ss.getFirstElement();
				if (sel == MediaLabelProvider.MEDIA_NODE_NONE)
				{
					addLessThemeButton.setEnabled(editingFlattenedSolution.getMedia(editingFlattenedSolution.getName() + ".less") == null);
				}
				else if (sel instanceof MediaNode)
				{
					MediaNode selected = (MediaNode)sel;
					if (selected.getName().endsWith(".css"))
					{
						addLessThemeButton.setEnabled(true);
					}
					else
					{
						String data = new String(selected.getMedia().getMediaData());
						addLessThemeButton.setEnabled(!data.contains("@import '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'"));
					}
				}
			}
		}

		protected boolean addCustomPropertiesImport(Media media)
		{
			String data = new String(media.getMediaData());
			if (!data.contains("@import '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'"))
			{
				StringBuilder sb = new StringBuilder();
				sb.append(new String(ThemeResourceLoader.getDefaultSolutionLess()) + "\n");
				data = data.replace("@import \"standard_ngclient.css\";", "");
				sb.append(data);
				media.setPermMediaData(sb.toString().getBytes());
				media.flagChanged();
				saveMedia(media);
				return true;
			}
			return false;
		}

		protected boolean renameCssToLess(Media media, String newName, String oldName, String path)
		{
			try
			{
				ServoyModelManager.getServoyModelManager().getServoyModel().getNameValidator().checkName(path + newName, -1,
					new ValidatorSearchContext(IRepository.MEDIA), false);
			}
			catch (RepositoryException e)
			{
				Display.getDefault().asyncExec(new Runnable()
				{
					public void run()
					{
						MessageDialog.openError(getShell(), "Cannot add less theme to " + oldName, e.getMessage());
					}
				});
				return false;
			}

			EditorUtil.closeEditor(media);
			media.setName(newName);
			saveMedia(media);
			return true;
		}
	}

	private Media addMediaFile(Solution solution, byte[] content, String fileName) throws RepositoryException
	{
		Media defaultTheme = solution.createNewMedia(new ScriptNameValidator(), fileName);
		defaultTheme.setMimeType("text/css");
		defaultTheme.setPermMediaData(content);
		EclipseRepository repository = (EclipseRepository)ServoyModel.getDeveloperRepository();
		repository.updateRootObject(solution);
		return defaultTheme;
	}

	private MediaNode getSelectedMediaNode(MediaNode[] elements, String mediaName, String path, String solution)
	{
		for (MediaNode node : elements)
		{
			if (node.getType() == MediaNode.TYPE.FOLDER && path.startsWith(node.getPath()))
			{
				MediaNode n = getSelectedMediaNode(node.getChildren(EnumSet.of(MediaNode.TYPE.IMAGE, MediaNode.TYPE.FOLDER)), mediaName, path, solution);
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

	protected void saveMedia(Media media)
	{
		try
		{
			ServoyProject project = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(editingFlattenedSolution.getName());
			project.saveEditingSolutionNodes(new IPersist[] { media }, true);
		}
		catch (RepositoryException e)
		{
			ServoyLog.logError(e);
		}
	}
}
