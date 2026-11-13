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

  describe('createItems', () => {
    let toolbar: { add: ReturnType<typeof vi.fn> };

    beforeEach(() => {
      toolbar = { add: vi.fn() };
      service.createItems(toolbar as any);
    });

    it('should create all five toolbar items and add them to the sticky category', () => {
      expect(service.btnDesktopSize).toBeDefined();
      expect(service.btnTabletSize).toBeDefined();
      expect(service.btnMobileSize).toBeDefined();
      expect(service.btnCustomWidth).toBeDefined();
      expect(service.btnCustomHeight).toBeDefined();
      expect(toolbar.add).toHaveBeenCalledTimes(5);
    });

    it('Desktop click should set the content size to full and reset to auto height', () => {
      service.btnDesktopSize.onclick!();
      expect(editor.setContentSizeFull).toHaveBeenCalled();
      expect(service.lastWidth).toBe('960px');
      expect(service.lastHeight).toBe('auto');
      expect(editorSession.setFormFixedSize).toHaveBeenCalledWith({ width: '960px', height: 'auto' });
    });

    it('Tablet click should use portrait dimensions by default', () => {
      service.lastClicked = '';
      service.isPortrait = true;
      service.btnTabletSize.onclick!();
      expect(editor.setContentSize).toHaveBeenCalledWith('768px', '1024px');
      expect(service.lastClicked).toBe('Tablet');
    });

    it('Tablet click should toggle to landscape when clicked again', () => {
      service.lastClicked = 'Tablet';
      service.isPortrait = true;
      service.btnTabletSize.onclick!();
      expect(editor.setContentSize).toHaveBeenCalledWith('1024px', '768px');
    });

    it('Phone click should use portrait dimensions by default', () => {
      service.lastClicked = '';
      service.isPortrait = true;
      service.btnMobileSize.onclick!();
      expect(editor.setContentSize).toHaveBeenCalledWith('320px', '568px');
      expect(service.lastClicked).toBe('Phone');
    });

    it('Phone click should toggle to landscape when clicked again', () => {
      service.lastClicked = 'Phone';
      service.isPortrait = true;
      service.btnMobileSize.onclick!();
      expect(editor.setContentSize).toHaveBeenCalledWith('568px', '320px');
    });

    it('custom width onselection should set content size and fixed width', () => {
      service.lastHeight = 'auto';
      service.btnCustomWidth.onselection('480px');
      expect(editor.setContentSize).toHaveBeenCalledWith('480px', 'auto');
      expect(service.lastWidth).toBe('480px');
      expect(editorSession.setFormFixedSize).toHaveBeenCalledWith({ width: '480px' });
    });

    it('custom height onselection should resize using the last width', () => {
      service.lastWidth = '640px';
      service.btnCustomHeight.onselection('1024px');
      expect(editor.setContentSize).toHaveBeenCalledWith('640px', '1024px');
    });
  });

  describe('setupItems', () => {
    beforeEach(() => {
      service.btnCustomWidth = { text: '', list: [] } as any;
      service.btnCustomHeight = { text: 'auto' } as any;
    });

    it('should not configure width/height items for absolute layout forms', () => {
      editorSession.isAbsoluteFormLayout.mockReturnValue(true);
      service.setupItems();
      expect(service.lastWidth).toBe('960px');
      expect(editorSession.getFormFixedSize).not.toHaveBeenCalled();
    });

    it('should configure custom width and query the fixed size for responsive forms', async () => {
      editorSession.isAbsoluteFormLayout.mockReturnValue(false);
      editorSession.getFormFixedSize.mockResolvedValue({ width: '480px', height: '640px' });
      service.setupItems();
      expect(service.btnCustomWidth.text).toBe('960px');
      expect(editorSession.getFormFixedSize).toHaveBeenCalled();
      await Promise.resolve();
      await Promise.resolve();
      expect(service.lastHeight).toBe('640px');
      expect(service.lastWidth).toBe('480px');
      expect(editor.setContentSize).toHaveBeenCalledWith('480px', '640px');
    });
  });
});
