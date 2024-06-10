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
package com.servoy.eclipse.ui.node;

public enum UserNodeType
{
	SERVERS,
	SERVER,
	TABLE,
	TABLES,
	VIEW,
	VIEWS,
	PROCEDURE,
	PROCEDURES,
	APPLICATION_ITEM,
	HISTORY_ITEM,
	SOLUTION_MODEL_ITEM,
	GLOBAL_VARIABLE_ITEM,
	GLOBAL_METHOD_ITEM,
	FORM_METHOD,
	FORM_CONTROLLER_FUNCTION_ITEM,
	FORM_ELEMENTS_ITEM_METHOD,
	TABLE_COLUMNS_ITEM,
	SECURITY_ITEM,
	SECURITY,
	FUNCTIONS_ITEM,
	PLUGINS_ITEM,
	EXCEPTIONS,
	EXCEPTIONS_ITEM,
	I18N,
	I18N_ITEM,
	CALCULATIONS_ITEM,
	RELEASE,
	SOLUTION,
	ALL_SOLUTIONS,
	SOLUTION_ITEM,
	SOLUTION_ITEM_NOT_ACTIVE_MODULE,
	ALL_WEB_PACKAGE_PROJECTS,
	WEB_PACKAGE_PROJECT_IN_WORKSPACE, // not necessarily used by active solution
	APPLICATION,
	SOLUTION_MODEL,
	HISTORY,
	GLOBALS_ITEM,
	SCOPES_ITEM,
	SCOPES_ITEM_CALCULATION_MODE,
	GLOBAL_VARIABLES,
	GLOBALSCRIPT,
	GLOBALRELATIONS,
	FOUNDSET,
	FOUNDSET_ITEM,
	FOUNDSET_MANAGER,
	STYLES,
	STYLE_ITEM,
	USER_GROUP_SECURITY,
	I18N_FILES,
	I18N_FILE_ITEM,
	TEMPLATES,
	TEMPLATE_ITEM,
	FOUNDSET_MANAGER_ITEM,
	MODULES,
	MODULE,
	FORMS,
	COMPONENT_FORMS,
	FORMS_GRAYED_OUT,
	FORM,
	JSLIB,
	APIEXPLORER,
	DATE,
	ARRAY,
	OBJECT,
	REGEXP,
	STRING,
	NUMBER,
	UTILS,
	UTIL_ITEM,
	CLIENT_UTILS,
	CLIENT_UTIL_ITEM,
	FORM_CONTROLLER,
	FORM_VARIABLES,
	FORM_VARIABLE_ITEM,
	FORM_ELEMENTS,
	FORM_ELEMENTS_INHERITED,
	FORM_ELEMENTS_ITEM,
	FORM_ELEMENTS_GROUP,
	FORM_CONTAINERS,
	FORM_CONTAINERS_ITEM,
	TABLE_COLUMNS,
	FUNCTIONS,
	JSON,
	PLUGINS,
	PLUGIN,
	MIGRATION,
	CALCULATIONS,
	ALL_RELATIONS,
	RELATIONS,
	RELATION,
	CALC_RELATION,
	RELATION_COLUMN,
	RELATION_METHODS,
	STATEMENTS,
	STATEMENTS_ITEM,
	SPECIAL_OPERATORS,
	CURRENT_FORM,
	CURRENT_FORM_ITEM,
	BEANS,
	BEAN,
	BEAN_METHOD,
	RETURNTYPEPLACEHOLDER,
	RETURNTYPE,
	RETURNTYPE_CONSTANT,
	RETURNTYPE_ELEMENT,
	FORM_FOUNDSET,
	VALUELISTS,
	VALUELIST_ITEM,
	MEDIA,
	MEDIA_IMAGE,
	MEDIA_FOLDER,
	RESOURCES,
	SOLUTION_CONTAINED_AND_REFERENCED_WEB_PACKAGES, //unique node in solex under each solution/module - holds web packages - binaries and project references
	COMPONENTS_FROM_RESOURCES, //unique node in solex under Resources - holds folder and zip component packages
	COMPONENTS_NONPROJECT_PACKAGE, //node in solex under Resources->NG Components or under Solution->Web Packages it is a folder or zip component package
	COMPONENTS_PROJECT_PACKAGE, //node in solex under Solution->Web Packages it is a project component package
	COMPONENT, // one component - it can either belong to a folder/zip or to a project package
	SERVICES_FROM_RESOURCES, //unique node in solex under Resources - holds folder and zip service packages
	SERVICES_NONPROJECT_PACKAGE, //node in solex under Resources->NG Services or under Solution->Web Packages  it is a folder or zip service package
	SERVICES_PROJECT_PACKAGE, //node in solex under Solution->Web Packages it is a project service package
	SERVICE, // one service - it can either belong to a folder/zip or to a project package
	LAYOUT_NONPROJECT_PACKAGE, //node in solex under  Solution->Web Packages it is a folder or zip layout package
	LAYOUT_PROJECT_PACKAGE, //node in solex under Solution->Web Packages it is a project layout package
	LAYOUT, // one layout - it can either belong to a folder/zip or to a project package	XML_METHODS,
	XML_METHODS,
	XML_LIST_METHODS,
	JSUNIT,
	JSUNIT_ITEM,
	GRAYED_OUT,
	WORKING_SET,
	DATASOURCES,
	COMPONENT_RESOURCE,
	SOLUTION_DATASOURCES,
	INMEMORY_DATASOURCES,
	INMEMORY_DATASOURCE,
	VIEW_FOUNDSETS,
	VIEW_FOUNDSET,
	WEB_OBJECT_FOLDER,
	ZIP_RESOURCE,
	MAP,
	SET,
	ITERATOR,
	ITERABELVALUE,
	LOADING, //a temporary node type which is displayed until the normal ones are loaded
	CUSTOM_TYPE
}