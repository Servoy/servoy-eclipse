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
package com.servoy.eclipse.ui.editors.table;

import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;

/**
 * 
 * @author asisu
 * 
 */
public class ColumnInfoBean
{
	protected final ColumnInfo columnInfo;

	public ColumnInfoBean(ColumnInfo columnInfo)
	{
		this.columnInfo = columnInfo;
	}


	public int getAutoEnterSubType()
	{
		return columnInfo.getAutoEnterSubType();
	}

	public String getAutoEnterSubTypeString(int mainType, int subType)
	{
		return columnInfo.getAutoEnterSubTypeString(mainType, subType);
	}

	public int getAutoEnterType()
	{
		return columnInfo.getAutoEnterType();
	}

	public String getConverterName()
	{
		return columnInfo.getConverterName();
	}

	public String getConverterProperties()
	{
		return columnInfo.getConverterProperties();
	}

	public String getDatabaseDefaultValue()
	{
		return columnInfo.getDatabaseDefaultValue();
	}

	public String getDatabaseSequenceName()
	{
		return columnInfo.getDatabaseSequenceName();
	}

	public String getDefaultFormat()
	{
		return columnInfo.getDefaultFormat();
	}

	public String getDefaultValue()
	{
		return columnInfo.getDefaultValue();
	}

	public String getDescription()
	{
		return columnInfo.getDescription();
	}

	public String getDataProviderID()
	{
		return columnInfo.getDataProviderID();
	}

	public String getElementTemplateProperties()
	{
		return columnInfo.getElementTemplateProperties();
	}

	public String getForeignType()
	{
		return columnInfo.getForeignType();
	}

	public int getID()
	{
		return columnInfo.getID();
	}

	public String getLookupValue()
	{
		return columnInfo.getLookupValue();
	}

	public long getNextSequence()
	{
		return columnInfo.getNextSequence();
	}

	public String getPostSequenceChars()
	{
		return columnInfo.getPostSequenceChars();
	}

	public String getPreSequenceChars()
	{
		return columnInfo.getPreSequenceChars();
	}

	public int getSequenceStepSize()
	{
		return columnInfo.getSequenceStepSize();
	}

	public String getTextualPropertyInfo(boolean html)
	{
		return columnInfo.getTextualPropertyInfo(html);
	}

	public String getTitleText()
	{
		return columnInfo.getTitleText();
	}

	public String getValidatorName()
	{
		return columnInfo.getValidatorName();
	}

	public String getValidatorProperties()
	{
		return columnInfo.getValidatorProperties();
	}

	public boolean hasSequence()
	{
		return columnInfo.hasSequence();
	}

	public boolean hasSystemValue()
	{
		return columnInfo.hasSystemValue();
	}

	public boolean isDBIdentity()
	{
		return columnInfo.isDBIdentity();
	}

	public boolean isDBManaged()
	{
		return columnInfo.isDBManaged();
	}

	public boolean isDBSequence()
	{
		return columnInfo.isDBSequence();
	}

	public boolean isExcluded()
	{
		return columnInfo.isExcluded();
	}

	public boolean isStoredPersistently()
	{
		return columnInfo.isStoredPersistently();
	}

	public void setAutoEnterSubType(int t)
	{
		columnInfo.setAutoEnterSubType(t);
	}

	public void setAutoEnterType(int t)
	{
		columnInfo.setAutoEnterType(t);
	}

	public void setConverterName(String s)
	{
		columnInfo.setConverterName(s);
	}

	public void setConverterProperties(String s)
	{
		columnInfo.setConverterProperties(s);
	}

	public void setDatabaseSequenceName(String databaseSequenceName)
	{
		columnInfo.setDatabaseSequenceName(databaseSequenceName);
	}

	public void setDefaultFormat(String s)
	{
		columnInfo.setDefaultFormat(s);
	}

	public void setDefaultValue(String s)
	{
		columnInfo.setDefaultValue(s);
	}

	public void setDescription(String s)
	{
		columnInfo.setDescription(s);
	}

	public void setDataProviderID(String s)
	{
		columnInfo.setDataProviderID(s);
	}

	public void setElementTemplateProperties(String s)
	{
		columnInfo.setElementTemplateProperties(s);
	}

	public void setForeignType(String s)
	{
		columnInfo.setForeignType(s);
	}

	public void setLookupValue(String s)
	{
		columnInfo.setLookupValue(s);
	}

	public void setNextSequence(long l)
	{
		columnInfo.setNextSequence(l);
	}

	public void setPostSequenceChars(String s)
	{
		columnInfo.setPostSequenceChars(s);
	}

	public void setPreSequenceChars(String s)
	{
		columnInfo.setPreSequenceChars(s);
	}

	public void setSequenceStepSize(int s)
	{
		columnInfo.setSequenceStepSize(s);
	}

	public void setTitleText(String s)
	{
		columnInfo.setTitleText(s);
	}

	public void setValidatorName(String s)
	{
		columnInfo.setValidatorName(s);
	}

	public void setValidatorProperties(String s)
	{
		columnInfo.setValidatorProperties(s);
	}

	public boolean getExcludedFlag()
	{
		return (columnInfo.getFlags() & Column.EXCLUDED_COLUMN) == Column.EXCLUDED_COLUMN;
	}

	public void setExcludedFlag(boolean excluded)
	{
		columnInfo.setFlag(Column.EXCLUDED_COLUMN, excluded);
		columnInfo.flagChanged();
	}

	public boolean getUuidFlag()
	{
		return (columnInfo.getFlags() & Column.UUID_COLUMN) == Column.UUID_COLUMN;
	}

	public void setUuidFlag(boolean uuid)
	{
		columnInfo.setFlag(Column.UUID_COLUMN, uuid);
		columnInfo.flagChanged();
	}

}