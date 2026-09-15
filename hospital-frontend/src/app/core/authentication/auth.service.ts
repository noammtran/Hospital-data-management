import { Injectable, inject } from '@angular/core';
import { Observable, ReplaySubject, catchError, iif, map, of, switchMap, tap, throwError } from 'rxjs';
import { AppPermission, User, UserRole } from './interface';
import { LoginService } from './login.service';
import { TokenService } from './token.service';

@Injectable({
  providedIn: 'root',
})
export class AuthService {
  private readonly loginService = inject(LoginService);
  private readonly tokenService = inject(TokenService);

  private readonly user$ = new ReplaySubject<User>(1);
  private currentUser: User = {};

  init() {
    return new Promise<void>(resolve => this.restore().subscribe(() => resolve()));
  }

  change() {
    return this.user$.asObservable();
  }

  check() {
    return this.tokenService.valid();
  }

  login(username: string, password: string, rememberMe = false) {
    return this.loginService.login(username, password, rememberMe).pipe(
      tap(token => this.tokenService.set(token)),
      switchMap(() => this.loginService.user()),
      tap(user => this.publishUser(user)),
      map(() => this.check()),
      catchError(error => {
        this.clearSession();
        return throwError(() => error);
      })
    );
  }

  restore(): Observable<User> {
    if (!this.check()) {
      const user: User = {};
      this.publishUser(user);
      return of(user);
    }

    return this.loginService.user().pipe(
      tap(user => this.publishUser(user)),
      catchError(() => {
        this.clearSession();
        return of<User>({});
      })
    );
  }

  logout() {
    return this.loginService.logout().pipe(
      tap(() => this.clearSession()),
      map(() => !this.check())
    );
  }

  user() {
    return this.user$.asObservable();
  }

  menu() {
    return iif(() => this.check(), this.loginService.menu(), of([]));
  }

  hasPermission(permission: AppPermission): boolean {
    return this.currentUser.permissions?.includes(permission) ?? false;
  }

  hasAnyPermission(permissions: AppPermission[]): boolean {
    return permissions.some(permission => this.hasPermission(permission));
  }

  hasRole(role: UserRole): boolean {
    return this.currentUser.roles?.includes(role) ?? false;
  }

  mustChangePassword(): boolean {
    return this.currentUser.mustChangePassword === true;
  }

  landingPath(): string {
    if (this.mustChangePassword()) {
      return '/auth/change-password';
    }
    if (this.hasPermission('VIEW_OWN_RECORD')) return '/my-record';
    return this.hasPermission('CARE_VIEW') ? '/care' : '/patients';
  }

  changePassword(currentPassword: string, newPassword: string): Observable<void> {
    return this.loginService.changePassword(currentPassword, newPassword).pipe(
      tap(() => this.clearSession())
    );
  }

  private publishUser(user: User): void {
    this.currentUser = user;
    this.user$.next(user);
  }

  private clearSession(): void {
    this.tokenService.clear();
    this.publishUser({});
  }
}
