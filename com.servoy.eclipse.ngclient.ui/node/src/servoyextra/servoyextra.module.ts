import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ServoyExtraTable } from "./table/table";
import { ServoyExtraHtmlarea } from "./htmlarea/htmlarea";
import { ServoyPublicModule } from "../ngclient/servoy_public.module";
import { SabloModule } from "../sablo/sablo.module";
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ResizableModule } from 'angular-resizable-element';
import { FormsModule } from '@angular/forms';
import { CommonModule, AsyncPipe } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
    declarations: [
        ServoyExtraTable,
        ServoyExtraHtmlarea
    ],
    imports: [
        ServoyPublicModule,
        SabloModule,
        CommonModule,
        FormsModule,
        ResizableModule,
        ScrollingModule,
        NgbModule
    ],
    providers: [AsyncPipe
    ],
    exports: [ServoyExtraTable,
              ServoyExtraHtmlarea
    ],
    schemas: [
             CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyExtraComponentsModule {}