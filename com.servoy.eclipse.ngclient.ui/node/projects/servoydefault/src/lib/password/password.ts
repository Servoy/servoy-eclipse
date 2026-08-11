
import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ServoyDefaultBaseField} from '../basefield';
@Component({
    selector: 'servoydefault-password',
    templateUrl: './password.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
})
export class ServoyDefaultPassword extends ServoyDefaultBaseField<HTMLInputElement> {
}
