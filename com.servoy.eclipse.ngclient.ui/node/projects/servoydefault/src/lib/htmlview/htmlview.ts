import { SabloTabseq, TooltipDirective, TrustAsHtmlPipe } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { Component, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseField} from  '../basefield';

@Component( {
    selector: 'servoydefault-htmlview',
    templateUrl: './htmlview.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, TooltipDirective, SabloTabseq, TrustAsHtmlPipe]
} )
export class ServoyDefaultHTMLView extends ServoyDefaultBaseField<HTMLDivElement> {
}

