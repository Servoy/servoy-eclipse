import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';

import { ServoyBootstrapTablesspanel } from './tablesspanel';

describe('TablesspanelComponent', () => {
  let component: ServoyBootstrapTablesspanel;
  let fixture: ComponentFixture<ServoyBootstrapTablesspanel>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTablesspanel ],
      imports: [NgbModule, SabloModule, ServoyPublicModule, SabloModule]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTablesspanel);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId', 'trustAsHtml']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
