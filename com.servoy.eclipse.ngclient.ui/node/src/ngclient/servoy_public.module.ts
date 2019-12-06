import { NgModule } from '@angular/core';

import { TooltipDirective } from './tooltip/tooltip.directive'
import { TooltipService } from './tooltip/tooltip.service'

@NgModule({
    declarations: [ TooltipDirective ],
    imports: [],
    exports: [ TooltipDirective ],
    providers: [ TooltipService ]
  })

export class ServoyPublicModule { }