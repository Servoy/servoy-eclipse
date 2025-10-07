import { Component, Input, ViewChild, ElementRef, HostListener} from '@angular/core';

@Component({
  selector: 'servoycore-message-dialog-window',
  templateUrl: './message-dialog-window.component.html',
  styleUrls: ['./message-dialog-window.component.css']
})
export class MessageDialogWindowComponent {

  @Input() message: string;
  @Input() styleClass: string;
  @Input() values: string[];
  @Input() buttonsText: string[];
  @Input() okButtonText:string = 'OK';

  @ViewChild("inputfield") inputfield: ElementRef;
  @ViewChild("buttons") buttons: ElementRef;

  retValue: string;
  onCloseCallback: (r: string) => void;

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    const key = event.key;
    if (key === 'Escape') {
      this.dismiss(null);
    } else if (key === 'Enter' && (this.styleClass === 'type-input' || this.styleClass === 'type-select')) {
      this.dismiss('OK');
    }
  }

  ngOnInit(): void {
    if (!this.okButtonText) this.okButtonText  = 'OK';
    if(!this.buttonsText || !this.buttonsText.length) {
      this.buttonsText = [this.okButtonText];
    }
    if(this.values && this.values.length && (this.styleClass === 'type-input' || this.styleClass === 'type-select')) {
      this.retValue = this.values[0];
    }
  }

  ngAfterViewInit() {
    if (this.styleClass === 'type-input' || this.styleClass === 'type-select') {
      this.inputfield.nativeElement.focus();
    } else {
      this.buttons.nativeElement.children[0].focus();
    }
  }

  getButtonClass(btnIndex: number): string {
    if (this.styleClass === 'type-input' || this.styleClass === 'type-select') {
      if (btnIndex === 1) {
        return 'svy-btn-primary';
      }
      return '';
    }
    if (btnIndex === 0) {
      return 'svy-btn-primary';
    }
    return '';
  }

  dismiss(value: string): void {
    if (this.styleClass === 'type-input' || this.styleClass === 'type-select') {
      if (value !==this.okButtonText) {
       this.retValue = null;
     }
    } else {
      this.retValue = value;
    }    
    this.onCloseCallback(this.retValue);
  }
}
