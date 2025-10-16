import { Component, Pipe, PipeTransform, Renderer2, AfterViewInit, OnDestroy } from '@angular/core';
import { EditorSessionService, Package, PaletteComp, ISupportAutoscroll, ISupportRefreshPalette } from '../services/editorsession.service';
import { HttpClient } from '@angular/common/http';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';
import { WindowRefService } from '@servoy/public';
import { DynamicGuidesService, SnapData } from '../services/dynamicguides.service';
import { Subscription } from 'rxjs';

@Component({
    selector: 'designer-palette',
    templateUrl: './palette.component.html',
    styleUrls: ['./palette.component.css'],
    standalone: false
})
export class PaletteComponent implements ISupportAutoscroll, ISupportRefreshPalette, AfterViewInit, OnDestroy {

    public searchText: string;
    public activeIds: Array<string>;

    dragItem: DragItem = {};
    canDrop: { dropAllowed: boolean, dropTarget?: Element, beforeChild?: Element, append?: boolean };
    draggedVariant: DraggedVariant = {};
    isDraggedVariant = false;
    snapData: SnapData;
    subscription: Subscription;

    searchHistory: string[] = [];
    filteredSuggestions: string[] = [];
    showSuggestions = false;
    keepSuggestionsOpen = false;
    showSearchDeleteBtn = false;

    constructor(protected readonly editorSession: EditorSessionService, private http: HttpClient, private urlParser: URLParserService,
        protected readonly renderer: Renderer2, protected designerUtilsService: DesignerUtilsService, private editorContentService: EditorContentService,
        private windowRef: WindowRefService, private guidesService: DynamicGuidesService) {

        this.editorSession.setPaletteRefresher(this);
        this.refreshPalette();
        this.windowRef.nativeWindow.addEventListener('message', (event) => {
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            if (event.data.id === 'onVariantMouseDown') {
                //element
                // eslint-disable-next-line @typescript-eslint/no-unsafe-argument, @typescript-eslint/no-unsafe-member-access
                this.onVariantMouseDown(event.data.pageX, event.data.pageY, event.data.model);
            }
            // eslint-disable-next-line @typescript-eslint/no-unsafe-member-access
            if (event.data.id === 'onVariantMouseUp') {
                //element
                this.onVariantMouseUp();
            }

            if (event.data.id === 'onDestroyVariants') {
                this.onVariantsDestroy();
            }
        });
        this.editorContentService.getBodyElement().addEventListener('mouseup', this.onMouseUp);
        this.editorContentService.getBodyElement().addEventListener('mousemove', this.onMouseMove);
        this.editorContentService.getBodyElement().addEventListener('keyup', (event: KeyboardEvent) => {
            if (event.keyCode == 27) {
                // esc key, close menu
                this.editorSession.variantsTrigger.emit({ show: false });
            }
        });
        
        this.searchHistory = localStorage.getItem('searchHistory') ? JSON.parse(localStorage.getItem('searchHistory')) : [];
    }

    ngAfterViewInit(): void {
        if (this.urlParser.isAbsoluteFormLayout()) {
            this.subscription = this.guidesService.snapDataListener.subscribe((value: SnapData) => {
                this.snap(value);
            })
        }
    }

    ngOnDestroy(): void {
        if (this.subscription !== undefined) this.subscription.unsubscribe();
    }

    openPackageManager() {
        void this.editorSession.openPackageManager();
    }

    onClick(component: PaletteComp) {
        component.isOpen = !component.isOpen;
    }

    onPaletteScroll() {
        this.editorSession.variantsScroll.emit({ scrollPos: this.editorContentService.getPallete().scrollTop });
    }
    
    onFavoriteCLick(event: MouseEvent, component: PaletteComp) {
        event.stopPropagation();
        this.editorSession.updateFavoritesComponents(component);
    }

    onVariantClick(event: MouseEvent, component: PaletteComp, packageName: string) {
        this.draggedVariant.packageName = packageName;
        this.draggedVariant.name = component.name;
        this.draggedVariant.type = component.componentType;
        let targetElement: HTMLElement = (event.target as HTMLElement).parentElement;
        while (targetElement.nodeName.toUpperCase() != 'LI') {
            if (!targetElement.parentElement || targetElement.parentElement.nodeName.toUpperCase() == 'UL')
                break;
            targetElement = targetElement.parentElement;
        }
        this.draggedVariant.element = targetElement.cloneNode(true) as Element;

        if (this.draggedVariant.element.children) {//without this check I'll get a console exception of null children
            Array.from(this.draggedVariant.element.children).forEach((child) => {
                if (child.tagName.toUpperCase() == 'UL' || child.nodeName.toUpperCase() == 'DESIGNER-VARIANTSCONTENT') {
                    this.draggedVariant.element.removeChild(child);
                }
            });
        }

        let variantBtn = this.editorContentService.getDocument().elementFromPoint(event.pageX, event.pageY) as HTMLButtonElement;
        if (variantBtn.tagName === 'I') { //clicked on the inner down arrow
            variantBtn = variantBtn.parentElement as HTMLButtonElement;
        }
        if (variantBtn.tagName === 'BUTTON') { //clicked on the inner or button
            this.editorSession.variantsTrigger.emit({ show: true, top: variantBtn.offsetTop, left: variantBtn.offsetLeft, component: component });
        } //else a very narrow margin (cca. 1 px) of this component was clicked and popup will be wrongly positioned
    }

    onVariantMouseDown(pageX: number, pageY: number, model: { [property: string]: unknown }) {
        this.isDraggedVariant = true;
        this.draggedVariant.variant = model._variantName as string;
        this.draggedVariant.size = model.size as { width: number; height: number };
        this.draggedVariant.text = model.text as string;

        this.dragItem.paletteItemBeingDragged = this.draggedVariant.element;
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'left', this.editorContentService.getTopPositionIframe(true) + pageX + 'px');
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'top', this.editorContentService.getTopPositionIframe(true) + pageY + 'px');
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'position', 'absolute');
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'list-style-type', 'none');
        this.editorContentService.getBodyElement().appendChild(this.dragItem.paletteItemBeingDragged);

        this.dragItem.elementName = this.draggedVariant.name;
        this.dragItem.packageName = this.draggedVariant.packageName;
        this.dragItem.ghost = null;
        this.dragItem.propertyName = null;
        this.dragItem.propertyValue = null;
        this.dragItem.topContainer = null;

        this.dragItem.componentType = this.draggedVariant.type;
        this.dragItem.layoutName = null;
        this.dragItem.attributes = null;
        this.dragItem.model = model;

        this.canDrop = { dropAllowed: false };
        this.editorSession.getState().dragging = true;
        this.editorContentService.sendMessageToIframe({
            id: 'createElement',
            name: this.convertToJSName(this.dragItem.elementName),
            model: model,
            type: this.dragItem.componentType,
            attributes: this.dragItem.attributes,
            children: null
        });
    }

    onVariantMouseUp() {
        if (this.dragItem.paletteItemBeingDragged) {
            this.editorContentService.getBodyElement().removeChild(this.dragItem.paletteItemBeingDragged);
            this.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
            this.dragItem.paletteItemBeingDragged = null;
            this.dragItem.contentItemBeingDragged = null;
        }
    }

    onVariantsDestroy() {
        if (this.dragItem.paletteItemBeingDragged) {
            this.draggedVariant.element = null;
        }
    }

    onMouseDown(event: MouseEvent, elementName: string, packageName: string, model: { [property: string]: unknown }, ghost: PaletteComp, propertyName?: string, propertyValue?: { [property: string]: string }, componentType?: string, topContainer?: boolean, layoutName?: string, attributes?: { [property: string]: string }, children?: [{ [property: string]: string }]) {
        if (event.target && ((event.target as Element).getAttribute('name') === 'variants' || (event.target as Element).getAttribute('name') === 'favIcon') || (event.target as HTMLElement).id === 'chevron') {
            return; // it has a separate handler
        }
        event.stopPropagation();
        this.editorSession.variantsTrigger.emit({ show: false });

        let target = event.target as HTMLElement;
        if (target.localName === 'designer-variantscontent') {
            target = target.closest('li');
        } else if (target.localName === 'img' && target.id === '') {
            target = target.closest('li').querySelector('span');
        }
        this.dragItem.paletteItemBeingDragged = target.cloneNode(true) as Element;
        Array.from(this.dragItem.paletteItemBeingDragged.children).forEach(child => {
            if (child.tagName === 'DESIGNER-VARIANTSCONTENT' || child.getAttribute('name') === 'favIcon' || child.id === 'chevron') {
                this.dragItem.paletteItemBeingDragged.removeChild(child);
            }
        })
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'left', event.pageX + 'px');
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'top', event.pageY + 'px');
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'position', 'absolute');
        this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'list-style-type', 'none');
        this.editorContentService.getBodyElement().appendChild(this.dragItem.paletteItemBeingDragged);

        this.dragItem.elementName = elementName;
        this.dragItem.packageName = packageName;
        this.dragItem.ghost = ghost;
        this.dragItem.propertyName = propertyName;
        this.dragItem.propertyValue = propertyValue;
        this.dragItem.topContainer = topContainer;
        this.dragItem.componentType = componentType;
        this.dragItem.layoutName = layoutName;
        this.dragItem.attributes = attributes;
        this.dragItem.model = model;

        this.canDrop = { dropAllowed: false };
        if (!ghost) {
            this.editorSession.setDragging(true);
            this.editorContentService.sendMessageToIframe({ id: 'createElement', name: this.convertToJSName(elementName), model: model, type: componentType, attributes: attributes, children: children });
        }

        this.editorSession.registerAutoscroll(this);
    }

    onMouseUp = (event: MouseEvent) => {
        if (event.target && (event.target as Element).className === 'popover-body') {
            return; // it has a separate handler
        }
        if (this.canDrop && !this.canDrop.dropTarget) {
            this.canDrop = this.designerUtilsService.getDropNode(this.urlParser.isAbsoluteFormLayout(), this.dragItem.componentType, this.dragItem.topContainer, this.dragItem.layoutName ? this.dragItem.packageName + '.' + this.dragItem.layoutName : this.dragItem.layoutName, event, this.dragItem.elementName);   
        }
        if (this.dragItem.paletteItemBeingDragged) {
            this.editorSession.setDragging(false);
            this.editorContentService.getBodyElement().removeChild(this.dragItem.paletteItemBeingDragged);
            this.dragItem.paletteItemBeingDragged = null;
            this.dragItem.contentItemBeingDragged = null;
            this.editorContentService.getGlassPane().style.cursor = '';

            const component = {} as PaletteComp;
            component.name = this.dragItem.elementName;
            component.packageName = this.dragItem.packageName;

            if (this.snapData) {
                component.x = Math.round(this.snapData.left);
                component.y = Math.round(this.snapData.top);
                if (this.snapData.width) {
                    component.w = Math.round(this.snapData.width);
                }
                if (this.snapData.height) {
                    component.h = Math.round(this.snapData.height);
                }
                component.cssPos = this.snapData.cssPosition;
                this.snapData = null;
            }
            else {
                component.x = event.pageX;
                component.y = event.pageY;
                if (this.urlParser.isAbsoluteFormLayout()) {
                    component.x = component.x - this.editorContentService.getLeftPositionIframe();
                    component.y = component.y - this.editorContentService.getTopPositionIframe();
                }
            }

            if (this.isDraggedVariant) {
                component.w = this.draggedVariant.size.width;
                component.h = this.draggedVariant.size.height;
                component.text = this.draggedVariant.text;
                if (this.draggedVariant.variant) {
                    component.variant = this.draggedVariant.variant;
                }
                this.isDraggedVariant = false;
            }

            if (this.urlParser.isAbsoluteFormLayout()) {
                if (this.canDrop.dropAllowed && this.canDrop.dropTarget) {
                    component.dropTargetUUID = this.canDrop.dropTarget.getAttribute('svy-id');
                }
                if (this.canDrop.beforeChild) {
                    component.rightSibling = this.canDrop.beforeChild.getAttribute('svy-id');
                }
            }
            else {
                if (this.canDrop.dropAllowed) {
                    if (this.canDrop.dropTarget) {
                        component.dropTargetUUID = this.canDrop.dropTarget.getAttribute('svy-id');
                    }
                    if (this.canDrop.beforeChild) {
                        component.rightSibling = this.canDrop.beforeChild.getAttribute('svy-id');
                    }
                } else if (!this.dragItem.ghost) {
                    this.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
                    this.editorSession.unregisterAutoscroll(this);
                    return;
                }
            }

            if (this.dragItem.ghost) {
                const elements = this.editorContentService.getAllContentElements();
                const found = Array.from(elements).find((node) => {
                    const position = this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                    if (position.x <= component.x && position.x + position.width >= component.x && position.y <= component.y && position.y + position.height >= component.y) {
                        const types = node.getAttribute('svy-types');
                        if (types && types.split(',').indexOf(this.dragItem.ghost.type) >= 0) {
                            this.editorSession.unregisterAutoscroll(this);
                            return node;
                        }
                    }
                });
                if (!found) return;
                component.type = this.dragItem.ghost.type;
                component.ghostPropertyName = this.dragItem.ghost.propertyName;
                component.dropTargetUUID = found.getAttribute('svy-id');
            }
            if (this.dragItem.propertyName) {
                component[this.dragItem.propertyName] = this.dragItem.propertyValue;
            }

            if (component.x >= 0 && component.y >= 0) {
                this.editorSession.createComponent(component);
                this.editorContentService.getContentArea().focus();
            }

            this.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
        }
        this.editorSession.unregisterAutoscroll(this);

        if (this.draggedVariant.element) {
            this.draggedVariant.element = null;
            this.draggedVariant.variant = null;
            this.editorSession.variantsTrigger.emit({ show: false });
        }
    }

    onMouseMove = (event: MouseEvent) => {
        const paletteRect: DOMRect = this.editorContentService.getPallete().getBoundingClientRect();
        if (event.pageX >= paletteRect.width && event.pageX >= this.editorContentService.getLeftPositionIframe() && event.pageY >= this.editorContentService.getTopPositionIframe() && this.dragItem.paletteItemBeingDragged && this.dragItem.contentItemBeingDragged) {
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'opacity', '0');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
        }

        if (this.snapData) return;

        if (this.dragItem.paletteItemBeingDragged) {
        
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'left', event.pageX + 'px');
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'top', event.pageY + 'px');
            if (this.dragItem.contentItemBeingDragged) {
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'left', event.pageX - this.editorContentService.getLeftPositionIframe() + 'px');
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'top', event.pageY - this.editorContentService.getTopPositionIframe() + 'px');

                this.canDrop = this.designerUtilsService.getDropNode(this.urlParser.isAbsoluteFormLayout(), this.dragItem.componentType, this.dragItem.topContainer, this.dragItem.layoutName ? this.dragItem.packageName + '.' + this.dragItem.layoutName : this.dragItem.layoutName, event, this.dragItem.elementName);

                if (!this.canDrop.dropAllowed) {
                    this.editorContentService.getGlassPane().style.cursor = 'not-allowed';
                }
                else {
                    this.editorContentService.getGlassPane().style.cursor = 'pointer';
                }

                if (this.dragItem.contentItemBeingDragged) {
                    if (this.canDrop.dropAllowed) {
                        //TODO do we need to optimize the calls to insert the dragged component?
                        this.editorContentService.sendMessageToIframe({
                            id: 'insertDraggedComponent',
                            dropTarget: this.canDrop.dropTarget && !this.canDrop.dropTarget.classList.contains('svy-csspositioncontainer') ? this.canDrop.dropTarget.getAttribute('svy-id') : null,
                            insertBefore: this.canDrop.beforeChild ? this.canDrop.beforeChild.getAttribute('svy-id') : null
                        });
                    }
                    if (this.canDrop.dropAllowed && (this.canDrop.dropTarget || !this.urlParser.isAbsoluteFormLayout()) && !this.canDrop.dropTarget?.classList.contains('svy-csspositioncontainer')) {
                        // hide the dragged item and rely on inserted item at specific parent 
                        this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '0');
                        this.renderer.removeClass(this.dragItem.contentItemBeingDragged, 'highlight_element');
                    } else {
                        this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
                        this.renderer.addClass(this.dragItem.contentItemBeingDragged, 'highlight_element');
                    }
                }
            }
            else {
                if (!this.dragItem.ghost) {
                    // if is a type, do try to create the preview
                    this.dragItem.contentItemBeingDragged = this.editorContentService.getContentElementById('svy_draggedelement');
                    if (this.dragItem.contentItemBeingDragged) {
                        this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '0');
                    }
                }
                else {
                    const elements = this.editorContentService.getAllContentElements();
                    const x = event.pageX - this.editorContentService.getLeftPositionIframe();
                    const y = event.pageY - this.editorContentService.getTopPositionIframe();
                    const found = Array.from(elements).find((node) => {
                        const position = this.designerUtilsService.adjustElementRect(node, node.getBoundingClientRect());
                        if (position.x <= x && position.x + position.width >= x && position.y <= y && position.y + position.height >= y) {
                            const types = node.getAttribute('svy-types');
                            if (types && types.split(',').indexOf(this.dragItem.ghost.type) >= 0) {
                                return node;
                            }
                        }
                    });
                    if (!found) {
                        this.editorContentService.getGlassPane().style.cursor = 'not-allowed';
                    }
                    else {
                        this.editorContentService.getGlassPane().style.cursor = 'pointer';
                    }
                }
            }
        }
    }

    convertToJSName(name: string): string {
        // this should do the same as websocket.ts #scriptifyServiceNameIfNeeded() and ClientService.java #convertToJSName()
        if (name) {
            const packageAndName = name.split('-');
            if (packageAndName.length > 1) {
                name = packageAndName[0];
                for (let i = 1; i < packageAndName.length; i++) {
                    if (packageAndName[1].length > 0) name += packageAndName[i].charAt(0).toUpperCase() + packageAndName[i].slice(1);
                }
            }
        }
        return name;
    }

    getPackages(): Array<Package> {
        return this.editorSession.getState().packages;
    }

    updateLocationCallback(changeX: number, changeY: number) {
        this.editorContentService.getContentArea().scrollTop += changeY;
        this.editorContentService.getContentArea().scrollLeft += changeX;
    }

    getAutoscrollLockId(): string {
        return 'palette';
    }

    refreshPalette(): void {
        let layoutType: string;
        if (this.urlParser.isAbsoluteFormLayout())
            layoutType = 'Absolute-Layout';
        else
            layoutType = 'Responsive-Layout';
        this.activeIds = [];
        this.http.get('/designer/palette?layout=' + layoutType + '&formName=' + this.urlParser.getFormName()).subscribe((got: Array<Package>) => {
            let packages: Array<Package>
            let propertyValues: Array<PaletteComp>;
            if (got[got.length - 1] && got[got.length - 1].propertyValues) {
                propertyValues = got[got.length - 1].propertyValues;
                packages = got.slice(0, got.length - 1);
            }
            else {
                packages = got;
            }
            for (let i = 0; i < packages.length; i++) {
                packages[i].id = ('svy_' + packages[i].packageName).replace(/[|&;$%@"<>()+,]/g, '').replace(/\s+/g, '_');
                this.activeIds.push(packages[i].id);
                if (packages[i].components) {
                    for (let j = 0; j < packages[i].components.length; j++) {
                        if (propertyValues && packages[i].components[j].properties) {
                            packages[i].components[j].isOpen = false;
                            //we still need to have the components with properties on the component for filtering

                            if (propertyValues && propertyValues.length && packages[i].components[j].name == 'servoycore-formcomponent') {
                                const newPropertyValues: Array<PaletteComp> = [];
                                for (let n = 0; n < propertyValues.length; n++) {
                                    if (!propertyValues[n].isAbsoluteCSSPositionMix) {
                                        newPropertyValues.push(propertyValues[n]);
                                    }
                                }
                                packages[i].components[j].components = newPropertyValues;
                            }
                            else if (packages[i].components[j].name != 'servoycore-listformcomponent') {
                                // added autowizard for list form component 
                                packages[i].components[j].components = propertyValues;
                            }
                            else {
                                packages[i].components[j].properties = null;
                            }
                        }
                    }
                }
            }
            this.editorSession.getState().packages = packages;
        });
    }

    snap(data: SnapData) {
        if (this.dragItem?.paletteItemBeingDragged && !this.dragItem.ghost && !this.dragItem.contentItemBeingDragged) {
            this.dragItem.contentItemBeingDragged = this.editorContentService.getContentElementById('svy_draggedelement');
        }
        if (this.dragItem?.contentItemBeingDragged) {
            this.snapData = data;
            if (this.snapData?.top && this.snapData?.left) {
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'left', this.snapData.left + 'px');
                if (this.snapData?.width) {
                    this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'width', this.snapData.width + 'px');
                }
                if (this.snapData?.height) {
                    this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'height', this.snapData.height + 'px');
                }
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'top', this.snapData.top + 'px');
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
                this.renderer.addClass(this.dragItem.contentItemBeingDragged, 'highlight_element');
            }
        }
        else {
            this.snapData = null;
        }
    }

    clearSearch(): void {
        this.addToHistory(this.searchText);
        this.searchText = '';
        this.showSearchDeleteBtn = false;
    }

    onSearchInput(value: string): void {
        this.updateDeleteVisibility();
        this.filteredSuggestions = this.filterSuggestions(this.searchText);
    }

    openSuggestions(): void {
        this.searchHistory = localStorage.getItem('searchHistory') ? JSON.parse(localStorage.getItem('searchHistory')) : [];
        this.keepSuggestionsOpen = false;
        this.showSuggestions = true;
        this.updateDeleteVisibility();
        this.filteredSuggestions = this.filterSuggestions(this.searchText);
    }

    applySuggestion(suggestion: string): void {
        this.searchText = suggestion;
        this.showSuggestions = false;
        this.updateDeleteVisibility();
    }

    removeSuggestion(event: MouseEvent, suggestion: string): void {
        event.stopPropagation();
        const index = this.searchHistory.indexOf(suggestion);
        if (index !== -1) {
            this.searchHistory.splice(index, 1);
            localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
        }
        setTimeout(() => {
            this.keepSuggestionsOpen = true;
            (document.querySelector('#searchInput') as HTMLElement)?.focus();
        });
    }

    closeSuggestions(): void {
        if (!this.keepSuggestionsOpen) {
            this.searchText && this.addToHistory(this.searchText);
            this.showSuggestions = false;
        }
    }
    
    private addToHistory(value: string): void {
        const trimmedText = value.trim();
        if (trimmedText && this.filteredSuggestions.length === 0) {
            const index = this.searchHistory.indexOf(trimmedText);
            if (index === -1) {
                this.searchHistory.push(trimmedText);
                localStorage.setItem('searchHistory', JSON.stringify(this.searchHistory));
            }
        }
    }

    private updateDeleteVisibility(): void {
        this.showSearchDeleteBtn = this.searchText?.trim().length > 0;
    }

    private filterSuggestions(value: string): string[] {
        if (this.searchHistory.length === 0) return [];
        const numberOfSuggestions = 5;
        const reversedHistory = [...this.searchHistory].reverse();
        if (value && value.trim().length > 0) {
            const lowerValue = value.toLowerCase();
            return reversedHistory.filter(suggestion => suggestion.toLowerCase().includes(lowerValue) && suggestion.toLowerCase() !== lowerValue).slice(0, numberOfSuggestions);
        }
        return reversedHistory.slice(0, numberOfSuggestions);
    }
}

@Pipe({
    name: 'searchTextFilter',
    standalone: false
})
export class SearchTextPipe implements PipeTransform {
    transform(items: Array<PaletteComp>, text: string): Array<PaletteComp> {
        let sortedItems = items;
        if (items && text)
            sortedItems = items.filter(item => {
                if (item && item.displayName) { 
                    return text.toLowerCase().split(' ').find(searchText => item.displayName.toLowerCase().indexOf(searchText) >= 0) != undefined;
                }
                return false;
            });
        sortedItems.sort((item1, item2) => {
            return (item1.displayName < item2.displayName ? -1 : (item1.displayName > item2.displayName ? 1 : 0))
        });
        return sortedItems;
    }
}

@Pipe({
    name: 'searchTextFilterDeep',
    standalone: false
})
export class SearchTextDeepPipe implements PipeTransform {
    transform(items: Array<Package>, text: string): Array<Package> {
        if (items)
            return items.filter(item => {
                if (!item.components || item.components.length == 0) return false;
                if (!text) return true;
                const compBool = item.components.filter(component => {
                    return text.toLowerCase().split(' ').find(searchText => component.displayName.toLowerCase().indexOf(searchText) >= 0) != undefined;
                }).length > 0;
                const catKeys = item.categories ? Object.keys(item.categories) : [];
                let catBool = false;
                if (catKeys.length > 0) {
                    for (const prop of catKeys) {
                        // eslint-disable-next-line @typescript-eslint/no-unsafe-assignment
                        const catProp: Array<PaletteComp> = item.categories[prop];
                        catBool = catProp.filter(component => {
                            return text.toLowerCase().split(' ').find(searchText => component.displayName.toLowerCase().indexOf(searchText) >= 0) != undefined;
                        }).length > 0;
                        if (catBool) {
                            break;
                        }
                    }
                }
                return (compBool || catBool);
            });
        return items;
    }
}
export class DragItem {
    paletteItemBeingDragged?: Element;
    contentItemBeingDragged?: Node;
    elementName?: string;
    packageName?: string;
    ghost?: PaletteComp; // should this be Ghost object or are they they same
    propertyName?: string;
    propertyValue?: { [property: string]: string };
    componentType?: string;
    topContainer?: boolean = false;
    layoutName?: string;
    attributes?: { [property: string]: string };
    model?: { [property: string]: unknown };
}

export class DraggedVariant {
    packageName?: string;
    name?: string;
    type?: string;
    text?: string;
    variant?: string;
    element?: Element;
    size?: {
        width: number,
        height: number
    }
}