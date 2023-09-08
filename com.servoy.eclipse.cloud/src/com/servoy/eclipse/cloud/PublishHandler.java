package com.servoy.eclipse.cloud;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;

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

import com.servoy.eclipse.core.util.UIUtils;
import com.servoy.eclipse.model.util.ServoyLog;

public class PublishHandler extends AbstractHandler implements IResourceChangeListener{

	private static final AtomicBoolean isCleanGit = new AtomicBoolean(true);
    private boolean isGitWs = false;
    private IWorkspace workspace = null;
    Repository repository = null;
    
    public PublishHandler() {
    	try {
    		workspace = ResourcesPlugin.getWorkspace();
    		workspace.addResourceChangeListener(this);
    		IPath workspaceRoot = workspace.getRoot().getLocation();
    		repository = new  RepositoryBuilder().setWorkTree(workspaceRoot.toFile()).build(); 
    		isGitWs = isGitWorkspace();
    		isCleanGit.set(isCleanGitRepo()); //detect changes from eventually outside apps
    		fireHandlerChanged(new HandlerEvent(this, true, false));//force eclipse to call isEnabled() method
    	} catch (IOException | NoWorkTreeException e) {
    		ServoyLog.logError(e);
    	}
    }
    
    @Override
	public Object execute(ExecutionEvent event) throws ExecutionException {
		if (isGitWs) {
			String commitMessage = askForCommitMessage();
        	if (commitMessage != null) {
        		Job.create("Publish to ServoyCloud", (monitor) -> {
            		try (Git git = new Git(repository);) {
            			monitor.beginTask("Pull from repository", 100);
                        git.pull().call(); //pull from remote
           		        commitChanges(git, commitMessage);
           		        monitor.subTask(commitMessage);
           		        git.push().call(); //push to remote   
           		        isCleanGit.set(isCleanGitRepo());
            			fireHandlerChanged(new HandlerEvent(this, true, false));
            			monitor.done(); 
                    } catch (GitAPIException e) {
                        ServoyLog.logError(e);
                    }
             	}).schedule();
        	}
		}
		return null;
	}
   

    private String askForCommitMessage() {
    	InputDialog dialog = new InputDialog(UIUtils.getActiveShell(), "Commit Changes", "Enter a commit message:", "", null);
        int result = dialog.open();
        if (result == InputDialog.OK) {
            return dialog.getValue();
        } else {
            return null;
        }
    }

    private void commitChanges(Git git, String commitMessage) throws GitAPIException {
        git.add().addFilepattern(".").call();
        CommitCommand commitCmd = git.commit();
        commitCmd.setMessage(commitMessage).call();
    }
    
    @Override
    public boolean isEnabled() {
    	return !isCleanGit.get(); //git repo is dirty so we may publish the changes
    }
    
    @Override
    public void resourceChanged(IResourceChangeEvent event) {
        if (event.getType() == IResourceChangeEvent.POST_CHANGE) {//save or save all buttons were pressed
        	isGitWs = isGitWorkspace();
        	if (isGitWs) isCleanGit.set(isCleanGitRepo()); //update git status
    		fireHandlerChanged(new HandlerEvent(this, true, false));
        }
    }
    
    private boolean isGitWorkspace() {
    	IPath workspaceRoot = workspace.getRoot().getLocation();
        File gitDir = new File(workspaceRoot.toFile(), ".git");
        if (!gitDir.exists() || !gitDir.isDirectory()) {
            return false;
        }
        return true;
    }
    
    
    private boolean isCleanGitRepo() {
    	if (isGitWs) {
    		try (Git git = new Git(repository)) {
				return git.status().call().isClean();
			} catch (NoWorkTreeException | GitAPIException e) {
				ServoyLog.logError(e);
			}
    	}
    	return true; //this will disable Publish
    }
}
