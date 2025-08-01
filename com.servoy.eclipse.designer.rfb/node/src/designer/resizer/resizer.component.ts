import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject, DOCUMENT } from '@angular/core';

import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-resizer',
    templateUrl: './resizer.component.html',
    styleUrls: ['./resizer.component.css'],
    standalone: false
})
export class ResizerComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef: ElementRef<HTMLElement>;

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
        const palette = this.doc.querySelector('.palette');
        this.renderer.setStyle(palette, 'width', event.pageX +'px');
    }

    mouseup = () => {
        this.doc.removeEventListener('mousemove', this.mousemove);
        this.doc.removeEventListener('mouseup', this.mouseup);
        this.editorSession.setDragging(false);
    }
}
