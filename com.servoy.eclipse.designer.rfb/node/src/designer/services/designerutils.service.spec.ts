import { describe, it, expect, beforeEach, vi } from 'vitest';
import { signal, WritableSignal } from '@angular/core';

import { DesignerUtilsService } from './designerutils.service';

describe('DesignerUtilsService', () => {
  let service: DesignerUtilsService;
  let editorSession: any;
  let editorContentService: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    editorSession = {
      packages: signal([]),
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
      (editorSession.packages as WritableSignal<any[]>).set([{
        packageName: 'mypackage',
        components: [
          { componentType: 'layout', topContainer: true, layoutName: 'container12' },
          { componentType: 'layout', topContainer: false, layoutName: 'column' },
        ],
      }]);
      expect(service.isTopContainer('mypackage.container12')).toBe(true);
    });

    it('should return false when layout is not a top container', () => {
      (editorSession.packages as WritableSignal<any[]>).set([{
        packageName: 'mypackage',
        components: [
          { componentType: 'layout', topContainer: false, layoutName: 'column' },
        ],
      }]);
      expect(service.isTopContainer('mypackage.column')).toBe(false);
    });

    it('should return false when layoutName does not match', () => {
      (editorSession.packages as WritableSignal<any[]>).set([{
        packageName: 'mypackage',
        components: [
          { componentType: 'layout', topContainer: true, layoutName: 'container12' },
        ],
      }]);
      expect(service.isTopContainer('mypackage.nonexistent')).toBe(false);
    });

    it('should search in categories as well', () => {
      (editorSession.packages as WritableSignal<any[]>).set([{
        packageName: 'bootstrap',
        components: [{ componentType: 'component' }],
        categories: {
          'Layout': [
            { componentType: 'layout', topContainer: true, layoutName: 'container' },
          ],
        },
      }]);
      expect(service.isTopContainer('bootstrap.container')).toBe(true);
    });

    it('should return false when packages is empty', () => {
      (editorSession.packages as WritableSignal<any[]>).set([]);
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

  const makeNode = (attrs: Record<string, string | null>, opts: {
    classes?: string[];
    parent?: any;
    rect?: DOMRect;
    offsetParent?: unknown;
    childNodes?: any[];
    clientWidth?: number;
    clientHeight?: number;
  } = {}): any => {
    const classList = new Set(opts.classes ?? []);
    const node: any = {
      getAttribute: (name: string) => (name in attrs ? attrs[name] : null),
      hasAttribute: (name: string) => name in attrs && attrs[name] !== null,
      classList: {
        contains: (c: string) => classList.has(c),
      },
      getBoundingClientRect: () => opts.rect ?? new DOMRect(0, 0, 100, 50),
      parentElement: opts.parent ?? null,
      offsetParent: 'offsetParent' in opts ? opts.offsetParent : {},
      childNodes: opts.childNodes ?? [],
      nodeType: Node.ELEMENT_NODE,
      clientWidth: opts.clientWidth ?? 100,
      clientHeight: opts.clientHeight ?? 50,
      querySelector: vi.fn().mockReturnValue(null),
      closest: vi.fn().mockReturnValue(null),
    };
    return node;
  };

  describe('adjustElementRect', () => {
    it('should return position unchanged when parent is a layout container and size is zero', () => {
      const parent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const node = makeNode({}, { parent });
      const position = new DOMRect(10, 20, 0, 0);
      expect(service.adjustElementRect(node, position)).toBe(position);
    });

    it('should walk up parents to find a non-zero width/height when size is zero', () => {
      const grandParent = makeNode({}, { rect: new DOMRect(0, 0, 300, 200) });
      const parent = makeNode({}, { parent: grandParent, rect: new DOMRect(0, 0, 0, 0) });
      const node = makeNode({}, { parent });
      const result = service.adjustElementRect(node, new DOMRect(5, 6, 0, 0));
      expect(result.width).toBe(300);
      expect(result.height).toBe(200);
      expect(result.x).toBe(5);
      expect(result.y).toBe(6);
    });

    it('should use the svy-wrapper parent rect when node has svy-id', () => {
      const wrapper = makeNode({}, { classes: ['svy-wrapper'], rect: new DOMRect(1, 2, 40, 60) });
      const node = makeNode({ 'svy-id': 'id1' }, { parent: wrapper });
      const result = service.adjustElementRect(node, new DOMRect(0, 0, 10, 10));
      expect(result.x).toBe(1);
      expect(result.width).toBe(40);
      expect(result.height).toBe(60);
    });

    it('should return position unchanged for svy-id node whose parent is a layout container', () => {
      const parent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const node = makeNode({ 'svy-id': 'id1' }, { parent });
      const position = new DOMRect(3, 4, 10, 10);
      expect(service.adjustElementRect(node, position)).toBe(position);
    });

    it('should return position unchanged when no wrapper is found and size is non-zero', () => {
      const greatGrandParent = makeNode({}, { classes: [] });
      const grandParent = makeNode({}, { parent: greatGrandParent, classes: [] });
      const parent = makeNode({}, { parent: grandParent, classes: [] });
      const node = makeNode({ 'svy-id': 'id1' }, { parent });
      const position = new DOMRect(0, 0, 20, 20);
      expect(service.adjustElementRect(node, position)).toBe(position);
    });
  });

  describe('getParent', () => {
    it('should return null when element is null', () => {
      expect(service.getParent(null as any, 'component')).toBeNull();
    });

    it('should return the element when its layout allows the child', () => {
      editorSession.getAllowedChildrenForContainer.mockReturnValue(['component']);
      const dt = makeNode({ 'svy-layoutname': 'row' });
      expect(service.getParent(dt, 'component')).toBe(dt);
    });

    it('should return the element when svy-types allows the child', () => {
      editorSession.getAllowedChildrenForContainer.mockReturnValue(null);
      const dt = makeNode({ 'svy-layoutname': 'row', 'svy-types': 'component,layout' });
      expect(service.getParent(dt, 'component')).toBe(dt);
    });
  });

  describe('getNextElementSibling', () => {
    it('should return the next sibling with an svy-id', () => {
      const sibling = { getAttribute: (n: string) => (n === 'svy-id' ? 'sib' : null) };
      const element: any = {
        parentElement: { getAttribute: () => 'parent-id' },
        nextElementSibling: sibling,
      };
      expect(service.getNextElementSibling(element)).toBe(sibling);
    });

    it('should drill into a sibling without svy-id to find a child with svy-id', () => {
      const inner = { getAttribute: () => 'inner' };
      const sibling: any = {
        getAttribute: () => null,
        querySelector: vi.fn().mockReturnValue(inner),
      };
      const element: any = {
        parentElement: { getAttribute: () => 'parent-id' },
        nextElementSibling: sibling,
      };
      expect(service.getNextElementSibling(element)).toBe(inner);
    });

    it('should fall back to the original element sibling when none found', () => {
      // sibling without svy-id and whose querySelector finds nothing => sibbling becomes null
      // then it falls back to originalElement.nextElementSibling (same object here)
      const sibling: any = {
        getAttribute: () => null,
        querySelector: vi.fn().mockReturnValue(null),
      };
      const element: any = {
        parentElement: { getAttribute: () => 'parent-id' },
        nextElementSibling: sibling,
      };
      const result = service.getNextElementSibling(element);
      expect(result).toBe(sibling);
    });
  });

  describe('getNodeBasedOnSelectionFCorLFC', () => {
    it('should return null when there is no single selection', () => {
      editorSession.getSelection.mockReturnValue([]);
      editorContentService.getAllContentElements.mockReturnValue([makeNode({ 'svy-id': 'a' })]);
      expect(service.getNodeBasedOnSelectionFCorLFC()).toBeNull();
    });

    it('should return the node when selected element is a form component', () => {
      editorSession.getSelection.mockReturnValue(['sel']);
      const node = makeNode({ 'svy-id': 'sel' }, { classes: ['svy-formcomponent'] });
      editorContentService.getAllContentElements.mockReturnValue([node]);
      expect(service.getNodeBasedOnSelectionFCorLFC()).toBe(node);
    });

    it('should return the node when selected element is a list form component', () => {
      editorSession.getSelection.mockReturnValue(['sel']);
      const node = makeNode({ 'svy-id': 'sel' }, { classes: ['svy-listformcomponent'] });
      editorContentService.getAllContentElements.mockReturnValue([node]);
      expect(service.getNodeBasedOnSelectionFCorLFC()).toBe(node);
    });

    it('should return null when the selected element is not a form/list component', () => {
      editorSession.getSelection.mockReturnValue(['sel']);
      const node = makeNode({ 'svy-id': 'sel' }, { classes: [] });
      editorContentService.getAllContentElements.mockReturnValue([node]);
      expect(service.getNodeBasedOnSelectionFCorLFC()).toBeNull();
    });
  });

  describe('getAutoscrollElementClientBounds', () => {
    it('should return undefined when there is no bottom autoscroll area', () => {
      editorContentService.querySelector.mockReturnValue(null);
      expect(service.getAutoscrollElementClientBounds()).toBeUndefined();
    });

    it('should return 4 bounds when autoscroll areas exist', () => {
      const rect = (t: number) => ({ getBoundingClientRect: () => new DOMRect(t, t, 10, 10) });
      editorContentService.querySelector.mockImplementation((sel: string) => {
        if (sel === '.bottomAutoscrollArea') return rect(1);
        if (sel === '.rightAutoscrollArea') return rect(2);
        if (sel === '.leftAutoscrollArea') return rect(3);
        if (sel === '.topAutoscrollArea') return rect(4);
        return null;
      });
      const bounds = service.getAutoscrollElementClientBounds();
      expect(bounds).toHaveLength(4);
      expect(bounds[0].x).toBe(1);
      expect(bounds[3].x).toBe(4);
    });
  });

  describe('getNode', () => {
    it('should return the matching element under the point', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const node = makeNode({ 'svy-id': 'x' }, {
        rect: new DOMRect(0, 0, 100, 100),
        parent: layoutParent,
      });
      editorContentService.getAllContentElements.mockReturnValue([node]);
      const event = { pageX: 50, pageY: 50 } as MouseEvent;
      expect(service.getNode(event)).toBe(node);
    });

    it('should skip nodes outside the point', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const node = makeNode({ 'svy-id': 'x' }, {
        rect: new DOMRect(200, 200, 50, 50),
        parent: layoutParent,
        offsetParent: null,
      });
      editorContentService.getAllContentElements.mockReturnValue([node]);
      const event = { pageX: 10, pageY: 10 } as MouseEvent;
      expect(service.getNode(event)).toBeUndefined();
    });
  });

  describe('getDropNode - type without component/layout handling', () => {
    it('should disallow when ghost drop target has no svy-types', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const dropTarget = makeNode({ 'svy-id': 'x' }, { rect: new DOMRect(0, 0, 100, 100), parent: layoutParent });
      editorContentService.getAllContentElements.mockReturnValue([dropTarget]);
      const event = { pageX: 50, pageY: 50 } as MouseEvent;
      const result = service.getDropNode(true, 'tab', false, '', event);
      expect(result.dropAllowed).toBe(false);
    });

    it('should allow when drop target svy-types contains the type', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const dropTarget = makeNode({ 'svy-id': 'x', 'svy-types': 'x,tab' }, { rect: new DOMRect(0, 0, 100, 100), parent: layoutParent });
      editorContentService.getAllContentElements.mockReturnValue([dropTarget]);
      const event = { pageX: 50, pageY: 50 } as MouseEvent;
      const result = service.getDropNode(true, 'tab', false, '', event);
      expect(result.dropAllowed).toBe(true);
      expect(result.dropTarget).toBe(dropTarget);
    });

    it('should disallow when drop target svy-types does not contain the type', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const dropTarget = makeNode({ 'svy-id': 'x', 'svy-types': 'other' }, { rect: new DOMRect(0, 0, 100, 100), parent: layoutParent });
      editorContentService.getAllContentElements.mockReturnValue([dropTarget]);
      const event = { pageX: 50, pageY: 50 } as MouseEvent;
      const result = service.getDropNode(true, 'tab', false, '', event);
      expect(result.dropAllowed).toBe(false);
    });
  });

  describe('getDropNode - absolute component', () => {
    it('should allow a component drop directly on the node', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const dropTarget = makeNode({ 'svy-id': 'x' }, { rect: new DOMRect(0, 0, 100, 100), parent: layoutParent });
      editorContentService.getAllContentElements.mockReturnValue([dropTarget]);
      const event = { pageX: 50, pageY: 50 } as MouseEvent;
      const result = service.getDropNode(true, 'component', false, '', event);
      expect(result.dropAllowed).toBe(true);
      expect(result.dropTarget).toBe(dropTarget);
    });

    it('should disallow a forbidden component', () => {
      const layoutParent = makeNode({}, { classes: ['svy-layoutcontainer'] });
      const dropTarget = makeNode({ 'svy-id': 'x', 'svy-forbidden-components': 'abc,mybean' }, { rect: new DOMRect(0, 0, 100, 100), parent: layoutParent });
      editorContentService.getAllContentElements.mockReturnValue([dropTarget]);
      const event = { pageX: 50, pageY: 50 } as MouseEvent;
      const result = service.getDropNode(true, 'component', false, '', event, 'mybean');
      expect(result.dropAllowed).toBe(false);
    });
  });

  describe('getDropNode - responsive layout drop on the form', () => {
    it('should allow dropping a layout on an empty form, computing the before child', () => {
      // no node found under cursor -> falls into the "drop on form" branch
      editorContentService.getAllContentElements.mockReturnValue([]);
      const content = {
        getBoundingClientRect: () => new DOMRect(0, 0, 500, 500),
        scrollLeft: 0,
        scrollTop: 0
      };
      editorContentService.getContentArea.mockReturnValue(content);
      // form has one child with an svy-id, positioned so the cursor is before it
      const child = makeNode({ 'svy-id': 'child1' }, { rect: new DOMRect(0, 0, 100, 100) });
      const form = {
        childNodes: [child],
      };
      editorContentService.getContentForm.mockReturnValue(form);
      const event = { pageX: 50, pageY: 50, clientX: 50, clientY: 50 } as MouseEvent;
      const result = service.getDropNode(false, 'layout', true, 'bootstrap.row', event);
      expect(result.dropAllowed).toBe(true);
    });

    it('should disallow a non-top container dropped directly on the form', () => {
      editorContentService.getAllContentElements.mockReturnValue([]);
      const content = {
        getBoundingClientRect: () => new DOMRect(0, 0, 500, 500),
        scrollLeft: 0,
        scrollTop: 0
      };
      editorContentService.getContentArea.mockReturnValue(content);
      editorContentService.getContentForm.mockReturnValue({ childNodes: [] });
      // cursor on the form, topContainer=false, not an absolute layout -> (!isForm || !topContainer) is true -> disallowed
      const event = { pageX: 50, pageY: 50, clientX: 50, clientY: 50 } as MouseEvent;
      const result = service.getDropNode(false, 'layout', false, 'bootstrap.row', event);
      expect(result.dropAllowed).toBe(false);
    });
  });

  describe('getParent recursion', () => {
    it('should recurse to the parent element when the current layout does not allow the child', () => {
      // 'row' allows 'component', 'col' does not
      editorSession.getAllowedChildrenForContainer.mockImplementation((name: string) =>
        name === 'row' ? ['component'] : null);
      const allowed = makeNode({ 'svy-id': 'p', 'svy-layoutname': 'row' });
      // dt has an svy-id so the while loop does not run; it recurses on dt.parentElement (allowed)
      const dt = makeNode({ 'svy-id': 'c', 'svy-layoutname': 'col' }, { classes: ['svy-layoutcontainer'], parent: allowed });
      const result = service.getParent(dt, 'component');
      expect(result).toBe(allowed);
    });
  });
});
