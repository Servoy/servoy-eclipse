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
import {ServoyDefaultCalendar} from './calendar/calendar';

import {SabloModule} from '../sablo/sablo.module'

import {FormatFilterPipe,MnemonicletterFilterPipe,SvyFormat,FormattingService,I18NProvider} from '../ngclient/servoy_public'

import {OwlDateTimeModule,OWL_DATE_TIME_FORMATS} from 'ng-pick-datetime';
import { OwlMomentDateTimeModule ,OWL_MOMENT_DATE_TIME_FORMATS} from 'ng-pick-datetime-moment';

import {BrowserAnimationsModule} from '@angular/platform-browser/animations';


@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultButton,
    ServoyDefaultLabel,
    ServoyDefaultTabpanel,
    ServoyDefaultTablesspanel,
    ServoyDefaultSplitpane,
    ServoyDefaultCalendar,
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
    BrowserAnimationsModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule
  ],
  exports: [
            ServoyDefaultTextField,
            ServoyDefaultButton,
            ServoyDefaultLabel,
            ServoyDefaultTabpanel,
            ServoyDefaultTablesspanel,
            ServoyDefaultSplitpane,
            ServoyDefaultCalendar
  ],
  providers: [FormattingService,
                      I18NProvider,
                      {provide: OWL_DATE_TIME_FORMATS, useValue: OWL_MOMENT_DATE_TIME_FORMATS}
                     ]
})
export class ServoyDefaultComponentsModule { }
