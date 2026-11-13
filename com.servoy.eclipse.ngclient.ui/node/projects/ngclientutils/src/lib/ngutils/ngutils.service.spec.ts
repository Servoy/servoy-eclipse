import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed, inject } from '@angular/core/testing';

import { NGUtilsService } from './ngutils.service';

import { WindowRefService, ServoyPublicTestingModule, LoggerFactory, ServoyPublicService } from '@servoy/public';

import { PlatformLocation } from '@angular/common';

describe('NGUtilsService', () => {
  let windowRef: any;
  let platformLocation: any;
  let servoyService: any;
  beforeEach(() => {
     windowRef =  {};
     windowRef.nativeWindow = {};
     windowRef.nativeWindow.location = {};
     platformLocation = { onPopState: vi.fn() } as any;
     servoyService = { setFormStyleClasses: vi.fn() };
     // we use a useFactory because when using useValue that will be cloned, so you can adjust windowRef later on.
    TestBed.configureTestingModule({
      providers: [NGUtilsService, {provide: WindowRefService, useFactory:()=> windowRef },
                   LoggerFactory,
                   {provide: ServoyPublicService, useFactory:()=> servoyService },
                  {provide: PlatformLocation, useValue: platformLocation}],
       imports: [ ServoyPublicTestingModule],
    });
  });

  it('should be created', inject([NGUtilsService], (service: NGUtilsService) => {
    expect(service).toBeTruthy();
  }));

  it('should clear the window location hash on construction', inject([NGUtilsService], (service: NGUtilsService) => {
    expect(service).toBeTruthy();
    expect(windowRef.nativeWindow.location.hash).toBe('');
  }));

  it('it should return the user agent', inject([NGUtilsService], (service: NGUtilsService) => {
      windowRef.nativeWindow = {navigator:{userAgent:'test'}};
      expect(service.getUserAgent()).toBe('test'); // test
  }));

  it('it should call the setBackActionCallback method', inject([NGUtilsService], (service: NGUtilsService) => {
      service.backActionCB = '';
      expect(platformLocation.onPopState).toHaveBeenCalled();
  }));

  describe('backActionCB / setBackActionCallback', () => {
    const triggerPopState = () => {
      const popStateHandler = platformLocation.onPopState.mock.calls[0][0] as () => void;
      popStateHandler();
    };

    it('should invoke the callback with the hash when a hash is present', inject([NGUtilsService], (service: NGUtilsService) => {
      platformLocation.hash = '#myform';
      platformLocation.href = 'http://localhost/#myform';
      const cb = vi.fn();
      service.backActionCB = cb;

      triggerPopState();

      expect(cb).toHaveBeenCalledWith('#myform');
    }));

    it('should force a forward navigation when there is no hash and the url ends with /index.html', inject([NGUtilsService], (service: NGUtilsService) => {
      platformLocation.hash = '';
      platformLocation.href = 'http://localhost/index.html';
      platformLocation.forward = vi.fn();
      const cb = vi.fn();
      service.backActionCB = cb;

      triggerPopState();

      expect(platformLocation.forward).toHaveBeenCalled();
      expect(cb).not.toHaveBeenCalled();
    }));

    it('should do nothing in the pop handler when no callback is registered', inject([NGUtilsService], (service: NGUtilsService) => {
      platformLocation.hash = '#x';
      platformLocation.href = 'http://localhost/#x';
      platformLocation.forward = vi.fn();
      // call setBackActionCallback directly without assigning a callback
      service.setBackActionCallback();

      triggerPopState();

      expect(platformLocation.forward).not.toHaveBeenCalled();
    }));
  });

  describe('styleclasses', () => {
    it('should set the styleclasses and forward them to the servoy service', inject([NGUtilsService], (service: NGUtilsService) => {
      const styleclasses = { property: 'myForm css-class' } as any;
      service.styleclasses = styleclasses;

      expect(service.styleclasses).toBe(styleclasses);
      expect(servoyService.setFormStyleClasses).toHaveBeenCalledWith(styleclasses);
    }));
  });

  describe('contributedTags', () => {
    afterEach(() => {
      document.querySelectorAll('head > [hhsManagedTag]').forEach(e => e.remove());
    });

    it('should append the contributed tags into the document head and expose them via the getter', inject([NGUtilsService], (service: NGUtilsService) => {
      const tags = [{ tagName: 'meta', attrs: [{ name: 'name', value: 'viewport' }, { name: 'content', value: 'width=device-width' }] }] as any;
      service.contributedTags = tags;

      const added = document.querySelectorAll('head > meta[hhsManagedTag]');
      expect(added.length).toBe(1);
      expect(added[0].getAttribute('name')).toBe('viewport');
      expect(added[0].getAttribute('content')).toBe('width=device-width');
      expect(service.contributedTags).toBe(tags);
    }));

    it('should leave an unchanged tag in place across a second assignment', inject([NGUtilsService], (service: NGUtilsService) => {
      const tags = [{ tagName: 'meta', attrs: [{ name: 'name', value: 'viewport' }] }] as any;
      service.contributedTags = tags;
      const firstElement = document.querySelector('head > meta[hhsManagedTag]');

      // assign an equal (but new) tags array - the matching tag should not be re-created
      service.contributedTags = [{ tagName: 'meta', attrs: [{ name: 'name', value: 'viewport' }] }] as any;

      const after = document.querySelectorAll('head > meta[hhsManagedTag]');
      expect(after.length).toBe(1);
      expect(after[0]).toBe(firstElement);
    }));

    it('should remove a previously added tag that is no longer contributed', inject([NGUtilsService], (service: NGUtilsService) => {
      service.contributedTags = [{ tagName: 'meta', attrs: [{ name: 'name', value: 'viewport' }] }] as any;
      expect(document.querySelectorAll('head > meta[hhsManagedTag]').length).toBe(1);

      service.contributedTags = [] as any;

      expect(document.querySelectorAll('head > meta[hhsManagedTag]').length).toBe(0);
    }));

    it('should replace a changed tag (different attribute value)', inject([NGUtilsService], (service: NGUtilsService) => {
      service.contributedTags = [{ tagName: 'meta', attrs: [{ name: 'name', value: 'viewport' }] }] as any;
      service.contributedTags = [{ tagName: 'meta', attrs: [{ name: 'name', value: 'description' }] }] as any;

      const after = document.querySelectorAll('head > meta[hhsManagedTag]');
      expect(after.length).toBe(1);
      expect(after[0].getAttribute('name')).toBe('description');
    }));
  });

  describe('unload confirmation', () => {
    let addEventListener: ReturnType<typeof vi.fn>;
    let removeEventListener: ReturnType<typeof vi.fn>;
    beforeEach(() => {
      addEventListener = vi.fn();
      removeEventListener = vi.fn();
      windowRef.nativeWindow.window = { addEventListener, removeEventListener };
    });

    it('setOnUnloadConfirmationMessage should add a beforeunload listener when a message is given', inject([NGUtilsService], (service: NGUtilsService) => {
      service.setOnUnloadConfirmationMessage('unsaved');
      expect(addEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
      expect(removeEventListener).not.toHaveBeenCalled();
    }));

    it('setOnUnloadConfirmationMessage should remove the beforeunload listener when the message is empty', inject([NGUtilsService], (service: NGUtilsService) => {
      service.setOnUnloadConfirmationMessage(null as any);
      expect(removeEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
      expect(addEventListener).not.toHaveBeenCalled();
    }));

    it('setOnUnloadConfirmation should add the beforeunload listener when confirmation is enabled', inject([NGUtilsService], (service: NGUtilsService) => {
      service.setOnUnloadConfirmation(true);
      expect(addEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
    }));

    it('setOnUnloadConfirmation should remove the beforeunload listener when confirmation is disabled', inject([NGUtilsService], (service: NGUtilsService) => {
      service.setOnUnloadConfirmation(false);
      expect(removeEventListener).toHaveBeenCalledWith('beforeunload', expect.any(Function));
    }));
  });

  describe('server-side no-op stubs', () => {
    it('should not throw when calling the server-implemented stubs', inject([NGUtilsService], (service: NGUtilsService) => {
      expect(() => service.setViewportMetaDefaultForMobileAwareSites()).not.toThrow();
      expect(() => service.setViewportMetaForMobileAwareSites(1)).not.toThrow();
      expect(() => service.replaceHeaderTag('meta', 'name', 'viewport', 'newTag')).not.toThrow();
      expect(() => service.addFormStyleClass('form', 'cls')).not.toThrow();
      expect(() => service.getFormStyleClass('form')).not.toThrow();
      expect(() => service.removeFormStyleClass('form', 'cls')).not.toThrow();
    }));
  });

  describe('getAbsoluteLocation', () => {
    afterEach(() => {
      document.querySelectorAll('[data-ngutils-test]').forEach(e => e.remove());
    });

    it('should return the element location offset by the window scroll position', inject([NGUtilsService], (service: NGUtilsService) => {
      const el = document.createElement('div');
      el.id = 'myComp';
      el.setAttribute('data-ngutils-test', '');
      document.body.appendChild(el);
      vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({ left: 10, top: 20 } as DOMRect);
      windowRef.nativeWindow.scrollX = 5;
      windowRef.nativeWindow.scrollY = 7;

      expect(service.getAbsoluteLocation('myComp')).toEqual({ x: 15, y: 27 });
    }));

    it('should return null when the element is not found', inject([NGUtilsService], (service: NGUtilsService) => {
      expect(service.getAbsoluteLocation('doesNotExist')).toBeNull();
    }));
  });

  describe('setLangAttribute', () => {
    it('should set the lang attribute on the html element', inject([NGUtilsService], (service: NGUtilsService) => {
      service.setLangAttribute('nl');
      expect(document.querySelector('html')!.getAttribute('lang')).toBe('nl');
    }));
  });

  describe('printDocument', () => {
    let appendChild: any;
    let originalCreateElement: typeof document.createElement;
    afterEach(() => {
      if (originalCreateElement) document.createElement = originalCreateElement;
      if (appendChild) appendChild.mockRestore();
    });

    it('should create a hidden iframe, append it and set its src, printing on load', inject([NGUtilsService], (service: NGUtilsService) => {
      const contentWindow = { focus: vi.fn(), print: vi.fn() };
      const iframe: any = { style: {}, contentWindow };
      originalCreateElement = document.createElement.bind(document);
      vi.spyOn(document, 'createElement').mockImplementation(((tag: string) => {
        if (tag === 'iframe') return iframe;
        return originalCreateElement(tag);
      }) as any);
      appendChild = vi.spyOn(document.body, 'appendChild').mockImplementation(((node: any) => node) as any);

      service.printDocument('http://localhost/doc.pdf');

      expect(iframe.style.visibility).toBe('hidden');
      expect(appendChild).toHaveBeenCalledWith(iframe);
      expect(iframe.src).toBe('http://localhost/doc.pdf');

      iframe.onload();
      expect(contentWindow.focus).toHaveBeenCalled();
      expect(contentWindow.print).toHaveBeenCalled();
    }));
  });

  describe('scrollIntoView', () => {
    afterEach(() => {
      document.querySelectorAll('[data-ngutils-test]').forEach(e => e.remove());
    });

    it('should call scrollIntoView with the given options when the anchor exists', inject([NGUtilsService], (service: NGUtilsService) => {
      const el = document.createElement('div');
      el.className = 'anchor-target';
      el.setAttribute('data-ngutils-test', '');
      document.body.appendChild(el);
      const scrollIntoView = vi.fn();
      el.scrollIntoView = scrollIntoView;

      const options = { behavior: 'auto' };
      service.scrollIntoView('.anchor-target', options);

      expect(scrollIntoView).toHaveBeenCalledWith(options);
    }));

    it('should use default options when none are passed', inject([NGUtilsService], (service: NGUtilsService) => {
      const el = document.createElement('div');
      el.className = 'anchor-target';
      el.setAttribute('data-ngutils-test', '');
      document.body.appendChild(el);
      const scrollIntoView = vi.fn();
      el.scrollIntoView = scrollIntoView;

      service.scrollIntoView('.anchor-target');

      expect(scrollIntoView).toHaveBeenCalledWith({ behavior: 'smooth', block: 'start', inline: 'nearest' });
    }));

    it('should log and not throw when the anchor is missing', inject([NGUtilsService], (service: NGUtilsService) => {
      const log = vi.spyOn(console, 'log').mockImplementation(() => undefined);
      expect(() => service.scrollIntoView('.does-not-exist')).not.toThrow();
      expect(log).toHaveBeenCalled();
      log.mockRestore();
    }));
  });

  describe('scrollToTop', () => {
    let scrollTo: any;
    afterEach(() => {
      document.querySelectorAll('[data-ngutils-test]').forEach(e => e.remove());
      if (scrollTo) scrollTo.mockRestore();
    });

    it('should scroll the window to the container top position', inject([NGUtilsService], (service: NGUtilsService) => {
      const el = document.createElement('div');
      el.className = 'scroll-target';
      el.setAttribute('data-ngutils-test', '');
      document.body.appendChild(el);
      vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({ top: 100 } as DOMRect);
      scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);

      service.scrollToTop('.scroll-target');

      expect(scrollTo).toHaveBeenCalledWith({ top: 100 + window.scrollY, behavior: 'smooth' });
    }));

    it('should warn and not scroll when the container is missing', inject([NGUtilsService], (service: NGUtilsService) => {
      const warn = vi.spyOn(console, 'warn').mockImplementation(() => undefined);
      scrollTo = vi.spyOn(window, 'scrollTo').mockImplementation(() => undefined);

      service.scrollToTop('.missing');

      expect(warn).toHaveBeenCalled();
      expect(scrollTo).not.toHaveBeenCalled();
      warn.mockRestore();
    }));
  });

  describe('add/remove class on DOM element', () => {
    afterEach(() => {
      document.querySelectorAll('[data-ngutils-test]').forEach(e => e.remove());
    });

    it('addClassToDOMElement should add the class to all matching elements', inject([NGUtilsService], (service: NGUtilsService) => {
      const el1 = document.createElement('div');
      const el2 = document.createElement('div');
      [el1, el2].forEach(e => { e.className = 'target'; e.setAttribute('data-ngutils-test', ''); document.body.appendChild(e); });

      service.addClassToDOMElement('.target', 'added');

      expect(el1.classList.contains('added')).toBe(true);
      expect(el2.classList.contains('added')).toBe(true);
    }));

    it('removeClassFromDOMElement should remove the class from all matching elements', inject([NGUtilsService], (service: NGUtilsService) => {
      const el1 = document.createElement('div');
      const el2 = document.createElement('div');
      [el1, el2].forEach(e => { e.className = 'target added'; e.setAttribute('data-ngutils-test', ''); document.body.appendChild(e); });

      service.removeClassFromDOMElement('.target', 'added');

      expect(el1.classList.contains('added')).toBe(false);
      expect(el2.classList.contains('added')).toBe(false);
    }));
  });

  describe('removeArguments', () => {
    it('should replace the history state with the pathname + hash', inject([NGUtilsService], (service: NGUtilsService) => {
      const replaceState = vi.fn();
      windowRef.nativeWindow.history = { replaceState };
      windowRef.nativeWindow.location = { pathname: '/solution/', hash: '#form' };

      service.removeArguments();

      expect(replaceState).toHaveBeenCalledWith({}, '', '/solution/#form');
    }));
  });
});
