import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { EditorContentComponent } from './editorcontent.component';

describe('EditorContentComponent', () => {
  let component: EditorContentComponent;
  let urlParser: any;
  let editorSession: any;
  let editorContentService: any;

  beforeEach(() => {
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      getSolutionName: vi.fn().mockReturnValue('sol'),
      getFormName: vi.fn().mockReturnValue('form1'),
      getContentClientNr: vi.fn().mockReturnValue('1'),
      getFormWidth: vi.fn().mockReturnValue(800),
      getFormHeight: vi.fn().mockReturnValue(600)
    };
    editorSession = {
      keyPressed: vi.fn(),
      getFixedKeyEvent: vi.fn((e: any) => e),
      buildTiNG: vi.fn()
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getGlassPane: vi.fn().mockReturnValue({ clientHeight: 500, style: {} }),
      executeOnlyAfterInit: vi.fn((cb: any) => cb()),
      getContentBodyElement: vi.fn().mockReturnValue({ clientHeight: 400 }),
      getPallete: vi.fn().mockReturnValue({ style: {} }),
      getContentArea: vi.fn().mockReturnValue({ scrollHeight: 500, scrollWidth: 800, style: {} }),
      getContentForm: vi.fn().mockReturnValue({ style: {} }),
      sendMessageToIframe: vi.fn()
    };

    component = Object.create(EditorContentComponent.prototype);
    (component as any).urlParser = urlParser;
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).renderer = { setStyle: vi.fn() };
    (component as any).sanitizer = { bypassSecurityTrustResourceUrl: vi.fn((url: string) => url) };
    (component as any).windowRef = { nativeWindow: { location: { host: 'localhost:4200' } } };
    (component as any).designSize = { setEditor: vi.fn() };
    (component as any).elementRef = { nativeElement: { clientHeight: 500, getBoundingClientRect: () => ({ width: 800 }) } };
    // contentStyle is now a signal; production code reads it via contentStyle() and writes via .set(...)
    (component as any).contentStyle = signal<Record<string, string>>({ position: 'absolute', top: '20px', left: '20px' });
    (component as any).contentSizeFull = false;
    (component as any).lastHeight = undefined;
    (component as any).styleVariantPreview = false;
    (component as any).previewReady = { emit: vi.fn() };
  });

  describe('contentMessageReceived', () => {
    it('should update content size on updateFormSize for absolute layout', () => {
      component.contentMessageReceived('updateFormSize', { property: '', width: 1024, height: 768 });
      expect(component.contentStyle()['width']).toBe('1024px');
      expect(component.contentStyle()['height']).toBe('768px');
    });

    it('should set a new object reference so the signal fires change detection', () => {
      const before = component.contentStyle();
      component.contentMessageReceived('updateFormSize', { property: '', width: 1024, height: 768 });
      expect(component.contentStyle()).not.toBe(before);
    });

    it('should not update on updateFormSize for non-absolute layout', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      (component as any).contentStyle.set({ ...component.contentStyle(), width: '800px' });
      component.contentMessageReceived('updateFormSize', { property: '', width: 1024, height: 768 });
      expect(component.contentStyle()['width']).toBe('800px');
      expect(component.contentStyle()['height']).toBeUndefined();
    });

    it('should call buildTiNG on buildTitaniumClient', () => {
      component.contentMessageReceived('buildTitaniumClient', { property: '' });
      expect(editorSession.buildTiNG).toHaveBeenCalled();
    });
  });

  describe('ngOnInit', () => {
    it('should add width/height for absolute layout', () => {
      component.ngOnInit();
      expect(component.contentStyle()['position']).toBe('absolute');
      expect(component.contentStyle()['width']).toBe('800px');
      expect(component.contentStyle()['height']).toBe('600px');
    });

    it('should add bottom/right/minWidth for responsive layout', () => {
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      component.ngOnInit();
      expect(component.contentStyle()['bottom']).toBe('20px');
      expect(component.contentStyle()['right']).toBe('20px');
      expect(component.contentStyle()['minWidth']).toBe('992px');
      expect(component.contentStyle()['width']).toBeUndefined();
    });
  });

  describe('setContentSizeFull', () => {
    it('should set content style to full and remove width/height', () => {
      component.setContentSizeFull();
      expect(component.contentStyle()['right']).toBe('20px');
      expect(component.contentStyle()['bottom']).toBe('20px');
      expect(component.contentStyle()['position']).toBe('absolute');
      expect((component as any).contentSizeFull).toBe(true);
      expect(component.contentStyle()['width']).toBeUndefined();
      expect(component.contentStyle()['height']).toBeUndefined();
    });
  });

  describe('setContentSize', () => {
    it('should set width and height', () => {
      component.setContentSize('500px', '300px');
      expect(component.contentStyle()['width']).toBe('500px');
      expect(component.contentStyle()['height']).toBe('300px');
      expect((component as any).contentSizeFull).toBe(false);
    });

    it('should remove absolute/responsive positioning keys', () => {
      (component as any).contentStyle.set({ position: 'absolute', top: '20px', left: '20px', minWidth: '992px', bottom: '20px', right: '20px' });
      component.setContentSize('500px', '300px');
      expect(component.contentStyle()['top']).toBeUndefined();
      expect(component.contentStyle()['left']).toBeUndefined();
      expect(component.contentStyle()['position']).toBeUndefined();
      expect(component.contentStyle()['minWidth']).toBeUndefined();
      expect(component.contentStyle()['bottom']).toBeUndefined();
      expect(component.contentStyle()['right']).toBeUndefined();
      expect(component.contentStyle()['width']).toBe('500px');
    });

    it('should handle auto height', () => {
      const spy = vi.spyOn(component, 'adjustFromContentSize').mockImplementation(() => undefined);
      component.setContentSize('500px', 'auto');
      expect(component.contentStyle()['width']).toBe('500px');
      expect(component.contentStyle()['height']).toBeUndefined();
      expect(spy).toHaveBeenCalled();
    });
  });

  describe('onKeyUp', () => {
    it('should call keyPressed for delete key', () => {
      const event = { keyCode: 46 } as any;
      const result = component.onKeyUp(event);
      expect(editorSession.keyPressed).toHaveBeenCalledWith(event);
      expect(result).toBe(false);
    });

    it('should return true for non-special keys', () => {
      const event = { keyCode: 65 } as any;
      const result = component.onKeyUp(event);
      expect(result).toBe(true);
    });
  });

  describe('onKeyDown', () => {
    it('should call keyPressed for ctrl combos not in inlineEdit', () => {
      const event = { target: { className: '' }, ctrlKey: true, metaKey: false, altKey: false } as any;
      const result = component.onKeyDown(event);
      expect(editorSession.keyPressed).toHaveBeenCalled();
      expect(result).toBe(false);
    });

    it('should not call keyPressed when target is inlineEdit', () => {
      const event = { target: { className: 'inlineEdit' }, ctrlKey: true, metaKey: false, altKey: false } as any;
      const result = component.onKeyDown(event);
      expect(editorSession.keyPressed).not.toHaveBeenCalled();
      expect(result).toBe(true);
    });

    it('should return true for events without modifier keys', () => {
      const event = { target: { className: '' }, ctrlKey: false, metaKey: false, altKey: false } as any;
      const result = component.onKeyDown(event);
      expect(result).toBe(true);
    });
  });
});
