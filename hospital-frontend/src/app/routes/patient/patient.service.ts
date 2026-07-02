import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  ApiResponse,
  PageResponse,
  PatientDetail,
  PatientHistoryEntry,
  PatientHistoryItem,
  PatientListItem,
  PatientPayload,
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

  delete(id: string): Observable<void> {
    return this.http.delete<ApiResponse<void>>(`${this.url}/${id}`).pipe(map(() => undefined));
  }

  bulkDelete(ids: string[]): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.url}/bulk-delete`, { ids })
      .pipe(map(() => undefined));
  }
}
