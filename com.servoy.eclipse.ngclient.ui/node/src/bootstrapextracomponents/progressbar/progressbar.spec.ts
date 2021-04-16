import { waitForAsync, ComponentFixture, TestBed } from '@angular/core/testing';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ServoyPublicModule } from '@servoy/public';

import { ServoyBootstrapExtraProgressBar } from './progressbar';

describe('FileUploadComponent', () => {
  let component: ServoyBootstrapExtraProgressBar;
  let fixture: ComponentFixture<ServoyBootstrapExtraProgressBar>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraProgressBar ],
      imports: [ NgbModule, ServoyPublicModule ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraProgressBar);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
