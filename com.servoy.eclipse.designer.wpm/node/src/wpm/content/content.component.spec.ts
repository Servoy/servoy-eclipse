import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Observable, Observer } from 'rxjs';
import { share } from 'rxjs/operators';

import { ContentComponent } from './content.component';
import { PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_WEB_SERVICE } from '../wpm.service';
import { PackagesInfo } from '../websocket.service';

describe('ContentComponent', () => {
  let component: ContentComponent;
  let wpmService: Record<string, any>;
  let packagesObserver: Observer<PackagesInfo>;
  let packagesObservable: Observable<PackagesInfo>;
  let cdr: { markForCheck: ReturnType<typeof vi.fn> };

  beforeEach(() => {
    packagesObservable = new Observable((obs: Observer<PackagesInfo>) => {
      packagesObserver = obs;
    }).pipe(share());

    wpmService = {
      getPackages: vi.fn(() => packagesObservable),
      setPackageLists: vi.fn(),
      versionCompare: vi.fn((v1: string, v2: string) => {
        const a = v1.split('.').map(Number);
        const b = v2.split('.').map(Number);
        for (let i = 0; i < Math.max(a.length, b.length); i++) {
          if ((a[i] || 0) < (b[i] || 0)) return -1;
          if ((a[i] || 0) > (b[i] || 0)) return 1;
        }
        return 0;
      }),
    };

    cdr = { markForCheck: vi.fn() };

    component = Object.create(ContentComponent.prototype);
    (component as any).wpmService = wpmService;
    (component as any).cdr = cdr;
    component.packageLists = [];
  });

  describe('ngOnInit', () => {
    it('should subscribe to packages and populate packageLists', () => {
      component.ngOnInit();

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: [
          { name: 'comp1', installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] } as any,
          { name: 'comp2', installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] } as any,
        ]
      });

      expect(component.packageLists).toHaveLength(1);
      expect(component.packageLists[0].title).toBe('Components');
      expect(component.packageLists[0].packages).toHaveLength(2);
      expect(component.packageLists[0].updateCount).toBe(1);
    });

    it('should call markForCheck after receiving packages', () => {
      component.ngOnInit();

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: [{ name: 'comp1', installed: '1.0.0', releases: [{ version: '1.0.0', url: '' }] } as any]
      });

      expect(cdr.markForCheck).toHaveBeenCalled();
    });

    it('should remove package list when empty packages received', () => {
      component.ngOnInit();

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: [{ name: 'comp1', installed: '1.0.0', releases: [{ version: '1.0.0', url: '' }] } as any]
      });
      expect(component.packageLists).toHaveLength(1);

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: []
      });
      expect(component.packageLists).toHaveLength(0);
    });

    it('should update existing package list when same type received again', () => {
      component.ngOnInit();

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: [{ name: 'comp1', installed: '1.0.0', releases: [{ version: '1.0.0', url: '' }] } as any]
      });

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: [
          { name: 'comp1', installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] } as any,
          { name: 'comp2', installed: '1.0.0', releases: [{ version: '1.0.0', url: '' }] } as any,
        ]
      });

      expect(component.packageLists).toHaveLength(1);
      expect(component.packageLists[0].packages).toHaveLength(2);
      expect(component.packageLists[0].updateCount).toBe(1);
    });

    it('should maintain ordering based on ALL_PACKAGE_TYPES', () => {
      component.ngOnInit();

      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_SERVICE,
        packages: [{ name: 'svc1', installed: '', releases: [{ version: '1.0.0', url: '' }] } as any]
      });
      packagesObserver!.next({
        packageType: PACKAGE_TYPE_WEB_COMPONENT,
        packages: [{ name: 'comp1', installed: '', releases: [{ version: '1.0.0', url: '' }] } as any]
      });

      expect(component.packageLists[0].type).toBe(PACKAGE_TYPE_WEB_COMPONENT);
      expect(component.packageLists[1].type).toBe(PACKAGE_TYPE_WEB_SERVICE);
    });
  });

  describe('getPackageTabLabel', () => {
    it('should return title when no updates', () => {
      const result = component.getPackageTabLabel({ title: 'Components', type: '', updateCount: 0, packages: [] });
      expect(result).toBe('Components');
    });

    it('should append update count when updates available', () => {
      const result = component.getPackageTabLabel({ title: 'Components', type: '', updateCount: 3, packages: [] });
      expect(result).toBe('Components (3)');
    });
  });

  describe('getUpgradeCount', () => {
    it('should count packages with newer release available', () => {
      const packages = [
        { installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] },
        { installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] },
        { installed: '1.0.0', releases: [{ version: '3.0.0', url: '' }] },
      ] as any[];

      expect(component.getUpgradeCount(packages)).toBe(2);
    });

    it('should return 0 when no upgrades available', () => {
      const packages = [
        { installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] },
        { installed: '3.0.0', releases: [{ version: '2.0.0', url: '' }] },
      ] as any[];

      expect(component.getUpgradeCount(packages)).toBe(0);
    });

    it('should skip packages without installed version', () => {
      const packages = [
        { installed: '', releases: [{ version: '2.0.0', url: '' }] },
      ] as any[];

      expect(component.getUpgradeCount(packages)).toBe(0);
    });
  });
});
