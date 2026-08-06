import { describe, it, expect, beforeEach, vi } from 'vitest';

import { KeyboardLayoutDirective } from './keyboardlayout.directive';

describe('KeyboardLayoutDirective', () => {
  let directive: KeyboardLayoutDirective;
  let editorSession: any;
  let urlParser: any;
  let editorContentService: any;

  beforeEach(() => {
    editorSession = {
      getFixedKeyEvent: vi.fn(),
      isInlineEditMode: vi.fn().mockReturnValue(false),
      getSelection: vi.fn().mockReturnValue([]),
      updateSelection: vi.fn(),
      sendChanges: vi.fn(),
      keyPressed: vi.fn(),
    };
    urlParser = {
      isAbsoluteFormLayout: vi.fn().mockReturnValue(true),
    };
    editorContentService = {
      getContentElement: vi.fn(),
    };

    directive = Object.create(KeyboardLayoutDirective.prototype);
    (directive as any).editorSession = editorSession;
    (directive as any).urlParser = urlParser;
    (directive as any).editorContentService = editorContentService;
    directive.isSendChanges = true;
    directive.boundsUpdating = false;
  });

  describe('onKeyDown', () => {
    it('should ignore non-arrow keys', () => {
      editorSession.getFixedKeyEvent.mockReturnValue({ keyCode: 65, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false });
      const result = directive.onKeyDown({ keyCode: 65 } as KeyboardEvent);
      expect(result).toBeUndefined();
      expect(directive.boundsUpdating).toBe(false);
    });

    it('should ignore arrow keys when in inline edit mode', () => {
      editorSession.isInlineEditMode.mockReturnValue(true);
      editorSession.getFixedKeyEvent.mockReturnValue({ keyCode: 37, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false });
      const result = directive.onKeyDown({ keyCode: 37 } as KeyboardEvent);
      expect(result).toBeUndefined();
    });

    it('should return true and set isSendChanges=false when no selection', () => {
      editorSession.getFixedKeyEvent.mockReturnValue({ keyCode: 37, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false });
      editorSession.getSelection.mockReturnValue([]);
      const result = directive.onKeyDown({ keyCode: 37 } as KeyboardEvent);
      expect(directive.isSendChanges).toBe(false);
      expect(result).toBe(true);
    });

    it('should return true when not absolute form layout', () => {
      editorSession.getFixedKeyEvent.mockReturnValue({ keyCode: 37, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false });
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      urlParser.isAbsoluteFormLayout.mockReturnValue(false);
      const result = directive.onKeyDown({ keyCode: 37 } as KeyboardEvent);
      expect(directive.isSendChanges).toBe(false);
      expect(result).toBe(true);
    });

    it('should return true when ctrl+alt is pressed (pass-through)', () => {
      editorSession.getFixedKeyEvent.mockReturnValue({ keyCode: 37, ctrlKey: true, shiftKey: false, altKey: true, metaKey: false });
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      const result = directive.onKeyDown({ keyCode: 37 } as KeyboardEvent);
      expect(directive.isSendChanges).toBe(false);
      expect(result).toBe(true);
    });

    it('should set boundsUpdating and return false for arrow key with selection in absolute layout', () => {
      editorSession.getFixedKeyEvent.mockReturnValue({ keyCode: 39, ctrlKey: false, shiftKey: false, altKey: false, metaKey: false });
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      const element = document.createElement('div');
      element.classList.add('svy-wrapper');
      element.style.left = '10px';
      element.style.top = '20px';
      element.style.width = '100px';
      element.style.height = '50px';
      editorContentService.getContentElement.mockReturnValue(element);
      const result = directive.onKeyDown({ keyCode: 39 } as KeyboardEvent);
      expect(directive.boundsUpdating).toBe(true);
      expect(result).toBe(false);
      expect(editorSession.updateSelection).toHaveBeenCalledWith(['uuid-1'], true, true);
    });
  });

  describe('onKeyup', () => {
    it('should reset boundsUpdating and process selection on keyup', () => {
      directive.boundsUpdating = true;
      directive.isSendChanges = true;
      const element = document.createElement('div');
      element.classList.add('svy-wrapper');
      element.style.left = '10px';
      element.style.top = '20px';
      element.style.width = '100px';
      element.style.height = '50px';
      editorSession.getSelection.mockReturnValue(['uuid-1']);
      editorContentService.getContentElement.mockReturnValue(element);
      directive.onKeyup({ keyCode: 37, shiftKey: false } as KeyboardEvent);
      expect(directive.boundsUpdating).toBe(false);
      expect(editorSession.sendChanges).toHaveBeenCalled();
    });

    it('should call keyPressed for arrow keys when no changes to send', () => {
      directive.boundsUpdating = false;
      directive.isSendChanges = false;
      directive.onKeyup({ keyCode: 38, shiftKey: false } as KeyboardEvent);
      expect(editorSession.keyPressed).toHaveBeenCalled();
    });

    it('should not call keyPressed for non-arrow keys', () => {
      directive.boundsUpdating = false;
      directive.isSendChanges = false;
      directive.onKeyup({ keyCode: 65, shiftKey: false } as KeyboardEvent);
      expect(editorSession.keyPressed).not.toHaveBeenCalled();
    });
  });
});
