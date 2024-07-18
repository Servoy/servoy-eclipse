import { Component, ElementRef, OnInit, Renderer2, ViewChild } from '@angular/core';
import { EditorSessionService } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css']
})
export class DesignerComponent implements OnInit {

    @ViewChild('contentArea', { static: false }) contentArea: ElementRef<HTMLElement>;

    constructor(public readonly editorSession: EditorSessionService, 
                public urlParser: URLParserService, 
                protected readonly renderer: Renderer2) {
    }

    ngOnInit() {
        this.editorSession.connect();
        this.editorSession.registerCallback.subscribe(value => {
            if (this.contentArea) this.renderer.listen(this.contentArea.nativeElement, value.event, value.function);
        })
    }
}