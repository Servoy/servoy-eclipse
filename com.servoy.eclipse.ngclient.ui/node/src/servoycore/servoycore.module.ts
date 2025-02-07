import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DefaultNavigator } from './default-navigator/default-navigator';
import { ErrorBean } from './error-bean/error-bean';
import { ServoyCoreSlider } from './slider/slider';
import { SessionView } from './session-view/session-view';
import { ServoyCoreFormContainer } from './formcontainer/formcontainer';
import { AddAttributeDirective } from './addattribute.directive';
import { ServoyPublicModule } from '@servoy/public';
import { provideGlobalGridOptions, ModuleRegistry, ClientSideRowModelModule, RowAutoHeightModule, RowApiModule, ScrollApiModule, ValidationModule } from 'ag-grid-community';
import { LicenseManager, ServerSideRowModelModule, ServerSideRowModelApiModule } from 'ag-grid-enterprise';
import { ServoyCoreFormcomponentResponsiveCotainer } from './formcomponent-responsive-container/formcomponent-responsive-container';

@NgModule( {
    declarations: [
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        AddAttributeDirective,
        ServoyCoreFormcomponentResponsiveCotainer
    ],
    imports: [CommonModule,
        FormsModule,
        ServoyPublicModule
    ],
    providers: [],
    bootstrap: [],
    exports: [
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        AddAttributeDirective,
        ServoyCoreFormcomponentResponsiveCotainer
    ]
} )
export class ServoyCoreComponentsModule {
    constructor() {
        // eslint-disable-next-line max-len
        LicenseManager.setLicenseKey('Using_this_{AG_Grid}_Enterprise_key_{AG-065608}_in_excess_of_the_licence_granted_is_not_permitted___Please_report_misuse_to_legal@ag-grid.com___For_help_with_changing_this_key_please_contact_info@ag-grid.com___{Servoy_B.V.}_is_granted_a_{Single_Application}_Developer_License_for_the_application_{Servoy}_only_for_{7}_Front-End_JavaScript_developers___All_Front-End_JavaScript_developers_working_on_{Servoy}_need_to_be_licensed___{Servoy}_has_been_granted_a_Deployment_License_Add-on_for_{Unlimited}_Production_Environments___This_key_works_with_{AG_Grid}_Enterprise_versions_released_before_{10_November_2025}____[v3]_[01]_MTc2MjczMjgwMDAwMA==2e8fcb0d114b18b4820dafb7a1d2b70d');
        provideGlobalGridOptions({ theme: "legacy" });
        ModuleRegistry.registerModules([
            ServerSideRowModelModule,
            ClientSideRowModelModule,
            RowAutoHeightModule,
            ServerSideRowModelApiModule,
            RowApiModule,
            ScrollApiModule,
            ValidationModule]);
    }
}
