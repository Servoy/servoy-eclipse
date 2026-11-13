import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Renderer2, TemplateRef, SimpleChange } from '@angular/core';

import { AbstractFormComponent } from './abstract_form_component.component';
import { FormCache, StructureCache, ComponentCache, FormComponentCache } from '../types';

class TestFormComponent extends AbstractFormComponent {
  containersByName: Record<string, Element> = {};
  getFormCache(): FormCache {
    return null!;
  }
  getTemplate(_item: StructureCache | ComponentCache | FormComponentCache): TemplateRef<any> {
    return null!;
  }
  getTemplateForLFC(_state: ComponentCache): TemplateRef<any> {
    return null!;
  }
  getContainerByName(containername: string): Element {
    return this.containersByName[containername];
  }
  // expose for test registration
  setComponent(name: string, comp: any) {
    (this as any).componentCache[name] = comp;
  }
}

describe('AbstractFormComponent', () => {
  let renderer: any;
  let component: TestFormComponent;

  beforeEach(() => {
    renderer = { addClass: vi.fn(), removeClass: vi.fn(), setStyle: vi.fn() };
    component = new TestFormComponent(renderer as Renderer2);
  });

  it('isDesigner should be false by default', () => {
    expect(component.isDesigner()).toBe(false);
  });

  it('getNGClass should return null by default', () => {
    expect(component.getNGClass({} as StructureCache)).toBeNull();
  });

  describe('containers setter', () => {
    it('should ignore a null value', () => {
      component.containers = null!;
      expect(component.containers).toBeUndefined();
    });

    it('should add classes to a matching container', () => {
      const el = document.createElement('div');
      component.containersByName['c1'] = el;
      component.containers = { added: { c1: ['cls-a', 'cls-b'] }, removed: {} };
      expect(renderer.addClass).toHaveBeenCalledWith(el, 'cls-a');
      expect(renderer.addClass).toHaveBeenCalledWith(el, 'cls-b');
      expect(component.containers.added.c1).toEqual(['cls-a', 'cls-b']);
    });

    it('should remove classes present in the previous added set but not the new one', () => {
      const el = document.createElement('div');
      component.containersByName['c1'] = el;
      component.containers = { added: { c1: ['cls-a', 'cls-b'] }, removed: {} };
      renderer.removeClass.mockClear();
      component.containers = { added: { c1: ['cls-a'] }, removed: {} };
      expect(renderer.removeClass).toHaveBeenCalledWith(el, 'cls-b');
    });

    it('should remove classes from the removed set', () => {
      const el = document.createElement('div');
      component.containersByName['c1'] = el;
      component.containers = { added: {}, removed: { c1: ['old'] } };
      expect(renderer.removeClass).toHaveBeenCalledWith(el, 'old');
    });
  });

  describe('cssstyles setter', () => {
    it('should ignore a null value', () => {
      component.cssstyles = null!;
      expect(component.cssstyles).toBeUndefined();
    });

    it('should apply the styles to a matching container', () => {
      const el = document.createElement('div');
      component.containersByName['c1'] = el;
      component.cssstyles = { c1: { color: 'red', width: '10px' } };
      expect(renderer.setStyle).toHaveBeenCalledWith(el, 'color', 'red');
      expect(renderer.setStyle).toHaveBeenCalledWith(el, 'width', '10px');
      expect(component.cssstyles).toEqual({ c1: { color: 'red', width: '10px' } });
    });
  });

  describe('triggerNgOnChangeWithSameRefDueToSmartPropUpdate', () => {
    it('should call ngOnChanges and detectChanges on a registered component', () => {
      const comp = { ngOnChanges: vi.fn(), detectChanges: vi.fn() };
      component.setComponent('comp1', comp);
      component.triggerNgOnChangeWithSameRefDueToSmartPropUpdate('comp1', [{ propertyName: 'p', newPropertyValue: 42 }]);
      expect(comp.ngOnChanges).toHaveBeenCalled();
      const changes = comp.ngOnChanges.mock.calls[0][0];
      expect(changes.p).toBeInstanceOf(SimpleChange);
      expect(changes.p.currentValue).toBe(42);
      expect(comp.detectChanges).toHaveBeenCalled();
    });

    it('should be a no-op for an unknown component', () => {
      expect(() => component.triggerNgOnChangeWithSameRefDueToSmartPropUpdate('unknown', [])).not.toThrow();
    });
  });

  describe('doCallApiOnComponent', () => {
    const converterService = {
      convertFromServerToClient: (v: any) => v,
      convertFromClientToServer: (v: any) => [v],
    } as any;
    const log = { error: vi.fn(), buildMessage: (fn: () => string) => fn() } as any;

    it('should return null and log when the component is missing', () => {
      const result = AbstractFormComponent.doCallApiOnComponent(null!, { getApiFunction: () => null } as any, 'doIt', [], converterService, log, 'compX');
      expect(result).toBeNull();
      expect(log.error).toHaveBeenCalled();
    });

    it('should return null and log when the api function does not exist on the component', () => {
      class NoApi {
        name() {
          return 'c';
        }
      }
      const comp = new NoApi() as any;
      const result = AbstractFormComponent.doCallApiOnComponent(comp, { getApiFunction: () => null } as any, 'missingApi', [], converterService, log, 'c');
      expect(result).toBeNull();
    });

    it('should call the api function and convert the result', async () => {
      class WithApi {
        name() {
          return 'c';
        }
        doIt(a: number) {
          return a + 1;
        }
      }
      const comp = new WithApi() as any;
      const spec = { getApiFunction: () => ({ getArgumentType: () => null, returnType: null }) } as any;
      const result = await AbstractFormComponent.doCallApiOnComponent(comp, spec, 'doIt', [5], converterService, log, 'c');
      expect(result).toBe(6);
    });
  });
});
