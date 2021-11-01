import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { FormService } from '../../ngclient/form.service';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';

import { ServoyCoreFormContainer } from './formcontainer';
import { NoopAnimationsModule } from '@angular/platform-browser/animations';

describe('ServoyCoreFormContainer', () => {
  let component: ServoyCoreFormContainer;
  let fixture: ComponentFixture<ServoyCoreFormContainer>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyCoreFormContainer ],
      imports: [ServoyTestingModule, ServoyPublicModule, NoopAnimationsModule],
      providers: [ { provide: FormService, useValue: {getFormCacheByName: () => {} }} ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyCoreFormContainer);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
