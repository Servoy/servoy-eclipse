
import { NgModule } from '@angular/core';
import {SvyChartJS} from './chart/chart';
import { ChartsModule } from 'ng2-charts';
 
@NgModule({
    declarations: [
        SvyChartJS
    ],
    providers: [],
    imports: [
        ChartsModule
    ],
    exports: [ 
        SvyChartJS
      ]
})
export class SvyChartJSModule {}
