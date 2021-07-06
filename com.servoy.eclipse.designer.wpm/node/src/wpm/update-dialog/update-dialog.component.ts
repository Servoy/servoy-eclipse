import {Component, Inject} from '@angular/core';
import {MAT_DIALOG_DATA} from '@angular/material/dialog';
import { Package, WpmService } from '../wpm.service';

@Component({
    selector: 'update-dialog',
    templateUrl: './update-dialog.component.html',
    styleUrls: ['./update-dialog.component.css']
})
export class UpdatePackagesDialog {

    extendedData: ExtendedPackage[] = [];
    installingOrRemoving = false;

    constructor(@Inject(MAT_DIALOG_DATA) public data: Package[], public wpmService: WpmService) {
        data.forEach(p => {
            if (this.wpmService.versionCompare(p.installed, p.releases[0].version) < 0) {
                this.extendedData.push({package: p, isSelected: p.packageType != 'Solution-Main' ? true : false});
            }
        });
     }  

     updateSelected() {
        this.extendedData.forEach(data => {
            if (data.isSelected) {
                data.package.selected = data.package.releases[0].version;
                this.wpmService.install(data.package);
            }
        })
     }

     updateAll() {
        this.extendedData.forEach(data => {
            data.package.selected = data.package.releases[0].version;
            this.wpmService.install(data.package);
        })
     }
}

interface ExtendedPackage {
    package: Package;
    isSelected: boolean;
}