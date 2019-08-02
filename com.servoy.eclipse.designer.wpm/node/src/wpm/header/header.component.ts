import { Component, OnInit, Inject } from '@angular/core';
import { WpmService, Repository } from '../wpm.service';
import { MatDialog, MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';

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
    })


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
