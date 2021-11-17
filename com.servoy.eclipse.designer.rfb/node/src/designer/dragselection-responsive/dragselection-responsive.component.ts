import { DOCUMENT } from '@angular/common';
import { Component, Inject, OnInit, Renderer2 } from '@angular/core';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';

@Component({
  selector: 'dragselection-responsive',
  templateUrl: './dragselection-responsive.component.html'
})
export class DragselectionResponsiveComponent implements OnInit, ISupportAutoscroll {
  dragCloneDiv: HTMLElement;
  allowedChildren: any;
  dragNode: HTMLElement;
  urlParser: any;
  dragStartEvent: MouseEvent;
  initialParent: any;
  dragging: boolean;
  glasspane: HTMLElement;
  highlightEl: HTMLElement;
  dropHighlight: string;
  type: string;
  topContainer: boolean;
  autoscrollElementClientBounds: Array<DOMRect>;
  autoscrollAreasEnabled: any;
  
  constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2, private readonly designerUtilsService: DesignerUtilsService) { }

  ngOnInit(): void {
    this.glasspane = this.doc.querySelector('.contentframe-overlay') as HTMLElement;
    const content = this.doc.querySelector('.content-area') as HTMLElement;
    content.addEventListener('mousedown', (event) => this.onMouseDown(event));
    content.addEventListener('mouseup', (event) => this.onMouseUp(event));
    content.addEventListener('mousemove', (event) => this.onMouseMove(event));
}
  onMouseDown(event: MouseEvent) {
    if (this.editorSession.getState().dragging) return; //prevent dnd when dragging from palette
    if (this.editorSession.getSelection() != null && this.editorSession.getSelection().length > 1)
    {
        // do not allow drag of multiple elements in responsive design
        return;
    }
    this.dragNode = this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' + this.editorSession.getSelection()[0] + '"]')[0] as HTMLElement;
    if (!this.dragNode) return; 

    // do not allow moving elements inside css position container in responsive layout
    if (this.dragNode && this.findAncestor(this.dragNode, '.svy-csspositioncontainer') !== null)
        return;

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
        else if (!this.dragNode.getAttribute("svy-layoutname")){
          this.highlightEl = this.dragNode.parentElement.cloneNode(true) as HTMLElement; //component
        }
      }

      this.renderer.addClass(this.highlightEl, 'highlight_element');
      this.renderer.removeAttribute(this.highlightEl, 'svy-id');

      this.dragCloneDiv = this.renderer.createElement('div');
      this.renderer.appendChild(this.doc.querySelector('iframe').contentWindow.document.body, this.dragCloneDiv);
      this.renderer.appendChild(this.dragCloneDiv, this.highlightEl);
      this.renderer.setStyle(this.dragCloneDiv, 'display', 'none');
      const point = this.adjustPoint(event.pageX, event.pageY);
      this.renderer.setStyle(this.dragCloneDiv, 'position', 'absolute');
      this.renderer.setStyle(this.dragCloneDiv, 'top', point.y + 'px');
      this.renderer.setStyle(this.dragCloneDiv, 'left', point.x + 'px');
      this.renderer.setStyle(this.dragCloneDiv, 'width', '200px');
      this.renderer.setStyle(this.dragCloneDiv, 'height', '100px');
    }
    //TODO needed???
    //let firstSelectedNode = this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' +  this.dragNode.id + '"]')[0] as Element;
		//if (firstSelectedNode[0]) firstSelectedNode = firstSelectedNode[0];
    this.topContainer = this.designerUtilsService.isTopContainer(this.dragNode.getAttribute("svy-layoutname"));
  }

  onMouseMove(event: MouseEvent) {
    if (!this.dragStartEvent || event.buttons == 0) return;
    if (!this.editorSession.getState().dragging) {
      if (Math.abs(this.dragStartEvent.clientX - event.clientX) > 5 || Math.abs(this.dragStartEvent.clientY - event.clientY) > 5) {
        this.editorSession.getState().dragging = true;
        this.autoscrollElementClientBounds = this.designerUtilsService.getAutoscrollElementClientBounds(this.doc);
        const layoutName = this.dragNode.getAttribute("svy-layoutname");
        if (layoutName) {
          if (this.dropHighlight !== layoutName) {
            const elements = this.dragNode.querySelectorAll('[svy-id]');
            const dropHighlightIgnoredIds = Array.from(elements).map((element) => { 
                return element.getAttribute('svy-id');
            });
            this.editorSession.sendState('dropHighlight', { dropHighlight : layoutName, dropHighlightIgnoredIds : dropHighlightIgnoredIds});
            this.dropHighlight = layoutName;
          }
          this.type = "layout";
        }
        else {
          this.type = "component";
        }
        this.editorSession.getState().drop_highlight = this.type;
      } else return;
    }

    if (this.autoscrollElementClientBounds && !this.autoscrollAreasEnabled && !this.designerUtilsService.isInsideAutoscrollElementClientBounds(this.autoscrollElementClientBounds, event.clientX, event.clientY)) {
      this.autoscrollAreasEnabled = true;
      this.editorSession.startAutoscroll(this);
    }

    if (this.dragCloneDiv){
      const point = this.adjustPoint(event.pageX + 1, event.pageY + 1);
      this.renderer.setStyle(this.dragCloneDiv, 'top',  point.y + 'px');
      this.renderer.setStyle(this.dragCloneDiv, 'left',  point.x + 'px');
      this.renderer.setStyle(this.dragCloneDiv, 'position', 'absolute');
      this.renderer.setStyle(this.dragCloneDiv, 'display', 'block' );
      this.renderer.setStyle(this.dragCloneDiv, 'z-index', 4 );
      this.renderer.setStyle(this.dragCloneDiv, 'transition', 'opacity .5s ease-in-out 0' );
    }

    const canDrop = this.getDropNode(this.type, this.topContainer, this.dragNode.getAttribute("svy-layoutname"), event, this.dragNode.getAttribute("svy-id"));
    if (!canDrop.dropAllowed) {
      this.glasspane.style.cursor = "not-allowed";
    } else this.glasspane.style.cursor = "pointer";

    this.dragStartEvent = event;

    if (canDrop.beforeChild && canDrop.beforeChild.getAttribute("svy-id") === this.dragNode.getAttribute("svy-id")) {
      canDrop.beforeChild = canDrop.beforeChild.nextElementSibling;
    }

    if (canDrop.dropTarget) {
      if (this.glasspane.style.cursor == "pointer" && !this.dragNode.contains(canDrop.dropTarget)) {
        if (canDrop.beforeChild) {
          this.renderer.insertBefore(canDrop.dropTarget, this.dragNode, this.type === "component" && canDrop.beforeChild.parentElement !== canDrop.dropTarget ? canDrop.beforeChild.parentElement : canDrop.beforeChild);
        } else if (this.designerUtilsService.getParent(this.dragNode) != canDrop.dropTarget || canDrop.append) {
          this.renderer.appendChild(canDrop.dropTarget, this.dragNode);
        }
      }
    }
  }
  
  onMouseUp(event: MouseEvent) {
    if (this.dragStartEvent !== null && this.dragNode && this.editorSession.getState().dragging) {
      this.dragStartEvent = null;
      this.editorSession.getState().dragging = false;
      this.glasspane.style.cursor = "default";

      //disable mouse events on the autoscroll
      this.editorSession.getState().pointerEvents = 'none'; 
      this.autoscrollAreasEnabled = false;
      this.editorSession.stopAutoscroll();

      let obj = (event.ctrlKey || event.metaKey) ? [] : {};
      const layoutName = this.dragNode.getAttribute("svy-layoutname");
      const type = layoutName ? "layout" : "component";
      const topContainer = layoutName ? this.designerUtilsService.isTopContainer(layoutName) : false;
     
      const canDrop = this.designerUtilsService.getDropNode(this.doc, type, topContainer, layoutName, event);
      if (canDrop.dropAllowed && !canDrop.beforeChild && !canDrop.append) {
        canDrop.beforeChild = this.dragNode.nextElementSibling;
      }

      if (canDrop.beforeChild && canDrop.beforeChild.getAttribute("svy-id") === this.dragNode.getAttribute("svy-id")) {
        canDrop.beforeChild = canDrop.beforeChild.nextElementSibling;
      }

      let key = this.dragNode.getAttribute("svy-id");
      obj[key] = {};
      if ((event.ctrlKey || event.metaKey)) {
        obj[key].uuid = this.dragNode.getAttribute('cloneuuid');
      }

      if (canDrop.dropTarget) {
        obj[key].dropTargetUUID = canDrop.dropTarget.getAttribute("svy-id");
      }

      if (canDrop.beforeChild) {
        obj[key].rightSibling = canDrop.beforeChild.getAttribute("svy-id");
      }
      this.editorSession.getSession().callService('formeditor', 'moveComponent', obj, true);
      //force redrawing of the selection decorator to the new position
      this.editorSession.updateSelection(this.editorSession.getSelection());
      if (this.dragCloneDiv) {
        this.renderer.removeChild(this.doc.querySelector('iframe').contentWindow.document.body, this.dragCloneDiv);
      }
      this.dragNode = null;
      this.dropHighlight = null;
      this.editorSession.sendState('dropHighlight', null);
    }
  }

  private findAncestor(el: HTMLElement, cls: string): HTMLElement {
    while ((el = el.parentElement) && !el.classList.contains(cls));
    return el !== undefined && el !== null && el.classList.contains(cls) ? el : null;
  }

  private adjustPoint(x: number, y: number): {x: number, y: number} {
    const style = window.getComputedStyle( this.glasspane.parentElement);
    const rectangle = this.glasspane.getBoundingClientRect();
    return {x: x - parseInt(style.paddingLeft) - rectangle.left, y: y - parseInt(style.paddingLeft) - rectangle.top};
  }

  private getDropNode(type: string, topContainer: boolean, layoutName: string, event: MouseEvent, svyId: string)
  {
    const canDrop = this.designerUtilsService.getDropNode(this.doc, type, topContainer, layoutName, event, undefined, svyId);
    canDrop.dropAllowed = canDrop.dropAllowed && this.dragNode.classList.contains("inheritedElement")
        && this.initialParent !== null && this.initialParent[0].getAttribute("svy-id") !== canDrop.dropTarget.getAttribute("svy-id") ? false : canDrop.dropAllowed;
    return canDrop;
  }

  getUpdateLocationCallback(): (changeX: number, changeY: number, minX?: number, minY?: number) => void{
    return null;
  }
}