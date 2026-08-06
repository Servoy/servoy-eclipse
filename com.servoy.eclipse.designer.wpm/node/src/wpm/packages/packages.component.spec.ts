import { describe, it, expect, beforeEach, vi } from 'vitest';

import { PackagesComponent } from './packages.component';
import { Package } from '../websocket.service';
import { PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_MODULE, PACKAGE_TYPE_SOLUTION } from '../wpm.service';

describe('PackagesComponent', () => {
  let component: PackagesComponent;
  let wpmService: Record<string, any>;
  let dialog: Record<string, any>;

  const createPackage = (overrides: Partial<Package> = {}): Package => ({
    markedAsRemoved: false,
    activeSolution: 'mySolution',
    description: 'A test package',
    displayName: 'Test Package',
    icon: 'icon.png',
    installed: '1.0.0',
    installedIsWPA: true,
    installing: false,
    name: 'test-package',
    packageType: PACKAGE_TYPE_WEB_COMPONENT,
    releases: [{ version: '2.0.0', url: 'http://example.com' }],
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
    wpmService = {
      install: vi.fn(),
      uninstall: vi.fn(),
      showUrl: vi.fn(),
      isDarkTheme: vi.fn(() => false),
      getAllSolutions: vi.fn(() => ['sol1', 'sol2']),
      getActiveSolution: vi.fn(() => 'sol2'),
      versionCompare: (v1: string, v2: string) => {
        const a = v1.split('.').map(Number);
        const b = v2.split('.').map(Number);
        for (let i = 0; i < Math.max(a.length, b.length); i++) {
          if ((a[i] || 0) < (b[i] || 0)) return -1;
          if ((a[i] || 0) > (b[i] || 0)) return 1;
        }
        return 0;
      },
    };

    dialog = { open: vi.fn() };

    component = Object.create(PackagesComponent.prototype);
    (component as any).wpmService = wpmService;
    (component as any).dialog = dialog;
    component.packages = [];
    component.selectedPackage = undefined as any;
    component.descriptionExpanded = false;
  });

  describe('ngOnChanges', () => {
    it('should set default selected version for packages without one', () => {
      const pkg = createPackage({ selected: '' });
      component.packages = [pkg];
      component.ngOnChanges({ packages: { currentValue: [pkg], previousValue: undefined, firstChange: true, isFirstChange: () => true } });

      expect(pkg.selected).toBe('2.0.0');
    });

    it('should not override existing selected version', () => {
      const pkg = createPackage({ selected: '1.5.0' });
      component.packages = [pkg];
      component.ngOnChanges({ packages: { currentValue: [pkg], previousValue: undefined, firstChange: true, isFirstChange: () => true } });

      expect(pkg.selected).toBe('1.5.0');
    });
  });

  describe('install', () => {
    it('should delegate to wpmService', () => {
      const pkg = createPackage();
      component.install(pkg);
      expect(wpmService.install).toHaveBeenCalledWith(pkg);
    });
  });

  describe('uninstall', () => {
    it('should delegate to wpmService', () => {
      const pkg = createPackage();
      component.uninstall(pkg);
      expect(wpmService.uninstall).toHaveBeenCalledWith(pkg);
    });
  });

  describe('showUrl', () => {
    it('should delegate to wpmService', () => {
      component.showUrl('http://example.com');
      expect(wpmService.showUrl).toHaveBeenCalledWith('http://example.com');
    });
  });

  describe('installAvailable', () => {
    it('should return true when not installed', () => {
      const pkg = createPackage({ installed: '' });
      expect(component.installAvailable(pkg)).toBe(true);
    });

    it('should return true when installed version differs from selected', () => {
      const pkg = createPackage({ installed: '1.0.0', selected: '2.0.0', installedIsWPA: true });
      expect(component.installAvailable(pkg)).toBe(true);
    });

    it('should return false when installed version equals selected', () => {
      const pkg = createPackage({ installed: '2.0.0', selected: '2.0.0', installedIsWPA: true });
      expect(component.installAvailable(pkg)).toBe(false);
    });

    it('should return false when not installed via WPA', () => {
      const pkg = createPackage({ installed: '1.0.0', selected: '2.0.0', installedIsWPA: false });
      expect(component.installAvailable(pkg)).toBe(false);
    });
  });

  describe('canBeRemoved', () => {
    it('should return true for installed WPA web components', () => {
      const pkg = createPackage({ installed: '1.0.0', installedIsWPA: true, packageType: PACKAGE_TYPE_WEB_COMPONENT });
      expect(component.canBeRemoved(pkg)).toBe(true);
    });

    it('should return false for modules', () => {
      const pkg = createPackage({ installed: '1.0.0', installedIsWPA: true, packageType: PACKAGE_TYPE_MODULE });
      expect(component.canBeRemoved(pkg)).toBe(false);
    });

    it('should return false for solutions', () => {
      const pkg = createPackage({ installed: '1.0.0', installedIsWPA: true, packageType: PACKAGE_TYPE_SOLUTION });
      expect(component.canBeRemoved(pkg)).toBe(false);
    });

    it('should return false when not installed', () => {
      const pkg = createPackage({ installed: '' });
      expect(component.canBeRemoved(pkg)).toBe(false);
    });

    it('should return false when not installed via WPA', () => {
      const pkg = createPackage({ installed: '1.0.0', installedIsWPA: false });
      expect(component.canBeRemoved(pkg)).toBe(false);
    });
  });

  describe('isLatestRelease', () => {
    it('should return true when installed matches latest release', () => {
      const pkg = createPackage({ installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.isLatestRelease(pkg)).toBe(true);
    });

    it('should return false when installed is older', () => {
      const pkg = createPackage({ installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.isLatestRelease(pkg)).toBe(false);
    });
  });

  describe('isInstallingOrRemoving', () => {
    it('should return true when installing', () => {
      const pkg = createPackage({ installing: true });
      expect(component.isInstallingOrRemoving(pkg)).toBe(true);
    });

    it('should return true when removing', () => {
      const pkg = createPackage({ removing: true });
      expect(component.isInstallingOrRemoving(pkg)).toBe(true);
    });

    it('should return false when neither', () => {
      const pkg = createPackage({ installing: false, removing: false });
      expect(component.isInstallingOrRemoving(pkg)).toBe(false);
    });
  });

  describe('togglePackageSelection', () => {
    it('should select package and expand description', () => {
      const pkg = createPackage({ description: 'Some description' });
      const event = { stopPropagation: vi.fn() } as any;

      component.togglePackageSelection(event, pkg);

      expect(component.selectedPackage).toBe(pkg);
      expect(component.descriptionExpanded).toBe(true);
      expect(event.stopPropagation).toHaveBeenCalled();
    });

    it('should toggle description when same package clicked again', () => {
      const pkg = createPackage({ description: 'Some description' });
      const event = { stopPropagation: vi.fn() } as any;

      component.togglePackageSelection(event, pkg);
      expect(component.descriptionExpanded).toBe(true);

      component.togglePackageSelection(event, pkg);
      expect(component.descriptionExpanded).toBe(false);
    });

    it('should not expand when package has no description', () => {
      const pkg = createPackage({ description: '' });
      const event = { stopPropagation: vi.fn() } as any;

      component.togglePackageSelection(event, pkg);
      expect(component.descriptionExpanded).toBe(false);
    });
  });

  describe('getSelectedRelease', () => {
    it('should return Latest when selected is latest version', () => {
      const pkg = createPackage({ selected: '2.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.getSelectedRelease(pkg)).toBe('Latest');
    });

    it('should return empty when selected is not latest', () => {
      const pkg = createPackage({ selected: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      expect(component.getSelectedRelease(pkg)).toBe('');
    });
  });

  describe('invertIcon', () => {
    it('should return true when dark theme and invertIcon is set', () => {
      wpmService.isDarkTheme.mockReturnValue(true);
      const pkg = createPackage({ invertIcon: true });
      expect(component.invertIcon(pkg)).toBe(true);
    });

    it('should return false when not dark theme', () => {
      wpmService.isDarkTheme.mockReturnValue(false);
      const pkg = createPackage({ invertIcon: true });
      expect(component.invertIcon(pkg)).toBe(false);
    });

    it('should return false when invertIcon not set', () => {
      wpmService.isDarkTheme.mockReturnValue(true);
      const pkg = createPackage({ invertIcon: undefined });
      expect(component.invertIcon(pkg)).toBe(false);
    });
  });

  describe('needsActiveSolution', () => {
    it('should return true for non-solution packages', () => {
      const pkg = createPackage({ packageType: PACKAGE_TYPE_WEB_COMPONENT });
      expect(component.needsActiveSolution(pkg)).toBe(true);
    });

    it('should return false for solution packages', () => {
      const pkg = createPackage({ packageType: PACKAGE_TYPE_SOLUTION });
      expect(component.needsActiveSolution(pkg)).toBe(false);
    });
  });

  describe('getSolutions', () => {
    it('should delegate to wpmService', () => {
      expect(component.getSolutions()).toEqual(['sol1', 'sol2']);
    });
  });
});
