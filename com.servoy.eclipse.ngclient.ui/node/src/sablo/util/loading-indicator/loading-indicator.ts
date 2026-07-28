import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'loading-indicator',
    templateUrl: './loading-indicator.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: false
})
export class LoadingIndicatorComponent {}
