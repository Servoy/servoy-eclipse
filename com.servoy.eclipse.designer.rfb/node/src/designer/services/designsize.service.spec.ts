import { describe, it, expect, beforeEach, vi } from 'vitest';

import { DesignSizeService } from './designsize.service';

describe('DesignSizeService', () => {
  let service: DesignSizeService;
  let editorSession: Record<string, ReturnType<typeof vi.fn>>;
  let editor: Record<string, ReturnType<typeof vi.fn>>;

  beforeEach(() => {
    editorSession = {
      setFormFixedSize: vi.fn().mockResolvedValue(null),
      getFormFixedSize: vi.fn().mockResolvedValue({ width: '', height: '' }),
      isAbsoluteFormLayout: vi.fn().mockReturnValue(false),
    };
    editor = {
      setContentSize: vi.fn(),
      setContentSizeFull: vi.fn(),
      getFormInitialWidth: vi.fn().mockReturnValue('960px'),
    };

    service = Object.create(DesignSizeService.prototype);
    (service as any).editorSession = editorSession;
    service.lastHeight = 'auto';
    service.isPortrait = true;
    service.setEditor(editor as any);
    service.btnCustomWidth = { text: '' } as any;
    service.btnCustomHeight = { text: 'auto' } as any;
  });

  describe('setSize', () => {
    it('should set content size on editor', () => {
      service.setSize('768px', '1024px');
      expect(editor.setContentSize).toHaveBeenCalledWith('768px', '1024px');
    });

    it('should update lastWidth and lastHeight', () => {
      service.setSize('768px', '1024px');
      expect(service.lastWidth).toBe('768px');
      expect(service.lastHeight).toBe('1024px');
    });

    it('should update btnCustomWidth and btnCustomHeight text', () => {
      service.setSize('320px', '568px');
      expect(service.btnCustomWidth.text).toBe('320px');
      expect(service.btnCustomHeight.text).toBe('568px');
    });

    it('should call editorSession.setFormFixedSize', () => {
      service.setSize('768px', '1024px');
      expect(editorSession.setFormFixedSize).toHaveBeenCalledWith({ 'width': '768px', 'height': '1024px' });
    });
  });

  describe('setEditor', () => {
    it('should store the editor reference', () => {
      const newEditor = { setContentSize: vi.fn() } as any;
      service.setEditor(newEditor);
      expect((service as any).editor).toBe(newEditor);
    });
  });

  describe('portrait/landscape toggle logic', () => {
    it('tablet should use portrait dimensions by default', () => {
      service.lastClicked = '';
      service.isPortrait = true;
      service.setSize('768px', '1024px');
      expect(editor.setContentSize).toHaveBeenCalledWith('768px', '1024px');
    });

    it('when lastClicked is Tablet, clicking again toggles portrait', () => {
      service.lastClicked = 'Tablet';
      service.isPortrait = true;
      service.isPortrait = !service.isPortrait;
      expect(service.isPortrait).toBe(false);
    });
  });
});
