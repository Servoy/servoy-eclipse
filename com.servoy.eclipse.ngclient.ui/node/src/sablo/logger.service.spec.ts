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
      var log = logFactory.getLogger("LoggerService");
      expect(log).toBeTruthy();
  }));
  
  it('should not log anything but errors', inject([LoggerFactory], (logFactory: LoggerFactory) => {
   	   //by default isDebugMode is false, so it should only log errors
      var log = logFactory.getLogger("LoggerService"); //by default isDebugMode is false
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.info('test');
      expect(windowRef.nativeWindow.console.info).not.toHaveBeenCalled();
      
      log.warn('test');
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(jasmine.stringMatching(' ERROR LoggerService - '),'ERROR!');
  
  	  log.warn(log.buildMessage(()=>('test')));
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
  }));
  
  it('should log error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger("LoggerService");
      log.setLogLevel(LogLevel.ERROR);
      
      log.warn('test');
      expect(windowRef.nativeWindow.console.warn).not.toHaveBeenCalled();
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(jasmine.stringMatching(' ERROR LoggerService - '),'ERROR!');
    }));
  
  it('should log info, warning, error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger("LoggerService");
      log.setLogLevel(LogLevel.INFO);
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      
      log.info('some info');
      expect(windowRef.nativeWindow.console.info).toHaveBeenCalledWith(jasmine.stringMatching(' INFO LoggerService - '),'some info');
      
      log.warn('warning...');
      expect(windowRef.nativeWindow.console.warn).toHaveBeenCalledWith(jasmine.stringMatching(' WARN LoggerService - '),'warning...');
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(jasmine.stringMatching(' ERROR LoggerService - '),'ERROR!');
    }));
  
  it('should log debug ... error', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger("LoggerService");
      log.setLogLevel(LogLevel.DEBUG);
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      let stringResolved = false;
      log.spam(log.buildMessage(()=>{stringResolved=true;return 'some spam' + 2}));
      expect(windowRef.nativeWindow.console.debug).not.toHaveBeenCalled();
      expect(stringResolved).toBeFalsy("string resolved should not be true, spam should not have resolved it");
      
      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(jasmine.stringMatching(' DEBUG LoggerService - '),'test');

      log.debug(log.buildMessage(()=>{stringResolved=true;return 'test' + 2}));
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(jasmine.stringMatching(' DEBUG LoggerService - '),'test2');
      expect(stringResolved).toBeTruthy("stringResolved should be true, debug should have resolved it");

      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(jasmine.stringMatching(' ERROR LoggerService - '),'ERROR!');

      log.error(log.buildMessage(() =>'ERROR!' + 2));
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(jasmine.stringMatching(' ERROR LoggerService - '),'ERROR!2');
    }));
  
  it('should spam', inject([LoggerFactory], (logFactory: LoggerFactory) => {
      var log = logFactory.getLogger("LoggerService");
      log.setLogLevel(LogLevel.SPAM);
      
      log.spam('some spam');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(jasmine.stringMatching(' SPAM LoggerService - '),'some spam');

      log.spam(log.buildMessage(()=>"some spam" + 2));
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(jasmine.stringMatching(' SPAM LoggerService - '),'some spam2');

      log.debug('test');
      expect(windowRef.nativeWindow.console.debug).toHaveBeenCalledWith(jasmine.stringMatching(' DEBUG LoggerService - '),'test');
      
      log.error('ERROR!');
      expect(windowRef.nativeWindow.console.error).toHaveBeenCalledWith(jasmine.stringMatching(' ERROR LoggerService - '),'ERROR!');
    }));
  
});
