import { describe, it, expect, beforeEach } from 'vitest';
import { Component } from '@angular/core';
import { TestBed, ComponentFixture } from '@angular/core/testing';
import { By } from '@angular/platform-browser';
import { SabloTabseq, SabloTabseqConfig } from './sablotabseq.directive';

/**
 * Host renders a root tab-seq container (design seq 1, so its children get real
 * positive runtime tabindexes) with children that each carry a design tab
 * sequence. This exercises the real register/recalculate flow that assigns
 * runtime tabindex attributes down the tree.
 */
@Component({
    standalone: true,
    imports: [SabloTabseq],
    template: `
        <div [sabloTabseq]="1" [sabloTabseqConfig]="rootConfig" id="root">
            <input [sabloTabseq]="a" id="childA">
            <input [sabloTabseq]="b" id="childB">
            <input [sabloTabseq]="c" id="childC">
        </div>
    `
})
class HostComponent {
    rootConfig: SabloTabseqConfig = { root: true };
    a = 1;
    b = 2;
    c = 3;
}

const tabIndexOf = (fixture: ComponentFixture<HostComponent>, id: string): number =>
    Number(fixture.debugElement.query(By.css('#' + id)).nativeElement.getAttribute('tabindex'));

describe('Directive: SabloTabseq', () => {
    let fixture: ComponentFixture<HostComponent>;

    beforeEach(() => {
        TestBed.configureTestingModule({ imports: [HostComponent] });
        fixture = TestBed.createComponent(HostComponent);
    });

    it('should assign increasing runtime tab indexes to the children in design order', () => {
        fixture.detectChanges();
        const a = tabIndexOf(fixture, 'childA');
        const b = tabIndexOf(fixture, 'childB');
        const c = tabIndexOf(fixture, 'childC');
        expect(a).toBeGreaterThan(0);
        expect(b).toBeGreaterThan(a);
        expect(c).toBeGreaterThan(b);
    });

    it('should keep children ordered by their design tab sequence', () => {
        fixture.detectChanges();
        const a = tabIndexOf(fixture, 'childA');
        const b = tabIndexOf(fixture, 'childB');
        const c = tabIndexOf(fixture, 'childC');
        // design order 1,2,3 must map to a strictly increasing runtime sequence
        expect(a).toBeLessThan(b);
        expect(b).toBeLessThan(c);
    });

    it('should mark a child excluded (design -2) so it is skipped from the sequence', () => {
        fixture.componentInstance.b = -2;
        fixture.detectChanges();
        // excluded element gets a -1 runtime tabindex (SKIP)
        expect(fixture.debugElement.query(By.css('#childB')).nativeElement.getAttribute('tabindex')).toBe('-1');
    });

    it('the directive instance exposes the design tab seq and enabled state', () => {
        fixture.detectChanges();
        const dirA = fixture.debugElement.query(By.css('#childA')).injector.get(SabloTabseq);
        expect(dirA.designTabSeq).toBe(1);
        expect(dirA.isEnabled).toBe(true);
        expect(dirA.hasOwnTabIndex()).toBe(true);
    });

    it('disableTabseq / enableTabseq events toggle the enabled flag', () => {
        fixture.detectChanges();
        const childA = fixture.debugElement.query(By.css('#childA')).nativeElement;
        const dirA = fixture.debugElement.query(By.css('#childA')).injector.get(SabloTabseq);

        childA.dispatchEvent(new CustomEvent('disableTabseq', { detail: true }));
        expect(dirA.isEnabled).toBe(false);

        childA.dispatchEvent(new CustomEvent('enableTabseq', { detail: true }));
        expect(dirA.isEnabled).toBe(true);
    });

    it('a root container does not set its own tabindex', () => {
        fixture.detectChanges();
        const root = fixture.debugElement.query(By.css('#root')).injector.get(SabloTabseq);
        expect(root.hasOwnTabIndex()).toBe(false);
    });

    it('should unregister a child from the parent on destroy without errors', () => {
        fixture.detectChanges();
        expect(() => fixture.destroy()).not.toThrow();
    });
});

describe('Directive: SabloTabseq with equal design indexes', () => {
    @Component({
        standalone: true,
        imports: [SabloTabseq],
        template: `
            <div [sabloTabseq]="1" [sabloTabseqConfig]="rootConfig" id="root">
                <input [sabloTabseq]="1" id="c1">
                <input [sabloTabseq]="1" id="c2">
                <input [sabloTabseq]="2" id="c3">
            </div>
        `
    })
    class DupHost {
        rootConfig: SabloTabseqConfig = { root: true };
    }

    it('should handle duplicate design indexes as sibling runtime indexes', () => {
        TestBed.configureTestingModule({ imports: [DupHost] });
        const fixture = TestBed.createComponent(DupHost);
        fixture.detectChanges();
        const c1 = Number(fixture.debugElement.query(By.css('#c1')).nativeElement.getAttribute('tabindex'));
        const c2 = Number(fixture.debugElement.query(By.css('#c2')).nativeElement.getAttribute('tabindex'));
        const c3 = Number(fixture.debugElement.query(By.css('#c3')).nativeElement.getAttribute('tabindex'));
        // the two equal-design siblings share the same runtime start index
        expect(c1).toBe(c2);
        // the design index 2 child comes after them
        expect(c3).toBeGreaterThan(c1);
    });
});
