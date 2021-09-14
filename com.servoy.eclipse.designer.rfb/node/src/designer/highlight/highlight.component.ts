import { Component, Inject, Renderer2 } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService, IShowHighlightChangedListener } from '../services/editorsession.service';

@Component({
    selector: 'designer-highlight',
    templateUrl: './highlight.component.html',
    styleUrls: ['./highlight.component.css']
})
export class HighlightComponent implements IShowHighlightChangedListener {
    highlightedComponent: Node;

    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, private readonly renderer: Renderer2) {
        this.editorSession.addHighlightChangedListener(this);
    }

    ngOnInit(): void {
        let content = this.doc.querySelector('.content-area') as HTMLElement;
        content.addEventListener('mousemove', (event) => this.onMouseMove(event));
    }

    private onMouseMove(event: MouseEvent) {
        let point = { x: event.pageX, y: event.pageY };
        let frameElem = this.doc.querySelector('iframe');
        let frameRect = frameElem.getBoundingClientRect();
        point.x = point.x - frameRect.left;
        point.y = point.y - frameRect.top;
        let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
        let found = Array.from(elements).reverse().find((node) => {
            let position = node.getBoundingClientRect();
            if (position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                return node;
            }
        });
        if (this.highlightedComponent != found) {
            if (found) {
                this.renderer.addClass(found, "highlight_element");
            }
            if (this.highlightedComponent) {
                this.renderer.removeClass(this.highlightedComponent, "highlight_element");
            }
        }
        this.highlightedComponent = found;
    }

    highlightChanged(showHighlight: boolean): void {
        let frameElem = this.doc.querySelector('iframe');
        let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
        if (elements.length == 0 )   setTimeout(() => this.highlightChanged(showHighlight), 400);
        Array.from(elements).forEach((node) => {
            if (showHighlight) {
                this.renderer.addClass(node, "highlight_element");
            }
            else {
                this.renderer.removeClass(node, "highlight_element");
            }
        });
    }
}
