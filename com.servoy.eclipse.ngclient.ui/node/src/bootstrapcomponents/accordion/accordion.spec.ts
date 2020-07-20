import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapAccordion } from './accordion';

describe('AccordionComponent', () => {
  let component: ServoyBootstrapAccordion;
  let fixture: ComponentFixture<ServoyBootstrapAccordion>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapAccordion ]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapAccordion);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
