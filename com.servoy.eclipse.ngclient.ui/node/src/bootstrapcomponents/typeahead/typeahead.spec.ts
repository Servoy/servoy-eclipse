import { ComponentFixture, TestBed, waitForAsync } from '@angular/core/testing';

import { ServoyBootstrapTypeahead } from './typeahead';

import { NgbModule } from '@ng-bootstrap/ng-bootstrap';
import { FormattingService, TooltipService } from '../../ngclient/servoy_public';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';
import { FormsModule } from '@angular/forms';

describe('TypeaheadComponent', () => {
  let component: ServoyBootstrapTypeahead;
  let fixture: ComponentFixture<ServoyBootstrapTypeahead>;

  beforeEach(waitForAsync(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapTypeahead ],
      providers: [ FormattingService, TooltipService],
      imports: [ServoyPublicModule, SabloModule, NgbModule, FormsModule]
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
