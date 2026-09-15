import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { Observable, map, of } from 'rxjs';

import { Menu } from '@core';
import { ApiResponse, AppPermission, Token, User, UserRole } from './interface';

interface AuthTokenPayload {
  accessToken: string;
  tokenType: string;
  expiresIn: number;
}

interface CurrentUserPayload {
  id: number;
  username: string;
  email: string | null;
  displayName: string;
  role: UserRole;
  permissions: AppPermission[];
  mustChangePassword: boolean;
}

@Injectable({
  providedIn: 'root',
})
export class LoginService {
  protected readonly http = inject(HttpClient);

  login(username: string, password: string, _rememberMe = false): Observable<Token> {
    return this.http
      .post<ApiResponse<AuthTokenPayload>>('/api/v1/auth/login', { username, password })
      .pipe(
        map(({ data }) => ({
          access_token: data.accessToken,
          token_type: data.tokenType,
          expires_in: data.expiresIn,
        }))
      );
  }

  refresh(params: Record<string, any>) {
    return this.http.post<Token>('/api/v1/auth/refresh', params);
  }

  logout(): Observable<unknown> {
    return of(undefined);
  }

  user(): Observable<User> {
    return this.http.get<ApiResponse<CurrentUserPayload>>('/api/v1/auth/me').pipe(
      map(({ data }) => ({
        ...data,
        name: data.displayName,
        roles: [data.role],
        avatar: 'images/avatar-default.jpg',
      }))
    );
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.http
      .post<ApiResponse<void>>('/api/v1/auth/change-password', { currentPassword, newPassword })
      .pipe(map(() => undefined));
  }

  menu() {
    return this.http
      .get<{ menu: Menu[] }>('data/menu.json?_t=' + Date.now())
      .pipe(map(res => res.menu));
  }
}
