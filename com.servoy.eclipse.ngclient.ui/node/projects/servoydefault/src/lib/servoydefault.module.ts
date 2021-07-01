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
import { ServoyDefaultRadio } from './radio/radio';
import { ServoyDefaultRadiogroup } from './radiogroup/radiogroup';
import { ServoyDefaultCheck } from './check/check';
import { ServoyDefaultPassword } from './password/password';
import { ServoyDefaultHtmlarea } from './htmlarea/htmlarea';
import { ServoyDefaultRectangle } from './rectangle/rectangle';
import { ServoyDefaultHTMLView } from './htmlview/htmlview';
import { ServoyDefaultListBox } from './listbox/listbox';
import { ServoyDefaultImageMedia } from './imagemedia/imagemedia';
import {ServoyDefaultSpinner} from './spinner/spinner';

import {ChoiceElementDirective} from './basechoice';

import { ServoyPublicModule, SpecTypesService } from '@servoy/public';

import { OwlDateTimeModule, OwlNativeDateTimeModule } from '@danielmoncada/angular-datetime-picker';

import { Select2Module } from 'ng-select2-component';
import { Tab } from './tabpanel/basetabpanel';
import { EditorModule , TINYMCE_SCRIPT_SRC} from '@tinymce/tinymce-angular';

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
    ServoyDefaultRadio,
    ServoyDefaultRadiogroup,
    ServoyDefaultCheck,
    ServoyDefaultPassword,
    ServoyDefaultRectangle,
    ServoyDefaultHtmlarea,
    ServoyDefaultSpinner,
    ServoyDefaultHTMLView,
    ServoyDefaultListBox,
    ServoyDefaultImageMedia,
    ChoiceElementDirective,
    BGSplitter,
    BGPane
  ],
  imports: [
    FormsModule,
    CommonModule,
    NgbModule,
    OwlDateTimeModule,
    OwlNativeDateTimeModule,
    ServoyPublicModule,
    Select2Module,
    EditorModule
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
            ServoyDefaultRadio,
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
              { provide: TINYMCE_SCRIPT_SRC, useValue: 'tinymce/tinymce.min.js' }
             ],
  schemas: [
    CUSTOM_ELEMENTS_SCHEMA
  ]
})
export class ServoyDefaultComponentsModule {
    constructor( specTypesService: SpecTypesService ) {
        specTypesService.registerType('servoydefault-tabpanel.tab', Tab);
        specTypesService.registerType('servoydefault-splitpane.tab', Tab);
    }
}
