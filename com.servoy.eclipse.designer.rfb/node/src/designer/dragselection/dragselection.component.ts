import { Component, OnInit, Inject, Renderer2 } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from 'src/designer/services/editorsession.service';
import { DOCUMENT } from '@angular/common';
import { URLParserService } from 'src/designer/services/urlparser.service';
import { ElementInfo } from 'src/designer/directives/resizeknob.directive';
import { DesignerUtilsService } from '../services/designerutils.service';

@Component({
  selector: 'dragselection',
  templateUrl: './dragselection.component.html'
})
export class DragselectionComponent implements OnInit, ISupportAutoscroll {
    frameRect: {x?:number; y?:number; top?:number; left?:number};
    contentArea: HTMLElement;
    leftContentAreaAdjust: number;
    topContentAreaAdjust: number;
    dragCopy: boolean;
   
    autoscrollAreasEnabled: boolean;
    autoscrollElementClientBounds: Array<DOMRect>;
    
    dragStartEvent: MouseEvent;
    dragNode: HTMLElement = null;
    glasspane: HTMLElement;
    selectionToDrag: string[]= null;
    currentElementInfo: Map<string, ElementInfo>;

  constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2, protected urlParser: URLParserService, private readonly designerUtilsService: DesignerUtilsService) { }

  ngOnInit(): void {
      const content: HTMLElement = this.doc.querySelector('.content-area');
      content.addEventListener('mousedown', (event) => this.onMouseDown(event));
      content.addEventListener('mouseup', (event) => this.onMouseUp(event));
      content.addEventListener('mousemove', (event) => this.onMouseMove(event));
      content.addEventListener('keyup', (event) => this.onKeyup(event));
      
      this.glasspane = this.doc.querySelector('.contentframe-overlay');
      const computedStyle = window.getComputedStyle(content, null)
      this.topContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
      this.leftContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''));
  }

  private onKeyup(event: KeyboardEvent) {
    //if control is released during drag, the copy is deleted and the original element must be moved
	if (this.dragCopy && this.dragStartEvent && this.dragStartEvent.ctrlKey && (event.code.startsWith('Control') || event.code.startsWith('Meta'))) {
	    const contentDocument = this.doc.querySelector('iframe').contentWindow.document;
        for (let i = 0; i < this.selectionToDrag.length; i++) {
            const cloneInfo = this.currentElementInfo.get(this.selectionToDrag[i]);
		    const node = this.getSvyWrapper(contentDocument.querySelectorAll('[svy-id="' +  cloneInfo.element.getAttribute('cloneuuid')  + '"]')[0] as HTMLElement);
            node.style.top = cloneInfo.element.style.top;
            node.style.left = cloneInfo.element.style.left;
            this.currentElementInfo.delete(this.selectionToDrag[i]);
            this.selectionToDrag[i] = cloneInfo.element.getAttribute('cloneuuid');
            this.currentElementInfo.set(this.selectionToDrag[i], new ElementInfo(node));
            this.renderer.removeChild(cloneInfo.element.parentElement, cloneInfo.element);
	    }
        this.dragCopy = false;
	}
  }
  
  private onMouseDown(event: MouseEvent) {
      this.dragNode = this.designerUtilsService.getNode(this.doc, event) as HTMLElement;
      if(!this.currentElementInfo) {
          this.currentElementInfo = new Map<string, ElementInfo>();
      }
      //TODO skip dragging if it is a child element of a form reference
      if (event.button == 0 && this.dragNode) {
          this.dragStartEvent = event; 
      }
  }  
  
  onMouseUp(event: MouseEvent) {
      if (this.dragStartEvent != null) {
          this.sendChanges(this.currentElementInfo, event);
          this.dragStartEvent = null;
          
          //disable mouse events on the autoscroll
          this.editorSession.getState().pointerEvents = 'none'; 
          this.autoscrollAreasEnabled = false;
          this.editorSession.stopAutoscroll();
          
          //force redrawing of the selection decorator to the new position
          this.editorSession.updateSelection(this.editorSession.getSelection());
      }
      this.selectionToDrag = null;
      this.editorSession.getState().dragging = false;
      this.currentElementInfo = null;
      this.dragCopy = false;
  }

  private sendChanges(elementInfos: Map<string, ElementInfo>, event: MouseEvent) {
      if(elementInfos) {
          const changes = (event.ctrlKey||event.metaKey) ? [] : {};
          let i = 0;
          for (const nodeId of elementInfos.keys()) {
              const elementInfo = elementInfos.get(nodeId);
              const id = (event.ctrlKey || event.metaKey) ? i++ : nodeId;
              changes[id] = {
                  x: elementInfo.x,
                  y: elementInfo.y
              };

              if ((event.ctrlKey || event.metaKey) && this.dragCopy) {
                  changes[id].uuid = elementInfo.element.getAttribute('cloneuuid');
                  this.renderer.removeChild(elementInfo.element.parentElement, elementInfo.element);
              }
          }
          if((event.ctrlKey||event.metaKey) && (changes as Array<any>).length){
            this.editorSession.createComponents({
                "components": changes
            });
        } else {
            this.editorSession.sendChanges(changes);
        }
      }
  }
  
  private onMouseMove(event: MouseEvent) {
          if (!this.dragStartEvent)  return;
              if (!this.editorSession.getState().dragging) {
                  if (Math.abs(this.dragStartEvent.clientX - event.clientX) > 5 || Math.abs(this.dragStartEvent.clientY - event.clientY) > 5) {
                    this.editorSession.getState().dragging = true;
                    this.autoscrollElementClientBounds = this.designerUtilsService.getAutoscrollElementClientBounds(this.doc);
                  } else return;
              }
             
              // enable auto-scroll areas only if current mouse event is outside of them (this way, when starting to drag from an auto-scroll area it will not immediately auto-scroll)
				if (this.autoscrollElementClientBounds && !this.autoscrollAreasEnabled && !this.designerUtilsService.isInsideAutoscrollElementClientBounds(this.autoscrollElementClientBounds, event.clientX, event.clientY)) {
					this.autoscrollAreasEnabled = true;
                    this.editorSession.startAutoscroll(this);
				}

              if ((event.ctrlKey || event.metaKey) && this.selectionToDrag == null || !this.selectionToDrag || this.selectionToDrag.length == 0) {
                  if (event.ctrlKey || event.metaKey) this.dragCopy = true;
                  this.selectionToDrag = [];
                  const selection = this.editorSession.getSelection();
                  if (this.dragNode && !selection.includes(this.dragNode.getAttribute('svy-id'))){
                    selection.push(this.dragNode.getAttribute('svy-id'));
                  }

                  for (let i = 0; i < selection.length; i++) {
                      let node = this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' +  selection[i]  + '"]')[0] as HTMLElement;
                      if (node === undefined) {
                          console.log('Node with uuid '+selection[i]+' was not found');
                          continue;
                      }
                      node = this.getSvyWrapper(node);
                      if (this.dragCopy) {  
                        const clone = node.cloneNode(true) as HTMLElement;
                        this.renderer.setAttribute(clone, 'id', 'dragNode' + i);
                        this.renderer.setAttribute(clone, 'cloneuuid', selection[i]);
                        this.renderer.appendChild(this.doc.querySelector('iframe').contentWindow.document.body, clone);

                        this.selectionToDrag.push('dragNode' + i);
						this.currentElementInfo.set(this.selectionToDrag[i], new ElementInfo(clone));
                      }
                      else {
                        this.selectionToDrag.push(selection[i]); 
                        this.currentElementInfo.set( selection[i], new ElementInfo(node));
                      }
                  }    
              }

              if (this.selectionToDrag.length > 0) {
                  let firstSelectedNode = this.selectionToDrag[0];
                  if (firstSelectedNode[0]) firstSelectedNode = firstSelectedNode[0];
                      const changeX = event.clientX - this.dragStartEvent.clientX;
                      const changeY = event.clientY - this.dragStartEvent.clientY;
                      
                      //make sure no element goes offscreen
                      let canMove = true;                    
                      for (let i = 0; i < this.selectionToDrag.length; i++)
                      {
                          const elementInfo =  this.currentElementInfo.get( this.selectionToDrag[i]);
                          if (!elementInfo) {
                              let node =this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' +  this.selectionToDrag[i] + '"]')[0] as HTMLElement;
                              node = this.getSvyWrapper(node);
                              this.currentElementInfo.set(this.selectionToDrag[i], new ElementInfo(node));
                          }
                      
                          if (elementInfo &&  (elementInfo.y + changeY < 0 || elementInfo.x + changeX < 0))  {
                              canMove = false;
                              break;
                          }
                      }
                      
                      if (canMove)
                      {
                          this.updateLocation(changeX, changeY);
                      }
                      this.dragStartEvent = event;
              }
      }
    
    private getSvyWrapper(node: HTMLElement) {
        while (node && !node.classList.contains('svy-wrapper')) {
            node = node.parentElement;
        }
        return node;
    }

    getUpdateLocationCallback(): (changeX: number, changeY: number, minX?: number, minY?: number)=> void{
        return this.updateLocation.bind(this);
    }
              
    updateLocation(changeX: number, changeY: number, minX?: number, minY?: number): void {
        if (this.selectionToDrag == null) return;
        for (let i = 0; i < this.selectionToDrag.length; i++) {
            const elementInfo = this.currentElementInfo.get(this.selectionToDrag[i]);
            elementInfo.x = elementInfo.x + changeX;
            if (minX != undefined && elementInfo.x < minX) elementInfo.x = minX;
            elementInfo.y = elementInfo.y + changeY;
            if (minY != undefined && elementInfo.y < minY) elementInfo.y = minY;
            elementInfo.element.style.position = 'absolute';
            elementInfo.element.style.top = (parseInt(elementInfo.element.style.top.replace('px', '')) || 0)  + changeY + 'px';
            elementInfo.element.style.left = (parseInt(elementInfo.element.style.left.replace('px', ''))|| 0)  + changeX + 'px';
        }
    }
}
