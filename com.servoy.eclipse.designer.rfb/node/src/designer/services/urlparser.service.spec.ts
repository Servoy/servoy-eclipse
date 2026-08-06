import { describe, it, expect, beforeEach, vi } from 'vitest';

import { URLParserService } from './urlparser.service';

describe('URLParserService', () => {
  let service: URLParserService;
  let websocketService: Record<string, ReturnType<typeof vi.fn>>;

  const setupWithParams = (params: Record<string, string | null>) => {
    websocketService = {
      getURLParameter: vi.fn((key: string) => params[key] ?? null),
    };
    service = Object.create(URLParserService.prototype);
    (service as any).websocketService = websocketService;
    service.parseURL();
  };

  describe('parseURL', () => {
    beforeEach(() => {
      setupWithParams({
        f: 'myForm',
        s: 'mySolution',
        l: 'absolute',
        hd: 'true',
        mso: 'false',
        w: '800',
        h: '600',
        fc: 'true',
        cont: 'myContainer',
        c_clientnr: '42',
      });
    });

    it('should parse form name', () => {
      expect(service.getFormName()).toBe('myForm');
    });

    it('should parse solution name', () => {
      expect(service.getSolutionName()).toBe('mySolution');
    });

    it('should parse layout', () => {
      expect(service.layout).toBe('absolute');
    });

    it('should parse hideDefault as boolean', () => {
      expect(service.isHideDefault()).toBe(true);
    });

    it('should parse formWidth as number', () => {
      expect(service.getFormWidth()).toBe(800);
    });

    it('should parse formHeight as number', () => {
      expect(service.getFormHeight()).toBe(600);
    });

    it('should parse formComponent', () => {
      expect(service.isFormComponent()).toBe(true);
    });

    it('should parse showingInContainer', () => {
      expect(service.isShowingContainer()).toBe('myContainer');
    });

    it('should parse contentClientNr', () => {
      expect(service.getContentClientNr()).toBe('42');
    });
  });

  describe('isAbsoluteFormLayout', () => {
    it('should return true for absolute layout', () => {
      setupWithParams({ l: 'absolute', f: '', s: '', hd: '', mso: '', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isAbsoluteFormLayout()).toBe(true);
    });

    it('should return true for csspos layout', () => {
      setupWithParams({ l: 'csspos', f: '', s: '', hd: '', mso: '', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isAbsoluteFormLayout()).toBe(true);
    });

    it('should return false for responsive layout', () => {
      setupWithParams({ l: 'responsive', f: '', s: '', hd: '', mso: '', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isAbsoluteFormLayout()).toBe(false);
    });
  });

  describe('isCSSPositionFormLayout', () => {
    it('should return true only for csspos', () => {
      setupWithParams({ l: 'csspos', f: '', s: '', hd: '', mso: '', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isCSSPositionFormLayout()).toBe(true);
    });

    it('should return false for absolute', () => {
      setupWithParams({ l: 'absolute', f: '', s: '', hd: '', mso: '', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isCSSPositionFormLayout()).toBe(false);
    });
  });

  describe('isMarqueeSelectOuter', () => {
    it('should return true when mso is "true"', () => {
      setupWithParams({ l: '', f: '', s: '', hd: '', mso: 'true', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isMarqueeSelectOuter()).toBe(true);
    });

    it('should return false when mso is not "true"', () => {
      setupWithParams({ l: '', f: '', s: '', hd: '', mso: 'false', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isMarqueeSelectOuter()).toBe(false);
    });
  });

  describe('hideDefault', () => {
    it('should return false when hd is not "true"', () => {
      setupWithParams({ l: '', f: '', s: '', hd: 'false', mso: '', w: '0', h: '0', fc: '', cont: '', c_clientnr: '' });
      expect(service.isHideDefault()).toBe(false);
    });
  });
});
