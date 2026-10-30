import { FormControl } from '@angular/forms';

import { moneyFormat, notBlank, wholeNumber } from './campaign-validators';

describe('campaign validators', () => {
  it('notBlank rejects whitespace only', () => {
    expect(notBlank(new FormControl('   '))).toEqual({ required: true });
    expect(notBlank(new FormControl('Summer sale'))).toBeNull();
  });

  it('moneyFormat allows up to two decimals', () => {
    expect(moneyFormat(new FormControl(10))).toBeNull();
    expect(moneyFormat(new FormControl(0.1))).toBeNull();
    expect(moneyFormat(new FormControl(1.25))).toBeNull();
    expect(moneyFormat(new FormControl(1.255))).toEqual({ money: true });
    expect(moneyFormat(new FormControl(-1))).toEqual({ money: true });
  });

  it('wholeNumber rejects fractions', () => {
    expect(wholeNumber(new FormControl(25))).toBeNull();
    expect(wholeNumber(new FormControl(2.5))).toEqual({ wholeNumber: true });
  });

  it('leaves empty values to the required validator', () => {
    expect(moneyFormat(new FormControl(null))).toBeNull();
    expect(wholeNumber(new FormControl(null))).toBeNull();
  });
});
