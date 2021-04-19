import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { FormattingService, I18NProvider, LocaleService, TooltipService } from '../../ngclient/servoy_public';
import { ServoyPublicModule } from '../../ngclient/servoy_public.module';
import { SabloModule } from '../../sablo/sablo.module';

import { ServoyBootstrapExtraButtonsGroup } from './buttonsgroup';

describe('ServoyBootstrapExtraButtonsGroup', () => {
  let component: ServoyBootstrapExtraButtonsGroup;
  let fixture: ComponentFixture<ServoyBootstrapExtraButtonsGroup>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraButtonsGroup ],
      imports: [SabloModule, ServoyPublicModule, FormsModule],
      providers: [I18NProvider, FormattingService, TooltipService, LocaleService ]
    })
    .compileComponents();
  });

  beforeEach(() => {
    fixture = TestBed.createComponent(ServoyBootstrapExtraButtonsGroup);
    component = fixture.componentInstance;
    component.servoyApi =  jasmine.createSpyObj('ServoyApi', ['getMarkupId','trustAsHtml', 'startEdit','registerComponent','unRegisterComponent']);
    fixture.detectChanges();
  });

  it('should create', () => {
    expect(component).toBeTruthy();
  });
});
