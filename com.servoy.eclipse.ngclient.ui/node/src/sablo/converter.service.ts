import { Injectable } from '@angular/core';
import { LoggerService, LoggerFactory } from './logger.service';


class SwingModifiers {
  public static readonly SHIFT_MASK = 1;
  public static readonly CTRL_MASK = 2;
  public static readonly META_MASK = 4;
  public static readonly ALT_MASK = 8;
  public static readonly ALT_GRAPH_MASK = 32;
  public static readonly BUTTON1_MASK = 16;
  public static readonly BUTTON2_MASK = 8;
  public static readonly SHIFT_DOWN_MASK = 64;
  public static readonly CTRL_DOWN_MASK = 128;
  public static readonly META_DOWN_MASK = 256;
  public static readonly ALT_DOWN_MASK = 512;
  public static readonly BUTTON1_DOWN_MASK = 1024;
  public static readonly BUTTON2_DOWN_MASK = 2048;
  public static readonly DOWN_MASK = 4096;
  public static readonly ALT_GRAPH_DOWN_MASK = 8192;
}

export type PropertyContext = (propertyName: string) => any;

@Injectable()
export class ConverterService {

  constructor(private logFactory: LoggerFactory) {
    this.log = logFactory.getLogger('ConverterService');
  }


  public static readonly INTERNAL_IMPL = '__internalState';
  public static readonly TYPES_KEY = 'svy_types'; // TODO this should be sablo_types...
  // objects that have a function named like this in them will send to server the result of that function call when no conversion type is available (in case of
  // usage as handler arg. for example where we don't know the arg. types on client)
  public static readonly DEFAULT_CONVERSION_TO_SERVER_FUNC = '_dctsf';

  private customPropertyConverters: { [s: string]: IConverter } = {};
  private log: LoggerService;


  public static getCombinedPropertyNames(now, prev) {
    const fulllist = {};
    if (prev) {
      const prevNames = Object.getOwnPropertyNames(prev);
      for (let i = 0; i < prevNames.length; i++) {
        fulllist[prevNames[i]] = true;
      }
    }
    if (now) {
      const nowNames = Object.getOwnPropertyNames(now);
      for (let i = 0; i < nowNames.length; i++) {
        fulllist[nowNames[i]] = true;
      }
    }
    return fulllist;
  }

  public convertFromServerToClient(serverSentData: any, conversionInfo, currentClientData?: any, propertyContext?: PropertyContext) {
    if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
      const customConverter = this.customPropertyConverters[conversionInfo];
      if (customConverter) {
        serverSentData = customConverter.fromServerToClient(serverSentData, currentClientData, propertyContext);
      } else { // converter not found - will not convert
        this.log.error(this.log.buildMessage(() => ('cannot find type converter (s->c) for: \'' + conversionInfo + '\'.')));
      }
    } else if (conversionInfo) {
      // eslint-disable-next-line guard-for-in
      for (const conKey in conversionInfo) {
        serverSentData[conKey] = this.convertFromServerToClient(serverSentData[conKey], conversionInfo[conKey],
          currentClientData ? currentClientData[conKey] : undefined, propertyContext);
      }
    }
    return serverSentData;
  }

  public convertFromClientToServer(newClientData: any, conversionInfo, oldClientData?: any, propertyContext?: PropertyContext) {
    if (typeof conversionInfo === 'string' || typeof conversionInfo === 'number') {
      const customConverter = this.customPropertyConverters[conversionInfo];
      if (customConverter) return customConverter.fromClientToServer(newClientData, oldClientData);
      else { // converter not found - will not convert
        this.log.error(this.log.buildMessage(() => ('cannot find type converter (c->s) for: \'' + conversionInfo + '\'.')));
        return newClientData;
      }
    } else if (conversionInfo) {
      const retVal = Array.isArray(newClientData) ? [] : {}; // was: (Array.isArray ? Array.isArray(newClientData) : $.isArray(newClientData)) ? [] : {};
      // eslint-disable-next-line guard-for-in
      for (const conKey in conversionInfo) {
        retVal[conKey] = this.convertFromClientToServer(newClientData[conKey], conversionInfo[conKey], oldClientData ? oldClientData[conKey] : undefined, propertyContext);
      }
      return retVal;
    } else {
      return newClientData;
    }
  }
  public convertClientObject(value) {
    if (value instanceof Date) {
      value = this.convertFromClientToServer(value, 'Date', null);
    } else if (value && typeof value[ConverterService.DEFAULT_CONVERSION_TO_SERVER_FUNC] === 'function') {
      return value[ConverterService.DEFAULT_CONVERSION_TO_SERVER_FUNC]();
    }
    return value;
  }

  /**
   * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
   * return obj[a][b][c] making sure that if any does not exist or is null (for example b) it will be set to {}.
   */
  public getOrCreateInDepthProperty(...args) {
    if (arguments.length === 0) return undefined;

    let ret = arguments[0];
    if (ret === undefined || ret === null || arguments.length === 1) return ret;
    for (let i = 1; i < arguments.length; i++) {
      const p = ret;
      ret = ret[arguments[i]];
      if (ret === undefined || ret === null) {
        ret = {};
        p[arguments[i]] = ret;
      }
    }
    return ret;
  }

  /**
   * Receives variable arguments. First is the object obj and the others (for example a, b, c) are used to
   * return obj[a][b][c] making sure that if any does not exist or is null it will just return null/undefined instead of erroring out.
   */
  public getInDepthProperty(...args) {
    if (arguments.length === 0) return undefined;

    let ret = arguments[0];
    if (ret === undefined || ret === null || arguments.length === 1) return ret;
    let i;
    for (i = 1; i < arguments.length; i++) {
      ret = ret[arguments[i]];
      if (ret === undefined || ret === null) {
        return i === arguments.length - 1 ? ret : undefined;
      }
    }

    return ret;
  }


  public registerCustomPropertyHandler(propertyTypeID: string, customHandler: IConverter, overwrite?: boolean) {
    if (overwrite === false && this.customPropertyConverters[propertyTypeID]) return;
    this.customPropertyConverters[propertyTypeID] = customHandler;
  }

  public getEventArgs(args, eventName) {
    const newargs = [];
    for (let i = 0; i < args.length; i++) {
      let arg = args[i];
      if (arg && arg.originalEvent) arg = arg.originalEvent;
      if (arg instanceof MouseEvent || arg instanceof KeyboardEvent) {
        const $event = arg;
        const eventObj = {};
        let modifiers = 0;
        if ($event.shiftKey) modifiers = modifiers || SwingModifiers.SHIFT_MASK;
        if ($event.metaKey) modifiers = modifiers || SwingModifiers.META_MASK;
        if ($event.altKey) modifiers = modifiers || SwingModifiers.ALT_MASK;
        if ($event.ctrlKey) modifiers = modifiers || SwingModifiers.CTRL_MASK;

        eventObj['type'] = 'event';
        eventObj['eventName'] = eventName;
        eventObj['modifiers'] = modifiers;
        eventObj['timestamp'] = new Date().getTime();
        eventObj['x'] = $event['pageX'];
        eventObj['y'] = $event['pageY'];
        arg = eventObj;
      } else if (arg instanceof Event) {
        const eventObj = {};
        eventObj['type'] = 'event';
        eventObj['eventName'] = eventName;
        eventObj['timestamp'] = new Date().getTime();
        arg = eventObj;
        // TODO should be $sabloConverters.convertFromClientToServer(now, beanConversionInfo[property] ?, undefined);,
        // but as we do not know handler arg types, we just do default conversion (for dates & types that use $sabloUtils.DEFAULT_CONVERSION_TO_SERVER_FUNC)
      } else arg = this.convertClientObject(arg);

      newargs.push(arg);
    }
    return newargs;
  }
}

export interface IConverter {
  fromServerToClient(serverSentData: Object, currentClientData: Object, propertyContext: PropertyContext): Object;
  fromClientToServer(newClientData: Object, oldClientData: Object): Object;
}


