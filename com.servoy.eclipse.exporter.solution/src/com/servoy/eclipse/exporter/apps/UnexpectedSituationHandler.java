package com.servoy.eclipse.exporter.apps;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.runtime.CoreException;

import com.servoy.eclipse.model.extensions.IUnexpectedSituationHandler;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.util.Utils;

public class UnexpectedSituationHandler implements IUnexpectedSituationHandler
{

	@Override
	public void cannotFindRepository()
	{
		// bummer; this is logged anyway so nothing more to do here
	}

	@Override
	public boolean allowUnexpectedDBIWrite(ITable t)
	{
		ServoyLog.logWarning("Allowed undexpected DBI write for table " + (t != null ? t.getDataSource() : null), null);
		return true;
	}

	@Override
	public void cannotWriteI18NFiles(Exception ex)
	{
		// bummer; this is logged anyway so nothing more to do here
	}

	@Override
	public void writeOverExistingScriptFile(IFile scriptFile, String fileContent)
	{
		try
		{
			scriptFile.setContents(Utils.getUTF8EncodedStream(fileContent), IResource.FORCE, null);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
	}

}
