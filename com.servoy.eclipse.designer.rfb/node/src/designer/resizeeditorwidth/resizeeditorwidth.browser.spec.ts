import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, provideZonelessChangeDetection, signal } from '@angular/core';

import { ResizeEditorWidthComponent } from './resizeeditorwidth.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

/**
 * Browser spec: onMouseMove/onMouseUp are arrow-function instance fields, so they only exist on a
 * constructed instance. The component delegates the actual resizing to WidthResizer (which keeps
 * its own state), so these tests drive the real component + real WidthResizer through TestBed and
 * assert on the observable effects (styles, scroll position, sendChanges), not on internal fields.
 */
describe('ResizeEditorWidthComponent (browser)', () => {
  let fixture: ComponentFixture<ResizeEditorWidthComponent>;
  let component: ResizeEditorWidthComponent;
  let editorSession: any;
  let ghostContainer: HTMLElement;
  let contentEl: HTMLElement;
  let glassPane: HTMLElement;
  let formGhost: HTMLElement;
  let contentArea: { getElementsByClassName: () => HTMLCollection; scrollLeft: number };

  /** dispatches a real mousedown on the resize handle and returns it (its pageX is the drag start) */
  const startDrag = (): MouseEvent => {
    const el = fixture.nativeElement.querySelector('.resize-editor-width') as HTMLElement;
    const down = new MouseEvent('mousedown', { bubbles: true, clientX: 100 });
    el.dispatchEvent(down);
    return down;
  };

  beforeEach(async () => {
    editorSession = {
      dragging: signal(false),
      sendChanges: vi.fn(),
      registerAutoscroll: vi.fn(),
      unregisterAutoscroll: vi.fn()
    };
    formGhost = document.createElement('div');
    formGhost.className = 'ghost';
    formGhost.setAttribute('svy-ghosttype', 'form');
    formGhost.setAttribute('svy-id', 'form-id');

    const fakeDoc = { addEventListener: vi.fn(), removeEventListener: vi.fn() };
    ghostContainer = document.createElement('div');
    Object.defineProperty(ghostContainer, 'offsetWidth', { value: 400, configurable: true });
    contentEl = document.createElement('div');
    // use a plain object with a writable scrollLeft - a chromium div keeps scrollLeft at 0 when not scrollable
    contentArea = { getElementsByClassName: () => ([] as unknown as HTMLCollection), scrollLeft: 0 };
    glassPane = document.createElement('div');
    Object.defineProperty(glassPane, 'offsetWidth', { value: 600, configurable: true });
    const editorContentService = {
      getDocument: () => fakeDoc,
      getContentArea: () => contentArea,
      querySelector: (sel: string) => (sel.indexOf('form') >= 0 ? formGhost : contentEl),
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
    startDrag();
    expect(editorSession.dragging()).toBe(true);
    expect(editorSession.registerAutoscroll).toHaveBeenCalledWith(component);
  });

  it('onMouseMove should grow the content width while dragging', () => {
    const down = startDrag();
    // step = 115 -> starts at the ghost container width (400) -> 515
    component.onMouseMove({ pageX: down.pageX + 115, stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect(ghostContainer.style.width).toBe('515px');
    expect(contentEl.style.width).toBe('515px');
  });

  it('onMouseUp should send width changes and clear the drag', () => {
    const down = startDrag();
    component.onMouseMove({ pageX: down.pageX + 115, stopPropagation: vi.fn() } as unknown as MouseEvent);
    component.onMouseUp({ stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 515 } });
    expect(editorSession.dragging()).toBe(false);
    expect(editorSession.unregisterAutoscroll).toHaveBeenCalledWith(component);
  });

  it('onMouseUp without a prior mousedown should not send anything', () => {
    component.onMouseUp({ stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect(editorSession.sendChanges).not.toHaveBeenCalled();
    expect(editorSession.unregisterAutoscroll).not.toHaveBeenCalled();
  });

  it('updateLocationCallback should scroll and advance the position', () => {
    startDrag();
    component.updateLocationCallback(10, 0);
    expect(ghostContainer.style.width).toBe('400px');
    expect(contentArea.scrollLeft).toBe(10);
    // the position advanced by 10 on the previous call, so the second call applies 410
    component.updateLocationCallback(10, 0);
    expect(ghostContainer.style.width).toBe('410px');
    expect(contentArea.scrollLeft).toBe(20);
  });
});
