package com.servoy.eclipse.aibridge.editors;

import java.util.UUID;

import org.eclipse.core.resources.IFile;
import org.eclipse.core.runtime.Path;
import org.eclipse.jface.resource.ImageDescriptor;
import org.eclipse.ui.IEditorInput;
import org.eclipse.ui.IPersistableElement;

import com.servoy.eclipse.aibridge.dto.Completion;
import com.servoy.eclipse.core.ServoyModel;
import com.servoy.eclipse.core.ServoyModelManager;
import com.servoy.eclipse.model.repository.SolutionSerializer;
import com.servoy.j2db.FlattenedSolution;
import com.servoy.j2db.persistence.Form;
import com.servoy.j2db.persistence.IPersist;
import com.servoy.j2db.persistence.IRepository;
import com.servoy.j2db.persistence.Solution;

public class DualEditorInput implements IEditorInput
{

	private IPersist persist;
	private String scopeName;
	private IFile inputFile;
	private final Completion completion;
	private final String leftTitle;


	public DualEditorInput(Completion completion)
	{
		this.completion = completion;
		String[] pathParts = extractPathParts(completion.getSourcePath());
		Solution targetSolution = locateTargetSolution(pathParts[0]);
		assignPersistAndScopeName(pathParts, targetSolution);
		setInputFile();
		leftTitle = pathParts[pathParts.length - 1];
	}

	private String[] extractPathParts(String selectionPath)
	{
		return selectionPath.substring(1).split("/");
	}

	private Solution locateTargetSolution(String solutionName)
	{
		Solution activeSolution = getActiveSolution();
		if (isActiveSolution(solutionName, activeSolution))
		{
			return activeSolution;
		}
		return findTargetSolutionInModules(solutionName);
	}

	private Solution getActiveSolution()
	{
		return ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject().getSolution();
	}

	private boolean isActiveSolution(String solutionName, Solution activeSolution)
	{
		return activeSolution.getName().equals(solutionName);
	}

	private Solution findTargetSolutionInModules(String solutionName)
	{
		FlattenedSolution flSol = ServoyModelManager.getServoyModelManager().getServoyModel().getFlattenedSolution();
		for (Solution module : flSol.getModules())
		{
			if (module.getName().equals(solutionName))
			{
				return module;
			}
		}
		return null; // Or handle the case when no solution is found
	}

	private void assignPersistAndScopeName(String[] parts, Solution targetSolution)
	{
		scopeName = parts[parts.length - 1];
		boolean isForm = (parts.length > 2 && parts[1].equals("forms"));
		if (scopeName.endsWith(".js"))
		{
			scopeName = scopeName.substring(0, scopeName.length() - 3);
		}
		if (isForm)
		{
			persist = targetSolution.getForm(scopeName);
			scopeName = null;
		}
		else
		{
			persist = targetSolution;
		}
	}

	private void setInputFile()
	{
		String scriptPath = getScriptPath();
		if (scriptPath != null)
		{
			inputFile = ServoyModel.getWorkspace().getRoot().getFile(new Path(scriptPath));
		}
	}

	private String getScriptPath()
	{
		if (persist instanceof Solution && scopeName != null)
		{
			return SolutionSerializer.getRelativePath(persist, false) + scopeName + SolutionSerializer.JS_FILE_EXTENSION;
		}
		else if (isValidForm())
		{
			return SolutionSerializer.getScriptPath(persist, false);
		}
		return null;
	}

	private boolean isValidForm()
	{
		Form parentForm = (Form)persist.getAncestor(IRepository.FORMS);
		return parentForm == null || !parentForm.isFormComponent();
	}

	@Override
	public boolean exists()
	{
		return true;
	}

	@Override
	public ImageDescriptor getImageDescriptor()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return "JS Content";
	}

	@Override
	public IPersistableElement getPersistable()
	{
		return null;
	}

	@Override
	public String getToolTipText()
	{
		return "JavaScript Content";
	}

	@SuppressWarnings("rawtypes")
	@Override
	public Object getAdapter(Class adapter)
	{
		return null;
	}

	public IFile getInputFile()
	{
		return inputFile;
	}

	public String getContent()
	{
		return completion.getSelection();
	}

	public String getHtmlContent()
	{
		if (completion.getResponse() != null)
		{
			return completion.getFullCompletion().getResponse().getResponseMessage();
		}
		return getNoContentHtml(); //just in case
	}

	public static String getNoContentHtml()
	{
		return "<html><body><br><br><br><h1 style=\"text-align:center;\">. . .</h1></body></html>";
	}

	public UUID getId()
	{
		return completion.getId();
	}

	public String getLeftTitle()
	{
		return completion.getId().toString().replaceAll("(.{2}).{6}-(.{2}).{2}-(.{2}).{2}-(.{2}).*", "$1$2$3$4") + ": " + leftTitle;
	}

	public String getRightTitle()
	{
		return completion.getId().toString().replaceAll("(.{2}).{6}-(.{2}).{2}-(.{2}).{2}-(.{2}).*", "$1$2$3$4") + ": " + completion.getCmdName();
	}

	public String getSelectionContent()
	{
		return completion.getSelection();
	}
}
