import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, provideZonelessChangeDetection, signal } from '@angular/core';

import { ResizeEditorHeightComponent } from './resizeeditorheight.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

/**
 * Browser spec that drives the REAL arrow-function handlers of the height resizer through a
 * constructed component. The component delegates the resizing to HeightResizer (which keeps its
 * own state), so the tests assert on observable effects (styles, scroll position, sendChanges)
 * and not on internal component fields.
 */
describe('ResizeEditorHeightComponent (browser)', () => {
  let fixture: ComponentFixture<ResizeEditorHeightComponent>;
  let component: ResizeEditorHeightComponent;
  let editorSession: any;
  let formGhost: HTMLElement;
  let ghostContainer: HTMLElement;
  let contentEl: HTMLElement;
  let contentArea: { getElementsByClassName: () => HTMLCollection; scrollTop: number };

  /** dispatches a real mousedown on the resize handle and returns it (its pageY is the drag start) */
  const startDrag = (): MouseEvent => {
    const el = fixture.nativeElement.querySelector('.resize-editor-height') as HTMLElement;
    const down = new MouseEvent('mousedown', { bubbles: true, clientY: 100 });
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
    Object.defineProperty(ghostContainer, 'offsetHeight', { value: 400, configurable: true });
    contentEl = document.createElement('div');
    const glassPane = document.createElement('div');
    Object.defineProperty(glassPane, 'offsetHeight', { value: 600, configurable: true });
    // plain object with a writable scrollTop - a chromium div keeps scrollTop at 0 when not scrollable
    contentArea = { getElementsByClassName: () => ([] as unknown as HTMLCollection), scrollTop: 0 };
    const editorContentService = {
      getDocument: () => fakeDoc,
      getContentArea: () => contentArea,
      querySelector: (sel: string) => (sel.indexOf('form') >= 0 ? formGhost : contentEl),
      querySelectorAll: () => [ghostContainer],
      getGlassPane: () => glassPane
    };

    await TestBed.configureTestingModule({
      imports: [ResizeEditorHeightComponent],
      providers: [
        provideZonelessChangeDetection(),
        { provide: EditorSessionService, useValue: editorSession },
        { provide: EditorContentService, useValue: editorContentService }
      ],
      schemas: [NO_ERRORS_SCHEMA]
    }).compileComponents();

    fixture = TestBed.createComponent(ResizeEditorHeightComponent);
    component = fixture.componentInstance;
    fixture.detectChanges();
    await fixture.whenStable();
  });

  afterEach(() => fixture?.destroy());

  it('getAutoscrollLockId returns the height lock id', () => {
    expect(component.getAutoscrollLockId()).toBe('resize-editor-height');
  });

  it('mousedown should register the drag and autoscroll', () => {
    startDrag();
    expect(editorSession.dragging()).toBe(true);
    expect(editorSession.registerAutoscroll).toHaveBeenCalledWith(component);
  });

  it('onMouseMove should grow the content height while dragging', () => {
    const down = startDrag();
    // step = 110 -> starts at the ghost container height (400) -> 510
    component.onMouseMove({ pageY: down.pageY + 110, stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect(ghostContainer.style.height).toBe('510px');
    expect(contentEl.style.height).toBe('510px');
  });

  it('onMouseUp should send height changes and clear the drag', () => {
    const down = startDrag();
    component.onMouseMove({ pageY: down.pageY + 110, stopPropagation: vi.fn() } as unknown as MouseEvent);
    component.onMouseUp({ stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { height: 510 } });
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
    component.updateLocationCallback(0, 10);
    expect(ghostContainer.style.height).toBe('400px');
    expect(contentArea.scrollTop).toBe(10);
    // the position advanced by 10 on the previous call, so the second call applies 410
    component.updateLocationCallback(0, 10);
    expect(ghostContainer.style.height).toBe('410px');
    expect(contentArea.scrollTop).toBe(20);
  });
});
