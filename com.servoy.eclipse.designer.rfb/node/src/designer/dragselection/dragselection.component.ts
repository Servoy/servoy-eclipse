import { HttpClient } from '@angular/common/http';

import { Component, OnInit, Renderer2, AfterViewInit, OnDestroy } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from 'src/designer/services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { ElementInfo } from 'src/designer/directives/resizeknob.directive';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
  selector: 'dragselection',
  templateUrl: './dragselection.component.html'
})
export class DragselectionComponent implements OnInit, ISupportAutoscroll, IContentMessageListener, OnDestroy {
    frameRect: {x?:number; y?:number; top?:number; left?:number};
    leftContentAreaAdjust: number;
    topContentAreaAdjust: number;
    dragCopy: boolean;
    
    dragStartEvent: MouseEvent;
    dragNode: HTMLElement = null;
    selectionToDrag: string[]= null;
    currentElementsInfo: Map<string, ElementInfo>;

    glasspane: HTMLElement;
    contentArea: HTMLElement;

    autoscrollMargin = 100; //how many pixels we can drag beyong current margins
    autoscrollOffset = {x: 0, y: 0};
    containerOffset = 0; //same for vertical / horizontal positioning
    minimumMargins = {bottom: 0, right: 0};
    selectionRect = {top: 0, left: 0, width: 0, height: 0};
    mousedownpoint = {x: 0, y: 0};
    mouseOffset = {top: 0, left: 0}; //autoscroll limits will refer to this parameter
    snapData: {top: number, left: number, cssPosition: { property: string }, guides: [] };

  constructor(protected readonly editorSession: EditorSessionService, protected readonly renderer: Renderer2, protected urlParser: URLParserService, private readonly designerUtilsService: DesignerUtilsService, private editorContentService : EditorContentService) { }

  ngOnInit(): void {
      this.contentArea = this.editorContentService.getContentArea();
      this.contentArea.addEventListener('mousedown', (event) => this.onMouseDown(event));
      this.contentArea.addEventListener('mouseup', (event) => this.onMouseUp(event));
      this.contentArea.addEventListener('mousemove', (event) => this.onMouseMove(event));
      this.contentArea.addEventListener('keyup', (event) => this.onKeyup(event));
      
      const computedStyle = window.getComputedStyle(this.editorContentService.getContentArea(), null)
      this.topContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-left').replace('px', ''));
      this.leftContentAreaAdjust = parseInt(computedStyle.getPropertyValue('padding-top').replace('px', ''));
      this.glasspane = this.editorContentService.getGlassPane();
      
      this.selectionRect = {top: 0, left: 0, width: 0, height: 0};
      this.autoscrollOffset.x = 0;
      this.autoscrollOffset.y = 0;

      this.editorContentService.addContentMessageListener(this);
  }

  ngOnDestroy(): void {
    this.editorContentService.removeContentMessageListener(this);
  }

  private onKeyup(event: KeyboardEvent) {
    //if control is released during drag, the copy is deleted and the original element must be moved
	if (this.dragCopy && this.dragStartEvent && this.dragStartEvent.ctrlKey && (event.code.startsWith('Control') || event.code.startsWith('Meta'))) {
        for (let i = 0; i < this.selectionToDrag.length; i++) {
            const cloneInfo = this.currentElementsInfo.get(this.selectionToDrag[i]);
           const node = this.getSvyWrapper(this.editorContentService.getContentElement(cloneInfo.element.getAttribute('cloneuuid')));
            node.style.top = cloneInfo.element.style.top;
            node.style.left = cloneInfo.element.style.left;
            this.currentElementsInfo.delete(this.selectionToDrag[i]);
            this.selectionToDrag[i] = cloneInfo.element.getAttribute('cloneuuid');
            this.currentElementsInfo.set(this.selectionToDrag[i], new ElementInfo(node));
            this.renderer.removeChild(cloneInfo.element.parentElement, cloneInfo.element);
        }
        this.dragCopy = false;
	}
  }
  
  onMouseDown(event: MouseEvent) {
      this.dragNode = this.designerUtilsService.getNode(event);
      if (this.dragNode && this.dragNode.parentElement.closest('.svy-responsivecontainer')){
         // we are inside responsive container, let dragselection-responsive handle this
         this.dragNode = null;
         return;
      }
      if(!this.currentElementsInfo) {
          this.currentElementsInfo = new Map<string, ElementInfo>();
      }
      //TODO skip dragging if it is a child element of a form reference
      if (event.button == 0 && this.dragNode) {
          this.dragStartEvent = event; 
      }

      this.mousedownpoint.x = event.pageX;
      this.mousedownpoint.y = event.pageY;
      this.editorSession.registerAutoscroll(this);
  }  
  
  onMouseUp(event: MouseEvent) {
      if (this.dragStartEvent != null) {
          if (this.currentElementsInfo.size > 0) this.sendChanges(this.currentElementsInfo, event);
          this.dragStartEvent = null;
          
          //force redrawing of the selection decorator to the new position
          this.editorSession.updateSelection(this.editorSession.getSelection(), false, true);

          this.resizeGlasspane();
      }
      this.selectionToDrag = null;
      this.editorSession.setDragging(false);
      this.currentElementsInfo = null;
      this.dragCopy = false;
      this.editorSession.unregisterAutoscroll(this);
      this.snapData = null;
  }

  private sendChanges(elementInfos: Map<string, ElementInfo>, event: MouseEvent) {
      if(elementInfos && elementInfos.size) {
          const changes = (event.ctrlKey||event.metaKey) ? [] : {};
          let i = 0;
          for (const nodeId of elementInfos.keys()) {
              const elementInfo = elementInfos.get(nodeId);
              const parentFC = elementInfo.element.closest('.svy-formcomponent');
              if (parentFC && !elementInfo.element.classList.contains('svy-formcomponent')) {
				const parentRect = parentFC.getBoundingClientRect();
				elementInfo.x = elementInfo.x - parentRect.x;
				elementInfo.y = elementInfo.y - parentRect.y;
              }
              const id = (event.ctrlKey || event.metaKey) ? i++ : nodeId;
              changes[id] = {
                  x: elementInfo.x,
                  y: elementInfo.y
              };
              if (this.snapData && this.selectionToDrag.length == 1) {
                  const cssPos = this.snapData.cssPosition;
                  changes[id]['cssPos'] = cssPos;
                  const contentRect = this.editorContentService.getContentArea().getBoundingClientRect();
                  if (cssPos) {
                      if (!cssPos['left'] && !cssPos['right'] && !cssPos['middleH']) {
                          changes[id]['x'] = event.clientX + this.editorContentService.getContentArea().scrollLeft - contentRect?.left - this.leftContentAreaAdjust;
                      }
                      if (!cssPos['top'] && !cssPos['bottom'] && !cssPos['middleV']) {
                          changes[id]['y'] = event.clientY + this.editorContentService.getContentArea().scrollTop - contentRect?.top - this.topContentAreaAdjust;
                      }
                  }
              }

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
      this.snapData = null;
  }
  
    onMouseMove(event: MouseEvent) {
          if (!this.dragStartEvent || this.snapData)  return;
              if (!this.editorSession.getState().dragging) {
                  if (Math.abs(this.dragStartEvent.clientX - event.clientX) > 5 || Math.abs(this.dragStartEvent.clientY - event.clientY) > 5) {
                    this.editorSession.setDragging( true );
                    //this.autoscrollElementClientBounds = this.designerUtilsService.getAutoscrollElementClientBounds();
                  } else return;
              }

              if ((event.ctrlKey || event.metaKey) && this.selectionToDrag == null) {
                  this.dragCopy = true;
                  const selection = this.editorSession.getSelection();
                  if (this.dragNode && !selection.includes(this.dragNode.getAttribute('svy-id'))){
                    selection.push(this.dragNode.getAttribute('svy-id'));
                  }
                  this.initSelectionToDrag(selection);
              }

              if (!this.selectionToDrag) {
                let selection = this.editorSession.getSelection();
                if (!selection || selection.length == 0)
                {
                    selection = [this.dragNode.getAttribute('svy-id')];
                }
                this.initSelectionToDrag(selection);
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
                          const elementInfo =  this.currentElementsInfo.get( this.selectionToDrag[i]);
                          if (!elementInfo) {
                              let node = this.editorContentService.getContentElement(this.selectionToDrag[i]);
                              node = this.getSvyWrapper(node);
                              this.currentElementsInfo.set(this.selectionToDrag[i], new ElementInfo(node));
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
    
    private initSelectionToDrag(selection: string[]) {
        this.selectionToDrag = [];
        for (let i = 0; i < selection.length; i++) {
            let node = this.editorContentService.getContentElement(selection[i]);
            if (!node) {
                continue;
            }
            node = this.getSvyWrapper(node);
            if (this.dragCopy) {
                const clone = node.cloneNode(true) as HTMLElement;
                this.renderer.setAttribute(clone, 'id', 'dragNode' + i);
                this.renderer.setAttribute(clone, 'cloneuuid', selection[i]);
                this.renderer.appendChild(this.editorContentService.getContentBodyElement(), clone);

                this.selectionToDrag.push('dragNode' + i);
                this.currentElementsInfo.set(this.selectionToDrag[i], new ElementInfo(clone));
            }
            else {
                this.selectionToDrag.push(selection[i]);
                this.currentElementsInfo.set(selection[i], new ElementInfo(node));
            }
        }
        this.initAutoscrollData();
    }
    
    private getSvyWrapper(node: HTMLElement) {
        while (node && !node.classList.contains('svy-wrapper')) {
            node = node.parentElement;
        }
        return node;
    }
              
    updateLocation(changeX: number, changeY: number, minX?: number, minY?: number): void {
        if (this.selectionToDrag == null) return;

        this.selectionRect.top += changeY;
        this.selectionRect.left += changeX;
        if (minY != undefined && this.selectionRect.top < minY) {
            this.selectionRect.top = minY;
            return;
        }
        if (minX != undefined && this.selectionRect.left < minX) {
            this.selectionRect.left = minX;
            return;
        }

        for (let i = 0; i < this.selectionToDrag.length; i++) {
            const elementInfo = this.currentElementsInfo.get(this.selectionToDrag[i]);
            elementInfo.x = elementInfo.x + changeX;
            if (minX != undefined && elementInfo.x < minX) elementInfo.x = minX;
            elementInfo.y = elementInfo.y + changeY;
            if (minY != undefined && elementInfo.y < minY) elementInfo.y = minY;
            elementInfo.element.style.position = 'absolute';
            elementInfo.element.style.top = (parseInt(elementInfo.element.style.top.replace('px', '')) || 0)  + changeY + 'px';
            elementInfo.element.style.left = (parseInt(elementInfo.element.style.left.replace('px', ''))|| 0)  + changeX + 'px';
            if (elementInfo.element.style.right.length) {
				elementInfo.element.style.right = (parseInt(elementInfo.element.style.right.replace('px', ''))|| 0)  - changeX + 'px';
			}
			if (elementInfo.element.style.bottom.length) {
				elementInfo.element.style.bottom = (parseInt(elementInfo.element.style.bottom.replace('px', ''))|| 0)  - changeY + 'px';
			}
        }
    }

    contentMessageReceived(id: string, data: { property: string }) {
        if (id === 'snap' && this.selectionToDrag) {
                this.snapData = data['properties'];
                if (this.snapData?.top && this.snapData?.left && this.selectionToDrag.length == 1) {
                    const elementInfo = this.currentElementsInfo.get(this.selectionToDrag[0]);
                    elementInfo.element.style.position = 'absolute';
                    elementInfo.element.style.top = this.snapData?.top + 'px';
                    elementInfo.element.style.left = this.snapData?.left + 'px';
                }
        }
    }

    initAutoscrollData() {
        if (this.selectionToDrag == null) return;

        //compute selected components rectangle
        this.selectionRect.top = 0;
        this.selectionRect.left = 0;
        let bottom = 0;
        let right = 0;
        for (let i = 0; i < this.selectionToDrag.length; i++) {
            const elementInfo = this.currentElementsInfo.get(this.selectionToDrag[i]);
            this.selectionRect.top = i == 0 ? elementInfo.y : Math.min(this.selectionRect.top, elementInfo.y);
            this.selectionRect.left = i == 0 ? elementInfo.x : Math.min(this.selectionRect.left, elementInfo.x);
            bottom = i == 0 ? elementInfo.y + elementInfo.height : Math.max(bottom, elementInfo.y + elementInfo.height);
            right = i == 0 ? elementInfo.x + elementInfo.width : Math.max(right, elementInfo.x + elementInfo.width);
        }
        this.selectionRect.width = right - this.selectionRect.left;
        this.selectionRect.height = bottom - this.selectionRect.top;

        //compute minimum bottom - right (that is visible area + margins, coordinates relative to glasspane)
        //for coordinates, can't refer to the parts only since they can be all missing
        //al ghstcontainer coordinates are the same, so processing first
        const container =  this.editorContentService.querySelector('.ghostcontainer');
        this.containerOffset = container.offsetTop; //same for vertical / horizontal positioning
        this.minimumMargins.bottom = this.containerOffset + container.offsetHeight;
        this.minimumMargins.right = this.containerOffset + container.offsetWidth;

        //mousedownpoint is relative to editor and scroll position
        //contentarea coordinates are relative to editor
        this.mouseOffset.top = (this.mousedownpoint.y + this.contentArea.scrollTop) - (this.contentArea.offsetTop + this.containerOffset) - this.selectionRect.top;
        this.mouseOffset.left = (this.mousedownpoint.x + this.contentArea.scrollLeft) - (this.contentArea.offsetLeft + this.containerOffset) - this.selectionRect.left
        
        const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
        for (let index=0; index < ghostsList.length; index++) {
            const ghost = ghostsList.item(index);
            const ghostType = ghost.getAttribute('svy-ghosttype');
            if (ghostType == 'part') {//coordinates relative to itself
                this.minimumMargins.bottom += (ghost as HTMLElement).offsetHeight;
                this.minimumMargins.right += (ghost as HTMLElement).offsetWidth;
                break;
            }
        }

        this.autoscrollOffset.x = 0;
        this.autoscrollOffset.y = 0;
    }

    resizeGlasspane() {
        let scrollTop = Math.max(0, (this.selectionRect.top + this.selectionRect.height) - this.glasspane.offsetHeight);
        let scrollLeft = Math.max(0, (this.selectionRect.left + this.selectionRect.width) - this.glasspane.offsetWidth);
        if (scrollTop > 0) scrollTop += 10;
        if (scrollLeft > 0) scrollLeft += 10;

        const ghostMargins = this.getGhostMargins();
        const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
        if (ghostsList.length <= 1) return; //empty container (only one form) => nothing to resize
        const margins = {bottom: Math.max(ghostMargins.bottom, this.minimumMargins.bottom), right: Math.max(ghostMargins.right, this.minimumMargins.right)};

        margins.bottom = Math.max(margins.bottom, this.selectionRect.top + this.selectionRect.height) + 10;
        margins.right = Math.max(margins.right, this.selectionRect.left + this.selectionRect.width) + 10;
        if (margins.bottom > this.glasspane.offsetHeight) {
            this.glasspane.style.height = margins.bottom + 'px';
            this.contentArea.scrollTop += scrollTop;
        }
        if (margins.right > this.glasspane.offsetHeight) {
            this.glasspane.style.width = margins.right + 'px';
            this.contentArea.scrollLeft += scrollLeft;
        } 
    }

     getGhostMargins(): {bottom: number, right: number} {
        const margins = {bottom: 0, right: 0};
        const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
        for (let index = 0; index < ghostsList.length - 1; index++) {
            const ghost = ghostsList.item(index);
            const bottom = (ghost as HTMLElement).offsetTop + (ghost as HTMLElement).offsetHeight;
            const right = (ghost as HTMLElement).offsetLeft + (ghost as HTMLElement).offsetWidth;
            margins.bottom = Math.max(margins.bottom, bottom);
            margins.right = Math.max(margins.right, right);
        }
        return margins;
    }

    getAutoscrollLockId(): string {
        return 'drag-selection';
    }

    /* 
     * glasspane - is the absolute reference coordinates system
     * containerOffset is relative to glasspane origin
     * selectionRect coordinates are relative to the top/left container area
     */
    updateLocationCallback(changeX: number, changeY: number) {
        if (this.selectionToDrag == null) return;
        if (changeY > 0 && this.autoscrollOffset.y > this.autoscrollMargin) return;
        if (changeX > 0 && this.autoscrollOffset.x > this.autoscrollMargin) return;
        if ((this.selectionRect.top + this.mouseOffset.top) > (Math.max(this.glasspane.offsetHeight, this.minimumMargins.bottom) - this.containerOffset)) {
             this.glasspane.style.height = this.glasspane.offsetHeight + changeY + 'px';
             this.autoscrollOffset.y += changeY;
         } 

        if ((this.selectionRect.left + this.mouseOffset.left) > Math.max(this.glasspane.offsetWidth, this.minimumMargins.right) - this.containerOffset) {
            this.glasspane.style.width = this.glasspane.offsetWidth + changeX + 'px';
            this.autoscrollOffset.x += changeX;
        } 

        this.updateLocation(changeX, changeY, 0, 0);
        this.contentArea.scrollTop += changeY; 
        this.contentArea.scrollLeft += changeX;
    }
}
