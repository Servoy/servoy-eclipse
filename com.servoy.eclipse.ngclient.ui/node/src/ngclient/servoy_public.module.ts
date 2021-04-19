import { NgModule } from '@angular/core';

import { TooltipDirective } from './tooltip/tooltip.directive';
import { TooltipService } from './tooltip/tooltip.service';
import { MnemonicletterFilterPipe, NotNullOrEmptyPipe, HtmlFilterPipe} from './pipes/pipes';
import { DecimalkeyconverterDirective } from './utils/decimalkeyconverter.directive';
import { FormatFilterPipe } from './format/format.pipe';
import { EmptyValueFilterPipe } from './format/emptyvalue.pipe';
import { StartEditDirective } from './utils/startedit.directive';
import { ImageMediaIdDirective } from './utils/imagemediaid.directive';
import { AutosaveDirective } from './utils/autosave.directive';
import { FormatDirective } from './format/formatcontrolvalueaccessor.directive';
import { FormattingService } from './format/formatting.service';

@NgModule({
    declarations: [ TooltipDirective,
                    MnemonicletterFilterPipe,
                    NotNullOrEmptyPipe,
                    HtmlFilterPipe,
                    FormatDirective,
                    DecimalkeyconverterDirective,
                    FormatFilterPipe,
                    EmptyValueFilterPipe,
                    StartEditDirective,
                    ImageMediaIdDirective,
                    AutosaveDirective
                  ],
    imports: [],
    exports: [TooltipDirective,
              MnemonicletterFilterPipe,
              NotNullOrEmptyPipe,
              HtmlFilterPipe,
              FormatDirective,
              DecimalkeyconverterDirective,
              FormatFilterPipe,
              EmptyValueFilterPipe,
              StartEditDirective,
              ImageMediaIdDirective,
              AutosaveDirective
             ],
    providers: [ TooltipService, FormattingService ]
  })

export class ServoyPublicModule { }
