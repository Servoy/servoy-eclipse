import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ServoyExtraTable } from './table/table';
import { ServoyExtraHtmlarea } from './htmlarea/htmlarea';
import { ServoyExtraImageLabel } from './imagelabel/imagelabel';
import { ServoyPublicModule } from '../ngclient/servoy_public.module';
import { SabloModule } from '../sablo/sablo.module';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ResizableModule } from 'angular-resizable-element';
import { FormsModule } from '@angular/forms';
import { CommonModule, AsyncPipe } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { UiScrollModule } from 'ngx-ui-scroll';

@NgModule({
    declarations: [
        ServoyExtraTable,
        ServoyExtraHtmlarea,
        ServoyExtraImageLabel
    ],
    imports: [
        ServoyPublicModule,
        SabloModule,
        CommonModule,
        FormsModule,
        ResizableModule,
        ScrollingModule,
        UiScrollModule,
        NgbModule,
        AngularEditorModule
    ],
    providers: [AsyncPipe
    ],
    exports: [ServoyExtraTable,
              ServoyExtraHtmlarea,
              ServoyExtraImageLabel
    ],
    schemas: [
             CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyExtraComponentsModule {}
