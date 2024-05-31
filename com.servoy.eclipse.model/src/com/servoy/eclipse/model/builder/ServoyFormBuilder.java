/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2020 Servoy BV

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
import java.lang.reflect.Method;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.swing.ImageIcon;

import org.eclipse.core.resources.IMarker;
import org.eclipse.core.resources.IResource;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.Path;
import org.json.JSONArray;
import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebComponentSpecProvider;
import org.sablo.specification.WebObjectFunctionDefinition;
import org.sablo.specification.WebObjectSpecification;
import org.sablo.specification.property.CustomJSONPropertyType;
import org.sablo.specification.property.ICustomType;
import org.sablo.specification.property.types.FunctionPropertyType;

import com.servoy.base.persistence.IBaseColumn;
import com.servoy.base.persistence.IMobileProperties;
import com.servoy.base.persistence.PersistUtils;
import com.servoy.base.persistence.constants.IValueListConstants;
import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.builder.MarkerMessages.ServoyMarker;
import com.servoy.eclipse.model.inmemory.AbstractMemTable;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.repository.SolutionDeserializer;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.FormController;
import com.servoy.j2db.IForm;
import com.servoy.j2db.component.ComponentFactory;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.BaseComponent;
import com.servoy.j2db.persistence.CSSPositionUtils;
import com.servoy.j2db.persistence.Column;
import com.servoy.j2db.persistence.ColumnWrapper;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Field;
import com.servoy.j2db.persistence.FlattenedPortal;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.GraphicalComponent;
import com.servoy.j2db.persistence.IBasicWebObject;
import com.servoy.j2db.persistence.IColumnTypes;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IFormElement;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.IScriptElement;
import com.servoy.j2db.persistence.IScriptProvider;
import com.servoy.j2db.persistence.ISupportDataProviderID;
import com.servoy.j2db.persistence.ISupportEncapsulation;
import com.servoy.j2db.persistence.ISupportExtendsID;
import com.servoy.j2db.persistence.ISupportName;
import com.servoy.j2db.persistence.ISupportScope;
import com.servoy.j2db.persistence.ISupportTabSeq;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.Portal;
import com.servoy.j2db.persistence.Relation;
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
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.ValueList;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebCustomType;
import com.servoy.j2db.server.ngclient.FormElement;
import com.servoy.j2db.server.ngclient.FormElementHelper;
import com.servoy.j2db.server.ngclient.FormElementHelper.FormComponentCache;
import com.servoy.j2db.server.ngclient.property.ComponentTypeConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedConfig;
import com.servoy.j2db.server.ngclient.property.FoundsetLinkedPropertyType;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.server.ngclient.property.ValueListConfig;
import com.servoy.j2db.server.ngclient.property.types.DataproviderPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormComponentPropertyType;
import com.servoy.j2db.server.ngclient.property.types.FormPropertyType;
import com.servoy.j2db.server.ngclient.property.types.IDataLinkedType.TargetDataLinks;
import com.servoy.j2db.server.ngclient.property.types.MediaPropertyType;
import com.servoy.j2db.server.ngclient.property.types.PropertyPath;
import com.servoy.j2db.server.ngclient.property.types.RelationPropertyType;
import com.servoy.j2db.server.ngclient.property.types.TagStringPropertyType;
import com.servoy.j2db.server.ngclient.property.types.ValueListPropertyType;
import com.servoy.j2db.server.ngclient.property.types.VariantPropertyType;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.Debug;
import com.servoy.j2db.util.FormatParser;
import com.servoy.j2db.util.FormatParser.ParsedFormat;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.RoundHalfUpDecimalFormat;
import com.servoy.j2db.util.ScopesUtils;
import com.servoy.j2db.util.Settings;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;
import com.servoy.j2db.util.docvalidator.IdentDocumentValidator;

/**
 * @author lvostinar
 *
 */
public class ServoyFormBuilder
{
	public static void addFormMarkers(ServoyProject servoyProject, Form form, Set<UUID> methodsParsed, Map<Form, Boolean> formsAbstractChecked)
	{
		IPersist context = form;
		IResource markerResource = ServoyBuilderUtils.getPersistResource(form);
		FlattenedSolution fs = ServoyBuilderUtils.getReferenceFlattenedSolution(servoyProject.getSolution());

		String styleName = form.getStyleName();
		if (styleName != null && !"_servoy_mobile".equals(styleName))// internal style for mobile
		{
			Style style = null;
			try
			{
				style = (Style)ApplicationServerRegistry.get().getDeveloperRepository().getActiveRootObject(styleName, IRepository.STYLES);
			}
			catch (Exception ex)
			{
				ServoyLog.logError(ex);
			}
			if (style == null)
			{
				ServoyMarker mk = MarkerMessages.StyleNotFound.fill(styleName, form.getName());
				IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.STYLE_NOT_FOUND, IMarker.PRIORITY_NORMAL,
					null, form);
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
			}
		}
		boolean checkStyleClass = PersistHelper.getOrderedStyleSheets(servoyProject.getEditingFlattenedSolution()).size() == 0;

		boolean isLoginSolution = servoyProject.getSolution().getSolutionType() == SolutionMetaData.LOGIN_SOLUTION;
		if (isLoginSolution && form.getDataSource() != null) // login solution cannot have forms with datasource
		{
			String message = "Form '" + form.getName() +
				"' is part of a login solution and it must not have the datasource property set; its current datasource is : '" +
				form.getDataSource() + "'";
			IMarker marker = ServoyBuilder.addMarker(ServoyBuilderUtils.getPersistResource(form), ServoyBuilder.FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, message,
				-1,
				ServoyBuilder.LOGIN_FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, IMarker.PRIORITY_HIGH, null, form);
			if (marker != null)
			{
				try
				{
					marker.setAttribute("Uuid", form.getUUID().toString());
					marker.setAttribute("SolutionName", form.getSolution().getName());
					marker.setAttribute("PropertyName", "dataSource");
					marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("dataSource", form.getClass()));
				}
				catch (CoreException ex)
				{
					ServoyLog.logError(ex);
				}
			}
		}

		BuilderDependencies.getInstance().addDatasourceDependency(form.getDataSource(), form);

		Map<String, Set<IPersist>> formElementsByName = new HashMap<String, Set<IPersist>>();
		Map<String, Set<IPersist>> formScriptProviderByName = new HashMap<String, Set<IPersist>>();

		form.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
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
						// Don't set meta data properties
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
								IPersist foundPersist = fs.searchPersist(((EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository())
									.getUUIDForElementId(element_id, element_id, -1, -1, null));

								if (foundPersist instanceof Form)
								{
									BuilderDependencies.getInstance().addDependency(form, (Form)foundPersist);
								}

								ServoyBuilderUtils.addNullReferenceMarker(markerResource, o, foundPersist, context, element);
								ServoyBuilderUtils.addNotAccessibleMethodMarkers(markerResource, o, foundPersist, context, element, fs);
								ServoyBuilderUtils.addMethodParseErrorMarkers(markerResource, o, foundPersist, context, element, methodsParsed,
									methodsReferences);

								if (((Form)context).isFormComponent().booleanValue() &&
									BaseComponent.isEventOrCommandProperty(element.getName()) &&
									((Form)context).getFlattenedPropertiesMap().containsKey(element.getName()))
								{
									ServoyMarker mk = MarkerMessages.FormReferenceInvalidProperty.fill(((Form)context).getName(), element.getName());
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_REFERENCE_INVALID_PROPERTY,
										IMarker.PRIORITY_NORMAL, null,
										context);
								}

								if (foundPersist instanceof ISupportEncapsulation)
								{
									ServoyBuilder.addEncapsulationMarker(markerResource, servoyProject.getProject(), o, foundPersist, (Form)context);
								}

								if (foundPersist instanceof Form)
								{
									if (!StaticContentSpecLoader.PROPERTY_EXTENDSID.getPropertyName().equals(element.getName()) &&
										!ServoyBuilderUtils.formCanBeInstantiated(((Form)foundPersist),
											ServoyBuilder.getPersistFlattenedSolution(foundPersist, fs), formsAbstractChecked))
									{
										ServoyMarker mk = MarkerMessages.PropertyFormCannotBeInstantiated.fill(element.getName());
										ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
											ServoyBuilder.SOLUTION_PROPERTY_FORM_CANNOT_BE_INSTANTIATED,
											IMarker.PRIORITY_LOW, null, o);
									}
								}
								if (foundPersist instanceof ISupportScope)
								{
									BuilderDependencies.getInstance().addDependency(((ISupportScope)foundPersist).getScopeName(), form);
								}
							}
						}
					}
				}
				catch (Exception e)
				{
					throw new RuntimeException(e);
				}

				addWebComponentMissingReferences(markerResource, fs, o, form, form.getDataSource());

				if (((AbstractBase)o).getRuntimeProperty(
					SolutionDeserializer.POSSIBLE_DUPLICATE_UUID) != null)
				{
					ServoyBuilder.checkDuplicateUUID(o, servoyProject.getProject());
				}

				if (o instanceof WebComponent)
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(((WebComponent)o).getTypeName());
					if (spec != null)
					{
						Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
						if (properties.size() > 0)
						{
							FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement((WebComponent)o, fs, null, true);
							for (PropertyDescription pd : properties)
							{
								String datasource = form.getDataSource();
								Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
								Form frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, fs);
								if (frm == null) continue;
								BuilderDependencies.getInstance().addDependency(form, frm);
								if (pd.getConfig() instanceof ComponentTypeConfig)
								{
									checkForListFormComponent(markerResource, frm, o, fs);
									String forFoundsetName = ((ComponentTypeConfig)pd.getConfig()).forFoundset;
									String foundsetValue = null;
									if (((WebComponent)o).hasProperty(forFoundsetName))
									{
										Object foundsetJson = ((WebComponent)o).getProperty(forFoundsetName);
										if (foundsetJson instanceof JSONObject)
										{
											foundsetValue = (String)((JSONObject)foundsetJson).get(FoundsetPropertyType.FOUNDSET_SELECTOR);
										}
									}
									else
									{
										//default is form foundset
										foundsetValue = "";
									}
									if (foundsetValue != null)
									{
										if (DataSourceUtils.isDatasourceUri(foundsetValue))
										{
											datasource = foundsetValue;
										}
										else if (foundsetValue.equals(""))
										{
											datasource = form.getDataSource();
										}
										else
										{
											Relation[] relations = fs.getRelationSequence(foundsetValue);
											if (relations != null && relations.length > 0)
											{
												datasource = relations[relations.length - 1].getForeignDataSource();
											}
											else
											{
												List<Form> forms = fs.getFormsForNamedFoundset(Form.NAMED_FOUNDSET_SEPARATE_PREFIX + foundsetValue);
												if (forms.size() > 0)
												{
													datasource = forms.get(0).getDataSource();
												}
											}
										}
										if (frm.getDataSource() != null && !Utils.equalObjects(datasource, frm.getDataSource()))
										{
											ServoyMarker mk = MarkerMessages.FormComponentForFoundsetInvalidDataSource.fill(((WebComponent)o).getName(),
												pd.getName(), frm.getName(), forFoundsetName);
											ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
												ServoyBuilder.FORM_COMPONENT_INVALID_DATASOURCE,
												IMarker.PRIORITY_NORMAL, null, o);
										}
									}
								}
								else if (frm.getDataSource() != null && !Utils.equalObjects(form.getDataSource(), frm.getDataSource()))
								{
									ServoyMarker mk = MarkerMessages.FormComponentInvalidDataSource.fill(((WebComponent)o).getName(), pd.getName(),
										frm.getName(), form.getName());
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_COMPONENT_INVALID_DATASOURCE,
										IMarker.PRIORITY_NORMAL,
										null,
										o);
								}

								if (frm.isResponsiveLayout() && !form.isResponsiveLayout())
								{
									ServoyMarker mk = MarkerMessages.FormComponentIncompatibleLayout.fill(((WebComponent)o).getName(),
										pd.getName(), frm.getName());
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
										ServoyBuilder.FORM_COMPONENT_INVALID_LAYOUT_COMBINATION,
										IMarker.PRIORITY_NORMAL, null, o);
								}

								BuilderDependencies.getInstance().addDatasourceDependency(datasource, form);
								BuilderDependencies.getInstance().addDatasourceDependency(frm.getDataSource(), form);

								FormComponentCache cache = FormElementHelper.INSTANCE.getFormComponentCache(formComponentEl, pd,
									(JSONObject)propertyValue, frm, fs);
								for (FormElement element : cache.getFormComponentElements())
								{
									checkDataProviders(markerResource, servoyProject, element.getPersistIfAvailable(), context, datasource,
										fs);
									addWebComponentMissingReferences(markerResource, fs, element.getPersistIfAvailable(), form, datasource);
								}
							}
						}
					}
				}

				checkVariants(markerResource, servoyProject, o, context, null, fs);

				checkDataProviders(markerResource, servoyProject, o, context, null, fs);
				if (o instanceof IFormElement)
				{
					String name = ((ISupportName)o).getName();
					if (name != null && name.startsWith(ComponentFactory.WEB_ID_PREFIX))
					{
						ServoyMarker mk = MarkerMessages.ElementNameReservedPrefixIdentifier.fill(name, ComponentFactory.WEB_ID_PREFIX);
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.SOLUTION_ELEMENT_NAME_RESERVED_PREFIX_IDENTIFIER,
							IMarker.PRIORITY_NORMAL,
							null, o);
					}
				}
				if (o instanceof BaseComponent && ((BaseComponent)o).getVisible())
				{
					// check if not outside form
					Form flattenedForm = ServoyBuilder.getPersistFlattenedSolution(o, fs).getFlattenedForm(form);
					if (flattenedForm != null && flattenedForm.getCustomMobileProperty(IMobileProperties.MOBILE_FORM.propertyName) == null)
					{
						Point location = CSSPositionUtils.getLocation((BaseComponent)o);
						if (location != null)
						{
							boolean outsideForm = false;
							Iterator<com.servoy.j2db.persistence.Part> parts = flattenedForm.getParts();
							while (parts.hasNext())
							{
								com.servoy.j2db.persistence.Part part = parts.next();
								int startPos = flattenedForm.getPartStartYPos(part.getID());
								int endPos = part.getHeight();
								if (startPos <= location.y && endPos > location.y)
								{
									// found the part
									int height = CSSPositionUtils.getSize((BaseComponent)o).height;
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
										ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_ELEMENT_OUTSIDE_BOUNDS,
											IMarker.PRIORITY_LOW, null, o);
									}
									int width = flattenedForm.getWidth();
									if (PersistUtils.isHeaderPart(part.getPartType()) || PersistUtils.isFooterPart(part.getPartType()))
									{
										String defaultPageFormat = flattenedForm.getDefaultPageFormat();
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
											int w = (int)(currentPageFormat.getImageableWidth() * (flattenedForm.getPaperPrintScale() / 100d));
											if (width < w)
											{
												width = w;
											}
										}
									}
									if (width < location.x + CSSPositionUtils.getSize((BaseComponent)o).width)
									{
										outsideForm = true;
									}
									break;
								}
							}
							if (location.y > flattenedForm.getSize().height && flattenedForm.getParts().hasNext())
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
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_ELEMENT_OUTSIDE_BOUNDS,
									IMarker.PRIORITY_LOW, null, o);
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
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER,
							IMarker.PRIORITY_LOW, null, o);
					}
				}
				if (o instanceof Form)
				{
					FlattenedSolution formFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(form, fs);
					ITable table = null;
					String path = form.getSerializableRuntimeProperty(IScriptProvider.FILENAME);
					if (path != null && !path.endsWith(SolutionSerializer.getFileName(form, false)))
					{
						ServoyMarker mk = MarkerMessages.FormFileNameInconsistent.fill(form.getName(), path);
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FILE_NAME_INCONSISTENT,
							IMarker.PRIORITY_NORMAL,
							null, form);
					}
					try
					{
						table = formFlattenedSolution.getTable(form.getDataSource());

						if (table == null && form.getDataSource() != null)
						{
							ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH,
								null, form);
						}
						else if (table != null && !(table instanceof AbstractMemTable) && table.getRowIdentColumnsCount() == 0)
						{
							ServoyMarker mk = MarkerMessages.FormTableNoPK.fill(form.getName(), form.getTableName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH,
								null, form);
						}
						else if (table != null && form.getInitialSort() != null)
						{
							ServoyBuilder.addMarkers(markerResource, ServoyBuilder.checkSortOptions(table, form.getInitialSort(), form, formFlattenedSolution),
								form);
						}
						if (table != null && table.isMarkedAsHiddenInDeveloper())
						{
							ServoyMarker mk = MarkerMessages.TableMarkedAsHiddenButUsedIn.fill(table.getDataSource(), "form ", form.getName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.INVALID_TABLE_REFERENCE, IMarker.PRIORITY_LOW,
								null, form);
						}
					}
					catch (Exception e)
					{
						ServoyLog.logError(e);
						ServoyMarker mk = MarkerMessages.FormTableNotAccessible.fill(form.getName(), form.getTableName());
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_TABLE, IMarker.PRIORITY_HIGH, null,
							form);
					}

					// if form uses global relation named foundset, check to see that it is valid
					String namedFoundset = form.getNamedFoundSet();
					if (namedFoundset != null && !namedFoundset.equals(Form.NAMED_FOUNDSET_EMPTY) &&
						!namedFoundset.equals(Form.NAMED_FOUNDSET_SEPARATE) && !namedFoundset.startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
					{
						// it must be a global relation then
						Relation r = formFlattenedSolution.getRelation(form.getGlobalRelationNamedFoundset());
						if (r != null)
						{
							BuilderDependencies.getInstance().addDependency(form, fs.getRelation(r.getName()));
						}
						if (r == null)
						{
							// what is this then?
							ServoyMarker mk = MarkerMessages.PropertyInFormTargetNotFound.fill("namedFoundset", form.getName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
								IMarker.PRIORITY_NORMAL, null, form);
						}
						else if (!r.isGlobal() ||
							!Solution.areDataSourcesCompatible(ApplicationServerRegistry.get().getDeveloperRepository(), form.getDataSource(),
								r.getForeignDataSource()))
						{
							// wrong kind of relation
							ServoyMarker mk = MarkerMessages.FormNamedFoundsetIncorrectValue.fill(form.getName(),
								" The relation must be global and it's foreign data source must match the form's datasource.");
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FOUNDSET_INCORRECT_VALUE,
								IMarker.PRIORITY_NORMAL, null, form);
						}
					}
					if (namedFoundset != null && namedFoundset.startsWith(Form.NAMED_FOUNDSET_SEPARATE_PREFIX))
					{
						fs.getFormsForNamedFoundset(namedFoundset).stream()
							.filter(defineForm -> Utils.equalObjects(namedFoundset, defineForm.getNamedFoundSet()) &&
								!Utils.equalObjects(form.getDataSource(), defineForm.getDataSource()))
							.forEach(defineForm -> {
								ServoyMarker mk = MarkerMessages.NamedFoundsetDatasourceNotMatching.fill(
									namedFoundset.substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH), form.getName(), form.getDataSource(),
									defineForm.getName());
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_NAMED_FOUNDSET_DATASOURCE_MISMATCH,
									IMarker.PRIORITY_NORMAL, null,
									form);
								IResource defineFormFile = ServoyBuilderUtils.getPersistResource(defineForm);
								ServoyBuilder.deleteMarkers(defineFormFile, ServoyBuilder.NAMED_FOUNDSET_DATASOURCE);

								mk = MarkerMessages.NamedFoundsetDatasourceNotMatching.fill(
									namedFoundset.substring(Form.NAMED_FOUNDSET_SEPARATE_PREFIX_LENGTH), defineForm.getName(), defineForm.getDataSource(),
									form.getName());
								ServoyBuilder.addMarker(defineFormFile, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_NAMED_FOUNDSET_DATASOURCE_MISMATCH, IMarker.PRIORITY_NORMAL, null,
									defineForm);

								BuilderDependencies.getInstance().addDependency(form, defineForm);
								BuilderDependencies.getInstance().addDependency(defineForm, form);
							});
					}

					addFormVariablesHideTableColumn(markerResource, form, table);

					if (form.getExtendsID() > 0 && formFlattenedSolution != null)
					{
						Form superForm = formFlattenedSolution.getForm(form.getExtendsID());
						if (superForm != null)
						{
							if (form.getDataSource() != null && superForm.getDataSource() != null &&
								!form.getDataSource().equals(superForm.getDataSource()))
							{
								ServoyMarker mk = MarkerMessages.FormDerivedFormDifferentTable.fill(form.getName(), superForm.getName());
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_DERIVED_FORM_DIFFERENT_TABLE,
									IMarker.PRIORITY_NORMAL, null,
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
											ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_EXTENDS_CYCLE,
												IMarker.PRIORITY_LOW,
												null, form);
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
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_EXTENDS_FORM_ELEMENT_NOT_FOUND,
								IMarker.PRIORITY_NORMAL, null,
								form);
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
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_DUPLICATE_PART,
										IMarker.PRIORITY_NORMAL, null, part);
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
						else if (isTableView && persist instanceof IFormElement &&
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
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_ELEMENT_DUPLICATE_TAB_SEQUENCE,
									IMarker.PRIORITY_NORMAL, null,
									persist);
							}
							else
							{
								tabSequences.put(Integer.valueOf(tabSeq), null);
							}
						}
						else if (persist instanceof TabPanel && ((TabPanel)persist).getTabSeq() < 0)
						{
							ServoyMarker mk = MarkerMessages.FormTabPanelTabSequenceNotSet.fill(form.getName(), ((TabPanel)persist).getName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.TAB_SEQUENCE_NOT_SET, IMarker.PRIORITY_NORMAL,
								null, persist);
						}
					}
					if (portalAndTabPanelCount > ServoyBuilder.LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM)
					{
						ServoyMarker mk = MarkerMessages.FormHasTooManyThingsAndProbablyLowPerformance.fill(
							String.valueOf(ServoyBuilder.LIMIT_FOR_PORTAL_TABPANEL_COUNT_ON_FORM), "portals/tab panels", "");
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.LEVEL_PERFORMANCE_TABS_PORTALS,
							IMarker.PRIORITY_NORMAL, null, form);
					}
					if (fieldCount > ServoyBuilder.LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM)
					{
						ServoyMarker mk = MarkerMessages.FormHasTooManyThingsAndProbablyLowPerformance.fill(
							String.valueOf(ServoyBuilder.LIMIT_FOR_FIELD_COUNT_ON_TABLEVIEW_FORM), "columns", " table view");
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.LEVEL_PERFORMANCE_COLUMNS_TABLEVIEW,
							IMarker.PRIORITY_NORMAL, null, form);
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
								ScriptCalculation calc = formFlattenedSolution.getScriptCalculation(form.getRowBGColorCalculation(), table);
								if (calc != null)
								{
									unresolved = false;
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
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
								IMarker.PRIORITY_NORMAL, null, form);
						}
					}
				}
				if (o instanceof ScriptMethod)
				{
					ScriptMethod scriptMethod = (ScriptMethod)o;
					if (scriptMethod.getDeclaration() != null && scriptMethod.getDeclaration().contains(SolutionSerializer.OVERRIDEKEY) &&
						PersistHelper.getOverridenMethod(scriptMethod) == null)
					{
						ServoyMarker mk = MarkerMessages.MethodOverrideProblem.fill(scriptMethod.getName(), ((Form)scriptMethod.getParent()).getName());
						IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.METHOD_OVERRIDE_PROBLEM,
							IMarker.PRIORITY_NORMAL, null, o);
						try
						{
							marker.setAttribute("Uuid", scriptMethod.getUUID().toString());
							marker.setAttribute("SolutionName", scriptMethod.getRootObject().getName());
						}
						catch (CoreException e)
						{
							Debug.error(e);
						}


					}
					ServoyBuilderUtils.addScriptMethodErrorMarkers(markerResource, scriptMethod);
				}

				if (!(o instanceof ScriptVariable) && !(o instanceof ScriptMethod) && !(o instanceof Form) && o instanceof ISupportName &&
					((ISupportName)o).getName() != null)
				{
					Set<IPersist> duplicates = formElementsByName.get(((ISupportName)o).getName());
					if (duplicates != null)
					{
						for (IPersist duplicatePersist : duplicates)
						{
							UUID wrongOverrideUUID = null;
							if (duplicatePersist instanceof IFormElement && ((IFormElement)duplicatePersist).getExtendsID() == o.getID())
							{
								wrongOverrideUUID = duplicatePersist.getUUID();
							}
							else if (o instanceof IFormElement && ((IFormElement)o).getExtendsID() == duplicatePersist.getID())
							{
								wrongOverrideUUID = o.getUUID();
							}
							if (wrongOverrideUUID != null)
							{
								ServoyMarker mk = MarkerMessages.DuplicateOverrideFound.fill(((ISupportName)o).getName(), form.getName());
								IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.DUPLICATION_DUPLICATE_OVERRIDE_FOUND,
									IMarker.PRIORITY_NORMAL, null, duplicatePersist);
								try
								{
									marker.setAttribute("Uuid", wrongOverrideUUID.toString());
									marker.setAttribute("SolutionName", servoyProject.getSolution().getName());
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}

								mk = MarkerMessages.DuplicateOverrideFound.fill(((ISupportName)o).getName(), form.getName());
								marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.DUPLICATION_DUPLICATE_OVERRIDE_FOUND, IMarker.PRIORITY_NORMAL,
									null, o);
								try
								{
									marker.setAttribute("Uuid", wrongOverrideUUID.toString());
									marker.setAttribute("SolutionName", servoyProject.getSolution().getName());
								}
								catch (CoreException e)
								{
									ServoyLog.logError(e);
								}
							}
							else
							{
								ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill("form element", ((ISupportName)o).getName(),
									form.getName());
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DUPLICATION_DUPLICATE_ENTITY_FOUND,
									IMarker.PRIORITY_NORMAL, null,
									duplicatePersist);

								mk = MarkerMessages.DuplicateEntityFound.fill("form element", ((ISupportName)o).getName(), form.getName());
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DUPLICATION_DUPLICATE_ENTITY_FOUND,
									IMarker.PRIORITY_NORMAL, null, o);
							}
						}
					}
					else
					{
						duplicates = new HashSet<IPersist>();
						duplicates.add(o);
					}
					formElementsByName.put(((ISupportName)o).getName(), duplicates);
				}
				if (o instanceof IScriptElement)
				{
					Set<IPersist> duplicates = formScriptProviderByName.get(((IScriptElement)o).getName());
					if (duplicates != null)
					{
						String type = "method";
						String otherChildsType = "method";
						for (IPersist duplicatePersist : duplicates)
						{
							int lineNumber = ((IScriptElement)o).getLineNumberOffset();
							if (o instanceof ScriptVariable)
							{
								otherChildsType = "variable";
							}
							if (duplicatePersist instanceof ScriptVariable)
							{
								type = "variable";
							}
							ServoyMarker mk = MarkerMessages.DuplicateEntityFound.fill(type, ((IScriptElement)o).getName(), form.getName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), lineNumber,
								ServoyBuilder.DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, o);

							lineNumber = ((IScriptElement)duplicatePersist).getLineNumberOffset();
							mk = MarkerMessages.DuplicateEntityFound.fill(otherChildsType, ((IScriptElement)o).getName(), form.getName());
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), lineNumber,
								ServoyBuilder.DUPLICATION_DUPLICATE_ENTITY_FOUND, IMarker.PRIORITY_NORMAL, null, duplicatePersist);
						}
					}
					else
					{
						duplicates = new HashSet<IPersist>();
						duplicates.add(o);
					}
					formScriptProviderByName.put(((IScriptElement)o).getName(), duplicates);
				}
				if (o instanceof Tab)
				{
					Tab tab = (Tab)o;
					FlattenedSolution tabFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(tab, fs);
					if (tab.getRelationName() != null)
					{
						Relation[] relations = tabFlattenedSolution.getRelationSequence(tab.getRelationName());
						if (relations == null)
						{
							if (Utils.getAsUUID(tab.getRelationName(), false) != null)
							{
								// relation name was not resolved from uuid to relation name during import
								ServoyMarker mk = MarkerMessages.FormRelatedTabUnsolvedUuid.fill(tab.getRelationName());
								IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_RELATED_TAB_UNSOLVED_UUID,
									IMarker.PRIORITY_NORMAL, null, tab);
								if (marker != null)
								{
									try
									{
										marker.setAttribute("Uuid", o.getUUID().toString());
										marker.setAttribute("SolutionName", ((Solution)tab.getAncestor(IRepository.SOLUTIONS)).getName());
										marker.setAttribute("PropertyName", "relationName");
										marker.setAttribute("DisplayName", RepositoryHelper.getDisplayName("relationName", o.getClass()));
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
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_RELATED_TAB_UNSOLVED_RELATION,
									IMarker.PRIORITY_NORMAL, null,
									tab);
							}
						}
						else
						{
							Relation relation = relations[0];
							if (!relation.isGlobal() && relation.getPrimaryDataSource() != null)
							{
								if (context instanceof Form && (!relation.getPrimaryDataSource().equals(((Form)context).getDataSource())))
								{
									ServoyMarker mk = MarkerMessages.FormRelatedTabDifferentTable.fill(((Form)context).getName(), relation.getName());
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_RELATED_TAB_DIFFERENT_TABLE,
										IMarker.PRIORITY_NORMAL, null,
										tab);
								}
							}
							relation = relations[relations.length - 1];
							if (!relation.isGlobal() && relation.getForeignDataSource() != null)
							{
								Form form = tabFlattenedSolution.getForm(tab.getContainsFormID());
								if (form != null && !relation.getForeignDataSource().equals(form.getDataSource()))
								{
									ServoyMarker mk = MarkerMessages.FormRelatedTabDifferentTable.fill(form.getName(), relation.getName());
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_RELATED_TAB_DIFFERENT_TABLE,
										IMarker.PRIORITY_NORMAL,
										null,
										tab);
								}
							}
							if (context instanceof Form)
							{
								for (Relation r : relations)
								{
									ServoyBuilder.addEncapsulationMarker(markerResource, servoyProject.getProject(), tab, r, (Form)context);
									if (r != null)
									{
										BuilderDependencies.getInstance().addDependency(form, fs.getRelation(r.getName()));
									}
								}
							}
						}
					}
					if (tab.getImageMediaID() > 0)
					{
						Media media = tabFlattenedSolution.getMedia(tab.getImageMediaID());
						if (media != null)
						{
							BuilderDependencies.getInstance().addDependency(form, fs.getMedia(media.getID()));
							ImageIcon mediaImageIcon = new ImageIcon(media.getMediaData());
							if (mediaImageIcon.getIconWidth() > 20 || mediaImageIcon.getIconHeight() > 20)
							{
								String formName = ((Form)tab.getAncestor(IRepository.FORMS)).getName();
								String tabPanelName = ((TabPanel)tab.getAncestor(IRepository.TABPANELS)).getName();
								String tabName = tab.getName();

								ServoyMarker mk = MarkerMessages.FormTabPanelTabImageTooLarge.fill(tabName != null ? tabName : "",
									tabPanelName != null ? tabPanelName : "", formName != null ? formName : "");
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_TABPANEL_TAB_IMAGE_TOO_LARGE,
									IMarker.PRIORITY_NORMAL, null,
									tab);
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
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.SOLUTION_ELEMENT_NAME_INVALID_IDENTIFIER,
							IMarker.PRIORITY_LOW, null, o);
					}
				}
				if (o instanceof Field)
				{
					Field field = (Field)o;
					FlattenedSolution fieldFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(field, fs);
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
							// always add as reference the normal persist, not editing one
							BuilderDependencies.getInstance().addDependency(form, fs.getValueList(field.getValuelistID()));
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
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
										ServoyBuilder.FORM_EDITABLE_COMBOBOX_CUSTOM_VALUELIST, IMarker.PRIORITY_NORMAL,
										null, field);
								}
							}
							if (type == Field.TEXT_FIELD || type == Field.TYPE_AHEAD)
							{
								boolean vlWithUnstoredCalcError = false;
								if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES)
								{
									ITable table = null;
									try
									{
										if (vl.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
										{
											table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(vl.getDataSource());
										}
										else if (vl.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES && vl.getRelationName() != null)
										{
											Relation[] relations = fieldFlattenedSolution.getRelationSequence(vl.getRelationName());
											if (relations != null)
											{
												table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
													relations[relations.length - 1].getForeignDataSource());
											}
										}
									}
									catch (Exception e)
									{
										ServoyLog.logError(e);
									}
									vlWithUnstoredCalcError = /**/isUnstoredCalc(vl.getDataProviderID1(), table, fieldFlattenedSolution) || //
										isUnstoredCalc(vl.getDataProviderID2(), table, fieldFlattenedSolution) || //
										isUnstoredCalc(vl.getDataProviderID3(), table, fieldFlattenedSolution);
								}
								if (!vlWithUnstoredCalcError && vl.getFallbackValueListID() > 0)
								{
									ValueList fallback = fieldFlattenedSolution.getValueList(vl.getFallbackValueListID());
									if (fallback != null && fallback.getValueListType() == IValueListConstants.DATABASE_VALUES)
									{
										BuilderDependencies.getInstance().addDependency(form, fs.getValueList(vl.getFallbackValueListID()));
										ITable table = null;
										try
										{
											if (fallback.getDatabaseValuesType() == IValueListConstants.TABLE_VALUES)
											{
												table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
													fallback.getDataSource());
											}
											else if (fallback.getDatabaseValuesType() == IValueListConstants.RELATED_VALUES &&
												fallback.getRelationName() != null)
											{
												Relation[] relations = fieldFlattenedSolution.getRelationSequence(fallback.getRelationName());
												if (relations != null)
												{
													table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
														relations[relations.length - 1].getForeignDataSource());
												}
											}
										}
										catch (Exception e)
										{
											ServoyLog.logError(e);
										}
										vlWithUnstoredCalcError = /**/isUnstoredCalc(fallback.getDataProviderID1(), table, fieldFlattenedSolution) || //
											isUnstoredCalc(fallback.getDataProviderID2(), table, fieldFlattenedSolution) || //
											isUnstoredCalc(fallback.getDataProviderID3(), table, fieldFlattenedSolution);
									}
								}
								if (vlWithUnstoredCalcError)
								{
									ServoyMarker mk = MarkerMessages.FormTypeAheadNamedUnstoredCalculation.fill(fieldName);
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_TYPEAHEAD_UNSTORED_CALCULATION,
										IMarker.PRIORITY_NORMAL,
										null, field);
								}
							}
							String parentDataSource = null;
							String parentString = null;
							if (field.getParent() instanceof Portal)
							{
								Relation[] relations = fieldFlattenedSolution.getRelationSequence(((Portal)field.getParent()).getRelationName());
								if (relations != null && relations.length > 0)
								{
									parentDataSource = relations[relations.length - 1].getForeignDataSource();
								}
								parentString = "portal \"" + ((Portal)field.getParent()).getName() + "\"";
							}
							else
							{
								parentDataSource = form.getDataSource();
								parentString = "form \"" + form.getName() + "\"";
							}
							if (parentDataSource != null)
							{
								if (vl.getValueListType() == IValueListConstants.DATABASE_VALUES && vl.getRelationName() != null)
								{
									String[] parts = vl.getRelationName().split("\\.");
									Relation relation = fieldFlattenedSolution.getRelation(parts[0]);
									if (relation != null && !relation.isGlobal())
									{
										boolean addMarker = !relation.getPrimaryDataSource().equals(parentDataSource);
										if (addMarker && field.getDataProviderID() != null)
										{
											int index = field.getDataProviderID().lastIndexOf('.');
											if (index > 0)
											{
												Relation[] dpRelations = fieldFlattenedSolution.getRelationSequence(
													field.getDataProviderID().substring(0, index));
												if (dpRelations != null && dpRelations.length > 0 &&
													dpRelations[dpRelations.length - 1].getForeignDataSource().equals(relation.getPrimaryDataSource()))
												{
													addMarker = false;
												}
											}
										}
										if (addMarker)
										{
											ServoyMarker mk = MarkerMessages.FormNamedFieldRelatedValuelist.fill(fieldName, vl.getName(), parentString);
											ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FIELD_RELATED_VALUELIST,
												IMarker.PRIORITY_NORMAL,
												null, field);
										}
									}
								}
								if (vl.getFallbackValueListID() > 0)
								{
									ValueList fallback = fieldFlattenedSolution.getValueList(vl.getFallbackValueListID());
									if (fallback != null && fallback.getValueListType() == IValueListConstants.DATABASE_VALUES &&
										fallback.getRelationName() != null)
									{
										BuilderDependencies.getInstance().addDependency(form, fs.getValueList(vl.getFallbackValueListID()));
										String[] parts = fallback.getRelationName().split("\\.");
										Relation relation = fieldFlattenedSolution.getRelation(parts[0]);
										if (relation != null && !relation.isGlobal() && !relation.isLiteral() &&
											!relation.getPrimaryDataSource().equals(parentDataSource))
										{
											ServoyMarker mk = MarkerMessages.FormNamedFieldFallbackRelatedValuelist.fill(fieldName, vl.getName(),
												fallback.getName(), parentString);
											ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FIELD_RELATED_VALUELIST,
												IMarker.PRIORITY_NORMAL,
												null, field);
										}
										if (relation != null) BuilderDependencies.getInstance().addDependency(form, fs.getRelation(relation.getName()));
									}
								}
							}
							if (vl.getRelationName() != null)
							{
								Relation[] relations = fs.getRelationSequence(vl.getRelationName());
								if (relations != null && context instanceof Form)
								{
									for (Relation r : relations)
									{
										ServoyBuilder.addEncapsulationMarker(markerResource, servoyProject.getProject(), field, r, (Form)context);
										if (r != null) BuilderDependencies.getInstance().addDependency(form, r);
									}
								}
							}
						}
					}
					else if (type == Field.LIST_BOX || type == Field.MULTISELECT_LISTBOX || type == Field.SPINNER)
					{
						// these types of fields MUST have a valuelist
						ServoyMarker mk = MarkerMessages.RequiredPropertyMissingOnElement.fill("valuelist", fieldName, form.getName());
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_REQUIRED_PROPERTY_MISSING,
							IMarker.PRIORITY_NORMAL, null, field);
					}
				}
				if (o instanceof Portal && ((Portal)o).getRelationName() != null)
				{
					Portal portal = (Portal)o;
					FlattenedSolution portalFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(portal, fs);
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
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_PORTAL_INVALID_RELATION_NAME,
							IMarker.PRIORITY_NORMAL, null, o);
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
												ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
													ServoyBuilder.FORM_PORTAL_INVALID_RELATION_NAME,
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
								ServoyBuilder.addEncapsulationMarker(markerResource, servoyProject.getProject(), portal, r, (Form)context);
								if (r != null) BuilderDependencies.getInstance().addDependency(form, fs.getRelation(r.getName()));
							}
						}
					}
				}
				if (o instanceof GraphicalComponent && ((GraphicalComponent)o).getLabelFor() != null &&
					!"".equals(((GraphicalComponent)o).getLabelFor()))
				{
					IPersist parent = null;
					if (o.getParent() instanceof Form)
					{
						parent = ServoyBuilder.getPersistFlattenedSolution(o, fs).getFlattenedForm(o);
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
								if (persist instanceof ISupportName && ((GraphicalComponent)o).getLabelFor().equals(((ISupportName)persist).getName()))
									return persist;
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
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_LABEL_FOR_ELEMENT_NOT_FOUND,
								IMarker.PRIORITY_NORMAL, null, o);
						}
					}
				}
				if (o instanceof GraphicalComponent && ((GraphicalComponent)o).getRolloverImageMediaID() > 0 &&
					((GraphicalComponent)o).getImageMediaID() <= 0)
				{
					ServoyMarker mk = MarkerMessages.ImageMediaNotSet.fill(((Form)o.getAncestor(IRepository.FORMS)).getName());
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.IMAGE_MEDIA_NOT_SET, IMarker.PRIORITY_NORMAL, null,
						o);
				}
				if (o instanceof GraphicalComponent &&
					(((GraphicalComponent)o).getRolloverImageMediaID() > 0 || ((GraphicalComponent)o).getRolloverCursor() > 0))
				{
					ServoyProject activeProject = ServoyModelFinder.getServoyModel().getActiveProject();
					if (activeProject != null && (activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SMART_CLIENT_ONLY ||
						activeProject.getSolutionMetaData().getSolutionType() == SolutionMetaData.SOLUTION))
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
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.ROLLOVER_NOT_WORKING,
									IMarker.PRIORITY_NORMAL, null, o);
							}
						}
					}

				}
				if (o instanceof GraphicalComponent)
				{
					if (((GraphicalComponent)o).getImageMediaID() > 0)
					{
						Media media = fs.getMedia(((GraphicalComponent)o).getImageMediaID());
						if (media != null)
						{
							BuilderDependencies.getInstance().addDependency(form, media);
						}
					}
					if (((GraphicalComponent)o).getRolloverImageMediaID() > 0)
					{
						Media media = fs.getMedia(((GraphicalComponent)o).getRolloverImageMediaID());
						if (media != null)
						{
							BuilderDependencies.getInstance().addDependency(form, media);
						}
					}
				}
				ServoyBuilder.checkSpecs(o, servoyProject.getProject(), WebComponentSpecProvider.getSpecProviderState());

				if (o.getTypeID() == IRepository.SHAPES)
				{
					ServoyMarker mk = MarkerMessages.ObsoleteElement.fill(((Form)o.getAncestor(IRepository.FORMS)).getName());
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_OBSOLETE_ELEMENT, IMarker.PRIORITY_NORMAL, null,
						o);
				}
				ServoyBuilderUtils.addMobileReservedWordsVariable(servoyProject.getProject(), o);
				ServoyBuilder.checkDeprecatedElementUsage(o, markerResource, fs);
				ServoyBuilder.checkDeprecatedPropertyUsage(o, markerResource, servoyProject.getProject());

				if (((Form)context).isFormComponent().booleanValue() && o instanceof IScriptElement)
				{
					ServoyMarker mk = MarkerMessages.FormReferenceInvalidScript.fill(((Form)context).getName(), ((IScriptElement)o).getName());
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), ((IScriptElement)o).getLineNumberOffset(),
						ServoyBuilder.FORM_REFERENCE_INVALID_SCRIPT,
						IMarker.PRIORITY_NORMAL, null, o);
				}
				if (o instanceof AbstractBase &&
					Boolean.TRUE.equals(((AbstractBase)o).getCustomMobileProperty(IMobileProperties.HEADER_LEFT_BUTTON.propertyName)))
				{
					IPersist parentForm = o.getAncestor(IRepository.FORMS);
					if (parentForm != null && ((Form)parentForm).getNavigatorID() != Form.NAVIGATOR_NONE)
					{
						ServoyMarker mk = MarkerMessages.MobileFormNavigatorOverlapsHeaderButton.fill();
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.MOBILE_NAVIGATOR_OVERLAPS_HEADER_BUTTON,
							IMarker.PRIORITY_NORMAL, null, o);
					}
				}
				if (o instanceof ISupportExtendsID && PersistHelper.isOverrideOrphanElement((ISupportExtendsID)o))
				{
					IPersist parentForm = o.getAncestor(IRepository.FORMS);
					ServoyMarker mk = MarkerMessages.ElementExtendsDeletedElement.fill(parentForm);
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.ELEMENT_EXTENDS_DELETED_ELEMENT,
						IMarker.PRIORITY_NORMAL, null, o);
				}
				if (checkStyleClass)
				{
					if (styleName != null)
					{
						if (o instanceof BaseComponent || o instanceof Form || o instanceof Part)
						{
							String styleClass = null;
							if (o instanceof BaseComponent) styleClass = ((BaseComponent)o).getStyleClass();
							else if (o instanceof Form) styleClass = ((Form)o).getStyleClass();
							else if (o instanceof Part) styleClass = ((Part)o).getStyleClass();
							if (styleClass != null)
							{
								List<String> styleClasses = Arrays.asList(ModelUtils.getStyleClasses(fs, form, o,
									StaticContentSpecLoader.PROPERTY_STYLECLASS.getPropertyName(), ModelUtils.getStyleLookupname(o), true)
									.getLeft());
								if (!styleClasses.contains(styleClass))
								{
									ServoyMarker mk = MarkerMessages.StyleFormClassNotFound.fill(styleClass, form.getName());
									IMarker marker = null;
									if (o instanceof Part)
									{
										mk = MarkerMessages.StyleElementClassNotFound.fill(styleClass, Part.getDisplayName(((Part)o).getPartType()),
											form.getName());
										marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.STYLE_CLASS_NOT_FOUND,
											IMarker.PRIORITY_NORMAL,
											null, o);
									}
									else if (o instanceof ISupportName && !(o instanceof Form) && (((ISupportName)o).getName() != null))
									{
										mk = MarkerMessages.StyleElementClassNotFound.fill(styleClass, ((ISupportName)o).getName(), form.getName());
										marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.STYLE_CLASS_NOT_FOUND,
											IMarker.PRIORITY_NORMAL,
											null, o);
									}
									else marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.STYLE_CLASS_NOT_FOUND,
										IMarker.PRIORITY_NORMAL,
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
					}
					else
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
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.STYLE_CLASS_NO_STYLE,
										IMarker.PRIORITY_NORMAL, null, o);
								}
								else ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.STYLE_CLASS_NO_STYLE,
									IMarker.PRIORITY_NORMAL, null, o);
							}
						}
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
	}

	public static void addWebComponentMissingReferences(IResource markerResource, FlattenedSolution flattenedSolution, IPersist o, Form form, String datasource)
	{
		if (o instanceof WebComponent)
		{
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(((WebComponent)o).getTypeName());
			if (spec != null && spec.getHandlers() != null)
			{
				for (String handler : spec.getHandlers().keySet())
				{
					UUID uuid = Utils.getAsUUID(((WebComponent)o).getProperty(handler), false);
					if (uuid != null)
					{
						ScriptMethod scriptMethod = flattenedSolution.getScriptMethod(uuid.toString());
						if (scriptMethod == null)
						{
							ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(handler, ((WebComponent)o).getName(),
								form);
							IMarker marker = ServoyBuilder.addMarker(markerResource, ServoyBuilder.INVALID_EVENT_METHOD, mk.getText(), -1,
								ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
								IMarker.PRIORITY_LOW, null, o);
							if (marker != null)
							{
								try
								{
									marker.setAttribute("EventName", handler);
								}
								catch (Exception ex)
								{
									ServoyLog.logError(ex);
								}
							}
						}
						else
						{
							if (scriptMethod.getParent() instanceof Form)
							{
								List<Form> hierarchy = ServoyModelFinder.getServoyModel().getFlattenedSolution().getFormHierarchy(form);
								if (!hierarchy.contains(scriptMethod.getParent()))
								{
									ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(handler, ((WebComponent)o).getName(),
										form);
									IMarker marker = ServoyBuilder.addMarker(markerResource, ServoyBuilder.INVALID_EVENT_METHOD, mk.getText(), -1,
										ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
										IMarker.PRIORITY_LOW, null, o);
									if (marker != null)
									{
										try
										{
											marker.setAttribute("EventName", handler);
										}
										catch (Exception ex)
										{
											ServoyLog.logError(ex);
										}
									}
								}
							}
							else if (scriptMethod.getParent() instanceof TableNode &&
								!Utils.equalObjects(datasource, ((TableNode)scriptMethod.getParent()).getDataSource()))
							{
								ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(handler, ((WebComponent)o).getName(),
									form);
								IMarker marker = ServoyBuilder.addMarker(markerResource, ServoyBuilder.INVALID_EVENT_METHOD, mk.getText(), -1,
									ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
									IMarker.PRIORITY_LOW, null, o);
								if (marker != null)
								{
									try
									{
										marker.setAttribute("EventName", handler);
									}
									catch (Exception ex)
									{
										ServoyLog.logError(ex);
									}
								}
							}

							BuilderDependencies.getInstance().addDependency(scriptMethod.getScopeName(), form);
						}
					}
					WebObjectFunctionDefinition handlerDefinition = spec.getHandler(handler);
					List<Object> instanceMethodArguments = ((WebComponent)o).getFlattenedMethodArguments(handlerDefinition.getName());
					if (instanceMethodArguments != null && instanceMethodArguments.size() > 0 &&
						handlerDefinition.getParameters().getDefinedArgsCount() >= instanceMethodArguments.size())
					{
						ServoyMarker mk = MarkerMessages.Parameters_Mismatch.fill(((WebComponent)o).getName(), handler);
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.PARAMETERS_MISMATCH_SEVERITY,
							IMarker.PRIORITY_NORMAL,
							null, o);
					}
					if (uuid != null && handlerDefinition.isDeprecated())
					{
						ServoyMarker mk = MarkerMessages.DeprecatedHandler.fill(handler,
							"web component" + (((WebComponent)o).getName() != null ? " with name '" + ((WebComponent)o).getName() + "'" : "'"),
							handlerDefinition.getDeprecatedMessage());
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
							ServoyBuilder.DEPRECATED_HANDLER, IMarker.PRIORITY_NORMAL, null, o);
					}
				}

			}
			if (spec != null)
			{
				addWebComponentMissingReferences((WebComponent)o, spec, ((WebComponent)o).getFlattenedJson(), flattenedSolution, form, markerResource);
			}
		}
	}

	private static void addWebComponentMissingReferences(WebComponent wc, PropertyDescription spec, JSONObject json, FlattenedSolution flattenedSolution,
		Form form, IResource markerResource)
	{
		if (spec != null && json != null)
		{
			Map<String, PropertyDescription> properties = spec.getProperties();
			if (properties != null)
			{
				for (PropertyDescription pd : properties.values())
				{
					Object value = json.opt(pd.getName());
					if (value != null && !"".equals(value) && value != JSONObject.NULL)
					{
						if (pd.getType() instanceof MediaPropertyType)
						{
							Media media = flattenedSolution.getMedia(value.toString());
							if (media != null)
							{
								BuilderDependencies.getInstance().addDependency(form, media);
							}
							else
							{
								ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(pd.getName(), wc.getName(),
									form);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
									IMarker.PRIORITY_LOW, null, wc);
							}
						}
						else if (pd.getType() instanceof ValueListPropertyType)
						{
							ValueList vl = flattenedSolution.getValueList(value.toString());
							if (vl != null)
							{
								BuilderDependencies.getInstance().addDependency(form, vl);
							}
							else
							{
								ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(pd.getName(), wc.getName(),
									form);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
									IMarker.PRIORITY_LOW, null, wc);
							}
						}
						else if (pd.getType() instanceof FunctionPropertyType)
						{
							ScriptMethod scriptMethod = null;
							int methodId = Utils.getAsInteger(value);
							if (methodId > 0)
							{
								scriptMethod = flattenedSolution.getScriptMethod(methodId);
							}
							else if (value instanceof String)
							{
								scriptMethod = flattenedSolution.getScriptMethod((String)value);
							}
							if (scriptMethod == null)
							{
								ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(pd.getName(), wc.getName(),
									form);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
									IMarker.PRIORITY_LOW, null, wc);
							}
						}
						else if (pd.getType() instanceof RelationPropertyType)
						{
							Relation[] relations = flattenedSolution.getRelationSequence(value.toString());
							if (relations != null)
							{
								for (Relation relationObj : relations)
								{
									if (relationObj != null)
									{
										BuilderDependencies.getInstance().addDependency(form, relationObj);
									}
								}
							}
							else
							{
								ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(pd.getName(), wc.getName(),
									form);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
									IMarker.PRIORITY_LOW, null, wc);
							}
						}
						else if (pd.getType() instanceof FormPropertyType)
						{
							Form frm = flattenedSolution.getForm(value.toString());
							if (frm != null)
							{
								BuilderDependencies.getInstance().addDependency(form, frm);
							}
							else
							{
								ServoyMarker mk = MarkerMessages.PropertyOnElementInFormTargetNotFound.fill(pd.getName(), wc.getName(),
									form);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
									ServoyBuilder.FORM_PROPERTY_TARGET_NOT_FOUND,
									IMarker.PRIORITY_LOW, null, wc);
							}
						}
						else if (pd.getType() instanceof CustomJSONPropertyType< ? >)
						{
							if (value instanceof JSONObject)
							{
								addWebComponentMissingReferences(wc, ((CustomJSONPropertyType)pd.getType()).getCustomJSONTypeDefinition(), (JSONObject)value,
									flattenedSolution, form, markerResource);
							}
							else if (value instanceof JSONArray)
							{
								JSONArray arr = ((JSONArray)value);
								for (int i = 0; i < arr.length(); i++)
								{
									if (arr.get(i) instanceof JSONObject)
									{
										addWebComponentMissingReferences(wc, ((CustomJSONPropertyType)pd.getType()).getCustomJSONTypeDefinition(),
											(JSONObject)arr.get(i), flattenedSolution, form, markerResource);
									}
								}
							}
						}
					}
				}
			}
		}
	}

	public static void addFormVariablesHideTableColumn(IResource markerResource, Form form, ITable table)
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
					ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_VARIABLE_TYPE_COL, IMarker.PRIORITY_NORMAL, null,
						var);
				}
			}
		}
	}

	public static void checkVariants(IResource markerResource, ServoyProject project, final IPersist o, IPersist context, String datasource,
		FlattenedSolution flattenedSolution)
	{

		if (o instanceof WebComponent webComp)
		{
			Collection<PropertyDescription> properties = new ArrayList<PropertyDescription>();
			WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(((WebComponent)o).getTypeName());
			if (spec != null)
			{
				properties.addAll(spec.getProperties().values());
				for (PropertyDescription pd : properties)
				{
					if (pd.getType() instanceof VariantPropertyType)
					{
						Object propertyValue = ((IBasicWebObject)o).getProperty(pd.getName());
						if (propertyValue != null)
						{
							String elementName = null;
							if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
							{
								elementName = ((ISupportName)o).getName();
							}
							BuilderDependencies.getInstance().addVariantDependency((Form)context);
							if (!flattenedSolution.getVariantsHandler().variantExists(propertyValue.toString()))
							{
								try
								{
									ServoyMarker mk = MarkerMessages.VariantIdUnresolved.fill(elementName != null ? elementName : "UNNAMED",
										((Form)context).getName());
									IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
										ServoyBuilder.VARIANT_ID_UNRESOLVED,
										IMarker.PRIORITY_NORMAL, null, o);
									//							marker.setAttribute("Uuid", valuelistUUID.toString());
									marker.setAttribute("SolutionName", flattenedSolution.getName());
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
		}
	}

	public static void checkDataProviders(IResource markerResource, ServoyProject project, final IPersist o, IPersist context, String datasource,
		FlattenedSolution flattenedSolution)
	{
		String id = null;
		if (o instanceof ISupportDataProviderID)
		{
			id = ((ISupportDataProviderID)o).getDataProviderID();
		}
		else if (o instanceof WebComponent || o instanceof WebCustomType)
		{
			Collection<PropertyDescription> dpProperties = new ArrayList<PropertyDescription>();

			if (o instanceof WebComponent)
			{
				WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(((WebComponent)o).getTypeName());
				if (spec != null)
				{
					dpProperties.addAll(spec.getProperties().values());
				}
			}
			else
			{
				WebCustomType customType = (WebCustomType)o;
				WebComponent parent = (WebComponent)customType.getAncestor(IRepository.WEBCOMPONENTS);
				WebObjectSpecification parentSpec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(parent.getTypeName());
				if (parentSpec != null)
				{
					PropertyDescription cpd = ((ICustomType< ? >)parentSpec.getDeclaredCustomObjectTypes().get(
						customType.getTypeName())).getCustomJSONTypeDefinition();
					if (cpd != null)
					{
						dpProperties.addAll(cpd.getProperties().values());
					}
				}
			}

			for (PropertyDescription pd : dpProperties)
			{
				if (pd.getType() instanceof DataproviderPropertyType || pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? > ||
					pd.getType() instanceof FoundsetPropertyType)
				{

					Object propertyValue = ((IBasicWebObject)o).getProperty(pd.getName());
					if (propertyValue != null)
					{
						if ((pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? > &&
							((FoundsetLinkedConfig)pd.getConfig()).getWrappedPropertyDescription().getType() instanceof TagStringPropertyType))
						{
							TagStringPropertyType wrappedPd = (TagStringPropertyType)((FoundsetLinkedConfig)pd.getConfig())
								.getWrappedPropertyDescription().getType();
							TargetDataLinks links = wrappedPd.getDataLinks((String)propertyValue, pd, flattenedSolution,
								new FormElement((IFormElement)o.getParent(), flattenedSolution, new PropertyPath(), true));
							if (!TargetDataLinks.NOT_LINKED_TO_DATA.equals(links))
							{
								for (String dp : links.dataProviderIDs)
								{
									checkDataProvider(markerResource, project, flattenedSolution, o, context, dp, pd, datasource);
								}
							}
							continue;
						}
						else if (pd.getType() instanceof FoundsetPropertyType)
						{
							if (propertyValue instanceof JSONObject)
							{
								JSONObject val = (JSONObject)propertyValue;
								FlattenedSolution persistFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(o, flattenedSolution);

								//first check if the foundset is valid
								boolean invalid = false;
								String fs = val.optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
								if (!"".equals(fs)) //Form foundset, no need to check
								{
									if (DataSourceUtils.isDatasourceUri(fs))
									{
										ITable table = persistFlattenedSolution.getTable(fs);
										invalid = table == null;
										if (table != null && context instanceof Form)
										{
											Form form = (Form)context;
											Iterator<ScriptVariable> iterator = form.getScriptVariables(false);
											while (iterator.hasNext())
											{
												ScriptVariable var = iterator.next();
												if (table.getColumn(var.getName()) != null)
												{
													ServoyMarker mk = MarkerMessages.FormVariableTableColFromComponent.fill(form.getName(),
														var.getName(), table.getName(),
														((ISupportName)o).getName() != null ? ((ISupportName)o).getName() : "", pd.getName());
													ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1,
														ServoyBuilder.FORM_VARIABLE_TYPE_COL,
														IMarker.PRIORITY_NORMAL, null, o);
												}
											}
										}
									}
									else
									{
										Relation[] relationSequence = persistFlattenedSolution.getRelationSequence(fs);
										invalid = relationSequence == null;
										if (invalid)
										{
											if (flattenedSolution.getFormsForNamedFoundset(Form.NAMED_FOUNDSET_SEPARATE_PREFIX + fs).size() > 0)
											{
												invalid = false;
											}
										}
										else
										{
											for (Relation r : relationSequence)
											{
												if (r != null) BuilderDependencies.getInstance().addDependency(context,
													ServoyModelFinder.getServoyModel().getFlattenedSolution().getRelation(r.getName()));
											}
										}
									}
									if (invalid)
									{
										String comp_name = ((ISupportName)o).getName() != null ? ((ISupportName)o).getName() : "";
										ServoyMarker mk = MarkerMessages.ComponentInvalidFoundset.fill(fs, comp_name);
										ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.COMPONENT_FOUNDSET_INVALID,
											IMarker.PRIORITY_NORMAL,
											null, o);
										continue;
									}
								}
								if (val.opt("dataproviders") instanceof JSONObject)
								{
									JSONObject dataproviders = val.getJSONObject("dataproviders");
									for (String dp : dataproviders.keySet())
									{
										checkDataProvider(markerResource, project, flattenedSolution, o, context, dataproviders.optString(dp), pd, datasource);
									}
								}
							}
						}
						else if (pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? > &&
							pd.getConfig() instanceof FoundsetLinkedConfig &&
							(((FoundsetLinkedConfig)pd.getConfig()).getWrappedPropertyDescription().getType() instanceof ValueListPropertyType))
						{
							continue;
						}
						else
						{
							checkDataProvider(markerResource, project, flattenedSolution, o, context, (String)propertyValue, pd, datasource);
						}
					}
				}
			}

		}
		checkDataProvider(markerResource, project, flattenedSolution, o, context, id, null, datasource);

	}

	private static void checkDataProvider(IResource markerResource, ServoyProject project, FlattenedSolution flattenedSolution, final IPersist o,
		IPersist context, String id,
		PropertyDescription pd, String datasource)
	{
		try
		{
			if (id != null && !"".equals(id))
			{
				if (!(context instanceof Form))
				{
					ServoyLog.logError("Could not find parent form for element " + o, null);
				}
				else
				{
					Form parentForm = (Form)context;
					FlattenedSolution persistFlattenedSolution = ServoyBuilder.getPersistFlattenedSolution(context, flattenedSolution);
					IDataProvider dataProvider = persistFlattenedSolution.getDataProviderForTable(
						ServoyModelFinder.getServoyModel().getDataSourceManager()
							.getDataSource(datasource != null ? datasource : parentForm.getDataSource()),
						id);
					if (dataProvider == null)
					{
						Form flattenedForm = persistFlattenedSolution.getFlattenedForm(context);
						if (flattenedForm != null)
						{
							dataProvider = flattenedForm.getScriptVariable(id);
						}
					}
					if (dataProvider == null)
					{
						try
						{
							dataProvider = persistFlattenedSolution.getGlobalDataProvider(id, false);
							if (dataProvider instanceof ISupportScope)
							{
								BuilderDependencies.getInstance().addDependency(((ISupportScope)dataProvider).getScopeName(), context);
							}
						}
						catch (Exception e)
						{
							ServoyLog.logError(e);
						}
					}
					if (dataProvider == null && o.getParent() instanceof WebComponent)
					{
						WebComponent parent = (WebComponent)o.getParent();
						dataProvider = checkComponentDataproviders(id, persistFlattenedSolution, parent);
					}
					if (dataProvider == null && o instanceof WebComponent)
					{
						dataProvider = checkComponentDataproviders(id, persistFlattenedSolution, (WebComponent)o);
					}

					String elementName = null;
					String inForm = null;
					if (o instanceof ISupportName && ((ISupportName)o).getName() != null)
					{
						elementName = ((ISupportName)o).getName();
					}
					inForm = parentForm.getName();


					// check for valuelist type matching dataprovider type in web components
					if ((o instanceof WebComponent || o instanceof WebCustomType) && dataProvider != null)
					{
						Collection<PropertyDescription> dpProperties = new ArrayList<PropertyDescription>();

						if (o instanceof WebComponent)
						{
							WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
								((WebComponent)o).getTypeName());
							if (spec != null)
							{
								dpProperties.addAll(spec.getProperties().values());
							}
						}
						else
						{
							WebCustomType customType = (WebCustomType)o;
							WebComponent parent = (WebComponent)customType.getAncestor(IRepository.WEBCOMPONENTS);
							WebObjectSpecification parentSpec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(
								parent.getTypeName());
							if (parentSpec != null)
							{
								PropertyDescription cpd = ((ICustomType< ? >)parentSpec.getDeclaredCustomObjectTypes().get(
									customType.getTypeName())).getCustomJSONTypeDefinition();
								if (cpd != null)
								{
									dpProperties.addAll(cpd.getProperties().values());
								}
							}
						}

						for (PropertyDescription pd1 : dpProperties)
						{
							if (pd1.getType() instanceof ValueListPropertyType &&
								pd.getName().equals(((ValueListConfig)pd1.getConfig()).getFor()))
							{
								Object valuelistUUID = ((AbstractBase)o).getProperty(pd1.getName());
								if (valuelistUUID != null)
								{
									ValueList valuelist = (ValueList)flattenedSolution.searchPersist(valuelistUUID.toString());
									if (valuelist != null)
									{
										BuilderDependencies.getInstance().addDependency(parentForm, valuelist);
										checkValueListRealValueToDataProviderTypeMatch(project.getProject(), valuelist, dataProvider, elementName, inForm,
											o, valuelistUUID);
									}
								}
							}
						}

					}
					if ((o instanceof Field || o instanceof GraphicalComponent) && dataProvider != null)
					{
						// check for valuelist type matching dataprovider type
						int valuelistID = o instanceof Field ? ((Field)o).getValuelistID() : ((GraphicalComponent)o).getValuelistID();
						ValueList valuelist = persistFlattenedSolution.getValueList(valuelistID);
						if (valuelist != null)
						{
							BuilderDependencies.getInstance().addDependency(parentForm, valuelist);
							checkValueListRealValueToDataProviderTypeMatch(project.getProject(), valuelist, dataProvider, elementName, inForm,
								o,
								valuelist.getUUID());
						}

						String format = (o instanceof Field) ? ((Field)o).getFormat() : ((GraphicalComponent)o).getFormat();
						if (o instanceof Field && ((Field)o).getDisplayType() != Field.TEXT_FIELD &&
							((Field)o).getDisplayType() != Field.TYPE_AHEAD && ((Field)o).getDisplayType() != Field.CALENDAR)
						{
							format = null;
						}
						if (format != null && format.length() > 0)
						{
							ParsedFormat parsedFormat = FormatParser.parseFormatProperty(format);
							int dataType = ServoyBuilder.getDataType(project.getProject(), dataProvider, parsedFormat, o);
							if (parsedFormat.getDisplayFormat() != null && !parsedFormat.getDisplayFormat().startsWith("i18n:"))
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
									ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FORMAT_INVALID,
										IMarker.PRIORITY_NORMAL, null,
										o);
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
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_FORMAT_INVALID,
								IMarker.PRIORITY_NORMAL,
								null, o);
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
							if (column.getLength() < ServoyBuilder.MIN_FIELD_LENGTH && column.getLength() > 0)
							{
								ServoyMarker mk = MarkerMessages.FormColumnLengthTooSmall.fill(elementName != null ? elementName : "",
									inForm);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_COLUMN_LENGTH_TOO_SMALL,
									IMarker.PRIORITY_NORMAL,
									null, o);
							}
						}
						if (((dataProvider instanceof ScriptVariable &&
							((ScriptVariable)dataProvider).getVariableType() == IColumnTypes.MEDIA &&
							((ScriptVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
							(dataProvider instanceof AggregateVariable &&
								((AggregateVariable)dataProvider).getType() == IColumnTypes.MEDIA &&
								((AggregateVariable)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
							(dataProvider instanceof ScriptCalculation &&
								((ScriptCalculation)dataProvider).getType() == IColumnTypes.MEDIA &&
								((ScriptCalculation)dataProvider).getSerializableRuntimeProperty(IScriptProvider.TYPE) == null) ||
							(dataProvider instanceof Column &&
								Column.mapToDefaultType(((Column)dataProvider).getType()) == IColumnTypes.MEDIA) &&
								((Column)dataProvider).getColumnInfo() != null &&
								((Column)dataProvider).getColumnInfo().getConverterName() == null) &&
							field.getDisplayType() != Field.IMAGE_MEDIA)
						{
							ServoyMarker mk = MarkerMessages.FormIncompatibleElementType.fill(
								elementName != null ? elementName : field.getUUID(), inForm);
							ServoyBuilder.addMarker(project.getProject(), mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INCOMPATIBLE_ELEMENT_TYPE,
								IMarker.PRIORITY_NORMAL,
								null, o);
						}
						if (dataProvider instanceof ScriptVariable && ((ScriptVariable)dataProvider).isDeprecated())
						{
							ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedVariable.fill(((ScriptVariable)dataProvider).getName(),
								"form " + inForm, "dataProvider");
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
								IMarker.PRIORITY_NORMAL, null, o);
						}
						else if (dataProvider instanceof ScriptCalculation && ((ScriptCalculation)dataProvider).isDeprecated())
						{
							ServoyMarker mk = MarkerMessages.ElementUsingDeprecatedCalculation.fill(
								((ScriptCalculation)dataProvider).getName(), "form " + inForm, "dataProvider");
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE_PROBLEM,
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
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_DATAPROVIDER,
							IMarker.PRIORITY_LOW,
							null, o);
					}

					boolean checkIfDataproviderIsBasedOnFormTable = true;
					if (o instanceof IBasicWebObject && pd != null)
					{
						String foundsetSelector = getWebBaseObjectPropertyFoundsetSelector((IBasicWebObject)o, pd);
						if (!"".equals(foundsetSelector))
						{
							// it is not form foundset based
							checkIfDataproviderIsBasedOnFormTable = false;
						}
					}

					if (checkIfDataproviderIsBasedOnFormTable && parentForm.getDataSource() != null &&
						dataProvider instanceof ColumnWrapper)
					{
						Relation[] relations = ((ColumnWrapper)dataProvider).getRelations();
						if (relations != null && !relations[0].isGlobal() &&
							!Utils.equalObjects(datasource != null ? datasource : parentForm.getDataSource(), relations[0].getPrimaryDataSource()))
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
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_DATAPROVIDER,
								IMarker.PRIORITY_LOW,
								null, o);
						}
						if (relations != null)
						{
							for (Relation r : relations)
							{
								if (r != null) BuilderDependencies.getInstance().addDependency(context,
									ServoyModelFinder.getServoyModel().getFlattenedSolution().getRelation(r.getName()));
							}
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
						ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_DATAPROVIDER_AGGREGATE_NOT_EDITABLE,
							IMarker.PRIORITY_LOW,
							null, o);
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
							ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_INVALID_DATAPROVIDER,
								IMarker.PRIORITY_LOW,
								null, o);
						}
					}
					if (dataProvider instanceof ColumnWrapper)
					{
						Relation[] relations = ((ColumnWrapper)dataProvider).getRelations();
						if (relations != null)
						{
							for (Relation r : relations)
							{
								ServoyBuilder.addEncapsulationMarker(markerResource, project.getProject(), o, r, (Form)context);
								if (r != null) BuilderDependencies.getInstance().addDependency(context,
									ServoyModelFinder.getServoyModel().getFlattenedSolution().getRelation(r.getName()));
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


	private static void checkValueListRealValueToDataProviderTypeMatch(IResource markerResource, ValueList valuelist, IDataProvider dataProvider,
		String elementName,
		String inForm, IPersist o, Object valuelistUUID) throws CoreException
	{
		int realValueType = valuelist.getRealValueType();
		int dataProviderType = dataProvider.getDataProviderType();
		if (realValueType != 0 && realValueType != dataProviderType)
		{

			boolean isValidNumberVariable = dataProvider instanceof ScriptVariable &&
				((realValueType == IColumnTypes.INTEGER && dataProviderType == IColumnTypes.NUMBER) ||
					(realValueType == IColumnTypes.NUMBER && dataProviderType == IColumnTypes.INTEGER));

			if (!isValidNumberVariable && dataProvider.hasFlag(IBaseColumn.UUID_COLUMN))
			{
				// if the dataprovider is a uuid column then allow text or media columns.
				isValidNumberVariable = realValueType == IColumnTypes.TEXT || realValueType == IColumnTypes.MEDIA;
			}

			if (!isValidNumberVariable)
			{

				ServoyMarker mk = MarkerMessages.ValuelistDataproviderTypeMismatch.fill(valuelist.getName(), Column.getDisplayTypeString(realValueType),
					Column.getDisplayTypeString(dataProviderType), elementName != null ? elementName : "UNNAMED", inForm);
				IMarker marker = ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.VALUELIST_DATAPROVIDER_TYPE_MISMATCH,
					IMarker.PRIORITY_NORMAL, null, o);
				marker.setAttribute("Uuid", valuelistUUID.toString());
				marker.setAttribute("SolutionName", valuelist.getRootObject().getName());
			}
		}
	}

	private static String getWebBaseObjectPropertyFoundsetSelector(IBasicWebObject webObject, PropertyDescription pd)
	{
		if (pd.getType() instanceof FoundsetLinkedPropertyType< ? , ? >)
		{
			String forFoundset = ((FoundsetLinkedConfig)pd.getConfig()).getForFoundsetName();
			IBasicWebObject parent = webObject;
			while (parent.getParent() instanceof IBasicWebObject)
			{
				parent = (IBasicWebObject)parent.getParent();
			}
			Object forFoundsetValue = parent.getProperty(forFoundset);
			if (forFoundsetValue instanceof JSONObject)
			{
				return ((JSONObject)forFoundsetValue).optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
			}
		}
		else if (pd.getType() instanceof FoundsetPropertyType)
		{
			Object forFoundsetValue = webObject.getProperty(pd.getName());
			if (forFoundsetValue instanceof JSONObject)
			{
				return ((JSONObject)forFoundsetValue).optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
			}
		}
		return "";
	}

	private static IDataProvider checkComponentDataproviders(String id, FlattenedSolution persistFlattenedSolution, WebComponent component)
		throws RepositoryException
	{
		IDataProvider dataProvider = null;
		WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(component.getTypeName());
		if (spec != null)
		{
			Collection<PropertyDescription> fsPD = spec.getProperties(FoundsetPropertyType.INSTANCE);
			for (PropertyDescription pd : fsPD)
			{
				Object relatedFS = component.getProperty(pd.getName());
				if (relatedFS instanceof JSONObject)
				{
					String fs = ((JSONObject)relatedFS).optString(FoundsetPropertyType.FOUNDSET_SELECTOR);
					if ("".equals(fs)) //Form foundset
					{
						Form f = (Form)component.getAncestor(IRepository.FORMS);
						fs = f.getDataSource();
					}
					if (fs == null) break;
					if (DataSourceUtils.isDatasourceUri(fs))
					{
						ITable table = persistFlattenedSolution.getTable(fs);
						if (table != null)
						{
							dataProvider = persistFlattenedSolution.getDataProviderForTable(table, id);
						}
					}
					else
					{
						Relation[] relations = persistFlattenedSolution.getRelationSequence(fs);
						if (relations != null)
						{
							Relation r = relations[relations.length - 1];
							dataProvider = getDataProvider(persistFlattenedSolution, id, r.getForeignDataSource());
							if (r != null) BuilderDependencies.getInstance().addDependency(component.getAncestor(IRepository.FORMS),
								ServoyModelFinder.getServoyModel().getFlattenedSolution().getRelation(r.getName()));
						}
					}
					if (dataProvider != null) break;
				}
			}
		}
		return dataProvider;
	}

	private static IDataProvider getDataProvider(FlattenedSolution fs, String id, String dataSource) throws RepositoryException
	{
		ITable table = ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(dataSource);
		if (table != null)
		{
			return fs.getDataProviderForTable(table, id);
		}
		return null;
	}

	private static void checkForListFormComponent(IResource markerResource, Form form, IPersist listFormComponent, FlattenedSolution flattenedSolution)
	{
		form.acceptVisitor(new IPersistVisitor()
		{
			@Override
			public Object visit(IPersist o)
			{
				if (o instanceof WebComponent)
				{
					WebObjectSpecification spec = WebComponentSpecProvider.getSpecProviderState().getWebObjectSpecification(((WebComponent)o).getTypeName());
					if (spec != null)
					{
						Collection<PropertyDescription> properties = spec.getProperties(FormComponentPropertyType.INSTANCE);
						if (properties.size() > 0)
						{
							FormElement formComponentEl = FormElementHelper.INSTANCE.getFormElement((WebComponent)o, flattenedSolution, null,
								true);
							boolean hasComponentTypeConfig = false; // has forFoundset
							Form frm = null;
							for (PropertyDescription pd : properties)
							{
								Object propertyValue = formComponentEl.getPropertyValue(pd.getName());
								frm = FormComponentPropertyType.INSTANCE.getForm(propertyValue, flattenedSolution);
								if (frm == null) continue;
								if (pd.getConfig() instanceof ComponentTypeConfig)
								{
									hasComponentTypeConfig = true;
									break;
								}
							}
							if (hasComponentTypeConfig)
							{
								ServoyMarker mk = MarkerMessages.FormComponentNestedList.fill(listFormComponent, o);
								ServoyBuilder.addMarker(markerResource, mk.getType(), mk.getText(), -1, ServoyBuilder.FORM_COMPONENT_NESTED_LIST,
									IMarker.PRIORITY_NORMAL, null,
									listFormComponent);
							}
							else if (frm != null)
							{
								checkForListFormComponent(markerResource, frm, listFormComponent, flattenedSolution);
							}
						}
					}
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}
		});
	}

	private static boolean isUnstoredCalc(String dpid, ITable table, FlattenedSolution flattenedSolution)
	{
		return table != null && dpid != null && flattenedSolution.getScriptCalculation(dpid, table) != null && table.getColumn(dpid) == null;
	}

	public static void deleteMarkers(Form form)
	{
		IResource markerResource = ServoyBuilderUtils.getPersistResource(form);
		try
		{
			if (markerResource.exists())
			{
				markerResource.deleteMarkers(ServoyBuilder.PROJECT_FORM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_SCRIPT_ELEMENT_USAGE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.EVENT_METHOD_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.MULTIPLE_METHODS_ON_SAME_ELEMENT, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.METHOD_NUMBER_OF_ARGUMENTS_MISMATCH_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.PARAMETERS_MISMATCH, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.INVALID_DATAPROVIDERID, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.INVALID_EVENT_METHOD, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.SOLUTION_PROBLEM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.HIDDEN_TABLE_STILL_IN_USE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.INVALID_SORT_OPTION, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.FORM_DUPLICATE_PART_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.UNRESOLVED_RELATION_UUID, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.PORTAL_DIFFERENT_RELATION_NAME_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.LABEL_FOR_ELEMENT_NOT_FOUND_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.MEDIA_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.MISSING_SPEC, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.MISSING_PROPERTY_FROM_SPEC, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_SPEC, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.OBSOLETE_ELEMENT, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_ELEMENT_USAGE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.DEPRECATED_PROPERTY_USAGE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.ELEMENT_EXTENDS_DELETED_ELEMENT_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.MISSING_STYLE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.FORM_WITH_DATASOURCE_IN_LOGIN_SOLUTION, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.NAMED_FOUNDSET_DATASOURCE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.DUPLICATE_SIBLING_UUID, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.METHOD_OVERRIDE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.WRONG_OVERRIDE_PARENT, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.DUPLICATE_NAME_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.SUPERFORM_PROBLEM_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.VARIANT_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}
		markerResource = ResourcesPlugin.getWorkspace().getRoot()
			.getFile(new Path(SolutionSerializer.getScriptPath(form, false)));
		try
		{
			if (markerResource.exists())
			{
				markerResource.deleteMarkers(ServoyBuilder.RESERVED_WINDOW_OBJECT_USAGE_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.PROJECT_FORM_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.SCRIPT_MARKER_TYPE, true, IResource.DEPTH_INFINITE);
				markerResource.deleteMarkers(ServoyBuilder.METHOD_OVERRIDE, true, IResource.DEPTH_INFINITE);
			}
		}
		catch (CoreException e)
		{
			ServoyLog.logError(e);
		}

	}
}
