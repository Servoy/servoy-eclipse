import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { SvyTextfield } from './textfield';

describe("SvyTextfield", () => {
  let component: SvyTextfield;
  let fixture: ComponentFixture<SvyTextfield>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ SvyTextfield ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(SvyTextfield);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
