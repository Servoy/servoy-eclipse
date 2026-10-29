import { FormCache, PartCache } from './types';

describe('FormCache.getBodyPartLayout (SVY-21655)', () => {
    const bodyLayout = { position: 'absolute', 'overflow-x': 'hidden', 'overflow-y': 'hidden' };
    const otherLayout = { position: 'absolute', overflow: 'hidden' };

    function createFormCache(): FormCache {
        return new FormCache('testform', { width: 640, height: 490 }, false, '', null);
    }

    it('should return the layout of the body part when it is the only part', () => {
        const formCache = createFormCache();
        formCache.addPart(new PartCache('body', ['svy-body'], bodyLayout));

        expect(formCache.getBodyPartLayout()).toEqual(bodyLayout);
    });

    it('should return the layout of the body part when header parts come before it', () => {
        const formCache = createFormCache();
        formCache.addPart(new PartCache('titleheader', ['svy-titleheader'], otherLayout));
        formCache.addPart(new PartCache('header', ['svy-header'], otherLayout));
        formCache.addPart(new PartCache('body', ['svy-body'], bodyLayout));
        formCache.addPart(new PartCache('footer', ['svy-footer'], otherLayout));

        expect(formCache.getBodyPartLayout()).toEqual(bodyLayout);
    });
});
