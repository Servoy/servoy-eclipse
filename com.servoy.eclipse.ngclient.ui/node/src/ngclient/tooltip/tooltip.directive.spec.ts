import {TestBed, ComponentFixture, tick, fakeAsync} from '@angular/core/testing';
import {TooltipDirective} from './tooltip.directive';
import {TooltipService} from './tooltip.service';
import {Component, DebugElement} from '@angular/core';
import {By} from "@angular/platform-browser";

@Component({
  template: '<input  type="text" [svyTooltip]="textTooltip">',
})
class TestTooltipWrapperComponent{
  textTooltip = "Hi";
}

describe('Directive: Tooltip', () => {
    let component: TestTooltipWrapperComponent;
    let fixture: ComponentFixture<TestTooltipWrapperComponent>;
    let inputEl: DebugElement;

  let directiveInstance: TooltipDirective;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [TooltipDirective,TestTooltipWrapperComponent],
      providers: [TooltipService]
    });

    fixture = TestBed.createComponent(TestTooltipWrapperComponent);
    component = fixture.componentInstance;
    inputEl = fixture.debugElement.query(By.css('input'));

    directiveInstance = inputEl.injector.get(TooltipDirective);
  });

  afterEach( () => {
    fixture.destroy();
    component = null;
  });

  it('should not display when text is undefined', ()=> {
    directiveInstance.tooltipText = undefined;
    directiveInstance.onMouseEnter(this);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);
  });

  it('should display when text is present' , () => {
    directiveInstance.tooltipText = "Hi";
    fixture.detectChanges();
    expect(directiveInstance.tooltipText).toBe('Hi');
  });

  it('show tooltip on requestedDelay', fakeAsync(() => {
    tick();
    directiveInstance.tooltipText = "Hi";
    inputEl.triggerEventHandler('mouseenter', null );

    tick(200);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);

    tick(800);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, true);

    tick(6000);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);

  }));

  it('close tooltip after requestedDelay', fakeAsync(() => {
    tick();
    directiveInstance.tooltipText = "Hi";
    inputEl.triggerEventHandler('mouseenter', null );

    tick(6000);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);
  }));

  it('show when hover over input', fakeAsync (() => {
    tick();
    directiveInstance.tooltipText = "Hi";

    inputEl.triggerEventHandler('mouseenter', null );
    tick(200);
    assertTooltipInstance(directiveInstance, false);

    tick(800);
    assertTooltipInstance(directiveInstance, true);

    tick(60000);
    assertTooltipInstance(directiveInstance, false);

    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);

  }));

  it('should destroy on mouseout' , fakeAsync(() => {
    initTooltipWithDelay(directiveInstance, inputEl);
    assertTooltipInstance(directiveInstance, true);

    inputEl.triggerEventHandler('mouseout', null );
    assertTooltipInstance(directiveInstance, false);
  }));

  it('should destroy on click' , fakeAsync(() => {
    initTooltipWithDelay(directiveInstance, inputEl);
    assertTooltipInstance(directiveInstance, true);

    inputEl.triggerEventHandler('click', null );
    assertTooltipInstance(directiveInstance, false);

  }));

  it('should destroy on right-click' , fakeAsync(() => {
    initTooltipWithDelay(directiveInstance, inputEl);
    assertTooltipInstance(directiveInstance, true);

    inputEl.triggerEventHandler('contextmenu', null );
    assertTooltipInstance(directiveInstance, false);
  }));

  it('should destroy on onDestroy' , fakeAsync(() => {
    initTooltipWithDelay(directiveInstance, inputEl);
    assertTooltipInstance(directiveInstance, true);
    directiveInstance.ngOnDestroy();
    assertTooltipInstance(directiveInstance, false);
  }));

  /** Asserts whether a tooltip directive has a tooltip instance. */
  function assertTooltipInstance(tooltip: TooltipDirective, shouldExist: boolean): void {
    expect(tooltip.isActive).toBe(shouldExist);
  }

  /**Create tooltip and add 800 delay in order to really display tooltip. */
  function initTooltipWithDelay(directiveInstance: TooltipDirective, inputEl: DebugElement) {
    directiveInstance.tooltipText = "Him";
    inputEl.triggerEventHandler('mouseenter', null);
    tick(800);
  }
});
