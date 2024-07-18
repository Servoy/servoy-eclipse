import { Component, OnInit, Renderer2 } from '@angular/core';
import { DragItem } from '../palette/palette.component';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { URLParserService } from '../services/urlparser.service';

@Component({
    selector: 'dragselection-responsive',
    templateUrl: './dragselection-responsive.component.html'
})
export class DragselectionResponsiveComponent implements OnInit, ISupportAutoscroll { // ISupportAutoscroll
    allowedChildren: any;
    dragNode: HTMLElement;
    dragStartEvent: MouseEvent;
    initialParent: Element;
    dragging: boolean;
    highlightEl: HTMLElement;
    dropHighlight: string;
    canDrop: { dropAllowed: boolean; dropTarget?: Element; beforeChild?: Element; append?: boolean; };
    dragItem: DragItem = {};
    dragCopy: boolean;
    currentPoint: {x: number, y: number};

    constructor(protected readonly editorSession: EditorSessionService, protected readonly renderer: Renderer2, private readonly designerUtilsService: DesignerUtilsService, private editorContentService: EditorContentService, protected urlParser: URLParserService) { }

    ngOnInit(): void {
        this.editorContentService.getContentArea().addEventListener('mousedown', (event) => this.onMouseDown(event));
        this.editorContentService.getContentArea().addEventListener('mouseup', (event) => this.onMouseUp(event));
        this.editorContentService.getContentArea().addEventListener('mousemove', (event) => this.onMouseMove(event));
        this.editorContentService.getContentArea().addEventListener('keyup', (event) => this.onKeyup(event));
    }

    onKeyup(event: KeyboardEvent) {
        //if control is released during drag, the copy is deleted and the original element must be moved
        if (this.dragCopy && this.dragStartEvent && this.dragStartEvent.ctrlKey && (event.code.startsWith('Control') || event.code.startsWith('Meta'))) {
            this.editorContentService.sendMessageToIframe({
                id: 'removeDragCopy', uuid: this.dragNode.getAttribute("svy-id"),
                insertBefore: this.canDrop.beforeChild ? this.canDrop.beforeChild.getAttribute('svy-id') : null
            });
            this.dragCopy = false;
        }
    }

    onMouseDown(event: MouseEvent) {
        if (this.editorSession.getState().dragging || event.buttons !== 1) return; //prevent dnd when dragging from palette
        if (this.editorSession.getSelection() != null && this.editorSession.getSelection().length > 1) {
            // do not allow drag of multiple elements in responsive design
            return;
        }
        this.dragNode = this.designerUtilsService.getNodeBasedOnSelectionFCorLFC();
      	if (this.dragNode === null) {
			  this.dragNode = this.designerUtilsService.getNode(event);
		}
        if (!this.dragNode) return;

        // do not allow moving elements inside css position container in responsive layout
        if (this.dragNode && this.findAncestor(this.dragNode, '.svy-csspositioncontainer') !== null)
            return;
        
        if (this.urlParser.isAbsoluteFormLayout() && !this.dragNode.parentElement.closest('.svy-responsivecontainer')){
             // only use this for responsive container
            this.dragNode = null;
            return;
        }
      
        // skip dragging if it is an child element of a form reference
        if (event.button == 0 && this.dragNode) {
            this.dragStartEvent = event;
            this.initialParent = null;

            if (this.dragNode.classList.contains("formComponentChild")) {//do not grab if this is a form component element
                this.dragStartEvent = null;
            }
            this.initialParent = this.designerUtilsService.getParent(this.dragNode, this.dragNode.getAttribute("svy-layoutname") ? "layout" : "component");

            this.highlightEl = this.dragNode.cloneNode(true) as HTMLElement;
            if (this.dragNode.clientWidth == 0 && this.dragNode.clientHeight == 0) {
                if (this.dragNode.firstElementChild) {
                    this.highlightEl = this.dragNode.firstElementChild.cloneNode(true) as HTMLElement;
                }
                else if (!this.dragNode.getAttribute("svy-layoutname")) {
                    this.highlightEl = this.dragNode.parentElement.cloneNode(true) as HTMLElement; //component
                }
            }

            this.renderer.addClass(this.highlightEl, 'highlight_element');
            this.renderer.removeAttribute(this.highlightEl, 'svy-id');

            this.dragItem.topContainer = this.designerUtilsService.isTopContainer(this.dragNode.getAttribute("svy-layoutname"));
            this.dragItem.layoutName = this.dragNode.getAttribute("svy-layoutname");
            this.dragItem.componentType = this.dragItem.layoutName ? "layout" : "component";
        }

        this.currentPoint = this.adjustPoint(event.pageX, event.pageY);
        this.editorSession.registerAutoscroll(this);
    }

    onMouseMove(event: MouseEvent) {
        if (!this.dragStartEvent || event.buttons !== 1 || !this.dragNode) return;
        if (!this.editorSession.getState().dragging) {
            if (Math.abs(this.dragStartEvent.clientX - event.clientX) > 5 || Math.abs(this.dragStartEvent.clientY - event.clientY) > 5) {
                this.editorSession.setDragging( true );
                this.dragCopy = event.ctrlKey || event.metaKey;
                this.editorContentService.sendMessageToIframe({ id: 'createDraggedComponent', uuid: this.dragNode.getAttribute("svy-id"), dragCopy: this.dragCopy });
                if (this.dropHighlight !== this.dragItem.layoutName) {
                    const elements = this.dragNode.querySelectorAll('[svy-id]');
                    const dropHighlightIgnoredIds = Array.from(elements).map((element) => {
                        return element.getAttribute('svy-id');
                    });
                    this.editorContentService.executeOnlyAfterInit(() => {
                        this.editorContentService.sendMessageToIframe({ id: 'dropHighlight', value: { dropHighlight: this.dragItem.layoutName, dropHighlightIgnoredIds: dropHighlightIgnoredIds } });
                    });
                    this.dropHighlight = this.dragItem.layoutName;
                }
                this.editorSession.getState().drop_highlight = this.dragItem.componentType;
            } else return;
        }

        if (!this.dragItem.contentItemBeingDragged) {
            this.dragItem.contentItemBeingDragged = this.editorContentService.getContentElementById('svy_draggedelement');
        }
        if (this.dragItem.contentItemBeingDragged) {
            const point = this.adjustPoint(event.pageX + 1, event.pageY + 1);
            this.currentPoint = point;
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'top', point.y + 'px');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'left', point.x + 'px');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'position', 'absolute');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'display', 'block');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'z-index', 4);
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'transition', 'opacity .5s ease-in-out 0');
        }

        this.canDrop = this.getDropNode(this.dragItem.componentType, this.dragItem.topContainer, this.dragNode.getAttribute("svy-layoutname"), event, this.dragNode.getAttribute("svy-id"));
        if (!this.canDrop.dropAllowed) {
            this.editorContentService.getGlassPane().style.cursor = "not-allowed";
        } else this.editorContentService.getGlassPane().style.cursor = "pointer";

        this.dragStartEvent = event;

        if (this.editorContentService.getGlassPane().style.cursor === "pointer") {
            if (this.canDrop.beforeChild && this.canDrop.beforeChild.getAttribute("svy-id") === this.dragNode.getAttribute("svy-id")) {
               // we should check for siblings on mouseUp
               //this.canDrop.beforeChild = this.designerUtilsService.getNextElementSibling(this.canDrop);
               return; //preview position not changed, return here
            }
            if (this.canDrop.dropAllowed && this.canDrop.dropTarget === this.dragNode.parentElement?.closest("[svy-id]") && this.canDrop.beforeChild === this.designerUtilsService.getNextElementSibling(this.dragNode)) {
                return; //preview position not changed, return here
            }
            if (this.canDrop.dropAllowed) {
                this.renderer.setStyle(this.dragNode, 'opacity', '1');
                this.editorContentService.sendMessageToIframe({
                    id: 'insertDraggedComponent',
                    dropTarget: this.canDrop.dropTarget ? this.canDrop.dropTarget.getAttribute('svy-id') : null,
                    insertBefore: this.canDrop.beforeChild ? this.canDrop.beforeChild.getAttribute('svy-id') : null
                });
            }
        }
        else if (this.dragItem.contentItemBeingDragged) {
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'opacity', '1');
        }
    }

    onMouseUp(event: MouseEvent) {
        if (this.dragStartEvent !== null && this.dragNode && this.editorSession.getState().dragging && this.canDrop.dropAllowed) {
            const obj = (event.ctrlKey || event.metaKey) ? [] : {};

            if (!this.canDrop.beforeChild && !this.canDrop.append) {
                this.canDrop.beforeChild = this.designerUtilsService.getNextElementSibling(this.dragNode);
            }

            if (this.canDrop.beforeChild && this.canDrop.beforeChild.getAttribute("svy-id") === this.dragNode.getAttribute("svy-id")) {
                this.canDrop.beforeChild = this.designerUtilsService.getNextElementSibling(this.canDrop.beforeChild);
            }

            const key = (event.ctrlKey || event.metaKey) && this.dragCopy ? 0 : this.dragNode.getAttribute("svy-id");
            obj[key] = {};
            if ((event.ctrlKey || event.metaKey) && this.dragCopy) {
                obj[key].uuid = this.dragNode.getAttribute('svy-id');
            }

            if (this.canDrop.dropTarget) {
                obj[key].dropTargetUUID = this.canDrop.dropTarget.getAttribute("svy-id");
            }

            if (this.canDrop.beforeChild) {
                obj[key].rightSibling = this.canDrop.beforeChild.getAttribute("svy-id");
            }
            if (event.ctrlKey || event.metaKey) {
                this.editorSession.createComponents({
                    "components": obj
                });
            } else {
                this.editorSession.getSession().callService('formeditor', 'moveComponent', obj, true);
            }
        }

        this.dragStartEvent = null;
        this.editorSession.setDragging( false );
        this.editorContentService.getGlassPane().style.cursor = "default";
        this.dragNode = null;
        this.dropHighlight = null;
        if (this.dragItem && this.dragItem.contentItemBeingDragged) {
            this.editorContentService.sendMessageToIframe({ id: 'destroyElement', existingElement: !this.dragCopy });
        }
        //force redrawing of the selection decorator to the new position
        //this.editorSession.updateSelection(this.editorSession.getSelection());
        this.editorContentService.executeOnlyAfterInit(() => {
            this.editorContentService.sendMessageToIframe({ id: 'dropHighlight', value: null });
        });
        this.dragItem = {};
        this.dragCopy = false;
        this.editorSession.unregisterAutoscroll(this);
    }

    private findAncestor(el: HTMLElement, cls: string): HTMLElement {
        while ((el = el.parentElement) && !el.classList.contains(cls));
        return el !== undefined && el !== null && el.classList.contains(cls) ? el : null;
    }

    private adjustPoint(x: number, y: number): { x: number, y: number } {
        const style = window.getComputedStyle(this.editorContentService.getGlassPane().parentElement);
        const rectangle = this.editorContentService.getGlassPane().getBoundingClientRect();
        return { x: x - parseInt(style.paddingLeft) - rectangle.left, y: y - parseInt(style.paddingLeft) - rectangle.top };
    }

    private getDropNode(type: string, topContainer: boolean, layoutName: string, event: MouseEvent, svyId: string) {
        const canDrop = this.designerUtilsService.getDropNode(false, type, topContainer, layoutName, event, undefined, svyId);
        canDrop.dropAllowed = canDrop.dropAllowed && this.dragNode.classList.contains("inheritedElement")
            && this.initialParent !== null && this.initialParent.getAttribute("svy-id") !== canDrop.dropTarget.getAttribute("svy-id") ? false : canDrop.dropAllowed;
        return canDrop;
    }

    getAutoscrollLockId(): string {
        return 'drag-selection-responsive';
    }
    
    updateLocationCallback(changeX: number, changeY: number) {
        if (this.dragItem.contentItemBeingDragged) {
            this.currentPoint.x += changeX;
            this.currentPoint.y += changeY; 
            const point = this.adjustPoint(this.currentPoint.x, this.currentPoint.y);
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'top', point.y + 'px');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'left', point.x + 'px');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'position', 'absolute');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'display', 'block');
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'z-index', 4);
            this.renderer.setStyle(this.dragItem.contentItemBeingDragged, 'transition', 'opacity .5s ease-in-out 0');

            this.editorContentService.getContentArea().scrollTop += changeY;
            this.editorContentService.getContentArea().scrollLeft += changeX;
        }
    }
}