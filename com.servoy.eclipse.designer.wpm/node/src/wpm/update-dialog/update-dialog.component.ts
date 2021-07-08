import {Component, Inject} from '@angular/core';
import {MatDialogRef, MAT_DIALOG_DATA} from '@angular/material/dialog';
import { Package, WpmService } from '../wpm.service';

@Component({
    selector: 'update-dialog',
    templateUrl: './update-dialog.component.html',
    styleUrls: ['./update-dialog.component.css']
})
export class UpdatePackagesDialog {

    extendedData: ExtendedPackage[] = [];
    installingOrRemoving = false;

    constructor(public dialogRef: MatDialogRef<UpdatePackagesDialog>, 
        @Inject(MAT_DIALOG_DATA) public data: Package[], public wpmService: WpmService) {
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
        this.dialogRef.close();
     }

     updateAll(): void {
        this.extendedData.forEach(data => {
            data.package.selected = data.package.releases[0].version;
            this.wpmService.install(data.package);
        });
        this.dialogRef.close();
     }

     closeDialog(): void {
         this.dialogRef.close();
     }
}

interface ExtendedPackage {
    package: Package;
    isSelected: boolean;
}