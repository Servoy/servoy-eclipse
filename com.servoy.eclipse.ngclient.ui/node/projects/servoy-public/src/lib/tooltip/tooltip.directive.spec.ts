import { describe, it, expect, beforeEach, afterEach, vi } from 'vitest';
import {TestBed, ComponentFixture} from '@angular/core/testing';
import {TooltipDirective} from './tooltip.directive';
import {TooltipService} from './tooltip.service';
import {Component, DebugElement, ChangeDetectionStrategy} from '@angular/core';
import {By} from '@angular/platform-browser';
import { WindowRefService } from '../services/windowref.service';
import { ServoyPublicServiceTestingImpl } from '../testing/publictesting.module';
import { ServoyPublicService } from '../services/servoy_public.service';
import { ServoyPublicModule } from '../servoy_public.module';
const mouseEnter: Event = new Event('pointerenter');

@Component({
    template: '<input  type="text" [svyTooltip]="textTooltip">',
    changeDetection: ChangeDetectionStrategy.Eager,
    standalone: true,
    imports: [ServoyPublicModule]
})
class TestTooltipWrapperComponent {
  textTooltip = 'Hi';
}

describe('Directive: Tooltip', () => {
  let component: TestTooltipWrapperComponent;
  let fixture: ComponentFixture<TestTooltipWrapperComponent>;
  let inputEl: DebugElement;

  let directiveInstance: TooltipDirective;

 const service = new ServoyPublicServiceTestingImpl();
  beforeEach(() => {
    TestBed.configureTestingModule({
      imports: [TestTooltipWrapperComponent],
      providers: [TooltipService, WindowRefService,
        { provide: ServoyPublicService, useValue: service }]
    });

    fixture = TestBed.createComponent(TestTooltipWrapperComponent);
    component = fixture.componentInstance;
    inputEl = fixture.debugElement.query(By.css('input'));

    directiveInstance = inputEl.injector.get(TooltipDirective);
    fixture.detectChanges();
  });

  afterEach(() => {
    fixture.destroy();
    component = null as any;
  });

  it('should not display when text is undefined', () => {
    component.textTooltip = undefined as any;
    fixture.detectChanges();
    inputEl.nativeElement.dispatchEvent(mouseEnter);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);
  });

  it('should display when text is present', () => {
    component.textTooltip = 'Hi'; fixture.detectChanges();
    fixture.detectChanges();
    expect(directiveInstance.tooltipText()).toBe('Hi');
  });

  it('show tooltip on requestedDelay', () => {
    vi.useFakeTimers();
    vi.advanceTimersByTime(0);
    component.textTooltip = 'Hi'; fixture.detectChanges();
    inputEl.nativeElement.dispatchEvent(mouseEnter);

    vi.advanceTimersByTime(200);
    assertTooltipInstance(directiveInstance, false);

    vi.advanceTimersByTime(800);
    assertTooltipInstance(directiveInstance, true);

    vi.advanceTimersByTime(6000);
    assertTooltipInstance(directiveInstance, false);

    vi.useRealTimers();
  });

  it('close tooltip after requestedDelay', () => {
    vi.useFakeTimers();
    vi.advanceTimersByTime(0);
    component.textTooltip = 'Hi'; fixture.detectChanges();
    inputEl.nativeElement.dispatchEvent(mouseEnter);

    vi.advanceTimersByTime(6000);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);
    vi.useRealTimers();
  });

  it('show when hover over input', () => {
    vi.useFakeTimers();
    vi.advanceTimersByTime(0);
    component.textTooltip = 'Hi'; fixture.detectChanges();
    inputEl.nativeElement.dispatchEvent(mouseEnter);

    vi.advanceTimersByTime(200);
    assertTooltipInstance(directiveInstance, false);

    vi.advanceTimersByTime(800);
    assertTooltipInstance(directiveInstance, true);

    vi.advanceTimersByTime(60000);
    assertTooltipInstance(directiveInstance, false);

    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);

    vi.useRealTimers();
  });

  it('isTooltipActiveSignal reflects tooltip state', () => {
    const tooltipService = TestBed.inject(TooltipService);

    expect(tooltipService.isTooltipActiveSignal()).toBe(false);

    tooltipService.isTooltipActive.next(true);
    expect(tooltipService.isTooltipActiveSignal()).toBe(true);

    tooltipService.isTooltipActive.next(false);
    expect(tooltipService.isTooltipActiveSignal()).toBe(false);
  });

  describe('should destroy', () => {
    beforeEach(() => {
      initTooltip(directiveInstance, inputEl);
      fixture.detectChanges();
    });

    it('should destroy on mouseout', () => {
      inputEl.triggerEventHandler('mouseout', null);
      assertTooltipInstance(directiveInstance, false);
    });

    it('should destroy on click', () => {
      inputEl.triggerEventHandler('click', null);
      assertTooltipInstance(directiveInstance, false);

    });

    it('should destroy on right-click', () => {
      inputEl.triggerEventHandler('contextmenu', null);
      assertTooltipInstance(directiveInstance, false);
    });

    it('should destroy on onDestroy', () => {
      (directiveInstance as any).ngOnDestroy?.();
      assertTooltipInstance(directiveInstance, false);
    });
  });


  /** Asserts whether a tooltip directive has a tooltip instance. */
  function assertTooltipInstance(tooltip: TooltipDirective, shouldExist: boolean): void {
    expect(tooltip.isActive).toBe(shouldExist);
  }

  /**Create tooltip and add 800 delay in order to really display tooltip. */
   function initTooltip(directiveInstance: TooltipDirective, inputEl: DebugElement) {
    component.textTooltip = 'Him';
    fixture.detectChanges();
    inputEl.nativeElement.dispatchEvent(mouseEnter);
  }
});
