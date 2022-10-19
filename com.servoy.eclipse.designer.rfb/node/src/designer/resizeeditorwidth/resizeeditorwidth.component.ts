import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-resizeeditorwidth',
    templateUrl: './resizeeditorwidth.component.html',
    styleUrls: ['./resizeeditorwidth.component.css']
})
export class ResizeEditorWidthComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;
    changes = {};
    diffWidth: number;

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
        
        this.calculateDiffWidth(event.pageX);
        
        this.renderer.setStyle(container, 'width', event.pageX-this.diffWidth +'px');
        this.renderer.setStyle(content, 'width', event.pageX-this.diffWidth +'px');
        
        this.changes[id] = {
			'width': event.pageX-this.diffWidth
        };
        
    }

    mouseup = () => {
        this.doc.removeEventListener('mousemove', this.mousemove);
        this.doc.removeEventListener('mouseup', this.mouseup);
        this.editorSession.setDragging(false);     
        this.editorSession.sendChanges(this.changes);
    }
    
    private calculateDiffWidth(pageX: number) {
        if (!this.diffWidth) {
            const container: HTMLElement = this.doc.querySelector('.ghostcontainer');
            this.diffWidth = pageX - container.offsetWidth;
        }
    }
}
