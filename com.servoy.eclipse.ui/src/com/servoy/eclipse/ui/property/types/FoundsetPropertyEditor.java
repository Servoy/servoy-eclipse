/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2015 Servoy BV

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

package com.servoy.eclipse.ui.property.types;

import org.eclipse.jface.viewers.IFilter;
import org.eclipse.jface.viewers.ILabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.json.JSONObject;

import com.servoy.eclipse.model.ServoyModelFinder;
import com.servoy.eclipse.model.util.IDataSourceWrapper;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.TableWrapper;
import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider;
import com.servoy.eclipse.ui.dialogs.CombinedTreeContentProvider.CombinedTreeOptions;
import com.servoy.eclipse.ui.dialogs.FormFoundsetEntryContentProvider;
import com.servoy.eclipse.ui.dialogs.FormFoundsetEntryLabelProvider;
import com.servoy.eclipse.ui.dialogs.NamedFoundsetContentProvider;
import com.servoy.eclipse.ui.dialogs.NamedFoundsetLabelProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider;
import com.servoy.eclipse.ui.dialogs.RelationContentProvider.RelationsWrapper;
import com.servoy.eclipse.ui.dialogs.TableContentProvider;
import com.servoy.eclipse.ui.dialogs.TableContentProvider.TableListOptions;
import com.servoy.eclipse.ui.editors.IValueEditor;
import com.servoy.eclipse.ui.editors.ListSelectCellEditor;
import com.servoy.eclipse.ui.labelproviders.CombinedTreeLabelProvider;
import com.servoy.eclipse.ui.labelproviders.DatasourceLabelProvider;
import com.servoy.eclipse.ui.labelproviders.RelationLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.RelationPropertyController.RelationValueEditor;
import com.servoy.eclipse.ui.property.TableValueEditor;
import com.servoy.eclipse.ui.util.EditorUtil;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Relation;
import com.servoy.j2db.persistence.WebComponent;
import com.servoy.j2db.persistence.WebObjectImpl;
import com.servoy.j2db.server.ngclient.property.FoundsetPropertyType;
import com.servoy.j2db.util.DataSourceUtils;
import com.servoy.j2db.util.ServoyJSONObject;

/**
 * Editor for a foundset typed property (NG).
 * It is able to let you choose a related foundset, the form foundset or a any table's foundset.
 *
 * @author acostescu
 */
public class FoundsetPropertyEditor extends ListSelectCellEditor
{

	protected final FoundsetDesignToChooserConverter designToChooserConverter;

	public FoundsetPropertyEditor(Composite parent, PersistContext persistContext, ITable primaryTableForRelation, final ITable foreignTableForRelation,
		boolean isReadOnly, FoundsetDesignToChooserConverter designToChooserConverter, String property)
	{
		super(parent, "Please select a foundset", getFoundsetContentProvider(persistContext),
			getFoundsetLabelProvider(persistContext, designToChooserConverter, property), new FoundsetValueEditor(persistContext.getContext()), isReadOnly,
			getFoundsetInputOptions(primaryTableForRelation, foreignTableForRelation), SWT.NONE, null, "selectFoundsetDialog");
		setShowFilterMenu(true);

		this.designToChooserConverter = designToChooserConverter;

		setSelectionFilter(new IFilter()
		{
			public boolean select(Object toTest)
			{
				if (toTest == CombinedTreeContentProvider.NONE || toTest == FormFoundsetEntryContentProvider.FORM_FOUNDSET)
				{
					return true;
				}
				if (toTest instanceof RelationsWrapper)
				{
					return ((RelationsWrapper)toTest).relations != null && ((RelationsWrapper)toTest).relations.length > 0 && foreignTableForRelation == null ||
						foreignTableForRelation.equals(ServoyModelFinder.getServoyModel().getDataSourceManager().getDataSource(
							((RelationsWrapper)toTest).relations[((RelationsWrapper)toTest).relations.length - 1].getForeignDataSource()));
				}
				else if (toTest instanceof IDataSourceWrapper)
				{
					return ((IDataSourceWrapper)toTest).getTableName() != null && ((IDataSourceWrapper)toTest).getTableName().length() > 0;
				}
				else if (toTest instanceof Form)
				{
					return true;
				}
				return false;
			}
		});
	}

	public static ITreeContentProvider getFoundsetContentProvider(PersistContext persistContext)
	{
		// @formatter:off
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(persistContext.getPersist());
		IPersist contextPersist = persistContext.getContext();
		CombinedTreeContentProvider foundsetContentProvider = new CombinedTreeContentProvider(1,
			new ITreeContentProvider[] { new FormFoundsetEntryContentProvider(), new CombinedTreeContentProvider(2,
				new ITreeContentProvider[] { new RelationContentProvider(flattenedSolution,
					contextPersist), new TableContentProvider() }), new NamedFoundsetContentProvider(flattenedSolution) });
		// @formatter:on

		return foundsetContentProvider;
	}

	public static ILabelProvider getFoundsetLabelProvider(PersistContext persistContext, final FoundsetDesignToChooserConverter designToChooserConv,
		String property)
	{
		// @formatter:off
		CombinedTreeLabelProvider foundsetChooserLabelProvider = new CombinedTreeLabelProvider(1,
			new ILabelProvider[] { new FormFoundsetEntryLabelProvider(), new NamedFoundsetLabelProvider(), new CombinedTreeLabelProvider(2,
				new ILabelProvider[] { new RelationLabelProvider("", true, true), new DatasourceLabelProvider("", true, false) }) });
		CombinedTreeLabelProvider foundsetCellLabelProvider = new CombinedTreeLabelProvider(1,
			new ILabelProvider[] { new FormFoundsetEntryLabelProvider(), new NamedFoundsetLabelProvider(), new CombinedTreeLabelProvider(2,
				new ILabelProvider[] { new RelationLabelProvider("", true, true), new DatasourceLabelProvider("", true, true) }) });

		final SolutionContextDelegateLabelProvider withSolutionContextForChooser = new SolutionContextDelegateLabelProvider(foundsetChooserLabelProvider,
			persistContext != null? persistContext.getContext(): null);
		final SolutionContextDelegateLabelProvider withSolutionContextForCell = new SolutionContextDelegateLabelProvider(foundsetCellLabelProvider,
			persistContext != null? persistContext.getContext(): null);
		// @formatter:on

		// this is a bit confusing as this label provider is used both in tree dialog and in properties view cell; but we don't have
		// the same kind of things in those two so we need some conversions here; we can't use 2 separate label providers because ListSelectCellEditor only uses 1
		return new LabelProvider()
		{
			@Override
			public Image getImage(Object element)
			{
				if (element instanceof JSONObject) return null; // properties view cell label provider // TODO why does this look weirdly small !? it gets shrinked for some reason...
				return withSolutionContextForChooser.getImage(designToChooserConv.convertJSONValueToChooserValue(element));
			}

			@Override
			public String getText(Object element)
			{
				Object value = element;
				if (value == null && persistContext != null && persistContext.getPersist() instanceof WebComponent wc && !wc.hasProperty(property))
				{
					value = ((WebObjectImpl)wc.getImplementation()).getPropertyDefaultValue(property);
				}
				if (value instanceof JSONObject) return withSolutionContextForCell.getText(designToChooserConv.convertJSONValueToChooserValue(value)); // properties view cell label provider
				else return withSolutionContextForChooser.getText(value);
			}
		};

	}

	public static Object getFoundsetInputOptions(ITable primaryTableForRelation, ITable foreignTableForRelation)
	{
		return getFoundsetInputOptions(primaryTableForRelation, foreignTableForRelation, true);
	}

	public static Object getFoundsetInputOptions(ITable primaryTableForRelation, ITable foreignTableForRelation, boolean includeNone)
	{
		// @formatter:off
		return new CombinedTreeOptions(null, includeNone,
			new Object[] { null, new CombinedTreeOptions(new String[] { "Related foundset", "Separate foundset (random table)" }, false,
				new Object[] { new RelationContentProvider.RelationListOptions(primaryTableForRelation, foreignTableForRelation, false,
					true), new TableContentProvider.TableListOptions(TableListOptions.TableListType.ALL, false) }), null });
		// @formatter:on
	}

	@Override
	public StructuredSelection getSelection()
	{
		Object value = getValue();
		Object valueForChooser = designToChooserConverter.convertJSONValueToChooserValue(value);

		return super.getSelection(valueForChooser);
	}

	@Override
	protected void doSetValue(Object value)
	{
		// as this editor works directly with the foundset property whole JSON value (selector/DPs), but it's tree select dialog is only interested in
		// the selector part and will only give what is needed for that; so doSetValue can be called with JSON from the properties system but
		// also with a RelationsWrapper, TableWrapper, FormFoundsetEntryContentProvider.FORM_FOUNDSET or CombinedTreeContentProvider.NONE when results of
		// picking something from the dialog are set; the underlying value is always and will become the JSON design value
		JSONObject jsonValue = designToChooserConverter.convertFromChooserValueToJSONValue(value, (JSONObject)getValue());
		super.doSetValue(jsonValue);
	}

	public static class FoundsetValueEditor implements IValueEditor<Object>
	{

		protected final IPersist contextPersist;

		public FoundsetValueEditor(IPersist contextPersist)
		{
			this.contextPersist = contextPersist;
		}

		public void openEditor(Object value)
		{
			if (value instanceof JSONObject)
			{
				String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(((JSONObject)value).opt(FoundsetPropertyType.FOUNDSET_SELECTOR));
				if (foundsetSelector != null)
				{
					if (DataSourceUtils.isDatasourceUri(foundsetSelector))
					{
						EditorUtil.openTableEditor(foundsetSelector);
					}
					else if (foundsetSelector.equals(""))
					{
						EditorUtil.openTableEditor(((Form)contextPersist.getAncestor(IRepository.FORMS)).getDataSource());
					}
					else
					{
						FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(contextPersist);
						Relation[] relations = flattenedSolution.getRelationSequence(foundsetSelector);
						if (relations != null && relations.length > 0)
						{
							EditorUtil.openRelationEditor(relations[relations.length - 1]);
						}
					}
				}
			}
			else if (value instanceof RelationsWrapper) RelationValueEditor.INSTANCE.openEditor(value);
			else if (value instanceof TableWrapper) TableValueEditor.INSTANCE.openEditor(value);
			else if (value == FormFoundsetEntryContentProvider.FORM_FOUNDSET)
			{
				Form form = (Form)contextPersist.getAncestor(IRepository.FORMS);
				if (form.getDataSource() != null && form.getDataSource().length() > 0) EditorUtil.openTableEditor(form.getDataSource());
			}
		}

		public boolean canEdit(Object value)
		{
			if (value instanceof JSONObject)
			{
				String foundsetSelector = (String)ServoyJSONObject.jsonNullToNull(((JSONObject)value).opt(FoundsetPropertyType.FOUNDSET_SELECTOR));
				if (foundsetSelector != null)
				{
					if (DataSourceUtils.isDatasourceUri(foundsetSelector))
					{
						return true;
					}
					else if (foundsetSelector.equals(""))
					{
						return ((Form)contextPersist.getAncestor(IRepository.FORMS)).getDataSource() != null;
					}
					else
					{
						FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(contextPersist);
						Relation[] relations = flattenedSolution.getRelationSequence(foundsetSelector);
						if (relations != null && relations.length > 0)
						{
							return true;
						}
					}
				}
			}
			return RelationValueEditor.INSTANCE.canEdit(value) || TableValueEditor.INSTANCE.canEdit(value) ||
				(value == FormFoundsetEntryContentProvider.FORM_FOUNDSET && ((Form)contextPersist.getAncestor(IRepository.FORMS)).getDataSource() != null &&
					((Form)contextPersist.getAncestor(IRepository.FORMS)).getDataSource().length() > 0);
		}
	}

}