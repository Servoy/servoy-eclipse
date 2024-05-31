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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.dialogs.IPageChangedListener;
import org.eclipse.jface.dialogs.MessageDialog;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.jface.wizard.WizardDialog;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.CLabel;
import org.eclipse.swt.events.ModifyEvent;
import org.eclipse.swt.events.ModifyListener;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Combo;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.PlatformUI;

import com.servoy.base.persistence.constants.IRepositoryConstants;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.Activator;
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
import com.servoy.j2db.server.shared.ApplicationServerRegistry;

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
	protected void openMediaViewer(MediaNode value)
	{
		Media media = value.getMedia();
		String styleSheet = media.getName();
		int index = styleSheet.indexOf(".less");
		if (index > 0)
		{
			String ng2Filename = styleSheet.substring(0, index) + "_ng2.less";
			Media media2 = ModelUtils.getEditingFlattenedSolution(media.getParent()).getMedia(ng2Filename);
			if (media2 != null)
			{
				media = media2;
			}
		}
		EditorUtil.openMediaViewer(media);
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

					WizardDialog dlg = new WizardDialog(UIUtils.getActiveShell(), wizard);
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
			addLessThemeButton.setText("Add Less Theme Support");
			toggleLessButtonVisibility(dialog, addLessThemeButton);
			addLessThemeButton.addListener(SWT.Selection, event -> {

				AddLessThemeSupportDialog addLessThemeDialog = new AddLessThemeSupportDialog(PlatformUI.getWorkbench().getActiveWorkbenchWindow().getShell(),
					dialog);
				if (addLessThemeDialog.open() == Window.OK)
				{
					addLessThemeButton.setVisible(false);
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

		protected void toggleLessButtonVisibility(final TreeSelectDialog dialog, Button addLessThemeButton)
		{
			if (editingFlattenedSolution.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS) == null)
			{
				addLessThemeButton.setVisible(true);
			}
			else
			{
				StructuredSelection ss = (StructuredSelection)dialog.getSelection();
				Object sel = ss.getFirstElement();
				if (sel == MediaLabelProvider.MEDIA_NODE_NONE)
				{
					addLessThemeButton.setVisible(editingFlattenedSolution.getMedia(editingFlattenedSolution.getName() + ".less") == null);
				}
				else if (sel instanceof MediaNode && ((MediaNode)sel).getMedia() != null)
				{
					MediaNode selected = (MediaNode)sel;
					if (selected.getName().endsWith(".css"))
					{
						addLessThemeButton.setVisible(true);
					}
					else
					{
						String data = new String(selected.getMedia().getMediaData());
						addLessThemeButton.setVisible(!data.contains("@import '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'"));
					}
				}
			}
		}
	}

	private Media addMediaFile(Solution solution, byte[] content, String fileName) throws RepositoryException
	{
		Media defaultTheme = solution.createNewMedia(new ScriptNameValidator(), fileName);
		defaultTheme.setMimeType("text/css");
		defaultTheme.setPermMediaData(content);
		EclipseRepository repository = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
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

	private final class AddLessThemeSupportDialog extends Dialog
	{

		private Combo stylesCombo;
		private final TreeSelectDialog dialog;
		private CLabel description;
		private MediaNode[] elements;
		private MediaContentProvider provider;

		protected AddLessThemeSupportDialog(Shell parentShell, TreeSelectDialog dialog)
		{
			super(parentShell);
			this.dialog = dialog;
			setBlockOnOpen(true);
		}

		@Override
		protected void configureShell(Shell sh)
		{
			super.configureShell(sh);
			sh.setText("Setup a Servoy Less Theme");
			sh.setMinimumSize(500, 300);
		}

		@Override
		protected Control createDialogArea(Composite parent)
		{
			GridLayout gridLayout = new GridLayout(1, false);
			gridLayout.marginHeight = gridLayout.marginWidth = 20;
			Composite composite = new Composite(parent, SWT.NONE);
			composite.setLayout(gridLayout);
			composite.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

			provider = (MediaContentProvider)dialog.getTreeViewer().getContentProvider();
			boolean addCustom = editingFlattenedSolution.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS) == null;
			boolean includeNone = editingFlattenedSolution.getMedia(editingFlattenedSolution.getName() + ".less") == null;
			elements = getAllStyleSheets(provider.getElements(new MediaContentProvider.MediaListOptions(includeNone))).stream().filter(m -> {
				Media media = m.getMedia();
				if (media != null && media.getName().endsWith(".less"))
				{
					if (addCustom) return true;
					String data = new String(media.getMediaData());
					return !data.contains("@import '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'");
				}
				return m == MediaLabelProvider.MEDIA_NODE_NONE || media.getName().endsWith(".css") && !media.getName().endsWith("standard_ngclient.css") &&
					editingFlattenedSolution.getMedia(media.getName().replace(".css", ".less")) == null;
			}).toArray(MediaNode[]::new);
			ILabelProvider labelProvider = (ILabelProvider)dialog.getTreeViewer().getLabelProvider();
			String[] items = Arrays.stream(elements).map(e -> e == MediaLabelProvider.MEDIA_NODE_NONE ? labelProvider.getText(e)
				: e.getPath().replace(e.getName(), "") + labelProvider.getText(e)).toArray(String[]::new);
			Label l = new Label(composite, SWT.NONE);
			l.setText("Select a stylesheet to configure the less theme");
			stylesCombo = new Combo(composite, SWT.READ_ONLY | SWT.BORDER);
			stylesCombo.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, false, 1, 1));
			stylesCombo.setItems(items);

			int index = 0;
			Media currentStyleSheet = null;
			if (editingFlattenedSolution.getSolution().getStyleSheetID() > 0 &&
				(currentStyleSheet = editingFlattenedSolution.getMedia(editingFlattenedSolution.getSolution().getStyleSheetID())) != null)
			{
				index = Arrays.asList(items).indexOf(
					currentStyleSheet.toString() + (!editingFlattenedSolution.getName().equals(currentStyleSheet.getRootObject().getName())
						? " [" + currentStyleSheet.getRootObject().getName() + "]" : ""));
			}
			stylesCombo.select(index >= 0 ? index : 0);
			stylesCombo.addModifyListener(new ModifyListener()
			{
				public void modifyText(ModifyEvent e)
				{
					setDescriptionText();
				}
			});

			description = new CLabel(composite, SWT.WRAP);
			description.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true, 1, 4));
			setDescriptionText();

			return super.createDialogArea(parent);
		}

		private Collection<MediaNode> getAllStyleSheets(Object[] nodes)
		{
			ArrayList<MediaNode> result = new ArrayList<>();
			for (Object n : nodes)
			{
				MediaNode node = (MediaNode)n;
				if (node.getType() == MediaNode.TYPE.FOLDER)
				{
					result.addAll(getAllStyleSheets(provider.getChildren(node)));
				}
				else
				{
					result.add(node);
				}
			}
			return result;
		}

		private void setDescriptionText()
		{
			String msg = "";
			MediaNode selected = elements[stylesCombo.getSelectionIndex()];
			if (selected == MediaLabelProvider.MEDIA_NODE_NONE)
			{
				msg += "A new '" + editingFlattenedSolution.getName() + ".less' file will be created.";
			}
			else
			{
				Media media = selected.getMedia();
				if (media.getName().endsWith(".css"))
				{
					msg += "The selected css file will be renamed to '" + media.getName().replaceAll(".css", ".less") + "'.";
				}
				if (media.getName().endsWith(".less"))
				{
					String data = new String(media.getMediaData());
					if (!data.contains("@import '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'"))
					{
						msg += "The selected file will import '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'.";
					}
				}
			}
			if (editingFlattenedSolution.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS) == null)
			{
				msg += "\nThe default properties file '" + ThemeResourceLoader.CUSTOM_PROPERTIES_LESS + "'\nwill be added to the solution.";
			}
			description.setImage(Activator.getDefault().loadImageFromBundle("info.png"));
			description.setText(msg);
		}

		@Override
		protected void okPressed()
		{
			try
			{
				MediaNode selected = null;
				MediaNode sel = elements[stylesCombo.getSelectionIndex()];
				if (sel == MediaLabelProvider.MEDIA_NODE_NONE)
				{
					Media defaultTheme = addMediaFile(editingFlattenedSolution.getSolution(), ThemeResourceLoader.getDefaultSolutionLess(),
						editingFlattenedSolution.getName() + ".less");
					dialog.refreshTree();
					selected = getSelectedMediaNode((MediaNode[])provider.getElements(new MediaContentProvider.MediaListOptions(false)), defaultTheme.getName(),
						"", editingFlattenedSolution.getName());
				}
				else
				{
					selected = sel;
					boolean changed = false;
					Media media = selected.getMedia();
					if (selected.getName().endsWith(".css"))
					{
						String newName = selected.getName().replace(".css", ".less");
						String oldName = selected.getName();
						String path = selected.getPath().replace(oldName, "");
						if (!path.equals("") && !path.endsWith("/")) path += "/";

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
			super.okPressed();
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
			media.setName(path + newName);
			saveMedia(media);
			return true;
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
	}
}