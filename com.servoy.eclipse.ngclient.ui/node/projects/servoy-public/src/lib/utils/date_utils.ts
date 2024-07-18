import { DateTime } from 'luxon';

let fixedFirstDayOfWeek: number = null;

/**
 * Returns a zero-based index for first day of the week, as used by the specified locale
 * e.g. Sunday (returns 0), or Monday (returns 1)
 *
 * @param locale
 * @returns
 */
export  const getFirstDayOfWeek = (locale: string): number  => {
    if (fixedFirstDayOfWeek !== null) return fixedFirstDayOfWeek;
    
    // from http://www.unicode.org/cldr/data/common/supplemental/supplementalData.xml:supplementalData/weekData/firstDay
    const firstDay: {[property:string]: number} = {/*default is 1=Monday*/
        bd: 5,
        mv: 5,
        ae: 6,
        af: 6,
        bh: 6,
        dj: 6,
        dz: 6,
        eg: 6,
        iq: 6,
        ir: 6,
        jo: 6,
        kw: 6,
        ly: 6,
        ma: 6,
        om: 6,
        qa: 6,
        sa: 6,
        sd: 6,
        sy: 6,
        ye: 6,
        ag: 0,
        ar: 0,
        as: 0,
        au: 0,
        br: 0,
        bs: 0,
        bt: 0,
        bw: 0,
        by: 0,
        bz: 0,
        ca: 0,
        cn: 0,
        co: 0,
        dm: 0,
        do: 0,
        et: 0,
        gt: 0,
        gu: 0,
        hk: 0,
        hn: 0,
        id: 0,
        ie: 0,
        il: 0,
        in: 0,
        jm: 0,
        jp: 0,
        ke: 0,
        kh: 0,
        kr: 0,
        la: 0,
        mh: 0,
        mm: 0,
        mo: 0,
        mt: 0,
        mx: 0,
        mz: 0,
        ni: 0,
        np: 0,
        nz: 0,
        pa: 0,
        pe: 0,
        ph: 0,
        pk: 0,
        pr: 0,
        py: 0,
        sg: 0,
        sv: 0,
        th: 0,
        tn: 0,
        tt: 0,
        tw: 0,
        um: 0,
        us: 0,
        ve: 0,
        vi: 0,
        ws: 0,
        za: 0,
        zw: 0
    };

    const split = locale.split('-');
    const country =split.length > 1?split[1].toLowerCase():split[0].toLowerCase();
    const dow = firstDay[country];
    return (dow === undefined) ? 1 : dow; /*Number*/
};

/**
 * Floors the specified date to the beginning of week.
 *
 * @param date
 * @returns
 */
export const floorToWeek = (date: DateTime): DateTime => {
    const fd = getFirstDayOfWeek(date.locale);
    const day = date.weekday % 7;   // convert to 0=sunday .. 6=saturday
    const dayAdjust =  day >= fd ? -day + fd : -day + fd - 7;
    return date.plus({days: dayAdjust});
};

export  const setFirstDayOfWeek = (value: number): void=> {
    fixedFirstDayOfWeek = value;
}