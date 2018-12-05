import { BrowserModule } from '@angular/platform-browser';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms'

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

import { MainComponent } from './main.component';
import { FormService } from './form.service';
import { ServoyService } from './servoy.service';

import { FormComponent, AddAttributeDirective } from './form/form_component.component';

import { SabloModule } from '../sablo/sablo.module'

import { AllServicesModules } from './allservices.service';
import { AllComponentsModule } from './allcomponents.module';
import { DefaultLoginWindowComponent } from './services/default-login-window/default-login-window.component';
import { FileUploadWindowComponent } from './services/file-upload-window/file-upload-window.component';
import { HttpClientModule } from '@angular/common/http';
import { FormatFilterPipe} from './servoy_public'
import {UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe } from '@angular/common';

import {I18NProvider} from './services/i18n_provider.service'
import { DefaultNavigator } from '../servoycore/default-navigator/default-navigator';

import { ComponentContributor } from './component_contributor.service';

@NgModule( {
    declarations: [
        MainComponent,
        FormComponent,
        AddAttributeDirective,
        DefaultLoginWindowComponent,
        FileUploadWindowComponent,
        DefaultNavigator
    ],
    imports: [
        BrowserModule,
        NgbModule.forRoot(),
        FormsModule,
        SabloModule,
		AllComponentsModule,
        AllServicesModules,
        HttpClientModule
    ],
    providers: [FormService, ServoyService, I18NProvider, UpperCasePipe, LowerCasePipe, DatePipe, DecimalPipe, FormatFilterPipe, ComponentContributor],
    bootstrap: [MainComponent],
    entryComponents: [DefaultLoginWindowComponent, FileUploadWindowComponent]
} )
export class ServoyModule { }
