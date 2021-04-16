
import {DateConverter} from './date_converter';

describe('DateConverter', () => {
  let dateConverter: DateConverter;
  const currentOffset = new Date(2018,0,1).getTimezoneOffset()/60*-1; // offset is negative -60 for +01:00
  beforeEach(() => {
          dateConverter = new DateConverter();
  });

  const offsetString = (wantedOffset) => {
      let offsetStr = '';
      const offset = currentOffset+wantedOffset;
      if (offset < 0) {
          if (offset > -10){
              offsetStr = '-0' + Math.abs(offset) + ':00';
          } else {
              offsetStr =offset + ':00';
          }
      } else {
          if (offset > 10){
              offsetStr = '+' + offset + ':00';
          } else {
              offsetStr = '+0' + offset + ':00';
          }
      }
      return offsetStr;
  }

  it('should parse date string from server without timezone',  () => {
      const date: Date = dateConverter.fromServerToClient('2018-01-01T00:00:00');
      expect(date).toBeDefined();
      expect(date.getFullYear()).toBe(2018);
      expect(date.getMonth()).toBe(0);
      expect(date.getDate()).toBe(1);
      expect(date.getHours()).toBe(0);
      expect(date.getMinutes()).toBe(0);
      expect(date.getMilliseconds()).toBe(0);
  });

  it('should parse date string from server with timezone -1',  () => {
      const date: Date = dateConverter.fromServerToClient('2018-01-01T00:00:00' + offsetString(-1));
      expect(date).toBeDefined();
      expect(date.getFullYear()).toBe(2018);
      expect(date.getMonth()).toBe(0);
      expect(date.getDate()).toBe(1);
      expect(date.getHours()).toBe(1);
      expect(date.getMinutes()).toBe(0);
      expect(date.getMilliseconds()).toBe(0);
  });

  it('should parse date string from server with timezone +0',  () => {
      const date: Date = dateConverter.fromServerToClient('2018-01-01T00:00:00' + offsetString(0));
      expect(date).toBeDefined();
      expect(date.getFullYear()).toBe(2018);
      expect(date.getMonth()).toBe(0);
      expect(date.getDate()).toBe(1);
      expect(date.getHours()).toBe(0);
      expect(date.getMinutes()).toBe(0);
      expect(date.getMilliseconds()).toBe(0);
  });

  it('should parse date string from server with timezone +1',  () => {
      const date: Date = dateConverter.fromServerToClient('2018-01-01T00:00:00' + offsetString(1));
      expect(date).toBeDefined();
      expect(date.getFullYear()).toBe(2017);
      expect(date.getMonth()).toBe(11);
      expect(date.getDate()).toBe(31);
      expect(date.getHours()).toBe(23);
      expect(date.getMinutes()).toBe(0);
      expect(date.getMilliseconds()).toBe(0);
  });

  it('should convert  date to a string',  () => {
      const date = new Date();
      date.setFullYear(2018, 0, 1);
      date.setHours(0, 0, 0, 0);
    const dateString = dateConverter.fromClientToServer(date);
    expect(dateString).toBe('2018-01-01T00:00:00.000' + offsetString(0));
  });

});


