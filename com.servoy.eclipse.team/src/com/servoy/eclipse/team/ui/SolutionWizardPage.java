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
package com.servoy.eclipse.team.ui;

import java.util.Date;

import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.IOpenListener;
import org.eclipse.jface.viewers.ISelectionChangedListener;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.OpenEvent;
import org.eclipse.jface.viewers.SelectionChangedEvent;
import org.eclipse.jface.wizard.WizardPage;
import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Image;
import org.eclipse.swt.widgets.Button;
import org.eclipse.swt.widgets.Composite;

import com.servoy.eclipse.ui.Activator;
import com.servoy.eclipse.ui.dialogs.FilteredTreeViewer;
import com.servoy.eclipse.ui.dialogs.LeafnodesSelectionFilter;
import com.servoy.eclipse.ui.dialogs.TreePatternFilter;
import com.servoy.j2db.persistence.RootObjectMetaData;
import com.servoy.j2db.util.Pair;
import com.servoy.j2db.util.Utils;


public class SolutionWizardPage extends WizardPage
{
	private FilteredTreeViewer treeViewer;
	private Button chActivateSolution;
	private Button chCheckoutModules;
	private boolean canFinish;
	public static final String NAME = "SolutionWizardPage";

	public static final Image SOLUTION = Activator.getDefault().loadImageFromBundle("solution.gif"); //$NON-NLS-1$
	public static final Image ACTIVE_RELEASE = Activator.getDefault().loadImageFromBundle("active_release.gif"); //$NON-NLS-1$
	public static final Image RELEASE = Activator.getDefault().loadImageFromBundle("release.gif"); //$NON-NLS-1$

	public SolutionWizardPage(String title, String description, ImageDescriptor titleImage)
	{
		super(SolutionWizardPage.NAME, title, titleImage);
		setDescription(description);
		setTitle(title);
	}

	public void createControl(Composite parent)
	{
		if (treeViewer == null)
		{
			Composite container = Util.createComposite(parent, 1);
			setControl(container);

			SolutionContentProvider contentProvider = new SolutionContentProvider();
			treeViewer = new FilteredTreeViewer(container, true, false, contentProvider, new SolutionLabelProvider(), null, SWT.SINGLE, new TreePatternFilter(
				TreePatternFilter.FILTER_PARENTS), new LeafnodesSelectionFilter(contentProvider));
			treeViewer.addSelectionChangedListener(new ISelectionChangedListener()
			{
				public void selectionChanged(SelectionChangedEvent event)
				{
					setPageComplete(((IStructuredSelection)treeViewer.getSelection()).getFirstElement() instanceof Pair);
					canFinish = isPageComplete();
					SolutionWizardPage.this.getContainer().updateButtons();
				}
			});
			treeViewer.addOpenListener(new IOpenListener()
			{
				public void open(OpenEvent event)
				{
					SolutionWizardPage.this.getWizard().performFinish();
					SolutionWizardPage.this.getContainer().getShell().close();
				}
			});


			chActivateSolution = Util.createCheckBox(container, "Activate solution");
			chActivateSolution.setSelection(true);
			chCheckoutModules = Util.createCheckBox(container, "Checkout solution modules");
			chCheckoutModules.setSelection(true);
		}
	}


	private static class SolutionLabelProvider extends LabelProvider
	{
		@Override
		public Image getImage(Object element)
		{
			if (element instanceof NewSolutionWizard.SolutionInfo)
			{
				return SOLUTION;
			}
			if (element instanceof Pair)
			{
				NewSolutionWizard.SolutionInfo solutionInfo = (NewSolutionWizard.SolutionInfo)((Pair)element).getLeft();
				String release = ((Pair)element).getRight().toString();

				return solutionInfo.getMetaData().getActiveRelease() == Integer.valueOf(release).intValue() ? ACTIVE_RELEASE : RELEASE;
			}
			return super.getImage(element);
		}

		@Override
		public String getText(Object element)
		{
			if (element instanceof NewSolutionWizard.SolutionInfo)
			{
				return ((NewSolutionWizard.SolutionInfo)element).getMetaData().getName();
			}
			if (element instanceof Pair)
			{
				NewSolutionWizard.SolutionInfo solutionInfo = (NewSolutionWizard.SolutionInfo)((Pair)element).getLeft();
				String release = ((Pair)element).getRight().toString();

				if (release.equals("-1"))
				{
					return "HEAD";
				}
				else
				{
					Date releaseDate = solutionInfo.getReleaseDates()[Integer.valueOf(release).intValue() - 1];

					if (releaseDate != null)
					{
						return release + " - (" + Utils.formatTime(releaseDate.getTime(), "dd-MMM-yyyy") + ")";
					}
					return release;
				}
			}
			return super.getText(element);
		}
	}

	private static class SolutionContentProvider extends ArrayContentProvider implements ITreeContentProvider
	{
		private static final Object[] EMPTY_LIST = new Object[0];

		public Object[] getChildren(Object parentElement)
		{
			if (parentElement instanceof NewSolutionWizard.SolutionInfo)
			{
				RootObjectMetaData data = ((NewSolutionWizard.SolutionInfo)parentElement).getMetaData();

				int latestRelease = data.getLatestRelease();
				Object[] releases = new Object[latestRelease + 1];
				int idx = 1;
				for (int i = latestRelease; i > 0; i--)
				{
					releases[idx++] = new Pair(parentElement, new Integer(i));
				}

				releases[0] = new Pair(parentElement, new Integer(-1));

				return releases;
			}
			return EMPTY_LIST;
		}

		public Object getParent(Object element)
		{
			if (element instanceof Pair)
			{
				return ((Pair)element).getLeft();
			}

			return null;
		}

		public boolean hasChildren(Object element)
		{
			return (element instanceof NewSolutionWizard.SolutionInfo);
		}
	}

	public RootObjectMetaData getSelectedRootObjectMetaData()
	{
		Object selected = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
		if (selected instanceof Pair)
		{
			return ((NewSolutionWizard.SolutionInfo)((Pair)selected).getLeft()).getMetaData();
		}

		return null;
	}

	public String getSelectedSolution()
	{
		Object selected = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
		if (selected instanceof Pair)
		{
			return ((NewSolutionWizard.SolutionInfo)((Pair)selected).getLeft()).getMetaData().getName();
		}
		return null;
	}

	public int getSelectedVersion()
	{
		Object selected = ((IStructuredSelection)treeViewer.getSelection()).getFirstElement();
		if (selected instanceof Pair)
		{
			return ((Integer)((Pair)selected).getRight()).intValue();
		}
		return 1;
	}

	public void setRepositorySolutionInfos(NewSolutionWizard.SolutionInfo[] solutionInfos)
	{
		treeViewer.setInput(solutionInfos);
	}

	public boolean canFinish()
	{
		return canFinish;
	}

	public boolean shouldCheckoutModules()
	{
		return chCheckoutModules.getSelection();
	}

	public boolean shouldActivateSolution()
	{
		return chActivateSolution.getSelection();
	}
}
