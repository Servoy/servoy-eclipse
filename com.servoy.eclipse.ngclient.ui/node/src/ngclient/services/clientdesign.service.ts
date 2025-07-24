import { Injectable, Inject, DOCUMENT } from '@angular/core';
import { SabloService } from '../../sablo/sablo.service';
import { SvyUtilsService } from '../utils.service';


@Injectable()
export class ClientDesignService {

    currentForms: { property?: DragResize } = {};

    constructor(private sabloService: SabloService, private utils: SvyUtilsService, @Inject(DOCUMENT) private document: Document) {
    }

    public setFormInDesign(formname: string, names: Array<string>) {
        let dragresize = this.currentForms[formname];
        if (dragresize) return true;

        const x =  this.document.querySelector('[name="' + formname + '"]');

        if (!x) return false;

        dragresize = new DragResize('dragresize');
        this.currentForms[formname] = dragresize;
        //if(designChangeListener) designChangeListener(formname, true);
        const selectElement = (elm) => {
            if (elm.classList.contains('svy-wrapper'))
            {
                elm = elm.firstChild;
                let name = elm.getAttribute("name");
                if (! name) name = elm.getAttribute("ng-reflect-name");
                if (name) {
                    return names.indexOf(name) != -1;
                }
            }
            return false;
        };
        dragresize.isElement = selectElement;
        dragresize.isHandle = selectElement;
        dragresize.ondragfocus = (e) => {
            var jsevent = this.utils.createJSEvent(e, "ondrag");
            this.sabloService.callService("clientdesign", "onselect", { element: jsevent.elementName, formname: formname, event: jsevent }).then((result) => {
                if (!result) dragresize.deselect(true);
                else if (dragresize.resizeHandleSet) dragresize.resizeHandleSet(dragresize.element, true);
            });
        };
        dragresize.ondragend = (isResize, e) => {
            var jsevent = this.utils.createJSEvent(e, "ondrop");
            const domRect = dragresize.element.getBoundingClientRect();
            if (isResize) this.sabloService.callService("clientdesign", "onresize", {
                element: jsevent.elementName,
                location: { x: domRect.left, y: domRect.top },
                size: { width: domRect.width, height: domRect.height },
                formname: formname,
                event: jsevent
            })
            else this.sabloService.callService("clientdesign", "ondrop", {
                element: jsevent.elementName,
                location: { x: domRect.left, y: domRect.top },
                size: { width: domRect.width, height: domRect.height },
                formname: formname,
                event: jsevent
            })
        };
        dragresize.ondragstart = (e) => {
            var jsevent = this.utils.createJSEvent(e, "ondrag");
            this.sabloService.callService("clientdesign", "ondrag", { element: jsevent.elementName, formname: formname, event: jsevent })
        };
        dragresize.ondoubleclick = (e) => {
            var jsevent = this.utils.createJSEvent(e, "ondoubleclick");
            this.sabloService.callService("clientdesign", "ondoubleclick", { element: jsevent.elementName, formname: formname, event: jsevent })
        };
        dragresize.onrightclick = (e) => {
            var jsevent = this.utils.createJSEvent(e, "onrightclick");
            this.sabloService.callService("clientdesign", "onrightclick", { element: jsevent.elementName, formname: formname, event: jsevent })
        };

        dragresize.apply(x);
        return true;
    }

    removeFormDesign(formname: string) {
        const dragresize = this.currentForms[formname];
        if (dragresize) {
            this.currentForms[formname].destroy();
            delete this.currentForms[formname];
            //if(designChangeListener) designChangeListener(formname, false);
            return true;
        }
        return false;
    }

    isFormInDesign(formname: string) {
        return formname && (this.currentForms[formname] != null);
    }
    /*
    setDesignChangeCallback: function(designChangeListenerCallback) {
        designChangeListener = designChangeListenerCallback;    
    },*/

    recreateUI(formname: string, names: Array<string>) {
        const dragresize = this.currentForms[formname];
        if (dragresize) {
            this.currentForms[formname].destroy();
            delete this.currentForms[formname];
            // recreate ui of the actual form must be waited on.
            setTimeout(() =>
                this.setFormInDesign(formname, names)
            );
        }
    }
}
class DragResize {
    myName: string;                  // Name of the object.
    enabled = true;                   // Global toggle of drag/resize.
    handles = ['tl', 'tm', 'tr',
        'ml', 'mr', 'bl', 'bm', 'br']; // Array of drag handles: top/mid/bot/right.
    isElement: (el: HTMLElement) => boolean;                 // Function ref to test for an element.
    isHandle: (el: HTMLElement) => boolean;                  // Function ref to test for move handle.
    element: HTMLElement;                   // The currently selected element.
    node: Element;
    handle: Element;                  // Active handle reference of the element.
    minWidth = 10; minHeight = 10;     // Minimum pixel size of elements.
    minLeft = 0; maxLeft = 9999;       // Bounding box area, in pixels.
    minTop = 0; maxTop = 9999;
    zIndex = 1;                       // The highest Z-Index yet allocated.
    mouseX = 0; mouseY = 0;            // Current mouse position, recorded live.
    lastMouseX = 0; lastMouseY = 0;    // Last processed mouse positions.
    mOffX = 0; mOffY = 0;              // A known offset between position & mouse.
    elmX = 0; elmY = 0;                // Element position.
    elmW = 0; elmH = 0;                // Element size.
    allowBlur = true;                 // Whether to allow automatic blur onclick.
    ondragfocus: (e: MouseEvent) => void;               // Event handler functions.
    ondragstart: (e: MouseEvent) => void;
    ondragmove: (isResize: boolean) => void;
    ondragend: (isResize: boolean, e: MouseEvent) => void;
    ondragblur: () => void;
    ondoubleclick: (e: MouseEvent) => void;
    onrightclick: (e: MouseEvent) => void;

    mouseDownHandler: (e: MouseEvent) => void;
    mouseMoveHandler: (e: MouseEvent) => void;
    mouseUpHandler: (e: MouseEvent) => void;
    doubleClickHandler: (e: MouseEvent) => void;
    rightClickHandler: (e: MouseEvent) => void;

    startDragging: boolean;
    _i : number = 1;
     // *** DRAG/RESIZE CODE ***
    constructor(myName: string) {
        this.myName = myName;
    }

    addEvent(o, t, f, l) {
        const d = 'addEventListener', n = 'on' + t, rO = o, rT = t, rF = f, rL = l;
        if (o[d] && !l) return o[d](t, f, false);
        if (!o._evts) o._evts = {};
        if (!o._evts[t]) {
            o._evts[t] = o[n] ? { b: o[n] } : {};
            o[n] = new Function('e',
                'var r = true, o = this, a = o._evts["' + t + '"], i; for (i in a) {' +
                'o._f = a[i]; r = o._f(e||window.event) != false && r; o._f = null;' +
                '} return r');
            if (t != 'unload') this.addEvent(window, 'unload', () => {
                this.removeEvent(rO, rT, rF, rL);
            }, null);
        }
        if (!f._i) f._i = this._i++;
        o._evts[t][f._i] = f;
    };

    removeEvent(o, t, f, l) {
        const d = 'removeEventListener';
        if (o[d] && !l) return o[d](t, f, false);
        if (o._evts && o._evts[t] && f._i) delete o._evts[t][f._i];
    }


    cancelEvent(e, c) {
        e.returnValue = false;
        if (e.preventDefault) e.preventDefault();
        if (c) {
            e.cancelBubble = true;
            if (e.stopPropagation) e.stopPropagation();
        }
    }

    apply(node: Element) {
        // Adds object event handlers to the specified DOM node.

        this.node = node;
        this.mouseDownHandler = (e) => this.mouseDown(e);
        this.mouseMoveHandler = (e) => this.mouseMove(e);
        this.mouseUpHandler = (e) => this.mouseUp(e);
        this.doubleClickHandler = (e) => this.doubleClick(e);
        this.rightClickHandler = (e) => this.rightClick(e);
        this.addEvent(this.node, 'mousedown', this.mouseDownHandler, null);
        this.addEvent(this.node, 'mousemove', this.mouseMoveHandler, null);
        this.addEvent(this.node, 'mouseup', this.mouseUpHandler, null);
        this.addEvent(this.node, 'dblclick', this.doubleClickHandler, null);
        this.addEvent(this.node, 'contextmenu', this.rightClickHandler, null);
    };

    destroy() {
        this.removeEvent(this.node, 'mousedown', this.mouseDownHandler, null);
        this.removeEvent(this.node, 'mousemove', this.mouseMoveHandler, null);
        this.removeEvent(this.node, 'mouseup', this.mouseUpHandler, null);
        this.removeEvent(this.node, 'dblclick', this.doubleClickHandler, null);
        this.removeEvent(this.node, 'contextmenu', this.rightClickHandler, null);
        this.deselect(true);
        this.enabled = false;
    };


    select(newElement: HTMLElement, newHandle, e) {
        // Selects an element for dragging.

        if (!document.getElementById || !this.enabled) return;

        // Activate and record our new dragging element.
        if (newElement && (newElement != this.element) && this.enabled) {
            this.element = newElement;
            // Elevate it and give it resize handles.
            this.element.style.zIndex = (++this.zIndex).toString();
            // Record element attributes for mouseMove().
            this.elmX = parseInt(this.element.style.left);
            this.elmY = parseInt(this.element.style.top);
            this.elmW = this.element.offsetWidth;
            this.elmH = this.element.offsetHeight;
            if (this.ondragfocus) this.ondragfocus(e);
        }
    };


    deselect(delHandles: boolean) {
        // Immediately stops dragging an element. If 'delHandles' is true, this
        // remove the handles from the element and clears the element flag,
        // completely resetting the .

        if (!document.getElementById || !this.enabled) return;

        if (delHandles && this.element) {
            if (this.ondragblur) this.ondragblur();
            if (this.resizeHandleSet) this.resizeHandleSet(this.element, false);
            this.element = null;
        }

        this.handle = null;
        this.mOffX = 0;
        this.mOffY = 0;
        this.startDragging = false;
    };


    mouseDown(e: MouseEvent) {
        // Suitable elements are selected for drag/resize on mousedown.
        // We also initialise the resize boxes, and drag parameters like mouse position etc.
        if (!document.getElementById || !this.enabled) return true;

        let elm = <HTMLElement>e.target,
            newElement = null,
            newHandle = null,
            hRE = new RegExp(this.myName + '-([trmbl]{2})', '');

        while (elm) {
            // Loop up the DOM looking for matching elements. Remember one if found.
            if (!newHandle && (hRE.test(elm.className) || this.isHandle(elm))) newHandle = elm;
            if (this.isElement(elm)) { newElement = elm; break }
            elm = <HTMLElement>elm.parentNode;
            if (elm == this.node) break;
        }

        // If this isn't on the last dragged element, call deselect(),
        // which will hide its handles and clear element.
        if (this.element && (this.element != newElement) && this.allowBlur) this.deselect(true);

        // If we have a new matching element, call select().
        if (newElement && (!this.element || (newElement == this.element))) {
            // Stop mouse selections if we're dragging a handle.
            if (newHandle) this.cancelEvent(e, null);
            this.select(newElement, newHandle, e);
            this.handle = newHandle;
        }
    };


    mouseMove(e: MouseEvent) {
        // This continually offsets the dragged element by the difference between the
        // last recorded mouse position (mouseX/Y) and the current mouse position.
        if (!document.getElementById || !this.enabled) return true;

        // We always record the current mouse position.
        this.mouseX = e.pageX || e.clientX + document.documentElement.scrollLeft;
        this.mouseY = e.pageY || e.clientY + document.documentElement.scrollTop;
        // Record the relative mouse movement, in case we're dragging.
        // Add any previously stored & ignored offset to the calculations.
        var diffX = this.mouseX - this.lastMouseX + this.mOffX;
        var diffY = this.mouseY - this.lastMouseY + this.mOffY;
        this.mOffX = this.mOffY = 0;
        // Update last processed mouse positions.
        this.lastMouseX = this.mouseX;
        this.lastMouseY = this.mouseY;

        // That's all we do if we're not dragging anything.
        if (!this.handle) return true;

        // If included in the script, run the resize handle drag routine.
        // Let it create an object representing the drag offsets.
        var isResize = false;
        if (this.resizeHandleDrag && this.resizeHandleDrag(diffX, diffY)) {
            isResize = true;
            this.startDragging = true;
        }
        else {
            if (!this.startDragging && this.handle && this.ondragstart && (diffX != 0 || diffY != 0)) {
                this.ondragstart(e);
                this.startDragging = true;
            }

            // If the resize drag handler isn't set or returns fase (to indicate the drag was
            // not on a resize handle), we must be dragging the whole element, so move that.
            // Bounds check left-right...
            var dX = diffX, dY = diffY;
            if (this.elmX + dX < this.minLeft) this.mOffX = (dX - (diffX = this.minLeft - this.elmX));
            else if (this.elmX + this.elmW + dX > this.maxLeft) this.mOffX = (dX - (diffX = this.maxLeft - this.elmX - this.elmW));
            // ...and up-down.
            if (this.elmY + dY < this.minTop) this.mOffY = (dY - (diffY = this.minTop - this.elmY));
            else if (this.elmY + this.elmH + dY > this.maxTop) this.mOffY = (dY - (diffY = this.maxTop - this.elmY - this.elmH));
            this.elmX += diffX;
            this.elmY += diffY;
        }

        // Assign new info back to the element, with minimum dimensions.
        this.element.style.left = this.elmX + 'px';
        this.element.style.width = this.elmW + 'px';
        this.element.style.top = this.elmY + 'px';
        this.element.style.height = this.elmH + 'px';

        // Evil, dirty, hackish Opera select-as-you-drag fix.
        if (window['opera'] && document.documentElement) {
            var oDF = document.getElementById('op-drag-fix');
            if (!oDF) {
                oDF = document.createElement('input');
                oDF.id = 'op-drag-fix';
                oDF.style.display = 'none';
                document.body.appendChild(oDF);
            }
            oDF.focus();
        }

        if (this.ondragmove) this.ondragmove(isResize);

        // Stop a normal drag event.
        this.cancelEvent(e, null);
    };


    mouseUp(e: MouseEvent) {
        // On mouseup, stop dragging, but don't reset handler visibility.
        if (!document.getElementById || !this.enabled) return;

        const hRE = new RegExp(this.myName + '-([trmbl]{2})', '');
        if (this.startDragging && this.handle && this.ondragend) this.ondragend(hRE.test(this.handle.className), e);
        this.deselect(false);

    };

    rightClick(e: MouseEvent) {
        if (this.onrightclick) this.onrightclick(e);
    }

    doubleClick(e: MouseEvent) {
        if (this.ondoubleclick) this.ondoubleclick(e);
    }



    /* Resize Code -- can be deleted if you're not using it. */

    resizeHandleSet(elm: HTMLElement, show: boolean) {
        // Either creates, shows or hides the resize handles within an element.

        // If we're showing them, and no handles have been created, create 4 new ones.
        if (!elm['_handle_tr']) {
            for (var h = 0; h < this.handles.length; h++) {
                // Create 4 news divs, assign each a generic + specific class.
                var hDiv = document.createElement('div');
                hDiv.className = this.myName + ' ' + this.myName + '-' + this.handles[h];
                elm['_handle_' + this.handles[h]] = elm.appendChild(hDiv);
            }
        }

        // We now have handles. Find them all and show/hide.
        for (var h = 0; h < this.handles.length; h++) {
            elm['_handle_' + this.handles[h]].style.visibility = show ? 'inherit' : 'hidden';
        }
    };


    resizeHandleDrag(diffX: number, diffY: number) {
        // Passed the mouse movement amounts. This function checks to see whether the
        // drag is from a resize handle created above; if so, it changes the stored
        // elm* dimensions and mOffX/Y.

        var hClass = this.handle && this.handle.className &&
            this.handle.className.match(new RegExp(this.myName + '-([tmblr]{2})')) ? RegExp.$1 : '';

        // If the hClass is one of the resize handles, resize one or two dimensions.
        // Bounds checking is the hard bit -- basically for each edge, check that the
        // element doesn't go under minimum size, and doesn't go beyond its boundary.
        var dY = diffY, dX = diffX, processed = false;
        if (hClass.indexOf('t') >= 0) {
            if (this.elmH - dY < this.minHeight) this.mOffY = (dY - (diffY = this.elmH - this.minHeight));
            else if (this.elmY + dY < this.minTop) this.mOffY = (dY - (diffY = this.minTop - this.elmY));
            this.elmY += diffY;
            this.elmH -= diffY;
            processed = true;
        }
        if (hClass.indexOf('b') >= 0) {
            if (this.elmH + dY < this.minHeight) this.mOffY = (dY - (diffY = this.minHeight - this.elmH));
            else if (this.elmY + this.elmH + dY > this.maxTop) this.mOffY = (dY - (diffY = this.maxTop - this.elmY - this.elmH));
            this.elmH += diffY;
            processed = true;
        }
        if (hClass.indexOf('l') >= 0) {
            if (this.elmW - dX < this.minWidth) this.mOffX = (dX - (diffX = this.elmW - this.minWidth));
            else if (this.elmX + dX < this.minLeft) this.mOffX = (dX - (diffX = this.minLeft - this.elmX));
            this.elmX += diffX;
            this.elmW -= diffX;
            processed = true;
        }
        if (hClass.indexOf('r') >= 0) {
            if (this.elmW + dX < this.minWidth) this.mOffX = (dX - (diffX = this.minWidth - this.elmW));
            else if (this.elmX + this.elmW + dX > this.maxLeft) this.mOffX = (dX - (diffX = this.maxLeft - this.elmX - this.elmW));
            this.elmW += diffX;
            processed = true;
        }

        return processed;
    };
}
