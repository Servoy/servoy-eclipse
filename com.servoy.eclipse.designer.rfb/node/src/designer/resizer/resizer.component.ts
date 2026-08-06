import { Component, ViewChild, ElementRef, OnInit, Renderer2, DOCUMENT, ChangeDetectionStrategy, inject } from '@angular/core';

import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-resizer',
    templateUrl: './resizer.component.html',
    styleUrls: ['./resizer.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResizerComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef!: ElementRef<HTMLElement>;

    protected readonly renderer = inject(Renderer2);
    private doc = inject(DOCUMENT);
    protected readonly editorSession = inject(EditorSessionService);

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
