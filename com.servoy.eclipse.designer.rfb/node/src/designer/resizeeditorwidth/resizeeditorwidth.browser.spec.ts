import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, provideZonelessChangeDetection, signal } from '@angular/core';

import { ResizeEditorWidthComponent } from './resizeeditorwidth.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

/**
 * Browser spec: onMouseMove/onMouseUp/updateLocationCallback are arrow-function instance
 * fields, so they only exist on a constructed instance. This exercises the REAL arrow
 * functions (not reimplementations) through a TestBed-created component.
 */
describe('ResizeEditorWidthComponent (browser)', () => {
  let fixture: ComponentFixture<ResizeEditorWidthComponent>;
  let component: ResizeEditorWidthComponent;
  let editorSession: any;
  let docListeners: { mousemove?: EventListener; mouseup?: EventListener };

  beforeEach(async () => {
    docListeners = {};
    editorSession = {
      dragging: signal(false),
      sendChanges: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };
    const fakeDoc = {
      addEventListener: vi.fn((type: string, cb: EventListener) => {
        (docListeners as any)[type] = cb;
      }),
      removeEventListener: vi.fn()
    };
    const ghostContainer = document.createElement('div');
    Object.defineProperty(ghostContainer, 'offsetWidth', { value: 400, configurable: true });
    const contentEl = document.createElement('div');
    // use a plain object with a writable scrollLeft - a jsdom/chromium div keeps scrollLeft at 0
    const contentArea = { getElementsByClassName: () => ([] as unknown as HTMLCollection), scrollLeft: 0 };
    const glassPane = document.createElement('div');
    Object.defineProperty(glassPane, 'offsetWidth', { value: 600, configurable: true });
    const editorContentService = {
      getDocument: () => fakeDoc,
      getContentArea: () => contentArea,
      querySelector: () => contentEl,
      querySelectorAll: () => [ghostContainer],
      getGlassPane: () => glassPane
    };

    await TestBed.configureTestingModule({
      imports: [ResizeEditorWidthComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: EditorSessionService, useValue: editorSession },
        { provide: EditorContentService, useValue: editorContentService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ResizeEditorWidthComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture?.destroy());

  it('mousedown should register the drag and autoscroll', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-width') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    expect(editorSession.dragging()).toBe(true);
    expect(editorSession.registerAutoscroll).toHaveBeenCalledWith(component);
    expect(typeof docListeners.mousemove).toBe('function');
  });

  it('onMouseMove should grow the content width while dragging', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-width') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true, clientX: 100 }));
    (component as any).mousePoint = { x: 100, y: 0 };
    (component as any).currentPosition = 200;
    // step = pageX(215) - mousePoint.x(100) = 115 -> 200 + 115 = 315
    component.onMouseMove({ pageX: 215, stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect((component as any).currentPosition).toBe(315);
  });

  it('onMouseUp should send width changes and clear the drag', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-width') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    (component as any).currentPosition = 320;
    const formGhost = document.createElement('div');
    formGhost.className = 'ghost';
    formGhost.setAttribute('svy-ghosttype', 'form');
    formGhost.setAttribute('svy-id', 'form-id');
    document.body.appendChild(formGhost);
    try {
      component.onMouseUp({ stopPropagation: vi.fn() } as unknown as MouseEvent);
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 320 } });
      expect(editorSession.dragging()).toBe(false);
      expect(editorSession.unregisterAutoscroll).toHaveBeenCalledWith(component);
    } finally {
      document.body.removeChild(formGhost);
    }
  });

  it('updateLocationCallback should scroll and advance the position', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-width') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    (component as any).currentPosition = 100;
    const before = (component as any).contentArea.scrollLeft;
    component.updateLocationCallback(10, 0);
    expect((component as any).currentPosition).toBe(110);
    expect((component as any).contentArea.scrollLeft).toBe(before + 10);
  });
});
