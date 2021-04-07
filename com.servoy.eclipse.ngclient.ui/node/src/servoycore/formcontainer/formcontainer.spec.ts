import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormService } from '../../ngclient/form.service';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';

import { ServoyCoreFormContainer } from './formcontainer';

describe('ServoyCoreFormContainer', () => {
  let component: ServoyCoreFormContainer;
  let fixture: ComponentFixture<ServoyCoreFormContainer>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyCoreFormContainer ],
      imports: [NgbModule, SabloModule, ServoyPublicModule, SabloModule],
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
