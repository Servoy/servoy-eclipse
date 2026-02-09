import { Component, ElementRef, HostListener, input, viewChild, signal } from '@angular/core';

@Component({
    selector: 'servoycore-message-dialog-window',
    templateUrl: './message-dialog-window.component.html',
    styleUrls: ['./message-dialog-window.component.css'],
    standalone: false
})
export class MessageDialogWindowComponent {

  readonly message = input<string>(undefined);
  readonly styleClass = input<string>(undefined);
  readonly values = input<string[]>(undefined);
  readonly buttonsText = input<string[]>(undefined);
  readonly inputType = input<string>(undefined);
  readonly defaultButtonIndex = input<number>(undefined);
  readonly okButtonText = input<string>('OK');
  
  _message = signal<string>(undefined);
  _styleClass = signal<string>(undefined);
  _values = signal<string[]>(undefined);
  _buttonsText = signal<string[]>(undefined);
  _inputType = signal<string>(undefined);
  _defaultButtonIndex = signal<number>(undefined);
  _okButtonText = signal<string>('OK');

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
    this._message.set(this.message());
    this._styleClass.set(this.styleClass());
    this._values.set(this.values());
    this._buttonsText.set(this.buttonsText());
    this._inputType.set(this.inputType());
    this._defaultButtonIndex.set(this.defaultButtonIndex());
    this._okButtonText.set(this.okButtonText());
    const okButtonText = this._okButtonText();
    if (!okButtonText) this._okButtonText.set('OK');
    const buttonsText = this._buttonsText();
    if(!buttonsText || !buttonsText.length) {
      this._buttonsText.set([okButtonText]);
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
