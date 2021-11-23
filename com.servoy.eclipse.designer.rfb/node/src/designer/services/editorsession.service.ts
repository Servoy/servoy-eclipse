import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { WebsocketSession, WebsocketService, ServicesService, ServiceProvider } from '@servoy/sablo';
import { BehaviorSubject } from 'rxjs';
import { URLParserService } from './urlparser.service';

@Injectable()
export class EditorSessionService implements ServiceProvider {

    private wsSession: WebsocketSession;
    private inlineEdit: boolean;
    private state = new State();
    private selection = new Array<string>();
    private selectionChangedListeners = new Array<ISelectionChangedListener>();
    private highlightChangedListeners = new Array<IShowHighlightChangedListener>();
    public stateListener: BehaviorSubject<string>;
    public autoscrollBehavior: BehaviorSubject<ISupportAutoscroll>;
    private allowedChildren: unknown;

    private bIsDirty = false;

    constructor(private websocketService: WebsocketService, private services: ServicesService,
        @Inject(DOCUMENT) private doc: Document, private urlParser: URLParserService) {
        this.services.setServiceProvider(this);
        this.stateListener = new BehaviorSubject('');
        this.autoscrollBehavior = new BehaviorSubject(null);
    }

   public getService(name: string) {
            if (name == '$editorService') {
                return this;
            }
            return null;
    }
    connect() {
        //if (deferred) return deferred.promise;
        //deferred = $q.defer();
        // var promise = deferred.promise;
        // if (!connected) testWebsocket();
        // else {
        //    deferred.resolve();
        //     deferred = null;
        // }
        // return promise;

        // do we need the promise
        this.wsSession = this.websocketService.connect('', [this.websocketService.getURLParameter('clientnr')]);
        if (!this.urlParser.isAbsoluteFormLayout()) {
            this.wsSession.callService('formeditor', 'getAllowedChildren').then((result: string) => {
                this.allowedChildren = JSON.parse(result);
				this.sendState('allowedChildren', this.allowedChildren);
            }).catch(e => console.log(e));
        }
    }

    activated() {
        return this.wsSession.callService('formeditor', 'activated')
    }

    keyPressed(event: {ctrlKey?: string; shiftKey?: string; altKey?: string; metaKey?: string; keyCode?: number}) {
        void this.wsSession.callService('formeditor', 'keyPressed', {
            ctrl: event.ctrlKey,
            shift: event.shiftKey,
            alt: event.altKey,
            meta: event.metaKey,
            keyCode: event.keyCode
        }, true)
    }

    sendChanges(properties) {
        void this.wsSession.callService('formeditor', 'setProperties', properties, true)
    }

    moveResponsiveComponent(properties) {
        void this.wsSession.callService('formeditor', 'moveComponent', properties, true)
    }

    createComponent(component) {
        void this.wsSession.callService('formeditor', 'createComponent', component, true)
    }

    getGhostComponents<T>() {
        return this.wsSession.callService<T>('formeditor', 'getGhostComponents', null, false)
    }

    getPartsStyles() {
        return this.wsSession.callService('formeditor', 'getPartsStyles', null, false)
    }

    getSystemFont() {
        return this.wsSession.callService('formeditor', 'getSystemFont', null, false)
    }

    requestSelection() {
        return this.wsSession.callService('formeditor', 'requestSelection', null, true)
    }

    setSelection(selection: Array<string>, skipListener?: ISelectionChangedListener) {
        this.selection = selection;
        void this.wsSession.callService('formeditor', 'setSelection', {
            selection: selection
        }, true);
        this.selectionChangedListeners.forEach(listener => { if (listener != skipListener) listener.selectionChanged(selection) });
    }

    isInheritedForm() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            'isInheritedForm': true
        }, false)
    }

    isShowData() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showData': true
        }, false)
    }

    isShowWireframe() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showWireframe': true
        }, false)
    }

    toggleShowWireframe() {
        const res = this.wsSession.callService<boolean>('formeditor', 'toggleShow', {
            'show': 'showWireframeInDesigner'
        }, false);
        //this.getEditor().redrawDecorators();
        return res;
    }

    isShowSolutionLayoutsCss() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showSolutionLayoutsCss': true
        }, false)
    }

    toggleShowSolutionLayoutsCss() {
        const res = this.wsSession.callService<boolean>('formeditor', 'toggleShow', {
            'show': 'showSolutionLayoutsCssInDesigner'
        }, false);
        //this.getEditor().redrawDecorators();
        return res;
    }

    isShowSolutionCss() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showSolutionCss': true
        }, false)
    }

    toggleShowSolutionCss() {
        return this.wsSession.callService<boolean>('formeditor', 'toggleShow', {
            'show': 'showSolutionCssInDesigner'
        }, false);
    }

    createComponents(components) {
        void this.wsSession.callService('formeditor', 'createComponents', components, true)
    }

    openElementWizard(elementType: string) {
        void this.wsSession.callService('formeditor', 'openElementWizard', {
            elementType: elementType
        }, true)
    }

    updateFieldPositioner(location: {x: number;y: number}) {
        void this.wsSession.callService('formeditor', 'updateFieldPositioner', {
            location: location
        }, true)
    }

    executeAction(action: string, params?) {
        void this.wsSession.callService('formeditor', action, params, true)
    }

    updateSelection(ids: Array<string>, redrawDecorators?: boolean) {
        this.selection = ids;
        this.selectionChangedListeners.forEach(listener => listener.selectionChanged(ids, redrawDecorators));
    }

    addSelectionChangedListener(listener: ISelectionChangedListener): () => void {
        this.selectionChangedListeners.push(listener);
        return () => this.removeSelectionChangedListener(listener);
    }
    removeSelectionChangedListener(listener: ISelectionChangedListener): void {
        const index = this.selectionChangedListeners.indexOf(listener);
        if (index > -1) {
            this.selectionChangedListeners.splice(index, 1);
        }
    }

    addHighlightChangedListener(listener: IShowHighlightChangedListener): void {
        this.highlightChangedListeners.push(listener);
    }

    fireHighlightChangedListeners(showHighlight: boolean) {
        this.highlightChangedListeners.forEach(listener => listener.highlightChanged(showHighlight));
    }

    getSelection(): Array<string> {
        return this.selection;
    }

    openContainedForm(uuid: string) {
        void this.wsSession.callService('formeditor', 'openContainedForm', {
            'uuid': uuid
        }, true)
    }

    setInlineEditMode(edit: boolean) {
        this.inlineEdit = edit
        void this.wsSession.callService('formeditor', 'setInlineEditMode', {
            'inlineEdit': this.inlineEdit
        }, true)
    }

    isInlineEditMode() {
        return this.inlineEdit;
    }

    getComponentPropertyWithTags(svyId: string, propertyName: string) {
        return this.wsSession.callService('formeditor', 'getComponentPropertyWithTags', {
            'svyId': svyId,
            'propertyName': propertyName
        }, false);
    }

    getShortcuts() {
        return this.wsSession.callService< { [key: string]: string; }>('formeditor', 'getShortcuts');
    }

    toggleHighlight() {
        return this.wsSession.callService<boolean>('formeditor', 'toggleShow', {
            'show': 'showHighlightInDesigner'
        }, false);
    }

    isShowHighlight() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showHighlight': true
        }, false)
    }

    toggleShowData() {
        void this.wsSession.callService('formeditor', 'toggleShowData', null, true);
    }

    isHideInherited() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'isHideInherited': false
        }, false)
    }

    updatePaletteOrder(paletteOrder) {
        return this.wsSession.callService('formeditor', 'updatePaletteOrder', paletteOrder, false);
    }

    openPackageManager() {
        return this.wsSession.callService('formeditor', 'openPackageManager', null, true);
    }

    getAllowedChildrenForContainer(container:string): string[] {
        if (this.allowedChildren) {
            return this.allowedChildren[container ? container : 'topContainer'];
        }
        return null;
    }

    getSuperForms() {
        return this.wsSession.callService<Array<string>>('formeditor', 'getSuperForms');
    }

    setCssAnchoring(selection: Array<string>, anchors: {top:string;left:string;bottom:string;right:string}) {
        void this.wsSession.callService('formeditor', 'setCssAnchoring', { 'selection': selection, 'anchors': anchors }, true);
    }

    getFormFixedSize() {
        return this.wsSession.callService<{width:string;height:string}>('formeditor', 'getFormFixedSize');
    }

    setFormFixedSize(args: {width:string;height?:string}) {
        return this.wsSession.callService('formeditor', 'setFormFixedSize', args);
    }

    getZoomLevel() {
       return this.wsSession.callService<number>('formeditor', 'getZoomLevel', {}, false);
    }

    setZoomLevel(value: number) {
        void this.wsSession.callService('formeditor', 'setZoomLevel', {
            'zoomLevel': value
        }, false)
    }

    setStatusBarText(text:string) {
        this.state.statusText = text;
        this.stateListener.next('statusText');
    }

    getState(): State {
        return this.state;
    }

    getSession(): WebsocketSession {
        return this.wsSession;
    }

    sameSize(width: boolean) {
        const selection = this.getSelection();
        if (selection && selection.length > 1) {
            const obj: { [key: string]: {width:number; height:number} ; } = {};
            let firstSize: {width:number; height:number} = null;
            const frameElem = this.doc.querySelector('iframe');
            for (let i = 0; i < selection.length; i++) {
                const nodeid = selection[i];
                const element = frameElem.contentWindow.document.querySelector("[svy-id='" + nodeid + "']");
                if (element) {
                    const elementRect = element.getBoundingClientRect();
                    if (firstSize == null) {
                        firstSize = { width: elementRect.width, height: elementRect.height };
                    } else {
                        let newSize:  {width:number; height:number};
                        if (width) {
                            newSize = {
                                width: firstSize.width,
                                height: elementRect.height
                            };
                        } else {
                            newSize = {
                                width: elementRect.width,
                                height: firstSize.height
                            };
                        }
                        obj[nodeid] = newSize;
                    }
                }
            }
            this.sendChanges(obj);
        }
    }

    isDirty(): boolean {
        return this.bIsDirty;
    }

    sendState(key: string, result: unknown): void {
        const iframe = this.doc.querySelector('iframe');
        const elements = iframe.contentWindow.document.querySelectorAll('[svy-id]');
        if (elements.length == 0) {
            setTimeout(() => this.sendState(key, result), 400);
            return;
        }
        else {
            iframe.contentWindow.postMessage({ id: key, value: result }, '*');
        }
    }

    startAutoscroll(scrollComponent: ISupportAutoscroll){
        if ( this.autoscrollBehavior == null) {
            this.autoscrollBehavior = new BehaviorSubject(scrollComponent);
        }  else {
            this.autoscrollBehavior.next(scrollComponent);
        }
    }

    stopAutoscroll(){
        this.autoscrollBehavior.next(null);
    }
}

export interface ISelectionChangedListener {

    selectionChanged(selection: Array<string>, redrawDecorators?:boolean): void;

}

export interface IShowHighlightChangedListener {

    highlightChanged(showHighlight: boolean): void;

}

class State {
    showWireframe: boolean;
    showSolutionSpecificLayoutContainerClasses: boolean;
    showSolutionCss: boolean;
    statusText: string;
    maxLevel: number;
    dragging = false;
    pointerEvents = 'none';
    packages : Array<Package>;
	drop_highlight: string;
}

export class PaletteComp {
    name: string;
    displayName : string;
    packageName: string;
    x: number;
    y: number;
    type: string;
    ghostPropertyName: string;
    dropTargetUUID?: string; 
    isOpen: boolean;
    propertyName: string; // ghost
    components: Array<PaletteComp>;
    properties: Array<string>;
    isAbsoluteCSSPositionMix ?:boolean; // formcomponent property
    icon? : string;
    model? : {property : any};
    types? : Array<PaletteComp>; // the ghosts
    multiple? : boolean; //ghost property
    propertyValue?: {property : string}; // formcomponents
    componentType?: string;
    topContainer: boolean;
    layoutName?: string;
    attributes?: { [property: string]: string };
    rightSibling?: string;
}

export class Package {
    id: string;
    packageName: string;
    packageDisplayname : string;
    components: Array<PaletteComp>;
    propertyValues?: Array<PaletteComp>;
    categories?: {property : PaletteComp};
}

export interface ISupportAutoscroll {
    getUpdateLocationCallback(): (changeX: number, changeY: number, minX?: number, minY?: number) => void;
    onMouseUp(event: MouseEvent): void;
}