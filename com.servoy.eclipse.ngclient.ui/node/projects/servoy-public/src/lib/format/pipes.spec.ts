import { describe, it, expect, vi } from 'vitest';
import { DomSanitizer } from '@angular/platform-browser';
import { MnemonicletterFilterPipe, NotNullOrEmptyPipe, HtmlFilterPipe, TrustAsHtmlPipe } from './pipes';
import { EmptyValueFilterPipe } from './emptyvalue.pipe';

describe('format pipes', () => {

    describe('MnemonicletterFilterPipe', () => {
        const pipe = new MnemonicletterFilterPipe();

        it('should underline the first occurrence of the letter', () => {
            expect(pipe.transform('Save', 'a')).toBe('S<u>a</u>ve');
        });

        it('should return the input unchanged when no letter is given', () => {
            expect(pipe.transform('Save', '')).toBe('Save');
        });

        it('should return the input unchanged when input is empty', () => {
            expect(pipe.transform('', 'a')).toBe('');
        });
    });

    describe('NotNullOrEmptyPipe', () => {
        const pipe = new NotNullOrEmptyPipe();

        it('should keep items with a non-empty realValue', () => {
            const list = [{ realValue: 'x', displayValue: null }, { realValue: null, displayValue: null }];
            const result = pipe.transform(list);
            expect(result).toContain(list[0]);
        });

        it('should keep items with a non-empty displayValue even if realValue is empty', () => {
            const item = { realValue: null, displayValue: 'shown' };
            const result = pipe.transform([item]);
            expect(result).toContain(item);
        });

        it('should return the value unchanged when it is falsy', () => {
            expect(pipe.transform(null as any)).toBeNull();
        });
    });

    describe('HtmlFilterPipe', () => {
        const pipe = new HtmlFilterPipe();

        it('should extract the body content', () => {
            const html = '<html><body>hello</body></html>';
            expect(pipe.transform(html)).toBe('hello');
        });

        it('should include attributes on the body tag past the 6-char offset', () => {
            const html = '<html><body class="x">content</body></html>';
            // indexOf('<body')+6 lands right after '<body' and the space, keeping ' class...'
            expect(pipe.transform(html)).toContain('content');
        });

        it('should return the input unchanged when there is no body', () => {
            expect(pipe.transform('just text')).toBe('just text');
        });

        it('should return non-string input unchanged', () => {
            expect(pipe.transform(null as any)).toBeNull();
        });
    });

    describe('TrustAsHtmlPipe', () => {
        it('should call bypassSecurityTrustHtml when trustAsHtml is true', () => {
            const sanitizer = { bypassSecurityTrustHtml: vi.fn().mockReturnValue('TRUSTED') } as unknown as DomSanitizer;
            const pipe = new TrustAsHtmlPipe(sanitizer);
            expect(pipe.transform('<b>hi</b>', true)).toBe('TRUSTED');
            expect(sanitizer.bypassSecurityTrustHtml).toHaveBeenCalledWith('<b>hi</b>');
        });

        it('should return the raw input when trustAsHtml is false', () => {
            const sanitizer = { bypassSecurityTrustHtml: vi.fn() } as unknown as DomSanitizer;
            const pipe = new TrustAsHtmlPipe(sanitizer);
            expect(pipe.transform('<b>hi</b>', false)).toBe('<b>hi</b>');
            expect(sanitizer.bypassSecurityTrustHtml).not.toHaveBeenCalled();
        });

        it('should return the raw input when input is empty even if trust is true', () => {
            const sanitizer = { bypassSecurityTrustHtml: vi.fn() } as unknown as DomSanitizer;
            const pipe = new TrustAsHtmlPipe(sanitizer);
            expect(pipe.transform('', true)).toBe('');
            expect(sanitizer.bypassSecurityTrustHtml).not.toHaveBeenCalled();
        });
    });

    describe('EmptyValueFilterPipe', () => {
        const pipe = new EmptyValueFilterPipe();

        it('should return a non-breaking space for empty string', () => {
            expect(pipe.transform('')).toBe('\u00A0');
        });

        it('should return the input unchanged otherwise', () => {
            expect(pipe.transform('value')).toBe('value');
            expect(pipe.transform(5)).toBe(5);
        });
    });
});
