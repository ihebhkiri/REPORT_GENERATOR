import {fromExposureMode, toExposureMode} from './dataset-exposure.model';

describe('dataset exposure mode conversions', () => {
  it('maps all boolean combinations to their UI mode and back', () => {
    const cases = [
      {main: false, related: false, mode: 'NONE' as const},
      {main: true, related: false, mode: 'MAIN_ONLY' as const},
      {main: false, related: true, mode: 'RELATED_ONLY' as const},
      {main: true, related: true, mode: 'MAIN_AND_RELATED' as const},
    ];

    for (const current of cases) {
      expect(toExposureMode(current.main, current.related)).toBe(current.mode);
      expect(fromExposureMode(current.mode)).toEqual({
        displayMain: current.main,
        displayRelated: current.related,
      });
    }
  });
});
