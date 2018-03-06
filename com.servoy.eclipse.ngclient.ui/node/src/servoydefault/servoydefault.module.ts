import { NgModule } from '@angular/core';
import {FormsModule}        from '@angular/forms';
import { CommonModule } from '@angular/common';

import {NgbModule} from '@ng-bootstrap/ng-bootstrap';

import { ServoyDefaultTextField } from './textfield/textfield';
import { ServoyDefaultButton } from './button/button';
import { ServoyDefaultLabel} from './label/label';
import { ServoyDefaultTabpanel} from './tabpanel/tabpanel';
import { ServoyDefaultTablesspanel} from './tabpanel/tablesspanel';

import {SabloModule} from '../sablo/sablo.module'

import {FormatFilterPipe,MnemonicletterFilterPipe} from '../ngclient/servoy_public'

@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultButton,
    ServoyDefaultLabel,
    ServoyDefaultTabpanel,
    ServoyDefaultTablesspanel,
    FormatFilterPipe,
    MnemonicletterFilterPipe
  ],
  imports:[
    FormsModule,
    CommonModule,
    NgbModule,
    SabloModule,
  ],
  exports: [
            ServoyDefaultTextField,
            ServoyDefaultButton,
            ServoyDefaultLabel,
            ServoyDefaultTabpanel,
            ServoyDefaultTablesspanel
  ],
  providers: []
})
export class ServoyDefaultComponentsModule { }
