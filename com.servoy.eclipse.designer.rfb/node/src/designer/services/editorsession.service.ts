import { Injectable, EventEmitter } from '@angular/core';
import { WebsocketSession, WebsocketService, ServicesService, ServiceProvider, TypesRegistry } from '@servoy/sablo';
import { BehaviorSubject } from 'rxjs';
import { URLParserService } from './urlparser.service';
import { EditorContentService } from './editorcontent.service';

interface CallbackFunction {
    event: string,
    function: (event: any) => boolean | void
}

@Injectable()
export class EditorSessionService implements ServiceProvider {

    private wsSession: WebsocketSession;
    private inlineEdit: boolean;
    private state = new State();
    private selection = new Array<string>();
    private selectionChangedListeners = new Array<ISelectionChangedListener>();
    private highlightChangedListeners = new Array<IShowHighlightChangedListener>();
    private dynamicGuidesChangedListeners = new Array<IShowDynamicGuidesChangedListener>();
    public stateListener: BehaviorSubject<string>;
    public autoscrollBehavior: BehaviorSubject<ISupportAutoscroll>;
    public registerCallback = new BehaviorSubject<CallbackFunction>(null);
    private allowedChildren: { [key: string]: string[] }  = { 'servoycore.servoycore-responsivecontainer': ['component', 'servoycore.servoycore-responsivecontainer'] };
    private wizardProperties: { [key: string]: string[] } = {};

    private bIsDirty = false;
    private lockAutoscrollId = '';
    
    variantsTrigger = new EventEmitter<{show: boolean, top?: number, left?: number, component?: PaletteComp}>();
    variantsScroll = new EventEmitter<{scrollPos: number}>();
    variantsPopup = new EventEmitter<{status: string}>();
    paletteRefresher : ISupportRefreshPalette;

    constructor(private websocketService: WebsocketService, private services: ServicesService,
        private urlParser: URLParserService, private editorContentService: EditorContentService, private typesRegistry: TypesRegistry) {
        this.services.setServiceProvider(this);
        this.stateListener = new BehaviorSubject('');
        this.autoscrollBehavior = new BehaviorSubject<ISupportAutoscroll>(null);
        this.editorContentService.executeOnlyAfterInit(() => {
            this.initialized();
        });
    }

    public getService(name: string) {
        if (name === '$editorService') {
            return this;
        } else if (name === '$typesRegistry') {
            return this.typesRegistry;
        }
        return null;
    }
    connect() {
        // do we need the promise
        this.wsSession = this.websocketService.connect('', [this.websocketService.getURLParameter('clientnr')]);
        if (!this.urlParser.isAbsoluteFormLayout()) {
            this.wsSession.callService('formeditor', 'getAllowedChildren').then((result: string) => {
                this.allowedChildren = JSON.parse(result) as { [key: string]: string[] } ;
                this.editorContentService.executeOnlyAfterInit(() => {
                    this.editorContentService.sendMessageToIframe({ id: 'allowedChildren', value: this.allowedChildren });
                });
            }).catch(e => console.log(e));
        }
        this.wsSession.callService('formeditor', 'getWizardProperties').then((result: { [key: string]: string[] }) => {
            this.wizardProperties = result;
        }).catch(e => console.log(e));
    }

    activated() {
        return this.wsSession.callService('formeditor', 'activated')
    }
    
    initialized() {
        void this.wsSession.callService('formeditor', 'initialized');
    }
    
    keyPressed(event: { ctrlKey?: boolean; shiftKey?: boolean; altKey?: boolean; metaKey?: boolean; keyCode?: number }) {
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

    addStyleVariantFor(variantCategory: string) {
        void this.wsSession.callService('formeditor', 'addStyleVariantFor', { p: variantCategory }, true);
    }

    editStyleVariantsFor(variantCategory: string) {
        void this.wsSession.callService('formeditor', 'editStyleVariantsFor', { p: variantCategory }, true);
    }

    getVariantsForCategory<T>(variantCategory: string) {
        return this.wsSession.callService<T>('formeditor', 'getVariantsForCategory', { variantCategory: variantCategory }, false);
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

    openConfigurator(property: string) {
        return this.wsSession.callService('formeditor', 'openConfigurator', { name: property }, false);
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

    showSameSizeIndicator() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'sameSizeIndicator': true
        }, false)
    }

    showAnchoringIndicator() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'anchoringIndicator': true
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

    isShowI18NValues() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showI18NValuesInDesigner': true
        }, false)
    }

    toggleShowI18NValues() {
        return this.wsSession.callService<boolean>('formeditor', 'toggleShow', {
            'show': 'showI18NValuesInDesigner'
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

    updateFieldPositioner(location: { x: number; y: number }) {
        void this.wsSession.callService('formeditor', 'updateFieldPositioner', {
            location: location
        }, true)
    }

    executeAction(action: string, params?) {
        void this.wsSession.callService('formeditor', action, params, true);
    }

    updateSelection(ids: Array<string>, redrawDecorators?: boolean, designerChange?: boolean) {
        this.selection = ids;
        this.selectionChangedListeners.forEach(listener => listener.selectionChanged(ids, redrawDecorators, designerChange));
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

    addDynamicGuidesChangedListener(listener: IShowDynamicGuidesChangedListener): void {
        this.dynamicGuidesChangedListeners.push(listener);
    }

    fireShowDynamicGuidesChangedListeners(result: boolean) {
        this.dynamicGuidesChangedListeners.forEach(listener => listener.showDynamicGuidesChanged(result));
    }

    getSelection(): Array<string> {
        return this.selection;
    }

    openContainedForm(uuid: string) {
        void this.wsSession.callService('formeditor', 'openContainedForm', {
            'uuid': uuid
        }, true)
    }
    
    buildTiNG() {
        void this.wsSession.callService('formeditor', 'buildTiNG', {}, true);
    }

    consoleLog(message: string) {//log message to eclipse console
        void this.wsSession.callService('formeditor', 'consoleLog', {message: message}, true);
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
        return this.wsSession.callService<{ [key: string]: string; }>('formeditor', 'getShortcuts');
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

    toggleShowDynamicGuides() {
        return this.wsSession.callService<boolean>('formeditor', 'toggleShow', {
            'show': 'showDynamicGuidesInDesigner'
        }, false);
    }

    isShowDynamicGuides() {
        return this.wsSession.callService<boolean>('formeditor', 'getBooleanState', {
            'showDynamicGuides': true
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

    getAllowedChildrenForContainer(container: string): string[] {
        if (this.allowedChildren) {
            return this.allowedChildren[container ? container : 'topContainer'];
        }
        return null;
    }

    getWizardProperties(spec: string): string[] {
        if (this.wizardProperties) {
            return this.wizardProperties[spec];
        }
        return null;
    }

    getSuperForms() {
        return this.wsSession.callService<Array<string>>('formeditor', 'getSuperForms');
    }

    setCssAnchoring(selection: Array<string>, anchors: { top: string; left: string; bottom: string; right: string }) {
        void this.wsSession.callService('formeditor', 'setCssAnchoring', { 'selection': selection, 'anchors': anchors }, true);
    }

    getFormFixedSize() {
        return this.wsSession.callService<{ width: string; height: string }>('formeditor', 'getFormFixedSize');
    }

    setFormFixedSize(args: { width: string; height?: string }) {
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

    setStatusBarText(text: string) {
        this.state.statusText = text;
        this.stateListener.next('statusText');
    }

    setSameSizeIndicator(flag: boolean) {
        this.state.sameSizeIndicator = flag;
        this.stateListener.next('sameSizeIndicator');
    }

    setAnchoringIndicator(flag: boolean) {
        this.state.anchoringIndicator = flag;
        this.stateListener.next('anchoringIndicator');
    }

    setDragging(dragging : boolean){
        this.state.dragging = dragging;
        this.stateListener.next('dragging');
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
            const obj: { [key: string]: { width: number; height: number }; } = {};
            let firstSize: { width: number; height: number } = null;
            for (let i = 0; i < selection.length; i++) {
                const nodeid = selection[i];
                const element = this.editorContentService.getContentElement(nodeid);
                if (element) {
                    const elementRect = element.getBoundingClientRect();
                    if (firstSize == null) {
                        firstSize = { width: elementRect.width, height: elementRect.height };
                    } else {
                        let newSize: { width: number; height: number };
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

    setDirty(dirty: boolean) {
        this.bIsDirty = dirty;
    }
    
    refreshPalette(){
       this.paletteRefresher.refreshPalette();
    }
    
    setPaletteRefresher( refresher : ISupportRefreshPalette){
        this.paletteRefresher = refresher;
    }
    
    registerAutoscroll(scrollComponent: ISupportAutoscroll) {
        if (this.lockAutoscrollId && scrollComponent.getAutoscrollLockId() !== this.lockAutoscrollId) return;
        this.lockAutoscrollId = scrollComponent.getAutoscrollLockId();
        if (this.autoscrollBehavior == null) {
            this.autoscrollBehavior = new BehaviorSubject(scrollComponent);
        } else {
            this.autoscrollBehavior.next(scrollComponent);
        }
    }

    unregisterAutoscroll(scrollComponent: ISupportAutoscroll) {
        if (this.lockAutoscrollId && this.lockAutoscrollId === scrollComponent.getAutoscrollLockId()) {
            this.lockAutoscrollId = '';
            this.autoscrollBehavior.next(null);
        }
    }

    getFixedKeyEvent(event: KeyboardEvent) {
        let keyCode = event.keyCode;
        if ((event.metaKey && event.key == 'Meta') || (event.ctrlKey && event.key == 'Control') || (event.altKey && event.key == 'Alt')) { 
            //standalone special keys have a javascript keyCode (91 = Meta, 17 = Ctrl, 18 = Alt) which may be wrongly interpreted in the KeyPressHandler (server side)
            //they must produce no action by themselves
            keyCode = 0
        } 
 
        return {
            keyCode: keyCode,
            ctrlKey: event.ctrlKey,
            shiftKey: event.shiftKey,
            altKey: event.altKey,
            metaKey: event.metaKey
        }
    }

    isAbsoluteFormLayout(): boolean {
        return this.urlParser.isAbsoluteFormLayout();
    }

    getSnapThreshold() {
        return this.wsSession.callService<object>('formeditor', 'getSnapThresholds', false);
    }
}

export interface ISelectionChangedListener {

    selectionChanged(selection: Array<string>, redrawDecorators?: boolean, designerChange?: boolean): void;

}

export interface IShowHighlightChangedListener {
    highlightChanged(showHighlight: boolean): void;
}

export interface IShowDynamicGuidesChangedListener {
    showDynamicGuidesChanged(result: boolean): void;
}

class State {
    showWireframe: boolean;
    showSolutionSpecificLayoutContainerClasses: boolean;
    showSolutionCss: boolean;
    sameSizeIndicator: boolean;
    anchoringIndicator: boolean;
    statusText: string;
    maxLevel: number;
    dragging = false;
    resizing = false;
    ghosthandle = false;
    pointerEvents = 'none';
    packages: Array<Package>;
    drop_highlight: string;
}

export class PaletteComp {
    name: string;
    displayName: string;
    packageName: string;
    x: number;
    y: number;
    w: number;
    h: number;
    text: string;
    type: string;
    ghostPropertyName: string;
    styleVariantCategory: string;
    lastChosenVariant: string;
    dropTargetUUID?: string;
    isOpen: boolean;
    propertyName: string; // ghost
    components: Array<PaletteComp>;
    properties: Array<string>;
    isAbsoluteCSSPositionMix?: boolean; // formcomponent property
    icon?: string;
    model?: { property: any };
    types?: Array<PaletteComp>; // the ghosts
    multiple?: boolean; //ghost property
    propertyValue?: { property: string }; // formcomponents
    componentType?: string;
    topContainer: boolean;
    layoutName?: string;
    attributes?: { [property: string]: string };
    children?: [{ [property: string]: string }];
    rightSibling?: string;
    variant?: string;
    cssPos?: { property: string };
}

export class Package {
    id: string;
    packageName: string;
    packageDisplayname: string;
    components: Array<PaletteComp>;
    propertyValues?: Array<PaletteComp>;
    categories?: { property: Array<PaletteComp> };
}

export interface ISupportAutoscroll {
    updateLocationCallback(changeX: number, changeY: number): void;
    onMouseUp(event: MouseEvent): void;
    onMouseMove(event: MouseEvent): void;
    getAutoscrollLockId(): string;
}

export interface ISupportRefreshPalette{
    refreshPalette() : void;
}

export class Variant {
    name: string;
    category: string;
    displayName: string;
    classes: Array<string>;
}