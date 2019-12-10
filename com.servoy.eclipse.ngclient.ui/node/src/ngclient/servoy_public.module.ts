import { NgModule } from '@angular/core';

import { TooltipDirective } from './tooltip/tooltip.directive'
import { TooltipService } from './tooltip/tooltip.service'
import { MnemonicletterFilterPipe, NotNullOrEmptyPipe} from './pipes/pipes'
import { SvyFormat } from './format/format.directive'
import { DecimalkeyconverterDirective } from './utils/decimalkeyconverter.directive'
import { FormatFilterPipe } from './format/format.pipe'
import { StartEditDirective } from './utils/startedit.directive'

@NgModule({
    declarations: [ TooltipDirective,
                    MnemonicletterFilterPipe,
                    NotNullOrEmptyPipe,
                    SvyFormat,
                    DecimalkeyconverterDirective,
                    FormatFilterPipe,
                    StartEditDirective ],
    imports: [],
    exports: [TooltipDirective,
              MnemonicletterFilterPipe,
              NotNullOrEmptyPipe,
              SvyFormat,
              DecimalkeyconverterDirective,
              FormatFilterPipe,
              StartEditDirective ],
    providers: [ TooltipService ]
  })

export class ServoyPublicModule { }