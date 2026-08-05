import { describe, it, expect, beforeEach, vi } from 'vitest';

import { DesignerUtilsService } from './designerutils.service';

describe('DesignerUtilsService', () => {
  let service: DesignerUtilsService;
  let editorSession: Record<string, ReturnType<typeof vi.fn>>;
  let editorContentService: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    editorSession = {
      getState: vi.fn().mockReturnValue({ packages: [] }),
      getSelection: vi.fn().mockReturnValue([]),
      getAllowedChildrenForContainer: vi.fn().mockReturnValue(null),
    };
    editorContentService = {
      getContentArea: vi.fn(),
      getContent: vi.fn(),
      getContentForm: vi.fn(),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getLeftPositionIframe: vi.fn().mockReturnValue(0),
      getTopPositionIframe: vi.fn().mockReturnValue(0),
      querySelector: vi.fn().mockReturnValue(null),
    };

    service = Object.create(DesignerUtilsService.prototype);
    (service as any).editorSession = editorSession;
    (service as any).editorContentService = editorContentService;
  });

  describe('convertToContentPoint', () => {
    it('should convert x/y point relative to glasspane', () => {
      const glasspane = { getBoundingClientRect: () => new DOMRect(50, 30, 800, 600) } as HTMLElement;
      const point = { x: 150, y: 130 };
      const result = service.convertToContentPoint(glasspane, point);
      expect(result.x).toBe(100);
      expect(result.y).toBe(100);
    });

    it('should convert top/left point relative to glasspane', () => {
      const glasspane = { getBoundingClientRect: () => new DOMRect(50, 30, 800, 600) } as HTMLElement;
      const point = { top: 130, left: 150 };
      const result = service.convertToContentPoint(glasspane, point);
      expect(result.left).toBe(100);
      expect(result.top).toBe(100);
    });

    it('should not modify point with neither x/y nor top/left', () => {
      const glasspane = { getBoundingClientRect: () => new DOMRect(50, 30, 800, 600) } as HTMLElement;
      const point = { x: 0, y: 0 };
      const result = service.convertToContentPoint(glasspane, point);
      expect(result.x).toBe(0);
      expect(result.y).toBe(0);
    });
  });

  describe('isSameElement', () => {
    it('should return true when both elements have the same svy-id', () => {
      const elem1 = { getAttribute: vi.fn().mockReturnValue('uuid-1') } as unknown as HTMLElement;
      const elem2 = { getAttribute: vi.fn().mockReturnValue('uuid-1') } as unknown as Element;
      expect(service.isSameElement(elem1, elem2)).toBe(true);
    });

    it('should return false when elements have different svy-id', () => {
      const elem1 = { getAttribute: vi.fn().mockReturnValue('uuid-1') } as unknown as HTMLElement;
      const elem2 = { getAttribute: vi.fn().mockReturnValue('uuid-2') } as unknown as Element;
      expect(service.isSameElement(elem1, elem2)).toBe(false);
    });

    it('should return false when first element is undefined', () => {
      const elem2 = { getAttribute: vi.fn().mockReturnValue('uuid-1') } as unknown as Element;
      expect(service.isSameElement(undefined, elem2)).toBe(false);
    });

    it('should return false when second element is null', () => {
      const elem1 = { getAttribute: vi.fn().mockReturnValue('uuid-1') } as unknown as HTMLElement;
      expect(service.isSameElement(elem1, null as any)).toBe(false);
    });
  });

  describe('isInsideAutoscrollElementClientBounds', () => {
    const bounds = [
      new DOMRect(0, 580, 800, 20),
      new DOMRect(780, 0, 20, 600),
      new DOMRect(0, 0, 20, 600),
      new DOMRect(0, 0, 800, 20),
    ];

    it('should return true when point is inside bottom area', () => {
      expect(service.isInsideAutoscrollElementClientBounds(bounds, 400, 590)).toBe(true);
    });

    it('should return true when point is inside right area', () => {
      expect(service.isInsideAutoscrollElementClientBounds(bounds, 790, 300)).toBe(true);
    });

    it('should return true when point is inside left area', () => {
      expect(service.isInsideAutoscrollElementClientBounds(bounds, 10, 300)).toBe(true);
    });

    it('should return true when point is inside top area', () => {
      expect(service.isInsideAutoscrollElementClientBounds(bounds, 400, 10)).toBe(true);
    });

    it('should return false when point is in the center', () => {
      expect(service.isInsideAutoscrollElementClientBounds(bounds, 400, 300)).toBe(false);
    });

    it('should return false when bounds is null/undefined', () => {
      expect(service.isInsideAutoscrollElementClientBounds(null as any, 400, 300)).toBe(false);
    });
  });

  describe('isTopContainer', () => {
    it('should return true when layout is a top container in a package', () => {
      editorSession.getState.mockReturnValue({
        packages: [{
          packageName: 'mypackage',
          components: [
            { componentType: 'layout', topContainer: true, layoutName: 'container12' },
            { componentType: 'layout', topContainer: false, layoutName: 'column' },
          ],
        }],
      });
      expect(service.isTopContainer('mypackage.container12')).toBe(true);
    });

    it('should return false when layout is not a top container', () => {
      editorSession.getState.mockReturnValue({
        packages: [{
          packageName: 'mypackage',
          components: [
            { componentType: 'layout', topContainer: false, layoutName: 'column' },
          ],
        }],
      });
      expect(service.isTopContainer('mypackage.column')).toBe(false);
    });

    it('should return false when layoutName does not match', () => {
      editorSession.getState.mockReturnValue({
        packages: [{
          packageName: 'mypackage',
          components: [
            { componentType: 'layout', topContainer: true, layoutName: 'container12' },
          ],
        }],
      });
      expect(service.isTopContainer('mypackage.nonexistent')).toBe(false);
    });

    it('should search in categories as well', () => {
      editorSession.getState.mockReturnValue({
        packages: [{
          packageName: 'bootstrap',
          components: [{ componentType: 'component' }],
          categories: {
            'Layout': [
              { componentType: 'layout', topContainer: true, layoutName: 'container' },
            ],
          },
        }],
      });
      expect(service.isTopContainer('bootstrap.container')).toBe(true);
    });

    it('should return false when packages is empty', () => {
      editorSession.getState.mockReturnValue({ packages: [] });
      expect(service.isTopContainer('anything.anything')).toBe(false);
    });
  });

  describe('convertToAbsolutePoint', () => {
    it('should add iframe offset to point coordinates', () => {
      editorContentService.getContent.mockReturnValue({
        getBoundingClientRect: () => new DOMRect(50, 30, 800, 600),
      });
      const point = { x: 100, y: 200 };
      const result = service.convertToAbsolutePoint(point);
      expect(result.x).toBe(150);
      expect(result.y).toBe(230);
    });

    it('should not modify non-finite values', () => {
      editorContentService.getContent.mockReturnValue({
        getBoundingClientRect: () => new DOMRect(50, 30, 800, 600),
      });
      const point = { x: Infinity, y: 200 };
      const result = service.convertToAbsolutePoint(point);
      expect(result.x).toBe(Infinity);
      expect(result.y).toBe(200);
    });
  });
});
