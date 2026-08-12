import { CommonModule } from '@angular/common';
import { NgModule } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AllServicesModules } from './allservices.service';
import { AllComponentsModule } from './allcomponents.module';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyCoreComponentsModule } from '../servoycore/servoycore.module';
import { ListFormComponent } from '../servoycore/listformcomponent/listformcomponent';
import { RowRenderer } from '../servoycore/listformcomponent/row-renderer.component';

@NgModule( {
    imports: [
        CommonModule,
        FormsModule,
        AllComponentsModule,
        AllServicesModules,
        ServoyPublicModule,
        ServoyCoreComponentsModule,
        ListFormComponent,
        RowRenderer
    ],
    exports: [
        CommonModule,
        FormsModule,
        AllComponentsModule,
        AllServicesModules,
        ServoyPublicModule,
        ServoyCoreComponentsModule,
        ListFormComponent,
        RowRenderer
    ]
} )
export class LFCModule {
 }
