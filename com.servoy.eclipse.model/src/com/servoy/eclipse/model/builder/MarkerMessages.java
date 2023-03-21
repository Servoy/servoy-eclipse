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
package com.servoy.eclipse.model.builder;

import java.text.MessageFormat;

/**
 * Be careful if you use ' inside the String constants listed in this class.
 *
 * @see http://java.sun.com/j2se/1.5.0/docs/api/java/util/Formatter.html#syntax
 */
public class MarkerMessages
{
	public static class ServoyMarker
	{
		private final String template;
		private final String type;
		private final String message;
		private final String fix;

		public ServoyMarker(String template, String type)
		{
			this(template, type, null, null);
		}

		public ServoyMarker(String template, String type, String fix)
		{
			this(template, type, fix, null);
		}

		/*
		 * Internal, used with formatted message
		 */
		private ServoyMarker(String template, String type, String fix, String message)
		{
			this.template = template;
			this.type = type;
			this.message = message;
			this.fix = fix;
		}

		public ServoyMarker fill(Object... values)
		{
			return new ServoyMarker(template, type, fix, MessageFormat.format(template, values));
		}

		public String getType()
		{
			return type;
		}

		public String getText()
		{
			return message != null ? message : template;
		}

		public String getFix()
		{
			return fix;
		}
	}

	/*
	 * The following messages seem to no longer appear. They are filtered out by other checks and actions. Specific reasons are listed where I could find them.
	 */
	// If no resource project is found for a solution, the solution never gets
	// loaded (and so the message never gets displayed).
	public static ServoyMarker ReferencesToMultipleResources = new ServoyMarker(
		"Solution project \"{0}\" has references to more than one Servoy Resources Projects.", ServoyBuilder.MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE);
	// It seems that elements with duplicate UUIDs are not loaded at all.
	public static ServoyMarker UUIDDuplicateIn = new ServoyMarker("UUID duplicate found \"{0}\" of {1} in {2}, duplicates: {3} in {4}.",
		ServoyBuilder.DUPLICATE_UUID);
	// Seems to be superseded by the Marker_Form_Solution_PropertyTargetNotFound family of messages.
	public static ServoyMarker PropertyFormCannotBeInstantiated = new ServoyMarker("Property \"{0}\" refers to a form that cannot be instantiated.",
		ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);
	public static ServoyMarker PropertyMultipleMethodsOnSameElement = new ServoyMarker(
		"Element \"{0}\" has the same method attached to multiple events/commands.", ServoyBuilder.MULTIPLE_METHODS_ON_SAME_ELEMENT);
	public static ServoyMarker PropertyMultipleMethodsOnSameTable = new ServoyMarker("Table \"{0}\" has the same method attached to multiple events.",
		ServoyBuilder.MULTIPLE_METHODS_ON_SAME_ELEMENT);

	// Not sure in which conditions the valuelist returns an invalid type, because the type is calculated
	// and not stored.
	public static ServoyMarker ValuelistTypeUnknown = new ServoyMarker("Valuelist \"{0}\" has unknown type: {1}.", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	// These may probably appear, but I didn't find scenarios when to catch them.
	public static ServoyMarker RelationGenericError = new ServoyMarker("Relation \"{0}\" is referring to a server table or column which does not exist.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationGenericErrorWithDetails = new ServoyMarker(
		"Relation \"{0}\" is referring to a server table or column which does not exist: {1}.", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker ValuelistGenericError = new ServoyMarker("Exception while checking valuelist \"{0}\".",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistGenericErrorWithDetails = new ServoyMarker("Exception while checking valuelist \"{0}\": {1}.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	// These two seem to be superseded by Marker_Solution_ServerNotAccessibleFirstOccurence.
	public static ServoyMarker RelationPrimaryServerWithProblems = new ServoyMarker(
		"Relation \"{0}\" is referring to an invalid/disabled/unknown primary server \"{1}\"", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationForeignServerWithProblems = new ServoyMarker(
		"Relation \"{0}\" is referring to an invalid/disabled/unknown foreign server \"{1}\".", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);


	/**
	 * This message appears when the number of arguments of a script method which is used on an element of a form is lower than the the number of arguments stored in the custom properties of that element.
	 */
	public static ServoyMarker EventHandlerSignatureMismatch = new ServoyMarker("Signature mismatch between {0} and its use as event handler {1} on {2}{3}.",
		ServoyBuilder.METHOD_NUMBER_OF_ARGUMENTS_MISMATCH_TYPE);

	public static ServoyMarker ColumnRowIdentShouldNotAllowNull = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which has row identity set but column allows null.Make sure column data is never null.",
		ServoyBuilder.COLUMN_MARKER_TYPE);
	/*
	 * The items below should work fine, they were tested.
	 */

	/**
	 * When this message appears it means that the content in the files in the solution folder is
	 * corrupted.
	 */
	public static ServoyMarker SolutionBadStructure = new ServoyMarker(
		"Structure of the files for solution \"{0}\" is broken (incorrect parent-child combination).", ServoyBuilder.BAD_STRUCTURE_MARKER_TYPE);

	/**
	 * When this message appears it means that some solution file is corrupted and no longer conforms to the JSON syntax.
	 */
	public static ServoyMarker SolutionDeserializeError = new ServoyMarker("Error while reading solution \"{0}\": {1}.",
		ServoyBuilder.PROJECT_DESERIALIZE_MARKER_TYPE);

	/**
	 * This message appears when a checked-out solution/module has a higher fileVersion than the current version of the repository.
	 */
	public static ServoyMarker SolutionWithHigherFileVersion = new ServoyMarker("{0} \"{1}\" has a higher version of Servoy.",
		ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);

	public static ServoyMarker SolutionBadStructure_EntityManuallyMoved = new ServoyMarker(
		"Structure of the files for solution \"{0}\" is broken (incorrect parent-child combination). Entity \"{1}\" has been manually moved?",
		ServoyBuilder.BAD_STRUCTURE_MARKER_TYPE);

	public static ServoyMarker InvalidSortOptionsRelationNotFound = new ServoyMarker(
		"{0} \"{1}\" has invalid sort options \"{2}\" , relation \"{3}\" can not be found.", ServoyBuilder.INVALID_SORT_OPTION);
	public static ServoyMarker InvalidSortOptionsRelationDifferentPrimaryDatasource = new ServoyMarker(
		"{0} \"{1}\" has invalid sort options \"{2}\" , relation \"{3}\" has a different primary datasource than expected.", ServoyBuilder.INVALID_SORT_OPTION);
	public static ServoyMarker InvalidSortOptionsColumnNotFound = new ServoyMarker(
		"{0} \"{1}\" has invalid sort options \"{2}\" , column \"{3}\" does not exist.", ServoyBuilder.INVALID_SORT_OPTION);

	/**
	 * This means that two or more entities have the same name. This can create various kinds of conflicts and should be avoided.
	 */
	public static ServoyMarker DuplicateEntityFound = new ServoyMarker("Duplicate {0} found \"{1}\" in {2}.", ServoyBuilder.DUPLICATE_NAME_MARKER_TYPE);

	public static ServoyMarker DuplicateReferencedFormFound = new ServoyMarker(
		"Duplicate referenced form \"{0}\" found in form \"{1}\". This is not supported.", ServoyBuilder.DUPLICATE_REFERENCED_FORM_MARKER_TYPE);

	public static ServoyMarker DuplicateOverrideFound = new ServoyMarker(
		"Duplicate form element found \"{0}\" in {1}. This is caused by wrong parent of override element.", ServoyBuilder.WRONG_OVERRIDE_PARENT);

	/**
	 * This means that for a mobile solution, they use for variables names that are reserved because of the window object in a browser.
	 */
	public static ServoyMarker ReservedWindowObjectProperty = new ServoyMarker("Reserved browser window object property name \"{0}\" found.",
		ServoyBuilder.RESERVED_WINDOW_OBJECT_USAGE_TYPE);

	/**
	 * This means that for a mobile solution, they use table column names that are reserved because of the window object in a browser.
	 */
	public static ServoyMarker ReservedWindowObjectColumn = new ServoyMarker("Reserved browser window object table column name \"{0}\" found.",
		ServoyBuilder.RESERVED_WINDOW_OBJECT_USAGE_TYPE);

	/**
	 * This means that a solution is used as a web service, and has mustAuthenticate property checked.
	 */
	public static ServoyMarker SolutionUsedAsWebServiceMustAuthenticateProblem = new ServoyMarker(
		"Solution \"{0}\" has mustAuthenticate property checked while used as a web service.", ServoyBuilder.SERVICE_MUST_AUTHENTICATE_MARKER_TYPE);

	/**
	 * This means that two or more scopes have the same name. This can create various kinds of conflicts and should be avoided.
	 */
	public static ServoyMarker DuplicateScopeFound = new ServoyMarker("Duplicate scope \"{0}\" found in {1}.", ServoyBuilder.DUPLICATE_SCOPE_NAME_MARKER_TYPE);

	/**
	 * This means that two or more entities have the same UUID. This can create various kinds of conflicts and should be avoided.
	 */
	public static ServoyMarker UUIDDuplicate = new ServoyMarker("UUID duplicate found \"{0}\".", ServoyBuilder.DUPLICATE_SIBLING_UUID);

	public static ServoyMarker ValuelistDBServerDuplicate = new ServoyMarker("Valuelist \"{0}\" is based on server \"{1}\" which is a duplicate.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistDBWithCustomValues = new ServoyMarker("Valuelist \"{0}\" is a database valuelist but it also specifies custom values.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE, "Removed custom values.");
	public static ServoyMarker ValuelistDBNotTableOrRelation = new ServoyMarker(
		"Valuelist \"{0}\" is a database valuelist but it does not specify relation or table.", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);

	/**
	 * This means that the datasource definition for the valuelist does not respect the proper syntaxt and the server and table names cannot be extracted from it.
	 */
	public static ServoyMarker ValuelistDBMalformedTableDefinition = new ServoyMarker("Valuelist \"{0}\" is based on malformed table definition \"{1}\".",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistDBTableNotAccessible = new ServoyMarker("Valuelist \"{0}\" is based on table \"{1}\" which is not accessible.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistDBTableNoPk = new ServoyMarker("Valuelist \"{0}\" is based on table \"{1}\" which does not have a primary key.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistDBDatasourceNotFound = new ServoyMarker(
		"Valuelist \"{0}\" is based on nonexisting column/calculation \"{1}\" from table \"{2}\".", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistRelationWithDatasource = new ServoyMarker("Valuelist \"{0}\" is relation based so it should not specify a datasource.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE, "Removed datasource.");
	public static ServoyMarker ValuelistRelationNotFound = new ServoyMarker("Valuelist \"{0}\" is based on relation \"{1}\" which is not found.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistRelationSequenceInconsistent = new ServoyMarker(
		"Valuelist \"{0}\" is based on relation sequence \"{1}\" which is not consistent.", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistCustomValuesWithDBInfo = new ServoyMarker(
		"Valuelist \"{0}\" is a custom valuelist so it should not specify table, server or relation.", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE,
		"Removed table, server and relation.");
	public static ServoyMarker ValuelistGlobalMethodNotFound = new ServoyMarker("Valuelist \"{0}\" references a nonexisting global method.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistGlobalMethodNotAccessible = new ServoyMarker(
		"Valuelist \"{0}\" references a private global method that is not accessible from this solution.", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistInvalidCustomValues = new ServoyMarker(
		"Valuelist \"{0}\" has invalid custom value (either all rows or no rows should have a real value).", ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistFallbackOfFallbackFound = new ServoyMarker(
		"Valuelist \"{0}\" has fallback valuelist \"{1}\" which has a fallback valuelist as well. This is not supported.",
		ServoyBuilder.PROJECT_VALUELIST_MARKER_TYPE);
	public static ServoyMarker ValuelistDataproviderTypeMismatch = new ServoyMarker(
		"Valuelist \"{0}\" real value type \"{1}\" does not match the dataProvider type \"{2}\" of element \"{3}\" in form \"{4}\"",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker StyleNotFound = new ServoyMarker("Style \"{0}\" used in form \"{1}\" does not exist.", ServoyBuilder.MISSING_STYLE);
	public static ServoyMarker StyleFormClassNotFound = new ServoyMarker("Style class \"{0}\" used in form \"{1}\" does not exist.",
		ServoyBuilder.MISSING_STYLE);
	public static ServoyMarker StyleElementClassNotFound = new ServoyMarker("Style class \"{0}\" from element \"{1}\" in form \"{2}\" does not exist.",
		ServoyBuilder.MISSING_STYLE);
	public static ServoyMarker StyleFormClassNoStyle = new ServoyMarker("Style class \"{0}\" in form \"{1}\" is set but no style is assigned to form.",
		ServoyBuilder.MISSING_STYLE);
	public static ServoyMarker StyleElementClassNoStyle = new ServoyMarker(
		"Style class \"{0}\" from element \"{1}\" in form \"{2}\" is set but no style is assigned to form.", ServoyBuilder.MISSING_STYLE);

	public static ServoyMarker MediaTIFF = new ServoyMarker(
		"Media {0} will not display correctly in Webclient since many browsers do not support TIFF image format.", ServoyBuilder.MEDIA_MARKER_TYPE);


	public static ServoyMarker VariantIdUnresolved = new ServoyMarker(
		"Unresolved variant of element \"{0}\" in form \"{1}\"",
		ServoyBuilder.VARIANT_MARKER_TYPE);

	/**
	 * This means that one of the modules referenced by the project could not be found in the workspace. This can happen if the module was manually deleted from the workspace folder.
	 */
	public static ServoyMarker ModuleNotFound = new ServoyMarker("Module \"{0}\" which is referenced by project \"{1}\" does not exist.",
		ServoyBuilder.MISSING_MODULES_MARKER_TYPE);

	public static ServoyMarker ModuleDifferentI18NTable = new ServoyMarker("Module \"{0}\" has a different i18n table than main solution \"{1}\".",
		ServoyBuilder.I18N_MARKER_TYPE);

	public static ServoyMarker ModuleDifferentResourceProject = new ServoyMarker(
		"Module \"{0}\" of solution \"{1}\" references a different Servoy Resources Project.", ServoyBuilder.DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE);

	/**
	 * This means that a table that is referenced by an entity could not be accessed. In most cases this means that the table does not exist, but it can
	 * also mean that some exception occured while accessing an existing table.
	 */
	public static ServoyMarker ItemReferencesInvalidTable = new ServoyMarker("{0} \"{1}\" references invalid table \"{2}\".",
		ServoyBuilder.INVALID_TABLE_NODE_PROBLEM);

	public static ServoyMarker CalculationFormAccess = new ServoyMarker("Form access found in calculation \"{0}\". This is not supported.",
		ServoyBuilder.CALCULATION_MARKER_TYPE);
	public static ServoyMarker CalculationInTableFormAccess = new ServoyMarker(
		"Form access found in calculation \"{0}\" from table \"{1}\". This is not supported.", ServoyBuilder.CALCULATION_MARKER_TYPE);

	public static ServoyMarker PropertyTargetNotFound = new ServoyMarker("Property \"{0}\" is linked to an entity that does not exist.",
		ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);
	public static ServoyMarker PropertyOnElementTargetNotFound = new ServoyMarker(
		"Property \"{0}\" from element \"{1}\" is linked to an entity that does not exist.", ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);
	public static ServoyMarker PropertyInFormTargetNotFound = new ServoyMarker("Property \"{0}\" in form \"{1}\" is linked to an entity that does not exist.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker PropertyOnElementInFormTargetNotFound = new ServoyMarker(
		"Property \"{0}\" from element \"{1}\" in form \"{2}\" is linked to an entity that does not exist.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * Private methods accessed from outside context.
	 */
	public static ServoyMarker PropertyTargetNotAccessible = new ServoyMarker(
		"Property \"{0}\" is linked to a private method \"{1}\" which is not accessible from the context.", ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);
	public static ServoyMarker PropertyOnElementTargetNotAccessible = new ServoyMarker(
		"Property \"{0}\" from element \"{1}\" is linked to private method \"{2}\" which is not accessible from the context.",
		ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);
	public static ServoyMarker PropertyInFormTargetNotAccessible = new ServoyMarker(
		"Property \"{0}\" in form \"{1}\" is linked to a private method \"{2}\" which is not accessible from the form.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker PropertyOnElementInFormTargetNotAccessible = new ServoyMarker(
		"Property \"{0}\" from element \"{1}\" in form \"{2}\" is linked to a private method \"{3}\" which is not accessible from the context.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormTableNoPK = new ServoyMarker("Form \"{0}\" is based on table \"{1}\" which does not have a primary key.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormHasTooManyThingsAndProbablyLowPerformance = new ServoyMarker(
		"For performance reasons on the internet/WAN it is strongly suggested to place no more then {0} {1} on a{2} form.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormRowBGCalcTargetNotFound = new ServoyMarker(
		"rowBGColorCalculation of form \"{0}\" is linked to an entity that does not exist.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormDuplicatePart = new ServoyMarker("Form \"{0}\" has multiple parts of type: \"{1}\".",
		ServoyBuilder.FORM_DUPLICATE_PART_MARKER_TYPE);

	public static ServoyMarker NonAccessibleFormInModuleUsedInParentSolutionForm = new ServoyMarker(
		"Non-public {0} \"{1}\" from module \"{2}\" is used in solution \"{3}\", in form \"{4}\". This is not allowed.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker NamedFoundsetDatasourceNotMatching = new ServoyMarker(
		"Named foundset \"{0}\" used on form \"{1}\" from datasource \"{2}\" does not match its usage from form \"{3}\". Datasources should match.",
		ServoyBuilder.NAMED_FOUNDSET_DATASOURCE);

	/**
	 * This means that the sam tab sequence position is assigned to more than one element on a form.
	 */
	public static ServoyMarker FormNamedElementDuplicateTabSequence = new ServoyMarker(
		"Form \"{0}\" contains an element \"{1}\" which has a duplicate tab sequence.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormDerivedFormRedefinedVariable = new ServoyMarker(
		"Form \"{0}\" has a variable \"{1}\" which is also present in the parent hierarchy of the form.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormVariableTableCol = new ServoyMarker("Form \"{0}\" has a variable \"{1}\" which is also a column in table \"{2}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormVariableTableColFromComponent = new ServoyMarker(
		"Form \"{0}\" has a variable \"{1}\" which is also a column in table \"{2}\" which is used in web component \"{3}\" in property \"{4}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormTableNotAccessible = new ServoyMarker("Form \"{0}\" is based on table \"{1}\" which is not accessible.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormFileNameInconsistent = new ServoyMarker("Form \"{0}\" is saved on disk in a file with a different name( \"{1}\" ).",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormReferenceInvalidProperty = new ServoyMarker(
		"Form \"{0}\" is a form component and it is not allowed to have the property \"{1}\" set.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormReferenceInvalidScript = new ServoyMarker(
		"Form \"{0}\" is a form component and it is not allowed to have script variables or methods defined (\"{1}\").",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormComponentInvalidDataSource = new ServoyMarker(
		"Element \"{0}\" has form component property \"{1}\" attached with form \"{2}\" which has different datasource than main form \"{3}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormComponentForFoundsetInvalidDataSource = new ServoyMarker(
		"Element \"{0}\" has form component property \"{1}\" attached with form \"{2}\" which has different datasource than forFoundset property \"{3}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormComponentIncompatibleLayout = new ServoyMarker(
		"Element \"{0}\" in absolute position form has form component property \"{1}\" attached with responsive form \"{2}\". This is not supported.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormComponentNestedList = new ServoyMarker("List form component \"{0}\" has a nested list form component \"{1}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * This means that when using form inheritance a cycle was introduced in the dependency relation of some forms. This is not allowed.
	 */
	public static ServoyMarker FormExtendsCycle = new ServoyMarker("Form \"{0}\" is part of a cycle through the \"extendsForm\" property.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormDerivedFormDifferentTable = new ServoyMarker(
		"Form \"{0}\" which extends form \"{1}\" does not have the same table as its parent.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormNamedFoundsetIncorrectValue = new ServoyMarker("\"namedFoundSet\" property of form \"{0}\" is incorrect.{1}",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormNamedElementOutsideBoundsOfForm = new ServoyMarker("Element \"{0}\" in form \"{1}\" is outside the bounds of the form.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormNamedElementOutsideBoundsOfPart = new ServoyMarker("Element \"{0}\" in form \"{1}\" is outside the bounds of part {2}.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormTypeAheadNamedUnstoredCalculation = new ServoyMarker(
		"Type ahead field \"{0}\" has attached a valuelist (or fallback valuelist) that contains unstored calculation(s). This is not supported.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormEditableNamedComboboxCustomValuelist = new ServoyMarker(
		"Editable combobox \"{0}\" has attached a valuelist that contains real values. This is not supported.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormNamedFieldRelatedValuelist = new ServoyMarker(
		"Field \"{0}\" has attached a related valuelist \"{1}\" that doesn't have the same datasource as {2} .", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormNamedFieldFallbackRelatedValuelist = new ServoyMarker(
		"Field \"{0}\" has attached a valuelist \"{1}\" whose fallback valuelist \"{2}\" is related but doesn't have the same datasource as {3}.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker ComponentInvalidFoundset = new ServoyMarker("Unresolved foundset \"{0}\" for component \"{1}\"",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * The labelfor element used by a label cannot be found.
	 */
	public static ServoyMarker FormLabelForElementNotFound = new ServoyMarker("Label in form \"{0}\" has labelfor property \"{1}\" that does not exist.",
		ServoyBuilder.LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE);

	public static ServoyMarker FormExtendsFormElementNotFound = new ServoyMarker("Form \"{0}\" extends a form that cannot be found.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker ObsoleteElement = new ServoyMarker("Form \"{0}\" contains an obsolete element type, this should be removed.",
		ServoyBuilder.OBSOLETE_ELEMENT);
	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static ServoyMarker FormPortalUnnamedElementUnnamedMismatchedRelation = new ServoyMarker(
		"Element from portal has relation sequence \"{0}\" while portal has relationName \"{1}\". It should be the same value.",
		ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE);

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static ServoyMarker FormPortalUnnamedElementNamedMismatchedRelation = new ServoyMarker(
		"Element \"{0}\" from portal has relation sequence \"{1}\" while portal has relationName \"{2}\". It should be the same value.",
		ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE);

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static ServoyMarker FormPortalNamedElementUnnamedMismatchedRelation = new ServoyMarker(
		"Element from portal \"{0}\" has relation sequence \"{1}\" while portal has relationName \"{2}\". It should be the same value.",
		ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE);

	/**
	 * This means that inside a portal you placed an element which is based on a relation sequence that is different from the relation sequence of the portal.
	 * This is not supported.
	 */
	public static ServoyMarker FormPortalNamedElementNamedMismatchedRelation = new ServoyMarker(
		"Element \"{0}\" from portal \"{1}\" has relation sequence \"{2}\" while portal has relationName \"{3}\". It should be the same value.",
		ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE);

	/**
	 * This means that the relations that build up the relation sequence of the portal do not fit together properly. At some point in the
	 * sequence two relations are joined, but not on the same table.
	 */
	public static ServoyMarker FormPortalUnnamedInvalidRelationName = new ServoyMarker("Portal has invalid relationName (relation chain is not correct).",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * This means that the relations that build up the relation sequence of the portal do not fit together properly. At some point in the
	 * sequence two relations are joined, but not on the same table.
	 */
	public static ServoyMarker FormPortalNamedInvalidRelationName = new ServoyMarker("Portal \"{0}\" has invalid relationName (relation chain not correct).",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * This means that in a tabpanel you have a tab with an image larger then 20x20 pixels
	 */
	public static ServoyMarker FormTabPanelTabImageTooLarge = new ServoyMarker(
		"Tab \"{0}\" of tab panel \"{1}\" in form \"{2}\" has an image larger then 20x20 pixels", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * This means that in a tabpanel you have a tab based on a certain relation, but the form displayed in the tab is based on a table
	 * that is different from the child table of the relation.
	 */
	public static ServoyMarker FormRelatedTabDifferentTable = new ServoyMarker(
		"Related tab error: form \"{0}\" is based on a different table than relation \"{1}\".", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * This means that the relation sequence of a tab inside a tabpanel is invalid. This can happen when a relation from the relation sequence
	 * cannot be found, or when two adiacent relations in the relation sequence do not fit together properly (they are being joined on diffent tables).
	 */
	public static ServoyMarker FormRelatedTabUnsolvedRelation = new ServoyMarker("Related tab error: cannot resolve relation sequence \"{0}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * When solutions are imported from older Servoy versions, the relation which was stored as uuid, is converted to relation name.
	 * When this fails this problem marker has an attached quickfix to correct the unresolved relation name.
	 */
	public static ServoyMarker FormRelatedTabUnsolvedUuid = new ServoyMarker("Related tab error: relation UUID was not resolved to relation name \"{0}\".",
		ServoyBuilder.UNRESOLVED_RELATION_UUID);

	public static ServoyMarker FormTabPanelTabSequenceNotSet = new ServoyMarker(
		"Form \"{0}\" has tab panel \"{1}\" with tab sequence not set. Components inside \"{1}\" will have their tabsequences ignored.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormPropertyMethodNotAccessible = new ServoyMarker(
		"Property \"{0}\" in form \"{1}\" is linked to a non accessible method (method belongs to form \"{2}\").", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormPropertyOnElementMethodNotAccessible = new ServoyMarker(
		"Property \"{0}\" from element \"{1}\" in form \"{2}\" is linked to a non accessible method (method belongs to form \"{3}\").",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormDataproviderNotBasedOnFormTable = new ServoyMarker(
		"Element in form \"{0}\" has dataprovider \"{1}\" that is not based on the form table.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormDataproviderOnElementNotBasedOnFormTable = new ServoyMarker(
		"Element \"{0}\" in form \"{1}\" has dataprovider \"{2}\" that is not based on form table.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * Aggregates are non-editable values and this should not be displayed in editable fields.
	 */
	public static ServoyMarker FormDataproviderAggregateNotEditable = new ServoyMarker(
		"Element in form \"{0}\" has dataprovider \"{1}\" which is an aggregate and cannot be editable.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * Aggregates are non-editable values and this should not be displayed in editable fields.
	 */
	public static ServoyMarker FormDataproviderOnElementAggregateNotEditable = new ServoyMarker(
		"Element \"{0}\" in form \"{1}\" has dataprovider \"{2}\" which is an aggregate and cannot be editable.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormFormatInvalid = new ServoyMarker("Element in form \"{0}\" has invalid format: \"{1}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormFormatOnElementInvalid = new ServoyMarker("Element \"{0}\" in form \"{1}\" has invalid format: \"{2}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker FormFormatIncompatible = new ServoyMarker(
		"Format not supported on element from form \"{0}\" as it has a valuelist with real and display values.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker FormFormatOnElementIncompatible = new ServoyMarker(
		"Format not supported on element \"{0}\" from form \"{1}\" as it has a valuelist with real and display values.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker RequiredPropertyMissingOnElement = new ServoyMarker("Property \"{0}\" must be set on element \"{1}\", in form \"{2}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * The dataprovider used by an element cannot be found. This can happen for example if the structure of a database is changed and columns are deleted or renamed.
	 */
	public static ServoyMarker FormDataproviderNotFound = new ServoyMarker("Element in form \"{0}\" has a dataprovider \"{1}\" that does not exist.",
		ServoyBuilder.INVALID_DATAPROVIDERID);

	/**
	 * The dataprovider used by an element cannot be found. This can happen for example if the structure of a database is changed and columns are deleted or renamed.
	 */
	public static ServoyMarker FormIncompatibleElementType = new ServoyMarker("Element \"{0}\" in form \"{1}\" has an incompatible dataprovider type (MEDIA).",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * The roll over image Media will only work if an image property is also set.
	 */
	public static ServoyMarker ImageMediaNotSet = new ServoyMarker(
		"Element in form \"{0}\" has a 'rolloverImageMedia' property while no 'imageMedia' property is set.", ServoyBuilder.MEDIA_MARKER_TYPE);

	/**
	 * The roll over imageand cursor is not working in Smart Client tableview/listview form.
	 */
	public static ServoyMarker RolloverImageAndCursorNotWorking = new ServoyMarker(
		"Roll over image and cursor are not supported in Smart client tableview/listview forms.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * The mobile form navigator overlaps the left header button
	 */
	public static ServoyMarker MobileFormNavigatorOverlapsHeaderButton = new ServoyMarker(
		"Mobile form navigator and left header button cannot be used in the same time, as the navigator toggle button overlaps the left header button.",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * The HTML or RTF field has as dataprovider a column whose length may be too small.
	 */
	public static ServoyMarker FormColumnLengthTooSmall = new ServoyMarker(
		"HTML/RTF element \"{0}\" in form \"{1}\" has column dataprovider with small length. This may cause unpredictable results(if length of the value will get bigger than the column length).",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	/**
	 * The dataprovider used by an element cannot be found. This can happen for example if the structure of a database is changed and columns are deleted or renamed.
	 */
	public static ServoyMarker FormDataproviderOnElementNotFound = new ServoyMarker(
		"Element \"{0}\" in form \"{1}\" has a dataprovider \"{2}\" that does not exist.", ServoyBuilder.INVALID_DATAPROVIDERID);

	public static ServoyMarker TableMarkedAsHiddenButUsedIn = new ServoyMarker(
		"Table \"{0}\" is marked as hidden in developer, but it is still used in {1}\"{2}\".", ServoyBuilder.HIDDEN_TABLE_STILL_IN_USE);

	public static ServoyMarker ColumnUUIDFlagNotSet = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which is a UUID generator but does not have the UUID flag set.", ServoyBuilder.COLUMN_MARKER_TYPE);
	public static ServoyMarker ColumnIncompatibleTypeForSequence = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which has an incompatible type for its sequence.", ServoyBuilder.COLUMN_MARKER_TYPE);
	public static ServoyMarker ColumnIncompatbleWithUUID = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" marked as UUID, but the type and/or length are not compatible with UUID (MEDIA:16 or TEXT:36).",
		ServoyBuilder.COLUMN_MARKER_TYPE);
	public static ServoyMarker ColumnDatabaseIdentityProblem = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which is a database identity but is not a primary key in the table.", ServoyBuilder.COLUMN_MARKER_TYPE);
	public static ServoyMarker ColumnForeignTypeProblem = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which has an invalid foreign type suggestion: \"{2}\".", ServoyBuilder.COLUMN_MARKER_TYPE);
	public static ServoyMarker ColumnMultipleTenantTypeProblem = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which is marked as tenant. Table has multiple tenant columns.", ServoyBuilder.COLUMN_MARKER_TYPE);

	/**
	 * The lookup value used for a column is invalid. This can happen when the lookup points to a global identifier that does not exist,
	 * or when the lookup contains a relation that does not exist, or a relation sequence that does not fit together properly, or a column
	 * that does not exist.
	 */
	public static ServoyMarker ColumnLookupInvalid = new ServoyMarker("Table \"{0}\" has column \"{1}\" which has an invalid lookup value.",
		ServoyBuilder.COLUMN_MARKER_TYPE);
	/**
	 * The validator value used for a column is invalid. This can happen when the validator points to a global identifier that does not exist.
	 */
	public static ServoyMarker ColumnValidatorInvalid = new ServoyMarker("Table \"{0}\" has column \"{1}\" which has an invalid validator.",
		ServoyBuilder.COLUMN_MARKER_TYPE);
	/**
	 * The converter value used for a column is invalid. This can happen when the converter points to a global identifier that does not exist.
	 */
	public static ServoyMarker ColumnConverterInvalid = new ServoyMarker("Table \"{0}\" has column \"{1}\" which has an invalid converter.",
		ServoyBuilder.COLUMN_MARKER_TYPE);

	public static ServoyMarker UIConverterInvalid = new ServoyMarker("Element in form \"{0}\" has UI converter: \"{1}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);
	public static ServoyMarker UIConverterOnElementInvalid = new ServoyMarker("Element \"{0}\" in form \"{1}\" has invalid UI converter: \"{2}\".",
		ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker ColumnDuplicateNameDPID = new ServoyMarker(
		"Table \"{0}\" has column \"{1}\" which has a duplicate name/dataProviderID as column \"{2}\".", ServoyBuilder.COLUMN_MARKER_TYPE);

	public static ServoyMarker DBIColumnMissingFromDB = new ServoyMarker("Column \"{0}\" appears in the DB information file, but is missing from the DB table.",
		ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBIColumnMissingFromDBIFile = new ServoyMarker(
		"Column \"{0}\" appears in the DB table, but is missing from the DB information file.", ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBIColumnConflict = new ServoyMarker(
		"\"{0}\" column difference. {1} in DB <-> {2} in DB information file. The DB information on this column differs from the actual column in the DB.",
		ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBITableMissing = new ServoyMarker("Table \"{0}\" is missing from the DB (there is a database information file for this table).",
		ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBIFileMissing = new ServoyMarker("Missing database information file for table \"{0}\" (the table exists in the DB).",
		ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBIGenericError = new ServoyMarker(
		"Some kind of column difference between the DB and the DB information files for column \"{0}\".", ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBIBadDBInfo = new ServoyMarker("Bad database information: {0}", ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker DBIColumnSequenceTypeOverride = new ServoyMarker(
		"The sequence type for column \"{0}\" in table \"{1}\" has been overriden to db_identity; dbi file needs to be updated.",
		ServoyBuilder.DATABASE_INFORMATION_MARKER_TYPE);
	public static ServoyMarker LingeringTableFiles = new ServoyMarker(
		"Table \"{0}\" was deleted, but tbl file and possibly calculations/aggregations still exist.", ServoyBuilder.LINGERING_TABLE_FILES_TYPE);
	public static ServoyMarker InvalidTableNoPrimaryKey = new ServoyMarker("Table \"{0}\" has no primary key.",
		ServoyBuilder.INVALID_TABLE_NO_PRIMARY_KEY_TYPE);
	public static ServoyMarker DuplicateMemTable = new ServoyMarker("Duplicate Mem Table \"{0}\" in solutions \"{1}\" and \"{2}\"",
		ServoyBuilder.DUPLICATE_MEM_TABLE_TYPE);

	public static ServoyMarker ServerNotAccessibleFirstOccurence = new ServoyMarker(
		"Solution \"{0}\" references server \"{1}\" which is not accessible (first occurrence error).", ServoyBuilder.MISSING_SERVER);
	public static ServoyMarker ElementNameInvalidIdentifier = new ServoyMarker("Element has name \"{0}\" which is not a valid identifier.",
		ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE);
	public static ServoyMarker ElementNameReservedPrefixIdentifier = new ServoyMarker(
		"Element has name \"{0}\" which starts with the Servoy reserved \"{1}\" prefix.", ServoyBuilder.PROJECT_FORM_MARKER_TYPE);

	public static ServoyMarker RelationPrimaryTableNotFound = new ServoyMarker(
		"Relation \"{0}\" is referring to a primary table \"{1}\" which does not exist on server \"{2}\".", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationPrimaryTableWithoutPK = new ServoyMarker(
		"Relation \"{0}\" is referring to a primary table \"{1}\" on server \"{2}\" but the primary table does not have a primary key.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationPrimaryServerDuplicate = new ServoyMarker("Relation \"{0}\" is referring to duplicate primary server \"{1}\".",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationForeignTableNotFound = new ServoyMarker(
		"Relation \"{0}\" is referring to a foreign table \"{1}\" which does not exist on server \"{2}\".", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationForeignTableWithoutPK = new ServoyMarker(
		"Relation \"{0}\" is referring to a foreign table \"{1}\" on server \"{2}\" but the foreign table does not have a primary key.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationForeignServerDuplicate = new ServoyMarker("Relation \"{0}\" is referring to a duplicate foreign server \"{1}\".",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationItemNoPrimaryDataprovider = new ServoyMarker("Relation \"{0}\" has an item without primary dataprovider.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationItemPrimaryDataproviderNotFound = new ServoyMarker(
		"Relation \"{0}\" has a primary dataprovider \"{1}\" which is not found.", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationItemNoForeignDataprovider = new ServoyMarker("Relation \"{0}\" has an item without foreign dataprovider.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationItemForeignDataproviderNotFound = new ServoyMarker("Relation \"{0}\" has a foreign column \"{1}\" which is not found.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);

	/**
	 * The relation does not contain any comparison keys. Each relation should have at least one comparison key.
	 */
	public static ServoyMarker RelationEmpty = new ServoyMarker("Relation \"{0}\" has no keys (is empty).", ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);

	/**
	 * The relation contains a comparison key where the types of the two involved dataproviders do not fit together properly.
	 */
	public static ServoyMarker RelationItemTypeProblem = new ServoyMarker("Relation \"{0}\" has a relation item with mismatched keys: {1}.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);
	public static ServoyMarker RelationItemUUIDProblem = new ServoyMarker(
		"Relation \"{0}\" has a relation item where UUID flag is set for only one column (\"{1}\" - \"{2}\"). Either both columns or neither column should have the UUID flag set.",
		ServoyBuilder.PROJECT_RELATION_MARKER_TYPE);

	public static ServoyMarker MethodEventParameters = new ServoyMarker(
		"Event parameter is passed to event method; make sure it is used with right type (change method signature).", ServoyBuilder.EVENT_METHOD_MARKER_TYPE);

	public static ServoyMarker MethodNoReturn = new ServoyMarker(
		"Script method \"{0}\" should contain @return tag as it is returning a value from its code.", ServoyBuilder.SCRIPT_MARKER_TYPE);

	public static ServoyMarker MissingDriver = new ServoyMarker("Server \"{0}\" has invalid/missing driver ( \"{1}\" ).", ServoyBuilder.MISSING_DRIVER);

	public static ServoyMarker MissingConverter = new ServoyMarker("Converter \"{0}\" has been configured on dataprovider \"{1}\" but cannot be found.",
		ServoyBuilder.MISSING_CONVERTER);

	public static ServoyMarker ServerCloneCycle = new ServoyMarker("Server \"{0}\" clone data model, cannot reference itself.",
		ServoyBuilder.SERVER_CLONE_CYCLE_TYPE);

	/**
	 * The element uses deprecated script element.
	 */
	public static ServoyMarker ElementUsingDeprecatedVariable = new ServoyMarker("Variable \"{0}\" in \"{1}\" as \"{2}\" property while deprecated.",
		ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE);
	public static ServoyMarker ElementUsingDeprecatedFunction = new ServoyMarker("Function \"{0}\" is used in \"{1}\" as \"{2}\" property while deprecated.",
		ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE);
	public static ServoyMarker ElementUsingDeprecatedCalculation = new ServoyMarker(
		"Calculation \"{0}\" is used in \"{1}\" as \"{2}\" property while deprecated.", ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE); //$NON-NLS-1$

	/**
	 * The element extends a deleted element
	 */
	public static ServoyMarker ElementExtendsDeletedElement = new ServoyMarker("Element in form \"{0}\" extends a deleted element from the parent form.", //$NON-NLS-1$
		ServoyBuilder.ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE);

	/**
	 * The roll over imageand cursor is not working in Smart Client tableview/listview form.
	 */
	public static ServoyMarker MissingSpecification = new ServoyMarker("{0} specification \"{1}\" from package \"{2}\" is missing.",
		ServoyBuilder.MISSING_SPEC);

	public static ServoyMarker MissingProjectReference = new ServoyMarker("Missing project reference \"{0}\" for solution \"{1}\".",
		ServoyBuilder.MISSING_PROJECT_REFERENCE);

	public static ServoyMarker MethodOverrideProblem = new ServoyMarker("The function \"{0}\" in form \"{1}\" must override a superform method.",
		ServoyBuilder.METHOD_OVERRIDE);

	public static ServoyMarker DeprecatedSpecification = new ServoyMarker("The type \"{0}\" of the {1} is deprecated. {2}", ServoyBuilder.DEPRECATED_SPEC);

	public static ServoyMarker MissingPropertyFromSpecification = new ServoyMarker(
		"The component \"{0}\" in form {1} contains property {2} which is missing from spec.", ServoyBuilder.MISSING_PROPERTY_FROM_SPEC);

	public static ServoyMarker DeprecatedHandler = new ServoyMarker("The handler \"{0}\" of the {1} is deprecated. {2}",
		ServoyBuilder.DEPRECATED_ELEMENT_USAGE);

	public static ServoyMarker Parameters_Mismatch = new ServoyMarker(
		"Component {0} has handler \"{1}\" which has different number of parameters in spec and properties view.", ServoyBuilder.PARAMETERS_MISMATCH);
}
