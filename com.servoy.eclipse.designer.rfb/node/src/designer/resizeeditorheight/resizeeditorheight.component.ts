import { Component, ViewChild, ElementRef, OnInit, Renderer2 } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { Point } from './../mouseselection/mouseselection.component';

@Component({
    selector: 'designer-resizeeditorheight',
    templateUrl: './resizeeditorheight.component.html',
    styleUrls: ['./resizeeditorheight.component.css'],
    standalone: false
})
export class ResizeEditorHeightComponent implements OnInit, ISupportAutoscroll {
    @ViewChild('resizer', { static: true }) resizerRef: ElementRef<HTMLElement>;

    
    private lowestPart: Element | null = null;
    private dragingEvent = null;
    private ghostContainers: HTMLElement[];
    private editorContent: HTMLElement;
    private contentArea: HTMLElement;
    private glasspane: HTMLElement;
    private mousePoint: Point

    private heightOffset = 0; //relative to glasspane
    private currentPosition = 0; //relative to content / container
    private heightLimit = 5;     //minimum distance between parts labels
    private ghostsBottom = 0;

    constructor(protected readonly renderer: Renderer2, protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
    }

    ngOnInit() {
        this.resizerRef.nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            event.stopPropagation();
            this.editorContentService.getDocument().addEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().addEventListener('mouseup', this.onMouseUp);
            this.lowestPart = null;
                 
            this.contentArea = this.editorContentService.getContentArea();
            this.ghostContainers = this.editorContentService.querySelectorAll('.ghostcontainer'); //separate for part & comp types
            this.editorContent = this.editorContentService.querySelector('.content');  
            this.glasspane = this.editorContentService.getGlassPane();

            this.currentPosition = this.ghostContainers[0].offsetHeight;
            this.mousePoint = { x: event.pageX, y: event.pageY };

            //select lowest part
            let partHeight = 0;
            const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
            for (let index=0; index < ghostsList.length; index++) {
                const ghost = ghostsList.item(index);
                const ghostType = ghost.getAttribute('svy-ghosttype');
                if (ghostType == 'part') {
                    if (!this.lowestPart) {
                        this.lowestPart = ghost;
                    }
                    partHeight = (ghost as HTMLAnchorElement).offsetHeight;
                    const tmpTop = (ghost as HTMLElement).offsetTop;
                    const ghostTop = (this.lowestPart as HTMLElement).offsetTop
                    if (tmpTop > ghostTop) {
                        this.heightLimit = (this.lowestPart as HTMLElement).offsetTop + 5;
                        this.lowestPart = ghost;
                    }
                } else { 
                    this.ghostsBottom = Math.max(this. ghostsBottom, (ghost as HTMLElement).offsetTop + (ghost as HTMLElement).offsetHeight);
                }
            }
            this.heightOffset = this.ghostContainers[0].offsetTop + partHeight; //offset relative to glasspane

            this.dragingEvent = event;
            this.editorSession.getState().dragging = true;
            this.editorSession.registerAutoscroll(this);
        });
    }

    onMouseMove = (event: MouseEvent) => { 
        if (this.dragingEvent) {
            event.stopPropagation();
            const step = event.pageY - this.mousePoint.y;
            if ( step != 0 ) {                
                this.currentPosition += step;
                if (this.currentPosition >= this.heightLimit) {
                    for (let index = 0; index < this.ghostContainers.length; index++) {//components outside view became ghost places in a separate container
                        this.renderer.setStyle(this.ghostContainers[index], 'height', this.currentPosition +'px');
                    }                    
                    this.renderer.setStyle(this.editorContent, 'height', this.currentPosition  + 'px');
                    if (this.lowestPart) {
                        this.renderer.setStyle(this.lowestPart, 'top', this.currentPosition + 'px');
                    }
                    if (step > 0 && this.currentPosition + this.heightOffset > this.glasspane.offsetHeight) {
                        this.glasspane.style.height = this.currentPosition + this.heightOffset + 'px' ;
                    } 
                }
                this.mousePoint.y = event.pageY;
            } 
        }
    }

    onMouseUp = (event: MouseEvent) => {
        if (this.dragingEvent) {
            event.stopPropagation();

            this.editorContentService.getDocument().removeEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().removeEventListener('mouseup', this.onMouseUp);  
            
            if (this.currentPosition < this.heightLimit) {
                this.currentPosition = this.heightLimit;
            }
            if (this.lowestPart) {
                const changes = {};
                const id = this.lowestPart.getAttribute('svy-id');
                changes[id] = { 'y': this.currentPosition};
                this.editorSession.sendChanges(changes); 
            } 

            const changes = {};
            const id = this.editorContentService.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
            changes[id] = { 'height': this.currentPosition};
            this.editorSession.sendChanges(changes);

            const glasspaneHeight = Math.max((this.lowestPart ? this.currentPosition : this.ghostContainers[0].offsetHeight) + this.heightOffset ,  this.ghostsBottom);
            this.glasspane.style.height = glasspaneHeight + 'px';

            this.editorSession.getState().dragging = false;   
            this.dragingEvent = null;
            this.editorSession.unregisterAutoscroll(this);
        }
    }

    getAutoscrollLockId(): string {
        return 'resize-editor-height';
    }
 
    updateLocationCallback(_changeX: number, changeY: number) {
        if (this.currentPosition >= this.heightLimit) {
            for (let index = 0; index < this.ghostContainers.length; index++) {
                const ghostContainer = this.ghostContainers[index];
                this.renderer.setStyle(ghostContainer, 'height', this.currentPosition +'px');
            }
            this.renderer.setStyle(this.editorContent, 'height', this.currentPosition +'px');
            if (this.lowestPart) {
                this.renderer.setStyle(this.lowestPart, 'top', this.currentPosition + 'px');
            }
    
            if (this.currentPosition + this.heightOffset > this.glasspane.offsetHeight) {
                this.glasspane.style.height = this.currentPosition + this.heightOffset + 'px';
            }   
            this.currentPosition += changeY;
            this.contentArea.scrollTop += changeY;
        }
    }
}
