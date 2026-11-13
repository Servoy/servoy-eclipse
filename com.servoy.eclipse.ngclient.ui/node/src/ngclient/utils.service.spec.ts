import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { DOCUMENT } from '@angular/core';

import { SvyUtilsService } from './utils.service';
import { LoggerFactory } from '@servoy/public';

describe('SvyUtilsService', () => {
  let service: SvyUtilsService;
  let originalQuerySelector: typeof document.querySelector;

  const makeElement = (tagName: string, attrs: Record<string, string> = {}, parent?: any): any => {
    const el: any = {
      tagName: tagName.toUpperCase(),
      getAttribute: (name: string) => attrs[name] ?? null,
      parentElement: parent ?? null,
    };
    return el;
  };

  beforeEach(() => {
    originalQuerySelector = document.querySelector;
    TestBed.configureTestingModule({
      providers: [SvyUtilsService, LoggerFactory, { provide: DOCUMENT, useValue: document }],
    });
    service = TestBed.inject(SvyUtilsService);
  });

  afterEach(() => {
    document.querySelector = originalQuerySelector;
  });

  describe('createJSEvent', () => {
    it('should return a default event when the event is undefined and no context filter', () => {
      const result = service.createJSEvent(null!, 'onClick');
      expect(result).toEqual(expect.objectContaining({ svyType: 'JSEvent', eventType: 'onClick' }));
      expect(result!.timestamp).toBeDefined();
    });

    it('should return null when the event is undefined but a context filter is given', () => {
      expect(service.createJSEvent(null!, 'onClick', 'formA')).toBeNull();
    });

    it('should build an event when walking up finds a matching svy-form', () => {
      const form = makeElement('svy-form', { name: 'formA' });
      const target = makeElement('div', { name: 'myComp' }, form);
      const event: any = { target, altKey: true, shiftKey: true, ctrlKey: false, metaKey: false, pageX: 5, pageY: 6, detail: { foo: 'bar' } };

      const result = service.createJSEvent(event, 'onClick');

      expect(result).not.toBeNull();
      expect(result!.formName).toBe('formA');
      expect(result!.modifiers).toBe(8 | 1);
      expect(result!.x).toBe(5);
      expect(result!.y).toBe(6);
      expect(result!.data).toEqual({ foo: 'bar' });
    });

    it('should return null when the context filter does not match the form', () => {
      const form = makeElement('svy-form', { name: 'formA' });
      const target = makeElement('div', {}, form);
      const event: any = { target };
      expect(service.createJSEvent(event, 'onClick', 'otherForm')).toBeNull();
    });

    it('should match body when no context filter is set and no form is found', () => {
      const body = makeElement('body', {});
      const target = makeElement('div', {}, body);
      const event: any = { target };
      const result = service.createJSEvent(event, 'onClick');
      expect(result).not.toBeNull();
      expect(result!.formName).toBeUndefined();
    });

    it('should look up an active dialog form when the walk finds no form', () => {
      const body = makeElement('body', {});
      const target = makeElement('div', {}, body);
      const event: any = { target };

      document.querySelector = vi.fn().mockReturnValue({
        querySelector: () => ({ getAttribute: () => 'dialogForm' }),
      }) as any;

      const result = service.createJSEvent(event, 'onClick');
      expect(result).not.toBeNull();
      expect(result!.formName).toBe('dialogForm');
    });

    it('should set elementName from the target name chain and respect contextFilterElement', () => {
      const form = makeElement('svy-form', { name: 'formA' });
      const target = makeElement('div', { name: 'myComp' }, form);
      const event: any = { target };

      service.setFormService({ getFormCacheByName: () => ({ getComponent: () => ({}) }) } as any);

      const result = service.createJSEvent(event, 'onClick', undefined, 'myComp');
      expect(result).not.toBeNull();
      expect(result!['elementName']).toBe('myComp');
    });

    it('should return null when the contextFilterElement does not match the resolved element', () => {
      const form = makeElement('svy-form', { name: 'formA' });
      const target = makeElement('div', { name: 'myComp' }, form);
      const event: any = { target };

      service.setFormService({ getFormCacheByName: () => ({ getComponent: () => ({}) }) } as any);

      expect(service.createJSEvent(event, 'onClick', undefined, 'differentComp')).toBeNull();
    });
  });

  describe('deepExtend', () => {
    it('should shallow merge objects by default', () => {
      const result = service.deepExtend([{ a: 1 }, { b: 2 }]);
      expect(result).toEqual({ a: 1, b: 2 });
    });

    it('should overwrite nested objects in a shallow merge', () => {
      const result = service.deepExtend([{ nested: { a: 1 } }, { nested: { b: 2 } }]);
      expect(result).toEqual({ nested: { b: 2 } });
    });

    it('should recursively merge nested objects in a deep merge', () => {
      const result = service.deepExtend([true, { nested: { a: 1 } }, { nested: { b: 2 } }]);
      expect(result).toEqual({ nested: { a: 1, b: 2 } });
    });
  });

  describe('getMainBody', () => {
    it('should return the mainBody element from the document', () => {
      const originalGetElementById = document.getElementById;
      const el = {} as any;
      document.getElementById = vi.fn().mockReturnValue(el);
      expect(service.getMainBody()).toBe(el);
      expect(document.getElementById).toHaveBeenCalledWith('mainBody');
      document.getElementById = originalGetElementById;
    });
  });
});
