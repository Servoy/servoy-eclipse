import { async, ComponentFixture, TestBed } from '@angular/core/testing';

import { ServoyBootstrapCombobox } from './combobox';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module'
import { SabloModule } from '../../sablo/sablo.module';
import { FormattingService, TooltipService } from "../../ngclient/servoy_public";

describe('ComboboxComponent', () => {
  let component: ServoyBootstrapCombobox;
  let fixture: ComponentFixture<ServoyBootstrapCombobox>;

  beforeEach(async(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapCombobox ],
      providers: [ FormattingService, TooltipService],
      imports: [ServoyPublicModule,SabloModule]
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
