import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-resizeeditorheight',
    templateUrl: './resizeeditorheight.component.html',
    styleUrls: ['./resizeeditorheight.component.css']
})
export class ResizeEditorHeightComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;
    diffHeight: number;

    constructor(protected readonly renderer: Renderer2, @Inject(DOCUMENT) private doc: Document, protected readonly editorSession: EditorSessionService) {
    }

    ngOnInit() {
        this.elementRef.nativeElement.addEventListener('mousedown', () => {
            this.doc.addEventListener('mousemove', this.mousemove);
            this.doc.addEventListener('mouseup', this.mouseup);
            this.editorSession.setDragging(true);
        });
    }

    mousemove = (event: MouseEvent) => {
		const id = document.querySelector('.ghost[svy-ghosttype="form"]').getAttribute('svy-id');
		const container: HTMLElement = this.doc.querySelector('.ghostcontainer');
        const content: HTMLElement = this.doc.querySelector('.content');
        
        this.calculateDiffHeight(event.pageY);
        
        this.renderer.setStyle(container, 'height', event.pageY-this.diffHeight +'px');
        this.renderer.setStyle(content, 'height', event.pageY-this.diffHeight +'px');
        
    }

    mouseup = (event: MouseEvent) => {
        this.doc.removeEventListener('mousemove', this.mousemove);
        this.doc.removeEventListener('mouseup', this.mouseup);
        this.editorSession.setDragging(false);    
        let changes = {};
        const id = document.getElementById('ghostBody').getAttribute('svy-id');
        changes[id] = {
			'y': event.pageY-this.diffHeight
        };
        this.editorSession.sendChanges(changes); 
    }
    
    private calculateDiffHeight(pageY: number) {
        if (!this.diffHeight) {
            const container: HTMLElement = this.doc.querySelector('.ghostcontainer');
            this.diffHeight = pageY - container.offsetHeight;
        }
    }
}
