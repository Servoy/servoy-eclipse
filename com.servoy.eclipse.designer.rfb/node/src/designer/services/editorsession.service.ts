import { DOCUMENT } from '@angular/common';
import { Inject, Injectable } from '@angular/core';
import { WebsocketSession, WebsocketService, ServicesService, ServiceProvider } from '@servoy/sablo';
import { BehaviorSubject, Observable, Observer } from 'rxjs';
import { URLParserService } from './urlparser.service';

@Injectable()
export class EditorSessionService {

    private wsSession: WebsocketSession;
    private inlineEdit: boolean;
    private state = new State();
    private selection = new Array<string>();
    private selectionChangedListeners = new Array<ISelectionChangedListener>();
    private highlightChangedListeners = new Array<IShowHighlightChangedListener>();
    public stateListener: BehaviorSubject<string>;
    private allowedChildren: any;

    private bIsDirty = false;

    constructor(private websocketService: WebsocketService, private services: ServicesService,
        @Inject(DOCUMENT) private doc: Document, private urlParser: URLParserService) {
        let _this = this;
        this.services.setServiceProvider({
            getService(name: string) {
                if (name == '$editorService') {
                    return _this;
                }
                return null;
            }
        } as ServiceProvider)
        this.stateListener = new BehaviorSubject('');
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
            this.wsSession.callService('formeditor', 'getAllowedChildren').then((result) => {
                this.allowedChildren = JSON.parse(result);
            });
        }
    }

    activated() {
        return this.wsSession.callService('formeditor', 'activated')
    }

    keyPressed(event) {
        this.wsSession.callService('formeditor', 'keyPressed', {
            ctrl: event.ctrlKey,
            shift: event.shiftKey,
            alt: event.altKey,
            meta: event.metaKey,
            keyCode: event.keyCode
        }, true)
    }

    sendChanges(properties) {
        this.wsSession.callService('formeditor', 'setProperties', properties, true)
    }

    moveResponsiveComponent(properties) {
        this.wsSession.callService('formeditor', 'moveComponent', properties, true)
    }

    createComponent(component) {
        this.wsSession.callService('formeditor', 'createComponent', component, true)
    }

    getGhostComponents() {
        return this.wsSession.callService('formeditor', 'getGhostComponents', null, false)
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
        this.wsSession.callService('formeditor', 'setSelection', {
            selection: selection
        }, true);
        this.selectionChangedListeners.forEach(listener => { if (listener != skipListener) listener.selectionChanged(selection) });
    }

    isInheritedForm() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "isInheritedForm": true
        }, false)
    }

    isShowData() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "showData": true
        }, false)
    }

    isShowWireframe() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "showWireframe": true
        }, false)
    }

    toggleShowWireframe() {
        var res = this.wsSession.callService('formeditor', 'toggleShow', {
            "show": "showWireframeInDesigner"
        }, false);
        //this.getEditor().redrawDecorators();
        return res;
    }

    isShowSolutionLayoutsCss() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "showSolutionLayoutsCss": true
        }, false)
    }

    toggleShowSolutionLayoutsCss() {
        var res = this.wsSession.callService('formeditor', 'toggleShow', {
            "show": "showSolutionLayoutsCssInDesigner"
        }, false);
        //this.getEditor().redrawDecorators();
        return res;
    }

    isShowSolutionCss() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "showSolutionCss": true
        }, false)
    }

    toggleShowSolutionCss() {
        return this.wsSession.callService('formeditor', 'toggleShow', {
            "show": "showSolutionCssInDesigner"
        }, false);
    }

    createComponents(components) {
        this.wsSession.callService('formeditor', 'createComponents', components, true)
    }

    openElementWizard(elementType) {
        this.wsSession.callService('formeditor', 'openElementWizard', {
            elementType: elementType
        }, true)
    }

    updateFieldPositioner(location) {
        this.wsSession.callService('formeditor', 'updateFieldPositioner', {
            location: location
        }, true)
    }

    executeAction(action, params?) {
        this.wsSession.callService('formeditor', action, params, true)
    }

    updateSelection(ids) {
        this.selection = ids;
        this.selectionChangedListeners.forEach(listener => listener.selectionChanged(ids));
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

    openContainedForm(ghost) {
        this.wsSession.callService('formeditor', 'openContainedForm', {
            "uuid": ghost.uuid
        }, true)
    }

    setInlineEditMode(edit) {
        this.inlineEdit = edit
        this.wsSession.callService('formeditor', 'setInlineEditMode', {
            "inlineEdit": this.inlineEdit
        }, true)
    }

    isInlineEditMode() {
        return this.inlineEdit;
    }

    getComponentPropertyWithTags(svyId, propertyName) {
        return this.wsSession.callService('formeditor', 'getComponentPropertyWithTags', {
            "svyId": svyId,
            "propertyName": propertyName
        }, false);
    }

    getShortcuts() {
        return this.wsSession.callService('formeditor', 'getShortcuts');
    }

    toggleHighlight() {
        return this.wsSession.callService('formeditor', 'toggleShow', {
            "show": "showHighlightInDesigner"
        }, false);
    }

    isShowHighlight() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "showHighlight": true
        }, false)
    }

    toggleShowData() {
        this.wsSession.callService('formeditor', 'toggleShowData', null, true);
    }

    isHideInherited() {
        return this.wsSession.callService('formeditor', 'getBooleanState', {
            "isHideInherited": false
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
        return this.wsSession.callService('formeditor', 'getSuperForms');
    }

    setCssAnchoring(selection, anchors) {
        this.wsSession.callService('formeditor', 'setCssAnchoring', { "selection": selection, "anchors": anchors }, true);
    }

    getFormFixedSize() {
        return this.wsSession.callService('formeditor', 'getFormFixedSize');
    }

    setFormFixedSize(args) {
        return this.wsSession.callService('formeditor', 'setFormFixedSize', args);
    }

    getZoomLevel() {
        return this.wsSession.callService('formeditor', 'getZoomLevel', {}, false)
    }

    setZoomLevel(value) {
        return this.wsSession.callService('formeditor', 'setZoomLevel', {
            "zoomLevel": value
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
        var selection = this.getSelection();
        if (selection && selection.length > 1) {
            let obj = {};
            let firstSize = null;
            let frameElem = this.doc.querySelector('iframe');
            for (let i = 0; i < selection.length; i++) {
                let nodeid = selection[i];
                let element = frameElem.contentWindow.document.querySelector("[svy-id='" + nodeid + "']");
                if (element) {
                    let elementRect = element.getBoundingClientRect();
                    if (firstSize == null) {
                        firstSize = { width: elementRect.width, height: elementRect.height };
                    } else {
                        let newSize;
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
}

export interface ISelectionChangedListener {

    selectionChanged(selection: Array<string>): void;

}

export interface IShowHighlightChangedListener {

    highlightChanged(showHighlight: boolean): void;

}

class State {
    showWireframe: boolean;
    showSolutionSpecificLayoutContainerClasses: boolean;
    showSolutionCss: boolean;
    statusText: string;
    maxLevel: any;
    dragging: boolean = false;
    pointerEvents: string = 'none';
}
