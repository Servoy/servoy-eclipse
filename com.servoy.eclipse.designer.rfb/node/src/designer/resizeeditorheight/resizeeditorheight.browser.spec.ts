import { describe, beforeEach, afterEach, it, expect, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { NO_ERRORS_SCHEMA, provideZonelessChangeDetection, signal } from '@angular/core';

import { ResizeEditorHeightComponent } from './resizeeditorheight.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

/**
 * Browser spec that drives the REAL arrow-function handlers of the height resizer through a
 * constructed component (the jsdom unit spec only reimplements them).
 */
describe('ResizeEditorHeightComponent (browser)', () => {
  let fixture: ComponentFixture<ResizeEditorHeightComponent>;
  let component: ResizeEditorHeightComponent;
  let editorSession: any;
  let formGhost: HTMLElement;

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
    const ghostContainer = document.createElement('div');
    Object.defineProperty(ghostContainer, 'offsetHeight', { value: 400, configurable: true });
    const contentEl = document.createElement('div');
    const glassPane = document.createElement('div');
    Object.defineProperty(glassPane, 'offsetHeight', { value: 600, configurable: true });
    const contentArea = { getElementsByClassName: () => ([] as unknown as HTMLCollection), scrollTop: 0 };
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
    const el = fixture.nativeElement.querySelector('.resize-editor-height') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    expect(editorSession.dragging()).toBe(true);
    expect(editorSession.registerAutoscroll).toHaveBeenCalledWith(component);
  });

  it('onMouseMove should grow the content height while dragging', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-height') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    (component as any).mousePoint = { x: 0, y: 100 };
    (component as any).currentPosition = 200;
    // step = pageY(210) - mousePoint.y(100) = 110 -> 200 + 110 = 310
    component.onMouseMove({ pageY: 210, stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect((component as any).currentPosition).toBe(310);
  });

  it('onMouseUp should send height changes and clear the drag', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-height') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    (component as any).currentPosition = 350;
    component.onMouseUp({ stopPropagation: vi.fn() } as unknown as MouseEvent);
    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { height: 350 } });
    expect(editorSession.dragging()).toBe(false);
    expect(editorSession.unregisterAutoscroll).toHaveBeenCalledWith(component);
  });

  it('updateLocationCallback should scroll and advance the position', () => {
    const el = fixture.nativeElement.querySelector('.resize-editor-height') as HTMLElement;
    el.dispatchEvent(new MouseEvent('mousedown', { bubbles: true }));
    (component as any).currentPosition = 100;
    const before = (component as any).contentArea.scrollTop;
    component.updateLocationCallback(0, 10);
    expect((component as any).currentPosition).toBe(110);
    expect((component as any).contentArea.scrollTop).toBe(before + 10);
  });
});
