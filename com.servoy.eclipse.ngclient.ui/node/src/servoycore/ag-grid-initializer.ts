import { APP_INITIALIZER, EnvironmentProviders, makeEnvironmentProviders } from '@angular/core';
import { provideGlobalGridOptions, ModuleRegistry, ClientSideRowModelModule, RowAutoHeightModule, RowApiModule, ScrollApiModule, ValidationModule, RenderApiModule } from 'ag-grid-community';
import { LicenseManager, ServerSideRowModelModule, ServerSideRowModelApiModule } from 'ag-grid-enterprise';

// eslint-disable-next-line max-len
const AG_GRID_LICENSE_KEY = 'Using_this_{AG_Grid}_Enterprise_key_{AG-093974}_in_excess_of_the_licence_granted_is_not_permitted___Please_report_misuse_to_legal@ag-grid.com___For_help_with_changing_this_key_please_contact_info@ag-grid.com___{Servoy_B.V.}_is_granted_a_{Single_Application}_Developer_License_for_the_application_{Servoy}_only_for_{7}_Front-End_JavaScript_developers___All_Front-End_JavaScript_developers_working_on_{Servoy}_need_to_be_licensed___{Servoy}_has_been_granted_a_Deployment_License_Add-on_for_{Unlimited}_Production_Environments___This_key_works_with_{AG_Grid}_Enterprise_versions_released_before_{10_November_2026}____[v3]_[01]_MTc5NDI2ODgwMDAwMA==663af84e061124735c6afce309884a66';

let agGridInitialized = false;

function initializeAgGrid(): void {
    if (agGridInitialized) return;
    agGridInitialized = true;
    LicenseManager.setLicenseKey(AG_GRID_LICENSE_KEY);
    provideGlobalGridOptions({ theme: 'legacy' });
    ModuleRegistry.registerModules([
        ServerSideRowModelModule,
        ClientSideRowModelModule,
        RowAutoHeightModule,
        ServerSideRowModelApiModule,
        RowApiModule,
        ScrollApiModule,
        ValidationModule.with({ showOverlayOn: [] }),
        RenderApiModule
    ]);
}

export function provideAgGrid(): EnvironmentProviders {
    return makeEnvironmentProviders([
        { provide: APP_INITIALIZER, useValue: initializeAgGrid, multi: true }
    ]);
}
