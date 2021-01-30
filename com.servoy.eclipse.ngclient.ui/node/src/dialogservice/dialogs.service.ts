import { Injectable } from '@angular/core';
import {MatDialog, MatDialogConfig } from '@angular/material/dialog';
import { DialogBodyComponent } from './dialog-body/dialog-body.component';

@Injectable()
export class DialogService{

  dialogConfig = new MatDialogConfig();

  constructor(private matDialog: MatDialog) {}

    public async showErrorDialog(dialogTitle: string, dialogMessage: string, ...buttonsText: string[]): Promise<any> {
      return await this.showDialog(dialogTitle, dialogMessage, 'type-error', null, buttonsText);
    }

    public async showInfoDialog(dialogTitle: string, dialogMessage: string, ...buttonsText: string[]): Promise<any>{
      return await this.showDialog(dialogTitle, dialogMessage, 'type-info', null, buttonsText);
    }

    public async showQuestionDialog(dialogTitle: string, dialogMessage: string, ...buttonsText: string[]): Promise<any> {
      return await this.showDialog(dialogTitle, dialogMessage, 'type-question', null, buttonsText);
    }

    public async showInputDialog(dialogTitle: string, dialogMessage: string, initValue: string) {
        if (!dialogTitle) {
          dialogTitle = 'Enter value';
        }
        return await this.showDialog(dialogTitle, dialogMessage, 'type-input', [initValue], ['Cancel', 'OK']);
    }

    public async showSelectDialog(dialogTitle: string, dialogMessage: string, ...options: string[]): Promise<any> {
      return await this.showDialog(dialogTitle, dialogMessage, 'type-select', options, ['Cancel', 'OK']);
    }

    public async showWarningDialog(dialogTitle: string, dialogMessage: string, ...buttonsText: string[]): Promise<any> {
      return await this.showDialog(dialogTitle, dialogMessage, 'type-warning', null, buttonsText);
    }

    private async showDialog(dialogTitle: string, dialogMessage: string, styleClass: string, values: string[], buttonsText: string[]): Promise<any> {
      this.dialogConfig.disableClose = true;
      this.dialogConfig.autoFocus = true;
      this.dialogConfig.minWidth = 600;
      this.dialogConfig.minHeight =150;
      this.dialogConfig.closeOnNavigation = false;

      if (!buttonsText || buttonsText.length === 0) {// is this java like? Means - stop after first true statement?
        buttonsText = ['OK'];
      }

      this.dialogConfig.data = {
          title: dialogTitle,
          message: dialogMessage,
          btnsText: this.escapeString(buttonsText.toString(), true).split(','),
          class: styleClass,
          initValues: values != null ? values.toString().split(',') : null
      };

      const dialogRef = this.matDialog.open(DialogBodyComponent, this.dialogConfig);
      return await dialogRef.afterClosed().toPromise().then(
          (value) => {return Promise.resolve(value)}
        );
    }
    private escapeString(str: string, forDisplay: boolean): string {
        return str.replace(/&/g, forDisplay ? '&amp;' : 'amp').
            replace(/</g, forDisplay ? '&lt;' : 'lt').
            replace(/>/g, forDisplay ? '&gt;' : 'gt').
            replace(/'/g, forDisplay ? '&apos;' : 'apos').
            replace(/"/g, forDisplay ? '&quot;' : 'quot');
    }
}