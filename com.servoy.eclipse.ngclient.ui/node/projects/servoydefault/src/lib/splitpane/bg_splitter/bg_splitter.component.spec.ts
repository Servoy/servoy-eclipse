import { TestBed, ComponentFixture } from '@angular/core/testing';
import { describe, it, expect, beforeEach, vi } from 'vitest';
import { Component, provideZonelessChangeDetection, signal } from '@angular/core';
import { BGSplitter } from './bg_splitter.component';
import { BGPane } from './bg_pane.component';

@Component({
    selector: 'test-splitter-host',
    template: `
        <bg-splitter [orientation]="orientation()" [divSize]="divSize()" [divLocation]="divLocation()" (onDividerChange)="onChange($event)">
            <bg-pane [minSize]="10"></bg-pane>
            <bg-pane [minSize]="10"></bg-pane>
        </bg-splitter>
    `,
    standalone: true,
    imports: [BGSplitter, BGPane],
})
class TestSplitterHostComponent {
    orientation = signal('horizontal');
    divSize = signal<any>(5);
    divLocation = signal<any>(0.5);
    changes: any[] = [];
    onChange(value: any) {
        this.changes.push(value);
    }
}

describe('BGSplitter', () => {
    let fixture: ComponentFixture<TestSplitterHostComponent>;
    let host: TestSplitterHostComponent;

    beforeEach(() => {
        TestBed.configureTestingModule({
            imports: [TestSplitterHostComponent],
            providers: [provideZonelessChangeDetection()],
        });
        fixture = TestBed.createComponent(TestSplitterHostComponent);
        host = fixture.componentInstance;
    });

    function getSplitter(): BGSplitter {
        return fixture.debugElement.children[0].componentInstance as BGSplitter;
    }

    it('creates and renders with a split-handler inserted', () => {
        expect(() => fixture.detectChanges()).not.toThrow();
        const handler = fixture.nativeElement.querySelector('.split-handler');
        expect(handler).toBeTruthy();
    });

    it('adds the orientation class to the element', () => {
        fixture.detectChanges();
        const panesContainer = fixture.nativeElement.querySelector('.split-panes');
        expect(panesContainer.classList.contains('horizontal')).toBe(true);
    });

    it('applies the divSize to the handler width for horizontal orientation', () => {
        fixture.detectChanges();
        const handler = fixture.nativeElement.querySelector('.split-handler');
        expect(handler.style.width).toBe('5px');
    });

    it('applies the divSize to the handler height for vertical orientation', () => {
        host.orientation.set('vertical');
        host.divSize.set(8);
        fixture.detectChanges();
        const handler = fixture.nativeElement.querySelector('.split-handler');
        expect(handler.style.height).toBe('8px');
    });

    describe('mouseup', () => {
        it('does not emit when not dragging', () => {
            fixture.detectChanges();
            getSplitter().mouseup({});
            expect(host.changes.length).toBe(0);
        });

        it('emits the parsed divider location when a drag ends (horizontal)', () => {
            fixture.detectChanges();
            const splitter: any = getSplitter();
            splitter.drag = true;
            splitter.handler.style.left = '120px';
            splitter.mouseup({});
            expect(host.changes).toEqual([120]);
            expect(splitter.drag).toBe(false);
        });

        it('emits 0 when the handler has no position set', () => {
            fixture.detectChanges();
            const splitter: any = getSplitter();
            splitter.drag = true;
            splitter.handler.style.left = '';
            splitter.mouseup({});
            expect(host.changes).toEqual([0]);
        });
    });

    describe('mousemove', () => {
        it('does nothing when not dragging', () => {
            fixture.detectChanges();
            const splitter: any = getSplitter();
            const adjustSpy = vi.spyOn(splitter as any, 'adjustLocation');
            splitter.mousemove({ clientX: 10 });
            expect(adjustSpy).not.toHaveBeenCalled();
        });
    });
});
