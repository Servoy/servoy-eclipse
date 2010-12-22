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

package com.servoy.eclipse.ui.search;

import java.util.regex.Pattern;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.dltk.core.DLTKCore;
import org.eclipse.dltk.core.IField;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.core.search.IDLTKSearchConstants;
import org.eclipse.search.core.text.TextSearchEngine;
import org.eclipse.search.core.text.TextSearchRequestor;
import org.eclipse.search.ui.ISearchQuery;
import org.eclipse.search.ui.text.FileTextSearchScope;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;

/**
 * An {@link ISearchQuery} implementation for finding relations in frm and js files.
 * 
 * @author jcompagner
 * @since 6.0
 */
public class ScriptVariableSearch extends DLTKSearchEngineSearch
{

	private final ScriptVariable variable;

	public ScriptVariableSearch(ScriptVariable variable)
	{
		this.variable = variable;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#run(org.eclipse.core.runtime.IProgressMonitor)
	 */
	@SuppressWarnings("nls")
	public IStatus run(IProgressMonitor monitor) throws OperationCanceledException
	{
		IResource[] scopes = getScopes((Solution)variable.getRootObject());
		final TextSearchRequestor collector = getResultCollector();

		if (variable.getParent() instanceof Solution)
		{
			FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm", "*.rel" }, true);
			TextSearchEngine.create().search(scope, collector, Pattern.compile("\\bglobals." + variable.getName() + "\\b"), monitor);
		}
		else
		{
			FileTextSearchScope scope = FileTextSearchScope.newSearchScope(scopes, new String[] { "*.frm" }, true);
			TextSearchEngine.create().search(scope, collector, Pattern.compile("\\b" + variable.getName() + "\\b"), monitor);
		}

		String scriptPath = SolutionSerializer.getScriptPath(variable, false);
		IFile file = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
		ISourceModule sourceModule = DLTKCore.createSourceModuleFrom(file);
		IField variableElement = sourceModule.getField(variable.getName());
		callDLTKSearchEngine(monitor, collector, variableElement, IDLTKSearchConstants.REFERENCES, (Solution)variable.getRootObject());
		return Status.OK_STATUS;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.search.ui.ISearchQuery#getLabel()
	 */
	public String getLabel()
	{
		return "Searching references to variable '" + variable.getName() + "'"; //$NON-NLS-1$ //$NON-NLS-2$
	}
}
