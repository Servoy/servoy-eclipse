import { Component, OnInit, Inject, ChangeDetectionStrategy, ChangeDetectorRef } from '@angular/core';
import { WpmService } from '../wpm.service';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA, MatDialogTitle, MatDialogContent, MatDialogActions, MatDialogClose } from '@angular/material/dialog';
import { ExtendedPackage, UpdatePackagesDialogComponent } from '../update-dialog/update-dialog.component';
import { Package, Repository } from '../websocket.service';
import { MatSelect, MatOption, MatFormField } from '@angular/material/select';
import { MatTooltip } from '@angular/material/tooltip';
import { MatIconButton, MatButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { NgStyle } from '@angular/common';
import { CdkScrollable } from '@angular/cdk/scrolling';
import { FormsModule } from '@angular/forms';
import { MatInput } from '@angular/material/input';

const ADD_REMOVE_TEXT = 'Add...';
const SERVOY_DEFAULT= 'Servoy Default';

@Component({
    selector: 'app-header',
    templateUrl: './header.component.html',
    styleUrls: ['./header.component.css'],
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatSelect, MatTooltip, MatOption, MatIconButton, MatIcon, NgStyle, MatButton]
})
export class HeaderComponent implements OnInit {

  repositories: string[] = [SERVOY_DEFAULT, ADD_REMOVE_TEXT];
  activeRepository: string = SERVOY_DEFAULT;
  packages: Package[] = [];
  isUpdateAllButtonDisabled = false;

  constructor(public wpmService: WpmService, public dialog: MatDialog, private cdr: ChangeDetectorRef) {
  }

  ngOnInit() {

    this.wpmService.getAllRepositories().subscribe(repositories => {
      const newRepositories: string[] = [];
      let newActiveRepository = this.activeRepository;
      for(const repository of repositories) {
        if(repository.selected) newActiveRepository = repository.name;
        newRepositories.push(repository.name);
      }
      newRepositories.push(ADD_REMOVE_TEXT);
      this.repositories = newRepositories;
      if(this.activeRepository != newActiveRepository) {
        this.activeRepository = newActiveRepository;
        this.wpmService.setNewSelectedRepository(this.activeRepository);
      }
      this.cdr.markForCheck();
    });  

    this.wpmService.packageLists.subscribe(packageLists => {
      packageLists.forEach(list => {
        list.packages.forEach((pack: Package) => {
          if (pack.installed && !this.isLatestRelease(pack) && !this.packages.find(p => p.name === pack.name)) {
            this.packages.push(pack);
          } 

          // update the package list in case the version has changed
          this.packages.forEach((p, i, arr) => {
            if (p.name === pack.name && pack.selected && pack.installing) {  
              if (this.wpmService.versionCompare(pack.selected, p.selected) !== 0) {
                arr[i].selected = pack.selected; 
              }  
              arr[i].hasLatestVersion = arr[i].selected === p.releases[0].version;    
              arr[i].markedAsRemoved = false;
            }   
          }); 
        });
      });
      this.updateStateForUpdateAllButton();
      this.cdr.markForCheck();
    });

    this.wpmService.packageToBeRemoved.subscribe(pack => {
      this.packages.forEach(p => {
        if (p.name === pack.name) {
          p.markedAsRemoved = true;
        }
      });
      this.cdr.markForCheck();
    });

    this.updateStateForUpdateAllButton();
  }

  openDialog() { 
    const readyPackages = this.packages.filter(p => !p.hasLatestVersion && !p.markedAsRemoved); 
    const dialogRef = this.dialog.open(UpdatePackagesDialogComponent, {data: readyPackages});
    dialogRef.beforeClosed().subscribe( (result: ExtendedPackage[]) => {
      if (result) {
        result.forEach((el) => {
          if (el.isSelected) {
            this.packages.forEach(p => {
              if (p.name === el.package.name) {
                p.hasLatestVersion = true; 
              }
            });
          }
        });
        this.updateStateForUpdateAllButton();
      }
    });
  }
 
  updateStateForUpdateAllButton() { 
      // the update all button will be disabled if all packages have the latest version installed
      this.isUpdateAllButtonDisabled = this.packages.find(p => !p.hasLatestVersion && !p.markedAsRemoved && (p.installedIsWPA || p.installedIsWPA === undefined)) ? false : true;
  }

  isLatestRelease(p: Package): boolean {
    return (p.installed == p.releases[0].version) || (this.wpmService.versionCompare(p.installed, p.releases[0].version) > 0);
  }

  getActiveSolution(): string {
    return this.wpmService.getActiveSolution();
  }

  isNeedRefresh(): boolean {
    return this.wpmService.isNeedRefresh();
  }

  refresh() {
    window.location.reload();
  }

  isContentAvailable(): boolean {
    return this.wpmService.isContentAvailable();
  }

  onActiveRepositoryChange() {
    if(this.activeRepository == ADD_REMOVE_TEXT) {
      this.showAddRepositoryDialog();
    } else {
      this.wpmService.setNewSelectedRepository(this.activeRepository);
    }
  }

  showRemoveRepository(): boolean {
    return this.activeRepository != ADD_REMOVE_TEXT && this.activeRepository != SERVOY_DEFAULT;
  }

  removeRepository() {
    this.wpmService.removeRepositoryWithName(this.activeRepository);
  }

  showAddRepositoryDialog(): void {
    const dialogRef = this.dialog.open(AddRepositoryDialogComponent, {
      data: { name: '', url: ''} as Repository
    });
  
    dialogRef.afterClosed().subscribe(result => {
      const newRepo = result as Repository;

      if(newRepo) {
        if(newRepo.name == ADD_REMOVE_TEXT) {
          this.showAddRepositoryErrorDialog("The name can't be " + ADD_REMOVE_TEXT);
          return;
        }
        if (newRepo.name == '' || newRepo.url == '') {
          this.showAddRepositoryErrorDialog('The name or url must be filled in');
          return;
          
        }
        for (const repository of this.repositories) {
          if (newRepo.name == repository) {
            this.showAddRepositoryErrorDialog('The name is already defined');
            return; 
          }
        }
        this.wpmService.addNewRepository(newRepo);
      } else {
        this.activeRepository = SERVOY_DEFAULT;
        this.wpmService.setNewSelectedRepository(this.activeRepository);
      }
    }, _err => {
      this.activeRepository = SERVOY_DEFAULT;
      this.wpmService.setNewSelectedRepository(this.activeRepository);
    });
  }

  showAddRepositoryErrorDialog(message: string) {
    this.dialog.open(ErrorDialogComponent, {
      data: message
    }).afterClosed().subscribe(_result => {
      this.showAddRepositoryDialog();
    })
  }
}

@Component({
    selector: 'wpm-add-repository-dialog',
    templateUrl: 'add-repository-dialog.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, FormsModule, MatFormField, MatInput, MatDialogActions, MatButton, MatDialogClose]
})
export class AddRepositoryDialogComponent {

  constructor(
    public dialogRef: MatDialogRef<AddRepositoryDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: Repository) {}

  onCancelClick(): void {
    this.dialogRef.close();
  }

}
@Component({
    selector: 'wpm-error-dialog',
    templateUrl: 'error-dialog.html',
    changeDetection: ChangeDetectionStrategy.OnPush,
    imports: [MatDialogTitle, CdkScrollable, MatDialogContent, MatDialogActions, MatButton]
})
export class ErrorDialogComponent {
  constructor(
    public dialogRef: MatDialogRef<ErrorDialogComponent>, @Inject(MAT_DIALOG_DATA) public data: string) {}

  onOkClick(): void {
    this.dialogRef.close();
  }
}
