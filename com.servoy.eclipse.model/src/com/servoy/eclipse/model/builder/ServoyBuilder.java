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

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;

import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IFolder;
import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.IResourceDelta;
import org.eclipse.core.resources.IResourceDeltaVisitor;
import org.eclipse.core.resources.IResourceVisitor;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IncrementalProjectBuilder;
import org.eclipse.core.resources.ProjectScope;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.PackageSpecification;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.SpecProviderState;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebLayoutSpecification;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.DeveloperFlattenedSolution;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IDataSourceManager;
import com.servoy.eclipse.model.extensions.IMarkerAttributeContributor;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.inmemory.MemServer;
import com.servoy.eclipse.model.inmemory.MemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.IColumnValidator;
import com.servoy.j2db.dataprocessing.IPropertyDescriptor;
import com.servoy.j2db.dataprocessing.IPropertyDescriptorProvider;
import com.servoy.j2db.dataprocessing.ITypedColumnConverter;
import com.servoy.j2db.dataprocessing.IUIConverter;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IChildWebObject;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IRootObject;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.IServer;
import com.servoy.j2db.persistence.IServerInternal;
import com.servoy.j2db.persistence.IServerManagerInternal;
import com.servoy.j2db.persistence.ISupportChilds;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportDeprecated;
import com.servoy.j2db.persistence.ISupportEncapsulation;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportFormElement;
import com.servoy.j2db.persistence.ISupportMedia;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LayoutContainer;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptNameValidator;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * Builds Servoy projects. Adds problem markers where needed.
 */
public class ServoyBuilder extends IncrementalProjectBuilder
{
	public static int MAX_EXCEPTIONS = 25;
	public static int MIN_FIELD_LENGTH = 1000;

	public static int exceptionCount = 0;

	public static final int LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM = 3;
	public static final int LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM = 20;

	class ServoyDeltaVisitor implements IResourceDeltaVisitor
	{
		final List<IResource> resources = new ArrayList<IResource>();

		public boolean visit(IResourceDelta delta) throws CoreException
		{
			IResource resource = delta.getResource();
			resources.add(resource);
			return true;
		}
	}

	class ServoyResourceVisitor implements IResourceVisitor
	{
		public boolean visit(IResource resource)
		{
			checkResource(resource);
			//return true to continue visiting children.
			return true;
		}
	}

	static class XMLErrorHandler extends DefaultHandler
	{

		private final IFile file;

		public XMLErrorHandler(IFile file)
		{
			this.file = file;
		}

		private void addMarker(SAXParseException e, int severity)
		{
			ServoyBuilder.addMarker(file, XML_MARKER_TYPE, e.getMessage(), e.getLineNumber(), severity, IMarker.PRIORITY_NORMAL, null, null);
		}

		@Override
		public void error(SAXParseException exception) throws SAXException
		{
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		@Override
		public void fatalError(SAXParseException exception) throws SAXException
		{
			addMarker(exception, IMarker.SEVERITY_ERROR);
		}

		@Override
		public void warning(SAXParseException exception) throws SAXException
		{
			addMarker(exception, IMarker.SEVERITY_WARNING);
		}
	}

	public static final String BUILDER_ID = "com.servoy.eclipse.core.servoyBuilder";
	private static final String _PREFIX = "com.servoy.eclipse.core";
	public static final String SERVOY_MARKER_TYPE = _PREFIX + ".servoyProblem";
	public static final String SERVOY_BUILDER_MARKER_TYPE = _PREFIX + ".builderProblem";

	/**
	 * If you add a new type, do not forget to add the extension point also!!!
	 */
	public static final String XML_MARKER_TYPE = _PREFIX + ".xmlProblem";
	public static final String PROJECT_DESERIALIZE_MARKER_TYPE = _PREFIX + ".deserializeProblem";
	public static final String SOLUTION_PROBLEM_MARKER_TYPE = _PREFIX + ".solutionProblem";
	public static final String BAD_STRUCTURE_MARKER_TYPE = _PREFIX + ".badStructure";
	public static final String MISSING_MODULES_MARKER_TYPE = _PREFIX + ".missingModulesProblem";
	public static final String MISPLACED_MODULES_MARKER_TYPE = _PREFIX + ".misplacedModulesProblem";
	public static final String MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".multipleResourcesProblem";
	public static final String DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".differentResourcesProblem";
	public static final String PROJECT_RELATION_MARKER_TYPE = _PREFIX + ".relationProblem";
	public static final String MEDIA_MARKER_TYPE = _PREFIX + ".mediaProblem";
	public static final String VARIANT_MARKER_TYPE = _PREFIX + ".variantProblem";
	public static final String CALCULATION_MARKER_TYPE = _PREFIX + ".calculationProblem";
	public static final String SCRIPT_MARKER_TYPE = _PREFIX + ".scriptProblem";
	public static final String EVENT_METHOD_MARKER_TYPE = _PREFIX + ".eventProblem";
	public static final String USER_SECURITY_MARKER_TYPE = _PREFIX + ".userSecurityProblem";
	public static final String DATABASE_INFORMATION_MARKER_TYPE = _PREFIX + ".databaseInformationProblem";
	public static final String PROJECT_FORM_MARKER_TYPE = _PREFIX + ".formProblem";
	public static final String INVALID_TABLE_NODE_PROBLEM = _PREFIX + ".invalidTableNodeProblem";
	public static final String PROJECT_VALUELIST_MARKER_TYPE = _PREFIX + ".valuelistProblem";
	public static final String DUPLICATE_UUID = _PREFIX + ".duplicateUUID";
	public static final String DUPLICATE_SIBLING_UUID = _PREFIX + ".duplicateSiblingUUID";
	public static final String DUPLICATE_NAME_MARKER_TYPE = _PREFIX + ".duplicateNameProblem";
	public static final String DUPLICATE_REFERENCED_FORM_MARKER_TYPE = _PREFIX + ".duplicateReferencedFormProblem";
	public static final String RESERVED_WINDOW_OBJECT_USAGE_TYPE = _PREFIX + ".reservedWindowObjectUsageProblem";
	public static final String DUPLICATE_SCOPE_NAME_MARKER_TYPE = _PREFIX + ".duplicateScopeNameProblem";
	public static final String INVALID_SORT_OPTION = _PREFIX + ".invalidSortOption";
	public static final String PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE = _PREFIX + ".differentRelationName";
	public static final String MISSING_SERVER = _PREFIX + ".missingServer";
	public static final String MISSING_STYLE = _PREFIX + ".missingStyle";
	public static final String I18N_MARKER_TYPE = _PREFIX + ".i18nProblem";
	public static final String COLUMN_MARKER_TYPE = _PREFIX + ".columnProblem";
	public static final String INVALID_EVENT_METHOD = _PREFIX + ".invalidEventMethod";
	public static final String INVALID_COMMAND_METHOD = _PREFIX + ".invalidCommandMethod";
	public static final String INVALID_DATAPROVIDERID = _PREFIX + ".invalidDataProviderID";
	public static final String DEPRECATED_PROPERTY_USAGE = _PREFIX + ".deprecatedPropertyUsage";
	public static final String FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION = _PREFIX + ".formWithDatasourceInLoginSolution";
	public static final String MULTIPLE_METHODS_ON_SAME_ELEMENT = _PREFIX + ".multipleMethodsInfo";
	public static final String UNRESOLVED_RELATION_UUID = _PREFIX + ".unresolvedRelationUuid";
	public static final String CONSTANTS_USED_MARKER_TYPE = _PREFIX + ".constantsUsed"; // removed marker type, kept only for removal
	public static final String MISSING_DRIVER = _PREFIX + ".missingDriver";
	public static final String OBSOLETE_ELEMENT = _PREFIX + ".obsoleteElement";
	public static final String HIDDEN_TABLE_STILL_IN_USE = _PREFIX + ".hiddenTableInUse";
	public static final String MISSING_CONVERTER = _PREFIX + ".missingConverter";
	public static final String LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE = _PREFIX + ".labelForElementProblem";
	public static final String INVALID_MOBILE_MODULE_MARKER_TYPE = _PREFIX + ".invalidMobileModuleProblem";
	public static final String FORM_DUPLICATE_PART_MARKER_TYPE = _PREFIX + ".formDuplicatePart";
	public static final String DEPRECATED_SCRIPT_ELEMENT_USAGE = _PREFIX + ".deprecatedScriptElementUsage";
	public static final String METHOD_NUMBER_OF_ARGUMENTS_MISMATCH_TYPE = _PREFIX + ".methodNumberOfArgsMismatch";
	public static final String SERVER_CLONE_CYCLE_TYPE = _PREFIX + ".serverCloneCycle";
	public static final String DEPRECATED_ELEMENT_USAGE = _PREFIX + ".deprecatedElementUsage";
	public static final String ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE = _PREFIX + ".elementExtendsDeletedElement";
	public static final String LINGERING_TABLE_FILES_TYPE = _PREFIX + ".lingeringTableFiles";
	public static final String DUPLICATE_MEM_TABLE_TYPE = _PREFIX + ".duplicateMemTable";
	public static final String SUPERFORM_PROBLEM_TYPE = _PREFIX + ".superformProblem";
	public static final String MISSING_SPEC = _PREFIX + ".missingSpec";
	public static final String MISSING_PROJECT_REFERENCE = _PREFIX + ".missingProjectReference";
	public static final String METHOD_OVERRIDE = _PREFIX + ".methodOverride";
	public static final String DEPRECATED_SPEC = _PREFIX + ".deprecatedSpec";
	public static final String MISSING_PROPERTY_FROM_SPEC = _PREFIX + ".missingPropetyFromSpec";
	public static final String PARAMETERS_MISMATCH = _PREFIX + ".parametersMismatch";
	public static final String WRONG_OVERRIDE_PARENT = _PREFIX + ".wrongOverridePosition";
	public static final String INVALID_TABLE_NO_PRIMARY_KEY_TYPE = _PREFIX + ".invalidTableNoPrimaryKey";
	public static final String SERVICE_MUST_AUTHENTICATE_MARKER_TYPE = _PREFIX + ".serviceMustAuthenticateProblem";
	public static final String NAMED_FOUNDSET_DATASOURCE = _PREFIX + ".namedFoundsetDatasourceProblem";

	public static final String PROJECT_REFERENCE_NAME = "projectReferenceName";

	// warning/error level settings keys/defaults
	public final static String ERROR_WARNING_PREFERENCES_NODE = Activator.PLUGIN_ID + "/errorWarningLevels";

	// performance related
	public final static Pair<String, ProblemSeverity> LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW = new Pair<String, ProblemSeverity>("performanceTableColumns",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> LEVEL_PERFORMANCE_TABS_PORTALS = new Pair<String, ProblemSeverity>("performanceTabsPortals",
		ProblemSeverity.WARNING);

	// developer problems
	public final static Pair<String, ProblemSeverity> INVALID_TABLE_REFERENCE = new Pair<String, ProblemSeverity>("invalidTableReference",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> METHOD_EVENT_PARAMETERS = new Pair<String, ProblemSeverity>("methodEventParameters",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> METHOD_NO_RETURN = new Pair<String, ProblemSeverity>("methodNoReturn",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> MEDIA_TIFF = new Pair<String, ProblemSeverity>("mediaTiff", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> VARIANT_ID_UNRESOLVED = new Pair<String, ProblemSeverity>("variantIdUnresolved", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> CALCULATION_FORM_ACCESS = new Pair<String, ProblemSeverity>("calculationFormAccess",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> IMAGE_MEDIA_NOT_SET = new Pair<String, ProblemSeverity>("imageMediaNotSet", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> ROLLOVER_NOT_WORKING = new Pair<String, ProblemSeverity>("rolloverNotWorking", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> MOBILE_NAVIGATOR_OVERLAPS_HEADER_BUTTON = new Pair<String, ProblemSeverity>(
		"mobileNavigatorOverlapsHeaderButton", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> DATAPROVIDER_MISSING_CONVERTER = new Pair<String, ProblemSeverity>("dataproviderMissingConverter",
		ProblemSeverity.WARNING);

	// problems with resource projects
	public final static Pair<String, ProblemSeverity> REFERENCES_TO_MULTIPLE_RESOURCES = new Pair<String, ProblemSeverity>("referencesToMultipleResources",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> ERROR_MISSING_PROJECT_REFERENCE = new Pair<String, ProblemSeverity>("missingProjectReference",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> NO_RESOURCE_REFERENCE = new Pair<String, ProblemSeverity>("noResourceReference", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> PROPERTY_MULTIPLE_METHODS_ON_SAME_TABLE = new Pair<String, ProblemSeverity>(
		"propertyMultipleMethodsOnSameTable", ProblemSeverity.INFO);
	public final static Pair<String, ProblemSeverity> SERVER_MISSING_DRIVER = new Pair<String, ProblemSeverity>("severMissingDriver", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> SERVER_CLONE_CYCLE = new Pair<String, ProblemSeverity>("serverCloneCycle", ProblemSeverity.WARNING);

	// login problems
	public final static Pair<String, ProblemSeverity> LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION = new Pair<String, ProblemSeverity>(
		"formWithDatasourceInLoginSolution", ProblemSeverity.WARNING);

	// deprecated properties usage problems
	public final static Pair<String, ProblemSeverity> DEPRECATED_PROPERTY_USAGE_PROBLEM = new Pair<String, ProblemSeverity>("deprecatedPropertyUsage",
		ProblemSeverity.WARNING);

	// deprecated element usage problems
	public final static Pair<String, ProblemSeverity> DEPRECATED_ELEMENT_USAGE_PROBLEM = new Pair<String, ProblemSeverity>("deprecatedElementUsage",
		ProblemSeverity.WARNING);

	//deprecated script elements usage problems
	public final static Pair<String, ProblemSeverity> DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM = new Pair<String, ProblemSeverity>(
		"deprecatedScriptElementUsage", ProblemSeverity.WARNING);

	// duplication problems
	public final static Pair<String, ProblemSeverity> DUPLICATION_UUID_DUPLICATE = new Pair<String, ProblemSeverity>("duplicationUUIDDuplicate",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DUPLICATION_DUPLICATE_ENTITY_FOUND = new Pair<String, ProblemSeverity>("duplicationDuplicateEntityFound",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DUPLICATION_SAME_REFERENCED_FORM = new Pair<String, ProblemSeverity>("duplicationSameReferencedForm",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DUPLICATION_DUPLICATE_OVERRIDE_FOUND = new Pair<String, ProblemSeverity>(
		"duplicationDuplicateOverrideFound", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RESERVED_WINDOW_OBJECT_PROPERTY = new Pair<String, ProblemSeverity>("reservedWindowObjectProperty",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> RESERVED_WINDOW_OBJECT_COLUMN = new Pair<String, ProblemSeverity>("reservedWindowObjectColumn",
		ProblemSeverity.WARNING);

	// database information problems
	public final static Pair<String, ProblemSeverity> DBI_BAD_INFO = new Pair<String, ProblemSeverity>("DBIBadDBInfo", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_MISSING_FROM_DB = new Pair<String, ProblemSeverity>("DBIColumnMissingFromDB",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_MISSING_FROM_DB_FILE = new Pair<String, ProblemSeverity>("DBIColumnMissingFromDBIFile",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_CONFLICT = new Pair<String, ProblemSeverity>("DBIColumnConflict", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> DBI_TABLE_MISSING = new Pair<String, ProblemSeverity>("DBITableMissing", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DBI_FILE_MISSING = new Pair<String, ProblemSeverity>("DBIFileMissing", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_INFO_SEQ_TYPE_OVERRIDE = new Pair<String, ProblemSeverity>("DBIColumnSequenceTypeOverride",
		ProblemSeverity.WARNING);
	public static final Pair<String, ProblemSeverity> LINGERING_TABLE_FILES = new Pair<String, ProblemSeverity>("LingeringTableFiles", ProblemSeverity.ERROR);
	public static final Pair<String, ProblemSeverity> INVALID_TABLE_NO_PRIMARY_KEY = new Pair<String, ProblemSeverity>("InvalidTableNoPrimaryKey",
		ProblemSeverity.ERROR);
	public static final Pair<String, ProblemSeverity> DUPLICATE_MEM_TABLE = new Pair<String, ProblemSeverity>("DuplicateMemTable", ProblemSeverity.ERROR);

	// column problems
	public final static Pair<String, ProblemSeverity> COLUMN_UUID_FLAG_NOT_SET = new Pair<String, ProblemSeverity>("ColumnUUIDFlagNotSet",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_DATABASE_IDENTITY_PROBLEM = new Pair<String, ProblemSeverity>("ColumnDatabaseIdentityProblem",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_DUPLICATE_NAME_DPID = new Pair<String, ProblemSeverity>("ColumnDuplicateNameDPID",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_FOREIGN_TYPE_PROBLEM = new Pair<String, ProblemSeverity>("ColumnForeignTypeProblem",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_MULTIPLE_TENANT_PROBLEM = new Pair<String, ProblemSeverity>("ColumnMultipleTenantProblem",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_INCOMPATIBLE_TYPE_FOR_SEQUENCE = new Pair<String, ProblemSeverity>(
		"ColumnIncompatibleTypeForSequence", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_INCOMPATIBLE_WITH_UUID = new Pair<String, ProblemSeverity>("ColumnIncompatbleWithUUID",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_LOOKUP_INVALID = new Pair<String, ProblemSeverity>("ColumnLookupInvalid", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_VALIDATOR_INVALID = new Pair<String, ProblemSeverity>("ColumnValidatorInvalid",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> COLUMN_CONVERTER_INVALID = new Pair<String, ProblemSeverity>("ColumnConverterInvalid",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> ROW_IDENT_SHOULD_NOT_BE_NULL = new Pair<String, ProblemSeverity>("ColumnRowIdentShouldNotBeNull",
		ProblemSeverity.WARNING);

	// sort problems
	public final static Pair<String, ProblemSeverity> INVALID_SORT_OPTIONS_RELATION_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"InvalidSortOptionsRelationNotFound", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE = new Pair<String, ProblemSeverity>(
		"InvalidSortOptionsRelationDifferentPrimaryDatasource", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"InvalidSortOptionsColumnNotFound", ProblemSeverity.WARNING);

	// module problems
	public final static Pair<String, ProblemSeverity> MODULE_NOT_FOUND = new Pair<String, ProblemSeverity>("moduleNotFound", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> MODULE_MISPLACED = new Pair<String, ProblemSeverity>("moduleMisplaced", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> MODULE_DIFFERENT_I18N_TABLE = new Pair<String, ProblemSeverity>("moduleDifferentI18NTable",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> MODULE_DIFFERENT_RESOURCE_PROJECT = new Pair<String, ProblemSeverity>("moduleDifferentResourceProject",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> MODULE_INVALID_MOBILE = new Pair<String, ProblemSeverity>("moduleInvalidMobile", ProblemSeverity.ERROR);

	// form problems
	public final static Pair<String, ProblemSeverity> FORM_COLUMN_LENGTH_TOO_SMALL = new Pair<String, ProblemSeverity>("formColumnLengthTooSmall",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE = new Pair<String, ProblemSeverity>(
		"formDataproviderAggregateNotEditable", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_INVALID_DATAPROVIDER = new Pair<String, ProblemSeverity>("formInvalidDataprovider",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_DERIVED_FORM_DIFFERENT_TABLE = new Pair<String, ProblemSeverity>("formDerivedFormDifferentTable",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_DERIVED_FORM_REDEFINED_VARIABLE = new Pair<String, ProblemSeverity>(
		"formDerivedFormRedefinedVariable", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_DUPLICATE_PART = new Pair<String, ProblemSeverity>("formDuplicatePart", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_EDITABLE_COMBOBOX_CUSTOM_VALUELIST = new Pair<String, ProblemSeverity>(
		"formEditableComboboxCustomValuelist", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_EXTENDS_CYCLE = new Pair<String, ProblemSeverity>("formExtendsCycle", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> SUPERFORM_PROBLEM = new Pair<String, ProblemSeverity>("superformProblem", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_FILE_NAME_INCONSISTENT = new Pair<String, ProblemSeverity>("formFileNameInconsistent",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_FORMAT_INVALID = new Pair<String, ProblemSeverity>("formFormatInvalid", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_INCOMPATIBLE_ELEMENT_TYPE = new Pair<String, ProblemSeverity>("formIncompatibleElementType",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_LABEL_FOR_ELEMENT_NOT_FOUND = new Pair<String, ProblemSeverity>("formLabelForElementNotFound",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_EXTENDS_FORM_ELEMENT_NOT_FOUND = new Pair<String, ProblemSeverity>("formExtendsFormElementNotFound",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_ELEMENT_DUPLICATE_TAB_SEQUENCE = new Pair<String, ProblemSeverity>("formElementDuplicateTabSequence",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_ELEMENT_OUTSIDE_BOUNDS = new Pair<String, ProblemSeverity>("formElementOutsideBounds",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_OBSOLETE_ELEMENT = new Pair<String, ProblemSeverity>("formObsoleteElement", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_REQUIRED_PROPERTY_MISSING = new Pair<String, ProblemSeverity>("formRequiredPropertyMissing",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_FIELD_RELATED_VALUELIST = new Pair<String, ProblemSeverity>("formFieldRelatedValuelist",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_FOUNDSET_INCORRECT_VALUE = new Pair<String, ProblemSeverity>("formFoundsetIncorrectValue",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_NAMED_FOUNDSET_DATASOURCE_MISMATCH = new Pair<String, ProblemSeverity>(
		"namedFoundsetDatasourceMismatch", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> COMPONENT_FOUNDSET_INVALID = new Pair<String, ProblemSeverity>("componentFoundsetInvalid",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_PORTAL_INVALID_RELATION_NAME = new Pair<String, ProblemSeverity>("formPortalInvalidRelationName",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_PROPERTY_METHOD_NOT_ACCESIBLE = new Pair<String, ProblemSeverity>("formPropertyMethodNotAccessible",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_TABPANEL_TAB_IMAGE_TOO_LARGE = new Pair<String, ProblemSeverity>("formTabPanelTabImageTooLarge",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_RELATED_TAB_DIFFERENT_TABLE = new Pair<String, ProblemSeverity>("formRelatedTabDifferentTable",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_RELATED_TAB_UNSOLVED_RELATION = new Pair<String, ProblemSeverity>("formRelatedTabUnsolvedRelation",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_RELATED_TAB_UNSOLVED_UUID = new Pair<String, ProblemSeverity>("formRelatedTabUnsolvedUuid",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_PROPERTY_TARGET_NOT_FOUND = new Pair<String, ProblemSeverity>("formPropertyTargetNotFound",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_INVALID_TABLE = new Pair<String, ProblemSeverity>("formInvalidTable", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_TYPEAHEAD_UNSTORED_CALCULATION = new Pair<String, ProblemSeverity>(
		"formTypeAheadUnstoredCalculation", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> FORM_VARIABLE_TYPE_COL = new Pair<String, ProblemSeverity>("formVariableTableCol",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_PROPERTY_MULTIPLE_METHODS_ON_SAME_ELEMENT = new Pair<String, ProblemSeverity>(
		"formPropertyMultipleMethodsOnSameElement", ProblemSeverity.INFO);
	public final static Pair<String, ProblemSeverity> FORM_REFERENCE_INVALID_PROPERTY = new Pair<String, ProblemSeverity>("formReferenceInvalidProperty",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_REFERENCE_INVALID_SCRIPT = new Pair<String, ProblemSeverity>("formReferenceInvalidScript",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_COMPONENT_INVALID_DATASOURCE = new Pair<String, ProblemSeverity>("formComponentInvalidDataSource",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_COMPONENT_INVALID_LAYOUT_COMBINATION = new Pair<String, ProblemSeverity>(
		"formComponentInvalidLayoutCombination",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> FORM_COMPONENT_NESTED_LIST = new Pair<String, ProblemSeverity>("formComponentNestedList",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> NON_ACCESSIBLE_PERSIST_IN_MODULE_USED_IN_PARENT_SOLUTION = new Pair<String, ProblemSeverity>(
		"nonAccessibleFormInModuleUsedInParentSolution", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> METHOD_NUMBER_OF_ARGUMENTS_MISMATCH = new Pair<String, ProblemSeverity>("methodNumberOfArgsMismatch",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> TAB_SEQUENCE_NOT_SET = new Pair<String, ProblemSeverity>("tabpanelTabSequenceNotSet",
		ProblemSeverity.INFO);
	public final static Pair<String, ProblemSeverity> ELEMENT_EXTENDS_DELETED_ELEMENT = new Pair<String, ProblemSeverity>("elementExtendsDeletedElement", //$NON-NLS-1$
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> MISSING_SPECIFICATION = new Pair<String, ProblemSeverity>("missingSpec", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> METHOD_OVERRIDE_PROBLEM = new Pair<String, ProblemSeverity>("methodOverride", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> DEPRECATED_SPECIFICATION = new Pair<String, ProblemSeverity>("deprecatedSpec", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> MISSING_PROPERTY_FROM_SPECIFICATION = new Pair<String, ProblemSeverity>("missingPropetyFromSpec",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> DEPRECATED_HANDLER = new Pair<String, ProblemSeverity>("deprecatedHandler", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> PARAMETERS_MISMATCH_SEVERITY = new Pair<String, ProblemSeverity>("parametersMismatch",
		ProblemSeverity.WARNING);

	// relations related
	public final static Pair<String, ProblemSeverity> RELATION_PRIMARY_SERVER_WITH_PROBLEMS = new Pair<String, ProblemSeverity>(
		"relationPrimaryServerWithProblems", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_SERVER_DUPLICATE = new Pair<String, ProblemSeverity>("relationServerDuplicate",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> RELATION_TABLE_NOT_FOUND = new Pair<String, ProblemSeverity>("relationTableNotFound",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_TABLE_WITHOUT_PK = new Pair<String, ProblemSeverity>("relationTableWithoutPK",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_FOREIGN_SERVER_WITH_PROBLEMS = new Pair<String, ProblemSeverity>(
		"relationForeignServerWithProblems", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_EMPTY = new Pair<String, ProblemSeverity>("relationEmpty", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_ITEM_DATAPROVIDER_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"relationItemDataproviderNotFound", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_ITEM_UUID_PROBLEM = new Pair<String, ProblemSeverity>("relationItemUUIDProblem",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> RELATION_ITEM_TYPE_PROBLEM = new Pair<String, ProblemSeverity>("relationItemTypeProblem",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> RELATION_GENERIC_ERROR = new Pair<String, ProblemSeverity>("relationGenericError", ProblemSeverity.ERROR);

	// valuelist problems
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_WITH_CUSTOM_VALUES = new Pair<String, ProblemSeverity>("valuelistDBWithCustomValues",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_NOT_TABLE_OR_RELATION = new Pair<String, ProblemSeverity>("valuelistDBNotTableOrRelation",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_MALFORMED_TABLE_DEFINITION = new Pair<String, ProblemSeverity>(
		"valuelistDBMalformedTableDefinition", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_SERVER_DUPLICATE = new Pair<String, ProblemSeverity>("valuelistDBServerDuplicate",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_TABLE_NO_PK = new Pair<String, ProblemSeverity>("valuelistDBTableNoPk",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> VALUELIST_ENTITY_NOT_FOUND = new Pair<String, ProblemSeverity>("valuelistEntityNotFound",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_RELATION_WITH_DATASOURCE = new Pair<String, ProblemSeverity>("valuelistRelationWithDatasource",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_RELATION_SEQUENCE_INCONSISTENT = new Pair<String, ProblemSeverity>(
		"valuelistRelationSequenceInconsistent", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_CUSTOM_VALUES_WITH_DB_INFO = new Pair<String, ProblemSeverity>(
		"valuelistCustomValuesWithDBInfo", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_INVALID_CUSTOM_VALUES = new Pair<String, ProblemSeverity>("valuelistInvalidCustomValues",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> VALUELIST_TYPE_UNKNOWN = new Pair<String, ProblemSeverity>("valuelistTypeUnknown", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_GENERIC_ERROR = new Pair<String, ProblemSeverity>("valuelistGenericError",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_WITH_FALLBACK_OF_FALLBACK = new Pair<String, ProblemSeverity>("valuelistWithFallbackofFallback",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> VALUELIST_DATAPROVIDER_TYPE_MISMATCH = new Pair<String, ProblemSeverity>(
		"valuelistDataproviderTypeMismatch", ProblemSeverity.ERROR);
	// styles
	public final static Pair<String, ProblemSeverity> STYLE_NOT_FOUND = new Pair<String, ProblemSeverity>("styleNotFound", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> STYLE_CLASS_NO_STYLE = new Pair<String, ProblemSeverity>("styleClassNoStyle", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> STYLE_CLASS_NOT_FOUND = new Pair<String, ProblemSeverity>("styleClassNotFound", ProblemSeverity.WARNING);

	// solution problems
	public final static Pair<String, ProblemSeverity> SOLUTION_BAD_STRUCTURE = new Pair<String, ProblemSeverity>("solutionBadStructure", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> SOLUTION_DESERIALIZE_ERROR = new Pair<String, ProblemSeverity>("solutionDeserializeError",
		ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER = new Pair<String, ProblemSeverity>(
		"solutionElementNameInvalidIdentifier", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> SOLUTION_ELEMENT_NAME_RESERVED_PREFIX_IDENTIFIER = new Pair<String, ProblemSeverity>(
		"solutionElementNameReservedPrefixIdentifier", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED = new Pair<String, ProblemSeverity>(
		"solutionPropertyFormCannotBeInstantiated", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> SOLUTION_PROPERTY_TARGET_NOT_FOUND = new Pair<String, ProblemSeverity>("solutionPropertyTargetNotFound",
		ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> SERVER_NOT_ACCESSIBLE_FIRST_OCCURENCE = new Pair<String, ProblemSeverity>(
		"serverNotAccessibleFirstOccurence", ProblemSeverity.ERROR);
	public final static Pair<String, ProblemSeverity> SOLUTION_USED_AS_WEBSERVICE_MUSTAUTHENTICATE_PROBLEM = new Pair<String, ProblemSeverity>(
		"solutionUsedAsWebServiceMustAuthenticateProblem", ProblemSeverity.WARNING);
	public final static Pair<String, ProblemSeverity> SOLUTION_WITH_HIGHER_FILE_VERSION = new Pair<String, ProblemSeverity>("solutionWithHigherFileVersion",
		ProblemSeverity.ERROR);

	private SAXParserFactory parserFactory;
	private final HashSet<String> referencedProjectsSet = new HashSet<String>();
	private final HashSet<String> moduleProjectsSet = new HashSet<String>();

	private IServoyModel servoyModel;
	private static IMarkerAttributeContributor[] markerContributors;

	private IProgressMonitor monitor;

	public ServoyBuilder()
	{
	}

	public static String getSeverity(String key, String def, IPersist persist)
	{
		IProject project = null;
		if (persist != null)
		{
			IRootObject rootObject = persist.getRootObject();
			if (rootObject instanceof Solution)
			{
				ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(rootObject.getName());
				if (servoyProject != null)
				{
					project = servoyProject.getProject();
				}
			}
		}
		return getSeverity(key, def, project);
	}

	public static String getSeverity(String key, String def, IProject project)
	{
		if (project != null)
		{
			String value = new ProjectScope(project).getNode(ERROR_WARNING_PREFERENCES_NODE).get(key, null);
			if (value != null) return value;
		}
		return InstanceScope.INSTANCE.getNode(ERROR_WARNING_PREFERENCES_NODE).get(key, def);
	}

	@Override
	protected IProject[] build(int kind, Map<String, String> args, IProgressMonitor progressMonitor) throws CoreException
	{
		// make sure the IServoyModel is initialized
		getServoyModel();
		referencedProjectsSet.clear();
		moduleProjectsSet.clear();

		IProject[] referencedProjects = getProject().getReferencedProjects();
		ArrayList<IProject> moduleAndModuleReferencedProjects = null;
		// we are interested in showing module error markers only if the project is in use (active prj or active module)
		if (servoyModel.isSolutionActive(getProject().getName()))
		{
			ServoyProject sp = getServoyProject(getProject());
			if (sp != null)
			{
				Solution sol = sp.getSolution();
				if (sol != null)
				{
					String moduleNames = sol.getModulesNames();
					if (moduleNames != null)
					{
						StringTokenizer st = new StringTokenizer(moduleNames, ";,");
						String moduleName;
						IProject moduleProject;
						while (st.hasMoreTokens())
						{
							if (moduleAndModuleReferencedProjects == null)
							{
								moduleAndModuleReferencedProjects = new ArrayList<IProject>();
							}
							moduleName = st.nextToken();
							moduleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(moduleName);
							moduleAndModuleReferencedProjects.add(moduleProject);
							moduleProjectsSet.add(moduleName);

							if (moduleProject.exists() && moduleProject.isOpen() && moduleProject.hasNature(ServoyProject.NATURE_ID))
							{
								IProject[] moduleReferencedProjects = moduleProject.getReferencedProjects();
								if (moduleReferencedProjects.length > 0)
								{
									for (IProject mrp : moduleReferencedProjects)
									{
										referencedProjectsSet.add(mrp.getName());
										if (!moduleAndModuleReferencedProjects.contains(mrp))
										{
											moduleAndModuleReferencedProjects.add(mrp);
										}
									}
								}
							}
						}
					}
				}
			}
		}
		IProject[] monitoredProjects;
		if (moduleAndModuleReferencedProjects != null)
		{
			// we now add all the remaining referenced projects to be monitored inside the moduleProjects (in order to create an array out of them)
			for (IProject p : referencedProjects)
			{
				if (!moduleAndModuleReferencedProjects.contains(p))
				{
					moduleAndModuleReferencedProjects.add(p);
				}
				referencedProjectsSet.add(p.getName());
			}

			monitoredProjects = moduleAndModuleReferencedProjects.toArray(new IProject[moduleAndModuleReferencedProjects.size()]);
		}
		else
		{
			for (IProject p : referencedProjects)
			{
				referencedProjectsSet.add(p.getName());
			}
			monitoredProjects = referencedProjects;
		}
		if (!BuilderDependencies.getInstance().isInitialized() && servoyModel.getActiveProject() != null)
		{
			// cache is empty we need a full build on all active solutions
			// this will also delete all markers and create them on accurate file
			BuilderDependencies.getInstance().initialize();
			for (ServoyProject project : servoyModel.getModulesOfActiveProject())
			{
				fullBuild(project.getProject(), progressMonitor);
			}
		}
		else if (kind == FULL_BUILD)
		{
			fullBuild(getProject(), progressMonitor);
		}
		else
		{
			try
			{
				boolean needFullBuild = false;
				IResourceDelta resourcesProjectDelta = null;
				for (IProject p : monitoredProjects)
				{
					/*
					 * If you have a reference to a project and then you close or delete that project it will not do a build. That is why p.exists() and
					 * p.isOpen() is commented.
					 */
					if (/* p.exists() && p.isOpen() && */ !needFullBuild)
					{
						IResourceDelta delta = getDelta(p);
						if (delta != null)
						{
							ServoyDeltaVisitor visitor = new ServoyDeltaVisitor();
							delta.accept(visitor);
							if (visitor.resources.size() > 0 && !ServoyBuilderUtils.canBuildIncremental(visitor.resources))
							{
								// if a module/resources project is changed and we cannot handle the change via the new incremental build we have to fully build the main project in order to make sure all markers are fine
								needFullBuild = true;
							}
							else if (visitor.resources.size() > 0 && p.hasNature(ServoyResourcesProject.NATURE_ID))
							{
								resourcesProjectDelta = delta;
							}
						}
					}
				}
				if (needFullBuild)
				{
					fullBuild(getProject(), progressMonitor);
				}
				else
				{
					IResourceDelta delta = getDelta(getProject());
					if (delta != null)
					{
						incrementalBuild(delta, progressMonitor);
					}
					// servoy builder is not called on resources project, so, if we can do an incremental build, do it when main active project is checked
					if (resourcesProjectDelta != null)
					{
						ServoyProject activeProject = getServoyModel().getActiveProject();
						if (activeProject != null && activeProject.getProject().getName().equals(getProject().getName()))
						{
							incrementalBuild(resourcesProjectDelta, progressMonitor);
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}
		return monitoredProjects;
	}

	@Override
	protected void clean(IProgressMonitor progressMonitor) throws CoreException
	{
		getProject().deleteMarkers(SERVOY_BUILDER_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		BuilderDependencies.getInstance().clear();
	}

	void checkResource(IResource resource)
	{
		try
		{
			SpecProviderState componentsSpecProviderState = WebComponentSpecProvider.getSpecProviderState();

			if (resource instanceof IFile && resource.getName().endsWith(".js"))
			{
				checkDuplicateScopes((IFile)resource);
			}
			else if (resource instanceof IFile && resource.getName().endsWith(".xml"))
			{
				checkXML((IFile)resource);
			}
			else if (resource instanceof IProject)
			{
				IProject project = (IProject)resource;
				if (!project.exists() || (project.isOpen() && project.hasNature(ServoyProject.NATURE_ID)))
				{
					// a project that this builder in interested in was deleted (so a module or the resources proj.)
					// or something has changed in this builder's solution project
					checkServoyProject(project, componentsSpecProviderState);
					checkSpecs(project, componentsSpecProviderState);
					checkModules(project);
					checkResourcesForServoyProject(project);
					checkResourcesForModules(project);
					if (project.exists())
					{
						if (servoyModel.isSolutionActive(project.getName()))
						{
							checkColumns(project);
						}
					}
				}
				else
				{
					if (project.isOpen() && project.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						if (servoyModel.isSolutionActive(getProject().getName()))
						{
							checkServoyProject(getProject(), componentsSpecProviderState);
							checkColumns(getProject());
						}
					}
					if (referencedProjectsSet.contains(resource.getName()))
					{
						// a referenced project has changed... check
						checkResourcesForServoyProject(getProject());
						checkResourcesForModules(getProject());
					}
					if (moduleProjectsSet.contains(resource.getName()))
					{
						// a module project has changed (maybe it was deleted/added); check the modules list
						checkModules(getProject());
						checkResourcesForModules(getProject());
					}
				}
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Exception while performing build", e);
		}
	}

	private void checkResourcesForModules(IProject project)
	{
		deleteMarkers(project, DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE);

		// check this solution project and it's modules to see that they use the same resources project
		ServoyProject servoyProject = getServoyProject(project);
		boolean active = servoyModel.isSolutionActive(project.getName());

		if (servoyProject != null)
		{
			ServoyResourcesProject resourcesProject = servoyProject.getResourcesProject();
			if (active && servoyProject.getSolution() != null && resourcesProject != null)
			{
				// check if all modules are checked out
				String modulesNames = servoyProject.getSolution().getModulesNames();
				ServoyResourcesProject moduleResourcesProject;
				if (modulesNames != null)
				{
					StringTokenizer st = new StringTokenizer(modulesNames, ";,");
					while (st.hasMoreTokens())
					{
						String name = st.nextToken().trim();
						ServoyProject module = getServoyModel().getServoyProject(name);
						if (module != null)
						{
							moduleResourcesProject = module.getResourcesProject();
							if (moduleResourcesProject != null && (!moduleResourcesProject.equals(resourcesProject)))
							{
								// this module has a resources project different than the one of the main solution
								ServoyMarker mk = MarkerMessages.ModuleDifferentResourceProject.fill(name, project.getName());
								addMarker(project, mk.getType(), mk.getText(), -1, MODULE_DIFFERENT_RESOURCE_PROJECT, IMarker.PRIORITY_NORMAL, null, null);
							}
						}
					}
				}
			}
		}
	}

	private void checkSpecs(IProject buildProject, final SpecProviderState componentsSpecProviderState)
	{
		ServoyProject[] modules = servoyModel.getModulesOfActiveProject();
		if (modules != null)
		{
			for (final ServoyProject module : modules)
			{
				if (!Utils.equalObjects(module.getProject().getName(), buildProject.getName()))
				{
					deleteMarkers(module.getProject(), MISSING_SPEC);
					deleteMarkers(module.getProject(), DEPRECATED_SPEC);
					deleteMarkers(module.getProject(), MISSING_PROPERTY_FROM_SPEC);
					module.getSolution().acceptVisitor(new IPersistVisitor()
					{

						@Override
						public Object visit(IPersist o)
						{
							checkSpecs(o, module.getProject(), componentsSpecProviderState);
							return null;
						}
					});
				}
			}
		}
	}

	public static void checkSpecs(IPersist o, IProject project, SpecProviderState componentsSpecProviderState)
	{
		if (o instanceof WebComponent)
		{
			String typeName = ((WebComponent)o).getTypeName();
			WebObjectSpecification spec = componentsSpecProviderState.getWebObjectSpecification(typeName);
			if (spec == null)
			{
				if (typeName != null)
				{
					String[] webcomponentNameAndSpec = typeName.split("-");
					ServoyMarker mk = MarkerMessages.MissingSpecification.fill("Web Component", webcomponentNameAndSpec[webcomponentNameAndSpec.length - 1],
						webcomponentNameAndSpec[0]);
					IMarker marker = addMarker(ServoyBuilderUtils.getPersistResource(o.getAncestor(IRepository.FORMS)), mk.getType(), mk.getText(), -1,
						MISSING_SPECIFICATION, IMarker.PRIORITY_NORMAL, null, o);
					try
					{
						if (marker != null) marker.setAttribute("packageName", webcomponentNameAndSpec[0]);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
				else if (!PersistHelper.isOverrideOrphanElement((ISupportExtendsID)o))
				{
					ServoyLog.logError("Type name not found for webcomponent " + ((WebComponent)o).getName(), null);
				}
			}
			else if (spec.isDeprecated())
			{
				String customSeverity = getSeverity(DEPRECATED_SPECIFICATION.getLeft(), DEPRECATED_SPECIFICATION.getRight().name(), o);
				if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
				{
					ServoyMarker mk = MarkerMessages.DeprecatedSpecification.fill(typeName,
						"web component" + (((WebComponent)o).getName() != null ? " with name '" + ((WebComponent)o).getName() + "'" : "'"),
						spec.getDeprecatedMessage());
					IMarker marker = addMarker(ServoyBuilderUtils.getPersistResource(o.getAncestor(IRepository.FORMS)), mk.getType(), mk.getText(), -1,
						getTranslatedSeverity(customSeverity, DEPRECATED_SPECIFICATION.getRight()), IMarker.PRIORITY_NORMAL, null, o);
					try
					{
						marker.setAttribute("replacement", spec.getReplacement());
						marker.setAttribute("uuid", o.getUUID().toString());
						marker.setAttribute("solutionName", project.getName());
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
			else
			{
				checkMissingProperties((JSONObject)((WebComponent)o).getOwnProperty(StaticContentSpecLoader.PROPERTY_JSON.getPropertyName()), spec, o, project,
					"");
			}
		}
		if (o instanceof LayoutContainer && ((LayoutContainer)o).getPackageName() != null && !PersistHelper.isOverrideOrphanElement((LayoutContainer)o))
		{
			WebLayoutSpecification spec = null;
			PackageSpecification<WebLayoutSpecification> pkg = componentsSpecProviderState.getLayoutSpecifications().get(((LayoutContainer)o).getPackageName());
			if (pkg != null)
			{
				spec = pkg.getSpecification(((LayoutContainer)o).getSpecName());
			}
			if (spec == null)
			{
				ServoyMarker mk = MarkerMessages.MissingSpecification.fill("Layout", ((LayoutContainer)o).getSpecName(), ((LayoutContainer)o).getPackageName());
				IMarker marker = addMarker(ServoyBuilderUtils.getPersistResource(o.getAncestor(IRepository.FORMS)), mk.getType(), mk.getText(), -1,
					MISSING_SPECIFICATION, IMarker.PRIORITY_NORMAL, null, o);
				try
				{
					marker.setAttribute("packageName", ((LayoutContainer)o).getPackageName());
				}
				catch (CoreException e)
				{
					ServoyLog.logError(e);
				}
			}
			else if (spec.isDeprecated())
			{
				String customSeverity = getSeverity(DEPRECATED_SPECIFICATION.getLeft(), DEPRECATED_SPECIFICATION.getRight().name(), o);
				if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
				{
					ServoyMarker mk = MarkerMessages.DeprecatedSpecification.fill(((LayoutContainer)o).getSpecName(), "layout", spec.getDeprecatedMessage());
					IMarker marker = addMarker(ServoyBuilderUtils.getPersistResource(o.getAncestor(IRepository.FORMS)), mk.getType(), mk.getText(), -1,
						getTranslatedSeverity(customSeverity, DEPRECATED_SPECIFICATION.getRight()), IMarker.PRIORITY_NORMAL, null, o);
					try
					{
						marker.setAttribute("replacement", spec.getReplacement());
						marker.setAttribute("uuid", o.getUUID().toString());
						marker.setAttribute("solutionName", project.getName());
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}
			}
		}
	}

	private void checkModules(IProject project)
	{
		deleteMarkers(project, MISSING_MODULES_MARKER_TYPE);
		deleteMarkers(project, MISPLACED_MODULES_MARKER_TYPE);
		deleteMarkers(project, INVALID_MOBILE_MODULE_MARKER_TYPE);

		final ServoyProject servoyProject = getServoyProject(project);
		boolean active = servoyModel.isSolutionActive(project.getName());

		if (servoyProject != null && active && servoyProject.getSolution() != null)
		{
			// check if all modules are checked out
			String[] modulesNames = Utils.getTokenElements(servoyProject.getSolution().getModulesNames(), ",", true);
			if (modulesNames != null)
			{
				for (String name : modulesNames)
				{
					ServoyProject module = getServoyModel().getServoyProject(name);
					if (module == null)
					{
						// test if the project is not really there (but closed)
						IProject prj = ResourcesPlugin.getWorkspace().getRoot().getProject(name);
						if (prj != null && prj.exists() && !prj.isOpen())
						{
							try
							{
								// if it is closed then just open it and try to get the servoy project again.
								prj.open(new NullProgressMonitor());
								getServoyModel().refreshServoyProjects();
								module = getServoyModel().getServoyProject(name);
							}
							catch (CoreException e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					if (module == null)
					{
						ServoyMarker mk = MarkerMessages.ModuleNotFound.fill(name, servoyProject.getSolution().getName());
						IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, MODULE_NOT_FOUND, IMarker.PRIORITY_HIGH, null, null);
						if (marker != null)
						{
							try
							{

								marker.setAttribute("moduleName", name);
								marker.setAttribute("solutionName", servoyProject.getSolution().getName());
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					else if (SolutionMetaData.isServoyMobileSolution(servoyProject.getSolution()) &&
						!SolutionMetaData.isServoyMobileSolution(module.getSolution())) checkMobileModule(servoyProject, module);
					if (SolutionMetaData.isServoyMobileSolution(servoyProject.getSolution()) && module != null &&
						(module.getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE &&
							module.getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE_MODULE))
					{
						String message = "Module " + module.getSolution().getName() +
							" is a mobile solution module, it should have solution type Mobile or Mobile shared module.";
						IMarker marker = addMarker(project, MISPLACED_MODULES_MARKER_TYPE, message, -1, MODULE_MISPLACED, IMarker.PRIORITY_LOW, null, null);
						try
						{
							marker.setAttribute("SolutionName", module.getSolution().getName());
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
					if (!SolutionMetaData.isServoyMobileSolution(servoyProject.getSolution()) && module != null &&
						SolutionMetaData.isServoyMobileSolution(module.getSolution()))
					{
						String message = "Module " + module.getSolution().getName() +
							" is a non-mobile solution module, it should have solution type Mobile shared module or other non-mobile type.";
						IMarker marker = addMarker(project, MISPLACED_MODULES_MARKER_TYPE, message, -1, MODULE_MISPLACED, IMarker.PRIORITY_LOW, null, null);
						try
						{
							marker.setAttribute("SolutionName", module.getSolution().getName());
							marker.setAttribute("SolutionType", SolutionMetaData.MOBILE);
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}
		}
	}

	private void checkMobileModule(ServoyProject mobileProject, ServoyProject module)
	{
		final boolean[] isMobileModuleValid = { true };
		module.getSolution().acceptVisitor(new IPersistVisitor()
		{
			public Object visit(IPersist o)
			{
				if (o instanceof Solution || o instanceof Relation || o instanceof RelationItem || o instanceof ScriptCalculation || o instanceof TableNode ||
					(o instanceof ScriptVariable && ((ScriptVariable)o).getParent() instanceof Solution))
				{
					return IPersistVisitor.CONTINUE_TRAVERSAL;
				}
				else
				{
					isMobileModuleValid[0] = false;
					return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
			}
		});

		if (!isMobileModuleValid[0])
		{
			String message = "Module " + module.getSolution().getName() + " is a mobile solution module, so it should contain only relations and calculations.";
			addMarker(mobileProject.getProject(), INVALID_MOBILE_MODULE_MARKER_TYPE, message, -1, MODULE_INVALID_MOBILE, IMarker.PRIORITY_NORMAL, null, null);
		}
	}

	public static void checkDuplicateScopes(IFile scriptFile)
	{
		IServoyModel servoyModel = ServoyModelFinder.getServoyModel();
		if (!servoyModel.isSolutionActive(scriptFile.getProject().getName())) return;

		if (scriptFile.getParent() == scriptFile.getProject() && scriptFile.getName().endsWith(SolutionSerializer.JS_FILE_EXTENSION))
		{
			String scopeName = scriptFile.getName().substring(0, scriptFile.getName().length() - SolutionSerializer.JS_FILE_EXTENSION.length());
			String lowerCaseScopeName = scopeName.toLowerCase();
			if (lowerCaseScopeName.equals(ScriptVariable.GLOBAL_SCOPE)) return;

			if (scriptFile.exists())
			{
				deleteMarkers(scriptFile, DUPLICATE_SCOPE_NAME_MARKER_TYPE);
			}
			List<Pair<String, IRootObject>> scopes = servoyModel.getFlattenedSolution().getAllScopes();
			for (Pair<String, IRootObject> scope : scopes)
			{
				if (scope.getLeft().toLowerCase().equals(lowerCaseScopeName) && !scope.getRight().getName().equals(scriptFile.getProject().getName()))
				{
					String otherFile = scope.getRight().getName() + '/' + scope.getLeft() + SolutionSerializer.JS_FILE_EXTENSION;
					IFile file = scriptFile.getWorkspace().getRoot().getFile(Path.fromPortableString(otherFile));
					if (scriptFile.exists() &&
						!isParentImportHook(servoyModel.getServoyProject(scriptFile.getProject().getName()).getSolution(), (Solution)scope.getRight()))
					{
						// duplicate found
						ServoyMarker mk = MarkerMessages.DuplicateScopeFound.fill(scope.getLeft(), scope.getRight().getName());
						addMarker(scriptFile, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL,
							scriptFile.getProject().getName());
						if (file.exists())
						{
							deleteMarkers(file, DUPLICATE_SCOPE_NAME_MARKER_TYPE);
							mk = MarkerMessages.DuplicateScopeFound.fill(scopeName, scriptFile.getProject().getName());
							addMarker(file, mk.getType(), mk.getText(), -1, IMarker.SEVERITY_ERROR, IMarker.PRIORITY_NORMAL, file.getProject().getName());
						}
					}
					else if (file.exists())
					{
						// this file was deleted or renamed, check the other file
						checkDuplicateScopes(file);
					}
				}
			}
		}
	}

	private static boolean isParentImportHook(Solution persistParent, Solution dupParent)
	{
		if (!dupParent.equals(persistParent) &&
			(persistParent.getSolutionType() == SolutionMetaData.PRE_IMPORT_HOOK || persistParent.getSolutionType() == SolutionMetaData.POST_IMPORT_HOOK))
		{
			return true;
		}
		return false;
	}

	public static void checkPersistDuplicateUUID()
	{
		ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
		if (modules != null)
		{
			for (ServoyProject module : modules)
			{
				deleteMarkers(module.getProject(), DUPLICATE_UUID);
			}
		}
		IProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject().getProject();
		FlattenedSolution fs = ServoyModelFinder.getServoyModel().getFlattenedSolution();
		if (fs instanceof DeveloperFlattenedSolution)
		{
			Map<UUID, List<IPersist>> duplicates = ((DeveloperFlattenedSolution)fs).getDuplicateUUIDList();
			if (duplicates != null)
			{
				for (UUID uuid : duplicates.keySet())
				{
					List<IPersist> lst = duplicates.get(uuid);
					if (lst.size() >= 2)
					{
						IPersist first = lst.get(0);
						for (IPersist persist : lst)
						{
							IPersist other = first;
							if (persist == other) other = lst.get(1);
							ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(persist.getUUID(), persist,
								SolutionSerializer.getRelativePath(persist, false) + SolutionSerializer.getFileName(persist, false), other,
								SolutionSerializer.getRelativePath(other, false) + SolutionSerializer.getFileName(other, false));
							addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE,
								IMarker.PRIORITY_HIGH, null, persist);
						}
					}
				}
			}
		}

	}

	public static void checkPersistDuplicateName()
	{
		// this is a special case
		ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
		if (modules != null)
		{
			for (ServoyProject module : modules)
			{
				deleteMarkers(module.getProject(), DUPLICATE_NAME_MARKER_TYPE);
				deleteMarkers(module.getProject(), DUPLICATE_REFERENCED_FORM_MARKER_TYPE);
				deleteMarkers(module.getProject(), WRONG_OVERRIDE_PARENT);
			}
			FlattenedSolution fs = ServoyModelFinder.getServoyModel().getFlattenedSolution();
			IProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject().getProject();
			if (fs instanceof DeveloperFlattenedSolution)
			{
				Map<String, Map<String, List<IPersist>>> duplicates = ((DeveloperFlattenedSolution)fs).getDuplicateNamesList();
				if (duplicates != null)
				{
					for (String name : duplicates.keySet())
					{
						Map<String, List<IPersist>> duplicateMap = duplicates.get(name);
						for (String key : duplicateMap.keySet())
						{
							List<IPersist> lst = duplicateMap.get(key);
							if (lst.size() >= 2)
							{
								for (IPersist persist : lst)
								{
									if (persist instanceof Relation)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("relation", name, persist.getRootObject().getName());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null,
											persist);
									}
									else if (persist instanceof ValueList)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("valuelist", name, persist.getRootObject().getName());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null, persist);
									}
									else if (persist instanceof Media)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("media", name, persist.getRootObject().getName());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null, persist);
									}
									else if (persist instanceof Form)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("form", name, persist.getRootObject().getName());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null, persist);
									}
									else if (persist instanceof ScriptCalculation)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("table calculation ", ((ScriptCalculation)persist).getName(),
											persist.getRootObject().getName() + "  on table  " + ((TableNode)persist.getParent()).getDataSource());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null,
											persist);

									}
									else if (persist instanceof AggregateVariable)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("aggregate variable", ((AggregateVariable)persist).getName(),
											persist.getRootObject().getName() + "  on table  " + ((TableNode)persist.getParent()).getDataSource());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null,
											persist);
									}
									else if (persist instanceof ScriptMethod && persist.getParent() instanceof TableNode)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("table method", ((ScriptMethod)persist).getName(),
											persist.getRootObject().getName() + "  on table  " + ((TableNode)persist.getParent()).getDataSource());
										addMarker(activeProject, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL,
											null,
											persist);
									}
									else if (persist instanceof IScriptElement)
									{
										String type = "method";
										if (persist instanceof ScriptVariable)
										{
											type = "variable";
										}
										int lineNumber = ((IScriptElement)persist).getLineNumberOffset();
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(type, ((IScriptElement)persist).getName(),
											persist.getRootObject().getName() + "." + ((IScriptElement)persist).getScopeName());
										ServoyBuilder.addMarker(activeProject, mk.getType(), mk.getText(), lineNumber,
											ServoyBuilder.DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, persist);

									}
								}
							}
						}
					}
				}
			}
		}
	}

	private static void addDeprecatedRelationWarningIfNeeded(IPersist persist, String relationsString, IResource markerResource, String message,
		FlattenedSolution flattenedSolution)
	{
		if (relationsString != null)
		{
			String[] aRelationsString = relationsString.split(" ");
			if (aRelationsString.length > 0)
			{
				String[] relations = Utils.stringSplit(aRelationsString[0], ".");
				Relation relation;
				for (String r : relations)
				{
					relation = flattenedSolution.getRelation(r);
					if (relation != null)
					{
						addDeprecatedElementWarningIfNeeded(persist, relation, markerResource, message.replace("{r}", relation.getName()));
					}
				}
			}
		}
	}

	private static void addDeprecatedElementWarningIfNeeded(IPersist persist, ISupportDeprecated deprecatedPersist, IResource markerResource, String message)
	{
		if (deprecatedPersist != null)
		{
			String deprecatedInfo = deprecatedPersist.getDeprecated();

			if (deprecatedInfo != null)
			{
				addMarker(markerResource, DEPRECATED_ELEMENT_USAGE, message + " " + deprecatedInfo, -1, DEPRECATED_ELEMENT_USAGE_PROBLEM,
					IMarker.PRIORITY_NORMAL,
					null, persist);
			}
		}
	}

	/*
	 * Checks usage of deprecated Form/Relation/Valuelist/Media inside elements properties
	 */
	public static void checkDeprecatedElementUsage(IPersist persist, IResource markerResource, FlattenedSolution flattenedSolution)
	{
		String elementName = null;
		if (persist instanceof ISupportName) elementName = ((ISupportName)persist).getName();
		if (elementName == null && persist != null) elementName = persist.getUUID().toString();

		if (persist instanceof Solution)
		{
			// check for deprecated first form
			Form firstForm = flattenedSolution.getForm(((Solution)persist).getFirstFormID());
			if (firstForm != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, firstForm, markerResource,
					"Solution \"" + elementName + "\" has a deprecated first form \"" + firstForm.getName() + "\".");
			}
		}

		if (persist instanceof Form)
		{
			Form form = (Form)persist;
			Form extendsForm = form.getExtendsForm();

			if (extendsForm != null)
			{
				if ((form.isResponsiveLayout() != extendsForm.isResponsiveLayout()) || (form.getUseCssPosition() != extendsForm.getUseCssPosition()))
				{
					Iterator<ISupportFormElement> uiElements = extendsForm.getFormElementsSortedByFormIndex();
					// do now show if no ui is present
					if (uiElements.hasNext())
					{
						String formLayoutType = "absolute layout";
						if (form.isResponsiveLayout())
						{
							formLayoutType = "responsive layout";
						}
						else if (form.getUseCssPosition())
						{
							formLayoutType = "css position layout";
						}
						String message = "The " + formLayoutType + " form '" + form.getName() + "' should not extend the ";
						String extendsFormLayoutType = "absolute layout";
						if (extendsForm.isResponsiveLayout())
						{
							extendsFormLayoutType = "responsive layout";
						}
						else if (extendsForm.getUseCssPosition())
						{
							extendsFormLayoutType = "css position layout";
						}
						message += extendsFormLayoutType + " form '" + extendsForm.getName() + "'.";

						IMarker marker = addMarker(markerResource, SUPERFORM_PROBLEM_TYPE, message, -1, SUPERFORM_PROBLEM, IMarker.PRIORITY_NORMAL, null,
							persist);
						try
						{
							marker.setAttribute("Uuid", form.getUUID().toString());
							marker.setAttribute("SolutionName", form.getSolution().getName());
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}

				// check form extends of a deprecated form
				addDeprecatedElementWarningIfNeeded(persist, extendsForm, markerResource,
					"Form \"" + elementName + "\" extends a deprecated form \"" + extendsForm.getName() + "\".");
			}

			String initialSort = form.getInitialSort();
			if (initialSort != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, initialSort, markerResource,
					"Form \"" + elementName + "\" has a deprecated relation \"{r}\" as initial sort.", flattenedSolution);
			}
		}
		else if (persist instanceof TabPanel)
		{
			Iterator<IPersist> tabs = ((TabPanel)persist).getTabs();
			Tab tab;
			Form tabForm;
			String tabRelationName;
			Relation tabRelation;
			while (tabs.hasNext())
			{
				// check usage of deprecated form inside a tab
				tab = (Tab)tabs.next();
				tabForm = flattenedSolution.getForm(tab.getContainsFormID());
				if (tabForm != null)
				{
					addDeprecatedElementWarningIfNeeded(persist, tabForm, markerResource,
						"Element \"" + elementName + "\" contains a deprecated form \"" + tabForm.getName() + "\".");
				}
				// check usage of deprecated relation for a tab
				tabRelationName = tab.getRelationName();
				if (tabRelationName != null)
				{
					addDeprecatedRelationWarningIfNeeded(persist, tabRelationName, markerResource,
						"Element \"" + elementName + "\" has a deprecated relation \"{r}\".", flattenedSolution);
				}
			}
		}
		else if (persist instanceof Field)
		{
			// check usage of deprecated valuelist inside a field
			ValueList valuelist = flattenedSolution.getValueList(((Field)persist).getValuelistID());
			if (valuelist != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, valuelist, markerResource,
					"Field \"" + elementName + "\" has a deprecated valuelist \"" + valuelist.getName() + "\".");
			}
		}
		else if (persist instanceof ValueList)
		{
			// check usage of deprecated valuelist as fallback valuelist
			ValueList fallbackValuelist = flattenedSolution.getValueList(((ValueList)persist).getFallbackValueListID());
			if (fallbackValuelist != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, fallbackValuelist, markerResource,
					"Valuelist \"" + elementName + "\" has a deprecated fallback valuelist \"" + fallbackValuelist.getName() + "\".");
				if (fallbackValuelist.getFallbackValueListID() != null)
				{
					ServoyMarker mk = MarkerMessages.ValuelistFallbackOfFallbackFound.fill(((ValueList)persist).getName(), fallbackValuelist.getName());
					addMarker(markerResource, mk.getType(), mk.getText(), -1, VALUELIST_WITH_FALLBACK_OF_FALLBACK, IMarker.PRIORITY_HIGH, null, persist);
				}
			}

			// check usage of deprecated relation inside a valuelist
			String relationName = ((ValueList)persist).getRelationName();
			if (relationName != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, relationName, markerResource,
					"Valuelist \"" + elementName + "\" has a deprecated relation \"{r}\".",
					flattenedSolution);
			}

			String sortOptions = ((ValueList)persist).getSortOptions();
			if (sortOptions != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, sortOptions, markerResource,
					"Valuelist \"" + elementName + "\" has a deprecated relation \"{r}\" as sort option.", flattenedSolution);
			}
		}
		else if (persist instanceof Portal)
		{
			// check usage of deprecated relation inside a portal
			String relationName = ((Portal)persist).getRelationName();
			if (relationName != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, relationName, markerResource, "Portal \"" + elementName + "\" has a deprecated relation \"{r}\".",
					flattenedSolution);
			}
		}
		else if (persist instanceof Relation)
		{
			String initialSort = ((Relation)persist).getInitialSort();
			if (initialSort != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, initialSort, markerResource,
					"Relation \"" + elementName + "\" has a deprecated relation \"{r}\" as initial sort.", flattenedSolution);
			}
		}

		if (persist instanceof ISupportMedia)
		{
			// check usage of deprecated media
			Media media = flattenedSolution.getMedia(((ISupportMedia)persist).getImageMediaID());
			if (media != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, media, markerResource,
					"Element \"" + elementName + "\" has a deprecated image media \"" + media.getName() + "\".");
			}
		}
		if (persist instanceof ISupportDataProviderID)
		{
			// check usage of deprecated relation inside dataprovider ids
			String dataProviderID = ((ISupportDataProviderID)persist).getDataProviderID();
			if (dataProviderID != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, dataProviderID, markerResource,
					"Element \"" + elementName + "\" has a dataprovider with a deprecated relation \"{r}\".", flattenedSolution);
			}
		}
	}

	public static void checkDeprecatedPropertyUsage(IPersist persist, IResource markerResource, IProject project)
	{
//		if (persist instanceof Solution)
//		{
//			Solution solution = (Solution)persist;
//
//			// loginForm is deprecated, use loginSolution (not needed for WebClient)
//			if (solution.getLoginFormID() > 0)
//			{
//				try
//				{
//					if (solution.getLoginSolutionName() != null)
//					{
//						// login form will be ignored
//						addDeprecatedPropertyUsageMarker(persist, markerResource, project, DEPRECATED_PROPERTY_USAGE_PROBLEM,
//							StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName(),
//							"Solution '" + solution.getName() + "' has a loginForm property set which is overridden by the loginSolutionName property.");
//					}
//					else if (solution.getSolutionType() != SolutionMetaData.WEB_CLIENT_ONLY && solution.getSolutionType() != SolutionMetaData.MOBILE &&
//						solution.getSolutionType() != SolutionMetaData.NG_CLIENT_ONLY)
//					{
//						// loginForm is deprecated
//						addDeprecatedPropertyUsageMarker(persist, markerResource, project, DEPRECATED_PROPERTY_USAGE_PROBLEM,
//							StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName(),
//							"Solution '" + solution.getName() + "' has a loginForm property set which is deprecated, use loginSolutionName property instead.");
//					}
//				}
//				catch (Exception e)
//				{
//					ServoyLog.logError(e);
//				}
//			}
//		}

		if (persist instanceof Form || persist instanceof Portal)
		{
			String rowBgColorCalculation = null;
			String type = "Form";
			if (persist instanceof Form)
			{
				rowBgColorCalculation = ((Form)persist).getRowBGColorCalculation();
			}
			else
			{
				rowBgColorCalculation = ((Portal)persist).getRowBGColorCalculation();
				type = "Portal";
			}
			if (rowBgColorCalculation != null)
			{
				try
				{
					//only rowBGColorCalculation deprecated property usage marker is error by default
					addDeprecatedPropertyUsageMarker(persist, markerResource, project,
						new Pair<String, ProblemSeverity>("deprecatedPropertyUsage", ProblemSeverity.ERROR),
						StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION.getPropertyName(), type + " '" + ((ISupportName)persist).getName() +
							"' has rowBGColorCalculation property set which is deprecated, use CSS (odd/even/selected) or onRender event instead.");
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	private static void addDeprecatedPropertyUsageMarker(IPersist persist, IResource markerResource, IProject project,
		Pair<String, ProblemSeverity> serverityPair,
		String propertyName,
		String message) throws CoreException
	{
		if (message != null)
		{
			IMarker marker = addMarker(markerResource, DEPRECATED_PROPERTY_USAGE, message, -1, serverityPair, IMarker.PRIORITY_NORMAL, null, persist);
			if (marker != null)
			{
				marker.setAttribute("Uuid", persist.getUUID().toString());
				marker.setAttribute("SolutionName", project.getName());
				marker.setAttribute("PropertyName", propertyName);
				marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName(propertyName, persist.getClass()));
			}
		}
	}

	private static void checkMissingProperties(JSONObject json, PropertyDescription description, IPersist o, IProject project, String currentPath)
	{
		if (json != null)
		{
			for (String key : json.keySet())
			{
				if (StaticContentSpecLoader.PROPERTY_SIZE.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_CSS_POSITION.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_LOCATION.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_VISIBLE.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_ENABLED.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_NAME.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_COMMENT.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_ANCHORS.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_GROUPID.getPropertyName().equals(key) ||
					StaticContentSpecLoader.PROPERTY_FORMINDEX.getPropertyName().equals(key) ||
					IChildWebObject.UUID_KEY.equals(key))
				{
					continue;
				}
				PropertyDescription pd = description.getProperty(key);
				if (pd == null)
				{
					if ((description instanceof WebObjectSpecification) && ((WebObjectSpecification)description).getHandler(key) != null)
						continue;
					addMissingPropertyFromSpecMarker(o, project, key, currentPath != null && currentPath.length() == 0 ? key : currentPath + "." + key);
				}
				else if (pd.getType() instanceof CustomJSONPropertyType< ? >)
				{
					Object value = json.opt(key);
					if (value instanceof JSONObject)
					{
						checkMissingProperties((JSONObject)value, ((CustomJSONPropertyType)pd.getType()).getCustomJSONTypeDefinition(), o, project,
							currentPath != null && currentPath.length() == 0 ? key : currentPath + "." + key);
					}
					else if (value instanceof JSONArray)
					{
						JSONArray arr = ((JSONArray)value);
						for (int i = 0; i < arr.length(); i++)
						{
							if (arr.get(i) instanceof JSONObject)
							{
								checkMissingProperties((JSONObject)arr.get(i), ((CustomJSONPropertyType)pd.getType()).getCustomJSONTypeDefinition(), o,
									project,
									currentPath != null && currentPath.length() == 0 ? key + "_arrindex" + i : currentPath + "." + key + "_arrindex" + i);
							}
						}
					}
				}
			}
		}
	}

	private static void addMissingPropertyFromSpecMarker(IPersist persist, IProject project,
		String propertyName, String propertyPath)
	{
		Form form = (Form)persist.getAncestor(IRepository.FORMS);
		ServoyMarker mk = MarkerMessages.MissingPropertyFromSpecification.fill(((WebComponent)persist).getName(), form.getName(),
			propertyName);
		IMarker marker = addMarker(ServoyBuilderUtils.getPersistResource(form), mk.getType(), mk.getText(), -1, MISSING_PROPERTY_FROM_SPECIFICATION,
			IMarker.PRIORITY_NORMAL,
			null, persist);
		if (marker != null)
		{
			try
			{
				marker.setAttribute("Uuid", persist.getUUID().toString());
				marker.setAttribute("SolutionName", project.getName());
				marker.setAttribute("PropertyName", propertyPath);
			}
			catch (CoreException e)
			{
				ServoyLog.logError(e);
			}
		}
	}

	private void addMissingServer(IPersist persist, Map<String, IPersist> missingServers, List<String> goodServers)
	{
		String serverName = null;
		if (persist instanceof Form)
		{
			serverName = ((Form)persist).getServerName();
		}
		else if (persist instanceof ValueList)
		{
			serverName = ((ValueList)persist).getServerName();
		}
		else if (persist instanceof TableNode)
		{
			serverName = ((TableNode)persist).getServerName();
		}
		else if (persist instanceof Relation)
		{
			serverName = ((Relation)persist).getPrimaryServerName();
			String foreignServer = ((Relation)persist).getForeignServerName();
			if (foreignServer != null && !missingServers.containsKey(foreignServer) && !goodServers.contains(foreignServer))
			{
				IServer server = ApplicationServerRegistry.get().getServerManager().getServer(foreignServer);
				if (server != null) goodServers.add(foreignServer);
				else missingServers.put(foreignServer, persist);
			}
		}
		if (serverName != null && !missingServers.containsKey(serverName) && !goodServers.contains(serverName) &&
			!serverName.equals(DataSourceUtils.INMEM_DATASOURCE) && !serverName.equals(DataSourceUtils.VIEW_DATASOURCE))
		{
			IServerManagerInternal sm = ApplicationServerRegistry.get().getServerManager();
			IServer server = sm.getServer(serverName);
			if (server != null) goodServers.add(serverName);
			else missingServers.put(serverName, persist);
		}
	}

	public static void checkDuplicateUUID(IPersist persist, IProject project)
	{
		boolean found = false;

		if (Utils.getAsBoolean(((AbstractBase)persist).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID)))
		{
			UUID uuid = persist.getUUID();
			Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
			IPath path = new Path(pathPair.getLeft());
			String location = null;
			if (path.segmentCount() == 1)
			{
				IProject p = ResourcesPlugin.getWorkspace().getRoot().getProject(pathPair.getLeft());
				location = p.getLocation().toOSString();
			}
			else
			{
				IFolder folder = ResourcesPlugin.getWorkspace().getRoot().getFolder(path);
				location = folder.getLocation().toOSString();

			}
			java.io.File file = new File(location);
			File[] files = file.listFiles(new FileFilter()
			{
				public boolean accept(File pathname)
				{
					return SolutionSerializer.isJSONFile(pathname.getName()) && pathname.isFile() && !pathname.getName().equals(SolutionSerializer.MEDIAS_FILE);
				}
			});
			String persistFile = ((AbstractBase)persist).getSerializableRuntimeProperty(IScriptProvider.FILENAME);
			if (files != null)
			{
				for (File f : files)
				{
					UUID newUUID = SolutionDeserializer.getUUID(f);
					if (newUUID != null && newUUID.equals(uuid) && !pathPair.getRight().equals(f.getName()))
					{
						found = true;
						IFile fileForLocation = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(
							Path.fromPortableString(persistFile.replace('\\', '/')));
						ServoyMarker mk = MarkerMessages.UUIDDuplicate.fill(persist.getUUID());
						addMarker(ResourcesPlugin.getWorkspace().getRoot()
							.getFile(new Path(pathPair.getLeft() + pathPair.getRight())), mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE,
							IMarker.PRIORITY_HIGH, fileForLocation.toString(),
							persist);
						break; // only 1 marker has to be set for this persist.
					}
				}
			}
		}
		if (!found)
		{
			((AbstractBase)persist).setRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID, null);
		}
	}

	private boolean hasDeletedMarkers = false;

	private void checkServoyProject(final IProject project, final SpecProviderState componentsSpecProviderState)
	{
		// only log exceptions to max count
		exceptionCount = 0;
		deleteMarkers(project, PROJECT_DESERIALIZE_MARKER_TYPE);
		deleteMarkers(project, SOLUTION_PROBLEM_MARKER_TYPE);
		deleteMarkers(project, PROJECT_RELATION_MARKER_TYPE);
		deleteMarkers(project, MEDIA_MARKER_TYPE);
		deleteMarkers(project, CALCULATION_MARKER_TYPE);
		deleteMarkers(project, PROJECT_FORM_MARKER_TYPE);
		deleteMarkers(project, INVALID_TABLE_NODE_PROBLEM);
		deleteMarkers(project, PROJECT_VALUELIST_MARKER_TYPE);
		deleteMarkers(project, DUPLICATE_UUID);
		deleteMarkers(project, DUPLICATE_SIBLING_UUID);
		deleteMarkers(project, DUPLICATE_NAME_MARKER_TYPE);
		deleteMarkers(project, WRONG_OVERRIDE_PARENT);
		deleteMarkers(project, DUPLICATE_REFERENCED_FORM_MARKER_TYPE);
		deleteMarkers(project, RESERVED_WINDOW_OBJECT_USAGE_TYPE);
		deleteMarkers(project, MISSING_SERVER);
		deleteMarkers(project, BAD_STRUCTURE_MARKER_TYPE);
		deleteMarkers(project, MISSING_STYLE);
		deleteMarkers(project, SCRIPT_MARKER_TYPE);
		deleteMarkers(project, EVENT_METHOD_MARKER_TYPE);
		deleteMarkers(project, I18N_MARKER_TYPE);
		deleteMarkers(project, INVALID_SORT_OPTION);
		deleteMarkers(project, PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE);
		deleteMarkers(project, INVALID_EVENT_METHOD);
		deleteMarkers(project, INVALID_DATAPROVIDERID);
		deleteMarkers(project, INVALID_COMMAND_METHOD);
		deleteMarkers(project, DEPRECATED_PROPERTY_USAGE);
		deleteMarkers(project, DEPRECATED_SCRIPT_ELEMENT_USAGE);
		deleteMarkers(project, MULTIPLE_METHODS_ON_SAME_ELEMENT);
		deleteMarkers(project, UNRESOLVED_RELATION_UUID);
		deleteMarkers(project, MISSING_DRIVER);
		deleteMarkers(project, OBSOLETE_ELEMENT);
		deleteMarkers(project, HIDDEN_TABLE_STILL_IN_USE);
		deleteMarkers(project, MISSING_CONVERTER);
		deleteMarkers(project, LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE);
		deleteMarkers(project, FORM_DUPLICATE_PART_MARKER_TYPE);
		deleteMarkers(project, METHOD_NUMBER_OF_ARGUMENTS_MISMATCH_TYPE);
		deleteMarkers(project, SERVER_CLONE_CYCLE_TYPE);
		deleteMarkers(project, DEPRECATED_ELEMENT_USAGE);
		deleteMarkers(project, ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE);
		deleteMarkers(project, LINGERING_TABLE_FILES_TYPE);
		deleteMarkers(project, DUPLICATE_MEM_TABLE_TYPE);
		deleteMarkers(project, SUPERFORM_PROBLEM_TYPE);
		deleteMarkers(project, MISSING_SPEC);
		deleteMarkers(project, METHOD_OVERRIDE);
		deleteMarkers(project, DEPRECATED_SPEC);
		deleteMarkers(project, MISSING_PROPERTY_FROM_SPEC);
		deleteMarkers(project, PARAMETERS_MISMATCH);
		deleteMarkers(project, NAMED_FOUNDSET_DATASOURCE);
		deleteMarkers(project, CONSTANTS_USED_MARKER_TYPE);
		try
		{
			if (project.getReferencedProjects() != null)
			{
				for (IProject referenced : project.getReferencedProjects())
				{
					if (referenced.exists() && referenced.isOpen() && referenced.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						deleteMarkers(referenced, DEPRECATED_SCRIPT_ELEMENT_USAGE);
						deleteMarkers(referenced, RESERVED_WINDOW_OBJECT_USAGE_TYPE);
					}
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}

		hasDeletedMarkers = true;

		if (getServoyModel().getDataModelManager() != null)
		{
			getServoyModel().getDataModelManager().removeAllMissingDBIFileMarkers();
		}

		final ServoyProject servoyProject = getServoyProject(project);
		boolean active = servoyModel.isSolutionActive(project.getName());

		final Map<String, IPersist> missingServers = new HashMap<String, IPersist>();
		final List<String> goodServers = new ArrayList<String>();
		if (servoyProject != null)
		{
			if (active)
			{
				addDeserializeProblemMarkersIfNeeded(servoyProject);
				refreshDBIMarkers();
				checkPersistDuplicateName();
				checkPersistDuplicateUUID();
				checkServers(project);
				checkDataSources(project);

				final Solution solution = servoyProject.getSolution();

				ServoyBuilderUtils.checkServiceSolutionMustAuthenticate(servoyModel, solution, project);

				if (!servoyModel.shouldBeModuleOfActiveSolution(solution.getName()) &&
					solution.getSolutionMetaData().getFileVersion() > AbstractRepository.repository_version)
				{
					ServoyMarker mk = MarkerMessages.SolutionWithHigherFileVersion.fill("Solution", solution.getName());
					addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_WITH_HIGHER_FILE_VERSION, IMarker.PRIORITY_HIGH, null, null);
				}
				final ServoyProject[] modules = getSolutionModules(servoyProject);
				if (modules != null)
				{
					for (ServoyProject module : modules)
					{
						if (module.getSolution() != null && !module.equals(servoyProject))
						{
							final IProject moduleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
								module.getSolution().getName());
							deleteMarkers(moduleProject, SOLUTION_PROBLEM_MARKER_TYPE);

							if (module.getSolutionMetaData().getFileVersion() > AbstractRepository.repository_version)
							{
								ServoyMarker mk = MarkerMessages.SolutionWithHigherFileVersion.fill("Module",
									module.getSolution().getName());
								addMarker(moduleProject, mk.getType(), mk.getText(), -1, SOLUTION_WITH_HIGHER_FILE_VERSION,
									IMarker.PRIORITY_HIGH, null, null);
							}
						}
					}
				}
				final FlattenedSolution flattenedSolution = ServoyBuilderUtils.getReferenceFlattenedSolution(solution);
				solution.acceptVisitor(new IPersistVisitor()
				{
					private final Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
					private final Set<UUID> methodsParsed = new HashSet<UUID>();

					public Object visit(final IPersist o)
					{
						if (o instanceof Form)
						{
							ServoyFormBuilder.addFormMarkers(servoyProject, (Form)o, methodsParsed, formsAbstractChecked);
							return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
						}
						checkCancel();

						IPersist context = o.getAncestor(IRepository.FORMS);
						if (context == null)
						{
							context = o.getAncestor(IRepository.TABLENODES);
							if (context == null)
							{
								context = o.getAncestor(IRepository.SOLUTIONS);
							}
						}

						Map<IPersist, Boolean> methodsReferences = new HashMap<IPersist, Boolean>();
						try
						{
							final Map<String, Method> methods = ((EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository())
								.getGettersViaIntrospection(
									o);
							for (ContentSpec.Element element : Utils.iterate(
								((EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository()).getContentSpec().getPropertiesForObjectType(
									o.getTypeID())))
							{
								// Don't set meta data properties.
								if (element.isMetaData() || element.isDeprecated()) continue;

								if (o instanceof AbstractBase && !((AbstractBase)o).hasProperty(element.getName()))
								{
									// property is not defined on object itself, check will be done on super element that defines the property
									continue;
								}

								// Get default property value as an object.
								final int typeId = element.getTypeID();

								if (typeId == IRepository.ELEMENTS)
								{
									final Method method = methods.get(element.getName());
									Object property_value = method.invoke(o, new Object[] { });
									final UUID element_uuid = Utils.getAsUUID(property_value, false);
									if (element_uuid != null)
									{
										final IPersist foundPersist = flattenedSolution
											.searchPersist(element_uuid);
										ServoyBuilderUtils.addNullReferenceMarker(project, o, foundPersist, context, element);
										ServoyBuilderUtils.addNotAccessibleMethodMarkers(project, o, foundPersist, context, element, flattenedSolution);
										ServoyBuilderUtils.addMethodParseErrorMarkers(project, o, foundPersist, context, element, methodsParsed,
											methodsReferences);

										if (foundPersist instanceof Form)
										{
											if (!StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(element.getName()) &&
												!ServoyBuilderUtils.formCanBeInstantiated(((Form)foundPersist),
													ServoyBuilder.getPersistFlattenedSolution(foundPersist, flattenedSolution), formsAbstractChecked))
											{
												ServoyMarker mk = MarkerMessages.PropertyFormCannotBeInstantiated.fill(element.getName());
												addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED,
													IMarker.PRIORITY_LOW, null, o);
											}
										}
									}
								}
							}
						}
						catch (Exception e)
						{
							throw new RuntimeException(e);
						}

						if (((AbstractBase)o).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID) != null)
						{
							checkDuplicateUUID(o, project);
						}
						checkCancel();
						addMissingServer(o, missingServers, goodServers);
						checkCancel();
						if (o instanceof ValueList && (!missingServers.containsKey(((ValueList)o).getServerName())))
						{
							ValueList vl = (ValueList)o;
							addMarkers(project, ServoyValuelistBuilder.checkValuelist(vl, ServoyBuilder.getPersistFlattenedSolution(vl, flattenedSolution),
								ApplicationServerRegistry.get().getServerManager(), false), vl);
						}
						checkCancel();
						if (o instanceof Relation)
						{
							Relation relation = (Relation)o;
							if (!missingServers.containsKey(relation.getPrimaryServerName()) && !missingServers.containsKey(relation.getForeignServerName()))
							{
								ServoyRelationBuilder.checkRelation(relation);
							}
						}
						checkCancel();
						if (o instanceof Media)
						{
							ServoyMediaBuilder.checkMedia((Media)o);
						}
						checkCancel();
						if (o instanceof ISupportName && !(o instanceof Media))
						{
							String name = ((ISupportName)o).getName();
							if (name != null && !"".equals(name) && !IdentDocumentValidator.isJavaIdentifier(name))
							{
								ServoyMarker mk = MarkerMessages.ElementNameInvalidIdentifier.fill(name);
								addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER, IMarker.PRIORITY_LOW, null, o);
							}
						}
						checkCancel();
						if (o instanceof TableNode)
						{
							TableNode node = (TableNode)o;
							if (!missingServers.containsKey(node.getServerName()))
							{
								ITable table = servoyModel.getDataSourceManager().getDataSource(node.getDataSource());
								if (table == null || table.isMarkedAsHiddenInDeveloper())
								{
									Iterator<IPersist> iterator = node.getAllObjects();
									while (iterator.hasNext())
									{
										IPersist persist = iterator.next();
										String what;
										if (persist instanceof AggregateVariable) what = "Aggregation";
										else if (persist instanceof ScriptCalculation) what = "Calculation";
										else what = "Function";
										ServoyMarker mk = null;
										Pair<String, ProblemSeverity> problemSeverity;
										if (table != null)
										{
											mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), what + " ",
												((ISupportName)persist).getName());
											problemSeverity = INVALID_TABLE_REFERENCE;
										}
										else
										{
											mk = MarkerMessages.ItemReferencesInvalidTable.fill(what, ((ISupportName)persist).getName(), node.getTableName());
											problemSeverity = INVALID_TABLE_REFERENCE;
										}
										if (mk != null) addMarker(project, mk.getType(), mk.getText(), -1, problemSeverity,
											table != null ? IMarker.PRIORITY_LOW : IMarker.PRIORITY_NORMAL, null, persist);
									}
								}
							}
						}
						checkCancel();
						if (o instanceof ScriptCalculation)
						{
							ScriptCalculation calc = (ScriptCalculation)o;
							String methodCode = calc.getMethodCode();
							if (methodCode != null)
							{
								String text = methodCode.toLowerCase();
								if (text.contains("forms."))
								{
									String[] s = text.split("forms.");
									for (int i = 0; i < s.length - 1; i++)
									{
										if (s[i] == null || s[i].length() == 0 || Character.isWhitespace(s[i].charAt(s[i].length() - 1)))
										{
											Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, true);
											Path path = new Path(pathPair.getLeft() + pathPair.getRight());
											ServoyMarker mk = MarkerMessages.CalculationFormAccess.fill(calc.getName());
											try
											{
												ITable table = calc.getTable();
												if (table != null)
												{
													mk = MarkerMessages.CalculationInTableFormAccess.fill(calc.getName(), table.getName());
												}
												addMarker(project, mk.getType(), mk.getText(), -1, CALCULATION_FORM_ACCESS, IMarker.PRIORITY_NORMAL,
													path.toString(), calc);
											}
											catch (RepositoryException e)
											{
												Debug.log("table not found for calc: " + calc, e);
											}
											break;
										}
									}

								}
							}

						}
						checkCancel();
						if (o instanceof ScriptMethod)
						{
							ServoyBuilderUtils.addScriptMethodErrorMarkers(project, (ScriptMethod)o);
						}
						checkCancel();
						checkDeprecatedElementUsage(o, project, flattenedSolution);
						checkDeprecatedPropertyUsage(o, project, project);
						ISupportChilds parent = o.getParent();
						if (o.getTypeID() == IRepository.SOLUTIONS && parent != null)
						{
							// solution should have no parent
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (parent == null)
						{
							// only a solution have no parents the rest should have a parent.
							if (o.getTypeID() != IRepository.SOLUTIONS) addBadStructureMarker(o, servoyProject, project);
						}
						else if (parent.getTypeID() == IRepository.SOLUTIONS)
						{

							switch (o.getTypeID())
							{
								case IRepository.MEDIA :
								case IRepository.FORMS :
								case IRepository.RELATIONS :
								case IRepository.TABLENODES :
								case IRepository.VALUELISTS :
								case IRepository.SCRIPTVARIABLES :
								case IRepository.METHODS :
								case IRepository.MENUS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}

						}
						else if (parent.getTypeID() == IRepository.FORMS)
						{
							switch (o.getTypeID())
							{
								case IRepository.SCRIPTVARIABLES :
								case IRepository.PORTALS :
								case IRepository.METHODS :
								case IRepository.TABPANELS :
								case IRepository.BEANS :
								case IRepository.RECTSHAPES :
								case IRepository.SHAPES :
								case IRepository.GRAPHICALCOMPONENTS :
								case IRepository.PARTS :
								case IRepository.FIELDS :
								case IRepository.LAYOUTCONTAINERS :
								case IRepository.WEBCOMPONENTS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}
						}
						else if (parent.getTypeID() == IRepository.TABLENODES)
						{
							switch (o.getTypeID())
							{
								case IRepository.AGGREGATEVARIABLES :
								case IRepository.SCRIPTCALCULATIONS :
								case IRepository.METHODS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}
						}
						else if (parent.getTypeID() == IRepository.PORTALS)
						{
							switch (o.getTypeID())
							{
								case IRepository.RECTSHAPES :
								case IRepository.SHAPES :
								case IRepository.GRAPHICALCOMPONENTS :
								case IRepository.FIELDS :
									break;
								default :
									addBadStructureMarker(o, servoyProject, project);
							}
						}
						else if (parent.getTypeID() == IRepository.RELATIONS && o.getTypeID() != IRepository.RELATION_ITEMS)
						{
							addBadStructureMarker(o, servoyProject, project);
						}
						else if (parent.getTypeID() == IRepository.TABPANELS && o.getTypeID() != IRepository.TABS)
						{
							addBadStructureMarker(o, servoyProject, project);
						}

						if (!(o instanceof IScriptElement) &&
							!Utils.getAsBoolean(((AbstractBase)o).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID)))
						{
							// remove this property as it takes too much memory
							// debugging engine needs this info for scriptproviders !!
							((AbstractBase)o).setSerializableRuntimeProperty(IScriptProvider.FILENAME, null);
						}
						ServoyBuilderUtils.addMobileReservedWordsVariable(project, o);

						checkCancel();
						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				});
				checkCancel();
				checkI18n(project);
				checkLoginSolution(project);
			}
			else if (servoyModel.shouldBeModuleOfActiveSolution(project.getName()))
			{
				// so we have an actual Servoy project that is not active, but it should be active
				addDeserializeProblemMarkersIfNeeded(servoyProject);
				if (servoyProject.getDeserializeExceptions().size() == 0 && servoyProject.getSolution() == null)
				{
					addDeserializeProblemMarker(servoyProject.getProject(), "Probably some corrupted file(s). Please check solution metadata file.",
						servoyProject.getProject().getName());
					ServoyLog.logError("No solution in a servoy project that has no deserialize problems", null);
				}
			}

			for (Entry<String, IPersist> entry : missingServers.entrySet())
			{
				String missingServer = entry.getKey();
				IPersist persist = entry.getValue();
				ServoyMarker mk = MarkerMessages.ServerNotAccessibleFirstOccurence.fill(project.getName(), missingServer);
				IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, SERVER_NOT_ACCESSIBLE_FIRST_OCCURENCE, IMarker.PRIORITY_HIGH, null,
					persist);
				if (marker != null)

				{
					try
					{
						marker.setAttribute("missingServer", missingServer);
						marker.setAttribute("Uuid", persist.getUUID().toString());
						marker.setAttribute("SolutionName", project.getName());
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
					}
				}

			}
		}
		else
		{
			ServoyLog.logError("Servoy project is null for a eclipse project with correct nature", null);
		}

		createAndRefreshDataSourceCollectorVisitor();
		if (getServoyModel().getDataModelManager() != null)
		{
			getServoyModel().getDataModelManager().addAllMissingDBIFileMarkersForDataSources(dataSourceCollectorVisitor.getDataSources());
		}
	}

	public static int getTranslatedSeverity(String severity, ProblemSeverity problemSeverity)
	{
		if (severity.equals(ProblemSeverity.WARNING.name())) return IMarker.SEVERITY_WARNING;
		else if (severity.equals(ProblemSeverity.ERROR.name())) return IMarker.SEVERITY_ERROR;
		else if (severity.equals(ProblemSeverity.INFO.name())) return IMarker.SEVERITY_INFO;
		// the switch is added for problem cases, such as svn conflicts, invalid content in settings file
		// so the "severity" parameter comes in with an invalid value - in this case we set to the problemSeverity (default key setting)
		else switch (problemSeverity)
		{
			case WARNING :
				return IMarker.SEVERITY_WARNING;
			case ERROR :
				return IMarker.SEVERITY_ERROR;
			case INFO :
				return IMarker.SEVERITY_INFO;
			default :
				return IMarker.SEVERITY_INFO; // should never happen
		}
	}

	public static ServoyProject[] getSolutionModules(ServoyProject project)
	{
		List<ServoyProject> modules = new ArrayList<ServoyProject>();
		addModules(modules, project);
		return modules.toArray(new ServoyProject[] { });
	}

	private static void addModules(List<ServoyProject> modules, ServoyProject servoyProject)
	{
		String modulesNames = null;
		if (servoyProject.getSolution() != null) modulesNames = servoyProject.getSolution().getModulesNames();
		if (modulesNames != null && !"".equals(modulesNames))
		{
			StringTokenizer st = new StringTokenizer(modulesNames, ";,");
			while (st.hasMoreTokens())
			{
				String name = st.nextToken().trim();
				ServoyProject module = ServoyModelFinder.getServoyModel().getServoyProject(name);
				if (module != null && !modules.contains(module))
				{
					modules.add(module);
					addModules(modules, module);
				}
			}
		}
	}

	private void addDeserializeProblemMarkersIfNeeded(ServoyProject servoyProject)
	{
		HashMap<File, String> deserializeExceptionMessages = servoyProject.getDeserializeExceptions();
		for (Map.Entry<File, String> entry : deserializeExceptionMessages.entrySet())
		{
			IResource file = getEclipseResourceFromJavaIO(entry.getKey(), servoyProject.getProject());
			if (file == null) file = servoyProject.getProject();
			addDeserializeProblemMarker(file, entry.getValue(), servoyProject.getProject().getName());
		}
	}

	private void checkServers(IProject project)
	{
		ServoyProject activeProject = getServoyModel().getActiveProject();
		if (activeProject != null && activeProject.getProject().getName().equals(project.getName()))
		{
			String[] array = ApplicationServerRegistry.get().getServerManager().getServerNames(true, false, false, true);
			for (String server_name : array)
			{
				IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(server_name, true, false);
				boolean existing = false;
				for (String name : ApplicationServerRegistry.get().getServerManager().getAllDriverClassNames())
				{
					if (server != null && name.equals(server.getConfig().getDriver()))
					{
						existing = true;
						break;
					}
				}
				if (!existing)
				{
					ServoyMarker mk = MarkerMessages.MissingDriver.fill(server_name, server.getConfig().getDriver());
					IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, SERVER_MISSING_DRIVER, IMarker.PRIORITY_NORMAL, null, null);
					if (marker != null)
					{
						try
						{
							marker.setAttribute("serverName", server_name);
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}

				if (ApplicationServerRegistry.get().getServerManager().isServerDataModelCloneCycling(server))
				{
					ServoyMarker mk = MarkerMessages.ServerCloneCycle.fill(server_name);
					addMarker(project, mk.getType(), mk.getText(), -1, SERVER_CLONE_CYCLE, IMarker.PRIORITY_NORMAL, null, null);
				}
			}
		}
	}

	private static DataSourceCollectorVisitor dataSourceCollectorVisitor;

	private static void createAndRefreshDataSourceCollectorVisitor()
	{
		dataSourceCollectorVisitor = new DataSourceCollectorVisitor();
		for (ServoyProject sp : ServoyModelFinder.getServoyModel().getModulesOfActiveProject())
		{
			sp.getSolution().acceptVisitor(dataSourceCollectorVisitor);
		}
	}

	public static DataSourceCollectorVisitor getDataSourceCollectorVisitor()
	{
		if (dataSourceCollectorVisitor == null)
		{
			createAndRefreshDataSourceCollectorVisitor();
		}
		return dataSourceCollectorVisitor;
	}

	public void refreshDBIMarkers()
	{
		// do not delete or add dbi marker here
		createAndRefreshDataSourceCollectorVisitor();
		ServoyResourcesProject resourcesProject = getServoyModel().getActiveResourcesProject();
		if (resourcesProject != null && resourcesProject.getProject() != null)
		{
			try
			{
				IMarker[] markers = resourcesProject.getProject().findMarkers(DATABASE_INFORMATION_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				if (markers != null && markers.length > 0)
				{
					for (IMarker marker : markers)
					{
						String serverName = marker.getAttribute(TableDifference.ATTRIBUTE_SERVERNAME, null);
						String tableName = marker.getAttribute(TableDifference.ATTRIBUTE_TABLENAME, null);
						if (serverName != null && tableName != null)
						{
							String datasource = DataSourceUtils.createDBTableDataSource(serverName, tableName);
							if (!dataSourceCollectorVisitor.getDataSources().contains(datasource))
							{
								int markerSeverity = marker.getAttribute(IMarker.SEVERITY, IMarker.SEVERITY_ERROR);
								if (markerSeverity > IMarker.SEVERITY_WARNING)
								{
									marker.setAttribute(IMarker.SEVERITY, IMarker.SEVERITY_WARNING);
								}
							}
							else
							{
								String columnName = marker.getAttribute(TableDifference.ATTRIBUTE_COLUMNNAME, null);
								if (getServoyModel().getDataModelManager() != null)
								{
									TableDifference tableDifference = getServoyModel().getDataModelManager().getColumnDifference(serverName, tableName,
										columnName);
									if (tableDifference != null)
									{
										int severity = tableDifference.getSeverity();
										if (severity >= 0)
										{
											marker.setAttribute(IMarker.SEVERITY, severity);
										}
									}
								}
							}
						}
					}
				}
			}
			catch (Exception e)
			{
				ServoyLog.logError(e);
			}
		}

	}


	private void addDeserializeProblemMarker(IResource resource, String deserializeExceptionMessage, String solutionName)
	{
		ServoyMarker mk;
		int charNo = -1;
		if (deserializeExceptionMessage == null)
		{
			mk = MarkerMessages.SolutionDeserializeError.fill(solutionName, "Errors in file content.");
		}
		else
		{
			mk = MarkerMessages.SolutionDeserializeError.fill(solutionName, deserializeExceptionMessage);
			// find out where the error occurred if possible... this could work for JSON errors
			int idx = deserializeExceptionMessage.indexOf("character");
			if (idx >= 0)
			{
				StringTokenizer st = new StringTokenizer(deserializeExceptionMessage.substring(idx + 9), " ");
				if (st.hasMoreTokens())
				{
					String charNoString = st.nextToken();
					try
					{
						charNo = Integer.parseInt(charNoString);
					}
					catch (NumberFormatException e)
					{
						// cannot find character number... this is not a tragedy
					}
				}
			}
		}
		addMarker(resource, mk.getType(), mk.getText(), charNo, SOLUTION_DESERIALIZE_ERROR, IMarker.PRIORITY_NORMAL, null);
		ServoyLog.logWarning(mk.getText(), null);
	}

	public static IResource getEclipseResourceFromJavaIO(File javaIOFile, IProject project)
	{
		IWorkspace workspace = ResourcesPlugin.getWorkspace();
		IPath location = Path.fromOSString(javaIOFile.getAbsolutePath());
		IResource resource = workspace.getRoot().getFileForLocation(location);
		if (resource == null)
		{
			resource = workspace.getRoot().getContainerForLocation(location);
		}
		return (resource.exists() && resource.getProject() == project) ? resource : null;
	}

	private void addBadStructureMarker(IPersist o, ServoyProject servoyProject, IProject project)
	{
		ServoyMarker mk;
		Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, true);
		String path = ((AbstractBase)o).getSerializableRuntimeProperty(IScriptProvider.FILENAME);
		IResource file = project;
		if (path != null && !"".equals(path))
		{
			file = getEclipseResourceFromJavaIO(new java.io.File(path), project);
			if (file != null) path = file.getProjectRelativePath().toString();
		}
		if (path == null || "".equals(path)) path = pathPair.getRight();
		if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
		{
			mk = MarkerMessages.SolutionBadStructure_EntityManuallyMoved.fill(servoyProject.getSolution().getName(), ((ISupportName)o).getName());
		}
		else
		{
			mk = MarkerMessages.SolutionBadStructure.fill(servoyProject.getSolution().getName());
		}
		addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_BAD_STRUCTURE, IMarker.PRIORITY_LOW, path, o);
	}

	private static ServoyProject getServoyProject(IProject project)
	{
		ServoyProject sp = null;
		try
		{
			sp = (ServoyProject)project.getNature(ServoyProject.NATURE_ID);
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		return sp;
	}

	private void checkI18n(IProject project)
	{
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		Solution solution = servoyProject.getSolution();
		if (solution.getI18nTableName() != null && solution.getI18nServerName() != null)
		{
			// is this table actually hidden in developer? If yes, show a warning. (developer would work even if table is not there based on resources files, but if it was
			// hidden on purpose, it is probably meant as deprecated and we should issue a warning)
			IServerInternal s = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(solution.getI18nServerName());
			if (s != null && s.isValid() && s.getConfig().isEnabled())
			{
				if (s.isTableMarkedAsHiddenInDeveloper(solution.getI18nTableName()))
				{
					ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(
						DataSourceUtils.createDBTableDataSource(solution.getI18nServerName(), solution.getI18nTableName()), "i18n for solution ",
						solution.getName());
					addMarker(project, mk.getType(), mk.getText(), -1, INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW, null, solution);
				}
			}

			ServoyProject[] modules = getSolutionModules(servoyProject);
			if (modules != null)
			{
				for (ServoyProject module : modules)
				{
					Solution mod = module.getSolution();
					if (mod != null && mod.getI18nServerName() != null && mod.getI18nTableName() != null &&
						(!mod.getI18nServerName().equals(solution.getI18nServerName()) || !mod.getI18nTableName().equals(solution.getI18nTableName())))
					{
						ServoyMarker mk = MarkerMessages.ModuleDifferentI18NTable.fill(mod.getName(), solution.getName());
						addMarker(project, mk.getType(), mk.getText(), -1, MODULE_DIFFERENT_I18N_TABLE, IMarker.PRIORITY_NORMAL, null, null);
					}
				}
			}
		}
	}

	private void checkColumns(final IProject project)
	{
		deleteMarkers(project, COLUMN_MARKER_TYPE);
		if (!hasDeletedMarkers)
		{
			deleteMarkers(project, DEPRECATED_SCRIPT_ELEMENT_USAGE); //deprecation markers are also deleted from checkServoyProject method. we need to not delete markers twice.
			deleteMarkers(project, RESERVED_WINDOW_OBJECT_USAGE_TYPE); //reserved words markers are also deleted from checkServoyProject method. we need to not delete markers twice.
		}
		try
		{
			if (project.getReferencedProjects() != null && project.isOpen())
			{
				for (IProject referenced : project.getReferencedProjects())
				{
					if (referenced.exists() && referenced.isOpen() && referenced.hasNature(ServoyResourcesProject.NATURE_ID))
					{
						deleteMarkers(referenced, COLUMN_MARKER_TYPE);
						deleteMarkers(referenced, INVALID_TABLE_NO_PRIMARY_KEY_TYPE);
						if (!hasDeletedMarkers)
						{
							deleteMarkers(referenced, DEPRECATED_SCRIPT_ELEMENT_USAGE);
							deleteMarkers(referenced, RESERVED_WINDOW_OBJECT_USAGE_TYPE);
						}
					}
				}
			}
		}
		catch (Exception ex)
		{
			ServoyLog.logError(ex);
		}
		hasDeletedMarkers = false;
		ScriptNameValidator columnnameValidator = new ScriptNameValidator(getServoyModel().getFlattenedSolution());

		String[] array = ApplicationServerRegistry.get().getServerManager().getServerNames(true, true, false, true);
		for (String serverName : array)
		{
			try
			{
				//get the tables used in current solution
				Set<String> dataSources = getDataSourceCollectorVisitor().getDataSources();
				SortedSet<String> serverNames = DataSourceUtils.getServerNames(dataSources);

				IServerInternal server = (IServerInternal)ApplicationServerRegistry.get().getServerManager().getServer(serverName, true, true);
				if (server != null) // server may have become invalid in the mean time
				{
					List<String> tableNames = server.getTableAndViewNames(true);
					for (String tableName : tableNames)
					{
						if (server.isTableLoaded(tableName) && !server.isTableMarkedAsHiddenInDeveloper(tableName))
						{
							ITable table = server.getTable(tableName);
							IResource res = project;
							if (getServoyModel().getDataModelManager() != null &&
								getServoyModel().getDataModelManager().getDBIFile(serverName, tableName) != null &&
								getServoyModel().getDataModelManager().getDBIFile(serverName, tableName).exists())
							{
								res = getServoyModel().getDataModelManager().getDBIFile(serverName, tableName);
							}
							if (table.isTableInvalidInDeveloperBecauseNoPk())
							{

								ServoyMarker servoyMarker = MarkerMessages.InvalidTableNoPrimaryKey.fill(tableName);
								IMarker marker = addMarker(res, servoyMarker.getType(), servoyMarker.getText(), -1, INVALID_TABLE_NO_PRIMARY_KEY,
									IMarker.PRIORITY_HIGH, null, null);
								try
								{
									marker.setAttribute("serverName", serverName);
									marker.setAttribute("tableName", tableName);
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
							}
							Map<String, Column> columnsByName = new HashMap<String, Column>();
							Map<String, Column> columnsByDataProviderID = new HashMap<String, Column>();
							for (Column column : table.getColumns())
							{
								if (column.getColumnInfo() != null && column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
									!column.getColumnInfo().hasFlag(IBaseColumn.UUID_COLUMN))
								{
									ServoyMarker mk = MarkerMessages.ColumnUUIDFlagNotSet.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_UUID_FLAG_NOT_SET, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
								}
								// check type defined by column converter
								int dataProviderType = getDataType(res, column, null, null);
								if ((column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
									(dataProviderType != IColumnTypes.TEXT && dataProviderType != IColumnTypes.MEDIA)) ||
									(column.getSequenceType() == ColumnInfo.SERVOY_SEQUENCE &&
										(dataProviderType != IColumnTypes.INTEGER && dataProviderType != IColumnTypes.NUMBER)))
								{
									ServoyMarker mk = MarkerMessages.ColumnIncompatibleTypeForSequence.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_INCOMPATIBLE_TYPE_FOR_SEQUENCE, IMarker.PRIORITY_NORMAL, null,
										null).setAttribute("columnName", column.getName());
								}
								if (table.getTableType() != ITable.VIEW && column.getAllowNull() && column.getRowIdentType() != IBaseColumn.NORMAL_COLUMN)
								{
									ServoyMarker mk = MarkerMessages.ColumnRowIdentShouldNotAllowNull.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, ROW_IDENT_SHOULD_NOT_BE_NULL, IMarker.PRIORITY_NORMAL, null,
										null).setAttribute("columnName", column.getName());
								}
								if (column.hasFlag(IBaseColumn.UUID_COLUMN))
								{
									int length = columnHasConvertedType(column) ? 0 : column.getConfiguredColumnType().getLength();
									boolean compatibleForUUID = false;
									switch (dataProviderType)
									{
										case IColumnTypes.MEDIA :
											compatibleForUUID = length == 0 || length >= 16;
											break;
										case IColumnTypes.TEXT :
											if (column.hasFlag(IBaseColumn.NATIVE_COLUMN))
											{
												compatibleForUUID = length == 0 || length >= 16;
												break;
											}
											compatibleForUUID = length == 0 || length >= 36;
											break;
									}
									if (!compatibleForUUID)
									{
										ServoyMarker mk = MarkerMessages.ColumnIncompatbleWithUUID.fill(tableName, column.getName());
										addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_INCOMPATIBLE_WITH_UUID, IMarker.PRIORITY_NORMAL, null,
											null).setAttribute("columnName", column.getName());
									}
								}
								if (column.isDBIdentity() && !column.isDatabasePK() && column.getRowIdentType() == 0)
								{
									ServoyMarker mk = MarkerMessages.ColumnDatabaseIdentityProblem.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_DATABASE_IDENTITY_PROBLEM, IMarker.PRIORITY_NORMAL, null,
										null).setAttribute("columnName", column.getName());
								}
								if (column.getColumnInfo() != null && column.getColumnInfo().getForeignType() != null &&
									!tableNames.contains(column.getColumnInfo().getForeignType()))
								{
									ServoyMarker mk = MarkerMessages.ColumnForeignTypeProblem.fill(tableName, column.getName(),
										column.getColumnInfo().getForeignType());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_FOREIGN_TYPE_PROBLEM, IMarker.PRIORITY_NORMAL, null,
										null).setAttribute("columnName", column.getName());
								}
								if (column.getColumnInfo() != null)
								{
									if (column.getColumnInfo().hasFlag(IBaseColumn.TENANT_COLUMN))
									{
										for (Column col : table.getColumns())
										{
											if (col != column && col.getColumnInfo() != null && col.getColumnInfo().hasFlag(IBaseColumn.TENANT_COLUMN))
											{
												ServoyMarker mk = MarkerMessages.ColumnMultipleTenantTypeProblem.fill(tableName, column.getName());
												addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_MULTIPLE_TENANT_PROBLEM, IMarker.PRIORITY_NORMAL, null,
													null).setAttribute("columnName", column.getName());
												break;
											}
										}
									}
									if (column.getColumnInfo().getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
									{
										String lookup = column.getColumnInfo().getLookupValue();
										if (lookup != null && !"".equals(lookup))
										{
											boolean invalid = false;
											if (ScopesUtils.isVariableScope(lookup))
											{
												IDataProvider globalDataProvider = getServoyModel().getFlattenedSolution().getGlobalDataProvider(lookup, true);
												ScriptMethod scriptMethod = getServoyModel().getFlattenedSolution().getScriptMethod(null, lookup);
												if (globalDataProvider == null && scriptMethod == null)
												{
													invalid = true;
												}
												else
												{
													if (globalDataProvider != null && globalDataProvider instanceof ScriptVariable)
													{
														if (((ScriptVariable)globalDataProvider).isDeprecated())
														{
															ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedVariable.fill(
																((ScriptVariable)globalDataProvider).getName(), "table " + tableName, "Lookup value");
															addMarker(res, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
																IMarker.PRIORITY_NORMAL, null, null).setAttribute("columnName", column.getName());
														}
													}
													else if (scriptMethod != null && scriptMethod.isDeprecated())
													{
														ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(
															scriptMethod.getDisplayName() + "()", "table " + tableName, "Lookup value");
														addMarker(res, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
															IMarker.PRIORITY_NORMAL, null, null).setAttribute("columnName", column.getName());
													}
												}
											}
											else
											{
												ITable lookupTable = table;
												int indx = lookup.lastIndexOf('.');
												if (indx > 0)
												{
													String rel_name = lookup.substring(0, indx);
													Relation[] relations = getServoyModel().getFlattenedSolution().getRelationSequence(rel_name);
													if (relations == null)
													{
														invalid = true;
													}
													else if (relations.length > 0)
													{
														Relation r = relations[relations.length - 1];
														lookupTable = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
															r.getForeignDataSource());
													}
												}
												String col = lookup.substring(indx + 1);
												if (lookupTable != null && lookupTable.getColumn(col) == null)
												{
													invalid = true;
												}
											}
											if (invalid)
											{
												ServoyMarker mk = MarkerMessages.ColumnLookupInvalid.fill(tableName, column.getName());
												addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_LOOKUP_INVALID, IMarker.PRIORITY_NORMAL, null,
													null).setAttribute("columnName", column.getName());
											}
										}
									}

									if (column.getColumnInfo().getValidatorName() != null)
									{
										IServiceProvider serviceProvider = ServoyModelFinder.getServiceProvider();
										if (serviceProvider != null)
										{
											IColumnValidator validator = serviceProvider.getFoundSetManager().getColumnValidatorManager().getValidator(
												column.getColumnInfo().getValidatorName());
											if (validator instanceof IPropertyDescriptorProvider)
											{
												for (String key : validator.getDefaultProperties().keySet())
												{
													IPropertyDescriptor propertyDescriptor = ((IPropertyDescriptorProvider)validator).getPropertyDescriptor(
														key);
													if (propertyDescriptor != null && propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
													{
														ScriptMethod scriptMethod = null;
														Map<String, String> parsedValidatorProperties = ComponentFactory.parseJSonProperties(
															column.getColumnInfo().getValidatorProperties());
														if (parsedValidatorProperties != null)
														{
															String methodName = parsedValidatorProperties.get(key);
															scriptMethod = getServoyModel().getFlattenedSolution().getScriptMethod(null, methodName);
														}
														if (scriptMethod == null)
														{
															ServoyMarker mk = MarkerMessages.ColumnValidatorInvalid.fill(tableName, column.getName());
															addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_VALIDATOR_INVALID, IMarker.PRIORITY_NORMAL,
																null, null).setAttribute("columnName", column.getName());
														}
														else if (scriptMethod.isDeprecated())
														{
															ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(
																scriptMethod.getDisplayName() + "()", "table " + tableName, validator.getName());
															addMarker(res, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
																IMarker.PRIORITY_NORMAL, null, null).setAttribute("columnName", column.getName());
														}
													}
												}
											}
										}
									}
									if (column.getColumnInfo().getConverterName() != null)
									{
										IServiceProvider serviceProvider = ServoyModelFinder.getServiceProvider();
										if (serviceProvider != null)
										{
											IColumnConverter converter = serviceProvider.getFoundSetManager().getColumnConverterManager().getConverter(
												column.getColumnInfo().getConverterName());
											if (converter instanceof IPropertyDescriptorProvider)
											{
												for (String key : converter.getDefaultProperties().keySet())
												{
													IPropertyDescriptor propertyDescriptor = ((IPropertyDescriptorProvider)converter).getPropertyDescriptor(
														key);
													if (propertyDescriptor != null && propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
													{
														Map<String, String> parsedConverterProperties = ComponentFactory.parseJSonProperties(
															column.getColumnInfo().getConverterProperties());
														if (parsedConverterProperties != null)
														{
															String methodName = parsedConverterProperties.get(key);
															if (methodName != null)
															{
																ScriptMethod scriptMethod = getServoyModel().getFlattenedSolution().getScriptMethod(null,
																	methodName);
																if (scriptMethod == null)
																{
																	ServoyMarker mk = MarkerMessages.ColumnConverterInvalid.fill(tableName, column.getName());
																	addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_CONVERTER_INVALID,
																		IMarker.PRIORITY_NORMAL, null, null).setAttribute("columnName", column.getName());
																}
																else if (scriptMethod.isDeprecated())
																{
																	ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(
																		scriptMethod.getDisplayName() + "()", "table " + tableName, converter.getName());
																	addMarker(res, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
																		IMarker.PRIORITY_NORMAL, null, null).setAttribute("columnName", column.getName());
																}
															}
														}
													}
												}
											}
										}
									}

									if ((getServoyModel().getServoyProject(project.getName()).getSolution().getSolutionType() == SolutionMetaData.MOBILE ||
										getServoyModel().getServoyProject(
											project.getName()).getSolution().getSolutionType() == SolutionMetaData.MOBILE_MODULE) &&
										serverNames.contains(serverName) &&
										DataSourceUtils.getServerTablenames(dataSources, serverName).contains(tableName) &&
										column.hasBadNaming(columnnameValidator, true))
									{
										ServoyMarker mk = MarkerMessages.ReservedWindowObjectColumn.fill(column.getName());
										addMarker(res, mk.getType(), mk.getText(), -1, RESERVED_WINDOW_OBJECT_COLUMN, IMarker.PRIORITY_NORMAL, null,
											null).setAttribute("columnName", column.getName());
									}
								}
								String columnName = column.getName();
								String columnDataProviderID = column.getDataProviderID();
								if (columnsByName.containsKey(columnName) || columnsByName.containsKey(columnDataProviderID) ||
									columnsByDataProviderID.containsKey(columnName) || columnsByDataProviderID.containsKey(columnDataProviderID))
								{
									Column otherColumn = columnsByName.get(columnName);
									if (otherColumn == null)
									{
										otherColumn = columnsByName.get(columnDataProviderID);
									}
									if (otherColumn == null)
									{
										otherColumn = columnsByDataProviderID.get(columnDataProviderID);
									}
									if (otherColumn == null)
									{
										otherColumn = columnsByDataProviderID.get(columnName);
									}
									ServoyMarker mk = MarkerMessages.ColumnDuplicateNameDPID.fill(tableName, column.getName(), otherColumn.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_DUPLICATE_NAME_DPID, IMarker.PRIORITY_NORMAL, null,
										null).setAttribute("columnName", column.getName());
								}
								columnsByName.put(columnName, column);
								columnsByDataProviderID.put(columnDataProviderID, column);
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
	}

	private void checkLoginSolution(IProject project)
	{
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		if (servoyProject != null)
		{
			boolean isLoginSolution = servoyProject.getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION;
			ServoyProject[] modules = getSolutionModules(servoyProject);
			ServoyProject[] projectWithModules = new ServoyProject[modules.length + 1];
			projectWithModules[0] = servoyProject;
			System.arraycopy(modules, 0, projectWithModules, 1, modules.length);

			for (ServoyProject sp : projectWithModules)
			{
				final IProject prj = sp.getProject();
				deleteMarkers(prj, FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION);
				if (isLoginSolution)
				{
					sp.getSolution().acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o.getTypeID() == IRepository.FORMS)
							{
								Form form = (Form)o;
								if (((Form)o).getDataSource() != null) // login solution cannot have forms with datasource
								{
									String message = "Form '" + form.getName() +
										"' is part of a login solution and it must not have the datasource property set; its current datasource is : '" +
										form.getDataSource() + "'";
									IMarker marker = addMarker(ServoyBuilderUtils.getPersistResource(form), FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, message, -1,
										LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, IMarker.PRIORITY_HIGH, null, form);
									if (marker != null)
									{
										try
										{
											marker.setAttribute("Uuid", o.getUUID().toString());
											marker.setAttribute("SolutionName", form.getSolution().getName());
											marker.setAttribute("PropertyName", "dataSource");
											marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("dataSource", o.getClass()));
										}
										catch (CoreException ex)
										{
											ServoyLog.logError(ex);
										}
									}
								}
								return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}

					});
				}
			}
		}
	}

	public static List<Problem> checkSortOptions(ITable table, String sortOptions, IPersist persist, FlattenedSolution flattenedSolution)
	{
		if (persist == null || sortOptions == null) return null;
		List<Problem> problems = new ArrayList<Problem>();

		String elementName = null;
		if (persist instanceof Form)
		{
			elementName = "Form";
		}
		else if (persist instanceof Relation)
		{
			elementName = "Relation";
		}
		else if (persist instanceof ValueList)
		{
			elementName = "Valuelist";
		}
		else
		{
			elementName = "Element";
		}
		String name = null;
		if (persist instanceof ISupportName) name = ((ISupportName)persist).getName();
		StringTokenizer tk = new StringTokenizer(sortOptions, ",");
		while (tk.hasMoreTokens())
		{
			String columnName = null;
			String def = tk.nextToken().trim();
			int index = def.indexOf(" ");
			if (index != -1)
			{
				columnName = def.substring(0, index);
			}
			else
			{
				columnName = def;
			}
			try
			{
				ITable lastTable = table;
				String[] split = columnName.split("\\.");
				IDataSourceManager dsm = ServoyModelFinder.getServoyModel().getDataSourceManager();
				for (int i = 0; i < split.length - 1; i++)
				{
					Relation relation = flattenedSolution.getRelation(split[i]);
					if (relation == null)
					{
						String customSeverity = getSeverity(INVALID_SORT_OPTIONS_RELATION_NOT_FOUND.getLeft(),
							INVALID_SORT_OPTIONS_RELATION_NOT_FOUND.getRight().name(), persist);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.InvalidSortOptionsRelationNotFound.fill(elementName, name, sortOptions, split[i]);
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, INVALID_SORT_OPTIONS_RELATION_NOT_FOUND.getRight()),
								mk.getText()));
						}
						lastTable = null;
						break;
					}
					else
					{
						if (!lastTable.equals(dsm.getDataSource(relation.getPrimaryDataSource())))
						{
							String customSeverity = getSeverity(INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE.getLeft(),
								INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE.getRight().name(), persist);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.InvalidSortOptionsRelationDifferentPrimaryDatasource.fill(elementName, name, sortOptions,
									relation.getName());
								problems.add(new Problem(mk.getType(),
									getTranslatedSeverity(customSeverity, INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE.getRight()),
									mk.getText()));
							}
						}
						lastTable = dsm.getDataSource(relation.getForeignDataSource());
					}
				}
				if (lastTable != null)
				{
					String colName = split[split.length - 1];
					Column c = lastTable.getColumn(colName);
					if (c == null || (c.getColumnInfo() != null && c.getColumnInfo().isExcluded()))
					{
						String customSeverity = getSeverity(INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND.getLeft(),
							INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND.getRight().name(), persist);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.InvalidSortOptionsColumnNotFound.fill(elementName, name, sortOptions, colName);
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND.getRight()),
								mk.getText()));
						}
					}
				}
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
		}
		return problems;
	}

	private void checkResourcesForServoyProject(IProject project)
	{
		deleteMarkers(project, MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE);
		deleteMarkers(project, MISSING_PROJECT_REFERENCE);
		try
		{
			// check if this project references more than one or no resources projects
			final IProject[] referencedProjects = project.getDescription().getReferencedProjects();
			int count = 0;
			for (IProject p : referencedProjects)
			{
				if (p.exists() && p.isOpen() && p.hasNature(ServoyResourcesProject.NATURE_ID))
				{
					count++;
				}
				if (!p.isAccessible())
				{
					ServoyMarker mk = MarkerMessages.MissingProjectReference.fill(p.getName(), project.getName());
					final IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, ERROR_MISSING_PROJECT_REFERENCE, IMarker.PRIORITY_NORMAL, null,
						null);
					if (marker != null)
					{
						try
						{
							marker.setAttribute(PROJECT_REFERENCE_NAME, p.getName());
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}
			}

			if (count > 1)
			{
				// > 1 => multiple referenced resources projects; error; quick fix would be choose one of them
				ServoyMarker mk = MarkerMessages.ReferencesToMultipleResources.fill(project.getName());
				addMarker(project, mk.getType(), mk.getText(), -1, REFERENCES_TO_MULTIPLE_RESOURCES, IMarker.PRIORITY_NORMAL, null, null);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while reading referenced projects for " + project.getName(), e);
		}
	}

	private void checkDataSources(IProject project)
	{
		ServoyProject activeProject = getServoyModel().getActiveProject();
		if (activeProject != null && activeProject.getProject().getName().equals(project.getName()) && getServoyModel().getDataModelManager() != null)
		{
			final DataModelManager dm = getServoyModel().getDataModelManager();
			ServoyProject[] modules = ServoyModelFinder.getServoyModel().getModulesOfActiveProject();
			final Map<String, MemTable> memTables = new HashMap<>();

			IPersistVisitor visitor = new IPersistVisitor()
			{

				@Override
				public Object visit(IPersist o)
				{
					if (o instanceof TableNode)
					{
						TableNode node = (TableNode)o;
						if (DataSourceUtils.getDBServernameTablename(node.getDataSource()) != null)
						{

							IFile f = dm.getDBIFile(node.getDataSource());
							if (f == null || !f.exists())
							{
								String parentName = ((Solution)node.getParent()).getSolutionMetaData().getName();
								ServoyMarker mk = MarkerMessages.LingeringTableFiles.fill(node.getTableName());
								IMarker marker = addMarker(getServoyModel().getServoyProject(parentName).getProject(), mk.getType(), mk.getText(), -1,
									LINGERING_TABLE_FILES, IMarker.PRIORITY_NORMAL, null, node);
								try
								{
									marker.setAttribute("Uuid", node.getUUID().toString());
									marker.setAttribute("SolutionName", parentName);
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
							}
						}
						else if (node.getDataSource().startsWith(DataSourceUtils.INMEM_DATASOURCE_SCHEME_COLON))
						{
							try
							{
								String solutionName = ((Solution)node.getParent()).getName();
								ServoyProject servoyProject = ServoyModelFinder.getServoyModel().getServoyProject(solutionName);
								MemServer memServer = servoyProject.getMemServer();
								MemTable table = memServer.getTable(node.getTableName());
								if (memTables.containsKey(node.getTableName()) && memTables.get(node.getTableName()).getParent() != memServer)
								{
									if (memServer.hasTable(node.getTableName()))
									{
										ServoyMarker mk = MarkerMessages.DuplicateMemTable.fill(node.getTableName(),
											memTables.get(node.getTableName()).getParent().getServoyProject().getSolution().getName(), solutionName);
										IMarker marker = addMarker(servoyProject.getProject(), mk.getType(), mk.getText(), -1, DUPLICATE_MEM_TABLE,
											IMarker.PRIORITY_NORMAL, null, node);
										try
										{
											marker.setAttribute("Uuid", node.getUUID().toString());
											marker.setAttribute("SolutionName", solutionName);
										}
										catch (CoreException e)
										{
											ServoyLog.logError(e);
										}
										return CONTINUE_TRAVERSAL;
									}
								}
								if (table != null)
								{
									memTables.put(node.getTableName(), table);
								}
								else
								{
									Debug.log("Could not find mem table for datasource '" + node.getDataSource() + "', solution " + solutionName);
								}
							}
							catch (Exception e)
							{
								Debug.error(e);
							}

						}
					}
					if (o instanceof Form)
					{
						return CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
					}
					return CONTINUE_TRAVERSAL;
				}
			};
			for (ServoyProject module : modules)
			{
				module.getEditingSolution().acceptVisitor(visitor);
			}
		}
	}

	void checkXML(IFile file)
	{
		if (servoyModel.isSolutionActive(file.getProject().getName()))
		{
			deleteMarkers(file, XML_MARKER_TYPE);
			XMLErrorHandler reporter = new XMLErrorHandler(file);
			try
			{
				getParser().parse(file.getContents(true), reporter);
			}
			catch (Exception e)
			{
			}
		}
	}

	public static void addMarkers(IResource resource, List<Problem> problems, IPersist persist)
	{
		if (problems != null)
		{
			for (Problem problem : problems)
			{
				addMarker(resource, problem.type, problem.message, -1, problem.severity, problem.priority, null, persist);
			}
		}
	}

	public static IMarker addMarker(IResource resource, String type, String message, int lineNumber, Pair<String, ProblemSeverity> problemSeverity,
		int priority, String location, IPersist persist)
	{
		if (problemSeverity == null) return null;
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject == null) return null;
		String customSeverity = persist != null ? getSeverity(problemSeverity.getLeft(), problemSeverity.getRight().name(), persist)
			: getSeverity(problemSeverity.getLeft(), problemSeverity.getRight().name(), activeProject.getProject());
		if (customSeverity.equals(ProblemSeverity.IGNORE.name())) return null;
		int severity = getTranslatedSeverity(customSeverity, problemSeverity.getRight());

		return addMarker(resource, type, message, lineNumber, severity, priority, location, persist);
	}

	public static IMarker addMarker(IResource resource, String type, String message, int lineNumber, int severity, int priority, String location,
		IPersist persist)
	{
		try
		{
			IMarker marker = null;
			String elementName = null;
			if (persist != null)
			{
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (persist instanceof ISupportFormElement)
				{
					Form parent = persist.getAncestor(Form.class);
					if (parent != null && parent.isFormComponent())
					{
						String name = ((ISupportFormElement)persist).getName();
						if (name != null)
						{
							String[] nameParts = name.split("\\$");
							if (nameParts.length == 3)
							{
								// look for real form to add marker on it
								IPersist form = ServoyModelFinder.getServoyModel().getFlattenedSolution().searchPersist(nameParts[0]);
								if (form != null)
								{
									elementName = name;
									pathPair = SolutionSerializer.getFilePath(form, true);
									path = new Path(pathPair.getLeft() + pathPair.getRight());
									file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
								}
							}
						}
					}

				}
				if (file.exists())
				{
					marker = file.createMarker(type);
				}
				else if (persist.getParent() instanceof WebComponent)
				{
					pathPair = SolutionSerializer.getFilePath(persist.getParent(), true);
					path = new Path(pathPair.getLeft() + pathPair.getRight());
					file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
					marker = file.exists() ? file.createMarker(type) : resource.createMarker(type);
				}
				else
				{
					marker = resource.createMarker(type);
				}
				if (location == null)
				{
					marker.setAttribute(IMarker.LOCATION, path.toString());
				}
			}
			else
			{
				marker = resource.createMarker(type);
			}
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.PRIORITY, priority);
			int lineNmbr = lineNumber;
			if (lineNmbr == -1 && persist instanceof IScriptElement)
			{
				lineNmbr = ((IScriptElement)persist).getLineNumberOffset();
			}
			if (lineNmbr > 0)
			{
				marker.setAttribute(IMarker.LINE_NUMBER, lineNmbr);
				if (location == null)
				{
					marker.setAttribute(IMarker.LOCATION, "Line " + lineNmbr);
				}
			}
			if (location != null)
			{
				marker.setAttribute(IMarker.LOCATION, location);
			}

			if (persist != null || type.equals(MISSING_DRIVER))
			{
				addExtensionMarkerAttributes(marker, persist);
			}

			if (type.equals(INVALID_TABLE_NODE_PROBLEM) || type.equals(ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString());
				marker.setAttribute("Name", type.equals(ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE) ? "element" : ((ISupportName)persist).getName());
				marker.setAttribute("SolutionName", resource.getProject().getName());
			}
			else if (type.equals(DUPLICATE_UUID) || type.equals(DUPLICATE_SIBLING_UUID) || type.equals(BAD_STRUCTURE_MARKER_TYPE) ||
				type.equals(INVALID_SORT_OPTION) || type.equals(EVENT_METHOD_MARKER_TYPE) || type.equals(PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE) ||
				type.equals(INVALID_EVENT_METHOD) || type.equals(MISSING_STYLE) || type.equals(INVALID_COMMAND_METHOD) || type.equals(INVALID_DATAPROVIDERID) ||
				type.equals(OBSOLETE_ELEMENT) || type.equals(HIDDEN_TABLE_STILL_IN_USE) || type.equals(LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE) ||
				type.equals(FORM_DUPLICATE_PART_MARKER_TYPE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString());
				marker.setAttribute("SolutionName", resource.getProject().getName());
				if (elementName != null) marker.setAttribute("Name", elementName);
				if (type.equals(INVALID_DATAPROVIDERID) && persist instanceof ISupportDataProviderID)
				{
					marker.setAttribute("DataProviderID", ((ISupportDataProviderID)persist).getDataProviderID());
				}
			}
			else if (type.equals(DUPLICATE_NAME_MARKER_TYPE) || type.equals(DUPLICATE_REFERENCED_FORM_MARKER_TYPE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString());
				marker.setAttribute("SolutionName", persist.getRootObject().getName());
			}
			else if (type.equals(PROJECT_FORM_MARKER_TYPE))
			{
				marker.setAttribute("SolutionName", persist.getRootObject().getName());
			}

			return marker;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot create problem marker", e);
		}
		return null;
	}

	// Extensions that want to add marker attributes based on persists will do that here (for example preferred editor to open).
	private static void addExtensionMarkerAttributes(IMarker marker, IPersist persist)
	{
		if (markerContributors == null)
		{
			List<IMarkerAttributeContributor> contributors = ModelUtils.getExtensions(IMarkerAttributeContributor.EXTENSION_ID);
			markerContributors = contributors.toArray(new IMarkerAttributeContributor[contributors.size()]);
		}

		for (IMarkerAttributeContributor markerContributor : markerContributors)
		{
			markerContributor.contributeToMarker(marker, persist);
		}
	}

	public static IMarker addMarker(IResource resource, String type, String message, int charNumber, Pair<String, ProblemSeverity> problemSeverity,
		int priority, String location)
	{
		ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
		if (activeProject == null) return null;
		String customSeverity = getSeverity(problemSeverity.getLeft(), problemSeverity.getRight().name(), activeProject.getProject());
		if (customSeverity.equals(ProblemSeverity.IGNORE.name())) return null;
		int severity = getTranslatedSeverity(customSeverity, problemSeverity.getRight());

		return addMarker(resource, type, message, charNumber, severity, priority, location);
	}

	public static IMarker addMarker(IResource resource, String type, String message, int charNumber, int severity, int priority, String location)
	{
		try
		{
			IMarker marker = resource.createMarker(type);
			marker.setAttribute(IMarker.MESSAGE, message);
			marker.setAttribute(IMarker.SEVERITY, severity);
			marker.setAttribute(IMarker.PRIORITY, priority);
			if (charNumber > 0)
			{
				marker.setAttribute(IMarker.CHAR_START, charNumber);
				marker.setAttribute(IMarker.CHAR_END, charNumber);
				if (location == null)
				{
					marker.setAttribute(IMarker.LOCATION, "Character " + charNumber);
				}
			}
			if (location != null)
			{
				marker.setAttribute(IMarker.LOCATION, location);
			}
			return marker;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot create problem marker", e);
		}
		return null;
	}

	public static void deleteMarkers(IResource file, String type)
	{
		try
		{
			if (file.getProject().isOpen() && file.exists()) file.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e);
		}
	}

	public static void deleteAllMarkers(IResource file)
	{
		try
		{
			file.deleteMarkers(IMarker.PROBLEM, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e);
		}
	}

	public static void deleteAllBuilderMarkers()
	{
		try
		{
			ResourcesPlugin.getWorkspace().getRoot().deleteMarkers(SERVOY_BUILDER_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e);
		}
	}

	private SAXParser getParser() throws ParserConfigurationException, SAXException
	{
		if (parserFactory == null)
		{
			parserFactory = SAXParserFactory.newInstance();
		}
		return parserFactory.newSAXParser();
	}

	protected void fullBuild(IProject project, final IProgressMonitor progressMonitor)
	{
		try
		{
			this.monitor = progressMonitor;
			project.accept(new ServoyResourceVisitor());
			this.monitor = null;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Full Servoy build failed", e);
		}
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor progressMonitor) throws CoreException
	{
		// the visitor does the work.
		this.monitor = progressMonitor;

		ServoyDeltaVisitor visitor = new ServoyDeltaVisitor();
		delta.accept(visitor);
		if (!ServoyBuilderUtils.checkIncrementalBuild(visitor.resources))
		{
			for (IResource resource : visitor.resources)
			{
				checkResource(resource);
			}
		}
		this.monitor = null;
	}

	protected void checkCancel()
	{
		if (monitor != null && monitor.isCanceled())
		{
			forgetLastBuiltState();
			throw new OperationCanceledException();
		}
	}

	/**
	 * @return the servoyModel
	 */
	private IServoyModel getServoyModel()
	{
		if (servoyModel == null)
		{
			servoyModel = ServoyModelFinder.getServoyModel();
		}
		return servoyModel;
	}

	/**
	 * @param dataProvider
	 * @param parsedFormat
	 * @return
	 * @throws IOException
	 */
	public static int getDataType(IResource resource, IDataProvider dataProvider, ParsedFormat parsedFormat, IPersist persist) throws IOException
	{
		int dataType = dataProvider.getDataProviderType();
		IServiceProvider serviceProvider = ServoyModelFinder.getServiceProvider();
		if (serviceProvider == null) return dataType;
		String uiConverterName = parsedFormat != null ? parsedFormat.getUIConverterName() : null;
		if (uiConverterName != null)
		{
			IUIConverter converter = serviceProvider.getFoundSetManager().getUIConverterManager().getConverter(uiConverterName);
			if (converter != null)
			{
				int convType = converter.getToObjectType(parsedFormat.getUIConverterProperties());
				if (convType != Integer.MAX_VALUE)
				{
					dataType = Column.mapToDefaultType(convType);
				}

				// check global method ui converters
				if (converter instanceof IPropertyDescriptorProvider)
				{
					for (String key : converter.getDefaultProperties().keySet())
					{
						IPropertyDescriptor propertyDescriptor = ((IPropertyDescriptorProvider)converter).getPropertyDescriptor(key);
						if (propertyDescriptor != null && propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
						{
							String methodName = parsedFormat.getUIConverterProperties().get(key);
							ScriptMethod scriptMethod = ServoyModelFinder.getServoyModel().getFlattenedSolution().getScriptMethod(null, methodName);
							if (scriptMethod == null)
							{
								Form form = (Form)persist.getAncestor(IRepository.FORMS);
								String inForm = form == null ? "<unknown>" : form.getName();
								String elementName = null;
								if (persist instanceof ISupportName)
								{
									elementName = ((ISupportName)persist).getName();
								}
								ServoyMarker mk;
								if (elementName == null)
								{
									mk = MarkerMessages.UIConverterInvalid.fill(inForm, methodName);
								}
								else
								{
									mk = MarkerMessages.UIConverterOnElementInvalid.fill(elementName, inForm, methodName);
								}

								addMarker(resource, mk.getType(), mk.getText(), -1, DATAPROVIDER_MISSING_CONVERTER, IMarker.PRIORITY_HIGH, null, persist);
							}
							else if (scriptMethod.isDeprecated())
							{
								Form form = (Form)persist.getAncestor(IRepository.FORMS);
								ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
									"uiconverter on form " + (form == null ? "<unknown>" : form.getName()) + " on " + ((ISupportName)persist).getName(),
									converter.getName());
								addMarker(resource, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM, IMarker.PRIORITY_NORMAL, null,
									null);
							}
						}
					}
				}
			}
			else
			{
				ServoyMarker mk = MarkerMessages.MissingConverter.fill(uiConverterName, dataProvider);
				addMarker(resource, mk.getType(), mk.getText(), -1, DATAPROVIDER_MISSING_CONVERTER, IMarker.PRIORITY_HIGH, null, persist);
			}
		}
		else if (dataProvider instanceof Column && ((Column)dataProvider).getColumnInfo() != null)
		{
			ColumnInfo columnInfo = ((Column)dataProvider).getColumnInfo();
			String converterName = columnInfo.getConverterName();
			if (converterName != null)
			{
				// check type defined by column converter
				IColumnConverter converter = serviceProvider.getFoundSetManager().getColumnConverterManager().getConverter(converterName);
				if (converter instanceof ITypedColumnConverter)
				{
					int convType = ((ITypedColumnConverter)converter).getToObjectType(
						ComponentFactory.<String> parseJSonProperties(columnInfo.getConverterProperties()));
					if (convType != Integer.MAX_VALUE)
					{
						dataType = Column.mapToDefaultType(convType);
					}
				}
				else if (converter == null)
				{
					ServoyMarker mk = MarkerMessages.MissingConverter.fill(converterName, dataProvider);
					addMarker(resource, mk.getType(), mk.getText(), -1, DATAPROVIDER_MISSING_CONVERTER, IMarker.PRIORITY_HIGH, null, persist);
				}
			}
		}
		return dataType;
	}

	private boolean columnHasConvertedType(Column column) throws IOException
	{
		IServiceProvider serviceProvider = ServoyModelFinder.getServiceProvider();
		if (serviceProvider != null)
		{
			ColumnInfo columnInfo = column.getColumnInfo();
			if (columnInfo != null)
			{
				String converterName = columnInfo.getConverterName();
				if (converterName != null)
				{
					// check type defined by column converter
					IColumnConverter converter = serviceProvider.getFoundSetManager().getColumnConverterManager().getConverter(converterName);
					if (converter instanceof ITypedColumnConverter)
					{
						int convType = ((ITypedColumnConverter)converter).getToObjectType(
							ComponentFactory.<String> parseJSonProperties(columnInfo.getConverterProperties()));
						return convType != Integer.MAX_VALUE;
					}
				}
			}
		}
		return false;
	}

	public static FlattenedSolution getPersistFlattenedSolution(IPersist persist, FlattenedSolution fallbackFlattenedSolution)
	{
		FlattenedSolution persistFlattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
		return persistFlattenedSolution != null ? persistFlattenedSolution : fallbackFlattenedSolution;
	}

	public static void addEncapsulationMarker(IResource markerResource, IProject project, IPersist persist, IPersist foundPersist, Form context)
	{
		if (foundPersist instanceof ISupportEncapsulation)
		{
			if (PersistEncapsulation.isModuleScope((ISupportEncapsulation)foundPersist, (Solution)persist.getRootObject()) &&
				!(context.getSolution().equals(foundPersist.getRootObject())))
			{
				ServoyMarker mk = MarkerMessages.NonAccessibleFormInModuleUsedInParentSolutionForm.fill(
					RepositoryHelper.getObjectTypeName(foundPersist.getTypeID()), ((ISupportName)foundPersist).getName(),
					foundPersist.getRootObject().getName(), getServoyProject(project).getSolution().getName(), context.getName());
				addMarker(markerResource, mk.getType(), mk.getText(), -1, NON_ACCESSIBLE_PERSIST_IN_MODULE_USED_IN_PARENT_SOLUTION, IMarker.PRIORITY_LOW, null,
					persist);
			}
		}
	}

	/**
	 * Container class for problem and optional fix.
	 *
	 * @author rgansevles
	 *
	 */
	public static class Problem
	{
		public final String type;
		public final int severity;
		public final String message;
		public final String fix;
		public final int priority;

		public Problem(String type, int severity, int priority, String message, String fix)
		{
			this.type = type;
			this.severity = severity;
			this.priority = priority;
			this.message = message;
			this.fix = fix;
		}

		public Problem(String type, int severity, String message, String fix)
		{
			this(type, severity, IMarker.PRIORITY_NORMAL, message, fix);
		}

		public Problem(String type, int severity, String message)
		{
			this(type, severity, IMarker.PRIORITY_NORMAL, message, null);
		}
	}

}
