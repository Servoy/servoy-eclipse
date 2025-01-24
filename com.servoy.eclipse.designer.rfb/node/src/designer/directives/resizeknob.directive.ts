import { AfterViewInit, Directive, HostListener, Input, OnDestroy, OnInit } from '@angular/core';
import { SelectionNode } from '../mouseselection/mouseselection.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';
import { DynamicGuidesService, SnapData } from '../services/dynamicguides.service';
import { Subscription } from 'rxjs';

@Directive({
    selector: '[resizeKnob]',
    standalone: false
})
export class ResizeKnobDirective implements OnInit, AfterViewInit, OnDestroy {

    @Input('resizeKnob') resizeInfo: ResizeInfo;

    lastresizeStartPosition: {x: number; y: number};
    initialElementInfo: Map<string, ElementInfo>;
    currentElementInfo: Map<string, ElementInfo>;

    contentAreaMouseMove:  (event: MouseEvent) => void;
    contentAreaMouseUp: (event: MouseEvent) => void;
    contentAreaMouseLeave: (event: MouseEvent) => void;
    contentAreaKeyDown: (event: KeyboardEvent) => void;

    topContentAreaAdjust: number;
    leftContentAreaAdjust: number;
    snapData: SnapData;
    subscription: Subscription;

    constructor(protected readonly editorSession: EditorSessionService, private editorContentService : EditorContentService, private guidesService: DynamicGuidesService) {
    }

    ngOnInit(): void {
        const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
        this.topContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
        this.leftContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''));

        //set initial glasspane size
        this.setGlasspaneSize();
    }

    private setGlasspaneSize(): void {
        // Get all component elements
        const componentElements = this.editorContentService.getAllContentElements();
        let maxWidth = 0;
        let maxHeight = 0;
    
        // Calculate maximum dimensions based on components
        componentElements.forEach(element => {
            const componentRect = element.getBoundingClientRect();
            maxWidth = Math.max(maxWidth, componentRect.left + componentRect.width);
            maxHeight = Math.max(maxHeight, componentRect.top + componentRect.height);
        });
    
        // Get visible dimensions of the designer
        const contentArea = this.editorContentService.getContentArea();
        const visibleWidth = contentArea.clientWidth;
        const visibleHeight = contentArea.clientHeight;
    
        // Get dimensions from the content form
        const contentForm = this.editorContentService.getContentForm();
        const formRect = contentForm.getBoundingClientRect();
        const formWidth = formRect.width;
        const formHeight = formRect.height;
    
        // Determine the greater dimensions
        const finalWidth = Math.max(maxWidth, visibleWidth, formWidth) + 100;
        const finalHeight = Math.max(maxHeight, visibleHeight, formHeight) + 100;
    
        // Set the glasspane size to cover all components, visible area, and content form
        const glasspane = this.editorContentService.getGlassPane();
        if (glasspane) {
            glasspane.style.width = `${finalWidth}px`;
            glasspane.style.height = `${finalHeight}px`;
        }
    }

    ngAfterViewInit(): void {
        this.subscription = this.guidesService.snapDataListener.subscribe((value: SnapData) => {
            this.snap(value);
        })
    }

    snap( data: SnapData): void {
        if (this.currentElementInfo && this.editorSession.getState().resizing) {
            this.snapData = data;
            if (this.initialElementInfo.size == 1 && (this.snapData?.width || this.snapData?.height)) {
                const elementInfo = this.initialElementInfo.values().next().value;
                elementInfo.element.style.position = 'absolute';
                
                if (this.snapData.width) {
                    elementInfo.x = this.snapData.left;
                    elementInfo.element.style.left = this.snapData.left + 'px';
                    this.resizeInfo.node.style.left = this.snapData.left + this.leftContentAreaAdjust + 'px';
                    this.resizeInfo.node.style.width = elementInfo.element.style.width = this.snapData.width + 'px';
                }
                if (this.snapData.height) {
                    elementInfo.y = this.snapData.top;
                    elementInfo.element.style.top = this.snapData.top + 'px';
                    this.resizeInfo.node.style.top = this.snapData.top + this.topContentAreaAdjust + 'px';
                    this.resizeInfo.node.style.height = elementInfo.element.style.height = this.snapData.height + 'px';
                }
            }
        }
    }

    ngOnDestroy(): void {
        this.subscription.unsubscribe();
    }

    @HostListener('mousedown', ['$event'])
    onMouseDown(event: MouseEvent): void {
        if(event.button == 0)
        {
            this.setCursorStyle(this.resizeInfo.direction +'-resize');
            event.preventDefault();
            event.stopPropagation();
            this.lastresizeStartPosition = {
                x: event.clientX,
                y: event.clientY
            };

            this.cleanResizeState();
            this.editorSession.getState().resizing = true;

            const selection = this.editorSession.getSelection();
            if (selection && selection.length > 0) {
                for (let i = 0; i < selection.length; i++) {
                    const nodeid = selection[i];
                    let element = this.editorContentService.getContentElement(nodeid);
                    while(element && !element.classList.contains('svy-wrapper')) {
                        element = element.parentElement;
                    }

                    if (element) {
                        if(!this.initialElementInfo) {
                            this.initialElementInfo = new Map<string, ElementInfo>();
                            this.currentElementInfo = new Map<string, ElementInfo>();
                        }
                        this.initialElementInfo.set(nodeid, new ElementInfo(element));
                        this.currentElementInfo.set(nodeid, new ElementInfo(element));
                    }
                }
            }

            this.editorContentService.getContentArea().addEventListener('mousemove', this.contentAreaMouseMove = (event: MouseEvent) => {
                this.resizeSelection(event);
            });
            this.editorContentService.getContentArea().addEventListener('mouseup', this.contentAreaMouseUp = (event: MouseEvent) => {
                this.resizeSelection(event);
                this.sendChanges(this.currentElementInfo);
                this.setCursorStyle('');
                this.cleanResizeState();
            });
            this.editorContentService.getContentArea().addEventListener('mouseleave', this.contentAreaMouseLeave = (event: MouseEvent) => {
                this.contentAreaMouseUp(event);
            });
            this.editorContentService.getContentArea().addEventListener('keydown', this.contentAreaKeyDown = (event: KeyboardEvent) => {
                if (event.keyCode == 27)
                {
                    for(const elementInfo of this.initialElementInfo.values()) {
                        elementInfo.element.style.top = elementInfo.y + 'px';
                        elementInfo.element.style.left =  elementInfo.x + 'px';
                        this.resizeInfo.node.style.top = elementInfo.y + this.topContentAreaAdjust + 'px';
                        this.resizeInfo.node.style.left = elementInfo.x + this.leftContentAreaAdjust + 'px';
        
        
                        this.resizeInfo.node.style.width = elementInfo.element.style.width = elementInfo.width + 'px';
                        this.resizeInfo.node.style.height = elementInfo.element.style.height = elementInfo.height + 'px';
                    }                    
                    this.sendChanges(this.initialElementInfo);
                    this.setCursorStyle('');
                    this.cleanResizeState();
                }
            });
        }        
    }

    @HostListener('mouseup', ['$event'])
    onMouseUp(event: MouseEvent): void {
        this.sendChanges(this.currentElementInfo);
        this.setCursorStyle('');
        this.cleanResizeState();
    }

    private cleanResizeState() {
        this.initialElementInfo = null;
        this.currentElementInfo = null;
        this.editorContentService.getContentArea().removeEventListener('mousemove', this.contentAreaMouseMove);
        this.editorContentService.getContentArea().removeEventListener('mouseup', this.contentAreaMouseUp);
        this.editorContentService.getContentArea().removeEventListener('mouseleave', this.contentAreaMouseLeave);
        this.editorContentService.getContentArea().removeEventListener('keydown', this.contentAreaKeyDown);
        this.editorSession.getState().resizing = false;
    }

    private setCursorStyle(style: string) {
        this.editorContentService.getGlassPane().style.cursor = style;
    }

    private resizeSelection(event: MouseEvent) {
        if(this.currentElementInfo && !this.snapData?.width && !this.snapData?.height) {
            let deltaX = event.clientX - this.lastresizeStartPosition.x;
            let deltaY = event.clientY - this.lastresizeStartPosition.y;

            for(const elementInfo of this.currentElementInfo.values()) {
				let parentX = 0;
				let parentY = 0;
				const parentFC = elementInfo.element.closest('.svy-formcomponent');
				if (parentFC && !elementInfo.element.classList.contains('svy-formcomponent')) {
					const parentRect = parentFC.getBoundingClientRect();
					parentX = parentRect.x;
					parentY = parentRect.y;
				}
                elementInfo.y = elementInfo.y + deltaY* this.resizeInfo.top;
                if(elementInfo.y < 0) {
                    elementInfo.y = 0;
                    deltaY = 0;
                }
                elementInfo.x = elementInfo.x + deltaX* this.resizeInfo.left;
                if(elementInfo.x < 0) {
                    elementInfo.x = 0;
                    deltaX = 0;
                }
                elementInfo.width = elementInfo.width + deltaX* this.resizeInfo.width;
                if(elementInfo.width < 1) elementInfo.width = 1;
                elementInfo.height = elementInfo.height + deltaY* this.resizeInfo.height;
                if(elementInfo.height < 1) elementInfo.height = 1;

                elementInfo.element.style.top = elementInfo.y + 'px';
                elementInfo.element.style.left =  elementInfo.x + 'px';
                this.resizeInfo.node.style.top = elementInfo.y + parentY + this.topContentAreaAdjust + 'px';
                this.resizeInfo.node.style.left = elementInfo.x + parentX + this.leftContentAreaAdjust + 'px';


                this.resizeInfo.node.style.width = elementInfo.element.style.width = elementInfo.width + 'px';
                this.resizeInfo.node.style.height = elementInfo.element.style.height = elementInfo.height + 'px';
            }

            this.lastresizeStartPosition = {
                x: event.clientX,
                y: event.clientY
            };
        }
    }

    private sendChanges(elementInfos: Map<string, ElementInfo>) {
        if(elementInfos) {
            const changes = {};
            for(const nodeId of elementInfos.keys()) {
                const elementInfo = elementInfos.get(nodeId);
                if (this.snapData && elementInfos.size == 1) {
                    changes[nodeId] = {
                        x: this.snapData?.width ? Math.round(this.snapData.left) : elementInfo.x,
                        y: this.snapData?.height ? Math.round(this.snapData.top) : elementInfo.y,
                        width: this.snapData?.width ? Math.round(this.snapData.width) : elementInfo.width,
                        height: this.snapData?.height ? Math.round(this.snapData.height) : elementInfo.height,
                        cssPos: this.snapData.cssPosition
                    }
                }
                else {
                    changes[nodeId] = {
                        x: elementInfo.x,
                        y: elementInfo.y,
                        width: elementInfo.width,
                        height: elementInfo.height,
                        move: false
                    }
                }
            }
            this.editorSession.sendChanges(changes);
        }
    }
}

export class ElementInfo {

    x:number;
    y:number;
    width:number;
    height:number;

    constructor(public element: HTMLElement) {
        const elementRect = element.getBoundingClientRect();      
        this.x = element.offsetLeft;
        this.y = element.offsetTop;
        this.width = elementRect.width
        this.height = elementRect.height;
    }
}

class ResizeInfo {
    node: SelectionNode;
    position: string;
    direction: string;
    top: number;
    left: number;
    width: number;
    height: number;
}
