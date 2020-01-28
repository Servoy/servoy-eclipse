import { BSWindow } from './bswindow.service';
import { Injectable, Inject, RendererFactory2, Renderer2 } from "@angular/core";
import { DOCUMENT } from "@angular/common";
import { SvyUtilsService } from "../utils.service";

@Injectable()
export class BSWindowManager {

    zIndexIncrement = 100;
    options: any;
    windows: any;
    modalStack: any;
    
    private renderer: Renderer2;

    constructor(@Inject(DOCUMENT) private document: Document,
            private rendererFactory: RendererFactory2, 
            private bsWindow : BSWindow,
            private utils: SvyUtilsService) {
        this.windows = [];
        this.modalStack = [];
        this.initialize({});
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    findWindowByID(id) {
        var returnValue = null;
        Array.prototype.forEach.call(this.windows, function(window, index){
            console.log(arguments);
            if (window.id === id) {
                returnValue = window;
            }
        });
        return returnValue;
    }

    destroyWindow(window_handle) {
        var _this = this;
        this.removeModal(window_handle);
        Array.prototype.forEach.call(this.windows, function(window, index){
            if (window === window_handle) {
                _this.windows.splice(index, 1);
                _this.resortWindows();
            }
        });
    }

    resortWindows() {
        var startZIndex = 900;
        Array.prototype.forEach.call(this.windows, (window, index) => {
            window.setIndex(startZIndex + index * this.zIndexIncrement);
        });
        
        if(this.modalStack.length>0){
            //update modal backdrop z-index.
            var lastWindowZindex = parseInt(this.modalStack[this.modalStack.length-1].element.style.zIndex);
            var backdropModals = this.document.getElementsByClassName('modal-backdrop');
           Array.from(backdropModals).forEach((el, index) => {
                this.renderer.setStyle(el, 'z-index', lastWindowZindex - 1);
            });
        }
    }

    setFocused(focused_window) {
        var focusedWindowIndex;
        while (focused_window.getBlocker()) {
            focused_window = focused_window.getBlocker();
        }
        Array.prototype.forEach.call(this.windows, function(windowHandle, index){
            windowHandle.setActive(false);
            if (windowHandle === focused_window) {
                focusedWindowIndex = index;
            }
        });
        if(this.modalStack.indexOf(focused_window) == -1){
            this.windows.push(this.windows.splice(focusedWindowIndex, 1)[0]);
        }        
        focused_window.setActive(true);
        this.resortWindows();

    }

    /**
     * moves the window to the back of the stack (stops at the first modal window)
     * */
    sendToBack(window) {
        //move the BS window instance to the front of the array
        var from = this.windows.indexOf(window)
        var toWindow = this.modalStack.length > 0 ? this.modalStack[this.modalStack.length-1] : null;
        var to = toWindow ? this.windows.indexOf(toWindow)+1: 0;
        this.windows.splice(to/*to*/, 0, this.windows.splice(from, 1)[0]);
        this.setFocused(this.windows[this.windows.length-1]);
   }

   initialize(options) {
       this.options = options;
       if (this.options.container) {
           [...this.document.querySelectorAll(this.options.container)].forEach((element => {
               this.renderer.addClass(element, 'window-pane');
           }));
       }
       if(!this.options.backdrop){
           this.options.modalBackdrop = this.document.createElement('div');
           this.options.modalBackdrop.setAttribute('class', 'modal-backdrop fade');
           this.options.modalBackdrop.setAttribute('style', 'z-index: 899');
       }
   }

   setNextFocused = function () {
       this.setFocused(this.windows[this.windows.length-1]);
   }

   addWindow(window_object) {
        var _this = this;
        this.renderer.listen(window_object.getElement(), 'focused', () => {
            _this.setFocused(window_object);
        })
        this.renderer.listen(window_object.getElement(), 'close', () => {
            _this.destroyWindow(window_object);
            if (window_object.getWindowTab()) {
                window_object.getWindowTab().remove(); // change remove
            }
        })

        if (this.options.container) {
            let spanElement = this.document.createElement('span');
            spanElement.className = 'label label-default';
            spanElement.innerHTML = '<button class="close">x</button>';
            window_object.setWindowTab(spanElement);
            this.renderer.listen(window_object.getWindowTab().querySelector('.close'), 'click', () => {
                window_object.close();
            })
            this.renderer.listen(window_object.getWindowTab(), 'click', () => {
                _this.setFocused(window_object);
                if (window_object.getSticky()) {
                    window.scrollTo(0, window_object.getElement().offsetTop);
                }
            })
            this.options.container.appendChild(window_object.getWindowTab());
        }

        this.windows.push(window_object);
        window_object.setManager(this);
        this.setFocused(window_object);
        return window_object;
    }

    createWindow(window_options) {
        var _this = this;
        let final_options: any;
        final_options = this.utils.deepExtend([true, final_options, window_options, this.options]);
        if (this.options.windowTemplate && !final_options.template) {
            final_options.template = this.options.windowTemplate;
        }
        var newWindow = this.bsWindow;
        newWindow.setOptions(final_options);
        if(final_options.isModal) {
            this.addModal(newWindow);
        }                  
        return this.addWindow(newWindow);
    }
    
    addModal(windowObj) {
        /*PRIVATE FUNCTION*/
        if(this.modalStack.length  === 0) {
            this.document.body.appendChild(windowObj.options.modalBackdrop);
            setTimeout(() => {
                const backdrop = this.document.getElementsByClassName('modal-backdrop');
                Array.from(backdrop).forEach((el, index) => {
                     this.renderer.addClass(el, "in");
                 });
            },50);
        }
        this.modalStack.push(windowObj);
    }
    
    removeModal(windowObj) {
        /*PRIVATE FUNCTION*/
        var i = this.modalStack.indexOf(windowObj);
            if(i != -1) {
                this.modalStack.splice(i, 1);
                //also remove from dom with animation
                if(this.modalStack.length === 0) {
                    const backdrop = this.document.getElementsByClassName('modal-backdrop');
                    Array.from(backdrop).forEach((el, index) => {
                         this.renderer.removeClass(el, "in");
                     });
                    setTimeout(() => { 
                        Array.from(backdrop).forEach((el, index) => {
                            el.remove();
                        });
                    },50);
                }
            }
    }
}