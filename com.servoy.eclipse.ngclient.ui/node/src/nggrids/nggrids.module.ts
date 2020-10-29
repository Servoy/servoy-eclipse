import { NgModule } from '@angular/core';
import { AgGridModule } from 'ag-grid-angular';
import 'ag-grid-enterprise';
import { LicenseManager } from 'ag-grid-enterprise';
import { DataGrid } from './datagrid/datagrid';

@NgModule({
    declarations: [
        DataGrid
    ],
    imports: [
        AgGridModule.withComponents([])
    ],
    exports: [
        DataGrid
    ]
})
export class NGGridsModule {
    constructor() {
        LicenseManager.setLicenseKey("CompanyName=Servoy B.V.,LicensedApplication=Servoy,LicenseType=SingleApplication,LicensedConcurrentDeveloperCount=7,LicensedProductionInstancesCount=200,AssetReference=AG-010463,ExpiryDate=11_October_2021_[v2]_MTYzMzkwNjgwMDAwMA==4c6752fe4cb2066ab1f0e9c572bc7491");
    }
}