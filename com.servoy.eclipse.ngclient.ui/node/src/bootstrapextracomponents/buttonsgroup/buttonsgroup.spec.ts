import { ComponentFixture, TestBed } from '@angular/core/testing';
import { FormsModule } from '@angular/forms';
import { FormattingService, TooltipService } from '@servoy/public';
import { ServoyPublicModule } from '@servoy/public';
import { ServoyTestingModule } from '../../testing/servoytesting.module';
import { LocaleService } from '../../ngclient/locale.service';
import { I18NProvider } from '../../ngclient/services/i18n_provider.service';

import { ServoyBootstrapExtraButtonsGroup } from './buttonsgroup';

describe('ServoyBootstrapExtraButtonsGroup', () => {
  let component: ServoyBootstrapExtraButtonsGroup;
  let fixture: ComponentFixture<ServoyBootstrapExtraButtonsGroup>;

  beforeEach(() => {
    TestBed.configureTestingModule({
      declarations: [ ServoyBootstrapExtraButtonsGroup ],
      imports: [ServoyTestingModule, ServoyPublicModule, FormsModule],
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
