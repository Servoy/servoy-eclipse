import { FormatFilterPipe, ImageMediaIdDirective, MnemonicletterFilterPipe, SabloTabseq, TooltipDirective, TrustAsHtmlPipe } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { NgTemplateOutlet } from '@angular/common';
import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy, input } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-label',
    templateUrl: './label.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, TooltipDirective, SabloTabseq, FormatFilterPipe, MnemonicletterFilterPipe, TrustAsHtmlPipe, ImageMediaIdDirective, NgTemplateOutlet]
} )
export class ServoyDefaultLabel extends ServoyDefaultBaseLabel<HTMLDivElement> {

    readonly labelFor = input<any>(undefined);
}

