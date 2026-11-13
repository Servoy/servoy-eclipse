# Spec: SVY-20602 — Add "Remove" and "Delete" developer solution actions to the Solution Explorer

## 1. Goal
Add two context-menu actions on each `DEVELOPER_SOLUTION` child node under the active
solution's *Developer solutions* node in the Solution Explorer, mirroring the existing
modules experience (Remove module... + Delete):
- **"Remove developer solution"** (non-destructive) — un-references the selected
  developer-solution project from the active solution's `IProjectDescription`, the exact
  inverse of *Add developer solution*, leaving the project in the workspace on disk.
- **"Delete developer solution"** (destructive) — runs the standard Eclipse delete-resource
  operation (the same *Delete Resources* dialog modules use, offering the "Delete project
  contents on disk (cannot be undone)" option). Only when a delete actually proceeds does it
  un-reference the project from the active solution and refresh the tree; cancelling the
  dialog changes nothing.

This closes the asymmetry in the experimental "Developer solutions" feature, which shipped
with create/add actions but no remove or delete counterpart. It also fixes a stale model
cache / missing tree-refresh bug in the feature (see §3.4 / §4).

## 2. Background
The "Developer solutions" feature was introduced under **SVY-20274** ("servoy developer
module", commits `10c39f5320`, `97a70b31af`, `47cae01947`, `76998456ab`, `0cb43d1415`). It
is gated behind the experimental preference *"Show Developer Solution node (experimental)"*
(`SolutionExplorerView.INCLUDE_DEVELOPER_SOLUTION`), and only renders its tree nodes when
that preference is on (`SolutionExplorerTreeContentProvider.java:989`).

Node types (`com.servoy.eclipse.ui/.../node/UserNodeType.java`):
- `DEVELOPER_SOLUTIONS` — the parent container node (one per active solution).
- `DEVELOPER_SOLUTION` — a child node per referenced developer-solution project. Its real
  object is a `ServoyDeveloperProject` (a subclass of `ServoyProject`), created at
  `SolutionExplorerTreeContentProvider.java:900-907`.

Existing actions, wired for the **parent** node only:
- **Create developer solution** → `NewDeveloperSolutionAction`
  (`com.servoy.eclipse.ui/.../wizards/NewDeveloperSolutionAction.java`), registered on
  `DEVELOPER_SOLUTIONS`. It creates a new project (at the default workspace location, open)
  with the `ServoyDeveloperProject` nature and adds it as a referenced project of the active
  solution via `ServoyProject.addDeveloperProject(...)`.
- **Add developer solution** → `AddDevSolutionAction`
  (`com.servoy.eclipse.ui/.../views/solutionexplorer/actions/AddDevSolutionAction.java`),
  enabled only when a `DEVELOPER_SOLUTIONS` node is selected. It edits the active solution's
  `IProjectDescription.setReferencedProjects(...)` to add the chosen developer projects
  (preserving all non-developer referenced projects).

The reference edit used by Add is:
```java
IProjectDescription desc = activeSolution.getProject().getDescription();
desc.setReferencedProjects(referencedProjects.toArray(new IProject[0]));
activeSolution.getProject().setDescription(desc, null);
```

The model already has `ServoyProject.addDeveloperProject(IProject)`, which appends a project
to the active solution's referenced projects. There was **no** symmetric
`removeDeveloperProject(...)`.

**The gap:** no action was registered for the `DEVELOPER_SOLUTION` child node — neither a
dedicated remove action nor an entry in the generic delete dispatcher `deleteActionInTree`.
Selecting a developer-solution child offered no remove or delete.

**Precedents mirrored:**
- `RemoveModuleAction` — removes a module reference from a parent solution without deleting
  the project ("the solution itself will not be deleted"); self-enables via
  `ISelectionChangedListener`.
- `DeleteSolutionAction` — deletes the selected project(s) via the standard Eclipse
  `org.eclipse.ui.actions.DeleteResourceAction`, detecting a real delete through the
  `IDEWorkbenchMessages.DeleteResourceAction_jobName` background job.

### Git history (carried from triage)
- The whole feature, including both existing actions, the two node types, and the
  experimental preference, was added under **SVY-20274**. The absence of a remove/delete
  action is an original omission of that feature work, not a regression.
- No dependency/target-platform version change is involved.

## 3. Design

### 3.1 Model — `ServoyProject.removeDeveloperProject(IProject)`
Add a method to `com.servoy.eclipse.model.nature.ServoyProject`, directly after
`addDeveloperProject(...)`, that is the exact inverse: it reads the active solution project's
`IProjectDescription`, removes the given developer project from the referenced-projects array
(keeping everything else), writes the description back, and invalidates the model's project
cache (see §3.4). As implemented:

```java
/**
 * Removes the given developer-solution project from this (active) solution's referenced
 * projects. The developer-solution project itself is left in the workspace on disk.
 *
 * @param developerProject the developer-solution project to un-reference
 */
public void removeDeveloperProject(IProject developerProject)
{
    try
    {
        IProjectDescription description = project.getDescription();
        IProject[] referencedProjects = description.getReferencedProjects();
        ArrayList<IProject> newProjects = new ArrayList<IProject>(Arrays.asList(referencedProjects));
        if (newProjects.remove(developerProject))
        {
            description.setReferencedProjects(newProjects.toArray(new IProject[newProjects.size()]));
            project.setDescription(description, null);
            ServoyModelFinder.getServoyModel().refreshServoyProjects();
        }
    }
    catch (CoreException e)
    {
        ServoyLog.logError("Exception while removing developer project " + developerProject.getName() + " from " + project.getName(), e);
    }
}
```

All required imports (`IProject`, `IProjectDescription`, `CoreException`, `ArrayList`,
`Arrays`, `ServoyLog`, `ServoyModelFinder`) are already present in `ServoyProject.java` — no
new imports needed. The `newProjects.remove(...)` guard avoids writing the description back
(and invalidating the cache) when nothing changed.

### 3.2 UI — `RemoveDevSolutionAction` (non-destructive un-reference)
Create
`com.servoy.eclipse.ui/.../views/solutionexplorer/actions/RemoveDevSolutionAction.java`,
modelled on `AddDevSolutionAction` (constructor shape, `ISelectionChangedListener`
enablement) and `RemoveModuleAction` (single-node selection, grabbing the selected
`ServoyProject` real object, non-destructive remove, "solution not deleted" wording).

Behaviour:
- **Text / tooltip / icon.** `setText("Remove developer solution")`,
  `setToolTipText("Remove the developer solution from the active solution (the solution itself will not be deleted)")`,
  and reuse the module remove icon `remove_module.png` so the look matches the existing
  Remove entry.
- **Enablement (`selectionChanged`).** Enable only when exactly one node is selected, the
  active project is non-null, and the node type is `DEVELOPER_SOLUTION`; capture the node's
  `ServoyProject` real object.
- **`run()`.** Resolve the active solution; if null, return. Call
  `activeProject.removeDeveloperProject(selectedProject.getProject())`, then
  `viewer.refreshTreeCompletely()`. No confirmation dialog — the operation is non-destructive
  and reversible via *Add developer solution* (consistent with `RemoveModuleAction`).

As implemented:
```java
public class RemoveDevSolutionAction extends Action implements ISelectionChangedListener
{
    private ServoyProject selectedProject = null;
    private final SolutionExplorerView viewer;

    public RemoveDevSolutionAction(Shell shell, SolutionExplorerView viewer)
    {
        this.viewer = viewer;
        setText("Remove developer solution");
        setToolTipText("Remove the developer solution from the active solution (the solution itself will not be deleted)");
        setImageDescriptor(Activator.loadImageDescriptorFromBundle("remove_module.png"));
    }

    @Override
    public void run()
    {
        if (selectedProject == null) return;
        ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
        if (activeProject == null) return;
        activeProject.removeDeveloperProject(selectedProject.getProject());
        if (viewer != null) viewer.refreshTreeCompletely();
    }

    public void selectionChanged(SelectionChangedEvent event)
    {
        IDeveloperServoyModel servoyModel = ServoyModelManager.getServoyModelManager().getServoyModel();
        selectedProject = null;
        IStructuredSelection sel = (IStructuredSelection)event.getSelection();
        boolean state = (sel.size() == 1) && (servoyModel.getActiveProject() != null);
        if (state)
        {
            SimpleUserNode node = (SimpleUserNode)sel.getFirstElement();
            state = (node.getType() == UserNodeType.DEVELOPER_SOLUTION) && (node.getRealObject() instanceof ServoyProject);
            if (state) selectedProject = (ServoyProject)node.getRealObject();
        }
        setEnabled(state);
    }
}
```
The `shell` parameter is kept in the constructor signature for wiring symmetry with the
sibling actions even though this action shows no dialog (the unused `shell` field was dropped
per code review).

### 3.3 UI — `DeleteDevSolutionAction` (destructive delete)
Create
`com.servoy.eclipse.ui/.../views/solutionexplorer/actions/DeleteDevSolutionAction.java`,
modelled on `DeleteSolutionAction` (which uses the standard Eclipse
`org.eclipse.ui.actions.DeleteResourceAction`). Enablement mirrors `RemoveDevSolutionAction`:
a single `DEVELOPER_SOLUTION` node with a non-null active project, capturing the node's
`ServoyProject` real object. Icon: `delete.png` (the same icon the module/persist delete
uses).

`run()`:
1. `deleteAction.selectionChanged(new StructuredSelection(new IProject[] { project }))` then
   `deleteAction.run()` — shows the standard *Delete Resources* dialog ("Remove project 'x'
   from the workspace?" with the "Delete project contents on disk (cannot be undone)" checkbox
   and Preview/OK/Cancel).
2. The standard delete runs in a background job. Determine whether a delete actually proceeded
   using the same check `DeleteSolutionAction` uses — a running
   `IDEWorkbenchMessages.DeleteResourceAction_jobName` job, or the project no longer existing.
3. **Only when a delete actually proceeded:** un-reference the project from the active solution
   (`activeProject.removeDeveloperProject(project)`) so no dangling reference is left behind,
   then `viewer.refreshTreeCompletely()`. If the user cancelled the dialog, nothing is touched
   — no un-reference, no refresh.

As implemented:
```java
@Override
public void run()
{
    if (selectedProject == null) return;
    IProject project = selectedProject.getProject();
    deleteAction.selectionChanged(new StructuredSelection(new IProject[] { project }));
    deleteAction.run();
    boolean deleting = Job.getJobManager().find(IDEWorkbenchMessages.DeleteResourceAction_jobName).length > 0 || !project.exists();
    if (deleting)
    {
        ServoyProject activeProject = ServoyModelManager.getServoyModelManager().getServoyModel().getActiveProject();
        if (activeProject != null)
        {
            activeProject.removeDeveloperProject(project);
        }
        if (viewer != null) viewer.refreshTreeCompletely();
    }
}
```

The action takes an `IShellProvider` (the view's `getSite()`) and the `SolutionExplorerView`
in its constructor.

**Disk-contents behaviour (matches modules):** the "Delete project contents on disk" checkbox
maps to Eclipse's `IProject.delete` flags. Per the platform contract, deleting an *open*
project deletes its files regardless of the checkbox (the checkbox only preserves files for a
*closed* or custom-location project). Developer solutions are created open and at the default
workspace location, so — exactly like module/solution Delete, which uses the same
`DeleteResourceAction` — the files are removed either way. This is standard Eclipse behaviour,
not specific to this action; forcing `NEVER_DELETE_PROJECT_CONTENT` to make the checkbox
meaningful for open projects would require a custom dialog and is deliberately out of scope.

### 3.4 Model cache + tree refresh (feature-wide bug fixed alongside)
The experimental feature shipped with a stale-cache / no-refresh bug surfaced during manual
testing of this change: editing the active solution's referenced projects did not invalidate
the model's project cache (`AbstractServoyModel.servoyProjectCache`) nor refresh the Solution
Explorer. As a result a developer-solution child only appeared after a manual refresh, and a
remove-then-re-add of the same project did not reappear until a Developer restart (the tree's
`DEVELOPER_SOLUTION` children come from `activeProject.getDeveloperProjects()`, which resolves
the nature object from that cache). Create escaped the bug only because creating a project
fires a workspace delta large enough for `ServoyModel` to invalidate the cache; a plain
referenced-projects edit does not.

Fixed at the shared source:
- **Model:** `addDeveloperProject(...)` and `removeDeveloperProject(...)` call
  `ServoyModelFinder.getServoyModel().refreshServoyProjects()` after `setDescription(...)`,
  invalidating the cache so `getDeveloperProjects()` resolves fresh nature objects. This one
  change covers Create, Add, Remove and Delete (all route through these two methods).
- **UI:** the four actions refresh the tree after their change —
  `AddDevSolutionAction` and `RemoveDevSolutionAction` each gain a `SolutionExplorerView`
  constructor argument and call `viewer.refreshTreeCompletely()`; `DeleteDevSolutionAction`
  does so only when a delete proceeded; `NewDeveloperSolutionAction` (which already holds the
  view) calls it after a successful create.

### 3.5 UI wiring — `SolutionExplorerView`
For both `removeDevSolutionAction` and `deleteDevSolutionAction`, mirror the
`addDevSolutionAction` touch points:
1. Field declarations next to `AddDevSolutionAction addDevSolutionAction;`.
2. Instantiation next to `addDevSolutionAction = new AddDevSolutionAction(shell, this);` —
   `removeDevSolutionAction = new RemoveDevSolutionAction(shell, this);` and
   `deleteDevSolutionAction = new DeleteDevSolutionAction(getSite(), this);`.
3. Register as tree selection listeners next to
   `addTreeSelectionChangedListener(addDevSolutionAction);`.
4. Add to the context menu next to
   `if (addDevSolutionAction.isEnabled()) manager.add(addDevSolutionAction);` — the Remove
   entry then the Delete entry, both conditional on `isEnabled()`.

`AddDevSolutionAction`'s constructor also gains the `SolutionExplorerView viewer` argument so
it can refresh the tree.

Because Remove/Delete self-enable only for `DEVELOPER_SOLUTION` nodes and Add self-enables
only for `DEVELOPER_SOLUTIONS` nodes, Add never shows together with Remove/Delete; no
preference re-check is needed in the view (the preference already governs whether the nodes
exist at all).

## 4. Implementation plan
1. **Model:** add `ServoyProject.removeDeveloperProject(IProject)` immediately after
   `addDeveloperProject(...)` in `com.servoy.eclipse.model/.../nature/ServoyProject.java`
   (§3.1), and add `ServoyModelFinder.getServoyModel().refreshServoyProjects()` after the
   `setDescription(...)` in **both** `addDeveloperProject` and `removeDeveloperProject` (§3.4).
   No new imports.
2. **Remove action:** create
   `com.servoy.eclipse.ui/.../views/solutionexplorer/actions/RemoveDevSolutionAction.java`
   (§3.2), modelled on `AddDevSolutionAction` + `RemoveModuleAction`, refreshing the tree.
3. **Delete action:** create
   `com.servoy.eclipse.ui/.../views/solutionexplorer/actions/DeleteDevSolutionAction.java`
   (§3.3), modelled on `DeleteSolutionAction`, using the standard `DeleteResourceAction`;
   un-reference + refresh only when a delete actually proceeds.
4. **View wiring:** in `SolutionExplorerView.java`, add fields, instantiation,
   tree-selection-listener registration, and the conditional `manager.add(...)` entries for
   both new actions, and pass `this` into `AddDevSolutionAction`/`RemoveDevSolutionAction`
   (§3.5).
5. **No MANIFEST / build.properties changes.** Both bundles
   (`com.servoy.eclipse.model`, `com.servoy.eclipse.ui`) already export/require everything
   used; `ServoyModelFinder` is already imported in `ServoyProject.java`, `SolutionExplorerView`
   is in an already-required package, and the new actions reuse already-bundled images
   (`remove_module.png`, `delete.png`). No new packages or resources.
6. **Verify:** `getCompilationErrors` on both edited bundles; organize imports; run
   `ServoyProjectDeveloperReferenceTest` (PDE integration); smoke-test manually with the
   experimental preference enabled.

## 5. Acceptance criteria
- [ ] With *"Show Developer Solution node (experimental)"* enabled and at least one developer
      solution referenced by the active solution, right-clicking a `DEVELOPER_SOLUTION` child
      node shows **"Remove developer solution"** and **"Delete developer solution"**
      context-menu entries (with the remove/delete icons).
- [ ] Neither entry is shown on the parent *Developer solutions* (`DEVELOPER_SOLUTIONS`) node
      nor on any other node type; Add/Create remain only on the parent node.
- [ ] **Remove** un-references the developer-solution project from the active solution's
      referenced projects (the child node disappears), and the project itself **remains in the
      workspace on disk** (not deleted).
- [ ] After removing, the same developer solution can be re-added via *Add developer solution*
      (round-trip works), confirming Remove is the exact inverse of Add.
- [ ] Non-developer referenced projects of the active solution are left untouched by Remove.
- [ ] **Delete** opens the standard Eclipse *Delete Resources* dialog with the "Delete project
      contents on disk (cannot be undone)" option. Confirming removes the project from the
      workspace (and, for an open default-location project, from disk) and leaves the active
      solution with no dangling reference to it. **Cancelling the dialog changes nothing** —
      the project stays referenced and the tree is not refreshed.
- [ ] After Create, Add, Remove or a confirmed Delete, the Solution Explorer tree updates
      **immediately** without a manual refresh.
- [ ] Removing a developer solution and then re-adding the same project shows its child node
      again **without a Developer restart** (the model project cache is invalidated on
      add/remove).
- [ ] Both bundles compile with no new errors; `com.servoy.eclipse.model` and
      `com.servoy.eclipse.ui` build cleanly.

## 6. Out of scope
- A confirmation dialog for **Remove** (not needed for a reversible, non-destructive
  un-reference, consistent with `RemoveModuleAction`). Delete uses the standard Eclipse
  *Delete Resources* dialog, which already confirms and offers the on-disk option.
- Making the "Delete project contents on disk" checkbox meaningful for open default-location
  projects (would require a custom dialog forcing `NEVER_DELETE_PROJECT_CONTENT`); current
  behaviour matches module/solution Delete.
- Multi-select Remove/Delete (match the single-node enablement of the sibling actions).
- Any change to the experimental preference gating or the node rendering logic in
  `SolutionExplorerTreeContentProvider`. (The `Create`/`Add` actions **are** touched, but only
  to add the tree refresh described in §3.4 — their core behaviour is unchanged.)
- The preference-label typo ("Developer *Sotion*") in `SolutionExplorerPreferences` — noted
  but left untouched unless explicitly requested.

## 7. Resolved decisions
| Question | Outcome |
|----------|---------|
| Keep the `shell` constructor parameter on `RemoveDevSolutionAction`? | Kept for wiring symmetry; the unused `shell` field was dropped per code review. |
| Reuse an existing icon or add a new one? | Reused `remove_module.png` for Remove and `delete.png` for Delete (same icons as the module/persist equivalents). |
| Remove (un-reference) only, or also Delete (remove from disk)? | Both implemented, mirroring the modules experience (Remove + Delete). |
