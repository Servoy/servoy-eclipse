import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BehaviorSubject, Observable, Observer } from 'rxjs';
import { share } from 'rxjs/operators';

import { HeaderComponent } from './header.component';
import { Package, Repository } from '../websocket.service';
import { PackageList } from '../content/content.component';
import { PACKAGE_TYPE_WEB_COMPONENT } from '../wpm.service';

describe('HeaderComponent', () => {
  let component: HeaderComponent;
  let wpmService: Record<string, any>;
  let dialog: Record<string, any>;
  let cdr: { markForCheck: ReturnType<typeof vi.fn> };
  let repositoriesObserver: Observer<Repository[]>;
  let packageListsSubject: BehaviorSubject<PackageList[]>;
  let packageToBeRemovedSubject: BehaviorSubject<Package>;

  const createPackage = (overrides: Partial<Package> = {}): Package => ({
    markedAsRemoved: false,
    activeSolution: 'mySolution',
    description: '',
    displayName: 'Test Package',
    icon: 'icon.png',
    installed: '1.0.0',
    installedIsWPA: true,
    installing: false,
    name: 'test-package',
    packageType: PACKAGE_TYPE_WEB_COMPONENT,
    releases: [{ version: '2.0.0', url: '' }],
    removing: false,
    selected: '2.0.0',
    sampleUrl: '',
    sourceUrl: '',
    top: false,
    wikiUrl: '',
    hasLatestVersion: false,
    ...overrides
  });

  beforeEach(() => {
    packageListsSubject = new BehaviorSubject<PackageList[]>([]);
    packageToBeRemovedSubject = new BehaviorSubject<Package>({} as Package);

    const repositoriesObservable = new Observable((obs: Observer<Repository[]>) => {
      repositoriesObserver = obs;
    }).pipe(share());

    wpmService = {
      getAllRepositories: vi.fn(() => repositoriesObservable),
      setNewSelectedRepository: vi.fn(),
      addNewRepository: vi.fn(),
      removeRepositoryWithName: vi.fn(),
      getActiveSolution: vi.fn(() => 'mySolution'),
      isNeedRefresh: vi.fn(() => false),
      isContentAvailable: vi.fn(() => true),
      versionCompare: (v1: string, v2: string) => {
        const a = v1.split('.').map(Number);
        const b = v2.split('.').map(Number);
        for (let i = 0; i < Math.max(a.length, b.length); i++) {
          if ((a[i] || 0) < (b[i] || 0)) return -1;
          if ((a[i] || 0) > (b[i] || 0)) return 1;
        }
        return 0;
      },
      packageLists: packageListsSubject,
      packageToBeRemoved: packageToBeRemovedSubject,
    };

    dialog = { open: vi.fn() };
    cdr = { markForCheck: vi.fn() };

    component = Object.create(HeaderComponent.prototype);
    (component as any).wpmService = wpmService;
    (component as any).dialog = dialog;
    (component as any).cdr = cdr;
    component.repositories = ['Servoy Default', 'Add...'];
    component.activeRepository = 'Servoy Default';
    component.packages = [];
    component.isUpdateAllButtonDisabled = false;
  });

  describe('ngOnInit', () => {
    it('should update repositories when received', () => {
      component.ngOnInit();

      repositoriesObserver!.next([
        { name: 'Repo1', selected: false },
        { name: 'Repo2', selected: true },
      ]);

      expect(component.repositories).toEqual(['Repo1', 'Repo2', 'Add...']);
      expect(component.activeRepository).toBe('Repo2');
      expect(cdr.markForCheck).toHaveBeenCalled();
    });

    it('should call setNewSelectedRepository when active repo changes', () => {
      component.ngOnInit();

      repositoriesObserver!.next([
        { name: 'New Repo', selected: true },
      ]);

      expect(wpmService.setNewSelectedRepository).toHaveBeenCalledWith('New Repo');
    });

    it('should not call setNewSelectedRepository when repo stays the same', () => {
      component.activeRepository = 'Servoy Default';
      component.ngOnInit();

      repositoriesObserver!.next([
        { name: 'Servoy Default', selected: true },
      ]);

      expect(wpmService.setNewSelectedRepository).not.toHaveBeenCalled();
    });

    it('should track packages needing update from packageLists', () => {
      component.ngOnInit();

      const pkg = createPackage({ name: 'comp1', installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      packageListsSubject.next([{
        title: 'Components',
        type: PACKAGE_TYPE_WEB_COMPONENT,
        updateCount: 1,
        packages: [pkg]
      }]);

      expect(component.packages).toHaveLength(1);
      expect(component.packages[0].name).toBe('comp1');
    });

    it('should not add already-latest packages to update list', () => {
      component.ngOnInit();

      const pkg = createPackage({ name: 'comp1', installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] });
      packageListsSubject.next([{
        title: 'Components',
        type: PACKAGE_TYPE_WEB_COMPONENT,
        updateCount: 0,
        packages: [pkg]
      }]);

      expect(component.packages).toHaveLength(0);
    });

    it('should mark packages as removed via packageToBeRemoved', () => {
      component.ngOnInit();

      const pkg = createPackage({ name: 'comp1', installed: '1.0.0' });
      component.packages = [pkg];

      packageToBeRemovedSubject.next(createPackage({ name: 'comp1' }));

      expect(pkg.markedAsRemoved).toBe(true);
      expect(cdr.markForCheck).toHaveBeenCalled();
    });
  });

  describe('isLatestRelease', () => {
    it('should return true when installed equals latest release', () => {
      const pkg = createPackage({ installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.isLatestRelease(pkg)).toBe(true);
    });

    it('should return true when installed is newer than latest release', () => {
      const pkg = createPackage({ installed: '3.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.isLatestRelease(pkg)).toBe(true);
    });

    it('should return false when installed is older', () => {
      const pkg = createPackage({ installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.isLatestRelease(pkg)).toBe(false);
    });
  });

  describe('onActiveRepositoryChange', () => {
    it('should call setNewSelectedRepository for normal repository', () => {
      component.activeRepository = 'My Custom Repo';
      component.onActiveRepositoryChange();
      expect(wpmService.setNewSelectedRepository).toHaveBeenCalledWith('My Custom Repo');
    });
  });

  describe('showRemoveRepository', () => {
    it('should return false for Servoy Default', () => {
      component.activeRepository = 'Servoy Default';
      expect(component.showRemoveRepository()).toBe(false);
    });

    it('should return false for Add...', () => {
      component.activeRepository = 'Add...';
      expect(component.showRemoveRepository()).toBe(false);
    });

    it('should return true for custom repository', () => {
      component.activeRepository = 'My Repo';
      expect(component.showRemoveRepository()).toBe(true);
    });
  });

  describe('removeRepository', () => {
    it('should delegate to wpmService', () => {
      component.activeRepository = 'My Repo';
      component.removeRepository();
      expect(wpmService.removeRepositoryWithName).toHaveBeenCalledWith('My Repo');
    });
  });

  describe('updateStateForUpdateAllButton', () => {
    it('should disable when all packages have latest version', () => {
      component.packages = [
        createPackage({ hasLatestVersion: true }),
        createPackage({ hasLatestVersion: true }),
      ];
      component.updateStateForUpdateAllButton();
      expect(component.isUpdateAllButtonDisabled).toBe(true);
    });

    it('should enable when some packages need update', () => {
      component.packages = [
        createPackage({ hasLatestVersion: true }),
        createPackage({ hasLatestVersion: false, installedIsWPA: true }),
      ];
      component.updateStateForUpdateAllButton();
      expect(component.isUpdateAllButtonDisabled).toBe(false);
    });

    it('should disable when non-latest packages are marked as removed', () => {
      component.packages = [
        createPackage({ hasLatestVersion: false, markedAsRemoved: true }),
      ];
      component.updateStateForUpdateAllButton();
      expect(component.isUpdateAllButtonDisabled).toBe(true);
    });
  });

  describe('getActiveSolution', () => {
    it('should delegate to wpmService', () => {
      expect(component.getActiveSolution()).toBe('mySolution');
    });
  });

  describe('isNeedRefresh', () => {
    it('should delegate to wpmService', () => {
      expect(component.isNeedRefresh()).toBe(false);
      wpmService.isNeedRefresh.mockReturnValue(true);
      expect(component.isNeedRefresh()).toBe(true);
    });
  });

  describe('isContentAvailable', () => {
    it('should delegate to wpmService', () => {
      expect(component.isContentAvailable()).toBe(true);
      wpmService.isContentAvailable.mockReturnValue(false);
      expect(component.isContentAvailable()).toBe(false);
    });
  });
});
