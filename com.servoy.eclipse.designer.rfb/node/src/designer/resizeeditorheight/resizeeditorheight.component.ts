import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';

@Component({
    selector: 'designer-resizeeditorheight',
    templateUrl: './resizeeditorheight.component.html',
    styleUrls: ['./resizeeditorheight.component.css']
})
export class ResizeEditorHeightComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;
    startPosition: number;
    currentPosition = 0;
    heightLimit = 5;
    ghostLabel: Element;
    dragingEvent = null;
    ghostContainers: HTMLElement[];
    editorContent: HTMLElement;
    glassPaneElement: HTMLElement;

    constructor(protected readonly renderer: Renderer2, protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
    }

    ngOnInit() {
        this.elementRef.nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            this.editorContentService.getDocument().addEventListener('mousemove', this.mousemove);
            this.editorContentService.getDocument().addEventListener('mouseup', this.mouseup);
            this.dragingEvent = event;
            this.editorSession.getState().dragging = true;
            this.ghostLabel = null;
            //components placed in the outside view area will generate a second ghostview container
            this.ghostContainers = this.editorContentService.querySelectorAll('.ghostcontainer');
            this.editorContent = this.editorContentService.querySelector('.content');  
            this.glassPaneElement = this.editorContentService.querySelector('.contentframe-overlay');
            this.startPosition = event.pageY - this.ghostContainers[0].offsetHeight;
            this.currentPosition = this.startPosition;
            const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
            if (ghostsList.length <= 1) return; //contain only form; top limit is the upper margin of the form
            for (let index=0; index < ghostsList.length; index++) {
                const tmpGhost = ghostsList.item(index);
                const ghostType = tmpGhost.getAttribute('svy-ghosttype');
                if (ghostType == 'part') {
                    if (!this.ghostLabel) {
                        this.ghostLabel = tmpGhost;
                    }
                    let tmpTop = (tmpGhost as HTMLElement).offsetTop;
                    let ghostTop = (this.ghostLabel as HTMLElement).offsetTop
                    if (tmpTop > ghostTop) {
                        this.heightLimit = (this.ghostLabel as HTMLElement).offsetTop + 5;
                        this.ghostLabel = tmpGhost;
                    }
                } 
            }
        });
    }

    mousemove = (event: MouseEvent) => { 
        if (this.dragingEvent) {
            this.currentPosition = event.pageY-this.startPosition > this.heightLimit ? event.pageY-this.startPosition : this.heightLimit;

            for (let index = 0; index < this.ghostContainers.length; index++) {//components outside view became ghost places in a separate container
                this.renderer.setStyle(this.ghostContainers[index], 'height', this.currentPosition +'px');
            }
            this.renderer.setStyle(this.editorContent, 'height', this.currentPosition +'px');
            this.renderer.setStyle(this.glassPaneElement, 'height', this.currentPosition +'px');
            this.renderer.setStyle(this.ghostLabel, 'top', this.currentPosition + 'px');
        }
    }

    mouseup = (event: MouseEvent) => {
        this.currentPosition = event.pageY-this.startPosition > this.heightLimit ? event.pageY-this.startPosition : this.heightLimit;
        this.editorContentService.getDocument().removeEventListener('mousemove', this.mousemove);
        this.editorContentService.getDocument().removeEventListener('mouseup', this.mouseup);  
        let glasspaneLimit = this.heightLimit;

        const ghostsList = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
        for (let index=0; index < ghostsList.length; index++) {
            const ghost = ghostsList.item(index);
            const ghostType = ghost.getAttribute('svy-ghosttype');
            if (ghostType == 'comp') {
                glasspaneLimit = Math.max(glasspaneLimit, (ghost as HTMLElement).offsetTop + (ghost as HTMLElement).offsetHeight) + 5
            }
        }
        if (this.currentPosition < glasspaneLimit) {
            this.renderer.setStyle(this.glassPaneElement, 'height', this.currentPosition +'px');
        }

        let changes = {};
        let id = this.editorContentService.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
        changes[id] = { 'y': this.currentPosition};
        this.editorSession.sendChanges(changes); 
        if (this.ghostLabel) {
            id = this.ghostLabel.getAttribute('svy-id');
            changes[id] = { 'y': this.currentPosition};
            this.editorSession.sendChanges(changes); 
        } 
        this.dragingEvent = null;
        this.editorSession.getState().dragging = false;
    }
    
}
