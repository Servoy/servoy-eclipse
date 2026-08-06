import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { EditorSessionService } from '../services/editorsession.service';

@Component({
    selector: 'designer-status-bar',
    templateUrl: './statusbar.component.html',
    styleUrls: ['./statusbar.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class StatusBarComponent {
    protected readonly editorSession = inject(EditorSessionService);
}
