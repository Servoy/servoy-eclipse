import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Renderer2, SimpleChange, SimpleChanges } from '@angular/core';

import { ServoyDefaultRadio } from './radio';
import { TooltipDirective, SabloTabseq, ServoyPublicService, FormattingService, TooltipService } from '@servoy/public';
import { ServoyPublicServiceTestingImpl } from '@servoy/public';
import { FormsModule } from '@angular/forms';

function change(current: any, previous: any = undefined, first = false): SimpleChange {
    return new SimpleChange(previous, current, first);
}

describe('ServoyDefaultRadio', () => {
    let component: ServoyDefaultRadio;
    let fixture: ComponentFixture<ServoyDefaultRadio>;

    beforeEach(async () => {
        TestBed.configureTestingModule({
            imports: [ServoyDefaultRadio, TooltipDirective, SabloTabseq, FormsModule],
            providers: [FormattingService, TooltipService, { provide: ServoyPublicService, useClass: ServoyPublicServiceTestingImpl }]
        }).compileComponents();
    });

    beforeEach(() => {
        fixture = TestBed.createComponent(ServoyDefaultRadio);
        component = fixture.componentInstance;
        fixture.componentRef.setInput('servoyApi', { getMarkupId: vi.fn(), trustAsHtml: vi.fn(), registerComponent: vi.fn(), unRegisterComponent: vi.fn() } as any);
        fixture.componentRef.setInput('enabled', true);
        fixture.componentRef.setInput('editable', true);
        fixture.detectChanges();
    });

    it('should create', () => {
        expect(component).toBeTruthy();
    });

    it('getFocusElement returns the inner input', () => {
        expect(component.getFocusElement()).toBe(component.input()!.nativeElement);
    });

    it('does not need scrollbar information', () => {
        expect(component.needsScrollbarInformation()).toBe(false);
    });

    describe('getSelectionFromDataprovider', () => {
        it('returns false when there is no dataProviderID', () => {
            component.dataProviderID.set(null);
            expect(component.getSelectionFromDataprovider()).toBe(false);
        });

        it('compares against the valuelist first real value when a valuelist is present', () => {
            fixture.componentRef.setInput('valuelistID', [{ realValue: 5, displayValue: 'five' }] as any);
            component.dataProviderID.set(5);
            fixture.detectChanges();
            expect(component.getSelectionFromDataprovider()).toBe(true);
        });

        it('treats the string "1" as selected when the dataProviderID is a string', () => {
            component.dataProviderID.set('1');
            expect(component.getSelectionFromDataprovider()).toBe(true);
        });

        it('treats the string "0" as not selected', () => {
            component.dataProviderID.set('0');
            // '0' is truthy so it enters the string branch; '0' == '1' is false
            expect(component.getSelectionFromDataprovider()).toBe(false);
        });

        it('treats a positive number as selected', () => {
            component.dataProviderID.set(2);
            expect(component.getSelectionFromDataprovider()).toBe(true);
        });
    });

    describe('setSelectionFromDataprovider', () => {
        it('syncs the selected flag from the dataProviderID', () => {
            component.dataProviderID.set(1);
            component.setSelectionFromDataprovider();
            expect(component.selected).toBe(true);
        });
    });

    describe('itemClicked', () => {
        it('toggles a numeric dataProviderID from 0 to 1 and pushes the update', () => {
            const pushSpy = vi.spyOn(component, 'pushUpdate');
            component.dataProviderID.set(0);
            component.selected = false;
            component.itemClicked({ target: { localName: 'input' } });
            expect(component.dataProviderID()).toBe(1);
            expect(pushSpy).toHaveBeenCalled();
        });

        it('toggles a string dataProviderID from "0" to "1"', () => {
            component.dataProviderID.set('0');
            component.selected = false;
            component.itemClicked({ target: { localName: 'span' } });
            expect(component.dataProviderID()).toBe('1');
        });

        it('uses the valuelist real value when a valuelist is present', () => {
            fixture.componentRef.setInput('valuelistID', [{ realValue: 7, displayValue: 'seven' }] as any);
            fixture.detectChanges();
            component.dataProviderID.set(null);
            component.selected = false;
            component.itemClicked({ target: { localName: 'label' } });
            expect(component.dataProviderID()).toBe(7);
        });

        it('does nothing when already selected', () => {
            const pushSpy = vi.spyOn(component, 'pushUpdate');
            component.selected = true;
            component.itemClicked({ target: { localName: 'input' } });
            expect(pushSpy).not.toHaveBeenCalled();
        });
    });

    describe('setHorizontalAlignmentFlexbox', () => {
        let renderer: Renderer2;
        let el: HTMLElement;
        beforeEach(() => {
            renderer = component.getRenderer();
            el = document.createElement('div');
        });

        it('centers for alignment 0', () => {
            const spy = vi.spyOn(renderer, 'setStyle');
            component.setHorizontalAlignmentFlexbox(el, renderer, 0);
            expect(spy).toHaveBeenCalledWith(el, 'justify-content', 'center');
        });

        it('aligns to flex-end for alignment 4', () => {
            const spy = vi.spyOn(renderer, 'setStyle');
            component.setHorizontalAlignmentFlexbox(el, renderer, 4);
            expect(spy).toHaveBeenCalledWith(el, 'justify-content', 'flex-end');
        });

        it('aligns to flex-start for any other alignment', () => {
            const spy = vi.spyOn(renderer, 'setStyle');
            component.setHorizontalAlignmentFlexbox(el, renderer, 2);
            expect(spy).toHaveBeenCalledWith(el, 'justify-content', 'flex-start');
        });

        it('does not set style for alignment -1', () => {
            const spy = vi.spyOn(renderer, 'setStyle');
            component.setHorizontalAlignmentFlexbox(el, renderer, -1);
            expect(spy).not.toHaveBeenCalled();
        });
    });

    describe('svyOnChanges', () => {
        it('updates selection and checkbox state when dataProviderID changes', () => {
            vi.useFakeTimers();
            component.dataProviderID.set(1);
            component.svyOnChanges({ dataProviderID: change(1) } as SimpleChanges);
            vi.advanceTimersByTime(0);
            expect(component.selected).toBe(true);
            expect(component.input()!.nativeElement.checked).toBe(true);
            vi.useRealTimers();
        });
    });

    describe('attachHandlers', () => {
        it('calls itemClicked and onActionMethodID on click when enabled and not readOnly', () => {
            const action = vi.fn();
            fixture.componentRef.setInput('onActionMethodID', action);
            fixture.componentRef.setInput('enabled', true);
            fixture.componentRef.setInput('readOnly', false);
            fixture.detectChanges();
            const itemClickedSpy = vi.spyOn(component, 'itemClicked');
            let clickCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'click') clickCb = cb;
                return () => undefined;
            });
            component.attachHandlers();
            const evt = { target: { localName: 'input' } };
            clickCb(evt);
            expect(itemClickedSpy).toHaveBeenCalledWith(evt);
            expect(action).toHaveBeenCalledWith(evt);
        });

        it('ignores the click when readOnly', () => {
            fixture.componentRef.setInput('readOnly', true);
            fixture.detectChanges();
            const itemClickedSpy = vi.spyOn(component, 'itemClicked');
            let clickCb: (e: any) => void = () => undefined;
            vi.spyOn(component.getRenderer(), 'listen').mockImplementation((_t: any, evt: any, cb: any) => {
                if (evt === 'click') clickCb = cb;
                return () => undefined;
            });
            component.attachHandlers();
            clickCb({ target: { localName: 'input' } });
            expect(itemClickedSpy).not.toHaveBeenCalled();
        });
    });
});
