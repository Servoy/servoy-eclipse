import { Component, ChangeDetectionStrategy} from '@angular/core';

import {BaseTabpanel,Tab} from './basetabpanel';

@Component( {
    selector: 'servoydefault-tablesspanel',
    templateUrl: './tablesspanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: false
} )
export class ServoyDefaultTablesspanel extends BaseTabpanel {
}
