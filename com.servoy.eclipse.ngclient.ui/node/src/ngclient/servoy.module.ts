import { BrowserModule } from '@angular/platform-browser';
import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { MainComponent } from './main.component';
import { FormService } from './form.service';
import { ServoyService } from './servoy.service';

import { FormComponent, AddAttributeDirective } from './form/form_component.component';

import { SabloModule } from '../sablo/sablo.module';

import { AllServicesModules } from './allservices.service';
import { AllComponentsModule } from './allcomponents.module';
import { DefaultLoginWindowComponent } from './services/default-login-window/default-login-window.component';
import { FileUploadWindowComponent } from './services/file-upload-window/file-upload-window.component';
import { DialogWindowComponent } from './services/dialog-window/dialog-window.component';
import { HttpClientModule } from '@angular/common/http';
import { UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe } from '@angular/common';

import { I18NProvider } from './services/i18n_provider.service';
import { DefaultNavigator } from '../servoycore/default-navigator/default-navigator';
import { ErrorBean } from '../servoycore/error-bean/error-bean';
import { ServoyCoreSlider } from '../servoycore/slider/slider';
import { SessionView } from '../servoycore/session-view/session-view';
import { ViewportService } from './services/viewport.service';

import { ComponentContributor } from './component_contributor.service';
import { ServoyPublicModule } from './servoy_public.module';
import { LoadingIndicatorComponent } from '../sablo/util/loading-indicator/loading-indicator';
import { ListFormComponent } from '../servoycore/listformcomponent/listformcomponent';

import { LocaleService } from './locale.service';
import { NGGridsModule } from '../nggrids/nggrids.module';
import { SvyUtilsService } from './services/utils.service';
import { ServerDataService } from './services/serverdata.service';
import { TypesRegisterService } from '../servoydefault/types_register.service';
import { BSWindow } from './services/bootstrap-window/bswindow.service';
import { BSWindowManager } from './services/bootstrap-window/bswindow_manager.service';

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
        DialogWindowComponent,
        LoadingIndicatorComponent,
        ListFormComponent
    ],
    imports: [
        BrowserModule,
        NgbModule,
        FormsModule,
        SabloModule,
        AllComponentsModule,
        AllServicesModules,
        HttpClientModule,
        ServoyPublicModule,
        NGGridsModule
    ],
    providers: [FormService, ServoyService, LocaleService, I18NProvider, UpperCasePipe, LowerCasePipe,
					SvyUtilsService,ServerDataService,TypesRegisterService, BSWindow, BSWindowManager,
                DatePipe, DecimalPipe, ComponentContributor, ViewportService],
    bootstrap: [MainComponent],
    entryComponents: [DefaultLoginWindowComponent, FileUploadWindowComponent, DialogWindowComponent],
    schemas: [
              CUSTOM_ELEMENTS_SCHEMA
    ]
} )
export class ServoyModule { }
