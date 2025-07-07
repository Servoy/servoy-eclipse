/*
 This file belongs to the Servoy development and deployment environment, Copyright (C) 1997-2012 Servoy BV

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

package com.servoy.eclipse.ui.wizards;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Iterator;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.IWorkspaceRunnable;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.MultiStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.jface.dialogs.ErrorDialog;
import org.eclipse.jface.dialogs.IMessageProvider;
import org.eclipse.jface.layout.TreeColumnLayout;
import org.eclipse.jface.operation.IRunnableWithProgress;
import org.eclipse.jface.viewers.CheckboxCellEditor;
import org.eclipse.jface.viewers.ColumnPixelData;
import org.eclipse.jface.viewers.ColumnWeightData;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.TreeViewerColumn;
import org.eclipse.jface.wizard.Wizard;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.events.SelectionListener;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.layout.FormAttachment;
import org.eclipse.swt.layout.FormData;
import org.eclipse.swt.layout.FormLayout;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Shell;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.model.util.WorkspaceFileAccess;
import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.wizards.SynchronizeDBIWithDBWizard.SplitInThreeWizardPage;
import com.servoy.eclipse.ui.wizards.SynchronizeDBIWithDBWizard.SplitInThreeWizardPage.PairTreeContentProvider;
import com.servoy.eclipse.ui.wizards.SynchronizeDBIWithDBWizard.SplitInThreeWizardPage.RadioEditingSupport;
import com.servoy.j2db.IDebugJ2DBClient;
import com.servoy.j2db.dataprocessing.FoundSetManager;
import com.servoy.j2db.dataprocessing.MetaDataUtils;
import com.servoy.j2db.persistence.ITable;
import com.servoy.j2db.persistence.Table;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.SortedList;

/**
 * This wizard updates Meta data from workspace to database
 * @author obuligan
 *
 * @since 6.1.3
 */
public class UpdateMetaDataWziard extends Wizard
{
	// List of   Server name Table name Pair
	private SplitInTwoWizardPage<String, ITable> overwriteSelectionPage;
	private List<Pair<String, ITable>> tablesWithDataInDB = null;
	private List<Pair<String, ITable>> tablesWithoutDataInDB = null;
	private Shell shell = null;

	public UpdateMetaDataWziard(List<ITable> tablesWithDataInDB, List<ITable> tablesWithoutDataInDB, Shell shell)
	{
		super();
		setNeedsProgressMonitor(true);
		this.tablesWithDataInDB = new ArrayList<Pair<String, ITable>>();
		this.tablesWithoutDataInDB = new ArrayList<Pair<String, ITable>>();
		for (ITable t : tablesWithDataInDB)
		{
			this.tablesWithDataInDB.add(new Pair<String, ITable>(t.getServerName(), t));
		}
		for (ITable t : tablesWithoutDataInDB)
		{
			this.tablesWithoutDataInDB.add(new Pair<String, ITable>(t.getServerName(), t));
		}
		this.shell = shell;//this.getShell();
	}

	@Override
	public void addPages()
	{
		if (tablesWithDataInDB.size() == 0)
		{
			addPage(new WizardPage("All tables that are to be synchronized are empty in the database")
			{
				public void createControl(Composite parent)
				{
					setControl(new Label(parent, SWT.NONE));
					setTitle(getName());
				}
			});
		}
		else
		{
			Comparator<Pair<String, ITable>> comparator = new Comparator<Pair<String, ITable>>()
			{
				public int compare(Pair<String, ITable> o1, Pair<String, ITable> o2)
				{
					if (o1 == null && o2 == null) return 0;
					if (o1 == null) return -1;
					if (o2 == null) return 1;

					int result = o1.getLeft().compareToIgnoreCase(o2.getLeft());
					if (result == 0)
					{
						result = o1.getRight().getName().compareToIgnoreCase(o2.getRight().getName());
					}
					return result;

				}
			};

			Image serverImage = Activator.getDefault().loadImageFromBundle("server.png");
			Image tableImage = Activator.getDefault().loadImageFromBundle("portal.png");
			overwriteSelectionPage = new SplitInTwoWizardPage<String, ITable>("The following tables are not empty",
				"Data synchronize will overwrite existing data, continue?", "Skip", "Overwrite data", "Skip all/multiselection", "Overwrite all/multiselection",
				tablesWithDataInDB, comparator, serverImage, tableImage);

			addPage(overwriteSelectionPage);
		}
	}

	/*
	 * (non-Javadoc)
	 *
	 * @see org.eclipse.jface.wizard.Wizard#performFinish()
	 */
	@Override
	public boolean performFinish()
	{
		try
		{
			getContainer().run(true, false, new IRunnableWithProgress()
			{
				public void run(IProgressMonitor m) throws InvocationTargetException, InterruptedException
				{
					try
					{
						ServoyModel.getWorkspace().run(new IWorkspaceRunnable()
						{
							public void run(IProgressMonitor monitor) throws CoreException
							{
								List<Pair<String, ITable>> toUpdate = new ArrayList<Pair<String, ITable>>();
								if (overwriteSelectionPage != null)
								{
									//set2  the selected column to overwrite
									toUpdate.addAll(overwriteSelectionPage.getSet2());
								}
								toUpdate.addAll(tablesWithoutDataInDB);
								int work = toUpdate.size();

								monitor.beginTask("Synchronizing database meta data", work);
								try
								{
									MultiStatus warnings = new MultiStatus(Activator.PLUGIN_ID, 0, "For more information please click 'Details'.", null);
									StringBuilder sb = updateMetaData(toUpdate, warnings, monitor);
									/*
									 * handle the updating result
									 */
									// show warning status messages in eclipse Platform UI way
									if (warnings.getChildren().length > 0)
									{
										final MultiStatus fw = warnings;
										UIUtils.runInUI(new Runnable()
										{
											public void run()
											{
												ErrorDialog.openError(getShell(), null, null, fw);
											}
										}, false);
									}
									if (sb.length() > 0)
									{
										UIUtils.showScrollableDialog(shell, IMessageProvider.INFORMATION, "Update status", "The folowing tables were updated:",
											sb.toString());
									}
									else
									{
										UIUtils.showInformation(shell, "No tables were updated", "No tables in the database were updated.");
									}
								}
								finally
								{
									monitor.done();
								}
							}
						}, ServoyModel.getWorkspace().getRoot(), IWorkspace.AVOID_UPDATE, m);
					}
					catch (CoreException e)
					{
						ServoyLog.logError(e);
					}
				}

				/**
				 *  This method does the actual update to the database metadata tables with the selected tables in the wizard
				 * @param overrideSet
				 * @param subProgressMonitor
				 * @param warnings
				 * @param monitor
				 */
				private StringBuilder updateMetaData(List<Pair<String, ITable>> overrideSet, MultiStatus warnings, IProgressMonitor monitor)
				{
					StringBuilder sb = new StringBuilder();
					for (Pair<String, ITable> tablePair : overrideSet)
					{
						ITable table = tablePair.getRight();
						IFile dataFile = ServoyModelManager.getServoyModelManager().getServoyModel().getDataModelManager().getMetaDataFile(
							table.getDataSource());

						try
						{
							if (dataFile == null)
							{
								warnings.add(
									new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Cannot find data file for datasource '" + table.getDataSource() + "'."));
								continue;
							}

							// import file into table
							if (!dataFile.exists())
							{
								warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID,
									"Data file for datasource '" + table.getDataSource() + "' does not exist."));
								continue;
							}

							// read the json
							String contents = new WorkspaceFileAccess(ServoyModel.getWorkspace()).getUTF8Contents(dataFile.getFullPath().toString());

							int ninserted = MetaDataUtils.loadMetadataInTable(table, contents);

							// flush developer clients
							IDebugJ2DBClient debugJ2DBClient = com.servoy.eclipse.core.Activator.getDefault().getDebugJ2DBClient();
							if (debugJ2DBClient != null)
							{
								((FoundSetManager)debugJ2DBClient.getFoundSetManager()).flushCachedDatabaseData(table.getDataSource());
							}
							sb.append("Successfully saved " + ninserted + " records from workspace in table " + table.getName() + " in server " +
								table.getServerName());
							sb.append('\n');
						}
						catch (Exception e)
						{
							ServoyLog.logError("Error updating table", e);
							warnings.add(new Status(IStatus.WARNING, Activator.PLUGIN_ID, "Error updating table: " + e.getMessage()));
						}
						finally
						{
							monitor.worked(1);
						}
					}
					return sb;
				}

			});
		}
		catch (InvocationTargetException e)
		{
			ServoyLog.logError(e);
		}
		catch (InterruptedException e)
		{
			// operation canceled
		}
		return true;
	}
}


/**
 * The wizard page that is used by the user to split a set of tables into two sets.<br>
 * UI consists of a single tree table with check boxes.
 * Similar to {@link SplitInThreeWizardPage}
 *
 * @param <T1> the type of the first element in the pair.
 * @param <T2> the type of the second element in the pair.
 * @since 6.1.3
 */
class SplitInTwoWizardPage<T1, T2> extends WizardPage
{

	public static final int NO_CHECKBOX = -1;
	public static final int DATA = 0;
	public static final int SET1 = 1;
	public static final int SET2 = 2;

	private TreeViewer treeViewer;
	private final String labelForSet1;
	private final String labelForSet2;

	private final SortedList<Pair<T1, T2>> initialPairs;
	private PairTreeContentProvider<T1, T2> contentProvider;
	private final Image image1;
	private final Image image2;
	private final String allSet1Label;
	private final String allSet2Label;


	/**
	 * Creates a new split-in-two wizard page. It will split the initial set "servers" into 3 subsets.
	 * Similar to {@link SplitInThreeWizardPage}
	 * @param fatherChildrenPairs the initial set to be split up.
	 * @param title the title of the page.
	 * @param description the description of the page.
	 * @param image2
	 * @param image1
	 * @param labelProvider the label provider used by the three tree viewers.
	 */
	public SplitInTwoWizardPage(String title, String description, String labelForSet1, String labelForSet2, String allSet1Label, String allSet2Label,
		List<Pair<T1, T2>> fatherChildrenPairs, Comparator<Pair<T1, T2>> c, Image image1, Image image2)
	{
		super(title);
		this.initialPairs = new SortedList<Pair<T1, T2>>(c, fatherChildrenPairs);

		setTitle(title);
		setDescription(description);

		this.labelForSet1 = labelForSet1;
		this.labelForSet2 = labelForSet2;

		this.allSet1Label = allSet1Label;
		this.allSet2Label = allSet2Label;

		this.image1 = image1;
		this.image2 = image2;

	}

	public void createControl(Composite parent)
	{
		initializeDialogUnits(parent);

		Composite topLevel = new Composite(parent, SWT.NONE);
		topLevel.setLayoutData(new GridData(GridData.VERTICAL_ALIGN_FILL | GridData.HORIZONTAL_ALIGN_FILL));
		setControl(topLevel);
		FormLayout formLayout = new FormLayout();
		formLayout.marginHeight = formLayout.marginWidth = 5;
		topLevel.setLayout(formLayout);

		// set up components
		Composite treeComposite = new Composite(topLevel, SWT.NONE);
		treeViewer = new TreeViewer(treeComposite, SWT.BORDER | SWT.FULL_SELECTION | SWT.MULTI);

		Tree tree = treeViewer.getTree();
		tree.setLinesVisible(true);
		tree.setHeaderVisible(true);

		contentProvider = new PairTreeContentProvider<T1, T2>();
		treeViewer.setContentProvider(contentProvider);
		treeViewer.setInput(initialPairs);

		treeViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				IStructuredSelection sel = (IStructuredSelection)event.getSelection();
				Object element = sel.getFirstElement();
				if (element != null)
				{
					if (treeViewer.getExpandedState(element))
					{
						treeViewer.collapseToLevel(element, 1);
					}
					else
					{
						treeViewer.expandToLevel(element, 1);
					}
				}
			}
		});

		final CheckboxCellEditor editor = new CheckboxCellEditor(tree, SWT.RADIO);
		TreeColumn dataColumn = new TreeColumn(treeViewer.getTree(), SWT.LEFT, DATA);
		dataColumn.setText("");
		/* dataViewerColumn */new TreeViewerColumn(treeViewer, dataColumn);

		TreeColumn set1Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET1);
		set1Column.setText(labelForSet1);
		TreeViewerColumn set1ViewerColumn = new TreeViewerColumn(treeViewer, set1Column);
		set1ViewerColumn.setEditingSupport(new RadioEditingSupport<T1, T2>(treeViewer, contentProvider, editor, SET1));

		TreeColumn set2Column = new TreeColumn(treeViewer.getTree(), SWT.CENTER, SET2);
		set2Column.setText(labelForSet2);
		TreeViewerColumn set2ViewerColumn = new TreeViewerColumn(treeViewer, set2Column);
		set2ViewerColumn.setEditingSupport(new RadioEditingSupport<T1, T2>(treeViewer, contentProvider, editor, SET2));


		treeViewer.setLabelProvider(new PairSplitLabelProvider());
		treeViewer.expandAll();

		Button allInSet1 = new Button(topLevel, SWT.NONE);
		Button allInSet2 = new Button(topLevel, SWT.NONE);

		allInSet1.setText(allSet1Label);
		allInSet2.setText(allSet2Label);

		allInSet1.setToolTipText("Affects multiple selection or all elements(if single selection)");
		allInSet2.setToolTipText("Affects multiple selection or all elements(if single selection)");

		allInSet1.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e)
			{
				moveAllOrMultiSelectionToSet(SET1);
			}

			public void widgetSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}
		});
		allInSet2.addSelectionListener(new SelectionListener()
		{
			public void widgetDefaultSelected(SelectionEvent e)
			{
				moveAllOrMultiSelectionToSet(SET2);
			}

			public void widgetSelected(SelectionEvent e)
			{
				widgetDefaultSelected(e);
			}
		});

		// layout components
		TreeColumnLayout layout = new TreeColumnLayout();
		treeComposite.setLayout(layout);
		layout.setColumnData(dataColumn, new ColumnWeightData(55, 150, true));
		layout.setColumnData(set1Column, new ColumnPixelData(100, true));
		layout.setColumnData(set2Column, new ColumnPixelData(100, true));

		FormData fd = new FormData();
		fd.left = new FormAttachment(0, 0);
		fd.right = new FormAttachment(100, 0);
		fd.top = new FormAttachment(0, 0);
		fd.bottom = new FormAttachment(allInSet1, -5);
		treeComposite.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, 0);
		fd.right = new FormAttachment(allInSet2, -5);
		allInSet1.setLayoutData(fd);

		fd = new FormData();
		fd.bottom = new FormAttachment(100, 0);
		fd.right = new FormAttachment(100, -5);
		allInSet2.setLayoutData(fd);
	}

	private void moveAllOrMultiSelectionToSet(int set)
	{
		ISelection selection = treeViewer.getSelection();
		if (selection instanceof IStructuredSelection && ((IStructuredSelection)selection).size() > 1)
		{
			Iterator< ? > it = ((IStructuredSelection)selection).iterator();
			while (it.hasNext())
			{
				Object element = it.next();
				if (element instanceof Pair)
				{
					contentProvider.setSet(element, Integer.valueOf(set));
				}
			}
		}
		else
		{
			for (Pair<T1, T2> p : initialPairs)
			{
				contentProvider.setSet(p, Integer.valueOf(set));
			}
		}

		treeViewer.refresh();
	}

	public List<Pair<T1, T2>> getSet1()
	{
		ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
		for (Pair<T1, T2> p : initialPairs)
		{
			if (contentProvider.getSet(p) == SET1)
			{
				l.add(p);
			}
		}
		return l;
	}

	public List<Pair<T1, T2>> getSet2()
	{
		ArrayList<Pair<T1, T2>> l = new ArrayList<Pair<T1, T2>>();
		for (Pair<T1, T2> p : initialPairs)
		{
			if (contentProvider.getSet(p) == SET2)
			{
				l.add(p);
			}
		}
		return l;
	}

	private class PairSplitLabelProvider extends LabelProvider implements ITableLabelProvider
	{

		public Image getColumnImage(Object element, int columnIndex)
		{
			if (columnIndex == DATA)
			{
				if (element instanceof Pair)
				{
					return image2;
				}
				else
				{
					return image1;
				}
			}
			return getImage(element);
		}

		public String getColumnText(Object element, int columnIndex)
		{
			if (columnIndex == 0)
			{
				if (element instanceof Pair)
				{
					return ((Pair<String, Table>)element).getRight().getName();
				}
				if (element instanceof String)
				{
					return (String)element;
				}
			}
			else if (element instanceof Pair)
			{
				return (contentProvider.getSet(element) == columnIndex) ? "x" : "";
			}
			return "";
		}
	}


}
