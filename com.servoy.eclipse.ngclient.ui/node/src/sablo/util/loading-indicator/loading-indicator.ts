import { Component, ChangeDetectionStrategy } from '@angular/core';

@Component({
    selector: 'loading-indicator',
    templateUrl: './loading-indicator.html',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true
})
export class LoadingIndicatorComponent {}
