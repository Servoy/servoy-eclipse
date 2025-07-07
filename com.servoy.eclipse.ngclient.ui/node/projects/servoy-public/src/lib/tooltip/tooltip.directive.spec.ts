import {TestBed, ComponentFixture, tick, fakeAsync} from '@angular/core/testing';
import {TooltipDirective} from './tooltip.directive';
import {TooltipService} from './tooltip.service';
import {Component, DebugElement} from '@angular/core';
import {By} from '@angular/platform-browser';
import { WindowRefService } from '../services/windowref.service';
import { ServoyPublicServiceTestingImpl } from '../testing/publictesting.module';
import { ServoyPublicService } from '../services/servoy_public.service';
const mouseEnter: Event = new Event('pointerenter');

@Component({
    template: '<input  type="text" [svyTooltip]="textTooltip">',
    standalone: false
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
      declarations: [TooltipDirective, TestTooltipWrapperComponent],
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
    component = null;
  });

  it('should not display when text is undefined', () => {
    directiveInstance.tooltipText = undefined;
    inputEl.nativeElement.dispatchEvent(mouseEnter);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);
  });

  it('should display when text is present', () => {
    directiveInstance.tooltipText = 'Hi';
    fixture.detectChanges();
    expect(directiveInstance.tooltipText).toBe('Hi');
  });

  it('show tooltip on requestedDelay', fakeAsync(() => {
    tick();
    directiveInstance.tooltipText = 'Hi';
    inputEl.nativeElement.dispatchEvent(mouseEnter);

    tick(200);
    assertTooltipInstance(directiveInstance, false);

    tick(800);
    assertTooltipInstance(directiveInstance, true);

    tick(6000);
    assertTooltipInstance(directiveInstance, false);

  }));

  it('close tooltip after requestedDelay', fakeAsync(() => {
    tick();
    directiveInstance.tooltipText = 'Hi';
    inputEl.nativeElement.dispatchEvent(mouseEnter);

    tick(6000);
    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);
  }));

  it('show when hover over input', fakeAsync(() => {
    tick();
    directiveInstance.tooltipText = 'Hi';
    inputEl.nativeElement.dispatchEvent(mouseEnter);

    tick(200);
    assertTooltipInstance(directiveInstance, false);

    tick(800);
    assertTooltipInstance(directiveInstance, true);

    tick(60000);
    assertTooltipInstance(directiveInstance, false);

    fixture.detectChanges();
    assertTooltipInstance(directiveInstance, false);

  }));

  describe('should destroy', () => {
    beforeEach(() => {
      initTooltip(directiveInstance, inputEl);
      fixture.detectChanges();
    });

    it('should destroy on mouseout', fakeAsync(() => {
      inputEl.triggerEventHandler('mouseout', null);
      assertTooltipInstance(directiveInstance, false);
    }));

    it('should destroy on click', fakeAsync(() => {
      inputEl.triggerEventHandler('click', null);
      assertTooltipInstance(directiveInstance, false);

    }));

    it('should destroy on right-click', fakeAsync(() => {
      inputEl.triggerEventHandler('contextmenu', null);
      assertTooltipInstance(directiveInstance, false);
    }));

    it('should destroy on onDestroy', fakeAsync(() => {
      directiveInstance.ngOnDestroy();
      assertTooltipInstance(directiveInstance, false);
    }));
  });


  /** Asserts whether a tooltip directive has a tooltip instance. */
  function assertTooltipInstance(tooltip: TooltipDirective, shouldExist: boolean): void {
    expect(tooltip.isActive).toBe(shouldExist);
  }

  /**Create tooltip and add 800 delay in order to really display tooltip. */
   function initTooltip(directiveInstance: TooltipDirective, inputEl: DebugElement) {
    directiveInstance.tooltipText = 'Him';
    inputEl.nativeElement.dispatchEvent(mouseEnter);
  }
});
