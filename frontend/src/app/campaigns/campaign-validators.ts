import { AbstractControl, ValidationErrors } from '@angular/forms';

const MONEY_PATTERN = /^\d+(\.\d{1,2})?$/;
const WHOLE_NUMBER_PATTERN = /^\d+$/;

export function notBlank(control: AbstractControl): ValidationErrors | null {
  return typeof control.value === 'string' && control.value.trim() === '' ? { required: true } : null;
}

export function moneyFormat(control: AbstractControl): ValidationErrors | null {
  return isEmpty(control) || MONEY_PATTERN.test(String(control.value)) ? null : { money: true };
}

export function wholeNumber(control: AbstractControl): ValidationErrors | null {
  return isEmpty(control) || WHOLE_NUMBER_PATTERN.test(String(control.value)) ? null : { wholeNumber: true };
}

function isEmpty(control: AbstractControl): boolean {
  return control.value === null || control.value === '';
}
