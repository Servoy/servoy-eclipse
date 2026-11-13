import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';
import { LoggerFactory, ServoyPublicService } from '@servoy/public';

import { WindowPluginService, PopupMenuShowCommand } from './window.service';
import { ShortcutService } from './shortcut.service';
import { PopupMenuService } from './popupmenu.service';

describe('WindowPluginService', () => {
    let service: WindowPluginService;
    let shortcutService: { add: ReturnType<typeof vi.fn>; remove: ReturnType<typeof vi.fn>; all_shortcuts: Record<string, unknown> };
    let popupMenuService: {
        initClosePopupHandler: ReturnType<typeof vi.fn>;
        initMenu: ReturnType<typeof vi.fn>;
        showMenu: ReturnType<typeof vi.fn>;
        showMenuAt: ReturnType<typeof vi.fn>;
    };
    let servoyService: {
        cancelFormPopup: ReturnType<typeof vi.fn>;
        createJSEvent: ReturnType<typeof vi.fn>;
        executeInlineScript: ReturnType<typeof vi.fn>;
        sendServiceChanges: ReturnType<typeof vi.fn>;
        showForm: ReturnType<typeof vi.fn>;
    };
    let logError: ReturnType<typeof vi.fn>;

    beforeEach(() => {
        shortcutService = { add: vi.fn(), remove: vi.fn(), all_shortcuts: {} };
        popupMenuService = {
            initClosePopupHandler: vi.fn(),
            initMenu: vi.fn(),
            showMenu: vi.fn(),
            showMenuAt: vi.fn()
        };
        servoyService = {
            cancelFormPopup: vi.fn(),
            createJSEvent: vi.fn().mockReturnValue({ formName: 'theForm' }),
            executeInlineScript: vi.fn().mockResolvedValue(undefined),
            sendServiceChanges: vi.fn(),
            showForm: vi.fn()
        };
        logError = vi.fn();
        const loggerFactory = { getLogger: vi.fn().mockReturnValue({ error: logError }) };

        TestBed.configureTestingModule({
            providers: [
                WindowPluginService,
                { provide: ShortcutService, useValue: shortcutService },
                { provide: PopupMenuService, useValue: popupMenuService },
                { provide: ServoyPublicService, useValue: servoyService },
                { provide: LoggerFactory, useValue: loggerFactory }
            ]
        });
        service = TestBed.inject(WindowPluginService);
    });

    afterEach(() => {
        vi.useRealTimers();
    });

    it('should be created', () => {
        expect(service).toBeTruthy();
    });

    describe('cancelFormPopup', () => {
        it('cancelFormPopup should delegate to servoyService with false', () => {
            service.cancelFormPopup();
            expect(servoyService.cancelFormPopup).toHaveBeenCalledWith(false);
        });

        it('cancelFormPopupInternal should pass through the flag', () => {
            service.cancelFormPopupInternal(true);
            expect(servoyService.cancelFormPopup).toHaveBeenCalledWith(true);
        });

        it('cancelForm should delegate with the form name', () => {
            service.cancelForm('frmA');
            expect(servoyService.cancelFormPopup).toHaveBeenCalledWith('frmA');
        });
    });

    describe('popupform', () => {
        it('setting a popupform should call showForm', () => {
            const popup = { form: 'x' } as any;
            service.popupform = popup;
            expect(service.popupform).toBe(popup);
            expect(servoyService.showForm).toHaveBeenCalledWith(popup);
        });

        it('setting a falsy popupform should not call showForm', () => {
            service.popupform = null as any;
            expect(servoyService.showForm).not.toHaveBeenCalled();
        });
    });

    describe('shortcuts', () => {
        it('should register a translated shortcut with the shortcut service', () => {
            service.shortcuts = [{ shortcut: 'control shift A' } as any];

            expect(service.shortcuts.length).toBe(1);
            expect(shortcutService.add).toHaveBeenCalledTimes(1);
            expect(shortcutService.add).toHaveBeenCalledWith('CTRL+SHIFT+A', expect.any(Function), expect.objectContaining({ propagate: true, disable_in_input: false }));
        });

        it('should not re-add a shortcut that is already present in all_shortcuts', () => {
            shortcutService.all_shortcuts['CTRL+A'] = {};
            service.shortcuts = [{ shortcut: 'control A' } as any];
            expect(shortcutService.add).not.toHaveBeenCalled();
        });

        it('should translate numpad shortcuts', () => {
            service.shortcuts = [{ shortcut: 'numpad5' } as any];
            expect(shortcutService.add).toHaveBeenCalledWith('numpad-5', expect.any(Function), expect.anything());
        });

        it('should not register anything when set to a falsy value', () => {
            service.shortcuts = null as any;
            expect(shortcutService.add).not.toHaveBeenCalled();
        });

        it('the registered handler should run the callback via executeInlineScript on timeout', () => {
            vi.useFakeTimers();
            const callback = { script: 'myscript', formname: 'frmFallback' };
            service.shortcuts = [{ shortcut: 'control A', callback, arguments: null, consumeEvent: false } as any];

            const handler = shortcutService.add.mock.calls[0][1] as (e: KeyboardEvent) => boolean;
            const target = document.createElement('div');
            const event = { target } as unknown as KeyboardEvent;

            const retValue = handler(event);
            expect(retValue).toBe(true);

            vi.advanceTimersByTime(10);
            // createJSEvent returned { formName: 'theForm' } so that form name is used
            expect(servoyService.executeInlineScript).toHaveBeenCalledWith('theForm', 'myscript', expect.any(Array));
        });

        it('the registered handler should return false when consumeEvent is set', () => {
            const callback = { script: 'myscript', formname: 'f' };
            service.shortcuts = [{ shortcut: 'control A', callback, arguments: null, consumeEvent: true } as any];

            const handler = shortcutService.add.mock.calls[0][1] as (e: KeyboardEvent) => boolean;
            const target = document.createElement('div');
            const retValue = handler({ target } as unknown as KeyboardEvent);
            expect(retValue).toBe(false);
        });

        it('the registered handler should skip when createJSEvent returns null', () => {
            vi.useFakeTimers();
            servoyService.createJSEvent.mockReturnValue(null);
            const callback = { script: 'myscript', formname: 'f' };
            service.shortcuts = [{ shortcut: 'control A', callback, arguments: null, consumeEvent: false } as any];

            const handler = shortcutService.add.mock.calls[0][1] as (e: KeyboardEvent) => boolean;
            handler({ target: document.createElement('div') } as unknown as KeyboardEvent);
            vi.advanceTimersByTime(10);
            expect(servoyService.executeInlineScript).not.toHaveBeenCalled();
        });

        it('the handler should dispatch a change event for a modifier shortcut', () => {
            vi.useFakeTimers();
            const target = document.createElement('input');
            const dispatchSpy = vi.spyOn(target, 'dispatchEvent');
            const callback = { script: 's', formname: 'f' };
            service.shortcuts = [{ shortcut: 'control A', callback, arguments: null, consumeEvent: false } as any];

            const handler = shortcutService.add.mock.calls[0][1] as (e: KeyboardEvent) => boolean;
            handler({ target } as unknown as KeyboardEvent);

            expect(dispatchSpy).toHaveBeenCalled();
            expect((dispatchSpy.mock.calls[0][0] as CustomEvent).type).toBe('change');
        });

        it('the handler should concat array arguments onto the jsEvent', () => {
            vi.useFakeTimers();
            const callback = { script: 's', formname: 'f' };
            service.shortcuts = [{ shortcut: 'control A', callback, arguments: ['a', 'b'], consumeEvent: false } as any];

            const handler = shortcutService.add.mock.calls[0][1] as (e: KeyboardEvent) => boolean;
            handler({ target: document.createElement('div') } as unknown as KeyboardEvent);
            vi.advanceTimersByTime(10);

            const passedArgs = servoyService.executeInlineScript.mock.calls[0][2] as any[];
            // jsEvent + the two array elements
            expect(passedArgs.length).toBe(3);
            expect(passedArgs[1]).toBe('a');
            expect(passedArgs[2]).toBe('b');
        });
    });

    describe('popupMenus / popupMenuShowCommand', () => {
        const makePopup = () => ({ name: 'menu1', cssClass: '', items: [] });

        it('setting popupMenus stores the value', () => {
            const menus = [makePopup()] as any;
            service.popupMenus = menus;
            expect(service.popupMenus).toBe(menus);
        });

        it('setting popupMenuShowCommand stores the value', () => {
            const cmd = { popupName: 'menu1', x: 1, y: 2 } as PopupMenuShowCommand;
            service.popupMenuShowCommand = cmd;
            expect(service.popupMenuShowCommand).toBe(cmd);
        });

        it('showPopupMenu (debounced) should call initMenu and showMenu when no elementId', () => {
            vi.useFakeTimers();
            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'menu1', x: 5, y: 6, positionTop: false } as PopupMenuShowCommand;

            vi.advanceTimersByTime(1);
            expect(popupMenuService.initClosePopupHandler).toHaveBeenCalled();
            expect(popupMenuService.initMenu).toHaveBeenCalledWith(expect.objectContaining({ name: 'menu1' }));
            expect(popupMenuService.showMenu).toHaveBeenCalledWith(5, 6, false);
        });

        it('showPopupMenu should use showMenuAt when an element with the id is found', () => {
            vi.useFakeTimers();
            const el = document.createElement('div');
            el.id = 'elemA';
            document.body.appendChild(el);

            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'menu1', elementId: 'elemA', positionTop: true } as PopupMenuShowCommand;

            vi.advanceTimersByTime(1);
            expect(popupMenuService.showMenuAt).toHaveBeenCalledWith(el, true);
            document.body.removeChild(el);
        });

        it('showPopupMenu should log an error when the elementId is not found', () => {
            vi.useFakeTimers();
            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'menu1', elementId: 'missing' } as PopupMenuShowCommand;

            vi.advanceTimersByTime(1);
            expect(logError).toHaveBeenCalledWith(expect.stringContaining('missing'));
            expect(popupMenuService.showMenu).not.toHaveBeenCalled();
            expect(popupMenuService.showMenuAt).not.toHaveBeenCalled();
        });

        it('showPopupMenu should offset by element rect when x and y are provided with an element', () => {
            vi.useFakeTimers();
            const el = document.createElement('div');
            el.id = 'elemB';
            document.body.appendChild(el);
            vi.spyOn(el, 'getBoundingClientRect').mockReturnValue({ x: 100, y: 200 } as DOMRect);

            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'menu1', elementId: 'elemB', x: 5, y: 6, positionTop: false } as PopupMenuShowCommand;

            vi.advanceTimersByTime(1);
            expect(popupMenuService.showMenu).toHaveBeenCalledWith(105, 206, false);
            document.body.removeChild(el);
        });

        it('showPopupMenu should do nothing when the popup name does not match', () => {
            vi.useFakeTimers();
            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'doesNotExist', x: 1, y: 2 } as PopupMenuShowCommand;

            vi.advanceTimersByTime(1);
            expect(popupMenuService.initMenu).not.toHaveBeenCalled();
            expect(popupMenuService.showMenu).not.toHaveBeenCalled();
        });

        it('the close handler should reset the command and send service changes', () => {
            vi.useFakeTimers();
            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'menu1', x: 1, y: 2 } as PopupMenuShowCommand;
            vi.advanceTimersByTime(1);

            const closeHandler = popupMenuService.initClosePopupHandler.mock.calls[0][0] as () => void;
            closeHandler();

            expect(service.popupMenuShowCommand).toBeNull();
            expect(servoyService.sendServiceChanges).toHaveBeenCalledWith('window', 'popupMenuShowCommand', null);
        });

        it('should debounce multiple rapid show commands into a single showPopupMenu run', () => {
            vi.useFakeTimers();
            service.popupMenus = [makePopup()] as any;
            service.popupMenuShowCommand = { popupName: 'menu1', x: 1, y: 2 } as PopupMenuShowCommand;
            service.popupMenuShowCommand = { popupName: 'menu1', x: 3, y: 4 } as PopupMenuShowCommand;

            vi.advanceTimersByTime(1);
            expect(popupMenuService.showMenu).toHaveBeenCalledTimes(1);
            expect(popupMenuService.showMenu).toHaveBeenCalledWith(3, 4, false);
        });
    });
});
