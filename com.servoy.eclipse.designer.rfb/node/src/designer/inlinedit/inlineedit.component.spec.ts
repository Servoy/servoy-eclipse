import { vi, describe, beforeEach, it, expect } from 'vitest';
import { signal } from '@angular/core';
import { InlineEditComponent } from './inlineedit.component';

describe('InlineEditComponent', () => {
  let component: InlineEditComponent;
  let editorSession: any;

  beforeEach(() => {
    editorSession = {
      sendChanges: vi.fn(),
      setInlineEditMode: vi.fn(),
      getSelection: vi.fn().mockReturnValue([]),
      isInlineEditMode: vi.fn().mockReturnValue(false),
      registerCallback: signal(null),
      getComponentPropertyWithTags: vi.fn().mockResolvedValue('')
    };

    component = Object.create(InlineEditComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).designerUtilsService = { getNode: vi.fn(), convertToAbsolutePoint: vi.fn((p: any) => p) };
    (component as any).doc = document;
    (component as any).renderer = { listen: vi.fn(), setProperty: vi.fn(), setStyle: vi.fn() };
    const elementRefValue = { nativeElement: document.createElement('div') };
    (component as any).elementRef = () => elementRefValue;
    (component as any).showDirectEdit = signal(false);
    (component as any).node = '';
    (component as any).directEditProperty = '';
    (component as any).propertyValue = '';
    (component as any).lastValue = { node: '', directEditProperty: '', propertyValue: '' };
    (component as any).lastTimestamp = 0;
    (component as any).keyupListener = null;
    (component as any).keydownListener = null;
    (component as any).blurListener = null;
  });

  describe('applyValue', () => {
    it('should send changes when value differs', () => {
      (component as any).elementRef().nativeElement.textContent = 'new value';
      component.applyValue('node1', 'text', 'old value');
      expect(editorSession.sendChanges).toHaveBeenCalledWith({ node1: { text: 'new value' } });
      expect(editorSession.setInlineEditMode).toHaveBeenCalledWith(false);
    });

    it('should not send changes when value is the same', () => {
      (component as any).elementRef().nativeElement.textContent = 'same';
      component.applyValue('node1', 'text', 'same');
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });

    it('should not send changes when old is null and new is empty', () => {
      (component as any).elementRef().nativeElement.textContent = '';
      component.applyValue('node1', 'text', null as any);
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });

    it('should not send duplicate changes', () => {
      (component as any).elementRef().nativeElement.textContent = 'new';
      component.applyValue('node1', 'text', 'old');
      editorSession.sendChanges.mockClear();
      (component as any).elementRef().nativeElement.textContent = 'new';
      component.applyValue('node1', 'text', 'old');
      expect(editorSession.sendChanges).not.toHaveBeenCalled();
    });

    it('should set showDirectEdit to false', () => {
      (component as any).showDirectEdit.set(true);
      (component as any).elementRef().nativeElement.textContent = 'x';
      component.applyValue('n', 'p', 'x');
      expect((component as any).showDirectEdit()).toBe(false);
    });
  });

  describe('handleDirectEdit', () => {
    it('should set showDirectEdit and position the element', () => {
      component.handleDirectEdit('node1', { x: 10, y: 20, width: 100, height: 30 }, 'text', 'hello');
      expect((component as any).showDirectEdit()).toBe(true);
      expect((component as any).node).toBe('node1');
      expect((component as any).directEditProperty).toBe('text');
      expect((component as any).propertyValue).toBe('hello');
      expect(editorSession.setInlineEditMode).toHaveBeenCalledWith(true);
    });
  });

  describe('ngAfterViewInit', () => {
    it('should register a mouseup callback that enters inline edit on a fast double click', () => {
      (component as any).lastTimestamp = 0;
      component.ngAfterViewInit();
      const cb = editorSession.registerCallback();
      expect(cb.event).toBe('mouseup');
      const enterSpy = vi.spyOn(component, 'enterInlineEdit').mockImplementation(() => undefined);
      editorSession.isInlineEditMode.mockReturnValue(false);
      cb.function({ timeStamp: 100 } as MouseEvent);
      expect(enterSpy).toHaveBeenCalled();
      expect((component as any).lastTimestamp).toBe(100);
    });

    it('should not enter inline edit when already in inline edit mode', () => {
      component.ngAfterViewInit();
      const cb = editorSession.registerCallback();
      const enterSpy = vi.spyOn(component, 'enterInlineEdit').mockImplementation(() => undefined);
      editorSession.isInlineEditMode.mockReturnValue(true);
      cb.function({ timeStamp: 100 } as MouseEvent);
      expect(enterSpy).not.toHaveBeenCalled();
    });
  });

  describe('enterInlineEdit', () => {
    it('should do nothing when there is no selection', () => {
      editorSession.getSelection.mockReturnValue([]);
      const handleSpy = vi.spyOn(component, 'handleDirectEdit');
      component.enterInlineEdit({} as MouseEvent);
      expect(handleSpy).not.toHaveBeenCalled();
    });

    it('should handle a direct edit for a matching selected node', async () => {
      editorSession.getSelection.mockReturnValue(['node1']);
      const eventNode = {
        getAttribute: (n: string) => (n === 'svy-id' ? 'node1' : n === 'directEditPropertyName' ? 'dataProviderID' : null),
        clientHeight: 20,
        clientWidth: 100,
        getBoundingClientRect: () => ({ x: 5, y: 6, width: 100, height: 20 })
      };
      (component as any).designerUtilsService.getNode = vi.fn().mockReturnValue(eventNode);
      (component as any).designerUtilsService.convertToAbsolutePoint = vi.fn((p: any) => p);
      editorSession.getComponentPropertyWithTags.mockResolvedValue('value');
      const handleSpy = vi.spyOn(component, 'handleDirectEdit').mockImplementation(() => undefined);
      component.enterInlineEdit({} as MouseEvent);
      await Promise.resolve();
      expect(handleSpy).toHaveBeenCalledWith('node1', expect.objectContaining({ width: 100, height: 20 }), 'dataProviderID', 'value');
    });

    it('should not call handleDirectEdit when the node has no direct edit property', () => {
      editorSession.getSelection.mockReturnValue(['node1']);
      const eventNode = {
        getAttribute: (n: string) => (n === 'svy-id' ? 'node1' : null)
      };
      (component as any).designerUtilsService.getNode = vi.fn().mockReturnValue(eventNode);
      const handleSpy = vi.spyOn(component, 'handleDirectEdit');
      component.enterInlineEdit({} as MouseEvent);
      expect(handleSpy).not.toHaveBeenCalled();
    });
  });

  describe('addListeners', () => {
    it('should register keyup, keydown and blur listeners once', () => {
      const listen = vi.fn().mockReturnValue(() => undefined);
      (component as any).renderer = { listen };
      (component as any).keyupListener = null;
      (component as any).addListeners();
      expect(listen).toHaveBeenCalledTimes(3);
      listen.mockClear();
      (component as any).addListeners();
      expect(listen).not.toHaveBeenCalled();
    });
  });
});
