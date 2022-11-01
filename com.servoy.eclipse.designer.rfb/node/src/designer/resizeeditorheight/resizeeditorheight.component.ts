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
    position: number;
    topLimit = 0;
    ghostLabel: Element;

    constructor(protected readonly renderer: Renderer2, protected readonly editorSession: EditorSessionService, private editorContentService: EditorContentService) {
    }

    ngOnInit() {
        this.elementRef.nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            this.editorContentService.getContentArea().addEventListener('mousemove', this.mousemove);
            this.editorContentService.getContentArea().addEventListener('mouseup', this.mouseup);
            this.editorSession.setDragging(true);
            this.position = event.pageY - (this.editorContentService.querySelector('.ghostcontainer') as HTMLElement).offsetHeight;
            // title / header / body / footer / form
            // form ghost is always present; position is always initialized to the ghost's top before form ghost
            const ghostsLabels = this.editorContentService.getContentArea().getElementsByClassName('ghost label');
            const lastIndex = ghostsLabels.length - 2;//index of the last visible ghost (if present)
            if (ghostsLabels.length < 0) {//no visible parts in form
                this.ghostLabel = null;
                this.topLimit = 5;
            } else {
                this.ghostLabel = ghostsLabels[lastIndex ]; //select the last visible ghost
                this.topLimit = lastIndex > 0 ? (ghostsLabels[lastIndex - 1] as HTMLElement).offsetTop + 5 : 5;//the previous ghost is the upper limit
            }
            
        });
    }

    mousemove = (event: MouseEvent) => { 
        const topPosition = event.pageY-this.position > this.topLimit ? event.pageY-this.position : this.topLimit;
        const container: HTMLElement = this.editorContentService.querySelector('.ghostcontainer');
        const content: HTMLElement = this.editorContentService.querySelector('.content');    
        this.renderer.setStyle(container, 'height', topPosition +'px');
        this.renderer.setStyle(content, 'height', topPosition +'px');
        this.renderer.setStyle(this.ghostLabel, 'top', topPosition + 'px');
    }

    mouseup = (event: MouseEvent) => {
        const topPosition = event.pageY-this.position > this.topLimit ? event.pageY-this.position : this.topLimit;
        this.editorContentService.getContentArea().removeEventListener('mousemove', this.mousemove);
        this.editorContentService.getContentArea().removeEventListener('mouseup', this.mouseup);
        this.editorSession.setDragging(false);    

        let changes = {};
        let id = this.editorContentService.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
        changes[id] = { 'y': topPosition};
        this.editorSession.sendChanges(changes); 
        if (this.ghostLabel) {
            id = this.ghostLabel.getAttribute('svy-id');
            changes[id] = { 'y': topPosition};
            this.editorSession.sendChanges(changes); 
        } 
    }
}
