import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import { ApiResponse } from '../patient/patient.models';
import { DiagnosisOption, MedicineOption, StatisticsResult } from './statistics.models';

export interface StatisticsFilter {
  from?: string;
  to?: string;
  icdCode?: string;
  medicineId?: number;
}

@Injectable({ providedIn: 'root' })
export class StatisticsService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/statistics';

  filter(f: StatisticsFilter): Observable<StatisticsResult> {
    let params = new HttpParams();
    if (f.from) {
      params = params.set('from', f.from);
    }
    if (f.to) {
      params = params.set('to', f.to);
    }
    if (f.icdCode) {
      params = params.set('icdCode', f.icdCode);
    }
    if (f.medicineId != null) {
      params = params.set('medicineId', f.medicineId);
    }
    return this.http
      .get<ApiResponse<StatisticsResult>>(`${this.url}/patients`, { params })
      .pipe(map(res => res.data));
  }

  diagnosisOptions(): Observable<DiagnosisOption[]> {
    return this.http
      .get<ApiResponse<DiagnosisOption[]>>(`${this.url}/diagnosis-options`)
      .pipe(map(res => res.data));
  }

  medicineOptions(): Observable<MedicineOption[]> {
    return this.http
      .get<ApiResponse<MedicineOption[]>>(`${this.url}/medicine-options`)
      .pipe(map(res => res.data));
  }
}
