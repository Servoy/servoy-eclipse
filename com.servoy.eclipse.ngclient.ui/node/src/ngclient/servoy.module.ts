import { CommonModule } from '@angular/common';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { MainComponent } from './main.component';

import { FormComponent, AddAttributeDirective } from './form/form_component.component';

import { SabloModule } from '../sablo/sablo.module';

import { AllServicesModules } from './allservices.service';
import { AllComponentsModule } from './allcomponents.module';
import { DefaultLoginWindowComponent } from './services/default-login-window/default-login-window.component';
import { FileUploadWindowComponent } from './services/file-upload-window/file-upload-window.component';
import { DialogWindowComponent } from './services/dialog-window/dialog-window.component';
import { ServoyFormPopupComponent } from './services/popupform/popupform';
import { UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe } from '@angular/common';

import { I18NProvider } from './services/i18n_provider.service';
import { DefaultNavigator } from '../servoycore/default-navigator/default-navigator';
import { ErrorBean } from '../servoycore/error-bean/error-bean';
import { ServoyCoreSlider } from '../servoycore/slider/slider';
import { SessionView } from '../servoycore/session-view/session-view';
import { ServoyCoreFormContainer } from '../servoycore/formcontainer/formcontainer';

import { ServoyPublicModule, ServoyPublicService } from '@servoy/public';
import { LoadingIndicatorComponent } from '../sablo/util/loading-indicator/loading-indicator';
import { ListFormComponent } from '../servoycore/listformcomponent/listformcomponent';

import { ServerDataService } from './services/serverdata.service';
import { BSWindow } from './services/bootstrap-window/bswindow.service';
import { BSWindowManager } from './services/bootstrap-window/bswindow_manager.service';
import { ServoyPublicServiceImpl } from './services/servoy_public_impl.service';
import {MainRoutingModule} from './main-routing.module';

@NgModule( {
    declarations: [
        MainComponent,
        FormComponent,
        AddAttributeDirective,
        DefaultLoginWindowComponent,
        FileUploadWindowComponent,
        DefaultNavigator,
        SessionView,
        ErrorBean,
        ServoyCoreSlider,
        ServoyCoreFormContainer,
        DialogWindowComponent,
        ServoyFormPopupComponent,
        LoadingIndicatorComponent,
        ListFormComponent
    ],
    imports: [
        CommonModule,
        NgbModule,
        FormsModule,
        SabloModule,
        AllComponentsModule,
        AllServicesModules,
        ServoyPublicModule,
        MainRoutingModule
    ],
    providers: [I18NProvider, UpperCasePipe, LowerCasePipe,
					ServerDataService, BSWindow, BSWindowManager, DatePipe, DecimalPipe,
					ServoyPublicServiceImpl, { provide: ServoyPublicService, useExisting: ServoyPublicServiceImpl }],
    bootstrap: [MainComponent],
    entryComponents: [DefaultLoginWindowComponent, FileUploadWindowComponent, DialogWindowComponent, ServoyFormPopupComponent],
    schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
} )
export class ServoyModule { }
