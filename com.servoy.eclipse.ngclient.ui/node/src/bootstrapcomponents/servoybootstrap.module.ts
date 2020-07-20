
import { NgModule } from "@angular/core";
import { ServoyBootstrapBaseComponent } from "./bts_basecomp";
import { ServoyPublicModule } from "../ngclient/servoy_public.module";
import { SabloModule } from "../sablo/sablo.module";
import { ServoyBootstrapButton } from "./button/button";
import { ServoyBootstrapLabel } from "./label/label";
import { CommonModule } from "@angular/common";
import { ServoyBootstrapTextarea } from './textarea/textarea';
import { FormsModule } from "@angular/forms";
import { ServoyBootstrapChoicegroup } from './choicegroup/choicegroup';
import { ServoyBootstrapBasefield } from "./bts_basefield";
import { ServoyBootstrapCheckbox } from "./checkbox/checkbox";
import { ServoyBootstrapTextbox } from "./textbox/textbox";
import { ServoyBootstrapDatalabel } from "./datalabel/datalabel";
import { ServoyBootstrapBaseLabel } from "./bts_baselabel";
import { ServoyBootstrapList } from './list/list';
import { ServoyBootstrapSelect } from './select/select';
import { ServoyBootstrapAccordion } from './accordion/accordion';
import { ServoyBootstrapTypeahead } from './typeahead/typeahead';
import { ServoyBootstrapTabpanel } from './tabpanel/tabpanel';
import { ServoyBootstrapTablesspanel } from './tablesspanel/tablesspanel';
import { ServoyBootstrapCombobox } from './combobox/combobox';
import { ServoyBootstrapCalendar } from './calendar/calendar';
import { ServoyBootstrapCalendarinline } from './calendarinline/calendarinline';
import { DatalistPolyFill } from "./list/lib/purejs-datalist-polyfill/datalist.polyfill";
import { ShowDisplayValuePipe } from "./lib/showDisplayValue.pipe";
import { ServoyBootstrapImageMedia } from "./imagemedia/imagemedia";

@NgModule({
    declarations: [
      ServoyBootstrapImageMedia,
      ServoyBootstrapButton,
      ServoyBootstrapLabel,
      ServoyBootstrapTextarea,
      ServoyBootstrapChoicegroup,
      ServoyBootstrapCheckbox,
      ServoyBootstrapTextbox,
      ServoyBootstrapDatalabel,
      ServoyBootstrapList,
      ServoyBootstrapSelect,
      ServoyBootstrapAccordion,
      ServoyBootstrapTypeahead,
      ServoyBootstrapTabpanel,
      ServoyBootstrapTablesspanel,
      ServoyBootstrapCombobox,
      ServoyBootstrapCalendar,
      ServoyBootstrapCalendarinline,
      ShowDisplayValuePipe
    ],
    providers: [DatalistPolyFill, ShowDisplayValuePipe],
    imports: [
      ServoyPublicModule,
      SabloModule,
      CommonModule, 
      FormsModule
    ], 
    exports: [
      ServoyBootstrapImageMedia,
      ServoyBootstrapButton,
      ServoyBootstrapLabel,
      ServoyBootstrapTextarea,
      ServoyBootstrapChoicegroup,
      ServoyBootstrapCheckbox,
      ServoyBootstrapTextbox,
      ServoyBootstrapDatalabel,
      ServoyBootstrapList,
      ServoyBootstrapSelect,
      ServoyBootstrapAccordion,
      ServoyBootstrapTypeahead,
      ServoyBootstrapTabpanel,
      ServoyBootstrapTablesspanel,
      ServoyBootstrapCombobox,
      ServoyBootstrapCalendar,
      ServoyBootstrapCalendarinline]
})
export class ServoyBootstrapComponentsModule {}