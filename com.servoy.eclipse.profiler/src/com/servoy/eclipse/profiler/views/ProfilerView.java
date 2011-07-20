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
package com.servoy.eclipse.profiler.views;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Path;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.dltk.core.IMethod;
import org.eclipse.dltk.core.ISourceModule;
import org.eclipse.dltk.internal.ui.editor.EditorUtility;
import org.eclipse.dltk.ui.DLTKUIPlugin;
import org.eclipse.jface.action.Action;
import org.eclipse.jface.action.IAction;
import org.eclipse.jface.action.IMenuListener;
import org.eclipse.jface.action.IMenuManager;
import org.eclipse.jface.action.IToolBarManager;
import org.eclipse.jface.action.MenuManager;
import org.eclipse.jface.action.Separator;
import org.eclipse.jface.text.BadLocationException;
import org.eclipse.jface.text.IDocument;
import org.eclipse.jface.text.IRegion;
import org.eclipse.jface.viewers.DoubleClickEvent;
import org.eclipse.jface.viewers.IDoubleClickListener;
import org.eclipse.jface.viewers.ILabelProviderListener;
import org.eclipse.jface.viewers.ISelection;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredContentProvider;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITableLabelProvider;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerSorter;
import org.eclipse.swt.SWT;
import org.eclipse.swt.custom.SashForm;
import org.eclipse.swt.dnd.Clipboard;
import org.eclipse.swt.dnd.TextTransfer;
import org.eclipse.swt.dnd.Transfer;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.Table;
import org.eclipse.swt.widgets.TableColumn;
import org.eclipse.swt.widgets.Tree;
import org.eclipse.swt.widgets.TreeColumn;
import org.eclipse.ui.IActionBars;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IEditorPart;
import org.eclipse.ui.IMemento;
import org.eclipse.ui.IPageService;
import org.eclipse.ui.IPerspectiveDescriptor;
import org.eclipse.ui.IViewSite;
import org.eclipse.ui.IWorkbenchActionConstants;
import org.eclipse.ui.IWorkbenchPage;
import org.eclipse.ui.IWorkbenchPartReference;
import org.eclipse.ui.PartInitException;
import org.eclipse.ui.PerspectiveAdapter;
import org.eclipse.ui.WorkbenchException;
import org.eclipse.ui.XMLMemento;
import org.eclipse.ui.part.DrillDownAdapter;
import org.eclipse.ui.part.ViewPart;
import org.eclipse.ui.texteditor.IDocumentProvider;
import org.eclipse.ui.texteditor.ITextEditor;

import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.model.util.ServoyLog;
import com.servoy.eclipse.profiler.Activator;
import com.servoy.eclipse.ui.resource.FileEditorInputFactory;
import com.servoy.j2db.debug.DataCallProfileData;
import com.servoy.j2db.debug.IProfileListener;
import com.servoy.j2db.debug.ProfileData;
import com.servoy.j2db.debug.RemoteDebugScriptEngine;

/**
 * This sample class demonstrates how to plug-in a new workbench view. The view
 * shows data obtained from the model. The sample creates a dummy model on the
 * fly, but a real implementation would connect to the model available either in
 * this or another plug-in (e.g. the workspace). The view is connected to the
 * model using a content provider.
 * <p>
 * The view uses a label provider to define how model objects should be
 * presented in the view. Each view can present the same model objects using
 * different labels and icons, if needed. Alternatively, a single label provider
 * can be shared between views in order to ensure that objects of the same type
 * are presented in the same way everywhere.
 * <p>
 */
@SuppressWarnings("nls")
public class ProfilerView extends ViewPart
{
	private static final String PROFILER_VIEW = "ProfilerViewSettings";

	public static final String METHOD_NAME_COLUMN_WIDTH_SETTING = "profilerView.methodNameColumnWidth";
	public static final String OWN_TIME_COLUMN_WIDTH_SETTING = "profilerView.ownTimeColumnWidth";
	public static final String TIME_COLUMN_WIDTH_SETTING = "profilerView.timeColumnWidth";
	public static final String FILE_COLUMN_WIDTH_SETTING = "profilerView.fileColumnWidth";
	public static final String ARGS_COLUMN_WIDTH_SETTING = "profilerView.argsColumnWidth";
	public static final String NAME_TABLE_COLUMN_WIDTH_SETTING = "profilerView.nameTableColumnWidth";
	public static final String TIME_TABLE_COLUMN_WIDTH_SETTING = "profilerView.timeTableColumnWidth";
	public static final String QUERY_TABLE_COLUMN_WIDTH_SETTING = "profilerView.queryTableColumnWidth";
	public static final String ARGUMENTS_TABLE_COLUMN_WIDTH_SETTING = "profilerView.argumentsTableColumnWidth";
	public static final String DATASOURCE_TABLE_COLUMN_WIDTH_SETTING = "profilerView.datasourceTableColumnWidth";
	public static final String TRANSACTION_TABLE_COLUMN_WIDTH_SETTING = "profilerView.transactionTableColumnWidth";
	public static final String TREE_WIDTH_SETTING = "profilerView.treeWidth";
	public static final String TABLE_WIDTH_SETTING = "profilerView.tableWidth";

	public static final int METHOD_NAME_COLUMN_WIDTH_DEFAULT = 200;
	public static final int OWN_TIME_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TIME_COLUMN_WIDTH_DEFAULT = 80;
	public static final int FILE_COLUMN_WIDTH_DEFAULT = 400;
	public static final int ARGS_COLUMN_WIDTH_DEFAULT = 120;
	public static final int NAME_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TIME_TABLE_COLUMN_WIDTH_DEFAULT = 70;
	public static final int QUERY_TABLE_COLUMN_WIDTH_DEFAULT = 350;
	public static final int ARGUMENTS_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int DATASOURCE_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TRANSACTION_TABLE_COLUMN_WIDTH_DEFAULT = 100;
	public static final int TREE_WIDTH_DEFAULT = 50;
	public static final int TABLE_WIDTH_DEFAULT = 50;

	private static final class AggregateData
	{
		private final String methodName;

		private final String sourceName;

		private int count;

		private long time;

		private long ownTime;

		private final List<AggregateData> callees = new ArrayList<AggregateData>();

		private HashMap<String, DataCallProfileData> dataCallProfileDataMap;

		private Long queryTime;

		private Integer queryCount;

		private int innerFunctionLineStart = -1;

		/**
		 * @param aggregateData
		 */
		public AggregateData(ProfileData pd)
		{
			this.methodName = pd.getMethodName();
			this.sourceName = pd.getSourceName();
			if (pd.isInnerFunction() && pd.getLineNumbers() != null && pd.getLineNumbers().length > 0)
			{
				this.innerFunctionLineStart = pd.getLineNumbers()[0];
			}
			if (pd.getDataCallProfileDatas() != null && !pd.getDataCallProfileDatas().isEmpty())
			{
				dataCallProfileDataMap = new HashMap<String, DataCallProfileData>();
			}
			add(pd);
		}

		public int getInnerFunctionLineStart()
		{
			return innerFunctionLineStart;
		}

		public void add(ProfileData pd)
		{
			if (pd.getMethodName().equals(methodName) && pd.getSourceName().equals(sourceName))
			{
				if (pd.isInnerFunction() && pd.getLineNumbers() != null && pd.getLineNumbers().length > 0)
				{
					if (this.innerFunctionLineStart != pd.getLineNumbers()[0])
					{
						throw new IllegalArgumentException("ProfileData should have the right method and/or sourcename and inner function linestart");
					}
				}
				time += pd.getTime();
				ownTime += pd.getOwnTime();
				count++;

				for (DataCallProfileData dataCallProfileData : pd.getDataCallProfileDatas())
				{
					DataCallProfileData oldDataCallProfileData = dataCallProfileDataMap.get(dataCallProfileData.getQuery());
					if (oldDataCallProfileData != null)
					{
						DataCallProfileData newDataCallProfileData = new DataCallProfileData(oldDataCallProfileData.getName(),
							oldDataCallProfileData.getDatasource(), oldDataCallProfileData.getTransactionId(), 0, oldDataCallProfileData.getTime() +
								dataCallProfileData.getTime(), oldDataCallProfileData.getQuery(), oldDataCallProfileData.getArgumentString(),
							dataCallProfileData.getCount() + oldDataCallProfileData.getCount());
						dataCallProfileDataMap.put(oldDataCallProfileData.getQuery(), newDataCallProfileData);
					}
					else
					{
						dataCallProfileDataMap.put(dataCallProfileData.getQuery(), dataCallProfileData);
					}
				}

				if (pd.getParent() != null)
				{
					AggregateData parent = new AggregateData(pd.getParent());
					int index = callees.indexOf(parent);
					if (index == -1)
					{
						callees.add(parent);
					}
					else
					{
						parent = callees.get(index);
						parent.increaseCount();
					}
				}

			}
			else
			{
				throw new IllegalArgumentException("ProfileData should have the right method and/or sourcename");
			}
		}

		public void increaseCount()
		{
			count++;
		}

		public AggregateData[] getChildren()
		{
			return callees.toArray(new AggregateData[0]);
		}

		/**
		 * @see java.lang.Object#equals(java.lang.Object)
		 */
		@Override
		public boolean equals(Object obj)
		{
			if (obj instanceof AggregateData)
			{
				AggregateData ad = (AggregateData)obj;
				return ad.methodName.equals(methodName) && ad.sourceName.equals(sourceName) && ad.innerFunctionLineStart == this.innerFunctionLineStart;
			}
			return false;
		}

		/**
		 * @return
		 */
		public String getMethodName()
		{
			return methodName;
		}

		/**
		 * @return
		 */
		public String getSourceName()
		{
			return sourceName;
		}

		/**
		 * @return
		 */
		public long getOwnTime()
		{
			return ownTime;
		}

		/**
		 * @return
		 */
		public long getTime()
		{
			return time;
		}

		/**
		 * @return
		 */
		public int getCount()
		{
			return count;
		}

		/**
		 * @param sb
		 */
		public void toXML(StringBuilder sb)
		{
			String childPrefix = "\t"; //$NON-NLS-1$

			int endLine = sb.lastIndexOf("\n"); //$NON-NLS-1$
			if (endLine != -1)
			{
				childPrefix = sb.substring(endLine + 1) + '\t';
			}

			sb.append("<aggregatedata ");
			sb.append("methodname=\"");
			sb.append(methodName);
			sb.append("\" owntime=\"");
			sb.append(ownTime);
			sb.append("\" totaltime=\"");
			sb.append(time);
			sb.append("\" count=\"");
			sb.append(count);
			sb.append("\" source=\"");
			sb.append(sourceName);
			sb.append("\">");
			for (AggregateData child : callees)
			{
				sb.append('\n');
				sb.append(childPrefix);
				child.toXML(sb);
			}
			sb.append("</aggregatedata>");
		}

		public Collection<DataCallProfileData> getDataCallProfileDataMap()
		{
			if (dataCallProfileDataMap != null) return dataCallProfileDataMap.values();
			else return null;
		}
	}

	private static final class AggregateDataComparator implements Comparator<AggregateData>
	{

		private final static Comparator<AggregateData> INSTANCE = new AggregateDataComparator();

		/**
		 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
		 */
		public int compare(AggregateData o1, AggregateData o2)
		{
			return (int)(o2.ownTime - o1.ownTime);
		}

	}

	private static class DataCallProfileDataAggregate extends DataCallProfileData
	{
		public DataCallProfileDataAggregate(String name, String datasource, String transaction_id, long startTime, long endTime, String query,
			String argumentString, int count)
		{
			super(name, datasource, transaction_id, startTime, endTime, query, argumentString, count);
		}
	}

	/**
	 * The ID of the view as specified by the extension.
	 */
	public static final String ID = "com.servoy.eclipse.profiler.views.ProfilerView";

	private TreeViewer methodCallViewer;

	private DrillDownAdapter drillDownAdapter;

	private Action clearData;

	private Action exportData;

	private Action toggleAggregateView;

	private Action toggleProfile;

	private Action openCallPostion;

	private Action doubleClickAction;

	private MethodCallContentProvider methodCallContentProvider;

	private DataCallContentProvider dataCallContentProvider;

	private TreeColumn argsColumn;

	private TableViewer sqlDataViewer;

	class MethodCallContentProvider implements IStructuredContentProvider, ITreeContentProvider, IProfileListener
	{

		private final List<ProfileData> invisibleRoot = new ArrayList<ProfileData>();

		private final List<AggregateData> aggregateData = new ArrayList<AggregateData>();

		private boolean aggregateView = false;

		public void inputChanged(Viewer v, Object oldInput, Object newInput)
		{
		}

		public void dispose()
		{
		}

		public Object[] getElements(Object parent)
		{
			if (parent.equals(getViewSite()))
			{
				if (aggregateView)
				{
					return aggregateData.toArray();
				}
				return invisibleRoot.toArray();
			}
			return getChildren(parent);
		}

		public Object getParent(Object child)
		{
			if (child instanceof ProfileData)
			{
				return ((ProfileData)child).getParent();
			}
			return null;
		}

		public Object[] getChildren(Object parent)
		{
			if (parent instanceof ProfileData)
			{
				return ((ProfileData)parent).getChildren();
			}
			if (parent instanceof AggregateData)
			{
				return ((AggregateData)parent).getChildren();
			}
			return new Object[0];
		}

		public boolean hasChildren(Object parent)
		{
			if (parent instanceof ProfileData) return ((ProfileData)parent).getChildren().length > 0;
			if (parent instanceof AggregateData) return ((AggregateData)parent).getChildren().length > 0;
			return false;
		}

		/**
		 * @see com.servoy.j2db.debug.IProfileListener#addProfileData(com.servoy.j2db.debug.ProfileData)
		 */
		public void addProfileData(ProfileData profileData)
		{
			invisibleRoot.add(0, profileData);

			if (invisibleRoot.size() > 400)
			{
				invisibleRoot.remove(invisibleRoot.size() - 1);
			}

			calculateAggregateData(profileData);
			Collections.sort(aggregateData, AggregateDataComparator.INSTANCE);

			Display.getDefault().asyncExec(new Runnable()
			{

				public void run()
				{
					methodCallViewer.refresh();
				}
			});

		}

		/**
		 * @param profileData
		 */
		private void calculateAggregateData(ProfileData profileData)
		{
			AggregateData ad = new AggregateData(profileData);
			int index = aggregateData.indexOf(ad);
			if (index != -1)
			{
				ad = aggregateData.get(index);
				ad.add(profileData);
			}
			else
			{
				aggregateData.add(ad);
			}
			ProfileData[] children = profileData.getChildren();
			for (ProfileData pd : children)
			{
				calculateAggregateData(pd);
			}
		}

		void toggleAggregateView()
		{
			if (aggregateView)
			{
				argsColumn.setText("Arguments");
			}
			else
			{
				argsColumn.setText("Count");

			}
			aggregateView = !aggregateView;
			Display.getDefault().asyncExec(new Runnable()
			{

				public void run()
				{
					methodCallViewer.refresh();
				}
			});
		}
	}

	class DataCallContentProvider implements IStructuredContentProvider, IProfileListener
	{

		private final List<AggregateData> aggregateData = new ArrayList<AggregateData>();

		private boolean aggregateView = false;

		public void addProfileData(ProfileData profileData)
		{
			AggregateData ad = new AggregateData(profileData);
			int index = aggregateData.indexOf(ad);
			if (index != -1)
			{
				ad = aggregateData.get(index);
				ad.add(profileData);
			}
			else
			{
				aggregateData.add(ad);
			}
			ProfileData[] children = profileData.getChildren();
			for (ProfileData pd : children)
			{
				addProfileData(pd);
			}
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#dispose()
		 */
		public void dispose()
		{
		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IContentProvider#inputChanged(org.eclipse.jface.viewers.Viewer, java.lang.Object, java.lang.Object)
		 */
		public void inputChanged(Viewer viewer, Object oldInput, Object newInput)
		{
			// TODO Auto-generated method stub

		}

		/*
		 * (non-Javadoc)
		 * 
		 * @see org.eclipse.jface.viewers.IStructuredContentProvider#getElements(java.lang.Object)
		 */
		public Object[] getElements(Object inputElement)
		{
			if (inputElement instanceof ProfileData)
			{
				return ((ProfileData)inputElement).getDataCallProfileDatas().toArray();
			}
			else if (inputElement instanceof AggregateData)
			{
				if (aggregateView)
				{
					AggregateData ad = (AggregateData)inputElement;
					int adIndex = aggregateData.indexOf(ad);
					if (adIndex >= 0)
					{
						Collection<DataCallProfileData> dataCallProfilecollection = aggregateData.get(adIndex).getDataCallProfileDataMap();
						if (dataCallProfilecollection != null)
						{
							DataCallProfileDataAggregate[] dataCallArray = new DataCallProfileDataAggregate[dataCallProfilecollection.size()];
							int index = 0;
							for (DataCallProfileData dataCallProfileData : dataCallProfilecollection)
							{
								dataCallArray[index] = new DataCallProfileDataAggregate(dataCallProfileData.getName(), dataCallProfileData.getDatasource(),
									dataCallProfileData.getTransactionId(), 0, dataCallProfileData.getTime(), dataCallProfileData.getQuery(),
									dataCallProfileData.getArgumentString(), dataCallProfileData.getCount());
								index++;
							}

							return dataCallArray;
						}
					}
				}
			}

			return new Object[0];
		}

		void toggleAggregateView()
		{
			if (aggregateView)
			{
				arguments.setText("Arguments");
			}
			else
			{
				arguments.setText("Count");

			}
			aggregateView = !aggregateView;
			Display.getDefault().asyncExec(new Runnable()
			{

				public void run()
				{
					sqlDataViewer.refresh();
				}
			});
		}

	}

	class MethodCallLabelProvider implements ITableLabelProvider
	{
		private final Image methodImage = Activator.getImageDescriptor("icons/form_method.gif").createImage();

		private final Image calcImage = Activator.getImageDescriptor("icons/columnaggr.gif").createImage();

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object,
		 *      int)
		 */
		public Image getColumnImage(Object element, int columnIndex)
		{
			boolean calc = false;
			if (element instanceof ProfileData)
			{
				calc = ((ProfileData)element).isCalculation();
			}
			switch (columnIndex)
			{
				case 0 :
					return calc ? calcImage : methodImage;
				case 1 :
					return null;
				case 2 :
					return null;
				case 3 :
					return null;
				case 4 :
					return null;
			}
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object,
		 *      int)
		 */
		@SuppressWarnings("nls")
		public String getColumnText(Object element, int columnIndex)
		{
			if (element instanceof ProfileData)
			{
				ProfileData pd = (ProfileData)element;
				if (pd.getMethodName().equals("<eval>") && pd.getSourceName().equals("internal_anon"))
				{
					pd = pd.getChildren()[0];
				}
				String sourceName;
				IFile file;
				switch (columnIndex)
				{
					case 0 :
						String lineStart = "";
						int[] lineNumbers = pd.getLineNumbers();
						if (lineNumbers != null && lineNumbers.length > 0)
						{
							lineStart = "#" + lineNumbers[0];
						}

						String printedMethodName = "";
						file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(pd.getSourceName()));
						if (file.getName().equals("globals.js"))
						{
							printedMethodName = "globals.";
							printedMethodName = printedMethodName + pd.getMethodName() + "[" + file.getProject().getName() + "]";
						}
						else
						{
							printedMethodName = printedMethodName + pd.getMethodName() + "[" + file.getName().replace(".js", "") + "]";
						}

						return pd.isInnerFunction() ? printedMethodName + " (innerfunction" + lineStart + ")" : printedMethodName;
					case 1 :
						return Long.toString(pd.getOwnTime());
					case 2 :
						return Long.toString(pd.getTime());
					case 3 :
						return pd.getArgs();
					case 4 :
					{
						sourceName = pd.getSourceName();
						file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(sourceName));
						return file.getProject().getName() + '/' + file.getProjectRelativePath().toPortableString();
					}
				}
			}
			else if (element instanceof AggregateData)
			{
				AggregateData pd = (AggregateData)element;
				switch (columnIndex)
				{
					case 0 :
						String printedMethodName = "";
						IFile file = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(pd.getSourceName()));
						if (file.getName().equals("globals.js"))
						{
							printedMethodName = "globals.";
							printedMethodName = printedMethodName + pd.getMethodName() + "[" + file.getProject().getName() + "]";
						}
						else
						{
							printedMethodName = printedMethodName + pd.getMethodName() + "[" + file.getName().replace(".js", "") + "]";
						}

						return pd.getInnerFunctionLineStart() == -1 ? printedMethodName : printedMethodName + "#" + pd.getInnerFunctionLineStart();
					case 1 :
						return Long.toString(pd.getOwnTime());
					case 2 :
						return Long.toString(pd.getTime());
					case 3 :
						return Integer.toString(pd.getCount());
					case 4 :
						return pd.getSourceName();
				}
			}
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener)
		{
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose()
		{
			methodImage.dispose();
			calcImage.dispose();
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
		 *      java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property)
		{
			return false;
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener)
		{
		}
	}

	class DataCallLabelProvider implements ITableLabelProvider
	{
		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnImage(java.lang.Object,
		 *      int)
		 */
		public Image getColumnImage(Object element, int columnIndex)
		{
			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.ITableLabelProvider#getColumnText(java.lang.Object,
		 *      int)
		 */
		@SuppressWarnings("nls")
		public String getColumnText(Object element, int columnIndex)
		{

			if (element instanceof DataCallProfileDataAggregate)
			{
				DataCallProfileDataAggregate pd = (DataCallProfileDataAggregate)element;
				switch (columnIndex)
				{
					case 0 :
						return pd.getName();
					case 1 :
						return Long.toString(pd.getTime());
					case 2 :
						return pd.getQuery();
					case 3 :
						return (new Integer(pd.getCount())).toString();
					case 4 :
						return pd.getDatasource();
					case 5 :
						return pd.getTransactionId();
				}
			}
			else if (element instanceof DataCallProfileData)
			{
				DataCallProfileData pd = (DataCallProfileData)element;
				switch (columnIndex)
				{
					case 0 :
						return pd.getName();
					case 1 :
						return Long.toString(pd.getTime());
					case 2 :
						return pd.getQuery();
					case 3 :
						return pd.getArgumentString();
					case 4 :
						return pd.getDatasource();
					case 5 :
						return pd.getTransactionId();
				}
			}

			return null;
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#addListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void addListener(ILabelProviderListener listener)
		{
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#dispose()
		 */
		public void dispose()
		{
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#isLabelProperty(java.lang.Object,
		 *      java.lang.String)
		 */
		public boolean isLabelProperty(Object element, String property)
		{
			return false;
		}

		/**
		 * @see org.eclipse.jface.viewers.IBaseLabelProvider#removeListener(org.eclipse.jface.viewers.ILabelProviderListener)
		 */
		public void removeListener(ILabelProviderListener listener)
		{
		}
	}

	class NameSorter extends ViewerSorter
	{
	}

	private TreeColumn methodNameColumn;
	private TreeColumn ownTimeColumn;
	private TreeColumn timeColumn;
	private TreeColumn fileColumn;
	private TableColumn name;
	private TableColumn time;
	private TableColumn query;
	private TableColumn arguments;
	private TableColumn datasource;
	private TableColumn transaction;
	private Tree tree;

	/**
	 * The constructor.
	 */
	public ProfilerView()
	{
	}

	SashForm sashForm;

	private IMemento memento;

	/**
	 * This is a callback that will allow us to create the viewer and initialize
	 * it.
	 */
	@Override
	public void createPartControl(Composite parent)
	{
		sashForm = new SashForm(parent, SWT.NONE);
		methodCallViewer = new TreeViewer(sashForm, SWT.MULTI | SWT.H_SCROLL | SWT.V_SCROLL);
		drillDownAdapter = new DrillDownAdapter(methodCallViewer);
		makeActions();
		hookContextMenu();
		hookDoubleClickAction();
		contributeToActionBars();


		int methodNameColumnWidth = getSavedState(METHOD_NAME_COLUMN_WIDTH_SETTING, METHOD_NAME_COLUMN_WIDTH_DEFAULT);
		int ownTimeColumnWidth = getSavedState(OWN_TIME_COLUMN_WIDTH_SETTING, OWN_TIME_COLUMN_WIDTH_DEFAULT);
		int timeColumnWidth = getSavedState(TIME_COLUMN_WIDTH_SETTING, TIME_COLUMN_WIDTH_DEFAULT);
		int fileColumnWidth = getSavedState(FILE_COLUMN_WIDTH_SETTING, FILE_COLUMN_WIDTH_DEFAULT);
		int argsColumnWidth = getSavedState(ARGS_COLUMN_WIDTH_SETTING, ARGS_COLUMN_WIDTH_DEFAULT);
		int nameTableColumnWidth = getSavedState(NAME_TABLE_COLUMN_WIDTH_SETTING, NAME_TABLE_COLUMN_WIDTH_DEFAULT);
		int timeTableColumnWidth = getSavedState(TIME_TABLE_COLUMN_WIDTH_SETTING, TIME_TABLE_COLUMN_WIDTH_DEFAULT);
		int queryTableColumnWidth = getSavedState(QUERY_TABLE_COLUMN_WIDTH_SETTING, QUERY_TABLE_COLUMN_WIDTH_DEFAULT);
		int argumentsTableColumnWidth = getSavedState(ARGUMENTS_TABLE_COLUMN_WIDTH_SETTING, ARGUMENTS_TABLE_COLUMN_WIDTH_DEFAULT);
		int datasourceTableColumnWidth = getSavedState(DATASOURCE_TABLE_COLUMN_WIDTH_SETTING, DATASOURCE_TABLE_COLUMN_WIDTH_DEFAULT);
		int transactionTableColumnWidth = getSavedState(TRANSACTION_TABLE_COLUMN_WIDTH_SETTING, TRANSACTION_TABLE_COLUMN_WIDTH_DEFAULT);
		int[] sashFormWeights = new int[] { getSavedState(TREE_WIDTH_SETTING, TREE_WIDTH_DEFAULT), getSavedState(TABLE_WIDTH_SETTING, TABLE_WIDTH_DEFAULT) };

		methodCallContentProvider = new MethodCallContentProvider();

		tree = methodCallViewer.getTree();
		tree.setHeaderVisible(true);
		tree.setLinesVisible(true);

		methodNameColumn = new TreeColumn(tree, SWT.NONE);
		methodNameColumn.setText("Method Name");
		methodNameColumn.setResizable(true);
		methodNameColumn.setWidth(methodNameColumnWidth);

		ownTimeColumn = new TreeColumn(tree, SWT.NONE);
		ownTimeColumn.setText("Own Time (ms)");
		ownTimeColumn.setResizable(true);
		ownTimeColumn.setWidth(ownTimeColumnWidth);

		timeColumn = new TreeColumn(tree, SWT.NONE);
		timeColumn.setText("Time (ms)");
		timeColumn.setResizable(true);
		timeColumn.setWidth(timeColumnWidth);

		argsColumn = new TreeColumn(tree, SWT.NONE);
		argsColumn.setText("Arguments");
		argsColumn.setResizable(true);
		argsColumn.setWidth(argsColumnWidth);

		fileColumn = new TreeColumn(tree, SWT.NONE);
		fileColumn.setText("Source File");
		fileColumn.setResizable(true);
		fileColumn.setWidth(fileColumnWidth);

		methodCallViewer.setContentProvider(methodCallContentProvider);
		methodCallViewer.setLabelProvider(new MethodCallLabelProvider());
		// viewer.setSorter(new NameSorter());
		methodCallViewer.setInput(getViewSite());

		sqlDataViewer = new TableViewer(sashForm);
		Table table = sqlDataViewer.getTable();
		table.setHeaderVisible(true);
		table.setLinesVisible(true);

		name = new TableColumn(table, SWT.NONE);
		name.setText("Action");
		name.setWidth(nameTableColumnWidth);
		name.setResizable(true);

		time = new TableColumn(table, SWT.NONE);
		time.setText("Time (ms)");
		time.setWidth(timeTableColumnWidth);
		time.setResizable(true);

		query = new TableColumn(table, SWT.NONE);
		query.setText("Query/Action");
		query.setWidth(queryTableColumnWidth);
		query.setResizable(true);

		arguments = new TableColumn(table, SWT.NONE);
		arguments.setText("Arguments");
		arguments.setWidth(argumentsTableColumnWidth);
		arguments.setResizable(true);

		datasource = new TableColumn(table, SWT.NONE);
		datasource.setText("Datasource");
		datasource.setWidth(datasourceTableColumnWidth);
		datasource.setResizable(true);

		transaction = new TableColumn(table, SWT.NONE);
		transaction.setText("TransactionId");
		transaction.setWidth(transactionTableColumnWidth);
		transaction.setResizable(true);


		dataCallContentProvider = new DataCallContentProvider();
		sqlDataViewer.setLabelProvider(new DataCallLabelProvider());
		sqlDataViewer.setContentProvider(dataCallContentProvider);
		sqlDataViewer.setInput(getViewSite());

		methodCallViewer.addSelectionChangedListener(new ISelectionChangedListener()
		{
			public void selectionChanged(SelectionChangedEvent event)
			{
				IStructuredSelection selection = (IStructuredSelection)event.getSelection();
				if (selection.getFirstElement() != null)
				{
					sqlDataViewer.setInput(selection.getFirstElement());
				}
				else
				{
					sqlDataViewer.setInput(getViewSite());
				}
			}
		});

		sashForm.setWeights(sashFormWeights);
	}

	/**
	 * @param methodNameColumnWidthSetting
	 * @param methodNameColumnWidthDefault
	 * @return
	 */
	private int getSavedState(String settingString, int defaultValue)
	{
		if (memento != null)
		{
			Integer integer = memento.getInteger(settingString);
			if (integer != null) return integer.intValue();
		}
		return defaultValue;
	}

	/**
	 * @see org.eclipse.ui.part.WorkbenchPart#dispose()
	 */
	@Override
	public void dispose()
	{
		super.dispose();

		RemoteDebugScriptEngine.deregisterProfileListener(methodCallContentProvider);
		RemoteDebugScriptEngine.deregisterProfileListener(dataCallContentProvider);
	}

	private void hookContextMenu()
	{
		MenuManager menuMgr = new MenuManager("#PopupMenu");
		menuMgr.setRemoveAllWhenShown(true);
		menuMgr.addMenuListener(new IMenuListener()
		{
			public void menuAboutToShow(IMenuManager manager)
			{
				ProfilerView.this.fillContextMenu(manager);
			}
		});
		Menu menu = menuMgr.createContextMenu(methodCallViewer.getControl());
		methodCallViewer.getControl().setMenu(menu);
		getSite().registerContextMenu(menuMgr, methodCallViewer);
	}

	private void contributeToActionBars()
	{
		IActionBars bars = getViewSite().getActionBars();
		fillLocalPullDown(bars.getMenuManager());
		fillLocalToolBar(bars.getToolBarManager());
	}

	private void fillLocalPullDown(IMenuManager manager)
	{
		manager.add(toggleProfile);
		manager.add(new Separator());
		manager.add(toggleAggregateView);
		manager.add(new Separator());
		manager.add(clearData);
		manager.add(new Separator());
		manager.add(exportData);
	}

	private void fillContextMenu(IMenuManager manager)
	{
		ISelection selection = methodCallViewer.getSelection();
		Object obj = ((IStructuredSelection)selection).getFirstElement();
		if (obj instanceof ProfileData && ((ProfileData)obj).getParentSourceCall() != null)
		{
			manager.add(openCallPostion);
		}
		manager.add(new Separator());
		manager.add(toggleAggregateView);
		manager.add(clearData);
		manager.add(exportData);
		manager.add(new Separator());
		manager.add(toggleProfile);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
		// Other plug-ins can contribute there actions here
		manager.add(new Separator(IWorkbenchActionConstants.MB_ADDITIONS));
	}

	private void fillLocalToolBar(IToolBarManager manager)
	{
		manager.add(toggleProfile);
		manager.add(toggleAggregateView);
		manager.add(clearData);
		manager.add(exportData);
		manager.add(new Separator());
		drillDownAdapter.addNavigationActions(manager);
	}

	private void makeActions()
	{
		clearData = new Action()
		{
			@Override
			public void run()
			{
				methodCallContentProvider.invisibleRoot.clear();
				if (methodCallContentProvider.aggregateView)
				{
					methodCallContentProvider.aggregateData.clear();
					dataCallContentProvider.aggregateData.clear();
				}
				methodCallViewer.refresh();
			}
		};
		clearData.setText("Clear data");
		clearData.setToolTipText("Clear data");
		clearData.setImageDescriptor(Activator.getImageDescriptor("icons/clear.gif"));

		exportData = new Action()
		{
			@Override
			public void run()
			{
				exportData();
			}
		};
		exportData.setText("Export data");
		exportData.setToolTipText("Export data to clipboard");
		exportData.setImageDescriptor(Activator.getImageDescriptor("icons/exportdata.gif"));

		toggleAggregateView = new Action("Aggregate View", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				methodCallContentProvider.toggleAggregateView();
				dataCallContentProvider.toggleAggregateView();
			}
		};
		toggleAggregateView.setText("Aggregate View");
		toggleAggregateView.setToolTipText("Aggregate View");
		toggleAggregateView.setImageDescriptor(Activator.getImageDescriptor("icons/aggregates.gif"));

		toggleProfile = new Action("Start profiling", IAction.AS_CHECK_BOX)
		{
			@Override
			public void run()
			{
				if (toggleProfile.isChecked())
				{
					RemoteDebugScriptEngine.registerProfileListener(methodCallContentProvider);
					RemoteDebugScriptEngine.registerProfileListener(dataCallContentProvider);
					toggleProfile.setText("Stop profiling");
					toggleProfile.setToolTipText("Stop profiling");
				}
				else
				{
					toggleProfile.setText("Start profiling");
					toggleProfile.setToolTipText("Start profiling");
					RemoteDebugScriptEngine.deregisterProfileListener(methodCallContentProvider);
					RemoteDebugScriptEngine.deregisterProfileListener(dataCallContentProvider);
				}
			}
		};
		toggleProfile.setText("Start profiling");
		toggleProfile.setToolTipText("Start profiling");
		toggleProfile.setImageDescriptor(Activator.getImageDescriptor("icons/resume_co.gif"));

		openCallPostion = new Action()
		{
			@Override
			public void run()
			{
				ISelection selection = methodCallViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				if (obj instanceof ProfileData && ((ProfileData)obj).getParentSourceCall() != null)
				{
					String parentSource = ((ProfileData)obj).getParentSourceCall();

					int lineNumberIndex = parentSource.indexOf('#');
					if (lineNumberIndex > 0)
					{
						int lineNumber = Integer.parseInt(parentSource.substring(lineNumberIndex + 1));
						String filename = parentSource.substring(0, lineNumberIndex);
						IPath path = Path.fromOSString(filename);
						IFile file = ServoyModel.getWorkspace().getRoot().getFileForLocation(path);
						try
						{
							IEditorPart editorPart = EditorUtility.openInEditor(file, true);
							if (editorPart instanceof ITextEditor && lineNumber > 0)
							{
								ITextEditor textEditor = (ITextEditor)editorPart;
								IDocumentProvider provider = textEditor.getDocumentProvider();
								IEditorInput input = editorPart.getEditorInput();
								provider.connect(input);
								IDocument document = provider.getDocument(input);
								try
								{
									IRegion line = document.getLineInformation(lineNumber - 1);
									textEditor.selectAndReveal(line.getOffset(), line.getLength());
									return;
								}
								catch (BadLocationException e)
								{

								}
								finally
								{
									provider.disconnect(input);
								}
							}
						}
						catch (Exception ex)
						{
							ServoyLog.logError(ex);
						}

					}
				}
			}
		};
		openCallPostion.setText("Open call position");
		openCallPostion.setToolTipText("Open call position");
		// openCallPostion.setImageDescriptor(Activator.getImageDescriptor("icons/resume_co.gif"));

		doubleClickAction = new Action()
		{
			@Override
			public void run()
			{
				ISelection selection = methodCallViewer.getSelection();
				Object obj = ((IStructuredSelection)selection).getFirstElement();
				String sourceName = null;
				String methodName = null;
				int lineNumber = -1;
				if (obj instanceof ProfileData)
				{
					ProfileData pd = (ProfileData)obj;
					sourceName = pd.getSourceName();
					methodName = pd.getMethodName();
					if (pd.isInnerFunction() && pd.getLineNumbers() != null && pd.getLineNumbers().length > 0)
					{
						lineNumber = pd.getLineNumbers()[0];
					}
				}
				else if (obj instanceof AggregateData)
				{
					sourceName = ((AggregateData)obj).getSourceName();
					methodName = ((AggregateData)obj).getMethodName();
					lineNumber = ((AggregateData)obj).getInnerFunctionLineStart();
				}
				IFile f = ResourcesPlugin.getWorkspace().getRoot().getFileForLocation(new Path(sourceName));
				if (f != null)
				{
					ISourceModule sourceModule = DLTKUIPlugin.getEditorInputModelElement(FileEditorInputFactory.createFileEditorInput(f));
					IMethod method = sourceModule.getMethod(methodName);
					try
					{
						IEditorPart openInEditor = DLTKUIPlugin.openInEditor(method);
						if (openInEditor instanceof ITextEditor && lineNumber != -1)
						{
							ITextEditor editor = (ITextEditor)openInEditor;

							IDocumentProvider provider = editor.getDocumentProvider();
							IDocument document = provider.getDocument(editor.getEditorInput());
							int start = document.getLineOffset(lineNumber);
							editor.selectAndReveal(start, 0);
						}
					}
					catch (Exception ex)
					{
						ServoyLog.logError(ex);
					}
				}
			}
		};
	}

	/**
	 * 
	 */
	protected void exportData()
	{
		final boolean exportAggregate = methodCallContentProvider.aggregateView;
		Job job = new Job("generating export")
		{
			@Override
			protected IStatus run(IProgressMonitor monitor)
			{
				final StringBuilder sb = new StringBuilder(200);
				if (exportAggregate)
				{
					List<AggregateData> aggregateData = methodCallContentProvider.aggregateData;
					for (AggregateData ad : aggregateData)
					{
						ad.toXML(sb);
					}
				}
				else
				{
					List<ProfileData> invisibleRoot = methodCallContentProvider.invisibleRoot;
					for (ProfileData profileData : invisibleRoot)
					{
						profileData.toXML(sb);
					}
				}
				if (sb.length() > 0)
				{
					getSite().getShell().getDisplay().asyncExec(new Runnable()
					{
						public void run()
						{
							Clipboard clipboard = new Clipboard(getSite().getShell().getDisplay());
							TextTransfer textTransfer = TextTransfer.getInstance();
							clipboard.setContents(new Object[] { sb.toString() }, new Transfer[] { textTransfer });
							clipboard.dispose();
						}
					});
				}
				return Status.OK_STATUS;
			}
		};
		job.schedule();
	}

	private void hookDoubleClickAction()
	{
		methodCallViewer.addDoubleClickListener(new IDoubleClickListener()
		{
			public void doubleClick(DoubleClickEvent event)
			{
				doubleClickAction.run();
			}
		});
	}

	/**
	 * Passing the focus request to the viewer's control.
	 */
	@Override
	public void setFocus()
	{
		methodCallViewer.getControl().setFocus();
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#init(org.eclipse.ui.IViewSite, org.eclipse.ui.IMemento)
	 */
	@Override
	public void init(IViewSite site, IMemento mem) throws PartInitException
	{
		if (mem == null)
		{
			String persistedMemento = Activator.getDefault().getDialogSettings().get(PROFILER_VIEW);
			if (persistedMemento != null)
			{
				try
				{
					this.memento = XMLMemento.createReadRoot(new StringReader(persistedMemento));
				}
				catch (WorkbenchException e)
				{
					// don't do anything. Simply don't restore the settings
				}
			}
		}
		else
		{
			this.memento = mem;
		}

		super.init(site, mem);

		IPageService service = (IPageService)getSite().getService(IPageService.class);
		service.addPerspectiveListener(new PerspectiveAdapter()
		{
			@Override
			public void perspectiveChanged(IWorkbenchPage page, IPerspectiveDescriptor perspective, IWorkbenchPartReference partRef, String changeId)
			{
				if (IWorkbenchPage.CHANGE_VIEW_HIDE.equals(changeId) && partRef.getPart(false) == ProfilerView.this)
				{
					XMLMemento xmlMemento = XMLMemento.createWriteRoot(PROFILER_VIEW);
					saveState(xmlMemento);
					StringWriter writer = new StringWriter();
					try
					{
						xmlMemento.save(writer);
						Activator.getDefault().getDialogSettings().put(PROFILER_VIEW, writer.getBuffer().toString());
					}
					catch (IOException e)
					{
						// don't do anything. Simply don't store the settings
					}
				}
			}
		});
	}

	/*
	 * (non-Javadoc)
	 * 
	 * @see org.eclipse.ui.part.ViewPart#saveState(org.eclipse.ui.IMemento)
	 */
	@Override
	public void saveState(IMemento mem)
	{
		super.saveState(mem);

		mem.putInteger(METHOD_NAME_COLUMN_WIDTH_SETTING, methodNameColumn.getWidth());
		mem.putInteger(OWN_TIME_COLUMN_WIDTH_SETTING, ownTimeColumn.getWidth());
		mem.putInteger(TIME_COLUMN_WIDTH_SETTING, timeColumn.getWidth());
		mem.putInteger(FILE_COLUMN_WIDTH_SETTING, fileColumn.getWidth());
		mem.putInteger(ARGS_COLUMN_WIDTH_SETTING, argsColumn.getWidth());
		mem.putInteger(NAME_TABLE_COLUMN_WIDTH_SETTING, name.getWidth());
		mem.putInteger(TIME_TABLE_COLUMN_WIDTH_SETTING, time.getWidth());
		mem.putInteger(QUERY_TABLE_COLUMN_WIDTH_SETTING, query.getWidth());
		mem.putInteger(ARGUMENTS_TABLE_COLUMN_WIDTH_SETTING, arguments.getWidth());
		mem.putInteger(DATASOURCE_TABLE_COLUMN_WIDTH_SETTING, datasource.getWidth());
		mem.putInteger(TRANSACTION_TABLE_COLUMN_WIDTH_SETTING, transaction.getWidth());
		mem.putInteger(TREE_WIDTH_SETTING, sashForm.getWeights()[0]);
		mem.putInteger(TABLE_WIDTH_SETTING, sashForm.getWeights()[1]);
	}
}