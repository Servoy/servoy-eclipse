import { DOCUMENT } from '@angular/common';
import {Component, Renderer2, ChangeDetectorRef, ChangeDetectionStrategy, Inject} from '@angular/core';
import {FormattingService} from 'servoy-public';
import {ServoyDefaultBaseField} from '../basefield';
@Component({
  selector: 'servoydefault-password',
  templateUrl: './password.html',
    changeDetection: ChangeDetectionStrategy.OnPush
})
export class ServoyDefaultPassword extends ServoyDefaultBaseField<HTMLInputElement> {
  constructor(renderer: Renderer2, cdRef: ChangeDetectorRef, formattingService: FormattingService, @Inject(DOCUMENT) doc: Document) {
    super(renderer, cdRef, formattingService, doc);
  }
}
