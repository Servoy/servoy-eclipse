import { Injectable, Inject, ComponentRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ServoyFormPopupComponent } from './popupform/popupform';
import { FormService } from '../form.service';
import { ServicesService } from '../../sablo/services.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { MainViewRefService, PopupForm } from '@servoy/public';

@Injectable()
export class PopupFormService {

    formPopupComponent: ComponentRef<ServoyFormPopupComponent>;
    clickedComponentId: string;
    x: number;
    y: number;
    sequencePopup: boolean; 

    constructor(private mainViewRefService: MainViewRefService,
        private formService: FormService,
        private servicesService: ServicesService,
        private servoyService: ServoyService,
        private utils: SvyUtilsService,
        @Inject(DOCUMENT) private doc: Document) {
    }

    public showForm(popup: PopupForm) {
        const mainForm = this.doc.getElementById('mainForm');
        if (mainForm) {
            // main form can be null at startup
            const customEvent = new CustomEvent('disableTabseq', {
                bubbles: true
            });
            mainForm.dispatchEvent(customEvent);
        }

        Promise.resolve(this.formService.formWillShow(popup.form, true)).then(() =>
            this.servoyService.loaded()
        ).then(() => {
            this.showPopup(popup);
        });
    }

    public cancelFormPopup(disableClearPopupFormCallToServer: boolean): void {
        this.doc.body.removeEventListener('mouseup', this.formPopupBodyListener);
        if (this.formPopupComponent) {
            this.formService.hideForm(this.formPopupComponent.instance.popup.form);
            if (this.formPopupComponent.instance.popup.onClose) {
                const jsEvent = this.utils.createJSEvent({ target: this.doc.getElementById('formpopup') }, 'popupClose');
                if (jsEvent) {
                    jsEvent.formName = this.formPopupComponent.instance.popup.onClose.formname;
                }
                this.servicesService.callServiceServerSideApi('window', 'formPopupClosed',  [jsEvent]);
            }
        }

        const customEvent = new CustomEvent('enableTabseq', {
            bubbles: true
        });
        this.doc.getElementById('mainForm').dispatchEvent(customEvent);

        /*
         * Because server side code in window_server.js checks for scope.model.popupform != null when closing a form popup it must have the correct value server-side; so
         *     - when it is closed by a click outside the popup form area that happens to be exactly on a button that opens it again, the current method executes and
         *       "scope.model.popupform" needs to reach server before the button click that will open the form editor again executes not after (because if it is set to null
         *       after the reshow, it will be in a wrong state server-side); that is why we use callServerSideApi here instead of relying on a shallow watch (pushToServer in spec)
         *       on the model property which would send the null change too late
         *     - if one would click twice really fast on a button that shows a form popup, both those clicks are queued on the server's event queue; it shows the first time
         *       then in server side code - when the show is called the second time it would close the first one and show it again; but in this case we must not call callServerSideApi
         *       to set scope.model.popupform to null because that would execute after the second show is done (it is queued after it on server) and again we'd end up with a shown
         *       form popup but a null scope.model.popupform on server which is wrong... that is the purpose of "disableClearPopupFormCallToServer" flag
         */
        if (!disableClearPopupFormCallToServer) {
            this.servicesService.callServiceServerSideApi('window', 'clearPopupForm', []);
        }
        if (this.formPopupComponent) {
            this.formPopupComponent.destroy();
            this.formPopupComponent = null;
        }
    }

    private showPopup(popup: PopupForm, counter?: number) {
		if (this.formPopupComponent) return; // another popup is still visible
        const docComponent = this.doc.getElementById(popup.component);
        if ((popup.component && !docComponent && (!counter || counter < 10)) && (popup.component != this.clickedComponentId)) {
            setTimeout(() => {
                const c = counter? ++counter:1;
                this.showPopup(popup, c);
            }, 50);
        } else {
            this.sequencePopup = false;
            if (popup.component && popup.component == this.clickedComponentId) {
                this.sequencePopup = true;
            }
            this.formPopupComponent = this.mainViewRefService.mainContainer.createComponent(ServoyFormPopupComponent);
            this.formPopupComponent.instance.setPopupForm(popup);
            this.sequencePopup = false;
            setTimeout(() => {
                this.doc.body.addEventListener('mouseup', this.formPopupBodyListener);
            }, 300);

        }
    }


    private formPopupBodyListener = (event: Event) => {

        const target = event.target as HTMLElement;
        if (target && target.id && this.formPopupComponent) {
            this.clickedComponentId = target.id;
            this.x = this.formPopupComponent.instance._left;
            this.y = this.formPopupComponent.instance._top;
        } else {
            this.clickedComponentId = null;
        }

        if (this.formPopupComponent && this.formPopupComponent.instance.popup.doNotCloseOnClickOutside) {
            return;
        }
        const backdrop = this.doc.querySelector('.formpopup-backdrop');
        if (backdrop && (backdrop === event.target)) {
            //backdrop.remove();
            this.cancelFormPopup(false);
            return;
        }
        let mainform = this.doc.querySelector('.svy-main-window-container');
        if (mainform && mainform.contains(event.target as Node)) {
            this.cancelFormPopup(false);
            return;
        }
        mainform = this.doc.querySelector('.svy-dialog');
        if (mainform && mainform.contains(event.target as Node)) {
            this.cancelFormPopup(false);
            return;
        }
    };
}