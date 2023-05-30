package com.servoy.eclipse.cloud;

import java.io.IOException;

import org.eclipse.core.commands.AbstractHandler;
import org.eclipse.core.commands.ExecutionEvent;
import org.eclipse.core.commands.ExecutionException;
import org.eclipse.core.commands.HandlerEvent;
import org.eclipse.core.resources.IResourceChangeEvent;
import org.eclipse.core.resources.IResourceChangeListener;
import org.eclipse.core.resources.IWorkspace;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.IPath;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.jface.dialogs.InputDialog;
import org.eclipse.jgit.api.CommitCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.errors.NoWorkTreeException;
import org.eclipse.jgit.lib.Repository;
import org.eclipse.jgit.lib.RepositoryBuilder;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Shell;
import com.servoy.eclipse.model.util.ServoyLog;

public class PublishHandler extends AbstractHandler implements IResourceChangeListener{

    private Git git = null;
    private IWorkspace workspace = null;
    
    public PublishHandler() {
    	try {
    		workspace = ResourcesPlugin.getWorkspace();
    		workspace.addResourceChangeListener(this);
    		IPath workspaceRoot = workspace.getRoot().getLocation();
    		Repository repository = new  RepositoryBuilder().setWorkTree(workspaceRoot.toFile()).build();
    		if (repository.getDirectory().exists())
    			git = new Git(repository);
    		fireHandlerChanged(new HandlerEvent(this, true, false));//force eclipse to call isEnabled() method
    	} catch (IOException | NoWorkTreeException e) {
    		ServoyLog.logError(e);
    	}
    }

    @Override
    public Object execute(ExecutionEvent event) throws ExecutionException {
    	String commitMessage = askForCommitMessage();
    	if (commitMessage != null) {
    		Job.create("Publish to ServoyCloud", (monitor) -> {
        		try {
        			monitor.beginTask("Pull from repository", 100);
                    git.pull().call(); //pull from remote
       		        commitChanges(commitMessage);
       		        monitor.subTask(commitMessage);
       		        git.push().call(); //push to remote    
        			monitor.done(); 
        			fireHandlerChanged(new HandlerEvent(this, true, false));
                } catch (GitAPIException e) {
                    ServoyLog.logError(e);
                }
         	}).schedule();
    		
    		
    	}
        return null;
    }

    private String askForCommitMessage() {
    	Shell shell = Display.getCurrent().getActiveShell();
    	InputDialog dialog = new InputDialog(shell, "Commit Changes", "Enter a commit message:", "", null);
        int result = dialog.open();
        if (result == InputDialog.OK) {
            return dialog.getValue();
        } else {
            return null;
        }
    }

    private void commitChanges(String commitMessage) throws GitAPIException {
        git.add().addFilepattern(".").call();
        CommitCommand commitCmd = git.commit();
        commitCmd.setMessage(commitMessage).call();
    }
    
    @Override
    public boolean isEnabled() {
    	return (git != null) && !isCleanGitRepo(git);
    }
    
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {//save or save all buttons were pressed
    		fireHandlerChanged(new HandlerEvent(this, true, false));
        }
    }
    
    private boolean isCleanGitRepo(Git gitRepo) {
    	if (gitRepo != null) {
    		try {
				return gitRepo.status().call().isClean();
			} catch (NoWorkTreeException | GitAPIException e) {
				ServoyLog.logError(e);
			}
    	} 
    	return true;
    }
}
