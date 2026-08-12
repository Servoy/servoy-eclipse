import { Component, input, ChangeDetectionStrategy } from '@angular/core';
import { ServoyBaseComponent, ServoyPublicModule } from '@servoy/public';

@Component({
    selector: 'servoycore-errorbean',
    templateUrl: './error-bean.html',
    styles: ['.svy-errorbean { color: #a94442; }'],
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true,
    imports: [ServoyPublicModule]
})
export class ErrorBean extends ServoyBaseComponent<HTMLDivElement> {

    readonly error = input(undefined);
    readonly toolTipText = input<string>(undefined!);
}
