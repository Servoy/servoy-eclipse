import { SabloTabseq, TooltipDirective } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import {Component, ChangeDetectionStrategy} from '@angular/core';
import {ServoyDefaultBaseField} from '../basefield';
@Component({
    selector: 'servoydefault-password',
    templateUrl: './password.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, TooltipDirective, SabloTabseq]
})
export class ServoyDefaultPassword extends ServoyDefaultBaseField<HTMLInputElement> {
}
