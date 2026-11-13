import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { PopupFormService } from './popupform.service';
import { FormService } from '../form.service';
import { ServicesService } from '../../sablo/services.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { MainViewRefService } from '@servoy/public';

describe('PopupFormService', () => {
  let service: PopupFormService;
  let formService: any;
  let servicesService: any;
  let servoyService: any;
  let utils: any;

  beforeEach(() => {
    formService = { formWillShow: vi.fn().mockResolvedValue(true), hideForm: vi.fn() };
    servicesService = { callServiceServerSideApi: vi.fn().mockResolvedValue(undefined) };
    servoyService = { loaded: vi.fn().mockResolvedValue(undefined) };
    utils = { createJSEvent: vi.fn().mockReturnValue({}) };

    TestBed.configureTestingModule({
      providers: [
        PopupFormService,
        { provide: MainViewRefService, useValue: { mainContainer: { createComponent: vi.fn() } } },
        { provide: FormService, useValue: formService },
        { provide: ServicesService, useValue: servicesService },
        { provide: ServoyService, useValue: servoyService },
        { provide: SvyUtilsService, useValue: utils },
        { provide: DOCUMENT, useValue: document },
      ],
    });
    service = TestBed.inject(PopupFormService);
  });

  afterEach(() => {
    document.body.querySelectorAll('#mainForm').forEach((e) => e.remove());
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  describe('showForm', () => {
    it('should dispatch disableTabseq to a present main form and chain formWillShow/loaded', async () => {
      const mainForm = document.createElement('div');
      mainForm.id = 'mainForm';
      const dispatchSpy = vi.spyOn(mainForm, 'dispatchEvent');
      document.body.appendChild(mainForm);

      service.showForm({ form: 'f1' } as any);
      expect(dispatchSpy).toHaveBeenCalled();
      await Promise.resolve();
      expect(formService.formWillShow).toHaveBeenCalledWith('f1', true);
    });

    it('should not throw when there is no main form', () => {
      expect(() => service.showForm({ form: 'f1' } as any)).not.toThrow();
    });
  });

  describe('cancelFormPopup (boolean)', () => {
    it('should remove the body mouseup listener and skip the clear call when disabling the server call', () => {
      const removeSpy = vi.spyOn(document.body, 'removeEventListener');
      service.cancelFormPopup(true);
      expect(removeSpy).toHaveBeenCalledWith('mouseup', expect.any(Function));
      expect(servicesService.callServiceServerSideApi).not.toHaveBeenCalled();
    });

    it('should call clearPopupForm when not disabling the server call', () => {
      service.cancelFormPopup(false);
      expect(servicesService.callServiceServerSideApi).toHaveBeenCalledWith('window', 'clearPopupForm', []);
    });

    it('should hide and destroy the popup chain when a popup component exists', () => {
      const destroy = vi.fn();
      service.formPopupComponent = { instance: { popup: { form: 'f1', parent: null } }, destroy } as any;
      service.cancelFormPopup(false);
      expect(formService.hideForm).toHaveBeenCalledWith('f1');
      expect(destroy).toHaveBeenCalled();
      expect(service.formPopupComponent).toBeNull();
    });
  });

  describe('cancelFormPopup (string)', () => {
    it('should walk up to the named form and set it as the new current popup', () => {
      const parentComp = { instance: { popup: { form: 'target', parentInstance: null } } } as any;
      const childComp = { instance: { popup: { form: 'child', parentInstance: parentComp } } } as any;
      service.formPopupComponent = childComp;
      const closeSpy = vi.spyOn(service as any, 'closeSiblingsFormPopup').mockImplementation(() => undefined);
      service.cancelFormPopup('target');
      expect(service.formPopupComponent).toBe(parentComp);
      closeSpy.mockRestore();
    });

    it('should be a no-op when there is no popup component', () => {
      service.formPopupComponent = null;
      expect(() => service.cancelFormPopup('target')).not.toThrow();
      expect(service.formPopupComponent).toBeNull();
    });
  });
});
