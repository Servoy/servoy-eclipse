import { Directive, HostListener, Input, OnInit } from '@angular/core';
import { SelectionNode } from '../mouseselection/mouseselection.component';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Directive({
    selector: '[resizeKnob]'
})
export class ResizeKnobDirective implements OnInit {

    @Input('resizeKnob') resizeInfo: ResizeInfo;

    lastresizeStartPosition: {x: number; y: number};
    initialElementInfo: Map<string, ElementInfo>;
    currentElementInfo: Map<string, ElementInfo>;

    contentAreaMouseMove:  (event: MouseEvent) => void;;
    contentAreaMouseUp: (event: MouseEvent) => void;
    contentAreaMouseLeave: (event: MouseEvent) => void;
    contentAreaKeyDown: (event: KeyboardEvent) => void;

    topContentAreaAdjust: number;
    leftContentAreaAdjust: number;

    constructor(protected readonly editorSession: EditorSessionService, private editorContentService : EditorContentService) {
    }

    ngOnInit(): void {
        const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
        this.topContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
        this.leftContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''));
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
    }

    private setCursorStyle(style: string) {
        this.editorContentService.getGlassPane().style.cursor = style;
    }

    private resizeSelection(event: MouseEvent) {
        if(this.currentElementInfo) {
            let deltaX = event.clientX - this.lastresizeStartPosition.x;
            let deltaY = event.clientY - this.lastresizeStartPosition.y;

            for(const elementInfo of this.currentElementInfo.values()) {
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
                this.resizeInfo.node.style.top = elementInfo.y + this.topContentAreaAdjust + 'px';
                this.resizeInfo.node.style.left = elementInfo.x + this.leftContentAreaAdjust + 'px';


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
                changes[nodeId] = {
                    x: elementInfo.x,
                    y: elementInfo.y,
                    width: elementInfo.width,
                    height: elementInfo.height
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
    height:number

    constructor(public element: HTMLElement) {
        const elementRect = element.getBoundingClientRect();
        this.x = elementRect.x
        this.y = elementRect.y;
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
