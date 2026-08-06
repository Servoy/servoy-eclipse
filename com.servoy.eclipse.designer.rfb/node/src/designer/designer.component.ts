import { Component, ElementRef, OnInit, Renderer2, ViewChild, ChangeDetectionStrategy, inject } from '@angular/core';
import { EditorSessionService } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class DesignerComponent implements OnInit {

    @ViewChild('contentArea', { static: false }) contentArea!: ElementRef<HTMLElement>;

    public readonly editorSession = inject(EditorSessionService);
    public urlParser = inject(URLParserService);
    protected readonly renderer = inject(Renderer2);

    ngOnInit() {
        this.editorSession.connect();
        this.editorSession.registerCallback.subscribe(value => {
            if (this.contentArea) this.renderer.listen(this.contentArea.nativeElement, value.event, value.function);
        })

        this.renderer.listen('window', 'mouseup', (event: MouseEvent) => {
            if (event.button > 2) { // special mouse buttons are not allowed
                event.preventDefault();  // Stop the browser from navigating back or forward
                event.stopPropagation(); // Stop further propagation of the event
            }
        });
    }
}