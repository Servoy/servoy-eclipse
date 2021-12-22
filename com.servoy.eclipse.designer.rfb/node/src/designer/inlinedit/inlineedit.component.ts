import { DOCUMENT } from '@angular/common';
import { AfterViewInit, Component, ElementRef, HostListener, Inject, OnInit, Renderer2, ViewChild } from '@angular/core';
import { DesignerUtilsService } from '../services/designerutils.service';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-inline-edit',
    templateUrl: './inlineedit.component.html'
})
export class InlineEditComponent implements AfterViewInit {

    @ViewChild('inlineedit', { static: true }) elementRef: ElementRef<HTMLElement>;
    showDirectEdit = false;
    keyupListener: () => void;
    keydownListener: () => void;
    blurListener: () => void;

    constructor(protected readonly editorSession: EditorSessionService, private readonly designerUtilsService: DesignerUtilsService,
        @Inject(DOCUMENT) private doc: Document, protected readonly renderer: Renderer2) {
    }

    ngAfterViewInit(): void {
        this.editorSession.registerCallback.next({event: 'dblclick', function: this.onDoubleClick});
        // const contentArea = this.doc.querySelector('.content-area');
        // this.renderer.listen(contentArea, 'dblclick', this.onDoubleClick);
    } 

    onDoubleClick = (event: MouseEvent) => {
        const selection = this.editorSession.getSelection();
        if (selection && selection.length > 0) {
            var eventNode = this.designerUtilsService.getNode(this.doc, event);
            if (eventNode) {
                for (let i = 0; i < selection.length; i++) {
                    const node = selection[i];
                    if (eventNode.getAttribute("svy-id") === node) {
                        // const directEditProperty = eventNode.getAttribute("directEditPropertyName");
                        const directEditProperty = 'text';
                        if (directEditProperty) {
                            this.editorSession.getComponentPropertyWithTags(node, directEditProperty).then((propertyValue: string) => {
                                if (eventNode.clientHeight === 0 && eventNode.clientWidth === 0 && eventNode.firstElementChild) {
                                    eventNode = eventNode.firstElementChild;
                                }
                                const absolutePoint = this.designerUtilsService.convertToAbsolutePoint(this.doc, {
                                    x: eventNode.getBoundingClientRect().left,
                                    y: eventNode.getBoundingClientRect().top
                                });
                                let newAbsolutePoint = {
                                    x: absolutePoint.x,
                                    y: absolutePoint.y,
                                    width: eventNode.getBoundingClientRect().right - eventNode.getBoundingClientRect().left,
                                    height: eventNode.getBoundingClientRect().bottom - eventNode.getBoundingClientRect().top
                                }
                                this.handleDirectEdit(node, newAbsolutePoint, directEditProperty, propertyValue);
                            });
                            break;
                        }
                    }
                }
            }
        }
    }

    handleDirectEdit(node: string, absolutePoint: { x: number; y: number; width: number, height: number}, directEditProperty: string, propertyValue: string) {
        this.showDirectEdit = true;
        const changes = {};
        const applyValue = () => {
            this.showDirectEdit = false;
            const newValue = this.elementRef.nativeElement.textContent;
            const oldValue = propertyValue;
            if (oldValue != newValue && !(oldValue === null && newValue === "")) {
                const value = {};
                value[directEditProperty] = newValue;
                changes[node] = value;
                this.editorSession.sendChanges(changes);
            } 
            this.editorSession.setInlineEditMode(false);
        }
        this.editorSession.setInlineEditMode(true);
        this.renderer.setProperty(this.elementRef.nativeElement, 'innerHTML', propertyValue);
        this.renderer.setStyle(this.elementRef.nativeElement, 'display', 'block');
        this.renderer.setStyle(this.elementRef.nativeElement, 'left', absolutePoint.x);
        this.renderer.setStyle(this.elementRef.nativeElement, 'top', absolutePoint.y);
        this.renderer.setStyle(this.elementRef.nativeElement, 'width', absolutePoint.width);
        this.renderer.setStyle(this.elementRef.nativeElement, 'height', absolutePoint.height);

        this.keyupListener = this.renderer.listen(this.elementRef.nativeElement, 'keyup', (event: KeyboardEvent) => {
            if (event.key === 'Escape') {
                this.renderer.setStyle(this.elementRef.nativeElement, 'display', 'none');
                this.showDirectEdit = false;
                this.editorSession.setInlineEditMode(false);
            }
            if (event.key === 'Enter') {
                applyValue();
            }
            if (event.key == 'Delete') {
                return false;
            }
        });
        this.keydownListener = this.renderer.listen(this.elementRef.nativeElement, 'keydown', (event: KeyboardEvent) => {
            if (event.key === 'Backspace') {
                event.stopPropagation();
            }
            if (event.key === 'a' && event.ctrlKey) {
                // TODO: find an alternative for execCommand
                this.doc.execCommand('selectAll', false, null);
            }
        });
        this.blurListener = this.renderer.listen(this.elementRef.nativeElement, 'blur', (event: Event) => {
            applyValue();
        });
        this.elementRef.nativeElement.focus();
    }

}

