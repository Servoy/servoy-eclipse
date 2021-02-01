import { Component, OnInit, Inject, HostListener } from '@angular/core';
import { MatDialogRef, MAT_DIALOG_DATA } from '@angular/material/dialog';


@Component({
  selector: 'servoycore-dialog-body',
  templateUrl: './dialog-body.component.html',
  styleUrls: ['./dialog-body.component.css']
})
export class DialogBodyComponent implements OnInit {

  title: string;
  message: string;
  btnsText: string[];
  class: string;
  retValue: string;
  values: string[];

  constructor(@Inject(MAT_DIALOG_DATA) public data: any,
              public dialogRef: MatDialogRef<DialogBodyComponent>){}

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    const key = event.key;
    if (key === 'Escape') {
      const nodeName = (event.target as HTMLElement).nodeName;
      if (!nodeName.match(/mat-select/i)) {
        this.close(null);
      } 
    } else if (key === 'Enter') {
      if (this.class === 'type-input' || this.class === 'type-select') {
        this.close('OK');
      }
    }
  }

  ngOnInit(): void {
    this.title = this.data.title;
    this.message = this.data.message;
    this.btnsText = this.data.btnsText;
    this.class = this.data.class;
    this.values = this.data.initValues;
    if (this.class === 'type-input' && this.values != null) {
      this.retValue = this.values[0];
    }
  }

  close(value: string) {
    if (this.class === 'type-input' || this.class === 'type-select') {
       if (value !== 'OK') {
        this.retValue = null;
      }
    } else {
      this.retValue = value;
    }
    this.dialogRef.close(this.retValue);
  }

  getButtonColor(btnIndex: number, btnText: string) {
    if (this.class === 'type-input' || this.class === 'type-select') {
      if (btnIndex === 1) {
        return 'primary';
      }
      return '';
    }
    if (btnIndex === 0) {
      return 'primary';
    }
    return '';
  }

}
