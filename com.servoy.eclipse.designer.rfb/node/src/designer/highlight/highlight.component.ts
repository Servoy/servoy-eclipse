import { Component, Inject, Renderer2 } from '@angular/core';
import { DOCUMENT } from '@angular/common';
import { EditorSessionService, IShowHighlightChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';

@Component({
    selector: 'designer-highlight',
    templateUrl: './highlight.component.html',
    styleUrls: ['./highlight.component.css']
})
export class HighlightComponent implements IShowHighlightChangedListener {
    highlightedComponent: Node;
    showPermanentHighlight: boolean = false;
    onMoveTimer: any;

    constructor(protected readonly editorSession: EditorSessionService, @Inject(DOCUMENT) private doc: Document, private readonly renderer: Renderer2, private urlParser: URLParserService) {
        this.editorSession.addHighlightChangedListener(this);
    }

    ngOnInit(): void {
        let content = this.doc.querySelector('.content-area') as HTMLElement;
        content.addEventListener('mousemove', (event) => this.onMouseMove(event));
    }

    private onMouseMove(event: MouseEvent) {
        if (this.onMoveTimer) {
            clearTimeout(this.onMoveTimer);
        }
        this.onMoveTimer = setTimeout(() => {
            this.drawHighlightOnMove(event);
        }, 300);

    }

    private drawHighlightOnMove(event: MouseEvent) {
        let statusBarTxt = "";
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
        if (!this.showPermanentHighlight && this.highlightedComponent != found) {
            if (found) {
                this.renderer.addClass(found, "highlight_element");
            }
            if (this.highlightedComponent) {
                this.renderer.removeClass(this.highlightedComponent, "highlight_element");
            }
        }
        if (found && !this.urlParser.isAbsoluteFormLayout()) {
            let parent = found;
            while (parent) {
                let id = parent.getAttribute('svy-id');
                if (id) {
                    let type = parent.getAttribute('svy-layoutname');
                    if (!type) type = parent.getAttribute('svy-formelement-type');
                    if (!type) type = parent.children[0].nodeName;
                    if (type && type.indexOf(".") >= 0) type = type.substring(type.indexOf(".") + 1);
                    let name = parent.getAttribute('svy-name');
                    if (name) type += ' [ ' + name + ' ] ';

                    statusBarTxt = type + (statusBarTxt.length == 0 ? "" : ' <strong>&nbsp;/&nbsp;</strong> ') + statusBarTxt;
                }
                parent = parent.parentElement;
            }
        }
        this.editorSession.setStatusBarText(statusBarTxt);
        this.highlightedComponent = found;
    }

    highlightChanged(showHighlight: boolean): void {
        this.showPermanentHighlight = showHighlight;
        let frameElem = this.doc.querySelector('iframe');
        let elements = frameElem.contentWindow.document.querySelectorAll('[svy-id]');
        if (elements.length == 0) setTimeout(() => this.highlightChanged(showHighlight), 400);
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
