import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  Admission,
  AdmissionEditData,
  AdmissionOptions,
  ApiResponse,
  CreateAdmissionPayload,
  PageResponse,
  PatientDetail,
  PatientHistoryEntry,
  PatientHistoryItem,
  PatientListItem,
  PatientPayload,
  UpdateAdmissionPayload,
  Visit,
} from './patient.models';

export interface PatientQuery {
  page: number;
  size: number;
  search?: string;
  searchField?: string;
  status?: number | null;
  gender?: number | null;
}

@Injectable({ providedIn: 'root' })
export class PatientService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/patients';

  list(query: PatientQuery): Observable<PageResponse<PatientListItem>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
      params = params.set('searchField', query.searchField ?? 'name');
    }
    if (query.status != null) {
      params = params.set('status', query.status);
    }
    if (query.gender != null) {
      params = params.set('gender', query.gender);
    }
    return this.http
      .get<ApiResponse<PageResponse<PatientListItem>>>(this.url, { params })
      .pipe(map(res => res.data));
  }

  getDetail(id: string): Observable<PatientDetail> {
    return this.http.get<ApiResponse<PatientDetail>>(`${this.url}/${id}`).pipe(map(res => res.data));
  }

  getMedicalRecord(id: string): Observable<Visit[]> {
    return this.http
      .get<ApiResponse<Visit[]>>(`${this.url}/${id}/medical-record`)
      .pipe(map(res => res.data));
  }

  getAdmissionOptions(): Observable<AdmissionOptions> {
    return this.http
      .get<ApiResponse<AdmissionOptions>>('/api/v1/admissions/options')
      .pipe(map(res => res.data));
  }

  createAdmission(patientId: string, payload: CreateAdmissionPayload): Observable<Admission> {
    return this.http
      .post<ApiResponse<Admission>>(`${this.url}/${patientId}/admissions`, payload)
      .pipe(map(res => res.data));
  }

  getAdmissionEditData(patientId: string, admissionId: string): Observable<AdmissionEditData> {
    return this.http
      .get<ApiResponse<AdmissionEditData>>(
        `${this.url}/${patientId}/admissions/${admissionId}/edit-data`
      )
      .pipe(map(res => res.data));
  }

  updateAdmission(
    patientId: string,
    admissionId: string,
    payload: UpdateAdmissionPayload
  ): Observable<Admission> {
    return this.http
      .put<ApiResponse<Admission>>(
        `${this.url}/${patientId}/admissions/${admissionId}`,
        payload
      )
      .pipe(map(res => res.data));
  }

  deleteAdmission(patientId: string, admissionId: string): Observable<void> {
    return this.http
      .delete<ApiResponse<void>>(`${this.url}/${patientId}/admissions/${admissionId}`)
      .pipe(map(() => undefined));
  }

  getAllHistory(page: number, size: number): Observable<PageResponse<PatientHistoryEntry>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<ApiResponse<PageResponse<PatientHistoryEntry>>>(`${this.url}/history`, { params })
      .pipe(map(res => res.data));
  }

  getHistory(id: string, page: number, size: number): Observable<PageResponse<PatientHistoryItem>> {
    const params = new HttpParams().set('page', page).set('size', size);
    return this.http
      .get<ApiResponse<PageResponse<PatientHistoryItem>>>(`${this.url}/${id}/history`, { params })
      .pipe(map(res => res.data));
  }

  create(payload: PatientPayload): Observable<PatientDetail> {
    return this.http.post<ApiResponse<PatientDetail>>(this.url, payload).pipe(map(res => res.data));
  }

  update(id: string, payload: PatientPayload): Observable<PatientDetail> {
    return this.http
      .put<ApiResponse<PatientDetail>>(`${this.url}/${id}`, payload)
      .pipe(map(res => res.data));
  }

  checkIn(id: string): Observable<PatientDetail> {
    return this.http
      .post<ApiResponse<PatientDetail>>(`${this.url}/${id}/check-in`, null)
      .pipe(map(res => res.data));
  }

  startExamination(id: string): Observable<PatientDetail> {
    return this.http
      .post<ApiResponse<PatientDetail>>(`${this.url}/${id}/start-examination`, null)
      .pipe(map(res => res.data));
  }

  completeExamination(id: string): Observable<PatientDetail> {
    return this.http
      .post<ApiResponse<PatientDetail>>(`${this.url}/${id}/complete-examination`, null)
      .pipe(map(res => res.data));
  }

  dischargeAdmission(patientId: string, admissionId: string): Observable<Admission> {
    return this.http
      .post<ApiResponse<Admission>>(
        `${this.url}/${patientId}/admissions/${admissionId}/discharge`,
        null
      )
      .pipe(map(res => res.data));
  }

  delete(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.url}/${id}`).pipe(map(() => undefined));
  }

  bulkDelete(ids: string[]): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.url}/bulk-delete`, { ids })
      .pipe(map(() => undefined));
  }
}
