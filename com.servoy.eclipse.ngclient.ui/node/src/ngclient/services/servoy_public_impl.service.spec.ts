import { describe, it, expect, beforeEach, vi } from 'vitest';
import { TestBed } from '@angular/core/testing';

import { ServoyPublicServiceImpl } from './servoy_public_impl.service';
import { SabloService } from '../../sablo/sablo.service';
import { WebsocketService } from '../../sablo/websocket.service';
import { ServicesService } from '../../sablo/services.service';
import { FormService } from '../form.service';
import { LocaleService } from '../locale.service';
import { ServoyService } from '../servoy.service';
import { SvyUtilsService } from '../utils.service';
import { ApplicationService } from './application.service';
import { I18NProvider } from './i18n_provider.service';
import { PopupFormService } from './popupform.service';

describe('ServoyPublicServiceImpl', () => {
  let service: ServoyPublicServiceImpl;
  let sabloService: any;
  let i18nProvider: any;
  let utils: any;
  let localeService: any;
  let applicationService: any;
  let servoyService: any;
  let formService: any;
  let servicesService: any;
  let popupFormService: any;

  beforeEach(() => {
    sabloService = { getClientnr: vi.fn().mockReturnValue('7'), callService: vi.fn().mockReturnValue('csResult'), getLocale: vi.fn().mockReturnValue({ full: 'en-US' }) };
    i18nProvider = { getI18NMessages: vi.fn().mockReturnValue('i18n'), listenForI18NMessages: vi.fn().mockReturnValue('listener') };
    utils = { createJSEvent: vi.fn().mockReturnValue({ svyType: 'JSEvent' }) };
    localeService = { getLocale: vi.fn().mockReturnValue('en'), getAgGridLocale: vi.fn().mockReturnValue({ a: 'b' }) };
    applicationService = {
      showFileOpenDialog: vi.fn(),
      showMessageDialog: vi.fn().mockResolvedValue('ok'),
      generateServiceUploadUrl: vi.fn().mockReturnValue('svcUrl'),
      generateUploadUrl: vi.fn().mockReturnValue('uploadUrl'),
      generateMediaDownloadUrl: vi.fn().mockReturnValue('mediaUrl'),
      getUIProperty: vi.fn().mockReturnValue('uiVal'),
    };
    servoyService = { executeInlineScript: vi.fn().mockResolvedValue('inline') };
    formService = { getFormCacheByName: vi.fn().mockReturnValue({}), setFormStyleClasses: vi.fn(), getFormComponentInstance: vi.fn() };
    servicesService = { callServiceServerSideApi: vi.fn().mockResolvedValue('ssResult'), sendServiceChangesWithValue: vi.fn() };
    popupFormService = { showForm: vi.fn(), cancelFormPopup: vi.fn() };

    TestBed.configureTestingModule({
      providers: [
        ServoyPublicServiceImpl,
        { provide: SabloService, useValue: sabloService },
        { provide: I18NProvider, useValue: i18nProvider },
        { provide: SvyUtilsService, useValue: utils },
        { provide: LocaleService, useValue: localeService },
        { provide: ApplicationService, useValue: applicationService },
        { provide: ServoyService, useValue: servoyService },
        { provide: FormService, useValue: formService },
        { provide: ServicesService, useValue: servicesService },
        { provide: PopupFormService, useValue: popupFormService },
        { provide: WebsocketService, useValue: {} },
      ],
    });
    service = TestBed.inject(ServoyPublicServiceImpl);
  });

  it('should be created', () => {
    expect(service).toBeTruthy();
  });

  it('executeInlineScript should delegate to the servoy service', () => {
    service.executeInlineScript('frm', 'script', [1]);
    expect(servoyService.executeInlineScript).toHaveBeenCalledWith('frm', 'script', [1]);
  });

  it('callServiceServerSideApi should delegate to the services service', () => {
    service.callServiceServerSideApi('svc', 'm', [1]);
    expect(servicesService.callServiceServerSideApi).toHaveBeenCalledWith('svc', 'm', [1]);
  });

  it('getI18NMessages and listenForI18NMessages should delegate to the i18n provider', () => {
    expect(service.getI18NMessages('a')).toBe('i18n');
    expect(service.listenForI18NMessages('a')).toBe('listener');
  });

  it('getClientnr should delegate to the sablo service', () => {
    expect(service.getClientnr()).toBe('7');
  });

  it('callService should delegate to the sablo service', () => {
    service.callService('svc', 'm', { a: 1 }, true);
    expect(sabloService.callService).toHaveBeenCalledWith('svc', 'm', { a: 1 }, true);
  });

  it('getLocale and getAGGridLocale should delegate to the locale service', () => {
    expect(service.getLocale()).toBe('en');
    expect(service.getAGGridLocale()).toEqual({ a: 'b' });
  });

  it('getLocaleObject should delegate to the sablo service', () => {
    expect(service.getLocaleObject()).toEqual({ full: 'en-US' });
  });

  it('createJSEvent should delegate to utils', () => {
    expect(service.createJSEvent({} as any, 'onClick')).toEqual({ svyType: 'JSEvent' });
  });

  it('showFileOpenDialog should delegate to the application service', () => {
    service.showFileOpenDialog('t', true, 'f', 'u');
    expect(applicationService.showFileOpenDialog).toHaveBeenCalledWith('t', true, 'f', 'u');
  });

  it('showMessageDialog should delegate to the application service', () => {
    service.showMessageDialog('t', 'm', 'c', [], [], 'i', 0);
    expect(applicationService.showMessageDialog).toHaveBeenCalled();
  });

  it('upload and media url helpers should delegate to the application service', () => {
    expect(service.generateServiceUploadUrl('s', 'a', true)).toBe('svcUrl');
    expect(service.generateUploadUrl('f', 'c', 'p')).toBe('uploadUrl');
    expect(service.generateMediaDownloadUrl('m')).toBe('mediaUrl');
  });

  it('getUIProperty should delegate to the application service', () => {
    expect(service.getUIProperty('key')).toBe('uiVal');
  });

  it('getFormCacheByName should delegate to the form service', () => {
    expect(service.getFormCacheByName('frm')).toEqual({});
  });

  it('sendServiceChanges should send with the same value for old and new', () => {
    service.sendServiceChanges('svc', 'prop', 'v');
    expect(servicesService.sendServiceChangesWithValue).toHaveBeenCalledWith('svc', 'prop', 'v', 'v');
  });

  it('sendServiceChangeToServer should forward the old value', () => {
    service.sendServiceChangeToServer('svc', 'prop', 'new', 'old');
    expect(servicesService.sendServiceChangesWithValue).toHaveBeenCalledWith('svc', 'prop', 'new', 'old');
  });

  it('showForm and cancelFormPopup should delegate to the popup form service', () => {
    const popup = { form: 'f' } as any;
    service.showForm(popup);
    expect(popupFormService.showForm).toHaveBeenCalledWith(popup);
    service.cancelFormPopup(true);
    expect(popupFormService.cancelFormPopup).toHaveBeenCalledWith(true);
  });

  it('setFormStyleClasses should delegate to the form service', () => {
    service.setFormStyleClasses({ property: 'x' });
    expect(formService.setFormStyleClasses).toHaveBeenCalledWith({ property: 'x' });
  });

  it('isInTestingMode should read the testingMode UI property', () => {
    applicationService.getUIProperty.mockReturnValue(true);
    expect(service.isInTestingMode()).toBe(true);
  });

  it('getTemplateForFormComponentChild should return the template from the form component instance', () => {
    const tmpl = {} as any;
    formService.getFormComponentInstance.mockReturnValue({ getTemplateForLFC: () => tmpl });
    expect(service.getTemplateForFormComponentChild('frm', { name: 'c' } as any)).toBe(tmpl);
  });

  it('getTemplateForFormComponentChild should return null when no form component instance exists', () => {
    formService.getFormComponentInstance.mockReturnValue(undefined);
    expect(service.getTemplateForFormComponentChild('frm', { name: 'c' } as any)).toBeNull();
  });
});
