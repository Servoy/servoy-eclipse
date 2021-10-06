import { Component, OnInit, Inject } from '@angular/core';
import { WpmService, Repository, Package } from '../wpm.service';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';
import { ExtendedPackage, UpdatePackagesDialog } from '../update-dialog/update-dialog.component';

const ADD_REMOVE_TEXT: string = "Add...";
const SERVOY_DEFAULT: string = "Servoy Default";

@Component({
  selector: 'app-header',
  templateUrl: './header.component.html',
  styleUrls: ['./header.component.css']
})
export class HeaderComponent implements OnInit {

  repositories: string[] = [SERVOY_DEFAULT, ADD_REMOVE_TEXT];
  activeRepository: string = SERVOY_DEFAULT;
  packages: Package[] = [];
  isUpdateAllButtonDisabled = false;

  constructor(public wpmService: WpmService, public dialog: MatDialog) {
  }

  ngOnInit() {

    this.wpmService.getAllRepositories().subscribe(repositories => {
      const newRepositories = [];
      let newActiveRepository = this.activeRepository;
      for(let repository of repositories) {
        if(repository.selected) newActiveRepository = repository.name;
        newRepositories.push(repository.name);
      }
      newRepositories.push(ADD_REMOVE_TEXT);
      this.repositories = newRepositories;
      if(this.activeRepository != newActiveRepository) {
        this.activeRepository = newActiveRepository;
        this.wpmService.setNewSelectedRepository(this.activeRepository);
      }
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
            } else if (p.name === pack.name && !pack.installedIsWPA) {
              arr.splice(i, 1);
            }   
          }); 
        });
      });
      this.updateStateForUpdateAllButton();
    });

    this.wpmService.packageToBeRemoved.subscribe(pack => {
      this.packages.forEach(p => {
        if (p.name === pack.name) {
          p.markedAsRemoved = true;
        }
      });
    });

    this.updateStateForUpdateAllButton();
  }

  openDialog() { 
    const readyPackages = this.packages.filter(p => !p.hasLatestVersion && !p.markedAsRemoved); 
    const dialogRef = this.dialog.open(UpdatePackagesDialog, {data: readyPackages});
    dialogRef.beforeClosed().subscribe(result => {
      if (result) {
        result.forEach((el: ExtendedPackage) => {
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
      this.isUpdateAllButtonDisabled = this.packages.find(p => !p.hasLatestVersion && !p.markedAsRemoved) ? false : true;
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
    }
    else {
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
    const dialogRef = this.dialog.open(AddRepositoryDialog, {
      data: <Repository> { name: "", url: ""}
    });
  
    dialogRef.afterClosed().subscribe(result => {
      const newRepo = <Repository>result;

      if(newRepo) {
        if(newRepo.name == ADD_REMOVE_TEXT) {
          this.showAddRepositoryErrorDialog("The name can't be " + ADD_REMOVE_TEXT);
          return;
        }
        if (newRepo.name == "" || newRepo.url == "") {
          this.showAddRepositoryErrorDialog("The name or url must be filled in");
          return;
          
        }
        for (let repository of this.repositories) {
          if (newRepo.name == repository) {
            this.showAddRepositoryErrorDialog("The name is already defined");
            return; 
          }
        }
        this.wpmService.addNewRepository(newRepo);
      }
      else {
        this.activeRepository = SERVOY_DEFAULT;
        this.wpmService.setNewSelectedRepository(this.activeRepository);
      }
    }, err => {
      this.activeRepository = SERVOY_DEFAULT;
      this.wpmService.setNewSelectedRepository(this.activeRepository);
    });
  }

  showAddRepositoryErrorDialog(message: string) {
    this.dialog.open(ErrorDialog, {
      data: message
    }).afterClosed().subscribe(result => {
      this.showAddRepositoryDialog();
    })
  }
}

@Component({
  selector: 'add-repository-dialog',
  templateUrl: 'add-repository-dialog.html',
})
export class AddRepositoryDialog {

  constructor(
    public dialogRef: MatDialogRef<AddRepositoryDialog>, @Inject(MAT_DIALOG_DATA) public data: Repository) {}

  onCancelClick(): void {
    this.dialogRef.close();
  }

}
@Component({
  selector: 'error-dialog',
  templateUrl: 'error-dialog.html'
})
export class ErrorDialog {
  constructor(
    public dialogRef: MatDialogRef<ErrorDialog>, @Inject(MAT_DIALOG_DATA) public data: string) {}

  onOkClick(): void {
    this.dialogRef.close();
  }
}
