import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { Subject } from 'rxjs';
import { WindowRefService } from '../services/windowref.service';

@Injectable()
export class TooltipService {
    isTooltipActive: Subject<boolean>;
    tipInitialTimeout: any;
    tipTimeout: any;
    private tooltipDiv: HTMLDivElement;
    private tipmousemouveEventX: any;
    private tipmousemouveEventY: any;
    private tipmousemouveEventIsPage: boolean;
    private doc: Document;
    constructor(@Inject(DOCUMENT) _doc: any, private windowRefService: WindowRefService) {
        this.isTooltipActive = new Subject<boolean>();
        this.doc = _doc;
    }


    /**
     * Showsthe tooltip on the screen on the position of the mouse event, showing the message with a initial delay and dismissing it after dismissDelay.
     */
    public showTooltip(event: MouseEvent, message: string, initialDelay: number, dismissDelay: number) {
        let e = event;
        if (!e) e = this.windowRefService.nativeWindow.event as MouseEvent;

        let targ;
        if (e.target) targ = e.target;
        else if (e.srcElement) targ = e.srcElement;
        if (targ.nodeType === 3) // defeat Safari bug
            targ = targ.parentNode;

        if (targ.tagName && targ.tagName.toLowerCase() === 'option') { // stop tooltip if over option element
            this.hideTooltip();
            return;
        }

        const tDiv = this.getTooltipDiv();
        tDiv.innerHTML = message;
        tDiv.style.zIndex = '99999';
        tDiv.style.width = '';
        tDiv.style.overflow = 'hidden';

        this.tipmousemove(e);
        if (this.doc.addEventListener) {
            this.doc.addEventListener('mousemove', this.tipmousemove, false);
        }
        this.tipInitialTimeout = setTimeout(() => this.adjustAndShowTooltip(dismissDelay), initialDelay);
    }

    /**
     *  hides the tooltip directly that is currently showing.
     */
    public hideTooltip() {
        this.internalHideTooltip();
    }

    private getTooltipDiv(): HTMLDivElement {
        if (!this.tooltipDiv) {
            this.tooltipDiv = this.doc.createElement('div');
            this.tooltipDiv.id = 'mktipmsg';
            this.tooltipDiv.className = 'mktipmsg tooltip-inner'; // tooltip-inner class is also used by ui-bootstrap-tpls-0.10.0
            this.doc.getElementsByTagName('body')[0].appendChild(this.tooltipDiv);
        }
        return this.tooltipDiv;
    }

    private tipmousemove = (e: MouseEvent) => {
        if (e.pageX || e.pageY) {
            this.tipmousemouveEventIsPage = true;
            this.tipmousemouveEventX = e.pageX;
            this.tipmousemouveEventY = e.pageY;
        } else if (e.clientX || e.clientY) {
            this.tipmousemouveEventIsPage = false;
            this.tipmousemouveEventX = e.clientX;
            this.tipmousemouveEventY = e.clientY;
        }
    };

    private adjustAndShowTooltip(dismissDelay: number) {
        let x = 0;
        let y = 0;

        if (this.tipmousemouveEventX || this.tipmousemouveEventY) {
            if (this.tipmousemouveEventIsPage) {
                x = this.tipmousemouveEventX;
                y = this.tipmousemouveEventY;
            } else {
                x = this.tipmousemouveEventX + this.doc.body.scrollLeft + this.doc.documentElement.scrollLeft;
                y = this.tipmousemouveEventY + this.doc.body.scrollTop + this.doc.documentElement.scrollTop;
            }
        }

        let wWidth = 0; let wHeight = 0;
        if (typeof ( this.windowRefService.nativeWindow.innerWidth) == 'number') {
            //Non-IE
            wWidth =  this.windowRefService.nativeWindow.innerWidth;
            wHeight =  this.windowRefService.nativeWindow.innerHeight;
        } else if (this.doc.documentElement && (this.doc.documentElement.clientWidth || this.doc.documentElement.clientHeight)) {
            //IE 6+ in 'standards compliant mode'
            wWidth = this.doc.documentElement.clientWidth;
            wHeight = this.doc.documentElement.clientHeight;
        }


        const tDiv = this.getTooltipDiv();
        tDiv.style.left = x + 10 + 'px';
        tDiv.style.top = y + 10 + 'px';
        tDiv.style.display = 'block';
        const tooltipOffsetWidth = x + 10 + tDiv.offsetWidth;

        if (wWidth < tooltipOffsetWidth) {
            let newLeft = x - 10 - tDiv.offsetWidth;
            if (newLeft < 0) {
                newLeft = 0;
                tDiv.style.width = x - 10 + 'px';
            }
            if (newLeft === 0)
                newLeft = tDiv.offsetWidth;
            tDiv.style.left = newLeft + 'px';
        }

        const tooltipOffsetHeight = y + 10 + tDiv.offsetHeight;
        if (wHeight < tooltipOffsetHeight) {
            const newTop = y - 10 - tDiv.offsetHeight;
            tDiv.style.top = newTop + 'px';
        }
        this.isTooltipActive.next(true);
        this.tipTimeout = setTimeout(() => this.hideTooltip(), dismissDelay);


    }

    private internalHideTooltip() {
        if (this.doc.removeEventListener)
           this.doc.removeEventListener('mousemove', this.tipmousemove, false);
        clearTimeout(this.tipInitialTimeout);
        clearTimeout(this.tipTimeout);

        const tDiv = this.getTooltipDiv();
        tDiv.style.display = 'none';
        this.isTooltipActive.next(false);
    }
}





