/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2025 Servoy BV

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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Iterator;
import java.util.jar.Manifest;

import org.eclipse.jface.action.Action;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.sablo.specification.Package.IPackageReader;

//import com.servoy.eclipse.knowledgebase.IKnowledgeBaseOperations;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.node.SimpleUserNode;
import com.servoy.eclipse.ui.views.solutionexplorer.SolutionExplorerView;

/**
 * Action to reload all AI knowledge bases (embeddings and rules) from all installed
 * knowledge base packages. Clears existing knowledge and reloads fresh from all bundles.
 *
 * This prevents duplicates and ensures clean state.
 *
 * @author mvid
 */
public class ReloadAllKnowledgeBasesAction extends Action implements ISelectionChangedListener
{
	private final SolutionExplorerView viewer;

	public ReloadAllKnowledgeBasesAction(SolutionExplorerView viewer, String text)
	{
		super();
		this.viewer = viewer;
		setText(text);
		setToolTipText(text);
	}

	@Override
	public void selectionChanged(SelectionChangedEvent event)
	{
		IStructuredSelection sel = (IStructuredSelection)event.getSelection();
		boolean state = false;
		Iterator<SimpleUserNode> it = sel.iterator();
		if (it.hasNext())
		{
			SimpleUserNode node = it.next();
			state = isKnowledgeBasePackage(node);
		}
		setEnabled(state);
	}

	/**
	 * Validates if the selected node is a valid knowledge base package.
	 *
	 * Checks:
	 * 1. Node != null
	 * 2. Has MANIFEST.MF
	 * 3. MANIFEST.MF contains Knowledge-Base: true
	 * 4. Has embeddings/ and rules/ directories (with embeddings.list and rules.list)
	 */
	private boolean isKnowledgeBasePackage(SimpleUserNode node)
	{
		try
		{
			if (node != null && (node.getRealObject() instanceof IPackageReader packageReader))
			{
				String packageName = packageReader.getPackageName();
				if (packageName != null && checkManifestForKnowledgeBase(packageReader, packageName))
				{
					URL embeddingsList = packageReader.getUrlForPath("embeddings/embeddings.list");
					URL rulesList = packageReader.getUrlForPath("rules/rules.list");

					if (embeddingsList != null || rulesList != null)
					{
						return true;
					}
				}
			}
		}
		catch (MalformedURLException e)
		{
			ServoyLog.logError(e.getMessage());
		}
		return false;
	}

	private boolean checkManifestForKnowledgeBase(IPackageReader packageReader, String packageName)
	{
		try
		{
			Manifest manifest = packageReader.getManifest();
			if (manifest != null)
			{
				return "true".equalsIgnoreCase(manifest.getMainAttributes().getValue("Knowledge-Base"));
			}
		}
		catch (Exception e)
		{
			ServoyLog.logError("Error reading MANIFEST.MF for package: " + packageName, e);
		}
		return false;
	}

	@Override
	public void run()
	{
		Object ops = getKnowledgeBaseOperations();
		if (ops != null)
		{
			try
			{
				ops.getClass().getMethod("reloadAllKnowledgeBases").invoke(ops);
			}
			catch (Exception e)
			{
				ServoyLog.logError("Failed to reload all knowledge bases", e);
			}
		}
		else
		{
			ServoyLog.logError("Knowledge base operations provider not found. Ensure com.servoy.eclipse.knowledgebase plugin is installed.");
		}
	}

	/**
	 * Gets the knowledge base operations provider via extension point.
	 * @return the operations provider instance or null if not available
	 */
	private Object getKnowledgeBaseOperations()
	{
//		IExtensionRegistry reg = Platform.getExtensionRegistry();
//		IExtensionPoint ep = reg.getExtensionPoint(IKnowledgeBaseOperations.EXTENSION_ID);
//		if (ep == null)
//		{
//			return null;
//		}
//
//		IExtension[] extensions = ep.getExtensions();
//		if (extensions != null && extensions.length > 0)
//		{
//			IConfigurationElement[] ce = extensions[0].getConfigurationElements();
//			if (ce != null && ce.length > 0)
//			{
//				try
//				{
//					return ce[0].createExecutableExtension("class");
//				}
//				catch (CoreException e)
//				{
//					ServoyLog.logError("Failed to create knowledge base operations provider", e);
//				}
//			}
//		}
		return null;
	}
}
