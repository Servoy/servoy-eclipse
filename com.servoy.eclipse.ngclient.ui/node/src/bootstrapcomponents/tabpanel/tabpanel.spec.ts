import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapTabpanel } from './tabpanel';

describe('TabpanelComponent', () => {
  let component: ServoyBootstrapTabpanel;
  let fixture: ComponentFixture<ServoyBootstrapTabpanel>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTabpanel ]
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
