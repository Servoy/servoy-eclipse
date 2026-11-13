import { describe, it, expect, afterEach } from 'vitest';
import { DateTime } from 'luxon';
import { getFirstDayOfWeek, floorToWeek, setFirstDayOfWeek } from './date_utils';

describe('date_utils', () => {

    afterEach(() => {
        // reset the module-level fixed first-day-of-week so tests stay independent
        setFirstDayOfWeek(null as any);
    });

    describe('getFirstDayOfWeek', () => {
        it('should return 0 (Sunday) for US locale', () => {
            expect(getFirstDayOfWeek('en-US')).toBe(0);
        });

        it('should default to 1 (Monday) for a locale not in the table', () => {
            expect(getFirstDayOfWeek('en-GB')).toBe(1);
        });

        it('should return 6 (Saturday) for an arabic/egypt locale', () => {
            expect(getFirstDayOfWeek('ar-EG')).toBe(6);
        });

        it('should return 5 for Bangladesh', () => {
            expect(getFirstDayOfWeek('bn-BD')).toBe(5);
        });

        it('should fall back to using the language part when there is no country part', () => {
            // 'us' as the only segment -> looked up as country 'us' -> 0
            expect(getFirstDayOfWeek('us')).toBe(0);
            // unknown single-segment locale defaults to 1
            expect(getFirstDayOfWeek('xx')).toBe(1);
        });

        it('should be case-insensitive on the country part', () => {
            expect(getFirstDayOfWeek('en-us')).toBe(0);
        });

        it('should return the fixed value once set, ignoring the locale', () => {
            setFirstDayOfWeek(3);
            expect(getFirstDayOfWeek('en-US')).toBe(3);
            expect(getFirstDayOfWeek('ar-EG')).toBe(3);
        });

        it('should honor a fixed value of 0', () => {
            setFirstDayOfWeek(0);
            expect(getFirstDayOfWeek('en-GB')).toBe(0);
        });
    });

    describe('floorToWeek', () => {
        it('should floor to the Sunday of the week for a US (Sunday-first) locale', () => {
            // 2024-03-13 is a Wednesday
            const date = DateTime.fromObject({ year: 2024, month: 3, day: 13 }, { locale: 'en-US' });
            const floored = floorToWeek(date);
            expect(floored.weekdayLong).toBe('Sunday');
            expect(floored.day).toBe(10);
            expect(floored.month).toBe(3);
        });

        it('should floor to the Monday of the week for a Monday-first locale', () => {
            // 2024-03-13 is a Wednesday
            const date = DateTime.fromObject({ year: 2024, month: 3, day: 13 }, { locale: 'en-GB' });
            const floored = floorToWeek(date);
            expect(floored.weekdayLong).toBe('Monday');
            expect(floored.day).toBe(11);
        });

        it('should return the same day when already on the first day of the week', () => {
            // 2024-03-11 is a Monday
            const monday = DateTime.fromObject({ year: 2024, month: 3, day: 11 }, { locale: 'en-GB' });
            const floored = floorToWeek(monday);
            expect(floored.day).toBe(11);
            expect(floored.weekdayLong).toBe('Monday');
        });

        it('should roll back across a month boundary (Sunday-first) when needed', () => {
            // 2024-03-02 is a Saturday; Sunday-first week starts 2024-02-25
            const date = DateTime.fromObject({ year: 2024, month: 3, day: 2 }, { locale: 'en-US' });
            const floored = floorToWeek(date);
            expect(floored.weekdayLong).toBe('Sunday');
            expect(floored.month).toBe(2);
            expect(floored.day).toBe(25);
        });
    });
});
