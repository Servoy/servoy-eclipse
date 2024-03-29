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
package com.servoy.eclipse.designer.editor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.eclipse.core.databinding.DataBindingContext;
import org.eclipse.core.databinding.beans.typed.PojoProperties;
import org.eclipse.core.databinding.observable.value.IObservableValue;
import org.eclipse.gef.commands.Command;
import org.eclipse.gef.commands.CompoundCommand;
import org.eclipse.jface.databinding.swt.typed.WidgetProperties;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ListViewer;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.ScrolledComposite;
import org.eclipse.swt.events.SelectionAdapter;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.layout.FillLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout;
import org.eclipse.swt.layout.grouplayout.GroupLayout.ParallelGroup;
import org.eclipse.swt.layout.grouplayout.GroupLayout.SequentialGroup;
import org.eclipse.swt.layout.grouplayout.LayoutStyle;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Text;
import org.eclipse.ui.views.properties.IPropertySource;

import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.designer.editor.commands.RefreshingCommand;
import com.servoy.eclipse.designer.property.SetValueCommand;
import com.servoy.eclipse.model.util.ModelUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.ui.dialogs.DataProviderDialog;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer;
import com.servoy.eclipse.ui.dialogs.DataProviderTreeViewer.DataProviderOptions.INCLUDE_RELATIONS;
import com.servoy.eclipse.ui.labelproviders.DataProviderLabelProvider;
import com.servoy.eclipse.ui.labelproviders.FormContextDelegateLabelProvider;
import com.servoy.eclipse.ui.labelproviders.SolutionContextDelegateLabelProvider;
import com.servoy.eclipse.ui.property.PartTypeLabelProvider;
import com.servoy.eclipse.ui.property.PersistContext;
import com.servoy.eclipse.ui.property.PersistPropertySource;
import com.servoy.eclipse.ui.views.IndexedListViewer;
import com.servoy.eclipse.ui.views.IndexedStructuredSelection;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IDataProvider;
import com.servoy.j2db.persistence.IDeveloperRepository;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Part;
import com.servoy.j2db.persistence.RepositoryException;
import com.servoy.j2db.persistence.SolutionMetaData;
import com.servoy.j2db.persistence.StaticContentSpecLoader;
import com.servoy.j2db.util.PersistHelper;
import com.servoy.j2db.util.Utils;

/**
 * Tab in form editor for managing form parts.
 *
 * @author rgansevles
 */

public class VisualFormEditorPartsPage extends Composite
{

	private Button discardRemainderOfButton;
	private Button allowPartToButton;
	private Button restartPageNumbersButton;
	private Button pageBreakAfterButton;
	private Button pageBreakBeforeButton;
	private Button sinkPartIfButton;
	private final DataBindingContext m_bindingContext;
	private Text pageBreakAfterText;
	private final PartBean currentPartBean = new PartBean();
	private final BaseVisualFormEditor editor;
	private ListViewer availableParts;
	private ListViewer currentParts;
	private Button upButton;
	private Button downButton;
	private Group optionsGroup;
	private Button addPartsButton;
	private Button removePartsButton;
	private IndexedListViewer groupByFields;
	private Button removeGroupByButton;
	private Button addGroupByButton;
	private boolean doRefresh;

	private boolean isNGClientOnly = false;

	public VisualFormEditorPartsPage(BaseVisualFormEditor editor, Composite parent, int style)
	{
		super(parent, style);
		this.editor = editor;
		isNGClientOnly = SolutionMetaData.isNGOnlySolution(editor.getForm().getSolution().getSolutionType());

		createContents();
		m_bindingContext = initDataBindings();

		doRefresh();
	}


	protected void createContents()
	{
		// Scrolling container
		this.setLayout(new FillLayout());
		ScrolledComposite scrolledComposite = new ScrolledComposite(this, SWT.H_SCROLL | SWT.V_SCROLL);
		scrolledComposite.setExpandHorizontal(true);
		scrolledComposite.setExpandVertical(true);

		Composite container = new Composite(scrolledComposite, SWT.NONE);
		scrolledComposite.setContent(container);

		// Parts group
		Group partsGroup;
		partsGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		partsGroup.setText("Parts");

		optionsGroup = new Group(container, SWT.SHADOW_ETCHED_IN);
		optionsGroup.setText("Part options");

		Label groupbyFieldsLabel;
		groupbyFieldsLabel = new Label(optionsGroup, SWT.NONE);

		groupbyFieldsLabel.setText("Group-by fields");

		groupByFields = new IndexedListViewer(optionsGroup, SWT.BORDER | SWT.MULTI);
		groupByFields.setContentProvider(new ArrayContentProvider());

		org.eclipse.swt.widgets.List groupByFieldsList = groupByFields.getList();
		groupByFieldsList.setToolTipText("Group-by fields");
		groupByFields.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				configureOptionsButtons();
			}
		});


		sinkPartIfButton = new Button(optionsGroup, SWT.CHECK);
		sinkPartIfButton.setText("Sink part if last on page");


		pageBreakBeforeButton = new Button(optionsGroup, SWT.CHECK);

		pageBreakBeforeButton.setText("Page break before each occurrence");
		restartPageNumbersButton = new Button(optionsGroup, SWT.CHECK);

		restartPageNumbersButton.setText("Restart page numbers after each occurrence");
		allowPartToButton = new Button(optionsGroup, SWT.CHECK);

		allowPartToButton.setText("Allow part to break across page boundaries");
		Label availablePartsLabel = new Label(partsGroup, SWT.NONE);
		availablePartsLabel.setAlignment(SWT.RIGHT);
		availablePartsLabel.setText("Available");

		availableParts = new ListViewer(partsGroup, SWT.BORDER | SWT.MULTI);
		availableParts.addOpenListener(new IOpenListener()
		{
			public void open(OpenEvent event)
			{
				handleAddParts();
			}
		});
		availableParts.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				configurePartsButtons();
				fillOptionsection();
			}
		});

		org.eclipse.swt.widgets.List availablePartsList = availableParts.getList();
		availablePartsList.setToolTipText("Available parts");
		availableParts.setLabelProvider(PartTypeLabelProvider.INSTANCE);
		availableParts.setContentProvider(new ArrayContentProvider());


		addPartsButton = new Button(partsGroup, SWT.NONE);
		addPartsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleAddParts();
			}
		});
		addPartsButton.setText("&>>");

		removePartsButton = new Button(partsGroup, SWT.NONE);
		removePartsButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleRemoveParts();
			}
		});
		removePartsButton.setText("&<<");

		Label onCurrentFormLabel = new Label(partsGroup, SWT.NONE);
		onCurrentFormLabel.setAlignment(SWT.RIGHT);
		onCurrentFormLabel.setText("Used by form");

		currentParts = new ListViewer(partsGroup, SWT.BORDER | SWT.MULTI);
		currentParts.addOpenListener(new IOpenListener()
		{
			public void open(OpenEvent event)
			{
				handleRemoveParts();
			}
		});
		currentParts.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				configurePartsButtons();
				fillOptionsection();
			}
		});
		org.eclipse.swt.widgets.List currentPartsList = currentParts.getList();
		currentPartsList.setToolTipText("Parts on current form");
		currentParts.setLabelProvider(new SolutionContextDelegateLabelProvider(
			new FormContextDelegateLabelProvider(new PartTypeLabelProvider(editor.getForm()), editor.getForm()), editor.getForm().getSolution(), false));
		currentParts.setContentProvider(new ArrayContentProvider());

		upButton = new Button(partsGroup, SWT.NONE);
		upButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleMoveParts(true);
			}
		});
		upButton.setText("up");

		downButton = new Button(partsGroup, SWT.NONE);
		downButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleMoveParts(false);
			}
		});
		downButton.setText("down");

		discardRemainderOfButton = new Button(optionsGroup, SWT.CHECK);

		discardRemainderOfButton.setText("Discard remainder of part before new page");

		Composite groupByButtonGroup = new Composite(optionsGroup, SWT.NONE);

		Composite pageBreakAfterGroup = new Composite(optionsGroup, SWT.NONE);

		pageBreakAfterButton = new Button(pageBreakAfterGroup, SWT.CHECK);
		pageBreakAfterButton.setBounds(0, 0, 171, 22);
		pageBreakAfterButton.setText("Page break after every");

		pageBreakAfterText = new Text(pageBreakAfterGroup, SWT.BORDER);
		pageBreakAfterText.setBounds(177, 0, 37, 25);

		Label occurrencesLabel = new Label(pageBreakAfterGroup, SWT.NONE);
		occurrencesLabel.setBounds(218, 3, 91, 17);
		occurrencesLabel.setText("occurrences");
		final GroupLayout groupLayout_1 = new GroupLayout(container);
		ParallelGroup pg = groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(
			groupLayout_1.createSequentialGroup().add(partsGroup, GroupLayout.PREFERRED_SIZE, 659, Short.MAX_VALUE).add(3, 3, 3));
		if (!isNGClientOnly) pg.add(optionsGroup, GroupLayout.PREFERRED_SIZE, 662, Short.MAX_VALUE);
		groupLayout_1.setHorizontalGroup(
			groupLayout_1.createParallelGroup(GroupLayout.LEADING).add(groupLayout_1.createSequentialGroup().addContainerGap().add(pg).add(9, 9, 9)));

		SequentialGroup sg = groupLayout_1.createSequentialGroup().addContainerGap().add(partsGroup, GroupLayout.PREFERRED_SIZE, 242, Short.MAX_VALUE);
		if (!isNGClientOnly) sg.addPreferredGap(LayoutStyle.RELATED).add(optionsGroup, GroupLayout.PREFERRED_SIZE, 222, GroupLayout.PREFERRED_SIZE);
		sg.addContainerGap();
		groupLayout_1.setVerticalGroup(groupLayout_1.createParallelGroup(GroupLayout.TRAILING).add(sg));
		addGroupByButton = new Button(groupByButtonGroup, SWT.NONE);
		addGroupByButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleAddGroupByFields();
			}
		});
		addGroupByButton.setToolTipText("Add group-by fields");
		addGroupByButton.setText("Add...");
		removeGroupByButton = new Button(groupByButtonGroup, SWT.NONE);
		removeGroupByButton.addSelectionListener(new SelectionAdapter()
		{
			@Override
			public void widgetSelected(final SelectionEvent e)
			{
				handleRemoveGroupByFields();
			}
		});
		removeGroupByButton.setToolTipText("Remove selected group-by fields");
		removeGroupByButton.setText("Remove");
		final GroupLayout groupLayout_3 = new GroupLayout(groupByButtonGroup);
		groupLayout_3.setHorizontalGroup(groupLayout_3.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout_3.createSequentialGroup().add(0, 0, Short.MAX_VALUE).add(addGroupByButton, GroupLayout.PREFERRED_SIZE, 85,
				GroupLayout.PREFERRED_SIZE).add(18, 18, 18).add(removeGroupByButton, GroupLayout.PREFERRED_SIZE, 89, GroupLayout.PREFERRED_SIZE).add(1, 1, 1)));
		groupLayout_3.setVerticalGroup(groupLayout_3.createParallelGroup(GroupLayout.TRAILING).add(groupLayout_3.createSequentialGroup().add(
			groupLayout_3.createParallelGroup(GroupLayout.LEADING).add(removeGroupByButton, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE).add(
				addGroupByButton, GroupLayout.PREFERRED_SIZE, 29, GroupLayout.PREFERRED_SIZE))
			.addContainerGap(GroupLayout.DEFAULT_SIZE, Short.MAX_VALUE)));
		groupByButtonGroup.setLayout(groupLayout_3);
		final GroupLayout groupLayout_2 = new GroupLayout(optionsGroup);
		groupLayout_2.setHorizontalGroup(groupLayout_2.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout_2.createSequentialGroup().addContainerGap().add(groupLayout_2.createParallelGroup(GroupLayout.TRAILING).add(GroupLayout.LEADING,
				groupByFieldsList, GroupLayout.DEFAULT_SIZE, 235, Short.MAX_VALUE).add(GroupLayout.LEADING, groupbyFieldsLabel).add(groupByButtonGroup,
					GroupLayout.PREFERRED_SIZE, 193, GroupLayout.PREFERRED_SIZE))
				.addPreferredGap(LayoutStyle.RELATED).add(
					groupLayout_2.createParallelGroup(GroupLayout.LEADING).add(groupLayout_2.createSequentialGroup().add(20, 20, 20).add(
						groupLayout_2.createParallelGroup(GroupLayout.LEADING).add(pageBreakAfterGroup, GroupLayout.PREFERRED_SIZE, 318,
							Short.MAX_VALUE).add(pageBreakBeforeButton, GroupLayout.PREFERRED_SIZE, 261, GroupLayout.PREFERRED_SIZE).add(sinkPartIfButton,
								GroupLayout.PREFERRED_SIZE, 300, GroupLayout.PREFERRED_SIZE)
							.add(allowPartToButton, GroupLayout.PREFERRED_SIZE, 318,
								Short.MAX_VALUE)
							.add(restartPageNumbersButton))
						.add(73, 73, 73)).add(
							groupLayout_2.createSequentialGroup().add(38, 38, 38).add(discardRemainderOfButton).addContainerGap()))));
		groupLayout_2.setVerticalGroup(groupLayout_2.createParallelGroup(GroupLayout.TRAILING).add(
			groupLayout_2.createSequentialGroup().addContainerGap().add(groupbyFieldsLabel).addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout_2.createParallelGroup(GroupLayout.LEADING).add(
					groupLayout_2.createSequentialGroup().add(sinkPartIfButton).addPreferredGap(LayoutStyle.RELATED).add(pageBreakBeforeButton).addPreferredGap(
						LayoutStyle.RELATED).add(pageBreakAfterGroup, GroupLayout.PREFERRED_SIZE, 25, GroupLayout.PREFERRED_SIZE).addPreferredGap(
							LayoutStyle.RELATED)
						.add(restartPageNumbersButton).addPreferredGap(LayoutStyle.RELATED).add(allowPartToButton).add(3, 3, 3).add(
							discardRemainderOfButton))
					.add(
						groupLayout_2.createSequentialGroup().add(groupByFieldsList, GroupLayout.PREFERRED_SIZE, 118,
							GroupLayout.PREFERRED_SIZE).addPreferredGap(LayoutStyle.RELATED).add(groupByButtonGroup, GroupLayout.PREFERRED_SIZE, 31,
								GroupLayout.PREFERRED_SIZE)))
				.add(101, 101, 101)));
		optionsGroup.setLayout(groupLayout_2);
		final GroupLayout groupLayout = new GroupLayout(partsGroup);
		groupLayout.setHorizontalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.TRAILING).add(availablePartsList, GroupLayout.DEFAULT_SIZE, 251, Short.MAX_VALUE).add(
				availablePartsLabel, GroupLayout.PREFERRED_SIZE, 106, GroupLayout.PREFERRED_SIZE))
			.add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(groupLayout.createSequentialGroup().add(6, 6, 6).add(
					groupLayout.createParallelGroup(GroupLayout.LEADING).add(addPartsButton, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE).add(
						removePartsButton, GroupLayout.DEFAULT_SIZE, 51, Short.MAX_VALUE))
					.addPreferredGap(LayoutStyle.RELATED).add(currentPartsList,
						GroupLayout.DEFAULT_SIZE, 233, Short.MAX_VALUE))
					.add(
						groupLayout.createSequentialGroup().addPreferredGap(LayoutStyle.RELATED).add(onCurrentFormLabel, GroupLayout.PREFERRED_SIZE,
							177, GroupLayout.PREFERRED_SIZE)))
			.add(6, 6, 6).add(
				groupLayout.createParallelGroup(GroupLayout.TRAILING).add(upButton, GroupLayout.PREFERRED_SIZE, 78,
					GroupLayout.PREFERRED_SIZE).add(downButton, GroupLayout.PREFERRED_SIZE, 78,
						GroupLayout.PREFERRED_SIZE))
			.addContainerGap()));
		groupLayout.setVerticalGroup(groupLayout.createParallelGroup(GroupLayout.LEADING).add(groupLayout.createSequentialGroup().addContainerGap().add(
			groupLayout.createParallelGroup(GroupLayout.TRAILING).add(availablePartsLabel, GroupLayout.PREFERRED_SIZE, 17, GroupLayout.PREFERRED_SIZE).add(
				onCurrentFormLabel))
			.addPreferredGap(LayoutStyle.RELATED).add(
				groupLayout.createParallelGroup(GroupLayout.LEADING).add(currentPartsList, GroupLayout.PREFERRED_SIZE, 176, Short.MAX_VALUE).add(
					availablePartsList, GroupLayout.PREFERRED_SIZE, 176, Short.MAX_VALUE).add(
						groupLayout.createSequentialGroup().add(upButton).addPreferredGap(LayoutStyle.RELATED).add(downButton))
					.add(
						groupLayout.createSequentialGroup().add(addPartsButton).addPreferredGap(LayoutStyle.RELATED).add(
							removePartsButton)))
			.addContainerGap()));
		partsGroup.setLayout(groupLayout);
		container.setLayout(groupLayout_1);

		scrolledComposite.setMinSize(container.computeSize(SWT.DEFAULT, SWT.DEFAULT));
	}

	protected void fillPartsSection()
	{
		if (editor.getForm() == null) return;
		Form flattenedForm = ModelUtils.getEditingFlattenedSolution(editor.getForm()).getFlattenedForm(editor.getForm());

		// preserve selection when possible
		ISelection currentPartsSelection = currentParts.getSelection();
		ISelection availablePartsSelection = availableParts.getSelection();

		Part lastSuperPart = null;
		Form flattenedSuperForm = ModelUtils.getEditingFlattenedSolution(editor.getForm()).getFlattenedForm(editor.getForm().getExtendsForm());
		if (flattenedSuperForm != null)
		{
			// there is a super-form, can only add parts on the bottom
			for (Part p : Utils.iterate(flattenedSuperForm.getParts()))
			{
				lastSuperPart = p;
			}
		}

		Set<Integer> currentTypes = new HashSet<Integer>();
		List<Part> currentPartList = new ArrayList<Part>();

		// currently used parts
		for (Part p : Utils.iterate(flattenedForm.getParts()))
		{
			if (!PersistHelper.isOverrideOrphanElement(p))
			{
				currentPartList.add(p);
				currentTypes.add(Integer.valueOf(p.getPartType()));
			}
		}
		currentParts.setInput(currentPartList);

		// available parts
		List<Integer> partTypes = new ArrayList<Integer>();
		addAvailablePartType(Part.TITLE_HEADER, lastSuperPart, currentTypes, partTypes);
		addAvailablePartType(Part.HEADER, lastSuperPart, currentTypes, partTypes);
		if (!isNGClientOnly) addAvailablePartType(Part.LEADING_GRAND_SUMMARY, lastSuperPart, currentTypes, partTypes);
		if (!isNGClientOnly) addAvailablePartType(Part.LEADING_SUBSUMMARY, lastSuperPart, currentTypes, partTypes);
		addAvailablePartType(Part.BODY, lastSuperPart, currentTypes, partTypes);
		if (!isNGClientOnly) addAvailablePartType(Part.TRAILING_SUBSUMMARY, lastSuperPart, currentTypes, partTypes);
		if (!isNGClientOnly) addAvailablePartType(Part.TRAILING_GRAND_SUMMARY, lastSuperPart, currentTypes, partTypes);
		addAvailablePartType(Part.FOOTER, lastSuperPart, currentTypes, partTypes);
		if (!isNGClientOnly) addAvailablePartType(Part.TITLE_FOOTER, lastSuperPart, currentTypes, partTypes);

		// keep selection even is a part was overriden/unoverriden (can't simply use part type below because of subsummaries)
		ISelection newCurrentPartsSelection = currentPartsSelection;
		if (newCurrentPartsSelection instanceof StructuredSelection)
		{
			Object[] selectedParts = ((StructuredSelection)newCurrentPartsSelection).toArray();
			List<Object> newSelection = new ArrayList<Object>();
			for (Object o : selectedParts)
			{
				if (o instanceof Part) // always true
				{
					Part oldP = (Part)o;
					for (Part newP : currentPartList)
					{
						if (newP == oldP ||
							(newP.getPartType() == oldP.getPartType() && PersistHelper.getBasePersist(newP) == PersistHelper.getBasePersist(oldP)))
						{
							newSelection.add(newP);
							break;
						}
					}
				}
			}
			newCurrentPartsSelection = new StructuredSelection(newSelection);
		}
		currentParts.setSelection(newCurrentPartsSelection);
		availableParts.setInput(partTypes);
		availableParts.setSelection(availablePartsSelection);

		configurePartsButtons();
	}

	/**
	 * Add an available part type to partTypes if part type may be added to the form.
	 *
	 * @param partType
	 * @param lastSuperPart
	 * @param currentTypes
	 * @param partTypes
	 */
	private void addAvailablePartType(int partType, Part lastSuperPart, Set<Integer> currentTypes, List<Integer> partTypes)
	{
		if (lastSuperPart != null && (lastSuperPart.getPartType() > partType || (!lastSuperPart.canBeMoved() && lastSuperPart.getPartType() == partType)))
		{
			// may not be added to current form
			return;
		}

		if (Part.canBeMoved(partType) || !currentTypes.contains(Integer.valueOf(partType)))
		{
			partTypes.add(Integer.valueOf(partType));
		}
	}

	protected void configurePartsButtons()
	{
		IStructuredSelection selection = (IStructuredSelection)currentParts.getSelection();
		boolean enableUp = false;
		boolean enableDown = false;
		if (selection.size() == 1)
		{
			Part selectedPart = (Part)selection.getFirstElement();
			if (selectedPart.canBeMoved())
			{
				Part siblingPart = getSiblingPart(selectedPart, true);
				enableUp = siblingPart != null && siblingPart.canBeMoved();
				siblingPart = getSiblingPart(selectedPart, false);
				enableDown = siblingPart != null && siblingPart.canBeMoved();
			}
		}
		upButton.setEnabled(enableUp);
		downButton.setEnabled(enableDown);

		removePartsButton.setEnabled(selection.size() > 0 && !hasInheritedPart(selection));
		addPartsButton.setEnabled(((IStructuredSelection)availableParts.getSelection()).size() > 0);
	}

	private boolean hasInheritedPart(IStructuredSelection selection)
	{
		for (Part part : Utils.iterate((Iterator<Part>)selection.iterator()))
		{
			if (part.getAncestor(IRepository.FORMS) != editor.getForm() || PersistHelper.isOverrideElement(part))
			{
				return true;
			}
		}
		return false;
	}

	protected void fillOptionsection()
	{
		IStructuredSelection selection = (IStructuredSelection)currentParts.getSelection();
		Part part = null;
		if (selection.size() == 1)
		{
			part = (Part)selection.getFirstElement();
		}
		currentPartBean.setPart(part);
		m_bindingContext.updateTargets();

		groupByFields.setInput(currentPartBean.getPartProperty("groupbyDataProviderIDs"));

		configureOptionsButtons();

		optionsGroup.setEnabled(part != null);
	}


	protected void configureOptionsButtons()
	{
		boolean enableAdd = false;
		boolean enableRemove = false;

		if (currentPartBean.part != null)
		{
			enableAdd = enableRemove = currentPartBean.part.getPartType() == Part.LEADING_SUBSUMMARY ||
				currentPartBean.part.getPartType() == Part.TRAILING_SUBSUMMARY;
			enableAdd &= ((IStructuredSelection)groupByFields.getSelection()).size() < 2;
			enableRemove &= ((IStructuredSelection)groupByFields.getSelection()).size() > 0;

			enableAdd &= (editor.getForm().getDataSource() != null);
		}

		addGroupByButton.setEnabled(enableAdd);
		removeGroupByButton.setEnabled(enableRemove);
	}

	@Override
	public void setVisible(boolean visible)
	{
		super.setVisible(visible);
		if (visible && doRefresh)
		{
			doRefresh();
		}
	}

	public void refresh()
	{
		if (isDisposed())
		{
			return;
		}
		if (isVisible())
		{
			doRefresh();
		}
		else
		{
			doRefresh = true;
		}
	}

	public void doRefresh()
	{
		fillPartsSection();
		fillOptionsection();
		doRefresh = false;
	}

	/**
	 * Get the part above(up=true) or below(up=false) this part.
	 */
	public Part getSiblingPart(Part part, boolean up)
	{
		Iterator<Part> formParts;
		formParts = editor.getForm().getParts();

		Part prevPart = null;
		while (formParts.hasNext())
		{
			Part formPart = formParts.next();
			if (up)
			{
				if (formPart == part)
				{
					return prevPart;
				}
				prevPart = formPart;
			}
			else
			// down
			{
				if (formPart == part)
				{
					if (formParts.hasNext())
					{
						return formParts.next();
					}
				}
			}
		}

		// not found
		return null;
	}


	protected void handleAddParts()
	{
		final StructuredSelection currentSelection = (StructuredSelection)availableParts.getSelection();
		if (currentSelection.size() > 0)
		{
			int[] partTypeIds = new int[currentSelection.size()];
			Iterator<Integer> sel = currentSelection.iterator();
			for (int i = 0; sel.hasNext(); i++)
			{
				partTypeIds[i] = sel.next().intValue();
			}
			final AddPartsCommand addPartsCommand = new AddPartsCommand(editor.getForm(), partTypeIds);
			executeCommand(new RefreshingCommand(addPartsCommand)
			{
				@Override
				public void refresh(boolean haveExecuted)
				{
					fillPartsSection(); // for setting selection correctly
					if (haveExecuted && addPartsCommand.createdParts != null)
					{
						currentParts.setSelection(new StructuredSelection(addPartsCommand.createdParts));
					}
					else
					{
						// undo
						currentParts.setSelection(StructuredSelection.EMPTY);
						availableParts.setSelection(currentSelection);
					}
				}
			});
		}
	}

	protected void handleRemoveParts()
	{
		final StructuredSelection currentSelection = (StructuredSelection)currentParts.getSelection();
		if (currentSelection.size() > 0 && !hasInheritedPart(currentSelection))
		{
			executeCommand(new RefreshingCommand(new RemovePartsCommand(editor.getForm(), currentSelection.toArray()))
			{
				@Override
				public void refresh(boolean haveExecuted)
				{
					if (haveExecuted)
					{
						currentParts.setSelection(StructuredSelection.EMPTY);
					}
					else
					{
						// undo
						fillPartsSection(); // for resetting back old selection
						currentParts.setSelection(currentSelection);
					}
				}
			});
		}
	}

	protected void handleMoveParts(boolean up)
	{
		final StructuredSelection currentSelection = ((StructuredSelection)currentParts.getSelection());
		if (currentSelection.size() == 1)
		{
			executeCommand(new RefreshingCommand(createMovePartsCommand((Part)currentSelection.getFirstElement(), up))
			{
				@Override
				public void refresh(boolean haveExecuted)
				{
					currentParts.setSelection(currentSelection);
				}
			});
		}
	}

	/**
	 * Execute a command on the command stack and refresh the views.
	 *
	 * @param command
	 */
	protected void executeCommand(Command command)
	{
		if (command == null)
		{
			return;
		}
		editor.getCommandStack().execute(command);
	}

	protected Command createMovePartsCommand(Part partToMove, boolean up)
	{
		Part switchPart = getSiblingPart(partToMove, up);
		if (switchPart == null)
		{
			return null;
		}

		CompoundCommand command = new CompoundCommand("Move Part " + (up ? "up" : "down"));

		command.add(SetValueCommand.createSetvalueCommand("", PersistPropertySource.createPersistPropertySource(partToMove, editor.getForm(), false),
			StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName(), Integer.valueOf(switchPart.getHeight())));

		command.add(SetValueCommand.createSetvalueCommand("", PersistPropertySource.createPersistPropertySource(switchPart, editor.getForm(), false),
			StaticContentSpecLoader.PROPERTY_HEIGHT.getPropertyName(), Integer.valueOf(partToMove.getHeight())));

		return command;
	}

	protected void handleAddGroupByFields()
	{
		if (currentPartBean.part == null)
		{
			return;
		}

		IndexedStructuredSelection selection = (IndexedStructuredSelection)groupByFields.getSelection();
		if (selection.size() > 1)
		{
			return;
		}

		DataProviderDialog dialog;
		FlattenedSolution flattenedSolution = ModelUtils.getEditingFlattenedSolution(editor.getForm());
		dialog = new DataProviderDialog(getShell(), DataProviderLabelProvider.INSTANCE_HIDEPREFIX, PersistContext.create(editor.getForm()), flattenedSolution,
			flattenedSolution.getTable(flattenedSolution.getFlattenedForm(editor.getForm()).getDataSource()),
			new DataProviderTreeViewer.DataProviderOptions(false, true, false, false, false, false, false, false, INCLUDE_RELATIONS.NESTED, false, true, null),
			null, SWT.MULTI, "Select Group-by fields");
		if (dialog.open() == SWT.CANCEL)
		{
			return;
		}

		Object[] dataProviders = ((IStructuredSelection)dialog.getSelection()).toArray();

		String[] dataProviderIds = new String[dataProviders.length];
		for (int i = 0; i < dataProviders.length; i++)
		{
			dataProviderIds[i] = ((IDataProvider)dataProviders[i]).getDataProviderID();
		}

		String[] oldGroupbyDataProviderIDs = (String[])currentPartBean.getPartProperty("groupbyDataProviderIDs");

		int position;
		if (selection.size() == 1)
		{
			// add after selected item
			position = selection.getSelectedIndices()[0] + 1;
		}
		else
		{
			// add at the end
			position = oldGroupbyDataProviderIDs.length;
		}

		String[] newGroupbyDataProviderIDs = Utils.arrayInsert(oldGroupbyDataProviderIDs, dataProviderIds, position, dataProviderIds.length);
		currentPartBean.setPartProperty("groupbyDataProviderIDs", newGroupbyDataProviderIDs);
	}

	protected void handleRemoveGroupByFields()
	{
		IndexedStructuredSelection selection = (IndexedStructuredSelection)groupByFields.getSelection();
		int[] indices = selection.getSelectedIndices();

		// groupbyDataProviderIDs was split up by converter see PersistProperties
		String[] dataProviderIds = (String[])currentPartBean.getPartProperty("groupbyDataProviderIDs");
		if (dataProviderIds != null)
		{
			List<String> lst = new ArrayList<String>();
			int idx = 0;
			for (int i = 0; i < dataProviderIds.length; i++)
			{
				if (idx < indices.length && indices[idx] == i)
				{
					// skip this data provider
					idx++;
				}
				else
				{
					lst.add(dataProviderIds[i]);
				}
			}
			currentPartBean.setPartProperty("groupbyDataProviderIDs", lst.toArray(new String[lst.size()]));
		}
	}

	protected DataBindingContext initDataBindings()
	{
		IObservableValue currentPartSinkWhenLastObserveValue = PojoProperties.value(PartBean.class, "sinkWhenLast").observe(currentPartBean);
		IObservableValue currentPartAllowBreakAcrossPageBoundsObserveValue = PojoProperties.value("allowBreakAcrossPageBounds").observe(currentPartBean);
		IObservableValue pageBreakAfterTextTextObserveWidget = WidgetProperties.text(SWT.Modify).observe(pageBreakAfterText);
		IObservableValue pageBreakBeforeButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(pageBreakBeforeButton);
		IObservableValue currentPartPageBreakAfterOccurrenceObserveValue = PojoProperties.value(PartBean.class, "pageBreakAfterOccurrence")
			.observe(currentPartBean);
		IObservableValue currentPartDiscardRemainderAfterBreakObserveValue = PojoProperties.value(PartBean.class, "discardRemainderAfterBreak")
			.observe(currentPartBean);
		IObservableValue currentPartRestartPageNumberObserveValue = PojoProperties.value(PartBean.class, "restartPageNumber").observe(currentPartBean);
		IObservableValue pageBreakAfterButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(pageBreakAfterButton);
		IObservableValue allowPartToButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(allowPartToButton);
		IObservableValue discardRemainderOfButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(discardRemainderOfButton);
		IObservableValue sinkPartIfButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(sinkPartIfButton);
		IObservableValue currentPartPageBreakBeforeObserveValue = PojoProperties.value(PartBean.class, "pageBreakBefore").observe(currentPartBean);
		IObservableValue currentPartPageBreakAfterEveryNthOccurenceObserveValue = PojoProperties.value(PartBean.class, "pageBreakAfterEveryNthOccurence")
			.observe(currentPartBean);
		IObservableValue restartPageNumbersButtonSelectionObserveWidget = WidgetProperties.widgetSelection().observe(restartPageNumbersButton);
		//
		//
		DataBindingContext bindingContext = new DataBindingContext();
		//
		bindingContext.bindValue(sinkPartIfButtonSelectionObserveWidget, currentPartSinkWhenLastObserveValue, null, null);
		bindingContext.bindValue(pageBreakBeforeButtonSelectionObserveWidget, currentPartPageBreakBeforeObserveValue, null, null);
		bindingContext.bindValue(pageBreakAfterButtonSelectionObserveWidget, currentPartPageBreakAfterEveryNthOccurenceObserveValue, null, null);
		bindingContext.bindValue(restartPageNumbersButtonSelectionObserveWidget, currentPartRestartPageNumberObserveValue, null, null);
		bindingContext.bindValue(allowPartToButtonSelectionObserveWidget, currentPartAllowBreakAcrossPageBoundsObserveValue, null, null);
		bindingContext.bindValue(discardRemainderOfButtonSelectionObserveWidget, currentPartDiscardRemainderAfterBreakObserveValue, null, null);
		bindingContext.bindValue(pageBreakAfterTextTextObserveWidget, currentPartPageBreakAfterOccurrenceObserveValue, null, null);
		//
		return bindingContext;
	}

	/**
	 * Wrapper bean for the current part.
	 * <p>
	 * Changes to the bean properties are set via the command stack.
	 *
	 * @author rgansevles
	 *
	 */
	public class PartBean
	{
		private Part part;
		private IPropertySource propertySource;

		public void setPart(Part part)
		{
			this.part = part;
			propertySource = null;
		}

		public boolean isPageBreakBefore()
		{
			return part != null && part.getPageBreakBefore();
		}

		public void setPageBreakBefore(boolean pageBreakBefore)
		{
			setPartProperty("pageBreakBefore", Boolean.valueOf(pageBreakBefore));
		}

		public boolean isRestartPageNumber()
		{
			return part != null && part.getRestartPageNumber();
		}

		public void setRestartPageNumber(boolean restartPageNumber)
		{
			setPartProperty("restartPageNumber", Boolean.valueOf(restartPageNumber));
		}

		public boolean isAllowBreakAcrossPageBounds()
		{
			return part != null && part.getAllowBreakAcrossPageBounds();
		}

		public void setAllowBreakAcrossPageBounds(boolean allowBreakAcrossPageBounds)
		{
			setPartProperty("allowBreakAcrossPageBounds", Boolean.valueOf(allowBreakAcrossPageBounds));
		}

		public boolean isDiscardRemainderAfterBreak()
		{
			return part != null && part.getDiscardRemainderAfterBreak();
		}

		public void setDiscardRemainderAfterBreak(boolean discardRemainderAfterBreak)
		{
			setPartProperty("discardRemainderAfterBreak", Boolean.valueOf(discardRemainderAfterBreak));
		}

		public boolean isSinkWhenLast()
		{
			return part != null && part.getSinkWhenLast();
		}

		public void setSinkWhenLast(boolean sinkWhenLast)
		{
			setPartProperty("sinkWhenLast", Boolean.valueOf(sinkWhenLast));
		}

		public boolean isPageBreakAfterEveryNthOccurence()
		{
			return part != null && part.getPageBreakAfterOccurrence() > 0;
		}

		public void setPageBreakAfterEveryNthOccurence(boolean pageBreakAfterEveryNthOccurence)
		{
			setPartProperty("pageBreakAfterOccurrence", Integer.valueOf(pageBreakAfterEveryNthOccurence ? 1 : 0));
		}

		public String getPageBreakAfterOccurrence()
		{
			return part != null && part.getPageBreakAfterOccurrence() > 0 ? String.valueOf(part.getPageBreakAfterOccurrence()) : "";
		}

		public void setPageBreakAfterOccurrence(String pageBreakAfterOccurrence)
		{
			setPartIntegerProperty("pageBreakAfterOccurrence", pageBreakAfterOccurrence, 0);
		}

		public void setPartIntegerProperty(String property, String value, int def)
		{
			int n = def;
			try
			{
				n = Integer.parseInt(value);
			}
			catch (NumberFormatException e)
			{ // ignore
			}
			setPartProperty(property, Integer.valueOf(n));
		}

		/**
		 * Set the property via a command that is executed on the editors' command stack.
		 *
		 * @param property
		 * @param value
		 */
		private Object getPartProperty(String property)
		{
			if (part == null)
			{
				return null;
			}
			if (propertySource == null)
			{
				propertySource = PersistPropertySource.createPersistPropertySource(part, editor.getForm(), false);
				if (propertySource == null)
				{
					// part was deleted
					return null;
				}
			}
			return propertySource.getPropertyValue(property);
		}

		/**
		 * Set the property via a command that is executed on the editors' command stack.
		 *
		 * @param property
		 * @param value
		 */
		private void setPartProperty(String property, Object value)
		{
			if (part != null)
			{
				// set the property via the command stack to support undo/redo
				executeCommand(SetValueCommand.createSetvalueCommand("Edit Part Property",
					PersistPropertySource.createPersistPropertySource(part, editor.getForm(), false), property, value));
			}
		}
	}

	public class RemovePartsCommand extends Command
	{
		private final Form form;
		private final Object[] parts;
		List<Part> removedParts = null;

		public RemovePartsCommand(Form form, Object[] parts)
		{
			this.form = form;
			this.parts = parts;
			setLabel("Remove part(s)");
		}

		@Override
		public void execute()
		{
			List<Part> list = new ArrayList<Part>();
			for (Object p : parts)
			{
				Part part = ((Part)p);
				list.add(part);
				try
				{
					((IDeveloperRepository)part.getRootObject().getRepository()).deleteObject(part);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not delete part " + part, e);
				}
				ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, part, false);
			}
			removedParts = list;
		}

		@Override
		public boolean canUndo()
		{
			return removedParts != null && removedParts.size() > 0;
		}

		@Override
		public void undo()
		{
			List<Part> list = removedParts;
			removedParts = null;
			for (Part part : list)
			{
				try
				{
					((IDeveloperRepository)form.getRootObject().getRepository()).undeleteObject(form, part);
					ServoyModelManager.getServoyModelManager().getServoyModel().firePersistChanged(false, part, false);
				}
				catch (RepositoryException e)
				{
					ServoyLog.logError("Could not restore part " + part, e);
				}
			}
		}
	}
}
