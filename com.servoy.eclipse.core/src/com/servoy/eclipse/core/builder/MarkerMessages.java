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
package com.servoy.eclipse.core.builder;

import java.text.MessageFormat;

/**
 * Be careful if you use ' inside the String constants listed in this class.
 * 
 * @see http://java.sun.com/j2se/1.5.0/docs/api/java/util/Formatter.html#syntax
 */
public class MarkerMessages
{
	/*
	 * The following messages seem to no longer appear. They are filtered out by other checks and actions. Specific reasons are listed where I could find them.
	 */
	// If no resource project is found for a solution, the solution never gets 
	// loaded (and so the message never gets displayed).
	public static String Marker_Resources_ReferencesToMultipleResources = "Solution project \"{0}\" has references to more than one Servoy Resources Projects."; //$NON-NLS-1$
	public static String Marker_Resources_NoResourceReference = "Solution project \"{1}\" has no Servoy Resources Project referenced."; //$NON-NLS-1$
	// It seems that elements with duplicate UUIDs are not loaded at all.
	public static String Marker_Duplicate_UUIDDuplicateIn = "UUID duplicate found \"{0}\" in %s."; //$NON-NLS-1$
	// Seems to be superseded by the Marker_Form_Solution_PropertyTargetNotFound family of messages.
	public static String Marker_PropertyFormCannotBeInstantiated = "Property \"{0}\" refers to a form that cannot be instantiated."; //$NON-NLS-1$
	// Not sure in which conditions the valuelist returns an invalid type, because the type is calculated
	// and not stored.
	public static String Marker_Valuelist_TypeUnknown = "Valuelist \"{0}\" has unknown type: {1}."; //$NON-NLS-1$
	// These may probably appear, but I didn't find scenarios when to catch them.
	public static String Marker_Relation_GenericError = "Relation \"{0}\" is referring to a server table or column which does not exist."; //$NON-NLS-1$
	public static String Marker_Valuelist_GenericError = "Exception while checking valuelist \"{0}\"."; //$NON-NLS-1$
	// These two seem to be superseded by Marker_Solution_ServerNotAccessibleFirstOccurence.
	public static String Marker_Relation_PrimaryServerWithProblems = "Relation \"{0}\" is referring to an invalid/disabled/unknown primary server \"{1}\""; //$NON-NLS-1$
	public static String Marker_Relation_ForeignServerWithProblems = "Relation \"{0}\" is referring to an invalid/disabled/unknown foreign server \"{1}\"."; //$NON-NLS-1$


	/*
	 * The items below should work fine, they were tested.
	 */

	/**
	 * When this message appears it means that the content in the files in the solution folder is 
	 * corrupted. 
	 */
	public static String Marker_Structure_BadStructure = "Structure of the files for solution \"{0}\" is broken (incorrect parent-child combination)."; //$NON-NLS-1$

	/**
	 * When this message appears it means that some solution file is corrupted and no longer conforms to the JSON syntax.
	 */
	public static String Marker_Deserialize_Error = "Error while reading solution \"{0}\": {1}."; //$NON-NLS-1$

	public static String Marker_Fix_RemoveCustomValues = "Removed custom values."; //$NON-NLS-1$
	public static String Marker_Fix_RemoveDatasource = "Removed datasource."; //$NON-NLS-1$
	public static String Marker_Fix_RemovedDBInfo = "Removed table, server and relation."; //$NON-NLS-1$
	public static String Marker_Advice_EntityManuallyMoved = "Entity \"{0}\" has been manually moved?"; //$NON-NLS-1$

	public static String Marker_Sort_InvalidSortOptionsRelationNotFound = "{0} \"{1}\" has invalid sort options \"{2}\" , relation \"{3}\" can not be found."; //$NON-NLS-1$
	public static String Marker_Sort_InvalidSortOptionsRelationDifferentPrimaryDatasource = "{0} \"{1}\" has invalid sort options \"{2}\" , relation \"{3}\" has different primary datasource than expected."; //$NON-NLS-1$
	public static String Marker_Sort_InvalidSortOptionsColumnNotFound = "{0} \"{1}\" has invalid sort options \"{2}\" , column \"{3}\" does not exist."; //$NON-NLS-1$

	/**
	 * This means that two or more entities have the same name. This can create various kinds of conflicts and should be avoided.
	 */
	public static String Marker_Duplicate_DuplicateEntityFound = "Duplicate {0} found \"{1}\" in {2}."; //$NON-NLS-1$

	/**
	 * This means that two or more entities have the same UUID. This can create various kinds of conflicts and should be avoided.
	 */
	public static String Marker_Duplicate_UUIDDuplicate = "UUID duplicate found \"{0}\"."; //$NON-NLS-1$

	public static String Marker_Valuelist_DBServerDuplicate = "Valuelist \"{0}\" is based on server \"{1}\" which is a duplicate."; //$NON-NLS-1$
	public static String Marker_Valuelist_DBWithCustomValues = "Valuelist \"{0}\" is a database valuelist but it also specifies custom values."; //$NON-NLS-1$
	public static String Marker_Valuelist_DBNotTableOrRelation = "Valuelist \"{0}\" is a database valuelist but it does not specify relation or table."; //$NON-NLS-1$

	/**
	 * This means that the datasource definition for the valuelist does not respect the proper syntaxt and the server and table names cannot be extracted from it.
	 */
	public static String Marker_Valuelist_DBMalformedTableDefinition = "Valuelist \"{0}\" is based on malformed table definition \"{1}\"."; //$NON-NLS-1$
	public static String Marker_Valuelist_DBTableNotAccessible = "Valuelist \"{0}\" is based on table \"{1}\" which is not accessible."; //$NON-NLS-1$
	public static String Marker_Valuelist_DBDatasourceNotFound = "Valuelist \"{0}\" is based on inexisting column/calculation \"{1}\" from table \"{2}\"."; //$NON-NLS-1$
	public static String Marker_Valuelist_RelationWithDatasource = "Valuelist \"{0}\" is relation based so it should not specify a datasource."; //$NON-NLS-1$
	public static String Marker_Valuelist_RelationNotFound = "Valuelist \"{0}\" is based on relation \"{1}\" which is not found."; //$NON-NLS-1$
	public static String Marker_Valuelist_RelationSequenceInconsistent = "Valuelist \"{0}\" is based on relation sequence \"{1}\" which is not consistent."; //$NON-NLS-1$
	public static String Marker_Valuelist_CustomValuesWithDBInfo = "Valuelist \"{0}\" is custom values valuelist so it should not specify table, server or relation."; //$NON-NLS-1$
	public static String Marker_Valuelist_GlobalMethodNotFound = "Valuelist \"{0}\" references an inexisting global method."; //$NON-NLS-1$
	public static String Marker_Valuelist_InvalidCustomValues = "Valuelist \"{0}\" has invalid custom value (all or none rows should have real value)."; //$NON-NLS-1$

	public static String Marker_Style_NotFound = "Style \"{0}\" used in form \"{1}\" does not exist."; //$NON-NLS-1$
	public static String Marker_Style_FormClassNotFound = "Style class \"{0}\" used in form \"{1}\" does not exist."; //$NON-NLS-1$
	public static String Marker_Style_ElementClassNotFound = "Style class \"{0}\" from element \"{1}\" in form \"{2}\" does not exist."; //$NON-NLS-1$
	public static String Marker_Style_FormClassNoStyle = "Style class \"{0}\" in form \"{1}\" is set but no style is assigned to form."; //$NON-NLS-1$
	public static String Marker_Style_ElementClassNoStyle = "Style class \"{0}\" from element \"{1}\" in form \"{2}\" is set but no style is assigned to form."; //$NON-NLS-1$

	public static String Marker_Media_TIFF = "Media {0} will not display correctly in Webclient since many browsers do not support TIFF image format."; //$NON-NLS-1$

	/**
	 * This means that one of the modules referenced by the project could not be found in the workspace. This can happen if the module was manually deleted from the workspace folder.
	 */
	public static String Marker_Module_ModuleNotFound = "Module \"{0}\" which is referenced by project \"{1}\" does not exist."; //$NON-NLS-1$

	public static String Marker_i18n_ModuleDifferentI18NTable = "Module \"{0}\" has a different i18n table than main solution \"{1}\"."; //$NON-NLS-1$

	public static String Marker_Resources_ModuleDifferentResourceProject = "Module \"{0}\" of solution \"{1}\" references a different Servoy Resources Project."; //$NON-NLS-1$

	/**
	 * This means that a table that is referenced by an entity could not be accessed. In most cases this means that the table does not exist, but it can
	 * also mean that some exception occured while accessing an existing table.
	 */
	public static String Marker_Table_ItemReferencesInvalidTable = "{0} \"{1}\" references invalid table \"{2}\"."; //$NON-NLS-1$

	public static String Marker_Calculation_FormAccess = "Form access found in calculation \"{0}\". This is not supported."; //$NON-NLS-1$
	public static String Marker_Calculation_InTableFormAccess = "Form access found in calculation \"{0}\" from table \"{1}\". This is not supported."; //$NON-NLS-1$
	public static String Marker_Calculation_UndeclaredVariable = "Calculation \"{0}\" contains an undeclared variable \"{1}\" which may lead to unexpected results (in tableview). Declare the variable first."; //$NON-NLS-1$

	public static String Marker_Form_Solution_PropertyTargetNotFound = "Property \"{0}\" is linked to an entity that does not exist."; //$NON-NLS-1$
	public static String Marker_Form_Solution_PropertyOnElementTargetNotFound = "Property \"{0}\" from element \"{1}\" is linked to an entity that does not exist."; //$NON-NLS-1$
	public static String Marker_Form_Solution_PropertyInFormTargetNotFound = "Property \"{0}\" in form \"{1}\" is linked to an entity that does not exist."; //$NON-NLS-1$
	public static String Marker_Form_Solution_PropertyOnElementInFormTargetNotFound = "Property \"{0}\" from element \"{1}\" in form \"{2}\" is linked to an entity that does not exist."; //$NON-NLS-1$

	public static String Marker_Form_TableNoPK = "Form \"{0}\" is based on table \"{1}\" which does not have a primary key."; //$NON-NLS-1$
	public static String Marker_Form_RowBGCalcTargetNotFound = "rowBGColorCalculation of form \"{0}\" is linked to an entity that does not exist."; //$NON-NLS-1$
	public static String Marker_Form_DuplicatePart = "Form \"{0}\" has multiple parts of type: \"{1}\"."; //$NON-NLS-1$

	/** 
	 * This means that the sam tab sequence position is assigned to more than one element on a form.
	 */
	public static String Marker_Form_UnnamedElementDuplicateTabSequence = "Form \"{0}\" contains an element which has duplicate tab sequence."; //$NON-NLS-1$

	/** 
	 * This means that the sam tab sequence position is assigned to more than one element on a form.
	 */
	public static String Marker_Form_NamedElementDuplicateTabSequence = "Form \"{0}\" contains an element \"{1}\" which has duplicate tab sequence."; //$NON-NLS-1$

	public static String Marker_Form_DerivedFormRedefinedVariable = "Form \"{0}\" has a variable \"{1}\" which is also present in the parent hierarchy of the form."; //$NON-NLS-1$
	public static String Marker_Form_VariableTableCol = "Form \"{0}\" has a variable \"{1}\" which is also a column in table \"{2}\"."; //$NON-NLS-1$
	public static String Marker_Form_TableNotAccessible = "Form \"{0}\" is based on table \"{1}\" which is not accessible."; //$NON-NLS-1$

	/**
	 * This means that when using form inheritance a cycle was introduced in the dependency relation of some forms. This is not allowed.
	 */
	public static String Marker_Form_ExtendsCycle = "Form \"{0}\" is part of a cycle through \"extendsForm\" property."; //$NON-NLS-1$
	public static String Marker_Form_DerivedFormDifferentTable = "Form \"{0}\" which extends form \"{1}\" does not have the same table as its parent."; //$NON-NLS-1$
	public static String Marker_Form_UnnamedElementOutsideBoundsOfForm = "Element in form \"{0}\" is outside the bounds of form."; //$NON-NLS-1$
	public static String Marker_Form_NamedElementOutsideBoundsOfForm = "Element \"{0}\" in form \"{1}\" is outside the bounds of form."; //$NON-NLS-1$
	public static String Marker_Form_UnnamedElementOutsideBoundsOfPart = "Element in form \"{0}\" is outside the bounds of part {1}."; //$NON-NLS-1$
	public static String Marker_Form_NamedElementOutsideBoundsOfPart = "Element \"{0}\" in form \"{1}\" is outside the bounds of part {2}."; //$NON-NLS-1$
	public static String Marker_Form_TypeAheadNamedUnstoredCalculation = "Type ahead field \"{0}\" has attached a valuelist that contains unstored calculation(s). This is not supported."; //$NON-NLS-1$
	public static String Marker_Form_TypeAheadUnnamedUnstoredCalculation = "Type ahead field has attached a valuelist that contains unstored calculation(s). This is not supported."; //$NON-NLS-1$

	public static String Marker_Form_EditableNamedComboboxCustomValuelist = "Editable combobox \"{0}\" has attached a valuelist that contains real values. This is not supported."; //$NON-NLS-1$
	public static String Marker_Form_EditableUnnamedComboboxCustomValuelist = "Editable combobox has attached a valuelist that contains real values. This is not supported."; //$NON-NLS-1$

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static String Marker_Form_PortalUnnamedElementUnnamedMismatchedRelation = "Element from portal has relation sequence \"{0}\" while portal has relationName \"{1}\". It should be the same value."; //$NON-NLS-1$

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static String Marker_Form_PortalUnnamedElementNamedMismatchedRelation = "Element \"{0}\" from portal has relation sequence \"{1}\" while portal has relationName \"{2}\". It should be the same value."; //$NON-NLS-1$

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static String Marker_Form_PortalNamedElementUnnamedMismatchedRelation = "Element from portal \"{0}\" has relation sequence \"{1}\" while portal has relationName \"{2}\". It should be the same value."; //$NON-NLS-1$

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static String Marker_Form_PortalNamedElementNamedMismatchedRelation = "Element \"{0}\" from portal \"{1}\" has relation sequence \"{2}\" while portal has relationName \"{3}\". It should be the same value."; //$NON-NLS-1$

	/**
	 * This means that the relations that build up the relation sequence of the portal do not fit together properly. At some point in the 
	 * sequence two relations are joined, but not on the same table.
	 */
	public static String Marker_Form_PortalUnnamedInvalidName = "Portal has invalid relationName (relation chain not correct)."; //$NON-NLS-1$

	/**
	 * This means that the relations that build up the relation sequence of the portal do not fit together properly. At some point in the 
	 * sequence two relations are joined, but not on the same table.
	 */
	public static String Marker_Form_PortalNamedInvalidName = "Portal \"{0}\" has invalid relationName (relation chain not correct)."; //$NON-NLS-1$

	/**
	 * This means that in a tabpanel you have a tab based on a certain relation, but the form displayed in the tab is based on a table
	 * that is different from the child table of the relation.
	 */
	public static String Marker_Form_RelatedTabDifferentTable = "Related tab error: form \"{0}\" is based on a different table then relation \"{1}\"."; //$NON-NLS-1$

	/**
	 * This means that the relation sequence of a tab inside a tabpanel is invalid. This can happen when a relation from the relation sequence
	 * cannot be found, or when two adiacent relations in the relation sequence do not fit together properly (they are being joined on diffent tables).
	 */
	public static String Marker_Form_RelatedTabUnsolvedRelation = "Related tab error: cannot resolve relation sequence \"{0}\"."; //$NON-NLS-1$

	public static String Marker_Form_PropertyMethodNotAccessible = "Property \"{0}\" in form \"{1}\" is linked to a non accessible method (method belongs to form \"{2}\")."; //$NON-NLS-1$
	public static String Marker_Form_PropertyOnElementMethodNotAccessible = "Property \"{0}\" from element \"{1}\" in form \"{2}\" is linked to a non accessible method (method belongs to form \"{3}\")."; //$NON-NLS-1$
	public static String Marker_Form_DataproviderNotBasedOnFormTable = "Element in form \"{0}\" has dataprovider \"{1}\" that is not based on form table."; //$NON-NLS-1$
	public static String Marker_Form_DataproviderOnElementNotBasedOnFormTable = "Element \"{0}\" in form \"{1}\" has dataprovider \"{2}\" that is not based on form table."; //$NON-NLS-1$

	/**
	 * Aggregates are non-editable values and thus should not be displayed in editable fields.
	 */
	public static String Marker_Form_DataproviderAggregateNotEditable = "Element in form \"{0}\" has dataprovider \"{1}\" which is an aggregate and cannot be editable."; //$NON-NLS-1$

	/**
	 * Aggregates are non-editable values and thus should not be displayed in editable fields.
	 */
	public static String Marker_Form_DataproviderOnElementAggregateNotEditable = "Element \"{0}\" in form \"{1}\" has dataprovider \"{2}\" which is an aggregate and cannot be editable."; //$NON-NLS-1$

	public static String Marker_Form_FormatInvalid = "Element in form \"{0}\" has invalid format: \"{1}\"."; //$NON-NLS-1$
	public static String Marker_Form_FormatOnElementInvalid = "Element \"{0}\" in form \"{1}\" has invalid format: \"{2}\"."; //$NON-NLS-1$

	/**
	 * The dataprovider used by an element cannot be found. This can happen for example if the structure of a database is changed and columns are deleted or renamed.
	 */
	public static String Marker_Form_DataproviderNotFound = "Element in form \"{0}\" has dataprovider \"{1}\" that does not exist."; //$NON-NLS-1$

	/**
	 * The dataprovider used by an element cannot be found. This can happen for example if the structure of a database is changed and columns are deleted or renamed.
	 */
	public static String Marker_Form_IncompatibleElementType = "Element \"{0}\" in form \"{1}\" has incompatible dataprovider type (MEDIA)."; //$NON-NLS-1$

	/**
	 * The HTML or RTF field has as dataprovider a column whose length may be too small.
	 */
	public static String Marker_Form_ColumnLengthTooSmall = "HTML/RTF element \"{0}\" in form \"{1}\" has column dataprovider with small length. This may cause unpredictable results(if length of value will get bigger than column length)."; //$NON-NLS-1$

	/**
	 * The dataprovider used by an element cannot be found. This can happen for example if the structure of a database is changed and columns are deleted or renamed.
	 */
	public static String Marker_Form_DataproviderOnElementNotFound = "Element \"{0}\" in form \"{1}\" has dataprovider \"{2}\" that does not exist."; //$NON-NLS-1$

	public static String Marker_Column_UUIDFlagNotSet = "Table \"{0}\" has column \"{1}\" which is an UUID generator but does not have the UUID flag set."; //$NON-NLS-1$

	/**
	 * The lookup value used for a column is invalid. This can happen when the lookup points to a global identifier that does not exist,
	 * or when the lookup contains a relation that does not exist, or a relation sequence that does not fit together properly, or a column
	 * that does not exist.
	 */
	public static String Marker_Column_LookupInvalid = "Table \"{0}\" has column \"{1}\" which has invalid lookup value."; //$NON-NLS-1$

	public static String Marker_DBI_ColumnMissingFromDB = "Column \"{0}\" appears in the DB information file, but it is missing from the DB table."; //$NON-NLS-1$
	public static String Marker_DBI_ColumnMissingFromDBIFile = "Column \"{0}\" appears in the DB table, but it is missing from the DB information file."; //$NON-NLS-1$
	public static String Marker_DBI_ColumnConflict = "\"{0}\" column difference. {1} in DB <-> {2} in DB information file. The DB information on this column differs from the actual column in the DB."; //$NON-NLS-1$
	public static String Marker_DBI_TableMissing = "Table \"{0}\" is missing from the DB (there is a database information file for this table)."; //$NON-NLS-1$
	public static String Marker_DBI_DBIFileMissing = "Missing database information file for table \"{0}\" (table exists in the DB)."; //$NON-NLS-1$
	public static String Marker_DBI_GenericError = "Some kind of column difference between the DB and the DB information files for column \"{0}\"."; //$NON-NLS-1$
	public static String Marker_DBI_BadDBInfo = "Bad database information: {0}"; //$NON-NLS-1$

	public static String Marker_Solution_ServerNotAccessibleFirstOccurence = "Solution \"{0}\" references server \"{1}\" which is not accessible (first occurence error)."; //$NON-NLS-1$
	public static String Marker_Solution_ElementNameInvalidIdentifier = "Element has name \"{0}\" which is not a valid identifier."; //$NON-NLS-1$

	public static String Marker_Relation_PrimaryTableNotFound = "Relation \"{0}\" is referring to inexistent primary table with name \"{1}\" on server \"{2}\"."; //$NON-NLS-1$
	public static String Marker_Relation_PrimaryTableWithoutPK = "Relation \"{0}\" is referring to primary table with name \"{1}\" on server \"{2}\" but the primary table does not have a primary key."; //$NON-NLS-1$
	public static String Marker_Relation_PrimaryServerDuplicate = "Relation \"{0}\" is referring to duplicate primary server \"{1}\"."; //$NON-NLS-1$
	public static String Marker_Relation_ForeignTableNotFound = "Relation \"{0}\" is referring to inexistent foreign table with name \"{1}\" on server \"{2}\"."; //$NON-NLS-1$
	public static String Marker_Relation_ForeignTableWithoutPK = "Relation \"{0}\" is referring to foreign table with name \"{1}\" on server \"{2}\" but the foreign table does not have a primary key."; //$NON-NLS-1$
	public static String Marker_Relation_ForeignServerDuplicate = "Relation \"{0}\" is referring to a duplicate foreign server \"{1}\"."; //$NON-NLS-1$
	public static String Marker_Relation_ItemNoPrimaryDataprovider = "Relation \"{0}\" has an item without primary dataprovider."; //$NON-NLS-1$
	public static String Marker_Relation_ItemPrimaryDataproviderNotFound = "Relation \"{0}\" has a primary dataprovider \"{1}\" which is not found."; //$NON-NLS-1$
	public static String Marker_Relation_ItemNoForeignDataprovider = "Relation \"{0}\" has an item without foreign dataprovider."; //$NON-NLS-1$
	public static String Marker_Relation_ItemForeignDataproviderNotFound = "Relation \"{0}\" has a foreign column \"{1}\" which is not found."; //$NON-NLS-1$

	/**
	 * The relation does not contain any comparison keys. Each relation should have at least one comparison key.
	 */
	public static String Marker_Relation_Empty = "Relation \"{0}\" has no keys (is empty)."; //$NON-NLS-1$

	/**
	 * The relation contains a comparison key where the types of the two involved dataproviders do not fit together properly.
	 */
	public static String Marker_Relation_ItemTypeProblem = "Relation \"{0}\" has a relation item with mismatched keys: {1}."; //$NON-NLS-1$
	public static String Marker_Relation_ItemUUIDProblem = "Relation \"{0}\" has a relation item where UUID flag is set for only one column (\"{1}\" - \"{2}\"). Either none or both columns should have the UUID flag set."; //$NON-NLS-1$

	public static String Marker_Method_EventParameters = "Event parameter is passed to event method, make sure it is used with right type (change method signature)."; //$NON-NLS-1$

	public static String getMessage(String key, Object... args)
	{
		return MessageFormat.format(key, args);
	}

}
