import { Component, ChangeDetectionStrategy, inject } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { Package } from '../websocket.service';
import {  WpmService } from '../wpm.service';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { MatCheckbox } from '@angular/material/checkbox';
import { FormsModule } from '@angular/forms';
import { MatButton } from '@angular/material/button';

@Component({
    selector: 'wpm-update-dialog',
    templateUrl: './update-dialog.component.html',
    styleUrls: ['./update-dialog.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatCheckbox, FormsModule, MatDialogActions, MatButton, MatDialogClose]
})
export class UpdatePackagesDialogComponent {
    dialogRef = inject<MatDialogRef<UpdatePackagesDialogComponent>>(MatDialogRef);
    data = inject<Package[]>(MAT_DIALOG_DATA);
    wpmService = inject(WpmService);


    extendedData: ExtendedPackage[] = [];
    installingOrRemoving = false;

    constructor() {
        const data = this.data;

        data.forEach(p => {
            if (this.wpmService.versionCompare(p.installed, p.releases[0].version) < 0) {
                this.extendedData.push({package: p, isSelected: p.packageType != 'Solution-Main' ? true : false});
            }
        });
     }  

     updateSelected(): void {
        this.extendedData.forEach(data => {
            if (data.isSelected) {
                data.package.selected = data.package.releases[0].version;
                this.wpmService.install(data.package);
            }
        });
     }

     updateAll(): void {
        this.extendedData.forEach(data => {
            data.package.selected = data.package.releases[0].version;
            this.wpmService.install(data.package);
        });
     }

     closeDialog(): void {
         this.dialogRef.close();
     }
}

export interface ExtendedPackage {
    package: Package;
    isSelected: boolean;
}