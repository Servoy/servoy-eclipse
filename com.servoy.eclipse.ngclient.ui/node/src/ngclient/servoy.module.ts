import { CommonModule } from '@angular/common';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';

import { MainComponent } from './main.component';

import { FormComponent } from './form/form_component.component';

import { AllServicesModules } from './allservices.service';
import { AllComponentsModule } from './allcomponents.module';
import { DefaultLoginWindowComponent } from './services/default-login-window/default-login-window.component';
import { FileUploadWindowComponent } from './services/file-upload-window/file-upload-window.component';
import { DialogWindowComponent } from './services/dialog-window/dialog-window.component';
import { ServoyFormPopupComponent } from './services/popupform/popupform';
import { UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe } from '@angular/common';

import { ServoyPublicModule, ServoyPublicService } from '@servoy/public';
import { LoadingIndicatorComponent } from '../sablo/util/loading-indicator/loading-indicator';
import { ServoyCoreComponentsModule } from '../servoycore/servoycore.module';

import { ServerDataService } from './services/serverdata.service';
import { BSWindowManager } from './services/bootstrap-window/bswindow_manager.service';
import { ServoyPublicServiceImpl } from './services/servoy_public_impl.service';
import {MainRoutingModule} from './main-routing.module';
import { DeveloperService } from './developer.service';
import { AlertWindowComponent} from './services/alert-window/alert-window.component';
import { MessageDialogWindowComponent } from './services/message-dialog-window/message-dialog-window.component';
import { ListFormComponent } from '../servoycore/listformcomponent/listformcomponent';
import { RowRenderer } from '../servoycore/listformcomponent/row-renderer.component';

@NgModule( {
    declarations: [
        MainComponent,
        FormComponent,
        DefaultLoginWindowComponent,
        FileUploadWindowComponent,
        DialogWindowComponent,
        ServoyFormPopupComponent,
        LoadingIndicatorComponent,
        AlertWindowComponent,
        MessageDialogWindowComponent,
        ListFormComponent,
        RowRenderer
    ],
    imports: [
        CommonModule,
        FormsModule,
        AllComponentsModule,
        AllServicesModules,
        ServoyPublicModule,
        MainRoutingModule,
        ServoyCoreComponentsModule
    ],
    providers: [UpperCasePipe, LowerCasePipe,
        ServerDataService, BSWindowManager, DatePipe, DecimalPipe,
        ServoyPublicServiceImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceImpl }],
    bootstrap: [MainComponent],
    schemas: [
        CUSTOM_ELEMENTS_SCHEMA
    ]
} )
export class ServoyModule {
    constructor(_developerService: DeveloperService) {
        // the above developer service must just be loaded..
    }
 }
