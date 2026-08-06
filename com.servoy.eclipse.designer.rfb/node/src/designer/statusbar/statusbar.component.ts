import { Component, AfterViewInit, OnDestroy, ChangeDetectionStrategy, ChangeDetectorRef, inject } from '@angular/core';
import { Subscription } from 'rxjs';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-status-bar',
    templateUrl: './statusbar.component.html',
    styleUrls: ['./statusbar.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class StatusBarComponent implements AfterViewInit, OnDestroy {
    statusText = '';
    editorStateSubscription!: Subscription;

    protected readonly editorSession = inject(EditorSessionService);
    private readonly cdr = inject(ChangeDetectorRef);

    ngAfterViewInit(): void {
        this.editorStateSubscription = this.editorSession.stateListener.subscribe(id => {
            if (id === 'statusText') {
                this.statusText = this.editorSession.getState().statusText;
                this.cdr.markForCheck();
            }
        });
    }

    ngOnDestroy(): void {
        this.editorStateSubscription.unsubscribe();
    }
}
