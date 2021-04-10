import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapTypeahead } from './typeahead';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService, TooltipService } from 'servoy-public';
import { ServoyPublicModule } from 'servoy-public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { FormsModule } from '@angular/forms';

describe('TypeaheadComponent', () => {
  let component: ServoyBootstrapTypeahead;
  let fixture: ComponentFixture<ServoyBootstrapTypeahead>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTypeahead ],
      providers: [ FormattingService, TooltipService],
      imports: [ServoyPublicModule, ServoyTestingModule, NgbModule, FormsModule]
    })
    .compileComponents();
  }));

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapTypeahead);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
