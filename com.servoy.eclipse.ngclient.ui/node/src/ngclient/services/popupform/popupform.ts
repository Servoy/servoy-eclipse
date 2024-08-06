import { Component, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ServoyPublicService, PopupForm } from '@servoy/public';
import { PopupFormService } from '../popupform.service';

@Component({
    selector: 'svy-popupform',
    templateUrl: './popupform.html'
})
export class ServoyFormPopupComponent {

    public popup: PopupForm;
    _left = 0;
    _top = 0;
    _width = 0;
    _height = 0;

    constructor(@Inject(DOCUMENT) private doc: Document, private formService: ServoyPublicService, private popupFormService: PopupFormService) {
    }

    setPopupForm(popup: PopupForm) {
        this.popup = popup;
        this.initSize();
    }

    initSize() {
        const formCache = this.formService.getFormCacheByName(this.popup.form);

        let popupwidth = this.popup.width;
        if (!popupwidth || popupwidth <= 0) {
            popupwidth = formCache.size.width;
        }
        let popupheight = this.popup.height;
        if (!popupheight || popupheight <= 0) {
            popupheight = formCache.size.height;
        }
        let popupLeft: number;
        let popupTop: number;
        let rect: DOMRect;
        let compWidth = 0;
        let compHeight = 0;
        
        if (this.popup.component) {
			const element = this.doc.getElementById(this.popup.component);
            if (element) {
                rect = element.getBoundingClientRect();
                compWidth = rect.width;
				compHeight = rect.height;
            }
		}
        
        if (this.popup.x !== null) {
			 popupLeft = this.popup.x;
		} else if (rect) {
			popupLeft = rect.left;
		} else {
			popupLeft = this.doc.defaultView.innerWidth / 2 - popupwidth / 2;
		}
		if (this.popup.x !== null || this.popup.component) {
			if ((popupLeft + popupwidth > this.doc.defaultView.innerWidth) && (popupLeft - popupwidth + compWidth > 0)) {
                popupLeft = popupLeft - popupwidth + compWidth;
            }
		}
		
		if (this.popup.y !== null) {
			 popupTop = this.popup.y;
		} else if (rect) {
			popupTop = rect.top;
		} else {
			popupTop = this.doc.defaultView.innerHeight / 2 - popupheight / 2;
		}		
		if (this.popup.y !== null || this.popup.component) {
			if ((popupTop + popupheight > this.doc.defaultView.innerHeight) && (popupTop - popupheight + compHeight > 0)) {
                popupTop = popupTop - popupheight + compHeight;
            }
		}
		
        this._width = popupwidth;
        this._height = popupheight;
        if (this.popupFormService.sequencePopup) {
            this._left = this.popupFormService.x;
            this._top = this.popupFormService.y;    
        } else {
            this._left = popupLeft;
            this._top = popupTop;
        }
    }

    getForm() {
        return this.popup.form;
    }

    getStyle() {
        return { position: 'absolute', zIndex: 1050, left: this._left + 'px', top: this._top + 'px', width: this._width + 'px', height: this._height + 'px' };
    }

    firstElementFocused(event: Event) {
        const tabIndex = parseInt(this.doc.getElementById('tabStop').getAttribute('tabindex'), 10);
        const newTarget: any = this.doc.querySelector('[tabindex=\'' + (tabIndex - 1) + '\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (event.target !== newTarget) {
            newTarget.focus();
        }
    }

    lastElementFocused(event: Event) {
        const newTarget: any = this.doc.querySelector('[tabindex=\'2\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (event.target !== newTarget) {
            newTarget.focus();
        }
    }
}
