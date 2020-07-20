import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCombobox } from './combobox';

describe('ComboboxComponent', () => {
  let component: ServoyBootstrapCombobox;
  let fixture: ComponentFixture<ServoyBootstrapCombobox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCombobox ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapCombobox);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
