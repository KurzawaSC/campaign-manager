import { HttpErrorResponse } from '@angular/common/http';

import { ApiProblem } from './models';

export function toApiProblem(error: unknown): ApiProblem {
  return error instanceof HttpErrorResponse && error.error && typeof error.error === 'object' ? error.error : {};
}

export function problemMessage(error: unknown): string {
  if (error instanceof HttpErrorResponse && error.status === 0) {
    return 'Cannot reach the server. Check that the API is running.';
  }
  return toApiProblem(error).detail ?? 'Something went wrong. Please try again.';
}
