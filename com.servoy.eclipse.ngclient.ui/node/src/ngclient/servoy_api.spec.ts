import { describe, it, expect, beforeEach, vi } from 'vitest';
import { ServoyApi } from './servoy_api';

describe('ServoyApi', () => {
  let formService: any;
  let servoyService: any;
  let item: any;
  let api: ServoyApi;

  beforeEach(() => {
    formService = {
      formWillShow: vi.fn().mockResolvedValue(true),
      hideForm: vi.fn().mockResolvedValue(true),
      pushEditingStarted: vi.fn(),
      sendChanges: vi.fn(),
      callComponentServerSideApi: vi.fn().mockReturnValue('result'),
    };
    servoyService = {
      getUIProperties: vi.fn().mockReturnValue({ getUIProperty: vi.fn().mockReturnValue('uiProp') }),
    };
    item = { name: 'mycomp', model: {} as any };
    api = new ServoyApi(item, 'myform', true, formService, servoyService, false);
  });

  it('formWillShow should delegate to the form service with the component name', () => {
    api.formWillShow('otherform', 'rel', 2);
    expect(formService.formWillShow).toHaveBeenCalledWith('otherform', true, 'myform', 'mycomp', 'rel', 2);
  });

  it('hideForm should delegate to the form service', () => {
    api.hideForm('otherform', 'rel', 1, 'showForm', 'showRel', 3);
    expect(formService.hideForm).toHaveBeenCalledWith('otherform', 'myform', 'mycomp', 'rel', 1, 'showForm', 'showRel', 3);
  });

  it('startEdit should push editing started', () => {
    api.startEdit('dp');
    expect(formService.pushEditingStarted).toHaveBeenCalledWith('myform', 'mycomp', 'dp');
  });

  it('apply without tracked changes should send a full replacement', () => {
    api.apply('prop', 'newVal');
    expect(formService.sendChanges).toHaveBeenCalledWith('myform', 'mycomp', 'prop', 'newVal', null, true);
  });

  it('apply with tracked changes should pass the old value and update the model', () => {
    item.model.prop = 'oldVal';
    api.apply('prop', 'newVal', true);
    expect(item.model.prop).toBe('newVal');
    expect(formService.sendChanges).toHaveBeenCalledWith('myform', 'mycomp', 'prop', 'newVal', 'oldVal', false);
  });

  it('callServerSideApi should delegate to the form service', () => {
    expect(api.callServerSideApi('method', [1, 2])).toBe('result');
    expect(formService.callComponentServerSideApi).toHaveBeenCalledWith('myform', 'mycomp', 'method', [1, 2]);
  });

  it('isInDesigner should reflect the constructor flag', () => {
    expect(api.isInDesigner()).toBe(false);
    expect(new ServoyApi(item, 'f', true, formService, servoyService, true).isInDesigner()).toBe(true);
  });

  it('trustAsHtml should prefer the component clientProperty value', () => {
    item.model.clientProperty = { trustDataAsHtml: true };
    expect(api.trustAsHtml()).toBe(true);
  });

  it('trustAsHtml should fall back to the UI property', () => {
    expect(api.trustAsHtml()).toBe('uiProp');
  });

  it('isInAbsoluteLayout should reflect the constructor flag', () => {
    expect(api.isInAbsoluteLayout()).toBe(true);
    expect(new ServoyApi(item, 'f', false, formService, servoyService, false).isInAbsoluteLayout()).toBe(false);
  });

  it('getMarkupId should read from the model', () => {
    item.model.svyMarkupId = 'mk1';
    expect(api.getMarkupId()).toBe('mk1');
  });

  it('getFormName should return the form name', () => {
    expect(api.getFormName()).toBe('myform');
  });

  it('registerComponent and unRegisterComponent should be no-ops', () => {
    expect(() => api.registerComponent(null!)).not.toThrow();
    expect(() => api.unRegisterComponent(null!)).not.toThrow();
  });

  it('getClientProperty should return a value from the model clientProperty', () => {
    item.model.clientProperty = { key1: 'val1' };
    expect(api.getClientProperty('key1')).toBe('val1');
  });

  it('getClientProperty should return null when there is no clientProperty', () => {
    expect(api.getClientProperty('key1')).toBeNull();
  });
});
