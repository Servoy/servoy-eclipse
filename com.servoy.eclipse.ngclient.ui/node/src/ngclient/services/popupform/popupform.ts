import { Component, Inject } from '@angular/core';
import { PopupForm } from '../popupform.service';
import { DOCUMENT } from '@angular/common';
import { FormService } from '../../form.service';

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

    constructor(@Inject(DOCUMENT) private document: Document, private formService: FormService) {
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
        if (this.popup.component || (this.popup.x && this.popup.y)) {
            const element = document.getElementById(this.popup.component);
            let compWidth = 0;
            let compHeight = 0;
            if (element) {
                const rect = element.getBoundingClientRect();
                popupLeft = rect.left;
                popupTop = rect.top + rect.height;
                compWidth = rect.width;
                compHeight = rect.height;
            } else {
                popupLeft = this.popup.x;
                popupTop = this.popup.y;
            }

            if ((popupLeft + popupwidth > this.document.defaultView.innerWidth) && (popupLeft - popupwidth + compWidth > 0)) {
                popupLeft = popupLeft - popupwidth + compWidth;
            }

            if ((popupTop + popupheight > this.document.defaultView.innerHeight) && (popupTop - popupheight + compHeight > 0)) {
                popupTop = popupTop - popupheight + compHeight;
            }

        } else if (!this.popup.component) {
            // calculate the real center
            popupLeft = this.document.defaultView.innerWidth / 2 - popupwidth / 2;
            popupTop = this.document.defaultView.innerHeight / 2 - popupheight / 2;
        }
        this._width = popupwidth;
        this._height = popupheight;
        this._left = popupLeft;
        this._top = popupTop;
    }

    getForm() {
        return this.popup.form;
    }

    getStyle() {
        return { position: 'absolute', zIndex: 1499, left: this._left + 'px', top: this._top + 'px', width: this._width + 'px', height: this._height + 'px' };
    }

    firstElementFocused(event: Event) {
        const tabIndex = parseInt(this.document.getElementById('tabStop').getAttribute('tabindex'), 10);
        const newTarget: any = document.querySelector('[tabindex=\'' + (tabIndex - 1) + '\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (event.target !== newTarget) {
            newTarget.focus();
        }
    }

    lastElementFocused(event: Event) {
        const newTarget: any = document.querySelector('[tabindex=\'2\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (event.target !== newTarget) {
            newTarget.focus();
        }
    }
}
