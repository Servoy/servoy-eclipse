import { FormattingService, Format } from './formatting.service';
import numbro from 'numbro';
import languages from 'numbro/dist/languages.min';
import { ServoyPublicServiceTestingImpl } from '../testing/publictesting.module';

describe('FormattingService', () => {
    let service: FormattingService;
    beforeEach(() => {
 service = new FormattingService(new ServoyPublicServiceTestingImpl());
});

    beforeAll(() => {
       numbro.registerLanguage(languages['en-GB']);
       numbro.registerLanguage(languages['nl-NL']);
    });
    it('should corectly format numbers', () => {
        numbro.setLanguage('en-GB');
        const MILLSIGN = '\u2030'; //�
        const CURRENCY = '\u00A4';
        const myformat: Format = new Format();
        myformat.type = 'NUMBER';
        let num;

        myformat.display = '0.000';
        expect(service.format(10.49, myformat, false)).toEqual('10.490');

        myformat.display = '0000.000';
        expect(service.format(10, myformat, false)).toEqual('0010.000');

        myformat.display = '0000.000';
        expect(service.format(-10, myformat, false)).toEqual('-0010.000');

        myformat.display = '#.###';
        expect(service.format(10.49, myformat, false)).toEqual('10.49');
        
        myformat.display = '#.###';
        num = 1;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeTrue;
        num = 12;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeTrue;
        num = 12.;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeTrue;
        num = 12.1;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeTrue;
        num = 12.12;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeTrue;
        num = 12.123;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeTrue;
        num = 12.1234;
        expect(service.testForNumbersOnly(num, null, null, false, true, myformat, false)).toBeFalse;

        myformat.display = '#.###' + CURRENCY;
        expect(service.format(10.49, myformat, false)).toEqual('10.49\u00A3');

        myformat.display = '#.###$';
        expect(service.format(10.49, myformat, false)).toEqual('10.49$');

        myformat.display = '$ #.###';
        expect(service.format(10.49, myformat, false)).toEqual('$ 10.49');

        myformat.display = '$ -#.###';
        expect(service.format(10.49, myformat, false)).toEqual('$ 10.49');

        myformat.display = '-#.###$';
        expect(service.format(10.49, myformat, false)).toEqual('10.49$');

        myformat.display = '$ -#.###';
        expect(service.format(-10.49, myformat, false)).toEqual('$ -10.49');

        myformat.display = '-#.###$';
        expect(service.format(-10.49, myformat, false)).toEqual('-10.49$');

        myformat.display = '$ #.###-';
        expect(service.format(-10.49, myformat, false)).toEqual('$ 10.49-');

        myformat.display = '#,######.00000';
        expect(service.format(0, myformat, false)).toEqual('0.00000');

        myformat.display = '#,######.00000';
        expect(service.format(1000000, myformat, false)).toEqual('1,000,000.00000');
        
        myformat.display = '#,######.00000';
        expect(service.format(125.5, myformat, false)).toEqual('125.50000');

        myformat.display = '#,######.00000';
        expect(service.format(1125.5, myformat, false)).toEqual('1,125.50000');

        myformat.display = '#.###-$';
        expect(service.format(-10.49, myformat, false)).toEqual('10.49-$');

        myformat.display = '€ #,##0.00;€ -#,##0.00#';
        expect(service.format(10.49, myformat, false)).toEqual('€ 10.49');

        myformat.display = '€ #,##0.00;€ -#,##0.00#';
        expect(service.format(-10.49, myformat, false)).toEqual('€ -10.49');

        myformat.display = '+#.###';
        expect(service.format(10.49, myformat, false)).toEqual('+10.49');

        myformat.display = '#,###.00';
        expect(service.format(1000, myformat, false)).toEqual('1,000.00');

        myformat.display = '#,###.##';
        expect(service.format(1000, myformat, false)).toEqual('1,000');

        myformat.display = '##-';
        expect(service.format(12, myformat, false)).toEqual('12');

        myformat.display = '##-';
        expect(service.format(1, myformat, false)).toEqual('1');

        myformat.display = '##-';
        expect(service.format(-12, myformat, false)).toEqual('12-');

        myformat.display = '-##';
        expect(service.format(12, myformat, false)).toEqual('12');

        myformat.display = '-##';
        expect(service.format(-12, myformat, false)).toEqual('-12');

        myformat.display = '##.##-';
        expect(service.format(12.34, myformat, false)).toEqual('12.34');

        myformat.display = '##.##-';
        expect(service.format(-12.34, myformat, false)).toEqual('12.34-');

        myformat.display = '##.##+';
        expect(service.format(+12.34, myformat, false)).toEqual('12.34+');
        
        myformat.display = '##.##+';
        expect(service.format(12.34, myformat, false)).toEqual('12.34+')

        myformat.display = '-##.##';
        expect(service.format(12.34, myformat, false)).toEqual('12.34');

        myformat.display = '-##.##';
        expect(service.format(-12.34, myformat, false)).toEqual('-12.34');

        myformat.display = '+0';
        expect(service.format(10.49, myformat, false)).toEqual('+10');

        myformat.display = '+0';
        expect(service.format(10.59, myformat, false)).toEqual('+11')
        
        myformat.display = '-0';
        expect(service.format(10.59, myformat, false)).toEqual('11')
        
        myformat.display = '0-';
        expect(service.format(10.59, myformat, false)).toEqual('11')
        
        myformat.display = '0-';
        expect(service.format(-10.59, myformat, false)).toEqual('11-')
        
        myformat.display = '0+';
        expect(service.format(10.59, myformat, false)).toEqual('11+')
        
        myformat.display = '0+';
        expect(service.format(-10.59, myformat, false)).toEqual('-11+')

        myformat.display = '+%00.00';
        expect(service.format(10.49, myformat, false)).toEqual('+%1049.00');

        myformat.display = MILLSIGN + '+00.00';
        expect(service.format(10.49, myformat, false)).toEqual(MILLSIGN + '+10490.00');

        myformat.display = '+' + MILLSIGN + '00.00';
        expect(service.format(10.49, myformat, false)).toEqual('+' + MILLSIGN + '10490.00');

        myformat.display = '00.00E00';
        expect(service.format(10.49, myformat, false)).toEqual('1.0490e+1');

        myformat.display = '##0.0';
        expect(service.format(3.9, myformat, false)).toEqual('3.9');

        myformat.display = '##0.0';
        expect(service.format(30.9, myformat, false)).toEqual('30.9');

        myformat.display = '##0.0';
        expect(service.format(300, myformat, false)).toEqual('300.0');

        myformat.display = '000.0';
        expect(service.format(3.9, myformat, false)).toEqual('003.9');

        myformat.display = '#.#';
        expect(service.format(0.9, myformat, false)).toEqual('0.9');
        
        myformat.display = '#.#';
        expect(service.format(0, myformat, false)).toEqual('');
        
        myformat.display = '#,##0.##';
        expect(service.format(0, myformat, false)).toEqual('0');
        
        myformat.display = '#,##0.##';
        expect(service.format(0.1, myformat, false)).toEqual('0.1');
        
        myformat.display = '#0.##';
        expect(service.format(1, myformat, false)).toEqual('1');
        
        myformat.display = '#0.##';
        expect(service.format(0.1, myformat, false)).toEqual('0.1');

        myformat.display = '#0.##';
        expect(service.format(null, myformat, false)).toEqual('');
        
        myformat.display = '#0.##';
        expect(service.format(1.1, myformat, false)).toEqual('1.1');

        myformat.display = '#0.##';
        expect(service.format(1000.1, myformat, false)).toEqual('1000.1');

        myformat.display = '#0.##';
        expect(service.format(1000.100, myformat, false)).toEqual('1000.1');
        
        myformat.display = '#0.##';
        expect(service.format(1000.101, myformat, false)).toEqual('1000.1');

        myformat.display = '#0.##';
        expect(service.format(0.100, myformat, false)).toEqual('0.1');
        
        myformat.display = '#0.##';
        expect(service.format(0, myformat, false)).toEqual('0');
        
        myformat.display = '#0';
        expect(service.format(0.101, myformat, false)).toEqual('0');
        
        myformat.display = '#0';
        expect(service.format(0.901, myformat, false)).toEqual('1');
        
        myformat.display = '#0';
        expect(service.format(125.5, myformat, false)).toEqual('126');

        myformat.display = '#0';
        expect(service.format(0, myformat, false)).toEqual('0');

        myformat.display = '0#.##';
        expect(service.format(0, myformat, false)).toEqual('00');

        myformat.display = '0#.##';
        expect(service.format(1, myformat, false)).toEqual('01');
        
        myformat.display = '0#.##';
        expect(service.format(100, myformat, false)).toEqual('100');
        
        myformat.display = '0#.##';
        expect(service.format(0.1, myformat, false)).toEqual('00.1');
        
        myformat.display = '0#.##';
        expect(service.format(0.111, myformat, false)).toEqual('00.11');
        
        myformat.display = '0,000.00';
        expect(service.format(0, myformat, false)).toEqual('0000.00');
        
        myformat.display = '0,000.00';
        expect(service.format(1, myformat, false)).toEqual('0001.00');
        
        myformat.display = '0,000.00';
        expect(service.format(1.1, myformat, false)).toEqual('0001.10');
        
        myformat.display = '0,000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('1,000.10');
        
        myformat.display = '0000,000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('0,001,000.10');
        
        myformat.display = '0000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('1000.10');
        
        myformat.display = '0000000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('0001000.10');

        myformat.display = '0,000.00';
        expect(service.format(125.5, myformat, false)).toEqual('0125.50');
        
        myformat.display = '#,##0.00##';
        expect(service.format(125.5, myformat, false)).toEqual('125.50');
        expect(service.format(125.556789, myformat, false)).toEqual('125.5568');
        expect(service.format(125.5567, myformat, false)).toEqual('125.5567');
        expect(service.format(125.556, myformat, false)).toEqual('125.556');
    });

    it('should corectly UNformat  numbers', () => {
        numbro.setLanguage('en-GB');
        const MILLSIGN = '\u2030'; //�
        expect(service.unformat('10.49', '0.000', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('+%1049.00', '+%00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('-10.000', '-0000.000', 'NUMBER')).toEqual(-10);
        expect(service.unformat('-10.000', '###.###', 'NUMBER')).toEqual(-10);
        expect(service.unformat('1,000', '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat('1,000.00', '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat('1,000.00', '#,###.##', 'NUMBER')).toEqual(1000);
        //expect(service.unformat(MILLSIGN + "+10490.00", MILLSIGN + '+00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('1.0490e+1', '00.00E00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('3', '0 \'μm\'', 'NUMBER')).toEqual(3);
    });

    it('should corectly format numbers in dutch locale', () => {
        numbro.setLanguage('nl-NL');
        const MILLSIGN = '\u2030'; //�
        const myformat: Format = new Format();
        myformat.type = 'NUMBER';

        myformat.display = '0.000';
        expect(service.format(10.49,  myformat, false)).toEqual('10,490');

        myformat.display = '0000.000';
        expect(service.format(10,  myformat, false)).toEqual('0010,000');

        myformat.display = '0000.000';
        expect(service.format(-10,  myformat, false)).toEqual('-0010,000');

        myformat.display = '#.###';
        expect(service.format(10.49,  myformat, false)).toEqual('10,49');

        myformat.display = '+#.###';
        expect(service.format(10.49,  myformat, false)).toEqual('+10,49');

        myformat.display = '#,###.00';
        expect(service.format(1000,  myformat, false)).toEqual('1.000,00');

        myformat.display = '#,###.##';
        expect(service.format(1000,  myformat, false)).toEqual('1.000');

        myformat.display = '+0';
        expect(service.format(10.49,  myformat, false)).toEqual('+10');

        myformat.display = '+0';
        expect(service.format(10.79,  myformat, false)).toEqual('+11');
        
        myformat.display = '+%00.00';
        expect(service.format(10.49,  myformat, false)).toEqual('+%1049,00');

        myformat.display = MILLSIGN + '+00.00';
        expect(service.format(10.49,  myformat, false)).toEqual(MILLSIGN + '+10490,00');

        myformat.display = '+' + MILLSIGN + '00.00';
        expect(service.format(10.49,  myformat, false)).toEqual('+' + MILLSIGN + '10490,00');

        myformat.display = '00.00E00';
        expect(service.format(10.49,  myformat, false)).toEqual('1.0490e+1'); // TODO shouldn't this also be in dutch notation??
   
        myformat.display = '0,000.00';
        expect(service.format(0, myformat, false)).toEqual('0000,00');
        
        myformat.display = '0,000.00';
        expect(service.format(1, myformat, false)).toEqual('0001,00');
        
        myformat.display = '0,000.00';
        expect(service.format(1.1, myformat, false)).toEqual('0001,10');
        
        myformat.display = '0,000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('1.000,10');
        
        myformat.display = '0000,000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('0.001.000,10');
        
        myformat.display = '0000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('1000,10');
        
        myformat.display = '0000000.00';
        expect(service.format(1000.1, myformat, false)).toEqual('0001000,10');

        myformat.display = '0,000.00';
        expect(service.format(125.5, myformat, false)).toEqual('0125,50');
   
    });

    it('should corectly UNformat  numbers in dutch locale', () => {
        numbro.setLanguage('nl-NL');
        const MILLSIGN = '\u2030'; //�
        expect(service.unformat('10,49', '0.000', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('+%1049,00', '+%00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('-10,000', '-0000.000', 'NUMBER')).toEqual(-10);
        expect(service.unformat('-10,000', '###.###', 'NUMBER')).toEqual(-10);
        expect(service.unformat('1.000', '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat('1.000,00', '#,###.00', 'NUMBER')).toEqual(1000);
        expect(service.unformat('1.000,00', '#,###.##', 'NUMBER')).toEqual(1000);
        //expect(service.unformat(MILLSIGN + "+10490,00", MILLSIGN + '+00.00', 'NUMBER')).toEqual(10.49);
        expect(service.unformat('1.0490e+1', '00.00E00', 'NUMBER')).toEqual(10.49); // TODO shouldn't this also be in dutch notation??
    });

     it('should corectly unformat dates',  () => {
          let date: Date = service.unformat('01-02-2018', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2018);
          expect(date.getMonth()).toBe(1);
          expect(date.getDate()).toBe(1);
          
          date = service.unformat('10/11/2023', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2023);
          expect(date.getMonth()).toBe(10);
          expect(date.getDate()).toBe(10);
          
          date = service.unformat('11/10/2023', 'MM/dd/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2023);
          expect(date.getMonth()).toBe(10);
          expect(date.getDate()).toBe(10);
          
          const today = new Date();
          date = service.unformat('1', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(today.getMonth());
          expect(date.getDate()).toBe(1);
          
          date = service.unformat('15', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(today.getMonth());
          expect(date.getDate()).toBe(15);
          
          date = service.unformat('11-', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(today.getMonth());
          expect(date.getDate()).toBe(11);
          
          date = service.unformat('112', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(1);
          expect(date.getDate()).toBe(11);
          
          date = service.unformat('1112', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(11);
          expect(date.getDate()).toBe(11);
          
          date = service.unformat('1112-', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(11);
          expect(date.getDate()).toBe(11);
          
          date = service.unformat('11122011', 'dd-MM-yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2011);
          expect(date.getMonth()).toBe(11);
          expect(date.getDate()).toBe(11);
          
           date = service.unformat('11122011', 'dd MM yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2011);
          expect(date.getMonth()).toBe(11);
          expect(date.getDate()).toBe(11);
          
          date = service.unformat('11122015', 'MM/dd/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2015);
          expect(date.getMonth()).toBe(10);
          expect(date.getDate()).toBe(12);
          
          date = service.unformat('2/3', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(2);
          expect(date.getDate()).toBe(2);
          
          date = service.unformat('12/3', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(2);
          expect(date.getDate()).toBe(12);
          
          date = service.unformat('2/11', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(today.getFullYear());
          expect(date.getMonth()).toBe(10);
          expect(date.getDate()).toBe(2);
          
          date = service.unformat('2/3/24', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2024);
          expect(date.getMonth()).toBe(2);
          expect(date.getDate()).toBe(2);
          
          date = service.unformat('2/3/2024', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2024);
          expect(date.getMonth()).toBe(2);
          expect(date.getDate()).toBe(2);
          
          date = service.unformat('020323', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2023);
          expect(date.getMonth()).toBe(2);
          expect(date.getDate()).toBe(2);
          
          date = service.unformat('110123', 'dd/MM/yyyy', 'DATETIME', null, true);
          expect(date).toBeDefined();
          expect(date.getFullYear()).toBe(2023);
          expect(date.getMonth()).toBe(0);
          expect(date.getDate()).toBe(11);
          
      });
  
     it('should corectly format dates', () => {
        numbro.setLanguage('en-GB');
        const MILLSIGN = '\u2030'; //�
        // this test depends on locale, p.m. is for nl

        const myformat: Format = new Format();
        myformat.type = 'DATETIME';
        myformat.display = 'Z';
        const z = service.format(new Date(2014, 10, 3, 15, 23, 14), myformat, false);

        myformat.display = 'dd-MM-yyyy HH:mma s  G S';
        expect(service.format(new Date(2014, 10, 1, 23, 23, 14, 500), myformat, false)).toEqual('01-11-2014 23:23PM 14  AD 500');

        myformat.display = 'dd-MM-yyyy Z D';
        expect(service.format(new Date(2014, 10, 3, 15, 23, 14), myformat, false)).toEqual('03-11-2014 ' + z + ' 307'); // TODO fix timezone issues

        myformat.display = 'dd/MM/yyyy Z D';
        expect(service.format(new Date(2014, 10, 4, 15, 23, 14), myformat, false)).toEqual('04/11/2014 ' + z + ' 308'); // TODO fix timezone issues

        myformat.display = 'dd MM yyyy KK:mm D';
        expect(service.format(new Date(2014, 10, 5, 12, 23, 14), myformat, false)).toEqual('05 11 2014 12:23 309');

        myformat.display = 'dd MM yyyy kk:mm D';
        // the following sets hour to 24:23 which is next day ,so 6'th
        expect(service.format(new Date(2014, 10, 5, 24, 23, 14), myformat, false)).toEqual('06 11 2014 00:23 310');
    });

    it('should corectly format strings', () => {
        const myformat: Format = new Format();
        myformat.type = 'TEXT';

        myformat.display = 'UU##UU##';
        expect(service.format('aa11BB22', myformat,false)).toEqual('AA11BB22');

        myformat.display = 'HHHHUU##';
        expect(service.format('aa11BB22', myformat,false)).toEqual('AA11BB22');

        myformat.display = '#HHHUU##';
        expect(()=>{
service.format('aa11BB22', myformat,false);
}).toThrow('input string not corresponding to format : aa11BB22 , #HHHUU##');
    });
});
