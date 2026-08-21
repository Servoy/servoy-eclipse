import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SabloTabseq } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy} from '@angular/core';

import {BaseTabpanel,Tab} from './basetabpanel';

@Component( {
    selector: 'servoydefault-tablesspanel',
    templateUrl: './tablesspanel.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, SabloTabseq, NgbModule]
} )
export class ServoyDefaultTablesspanel extends BaseTabpanel {

    containerStyle: { [property: string]: any } = { overflow: 'auto' };

    getContainerStyle(): { [property: string]: any } {
        this.containerStyle['border'] = this.borderStyle;
        this.applyOverflowFromForm(this.containerStyle);
        return this.containerStyle;
    }
}
