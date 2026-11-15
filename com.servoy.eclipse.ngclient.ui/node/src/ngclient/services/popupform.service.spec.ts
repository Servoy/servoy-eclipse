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

  describe('hidePopupForm (private)', () => {
    it('hides the form and does not call the close api when the popup has no onClose', () => {
      (service as any).hidePopupForm({ form: 'f1' });
      expect(formService.hideForm).toHaveBeenCalledWith('f1');
      expect(servicesService.callServiceServerSideApi).not.toHaveBeenCalled();
    });

    it('builds a popupClose JSEvent and calls formPopupClosed when the popup has an onClose', () => {
      const formpopup = document.createElement('div');
      formpopup.id = 'formpopup';
      document.body.appendChild(formpopup);
      utils.createJSEvent.mockReturnValue({});

      (service as any).hidePopupForm({ form: 'f1', onClose: { formname: 'cbForm' } });

      expect(formService.hideForm).toHaveBeenCalledWith('f1');
      expect(utils.createJSEvent).toHaveBeenCalledWith({ target: formpopup }, 'popupClose');
      expect(servicesService.callServiceServerSideApi).toHaveBeenCalledWith('window', 'formPopupClosed', [{ formName: 'cbForm' }]);
      formpopup.remove();
    });
  });

  describe('closeSiblingsFormPopup (private)', () => {
    it('hides the sibling popup form and destroys its component ref', () => {
      const destroy = vi.fn();
      const frmRef = { instance: { popup: { form: 'sibling' } }, destroy } as any;

      (service as any).closeSiblingsFormPopup(frmRef);

      expect(formService.hideForm).toHaveBeenCalledWith('sibling');
      expect(destroy).toHaveBeenCalled();
    });
  });

  describe('formPopupBodyListener (outside-click handling)', () => {
    // The listener is a private arrow field; invoke it directly.
    const invoke = (event: any) => (service as any).formPopupBodyListener(event);

    it('records the clicked component id and the current popup position', () => {
      service.formPopupComponent = { instance: { _left: 42, _top: 24, popup: {} } } as any;
      const target = document.createElement('div');
      target.id = 'someComp';

      invoke({ target });

      expect(service.clickedComponentId).toBe('someComp');
      expect(service.x).toBe(42);
      expect(service.y).toBe(24);
    });

    it('clears the clicked component id when the target has no id', () => {
      service.formPopupComponent = { instance: { _left: 0, _top: 0, popup: {} } } as any;
      invoke({ target: document.createElement('div') });
      expect(service.clickedComponentId).toBeNull();
    });

    it('does nothing further when the popup has doNotCloseOnClickOutside', () => {
      service.formPopupComponent = { instance: { _left: 0, _top: 0, popup: { doNotCloseOnClickOutside: true } } } as any;
      const cancelSpy = vi.spyOn(service, 'cancelFormPopup');

      invoke({ target: document.createElement('div') });

      expect(cancelSpy).not.toHaveBeenCalled();
      cancelSpy.mockRestore();
    });

    it('cancels the popup when the backdrop itself is clicked', () => {
      const backdrop = document.createElement('div');
      backdrop.className = 'formpopup-backdrop';
      document.body.appendChild(backdrop);
      service.formPopupComponent = { instance: { _left: 0, _top: 0, popup: {} } } as any;
      const cancelSpy = vi.spyOn(service, 'cancelFormPopup').mockImplementation(() => undefined);

      invoke({ target: backdrop });

      expect(cancelSpy).toHaveBeenCalledWith(false);
      cancelSpy.mockRestore();
      backdrop.remove();
    });

    it('cancels the popup when the click falls inside the main window container', () => {
      const mainWindow = document.createElement('div');
      mainWindow.className = 'svy-main-window-container';
      const inner = document.createElement('span');
      mainWindow.appendChild(inner);
      document.body.appendChild(mainWindow);
      service.formPopupComponent = { instance: { _left: 0, _top: 0, popup: {} } } as any;
      const cancelSpy = vi.spyOn(service, 'cancelFormPopup').mockImplementation(() => undefined);

      invoke({ target: inner });

      expect(cancelSpy).toHaveBeenCalledWith(false);
      cancelSpy.mockRestore();
      mainWindow.remove();
    });
  });

  describe('showForm -> showPopup', () => {
    it('creates the popup component and registers the body mouseup listener (via fake timers)', async () => {
      vi.useFakeTimers();
      try {
        const setPopupForm = vi.fn();
        const createComponent = vi.fn().mockReturnValue({ instance: { setPopupForm, popup: {} } });
        (service as any).mainViewRefService.mainContainer.createComponent = createComponent;
        const addSpy = vi.spyOn(document.body, 'addEventListener');

        await (service as any).showPopup({ form: 'f1', component: null });

        expect(createComponent).toHaveBeenCalled();
        expect(setPopupForm).toHaveBeenCalledWith({ form: 'f1', component: null });

        vi.advanceTimersByTime(300);
        expect(addSpy).toHaveBeenCalledWith('mouseup', expect.any(Function));
      } finally {
        vi.useRealTimers();
      }
    });

    it('does not reshow when a popup is already open without a parent', async () => {
      const createComponent = vi.fn();
      (service as any).mainViewRefService.mainContainer.createComponent = createComponent;
      service.formPopupComponent = { instance: { popup: { form: 'existing' } } } as any;

      await (service as any).showPopup({ form: 'other' });

      expect(createComponent).not.toHaveBeenCalled();
    });

    it('clears the popup form on the server when a child popup arrives with no open parent popup', async () => {
      service.formPopupComponent = null;
      await (service as any).showPopup({ form: 'child', parent: { form: 'p' } });
      expect(servicesService.callServiceServerSideApi).toHaveBeenCalledWith('window', 'clearPopupForm', []);
    });
  });
});
