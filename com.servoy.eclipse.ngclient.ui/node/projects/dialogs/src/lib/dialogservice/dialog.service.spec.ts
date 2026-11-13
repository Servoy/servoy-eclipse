import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed, inject } from '@angular/core/testing';

import { ServoyPublicService } from '@servoy/public';

import { DialogService } from './dialogs.service';

describe('DialogService', () => {
  let servoyService: any;
  let showMessageDialog: ReturnType<typeof vi.fn>;
  let listenForI18NMessages: ReturnType<typeof vi.fn>;

  beforeEach(() => {
    showMessageDialog = vi.fn().mockResolvedValue('OK');
    listenForI18NMessages = vi.fn();
    servoyService = {
      showMessageDialog,
      listenForI18NMessages,
    };
    TestBed.configureTestingModule({
      providers: [DialogService, { provide: ServoyPublicService, useFactory: () => servoyService }],
    });
  });

  it('should be created', inject([DialogService], (service: DialogService) => {
    expect(service).toBeTruthy();
    expect(service.i18nOK).toBe('OK');
    expect(service.i18nCancel).toBe('Cancel');
  }));

  describe('init', () => {
    it('should register an i18n listener and update the OK/Cancel labels from the messages', inject([DialogService], (service: DialogService) => {
      let messagesCallback: (messages: Map<string, string>) => void = null!;
      listenForI18NMessages.mockReturnValue({
        messages: (cb: (messages: Map<string, string>) => void) => {
          messagesCallback = cb;
          return {} as any;
        }
      });

      service.init();

      expect(listenForI18NMessages).toHaveBeenCalledWith('servoy.button.cancel', 'servoy.button.ok');
      expect(messagesCallback).toBeTypeOf('function');

      messagesCallback(new Map([['servoy.button.ok', 'Oké'], ['servoy.button.cancel', 'Annuleren']]));
      expect(service.i18nOK).toBe('Oké');
      expect(service.i18nCancel).toBe('Annuleren');
    }));

    it('should fall back to empty strings when the i18n messages are missing', inject([DialogService], (service: DialogService) => {
      let messagesCallback: (messages: Map<string, string>) => void = null!;
      listenForI18NMessages.mockReturnValue({
        messages: (cb: (messages: Map<string, string>) => void) => {
          messagesCallback = cb;
          return {} as any;
        }
      });

      service.init();
      messagesCallback(new Map());

      expect(service.i18nOK).toBe('');
      expect(service.i18nCancel).toBe('');
    }));

    it('should swallow errors thrown by listenForI18NMessages', inject([DialogService], (service: DialogService) => {
      const consoleError = vi.spyOn(console, 'error').mockImplementation(() => undefined);
      listenForI18NMessages.mockImplementation(() => { throw new Error('boom'); });

      expect(() => service.init()).not.toThrow();
      expect(consoleError).toHaveBeenCalled();

      consoleError.mockRestore();
    }));
  });

  describe('showErrorDialog', () => {
    it('should call showMessageDialog with the type-error style class and no values', inject([DialogService], async (service: DialogService) => {
      const result = await service.showErrorDialog('title', 'message', ['Yes', 'No']);

      expect(result).toBe('OK');
      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-error', [], ['Yes', 'No'], '', 0, 'OK');
    }));
  });

  describe('showInfoDialog', () => {
    it('should call showMessageDialog with the type-info style class', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['Close']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['Close'], '', 0, 'OK');
    }));
  });

  describe('showQuestionDialog', () => {
    it('should call showMessageDialog with the type-question style class', inject([DialogService], async (service: DialogService) => {
      await service.showQuestionDialog('title', 'message', ['Yes', 'No']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-question', [], ['Yes', 'No'], '', 0, 'OK');
    }));
  });

  describe('showWarningDialog', () => {
    it('should call showMessageDialog with the type-warning style class', inject([DialogService], async (service: DialogService) => {
      await service.showWarningDialog('title', 'message', ['Ok']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-warning', [], ['Ok'], '', 0, 'OK');
    }));
  });

  describe('showInputDialog', () => {
    it('should use the Cancel/OK buttons and pass the init value as a single-element values array', inject([DialogService], async (service: DialogService) => {
      await service.showInputDialog('title', 'message', 'initial', 'text');

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-input', ['initial'], ['Cancel', 'OK'], 'text', 0, 'OK');
    }));

    it('should pass an empty values array when the init value is null', inject([DialogService], async (service: DialogService) => {
      await service.showInputDialog('title', 'message', null as any, 'text');

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-input', [], ['Cancel', 'OK'], 'text', 0, 'OK');
    }));

    it('should pass an empty values array when the init value is undefined', inject([DialogService], async (service: DialogService) => {
      await service.showInputDialog('title', 'message', undefined as any, 'text');

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-input', [], ['Cancel', 'OK'], 'text', 0, 'OK');
    }));

    it('should keep an empty-string init value as a single-element values array', inject([DialogService], async (service: DialogService) => {
      await service.showInputDialog('title', 'message', '', 'text');

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-input', [''], ['Cancel', 'OK'], 'text', 0, 'OK');
    }));

    it('should default the title to "Enter value" when none is provided', inject([DialogService], async (service: DialogService) => {
      await service.showInputDialog('', 'message', 'initial', 'text');

      expect(showMessageDialog).toHaveBeenCalledWith('Enter value', 'message', 'type-input', ['initial'], ['Cancel', 'OK'], 'text', 0, 'OK');
    }));
  });

  describe('showSelectDialog', () => {
    it('should call showMessageDialog with the type-select style class and the options as values', inject([DialogService], async (service: DialogService) => {
      await service.showSelectDialog('title', 'message', ['a', 'b', 'c']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-select', ['a', 'b', 'c'], ['Cancel', 'OK'], '', 0, 'OK');
    }));
  });

  describe('button text / default button index handling', () => {
    it('should fall back to the i18nOK button when no button text is provided', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', []);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['OK'], '', 0, 'OK');
    }));

    it('should fall back to the i18nOK button when button text is null', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', null as any);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['OK'], '', 0, 'OK');
    }));

    it('should treat a trailing numeric item as the default button index and strip it from the buttons', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['One', 'Two', 'Three', '2']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['One', 'Two', 'Three'], '', 2, 'OK');
    }));

    it('should clamp an out-of-range default button index back to 0', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['One', 'Two', 'Three', '9']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['One', 'Two', 'Three'], '', 0, 'OK');
    }));

    it('should clamp a negative default button index back to 0', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['One', 'Two', 'Three', '-1']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['One', 'Two', 'Three'], '', 0, 'OK');
    }));

    it('should not treat a trailing non-numeric item as a default button index', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['One', 'Two', 'Three']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['One', 'Two', 'Three'], '', 0, 'OK');
    }));

    it('should not treat a trailing non-integer numeric item as a default button index', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['One', 'Two', 'Three', '1.5']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['One', 'Two', 'Three', '1.5'], '', 0, 'OK');
    }));

    it('should not look for a default button index when there are 2 or fewer buttons', inject([DialogService], async (service: DialogService) => {
      await service.showInfoDialog('title', 'message', ['One', '1']);

      expect(showMessageDialog).toHaveBeenCalledWith('title', 'message', 'type-info', [], ['One', '1'], '', 0, 'OK');
    }));
  });
});
