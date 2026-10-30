import { HttpClient, HttpParams } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { Observable } from 'rxjs';

import { Keyword, Town } from './models';

@Injectable({ providedIn: 'root' })
export class DictionaryService {
  private readonly http = inject(HttpClient);

  findKeywords(query: string): Observable<Keyword[]> {
    return this.http.get<Keyword[]>('/api/keywords', { params: new HttpParams().set('query', query) });
  }

  getTowns(): Observable<Town[]> {
    return this.http.get<Town[]>('/api/towns');
  }
}
