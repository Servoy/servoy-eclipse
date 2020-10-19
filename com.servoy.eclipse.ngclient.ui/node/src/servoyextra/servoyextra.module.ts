import { ServoyExtraTable } from "./table/table";
import { ServoyPublicModule } from "../ngclient/servoy_public.module";
import { SabloModule } from "../sablo/sablo.module";
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ResizableModule } from 'angular-resizable-element';
import { NgModule } from "@angular/core";
import { FormsModule } from '@angular/forms';
import { CommonModule, AsyncPipe } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

@NgModule({
    declarations: [
        ServoyExtraTable
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
    providers: [],
    exports: [ServoyExtraTable]})
export class ServoyExtraComponentsModule {}