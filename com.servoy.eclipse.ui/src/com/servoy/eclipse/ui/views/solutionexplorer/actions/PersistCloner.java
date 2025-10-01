/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2014 Servoy BV

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

package com.servoy.eclipse.ui.views.solutionexplorer.actions;

import java.util.Iterator;

import org.json.JSONObject;
import org.sablo.specification.PropertyDescription;
import org.sablo.specification.WebObjectSpecification;

import com.servoy.eclipse.core.IDeveloperServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.nature.ServoyProject;
import com.servoy.eclipse.model.repository.EclipseRepository;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.AbstractBase;
import com.servoy.j2db.persistence.AggregateVariable;
import com.servoy.j2db.persistence.ContentSpec;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IPersistVisitor;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ISupportUpdateableName;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.IValidateName;
import com.servoy.j2db.persistence.Media;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.ScriptCalculation;
import com.servoy.j2db.persistence.ScriptMethod;
import com.servoy.j2db.persistence.ScriptVariable;
import com.servoy.j2db.persistence.Solution;
import com.servoy.j2db.persistence.TableNode;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.shared.ApplicationServerRegistry;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.UUID;
import com.servoy.j2db.util.Utils;

public class PersistCloner
{
	/**
	 * Creates a copy of the given form at the specified location. Also updates references within the new form to form methods and form variables.
	 *
	 * @param formToDuplicate the form to be duplicated/copied.
	 * @param location the location where the new form should be put. Object[2] - { newFormName, destinationSolutionServoyProject }
	 * @param nameValidator the name validator to be used.
	 * @throws RepositoryException when the new form cannot be persisted.
	 */
	public static IPersist intelligentClonePersist(IPersist persist, String newPersistName, ServoyProject servoyProject, IValidateName nameValidator,
		boolean save) throws RepositoryException
	{
		IPersist duplicate = duplicatePersist(persist, newPersistName, servoyProject, nameValidator);
		if (duplicate != null && persist instanceof Form)
		{
			// link the events that point to form methods to the cloned methods instead of the original form methods
			relinkFormMethodsToEvents((Form)persist, (Form)duplicate);
		}
		if (save)
		{
			ServoyProject sp = ServoyModelManager.getServoyModelManager().getServoyModel().getServoyProject(duplicate.getRootObject().getName());
			if (sp != null)
			{
				if (duplicate.getParent() instanceof Solution)
				{
					sp.saveEditingSolutionNodes(new IPersist[] { duplicate }, true);
				}
				else
				{
					sp.saveEditingSolutionNodes(new IPersist[] { duplicate.getParent() }, true);
				}
			}
			else
			{
				ServoyLog.logError("Cannot find solution project for duplicated persist", null);
			}
		}
		return duplicate;
	}

	protected static void relinkFormMethodsToEvents(final Form original, final Form duplicate)
	{
		duplicate.acceptVisitor(new IPersistVisitor()
		{

			public Object visit(IPersist o)
			{
				try
				{
					if (o instanceof WebComponent)
					{
						WebComponent wc = (WebComponent)o;
						PropertyDescription pd = ((WebObjectImpl)wc.getImplementation()).getPropertyDescription();
						if (pd instanceof WebObjectSpecification)
						{
							for (String handler : ((WebObjectSpecification)pd).getHandlers().keySet())
							{
								ScriptMethod clonedMethod = this.getClonedMethod(wc.getProperty(handler));
								if (clonedMethod != null)
								{
									wc.setProperty(handler, clonedMethod.getUUID().toString());
								}
							}
						}
						if (wc.getTypeName().equals("servoycore-listformcomponent") || wc.getTypeName().equals("servoycore-formcomponent"))
						{
							JSONObject jsonObject = (JSONObject)wc.getProperty("json");
							this.updateEventsUUID(jsonObject);
						}
					}
					else
					{
						ServoyModelManager.getServoyModelManager().getServoyModel();
						EclipseRepository er = (EclipseRepository)ApplicationServerRegistry.get().getDeveloperRepository();
						Iterator<ContentSpec.Element> iterator = er.getContentSpec().getPropertiesForObjectType(o.getTypeID());

						while (iterator.hasNext())
						{
							final ContentSpec.Element element = iterator.next();
							// Don't set meta data properties.
							if (element.isMetaData() || element.isDeprecated()) continue;
							// Get default property value as an object.
							final int typeId = element.getTypeID();

							if (typeId == IRepository.ELEMENTS)
							{
								Object property_value = ((AbstractBase)o).getProperty(element.getName());
								final UUID element_uuid = Utils.getAsUUID(property_value, false);
								if (element_uuid != null)
								{
									boolean idFound = false;
									Iterator<ScriptMethod> originalScriptMethods = original.getScriptMethods(false);
									while (originalScriptMethods.hasNext())
									{
										ScriptMethod originalMethod = originalScriptMethods.next();
										if (originalMethod.getUUID().equals(element_uuid))
										{
											// reference to a method in the original form - change this to the duplicated form method
											ScriptMethod duplicateMethod = duplicate.getScriptMethod(originalMethod.getName());
											if (duplicateMethod != null)
											{
												((AbstractBase)o).setProperty(element.getName(), duplicateMethod.getUUID().toString());
											}
											idFound = true;
										}
									}

									if (!idFound)
									{
										Iterator<ScriptVariable> originalScriptVariables = original.getScriptVariables(false);
										while (originalScriptVariables.hasNext())
										{
											ScriptVariable originalVariable = originalScriptVariables.next();
											if (originalVariable.getUUID().equals(element_uuid))
											{
												// reference to a variable in the original form - change this to the duplicated form variable
												ScriptVariable duplicateVariable = duplicate.getScriptVariable(originalVariable.getName());
												if (duplicateVariable != null)
												{
													((AbstractBase)o).setProperty(element.getName(), duplicateVariable.getUUID().toString());
												}
											}
										}
									}
								}
							}
						}
					}
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError(e);
				}
				catch (IllegalArgumentException e)
				{
					ServoyLog.logError(e);
				}
				return IPersistVisitor.CONTINUE_TRAVERSAL;
			}

			private ScriptMethod getClonedMethod(Object obj)
			{
				UUID uuid = Utils.getAsUUID(obj, false);
				if (uuid != null)
				{
					IPersist originalMethod = original.findChild(uuid);
					if (originalMethod instanceof ScriptMethod)
					{
						return duplicate.getScriptMethod(((ScriptMethod)originalMethod).getName());
					}
				}
				return null;
			}

			private void updateEventsUUID(JSONObject jsonObject)
			{
				Iterator<String> keys = jsonObject.keys();
				while (keys.hasNext())
				{
					String key = keys.next();
					Object value = jsonObject.get(key);
					if (value instanceof JSONObject)
					{
						updateEventsUUID((JSONObject)value);
					}
					else
					{
						ScriptMethod clonedMethod = this.getClonedMethod(jsonObject.get(key));
						if (clonedMethod != null)
						{
							jsonObject.put(key, clonedMethod.getUUID().toString());
						}
					}
				}
			}
		});
	}

	protected static IPersist duplicatePersist(IPersist persist, String newPersistName, ServoyProject servoyProject, IValidateName nameValidator)
		throws RepositoryException
	{
		if (newPersistName != null && servoyProject != null)
		{
			Solution destinationEditingSolution = servoyProject.getEditingSolution();

			if (destinationEditingSolution != null)
			{
				if (persist instanceof Media)
				{
					newPersistName = Utils.stringReplace(newPersistName, " ", "_");
				}
				AbstractBase clone = null;
				if (persist instanceof ScriptCalculation)
				{
					FlattenedSolution editingFlattenedSolution = servoyProject.getEditingFlattenedSolution();
					ITable table = editingFlattenedSolution.getTable(((TableNode)((ScriptCalculation)persist).getParent()).getDataSource());
					clone = destinationEditingSolution.createNewScriptCalculation(nameValidator, table, newPersistName, null);
					clone.copyPropertiesMap(((ScriptCalculation)persist).getPropertiesMap(), true);
				}
				else if (persist instanceof AggregateVariable)
				{
					FlattenedSolution editingFlattenedSolution = servoyProject.getEditingFlattenedSolution();
					ITable table = editingFlattenedSolution.getTable(((TableNode)((AggregateVariable)persist).getParent()).getDataSource());
					clone = destinationEditingSolution.createNewAggregateVariable(nameValidator, table, newPersistName, ((AggregateVariable)persist).getType(),
						((AggregateVariable)persist).getDataProviderIDToAggregate());
					clone.copyPropertiesMap(((AggregateVariable)persist).getPropertiesMap(), true);
				}
				else
				{
					clone = (AbstractBase)((AbstractBase)persist).cloneObj(destinationEditingSolution, true, nameValidator, true, false //
						, false /* elements of original form should remain override, not a flattened element */);
				}
				if (clone instanceof ISupportUpdateableName)
				{
					((ISupportUpdateableName)clone).updateName(nameValidator, newPersistName);
					IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
					FlattenedSolution fs = servoyModel.getActiveProject().getEditingFlattenedSolution();
					fs.flushAllCachedData(); //make sure the name caches are flushed from the active solution and modules
					for (Solution mod : fs.getModules())
					{
						FlattenedSolution editingFlattenedSolution = servoyModel.getServoyProject(mod.getName()).getEditingFlattenedSolution();
						if (editingFlattenedSolution.getForm(clone.getUUID().toString()) != null)
						{
							editingFlattenedSolution.flushAllCachedData();
						}
					}
				}
				else if (clone instanceof Media)
				{
					((Media)clone).setName(newPersistName);
					((Media)clone).setPermMediaData(((Media)persist).getMediaData());
				}
				else if (clone instanceof TableNode && DataSourceUtils.getInmemDataSourceName(((TableNode)clone).getDataSource()) != null)
				{
					((TableNode)clone).setDataSource(DataSourceUtils.createInmemDataSource(newPersistName));
				}
				clone.setRuntimeProperty(AbstractBase.NameChangeProperty, "");
				return clone;
			}
			else
			{
				ServoyLog.logError("Cannot get solution for destination solution project", null);
			}
		}
		return null;
	}


}
