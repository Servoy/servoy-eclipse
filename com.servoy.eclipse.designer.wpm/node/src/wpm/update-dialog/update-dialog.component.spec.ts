import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

import { UpdatePackagesDialogComponent } from './update-dialog.component';
import { Package } from '../websocket.service';
import { WpmService, PACKAGE_TYPE_WEB_COMPONENT, PACKAGE_TYPE_SOLUTION } from '../wpm.service';

describe('UpdatePackagesDialogComponent', () => {
  let wpmService: Record<string, any>;
  let dialogRef: { close: ReturnType<typeof vi.fn> };

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

  const versionCompare = (v1: string, v2: string): number => {
    const a = v1.split('.').map(Number);
    const b = v2.split('.').map(Number);
    for (let i = 0; i < Math.max(a.length, b.length); i++) {
      if ((a[i] || 0) < (b[i] || 0)) return -1;
      if ((a[i] || 0) > (b[i] || 0)) return 1;
    }
    return 0;
  };

  const createComponent = (data: Package[]): UpdatePackagesDialogComponent => {
    TestBed.resetTestingModule();
    TestBed.configureTestingModule({
      providers: [
        UpdatePackagesDialogComponent,
        { provide: WpmService, useValue: wpmService },
        { provide: MatDialogRef, useValue: dialogRef },
        { provide: MAT_DIALOG_DATA, useValue: data },
      ],
    });
    return TestBed.inject(UpdatePackagesDialogComponent);
  };

  beforeEach(() => {
    wpmService = {
      install: vi.fn(),
      versionCompare: vi.fn(versionCompare),
    };
    dialogRef = { close: vi.fn() };
  });

  describe('constructor / extendedData building', () => {
    it('should include packages whose installed version is older than the latest release', () => {
      const pkg = createPackage({ installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      const component = createComponent([pkg]);

      expect(component.extendedData).toHaveLength(1);
      expect(component.extendedData[0].package).toBe(pkg);
    });

    it('should exclude packages already on the latest release', () => {
      const pkg = createPackage({ installed: '2.0.0', releases: [{ version: '2.0.0', url: '' }] });
      const component = createComponent([pkg]);

      expect(component.extendedData).toHaveLength(0);
    });

    it('should pre-select non-solution-main packages', () => {
      const pkg = createPackage({ installed: '1.0.0', packageType: PACKAGE_TYPE_WEB_COMPONENT, releases: [{ version: '2.0.0', url: '' }] });
      const component = createComponent([pkg]);

      expect(component.extendedData[0].isSelected).toBe(true);
    });

    it('should not pre-select solution-main packages', () => {
      const pkg = createPackage({ installed: '1.0.0', packageType: PACKAGE_TYPE_SOLUTION, releases: [{ version: '2.0.0', url: '' }] });
      const component = createComponent([pkg]);

      expect(component.extendedData[0].isSelected).toBe(false);
    });

    it('should default installingOrRemoving to false', () => {
      const component = createComponent([]);
      expect(component.installingOrRemoving).toBe(false);
    });
  });

  describe('updateSelected', () => {
    it('should install only selected packages with selected set to latest release', () => {
      const older = createPackage({ name: 'older', installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      const component = createComponent([older]);
      component.extendedData[0].isSelected = true;

      component.updateSelected();

      expect(component.extendedData[0].package.selected).toBe('2.0.0');
      expect(wpmService.install).toHaveBeenCalledTimes(1);
      expect(wpmService.install).toHaveBeenCalledWith(component.extendedData[0].package);
    });

    it('should not install packages that are not selected', () => {
      const older = createPackage({ name: 'older', installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      const component = createComponent([older]);
      component.extendedData[0].isSelected = false;

      component.updateSelected();

      expect(wpmService.install).not.toHaveBeenCalled();
    });
  });

  describe('updateAll', () => {
    it('should set every package to its latest release and install it', () => {
      const a = createPackage({ name: 'a', installed: '1.0.0', releases: [{ version: '2.0.0', url: '' }] });
      const b = createPackage({ name: 'b', installed: '1.5.0', releases: [{ version: '3.0.0', url: '' }] });
      const component = createComponent([a, b]);
      component.extendedData.forEach(d => (d.isSelected = false));

      component.updateAll();

      expect(a.selected).toBe('2.0.0');
      expect(b.selected).toBe('3.0.0');
      expect(wpmService.install).toHaveBeenCalledTimes(2);
      expect(wpmService.install).toHaveBeenCalledWith(a);
      expect(wpmService.install).toHaveBeenCalledWith(b);
    });
  });

  describe('closeDialog', () => {
    it('should close the dialog reference', () => {
      const component = createComponent([]);
      component.closeDialog();
      expect(dialogRef.close).toHaveBeenCalled();
    });
  });
});
