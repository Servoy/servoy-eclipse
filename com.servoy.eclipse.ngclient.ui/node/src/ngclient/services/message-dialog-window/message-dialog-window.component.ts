import { Component, Input, ViewChild, ElementRef, HostListener} from '@angular/core';

@Component({
    selector: 'servoycore-message-dialog-window',
    templateUrl: './message-dialog-window.component.html',
    styleUrls: ['./message-dialog-window.component.css'],
    standalone: false
})
export class MessageDialogWindowComponent {

  @Input() message: string;
  @Input() styleClass: string;
  @Input() values: string[];
  @Input() buttonsText: string[]
  @Input() inputType: string;

  @ViewChild("inputfield") inputfield: ElementRef;
  @ViewChild("buttons") buttons: ElementRef;
  @ViewChild("svyMessageDialog") svyMessageDialog: ElementRef;

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
    if(!this.buttonsText || !this.buttonsText.length) {
      this.buttonsText = ['OK'];
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
    const headerHeight = this.svyMessageDialog.nativeElement.querySelector('.window-header').offsetHeight;
    const footerHeight = this.buttons.nativeElement.offsetHeight;
    document.documentElement.style.setProperty('--header-height', `${headerHeight}px`);
    document.documentElement.style.setProperty('--footer-height', `${footerHeight}px`);
    this.svyMessageDialog.nativeElement.scrollTop = 0;
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
      if (value !== 'OK') {
       this.retValue = null;
     }
    } else {
      this.retValue = value;
    }    
    this.onCloseCallback(this.retValue);
  }

  getType(): string {
    return this.inputType ? this.inputType : 'text';
  }
}
