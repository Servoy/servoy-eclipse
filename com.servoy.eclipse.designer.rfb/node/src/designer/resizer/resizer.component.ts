import { Component, ViewChild, ElementRef, OnInit, Renderer2, Inject } from '@angular/core';
import { DOCUMENT } from '@angular/common';

@Component({
    selector: 'designer-resizer',
    templateUrl: './resizer.component.html',
    styleUrls: ['./resizer.component.css']
})
export class ResizerComponent implements OnInit {
    @ViewChild('element', { static: true }) elementRef: ElementRef;

    constructor(protected readonly renderer: Renderer2, @Inject(DOCUMENT) private doc: Document) {
    }

    ngOnInit() {
        this.elementRef.nativeElement.addEventListener('mousedown', (event) => {
            this.doc.addEventListener('mousemove', this.mousemove);
            this.doc.addEventListener('mouseup', this.mouseup);
        });
    }

    mousemove = (event) => {
        const palette = this.doc.querySelector('.palette');
        this.renderer.setStyle(palette, 'width', event.pageX +'px');
    }

    mouseup = () => {
        this.doc.removeEventListener('mousemove', this.mousemove);
        this.doc.removeEventListener('mouseup', this.mouseup);
    }
}
