import { vi, describe, beforeEach, it, expect } from 'vitest';
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
      registerCallback: { next: vi.fn() },
      getComponentPropertyWithTags: vi.fn().mockResolvedValue('')
    };

    component = Object.create(InlineEditComponent.prototype);
    (component as any).editorSession = editorSession;
    (component as any).designerUtilsService = { getNode: vi.fn(), convertToAbsolutePoint: vi.fn((p: any) => p) };
    (component as any).doc = document;
    (component as any).renderer = { listen: vi.fn(), setProperty: vi.fn(), setStyle: vi.fn() };
    (component as any).cdRef = { detectChanges: vi.fn(), markForCheck: vi.fn() };
    const elementRefValue = { nativeElement: document.createElement('div') };
    (component as any).elementRef = () => elementRefValue;
    (component as any).showDirectEdit = false;
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
      (component as any).showDirectEdit = true;
      (component as any).elementRef().nativeElement.textContent = 'x';
      component.applyValue('n', 'p', 'x');
      expect((component as any).showDirectEdit).toBe(false);
    });
  });

  describe('handleDirectEdit', () => {
    it('should set showDirectEdit and position the element', () => {
      component.handleDirectEdit('node1', { x: 10, y: 20, width: 100, height: 30 }, 'text', 'hello');
      expect((component as any).showDirectEdit).toBe(true);
      expect((component as any).node).toBe('node1');
      expect((component as any).directEditProperty).toBe('text');
      expect((component as any).propertyValue).toBe('hello');
      expect(editorSession.setInlineEditMode).toHaveBeenCalledWith(true);
    });
  });
});
