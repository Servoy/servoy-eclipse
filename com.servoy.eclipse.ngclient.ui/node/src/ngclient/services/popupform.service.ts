import { Injectable, Inject, ComponentFactoryResolver, Injector, ApplicationRef, EmbeddedViewRef, ComponentRef } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { ServoyFormPopupComponent } from './popupform/popupform';
import { FormService } from '../form.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../servoy_public';

@Injectable({
    providedIn: 'root',
})
export class PopupFormService {

    formPopupComponent: ComponentRef<ServoyFormPopupComponent>;

    constructor(private componentFactoryResolver: ComponentFactoryResolver,
        private _applicationRef: ApplicationRef,
        private _injector: Injector,
        private formService: FormService,
        private servoyService: ServoyService,
        private utils: SvyUtilsService,
        @Inject(DOCUMENT) private document: Document) {
    }

    public showForm(popup: PopupForm) {
        const mainForm = this.document.getElementById('mainForm');
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
        document.body.removeEventListener('mouseup', this.formPopupBodyListener);
        if (this.formPopupComponent) {
            this.formService.hideForm(this.formPopupComponent.instance.popup.form);
            if (this.formPopupComponent.instance.popup.onClose) {
                const jsEvent = this.utils.createJSEvent({ target: this.document.getElementById('formpopup') }, 'popupClose');
                if (jsEvent) {
                    jsEvent.formName = this.formPopupComponent.instance.popup.onClose.formname;
                }
                this.servoyService.executeInlineScript(this.formPopupComponent.instance.popup.onClose.formname, this.formPopupComponent.instance.popup.onClose.script, [jsEvent]);
            }
        }

        const customEvent = new CustomEvent('enableTabseq', {
            bubbles: true
        });
        this.document.getElementById('mainForm').dispatchEvent(customEvent);

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
            this.formService.callServiceServerSideApi('window', 'clearPopupForm', []);
        }
        if (this.formPopupComponent) {
            this.formPopupComponent.destroy();
            this.formPopupComponent = null;
        }
    }

    private showPopup(popup: PopupForm, counter?: number) {
        if (popup.component && !this.document.getElementById(popup.component) && (!counter || counter < 10)) {
            setTimeout(() => {
                const c = counter? counter++:1;
                this.showPopup(popup, counter);
            }, 50);
        } else {
            const componentFactory = this.componentFactoryResolver.resolveComponentFactory(ServoyFormPopupComponent);
            this.formPopupComponent = componentFactory.create(this._injector);
            this.formPopupComponent.instance.setPopupForm(popup);
            this._applicationRef.attachView(this.formPopupComponent.hostView);
            this.document.body.appendChild((this.formPopupComponent.hostView as EmbeddedViewRef<any>).rootNodes[0] as HTMLElement);
            this.document.body.addEventListener('mouseup', this.formPopupBodyListener);
        }
    }

    private formPopupBodyListener = (event: Event) => {
        if (this.formPopupComponent && this.formPopupComponent.instance.popup.doNotCloseOnClickOutside) {
            return;
        }
        const backdrop = this.document.querySelector('.formpopup-backdrop');
        if (backdrop && (backdrop === event.target)) {
            //backdrop.remove();
            this.cancelFormPopup(false);
            return;
        }
        let mainform = this.document.querySelector('.svy-main-window-container');
        if (mainform && mainform.contains(event.target as Node)) {
            this.cancelFormPopup(false);
            return;
        }
        mainform = this.document.querySelector('.svy-dialog');
        if (mainform && mainform.contains(event.target as Node)) {
            this.cancelFormPopup(false);
            return;
        }
    };
}

export class PopupForm {
    public component: string;
    public form: string;
    public x: number;
    public y: number;
    public width: number;
    public height: number;
    public showBackdrop: boolean;
    public doNotCloseOnClickOutside: boolean;
    public onClose: Callback;
}

export class Callback {
    public formname: string;
    public script: string;
}
