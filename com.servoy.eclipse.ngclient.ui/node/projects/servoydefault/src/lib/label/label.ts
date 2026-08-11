import { Component, ChangeDetectionStrategy, input } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultLabel extends ServoyDefaultBaseLabel<HTMLDivElement> {

    readonly labelFor = input<any>(undefined);
}

