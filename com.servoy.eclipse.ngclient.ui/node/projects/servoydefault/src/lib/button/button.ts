import { FormatFilterPipe, ImageMediaIdDirective, MnemonicletterFilterPipe, SabloTabseq, TooltipDirective, TrustAsHtmlPipe } from '@servoy/public';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Component, ChangeDetectionStrategy } from '@angular/core';

import {ServoyDefaultBaseLabel} from  '../baselabel';

@Component( {
    selector: 'servoydefault-button',
    templateUrl: './button.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    standalone: true,
    imports: [CommonModule, FormsModule, TooltipDirective, SabloTabseq, FormatFilterPipe, MnemonicletterFilterPipe, TrustAsHtmlPipe, ImageMediaIdDirective]
} )
export class ServoyDefaultButton extends ServoyDefaultBaseLabel<HTMLButtonElement> {
}

