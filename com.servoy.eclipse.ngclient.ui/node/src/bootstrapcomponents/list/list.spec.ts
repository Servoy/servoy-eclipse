import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapList } from './list';
import { ShowDisplayValuePipe } from '../lib/showDisplayValue.pipe';

describe('ListComponent', () => {
  let component: ServoyBootstrapList;
  let fixture: ComponentFixture<ServoyBootstrapList>;
  let datalistPolyfill;

  beforeEach(waitForAsync(() => {

    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapList ],
      providers: [ShowDisplayValuePipe]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapList);
    component = fixture.componentInstance;
    fixture.detectChanges();
  });

  xit('should create', () => {
    expect(component).toBeTruthy();
  });
});
