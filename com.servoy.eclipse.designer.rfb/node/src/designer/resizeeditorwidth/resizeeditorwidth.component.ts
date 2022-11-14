import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-resizeeditorwidth',
    templateUrl: './resizeeditorwidth.component.html',
    styleUrls: ['./resizeeditorwidth.component.css']
})
export class ResizeEditorWidthComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;

    startPosition = 0;
    currentPosition = 0;
    widthLimit = 5;
    draggingEvent = null;
    ghostContainers: HTMLElement[];
    editorContent: HTMLElement;
    glassPaneElement: HTMLElement;

    constructor(protected readonly renderer: Renderer2, protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
    }

    ngOnInit() {
        this.elementRef.nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            this.editorContentService.getDocument().addEventListener('mousemove', this.mousemove);
            this.editorContentService.getDocument().addEventListener('mouseup', this.mouseup);
            //components placed outside view area will generate a second ghostcontainer
            this.ghostContainers = this.editorContentService.querySelectorAll('.ghostcontainer');
            this.editorContent = this.editorContentService.querySelector('.content');
            this.startPosition = event.pageX - this.ghostContainers[0].offsetWidth;
            this.currentPosition = this.startPosition;
            this.editorSession.getState().dragging = true;
            this.draggingEvent = event;
            this.glassPaneElement = this.editorContentService.querySelector('.contentframe-overlay');
        });
    }

    mousemove = (event: MouseEvent) => {
        if (this.draggingEvent) {
            this.currentPosition = event.pageX-this.startPosition > this.widthLimit ? event.pageX-this.startPosition : this.widthLimit;
            
            for (let index = 0; index < this.ghostContainers.length; index++) {
                this.renderer.setStyle(this.ghostContainers[index], 'width', this.currentPosition +'px');
            }
            this.renderer.setStyle(this.editorContent, 'width', this.currentPosition +'px'); 
        }
    }

    mouseup = (event: MouseEvent) => {
        this.currentPosition = event.pageX-this.startPosition > this.widthLimit ? event.pageX-this.startPosition : this.widthLimit;
        this.editorContentService.getDocument().removeEventListener('mousemove', this.mousemove);
        this.editorContentService.getDocument().removeEventListener('mouseup', this.mouseup);
        let glasspaneLimit = this.widthLimit;
        const changes = {};
        const id = document.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
        changes[id] = {
            'width': this.currentPosition
        };

        this.editorSession.sendChanges(changes);
        this.editorSession.getState().dragging = false;   
        this.draggingEvent = null;
    }
}
