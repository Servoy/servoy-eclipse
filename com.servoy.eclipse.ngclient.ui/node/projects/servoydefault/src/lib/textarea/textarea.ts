import { SabloTabseq, StartEditDirective, TooltipDirective } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { Component, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseField} from '../basefield';

@Component( {
    selector: 'servoydefault-textarea',
    templateUrl: './textarea.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, TooltipDirective, SabloTabseq, StartEditDirective]
} )
export class ServoyDefaultTextArea extends ServoyDefaultBaseField<HTMLTextAreaElement> {
}
