import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { ServoyPublicModule } from 'servoy-public';

import { ServoyCoreSlider } from './slider';
import { TooltipService } from 'servoy-public';


describe('ServoyCoreSlider', () => {
  let component: ServoyCoreSlider;
  let fixture: ComponentFixture<ServoyCoreSlider>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyCoreSlider],
      imports: [ServoyTestingModule, ServoyPublicModule],
      providers: [TooltipService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyCoreSlider);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
