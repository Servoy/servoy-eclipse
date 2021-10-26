import { Component, OnInit, Inject, Renderer2 } from '@angular/core';
import { EditorSessionService } from 'src/designer/services/editorsession.service';
import { DOCUMENT } from '@angular/common';
import { URLParserService } from 'src/designer/services/urlparser.service';
import { ElementInfo } from 'src/designer/directives/resizeknob.directive';
import { DesignerUtilsService } from '../services/designerutils.service';

@Component({
  selector: 'dragselection',
  templateUrl: './dragselection.component.html'
})
export class DragselectionComponent implements OnInit {
    frameRect: {x?:number; y?:number; top?:number; left?:number};
    contentArea: HTMLElement;
    leftContentAreaAdjust: number;
    topContentAreaAdjust: number;
    dragCopy: boolean;
   
    autoscrollAreasEnabled: boolean;
    autoscrollElementClientBounds: Array<DOMRect>;
    
//    initialParent: any;
    dragStartEvent: MouseEvent;
    dragNode: HTMLElement = null;
    glasspane: HTMLElement;
    selectionToDrag: string[]= null;
    
    currentElementInfo: Map<string, ElementInfo>;
    autoscrollEnter:  { [key: string]: (event: MouseEvent) => void; } = {};
    autoscrollStop: { [key: string]: ReturnType<typeof setInterval>} = {};
    autoscrollLeave: { [key: string]: (event: MouseEvent) => void; } = {};
    

  constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2, protected urlParser: URLParserService, private readonly designerUtilsService: DesignerUtilsService) { }

  ngOnInit(): void {
      const content: HTMLElement = this.doc.querySelector('.content-area');
      content.addEventListener('mousedown', (event) => this.onMouseDown(event));
      content.addEventListener('mouseup', () => this.onMouseUp());
      content.addEventListener('mousemove', (event) => this.onMouseMove(event));
      
      this.glasspane = this.doc.querySelector('.contentframe-overlay');
      const computedStyle = window.getComputedStyle(content, null)
      this.topContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
      this.leftContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''));
  }
  
  private onMouseDown(event: MouseEvent) {
      this.dragNode = this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' + this.editorSession.getSelection()[0] + '"]')[0] as HTMLElement
      if(!this.currentElementInfo) {
          this.currentElementInfo = new Map<string, ElementInfo>();
      }
      // skip dragging if it is a child element of a form reference
      if (event.button == 0 && this.dragNode) {
          this.dragStartEvent = event; 
      }
  }
  
  convertToContentPoint(point: {x?:number; y?:number; top?:number; left?:number}) {
      if  (this.frameRect === undefined) {
          this.frameRect = this.glasspane.getBoundingClientRect();
      }
      if (point.x && point.y) {
          point.x = point.x - this.frameRect.left;
          point.y = point.y - this.frameRect.top;
      } else if (point.top && point.left) {
          point.left = point.left - this.frameRect.left;
          point.top = point.top - this.frameRect.top;
      }
      return point
  }
  
  addAutoscrollListeners(direction: string) {
    this.editorSession.getState().pointerEvents = 'all';
   
    this.autoscrollEnter[direction] = this.registerDOMEvent('mouseenter', direction, () => {
        this.autoscrollStop[direction] = this.startAutoScroll(direction, this.updateAbsoluteLayoutComponentsLocations);
    }) as () => void;

    this.autoscrollLeave[direction] = (event: MouseEvent) => {
        if (this.autoscrollStop[direction]) {
            clearInterval(this.autoscrollStop[direction]);
            this.autoscrollStop[direction] = undefined;
        }
        if (event.type == 'mouseup')
            this.onMouseUp();
    }

    this.registerDOMEvent('mouseleave', direction, this.autoscrollLeave[direction]);
    this.registerDOMEvent('mouseup', direction, this.autoscrollLeave[direction]);
   }

    startAutoScroll(direction: string, callback: (selectionToDrag:string[], currentElementInfo: Map<string, ElementInfo>, 
                                changeX: number, changeY: number, minX?: number, minY?: number) => void): ReturnType<typeof setInterval> {
        let autoScrollPixelSpeed = 2;
        const selection = this.selectionToDrag;
        const info = this.currentElementInfo;
        return setInterval(() => {
            autoScrollPixelSpeed = this.autoScroll(selection, info, direction, autoScrollPixelSpeed, callback);
        }, 50);
    }

    private autoScroll(selection: string[], info: Map<string, ElementInfo>, direction: string, autoScrollPixelSpeed: number, callback: (selectionToDrag:string[], currentElementInfo: Map<string, ElementInfo>, changeX: number, changeY: number, minX?: number, minY?: number) => void) {
        const content: HTMLElement = this.doc.querySelector('.content-area') ;
        let changeX = 0;
        let changeY = 0;
        switch (direction) {
            case 'BOTTOM_AUTOSCROLL':
                if ((content.scrollTop + content.offsetHeight) === content.scrollHeight)
                    this.glasspane.style.height = parseInt(this.glasspane.style.height.replace('px', '')) + autoScrollPixelSpeed + 'px';
                content.scrollTop += autoScrollPixelSpeed;
                changeY = autoScrollPixelSpeed;
                break;
            case 'RIGHT_AUTOSCROLL':
                if ((content.scrollLeft + content.offsetWidth) === content.scrollWidth)
                    this.glasspane.style.width = parseInt(this.glasspane.style.width.replace('px', '')) + autoScrollPixelSpeed + 'px';
                content.scrollLeft += autoScrollPixelSpeed;
                changeX = autoScrollPixelSpeed;
                break;
            case 'LEFT_AUTOSCROLL':
                if (content.scrollLeft >= autoScrollPixelSpeed) {
                    content.scrollLeft -= autoScrollPixelSpeed;
                    changeX = -autoScrollPixelSpeed;
                } else {
                    changeX = -content.scrollLeft;
                    content.scrollLeft = 0;
                }
                break;
            case 'TOP_AUTOSCROLL':
                if (content.scrollTop >= autoScrollPixelSpeed) {
                    content.scrollTop -= autoScrollPixelSpeed;
                    changeY = -autoScrollPixelSpeed;
                } else {
                    changeY = -content.scrollTop;
                    content.scrollTop -= 0;
                }
                break;
        }

        if (autoScrollPixelSpeed < 15)
            autoScrollPixelSpeed++;

        if (callback)
            callback(selection, info, changeX, changeY, 0, 0);
        return autoScrollPixelSpeed;
    }

   isInsideAutoscrollElementClientBounds(clientX: number, clientY: number): boolean {
    if (!this.autoscrollElementClientBounds) return false;
			
    let inside = false;
    let i = 0;
    while (!inside && i < 4) { // 4 == autoscrollElementClientBounds.length always
        const rect = this.autoscrollElementClientBounds[i++];
        inside = (clientX >= rect.left && clientX <= rect.right && clientY >= rect.top && clientY <= rect.bottom);
    }
    return inside;
   } 

    registerDOMEvent(eventType: string, target: string, callback: (event: MouseEvent) => void): (event: MouseEvent) => void {
        let element: HTMLElement;
        if (target === 'BOTTOM_AUTOSCROLL') {
            element = this.doc.querySelector('.bottomAutoscrollArea') ;
        } else if (target === 'RIGHT_AUTOSCROLL') {
            element = this.doc.querySelector('.rightAutoscrollArea') ;
        } else if (target === 'LEFT_AUTOSCROLL') {
            element = this.doc.querySelector('.leftAutoscrollArea') ;
        } else if (target === 'TOP_AUTOSCROLL') {
            element = this.doc.querySelector('.topAutoscrollArea') ;
        }
        element.addEventListener(eventType, callback)
        return callback;
    }

    unregisterDOMEvent(eventType: string, target: string, callback: (event: MouseEvent) => void) {
        let element: HTMLElement;
        if (target === 'BOTTOM_AUTOSCROLL') {
            element = this.doc.querySelector('.bottomAutoscrollArea') ;
        } else if (target === 'RIGHT_AUTOSCROLL') {
            element = this.doc.querySelector('.rightAutoscrollArea') ;
        } else if (target === 'LEFT_AUTOSCROLL') {
            element = this.doc.querySelector('.leftAutoscrollArea') ;
        } else if (target === 'TOP_AUTOSCROLL') {
            element = this.doc.querySelector('.topAutoscrollArea') ;
        }
        element.removeEventListener(eventType, callback);
    }
  
  
  private onMouseUp() {
      if (this.dragStartEvent != null) {
          this.dragStartEvent = null;
          this.selectionToDrag = null;
          this.editorSession.getState().dragging = false;
          this.currentElementInfo = null;
          
          //disable mouse events on the autoscroll
          this.editorSession.getState().pointerEvents = 'none'; 
          this.autoscrollAreasEnabled = false;
			
			for (const direction in this.autoscrollEnter) {
				if(this.autoscrollEnter[direction]) this.unregisterDOMEvent('mouseenter', direction, this.autoscrollEnter[direction]);
			}
			for (const direction in this.autoscrollLeave) {
				if(this.autoscrollLeave[direction]) {
					this.unregisterDOMEvent('mouseleave', direction, this.autoscrollLeave[direction]);
					this.unregisterDOMEvent('mouseup', direction, this.autoscrollLeave[direction]);
				}
			}
          
          this.sendChanges(this.currentElementInfo);
          //force redrawing of the selection decorator to the new position
          this.editorSession.updateSelection(this.editorSession.getSelection());
      }
  }

  private sendChanges(elementInfos: Map<string, ElementInfo>) {
      if(elementInfos) {
          const changes = {};
          for(const nodeId of elementInfos.keys()) {
              const elementInfo = elementInfos.get(nodeId);
              changes[nodeId] = {
                  x: elementInfo.x,
                  y: elementInfo.y
              }
          }
          this.editorSession.sendChanges(changes);
      }
  }
  
  private onMouseMove(event: MouseEvent) {
          if (!this.dragStartEvent)  return;
              if (!this.editorSession.getState().dragging) {
                  if (Math.abs(this.dragStartEvent.clientX - event.clientX) > 5 || Math.abs(this.dragStartEvent.clientY - event.clientY) > 5) {
                    this.editorSession.getState().dragging = true;
                    this.autoscrollElementClientBounds = this.getAutoscrollElementClientBounds();
                  } else return;
              }
             
              // enable auto-scroll areas only if current mouse event is outside of them (this way, when starting to drag from an auto-scroll area it will not immediately auto-scroll)
				if (this.autoscrollElementClientBounds && !this.autoscrollAreasEnabled && !this.isInsideAutoscrollElementClientBounds(event.clientX, event.clientY)) {
					this.autoscrollAreasEnabled = true;
					
					this.addAutoscrollListeners('BOTTOM_AUTOSCROLL')
					this.addAutoscrollListeners('RIGHT_AUTOSCROLL')
					this.addAutoscrollListeners('TOP_AUTOSCROLL')
					this.addAutoscrollListeners('LEFT_AUTOSCROLL')
				}

              if ((event.ctrlKey || event.metaKey) && this.selectionToDrag == null) {
                  this.dragCopy = true;
                  this.selectionToDrag = [];
                  const selection = this.editorSession.getSelection();
                
                  const elements = this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id]');
                  const point = this.convertToContentPoint({ x: this.dragStartEvent.pageX, y: this.dragStartEvent.pageY });
                  const elementAtMousePos = Array.from(elements).reverse().find((node) => {
                      const position = node.getBoundingClientRect();
                      this.designerUtilsService.adjustElementRect(node, position);
                      if (node['offsetParent'] !== null && position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                          return node;
                      }
                  });
                  if (elementAtMousePos && !selection.includes(elementAtMousePos.getAttribute('svy-id'))){
                        selection.push(elementAtMousePos.getAttribute('svy-id'));
                  }

                  for (let i = 0; i < selection.length; i++) {
                      const node = this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' +  selection[i]  + '"]')[0] as HTMLElement;
                      this.selectionToDrag[i] = selection[i]; 
                      this.currentElementInfo.set( selection[i], new ElementInfo(node));
                  }    
              }

              if (!this.selectionToDrag) {
                  this.selectionToDrag = this.editorSession.getSelection();
                  if (!this.selectionToDrag || this.selectionToDrag.length == 0)
                  {
                      this.selectionToDrag = [this.dragNode.getAttribute('svy-id')];
                      this.currentElementInfo.set(this.editorSession.getSelection()[0], new ElementInfo(this.dragNode));
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
                              const node =this.doc.querySelector('iframe').contentWindow.document.querySelectorAll('[svy-id="' +  this.selectionToDrag[i] + '"]')[0] as HTMLElement;
                              this.currentElementInfo.set(this.selectionToDrag[i],new ElementInfo(node));
                          }
                      
                          if (elementInfo &&  (elementInfo.y + changeY < 0 || elementInfo.x + changeX < 0))  {
                              canMove = false;
                              break;
                          }
                      }
                      
                      if (canMove)
                      {
                          this.updateAbsoluteLayoutComponentsLocations(this.selectionToDrag, this.currentElementInfo, changeX, changeY);
                      }
                      this.dragStartEvent = event;
              }
      }
    getAutoscrollElementClientBounds(): Array<DOMRect> {
        const bottomAutoscrollArea: HTMLElement = this.doc.querySelector('.bottomAutoscrollArea');

        let autoscrollElementClientBounds: Array<DOMRect>;
        if (bottomAutoscrollArea) {
            autoscrollElementClientBounds = [];
            autoscrollElementClientBounds[0] = bottomAutoscrollArea.getBoundingClientRect();
            autoscrollElementClientBounds[1] = this.doc.querySelector('.rightAutoscrollArea').getBoundingClientRect();
            autoscrollElementClientBounds[2] = this.doc.querySelector('.leftAutoscrollArea').getBoundingClientRect();
            autoscrollElementClientBounds[3] = this.doc.querySelector('.topAutoscrollArea').getBoundingClientRect();
        }
        return autoscrollElementClientBounds;
    }
              
    private updateAbsoluteLayoutComponentsLocations = (selectionToDrag:string[], currentElementInfo: Map<string, ElementInfo>, changeX: number, changeY: number, minX?: number, minY?: number) => {
        for (let i = 0; i < selectionToDrag.length; i++) {
            const elementInfo = currentElementInfo.get(selectionToDrag[i]);
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
