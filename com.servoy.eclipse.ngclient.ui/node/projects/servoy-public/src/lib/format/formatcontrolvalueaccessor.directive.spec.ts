import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import { Component } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { FormatDirective } from './formatcontrolvalueaccessor.directive';
import { Format, FormattingService } from './formatting.service';
import { ServoyPublicService } from '../services/servoy_public.service';
import { ServoyPublicServiceTestingImpl } from '../testing/publictesting.module';
import numbro from 'numbro';
import languages from 'numbro/dist/languages.min';

@Component({
    standalone: true,
    imports: [FormatDirective],
    template: `
        <input id="text" type="text" [svyFormat]="textFormat" [findmode]="findmode">
        <input id="num" type="text" [svyFormat]="numberFormat" [findmode]="findmode">
        <input id="date" type="text" [svyFormat]="dateFormat" [findmode]="findmode">
        <input id="nativeDate" type="date" [svyFormat]="dateFormat" [findmode]="findmode">
        <input id="nativeNumber" type="number" [svyFormat]="numberFormat" [findmode]="findmode">
    `
})
class HostComponent {
    textFormat: Format = Object.assign(new Format(), { type: 'TEXT', display: 'UUUUUUU' });
    numberFormat: Format = Object.assign(new Format(), { type: 'NUMBER', display: '#.##', edit: '#.##' });
    dateFormat: Format = Object.assign(new Format(), { type: 'DATETIME', display: 'dd-MM-yyyy' });
    findmode = false;
}

const getDirective = (fixture: ComponentFixture<HostComponent>, id: string): FormatDirective =>
    fixture.debugElement.query(By.css('#' + id)).injector.get(FormatDirective);

describe('FormatDirective', () => {
    let fixture: ComponentFixture<HostComponent>;
    const service = new ServoyPublicServiceTestingImpl();

    beforeEach(() => {
        try {
            numbro.registerLanguage(languages['en-GB']);
        } catch { /* already registered */ }
        numbro.setLanguage('en-GB');
        TestBed.configureTestingModule({
            imports: [HostComponent],
            providers: [
                FormattingService,
                { provide: ServoyPublicService, useValue: service }
            ]
        });
        fixture = TestBed.createComponent(HostComponent);
        fixture.detectChanges();
    });

    afterEach(() => {
        vi.useRealTimers();
        fixture.destroy();
    });

    describe('writeValue', () => {
        it('should write a formatted TEXT value to the input', () => {
            const dir = getDirective(fixture, 'text');
            dir.writeValue('hello');
            expect(fixture.debugElement.query(By.css('#text')).nativeElement.value).toBe('HELLO');
        });

        it('should write a formatted NUMBER value to the input', () => {
            const dir = getDirective(fixture, 'num');
            dir.writeValue(12.5);
            expect(fixture.debugElement.query(By.css('#num')).nativeElement.value).toBe('12.5');
        });

        it('should write an empty string for null/undefined without a format', () => {
            const dir = getDirective(fixture, 'text');
            dir.writeValue(null);
            // null with a TEXT format still writes empty string
            expect(fixture.debugElement.query(By.css('#text')).nativeElement.value).toBe('');
        });

        it('should use the native date formatting for type=date inputs', () => {
            const dir = getDirective(fixture, 'nativeDate');
            dir.writeValue(new Date(2024, 2, 5));
            expect(fixture.debugElement.query(By.css('#nativeDate')).nativeElement.value).toBe('2024-03-05');
        });

        it('should write the raw value for a native number input', () => {
            const dir = getDirective(fixture, 'nativeNumber');
            dir.writeValue(42);
            expect(fixture.debugElement.query(By.css('#nativeNumber')).nativeElement.value).toBe('42');
        });
    });

    describe('change handling / setRealValue', () => {
        it('should parse the input value and call the registered change callback', () => {
            const dir = getDirective(fixture, 'num');
            const onChange = vi.fn();
            dir.registerOnChange(onChange);
            const input = fixture.debugElement.query(By.css('#num')).nativeElement as HTMLInputElement;
            input.value = '12.5';
            input.dispatchEvent(new Event('change'));
            expect(onChange).toHaveBeenCalledWith(12.5);
        });

        it('should parse a native number input using the browser number parser', () => {
            const dir = getDirective(fixture, 'nativeNumber');
            const onChange = vi.fn();
            dir.registerOnChange(onChange);
            const input = fixture.debugElement.query(By.css('#nativeNumber')).nativeElement as HTMLInputElement;
            input.value = '77';
            input.dispatchEvent(new Event('change'));
            expect(onChange).toHaveBeenCalledWith(77);
        });

        it('should skip change parsing for native date inputs (handled on blur/enter)', () => {
            const dir = getDirective(fixture, 'nativeDate');
            const onChange = vi.fn();
            dir.registerOnChange(onChange);
            const input = fixture.debugElement.query(By.css('#nativeDate')).nativeElement as HTMLInputElement;
            input.value = '2024-03-05';
            input.dispatchEvent(new Event('change'));
            expect(onChange).not.toHaveBeenCalled();
        });
    });

    describe('dateInputChange', () => {
        it('should convert an ISO value to a Date for native date inputs', () => {
            const dir = getDirective(fixture, 'nativeDate');
            const onChange = vi.fn();
            dir.registerOnChange(onChange);
            dir.dateInputChange('2024-03-05');
            expect(onChange).toHaveBeenCalled();
            const arg = onChange.mock.calls[0][0] as Date;
            expect(arg).toBeInstanceOf(Date);
            expect(arg.getFullYear()).toBe(2024);
        });

        it('should set null when the ISO value is invalid', () => {
            const dir = getDirective(fixture, 'nativeDate');
            const onChange = vi.fn();
            dir.registerOnChange(onChange);
            dir.dateInputChange('not-a-date');
            expect(onChange).toHaveBeenCalledWith(null);
        });

        it('should do nothing for a non-native-date input type', () => {
            const dir = getDirective(fixture, 'text');
            const onChange = vi.fn();
            dir.registerOnChange(onChange);
            dir.dateInputChange('anything');
            expect(onChange).not.toHaveBeenCalled();
        });
    });

    describe('focus / blur lifecycle', () => {
        it('focus should capture the focus text after a tick', () => {
            vi.useFakeTimers();
            const dir = getDirective(fixture, 'num');
            const input = fixture.debugElement.query(By.css('#num')).nativeElement as HTMLInputElement;
            input.value = '12.5';
            dir.focussed();
            vi.advanceTimersByTime(0);
            expect((dir as any).focusText).toBe('12.5');
        });

        it('blur should call the onTouched callback', () => {
            const dir = getDirective(fixture, 'num');
            const onTouched = vi.fn();
            dir.registerOnTouched(onTouched);
            dir.touched(fixture.debugElement.query(By.css('#num')).nativeElement);
            expect(onTouched).toHaveBeenCalled();
        });

        it('blur should ignore targets without a value property', () => {
            const dir = getDirective(fixture, 'num');
            const onTouched = vi.fn();
            dir.registerOnTouched(onTouched);
            dir.touched({} as EventTarget);
            expect(onTouched).not.toHaveBeenCalled();
        });
    });

    describe('keyDown', () => {
        it('should run dateInputChange on Enter for native date inputs', () => {
            const dir = getDirective(fixture, 'nativeDate');
            const spy = vi.spyOn(dir, 'dateInputChange');
            const input = fixture.debugElement.query(By.css('#nativeDate')).nativeElement as HTMLInputElement;
            input.value = '2024-03-05';
            dir.keyDown({ key: 'Enter', target: input } as unknown as KeyboardEvent);
            expect(spy).toHaveBeenCalledWith('2024-03-05');
        });

        it('should not react to non-Enter keys', () => {
            const dir = getDirective(fixture, 'nativeDate');
            const spy = vi.spyOn(dir, 'dateInputChange');
            dir.keyDown({ key: 'a', target: { value: 'x' } } as unknown as KeyboardEvent);
            expect(spy).not.toHaveBeenCalled();
        });
    });

    // these set the format BEFORE the first change detection so the signal input
    // picks up the value (reassigning the host field after init does not reliably
    // re-trigger the directive's input() signal in this OnPush/zoneless setup)
    const buildWith = (format: Format, findmode = false): ComponentFixture<HostComponent> => {
        const f = TestBed.createComponent(HostComponent);
        f.componentInstance.textFormat = format;
        f.componentInstance.findmode = findmode;
        f.detectChanges();
        return f;
    };

    describe('maxlength attribute', () => {
        it('should set a maxlength attribute from the format when not in find mode', () => {
            const f = buildWith(Object.assign(new Format(), { type: 'TEXT', maxLength: 5 }));
            expect(f.debugElement.query(By.css('#text')).nativeElement.getAttribute('maxlength')).toBe('5');
            f.destroy();
        });

        it('should remove the maxlength attribute in find mode', () => {
            const f = buildWith(Object.assign(new Format(), { type: 'TEXT', maxLength: 5 }), true);
            expect(f.debugElement.query(By.css('#text')).nativeElement.getAttribute('maxlength')).toBeNull();
            f.destroy();
        });
    });

    describe('mask format', () => {
        it('should create a MaskFormat when the format is a mask', () => {
            const f = buildWith(Object.assign(new Format(), { type: 'TEXT', isMask: true, edit: '##-##' }));
            const dir = getDirective(f, 'text');
            expect((dir as any).maskFormat).not.toBeNull();
            f.destroy();
        });

        it('should not create a MaskFormat for a non-mask format', () => {
            const f = buildWith(Object.assign(new Format(), { type: 'TEXT', display: 'U' }));
            const dir = getDirective(f, 'text');
            expect((dir as any).maskFormat).toBeNull();
            f.destroy();
        });
    });
});
