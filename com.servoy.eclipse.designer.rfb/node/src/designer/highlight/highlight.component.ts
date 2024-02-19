import { Component, Inject, OnInit, Renderer2, OnDestroy } from '@angular/core';
import { EditorSessionService, IShowHighlightChangedListener } from '../services/editorsession.service';
import { URLParserService } from '../services/urlparser.service';
import { EditorContentService, IContentMessageListener } from '../services/editorcontent.service';

@Component({
    selector: 'designer-highlight',
    templateUrl: './highlight.component.html',
    styleUrls: ['./highlight.component.css']
})
export class HighlightComponent implements IShowHighlightChangedListener, OnInit, IContentMessageListener, OnDestroy {
    highlightedComponent: Node;
    showPermanentHighlight = false;
    onMoveTimer: ReturnType<typeof setTimeout>;

    constructor(protected readonly editorSession: EditorSessionService, private readonly renderer: Renderer2, private urlParser: URLParserService, private editorContentService: EditorContentService) {
        this.editorSession.addHighlightChangedListener(this);
        this.editorContentService.addContentMessageListener(this);
    }

    ngOnInit(): void {
        this.editorContentService.getContentArea().addEventListener('mousemove', (event: MouseEvent) => this.onMouseMove(event));
    }

    ngOnDestroy(): void {
        this.editorContentService.removeContentMessageListener(this);
    }

    contentMessageReceived(id: string, data: { property: string }) {
        if (id === 'redrawDecorators') {
            this.highlightChanged(this.showPermanentHighlight);
        }
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
        let statusBarTxt = '';
        const point = { x: event.pageX, y: event.pageY };
        point.x = point.x - this.editorContentService.getLeftPositionIframe();
        point.y = point.y - this.editorContentService.getTopPositionIframe();
        const elements = this.editorContentService.getAllContentElements();
        const found = Array.from(elements).reverse().find((node) => {
            const position = node.getBoundingClientRect();
            if (position.x <= point.x && position.x + position.width >= point.x && position.y <= point.y && position.y + position.height >= point.y) {
                return node;
            }
        });
        if (!this.showPermanentHighlight && this.highlightedComponent != found) {
            if (found) {
                this.renderer.addClass(found, 'highlight_element');
            }
            if (this.highlightedComponent) {
                this.renderer.removeClass(this.highlightedComponent, 'highlight_element');
            }
        }
        if (found && !this.urlParser.isAbsoluteFormLayout()) {
            let parent = found;
            while (parent) {
                const id = parent.getAttribute('svy-id');
                if (id) {
                    let type = parent.getAttribute('svy-layoutname');
                    if (!type) type = parent.getAttribute('svy-formelement-type');
                    if (!type) type = parent.children[0].nodeName;
                    if (type && type.indexOf('.') >= 0) type = type.substring(type.indexOf('.') + 1);
                    const name = parent.getAttribute('svy-name');
                    if (name) type += ' [ ' + name + ' ] ';

                    statusBarTxt = type + (statusBarTxt.length == 0 ? '' : ' <strong>&nbsp;/&nbsp;</strong> ') + statusBarTxt;
                }
                parent = parent.parentElement;
            }
        }
        this.editorSession.setStatusBarText(statusBarTxt);
        this.highlightedComponent = found;
    }

    highlightChanged(showHighlight: boolean): void {
        this.showPermanentHighlight = showHighlight;
        this.editorContentService.executeOnlyAfterInit(() => {
            const elements = this.editorContentService.getAllContentElements();
            Array.from(elements).forEach((node, i) => {
                if (node.parentElement.classList.contains('svy-wrapper')) {
                    node = node.parentElement;
                } else if (node.parentElement.parentElement.classList.contains('svy-wrapper')) {
                    node = node.parentElement.parentElement;
                }
                if (showHighlight) {
                    this.renderer.addClass(node, 'highlight_element');
                }
                else {
                    this.renderer.removeClass(node, 'highlight_element');
                }
                if (i === Array.from(elements).length - 1) {
                    const lastElement = Array.from(node.closest('.svy-form').childNodes).filter(item => item.toString() != '[object Comment]').slice(-1)[0] as HTMLElement;
                    lastElement.style.marginBottom = '1px';
                }
            });
        });
    }
}
