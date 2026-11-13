import { describe, it, expect, vi } from 'vitest';
import { FormatFilterPipe } from './format.pipe';
import { Format, FormattingService } from './formatting.service';
import { LoggerFactory, LoggerService } from '../logger.service';

const createLoggerFactoryMock = (logger: LoggerService): LoggerFactory =>
    ({ getLogger: vi.fn().mockReturnValue(logger) } as unknown as LoggerFactory);

describe('FormatFilterPipe', () => {

    it('should return the input unchanged when no format is given', () => {
        const formatService = { format: vi.fn() } as unknown as FormattingService;
        const logger = { error: vi.fn() } as unknown as LoggerService;
        const pipe = new FormatFilterPipe(formatService, createLoggerFactoryMock(logger));
        expect(pipe.transform('abc', null as any)).toBe('abc');
        expect(formatService.format).not.toHaveBeenCalled();
    });

    it('should delegate to the formatting service using display when display is set', () => {
        const formatService = { format: vi.fn().mockReturnValue('formatted') } as unknown as FormattingService;
        const logger = { error: vi.fn() } as unknown as LoggerService;
        const pipe = new FormatFilterPipe(formatService, createLoggerFactoryMock(logger));
        const format = new Format();
        format.type = 'NUMBER';
        format.display = '#.##';
        expect(pipe.transform(1.234, format)).toBe('formatted');
        // useEditFormat is !format.display => false when display is set
        expect(formatService.format).toHaveBeenCalledWith(1.234, format, false);
    });

    it('should pass useEditFormat=true when there is no display format', () => {
        const formatService = { format: vi.fn().mockReturnValue('x') } as unknown as FormattingService;
        const logger = { error: vi.fn() } as unknown as LoggerService;
        const pipe = new FormatFilterPipe(formatService, createLoggerFactoryMock(logger));
        const format = new Format();
        format.type = 'NUMBER';
        pipe.transform(5, format);
        expect(formatService.format).toHaveBeenCalledWith(5, format, true);
    });

    it('should log the error and return undefined when formatting throws', () => {
        const err = new Error('bad format');
        const formatService = {
            format: vi.fn().mockImplementation(() => {
                throw err;
            })
        } as unknown as FormattingService;
        const logger = { error: vi.fn() } as unknown as LoggerService;
        const pipe = new FormatFilterPipe(formatService, createLoggerFactoryMock(logger));
        const format = new Format();
        format.type = 'TEXT';
        format.display = 'UU';
        expect(pipe.transform('a', format)).toBeUndefined();
        expect(logger.error).toHaveBeenCalledWith(err);
    });
});
