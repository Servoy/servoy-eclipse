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

import static java.util.Collections.emptyIterator;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.rmi.RemoteException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.eclipse.core.databinding.conversion.Converter;
import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IProjectNature;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
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
import org.eclipse.ui.IWorkbench;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPart;
import org.eclipse.ui.IWorkbenchPartSite;
import org.eclipse.ui.IWorkbenchWindow;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PlatformUI;
import org.eclipse.ui.browser.IWebBrowser;
import org.eclipse.ui.part.FileEditorInput;
import org.eclipse.wst.sse.ui.StructuredTextEditor;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.resource.I18NEditorInput;
import com.servoy.eclipse.core.resource.PersistEditorInput;
import com.servoy.eclipse.core.resource.ServerEditorInput;
import com.servoy.eclipse.core.resource.TableEditorInput;
import com.servoy.eclipse.core.resource.WebPackageManagerEditorInput;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.repository.StringResourceDeserializer;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.Messages;
import com.servoy.eclipse.ui.dialogs.FlatTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.eclipse.ui.dialogs.TreeSelectDialog;
import com.servoy.eclipse.ui.editors.TableEditor;
import com.servoy.eclipse.ui.editors.less.PropertiesLessEditorInput;
import com.servoy.eclipse.ui.preferences.DesignerPreferences;
import com.servoy.eclipse.ui.property.MobileListModel;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.eclipse.ui.views.solutionexplorer.PlatformSimpleUserNode;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.FormElementGroup;
import com.servoy.j2db.persistence.IColumn;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IMediaProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Menu;
import com.servoy.j2db.persistence.MenuItem;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.ServerConfig;
import com.servoy.j2db.persistence.ServerSettings;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.ngclient.less.resources.ThemeResourceLoader;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FilteredIterator;
import com.servoy.j2db.util.Pair;
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
		Form parentForm = (Form)persist.getAncestor(IRepository.FORMS);
		if (parentForm != null && parentForm.isFormComponent())
		{
			// no scripting for form component
			return null;
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
					selection = new SourceField((ModelElement)parent, elementName);
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
		return openFormDesignEditor(form, false, true);
	}

	/**
	 * Open an editor for the form.
	 * @param newForm
	 *
	 * @param formId
	 */
	public static IEditorPart openFormDesignEditor(Form form, boolean newForm, boolean activate)
	{
		if (form != null)
		{
			try
			{
				IWorkbenchPage activePage = getActivePage();
				if (activePage != null)
				{
					return activePage.openEditor(PersistEditorInput.createFormEditorInput(form).setNew(newForm),
						PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
							Platform.getContentTypeManager().getContentType(PersistEditorInput.FORM_RESOURCE_ID)).getId(),
						activate);
				}
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
		return openTableEditor(table, true);
	}

	public static IEditorPart openTableEditor(ITable table, boolean activate)
	{
		if (table == null) return null;
		return openTableEditor(table.getDataSource(), activate);
	}

	public static IEditorPart openTableEditor(String dataSource)
	{
		return openTableEditor(dataSource, true);
	}

	public static IEditorPart openTableEditor(String dataSource, boolean activate)
	{
		if (dataSource == null) return null;
		try
		{
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(new TableEditorInput(dataSource), PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(TableEditorInput.TABLE_RESOURCE_ID)).getId(), activate);
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openValueListEditor(ValueList valueList)
	{
		return openValueListEditor(valueList, true);
	}

	public static IEditorPart openValueListEditor(ValueList valueList, boolean activate)
	{
		if (valueList == null) return null;
		try
		{
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(new PersistEditorInput(valueList.getName(), valueList.getRootObject().getName(), valueList.getUUID()),
					PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
						Platform.getContentTypeManager().getContentType(PersistEditorInput.VALUELIST_RESOURCE_ID)).getId(),
					activate);
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openMediaViewer(Media media)
	{
		return openMediaViewer(media, true);
	}

	public static IEditorPart openMediaViewer(Media media, boolean activate)
	{
		if (media == null) return null;
		try
		{
			IWorkbenchPage page = getActivePage();
			if (page != null)
			{

				Pair<String, String> pathPair = SolutionSerializer.getFilePath(media, false);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);

				IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(media.getName());
				if (desc == null) desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(PersistEditorInput.MEDIA_RESOURCE_ID));
				IEditorInput editorInput;

				if (desc.getId().equals("com.servoy.eclipse.ui.editors.MediaViewer"))
					editorInput = new PersistEditorInput(media.getName(), media.getRootObject().getName(), media.getUUID());
				else editorInput = new FileEditorInput(file);
				return page.openEditor(editorInput, desc.getId(), activate);
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	/**
	 * Open file.
	 * @param resource
	 * @return
	 */
	public static IEditorPart openMenuEditor(IPersist menu, boolean activate)
	{
		if (menu instanceof MenuItem)
		{
			menu = menu.getAncestor(IRepository.MENUS);
		}
		if (menu instanceof Menu mnu)
		{
			try
			{
				IWorkbenchPage activePage = getActivePage();
				if (activePage != null)
				{
					return activePage.openEditor(new PersistEditorInput(mnu.getName(), menu.getRootObject().getName(), menu.getUUID()),
						PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
							Platform.getContentTypeManager().getContentType(PersistEditorInput.MENU_RESOURCE_ID)).getId(),
						activate);
				}
			}
			catch (PartInitException ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return null;
	}

	/**
	 * Open file.
	 * @param resource
	 * @return
	 */
	public static IEditorPart openFileEditor(IFile resource)
	{
		if (resource == null) return null;
		try
		{
			IWorkbenchPage page = getActivePage();
			if (page != null)
			{
				IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(resource.getName());
				if (desc == null)
				{
					if (resource.getName().endsWith(".png") || resource.getName().endsWith(".jpg") || resource.getName().endsWith(".bmp") ||
						resource.getName().endsWith(".gif"))
					{
						PlatformUI.getWorkbench().getBrowserSupport().createBrowser("com.servoy.component.icon.browser").openURL(
							resource.getLocationURI().toURL());
						return null;
					}

					desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
						Platform.getContentTypeManager().getContentType("org.eclipse.core.runtime.text"));
				}
				if (desc != null)
				{
					return page.openEditor(new FileEditorInput(resource), desc.getId(), true);
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openPersistEditor(IPersist persist)
	{
		return openPersistEditor(persist, true);
	}

	public static IEditorPart openPersistEditor(Object model, boolean activate)
	{
		IPersist persist = null;
		if (model instanceof IPersist)
		{
			persist = (IPersist)model;
		}
		else if (model instanceof MobileListModel)
		{
			persist = ((MobileListModel)model).component;
		}
		else if (model instanceof FormElementGroup)
		{
			persist = ((FormElementGroup)model).getParent();
		}
		else if (model instanceof PlatformSimpleUserNode platformSimpleUserNode)
		{
			persist = platformSimpleUserNode.getForm();
		}
		if (persist == null)
		{
			return null;
		}
		if (persist instanceof IDataProvider)
		{
			return openDataProviderEditor((IDataProvider)persist, activate);
		}
		if (persist instanceof Relation)
		{
			return openRelationEditor((Relation)persist, activate);
		}
		if (persist instanceof Style)
		{
			return openStyleEditor((Style)persist, activate);
		}
		if (persist instanceof ValueList)
		{
			return openValueListEditor((ValueList)persist, activate);
		}
		if (persist instanceof Media)
		{
			return openMediaViewer((Media)persist, activate);
		}
		if (persist instanceof Menu || persist instanceof MenuItem)
		{
			return openMenuEditor(persist, activate);
		}
		if (persist instanceof IScriptProvider)
		{
			return openScriptEditor(persist, null, activate);
		}
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		if (form != null)
		{
			return openFormDesignEditor(form, false, activate);
		}
		if (persist instanceof TableNode)
		{
			IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
			return openTableEditor(dsm.getDataSource(((TableNode)persist).getDataSource()), activate);
		}
		return openScriptEditor(persist, null, activate);
	}

	public static IEditorPart openSecurityEditor(IFile f)
	{
		try
		{
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(FileEditorInputFactory.createFileEditorInput(f), "com.servoy.eclipse.ui.editors.SecurityEditor");
//				PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
//					Platform.getContentTypeManager().getContentType(SecurityEditorInput.SECURITY_RESOURCE_ID)).getId());
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openRelationEditor(Relation relation)
	{
		return openRelationEditor(relation, true);
	}

	public static IEditorPart openRelationEditor(Relation relation, boolean activate)
	{
		if (relation == null) return null;
		try
		{
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(new PersistEditorInput(relation.getName(), relation.getRootObject().getName(), relation.getUUID()),
					PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
						Platform.getContentTypeManager().getContentType(PersistEditorInput.RELATION_RESOURCE_ID)).getId(),
					activate);
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openStyleEditor(Style style)
	{
		return openStyleEditor(style, true);
	}

	public static IEditorPart openStyleEditor(Style style, boolean activate)
	{
		IWorkspace workspace = ServoyModel.getWorkspace();
		IFile styleFile = workspace.getRoot().getFile(new Path(StringResourceDeserializer.getStringResourceContentFilePath(
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
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(FileEditorInputFactory.createFileEditorInput(styleFile), editorId, activate);
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static IEditorPart openStyleEditor(Style style, String lookup)
	{
		return openStyleEditor(style, lookup, true);
	}

	public static IEditorPart openStyleEditor(Style style, String lookup, boolean activate)
	{
		IEditorPart editor = openStyleEditor(style, activate);
		if (editor instanceof StructuredTextEditor)
		{
			selectAndReveal((StructuredTextEditor)editor, lookup);
		}
		return editor;
	}

	public static void selectAndReveal(StructuredTextEditor editor, String lookup)
	{
		if (lookup != null)
		{
			FindReplaceDocumentAdapter finder = new FindReplaceDocumentAdapter(editor.getDocumentProvider().getDocument(editor.getEditorInput()));
			try
			{
				IRegion region = finder.find(0, lookup, true, true, true, false);
				if (region != null)
				{
					editor.selectAndReveal(region.getOffset(), region.getLength());
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
	}

	public static IEditorPart openDataProviderEditor(IDataProvider dataProvider)
	{
		return openDataProviderEditor(dataProvider, true);
	}

	public static IEditorPart openDataProviderEditor(IDataProvider dataProvider, boolean activate)
	{
		if (dataProvider == null)
		{
			return null;
		}
		if (dataProvider instanceof ScriptCalculation || dataProvider instanceof ScriptVariable)
		{
			return openScriptEditor((IPersist)dataProvider, null, activate);
		}
		ColumnWrapper cw = dataProvider.getColumnWrapper();
		IEditorPart part = null;
		IColumn column = null;
		if (cw != null && cw.getColumn() != null)
		{
			try
			{
				part = openTableEditor(cw.getColumn().getTable(), activate);
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
				part = openTableEditor(((AggregateVariable)dataProvider).getTable(), activate);
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


	public static IEditorPart openServerEditor(ServerConfig serverConfig, ServerSettings serverSettings)
	{
		return openServerEditor(serverConfig, serverSettings, false);
	}

	public static IEditorPart openServerEditor(ServerConfig serverConfig, ServerSettings serverSettings, boolean isNew)
	{
		if (serverConfig == null || serverSettings == null) return null;
		try
		{
			ServerEditorInput sei = new ServerEditorInput(serverConfig, serverSettings);
			sei.setIsNew(isNew);
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(sei, PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(ServerEditorInput.SERVER_RESOURCE_ID)).getId());
			}
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
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				return activePage.openEditor(new I18NEditorInput(i18nServer, i18nTable), PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(null,
					Platform.getContentTypeManager().getContentType(I18NEditorInput.I18N_RESOURCE_ID)).getId());
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
		return null;
	}

	public static void openWebPackageManager()
	{
		openWebPackageManager(null);
	}

	public static void openWebPackageManager(String solutionName)
	{
		try
		{
			PlatformUI.getWorkbench().getActiveWorkbenchWindow().getActivePage().openEditor(new WebPackageManagerEditorInput(solutionName),
				"com.servoy.eclipse.ui.webpackagemanager");
		}
		catch (PartInitException e)
		{
			Debug.log(e);
		}
	}

	public static void openThemeEditor(final IMediaProvider project)
	{
		Media media = project.getMedia(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS);
		Pair<String, String> pathPair = SolutionSerializer.getFilePath(media, false);
		Path path = new Path(pathPair.getLeft() + pathPair.getRight());
		IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
		try
		{
			IWorkbenchPage activePage = getActivePage();
			if (activePage != null)
			{
				IEditorDescriptor desc = PlatformUI.getWorkbench().getEditorRegistry().getDefaultEditor(ThemeResourceLoader.CUSTOM_PROPERTIES_LESS);
				activePage.openEditor(PropertiesLessEditorInput.createFromFileEditorInput(new FileEditorInput(file)), desc.getId(),
					true);
			}
		}
		catch (PartInitException ex)
		{
			ServoyLog.logError(ex);
		}
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
						(object instanceof ITable && object.equals(editor.getAdapter(ITable.class))) ||
						((object instanceof IServerInternal) && editor.getAdapter(ServerConfig.class) != null &&
							((IServerInternal)object).getConfig().getServerName().equals(editor.getAdapter(ServerConfig.class).getServerName())))
					{
						boolean closed = page.closeEditor(editor, false);
						if (!closed) ServoyLog.logInfo("Could not close editor for" + object.toString());
					}
				}
			}
		}
	}

	public static Form getForm(IEditorPart editorPart)
	{
		if (editorPart == null || ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject() == null)
		{
			return null;
		}
		IFile file = editorPart.getEditorInput().getAdapter(IFile.class);
		if (file != null)
		{
			try
			{
				IProjectNature nature = file.getProject().getNature(ServoyProject.NATURE_ID);
				if (nature instanceof ServoyProject)
				{
					File formFile = SolutionSerializer.getParentFile(
						new WorkspaceFileAccess(ServoyModel.getWorkspace()).getProjectFile(file.getProject().getName()), file.getRawLocation().toFile());
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

	public enum SaveDirtyEditorsOutputEnum
	{
		CANCELED, ALL_SAVED, SOME_SAVED;
	}

	/**
	 * @param shell
	 * @param prompt
	 * @return if it is canceled or not.
	 */
	public static SaveDirtyEditorsOutputEnum saveDirtyEditors(final Shell shell, final boolean prompt)
	{
		final SaveDirtyEditorsOutputEnum[] saveDirtyEditorsOutput = new SaveDirtyEditorsOutputEnum[] { SaveDirtyEditorsOutputEnum.ALL_SAVED };
		Display.getDefault().syncExec(new Runnable()
		{
			public void run()
			{
				try
				{
					int noOfDirtyEditors = 0;
					List<IEditorPart> dirtyParts = getDirtyEditors();
					if (dirtyParts.size() > 0)
					{
						noOfDirtyEditors = dirtyParts.size();
						Object[] parts = dirtyParts.toArray();
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
								}, null, null, SWT.MULTI | SWT.CHECK, "Select editors to save", dirtyParts, new StructuredSelection(dirtyParts), true, null,
								null, false);

							dialog.open();
							if (dialog.getReturnCode() == Window.OK)
							{
								parts = ((IStructuredSelection)dialog.getSelection()).toArray();
							}
							else if (dialog.getReturnCode() == Window.CANCEL)
							{
								saveDirtyEditorsOutput[0] = SaveDirtyEditorsOutputEnum.CANCELED;
								parts = null;
							}
							else
							{
								saveDirtyEditorsOutput[0] = SaveDirtyEditorsOutputEnum.CANCELED;
								parts = null;
							}
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
							if (noOfDirtyEditors > parts.length)
							{
								saveDirtyEditorsOutput[0] = SaveDirtyEditorsOutputEnum.SOME_SAVED;
							}
							else
							{
								saveDirtyEditorsOutput[0] = SaveDirtyEditorsOutputEnum.ALL_SAVED;
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
		return saveDirtyEditorsOutput[0];
	}

	/**
	 * Set the status line message for the current active editor
	 * @param message
	 */
	public static void setStatuslineMessage(String message)
	{
		IWorkbenchPage activePage = getActivePage();
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
	public static Iterator<Column> getTableColumns(ITable table, boolean includeExcluded)
	{
		if (table == null)
		{
			return emptyIterator();
		}
		Iterator<Column> columns;
		if (new DesignerPreferences().getShowColumnsInDbOrder())
		{
			// columns as they appear in the database
			columns = table.getColumns().iterator();
		}
		else
		{
			// columns sorted by name (PK always first)
			columns = table.getColumnsSortedByName();
		}

		if (includeExcluded)
		{
			return columns;
		}
		return new FilteredIterator<>(columns, column -> !((Column)column).hasFlag(IBaseColumn.EXCLUDED_COLUMN));
	}

	public static final String CHILD_TABLE_KEYWORD = "childtable";
	public static final String PARENT_TABLE_KEYWORD = "parenttable";
	public static final String CHILD_COLUMN_KEYWORD = "childcolumn";
	public static final String PARENT_COLUMN_KEYWORD = "parentcolumn";

	public static String getRelationName(String parentTable, String childTable, String parentColumn, String childColumn)
	{
		String loadedRelationsNamingPattern = new DesignerPreferences().getLoadedRelationsNamingPattern();
		if (loadedRelationsNamingPattern != null && loadedRelationsNamingPattern.length() > 0)
		{
			Map<String, String> substitutions = new HashMap<String, String>(4);
			substitutions.put(CHILD_TABLE_KEYWORD, childTable != null ? childTable : "");
			substitutions.put(CHILD_COLUMN_KEYWORD, childColumn != null ? childColumn : "");
			substitutions.put(PARENT_TABLE_KEYWORD, parentTable != null ? parentTable : "");
			substitutions.put(PARENT_COLUMN_KEYWORD, parentColumn != null ? parentColumn : "");

			Matcher matcher = Pattern.compile("\\$\\{(\\w+)\\}").matcher(loadedRelationsNamingPattern.trim());
			StringBuffer stringBuffer = new StringBuffer();
			while (matcher.find())
			{
				String key = matcher.group(1);
				String value = substitutions.get(key);
				matcher.appendReplacement(stringBuffer, value == null ? "??" + key + "??" : value);
			}
			matcher.appendTail(stringBuffer);
			return stringBuffer.toString().toLowerCase();
		}
		return null;
	}

	public static boolean isViewTypeTable(String serverName, String tableName)
	{
		boolean isView = false;
		try
		{
			IServer s = ApplicationServerRegistry.get().getServerManager().getServer(serverName);
			if (s != null) isView = (s.getTableType(tableName) == ITable.VIEW);
		}
		catch (RepositoryException repEx)
		{
			ServoyLog.logError(repEx);
		}
		catch (RemoteException remEx)
		{
			ServoyLog.logError(remEx);
		}
		return isView;
	}

	public static IWorkbenchPage getActivePage()
	{
		IWorkbench workbench = PlatformUI.getWorkbench();
		if (workbench != null)
		{
			IWorkbenchWindow activeWorkbenchWindow = workbench.getActiveWorkbenchWindow();
			if (activeWorkbenchWindow != null)
			{
				return activeWorkbenchWindow.getActivePage();
			}
		}
		return null;
	}

	public static void openURL(IWebBrowser webBrowser, String url) throws Exception
	{
		if (webBrowser != null && url != null)
		{
			webBrowser.openURL(new URL(url));
		}
	}


	public static class Encapsulation2StringConverter extends Converter
	{
		public static final Encapsulation2StringConverter INSTANCE = new Encapsulation2StringConverter();

		private Encapsulation2StringConverter()
		{
			super(int.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject instanceof Integer)
			{
				int enc = ((Integer)fromObject).intValue();
				switch (enc)
				{
					case PersistEncapsulation.DEFAULT :
						return Messages.Public;
					case PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE :
						return Messages.HideInScriptingModuleScope;
					case PersistEncapsulation.MODULE_SCOPE :
						return Messages.ModuleScope;
				}
			}
			return Messages.LabelUnresolved;
		}
	}
	public static class String2EncapsulationConverter extends Converter
	{
		public static final String2EncapsulationConverter INSTANCE = new String2EncapsulationConverter();

		private String2EncapsulationConverter()
		{
			super(String.class, int.class);
		}

		public Object convert(Object fromObject)
		{
			if (Messages.Public.equals(fromObject))
			{
				return Integer.valueOf(PersistEncapsulation.DEFAULT);
			}
			else if (Messages.HideInScriptingModuleScope.equals(fromObject))
			{
				return Integer.valueOf(PersistEncapsulation.HIDE_IN_SCRIPTING_MODULE_SCOPE);
			}
			else if (Messages.ModuleScope.equals(fromObject))
			{
				return Integer.valueOf(PersistEncapsulation.MODULE_SCOPE);
			}
			return new Integer(-1);
		}
	}

	public static class Type2StringConverter extends Converter
	{
		public static final Type2StringConverter INSTANCE = new Type2StringConverter();

		private Type2StringConverter()
		{
			super(int.class, String.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject instanceof Integer)
			{
				int enc = ((Integer)fromObject).intValue();
				if (enc != 0) return Column.getDisplayTypeString(enc);
			}
			return "";
		}

		public String[] getAllTypes()
		{
			int[] iTypes = Column.allDefinedTypes;
			String[] types = new String[iTypes.length];
			for (int i = 0; i < iTypes.length; i++)
			{
				types[i] = Column.getDisplayTypeString(iTypes[i]);
			}
			return types;
		}
	}
	public static class String2TypeConverter extends Converter
	{
		public static final String2TypeConverter INSTANCE = new String2TypeConverter();

		private String2TypeConverter()
		{
			super(String.class, int.class);
		}

		public Object convert(Object fromObject)
		{
			if (fromObject instanceof String)
			{
				int idx = Arrays.asList(Type2StringConverter.INSTANCE.getAllTypes()).indexOf(fromObject);
				if (idx > -1 && idx < Column.allDefinedTypes.length) return new Integer(Column.allDefinedTypes[idx]);
			}

			return new Integer(0);
		}
	}

	/**
	 * The method returns an array of dirty editors from the project.
	 * @return an array of IEditorPart
	 */
	public static List<IEditorPart> getDirtyEditors()
	{
		List<IEditorPart> dirtyParts = new ArrayList<IEditorPart>();
		IWorkbenchWindow[] windows = PlatformUI.getWorkbench().getWorkbenchWindows();
		for (IWorkbenchWindow element : windows)
		{
			IWorkbenchPage[] pages = element.getPages();
			for (IWorkbenchPage element2 : pages)
			{
				IEditorPart[] eparts = element2.getDirtyEditors();
				if (eparts != null && eparts.length > 0) dirtyParts.addAll(Arrays.asList(eparts));
			}
		}
		return dirtyParts;
	}

	public static boolean hideDefaultComponents(Form form)
	{
		boolean hideDefault = !PlatformUI.getPreferenceStore().getBoolean("com.servoy.eclipse.designer.rfb.show.default.package");
		if (form != null && !form.isResponsiveLayout() && hideDefault)
		{
			Iterator<ISupportFormElement> elements = form.getFormElementsSortedByFormIndex();
			while (elements.hasNext() && hideDefault)
			{
				hideDefault = hideDefault && (elements.next() instanceof WebComponent);
			}
		}
		return hideDefault;
	}
}
