import { Directive, ElementRef, HostListener, Input, Injector} from '@angular/core';
import {FormatFilterPipe} from '../pipes/pipes'

@Directive({ selector: '[svyFormat]'}) 
export class SvyFormat{
      private caretPosition : number;
      private element: HTMLInputElement;
      private formatFilterPipe : FormatFilterPipe;

      @Input('svyFormat') servoyFormat :string;
      @Input() svyType : string;
      @Input() svyFullFormat : {uppercase:false,lowercase:false, type:null};

      public constructor(private el: ElementRef, private injector: Injector) {
          this.caretPosition = -1;
          this.element = el.nativeElement;
          this.formatFilterPipe = this.injector.get(FormatFilterPipe);
      }
      
      @HostListener('input',['$event']) onInput(event:KeyboardEvent) {
          if (this.svyType == 'TEXT') this.format();          
      }
      
      @HostListener('keydown',['$event']) onKeydown(event:KeyboardEvent) {
          this.saveCaretPosition(event);   
      }
      
      @HostListener('keyup',['$event']) onKeyup(event:Event) {
          this.setCaretPosition();
      }
      
      private saveCaretPosition(event:KeyboardEvent) { 
          if(event.keyCode > 40) {
             this.caretPosition = this.element.selectionStart +1;
          }
          else if (event.keyCode == 8) { //backspace
              this.caretPosition = this.element.selectionStart -1; 
          }
      }
       
      private format() {
          var value = this.element.value;
          var ret = this.formatFilterPipe.transform(value, this.servoyFormat, this.svyType, this.svyFullFormat);
          this.element.value = ret;
      }
       
      private setCaretPosition() {  
          if (this.caretPosition < 0) return;
          if (this.element != null) {
//              if (this.element.createTextRange) {
//                  var range = this.element.createTextRange();
//                  range.move('character', this.caretPosition);
//                  range.select();
//              } else { 
                  if (this.element.selectionStart) {
                      this.element.focus();
                      this.element.setSelectionRange(this.caretPosition, this.caretPosition);
                  } else
                      this.element.focus();
              }
//          } 
          this.caretPosition = -1;
      }
  }
