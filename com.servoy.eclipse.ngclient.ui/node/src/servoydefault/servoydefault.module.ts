import { NgModule } from '@angular/core';
import {FormsModule}        from '@angular/forms';
import { CommonModule } from '@angular/common';

import {NgbModule} from '@ng-bootstrap/ng-bootstrap';

import {BGPane} from './splitpane/bg_splitter/bg_pane.component';
import {BGSplitter} from './splitpane/bg_splitter/bg_splitter.component';

import { ServoyDefaultTextField } from './textfield/textfield';
import { ServoyDefaultButton } from './button/button';
import { ServoyDefaultLabel} from './label/label';
import { ServoyDefaultTabpanel} from './tabpanel/tabpanel';
import { ServoyDefaultTablesspanel} from './tabpanel/tablesspanel';
import {ServoyDefaultSplitpane} from './splitpane/splitpane';

import {SabloModule} from '../sablo/sablo.module'

import {FormatFilterPipe,MnemonicletterFilterPipe,SvyFormat} from '../ngclient/servoy_public'

@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultButton,
    ServoyDefaultLabel,
    ServoyDefaultTabpanel,
    ServoyDefaultTablesspanel,
    ServoyDefaultSplitpane,
    FormatFilterPipe,
    MnemonicletterFilterPipe,
    SvyFormat,
	BGSplitter,
    BGPane
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
            ServoyDefaultTablesspanel,
            ServoyDefaultSplitpane
  ],
  providers: []
})
export class ServoyDefaultComponentsModule { }
