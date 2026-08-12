import { NgModule } from '@angular/core';
import { RouterModule } from '@angular/router';

import { MainComponent } from './main.component';

import { FormComponent } from './form/form_component.component';

import { DefaultLoginWindowComponent } from './services/default-login-window/default-login-window.component';
import { FileUploadWindowComponent } from './services/file-upload-window/file-upload-window.component';
import { DialogWindowComponent } from './services/dialog-window/dialog-window.component';
import { ServoyFormPopupComponent } from './services/popupform/popupform';
import { UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe } from '@angular/common';

import { LoadingIndicatorComponent } from '../sablo/util/loading-indicator/loading-indicator';

import { ServerDataService } from './services/serverdata.service';
import { BSWindowManager } from './services/bootstrap-window/bswindow_manager.service';
import { ServoyPublicServiceImpl } from './services/servoy_public_impl.service';
import { DeveloperService } from './developer.service';
import { AlertWindowComponent} from './services/alert-window/alert-window.component';
import { MessageDialogWindowComponent } from './services/message-dialog-window/message-dialog-window.component';
import { ServoyCoreComponentsModule } from '../servoycore/servoycore.module';
import { ServoyPublicService } from '@servoy/public';
import { SERVOY_ROUTES } from './servoy.routes';

@NgModule( {
    imports: [
        RouterModule.forChild(SERVOY_ROUTES),
        ServoyCoreComponentsModule,
        MainComponent,
        FormComponent,
        DefaultLoginWindowComponent,
        FileUploadWindowComponent,
        DialogWindowComponent,
        ServoyFormPopupComponent,
        LoadingIndicatorComponent,
        AlertWindowComponent,
        MessageDialogWindowComponent
    ],
    providers: [UpperCasePipe, LowerCasePipe,
        ServerDataService, BSWindowManager, DatePipe, DecimalPipe,
        ServoyPublicServiceImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceImpl }]
} )
export class ServoyModule {
    constructor(_developerService: DeveloperService) {
        // the above developer service must just be loaded..
    }
 }
