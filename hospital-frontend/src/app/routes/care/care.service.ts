import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { map } from 'rxjs';
import { ApiResponse } from '@core/authentication/interface';
import {
  CareDetail,
  CareInpatient,
  CareOptions,
  CarePage,
  CareQuery,
  CareReference,
  CareTask,
  CareVisit,
} from './care.models';

@Injectable({
  providedIn: 'root',
})
export class CareService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/care';
  options() {
    return this.get<CareOptions>('/options');
  }
  visits(query: CareQuery) {
    return this.get<CarePage<CareVisit>>('/visits', query);
  }
  tasks(query: CareQuery) {
    return this.get<CarePage<CareTask>>('/tasks', query);
  }
  inpatients(query: CareQuery) {
    return this.get<CarePage<CareInpatient>>('/inpatients', query);
  }
  medicines(search: string) {
    return this.get<CarePage<CareReference>>('/medicines', { search });
  }
  detail(id: string) {
    return this.get<CareDetail>(`/visits/${encodeURIComponent(id)}`);
  }
  register(body: unknown) {
    return this.post('/visits', body);
  }
  visitAction(id: string, action: string, body: unknown = {}) {
    return this.post(`/visits/${encodeURIComponent(id)}/${action}`, body);
  }
  taskAction(id: number, action: string, body: unknown = {}) {
    return this.post(`/tasks/${id}/${action}`, body);
  }
  adoptAdmission(id: string, body: unknown) {
    return this.post(`/admissions/${encodeURIComponent(id)}/workspace`, body);
  }
  configure(resource: 'points' | 'services' | 'assignments', body: unknown, id?: number) {
    const url = `${this.url}/${resource}${id != null ? `/${id}` : ''}`;
    return (
      resource === 'assignments' || id != null
        ? this.http.put<ApiResponse<CareOptions>>(url, body)
        : this.http.post<ApiResponse<CareOptions>>(url, body)
    ).pipe(map(res => res.data));
  }
  private get<T>(path: string, query: CareQuery = {}) {
    let params = new HttpParams();
    for (const [key, value] of Object.entries(query)) {
      if (value != null && value !== '') params = params.set(key, value);
    }
    return this.http
      .get<ApiResponse<T>>(`${this.url}${path}`, { params })
      .pipe(map(res => res.data));
  }
  private post(path: string, body: unknown) {
    return this.http
      .post<ApiResponse<CareDetail>>(`${this.url}${path}`, body)
      .pipe(map(res => res.data));
  }
}
