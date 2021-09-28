import { Component, AfterViewInit, OnDestroy } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-status-bar',
    templateUrl: './statusbar.component.html',
    styleUrls: ['./statusbar.component.css']
})
export class StatusBarComponent implements AfterViewInit, OnDestroy {
    statusText: string = "";
    editorStateSubscription: any;

    constructor(protected readonly editorSession: EditorSessionService) {
    }

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'statusText') {
                this.statusText = this.editorSession.getState().statusText;
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
    }
}
