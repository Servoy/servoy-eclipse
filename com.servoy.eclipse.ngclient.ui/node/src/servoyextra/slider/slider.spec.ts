import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormattingService } from '../../ngclient/servoy_public';

import { ServoyExtraSlider } from './slider';
import { NgxSliderModule } from '@angular-slider/ngx-slider';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';

describe('ServoyExtraSlider', () => {
  let component: ServoyExtraSlider;
  let fixture: ComponentFixture<ServoyExtraSlider>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyExtraSlider ],
 	  imports: [ NgxSliderModule, ServoyPublicModule],
 	  providers: [FormattingService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyExtraSlider);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
	component.ceil = 0;
	component.floor = 0;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
