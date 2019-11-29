import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';

import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';

import { BGPane } from './splitpane/bg_splitter/bg_pane.component';
import { BGSplitter } from './splitpane/bg_splitter/bg_splitter.component';

import { ServoyDefaultTextField } from './textfield/textfield';
import { ServoyDefaultTextArea } from './textarea/textarea';
import { ServoyDefaultButton } from './button/button';
import { ServoyDefaultLabel } from './label/label';
import { ServoyDefaultTabpanel } from './tabpanel/tabpanel';
import { ServoyDefaultTablesspanel } from './tabpanel/tablesspanel';
import { ServoyDefaultSplitpane } from './splitpane/splitpane';
import { ServoyDefaultCalendar } from './calendar/calendar';
import { ServoyDefaultCombobox } from './combobox/combobox';
import { ServoyDefaultTypeahead } from './typeahead/typeahead';
import { ServoyDefaultCheckGroup } from './checkgroup/checkgroup';
import { ServoyDefaultRadiogroup } from './radiogroup/radiogroup';
import { ServoyDefaultCheck } from './check/check';
import { ServoyDefaultPassword } from './password/password';
import { ServoyDefaultHtmlarea } from "./htmlarea/htmlarea";
import { ServoyDefaultRectangle } from './rectangle/rectangle';
import { ServoyDefaultHTMLView } from './htmlview/htmlview';
import { ServoyDefaultListBox } from './listbox/listbox';
import { ServoyDefaultImageMedia } from './imagemedia/imagemedia';

import { SabloModule } from '../sablo/sablo.module'

import { FormatFilterPipe, NotNullOrEmptyPipe, MnemonicletterFilterPipe, SvyFormat, FormattingService, I18NProvider, DecimalkeyconverterDirective, StartEditDirective, TooltipDirective, TooltipService} from '../ngclient/servoy_public'

import { OwlDateTimeModule,OWL_DATE_TIME_FORMATS} from 'ng-pick-datetime';
import { OwlMomentDateTimeModule ,OWL_MOMENT_DATE_TIME_FORMATS} from 'ng-pick-datetime-moment';

import { BrowserAnimationsModule } from '@angular/platform-browser/animations';
import {ServoyDefaultSpinner} from "./spinner/spinner";
import { AngularEditorModule } from '@kolkov/angular-editor';
import { UploadDirective } from "../ngclient/utils/upload.directive";

@NgModule({
  declarations: [
    ServoyDefaultTextField,
    ServoyDefaultTextArea,
    ServoyDefaultButton,
    ServoyDefaultLabel,
    ServoyDefaultTabpanel,
    ServoyDefaultTablesspanel,
    ServoyDefaultSplitpane,
    ServoyDefaultCalendar,
    ServoyDefaultCombobox,
    ServoyDefaultTypeahead,
    ServoyDefaultCheckGroup,
    ServoyDefaultRadiogroup,
    ServoyDefaultCheck,
    ServoyDefaultPassword,
    ServoyDefaultRectangle,
    ServoyDefaultHtmlarea,
    ServoyDefaultSpinner,
    ServoyDefaultHTMLView,
    ServoyDefaultListBox,
    ServoyDefaultImageMedia,
    FormatFilterPipe,
    MnemonicletterFilterPipe,
    NotNullOrEmptyPipe,
    SvyFormat,
    DecimalkeyconverterDirective,
    StartEditDirective,
    TooltipDirective,
    BGSplitter,
    BGPane,
    UploadDirective
  ],
  imports:[
    FormsModule,
    CommonModule,
    NgbModule,
    SabloModule,
    BrowserAnimationsModule,
    OwlDateTimeModule,
    OwlMomentDateTimeModule,
    AngularEditorModule
  ],
  exports: [
            ServoyDefaultTextField,
            ServoyDefaultTextArea,
            ServoyDefaultButton,
            ServoyDefaultLabel,
            ServoyDefaultTabpanel,
            ServoyDefaultTablesspanel,
            ServoyDefaultSplitpane,
            ServoyDefaultCalendar,
            ServoyDefaultCombobox,
            ServoyDefaultTypeahead,
            ServoyDefaultCheckGroup,
            ServoyDefaultRadiogroup,
            ServoyDefaultCheck,
            ServoyDefaultPassword,
            ServoyDefaultRectangle,
            ServoyDefaultHtmlarea,
            ServoyDefaultSpinner,
            ServoyDefaultHTMLView,
            ServoyDefaultListBox,
            ServoyDefaultImageMedia

  ],
  providers: [
              FormattingService,
              I18NProvider,
              TooltipService,
              {provide: OWL_DATE_TIME_FORMATS, useValue: OWL_MOMENT_DATE_TIME_FORMATS}
                     ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ]
})
export class ServoyDefaultComponentsModule { }
