import { NgModule, CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';
import { ServoyExtraTable } from './table/table';
import { ServoyExtraHtmlarea } from './htmlarea/htmlarea';
import { ServoyExtraImageLabel } from './imagelabel/imagelabel';
import { ServoyExtraFileUpload } from './fileupload/fileupload';
import { ServoyExtraSlider } from './slider/slider';
import { ServoyExtraSplitpane } from './splitpane/splitpane';
import { ServoyPublicModule } from '../ngclient/servoy_public.module';
import { SabloModule } from '../sablo/sablo.module';
import { ScrollingModule } from '@angular/cdk/scrolling';
import { ResizableModule } from 'angular-resizable-element';
import { FormsModule } from '@angular/forms';
import { CommonModule, AsyncPipe } from '@angular/common';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { AngularEditorModule } from '@kolkov/angular-editor';
import { FileUploadModule } from 'ng2-file-upload';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { BGPane } from './splitpane/bg_splitter/bg_pane.component';
import { BGSplitter } from './splitpane/bg_splitter/bg_splitter.component';

@NgModule({
    declarations: [
        ServoyExtraTable,
        ServoyExtraHtmlarea,
        ServoyExtraImageLabel,
        ServoyExtraFileUpload,
        ServoyExtraSlider,
		ServoyExtraSplitpane,
		BGSplitter,
    	BGPane
    ],
    imports: [
        ServoyPublicModule,
        SabloModule,
        CommonModule,
        FormsModule,
        ResizableModule,
        ScrollingModule,
        NgbModule,
        AngularEditorModule,
        FileUploadModule,
        NgxSliderModule
    ],
    providers: [AsyncPipe
    ],
    exports: [ServoyExtraTable,
              ServoyExtraHtmlarea,
              ServoyExtraImageLabel,
              ServoyExtraFileUpload,
              ServoyExtraSlider,
			  ServoyExtraSplitpane
    ],
    schemas: [
             CUSTOM_ELEMENTS_SCHEMA
    ]
})
export class ServoyExtraComponentsModule {}
