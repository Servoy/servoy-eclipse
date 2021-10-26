import { Component, Pipe, PipeTransform, Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService } from '../services/editorsession.service';
import { HttpClient } from '@angular/common/http';
import { URLParserService } from '../services/urlparser.service';
import {DesignerUtilsService} from '../services/designerutils.service';

@Component({
    selector: 'designer-palette',
    templateUrl: './palette.component.html',
    styleUrls: ['./palette.component.css']
})
export class PaletteComponent {

    public searchText: string;
    public packages: Array<Package>;;
    public activeIds: Array<string>;

    dragItem: DragItem = {};
    topAdjust: number;
    leftAdjust: number;
    glasspane: HTMLElement;

    constructor(protected readonly editorSession: EditorSessionService, private http: HttpClient, private urlParser: URLParserService, @Inject(DOCUMENT) private doc: Document, 
        protected readonly renderer: Renderer2, protected designerUtilsService : DesignerUtilsService) {
        let layoutType: string;
        if (urlParser.isAbsoluteFormLayout())
            layoutType = 'Absolute-Layout';
        else
            layoutType = 'Responsive-Layout';
        this.activeIds = [];
        this.http.get('/designer/palette?layout=' + layoutType + '&formName=' + this.urlParser.getFormName()).subscribe((got: Array<Package>) => {
            let propertyValues: Array<PaletteComp>;
            if (got[got.length - 1] && got[got.length - 1].propertyValues) {
                propertyValues = got[got.length - 1].propertyValues;
                this.packages = got.slice(0, got.length - 1);
            }
            else {
                this.packages = got;
            }
            for (let i = 0; i < this.packages.length; i++) {
                this.packages[i].id = ('svy_' + this.packages[i].packageName).replace(/[|&;$%@"<>()+,]/g, '').replace(/\s+/g, '_');
                this.activeIds.push(this.packages[i].id);
                if (this.packages[i].components) {
                    for (let j = 0; j < this.packages[i].components.length; j++) {
                        if (propertyValues && this.packages[i].components[j].properties) {
                            this.packages[i].components[j].isOpen = false;
                            //we still need to have the components with properties on the component for filtering

                            if (propertyValues && propertyValues.length && this.packages[i].components[j].name == 'servoycore-formcomponent') {
                                const newPropertyValues : Array<PaletteComp> = [];
                                for (let n = 0; n < propertyValues.length; n++) {
                                    if (!propertyValues[n].isAbsoluteCSSPositionMix) {
                                        newPropertyValues.push(propertyValues[n]);
                                    }
                                }
                                this.packages[i].components[j].components = newPropertyValues;
                            }
                            else {
                                this.packages[i].components[j].components = propertyValues;
                            }
                        }
                    }
                }
            }
        });
        this.doc.body.addEventListener('mouseup', this.onMouseUp);
        this.doc.body.addEventListener('mousemove', this.onMouseMove);
    }

    openPackageManager() {
        void this.editorSession.openPackageManager();
    }

    onClick(component: PaletteComp) {
        component.isOpen = !component.isOpen;
    }

    onMouseDown(event: MouseEvent, elementName: string, packageName: string, model: {property : any}, ghost: PaletteComp, propertyName? :string, propertyValue? : {property : string}) {
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
        this.doc.body.appendChild(this.dragItem.paletteItemBeingDragged);

        this.dragItem.elementName = elementName;
        this.dragItem.packageName = packageName;
        this.dragItem.ghost = ghost;
        this.dragItem.propertyName = propertyName;
        this.dragItem.propertyValue = propertyValue;
  
        this.glasspane = this.doc.querySelector('.contentframe-overlay');
        const frameElem = this.doc.querySelector('iframe');
        const frameRect = frameElem.getBoundingClientRect();

        this.topAdjust = frameRect.top;
        this.leftAdjust = frameRect.left;
        if (!ghost) {
            frameElem.contentWindow.postMessage({ id: 'createElement', name: this.convertToJSName(elementName), model: model }, '*');
        }
    }

    onMouseUp = (event: MouseEvent) => {
        if (this.dragItem.paletteItemBeingDragged) {
            this.doc.body.removeChild(this.dragItem.paletteItemBeingDragged);
            this.dragItem.paletteItemBeingDragged = null;
            this.dragItem.contentItemBeingDragged = null;
            this.glasspane.style.cursor = '';
            const frameElem = this.doc.querySelector('iframe');

            const component = {} as PaletteComp;
            component.name = this.dragItem.elementName;
            component.packageName = this.dragItem.packageName;
            component.x = event.pageX;
            component.y = event.pageY;

            // do we also need to set size here ?
            component.x = component.x - this.leftAdjust;
            component.y = component.y - this.topAdjust;
            if (this.dragItem.ghost) {
                const elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
                const found = Array.from(elements).find((node) => {
                    const position = node.getBoundingClientRect();
                    if (position.x <= component.x && position.x + position.width >= component.x && position.y <= component.y && position.y + position.height >= component.y) {
                        if (node.getAttribute('svy-types').split(',').indexOf(this.dragItem.ghost.type) >= 0) {
                            return node;
                        }
                    }
                });
                if (!found) return;
                component.type = this.dragItem.ghost.type;
                component.ghostPropertyName = this.dragItem.ghost.propertyName;
                component.dropTargetUUID = found.getAttribute('svy-id');
            }
            if (this.dragItem.propertyName){
                component[this.dragItem.propertyName] = this.dragItem.propertyValue;
            }
            
            if (component.x >= 0 && component.y >= 0) {
                this.editorSession.createComponent(component);
            }

            frameElem.contentWindow.postMessage({ id: 'destroyElement' }, '*');
        }
    }

    onMouseMove = (event: MouseEvent) => {
        if (event.pageX >= this.leftAdjust && event.pageY >= this.topAdjust && this.dragItem.paletteItemBeingDragged && this.dragItem.contentItemBeingDragged) {
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'opacity', '0');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
        }

        if (this.dragItem.paletteItemBeingDragged) {
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'left', event.pageX + 'px');
            this.renderer.setStyle(this.dragItem.paletteItemBeingDragged, 'top', event.pageY + 'px');
            if (this.dragItem.contentItemBeingDragged) {
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'left', event.pageX - this.leftAdjust + 'px');
                this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'top', event.pageY - this.topAdjust + 'px');
            }
            else {
                const frameElem = this.doc.querySelector('iframe');
                if (!this.dragItem.ghost) {
                    // if is a type, do try to create the preview
                    this.dragItem.contentItemBeingDragged = frameElem.contentWindow.document.getElementById('svy_draggedelement');
                    if (this.dragItem.contentItemBeingDragged) {
                        this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '0');
                    }
                }
                else {
                    const elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
                    const x = event.pageX - this.leftAdjust;
                    const y = event.pageY - this.topAdjust;
                    const found = Array.from(elements).find((node) => {
                        const position = node.getBoundingClientRect();
                        this.designerUtilsService.adjustElementRect(node, position);
                        if (position.x <= x && position.x + position.width >= x && position.y <= y && position.y + position.height >= y) {
                            if (node.getAttribute('svy-types').split(',').indexOf(this.dragItem.ghost.type) >= 0) {
                                return node;
                            }
                        }
                    });
                    if (!found) {
                        this.glasspane.style.cursor = 'not-allowed';
                    }
                    else {
                        this.glasspane.style.cursor = 'pointer';
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
}

@Pipe({ name: 'searchTextFilter' })
export class SearchTextPipe implements PipeTransform {
    transform(items: Array<PaletteComp>, text: string):  Array<PaletteComp> {
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

class DragItem {
    paletteItemBeingDragged?: Element;
    contentItemBeingDragged?: Node;
    elementName?: string;
    packageName?: string;
    ghost?: PaletteComp; // should this be Ghost object or are they they same
    propertyName? :string;
    propertyValue? : {property : string};
}

export class PaletteComp {
    name: string;
    displayName : string;
    packageName: string;
    x: number;
    y: number;
    type: string;
    ghostPropertyName: string;
    dropTargetUUID: string; 
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
}

class Package {
    id: string;
    packageName: string;
    packageDisplayname : string;
    components: Array<PaletteComp>;
    propertyValues?: Array<PaletteComp>;
}
