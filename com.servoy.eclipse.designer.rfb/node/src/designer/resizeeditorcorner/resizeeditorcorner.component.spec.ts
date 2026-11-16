import { vi, describe, beforeEach, it, expect } from 'vitest';
import { ResizeEditorCornerComponent } from './resizeeditorcorner.component';
import { WidthResizer } from '../resize/widthresizer';
import { HeightResizer } from '../resize/heightresizer';

describe('ResizeEditorCornerComponent (prototype methods)', () => {
  let component: ResizeEditorCornerComponent;
  let widthResizer: any;
  let heightResizer: any;

  beforeEach(() => {
    widthResizer = { start: vi.fn(), move: vi.fn(), end: vi.fn(), updateLocation: vi.fn() };
    heightResizer = { start: vi.fn(), move: vi.fn(), end: vi.fn(), updateLocation: vi.fn() };

    component = Object.create(ResizeEditorCornerComponent.prototype);
    (component as any).widthResizer = widthResizer;
    (component as any).heightResizer = heightResizer;
  });

  describe('getAutoscrollLockId', () => {
    it('should return resize-editor-corner so it locks its own autoscroll target', () => {
      expect(component.getAutoscrollLockId()).toBe('resize-editor-corner');
    });
  });

  describe('updateLocationCallback', () => {
    it('should forward x to the width resizer and y to the height resizer (diagonal auto-grow)', () => {
      component.updateLocationCallback(7, 11);
      expect(widthResizer.updateLocation).toHaveBeenCalledWith(7);
      expect(heightResizer.updateLocation).toHaveBeenCalledWith(11);
    });
  });
});

describe('ResizeEditorCornerComponent (dual-axis drag via the two resizers)', () => {
  let editorSession: any;
  let editorContentService: any;
  let renderer: any;
  let ghost: any;
  let editorContent: any;
  let contentArea: any;
  let glasspane: any;
  let widthResizer: WidthResizer;
  let heightResizer: HeightResizer;

  const buildGhostsList = (ghosts: any[]) => ({
    length: ghosts.length,
    item: (i: number) => ghosts[i] ?? null,
    [Symbol.iterator]: function* () {
      for (const g of ghosts) yield g;
    }
  });

  beforeEach(() => {
    editorSession = { sendChanges: vi.fn() };
    renderer = { setStyle: vi.fn() };

    ghost = { offsetWidth: 400, offsetLeft: 0, offsetHeight: 300, offsetTop: 0, style: {} };
    editorContent = { style: {} };
    contentArea = { scrollLeft: 0, scrollTop: 0, getElementsByClassName: vi.fn().mockReturnValue(buildGhostsList([])) };
    glasspane = { style: {}, offsetWidth: 600, offsetHeight: 600 };
    const formGhost = { getAttribute: vi.fn().mockReturnValue('form-id') };

    editorContentService = {
      getDocument: vi.fn(),
      getContentArea: vi.fn().mockReturnValue(contentArea),
      querySelector: vi.fn().mockImplementation((selector: string) => (selector === '.content' ? editorContent : formGhost)),
      querySelectorAll: vi.fn().mockReturnValue([ghost]),
      getGlassPane: vi.fn().mockReturnValue(glasspane)
    };

    widthResizer = new WidthResizer(renderer, editorSession, editorContentService);
    heightResizer = new HeightResizer(renderer, editorSession, editorContentService);
  });

  it('a diagonal drag resizes BOTH axes 1:1', () => {
    const down = { pageX: 100, pageY: 100 } as MouseEvent;
    widthResizer.start(down);
    heightResizer.start(down);

    const move = { pageX: 150, pageY: 170 } as MouseEvent;
    widthResizer.move(move);
    heightResizer.move(move);

    expect(renderer.setStyle).toHaveBeenCalledWith(ghost, 'width', '450px');
    expect(renderer.setStyle).toHaveBeenCalledWith(ghost, 'height', '370px');
  });

  it('on mouseup the corner sends BOTH the width and the height change', () => {
    const down = { pageX: 100, pageY: 100 } as MouseEvent;
    widthResizer.start(down);
    heightResizer.start(down);
    const move = { pageX: 150, pageY: 170 } as MouseEvent;
    widthResizer.move(move);
    heightResizer.move(move);

    widthResizer.end();
    heightResizer.end();

    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 450 } });
    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { height: 370 } });
  });

  it('on mouseup it also sends the lowest part y alongside width and height', () => {
    const part = {
      getAttribute: vi.fn((name: string) => (name === 'svy-ghosttype' ? 'part' : 'part-id')),
      offsetHeight: 10,
      offsetTop: 0,
      style: {}
    };
    contentArea.getElementsByClassName.mockReturnValue(buildGhostsList([part]));

    const down = { pageX: 100, pageY: 100 } as MouseEvent;
    widthResizer.start(down);
    heightResizer.start(down);
    const move = { pageX: 150, pageY: 170 } as MouseEvent;
    widthResizer.move(move);
    heightResizer.move(move);

    widthResizer.end();
    heightResizer.end();

    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { width: 450 } });
    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'form-id': { height: 370 } });
    expect(editorSession.sendChanges).toHaveBeenCalledWith({ 'part-id': { y: 370 } });
  });
});
