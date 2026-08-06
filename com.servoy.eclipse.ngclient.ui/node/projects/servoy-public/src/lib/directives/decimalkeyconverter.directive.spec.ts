import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { Component, Input, ViewChild, ElementRef, DebugElement, ChangeDetectionStrategy } from '@angular/core';
import { By } from '@angular/platform-browser';

import { ServoyPublicServiceTestingImpl, ServoyPublicTestingModule } from '../testing/publictesting.module';
import { ServoyPublicService } from '../services/servoy_public.service';
import { ServoyPublicModule } from '../servoy_public.module';

@Component({
    template: '<input type="text" [svyDecimalKeyConverter]="format" #element>',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true,
    imports: [ServoyPublicModule]
})
class TestDecimalKeyConverterComponent {
    @Input() format: any;
    @ViewChild('element', { static: true }) elementRef!: ElementRef;
}

describe('Directive: DecimalKeyConverter', () => {
    let component: TestDecimalKeyConverterComponent;
    let fixture: ComponentFixture<TestDecimalKeyConverterComponent>;
    let inputEl: DebugElement;
    const service = new ServoyPublicServiceTestingImpl();

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestDecimalKeyConverterComponent],
            providers: [
                  { provide: ServoyPublicService, useValue: service }
            ]
        });
        fixture = TestBed.createComponent(TestDecimalKeyConverterComponent);
        component = fixture.componentInstance;
        inputEl = fixture.debugElement.query(By.css('input'));

    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('should insert numpad decimal (nl == ,)', () => {
        vi.useFakeTimers();
        service.setLocaleNumberSymbol(',');
        component.format = {
            uppercase: true, allowedCharacters: null, isMask: false, isRaw: false, edit: null,
            display: null, type: 'NUMBER', placeHolder: null, isNumberValidator: false
        };
        fixture.detectChanges();

        component.elementRef.nativeElement.value = '12';
        fixture.detectChanges();

        inputEl.triggerEventHandler('keydown', { keyCode: 110, which: 110 });
        fixture.detectChanges();

        expect(inputEl.nativeElement.value).toEqual('12,');
        service.setLocaleNumberSymbol(null as any);
        vi.useRealTimers();
    });

    it('should insert comma decimal (en == .)', () => {
        vi.useFakeTimers();
        service.setLocaleNumberSymbol('.');
        component.format = {
            uppercase: true, allowedCharacters: null, isMask: false, isRaw: false, edit: null,
            display: null, type: 'NUMBER', placeHolder: null, isNumberValidator: false
        };
        fixture.detectChanges();

        component.elementRef.nativeElement.value = '12';
        fixture.detectChanges();
        vi.advanceTimersByTime(0);

        inputEl.triggerEventHandler('keydown', { keyCode: 110, which: 110 });
        fixture.detectChanges();
        vi.advanceTimersByTime(0);
        expect(inputEl.nativeElement.value).toEqual('12.');
        service.setLocaleNumberSymbol(null as any);
        vi.useRealTimers();
    });
});
