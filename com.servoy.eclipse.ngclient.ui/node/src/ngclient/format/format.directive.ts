import { Directive, ElementRef, HostListener, Input, Injector, OnChanges, SimpleChanges } from '@angular/core';


const MASK_CONST = {
        //Predefined character definitions
        definitions: {
            '#': "[0-9]",
            '0': "[0-9]",
            'U': "[A-Z]",
            'L': "[a-z]",
            'A': "[A-Za-z0-9]",
            '?': "[A-Za-z]",
            '*': ".",
            'H': "[A-F0-9]"
        },
        converters: {
            'U': function(c){return c.toUpperCase()},
            'L': function(c){return c.toLowerCase()},
            'H': function(c){return c.toUpperCase()}
        }
};
      
@Directive({ selector: '[svyFormat]'}) 
export class SvyFormat implements OnChanges{
    
      @Input('svyFormat') svyFormat : Format;
      private element: HTMLInputElement;

      private ignore : boolean;
      private firstNonMaskPos : number;
      private settings : Object;
      private buffer: string[] = [];
      private tests: RegExp[] = [];
      private converters : any[] = [];
      private mask : string;
      private focusText : string;
      private filteredMask : string;
      
      public constructor(private el: ElementRef) {
          this.element = el.nativeElement;
      }
      
      ngOnChanges(changes: SimpleChanges) {
          if (!this.svyFormat || (!this.svyFormat.isMask && !this.svyFormat.edit)) return;
          
          this.ignore = false;
          this.settings = {};
          this.settings['placeholder'] = this.svyFormat.placeHolder ? this.svyFormat.placeHolder : "_";
          if (this.svyFormat.allowedCharacters)
          this.settings['allowedCharacters'] = this.svyFormat.allowedCharacters;
          
          if (!this.settings['completed']) {
              this.settings['completed'] = null;
          }
          
          this.mask = this.svyFormat.edit;
//          if (!this.mask && this.element.value.length > 0) { //TODO check condition
//              return this.buffer.map(function(c, i) { //TODO why return??
//                  return this.tests[i] ? c : null;
//              }, this).join('');
//          }

          var skipNextMask = false;
          this.filteredMask = '';
          var defs = MASK_CONST.definitions;
          var converts = MASK_CONST.converters;
          this.converters = [];
          var partialPosition = this.mask.length;
          var firstNonMaskPos = null;
          var len = this.mask.length;

          var chars = this.mask.split("");
          for (var i =0 ; i < chars.length; i++) {
              var c = chars[i];
//            if (c == '?') {
//                len--;
//                partialPosition = i; 
//            } else 
              if (!skipNextMask && c == "'") {
                  skipNextMask = true;
                  len--;
                  partialPosition--;
              }
              else {
                  if (!skipNextMask && defs[c]) {
                      if (c == '*' && this.settings['allowedCharacters']) {
                          this.tests.push(new RegExp('[' + this.settings['allowedCharacters'] + ']'));
                      }
                      else {
                          this.tests.push(new RegExp(defs[c]));
                      }
                      if(firstNonMaskPos==null)
                          firstNonMaskPos =  this.tests.length - 1;
                  } else {
                      this.tests.push(null);
                      skipNextMask = false;
                  }
                  this.converters.push(converts[c]);
                  this.filteredMask += c;
              }
          };

          
          this.buffer = this.filteredMask.split("").map(function(c, i, array) { 
              return this.tests[i] ? this.getPlaceHolder(i) : c 
          }, this);
          this.focusText = this.element.value;
      }
      
      @HostListener('input',['$event']) onInput(e:KeyboardEvent) {
          if (this.svyFormat.lowercase || this.svyFormat.uppercase) {
              if (this.svyFormat.lowercase) {
                  this.element.value = this.element.value.toLowerCase();
              }
              else if (this.svyFormat.uppercase) {
                  this.element.value = this.element.value.toUpperCase();
              }
              return;
          }
          else if (this.mask) {
              this.setCaret(this.checkVal(true));
          }
      }
      
      @HostListener('focus') onFocus() {
          if (!this.mask) return;
          
          this.focusText = this.element.value;
          var pos = this.checkVal(true);
          this.writeBuffer();
          setTimeout( () => this.setCaretOnFocus(pos), 0);
      }
      
      private setCaretOnFocus(pos) {
          if (pos != this.filteredMask.length) {
              this.setCaret(pos);
          }
          else {
              this.setCaret(this.element.selectionStart);
          }
      }
      
      @HostListener('blur') onBlur() {
          if (!this.mask) return;
          
          this.checkVal(true);
          if (this.element.value != this.focusText) {
              this.element.dispatchEvent(new CustomEvent('change', { bubbles: true, detail: { text: () => this.element.value } }))
          }
      }
      
      @HostListener('keypress',['$event']) onKeypress(e:KeyboardEvent) {          
          if (!this.mask) return;

          if (this.ignore) {
              this.ignore = false;
              //Fixes Mac FF bug on backspace
              return (e.keyCode == 8) ? false : null;
          }
          //TODO needed? e = e || window.event;
          var k = e.charCode || e.keyCode || e.which;
          var posBegin = this.element.selectionStart;
          var posEnd = this.element.selectionEnd;

          if (e.ctrlKey || e.altKey || e.metaKey) {//Ignore
              return true;
          } else if ((k >= 32 && k <= 125) || k > 186) {//typeable characters
              var p = this.seekNext(posBegin - 1);
              if (p < this.mask.length) {
                  var c = String.fromCharCode(k);
                  if (this.converters[p]) {
                      c = this.converters[p](c);
                  }
                  if (this.tests[p].test(c)) {
//                    shiftR(p);
                      this.buffer[p] = c;
                      this.writeBuffer();
                      var next = this.seekNext(p);
                      this.setCaret(next);
                      if (this.settings['completed'] && next == this.mask.length)
                          this.settings['completed'].call(this.element);
                  }
              }
          }
          return false;
      }
      
      @HostListener('keydown',['$event']) onKeydown(e:KeyboardEvent) {
          if (!this.mask) return;
          
          var iPhone = (window.orientation != undefined);
          this.focusText = this.element.value;
          var posBegin = this.element.selectionStart;
          var posEnd = this.element.selectionEnd;
          var k = e.keyCode;
          this.ignore = (k < 16 || (k > 16 && k < 32) || (k > 32 && k < 41));
          if (k == 37) {
          var nextValidChar = this.seekPrevious(posBegin);
          if (nextValidChar != posBegin) {
              this.setCaret(nextValidChar);
              return false;
          }
          return;
          } else if (k == 39) {
              var nextValidChar = this.seekNext(posBegin);
              if (nextValidChar != posBegin) {
                  this.setCaret(nextValidChar);
                  return false;
              }
              return;
          }
          var nextValidChar = this.seekNext(posBegin - 1) - posBegin;
          if (nextValidChar > 0) {
              posBegin += nextValidChar;
              posEnd += nextValidChar;
          }
          
          //backspace, delete, and escape get special treatment
          if (k == 8 || k == 46 || (iPhone && k == 127)) {
              if (posBegin == posEnd) {
                  this.clear(posBegin + (k == 46 ? 0 : -1),(k == 46 ? 1 : 0));
              } else {
                  this.clearBuffer(posBegin, posEnd);
                  this.writeBuffer();
                  this.setCaret(Math.max(this.firstNonMaskPos, posBegin));
              }
              return false;
          } else if (k == 27) {//escape
              this.element.value = this.focusText;
              this.setCaret(0, this.checkVal());
              return false;
          } else if (posBegin != posEnd && !this.ignore) {
              this.clearBuffer(posBegin, posEnd);
          }
      }
      
      private setCaret(begin: number, end?: number) {
          if (this.element.value.length == 0) return;
          if (typeof begin == 'number') {
              end = (typeof end == 'number') ? end : begin;
                  if (this.element != null) {
                        if (this.element['createTextRange']) {
                            var range = this.element['createTextRange']();
                            range.move('character', begin);
                            range.select();
                        } else { 
                            if (this.element.selectionStart) {
                                this.element.focus();
                                this.element.setSelectionRange(begin, end);
                            } else 
                                this.element.focus();
                        }
                  }
          } else {
              if (this.element['setSelectionRange']) {
                  begin = this.element.selectionStart;
                  end = this.element.selectionEnd;
              } 
              else if (document.getSelection() && document.getSelection()['createRange']) {
                  var range = document.getSelection()['createRange']();
                  begin = 0 - range.duplicate().moveStart('character', -100000);
                  end = begin + range.text.length;
              }
              return { begin: begin, end: end };
          }
      }
      
      private seekPrevious(pos : number) : number {
          while (--pos >= 0 && !this.tests[pos]);
          return pos;
      }
       
      private seekNext(pos : number) : number{
          while (++pos <= this.mask.length && !this.tests[pos]);
          return pos;
      }
      
      private clear(pos,caretAddition) {
          while (!this.tests[pos] && --pos >= 0);
          if (this.tests[pos]) {
              this.buffer[pos] = this.getPlaceHolder(pos);
          }
          this.writeBuffer();
          if (caretAddition != 0) {
              var nextPos = pos + caretAddition;
              while ( nextPos >=0 && nextPos < this.mask.length ) {
                  if (this.tests[nextPos]) {
                      pos = nextPos;
                      break;
                  }
                  nextPos = nextPos + caretAddition;
              }
          }
          this.setCaret(Math.max(this.firstNonMaskPos, pos));
      }
      
      private clearBuffer(start: number, end: number) {
          for (var i = start; i < end && i < this.mask.length; i++) {
              if (this.tests[i])
                  this.buffer[i] = this.getPlaceHolder(i);
          }
      }

      private writeBuffer() : string { 
          this.element.value = this.buffer.join('');
          return this.element.value;
      }
      
      private checkVal(allow = false) : number {
          //try to place characters where they belong
          var partialPosition = this.mask.length;
          var test = this.element.value;
          var lastMatch = -1;
          var firstError = -1;
          for (var i = 0, pos = 0; i < this.mask.length; i++) {
              if (this.tests[i]) {
                  this.buffer[i] = this.getPlaceHolder(i);
                  while (pos++ < test.length) {
                      var c = test.charAt(pos - 1);
                      // if the char is the place holder then dont shift..
                      if (c == this.buffer[i]) {
                         if (firstError == -1) firstError = i;
                         break;
                      }
                      if (this.tests[i].test(c)) {
                          this.buffer[i] = c;
                          lastMatch = i;
                          break;
                      }
                  }
                  if (pos > test.length)
                      break;
              } else if (this.buffer[i] == test.charAt(pos) && i!= partialPosition) {
                  pos++;
//                    lastMatch = i;
              } 
          }
          if (!allow && lastMatch + 1 < partialPosition) {
              this.element.value = "";
              this.clearBuffer(0, this.mask.length);
          } else if (allow && lastMatch == -1) {
              this.element.value = "";
              this.clearBuffer(0, this.mask.length);
          } else if (allow || lastMatch + 1 >= partialPosition) {
              this.writeBuffer();
              if (!allow) this.element.value = this.element.value.substring(0, lastMatch + 1);
          }
          return firstError != -1 ? firstError:(partialPosition ? i : this.firstNonMaskPos);
      }
      
      private getPlaceHolder(i:number) : any
      {
          return this.settings['placeholder'].length > 1 ? this.settings['placeholder'].charAt(i) : this.settings['placeholder'];
      }
  }

export class Format {
    display : string = null;
    uppercase:boolean = false;
    lowercase: boolean = false;
    type : string = null;
    isMask : boolean = false;
    edit : string = null;
    placeHolder: string = '';
    allowedCharacters : string = '';
}