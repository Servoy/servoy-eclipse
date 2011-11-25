/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2010 Servoy BV

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
package com.servoy.eclipse.ui.util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.filesystem.IFileStore;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Platform;
import org.eclipse.dltk.core.IModelElement;
import org.eclipse.dltk.core.ModelException;
import org.eclipse.dltk.internal.core.ModelElement;
import org.eclipse.dltk.internal.core.SourceField;
import org.eclipse.dltk.internal.core.SourceMethod;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.dialogs.IDialogSettings;
import org.eclipse.jface.text.FindReplaceDocumentAdapter;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.ui.IEditorDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IEditorReference;
import org.eclipse.ui.IEditorSite;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.ide.FileStoreEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.I18NEditorInput;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.core.resource.TableEditorInput;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.util.UUID;

/**
 * Utilities for editors.
 * 
 * @author rgansevles
 */

public class EditorUtil
{
	/**
	 * Opens the javaScript editor file, making a jump to the specified method/variable/calculation (if found).
	 * 
	 * @param activate TODO
	 * 
	 */
	public static IEditorPart openScriptEditor(IPersist persist, String scopeName, boolean activate)
	{
		String scriptPath;
		if (persist instanceof Solution && scopeName != null)
		{
			scriptPath = SolutionSerializer.getRelativePath(persist, false) + scopeName + SolutionSerializer.JS_FILE_EXTENSION;
		}
		else
		{
			scriptPath = SolutionSerializer.getScriptPath(persist, false);
		}
		if (scriptPath == null)
		{
			return null;
		}

		IFile globalsNodeFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
		try
		{
			String elementName = (persist instanceof ISupportName) ? ((ISupportName)persist).getName() : null;
			if (persist instanceof Form && (!globalsNodeFile.exists()))
			{
				// a form that has no .js; as the user wants to open the js for this form - create the file and its folder parents
				new WorkspaceFileAccess(ServoyModel.getWorkspace()).setContents(scriptPath, new byte[0]);
			}
			IModelElement parent = DLTKUIPlugin.getEditorInputModelElement(FileEditorInputFactory.createFileEditorInput(globalsNodeFile));
			IModelElement selection = null;
			if (elementName != null && parent instanceof ModelElement)
			{
				if (persist instanceof ScriptMethod)
				{
					selection = new SourceMethod((ModelElement)parent, elementName);
				}
				else if (persist instanceof ScriptVariable)
				{
					selection = new SourceField((ModelElement)parent, elementName);
				}
				else if (persist instanceof ScriptCalculation)
				{
					selection = new SourceMethod((ModelElement)parent, elementName);
				}
			}
			return DLTKUIPlugin.openInEditor(selection == null ? parent : selection, activate, activate);
		}
		catch (ModelException ex)
		{
			ServoyLog.logError(ex);
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		catch (IOException e)
		{
			ServoyLog.logError("Cannot create form .js file", e);
		}
		return null;
	}

	/**
	 * Open an editor for the form.
	 * 
	 * @param formId
	 */
	public static IEditorPart openFormDesignEditor(Form form)
	{
		return openFormDesignEditor(form, false);
	}

	/**
	 * Open an editor for the form.
	 * @param newForm 
	 * 
	 * @param formId
	 */
	public static IEditorPart openFormDesignEditor(Form form, boolean newForm)
	{
		if (form != null)
		{
			try
			{
				return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
					new PersistEditorInput(form.getName(), form.getSolution().getName(), form.getUUID()).setNew(newForm),
					PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
						Platform.getContentTypeManager().getContentType(PersistEditorInput.FORM_RESOURCE_ID)).getId());
			}
			catch (PartInitException ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return null;
	}


	public static IEditorPart openTableEditor(ITable table)
	{
		if (table == null) return null;
		return openTableEditor(table.getServerName(), table.getName());
	}

	public static IEditorPart openTableEditor(String serverName, String tableName)
	{
		if (serverName == null || tableName == null) return null;
		try
		{
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				new TableEditorInput(serverName, tableName),
				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(TableEditorInput.TABLE_RESOURCE_ID)).getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openValueListEditor(FlattenedSolution flattenedSolution, int valueListId)
	{
		return openValueListEditor(AbstractBase.selectById(flattenedSolution.getValueLists(false), valueListId));
	}

	public static IEditorPart openValueListEditor(ValueList valueList)
	{
		if (valueList == null) return null;
		try
		{
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				new PersistEditorInput(valueList.getName(), valueList.getRootObject().getName(), valueList.getUUID()),
				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(PersistEditorInput.VALUELIST_RESOURCE_ID)).getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openMediaViewer(Media media)
	{
		if (media == null) return null;
		try
		{
			IWorkbenchPage page = PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage();

			String filePath = media.getSerializableRuntimeProperty(IScriptProvider.FILENAME) + System.getProperty("file.separator") + media.getName();

			File fileToOpen = new File(filePath);
			IFileStore fileOnLocalDisk = org.eclipse.core.filesystem.EFS.getLocalFileSystem().getStore(fileToOpen.toURI());

			IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(fileOnLocalDisk.getName());
			if (desc == null) desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
				Platform.getContentTypeManager().getContentType(PersistEditorInput.MEDIA_RESOURCE_ID));
			IEditorInput editorInput;

			if (desc.getId().equals("com.servoy.eclipse.ui.editors.MediaViewer")) editorInput = new PersistEditorInput(media.getName(),
				media.getRootObject().getName(), media.getUUID());
			else editorInput = new FileStoreEditorInput(fileOnLocalDisk);
			return page.openEditor(editorInput, desc.getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openPersistEditor(IPersist persist)
	{
		if (persist == null)
		{
			return null;
		}
		if (persist instanceof IDataProvider)
		{
			return openDataProviderEditor((IDataProvider)persist);
		}
		if (persist instanceof Relation)
		{
			return openRelationEditor((Relation)persist);
		}
		if (persist instanceof Relation)
		{
			return openRelationEditor((Relation)persist);
		}
		if (persist instanceof Style)
		{
			return openStyleEditor((Style)persist);
		}
		if (persist instanceof ValueList)
		{
			return openValueListEditor((ValueList)persist);
		}
		if (persist instanceof Media)
		{
			return openMediaViewer((Media)persist);
		}
		if (persist instanceof IScriptProvider)
		{
			return openScriptEditor(persist, null, true);
		}
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		if (form != null)
		{
			return openFormDesignEditor(form);
		}
		return openScriptEditor(persist, null, true);
	}

	public static IEditorPart openSecurityEditor(IFile f)
	{
		try
		{
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(FileEditorInputFactory.createFileEditorInput(f),
				"com.servoy.eclipse.ui.editors.SecurityEditor");
//				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
//					Platform.getContentTypeManager().getContentType(SecurityEditorInput.SECURITY_RESOURCE_ID)).getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openRelationEditor(Relation relation)
	{
		if (relation == null) return null;
		try
		{
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				new PersistEditorInput(relation.getName(), relation.getRootObject().getName(), relation.getUUID()),
				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(PersistEditorInput.RELATION_RESOURCE_ID)).getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openStyleEditor(Style style)
	{
		IWorkspace workspace = ServoyModel.getWorkspace();
		IFile styleFile = workspace.getRoot().getFile(
			new Path(StringResourceDeserializer.getStringResourceContentFilePath(
				ServoyModelManager.getServoyModelManager().getServoyModel().getActiveResourcesProject().getProject().getName(), style.getName(),
				IRepository.STYLES)));
		IEditorDescriptor editorDescriptor = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(styleFile.getName());
		String editorId;
		if (editorDescriptor == null)
		{
			// use standard text editor
			editorId = "org.eclipse.ui.DefaultTextEditor";
		}
		else
		{
			editorId = editorDescriptor.getId();
		}
		try
		{
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(FileEditorInputFactory.createFileEditorInput(styleFile),
				editorId);
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openStyleEditor(Style style, String lookup)
	{
		IEditorPart editor = openStyleEditor(style);
		if (editor instanceof StructuredTextEditor && lookup != null)
		{
			FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(((StructuredTextEditor)editor).getDocumentProvider().getDocument(
				((StructuredTextEditor)editor).getEditorInput()));
			try
			{
				IRegion region = finder.find(0, lookup, true, true, true, false);
				((StructuredTextEditor)editor).selectAndReveal(region.getOffset(), region.getLength());
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return editor;
	}

	public static IEditorPart openDataProviderEditor(IDataProvider dataProvider)
	{
		if (dataProvider == null)
		{
			return null;
		}
		if (dataProvider instanceof ScriptCalculation || dataProvider instanceof ScriptVariable)
		{
			return openScriptEditor((IPersist)dataProvider, null, true);
		}
		ColumnWrapper cw = dataProvider.getColumnWrapper();
		IEditorPart part = null;
		IColumn column = null;
		if (cw != null && cw.getColumn() != null)
		{
			try
			{
				part = openTableEditor(cw.getColumn().getTable());
				column = cw.getColumn();
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		else if (dataProvider instanceof AggregateVariable)
		{
			try
			{
				part = openTableEditor(((AggregateVariable)dataProvider).getTable());
				column = (AggregateVariable)dataProvider;
			}
			catch (RepositoryException e)
			{
				ServoyLog.logError(e);
			}
		}
		if (part instanceof TableEditor && column != null)
		{
			((TableEditor)part).selectColumn(column);
		}
		return part;
	}


	public static IEditorPart openServerEditor(ServerConfig serverConfig)
	{
		return openServerEditor(serverConfig, false);
	}

	public static IEditorPart openServerEditor(ServerConfig serverConfig, boolean isNew)
	{
		if (serverConfig == null) return null;
		try
		{
			ServerEditorInput sei = new ServerEditorInput(serverConfig);
			sei.setIsNew(isNew);
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				sei,
				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(ServerEditorInput.SERVER_RESOURCE_ID)).getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openI18NEditor(String i18nServer, String i18nTable)
	{
		if (i18nServer == null || i18nTable == null) return null;
		try
		{
			return PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(
				new I18NEditorInput(i18nServer, i18nTable),
				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(I18NEditorInput.I18N_RESOURCE_ID)).getId());
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static void closeEditor(Object object)
	{
		for (IWorkbenchPage page : PlatformUI.getWorkbench().getActiveWorkbenchWindow().getPages())
		{
			for (IEditorReference editorReference : page.getEditorReferences())
			{
				IEditorPart editor = editorReference.getEditor(false);
				if (editor != null)
				{
					if ((object instanceof IPersist && object.equals(editor.getAdapter(IPersist.class))) ||
						(object instanceof Table && object.equals(editor.getAdapter(Table.class))) ||
						((object instanceof IServerInternal) && editor.getAdapter(ServerConfig.class) != null && ((IServerInternal)object).getConfig().getServerName().equals(
							((ServerConfig)editor.getAdapter(ServerConfig.class)).getServerName())))
					{
						page.closeEditor(editor, false);
					}
				}
			}
		}
	}

	public static Form getForm(IEditorPart editorPart)
	{
		if (editorPart == null)
		{
			return null;
		}
		IFile file = (IFile)editorPart.getEditorInput().getAdapter(IFile.class);
		if (file != null)
		{
			try
			{
				IProjectNature nature = file.getProject().getNature(ServoyProject.NATURE_ID);
				if (nature instanceof ServoyProject)
				{
					File workspaceFile = ServoyModel.getWorkspace().getRoot().getRawLocation().toFile();
					File formFile = SolutionSerializer.getParentFile(workspaceFile, file.getRawLocation().toFile());
					if (formFile != null)
					{
						UUID formUuid = SolutionDeserializer.getUUID(formFile);
						IPersist persist = AbstractRepository.searchPersist(((ServoyProject)nature).getSolution(), formUuid);
						if (persist instanceof Form)
						{
							return (Form)persist;
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return null;
	}

	public static IDialogSettings getDialogSettings(String dialogName)
	{
		// store the settings based on the dialog title
		IDialogSettings settings = Activator.getDefault().getDialogSettings();
		if (settings != null && dialogName != null && !"".equals(dialogName))
		{
			IDialogSettings current = null;
			IDialogSettings servoyDialogs = settings.getSection("serclipseDialogs");
			if (servoyDialogs == null) servoyDialogs = settings.addNewSection("serclipseDialogs");
			if (servoyDialogs != null)
			{
				current = servoyDialogs.getSection(dialogName);
				if (current == null)
				{
					current = servoyDialogs.addNewSection(dialogName);
				}
			}
			return current;
		}
		return null;
	}

	public static void saveDirtyEditors(final Shell shell, final boolean prompt)
	{
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					List<IEditorPart> dirtyparts = new ArrayList<IEditorPart>();
					IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
					for (IWorkbenchWindow element : windows)
					{
						IWorkbenchPage[] pages = element.getPages();
						for (IWorkbenchPage element2 : pages)
						{
							IEditorPart[] eparts = element2.getDirtyEditors();
							if (eparts != null && eparts.length > 0) dirtyparts.addAll(Arrays.asList(eparts));
						}
					}
					if (dirtyparts.size() > 0)
					{
						Object[] parts = dirtyparts.toArray();
						if (prompt)
						{
							TreeSelectDialog dialog = new TreeSelectDialog(shell, false, false, TreePatternFilter.FILTER_LEAFS,
								FlatTreeContentProvider.INSTANCE, new LabelProvider()
								{
									@Override
									public String getText(Object element)
									{
										if (element instanceof IEditorPart)
										{
											IEditorPart part = (IEditorPart)element;
											return part.getTitle();
										}
										return super.getText(element);
									}
								}, null, null, SWT.MULTI | SWT.CHECK, "Select editors to save", dirtyparts, new StructuredSelection(dirtyparts), true,
								"saveEditors", null);
							dialog.open();
							if (dialog.getReturnCode() == Window.OK)
							{
								parts = ((IStructuredSelection)dialog.getSelection()).toArray();
							}
							else parts = null;
						}
						if (parts != null)
						{
							for (Object part : parts)
							{
								try
								{
									((IEditorPart)part).doSave(null);
								}
								catch (Exception ex)
								{
									ServoyLog.logError(ex);
								}
							}
						}
					}
				}
				catch (Exception ex)
				{
					ServoyLog.logError(ex);
				}
			}
		});
	}

	/**
	 * Set the status line message for the current active editor
	 * @param message
	 */
	public static void setStatuslineMessage(String message)
	{
		IWorkbenchWindow activeWorkbenchWindow = PlatformUI.getWorkbench().getActiveWorkbenchWindow();
		if (activeWorkbenchWindow == null)
		{
			return;
		}
		IWorkbenchPage activePage = activeWorkbenchWindow.getActivePage();
		if (activePage == null)
		{
			return;
		}
		IWorkbenchPart activePart = activePage.getActivePart();
		if (activePart != null)
		{
			IWorkbenchPartSite site = activePart.getSite();
			if (site instanceof IViewSite)
			{
				((IViewSite)site).getActionBars().getStatusLineManager().setMessage(message);
				return;
			}
		}

		IEditorPart activeEditor = activePage.getActiveEditor();
		if (activeEditor == null)
		{
			return;
		}
		IWorkbenchPartSite site = activeEditor.getSite();
		if (site instanceof IEditorSite)
		{
			((IEditorSite)site).getActionBars().getStatusLineManager().setMessage(message);
		}
	}

	/*
	 * Get the table columns in order as configured in the preferences.
	 */
	public static Iterator<Column> getTableColumns(Table table)
	{
		if (table == null)
		{
			return Collections.<Column> emptyList().iterator();
		}
		if (new DesignerPreferences().getShowColumnsInDbOrder())
		{
			// columns as they appear in the database
			return table.getColumns().iterator();
		}
		// columns sorted by name (PK always first)
		return table.getColumnsSortedByName();
	}
}
