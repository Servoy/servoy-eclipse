import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { PopupFormService } from './popupform.service';
import { FormService } from '../form.service';
import { ServicesService } from '../../sablo/services.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { MainViewRefService, PopupStateService } from '@servoy/public';
import { WindowService } from './window.service';

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
    window.removeEventListener('keyup', (service as any).formPopupKeyListener, true);
    document.body.removeEventListener('mouseup', (service as any).formPopupBodyListener);
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

    describe('allowlisted areas (SVY-21789)', () => {
      let attached: Node[];
      let cancelSpy: any;

      const attach = (className: string, parent: Node = document.body) => {
        const el = document.createElement('div');
        el.className = className;
        parent.appendChild(el);
        attached.push(el);
        return el;
      };

      beforeEach(() => {
        attached = [];
        service.formPopupComponent = { instance: { _left: 11, _top: 22, popup: {} } } as any;
        cancelSpy = vi.spyOn(service, 'cancelFormPopup').mockImplementation(() => undefined);
      });

      afterEach(() => {
        attached.forEach((n) => n.parentNode?.removeChild(n));
        vi.restoreAllMocks();
      });

      it('cancels the popup when the click falls inside an open dialog', () => {
        const dialog = attach('svy-dialog');
        const inner = attach('dialog-content', dialog);

        invoke({ target: inner });

        expect(cancelSpy).toHaveBeenCalledTimes(1);
        expect(cancelSpy).toHaveBeenCalledWith(false);
      });

      it('records clickedComponentId, x and y before cancelling for a click on the opener inside the main form', () => {
        const mainWindow = attach('svy-main-window-container');
        const opener = attach('opener', mainWindow);
        opener.id = 'openerButton';

        invoke({ target: opener });

        expect(service.clickedComponentId).toBe('openerButton');
        expect(service.x).toBe(11);
        expect(service.y).toBe(22);
        expect(cancelSpy).toHaveBeenCalledWith(false);
      });

      it('never cancels for doNotCloseOnClickOutside, even for a click inside the main form', () => {
        service.formPopupComponent = { instance: { _left: 0, _top: 0, popup: { doNotCloseOnClickOutside: true } } } as any;
        const mainWindow = attach('svy-main-window-container');

        invoke({ target: attach('inner', mainWindow) });

        expect(cancelSpy).not.toHaveBeenCalled();
      });
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

  describe('Escape key handling (SVY-21789)', () => {
    let popupStateService: PopupStateService;
    let cancelSpy: any;

    const pressKey = (key: string) => window.dispatchEvent(new KeyboardEvent('keyup', { key }));

    const show = async (popup: any) => {
      await (service as any).showPopup(popup);
    };

    beforeEach(() => {
      vi.useFakeTimers();
      popupStateService = TestBed.inject(PopupStateService);
      (service as any).mainViewRefService.mainContainer.createComponent = vi.fn().mockImplementation(() => {
        const ref: any = { instance: { popup: null, _left: 0, _top: 0 }, destroy: vi.fn() };
        ref.instance.setPopupForm = vi.fn().mockImplementation((p: any) => {
          ref.instance.popup = p;
        });
        return ref;
      });
      cancelSpy = vi.spyOn(service, 'cancelFormPopup');
    });

    afterEach(() => {
      vi.runOnlyPendingTimers();
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    it('calls cancelFormPopup(false) when Escape is released while a popup is shown', async () => {
      await show({ form: 'f1' });

      pressKey('Escape');

      expect(cancelSpy).toHaveBeenCalledTimes(1);
      expect(cancelSpy).toHaveBeenCalledWith(false);
      expect(servicesService.callServiceServerSideApi).toHaveBeenCalledWith('window', 'clearPopupForm', []);
      expect(service.formPopupComponent).toBeNull();
    });

    it('ignores keys other than Escape', async () => {
      await show({ form: 'f1' });

      pressKey('Enter');
      pressKey('a');

      expect(cancelSpy).not.toHaveBeenCalled();
      expect(service.formPopupComponent).not.toBeNull();
    });

    it('ignores Escape when the popup has doNotCloseOnClickOutside', async () => {
      await show({ form: 'f1', doNotCloseOnClickOutside: true });

      pressKey('Escape');

      expect(cancelSpy).not.toHaveBeenCalled();
      expect(service.formPopupComponent).not.toBeNull();
    });

    it('does not handle Escape after the popup was cancelled and removes the window keyup listener', async () => {
      const removeSpy = vi.spyOn(window, 'removeEventListener');
      await show({ form: 'f1' });
      service.cancelFormPopup(true);

      expect(removeSpy).toHaveBeenCalledWith('keyup', expect.any(Function), true);

      cancelSpy.mockClear();
      service.formPopupComponent = { instance: { popup: { form: 'other' } }, destroy: vi.fn() } as any;
      pressKey('Escape');

      expect(cancelSpy).not.toHaveBeenCalled();
    });

    it('activates the popup state when the popup is shown and deactivates it when cancelled', async () => {
      const activateSpy = vi.spyOn(popupStateService, 'activatePopup');
      const deactivateSpy = vi.spyOn(popupStateService, 'deactivatePopup');

      await show({ form: 'f1' });
      expect(activateSpy).toHaveBeenCalledWith('formpopup');
      expect(deactivateSpy).not.toHaveBeenCalled();

      service.cancelFormPopup(false);
      expect(deactivateSpy).toHaveBeenCalledWith('formpopup');
    });

    it('keeps the popup state active right after Escape and releases it after the 200 ms delay', async () => {
      await show({ form: 'f1' });
      expect(popupStateService.isAnyPopupActive()).toBe(true);

      pressKey('Escape');

      expect(service.formPopupComponent).toBeNull();
      expect(popupStateService.isAnyPopupActive()).toBe(true);
      vi.advanceTimersByTime(199);
      expect(popupStateService.isAnyPopupActive()).toBe(true);
      vi.advanceTimersByTime(1);
      expect(popupStateService.isAnyPopupActive()).toBe(false);
    });

    it('closes the whole chain with a single cancel when two nested popups are shown and Escape is pressed once', async () => {
      await show({ form: 'p1' });
      await show({ form: 'p2', parent: { form: 'p1' } });
      const child = service.formPopupComponent as any;
      const root = child.instance.popup.parentInstance;
      expect(root.instance.popup.form).toBe('p1');

      pressKey('Escape');

      expect(cancelSpy).toHaveBeenCalledTimes(1);
      expect(formService.hideForm).toHaveBeenCalledWith('p2');
      expect(formService.hideForm).toHaveBeenCalledWith('p1');
      expect(child.destroy).toHaveBeenCalledTimes(1);
      expect(root.destroy).toHaveBeenCalledTimes(1);
      expect(service.formPopupComponent).toBeNull();
    });

    it('closes the popup when Escape is released on an element inside the page', async () => {
      await show({ form: 'f1' });
      const input = document.createElement('input');
      document.body.appendChild(input);

      try {
        input.dispatchEvent(new KeyboardEvent('keyup', { key: 'Escape', bubbles: true }));
      } finally {
        input.remove();
      }

      expect(cancelSpy).toHaveBeenCalledTimes(1);
      expect(cancelSpy).toHaveBeenCalledWith(false);
      expect(service.formPopupComponent).toBeNull();
    });

    it('ignores the keydown of Escape, so only the keyup that the dialog handler also listens to closes the popup', async () => {
      await show({ form: 'f1' });

      window.dispatchEvent(new KeyboardEvent('keydown', { key: 'Escape' }));

      expect(cancelSpy).not.toHaveBeenCalled();
      expect(service.formPopupComponent).not.toBeNull();
    });

    it('registers the very same capture keyup handler for every popup of a chain', async () => {
      const addSpy = vi.spyOn(window, 'addEventListener');

      await show({ form: 'p1' });
      await show({ form: 'p2', parent: { form: 'p1' } });

      const keyupCalls = addSpy.mock.calls.filter((call) => call[0] === 'keyup');
      expect(keyupCalls.length).toBe(2);
      expect(keyupCalls.every((call) => call[1] === (service as any).formPopupKeyListener && call[2] === true)).toBe(true);
    });

    it('closes again on Escape after the popup was closed and shown again', async () => {
      await show({ form: 'f1' });
      pressKey('Escape');
      expect(service.formPopupComponent).toBeNull();

      await show({ form: 'f2' });
      expect(service.formPopupComponent).not.toBeNull();
      pressKey('Escape');

      expect(cancelSpy).toHaveBeenCalledTimes(2);
      expect(service.formPopupComponent).toBeNull();
    });

    it('removes the key listener and deactivates the popup state even when no popup component exists', () => {
      const removeSpy = vi.spyOn(window, 'removeEventListener');
      const deactivateSpy = vi.spyOn(popupStateService, 'deactivatePopup');
      service.formPopupComponent = null;

      service.cancelFormPopup(true);

      expect(removeSpy).toHaveBeenCalledWith('keyup', (service as any).formPopupKeyListener, true);
      expect(deactivateSpy).toHaveBeenCalledWith('formpopup');
    });

    describe('popup over a dialog that closes on Escape', () => {
      let dialogCancel: ReturnType<typeof vi.fn>;
      let dialogListener: (event: KeyboardEvent) => void;

      beforeEach(() => {
        dialogCancel = vi.fn();
        const dialog = { bsWindowInstance: {}, componentRef: { instance: { cancel: dialogCancel } } } as any;
        const windowServiceContext = { popupStateService } as any;
        dialogListener = (event: KeyboardEvent) => WindowService.prototype.handleEscapeKey.call(windowServiceContext, event, dialog);
        window.addEventListener('keyup', dialogListener, true);
      });

      afterEach(() => {
        window.removeEventListener('keyup', dialogListener, true);
      });

      it('closes the dialog on Escape when no form popup is shown', () => {
        pressKey('Escape');

        expect(dialogCancel).toHaveBeenCalledTimes(1);
      });

      it('closes only the popup on the first Escape and keeps the dialog open', async () => {
        await show({ form: 'f1' });

        pressKey('Escape');

        expect(cancelSpy).toHaveBeenCalledTimes(1);
        expect(service.formPopupComponent).toBeNull();
        expect(dialogCancel).not.toHaveBeenCalled();
      });

      it('keeps the dialog open for a second Escape within 200 ms and closes it with an Escape after that', async () => {
        await show({ form: 'f1' });
        pressKey('Escape');

        vi.advanceTimersByTime(100);
        pressKey('Escape');
        expect(dialogCancel).not.toHaveBeenCalled();

        vi.advanceTimersByTime(100);
        pressKey('Escape');
        expect(dialogCancel).toHaveBeenCalledTimes(1);
        expect(cancelSpy).toHaveBeenCalledTimes(1);
      });

      it('keeps the dialog open when the popup listener was registered before the dialog listener', async () => {
        window.removeEventListener('keyup', dialogListener, true);
        await show({ form: 'f1' });
        window.addEventListener('keyup', dialogListener, true);

        pressKey('Escape');

        expect(service.formPopupComponent).toBeNull();
        expect(dialogCancel).not.toHaveBeenCalled();
      });

      it('keeps both open on Escape when the popup has doNotCloseOnClickOutside', async () => {
        await show({ form: 'f1', doNotCloseOnClickOutside: true });

        pressKey('Escape');

        expect(service.formPopupComponent).not.toBeNull();
        expect(dialogCancel).not.toHaveBeenCalled();
      });
    });

    describe('cancelFormPopup(string)', () => {
      it('removes the key listener and deactivates the popup state when no popup remains', () => {
        const removeSpy = vi.spyOn(window, 'removeEventListener');
        const deactivateSpy = vi.spyOn(popupStateService, 'deactivatePopup');
        service.formPopupComponent = { instance: { popup: { form: 'child', parentInstance: null } } } as any;

        service.cancelFormPopup('unknown');

        expect(service.formPopupComponent).toBeNull();
        expect(removeSpy).toHaveBeenCalledWith('keyup', expect.any(Function), true);
        expect(deactivateSpy).toHaveBeenCalledWith('formpopup');
      });

      it('keeps the key listener and popup state when a parent popup remains', () => {
        const removeSpy = vi.spyOn(window, 'removeEventListener');
        const deactivateSpy = vi.spyOn(popupStateService, 'deactivatePopup');
        const parentComp = { instance: { popup: { form: 'target', parentInstance: null } } } as any;
        service.formPopupComponent = { instance: { popup: { form: 'child', parentInstance: parentComp } } } as any;
        vi.spyOn(service as any, 'closeSiblingsFormPopup').mockImplementation(() => undefined);

        service.cancelFormPopup('target');

        expect(service.formPopupComponent).toBe(parentComp);
        expect(removeSpy).not.toHaveBeenCalledWith('keyup', expect.any(Function), true);
        expect(deactivateSpy).not.toHaveBeenCalled();
      });
    });
  });

  describe('registered mouseup listener (SVY-21789)', () => {
    let cancelSpy: any;
    let attached: Node[];
    let mainArea: HTMLElement;
    let sequenceDuringSetPopup: boolean[];

    const attach = (tag: string, id?: string, parent: Node = document.body) => {
      const el = document.createElement(tag);
      if (id) el.id = id;
      parent.appendChild(el);
      attached.push(el);
      return el;
    };
    const release = (target: Element) => target.dispatchEvent(new MouseEvent('mouseup', { bubbles: true }));
    const show = async (popup: any) => {
      await (service as any).showPopup(popup);
    };

    beforeEach(() => {
      vi.useFakeTimers();
      attached = [];
      mainArea = attach('div');
      mainArea.className = 'svy-main-window-container';
      sequenceDuringSetPopup = [];
      (service as any).mainViewRefService.mainContainer.createComponent = vi.fn().mockImplementation(() => {
        const ref: any = { instance: { popup: null, _left: 5, _top: 6 }, destroy: vi.fn() };
        ref.instance.setPopupForm = vi.fn().mockImplementation((p: any) => {
          ref.instance.popup = p;
          sequenceDuringSetPopup.push(service.sequencePopup);
        });
        return ref;
      });
      cancelSpy = vi.spyOn(service, 'cancelFormPopup');
    });

    afterEach(() => {
      attached.forEach((n) => n.parentNode?.removeChild(n));
      vi.runOnlyPendingTimers();
      vi.useRealTimers();
      vi.restoreAllMocks();
    });

    it('ignores a mouseup released before the 300 ms registration delay has passed', async () => {
      await show({ form: 'f1' });
      const outside = attach('div', undefined, mainArea);

      vi.advanceTimersByTime(299);
      release(outside);
      expect(cancelSpy).not.toHaveBeenCalled();
      expect(service.formPopupComponent).not.toBeNull();

      vi.advanceTimersByTime(1);
      release(outside);
      expect(cancelSpy).toHaveBeenCalledTimes(1);
      expect(cancelSpy).toHaveBeenCalledWith(false);
      expect(service.formPopupComponent).toBeNull();
    });

    it('keeps the popup on a mouseup inside the popup element and closes it on one in the main form', async () => {
      await show({ form: 'f1' });
      vi.advanceTimersByTime(300);
      const popupEl = attach('div', 'formpopup');
      const inner = document.createElement('button');
      popupEl.appendChild(inner);
      const outside = attach('div', undefined, mainArea);

      release(inner);
      expect(cancelSpy).not.toHaveBeenCalled();
      expect(service.formPopupComponent).not.toBeNull();

      release(outside);
      expect(cancelSpy).toHaveBeenCalledTimes(1);
      expect(service.formPopupComponent).toBeNull();
    });

    it('stops listening once the popup was closed by a click', async () => {
      await show({ form: 'f1' });
      vi.advanceTimersByTime(300);
      const outside = attach('div', undefined, mainArea);

      release(outside);
      release(outside);

      expect(cancelSpy).toHaveBeenCalledTimes(1);
    });

    it('does not close on a mouseup in the main form when the popup has doNotCloseOnClickOutside', async () => {
      await show({ form: 'f1', doNotCloseOnClickOutside: true });
      vi.advanceTimersByTime(300);

      release(attach('div', undefined, mainArea));

      expect(cancelSpy).not.toHaveBeenCalled();
      expect(service.formPopupComponent).not.toBeNull();
    });

    it('marks the re-show as a sequence popup when the closing click was on the button that opened the popup', async () => {
      attach('button', 'openerBtn', mainArea);
      await show({ form: 'f1', component: 'openerBtn' });
      vi.advanceTimersByTime(300);

      release(document.getElementById('openerBtn')!);

      expect(service.clickedComponentId).toBe('openerBtn');
      expect(service.x).toBe(5);
      expect(service.y).toBe(6);
      expect(service.formPopupComponent).toBeNull();

      await show({ form: 'f1', component: 'openerBtn' });

      expect(sequenceDuringSetPopup).toEqual([false, true]);
      expect(service.sequencePopup).toBe(false);
      expect(service.formPopupComponent).not.toBeNull();
    });

    it('does not mark the re-show as a sequence popup when the closing click was on another element', async () => {
      attach('button', 'openerBtn', mainArea);
      const other = attach('button', 'otherBtn', mainArea);
      await show({ form: 'f1', component: 'openerBtn' });
      vi.advanceTimersByTime(300);

      release(other);

      expect(service.clickedComponentId).toBe('otherBtn');
      expect(service.formPopupComponent).toBeNull();

      await show({ form: 'f1', component: 'openerBtn' });

      expect(sequenceDuringSetPopup).toEqual([false, false]);
    });
  });
});
