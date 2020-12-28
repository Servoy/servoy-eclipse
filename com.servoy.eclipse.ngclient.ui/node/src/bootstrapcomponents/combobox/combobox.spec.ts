import { ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCombobox } from './combobox';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';
import { FormattingService, TooltipService } from '../../ngclient/servoy_public';
import { FormsModule } from '@angular/forms';
import { NgbModule } from '@ng-bootstrap/ng-bootstrap';

describe('ComboboxComponent', () => {
  let component: ServoyBootstrapCombobox;
  let fixture: ComponentFixture<ServoyBootstrapCombobox>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCombobox ],
      providers: [ FormattingService, TooltipService],
      imports: [ServoyPublicModule, SabloModule, NgbModule, FormsModule]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapCombobox);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
