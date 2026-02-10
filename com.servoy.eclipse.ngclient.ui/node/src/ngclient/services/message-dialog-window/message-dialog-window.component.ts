import { Component, ElementRef, HostListener, viewChild, signal } from '@angular/core';

@Component({
    selector: 'servoycore-message-dialog-window',
    templateUrl: './message-dialog-window.component.html',
    styleUrls: ['./message-dialog-window.component.css'],
    standalone: false
})
export class MessageDialogWindowComponent {

  readonly message = signal<string>(undefined);
  readonly styleClass = signal<string>(undefined);
  readonly values = signal<string[]>(undefined);
  readonly buttonsText = signal<string[]>(undefined);
  readonly inputType = signal<string>(undefined);
  readonly defaultButtonIndex = signal<number>(undefined);
  readonly okButtonText = signal<string>('OK');

  readonly inputfield = viewChild<ElementRef>("inputfield");
  readonly buttons = viewChild<ElementRef>("buttons");
  readonly svyMessageDialog = viewChild<ElementRef>("svyMessageDialog");

  retValue: string;
  onCloseCallback: (r: string) => void;

  @HostListener('document:keydown', ['$event'])
  handleKeyboardEvent(event: KeyboardEvent) {
    const key = event.key;
    const styleClass = this.styleClass();
    if (key === 'Escape') {
      this.dismiss(null);
    } else if (key === 'Enter' && (styleClass === 'type-input' || styleClass === 'type-select')) {
      this.dismiss('OK');
    }
  }

  ngOnInit(): void {
    const okButtonText = this.okButtonText();
    if (!okButtonText) this.okButtonText.set('OK');
    const buttonsText = this.buttonsText();
    if(!buttonsText || !buttonsText.length) {
      this.buttonsText.set([okButtonText]);
    }
    const styleClass = this.styleClass();
    const values = this.values();
    if(values && values.length && (styleClass === 'type-input' || styleClass === 'type-select')) {
      this.retValue = values[0];
    }
  }

  ngAfterViewInit() {
    const styleClass = this.styleClass();
    if (styleClass === 'type-input' || styleClass === 'type-select') {
      this.inputfield().nativeElement.focus();
    } else {
      this.buttons().nativeElement.children[this.defaultButtonIndex()].focus();
    }
    const headerHeight = this.svyMessageDialog().nativeElement.querySelector('.window-header').offsetHeight;
    const footerHeight = this.buttons().nativeElement.offsetHeight;
    document.documentElement.style.setProperty('--header-height', `${headerHeight}px`);
    document.documentElement.style.setProperty('--footer-height', `${footerHeight}px`);
    this.svyMessageDialog().nativeElement.scrollTop = 0;
  }

  getButtonClass(btnIndex: number): string {
    const styleClass = this.styleClass();
    if (styleClass === 'type-input' || styleClass === 'type-select') {
      if (btnIndex === 1) {
        return 'svy-btn-primary';
      }
      return '';
    }
    if (btnIndex === this.defaultButtonIndex()) {
      return 'svy-btn-primary';
    }
    return '';
  }

  dismiss(value: string): void {
    const styleClass = this.styleClass();
    if (styleClass === 'type-input' || styleClass === 'type-select') {
      if (value !==this.okButtonText()) {
       this.retValue = null;
     }
    } else {
      this.retValue = value;
    }    
    this.onCloseCallback(this.retValue);
  }

  getType(): string {
    const inputType = this.inputType();
    return inputType ? inputType : 'text';
  }
}
