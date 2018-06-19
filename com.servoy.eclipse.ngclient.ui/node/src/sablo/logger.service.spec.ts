import { TestBed, inject } from '@angular/core/testing';

import {LoggerService, LogLevel, LoggerFactory} from './logger.service'
import {WindowRefService} from './util/windowref.service'

describe('LoggerService', () => {
  let windowRef;
  
  beforeEach(() => {
    windowRef = {nativeWindow:{console:{}}};
      
    windowRef.nativeWindow.console = jasmine.createSpyObj('console', ['debug', 'info', 'warn', 'error']);    
    
    TestBed.configureTestingModule({
      providers: [LoggerFactory,{provide: WindowRefService, useFactory:()=>windowRef}]
    });
  });

  it('should be created', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger(LoggerService.name);
      expect(log).toBeTruthy();
  }));
  
  it('should not log anything but errors', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger(LoggerService.name);
      windowRef.nativeWindow.svyLogConfiguration.isDebugMode = false;
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.info('test');
      expect(windowRef.nativeWindow.console.info).not.toHaveBeenCalled();
      
      log.warn('test');
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
      
      var time = new Date();
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' ERROR LoggerService - ERROR!');
    }));
  
  it('should log error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger(LoggerService.name);
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.ERROR;
      
      log.warn('test');
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
      
      var time = new Date();
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' ERROR LoggerService - ERROR!');
    }));
  
  it('should log info, warning, error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger(LoggerService.name);
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.INFO;
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      var time = new Date();
      log.info('some info');
      expect(windowRef.nativeWindow.console.info).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' INFO LoggerService - some info');
      
      time = new Date();
      log.warn('warning...');
      expect(windowRef.nativeWindow.console.warn).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' WARN LoggerService - warning...');
      
      time = new Date();
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' ERROR LoggerService - ERROR!');
    }));
  
  it('should log debug ... error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger(LoggerService.name);
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.DEBUG;
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      let stringResolved = false;
      log.spam(()=>{stringResolved=true;return 'some spam' + 2});
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      expect(stringResolved).toBeFalsy("string resolved should not be true, spam should not have resolved it");
      
      var time = new Date();
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' DEBUG LoggerService - test');

      time = new Date();
      log.debug(()=>{stringResolved=true;return 'test' + 2});
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' DEBUG LoggerService - test2');
      expect(stringResolved).toBeTruthy("stringResolved should be true, debug should have resolved it");

      time = new Date();
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' ERROR LoggerService - ERROR!');

      time = new Date();
      log.error(() =>'ERROR!' + 2);
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' ERROR LoggerService - ERROR!2');
    }));
  
  it('should spam', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger(LoggerService.name);
      windowRef.nativeWindow.svyLogConfiguration.level = LogLevel.SPAM;
      
      var time = new Date();
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' SPAM LoggerService - some spam');

      time = new Date();
      log.spam(()=>"some spam" + 2);
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' SPAM LoggerService - some spam2');

      time = new Date();
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' DEBUG LoggerService - test');
      
      time = new Date();
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(time.getHours() + ":" + time.getMinutes() + ":" + time.getSeconds() + ' ERROR LoggerService - ERROR!');
    }));
  
});
