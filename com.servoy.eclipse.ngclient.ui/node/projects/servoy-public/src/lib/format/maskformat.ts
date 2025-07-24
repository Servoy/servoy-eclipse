
import { Inject, Renderer2, DOCUMENT } from '@angular/core';
import { Format, FormattingService } from './formatting.service';

const MASK_CONST = {
    // Predefined character definitions
    definitions: {
        '#': '[0-9]',
        0: '[0-9]',
        U: '[A-Z]',
        L: '[a-z]',
        A: '[A-Za-z0-9]',
        '?': '[A-Za-z]',
        '*': '.',
        H: '[A-F0-9]'
    },
    converters: {
        U(c) {
return c.toUpperCase();
},
        L(c) {
return c.toLowerCase();
},
        H(c) {
return c.toUpperCase();
}
    }
};

export class MaskFormat {
    private ignore: boolean;
    private firstNonMaskPos = -1;
    private settings: Object;
    private buffer: string[] = [];
    private tests: RegExp[] = [];
    private converters: any[] = [];
    private mask: string;
    private focusText: string;
    private filteredMask: string;
    
    private listeners = [];
    
    constructor(private format: Format, private _renderer: Renderer2, private element: HTMLInputElement,
                        private formatService: FormattingService, @Inject(DOCUMENT) private doc: Document) {
        this.ignore = false;
        this.settings = {};
        this.settings['placeholder'] = this.format.placeHolder ? this.format.placeHolder : ' ';
        if (this.format.allowedCharacters)
        this.settings['allowedCharacters'] = this.format.allowedCharacters;

        if (!this.settings['completed']) {
            this.settings['completed'] = null;
        }

        this.mask = this.format.edit;
//          if (!this.mask && this.element.value.length > 0) { //TODO check condition
//              return this.buffer.map(function(c, i) { //TODO why return??
//                  return this.tests[i] ? c : null;
//              }, this).join('');
//          }

        let skipNextMask = false;
        this.filteredMask = '';
        const defs = MASK_CONST.definitions;
        const converts = MASK_CONST.converters;
        this.converters = [];
        let partialPosition = this.mask.length;
        let len = this.mask.length;

        const chars = this.mask.split('');
        for (let i = 0 ; i < chars.length; i++) {
            const c = chars[i];
//            if (c == '?') {
//                len--;
//                partialPosition = i;
//            } else
            if (!skipNextMask && c === '\'') {
                skipNextMask = true;
                len--;
                partialPosition--;
            } else {
                if (!skipNextMask && defs[c]) {
                    if (c === '*' && this.settings['allowedCharacters']) {
                        this.tests.push(new RegExp('[' + this.settings['allowedCharacters'] + ']'));
                    } else {
                        this.tests.push(new RegExp(defs[c]));
                    }
                    if (this.firstNonMaskPos === -1)
                        this.firstNonMaskPos =  this.tests.length - 1;
                } else {
                    this.tests.push(null);
                    skipNextMask = false;
                }
                this.converters.push(converts[c]);
                this.filteredMask += c;
            }
        }


        this.buffer = this.filteredMask.split('').map(function(c, i, array) {
            return this.tests[i] ? this.getPlaceHolder(i) : c;
        }, this);

        this.listeners.push(this._renderer.listen(this.element, 'input', () => {
            this.setCaret(this.checkVal(true));
        }));
        this.listeners.push(this._renderer.listen(this.element, 'focus', () => this.onFocus()));
        this.listeners.push(this._renderer.listen(this.element, 'blur', () => this.onBlur()));
        this.listeners.push(this._renderer.listen(this.element, 'keypress', (event) => this.onKeypress(event)));
        this.listeners.push(this._renderer.listen(this.element, 'keydown', (event) => this.onKeydown(event)));
    }

    private onFocus() {
        this.focusText = this.element.value;
        const pos = this.checkVal(true);
        this.writeBuffer();
        setTimeout( () => this.setCaretOnFocus(pos), 0);
    }

    private setCaretOnFocus(pos) {
        if (pos !== this.filteredMask.length) {
            this.setCaret(pos);
        } else {
            this.setCaret(this.element.selectionStart);
        }
    }

    private onBlur() {
        this.checkVal(true);
        if (this.element.value !== this.focusText) {
            this.element.dispatchEvent(new CustomEvent('change', { bubbles: true}));
        }
    }

    private onKeypress(e) {
        if (this.formatService.testKeyPressed(e, 13) && e.target.tagName.toUpperCase() === 'INPUT') {
            //do not looses focus, just apply the format and push value
            this.element.dispatchEvent(new CustomEvent('change', { bubbles: true}));
            return true;
        }
        if (this.ignore) {
            this.ignore = false;
            // Fixes Mac FF bug on backspace
            return (e.keyCode === 8) ? false : null;
        }
        // TODO needed? e = e || window.event;
        const k = e.charCode || e.keyCode || e.which;
        const posBegin = this.element.selectionStart;
        const posEnd = this.element.selectionEnd;

        if (e.ctrlKey || e.altKey || e.metaKey) {// Ignore
            return true;
        } else if ((k >= 32 && k <= 125) || k > 186) {// typeable characters
            const p = this.seekNext(posBegin - 1);
            if (p < this.mask.length) {
                let c = String.fromCharCode(k);
                if (this.converters[p]) {
                    c = this.converters[p](c);
                }
                if (this.tests[p].test(c)) {
//                    shiftR(p);
                    this.buffer[p] = c;
                    this.writeBuffer();
                    const next = this.seekNext(p);
                    this.setCaret(next);
                    if (this.settings['completed'] && next == this.mask.length)
                        this.settings['completed'].call(this.element);
                }
            }
        }
        return false;
    }

    private onKeydown(e: KeyboardEvent) {
        const iPhone = (window.orientation !== undefined);
        let posBegin = this.element.selectionStart;
        let posEnd = this.element.selectionEnd;
        const k = e.keyCode;
        this.ignore = (k < 16 || (k > 16 && k < 32) || (k > 32 && k < 41));
        if (k === 37) {
            const nextValidChar = this.seekPrevious(posBegin);
            if (nextValidChar !== posBegin) {
                this.setCaret(nextValidChar);
                return false;
            }
            return;
        } else if (k === 39) {
            const nextValidChar = this.seekNext(posBegin);
            if (nextValidChar !== posBegin) {
                this.setCaret(nextValidChar);
                return false;
            }
            return;
        } else {
            const nextValidChar = this.seekNext(posBegin - 1) - posBegin;
            if (nextValidChar > 0) {
                posBegin += nextValidChar;
                posEnd += nextValidChar;
            }
        }

        // backspace, delete, and escape get special treatment
        if (k === 8 || k === 46 || (iPhone && k === 127)) {
            if (posBegin === posEnd) {
                this.clear(posBegin + (k === 46 ? 0 : -1), (k === 46 ? 1 : 0));
            } else {
                this.clearBuffer(posBegin, posEnd);
                this.writeBuffer();
                this.setCaret(Math.max(this.firstNonMaskPos, posBegin));
            }
            return false;
        } else if (k === 27) {// escape
            this.element.value = this.focusText;
            this.setCaret(0, this.checkVal());
            return false;
        } else if (posBegin !== posEnd && !this.ignore) {
            this.clearBuffer(posBegin, posEnd);
        }
    }

    private setCaret(begin: number, end?: number) {
        if (this.element.value.length === 0) return;
        if (typeof begin === 'number') {
            end = (typeof end === 'number') ? end : begin;
                if (this.element != null) {
                      if (this.element['createTextRange']) {
                          const range = this.element['createTextRange']();
                          range.move('character', begin);
                          range.select();
                      } else if (this.element.selectionStart >= 0) {
                        this.element.setSelectionRange(begin, end);
                      }
                }
        } else {
            if (this.element['setSelectionRange']) {
                begin = this.element.selectionStart;
                end = this.element.selectionEnd;
            } else if (this.doc.getSelection() && this.doc.getSelection()['createRange']) {
                const range = this.doc.getSelection()['createRange']();
                begin = 0 - range.duplicate().moveStart('character', -100000);
                end = begin + range.text.length;
            }
            return { begin, end };
        }
    }

    private seekPrevious(pos: number): number {
        while (--pos >= 0 && !this.tests[pos]);
        return pos;
    }

    private seekNext(pos: number): number {
        while (++pos <= this.mask.length && !this.tests[pos]);
        return pos;
    }

    private clear(pos, caretAddition) {
        while (!this.tests[pos] && --pos >= 0);
        if (this.tests[pos]) {
            this.buffer[pos] = this.getPlaceHolder(pos);
        }
        this.writeBuffer();
        if (caretAddition !== 0) {
            let nextPos = pos + caretAddition;
            while ( nextPos >= 0 && nextPos < this.mask.length ) {
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
        for (let i = start; i < end && i < this.mask.length; i++) {
            if (this.tests[i])
                this.buffer[i] = this.getPlaceHolder(i);
        }
    }

    private writeBuffer(): string {
        this.element.value = this.buffer.join('');
        return this.element.value;
    }

    private checkVal(allow = false): number {
        // try to place characters where they belong
        const partialPosition = this.mask.length;
        const test = this.element.value;
        let lastMatch = -1;
        let firstError = -1;
        let i = 0;
        for (let pos = 0; i < this.mask.length; i++) {
            if (this.tests[i]) {
                this.buffer[i] = this.getPlaceHolder(i);
                while (pos++ < test.length) {
                    const c = test.charAt(pos - 1);
                    // if the char is the place holder then dont shift..
                    if (c === this.buffer[i]) {
                       if (firstError === -1) firstError = i;
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
            } else if (this.buffer[i] === test.charAt(pos) && i !== partialPosition) {
                pos++;
//                    lastMatch = i;
            }
        }
        if (!allow && lastMatch + 1 < partialPosition) {
            this.element.value = '';
            this.clearBuffer(0, this.mask.length);
        } else if (allow && lastMatch === -1) {
            this.element.value = '';
            this.clearBuffer(0, this.mask.length);
        } else if (allow || lastMatch + 1 >= partialPosition) {
            this.writeBuffer();
            if (!allow) this.element.value = this.element.value.substring(0, lastMatch + 1);
        }
        return firstError !== -1 ? firstError : (partialPosition ? i : this.firstNonMaskPos);
    }

    private getPlaceHolder(i: number): any {
        return this.settings['placeholder'].length > 1 ? this.settings['placeholder'].charAt(i) : this.settings['placeholder'];
    }
    
    public destroy(){
         this.listeners.forEach(unregisterFunction => unregisterFunction());
         this.listeners = [];
    }
}

