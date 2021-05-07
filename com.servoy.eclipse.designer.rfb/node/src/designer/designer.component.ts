import { Component, OnInit } from '@angular/core';
import { EditorSessionService } from './services/editorsession.service';

@Component({
    selector: 'app-designer',
    templateUrl: './designer.component.html',
    styleUrls: ['./designer.component.css']
})
export class DesignerComponent implements OnInit {
    constructor(protected readonly editorSession: EditorSessionService) {
    }

    ngOnInit() {
        this.editorSession.connect();
    }
}
