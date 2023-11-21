import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { DefaultNavigator } from './default-navigator/default-navigator';
import { ErrorBean } from './error-bean/error-bean';
import { ServoyCoreSlider } from './slider/slider';
import { SessionView } from './session-view/session-view';
import { ServoyCoreFormContainer } from './formcontainer/formcontainer';
import {AddAttributeDirective} from './addattribute.directive';
import { ListFormComponent } from './listformcomponent/listformcomponent';
import { ServoyPublicModule } from '@servoy/public';
import { AgGridModule } from '@ag-grid-community/angular';
import { ModuleRegistry } from '@ag-grid-community/core';
import { LicenseManager } from '@ag-grid-enterprise/core';
import { ClientSideRowModelModule } from '@ag-grid-community/client-side-row-model';
import {  ServerSideRowModelModule } from '@ag-grid-enterprise/server-side-row-model';
import { RowRenderer } from './listformcomponent/row-renderer.component';
import { ServoyCoreFormcomponentResponsiveCotainer } from './formcomponent-responsive-container/formcomponent-responsive-container';

@NgModule( {
    declarations: [
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        ListFormComponent,
        AddAttributeDirective,
        RowRenderer,
        ServoyCoreFormcomponentResponsiveCotainer
    ],
    imports: [CommonModule,
        FormsModule,
        ServoyPublicModule,
        AgGridModule
    ],
    providers: [],
    bootstrap: [],
    exports: [
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        ListFormComponent,
        AddAttributeDirective,
        ServoyCoreFormcomponentResponsiveCotainer
    ]
} )
export class ServoyCoreComponentsModule {
    constructor() {
        // eslint-disable-next-line max-len
        LicenseManager.setLicenseKey('Using_this_AG_Grid_Enterprise_key_( AG-047126 )_in_excess_of_the_licence_granted_is_not_permitted___Please_report_misuse_to_( legal@ag-grid.com )___For_help_with_changing_this_key_please_contact_( info@ag-grid.com )___( Servoy B.V. )_is_granted_a_( Single Application )_Developer_License_for_the_application_( Servoy )_only_for_( 7 )_Front-End_JavaScript_developers___All_Front-End_JavaScript_developers_working_on_( Servoy )_need_to_be_licensed___( Servoy )_has_been_granted_a_Deployment_License_Add-on_for_( Unlimited )_Production_Environments___This_key_works_with_AG_Grid_Enterprise_versions_released_before_( 10 November 2024 )____[v2]_MTczMTE5NjgwMDAwMA==b4efc9fcdd26d4144e140977db93ef49');
        ModuleRegistry.registerModules([ServerSideRowModelModule, ClientSideRowModelModule]);
    }
}
