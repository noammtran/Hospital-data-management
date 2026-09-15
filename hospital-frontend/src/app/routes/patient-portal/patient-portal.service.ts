import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';

import { ApiResponse, PatientSelfRecord } from './patient-portal.models';

@Injectable({ providedIn: 'root' })
export class PatientPortalService {
  private readonly http = inject(HttpClient);

  getMyRecord() {
    return this.http
      .get<ApiResponse<PatientSelfRecord>>('/api/v1/patient-portal/me')
      .pipe(map(response => response.data));
  }
}
