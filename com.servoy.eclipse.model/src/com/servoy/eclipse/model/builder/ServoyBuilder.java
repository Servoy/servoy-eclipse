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

import java.awt.Point;
import java.awt.print.PageFormat;
import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.SortedSet;
import java.util.StringTokenizer;

import javax.swing.ImageIcon;
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
import org.eclipse.core.runtime.OperationCanceledException;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.preferences.InstanceScope;
import org.eclipse.dltk.compiler.problem.ProblemSeverity;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.DefaultHandler;

import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.base.util.DataSourceUtilsBase;
import com.servoy.eclipse.model.Activator;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.extensions.IMarkerAttributeContributor;
import com.servoy.eclipse.model.extensions.IServoyModel;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.nature.ServoyResourcesProject;
import com.servoy.eclipse.model.repository.DataModelManager.TableDifference;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.IServiceProvider;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.dataprocessing.DBValueList;
import com.servoy.j2db.dataprocessing.IColumnConverter;
import com.servoy.j2db.dataprocessing.IColumnValidator;
import com.servoy.j2db.dataprocessing.IPropertyDescriptor;
import com.servoy.j2db.dataprocessing.IPropertyDescriptorProvider;
import com.servoy.j2db.dataprocessing.ITypedColumnConverter;
import com.servoy.j2db.dataprocessing.IUIConverter;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AbstractRepository;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnInfo;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.DataSourceCollectorVisitor;
import com.servoy.j2db.persistence.EnumDataProvider;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedPortal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
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
import com.servoy.j2db.persistence.ISupportMedia;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.LiteralDataprovider;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.MethodArgument;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.PersistEncapsulation;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.RelationItem;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.RepositoryHelper;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.persistence.Style;
import com.servoy.j2db.persistence.Tab;
import com.servoy.j2db.persistence.TabPanel;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.server.shared.ApplicationServerSingleton;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.IntHashMap;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;
import com.servoy.j2db.util.keyword.Ident;

/**
 * Builds Servoy projects. Adds problem markers where needed.
 */
public class ServoyBuilder extends IncrementalProjectBuilder
{
	public static int MAX_EXCEPTIONS = 25;
	public static int MIN_FIELD_LENGTH = 1000;
	public static int exceptionCount = 0;

	private static final int LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM = 3;
	private static final int LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM = 20;

	class ServoyDeltaVisitor implements IResourceDeltaVisitor
	{
		public boolean visit(IResourceDelta delta) throws CoreException
		{
			IResource resource = delta.getResource();
			switch (delta.getKind())
			{
				case IResourceDelta.ADDED :
					// handle added resource
					checkResource(resource);
					break;
				case IResourceDelta.REMOVED :
					// handle removed resource
					checkResource(resource);
					break;
				case IResourceDelta.CHANGED :
					// handle changed resource
					checkResource(resource);
					break;
			}
			//return true to continue visiting children.
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

	public static final String BUILDER_ID = "com.servoy.eclipse.core.servoyBuilder"; //$NON-NLS-1$
	private static final String _PREFIX = "com.servoy.eclipse.core"; //$NON-NLS-1$
	public static final String SERVOY_MARKER_TYPE = _PREFIX + ".servoyProblem"; //$NON-NLS-1$
	public static final String SERVOY_BUILDER_MARKER_TYPE = _PREFIX + ".builderProblem"; //$NON-NLS-1$

	/**
	 * If you add a new type, do not forget to add the extension point also!!!
	 */
	public static final String XML_MARKER_TYPE = _PREFIX + ".xmlProblem"; //$NON-NLS-1$
	public static final String PROJECT_DESERIALIZE_MARKER_TYPE = _PREFIX + ".deserializeProblem"; //$NON-NLS-1$
	public static final String SOLUTION_PROBLEM_MARKER_TYPE = _PREFIX + ".solutionProblem"; //$NON-NLS-1$
	public static final String BAD_STRUCTURE_MARKER_TYPE = _PREFIX + ".badStructure"; //$NON-NLS-1$
	public static final String MISSING_MODULES_MARKER_TYPE = _PREFIX + ".missingModulesProblem"; //$NON-NLS-1$
	public static final String MISPLACED_MODULES_MARKER_TYPE = _PREFIX + ".misplacedModulesProblem"; //$NON-NLS-1$
	public static final String MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".multipleResourcesProblem"; //$NON-NLS-1$
	public static final String NO_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".noResourcesProblem"; //$NON-NLS-1$
	public static final String DIFFERENT_RESOURCES_PROJECTS_MARKER_TYPE = _PREFIX + ".differentResourcesProblem"; //$NON-NLS-1$
	public static final String PROJECT_RELATION_MARKER_TYPE = _PREFIX + ".relationProblem"; //$NON-NLS-1$
	public static final String MEDIA_MARKER_TYPE = _PREFIX + ".mediaProblem"; //$NON-NLS-1$
	public static final String CALCULATION_MARKER_TYPE = _PREFIX + ".calculationProblem"; //$NON-NLS-1$
	public static final String SCRIPT_MARKER_TYPE = _PREFIX + ".scriptProblem"; //$NON-NLS-1$
	public static final String EVENT_METHOD_MARKER_TYPE = _PREFIX + ".eventProblem"; //$NON-NLS-1$
	public static final String USER_SECURITY_MARKER_TYPE = _PREFIX + ".userSecurityProblem"; //$NON-NLS-1$
	public static final String DATABASE_INFORMATION_MARKER_TYPE = _PREFIX + ".databaseInformationProblem"; //$NON-NLS-1$
	public static final String PROJECT_FORM_MARKER_TYPE = _PREFIX + ".formProblem"; //$NON-NLS-1$
	public static final String INVALID_TABLE_NODE_PROBLEM = _PREFIX + ".invalidTableNodeProblem"; //$NON-NLS-1$
	public static final String PROJECT_VALUELIST_MARKER_TYPE = _PREFIX + ".valuelistProblem"; //$NON-NLS-1$
	public static final String DUPLICATE_UUID = _PREFIX + ".duplicateUUID"; //$NON-NLS-1$
	public static final String DUPLICATE_SIBLING_UUID = _PREFIX + ".duplicateSiblingUUID"; //$NON-NLS-1$
	public static final String DUPLICATE_NAME_MARKER_TYPE = _PREFIX + ".duplicateNameProblem"; //$NON-NLS-1$
	public static final String RESERVED_WINDOW_OBJECT_USAGE_TYPE = _PREFIX + ".reservedWindowObjectUsageProblem"; //$NON-NLS-1$
	public static final String DUPLICATE_SCOPE_NAME_MARKER_TYPE = _PREFIX + ".duplicateScopeNameProblem"; //$NON-NLS-1$
	public static final String INVALID_SORT_OPTION = _PREFIX + ".invalidSortOption"; //$NON-NLS-1$
	public static final String PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE = _PREFIX + ".differentRelationName"; //$NON-NLS-1$
	public static final String MISSING_SERVER = _PREFIX + ".missingServer"; //$NON-NLS-1$
	public static final String MISSING_STYLE = _PREFIX + ".missingStyle"; //$NON-NLS-1$
	public static final String I18N_MARKER_TYPE = _PREFIX + ".i18nProblem"; //$NON-NLS-1$
	public static final String COLUMN_MARKER_TYPE = _PREFIX + ".columnProblem"; //$NON-NLS-1$
	public static final String INVALID_EVENT_METHOD = _PREFIX + ".invalidEventMethod"; //$NON-NLS-1$
	public static final String INVALID_COMMAND_METHOD = _PREFIX + ".invalidCommandMethod"; //$NON-NLS-1$
	public static final String INVALID_DATAPROVIDERID = _PREFIX + ".invalidDataProviderID"; //$NON-NLS-1$
	public static final String DEPRECATED_PROPERTY_USAGE = _PREFIX + ".deprecatedPropertyUsage"; //$NON-NLS-1$
	public static final String FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION = _PREFIX + ".formWithDatasourceInLoginSolution"; //$NON-NLS-1$
	public static final String MULTIPLE_METHODS_ON_SAME_ELEMENT = _PREFIX + ".multipleMethodsInfo"; //$NON-NLS-1$
	public static final String UNRESOLVED_RELATION_UUID = _PREFIX + ".unresolvedRelationUuid"; //$NON-NLS-1$
	public static final String CONSTANTS_USED_MARKER_TYPE = _PREFIX + ".constantsUsed"; //$NON-NLS-1$
	public static final String MISSING_DRIVER = _PREFIX + ".missingDriver"; //$NON-NLS-1$
	public static final String OBSOLETE_ELEMENT = _PREFIX + ".obsoleteElement"; //$NON-NLS-1$
	public static final String HIDDEN_TABLE_STILL_IN_USE = _PREFIX + ".hiddenTableInUse"; //$NON-NLS-1$
	public static final String MISSING_CONVERTER = _PREFIX + ".missingConverter"; //$NON-NLS-1$
	public static final String LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE = _PREFIX + ".labelForElementProblem"; //$NON-NLS-1$
	public static final String INVALID_MOBILE_MODULE_MARKER_TYPE = _PREFIX + ".invalidMobileModuleProblem"; //$NON-NLS-1$
	public static final String FORM_DUPLICATE_PART_MARKER_TYPE = _PREFIX + ".formDuplicatePart"; //$NON-NLS-1$
	public static final String DEPRECATED_SCRIPT_ELEMENT_USAGE = _PREFIX + ".deprecatedScriptElementUsage"; //$NON-NLS-1$
	public static final String METHOD_NUMBER_OF_ARGUMENTS_MISMATCH_TYPE = _PREFIX + ".methodNumberOfArgsMismatch"; //$NON-NLS-1$
	public static final String SERVER_CLONE_CYCLE_TYPE = _PREFIX + ".serverCloneCycle"; //$NON-NLS-1$
	public static final String DEPRECATED_ELEMENT_USAGE = _PREFIX + ".deprecatedElementUsage"; //$NON-NLS-1$

	// warning/error level settings keys/defaults
	public final static String ERROR_WARNING_PREFERENCES_NODE = Activator.PLUGIN_ID + "/errorWarningLevels"; //$NON-NLS-1$

	// performance related
	public final static Pair<String, ProblemSeverity> LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW = new Pair<String, ProblemSeverity>(
		"performanceTableColumns", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> LEVEL_PERFORMANCE_TABS_PORTALS = new Pair<String, ProblemSeverity>(
		"performanceTabsPortals", ProblemSeverity.WARNING); //$NON-NLS-1$

	// developer problems
	public final static Pair<String, ProblemSeverity> INVALID_TABLE_REFERENCE = new Pair<String, ProblemSeverity>(
		"invalidTableReference", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> METHOD_EVENT_PARAMETERS = new Pair<String, ProblemSeverity>(
		"methodEventParameters", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> MEDIA_TIFF = new Pair<String, ProblemSeverity>("mediaTiff", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> CALCULATION_FORM_ACCESS = new Pair<String, ProblemSeverity>(
		"calculationFormAccess", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> IMAGE_MEDIA_NOT_SET = new Pair<String, ProblemSeverity>("imageMediaNotSet", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> ROLLOVER_NOT_WORKING = new Pair<String, ProblemSeverity>("rolloverNotWorking", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> MOBILE_NAVIGATOR_OVERLAPS_HEADER_BUTTON = new Pair<String, ProblemSeverity>(
		"mobileNavigatorOverlapsHeaderButton", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DATAPROVIDER_MISSING_CONVERTER = new Pair<String, ProblemSeverity>(
		"dataproviderMissingConverter", ProblemSeverity.WARNING); //$NON-NLS-1$

	// problems with resource projects 
	public final static Pair<String, ProblemSeverity> REFERENCES_TO_MULTIPLE_RESOURCES = new Pair<String, ProblemSeverity>(
		"referencesToMultipleResources", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> NO_RESOURCE_REFERENCE = new Pair<String, ProblemSeverity>("noResourceReference", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> PROPERTY_MULTIPLE_METHODS_ON_SAME_TABLE = new Pair<String, ProblemSeverity>(
		"propertyMultipleMethodsOnSameTable", ProblemSeverity.INFO); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SERVER_MISSING_DRIVER = new Pair<String, ProblemSeverity>("severMissingDriver", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SERVER_CLONE_CYCLE = new Pair<String, ProblemSeverity>("serverCloneCycle", ProblemSeverity.WARNING); //$NON-NLS-1$

	// login problems 
	public final static Pair<String, ProblemSeverity> LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION = new Pair<String, ProblemSeverity>(
		"formWithDatasourceInLoginSolution", ProblemSeverity.WARNING); //$NON-NLS-1$

	// deprecated properties usage problems
	public final static Pair<String, ProblemSeverity> DEPRECATED_PROPERTY_USAGE_PROBLEM = new Pair<String, ProblemSeverity>(
		"deprecatedPropertyUsage", ProblemSeverity.WARNING); //$NON-NLS-1$

	// deprecated element usage problems
	public final static Pair<String, ProblemSeverity> DEPRECATED_ELEMENT_USAGE_PROBLEM = new Pair<String, ProblemSeverity>(
		"deprecatedElementUsage", ProblemSeverity.WARNING); //$NON-NLS-1$

	//deprecated script elements usage problems
	public final static Pair<String, ProblemSeverity> DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM = new Pair<String, ProblemSeverity>(
		"deprecatedScriptElementUsage", ProblemSeverity.WARNING); //$NON-NLS-1$

	// duplication problems
	public final static Pair<String, ProblemSeverity> DUPLICATION_UUID_DUPLICATE = new Pair<String, ProblemSeverity>(
		"duplicationUUIDDuplicate", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DUPLICATION_DUPLICATE_ENTITY_FOUND = new Pair<String, ProblemSeverity>(
		"duplicationDuplicateEntityFound", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RESERVED_WINDOW_OBJECT_PROPERTY = new Pair<String, ProblemSeverity>(
		"reservedWindowObjectProperty", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RESERVED_WINDOW_OBJECT_COLUMN = new Pair<String, ProblemSeverity>(
		"reservedWindowObjectColumn", ProblemSeverity.WARNING); //$NON-NLS-1$

	// database information problems
	public final static Pair<String, ProblemSeverity> DBI_BAD_INFO = new Pair<String, ProblemSeverity>("DBIBadDBInfo", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_MISSING_FROM_DB = new Pair<String, ProblemSeverity>(
		"DBIColumnMissingFromDB", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_MISSING_FROM_DB_FILE = new Pair<String, ProblemSeverity>(
		"DBIColumnMissingFromDBIFile", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_CONFLICT = new Pair<String, ProblemSeverity>("DBIColumnConflict", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DBI_TABLE_MISSING = new Pair<String, ProblemSeverity>("DBITableMissing", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DBI_FILE_MISSING = new Pair<String, ProblemSeverity>("DBIFileMissing", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> DBI_COLUMN_INFO_SEQ_TYPE_OVERRIDE = new Pair<String, ProblemSeverity>(
		"DBIColumnSequenceTypeOverride", ProblemSeverity.WARNING); //$NON-NLS-1$

	// column problems
	public final static Pair<String, ProblemSeverity> COLUMN_UUID_FLAG_NOT_SET = new Pair<String, ProblemSeverity>(
		"ColumnUUIDFlagNotSet", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_DATABASE_IDENTITY_PROBLEM = new Pair<String, ProblemSeverity>(
		"ColumnDatabaseIdentityProblem", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_DUPLICATE_NAME_DPID = new Pair<String, ProblemSeverity>(
		"ColumnDuplicateNameDPID", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_FOREIGN_TYPE_PROBLEM = new Pair<String, ProblemSeverity>(
		"ColumnForeignTypeProblem", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_INCOMPATIBLE_TYPE_FOR_SEQUENCE = new Pair<String, ProblemSeverity>(
		"ColumnIncompatibleTypeForSequence", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_INCOMPATIBLE_WITH_UUID = new Pair<String, ProblemSeverity>(
		"ColumnIncompatbleWithUUID", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_LOOKUP_INVALID = new Pair<String, ProblemSeverity>("ColumnLookupInvalid", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_VALIDATOR_INVALID = new Pair<String, ProblemSeverity>(
		"ColumnValidatorInvalid", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> COLUMN_CONVERTER_INVALID = new Pair<String, ProblemSeverity>(
		"ColumnConverterInvalid", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> ROW_IDENT_SHOULD_NOT_BE_NULL = new Pair<String, ProblemSeverity>(
		"ColumnRowIdentShouldNotBeNull", ProblemSeverity.WARNING); //$NON-NLS-1$

	// sort problems
	public final static Pair<String, ProblemSeverity> INVALID_SORT_OPTIONS_RELATION_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"InvalidSortOptionsRelationNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE = new Pair<String, ProblemSeverity>(
		"InvalidSortOptionsRelationDifferentPrimaryDatasource", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> INVALID_SORT_OPTIONS_COLUMN_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"InvalidSortOptionsColumnNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$

	// module problems
	public final static Pair<String, ProblemSeverity> MODULE_NOT_FOUND = new Pair<String, ProblemSeverity>("moduleNotFound", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> MODULE_MISPLACED = new Pair<String, ProblemSeverity>("moduleMisplaced", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> MODULE_DIFFERENT_I18N_TABLE = new Pair<String, ProblemSeverity>(
		"moduleDifferentI18NTable", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> MODULE_DIFFERENT_RESOURCE_PROJECT = new Pair<String, ProblemSeverity>(
		"moduleDifferentResourceProject", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> MODULE_INVALID_MOBILE = new Pair<String, ProblemSeverity>("moduleInvalidMobile", ProblemSeverity.ERROR); //$NON-NLS-1$

	// form problems
	public final static Pair<String, ProblemSeverity> FORM_COLUMN_LENGTH_TOO_SMALL = new Pair<String, ProblemSeverity>(
		"formColumnLengthTooSmall", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE = new Pair<String, ProblemSeverity>(
		"formDataproviderAggregateNotEditable", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_INVALID_DATAPROVIDER = new Pair<String, ProblemSeverity>(
		"formInvalidDataprovider", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_DERIVED_FORM_DIFFERENT_TABLE = new Pair<String, ProblemSeverity>(
		"formDerivedFormDifferentTable", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_DERIVED_FORM_REDEFINED_VARIABLE = new Pair<String, ProblemSeverity>(
		"formDerivedFormRedefinedVariable", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_DUPLICATE_PART = new Pair<String, ProblemSeverity>("formDuplicatePart", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_EDITABLE_COMBOBOX_CUSTOM_VALUELIST = new Pair<String, ProblemSeverity>(
		"formEditableComboboxCustomValuelist", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_EXTENDS_CYCLE = new Pair<String, ProblemSeverity>("formExtendsCycle", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_FILE_NAME_INCONSISTENT = new Pair<String, ProblemSeverity>(
		"formFileNameInconsistent", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_FORMAT_INVALID = new Pair<String, ProblemSeverity>("formFormatInvalid", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_INCOMPATIBLE_ELEMENT_TYPE = new Pair<String, ProblemSeverity>(
		"formIncompatibleElementType", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_LABEL_FOR_ELEMENT_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"formLabelForElementNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_EXTENDS_FORM_ELEMENT_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"formExtendsFormElementNotFound", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_ELEMENT_DUPLICATE_TAB_SEQUENCE = new Pair<String, ProblemSeverity>(
		"formElementDuplicateTabSequence", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_ELEMENT_OUTSIDE_BOUNDS = new Pair<String, ProblemSeverity>(
		"formElementOutsideBounds", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_OBSOLETE_ELEMENT = new Pair<String, ProblemSeverity>("formObsoleteElement", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_REQUIRED_PROPERTY_MISSING = new Pair<String, ProblemSeverity>(
		"formRequiredPropertyMissing", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_FIELD_RELATED_VALUELIST = new Pair<String, ProblemSeverity>(
		"formFieldRelatedValuelist", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_FOUNDSET_INCORRECT_VALUE = new Pair<String, ProblemSeverity>(
		"formFoundsetIncorrectValue", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_PORTAL_INVALID_RELATION_NAME = new Pair<String, ProblemSeverity>(
		"formPortalInvalidRelationName", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_PROPERTY_METHOD_NOT_ACCESIBLE = new Pair<String, ProblemSeverity>(
		"formPropertyMethodNotAccessible", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_TABPANEL_TAB_IMAGE_TOO_LARGE = new Pair<String, ProblemSeverity>(
		"formTabPanelTabImageTooLarge", ProblemSeverity.WARNING); //$NON-NLS-1$	
	public final static Pair<String, ProblemSeverity> FORM_RELATED_TAB_DIFFERENT_TABLE = new Pair<String, ProblemSeverity>(
		"formRelatedTabDifferentTable", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_RELATED_TAB_UNSOLVED_RELATION = new Pair<String, ProblemSeverity>(
		"formRelatedTabUnsolvedRelation", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_RELATED_TAB_UNSOLVED_UUID = new Pair<String, ProblemSeverity>(
		"formRelatedTabUnsolvedUuid", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_PROPERTY_TARGET_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"formPropertyTargetNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_INVALID_TABLE = new Pair<String, ProblemSeverity>("formInvalidTable", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_TYPEAHEAD_UNSTORED_CALCULATION = new Pair<String, ProblemSeverity>(
		"formTypeAheadUnstoredCalculation", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_VARIABLE_TYPE_COL = new Pair<String, ProblemSeverity>(
		"formVariableTableCol", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> FORM_PROPERTY_MULTIPLE_METHODS_ON_SAME_ELEMENT = new Pair<String, ProblemSeverity>(
		"formPropertyMultipleMethodsOnSameElement", ProblemSeverity.INFO); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> NON_ACCESSIBLE_PERSIST_IN_MODULE_USED_IN_PARENT_SOLUTION = new Pair<String, ProblemSeverity>(
		"nonAccessibleFormInModuleUsedInParentSolution", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> METHOD_NUMBER_OF_ARGUMENTS_MISMATCH = new Pair<String, ProblemSeverity>(
		"methodNumberOfArgsMismatch", ProblemSeverity.WARNING); //$NON-NLS-1$

	// relations related
	public final static Pair<String, ProblemSeverity> RELATION_PRIMARY_SERVER_WITH_PROBLEMS = new Pair<String, ProblemSeverity>(
		"relationPrimaryServerWithProblems", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_SERVER_DUPLICATE = new Pair<String, ProblemSeverity>(
		"relationServerDuplicate", ProblemSeverity.WARNING); //$NON-NLS-1$ 
	public final static Pair<String, ProblemSeverity> RELATION_TABLE_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"relationTableNotFound", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_TABLE_WITHOUT_PK = new Pair<String, ProblemSeverity>(
		"relationTableWithoutPK", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_FOREIGN_SERVER_WITH_PROBLEMS = new Pair<String, ProblemSeverity>(
		"relationForeignServerWithProblems", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_EMPTY = new Pair<String, ProblemSeverity>("relationEmpty", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_ITEM_DATAPROVIDER_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"relationItemDataproviderNotFound", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_ITEM_UUID_PROBLEM = new Pair<String, ProblemSeverity>(
		"relationItemUUIDProblem", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_ITEM_TYPE_PROBLEM = new Pair<String, ProblemSeverity>(
		"relationItemTypeProblem", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> RELATION_GENERIC_ERROR = new Pair<String, ProblemSeverity>("relationGenericError", ProblemSeverity.ERROR); //$NON-NLS-1$

	// valuelist problems
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_WITH_CUSTOM_VALUES = new Pair<String, ProblemSeverity>(
		"valuelistDBWithCustomValues", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_NOT_TABLE_OR_RELATION = new Pair<String, ProblemSeverity>(
		"valuelistDBNotTableOrRelation", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_MALFORMED_TABLE_DEFINITION = new Pair<String, ProblemSeverity>(
		"valuelistDBMalformedTableDefinition", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_SERVER_DUPLICATE = new Pair<String, ProblemSeverity>(
		"valuelistDBServerDuplicate", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_DB_TABLE_NO_PK = new Pair<String, ProblemSeverity>(
		"valuelistDBTableNoPk", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_ENTITY_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"valuelistEntityNotFound", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_RELATION_WITH_DATASOURCE = new Pair<String, ProblemSeverity>(
		"valuelistRelationWithDatasource", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_RELATION_SEQUENCE_INCONSISTENT = new Pair<String, ProblemSeverity>(
		"valuelistRelationSequenceInconsistent", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_CUSTOM_VALUES_WITH_DB_INFO = new Pair<String, ProblemSeverity>(
		"valuelistCustomValuesWithDBInfo", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_INVALID_CUSTOM_VALUES = new Pair<String, ProblemSeverity>(
		"valuelistInvalidCustomValues", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_TYPE_UNKNOWN = new Pair<String, ProblemSeverity>("valuelistTypeUnknown", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> VALUELIST_GENERIC_ERROR = new Pair<String, ProblemSeverity>(
		"valuelistGenericError", ProblemSeverity.ERROR); //$NON-NLS-1$

	// styles
	public final static Pair<String, ProblemSeverity> STYLE_NOT_FOUND = new Pair<String, ProblemSeverity>("styleNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> STYLE_CLASS_NO_STYLE = new Pair<String, ProblemSeverity>("styleClassNoStyle", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> STYLE_CLASS_NOT_FOUND = new Pair<String, ProblemSeverity>("styleClassNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$

	// solution problems
	public final static Pair<String, ProblemSeverity> SOLUTION_BAD_STRUCTURE = new Pair<String, ProblemSeverity>("solutionBadStructure", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_DESERIALIZE_ERROR = new Pair<String, ProblemSeverity>(
		"solutionDeserializeError", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER = new Pair<String, ProblemSeverity>(
		"solutionElementNameInvalidIdentifier", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_ELEMENT_NAME_RESERVED_PREFIX_IDENTIFIER = new Pair<String, ProblemSeverity>(
		"solutionElementNameReservedPrefixIdentifier", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED = new Pair<String, ProblemSeverity>(
		"solutionPropertyFormCannotBeInstantiated", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_PROPERTY_TARGET_NOT_FOUND = new Pair<String, ProblemSeverity>(
		"solutionPropertyTargetNotFound", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SERVER_NOT_ACCESSIBLE_FIRST_OCCURENCE = new Pair<String, ProblemSeverity>(
		"serverNotAccessibleFirstOccurence", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> CONSTANTS_USED = new Pair<String, ProblemSeverity>("constantsUsed", ProblemSeverity.ERROR); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_USED_AS_WEBSERVICE_MUSTAUTHENTICATE_PROBLEM = new Pair<String, ProblemSeverity>(
		"solutionUsedAsWebServiceMustAuthenticateProblem", ProblemSeverity.WARNING); //$NON-NLS-1$
	public final static Pair<String, ProblemSeverity> SOLUTION_WITH_HIGHER_FILE_VERSION = new Pair<String, ProblemSeverity>(
		"solutionWithHigherFileVersion", ProblemSeverity.ERROR); //$NON-NLS-1$

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
	protected IProject[] build(int kind, Map args, IProgressMonitor progressMonitor) throws CoreException
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
						StringTokenizer st = new StringTokenizer(moduleNames, ";,"); //$NON-NLS-1$
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
		if (kind == FULL_BUILD)
		{
			fullBuild(progressMonitor);
		}
		else
		{
			try
			{
				for (IProject p : monitoredProjects)
				{
					if (p.exists() && p.isOpen())
					{
						IResourceDelta delta = getDelta(p);
						if (delta != null)
						{
							incrementalBuild(delta, progressMonitor);
						}
					} // should we generate a problem marker otherwise?
				}
				IResourceDelta delta = getDelta(getProject());
				if (delta != null)
				{
					incrementalBuild(delta, progressMonitor);
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
	}

	void checkResource(IResource resource)
	{
		try
		{
			if (resource instanceof IFile && resource.getName().endsWith(".js")) //$NON-NLS-1$
			{
				checkDuplicateScopes((IFile)resource);
			}
			else if (resource instanceof IFile && resource.getName().endsWith(".xml")) //$NON-NLS-1$
			{
				checkXML((IFile)resource);
			}
			else if (resource instanceof IProject)
			{
				IProject project = (IProject)resource;
				if (!project.exists() || (project.isOpen() && project.hasNature(ServoyProject.NATURE_ID) && project == getProject()))
				{
					// a project that this builder in interested in was deleted (so a module or the resources proj.)
					// or something has changed in this builder's solution project
					checkServoyProject(getProject());
					checkModules(getProject());
					checkResourcesForServoyProject(getProject());
					checkResourcesForModules(getProject());
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
							checkServoyProject(getProject());
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
			ServoyLog.logWarning("Exception while performing build", e); //$NON-NLS-1$
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
					StringTokenizer st = new StringTokenizer(modulesNames, ";,"); //$NON-NLS-1$
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
			String[] modulesNames = Utils.getTokenElements(servoyProject.getSolution().getModulesNames(), ",", true); //$NON-NLS-1$
			if (modulesNames != null)
			{
				for (String name : modulesNames)
				{
					ServoyProject module = getServoyModel().getServoyProject(name);
					if (module == null)
					{
						ServoyMarker mk = MarkerMessages.ModuleNotFound.fill(name, servoyProject.getSolution().getName());
						IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, MODULE_NOT_FOUND, IMarker.PRIORITY_HIGH, null, null);
						if (marker != null)
						{
							try
							{

								marker.setAttribute("moduleName", name); //$NON-NLS-1$
								marker.setAttribute("solutionName", servoyProject.getSolution().getName()); //$NON-NLS-1$
							}
							catch (Exception e)
							{
								ServoyLog.logError(e);
							}
						}
					}
					else if (SolutionMetaData.isServoyMobileSolution(servoyProject.getSolution()) &&
						!SolutionMetaData.isServoyMobileSolution(module.getSolution())) checkMobileModule(servoyProject, module);
					if (SolutionMetaData.isServoyMobileSolution(servoyProject.getSolution()) &&
						module != null &&
						(module.getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE && module.getSolutionMetaData().getSolutionType() != SolutionMetaData.MOBILE_MODULE))
					{
						String message = "Module " + module.getSolution().getName() + " is a mobile solution module, it should have solution type Mobile or Mobile shared module."; //$NON-NLS-1$//$NON-NLS-2$
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
						String message = "Module " + module.getSolution().getName() + " is a non-mobile solution module, it should have solution type Mobile shared module or other non-mobile type."; //$NON-NLS-1$//$NON-NLS-2$
						IMarker marker = addMarker(project, MISPLACED_MODULES_MARKER_TYPE, message, -1, MODULE_MISPLACED, IMarker.PRIORITY_LOW, null, null);
						try
						{
							marker.setAttribute("SolutionName", module.getSolution().getName()); //$NON-NLS-1$
							marker.setAttribute("SolutionType", SolutionMetaData.MOBILE); //$NON-NLS-1$
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}

				// import hook modules should not contain other modules
				if (SolutionMetaData.isPreImportHook(servoyProject.getSolution()) && modulesNames.length > 0)
				{
					String message = "Module " + servoyProject.getSolution().getName() + " is a solution import hook, so it should not contain any modules."; //$NON-NLS-1$//$NON-NLS-2$
					addMarker(project, MISPLACED_MODULES_MARKER_TYPE, message, -1, MODULE_MISPLACED, IMarker.PRIORITY_LOW, null, null);
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
			String message = "Module " + module.getSolution().getName() + " is a mobile solution module, so it should contain only relations and calculations."; //$NON-NLS-1$//$NON-NLS-2$
			addMarker(mobileProject.getProject(), INVALID_MOBILE_MODULE_MARKER_TYPE, message, -1, MODULE_INVALID_MOBILE, IMarker.PRIORITY_NORMAL, null, null);
		}
	}

	private void checkDuplicateScopes(IFile scriptFile)
	{
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
			List<Pair<String, IRootObject>> scopes = getServoyModel().getFlattenedSolution().getAllScopes();
			for (Pair<String, IRootObject> scope : scopes)
			{
				if (scope.getLeft().toLowerCase().equals(lowerCaseScopeName) && !scope.getRight().getName().equals(scriptFile.getProject().getName()))
				{
					String otherFile = scope.getRight().getName() + '/' + scope.getLeft() + SolutionSerializer.JS_FILE_EXTENSION;
					IFile file = scriptFile.getWorkspace().getRoot().getFile(Path.fromPortableString(otherFile));
					if (scriptFile.exists())
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

	private static final Integer METHOD_DUPLICATION = Integer.valueOf(1);
	private static final Integer FORM_DUPLICATION = Integer.valueOf(2);
	private static final Integer RELATION_DUPLICATION = Integer.valueOf(3);
	private static final Integer VALUELIST_DUPLICATION = Integer.valueOf(4);
	private static final Integer MEDIA_DUPLICATION = Integer.valueOf(5);
	private static final Integer TABLE_CALCULATION_DUPLICATION = Integer.valueOf(6);
	private static final Integer TABLE_AGREGATION_DUPLICATION = Integer.valueOf(7);
	private static final Integer TABLE_SCRIPT_METHOD_DUPLICATION = Integer.valueOf(8);

	private void addDuplicatePersist(final IPersist persist, Map<String, Map<Integer, Set<Pair<String, ISupportChilds>>>> duplicationMap, final IProject project)
	{
		if (persist instanceof IScriptProvider || persist instanceof ScriptVariable)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				String scopeName = null;
				if (persist instanceof ISupportScope && persist.getParent() instanceof Solution)
				{
					scopeName = ((ISupportScope)persist).getScopeName();
					if (scopeName == null) scopeName = ScriptVariable.GLOBAL_SCOPE;
				}
				List<Pair<String, ISupportChilds>> duplicatedParents = new ArrayList<Pair<String, ISupportChilds>>(3);
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(name, persistSet);
				}
				Set<Pair<String, ISupportChilds>> parentScopeSet = persistSet.get(METHOD_DUPLICATION);
				Pair<String, ISupportChilds> scopedParent = new Pair<String, ISupportChilds>(scopeName, persist.getParent());
				if (parentScopeSet != null && parentScopeSet.contains(scopedParent))
				{
					duplicatedParents.add(scopedParent);
				}
				else if (parentScopeSet != null && persist.getParent() instanceof Solution)
				{
					for (Pair<String, ISupportChilds> supportChilds : parentScopeSet)
					{
						if (supportChilds.getRight() instanceof Solution && (scopeName == null || scopeName.equals(supportChilds.getLeft())))
						{
							duplicatedParents.add(supportChilds);
						}
					}
				}

				for (Pair<String, ISupportChilds> duplicatedParent : duplicatedParents)
				{
					String duplicateParentsName = ""; //$NON-NLS-1$
					if (duplicatedParent.getRight() instanceof ISupportName)
					{
						duplicateParentsName = ((ISupportName)duplicatedParent.getRight()).getName();
					}
					if (duplicatedParent.getLeft() != null)
					{
						duplicateParentsName += '.' + duplicatedParent.getLeft();
					}
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					if (scopeName != null)
					{
						parentsName += '.' + scopeName;
					}
					String type = "method"; //$NON-NLS-1$
					if (persist instanceof ScriptVariable)
					{
						type = "variable"; //$NON-NLS-1$
					}
					String otherChildsType = "method"; //$NON-NLS-1$
					for (IPersist child : Utils.iterate(duplicatedParent.getRight().getAllObjects()))
					{
						if ((child instanceof IScriptProvider || child instanceof ScriptVariable) && ((ISupportName)child).getName().equals(name))
						{
							int lineNumber;
							if (child instanceof IScriptElement)
							{
								if (child instanceof ScriptVariable)
								{
									otherChildsType = "variable"; //$NON-NLS-1$
								}
								lineNumber = ((IScriptElement)child).getLineNumberOffset();
							}
							else lineNumber = 0;
//							if (persist instanceof ScriptVariable && otherChildsType.equals("variable") && !duplicateParentsName.equals(parentsName)) //$NON-NLS-1$
//							{
//								severity = IMarker.SEVERITY_WARNING;
//							}
							ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(type, name, parentsName);
							addMarker(project, mk.getType(), mk.getText(), lineNumber, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, child);
							break;
						}
					}
					int lineNumber;
					if (persist instanceof IScriptElement)
					{
						if (persist instanceof ScriptVariable)
						{
							otherChildsType = "variable"; //$NON-NLS-1$
						}
						lineNumber = ((IScriptElement)persist).getLineNumberOffset();
					}
					else lineNumber = 0;
					ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(otherChildsType, name, duplicateParentsName);
					addMarker(project, mk.getType(), mk.getText(), lineNumber, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, persist);

				}
				Set<Pair<String, ISupportChilds>> parents = parentScopeSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(METHOD_DUPLICATION, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(scopeName, persist.getParent()));
			}
		}
		//Table entities 
		/*
		 * private static final Integer TABLE_CALCULATION_DUPLICATION = Integer.valueOf(6); private static final Integer TABLE_AGREGATION_DUPLICATION =
		 * Integer.valueOf(7); private static final Integer TABLE_SCRIPT_METHOD_DUPLICATION = Integer.valueOf(8);
		 */
		if (persist.getParent() instanceof TableNode)
		{
			String entityName = ((ISupportName)persist).getName();
			String dataSource = ((TableNode)persist.getParent()).getDataSource();

			if (entityName != null && dataSource != null)
			{
				String entityName_plus_dataSource = entityName + '~' + dataSource;
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(entityName_plus_dataSource);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(entityName_plus_dataSource, persistSet);
					if (persist instanceof IScriptProvider || persist instanceof ScriptVariable)
					{
						String name = ((ISupportName)persist).getName();
						duplicationMap.remove(name);
					}
				}

				Integer type = TABLE_CALCULATION_DUPLICATION; // ScriptCalculation
				if (persist instanceof AggregateVariable)
				{
					type = TABLE_AGREGATION_DUPLICATION;
				}
				else if (persist instanceof ScriptMethod)
				{
					type = TABLE_SCRIPT_METHOD_DUPLICATION;
				}
				Set<Pair<String, ISupportChilds>> parentSet = persistSet.get(type);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent().getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent().getParent()).getName();
					}
					for (Pair<String, ISupportChilds> parent : parentSet)
					{
						if (parent.getRight() instanceof TableNode)
						{
							TableNode tableNode = (TableNode)parent.getRight();

							if (persist instanceof ScriptCalculation)
							{
								ScriptCalculation duplicateScriptCalculation = tableNode.getScriptCalculation(entityName);
								if (dataSource.equals(tableNode.getDataSource()))
								{
									if (duplicateScriptCalculation != null)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
											"table calculation", entityName, parentsName + "   on table  " + tableNode.getDataSource()); //$NON-NLS-1$
										addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
											duplicateScriptCalculation);
									}
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
										"table calculation ", entityName, tableNode.getParent() + "  on table  " + tableNode.getDataSource()); //$NON-NLS-1$								
									addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
										persist);
								}

							}
							else if (persist instanceof AggregateVariable)
							{
								AggregateVariable duplicateAggregate = tableNode.getAggregateVariable(entityName);
								if (dataSource.equals(tableNode.getDataSource()))
								{
									if (duplicateAggregate != null)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
											"aggregate variable", entityName, parentsName + "   on table  " + tableNode.getDataSource()); //$NON-NLS-1$
										addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
											duplicateAggregate);
									}
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
										"aggregate variable", entityName, tableNode.getParent() + "  on table  " + tableNode.getDataSource()); //$NON-NLS-1$								
									addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
										persist);
								}
							}
							else if (persist instanceof ScriptMethod)
							{
								ScriptMethod duplicateFoundSetMethod = tableNode.getFoundsetMethod(entityName);
								if (dataSource.equals(tableNode.getDataSource()))
								{
									if (duplicateFoundSetMethod != null)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
											"table method", entityName, parentsName + "   on table  " + tableNode.getDataSource()); //$NON-NLS-1$
										addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
											duplicateFoundSetMethod);
									}
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
										"table method", entityName, tableNode.getParent() + "  on table  " + tableNode.getDataSource()); //$NON-NLS-1$								
									addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
										persist);
								}
							}
						}
					}
				}
				Set<Pair<String, ISupportChilds>> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(type, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(null, persist.getParent()));

			}


			/*
			 * ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(otherChildsType, name, duplicateParentsName); addMarker(project, mk.getType(),
			 * mk.getText(), lineNumber, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, persist);
			 */

		}
		if (persist instanceof Form)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(name, persistSet);
				}
				Set<Pair<String, ISupportChilds>> parentSet = persistSet.get(FORM_DUPLICATION);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					for (Pair<String, ISupportChilds> parent : parentSet)
					{
						if (parent.getRight() instanceof Solution)
						{
							Solution solution = (Solution)parent.getRight();
							Form duplicateForm = solution.getForm(name);
							if (duplicateForm != null)
							{
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("form", name, parentsName); //$NON-NLS-1$
								addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
									duplicateForm);
							}
							ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("form", name, solution.getName()); //$NON-NLS-1$
							addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, persist);
						}
					}
				}
				Set<Pair<String, ISupportChilds>> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(FORM_DUPLICATION, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(null, persist.getParent()));
			}
			final Map<String, Set<IPersist>> formElementsByName = new HashMap<String, Set<IPersist>>();
			Form flattenedForm = ServoyBuilder.getPersistFlattenedSolution(persist, getServoyModel().getFlattenedSolution()).getFlattenedForm(persist);
			flattenedForm.acceptVisitor(new IPersistVisitor()
			{
				public Object visit(IPersist o)
				{
					if (!(o instanceof ScriptVariable) && !(o instanceof ScriptMethod) && !(o instanceof Form) && o instanceof ISupportName &&
						((ISupportName)o).getName() != null)
					{
						Set<IPersist> duplicates = formElementsByName.get(((ISupportName)o).getName());
						if (duplicates != null)
						{
							for (IPersist duplicatePersist : duplicates)
							{
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(
									"form element", ((ISupportName)o).getName(), ((Form)persist).getName()); //$NON-NLS-1$
								addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
									duplicatePersist);

								mk = MarkerMessages.DuplicateEntityFound.fill("form element", ((ISupportName)o).getName(), ((Form)persist).getName()); //$NON-NLS-1$								
								addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, o);
							}
						}
						else
						{
							duplicates = new HashSet<IPersist>();
							duplicates.add(o);
						}
						formElementsByName.put(((ISupportName)o).getName(), duplicates);
					}
					if (o instanceof Form) return IPersistVisitor.CONTINUE_TRAVERSAL;
					else return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
				}
			});
		}
		if (persist instanceof Relation || persist instanceof ValueList || persist instanceof Media)
		{
			String name = ((ISupportName)persist).getName();
			if (name != null)
			{
				Map<Integer, Set<Pair<String, ISupportChilds>>> persistSet = duplicationMap.get(name);
				if (persistSet == null)
				{
					persistSet = new HashMap<Integer, Set<Pair<String, ISupportChilds>>>();
					duplicationMap.put(name, persistSet);
				}
				Integer type = RELATION_DUPLICATION;
				if (persist instanceof ValueList)
				{
					type = VALUELIST_DUPLICATION;
				}
				else if (persist instanceof Media)
				{
					type = MEDIA_DUPLICATION;
				}
				Set<Pair<String, ISupportChilds>> parentSet = persistSet.get(type);
				if (parentSet != null)
				{
					String parentsName = ""; //$NON-NLS-1$
					if (persist.getParent() instanceof ISupportName)
					{
						parentsName = ((ISupportName)persist.getParent()).getName();
					}
					for (Pair<String, ISupportChilds> parent : parentSet)
					{
						if (parent.getRight() instanceof Solution)
						{
							Solution solution = (Solution)parent.getRight();
							if (persist instanceof Relation)
							{
								Relation duplicateRelation = solution.getRelation(name);
								if (!((Relation)persist).contentEquals(duplicateRelation))
								{
									if (duplicateRelation != null)
									{
										ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("relation", name, parentsName); //$NON-NLS-1$
										addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
											duplicateRelation);
									}
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("relation", name, solution.getName()); //$NON-NLS-1$								
									addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
										persist);
								}
							}
							else if (persist instanceof ValueList)
							{
								ValueList duplicateValuelist = solution.getValueList(name);
								if (duplicateValuelist != null)
								{
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("valuelist", name, parentsName); //$NON-NLS-1$
									addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
										duplicateValuelist);
								}
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("valuelist", name, solution.getName()); //$NON-NLS-1$								
								addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, persist);
							}
							else if (persist instanceof Media)
							{
								Media duplicateMedia = solution.getMedia(name);
								if (duplicateMedia != null)
								{
									ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("media", name, parentsName); //$NON-NLS-1$
									addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null,
										duplicateMedia);
								}
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("media", name, solution.getName()); //$NON-NLS-1$								
								addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, persist);
							}
						}
					}
				}
				Set<Pair<String, ISupportChilds>> parents = parentSet;
				if (parents == null)
				{
					parents = new HashSet<Pair<String, ISupportChilds>>();
					persistSet.put(type, parents);
				}
				parents.add(new Pair<String, ISupportChilds>(null, persist.getParent()));
			}
		}
	}

	private void checkPersistDuplication()
	{
		// this is a special case
		ServoyProject[] modules = getServoyModel().getModulesOfActiveProject();
		final Map<String, Map<Integer, Set<Pair<String, ISupportChilds>>>> duplicationMap = new HashMap<String, Map<Integer, Set<Pair<String, ISupportChilds>>>>();
		if (modules != null)
		{
			for (ServoyProject module : modules)
			{
				deleteMarkers(module.getProject(), DUPLICATE_NAME_MARKER_TYPE);
			}
			for (final ServoyProject module : modules)
			{
				module.getSolution().acceptVisitor(new IPersistVisitor()
				{
					public Object visit(IPersist o)
					{
						addDuplicatePersist(o, duplicationMap, module.getProject());
						return CONTINUE_TRAVERSAL;
					}

				});
			}
		}
	}

	private void addDeprecatedRelationWarningIfNeeded(IPersist persist, String relationsString, IProject project, String message,
		FlattenedSolution flattenedSolution)
	{
		if (relationsString != null)
		{
			String[] aRelationsString = relationsString.split(" ");
			if (aRelationsString.length > 0)
			{
				String[] relations = Utils.stringSplit(aRelationsString[0], "."); //$NON-NLS-1$
				Relation relation;
				for (String r : relations)
				{
					relation = flattenedSolution.getRelation(r);
					if (relation != null)
					{
						addDeprecatedElementWarningIfNeeded(persist, relation, project, message.replace("{r}", relation.getName()));
					}
				}
			}
		}
	}

	private void addDeprecatedElementWarningIfNeeded(IPersist persist, ISupportDeprecated deprecatedPersist, IProject project, String message)
	{
		if (deprecatedPersist != null)
		{
			String deprecatedInfo = deprecatedPersist.getDeprecated();

			if (deprecatedInfo != null)
			{
				addMarker(project, DEPRECATED_ELEMENT_USAGE, message + " " + deprecatedInfo, -1, DEPRECATED_ELEMENT_USAGE_PROBLEM, IMarker.PRIORITY_NORMAL, //$NON-NLS-1$
					null, persist);
			}
		}
	}

	/*
	 * Checks usage of deprecated Form/Relation/Valuelist/Media inside elements properties
	 */
	private void checkDeprecatedElementUsage(IPersist persist, IProject project, FlattenedSolution flattenedSolution)
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
				addDeprecatedElementWarningIfNeeded(persist, firstForm, project,
					"Solution \"" + elementName + "\" has a deprecated first form \"" + firstForm.getName() + "\".");
			}
		}

		if (persist instanceof Form)
		{
			// check form extends of a deprecated form
			Form extendsForm = ((Form)persist).getExtendsForm();
			if (extendsForm != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, extendsForm, project,
					"Form \"" + elementName + "\" extends a deprecated form \"" + extendsForm.getName() + "\".");
			}

			String initialSort = ((Form)persist).getInitialSort();
			if (initialSort != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, initialSort, project, "Form \"" + elementName +
					"\" has a deprecated relation \"{r}\" as initial sort.", flattenedSolution);
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
					addDeprecatedElementWarningIfNeeded(persist, tabForm, project,
						"Element \"" + elementName + "\" contains a deprecated form \"" + tabForm.getName() + "\".");
				}
				// check usage of deprecated relation for a tab
				tabRelationName = tab.getRelationName();
				if (tabRelationName != null)
				{
					addDeprecatedRelationWarningIfNeeded(persist, tabRelationName, project, "Element \"" + elementName +
						"\" has a deprecated relation \"{r}\".", flattenedSolution);
				}
			}
		}
		else if (persist instanceof Field)
		{
			// check usage of deprecated valuelist inside a field
			ValueList valuelist = flattenedSolution.getValueList(((Field)persist).getValuelistID());
			if (valuelist != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, valuelist, project,
					"Field \"" + elementName + "\" has a deprecated valuelist \"" + valuelist.getName() + "\".");
			}
		}
		else if (persist instanceof ValueList)
		{
			// check usage of deprecated valuelist as fallback valuelist
			ValueList fallbackValuelist = flattenedSolution.getValueList(((ValueList)persist).getFallbackValueListID());
			if (fallbackValuelist != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, fallbackValuelist, project, "Valuelist \"" + elementName +
					"\" has a deprecated fallback valuelist \"" + fallbackValuelist.getName() + "\".");
			}

			// check usage of deprecated relation inside a valuelist
			String relationName = ((ValueList)persist).getRelationName();
			if (relationName != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, relationName, project, "Valuelist \"" + elementName + "\" has a deprecated relation \"{r}\".",
					flattenedSolution);
			}

			String sortOptions = ((ValueList)persist).getSortOptions();
			if (sortOptions != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, sortOptions, project, "Valuelist \"" + elementName +
					"\" has a deprecated relation \"{r}\" as sort option.", flattenedSolution);
			}
		}
		else if (persist instanceof Portal)
		{
			// check usage of deprecated relation inside a portal
			String relationName = ((Portal)persist).getRelationName();
			if (relationName != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, relationName, project, "Portal \"" + elementName + "\" has a deprecated relation \"{r}\".",
					flattenedSolution);
			}
		}
		else if (persist instanceof Relation)
		{
			String initialSort = ((Relation)persist).getInitialSort();
			if (initialSort != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, initialSort, project, "Relation \"" + elementName +
					"\" has a deprecated relation \"{r}\" as initial sort.", flattenedSolution);
			}
		}

		if (persist instanceof ISupportMedia)
		{
			// check usage of deprecated media
			Media media = flattenedSolution.getMedia(((ISupportMedia)persist).getImageMediaID());
			if (media != null)
			{
				addDeprecatedElementWarningIfNeeded(persist, media, project,
					"Element \"" + elementName + "\" has a deprecated image media \"" + media.getName() + "\".");
			}
		}
		if (persist instanceof ISupportDataProviderID)
		{
			// check usage of deprecated relation inside dataprovider ids
			String dataProviderID = ((ISupportDataProviderID)persist).getDataProviderID();
			if (dataProviderID != null)
			{
				addDeprecatedRelationWarningIfNeeded(persist, dataProviderID, project, "Element \"" + elementName +
					"\" has a dataprovider with a deprecated relation \"{r}\".", flattenedSolution);
			}
		}
	}

	private void checkDeprecatedPropertyUsage(IPersist persist, IProject project)
	{
		if (persist instanceof Solution)
		{
			Solution solution = (Solution)persist;

			// loginForm is deprecated, use loginSolution (not needed for WebClient)
			if (solution.getLoginFormID() > 0)
			{
				try
				{
					if (solution.getLoginSolutionName() != null)
					{
						// login form will be ignored
						addDeprecatedPropertyUsageMarker(persist, project, DEPRECATED_PROPERTY_USAGE_PROBLEM,
							StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName(), "Solution '" + solution.getName() +
								"' has a loginForm property set which is overridden by the loginSolutionName property.");
					}
					else if (solution.getSolutionType() != SolutionMetaData.WEB_CLIENT_ONLY && solution.getSolutionType() != SolutionMetaData.MOBILE)
					{
						// loginForm is deprecated
						addDeprecatedPropertyUsageMarker(persist, project, DEPRECATED_PROPERTY_USAGE_PROBLEM,
							StaticContentSpecLoader.PROPERTY_LOGINFORMID.getPropertyName(),
							"Solution '" + solution.getName() + "' has a loginForm property set which is deprecated, use loginSolutionName property instead."); //$NON-NLS-1$ //$NON-NLS-2$
					}
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}

		if (persist instanceof Form || persist instanceof Portal)
		{
			String rowBgColorCalculation = null;
			String type = "Form"; //$NON-NLS-1$
			if (persist instanceof Form)
			{
				rowBgColorCalculation = ((Form)persist).getRowBGColorCalculation();
			}
			else
			{
				rowBgColorCalculation = ((Portal)persist).getRowBGColorCalculation();
				type = "Portal";//$NON-NLS-1$
			}
			if (rowBgColorCalculation != null)
			{
				try
				{
					//only rowBGColorCalculation deprecated property usage marker is error by default
					addDeprecatedPropertyUsageMarker(persist,
						project,
						new Pair<String, ProblemSeverity>("deprecatedPropertyUsage", ProblemSeverity.ERROR), //$NON-NLS-1$
						StaticContentSpecLoader.PROPERTY_ROWBGCOLORCALCULATION.getPropertyName(),
						type +
							" '" + ((ISupportName)persist).getName() + "' has rowBGColorCalculation property set which is deprecated, use CSS (odd/even/selected) or onRender event instead."); //$NON-NLS-1$ //$NON-NLS-2$
				}
				catch (Exception e)
				{
					ServoyLog.logError(e);
				}
			}
		}
	}

	private void addDeprecatedPropertyUsageMarker(IPersist persist, IProject project, Pair<String, ProblemSeverity> serverityPair, String propertyName,
		String message) throws CoreException
	{
		if (message != null)
		{
			IMarker marker = addMarker(project, DEPRECATED_PROPERTY_USAGE, message, -1, serverityPair, IMarker.PRIORITY_NORMAL, null, persist);
			if (marker != null)
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", project.getName()); //$NON-NLS-1$
				marker.setAttribute("PropertyName", propertyName); //$NON-NLS-1$
				marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName(propertyName, persist.getClass())); //$NON-NLS-1$
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
				IServer server = ApplicationServerSingleton.get().getServerManager().getServer(foreignServer);
				if (server != null) goodServers.add(foreignServer);
				else missingServers.put(foreignServer, persist);
			}
		}
		if (serverName != null && !missingServers.containsKey(serverName) && !goodServers.contains(serverName))
		{
			IServer server = ApplicationServerSingleton.get().getServerManager().getServer(serverName);
			if (server != null) goodServers.add(serverName);
			else missingServers.put(serverName, persist);
		}
	}

	private void checkDuplicateUUID(IPersist persist, IProject project)
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
						addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE, IMarker.PRIORITY_HIGH, fileForLocation.toString(),
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

	private void checkServoyProject(final IProject project)
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
				checkPersistDuplication();
				checkServers(project);

				final Solution solution = servoyProject.getSolution();
				if (servoyModel.getActiveProject().getSolution().getName().equals(solution.getName()))
				{
					//skipping modules when checking for web service solutions
					if (solution.getMustAuthenticate())
					{
						//check if solution is used as web service
						Iterator<Form> formsIt = Solution.getForms(solution.getAllObjectsAsList(), null, false);
						boolean isServiceSolution = false;
						while (formsIt.hasNext() && !isServiceSolution)
						{
							Iterator<ScriptMethod> methodIt = formsIt.next().getScriptMethods(false);
							while (methodIt.hasNext())
							{
								String methodName = methodIt.next().getName();
								if (methodName.equals("ws_read") || methodName.equals("ws_create") || //$NON-NLS-1$//$NON-NLS-2$
									methodName.equals("ws_delete") || methodName.equals("ws_update") || //$NON-NLS-1$//$NON-NLS-2$
									methodName.equals("ws_authenticate") || methodName.equals("ws_response_headers")) //$NON-NLS-1$ //$NON-NLS-2$
								{
									isServiceSolution = true;
									break;
								}
							}
						}
						if (isServiceSolution)
						{
							//create warning marker
							ServoyMarker mk = MarkerMessages.SolutionUsedAsWebServiceMustAuthenticateProblem.fill(solution.getName());
							addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_USED_AS_WEBSERVICE_MUSTAUTHENTICATE_PROBLEM, IMarker.PRIORITY_HIGH,
								null, solution);
						}
					}
				}

				if (!servoyModel.shouldBeModuleOfActiveSolution(solution.getName()) &&
					solution.getSolutionMetaData().getFileVersion() > AbstractRepository.repository_version)
				{
					ServoyMarker mk = MarkerMessages.SolutionWithHigherFileVersion.fill("Solution", solution.getName());
					addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_WITH_HIGHER_FILE_VERSION, IMarker.PRIORITY_HIGH, null, null);
				}

				solution.acceptVisitor(new IPersistVisitor()
				{
					private final ServoyProject[] modules = getSolutionModules(servoyProject);
					private final FlattenedSolution flattenedSolution = getServoyModel().getFlattenedSolution();
					private IntHashMap<IPersist> elementIdPersistMap = null;
					private final Map<UUID, List<IPersist>> theMakeSureNoDuplicateUUIDsAreFound = new HashMap<UUID, List<IPersist>>();
					private final Map<Form, Boolean> formsAbstractChecked = new HashMap<Form, Boolean>();
					private final Set<UUID> methodsParsed = new HashSet<UUID>();

					public Object visit(final IPersist o)
					{
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
							final Map<String, Method> methods = ((EclipseRepository)ApplicationServerSingleton.get().getDeveloperRepository()).getGettersViaIntrospection(o);
							for (ContentSpec.Element element : Utils.iterate(((EclipseRepository)ApplicationServerSingleton.get().getDeveloperRepository()).getContentSpec().getPropertiesForObjectType(
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
									final int element_id = Utils.getAsInteger(property_value);
									if (element_id > 0)
									{
										if (elementIdPersistMap == null)
										{
											missingServers.clear();
											goodServers.clear();
											elementIdPersistMap = new IntHashMap<IPersist>();
											solution.acceptVisitor(new IPersistVisitor()
											{
												public Object visit(IPersist p)
												{
													elementIdPersistMap.put(p.getID(), p);
													List<IPersist> lst = theMakeSureNoDuplicateUUIDsAreFound.get(p.getUUID());
													if (lst == null)
													{
														lst = new ArrayList<IPersist>(3);
														lst.add(p);
														theMakeSureNoDuplicateUUIDsAreFound.put(p.getUUID(), lst);

														if (((AbstractBase)p).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID) != null)
														{
															checkDuplicateUUID(p, project);
														}
													}
													else
													{
														IPersist other = lst.get(0);

														// for now only add it on both if there is 1, just skip the rest.
														if (lst.size() == 1)
														{
															ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(other.getUUID(),
																SolutionSerializer.getRelativePath(p, false) + SolutionSerializer.getFileName(p, false));
															addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE,
																IMarker.PRIORITY_HIGH, null, other);
														}
														ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(p.getUUID(),
															SolutionSerializer.getRelativePath(other, false) + SolutionSerializer.getFileName(other, false));
														addMarker(project, mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE, IMarker.PRIORITY_HIGH,
															null, p);
													}
													return IPersistVisitor.CONTINUE_TRAVERSAL;
												}
											});
											if (modules != null)
											{
												for (ServoyProject module : modules)
												{
													if (module.getSolution() != null && !module.equals(servoyProject))
													{
														final IProject moduleProject = ResourcesPlugin.getWorkspace().getRoot().getProject(
															module.getSolution().getName());
														deleteMarkers(moduleProject, DUPLICATE_UUID);
														deleteMarkers(moduleProject, DUPLICATE_SIBLING_UUID);
														deleteMarkers(moduleProject, SOLUTION_PROBLEM_MARKER_TYPE);

														if (module.getSolutionMetaData().getFileVersion() > AbstractRepository.repository_version)
														{
															ServoyMarker mk = MarkerMessages.SolutionWithHigherFileVersion.fill("Module",
																module.getSolution().getName());
															addMarker(moduleProject, mk.getType(), mk.getText(), -1, SOLUTION_WITH_HIGHER_FILE_VERSION,
																IMarker.PRIORITY_HIGH, null, null);
														}

														module.getSolution().acceptVisitor(new IPersistVisitor()
														{
															public Object visit(IPersist p)
															{
																elementIdPersistMap.put(p.getID(), p);
																List<IPersist> lst = theMakeSureNoDuplicateUUIDsAreFound.get(p.getUUID());
																if (lst == null)
																{
																	lst = new ArrayList<IPersist>(3);
																	lst.add(p);
																	theMakeSureNoDuplicateUUIDsAreFound.put(p.getUUID(), lst);
																	if (((AbstractBase)p).getRuntimeProperty(SolutionDeserializer.POSSIBLE_DUPLICATE_UUID) != null)
																	{
																		checkDuplicateUUID(p, moduleProject);
																	}
																}
																else
																{
																	IPersist other = lst.get(0);

																	// for now only add it on both if there is 1, just skip the rest.
																	if (lst.size() == 1)
																	{
																		ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(
																			other.getUUID(),
																			SolutionSerializer.getRelativePath(p, false) +
																				SolutionSerializer.getFileName(p, false));
																		addMarker(moduleProject, mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE,
																			IMarker.PRIORITY_HIGH, null, other);
																	}
																	ServoyMarker mk = MarkerMessages.UUIDDuplicateIn.fill(
																		p.getUUID(),
																		SolutionSerializer.getRelativePath(other, false) +
																			SolutionSerializer.getFileName(other, false));
																	addMarker(moduleProject, mk.getType(), mk.getText(), -1, DUPLICATION_UUID_DUPLICATE,
																		IMarker.PRIORITY_HIGH, null, p);
																}
																return IPersistVisitor.CONTINUE_TRAVERSAL;
															}
														});
													}
												}
											}
										}
										final IPersist foundPersist = elementIdPersistMap.get(element_id);
										if (foundPersist == null && !element.getName().equals(StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName()))
										{
											String elementName = null;
											String inForm = null;
											if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null)) elementName = ((ISupportName)o).getName();
											if (context instanceof Form) inForm = ((Form)context).getName();
											ServoyMarker mk;
											Pair<String, ProblemSeverity> problemPair;
											boolean addMarker = true;
											if (elementName == null)
											{
												if (inForm == null)
												{
													mk = MarkerMessages.PropertyTargetNotFound.fill(element.getName());
													problemPair = SOLUTION_PROPERTY_TARGET_NOT_FOUND;
												}
												else
												{
													mk = MarkerMessages.PropertyInFormTargetNotFound.fill(element.getName(), inForm);
													problemPair = FORM_PROPERTY_TARGET_NOT_FOUND;
												}
											}
											else
											{
												if (inForm == null)
												{
													mk = MarkerMessages.PropertyOnElementTargetNotFound.fill(element.getName(), elementName);
													problemPair = SOLUTION_PROPERTY_TARGET_NOT_FOUND;
													IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW,
														null, o);
													marker.setAttribute("Uuid", o.getUUID().toString()); //$NON-NLS-1$
													marker.setAttribute("SolutionName", elementName); //$NON-NLS-1$
													marker.setAttribute("PropertyName", element.getName()); //$NON-NLS-1$ 
													marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName(element.getName(), o.getClass())); //$NON-NLS-1$ 
													addMarker = false;
												}
												else
												{
													mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(element.getName(), elementName, inForm);
													problemPair = FORM_PROPERTY_TARGET_NOT_FOUND;
												}
											}
											if (BaseComponent.isEventProperty(element.getName()) || BaseComponent.isCommandProperty(element.getName()))
											{
												// TODO: this is a place where the same marker appears in more than one category...
												IMarker marker = addMarker(project, BaseComponent.isEventProperty(element.getName()) ? INVALID_EVENT_METHOD
													: INVALID_COMMAND_METHOD, mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
												if (marker != null)
												{
													marker.setAttribute("EventName", element.getName()); //$NON-NLS-1$
													if (context instanceof Form)
													{
														marker.setAttribute("DataSource", ((Form)context).getDataSource()); //$NON-NLS-1$
														marker.setAttribute("ContextTypeId", context.getTypeID()); //$NON-NLS-1$
													}
													if (context instanceof TableNode)
													{
														marker.setAttribute("DataSource", ((TableNode)context).getDataSource()); //$NON-NLS-1$
														marker.setAttribute("ContextTypeId", context.getTypeID()); //$NON-NLS-1$
													}
												}
											}
											else if (addMarker)
											{
												addMarker(project, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
											}
										}
										else if (foundPersist instanceof ScriptMethod && context != null)
										{
											ScriptMethod scriptMethod = (ScriptMethod)foundPersist;
											if (scriptMethod.getParent() != context && scriptMethod.isPrivate())
											{
												String elementName = null;
												String inForm = null;
												if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null)) elementName = ((ISupportName)o).getName();
												if (context instanceof Form) inForm = ((Form)context).getName();
												ServoyMarker mk;
												Pair<String, ProblemSeverity> problemPair;
												String prefix = ""; //$NON-NLS-1$
												if (scriptMethod.getScopeName() != null)
												{
													prefix = scriptMethod.getScopeName() + '.';
												}
												else if (scriptMethod.getParent() instanceof TableNode)
												{
													prefix = ((TableNode)scriptMethod.getParent()).getDataSource() + '/';
												}
												if (elementName == null)
												{
													if (inForm == null)
													{
														mk = MarkerMessages.PropertyTargetNotAccessible.fill(element.getName(), prefix + scriptMethod.getName());
														problemPair = SOLUTION_PROPERTY_TARGET_NOT_FOUND;
													}
													else
													{
														mk = MarkerMessages.PropertyInFormTargetNotAccessible.fill(element.getName(), inForm, prefix +
															scriptMethod.getName());
														problemPair = FORM_PROPERTY_TARGET_NOT_FOUND;
													}
												}
												else
												{
													if (inForm == null)
													{
														mk = MarkerMessages.PropertyOnElementTargetNotAccessible.fill(element.getName(), elementName, prefix +
															scriptMethod.getName());
														problemPair = SOLUTION_PROPERTY_TARGET_NOT_FOUND;
													}
													else
													{
														mk = MarkerMessages.PropertyOnElementInFormTargetNotAccessible.fill(element.getName(), elementName,
															inForm, prefix + scriptMethod.getName());
														problemPair = FORM_PROPERTY_TARGET_NOT_FOUND;
													}
												}
												addMarker(project, mk.getType(), mk.getText(), -1, problemPair, IMarker.PRIORITY_LOW, null, o);
											}
											else if (context instanceof Form)
											{
												Form parentForm = (Form)context;
												Form methodForm = (Form)scriptMethod.getAncestor(IRepository.FORMS);
												if (methodForm != null &&
													!ServoyBuilder.getPersistFlattenedSolution(parentForm, flattenedSolution).getFormHierarchy(parentForm).contains(
														methodForm))
												{
													ServoyMarker mk;
													if (!(o instanceof ISupportName) || o instanceof Form || ((ISupportName)o).getName() == null)
													{
														mk = MarkerMessages.FormPropertyMethodNotAccessible.fill(element.getName(), parentForm.getName(),
															methodForm.getName());
													}
													else
													{
														mk = MarkerMessages.FormPropertyOnElementMethodNotAccessible.fill(element.getName(),
															((ISupportName)o).getName(), parentForm.getName(), methodForm.getName());
													}
													addMarker(project, mk.getType(), mk.getText(), -1, FORM_PROPERTY_METHOD_NOT_ACCESIBLE,
														IMarker.PRIORITY_LOW, null, o);
												}
												else if (scriptMethod.isDeprecated())
												{
													ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
														"form " + parentForm.getName(), element.getName());
													addMarker(project, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
														IMarker.PRIORITY_NORMAL, null, o);
												}

											}
											else if (scriptMethod.isDeprecated())
											{
												ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
													"solution " + project.getName(), element.getName());
												addMarker(project, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
													IMarker.PRIORITY_NORMAL, null, o);
											}
										}
										else if (foundPersist instanceof Form)
										{
											if (!StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(element.getName()) &&
												!formCanBeInstantiated(((Form)foundPersist),
													ServoyBuilder.getPersistFlattenedSolution(foundPersist, flattenedSolution), formsAbstractChecked))
											{
												ServoyMarker mk = MarkerMessages.PropertyFormCannotBeInstantiated.fill(element.getName());
												addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED,
													IMarker.PRIORITY_LOW, null, o);
											}
										}
										else if (context instanceof Form && foundPersist instanceof ISupportEncapsulation)
										{
											addEncapsulationMarker(project, o, foundPersist, (Form)context);
										}
										if (BaseComponent.isEventProperty(element.getName()) && !skipEventMethod(element.getName()) &&
											(foundPersist instanceof ScriptMethod) && !methodsParsed.contains(foundPersist.getUUID()))
										{
											methodsParsed.add(foundPersist.getUUID());
											parseEventMethod(project, (ScriptMethod)foundPersist, element.getName());
										}
										if ((foundPersist instanceof ScriptMethod) &&
											(BaseComponent.isEventProperty(element.getName()) || BaseComponent.isCommandProperty(element.getName())))
										{
											if (methodsReferences.containsKey(foundPersist))
											{
												if (methodsReferences.get(foundPersist).booleanValue())
												{
													String elementName = "";
													ServoyMarker mk = null;
													Pair<String, ProblemSeverity> problemSeverity = null;
													if (o instanceof ISupportName)
													{
														elementName = ((ISupportName)o).getName();
														if (elementName == null || "".equals(elementName)) elementName = "<no name>";
														mk = MarkerMessages.PropertyMultipleMethodsOnSameElement.fill(elementName);
														problemSeverity = FORM_PROPERTY_MULTIPLE_METHODS_ON_SAME_ELEMENT;
													}
													else if (o instanceof TableNode)
													{
														elementName = ((TableNode)o).getTableName();
														mk = MarkerMessages.PropertyMultipleMethodsOnSameTable.fill(elementName);
														problemSeverity = PROPERTY_MULTIPLE_METHODS_ON_SAME_TABLE;
													}
													methodsReferences.put(foundPersist, Boolean.FALSE);
													addMarker(project, mk.getType(), mk.getText(), -1, problemSeverity, IMarker.PRIORITY_LOW, null, o);
												}
											}
											else
											{
												methodsReferences.put(foundPersist, Boolean.TRUE);
											}
											if (o instanceof AbstractBase)
											{
												Pair<List<Object>, List<Object>> instanceParameters = ((AbstractBase)o).getInstanceMethodParameters(element.getName());
												MethodArgument[] methodArguments = ((ScriptMethod)foundPersist).getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS);
												if (instanceParameters != null && instanceParameters.getRight() != null)
												{
													boolean signatureMismatch = false;
													if (instanceParameters.getLeft() != null)
													{
														// check for parameter name differences
														for (int i = 0; i < instanceParameters.getLeft().size(); i++)
														{
															String name = (String)instanceParameters.getLeft().get(i);
															if (i >= methodArguments.length)
															{
																signatureMismatch = true;
																break;
															}
															else if (!name.equals(methodArguments[i].getName()))
															{
																signatureMismatch = true;
																break;
															}
														}
													}
													if (instanceParameters.getRight().size() > methodArguments.length)
													{
														signatureMismatch = true;
													}
													// add marker if signature mismach
													if (signatureMismatch)
													{
														String handlerName = element.getName().substring(
															0,
															(element.getName().indexOf("MethodID") > 0 ? element.getName().indexOf("MethodID")
																: element.getName().length()));

														String functionDefinitionName = ((ScriptMethod)foundPersist).getName();
														if (((ScriptMethod)foundPersist).getScopeName() != null)
														{
															functionDefinitionName = ((ScriptMethod)foundPersist).getScopeName() + "." + functionDefinitionName;
														}
														else
														{
															functionDefinitionName = "forms." +
																((ISupportName)((ScriptMethod)foundPersist).getParent()).getName() + "." +
																functionDefinitionName;
														}

														String componentName = "";
														if (o instanceof ISupportName && ((ISupportName)o).getName() != null &&
															((ISupportName)o).getName().length() > 1)
														{
															componentName = " \"" + ((ISupportName)o).getName() + "\"";
														}
														ServoyMarker mk = MarkerMessages.EventHandlerSignatureMismatch.fill(functionDefinitionName,
															handlerName, RepositoryHelper.getObjectTypeName(o.getTypeID()), componentName);
														addMarker(project, mk.getType(), mk.getText(), -1, METHOD_NUMBER_OF_ARGUMENTS_MISMATCH,
															IMarker.PRIORITY_LOW, null, o);
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
							throw new RuntimeException(e);
						}
						checkCancel();
						addMissingServer(o, missingServers, goodServers);
						checkCancel();
						if (o instanceof ValueList && !missingServers.containsKey(((ValueList)o).getServerName()))
						{
							ValueList vl = (ValueList)o;
							addMarkers(
								project,
								checkValuelist(vl, ServoyBuilder.getPersistFlattenedSolution(vl, flattenedSolution),
									ApplicationServerSingleton.get().getServerManager(), false), vl);
						}
						checkCancel();
						if (o instanceof Media)
						{
							Media oMedia = (Media)o;
							if (oMedia.getName().toLowerCase().endsWith(".tiff") || oMedia.getName().toLowerCase().endsWith(".tif")) //$NON-NLS-1$ //$NON-NLS-2$
							{
								Pair<String, String> path = SolutionSerializer.getFilePath(oMedia, false);
								ServoyMarker mk = MarkerMessages.MediaTIFF.fill(path.getLeft() + path.getRight());
								addMarker(project, mk.getType(), mk.getText(), -1, MEDIA_TIFF, IMarker.PRIORITY_NORMAL, null, oMedia);
							}
						}
						checkCancel();
						if (o instanceof ISupportDataProviderID)
						{
							try
							{
								String id = ((ISupportDataProviderID)o).getDataProviderID();
								if (id != null && !"".equals(id)) //$NON-NLS-1$
								{
									if (!(context instanceof Form))
									{
										ServoyLog.logError("Could not find parent form for element " + o, null); //$NON-NLS-1$
									}
									else
									{
										Form parentForm = (Form)context;
										if (!missingServers.containsKey(parentForm.getServerName()))
										{
											FlattenedSolution persistFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution);
											IDataProvider dataProvider = persistFlattenedSolution.getDataProviderForTable(parentForm.getTable(), id);
											if (dataProvider == null)
											{
												Form flattenedForm = persistFlattenedSolution.getFlattenedForm(o);
												if (flattenedForm != null)
												{
													dataProvider = flattenedForm.getScriptVariable(id);
												}
											}
											if (dataProvider == null)
											{
												try
												{
													dataProvider = persistFlattenedSolution.getGlobalDataProvider(id, true);
												}
												catch (Exception e)
												{
													exceptionCount++;
													if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
												}
											}

											String elementName = null;
											String inForm = null;
											if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
											{
												elementName = ((ISupportName)o).getName();
											}
											inForm = parentForm.getName();

											if ((o instanceof Field || o instanceof GraphicalComponent) && dataProvider != null)
											{
												String format = (o instanceof Field) ? ((Field)o).getFormat() : ((GraphicalComponent)o).getFormat();
												if (o instanceof Field && ((Field)o).getDisplayType() != Field.TEXT_FIELD &&
													((Field)o).getDisplayType() != Field.TYPE_AHEAD && ((Field)o).getDisplayType() != Field.CALENDAR)
												{
													format = null;
												}
												if (format != null && format.length() > 0)
												{
													ParsedFormat parsedFormat = FormatParser.parseFormatProperty(format);
													int dataType = getDataType(project, dataProvider, parsedFormat, o);
													if (parsedFormat.getDisplayFormat() != null && !parsedFormat.getDisplayFormat().startsWith("i18n:")) //$NON-NLS-1$
													{
														try
														{
															if (dataType == IColumnTypes.DATETIME)
															{
																new SimpleDateFormat(parsedFormat.getDisplayFormat());
																if (parsedFormat.getEditFormat() != null) new SimpleDateFormat(parsedFormat.getEditFormat());
															}
															else if (dataType == IColumnTypes.INTEGER || dataType == IColumnTypes.NUMBER)
															{
																new DecimalFormat(parsedFormat.getDisplayFormat(),
																	RoundHalfUpDecimalFormat.getDecimalFormatSymbols(Locale.getDefault()));
																if (parsedFormat.getEditFormat() != null) new DecimalFormat(parsedFormat.getEditFormat(),
																	RoundHalfUpDecimalFormat.getDecimalFormatSymbols(Locale.getDefault()));
															}
														}
														catch (Exception ex)
														{
															Debug.trace(ex);

															ServoyMarker mk;
															if (elementName == null)
															{
																mk = MarkerMessages.FormFormatInvalid.fill(inForm, parsedFormat.getFormatString());
															}
															else
															{
																mk = MarkerMessages.FormFormatOnElementInvalid.fill(elementName, inForm,
																	parsedFormat.getFormatString());
															}
															addMarker(project, mk.getType(), mk.getText(), -1, FORM_FORMAT_INVALID, IMarker.PRIORITY_NORMAL,
																null, o);
														}
													}
												}
											}
											if (o instanceof Field &&
												(((Field)o).getDisplayType() == Field.TYPE_AHEAD || ((Field)o).getDisplayType() == Field.TEXT_FIELD) &&
												((Field)o).getValuelistID() > 0 && ((Field)o).getFormat() != null)
											{
												boolean showWarning = false;
												ValueList vl = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution).getValueList(
													((Field)o).getValuelistID());
												if (vl != null && vl.getValueListType() == IValueListConstants.CUSTOM_VALUES && vl.getCustomValues() != null &&
													(vl.getCustomValues() == null || vl.getCustomValues().contains("|")))
												{
													showWarning = true;
												}
												if (vl != null && vl.getValueListType() == IValueListConstants.DATABASE_VALUES &&
													vl.getReturnDataProviders() != vl.getShowDataProviders())
												{
													showWarning = true;
												}
												if (showWarning)
												{
													ServoyMarker mk;
													if (elementName == null)
													{
														mk = MarkerMessages.FormFormatIncompatible.fill(inForm);
													}
													else
													{
														mk = MarkerMessages.FormFormatOnElementIncompatible.fill(elementName, inForm);
													}
													addMarker(project, mk.getType(), mk.getText(), -1, FORM_FORMAT_INVALID, IMarker.PRIORITY_NORMAL, null, o);
												}

											}
											if (o instanceof Field && dataProvider != null)
											{
												Field field = (Field)o;
												if (field.getEditable() &&
													(field.getDisplayType() == Field.HTML_AREA || field.getDisplayType() == Field.RTF_AREA) &&
													dataProvider.getColumnWrapper() != null && dataProvider.getColumnWrapper().getColumn() instanceof Column)
												{
													Column column = (Column)dataProvider.getColumnWrapper().getColumn();
													if (column.getLength() < MIN_FIELD_LENGTH && column.getLength() > 0)
													{
														ServoyMarker mk = MarkerMessages.FormColumnLengthTooSmall.fill(elementName != null ? elementName : "",
															inForm);
														addMarker(project, mk.getType(), mk.getText(), -1, FORM_COLUMN_LENGTH_TOO_SMALL,
															IMarker.PRIORITY_NORMAL, null, o);
													}
												}
												if (((dataProvider instanceof ScriptVariable &&
													((ScriptVariable)dataProvider).getVariableType() == IColumnTypes.MEDIA && ((ScriptVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
													(dataProvider instanceof AggregateVariable &&
														((AggregateVariable)dataProvider).getType() == IColumnTypes.MEDIA && ((AggregateVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
													(dataProvider instanceof ScriptCalculation &&
														((ScriptCalculation)dataProvider).getType() == IColumnTypes.MEDIA && ((ScriptCalculation)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) || (dataProvider instanceof Column && Column.mapToDefaultType(((Column)dataProvider).getType()) == IColumnTypes.MEDIA) &&
													((Column)dataProvider).getColumnInfo() != null &&
													((Column)dataProvider).getColumnInfo().getConverterName() == null) &&
													field.getDisplayType() != Field.IMAGE_MEDIA)
												{
													ServoyMarker mk = MarkerMessages.FormIncompatibleElementType.fill(
														elementName != null ? elementName : field.getUUID(), inForm);
													addMarker(project, mk.getType(), mk.getText(), -1, FORM_INCOMPATIBLE_ELEMENT_TYPE, IMarker.PRIORITY_NORMAL,
														null, o);
												}
												if (dataProvider instanceof ScriptVariable && ((ScriptVariable)dataProvider).isDeprecated())
												{
													ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedVariable.fill(
														((ScriptVariable)dataProvider).getName(), "form " + inForm, "dataProvider");
													addMarker(project, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
														IMarker.PRIORITY_NORMAL, null, o);
												}
												else if (dataProvider instanceof ScriptCalculation && ((ScriptCalculation)dataProvider).isDeprecated())
												{
													ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
														((ScriptCalculation)dataProvider).getName(), "form " + inForm, "dataProvider");
													addMarker(project, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
														IMarker.PRIORITY_NORMAL, null, o);
												}
											}

											if (dataProvider == null && (parentForm.getDataSource() != null || ScopesUtils.isVariableScope(id)))
											{
												ServoyMarker mk;
												if (elementName == null)
												{
													mk = MarkerMessages.FormDataproviderNotFound.fill(inForm, id);
												}
												else
												{
													mk = MarkerMessages.FormDataproviderOnElementNotFound.fill(elementName, inForm, id);
												}
												addMarker(project, mk.getType(), mk.getText(), -1, FORM_INVALID_DATAPROVIDER, IMarker.PRIORITY_LOW, null, o);
											}
											if (parentForm.getDataSource() != null && dataProvider instanceof ColumnWrapper)
											{
												Relation[] relations = ((ColumnWrapper)dataProvider).getRelations();
												if (relations != null && !relations[0].isGlobal() &&
													!parentForm.getDataSource().equals(relations[0].getPrimaryDataSource()))
												{
													ServoyMarker mk;
													if (elementName == null)
													{
														mk = MarkerMessages.FormDataproviderNotBasedOnFormTable.fill(inForm, id);
													}
													else
													{
														mk = MarkerMessages.FormDataproviderOnElementNotBasedOnFormTable.fill(elementName, inForm, id);
													}
													addMarker(project, mk.getType(), mk.getText(), -1, FORM_INVALID_DATAPROVIDER, IMarker.PRIORITY_LOW, null, o);
												}
											}
											if (dataProvider instanceof AggregateVariable && o instanceof Field && ((Field)o).getEditable())
											{
												ServoyMarker mk;
												if (elementName == null)
												{
													mk = MarkerMessages.FormDataproviderAggregateNotEditable.fill(inForm, id);
												}
												else
												{
													mk = MarkerMessages.FormDataproviderOnElementAggregateNotEditable.fill(elementName, inForm, id);
												}
												addMarker(project, mk.getType(), mk.getText(), -1, FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE,
													IMarker.PRIORITY_LOW, null, o);
											}
											if (dataProvider != null && dataProvider instanceof Column && ((Column)dataProvider).getColumnInfo() != null)
											{
												if (((Column)dataProvider).getColumnInfo().isExcluded())
												{
													ServoyMarker mk;
													if (elementName == null)
													{
														mk = MarkerMessages.FormDataproviderNotFound.fill(inForm, id);
													}
													else
													{
														mk = MarkerMessages.FormDataproviderOnElementNotFound.fill(elementName, inForm, id);
													}
													addMarker(project, mk.getType(), mk.getText(), -1, FORM_INVALID_DATAPROVIDER, IMarker.PRIORITY_LOW, null, o);
												}
											}
										}
									}
								}
							}
							catch (Exception e)
							{
								exceptionCount++;
								if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
							}
						}
						checkCancel();
						if (o instanceof IFormElement)
						{
							String name = ((ISupportName)o).getName();
							if (name != null && name.startsWith(ComponentFactory.WEB_ID_PREFIX))
							{
								ServoyMarker mk = MarkerMessages.ElementNameReservedPrefixIdentifier.fill(name, ComponentFactory.WEB_ID_PREFIX);
								addMarker(project, mk.getType(), mk.getText(), -1, SOLUTION_ELEMENT_NAME_RESERVED_PREFIX_IDENTIFIER, IMarker.PRIORITY_NORMAL,
									null, o);
							}
						}
						if (o instanceof BaseComponent && ((BaseComponent)o).getVisible())
						{
							// check if not outside form
							Form form = (Form)o.getAncestor(IRepository.FORMS);
							form = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution).getFlattenedForm(form);
							if (form != null && form.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) == null)
							{
								Point location = ((BaseComponent)o).getLocation();
								if (location != null)
								{
									boolean outsideForm = false;
									Iterator<com.servoy.j2db.persistence.Part> parts = form.getParts();
									while (parts.hasNext())
									{
										com.servoy.j2db.persistence.Part part = parts.next();
										int startPos = form.getPartStartYPos(part.getID());
										int endPos = part.getHeight();
										if (startPos <= location.y && endPos > location.y)
										{
											// found the part
											int height = ((BaseComponent)o).getSize().height;
											if (location.y + height > endPos)
											{
												String elementName = null;
												String inForm = null;
												String partName = com.servoy.j2db.persistence.Part.getDisplayName(part.getPartType());
												if (o instanceof ISupportName && ((ISupportName)o).getName() != null) elementName = ((ISupportName)o).getName();
												inForm = form.getName();
												if (elementName == null)
												{
													elementName = o.getUUID().toString();
												}
												ServoyMarker mk = MarkerMessages.FormNamedElementOutsideBoundsOfPart.fill(elementName, inForm, partName);
												addMarker(project, mk.getType(), mk.getText(), -1, FORM_ELEMENT_OUTSIDE_BOUNDS, IMarker.PRIORITY_LOW, null, o);
											}
											int width = form.getWidth();
											if (PersistUtils.isHeaderPart(part.getPartType()) || PersistUtils.isFooterPart(part.getPartType()))
											{
												String defaultPageFormat = form.getDefaultPageFormat();
												PageFormat currentPageFormat = null;
												try
												{
													currentPageFormat = PersistHelper.createPageFormat(defaultPageFormat);
													if (currentPageFormat == null)
													{
														defaultPageFormat = Settings.getInstance().getProperty("pageformat");
														currentPageFormat = PersistHelper.createPageFormat(defaultPageFormat);
													}
													if (currentPageFormat == null)
													{
														currentPageFormat = new PageFormat();
													}
												}
												catch (NoSuchElementException e)
												{
													ServoyLog.logWarning("Could not parse page format '" + defaultPageFormat + '\'', null);
												}
												if (currentPageFormat != null)
												{
													int w = (int)(currentPageFormat.getImageableWidth() * (form.getPaperPrintScale() / 100d));
													if (width < w)
													{
														width = w;
													}
												}
											}
											if (width < location.x + ((BaseComponent)o).getSize().width)
											{
												outsideForm = true;
											}
											break;
										}
									}
									if (location.y > form.getSize().height && form.getParts().hasNext())
									{
										outsideForm = true;
									}
									if (outsideForm)
									{
										String elementName = null;
										if (o instanceof ISupportName && ((ISupportName)o).getName() != null) elementName = ((ISupportName)o).getName();
										ServoyMarker mk;
										if (elementName == null)
										{
											elementName = o.getUUID().toString();
										}
										mk = MarkerMessages.FormNamedElementOutsideBoundsOfForm.fill(elementName, form.getName());
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_ELEMENT_OUTSIDE_BOUNDS, IMarker.PRIORITY_LOW, null, o);
									}
								}
							}
						}
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
								Table table = null;
								try
								{
									table = node.getTable();
								}
								catch (Exception e)
								{
								}
								if (table == null || table.isMarkedAsHiddenInDeveloper())
								{
									Iterator<IPersist> iterator = node.getAllObjects();
									while (iterator.hasNext())
									{
										IPersist persist = iterator.next();
										String what;
										if (persist instanceof AggregateVariable) what = "Aggregation"; //$NON-NLS-1$
										else if (persist instanceof ScriptCalculation) what = "Calculation"; //$NON-NLS-1$
										else what = "Function"; //$NON-NLS-1$
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
										if (mk != null) addMarker(project, mk.getType(), mk.getText(), -1, problemSeverity, table != null
											? IMarker.PRIORITY_LOW : IMarker.PRIORITY_NORMAL, null, persist);
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Form)
						{
							Form form = (Form)o;
							FlattenedSolution formFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(form, flattenedSolution);
							Table table = null;
							String path = form.getSerializableRuntimeProperty(IScriptProvider.FILENAME);
							if (path != null && !path.endsWith(SolutionSerializer.getFileName(form, false)))
							{
								ServoyMarker mk = MarkerMessages.FormFileNameInconsistent.fill(form.getName(), path);
								addMarker(project, mk.getType(), mk.getText(), -1, FORM_FILE_NAME_INCONSISTENT, IMarker.PRIORITY_NORMAL, null, form);
							}
							if (!missingServers.containsKey(form.getServerName()))
							{
								try
								{
									table = form.getTable();
									if (table != null && !table.getExistInDB())
									{
										// the table was probably deleted - update the form table as well 
										form.clearTable();
										table = form.getTable();
									}

									if (table == null && form.getDataSource() != null)
									{
										ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH, null, form);
									}
									else if (table != null && table.getRowIdentColumnsCount() == 0)
									{
										ServoyMarker mk = MarkerMessages.FormTableNoPK.fill(form.getName(), form.getTableName());
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH, null, form);
									}
									else if (table != null && form.getInitialSort() != null)
									{
										addMarkers(project, checkSortOptions(table, form.getInitialSort(), form, formFlattenedSolution), form);
									}
									if (table != null && table.isMarkedAsHiddenInDeveloper())
									{
										ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), "form ", form.getName());
										addMarker(project, mk.getType(), mk.getText(), -1, INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW, null, form);
									}
								}
								catch (Exception e)
								{
									exceptionCount++;
									if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(e);
									ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
									addMarker(project, mk.getType(), mk.getText(), -1, FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH, null, form);
								}
							}

							// if form uses global relation named foundset, check to see that it is valid
							String namedFoundset = form.getNamedFoundSet();
							if (namedFoundset != null && !namedFoundset.equals(Form.NAMED_FOUNDSET_EMPTY) &&
								!namedFoundset.equals(Form.NAMED_FOUNDSET_SEPARATE))
							{
								// it must be a global relation then
								Relation r = formFlattenedSolution.getRelation(form.getGlobalRelationNamedFoundset());
								if (r == null)
								{
									// what is this then?
									ServoyMarker mk = MarkerMessages.PropertyInFormTargetNotFound.fill("namedFoundset", form.getName());
									addMarker(project, mk.getType(), mk.getText(), -1, FORM_PROPERTY_TARGET_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, form);
								}
								else if (!r.isGlobal() ||
									!Solution.areDataSourcesCompatible(solution.getRepository(), form.getDataSource(), r.getForeignDataSource()))
								{
									// wrong kind of relation
									ServoyMarker mk = MarkerMessages.FormNamedFoundsetIncorrectValue.fill(form.getName(),
										" The relation must be global and it's foreign data source must match the form's datasource.");
									addMarker(project, mk.getType(), mk.getText(), -1, FORM_FOUNDSET_INCORRECT_VALUE, IMarker.PRIORITY_NORMAL, null, form);
								}
							}

							try
							{
								if (table != null)
								{
									Iterator<ScriptVariable> iterator = form.getScriptVariables(false);
									while (iterator.hasNext())
									{
										ScriptVariable var = iterator.next();
										if (table.getColumn(var.getName()) != null)
										{
											ServoyMarker mk = MarkerMessages.FormVariableTableCol.fill(form.getName(), var.getName(), form.getTableName());
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_VARIABLE_TYPE_COL, IMarker.PRIORITY_NORMAL, null, var);
										}
									}
								}
							}
							catch (Exception ex)
							{
								exceptionCount++;
								if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
							}

							if (form.getExtendsID() > 0 && formFlattenedSolution != null)
							{
								Form superForm = formFlattenedSolution.getForm(form.getExtendsID());
								if (superForm != null)
								{
									if (form.getDataSource() != null && superForm.getDataSource() != null &&
										!form.getDataSource().equals(superForm.getDataSource()))
									{
										ServoyMarker mk = MarkerMessages.FormDerivedFormDifferentTable.fill(form.getName(), superForm.getName());
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_DERIVED_FORM_DIFFERENT_TABLE, IMarker.PRIORITY_NORMAL, null,
											form);
									}

									List<Integer> forms = new ArrayList<Integer>();
									forms.add(Integer.valueOf(form.getID()));
									forms.add(Integer.valueOf(superForm.getID()));
									while (superForm != null)
									{
										if (superForm.getExtendsID() > 0)
										{
											superForm = formFlattenedSolution.getForm(superForm.getExtendsID());
											if (superForm != null)
											{
												if (forms.contains(Integer.valueOf(superForm.getID())))
												{
													// a cycle detected
													ServoyMarker mk = MarkerMessages.FormExtendsCycle.fill(form.getName());
													addMarker(project, mk.getType(), mk.getText(), -1, FORM_EXTENDS_CYCLE, IMarker.PRIORITY_LOW, null, form);
													break;
												}
												else
												{
													forms.add(Integer.valueOf(superForm.getID()));
												}
											}
										}
										else
										{
											superForm = null;
										}
									}
								}
								else
								{
									//superForm not found
									ServoyMarker mk = MarkerMessages.FormExtendsFormElementNotFound.fill(form.getName());
									addMarker(project, mk.getType(), mk.getText(), -1, FORM_EXTENDS_FORM_ELEMENT_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, form);
								}

							}
							// check for duplicate parts
							Map<Integer, Boolean> parts = new HashMap<Integer, Boolean>();
							Iterator<com.servoy.j2db.persistence.Part> it = form.getParts();
							while (it.hasNext())
							{
								com.servoy.j2db.persistence.Part part = it.next();
								if (!PersistHelper.isOverrideElement(part))
								{
									if (!part.canBeMoved() && parts.containsKey(Integer.valueOf(part.getPartType())))
									{
										if (parts.get(Integer.valueOf(part.getPartType())) != null &&
											parts.get(Integer.valueOf(part.getPartType())).booleanValue())
										{
											ServoyMarker mk = MarkerMessages.FormDuplicatePart.fill(form.getName(),
												com.servoy.j2db.persistence.Part.getDisplayName(part.getPartType()));
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_DUPLICATE_PART, IMarker.PRIORITY_NORMAL, null, part);
											parts.put(Integer.valueOf(part.getPartType()), Boolean.FALSE);
										}
									}
									else
									{
										parts.put(Integer.valueOf(part.getPartType()), Boolean.TRUE);
									}
								}
							}

							// check to see if there are too many portals/tab panels for an acceptable slow WAN SC deployment
							int portalAndTabPanelCount = 0;
							// check to see if there are too many columns in a table view form (that could result in poor WC performance when selecting rows for example)
							int fieldCount = 0;
							boolean isTableView = (form.getView() == FormController.LOCKED_TABLE_VIEW || form.getView() == FormController.TABLE_VIEW);
							// also check tab sequences
							Map<Integer, Boolean> tabSequences = new HashMap<Integer, Boolean>();
							Iterator<IPersist> iterator = form.getAllObjects();
							while (iterator.hasNext())
							{
								IPersist persist = iterator.next();
								if (persist.getTypeID() == IRepository.TABPANELS || persist.getTypeID() == IRepository.PORTALS) portalAndTabPanelCount++;
								else if (isTableView &&
									persist instanceof IFormElement &&
									(persist.getTypeID() == IRepository.FIELDS ||
										(persist.getTypeID() == IRepository.GRAPHICALCOMPONENTS && ((GraphicalComponent)persist).getLabelFor() == null) ||
										persist.getTypeID() == IRepository.BEANS || persist.getTypeID() == IRepository.SHAPES) &&
									form.getPartAt(((IFormElement)persist).getLocation().y) != null &&
									form.getPartAt(((IFormElement)persist).getLocation().y).getPartType() == Part.BODY) fieldCount++;

								if (persist instanceof ISupportTabSeq && ((ISupportTabSeq)persist).getTabSeq() > 0)
								{
									int tabSeq = ((ISupportTabSeq)persist).getTabSeq();
									if (tabSequences.containsKey(Integer.valueOf(tabSeq)))
									{
										String name = persist.getUUID().toString();
										if (persist instanceof ISupportName && ((ISupportName)persist).getName() != null)
										{
											name = ((ISupportName)persist).getName();
										}
										ServoyMarker mk = MarkerMessages.FormNamedElementDuplicateTabSequence.fill(form.getName(), name);
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_ELEMENT_DUPLICATE_TAB_SEQUENCE, IMarker.PRIORITY_NORMAL, null,
											persist);
									}
									else
									{
										tabSequences.put(Integer.valueOf(tabSeq), null);
									}
								}
							}
							if (portalAndTabPanelCount > LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM)
							{
								ServoyMarker mk = MarkerMessages.FormHasTooManyThingsAndProbablyLowPerformance.fill(
									String.valueOf(LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM), "portals/tab panels", ""); //$NON-NLS-1$ //$NON-NLS-2$
								addMarker(project, mk.getType(), mk.getText(), -1, LEVEL_PERFORMANCE_TABS_PORTALS, IMarker.PRIORITY_NORMAL, null, form);
							}
							if (fieldCount > LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM)
							{
								ServoyMarker mk = MarkerMessages.FormHasTooManyThingsAndProbablyLowPerformance.fill(
									String.valueOf(LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM), "columns", " table view"); //$NON-NLS-1$ //$NON-NLS-2$
								addMarker(project, mk.getType(), mk.getText(), -1, LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW, IMarker.PRIORITY_NORMAL, null, form);
							}

							if (form.getRowBGColorCalculation() != null)
							{
								ScriptMethod scriptMethod = null;
								boolean unresolved = true;
								Pair<String, String> scope = ScopesUtils.getVariableScope(form.getRowBGColorCalculation());
								if (scope.getLeft() != null)
								{
									scriptMethod = formFlattenedSolution.getScriptMethod(scope.getLeft(), scope.getRight());
								}
								if (scriptMethod == null)
								{
									if (table != null)
									{
										Iterator<TableNode> tableNodes = null;
										try
										{
											tableNodes = formFlattenedSolution.getTableNodes(table);
										}
										catch (RepositoryException e)
										{
											ServoyLog.logError(e);
										}
										if (tableNodes != null)
										{
											while (tableNodes.hasNext())
											{
												ScriptCalculation calc = AbstractBase.selectByName(tableNodes.next().getScriptCalculations(),
													form.getRowBGColorCalculation());
												if (calc != null)
												{
													unresolved = false;
													break;
												}
											}
										}
									}
								}
								else
								{
									unresolved = false;
								}
								if (unresolved)
								{
									ServoyMarker mk = MarkerMessages.FormRowBGCalcTargetNotFound.fill(form.getName());
									addMarker(project, mk.getType(), mk.getText(), -1, FORM_PROPERTY_TARGET_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, form);
								}
							}
						}
						checkCancel();
						if (o instanceof ScriptCalculation)
						{
							ScriptCalculation calc = (ScriptCalculation)o;
							if (calc.getMethodCode() != null)
							{
								String text = calc.getMethodCode().toLowerCase();
								if (text.contains("forms.")) //$NON-NLS-1$
								{
									String[] s = text.split("forms."); //$NON-NLS-1$
									for (int i = 0; i < s.length - 1; i++)
									{
										if (s[i] == null || s[i].length() == 0 || Character.isWhitespace(s[i].charAt(s[i].length() - 1)))
										{
											Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, true);
											Path path = new Path(pathPair.getLeft() + pathPair.getRight());
											ServoyMarker mk = MarkerMessages.CalculationFormAccess.fill(calc.getName());
											try
											{
												Table table = calc.getTable();
												if (table != null)
												{
													mk = MarkerMessages.CalculationInTableFormAccess.fill(calc.getName(), table.getName());
												}
												addMarker(project, mk.getType(), mk.getText(), -1, CALCULATION_FORM_ACCESS, IMarker.PRIORITY_NORMAL,
													path.toString(), calc);
											}
											catch (RepositoryException e)
											{
												Debug.log("table not found for calc: " + calc, e); //$NON-NLS-1$
											}
											break;
										}
									}

								}
							}

						}
						checkCancel();
						if (o instanceof Tab)
						{
							Tab tab = (Tab)o;
							FlattenedSolution tabFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(tab, flattenedSolution);
							if (tab.getRelationName() != null)
							{
								Relation[] relations = tabFlattenedSolution.getRelationSequence(tab.getRelationName());
								if (relations == null)
								{
									if (Utils.getAsUUID(tab.getRelationName(), false) != null)
									{
										// relation name was not resolved from uuid to relation name during import
										ServoyMarker mk = MarkerMessages.FormRelatedTabUnsolvedUuid.fill(tab.getRelationName());
										IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, FORM_RELATED_TAB_UNSOLVED_UUID,
											IMarker.PRIORITY_NORMAL, null, tab);
										if (marker != null)
										{
											try
											{
												marker.setAttribute("Uuid", o.getUUID().toString()); //$NON-NLS-1$
												marker.setAttribute("SolutionName", ((Solution)tab.getAncestor(IRepository.SOLUTIONS)).getName()); //$NON-NLS-1$
												marker.setAttribute("PropertyName", "relationName"); //$NON-NLS-1$ //$NON-NLS-2$
												marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("relationName", o.getClass())); //$NON-NLS-1$ //$NON-NLS-2$
											}
											catch (Exception e)
											{
												ServoyLog.logError(e);
											}
										}
									}
									else
									{
										ServoyMarker mk = MarkerMessages.FormRelatedTabUnsolvedRelation.fill(tab.getRelationName());
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_RELATED_TAB_UNSOLVED_RELATION, IMarker.PRIORITY_NORMAL, null,
											tab);
									}
								}
								else
								{
									Relation relation = relations[0];
									if (!relation.isGlobal() && relation.getPrimaryServerName() != null && relation.getPrimaryTableName() != null)
									{
										if (context instanceof Form && (!relation.getPrimaryDataSource().equals(((Form)context).getDataSource())))
										{
											ServoyMarker mk = MarkerMessages.FormRelatedTabDifferentTable.fill(((Form)context).getName(), relation.getName());
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_RELATED_TAB_DIFFERENT_TABLE, IMarker.PRIORITY_NORMAL, null,
												tab);
										}
									}
									relation = relations[relations.length - 1];
									if (!relation.isGlobal() && relation.getPrimaryServerName() != null && relation.getPrimaryTableName() != null)
									{
										Form form = tabFlattenedSolution.getForm(tab.getContainsFormID());
										if (form != null &&
											(!relation.getForeignServerName().equals(form.getServerName()) || !relation.getForeignTableName().equals(
												form.getTableName())))
										{
											ServoyMarker mk = MarkerMessages.FormRelatedTabDifferentTable.fill(form.getName(), relation.getName());
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_RELATED_TAB_DIFFERENT_TABLE, IMarker.PRIORITY_NORMAL, null,
												tab);
										}
									}
									if (context instanceof Form)
									{
										for (Relation r : relations)
										{
											addEncapsulationMarker(project, tab, r, (Form)context);
										}
									}
								}
							}
							if (tab.getImageMediaID() > 0)
							{
								Media media = tabFlattenedSolution.getMedia(tab.getImageMediaID());
								if (media != null)
								{
									ImageIcon mediaImageIcon = new ImageIcon(media.getMediaData());
									if (mediaImageIcon.getIconWidth() > 20 || mediaImageIcon.getIconHeight() > 20)
									{
										String formName = ((Form)tab.getAncestor(IRepository.FORMS)).getName();
										String tabPanelName = ((TabPanel)tab.getAncestor(IRepository.TABPANELS)).getName();
										String tabName = tab.getName();

										ServoyMarker mk = MarkerMessages.FormTabPanelTabImageTooLarge.fill(tabName != null ? tabName : "", tabPanelName != null //$NON-NLS-1$
											? tabPanelName : "", formName != null ? formName : ""); //$NON-NLS-1$ //$NON-NLS-2$
										addMarker(project, mk.getType(), mk.getText(), -1, FORM_TABPANEL_TAB_IMAGE_TOO_LARGE, IMarker.PRIORITY_NORMAL, null,
											tab);
									}
								}
							}
						}
						checkCancel();
						if (o instanceof Field)
						{
							Field field = (Field)o;
							FlattenedSolution fieldFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(field, flattenedSolution);
							int type = field.getDisplayType();
							String fieldName = field.getName();
							if (fieldName == null)
							{
								fieldName = field.getUUID().toString();
							}
							if (field.getValuelistID() > 0)
							{
								ValueList vl = fieldFlattenedSolution.getValueList(field.getValuelistID());
								if (vl != null)
								{
									if (type == Field.COMBOBOX && field.getEditable() && !SolutionMetaData.isServoyMobileSolution(servoyProject.getSolution()))
									{

										boolean showWarning = false;
										if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES &&
											vl.getReturnDataProviders() != vl.getShowDataProviders())
										{
											showWarning = true;
										}
										if (vl.getValueListType() == IValueListConstants.CUSTOM_VALUES && vl.getCustomValues() != null &&
											vl.getCustomValues().contains("|"))
										{
											showWarning = true;
										}
										if (showWarning)
										{
											ServoyMarker mk = MarkerMessages.FormEditableNamedComboboxCustomValuelist.fill(fieldName);
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_EDITABLE_COMBOBOX_CUSTOM_VALUELIST,
												IMarker.PRIORITY_NORMAL, null, field);
										}
									}
									if (type == Field.TEXT_FIELD || type == Field.TYPE_AHEAD)
									{
										boolean vlWithUnstoredCalcError = false;
										if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES)
										{
											Table table = null;
											try
											{
												if (vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
												{
													table = (Table)vl.getTable();
												}
												else if (vl.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES && vl.getRelationName() != null)
												{
													Relation[] relations = fieldFlattenedSolution.getRelationSequence(vl.getRelationName());
													if (relations != null)
													{
														table = (Table)DataSourceUtils.getTable(relations[relations.length - 1].getForeignDataSource(),
															solution, null); // we are not using directly .getForeignTable() because that one throws an exception if the table is not present
													}
												}
											}
											catch (Exception e)
											{
												ServoyLog.logError(e);
											}
											vlWithUnstoredCalcError =
											/**/isUnstoredCalc(vl.getDataProviderID1(), table, fieldFlattenedSolution) || //
												isUnstoredCalc(vl.getDataProviderID2(), table, fieldFlattenedSolution) || //
												isUnstoredCalc(vl.getDataProviderID3(), table, fieldFlattenedSolution);
										}
										if (!vlWithUnstoredCalcError && vl.getFallbackValueListID() > 0)
										{
											ValueList fallback = fieldFlattenedSolution.getValueList(vl.getFallbackValueListID());
											if (fallback != null && fallback.getValueListType() == IValueListConstants.DATABASE_VALUES)
											{
												Table table = null;
												try
												{
													if (fallback.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
													{
														table = (Table)fallback.getTable();
													}
													else if (fallback.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES &&
														fallback.getRelationName() != null)
													{
														Relation[] relations = fieldFlattenedSolution.getRelationSequence(fallback.getRelationName());
														if (relations != null)
														{
															table = relations[relations.length - 1].getForeignTable();
														}
													}
												}
												catch (Exception e)
												{
													ServoyLog.logError(e);
												}
												vlWithUnstoredCalcError =
												/**/isUnstoredCalc(fallback.getDataProviderID1(), table, fieldFlattenedSolution) || //
													isUnstoredCalc(fallback.getDataProviderID2(), table, fieldFlattenedSolution) || //
													isUnstoredCalc(fallback.getDataProviderID3(), table, fieldFlattenedSolution);
											}
										}
										if (vlWithUnstoredCalcError)
										{
											ServoyMarker mk = MarkerMessages.FormTypeAheadNamedUnstoredCalculation.fill(fieldName);
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_TYPEAHEAD_UNSTORED_CALCULATION, IMarker.PRIORITY_NORMAL,
												null, field);
										}
									}
									if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES && vl.getRelationName() != null)
									{
										Form form = (Form)o.getAncestor(IRepository.FORMS);
										String[] parts = vl.getRelationName().split("\\."); //$NON-NLS-1$
										Relation relation = fieldFlattenedSolution.getRelation(parts[0]);
										if (relation != null && !relation.isGlobal() && !relation.getPrimaryDataSource().equals(form.getDataSource()))
										{
											ServoyMarker mk = MarkerMessages.FormNamedFieldRelatedValuelist.fill(fieldName, vl.getName(), form.getName());
											addMarker(project, mk.getType(), mk.getText(), -1, FORM_FIELD_RELATED_VALUELIST, IMarker.PRIORITY_NORMAL, null,
												field);
										}
									}
									if (vl.getFallbackValueListID() > 0)
									{
										ValueList fallback = fieldFlattenedSolution.getValueList(vl.getFallbackValueListID());
										if (fallback != null && fallback.getValueListType() == IValueListConstants.DATABASE_VALUES &&
											fallback.getRelationName() != null)
										{
											Form form = (Form)o.getAncestor(IRepository.FORMS);
											String[] parts = fallback.getRelationName().split("\\."); //$NON-NLS-1$
											Relation relation = fieldFlattenedSolution.getRelation(parts[0]);
											if (relation != null && !relation.isGlobal() && !relation.isLiteral() &&
												!relation.getPrimaryDataSource().equals(form.getDataSource()))
											{
												ServoyMarker mk = MarkerMessages.FormNamedFieldFallbackRelatedValuelist.fill(fieldName, vl.getName(),
													fallback.getName(), form.getName());
												addMarker(project, mk.getType(), mk.getText(), -1, FORM_FIELD_RELATED_VALUELIST, IMarker.PRIORITY_NORMAL, null,
													field);
											}
										}
									}
									if (vl.getRelationName() != null)
									{
										Relation[] relations = flattenedSolution.getRelationSequence(vl.getRelationName());
										if (relations != null && context instanceof Form)
										{
											for (Relation r : relations)
											{
												addEncapsulationMarker(project, field, r, (Form)context);
											}
										}
									}
								}
							}
							else if (type == Field.LIST_BOX || type == Field.MULTISELECT_LISTBOX || type == Field.SPINNER)
							{
								// these types of fields MUST have a valuelist
								Form form = (Form)o.getAncestor(IRepository.FORMS);
								ServoyMarker mk = MarkerMessages.RequiredPropertyMissingOnElement.fill("valuelist", fieldName, form.getName()); //$NON-NLS-1$
								addMarker(project, mk.getType(), mk.getText(), -1, FORM_REQUIRED_PROPERTY_MISSING, IMarker.PRIORITY_NORMAL, null, field);
							}
						}
						checkCancel();
						if (o instanceof Portal && ((Portal)o).getRelationName() != null)
						{
							Portal portal = (Portal)o;
							FlattenedSolution portalFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(portal, flattenedSolution);
							Relation[] relations = portalFlattenedSolution.getRelationSequence(portal.getRelationName());
							if (relations == null)
							{
								ServoyMarker mk;
								if (portal.getName() != null)
								{
									mk = MarkerMessages.FormPortalNamedInvalidRelationName.fill(portal.getName());
								}
								else
								{
									mk = MarkerMessages.FormPortalUnnamedInvalidRelationName;
								}
								addMarker(project, mk.getType(), mk.getText(), -1, FORM_PORTAL_INVALID_RELATION_NAME, IMarker.PRIORITY_NORMAL, null, o);
							}
							else
							{
								for (IPersist child : portal.getAllObjectsAsList())
								{
									if (child instanceof ISupportDataProviderID)
									{
										try
										{
											String id = ((ISupportDataProviderID)child).getDataProviderID();
											if (id != null)
											{
												int indx = id.lastIndexOf('.');
												if (indx > 0)
												{
													String rel_name = id.substring(0, indx);
													if (!rel_name.startsWith(portal.getRelationName()))
													{
														ServoyMarker mk;
														String elementName = null;
														if (child instanceof ISupportName && ((ISupportName)child).getName() != null)
														{
															elementName = ((ISupportName)child).getName();
														}
														if (portal.getName() != null)
														{
															if (elementName != null)
															{
																mk = MarkerMessages.FormPortalNamedElementNamedMismatchedRelation.fill(elementName,
																	portal.getName(), rel_name, portal.getRelationName());
															}
															else
															{
																mk = MarkerMessages.FormPortalNamedElementUnnamedMismatchedRelation.fill(portal.getName(),
																	rel_name, portal.getRelationName());
															}
														}
														else
														{
															if (elementName != null)
															{
																mk = MarkerMessages.FormPortalUnnamedElementNamedMismatchedRelation.fill(elementName, rel_name,
																	portal.getRelationName());
															}
															else
															{
																mk = MarkerMessages.FormPortalUnnamedElementUnnamedMismatchedRelation.fill(rel_name,
																	portal.getRelationName());
															}
														}
														addMarker(project, mk.getType(), mk.getText(), -1, FORM_PORTAL_INVALID_RELATION_NAME,
															IMarker.PRIORITY_NORMAL, null, child);
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
								if (context instanceof Form)
								{
									for (Relation r : relations)
									{
										addEncapsulationMarker(project, portal, r, (Form)context);
									}
								}
							}
						}
						checkCancel();
						if (o instanceof GraphicalComponent && ((GraphicalComponent)o).getLabelFor() != null &&
							!"".equals(((GraphicalComponent)o).getLabelFor()))
						{
							IPersist parent = null;
							if (o.getParent() instanceof Form)
							{
								parent = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution).getFlattenedForm(o);
							}
							else if (o.getParent() instanceof Portal)
							{
								parent = new FlattenedPortal((Portal)o.getParent());
							}
							if (parent != null)
							{
								final IPersist finalParent = parent;
								Object labelFor = parent.acceptVisitor(new IPersistVisitor()
								{
									public Object visit(IPersist persist)
									{
										if (persist instanceof ISupportName && ((GraphicalComponent)o).getLabelFor().equals(((ISupportName)persist).getName())) return persist;
										if (persist == finalParent)
										{
											return IPersistVisitor.CONTINUE_TRAVERSAL;
										}
										else
										{
											return IPersistVisitor.CONTINUE_TRAVERSAL_BUT_DONT_GO_DEEPER;
										}
									}
								});
								if (labelFor == null)
								{
									ServoyMarker mk = MarkerMessages.FormLabelForElementNotFound.fill(((Form)o.getAncestor(IRepository.FORMS)).getName(),
										((GraphicalComponent)o).getLabelFor());
									addMarker(project, mk.getType(), mk.getText(), -1, FORM_LABEL_FOR_ELEMENT_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, o);
								}
							}
						}
						if (o instanceof GraphicalComponent && ((GraphicalComponent)o).getRolloverImageMediaID() > 0 &&
							((GraphicalComponent)o).getImageMediaID() <= 0)
						{
							ServoyMarker mk = MarkerMessages.ImageMediaNotSet.fill(((Form)o.getAncestor(IRepository.FORMS)).getName());
							addMarker(project, mk.getType(), mk.getText(), -1, IMAGE_MEDIA_NOT_SET, IMarker.PRIORITY_NORMAL, null, o);
						}
						if (o instanceof GraphicalComponent &&
							(((GraphicalComponent)o).getRolloverImageMediaID() > 0 || ((GraphicalComponent)o).getRolloverCursor() > 0))
						{
							ServoyProject activeProject = getServoyModel().getActiveProject();
							if (activeProject != null &&
								(activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY || activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SOLUTION))
							{
								Form parentForm = (Form)context;
								if (parentForm != null &&
									(parentForm.getView() == FormController.LOCKED_TABLE_VIEW || parentForm.getView() == FormController.LOCKED_LIST_VIEW ||
										parentForm.getView() == FormController.TABLE_VIEW || parentForm.getView() == IForm.LIST_VIEW))
								{
									Part part = parentForm.getPartAt(((GraphicalComponent)o).getLocation().y);
									if (part != null && part.getPartType() == Part.BODY)
									{
										ServoyMarker mk = MarkerMessages.RolloverImageAndCursorNotWorking.fill();
										addMarker(project, mk.getType(), mk.getText(), -1, ROLLOVER_NOT_WORKING, IMarker.PRIORITY_NORMAL, null, o);
									}
								}
							}

						}
						checkCancel();
						if (o.getTypeID() == IRepository.SHAPES)
						{
							ServoyMarker mk = MarkerMessages.ObsoleteElement.fill(((Form)o.getAncestor(IRepository.FORMS)).getName());
							addMarker(project, mk.getType(), mk.getText(), -1, FORM_OBSOLETE_ELEMENT, IMarker.PRIORITY_NORMAL, null, o);
						}
						checkDeprecatedElementUsage(o, project, flattenedSolution);
						checkDeprecatedPropertyUsage(o, project);
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
						if (o instanceof ScriptVariable && servoyModel.getFlattenedSolution().getSolution().getSolutionType() == SolutionMetaData.MOBILE &&
							Ident.checkIfReservedBrowserWindowObjectWord(((ScriptVariable)o).getName()))
						{
							Pair<String, String> pathPair = SolutionSerializer.getFilePath(o, false);
							String path = ((AbstractBase)o).getSerializableRuntimeProperty(IScriptProvider.FILENAME);
							IResource file = project;
							if (path != null && !"".equals(path)) //$NON-NLS-1$
							{
								file = getEclipseResourceFromJavaIO(new java.io.File(path), project);
								if (file != null) path = file.getProjectRelativePath().toString();
							}
							if (path == null || "".equals(path)) path = pathPair.getRight(); //$NON-NLS-1$

							ServoyMarker mk = MarkerMessages.ReservedWindowObjectProperty.fill(((ScriptVariable)o).getName());

							addMarker(file, mk.getType(), mk.getText(), -1, RESERVED_WINDOW_OBJECT_PROPERTY, IMarker.PRIORITY_NORMAL, path, o);
						}
						if (o instanceof AbstractBase &&
							Boolean.TRUE.equals(((AbstractBase)o).getCustomMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON.propertyName)))
						{
							IPersist parentForm = o.getAncestor(IRepository.FORMS);
							if (parentForm != null && ((Form)parentForm).getNavigatorID() != Form.NAVIGATOR_NONE)
							{
								ServoyMarker mk = MarkerMessages.MobileFormNavigatorOverlapsHeaderButton.fill();
								addMarker(project, mk.getType(), mk.getText(), -1, MOBILE_NAVIGATOR_OVERLAPS_HEADER_BUTTON, IMarker.PRIORITY_NORMAL, null, o);
							}
						}

						checkCancel();
						return IPersistVisitor.CONTINUE_TRAVERSAL;
					}
				});
				checkRelations(project, missingServers);
				checkCancel();
				checkStyles(project);
				checkI18n(project);
				checkLoginSolution(project);
			}
			else if (!servoyModel.isSolutionActiveImportHook(project.getName()) && servoyModel.shouldBeModuleOfActiveSolution(project.getName()))
			{
				// so we have an actual Servoy project that is not active, but it should be active
				addDeserializeProblemMarkersIfNeeded(servoyProject);
				if (servoyProject.getDeserializeExceptions().size() == 0 && servoyProject.getSolution() == null)
				{
					addDeserializeProblemMarker(servoyProject.getProject(), "Probably some corrupted file(s). Please check solution metadata file.", //$NON-NLS-1$
						servoyProject.getProject().getName());
					ServoyLog.logError("No solution in a servoy project that has no deserialize problems", null); //$NON-NLS-1$
				}
			}

			for (Entry<String, IPersist> entry : missingServers.entrySet())
			{
				String missingServer = entry.getKey();
				IPersist persist = entry.getValue();
				ServoyMarker mk = MarkerMessages.ServerNotAccessibleFirstOccurence.fill(project.getName(), missingServer);
				IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, SERVER_NOT_ACCESSIBLE_FIRST_OCCURENCE, IMarker.PRIORITY_HIGH, null, persist);
				if (marker != null)
				{
					try
					{
						marker.setAttribute("missingServer", missingServer); //$NON-NLS-1$
						marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
						marker.setAttribute("SolutionName", project.getName()); //$NON-NLS-1$
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
			ServoyLog.logError("Servoy project is null for a eclipse project with correct nature", null); //$NON-NLS-1$
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

	private boolean formCanBeInstantiated(Form form, FlattenedSolution flattenedSolution, Map<Form, Boolean> checked)
	{
		Boolean canBeInstantiated = checked.get(form);
		if (canBeInstantiated == null)
		{
			canBeInstantiated = Boolean.valueOf(flattenedSolution.formCanBeInstantiated(form));
			checked.put(form, canBeInstantiated);
		}
		return canBeInstantiated.booleanValue();
	}

	/**
	 * Checks the state of a valueList, and is able to automatically correct a few problems - but with the possibility of loosing info.
	 * 
	 * @param vl the valueList to be checked.
	 * @param flattenedSolution the flattened solution used to get relations if needed.
	 * @param sm the server manager to use.
	 * @param fixIfPossible if this is true, it will try to fix a few problems, but be careful - some invalid info in the valueList will probably be lost.
	 * @return a list of problems. Each element in the list represents a problem, and is a 3 object array like [ (int)severity, (String)problemMessage,
	 *         (String)fixDescription ]. "fixDescription" is the description of the fix that would be applied if fixIfPossible is true; null if no fix would be
	 *         applied.
	 */
	public static List<Problem> checkValuelist(ValueList vl, FlattenedSolution flattenedSolution, IServerManagerInternal sm, boolean fixIfPossible)
	{
		List<Problem> problems = new ArrayList<Problem>();
		try
		{
			if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES)
			{
				if (vl.getCustomValues() != null)
				{
					String customSeverity = getSeverity(VALUELIST_DB_WITH_CUSTOM_VALUES.getLeft(), VALUELIST_DB_WITH_CUSTOM_VALUES.getRight().name(), vl);
					if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
					{
						// this is not a custom valuelist
						ServoyMarker marker = MarkerMessages.ValuelistDBWithCustomValues.fill(vl.getName());
						problems.add(new Problem(marker.getType(), getTranslatedSeverity(customSeverity, VALUELIST_DB_WITH_CUSTOM_VALUES.getRight()),
							marker.getText(), marker.getFix()));
					}
					if (fixIfPossible) vl.setCustomValues(null);
				}
				String dataSource = null;
				Table table = null;
				if (vl.getRelationName() != null)
				{
					// vl. based on relation; make sure table name/server name are not specified
					if (vl.getTableName() != null || vl.getServerName() != null)
					{
						String customSeverity = getSeverity(VALUELIST_RELATION_WITH_DATASOURCE.getLeft(), VALUELIST_RELATION_WITH_DATASOURCE.getRight().name(),
							vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistRelationWithDatasource.fill(vl.getName());
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_RELATION_WITH_DATASOURCE.getRight()),
								mk.getText(), mk.getFix()));
						}
						if (fixIfPossible) vl.setDataSource(null);
					}
					String[] parts = vl.getRelationName().split("\\."); //$NON-NLS-1$
					for (String relName : parts)
					{
						Relation relation = flattenedSolution.getRelation(relName);
						if (relation == null)
						{
							String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistRelationNotFound.fill(vl.getName(), relName);
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
									mk.getText()));
							}
						}
						else
						{
							dataSource = relation.getForeignDataSource();
						}
					}
					if (dataSource != null)
					{
						// check if the relations match up (check foreign/primary tables)
						if (flattenedSolution.getRelationSequence(vl.getRelationName()) == null)
						{
							String customSeverity = getSeverity(VALUELIST_RELATION_SEQUENCE_INCONSISTENT.getLeft(),
								VALUELIST_RELATION_SEQUENCE_INCONSISTENT.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistRelationSequenceInconsistent.fill(vl.getName(), vl.getRelationName());
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity,
									VALUELIST_RELATION_SEQUENCE_INCONSISTENT.getRight()), mk.getText()));
							}
						}
					}
				}
				else if (vl.getDataSource() != null)
				{
					// this is table based...
					dataSource = vl.getDataSource();
				}
				else
				{
					String customSeverity = getSeverity(VALUELIST_DB_NOT_TABLE_OR_RELATION.getLeft(), VALUELIST_DB_NOT_TABLE_OR_RELATION.getRight().name(), vl);
					if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
					{
						ServoyMarker mk = MarkerMessages.ValuelistDBNotTableOrRelation.fill(vl.getName());
						problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_DB_NOT_TABLE_OR_RELATION.getRight()),
							mk.getText()));
					}
				}
				if (dataSource != null)
				{
					String[] stn = DataSourceUtilsBase.getDBServernameTablename(dataSource);
					if (stn == null || (stn != null && (stn.length == 0 || (stn.length > 0 && stn[0] == null))))
					{
						String customSeverity = getSeverity(VALUELIST_DB_MALFORMED_TABLE_DEFINITION.getLeft(),
							VALUELIST_DB_MALFORMED_TABLE_DEFINITION.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistDBMalformedTableDefinition.fill(vl.getName(), dataSource);
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_DB_MALFORMED_TABLE_DEFINITION.getRight()),
								mk.getText()));
						}
					}
					else
					{
						IServerInternal server = (IServerInternal)sm.getServer(stn[0]);
						if (server != null)
						{
							if (!server.getName().equals(stn[0]))
							{
								String customSeverity = getSeverity(VALUELIST_DB_SERVER_DUPLICATE.getLeft(), VALUELIST_DB_SERVER_DUPLICATE.getRight().name(),
									vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBServerDuplicate.fill(vl.getName(), stn[0]);
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_DB_SERVER_DUPLICATE.getRight()),
										mk.getText()));
								}
							}
							table = server.getTable(stn[1]);
							if (table == null)
							{
								String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBTableNotAccessible.fill(vl.getName(), stn[1]);
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
								}
							}
							else if (table.isMarkedAsHiddenInDeveloper())
							{
								String customSeverity = getSeverity(INVALID_TABLE_REFERENCE.getLeft(), INVALID_TABLE_REFERENCE.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), "valuelist ", vl.getName());
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, INVALID_TABLE_REFERENCE.getRight()),
										IMarker.PRIORITY_LOW, mk.getText(), null));
								}
							}
							else if (table.getRowIdentColumnsCount() == 0)
							{
								String customSeverity = getSeverity(VALUELIST_DB_TABLE_NO_PK.getLeft(), VALUELIST_DB_TABLE_NO_PK.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBTableNoPk.fill(vl.getName(), stn[1]);
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_DB_TABLE_NO_PK.getRight()),
										IMarker.PRIORITY_LOW, mk.getText(), null));
								}
							}
						} // server not found is reported elsewhere
					}
				}
				if (table != null)
				{
					if (vl.getDataProviderID1() != null && !"".equals(vl.getDataProviderID1())) //$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID1());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table) == null)
							{
								String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID1(), table.getName());
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
								}
							}
							else if (flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table).isDeprecated())
							{
								String customSeverity = getSeverity(DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
									DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
										flattenedSolution.getScriptCalculation(vl.getDataProviderID1(), table).getName(), "valuelist " + vl.getName(),
										"Related value");
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity,
										DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()), mk.getText()));
								}
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID1(), table.getName());
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
									mk.getText()));
							}
						}
					}
					if (vl.getDataProviderID2() != null && !vl.getDataProviderID2().equals(vl.getDataProviderID1()) && !"".equals(vl.getDataProviderID2()))//$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID2());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table) == null)
							{
								String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID2(), table.getName());
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
								}
							}
							else if (flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table).isDeprecated())
							{
								String customSeverity = getSeverity(DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
									DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
										flattenedSolution.getScriptCalculation(vl.getDataProviderID2(), table).getName(), "valuelist " + vl.getName(),
										"Related value");
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity,
										DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()), mk.getText()));
								}
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID2(), table.getName());
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
									mk.getText()));
							}
						}
					}
					if (vl.getDataProviderID3() != null && !vl.getDataProviderID3().equals(vl.getDataProviderID1()) &&
						!vl.getDataProviderID3().equals(vl.getDataProviderID2()) && !"".equals(vl.getDataProviderID3()))//$NON-NLS-1$
					{
						Column column = table.getColumn(vl.getDataProviderID3());
						if (column == null)
						{
							if (flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table) == null)
							{
								String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID3(), table.getName());
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
										mk.getText()));
								}
							}
							else if (flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table).isDeprecated())
							{
								String customSeverity = getSeverity(DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
									DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
								if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
								{
									ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
										flattenedSolution.getScriptCalculation(vl.getDataProviderID3(), table).getName(), "valuelist " + vl.getName(),
										"Related value");
									problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity,
										DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()), mk.getText()));
								}
							}
						}
						else if (column.getColumnInfo() != null && column.getColumnInfo().isExcluded())
						{
							String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), vl.getDataProviderID3(), table.getName());
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
									mk.getText()));
							}
						}
					}
					if (vl.getUseTableFilter() && vl.getValueListType() == IValueListConstants.DATABASE_VALUES &&
						vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
					{
						Column column = table.getColumn(DBValueList.NAME_COLUMN);
						if (column == null)
						{
							String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.ValuelistDBDatasourceNotFound.fill(vl.getName(), DBValueList.NAME_COLUMN, table.getName());
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()),
									mk.getText()));
							}
						}
					}

					if (vl.getSortOptions() != null)
					{
						List<Problem> sortProblems = checkSortOptions(table, vl.getSortOptions(), vl, flattenedSolution);
						if (sortProblems != null)
						{
							problems.addAll(sortProblems);
						}
					}
				}
			}
			else if (vl.getValueListType() == IValueListConstants.CUSTOM_VALUES || vl.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
			{
				// custom value list; make sure it does not specify table/server/relation
				if (vl.getTableName() != null || vl.getServerName() != null || vl.getRelationName() != null)
				{
					String customSeverity = getSeverity(VALUELIST_CUSTOM_VALUES_WITH_DB_INFO.getLeft(), VALUELIST_CUSTOM_VALUES_WITH_DB_INFO.getRight().name(),
						vl);
					if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
					{
						ServoyMarker marker = MarkerMessages.ValuelistCustomValuesWithDBInfo.fill(vl.getName());
						problems.add(new Problem(marker.getType(), getTranslatedSeverity(customSeverity, VALUELIST_CUSTOM_VALUES_WITH_DB_INFO.getRight()),
							marker.getText(), marker.getFix()));
					}
					if (fixIfPossible)
					{
						vl.setDataSource(null);
						vl.setRelationName(null);
					}
				}
				if (vl.getValueListType() == IValueListConstants.GLOBAL_METHOD_VALUES)
				{
					ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(vl.getCustomValues());
					if (scriptMethod == null)
					{
						String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistGlobalMethodNotFound.fill(vl.getName());
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()), mk.getText()));
						}
					}
					else if (scriptMethod.getParent() != vl.getParent() && scriptMethod.isPrivate())
					{
						String customSeverity = getSeverity(VALUELIST_ENTITY_NOT_FOUND.getLeft(), VALUELIST_ENTITY_NOT_FOUND.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistGlobalMethodNotAccessible.fill(vl.getName());
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_ENTITY_NOT_FOUND.getRight()), mk.getText()));
						}
					}
					else if (scriptMethod.isDeprecated())
					{
						String customSeverity = getSeverity(DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getLeft(),
							DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() + "()",
								"valuelist " + vl.getName(), "Global method");
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM.getRight()),
								mk.getText()));
						}
					}
				}
				if (vl.getValueListType() == IValueListConstants.CUSTOM_VALUES)
				{
					String values = vl.getCustomValues();
					boolean invalidValues = false;
					if (values != null && values.contains("|")) //$NON-NLS-1$
					{
						StringTokenizer tk = new StringTokenizer(values.trim(), "\r\n"); //$NON-NLS-1$
						while (tk.hasMoreTokens())
						{
							String line = tk.nextToken();
							if (!line.contains("|")) //$NON-NLS-1$
							{
								invalidValues = true;
								break;
							}
						}
					}
					if (invalidValues)
					{
						String customSeverity = getSeverity(VALUELIST_INVALID_CUSTOM_VALUES.getLeft(), VALUELIST_INVALID_CUSTOM_VALUES.getRight().name(), vl);
						if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
						{
							ServoyMarker mk = MarkerMessages.ValuelistInvalidCustomValues.fill(vl.getName());
							problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_INVALID_CUSTOM_VALUES.getRight()),
								mk.getText()));
						}
					}
				}
			}
			else
			{
				String customSeverity = getSeverity(VALUELIST_TYPE_UNKNOWN.getLeft(), VALUELIST_TYPE_UNKNOWN.getRight().name(), vl);
				if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
				{
					ServoyMarker mk = MarkerMessages.ValuelistTypeUnknown.fill(vl.getName(), vl.getValueListType());
					problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_TYPE_UNKNOWN.getRight()), mk.getText()));
				}
				if (fixIfPossible) vl.setValueListType(IValueListConstants.CUSTOM_VALUES);
			}
		}
		catch (Exception ex)
		{
			exceptionCount++;
			if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
			String customSeverity = getSeverity(VALUELIST_GENERIC_ERROR.getLeft(), VALUELIST_GENERIC_ERROR.getRight().name(), vl);
			if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
			{
				ServoyMarker mk;
				if (ex.getMessage() != null) mk = MarkerMessages.ValuelistGenericErrorWithDetails.fill(vl.getName(), ex.getMessage());
				else mk = MarkerMessages.ValuelistGenericError.fill(vl.getName());
				problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity, VALUELIST_GENERIC_ERROR.getRight()), mk.getText()));
			}
		}
		return problems;
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
		if (modulesNames != null && !"".equals(modulesNames)) //$NON-NLS-1$
		{
			StringTokenizer st = new StringTokenizer(modulesNames, ";,"); //$NON-NLS-1$
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
		HashMap<File, Exception> deserializeExceptionMessages = servoyProject.getDeserializeExceptions();
		for (Map.Entry<File, Exception> entry : deserializeExceptionMessages.entrySet())
		{
			IResource file = getEclipseResourceFromJavaIO(entry.getKey(), servoyProject.getProject());
			if (file == null) file = servoyProject.getProject();
			addDeserializeProblemMarker(file, entry.getValue().getMessage(), servoyProject.getProject().getName());
		}
	}

	private void checkServers(IProject project)
	{
		ServoyProject activeProject = getServoyModel().getActiveProject();
		if (activeProject != null && activeProject.getProject().getName().equals(project.getName()))
		{
			String[] array = ApplicationServerSingleton.get().getServerManager().getServerNames(true, false, false, true);
			for (String server_name : array)
			{
				IServerInternal server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(server_name, true, false);
				boolean existing = false;
				for (String name : ApplicationServerSingleton.get().getServerManager().getKnownDriverClassNames())
				{
					if (name.equals(server.getConfig().getDriver()))
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
							marker.setAttribute("serverName", server_name); //$NON-NLS-1$
						}
						catch (CoreException e)
						{
							ServoyLog.logError(e);
						}
					}
				}

				if (ApplicationServerSingleton.get().getServerManager().isServerDataModelCloneCycling(server))
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
								Object markerSeverity = marker.getAttribute(IMarker.SEVERITY);
								if (markerSeverity == null || (markerSeverity != null && ((Integer)markerSeverity).intValue() > IMarker.SEVERITY_WARNING))
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
			int idx = deserializeExceptionMessage.indexOf("character"); //$NON-NLS-1$
			if (idx >= 0)
			{
				StringTokenizer st = new StringTokenizer(deserializeExceptionMessage.substring(idx + 9), " "); //$NON-NLS-1$
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

	private void parseEventMethod(final IProject project, final ScriptMethod eventMethod, final String eventName)
	{
		if (eventMethod != null &&
			(eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS) == null || eventMethod.getRuntimeProperty(IScriptProvider.METHOD_ARGUMENTS).length == 0) &&
			eventMethod.getDeclaration().contains("arguments"))
		{
			int offset = ScriptingUtils.getArgumentsUsage(eventMethod.getDeclaration());
			if (offset >= 0)
			{
				ServoyMarker mk = MarkerMessages.MethodEventParameters;
				IMarker marker = addMarker(project, mk.getType(), mk.getText(), eventMethod.getLineNumberOffset() + offset, METHOD_EVENT_PARAMETERS,
					IMarker.PRIORITY_NORMAL, null, eventMethod);
				if (marker != null)
				{
					try
					{
						marker.setAttribute("EventName", eventName); //$NON-NLS-1$
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		}
	}

	private boolean skipEventMethod(String name)
	{
		if ("onOpenMethodID".equals(name)) return true;
		return false;
	}

	private IResource getEclipseResourceFromJavaIO(File javaIOFile, IProject project)
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
		if (path != null && !"".equals(path)) //$NON-NLS-1$
		{
			file = getEclipseResourceFromJavaIO(new java.io.File(path), project);
			if (file != null) path = file.getProjectRelativePath().toString();
		}
		if (path == null || "".equals(path)) path = pathPair.getRight(); //$NON-NLS-1$
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

	private ServoyProject getServoyProject(IProject project)
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
			IServerInternal s = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(solution.getI18nServerName());
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
		String[] array = ApplicationServerSingleton.get().getServerManager().getServerNames(true, true, false, true);
		for (String server_name : array)
		{
			try
			{
				//get the tables used in current solution
				Set<String> dataSources = getDataSourceCollectorVisitor().getDataSources();
				SortedSet<String> serverNames = DataSourceUtils.getServerNames(dataSources);

				IServerInternal server = (IServerInternal)ApplicationServerSingleton.get().getServerManager().getServer(server_name, true, true);
				if (server != null) // server may have become invalid in the mean time
				{
					List<String> tableNames = server.getTableAndViewNames(true);
					Iterator<String> tables = tableNames.iterator();
					while (tables.hasNext())
					{
						String tableName = tables.next();
						if (server.isTableLoaded(tableName) && !server.isTableMarkedAsHiddenInDeveloper(tableName))
						{
							Table table = server.getTable(tableName);
							IResource res = project;
							if (getServoyModel().getDataModelManager() != null &&
								getServoyModel().getDataModelManager().getDBIFile(server_name, tableName).exists())
							{
								res = getServoyModel().getDataModelManager().getDBIFile(server_name, tableName);
							}
							Map<String, Column> columnsByName = new HashMap<String, Column>();
							Map<String, Column> columnsByDataProviderID = new HashMap<String, Column>();
							for (Column column : table.getColumns())
							{
								if (column.getColumnInfo() != null && column.getSequenceType() == ColumnInfo.UUID_GENERATOR &&
									!column.getColumnInfo().hasFlag(Column.UUID_COLUMN))
								{
									ServoyMarker mk = MarkerMessages.ColumnUUIDFlagNotSet.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_UUID_FLAG_NOT_SET, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
								}
								// check type defined by column converter
								int dataProviderType = getDataType(res, column, null, null);
								if ((column.getSequenceType() == ColumnInfo.UUID_GENERATOR && (dataProviderType != IColumnTypes.TEXT && dataProviderType != IColumnTypes.MEDIA)) ||
									(column.getSequenceType() == ColumnInfo.SERVOY_SEQUENCE && (dataProviderType != IColumnTypes.INTEGER && dataProviderType != IColumnTypes.NUMBER)))
								{
									ServoyMarker mk = MarkerMessages.ColumnIncompatibleTypeForSequence.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_INCOMPATIBLE_TYPE_FOR_SEQUENCE, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
								}
								if (column.getAllowNull() && column.getRowIdentType() != Column.NORMAL_COLUMN)
								{
									ServoyMarker mk = MarkerMessages.ColumnRowIdentShouldNotAllowNull.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, ROW_IDENT_SHOULD_NOT_BE_NULL, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
								}
								if (column.hasFlag(Column.UUID_COLUMN))
								{
									int length = columnHasConvertedType(column) ? 0 : column.getConfiguredColumnType().getLength();
									boolean compatibleForUUID = false;
									switch (dataProviderType)
									{
										case IColumnTypes.MEDIA :
											compatibleForUUID = length == 0 || length >= 16;
											break;
										case IColumnTypes.TEXT :
											compatibleForUUID = length == 0 || length >= 36;
											break;
									}
									if (!compatibleForUUID)
									{
										ServoyMarker mk = MarkerMessages.ColumnIncompatbleWithUUID.fill(tableName, column.getName());
										addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_INCOMPATIBLE_WITH_UUID, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
											"columnName", column.getName());
									}
								}
								if (column.isDBIdentity() && !column.isDatabasePK())
								{
									ServoyMarker mk = MarkerMessages.ColumnDatabaseIdentityProblem.fill(tableName, column.getName());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_DATABASE_IDENTITY_PROBLEM, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
								}
								if (column.getColumnInfo() != null && column.getColumnInfo().getForeignType() != null &&
									!tableNames.contains(column.getColumnInfo().getForeignType()))
								{
									ServoyMarker mk = MarkerMessages.ColumnForeignTypeProblem.fill(tableName, column.getName(),
										column.getColumnInfo().getForeignType());
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_FOREIGN_TYPE_PROBLEM, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
								}
								if (column.getColumnInfo() != null)
								{
									if (column.getColumnInfo().getAutoEnterType() == ColumnInfo.LOOKUP_VALUE_AUTO_ENTER)
									{
										String lookup = column.getColumnInfo().getLookupValue();
										if (lookup != null && !"".equals(lookup)) //$NON-NLS-1$
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
														ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() +
															"()", "table " + tableName, "Lookup value");
														addMarker(res, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
															IMarker.PRIORITY_NORMAL, null, null).setAttribute("columnName", column.getName());
													}
												}
											}
											else
											{
												Table lookupTable = table;
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
														lookupTable = r.getForeignTable();
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
												addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_LOOKUP_INVALID, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
													"columnName", column.getName());
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
													IPropertyDescriptor propertyDescriptor = ((IPropertyDescriptorProvider)validator).getPropertyDescriptor(key);
													if (propertyDescriptor != null && propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
													{
														ScriptMethod scriptMethod = null;
														Map<String, String> parsedValidatorProperties = ComponentFactory.parseJSonProperties(column.getColumnInfo().getValidatorProperties());
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
															ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedFunction.fill(scriptMethod.getDisplayName() +
																"()", "table " + tableName, validator.getName());
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
													IPropertyDescriptor propertyDescriptor = ((IPropertyDescriptorProvider)converter).getPropertyDescriptor(key);
													if (propertyDescriptor != null && propertyDescriptor.getType() == IPropertyDescriptor.GLOBAL_METHOD)
													{
														Map<String, String> parsedConverterProperties = ComponentFactory.parseJSonProperties(column.getColumnInfo().getConverterProperties());
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

									if ((getServoyModel().getServoyProject(project.getName()).getSolution().getSolutionType() == SolutionMetaData.MOBILE || getServoyModel().getServoyProject(
										project.getName()).getSolution().getSolutionType() == SolutionMetaData.MOBILE_MODULE) &&
										serverNames.contains(server_name) &&
										DataSourceUtils.getServerTablenames(dataSources, server_name).contains(tableName) && column.hasBadNaming(true))
									{
										ServoyMarker mk = MarkerMessages.ReservedWindowObjectColumn.fill(column.getName());
										addMarker(res, mk.getType(), mk.getText(), -1, RESERVED_WINDOW_OBJECT_COLUMN, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
											"columnName", column.getName());
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
									addMarker(res, mk.getType(), mk.getText(), -1, COLUMN_DUPLICATE_NAME_DPID, IMarker.PRIORITY_NORMAL, null, null).setAttribute(
										"columnName", column.getName());
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

	private void checkStyles(final IProject project)
	{
		deleteMarkers(project, MISSING_STYLE);
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		FlattenedSolution flattenedSolution = getServoyModel().getFlattenedSolution();
		if (servoyProject != null)
		{
			Iterator<Form> it = servoyProject.getSolution().getForms(null, false);
			while (it.hasNext())
			{
				final Form form = it.next();
				String styleName = form.getStyleName();
				if (styleName == null && form.getExtendsID() > 0)
				{
					List<Form> forms = flattenedSolution.getFormHierarchy(form);
					for (Form parentForm : forms)
					{
						if (parentForm.getStyleName() != null)
						{
							styleName = parentForm.getStyleName();
							break;
						}
					}
				}
				if (styleName != null)
				{
					if (!"_servoy_mobile".equals(styleName))// internal style for mobile
					{
						Style style = null;
						try
						{
							style = (Style)ApplicationServerSingleton.get().getDeveloperRepository().getActiveRootObject(styleName, IRepository.STYLES);
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}
						final Style finalStyle = style;
						if (style == null)
						{
							ServoyMarker mk = MarkerMessages.StyleNotFound.fill(styleName, form.getName());
							IMarker marker = addMarker(project, mk.getType(), mk.getText(), -1, STYLE_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, form);
							if (marker != null)
							{
								try
								{
									marker.setAttribute("clearStyle", true);
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
							}
							continue;
						}
						form.acceptVisitor(new IPersistVisitor()
						{
							public Object visit(IPersist o)
							{
								if (o instanceof BaseComponent || o instanceof Form || o instanceof Part)
								{
									String styleClass = null;
									if (o instanceof BaseComponent) styleClass = ((BaseComponent)o).getStyleClass();
									else if (o instanceof Form) styleClass = ((Form)o).getStyleClass();
									else if (o instanceof Part) styleClass = ((Part)o).getStyleClass();
									if (styleClass != null)
									{
										String[] classes = ModelUtils.getStyleClasses(finalStyle, ModelUtils.getStyleLookupname(o), form.getStyleClass());
										List<String> styleClasses = Arrays.asList(classes);
										if (!styleClasses.contains(styleClass))
										{
											ServoyMarker mk = MarkerMessages.StyleFormClassNotFound.fill(styleClass, form.getName());
											IMarker marker = null;
											if (o instanceof Part)
											{
												mk = MarkerMessages.StyleElementClassNotFound.fill(styleClass, Part.getDisplayName(((Part)o).getPartType()),
													form.getName());
												marker = addMarker(project, mk.getType(), mk.getText(), -1, STYLE_CLASS_NOT_FOUND, IMarker.PRIORITY_NORMAL,
													null, o);
											}
											else if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
											{
												mk = MarkerMessages.StyleElementClassNotFound.fill(styleClass, ((ISupportName)o).getName(), form.getName());
												marker = addMarker(project, mk.getType(), mk.getText(), -1, STYLE_CLASS_NOT_FOUND, IMarker.PRIORITY_NORMAL,
													null, o);
											}
											else marker = addMarker(project, mk.getType(), mk.getText(), -1, STYLE_CLASS_NOT_FOUND, IMarker.PRIORITY_NORMAL,
												null, o);
											for (String currentClass : styleClasses)
											{
												if (currentClass.equalsIgnoreCase(styleClass))
												{
													try
													{
														marker.setAttribute("styleClass", currentClass);
													}
													catch (CoreException e)
													{
														ServoyLog.logError(e);
													}
													break;
												}
											}
										}
									}
								}
								return IPersistVisitor.CONTINUE_TRAVERSAL;
							}
						});
					}
				}
				else
				{
					form.acceptVisitor(new IPersistVisitor()
					{
						public Object visit(IPersist o)
						{
							if (o instanceof BaseComponent || o instanceof Form)
							{
								String styleClass = null;
								if (o instanceof BaseComponent) styleClass = ((BaseComponent)o).getStyleClass();
								else if (o instanceof Form) styleClass = ((Form)o).getStyleClass();
								if (styleClass != null)
								{
									ServoyMarker mk = MarkerMessages.StyleFormClassNoStyle.fill(styleClass, form.getName());
									if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
									{
										mk = MarkerMessages.StyleElementClassNoStyle.fill(styleClass, ((ISupportName)o).getName(), form.getName());
										addMarker(project, mk.getType(), mk.getText(), -1, STYLE_CLASS_NO_STYLE, IMarker.PRIORITY_NORMAL, null, o);
									}
									else addMarker(project, mk.getType(), mk.getText(), -1, STYLE_CLASS_NO_STYLE, IMarker.PRIORITY_NORMAL, null, o);
								}
							}
							return IPersistVisitor.CONTINUE_TRAVERSAL;
						}
					});
				}
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
									String message = "Form '" + form.getName() + "' is part of a login solution and it must not have the datasource property set; its current datasource is : '" + form.getDataSource() + "'"; //$NON-NLS-1$ //$NON-NLS-2$ //$NON-NLS-3$
									IMarker marker = addMarker(prj, FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, message, -1,
										LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, IMarker.PRIORITY_HIGH, null, form);
									if (marker != null)
									{
										try
										{
											marker.setAttribute("Uuid", o.getUUID().toString()); //$NON-NLS-1$
											marker.setAttribute("SolutionName", form.getSolution().getName()); //$NON-NLS-1$
											marker.setAttribute("PropertyName", "dataSource"); //$NON-NLS-1$ //$NON-NLS-2$
											marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("dataSource", o.getClass())); //$NON-NLS-1$ //$NON-NLS-2$
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

	private static List<Problem> checkSortOptions(Table table, String sortOptions, IPersist persist, FlattenedSolution flattenedSolution)
	{
		if (persist == null || sortOptions == null) return null;
		List<Problem> problems = new ArrayList<Problem>();

		String elementName = null;
		if (persist instanceof Form)
		{
			elementName = "Form";//$NON-NLS-1$ 
		}
		else if (persist instanceof Relation)
		{
			elementName = "Relation";//$NON-NLS-1$ 
		}
		else if (persist instanceof ValueList)
		{
			elementName = "Valuelist";//$NON-NLS-1$ 
		}
		else
		{
			elementName = "Element";//$NON-NLS-1$ 
		}
		String name = null;
		if (persist instanceof ISupportName) name = ((ISupportName)persist).getName();
		StringTokenizer tk = new StringTokenizer(sortOptions, ",");//$NON-NLS-1$ 
		while (tk.hasMoreTokens())
		{
			String columnName = null;
			String def = tk.nextToken().trim();
			int index = def.indexOf(" "); //$NON-NLS-1$
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
				Table lastTable = table;
				String[] split = columnName.split("\\."); //$NON-NLS-1$
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
						if (!lastTable.equals(relation.getPrimaryTable()))
						{
							String customSeverity = getSeverity(INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE.getLeft(),
								INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE.getRight().name(), persist);
							if (!customSeverity.equals(ProblemSeverity.IGNORE.name()))
							{
								ServoyMarker mk = MarkerMessages.InvalidSortOptionsRelationDifferentPrimaryDatasource.fill(elementName, name, sortOptions,
									relation.getName());
								problems.add(new Problem(mk.getType(), getTranslatedSeverity(customSeverity,
									INVALID_SORT_OPTIONS_RELATION_DIFFERENT_PRIMARY_DATASOURCE.getRight()), mk.getText()));
							}
						}
						lastTable = relation.getForeignTable();
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

	private void checkRelations(IProject project, Map<String, IPersist> missingServers)
	{
		ServoyProject servoyProject = getServoyModel().getServoyProject(project.getName());
		if (servoyProject != null)
		{
			ServoyMarker mk = null;

			IServerManagerInternal sm = ApplicationServerSingleton.get().getServerManager();
			Iterator<Relation> it = servoyProject.getSolution().getRelations(false);
			while (it.hasNext())
			{
				checkCancel();
				Relation element = it.next();
				if (!missingServers.containsKey(element.getPrimaryServerName()) && !missingServers.containsKey(element.getForeignServerName()))
				{
					FlattenedSolution relationFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(element, getServoyModel().getFlattenedSolution());
					element.setValid(true);//if is reload
					try
					{
						IServerInternal pserver = (IServerInternal)sm.getServer(element.getPrimaryServerName());
						if (pserver == null)
						{
							mk = MarkerMessages.RelationPrimaryServerWithProblems.fill(element.getName(), element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, RELATION_PRIMARY_SERVER_WITH_PROBLEMS, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (!pserver.getName().equals(element.getPrimaryServerName()))
							{
								mk = MarkerMessages.RelationPrimaryServerDuplicate.fill(element.getName(), element.getPrimaryServerName());
								addMarker(project, mk.getType(), mk.getText(), -1, RELATION_SERVER_DUPLICATE, IMarker.PRIORITY_NORMAL, null, element);
							}
						}
						ITable ptable = pserver.getTable(element.getPrimaryTableName());
						boolean usingHiddenTableInPrimary = false;
						if (ptable == null)
						{
							mk = MarkerMessages.RelationPrimaryTableNotFound.fill(element.getName(), element.getPrimaryTableName(),
								element.getPrimaryServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, RELATION_TABLE_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (((Table)ptable).isMarkedAsHiddenInDeveloper())
							{
								usingHiddenTableInPrimary = true;
								mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ptable.getDataSource(), "relation ", element.getName());
								addMarker(project, mk.getType(), mk.getText(), -1, INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW, null, element);
							}
							if (((Table)ptable).getRowIdentColumnsCount() == 0)
							{
								mk = MarkerMessages.RelationPrimaryTableWithoutPK.fill(element.getName(), element.getPrimaryTableName(),
									element.getPrimaryServerName());
								element.setValid(false);
								addMarker(project, mk.getType(), mk.getText(), -1, RELATION_TABLE_WITHOUT_PK, IMarker.PRIORITY_NORMAL, null, element);
								continue;
							}
						}

						IServerInternal fserver = (IServerInternal)sm.getServer(element.getForeignServerName());
						if (fserver == null)
						{
							mk = MarkerMessages.RelationForeignServerWithProblems.fill(element.getName(), element.getForeignServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, RELATION_FOREIGN_SERVER_WITH_PROBLEMS, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else if (!fserver.getName().equals(element.getForeignServerName()))
						{
							mk = MarkerMessages.RelationForeignServerDuplicate.fill(element.getName(), element.getForeignServerName());
							addMarker(project, mk.getType(), mk.getText(), -1, RELATION_SERVER_DUPLICATE, IMarker.PRIORITY_NORMAL, null, element);
						}

						ITable ftable = fserver.getTable(element.getForeignTableName());
						if (ftable == null)
						{
							mk = MarkerMessages.RelationForeignTableNotFound.fill(element.getName(), element.getForeignTableName(),
								element.getForeignServerName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, RELATION_TABLE_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						else
						{
							if (!usingHiddenTableInPrimary && ((Table)ftable).isMarkedAsHiddenInDeveloper())
							{
								mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(ftable.getDataSource(), "relation ", element.getName());
								addMarker(project, mk.getType(), mk.getText(), -1, INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW, null, element);
							}
							if (((Table)ftable).getRowIdentColumnsCount() == 0)
							{
								mk = MarkerMessages.RelationForeignTableWithoutPK.fill(element.getName(), element.getForeignTableName(),
									element.getForeignServerName());
								element.setValid(false);
								addMarker(project, mk.getType(), mk.getText(), -1, RELATION_TABLE_WITHOUT_PK, IMarker.PRIORITY_NORMAL, null, element);
								continue;
							}
						}
						if (!element.isParentRef() && element.getItemCount() == 0)
						{
							mk = MarkerMessages.RelationEmpty.fill(element.getName());
							element.setValid(false);
							addMarker(project, mk.getType(), mk.getText(), -1, RELATION_EMPTY, IMarker.PRIORITY_NORMAL, null, element);
							continue;
						}
						if (element.getInitialSort() != null)
						{
							addMarkers(project, checkSortOptions((Table)ftable, element.getInitialSort(), element, relationFlattenedSolution), element);
						}
						Iterator<RelationItem> items = element.getObjects(IRepository.RELATION_ITEMS);
						boolean errorsFound = false;
						while (items.hasNext())
						{
							RelationItem item = items.next();
							String primaryDataProvider = item.getPrimaryDataProviderID();
							String foreignColumn = item.getForeignColumnName();
							IDataProvider dataProvider = null;
							IDataProvider column = null;
							if (primaryDataProvider == null || "".equals(primaryDataProvider))//$NON-NLS-1$ 
							{
								mk = MarkerMessages.RelationItemNoPrimaryDataprovider.fill(element.getName());
								errorsFound = true;
								addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, element);
							}
							else if (!primaryDataProvider.startsWith(LiteralDataprovider.LITERAL_PREFIX))
							{
								if (ScopesUtils.isVariableScope(primaryDataProvider))
								{
									dataProvider = relationFlattenedSolution.getGlobalDataProvider(primaryDataProvider, true);
									if (dataProvider == null)
									{
										String[] enumParts = primaryDataProvider.split("\\."); //$NON-NLS-1$
										if (enumParts.length > 3)
										{
											String dp = enumParts[0] + '.' + enumParts[1] + '.' + enumParts[2];
											IDataProvider globalDataProvider = relationFlattenedSolution.getGlobalDataProvider(dp, true);
											if (globalDataProvider instanceof ScriptVariable)
											{
												if (((ScriptVariable)globalDataProvider).isEnum())
												{
													List<EnumDataProvider> enumDataProviders = ScriptingUtils.getEnumDataProviders((ScriptVariable)globalDataProvider);
													for (EnumDataProvider enumProvider : enumDataProviders)
													{
														if (enumProvider.getDataProviderID().equals(primaryDataProvider))
														{
															dataProvider = enumProvider;
															break;
														}
													}
												}
												else
												{
													// TODO better message
													mk = MarkerMessages.RelationItemPrimaryEnumDataproviderNotFound.fill(element.getName(), primaryDataProvider);
													errorsFound = true;
													addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND,
														IMarker.PRIORITY_NORMAL, null, element);
												}
											}
										}
									}
									else if (dataProvider != null && dataProvider instanceof ScriptVariable && ((ScriptVariable)dataProvider).isDeprecated())
									{
										mk = MarkerMessages.ElementUsingDeprecatedVariable.fill(((ScriptVariable)dataProvider).getName(),
											"relation " + element.getName(), "primary key");
										addMarker(project, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM, IMarker.PRIORITY_NORMAL,
											null, element);
									}
								}
								else
								{
									dataProvider = relationFlattenedSolution.getDataProviderForTable((Table)ptable, primaryDataProvider);
								}
								if (dataProvider == null)
								{
									mk = MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill(element.getName(), primaryDataProvider);
									errorsFound = true;
									addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND, IMarker.PRIORITY_NORMAL, null,
										element);
								}
								else
								{
									if (dataProvider instanceof ScriptCalculation)
									{
										ScriptCalculation calc = (ScriptCalculation)dataProvider;
										if (calc.isDeprecated())
										{
											mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(calc.getName(), "relation " + element.getName(),
												"primary key");
											addMarker(project, mk.getType(), mk.getText(), -1, DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
												IMarker.PRIORITY_NORMAL, null, element);
										}
									}
								}
							}
							if (foreignColumn == null || "".equals(foreignColumn))//$NON-NLS-1$ 
							{
								mk = MarkerMessages.RelationItemNoForeignDataprovider.fill(element.getName());
								errorsFound = true;
								addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND, IMarker.PRIORITY_NORMAL, null, element);
							}
							else
							{
								column = relationFlattenedSolution.getDataProviderForTable((Table)ftable, foreignColumn);
								if (column == null)
								{
									mk = MarkerMessages.RelationItemForeignDataproviderNotFound.fill(element.getName(), foreignColumn);
									errorsFound = true;
									addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND, IMarker.PRIORITY_NORMAL, null,
										element);
								}
							}
							if (dataProvider != null && column != null && dataProvider instanceof Column && column instanceof Column &&
								((Column)dataProvider).getColumnInfo() != null && ((Column)column).getColumnInfo() != null)
							{
								boolean primaryColumnUuidFlag = ((Column)dataProvider).getColumnInfo().hasFlag(Column.UUID_COLUMN);
								boolean foreignColumnUuidFlag = ((Column)column).getColumnInfo().hasFlag(Column.UUID_COLUMN);
								if ((primaryColumnUuidFlag && !foreignColumnUuidFlag) || (!primaryColumnUuidFlag && foreignColumnUuidFlag))
								{
									mk = MarkerMessages.RelationItemUUIDProblem.fill(element.getName(), primaryDataProvider, foreignColumn);
									errorsFound = true;
									addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_UUID_PROBLEM, IMarker.PRIORITY_NORMAL, null, element);
								}
								if (((Column)dataProvider).getColumnInfo().isExcluded())
								{
									mk = MarkerMessages.RelationItemPrimaryDataproviderNotFound.fill(element.getName(), primaryDataProvider);
									addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND, IMarker.PRIORITY_NORMAL, null,
										element);
								}
								if (((Column)column).getColumnInfo().isExcluded())
								{
									mk = MarkerMessages.RelationItemForeignDataproviderNotFound.fill(element.getName(), foreignColumn);
									addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_DATAPROVIDER_NOT_FOUND, IMarker.PRIORITY_NORMAL, null,
										element);
								}
							}
						}
						if (errorsFound)
						{
							element.setValid(false);
							continue;
						}
						if (getServoyModel().getActiveProject() == servoyProject)
						{
							String typeMismatchWarning = element.checkKeyTypes(relationFlattenedSolution);
							if (typeMismatchWarning != null)
							{
								mk = MarkerMessages.RelationItemTypeProblem.fill(element.getName(), typeMismatchWarning);
								addMarker(project, mk.getType(), mk.getText(), -1, RELATION_ITEM_TYPE_PROBLEM, IMarker.PRIORITY_LOW, null, element);
							}
						}
					}
					catch (Exception ex)
					{
						exceptionCount++;
						if (exceptionCount < MAX_EXCEPTIONS) ServoyLog.logError(ex);
						element.setValid(false);
						if (ex.getMessage() != null) mk = MarkerMessages.RelationGenericErrorWithDetails.fill(element.getName(), ex.getMessage());
						else mk = MarkerMessages.RelationGenericError.fill(element.getName());
						addMarker(project, mk.getType(), mk.getText(), -1, RELATION_GENERIC_ERROR, IMarker.PRIORITY_NORMAL, null, element);
					}
				}
			}
		}
	}

	private void checkResourcesForServoyProject(IProject project)
	{
		deleteMarkers(project, MULTIPLE_RESOURCES_PROJECTS_MARKER_TYPE);
		deleteMarkers(project, NO_RESOURCES_PROJECTS_MARKER_TYPE);
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
			}

			if (count > 1)
			{
				// > 1 => multiple referenced resources projects; error; quick fix would be choose one of them
				ServoyMarker mk = MarkerMessages.ReferencesToMultipleResources.fill(project.getName());
				addMarker(project, mk.getType(), mk.getText(), -1, REFERENCES_TO_MULTIPLE_RESOURCES, IMarker.PRIORITY_NORMAL, null, null);
			}
			else if (count == 0)
			{
				// 0 => no referenced resources projects; error; quick fix would be choose one of the resources projects in the work space
				ServoyMarker mk = MarkerMessages.NoResourceReference.fill(project.getName());
				addMarker(project, mk.getType(), mk.getText(), -1, NO_RESOURCE_REFERENCE, IMarker.PRIORITY_NORMAL, null, null);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError("Exception while reading referenced projects for " + project.getName(), e); //$NON-NLS-1$
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
		String customSeverity = persist != null ? getSeverity(problemSeverity.getLeft(), problemSeverity.getRight().name(), persist) : getSeverity(
			problemSeverity.getLeft(), problemSeverity.getRight().name(), activeProject.getProject());
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
			if (persist != null)
			{
				Pair<String, String> pathPair = SolutionSerializer.getFilePath(persist, true);
				Path path = new Path(pathPair.getLeft() + pathPair.getRight());
				IFile file = ResourcesPlugin.getWorkspace().getRoot().getFile(path);
				if (file.exists())
				{
					marker = file.createMarker(type);
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
					marker.setAttribute(IMarker.LOCATION, "Line " + lineNmbr); //$NON-NLS-1$
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

			if (type.equals(INVALID_TABLE_NODE_PROBLEM))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("Name", ((ISupportName)persist).getName()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", resource.getName()); //$NON-NLS-1$
			}
			else if (type.equals(DUPLICATE_UUID) || type.equals(DUPLICATE_SIBLING_UUID) || type.equals(BAD_STRUCTURE_MARKER_TYPE) ||
				type.equals(INVALID_SORT_OPTION) || type.equals(EVENT_METHOD_MARKER_TYPE) || type.equals(PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE) ||
				type.equals(INVALID_EVENT_METHOD) || type.equals(MISSING_STYLE) || type.equals(INVALID_COMMAND_METHOD) || type.equals(INVALID_DATAPROVIDERID) ||
				type.equals(OBSOLETE_ELEMENT) || type.equals(HIDDEN_TABLE_STILL_IN_USE) || type.equals(LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE) ||
				type.equals(FORM_DUPLICATE_PART_MARKER_TYPE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", resource.getName()); //$NON-NLS-1$
				if (type.equals(INVALID_DATAPROVIDERID) && persist instanceof ISupportDataProviderID)
				{
					marker.setAttribute("DataProviderID", ((ISupportDataProviderID)persist).getDataProviderID()); //$NON-NLS-1$
				}
			}
			else if (type.equals(DUPLICATE_NAME_MARKER_TYPE))
			{
				marker.setAttribute("Uuid", persist.getUUID().toString()); //$NON-NLS-1$
				marker.setAttribute("SolutionName", persist.getRootObject().getName()); //$NON-NLS-1$
			}
			else if (type.equals(PROJECT_FORM_MARKER_TYPE))
			{
				marker.setAttribute("SolutionName", persist.getRootObject().getName()); //$NON-NLS-1$
			}

			return marker;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot create problem marker", e); //$NON-NLS-1$
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
					marker.setAttribute(IMarker.LOCATION, "Character " + charNumber); //$NON-NLS-1$
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
			ServoyLog.logWarning("Cannot create problem marker", e); //$NON-NLS-1$
		}
		return null;
	}

	public static void deleteMarkers(IResource file, String type)
	{
		try
		{
			if (file.getProject().isOpen()) file.deleteMarkers(type, true, IResource.DEPTH_INFINITE);
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Cannot delete problem marker", e); //$NON-NLS-1$
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
			ServoyLog.logWarning("Cannot delete problem marker", e); //$NON-NLS-1$
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
			ServoyLog.logWarning("Cannot delete problem marker", e); //$NON-NLS-1$
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

	protected void fullBuild(final IProgressMonitor progressMonitor)
	{
		try
		{
			this.monitor = progressMonitor;
			getProject().accept(new ServoyResourceVisitor());
			this.monitor = null;
		}
		catch (CoreException e)
		{
			ServoyLog.logWarning("Full Servoy build failed", e); //$NON-NLS-1$
		}
	}

	protected void incrementalBuild(IResourceDelta delta, IProgressMonitor progressMonitor) throws CoreException
	{
		// the visitor does the work.
		this.monitor = progressMonitor;
		delta.accept(new ServoyDeltaVisitor());
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
	private int getDataType(IResource resource, IDataProvider dataProvider, ParsedFormat parsedFormat, IPersist persist) throws IOException
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
							ScriptMethod scriptMethod = getServoyModel().getFlattenedSolution().getScriptMethod(null, methodName);
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
					int convType = ((ITypedColumnConverter)converter).getToObjectType(ComponentFactory.<String> parseJSonProperties(columnInfo.getConverterProperties()));
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
						int convType = ((ITypedColumnConverter)converter).getToObjectType(ComponentFactory.<String> parseJSonProperties(columnInfo.getConverterProperties()));
						return convType != Integer.MAX_VALUE;
					}
				}
			}
		}
		return false;
	}

	private boolean isUnstoredCalc(String dpid, Table table, FlattenedSolution flattenedSolution)
	{
		return table != null && dpid != null && flattenedSolution.getScriptCalculation(dpid, table) != null && table.getColumn(dpid) == null;
	}

	private static FlattenedSolution getPersistFlattenedSolution(IPersist persist, FlattenedSolution fallbackFlattenedSolution)
	{
		FlattenedSolution persistFlattenedSolution = ModelUtils.getEditingFlattenedSolution(persist);
		return persistFlattenedSolution != null ? persistFlattenedSolution : fallbackFlattenedSolution;
	}

	private void addEncapsulationMarker(IProject project, IPersist persist, IPersist foundPersist, Form context)
	{
		if (foundPersist instanceof ISupportEncapsulation)
		{
			if (PersistEncapsulation.isModuleScope((ISupportEncapsulation)foundPersist, (Solution)persist.getRootObject()) &&
				!(context.getSolution().equals(foundPersist.getRootObject())))
			{
				ServoyMarker mk = MarkerMessages.NonAccessibleFormInModuleUsedInParentSolutionForm.fill(
					RepositoryHelper.getObjectTypeName(foundPersist.getTypeID()), ((ISupportName)foundPersist).getName(),
					foundPersist.getRootObject().getName(), getServoyProject(project).getSolution().getName(), context.getName());
				addMarker(project, mk.getType(), mk.getText(), -1, NON_ACCESSIBLE_PERSIST_IN_MODULE_USED_IN_PARENT_SOLUTION, IMarker.PRIORITY_LOW, null,
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
