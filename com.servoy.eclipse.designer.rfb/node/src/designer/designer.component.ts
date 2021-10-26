import { Component, OnInit } from '@angular/core';
import { EditorSessionService } from './services/editorsession.service';
import { URLParserService } from 'src/designer/services/urlparser.service';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css']
})
export class DesignerComponent implements OnInit {
    constructor(public readonly editorSession: EditorSessionService, public urlParser: URLParserService) {
    }

    ngOnInit() {
        this.editorSession.connect();
    }
}
