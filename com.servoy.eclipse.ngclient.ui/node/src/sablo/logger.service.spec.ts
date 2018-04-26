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
      let stringResolved = false;
      log.spam(()=>{stringResolved=true;return 'some spam' + 2});
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      expect(stringResolved).toBeFalsy("string resolved should not be true, spam should not have resolved it");
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('test');

      log.debug(()=>{stringResolved=true;return 'test' + 2});
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('test22');
      expect(stringResolved).toBeTruthy("stringResolved should be true, debug should have resolved it");

      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');

      log.error(() =>'ERROR!' + 2);
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!2');
    }));
  
  it('should spam', inject([LoggerService], (log: LoggerService) => {
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.SPAM;
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('some spam');

      log.spam(()=>"some spam" + 2);
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('some spam2');

      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith('test');
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith('ERROR!');
    }));
  
});
