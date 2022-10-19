import { Component, Pipe, PipeTransform, Renderer2 } from '@angular/core';
import { EditorSessionService, Package, PaletteComp, ISupportAutoscroll } from '../services/editorsession.service';
import { HttpClient } from '@angular/common/http';
import { URLParserService } from '../services/urlparser.service';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-palette',
    templateUrl: './palette.component.html',
    styleUrls: ['./palette.component.css']
})
export class PaletteComponent implements ISupportAutoscroll{

    public searchText: string;
    public activeIds: Array<string>;

    dragItem: DragItem = {};
    canDrop: { dropAllowed: boolean, dropTarget?: Element, beforeChild?: Element, append?: boolean };
    
    autoscrollAreasEnabled: boolean;
    autoscrollElementClientBounds: Array<DOMRect>;
    
    constructor(protected readonly editorSession: EditorSessionService, private http: HttpClient, private urlParser: URLParserService,
        protected readonly renderer: Renderer2, protected designerUtilsService: DesignerUtilsService, private editorContentService: EditorContentService) {
        let layoutType: string;
        if (urlParser.isAbsoluteFormLayout())
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
                            else {
                                packages[i].components[j].components = propertyValues;
                            }
                        }
                    }
                }
            }
            this.editorSession.getState().packages = packages;
        });
        this.editorContentService.getBodyElement().addEventListener('mouseup', this.onMouseUp);
        this.editorContentService.getBodyElement().addEventListener('mousemove', this.onMouseMove);
    }

    openPackageManager() {
        void this.editorSession.openPackageManager();
    }

    onClick(component: PaletteComp) {
        component.isOpen = !component.isOpen;
    }

    onMouseDown(event: MouseEvent, elementName: string, packageName: string, model: { [property: string]: any }, ghost: PaletteComp, propertyName?: string, propertyValue?: {[property: string]: string }, componentType?: string, topContainer?: boolean, layoutName?: string, attributes?: { [property: string]: string }, children?: [{ [property: string]: string }]) {
        event.stopPropagation();

        this.dragItem.paletteItemBeingDragged = (event.target as HTMLElement).cloneNode(true) as Element;
        Array.from(this.dragItem.paletteItemBeingDragged.children).forEach(child => {
            if (child.tagName == 'UL') {
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

        this.canDrop = { dropAllowed: false };
        if (!ghost) {
            this.editorSession.setDragging(true);
            this.editorContentService.sendMessageToIframe({ id: 'createElement', name: this.convertToJSName(elementName), model: model, type: componentType, attributes: attributes, children: children });
            this.autoscrollElementClientBounds = this.designerUtilsService.getAutoscrollElementClientBounds();
        }
    }

    onMouseUp = (event: MouseEvent) => {
        if (this.dragItem.paletteItemBeingDragged) {
            this.editorSession.setDragging( false );
            this.editorContentService.getBodyElement().removeChild(this.dragItem.paletteItemBeingDragged);
            this.dragItem.paletteItemBeingDragged = null;
            this.dragItem.contentItemBeingDragged = null;
            this.editorContentService.getGlassPane().style.cursor = '';

            const component = {} as PaletteComp;
            component.name = this.dragItem.elementName;
            component.packageName = this.dragItem.packageName;

            component.x = event.pageX;
            component.y = event.pageY;

            // do we also need to set size here ?
            component.x = component.x - this.editorContentService.getLeftPositionIframe();
            component.y = component.y - this.editorContentService.getTopPositionIframe();

            if (this.urlParser.isAbsoluteFormLayout()) {
                if (this.canDrop.dropAllowed && this.canDrop.dropTarget) {
                    component.dropTargetUUID = this.canDrop.dropTarget.getAttribute('svy-id');
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
                }
                else if (!this.dragItem.ghost){
                    this.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
                    return;
                }
            }

            if (this.dragItem.ghost) {
                const elements = this.editorContentService.getAllContentElements();
                const found = Array.from(elements).find((node) => {
                    const position = node.getBoundingClientRect();
                    this.designerUtilsService.adjustElementRect(node, position);
                    if (position.x <= component.x && position.x + position.width >= component.x && position.y <= component.y && position.y + position.height >= component.y) {
                        const types = node.getAttribute('svy-types');
                        if (types && types.split(',').indexOf(this.dragItem.ghost.type) >= 0) {
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
            }

            this.editorContentService.sendMessageToIframe({ id: 'destroyElement' });
             
             //disable mouse events on the autoscroll
            this.editorSession.getState().pointerEvents = 'none'; 
            this.autoscrollAreasEnabled = false;
            this.editorSession.stopAutoscroll();
        }
    }

    onMouseMove = (event: MouseEvent) => {
        if (event.pageX >= this.editorContentService.getLeftPositionIframe() && event.pageY >= this.editorContentService.getTopPositionIframe() && this.dragItem.paletteItemBeingDragged && this.dragItem.contentItemBeingDragged) {
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'opacity', '0');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
        }

        if (this.dragItem.paletteItemBeingDragged) {
           
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'left', event.pageX + 'px');
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'top', event.pageY + 'px');
            if (this.dragItem.contentItemBeingDragged) {
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'left', event.pageX - this.editorContentService.getLeftPositionIframe() + 'px');
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'top', event.pageY - this.editorContentService.getTopPositionIframe() + 'px');

                this.canDrop = this.designerUtilsService.getDropNode(this.urlParser.isAbsoluteFormLayout(), this.dragItem.componentType, this.dragItem.topContainer, this.dragItem.layoutName ? this.dragItem.packageName + "." + this.dragItem.layoutName : this.dragItem.layoutName, event, this.dragItem.elementName);
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
                            dropTarget: this.canDrop.dropTarget ? this.canDrop.dropTarget.getAttribute('svy-id') : null,
                            insertBefore: this.canDrop.beforeChild ? this.canDrop.beforeChild.getAttribute('svy-id') : null
                        });
                    }
                    if (this.canDrop.dropAllowed && (this.canDrop.dropTarget || !this.urlParser.isAbsoluteFormLayout() ))
                    {
                        // hide the dragged item and rely on inserted item at specific parent 
                        this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '0');
                    } else 
                    {
                        this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
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
                        const position = node.getBoundingClientRect();
                        this.designerUtilsService.adjustElementRect(node, position);
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
             // enable auto-scroll areas only if current mouse event is outside of them (this way, when starting to drag from an auto-scroll area it will not immediately auto-scroll)
            if (this.autoscrollElementClientBounds && !this.autoscrollAreasEnabled && !this.designerUtilsService.isInsideAutoscrollElementClientBounds(this.autoscrollElementClientBounds, event.clientX, event.clientY)) {
                this.autoscrollAreasEnabled = true;
                this.editorSession.startAutoscroll(this);
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
    
    getUpdateLocationCallback(): (changeX: number, changeY: number, minX?: number, minY?: number) => void {
        return null;
    }
}

@Pipe({ name: 'searchTextFilter' })
export class SearchTextPipe implements PipeTransform {
    transform(items: Array<PaletteComp>, text: string): Array<PaletteComp> {
        let sortedItems = items;
        if (items && text)
            sortedItems = items.filter(item => {
                return item && item.displayName && item.displayName.toLowerCase().indexOf(text.toLowerCase()) >= 0;
            });
        sortedItems.sort((item1, item2) => {
            return (item1.displayName < item2.displayName ? -1 : (item1.displayName > item2.displayName ? 1 : 0))
        });
        return sortedItems;
    }
}

@Pipe({ name: 'searchTextFilterDeep' })
export class SearchTextDeepPipe implements PipeTransform {
    transform(items: Array<Package>, text: string): Array<Package> {
        if (items)
            return items.filter(item => {
                if (!item.components || item.components.length == 0) return false;
                if (!text) return true;
                return item.components.filter(component => {
                    return component.displayName.toLowerCase().indexOf(text.toLowerCase()) >= 0;
                }).length > 0;
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
}
