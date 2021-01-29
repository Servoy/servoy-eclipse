import { Component, Inject } from '@angular/core';
import { Callback, PopupForm } from '../popupform.service';
import { DOCUMENT } from '@angular/common';
import { SabloService } from '../../../sablo/sablo.service';
import { FormService } from '../../form.service';

@Component({
    selector: 'svy-popupform',
    templateUrl: './popupform.html'
})
export class ServoyFormPopupComponent {

    public popup: PopupForm;
    _left: number = 0;
    _top: number = 0;
    _width: number = 0;
    _height: number = 0;

    constructor(private sabloService: SabloService, @Inject(DOCUMENT) private document, private formService: FormService) {
    }

    setPopupForm(popup: PopupForm) {
        this.popup = popup;
        this.initSize();
    }

    initSize() {
        let formCache = this.formService.getFormCacheByName(this.popup.form);

        let popupwidth = this.popup.width;
        if (!popupwidth || popupwidth <= 0) {
            popupwidth = formCache.size.width;
        }
        let popupheight = this.popup.height;
        if (!popupheight || popupheight <= 0) {
            popupheight = formCache.size.height;
        }
        let popupLeft;
        let popupTop;
        if (this.popup.component || (this.popup.x && this.popup.y)) {
            let element = document.getElementById(this.popup.component);
            let compWidth = 0;
            let compHeight = 0;
            if (element) {
                let rect = element.getBoundingClientRect();
                popupLeft = rect.left;
                popupTop = rect.top;
                compWidth = rect.width;
                compHeight = rect.height;
            }
            else {
                popupLeft = this.popup.x;
                popupTop = this.popup.y;
            }

            if ((popupLeft + popupwidth > this.document.defaultView.offsetWidth) && (popupLeft - popupwidth + compWidth > 0)) {
                popupLeft = popupLeft - popupwidth + compWidth;
            }

            if ((popupTop + popupheight > this.document.defaultView.offsetHeight) && (popupTop - popupheight + compHeight > 0)) {
                popupTop = popupTop - popupheight + compHeight;
            }

        }
        else if (!this.popup.component) {
            // calculate the real center
            popupLeft = this.document.defaultView.offsetWidth / 2 - popupwidth / 2;
            popupTop = this.document.defaultView.offsetHeight / 2 - popupheight / 2;
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

    firstElementFocused(event) {
        const tabIndex = parseInt(this.document.getElementById('tabStop').getAttribute('tabindex'));
        const newTarget: any = document.querySelector('[tabindex=\'' + (tabIndex - 1) + '\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (event.target != newTarget) {
            newTarget.focus();
        }
    }

    lastElementFocused(event) {
        const newTarget: any = document.querySelector('[tabindex=\'2\']');
        // if there is no focusable element in the window, then newTarget == e.target,
        // do a check here to avoid focus cycling
        if (event.target != newTarget) {
            newTarget.focus();
        }
    }
}
