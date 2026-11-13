import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { MouseSelectionComponent } from './mouseselection.component';

describe('MouseSelectionComponent', () => {
  let component: MouseSelectionComponent;
  let editorSession: any;
  let editorContentService: any;
  let designerUtilsService: any;
  let urlParser: any;

  beforeEach(() => {
    editorSession = {
      dragging: signal(false),
      ghosthandle: signal(false),
      showWireframe: signal(false),
      resizing: signal(false),
      getSelection: vi.fn().mockReturnValue([]),
      setSelection: vi.fn(),
      requestSelection: vi.fn().mockResolvedValue(undefined),
      addSelectionChangedListener: vi.fn().mockReturnValue(() => undefined),
      updateFieldPositioner: vi.fn(),
      executeAction: vi.fn(),
      keyPressed: vi.fn(),
      openConfigurator: vi.fn(),
      createComponent: vi.fn(),
      getWizardProperties: vi.fn().mockReturnValue(null),
      updateSelection: vi.fn()
    };
    editorContentService = {
      addContentMessageListener: vi.fn(),
      removeContentMessageListener: vi.fn(),
      getGlassPane: vi.fn().mockReturnValue({ addEventListener: vi.fn(), style: {} }),
      getContentArea: vi.fn().mockReturnValue({ getBoundingClientRect: vi.fn().mockReturnValue({ left: 0, top: 0 }), scrollLeft: 0, scrollTop: 0 }),
      executeOnlyAfterInit: vi.fn((cb: any) => cb()),
      getAllContentElements: vi.fn().mockReturnValue([]),
      getContentElement: vi.fn().mockReturnValue(undefined),
      getLeftPositionIframe: vi.fn().mockReturnValue(0),
      getTopPositionIframe: vi.fn().mockReturnValue(0)
    };
    designerUtilsService = {
      getNode: vi.fn().mockReturnValue(null),
      getNodeBasedOnSelectionFCorLFC: vi.fn().mockReturnValue(null),
      adjustElementRect: vi.fn((_, r) => r)
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
      isMarqueeSelectOuter: vi.fn().mockReturnValue(false)
    };

    component = Object.create(MouseSelectionComponent.prototype);
    (component as any).cdr = { markForCheck: vi.fn() };
    (component as any).editorSession = editorSession;
    (component as any).editorContentService = editorContentService;
    (component as any).designerUtilsService = designerUtilsService;
    (component as any).urlParser = urlParser;
    (component as any).renderer = { setStyle: vi.fn(), addClass: vi.fn(), removeClass: vi.fn(), setAttribute: vi.fn() };
    (component as any).nodes = signal([]);
    (component as any).contentInit = true;
    (component as any).topAdjust = 20;
    (component as any).leftAdjust = 20;
    (component as any).lassostarted = false;
    (component as any).lastTimestamp = 0;
    (component as any).moveFCorLFC = false;
    (component as any).mouseDownEvent = null;
    (component as any).lassoRef = { nativeElement: document.createElement('div') };
    (component as any).selectedRef = () => [];
  });

  describe('rectanglesIntersect', () => {
    it('should detect overlapping rectangles', () => {
      const r1 = new DOMRect(0, 0, 100, 100);
      const r2 = new DOMRect(50, 50, 100, 100);
      expect((component as any).rectanglesIntersect(r1, r2, false)).toBe(true);
    });

    it('should detect non-overlapping rectangles', () => {
      const r1 = new DOMRect(0, 0, 50, 50);
      const r2 = new DOMRect(100, 100, 50, 50);
      expect((component as any).rectanglesIntersect(r1, r2, false)).toBe(false);
    });

    it('should check full containment when compFullInside is true', () => {
      const r1 = new DOMRect(0, 0, 200, 200);
      const r2 = new DOMRect(10, 10, 50, 50);
      expect((component as any).rectanglesIntersect(r1, r2, true)).toBe(true);
    });

    it('should return false for partial overlap with compFullInside', () => {
      const r1 = new DOMRect(0, 0, 100, 100);
      const r2 = new DOMRect(50, 50, 100, 100);
      expect((component as any).rectanglesIntersect(r1, r2, true)).toBe(false);
    });
  });

  describe('selectionChanged', () => {
    it('should call createNodes when contentInit is true', () => {
      const spy = vi.spyOn(component as any, 'createNodes' as any).mockImplementation(() => undefined);
      component.selectionChanged(['id1']);
      expect(spy).toHaveBeenCalledWith(['id1']);
    });

    it('should not call createNodes when contentInit is false', () => {
      (component as any).contentInit = false;
      const spy = vi.spyOn(component as any, 'createNodes' as any).mockImplementation(() => undefined);
      component.selectionChanged(['id1']);
      expect(spy).not.toHaveBeenCalled();
    });
  });

  describe('contentMessageReceived', () => {
    it('should call selectionChanged on redrawDecorators message', () => {
      const spy = vi.spyOn(component, 'selectionChanged').mockImplementation(() => undefined);
      editorSession.getSelection.mockReturnValue(['abc']);
      component.contentMessageReceived('redrawDecorators', { property: '' });
      expect(spy).toHaveBeenCalledWith(['abc'], true);
    });
  });

  describe('deleteAction', () => {
    it('should stop propagation and call keyPressed with delete keyCode', () => {
      const event = { stopPropagation: vi.fn() } as any;
      component.deleteAction(event);
      expect(event.stopPropagation).toHaveBeenCalled();
      expect(editorSession.keyPressed).toHaveBeenCalledWith({ keyCode: 46 });
    });
  });

  describe('zoomInAction', () => {
    it('should execute zoomIn action', () => {
      const event = { stopPropagation: vi.fn() } as any;
      component.zoomInAction(event);
      expect(editorSession.executeAction).toHaveBeenCalledWith('zoomIn');
    });
  });

  describe('copyAction', () => {
    it('should execute copy action', () => {
      const event = { stopPropagation: vi.fn() } as any;
      component.copyAction(event);
      expect(editorSession.executeAction).toHaveBeenCalledWith('copy');
    });
  });

  describe('updateMoveFCorLFC', () => {
    it('should set moveFCorLFC to true', () => {
      component.updateMoveFCorLFC();
      expect((component as any).moveFCorLFC).toBe(true);
    });
  });

  describe('checkIfNodeIsVisible', () => {
    it('should return false when element has zero dimensions', () => {
      editorContentService.getContentElement.mockReturnValue({ getBoundingClientRect: () => ({ height: 0, width: 0 }) });
      const node = { svyid: 'test' } as any;
      expect(component.checkIfNodeIsVisible(node)).toBe(false);
    });

    it('should return true when element has dimensions', () => {
      editorContentService.getContentElement.mockReturnValue({ getBoundingClientRect: () => ({ height: 50, width: 100 }) });
      const node = { svyid: 'test' } as any;
      expect(component.checkIfNodeIsVisible(node)).toBe(true);
    });

    it('should return false when element is not found', () => {
      editorContentService.getContentElement.mockReturnValue(null);
      const node = { svyid: 'test' } as any;
      expect(component.checkIfNodeIsVisible(node)).toBe(false);
    });
  });

  describe('notInsideFormComponent', () => {
    it('should return true when not inside form component', () => {
      const el = document.createElement('div');
      el.closest = vi.fn().mockReturnValue(null);
      editorContentService.getContentElement.mockReturnValue(el);
      const node = { svyid: 'test' } as any;
      expect(component.notInsideFormComponent(node)).toBe(true);
    });

    it('should return false when inside svy-listformcomponent', () => {
      const el = document.createElement('div');
      el.closest = vi.fn((selector: string) => selector === '.svy-listformcomponent' ? el : null);
      editorContentService.getContentElement.mockReturnValue(el);
      const node = { svyid: 'test' } as any;
      expect(component.notInsideFormComponent(node)).toBe(false);
    });
  });

  // Regression guard for SVY-21578 "Insert column is done outside the container".
  // SVY-21248 wrapped every child of a responsive container in an intermediate
  // `.svy-responsive-wrapper` div that carries NO svy-id. The old insertACopyAction read
  // `htmlNode.parentNode.getAttribute('svy-id')` (the wrapper -> null) and
  // `htmlNode.nextElementSibling` (the next wrapper -> null), so neither dropTargetUUID nor
  // rightSibling were sent and Java created the new column on the form root. The fix resolves
  // the drop target via `parentElement.closest('[svy-id]')` and the "after" sibling via the
  // wrapper-safe DesignerUtilsService.getNextElementSibling. These tests build the real
  // wrapped DOM so they FAIL on the pre-fix raw-parentNode/nextElementSibling code (payload
  // dropTargetUUID/rightSibling null) and PASS after the fix.
  describe('insertACopyAction', () => {
    const PARENT_SVY_ID = 'parent-container-uuid';
    const SELECTED_SVY_ID = 'selected-column-uuid';
    const NEXT_SVY_ID = 'next-column-uuid';

    // Faithful port of the real DesignerUtilsService.getNextElementSibling (designerutils.service.ts:379):
    // walk up through wrapper divs lacking svy-id, take the next element sibling, and if that
    // sibling itself lacks svy-id, read its svy-id-bearing descendant.
    const realGetNextElementSibling = (element: Element): Element | null => {
      const originalElement = element;
      let current: Element = element;
      while (current.parentElement && !current.parentElement.getAttribute('svy-id')) {
        current = current.parentElement;
      }
      let sibling = current.nextElementSibling;
      if (sibling && !sibling.getAttribute('svy-id')) {
        sibling = sibling.querySelector('[svy-id]');
      }
      if (sibling === null) {
        sibling = originalElement.nextElementSibling;
      }
      return sibling;
    };

    // Builds the SVY-21248 wrapped structure:
    //   <div.svy-layoutcontainer svy-id=PARENT>            <- the real parent container
    //     <div.svy-responsive-wrapper>                     <- wrapper, NO svy-id
    //       <div.svy-layoutcontainer svy-id=SELECTED>      <- the selected column (htmlNode)
    //     <div.svy-responsive-wrapper>                     <- wrapper, NO svy-id
    //       <div.svy-layoutcontainer svy-id=NEXT>          <- the following sibling column
    // Returns the selected column element plus the parent so tests can trim it.
    const buildWrappedResponsiveDom = (includeNextSibling: boolean) => {
      const parent = document.createElement('div');
      parent.classList.add('svy-layoutcontainer');
      parent.setAttribute('svy-id', PARENT_SVY_ID);

      const selectedWrapper = document.createElement('div');
      selectedWrapper.classList.add('svy-responsive-wrapper');
      const selected = document.createElement('div');
      selected.classList.add('svy-layoutcontainer');
      selected.setAttribute('svy-id', SELECTED_SVY_ID);
      selected.setAttribute('svy-layoutname', '12grid.column');
      selectedWrapper.appendChild(selected);
      parent.appendChild(selectedWrapper);

      if (includeNextSibling) {
        const nextWrapper = document.createElement('div');
        nextWrapper.classList.add('svy-responsive-wrapper');
        const next = document.createElement('div');
        next.classList.add('svy-layoutcontainer');
        next.setAttribute('svy-id', NEXT_SVY_ID);
        next.setAttribute('svy-layoutname', '12grid.column');
        nextWrapper.appendChild(next);
        parent.appendChild(nextWrapper);
      }

      return { parent, selected };
    };

    beforeEach(() => {
      designerUtilsService.getNextElementSibling = vi.fn(realGetNextElementSibling);
    });

    it('AC1: inserts BEFORE inside the parent container through the wrapper', () => {
      const { selected } = buildWrappedResponsiveDom(true);
      editorContentService.getContentElement.mockReturnValue(selected);
      const node = { svyid: SELECTED_SVY_ID } as any;
      const event = { stopPropagation: vi.fn() } as any;

      component.insertACopyAction(event, node, true);

      expect(event.stopPropagation).toHaveBeenCalled();
      expect(editorSession.createComponent).toHaveBeenCalledTimes(1);
      const payload = editorSession.createComponent.mock.calls[0][0];
      expect(payload.dropTargetUUID).toBe(PARENT_SVY_ID);
      expect(payload.rightSibling).toBe(SELECTED_SVY_ID);
      expect(payload.keepOldSelection).toBe(true);
      expect(payload.packageName).toBe('12grid');
      expect(payload.name).toBe('column');
    });

    it('AC2 + AC4: inserts AFTER inside the parent container, resolving the next sibling through the wrapper', () => {
      const { selected } = buildWrappedResponsiveDom(true);
      editorContentService.getContentElement.mockReturnValue(selected);
      const node = { svyid: SELECTED_SVY_ID } as any;
      const event = { stopPropagation: vi.fn() } as any;

      component.insertACopyAction(event, node, false);

      const payload = editorSession.createComponent.mock.calls[0][0];
      // AC4: non-null dropTargetUUID in the wrapper case.
      expect(payload.dropTargetUUID).toBe(PARENT_SVY_ID);
      // AC2: rightSibling read through the wrapper is the NEXT column's svy-id.
      expect(payload.rightSibling).toBe(NEXT_SVY_ID);
      expect(payload.keepOldSelection).toBe(true);
    });

    it('AC3: inserts AFTER when selected is the last child, leaving rightSibling unset but dropTargetUUID set', () => {
      const { selected } = buildWrappedResponsiveDom(false);
      editorContentService.getContentElement.mockReturnValue(selected);
      const node = { svyid: SELECTED_SVY_ID } as any;
      const event = { stopPropagation: vi.fn() } as any;

      component.insertACopyAction(event, node, false);

      const payload = editorSession.createComponent.mock.calls[0][0];
      // Still dropped into the parent container (not the form root).
      expect(payload.dropTargetUUID).toBe(PARENT_SVY_ID);
      // No following sibling -> rightSibling stays unset so Java appends to the end.
      expect(payload.rightSibling).toBeUndefined();
    });
  });

  describe('redrawDecorators', () => {
    it('should update styles for existing nodes', () => {
      const mockEl = document.createElement('div');
      Object.defineProperty(mockEl, 'getBoundingClientRect', { value: () => ({ height: 50, width: 100, top: 10, left: 20 }) });
      editorContentService.getContentElement.mockReturnValue(mockEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });
      (component as any).nodes.set([{ svyid: 'node1', style: {} }]);
      component.redrawDecorators();
      expect((component as any).nodes()[0].style.height).toBe('50px');
      expect((component as any).nodes()[0].style.width).toBe('100px');
    });

    it('should skip nodes not found in content', () => {
      editorContentService.getContentElement.mockReturnValue(undefined);
      (component as any).nodes.set([{ svyid: 'missing', style: {} }]);
      component.redrawDecorators();
      expect((component as any).nodes()[0].style.height).toBeUndefined();
    });

    const createLayoutContentElement = (svyTitle: string) => {
      const el = document.createElement('div');
      el.classList.add('svy-layoutcontainer');
      el.setAttribute('svy-title', svyTitle);
      Object.defineProperty(el, 'getBoundingClientRect', { value: () => ({ height: 50, width: 100, top: 10, left: 20 }) });
      return el;
    };

    const createDecoratorRef = (svyid: string) => {
      const decoratorEl = document.createElement('div');
      decoratorEl.setAttribute('id', svyid);
      return { nativeElement: decoratorEl };
    };

    it('should refresh the decorator svytitle from the fresh svy-title when wireframe is on', () => {
      editorSession.showWireframe.set(true);
      const contentEl = createLayoutContentElement('md-3');
      editorContentService.getContentElement.mockReturnValue(contentEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });
      const decorator = createDecoratorRef('node1');
      (component as any).selectedRef = () => [decorator];
      (component as any).renderer.setAttribute = vi.fn((el: HTMLElement, name: string, value: string) => el.setAttribute(name, value));
      (component as any).nodes.set([{ svyid: 'node1', style: {} }]);

      component.redrawDecorators();

      expect(decorator.nativeElement.getAttribute('svytitle')).toBe('md-3');
    });

    it('should not set svytitle when wireframe is off', () => {
      editorSession.showWireframe.set(false);
      const contentEl = createLayoutContentElement('md-3');
      editorContentService.getContentElement.mockReturnValue(contentEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });
      const decorator = createDecoratorRef('node1');
      (component as any).selectedRef = () => [decorator];
      (component as any).renderer.setAttribute = vi.fn((el: HTMLElement, name: string, value: string) => el.setAttribute(name, value));
      (component as any).nodes.set([{ svyid: 'node1', style: {} }]);

      component.redrawDecorators();

      expect(decorator.nativeElement.getAttribute('svytitle')).toBeNull();
      expect((component as any).renderer.setAttribute).not.toHaveBeenCalledWith(expect.anything(), 'svytitle', expect.anything());
    });

    it('should not set svytitle when the content element is not a layout container', () => {
      editorSession.showWireframe.set(true);
      const contentEl = document.createElement('div');
      contentEl.setAttribute('svy-title', 'md-3');
      Object.defineProperty(contentEl, 'getBoundingClientRect', { value: () => ({ height: 50, width: 100, top: 10, left: 20 }) });
      editorContentService.getContentElement.mockReturnValue(contentEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });
      const decorator = createDecoratorRef('node1');
      (component as any).selectedRef = () => [decorator];
      (component as any).renderer.setAttribute = vi.fn((el: HTMLElement, name: string, value: string) => el.setAttribute(name, value));
      (component as any).nodes.set([{ svyid: 'node1', style: {} }]);

      component.redrawDecorators();

      expect(decorator.nativeElement.getAttribute('svytitle')).toBeNull();
      expect((component as any).renderer.setAttribute).not.toHaveBeenCalledWith(expect.anything(), 'svytitle', expect.anything());
    });

    it('should not throw when no decorator matches the node svyid', () => {
      editorSession.showWireframe.set(true);
      const contentEl = createLayoutContentElement('md-3');
      editorContentService.getContentElement.mockReturnValue(contentEl);
      designerUtilsService.adjustElementRect.mockReturnValue({ height: 50, width: 100, top: 10, left: 20 });
      (component as any).selectedRef = () => [createDecoratorRef('other')];
      (component as any).nodes.set([{ svyid: 'node1', style: {} }]);

      expect(() => component.redrawDecorators()).not.toThrow();
      expect((component as any).nodes()[0].style.height).toBe('50px');
    });
  });

  describe('action helpers', () => {
    it('zoomInAction should stop propagation and execute zoomIn', () => {
      const event = { stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.zoomInAction(event);
      expect(event.stopPropagation).toHaveBeenCalled();
      expect(editorSession.executeAction).toHaveBeenCalledWith('zoomIn');
    });

    it('copyAction should execute copy', () => {
      const event = { stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.copyAction(event);
      expect(editorSession.executeAction).toHaveBeenCalledWith('copy');
    });

    it('openWizardAction should open the configurator for the property', () => {
      const event = { stopPropagation: vi.fn() } as unknown as MouseEvent;
      component.openWizardAction(event, 'dataProviderID');
      expect(editorSession.openConfigurator).toHaveBeenCalledWith('dataProviderID');
    });

    it('updateMoveFCorLFC should flag a form-component move', () => {
      component.updateMoveFCorLFC();
      expect((component as any).moveFCorLFC).toBe(true);
    });
  });

  describe('onEnter / onLeave', () => {
    it('onEnter should display the next sibling', () => {
      const src = document.createElement('div');
      const sibling = document.createElement('div');
      const parent = document.createElement('div');
      parent.appendChild(src);
      parent.appendChild(sibling);
      component.onEnter({ srcElement: src } as unknown as MouseEvent);
      expect(sibling.style.display).toBe('block');
    });

    it('onLeave should hide the source element', () => {
      const src = document.createElement('div');
      component.onLeave({ srcElement: src } as unknown as MouseEvent);
      expect(src.style.display).toBe('none');
    });
  });

  describe('ngOnInit / ngAfterViewInit / ngOnDestroy', () => {
    it('ngOnInit should request selection and attach glasspane listeners', () => {
      const addEventListener = vi.fn();
      editorContentService.getGlassPane.mockReturnValue({ addEventListener, style: {} });
      component.ngOnInit();
      expect(editorSession.requestSelection).toHaveBeenCalled();
      expect(addEventListener).toHaveBeenCalledWith('mousedown', expect.any(Function));
      expect(addEventListener).toHaveBeenCalledWith('mouseup', expect.any(Function));
      expect(addEventListener).toHaveBeenCalledWith('mousemove', expect.any(Function));
    });

    it('ngOnDestroy should unsubscribe and remove the content listener', () => {
      const removeSelection = vi.fn();
      (component as any).removeSelectionChangedListener = removeSelection;
      component.ngOnDestroy();
      expect(removeSelection).toHaveBeenCalled();
      expect(editorContentService.removeContentMessageListener).toHaveBeenCalledWith(component);
    });
  });

  describe('onMouseDown', () => {
    it('should select a found node that is not already selected', () => {
      const node = { getAttribute: (n: string) => (n === 'svy-id' ? 'id1' : null), parentElement: { classList: { contains: () => false }, parentElement: null, style: {} } };
      designerUtilsService.getNode.mockReturnValue(node);
      editorSession.getSelection.mockReturnValue([]);
      (component as any).onMouseDown({ pageX: 10, pageY: 10, ctrlKey: false, metaKey: false, shiftKey: false, button: 0 } as MouseEvent);
      expect(editorSession.setSelection).toHaveBeenCalledWith(['id1'], component);
    });

    it('should start a lasso when no node is found', () => {
      designerUtilsService.getNode.mockReturnValue(null);
      editorContentService.getContentArea.mockReturnValue({ getBoundingClientRect: () => ({ left: 0, top: 0 }), scrollLeft: 0, scrollTop: 0 });
      (component as any).lassoRef = () => ({ nativeElement: document.createElement('div') });
      (component as any).onMouseDown({ pageX: 10, pageY: 10, button: 0, ctrlKey: false, metaKey: false, shiftKey: false } as MouseEvent);
      expect((component as any).lassostarted).toBe(true);
      expect(editorSession.setSelection).toHaveBeenCalledWith([], component);
    });

    it('should ignore mousedown while dragging', () => {
      editorSession.dragging.set(true);
      (component as any).onMouseDown({ pageX: 10, pageY: 10, button: 0 } as MouseEvent);
      expect(editorSession.setSelection).not.toHaveBeenCalled();
    });
  });

  describe('onMouseUp', () => {
    const makeContentNode = (id: string, rect: DOMRect) => ({
      getAttribute: (n: string) => (n === 'svy-id' ? id : n === 'svy-layoutname' ? null : null),
      getBoundingClientRect: () => rect,
      offsetParent: {},
      parentElement: { classList: { contains: () => false }, closest: () => null, style: {} },
      classList: { contains: () => false },
      closest: () => null
    });

    beforeEach(() => {
      (component as any).lassoRef = () => ({ nativeElement: document.createElement('div') });
      (component as any).selectedRef = () => [];
      editorContentService.getContentArea.mockReturnValue({ getBoundingClientRect: () => ({ left: 0, top: 0 }), scrollLeft: 0, scrollTop: 0, focus: vi.fn() });
    });

    it('should select the element directly under the cursor on a click (no lasso)', () => {
      (component as any).lassostarted = false;
      (component as any).mouseDownEvent = { x: 60, y: 60 };
      (component as any).fieldLocation = null;
      const node = makeContentNode('id1', new DOMRect(0, 0, 100, 100));
      editorContentService.getAllContentElements.mockReturnValue([node]);
      const event = { pageX: 50, pageY: 50, x: 60, y: 60, button: 0, timeStamp: 1000, ctrlKey: false, metaKey: false, shiftKey: false } as MouseEvent;
      component['onMouseUp'](event);
      expect(editorSession.setSelection).toHaveBeenCalledWith(['id1'], component);
    });

    it('should lasso-select intersecting elements when a lasso was dragged', () => {
      (component as any).lassostarted = true;
      (component as any).mousedownpoint = { x: 0, y: 0 };
      (component as any).mouseDownEvent = { x: 0, y: 0 };
      (component as any).fieldLocation = null;
      const node = makeContentNode('id1', new DOMRect(10, 10, 50, 50));
      editorContentService.getAllContentElements.mockReturnValue([node]);
      const event = { pageX: 200, pageY: 200, x: 200, y: 200, button: 0, timeStamp: 2000, ctrlKey: false, metaKey: false, shiftKey: false } as MouseEvent;
      component['onMouseUp'](event);
      expect(editorSession.setSelection).toHaveBeenCalledWith(['id1'], component);
    });

    it('should update the field positioner when the click did not move', () => {
      (component as any).lassostarted = false;
      (component as any).fieldLocation = { x: 50, y: 50 };
      (component as any).mouseDownEvent = { x: 50, y: 50 };
      editorContentService.getAllContentElements.mockReturnValue([]);
      const event = { pageX: 50, pageY: 50, x: 50, y: 50, button: 0, timeStamp: 1000, ctrlKey: false, metaKey: false, shiftKey: false } as MouseEvent;
      component['onMouseUp'](event);
      expect(editorSession.updateFieldPositioner).toHaveBeenCalled();
    });

    it('should trigger zoomIn on a fast double click of a maxLevelDesign node', () => {
      (component as any).lassostarted = false;
      (component as any).fieldLocation = null;
      (component as any).mouseDownEvent = { x: 1, y: 1 };
      (component as any).lastTimestamp = 1000;
      (component as any).nodes.set([{ svyid: 'id1', maxLevelDesign: true, style: {} }]);
      editorContentService.getAllContentElements.mockReturnValue([]);
      const event = { pageX: 9, pageY: 9, x: 9, y: 9, button: 0, timeStamp: 1100, ctrlKey: false, metaKey: false, shiftKey: false } as MouseEvent;
      component['onMouseUp'](event);
      expect(editorSession.executeAction).toHaveBeenCalledWith('zoomIn');
    });
  });

  describe('onMouseMove lasso', () => {
    it('should grow the lasso rectangle while dragging a lasso', () => {
      (component as any).lassostarted = true;
      (component as any).mousedownpoint = { x: 100, y: 100 };
      const lassoEl = document.createElement('div');
      lassoEl.style.display = 'none';
      (component as any).lassoRef = () => ({ nativeElement: lassoEl });
      editorContentService.getContentArea.mockReturnValue({ getBoundingClientRect: () => ({ left: 0, top: 0 }), scrollLeft: 0, scrollTop: 0 });
      (component as any).renderer = {
        setStyle: (el: HTMLElement, p: string, v: string) => {
          (el.style as any)[p] = v;
        }
      };
      (component as any).onMouseMove({ pageX: 150, pageY: 160 } as MouseEvent);
      expect(lassoEl.style.width).toBe('50px');
      expect(lassoEl.style.height).toBe('60px');
      expect(lassoEl.style.display).toBe('block');
    });

    it('should redraw decorators while resizing', () => {
      editorSession.dragging.set(false);
      editorSession.resizing.set(true);
      (component as any).lassostarted = false;
      const redraw = vi.spyOn(component, 'redrawDecorators').mockImplementation(() => undefined);
      (component as any).onMouseMove({ pageX: 1, pageY: 1 } as MouseEvent);
      expect(redraw).toHaveBeenCalled();
    });
  });
});
