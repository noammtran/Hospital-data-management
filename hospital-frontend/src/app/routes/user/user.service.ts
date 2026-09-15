import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map } from 'rxjs';

import {
  ApiResponse,
  CreateUserPayload,
  PageResponse,
  UpdateUserPayload,
  UserAccount,
  UserQuery,
  StaffWorkProfile,
  StaffDoctor,
  CreateStaffDoctorPayload,
} from './user.models';

@Injectable({ providedIn: 'root' })
export class UserService {
  private readonly http = inject(HttpClient);
  private readonly url = '/api/v1/users';

  list(query: UserQuery): Observable<PageResponse<UserAccount>> {
    let params = new HttpParams().set('page', query.page).set('size', query.size);
    if (query.search?.trim()) {
      params = params.set('search', query.search.trim());
    }
    if (query.role) {
      params = params.set('role', query.role);
    }
    if (query.active != null) {
      params = params.set('active', query.active);
    }

    return this.http
      .get<ApiResponse<PageResponse<UserAccount>>>(this.url, { params })
      .pipe(map(response => response.data!));
  }

  get(id: number): Observable<UserAccount> {
    return this.http
      .get<ApiResponse<UserAccount>>(`${this.url}/${id}`)
      .pipe(map(response => response.data!));
  }

  create(payload: CreateUserPayload): Observable<UserAccount> {
    return this.http
      .post<ApiResponse<UserAccount>>(this.url, payload)
      .pipe(map(response => response.data!));
  }

  workProfile(id: number): Observable<StaffWorkProfile> {
    return this.http
      .get<ApiResponse<StaffWorkProfile>>(`${this.url}/${id}/work-profile`)
      .pipe(map(response => response.data!));
  }

  createDoctorProfile(id: number, payload: CreateStaffDoctorPayload): Observable<StaffDoctor> {
    return this.http
      .post<ApiResponse<StaffDoctor>>(`${this.url}/${id}/doctor-profile`, payload)
      .pipe(map(response => response.data!));
  }

  update(id: number, payload: UpdateUserPayload): Observable<UserAccount> {
    return this.http
      .put<ApiResponse<UserAccount>>(`${this.url}/${id}`, payload)
      .pipe(map(response => response.data!));
  }

  setActive(id: number, active: boolean): Observable<UserAccount> {
    return this.http
      .patch<ApiResponse<UserAccount>>(`${this.url}/${id}/status`, { active })
      .pipe(map(response => response.data!));
  }

  resetPassword(id: number): Observable<void> {
    return this.http
      .post<ApiResponse<void>>(`${this.url}/${id}/reset-password`, {})
      .pipe(map(() => undefined));
  }
}
