import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { Point } from './../mouseselection/mouseselection.component';

@Component({
    selector: 'designer-resizeeditorwidth',
    templateUrl: './resizeeditorwidth.component.html',
    styleUrls: ['./resizeeditorwidth.component.css'],
    standalone: false
})
export class ResizeEditorWidthComponent implements OnInit, ISupportAutoscroll {
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;

    private currentPosition = 0;
    private widthLimit = 5;
    private widthOffset = 0;
    private draggingEvent = null;
    private ghostContainers: HTMLElement[];
    private editorContent: HTMLElement;
    private contentArea: HTMLElement;
    private mousePoint: Point;
    private glasspane: HTMLElement;
    private ghostsRight = 0;

    constructor(protected readonly renderer: Renderer2, protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
    }

    ngOnInit() {
        this.elementRef.nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            event.stopPropagation();
            this.draggingEvent = event;
            this.editorContentService.getDocument().addEventListener('mousemove', (event) =>this.onMouseMove(event));
            this.editorContentService.getDocument().addEventListener('mouseup', (event)=>this.onMouseUp(event));
            //components placed outside view area will generate a second ghostcontainer
            this.ghostContainers = this.editorContentService.querySelectorAll('.ghostcontainer');
            this.editorContent = this.editorContentService.querySelector('.content');
            this.contentArea = this.editorContentService.getContentArea();
            this.glasspane = this.editorContentService.getGlassPane();

            this.currentPosition = this.ghostContainers[0].offsetWidth;
            let partWidth = 0;

            const ghostsList = this.contentArea.getElementsByClassName('ghost label');
            for (let index=0; index < ghostsList.length; index++) {
                const ghost = ghostsList.item(0);
                const ghostType = ghost.getAttribute('svy-ghosttype');
                if (ghostType == 'comp') {
                    this.ghostsRight = Math.max(this. ghostsRight, (ghost as HTMLElement).offsetLeft + (ghost as HTMLElement).offsetWidth);
                } else if (ghostType == 'part') {
                    partWidth = (ghost as HTMLElement).offsetWidth;
                }
            }

            this.widthOffset = this.ghostContainers[0].offsetLeft + partWidth; //offset relative to glasspane
            
            this.mousePoint = { x: event.pageX, y: event.pageY };
            this.editorSession.getState().dragging = true;
            this.editorSession.registerAutoscroll(this);
        });
    }

    onMouseMove = (event: MouseEvent) => {
        if (this.draggingEvent) {
            event.stopPropagation();
            const step = event.pageX - this.mousePoint.x;
            if ( step != 0 ) {
                this.currentPosition += step;
                if (this.currentPosition >= this.widthLimit) {
                    for (let index = 0; index < this.ghostContainers.length; index++) {
                        this.renderer.setStyle(this.ghostContainers[index], 'width', this.currentPosition +'px');
                    }
                    this.renderer.setStyle(this.editorContent, 'width', this.currentPosition +'px'); 
        
                    if (step > 0 && this.currentPosition + this.widthOffset > this.glasspane.offsetWidth) {
                        this.glasspane.style.width = this.currentPosition + this.widthOffset + 'px' ;
                    }
                }
                this.mousePoint.x = event.pageX;
            } 
        }
    }

    onMouseUp = (event: MouseEvent) => {
        if (this.draggingEvent) {
            event.stopPropagation();
            this.editorContentService.getDocument().removeEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().removeEventListener('mouseup', this.onMouseUp);

            if (this.currentPosition < this.widthLimit) {
                this.currentPosition = this.widthLimit;
            }
            const changes = {};
            const id = document.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
            changes[id] = {
                'width': this.currentPosition
            };
            this.glasspane.style.width = Math.max(this.currentPosition + this.widthOffset, this.ghostsRight) + 'px';
            this.editorSession.sendChanges(changes);
            
            this.editorSession.getState().dragging = false;   
            this.editorSession.unregisterAutoscroll(this);
            this.draggingEvent = null;
        }
    }

    getAutoscrollLockId(): string {
        return 'resize-editor-width';
    }

    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    updateLocationCallback(changeX: number, _changeY: number) {
        if (this.currentPosition >= this.widthLimit) {
            for (let index = 0; index < this.ghostContainers.length; index++) {
                this.renderer.setStyle(this.ghostContainers[index], 'width', this.currentPosition +'px');
            }
            this.renderer.setStyle(this.editorContent, 'width', this.currentPosition +'px'); 
    
            if (this.currentPosition + this.widthOffset > this.glasspane.offsetWidth) {
                this.glasspane.style.width = this.currentPosition + this.widthOffset + 'px';
            }
            this.currentPosition += changeX;
            this.contentArea.scrollLeft += changeX;
        }
    }
}
