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
package com.servoy.eclipse.ui.wizards;

/**
 * Holds all options the user may set during a solution export.
 */
public class ExportSolutionModel implements IExportSolutionModel
{
	private String fileName = null;
	private boolean protectWithPassword = false;
	private boolean exportReferencedModules = false;
	private boolean exportMetaData = true;
	private boolean exportSampleData = false;
	private boolean exportI18NData = false;
	private boolean exportUsers = false;
	private boolean exportAllTablesFromReferencedServers = false;
	private String[] modulesToExport = null;
	private String password = null;
	private int numberOfSampleDataExported = 5000;
	private boolean exportUsingDbiFileInfoOnly = false;

	private String userAcknowledgedFileToOverwrite = null;
	private boolean checkMetadataTables = true; // default

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#getFileName()
	 */
	@Override
	public String getFileName()
	{
		return fileName;
	}

	public void setFileName(String newFileName)
	{
		// If the new file name differs from the old one, then delete any
		// acknowledged file for overwriting.
		if (newFileName == null)
		{
			if (this.fileName != null) userAcknowledgedFileToOverwrite = null;
		}
		else
		{
			if (!newFileName.equals(this.fileName)) userAcknowledgedFileToOverwrite = null;
		}
		this.fileName = newFileName;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isProtectWithPassword()
	 */
	@Override
	public boolean isProtectWithPassword()
	{
		return protectWithPassword;
	}

	public void setProtectWithPassword(boolean protectWithPassword)
	{
		this.protectWithPassword = protectWithPassword;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportReferencedModules()
	 */
	@Override
	public boolean isExportReferencedModules()
	{
		return exportReferencedModules;
	}

	public void setExportReferencedModules(boolean exportReferencedModules)
	{
		this.exportReferencedModules = exportReferencedModules;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportMetaData()
	 */
	@Override
	public boolean isExportMetaData()
	{
		return exportMetaData;
	}

	public void setExportMetaData(boolean exportMetaData)
	{
		this.exportMetaData = exportMetaData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportSampleData()
	 */
	@Override
	public boolean isExportSampleData()
	{
		return exportSampleData;
	}

	public void setExportSampleData(boolean exportSampleData)
	{
		this.exportSampleData = exportSampleData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportI18NData()
	 */
	@Override
	public boolean isExportI18NData()
	{
		return exportI18NData;
	}

	public void setExportI18NData(boolean exportI18NData)
	{
		this.exportI18NData = exportI18NData;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportUsers()
	 */
	@Override
	public boolean isExportUsers()
	{
		return exportUsers;
	}

	public void setExportUsers(boolean exportUsers)
	{
		this.exportUsers = exportUsers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#getModulesToExport()
	 */
	@Override
	public String[] getModulesToExport()
	{
		return modulesToExport;
	}

	public void setModulesToExport(String[] modulesToExport)
	{
		this.modulesToExport = modulesToExport;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#getPassword()
	 */
	@Override
	public String getPassword()
	{
		return password;
	}

	public void setPassword(String password)
	{
		this.password = password;
	}

	public boolean canFinish()
	{
		if (fileName == null || fileName.trim().length() == 0) return false;
		if (protectWithPassword && (password == null || password.length() == 0)) return false;
		return true;
	}

	@Override
	public String toString()
	{
		StringBuilder sb = new StringBuilder("[");
		sb.append("filename: ");
		sb.append(fileName);
		sb.append("\nprotect with password: ");
		sb.append(protectWithPassword);
		sb.append("\nexport referenced modules: ");
		sb.append(exportReferencedModules);
		sb.append("\nexport metadata tables: ");
		sb.append(exportMetaData);
		sb.append("\ncheck metadata tables: ");
		sb.append(checkMetadataTables);
		sb.append("\nexport sample data: ");
		sb.append(exportSampleData);
		if (exportSampleData)
		{
			sb.append("\nexport sample data size: ");
			sb.append(numberOfSampleDataExported);
		}
		sb.append("\nexport i18n data: ");
		sb.append(exportI18NData);
		sb.append("\nexport users: ");
		sb.append(exportUsers);
		sb.append("\nmodules to export: ");
		if (modulesToExport == null) sb.append("null");
		else
		{
			sb.append("[");
			sb.append(modulesToExport.length);
			sb.append("] ");
			for (int i = 0; i < modulesToExport.length; i++)
			{
				if (i > 0) sb.append(", ");
				sb.append(modulesToExport[i]);
			}
		}
		sb.append("\npassword: ");
		sb.append(password);
		sb.append("]");
		return sb.toString();
	}

	public String getUserAcknowledgedFileToOverwrite()
	{
		return userAcknowledgedFileToOverwrite;
	}

	public void setUserAcknowledgedFileToOverwrite(String userAcknowledgedFileToOverwrite)
	{
		this.userAcknowledgedFileToOverwrite = userAcknowledgedFileToOverwrite;
	}

	public void setNumberOfSampleDataExported(int numberOfSampleDataExported)
	{
		this.numberOfSampleDataExported = numberOfSampleDataExported;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#getNumberOfSampleDataExported()
	 */
	@Override
	public int getNumberOfSampleDataExported()
	{
		return numberOfSampleDataExported;
	}

	public void setExportAllTablesFromReferencedServers(boolean exportAllTablesFromReferencedServers)
	{
		this.exportAllTablesFromReferencedServers = exportAllTablesFromReferencedServers;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportAllTablesFromReferencedServers()
	 */
	@Override
	public boolean isExportAllTablesFromReferencedServers()
	{
		return exportAllTablesFromReferencedServers;
	}

	public void setCheckMetadataTables(boolean checkMetadataTables)
	{
		this.checkMetadataTables = checkMetadataTables;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isCheckMetadataTables()
	 */
	@Override
	public boolean isCheckMetadataTables()
	{
		return checkMetadataTables;
	}

	public void setExportUsingDbiFileInfoOnly(boolean exportUsingDbiFileInfoOnly)
	{
		this.exportUsingDbiFileInfoOnly = exportUsingDbiFileInfoOnly;
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see com.servoy.eclipse.ui.wizards.IExportSolutionModel#isExportUsingDbiFileInfoOnly()
	 */
	@Override
	public boolean isExportUsingDbiFileInfoOnly()
	{
		return exportUsingDbiFileInfoOnly;
	}
}
