import { Injectable } from '@angular/core';
import { WebsocketSession, WebsocketService } from '@servoy/sablo';

@Injectable()
export class EditorSessionService {
    
    private wsSession: WebsocketSession;
    private inlineEdit: boolean;
    private state = new State();

    
    constructor(private websocketService: WebsocketService) {
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
        this.wsSession = this.websocketService.connect('', [this.websocketService.getURLParameter('clientnr')])
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

    getGhostComponents(node) {
        return this.wsSession.callService('formeditor', 'getGhostComponents', node, false)
    }

    getPartsStyles() {
        return this.wsSession.callService('formeditor', 'getPartsStyles', null, false)
    }

    getSystemFont(node) {
        return this.wsSession.callService('formeditor', 'getSystemFont', null, false)
    }

    requestSelection(node) {
        return this.wsSession.callService('formeditor', 'requestSelection', null, true)
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

    loadAllowedChildren() {
        return this.wsSession.callService('formeditor', 'getAllowedChildren');
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

    getState(): State {
        return this.state;
    }
}

class State {
    showWireframe: boolean;
    design_highlight: string;
    showSolutionLayoutsCss: boolean;
    showSolutionCss: boolean;
    maxLevel: any;
}
