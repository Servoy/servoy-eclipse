import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapTablesspanel } from './tablesspanel';

describe('TablesspanelComponent', () => {
  let component: ServoyBootstrapTablesspanel;
  let fixture: ComponentFixture<ServoyBootstrapTablesspanel>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTablesspanel ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTablesspanel);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj("ServoyApi", ["getMarkupId","trustAsHtml"]);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
