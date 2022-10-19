import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { MatDialogModule } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatFormFieldModule } from '@angular/material/form-field';
import { MatInputModule } from '@angular/material/input';
import { MatSelectModule } from '@angular/material/select';

import { DialogBodyComponent } from './dialogservice/dialog-body/dialog-body.component';
import { DialogService } from './dialogservice/dialogs.service';


@NgModule({
    imports: [
        FormsModule,
        CommonModule,
        MatDialogModule,
        MatButtonModule,
        MatFormFieldModule,
        MatInputModule,
        MatSelectModule
    ],
    providers: [
        DialogService
    ],
    declarations: [DialogBodyComponent],
    exports: [
        DialogBodyComponent
    ]
})
export class DialogModule { }
