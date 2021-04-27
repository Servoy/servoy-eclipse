
import { NgModule } from '@angular/core';
import { ServoyBootstrapExtraBreadcrumbs } from './breadcrumbs/breadcrumbs';
import { ServoyBootstrapExtraButtonsGroup } from './buttonsgroup/buttonsgroup';
import { ServoyBootstrapExtraBadge } from './badge/badge';
import { ServoyBootstrapExtraInputGroup, SvyAttributesInputGroup, AddOn, AddOnButton } from './inputgroup/inputgroup';
import { ServoyBootstrapExtraRating } from './rating/rating';
import { ServoyBootstrapExtraProgressBar } from './progressbar/progressbar';
import {ServoyBootstrapExtraDropdown, MenuItem as dropdown_MenuItem} from './dropdown/dropdown';
import { ServoyBootstrapExtraCarousel, Slide } from './carousel/carousel';
import { MenuItem, ServoyBootstrapExtraNavbar, SvyAttributes } from './navbar/navbar';
import { CommonModule } from '@angular/common';
import { ServoyPublicModule } from '@servoy/public';
import { SabloModule } from '../sablo/sablo.module';
import { NgbModule }  from '@ng-bootstrap/ng-bootstrap';
import { FormsModule } from '@angular/forms';
import { SpecTypesService } from '../sablo/spectypes.service';
import { ServoyBootstrapExtraSwitch } from './switch/switch';
import { JwBootstrapSwitchNg2Module } from 'jw-bootstrap-switch-ng2';

@NgModule({
    declarations: [
      ServoyBootstrapExtraBreadcrumbs,
      ServoyBootstrapExtraNavbar,
      ServoyBootstrapExtraCarousel,
      ServoyBootstrapExtraBadge,
      ServoyBootstrapExtraButtonsGroup,
      ServoyBootstrapExtraInputGroup,
      SvyAttributesInputGroup,
      ServoyBootstrapExtraRating,
      ServoyBootstrapExtraProgressBar,
      ServoyBootstrapExtraDropdown,
      ServoyBootstrapExtraSwitch,
      SvyAttributes
    ],
    providers: [],
    imports: [
      CommonModule,
      ServoyPublicModule,
      SabloModule,
      NgbModule,
      FormsModule,
      JwBootstrapSwitchNg2Module
    ],
    exports: [
        ServoyBootstrapExtraBreadcrumbs,
        ServoyBootstrapExtraNavbar,
        ServoyBootstrapExtraCarousel,
        ServoyBootstrapExtraBadge,
        ServoyBootstrapExtraButtonsGroup,
        ServoyBootstrapExtraInputGroup,
        SvyAttributesInputGroup,
        ServoyBootstrapExtraRating,
        ServoyBootstrapExtraProgressBar,
        ServoyBootstrapExtraDropdown,
        ServoyBootstrapExtraSwitch,
        SvyAttributes
      ]
})
export class ServoyBootstrapExtraComponentsModule {
      constructor( specTypesService: SpecTypesService ) {
         specTypesService.registerType('bootstrapextracomponents-navbar.menuItem', MenuItem);
         specTypesService.registerType('bootstrapextracomponents-dropdown.MenuItem', dropdown_MenuItem);
         specTypesService.registerType('bootstrapextracomponents-carousel.slide', Slide);
         specTypesService.registerType('bootstrapextracomponents-input-group.addOn', AddOn);
         specTypesService.registerType('bootstrapextracomponents-input-group.addOnButton', AddOnButton);
      }
}
