
import { Component, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseField} from '../basefield';

@Component( {
    selector: 'servoydefault-textarea',
    templateUrl: './textarea.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultTextArea extends ServoyDefaultBaseField<HTMLTextAreaElement> {
}
