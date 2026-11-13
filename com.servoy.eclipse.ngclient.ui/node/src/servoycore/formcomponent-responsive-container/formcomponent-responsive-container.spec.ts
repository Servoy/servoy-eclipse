import { describe, it, expect, beforeEach } from 'vitest';
import { ComponentFixture, TestBed } from '@angular/core/testing';
import { CUSTOM_ELEMENTS_SCHEMA } from '@angular/core';

import { ServoyCoreFormcomponentResponsiveCotainer } from './formcomponent-responsive-container';

describe('ServoyCoreFormcomponentResponsiveCotainer', () => {
  let component: ServoyCoreFormcomponentResponsiveCotainer;
  let fixture: ComponentFixture<ServoyCoreFormcomponentResponsiveCotainer>;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [ServoyCoreFormcomponentResponsiveCotainer],
      schemas: [CUSTOM_ELEMENTS_SCHEMA],
    }).compileComponents();
    fixture = TestBed.createComponent(ServoyCoreFormcomponentResponsiveCotainer);
    component = fixture.componentInstance;
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });

  it('should expose the items and formComponent inputs', () => {
    const items = [{ rId: 1 }];
    const parent = {} as any;
    fixture.componentRef.setInput('items', items);
    fixture.componentRef.setInput('formComponent', parent);
    expect(component.items()).toBe(items);
    expect(component.formComponent()).toBe(parent);
  });
});
