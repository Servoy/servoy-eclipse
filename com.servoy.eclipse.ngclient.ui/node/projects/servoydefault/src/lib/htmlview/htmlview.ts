
import { Component, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-htmlview',
    templateUrl: './htmlview.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultHTMLView extends ServoyDefaultBaseField<HTMLDivElement> {
}

