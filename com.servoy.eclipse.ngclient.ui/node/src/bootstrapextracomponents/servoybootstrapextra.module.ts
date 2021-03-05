
import { NgModule } from '@angular/core';
import { ServoyBootstrapExtraBreadcrumbs } from './breadcrumbs/breadcrumbs';
import { MenuItem, ServoyBootstrapExtraNavbar, SvyAttributes } from './navbar/navbar';
import { CommonModule } from '@angular/common';
import { ServoyPublicModule } from '../ngclient/servoy_public.module';
import { SabloModule } from '../sablo/sablo.module';
import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { SpecTypesService } from '../sablo/spectypes.service';

@NgModule({
    declarations: [
      ServoyBootstrapExtraBreadcrumbs,
      ServoyBootstrapExtraNavbar,
      SvyAttributes
    ],
    providers: [],
    imports: [
      CommonModule,
      ServoyPublicModule,
      SabloModule,
      NgbModule,
      FormsModule
    ],
    exports: [
        ServoyBootstrapExtraBreadcrumbs,
        ServoyBootstrapExtraNavbar,
        SvyAttributes
      ]
})
export class ServoyBootstrapExtraComponentsModule {
      constructor( specTypesService: SpecTypesService ) {
         specTypesService.registerType('bootstrapextracomponents-navbar.menuItem', MenuItem);
    }
}
