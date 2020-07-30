import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapTabpanel } from './tabpanel';
import { WindowRefService } from '../../sablo/util/windowref.service'
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { SabloModule } from '../../sablo/sablo.module';
import { ServoyApi } from '../../ngclient/servoy_public'

describe('TabpanelComponent', () => {
  let component: ServoyBootstrapTabpanel;
  let fixture: ComponentFixture<ServoyBootstrapTabpanel>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTabpanel ],
      imports: [NgbModule, SabloModule],
      providers: [WindowRefService]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTabpanel);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
