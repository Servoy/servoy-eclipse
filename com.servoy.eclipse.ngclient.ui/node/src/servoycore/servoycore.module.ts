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
import { LicenseManager, ModuleRegistry, ServerSideRowModelModule, ClientSideRowModelModule } from '@ag-grid-enterprise/all-modules';
import { AgGridModule } from '@ag-grid-community/angular';
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
        AgGridModule.withComponents([])
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
        LicenseManager.setLicenseKey('CompanyName=Servoy B.V.,LicensedApplication=Servoy,LicenseType=SingleApplication,LicensedConcurrentDeveloperCount=7,LicensedProductionInstancesCount=10000,AssetReference=AG-018380,ExpiryDate=11_October_2022_[v2]_MTY2NTQ0MjgwMDAwMA==a725c314c19f2c87b1f6a2f4836eec3e');
        ModuleRegistry.registerModules([ServerSideRowModelModule, ClientSideRowModelModule]);
    }
}
