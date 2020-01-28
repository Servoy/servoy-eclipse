import { WindowRefService } from "../../../sablo/util/windowref.service";
import { Renderer2, Injectable, RendererFactory2, Inject } from "@angular/core";
import { DOCUMENT } from "@angular/common";
import { SvyUtilsService } from "../utils.service";

const NORTH = 1;
const SOUTH = 2;
const EAST = 4;
const WEST = 8;

const resizeConstants = {NORTH, SOUTH, EAST, WEST};

@Injectable()
export class BSWindow {

    resizeAnchorClasses = {
        '1':'ns-resize', // NORTH
        '2':'ns-resize', // SOUTH
        '4':'ew-resize', // EAST
        '8':'ew-resize', // WEST
        '5':'nesw-resize', // N-E
        '9':'nwse-resize', // N-W
        '6':'nwse-resize', // S-E
        '10':'nesw-resize',// S-W
        '0':''
    };

    options: any;
    element: any;
    id: any;
    windowTab: any;

    resizing: boolean;
    moving: boolean;
    offset: any;
    window_info: any;
    
    private renderer: Renderer2;

    constructor(private windowRefService: WindowRefService,
                private rendererFactory: RendererFactory2,
                private utilsService: SvyUtilsService,
                @Inject(DOCUMENT) private document: any) {
        this.renderer = rendererFactory.createRenderer(null, null);
    }

    
    private addClassToBodyChildren(cls:String) {
        let childNodesBody = this.document.body.childNodes;
        for(let i = 0; i < childNodesBody.length; i++) {
            if (childNodesBody[i] instanceof Element) {
                childNodesBody[i].classList.add(cls);
            }
        }
    }
    
    private getInteger(value:any) {
        if (value) {
            const parsed = parseInt(value, 10);
            if (parsed == NaN) return 0;
            return parsed;
        }
        return 0;
    }
    
    private removeClassToBodyChildren(cls) {
        let childNodesBody = this.document.body.childNodes;
        for(let i = 0; i < childNodesBody.length; i++) {
            if (childNodesBody[i] instanceof Element) {
                childNodesBody[i].classList.add(cls);
            }
        }
    }
    
    setOptions(options) {
        options = options || {};
        var defaults = {
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
                body: this.document.body, // Is there a better way?
                window: this.windowRefService
            },
            parseHandleForTitle: true,
            title: 'No Title',
            bodyContent: '',
            footerContent: ''
        };

        this.options = this.utilsService.deepExtend([true, defaults, options]);
        this.initialize(this.options);
    }

    initialize(options) {
        var _this = this;

        if (options.fromElement) {
            if (options.fromElement instanceof Element) {
                this.element = options.fromElement
            } else if (typeof options.fromElement) { // I don't get this ... 
                this.element = options.fromElement;
            }
        } else if (options.template) {
            this.element = options.template;
        } else {
            throw new Error("No template specified for window.");
        }

        options.elements.handle = this.element.querySelector(options.selectors.handle);
        options.elements.title = this.element.querySelector(options.selectors.title);
        options.elements.body = this.element.querySelector(options.selectors.body);
        options.elements.footer = this.element.querySelector(options.selectors.footer);
        options.elements.title.innerHTML = options.title // How ... ?
        if (options.fromElement && _this.element.querySelectorAll('[data-dismiss=window]').length <= 0) {
            options.elements.title.innerHTML += '<button class="close" data-dismiss="window">x</button>';
        }
        if(options.bodyContent)options.elements.body.innerHTML = options.bodyContent;
        if(options.footerContent)options.elements.footer.innerHTML = options.footerContent;
        this.undock();
        this.setSticky(options.sticky);
    }

    undock() {
        var _this = this;
        this.renderer.setStyle(this.element, 'visibility', 'hidden');
        this.renderer.appendChild(this.document.body, this.element);
        if(!this.options.location){
            //default positioning
            this.centerWindow();        
        } else {
            //user entered options
            this.renderer.setStyle(this.element, 'left', this.options.location.left +"px");
            this.renderer.setStyle(this.element, 'top', this.options.location.top + "px");
        }
        if(this.options.size){
            this.setSize(this.options.size);
        }
        if( /Android|webOS|iPhone|iPad|iPod|BlackBerry|IEMobile|Opera Mini/i.test(navigator.userAgent) ) {
            this.options.references.window.bind('orientationchange resize', function(event){
                _this.centerWindow();
            });
        }
        
        this.renderer.listen(this.element, 'touchmove', (e) => {
            e.stopPropagation();
        })

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
        /* old 
         this.$el.css('visibility', 'visible');
         this.$el.fadeIn(0); 
         */
        
        // new 
        this.renderer.setStyle(this.element, 'visibility', 'visible');
        this.renderer.setStyle(this.element, 'display', 'block');
        this.renderer.setStyle(this.element, 'transition', 'opacity 400ms');
    }

    setSize(size) {
//        var winBody = this.$el.find(this.options.selectors.body);
//        winBody.css('width', size.width - parseInt(this.$el.css("marginRight")) - parseInt(this.$el.css("marginLeft")) );
//        winBody.css('height', size.height);
        
        // this should be changed
        var winBody = this.element.querySelector(this.options.selectors.body);
        this.renderer.setStyle(winBody, 'width', size.width - this.getInteger(this.element.style.marginRight) - this.getInteger(this.element.style.marginLeft) + "px");
        this.renderer.setStyle(winBody, 'height', size.height + "px");
    }

    centerWindow() {
        var top, left,
            bodyTop = parseInt(this.options.references.body.offsetTop, 10) + parseInt(this.options.references.body.style.paddingTop, 10),
            maxHeight;
        if (!this.options.sticky) {
            left = (this.options.references.window.getBoundingClientRect().width / 2) - (this.element.getBoundingClientRect().width / 2);
            top = (this.options.references.window.getBoundingClientRect().height / 2) - (this.element.getBoundingClientRect().height / 2);
        } else {
            left = (this.options.references.window.getBoundingClientRect().width / 2) - (this.element.getBoundingClientRect().width / 2);
            top = (this.options.references.window.getBoundingClientRect().height / 2) - (this.element.getBoundingClientRect().height / 2);
        }

        if (top < bodyTop) {
            top = bodyTop;
        }
        maxHeight = ((this.options.references.window.getBoundingClientRect().height - bodyTop) - (this.getInteger(this.options.elements.handle.style.height) + this.getInteger(this.options.elements.footer.style.height))) - 45;
        this.renderer.setStyle(this.options.elements.body, 'maxHeight', maxHeight);
        this.renderer.setStyle(this.element, 'left', left);
        this.renderer.setStyle(this.element, 'top', top);
    }

    close() {
        var _this = this;
        if(this.options.window_manager && this.options.window_manager.modalStack.length === 1) {
            let backdropModals = this.document.getElementsByClassName('.modal-backdrop');
            while(backdropModals[0]) {
                backdropModals[0].parentNode.removeChild(backdropModals[0]);
            }​
        }
        var closeEvent = new Event('close');
        this.element.dispatchEvent(closeEvent);
        if (this.options.parent) {
            this.options.parent.clearBlocker();
            if (this.options.window_manager) {
                this.options.window_manager.setFocused(this.options.parent);
            }
        } else if (this.options.window_manager && this.options.window_manager.windows.length > 0) {
            this.options.window_manager.setNextFocused();
        }
        _this.element.remove();
        if (this.windowTab) {
            _this.windowTab.remove();
        }
    }

    setActive(active) {
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
        this.element.dispatchEvent(new CustomEvent('bswin.active', {detail: {active: active}}));
    }

    setIndex(index) {
        this.renderer.setStyle(this.element, 'zIndex', index);
    }

    setWindowTab(windowTab) {
        this.windowTab = windowTab;
    }

    getWindowTab() {
        return this.windowTab;
    }

    getTitle() {
        return this.options.title;
    }

    getElement() {
        return this.element;
    }

    setSticky(sticky) {
        this.options.sticky = sticky;
        this.renderer.setStyle(this.element, 'position', (sticky === false) ? 'absolute' : 'fixed');
    }

    getSticky() {
        return this.options.sticky;
    }

    setManager(window_manager) {
        this.options.window_manager = window_manager;
    }

    initHandlers() {
        var thisCopy = this;
        this.renderer.listen(this.element.querySelector('[data-dismiss=window]'), 'click', (e) => {
            if (thisCopy.options.blocker) {
                return;
            }
            thisCopy.close();
        })

        this.renderer.listen(this.element, 'mousedown', (e) => {
            e.preventDefault();
            e.stopPropagation();
        })
        
        this.renderer.listen(this.element, 'mousedown', (event) => {
            var focusedEvent = new Event('focused');
            if (thisCopy.options.blocker) {
                thisCopy.options.blocker.getElement().dispatchEvent(focusedEvent);
                thisCopy.options.blocker.blink();
                return;
            } else {
                thisCopy.element.dispatchEvent(focusedEvent);
            }
            
            if (thisCopy.element.classList.contains(lastResizeClass)) {
                thisCopy.addClassToBodyChildren('disable-select');
                thisCopy.resizing = true;
                thisCopy.offset = {};
                thisCopy.offset.x = event.pageX - thisCopy.element.offsetLeft;
                thisCopy.offset.y = event.pageY - thisCopy.element.offsetTop;
                thisCopy.window_info = {
                    top: thisCopy.element.offsetTop,
                    left: thisCopy.element.offsetLeft,
                    width: thisCopy.element.getBoundingClientRect().width,
                    height: thisCopy.element.getBoundingClientRect().height
                };
                
                var offX = event.offsetX;
                var offY = event.offsetY;
                if (!event.offsetX){
                    // FireFox Fix
                    offX = event.originalEvent.layerX;
                    offY = event.originalEvent.layerY;
                }
                let rectTarget = event.target.getBoundingClientRect();
                var windowOffsetX = (rectTarget.left + this.document.body.scrollLeft) - (this.element.left + this.document.body.scrollLeft);
                var windowOffsetY = (rectTarget.top + this.document.body.scrollTop) - (this.element.top + this.document.body.scrollTop);

                if (offY + windowOffsetY < 5) {
                    this.renderer.addClass(thisCopy.element, 'north');
                }
                if (offY + windowOffsetY > (thisCopy.element.getBoundingClientRect().height - 5)) {
                    this.renderer.addClass(thisCopy.element, 'south');
                }
                if (offX + windowOffsetX < 5) {
                    this.renderer.addClass(thisCopy.element, 'west');
                }
                if (offX + windowOffsetX > (thisCopy.element.getBoundingClientRect().width - 5)) {
                    this.renderer.addClass(thisCopy.element, 'east');
                }
            }
        });

        this.renderer.listen(thisCopy.options.references.body, 'mouseup', () => {
            thisCopy.resizing = false;
            thisCopy.moving = false;
            thisCopy.addClassToBodyChildren('disable-select');
            this.renderer.addClass(thisCopy.element, 'west');
            this.renderer.addClass(thisCopy.element, 'east');
            this.renderer.addClass(thisCopy.element, 'north');
            this.renderer.addClass(thisCopy.element, 'south');
            var width = thisCopy.element.getBoundingClientRect().width;
            var height = thisCopy.element.getBoundingClientRect().height;
            
            var size = {width:width,height:height};            
            // before: thisCopy.element.trigger('bswin.resize',size);  
            // not sure if it's okay, is there a better way to trigger events manually in Angular?
            this.element.dispatchEvent(new CustomEvent('bswin.resize', {detail: {resize: size}}));
        });
        this.document.removeEventListener('mousedown', thisCopy.options.elements.handle, false);
        this.renderer.listen( thisCopy.options.elements.handle, 'mousedown', (event) => {
            var handleHeight = thisCopy.options.elements.handle.offsetHeight;
            var handleWidth = thisCopy.options.elements.handle.offsetWidth;
            var offX = event.offsetX;
            var offY = event.offsetY;
            if (!event.offsetX){
                // FireFox Fix
                offX = event.originalEvent.layerX;
                offY = event.originalEvent.layerY;
            }
            if (thisCopy.options.blocker ||
                offY < 5 ||
                handleHeight - offY < 5 || 
                offX < 5 ||
                handleWidth - offX < 5) {
                return;
            }
            thisCopy.moving = true;
            thisCopy.offset = {};
            thisCopy.offset.x = event.pageX - thisCopy.element.offsetLeft;
            thisCopy.offset.y = event.pageY - thisCopy.element.offsetTop;
            thisCopy.addClassToBodyChildren('disable-select');
        });
        
        this.renderer.listen(thisCopy.options.elements.handle, 'mouseup', (event) => {
            thisCopy.moving = false;
            thisCopy.removeClassToBodyChildren('disable-select');
            var pos = {top:thisCopy.element.offsetTop,left:thisCopy.element.offsetLeft};         
//            thisCopy.element.trigger('bswin.move',pos); // still need to be replaced
            this.element.dispatchEvent(new CustomEvent('bswin.move', {detail: {pos: pos}}));
        });

        this.renderer.listen(thisCopy.options.references.body, 'mousemove', (event) => {
            if (thisCopy.moving) {
                this.renderer.setStyle(thisCopy.element, 'top', (event.pageY - thisCopy.offset.y) + "px");
                this.renderer.setStyle(thisCopy.element, 'left', (event.pageX - thisCopy.offset.x) + "px");
            }
            if (thisCopy.options.resizable && thisCopy.resizing) {
                var winBody = thisCopy.element.querySelector(thisCopy.options.selectors.body);
                var winHeadFootHeight = 0;
                var head = thisCopy.element.querySelector(thisCopy.options.selectors.handle);
                if(head){
                    winHeadFootHeight += head.outerHeight();
                }
                var foot = thisCopy.element.querySelector(thisCopy.options.selectors.footer);
                if(foot){
                    winHeadFootHeight += foot.outerHeight();
                }
                if (thisCopy.element.classList.contains("east")) {
                    this.renderer.setStyle(winBody, 'width', event.pageX - thisCopy.window_info.left);
                }
                if (thisCopy.element.classList.contains("west")) {
                    this.renderer.setStyle(winBody, 'width', thisCopy.window_info.width + (thisCopy.window_info.left  - event.pageX));
                    this.renderer.setStyle(thisCopy.element, 'left', event.pageX);
                }
                if (thisCopy.element.classList.contains("south")) {
                    this.renderer.setStyle(winBody, 'height', event.pageY - thisCopy.window_info.top - winHeadFootHeight);
                }
                if (thisCopy.element.classList.contains("north")) {
                    this.renderer.setStyle(winBody, 'height', thisCopy.window_info.height + (thisCopy.window_info.top  - event.pageY) - winHeadFootHeight);
                    this.renderer.setStyle(thisCopy.element, 'top', event.pageY);
                }
            }
        });

        this.renderer.listen(thisCopy.options.references.body, 'mouseleave', (event) => {
              thisCopy.moving = false;
              thisCopy.removeClassToBodyChildren('disable-select');
        });

        var lastResizeClass = '';
        this.renderer.listen(this.element, 'mousemove', (event) => {
            if (thisCopy.options.blocker) {
                return;
            }
            if (thisCopy.options.resizable ) {
                var resizeClassIdx = 0;
                //target can be the header or footer, and event.offsetX/Y will be relative to the header/footer .Adjust to '.window';
                let rectTarget = event.target.getBoundingClientRect();
                var windowOffsetX = (rectTarget.left + this.document.body.scrollLeft) - (this.element.left + this.document.body.scrollLeft);
                var windowOffsetY = (rectTarget.top + this.document.body.scrollTop) - (this.element.top + this.document.body.scrollTop);
                
                var offX = event.offsetX;
                var offY = event.offsetY;
                if (!event.offsetX){
                    // FireFox Fix
                    offX = event.originalEvent.layerX;
                    offY = event.originalEvent.layerY;
                }
                if (offY + windowOffsetY > (thisCopy.element.getBoundingClientRect().height - 5) ) {
                    resizeClassIdx += resizeConstants.SOUTH;
                }
                if (offY + windowOffsetY< 5) {
                    resizeClassIdx += resizeConstants.NORTH;
                }
                if (offX + windowOffsetX> thisCopy.element.getBoundingClientRect().width - 5) {
                    resizeClassIdx += resizeConstants.EAST;
                }
                if (offX + windowOffsetX< 5)
                {
                    resizeClassIdx += resizeConstants.WEST;
                }
                this.renderer.removeClass(thisCopy.element, lastResizeClass);
                lastResizeClass=thisCopy.resizeAnchorClasses[resizeClassIdx];
                this.renderer.addClass(thisCopy.element, lastResizeClass);
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
        this.setSize({height:options.height,width:options.width});
    }

    setBlocker(window_handle) {
        this.options.blocker = window_handle;
        this.element.querySelector('.disable-shade').remove();
        var shade = '<div class="disable-shade"></div>';
        this.options.elements.body.append(shade);
        this.renderer.addClass(this.options.elements.body, 'disable-scroll');
        this.options.elements.footer.append(shade);
        this.element.querySelector('.disable-shade').fadeIn();
        if (!this.options.blocker.getParent()) {
            this.options.blocker.setParent(this);
        }
    }


    getBlocker() {
        return this.options.blocker;
    }

    clearBlocker() {
        this.renderer.removeClass(this.options.elements.body, 'disable-scroll');
        this.element.querySelector('.disable-shade').fadeOut(function () {
            this.remove();
        });
        delete this.options.blocker;
    }

    setParent(window_handle) {
        this.options.parent = window_handle;
        if (!this.options.parent.getBlocker()) {
            this.options.parent.setBlocker(this);
        }
    }

    getParent() {
        return this.options.parent;
    }

    blink() {
        var _this = this,
            active = this.element.classList.contains('active'),

            blinkInterval = setInterval(function () {
            _this.element.toggleClass('active');
        }, 250),
            blinkTimeout = setTimeout(function () {
            clearInterval(blinkInterval);
            if (active) {
                this.renderer.addClass(_this.element, 'active');
            }
        }, 1000);
    }
}
