import { TestBed, inject } from '@angular/core/testing';

import {LoggerService, LogLevel} from './logger.service'
import {WindowRefService} from './util/windowref.service'


describe('LoggerService', () => {
  let windowRef;
  
  beforeEach(() => {
     windowRef = {nativeWindow:{console:{}}};
      
    windowRef.nativeWindow.console = jasmine.createSpyObj('console', ['debug', 'info', 'warn', 'error']);    
    
    TestBed.configureTestingModule({
      providers: [LoggerService,{provide: WindowRefService, useFactory:()=>windowRef}]
    });
  });

  it('should be created', inject([LoggerService], (log: LoggerService) => {
    expect(log).toBeTruthy();
  }));
  
  it('should not log anything but errors', inject([LoggerService], (log: LoggerService) => {
      windowRef.nativeWindow.svyLogConfiguration.isDebugMode = false;
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.info('test');
      expect(windowRef.nativeWindow.console.info).not.toHaveBeenCalled();
      
      log.warn('test');
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');
    }));
  
  it('should log error', inject([LoggerService], (log: LoggerService) => {
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.ERROR;
      
      log.warn('test');
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');
    }));
  
  it('should log info, warning, error', inject([LoggerService], (log: LoggerService) => {
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.INFO;
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.info('some info');
      expect(windowRef.nativeWindow.console.info).toHaveBeenCalledWith('some info');
      
      log.warn('warning...');
      expect(windowRef.nativeWindow.console.warn).toHaveBeenCalledWith('warning...');
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');
    }));
  
  it('should log debug ... error', inject([LoggerService], (log: LoggerService) => {
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.DEBUG;
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('test');
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');
    }));
  
  it('should spam', inject([LoggerService], (log: LoggerService) => {
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.SPAM;
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('some spam');
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('test');
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');
    }));
  
});
