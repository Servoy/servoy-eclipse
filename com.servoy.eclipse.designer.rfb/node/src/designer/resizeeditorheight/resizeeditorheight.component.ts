import { Component, ElementRef, OnInit, Renderer2, ChangeDetectionStrategy, inject, viewChild } from '@angular/core';
import { EditorSessionService, ISupportAutoscroll } from '../services/editorsession.service';
import { EditorContentService } from '../services/editorcontent.service';
import { HeightResizer } from '../resize/heightresizer';

@Component({
    selector: 'designer-resizeeditorheight',
    templateUrl: './resizeeditorheight.component.html',
    styleUrls: ['./resizeeditorheight.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ResizeEditorHeightComponent implements OnInit, ISupportAutoscroll {
    readonly resizerRef = viewChild.required<ElementRef<HTMLElement>>('resizer');

    private dragingEvent: MouseEvent | null = null;

    protected readonly renderer = inject(Renderer2);
    protected readonly editorSession = inject(EditorSessionService);
    private editorContentService = inject(EditorContentService);

    private resizer = new HeightResizer(this.renderer, this.editorSession, this.editorContentService);

    ngOnInit() {
        this.resizerRef().nativeElement.addEventListener('mousedown', (event: MouseEvent) => {
            event.stopPropagation();
            this.editorContentService.getDocument().addEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().addEventListener('mouseup', this.onMouseUp);
            this.resizer.start(event);
            this.dragingEvent = event;
            this.editorSession.dragging.set(true);
            this.editorSession.registerAutoscroll(this);
        });
    }

    onMouseMove = (event: MouseEvent) => {
        if (this.dragingEvent) {
            event.stopPropagation();
            this.resizer.move(event);
        }
    }

    onMouseUp = (event: MouseEvent) => {
        if (this.dragingEvent) {
            event.stopPropagation();

            this.editorContentService.getDocument().removeEventListener('mousemove', this.onMouseMove);
            this.editorContentService.getDocument().removeEventListener('mouseup', this.onMouseUp);

            this.resizer.end();

            this.editorSession.dragging.set(false);
            this.dragingEvent = null;
            this.editorSession.unregisterAutoscroll(this);
        }
    }

    getAutoscrollLockId(): string {
        return 'resize-editor-height';
    }

    updateLocationCallback(_changeX: number, changeY: number) {
        this.resizer.updateLocation(changeY);
    }
}
