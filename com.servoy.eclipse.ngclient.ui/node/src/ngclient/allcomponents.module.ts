import { NgModule } from '@angular/core';

import { ServoyDefaultComponentsModule } from '../servoydefault/servoydefault.module';
import { ServoyBootstrapComponentsModule } from '@servoy/bootstrapcomponents';
import { ServoyBootstrapExtraComponentsModule } from '../bootstrapextracomponents/servoybootstrapextra.module';
import { ServoyExtraComponentsModule } from '../servoyextra/servoyextra.module';
import { SvyChartJSModule } from '../svychartjs/svychartjs.module';
/**
 * This module should be generated in the developer and when exporting a WAR
 * This will list all the component modules that can or will be used in a solution.
 */
@NgModule({

  imports: [
    ServoyDefaultComponentsModule,
    ServoyBootstrapComponentsModule,
    ServoyBootstrapExtraComponentsModule,
    SvyChartJSModule,
    ServoyExtraComponentsModule
  ],
  exports: [
    ServoyDefaultComponentsModule,
    ServoyBootstrapComponentsModule,
    ServoyBootstrapExtraComponentsModule,
    SvyChartJSModule,
    ServoyExtraComponentsModule
  ]
})
export class AllComponentsModule { }
