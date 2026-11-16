import { Component, ElementRef, OnInit, Renderer2, ChangeDetectionStrategy, inject, viewChild } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { WidthResizer } from '../resize/widthresizer';

@Component({
    selector: 'designer-resizeeditorwidth',
    templateUrl: './resizeeditorwidth.component.html',
    styleUrls: ['./resizeeditorwidth.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResizeEditorWidthComponent implements OnInit, ISupportAutoscroll {
    readonly elementRef = viewChild.required<ElementRef<HTMLElement>>('element');

    private draggingEvent: MouseEvent | null = null;

    protected readonly renderer = inject(Renderer2);
    protected readonly editorSession = inject(EditorSessionService);
    private editorContentService = inject(EditorContentService);

    private resizer = new WidthResizer(this.renderer, this.editorSession, this.editorContentService);

    ngOnInit() {
        this.elementRef().nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            event.stopPropagation();
            this.draggingEvent = event;
            this.editorContentService.getDocument().addEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().addEventListener('mouseup', this.onMouseUp);
            this.resizer.start(event);
            this.editorSession.dragging.set(true);
            this.editorSession.registerAutoscroll(this);
        });
    }

    onMouseMove = (event: MouseEvent) => {
        if (this.draggingEvent) {
            event.stopPropagation();
            this.resizer.move(event);
        }
    }

    onMouseUp = (event: MouseEvent) => {
        if (this.draggingEvent) {
            event.stopPropagation();
            this.editorContentService.getDocument().removeEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().removeEventListener('mouseup', this.onMouseUp);

            this.resizer.end();

            this.editorSession.dragging.set(false);
            this.editorSession.unregisterAutoscroll(this);
            this.draggingEvent = null;
        }
    }

    getAutoscrollLockId(): string {
        return 'resize-editor-width';
    }

    updateLocationCallback(changeX: number, _changeY: number) {
        this.resizer.updateLocation(changeX);
    }
}
