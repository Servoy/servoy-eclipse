import { Injectable, Inject, Renderer2, RendererFactory2, ComponentRef, DOCUMENT } from '@angular/core';
import { Title } from '@angular/platform-browser';

import { FormService } from '../form.service';
import { ServoyService } from '../servoy.service';
import { DialogWindowComponent } from './dialog-window/dialog-window.component';
import { BSWindowManager } from './bootstrap-window/bswindow_manager.service';
import { BSWindow, BSWindowOptions } from './bootstrap-window/bswindow';
import { WindowRefService, LocalStorageService, SessionStorageService, MainViewRefService, PopupStateService } from '@servoy/public';
import { SabloService } from '../../sablo/sablo.service';
import { PlatformLocation } from '@angular/common';
import { ApplicationService } from './application.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { LoadingIndicatorService } from '../../sablo/util/loading-indicator/loading-indicator.service';
import { FormSettings } from '../types';
import { environment as env} from '../../environments/environment';

@Injectable()
export class WindowService {
    public static readonly WINDOW_TYPE_DIALOG = 0;
    public static readonly WINDOW_TYPE_MODAL_DIALOG = 1;
    public static readonly WINDOW_TYPE_WINDOW = 2;


    private instances: { [property: string]: SvyWindow } = {};
    private windowCounter: number;
    private windowsRestored = false;
    private dialogShown = false;
    private renderer2: Renderer2;

    constructor(private formService: FormService,
        public servoyService: ServoyService,
        private windowRefService: WindowRefService,
        private mainViewRefService: MainViewRefService,
        public localStorageService: LocalStorageService,
        public sessionStorageService: SessionStorageService,
        private titleService: Title,
        public sabloService: SabloService,
        private bsWindowManager: BSWindowManager,
        private appService: ApplicationService,
        private platformLocation: PlatformLocation,
        private webSocketService: WebsocketService,
        private sabloLoadingIndicatorService: LoadingIndicatorService,
        rendererFactory: RendererFactory2,
        private popupStateService: PopupStateService,
        @Inject(DOCUMENT) private doc: Document) {

        this.platformLocation.onPopState(() => {
            const form = this.platformLocation.hash.replace('#', '');
            if (form) this.formService.goToForm(form);
        });

        this.windowCounter = 0;
        this.renderer2 = rendererFactory.createRenderer(null, null);
        
        if (env.mobile && (this.windowRefService.nativeWindow as any)._formdata_) {
			const formsData: Array<{[key: string]: any}> = (this.windowRefService.nativeWindow as any)._formdata_;
			formsData.forEach(formData => {
				for (const formName in formData) {
					this.formService.createFormCache(formName, formData[formName], formName);
				}
			})   
        }
    }

    public updateController(formName: string, formStructure: string, url: string) {
        const formState = JSON.parse(formStructure)[formName];
        this.formService.createFormCache(formName, formState, url);
    }

    public getLoadedFormState() {
        this.formService.getLoadedFormState();
    }

    public create(name: string, type: number) {
        const storedWindow = this.sessionStorageService.get('window' + this.windowCounter);
        if (!storedWindow || storedWindow.name !== name) {
            this.sessionStorageService.set('window' + this.windowCounter, {
                name,
                type
            });
        }
        if (!this.instances[name]) {
            this.instances[name] = new SvyWindow(name, type, this, this.renderer2);
        }
    }

    public show(name: string, form: string, title: string) {
        this.dialogShown = true;
        const currentWindow = 'window' + this.windowCounter;
        const storedWindow = this.sessionStorageService.get(currentWindow);
        if (storedWindow && !storedWindow.showForm) {
            storedWindow.showForm = form;
            storedWindow.showTitle = title;
            this.sessionStorageService.set(currentWindow, storedWindow);
            this.windowCounter++;
        }
        const instance = this.instances[name];
        if (instance) {
            instance.title = title;
            if (instance.bsWindowInstance) {
                // already showing
                return;
            }

            if (this.doc.getElementById('mainForm') && this.doc.getElementsByClassName('svy-dialog').length < 1) {
                const customEvent = new CustomEvent('disableTabseq', {
                    bubbles: true
                });
                this.doc.getElementById('mainForm').dispatchEvent(customEvent);
            }

            if (instance.storeBounds) {
                instance.location = this.localStorageService.get(
                    this.servoyService.getSolutionSettings().solutionName + name + '.storedBounds.location');
                instance.size = this.localStorageService.get(
                    this.servoyService.getSolutionSettings().solutionName + name + '.storedBounds.size');
            }

            // resolve initial bounds
            let location = null;
            let size = instance.form.size;
            if (instance.initialBounds) {
                const bounds = instance.initialBounds;
                if (bounds.x > -1 && bounds.y > -1) {
                    location = {
                        x: bounds.x,
                        y: bounds.y
                    };
                }
                if (bounds.width > 0 && bounds.height > 0) {
                    size = { width: bounds.width, height: bounds.height };
                }
            }
            if (instance.location) {
                location = instance.location;
            }
            if (instance.size) {
                size = instance.size;
            }
            // -1 means default size and location(center)
            let formSize = size;
            if (!formSize || (formSize.width === -1 && formSize.height === -1))
                formSize = instance.form.size;

            const windowWidth = this.doc.documentElement.clientWidth;
            const windowHeight = this.doc.documentElement.clientHeight;

            // this can happen in case of responsive forms
            if (formSize.width === 0) formSize.width = windowWidth / 2;
            if (formSize.height === 0) formSize.height = windowHeight / 2;


            if (!location || (location.x < 0 && location.y < 0)) location = this.centerWindow(formSize);
            if (!size || size.width < 0 || size.height < 0) size = null;

            if (size) {
                // dialog shouldn't be bigger than viewport
                if (size.width && size.width > windowWidth) {
                    size.width = windowWidth;
                }
                if (size.height && size.height > windowHeight) {
                    size.height = windowHeight;
                }
            }
            // convert servoy x,y to library top , left
            const loc = { left: location.x, top: location.y };

            // create the bs window instance
            const dialogWindowComponent  = this.mainViewRefService.mainContainer.createComponent(DialogWindowComponent);
//
            dialogWindowComponent.instance.setWindow(instance);
            instance.componentRef = dialogWindowComponent;

            const opt: BSWindowOptions = {
                id: instance.name,
                fromElement: dialogWindowComponent.location.nativeElement.childNodes[0],
                title: instance.title,
                resizable: !!instance.resizable,
                location: loc,
                size,
                isModal: instance.type === WindowService.WINDOW_TYPE_MODAL_DIALOG
            };

            // test if it is modal dialog, then the request blocks on the server and we should hide the loading.
            if (instance.type === WindowService.WINDOW_TYPE_MODAL_DIALOG && this.sabloLoadingIndicatorService.isShowing()) {
                instance['loadingIndicatorIsHidden'] = 0;
                // as long as the indicator says it is still showing call hide loading to
                // get the loading counter really to 0
                // this happens a second modal dialog is showing in the hide of another...
                // then the stack on the server can't rewind, because it is still in the stack of the first dialog
                while (this.sabloLoadingIndicatorService.isShowing()) {
                    instance['loadingIndicatorIsHidden']++;
                    this.sabloLoadingIndicatorService.hideLoading();
                }
            }

            instance.bsWindowInstance = this.bsWindowManager.createWindow(opt);

            const resizeListener = (event: CustomEvent< { width: number; height: number }>) => {
                instance.onResize(event.detail);
            };
            
            const moveListener = (event: CustomEvent<{x: number; y: number}>) => {
                instance.onMove(event.detail);
            };
            
            const activeListener = (event: CustomEvent<boolean>) => {
                const customEvent = new CustomEvent(event.detail ? 'enableTabseq' : 'disableTabseq', {
                    bubbles: true
                });
                event.target.dispatchEvent(customEvent);
            };

            if (instance.closeOnEscape) {
                instance.keyUpListener = (event) => this.handleEscapeKey(event, instance);
                window.addEventListener('keyup', instance.keyUpListener, true);
            }
            
            instance.bsWindowInstance.element.addEventListener('bswin.resize', resizeListener);
            instance.bsWindowInstance.element.addEventListener('bswin.move', moveListener);
            instance.bsWindowInstance.element.addEventListener('bswin.active', activeListener);
            
            instance.bsWindowInstance.onClose = () => {
               instance.bsWindowInstance.element.removeEventListener('bswin.resize', resizeListener);
               instance.bsWindowInstance.element.removeEventListener('bswin.move', moveListener);
               instance.bsWindowInstance.element.removeEventListener('bswin.active', activeListener);
            };
            (this.doc.getElementsByClassName('window-header').item(0) as HTMLElement).focus();
            instance.bsWindowInstance.setActive(true);
            // init the size of the dialog
            const width = instance.bsWindowInstance.element.getBoundingClientRect().width;
            const height = instance.bsWindowInstance.element.getBoundingClientRect().height;
            if (width > 0 && height > 0) {
                const dialogSize = { width, height };
                this.sabloService.callService('$windowService', 'resize', { name: instance.name, size: dialogSize }, true);
            }
        }
    }
    
    handleEscapeKey(event: KeyboardEvent, instance: SvyWindow) {
        if (event.key === 'Escape' && instance.bsWindowInstance && !this.popupStateService.isAnyPopupActive()) {
            instance.componentRef.instance.cancel();
        }
    }

    public hide(name: string) {
        let winCounter = 0;
        while (this.sessionStorageService.get('window' + winCounter)) {
            const window = this.sessionStorageService.get('window' + winCounter);
            if (window.name === name) {
                this.sessionStorageService.remove('window' + winCounter);
                this.windowCounter--;
            }
            winCounter++;
        }
        const instance = this.instances[name];
        if (instance) {
            if (instance.closeOnEscape) {
                window.removeEventListener('keyup', instance.keyUpListener, true);
                instance.keyUpListener = null;
            }
            
            if (instance['loadingIndicatorIsHidden']) {
                let counter = instance['loadingIndicatorIsHidden'];
                delete instance['loadingIndicatorIsHidden'];
                while (counter-- > 0) {
                    this.sabloLoadingIndicatorService.showLoading();
                }
            }
            instance.hide();
            if (this.doc.getElementById('mainForm') && this.doc.getElementsByClassName('svy-dialog').length < 1) {
                const customEvent = new CustomEvent('enableTabseq', {
                    bubbles: true
                });
                this.doc.getElementById('mainForm').dispatchEvent(customEvent);
            }
        }
    }

    public destroy(name: string) {
        const instance = this.instances[name];
        if (instance) {
            delete this.instances[name];
        }
    }

    public setTitle(name: string, title: string ) {
        this.saveInSessionStorage(title, 'title');
        if ( this.instances[name] && this.instances[name].type !== WindowService.WINDOW_TYPE_WINDOW ) {
            this.instances[name].setTitle(title);
        } else {
            this.titleService.setTitle(title);
        }
    }

    public setInitialBounds(name: string, initialBounds: any ) {
        this.saveInSessionStorage(initialBounds, 'initialBounds');
        if ( this.instances[name] ) {
            this.instances[name].initialBounds = initialBounds;
        }
    }

    public setStoreBounds(name: string, storeBounds: boolean ) {
        this.saveInSessionStorage(storeBounds, 'storeBounds');
        if ( this.instances[name] ) {
            this.instances[name].storeBounds = storeBounds;
        }
    }

    public resetBounds(name: string) {
        if (this.instances[name]) {
            this.instances[name].storeBounds = false;
            this.instances[name].clearBounds();
        }
    }

    public setLocation(name: string, location: any ) {
        this.saveInSessionStorage(location, 'location');
        if ( this.instances[name] ) {
            this.instances[name].setLocation( location );
        }
    }

    public setSize(name: string, size: any ) {
        this.saveInSessionStorage(size, 'size');
        if ( this.instances[name] ) {
            this.instances[name].setSize( size );
        }
    }
	
	public requestFullscreen(name: string ){
		if ( this.instances[name] ) {
			this.instances[name].requestFullscreen();
		}
	}

    public getSize(name: string ) {
        if ( this.instances[name] && this.instances[name].bsWindowInstance ) {
            return this.instances[name].getSize();
        } else {
            return { width: this.windowRefService.nativeWindow.innerWidth, height: this.windowRefService.nativeWindow.innerHeight };
        }
    }

    public setUndecorated(name: string, undecorated: boolean ) {
        this.saveInSessionStorage(undecorated, 'undecorated');
        if ( this.instances[name] ) {
            this.instances[name].undecorated = undecorated;
        }
    }
    
    public setCloseOnEscape(name: string, closeOnEscape: boolean) {
        this.saveInSessionStorage(closeOnEscape, 'closeOnEscape');
        if (this.instances[name]) {
            this.instances[name].closeOnEscape = closeOnEscape;
        }
    }

    public setCSSClassName(name: string, cssClassName: string ) {
        this.saveInSessionStorage(cssClassName, 'cssClassName');
        if ( this.instances[name] ) {
            this.instances[name].cssClassName = cssClassName;
        }
    }

    public setOpacity(name: string, opacity: boolean) {
        this.saveInSessionStorage(opacity, 'opacity');
        if (this.instances[name]) {
            this.instances[name].opacity = opacity?0:1;
        }
    }

    public setResizable(name: string, resizable: boolean ) {
        this.saveInSessionStorage(resizable, 'resizable');
        if ( this.instances[name] ) {
            this.instances[name].resizable = resizable;
        }
    }

    public setTransparent(name: string, transparent: boolean ) {
        this.saveInSessionStorage(transparent, 'transparent');
        if ( this.instances[name] ) {
            this.instances[name].transparent = transparent;
        }
    }

    public toFront(name: string) {
        if (this.instances[name]) {
            this.bsWindowManager.setFocused(this.instances[name].bsWindowInstance);
        }
    }

    public toBack(name: string) {
        if (this.instances[name]) {
            this.bsWindowManager.sendToBack(this.instances[name].bsWindowInstance);
        }
    }

    centerWindow(formSize: {width: number; height: number}) {
        // var body = $( 'body' );
        const windowWidth = this.doc.documentElement.clientWidth;
        const windowHeight = this.doc.documentElement.clientHeight;
        let top: number; let left: number;
        // bodyTop = body.position().top + parseInt( body.css( 'paddingTop' ), 10 );
        left = (windowWidth / 2) - (formSize.width / 2);
        top = (windowHeight / 2) - (formSize.height / 2);
        // if ( top < bodyTop ) {
        //     top = bodyTop;
        // }
        if (left < 0) left = 0;
        if (top < 0) top = 0;
        return { x: left, y: top };
    }

    public switchForm(name: string, form: FormSettings, navigatorForm: FormSettings, isLoginForm?: boolean) {
        const currentWindow = 'window' + this.windowCounter;
        const storedWindow = this.sessionStorageService.get(currentWindow);
        if (storedWindow && !storedWindow.switchForm) {
            storedWindow.switchForm = form;
            storedWindow.navigatorForm = navigatorForm;
            this.sessionStorageService.set(currentWindow, storedWindow);
        }
        if (this.instances[name] && this.instances[name].type !== WindowService.WINDOW_TYPE_WINDOW) {
            this.instances[name].form = form;
            this.instances[name].navigatorForm = navigatorForm;
        }
        this.servoyService.loaded().then(() => {
            // if first show of this form in browser window then request initial data (dataproviders and such)
            // isn't this a nop in NG2? formWillShow with false doesn't do anything.
            this.formService.formWillShow(form.name, false); // false because form was already made visible server-side
            if (navigatorForm && navigatorForm.name && navigatorForm.name.lastIndexOf('default_navigator_container.html') === -1) {
                // if first show of this form in browser window then request initial data (dataproviders and such)
                this.formService.formWillShow(navigatorForm.name, false); // false because form was already made visible server-side
            }

            if (this.servoyService.getSolutionSettings().windowName === name) { // main window form switch
                this.servoyService.getSolutionSettings().mainForm = form;
                this.servoyService.getSolutionSettings().navigatorForm = navigatorForm;
                if (this.appService.getUIProperty('servoy.ngclient.formbased_browser_history') !== false && !isLoginForm) {
                    // this navigationId is angular router maybe in the future we need to have a look to just use that to set the navigation states to the forms.
                    const state = this.platformLocation.getState();
                    if (state && state['navigationId'])
                        this.platformLocation.replaceState(form.name,null, this.platformLocation.pathname + this.platformLocation.search + '#' + form.name);
                    else  this.platformLocation.pushState(form.name,null, this.platformLocation.pathname + this.platformLocation.search + '#' + form.name);
                }
            }
            const formCache = this.formService.getFormCacheByName(form.name);
            if (formCache) {
                formCache.navigatorForm = navigatorForm;
            }
            if (!this.windowsRestored) {
                this.windowsRestored = true;
                this.restoreWindows();
            }
        });
    }
    
    public switchToSessionProblemPage() {
        this.servoyService.loaded().then(() => {
            const sessionProblem = this.servoyService.getSolutionSettings().sessionProblem;
            if (sessionProblem && sessionProblem.viewUrl) {
                sessionProblem.nonce = this.windowRefService.nativeWindow.document.getElementsByTagName("app-root")[0].attributes['ngCspNonce']?.value;
                const name = sessionProblem.viewUrl.includes('/') ? sessionProblem.viewUrl.split('/')[1].split('.')[0] : sessionProblem.viewUrl.split('.')[0];
                this.platformLocation.pushState(name, null, this.platformLocation.pathname + this.platformLocation.search + '#' + name);
            }
        });
    }

    public reload() {
        window.location.reload();
    }

    public destroyController(formName: string) {
        this.formService.destroyFormCache(formName);
    }

    public requireFormLoaded() {
        // just empty method so no error will be given when called from the server.
        // in ng1 this was for creating the hidden div.
    }


    private saveInSessionStorage(property: any, propertyName: string) {
        const currentWindow = 'window' + this.windowCounter;
        const storedWindow = this.sessionStorageService.get(currentWindow);
        if (property && storedWindow && !storedWindow[propertyName]) {
            storedWindow[propertyName] = property;
            this.sessionStorageService.set(currentWindow, storedWindow);
        }
    }

    private restoreWindows() {
        const window0 = this.sessionStorageService.get('window0');
        // a dialog was shown and we are after a tab refresh; restore from storage
        if (window0 && window0.showForm && !this.dialogShown) {
            // wait until the server is connected
            const interval = setInterval(() => {
                if (this.webSocketService.isConnected()) {
                    clearInterval(interval);
                    
                    let windowsToRestore: any[] = [];
                    let counter = 0; 
                    while (this.sessionStorageService.get('window' + counter)) {
                        windowsToRestore.push(this.sessionStorageService.get('window' + counter));
                        counter++;
                    }
                    let windowCounterReset = false;
                    windowsToRestore.forEach( window =>  {
                        // server call for getting form's data (send data from server to client)
                        // call a couple of methods that will create and display the window
                        this.create(window.name, window.type);
                        this.switchForm(window.name, window.switchForm, window.navigatorForm);
                        this.setTitle(window.name, window.title);
                        this.setUndecorated(window.name, window.undecorated);
                        this.setCloseOnEscape(window.name, window.closeOnEscape);
                        this.setCSSClassName(window.name, window.cssClassName);
                        this.setInitialBounds(window.name, window.initialBounds);
                        this.setStoreBounds(window.name, window.storeBounds);
                        this.setSize(window.name, window.size);
						this.requestFullscreen(window.name);
                        this.setLocation(window.name, window.location);
                        this.setOpacity(window.name, window.opacity);
                        this.setTransparent(window.name, window.transparent);
                        this.sabloService.callService('$windowService', 'touchForm', { name: window.showForm }, false).then( () => {
                          // in order to show all the windows the counter must be reset
                          if (this.windowsRestored && !windowCounterReset) {
                              this.windowCounter = 0;
                              windowCounterReset = true;
                          }
                          this.show(window.name, window.showForm, window.showTitle);
                          this.windowCounter++;
                        });
                        this.windowCounter++;
                });
                }
            }, 1000);
        }
    }
}

export class SvyWindow {

    name: string;
    type: number;
    title = '';
    opacity = 1;
    undecorated = false;
    cssClassName: string = null;
    closeOnEscape = false;
    size: { width: number; height: number } = null;
    location: {x: number; y: number} = null;
    navigatorForm: any = null;
    form: any = null;
    initialBounds: any = null;
    resizable = false;
    transparent = false;
    storeBounds = false;
    renderer2: Renderer2;
    
    keyUpListener: (event: KeyboardEvent) => void;

    bsWindowInstance: BSWindow = null;  // bootstrap-window instance , available only after creation
    windowService: WindowService;
    componentRef: ComponentRef<DialogWindowComponent>;

    constructor(name: string, type: number, windowService: WindowService, renderer2: Renderer2) {
        this.name = name;
        this.type = type;
        this.renderer2 = renderer2;
        this.windowService = windowService;
    }

    hide() {
        if (this.bsWindowInstance) this.bsWindowInstance.close();
        if (this.componentRef) this.componentRef.destroy();
        if (!this.storeBounds) {
            delete this.location;
            delete this.size;
        }
        delete this.bsWindowInstance;
    }

    setLocation(location: {x: number; y: number}) {
        this.location = location;
        if (this.bsWindowInstance && this.location) {
            this.renderer2.setStyle(this.bsWindowInstance.element, 'left', this.location.x + 'px');
            this.renderer2.setStyle(this.bsWindowInstance.element, 'top', this.location.y + 'px');
        }
        if (this.storeBounds) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.location', this.location);
    }

    setSize(size: { width: number; height: number }) {
        this.size = size;
        if (this.bsWindowInstance) {
            this.bsWindowInstance.setSize(size);
        }
        if (this.storeBounds) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.size', this.size);
    }
	
	requestFullscreen() {
		const doc = document.documentElement as IHTMLElement;
		if (doc.requestFullscreen) {
			doc.requestFullscreen().catch(err => console.log(err));
		} else if (doc.webkitRequestFullscreen) { /* Safari */
			doc.webkitRequestFullscreen().catch(err => console.log(err));
		} else if (doc.msRequestFullscreen) { /* IE11 */
			doc.msRequestFullscreen().catch(err => console.log(err));
		}
	}

    getSize() {
        return this.size;
    }

    onResize(size: { width: number; height: number }) {
        this.size = size;
        if (this.storeBounds) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.size', this.size);
        this.windowService.sabloService.callService('$windowService', 'resize', { name: this.name, size: this.size }, true);
    }

    onMove(location: {x: number; y: number} ) {
        this.location = location;
        if (this.storeBounds) this.windowService.localStorageService.set(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.location', this.location);
        this.windowService.sabloService.callService('$windowService', 'move', { name: this.name, location: this.location }, true);
    }

    clearBounds() {
        this.windowService.localStorageService.remove(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.location');
        this.windowService.localStorageService.remove(
            this.windowService.servoyService.getSolutionSettings().solutionName + this.name + '.storedBounds.size');
    }

    setTitle(title: string) {
        this.title = title;
        if(this.bsWindowInstance) this.bsWindowInstance.setTitle(title);
    }
}

interface IHTMLElement extends HTMLElement {
	webkitRequestFullscreen?: () => Promise<void>;
	msRequestFullscreen?: () => Promise<void>;
}
