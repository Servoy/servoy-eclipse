import { BSWindow, BSWindowOptions } from './bswindow';
import { Injectable, Inject, RendererFactory2, Renderer2, DOCUMENT } from '@angular/core';

import { WindowRefService } from '@servoy/public';
import { SvyUtilsService } from '../../utils.service';

@Injectable()
export class BSWindowManager {

    zIndexIncrement = 100;
    options: any;
    windows: Array<BSWindow>;
    modalStack: Array<BSWindow>;
    modalBackdropRemoverTimeout: any;

    private renderer: Renderer2;

    constructor(@Inject(DOCUMENT) private doc: Document,
            private rendererFactory: RendererFactory2,
            private utils: SvyUtilsService,
            private windowRefService: WindowRefService) {
        this.windows = [];
        this.modalStack = [];
        this.initialize({});
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    findWindowByID(id) {
        let returnValue = null;
        this.windows.forEach( (window) => {
            if (window.id === id) {
                returnValue = window;
            }
        });
        return returnValue;
    }

    destroyWindow(window_handle: BSWindow) {
        const _this = this;
        if (window_handle.options.isModal) {
            this.removeModal(window_handle);
        }
         this.windows.forEach((window, index) => {
            if (window === window_handle) {
                _this.windows.splice(index, 1);
                _this.resortWindows();
            }
        });
    }

    resortWindows() {
        const startZIndex = 900;
         this.windows.forEach( (window, index) => {
            window.setIndex(startZIndex + index * this.zIndexIncrement);
        });

        if(this.modalStack.length>0){
            //update modal backdrop z-index.
            const lastWindowZindex = parseInt(this.modalStack[this.modalStack.length-1].element.style.zIndex, 10);
            const backdropModals = this.doc.getElementsByClassName('modal-backdrop');
           Array.from(backdropModals).forEach((el, index) => {
                this.renderer.setStyle(el, 'z-index', lastWindowZindex - 1);
            });
        }
    }

    setFocused(focused_window: BSWindow) {
        let focusedWindowIndex: number;
        while (focused_window.getBlocker()) {
            focused_window = focused_window.getBlocker();
        }
         this.windows.forEach((windowHandle, index ) => {
            windowHandle.setActive(false);
            if (windowHandle === focused_window) {
                focusedWindowIndex = index;
            }
        });
        if(this.modalStack.indexOf(focused_window) === -1){
            this.windows.push(this.windows.splice(focusedWindowIndex, 1)[0]);
        }
        focused_window.setActive(true);
        this.resortWindows();

    }

    /**
     * moves the window to the back of the stack (stops at the first modal window)
     * */
    sendToBack(window: BSWindow) {
        //move the BS window instance to the front of the array
        const from = this.windows.indexOf(window);
        const toWindow = this.modalStack.length > 0 ? this.modalStack[this.modalStack.length-1] : null;
        const to = toWindow ? this.windows.indexOf(toWindow)+1: 0;
        this.windows.splice(to/*to*/, 0, this.windows.splice(from, 1)[0]);
        this.setFocused(this.windows[this.windows.length-1]);
   }

   initialize(options) {
       this.options = options;
       if (this.options.container) {
           Array.from(this.doc.querySelectorAll(this.options.container)).forEach((element => {
               this.renderer.addClass(element, 'window-pane');
           }));
       }
       if(!this.options.backdrop){
           this.options.modalBackdrop = this.doc.createElement('div');
           this.options.modalBackdrop.setAttribute('class', 'modal-backdrop fade');
           this.options.modalBackdrop.setAttribute('style', 'z-index: 899');
       }
   }

   setNextFocused = function() {
       this.setFocused(this.windows[this.windows.length-1]);
   };

   addWindow(window_object: BSWindow) {
        this.renderer.listen(window_object.getElement(), 'focused', () => {
            this.setFocused(window_object);
        });
        this.renderer.listen(window_object.getElement(), 'close', () => {
            this.destroyWindow(window_object);
            if (window_object.getWindowTab()) {
                window_object.getWindowTab().remove(); // change remove
            }
        });

        if (this.options.container) {
            const spanElement = this.doc.createElement('span');
            spanElement.className = 'label label-default';
            spanElement.innerHTML = '<button class="close">x</button>';
            window_object.setWindowTab(spanElement);
            this.renderer.listen(window_object.getWindowTab().querySelector('.close'), 'click', () => {
                window_object.close();
            });
            this.renderer.listen(window_object.getWindowTab(), 'click', () => {
                this.setFocused(window_object);
                if (window_object.getSticky()) {
                    window.scrollTo(0, window_object.getElement().offsetTop);
                }
            });
            this.options.container.appendChild(window_object.getWindowTab());
        }

        this.windows.push(window_object);
        window_object.setManager(this);
        this.setFocused(window_object);
        return window_object;
    }

    createWindow(window_options: BSWindowOptions) {
        let final_options = Object.create(window_options) as BSWindowOptions;
        if (this.options.windowTemplate && !final_options.template) {
            final_options.template = this.options.windowTemplate;
        }
        final_options = this.utils.deepExtend([true, final_options, window_options, this.options]) as BSWindowOptions; // can this be done generically in deepExtend?
        const newWindow = new BSWindow(this.windowRefService, this.rendererFactory, this.utils, this.doc);
        newWindow.setOptions(final_options);
        if(final_options.isModal) {
            this.addModal(newWindow);
        }
        return this.addWindow(newWindow);
    }

    addModal(windowObj: BSWindow) {
        /*PRIVATE FUNCTION*/
        if(this.modalStack.length  === 0) {
            if(this.modalBackdropRemoverTimeout) {
                clearTimeout(this.modalBackdropRemoverTimeout);
                this.modalBackdropRemoverTimeout = null;
            }
            this.utils.getMainBody().appendChild(windowObj.options.modalBackdrop);
            setTimeout(() => {
                const backdrop = this.doc.getElementsByClassName('modal-backdrop');
                Array.from(backdrop).forEach((el, index) => {
                     this.renderer.addClass(el, 'in');
                 });
            },50);
        }
        this.modalStack.push(windowObj);
    }

    removeModal(windowObj: BSWindow) {
        /*PRIVATE FUNCTION*/
        const i = this.modalStack.indexOf(windowObj);
            if(i !== -1) {
                this.modalStack.splice(i, 1);
                //also remove from dom with animation
                if(this.modalStack.length === 0) {
                    const backdrop = this.doc.getElementsByClassName('modal-backdrop');
                    Array.from(backdrop).forEach((el) => {
                         this.renderer.removeClass(el, 'in');
                     });
                    if(this.modalBackdropRemoverTimeout) {
                        clearTimeout(this.modalBackdropRemoverTimeout);
                        this.modalBackdropRemoverTimeout = null;
                    }
                    this.modalBackdropRemoverTimeout = setTimeout(() => {
                        this.modalBackdropRemoverTimeout = null;
                        Array.from(backdrop).forEach((el) => {
                            el.remove();
                        });
                    },50);
                }
            }
    }
}
