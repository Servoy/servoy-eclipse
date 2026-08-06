import { describe, it, expect, beforeEach, vi } from 'vitest';
import { BehaviorSubject, Observable, Observer } from 'rxjs';

import { WpmService, ALL_PACKAGE_TYPES, PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE } from './wpm.service';
import { Package, Repository } from './websocket.service';
import { PackageList } from './content/content.component';

describe('WpmService', () => {
  let service: WpmService;
  let messageSender: { next: ReturnType<typeof vi.fn> };

  const createPackage = (overrides: Partial<Package> = {}): Package => ({
    markedAsRemoved: false,
    activeSolution: 'mySolution',
    description: 'A package',
    displayName: 'Test Package',
    icon: 'icon.png',
    installed: '',
    installedIsWPA: true,
    installing: false,
    name: 'test-package',
    packageType: PACKAGE_TYPE_WEB_COMPONENT,
    releases: [{ version: '1.0.0', url: 'http://example.com' }],
    removing: false,
    selected: '1.0.0',
    sampleUrl: '',
    sourceUrl: '',
    top: false,
    wikiUrl: '',
    hasLatestVersion: false,
    ...overrides
  });

  beforeEach(() => {
    service = Object.create(WpmService.prototype);
    messageSender = { next: vi.fn() };
    (service as any).messageSender = messageSender;
    (service as any).solutions = [];
    (service as any).needRefresh = false;
    (service as any).contentAvailable = true;
    (service as any).url = new URL('http://localhost:8080/wpm/angular2/?darkTheme=false');

    (service as any).packageLists = new BehaviorSubject<PackageList[]>([]);
    (service as any).packageToBeRemoved = new BehaviorSubject<Package>({} as Package);
    (service as any).packagesObservable = new Observable((obs: Observer<any>) => {
      (service as any).packagesObserver = obs;
    });
    (service as any).repositoriesObservable = new Observable((obs: Observer<any>) => {
      (service as any).repositoriesObserver = obs;
    });
  });

  describe('versionCompare', () => {
    it('should return 0 for equal versions', () => {
      expect(service.versionCompare('1.0.0', '1.0.0')).toBe(0);
    });

    it('should return positive when first version is greater', () => {
      expect(service.versionCompare('2.0.0', '1.0.0')).toBeGreaterThan(0);
    });

    it('should return negative when first version is smaller', () => {
      expect(service.versionCompare('1.0.0', '2.0.0')).toBeLessThan(0);
    });

    it('should handle versions with different segment counts', () => {
      expect(service.versionCompare('1.0', '1.0.0')).toBe(0);
      expect(service.versionCompare('1.0.1', '1.0')).toBeGreaterThan(0);
    });

    it('should compare multi-digit segments correctly', () => {
      expect(service.versionCompare('1.10.0', '1.9.0')).toBeGreaterThan(0);
      expect(service.versionCompare('1.2.10', '1.2.9')).toBeGreaterThan(0);
    });

    it('should handle non-numeric segments', () => {
      expect(service.versionCompare('1.0.0-alpha', '1.0.0-beta')).toBeLessThan(0);
      expect(service.versionCompare('1.0.0-beta', '1.0.0-alpha')).toBeGreaterThan(0);
    });
  });

  describe('install', () => {
    it('should set installing flag and send install command', () => {
      const pkg = createPackage({ name: 'my-pkg', selected: '2.0.0' });
      service.install(pkg);

      expect(pkg.installing).toBe(true);
      expect(messageSender.next).toHaveBeenCalledWith({
        method: 'install',
        package: pkg
      });
    });
  });

  describe('uninstall', () => {
    it('should set removing flag and send remove command', () => {
      const pkg = createPackage({ name: 'my-pkg' });
      service.uninstall(pkg);

      expect(pkg.removing).toBe(true);
      expect(messageSender.next).toHaveBeenCalledWith({
        method: 'remove',
        package: pkg
      });
    });

    it('should emit on packageToBeRemoved', () => {
      const pkg = createPackage({ name: 'my-pkg' });
      const emitted: Package[] = [];
      service.packageToBeRemoved.subscribe(p => emitted.push(p));
      service.uninstall(pkg);

      expect(emitted[emitted.length - 1]).toBe(pkg);
    });
  });

  describe('showUrl', () => {
    it('should send showurl command', () => {
      service.showUrl('http://example.com');
      expect(messageSender.next).toHaveBeenCalledWith({
        method: 'showurl',
        url: 'http://example.com'
      });
    });
  });

  describe('setNewSelectedRepository', () => {
    it('should send setSelectedRepository command and clear packages', () => {
      // Setup packagesObserver
      const emitted: any[] = [];
      service.getPackages().subscribe(p => emitted.push(p));

      service.setNewSelectedRepository('My Repo');

      expect(messageSender.next).toHaveBeenCalledWith({
        method: 'setSelectedRepository',
        name: 'My Repo'
      });
    });
  });

  describe('addNewRepository', () => {
    it('should send addRepository command', () => {
      const repo: Repository = { name: 'New Repo', url: 'http://repo.com' };
      service.addNewRepository(repo);

      expect(messageSender.next).toHaveBeenCalledWith({
        method: 'addRepository',
        values: repo
      });
    });
  });

  describe('removeRepositoryWithName', () => {
    it('should send removeRepository command', () => {
      service.removeRepositoryWithName('Old Repo');

      expect(messageSender.next).toHaveBeenCalledWith({
        method: 'removeRepository',
        name: 'Old Repo'
      });
    });
  });

  describe('requestAllInstalledPackages', () => {
    it('should group packages by type and emit to observer', () => {
      const emitted: any[] = [];
      service.getPackages().subscribe(p => emitted.push(p));

      const packages = [
        createPackage({ name: 'comp1', packageType: PACKAGE_TYPE_WEB_COMPONENT }),
        createPackage({ name: 'svc1', packageType: PACKAGE_TYPE_WEB_SERVICE }),
        createPackage({ name: 'comp2', packageType: PACKAGE_TYPE_WEB_COMPONENT }),
      ];
      service.requestAllInstalledPackages(packages);

      const componentEmit = emitted.find(e => e.packageType === PACKAGE_TYPE_WEB_COMPONENT);
      expect(componentEmit.packages).toHaveLength(2);

      const serviceEmit = emitted.find(e => e.packageType === PACKAGE_TYPE_WEB_SERVICE);
      expect(serviceEmit.packages).toHaveLength(1);
    });

    it('should emit empty arrays for missing package types', () => {
      const emitted: any[] = [];
      service.getPackages().subscribe(p => emitted.push(p));

      const packages = [
        createPackage({ name: 'comp1', packageType: PACKAGE_TYPE_WEB_COMPONENT }),
      ];
      service.requestAllInstalledPackages(packages);

      for (const pkgType of ALL_PACKAGE_TYPES) {
        const emit = emitted.find(e => e.packageType === pkgType);
        expect(emit).toBeDefined();
      }
    });

    it('should not emit anything for empty array', () => {
      const emitted: any[] = [];
      service.getPackages().subscribe(p => emitted.push(p));

      service.requestAllInstalledPackages([]);

      expect(emitted).toHaveLength(0);
    });
  });

  describe('getSolutionList', () => {
    it('should store the solutions', () => {
      service.getSolutionList(['sol1', 'sol2']);
      expect(service.getAllSolutions()).toEqual(['sol1', 'sol2']);
    });
  });

  describe('getActiveSolution', () => {
    it('should return the last solution in the list', () => {
      service.getSolutionList(['sol1', 'sol2', 'sol3']);
      expect(service.getActiveSolution()).toBe('sol3');
    });

    it('should return empty string when no solutions', () => {
      expect(service.getActiveSolution()).toBe('');
    });
  });

  describe('refreshRemotePackages', () => {
    it('should set needRefresh to true', () => {
      expect(service.isNeedRefresh()).toBe(false);
      (service as any).needRefresh = true;
      expect(service.isNeedRefresh()).toBe(true);
    });
  });

  describe('contentNotAvailable', () => {
    it('should set contentAvailable to false', () => {
      expect(service.isContentAvailable()).toBe(true);
      (service as any).contentAvailable = false;
      expect(service.isContentAvailable()).toBe(false);
    });
  });

  describe('isDarkTheme', () => {
    it('should return false when darkTheme param is not true', () => {
      expect(service.isDarkTheme()).toBe(false);
    });

    it('should return true when darkTheme param is true', () => {
      (service as any).url = new URL('http://localhost:8080/wpm/?darkTheme=true');
      expect(service.isDarkTheme()).toBe(true);
    });
  });

  describe('clearPackages', () => {
    it('should emit empty packages for all types', () => {
      const emitted: any[] = [];
      service.getPackages().subscribe(p => emitted.push(p));

      service.clearPackages();

      expect(emitted).toHaveLength(ALL_PACKAGE_TYPES.length);
      for (const emit of emitted) {
        expect(emit.packages).toHaveLength(0);
      }
    });
  });

  describe('setPackageLists', () => {
    it('should emit on packageLists BehaviorSubject', () => {
      const lists = [{ title: 'Components', type: PACKAGE_TYPE_WEB_COMPONENT, updateCount: 0, packages: [] }];
      const emitted: any[] = [];
      service.packageLists.subscribe(l => emitted.push(l));

      service.setPackageLists(lists);

      expect(emitted[emitted.length - 1]).toBe(lists);
    });
  });

  describe('addRepository', () => {
    it('should call getRepositories and requestAllInstalledPackages', () => {
      const emitted: any[] = [];
      service.getPackages().subscribe(p => emitted.push(p));

      const repos: Repository[] = [{ name: 'Repo1', selected: true }];
      const packages = [createPackage({ name: 'pkg1' })];

      // We need repositoriesObserver to be set
      const repoEmitted: any[] = [];
      service.getAllRepositories().subscribe(r => repoEmitted.push(r));

      service.addRepository({ repositories: repos, packages });

      expect(repoEmitted[repoEmitted.length - 1]).toEqual(repos);
      expect(emitted.length).toBeGreaterThan(0);
    });
  });
});
