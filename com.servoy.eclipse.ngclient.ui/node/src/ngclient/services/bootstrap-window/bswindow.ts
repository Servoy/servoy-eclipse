import { WindowRefService } from '@servoy/public';
import { Renderer2, RendererFactory2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { BSWindowManager } from './bswindow_manager.service';
import { SvyUtilsService } from '../../utils.service';

const NORTH = 1;
const SOUTH = 2;
const EAST = 4;
const WEST = 8;

const resizeConstants = { NORTH, SOUTH, EAST, WEST };

export class BSWindow {

    resizeAnchorClasses = {
        1: 'ns-resize', // NORTH
        2: 'ns-resize', // SOUTH
        4: 'ew-resize', // EAST
        8: 'ew-resize', // WEST
        5: 'nesw-resize', // N-E
        9: 'nwse-resize', // N-W
        6: 'nwse-resize', // S-E
        10: 'nesw-resize',// S-W
        0: ''
    };

    options: BSWindowOptions;
    element: HTMLElement;
    id: any;
    windowTab: any;

    resizing: boolean;
    moving: boolean;
    offset: any;
    window_info: any;

    onClose: () => void;
    mouseDownListenerElement: any;
    mouseDownListenerHandle: any;

    private renderer: Renderer2;
    constructor(private windowRefService: WindowRefService,
        rendererFactory: RendererFactory2,
        private utilsService: SvyUtilsService,
        @Inject(DOCUMENT) private doc: Document) {
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    setOptions(options: any) {
        options = options || {};
        const defaults = {
            selectors: {
                handle: '.window-header',
                title: '.window-title',
                body: '.window-body',
                footer: '.window-footer'
            },
            elements: {
                handle: null,
                title: null,
                body: null,
                footer: null
            },
            references: {
                body: this.utilsService.getMainBody() // Is there a better way?
            },
            parseHandleForTitle: true,
            title: 'No Title',
            bodyContent: '',
            footerContent: ''
        };

        this.options = this.utilsService.deepExtend([true, defaults, options]) as BSWindowOptions;
        this.options.references.window = this.windowRefService.nativeWindow;
        this.initialize(this.options);
    }

    initialize(options: BSWindowOptions) {
        const _this = this;

        if (options.fromElement) {
            if (options.fromElement instanceof HTMLElement) {
                this.element = options.fromElement;
            } else if (typeof options.fromElement) { // I don't get this ...
                this.element = options.fromElement;
            }
        } else if (options.template) {
            this.element = options.template;
        } else {
            throw new Error('No template specified for window.');
        }

        options.elements.handle = this.element.querySelector(options.selectors.handle);
        options.elements.title = this.element.querySelector(options.selectors.title);
        options.elements.body = this.element.querySelector(options.selectors.body);
        options.elements.footer = this.element.querySelector(options.selectors.footer);
        if (options.title) options.elements.title.innerHTML = options.title;
//        if (options.fromElement && _this.element.querySelectorAll('[data-dismiss=window]').length <= 0) {
//            options.elements.title.innerHTML += '<button class="close" data-dismiss="window">x</button>';
//        }
        if (options.bodyContent) options.elements.body.innerHTML = options.bodyContent;
        if (options.footerContent) options.elements.footer.innerHTML = options.footerContent;
        this.undock();
        this.setSticky(options.sticky);
    }

    undock() {
        const _this = this;
        this.renderer.setStyle(this.element, 'visibility', 'hidden');
        this.renderer.appendChild(this.utilsService.getMainBody(), this.element);
        if (!this.options.location) {
            //default positioning
            this.centerWindow();
        } else {
            //user entered options
            this.renderer.setStyle(this.element, 'left', this.options.location.left + 'px');
            this.renderer.setStyle(this.element, 'top', this.options.location.top + 'px');
        }
        if (this.options.size) {
            this.setSize(this.options.size);
        }
        if (/Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent)) {
            this.renderer.listen(this.options.references.window, 'orientationchange', () => _this.centerWindow());
            this.renderer.listen(this.options.references.window, 'resize', () => _this.centerWindow());
        }

        this.renderer.listen(this.element, 'touchmove', (e) => {
            e.stopPropagation();
        });

        this.initHandlers();
        this.renderer.setStyle(this.element, 'display', 'none');
        if (this.options.id) {
            this.id = this.options.id;
        } else {
            this.id = '';
        }
        this.show();
    }

    show() {
        this.renderer.setStyle(this.element, 'visibility', 'visible');
        this.renderer.setStyle(this.element, 'display', 'block');
        this.renderer.setStyle(this.element, 'transition', 'opacity 400ms');
    }

    setSize(size: { width: number; height: number }) {
        const winBody = this.element.querySelector(this.options.selectors.body);
        this.renderer.setStyle(winBody, 'min-width', size.width - this.getInteger(this.element.style.marginRight) - this.getInteger(this.element.style.marginLeft) + 'px');
        this.renderer.setStyle(winBody, 'min-height', size.height + 'px');
        this.renderer.setStyle(winBody, 'height', '1px');
    }

    centerWindow() {
        let top: number; let left: number;
        const bodyTop = this.options.references.body.offsetTop + this.getInteger(this.options.references.body.style.paddingTop);
        left = (this.options.references.window.innerWidth / 2) - (this.element.getBoundingClientRect().width / 2);
        top = (this.options.references.window.innerHeight / 2) - (this.element.getBoundingClientRect().height / 2);
        if (top < bodyTop) {
            top = bodyTop;
        }
        const maxHeight = ((this.options.references.body.getBoundingClientRect().height - bodyTop) - (this.getInteger(this.options.elements.handle.style.height) +
            this.getInteger(this.options.elements.footer.style.height))) - 45;
        this.renderer.setStyle(this.options.elements.body, 'maxHeight', maxHeight);
        this.renderer.setStyle(this.element, 'left', left+'px');
        this.renderer.setStyle(this.element, 'top', top+'px');
    }

    close() {
        const _this = this;
        if (this.options.window_manager && this.options.window_manager.modalStack.length === 1 && this.options.isModal) {
            const backdropModals = this.doc.getElementsByClassName('modal-backdrop');
            while (backdropModals[0]) {
                backdropModals[0].parentNode.removeChild(backdropModals[0]);
            }
        }
        const closeEvent = new Event('close');
        this.element.dispatchEvent(closeEvent);
        if (this.options.parent) {
            this.options.parent.clearBlocker();
            if (this.options.window_manager) {
                this.options.window_manager.setFocused(this.options.parent);
            }
        } else if (this.options.window_manager && this.options.window_manager.windows.length > 0) {
            this.options.window_manager.setNextFocused();
        }
        if (this.onClose){
            this.onClose();
        }
        _this.element.remove();
        if (this.windowTab) {
            _this.windowTab.remove();
        }
    }

    setActive(active: boolean) {
        if (active) {
            this.renderer.addClass(this.element, 'active');
            if (this.windowTab) {
                this.renderer.addClass(this.windowTab, 'label-primary');
            }

        } else {
            this.renderer.removeClass(this.element, 'active');
            if (this.windowTab) {
                this.renderer.removeClass(this.windowTab, 'label-primary');
                this.renderer.addClass(this.windowTab, 'label-default');
            }
        }

        // before: this.$el.trigger('bswin.active', active);
        // not sure if it's okay, is there a better way to trigger events manually in Angular?
        this.element.dispatchEvent(new CustomEvent<boolean>('bswin.active', { detail: active  }));
    }

    setIndex(index: number) {
        this.renderer.setStyle(this.element, 'zIndex', index);
    }

    setWindowTab(windowTab) {
        this.windowTab = windowTab;
    }

    getWindowTab() {
        return this.windowTab;
    }

    setTitle(title: string) {
        if(this.options) {
            this.options.title = title;
            if(this.options.elements && this.options.elements.title) {
                this.options.elements.title.innerHTML = title;
            }
        }
    }

    getTitle() {
        return this.options.title;
    }

    getElement() {
        return this.element;
    }

    setSticky(sticky: boolean) {
        this.options.sticky = sticky;
        this.renderer.setStyle(this.element, 'position', (sticky === false) ? 'absolute' : 'fixed');
    }

    getSticky() {
        return this.options.sticky;
    }

    setManager(window_manager: BSWindowManager) {
        this.options.window_manager = window_manager;
    }

    initHandlers() {
//        this.renderer.listen(this.element.querySelector('[data-dismiss=window]'), 'click', (e) => {
//            if (this.options.blocker) {
//                return;
//            }
//            this.close();
//        });
//

        if (this.mouseDownListenerElement) {
            this.mouseDownListenerElement();
        }
        this.mouseDownListenerElement = this.renderer.listen(this.element, 'mousedown', (event) => {
            const focusedEvent = new Event('focused');
            if (this.options.blocker) {
                this.options.blocker.getElement().dispatchEvent(focusedEvent);
                this.options.blocker.blink();
                return;
            } else {
                this.element.dispatchEvent(focusedEvent);
            }

            if (this.element.classList.contains(lastResizeClass)) {
                this.addClassToBodyChildren('disable-select');
                this.resizing = true;
                this.offset = {};
                this.offset.x = event.pageX - this.element.offsetLeft;
                this.offset.y = event.pageY - this.element.offsetTop;
                this.window_info = {
                    top: this.element.offsetTop,
                    left: this.element.offsetLeft,
                    width: this.element.getBoundingClientRect().width,
                    height: this.element.getBoundingClientRect().height
                };

                let offX = event.offsetX;
                let offY = event.offsetY;
                if (!event.offsetX && event.offsetX !== 0) {
                    // FireFox Fix
                    offX = event.originalEvent.layerX;
                    offY = event.originalEvent.layerY;
                }

                const rectTarget = event.target.getBoundingClientRect();
                const rectElement = this.element.getBoundingClientRect();
                const windowOffsetX = (rectTarget.left + this.utilsService.getMainBody().scrollLeft) - (rectElement.left + this.utilsService.getMainBody().scrollLeft);
                const windowOffsetY = (rectTarget.top + this.utilsService.getMainBody().scrollTop) - (rectElement.top + this.utilsService.getMainBody().scrollTop);

                if (offY + windowOffsetY < 5) {
                    this.renderer.addClass(this.element, 'north');
                }
                if (offY + windowOffsetY > (this.element.getBoundingClientRect().height - 5)) {
                    this.renderer.addClass(this.element, 'south');
                }
                if (offX + windowOffsetX < 5) {
                    this.renderer.addClass(this.element, 'west');
                }
                if (offX + windowOffsetX > (this.element.getBoundingClientRect().width - 5)) {
                    this.renderer.addClass(this.element, 'east');
                }
            }
        });

        this.renderer.listen(this.options.references.body, 'mouseup', () => {
            this.resizing = false;
            this.moving = false;
            this.removeClassToBodyChildren('disable-select');
            this.renderer.removeClass(this.element, 'west');
            this.renderer.removeClass(this.element, 'east');
            this.renderer.removeClass(this.element, 'north');
            this.renderer.removeClass(this.element, 'south');
            const width = this.element.querySelector(this.options.selectors.body).getBoundingClientRect().width;
            const height = this.element.querySelector(this.options.selectors.body).getBoundingClientRect().height;

            const size = { width, height };
            // before: this.element.trigger('bswin.resize',size);
            // not sure if it's okay, is there a better way to trigger events manually in Angular?
            this.element.dispatchEvent(new CustomEvent('bswin.resize', { detail: size  }));
        });
        if (this.mouseDownListenerHandle) {
            this.mouseDownListenerHandle();
        }
        this.mouseDownListenerHandle = this.renderer.listen(this.options.elements.handle, 'mousedown', (event) => {
            const handleHeight = this.options.elements.handle.offsetHeight;
            const handleWidth = this.options.elements.handle.offsetWidth;
            let offX = event.offsetX;
            let offY = event.offsetY;
            if (!event.offsetX && event.offsetX !== 0) {
                // FireFox Fix
                offX = event.originalEvent.layerX;
                offY = event.originalEvent.layerY;
            }
            if (this.options.blocker ||
                offY < 5 ||
                handleHeight - offY < 5 ||
                offX < 5 ||
                handleWidth - offX < 5) {
                return;
            }
            this.moving = true;
            this.offset = {};
            this.offset.x = event.pageX - this.element.offsetLeft;
            this.offset.y = event.pageY - this.element.offsetTop;
            this.addClassToBodyChildren('disable-select');
        });

        this.renderer.listen(this.options.elements.handle, 'mouseup', () => {
            this.moving = false;
            this.removeClassToBodyChildren('disable-select');
            const pos = { y: this.element.offsetTop, x: this.element.offsetLeft };
            this.element.dispatchEvent(new CustomEvent<{x: number; y: number}>('bswin.move', { detail: pos }));
        });

        this.renderer.listen(this.options.references.body, 'mousemove', (event: MouseEvent) => {
            if (this.moving) {
                this.renderer.setStyle(this.element, 'top', (event.pageY - this.offset.y) + 'px');
                this.renderer.setStyle(this.element, 'left', (event.pageX - this.offset.x) + 'px');
            }
            if (this.options.resizable && this.resizing) {
                const winBody = this.element.querySelector(this.options.selectors.body);
                let winHeadFootHeight = 0;
                const head = this.element.querySelector(this.options.selectors.handle) as HTMLElement;
                if (head) {
                    winHeadFootHeight += head.offsetHeight;
                }
                const foot = this.element.querySelector(this.options.selectors.footer) as HTMLElement;
                if (foot) {
                    winHeadFootHeight += foot.offsetHeight;
                }
                if (this.element.classList.contains('east')) {
                    this.renderer.setStyle(winBody, 'width', (event.pageX - this.window_info.left) + 'px');
                }
                if (this.element.classList.contains('west')) {
                    this.renderer.setStyle(winBody, 'width', this.window_info.width + (this.window_info.left - event.pageX) + 'px');
                    this.renderer.setStyle(this.element, 'left', event.pageX + 'px');
                }
                if (this.element.classList.contains('south')) {
                    this.renderer.setStyle(winBody, 'height', (event.pageY - this.window_info.top - winHeadFootHeight) + 'px');
                }
                if (this.element.classList.contains('north')) {
                    this.renderer.setStyle(winBody, 'height', (this.window_info.height + (this.window_info.top - event.pageY) - winHeadFootHeight) + 'px');
                    this.renderer.setStyle(this.element, 'top', event.pageY + 'px');
                }
            }
        });

        this.renderer.listen(this.options.references.body, 'mouseleave', () => {
            this.moving = false;
            this.removeClassToBodyChildren('disable-select');
        });

        let lastResizeClass = '';
        this.renderer.listen(this.element, 'mousemove', (event) => {
            if (this.options.blocker) {
                return;
            }
            if (this.options.resizable) {
                let resizeClassIdx = 0;
                //target can be the header or footer, and event.offsetX/Y will be relative to the header/footer .Adjust to '.window';
                const rectTarget = event.target.getBoundingClientRect();
                const rectElement = this.element.getBoundingClientRect();
                const windowOffsetX = (rectTarget.left + this.utilsService.getMainBody().scrollLeft) - (rectElement.left + this.utilsService.getMainBody().scrollLeft);
                const windowOffsetY = (rectTarget.top + this.utilsService.getMainBody().scrollTop) - (rectElement.top + this.utilsService.getMainBody().scrollTop);

                let offX = event.offsetX;
                let offY = event.offsetY;
                if (!event.offsetX && event.offsetX !== 0) {
                    // FireFox Fix
                    offX = event.originalEvent.layerX;
                    offY = event.originalEvent.layerY;
                }
                if (offY + windowOffsetY > (this.element.getBoundingClientRect().height - 5)) {
                    resizeClassIdx += resizeConstants.SOUTH;
                }
                if (offY + windowOffsetY < 5) {
                    resizeClassIdx += resizeConstants.NORTH;
                }
                if (offX + windowOffsetX > this.element.getBoundingClientRect().width - 5) {
                    resizeClassIdx += resizeConstants.EAST;
                }
                if (offX + windowOffsetX < 5) {
                    resizeClassIdx += resizeConstants.WEST;
                }
                if (lastResizeClass !== '' && lastResizeClass !== undefined && lastResizeClass != null) {
                    this.renderer.removeClass(this.element, lastResizeClass);
                }
                lastResizeClass = this.resizeAnchorClasses[resizeClassIdx];
                if (lastResizeClass !== '' && lastResizeClass !== undefined && lastResizeClass != null) {
                    this.renderer.addClass(this.element, lastResizeClass);
                }
            }
        });
    }

    resize(options) {
        options = options || {};
        if (options.top) {
            this.renderer.setStyle(this.element, 'top', options.top);
        }
        if (options.left) {
            this.renderer.setStyle(this.element, 'left', options.left);
        }
        this.setSize({ height: options.height, width: options.width });
    }

    setBlocker(window_handle: BSWindow) {
        this.options.blocker = window_handle;
        this.element.querySelector('.disable-shade').remove();
        const shade = '<div class="disable-shade"></div>';
        this.options.elements.body.append(shade);
        this.renderer.addClass(this.options.elements.body, 'disable-scroll');
        this.options.elements.footer.append(shade);
        this.renderer.addClass(this.element.querySelector('.disable-shade'), 'show');
        if (!this.options.blocker.getParent()) {
            this.options.blocker.setParent(this);
        }
    }


    getBlocker() {
        return this.options.blocker;
    }

    clearBlocker() {
        this.renderer.removeClass(this.options.elements.body, 'disable-scroll');
        this.renderer.addClass(this.element.querySelector('.disable-shade'), 'hide');
        delete this.options.blocker;
    }

    setParent(window_handle: BSWindow) {
        this.options.parent = window_handle;
        if (!this.options.parent.getBlocker()) {
            this.options.parent.setBlocker(this);
        }
    }

    getParent(): BSWindow {
        return this.options.parent;
    }

    blink() {
        const _this = this;
        const active = this.element.classList.contains('active');

        const blinkInterval = setInterval(() => {
            _this.element.classList.toggle('active');
        }, 250);
        const blinkTimeout = setTimeout(() => {
            clearInterval(blinkInterval);
            if (active) {
                this.renderer.addClass(_this.element, 'active');
            }
        }, 1000);
    }

    private addClassToBodyChildren(cls: string) {
        const childNodesBody = this.utilsService.getMainBody().childNodes;
        for (const key of Object.keys(childNodesBody)) {
            if (childNodesBody[key] instanceof Element) {
                const node = childNodesBody[key] as Element;
                node.classList.add(cls);
            }
        }
    }

    private getInteger(value: any) {
        if (value) {
            const parsed = parseInt(value, 10);
            if (isNaN(parsed)) return 0;
            return parsed;
        }
        return 0;
    }

    private removeClassToBodyChildren(cls: string) {
        const childNodesBody = this.utilsService.getMainBody().childNodes;
        for (const key of Object.keys(childNodesBody)) {
            if (childNodesBody[key] instanceof Element) {
                const node = childNodesBody[key] as Element;
                node.classList.remove(cls);
            }
        }
    }
}

export interface BSWindowOptions {
    id?: string;
    size?: { width: number; height: number };
    location?: {left: number; top: number};
    fromElement?: HTMLElement;
    template?: HTMLElement;
    bodyContent?: string;
    footerContent?: string;
    selectors?: {handle: string; title: string; body: string; footer: string};
    elements?: {handle: HTMLElement; title: HTMLElement; body: HTMLElement; footer: HTMLElement};
    title?: string;
    sticky?: boolean;
    references?: {body: HTMLElement; window: Window };
    parent?: BSWindow;
    blocker?: BSWindow;
    resizable?: boolean;
    isModal?: boolean;
    window_manager?: BSWindowManager;
    modalBackdrop?: HTMLElement;
}
