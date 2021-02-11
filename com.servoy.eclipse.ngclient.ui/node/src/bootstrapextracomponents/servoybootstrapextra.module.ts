
import { NgModule } from '@angular/core';
import { ServoyBootstrapExtraBreadcrumbs } from './breadcrumbs/breadcrumbs';
import { CommonModule } from '@angular/common';

@NgModule({
    declarations: [
      ServoyBootstrapExtraBreadcrumbs
    ],
    providers: [],
    imports: [
      CommonModule
    ],
    exports: [
        ServoyBootstrapExtraBreadcrumbs
      ]
})
export class ServoyBootstrapExtraComponentsModule {}
